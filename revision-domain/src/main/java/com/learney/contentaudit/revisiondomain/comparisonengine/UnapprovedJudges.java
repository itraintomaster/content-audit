package com.learney.contentaudit.revisiondomain.comparisonengine;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.CourseDiagnoses;
import com.learney.contentaudit.auditdomain.judging.JudgeCoverageDiagnosis;
import com.learney.contentaudit.auditdomain.judging.JudgeExamStatus;
import com.learney.contentaudit.auditdomain.localtools.LocalToolRunDiagnosis;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * F-CJUEZ-R004: the judges of a photo whose exam is not approved -- the ones that do not consult on the course -- and
 * what each of them reads. A judge of the catalog says it in its coverage: its version consults on the course or it does
 * not. A local tool without coverage of judge, as grammar, says it in the run of its tool: an exam of its version that is
 * not approved; a tool that is not installed, or that goes by its claude -p alternative, has no exam to fail and is not
 * here -- its exercises are already left unevaluated, as always.
 *
 * <p>A judge of exercises reads an exercise when it left a mark on it -- a score, a «not evaluated» or an evaluation in
 * part --; a judge of topics, when it left it on the topic of the exercise or on the exercise itself: the coverage of
 * near-duplicates counts topics -- it compares the exercises of one theme --, but, as the real analyzer does, it marks
 * the exercises it compares, never the theme. A judge that left none reads nothing the batch changed, so it blocks
 * nothing: one that marks only the exercises of another topic, with no change, is not read.
 */
final class UnapprovedJudges {

    /** Each judge without an approved exam, with the kind of node it judges, in the order the photo published them. */
    private final Map<String, AuditTarget> units;

    private UnapprovedJudges(Map<String, AuditTarget> units) {
        this.units = units;
    }

    /** The judges of the photo that do not consult on the course. */
    static UnapprovedJudges of(AuditReport photo) {
        Map<String, AuditTarget> units = new LinkedHashMap<>();
        if (photo != null && photo.getRoot() != null && photo.getRoot().getDiagnoses() instanceof CourseDiagnoses root) {
            Set<String> withCoverage = new HashSet<>();
            for (JudgeCoverageDiagnosis coverage : listOrEmpty(root.getJudgeCoverages())) {
                if (coverage == null || coverage.getJudgeName() == null) {
                    continue;
                }
                withCoverage.add(coverage.getJudgeName());
                if (coverage.getStanding() != null && !coverage.getStanding().isConsultsOnCourse()) {
                    units.put(coverage.getJudgeName(), coverage.getUnit() != null ? coverage.getUnit()
                            : AuditTarget.QUIZ);
                }
            }
            for (LocalToolRunDiagnosis run : listOrEmpty(root.getLocalToolRuns())) {
                if (run != null && run.getAnalyzer() != null && !withCoverage.contains(run.getAnalyzer())
                        && run.getExamState() != null && run.getExamState() != JudgeExamStatus.APPROVED) {
                    units.put(run.getAnalyzer(), AuditTarget.QUIZ);
                }
            }
        }
        return new UnapprovedJudges(units);
    }

    boolean isEmpty() {
        return units.isEmpty();
    }

    /**
     * The judges without an approved exam that read this exercise, in the order of the photo: the ones that marked the
     * exercise and, among the judges of topics, the ones that marked its topic.
     */
    Set<String> reading(AuditNode quiz) {
        Set<String> readers = new LinkedHashSet<>();
        AuditNode topic = quiz.ancestor(AuditTarget.KNOWLEDGE).orElse(null);
        for (Map.Entry<String, AuditTarget> judge : units.entrySet()) {
            boolean reads = marks(quiz, judge.getKey())
                    || judge.getValue() == AuditTarget.KNOWLEDGE && topic != null && marks(topic, judge.getKey());
            if (reads) {
                readers.add(judge.getKey());
            }
        }
        return readers;
    }

    private static boolean marks(AuditNode node, String judge) {
        return node.getScores() != null && node.getScores().containsKey(judge)
                || listOrEmpty(node.getUnevaluatedBy()).contains(judge)
                || listOrEmpty(node.getPartiallyEvaluatedBy()).contains(judge);
    }

    private static <T> List<T> listOrEmpty(List<T> list) {
        return list != null ? list : List.of();
    }
}
