package com.learney.contentaudit.auditdomain;

import com.learney.contentaudit.auditdomain.labs.LemmaPlacementDiagnosis;
import java.util.Optional;

public final class DefaultQuizDiagnoses implements QuizDiagnoses {

    private LemmaPlacementDiagnosis lemmaAbsenceDiagnosis;

    @Override
    public Optional<LemmaPlacementDiagnosis> getLemmaAbsenceDiagnosis() {
        return Optional.ofNullable(lemmaAbsenceDiagnosis);
    }

    public void setLemmaAbsenceDiagnosis(LemmaPlacementDiagnosis diagnosis) {
        this.lemmaAbsenceDiagnosis = diagnosis;
    }
}
