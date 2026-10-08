package com.learney.contentaudit.revisiondomain.batchengine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.learney.contentaudit.refinerdomain.RefinementPlanStore;
import com.learney.contentaudit.revisiondomain.RevisionArtifactStore;
import com.learney.contentaudit.revisiondomain.RevisionArtifact;
import com.learney.contentaudit.revisiondomain.RevisionProposal;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.revisiondomain.batch.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultBatchReviewTest {
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

    @Test void savesOneDecisionDuringRunWithoutWritingBatchOrApplyingCourse() {
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
