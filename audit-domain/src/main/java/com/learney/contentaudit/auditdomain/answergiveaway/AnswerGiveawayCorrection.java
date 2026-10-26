package com.learney.contentaudit.auditdomain.answergiveaway;

import com.learney.contentaudit.auditdomain.AnalyzerCorrection;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import java.util.List;
import java.util.Optional;

/**
 * F-REGALA-R003: what gives the answer away is corrected at the panel. It takes out of the instructions, or out of the
 * statement, what decides for the student and, if the sentence needs context for a single answer, puts it in the
 * sentence; so it may touch the instructions and the example, with its translation, stored sentence and audio
 * following, never the mini theory. A selected exercise it has not opined on is measured before it is corrected.
 */
public final class AnswerGiveawayCorrection implements AnalyzerCorrection {
    static final String TASK_KIND = "ANSWER_GIVEAWAY";

    private static final List<ExercisePart> PANEL_SCOPE = List.of(ExercisePart.INSTRUCTIONS, ExercisePart.STATEMENT,
            ExercisePart.HINT, ExercisePart.ANSWERS, ExercisePart.OPTIONS, ExercisePart.TRANSLATION,
            ExercisePart.STORED_SENTENCE, ExercisePart.AUDIO);

    @Override public String analyzerName() { return AnswerGiveawayAnalyzerProvider.NAME; }
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
