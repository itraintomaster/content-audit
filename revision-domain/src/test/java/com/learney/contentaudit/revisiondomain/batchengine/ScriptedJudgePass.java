package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.revisiondomain.batch.BatchCloseRequest;
import com.learney.contentaudit.revisiondomain.batch.BatchJudgeAccount;
import com.learney.contentaudit.revisiondomain.batch.CorrectionBatch;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link BatchJudgePass} that answers with the accounts and judge names a test prepares, instead of running any
 * engine, and keeps every call it got -- {@code estimate}, {@code judge} and {@code judges} -- with the courses and
 * request each one received: for the tests of the service and the closer that exercise how they use the pass -- what
 * they call, with what, and what they do with the answer -- not the pass itself (that is
 * {@code DefaultBatchJudgePassTest}'s job).
 */
class ScriptedJudgePass implements BatchJudgePass {
    /** What {@link #estimate} answers with. */
    public List<BatchJudgeAccount> estimateAccounts = List.of();

    /** What {@link #judge} answers with. */
    public List<BatchJudgeAccount> judgeAccounts = List.of();

    /** What {@link #judges} answers with, in the order a test wants read back as «the order of the catalog». */
    public List<String> judgeNames = List.of();

    public final List<CourseEntity> estimateStartCourses = new ArrayList<>();

    public final List<CourseEntity> estimateComposedCourses = new ArrayList<>();

    public final List<CourseEntity> judgeStartCourses = new ArrayList<>();

    public final List<CourseEntity> judgeComposedCourses = new ArrayList<>();

    /** Every request {@link #judge} got, in the order it got them: empty while nothing paid any judge. */
    public final List<BatchCloseRequest> judgeRequests = new ArrayList<>();

    public int judgesCalls;

    @Override
    public List<BatchJudgeAccount> estimate(CorrectionBatch batch, CourseEntity startCourse,
            CourseEntity composedCourse) {
        estimateStartCourses.add(startCourse);
        estimateComposedCourses.add(composedCourse);
        return estimateAccounts;
    }

    @Override
    public List<BatchJudgeAccount> judge(CorrectionBatch batch, CourseEntity startCourse,
            CourseEntity composedCourse, BatchCloseRequest request) {
        judgeStartCourses.add(startCourse);
        judgeComposedCourses.add(composedCourse);
        judgeRequests.add(request);
        return judgeAccounts;
    }

    @Override
    public List<String> judges(CorrectionBatch batch) {
        judgesCalls++;
        return judgeNames;
    }
}
