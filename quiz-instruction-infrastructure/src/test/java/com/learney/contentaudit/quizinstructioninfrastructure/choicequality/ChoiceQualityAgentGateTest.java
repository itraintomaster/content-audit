package com.learney.contentaudit.quizinstructioninfrastructure.choicequality;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ChoiceQualityAgentGateTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PASS = "{\"status\":\"PASS\",\"objective\":\"Inversión\",\"introductory\":false,\"issues\":[],\"rationale\":\"Practica el objetivo\"}";
    private static final String FAIL = PASS.replace("\"PASS\"", "\"FAIL\"").replace("[]",
            "[{\"kind\":\"ANSWER_CUE\",\"severity\":\"HIGH\",\"labels\":[\"Is he\"],\"reason\":\"Pista\",\"misconception\":\"Inversión\"}]");

    @Test void gateAndEmitAgreeWithStrictJavaReader(@TempDir Path run) throws Exception {
        for (String verdict : new String[]{PASS, FAIL, PASS.replace("\"PASS\"", "\"NEEDS_CONTEXT\"")}) {
            Files.writeString(run.resolve("verdicts.json"), verdict);
            assertEquals("passed", invoke(run, "gate").get("route"));
            String artifact = (String) invoke(run, "emit").get("choiceQualityVerdict");
            assertNotNull(new JacksonChoiceQualityVerdictReader().read(artifact));
        }
    }
    @Test void malformedOutputDeletesStaleValidatedArtifactAndNeverInventsPass(@TempDir Path run) throws Exception {
        Files.writeString(run.resolve("verdicts.json"), PASS);
        assertEquals("passed", invoke(run, "gate").get("route"));
        Files.writeString(run.resolve("verdicts.json"), FAIL.replace("\"Is he\"", "\"Is she\""));
        assertEquals("failed", invoke(run, "gate").get("route"));
        assertFalse(Files.exists(run.resolve("validated-verdict.json")));
        assertEquals(Map.of("outputUnusable", true), JSON.readValue((String) invoke(run, "emit").get("choiceQualityVerdict"), Map.class));
    }
    @Test void invalidTypesAndContradictionsCannotConsolidate(@TempDir Path run) throws Exception {
        for (String verdict : new String[]{PASS.replace("false", "\"false\""), FAIL.replace("\"Is he\"", "1"),
                FAIL.replace("\"HIGH\"", "\"BLOCKING\""), PASS.replace("\"PASS\"", "\"FAIL\""),
                FAIL.replace("\"FAIL\"", "\"PASS\""), FAIL.replace("\"reason\":", "\"unknown\":0,\"reason\":")}) {
            Files.writeString(run.resolve("verdicts.json"), verdict);
            assertEquals("failed", invoke(run, "gate").get("route"), verdict);
        }
    }
    @Test void emitRevalidatesArtifactRatherThanTrustingOnlyItsExistence(@TempDir Path run) throws Exception {
        Files.writeString(run.resolve("validated-verdict.json"), PASS.replace("\"objective\":\"Inversión\"", "\"objective\":null"));
        assertEquals(Map.of("outputUnusable", true), JSON.readValue((String) invoke(run, "emit").get("choiceQualityVerdict"), Map.class));
    }
    private static Map<?, ?> invoke(Path run, String mode) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(root.resolve(".sentinel/agents/choice-quality-judge"))) root = root.getParent();
        Path script = root.resolve(".sentinel/agents/choice-quality-judge/scripts/" + (mode.equals("gate") ? "gate_verdict.sh" : "emit.sh"));
        var process = new ProcessBuilder("bash", script.toString()).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        try (var input = process.getOutputStream()) {
            input.write(JSON.writeValueAsBytes(Map.of("inputs", Map.of("run_dir", run.toString(),
                    "options", "- Am I\n- Is he (correcta)"))));
        }
        assertTrue(process.waitFor(10, TimeUnit.SECONDS), "local gate timed out");
        assertEquals(0, process.exitValue());
        return JSON.readValue(process.getInputStream(), Map.class);
    }
}
