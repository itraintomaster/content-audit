package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.engine.DefaultCourseElementLocator;
import java.util.function.Function;

public final class DefaultQuizCorrectionServiceFactory {
    public QuizCorrectionService create(BatchServiceConfig config, Function<CourseEntity, String> fingerprint,
            String correctorRuntime) {
        var locator = config.getElementLocator() != null ? config.getElementLocator() : new DefaultCourseElementLocator();
        var ledger = new DefaultBatchChangeLedger(config.getArtifactStore(), locator).withBatches(config.getBatchStore());
        com.learney.contentaudit.refinerdomain.errortask.ErrorTaskBriefer briefer = (report, task) ->
                config.getBriefer().brief(report, task).map(brief -> {
                    brief.setContextFindings(DefaultQuizCorrectionService.contextFindings(report, task));
                    return brief;
                });
        var panels = new DefaultPanelStage(config.getPanelRunner(), briefer, config.getCorrectionCatalog(),
                config.getWordLightsQuery(), ledger, config.getBatchStore(), config.getAuditEngine(), config.getCourseMapper())
                .withPlans(config.getRefinementPlanStore());
        var reviews = new DefaultBatchReviewServiceFactory().create(config.getBatchStore(), config.getArtifactStore(),
                config.getRefinementPlanStore(), config.getReviewCache());
        // F-MESA-R032: what a rule fixes is fixed before the panel, with no query.
        var rules = new DefaultRuleStage(config.getRuleFixes(), config.getTopicMeter(),
                new com.learney.contentaudit.revisiondomain.nonregressionengine.DefaultGateFactory().changeGate(), ledger,
                config.getBatchStore()).withPlans(config.getRefinementPlanStore());
        return new DefaultQuizCorrectionService(config, ledger, panels, reviews, fingerprint, correctorRuntime, locator)
                .withRules(rules);
    }
}
