package com.learney.contentaudit.auditdomain;

import com.learney.contentaudit.auditdomain.labs.LemmaAbsenceLevelDiagnosis;
import java.util.Optional;

public final class DefaultLevelDiagnoses implements LevelDiagnoses {

    private LemmaAbsenceLevelDiagnosis lemmaAbsenceDiagnosis;

    @Override
    public Optional<LemmaAbsenceLevelDiagnosis> getLemmaAbsenceDiagnosis() {
        return Optional.ofNullable(lemmaAbsenceDiagnosis);
    }

    public void setLemmaAbsenceDiagnosis(LemmaAbsenceLevelDiagnosis diagnosis) {
        this.lemmaAbsenceDiagnosis = diagnosis;
    }
}
