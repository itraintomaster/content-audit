package com.learney.contentaudit.revisiondomain.comparisonengine;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.learney.contentaudit.auditdomain.AnalyzerCatalog;
import com.learney.contentaudit.auditdomain.AnalyzerDescriptor;
import com.learney.contentaudit.auditdomain.AuditEngine;
import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditReportStore;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableCourse;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableMilestone;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.AuditableTopic;
import com.learney.contentaudit.auditdomain.Baseline;
import com.learney.contentaudit.auditdomain.BaselineStore;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerFamily;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerMeasure;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.contextnumbers.AnalyzerErrorCounts;
import com.learney.contentaudit.auditdomain.contextnumbers.AnalyzerScore;
import com.learney.contentaudit.auditdomain.contextnumbers.ContextNumbers;
import com.learney.contentaudit.auditdomain.contextnumbers.ErrorCounts;
import com.learney.contentaudit.auditdomain.finding.AnalysisCost;
import com.learney.contentaudit.auditdomain.finding.EvidencePart;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingEvidence;
import com.learney.contentaudit.auditdomain.finding.FindingIdentity;
import com.learney.contentaudit.auditdomain.finding.FindingNodeRef;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.auditdomain.offlevelwords.DefaultTopicTeachingResolver;
import com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer;
import com.learney.contentaudit.coursedomain.SentenceMode;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.revisiondomain.SealedGoalsStore;
import com.learney.contentaudit.revisiondomain.blindsample.SampleOutcome;
import com.learney.contentaudit.revisiondomain.blindsample.SampleOutcomeSource;
import com.learney.contentaudit.revisiondomain.comparison.BatchComparison;
import com.learney.contentaudit.revisiondomain.comparison.BetterDirection;
import com.learney.contentaudit.revisiondomain.comparison.ChangeAttribution;
import com.learney.contentaudit.revisiondomain.comparison.ChangeOutcome;
import com.learney.contentaudit.revisiondomain.comparison.ChangeVerdict;
import com.learney.contentaudit.revisiondomain.comparison.ComparisonRow;
import com.learney.contentaudit.revisiondomain.comparison.FamilyFloor;
import com.learney.contentaudit.revisiondomain.comparison.FloorBreach;
import com.learney.contentaudit.revisiondomain.comparison.Goal;
import com.learney.contentaudit.revisiondomain.comparison.GoalChange;
import com.learney.contentaudit.revisiondomain.comparison.GoalDeclaration;
import com.learney.contentaudit.revisiondomain.comparison.GoalKind;
import com.learney.contentaudit.revisiondomain.comparison.GoalOrigin;
import com.learney.contentaudit.revisiondomain.comparison.GoalReference;
import com.learney.contentaudit.revisiondomain.comparison.GoalStatus;
import com.learney.contentaudit.revisiondomain.comparison.GoalUnit;
import com.learney.contentaudit.revisiondomain.comparison.MeasureChange;
import com.learney.contentaudit.revisiondomain.comparison.RowExample;
import com.learney.contentaudit.revisiondomain.comparison.RowValue;
import com.learney.contentaudit.revisiondomain.comparison.SealedGoals;
import com.learney.contentaudit.revisiondomain.comparison.VerdictKind;
import com.learney.contentaudit.revisiondomain.consolidatedview.ConsolidatedViewAvailability;
import com.learney.contentaudit.revisiondomain.projection.CourseProjection;
import com.learney.contentaudit.revisiondomain.projection.CourseProjector;
import com.learney.contentaudit.revisiondomain.projection.ProjectedChange;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.processing.Generated;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

@Generated(
        value = "com.sentinel.SentinelEngine",
        comments = "Generated by Sentinel"
)
public class DefaultBatchComparatorTest {
    @Test
    @DisplayName("should publish for every analyzer that ran in the baseline a row in each knowledge, topic, level and the course, plus a vocabulary row and an any-error row per node")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R001")
    public void shouldPublishForEveryAnalyzerThatRanInTheBaselineARowInEachKnowledgeTopicLevelAndTheCoursePlusAVocabularyRowAndAnAnyerrorRowPerNode(
            ) {
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.batch(Foto.deHoy(), Foto.deHoy());

        BatchComparison comparison = mesa.compare();

        List<String> subjects = List.of(LEMMA_ABSENCE, LEMMA_COUNT, HINT, INSTRUCTION, "vocabulario", "algún error");
        Map<String, List<String>> byNode = new LinkedHashMap<>();
        for (ComparisonRow row : comparison.getRows()) {
            assertNotEquals(AuditTarget.QUIZ, row.getNodeTarget(), "R001: an exercise has no row of its own");
            byNode.computeIfAbsent(row.getNodeTarget() + "|" + row.getNodeId(), k -> new ArrayList<>())
                    .add(row.getSubject());
        }
        assertEquals(List.of("COURSE|root", "MILESTONE|" + A1, "TOPIC|" + PRESENT_SIMPLE, "KNOWLEDGE|" + WITH_BE,
                "KNOWLEDGE|" + REGULAR, "MILESTONE|" + A2, "TOPIC|" + A2_TOPIC, "KNOWLEDGE|" + A2_KNOWLEDGE,
                "MILESTONE|" + B1, "TOPIC|" + B1_TOPIC, "KNOWLEDGE|" + INVEST), new ArrayList<>(byNode.keySet()),
                "R001: every knowledge, topic, level and the course, in the order of the course");
        for (Map.Entry<String, List<String>> node : byNode.entrySet()) {
            assertEquals(subjects, node.getValue(), "R001: every analyzer that ran, «vocabulario» and «algún error» in "
                    + node.getKey());
        }
        assertEquals("Curso", row(comparison, "vocabulario", ROOT).getLabel(), "R001: the course, by its name");
        assertEquals("Afirmativas regulares", row(comparison, LEMMA_ABSENCE, REGULAR).getLabel());
        assertEquals(AnalyzerFamily.ERRORS, row(comparison, "algún error", A1).getFamily());
        assertEquals(AnalyzerFamily.VOCABULARY, row(comparison, "vocabulario", A1).getFamily());
    }

    @Test
    @DisplayName("should copy into each row the number of the baseline, of the batch start and of the projection from their reports, without computing it again")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R001")
    public void shouldCopyIntoEachRowTheNumberOfTheBaselineOfTheBatchStartAndOfTheProjectionFromTheirReportsWithoutComputingItAgain(
            ) {
        // The vocabulary score of A1 is not the average of the two scores of A1 in any photo: a row that
        // recomputed it would not give these numbers.
        Mesa mesa = new Mesa().baseline(Foto.deHoy().vocabulary(A1, 0.9654).errors(A1, HINT, 3, 0).anyError(A1, 6, 4, 0));
        mesa.batch(Foto.deHoy().vocabulary(A1, 0.9650).errors(A1, HINT, 2, 0).anyError(A1, 6, 3, 0),
                Foto.deHoy().vocabulary(A1, 0.9611).errors(A1, HINT, 1, 1).anyError(A1, 6, 2, 1),
                cambio(P_VEGETABLES, VEGETABLES));

        BatchComparison comparison = mesa.compare();

        ComparisonRow vocabulary = row(comparison, "vocabulario", A1);
        assertEquals(new RowValue(0.9654, null, 0), vocabulary.getBaseline(), "R001: the baseline, as published");
        assertEquals(new RowValue(0.9650, null, 0), vocabulary.getApproved(), "R001: the batch start, as published");
        assertEquals(new RowValue(0.9611, null, 0), vocabulary.getProjected(), "R001: the projection, as published");
        ComparisonRow hint = row(comparison, HINT, A1);
        assertEquals(3.0, hint.getBaseline().getValue(), 1e-9);
        assertEquals(2.0, hint.getApproved().getValue(), 1e-9);
        assertEquals(1.0, hint.getProjected().getValue(), 1e-9);
        assertEquals(1, hint.getProjected().getNotEvaluated(), "R001: with what it left unevaluated");
        ComparisonRow any = row(comparison, "algún error", A1);
        assertEquals(4.0, any.getBaseline().getValue(), 1e-9);
        assertEquals(3.0, any.getApproved().getValue(), 1e-9);
        assertEquals(2.0, any.getProjected().getValue(), 1e-9);
        assertEquals(1, any.getProjected().getNotEvaluated());
        verifyNoInteractions(mesa.engine);
    }

    @Test
    @DisplayName("should give lemma-absence on «Afirmativas regulares» 100.0 before, 99.5 after and -0.5 points, and on the topic Present Simple 99.55, 99.52 and -0.03 points, with the four pending proposals")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R001")
    public void shouldGiveLemmaabsenceOnAfirmativasRegulares1000Before995AfterAnd05PointsAndOnTheTopicPresentSimple99559952And003PointsWithTheFourPendingProposals(
            ) {
        Mesa mesa = new Mesa().baseline(Foto.del299());
        mesa.batch(Foto.del299(), Foto.del299().conLasCuatro(), LAS_CUATRO);

        BatchComparison comparison = mesa.compare();

        assertEquals(List.of(P_GRANDMOTHER, P_OURSELVES, P_VEGETABLES, P_HATS), comparison.getAppliedProposalIds());
        ComparisonRow knowledge = row(comparison, LEMMA_ABSENCE, REGULAR);
        assertEquals(100.0, knowledge.getBaseline().getValue() * 100, 1e-9, "R001: before");
        assertEquals(99.5, knowledge.getProjected().getValue() * 100, 1e-9, "R001: after");
        assertEquals(-0.5, knowledge.getChange(), 1e-9, "R001: the change in points");
        assertEquals(GoalUnit.POINTS, knowledge.getUnit());
        ComparisonRow topic = row(comparison, LEMMA_ABSENCE, PRESENT_SIMPLE);
        assertEquals(99.55, topic.getBaseline().getValue() * 100, 0.005);
        assertEquals(99.52, topic.getProjected().getValue() * 100, 0.005);
        assertEquals(-0.03, topic.getChange(), 0.005);
        assertEquals(GoalUnit.POINTS, topic.getUnit());
    }

    @Test
    @DisplayName("should give the vocabulary rows of A1 and of the course 96.54 and 73.92 before and after, a change of 0.0 points, with the four pending proposals")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R001")
    public void shouldGiveTheVocabularyRowsOfA1AndOfTheCourse9654And7392BeforeAndAfterAChangeOf00PointsWithTheFourPendingProposals(
            ) {
        Mesa mesa = new Mesa().baseline(Foto.del299());
        mesa.batch(Foto.del299(), Foto.del299().conLasCuatro(), LAS_CUATRO);

        BatchComparison comparison = mesa.compare();

        ComparisonRow a1 = row(comparison, "vocabulario", A1);
        assertEquals(96.54, a1.getBaseline().getValue() * 100, 1e-9);
        assertEquals(96.54, a1.getProjected().getValue() * 100, 1e-9);
        assertEquals(0.0, a1.getChange(), 1e-9, "R001: no word of the four moves A1");
        ComparisonRow course = row(comparison, "vocabulario", ROOT);
        assertEquals(73.92, course.getBaseline().getValue() * 100, 1e-9);
        assertEquals(73.92, course.getProjected().getValue() * 100, 1e-9);
        assertEquals(0.0, course.getChange(), 1e-9);
        assertEquals(GoalUnit.POINTS, course.getUnit());
        assertEquals(GoalStatus.MET, course.getFloor().getStatus());
    }

    @Test
    @DisplayName("should count in an error row the exercises with an error, in number and in share, and give its change in exercises")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R001")
    public void shouldCountInAnErrorRowTheExercisesWithAnErrorInNumberAndInShareAndGiveItsChangeInExercises() {
        Mesa mesa = new Mesa().baseline(Foto.deHoy().errors(REGULAR, HINT, 3, 0));
        mesa.batch(Foto.deHoy().errors(REGULAR, HINT, 2, 0), Foto.deHoy().errors(REGULAR, HINT, 1, 0),
                cambio(P_VEGETABLES, VEGETABLES));

        BatchComparison comparison = mesa.compare();

        ComparisonRow hint = row(comparison, HINT, REGULAR);
        assertEquals(new RowValue(3.0, 0.75, 0), hint.getBaseline(), "R001: 3 of the 4 exercises");
        assertEquals(new RowValue(2.0, 0.5, 0), hint.getApproved());
        assertEquals(new RowValue(1.0, 0.25, 0), hint.getProjected());
        assertEquals(-2.0, hint.getChange(), 1e-9, "R001: two exercises fewer than the baseline");
        assertEquals(GoalUnit.EXERCISES, hint.getUnit(), "R001: in exercises");
    }

    @Test
    @DisplayName("should leave empty, and not at zero, the error rows of a baseline that ran no error analyzer, as the base of 29/9")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R001")
    public void shouldLeaveEmptyAndNotAtZeroTheErrorRowsOfABaselineThatRanNoErrorAnalyzerAsTheBaseOf299() {
        Mesa mesa = new Mesa().baseline(Foto.del299());
        mesa.batch(Foto.deHoy(), Foto.deHoy(), cambio(P_VEGETABLES, VEGETABLES));

        BatchComparison comparison = mesa.compare();

        for (String subject : List.of(HINT, INSTRUCTION, "algún error")) {
            ComparisonRow row = row(comparison, subject, A1);
            assertNull(row.getBaseline(), "R001: the base of 29/9 did not measure " + subject + ": empty, not 0");
            assertNull(row.getChange(), "R001: and no change against it");
            assertEquals(new RowValue(0.0, 0.0, 0), row.getProjected(), "R001: the projection did measure it");
            assertEquals(GoalStatus.MET, row.getFloor().getStatus(),
                    "R005: the floor of errors is decided against the batch start");
        }
        assertEquals(new RowValue(0.9654, null, 0), row(comparison, "vocabulario", A1).getBaseline(),
                "R001: what it did measure is there");
    }

    @Test
    @DisplayName("should attach to each row the floor and the progress goal that apply to it, with their threshold, unit and reference")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R002")
    public void shouldAttachToEachRowTheFloorAndTheProgressGoalThatApplyToItWithTheirThresholdUnitAndReference() {
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.declaration = new GoalDeclaration(List.of(), List.of(
                new Goal("avance · hint-coherence · niveles", GoalKind.PROGRESS, HINT, AuditTarget.MILESTONE, null, 0.0,
                        GoalUnit.EXERCISES, GoalReference.ABSOLUTE, GoalOrigin.DECLARED),
                new Goal("avance · algún error · curso", GoalKind.PROGRESS, "algún error", AuditTarget.COURSE, null, 5.0,
                        GoalUnit.PERCENT, GoalReference.ABSOLUTE, GoalOrigin.DECLARED)), null);
        mesa.batch(Foto.deHoy(), Foto.deHoy().errors(A1, HINT, 2, 0).anyError(ROOT, 100, 26, 0),
                cambio(P_VEGETABLES, VEGETABLES));

        BatchComparison comparison = mesa.compare();

        ComparisonRow vocabulary = row(comparison, "vocabulario", A1);
        assertEquals("piso de vocabulario · vocabulario · MILESTONE", vocabulary.getFloor().getGoalId());
        assertEquals(0.5, vocabulary.getFloor().getThreshold(), 1e-9);
        assertEquals(GoalUnit.POINTS, vocabulary.getFloor().getUnit());
        assertEquals(GoalReference.BASELINE, vocabulary.getFloor().getReference());
        assertEquals(GoalStatus.MET, vocabulary.getFloor().getStatus());
        assertNull(vocabulary.getProgress(), "R002: no progress goal for it");

        ComparisonRow hint = row(comparison, HINT, A1);
        assertEquals("piso de errores · hint-coherence · MILESTONE", hint.getFloor().getGoalId());
        assertEquals(0.0, hint.getFloor().getThreshold(), 1e-9);
        assertEquals(GoalUnit.EXERCISES, hint.getFloor().getUnit());
        assertEquals(GoalReference.BATCH_START, hint.getFloor().getReference());
        assertEquals(GoalStatus.NOT_MET, hint.getFloor().getStatus(), "R002: two errors more than the batch start");
        assertEquals("avance · hint-coherence · niveles", hint.getProgress().getGoalId());
        assertEquals(GoalReference.ABSOLUTE, hint.getProgress().getReference());
        assertEquals(GoalStatus.NOT_MET, hint.getProgress().getStatus(), "R002: 2 against a goal of 0");

        ComparisonRow course = row(comparison, "algún error", ROOT);
        assertEquals("avance · algún error · curso", course.getProgress().getGoalId());
        assertEquals(5.0, course.getProgress().getThreshold(), 1e-9);
        assertEquals(GoalUnit.PERCENT, course.getProgress().getUnit());
        assertEquals(GoalStatus.NOT_MET, course.getProgress().getStatus(), "R002: 26 % against a goal of 5 %");

        assertNull(row(comparison, "vocabulario", REGULAR).getFloor(),
                "R002: the vocabulary floor is of levels and the course, not of a knowledge");
        assertEquals("piso de errores · algún error · KNOWLEDGE", row(comparison, "algún error", REGULAR).getFloor()
                .getGoalId(), "R002: the errors floor is of every node");
    }

    @Test
    @DisplayName("should mark undecidable the any-error row of A1 with goal 0 when 30 exercises of A1 are unjudged and none of the judged ones has an error")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R002")
    public void shouldMarkUndecidableTheAnyerrorRowOfA1WithGoal0When30ExercisesOfA1AreUnjudgedAndNoneOfTheJudgedOnesHasAnError(
            ) {
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.declaration = new GoalDeclaration(List.of(), List.of(ANY_ERROR_IN_A1_ZERO), null);
        mesa.batch(Foto.deHoy(), Foto.deHoy().anyError(A1, 120, 0, 30), cambio(P_VEGETABLES, VEGETABLES));

        BatchComparison comparison = mesa.compare();

        ComparisonRow row = row(comparison, "algún error", A1);
        assertEquals("algún error en A1: 0", row.getFloor().getGoalId(), "R002: the goal of A1, over the family's");
        assertEquals(30, row.getProjected().getNotEvaluated());
        assertEquals(GoalStatus.UNDECIDABLE, row.getFloor().getStatus(),
                "R002: 0 errors among the judged, but 30 unjudged could have one");
    }

    @Test
    @DisplayName("should mark met that row once those 30 exercises are judged and none has an error")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R002")
    public void shouldMarkMetThatRowOnceThose30ExercisesAreJudgedAndNoneHasAnError() {
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.declaration = new GoalDeclaration(List.of(), List.of(ANY_ERROR_IN_A1_ZERO), null);
        mesa.batch(Foto.deHoy(), Foto.deHoy().anyError(A1, 120, 0, 0), cambio(P_VEGETABLES, VEGETABLES));

        BatchComparison comparison = mesa.compare();

        ComparisonRow row = row(comparison, "algún error", A1);
        assertEquals(0, row.getProjected().getNotEvaluated());
        assertEquals(GoalStatus.MET, row.getFloor().getStatus(), "R002: judged, and none with an error");
    }

    @Test
    @DisplayName("should mark not met a floor that the evaluated exercises already break, whatever the unevaluated ones would give")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R002")
    public void shouldMarkNotMetAFloorThatTheEvaluatedExercisesAlreadyBreakWhateverTheUnevaluatedOnesWouldGive() {
        // Against the batch start: 2 → 3 with 10 unjudged; as a value: 1 with an error and 30 unjudged.
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.declaration = new GoalDeclaration(List.of(), List.of(ANY_ERROR_IN_A1_ZERO), null);
        mesa.batch(Foto.deHoy().anyError(A1, 120, 2, 10).anyError(A2, 40, 2, 10),
                Foto.deHoy().anyError(A1, 120, 1, 30).anyError(A2, 40, 3, 10)
                        .unevaluated(VEGETABLES, INSTRUCTION),
                cambio(P_VEGETABLES, VEGETABLES));

        BatchComparison comparison = mesa.compare();

        assertEquals(GoalStatus.NOT_MET, row(comparison, "algún error", A1).getFloor().getStatus(),
                "R002: one error already breaks a goal of 0, whatever the 30 unjudged give");
        ComparisonRow a2 = row(comparison, "algún error", A2);
        assertEquals("piso de errores · algún error · MILESTONE", a2.getFloor().getGoalId());
        assertEquals(GoalStatus.NOT_MET, a2.getFloor().getStatus(),
                "R002: one error more than the batch start breaks the floor, whatever the 10 unjudged give");
    }

    @Test
    @DisplayName("should return «My grandmother ___ (come) every year.» with the reason «lemma-absence · palabra de otro nivel: grandmother es A2», and none of the other three proposals of «Afirmativas regulares»")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R003")
    public void shouldReturnMyGrandmotherComeEveryYearWithTheReasonLemmaabsencePalabraDeOtroNivelGrandmotherEsA2AndNoneOfTheOtherThreeProposalsOfAfirmativasRegulares(
            ) {
        Mesa mesa = new Mesa().baseline(Foto.del299());
        mesa.batch(Foto.del299(), Foto.del299().conLasCuatro(), LAS_CUATRO);

        BatchComparison comparison = mesa.compare();

        assertEquals(4, comparison.getChanges().size());
        ChangeOutcome grandmother = change(comparison, P_GRANDMOTHER);
        assertTrue(grandmother.isReturns(), "R003: «grandmother» is A2 in an exercise of A1");
        assertEquals(AuditTarget.QUIZ, grandmother.getNodeTarget());
        assertEquals(GRANDMOTHER, grandmother.getNodeId());
        assertEquals(1, grandmother.getNewFindings().size(), "R003: with its finding as the reason");
        Finding reason = grandmother.getNewFindings().get(0);
        assertEquals(LEMMA_ABSENCE, reason.getAnalyzer());
        assertEquals("misplaced-word", reason.getRule(), "R003: palabra de otro nivel");
        assertEquals("grandmother", reason.getIdentity().getMarker());
        assertEquals("A2", part(reason, "Nivel de la palabra"), "R003: grandmother es A2");
        for (String other : List.of(P_OURSELVES, P_VEGETABLES, P_HATS)) {
            ChangeOutcome outcome = change(comparison, other);
            assertFalse(outcome.isReturns(), "R003: " + other + " gains nothing that counts");
            assertTrue(outcome.getNewFindings().isEmpty());
        }
    }

    @Test
    @DisplayName("should never return a change whose only new finding is of sentence length, as «He isn't in the living-room.» with 9 tokens against 3 to 8 in A1")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R003")
    public void shouldNeverReturnAChangeWhoseOnlyNewFindingIsOfSentenceLengthAsHeIsntInTheLivingroomWith9TokensAgainst3To8InA1(
            ) {
        String sentence = "He isn't in the living-room.";
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.batch(Foto.deHoy(), Foto.deHoy().rewrite(LIVING_ROOM, sentence)
                .finding(largo(LIVING_ROOM, sentence, FindingSeverity.LOW)), cambio(P_LIVING_ROOM, LIVING_ROOM));
        assertFalse(mesa.compare().getChanges().get(0).isReturns(), "R003: 9 tokens against 3 to 8 does not return");

        // Not even if the length were declared of medium severity: the length never returns a change.
        Mesa medium = new Mesa().baseline(Foto.deHoy());
        medium.batch(Foto.deHoy(), Foto.deHoy().rewrite(LIVING_ROOM, sentence)
                .finding(largo(LIVING_ROOM, sentence, FindingSeverity.MEDIUM)), cambio(P_LIVING_ROOM, LIVING_ROOM));
        ChangeOutcome outcome = medium.compare().getChanges().get(0);
        assertFalse(outcome.isReturns(), "R003: the length counts only in the floor of the level");
        assertTrue(outcome.getNewFindings().isEmpty());
    }

    @Test
    @DisplayName("should not return a change for a word its knowledge teaches, as invest in «$Invest time$ + gerundio», although invest is B2")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R003")
    public void shouldNotReturnAChangeForAWordItsKnowledgeTeachesAsInvestInInvestTimeGerundioAlthoughInvestIsB2() {
        String invest = "I invest time reading.";
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.batch(Foto.deHoy(), Foto.deHoy().rewrite(INVEST_QUIZ, invest)
                .finding(palabraDeOtroNivel(INVEST_QUIZ, invest, "invest", "B2", "B1")), cambio(P_INVEST, INVEST_QUIZ));
        ChangeOutcome taught = mesa.compare().getChanges().get(0);
        assertFalse(taught.isReturns(), "R003: «$Invest time$ + gerundio» teaches invest");

        // The same knowledge, with a word it does not teach: that one returns.
        String fortnight = "I spend a fortnight reading.";
        Mesa other = new Mesa().baseline(Foto.deHoy());
        other.batch(Foto.deHoy(), Foto.deHoy().rewrite(INVEST_QUIZ, fortnight)
                .finding(palabraDeOtroNivel(INVEST_QUIZ, fortnight, "fortnight", "B2", "B1")),
                cambio(P_INVEST, INVEST_QUIZ));
        ChangeOutcome notTaught = other.compare().getChanges().get(0);
        assertTrue(notTaught.isReturns(), "R003: fortnight is B2 and the topic does not teach it");
        assertEquals("fortnight", notTaught.getNewFindings().get(0).getIdentity().getMarker());
    }

    @Test
    @DisplayName("should return a change that gains a finding of the errors family, and not one that gains a finding of an analyzer that only orders, as naturalness")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R003")
    public void shouldReturnAChangeThatGainsAFindingOfTheErrorsFamilyAndNotOneThatGainsAFindingOfAnAnalyzerThatOnlyOrdersAsNaturalness(
            ) {
        Finding hint = hallazgo(HINT, "hint-mismatch", OURSELVES, FindingSeverity.HIGH, FindingResolution.PANEL, null,
                "La pista no corresponde a la respuesta");
        Finding natural = hallazgo(NATURALNESS, "unnatural", VEGETABLES, FindingSeverity.LOW, FindingResolution.RANK_ONLY,
                null, "Suena poco natural");
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.batch(Foto.deHoy(), Foto.deHoy().finding(hint).finding(natural), cambio(P_OURSELVES, OURSELVES),
                cambio(P_VEGETABLES, VEGETABLES));

        BatchComparison comparison = mesa.compare();

        ChangeOutcome errors = change(comparison, P_OURSELVES);
        assertTrue(errors.isReturns(), "R003: a new error of hint-coherence returns it");
        assertEquals(List.of(hint), errors.getNewFindings());
        ChangeOutcome orders = change(comparison, P_VEGETABLES);
        assertFalse(orders.isReturns(), "R003: naturalness only orders");
        assertTrue(orders.getNewFindings().isEmpty());
    }

    @Test
    @DisplayName("should not return a change for a new vocabulary finding of low severity, below an off-level or an out-of-catalog word")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R003")
    public void shouldNotReturnAChangeForANewVocabularyFindingOfLowSeverityBelowAnOfflevelOrAnOutofcatalogWord() {
        Finding low = hallazgo(LEMMA_COUNT, "lemma-overuse", HATS, FindingSeverity.LOW, FindingResolution.RANK_ONLY,
                "wear", "wear aparece más de lo esperado en A1");
        Finding outOfCatalog = hallazgo(LEMMA_ABSENCE, "out-of-catalog-word", OURSELVES, FindingSeverity.MEDIUM,
                FindingResolution.PANEL, "selfie", "selfie no está en el catálogo EVP y es poco frecuente para A1");
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.batch(Foto.deHoy(), Foto.deHoy().finding(low).finding(outOfCatalog), cambio(P_HATS, HATS),
                cambio(P_OURSELVES, OURSELVES));

        BatchComparison comparison = mesa.compare();

        assertFalse(change(comparison, P_HATS).isReturns(), "R003: a vocabulary finding of low severity");
        assertTrue(change(comparison, P_OURSELVES).isReturns(), "R003: a word out of the catalog is medium");
        assertEquals(List.of(outOfCatalog), change(comparison, P_OURSELVES).getNewFindings());
    }

    @Test
    @DisplayName("should return a change when another node gains a finding that counts and names it as related in its evidence, as a duplicate does")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R003")
    public void shouldReturnAChangeWhenAnotherNodeGainsAFindingThatCountsAndNamesItAsRelatedInItsEvidenceAsADuplicateDoes(
            ) {
        // «She is English.», which the batch did not touch, is now a duplicate of the changed «She sports hats.».
        Finding duplicate = hallazgo(DUPLICATES, "same-sentence", SHE_IS_ENGLISH, FindingSeverity.HIGH,
                FindingResolution.RULE, null, "La oración se repite en otro ejercicio",
                new FindingNodeRef(AuditTarget.QUIZ, HATS, "She is English."));
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.batch(Foto.deHoy(), Foto.deHoy().rewrite(HATS, "She is English.").finding(duplicate),
                cambio(P_HATS, HATS), cambio(P_OURSELVES, OURSELVES));

        BatchComparison comparison = mesa.compare();

        ChangeOutcome hats = change(comparison, P_HATS);
        assertTrue(hats.isReturns(), "R003: the duplicate names it");
        assertEquals(List.of(duplicate), hats.getNewFindings(), "R003: with that finding as the reason");
        assertFalse(change(comparison, P_OURSELVES).isReturns(), "R003: a change no finding names does not");
    }

    @Test
    @DisplayName("should list as fixed the findings a change made disappear, matched by their identity of analyzer, rule, node and what they mark")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R003")
    public void shouldListAsFixedTheFindingsAChangeMadeDisappearMatchedByTheirIdentityOfAnalyzerRuleNodeAndWhatTheyMark(
            ) {
        Finding gone = hallazgo(HINT, "hint-mismatch", VEGETABLES, FindingSeverity.HIGH, FindingResolution.PANEL, null,
                "La pista no sirve para «a lot of»");
        Finding stays = hallazgo(INSTRUCTION, "instruction-mismatch", VEGETABLES, FindingSeverity.MEDIUM,
                FindingResolution.PANEL, null, "La consigna no pide la forma");
        // Same identity, other words: the same finding.
        Finding staysReworded = hallazgo(INSTRUCTION, "instruction-mismatch", VEGETABLES, FindingSeverity.MEDIUM,
                FindingResolution.PANEL, null, "La consigna sigue sin pedir la forma");
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.batch(Foto.deHoy().finding(gone).finding(stays), Foto.deHoy().finding(staysReworded),
                cambio(P_VEGETABLES, VEGETABLES));

        ChangeOutcome outcome = mesa.compare().getChanges().get(0);

        assertEquals(List.of(gone), outcome.getFixedFindings(), "R003: the hint is fixed");
        assertTrue(outcome.getNewFindings().isEmpty(), "R003: the instruction keeps its identity: not new");
        assertFalse(outcome.isReturns());
    }

    @Test
    @DisplayName("should leave a change undecided for an analyzer that did not evaluate it in the projection, instead of not returning it")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R003")
    public void shouldLeaveAChangeUndecidedForAnAnalyzerThatDidNotEvaluateItInTheProjectionInsteadOfNotReturningIt(
            ) {
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.samples(new SampleOutcome("muestra-1", PLAN, true, 100, 95, 0, 1, true, "cumple"));
        mesa.batch(Foto.deHoy(), Foto.deHoy().unevaluated(VEGETABLES, INSTRUCTION).errors(REGULAR, INSTRUCTION, 0, 1)
                .anyError(REGULAR, 4, 0, 1), cambio(P_VEGETABLES, VEGETABLES));

        BatchComparison comparison = mesa.compare();

        ChangeOutcome outcome = comparison.getChanges().get(0);
        assertEquals(List.of(INSTRUCTION), outcome.getUndecidedAnalyzers(),
                "R003: quiz-instruction did not judge the new exercise");
        assertFalse(outcome.isReturns(), "R003: nothing known returns it");
        assertEquals(VerdictKind.UNDECIDABLE, comparison.getVerdict().getKind(),
                "R003: so it is not «does not return»: the batch cannot be decided");
        assertEquals(GoalStatus.UNDECIDABLE, row(comparison, INSTRUCTION, REGULAR).getFloor().getStatus());
    }

    @Test
    @DisplayName("should attribute a broken floor of a level or of the course to the changes of the batch, and attribute nothing while every floor holds")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R004")
    public void shouldAttributeABrokenFloorOfALevelOrOfTheCourseToTheChangesOfTheBatchAndAttributeNothingWhileEveryFloorHolds(
            ) {
        Mesa holds = new Mesa().baseline(Foto.del299());
        holds.batch(Foto.del299(), Foto.del299().conLasCuatro(), LAS_CUATRO);
        assertTrue(holds.compare().getBreaches().isEmpty());
        verify(holds.attributor, never()).attribute(any(), any());

        Mesa breaks = new Mesa().baseline(Foto.del299());
        CourseProjection projection = breaks.batch(Foto.del299(), Foto.del299().conLasCuatro().vocabulary(A1, 0.9554)
                .vocabulary(REGULAR, 0.80), LAS_CUATRO);
        when(breaks.attributor.attribute(any(), any())).thenAnswer(call -> {
            List<FloorBreach> given = call.getArgument(1);
            List<FloorBreach> attributed = new ArrayList<>();
            for (FloorBreach breach : given) {
                attributed.add(new FloorBreach(breach.getGoalId(), breach.getRow(),
                        List.of(new ChangeAttribution(P_GRANDMOTHER, GRANDMOTHER, -1.0, GoalUnit.POINTS))));
            }
            return attributed;
        });

        BatchComparison comparison = breaks.compare();

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<List<FloorBreach>> given = ArgumentCaptor.forClass((Class) List.class);
        verify(breaks.attributor, times(1)).attribute(same(projection), given.capture());
        assertEquals(List.of("piso de vocabulario · vocabulario · MILESTONE"),
                given.getValue().stream().map(FloorBreach::getGoalId).toList(), "R004: the floor of A1 that broke");
        assertEquals(A1, given.getValue().get(0).getRow().getNodeId());
        FloorBreach a1 = comparison.getBreaches().get(0);
        assertEquals(A1, a1.getRow().getNodeId());
        assertEquals(List.of(new ChangeAttribution(P_GRANDMOTHER, GRANDMOTHER, -1.0, GoalUnit.POINTS)),
                a1.getAttributions(), "R004: with the changes that break it");
    }

    @Test
    @DisplayName("should check the vocabulary floor against the baseline, accumulated, so a second batch of -0.4 points after another one of -0.4 breaks the floor of 0.5")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R005")
    public void shouldCheckTheVocabularyFloorAgainstTheBaselineAccumulatedSoASecondBatchOf04PointsAfterAnotherOneOf04BreaksTheFloorOf05(
            ) {
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.batch(Foto.deHoy().vocabulary(A1, 0.9614), Foto.deHoy().vocabulary(A1, 0.9574),
                cambio(P_VEGETABLES, VEGETABLES));

        ComparisonRow row = row(mesa.compare(), "vocabulario", A1);

        assertEquals(-0.8, row.getChange(), 1e-9, "R005: the two batches add up against the baseline");
        assertEquals(GoalReference.BASELINE, row.getFloor().getReference());
        assertEquals(GoalStatus.NOT_MET, row.getFloor().getStatus(),
                "R005: −0,4 by itself would hold; −0,8 from the baseline does not");
    }

    @Test
    @DisplayName("should check the error floor against the batch start, so a batch cannot add an error to a level already below the baseline")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R005")
    public void shouldCheckTheErrorFloorAgainstTheBatchStartSoABatchCannotAddAnErrorToALevelAlreadyBelowTheBaseline(
            ) {
        Mesa mesa = new Mesa().baseline(Foto.deHoy().anyError(A1, 120, 10, 0));
        mesa.batch(Foto.deHoy().anyError(A1, 120, 4, 0), Foto.deHoy().anyError(A1, 120, 5, 0),
                cambio(P_VEGETABLES, VEGETABLES));

        ComparisonRow row = row(mesa.compare(), "algún error", A1);

        assertEquals(-5.0, row.getChange(), 1e-9, "R001: five fewer than the baseline");
        assertEquals(GoalReference.BATCH_START, row.getFloor().getReference());
        assertEquals(GoalStatus.NOT_MET, row.getFloor().getStatus(),
                "R005: but one more than where the batch started");
    }

    @Test
    @DisplayName("should evaluate with the current goals and say «metas cambiadas desde que se fijaron: piso de vocabulario 0,5 → 1,0» when the vocabulary floor sealed at 0.5 is now 1.0")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R006")
    public void shouldEvaluateWithTheCurrentGoalsAndSayMetasCambiadasDesdeQueSeFijaronPisoDeVocabulario0510WhenTheVocabularyFloorSealedAt05IsNow10(
            ) {
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        SealedGoals sealed = mesa.book().seal();
        mesa.declaration = new GoalDeclaration(List.of(new FamilyFloor(AnalyzerFamily.VOCABULARY, 1.0, GoalUnit.POINTS,
                GoalReference.BASELINE, List.of(AuditTarget.MILESTONE, AuditTarget.COURSE))), List.of(), null);
        mesa.batch(Foto.deHoy(), Foto.deHoy().vocabulary(A1, 0.9574), cambio(P_VEGETABLES, VEGETABLES));

        BatchComparison comparison = mesa.compare();

        assertEquals(sealed.getSealedAt(), comparison.getGoalsSealedAt());
        assertEquals(List.of(new GoalChange("piso de vocabulario", "0,5", "1,0")), comparison.getGoalChanges(),
                "R006: «metas cambiadas desde que se fijaron: piso de vocabulario 0,5 → 1,0»");
        ComparisonRow row = row(comparison, "vocabulario", A1);
        assertEquals(1.0, row.getFloor().getThreshold(), 1e-9, "R006: it evaluates with the current goals");
        assertEquals(GoalStatus.MET, row.getFloor().getStatus(), "R006: −0,8 holds a floor of 1,0");
        assertNotEquals(sealed.getGoals().getFingerprint(), comparison.getGoals().getFingerprint());
    }

    @Test
    @DisplayName("should say that the goals were never sealed when there is no sealed version of them")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R006")
    public void shouldSayThatTheGoalsWereNeverSealedWhenThereIsNoSealedVersionOfThem() {
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.batch(Foto.deHoy(), Foto.deHoy(), cambio(P_VEGETABLES, VEGETABLES));

        BatchComparison comparison = mesa.compare();

        assertNull(comparison.getGoalsSealedAt(), "R006: never sealed");
        assertTrue(comparison.getGoalChanges().isEmpty(), "R006: nothing to compare them with");
        assertEquals(mesa.book().effective(), comparison.getGoals(), "R006: it says which goals it evaluated with");

        mesa.book().seal();
        assertNotNull(mesa.compare().getGoalsSealedAt(), "R006: once sealed, it says when");
    }

    @Test
    @DisplayName("should show the vocabulary row of the course as a measure change 73.92 → 73.86 with the lemma-count bands 8, 6, 4 and 3, and count the batch from 73.86")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R007")
    public void shouldShowTheVocabularyRowOfTheCourseAsAMeasureChange73927386WithTheLemmacountBands864And3AndCountTheBatchFrom7386(
            ) {
        Mesa mesa = new Mesa().baseline(Foto.del299().measure(LEMMA_COUNT, BANDS_OF_29_9, Map.of("bands", "1 · 2 · 4 · 8")));
        mesa.remeasured(Foto.del299().conLasBandasNuevas());
        mesa.batch(Foto.del299().conLasBandasNuevas(), Foto.del299().conLasBandasNuevas().vocabulary(ROOT, 0.7384),
                cambio(P_VEGETABLES, VEGETABLES));

        BatchComparison comparison = mesa.compare();

        assertEquals(List.of(new MeasureChange(LEMMA_COUNT, BANDS_OF_29_9, BANDS_8_6_4_3,
                Map.of("bands", "1 · 2 · 4 · 8"), Map.of("bands", "8 · 6 · 4 · 3"))), comparison.getMeasureChanges(),
                "R007: the measure of lemma-count changed, apart");
        ComparisonRow course = row(comparison, "vocabulario", ROOT);
        assertEquals(73.92, course.getBaseline().getValue() * 100, 1e-9, "R007: the baseline with the old measure");
        assertEquals(73.86, course.getRemeasuredBaseline().getValue() * 100, 1e-9, "R007: and with the new one");
        assertEquals(-0.02, course.getChange(), 1e-9, "R007: the batch counted from 73,86, not from 73,92");
        assertEquals(GoalStatus.MET, course.getFloor().getStatus());
    }

    @Test
    @DisplayName("should show A1 as 96.54 → 96.02 and lemma-count on the course as 67.0 → 66.5 with those bands, so the first batch after them breaks no vocabulary floor in A1, A2 or B1")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R007")
    public void shouldShowA1As96549602AndLemmacountOnTheCourseAs670665WithThoseBandsSoTheFirstBatchAfterThemBreaksNoVocabularyFloorInA1A2OrB1(
            ) {
        Mesa mesa = new Mesa().baseline(Foto.del299().measure(LEMMA_COUNT, BANDS_OF_29_9, Map.of("bands", "1 · 2 · 4 · 8")));
        mesa.remeasured(Foto.del299().conLasBandasNuevas());
        mesa.batch(Foto.del299().conLasBandasNuevas(), Foto.del299().conLasBandasNuevas(), cambio(P_VEGETABLES, VEGETABLES));

        BatchComparison comparison = mesa.compare();

        ComparisonRow a1 = row(comparison, "vocabulario", A1);
        assertEquals(96.54, a1.getBaseline().getValue() * 100, 1e-9);
        assertEquals(96.02, a1.getRemeasuredBaseline().getValue() * 100, 1e-9, "R007: «96,54 → 96,02»");
        ComparisonRow lemmaCount = row(comparison, LEMMA_COUNT, ROOT);
        assertEquals(67.0, lemmaCount.getBaseline().getValue() * 100, 1e-9);
        assertEquals(66.5, lemmaCount.getRemeasuredBaseline().getValue() * 100, 1e-9, "R007: «67,0 → 66,5»");
        for (String level : List.of(A1, A2, B1)) {
            ComparisonRow row = row(comparison, "vocabulario", level);
            assertEquals(0.0, row.getChange(), 1e-9, "R007: not a word changed in " + row.getLabel());
            assertEquals(GoalStatus.MET, row.getFloor().getStatus(),
                    "R007: the floor of " + row.getLabel() + " holds, though the old measure was 0,5 higher");
        }
        assertTrue(comparison.getBreaches().isEmpty());
    }

    @Test
    @DisplayName("should re-measure the baseline with the new measure on the same course of the baseline report before counting the batch")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R007")
    public void shouldRemeasureTheBaselineWithTheNewMeasureOnTheSameCourseOfTheBaselineReportBeforeCountingTheBatch() {
        Foto base = Foto.del299().measure(LEMMA_COUNT, BANDS_OF_29_9, Map.of("bands", "1 · 2 · 4 · 8"));
        Mesa mesa = new Mesa().baseline(base);
        mesa.remeasured(Foto.del299().conLasBandasNuevas());
        mesa.batch(Foto.del299().conLasBandasNuevas(), Foto.del299().conLasBandasNuevas().conLasCuatro(), LAS_CUATRO);

        mesa.compare();

        ArgumentCaptor<AuditableCourse> course = ArgumentCaptor.forClass(AuditableCourse.class);
        ArgumentCaptor<AnalyzerRunSelection> selection = ArgumentCaptor.forClass(AnalyzerRunSelection.class);
        verify(mesa.engine, times(1)).runAudit(course.capture(), selection.capture());
        assertEquals(List.of(LEMMA_ABSENCE, LEMMA_COUNT), selection.getValue().getAnalyzers(),
                "R007: the analyzers the baseline ran, in its order");
        List<String> levels = course.getValue().getMilestones().stream().map(AuditableMilestone::getId).toList();
        assertEquals(List.of(A1, A2, B1), levels, "R007: the course of the baseline report");
        AuditableQuiz grandmother = course.getValue().getMilestones().get(0).getTopics().get(0).getKnowledge().get(1)
                .getQuizzes().get(0);
        assertSame(base.entity(GRANDMOTHER), grandmother,
                "R007: the same exercise of the baseline, with its tokens -- not the one the batch changed");
        assertEquals("It comes every year.", grandmother.getSentences().get(0));
    }

    @Test
    @DisplayName("should leave undecidable the rows of an analyzer with a new measure when the baseline cannot be measured again")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R007")
    public void shouldLeaveUndecidableTheRowsOfAnAnalyzerWithANewMeasureWhenTheBaselineCannotBeMeasuredAgain() {
        Mesa mesa = new Mesa().baseline(Foto.del299().measure(LEMMA_COUNT, BANDS_OF_29_9, Map.of("bands", "1 · 2 · 4 · 8")));
        when(mesa.engine.runAudit(any(AuditableCourse.class), any(AnalyzerRunSelection.class)))
                .thenThrow(new IllegalStateException("el motor no pudo medir"));
        mesa.batch(Foto.del299().conLasBandasNuevas(), Foto.del299().conLasBandasNuevas(), cambio(P_VEGETABLES, VEGETABLES));

        BatchComparison comparison = mesa.compare();

        assertEquals(1, comparison.getMeasureChanges().size(), "R007: the change of measure is still shown");
        for (String subject : List.of(LEMMA_COUNT, "vocabulario")) {
            ComparisonRow row = row(comparison, subject, A1);
            assertNull(row.getRemeasuredBaseline(), "R007: no baseline measured again");
            assertNull(row.getChange(), "R007: and no change against the old measure");
            assertEquals(GoalStatus.UNDECIDABLE, row.getFloor().getStatus(), "R007: " + subject + " cannot be decided");
        }
        assertEquals(GoalStatus.MET, row(comparison, LEMMA_ABSENCE, A1).getFloor().getStatus(),
                "R007: an analyzer whose measure did not change is decided as always");
        assertEquals(VerdictKind.UNDECIDABLE, comparison.getVerdict().getKind());
    }

    @Test
    @DisplayName("should show as a measure change, and not as a change of the course, naturalness measured with claude -p in the projection and with llama3 in Ollama in the baseline")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R007")
    public void shouldShowAsAMeasureChangeAndNotAsAChangeOfTheCourseNaturalnessMeasuredWithClaudePInTheProjectionAndWithLlama3InOllamaInTheBaseline(
            ) {
        Map<String, Object> llama = Map.of("consults", "ollama · llama3");
        Map<String, Object> claude = Map.of("consults", "claude -p");
        Mesa mesa = new Mesa().baseline(Foto.deHoy().measure(NATURALNESS, "naturalness-llama3", llama)
                .errors(ROOT, NATURALNESS, 12, 0).anyError(ROOT, 11287, 1368, 0));
        Foto withClaude = Foto.deHoy().measure(NATURALNESS, "naturalness-claude-p", claude).errors(ROOT, NATURALNESS, 0, 0)
                .anyError(ROOT, 11287, 1356, 0);
        mesa.remeasured(withClaude);
        mesa.batch(Foto.deHoy().measure(NATURALNESS, "naturalness-claude-p", claude).errors(ROOT, NATURALNESS, 0, 0)
                        .anyError(ROOT, 11287, 1356, 0),
                Foto.deHoy().measure(NATURALNESS, "naturalness-claude-p", claude).errors(ROOT, NATURALNESS, 0, 0)
                        .anyError(ROOT, 11287, 1356, 0), cambio(P_VEGETABLES, VEGETABLES));

        BatchComparison comparison = mesa.compare();

        assertEquals(List.of(new MeasureChange(NATURALNESS, "naturalness-llama3", "naturalness-claude-p", llama, claude)),
                comparison.getMeasureChanges(), "R007: who judges naturalness changed: a measure change");
        ComparisonRow naturalness = row(comparison, NATURALNESS, ROOT);
        assertEquals(12.0, naturalness.getBaseline().getValue(), 1e-9);
        assertEquals(0.0, naturalness.getRemeasuredBaseline().getValue(), 1e-9);
        assertEquals(0.0, naturalness.getChange(), 1e-9, "R007: not −12 for the course");
        ComparisonRow any = row(comparison, "algún error", ROOT);
        assertEquals(1356.0, any.getRemeasuredBaseline().getValue(), 1e-9);
        assertEquals(0.0, any.getChange(), 1e-9, "R007: «algún error» counted from the baseline measured with claude -p");
        assertNull(row(comparison, "vocabulario", ROOT).getRemeasuredBaseline(),
                "R007: the vocabulary did not change its measure");
    }

    @Test
    @DisplayName("should give the batch of the four proposals of «Afirmativas regulares» the verdict fails, with «My grandmother ___ (come) every year.» as the node that returns")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R008")
    public void shouldGiveTheBatchOfTheFourProposalsOfAfirmativasRegularesTheVerdictFailsWithMyGrandmotherComeEveryYearAsTheNodeThatReturns(
            ) {
        Mesa mesa = new Mesa().baseline(Foto.del299());
        mesa.samples(new SampleOutcome("muestra-1", PLAN, true, 100, 95, 0, 1, true, "cumple"));
        mesa.batch(Foto.del299(), Foto.del299().conLasCuatro(), LAS_CUATRO);

        BatchComparison comparison = mesa.compare();

        assertEquals(VerdictKind.FAILS, comparison.getVerdict().getKind(), "R008: a node returns");
        assertEquals(List.of(P_GRANDMOTHER), comparison.getVerdict().getReturningProposalIds());
        assertTrue(comparison.getVerdict().getBrokenFloorGoalIds().isEmpty(), "R008: no level number moved");
        assertEquals("No pasa: vuelve 1 cambio: " + P_GRANDMOTHER, comparison.getVerdict().getDetail());
    }

    @Test
    @DisplayName("should give that batch without the grandmother proposal the verdict passes once its blind sample is answered and meets its goal")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R008")
    public void shouldGiveThatBatchWithoutTheGrandmotherProposalTheVerdictPassesOnceItsBlindSampleIsAnsweredAndMeetsItsGoal(
            ) {
        SampleOutcome answered = new SampleOutcome("muestra-2026-10-01T10-00-00", PLAN, true, 100, 94, 1, 5, true,
                "94 mejor o igual y 1 peor de 100");
        Mesa mesa = new Mesa().baseline(Foto.del299());
        mesa.samples(answered);
        mesa.batch(Foto.del299(), Foto.del299().conLasCuatro().sinLaAbuela(), cambio(P_OURSELVES, OURSELVES),
                cambio(P_VEGETABLES, VEGETABLES), cambio(P_HATS, HATS));

        BatchComparison comparison = mesa.compare();

        assertEquals(VerdictKind.PASSES, comparison.getVerdict().getKind(), "R008: every floor holds, nothing returns");
        assertSame(answered, comparison.getVerdict().getSample(), "R008: and the blind sample meets its goal");
        assertTrue(comparison.getVerdict().getReturningProposalIds().isEmpty());
        verify(mesa.samples).outcomeFor(PLAN);
    }

    @Test
    @DisplayName("should give the verdict blind sample missing when every floor holds and the blind sample is missing or still open")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R008")
    public void shouldGiveTheVerdictBlindSampleMissingWhenEveryFloorHoldsAndTheBlindSampleIsMissingOrStillOpen() {
        Mesa missing = new Mesa().baseline(Foto.del299());
        missing.batch(Foto.del299(), Foto.del299(), cambio(P_VEGETABLES, VEGETABLES));
        BatchComparison none = missing.compare();
        assertEquals(VerdictKind.BLIND_SAMPLE_MISSING, none.getVerdict().getKind(), "R008: no sample drawn");
        assertNull(none.getVerdict().getSample());

        Mesa open = new Mesa().baseline(Foto.del299());
        SampleOutcome halfway = new SampleOutcome("muestra-1", PLAN, false, 40, 0, 0, 0, false, null);
        open.samples(halfway);
        open.batch(Foto.del299(), Foto.del299(), cambio(P_VEGETABLES, VEGETABLES));
        BatchComparison stillOpen = open.compare();
        assertEquals(VerdictKind.BLIND_SAMPLE_MISSING, stillOpen.getVerdict().getKind(), "R008: drawn, not closed");
        assertSame(halfway, stillOpen.getVerdict().getSample());
        assertTrue(stillOpen.getVerdict().getDetail().contains("muestra-1 sigue abierta"));
    }

    @Test
    @DisplayName("should give the verdict undecidable when no floor fails but one of them cannot be decided")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R008")
    public void shouldGiveTheVerdictUndecidableWhenNoFloorFailsButOneOfThemCannotBeDecided() {
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.samples(new SampleOutcome("muestra-1", PLAN, true, 100, 95, 0, 1, true, "cumple"));
        mesa.batch(Foto.deHoy(), Foto.deHoy().unevaluated(VEGETABLES, INSTRUCTION).errors(A1, INSTRUCTION, 0, 1)
                .anyError(A1, 6, 0, 1), cambio(P_VEGETABLES, VEGETABLES));

        BatchComparison comparison = mesa.compare();

        assertEquals(GoalStatus.UNDECIDABLE, row(comparison, "algún error", A1).getFloor().getStatus());
        assertTrue(comparison.getBreaches().isEmpty(), "R008: no floor fails");
        assertEquals(VerdictKind.UNDECIDABLE, comparison.getVerdict().getKind(),
                "R008: one floor cannot be decided, though the sample meets its goal");
        assertTrue(comparison.getVerdict().getDetail().startsWith("No se puede decidir: ningún piso falla"));
    }

    @Test
    @DisplayName("should give the verdict fails with the list of broken floors when a floor fails, even if the blind sample meets its goal")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R008")
    public void shouldGiveTheVerdictFailsWithTheListOfBrokenFloorsWhenAFloorFailsEvenIfTheBlindSampleMeetsItsGoal() {
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.samples(new SampleOutcome("muestra-1", PLAN, true, 100, 95, 0, 1, true, "cumple"));
        mesa.batch(Foto.deHoy(), Foto.deHoy().vocabulary(A1, 0.9574).vocabulary(ROOT, 0.7330),
                cambio(P_VEGETABLES, VEGETABLES));

        BatchComparison comparison = mesa.compare();

        assertEquals(VerdictKind.FAILS, comparison.getVerdict().getKind());
        assertEquals(List.of("piso de vocabulario · vocabulario · COURSE", "piso de vocabulario · vocabulario · MILESTONE"),
                comparison.getVerdict().getBrokenFloorGoalIds(), "R008: the floors of the course and of A1");
        assertEquals(2, comparison.getBreaches().size());
        assertTrue(comparison.getVerdict().getDetail().contains("2 pisos rotos"));
    }

    @Test
    @DisplayName("should give the verdict fails when the closed blind sample does not meet its goal")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R008")
    public void shouldGiveTheVerdictFailsWhenTheClosedBlindSampleDoesNotMeetItsGoal() {
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.samples(new SampleOutcome("muestra-1", PLAN, true, 100, 85, 4, 2, false,
                "85 mejor o igual de 100: la meta es 90; 4 peores: la meta es 1"));
        mesa.batch(Foto.deHoy(), Foto.deHoy(), cambio(P_VEGETABLES, VEGETABLES));

        BatchComparison comparison = mesa.compare();

        assertTrue(comparison.getBreaches().isEmpty(), "R008: every floor holds");
        assertEquals(VerdictKind.FAILS, comparison.getVerdict().getKind(), "R008: the sample does not");
        assertTrue(comparison.getVerdict().getDetail().contains("la muestra ciega muestra-1 no cumple su meta"));
    }

    @Test
    @DisplayName("should never let a progress goal decide the verdict, as any error above 5% in the course")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R008")
    public void shouldNeverLetAProgressGoalDecideTheVerdictAsAnyErrorAbove5InTheCourse() {
        Mesa mesa = new Mesa().baseline(Foto.deHoy().anyError(ROOT, 11287, 2900, 0));
        mesa.declaration = new GoalDeclaration(List.of(), List.of(new Goal("avance · algún error · curso",
                GoalKind.PROGRESS, "algún error", AuditTarget.COURSE, null, 5.0, GoalUnit.PERCENT, GoalReference.ABSOLUTE,
                GoalOrigin.DECLARED)), null);
        mesa.samples(new SampleOutcome("muestra-1", PLAN, true, 100, 95, 0, 1, true, "cumple"));
        mesa.batch(Foto.deHoy().anyError(ROOT, 11287, 2900, 0), Foto.deHoy().anyError(ROOT, 11287, 2896, 0),
                cambio(P_VEGETABLES, VEGETABLES));

        BatchComparison comparison = mesa.compare();

        ComparisonRow course = row(comparison, "algún error", ROOT);
        assertEquals(GoalStatus.NOT_MET, course.getProgress().getStatus(), "R008: 25,7 % is far from 5 %");
        assertEquals(VerdictKind.PASSES, comparison.getVerdict().getKind(), "R008: and it does not decide");
    }

    @Test
    @DisplayName("should write nothing when comparing: no course, proposal, plan, sealed goals or analysis of the history")
    @Tag("FEAT-METAS")
    @Tag("F-METAS-R009")
    public void shouldWriteNothingWhenComparingNoCourseProposalPlanSealedGoalsOrAnalysisOfTheHistory(
            @TempDir Path tmp) throws Exception {
        Path course = tmp.resolve("english-course");
        Files.createDirectories(course);
        Path file = course.resolve("quiz-templates.json");
        Files.writeString(file, "[{\"_id\":\"" + GRANDMOTHER + "\",\"sentence\":\"It comes every year.\"}]",
                StandardCharsets.UTF_8);
        byte[] before = Files.readAllBytes(file);
        Mesa mesa = new Mesa().baseline(Foto.del299());
        mesa.book().seal();
        int sealedBefore = mesa.sealed.size();
        CourseProjection projection = mesa.batch(Foto.del299(), Foto.del299().conLasCuatro(), LAS_CUATRO);

        mesa.comparator().compare(course, null);

        assertArrayEquals(before, Files.readAllBytes(file), "R009: the course, byte by byte");
        assertEquals(sealedBefore, mesa.sealed.size(), "R009: the sealed goals");
        verify(mesa.baselines).read();
        verifyNoMoreInteractions(mesa.baselines);
        verify(mesa.reports).load(BASELINE);
        verifyNoMoreInteractions(mesa.reports);
        verify(mesa.projector).project(course, null);
        verifyNoMoreInteractions(mesa.projector);
        verifyNoInteractions(mesa.engine);
        assertEquals(4, projection.getChanges().size(), "R009: the projection it read is as it was");
    }

    // =========================================================================
    // The course of 29/9 in three photos
    // =========================================================================

    static final Path COURSE = Path.of("db/english-course");
    static final String BASELINE = "2026-09-29T08-00-00";
    static final Instant FIXED_AT = Instant.parse("2026-09-30T09:00:00Z");
    static final String AUDIT = "2026-09-30T11-54-02";
    static final String PLAN = "2026-09-30T11-54-12";
    static final String ROOT = "root";
    static final String A1 = "6814dafa7d73e7209a13d382";
    static final String A2 = "6814dafa7d73e7209a13d218";
    static final String B1 = "6814dafa7d73e7209a13d2b5";
    static final String PRESENT_SIMPLE = "6814dafa7d73e7209a13d383";
    static final String A2_TOPIC = "6814dafa7d73e7209a13d219";
    static final String B1_TOPIC = "6814dafa7d73e7209a13d2b6";
    static final String WITH_BE = "6814dafa7d73e7209a13d384";
    static final String REGULAR = "6814dafa7d73e7209a13d389";
    static final String A2_KNOWLEDGE = "6814dafa7d73e7209a13d21a";
    static final String INVEST = "6814dafa7d73e7209a13d2b7";
    static final String SHE_IS_ENGLISH = "67fab6d59930102295341e5f";
    static final String LIVING_ROOM = "67fab6d59930102295341e60";
    static final String GRANDMOTHER = "67fab6d59930102295341ec4";
    static final String OURSELVES = "67fab6d59930102295341ecb";
    static final String VEGETABLES = "67fab6d59930102295341ecd";
    static final String HATS = "67fab6d59930102295341ed6";
    static final String A2_QUIZ = "67fab6d59930102295343a01";
    static final String INVEST_QUIZ = "67fab6d59930102295344b01";
    static final String P_GRANDMOTHER = "077-afirmativas-regulares-grandmother";
    static final String P_OURSELVES = "077-afirmativas-regulares-ourselves";
    static final String P_VEGETABLES = "077-afirmativas-regulares-vegetables";
    static final String P_HATS = "077-afirmativas-regulares-hats";
    static final String P_LIVING_ROOM = "077-afirmativas-con-be-living-room";
    static final String P_INVEST = "077-invest-time";
    static final ProjectedChange[] LAS_CUATRO = {cambio(P_GRANDMOTHER, GRANDMOTHER), cambio(P_OURSELVES, OURSELVES),
            cambio(P_VEGETABLES, VEGETABLES), cambio(P_HATS, HATS)};
    static final String LEMMA_ABSENCE = "lemma-absence";
    static final String LEMMA_COUNT = "lemma-count";
    static final String SENTENCE_LENGTH = "sentence-length";
    static final String HINT = "hint-coherence";
    static final String INSTRUCTION = "quiz-instruction";
    static final String DUPLICATES = "exact-duplicates";
    static final String NATURALNESS = "naturalness";
    static final String BANDS_OF_29_9 = "lemma-count · bandas 1 · 2 · 4 · 8";
    static final String BANDS_8_6_4_3 = "lemma-count · bandas 8 · 6 · 4 · 3";
    static final Goal ANY_ERROR_IN_A1_ZERO = new Goal("algún error en A1: 0", GoalKind.FLOOR, "algún error",
            AuditTarget.MILESTONE, A1, 0.0, GoalUnit.EXERCISES, GoalReference.ABSOLUTE, GoalOrigin.DECLARED);

    /** The Spanish name of the cards of the photo, as today's catalog names them (F-FILAS-R001). */
    static final Map<String, String> SPANISH = Map.of(
            LEMMA_ABSENCE, "Vocabulario del nivel presente", LEMMA_COUNT, "Palabras vistas suficientes veces",
            SENTENCE_LENGTH, "Oraciones con el largo de su nivel",
            HINT, "Pista que falta, confunde o regala la respuesta", INSTRUCTION, "Consigna que no se cumple",
            DUPLICATES, "Ejercicio repetido igual", NATURALNESS, "Oración poco natural");

    /** Today's catalog, in its order: vocabulary first, then errors; naturalness only orders. */
    static final List<AnalyzerDescriptor> CARDS = List.of(
            card(LEMMA_ABSENCE, AnalyzerFamily.VOCABULARY, FindingResolution.PANEL, FindingResolution.RANK_ONLY),
            card(LEMMA_COUNT, AnalyzerFamily.VOCABULARY, FindingResolution.RANK_ONLY),
            card(SENTENCE_LENGTH, AnalyzerFamily.VOCABULARY, FindingResolution.PANEL),
            card(HINT, AnalyzerFamily.ERRORS, FindingResolution.PANEL),
            card(INSTRUCTION, AnalyzerFamily.ERRORS, FindingResolution.PANEL),
            card(DUPLICATES, AnalyzerFamily.ERRORS, FindingResolution.RULE),
            card(NATURALNESS, AnalyzerFamily.ERRORS, FindingResolution.RANK_ONLY));

    static AnalyzerDescriptor card(String name, AnalyzerFamily family, FindingResolution... resolutions) {
        return new AnalyzerDescriptor(name, name, AuditTarget.QUIZ, "¿?", "la oración", List.of(), "1,0", family,
                List.of(AuditTarget.QUIZ), List.of(resolutions), AnalysisCost.INSTANT, SPANISH.getOrDefault(name, name),
                List.of(), "La frase de " + name + ".");
    }

    static ProjectedChange cambio(String proposalId, String quizId) {
        return new ProjectedChange(proposalId, AuditTarget.QUIZ, quizId, true);
    }

    static ComparisonRow row(BatchComparison comparison, String subject, String nodeId) {
        for (ComparisonRow row : comparison.getRows()) {
            if (subject.equals(row.getSubject()) && nodeId.equals(row.getNodeId())) {
                return row;
            }
        }
        throw new AssertionError("no row of " + subject + " in " + nodeId);
    }

    static ChangeOutcome change(BatchComparison comparison, String proposalId) {
        for (ChangeOutcome outcome : comparison.getChanges()) {
            if (proposalId.equals(outcome.getProposalId())) {
                return outcome;
            }
        }
        throw new AssertionError("no change " + proposalId);
    }

    static String part(Finding finding, String label) {
        for (EvidencePart part : finding.getEvidence().getExamined()) {
            if (label.equals(part.getLabel())) {
                return part.getText();
            }
        }
        return null;
    }

    /** lemma-absence, as it marks a word of another level (LemmaByLevelAbsenceAnalyzer, rule misplaced-word). */
    static Finding palabraDeOtroNivel(String quizId, String sentence, String word, String wordLevel, String quizLevel) {
        return new Finding(LEMMA_ABSENCE, "misplaced-word", new FindingNodeRef(AuditTarget.QUIZ, quizId, sentence),
                FindingSeverity.MEDIUM, new FindingEvidence(List.of(new EvidencePart("Oración", sentence),
                        new EvidencePart("Palabra", word), new EvidencePart("Nivel de la palabra", wordLevel),
                        new EvidencePart("Nivel del ejercicio", quizLevel)),
                        word + " es una palabra de " + wordLevel + " en un ejercicio de " + quizLevel, List.of()),
                FindingResolution.PANEL, AnalysisCost.INSTANT,
                new FindingIdentity(LEMMA_ABSENCE + "|misplaced-word|" + quizId + "|" + word, word));
    }

    /** sentence-length, as it marks «He isn't in the living-room.»: 9 tokens against 3 to 8 in A1. */
    static Finding largo(String quizId, String sentence, FindingSeverity severity) {
        return new Finding(SENTENCE_LENGTH, "longer-than-goal", new FindingNodeRef(AuditTarget.QUIZ, quizId, sentence),
                severity, new FindingEvidence(List.of(new EvidencePart("Oración", sentence),
                        new EvidencePart("Largo", "9 tokens"), new EvidencePart("Meta", "3 a 8 tokens")),
                        "9 tokens, más que los 8 de A1", List.of()),
                FindingResolution.PANEL, AnalysisCost.INSTANT,
                new FindingIdentity(SENTENCE_LENGTH + "|longer-than-goal|" + quizId, null));
    }

    static Finding hallazgo(String analyzer, String rule, String quizId, FindingSeverity severity,
            FindingResolution resolution, String marker, String observation, FindingNodeRef... related) {
        List<EvidencePart> examined = new ArrayList<>(List.of(new EvidencePart("Ejercicio", quizId)));
        if (marker != null) {
            examined.add(new EvidencePart("Palabra", marker));
        }
        return new Finding(analyzer, rule, new FindingNodeRef(AuditTarget.QUIZ, quizId, quizId), severity,
                new FindingEvidence(examined, observation, List.of(related)), resolution, AnalysisCost.INSTANT,
                new FindingIdentity(analyzer + "|" + rule + "|" + quizId + (marker != null ? "|" + marker : ""), marker));
    }

    /**
     * A photo of the course as the engine publishes it: the tree of 29/9 in miniature -- A1 with Present Simple,
     * A2 and B1 --, with the entities of its nodes and the numbers each node publishes. The numbers of the course,
     * A1 and «Afirmativas regulares» are the ones of 29/9 (vocabulary 73,92 and 96,54; lemma-absence 100,0 and
     * 99,552); the vocabulary score also weighs analyzers the photo leaves out, so it is not the average of the two
     * it keeps -- the comparison copies it, it never recomputes it. The base of 29/9 ran no error analyzer; today's
     * photo runs two.
     */
    static final class Foto {
        private static final Map<String, double[]> NUMBERS_OF_29_9 = new LinkedHashMap<>();

        static {
            // node: vocabulary, lemma-absence, lemma-count
            NUMBERS_OF_29_9.put(ROOT, new double[] {0.7392, 0.8650, 0.6700});
            NUMBERS_OF_29_9.put(A1, new double[] {0.9654, 0.9871, 0.9437});
            NUMBERS_OF_29_9.put(PRESENT_SIMPLE, new double[] {0.9712, 0.99552, 0.9469});
            NUMBERS_OF_29_9.put(WITH_BE, new double[] {0.9800, 0.9900, 0.9700});
            NUMBERS_OF_29_9.put(REGULAR, new double[] {0.9850, 1.0, 0.9700});
            NUMBERS_OF_29_9.put(A2, new double[] {0.9360, 0.9700, 0.9020});
            NUMBERS_OF_29_9.put(A2_TOPIC, new double[] {0.9400, 0.9700, 0.9100});
            NUMBERS_OF_29_9.put(A2_KNOWLEDGE, new double[] {0.9400, 0.9700, 0.9100});
            NUMBERS_OF_29_9.put(B1, new double[] {0.7887, 0.8300, 0.7474});
            NUMBERS_OF_29_9.put(B1_TOPIC, new double[] {0.8000, 0.8300, 0.7700});
            NUMBERS_OF_29_9.put(INVEST, new double[] {0.8100, 0.8300, 0.7900});
        }

        private final AuditNode root;
        private final Map<String, AuditNode> nodes = new LinkedHashMap<>();

        private Foto(boolean errors) {
            root = new AuditNode(null, AuditTarget.COURSE, null, new ArrayList<>(), new LinkedHashMap<>(),
                    new LinkedHashMap<>(), null);
            nodes.put(ROOT, root);
            AuditNode a1 = add(root, new AuditableMilestone(new ArrayList<>(), A1, "A1", "A1"), AuditTarget.MILESTONE);
            AuditNode presentSimple = add(a1, new AuditableTopic(new ArrayList<>(), PRESENT_SIMPLE, "Present Simple",
                    "a1.01"), AuditTarget.TOPIC);
            AuditNode withBe = knowledge(presentSimple, WITH_BE, "Afirmativas con $be$");
            quiz(withBe, SHE_IS_ENGLISH, "She is English.");
            quiz(withBe, LIVING_ROOM, "He isn't in the living room.");
            AuditNode regular = knowledge(presentSimple, REGULAR, "Afirmativas regulares");
            quiz(regular, GRANDMOTHER, "It comes every year.");
            quiz(regular, OURSELVES, "We know ourselves.");
            quiz(regular, VEGETABLES, "I love a lot of vegetables.");
            quiz(regular, HATS, "She sports hats.");
            AuditNode a2 = add(root, new AuditableMilestone(new ArrayList<>(), A2, "A2", "A2"), AuditTarget.MILESTONE);
            AuditNode pastSimple = add(a2, new AuditableTopic(new ArrayList<>(), A2_TOPIC, "Past Simple", "a2.01"),
                    AuditTarget.TOPIC);
            quiz(knowledge(pastSimple, A2_KNOWLEDGE, "Pasado regular"), A2_QUIZ, "I worked yesterday.");
            AuditNode b1 = add(root, new AuditableMilestone(new ArrayList<>(), B1, "B1", "B1"), AuditTarget.MILESTONE);
            AuditNode gerunds = add(b1, new AuditableTopic(new ArrayList<>(), B1_TOPIC, "Gerundios", "b1.01"),
                    AuditTarget.TOPIC);
            quiz(knowledge(gerunds, INVEST, "$Invest time$ + gerundio"), INVEST_QUIZ, "I spend time reading.");

            for (Map.Entry<String, double[]> node : NUMBERS_OF_29_9.entrySet()) {
                vocabulary(node.getKey(), node.getValue()[0]);
                score(node.getKey(), LEMMA_ABSENCE, node.getValue()[1]);
                score(node.getKey(), LEMMA_COUNT, node.getValue()[2]);
            }
            measure(LEMMA_ABSENCE, "lemma-absence · evp 2026-09", Map.of("catalog", "EVP"));
            measure(LEMMA_COUNT, BANDS_8_6_4_3, Map.of("bands", "8 · 6 · 4 · 3"));
            if (errors) {
                for (String id : NUMBERS_OF_29_9.keySet()) {
                    errors(id, HINT, 0, 0);
                    errors(id, INSTRUCTION, 0, 0);
                    anyError(id, quizzesUnder(nodes.get(id)), 0, 0);
                }
                measure(HINT, "hint-coherence · reglas", Map.of());
                measure(INSTRUCTION, "quiz-instruction · claude -p", Map.of("consults", "claude -p"));
            }
        }

        /** The base of 29/9: the vocabulary analyzers, no error analyzer. */
        static Foto del299() {
            return new Foto(false);
        }

        /** The same course measured today: with two analyzers of errors. */
        static Foto deHoy() {
            return new Foto(true);
        }

        /**
         * The four proposals of the 077 applied: «My grandmother comes every year.» puts an A2 word in A1 --
         * lemma-absence 1,0 → 0,9 on its exercise, 100,0 → 99,5 on its knowledge, 99,552 → 99,519 on its topic --
         * and nothing else moves in two decimals.
         */
        Foto conLasCuatro() {
            rewrite(GRANDMOTHER, "My grandmother comes every year.");
            rewrite(OURSELVES, "We know each other.");
            rewrite(VEGETABLES, "I love vegetables.");
            rewrite(HATS, "She wears hats.");
            finding(palabraDeOtroNivel(GRANDMOTHER, "My grandmother comes every year.", "grandmother", "A2", "A1"));
            score(REGULAR, LEMMA_ABSENCE, 0.995);
            score(PRESENT_SIMPLE, LEMMA_ABSENCE, 0.99519);
            return this;
        }

        /** The same batch without the grandmother proposal: its exercise back as the course has it. */
        Foto sinLaAbuela() {
            rewrite(GRANDMOTHER, "It comes every year.");
            nodes.get(GRANDMOTHER).getFindings().clear();
            score(REGULAR, LEMMA_ABSENCE, 1.0);
            score(PRESENT_SIMPLE, LEMMA_ABSENCE, 0.99552);
            return this;
        }

        /** The bands 8 · 6 · 4 · 3 José confirmed on 30/9: 73,86 the course, 96,02 A1, lemma-count 66,5. */
        Foto conLasBandasNuevas() {
            measure(LEMMA_COUNT, BANDS_8_6_4_3, Map.of("bands", "8 · 6 · 4 · 3"));
            vocabulary(ROOT, 0.7386);
            vocabulary(A1, 0.9602);
            vocabulary(A2, 0.9304);
            vocabulary(B1, 0.7829);
            score(ROOT, LEMMA_COUNT, 0.665);
            score(A1, LEMMA_COUNT, 0.9333);
            return this;
        }

        Foto vocabulary(String id, double value) {
            numbers(id).setVocabularyScore(value);
            return this;
        }

        Foto score(String id, String analyzer, double value) {
            ContextNumbers numbers = numbers(id);
            List<AnalyzerScore> scores = new ArrayList<>();
            for (AnalyzerScore score : numbers.getAnalyzerScores()) {
                if (!analyzer.equals(score.getAnalyzer())) {
                    scores.add(score);
                }
            }
            scores.add(new AnalyzerScore(analyzer, AnalyzerFamily.VOCABULARY, value, List.of()));
            numbers.setAnalyzerScores(scores);
            nodes.get(id).getScores().put(analyzer, value);
            return this;
        }

        /** The exercises with an error of an analyzer, of the ones below the node, and the ones it left open. */
        Foto errors(String id, String analyzer, int withError, int notEvaluated) {
            ErrorCounts errors = errorCounts(id);
            int reached = quizzesUnder(nodes.get(id));
            int evaluated = reached - notEvaluated;
            List<AnalyzerErrorCounts> analyzers = new ArrayList<>();
            for (AnalyzerErrorCounts counts : errors.getAnalyzers()) {
                if (!analyzer.equals(counts.getAnalyzer())) {
                    analyzers.add(counts);
                }
            }
            analyzers.add(new AnalyzerErrorCounts(analyzer, reached, evaluated, notEvaluated, withError,
                    evaluated > 0 ? (double) withError / evaluated : 0.0, 0, 0));
            errors.setAnalyzers(analyzers);
            return this;
        }

        /** «algún error» of a node as published: of how many exercises, how many have one, how many are open. */
        Foto anyError(String id, int quizzes, int withAny, int notFullyEvaluated) {
            ErrorCounts errors = errorCounts(id);
            errors.setQuizzes(quizzes);
            errors.setWithAnyError(withAny);
            errors.setAnyErrorShare(quizzes > 0 ? (double) withAny / quizzes : 0.0);
            errors.setNotFullyEvaluated(notFullyEvaluated);
            if (errors.getAnalyzers().isEmpty()) {
                errors(id, HINT, 0, 0);
            }
            return this;
        }

        Foto finding(Finding finding) {
            nodes.get(finding.getNode().getNodeId()).getFindings().add(finding);
            return this;
        }

        Foto unevaluated(String id, String analyzer) {
            nodes.get(id).getUnevaluatedBy().add(analyzer);
            return this;
        }

        Foto measure(String analyzer, String fingerprint, Map<String, Object> config) {
            root.getMeasures().removeIf(m -> analyzer.equals(m.getAnalyzer()));
            root.getMeasures().add(new AnalyzerMeasure(analyzer, fingerprint, config));
            return this;
        }

        /** The exercise with another sentence, as the batch leaves it. */
        Foto rewrite(String quizId, String sentence) {
            nodes.get(quizId).setEntity(quizEntity(quizId, sentence));
            return this;
        }

        AuditableQuiz entity(String quizId) {
            return (AuditableQuiz) nodes.get(quizId).getEntity();
        }

        AuditReport report() {
            return new AuditReport(root);
        }

        private ContextNumbers numbers(String id) {
            AuditNode node = nodes.get(id);
            if (node.getNumbers() == null) {
                node.setNumbers(new ContextNumbers(null, new ArrayList<>(), null, List.of()));
            }
            return node.getNumbers();
        }

        private ErrorCounts errorCounts(String id) {
            ContextNumbers numbers = numbers(id);
            if (numbers.getErrors() == null) {
                numbers.setErrors(new ErrorCounts(quizzesUnder(nodes.get(id)), 0, 0.0, null, 0, new ArrayList<>()));
            }
            return numbers.getErrors();
        }

        private AuditNode add(AuditNode parent, com.learney.contentaudit.auditdomain.AuditableEntity entity,
                AuditTarget target) {
            AuditNode node = new AuditNode(entity, target, parent, new ArrayList<>(), new LinkedHashMap<>(),
                    new LinkedHashMap<>(), null);
            parent.getChildren().add(node);
            nodes.put(entity.getId(), node);
            return node;
        }

        private AuditNode knowledge(AuditNode topic, String id, String title) {
            return add(topic, new AuditableKnowledge(new ArrayList<>(), title, "Escribe la forma correcta.", true, id,
                    title, id, SentenceMode.FILL, topic.getEntity().getLabel()), AuditTarget.KNOWLEDGE);
        }

        private void quiz(AuditNode knowledge, String id, String sentence) {
            add(knowledge, quizEntity(id, sentence), AuditTarget.QUIZ);
        }

        private static AuditableQuiz quizEntity(String id, String sentence) {
            return new AuditableQuiz(List.of(), id, sentence, id, null, List.of(sentence), sentence,
                    "Escribe la forma correcta.", List.of(), null, null, null);
        }

        private static int quizzesUnder(AuditNode node) {
            if (node.getTarget() == AuditTarget.QUIZ) {
                return 1;
            }
            int count = 0;
            for (AuditNode child : node.getChildren()) {
                count += quizzesUnder(child);
            }
            return count;
        }
    }

    /**
     * The comparator with doubles of its ports: the projector gives the batch start and the projection, the
     * baseline is read from its store and the history, the goals are the book's over a declaration and a sealed
     * store in memory, and what the topic teaches is decided by D's resolver, the real one.
     */
    static final class Mesa {
        final CourseProjector projector = mock(CourseProjector.class);
        final BaselineStore baselines = mock(BaselineStore.class);
        final AuditReportStore reports = mock(AuditReportStore.class);
        final AnalyzerCatalog catalog = mock(AnalyzerCatalog.class);
        final AuditEngine engine = mock(AuditEngine.class);
        final ChangeAttributor attributor = mock(ChangeAttributor.class);
        final SampleOutcomeSource samples = mock(SampleOutcomeSource.class);
        final List<SealedGoals> sealed = new ArrayList<>();
        GoalDeclaration declaration = new GoalDeclaration(List.of(), List.of(), null);
        private final SealedGoalsStore sealedStore = new SealedGoalsStore() {
            @Override
            public Optional<SealedGoals> read() {
                return sealed.isEmpty() ? Optional.empty() : Optional.of(sealed.get(sealed.size() - 1));
            }

            @Override
            public void write(SealedGoals sealedGoals) {
                sealed.add(sealedGoals);
            }
        };

        Mesa() {
            when(catalog.list()).thenReturn(CARDS);
            when(catalog.find(anyString())).thenAnswer(call -> CARDS.stream()
                    .filter(card -> card.getName().equals(call.getArgument(0))).findFirst());
            when(catalog.measure(anyString())).thenReturn(Optional.empty());
            when(samples.outcomeFor(any())).thenReturn(Optional.empty());
        }

        DefaultGoalBook book() {
            return new DefaultGoalBook(() -> declaration, sealedStore, catalog);
        }

        DefaultBatchComparator comparator() {
            return new DefaultBatchComparator(projector, baselines, reports, book(), catalog, engine, attributor,
                    samples, new DefaultTopicTeachingResolver(), new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create()));
        }

        Mesa baseline(Foto foto) {
            when(baselines.read()).thenReturn(Optional.of(new Baseline(BASELINE, FIXED_AT)));
            when(reports.load(BASELINE)).thenReturn(Optional.of(foto.report()));
            return this;
        }

        CourseProjection batch(Foto start, Foto projected, ProjectedChange... changes) {
            AuditReport approved = start.report();
            CourseProjection projection = new CourseProjection(AUDIT, PLAN, PLAN, ConsolidatedViewAvailability.AVAILABLE,
                    null, approved, approved, projected.report(), new ArrayList<>(List.of(changes)), new ArrayList<>());
            when(projector.project(any(), any())).thenReturn(projection);
            return projection;
        }

        void remeasured(Foto foto) {
            when(engine.runAudit(any(AuditableCourse.class), any(AnalyzerRunSelection.class))).thenReturn(foto.report());
        }

        void samples(SampleOutcome outcome) {
            when(samples.outcomeFor(any())).thenReturn(Optional.of(outcome));
        }

        BatchComparison compare() {
            return comparator().compare(COURSE, null);
        }
    }

    @Test
    @DisplayName("should show the vocabulary row as «Vocabulario, puntaje total» and the any-error row as «Ejercicios con algún error», keeping their subjects, the keys the goals name")
    @Tag("FEAT-FILAS")
    @Tag("F-FILAS-R001")
    public void shouldShowTheVocabularyRowAsVocabularioPuntajeTotalAndTheAnyerrorRowAsEjerciciosConAlgnErrorKeepingTheirSubjectsTheKeysTheGoalsName() {
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.declaration = new GoalDeclaration(List.of(), List.of(ANY_ERROR_IN_A1_ZERO), null);
        mesa.batch(Foto.deHoy(), Foto.deHoy());

        BatchComparison comparison = mesa.compare();

        for (String node : List.of(ROOT, A1, PRESENT_SIMPLE, REGULAR)) {
            ComparisonRow vocabulary = row(comparison, "vocabulario", node);
            assertEquals("Vocabulario, puntaje total", vocabulary.getDisplayName(), "R001: the vocabulary row of " + node);
            assertEquals("vocabulario", vocabulary.getSubject(), "R001 inv. 2: its key stays");
            ComparisonRow anyError = row(comparison, "algún error", node);
            assertEquals("Ejercicios con algún error", anyError.getDisplayName(), "R001: the any-error row of " + node);
            assertEquals("algún error", anyError.getSubject(), "R001 inv. 2: its key stays");
        }
        assertEquals("algún error en A1: 0", row(comparison, "algún error", A1).getFloor().getGoalId(),
                "R001 inv. 2: the goal declared for «algún error» still applies to its row");
        assertEquals("piso de errores · algún error · KNOWLEDGE",
                row(comparison, "algún error", REGULAR).getFloor().getGoalId(),
                "R001 inv. 2: and so does the floor of the family, with the id the sealed goals keep");
        assertEquals("piso de vocabulario · vocabulario · COURSE",
                row(comparison, "vocabulario", ROOT).getFloor().getGoalId());
    }

    @Test
    @DisplayName("should show each analyzer row with the Spanish name of its card, and with its unique name when today's catalog does not know it")
    @Tag("FEAT-FILAS")
    @Tag("F-FILAS-R001")
    public void shouldShowEachAnalyzerRowWithTheSpanishNameOfItsCardAndWithItsUniqueNameWhenTodaysCatalogDoesNotKnowIt() {
        // «dictado» ran in both photos but today's catalog does not know it: only its published numbers do.
        Mesa mesa = new Mesa().baseline(Foto.deHoy().measure("dictado", "dictado · v1", Map.of())
                .score(ROOT, "dictado", 0.5));
        mesa.batch(Foto.deHoy(), Foto.deHoy().measure("dictado", "dictado · v1", Map.of()).score(ROOT, "dictado", 0.5));

        BatchComparison comparison = mesa.compare();

        assertEquals("Vocabulario del nivel presente", row(comparison, LEMMA_ABSENCE, REGULAR).getDisplayName(),
                "R001: the name of its card");
        assertEquals("Palabras vistas suficientes veces", row(comparison, LEMMA_COUNT, A1).getDisplayName());
        assertEquals("Pista que falta, confunde o regala la respuesta", row(comparison, HINT, A1).getDisplayName());
        assertEquals("Consigna que no se cumple", row(comparison, INSTRUCTION, ROOT).getDisplayName());
        for (ComparisonRow row : comparison.getRows()) {
            if (SPANISH.containsKey(row.getSubject())) {
                assertEquals(SPANISH.get(row.getSubject()), row.getDisplayName(),
                        "R001: " + row.getSubject() + " in " + row.getNodeId() + ", its unique name still the subject");
            }
        }
        ComparisonRow unknown = row(comparison, "dictado", ROOT);
        assertEquals("dictado", unknown.getDisplayName(), "R001 inv. 4: an analyzer the catalog does not know");
        assertNull(unknown.getExplanation(), "F-FILAS-R004 inv. 3: and no sentence");
    }

    @Test
    @DisplayName("should say that lower is better in every row of the errors family and in the any-error row, and higher in every row of the vocabulary family and in the vocabulary row")
    @Tag("FEAT-FILAS")
    @Tag("F-FILAS-R002")
    public void shouldSayThatLowerIsBetterInEveryRowOfTheErrorsFamilyAndInTheAnyerrorRowAndHigherInEveryRowOfTheVocabularyFamilyAndInTheVocabularyRow() {
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.batch(Foto.deHoy(), Foto.deHoy());

        BatchComparison comparison = mesa.compare();

        assertEquals(BetterDirection.LOWER, row(comparison, HINT, REGULAR).getBetterWhen(), "R002: a problem row");
        assertEquals(BetterDirection.LOWER, row(comparison, INSTRUCTION, ROOT).getBetterWhen());
        assertEquals(BetterDirection.LOWER, row(comparison, "algún error", A1).getBetterWhen());
        assertEquals(BetterDirection.HIGHER, row(comparison, LEMMA_ABSENCE, REGULAR).getBetterWhen(), "R002: a score row");
        assertEquals(BetterDirection.HIGHER, row(comparison, LEMMA_COUNT, A1).getBetterWhen());
        assertEquals(BetterDirection.HIGHER, row(comparison, "vocabulario", ROOT).getBetterWhen());
        for (ComparisonRow row : comparison.getRows()) {
            assertEquals(row.getFamily() == AnalyzerFamily.ERRORS ? BetterDirection.LOWER : BetterDirection.HIGHER,
                    row.getBetterWhen(), "R002: " + row.getSubject() + " in " + row.getNodeId());
        }
    }

    @Test
    @DisplayName("should mark a drop of an errors row as improved and a rise as worsened, and a rise of a vocabulary row as improved and a drop as worsened")
    @Tag("FEAT-FILAS")
    @Tag("F-FILAS-R003")
    public void shouldMarkADropOfAnErrorsRowAsImprovedAndARiseAsWorsenedAndARiseOfAVocabularyRowAsImprovedAndADropAsWorsened() {
        Mesa mesa = new Mesa().baseline(Foto.deHoy().errors(REGULAR, HINT, 2, 0));
        mesa.batch(Foto.deHoy().errors(REGULAR, HINT, 2, 0),
                Foto.deHoy().errors(REGULAR, HINT, 1, 0).errors(REGULAR, INSTRUCTION, 1, 0)
                        .score(REGULAR, LEMMA_ABSENCE, 0.995).score(A1, LEMMA_COUNT, 0.95),
                cambio(P_VEGETABLES, VEGETABLES));

        BatchComparison comparison = mesa.compare();

        ComparisonRow hint = row(comparison, HINT, REGULAR);
        assertEquals(-1.0, hint.getChange(), 1e-9);
        assertEquals(ChangeVerdict.IMPROVED, hint.getChangeVerdict(), "R003: 2 → 1 exercises with an error");
        assertEquals(ChangeVerdict.WORSENED, row(comparison, INSTRUCTION, REGULAR).getChangeVerdict(),
                "R003: 0 → 1 exercises with an error");
        ComparisonRow absence = row(comparison, LEMMA_ABSENCE, REGULAR);
        assertEquals(-0.5, absence.getChange(), 1e-9);
        assertEquals(ChangeVerdict.WORSENED, absence.getChangeVerdict(), "R003: 100,0 → 99,5 points");
        assertEquals(ChangeVerdict.IMPROVED, row(comparison, LEMMA_COUNT, A1).getChangeVerdict(),
                "R003: 94,37 → 95,0 points");
    }

    @Test
    @DisplayName("should mark as unchanged a row whose change rounds to zero as it is shown, to two decimals in points and to whole exercises in errors")
    @Tag("FEAT-FILAS")
    @Tag("F-FILAS-R003")
    public void shouldMarkAsUnchangedARowWhoseChangeRoundsToZeroAsItIsShownToTwoDecimalsInPointsAndToWholeExercisesInErrors() {
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.batch(Foto.deHoy(), Foto.deHoy().score(PRESENT_SIMPLE, LEMMA_ABSENCE, 0.99549)
                .score(WITH_BE, LEMMA_ABSENCE, 0.98994), cambio(P_VEGETABLES, VEGETABLES));

        BatchComparison comparison = mesa.compare();

        ComparisonRow topic = row(comparison, LEMMA_ABSENCE, PRESENT_SIMPLE);
        assertEquals(-0.003, topic.getChange(), 1e-9);
        assertEquals(ChangeVerdict.UNCHANGED, topic.getChangeVerdict(), "R003: −0,003 points is shown as 0,0");
        assertEquals(ChangeVerdict.WORSENED, row(comparison, LEMMA_ABSENCE, WITH_BE).getChangeVerdict(),
                "R003: −0,006 points is shown as −0,01");
        ComparisonRow hint = row(comparison, HINT, REGULAR);
        assertEquals(0.0, hint.getChange(), 1e-9);
        assertEquals(ChangeVerdict.UNCHANGED, hint.getChangeVerdict(), "R003: 0 → 0 exercises");
        assertEquals(ChangeVerdict.UNCHANGED, row(comparison, "vocabulario", ROOT).getChangeVerdict(),
                "R003: 73,92 → 73,92");
    }

    @Test
    @DisplayName("should mark as not evaluated a row whose analyzer evaluated no exercise of the node, instead of reading its zero as a real zero")
    @Tag("FEAT-FILAS")
    @Tag("F-FILAS-R003")
    public void shouldMarkAsNotEvaluatedARowWhoseAnalyzerEvaluatedNoExerciseOfTheNodeInsteadOfReadingItsZeroAsARealZero() {
        // quiz-instruction ran without a budget: it left the four exercises of «Afirmativas regulares» open.
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.batch(Foto.deHoy(), Foto.deHoy().errors(REGULAR, INSTRUCTION, 0, 4), cambio(P_VEGETABLES, VEGETABLES));

        BatchComparison comparison = mesa.compare();

        ComparisonRow instruction = row(comparison, INSTRUCTION, REGULAR);
        assertEquals(new RowValue(0.0, 0.0, 4), instruction.getProjected(),
                "R003 inv. 2: its numbers are published as today, 0 (0,0 %) with 4 unevaluated");
        assertEquals(0.0, instruction.getChange(), 1e-9);
        assertEquals(ChangeVerdict.NOT_EVALUATED, instruction.getChangeVerdict(), "R003: not a real zero");
        assertEquals(ChangeVerdict.UNCHANGED, row(comparison, HINT, REGULAR).getChangeVerdict(),
                "R003: hint-coherence evaluated them: its zero is real");
    }

    @Test
    @DisplayName("should mark as not evaluated a row without a number in the baseline, as the error rows of the base of 29/9")
    @Tag("FEAT-FILAS")
    @Tag("F-FILAS-R003")
    public void shouldMarkAsNotEvaluatedARowWithoutANumberInTheBaselineAsTheErrorRowsOfTheBaseOf299() {
        Mesa mesa = new Mesa().baseline(Foto.del299());
        mesa.batch(Foto.deHoy(), Foto.deHoy(), cambio(P_VEGETABLES, VEGETABLES));

        BatchComparison comparison = mesa.compare();

        for (String subject : List.of(HINT, INSTRUCTION, "algún error")) {
            ComparisonRow row = row(comparison, subject, A1);
            assertNull(row.getBaseline(), "the base of 29/9 did not measure " + subject);
            assertEquals(new RowValue(0.0, 0.0, 0), row.getProjected(), "the projection did");
            assertEquals(ChangeVerdict.NOT_EVALUATED, row.getChangeVerdict(), "R003: no number in the baseline");
        }
        assertEquals(ChangeVerdict.UNCHANGED, row(comparison, "vocabulario", A1).getChangeVerdict(),
                "R003: what the base did measure has its verdict");
    }

    @Test
    @DisplayName("should mark as not evaluated the any-error row of a node where no errors analyzer evaluated an exercise")
    @Tag("FEAT-FILAS")
    @Tag("F-FILAS-R003")
    public void shouldMarkAsNotEvaluatedTheAnyerrorRowOfANodeWhereNoErrorsAnalyzerEvaluatedAnExercise() {
        // In «Afirmativas regulares» neither analyzer of errors evaluated an exercise; in «Afirmativas con be»
        // quiz-instruction left both open but hint-coherence evaluated them.
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.batch(Foto.deHoy(), Foto.deHoy().errors(REGULAR, HINT, 0, 4).errors(REGULAR, INSTRUCTION, 0, 4)
                .anyError(REGULAR, 4, 0, 4).errors(WITH_BE, INSTRUCTION, 0, 2).anyError(WITH_BE, 2, 0, 2),
                cambio(P_VEGETABLES, VEGETABLES));

        BatchComparison comparison = mesa.compare();

        ComparisonRow regular = row(comparison, "algún error", REGULAR);
        assertEquals(new RowValue(0.0, 0.0, 4), regular.getProjected());
        assertEquals(ChangeVerdict.NOT_EVALUATED, regular.getChangeVerdict(),
                "R003: no analyzer of errors evaluated an exercise of the knowledge");
        assertEquals(ChangeVerdict.UNCHANGED, row(comparison, "algún error", WITH_BE).getChangeVerdict(),
                "R003: one did, so its zero is a real one, open exercises and all");
    }

    @Test
    @DisplayName("should give each analyzer row the explanation of its card, and the vocabulary and any-error rows their own")
    @Tag("FEAT-FILAS")
    @Tag("F-FILAS-R004")
    public void shouldGiveEachAnalyzerRowTheExplanationOfItsCardAndTheVocabularyAndAnyerrorRowsTheirOwn() {
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.batch(Foto.deHoy(), Foto.deHoy());

        BatchComparison comparison = mesa.compare();

        assertEquals("La frase de lemma-absence.", row(comparison, LEMMA_ABSENCE, REGULAR).getExplanation(),
                "R004: the sentence of its card");
        assertEquals("La frase de hint-coherence.", row(comparison, HINT, A1).getExplanation());
        assertEquals("El puntaje de vocabulario de 0 a 100: el promedio de las filas de puntaje de arriba.",
                row(comparison, "vocabulario", ROOT).getExplanation(), "R004: the vocabulary row, its own");
        assertEquals("Ejercicios con al menos uno de los problemas de arriba; cada ejercicio cuenta una vez.",
                row(comparison, "algún error", ROOT).getExplanation(), "R004: the any-error row, its own");
        for (ComparisonRow row : comparison.getRows()) {
            assertNotNull(row.getExplanation(), "R004: " + row.getSubject() + " in " + row.getNodeId());
        }
    }

    @Test
    @DisplayName("should take as the example of a row a finding the batch fixes before one that stays in the projection, and say in its line that the batch fixes it")
    @Tag("FEAT-FILAS")
    @Tag("F-FILAS-R005")
    public void shouldTakeAsTheExampleOfARowAFindingTheBatchFixesBeforeOneThatStaysInTheProjectionAndSayInItsLineThatTheBatchFixesIt() {
        Finding fixed = hallazgo(HINT, "hint-words", HATS, FindingSeverity.HIGH, FindingResolution.PANEL, null,
                "La pista «(sport)» no anuncia «wears».");
        Finding stays = hallazgo(HINT, "hint-words", OURSELVES, FindingSeverity.HIGH, FindingResolution.PANEL, null,
                "La pista no anuncia «each other».");
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.batch(Foto.deHoy().finding(fixed).finding(stays), Foto.deHoy().finding(stays), cambio(P_HATS, HATS));

        BatchComparison comparison = mesa.compare();

        RowExample example = row(comparison, HINT, REGULAR).getExample();
        assertEquals(HATS, example.getQuizId(), "R005: the one the batch fixes, before one that stays with a lower id");
        assertEquals("La pista «(sport)» no anuncia «wears» — la tanda lo arregla", example.getDetail(),
                "R005: its line says the batch fixes it");
        assertEquals(example, row(comparison, HINT, PRESENT_SIMPLE).getExample(), "R005: also above the knowledge");

        Mesa without = new Mesa().baseline(Foto.deHoy());
        without.batch(Foto.deHoy().finding(stays), Foto.deHoy().finding(stays), cambio(P_HATS, HATS));
        RowExample staying = row(without.compare(), HINT, REGULAR).getExample();
        assertEquals(OURSELVES, staying.getQuizId(), "R005: with nothing fixed, one of the projection");
        assertEquals("La pista no anuncia «each other».", staying.getDetail());
    }

    @Test
    @DisplayName("should take as the example the finding of the lowest quiz id under the node, so the same comparison always gives the same example")
    @Tag("FEAT-FILAS")
    @Tag("F-FILAS-R005")
    public void shouldTakeAsTheExampleTheFindingOfTheLowestQuizIdUnderTheNodeSoTheSameComparisonAlwaysGivesTheSameExample() {
        Finding hats = hallazgo(HINT, "hint-words", HATS, FindingSeverity.HIGH, FindingResolution.PANEL, null,
                "La pista no anuncia «wears».");
        Finding vegetables = hallazgo(HINT, "hint-words", VEGETABLES, FindingSeverity.HIGH, FindingResolution.PANEL,
                null, "La pista no anuncia «a lot of».");
        // A finding of the knowledge itself is less concrete than one of an exercise below it.
        Finding ofTheKnowledge = new Finding(HINT, "hint-share", new FindingNodeRef(AuditTarget.KNOWLEDGE, REGULAR,
                "Afirmativas regulares"), FindingSeverity.HIGH, new FindingEvidence(List.of(),
                "Tres de cuatro llevan pista.", List.of()), FindingResolution.PANEL, AnalysisCost.INSTANT,
                new FindingIdentity(HINT + "|hint-share|" + REGULAR, null));
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.batch(Foto.deHoy().finding(ofTheKnowledge).finding(hats).finding(vegetables),
                Foto.deHoy().finding(ofTheKnowledge).finding(hats).finding(vegetables));

        RowExample example = row(mesa.compare(), HINT, REGULAR).getExample();

        assertEquals(VEGETABLES, example.getQuizId(), "R005: the lowest quiz id under the knowledge, an exercise "
                + "before the knowledge itself");
        assertEquals("La pista no anuncia «a lot of».", example.getDetail());
        assertEquals(example, row(mesa.compare(), HINT, REGULAR).getExample(), "R005: the same comparison, again");
        Mesa reordered = new Mesa().baseline(Foto.deHoy());
        reordered.batch(Foto.deHoy().finding(vegetables).finding(hats).finding(ofTheKnowledge),
                Foto.deHoy().finding(vegetables).finding(hats).finding(ofTheKnowledge));
        assertEquals(example, row(reordered.compare(), HINT, REGULAR).getExample(),
                "R005: whatever order the findings come in");
        assertEquals(VEGETABLES, row(mesa.compare(), HINT, ROOT).getExample().getQuizId(), "R005: up to the course");
    }

    @Test
    @DisplayName("should leave the example null when the analyzer has no finding under the node")
    @Tag("FEAT-FILAS")
    @Tag("F-FILAS-R005")
    public void shouldLeaveTheExampleNullWhenTheAnalyzerHasNoFindingUnderTheNode() {
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.batch(Foto.deHoy(), Foto.deHoy().conLasCuatro(), LAS_CUATRO);

        BatchComparison comparison = mesa.compare();

        assertNotNull(row(comparison, LEMMA_ABSENCE, REGULAR).getExample(), "the grandmother is there");
        assertNull(row(comparison, LEMMA_ABSENCE, A2).getExample(), "R005: nothing of lemma-absence under A2");
        assertNull(row(comparison, LEMMA_ABSENCE, WITH_BE).getExample(), "R005: nor in «Afirmativas con be»");
        assertNull(row(comparison, HINT, REGULAR).getExample(), "R005: hint-coherence found nothing");
        assertNull(row(comparison, "algún error", ROOT).getExample(), "R005: no error in the whole course");
    }

    @Test
    @DisplayName("should never take as the example of an errors row a finding that only ranks, as the ones of naturalness")
    @Tag("FEAT-FILAS")
    @Tag("F-FILAS-R005")
    public void shouldNeverTakeAsTheExampleOfAnErrorsRowAFindingThatOnlyRanksAsTheOnesOfNaturalness() {
        Finding doubtful = hallazgo(NATURALNESS, "unlikely-sentence", VEGETABLES, FindingSeverity.LOW,
                FindingResolution.RANK_ONLY, null, "El modelo duda de que un nativo diga «I love vegetables.».");
        Mesa mesa = new Mesa().baseline(Foto.deHoy().measure(NATURALNESS, "naturalness · llama3", Map.of())
                .errors(REGULAR, NATURALNESS, 0, 0));
        mesa.batch(Foto.deHoy().measure(NATURALNESS, "naturalness · llama3", Map.of())
                        .errors(REGULAR, NATURALNESS, 0, 0).finding(doubtful),
                Foto.deHoy().measure(NATURALNESS, "naturalness · llama3", Map.of())
                        .errors(REGULAR, NATURALNESS, 0, 0).finding(doubtful));

        BatchComparison comparison = mesa.compare();

        assertNull(row(comparison, NATURALNESS, REGULAR).getExample(),
                "R005: it only ranks, so it never illustrates a row that counts errors");
        assertNull(row(comparison, "algún error", REGULAR).getExample(), "R005: nor «algún error»");
    }

    @Test
    @DisplayName("should show the exercise of the example as the student sees it, with its gap and its hint, and say in one line what it has")
    @Tag("FEAT-FILAS")
    @Tag("F-FILAS-R005")
    public void shouldShowTheExerciseOfTheExampleAsTheStudentSeesItWithItsGapAndItsHintAndSayInOneLineWhatItHas() {
        Foto projected = Foto.deHoy().conLasCuatro();
        projected.nodes.get(GRANDMOTHER).setEntity(new AuditableQuiz(List.of(), GRANDMOTHER,
                "My grandmother comes every year.", GRANDMOTHER, "Mi abuela viene todos los años.",
                List.of("My grandmother comes every year."), "My grandmother comes every year.",
                "Escribe la forma correcta.", List.of(new SentencePartEntity(SentencePartKind.TEXT, "My grandmother", null),
                        new SentencePartEntity(SentencePartKind.CLOZE, "", List.of("comes")),
                        new SentencePartEntity(SentencePartKind.TEXT, "(come) every year.", null)), null, null, null));
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.batch(Foto.deHoy(), projected, LAS_CUATRO);

        BatchComparison comparison = mesa.compare();

        RowExample example = row(comparison, LEMMA_ABSENCE, REGULAR).getExample();
        assertEquals(new RowExample(GRANDMOTHER, "My grandmother ___ (come) every year.",
                "grandmother es una palabra de A2 en un ejercicio de A1"), example,
                "R005: the exercise with its gap and its hint, and what it has, in one line");
    }

    @Test
    @DisplayName("should take as the example of a score row a concrete finding of the analyzer, as a word of the level, with no exercise")
    @Tag("FEAT-FILAS")
    @Tag("F-FILAS-R005")
    public void shouldTakeAsTheExampleOfAScoreRowAConcreteFindingOfTheAnalyzerAsAWordOfTheLevelWithNoExercise() {
        Finding butter = new Finding(LEMMA_COUNT, "under-exposed", new FindingNodeRef(AuditTarget.MILESTONE, A1, "A1"),
                FindingSeverity.LOW, new FindingEvidence(List.of(new EvidencePart("Palabra", "butter"),
                        new EvidencePart("Oraciones", "2 de 6")), "butter (A1): 2 oraciones de 6", List.of()),
                FindingResolution.RANK_ONLY, AnalysisCost.INSTANT,
                new FindingIdentity(LEMMA_COUNT + "|under-exposed|" + A1 + "|butter|NOUN", "butter|NOUN"));
        Finding ofTheCourse = new Finding(LEMMA_COUNT, "under-exposed", new FindingNodeRef(AuditTarget.COURSE, ROOT,
                "Curso"), FindingSeverity.LOW, new FindingEvidence(List.of(new EvidencePart("Nivel", "A1")),
                "A1 da 94,37 %: 12 de sus 300 palabras aparecen en menos oraciones que las que pide su banda",
                List.of()), FindingResolution.RANK_ONLY, AnalysisCost.INSTANT,
                new FindingIdentity(LEMMA_COUNT + "|under-exposed|root|A1", "A1"));
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.batch(Foto.deHoy().finding(ofTheCourse).finding(butter), Foto.deHoy().finding(ofTheCourse).finding(butter));

        BatchComparison comparison = mesa.compare();

        RowExample word = new RowExample(null, null, "butter (A1): 2 oraciones de 6");
        assertEquals(word, row(comparison, LEMMA_COUNT, A1).getExample(), "R005: a word of the level, no exercise");
        assertEquals(word, row(comparison, LEMMA_COUNT, ROOT).getExample(),
                "R005: in the course too, the word before the summary of the level");
        assertEquals(new RowExample(null, null, "Palabras vistas suficientes veces: butter (A1): 2 oraciones de 6"),
                row(comparison, "vocabulario", ROOT).getExample(), "R005: the vocabulary row names the analyzer");
    }

    @Test
    @DisplayName("should not take as fixed a finding the projection lost only because its analyzer left the exercise unevaluated")
    @Tag("FEAT-FILAS")
    @Tag("F-FILAS-R005")
    public void shouldNotTakeAsFixedAFindingTheProjectionLostOnlyBecauseItsAnalyzerLeftTheExerciseUnevaluated() {
        Finding open = hallazgo(INSTRUCTION, "instruction-breach", VEGETABLES, FindingSeverity.MEDIUM,
                FindingResolution.PANEL, null, "No cumple la consigna.");
        Finding stays = hallazgo(INSTRUCTION, "instruction-breach", HATS, FindingSeverity.MEDIUM,
                FindingResolution.PANEL, null, "Tampoco cumple la consigna.");
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.batch(Foto.deHoy().finding(open).finding(stays),
                Foto.deHoy().unevaluated(VEGETABLES, INSTRUCTION).finding(stays), cambio(P_VEGETABLES, VEGETABLES));

        RowExample example = row(mesa.compare(), INSTRUCTION, REGULAR).getExample();

        assertEquals(HATS, example.getQuizId(), "R005: quiz-instruction did not judge the new exercise -- its "
                + "finding vanished for lack of a budget, it is not fixed");
        assertEquals("Tampoco cumple la consigna.", example.getDetail());

        Mesa judged = new Mesa().baseline(Foto.deHoy());
        judged.batch(Foto.deHoy().finding(open).finding(stays), Foto.deHoy().finding(stays),
                cambio(P_VEGETABLES, VEGETABLES));
        RowExample fixed = row(judged.compare(), INSTRUCTION, REGULAR).getExample();
        assertEquals(VEGETABLES, fixed.getQuizId(), "R005: judged and gone, it is fixed");
        assertEquals("No cumple la consigna — la tanda lo arregla", fixed.getDetail());
    }

    @Test
    @DisplayName("should take as the example of the any-error row an error of any analyzer, and name the analyzer in its line")
    @Tag("FEAT-FILAS")
    @Tag("F-FILAS-R005")
    public void shouldTakeAsTheExampleOfTheAnyerrorRowAnErrorOfAnyAnalyzerAndNameTheAnalyzerInItsLine() {
        Finding hint = hallazgo(HINT, "hint-words", HATS, FindingSeverity.HIGH, FindingResolution.PANEL, null,
                "La pista no anuncia «wears».");
        Finding instruction = hallazgo(INSTRUCTION, "instruction-breach", VEGETABLES, FindingSeverity.MEDIUM,
                FindingResolution.PANEL, null, "No cumple la consigna.");
        Mesa mesa = new Mesa().baseline(Foto.deHoy());
        mesa.batch(Foto.deHoy().finding(hint).finding(instruction), Foto.deHoy().finding(hint).finding(instruction));

        BatchComparison comparison = mesa.compare();

        RowExample anyError = row(comparison, "algún error", REGULAR).getExample();
        assertEquals(VEGETABLES, anyError.getQuizId(), "R005: an error of any analyzer, the lowest id");
        assertEquals("Consigna que no se cumple: No cumple la consigna.", anyError.getDetail(),
                "R005: its line names the analyzer");
        RowExample own = row(comparison, HINT, REGULAR).getExample();
        assertEquals(HATS, own.getQuizId(), "R005: the row of an analyzer, only its own findings");
        assertEquals("La pista no anuncia «wears».", own.getDetail(), "R005: the row already names its analyzer");
    }
}
