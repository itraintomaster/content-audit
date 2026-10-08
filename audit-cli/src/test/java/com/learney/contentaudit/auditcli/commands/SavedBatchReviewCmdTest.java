package com.learney.contentaudit.auditcli.commands;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditinfrastructure.FileSystemRevisionArtifactStore;
import com.learney.contentaudit.auditinfrastructure.batchstore.FileSystemBatchStore;
import com.learney.contentaudit.revisiondomain.RevisionArtifact;
import com.learney.contentaudit.revisiondomain.RevisionProposal;
import com.learney.contentaudit.revisiondomain.batch.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class SavedBatchReviewCmdTest {
    private final BatchReviewService service = mock(BatchReviewService.class);
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errors = new ByteArrayOutputStream();
    private final PrintStream output = new PrintStream(bytes);
    private final PrintStream error = new PrintStream(errors);
    @TempDir Path workdir;

    @Test void firstReadAlsoNeedsOnlySavedStoresAndNeverAnAuditOrModelRuntime() {
        var batch = new CorrectionBatch();
        batch.setId("batch");
        batch.setPlanId("plan");
        batch.setStatus(BatchStatus.OPEN);
        new FileSystemBatchStore(workdir).save(batch);
        assertEquals(0, SavedBatchReviewCmd.execute(List.of("batch", "review", "-f", "json"), workdir, output, error));
        assertEquals("batch", CommandJson.read(bytes.toString(), BatchReview.class).batchId());
        assertTrue(Files.isRegularFile(workdir.resolve(".content-audit/batch-review-cache/current.json")));
        verifyNoInteractions(service);
    }

    @Test void exactReadUsesTheSharedReviewServiceWithoutModelBootstrap() {
        when(service.read(null)).thenReturn(review());
        assertEquals(0, execute(List.of("batch", "review", "-f", "json")));
        assertEquals(review(), CommandJson.read(bytes.toString(), BatchReview.class));
        verify(service).read(null);
        verify(service, never()).decide(any());
    }

    @Test void namedHistoryAndLongFormatAreSupported() {
        when(service.read("batch")).thenReturn(review());
        assertEquals(0, execute(List.of("batch", "review", "batch", "--format", "json")));
    }

    @Test void humanDecisionsUseTheSameValidationWithoutStartingAnAudit() {
        var decision = new BatchReviewDecision("batch", List.of(), BatchReviewDecision.Status.APPROVED, "José", null);
        when(service.decide(decision)).thenReturn(review());
        assertEquals(0, execute(List.of("batch", "review", "--decision-json", CommandJson.write(decision), "-f", "json")));
        verify(service).decide(decision);
        verify(service, never()).read(any());
    }

    @Test void helpAndInvalidOrUnsupportedSyntaxAlwaysGoThroughTheNormalParser() {
        for (var args : List.of(
                List.of("batch", "review", "--help"),
                List.of("batch", "review", "unknown", "extra", "-f", "json"),
                List.of("batch", "review", "../other", "-f", "json"),
                List.of("batch", "review", "--bad", "-f", "json"),
                List.of("batch", "run", "-f", "json"),
                List.of("batch", "review", "-f", "text"))) {
            assertNull(execute(args), args.toString());
        }
        verifyNoInteractions(service);
        assertEquals(0, bytes.size());
    }

    @Test void missingBatchAndStaleDecisionKeepNormalErrorFormattingAndExitStatus() {
        when(service.read(null)).thenThrow(new IllegalArgumentException("missing batch"));
        assertEquals(2, execute(List.of("batch", "review", "-f", "json")));
        assertTrue(errors.toString().contains("Error: missing batch"));
        assertEquals(2, execute(List.of("batch", "review", "--decision-json", "bad JSON", "-f", "json")));
        verify(service, never()).decide(any());
        assertEquals(0, bytes.size());
    }

    @ParameterizedTest
    @EnumSource(BatchReviewDecision.Status.class)
    void decodedUiSelectionCanBeReviewedAndReadBackWhileTheBatchIsRunning(BatchReviewDecision.Status status) {
        var batch = saveRunningBatchWithRichSnapshot();
        var selection = readUiSelection();
        String batchBeforeDecision = assertDoesNotThrow(() -> Files.readString(
                workdir.resolve(".content-audit/batches/batch.json")));

        assertEquals(0, decideFromUi(selection, status), errors.toString());
        var updated = CommandJson.read(bytes.toString(), BatchReview.class);
        assertEquals(status, updated.items().getFirst().status());
        assertEquals(selection.step(), updated.items().getFirst().step());
        assertEquals(BatchStatus.RUNNING, new FileSystemBatchStore(workdir).load(batch.getId()).orElseThrow().getStatus());
        assertEquals(batchBeforeDecision, assertDoesNotThrow(() -> Files.readString(
                workdir.resolve(".content-audit/batches/batch.json"))), "Review must not overwrite a running checkpoint");

        bytes.reset();
        assertEquals(0, SavedBatchReviewCmd.execute(List.of("batch", "review", "-f", "json"), workdir, output, error));
        assertEquals(status, CommandJson.read(bytes.toString(), BatchReview.class).items().getFirst().status());
    }

    @Test void unrelatedCheckpointAdvanceDoesNotInvalidateAnUnchangedUiSelection() {
        var batch = saveRunningBatchWithRichSnapshot();
        var selection = readUiSelection();
        batch.setContentVersion(2);
        batch.setQueriesSpent(1);
        new FileSystemBatchStore(workdir).save(batch);

        assertEquals(0, decideFromUi(selection, BatchReviewDecision.Status.APPROVED), errors.toString());
        assertEquals(2, CommandJson.read(bytes.toString(), BatchReview.class).contentVersion());
    }

    @ParameterizedTest
    @ValueSource(strings = {"before", "after", "step", "withdrawn"})
    void aRealSnapshotChangeOrWithdrawalStillRejectsTheUiSelection(String changed) {
        var batch = saveRunningBatchWithRichSnapshot();
        var selection = readUiSelection();
        switch (changed) {
            case "before" -> selection.before().getQuiz().setTranslation("Contenido distinto");
            case "after" -> selection.after().getQuiz().setMiniTheory("Contenido distinto");
            case "step" -> selection.step().setParts(List.of(ExercisePart.TRANSLATION));
            case "withdrawn" -> {
                batch.getChanges().getFirst().setDroppedReason("Retirada durante la revisión");
                new FileSystemBatchStore(workdir).save(batch);
            }
            default -> fail("Unknown case");
        }

        assertEquals(2, decideFromUi(selection, BatchReviewDecision.Status.APPROVED));
        assertTrue(errors.toString().contains("La corrección cambió o fue retirada"));
        assertTrue(new FileSystemBatchStore(workdir).reviews("batch").isEmpty());
    }

    private CorrectionBatch saveRunningBatchWithRichSnapshot() {
        var before = CommandJsonTest.snapshot("before");
        var after = CommandJsonTest.snapshot("after");
        var proposal = new RevisionProposal();
        proposal.setProposalId("proposal");
        proposal.setPlanId("plan");
        proposal.setNodeTarget(AuditTarget.QUIZ);
        proposal.setNodeId("quiz");
        proposal.setElementBefore(before);
        proposal.setElementAfter(after);
        var artifact = new RevisionArtifact();
        artifact.setProposal(proposal);
        new FileSystemRevisionArtifactStore(workdir).save(artifact);
        var change = new BatchChange();
        change.setChangeId("change");
        change.setSteps(List.of(new BatchChangeStep("quiz", List.of(ExercisePart.MINI_THEORY), after)));
        var batch = new CorrectionBatch();
        batch.setId("batch");
        batch.setPlanId("plan");
        batch.setStatus(BatchStatus.RUNNING);
        batch.setChanges(List.of(change));
        batch.setQuizProposals(List.of(new BatchProposalRef("quiz", "proposal")));
        new FileSystemBatchStore(workdir).save(batch);
        return batch;
    }

    private BatchReviewDecision.Selection readUiSelection() {
        bytes.reset();
        assertEquals(0, SavedBatchReviewCmd.execute(List.of("batch", "review", "-f", "json"), workdir, output, error));
        var item = CommandJson.read(bytes.toString(), BatchReview.class).items().getFirst();
        return new BatchReviewDecision.Selection(item.changeId(), item.step(), item.before(), item.after());
    }

    private Integer decideFromUi(BatchReviewDecision.Selection selection, BatchReviewDecision.Status status) {
        var decision = new BatchReviewDecision("batch", List.of(selection), status, "Operador UI", null);
        // Mirror the decoded JSON maps the Flutter client sends back to the CLI.
        String payload = CommandJson.writeCompact(CommandJson.read(CommandJson.writeCompact(decision), Map.class));
        bytes.reset();
        return SavedBatchReviewCmd.execute(List.of("batch", "review", "--decision-json", payload, "-f", "json"),
                workdir, output, error);
    }

    private Integer execute(List<String> args) {
        return SavedBatchReviewCmd.execute(args, service, output, error);
    }

    private static BatchReview review() {
        return new BatchReview("batch", 1, 2, null, List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
