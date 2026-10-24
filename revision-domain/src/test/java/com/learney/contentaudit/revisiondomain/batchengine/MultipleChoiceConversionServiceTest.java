package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.refinerdomain.RefinementPlanStore;
import com.learney.contentaudit.revisiondomain.*;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.comparison.ChangeOutcome;
import com.learney.contentaudit.revisiondomain.conversion.MultipleChoiceConversion;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@org.junit.jupiter.api.Tag("F-CONVERSION-R002")
@org.junit.jupiter.api.Tag("F-CONVERSION-R003")
class MultipleChoiceConversionServiceTest {
    final BatchStore batches = mock(BatchStore.class);
    final RefinementPlanStore plans = mock(RefinementPlanStore.class);
    final CourseRepository courses = mock(CourseRepository.class);
    final BatchChangeLedger ledger = mock(BatchChangeLedger.class);
    final CorrectionBatch batch = new CorrectionBatch();
    final CourseEntity course = new CourseEntity();
    final Path path = Path.of("fake-course");
    final DefaultMultipleChoiceConversionService service = new DefaultMultipleChoiceConversionService(batches, plans, courses, path, ledger);

    MultipleChoiceConversionServiceTest() {
        var q = ExistingChoiceConversionTest.quiz("(your / yours)", "yours");
        var place = ExistingChoiceConversionTest.place(q, List.of());
        place.topic().setKnowledges(List.of(place.knowledge()));
        place.milestone().setTopics(List.of(place.topic()));
        var root = new RootNodeEntity(); root.setMilestones(List.of(place.milestone())); course.setRoot(root);
        batch.setId("batch"); batch.setPlanId("plan"); batch.setStatus(BatchStatus.OPEN); batch.setLevel(CefrLevel.A1); batch.setContentVersion(3);
        when(batches.load("batch")).thenReturn(Optional.of(batch)); when(batches.list()).thenReturn(List.of(batch));
        doAnswer(i -> ((java.util.function.Supplier<?>) i.getArgument(1)).get()).when(batches).withAnalysisLock(anyString(), any());
        when(courses.load(path)).thenReturn(course); when(ledger.composedCourse(batch, course)).thenReturn(course);
        when(plans.load("plan")).thenReturn(Optional.empty());
    }
    MultipleChoiceConversion.Request request(String action, int version) {
        return new MultipleChoiceConversion.Request(action, "batch", "q", version, "reviewer");
    }
    @Test void readingDoesNotWriteAndSuggestingRecordsOneAtomicCorrectionWithoutWritingTheCourse() {
        var state = service.execute(request("read", 3));
        assertEquals(1, state.candidates().size()); assertEquals(1, state.candidates().get(0).topicTotal());
        verify(batches, never()).save(any()); verify(ledger, never()).record(any(), any(), any());
        service.execute(request("suggest", 3));
        assertEquals(4, batch.getContentVersion()); assertEquals(BatchStatus.OPEN, batch.getStatus());
        var capture = org.mockito.ArgumentCaptor.forClass(BatchChange.class);
        verify(ledger).record(eq(batch), capture.capture(), eq(course));
        assertEquals(1, capture.getValue().getSteps().size());
        assertEquals(MultipleChoiceConversion.RULE + ":EXERCISE", capture.getValue().getRuleId());
        assertNotNull(capture.getValue().getSteps().get(0).getAfter().getQuiz().getClozeOriginal());
        verify(courses, never()).save(any(), any());
    }
    @Test void stalePublishedBusyAndRejectedConversionsNeverRecord() {
        assertThrows(IllegalArgumentException.class, () -> service.execute(request("suggest", 2)));
        batch.setStatus(BatchStatus.RUNNING);
        assertThrows(IllegalArgumentException.class, () -> service.execute(request("suggest", 3)));
        batch.setStatus(BatchStatus.PUBLISHED);
        assertThrows(IllegalArgumentException.class, () -> service.execute(request("suggest", 3)));
        batch.setStatus(BatchStatus.OPEN);
        var q = BatchBook.place(course, "q").orElseThrow().quiz();
        var snapshot = new CourseElementSnapshot(AuditTarget.QUIZ, "q", q, null);
        var step = new BatchChangeStep("q", List.of(), snapshot);
        when(batches.reviews("batch")).thenReturn(List.of(new BatchReviewDecision("batch", List.of(
                new BatchReviewDecision.Selection(MultipleChoiceConversion.RULE + "-q", step)),
                BatchReviewDecision.Status.REJECTED, "reviewer", Instant.now())));
        assertTrue(service.execute(request("read", 3)).candidates().get(0).reason().contains("rechazada"));
        assertThrows(IllegalArgumentException.class, () -> service.execute(request("suggest", 3)));
        verify(ledger, never()).record(any(), any(), any());
    }
    @Test void restorationIsAnotherCorrectionAndPreservesOriginalWhileRefusingLaterMetadataChanges() {
        var place = BatchBook.place(course, "q").orElseThrow();
        var converted = ExistingChoiceConversion.inspect(place, false).after();
        place.knowledge().setQuizTemplates(List.of(converted));
        service.execute(request("restore", 3));
        var capture = org.mockito.ArgumentCaptor.forClass(BatchChange.class);
        verify(ledger).record(eq(batch), capture.capture(), eq(course));
        var restored = capture.getValue().getSteps().get(0).getAfter().getQuiz();
        assertEquals("CLOZE", restored.getKind());
        assertEquals(converted.getClozeOriginal().getForm(), restored.getForm());
        assertEquals(converted.getClozeOriginal(), restored.getClozeOriginal());
        converted.setTranslation("A newer translation");
        assertThrows(IllegalArgumentException.class, () -> service.execute(request("restore", 4)));
    }
    @Test void conversionApprovalNeedsBothCompleteJudgesOnExactCurrentContent() {
        var place = BatchBook.place(course, "q").orElseThrow();
        var candidate = ExistingChoiceConversion.inspect(place, false);
        var before = new CourseElementSnapshot(AuditTarget.QUIZ, "q", candidate.before(), null);
        var after = new CourseElementSnapshot(AuditTarget.QUIZ, "q", candidate.after(), null);
        var item = new BatchReview.Item("conversion", new BatchChangeStep("q", List.of(), after), before, after,
                "CONVERSION", MultipleChoiceConversion.RULE + ":EXERCISE", BatchReviewDecision.Status.PENDING, null, null);
        assertEquals(MultipleChoiceConversion.JUDGES, item.requiredJudges());
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
        var serialized = mapper.convertValue(item, Map.class);
        assertEquals(MultipleChoiceConversion.JUDGES, serialized.get("requiredJudges"));
        assertEquals(item, mapper.convertValue(serialized, BatchReview.Item.class));
        assertThrows(IllegalArgumentException.class, () -> ConversionJudgments.requireApproved(batch, item));
        var proposal = new RevisionProposal(); proposal.setNodeId("q"); proposal.setElementBefore(before); proposal.setElementAfter(after);
        var outcome = new ChangeOutcome(); outcome.setNodeId("q"); outcome.setAfterFindings(List.of());
        outcome.setEvaluatedAnalyzers(MultipleChoiceConversion.JUDGES);
        var results = MultipleChoiceConversion.JUDGES.stream().map(j -> new ContextOpinion.Result(j, outcome)).toList();
        batch.setOpinionRuns(List.of(new ContextOpinion.Run("run", null, 0, 3, "fingerprint", 0, 0, "DONE",
                Instant.now(), Instant.now(), List.of(), List.of(proposal), results, null)));
        assertDoesNotThrow(() -> ConversionJudgments.requireApproved(batch, item));
        outcome.setAfterFindings(null);
        assertThrows(IllegalArgumentException.class, () -> ConversionJudgments.requireApproved(batch, item));
        outcome.setAfterFindings(List.of()); batch.setContentVersion(4);
        assertThrows(IllegalArgumentException.class, () -> ConversionJudgments.requireApproved(batch, item));
    }
}
