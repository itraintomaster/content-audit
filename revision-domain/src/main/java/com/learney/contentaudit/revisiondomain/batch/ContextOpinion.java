package com.learney.contentaudit.revisiondomain.batch;

import com.learney.contentaudit.revisiondomain.RevisionProposal;
import com.learney.contentaudit.revisiondomain.comparison.ChangeOutcome;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Measurements supplement a correction without closing it or changing any proposal. */
public final class ContextOpinion {
    private ContextOpinion() {}
    public record Request(String action, String batchId, Map<String, List<String>> selection,
            boolean wholeTopic, String operationId, Integer contentVersion, String fingerprint,
            Integer queryCap, String reviewer) {}
    public record Judge(String analyzer, String displayName, String unit, int quizzes, int topics,
            int pendingQueries, int reusedQueries, boolean allowed, String reason, String judgeVersion,
            String runtime, int selectedPendingQueries, int topicPendingQueries) {}
    public record Result(String analyzer, ChangeOutcome outcome) {}
    public record Run(String id, Request request, int correctionRevision, int contentVersion,
            String fingerprint, int reserved, int spent, String status, Instant createdAt,
            Instant finishedAt, List<Judge> judges, List<RevisionProposal> proposals,
            List<Result> results, String detail) {}
    public record State(String batchId, int contentVersion, String fingerprint, boolean allowed,
            String reason, int pendingQueries, int reusedQueries, List<Judge> judges,
            Map<String, List<String>> selection, boolean wholeTopic, List<Run> runs) {}
}
