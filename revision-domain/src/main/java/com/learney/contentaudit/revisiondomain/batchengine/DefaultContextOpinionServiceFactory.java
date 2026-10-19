package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.engine.DefaultCourseElementLocator;
import java.util.function.Function;

public final class DefaultContextOpinionServiceFactory {
    public ContextOpinionService create(BatchServiceConfig config, Function<CourseEntity, String> fingerprint) {
        return create(config, fingerprint, null);
    }
    public ContextOpinionService create(BatchServiceConfig config, Function<CourseEntity, String> fingerprint,
            PreliminaryOpinionSource preliminarySource) {
        var locator = config.getElementLocator() != null ? config.getElementLocator() : new DefaultCourseElementLocator();
        var ledger = new DefaultBatchChangeLedger(config.getArtifactStore(), locator);
        var reviews = new DefaultBatchReviewServiceFactory().create(config.getBatchStore(), config.getArtifactStore(),
                config.getRefinementPlanStore(), config.getReviewCache());
        return new DefaultContextOpinionService(config, ledger, reviews, fingerprint, preliminarySource);
    }
}
