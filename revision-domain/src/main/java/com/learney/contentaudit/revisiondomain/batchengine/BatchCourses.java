package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.revisiondomain.batch.*;

/** The single content source for all correction services, including legacy predecessors frozen by a successor. */
final class BatchCourses {
    private BatchCourses() {}
    static CourseEntity start(CorrectionBatch batch, CourseEntity live, BatchStore store) {
        CourseEntity checkpoint = batch.getBaseCourseSnapshot();
        if (checkpoint != null) return checkpoint;
        if (batch.getEvolution() != null) {
            CourseEntity base = batch.getEvolution().baseCourse();
            if (base != null) return base;
        }
        if (store != null) for (CorrectionBatch child : store.list()) {
            var lineage = child.getEvolution();
            if (lineage != null && batch.getId().equals(lineage.parentBatchId())) return lineage.baseCourse();
        }
        return live;
    }
}
