package com.learney.contentaudit.auditcli.commands;
import static org.junit.jupiter.api.Assertions.*;
import com.learney.contentaudit.revisiondomain.minitheorycorrection.MiniTheoryCorrection.*;
import com.learney.contentaudit.revisiondomain.batch.BatchService;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class MiniTheoryCorrectionCmdTest {
    @Test void routesEveryDashboardActionThroughTheRealBatchParentWithoutRunningTheBatch() {
        for (String action : List.of("read", "feedback", "save", "estimate", "rewrite", "analyze", "commit")) {
            var received = new AtomicReference<Request>();
            var batchService = (BatchService) java.lang.reflect.Proxy.newProxyInstance(
                    BatchService.class.getClassLoader(), new Class<?>[]{BatchService.class},
                    (proxy, method, args) -> { throw new AssertionError("Must not call batch " + method.getName()); });
            var command = new CommandLine(CommandLine.Model.CommandSpec.create().name("content-audit"))
                    .addSubcommand("batch", Main.batchCommandLine(batchService,
                    () -> request -> {
                        received.set(request);
                        return new State("batch", "topic", 5, 1, List.of(), List.of(), List.of(), List.of(), List.of(), null);
                    }));
            var output = new StringWriter();
            var error = new StringWriter();
            command.setOut(new PrintWriter(output));
            command.setErr(new PrintWriter(error));
            String request = """
                    {"action":"%s","operationId":"op","batchId":"batch","knowledgeId":"topic",
                    "quizIds":["quiz"],"draftIds":["draft"],"cap":2,"fingerprint":"context","contentVersion":5}
                    """.formatted(action);

            assertEquals(0, command.execute("batch", "mini-theory", "--request-json", request, "-f", "json"),
                    action + ": " + error);
            assertEquals(action, received.get().action());
            assertEquals(2, received.get().cap());
            assertEquals(List.of("quiz"), received.get().quizIds());
            assertEquals(List.of("draft"), received.get().draftIds());
            assertTrue(output.toString().contains("\"correctionRevision\":1"));
        }
    }

    @Test void bareBatchAndInvalidNestedRequestsDoNotReachEitherService() {
        var batchService = (BatchService) java.lang.reflect.Proxy.newProxyInstance(
                BatchService.class.getClassLoader(), new Class<?>[]{BatchService.class},
                (proxy, method, args) -> { throw new AssertionError("Must not call batch " + method.getName()); });
        for (String[] args : List.of(new String[]{"batch"},
                new String[]{"batch", "mini-theory"},
                new String[]{"batch", "mini-theory", "--request-json", "bad JSON", "-f", "json"},
                new String[]{"batch", "mini-theory", "--request-json", "{}", "-f", "text"})) {
            var command = new CommandLine(CommandLine.Model.CommandSpec.create().name("content-audit"))
                    .addSubcommand("batch", Main.batchCommandLine(batchService,
                    () -> { fail("Must not construct the minitheory service"); return null; }));
            command.setErr(new PrintWriter(new StringWriter()));
            assertEquals(2, command.execute(args));
        }
    }

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
