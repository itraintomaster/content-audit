package com.learney.contentaudit.auditdomain;

import com.learney.contentaudit.auditdomain.labs.LemmaAbsenceLevelDiagnosis;
import java.util.Optional;
public non-sealed interface LevelDiagnoses extends NodeDiagnoses {
    Optional<LemmaAbsenceLevelDiagnosis> getLemmaAbsenceDiagnosis();
}
