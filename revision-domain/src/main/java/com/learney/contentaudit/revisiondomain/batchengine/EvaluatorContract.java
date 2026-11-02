package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.analysis.DefaultAnalysisComposer;
import com.learney.contentaudit.auditdomain.contextnumbers.AnalyzerErrorCounts;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.auditdomain.judging.JudgeAbstention;
import com.learney.contentaudit.auditdomain.judging.JudgeVerdictFreshness;
import com.learney.contentaudit.revisiondomain.batch.CurrentAnalysisService.Assessment;
import com.learney.contentaudit.revisiondomain.batch.CurrentAnalysisService.ChangeVerdict;
import com.learney.contentaudit.revisiondomain.batch.CurrentAnalysisService.Evaluation;
import com.learney.contentaudit.revisiondomain.batch.CurrentAnalysisService.Route;
import com.learney.contentaudit.revisiondomain.batch.CurrentAnalysisService.Standing;
import com.learney.contentaudit.revisiondomain.changereview.ChangeReview;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * F-CONTR: the one place that decides where each evaluator leaves an exercise and what that decides. Every evaluator
 * answers the same five steps (R001) -- applies, has an opinion, of today's version, decided, nothing from medium up --;
 * a judge with a version of today, an analyzer and the reviewer only differ in where each answer is read.
 */
final class EvaluatorContract {
    /** F-CONTR-R001 step 5: from this severity up, every evaluator stops the exercise. */
    static final FindingSeverity THRESHOLD = FindingSeverity.MEDIUM;
    /** F-CONTR-R003: the finding of an evaluator whose two judges did not agree. */
    static final String DISAGREEMENT = "judges-disagree";
    /** F-CONTR-R004: the reviewer, beside the evaluators of the catalog. */
    static final String REVIEWER = "change-reviewer";

    private EvaluatorContract() {}

    /** The mark of a judge's opinion against today's version, as the state counts it (F-ALDIA-R003). */
    record JudgeMark(JudgeVerdictFreshness freshness, JudgeAbstention abstention) {}

    /**
     * One state of the batch as the contract reads it: its exercises, the judges it marks against today's version and
     * their marks by exercise and judge, and the analyzers measured anywhere in it.
     */
    record Reading(Map<String, AuditNode> nodes, Set<String> judges,
            Map<String, Map<String, JudgeMark>> marks, Set<String> measured) {}

    /**
     * F-CONTR-R005 and R006: every exercise of {@code now}, with each evaluator's state. {@code reviewed} holds the
     * reviewer's reading of each exercise the batch changed: the others have no change verdict.
     */
    static Map<String, Evaluation> evaluate(Collection<String> evaluators, Reading before, Reading now,
            Map<String, ChangeReview.Row> reviewed) {
        var result = new LinkedHashMap<String, Evaluation>();
        now.nodes().forEach((id, node) -> {
            var current = new LinkedHashMap<String, Assessment>();
            for (String evaluator : evaluators) current.put(evaluator, assess(evaluator, node, now));
            current.put(REVIEWER, reviewer(reviewed.get(id)));
            Route route = route(current.values());
            if (!reviewed.containsKey(id)) {
                result.put(id, new Evaluation(route, null, List.of(), current, Map.of()));
                return;
            }
            var production = new LinkedHashMap<String, Assessment>();
            AuditNode was = before.nodes().get(id);
            // An exercise production did not have: no evaluator applied to it there.
            for (String evaluator : evaluators) production.put(evaluator, was == null
                    ? new Assessment(Standing.NOT_APPLICABLE, "not-in-production") : assess(evaluator, was, before));
            production.put(REVIEWER, reviewerInProduction(reviewed.get(id)));
            List<String> worsened = worsened(current, production);
            result.put(id, new Evaluation(route, change(route, worsened), worsened, current, production));
        });
        return result;
    }

    /** F-CONTR-R005: any FAILS has a problem; otherwise any MISSING waits for opinions; otherwise all pass. */
    static Route route(Collection<Assessment> assessments) {
        if (assessments.stream().anyMatch(a -> a.standing() == Standing.FAILS)) return Route.HAS_A_PROBLEM;
        if (assessments.stream().anyMatch(a -> a.standing() == Standing.MISSING)) return Route.OPINIONS_MISSING;
        return Route.ALL_PASS;
    }

    /**
     * F-CONTR-R006: the evaluators that fail now and passed or did not apply in production, which has the same
     * evaluators. One that was missing there does not prove the change made it worse.
     */
    static List<String> worsened(Map<String, Assessment> now, Map<String, Assessment> before) {
        return now.entrySet().stream()
                .filter(e -> e.getValue().standing() == Standing.FAILS)
                .filter(e -> before.get(e.getKey()).standing() == Standing.PASSES
                        || before.get(e.getKey()).standing() == Standing.NOT_APPLICABLE)
                .map(Map.Entry::getKey).toList();
    }

    /**
     * F-CONTR-R006: worsened when some evaluator did; otherwise the route says it, so «Lo dan por bueno» is exactly
     * «Todos los jueces lo aprueban».
     */
    static ChangeVerdict change(Route route, List<String> worsened) {
        if (!worsened.isEmpty()) return ChangeVerdict.WORSENED;
        return switch (route) {
            case HAS_A_PROBLEM -> ChangeVerdict.NOT_SHOWN_FIXED;
            case OPINIONS_MISSING -> ChangeVerdict.NOT_MEASURED;
            case ALL_PASS -> ChangeVerdict.CONSIDERED_GOOD;
        };
    }

    /** F-CONTR-R001: the five steps for an evaluator of the catalog on one exercise of {@code reading}. */
    static Assessment assess(String evaluator, AuditNode node, Reading reading) {
        // 1. Applies.
        if (!DefaultAnalysisComposer.eligible(node, evaluator)) return new Assessment(Standing.NOT_APPLICABLE, "not-eligible");
        if (reading.judges().contains(evaluator)) {
            // 2, 3 and 4 for a judge with a version of today: the mark of its opinion against that version.
            JudgeMark mark = reading.marks().getOrDefault(node.getEntity().getId(), Map.of()).get(evaluator);
            if (mark == null) return new Assessment(Standing.MISSING, "no-opinion");
            if (mark.freshness() != JudgeVerdictFreshness.UP_TO_DATE) return new Assessment(Standing.MISSING, "older-version");
            if (mark.abstention() != null) return switch (mark.abstention()) {
                case NOT_APPLICABLE -> new Assessment(Standing.NOT_APPLICABLE, "abstained-not-applicable");
                case NEEDS_CONTEXT -> new Assessment(Standing.MISSING, "abstained-needs-context");
                case REVIEW -> new Assessment(Standing.MISSING, "abstained-review");
            };
        } else {
            // 1 and 2 for an analyzer, as the exercise's numbers count it. F-CONTR-R003: without a judge of today any
            // version counts, as it does for an instant analyzer.
            AnalyzerErrorCounts counts = errorCounts(node, evaluator);
            boolean scored = scored(node, evaluator);
            if (counts == null && !scored) return reading.measured().contains(evaluator)
                    ? new Assessment(Standing.NOT_APPLICABLE, "not-reached") : new Assessment(Standing.MISSING, "not-measured");
            if (counts != null && counts.getReached() == 0 && !scored) return new Assessment(Standing.NOT_APPLICABLE, "not-reached");
            if (BatchBook.list(node.getUnevaluatedBy()).contains(evaluator) || BatchBook.list(node.getPartiallyEvaluatedBy()).contains(evaluator)
                    || counts != null && (counts.getNotEvaluated() > 0 || counts.getPartiallyEvaluated() > 0))
                return new Assessment(Standing.MISSING, "not-evaluated");
        }
        // 5. From medium up. F-CONTR-R002: the production sentence is not the exercise's; F-CONTR-R003: a disagreement
        // waits for the tiebreak unless the same evaluator already fails it for something else.
        List<Finding> own = BatchBook.list(node.getFindings()).stream()
                .filter(f -> evaluator.equals(f.getAnalyzer()) && !DefaultQuizCorrectionService.waitsForPublishing(f)).toList();
        if (own.stream().anyMatch(f -> !DISAGREEMENT.equals(f.getRule()) && serious(f))) return new Assessment(Standing.FAILS, "finding");
        if (own.stream().anyMatch(f -> DISAGREEMENT.equals(f.getRule()))) return new Assessment(Standing.MISSING, DISAGREEMENT);
        return new Assessment(Standing.PASSES, own.isEmpty() ? "clean" : "minor");
    }

    /** F-CONTR-R004: the reviewer's mark on the exercise the batch changed; an exercise it did not change is not its. */
    static Assessment reviewer(ChangeReview.Row row) {
        if (row == null) return new Assessment(Standing.NOT_APPLICABLE, "unchanged");
        if (row.verdict() == null) return new Assessment(Standing.MISSING,
                row.state() == ChangeReview.State.UNUSABLE ? "unusable" : "unmarked");
        return switch (row.verdict().mark()) {
            case GOOD -> new Assessment(Standing.PASSES, "good");
            case DOUBTFUL -> row.verdict().inheritedOnly() ? new Assessment(Standing.FAILS, "inherited")
                    : new Assessment(Standing.PASSES, "doubtful");
            case WORSE -> new Assessment(Standing.FAILS, "worse");
        };
    }

    /** F-CONTR-R006: production already had what an inherited mark points at; of any other mark it says nothing. */
    static Assessment reviewerInProduction(ChangeReview.Row row) {
        return row != null && row.verdict() != null && row.verdict().inheritedOnly()
                ? new Assessment(Standing.FAILS, "inherited") : new Assessment(Standing.NOT_APPLICABLE, "production");
    }

    /** F-CONTR-R002: medium, high or blocking; a finding without a known severity is not assumed light. */
    private static boolean serious(Finding finding) {
        return finding.getSeverity() == null || finding.getSeverity().ordinal() <= THRESHOLD.ordinal();
    }

    private static AnalyzerErrorCounts errorCounts(AuditNode node, String evaluator) {
        if (node.getNumbers() == null || node.getNumbers().getErrors() == null) return null;
        return BatchBook.list(node.getNumbers().getErrors().getAnalyzers()).stream()
                .filter(c -> evaluator.equals(c.getAnalyzer())).findFirst().orElse(null);
    }

    private static boolean scored(AuditNode node, String evaluator) {
        return node.getNumbers() != null && BatchBook.list(node.getNumbers().getAnalyzerScores()).stream()
                .anyMatch(s -> evaluator.equals(s.getAnalyzer()));
    }

    /** The analyzers a state measured anywhere, as its exercises' numbers name them. */
    static Set<String> measured(Collection<AuditNode> nodes) {
        var names = new HashSet<String>();
        for (AuditNode node : nodes) {
            if (node.getNumbers() == null) continue;
            if (node.getNumbers().getErrors() != null)
                for (var c : BatchBook.list(node.getNumbers().getErrors().getAnalyzers())) names.add(c.getAnalyzer());
            for (var s : BatchBook.list(node.getNumbers().getAnalyzerScores())) names.add(s.getAnalyzer());
        }
        return names;
    }
}
