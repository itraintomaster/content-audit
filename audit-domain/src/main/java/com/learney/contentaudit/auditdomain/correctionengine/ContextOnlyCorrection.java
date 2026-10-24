package com.learney.contentaudit.auditdomain.correctionengine;

import com.learney.contentaudit.auditdomain.AnalyzerCorrection;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import java.util.List;
import java.util.Optional;

/**
 * Explicitly registers findings for correction context without inventing an automatic rewrite
 * task. A PANEL finding may require independent review while another task owns the actual
 * correction. Supplying that finding never broadens that task's declared edit scope.
 */
public final class ContextOnlyCorrection implements AnalyzerCorrection {
    private final String analyzerName;

    public ContextOnlyCorrection(String analyzerName) {
        if (analyzerName == null || analyzerName.isBlank()) {
            throw new IllegalArgumentException("La declaración de contexto necesita el nombre del analizador");
        }
        this.analyzerName = analyzerName;
    }

    @Override
    public String analyzerName() {
        return analyzerName;
    }

    @Override
    public boolean contextOnly() {
        return true;
    }

    @Override
    public List<ExercisePart> editScope(FindingResolution resolution) {
        return List.of();
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

    @Override
    public boolean concernsAudio(Finding finding) {
        return false;
    }
}
