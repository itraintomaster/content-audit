package com.learney.contentaudit.revisiondomain.batchengine;

import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.cloze;
import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.text;
import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.withParts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.Baseline;
import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.auditdomain.EvaluationRunPolicy;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.auditdomain.judging.JudgeExamStatus;
import com.learney.contentaudit.auditdomain.offlevelwords.DefaultTopicTeachingResolver;
import com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.refinerdomain.PlanFindingTally;
import com.learney.contentaudit.refinerdomain.RefinementPlan;
import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.refinerdomain.RefinerEngine;
import com.learney.contentaudit.refinerdomain.UnconvertedScoreCount;
import com.learney.contentaudit.revisiondomain.SealedGoalsStore;
import com.learney.contentaudit.revisiondomain.batch.BatchChange;
import com.learney.contentaudit.revisiondomain.batch.BatchChangeOrigin;
import com.learney.contentaudit.revisiondomain.batch.BatchCloseRequest;
import com.learney.contentaudit.revisiondomain.batch.BatchClosing;
import com.learney.contentaudit.revisiondomain.batch.BatchEstimate;
import com.learney.contentaudit.revisiondomain.batch.BatchJudgeAccount;
import com.learney.contentaudit.revisiondomain.batch.BatchNotPublishableException;
import com.learney.contentaudit.revisiondomain.batch.BatchProposalRef;
import com.learney.contentaudit.revisiondomain.batch.BatchPublishRequest;
import com.learney.contentaudit.revisiondomain.batch.BatchReport;
import com.learney.contentaudit.revisiondomain.batch.BatchService;
import com.learney.contentaudit.revisiondomain.batch.BatchServiceConfig;
import com.learney.contentaudit.revisiondomain.batch.BatchStatus;
import com.learney.contentaudit.revisiondomain.batch.BatchTaskOutcomeKind;
import com.learney.contentaudit.revisiondomain.batch.CorrectionBatch;
import com.learney.contentaudit.revisiondomain.comparison.BatchComparator;
import com.learney.contentaudit.revisiondomain.comparison.BatchVerdict;
import com.learney.contentaudit.revisiondomain.comparison.GoalDeclaration;
import com.learney.contentaudit.revisiondomain.comparison.SealedGoals;
import com.learney.contentaudit.revisiondomain.comparison.VerdictKind;
import com.learney.contentaudit.revisiondomain.comparisonengine.DefaultBatchComparator;
import com.learney.contentaudit.revisiondomain.comparisonengine.DefaultGoalBook;
import com.learney.contentaudit.revisiondomain.comparisonengine.LeaveOneOutChangeAttributor;
import com.learney.contentaudit.revisiondomain.consolidatedview.ConsolidatedViewAvailability;
import com.learney.contentaudit.revisiondomain.nonregression.ChangeGateVerdict;
import com.learney.contentaudit.revisiondomain.panelengine.DefaultPanelRunnerFactory;
import com.learney.contentaudit.revisiondomain.panelengine.MesaFixture;
import com.learney.contentaudit.revisiondomain.preservationengine.DefaultPreservationFactory;
import com.learney.contentaudit.revisiondomain.projection.CourseProjection;
import com.learney.contentaudit.revisiondomain.projection.CourseProjector;
import com.learney.contentaudit.revisiondomain.projection.ProjectedChange;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
@Tag("F-CJUEZ")
@Tag("F-CJUEZ-J001")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FCjuezJ001JourneyTest {
    @Test
    @Order(1)
    @Tag("path-1")
    @DisplayName("path-1: José pide la estimación de la tanda, ... → El cierre no le paga ninguna consulta... [José cierra sin tope] → failure")
    public void path1_josCierraSinTope_failure() {
        // J001, path 1. The batch of A1 ran its rules and changed two exercises of «Afirmativas regulares»: the vegetables
        // without «a lot of» and «Winter ___ (come) every year.». José asks for the estimate and then closes without asking
        // for any tope: the closing is a dry run -- it pays no judge, the floors that depend on what is missing stay
        // undecided, and the report says how many queries each judge is missing.
        Journey journey = new Journey();
        CorrectionBatch batch = journey.batch;

        // estimar [F-CJUEZ-R001, F-CJUEZ-R002]: per judge, what it judges before and after, what the book already has and
        // what is missing, and how long that takes -- without a single query.
        BatchEstimate estimate = journey.service.estimate();

        assertEquals(2, estimate.getChangedQuizzes(), "the vegetables and «Winter», the exercises the batch changes today");
        assertEquals(DRY_RUN, accounts(estimate.getJudges()), "the three judges that read what changed, in the order of the catalog");
        assertEquals(10, estimate.getJudgeQueries(), "4 of quiz-instruction, 4 of translation-sense and 2 of mini-theory");
        assertEquals(0.05, estimate.getJudgeHours(), 1e-9, "10 queries at 19 s with 1 at once");
        assertNoQueryPaid(journey.bench, "the estimate");

        // José closes without a tope: «ensayo».
        BatchCloseRequest request = new BatchCloseRequest(new LinkedHashMap<>(), null);
        BatchClosing closing = journey.service.close(request);

        // ensayo [F-CJUEZ-R003, F-CJUEZ-R005]: no query is paid to any judge, the floors that depend on what is missing stay
        // undecided and the report says what each judge is missing.
        assertNoQueryPaid(journey.bench, "the closing without a tope");
        assertEquals(request, closing.getRequest(), "the closing keeps what was asked");
        assertEquals(0, batch.getQueriesSpent(), "nothing was spent");
        assertEquals(DRY_RUN, accounts(closing.getJudges()), "nothing paid, 4 reused of translation-sense, the rest missing");
        assertEquals(Arrays.asList(null, null, null), budgets(closing.getJudges()), "no judge had a tope");
        assertEquals(List.of("quiz-instruction: 7 de 11", "translation-sense: 7 de 11", "mini-theory: 7 de 11"),
                floors(closing.getJudges()), "the four floors above what the batch changed -- the course, A1, Present Simple and"
                        + " «Afirmativas regulares» -- are undecided; the seven of the rest of the course stay decided");
        BatchVerdict verdict = closing.getComparison().getVerdict();
        assertEquals(VerdictKind.UNDECIDABLE, verdict.getKind(), "what is missing leaves the batch undecided");
        assertFalse(detailOf(verdict).contains("juez sin aprobar"), "every judge is approved: " + detailOf(verdict));
        BatchReport report = journey.service.report(batch.getId());
        assertEquals(closing.getJudges(), report.getJudges(), "batch report gives the accounts of the last closing");
    }

    @Test
    @Order(2)
    @Tag("path-2")
    @DisplayName("path-2: José pide la estimación de la tanda, ... → El cierre juzga con cada juez aprobad... [José cierra con un tope por juez o de la tanda] → Ese juez no consulta, sus pisos queda... [Un juez que lee lo que la tanda cambió no tiene su examen aprobado] → failure")
    public void path2_josCierraConUnTopePorJuezODeLaTanda_unJuezQueLeeLoQueLaTandaCambiNoTieneSuExamenAprobado_failure(
            ) {
        // J001, path 2. Same batch, but quiz-instruction never took its exam. José asks for the estimate, which lists it
        // with its reason and leaves it out of the total, and closes with a budget for each judge -- quiz-instruction
        // included, as a judge of the batch. The approved judges are paid what the batch changed; quiz-instruction asks
        // nothing; its floors stay undecided, the verdict names it and the batch cannot be published.
        Journey journey = new Journey();
        journey.bench.withoutApproval.put(CjuezJourneyBench.QUIZ_INSTRUCTION, JudgeExamStatus.NOT_TAKEN);
        CorrectionBatch batch = journey.batch;

        // estimar [F-CJUEZ-R001, F-CJUEZ-R002]
        BatchEstimate estimate = journey.service.estimate();

        assertEquals(2, estimate.getChangedQuizzes(), "the vegetables and «Winter»");
        assertEquals(List.of(NOT_TAKEN_QUIZ_INSTRUCTION, TRANSLATION_SENSE_DRY_RUN, MINI_THEORY_DRY_RUN),
                accounts(estimate.getJudges()), "quiz-instruction is listed with the reason of its exam");
        assertEquals(6, estimate.getJudgeQueries(), "4 of translation-sense and 2 of mini-theory: the judge without an exam does not add");
        assertEquals(0.03, estimate.getJudgeHours(), 1e-9, "6 queries at 19 s with 1 at once");
        assertNoQueryPaid(journey.bench, "the estimate");

        // José closes with a tope for every judge: «pagar».
        Map<String, Integer> budgets = new LinkedHashMap<>();
        budgets.put(CjuezJourneyBench.QUIZ_INSTRUCTION, 600);
        budgets.put(CjuezJourneyBench.TRANSLATION_SENSE, 6);
        budgets.put(CjuezJourneyBench.MINI_THEORY, 3);
        BatchCloseRequest request = new BatchCloseRequest(budgets, null);
        BatchClosing closing = journey.service.close(request);

        // pagar [F-CJUEZ-R001, F-CJUEZ-R003]: each approved judge is paid what the batch changed, once, and no more than its
        // tope; the book's verdicts of the start course are reused and nothing else of the course is judged.
        assertEquals(request, closing.getRequest(), "the closing keeps what was asked");
        List<BatchJudgeAccount> judges = closing.getJudges();
        assertEquals(4, journey.bench.consultationsOf(CjuezJourneyBench.TRANSLATION_SENSE).size(),
                "two evaluators for each of the two exercises, on the side the book did not have");
        assertEquals(Set.of(MesaFixture.VEGETABLES, MesaFixture.COME),
                journey.bench.unitsOf(CjuezJourneyBench.TRANSLATION_SENSE), "no other exercise of the course");
        for (CjuezJourneyBench.Consultation asked : journey.bench.consultationsOf(CjuezJourneyBench.TRANSLATION_SENSE)) {
            assertTrue(asked.read().contains("I ___ [love] vegetables.") || asked.read().contains("Winter ___ [comes]"),
                    "the start of the course was in the book, only the after is paid: " + asked.read());
        }
        assertEquals(2, journey.bench.consultationsOf(CjuezJourneyBench.MINI_THEORY).size(), "its topic before and after");
        assertEquals(Set.of(MesaFixture.AFIRMATIVAS), journey.bench.unitsOf(CjuezJourneyBench.MINI_THEORY),
                "no other topic of the course");
        assertEquals(6, batch.getQueriesSpent(), "what the judges were paid adds to the queries of the batch");
        assertEquals(Arrays.asList(6, 3), budgets(judges).subList(1, 3), "the tope each approved judge had");
        assertTrue(journey.courses.written.isEmpty(), "closing writes no course");
        assertEquals(2, journey.reports.saved.size(), "nor an analysis of the history: only the baseline and the start of the batch");

        // bloqueo [F-CJUEZ-R004]: the judge without an exam asked nothing, whatever its tope; its floors are undecided, the
        // verdict names it and the batch cannot be published.
        assertTrue(journey.bench.consultationsOf(CjuezJourneyBench.QUIZ_INSTRUCTION).isEmpty(),
                "quiz-instruction cannot ask: its exam is not approved, though 600 queries were named for it");
        assertEquals(List.of(NOT_TAKEN_QUIZ_INSTRUCTION, new Account("translation-sense", TRANSLATION_SENSE_NAME,
                AuditTarget.QUIZ, true, JudgeExamStatus.APPROVED, null, 4, 4, 4, 0, false, null),
                new Account("mini-theory", MINI_THEORY_NAME, AuditTarget.KNOWLEDGE, true, JudgeExamStatus.APPROVED, null,
                        2, 2, 0, 0, false, null)), accounts(judges), "the approved judges decided what they were paid");
        assertEquals(List.of("quiz-instruction: 7 de 11", "translation-sense: 11 de 11", "mini-theory: 11 de 11"), floors(judges),
                "only the floors above what quiz-instruction could not judge stay undecided");
        BatchVerdict verdict = closing.getComparison().getVerdict();
        assertEquals(VerdictKind.UNDECIDABLE, verdict.getKind(), "the judge without an exam leaves the batch undecided");
        assertTrue(detailOf(verdict).endsWith("juez sin aprobar: Consigna que no se cumple"),
                "the verdict names it, after everything else: " + detailOf(verdict));
        assertEquals(1, detailOf(verdict).split("juez sin aprobar", -1).length - 1, "once: " + detailOf(verdict));
        assertEquals(closing.getJudges(), journey.service.report(batch.getId()).getJudges(),
                "the report gives the same accounts");
        BatchNotPublishableException refused = assertThrows(BatchNotPublishableException.class,
                () -> journey.service.publish(new BatchPublishRequest("José", null, List.of())),
                "the batch cannot be published while a judge that reads what it changed is not approved");
        assertEquals(batch.getId(), refused.getBatchId());
        assertTrue(refused.getReasons().contains("juez sin aprobar: Consigna que no se cumple"),
                "the refusal copies the detail of the verdict: " + refused.getReasons());
        assertTrue(journey.courses.written.isEmpty(), "nothing was published");
    }

    @Test
    @Order(3)
    @Tag("path-3")
    @DisplayName("path-3: José pide la estimación de la tanda, ... → El cierre juzga con cada juez aprobad... [José cierra con un tope por juez o de la tanda] → El informe dice por juez las consulta... [Todos los jueces que leen lo que cambió están aprobados] → success")
    public void path3_josCierraConUnTopePorJuezODeLaTanda_todosLosJuecesQueLeenLoQueCambiEstnAprobados_success(
            ) {
        // J001, path 3. Same batch, all three judges approved. José asks for the estimate and closes with a tope for the
        // batch: the judges share it in the order of the catalog, each is paid what the batch changed -- before and after,
        // less what the book had -- and the report says, per judge, what it spent, reused, is missing and decided.
        Journey journey = new Journey();
        CorrectionBatch batch = journey.batch;

        // estimar [F-CJUEZ-R001, F-CJUEZ-R002]
        BatchEstimate estimate = journey.service.estimate();

        assertEquals(2, estimate.getChangedQuizzes(), "the vegetables and «Winter»");
        assertEquals(DRY_RUN, accounts(estimate.getJudges()), "what each judge judges, reuses and is missing");
        assertEquals(10, estimate.getJudgeQueries(), "4 of quiz-instruction, 4 of translation-sense and 2 of mini-theory");
        assertEquals(0.05, estimate.getJudgeHours(), 1e-9, "10 queries at 19 s with 1 at once");
        assertNoQueryPaid(journey.bench, "the estimate");

        // José closes with a tope of 12 queries for the batch: «pagar».
        BatchCloseRequest request = new BatchCloseRequest(new LinkedHashMap<>(), 12);
        BatchClosing closing = journey.service.close(request);

        // pagar [F-CJUEZ-R001, F-CJUEZ-R003]: quiz-instruction asks the exercises on both sides, translation-sense only the
        // after, mini-theory its topic on both sides; 10 queries in all, no more than the 12 of the batch.
        assertEquals(request, closing.getRequest());
        List<CjuezJourneyBench.Consultation> instruction = journey.bench.consultationsOf(CjuezJourneyBench.QUIZ_INSTRUCTION);
        assertEquals(4, instruction.size(), "each exercise before and after");
        assertEquals(2, instruction.stream().filter(c -> c.read().contains("a lot of vegetables") || c.read().contains("He ___ [comes]"))
                .count(), "the start of the course: the book had none of quiz-instruction");
        assertEquals(Set.of(MesaFixture.VEGETABLES, MesaFixture.COME), journey.bench.unitsOf(CjuezJourneyBench.QUIZ_INSTRUCTION));
        assertEquals(4, journey.bench.consultationsOf(CjuezJourneyBench.TRANSLATION_SENSE).size(),
                "the after of both exercises with both evaluators: the book had the before");
        assertEquals(Set.of(MesaFixture.VEGETABLES, MesaFixture.COME), journey.bench.unitsOf(CjuezJourneyBench.TRANSLATION_SENSE));
        assertEquals(2, journey.bench.consultationsOf(CjuezJourneyBench.MINI_THEORY).size(), "its topic before and after");
        assertEquals(Set.of(MesaFixture.AFIRMATIVAS), journey.bench.unitsOf(CjuezJourneyBench.MINI_THEORY));
        assertEquals(10, journey.bench.consultations.size(), "nothing else was paid, and less than the 12 of the batch");
        assertEquals(10, batch.getQueriesSpent(), "what the judges were paid adds to the queries of the batch");
        assertTrue(journey.courses.written.isEmpty(), "closing writes no course");
        assertEquals(2, journey.reports.saved.size(), "nor an analysis of the history: only the baseline and the start of the batch");

        // informe [F-CJUEZ-R005]: per judge, spent, reused, missing, the tope it had and its floors decided.
        BatchReport report = journey.service.report(batch.getId());

        assertEquals(List.of(
                new Account("quiz-instruction", QUIZ_INSTRUCTION_NAME, AuditTarget.QUIZ, true, JudgeExamStatus.APPROVED, null, 4, 4, 0, 0,
                        false, null),
                new Account("translation-sense", TRANSLATION_SENSE_NAME, AuditTarget.QUIZ, true, JudgeExamStatus.APPROVED, null, 4, 4, 4,
                        0, false, null),
                new Account("mini-theory", MINI_THEORY_NAME, AuditTarget.KNOWLEDGE, true, JudgeExamStatus.APPROVED, null, 2, 2, 0, 0,
                        false, null)), accounts(report.getJudges()), "what each judge spent, reused and is missing");
        assertEquals(Arrays.asList(12, 8, 4), budgets(report.getJudges()),
                "the 12 of the batch shared in order, each with what the previous ones left");
        assertEquals(List.of("quiz-instruction: 11 de 11", "translation-sense: 11 de 11", "mini-theory: 11 de 11"),
                floors(report.getJudges()), "every floor decided");
        assertEquals(closing.getJudges(), report.getJudges(), "the report of the batch is the one of its closing");
        BatchVerdict verdict = closing.getComparison().getVerdict();
        assertNotEquals(VerdictKind.UNDECIDABLE, verdict.getKind(), "no floor is left undecided by a judge");
        assertFalse(detailOf(verdict).contains("juez sin aprobar"), "every judge is approved: " + detailOf(verdict));
        assertEquals(BatchStatus.CLOSED, batch.getStatus());
    }


    private static final String QUIZ_INSTRUCTION_NAME = "Consigna que no se cumple";

    private static final String TRANSLATION_SENSE_NAME = "Traducción que no corresponde";

    private static final String MINI_THEORY_NAME = "Miniteoría con error";

    /** quiz-instruction that never took its exam: it judges the four exercises and has nothing paid nor reused. */
    private static final Account NOT_TAKEN_QUIZ_INSTRUCTION = new Account("quiz-instruction", QUIZ_INSTRUCTION_NAME,
            AuditTarget.QUIZ, false, JudgeExamStatus.NOT_TAKEN, CjuezJourneyBench.NOT_TAKEN_REASON, 4, 0, 0, 4, false, null);

    private static final Account TRANSLATION_SENSE_DRY_RUN = new Account("translation-sense", TRANSLATION_SENSE_NAME,
            AuditTarget.QUIZ, true, JudgeExamStatus.APPROVED, null, 4, 0, 4, 4, false, null);

    private static final Account MINI_THEORY_DRY_RUN = new Account("mini-theory", MINI_THEORY_NAME, AuditTarget.KNOWLEDGE, true,
            JudgeExamStatus.APPROVED, null, 2, 0, 0, 2, false, null);

    /**
     * What the three approved judges say before anything is paid: quiz-instruction and mini-theory have nothing in the book,
     * translation-sense has the start of the course, so its two evaluators reuse the before of both exercises and miss the after.
     */
    private static final List<Account> DRY_RUN = List.of(
            new Account("quiz-instruction", QUIZ_INSTRUCTION_NAME, AuditTarget.QUIZ, true, JudgeExamStatus.APPROVED, null, 4, 0, 0, 4,
                    false, null),
            TRANSLATION_SENSE_DRY_RUN, MINI_THEORY_DRY_RUN);

    /** What an account says of a judge, without its floors nor its tope. */
    private record Account(String judge, String displayName, AuditTarget unit, boolean approved, JudgeExamStatus examStatus,
            String reason, int units, int spent, int reused, int missing, boolean local, String unavailableReason) {
    }

    private static List<Account> accounts(List<BatchJudgeAccount> judges) {
        assertNotNull(judges, "the account of each judge");
        List<Account> accounts = new ArrayList<>();
        for (BatchJudgeAccount judge : judges) {
            accounts.add(new Account(judge.getJudge(), judge.getDisplayName(), judge.getUnit(), judge.isApproved(),
                    judge.getExamStatus(), judge.getReason(), judge.getUnits(), judge.getQueriesSpent(),
                    judge.getQueriesReused(), judge.getQueriesMissing(), judge.isLocal(), judge.getUnavailableReason()));
        }
        return accounts;
    }

    private static List<Integer> budgets(List<BatchJudgeAccount> judges) {
        List<Integer> budgets = new ArrayList<>();
        for (BatchJudgeAccount judge : judges) {
            budgets.add(judge.getBudget());
        }
        return budgets;
    }

    /** The floors of each judge as the report says them: «decided of all its floors». */
    private static List<String> floors(List<BatchJudgeAccount> judges) {
        List<String> floors = new ArrayList<>();
        for (BatchJudgeAccount judge : judges) {
            floors.add(judge.getJudge() + ": " + judge.getFloorsDecided() + " de "
                    + (judge.getFloorsDecided() + judge.getFloorsUndecided()));
        }
        return floors;
    }

    private static String detailOf(BatchVerdict verdict) {
        return verdict.getDetail() != null ? verdict.getDetail() : "";
    }

    /** No judge was asked a thing, and every run of the engine gave each judge a budget of 0. */
    private static void assertNoQueryPaid(CjuezJourneyBench bench, String when) {
        assertTrue(bench.consultations.isEmpty(), when + " paid " + bench.consultations);
        for (AnalyzerRunSelection selection : bench.selections) {
            Map<String, EvaluationRunPolicy> policies = selection.getPolicies() != null ? selection.getPolicies() : Map.of();
            for (Map.Entry<String, EvaluationRunPolicy> policy : policies.entrySet()) {
                assertEquals(0, policy.getValue().getMaxNewEvaluations(), when + ": " + policy.getKey() + " ran with a budget");
            }
        }
    }

    /**
     * The batch of A1 of the control, closing with the judges: the service the factory builds over the course in memory, with
     * the real selector, rules, panel stage, ledger, gate, closer, judges' pass, publisher and F's comparison, over the judges
     * of {@link CjuezJourneyBench} -- the only analyzers of the batch, with a book that holds the verdicts of the analysis of
     * 29/9. What stays by hand is what the batch does not own: the stores, the blind sample, and the projection, which measures
     * the start course and the course with the batch's changes with every judge at 0, so each one only reuses its book.
     */
    private static final class Journey {
        final CjuezJourneyBench bench = new CjuezJourneyBench();

        final TandaFixture.Batches batches = new TandaFixture.Batches();

        final TandaFixture.Artifacts artifacts = new TandaFixture.Artifacts();

        final TandaFixture.Courses courses = new TandaFixture.Courses(TandaFixture.course());

        /** The history of analyses the service reads and writes: the baseline and the start of the batch. */
        final TandaFixture.Reports reports = new TandaFixture.Reports();

        final CorrectionBatch batch;

        final BatchService service;

        Journey() {
            CourseEntity course = courses.course;
            // The book as the analysis of 29/9 left it: translation-sense judged the whole course; the others judged nothing.
            bench.judgeCourse(course, CjuezJourneyBench.TRANSLATION_SENSE);
            reports.put(TandaFixture.BASELINE, TandaFixture.report(course, List.of(MesaFixture.anotherAnswer(), comeFinding()),
                    Map.of(), MesaFixture.ANALYZERS));
            reports.put(TandaFixture.START_AUDIT, reports.load(TandaFixture.BASELINE).orElseThrow());
            TandaFixture.Plans plans = new TandaFixture.Plans(TandaFixture.plan(
                    TandaFixture.task("t-respuesta", MesaFixture.anotherAnswer(), MesaFixture.AFIRMATIVAS, CefrLevel.A1),
                    TandaFixture.task("t-come", comeFinding(), MesaFixture.AFIRMATIVAS, CefrLevel.A1)));
            this.batch = batchAfterItsRules(course);
            batches.save(batch);
            TandaFixture.Baselines baselines = new TandaFixture.Baselines(new Baseline(TandaFixture.BASELINE,
                    Instant.parse("2026-09-29T08:00:00Z")));
            BatchServiceConfig config = new BatchServiceConfig(batches, plans, reports, new TandaFixture.ActivePair(), artifacts,
                    courses, TandaFixture.locator(), bench.courseMapper(), bench.engine(), refiner(), TandaFixture.briefer(),
                    MesaFixture.corrections(), List.of(), new TandaFixture.Panels(), new TandaFixture.Meter(),
                    new DefaultPanelRunnerFactory(), comparator(baselines, course), baselines, new TandaFixture.Samples(), null,
                    TandaFixture.correctionScope(), new DefaultPreservationFactory().createCheck(), TandaFixture.COURSE_PATH,
                    new TandaFixture.Goals(), bench.catalog());
            config.setProductionDocumentProjector(
                    new com.learney.contentaudit.revisiondomain.publicationengine.EntityProjector());
            this.service = new DefaultBatchServiceFactory().create(config);
        }

        DefaultBatchChangeLedger ledger() {
            return new DefaultBatchChangeLedger(artifacts, TandaFixture.locator());
        }

        /** «He ___ (come) every year.» has another answer that fits: the task the panel took on. */
        private static Finding comeFinding() {
            return MesaFixture.finding("single-answer", "different-valid-answer", MesaFixture.COME, FindingSeverity.BLOCKING,
                    FindingResolution.PANEL, null, "«goes» también encaja");
        }

        /**
         * The batch after its rules and its panels: the vegetables without «a lot of», and «Winter ___ (come) every year.»,
         * both recorded through the real ledger as pending proposals -- so each changes what every judge reads of it.
         */
        private CorrectionBatch batchAfterItsRules(CourseEntity course) {
            CorrectionBatch batch = TandaFixture.batch("tanda-a1", "t-respuesta", "t-come");
            batch.setStatus(BatchStatus.RUNNING);
            batch.setAnalyzers(new ArrayList<>(CjuezJourneyBench.JUDGES));
            BatchChange vegetables = TandaFixture.change("mesa-" + MesaFixture.VEGETABLES, BatchChangeOrigin.PANEL, null,
                    List.of("t-respuesta"), TandaFixture.step(withParts(TandaFixture.exercise(course, MesaFixture.VEGETABLES),
                            text("I"), cloze("love"), text("vegetables.")), ExercisePart.STATEMENT));
            vegetables.setGateVerdict(new ChangeGateVerdict(true, List.of(), List.of(), List.of(), List.of(), List.of(),
                    List.of()));
            ledger().record(batch, vegetables, course);
            BatchBook.setOutcome(batch, "t-respuesta", BatchTaskOutcomeKind.RESOLVED_BY_PANEL, vegetables.getChangeId(), 1, null);
            BatchChange winter = TandaFixture.change("mesa-" + MesaFixture.COME, BatchChangeOrigin.PANEL, null,
                    List.of("t-come"), TandaFixture.step(withParts(TandaFixture.exercise(course, MesaFixture.COME),
                            text("Winter"), cloze("comes"), text("(come) every year.")), ExercisePart.STATEMENT));
            winter.setGateVerdict(new ChangeGateVerdict(true, List.of(), List.of(), List.of(), List.of(), List.of(),
                    List.of()));
            ledger().record(batch, winter, course);
            BatchBook.setOutcome(batch, "t-come", BatchTaskOutcomeKind.RESOLVED_BY_PANEL, winter.getChangeId(), 0, null);
            return batch;
        }

        /**
         * F's comparison, the real one, with its real book of goals -- the floors of the family of errors on every node --
         * over the judges of the bench: the projector measures the start of the batch and the course with its changes, and the
         * baseline is the analysis of 29/9 as the judges' book held it then.
         */
        private BatchComparator comparator(TandaFixture.Baselines baselines, CourseEntity course) {
            TandaFixture.Reports analyses = new TandaFixture.Reports();
            analyses.put(TandaFixture.BASELINE, bench.measure(course));
            CourseProjector projector = (coursePath, scope) -> {
                AuditReport approved = bench.measure(courses.course);
                List<ProjectedChange> changes = new ArrayList<>();
                for (String proposalId : scope.getProposalIds()) {
                    changes.add(new ProjectedChange(proposalId, AuditTarget.QUIZ, quizOf(proposalId), true));
                }
                return new CourseProjection(TandaFixture.START_AUDIT, TandaFixture.PLAN, scope.getScopeId(),
                        ConsolidatedViewAvailability.AVAILABLE, null, approved, approved,
                        bench.measure(ledger().composedCourse(batch, courses.course)), changes, new ArrayList<>());
            };
            List<SealedGoals> sealed = new ArrayList<>();
            SealedGoalsStore sealedStore = new SealedGoalsStore() {
                @Override
                public Optional<SealedGoals> read() {
                    return sealed.isEmpty() ? Optional.empty() : Optional.of(sealed.get(sealed.size() - 1));
                }

                @Override
                public void write(SealedGoals goals) {
                    sealed.add(goals);
                }
            };
            GoalDeclaration declaration = new GoalDeclaration(List.of(), List.of(), null);
            return new DefaultBatchComparator(projector, baselines, analyses,
                    new DefaultGoalBook(() -> declaration, sealedStore, bench.catalog()), bench.catalog(), bench.engine(),
                    new LeaveOneOutChangeAttributor(bench.engine()), scope -> Optional.empty(),
                    new DefaultTopicTeachingResolver(),
                    new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create()));
        }

        private String quizOf(String proposalId) {
            for (BatchProposalRef proposal : batch.getQuizProposals()) {
                if (proposal.getProposalId().equals(proposalId)) {
                    return proposal.getQuizId();
                }
            }
            throw new AssertionError("no proposal " + proposalId + " in " + batch.getQuizProposals());
        }

        /** The publisher analyzes the course it publishes; the one publication of this journey is refused before that. */
        private static RefinerEngine refiner() {
            return new RefinerEngine() {
                @Override
                public RefinementPlan plan(AuditReport report, String auditId) {
                    throw new AssertionError("no publication of this journey gets to analyze the course");
                }

                @Override
                public Optional<RefinementTask> nextTask(RefinementPlan plan) {
                    return Optional.empty();
                }

                @Override
                public List<UnconvertedScoreCount> unconvertedScores(AuditReport report) {
                    return List.of();
                }

                @Override
                public List<PlanFindingTally> findingTally(AuditReport report, RefinementPlan plan) {
                    return List.of();
                }
            };
        }
    }
}
