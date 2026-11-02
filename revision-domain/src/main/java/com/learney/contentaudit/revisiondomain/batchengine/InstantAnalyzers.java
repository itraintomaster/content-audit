package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.auditdomain.finding.AnalysisCost;
import com.learney.contentaudit.auditdomain.AnalyzerDescriptor;
import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.EvaluationRunPolicy;
import com.learney.contentaudit.auditdomain.EvaluationRunPurpose;
import com.learney.contentaudit.auditdomain.analysis.DefaultAnalysisComposer;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerFamily;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.AnalyzerCorrection;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.revisiondomain.batch.BatchServiceConfig;
import com.learney.contentaudit.revisiondomain.batch.CorrectionBatch;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * A batch keeps the analyzers it had when it was opened. An instant analyzer of errors of the catalog with a correction
 * of its own that the batch lacks -- it came after the batch was made -- is measured over the course for free: it gives
 * a correction its tasks (F-MESA-R028) and the reading of the analysis what it finds (F-CONTR-R007).
 */
final class InstantAnalyzers {
    private InstantAnalyzers() {}

    /** Adds to {@code report}, in place, the scores and findings of the free analyzers of errors it lacks. */
    static AuditReport theBatchLacks(BatchServiceConfig config, CorrectionBatch batch, CourseEntity course, AuditReport report) {
        if (config.getAnalyzerCatalog() == null || config.getCorrectionCatalog() == null || config.getAuditEngine() == null
                || config.getCourseMapper() == null || report == null || report.getRoot() == null) return report;
        var index = DefaultAnalysisComposer.index(report.getRoot());
        var names = new ArrayList<String>();
        for (AnalyzerDescriptor card : BatchBook.list(config.getAnalyzerCatalog().list())) {
            String name = card.getName();
            if (name == null || BatchBook.list(batch.getAnalyzers()).contains(name) || card.getCost() != AnalysisCost.INSTANT
                    || card.getFamily() != AnalyzerFamily.ERRORS
                    || config.getCorrectionCatalog().find(name).map(AnalyzerCorrection::contextOnly).orElse(true)
                    || index.values().stream().anyMatch(n -> n.getScores().containsKey(name))) continue;
            names.add(name);
        }
        if (names.isEmpty()) return report;
        var policies = new LinkedHashMap<String, EvaluationRunPolicy>();
        for (String name : names) policies.put(name, new EvaluationRunPolicy(0, false, null, null, EvaluationRunPurpose.READING, 1));
        AuditReport extra = config.getAuditEngine().runAudit(config.getCourseMapper().map(course),
                new AnalyzerRunSelection(names, policies));
        if (extra == null || extra.getRoot() == null) return report;
        for (var entry : DefaultAnalysisComposer.index(extra.getRoot()).entrySet()) {
            AuditNode to = index.get(entry.getKey());
            if (to == null) continue;
            for (String name : names) {
                Double score = entry.getValue().getScores().get(name);
                if (score != null) to.getScores().put(name, score);
                to.getFindings().addAll(entry.getValue().getFindings().stream().filter(f -> name.equals(f.getAnalyzer())).toList());
            }
        }
        return report;
    }
}
