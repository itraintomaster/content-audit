package com.learney.contentaudit.auditdomain.singleanswerengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableCourse;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableMilestone;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.AuditableTopic;
import com.learney.contentaudit.auditdomain.CorrectorMatcher;
import com.learney.contentaudit.auditdomain.DefaultCourseDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultQuizDiagnoses;
import com.learney.contentaudit.auditdomain.EvaluationRunPolicy;
import com.learney.contentaudit.auditdomain.IAuditEngine;
import com.learney.contentaudit.auditdomain.IScoreAggregator;
import com.learney.contentaudit.auditdomain.JudgeExamRegistry;
import com.learney.contentaudit.auditdomain.JudgeRuntimeConfig;
import com.learney.contentaudit.auditdomain.SingleAnswerConfig;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.contextnumbers.AnalyzerErrorCounts;
import com.learney.contentaudit.auditdomain.contextnumbers.AnalyzerFindingCounts;
import com.learney.contentaudit.auditdomain.contextnumbers.ErrorCounts;
import com.learney.contentaudit.auditdomain.contextnumbers.RuleFindingCount;
import com.learney.contentaudit.auditdomain.correctormatch.GapContext;
import com.learney.contentaudit.auditdomain.finding.EvidencePart;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.auditdomain.findingengine.DefaultAnalyzerCatalog;
import com.learney.contentaudit.auditdomain.findingengine.DefaultContextNumbersCalculator;
import com.learney.contentaudit.auditdomain.findingengine.DefaultFindingCollector;
import com.learney.contentaudit.auditdomain.judging.JudgeCoverageDiagnosis;
import com.learney.contentaudit.auditdomain.judging.JudgeExamRecord;
import com.learney.contentaudit.auditdomain.judging.JudgeExamStatus;
import com.learney.contentaudit.auditdomain.judgingengine.DefaultJudgeRuntime;
import com.learney.contentaudit.auditdomain.singleanswer.AlternativeAnswer;
import com.learney.contentaudit.auditdomain.singleanswer.AlternativeAnswerKind;
import com.learney.contentaudit.auditdomain.singleanswer.SingleAnswerDiagnosis;
import com.learney.contentaudit.auditdomain.singleanswer.SingleAnswerVerdict;
import com.learney.contentaudit.auditdomain.singleanswer.SingleAnswerVerdictReader;
import com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer;
import com.learney.contentaudit.coursedomain.SentenceMode;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.evaluationledgerdomain.ContentFingerprinter;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationEmitted;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationFailureKind;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationKey;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationLedger;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationNotEmitted;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationOutcome;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRecord;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSubject;
import com.learney.contentaudit.evaluationledgerdomain.Evaluator;
import com.learney.contentaudit.evaluationledgerdomain.contentfingerprint.Sha256ContentFingerprinter;
import com.learney.contentaudit.evaluationledgerdomain.evaluationsession.DefaultEvaluationSessionFactory;
import com.learney.contentaudit.evaluationledgerdomain.evaluationsession.ExamEvaluationSessionFactory;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.processing.Generated;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@Generated(
        value = "com.sentinel.SentinelEngine",
        comments = "Generated by Sentinel — journey test stubs"
)
@Tag("F-UNICA")
@Tag("F-UNICA-J001")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FUnicaJ001JourneyTest {

    private static final String NAME = "single-answer";

    private static final String JUDGE_ID = "single-answer-judge";

    private static final String VERSION = "single-answer-judge@guia-1";

    @Test
    @Order(1)
    @Tag("path-1")
    @DisplayName("path-1: El operador corre analyze pidiendo si... → El veredicto lista las otras respuest... [El ejercicio ya tiene veredicto para ese contenido, o se lo consulta dentro del tope] → El ejercicio no tiene hallazgo de sin... [La lista está vacía, o sólo tenía formas que el corrector acepta] → success")
    public void path1_elEjercicioYaTieneVeredictoParaEseContenidoOSeLoConsultaDentroDelTope_laListaEstVacaOSloTenaFormasQueElCorrectorAcepta_success(
            ) {
        Operator operator = new Operator();
        // «He ___ (not / be) at home.» accepts «isn't»; the judge lists only «is not», which the corrector takes.
        AuditableQuiz atHome = quiz("q-at-home", "Negativas con be", "Escribe la forma negativa.",
                text("He"), cloze("isn't"), text("(not / be) at home."));
        operator.judgeAnswers(atHome, verdict(alt("is not", AlternativeAnswerKind.DIFFERENT_VALID, FindingSeverity.HIGH,
                "La forma larga también vale.")));
        // The panel's candidate for «Does he need water?»: the judge lists nothing.
        AuditableQuiz yesHe = quiz("67fab6d59930102295341f57", "Respuestas cortas",
                "Usa $short forms$ si es posible. Escribe la respuesta corta.",
                text("Does he need water? Yes, he"), cloze("does"), text("."));
        operator.judgeAnswers(yesHe, verdict());
        // The first one already has a verdict for this content, from an earlier run.
        operator.analyze(budget(1), atHome);
        assertEquals(1, operator.judge.calls());

        AuditReport report = operator.analyze(budget(1), atHome, yesHe);

        assertEquals(2, operator.judge.calls(), "the recorded one is reused; the other is consulted within the budget");
        for (AuditableQuiz quiz : List.of(atHome, yesHe)) {
            AuditNode node = node(report, quiz);
            assertEquals(1.0, node.getScores().get(NAME), 1e-9, "it passes: " + quiz.getId());
            assertTrue(findingsOf(node).isEmpty(), "no single-answer finding: " + quiz.getId());
            assertFalse(node.getUnevaluatedBy().contains(NAME));
        }
        SingleAnswerDiagnosis atHomeDiagnosis = diagnosis(node(report, atHome));
        assertTrue(atHomeDiagnosis.isReusedFromPreviousRun());
        assertEquals(List.of("is not"), atHomeDiagnosis.getDroppedAsAccepted(), "the form the corrector accepts");
        assertEquals(List.of(), diagnosis(node(report, yesHe)).getCounted());
        AnalyzerErrorCounts course = errorsOf(report.getRoot());
        assertEquals(2, course.getEvaluated());
        assertEquals(0, course.getWithError());
        assertEquals(0, course.getMarked());
    }

    @Test
    @Order(2)
    @Tag("path-2")
    @DisplayName("path-2: El operador corre analyze pidiendo si... → El veredicto lista las otras respuest... [El ejercicio ya tiene veredicto para ese contenido, o se lo consulta dentro del tope] → El ejercicio queda marcado para respu... [Sólo hay opciones que ofrece la consigna] → success")
    public void path2_elEjercicioYaTieneVeredictoParaEseContenidoOSeLoConsultaDentroDelTope_sloHayOpcionesQueOfreceLaConsigna_success(
            ) {
        Operator operator = new Operator();
        AuditableQuiz italy = quiz("67fab6d59930102295342464", "Rangos temporales",
                "Crea oraciones usando 'from' ... 'to / until / till'.",
                text("He went to Italy on Friday and came back on Sunday.He was in Italy"),
                cloze("from Friday to Sunday|until Sunday|till Sunday"), text("."));
        operator.judgeAnswers(italy, verdict(
                alt("from Friday until Sunday", AlternativeAnswerKind.OFFERED_BY_INSTRUCTION, FindingSeverity.HIGH,
                        "La consigna ofrece «to / until / till»."),
                alt("from Friday till Sunday", AlternativeAnswerKind.OFFERED_BY_INSTRUCTION, FindingSeverity.HIGH,
                        "La consigna ofrece «to / until / till».")));

        AuditReport report = operator.analyze(budget(1), italy);

        AuditNode node = node(report, italy);
        assertEquals(1, operator.judge.calls(), "consulted within the budget");
        assertEquals(1.0, node.getScores().get(NAME), 1e-9, "not an error of single-answer");
        List<Finding> findings = findingsOf(node);
        assertEquals(List.of("from Friday till Sunday", "from Friday until Sunday"),
                findings.stream().map(f -> f.getIdentity().getMarker()).collect(Collectors.toList()));
        for (Finding finding : findings) {
            assertEquals("offered-alternative", finding.getRule());
            assertEquals(FindingResolution.RANK_ONLY, finding.getResolution(), "marked for answer-completeness");
            assertTrue(finding.getEvidence().getObservation().contains("answer-completeness"),
                    finding.getEvidence().getObservation());
            assertTrue(finding.getEvidence().getExamined().contains(new EvidencePart("Ejercicio",
                    "He went to Italy on Friday and came back on Sunday.He was in Italy ___ .")));
        }
        // Marked, never an error: in its topic, level and course.
        for (AuditNode context : contextsOf(node)) {
            AnalyzerErrorCounts errors = errorsOf(context);
            assertEquals(0, errors.getWithError(), context.getTarget().name());
            assertEquals(1, errors.getMarked(), context.getTarget().name());
            assertEquals(0, context.getNumbers().getErrors().getWithAnyError(), context.getTarget().name());
        }
        RuleFindingCount offered = ruleCount(report.getRoot(), "offered-alternative");
        assertEquals(1, offered.getWithFinding());
        assertEquals(2, offered.getFindings());
    }

    @Test
    @Order(3)
    @Tag("path-3")
    @DisplayName("path-3: El operador corre analyze pidiendo si... → El veredicto lista las otras respuest... [El ejercicio ya tiene veredicto para ese contenido, o se lo consulta dentro del tope] → El ejercicio tiene su hallazgo con gr... [Hay al menos una distinta y válida] → success")
    public void path3_elEjercicioYaTieneVeredictoParaEseContenidoOSeLoConsultaDentroDelTope_hayAlMenosUnaDistintaYVlida_success(
            ) {
        Operator operator = new Operator();
        AuditableQuiz doesHe = quiz("67fab6d59930102295341f57", "Respuestas cortas",
                "Usa $short forms$ si es posible. Escribe la respuesta corta.",
                text("Does he need water? He"), cloze("does"), text("."));
        operator.judgeAnswers(doesHe, verdict(alt("doesn't", AlternativeAnswerKind.DIFFERENT_VALID,
                FindingSeverity.HIGH, "Sólo la traducción, que se ve después, dice cuál.")));

        AuditReport report = operator.analyze(budget(1), doesHe);

        AuditNode node = node(report, doesHe);
        assertEquals(0.3, node.getScores().get(NAME), 1e-9);
        List<Finding> findings = findingsOf(node);
        assertEquals(1, findings.size());
        Finding finding = findings.get(0);
        assertEquals("different-valid-answer", finding.getRule());
        assertEquals("doesn't", finding.getIdentity().getMarker());
        assertEquals(FindingSeverity.HIGH, finding.getSeverity(), "with its severity");
        assertEquals(FindingResolution.PANEL, finding.getResolution());
        assertTrue(finding.getEvidence().getExamined().contains(new EvidencePart("Ejercicio",
                "Does he need water? He ___ .")), "and its evidence, as the student sees it");
        // It counts as an error in its topic, its level and the course, with its severity.
        for (AuditNode context : contextsOf(node)) {
            ErrorCounts errors = context.getNumbers().getErrors();
            assertEquals(1, errors.getWithAnyError(), context.getTarget().name());
            assertEquals(1, errors.getBySeverity().getHigh(), context.getTarget().name());
            assertEquals(1, errorsOf(context).getWithError(), context.getTarget().name());
        }
        assertNull(report.getRoot().getNumbers().getVocabularyScore(), "a judge never moves the vocabulary score");
    }

    @Test
    @Order(4)
    @Tag("path-4")
    @DisplayName("path-4: El operador corre analyze pidiendo si... → El ejercicio queda sin evaluar, sin p... [No tiene veredicto y no queda tope] → success")
    public void path4_noTieneVeredictoYNoQuedaTope_success() {
        Operator operator = new Operator();
        AuditableQuiz verduras = quiz("67fab6d59930102295341ecd", "Afirmativas regulares",
                "Escribe la forma afirmativa en presente simple.",
                text("I"), cloze("love"), text("a lot of vegetables."));
        AuditableQuiz sheWontCome = quiz("67fab6d59930102295342189", "Elegir la forma natural",
                "¿Es más natural usar 'will' o 'going to'? Escribe oraciones.",
                text("She"), cloze("isn't going to come|is not going to come|'s not going to come"),
                text("(not / come)."));
        operator.judgeAnswers(verduras, verdict(alt("eat", AlternativeAnswerKind.DIFFERENT_VALID,
                FindingSeverity.BLOCKING, "Encaja igual y no hay pista.")));
        operator.judgeAnswers(sheWontCome, verdict(alt("won't come", AlternativeAnswerKind.DIFFERENT_VALID,
                FindingSeverity.MEDIUM, "will también es natural.")));

        // A budget of one: the first exercise takes it, the second has no verdict and no budget left.
        AuditReport report = operator.analyze(budget(1), verduras, sheWontCome);

        assertEquals(List.of(verduras.getId()), operator.judge.asked());
        AuditNode left = node(report, sheWontCome);
        assertFalse(left.getScores().containsKey(NAME), "without score");
        assertTrue(findingsOf(left).isEmpty(), "without finding");
        assertTrue(left.getUnevaluatedBy().contains(NAME), "declared not evaluated");
        assertTrue(((DefaultQuizDiagnoses) left.getDiagnoses()).getSingleAnswerDiagnosis().isEmpty());

        JudgeCoverageDiagnosis coverage = coverageOf(report);
        assertEquals(2, coverage.getReached());
        assertEquals(1, coverage.getEvaluated());
        assertEquals(1, coverage.getNotEvaluated(), "the coverage counts it");
        assertEquals(1, coverage.getCost().getPendingUnits(), "and what it would cost");
        for (AuditNode context : contextsOf(left)) {
            AnalyzerErrorCounts errors = errorsOf(context);
            // Its own topic holds only it; from the topic of the course up, the verduras too.
            int below = context.getTarget() == AuditTarget.KNOWLEDGE ? 1 : 2;
            assertEquals(below, errors.getReached(), context.getTarget().name());
            assertEquals(below - 1, errors.getEvaluated(), context.getTarget().name());
            assertEquals(1, errors.getNotEvaluated(), context.getTarget().name());
            assertEquals(1, context.getNumbers().getErrors().getNotFullyEvaluated(), context.getTarget().name());
        }
    }

    // -------------------------------------------------------------------------
    // The operator: analyze with only single-answer in the catalog, a version that passed its exam
    // -------------------------------------------------------------------------

    private static final class Operator {
        final MemoryLedger ledger = new MemoryLedger();
        final RecordedJudge judge = new RecordedJudge();
        final RecordedVerdicts verdicts = new RecordedVerdicts();

        void judgeAnswers(AuditableQuiz quiz, SingleAnswerVerdict verdict) {
            judge.emits(quiz, verdicts.payloadOf(verdict));
        }

        AuditReport analyze(EvaluationRunPolicy policy, AuditableQuiz... quizzes) {
            ContentFingerprinter fp = new Sha256ContentFingerprinter();
            DefaultJudgeRuntime runtime = new DefaultJudgeRuntime(new DefaultEvaluationSessionFactory(ledger, fp),
                    new ExamEvaluationSessionFactory(ledger, fp), new Registry(), evaluatorId -> List.of(),
                    new RuntimeConfig());
            SingleAnswerAnalyzerProvider provider = new SingleAnswerAnalyzerProvider(runtime, judge, verdicts,
                    new Scale(), new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create()),
                    new FakeCorrector(List.of("isn't", "is not"), List.of("until", "till")));
            IAuditEngine engine = new IAuditEngine(new IScoreAggregator(),
                    new DefaultAnalyzerCatalog(List.of(provider)), new DefaultFindingCollector(),
                    new DefaultContextNumbersCalculator());
            return engine.runAudit(course(quizzes),
                    new AnalyzerRunSelection(List.of(NAME), Map.of(NAME, policy)));
        }
    }

    /** One knowledge per exercise, in one topic of A1. */
    private static AuditableCourse course(AuditableQuiz... quizzes) {
        List<AuditableKnowledge> knowledge = new ArrayList<>();
        for (AuditableQuiz quiz : quizzes) {
            knowledge.add(new AuditableKnowledge(List.of(quiz), quiz.getLabel(), "La consigna del tema.", false,
                    "k-" + quiz.getId(), quiz.getLabel(), "", SentenceMode.FILL, "Present Simple"));
        }
        AuditableTopic topic = new AuditableTopic(knowledge, "t-present-simple", "Present Simple", "");
        AuditableMilestone a1 = new AuditableMilestone(List.of(topic), "m-a1", "A1", "");
        return new AuditableCourse(List.of(a1));
    }

    private static AuditNode node(AuditReport report, AuditableQuiz quiz) {
        Deque<AuditNode> pending = new ArrayDeque<>();
        pending.push(report.getRoot());
        while (!pending.isEmpty()) {
            AuditNode node = pending.pop();
            if (node.getTarget() == AuditTarget.QUIZ && quiz.getId().equals(node.getEntity().getId())) {
                return node;
            }
            if (node.getChildren() != null) {
                node.getChildren().forEach(pending::push);
            }
        }
        throw new AssertionError("no node for " + quiz.getId());
    }

    /** Its topic (knowledge), its topic of the course, its level and the course. */
    private static List<AuditNode> contextsOf(AuditNode quiz) {
        List<AuditNode> contexts = new ArrayList<>();
        for (AuditNode current = quiz.getParent(); current != null; current = current.getParent()) {
            contexts.add(current);
        }
        assertEquals(List.of(AuditTarget.KNOWLEDGE, AuditTarget.TOPIC, AuditTarget.MILESTONE, AuditTarget.COURSE),
                contexts.stream().map(AuditNode::getTarget).collect(Collectors.toList()));
        return contexts;
    }

    private static List<Finding> findingsOf(AuditNode node) {
        return node.getFindings().stream().filter(f -> NAME.equals(f.getAnalyzer())).collect(Collectors.toList());
    }

    private static SingleAnswerDiagnosis diagnosis(AuditNode node) {
        return ((DefaultQuizDiagnoses) node.getDiagnoses()).getSingleAnswerDiagnosis().orElseThrow();
    }

    private static AnalyzerErrorCounts errorsOf(AuditNode context) {
        return context.getNumbers().getErrors().getAnalyzers().stream()
                .filter(a -> NAME.equals(a.getAnalyzer())).findFirst().orElseThrow();
    }

    private static RuleFindingCount ruleCount(AuditNode context, String rule) {
        AnalyzerFindingCounts counts = context.getNumbers().getFindingCounts().stream()
                .filter(c -> NAME.equals(c.getAnalyzer()) && c.getTarget() == AuditTarget.QUIZ).findFirst()
                .orElseThrow();
        return counts.getByRule().stream().filter(r -> rule.equals(r.getRule())).findFirst().orElseThrow();
    }

    private static JudgeCoverageDiagnosis coverageOf(AuditReport report) {
        return ((DefaultCourseDiagnoses) report.getRoot().getDiagnoses()).getJudgeCoverages().stream()
                .filter(c -> NAME.equals(c.getJudgeName())).findFirst().orElseThrow();
    }

    private static EvaluationRunPolicy budget(int maxNewEvaluations) {
        return new EvaluationRunPolicy(maxNewEvaluations, false, null, null, null, null);
    }

    // -------------------------------------------------------------------------
    // Doubles: no paid consultation ever
    // -------------------------------------------------------------------------

    private static final class MemoryLedger implements EvaluationLedger {
        private final List<EvaluationRecord> records = new ArrayList<>();

        @Override
        public synchronized void append(EvaluationRecord record) {
            records.add(record);
        }

        @Override
        public synchronized Optional<EvaluationRecord> findLatest(EvaluationKey key) {
            List<EvaluationRecord> matching = history(key);
            return matching.isEmpty() ? Optional.empty() : Optional.of(matching.get(matching.size() - 1));
        }

        @Override
        public synchronized List<EvaluationRecord> history(EvaluationKey key) {
            return records.stream().filter(r -> r.getKey().equals(key)).collect(Collectors.toList());
        }
    }

    /** The version of the judge passed its exam. */
    private static final class Registry implements JudgeExamRegistry {
        @Override
        public void record(JudgeExamRecord record) {
        }

        @Override
        public Optional<JudgeExamRecord> latest(String judgeName, String judgeVersion) {
            if (!NAME.equals(judgeName) || !VERSION.equals(judgeVersion)) {
                return Optional.empty();
            }
            return Optional.of(new JudgeExamRecord(judgeName, judgeVersion, JudgeExamStatus.APPROVED, 39, 0,
                    List.of(), List.of(), true, "2026-09-29", "prueba", Instant.parse("2026-09-30T12:00:00Z")));
        }

        @Override
        public List<JudgeExamRecord> history(String judgeName) {
            return List.of();
        }
    }

    private static final class RuntimeConfig implements JudgeRuntimeConfig {
        @Override
        public int getMaxConcurrentEvaluations() {
            return 4;
        }

        @Override
        public int getDefaultMaxNewEvaluations(String judgeName) {
            return 0;
        }

        @Override
        public double getDefaultRatePerHour(String judgeName) {
            return 189.0;
        }

        @Override
        public Map<String, Object> describe() {
            return Map.of("maxConcurrentEvaluations", 4);
        }
    }

    private static final class Scale implements SingleAnswerConfig {
        @Override
        public double getScoreFor(FindingSeverity severity) {
            return switch (severity) {
                case BLOCKING -> 0.0;
                case HIGH -> 0.3;
                case MEDIUM, LOW -> 0.6;
            };
        }

        @Override
        public Map<String, Object> describe() {
            return Map.of("scale", "0 / 0,3 / 0,6");
        }
    }

    /**
     * The corrector of package A is not built in this branch: two answers are the same once every
     * form of a group is read as the first one, word by word.
     */
    private static final class FakeCorrector implements CorrectorMatcher {
        private final List<List<String>> groups;

        @SafeVarargs
        FakeCorrector(List<String>... groups) {
            this.groups = List.of(groups);
        }

        private String canonical(String text) {
            String result = text.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
            for (List<String> group : groups) {
                for (String form : group.subList(1, group.size())) {
                    result = result.replaceAll("(?<![\\w'])" + java.util.regex.Pattern.quote(form) + "(?![\\w'])",
                            java.util.regex.Matcher.quoteReplacement(group.get(0)));
                }
            }
            return result;
        }

        @Override
        public Set<String> canonicalForms(String text) {
            return new LinkedHashSet<>(List.of(canonical(text)));
        }

        @Override
        public boolean accepts(GapContext context, List<String> acceptedOptions, String written) {
            String target = canonical(written);
            return acceptedOptions.stream().map(this::canonical).anyMatch(target::equals);
        }

        @Override
        public boolean sameAnswer(String first, String second) {
            return canonical(first).equals(canonical(second));
        }

        @Override
        public boolean sameAnswer(GapContext context, String first, String second) {
            return sameAnswer(first, second);
        }

        @Override
        public String tableVersion() {
            return "doble-de-prueba";
        }
    }

    /** The judge answers what the test recorded for the exercise the student sees, and counts its calls. */
    private static final class RecordedJudge implements Evaluator {
        private final Map<String, EvaluationOutcome> byExercise = new ConcurrentHashMap<>();
        private final List<String> asked = Collections.synchronizedList(new ArrayList<>());

        @Override
        public String evaluatorId() {
            return JUDGE_ID;
        }

        @Override
        public EvaluationOutcome evaluate(EvaluationSubject subject) {
            asked.add(subject.getSubjectRef());
            return byExercise.getOrDefault(subject.getContent().get("exercise"),
                    new EvaluationNotEmitted(EvaluationFailureKind.OUTPUT_UNUSABLE, "sin veredicto grabado"));
        }

        @Override
        public Optional<String> evaluatorVersion() {
            return Optional.of(VERSION);
        }

        void emits(AuditableQuiz quiz, String payload) {
            String statement = new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create()).render(quiz)
                    .getStatement();
            byExercise.put(statement, new EvaluationEmitted(payload));
        }

        List<String> asked() {
            synchronized (asked) {
                return new ArrayList<>(asked);
            }
        }

        int calls() {
            return asked.size();
        }
    }

    /** The reader of the adapters is built elsewhere: each payload is the verdict the test built. */
    private static final class RecordedVerdicts implements SingleAnswerVerdictReader {
        private final Map<String, SingleAnswerVerdict> byPayload = new HashMap<>();

        synchronized String payloadOf(SingleAnswerVerdict verdict) {
            StringBuilder sb = new StringBuilder("{\"alternatives\": [");
            for (int i = 0; i < verdict.getAlternatives().size(); i++) {
                AlternativeAnswer a = verdict.getAlternatives().get(i);
                sb.append(i > 0 ? ", " : "").append("{\"answer\": ").append(quoted(a.getAnswer()))
                        .append(", \"gap\": ").append(a.getGap())
                        .append(", \"kind\": ").append(quoted(a.getKind().name()))
                        .append(", \"severity\": ").append(quoted(a.getSeverity().name()))
                        .append(", \"reason\": ").append(quoted(a.getReason())).append('}');
            }
            String payload = sb.append("], \"rationale\": ").append(quoted(verdict.getRationale())).append('}')
                    .toString();
            byPayload.put(payload, verdict);
            return payload;
        }

        @Override
        public synchronized SingleAnswerVerdict read(String payload) {
            SingleAnswerVerdict verdict = byPayload.get(payload);
            if (verdict == null) {
                throw new IllegalArgumentException("veredicto ilegible: " + payload);
            }
            return verdict;
        }

        private static String quoted(String value) {
            return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
    }

    // -------------------------------------------------------------------------
    // Exercises
    // -------------------------------------------------------------------------

    private static AuditableQuiz quiz(String id, String title, String instructions, SentencePartEntity... parts) {
        return new AuditableQuiz(List.of(), id, title, "", "La traducción se ve después.", List.of(), null,
                instructions, List.of(parts), null, "La miniteoría se ve después.", "A1.00.00.00");
    }

    private static SentencePartEntity text(String text) {
        return new SentencePartEntity(SentencePartKind.TEXT, text, null);
    }

    private static SentencePartEntity cloze(String options) {
        return new SentencePartEntity(SentencePartKind.CLOZE, "", List.of(options));
    }

    private static AlternativeAnswer alt(String answer, AlternativeAnswerKind kind, FindingSeverity severity,
            String reason) {
        return new AlternativeAnswer(answer, 1, kind, severity, reason);
    }

    private static SingleAnswerVerdict verdict(AlternativeAnswer... alternatives) {
        return new SingleAnswerVerdict(List.of(alternatives), "Lo que el alumno ve antes de contestar.");
    }
}
