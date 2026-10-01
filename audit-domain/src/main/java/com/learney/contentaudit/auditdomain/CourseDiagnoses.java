package com.learney.contentaudit.auditdomain;

import com.learney.contentaudit.auditdomain.coca.CocaProgressionDiagnosis;
import com.learney.contentaudit.auditdomain.judging.JudgeCoverageDiagnosis;
import com.learney.contentaudit.auditdomain.labs.LemmaAbsenceCourseDiagnosis;
import com.learney.contentaudit.auditdomain.lemmacount.LemmaCountCourseDiagnosis;
import com.learney.contentaudit.auditdomain.localtools.LocalToolRunDiagnosis;
import com.learney.contentaudit.auditdomain.lrec.LemmaRecurrenceCourseDiagnosis;
import com.learney.contentaudit.auditdomain.naturalness.NaturalnessQueueDiagnosis;
import com.learney.contentaudit.auditdomain.quizinstruction.QuizInstructionCoverageDiagnosis;
import java.util.List;
import java.util.Optional;

/**
 * The typed place of every analyzer that diagnoses the course. FEAT-RONDA1 adds the common
 * coverage of the judges -- one list for all of them, each entry naming its judge (hook C-04,
 * which replaced A-05) -- and the slot of lemma-recurrence (hook D-10). FEAT-RONDA2 adds the common
 * list of the runs of the local tools -- one for the three of package B, each entry naming its
 * analyzer -- and the queue of naturalness per level (hook B-03).
 */
public non-sealed interface CourseDiagnoses extends NodeDiagnoses {
    Optional<LemmaAbsenceCourseDiagnosis> getLemmaAbsenceDiagnosis();
    Optional<CocaProgressionDiagnosis> getCocaBucketsDiagnosis();
    Optional<LemmaCountCourseDiagnosis> getLemmaCountDiagnosis();
    Optional<QuizInstructionCoverageDiagnosis> getQuizInstructionCoverage();
    List<JudgeCoverageDiagnosis> getJudgeCoverages();
    Optional<LemmaRecurrenceCourseDiagnosis> getLemmaRecurrenceDiagnosis();
    List<LocalToolRunDiagnosis> getLocalToolRuns();
    Optional<NaturalnessQueueDiagnosis> getNaturalnessQueue();
}
