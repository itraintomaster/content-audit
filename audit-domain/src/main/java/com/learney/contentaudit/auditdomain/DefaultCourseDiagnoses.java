package com.learney.contentaudit.auditdomain;

import com.learney.contentaudit.auditdomain.coca.CocaProgressionDiagnosis;
import com.learney.contentaudit.auditdomain.judging.JudgeCoverageDiagnosis;
import com.learney.contentaudit.auditdomain.labs.LemmaAbsenceCourseDiagnosis;
import com.learney.contentaudit.auditdomain.lemmacount.LemmaCountCourseDiagnosis;
import com.learney.contentaudit.auditdomain.localtools.LocalToolRunDiagnosis;
import com.learney.contentaudit.auditdomain.lrec.LemmaRecurrenceCourseDiagnosis;
import com.learney.contentaudit.auditdomain.naturalness.NaturalnessQueueDiagnosis;
import com.learney.contentaudit.auditdomain.quizinstruction.QuizInstructionCoverageDiagnosis;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DefaultCourseDiagnoses implements CourseDiagnoses {

    private LemmaAbsenceCourseDiagnosis lemmaAbsenceDiagnosis;

    private CocaProgressionDiagnosis cocaBucketsDiagnosis;

    private LemmaCountCourseDiagnosis lemmaCountDiagnosis;

    private QuizInstructionCoverageDiagnosis quizInstructionCoverage;

    // FEAT-RONDA1 (hook C-04): the coverage of every judge that ran, in the order they
    // published it; each judge adds its own and never touches another's.
    private List<JudgeCoverageDiagnosis> judgeCoverages = new ArrayList<>();

    // FEAT-RONDA1 (hook D-10)
    private LemmaRecurrenceCourseDiagnosis lemmaRecurrenceDiagnosis;

    // FEAT-RONDA2 (hook B-03): the run of every local tool that ran, in the order they published
    // it; each analyzer of package B adds its own and never touches another's.
    private List<LocalToolRunDiagnosis> localToolRuns = new ArrayList<>();

    // FEAT-RONDA2 (hook B-03)
    private NaturalnessQueueDiagnosis naturalnessQueue;

    @Override
    public Optional<LemmaAbsenceCourseDiagnosis> getLemmaAbsenceDiagnosis() {
        return Optional.ofNullable(lemmaAbsenceDiagnosis);
    }

    public void setLemmaAbsenceDiagnosis(LemmaAbsenceCourseDiagnosis diagnosis) {
        this.lemmaAbsenceDiagnosis = diagnosis;
    }

    @Override
    public Optional<CocaProgressionDiagnosis> getCocaBucketsDiagnosis() {
        return Optional.ofNullable(cocaBucketsDiagnosis);
    }

    public void setCocaBucketsDiagnosis(CocaProgressionDiagnosis diagnosis) {
        this.cocaBucketsDiagnosis = diagnosis;
    }

    @Override
    public Optional<LemmaCountCourseDiagnosis> getLemmaCountDiagnosis() {
        return Optional.ofNullable(lemmaCountDiagnosis);
    }

    public void setLemmaCountDiagnosis(LemmaCountCourseDiagnosis diagnosis) {
        this.lemmaCountDiagnosis = diagnosis;
    }

    @Override
    public Optional<QuizInstructionCoverageDiagnosis> getQuizInstructionCoverage() {
        return Optional.ofNullable(quizInstructionCoverage);
    }

    public void setQuizInstructionCoverage(QuizInstructionCoverageDiagnosis diagnosis) {
        this.quizInstructionCoverage = diagnosis;
    }

    @Override
    public List<JudgeCoverageDiagnosis> getJudgeCoverages() {
        return judgeCoverages;
    }

    /** Needed to read the list back from a stored report. */
    public void setJudgeCoverages(List<JudgeCoverageDiagnosis> judgeCoverages) {
        this.judgeCoverages = judgeCoverages != null ? new ArrayList<>(judgeCoverages) : new ArrayList<>();
    }

    /** What a judge calls to publish its coverage of the run. */
    public void addJudgeCoverage(JudgeCoverageDiagnosis coverage) {
        if (coverage != null) {
            judgeCoverages.add(coverage);
        }
    }

    @Override
    public Optional<LemmaRecurrenceCourseDiagnosis> getLemmaRecurrenceDiagnosis() {
        return Optional.ofNullable(lemmaRecurrenceDiagnosis);
    }

    public void setLemmaRecurrenceDiagnosis(LemmaRecurrenceCourseDiagnosis diagnosis) {
        this.lemmaRecurrenceDiagnosis = diagnosis;
    }

    @Override
    public List<LocalToolRunDiagnosis> getLocalToolRuns() {
        return localToolRuns;
    }

    /** Needed to read the list back from a stored report. */
    public void setLocalToolRuns(List<LocalToolRunDiagnosis> localToolRuns) {
        this.localToolRuns = localToolRuns != null ? new ArrayList<>(localToolRuns) : new ArrayList<>();
    }

    /** What an analyzer of package B calls to publish the run of its local tool. */
    public void addLocalToolRun(LocalToolRunDiagnosis run) {
        if (run != null) {
            localToolRuns.add(run);
        }
    }

    @Override
    public Optional<NaturalnessQueueDiagnosis> getNaturalnessQueue() {
        return Optional.ofNullable(naturalnessQueue);
    }

    public void setNaturalnessQueue(NaturalnessQueueDiagnosis queue) {
        this.naturalnessQueue = queue;
    }
}
