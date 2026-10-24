package com.learney.contentaudit.auditinfrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.revisiondomain.problems.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

/** One atomic record per problem, in the course folder; campaigns share it. */
public final class FileSystemProblemStore implements ProblemStore {
    private final Path directory;
    private final ObjectMapper mapper = JsonFiles.mapper();
    private static final Map<Path, Object> MONITORS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final ThreadLocal<Set<Path>> HELD = ThreadLocal.withInitial(HashSet::new);
    public FileSystemProblemStore(Path course) {
        try { directory = (Files.exists(course) ? course.toRealPath() : course.toAbsolutePath().normalize()).resolve(".content-audit/problems"); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
    Path directory() { return directory; }
    private Path file(String id) {
        if (id == null || !id.matches("PRB-[A-Za-z0-9-]+")) throw new IllegalArgumentException("Id de problema inválido.");
        return directory.resolve(id + ".json");
    }
    @Override public Optional<ContentProblem> load(String id) { return JsonFiles.read(mapper, file(id), ContentProblem.class); }
    @Override public List<ContentProblem> list() {
        if (!Files.isDirectory(directory)) return List.of();
        try (var paths = Files.list(directory)) {
            return paths.filter(p -> p.getFileName().toString().matches("PRB-[A-Za-z0-9-]+\\.json"))
                    .map(p -> JsonFiles.read(mapper, p, ContentProblem.class).orElseThrow())
                    .sorted(Comparator.comparing(ContentProblem::updatedAt).reversed()).toList();
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
    @Override public void save(ContentProblem value, long expectedRevision) {
        Path target = file(value.id());
        exclusive(() -> {
            var current = load(value.id());
            if (expectedRevision == -1 ? current.isPresent()
                    : current.isEmpty() || current.get().revision() != expectedRevision) {
                throw new IllegalStateException("El problema cambió. Volvé a leer la ficha.");
            }
            new FileSystemJudgeRegressionCatalog(directory.getParent().getParent()).confirm(value);
            JsonFiles.writeAtomically(mapper, target, value);
            return null;
        });
    }
    @Override public <T> T exclusive(java.util.function.Supplier<T> operation) {
        Path lockFile = directory.resolve(".write.lock");
        if (HELD.get().contains(lockFile)) return operation.get();
        synchronized (MONITORS.computeIfAbsent(lockFile, ignored -> new Object())) {
            try {
                Files.createDirectories(directory);
                try (var channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                        var lock = channel.lock()) {
                    HELD.get().add(lockFile);
                    try { return operation.get(); }
                    finally { HELD.get().remove(lockFile); if (HELD.get().isEmpty()) HELD.remove(); }
                }
            } catch (IOException e) { throw new UncheckedIOException(e); }
        }
    }
}
