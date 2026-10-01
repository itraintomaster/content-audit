package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.revisiondomain.batch.BatchCloseRequest;
import com.learney.contentaudit.revisiondomain.batch.BatchJudgeAccount;
import com.learney.contentaudit.revisiondomain.batch.CorrectionBatch;
import java.util.List;

/**
 * A {@link BatchJudgePass} that never judges anything: for the tests of the closer and the service that do not
 * exercise the judges' pass itself (that is {@code DefaultBatchJudgePassTest}'s job). Every query comes back empty,
 * so the callers it feeds behave exactly as they did before FEAT-CJUEZ.
 */
class NoJudgesPass implements BatchJudgePass {
    @Override
    public List<BatchJudgeAccount> estimate(CorrectionBatch batch, CourseEntity startCourse,
            CourseEntity composedCourse) {
        return List.of();
    }

    @Override
    public List<BatchJudgeAccount> judge(CorrectionBatch batch, CourseEntity startCourse,
            CourseEntity composedCourse, BatchCloseRequest request) {
        return List.of();
    }

    @Override
    public List<String> judges(CorrectionBatch batch) {
        return List.of();
    }
}
