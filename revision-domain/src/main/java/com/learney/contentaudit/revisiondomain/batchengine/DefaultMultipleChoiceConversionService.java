package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.refinerdomain.RefinementPlanStore;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.conversion.*;
import com.learney.contentaudit.revisiondomain.conversion.MultipleChoiceConversion.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/** Adds one indivisible, unjudged proposal; the existing review and budgeted judging flows own decisions. */
final class DefaultMultipleChoiceConversionService implements MultipleChoiceConversionService {
    private final BatchStore batches;
    private final RefinementPlanStore plans;
    private final CourseRepository courses;
    private final Path coursePath;
    private final BatchChangeLedger ledger;

    DefaultMultipleChoiceConversionService(BatchStore batches, RefinementPlanStore plans,
            CourseRepository courses, Path coursePath, BatchChangeLedger ledger) {
        this.batches = batches; this.plans = plans; this.courses = courses; this.coursePath = coursePath; this.ledger = ledger;
    }
    @Override public State execute(Request request) {
        if (request == null || !List.of("read", "suggest", "restore").contains(Objects.toString(request.action(), "")))
            throw new IllegalArgumentException("Pedí read, suggest o restore.");
        CorrectionBatch batch = batches.load(request.batchId()).orElseThrow(() -> new IllegalArgumentException("No existe la tanda."));
        CourseEntity start = courses.load(coursePath);
        CourseEntity current = ledger.composedCourse(batch, start);
        if (request.action().equals("read")) return state(batch, current, request.quizId());
        if (batch.getStatus() != BatchStatus.OPEN && batch.getStatus() != BatchStatus.CLOSED)
            throw new IllegalArgumentException("La tanda está publicada o tiene otra operación activa.");
        if (request.contentVersion() == null || request.contentVersion() != batch.getContentVersion())
            throw new IllegalArgumentException("El contenido cambió: volvé a leer antes de pedir la conversión.");
        if (request.reviewer() == null || request.reviewer().isBlank()) throw new IllegalArgumentException("Falta el autor del pedido.");
        BatchBook.Place place = BatchBook.place(current, request.quizId()).filter(p -> p.quiz() != null)
                .orElseThrow(() -> new IllegalArgumentException("No existe el ejercicio."));
        if (!inLevel(batch, place)) throw new IllegalArgumentException("El ejercicio pertenece a otro nivel.");
        QuizTemplateEntity after;
        String source;
        if (request.action().equals("restore")) {
            if (place.quiz().getClozeOriginal() == null || QuizTemplateEntities.formKind(place.quiz()) != FormKind.MULTIPLE_CHOICE)
                throw new IllegalArgumentException("No hay un cloze guardado para restaurar.");
            // Before publication, undo the decision through review. Restoration here creates a new correction after publication.
            if (!BatchBook.stepsOn(batch, request.quizId()).isEmpty())
                throw new IllegalArgumentException("Esta conversión aún está en la tanda: cambiala a rechazada en la revisión.");
            var unchanged = BatchBook.copy(place.quiz());
            unchanged.setKind("CLOZE");
            unchanged.setForm(BatchBook.copy(place.quiz().getClozeOriginal()).getForm());
            unchanged.setInstructions(place.quiz().getClozeOriginal().getInstructions());
            unchanged.setClozeOriginal(null);
            if (!unchanged.equals(place.quiz().getClozeOriginal()))
                throw new IllegalArgumentException("Otro atributo cambió desde la conversión; revisalo antes de restaurar.");
            if (blockedQuizzes(batch).contains(request.quizId()))
                throw new IllegalArgumentException("Tiene otra corrección pendiente; restaurá en la siguiente tanda.");
            after = BatchBook.copy(place.quiz().getClozeOriginal());
            after.setClozeOriginal(place.quiz().getClozeOriginal());
            source = "RESTORE";
        } else {
            Candidate candidate = candidate(batch, place, true);
            if (!candidate.eligible()) throw new IllegalArgumentException(candidate.reason());
            after = candidate.after(); source = candidate.source();
        }
        BatchStatus previous = batch.getStatus();
        int version = batch.getContentVersion();
        batch.setStatus(BatchStatus.RUNNING);
        batches.saveIfVersion(batch, version, previous);
        batch.setContentVersion(version + 1);
        try {
            record(batch, place, after, source, start);
        } finally {
            // The previous closing is stale. No judge or course write happens here.
            batch.setStatus(BatchStatus.OPEN);
            batches.save(batch);
        }
        return state(batch, ledger.composedCourse(batch, start), request.quizId());
    }

    State state(CorrectionBatch batch, CourseEntity current, String quizId) {
        List<Candidate> candidates = new ArrayList<>();
        Set<String> blocked = blockedQuizzes(batch), rejected = rejectedQuizzes();
        if (current.getRoot() != null) for (MilestoneEntity milestone : BatchBook.list(current.getRoot().getMilestones()))
            for (TopicEntity topic : BatchBook.list(milestone.getTopics()))
                for (KnowledgeEntity knowledge : BatchBook.list(topic.getKnowledges()))
                    for (QuizTemplateEntity quiz : BatchBook.list(knowledge.getQuizTemplates())) {
                        if (quiz == null || quizId != null && !quizId.equals(quiz.getId())) continue;
                        BatchBook.Place place = new BatchBook.Place(milestone, topic, knowledge, quiz);
                        if (!inLevel(batch, place)) continue;
                        Candidate candidate = candidate(batch, place, quizId != null, blocked, rejected);
                        if (quizId != null || candidate.eligible()) candidates.add(candidate);
                    }
        return new State(batch.getId(), batch.getContentVersion(), candidates);
    }

    void suggestAll(CorrectionBatch batch, CourseEntity start) {
        State state = state(batch, ledger.composedCourse(batch, start), null);
        for (Candidate candidate : state.candidates()) {
            BatchBook.Place place = BatchBook.place(start, candidate.quizId()).orElseThrow();
            record(batch, place, candidate.after(), candidate.source(), start);
            batches.save(batch);
        }
    }

    private Candidate candidate(CorrectionBatch batch, BatchBook.Place place, boolean manual) {
        return candidate(batch, place, manual, blockedQuizzes(batch), rejectedQuizzes());
    }

    private Candidate candidate(CorrectionBatch batch, BatchBook.Place place, boolean manual, Set<String> blocked, Set<String> rejected) {
        Candidate candidate = ExistingChoiceConversion.inspect(place, manual);
        String reason = candidate.reason();
        if (reason == null && !BatchBook.stepsOn(batch, place.quiz().getId()).isEmpty()) reason = "Ya tiene otra corrección en esta tanda; volvé a pedirla en la siguiente.";
        if (reason == null && blocked.contains(place.quiz().getId()))
            reason = "Tiene una corrección pendiente; primero resolvela y pedí la conversión en la siguiente tanda.";
        if (reason == null && BatchBook.list(batch.getChanges()).stream().anyMatch(c ->
                (MultipleChoiceConversion.RULE + "-" + place.quiz().getId()).equals(c.getChangeId())))
            reason = "Esta conversión ya está registrada en la tanda.";
        if (reason == null && rejected.contains(place.quiz().getId())) reason = "Esta conversión ya fue rechazada; no se vuelve a sugerir.";
        return reason == null ? candidate : new Candidate(candidate.quizId(), candidate.knowledgeId(), candidate.topic(),
                candidate.level(), candidate.source(), candidate.topicTotal(), candidate.multipleChoiceCount(), candidate.options(), reason, candidate.before(), null);
    }

    private Set<String> blockedQuizzes(CorrectionBatch batch) {
        Set<String> blocked = new HashSet<>();
        plans.load(batch.getPlanId()).ifPresent(plan -> BatchBook.list(plan.getTasks()).stream()
                .filter(t -> BatchBook.list(batch.getTaskIds()).contains(t.getId()))
                .forEach(t -> blocked.addAll(BatchBook.quizzesOf(t))));
        return blocked;
    }
    private Set<String> rejectedQuizzes() {
        Set<String> rejected = new HashSet<>();
        for (CorrectionBatch saved : batches.list()) for (BatchReviewDecision review : batches.reviews(saved.getId())) {
            if (review.status() == BatchReviewDecision.Status.REJECTED) for (var selection : review.selections())
                if (selection.changeId().startsWith(MultipleChoiceConversion.RULE + "-")) rejected.add(selection.step().getQuizId());
        }
        return rejected;
    }
    private static boolean inLevel(CorrectionBatch batch, BatchBook.Place place) {
        return batch.getLevel() != null && (batch.getLevel().name().equalsIgnoreCase(place.milestone().getLabel())
                || batch.getLevel().name().equalsIgnoreCase(place.milestone().getCode()));
    }
    private void record(CorrectionBatch batch, BatchBook.Place place, QuizTemplateEntity after, String source, CourseEntity start) {
        List<String> analyzers = BatchBook.copy(batch.getAnalyzers());
        for (String judge : MultipleChoiceConversion.JUDGES) if (!analyzers.contains(judge)) analyzers.add(judge);
        batch.setAnalyzers(analyzers);
        List<ExercisePart> parts = new ArrayList<>(List.of(ExercisePart.FORM_KIND, ExercisePart.OPTIONS,
                ExercisePart.STATEMENT, ExercisePart.ANSWERS, ExercisePart.INSTRUCTIONS, ExercisePart.STORED_SENTENCE));
        if ("RESTORE".equals(source)) parts.addAll(List.of(ExercisePart.TRANSLATION, ExercisePart.MINI_THEORY));
        String id = MultipleChoiceConversion.RULE + "-" + place.quiz().getId();
        if (BatchBook.list(batch.getChanges()).stream().anyMatch(c -> id.equals(c.getChangeId())))
            throw new IllegalArgumentException("Esta conversión ya está registrada en la tanda.");
        ledger.record(batch, new BatchChange(id, BatchChangeOrigin.CONVERSION, MultipleChoiceConversion.RULE + ":" + source,
                place.knowledge().getId(), List.of(), List.of(new BatchChangeStep(place.quiz().getId(), parts,
                        new CourseElementSnapshot(AuditTarget.QUIZ, place.quiz().getId(), after, null))),
                null, false, Instant.now(), null), start);
    }
}
