package com.learney.contentaudit.auditcli.commands;
import static org.junit.jupiter.api.Assertions.*;
import com.learney.contentaudit.revisiondomain.minitheorycorrection.MiniTheoryCorrection.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class MiniTheoryCorrectionCmdTest {
    @Test void forwardsExactRequestScopeAndBudgetToDedicatedServiceOnly() {
        var received = new AtomicReference<Request>();
        var command = new CommandLine(new MiniTheoryCorrectionCmd(() -> request -> {
            received.set(request); return new State("batch", "topic", 5, 1, List.of(), List.of(), List.of(), List.of(), List.of(), null);
        }));
        var output = new StringWriter(); command.setOut(new PrintWriter(output));
        assertEquals(0, command.execute("--request-json", "{\"action\":\"analyze\",\"operationId\":\"op\",\"batchId\":\"batch\",\"knowledgeId\":\"topic\",\"quizIds\":[\"quiz\"],\"draftIds\":[\"draft\"],\"cap\":2,\"fingerprint\":\"context\",\"contentVersion\":5}", "-f", "json"));
        assertEquals(2, received.get().cap()); assertEquals(List.of("draft"), received.get().draftIds());
        assertTrue(output.toString().contains("\"correctionRevision\":1"));
    }
    @Test void invalidJsonOrFormatDoesNotReachService() {
        var command = new CommandLine(new MiniTheoryCorrectionCmd(() -> { fail("Must not construct a service"); return null; }));
        command.setErr(new PrintWriter(new StringWriter()));
        assertEquals(2, command.execute("--request-json", "{}", "-f", "text"));
        assertEquals(2, command.execute("--request-json", "bad JSON", "-f", "json"));
        // JSON is validated before the lazily constructed service.
    }
    @Test void savedAnalysisCanBeReturnedWithCyclesAndOptionalDiagnoses() {
        var root = new com.learney.contentaudit.auditdomain.AuditNode(null, com.learney.contentaudit.auditdomain.AuditTarget.COURSE,
                null, List.of(), Map.of(), Map.of(), new com.learney.contentaudit.auditdomain.DefaultCourseDiagnoses());
        var data = new Workspace(List.of(), List.of(), List.of(new Job("op", "analyze", "topic", List.of("quiz"), List.of(),
                "DONE", 1, 1, 1, "context", null, new com.learney.contentaudit.auditdomain.AuditReport(root), java.time.Instant.now())));
        assertTrue(MiniTheoryCorrectionCmd.write(data).contains("CourseDiagnoses"));
    }
}
