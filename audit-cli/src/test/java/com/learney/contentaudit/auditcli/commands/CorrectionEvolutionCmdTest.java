package com.learney.contentaudit.auditcli.commands;

import static org.junit.jupiter.api.Assertions.*;
import com.learney.contentaudit.revisiondomain.batch.CorrectionEvolution;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class CorrectionEvolutionCmdTest {
    @Test void helpAndInvalidJsonDoNotCreateTheService() {
        var calls = new AtomicInteger();
        var command = new CommandLine(new CorrectionEvolutionCmd(() -> {
            calls.incrementAndGet(); return request -> null;
        }));
        var output = new StringWriter(); command.setOut(new PrintWriter(output));
        command.setErr(new PrintWriter(new StringWriter()));
        assertEquals(0, command.execute("--help"));
        assertTrue(output.toString().contains("sin publicar"));
        assertEquals(2, command.execute("--request-json", "no es JSON"));
        assertEquals(2, command.execute("--request-json", "{}", "-f", "text"));
        assertEquals(0, calls.get());
    }

    @Test void routesCreationWithAllContentAndAnalysisGuardsAndNoBudgetOrPublication() {
        var received = new AtomicReference<CorrectionEvolution.Request>();
        var command = new CommandLine(new CorrectionEvolutionCmd(() -> request -> {
            received.set(request); return null;
        }));
        command.setOut(new PrintWriter(new StringWriter()));
        assertEquals(0, command.execute("--request-json", """
                {"action":"create","batchId":"c1","operationId":"next-c2","expectedVersion":10,
                 "fingerprint":"full-content","analysisRevision":4,"reviewer":"Jose"}
                """));
        var request = received.get();
        assertEquals("create", request.action()); assertEquals("c1", request.batchId());
        assertEquals("next-c2", request.operationId()); assertEquals(10, request.expectedVersion());
        assertEquals("full-content", request.fingerprint()); assertEquals(4, request.analysisRevision());
        assertEquals("Jose", request.reviewer());
    }

    @Test void staleCreationReportsAnActionableError() {
        var command = new CommandLine(new CorrectionEvolutionCmd(() -> request -> {
            throw new IllegalStateException("La corrección o su análisis cambió: volvé a preparar.");
        }));
        var output = new StringWriter(); command.setErr(new PrintWriter(output));
        assertEquals(2, command.execute("--request-json", "{\"action\":\"create\",\"batchId\":\"c1\"}"));
        assertTrue(output.toString().contains("volvé a preparar"));
    }
}
