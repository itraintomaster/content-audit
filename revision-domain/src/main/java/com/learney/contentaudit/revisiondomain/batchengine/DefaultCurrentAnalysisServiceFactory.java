package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.auditdomain.QuizStudentViewRenderer;
import com.learney.contentaudit.auditdomain.analysis.AnalysisComposer;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.engine.DefaultCourseElementLocator;
import java.util.function.Function;

public final class DefaultCurrentAnalysisServiceFactory {
    public CurrentAnalysisService create(BatchServiceConfig config, AnalysisComposer composer,
            Function<CourseEntity, String> fingerprint, QuizStudentViewRenderer renderer) {
        config.setAnalysisComposer(composer);
        var locator = config.getElementLocator() == null ? new DefaultCourseElementLocator() : config.getElementLocator();
        return new DefaultCurrentAnalysisService(config, new DefaultBatchChangeLedger(config.getArtifactStore(), locator).withBatches(config.getBatchStore()),
                composer, fingerprint, renderer, new DefaultBatchReviewServiceFactory().create(config.getBatchStore(),
                config.getArtifactStore(), config.getRefinementPlanStore(), config.getReviewCache()));
    }
}
