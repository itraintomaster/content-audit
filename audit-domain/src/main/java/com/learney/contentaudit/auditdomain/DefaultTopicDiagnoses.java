package com.learney.contentaudit.auditdomain;

import com.learney.contentaudit.auditdomain.coca.CocaBucketsTopicDiagnosis;
import com.learney.contentaudit.auditdomain.labs.LemmaPlacementDiagnosis;
import java.util.Optional;

public final class DefaultTopicDiagnoses implements TopicDiagnoses {

    private LemmaPlacementDiagnosis lemmaAbsenceDiagnosis;

    private CocaBucketsTopicDiagnosis cocaBucketsDiagnosis;

    @Override
    public Optional<LemmaPlacementDiagnosis> getLemmaAbsenceDiagnosis() {
        return Optional.ofNullable(lemmaAbsenceDiagnosis);
    }

    public void setLemmaAbsenceDiagnosis(LemmaPlacementDiagnosis diagnosis) {
        this.lemmaAbsenceDiagnosis = diagnosis;
    }

    @Override
    public Optional<CocaBucketsTopicDiagnosis> getCocaBucketsDiagnosis() {
        return Optional.ofNullable(cocaBucketsDiagnosis);
    }

    public void setCocaBucketsDiagnosis(CocaBucketsTopicDiagnosis diagnosis) {
        this.cocaBucketsDiagnosis = diagnosis;
    }
}
