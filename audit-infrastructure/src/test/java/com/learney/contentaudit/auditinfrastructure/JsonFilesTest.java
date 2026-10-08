package com.learney.contentaudit.auditinfrastructure;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonFilesTest {
    @TempDir Path dir;

    @Test void concurrentWritersNeverShareTemporaryFilesOrLeavePartialJson() throws Exception {
        var mapper = JsonFiles.mapper();
        var file = dir.resolve("saved.json");
        var executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Void>> writes = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                final int writer = i;
                writes.add(() -> {
                    JsonFiles.writeAtomically(mapper, file, Map.of("writer", writer, "payload", "x".repeat(64_000)));
                    return null;
                });
            }
            for (var result : executor.invokeAll(writes)) result.get(5, TimeUnit.SECONDS);
            var saved = mapper.readTree(file.toFile());
            assertEquals(64_000, saved.path("payload").asText().length());
            assertTrue(saved.path("writer").asInt() >= 0 && saved.path("writer").asInt() < 20);
            try (var files = Files.list(dir)) {
                assertEquals(List.of(file), files.toList());
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
