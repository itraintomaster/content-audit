package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.revisiondomain.batch.BatchCloseRequest;
import com.learney.contentaudit.revisiondomain.batch.BatchJudgeAccount;
import com.learney.contentaudit.revisiondomain.batch.CorrectionBatch;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link BatchJudgePass} that records every call -- its arguments, and its place among the calls to F's comparator
 * when a test makes the comparator append to the same {@link #order} -- and answers with the accounts the test
 * prepares, instead of judging anything for real. For the tests of the closer and the service that exercise the
 * judges' pass itself (FEAT-CJUEZ), in place of {@link NoJudgesPass}, which never judges and cannot be told what to
 * answer.
 */
class RecordingJudgePass implements BatchJudgePass {
    /** One entry per call -- "judge", "estimate" or "judges" -- in the order they happened. */
    final List<String> order;

    final List<JudgeCall> judgeCalls = new ArrayList<>();

    final List<EstimateCall> estimateCalls = new ArrayList<>();

    final List<CorrectionBatch> judgesCalls = new ArrayList<>();

    /** What {@link #judge} answers. */
    List<BatchJudgeAccount> judgeAnswer = List.of();

    /** What {@link #estimate} answers. */
    List<BatchJudgeAccount> estimateAnswer = List.of();

    /** What {@link #judges} answers. */
    List<String> judgesAnswer = List.of();

    RecordingJudgePass() {
        this(new ArrayList<>());
    }

    /** Shares {@code order} with the comparator the test builds, to see the call order between the two. */
    RecordingJudgePass(List<String> order) {
        this.order = order;
    }

    record JudgeCall(CorrectionBatch batch, CourseEntity startCourse, CourseEntity composedCourse,
            BatchCloseRequest request) {
    }

    record EstimateCall(CorrectionBatch batch, CourseEntity startCourse, CourseEntity composedCourse) {
    }

    @Override
    public List<BatchJudgeAccount> estimate(CorrectionBatch batch, CourseEntity startCourse,
            CourseEntity composedCourse) {
        order.add("estimate");
        estimateCalls.add(new EstimateCall(batch, startCourse, composedCourse));
        return estimateAnswer;
    }

    @Override
    public List<BatchJudgeAccount> judge(CorrectionBatch batch, CourseEntity startCourse, CourseEntity composedCourse,
            BatchCloseRequest request) {
        order.add("judge");
        judgeCalls.add(new JudgeCall(batch, startCourse, composedCourse, request));
        return judgeAnswer;
    }

    @Override
    public List<String> judges(CorrectionBatch batch) {
        order.add("judges");
        judgesCalls.add(batch);
        return judgesAnswer;
    }
}
