package com.learney.contentaudit.auditdomain.englishquality;

import com.learney.contentaudit.auditdomain.AnalyzerCorrection;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import java.util.List;
import java.util.Optional;

/**
 * F-MESA-R024: what the judge of A1 finds in an example is corrected at the panel, changing the example -- its statement,
 * hint, answers or options, with its translation, stored sentence and audio following --, never the instructions of the
 * topic. The mini theory and the instructions of the exercise follow too: in the C2 of A1 (28/10) «The boy ___ (catch)
 * the bus.» in «Cambios ortográficos» became «study → studied», and its card still said «$catch$ es irregular». A selected
 * exercise it has not opined on is measured before it is corrected.
 */
public final class A1DidacticCorrection implements AnalyzerCorrection {
    static final String TASK_KIND = "A1_DIDACTIC";

    private static final List<ExercisePart> PANEL_SCOPE = List.of(ExercisePart.STATEMENT, ExercisePart.HINT,
            ExercisePart.ANSWERS, ExercisePart.OPTIONS, ExercisePart.TRANSLATION, ExercisePart.STORED_SENTENCE,
            ExercisePart.AUDIO, ExercisePart.MINI_THEORY, ExercisePart.INSTRUCTIONS);

    @Override public String analyzerName() { return EnglishQualityAnalyzerProvider.Kind.A1_DIDACTIC.name; }
    /** It never measured the course: in the topics that are corrected, it measures each exercise first. */
    @Override public boolean measuredBeforeCorrecting() { return true; }
    @Override public List<ExercisePart> editScope(FindingResolution resolution) {
        return resolution == FindingResolution.PANEL ? PANEL_SCOPE : List.of();
    }
    @Override public Optional<String> coveringAnalyzer() { return Optional.empty(); }
    @Override public boolean groupsByTopic() { return false; }
    @Override public List<String> tieRules() { return List.of(); }
    @Override public boolean offersTwoFixes() { return false; }
    @Override public boolean concernsAudio(Finding finding) { return false; }
}
