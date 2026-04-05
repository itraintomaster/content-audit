package com.learney.contentaudit.auditdomain;

import com.learney.contentaudit.auditdomain.coca.CocaProgressionDiagnosis;
import com.learney.contentaudit.auditdomain.labs.LemmaAbsenceCourseDiagnosis;
import java.util.Optional;

public final class DefaultCourseDiagnoses implements CourseDiagnoses {

    private LemmaAbsenceCourseDiagnosis lemmaAbsenceDiagnosis;

    private CocaProgressionDiagnosis cocaBucketsDiagnosis;

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
}
