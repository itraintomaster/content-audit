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
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerPlanBinding;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
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
        var naturalness = provider(EnglishQualityAnalyzerProvider.Kind.NATURALNESS);
        var analyzers = new DefaultAnalyzerCatalog(List.of(naturalness));
        var corrections = new DefaultAnalyzerCorrectionCatalog(List.of(new ContextOnlyCorrection("naturalness")), analyzers);
        assertEquals(List.of("naturalness"), corrections.list().stream().map(AnalyzerCorrection::analyzerName).toList());
        assertEquals(List.of(FindingResolution.PANEL), naturalness.describe().getResolutions());
        assertTrue(naturalness.planBinding().isEmpty());
        var declaration = corrections.find("naturalness").orElseThrow();
        assertTrue(declaration.contextOnly());
        assertTrue(declaration.editScope(FindingResolution.PANEL).isEmpty());
        assertTrue(corrections.find("unknown-unsupported").isEmpty());
    }

    @Test
    @DisplayName("should make what the judge of A1 finds in an example an A1_DIDACTIC task on the exercise, corrected at the panel by changing the example, with its card and its instructions following, and never the instructions of the topic")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R024")
    void shouldMakeWhatTheJudgeOfA1FindsInAnExampleATaskCorrectedAtThePanelByChangingTheExample() {
        // F-MESA-R024 (José, 25/10, brief 223): «Those are the sons' Saturdays.» passed the judges of English; only the
        // judge of A1 saw it, and it only said so. Its findings are now a problem with a task, not context.
        var a1 = provider(EnglishQualityAnalyzerProvider.Kind.A1_DIDACTIC);
        var corrections = new DefaultAnalyzerCorrectionCatalog(List.of(
                new com.learney.contentaudit.auditdomain.englishquality.A1DidacticCorrection()),
                new DefaultAnalyzerCatalog(List.of(a1)));
        assertEquals(Optional.of(new AnalyzerPlanBinding("A1_DIDACTIC", List.of(AuditTarget.QUIZ))), a1.planBinding());
        var declaration = corrections.find("a1-didactic").orElseThrow();
        assertFalse(declaration.contextOnly());
        // The card and the instructions of the exercise follow the example (28/10: «catch → study» kept a card about
        // «catch» and the panel rejected it every time); the instructions of the topic never.
        assertEquals(List.of(ExercisePart.STATEMENT, ExercisePart.HINT, ExercisePart.ANSWERS, ExercisePart.OPTIONS,
                ExercisePart.TRANSLATION, ExercisePart.STORED_SENTENCE, ExercisePart.AUDIO, ExercisePart.MINI_THEORY,
                ExercisePart.INSTRUCTIONS),
                declaration.editScope(FindingResolution.PANEL));
        assertFalse(declaration.editScope(FindingResolution.PANEL).contains(ExercisePart.TOPIC_INSTRUCTIONS));
        assertTrue(declaration.editScope(FindingResolution.RULE).isEmpty(), "a rule does not rewrite an example");
    }

    private static EnglishQualityAnalyzerProvider provider(EnglishQualityAnalyzerProvider.Kind kind) {
        return new EnglishQualityAnalyzerProvider(kind, Mockito.mock(JudgeRuntime.class), null,
                Mockito.mock(EnglishQualityVerdictReader.class), Mockito.mock(QuizStudentViewRenderer.class),
                Mockito.mock(FinishedSentenceReader.class));
    }
}
