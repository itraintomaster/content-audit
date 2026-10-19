package com.learney.contentaudit.auditdomain.englishquality;

public interface EnglishQualityVerdictReader {
    NativeUsageVerdict readNaturalness(String payload);
    A1DidacticVerdict readDidactic(String payload);
}
