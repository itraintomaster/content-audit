package com.learney.contentaudit.revisiondomain.comparisonengine;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.contextnumbers.AnalyzerErrorCounts;
import com.learney.contentaudit.auditdomain.contextnumbers.ErrorCounts;
import com.learney.contentaudit.auditdomain.contextnumbers.SeverityCounts;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.revisiondomain.projection.ProjectedChange;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The audio follows the exercise, never the other way round (orchestrator, 1/10; F-NOEMP-R004, F-TANDA-R012). When a
 * pending change of the scope rewrites what an exercise says, its audio and the sentence production keeps go on saying
 * the old one until José records the audio again and takes the course to production. data-coherence marks both, but
 * they are copies waiting for their step, not errors the change brought: counted as errors, no change that rewrites the
 * English of an exercise with audio would ever pass -- «I love vegetables.» raised by one the exercises with an error
 * of its topic, its level and the course.
 *
 * <p>In a photo with the changes, the findings of data-coherence about the audio or the production copy of an exercise
 * a change rewrote, that the photo without them did not have, become ranking only -- they do not count -- and the
 * numbers of errors of the exercise and of every node above it are counted again without them, as the engine counts
 * them: the exercises with an error of data-coherence, with «algún error» and by their highest severity, and the marked
 * ones. Nothing else moves. They travel apart, by exercise, so the comparison can say what is left pending.
 */
final class PendingCopies {

    static final String DATA_COHERENCE = "data-coherence";

    private static final int LISTED = 10;

    private PendingCopies() {
    }

    /** A finding of data-coherence about a copy outside the course: the audio, or the sentence production keeps. */
    static boolean isPendingCopy(Finding finding) {
        return isAudio(finding) || isProduction(finding);
    }

    /** The audio that no longer says what the exercise says: the mark {@code audio}, not «audio-compartido». */
    static boolean isAudio(Finding finding) {
        return marks(finding, "audio");
    }

    /** The sentence production keeps, which no longer is the exercise: the mark {@code produccion}. */
    static boolean isProduction(Finding finding) {
        return marks(finding, "produccion");
    }

    /**
     * The mark itself, or with the number data-coherence adds to tell two of an exercise apart ({@code audio (2)}). A
     * shared key, an exercise without key or a recording nobody knows are not a copy the change left behind: a change
     * does not bring them by rewriting what the exercise says, so they count.
     */
    private static boolean marks(Finding finding, String mark) {
        String marker = markerOf(finding);
        return marker != null && (marker.equals(mark) || marker.startsWith(mark + " ("));
    }

    private static String markerOf(Finding finding) {
        if (finding == null || !DATA_COHERENCE.equals(finding.getAnalyzer()) || finding.getIdentity() == null) {
            return null;
        }
        return finding.getIdentity().getMarker();
    }

    /** The identity of every finding of a photo. */
    static Set<String> keysOf(AuditReport report) {
        Set<String> keys = new HashSet<>();
        for (AuditNode node : ReportTree.index(report).values()) {
            for (Finding finding : list(node.getFindings())) {
                if (finding != null) {
                    keys.add(DefaultBatchComparator.identity(finding));
                }
            }
        }
        return keys;
    }

    /**
     * Settles, in the photo with the changes, the pending copies of the exercises they rewrote against the photo
     * without them; returns them by exercise. Settling twice changes nothing.
     */
    static Map<String, List<Finding>> settle(AuditReport start, AuditReport projected, List<ProjectedChange> changes) {
        if (start == null || projected == null || start == projected) {
            return new LinkedHashMap<>();
        }
        return settle(keysOf(start), projected, changes);
    }

    static Map<String, List<Finding>> settle(Set<String> startKeys, AuditReport projected,
            List<ProjectedChange> changes) {
        Map<String, List<Finding>> byExercise = new LinkedHashMap<>();
        if (projected == null || projected.getRoot() == null || changes == null || changes.isEmpty()) {
            return byExercise;
        }
        Map<String, AuditNode> index = ReportTree.index(projected);
        List<String> errorAnalyzers = errorAnalyzers(projected);
        Set<String> seen = new HashSet<>();
        for (ProjectedChange change : changes) {
            if (change == null || change.getNodeTarget() == null || change.getNodeId() == null) {
                continue;
            }
            for (AuditNode quiz : quizzesUnder(index.get(ReportTree.key(change.getNodeTarget(), change.getNodeId())))) {
                String quizId = ReportTree.idOf(quiz);
                if (quizId == null || !seen.add(quizId)) {
                    continue;
                }
                List<Finding> pending = new ArrayList<>();
                for (Finding finding : list(quiz.getFindings())) {
                    if (isPendingCopy(finding) && finding.getResolution() != FindingResolution.RANK_ONLY
                            && (startKeys == null || !startKeys.contains(DefaultBatchComparator.identity(finding)))) {
                        pending.add(finding);
                    }
                }
                if (pending.isEmpty()) {
                    continue;
                }
                Tally before = Tally.of(quiz, errorAnalyzers);
                for (Finding finding : pending) {
                    finding.setResolution(FindingResolution.RANK_ONLY);
                }
                Tally after = Tally.of(quiz, errorAnalyzers);
                byExercise.put(quizId, pending);
                for (AuditNode node = quiz; node != null; node = node.getParent()) {
                    recount(node, before, after);
                }
            }
        }
        return byExercise;
    }

    /** What the comparison says of the copies left pending, or null when there are none. */
    static String describe(Map<String, List<Finding>> byExercise) {
        Set<String> audio = new LinkedHashSet<>();
        Set<String> production = new LinkedHashSet<>();
        for (Map.Entry<String, List<Finding>> exercise : byExercise != null ? byExercise.entrySet()
                : Map.<String, List<Finding>>of().entrySet()) {
            for (Finding finding : list(exercise.getValue())) {
                if (isAudio(finding)) {
                    audio.add(exercise.getKey());
                } else if (isProduction(finding)) {
                    production.add(exercise.getKey());
                }
            }
        }
        List<String> parts = new ArrayList<>();
        if (!audio.isEmpty()) {
            parts.add("Pendiente de regrabar, sin contar como error: " + audio.size()
                    + (audio.size() == 1 ? " audio (" : " audios (") + listed(audio) + ").");
        }
        if (!production.isEmpty()) {
            parts.add("Por llevar a producción, sin contar como error: " + production.size()
                    + (production.size() == 1 ? " oración guardada (" : " oraciones guardadas (") + listed(production)
                    + ").");
        }
        return parts.isEmpty() ? null : String.join(" ", parts);
    }

    // -------------------------------------------------------------------------
    // The numbers of errors, counted again
    // -------------------------------------------------------------------------

    /** The analyzers of errors the photo counted: the ones its root lists, as the engine's catalog gave them. */
    private static List<String> errorAnalyzers(AuditReport report) {
        List<String> names = new ArrayList<>();
        AuditNode root = report.getRoot();
        ErrorCounts errors = root.getNumbers() != null ? root.getNumbers().getErrors() : null;
        for (AnalyzerErrorCounts counts : errors != null ? list(errors.getAnalyzers()) : List.<AnalyzerErrorCounts>of()) {
            if (counts != null && counts.getAnalyzer() != null) {
                names.add(counts.getAnalyzer());
            }
        }
        return names;
    }

    /**
     * How an exercise counts, as the engine counts it (DefaultContextNumbersCalculator): for each analyzer of errors
     * that did not leave it unevaluated, a finding of its own or of a node above that does more than order is an
     * error, and one that only orders marks it; «algún error» is any of them, with the highest severity of theirs.
     */
    private record Tally(Set<String> withError, Set<String> marked, FindingSeverity highest) {

        static Tally of(AuditNode quiz, List<String> analyzers) {
            List<AuditNode> chain = new ArrayList<>();
            for (AuditNode node = quiz; node != null; node = node.getParent()) {
                chain.add(node);
            }
            Set<String> withError = new LinkedHashSet<>();
            Set<String> marked = new LinkedHashSet<>();
            FindingSeverity highest = null;
            for (String analyzer : analyzers) {
                boolean unevaluated = false;
                for (AuditNode node : chain) {
                    if (node.getUnevaluatedBy() != null && node.getUnevaluatedBy().contains(analyzer)) {
                        unevaluated = true;
                    }
                }
                if (unevaluated) {
                    continue;
                }
                for (AuditNode node : chain) {
                    for (Finding finding : list(node.getFindings())) {
                        if (finding == null || !analyzer.equals(finding.getAnalyzer())) {
                            continue;
                        }
                        if (finding.getResolution() == FindingResolution.RANK_ONLY) {
                            marked.add(analyzer);
                        } else {
                            withError.add(analyzer);
                            highest = higher(highest, finding.getSeverity());
                        }
                    }
                }
            }
            return new Tally(withError, marked, highest);
        }
    }

    /** The counts of a node, with the exercise as it counts after the settling instead of before. */
    private static void recount(AuditNode node, Tally before, Tally after) {
        ErrorCounts errors = node.getNumbers() != null ? node.getNumbers().getErrors() : null;
        if (errors == null) {
            return;
        }
        for (AnalyzerErrorCounts counts : list(errors.getAnalyzers())) {
            if (counts == null || counts.getAnalyzer() == null) {
                continue;
            }
            String analyzer = counts.getAnalyzer();
            if (before.withError().contains(analyzer) && !after.withError().contains(analyzer)) {
                counts.setWithError(Math.max(0, counts.getWithError() - 1));
                counts.setErrorShare(counts.getEvaluated() > 0
                        ? (double) counts.getWithError() / counts.getEvaluated() : 0.0);
            }
            if (!before.marked().contains(analyzer) && after.marked().contains(analyzer)) {
                counts.setMarked(counts.getMarked() + 1);
            }
        }
        boolean wasError = !before.withError().isEmpty();
        boolean isError = !after.withError().isEmpty();
        if (wasError && !isError) {
            errors.setWithAnyError(Math.max(0, errors.getWithAnyError() - 1));
            errors.setAnyErrorShare(errors.getQuizzes() > 0
                    ? (double) errors.getWithAnyError() / errors.getQuizzes() : 0.0);
            move(errors, before.highest(), null);
        } else if (wasError && before.highest() != after.highest()) {
            move(errors, before.highest(), after.highest());
        }
    }

    /** One exercise less of a severity and, when it still has an error, one more of its new highest. */
    private static void move(ErrorCounts errors, FindingSeverity from, FindingSeverity to) {
        SeverityCounts counts = errors.getBySeverity();
        if (counts == null) {
            return;
        }
        if (from != null) {
            switch (from) {
                case BLOCKING -> counts.setBlocking(Math.max(0, counts.getBlocking() - 1));
                case HIGH -> counts.setHigh(Math.max(0, counts.getHigh() - 1));
                case MEDIUM -> counts.setMedium(Math.max(0, counts.getMedium() - 1));
                case LOW -> counts.setLow(Math.max(0, counts.getLow() - 1));
            }
        }
        if (to != null) {
            switch (to) {
                case BLOCKING -> counts.setBlocking(counts.getBlocking() + 1);
                case HIGH -> counts.setHigh(counts.getHigh() + 1);
                case MEDIUM -> counts.setMedium(counts.getMedium() + 1);
                case LOW -> counts.setLow(counts.getLow() + 1);
            }
        }
    }

    /** BLOCKING is the highest severity and LOW the lowest (the order of the enum), as the engine reads it. */
    private static FindingSeverity higher(FindingSeverity a, FindingSeverity b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.ordinal() <= b.ordinal() ? a : b;
    }

    // -------------------------------------------------------------------------

    private static List<AuditNode> quizzesUnder(AuditNode node) {
        List<AuditNode> quizzes = new ArrayList<>();
        collect(node, quizzes);
        return quizzes;
    }

    private static void collect(AuditNode node, List<AuditNode> quizzes) {
        if (node == null) {
            return;
        }
        if (node.getTarget() == AuditTarget.QUIZ) {
            quizzes.add(node);
            return;
        }
        for (AuditNode child : list(node.getChildren())) {
            collect(child, quizzes);
        }
    }

    private static String listed(Set<String> ids) {
        List<String> all = new ArrayList<>(ids);
        if (all.size() <= LISTED) {
            return String.join(", ", all);
        }
        return String.join(", ", all.subList(0, LISTED)) + " y " + (all.size() - LISTED) + " más";
    }

    private static <T> List<T> list(List<T> values) {
        return values != null ? values : List.of();
    }
}
