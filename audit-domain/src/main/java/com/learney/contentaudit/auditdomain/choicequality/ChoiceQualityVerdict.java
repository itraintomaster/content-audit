package com.learney.contentaudit.auditdomain.choicequality;

import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import java.util.List;

/** Independent pedagogical verdict; uncertainty is never a passing judgment. */
public record ChoiceQualityVerdict(Status status, String objective, boolean introductory,
        List<Issue> issues, String rationale) {
    public enum Status { PASS, FAIL, NEEDS_CONTEXT }
    public enum Kind {
        ANSWER_CUE("answer-cue"),
        IRRELEVANT_DISTRACTORS("irrelevant-distractors"),
        IMPLAUSIBLE_DISTRACTORS("implausible-distractors");
        private final String rule;
        Kind(String rule) { this.rule = rule; }
        public String rule() { return rule; }
    }
    public record Issue(Kind kind, FindingSeverity severity, List<String> labels,
            String reason, String misconception) {
        public Issue {
            if (kind == null || severity == null || severity == FindingSeverity.BLOCKING
                    || labels == null || labels.isEmpty()
                    || labels.stream().anyMatch(s -> s == null || s.isBlank())
                    || reason == null || reason.isBlank()
                    || misconception == null || misconception.isBlank()) {
                throw new IllegalArgumentException("Problema de distractores incompleto");
            }
            labels = List.copyOf(labels);
            if (labels.stream().distinct().count() != labels.size()) {
                throw new IllegalArgumentException("Una opción aparece dos veces en el problema");
            }
        }
    }
    public ChoiceQualityVerdict {
        if (status == null || objective == null || issues == null || rationale == null || rationale.isBlank()) {
            throw new IllegalArgumentException("Veredicto de distractores incompleto");
        }
        issues = List.copyOf(issues);
        if (status != Status.NEEDS_CONTEXT && (objective == null || objective.isBlank())) {
            throw new IllegalArgumentException("No se puede juzgar sin objetivo pedagógico");
        }
        if ((status == Status.FAIL) != !issues.isEmpty()) {
            throw new IllegalArgumentException("El estado contradice los problemas de distractores");
        }
        var unique = new java.util.HashSet<String>();
        for (Issue issue : issues) {
            String identity = issue.kind() + "|" + issue.labels().stream().sorted().toList();
            if (!unique.add(identity)) throw new IllegalArgumentException("Problema de distractores duplicado");
        }
    }
}
