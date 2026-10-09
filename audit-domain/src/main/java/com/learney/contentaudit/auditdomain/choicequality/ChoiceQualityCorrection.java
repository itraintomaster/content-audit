package com.learney.contentaudit.auditdomain.choicequality;

import com.learney.contentaudit.auditdomain.AnalyzerCorrection;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditdomain.finding.*;
import java.util.List;
import java.util.Optional;

/** The panel can clarify visible instructions, but cannot rewrite theory or change the quiz form. */
public final class ChoiceQualityCorrection implements AnalyzerCorrection {
    @Override public String analyzerName() { return ChoiceQualityAnalyzerProvider.NAME; }
    @Override public List<ExercisePart> editScope(FindingResolution resolution) {
        return resolution == FindingResolution.PANEL
                ? List.of(ExercisePart.OPTIONS, ExercisePart.INSTRUCTIONS, ExercisePart.STATEMENT,
                        ExercisePart.TRANSLATION, ExercisePart.STORED_SENTENCE, ExercisePart.AUDIO)
                : List.of();
    }
    @Override public Optional<String> coveringAnalyzer() { return Optional.empty(); }
    @Override public boolean groupsByTopic() { return false; }
    @Override public List<String> tieRules() { return List.of(); }
    @Override public boolean offersTwoFixes() { return false; }
    @Override public boolean concernsAudio(Finding finding) { return false; }
}
