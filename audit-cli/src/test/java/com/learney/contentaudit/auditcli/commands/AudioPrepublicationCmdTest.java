package com.learney.contentaudit.auditcli.commands;

import static org.junit.jupiter.api.Assertions.*;
import com.learney.contentaudit.revisiondomain.batch.AudioPrepublicationService;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class AudioPrepublicationCmdTest {
    @Test void helpInvalidJsonAndWrongFormatDoNotConstructTheService() {
        var calls = new AtomicInteger();
        var command = new CommandLine(new AudioPrepublicationCmd(() -> {
            calls.incrementAndGet(); return request -> null;
        }));
        var output = new StringWriter(); command.setOut(new PrintWriter(output));
        command.setErr(new PrintWriter(new StringWriter()));
        assertEquals(0,command.execute("--help"));
        assertTrue(output.toString().contains("sin generar"));
        assertEquals(2,command.execute("--request-json","not json"));
        assertEquals(2,command.execute("--request-json","{}","-f","text"));
        assertEquals(0,calls.get());
    }
    @Test void readsTheSelectedCorrectionAndReportsUnavailableInspection() {
        var received = new AtomicReference<AudioPrepublicationService.Request>();
        var command = new CommandLine(new AudioPrepublicationCmd(() -> request -> {
            received.set(request); throw new IllegalStateException("Falta la inspección del registro de audios.");
        }));
        var errors = new StringWriter(); command.setErr(new PrintWriter(errors));
        assertEquals(2,command.execute("--request-json","{\"action\":\"read\",\"batchId\":\"c2\"}"));
        assertEquals(new AudioPrepublicationService.Request("read","c2"),received.get());
        assertTrue(errors.toString().contains("Falta la inspección"));
    }
}
