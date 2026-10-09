package com.learney.contentaudit.auditdomain.choicequality;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.auditdomain.judging.*;
import com.learney.contentaudit.evaluationledgerdomain.*;
import java.util.*;

/** Batched, budgeted and ledger-backed. Unknown context, failures and missing verdicts never pass. */
final class ChoiceQualityAnalyzer implements ContentAnalyzer {
    private final JudgeSessions opened;
    private final JudgeRuntime runtime;
    private final ChoiceQualitySubjectBuilder builder;
    private final ChoiceQualityVerdictReader reader;
    private final List<AuditNode> reached = new ArrayList<>();
    private final Set<AuditNode> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<AuditNode, List<FindingDraft>> findings = new IdentityHashMap<>();
    private boolean completed;

    ChoiceQualityAnalyzer(JudgeSessions opened, JudgeRuntime runtime,
            ChoiceQualitySubjectBuilder builder, ChoiceQualityVerdictReader reader) {
        this.opened = opened; this.runtime = runtime; this.builder = builder; this.reader = reader;
    }
    @Override public Void onQuiz(AuditNode node) {
        if (node != null && node.getEntity() instanceof AuditableQuiz q
                && q.getMultipleChoice() != null && seen.add(node)) reached.add(node);
        return null;
    }
    @Override public Void onKnowledge(AuditNode node) { return null; }
    @Override public Void onTopic(AuditNode node) { return null; }
    @Override public Void onMilestone(AuditNode node) { return null; }
    @Override public String getName() { return ChoiceQualityAnalyzerProvider.NAME; }
    @Override public AuditTarget getTarget() { return AuditTarget.QUIZ; }
    @Override public String getDescription() { return "Calidad pedagógica de los distractores de opción múltiple"; }
    @Override public List<FindingDraft> findingsAt(AuditNode node) { return findings.getOrDefault(node, List.of()); }

    @Override public Void onCourseComplete(AuditNode root) {
        if (completed) return null;
        completed = true;
        var policy = opened != null ? opened.getEffectivePolicy() : null;
        var plan = runtime.plan(new ArrayList<>(reached), policy);
        var units = plan != null && plan.getUnits() != null ? plan.getUnits() : List.<AuditNode>of();
        var modes = plan != null && plan.getModes() != null ? plan.getModes() : List.<EvaluationRequestMode>of();
        var asked = new ArrayList<AuditNode>();
        var requests = new ArrayList<EvaluationRequest>();
        for (int i = 0; i < units.size(); i++) {
            AuditNode node = units.get(i);
            if (node.getDiagnoses() instanceof DefaultQuizDiagnoses diagnoses) diagnoses.setChoiceQualityDiagnosis(null);
            try {
                requests.add(new EvaluationRequest(builder.build(node),
                        i < modes.size() && modes.get(i) != null ? modes.get(i) : EvaluationRequestMode.REUSE_OR_EVALUATE));
                asked.add(node);
            } catch (RuntimeException failure) { unevaluated(node); }
        }
        List<EvaluationResolution> resolutions = List.of();
        var sessions = opened != null ? opened.getSessions() : null;
        if (!requests.isEmpty() && sessions != null && !sessions.isEmpty()) {
            try {
                int concurrent = policy != null && policy.getMaxConcurrentEvaluations() != null
                        ? Math.max(1, policy.getMaxConcurrentEvaluations()) : 1;
                var result = sessions.get(0).resolveAll(requests, concurrent);
                if (result != null) resolutions = result;
            } catch (RuntimeException failure) { /* unavailable: all remain unevaluated */ }
        }
        int evaluated = 0;
        Map<String, Integer> versions = new LinkedHashMap<>();
        for (int i = 0; i < asked.size(); i++) {
            var resolution = i < resolutions.size() ? resolutions.get(i) : null;
            if (judge(asked.get(i), resolution)) {
                evaluated++;
                if (resolution.getEvaluatorVersion() != null) versions.merge(resolution.getEvaluatorVersion(), 1, Integer::sum);
            } else unevaluated(asked.get(i));
        }
        if (root != null && opened != null) {
            if (!(root.getDiagnoses() instanceof DefaultCourseDiagnoses)) root.setDiagnoses(new DefaultCourseDiagnoses());
            var coverage = runtime.coverage(opened, new JudgeUnitTally(AuditTarget.QUIZ, units.size(),
                    evaluated, units.size() - evaluated, versions, plan != null ? plan.getUnmatchedDeclaredIds() : null,
                    new JudgeQuizTally(units.size(), evaluated, units.size() - evaluated)));
            if (coverage != null) ((DefaultCourseDiagnoses) root.getDiagnoses()).addJudgeCoverage(coverage);
        }
        return null;
    }

    private boolean judge(AuditNode node, EvaluationResolution resolution) {
        if (resolution == null || (resolution.getKind() != EvaluationResolutionKind.EVALUATED
                && resolution.getKind() != EvaluationResolutionKind.REUSED)) return false;
        try {
            var verdict = reader.read(resolution.getPayload());
            if (verdict == null) return false;
            var subject = builder.build(node);
            var quiz = (AuditableQuiz) node.getEntity();
            Set<String> labels = new HashSet<>();
            for (var option : quiz.getMultipleChoice().getItems()) if (option != null) labels.add(option.getLabel());
            var drafts = new ArrayList<FindingDraft>();
            double score = 1;
            Set<String> identities = new HashSet<>();
            for (var issue : verdict.issues()) {
                if (!labels.containsAll(issue.labels())) throw new IllegalArgumentException("El juez nombró una opción que no existe");
                String marker = String.join(" | ", issue.labels());
                if (!identities.add(issue.kind().rule() + "|" + marker)) throw new IllegalArgumentException("Problema repetido");
                drafts.add(new FindingDraft(issue.kind().rule(), marker, issue.severity(), FindingResolution.PANEL,
                        new FindingEvidence(List.of(
                                new EvidencePart("Objetivo pedagógico", verdict.objective()),
                                new EvidencePart("Nivel", subject.getContent().get("level")),
                                new EvidencePart("Consigna", subject.getContent().get("instructions")),
                                new EvidencePart("Ejercicio", subject.getContent().get("exercise")),
                                new EvidencePart("Opciones", subject.getContent().get("options")),
                                new EvidencePart("Confusión esperable", issue.misconception())),
                                issue.reason(), List.of())));
                score = Math.min(score, switch (issue.severity()) { case HIGH -> 0.3; case MEDIUM -> 0.6; default -> 0.9; });
            }
            if (!(node.getDiagnoses() instanceof DefaultQuizDiagnoses)) node.setDiagnoses(new DefaultQuizDiagnoses());
            ((DefaultQuizDiagnoses) node.getDiagnoses()).setChoiceQualityDiagnosis(new ChoiceQualityDiagnosis(verdict,
                    resolution.getEvaluatorVersion(), resolution.getKind() == EvaluationResolutionKind.REUSED));
            if (verdict.status() == ChoiceQualityVerdict.Status.NEEDS_CONTEXT) return false;
            if (node.getScores() == null) node.setScores(new LinkedHashMap<>());
            node.getScores().put(getName(), score);
            if (node.getUnevaluatedBy() != null) node.getUnevaluatedBy().remove(getName());
            findings.put(node, List.copyOf(drafts));
            return true;
        } catch (RuntimeException failure) { return false; }
    }
    private void unevaluated(AuditNode node) {
        if (node.getScores() != null) node.getScores().remove(getName());
        findings.remove(node);
        if (node.getUnevaluatedBy() == null) node.setUnevaluatedBy(new ArrayList<>());
        if (!node.getUnevaluatedBy().contains(getName())) node.getUnevaluatedBy().add(getName());
    }
}
