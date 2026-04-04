package com.learney.contentaudit.auditdomain;

import com.learney.contentaudit.auditdomain.labs.LemmaAbsenceCourseDiagnosis;
import java.util.Optional;

public final class DefaultCourseDiagnoses implements CourseDiagnoses {

    private LemmaAbsenceCourseDiagnosis lemmaAbsenceDiagnosis;

    @Override
    public Optional<LemmaAbsenceCourseDiagnosis> getLemmaAbsenceDiagnosis() {
        return Optional.ofNullable(lemmaAbsenceDiagnosis);
    }

    public void setLemmaAbsenceDiagnosis(LemmaAbsenceCourseDiagnosis diagnosis) {
        this.lemmaAbsenceDiagnosis = diagnosis;
    }
}
