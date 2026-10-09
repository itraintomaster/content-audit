package com.learney.contentaudit.auditinfrastructure.minitheory;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.revisiondomain.minitheorycorrection.MiniTheoryCorrection.Workspace;
import com.learney.contentaudit.revisiondomain.minitheorycorrection.MiniTheoryWorkspaceStore;
import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.security.*;
import java.util.HexFormat;
import java.util.function.Supplier;

/** Draft journal separate from the batch checkpoint. Readers never create or resume work. */
public final class FileSystemMiniTheoryWorkspaceStore implements MiniTheoryWorkspaceStore {
    private final Path directory;
    private final ObjectMapper mapper = com.learney.contentaudit.auditinfrastructure.AuditReportObjectMapper.create()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    public FileSystemMiniTheoryWorkspaceStore(Path baseDir) {
        directory = baseDir.resolve(".content-audit/mini-theory-corrections");
    }
    private Path file(String id, String suffix) {
        if (id == null || !id.matches("[A-Za-z0-9][A-Za-z0-9._-]*")) throw new IllegalArgumentException("Identidad de tanda inválida.");
        return directory.resolve(id + suffix);
    }
    @Override public Workspace load(String id) {
        Path path = file(id, ".json");
        if (!Files.exists(path)) return Workspace.empty();
        try { return mapper.readValue(path.toFile(), Workspace.class); }
        catch (IOException e) { throw new UncheckedIOException("No se pudo leer el historial de miniteoría.", e); }
    }
    @Override public void save(String id, Workspace workspace) {
        Path path = file(id, ".json");
        try {
            Files.createDirectories(directory);
            Path temporary = Files.createTempFile(directory, "draft-", ".tmp");
            try {
                mapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), workspace);
                try { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                catch (AtomicMoveNotSupportedException e) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
            } finally { Files.deleteIfExists(temporary); }
        } catch (IOException e) { throw new UncheckedIOException("No se pudo conservar el borrador.", e); }
    }
    @Override public String fingerprint(CourseEntity course) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(course))); }
        catch (IOException e) { throw new UncheckedIOException(e); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    @Override public String fingerprintIdentity(Object identity) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(mapper.writeValueAsBytes(canonicalIdentity(mapper.valueToTree(identity)))));
        } catch (IOException e) { throw new UncheckedIOException(e); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    // Identity sets/version lists are unordered. Content arrays keep their original order in fingerprint(course).
    private Object canonicalIdentity(JsonNode node) {
        if (node.isObject()) {
            var result = new java.util.TreeMap<String, Object>();
            node.fields().forEachRemaining(e -> result.put(e.getKey(), canonicalIdentity(e.getValue())));
            return result;
        }
        if (node.isArray()) {
            var result = new java.util.ArrayList<Object>();
            node.forEach(n -> result.add(canonicalIdentity(n)));
            result.sort(java.util.Comparator.comparing(Object::toString)); return result;
        }
        return node;
    }
    @Override public <T> T exclusive(String id, Supplier<T> action) {
        Path lockFile = file(id, ".lock");
        try {
            Files.createDirectories(directory);
            try (FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                FileLock lock;
                try { lock = channel.tryLock(); }
                catch (OverlappingFileLockException e) { throw new IllegalStateException("Hay otra operación de miniteoría en curso."); }
                if (lock == null) throw new IllegalStateException("Hay otra operación de miniteoría en curso.");
                try (lock) { return action.get(); }
            }
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
}
