package com.learney.contentaudit.auditinfrastructure;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Package-private helpers for the small JSON records the stores of package F keep under
 * {@code .content-audit/} (FEAT-RONDA2, hook F-06): the baseline, the sealed goals, the saved
 * projections and the blind samples. Every write goes to a temporary sibling first and is moved
 * into place, so a reader never sees half a file and saving again replaces the previous one.
 */
final class JsonFiles {

    private JsonFiles() {
    }

    /** Instants as ISO-8601 text, enums by name, and unknown fields ignored on read. */
    static ObjectMapper mapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new Jdk8Module());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    static <T> Optional<T> read(ObjectMapper mapper, Path file, Class<T> type) {
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(mapper.readValue(file.toFile(), type));
        } catch (IOException e) {
            throw new AuditPersistenceException("Failed to read " + file + ": " + e.getMessage(), e);
        }
    }

    static void writeAtomically(ObjectMapper mapper, Path file, Object value) {
        Path tmp = null;
        try {
            Files.createDirectories(file.getParent());
            tmp = Files.createTempFile(file.getParent(), "write-", ".tmp");
            mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), value);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            try {
                if (tmp != null) Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
                // the write already failed; the temporary file is the lesser problem
            }
            throw new AuditPersistenceException("Failed to write " + file + ": " + e.getMessage(), e);
        }
    }
}
