package com.learney.contentaudit.auditcli.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuditCliLauncherTest {
    @TempDir Path workspace;
    private Path classpath;
    private Path refreshed;
    private Path bin;

    @BeforeEach
    void setUp() throws IOException {
        Files.copy(projectRoot().resolve("audit-cli.sh"), workspace.resolve("audit-cli.sh"));
        Files.createDirectories(workspace.resolve("audit-cli/target/classes"));
        classpath = workspace.resolve("audit-cli/target/runtime-classpath.txt");
        Files.writeString(classpath, "dependency.jar");
        Files.setLastModifiedTime(classpath, FileTime.from(Instant.parse("2026-01-02T00:00:00Z")));
        writePom(workspace.resolve("pom.xml"), "2026-01-01T00:00:00Z");
        writePom(workspace.resolve("audit-cli/pom.xml"), "2026-01-01T00:00:00Z");
        refreshed = workspace.resolve("refreshed");
        Files.createDirectories(workspace.resolve("scripts"));
        Files.writeString(workspace.resolve("scripts/refresh-audit-cli-classpath.sh"), """
                #!/usr/bin/env bash
                set -eu
                project="$(cd "$(dirname "$0")/.." && pwd)"
                touch "$project/refreshed"
                mkdir -p "$project/audit-cli/target/classes"
                printf 'dependency.jar' > "$project/audit-cli/target/runtime-classpath.txt"
                """);
        bin = Files.createDirectories(workspace.resolve("bin"));
        executable(bin.resolve("java"), "#!/usr/bin/env bash\nprintf '%s\\n' \"$@\"\n");
        executable(bin.resolve("find"), "#!/usr/bin/env bash\ntouch \"$FIND_MARKER\"\nexit 1\n");
    }

    @Test
    void savedReadsDoNotRecursivelyScanTheWorkspaceOrRefreshAnUnchangedReactor() throws Exception {
        writePom(workspace.resolve(".content-audit/runtime/archive/nested/pom.xml"), "2026-01-03T00:00:00Z");
        String output = launch();
        assertFalse(Files.exists(refreshed));
        assertFalse(Files.exists(workspace.resolve("find-called")));
        assertTrue(output.contains("/audit-cli/target/classes:dependency.jar"), output);
        assertTrue(output.contains("batch\nreview\n-f\njson"), output);
    }

    @Test
    void aChangedRootPomRefreshesTheClasspath() throws Exception {
        writePom(workspace.resolve("pom.xml"), "2026-01-03T00:00:00Z");
        launch();
        assertTrue(Files.exists(refreshed));
    }

    @Test
    void aChangedModulePomRefreshesTheClasspath() throws Exception {
        writePom(workspace.resolve("revision-domain/pom.xml"), "2026-01-03T00:00:00Z");
        Files.createDirectories(workspace.resolve("revision-domain/target/classes"));
        String output = launch();
        assertTrue(Files.exists(refreshed));
        assertTrue(output.contains("/revision-domain/target/classes:dependency.jar"), output);
    }

    @Test
    void aMissingClasspathIsRebuilt() throws Exception {
        Files.delete(classpath);
        launch();
        assertTrue(Files.exists(refreshed));
    }

    @Test
    void missingCompiledClassesAreRebuilt() throws Exception {
        Files.delete(workspace.resolve("audit-cli/target/classes"));
        launch();
        assertTrue(Files.exists(refreshed));
    }

    private String launch() throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("bash", workspace.resolve("audit-cli.sh").toString(),
                "batch", "review", "-f", "json").redirectErrorStream(true);
        builder.environment().put("PATH", bin + ":/usr/bin:/bin");
        builder.environment().put("FIND_MARKER", workspace.resolve("find-called").toString());
        Process process = builder.start();
        boolean finished = process.waitFor(10, TimeUnit.SECONDS);
        if (!finished) process.destroyForcibly();
        assertTrue(finished, "El lanzador quedó bloqueado");
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(), output);
        return output;
    }

    private static void executable(Path file, String script) throws IOException {
        Files.writeString(file, script);
        assertTrue(file.toFile().setExecutable(true));
    }

    private static void writePom(Path file, String time) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, "<project/>");
        Files.setLastModifiedTime(file, FileTime.from(Instant.parse(time)));
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.isRegularFile(current.resolve("audit-cli.sh"))) current = current.getParent();
        if (current == null) throw new IllegalStateException("No se encontró la raíz de content-audit");
        return current;
    }
}
