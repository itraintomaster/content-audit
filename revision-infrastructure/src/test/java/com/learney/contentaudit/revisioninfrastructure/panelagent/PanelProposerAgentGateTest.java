package com.learney.contentaudit.revisioninfrastructure.panelagent;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** F-MESA-R033: the proposer's local gate asks for the card alone when the panel asked for it. */
class PanelProposerAgentGateTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String WHOLE = "{\"kind\":\"WHOLE_EXERCISE\",\"sentenceParts\":[],\"options\":[],"
            + "\"translation\":null,\"instructions\":null,\"miniTheory\":\"Con $they$ se usa $are$.\",\"rationale\":\"r\"}";
    private static final String CARD = "{\"kind\":\"MINI_THEORY\",\"miniTheory\":\"Con $they$ se usa $are$.\",\"rationale\":\"r\"}";

    @Test
    @DisplayName("should send the proposal back when the panel asked for the card alone and it did not come")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R033")
    void shouldSendTheProposalBackWhenTheCardAloneDidNotCome(@TempDir Path run) throws Exception {
        for (String candidates : new String[]{"[" + WHOLE + "]", "[" + WHOLE + "," + CARD.replace("Con $they$ se usa $are$.", " ") + "]"}) {
            Files.writeString(run.resolve("proposal.json"), "{\"candidates\":" + candidates + "}");
            assertEquals("failed", gate(run, "true").get("route"), candidates);
            assertFalse(Files.exists(run.resolve("validated-proposal.json")));
        }
    }

    @Test
    @DisplayName("should let the proposal through with the card alone, or without it when the panel did not ask for it")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R033")
    void shouldLetTheProposalThroughWithTheCardAloneOrWhenNotAsked(@TempDir Path run) throws Exception {
        Files.writeString(run.resolve("proposal.json"), "{\"candidates\":[" + WHOLE + "," + CARD + "]}");
        assertEquals("passed", gate(run, "true").get("route"));
        Files.writeString(run.resolve("proposal.json"), "{\"candidates\":[" + WHOLE + "]}");
        assertEquals("passed", gate(run, "false").get("route"));
    }

    private static Map<?, ?> gate(Path run, String cardApart) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(root.resolve(".sentinel/agents/panel-proposer"))) root = root.getParent();
        Path script = root.resolve(".sentinel/agents/panel-proposer/scripts/gate.sh");
        var process = new ProcessBuilder("bash", script.toString()).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try (var input = process.getOutputStream()) {
            input.write(JSON.writeValueAsBytes(Map.of("inputs", Map.of("run_dir", run.toString(), "cardApart", cardApart))));
        }
        assertTrue(process.waitFor(10, TimeUnit.SECONDS), "local gate timed out");
        assertEquals(0, process.exitValue());
        return JSON.readValue(process.getInputStream(), Map.class);
    }
}
