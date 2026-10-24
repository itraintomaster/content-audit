package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.datacoherence.*;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.batch.AudioPrepublicationService.*;
import com.learney.contentaudit.revisiondomain.nonregression.AudioRerecordRequest;
import java.util.*;
import java.util.function.Function;

/** A fresh, local read of the audio text registry against this exact content checkpoint. */
final class DefaultAudioPrepublicationService implements AudioPrepublicationService {
    private final BatchServiceConfig config;
    private final BatchChangeLedger ledger;
    private final Function<CourseEntity, String> fingerprint;
    private final SpokenTextBuilder spoken = new DefaultSpokenTextBuilder();
    DefaultAudioPrepublicationService(BatchServiceConfig config, BatchChangeLedger ledger,
            Function<CourseEntity, String> fingerprint) {
        this.config = config; this.ledger = ledger; this.fingerprint = fingerprint;
    }
    @Override public View execute(Request request) {
        if (request == null || !"read".equals(Objects.toString(request.action(), "read")))
            throw new IllegalArgumentException("La preparación de audios sólo permite leer; no genera ni publica.");
        var engine = config.getAuditPrepublicationEngine();
        if (engine == null) throw new IllegalStateException("Falta la inspección local del registro de audios de prepublicación.");
        var batch = request.batchId() == null ? config.getBatchStore().open().orElseThrow(
                () -> new IllegalArgumentException("Elegí una corrección para preparar sus audios."))
                : config.getBatchStore().load(request.batchId()).orElseThrow(
                        () -> new IllegalArgumentException("No existe esa corrección."));
        CourseEntity live = config.getCourseRepository().load(config.getCoursePath());
        CourseEntity start = BatchCourses.start(batch, live, config.getBatchStore());
        if (start == null) throw new IllegalStateException("Falta el contenido conservado de esa corrección.");
        CourseEntity complete = ledger.composedCourse(batch, start);
        CourseEntity current = DefaultCurrentAnalysisService.cut(complete, batch.getLevel());
        Set<String> selectedQuizzes = quizIds(current);
        boolean globalContextComplete = quizIds(complete).containsAll(quizIds(live));
        // Shared audio keys are a global invariant: inspect all levels before selecting the batch's rows.
        AuditReport report = engine.runAudit(config.getCourseMapper().map(complete), new AnalyzerRunSelection(
                List.of("data-coherence"), Map.of("data-coherence", new EvaluationRunPolicy(
                        0, false, null, null, EvaluationRunPurpose.READING, 1))));
        var requests = new LinkedHashMap<String, AudioRerecordRequest>();
        for (var item : BatchBook.list(batch.getPendingRerecords())) requests.put(key(item.getQuizId(),item.getAudioKey()), item);
        for (var change : BatchBook.activeChanges(batch)) if (change.getGateVerdict() != null)
            for (var item : BatchBook.list(change.getGateVerdict().getRerecords())) requests.put(key(item.getQuizId(),item.getAudioKey()), item);
        var originalQuizzes = mapped(config.getCourseMapper().map(start));
        // Derive missing legacy queue entries from the final spoken text; no audio finding is needed in CONTENT.
        for (var node : BatchBook.nodes(report)) if (node.getEntity() instanceof AuditableQuiz quiz
                && selectedQuizzes.contains(quiz.getId())) {
            AuditableQuiz original = originalQuizzes.get(quiz.getId());
            SentenceMode mode = mode(node);
            if (original != null && !spoken.spokenTexts(original, mode).equals(spoken.spokenTexts(quiz,mode)))
                requests.put(key(quiz.getId(),quiz.getAnswerAudioKey()), new AudioRerecordRequest(
                        quiz.getId(),quiz.getAnswerAudioKey(),List.copyOf(spoken.spokenTexts(quiz,mode))));
        }
        var entries = new ArrayList<Entry>();
        for (var node : BatchBook.nodes(report)) if (node.getEntity() instanceof AuditableQuiz quiz
                && selectedQuizzes.contains(quiz.getId())) {
            var texts = List.copyOf(spoken.spokenTexts(quiz,mode(node)));
            var findings = BatchBook.list(node.getFindings()).stream().filter(DataCoherenceStage::isAudioFinding).toList();
            boolean pending = requests.containsKey(key(quiz.getId(),quiz.getAnswerAudioKey()));
            boolean evaluated = node.getMetadata() != null && "PREPUBLICATION".equals(node.getMetadata().get("dataCoherenceStage"))
                    && node.getScores() != null && node.getScores().containsKey("data-coherence")
                    && !BatchBook.list(node.getUnevaluatedBy()).contains("data-coherence")
                    && !BatchBook.list(node.getPartiallyEvaluatedBy()).contains("data-coherence");
            boolean problem = !findings.isEmpty() || !evaluated || texts.isEmpty() || !globalContextComplete;
            // A completed registry check overrides an old request without rewriting its history.
            boolean needsGeneration = pending && problem;
            String status = needsGeneration ? "PENDING_GENERATION" : problem ? "TEXT_RECORD_PROBLEM" : "TEXT_RECORD_MATCHES";
            var reasons = new ArrayList<String>();
            if (needsGeneration) reasons.add("El texto hablado cambió o hay una regrabación pendiente; preparar el audio con el texto final antes de publicar.");
            for (Finding finding : findings) reasons.add(finding.getEvidence() != null && finding.getEvidence().getObservation() != null
                    ? finding.getEvidence().getObservation() : "El registro de texto señala " + finding.getRule() + ".");
            if (!evaluated) reasons.add("Falta una comprobación completa del registro de texto; no se considera aprobado.");
            if (!globalContextComplete) reasons.add("Faltan ejercicios de otros niveles en esta foto histórica; no se pudo descartar una clave de audio compartida.");
            if (texts.isEmpty()) reasons.add("No se pudo determinar un texto final para preparar el audio.");
            if (!problem) reasons.add("El registro de texto coincide; esto no comprueba el sonido del archivo.");
            entries.add(new Entry(quiz.getId(),quiz.getAnswerAudioKey(),texts,status,List.copyOf(reasons),findings,
                    problem && !texts.isEmpty() ? new AudioRerecordRequest(quiz.getId(),quiz.getAnswerAudioKey(),texts) : null));
        }
        entries.sort(Comparator.comparingInt((Entry e) -> "PENDING_GENERATION".equals(e.status()) ? 0 : "TEXT_RECORD_PROBLEM".equals(e.status()) ? 1 : 2)
                .thenComparing(Entry::quizId));
        int pending = (int) entries.stream().filter(e -> "PENDING_GENERATION".equals(e.status())).count();
        int matching = (int) entries.stream().filter(e -> "TEXT_RECORD_MATCHES".equals(e.status())).count();
        var limitations = new ArrayList<>(List.of(
                "Inspección del registro de texto contra esta versión del contenido; no escucha ni verifica el sonido real.",
                "No genera audios, no cambia aprobaciones, no consume consultas y no publica.",
                "Al cambiar el contenido hay que volver a preparar los audios de esa versión."));
        if (!globalContextComplete) limitations.add("La foto histórica no incluye todo el curso; las claves compartidas entre niveles quedan sin comprobar. No se mezcló contenido actual con el histórico.");
        return new View(batch.getId(),batch.getContentVersion(),fingerprint.apply(current),"PREPUBLICATION",0,
                entries.size(),pending,matching,entries.size()-pending-matching,List.copyOf(entries),List.copyOf(limitations));
    }
    private static Set<String> quizIds(CourseEntity course) {
        var ids = new LinkedHashSet<String>();
        if (course == null || course.getRoot() == null) return ids;
        for (var milestone : BatchBook.list(course.getRoot().getMilestones())) for (var topic : BatchBook.list(milestone.getTopics()))
            for (var knowledge : BatchBook.list(topic.getKnowledges())) for (var quiz : BatchBook.list(knowledge.getQuizTemplates()))
                ids.add(quiz.getId());
        return ids;
    }
    private static String key(String id,String audio) { return id + "\u0000" + audio; }
    private static SentenceMode mode(AuditNode node) {
        return node.ancestor(AuditTarget.KNOWLEDGE).map(AuditNode::getEntity).filter(AuditableKnowledge.class::isInstance)
                .map(AuditableKnowledge.class::cast).map(AuditableKnowledge::getSentenceMode).orElse(SentenceMode.FILL);
    }
    private static Map<String, AuditableQuiz> mapped(AuditableCourse course) {
        var result = new LinkedHashMap<String, AuditableQuiz>();
        for (var milestone : BatchBook.list(course.getMilestones())) for (var topic : BatchBook.list(milestone.getTopics()))
            for (var knowledge : BatchBook.list(topic.getKnowledge())) for (var quiz : BatchBook.list(knowledge.getQuizzes()))
                result.put(quiz.getId(),quiz);
        return result;
    }
}
