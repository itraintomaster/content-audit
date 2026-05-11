package com.learney.contentaudit.auditdomain;

import com.learney.contentaudit.auditdomain.coca.CocaProgressionDiagnosis;
import com.learney.contentaudit.auditdomain.labs.LemmaAbsenceCourseDiagnosis;
import com.learney.contentaudit.auditdomain.lemmacount.LemmaCountCourseDiagnosis;
import java.util.Optional;
public non-sealed interface CourseDiagnoses extends NodeDiagnoses {
    Optional<LemmaAbsenceCourseDiagnosis> getLemmaAbsenceDiagnosis();
    Optional<CocaProgressionDiagnosis> getCocaBucketsDiagnosis();
    Optional<LemmaCountCourseDiagnosis> getLemmaCountDiagnosis();
}
