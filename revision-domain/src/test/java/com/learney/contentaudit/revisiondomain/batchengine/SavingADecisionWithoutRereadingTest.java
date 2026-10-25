package com.learney.contentaudit.revisiondomain.batchengine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.refinerdomain.RefinementPlanStore;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.RevisionArtifact;
import com.learney.contentaudit.revisiondomain.RevisionArtifactStore;
import com.learney.contentaudit.revisiondomain.RevisionProposal;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.conversion.MultipleChoiceConversion;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * FEAT-GUARDA: saving a decision checks it against what is saved of the exercises it names and answers only what it
 * changed, without reading the whole review again. Two exercises: «quiz», with two corrections, and «other», with one.
 */
@Tag("FEAT-GUARDA")
class SavingADecisionWithoutRereadingTest {
    private final BatchStore store = mock(BatchStore.class);
    private final RevisionArtifactStore artifacts = mock(RevisionArtifactStore.class);
    private final RefinementPlanStore plans = mock(RefinementPlanStore.class);
    private final List<BatchReviewDecision> decisions = new ArrayList<>();
    private final CorrectionBatch batch = new CorrectionBatch();
    private final BatchChangeStep first = new BatchChangeStep("quiz", List.of(ExercisePart.TRANSLATION),
            snapshot("quiz", "after", "old theory"));
    private final BatchChangeStep second = new BatchChangeStep("quiz", List.of(ExercisePart.MINI_THEORY),
            snapshot("quiz", "ignored", "new theory"));
    private final BatchChangeStep third = new BatchChangeStep("other", List.of(ExercisePart.TRANSLATION),
            snapshot("other", "other after", "other theory"));
    private final DefaultBatchReview review = new DefaultBatchReview(store, artifacts, plans,
            new DefaultBatchReportBuilder());

    @BeforeEach void setup() {
        batch.setId("batch");
        batch.setPlanId("plan");
        batch.setStatus(BatchStatus.OPEN);
        batch.setContentVersion(3);
        batch.setCorrectionRevision(2);
        batch.setChanges(List.of(change("one", first), change("two", second), change("three", third)));
        batch.setQuizProposals(List.of(new BatchProposalRef("quiz", "proposal-quiz"),
                new BatchProposalRef("other", "proposal-other")));
        proposal("quiz", "proposal-quiz", snapshot("quiz", "before", "old theory"));
        proposal("other", "proposal-other", snapshot("other", "other before", "other theory"));
        when(store.load("batch")).thenReturn(Optional.of(batch));
        when(store.reviews("batch")).thenAnswer(invocation -> new ArrayList<>(decisions));
        doAnswer(invocation -> { decisions.add(invocation.getArgument(0)); return null; }).when(store).saveReview(any());
    }

    @Test
    @Tag("F-GUARDA-R001")
    @DisplayName("should save a decision reading the batch once and only the proposals of its exercise: no other batch, no plan, no composed course")
    void savingReadsTheBatchOnceAndOnlyTheProposalsOfItsExercise() {
        var selected = selection("one");
        clearInvocations(store, artifacts, plans);

        review.save(request(List.of(selected), BatchReviewDecision.Status.APPROVED));

        verify(store, times(1)).load("batch");
        verify(store, never()).list();
        verify(store, never()).open();
        verify(store, never()).hasSuccessor(any());
        verify(store, never()).analysisSelections(any());
        verifyNoInteractions(plans);
        verify(artifacts).load("plan", "proposal-quiz");
        verify(artifacts, never()).load("plan", "proposal-other");
        verify(store, never()).save(any());
        assertEquals(1, decisions.size());
    }

    @Test
    @Tag("F-GUARDA-R002")
    @DisplayName("should answer a saved decision with the batch, its version, its correction, its saved decisions and only the corrections it may have changed, as the whole review shows them")
    void aSavedDecisionAnswersOnlyWhatItMayHaveChanged() {
        var answer = review.save(request(List.of(selection("two")), BatchReviewDecision.Status.REJECTED));

        assertEquals("batch", answer.batchId());
        assertEquals(3, answer.contentVersion());
        assertEquals(2, answer.correctionRevision());
        assertEquals(BatchStatus.OPEN, answer.status());
        assertEquals(1, answer.reviewCount());
        var whole = review.read("batch").items();
        assertEquals(List.of(whole.get(1)), answer.items(), "the decided correction, exactly as the whole review shows it");
        assertEquals(BatchReviewDecision.Status.REJECTED, answer.items().get(0).status());
        assertEquals("José", answer.items().get(0).reviewer());
        assertNotNull(answer.items().get(0).decidedAt());
    }

    @Test
    @Tag("F-GUARDA-R002")
    @DisplayName("should answer a decision on a whole exercise with each of its corrections, and still answer the whole review to whoever does not ask for the short answer")
    void aWholeExerciseAnswersEachOfItsCorrectionsAndDecideStillAnswersEverything() {
        var answer = review.save(request(List.of(selection("one"), selection("two")),
                BatchReviewDecision.Status.APPROVED));
        assertEquals(List.of("one", "two"), answer.items().stream().map(BatchReview.Item::changeId).toList());
        assertTrue(answer.items().stream().allMatch(i -> i.status() == BatchReviewDecision.Status.APPROVED));

        var whole = review.decide(request(List.of(selection("three")), BatchReviewDecision.Status.APPROVED));
        assertEquals(3, whole.items().size(), "decide answers the whole review, as the Flutter app reads it");
        assertNotNull(whole.report());
    }

    @Test
    @Tag("F-GUARDA-R003")
    @DisplayName("should read the corrections of some exercises as the whole review shows them, without the plan, the other batches or the other exercises' proposals")
    void theCorrectionsOfSomeExercisesAreReadWithoutTheWholeReview() {
        review.save(request(List.of(selection("three")), BatchReviewDecision.Status.REJECTED));
        var whole = review.read("batch").items();
        clearInvocations(store, artifacts, plans);

        var slice = review.slice("batch", List.of("quiz"));

        assertEquals(whole.subList(0, 2), slice.items());
        assertEquals(1, slice.reviewCount(), "the decision on «other» counts, though its correction is not read");
        assertEquals(3, slice.contentVersion());
        verify(store, never()).list();
        verifyNoInteractions(plans);
        verify(artifacts, never()).load("plan", "proposal-other");
        assertEquals(List.of(whole.get(2)), review.slice("batch", List.of("other")).items());
    }

    @Test
    @Tag("F-GUARDA-R004")
    @DisplayName("should save the same decision with the short answer as with the whole one: same batch, selections, state, reviewer and guard")
    void theShortAnswerSavesTheSameDecision() {
        var selected = selection("one");
        var guarded = new BatchReviewDecision("batch", List.of(selected), BatchReviewDecision.Status.REJECTED,
                " Problemas:PRB-1 ", null, 0L);
        review.save(guarded);
        review.decide(new BatchReviewDecision("batch", List.of(selected), BatchReviewDecision.Status.REJECTED,
                " Problemas:PRB-1 ", null, 1L));

        var saved = ArgumentCaptor.forClass(BatchReviewDecision.class);
        verify(store, times(2)).saveReview(saved.capture());
        var shortOne = saved.getAllValues().get(0);
        var wholeOne = saved.getAllValues().get(1);
        assertEquals(wholeOne.batchId(), shortOne.batchId());
        assertEquals(wholeOne.selections(), shortOne.selections());
        assertEquals(wholeOne.status(), shortOne.status());
        assertEquals("Problemas:PRB-1", shortOne.reviewer());
        assertEquals(wholeOne.reviewer(), shortOne.reviewer());
        assertNotNull(shortOne.decidedAt());
        assertEquals(0L, shortOne.expectedReviewCount(), "the guard of annotating travels to the store");
    }

    @Test
    @Tag("F-GUARDA-R005")
    @DisplayName("should refuse, without writing, a correction that changed or was withdrawn, a selection without its step and a batch already integrated")
    void theSameChecksRefuseWithoutWriting() {
        var selected = selection("two");
        batch.getChanges().get(0).setDroppedReason("withdrawn before the decided step");
        assertThrows(IllegalArgumentException.class,
                () -> review.save(request(List.of(selected), BatchReviewDecision.Status.APPROVED)));
        assertThrows(IllegalArgumentException.class, () -> review.save(request(List.of(
                new BatchReviewDecision.Selection("missing", first)), BatchReviewDecision.Status.APPROVED)));
        assertThrows(IllegalArgumentException.class, () -> review.save(request(List.of(
                new BatchReviewDecision.Selection("one", null)), BatchReviewDecision.Status.APPROVED)));
        batch.getChanges().get(0).setDroppedReason(null);
        batch.setStatus(BatchStatus.PUBLISHED);
        assertThrows(IllegalArgumentException.class,
                () -> review.save(request(List.of(selection("one")), BatchReviewDecision.Status.APPROVED)));
        verify(store, never()).saveReview(any());
    }

    @Test
    @Tag("F-GUARDA-R005")
    @DisplayName("should refuse to approve a conversion to multiple choice without its judges' current approval, as before")
    void aConversionStillNeedsItsJudgesToBeApproved() {
        var cloze = snapshot("quiz", "before", "old theory").getQuiz();
        var converted = snapshot("quiz", "before", "old theory").getQuiz();
        converted.setKind("MULTIPLE_CHOICE");
        converted.setClozeOriginal(cloze);
        var form = new FormEntity();
        form.setKind("MULTIPLE_CHOICE");
        converted.setForm(form);
        var step = new BatchChangeStep("quiz", List.of(ExercisePart.FORM_KIND),
                new CourseElementSnapshot(AuditTarget.QUIZ, "quiz", converted, null));
        batch.setChanges(List.of(new BatchChange("conversion", BatchChangeOrigin.JOSE,
                MultipleChoiceConversion.RULE + ":EXERCISE", null, List.of(), List.of(step), null, false,
                Instant.now(), null)));
        var selected = selection("conversion");

        var refused = assertThrows(IllegalArgumentException.class,
                () -> review.save(request(List.of(selected), BatchReviewDecision.Status.APPROVED)));
        assertTrue(refused.getMessage().contains("La conversión necesita la aprobación vigente"), refused.getMessage());
        verify(store, never()).saveReview(any());
        review.save(request(List.of(selected), BatchReviewDecision.Status.REJECTED));
        assertEquals(1, decisions.size(), "rejecting it needs no judge");
    }

    private void proposal(String quiz, String id, CourseElementSnapshot before) {
        var proposal = new RevisionProposal();
        proposal.setProposalId(id);
        proposal.setNodeId(quiz);
        proposal.setElementBefore(before);
        var artifact = new RevisionArtifact();
        artifact.setProposal(proposal);
        when(artifacts.load("plan", id)).thenReturn(Optional.of(artifact));
    }

    private static CourseElementSnapshot snapshot(String id, String translation, String theory) {
        var quiz = new QuizTemplateEntity();
        quiz.setId(id);
        quiz.setTranslation(translation);
        quiz.setMiniTheory(theory);
        return new CourseElementSnapshot(AuditTarget.QUIZ, id, quiz, null);
    }

    private static BatchReviewDecision request(List<BatchReviewDecision.Selection> selections,
            BatchReviewDecision.Status status) {
        return new BatchReviewDecision("batch", selections, status, "José", null);
    }

    private BatchReviewDecision.Selection selection(String changeId) {
        var item = review.read("batch").items().stream().filter(i -> i.changeId().equals(changeId)).findFirst()
                .orElseThrow();
        return new BatchReviewDecision.Selection(changeId, item.step(), item.before(), item.after());
    }

    private static BatchChange change(String id, BatchChangeStep step) {
        return new BatchChange(id, BatchChangeOrigin.PANEL, null, null, List.of(), List.of(step), null, false,
                Instant.now(), null);
    }
}
