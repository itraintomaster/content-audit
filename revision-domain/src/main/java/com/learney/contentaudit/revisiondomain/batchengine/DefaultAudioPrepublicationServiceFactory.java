package com.learney.contentaudit.revisiondomain.batchengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.engine.DefaultCourseElementLocator;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.function.Function;

public final class DefaultAudioPrepublicationServiceFactory {
    public AudioPrepublicationService create(BatchServiceConfig config) {
        return create(config, course -> {
            try {
                byte[] bytes = new ObjectMapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).writeValueAsBytes(course);
                return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            } catch (Exception e) { throw new IllegalStateException("No se pudo identificar el contenido para los audios.", e); }
        });
    }
    public AudioPrepublicationService create(BatchServiceConfig config, Function<CourseEntity, String> fingerprint) {
        var locator = config.getElementLocator() == null ? new DefaultCourseElementLocator() : config.getElementLocator();
        return new DefaultAudioPrepublicationService(config,
                new DefaultBatchChangeLedger(config.getArtifactStore(), locator).withBatches(config.getBatchStore()), fingerprint);
    }
}
