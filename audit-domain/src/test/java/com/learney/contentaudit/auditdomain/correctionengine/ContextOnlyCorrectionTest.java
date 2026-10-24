package com.learney.contentaudit.auditdomain.correctionengine;

import com.learney.contentaudit.auditdomain.AnalyzerCorrection;
import com.learney.contentaudit.auditdomain.QuizStudentViewRenderer;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditdomain.englishquality.EnglishQualityAnalyzerProvider;
import com.learney.contentaudit.auditdomain.englishquality.EnglishQualityVerdictReader;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.findingengine.DefaultAnalyzerCatalog;
import com.learney.contentaudit.auditdomain.finishedsentence.FinishedSentenceReader;
import com.learney.contentaudit.auditdomain.judging.JudgeRuntime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;

class ContextOnlyCorrectionTest {
    @Test
    void existingDeclarationsRemainActionableByDefault() {
        assertFalse(new SentenceLengthCorrection().contextOnly());
        assertFalse(new LemmaAbsenceCorrection().contextOnly());
        assertFalse(new QuizInstructionCorrection().contextOnly());
    }

    @Test
    void contextOnlyIsExplicitAndPreservesCanonicalAnalyzerName() {
        AnalyzerCorrection declaration = new ContextOnlyCorrection("a1-didactic");
        assertTrue(declaration.contextOnly());
        assertEquals("a1-didactic", declaration.analyzerName());
    }

    @Test
    void noResolutionGrantsAnyEditPermission() {
        AnalyzerCorrection declaration = new ContextOnlyCorrection("independent-review");
        for (FindingResolution resolution : FindingResolution.values()) {
            assertTrue(declaration.editScope(resolution).isEmpty());
            for (ExercisePart part : ExercisePart.values()) {
                assertFalse(declaration.editScope(resolution).contains(part));
            }
        }
        assertTrue(declaration.editScope(null).isEmpty());
    }

    @Test
    void contextDeclarationCannotCoverGroupTieOfferFixesOrDiagnoseAudio() {
        AnalyzerCorrection declaration = new ContextOnlyCorrection("independent-review");
        assertTrue(declaration.coveringAnalyzer().isEmpty());
        assertFalse(declaration.groupsByTopic());
        assertTrue(declaration.tieRules().isEmpty());
        assertFalse(declaration.offersTwoFixes());
        assertFalse(declaration.concernsAudio(new Finding()));
        assertFalse(declaration.concernsAudio(null));
    }

    @Test
    void missingOrBlankAnalyzerNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ContextOnlyCorrection(null));
        assertThrows(IllegalArgumentException.class, () -> new ContextOnlyCorrection(""));
        assertThrows(IllegalArgumentException.class, () -> new ContextOnlyCorrection(" \t\n"));
    }

    @Test
    void realCatalogRetainsContextOnlyPanelDeclarationsWithoutInventingPlanBindings() {
        var providers = List.of(provider(EnglishQualityAnalyzerProvider.Kind.NATURALNESS),
                provider(EnglishQualityAnalyzerProvider.Kind.A1_DIDACTIC));
        var analyzers = new DefaultAnalyzerCatalog(List.copyOf(providers));
        var corrections = new DefaultAnalyzerCorrectionCatalog(List.of(
                new ContextOnlyCorrection("a1-didactic"), new ContextOnlyCorrection("naturalness")), analyzers);
        assertEquals(List.of("naturalness", "a1-didactic"), corrections.list().stream()
                .map(AnalyzerCorrection::analyzerName).toList());
        for (var provider : providers) {
            assertEquals(List.of(FindingResolution.PANEL), provider.describe().getResolutions());
            assertTrue(provider.planBinding().isEmpty());
            var declaration = corrections.find(provider.analyzerName()).orElseThrow();
            assertTrue(declaration.contextOnly());
            assertTrue(declaration.editScope(FindingResolution.PANEL).isEmpty());
        }
        assertTrue(corrections.find("unknown-unsupported").isEmpty());
    }

    private static EnglishQualityAnalyzerProvider provider(EnglishQualityAnalyzerProvider.Kind kind) {
        return new EnglishQualityAnalyzerProvider(kind, Mockito.mock(JudgeRuntime.class), null,
                Mockito.mock(EnglishQualityVerdictReader.class), Mockito.mock(QuizStudentViewRenderer.class),
                Mockito.mock(FinishedSentenceReader.class));
    }
}
