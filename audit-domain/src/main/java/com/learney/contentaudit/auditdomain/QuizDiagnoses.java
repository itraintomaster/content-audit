package com.learney.contentaudit.auditdomain;

import com.learney.contentaudit.auditdomain.labs.LemmaPlacementDiagnosis;
import java.util.Optional;
public non-sealed interface QuizDiagnoses extends NodeDiagnoses {
    Optional<LemmaPlacementDiagnosis> getLemmaAbsenceDiagnosis();
    Optional<SentenceLengthDiagnosis> getSentenceLengthDiagnosis();
}
