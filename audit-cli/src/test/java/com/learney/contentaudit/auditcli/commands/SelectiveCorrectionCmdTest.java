package com.learney.contentaudit.auditcli.commands;

import static org.junit.jupiter.api.Assertions.*;
import com.learney.contentaudit.revisiondomain.batch.SelectiveCorrection.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class SelectiveCorrectionCmdTest {
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
    @Test void invalidRequestNeverConstructsAService() {
        var command = new CommandLine(new SelectiveCorrectionCmd(() -> { fail("Unexpected service"); return null; }));
        command.setErr(new PrintWriter(new StringWriter()));
        assertEquals(2, command.execute("--request-json", "bad JSON"));
        assertEquals(2, command.execute("--request-json", "{}", "-f", "text"));
    }
}
