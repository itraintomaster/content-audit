package com.learney.contentaudit.auditdomain;

import com.learney.contentaudit.auditdomain.labs.LemmaPlacementDiagnosis;
import java.util.Optional;

public final class DefaultQuizDiagnoses implements QuizDiagnoses {

    private LemmaPlacementDiagnosis lemmaAbsenceDiagnosis;

    private SentenceLengthDiagnosis sentenceLengthDiagnosis;

    @Override
    public Optional<LemmaPlacementDiagnosis> getLemmaAbsenceDiagnosis() {
        return Optional.ofNullable(lemmaAbsenceDiagnosis);
    }

    public void setLemmaAbsenceDiagnosis(LemmaPlacementDiagnosis diagnosis) {
        this.lemmaAbsenceDiagnosis = diagnosis;
    }

    @Override
    public Optional<SentenceLengthDiagnosis> getSentenceLengthDiagnosis() {
        return Optional.ofNullable(sentenceLengthDiagnosis);
    }

    public void setSentenceLengthDiagnosis(SentenceLengthDiagnosis diagnosis) {
        this.sentenceLengthDiagnosis = diagnosis;
    }
}
