package com.learney.contentaudit.auditapplication;

import com.learney.contentaudit.auditdomain.studentview.StudentOptionView;
import com.learney.contentaudit.auditdomain.studentview.QuizStudentView;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learney.contentaudit.auditdomain.AnalyzerCatalog;
import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableCourse;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableMilestone;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.AuditableTopic;
import com.learney.contentaudit.auditdomain.CourseDiagnoses;
import com.learney.contentaudit.auditdomain.EvaluationRunPolicy;
import com.learney.contentaudit.auditdomain.EvaluationRunPurpose;
import com.learney.contentaudit.auditdomain.IAuditEngine;
import com.learney.contentaudit.auditdomain.IScoreAggregator;
import com.learney.contentaudit.auditdomain.JudgeExamRegistry;
import com.learney.contentaudit.auditdomain.QuizInstructionVerdictReader;
import com.learney.contentaudit.auditdomain.findingengine.DefaultAnalyzerCatalog;
import com.learney.contentaudit.auditdomain.findingengine.DefaultContextNumbersCalculator;
import com.learney.contentaudit.auditdomain.findingengine.DefaultFindingCollector;
import com.learney.contentaudit.auditdomain.judging.JudgeCoverageDiagnosis;
import com.learney.contentaudit.auditdomain.judging.JudgeExamGroupResult;
import com.learney.contentaudit.auditdomain.judging.JudgeExamOutcome;
import com.learney.contentaudit.auditdomain.judging.JudgeExamRecord;
import com.learney.contentaudit.auditdomain.judging.JudgeExamStanding;
import com.learney.contentaudit.auditdomain.judging.JudgeExamStatus;
import com.learney.contentaudit.auditdomain.judging.JudgeExamVerdict;
import com.learney.contentaudit.auditdomain.judgingengine.DefaultJudgeExamVerdictExporter;
import com.learney.contentaudit.auditdomain.judgingengine.DefaultJudgeRuntime;
import com.learney.contentaudit.auditdomain.quizinstruction.InstructionSeverity;
import com.learney.contentaudit.auditdomain.quizinstruction.InstructionViolation;
import com.learney.contentaudit.auditdomain.quizinstruction.QuizInstructionVerdict;
import com.learney.contentaudit.auditdomain.quizinstructionengine.DefaultQuizInstructionAnalyzerFactory;
import com.learney.contentaudit.auditinfrastructure.FileSystemJudgeExamRegistry;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.CourseRepository;
import com.learney.contentaudit.coursedomain.MultipleChoiceEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceItemEntity;
import com.learney.contentaudit.coursedomain.SentenceMode;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationEmitted;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationKey;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationLedger;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationOutcome;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRecord;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSubject;
import com.learney.contentaudit.evaluationledgerdomain.Evaluator;
import com.learney.contentaudit.evaluationledgerdomain.contentfingerprint.Sha256ContentFingerprinter;
import com.learney.contentaudit.evaluationledgerdomain.evaluationsession.DefaultEvaluationSessionFactory;
import com.learney.contentaudit.evaluationledgerdomain.evaluationsession.ExamEvaluationSessionFactory;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import com.learney.contentaudit.auditdomain.NlpToken;
import com.learney.contentaudit.auditdomain.finishedsentence.DefaultFinishedSentenceReader;
import com.learney.contentaudit.auditdomain.localtools.LocalToolStatus;
import com.learney.contentaudit.auditdomain.localtools.YesNoModel;
import com.learney.contentaudit.auditdomain.localtools.YesNoProbabilities;
import com.learney.contentaudit.auditdomain.localtoolsengine.DefaultLocalToolGate;
import com.learney.contentaudit.auditdomain.naturalness.NaturalnessAnalyzerProvider;

@Generated(
        value = "com.sentinel.SentinelEngine",
        comments = "Generated by Sentinel"
)
public class DefaultJudgeExamServiceTest {

    @TempDir
    Path workdir;

    @Test
    @DisplayName("should take the exam of a judge over the photo, running only its analyzer, and give one verdict per exercise of the file")
    @Tag("FEAT-JEXAM")
    @Tag("F-JEXAM-R001")
    public void shouldTakeTheExamOfAJudgeOverThePhotoRunningOnlyItsAnalyzerAndGiveOneVerdictPerExerciseOfTheFile(
            ) {
        Bench bench = new Bench(workdir);
        AuditRunner runner = spy(bench.runner());
        DefaultJudgeExamService service = bench.service(runner);
        Set<String> file = new LinkedHashSet<>(List.of(VERDURAS, OK_1, BREACH_1));

        JudgeExamTaken taken = service.take(new JudgeExamRequest(QUIZ_INSTRUCTION, PHOTO, file, 10, 4));

        ArgumentCaptor<AuditRunRequest> request = ArgumentCaptor.forClass(AuditRunRequest.class);
        verify(runner).runAudit(org.mockito.ArgumentMatchers.eq(PHOTO), request.capture());
        assertEquals(Set.of(QUIZ_INSTRUCTION), request.getValue().getIncludedAnalyzers(),
                "R001: the exam runs only the analyzer that hosts the judge");
        EvaluationRunPolicy policy = request.getValue().getAnalyzerPolicies().get(QUIZ_INSTRUCTION);
        assertEquals(EvaluationRunPurpose.EXAM, policy.getPurpose(), "R001: as an exam, never as a run on the course");
        assertEquals(file, policy.getReevaluationSubjectIds(), "R001: whose declared set is the file");
        assertEquals(List.of(VERDURAS, OK_1, BREACH_1),
                taken.getVerdicts().stream().map(JudgeExamVerdict::getCaseId).toList(),
                "R001: one verdict per exercise of the file, in its order");
        assertEquals(List.of(JudgeExamOutcome.PASSES, JudgeExamOutcome.PASSES, JudgeExamOutcome.FAILS),
                taken.getVerdicts().stream().map(JudgeExamVerdict::getOutcome).toList());
        assertEquals(List.of(OK_1, BREACH_1, VERDURAS).stream().sorted().toList(), bench.judge.askedSorted(),
                "R001: the judge was asked the three exercises of the file and nothing else");
        assertEquals("v-consigna", taken.getJudgeVersion());
    }

    @Test
    @DisplayName("should judge the exam exercises as they are in the photo of 29/9 even when the live course changed them since")
    @Tag("FEAT-JEXAM")
    @Tag("F-JEXAM-R001")
    public void shouldJudgeTheExamExercisesAsTheyAreInThePhotoOf299EvenWhenTheLiveCourseChangedThemSince(
            ) {
        Bench bench = new Bench(workdir);
        // Since the photo, a batch added «like» as an accepted answer of the verduras on the live course.
        bench.live = Bench.course(Map.of(VERDURAS, List.of("love|like")));
        DefaultJudgeExamService service = bench.service(bench.runner());

        service.take(new JudgeExamRequest(QUIZ_INSTRUCTION, PHOTO, Set.of(VERDURAS), 10, null));

        assertEquals(1, bench.judge.asked.size());
        String judged = bench.judge.contents.get(0).get("acceptedAnswers");
        assertEquals("Hueco 1: love", judged, "R001: the verduras as they are in the photo: " + judged);
        assertFalse(judged.contains("like"), "R001: never as the live course has them now: " + judged);
    }

    @Test
    @DisplayName("should record TAKEN for the version when every exercise of the file got a verdict, and not when one stayed without verdict or is not in the photo")
    @Tag("FEAT-JEXAM")
    @Tag("F-JEXAM-R001")
    @Tag("F-JEXAM-R014")
    public void shouldRecordTAKENForTheVersionWhenEveryExerciseOfTheFileGotAVerdictAndNotWhenOneStayedWithoutVerdictOrIsNotInThePhoto(
            ) {
        // An id that is not in the photo: the version does not take the exam.
        Bench missing = new Bench(workdir.resolve("missing"));
        JudgeExamTaken withMissing = missing.service(missing.runner()).take(new JudgeExamRequest(QUIZ_INSTRUCTION, PHOTO,
                new LinkedHashSet<>(List.of(VERDURAS, ABSENT)), 10, null));
        assertFalse(withMissing.isRecordedAsTaken());
        assertEquals(JudgeExamOutcome.NOT_JUDGED, withMissing.getVerdicts().get(1).getOutcome());
        assertTrue(missing.registry.history(QUIZ_INSTRUCTION).isEmpty(), "R001: nothing recorded");

        // A budget that does not reach every exercise: one stays without verdict.
        Bench shortBudget = new Bench(workdir.resolve("short"));
        JudgeExamTaken withPending = shortBudget.service(shortBudget.runner()).take(new JudgeExamRequest(
                QUIZ_INSTRUCTION, PHOTO, new LinkedHashSet<>(List.of(VERDURAS, OK_1)), 1, null));
        assertFalse(withPending.isRecordedAsTaken());
        assertTrue(shortBudget.registry.history(QUIZ_INSTRUCTION).isEmpty(), "R001: nothing recorded");

        // Every exercise with its verdict: TAKEN for the version, the condition to accept a measurement.
        Bench whole = new Bench(workdir.resolve("whole"));
        JudgeExamTaken complete = whole.service(whole.runner()).take(new JudgeExamRequest(QUIZ_INSTRUCTION, PHOTO,
                new LinkedHashSet<>(List.of(VERDURAS, OK_1)), 10, null,
                "codex-cli", "gpt-6.1-sol", "medium"));
        assertTrue(complete.isRecordedAsTaken());
        JudgeExamRecord taken = whole.registry.latest(QUIZ_INSTRUCTION, "v-consigna").orElseThrow();
        assertEquals(JudgeExamStatus.TAKEN, taken.getStatus(), "R001: the version is recorded as having taken it");
        assertEquals(2, taken.getCasesJudged());
        assertEquals(0, taken.getCasesNotJudged());
        assertEquals("codex-cli", taken.getProviderName(), "R014: effective provider stays with TAKEN");
        assertEquals("gpt-6.1-sol", taken.getModelName(), "R014: exact model stays with TAKEN");
        assertEquals("medium", taken.getReasoningEffort(), "R014: explicit effort stays with TAKEN");
    }

    @Test
    @DisplayName("should leave analyze on the course with the same scores, findings and pending quizzes before and after a version takes the exam")
    @Tag("FEAT-JEXAM")
    @Tag("F-JEXAM-R002")
    public void shouldLeaveAnalyzeOnTheCourseWithTheSameScoresFindingsAndPendingQuizzesBeforeAndAfterAVersionTakesTheExam(
            ) {
        Bench bench = new Bench(workdir);
        // What the course's ledger had: an August verdict for one quiz, under the version of today.
        bench.recordOnCourse(OK_1, Bench.COMPLIANT);
        List<EvaluationRecord> courseLedgerBefore = bench.ledger.records();

        Map<String, Object> before = Bench.snapshot(bench.runner().runAudit(PHOTO, new AuditRunRequest(null, null, null)));
        bench.service(bench.runner()).take(new JudgeExamRequest(QUIZ_INSTRUCTION, PHOTO,
                new LinkedHashSet<>(List.of(VERDURAS, BREACH_1, OK_1)), 10, 4));
        Map<String, Object> after = Bench.snapshot(bench.runner().runAudit(PHOTO, new AuditRunRequest(null, null, null)));

        assertEquals(2, bench.judge.asked.size(), "the exam did consult the judge, on what the version had not judged");
        assertEquals(before, after, "R002: analyze gives the same scores, findings and pending quizzes");
        assertEquals(courseLedgerBefore, bench.ledger.recordsOf(Bench.EVALUATOR),
                "R002: no verdict of the exam is a verdict of the course");
    }

    @Test
    @DisplayName("should let a version that already took the exam take it again without a single consultation")
    @Tag("FEAT-JEXAM")
    @Tag("F-JEXAM-R002")
    public void shouldLetAVersionThatAlreadyTookTheExamTakeItAgainWithoutASingleConsultation() {
        Bench bench = new Bench(workdir);
        DefaultJudgeExamService service = bench.service(bench.runner());
        Set<String> file = new LinkedHashSet<>(List.of(VERDURAS, OK_1, BREACH_1, MC_47CC));

        JudgeExamTaken first = service.take(new JudgeExamRequest(QUIZ_INSTRUCTION, PHOTO, file, 10, 4));
        int consultedFirst = bench.judge.asked.size();
        JudgeExamTaken again = service.take(new JudgeExamRequest(QUIZ_INSTRUCTION, PHOTO, file, 10, 4));

        assertEquals(4, consultedFirst, "the first time, each exercise once");
        assertEquals(4, bench.judge.asked.size(), "R002: the second time, not a single consultation");
        assertEquals(first.getVerdicts(), again.getVerdicts(), "R002: and the same verdicts");
    }

    @Test
    @DisplayName("should rehearse the exam with budget 0 without consulting, reusing what that version judged and saying how many consultations are missing")
    @Tag("FEAT-JEXAM")
    @Tag("F-JEXAM-R010")
    public void shouldRehearseTheExamWithBudget0WithoutConsultingReusingWhatThatVersionJudgedAndSayingHowManyConsultationsAreMissing(
            ) {
        Bench bench = new Bench(workdir);
        // The version of today already judged the verduras on the course; another version judged OK_1.
        bench.recordOnCourse(VERDURAS, Bench.COMPLIANT);
        bench.ledger.append(new EvaluationRecord(new EvaluationKey(Bench.EVALUATOR,
                new Sha256ContentFingerprinter().fingerprint(bench.contentOf(OK_1))), Bench.COMPLIANT, OK_1,
                Instant.parse("2026-08-10T10:00:00Z"), "v-vieja"));
        DefaultJudgeExamService service = bench.service(bench.runner());

        JudgeExamTaken rehearsal = service.take(new JudgeExamRequest(QUIZ_INSTRUCTION, PHOTO,
                new LinkedHashSet<>(List.of(VERDURAS, OK_1, BREACH_1)), 0, null));

        assertEquals(0, bench.judge.asked.size(), "R010: a rehearsal pays nothing");
        assertEquals(List.of(JudgeExamOutcome.PASSES, JudgeExamOutcome.NOT_JUDGED, JudgeExamOutcome.NOT_JUDGED),
                rehearsal.getVerdicts().stream().map(JudgeExamVerdict::getOutcome).toList(),
                "R010: it reuses only what that version judged");
        assertEquals(2, rehearsal.getCoverage().getCost().getPendingConsultations(),
                "R004: and says how many consultations are missing");
        assertFalse(rehearsal.isRecordedAsTaken(), "R001: a rehearsal that left exercises unjudged is not TAKEN");
    }

    @Test
    @DisplayName("should record the result measured outside for a version that took the whole exam and make it the standing of that version")
    @Tag("FEAT-JEXAM")
    @Tag("F-JEXAM-R001")
    @Tag("F-JEXAM-R014")
    public void shouldRecordTheResultMeasuredOutsideForAVersionThatTookTheWholeExamAndMakeItTheStandingOfThatVersion(
            ) {
        Bench bench = new Bench(workdir);
        DefaultJudgeExamService service = bench.service(bench.runner());
        service.take(new JudgeExamRequest(QUIZ_INSTRUCTION, PHOTO, new LinkedHashSet<>(List.of(VERDURAS, OK_1)), 10,
                null, "codex-cli", "gpt-6.1-sol", "medium"));
        JudgeExamRecord measured = new JudgeExamRecord(QUIZ_INSTRUCTION, "v-consigna", JudgeExamStatus.APPROVED, 2, 0,
                List.of(new JudgeExamGroupResult("a", 1, 1, 0), new JudgeExamGroupResult("c", 1, 1, 0)), List.of(), true,
                "2026-09-29", "orquestador", "claude-cli", "modelo-falsificado", "max", null);

        JudgeExamStanding standing = service.record(measured);

        assertEquals(JudgeExamStatus.APPROVED, standing.getStatus(), "R001: the measured result is its standing");
        assertTrue(standing.isConsultsOnCourse(), "R003: an approved version may consult on the course");
        JudgeExamRecord kept = bench.registry.latest(QUIZ_INSTRUCTION, "v-consigna").orElseThrow();
        assertEquals(JudgeExamStatus.APPROVED, kept.getStatus());
        assertEquals(measured.getGroups(), kept.getGroups(), "R001: with its groups, as measured outside");
        assertEquals("orquestador", kept.getMeasuredBy());
        assertEquals("codex-cli", kept.getProviderName(), "R014: inherited from TAKEN, not the measured file");
        assertEquals("gpt-6.1-sol", kept.getModelName(), "R014: measured input cannot forge the model");
        assertEquals("medium", kept.getReasoningEffort(), "R014: measured input cannot forge the effort");
        assertEquals(PHOTO.toString(), kept.getPhoto(), "R014: measured input cannot forge the examined photo");
    }

    @Test
    @DisplayName("should reject with JudgeExamNotTakenException the measured result of a version that did not take the whole exam, recording nothing")
    @Tag("FEAT-JEXAM")
    @Tag("F-JEXAM-R001")
    public void shouldRejectWithJudgeExamNotTakenExceptionTheMeasuredResultOfAVersionThatDidNotTakeTheWholeExamRecordingNothing(
            ) {
        Bench bench = new Bench(workdir);
        DefaultJudgeExamService service = bench.service(bench.runner());
        // It took the exam, but one exercise of the file is not in the photo.
        service.take(new JudgeExamRequest(QUIZ_INSTRUCTION, PHOTO, new LinkedHashSet<>(List.of(VERDURAS, ABSENT)), 10,
                null));
        JudgeExamRecord measured = new JudgeExamRecord(QUIZ_INSTRUCTION, "v-consigna", JudgeExamStatus.APPROVED, 1, 1,
                List.of(), List.of(), true, "2026-09-29", "orquestador", null);

        JudgeExamNotTakenException rejected = assertThrows(JudgeExamNotTakenException.class,
                () -> service.record(measured));

        assertEquals("v-consigna", rejected.getJudgeVersion());
        assertTrue(bench.registry.history(QUIZ_INSTRUCTION).isEmpty(), "R001: nothing is recorded");
        assertEquals(JudgeExamStatus.NOT_TAKEN, bench.runtime.standing(QUIZ_INSTRUCTION, "v-consigna").getStatus());
    }

    @Test
    @DisplayName("should list every exam record of a judge, across its versions, in the order they were recorded")
    @Tag("FEAT-JEXAM")
    @Tag("F-JEXAM-R001")
    public void shouldListEveryExamRecordOfAJudgeAcrossItsVersionsInTheOrderTheyWereRecorded() {
        Bench bench = new Bench(workdir);
        DefaultJudgeExamService service = bench.service(bench.runner());
        List<JudgeExamRecord> recorded = List.of(
                record("v1", JudgeExamStatus.TAKEN, "2026-09-30T10:00:00Z"),
                record("v1", JudgeExamStatus.FAILED, "2026-09-30T11:00:00Z"),
                record("v2", JudgeExamStatus.TAKEN, "2026-09-30T12:00:00Z"),
                record("v2", JudgeExamStatus.APPROVED, "2026-09-30T13:00:00Z"));
        recorded.forEach(bench.registry::record);
        bench.registry.record(new JudgeExamRecord("single-answer", "u1", JudgeExamStatus.TAKEN, 39, 0, List.of(),
                List.of(), false, "2026-09-29", null, Instant.parse("2026-09-30T14:00:00Z")));

        List<JudgeExamRecord> history = service.history(QUIZ_INSTRUCTION);

        assertEquals(recorded, history, "R001: every record of the judge, across its versions, in recorded order");
    }

    @Test
    @DisplayName("should give one verdict to every exercise of a quiz-instruction exam file that mixes CLOZE and multiple choice exercises, without knowing their groups")
    @Tag("FEAT-JEXAM")
    @Tag("F-JEXAM-R009")
    public void shouldGiveOneVerdictToEveryExerciseOfAQuizinstructionExamFileThatMixesCLOZEAndMultipleChoiceExercisesWithoutKnowingTheirGroups(
            ) {
        Bench bench = new Bench(workdir);
        DefaultJudgeExamService service = bench.service(bench.runner());
        // The file carries ids and nothing else: no group, nothing expected.
        Set<String> file = new LinkedHashSet<>(List.of(MC_47CC, VERDURAS, BREACH_1, OK_1));

        JudgeExamTaken taken = service.take(new JudgeExamRequest(QUIZ_INSTRUCTION, PHOTO, file, 10, null));

        assertEquals(List.of(MC_47CC, VERDURAS, BREACH_1, OK_1),
                taken.getVerdicts().stream().map(JudgeExamVerdict::getCaseId).toList(),
                "R009: one verdict per exercise of the file");
        assertTrue(taken.getVerdicts().stream().noneMatch(v -> v.getOutcome() == JudgeExamOutcome.NOT_JUDGED),
                "R009: CLOZE and multiple choice alike");
        assertTrue(bench.judge.contents.stream().anyMatch(c -> c.get("options").contains("(correcta)")),
                "R009: the multiple choice is read with its options, as on the course");
        assertEquals(JudgeExamOutcome.FAILS, taken.getVerdicts().get(2).getOutcome());
        assertEquals(List.of("instruction-breach"), taken.getVerdicts().get(2).getRules());
        assertNotNull(taken.getVerdicts().get(2).getWorstSeverity());
    }

    // -------------------------------------------------------------------------
    // The bench: the consigna judge in the catalog, over the real judges runtime, the real
    // engine and runner, a ledger in memory and the exam registry on disk. Never a model.
    // -------------------------------------------------------------------------

    private static final String QUIZ_INSTRUCTION = "quiz-instruction";

    private static final Path PHOTO = Path.of("db/fotos/2026-09-29/english-course");

    private static final Path LIVE = Path.of("db/english-course");

    private static final String VERDURAS = "67fab6d59930102295341ecd";

    private static final String MC_47CC = "67fab6d599301022953447cc";

    private static final String OK_1 = "67fab6d59930102295341ec3";

    private static final String BREACH_1 = "67fab6d59930102295342d9e";

    private static final String ABSENT = "67fab6d5000000000000ab5e";

    private static JudgeExamRecord record(String version, JudgeExamStatus status, String at) {
        return new JudgeExamRecord(QUIZ_INSTRUCTION, version, status, 238, 0, List.of(), List.of(),
                status == JudgeExamStatus.APPROVED, "2026-09-29", "orquestador", Instant.parse(at));
    }

    private static final class Bench {
        static final String EVALUATOR = "quiz-instruction-validator";
        static final String COMPLIANT = "{\"compliant\":true}";
        static final String BREACHING = "{\"compliant\":false}";

        final MemoryLedger ledger = new MemoryLedger();
        final JudgeExamRegistry registry;
        final CountingJudge judge = new CountingJudge(Set.of(BREACH_1));
        final DefaultJudgeRuntime runtime;
        final AnalyzerCatalog catalog;
        AuditableCourse photo = course(Map.of());
        AuditableCourse live = course(Map.of());

        Bench(Path workdir) {
            Sha256ContentFingerprinter fingerprinter = new Sha256ContentFingerprinter();
            registry = new FileSystemJudgeExamRegistry(workdir);
            runtime = new DefaultJudgeRuntime(new DefaultEvaluationSessionFactory(ledger, fingerprinter),
                    new ExamEvaluationSessionFactory(ledger, fingerprinter), registry, evaluatorId -> List.of(),
                    new DefaultJudgeRuntimeConfig());
            QuizInstructionVerdictReader reader = payload -> COMPLIANT.equals(payload)
                    ? new QuizInstructionVerdict(true, 0.9, InstructionSeverity.NONE, "Cumple", List.of(), List.of())
                    : new QuizInstructionVerdict(false, 0.9, InstructionSeverity.MAJOR, "Incumple la consigna",
                            List.of(new InstructionViolation("CALCO", "Natural", "the moment", "calco")), List.of());
            catalog = new DefaultAnalyzerCatalog(List.of(new DefaultQuizInstructionAnalyzerFactory(judge, reader,
                    new DefaultQuizInstructionConfig(new DefaultJudgeRuntimeConfig()), runtime, new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create()))));
        }

        DefaultAuditRunner runner() {
            CourseRepository repository = mock(CourseRepository.class);
            CourseToAuditableMapper mapper = mock(CourseToAuditableMapper.class);
            CourseEntity photoEntity = new CourseEntity();
            CourseEntity liveEntity = new CourseEntity();
            when(repository.load(PHOTO)).thenReturn(photoEntity);
            when(repository.load(LIVE)).thenReturn(liveEntity);
            when(mapper.map(any())).thenAnswer(invocation -> invocation.getArgument(0) == liveEntity ? live : photo);
            return new DefaultAuditRunner(repository, mapper, new IAuditEngine(new IScoreAggregator(), catalog,
                    new DefaultFindingCollector(), new DefaultContextNumbersCalculator()), catalog);
        }

        DefaultJudgeExamService service(AuditRunner runner) {
            return new DefaultJudgeExamService(runner, catalog, new DefaultJudgeExamVerdictExporter(), registry,
                    runtime);
        }

        /** What the consigna judge reads for a quiz of the photo, key by key: the exercise as the student sees it. */
        Map<String, String> contentOf(String quizId) {
            AuditableQuiz quiz = photo.getMilestones().get(0).getTopics().get(0).getKnowledge().get(0).getQuizzes()
                    .stream().filter(q -> q.getId().equals(quizId)).findFirst().orElseThrow();
            QuizStudentView view = new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create()).render(quiz);
            Map<String, String> content = new LinkedHashMap<>();
            content.put("cefrLevel", "A1");
            content.put("topic", "Present Simple");
            content.put("title", "Mezcla");
            content.put("instructions", "Completa la oración.");
            content.put("exercise", view.getStatement());
            List<String> gaps = new ArrayList<>();
            for (int i = 0; i < view.getGaps().size(); i++) {
                gaps.add("Hueco " + (i + 1) + ": " + String.join(" | ", view.getGaps().get(i).getAcceptedAnswers()));
            }
            content.put("acceptedAnswers", String.join("\n", gaps));
            List<String> options = new ArrayList<>();
            for (StudentOptionView option : view.getOptions()) {
                options.add("- " + option.getLabel() + (option.isCorrect() ? " (correcta)" : ""));
            }
            content.put("options", String.join("\n", options));
            return content;
        }

        void recordOnCourse(String quizId, String payload) {
            ledger.append(new EvaluationRecord(new EvaluationKey(EVALUATOR,
                    new Sha256ContentFingerprinter().fingerprint(contentOf(quizId))), payload, quizId,
                    Instant.parse("2026-08-14T14:39:40Z"), "v-consigna"));
        }

        private static String serialize(AuditableQuiz quiz) {
            StringBuilder sb = new StringBuilder();
            for (SentencePartEntity part : quiz.getSentenceParts()) {
                sb.append(part.getKind()).append(':').append(part.getText()).append(':');
                if (part.getOptions() != null) {
                    sb.append(String.join(",", part.getOptions()));
                }
                sb.append('|');
            }
            return sb.toString();
        }

        /** An A1 topic with the verduras, the …47cc multiple choice and two more quizzes. */
        static AuditableCourse course(Map<String, List<String>> answersById) {
            List<AuditableQuiz> quizzes = new ArrayList<>();
            quizzes.add(cloze(VERDURAS, "I", answersById.getOrDefault(VERDURAS, List.of("love")),
                    "a lot of vegetables."));
            quizzes.add(new AuditableQuiz(List.of(), MC_47CC, "Mezcla", "A1.02.01.2", "¿Tienes sed? Si es así...",
                    List.of("Are you thirsty? if not, let's drink water soon."), null, "Completa la oración.",
                    List.of(new SentencePartEntity(SentencePartKind.TEXT, "Are you thirsty?", null),
                            new SentencePartEntity(SentencePartKind.CLOZE, "", null),
                            new SentencePartEntity(SentencePartKind.TEXT, ", let's drink water soon.", null)),
                    new MultipleChoiceEntity("single", List.of(new MultipleChoiceItemEntity("1", 1.0, "if not"),
                            new MultipleChoiceItemEntity("2", 0.0, "if so"))), null, null));
            quizzes.add(cloze(OK_1, "You", List.of("see"), "(see) cats."));
            quizzes.add(cloze(BREACH_1, "It's the moment", List.of("for"), "dinner!"));
            AuditableKnowledge knowledge = new AuditableKnowledge(quizzes, "Mezcla", "Completa la oración.", false,
                    "k-mezcla", "Mezcla", "A1.02.01", SentenceMode.FILL, "Present Simple");
            AuditableTopic topic = new AuditableTopic(List.of(knowledge), "t-ps", "Present Simple", "A1.02");
            return new AuditableCourse(List.of(new AuditableMilestone(List.of(topic), "m-a1", "A1", "A1")));
        }

        private static AuditableQuiz cloze(String id, String before, List<String> answers, String after) {
            return new AuditableQuiz(List.of(), id, "Mezcla", "A1.02.01.x", "traducción", List.of(before + " "
                    + answers.get(0).split("\\|")[0] + " " + after), null, "Completa la oración.",
                    List.of(new SentencePartEntity(SentencePartKind.TEXT, before, null),
                            new SentencePartEntity(SentencePartKind.CLOZE, "____", answers),
                            new SentencePartEntity(SentencePartKind.TEXT, after, null)),
                    null, null, null);
        }

        /** Scores, findings and the not-evaluated marks of every quiz of an analysis. */
        static Map<String, Object> snapshot(AuditReport report) {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            collect(report.getRoot(), snapshot);
            if (report.getRoot().getDiagnoses() instanceof CourseDiagnoses diagnoses) {
                snapshot.put("coverage", diagnoses.getQuizInstructionCoverage().map(c -> c.getCoverage()).orElse(null));
                snapshot.put("pending", diagnoses.getJudgeCoverages().stream()
                        .map(JudgeCoverageDiagnosis::getNotEvaluated).toList());
            }
            return snapshot;
        }

        private static void collect(AuditNode node, Map<String, Object> snapshot) {
            if (node.getTarget() == AuditTarget.QUIZ) {
                String id = node.getEntity().getId();
                snapshot.put(id + ".scores", new LinkedHashMap<>(node.getScores()));
                snapshot.put(id + ".findings", node.getFindings().stream()
                        .map(f -> f.getAnalyzer() + "|" + f.getRule() + "|" + f.getSeverity()).toList());
                snapshot.put(id + ".unevaluated", List.copyOf(node.getUnevaluatedBy()));
            }
            if (node.getChildren() != null) {
                node.getChildren().forEach(child -> collect(child, snapshot));
            }
        }
    }

    /** The consigna judge of the bench: recorded verdicts, never a model; counts what it is asked. */
    private static final class CountingJudge implements Evaluator {
        final List<String> asked = Collections.synchronizedList(new ArrayList<>());
        final List<Map<String, String>> contents = Collections.synchronizedList(new ArrayList<>());
        private final Set<String> breaching;

        CountingJudge(Set<String> breaching) {
            this.breaching = breaching;
        }

        List<String> askedSorted() {
            List<String> sorted = new ArrayList<>(asked);
            Collections.sort(sorted);
            return sorted;
        }

        @Override
        public String evaluatorId() {
            return Bench.EVALUATOR;
        }

        @Override
        public Optional<String> evaluatorVersion() {
            return Optional.of("v-consigna");
        }

        @Override
        public EvaluationOutcome evaluate(EvaluationSubject subject) {
            asked.add(subject.getSubjectRef());
            contents.add(subject.getContent());
            return new EvaluationEmitted(breaching.contains(subject.getSubjectRef()) ? Bench.BREACHING : Bench.COMPLIANT);
        }
    }

    private static final class MemoryLedger implements EvaluationLedger {
        private final List<EvaluationRecord> records = Collections.synchronizedList(new ArrayList<>());

        List<EvaluationRecord> records() {
            return List.copyOf(records);
        }

        List<EvaluationRecord> recordsOf(String evaluatorId) {
            return records().stream().filter(r -> evaluatorId.equals(r.getKey().getEvaluatorId())).toList();
        }

        @Override
        public void append(EvaluationRecord record) {
            records.add(record);
        }

        @Override
        public Optional<EvaluationRecord> findLatest(EvaluationKey key) {
            EvaluationRecord latest = null;
            for (EvaluationRecord record : records()) {
                if (record.getKey().equals(key)) {
                    latest = record;
                }
            }
            return Optional.ofNullable(latest);
        }

        @Override
        public List<EvaluationRecord> history(EvaluationKey key) {
            return records().stream().filter(r -> r.getKey().equals(key)).toList();
        }
    }

    @Test
    @DisplayName("should record the exam that naturalness takes without its local model under the version of claude -p, leaving the version of llama3 without exam")
    @Tag("FEAT-MLOCAL")
    @Tag("F-MLOCAL-R006")
    public void shouldRecordTheExamThatNaturalnessTakesWithoutItsLocalModelUnderTheVersionOfClaudePLeavingTheVersionOfLlama3WithoutExam() {
        // F-MLOCAL-R006 and R007 (hook B-11): with CONTENT_AUDIT_OLLAMA_URL=off, naturalness takes its exam with
        // claude -p; what gets recorded is the version of claude -p, and llama3's stays without exam.
        Sha256ContentFingerprinter fingerprinter = new Sha256ContentFingerprinter();
        MemoryLedger ledger = new MemoryLedger();
        JudgeExamRegistry registry = new FileSystemJudgeExamRegistry(workdir);
        DefaultJudgeRuntime runtime = new DefaultJudgeRuntime(new DefaultEvaluationSessionFactory(ledger, fingerprinter),
                new ExamEvaluationSessionFactory(ledger, fingerprinter), registry, evaluatorId -> List.of(),
                new DefaultJudgeRuntimeConfig());
        NativeSaysNo claude = new NativeSaysNo("She is an engineer of hers.");
        AnalyzerCatalog catalog = new DefaultAnalyzerCatalog(List.of(new NaturalnessAnalyzerProvider(new Llama3Off(),
                claude, runtime, new DefaultFinishedSentenceReader(), new DefaultLocalToolGate(runtime))));
        CourseRepository repository = mock(CourseRepository.class);
        CourseToAuditableMapper mapper = mock(CourseToAuditableMapper.class);
        when(repository.load(PHOTO)).thenReturn(new CourseEntity());
        when(mapper.map(any())).thenReturn(naturalnessPhoto());
        DefaultAuditRunner runner = new DefaultAuditRunner(repository, mapper, new IAuditEngine(new IScoreAggregator(),
                catalog, new DefaultFindingCollector(), new DefaultContextNumbersCalculator()), catalog);
        DefaultJudgeExamService service = new DefaultJudgeExamService(runner, catalog,
                new DefaultJudgeExamVerdictExporter(), registry, runtime);

        JudgeExamTaken taken = service.take(new JudgeExamRequest("naturalness", PHOTO,
                new LinkedHashSet<>(List.of(ENGINEER, PEOPLE)), 10, 1));

        assertEquals("naturalness-yes-no-v1", taken.getJudgeVersion(), "the version of claude -p took it");
        assertTrue(taken.isRecordedAsTaken());
        assertEquals(List.of(JudgeExamOutcome.MARKED, JudgeExamOutcome.PASSES),
                taken.getVerdicts().stream().map(JudgeExamVerdict::getOutcome).toList(),
                "«no» only ranks: marked; «yes» passes");
        assertEquals(JudgeExamStatus.TAKEN, registry.latest("naturalness", "naturalness-yes-no-v1").orElseThrow()
                .getStatus());
        assertTrue(registry.latest("naturalness", "llama3:8b@365c0bd3c000 · native-yes-no-1").isEmpty(),
                "llama3's version did not take it");
        assertEquals(2, claude.calls(), "the exam publishes and asks the two sentences of the file");
    }

    // -----------------------------------------------------------------------
    // FEAT-MLOCAL: naturalness without Ollama, with claude -p
    // -----------------------------------------------------------------------

    private static final String ENGINEER = "67fab6d59930102295342c0d";

    private static final String PEOPLE = "67fab6d59930102295341f4a";

    /** Two exercises of the photo that reach naturalness: a sentence with a verb each. */
    private static AuditableCourse naturalnessPhoto() {
        List<NlpToken> verb = List.of(new NlpToken("It", "it", "PRON", null, true, false),
                new NlpToken("is", "be", "AUX", null, true, false));
        List<AuditableQuiz> quizzes = List.of(
                new AuditableQuiz(verb, ENGINEER, "Doble posesivo", "", null, List.of(), null, "Reescribe.",
                        List.of(new SentencePartEntity(SentencePartKind.TEXT, "She is her engineer.", null),
                                new SentencePartEntity(SentencePartKind.CLOZE, "", List.of("She is an engineer of hers."))),
                        null, null, null),
                new AuditableQuiz(verb, PEOPLE, "Contracciones", "", null, List.of(), null, "Completa.",
                        List.of(new SentencePartEntity(SentencePartKind.TEXT, "They", null),
                                new SentencePartEntity(SentencePartKind.CLOZE, "", List.of("don't")),
                                new SentencePartEntity(SentencePartKind.TEXT, "(do not) like people.", null)),
                        null, null, null));
        AuditableKnowledge knowledge = new AuditableKnowledge(quizzes, "Mezcla", "Completa.", true, "k-mezcla",
                "Mezcla", "A2.01.01", SentenceMode.FILL, "Pronombres");
        AuditableTopic topic = new AuditableTopic(List.of(knowledge), "t-pr", "Pronombres", "A2.01");
        return new AuditableCourse(List.of(new AuditableMilestone(List.of(topic), "m-a2", "A2", "A2")));
    }

    /** llama3:8b with Ollama off, as CONTENT_AUDIT_OLLAMA_URL=off leaves it. */
    private static final class Llama3Off implements YesNoModel {
        @Override
        public LocalToolStatus status() {
            return new LocalToolStatus("llama3:8b en Ollama", null, false,
                    "CONTENT_AUDIT_OLLAMA_URL=off: los modelos locales están apagados a pedido", "ollama pull llama3:8b");
        }

        @Override
        public YesNoProbabilities ask(String system, String question) {
            throw new AssertionError("Ollama is off: nothing is asked");
        }
    }

    /** The agent naturalness-yes-no: «no» for the sentences it is given, «yes» for the rest; it counts its calls. */
    private static final class NativeSaysNo implements Evaluator {
        private final Set<String> unnatural;
        private final List<String> asked = Collections.synchronizedList(new ArrayList<>());

        NativeSaysNo(String... unnatural) {
            this.unnatural = Set.of(unnatural);
        }

        @Override
        public String evaluatorId() {
            return "naturalness-yes-no";
        }

        @Override
        public Optional<String> evaluatorVersion() {
            return Optional.of("naturalness-yes-no-v1");
        }

        @Override
        public EvaluationOutcome evaluate(EvaluationSubject subject) {
            String question = subject.getContent().getOrDefault("question", "");
            asked.add(question);
            boolean no = unnatural.stream().anyMatch(sentence -> question.endsWith("\"" + sentence + "\""));
            return new EvaluationEmitted("{\"answer\":\"" + (no ? "no" : "yes") + "\"}");
        }

        int calls() {
            return asked.size();
        }
    }
}
