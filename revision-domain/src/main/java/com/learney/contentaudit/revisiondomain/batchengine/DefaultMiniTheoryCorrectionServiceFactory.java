package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.auditdomain.AuditEngine;
import com.learney.contentaudit.auditdomain.CourseMapper;
import com.learney.contentaudit.coursedomain.CourseRepository;
import com.learney.contentaudit.revisiondomain.RevisionArtifactStore;
import com.learney.contentaudit.revisiondomain.batch.BatchReviewService;
import com.learney.contentaudit.revisiondomain.batch.BatchStore;
import com.learney.contentaudit.revisiondomain.engine.DefaultCourseElementLocator;
import com.learney.contentaudit.revisiondomain.minitheorycorrection.*;
import java.nio.file.Path;
import java.util.function.Supplier;

public final class DefaultMiniTheoryCorrectionServiceFactory {
    public MiniTheoryCorrectionService create(BatchStore batches, BatchReviewService reviews,
            RevisionArtifactStore artifacts, CourseRepository courses, Path coursePath,
            MiniTheoryWorkspaceStore workspaces, CourseMapper mapper, AuditEngine engine,
            Supplier<MiniTheoryRewriter> rewriter) {
        return new DefaultMiniTheoryCorrectionService(batches, reviews,
                new DefaultBatchChangeLedger(artifacts, new DefaultCourseElementLocator()).withBatches(batches),
                courses, coursePath, workspaces, mapper, engine, rewriter);
    }
}
