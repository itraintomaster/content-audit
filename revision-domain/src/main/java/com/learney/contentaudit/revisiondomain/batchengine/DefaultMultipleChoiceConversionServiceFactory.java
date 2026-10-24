package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.coursedomain.CourseRepository;
import com.learney.contentaudit.refinerdomain.RefinementPlanStore;
import com.learney.contentaudit.revisiondomain.RevisionArtifactStore;
import com.learney.contentaudit.revisiondomain.batch.BatchStore;
import com.learney.contentaudit.revisiondomain.conversion.MultipleChoiceConversionService;
import com.learney.contentaudit.revisiondomain.engine.DefaultCourseElementLocator;
import java.nio.file.Path;

public final class DefaultMultipleChoiceConversionServiceFactory {
    public MultipleChoiceConversionService create(BatchStore batches, RefinementPlanStore plans,
            RevisionArtifactStore artifacts, CourseRepository courses, Path path) {
        return new DefaultMultipleChoiceConversionService(batches, plans, courses, path,
                new DefaultBatchChangeLedger(artifacts, new DefaultCourseElementLocator()).withBatches(batches));
    }
}
