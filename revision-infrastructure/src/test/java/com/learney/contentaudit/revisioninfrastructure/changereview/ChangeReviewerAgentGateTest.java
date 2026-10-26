package com.learney.contentaudit.revisioninfrastructure.changereview;

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

/** FEAT-MARCA: the reviewer agent's local gate lets through only what the Java reader reads. */
class ChangeReviewerAgentGateTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String BEFORE = "Ejercicio: When ___ (she / be) free?";
    private static final String AFTER = "Ejercicio: When ___ (she / be) free?\nTraducción: ¿Cuándo está libre?";
    private static final String INHERITED = "{\"quizId\":\"q\",\"mark\":\"DOUBTFUL\",\"problems\":[{\"where\":\"EXERCISE\","
            + "\"fragment\":\"When ___ (she / be) free?\",\"what\":\"Pregunta con When.\",\"inherited\":true}],\"reason\":\"r\"}";

    @Test
    @DisplayName("should let through a problem that says whether production had it, and the Java reader should read the same answer")
    @Tag("FEAT-MARCA")
    @Tag("F-MARCA-R010")
    void shouldLetThroughAProblemThatSaysWhetherProductionHadIt(@TempDir Path run) throws Exception {
        for (String verdict : new String[]{INHERITED, INHERITED.replace("true", "false")}) {
            Files.writeString(run.resolve("verdicts.json"), verdict);
            assertEquals("passed", invoke(run, "gate").get("route"), verdict);
            String artifact = (String) invoke(run, "emit").get("changeReviewVerdict");
            assertEquals(verdict.contains("true"), new JacksonChangeReviewVerdictReader().read(artifact).problems().get(0).inherited());
        }
    }

    @Test
    @DisplayName("should not let through a problem that does not say whether production had it, or says it as text")
    @Tag("FEAT-MARCA")
    @Tag("F-MARCA-R010")
    void shouldNotLetThroughAProblemWithoutInheritedAsTrueOrFalse(@TempDir Path run) throws Exception {
        for (String verdict : new String[]{INHERITED.replace(",\"inherited\":true", ""),
                INHERITED.replace("true", "\"true\""), INHERITED.replace("true", "null")}) {
            Files.writeString(run.resolve("verdicts.json"), verdict);
            assertEquals("failed", invoke(run, "gate").get("route"), verdict);
            assertFalse(Files.exists(run.resolve("validated-verdict.json")));
        }
    }

    private static Map<?, ?> invoke(Path run, String mode) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(root.resolve(".sentinel/agents/change-reviewer"))) root = root.getParent();
        Path script = root.resolve(".sentinel/agents/change-reviewer/scripts/" + (mode.equals("gate") ? "gate_verdict.sh" : "emit.sh"));
        var process = new ProcessBuilder("bash", script.toString()).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        try (var input = process.getOutputStream()) {
            input.write(JSON.writeValueAsBytes(Map.of("inputs", Map.of("run_dir", run.toString(), "quizId", "q",
                    "before", BEFORE, "after", AFTER))));
        }
        assertTrue(process.waitFor(10, TimeUnit.SECONDS), "local gate timed out");
        assertEquals(0, process.exitValue());
        return JSON.readValue(process.getInputStream(), Map.class);
    }
}
