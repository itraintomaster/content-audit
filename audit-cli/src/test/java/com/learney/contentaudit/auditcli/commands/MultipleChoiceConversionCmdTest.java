package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.revisiondomain.conversion.*;
import java.io.*;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MultipleChoiceConversionCmdTest {
    @Test void parsesAJsonRequestWithoutInventingBudgetAndSerializesTheLocalResult() {
        var service = mock(MultipleChoiceConversionService.class);
        var request = new MultipleChoiceConversion.Request("read", "batch", null, null, null);
        when(service.execute(request)).thenReturn(new MultipleChoiceConversion.State("batch", 2, List.of()));
        var bytes = new StringWriter(); var command = new CommandLine(new MultipleChoiceConversionCmd(() -> service));
        command.setOut(new PrintWriter(bytes));
        assertEquals(0, command.execute("--request-json", CommandJson.writeCompact(request), "--format", "json"));
        assertEquals(2, CommandJson.read(bytes.toString(), MultipleChoiceConversion.State.class).contentVersion());
        verify(service).execute(request);
    }
    @Test void earlyHelpAndSyntaxErrorsNeverBootstrapTheModelOrCourse() {
        var out = new ByteArrayOutputStream(); var err = new ByteArrayOutputStream();
        assertEquals(0, SavedMultipleChoiceConversionCmd.execute(List.of("batch", "conversion", "--help"),
                Path.of("missing-directory"), new PrintStream(out), new PrintStream(err)));
        assertTrue(out.toString().replaceAll("\\s+", " ").contains("No consulta modelos"));
        assertEquals(2, SavedMultipleChoiceConversionCmd.execute(List.of("batch", "conversion", "--request-json", "{}", "--unknown"),
                Path.of("missing-directory"), new PrintStream(out), new PrintStream(err)));
        assertTrue(err.toString().contains("Unknown option"));
    }
}
