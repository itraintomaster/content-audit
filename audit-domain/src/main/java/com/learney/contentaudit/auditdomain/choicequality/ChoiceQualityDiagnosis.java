package com.learney.contentaudit.auditdomain.choicequality;

/** Preserved even for NEEDS_CONTEXT, so a human can inspect why the judge abstained. */
public record ChoiceQualityDiagnosis(ChoiceQualityVerdict verdict, String evaluatorVersion, boolean reused) { }
