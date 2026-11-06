package com.learney.contentaudit.revisiondomain.comparisonengine;

import com.learney.contentaudit.auditdomain.AnalyzerCatalog;
import com.learney.contentaudit.auditdomain.AnalyzerDescriptor;
import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerFamily;
import com.learney.contentaudit.auditdomain.contextnumbers.AnalyzerErrorCounts;
import com.learney.contentaudit.auditdomain.contextnumbers.ErrorCounts;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * F-NOEMP-R005, F-METAS-R003 (José, 5/11, brief 275): at closing a finding of the errors family counts as an error by
 * the contract of the evaluators (F-CONTR-R002), as the board counts it -- from medium severity up, or without a known
 * one; what only ranks counts by its severity; the sentence production keeps is not the exercise's. The numbers each
 * report publishes count every finding that does more than rank, as the engine counts them, and the Mapa shows them so;
 * the comparison counts its rows of errors again by the contract, exercise by exercise, without touching the reports.
 */
final class ClosingErrors {

    /** F-CONTR-R002: from this severity up a finding stops the closing. */
    private static final FindingSeverity THRESHOLD = FindingSeverity.MEDIUM;

    /** Per node of every report counted: how many exercises with an error the contract adds, or takes, per subject. */
    private final Map<AuditNode, Map<String, Integer>> delta = new IdentityHashMap<>();

    private final AnalyzerCatalog catalog;

    private ClosingErrors(AnalyzerCatalog catalog) {
        this.catalog = catalog;
    }

    /** A finding of the errors family that stops the closing. */
    static boolean counts(Finding finding) {
        if (finding == null || PendingCopies.isProduction(finding)) {
            return false;
        }
        // The audio a change left saying the old sentence was settled as ranking only: a copy to record, not an error.
        if (PendingCopies.isAudio(finding) && finding.getResolution() == FindingResolution.RANK_ONLY) {
            return false;
        }
        return finding.getSeverity() == null || finding.getSeverity().ordinal() <= THRESHOLD.ordinal();
    }

    /** The exercises of {@code reports} counted by the contract instead of by the engine. */
    static ClosingErrors of(AnalyzerCatalog catalog, AuditReport... reports) {
        ClosingErrors closing = new ClosingErrors(catalog);
        for (AuditReport report : reports) {
            if (report != null && report.getRoot() != null) {
                closing.count(report.getRoot());
            }
        }
        return closing;
    }

    /**
     * The number of a subject in a node, as the comparison reads it: the published one, with the exercises with an error
     * of an errors analyzer and «algún error» counted by the contract.
     */
    ReportTree.Value value(AuditNode node, String subject, AnalyzerFamily family) {
        ReportTree.Value published = ReportTree.value(node, subject, family);
        int change = node == null ? 0 : delta.getOrDefault(node, Map.of()).getOrDefault(subject, 0);
        if (published == null || published.value() == null || change == 0) {
            return published;
        }
        ErrorCounts errors = node.getNumbers().getErrors();
        double count = Math.max(0, published.value() + change);
        int over = DefaultGoalBook.ANY_ERROR.equals(subject) ? errors.getQuizzes() : evaluated(errors, subject);
        return new ReportTree.Value(count, over > 0 ? count / over : 0.0, published.notEvaluated(), published.total(),
                published.evaluated());
    }

    private static int evaluated(ErrorCounts errors, String analyzer) {
        for (AnalyzerErrorCounts counts : list(errors.getAnalyzers())) {
            if (counts != null && analyzer.equals(counts.getAnalyzer())) {
                return counts.getEvaluated();
            }
        }
        return 0;
    }

    // -------------------------------------------------------------------------

    private void count(AuditNode root) {
        List<String> analyzers = new ArrayList<>();
        ErrorCounts errors = root.getNumbers() != null ? root.getNumbers().getErrors() : null;
        for (AnalyzerErrorCounts counts : errors != null ? list(errors.getAnalyzers()) : List.<AnalyzerErrorCounts>of()) {
            if (counts != null && counts.getAnalyzer() != null) {
                analyzers.add(counts.getAnalyzer());
            }
        }
        if (!analyzers.isEmpty()) {
            walk(root, new ArrayList<>(), analyzers);
        }
    }

    private void walk(AuditNode node, List<AuditNode> above, List<String> analyzers) {
        if (node.getTarget() == AuditTarget.QUIZ) {
            exercise(node, above, analyzers);
            return;
        }
        above.add(node);
        for (AuditNode child : list(node.getChildren())) {
            if (child != null) {
                walk(child, above, analyzers);
            }
        }
        above.remove(above.size() - 1);
    }

    /**
     * How the exercise counts for the engine and for the contract, with the engine's reading
     * (DefaultContextNumbersCalculator): for each analyzer of errors, its findings on the exercise and on every node
     * above it at a target the analyzer evaluates; an analyzer that left it unevaluated, or never reached it, counts it
     * neither way. Where the two differ, the exercise and every node above it move by one.
     */
    private void exercise(AuditNode quiz, List<AuditNode> above, List<String> analyzers) {
        List<AuditNode> chain = new ArrayList<>(above);
        chain.add(quiz);
        Map<String, Integer> moves = new HashMap<>();
        boolean engineAny = false;
        boolean closingAny = false;
        for (String analyzer : analyzers) {
            Set<AuditTarget> targets = targets(analyzer);
            boolean reached = false;
            boolean unevaluated = false;
            boolean engine = false;
            boolean closing = false;
            for (AuditNode node : chain) {
                if (targets != null && !targets.contains(node.getTarget())) {
                    continue;
                }
                if (list(node.getUnevaluatedBy()).contains(analyzer)) {
                    unevaluated = true;
                }
                if (list(node.getPartiallyEvaluatedBy()).contains(analyzer)
                        || node.getScores() != null && node.getScores().containsKey(analyzer)) {
                    reached = true;
                }
                for (Finding finding : list(node.getFindings())) {
                    if (finding == null || !analyzer.equals(finding.getAnalyzer())) {
                        continue;
                    }
                    engine |= finding.getResolution() != FindingResolution.RANK_ONLY;
                    closing |= counts(finding);
                }
            }
            if (unevaluated || !reached) {
                continue;
            }
            engineAny |= engine;
            closingAny |= closing;
            if (engine != closing) {
                moves.put(analyzer, closing ? 1 : -1);
            }
        }
        if (engineAny != closingAny) {
            moves.put(DefaultGoalBook.ANY_ERROR, closingAny ? 1 : -1);
        }
        if (moves.isEmpty()) {
            return;
        }
        for (AuditNode node : chain) {
            Map<String, Integer> counts = delta.computeIfAbsent(node, n -> new HashMap<>());
            moves.forEach((subject, move) -> counts.merge(subject, move, Integer::sum));
        }
    }

    /** The targets the card of an analyzer evaluates; null, every one, when today's catalog does not know it. */
    private Set<AuditTarget> targets(String analyzer) {
        Optional<AnalyzerDescriptor> card = catalog != null ? catalog.find(analyzer) : Optional.empty();
        if (card == null || card.isEmpty() || card.get().getEvaluatedTargets() == null
                || card.get().getEvaluatedTargets().isEmpty()) {
            return null;
        }
        return Set.copyOf(card.get().getEvaluatedTargets());
    }

    private static <T> List<T> list(List<T> values) {
        return values != null ? values : List.of();
    }
}
