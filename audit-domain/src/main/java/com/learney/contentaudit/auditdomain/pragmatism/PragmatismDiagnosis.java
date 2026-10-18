package com.learney.contentaudit.auditdomain.pragmatism;

/** Preserves explicit abstention and the effective judge version as well as successful judgments. */
public record PragmatismDiagnosis(PragmatismVerdict verdict, String evaluatorVersion, boolean reused) { }
