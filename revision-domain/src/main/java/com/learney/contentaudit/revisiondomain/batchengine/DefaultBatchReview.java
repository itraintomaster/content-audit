package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.refinerdomain.RefinementPlanStore;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.RevisionArtifactStore;
import com.learney.contentaudit.revisiondomain.RevisionProposal;
import com.learney.contentaudit.revisiondomain.batch.BatchChange;
import com.learney.contentaudit.revisiondomain.batch.BatchChangeStep;
import com.learney.contentaudit.revisiondomain.batch.BatchClosing;
import com.learney.contentaudit.revisiondomain.batch.BatchProposalRef;
import com.learney.contentaudit.revisiondomain.batch.BatchReview;
import com.learney.contentaudit.revisiondomain.batch.BatchReviewCache;
import com.learney.contentaudit.revisiondomain.batch.BatchReviewService;
import com.learney.contentaudit.revisiondomain.batch.BatchReviewDecision;
import com.learney.contentaudit.revisiondomain.batch.BatchStatus;
import com.learney.contentaudit.revisiondomain.batch.BatchStore;
import com.learney.contentaudit.revisiondomain.batch.CorrectionBatch;
import com.learney.contentaudit.revisiondomain.batch.NoOpenBatchException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Reads existing reports and proposals; human decisions never mutate the live correction or the course. */
final class DefaultBatchReview implements BatchReviewService {
    private final BatchStore batches;
    private final RevisionArtifactStore artifacts;
    private final RefinementPlanStore plans;
    private final BatchReportBuilder reports;
    private final BatchReviewCache cache;

    DefaultBatchReview(BatchStore batches, RevisionArtifactStore artifacts, RefinementPlanStore plans,
            BatchReportBuilder reports) {
        this(batches, artifacts, plans, reports, null);
    }

    DefaultBatchReview(BatchStore batches, RevisionArtifactStore artifacts, RefinementPlanStore plans,
            BatchReportBuilder reports, BatchReviewCache cache) {
        this.batches = batches;
        this.artifacts = artifacts;
        this.plans = plans;
        this.reports = reports;
        this.cache = cache;
    }

    @Override
    public BatchReview read(String batchId) {
        var version = cache == null ? java.util.Optional.<String>empty() : cache.version();
        if (version.isPresent()) {
            var cached = cache.load(batchId, version.get());
            if (cached.isPresent() && cached.get().items().stream().allMatch(item -> item.taskIds() != null)
                    && version.equals(cache.version())) return cached.get();
        }
        CorrectionBatch batch = batchId == null ? batches.open().or(batches::latestPublished)
                .orElseThrow(() -> new NoOpenBatchException("sin tanda para revisar"))
                : batches.load(batchId).orElseThrow(() -> new IllegalArgumentException("No existe la tanda " + batchId));
        BatchReview review = read(batch);
        // Never label a projection with a newer checkpoint than the one it actually read.
        if (version.isPresent() && version.equals(cache.version())) cache.save(batchId, version.get(), review);
        return review;
    }

    @Override
    public BatchReview decide(BatchReviewDecision request) {
        if (request == null || request.batchId() == null || request.batchId().isBlank()
                || request.status() == null || request.selections() == null
                || request.selections().isEmpty() || request.reviewer() == null || request.reviewer().isBlank()) {
            throw new IllegalArgumentException("La revisión necesita selección, decisión y revisor.");
        }
        CorrectionBatch batch = batches.load(request.batchId()).orElseThrow(
                () -> new IllegalArgumentException("No existe la tanda " + request.batchId()));
        if (batch.getStatus() == BatchStatus.PUBLISHED) {
            throw new IllegalArgumentException("La tanda ya fue integrada; su revisión es histórica.");
        }
        List<BatchReview.Item> current = items(batch, proposals(batch));
        // Validate the whole request before writing anything. An old screen cannot approve changed content.
        for (BatchReviewDecision.Selection selected : request.selections()) {
            if (selected == null || current.stream().noneMatch(item -> Objects.equals(item.changeId(), selected.changeId())
                    && Objects.equals(item.step(), selected.step()) && item.before() != null && item.after() != null
                    && Objects.equals(item.before(), selected.before()) && Objects.equals(item.after(), selected.after()))) {
                throw new IllegalArgumentException("La corrección cambió o fue retirada. Volvé a leer antes de aprobar.");
            }
        }
        batches.saveReview(new BatchReviewDecision(batch.getId(), List.copyOf(request.selections()), request.status(),
                request.reviewer().trim(), Instant.now()));
        return read(batch);
    }

    private BatchReview read(CorrectionBatch batch) {
        List<RevisionProposal> proposals = proposals(batch);
        var taskIds = new HashSet<>(BatchBook.list(batch.getTaskIds()));
        var tasks = plans.load(batch.getPlanId()).map(plan -> BatchBook.list(plan.getTasks()).stream()
                .filter(task -> taskIds.contains(task.getId())).toList()).orElse(List.of());
        return new BatchReview(batch.getId(), BatchBook.correctionRevision(batch), batch.getContentVersion(),
                reports.build(batch), tasks, proposals, items(batch, proposals), BatchBook.copy(batch.getClosings()), history(batch),
                BatchBook.copy(batch.getSelectiveRuns()));
    }

    private List<BatchReview.Version> history(CorrectionBatch selected) {
        Map<String, CorrectionBatch> related = new LinkedHashMap<>();
        for (CorrectionBatch batch : batches.list()) {
            if (batch.getLevel() == selected.getLevel()
                    && Objects.equals(batch.getBaselineAuditId(), selected.getBaselineAuditId())) {
                related.put(batch.getId(), batch);
            }
        }
        related.put(selected.getId(), selected);
        return related.values().stream()
                .sorted(Comparator.comparing(CorrectionBatch::getOpenedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(batch -> new BatchReview.Version(batch.getId(), BatchBook.correctionRevision(batch),
                        batch.getContentVersion(), batch.getStatus(), batch.getOpenedAt(),
                        BatchBook.list(batch.getClosings()).size(), batch.getBaselineAuditId(), batch.getStartAuditId()))
                .toList();
    }

    void snapshot(CorrectionBatch batch, BatchClosing closing) {
        closing.setProposals(proposals(batch));
        closing.setChanges(BatchBook.list(batch.getChanges()).stream().map(change -> new BatchChange(
                change.getChangeId(), change.getOrigin(), change.getRuleId(), change.getKnowledgeId(),
                BatchBook.copy(change.getTaskIds()), BatchBook.copy(change.getSteps()), change.getGateVerdict(),
                change.isJoseException(), change.getCreatedAt(), change.getDroppedReason())).toList());
    }

    private List<RevisionProposal> proposals(CorrectionBatch batch) {
        List<RevisionProposal> proposals = new ArrayList<>();
        for (BatchProposalRef ref : BatchBook.list(batch.getQuizProposals())) {
            artifacts.load(batch.getPlanId(), ref.getProposalId()).ifPresent(a -> {
                RevisionProposal p = a.getProposal();
                CourseElementSnapshot after = p.getElementBefore();
                for (BatchChangeStep step : BatchBook.stepsOn(batch, ref.getQuizId())) after = BatchBook.apply(after, step);
                // The writer may already have replaced the artifact but not the batch checkpoint.
                // Compose this checkpoint with the ledger's own primitive, never trust an ahead-of-checkpoint after.
                proposals.add(new RevisionProposal(p.getProposalId(), p.getTaskId(), p.getPlanId(), p.getSourceAuditId(),
                        p.getDiagnosisKind(), p.getNodeTarget(), p.getNodeId(), p.getElementBefore(), after,
                        p.getRationale(), p.getReviserKind(), p.getCreatedAt(), p.getStrategyId(), p.getUnmetCriteria(),
                        p.getLengthMeasurement()));
            });
        }
        return proposals;
    }

    private List<BatchReview.Item> items(CorrectionBatch batch, List<RevisionProposal> proposals) {
        Map<String, CourseElementSnapshot> states = new LinkedHashMap<>();
        for (RevisionProposal proposal : proposals) states.put(proposal.getNodeId(), proposal.getElementBefore());
        Map<String, BatchReviewDecision> decisions = new LinkedHashMap<>();
        var history = new ArrayList<>(batches.reviews(batch.getId()));
        history.sort(Comparator.comparing(BatchReviewDecision::decidedAt));
        for (BatchReviewDecision decision : history) {
            for (BatchReviewDecision.Selection selection : decision.selections()) {
                decisions.put(key(selection.changeId(), selection.step().getQuizId()), decision);
            }
        }
        List<BatchReview.Item> items = new ArrayList<>();
        for (BatchChange change : BatchBook.list(batch.getChanges())) {
            if (change == null || change.getDroppedReason() != null) continue;
            for (BatchChangeStep step : BatchBook.list(change.getSteps())) {
                if (step == null) continue;
                CourseElementSnapshot before = states.get(step.getQuizId());
                CourseElementSnapshot after = BatchBook.apply(before, step);
                states.put(step.getQuizId(), after);
                BatchReviewDecision decision = decisions.get(key(change.getChangeId(), step.getQuizId()));
                boolean matches = decision != null && decision.selections().stream().anyMatch(s ->
                        Objects.equals(s.changeId(), change.getChangeId()) && Objects.equals(s.step(), step)
                                && Objects.equals(s.before(), before) && Objects.equals(s.after(), after));
                items.add(new BatchReview.Item(change.getChangeId(), step, before, after,
                        change.getOrigin() == null ? null : change.getOrigin().name(), change.getRuleId(),
                        matches ? decision.status() : BatchReviewDecision.Status.PENDING,
                        matches ? decision.reviewer() : null, matches ? decision.decidedAt() : null,
                        BatchBook.copy(change.getTaskIds()), rationale(batch, change, step, before, after)));
            }
        }
        return items;
    }

    /** Explain only an accepted candidate that reproduces this exact persisted step, not the last request on a quiz. */
    private String rationale(CorrectionBatch batch, BatchChange change, BatchChangeStep step,
            CourseElementSnapshot before, CourseElementSnapshot after) {
        if (change.getOrigin() != com.learney.contentaudit.revisiondomain.batch.BatchChangeOrigin.PANEL
                || before == null || after == null) return null;
        var reasons = new java.util.LinkedHashSet<String>();
        for (var decision : BatchBook.list(batch.getPanelDecisions())) {
            var candidate = decision.getAccepted();
            if (candidate == null || decision.getOutcome() != com.learney.contentaudit.revisiondomain.panel.PanelOutcomeKind.ACCEPTED
                    || !Objects.equals(change.getKnowledgeId(), decision.getKnowledgeId())
                    || !new HashSet<>(BatchBook.list(change.getTaskIds()))
                            .equals(new HashSet<>(BatchBook.list(decision.getTaskIds())))
                    || !BatchBook.list(decision.getQuizIds()).contains(step.getQuizId())) continue;
            CourseElementSnapshot proposed = null;
            if (candidate.getQuiz() != null && before.getQuiz() != null) {
                proposed = new CourseElementSnapshot(before.getNodeTarget(), before.getNodeId(),
                        BatchBook.merged(before.getQuiz(), candidate.getQuiz()), null);
            } else if (candidate.getMiniTheoryText() != null && before.getQuiz() != null) {
                var quiz = BatchBook.copy(before.getQuiz());
                quiz.setMiniTheory(candidate.getMiniTheoryText());
                proposed = new CourseElementSnapshot(before.getNodeTarget(), before.getNodeId(), quiz, null);
            }
            if (proposed != null && Objects.equals(after, BatchBook.apply(before,
                    new BatchChangeStep(step.getQuizId(), step.getParts(), proposed)))
                    && candidate.getRationale() != null && !candidate.getRationale().isBlank()) {
                reasons.add(candidate.getRationale());
            }
        }
        // Conflicting historical explanations must not be presented as a verified origin.
        return reasons.size() == 1 ? reasons.iterator().next() : null;
    }

    private static String key(String changeId, String nodeId) {
        return changeId + "\u0000" + nodeId;
    }
}
