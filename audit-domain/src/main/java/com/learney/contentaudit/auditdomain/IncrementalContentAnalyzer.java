package com.learney.contentaudit.auditdomain;

/** A fresh instance per working session. onQuiz upserts a quiz; removeQuiz removes its contribution. */
public interface IncrementalContentAnalyzer extends ContentAnalyzer {
    void removeQuiz(String quizId);
}
