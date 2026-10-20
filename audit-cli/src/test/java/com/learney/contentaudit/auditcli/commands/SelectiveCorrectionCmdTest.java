package com.learney.contentaudit.auditcli.commands;

import static org.junit.jupiter.api.Assertions.*;
import com.learney.contentaudit.revisiondomain.batch.SelectiveCorrection.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class SelectiveCorrectionCmdTest {
    @Test void helpNeverConstructsAServiceOrNeedsARequest() {
        var command = new CommandLine(new SelectiveCorrectionCmd(() -> { fail("Unexpected service"); return null; }));
        var output = new StringWriter(); command.setOut(new PrintWriter(output));
        assertEquals(0, command.execute("--help"));
        assertTrue(output.toString().contains("--request-json"));
    }
    @Test void routesExplicitSelectionBudgetAndSourceWithoutInvokingTheGeneralBatchRunner() {
        var received = new AtomicReference<Request>();
        var command = new CommandLine(CommandLine.Model.CommandSpec.create().name("content-audit"))
                .addSubcommand("batch", new CommandLine(CommandLine.Model.CommandSpec.create().name("batch"))
                .addSubcommand("selective", new SelectiveCorrectionCmd(() -> r -> {
                    received.set(r); return new State("batch", 1, 6, r.analyzer(), "fp", "v1", "runtime", false,
                            "exam", 2, 1, 1, 1, 0, List.of(), List.of());
                })));
        var output = new StringWriter(); command.setOut(new PrintWriter(output));
        assertEquals(0, command.execute("batch", "selective", "--request-json", """
                {"action":"correct","batchId":"batch","analyzer":"multiple-choice-quality","operationId":"op",
                "sourceRunId":"analysis","contentVersion":6,"fingerprint":"fp","judgeVersion":"v1",
                "queryCap":7,"taskIds":["selected"],"reviewer":"human","includeApproved":false}
                """, "-f", "json"));
        assertEquals(List.of("selected"), received.get().taskIds()); assertEquals(7, received.get().queryCap());
        assertEquals("analysis", received.get().sourceRunId()); assertTrue(output.toString().contains("\"correctionRevision\":1"));
    }
    @Test @org.junit.jupiter.api.Tag("FEAT-CORTE") @org.junit.jupiter.api.Tag("F-CORTE-R006")
    void reconcileRoutesTheConfirmationAndTheAnswerNamesTheOpenMeasurements() {
        var received = new AtomicReference<Request>();
        var open = new OpenRun("ui-2026-10-20T00-19-29.907241Z-batch-selective", "analyze", "naturalness", "Naturalidad",
                "RUNNING", java.time.Instant.parse("2026-10-20T00:19:29Z"), 1919, 301, false);
        var command = new CommandLine(new SelectiveCorrectionCmd(() -> r -> {
            received.set(r);
            return new State("batch", 1, 6, "naturalness", "fp", "v1", "runtime", false, "cortada", 0, 0, 0, 0, 0,
                    List.of(), List.of(), "Naturalidad", List.of(), List.of(), List.of(open));
        }));
        var output = new StringWriter(); command.setOut(new PrintWriter(output));
        assertEquals(0, command.execute("--request-json", """
                {"action":"reconcile","batchId":"batch","operationId":"ui-2026-10-20T00-19-29.907241Z-batch-selective",
                "reviewer":"tablero","force":true}
                """, "-f", "json"));
        assertEquals("reconcile", received.get().action()); assertTrue(received.get().force());
        assertTrue(output.toString().contains("\"openRuns\":[{\"id\":\"ui-2026-10-20T00-19-29.907241Z-batch-selective\""), output.toString());
        assertTrue(output.toString().contains("\"saved\":301"), output.toString());
        assertTrue(output.toString().contains("\"running\":false"), output.toString());
        assertTrue(output.toString().contains("\"createdAt\":\"2026-10-20T00:19:29Z\""), output.toString());
    }
    @Test void invalidRequestNeverConstructsAService() {
        var command = new CommandLine(new SelectiveCorrectionCmd(() -> { fail("Unexpected service"); return null; }));
        command.setErr(new PrintWriter(new StringWriter()));
        assertEquals(2, command.execute("--request-json", "bad JSON"));
        assertEquals(2, command.execute("--request-json", "{}", "-f", "text"));
    }
}
