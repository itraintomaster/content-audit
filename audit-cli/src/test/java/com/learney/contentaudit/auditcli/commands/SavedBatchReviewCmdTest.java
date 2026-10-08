package com.learney.contentaudit.auditcli.commands;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.learney.contentaudit.auditinfrastructure.batchstore.FileSystemBatchStore;
import com.learney.contentaudit.revisiondomain.batch.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

    private Integer execute(List<String> args) {
        return SavedBatchReviewCmd.execute(args, service, output, error);
    }

    private static BatchReview review() {
        return new BatchReview("batch", 1, 2, null, List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
