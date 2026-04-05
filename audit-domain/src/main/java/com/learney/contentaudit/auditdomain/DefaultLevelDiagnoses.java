package com.learney.contentaudit.auditdomain;

import com.learney.contentaudit.auditdomain.coca.CocaBucketsLevelDiagnosis;
import com.learney.contentaudit.auditdomain.labs.LemmaAbsenceLevelDiagnosis;
import java.util.Optional;

public final class DefaultLevelDiagnoses implements LevelDiagnoses {

    private LemmaAbsenceLevelDiagnosis lemmaAbsenceDiagnosis;

    private CocaBucketsLevelDiagnosis cocaBucketsDiagnosis;

    @Override
    public Optional<LemmaAbsenceLevelDiagnosis> getLemmaAbsenceDiagnosis() {
        return Optional.ofNullable(lemmaAbsenceDiagnosis);
    }

    public void setLemmaAbsenceDiagnosis(LemmaAbsenceLevelDiagnosis diagnosis) {
        this.lemmaAbsenceDiagnosis = diagnosis;
    }

    @Override
    public Optional<CocaBucketsLevelDiagnosis> getCocaBucketsDiagnosis() {
        return Optional.ofNullable(cocaBucketsDiagnosis);
    }

    public void setCocaBucketsDiagnosis(CocaBucketsLevelDiagnosis diagnosis) {
        this.cocaBucketsDiagnosis = diagnosis;
    }
}
