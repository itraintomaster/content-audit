package com.learney.contentaudit.auditdomain.correctionengine;

import com.learney.contentaudit.auditdomain.AnalyzerCorrection;
import com.learney.contentaudit.auditdomain.AnalyzerDescriptor;
import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.auditdomain.DefaultQuizDiagnoses;
import com.learney.contentaudit.auditdomain.NlpTokenizer;
import com.learney.contentaudit.auditdomain.SentenceLengthAnalyzerProvider;
import com.learney.contentaudit.auditdomain.SentenceLengthConfig;
import com.learney.contentaudit.auditdomain.SentenceLengthDiagnosis;
import com.learney.contentaudit.auditdomain.TargetRange;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingDraft;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.findingengine.DefaultAnalyzerCatalog;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;

@Tag("F-TAREA-R006")
class SentenceLengthCorrectionTest {
    private final SentenceLengthCorrection correction = new SentenceLengthCorrection();

    @Test
    void panelCanRewriteOnlySentenceStatementHintsAnswersAndTranslation() {
        assertEquals(List.of(ExercisePart.STORED_SENTENCE, ExercisePart.STATEMENT,
                ExercisePart.HINT, ExercisePart.ANSWERS, ExercisePart.TRANSLATION),
                correction.editScope(FindingResolution.PANEL));
    }

    @Test
    void ruleRankOnlyAndUnknownResolutionHaveNoEditPermission() {
        assertTrue(correction.editScope(FindingResolution.RULE).isEmpty());
        assertTrue(correction.editScope(FindingResolution.RANK_ONLY).isEmpty());
        assertTrue(correction.editScope(null).isEmpty());
    }

    @Test
    @Tag("F-RPRES-R001")
    void optionsFormatInstructionsTheoryAudioAndWholeExerciseStayProtected() {
        EnumSet<ExercisePart> protectedParts = EnumSet.of(ExercisePart.OPTIONS,
                ExercisePart.FORM_KIND, ExercisePart.INSTRUCTIONS, ExercisePart.TOPIC_INSTRUCTIONS,
                ExercisePart.MINI_THEORY, ExercisePart.AUDIO, ExercisePart.WHOLE_EXERCISE);
        for (ExercisePart part : protectedParts) {
            assertFalse(correction.editScope(FindingResolution.PANEL).contains(part),
                    () -> "A length rewrite cannot implicitly edit " + part);
        }
    }

    @Test
    void eachQuizKeepsItsOwnTaskWithoutCoverageTiesAlternativeFixesOrAudioDiagnosis() {
        assertEquals("sentence-length", correction.analyzerName());
        assertEquals(Optional.empty(), correction.coveringAnalyzer());
        assertFalse(correction.groupsByTopic());
        assertTrue(correction.tieRules().isEmpty());
        assertFalse(correction.offersTwoFixes());
        Finding finding = new Finding();
        finding.setAnalyzer("sentence-length");
        finding.setResolution(FindingResolution.PANEL);
        assertFalse(correction.concernsAudio(finding));
        assertFalse(correction.concernsAudio(null));
    }

    @Test
    void declaredScopeMatchesBothActualProviderFindingResolutions() {
        SentenceLengthAnalyzerProvider provider = provider();
        AnalyzerDescriptor card = provider.describe();
        assertEquals(correction.analyzerName(), provider.analyzerName());
        assertEquals(List.of(FindingResolution.PANEL), card.getResolutions());
        for (int count : List.of(1, 10)) {
            DefaultQuizDiagnoses diagnoses = new DefaultQuizDiagnoses();
            diagnoses.setSentenceLengthDiagnosis(new SentenceLengthDiagnosis(
                    count, 3, 8, CefrLevel.A1, count < 3 ? count - 3 : count - 8, 4));
            AuditNode node = new AuditNode();
            node.setTarget(AuditTarget.QUIZ);
            node.setScores(Map.of("sentence-length", 0.5));
            node.setDiagnoses(diagnoses);
            List<FindingDraft> findings = provider.create(null).findingsAt(node);
            assertEquals(1, findings.size());
            FindingDraft finding = findings.get(0);
            assertEquals(count < 3 ? "shorter-than-goal" : "longer-than-goal", finding.getRule());
            assertEquals(FindingResolution.PANEL, finding.getResolution());
            assertFalse(correction.editScope(finding.getResolution()).isEmpty());
        }
    }

    @Test
    void realCatalogFindsTheExplicitDeclarationOnlyWhenItsAnalyzerIsRegistered() {
        DefaultAnalyzerCatalog analyzers = new DefaultAnalyzerCatalog(List.of(provider()));
        DefaultAnalyzerCorrectionCatalog catalog = new DefaultAnalyzerCorrectionCatalog(
                List.of(correction), analyzers);
        assertSame(correction, catalog.find("sentence-length").orElseThrow());
        assertEquals(List.of("sentence-length"), catalog.list().stream()
                .map(AnalyzerCorrection::analyzerName).toList());
        assertTrue(catalog.find("SENTENCE_LENGTH").isEmpty());
        DefaultAnalyzerCorrectionCatalog withoutAnalyzer = new DefaultAnalyzerCorrectionCatalog(
                List.of(correction), new DefaultAnalyzerCatalog(List.of()));
        assertTrue(withoutAnalyzer.find("sentence-length").isEmpty());
    }

    private static SentenceLengthAnalyzerProvider provider() {
        SentenceLengthConfig config = Mockito.mock(SentenceLengthConfig.class);
        Mockito.when(config.getTargetRange(Mockito.any())).thenReturn(Optional.empty());
        Mockito.when(config.getTargetRange(CefrLevel.A1)).thenReturn(
                Optional.of(new TargetRange(CefrLevel.A1, 3, 8)));
        Mockito.when(config.getToleranceMargin()).thenReturn(4);
        return new SentenceLengthAnalyzerProvider(Mockito.mock(NlpTokenizer.class), config);
    }
}
