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
                "--ignore-user-config",
                "-c",
                "model_reasoning_effort=\"medium\"",
                "--ephemeral",
                "--sandbox",
                "read-only",
                "-m",
                "gpt-6.1-sol",
                "-"), Files.readAllLines(captured));
    }

    @Test
    void keepsAnAlreadyIsolatedExecAndItsExplicitRuntimeArguments(@TempDir Path tempDir)
            throws IOException, InterruptedException {
        Path captured = tempDir.resolve("args.txt");
        Path fakeCodex = tempDir.resolve("real-codex");
        Files.writeString(fakeCodex, "#!/usr/bin/env bash\nprintf '%s\\n' \"$@\" > \"$CAPTURED_ARGS\"\n");
        fakeCodex.toFile().setExecutable(true);

        ProcessBuilder builder = new ProcessBuilder(projectRoot().resolve("scripts/codex-runtime/codex").toString(),
                "exec", "--ignore-user-config", "--ephemeral", "-m", "gpt-6.1-sol", "-c", "model_provider=\"openai\"", "-");
        builder.environment().put("CONTENT_AUDIT_CODEX_REAL_BINARY", fakeCodex.toString());
        builder.environment().put("CONTENT_AUDIT_REASONING_EFFORT", "xhigh");
        builder.environment().put("CAPTURED_ARGS", captured.toString());
        Process process = builder.start();
        assertEquals(0, process.waitFor(), new String(process.getErrorStream().readAllBytes()));
        assertEquals(List.of("exec", "-c", "model_reasoning_effort=\"xhigh\"", "--ignore-user-config",
                "--ephemeral", "-m", "gpt-6.1-sol", "-c", "model_provider=\"openai\"", "-"), Files.readAllLines(captured));
    }

    @Test
    void passesLoginThroughWithoutChangingItsArgumentsOrAuthenticationHome(@TempDir Path tempDir)
            throws IOException, InterruptedException {
        Path captured = tempDir.resolve("args.txt");
        Path home = tempDir.resolve("home.txt");
        Path fakeCodex = tempDir.resolve("real-codex");
        Files.writeString(fakeCodex, "#!/usr/bin/env bash\nprintf '%s\\n' \"$@\" > \"$CAPTURED_ARGS\"\n"
                + "printf '%s\\n' \"$HOME\" \"${CODEX_HOME-}\" > \"$CAPTURED_HOME\"\n");
        fakeCodex.toFile().setExecutable(true);

        ProcessBuilder builder = new ProcessBuilder(projectRoot().resolve("scripts/codex-runtime/codex").toString(),
                "login", "status");
        builder.environment().put("CONTENT_AUDIT_CODEX_REAL_BINARY", fakeCodex.toString());
        builder.environment().put("CONTENT_AUDIT_REASONING_EFFORT", "invalid-for-exec");
        builder.environment().put("CAPTURED_ARGS", captured.toString());
        builder.environment().put("CAPTURED_HOME", home.toString());
        List<String> expectedHome = List.of(builder.environment().getOrDefault("HOME", ""),
                builder.environment().getOrDefault("CODEX_HOME", ""));
        Process process = builder.start();
        assertEquals(0, process.waitFor(), new String(process.getErrorStream().readAllBytes()));
        assertEquals(List.of("login", "status"), Files.readAllLines(captured));
        assertEquals(expectedHome, Files.readAllLines(home));
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
