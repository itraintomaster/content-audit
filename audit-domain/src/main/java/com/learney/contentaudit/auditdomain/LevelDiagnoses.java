package com.learney.contentaudit.auditdomain;

import com.learney.contentaudit.auditdomain.coca.CocaBucketsLevelDiagnosis;
import com.learney.contentaudit.auditdomain.labs.LemmaAbsenceLevelDiagnosis;
import com.learney.contentaudit.auditdomain.lemmacount.LemmaCountLevelDiagnosis;
import java.util.Optional;
public non-sealed interface LevelDiagnoses extends NodeDiagnoses {
    Optional<LemmaAbsenceLevelDiagnosis> getLemmaAbsenceDiagnosis();
    Optional<CocaBucketsLevelDiagnosis> getCocaBucketsDiagnosis();
    Optional<LemmaCountLevelDiagnosis> getLemmaCountDiagnosis();
}
