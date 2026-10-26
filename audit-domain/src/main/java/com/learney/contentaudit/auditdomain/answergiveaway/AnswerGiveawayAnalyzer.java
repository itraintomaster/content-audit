package com.learney.contentaudit.auditdomain.answergiveaway;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.auditdomain.judging.*;
import com.learney.contentaudit.evaluationledgerdomain.*;
import java.util.*;

/**
 * F-REGALA-R002/R003: asks the judge about every exercise it reaches, within the budget; what it gives away becomes a
 * finding for the panel, marked by the exact fragment. An exercise not judged stays pending, never approved.
 */
final class AnswerGiveawayAnalyzer implements ContentAnalyzer {
    private final JudgeSessions opened;
    private final JudgeRuntime runtime;
    private final AnswerGiveawaySubjectBuilder builder;
    private final AnswerGiveawayVerdictReader reader;
    private final List<AuditNode> reached = new ArrayList<>();
    private final Set<AuditNode> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<AuditNode, List<FindingDraft>> findings = new IdentityHashMap<>();
    private final Map<String, Integer> counts = new LinkedHashMap<>();
    private boolean completed;

    AnswerGiveawayAnalyzer(JudgeSessions opened, JudgeRuntime runtime, AnswerGiveawaySubjectBuilder builder,
            AnswerGiveawayVerdictReader reader) {
        this.opened = opened; this.runtime = runtime; this.builder = builder; this.reader = reader;
    }
    @Override public Void onQuiz(AuditNode node) {
        if (node != null && node.getEntity() instanceof AuditableQuiz && seen.add(node)) reached.add(node);
        return null;
    }
    @Override public Void onKnowledge(AuditNode node) { return null; }
    @Override public Void onTopic(AuditNode node) { return null; }
    @Override public Void onMilestone(AuditNode node) { return null; }
    @Override public String getName() { return AnswerGiveawayAnalyzerProvider.NAME; }
    @Override public AuditTarget getTarget() { return AuditTarget.QUIZ; }
    @Override public String getDescription() { return AnswerGiveawayAnalyzerProvider.LABEL; }
    @Override public List<FindingDraft> findingsAt(AuditNode node) { return findings.getOrDefault(node, List.of()); }

    @Override public Void onCourseComplete(AuditNode root) {
        if (completed) return null;
        completed = true;
        var policy = opened == null ? null : opened.getEffectivePolicy();
        var plan = runtime.plan(reached, policy);
        var units = plan == null || plan.getUnits() == null ? List.<AuditNode>of() : plan.getUnits();
        var modes = plan == null || plan.getModes() == null ? List.<EvaluationRequestMode>of() : plan.getModes();
        var asked = new ArrayList<AuditNode>();
        var subjects = new ArrayList<EvaluationSubject>();
        var requests = new ArrayList<EvaluationRequest>();
        for (int i = 0; i < units.size(); i++) {
            var node = units.get(i);
            clear(node);
            try {
                var subject = builder.build(node);
                subjects.add(subject);
                asked.add(node);
                requests.add(new EvaluationRequest(subject, i < modes.size() && modes.get(i) != null
                        ? modes.get(i) : EvaluationRequestMode.REUSE_OR_EVALUATE));
            } catch (RuntimeException badInput) { increment("INPUT_UNAVAILABLE"); }
        }
        List<EvaluationResolution> answers = List.of();
        if (!requests.isEmpty() && opened != null && opened.getSessions() != null && !opened.getSessions().isEmpty()) {
            try {
                var resolved = opened.getSessions().get(0).resolveAll(requests, policy != null
                        && policy.getMaxConcurrentEvaluations() != null ? Math.max(1, policy.getMaxConcurrentEvaluations()) : 1);
                if (resolved != null) answers = resolved;
            } catch (RuntimeException unavailable) { /* unavailable is pending, never approval */ }
        }
        int evaluated = 0;
        var versions = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < asked.size(); i++) {
            var answer = i < answers.size() ? answers.get(i) : null;
            if (judge(asked.get(i), subjects.get(i), answer)) {
                evaluated++;
                if (answer.getEvaluatorVersion() != null) versions.merge(answer.getEvaluatorVersion(), 1, Integer::sum);
            }
        }
        if (root != null) {
            if (root.getMetadata() == null) root.setMetadata(new LinkedHashMap<>());
            root.getMetadata().put(getName() + "Counts", new LinkedHashMap<>(counts));
            if (opened != null) {
                if (!(root.getDiagnoses() instanceof DefaultCourseDiagnoses)) root.setDiagnoses(new DefaultCourseDiagnoses());
                var coverage = runtime.coverage(opened, new JudgeUnitTally(AuditTarget.QUIZ, units.size(), evaluated,
                        units.size() - evaluated, versions, plan == null ? null : plan.getUnmatchedDeclaredIds(),
                        new JudgeQuizTally(units.size(), evaluated, units.size() - evaluated)));
                if (coverage != null) ((DefaultCourseDiagnoses) root.getDiagnoses()).addJudgeCoverage(coverage);
            }
        }
        return null;
    }

    private boolean judge(AuditNode node, EvaluationSubject subject, EvaluationResolution answer) {
        if (answer == null || (answer.getKind() != EvaluationResolutionKind.EVALUATED
                && answer.getKind() != EvaluationResolutionKind.REUSED)) {
            increment("PENDING_PROVIDER_OR_BUDGET");
            return false;
        }
        try {
            var verdict = reader.read(answer.getPayload());
            if (!subject.getSubjectRef().equals(verdict.quizId()))
                throw new IllegalArgumentException("Veredicto de otro ejercicio");
            increment(verdict.status().name());
            var drafts = new ArrayList<FindingDraft>();
            boolean pending = verdict.status() == AnswerGiveawayVerdict.Status.REVIEW;
            Double score = pending || verdict.status() == AnswerGiveawayVerdict.Status.NOT_APPLICABLE ? null
                    : verdict.status() == AnswerGiveawayVerdict.Status.GIVES_AWAY ? 0.0 : 1.0;
            if (verdict.status() == AnswerGiveawayVerdict.Status.GIVES_AWAY) {
                String field = verdict.where() == AnswerGiveawayVerdict.Where.INSTRUCTIONS ? "instructions" : "exercise";
                String read = subject.getContent().get(field);
                // The fragment is what the panel removes: it has to be where the judge says, as it is.
                if (read == null || !read.contains(verdict.givenAway().strip()))
                    throw new IllegalArgumentException("El fragmento que regala no está en " + field);
                drafts.add(finding(verdict, subject));
            }
            node.getMetadata().put(getName() + "Verdict", verdict);
            node.getMetadata().put(getName() + "Version", answer.getEvaluatorVersion());
            node.getMetadata().put(getName() + "Reused", answer.getKind() == EvaluationResolutionKind.REUSED);
            node.getMetadata().put(getName() + "InputContract", AnswerGiveawaySubjectBuilder.CONTRACT);
            var versions = new LinkedHashMap<String, Object>();
            if (node.getMetadata().get("verdictVersions") instanceof Map<?, ?> prior)
                prior.forEach((key, value) -> versions.put(key.toString(), value));
            versions.put(getName(), answer.getEvaluatorVersion());
            node.getMetadata().put("verdictVersions", versions);
            findings.put(node, List.copyOf(drafts));
            if (score != null) node.getScores().put(getName(), score);
            // REVIEW stays pending; NOT_APPLICABLE is an answer without a score, as in the judge of A1.
            node.getUnevaluatedBy().remove(getName());
            if (pending) node.getUnevaluatedBy().add(getName());
            return !pending;
        } catch (RuntimeException invalid) {
            increment("OUTPUT_UNUSABLE");
            node.getMetadata().put(getName() + "OutputIssue", invalid.getMessage());
            return false;
        }
    }

    /** F-REGALA-R003: the whole decision weighs HIGH and a part of it MEDIUM; the marker is the fragment. */
    private FindingDraft finding(AnswerGiveawayVerdict verdict, EvaluationSubject subject) {
        var examined = new ArrayList<EvidencePart>();
        examined.add(new EvidencePart("Consigna visible", subject.getContent().get("instructions")));
        examined.add(new EvidencePart("Enunciado visible", subject.getContent().get("exercise")));
        examined.add(new EvidencePart("Lo que decide el alumno", verdict.practicedDecision()));
        examined.add(new EvidencePart(verdict.extent() == AnswerGiveawayVerdict.Extent.WHOLE
                ? "Regala toda la decisión" : "Regala una parte", verdict.givenAway()));
        return new FindingDraft(AnswerGiveawayAnalyzerProvider.RULE, verdict.givenAway().strip(),
                verdict.extent() == AnswerGiveawayVerdict.Extent.WHOLE ? FindingSeverity.HIGH : FindingSeverity.MEDIUM,
                FindingResolution.PANEL, new FindingEvidence(examined.stream()
                        .filter(part -> part.getText() != null && !part.getText().isBlank()).toList(),
                        verdict.reason() + " Propuesta: " + verdict.proposedAction(), List.of()));
    }

    private void clear(AuditNode node) {
        if (node.getScores() == null) node.setScores(new LinkedHashMap<>());
        node.getScores().remove(getName());
        if (node.getMetadata() == null) node.setMetadata(new LinkedHashMap<>());
        for (var suffix : List.of("Verdict", "Version", "Reused", "InputContract", "OutputIssue"))
            node.getMetadata().remove(getName() + suffix);
        if (node.getMetadata().get("verdictVersions") instanceof Map<?, ?> old) {
            var remaining = new LinkedHashMap<String, Object>();
            old.forEach((key, value) -> { if (!getName().equals(key.toString())) remaining.put(key.toString(), value); });
            node.getMetadata().put("verdictVersions", remaining);
        }
        if (node.getUnevaluatedBy() == null) node.setUnevaluatedBy(new ArrayList<>());
        node.getUnevaluatedBy().remove(getName());
        node.getUnevaluatedBy().add(getName());
        if (node.getPartiallyEvaluatedBy() == null) node.setPartiallyEvaluatedBy(new ArrayList<>());
        node.getPartiallyEvaluatedBy().remove(getName());
        findings.remove(node);
    }

    private void increment(String status) { counts.merge(status, 1, Integer::sum); }
}
