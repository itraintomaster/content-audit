package com.learney.contentaudit.auditdomain.multiplechoicevalidity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableCourse;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableMilestone;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.AuditableTopic;
import com.learney.contentaudit.auditdomain.ChoiceAlternativeConfig;
import com.learney.contentaudit.auditdomain.CorrectorMatcher;
import com.learney.contentaudit.auditdomain.DefaultCourseDiagnoses;
import com.learney.contentaudit.auditdomain.EvaluationRunPolicy;
import com.learney.contentaudit.auditdomain.IAuditEngine;
import com.learney.contentaudit.auditdomain.IScoreAggregator;
import com.learney.contentaudit.auditdomain.JudgeExamRegistry;
import com.learney.contentaudit.auditdomain.JudgeRuntimeConfig;
import com.learney.contentaudit.auditdomain.QuizStudentViewRenderer;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.choicealternative.ChoiceAlternativeOption;
import com.learney.contentaudit.auditdomain.choicealternative.ChoiceAlternativeVerdict;
import com.learney.contentaudit.auditdomain.choicealternative.ChoiceAlternativeVerdictReader;
import com.learney.contentaudit.auditdomain.choicealternativeengine.DefaultChoiceAlternativeJudgeRunFactory;
import com.learney.contentaudit.auditdomain.contextnumbers.AnalyzerErrorCounts;
import com.learney.contentaudit.auditdomain.correctormatch.GapContext;
import com.learney.contentaudit.auditdomain.finding.AnalysisCost;
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
import com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer;
import com.learney.contentaudit.coursedomain.MultipleChoiceEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceItemEntity;
import com.learney.contentaudit.coursedomain.SentenceMode;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.processing.Generated;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * F-MCVALE-J001: the operator runs analyze asking multiple-choice-validity with a budget, with a
 * version of the judge that approved its exam. The judge is a rule of that analyzer (package A), so
 * the journey runs the real engine with the analyzer of A wired as in Main and the judge of package
 * C, and asserts only what FEAT-MCVALE fixes: the finding of {@code another-option-valid} with its
 * severity, the score 0 of the blocking one and, for an exercise the judge did not see, that it is
 * evaluated halfway, without a finding of the judge, and counted by the judge's coverage.
 */
@Generated(
        value = "com.sentinel.SentinelEngine",
        comments = "Generated by Sentinel — journey test stubs"
)
@Tag("F-MCVALE")
@Tag("F-MCVALE-J001")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FMcvaleJ001JourneyTest {

    private static final String HOST = "multiple-choice-validity";

    private static final String RULE = "another-option-valid";

    private static final String JUDGE_ID = "choice-alternative-judge";

    private static final String VERSION = "v1";

    private static final String BROTHER_ID = "67fab6d599301022953430aa";

    private static final String FOOD_ID = "67fab6d59930102295342f1b";

    private static final String THIRSTY_ID = "67fab6d599301022953447cc";

    private static final String CAT_ID = "67fab6d59930102295342cb8";

    private static final String ONLY_CORRECT_PAYLOAD = "{\"correctIsValid\": true, \"validAlternatives\": [], "
            + "\"correctProblem\": null, \"rationale\": \"Sólo vale «is»\"}";

    private static final String FOOD_PAYLOAD = "{\"correctIsValid\": true, \"validAlternatives\": [{\"label\": "
            + "\"a lot\", \"severity\": \"HIGH\", \"reason\": \"«a lot» al final también es correcto\"}], "
            + "\"correctProblem\": null, \"rationale\": \"Vale «a lot»\"}";

    private static final String THIRSTY_PAYLOAD = "{\"correctIsValid\": false, \"validAlternatives\": [{\"label\": "
            + "\"if so\", \"severity\": \"HIGH\", \"reason\": \"«if so» sigue a «let's drink water soon»\"}], "
            + "\"correctProblem\": \"«if not» contradice el resto\", \"rationale\": \"La clave está invertida\"}";

    @Test
    @Order(1)
    @Tag("path-1")
    @DisplayName("path-1: El operador corre analyze pidiendo el... → El veredicto dice qué incorrectas tam... [El ejercicio ya tiene veredicto para ese contenido, o se lo consulta dentro del tope] → El ejercicio no tiene hallazgo de est... [Sólo vale la correcta] → success")
    @Disabled("Espera el analizador de FEAT-OMVAL (paquete A): se enciende al juntar A y C")
    public void path1_elEjercicioYaTieneVeredictoParaEseContenidoOSeLoConsultaDentroDelTope_sloValeLaCorrecta_success(
            ) {
        Journey journey = new Journey();
        journey.judge.answer(BROTHER_ID, ONLY_CORRECT_PAYLOAD);
        AuditableQuiz brother = mcQuiz(BROTHER_ID, "My brother", "a teacher.", "is", "is", "are", "am");

        AuditReport report = journey.analyze(5, brother);

        AuditNode node = quizNode(report, BROTHER_ID);
        assertEquals(1, journey.judge.calls.get(), "consulted within the budget");
        assertTrue(judgeFindings(node).isEmpty(), "only the correct one counts: no finding of " + RULE);
        assertFalse(node.getPartiallyEvaluatedBy().contains(HOST), "the judge saw it");
        JudgeCoverageDiagnosis coverage = judgeCoverage(report);
        assertEquals(1, coverage.getEvaluated());
        assertEquals(0, coverage.getNotEvaluated());
    }

    @Test
    @Order(2)
    @Tag("path-2")
    @DisplayName("path-2: El operador corre analyze pidiendo el... → El veredicto dice qué incorrectas tam... [El ejercicio ya tiene veredicto para ese contenido, o se lo consulta dentro del tope] → El ejercicio tiene su hallazgo, alta ... [Otra opción también vale] → success")
    @Disabled("Espera el analizador de FEAT-OMVAL (paquete A): se enciende al juntar A y C")
    public void path2_elEjercicioYaTieneVeredictoParaEseContenidoOSeLoConsultaDentroDelTope_otraOpcinTambinVale_success(
            ) {
        Journey journey = new Journey();
        journey.judge.answer(FOOD_ID, FOOD_PAYLOAD);
        AuditableQuiz food = mcQuiz(FOOD_ID, "I don't enjoy extra food", ".", "much", "a lot", "a lot of", "much",
                "really");

        AuditReport report = journey.analyze(5, food);

        AuditNode node = quizNode(report, FOOD_ID);
        List<Finding> findings = judgeFindings(node);
        assertEquals(1, findings.size());
        Finding finding = findings.get(0);
        assertEquals("a lot", finding.getIdentity().getMarker());
        assertEquals(FindingSeverity.HIGH, finding.getSeverity());
        assertEquals(FindingResolution.PANEL, finding.getResolution());
        assertEquals(AnalysisCost.PAID_MODEL, finding.getCost());
        assertFalse(finding.getEvidence().getExamined().isEmpty(), "with evidence");
        // It counts as error on the exercise and in its topic, level and course.
        assertTrue(node.getScores().get(HOST) < 1.0);
        AuditNode knowledge = node.getParent();
        for (AuditNode context : List.of(knowledge, knowledge.getParent(), knowledge.getParent().getParent(),
                report.getRoot())) {
            assertEquals(1, errorsOf(context).getWithError(), "counted as error in " + context.getTarget());
        }
    }

    @Test
    @Order(3)
    @Tag("path-3")
    @DisplayName("path-3: El operador corre analyze pidiendo el... → El veredicto dice qué incorrectas tam... [El ejercicio ya tiene veredicto para ese contenido, o se lo consulta dentro del tope] → El ejercicio tiene su hallazgo bloque... [La correcta no vale y otra sí] → success")
    @Disabled("Espera el analizador de FEAT-OMVAL (paquete A): se enciende al juntar A y C")
    public void path3_elEjercicioYaTieneVeredictoParaEseContenidoOSeLoConsultaDentroDelTope_laCorrectaNoValeYOtraS_success(
            ) {
        Journey journey = new Journey();
        journey.judge.answer(THIRSTY_ID, THIRSTY_PAYLOAD);
        AuditableQuiz thirsty = mcQuiz(THIRSTY_ID, "Are you thirsty?", ", let's drink water soon.", "if not",
                "if not", "if so");

        AuditReport report = journey.analyze(5, thirsty);

        AuditNode node = quizNode(report, THIRSTY_ID);
        Finding blocking = judgeFindings(node).stream()
                .filter(finding -> "(correcta)".equals(finding.getIdentity().getMarker()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no blocking finding marked «(correcta)»"));
        assertEquals(FindingSeverity.BLOCKING, blocking.getSeverity());
        assertEquals(AnalysisCost.PAID_MODEL, blocking.getCost());
        assertEquals(0.0, node.getScores().get(HOST), 1e-9, "the blocking one scores 0");
        assertEquals(1, errorsOf(report.getRoot()).getWithError(), "it counts as error");
    }

    @Test
    @Order(4)
    @Tag("path-4")
    @DisplayName("path-4: El operador corre analyze pidiendo el... → El ejercicio queda sin evaluar por es... [No tiene veredicto y no queda tope] → success")
    @Disabled("Espera el analizador de FEAT-OMVAL (paquete A): se enciende al juntar A y C")
    public void path4_noTieneVeredictoYNoQuedaTope_success() {
        Journey journey = new Journey();
        AuditableQuiz cat = mcQuiz(CAT_ID, "There's a cat", "the window.", "at", "at", "in", "on");

        AuditReport report = journey.analyze(0, cat);

        AuditNode node = quizNode(report, CAT_ID);
        assertEquals(0, journey.judge.calls.get(), "no budget left: nothing is consulted");
        assertTrue(judgeFindings(node).isEmpty(), "without a finding of the judge");
        assertTrue(node.getPartiallyEvaluatedBy().contains(HOST), "evaluated halfway: the judge did not see it");
        assertFalse(node.getUnevaluatedBy().contains(HOST), "its rules did run");
        JudgeCoverageDiagnosis coverage = judgeCoverage(report);
        assertEquals(1, coverage.getReached());
        assertEquals(0, coverage.getEvaluated());
        assertEquals(1, coverage.getNotEvaluated(), "the coverage counts it");
        assertEquals(1, errorsOf(report.getRoot()).getPartiallyEvaluated());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static List<Finding> judgeFindings(AuditNode node) {
        List<Finding> findings = new ArrayList<>();
        for (Finding finding : node.getFindings()) {
            if (HOST.equals(finding.getAnalyzer()) && RULE.equals(finding.getRule())) {
                findings.add(finding);
            }
        }
        return findings;
    }

    private static JudgeCoverageDiagnosis judgeCoverage(AuditReport report) {
        DefaultCourseDiagnoses diagnoses = (DefaultCourseDiagnoses) report.getRoot().getDiagnoses();
        return diagnoses.getJudgeCoverages().stream()
                .filter(coverage -> HOST.equals(coverage.getJudgeName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no coverage of the judge of " + HOST));
    }

    private static AnalyzerErrorCounts errorsOf(AuditNode context) {
        assertNotNull(context.getNumbers(), "numbers of " + context.getTarget());
        return context.getNumbers().getErrors().getAnalyzers().stream()
                .filter(counts -> HOST.equals(counts.getAnalyzer()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no error counts of " + HOST));
    }

    private static AuditNode quizNode(AuditReport report, String quizId) {
        List<AuditNode> pending = new ArrayList<>(List.of(report.getRoot()));
        while (!pending.isEmpty()) {
            AuditNode node = pending.remove(0);
            if (node.getTarget() == AuditTarget.QUIZ && quizId.equals(node.getEntity().getId())) {
                return node;
            }
            if (node.getChildren() != null) {
                pending.addAll(node.getChildren());
            }
        }
        throw new AssertionError("no exercise " + quizId);
    }

    private static AuditableQuiz mcQuiz(String id, String before, String after, String correct, String... options) {
        List<MultipleChoiceItemEntity> items = new ArrayList<>();
        for (int i = 0; i < options.length; i++) {
            items.add(new MultipleChoiceItemEntity(id + "-" + i, options[i].equals(correct) ? 1.0 : 0.0, options[i]));
        }
        List<SentencePartEntity> parts = List.of(
                new SentencePartEntity(SentencePartKind.TEXT, before, null),
                new SentencePartEntity(SentencePartKind.CLOZE, "", null),
                new SentencePartEntity(SentencePartKind.TEXT, after, null));
        return new AuditableQuiz(List.of(), id, "Elegir la opción", "A1-MC", "Una traducción",
                List.of(before + " " + correct + " " + after), null, "Elegí la opción correcta.", parts,
                new MultipleChoiceEntity("SINGLE", items), null, null);
    }

    /**
     * analyze with multiple-choice-validity as Main wires it: the provider of A with the judge of C
     * over the common runtime, a book in memory, an exam approved for the version under the name of
     * the analyzer, the real renderer and a judge with recorded verdicts that never reaches a model.
     */
    private static final class Journey {
        final MemoryLedger ledger = new MemoryLedger();
        final RecordedJudge judge = new RecordedJudge();
        final RecordedReader reader = new RecordedReader();
        final QuizStudentViewRenderer renderer = new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create());
        final CorrectorMatcher correctorMatcher = new TableCorrectorMatcher();

        Journey() {
            reader.verdicts.put(ONLY_CORRECT_PAYLOAD, new ChoiceAlternativeVerdict(true, List.of(), null,
                    "Sólo vale «is»"));
            reader.verdicts.put(FOOD_PAYLOAD, new ChoiceAlternativeVerdict(true, List.of(new ChoiceAlternativeOption(
                    "a lot", FindingSeverity.HIGH, "«a lot» al final también es correcto")), null, "Vale «a lot»"));
            reader.verdicts.put(THIRSTY_PAYLOAD, new ChoiceAlternativeVerdict(false, List.of(
                    new ChoiceAlternativeOption("if so", FindingSeverity.HIGH,
                            "«if so» sigue a «let's drink water soon»")),
                    "«if not» contradice el resto", "La clave está invertida"));
        }

        AuditReport analyze(int budget, AuditableQuiz... quizzes) {
            Sha256ContentFingerprinter fingerprinter = new Sha256ContentFingerprinter();
            ExamRegistry registry = new ExamRegistry();
            registry.statuses.put(HOST + "|" + VERSION, JudgeExamStatus.APPROVED);
            DefaultJudgeRuntime runtime = new DefaultJudgeRuntime(
                    new DefaultEvaluationSessionFactory(ledger, fingerprinter),
                    new ExamEvaluationSessionFactory(ledger, fingerprinter), registry, evaluatorId -> List.of(),
                    new RuntimeConfig());
            DefaultChoiceAlternativeJudgeRunFactory judgeRunFactory = new DefaultChoiceAlternativeJudgeRunFactory(HOST,
                    runtime, judge, reader, new Scale(), renderer, correctorMatcher);
            MultipleChoiceValidityAnalyzerProvider provider = new MultipleChoiceValidityAnalyzerProvider(
                    correctorMatcher, judgeRunFactory, renderer);
            IAuditEngine engine = new IAuditEngine(new IScoreAggregator(), new DefaultAnalyzerCatalog(List.of(provider)),
                    new DefaultFindingCollector(), new DefaultContextNumbersCalculator());
            return engine.runAudit(course(quizzes), new AnalyzerRunSelection(List.of(HOST),
                    Map.of(HOST, new EvaluationRunPolicy(budget, false, null, null, null, null))));
        }

        private static AuditableCourse course(AuditableQuiz... quizzes) {
            AuditableKnowledge knowledge = new AuditableKnowledge(List.of(quizzes), "Elegir la opción",
                    "Elegí la opción correcta.", true, "knowledge-mc", "Elegir la opción", "A1-MC", SentenceMode.FILL,
                    "Elegir la opción");
            AuditableTopic topic = new AuditableTopic(List.of(knowledge), "topic-mc", "Opción múltiple", "A1-T");
            return new AuditableCourse(List.of(new AuditableMilestone(List.of(topic), "milestone-a1", "A1", "A1")));
        }
    }

    /** The book in memory: the latest of a key wins, and the history keeps them all. */
    private static final class MemoryLedger implements EvaluationLedger {
        final List<EvaluationRecord> records = Collections.synchronizedList(new ArrayList<>());

        @Override
        public synchronized void append(EvaluationRecord record) {
            records.add(record);
        }

        @Override
        public synchronized Optional<EvaluationRecord> findLatest(EvaluationKey key) {
            List<EvaluationRecord> history = history(key);
            return history.isEmpty() ? Optional.empty() : Optional.of(history.get(history.size() - 1));
        }

        @Override
        public synchronized List<EvaluationRecord> history(EvaluationKey key) {
            List<EvaluationRecord> matching = new ArrayList<>();
            for (EvaluationRecord record : records) {
                if (key.equals(record.getKey())) {
                    matching.add(record);
                }
            }
            return matching;
        }
    }

    /** The judge choice-alternative-judge with recorded verdicts: it counts its consultations. */
    private static final class RecordedJudge implements Evaluator {
        final Map<String, EvaluationOutcome> outcomes = new HashMap<>();
        final AtomicInteger calls = new AtomicInteger();

        void answer(String quizId, String payload) {
            outcomes.put(quizId, new EvaluationEmitted(payload));
        }

        @Override
        public String evaluatorId() {
            return JUDGE_ID;
        }

        @Override
        public EvaluationOutcome evaluate(EvaluationSubject subject) {
            calls.incrementAndGet();
            EvaluationOutcome outcome = outcomes.get(subject.getSubjectRef());
            return outcome != null
                    ? outcome
                    : new EvaluationNotEmitted(EvaluationFailureKind.OUTPUT_UNUSABLE, "sin veredicto grabado");
        }

        @Override
        public Optional<String> evaluatorVersion() {
            return Optional.of(VERSION);
        }
    }

    /** Reads the recorded payloads back into their verdicts, as the Jackson reader would. */
    private static final class RecordedReader implements ChoiceAlternativeVerdictReader {
        final Map<String, ChoiceAlternativeVerdict> verdicts = new HashMap<>();

        @Override
        public ChoiceAlternativeVerdict read(String payload) {
            ChoiceAlternativeVerdict verdict = verdicts.get(payload);
            if (verdict == null) {
                throw new IllegalArgumentException("payload sin veredicto grabado: " + payload);
            }
            return verdict;
        }
    }

    /** The exam registry by judge and version. */
    private static final class ExamRegistry implements JudgeExamRegistry {
        final Map<String, JudgeExamStatus> statuses = new HashMap<>();

        @Override
        public void record(JudgeExamRecord record) {
            statuses.put(record.getJudgeName() + "|" + record.getJudgeVersion(), record.getStatus());
        }

        @Override
        public Optional<JudgeExamRecord> latest(String judgeName, String judgeVersion) {
            JudgeExamStatus status = statuses.get(judgeName + "|" + judgeVersion);
            if (status == null) {
                return Optional.empty();
            }
            return Optional.of(new JudgeExamRecord(judgeName, judgeVersion, status, 13, 0, List.of(), List.of(),
                    status == JudgeExamStatus.APPROVED, "2026-09-29", "prueba", Instant.parse("2026-09-30T10:00:00Z")));
        }

        @Override
        public List<JudgeExamRecord> history(String judgeName) {
            return List.of();
        }
    }

    /** José's decisions for the runtime: 4 at a time, budget 0 by default, 189 per hour. */
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
            return new LinkedHashMap<>();
        }
    }

    /** The scale of F-MCVALE-R003. */
    private static final class Scale implements ChoiceAlternativeConfig {
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
            return new LinkedHashMap<>();
        }
    }

    /**
     * A double of the corrector's comparison (package A, not built in this branch): case, spaces and
     * signs never count, and the long forms are the same answer as the contracted ones.
     */
    private static final class TableCorrectorMatcher implements CorrectorMatcher {
        @Override
        public Set<String> canonicalForms(String text) {
            return Set.of(canonical(text));
        }

        @Override
        public boolean accepts(GapContext context, List<String> acceptedOptions, String written) {
            return acceptedOptions.stream().anyMatch(accepted -> sameAnswer(accepted, written));
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

        private static String canonical(String text) {
            String lower = text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}' ]", " ");
            lower = lower.strip().replaceAll("\\s+", " ");
            return lower.replace("isn't", "is not").replace("aren't", "are not");
        }
    }
}
