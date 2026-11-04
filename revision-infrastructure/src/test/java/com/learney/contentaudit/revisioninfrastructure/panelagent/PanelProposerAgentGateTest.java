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

/**
 * F-MESA-R033: the proposer's local gate asks for the card alone when the panel asked for it, and only for the card
 * when every task of the exercise may touch only its card.
 */
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

    @Test
    @DisplayName("should send the proposal back when every task touches only the card and a candidate changes another part")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R033")
    void shouldSendTheProposalBackWhenEveryTaskTouchesOnlyTheCardAndACandidateChangesAnotherPart(@TempDir Path run)
            throws Exception {
        String tasks = "[" + task("[\"MINI_THEORY\"]", null) + "," + task("[\"MINI_THEORY\"]", null) + "]";
        for (String candidates : new String[]{"[" + WHOLE + "]", "[" + CARD + "," + WHOLE + "]",
                "[" + CARD.replace("Con $they$ se usa $are$.", " ") + "]"}) {
            Files.writeString(run.resolve("proposal.json"), "{\"candidates\":" + candidates + "}");
            assertEquals("failed", gate(run, Map.of("quizId", "q1", "tasks", tasks, "conversion", "{}")).get("route"),
                    candidates);
            assertFalse(Files.exists(run.resolve("validated-proposal.json")));
        }
        Files.writeString(run.resolve("proposal.json"), "{\"candidates\":[" + CARD + "]}");
        assertEquals("passed", gate(run, Map.of("quizId", "q1", "tasks", tasks, "conversion", "{}")).get("route"));
    }

    @Test
    @DisplayName("should let the whole exercise through when a task touches another part, it is the topic card, a conversion or a repair")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R033")
    void shouldLetTheWholeExerciseThroughWhenNotEveryTaskIsOfTheCard(@TempDir Path run) throws Exception {
        Files.writeString(run.resolve("proposal.json"), "{\"candidates\":[" + WHOLE + "]}");
        String onlyCard = "[" + task("[\"MINI_THEORY\"]", null) + "]";
        String mixed = "[" + task("[\"MINI_THEORY\"]", null) + "," + task("[\"STATEMENT\",\"TRANSLATION\"]", null) + "]";
        String repair = "[" + task("[\"MINI_THEORY\"]", "EXPLICIT_DIDACTIC_REPAIR") + "]";
        String order = "{\"targetKind\":\"MULTIPLE_CHOICE\",\"optionCount\":3}";
        for (Map<String, String> inputs : java.util.List.of(
                Map.of("quizId", "q1", "tasks", mixed, "conversion", "{}"),
                Map.of("quizId", "", "tasks", onlyCard, "conversion", "{}"),
                Map.of("quizId", "q1", "tasks", onlyCard, "conversion", order),
                Map.of("quizId", "q1", "tasks", repair, "conversion", "{}"))) {
            assertEquals("passed", gate(run, inputs).get("route"), inputs.toString());
        }
    }

    private static String task(String mayTouch, String origin) {
        return "{\"taskId\":\"t\",\"nodeId\":\"q1\"" + (origin != null ? ",\"origin\":\"" + origin + "\"" : "")
                + ",\"analyzer\":\"mini-theory\",\"mayTouch\":" + mayTouch + "}";
    }

    private static Map<?, ?> gate(Path run, String cardApart) throws Exception {
        return gate(run, Map.of("cardApart", cardApart));
    }

    private static Map<?, ?> gate(Path run, Map<String, String> given) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(root.resolve(".sentinel/agents/panel-proposer"))) root = root.getParent();
        Path script = root.resolve(".sentinel/agents/panel-proposer/scripts/gate.sh");
        var process = new ProcessBuilder("bash", script.toString()).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        Map<String, String> inputs = new java.util.HashMap<>(given);
        inputs.put("run_dir", run.toString());
        try (var input = process.getOutputStream()) {
            input.write(JSON.writeValueAsBytes(Map.of("inputs", inputs)));
        }
        assertTrue(process.waitFor(10, TimeUnit.SECONDS), "local gate timed out");
        assertEquals(0, process.exitValue());
        return JSON.readValue(process.getInputStream(), Map.class);
    }
}
