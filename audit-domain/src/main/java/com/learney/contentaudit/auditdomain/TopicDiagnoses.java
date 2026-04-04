package com.learney.contentaudit.auditdomain;

import com.learney.contentaudit.auditdomain.labs.LemmaPlacementDiagnosis;
import java.util.Optional;
public non-sealed interface TopicDiagnoses extends NodeDiagnoses {
    Optional<LemmaPlacementDiagnosis> getLemmaAbsenceDiagnosis();
}
