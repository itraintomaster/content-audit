package com.learney.contentaudit.auditdomain.analysis;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.DefaultCourseDiagnoses;
import com.learney.contentaudit.auditdomain.judging.JudgeCoverageDiagnosis;
import com.learney.contentaudit.auditdomain.judging.JudgeExamStanding;
import com.learney.contentaudit.auditdomain.judging.JudgeVersionStanding;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The version of a judge an exercise was read with, when only its run says it. */
class RunVersionOfAJudgeTest {

    static final String JUDGE = "near-duplicates";
    static final String OLD = "nomic-embed-text@0a109f422b47 · clustering-1 · candidatos-0,80 · juez-04c290f901e3";
    static final String TODAY = "nomic-embed-text@0a109f422b47 · clustering-1 · candidatos-0,80 · juez-729229ed1d8d";

    @Test
    @Tag("FEAT-MIDIO")
    @Tag("F-MIDIO-R009")
    void shouldReadARunWithTwoVersionsItsJudgeNamesAsItsOwnAsTodaysVersionAndOneWithAnotherVersionAsNone() {
        // 5/11: the C2 measured near duplicates reusing 4 verdicts of juez-04c290… and 23 of juez-729229…, the same
        // version (F-ALDIA-R002); the exercises were left without a version and out of every comparison.
        AuditNode quiz = new AuditNode();
        quiz.setTarget(AuditTarget.QUIZ);

        assertEquals(TODAY, DefaultAnalysisComposer.version(quiz, JUDGE, run(Set.of(OLD, TODAY))),
                "one version, today's");
        assertNull(DefaultAnalysisComposer.version(quiz, JUDGE, run(Set.of(TODAY))),
                "a version today's judge does not name leaves the run without one");
    }

    /** A run that read 4 verdicts of the old version and 23 of today's, its judge naming {@code same} as its own. */
    static AuditReport run(Set<String> same) {
        JudgeExamStanding standing = new JudgeExamStanding();
        standing.setJudgeVersion(TODAY);
        standing.setCompatibleJudgeVersions(same);
        JudgeCoverageDiagnosis coverage = new JudgeCoverageDiagnosis();
        coverage.setJudgeName(JUDGE);
        coverage.setUnit(AuditTarget.KNOWLEDGE);
        coverage.setStanding(standing);
        coverage.setVersions(List.of(new JudgeVersionStanding(OLD, 4, null),
                new JudgeVersionStanding(TODAY, 23, null)));
        DefaultCourseDiagnoses diagnoses = new DefaultCourseDiagnoses();
        diagnoses.setJudgeCoverages(List.of(coverage));
        AuditNode root = new AuditNode();
        root.setTarget(AuditTarget.COURSE);
        root.setDiagnoses(diagnoses);
        AuditReport report = new AuditReport();
        report.setRoot(root);
        return report;
    }
}
