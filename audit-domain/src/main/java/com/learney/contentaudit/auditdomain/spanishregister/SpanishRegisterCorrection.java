package com.learney.contentaudit.auditdomain.spanishregister;

import com.learney.contentaudit.auditdomain.AnalyzerCorrection;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import java.util.List;
import java.util.Optional;

/**
 * F-MESA-R025: a text in «vos» is written again in «tú» at the panel -- the instructions, the mini theory or the
 * translation of the exercise --, never its English.
 */
public final class SpanishRegisterCorrection implements AnalyzerCorrection {
    private static final List<ExercisePart> PANEL_SCOPE = List.of(ExercisePart.INSTRUCTIONS, ExercisePart.MINI_THEORY,
            ExercisePart.TRANSLATION);

    @Override public String analyzerName() { return SpanishRegisterAnalyzer.ANALYZER_NAME; }
    @Override public List<ExercisePart> editScope(FindingResolution resolution) {
        return resolution == FindingResolution.PANEL ? PANEL_SCOPE : List.of();
    }
    @Override public Optional<String> coveringAnalyzer() { return Optional.empty(); }
    @Override public boolean groupsByTopic() { return false; }
    @Override public List<String> tieRules() { return List.of(); }
    @Override public boolean offersTwoFixes() { return false; }
    @Override public boolean concernsAudio(Finding finding) { return false; }
}
