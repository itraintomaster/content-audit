package com.learney.contentaudit.auditcli.commands;

import static org.junit.jupiter.api.Assertions.*;
import com.learney.contentaudit.revisiondomain.batch.QuizCorrection;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class QuizCorrectionCmdTest {
    @Test void helpAndMalformedInputNeverConstructAService() {
        var command = new CommandLine(new QuizCorrectionCmd(() -> { fail("Unexpected service"); return null; }));
        var out = new StringWriter(); command.setOut(new PrintWriter(out));
        command.setErr(new PrintWriter(new StringWriter()));
        assertEquals(0, command.execute("--help"));
        assertTrue(out.toString().contains("--request-json"));
        assertEquals(2, command.execute("--request-json", "bad JSON"));
        assertEquals(2, command.execute("--request-json", "{}", "-f", "text"));
    }

    @Test void routesQuizSelectionStampBudgetAndExplicitApprovalChoice() {
        var received = new AtomicReference<QuizCorrection.Request>();
        var command = new CommandLine(new QuizCorrectionCmd(() -> request -> {
            received.set(request); return null;
        }));
        command.setOut(new PrintWriter(new StringWriter()));
        assertEquals(0, command.execute("--request-json", """
                {"action":"correct","batchId":"same-batch","operationId":"explicit-op",
                "contentVersion":9,"fingerprint":"all-current-findings","queryCap":30,
                "quizIds":["q1","q2"],"includeApproved":false,"reviewer":"UI"}
                """));
        var request = received.get();
        assertEquals(List.of("q1", "q2"), request.quizIds());
        assertEquals(9, request.contentVersion());
        assertEquals("all-current-findings", request.fingerprint());
        assertEquals(30, request.queryCap());
        assertFalse(request.includeApproved());
    }

    @Test void leavesMissingApprovalChoiceFalseAndReportsRejections() {
        var received = new AtomicReference<QuizCorrection.Request>();
        var command = new CommandLine(new QuizCorrectionCmd(() -> request -> {
            received.set(request); throw new IllegalArgumentException("El contenido cambió: volvé a estimar.");
        }));
        var error = new StringWriter(); command.setErr(new PrintWriter(error));
        assertEquals(2, command.execute("--request-json", """
                {"action":"estimate","batchId":"same-batch","quizIds":["q1"]}
                """));
        assertFalse(received.get().includeApproved());
        assertTrue(error.toString().contains("volvé a estimar"));
    }
}
