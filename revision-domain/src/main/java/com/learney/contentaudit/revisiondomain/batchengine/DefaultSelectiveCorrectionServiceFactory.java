package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.engine.DefaultCourseElementLocator;
import java.util.function.Function;

/** Composition root for the isolated selective workflow, sharing the native batch ledger and panel. */
public final class DefaultSelectiveCorrectionServiceFactory {
    public SelectiveCorrectionService create(BatchServiceConfig config, Function<CourseEntity, String> fingerprint,
            String correctorRuntime) {
        var locator = config.getElementLocator() != null ? config.getElementLocator() : new DefaultCourseElementLocator();
        var ledger = new DefaultBatchChangeLedger(config.getArtifactStore(), locator);
        var panels = new DefaultPanelStage(config.getPanelRunner(), config.getBriefer(), config.getCorrectionCatalog(),
                config.getWordLightsQuery(), ledger, config.getBatchStore(), config.getAuditEngine(), config.getCourseMapper())
                .withPlans(config.getRefinementPlanStore());
        var reviews = new DefaultBatchReviewServiceFactory().create(config.getBatchStore(), config.getArtifactStore(),
                config.getRefinementPlanStore(), config.getReviewCache());
        return new DefaultSelectiveCorrectionService(config, ledger, panels, reviews, fingerprint, correctorRuntime);
    }
}
