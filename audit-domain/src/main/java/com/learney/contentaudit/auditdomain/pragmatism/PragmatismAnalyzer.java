package com.learney.contentaudit.auditdomain.pragmatism;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.auditdomain.judging.*;
import com.learney.contentaudit.evaluationledgerdomain.*;
import java.util.*;

final class PragmatismAnalyzer implements ContentAnalyzer {
    private final JudgeSessions opened;
    private final JudgeRuntime runtime;
    private final PragmatismSubjectBuilder builder;
    private final PragmatismVerdictReader reader;
    private final List<AuditNode> reached = new ArrayList<>();
    private final Set<AuditNode> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<AuditNode, List<FindingDraft>> findings = new IdentityHashMap<>();
    private boolean completed;
    PragmatismAnalyzer(JudgeSessions opened, JudgeRuntime runtime, PragmatismSubjectBuilder builder,
            PragmatismVerdictReader reader) {
        this.opened = opened; this.runtime = runtime; this.builder = builder; this.reader = reader;
    }
    @Override public Void onQuiz(AuditNode node) {
        if (node != null && node.getEntity() instanceof AuditableQuiz && seen.add(node)) reached.add(node);
        return null;
    }
    @Override public Void onKnowledge(AuditNode node) { return null; }
    @Override public Void onTopic(AuditNode node) { return null; }
    @Override public Void onMilestone(AuditNode node) { return null; }
    @Override public String getName() { return PragmatismAnalyzerProvider.NAME; }
    @Override public AuditTarget getTarget() { return AuditTarget.QUIZ; }
    @Override public String getDescription() { return "Uso comunicativo plausible de la oración terminada"; }
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
            var node = units.get(i);
            if (!(node.getDiagnoses() instanceof DefaultQuizDiagnoses)) node.setDiagnoses(new DefaultQuizDiagnoses());
            ((DefaultQuizDiagnoses) node.getDiagnoses()).setPragmatismDiagnosis(null);
            unevaluated(node);
            try {
                requests.add(new EvaluationRequest(builder.build(node), i < modes.size() && modes.get(i) != null
                        ? modes.get(i) : EvaluationRequestMode.REUSE_OR_EVALUATE));
                asked.add(node);
            } catch (RuntimeException invalidInput) { /* no completed accepted sentence: no invented question */ }
        }
        List<EvaluationResolution> resolutions = List.of();
        var sessions = opened != null ? opened.getSessions() : null;
        if (!requests.isEmpty() && sessions != null && !sessions.isEmpty()) {
            try {
                int concurrent = policy != null && policy.getMaxConcurrentEvaluations() != null
                        ? Math.max(1, policy.getMaxConcurrentEvaluations()) : 1;
                var answers = sessions.get(0).resolveAll(requests, concurrent);
                if (answers != null) resolutions = answers;
            } catch (RuntimeException unavailable) { /* never convert unavailable or no budget into PASS */ }
        }
        int evaluated = 0;
        var versions = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < asked.size(); i++) {
            var answer = i < resolutions.size() ? resolutions.get(i) : null;
            if (judge(asked.get(i), answer)) {
                evaluated++;
                if (answer.getEvaluatorVersion() != null) versions.merge(answer.getEvaluatorVersion(), 1, Integer::sum);
            }
        }
        if (root != null && opened != null) {
            if (!(root.getDiagnoses() instanceof DefaultCourseDiagnoses)) root.setDiagnoses(new DefaultCourseDiagnoses());
            var coverage = runtime.coverage(opened, new JudgeUnitTally(AuditTarget.QUIZ, units.size(), evaluated,
                    units.size() - evaluated, versions, plan != null ? plan.getUnmatchedDeclaredIds() : null,
                    new JudgeQuizTally(units.size(), evaluated, units.size() - evaluated)));
            if (coverage != null) ((DefaultCourseDiagnoses) root.getDiagnoses()).addJudgeCoverage(coverage);
        }
        return null;
    }
    private boolean judge(AuditNode node, EvaluationResolution answer) {
        if (answer == null || (answer.getKind() != EvaluationResolutionKind.EVALUATED
                && answer.getKind() != EvaluationResolutionKind.REUSED)) return false;
        try {
            var verdict = reader.read(answer.getPayload());
            var expected = PragmatismSubjectBuilder.sentences((AuditableQuiz) node.getEntity());
            if (verdict == null || verdict.assessments().size() != expected.size()
                    || !new HashSet<>(expected).equals(verdict.assessments().stream()
                            .map(PragmatismVerdict.Assessment::sentence).collect(java.util.stream.Collectors.toSet())))
                throw new IllegalArgumentException("El juez no evaluó exactamente todas las oraciones aceptadas");
            var subject = builder.build(node);
            var drafts = new ArrayList<FindingDraft>();
            boolean uncertain = false;
            for (var a : verdict.assessments()) {
                if (a.status() == PragmatismVerdict.Status.NEEDS_CONTEXT || a.status() == PragmatismVerdict.Status.NOT_APPLICABLE)
                    uncertain = true;
                if (a.status() != PragmatismVerdict.Status.FAIL) continue;
                drafts.add(new FindingDraft(a.issue().rule(), a.sentence(), a.severity(), FindingResolution.RANK_ONLY,
                        new FindingEvidence(List.of(new EvidencePart("Oración aceptada", a.sentence()),
                                new EvidencePart("Consigna visible", subject.getContent().get("instructions")),
                                new EvidencePart("Enunciado visible", subject.getContent().get("exercise")),
                                new EvidencePart("Intención evaluada", a.communicativePurpose())), a.reason(), List.of())));
            }
            ((DefaultQuizDiagnoses) node.getDiagnoses()).setPragmatismDiagnosis(new PragmatismDiagnosis(verdict,
                    answer.getEvaluatorVersion(), answer.getKind() == EvaluationResolutionKind.REUSED));
            findings.put(node, List.copyOf(drafts));
            if (uncertain) return false;
            if (node.getScores() == null) node.setScores(new LinkedHashMap<>());
            // RANK_ONLY findings retain the neutral score; the verdict and findings carry the alert.
            node.getScores().put(getName(), 1.0);
            node.getUnevaluatedBy().remove(getName());
            if (node.getPartiallyEvaluatedBy() != null) node.getPartiallyEvaluatedBy().remove(getName());
            return true;
        } catch (RuntimeException invalidOutput) { return false; }
    }
    private void unevaluated(AuditNode node) {
        if (node.getScores() != null) node.getScores().remove(getName());
        findings.remove(node);
        if (node.getUnevaluatedBy() == null) node.setUnevaluatedBy(new ArrayList<>());
        if (!node.getUnevaluatedBy().contains(getName())) node.getUnevaluatedBy().add(getName());
    }
}
