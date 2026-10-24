package com.learney.contentaudit.auditdomain.correctionengine;

import com.learney.contentaudit.auditdomain.AnalyzerCorrection;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import java.util.List;
import java.util.Optional;

/**
 * Declares the panel rewrite for sentence-length, whose provider already publishes PANEL findings.
 * Shortening or lengthening keeps the stored sentence, its statement and gaps, and its translation
 * coherent (F-RCSL-R001, F-TAREA-R006). Every other part stays protected (F-RPRES-R001): this task
 * grants no permission to change options, instructions, mini theory, exercise format or audio.
 * A change to the spoken sentence still needs the explicit re-recording request required by
 * F-NOEMP-R004; this declaration does not bypass that guard or update audio implicitly.
 */
public final class SentenceLengthCorrection implements AnalyzerCorrection {
    private static final List<ExercisePart> PANEL_SCOPE = List.of(
            ExercisePart.STORED_SENTENCE, ExercisePart.STATEMENT, ExercisePart.HINT,
            ExercisePart.ANSWERS, ExercisePart.TRANSLATION);

    @Override
    public String analyzerName() {
        return "sentence-length";
    }

    @Override
    public List<ExercisePart> editScope(FindingResolution resolution) {
        return resolution == FindingResolution.PANEL ? PANEL_SCOPE : List.of();
    }

    @Override
    public Optional<String> coveringAnalyzer() {
        return Optional.empty();
    }

    @Override
    public boolean groupsByTopic() {
        return false;
    }

    @Override
    public List<String> tieRules() {
        return List.of();
    }

    @Override
    public boolean offersTwoFixes() {
        return false;
    }

    /** A length finding concerns the written sentence, not a diagnosed audio defect. */
    @Override
    public boolean concernsAudio(Finding finding) {
        return false;
    }
}
