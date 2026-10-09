package com.learney.contentaudit.revisiondomain.minitheorycorrection;

import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import java.time.Instant;
import java.util.List;

/** Directed work inside an existing correction. None of these operations publishes the course. */
public final class MiniTheoryCorrection {
    private MiniTheoryCorrection() {}

    public record Request(String action, String operationId, String batchId, String knowledgeId,
            List<String> quizIds, List<String> draftIds, Integer contentVersion, boolean includeApproved,
            String text, String feedback, String sourceId, Integer cap, String fingerprint, String reviewer) {}

    public record Quiz(String quizId, String miniTheory, CourseElementSnapshot snapshot, boolean approved) {}
    public record Candidate(String id, String quizId, String text, String origin, String status,
            String knowledgeId, String label) {
        public Candidate(String id, String quizId, String text, String origin, String status) {
            this(id, quizId, text, origin, status, null, null);
        }
    }
    public record Draft(String id, String knowledgeId, String quizId, CourseElementSnapshot before,
            String text, String origin, String sourceId, String feedback, String author, String provenance,
            int contentVersion, Instant createdAt) {}
    public record Feedback(String id, String knowledgeId, List<String> quizIds, String text,
            String author, int contentVersion, Instant createdAt) {}
    /** reserved is a durable upper charge; after an uncertain interruption it must not be granted again. */
    public record Job(String id, String action, String knowledgeId, List<String> quizIds,
            List<String> draftIds, String status, int cap, int reserved, int spent,
            String fingerprint, String error, AuditReport report, Instant createdAt) {}
    public record Workspace(List<Draft> drafts, List<Feedback> feedback, List<Job> jobs) {
        public static Workspace empty() { return new Workspace(List.of(), List.of(), List.of()); }
    }
    public record Estimate(String fingerprint, int topicQuizzes, int pendingQueries,
            boolean allowed, String reason, List<String> analyzers) {}
    public record State(String batchId, String knowledgeId, int contentVersion, int correctionRevision,
            List<Quiz> quizzes, List<Candidate> candidates, List<Draft> drafts,
            List<Feedback> feedback, List<Job> jobs, Estimate estimate) {}
    public record RewriteInput(String feedback, String quizId, KnowledgeEntity topic,
            QuizTemplateEntity quiz, List<Candidate> candidates) {}
    public record RewriteResult(String text, String sourceId, String provenance) {}
}
