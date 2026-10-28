package com.learney.contentaudit.revisiondomain.batchengine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.learney.contentaudit.refinerdomain.RefinementPlan;
import com.learney.contentaudit.refinerdomain.RefinementPlanStore;
import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.refinerdomain.RefinementTaskStatus;
import com.learney.contentaudit.revisiondomain.RevisionArtifactStore;
import com.learney.contentaudit.revisiondomain.RevisionArtifact;
import com.learney.contentaudit.revisiondomain.RevisionProposal;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.panel.PanelCandidate;
import com.learney.contentaudit.revisiondomain.panel.PanelDecision;
import com.learney.contentaudit.revisiondomain.panel.PanelOutcomeKind;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class DefaultBatchReviewTest {
    @Test void forwardsTheReviewJournalGuardToTheAtomicStore() {
        var item = review.read("batch").items().get(0);
        review.decide(new BatchReviewDecision("batch", List.of(new BatchReviewDecision.Selection(
                item.changeId(), item.step(), item.before(), item.after())), BatchReviewDecision.Status.REJECTED,
                "Problemas", Instant.now(), 0L));
        verify(store).saveReview(argThat(d -> Long.valueOf(0).equals(d.expectedReviewCount())));
    }
    private final BatchStore store = mock(BatchStore.class);
    private final RevisionArtifactStore artifacts = mock(RevisionArtifactStore.class);
    private final RefinementPlanStore plans = mock(RefinementPlanStore.class);
    private final List<BatchReviewDecision> decisions = new ArrayList<>();
    private final CorrectionBatch batch = new CorrectionBatch();
    private final BatchChangeStep step = new BatchChangeStep("quiz", List.of(ExercisePart.TRANSLATION), snapshot("after", "out of scope"));
    private final BatchChangeStep second = new BatchChangeStep("quiz", List.of(ExercisePart.MINI_THEORY), snapshot("wrong translation", "new theory"));
    private final DefaultBatchReview review = new DefaultBatchReview(store, artifacts, plans,
            new DefaultBatchReportBuilder());

    @BeforeEach void setup() {
        batch.setId("batch");
        batch.setPlanId("plan");
        batch.setStatus(BatchStatus.RUNNING);
        batch.setChanges(List.of(change("one", step), change("two", second)));
        batch.setQuizProposals(List.of(new BatchProposalRef("quiz", "proposal")));
        var proposal = new RevisionProposal();
        proposal.setProposalId("proposal");
        proposal.setNodeId("quiz");
        proposal.setElementBefore(snapshot("before", "old theory"));
        proposal.setElementAfter(snapshot("artifact ahead of checkpoint", "not saved"));
        var artifact = new RevisionArtifact();
        artifact.setProposal(proposal);
        when(artifacts.load("plan", "proposal")).thenReturn(Optional.of(artifact));
        when(store.load("batch")).thenReturn(Optional.of(batch));
        when(store.reviews("batch")).thenAnswer(invocation -> new ArrayList<>(decisions));
        doAnswer(invocation -> { decisions.add(invocation.getArgument(0)); return null; }).when(store).saveReview(any());
    }

    @Test @Tag("FEAT-ABIER") @Tag("F-ABIER-R001")
    void savesOneDecisionDuringRunWithoutWritingBatchOrApplyingCourse() {
        var result = review.decide(request(List.of(selection("one", step)),
                BatchReviewDecision.Status.APPROVED));
        assertEquals(BatchReviewDecision.Status.APPROVED, result.items().get(0).status());
        assertEquals(BatchReviewDecision.Status.PENDING, result.items().get(1).status());
        assertEquals(BatchStatus.RUNNING, batch.getStatus());
        verify(store, never()).save(any());
        verify(artifacts, never()).save(any());
    }

    @Test void unchangedReviewUsesBackendProjectionWithoutReloadingArtifactsOrPlans() {
        var saved = review.read("batch");
        var cache = mock(BatchReviewCache.class);
        when(cache.version()).thenReturn(Optional.of("v1"));
        when(cache.load("batch", "v1")).thenReturn(Optional.of(saved));
        clearInvocations(store, artifacts, plans);
        assertSame(saved, cachedReview(cache).read("batch"));
        verifyNoInteractions(store, artifacts, plans);
        verify(cache, never()).save(any(), any(), any());
    }

    @Test void cacheMissBuildsOnceAndSavesOnlyUnderTheVersionItRead() {
        var cache = mock(BatchReviewCache.class);
        when(cache.version()).thenReturn(Optional.of("v1"));
        var result = cachedReview(cache).read("batch");
        verify(cache).save("batch", "v1", result);
        verify(artifacts).load("plan", "proposal");
    }

    @Test void projectsFrozenRecorrectionReferencesAndKeepsOldBatchCompatible() {
        assertEquals(List.of(), review.read("batch").comparisonBaselines());
        var baseline = new QuizCorrection.ComparisonBaseline("recorrect-one", 5, "quiz",
                snapshot("current translation", "current theory"), snapshot("original translation", "original theory"));
        batch.setComparisonBaselines(List.of(baseline));
        var result = review.read("batch");
        assertEquals(List.of(baseline), result.comparisonBaselines());
        assertEquals("current translation", result.comparisonBaselines().get(0).before().getQuiz().getTranslation());
        verify(store, never()).save(any());
    }

    @Test void legacyCachedProjectionWithoutComparisonSchemaIsRebuilt() {
        var prior = review.read("batch");
        var legacy = new BatchReview(prior.batchId(), prior.correctionRevision(), prior.contentVersion(), prior.report(),
                prior.tasks(), prior.proposals(), prior.items(), prior.analyses(), prior.history(),
                prior.selectiveRuns(), prior.opinionRuns(), null);
        var baseline = new QuizCorrection.ComparisonBaseline("recorrect-one", 5, "quiz",
                snapshot("current", "current"), snapshot("original", "original"));
        batch.setComparisonBaselines(List.of(baseline));
        var cache = mock(BatchReviewCache.class);
        when(cache.version()).thenReturn(Optional.of("v1"));
        when(cache.load("batch", "v1")).thenReturn(Optional.of(legacy));
        var result = cachedReview(cache).read("batch");
        assertNotSame(legacy, result); assertEquals(List.of(baseline), result.comparisonBaselines());
        verify(cache).save("batch", "v1", result);
    }

    @Test void concurrentCheckpointDoesNotMakeOldCachedOrRebuiltReviewCurrent() {
        var old = review.read("batch");
        var cache = mock(BatchReviewCache.class);
        when(cache.version()).thenReturn(Optional.of("v1"), Optional.of("v2"));
        when(cache.load("batch", "v1")).thenReturn(Optional.of(old));
        var result = cachedReview(cache).read("batch");
        assertNotSame(old, result);
        verify(cache, never()).save(any(), any(), any());
    }

    @Test void decisionsAlwaysValidateAuthoritativeStepsAndNeverTrustCachedApprovals() {
        var cache = mock(BatchReviewCache.class);
        cachedReview(cache).decide(request(List.of(selection("one", step)), BatchReviewDecision.Status.APPROVED));
        verify(cache, never()).load(any(), any());
        assertEquals(1, decisions.size());
    }

    @Test void exposesExactTasksAndTheMatchingAcceptedRationaleWithoutWrites() {
        batch.getChanges().get(0).setTaskIds(List.of("grammar", "mini-theory"));
        var accepted = new PanelCandidate();
        accepted.setQuiz(snapshot("after", "old theory").getQuiz());
        accepted.setRationale("Changed vocabulary and aligned the translation.");
        var decision = new PanelDecision();
        decision.setOutcome(PanelOutcomeKind.ACCEPTED);
        decision.setQuizIds(List.of("quiz"));
        decision.setTaskIds(List.of("grammar", "mini-theory"));
        decision.setAccepted(accepted);
        batch.setPanelDecisions(List.of(decision));
        var item = review.read("batch").items().get(0);
        assertEquals(List.of("grammar", "mini-theory"), item.taskIds());
        assertEquals(accepted.getRationale(), item.rationale());
        assertNull(review.read("batch").items().get(1).rationale());
        verify(store, never()).save(any());
        verify(store, never()).saveReview(any());
        verify(artifacts, never()).save(any());
    }

    @Test @Tag("FEAT-MOTIVO") @Tag("F-MOTIVO-R001")
    void theReviewCarriesTheTasksItsChangesPointToThoughTheyAreNotChosenWork() {
        batch.setTaskIds(List.of("chosen"));
        batch.getChanges().get(0).setTaskIds(List.of("carried"));
        var dropped = change("dropped", step);
        dropped.setTaskIds(List.of("of-a-dropped-change"));
        dropped.setDroppedReason("volvió a pendiente: la reemplazó otra pasada");
        batch.setChanges(List.of(batch.getChanges().get(0), batch.getChanges().get(1), dropped));
        var plan = new RefinementPlan();
        plan.setTasks(List.of(task("chosen"), task("carried"), task("of-a-dropped-change"), task("unrelated")));
        when(plans.load("plan")).thenReturn(Optional.of(plan));
        assertEquals(List.of("chosen", "carried"),
                review.read("batch").tasks().stream().map(RefinementTask::getId).toList());
        assertEquals(List.of("chosen"), batch.getTaskIds(), "the copies are not chosen work");
    }

    private static RefinementTask task(String id) {
        return new RefinementTask(id, AuditTarget.QUIZ, "quiz", "quiz", null, 0, RefinementTaskStatus.IN_BATCH, null);
    }

    @Test void doesNotAttributeANonMatchingOrAmbiguousHistoricalCandidate() {
        var one = new PanelCandidate();
        one.setQuiz(snapshot("another answer", "old theory").getQuiz());
        one.setRationale("Not this saved step.");
        var decision = new PanelDecision();
        decision.setOutcome(PanelOutcomeKind.ACCEPTED);
        decision.setQuizIds(List.of("quiz"));
        decision.setTaskIds(List.of());
        decision.setAccepted(one);
        batch.setPanelDecisions(List.of(decision));
        assertNull(review.read("batch").items().get(0).rationale());
        one.setQuiz(snapshot("after", "old theory").getQuiz());
        decision.setOutcome(PanelOutcomeKind.NO_WINNER);
        assertNull(review.read("batch").items().get(0).rationale());
        decision.setOutcome(PanelOutcomeKind.ACCEPTED);
        var other = new PanelCandidate();
        other.setQuiz(one.getQuiz());
        other.setRationale("Conflicting explanation.");
        var another = new PanelDecision();
        another.setOutcome(PanelOutcomeKind.ACCEPTED);
        another.setQuizIds(List.of("quiz"));
        another.setTaskIds(List.of());
        another.setAccepted(other);
        batch.setPanelDecisions(List.of(decision, another));
        assertNull(review.read("batch").items().get(0).rationale());
    }

    @Test void legacyCachedProjectionIsRebuiltToExposeProvenance() {
        var saved = review.read("batch");
        var item = saved.items().get(0);
        var legacy = new BatchReview(saved.batchId(), saved.correctionRevision(), saved.contentVersion(),
                saved.report(), saved.tasks(), saved.proposals(), List.of(new BatchReview.Item(item.changeId(),
                item.step(), item.before(), item.after(), item.origin(), item.ruleId(), item.status(), null, null)),
                saved.analyses(), saved.history());
        var cache = mock(BatchReviewCache.class);
        when(cache.version()).thenReturn(Optional.of("v1"));
        when(cache.load("batch", "v1")).thenReturn(Optional.of(legacy));
        var result = cachedReview(cache).read("batch");
        assertNotSame(legacy, result);
        assertNotNull(result.items().get(0).taskIds());
        verify(cache).save("batch", "v1", result);
    }

    @Test void wholeExerciseIsOneAtomicDecisionAndAnUndoIsAnotherEvent() {
        var all = List.of(selection("one", step), selection("two", second));
        assertTrue(review.decide(request(all, BatchReviewDecision.Status.APPROVED)).items().stream()
                .allMatch(item -> item.status() == BatchReviewDecision.Status.APPROVED));
        review.decide(request(all, BatchReviewDecision.Status.PENDING));
        assertEquals(2, decisions.size());
        assertTrue(review.read("batch").items().stream().allMatch(item -> item.status() == BatchReviewDecision.Status.PENDING));
    }

    @Test void refusesEntireSelectionIfOneStepChangedOrWasRemoved() {
        var selection = List.of(selection("one", step),
                new BatchReviewDecision.Selection("missing", step));
        assertThrows(IllegalArgumentException.class, () -> review.decide(request(selection, BatchReviewDecision.Status.APPROVED)));
        verify(store, never()).saveReview(any());
        var wrongSnapshot = new BatchChangeStep("quiz", List.of(com.learney.contentaudit.auditdomain.correction.ExercisePart.TRANSLATION), null);
        assertThrows(IllegalArgumentException.class, () -> review.decide(request(List.of(
                new BatchReviewDecision.Selection("one", wrongSnapshot)), BatchReviewDecision.Status.APPROVED)));
    }

    @Test void changedContentDoesNotInheritAnOldApprovalButRetainsItsHistory() {
        review.decide(request(List.of(selection("one", step)), BatchReviewDecision.Status.APPROVED));
        batch.setChanges(List.of(change("one", new BatchChangeStep("quiz", List.of(
                com.learney.contentaudit.auditdomain.correction.ExercisePart.TRANSLATION), null))));
        assertEquals(BatchReviewDecision.Status.PENDING, review.read("batch").items().get(0).status());
        assertEquals(1, decisions.size());
    }

    @Test void publishedBatchIsHistoricalAndCannotReceiveNewDecisions() {
        batch.setStatus(BatchStatus.PUBLISHED);
        assertThrows(IllegalArgumentException.class, () -> review.decide(request(List.of(
                selection("one", step)), BatchReviewDecision.Status.APPROVED)));
    }

    @Test void changingAnEarlierStepInvalidatesTheApprovalOfItsLaterDiff() {
        var selected = selection("two", second);
        review.decide(request(List.of(selected), BatchReviewDecision.Status.APPROVED));
        batch.getChanges().get(0).setDroppedReason("removed before the approved step");
        assertEquals(BatchReviewDecision.Status.PENDING, review.read("batch").items().get(0).status());
        assertThrows(IllegalArgumentException.class,
                () -> review.decide(request(List.of(selected), BatchReviewDecision.Status.APPROVED)));
        assertEquals(1, decisions.size());
    }

    @Test void aManualMiniTheoryDraftNeedsNewReviewWithoutErasingEarlierApprovals() {
        review.decide(request(List.of(selection("one", step), selection("two", second)),
                BatchReviewDecision.Status.APPROVED));
        var changed = snapshot("after", "You va con are en presente simple");
        var human = new BatchChange("mini-human", BatchChangeOrigin.JOSE, "human-mini-theory", "exercise",
                List.of(), List.of(new BatchChangeStep("quiz", List.of(ExercisePart.MINI_THEORY), changed)),
                null, false, Instant.now(), null);
        batch.setChanges(new ArrayList<>(batch.getChanges()));
        batch.getChanges().add(human);
        batch.setContentVersion(batch.getContentVersion() + 1);
        var result = review.read("batch");
        assertEquals(List.of(BatchReviewDecision.Status.APPROVED, BatchReviewDecision.Status.APPROVED,
                BatchReviewDecision.Status.PENDING), result.items().stream().map(BatchReview.Item::status).toList());
        assertEquals("new theory", result.items().get(2).before().getQuiz().getMiniTheory());
        assertEquals("You va con are en presente simple", result.items().get(2).after().getQuiz().getMiniTheory());
        assertEquals(1, decisions.size());
    }

    @Test void integratedHistoryRemainsReadableWithoutAnOpenCorrection() {
        batch.setStatus(BatchStatus.PUBLISHED);
        when(store.latestPublished()).thenReturn(Optional.of(batch));
        assertEquals("batch", review.read(null).batchId());
    }

    @Test void readsExactlyTheCheckpointWithTheLedgerCompositionNotAnAheadOfCheckpointArtifact() {
        var result = review.read("batch");
        assertEquals("after", result.proposals().get(0).getElementAfter().getQuiz().getTranslation());
        assertEquals("new theory", result.proposals().get(0).getElementAfter().getQuiz().getMiniTheory());
        assertEquals("old theory", result.items().get(0).after().getQuiz().getMiniTheory(), "only the declared translation changes");
        assertEquals("after", result.items().get(1).before().getQuiz().getTranslation(), "the second diff starts after the first");
        verify(artifacts, never()).save(any());
        verify(store, never()).save(any());
    }

    @Test void laterDroppingOrRecomposingDoesNotRewriteTheSavedMeasurement() {
        var closing = new BatchClosing();
        review.snapshot(batch, closing);
        batch.getChanges().get(0).setDroppedReason("later decision");
        assertNull(closing.getChanges().get(0).getDroppedReason());
        assertEquals("after", closing.getProposals().get(0).getElementAfter().getQuiz().getTranslation());
    }

    @Test void historyIncludesOnlySameLevelAndFixedBaseWithoutChangingActiveBatch() {
        batch.setLevel(com.learney.contentaudit.auditdomain.CefrLevel.A1);
        batch.setBaselineAuditId("base");
        batch.setOpenedAt(Instant.parse("2026-10-08T00:00:00Z"));
        var first = new CorrectionBatch();
        first.setId("first");
        first.setLevel(batch.getLevel());
        first.setBaselineAuditId("base");
        first.setStatus(BatchStatus.PUBLISHED);
        first.setOpenedAt(Instant.parse("2026-10-07T00:00:00Z"));
        var unrelated = new CorrectionBatch();
        unrelated.setId("other");
        unrelated.setLevel(batch.getLevel());
        unrelated.setBaselineAuditId("another-base");
        when(store.list()).thenReturn(List.of(batch, unrelated, first));
        assertEquals(List.of("first", "batch"), review.read("batch").history().stream()
                .map(BatchReview.Version::batchId).toList());
        verify(store, never()).save(any());
    }

    private static CourseElementSnapshot snapshot(String translation, String theory) {
        var quiz = new QuizTemplateEntity();
        quiz.setId("quiz");
        quiz.setTranslation(translation);
        quiz.setMiniTheory(theory);
        return new CourseElementSnapshot(AuditTarget.QUIZ, "quiz", quiz, null);
    }

    private DefaultBatchReview cachedReview(BatchReviewCache cache) {
        return new DefaultBatchReview(store, artifacts, plans, new DefaultBatchReportBuilder(), cache);
    }

    private BatchReviewDecision request(List<BatchReviewDecision.Selection> selections, BatchReviewDecision.Status status) {
        return new BatchReviewDecision("batch", selections, status, "José", null);
    }

    private BatchReviewDecision.Selection selection(String id, BatchChangeStep step) {
        var item = review.read("batch").items().stream().filter(i -> i.changeId().equals(id)).findFirst().orElseThrow();
        return new BatchReviewDecision.Selection(id, step, item.before(), item.after());
    }
    private static BatchChange change(String id, BatchChangeStep step) {
        return new BatchChange(id, BatchChangeOrigin.PANEL, null, null, List.of(), List.of(step), null, false, Instant.now(), null);
    }
}
