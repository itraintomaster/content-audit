package com.learney.contentaudit.auditcli.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CodexRuntimeWrapperTest {

    @Test
    void injectsTheExactMediumReasoningArgumentIntoCodexExec(@TempDir Path tempDir)
            throws IOException, InterruptedException {
        Path captured = tempDir.resolve("args.txt");
        Path fakeCodex = tempDir.resolve("real-codex");
        Files.writeString(fakeCodex, "#!/usr/bin/env bash\nprintf '%s\\n' \"$@\" > \"$CAPTURED_ARGS\"\n");
        fakeCodex.toFile().setExecutable(true);

        Path wrapper = projectRoot().resolve("scripts/codex-runtime/codex");
        ProcessBuilder processBuilder = new ProcessBuilder(
                wrapper.toString(), "exec", "--ephemeral", "--sandbox", "read-only",
                "-m", "gpt-6.1-sol", "-");
        processBuilder.environment().put("CONTENT_AUDIT_CODEX_REAL_BINARY", fakeCodex.toString());
        processBuilder.environment().remove("CONTENT_AUDIT_REASONING_EFFORT");
        processBuilder.environment().put("CAPTURED_ARGS", captured.toString());

        Process process = processBuilder.start();
        int exit = process.waitFor();
        String stderr = new String(process.getErrorStream().readAllBytes());

        assertEquals(0, exit, stderr);
        assertEquals(List.of(
                "exec",
                "-c",
                "model_reasoning_effort=\"medium\"",
                "--ephemeral",
                "--sandbox",
                "read-only",
                "-m",
                "gpt-6.1-sol",
                "-"), Files.readAllLines(captured));
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.isRegularFile(current.resolve("audit-cli.sh"))) {
            current = current.getParent();
        }
        if (current == null) {
            throw new IllegalStateException("No se encontró la raíz de content-audit");
        }
        return current;
    }
}
