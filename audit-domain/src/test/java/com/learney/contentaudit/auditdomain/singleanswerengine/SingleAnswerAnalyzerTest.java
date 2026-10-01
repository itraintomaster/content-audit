package com.learney.contentaudit.auditdomain.singleanswerengine;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableEntity;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableMilestone;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.AuditableTopic;
import com.learney.contentaudit.auditdomain.ContentAnalyzer;
import com.learney.contentaudit.auditdomain.CorrectorMatcher;
import com.learney.contentaudit.auditdomain.DefaultCourseDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultKnowledgeDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultLevelDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultQuizDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultTopicDiagnoses;
import com.learney.contentaudit.auditdomain.EvaluationRunPolicy;
import com.learney.contentaudit.auditdomain.EvaluationRunPurpose;
import com.learney.contentaudit.auditdomain.JudgeExamRegistry;
import com.learney.contentaudit.auditdomain.JudgeRuntimeConfig;
import com.learney.contentaudit.auditdomain.SingleAnswerConfig;
import com.learney.contentaudit.auditdomain.correctormatch.GapContext;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.auditdomain.findingengine.DefaultFindingCollector;
import com.learney.contentaudit.auditdomain.judging.EvaluatorCoverage;
import com.learney.contentaudit.auditdomain.judging.JudgeCoverageDiagnosis;
import com.learney.contentaudit.auditdomain.judging.JudgeExamOutcome;
import com.learney.contentaudit.auditdomain.judging.JudgeExamRecord;
import com.learney.contentaudit.auditdomain.judging.JudgeExamStatus;
import com.learney.contentaudit.auditdomain.judging.JudgeExamVerdict;
import com.learney.contentaudit.auditdomain.judging.JudgeRuntime;
import com.learney.contentaudit.auditdomain.judging.JudgeVersionStanding;
import com.learney.contentaudit.auditdomain.judgingengine.DefaultJudgeExamVerdictExporter;
import com.learney.contentaudit.auditdomain.judgingengine.DefaultJudgeRuntime;
import com.learney.contentaudit.auditdomain.singleanswer.AlternativeAnswer;
import com.learney.contentaudit.auditdomain.singleanswer.AlternativeAnswerKind;
import com.learney.contentaudit.auditdomain.singleanswer.SingleAnswerDiagnosis;
import com.learney.contentaudit.auditdomain.singleanswer.SingleAnswerVerdict;
import com.learney.contentaudit.auditdomain.singleanswer.SingleAnswerVerdictReader;
import com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer;
import com.learney.contentaudit.coursedomain.MultipleChoiceEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceItemEntity;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.processing.Generated;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Generated(
        value = "com.sentinel.SentinelEngine",
        comments = "Generated by Sentinel"
)
public class SingleAnswerAnalyzerTest {

    private static final String NAME = "single-answer";

    private static final String JUDGE_ID = "single-answer-judge";

    private static final String VERSION = "single-answer-judge@guia-1";

    @Test
    @DisplayName("should neither ask the judge about a multiple choice quiz nor count it as reached")
    @Tag("FEAT-UNICA")
    @Tag("F-UNICA-R001")
    public void shouldNeitherAskTheJudgeAboutAMultipleChoiceQuizNorCountItAsReached() {
        Bench bench = new Bench(true);
        AuditableQuiz doesHe = doesHeNeedWater();
        AuditableQuiz aChild = isHeAChild();
        bench.answers(doesHe, verdict(alt("doesn't", AlternativeAnswerKind.DIFFERENT_VALID, FindingSeverity.HIGH)));
        // Even with a recorded answer at hand for it, the multiple choice is FEAT-MCVALE's.
        bench.answers(aChild, verdict(alt("Are", AlternativeAnswerKind.DIFFERENT_VALID, FindingSeverity.HIGH)));
        Course course = course(doesHe, aChild);

        ContentAnalyzer analyzer = bench.run(budget(10), course);

        assertEquals(List.of(doesHe.getId()), bench.judge.asked(), "only the exercise to write is asked");
        AuditNode multipleChoice = course.node(aChild);
        assertFalse(multipleChoice.getScores().containsKey(NAME), "no score for the multiple choice");
        assertFalse(multipleChoice.getUnevaluatedBy().contains(NAME), "nor reached and left unevaluated");
        assertTrue(diagnosis(multipleChoice).isEmpty());
        assertTrue(analyzer.findingsAt(multipleChoice).isEmpty());

        JudgeCoverageDiagnosis coverage = coverage(course);
        assertEquals(1, coverage.getReached(), "the multiple choice is not reached by single-answer");
        assertEquals(1, coverage.getEvaluated());
        assertEquals(0, coverage.getNotEvaluated());
        assertEquals(1, coverage.getEvaluators().get(0).getCoverage().getReached());
        assertEquals(0.3, course.node(doesHe).getScores().get(NAME), 1e-9);
    }

    @Test
    @DisplayName("should reuse the recorded verdict of a quiz without consulting and without spending budget")
    @Tag("FEAT-UNICA")
    @Tag("F-UNICA-R005")
    public void shouldReuseTheRecordedVerdictOfAQuizWithoutConsultingAndWithoutSpendingBudget() {
        Bench bench = new Bench(true);
        AuditableQuiz doesHe = doesHeNeedWater();
        AuditableQuiz verduras = verduras();
        bench.answers(doesHe, verdict(alt("doesn't", AlternativeAnswerKind.DIFFERENT_VALID, FindingSeverity.HIGH)));
        bench.answers(verduras, verdict(alt("eat", AlternativeAnswerKind.DIFFERENT_VALID, FindingSeverity.BLOCKING)));
        bench.run(budget(1), course(doesHe));
        assertEquals(1, bench.judge.calls(), "the first run records the verdict of «Does he need water?»");

        // Same content, a new run with a budget of one.
        Course again = course(doesHe, verduras);
        ContentAnalyzer analyzer = bench.run(budget(1), again);

        assertEquals(List.of(doesHe.getId(), verduras.getId()), bench.judge.asked(),
                "the recorded one is not asked again; its budget is left for the new one");
        SingleAnswerDiagnosis reused = diagnosis(again.node(doesHe)).orElseThrow();
        assertTrue(reused.isReusedFromPreviousRun());
        assertEquals(0.3, again.node(doesHe).getScores().get(NAME), 1e-9, "the reused verdict scores as the new one");
        assertEquals(List.of("doesn't"), markers(analyzer, again.node(doesHe)), "and gives the same finding");
        assertFalse(diagnosis(again.node(verduras)).orElseThrow().isReusedFromPreviousRun());
        assertEquals(0.0, again.node(verduras).getScores().get(NAME), 1e-9);

        EvaluatorCoverage judgeCoverage = coverage(again).getEvaluators().get(0);
        assertEquals(1, judgeCoverage.getCoverage().getReused());
        assertEquals(1, judgeCoverage.getCoverage().getEvaluatedInRun());
        assertEquals(0, judgeCoverage.getCoverage().getPending());
        assertEquals(2, bench.ledger.size(), "one record per consultation, none for the reuse");
    }

    @Test
    @DisplayName("should consult no more quizzes than its budget and leave the rest unevaluated, without score and without finding")
    @Tag("FEAT-UNICA")
    @Tag("F-UNICA-R005")
    public void shouldConsultNoMoreQuizzesThanItsBudgetAndLeaveTheRestUnevaluatedWithoutScoreAndWithoutFinding(
            ) {
        Bench bench = new Bench(true);
        AuditableQuiz doesHe = doesHeNeedWater();
        AuditableQuiz verduras = verduras();
        AuditableQuiz sheWontCome = sheWontCome();
        bench.answers(doesHe, verdict(alt("doesn't", AlternativeAnswerKind.DIFFERENT_VALID, FindingSeverity.HIGH)));
        bench.answers(verduras, verdict(alt("eat", AlternativeAnswerKind.DIFFERENT_VALID, FindingSeverity.BLOCKING)));
        bench.answers(sheWontCome,
                verdict(alt("won't come", AlternativeAnswerKind.DIFFERENT_VALID, FindingSeverity.MEDIUM)));
        Course course = course(doesHe, verduras, sheWontCome);

        ContentAnalyzer analyzer = bench.run(budget(2), course);

        assertEquals(2, bench.judge.calls(), "never more consultations than the budget");
        assertEquals(Set.of(doesHe.getId(), verduras.getId()), new HashSet<>(bench.judge.asked()),
                "the first two of the walk, in order");
        AuditNode left = course.node(sheWontCome);
        assertFalse(left.getScores().containsKey(NAME), "no score: no average takes it for a passing one");
        assertTrue(left.getUnevaluatedBy().contains(NAME), "declared reached and not evaluated");
        assertTrue(analyzer.findingsAt(left).isEmpty(), "no finding");
        assertTrue(diagnosis(left).isEmpty(), "no diagnosis");
        assertEquals(List.of("doesn't"), markers(analyzer, course.node(doesHe)), "the consulted ones do have theirs");
        assertEquals(List.of("eat"), markers(analyzer, course.node(verduras)));

        JudgeCoverageDiagnosis coverage = coverage(course);
        assertEquals(2, coverage.getEvaluated());
        assertEquals(1, coverage.getNotEvaluated());
        assertEquals(1, coverage.getCost().getPendingUnits());
    }

    @Test
    @DisplayName("should finish the run and record nothing when the judge's service fails or its output is unusable, leaving those quizzes unevaluated")
    @Tag("FEAT-UNICA")
    @Tag("F-UNICA-R005")
    public void shouldFinishTheRunAndRecordNothingWhenTheJudgesServiceFailsOrItsOutputIsUnusableLeavingThoseQuizzesUnevaluated(
            ) {
        Bench bench = new Bench(true);
        AuditableQuiz verduras = verduras();
        AuditableQuiz doesHe = doesHeNeedWater();
        AuditableQuiz sheWontCome = sheWontCome();
        bench.judge.fails(verduras, EvaluationFailureKind.OUTPUT_UNUSABLE, "el agente agotó sus intentos");
        bench.judge.fails(doesHe, EvaluationFailureKind.EVALUATOR_UNAVAILABLE,
                "sesión vencida: renovala con `claude auth login`");
        bench.answers(sheWontCome,
                verdict(alt("won't come", AlternativeAnswerKind.DIFFERENT_VALID, FindingSeverity.MEDIUM)));
        Course course = course(verduras, doesHe, sheWontCome);

        // One at a time, so the order of the failures is the order of the walk.
        assertDoesNotThrow(() -> bench.run(new EvaluationRunPolicy(10, false, null, null, null, 1), course));

        assertEquals(List.of(verduras.getId(), doesHe.getId()), bench.judge.asked(),
                "an unusable output does not stop the run; a service failure stops the consultations");
        assertEquals(0, bench.ledger.size(), "nothing is recorded: the three stay pending for a later run");
        for (AuditableQuiz quiz : List.of(verduras, doesHe, sheWontCome)) {
            AuditNode node = course.node(quiz);
            assertFalse(node.getScores().containsKey(NAME), quiz.getId());
            assertTrue(node.getUnevaluatedBy().contains(NAME), quiz.getId());
            assertTrue(diagnosis(node).isEmpty(), quiz.getId());
        }
        JudgeCoverageDiagnosis coverage = coverage(course);
        assertEquals(0, coverage.getEvaluated());
        assertEquals(3, coverage.getNotEvaluated());
        assertEquals("sesión vencida: renovala con `claude auth login`", coverage.getUnavailableReason(),
                "the report says why it stopped consulting");
    }

    @Test
    @DisplayName("should declare the CLOZE quizzes reached, evaluated and unevaluated in its coverage")
    @Tag("FEAT-UNICA")
    @Tag("F-UNICA-R005")
    public void shouldDeclareTheCLOZEQuizzesReachedEvaluatedAndUnevaluatedInItsCoverage() {
        Bench bench = new Bench(true);
        AuditableQuiz doesHe = doesHeNeedWater();
        AuditableQuiz verduras = verduras();
        AuditableQuiz italy = italy();
        AuditableQuiz sheWontCome = sheWontCome();
        AuditableQuiz aChild = isHeAChild();
        bench.answers(doesHe, verdict(alt("doesn't", AlternativeAnswerKind.DIFFERENT_VALID, FindingSeverity.HIGH)));
        bench.run(budget(1), course(doesHe));
        bench.answers(verduras, verdict(alt("eat", AlternativeAnswerKind.DIFFERENT_VALID, FindingSeverity.BLOCKING)));
        // The judge pronounced itself, but what it emitted cannot be read: that exercise alone stays out.
        bench.judge.emits(italy, "{\"alternatives\": [{\"answer\": \"from Friday until");
        bench.answers(sheWontCome,
                verdict(alt("won't come", AlternativeAnswerKind.DIFFERENT_VALID, FindingSeverity.MEDIUM)));
        Course course = course(doesHe, verduras, italy, sheWontCome, aChild);

        bench.run(budget(2), course);

        JudgeCoverageDiagnosis coverage = coverage(course);
        assertEquals(NAME, coverage.getJudgeName());
        assertEquals(AuditTarget.QUIZ, coverage.getUnit());
        assertEquals(4, coverage.getReached(), "the four exercises to write, never the multiple choice");
        assertEquals(2, coverage.getEvaluated(), "the reused one and the one consulted and read");
        assertEquals(2, coverage.getNotEvaluated(), "the unreadable one and the one beyond the budget");
        assertTrue(course.node(italy).getUnevaluatedBy().contains(NAME));
        assertTrue(course.node(sheWontCome).getUnevaluatedBy().contains(NAME));
        assertEquals(0.0, course.node(verduras).getScores().get(NAME), 1e-9, "the others go on");

        EvaluatorCoverage judgeCoverage = coverage.getEvaluators().get(0);
        assertEquals(JUDGE_ID, judgeCoverage.getEvaluatorId());
        assertEquals(VERSION, judgeCoverage.getConsultedVersion());
        assertEquals(4, judgeCoverage.getCoverage().getReached());
        assertEquals(1, judgeCoverage.getCoverage().getReused());
        assertEquals(2, judgeCoverage.getCoverage().getEvaluatedInRun());
        assertEquals(1, judgeCoverage.getCoverage().getPending());
        assertEquals(List.of(new JudgeVersionStanding(VERSION, 2, JudgeExamStatus.APPROVED)), coverage.getVersions(),
                "the two verdicts that score, with the exam of their version");
        assertEquals(JudgeExamStatus.APPROVED, coverage.getStanding().getStatus());
        assertNull(coverage.getUnmatchedDeclaredIds(), "no declared set in this run");
    }

    @Test
    @DisplayName("should consult the judge on no quiz when the run asks no budget for single-answer, and declare what the pending quizzes cost")
    @Tag("FEAT-UNICA")
    @Tag("F-UNICA-R005")
    public void shouldConsultTheJudgeOnNoQuizWhenTheRunAsksNoBudgetForSingleanswerAndDeclareWhatThePendingQuizzesCost(
            ) {
        Bench bench = new Bench(true);
        AuditableQuiz doesHe = doesHeNeedWater();
        AuditableQuiz verduras = verduras();
        bench.answers(doesHe, verdict(alt("doesn't", AlternativeAnswerKind.DIFFERENT_VALID, FindingSeverity.HIGH)));
        bench.answers(verduras, verdict(alt("eat", AlternativeAnswerKind.DIFFERENT_VALID, FindingSeverity.BLOCKING)));
        Course course = course(doesHe, verduras);

        // The run names no policy for single-answer: even an approved version gets budget 0.
        bench.run(null, course);

        assertEquals(0, bench.judge.calls(), "nothing is paid without an explicit request");
        assertTrue(course.node(doesHe).getUnevaluatedBy().contains(NAME));
        assertTrue(course.node(verduras).getUnevaluatedBy().contains(NAME));
        JudgeCoverageDiagnosis coverage = coverage(course);
        assertEquals(JudgeExamStatus.APPROVED, coverage.getStanding().getStatus());
        assertNull(coverage.getEvaluators().get(0).getConsultedVersion(), "it never consulted");
        assertEquals(2, coverage.getCost().getPendingUnits());
        assertEquals(2, coverage.getCost().getPendingConsultations(), "one consultation per exercise");
        assertFalse(coverage.getCost().isRateMeasured(), "no history: the configured rate");
        assertEquals(189.0, coverage.getCost().getRatePerHour(), 1e-9);
        assertEquals(2 / 189.0, coverage.getCost().getHoursOneAtATime(), 1e-9);
        assertEquals(4, coverage.getCost().getMaxConcurrentEvaluations());
        assertEquals(2 / 189.0 / 4, coverage.getCost().getHoursConcurrent(), 1e-9);
    }

    @Test
    @DisplayName("should judge in an EXAM run only the CLOZE exercises of the file and leave a multiple choice one of the file without verdict")
    @Tag("FEAT-UNICA")
    @Tag("F-UNICA-R006")
    public void shouldJudgeInAnEXAMRunOnlyTheCLOZEExercisesOfTheFileAndLeaveAMultipleChoiceOneOfTheFileWithoutVerdict(
            ) {
        // A version that never took the exam: the exam is how it takes it, so it is never braked.
        Bench bench = new Bench(false);
        AuditableQuiz doesHe = doesHeNeedWater();
        AuditableQuiz verduras = verduras();
        AuditableQuiz aChild = isHeAChild();
        bench.answers(doesHe, verdict(alt("doesn't", AlternativeAnswerKind.DIFFERENT_VALID, FindingSeverity.HIGH)));
        bench.answers(verduras, verdict(alt("eat", AlternativeAnswerKind.DIFFERENT_VALID, FindingSeverity.BLOCKING)));
        bench.answers(aChild, verdict(alt("Are", AlternativeAnswerKind.DIFFERENT_VALID, FindingSeverity.HIGH)));
        Course photo = course(doesHe, verduras, aChild);
        Set<String> file = new LinkedHashSet<>(List.of(doesHe.getId(), aChild.getId()));

        ContentAnalyzer analyzer = bench.run(
                new EvaluationRunPolicy(10, false, null, file, EvaluationRunPurpose.EXAM, null), photo);
        SingleAnswerAnalyzerProvider provider = bench.provider();
        new DefaultFindingCollector().collect(photo.root, analyzer, provider.describe());

        assertEquals(List.of(doesHe.getId()), bench.judge.asked(), "only the exercise to write of the file");
        assertEquals(0.3, photo.node(doesHe).getScores().get(NAME), 1e-9);
        AuditNode outOfFile = photo.node(verduras);
        assertFalse(outOfFile.getScores().containsKey(NAME), "an exercise out of the file is neither asked");
        assertFalse(outOfFile.getUnevaluatedBy().contains(NAME), "nor counted");
        AuditNode multipleChoice = photo.node(aChild);
        assertFalse(multipleChoice.getScores().containsKey(NAME));
        assertFalse(multipleChoice.getUnevaluatedBy().contains(NAME));
        assertEquals(1, coverage(photo).getReached());

        // The verdicts of the exam: the CLOZE one fails, the multiple choice is not judged by this judge.
        List<JudgeExamVerdict> verdicts = new DefaultJudgeExamVerdictExporter()
                .export(new AuditReport(photo.root), provider.describe(), file, coverage(photo));
        Map<String, JudgeExamVerdict> byCase = verdicts.stream()
                .collect(Collectors.toMap(JudgeExamVerdict::getCaseId, v -> v));
        assertEquals(JudgeExamOutcome.FAILS, byCase.get(doesHe.getId()).getOutcome());
        assertEquals(FindingSeverity.HIGH, byCase.get(doesHe.getId()).getWorstSeverity());
        assertEquals(JudgeExamOutcome.NOT_JUDGED, byCase.get(aChild.getId()).getOutcome());
        assertEquals("el juez no juzga este ejercicio", byCase.get(aChild.getId()).getDetail());

        // The exam writes in its own space, never as a verdict of the course.
        String fingerprint = bench.fingerprintOf(photo.node(doesHe));
        assertTrue(bench.ledger.findLatest(new EvaluationKey(JUDGE_ID, fingerprint)).isEmpty());
        assertTrue(bench.ledger.findLatest(new EvaluationKey("exam:" + JUDGE_ID, fingerprint)).isPresent());
    }

    @Test
    @DisplayName("should judge a candidate of a marked quiz with the same judge and version, and reuse its verdict once the candidate enters the course")
    @Tag("FEAT-UNICA")
    @Tag("F-UNICA-R007")
    public void shouldJudgeACandidateOfAMarkedQuizWithTheSameJudgeAndVersionAndReuseItsVerdictOnceTheCandidateEntersTheCourse(
            ) {
        Bench bench = new Bench(true);
        AuditableQuiz marked = doesHeNeedWater();
        bench.answers(marked, verdict(alt("doesn't", AlternativeAnswerKind.DIFFERENT_VALID, FindingSeverity.HIGH)));
        Course course = course(marked);
        bench.run(budget(1), course);
        assertEquals(0.3, course.node(marked).getScores().get(NAME), 1e-9, "the course marks it");

        // The panel's candidate: the same exercise with «Yes, he ___».
        AuditableQuiz candidate = quiz(marked.getId(), "Respuestas cortas",
                "Usa $short forms$ si es posible. Escribe la respuesta corta.",
                text("Does he need water? Yes, he"), cloze("does"), text("."));
        bench.answers(candidate, verdict());
        Course candidateCourse = course(candidate);
        bench.run(budget(1), candidateCourse);

        assertEquals(2, bench.judge.calls(), "the candidate is a new content: it is consulted");
        JudgeCoverageDiagnosis candidateCoverage = coverage(candidateCourse);
        assertEquals(JUDGE_ID, candidateCoverage.getEvaluators().get(0).getEvaluatorId(), "the same judge");
        assertEquals(VERSION, candidateCoverage.getEvaluators().get(0).getConsultedVersion(), "the same version");
        assertEquals(coverage(course).getStanding().getJudgeVersion(), candidateCoverage.getStanding().getJudgeVersion());
        assertEquals(1.0, candidateCourse.node(candidate).getScores().get(NAME), 1e-9, "the candidate passes");

        // The candidate enters the course: its verdict is reused, nothing is paid.
        Course entered = course(candidate);
        ContentAnalyzer analyzer = bench.run(null, entered);

        assertEquals(2, bench.judge.calls(), "no consultation once it entered");
        SingleAnswerDiagnosis reused = diagnosis(entered.node(candidate)).orElseThrow();
        assertTrue(reused.isReusedFromPreviousRun());
        assertEquals(1.0, entered.node(candidate).getScores().get(NAME), 1e-9);
        assertTrue(analyzer.findingsAt(entered.node(candidate)).isEmpty());
        assertEquals(List.of(new JudgeVersionStanding(VERSION, 1, JudgeExamStatus.APPROVED)),
                coverage(entered).getVersions());
    }

    // -------------------------------------------------------------------------
    // The bench: the real runtime, sessions and render, a judge that answers recorded verdicts
    // -------------------------------------------------------------------------

    private static final class Bench {
        final MemoryLedger ledger = new MemoryLedger();
        final RecordedJudge judge = new RecordedJudge();
        final RecordedVerdicts verdicts = new RecordedVerdicts();
        final ContentFingerprinter fingerprinter = new Sha256ContentFingerprinter();
        final DefaultQuizStudentViewRenderer renderer =
                new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create());
        final JudgeRuntime runtime;

        Bench(boolean approved) {
            ContentFingerprinter fp = fingerprinter;
            runtime = new DefaultJudgeRuntime(new DefaultEvaluationSessionFactory(ledger, fp),
                    new ExamEvaluationSessionFactory(ledger, fp), new Registry(approved ? VERSION : null),
                    evaluatorId -> List.of(), new RuntimeConfig());
        }

        SingleAnswerAnalyzerProvider provider() {
            return new SingleAnswerAnalyzerProvider(runtime, judge, verdicts, new Scale(), renderer,
                    new FakeCorrector());
        }

        ContentAnalyzer run(EvaluationRunPolicy policy, Course course) {
            ContentAnalyzer analyzer = provider().create(policy);
            course.walk(analyzer);
            return analyzer;
        }

        void answers(AuditableQuiz quiz, SingleAnswerVerdict verdict) {
            judge.emits(quiz, verdicts.payloadOf(verdict));
        }

        String fingerprintOf(AuditNode node) {
            return fingerprinter.fingerprint(new DefaultSingleAnswerSubjectBuilder(renderer).build(node).getContent());
        }
    }

    /** The ledger in memory: findLatest is the last record with that key, history all of them. */
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

        synchronized int size() {
            return records.size();
        }
    }

    /** The exam registry: APPROVED for the version given, nothing for any other. */
    private static final class Registry implements JudgeExamRegistry {
        private final String approvedVersion;

        Registry(String approvedVersion) {
            this.approvedVersion = approvedVersion;
        }

        @Override
        public void record(JudgeExamRecord record) {
        }

        @Override
        public Optional<JudgeExamRecord> latest(String judgeName, String judgeVersion) {
            if (approvedVersion == null || !approvedVersion.equals(judgeVersion) || !NAME.equals(judgeName)) {
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

    /** DefaultJudgeRuntimeConfig lives in audit-application: 4 at a time, budget 0, 189 per hour. */
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

    /** The scale of F-UNICA-R004, as audit-application configures it. */
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

    /** Package A's corrector is not built here: this double compares letter by letter. */
    private static final class FakeCorrector implements CorrectorMatcher {
        @Override
        public Set<String> canonicalForms(String text) {
            return Set.of(text.strip().toLowerCase());
        }

        @Override
        public boolean accepts(GapContext context, List<String> acceptedOptions, String written) {
            return acceptedOptions.stream().anyMatch(o -> o.strip().equalsIgnoreCase(written.strip()));
        }

        @Override
        public boolean sameAnswer(String first, String second) {
            return first.strip().equalsIgnoreCase(second.strip());
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

    /**
     * The judge, without any paid consultation: it answers what each test recorded for the
     * exercise the student sees and counts every time it is asked.
     */
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
            byExercise.put(statementOf(quiz), new EvaluationEmitted(payload));
        }

        void fails(AuditableQuiz quiz, EvaluationFailureKind kind, String reason) {
            byExercise.put(statementOf(quiz), new EvaluationNotEmitted(kind, reason));
        }

        List<String> asked() {
            synchronized (asked) {
                return new ArrayList<>(asked);
            }
        }

        int calls() {
            return asked.size();
        }

        private static String statementOf(AuditableQuiz quiz) {
            return new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create()).render(quiz)
                    .getStatement();
        }
    }

    /** The reader of package C's adapters is built elsewhere: payload to the verdict the test built. */
    private static final class RecordedVerdicts implements SingleAnswerVerdictReader {
        private final Map<String, SingleAnswerVerdict> byPayload = new HashMap<>();

        synchronized String payloadOf(SingleAnswerVerdict verdict) {
            String payload = json(verdict);
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
    }

    /** The JSON the agent emits, with the shape of the artifact singleAnswerVerdict. */
    private static String json(SingleAnswerVerdict verdict) {
        StringBuilder sb = new StringBuilder("{\"alternatives\": [");
        List<AlternativeAnswer> alternatives = verdict.getAlternatives();
        for (int i = 0; i < alternatives.size(); i++) {
            AlternativeAnswer a = alternatives.get(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("{\"answer\": ").append(quoted(a.getAnswer()))
                    .append(", \"gap\": ").append(a.getGap())
                    .append(", \"kind\": ").append(quoted(a.getKind().name()))
                    .append(", \"severity\": ").append(quoted(a.getSeverity().name()))
                    .append(", \"reason\": ").append(quoted(a.getReason())).append('}');
        }
        return sb.append("], \"rationale\": ").append(quoted(verdict.getRationale())).append('}').toString();
    }

    private static String quoted(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    // -------------------------------------------------------------------------
    // The course, walked as the engine walks it
    // -------------------------------------------------------------------------

    private static final class Course {
        final AuditNode root;
        private final Map<AuditableQuiz, AuditNode> nodes = new IdentityHashMap<>();

        Course(AuditableQuiz... quizzes) {
            root = node(null, AuditTarget.COURSE, null);
            root.setDiagnoses(new DefaultCourseDiagnoses());
            AuditNode milestone = node(new AuditableMilestone(List.of(), "m-a1", "A1", ""), AuditTarget.MILESTONE, root);
            milestone.setDiagnoses(new DefaultLevelDiagnoses());
            AuditNode topic = node(new AuditableTopic(List.of(), "t-1", "Present Simple", ""), AuditTarget.TOPIC,
                    milestone);
            topic.setDiagnoses(new DefaultTopicDiagnoses());
            AuditNode knowledge = node(new AuditableKnowledge(List.of(quizzes), "Respuestas cortas",
                    "La consigna del tema.", false, "k-1", "Respuestas cortas", "", SentenceMode.FILL,
                    "Present Simple"), AuditTarget.KNOWLEDGE, topic);
            knowledge.setDiagnoses(new DefaultKnowledgeDiagnoses());
            for (AuditableQuiz quiz : quizzes) {
                AuditNode quizNode = node(quiz, AuditTarget.QUIZ, knowledge);
                quizNode.setChildren(List.of());
                quizNode.setDiagnoses(new DefaultQuizDiagnoses());
                nodes.put(quiz, quizNode);
            }
        }

        AuditNode node(AuditableQuiz quiz) {
            return nodes.get(quiz);
        }

        void walk(ContentAnalyzer analyzer) {
            for (AuditNode milestone : root.getChildren()) {
                analyzer.onMilestone(milestone);
                for (AuditNode topic : milestone.getChildren()) {
                    analyzer.onTopic(topic);
                    for (AuditNode knowledge : topic.getChildren()) {
                        analyzer.onKnowledge(knowledge);
                        for (AuditNode quiz : knowledge.getChildren()) {
                            analyzer.onQuiz(quiz);
                        }
                    }
                }
            }
            analyzer.onCourseComplete(root);
        }

        private static AuditNode node(AuditableEntity entity, AuditTarget target, AuditNode parent) {
            AuditNode node = new AuditNode(entity, target, parent, new ArrayList<>(), new LinkedHashMap<>(),
                    new LinkedHashMap<>(), null);
            if (parent != null) {
                parent.getChildren().add(node);
            }
            return node;
        }
    }

    private static Course course(AuditableQuiz... quizzes) {
        return new Course(quizzes);
    }

    private static JudgeCoverageDiagnosis coverage(Course course) {
        List<JudgeCoverageDiagnosis> coverages = ((DefaultCourseDiagnoses) course.root.getDiagnoses())
                .getJudgeCoverages();
        assertEquals(1, coverages.size(), "one coverage per run of single-answer");
        return coverages.get(0);
    }

    private static Optional<SingleAnswerDiagnosis> diagnosis(AuditNode node) {
        return ((DefaultQuizDiagnoses) node.getDiagnoses()).getSingleAnswerDiagnosis();
    }

    private static List<String> markers(ContentAnalyzer analyzer, AuditNode node) {
        return analyzer.findingsAt(node).stream().map(f -> f.getMarker()).collect(Collectors.toList());
    }

    private static EvaluationRunPolicy budget(int maxNewEvaluations) {
        return new EvaluationRunPolicy(maxNewEvaluations, false, null, null, null, null);
    }

    // -------------------------------------------------------------------------
    // The exercises of the requirement, as they are in the photo of 29/9
    // -------------------------------------------------------------------------

    private static AuditableQuiz doesHeNeedWater() {
        return quiz("67fab6d59930102295341f57", "Respuestas cortas",
                "Usa $short forms$ si es posible. Escribe la respuesta corta.",
                text("Does he need water? He"), cloze("does"), text("."));
    }

    private static AuditableQuiz verduras() {
        return quiz("67fab6d59930102295341ecd", "Afirmativas regulares",
                "Escribe la forma afirmativa en presente simple.",
                text("I"), cloze("love"), text("a lot of vegetables."));
    }

    private static AuditableQuiz sheWontCome() {
        return quiz("67fab6d59930102295342189", "Elegir la forma natural",
                "¿Es más natural usar 'will' o 'going to'? Escribe oraciones.",
                text("She"), cloze("isn't going to come|is not going to come|'s not going to come"),
                text("(not / come)."));
    }

    private static AuditableQuiz italy() {
        return quiz("67fab6d59930102295342464", "Rangos temporales",
                "Crea oraciones usando 'from' ... 'to / until / till'.",
                text("He went to Italy on Friday and came back on Sunday.He was in Italy"),
                cloze("from Friday to Sunday|until Sunday|till Sunday"), text("."));
    }

    private static AuditableQuiz isHeAChild() {
        return new AuditableQuiz(List.of(), "67fab6d59930102295341e87", "Preguntas con be", "", "¿Él es un niño?",
                List.of("Is he a child?"), null, "Elige la opción correcta.",
                List.of(cloze(""), text("a child? (He)")),
                new MultipleChoiceEntity("single", List.of(new MultipleChoiceItemEntity("1", 1.0, "Is"),
                        new MultipleChoiceItemEntity("2", 0.0, "Are"), new MultipleChoiceItemEntity("3", 0.0, "Am"))),
                null, "A1.02.01.01");
    }

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

    private static AlternativeAnswer alt(String answer, AlternativeAnswerKind kind, FindingSeverity severity) {
        return new AlternativeAnswer(answer, 1, kind, severity, "Lo dice el juez grabado.");
    }

    private static SingleAnswerVerdict verdict(AlternativeAnswer... alternatives) {
        return new SingleAnswerVerdict(List.of(alternatives), "Lo que el alumno ve antes de contestar.");
    }

    @Test
    @DisplayName("should declare in its tally as many exercises as quizzes, in each state")
    @Tag("FEAT-CIERRE")
    @Tag("F-CIERRE-R001")
    public void shouldDeclareInItsTallyAsManyExercisesAsQuizzesInEachState() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
