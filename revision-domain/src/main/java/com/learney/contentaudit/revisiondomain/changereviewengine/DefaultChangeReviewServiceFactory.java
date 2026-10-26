package com.learney.contentaudit.revisiondomain.changereviewengine;

import com.learney.contentaudit.auditdomain.QuizStudentViewRenderer;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSessionFactory;
import com.learney.contentaudit.evaluationledgerdomain.Evaluator;
import com.learney.contentaudit.revisiondomain.batch.BatchReviewService;
import com.learney.contentaudit.revisiondomain.changereview.ChangeReviewService;
import com.learney.contentaudit.revisiondomain.changereview.ChangeReviewVerdictReader;

public final class DefaultChangeReviewServiceFactory {
    public ChangeReviewService create(BatchReviewService reviews, EvaluationSessionFactory sessions, Evaluator reviewer,
            ChangeReviewVerdictReader reader, QuizStudentViewRenderer renderer) {
        return new DefaultChangeReviewService(reviews, sessions, reviewer, reader, new ChangeReviewSubjects(renderer));
    }
}
