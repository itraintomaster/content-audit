package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.revisiondomain.problems.ContentProblem;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Read-only view of a delivery against the files currently in the checkout. */
final class ProblemInspection {
    static Map<String, Object> inspect(Path project, ContentProblem problem) throws Exception {
        Path root = project.toRealPath();
        var changes = new ArrayList<Map<String, Object>>();
        Object entries = problem.delivery().get("changes");
        if (entries instanceof Iterable<?> list) for (Object entry : list) {
            if (!(entry instanceof Map<?, ?> recorded)) continue;
            var change = new LinkedHashMap<String, Object>();
            recorded.forEach((key, value) -> change.put(String.valueOf(key), value));
            String raw = String.valueOf(recorded.get("path"));
            Path path = project.resolve(raw).normalize();
            if (!Files.isRegularFile(path)
                    || !path.toRealPath().startsWith(root)
                    || !raw.endsWith(".md") && !raw.endsWith(".yaml") && !raw.endsWith(".json") && !raw.endsWith(".java")) {
                change.put("available", false);
                change.put("reason", "El archivo de la entrega no está disponible dentro del proyecto.");
            } else {
                path = path.toRealPath();
                String relative = root.relativize(path).toString();
                change.put("available", true);
                change.put("relativePath", relative);
                String version = "sha256:" + HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
                change.put("currentVersion", version);
                change.put("matchesDelivery", version.equals(recorded.get("version")));
                if (Files.size(path) <= 65536) {
                    change.put("after", Files.readString(path));
                    Process process = new ProcessBuilder("git", "show", "HEAD:" + relative)
                            .directory(root.toFile()).redirectError(ProcessBuilder.Redirect.DISCARD).start();
                    byte[] before = process.getInputStream().readNBytes(65537);
                    if (process.waitFor(3, TimeUnit.SECONDS) && process.exitValue() == 0 && before.length <= 65536) {
                        String text = new String(before, StandardCharsets.UTF_8);
                        change.put("before", text);
                        change.put("changed", !text.equals(change.get("after")));
                    } else process.destroyForcibly();
                    Process diff = new ProcessBuilder("git", "diff", "--no-ext-diff", "--no-textconv", "HEAD", "--", relative)
                            .directory(root.toFile()).redirectError(ProcessBuilder.Redirect.DISCARD).start();
                    byte[] delta = diff.getInputStream().readNBytes(65537);
                    if (diff.waitFor(3, TimeUnit.SECONDS) && diff.exitValue() == 0 && delta.length <= 65536) {
                        change.put("diff", new String(delta, StandardCharsets.UTF_8));
                    } else diff.destroyForcibly();
                } else change.put("reason", "El archivo es grande; se muestra su versión, sin cargar el texto.");
            }
            changes.add(change);
        }
        var result = new LinkedHashMap<String, Object>();
        result.put("id", problem.id());
        result.put("revision", problem.revision());
        result.put("changes", changes);
        result.put("nextSteps", problem.delivery().getOrDefault("nextSteps", java.util.List.of()));
        result.put("judgeName", problem.delivery().getOrDefault("judgeName", ""));
        return result;
    }
}
