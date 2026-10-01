package com.learney.contentaudit.revisiondomain.nonregressionengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.auditdomain.finding.AnalysisCost;
import com.learney.contentaudit.auditdomain.finding.EvidencePart;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingEvidence;
import com.learney.contentaudit.auditdomain.finding.FindingIdentity;
import com.learney.contentaudit.auditdomain.finding.FindingNodeRef;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.revisiondomain.comparison.BatchComparison;
import com.learney.contentaudit.revisiondomain.comparison.ChangeOutcome;
import com.learney.contentaudit.revisiondomain.nonregression.AudioRerecordRequest;
import com.learney.contentaudit.revisiondomain.nonregression.BatchGateVerdict;
import com.learney.contentaudit.revisiondomain.nonregression.ChangeGateVerdict;
import com.learney.contentaudit.revisiondomain.nonregression.ChangeProbe;
import com.learney.contentaudit.revisiondomain.nonregression.SpokenText;
import com.learney.contentaudit.revisiondomain.nonregression.UnitMeasurement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * The audio follows the exercise (orchestrator, 1/10), at the gate of each change: the measurement after a fix that
 * rewrites the English carries what data-coherence marks of the copies outside the course -- the audio, the sentence
 * production keeps -- and those are pending, not new errors; what the change really breaks still is.
 */
public class PendingCopiesGateIntegrationTest {

    private static final String VEGETABLES = "67fab6d59930102295341ecd";
    private static final String VEGETABLES_AUDIO = "A1.02.01.cd";

    @Test
    @DisplayName("should pass the vegetables fix with its request to rerecord «I love vegetables.» and its production sentence pending, not as new errors")
    @Tag("FEAT-NOEMP")
    @Tag("F-NOEMP-R004")
    public void shouldPassTheVegetablesFixWithItsRequestToRerecordAndItsProductionSentencePending() {
        UnitMeasurement before = unit(List.of(), spoken(VEGETABLES, "I love a lot of vegetables."));
        UnitMeasurement after = unit(List.of(audio(VEGETABLES), production(VEGETABLES)),
                spoken(VEGETABLES, "I love vegetables."));

        ChangeGateVerdict verdict = gate().check(new ChangeProbe(List.of(VEGETABLES), false, before, after, List.of()));

        assertTrue(verdict.isPasses(), verdict.getReasons().toString());
        assertTrue(verdict.getNewErrors().isEmpty(), "pending, not new: " + verdict.getNewErrors());
        assertEquals(1, verdict.getRerecords().size());
        AudioRerecordRequest request = verdict.getRerecords().get(0);
        assertEquals(VEGETABLES, request.getQuizId());
        assertEquals(VEGETABLES_AUDIO, request.getAudioKey());
        assertEquals(List.of("I love vegetables."), request.getNewTexts(), "the audio is recorded with the new text");
    }

    @Test
    @DisplayName("should still count as a new error an audio that stops matching without its request to rerecord, a shared key and a stale tree sentence")
    @Tag("FEAT-NOEMP")
    @Tag("F-NOEMP-R001")
    public void shouldStillCountAsANewErrorAnAudioThatStopsMatchingWithoutItsRequestASharedKeyAndAStaleTreeSentence() {
        // What is heard does not change, so nothing asks to record it again: an audio that stops matching is the change's.
        UnitMeasurement before = unit(List.of(), spoken(VEGETABLES, "I love a lot of vegetables."));
        Finding shared = finding("audio-known", "audio-compartido", FindingSeverity.LOW, FindingResolution.RULE,
                "comparte la clave de audio A1.02.01.c8: se graba uno propio con el texto de hoy.");
        Finding tree = finding("saved-sentence", "arbol", FindingSeverity.MEDIUM, FindingResolution.RULE,
                "oración guardada (árbol): \"I love a lot of vegetables.\"; hoy: \"I love vegetables.\".");
        UnitMeasurement after = unit(List.of(audio(VEGETABLES), shared, tree, production(VEGETABLES)),
                spoken(VEGETABLES, "I love a lot of vegetables."));

        ChangeGateVerdict verdict = gate().check(new ChangeProbe(List.of(VEGETABLES), false, before, after, List.of()));

        assertFalse(verdict.isPasses());
        assertTrue(verdict.getRerecords().isEmpty(), "what is heard did not change");
        assertEquals(List.of("data-coherence|audio-known|" + VEGETABLES + "|audio-compartido",
                        "data-coherence|audio-matches|" + VEGETABLES + "|audio",
                        "data-coherence|saved-sentence|" + VEGETABLES + "|arbol"),
                verdict.getNewErrors().stream().map(GateRules::identity).toList(),
                "only the sentence production keeps is pending: it waits for the course to go there");
    }

    @Test
    @DisplayName("should list apart, and not return, the production sentence a comparison that did not settle it brings as a new finding")
    @Tag("FEAT-NOEMP")
    @Tag("F-NOEMP-R005")
    public void shouldListApartAndNotReturnTheProductionSentenceAComparisonThatDidNotSettleItBringsAsANewFinding() {
        BatchComparison comparison = new BatchComparison();
        comparison.setChanges(new ArrayList<>(List.of(new ChangeOutcome("tanda-a1-vegetables", AuditTarget.QUIZ,
                VEGETABLES, true, new ArrayList<>(List.of(audio(VEGETABLES), production(VEGETABLES))), List.of(),
                List.of()))));
        comparison.setRows(List.of());
        comparison.setBreaches(List.of());

        BatchGateVerdict verdict = new DefaultGateFactory().batchGate().evaluate(comparison, List.of(), List.of());

        assertTrue(verdict.isPasses(), verdict.getReasons().toString());
        assertEquals(List.of("audio para regrabar: " + VEGETABLES + " (data-coherence|audio-matches|" + VEGETABLES
                        + "|audio)",
                "oración de producción por llevar: " + VEGETABLES + " (data-coherence|saved-sentence|" + VEGETABLES
                        + "|produccion)"),
                verdict.getListedExceptions());
    }

    // =========================================================================

    private static com.learney.contentaudit.revisiondomain.nonregression.ChangeGate gate() {
        return new DefaultGateFactory().changeGate();
    }

    private static UnitMeasurement unit(List<Finding> findings, SpokenText spoken) {
        return new UnitMeasurement(CefrLevel.A1, findings, List.of(), List.of(), List.of(spoken), List.of());
    }

    private static SpokenText spoken(String quizId, String text) {
        return new SpokenText(quizId, VEGETABLES_AUDIO, List.of(text));
    }

    /** data-coherence on the audio, as it marks it when the recording says something else (AUDIO_DIFFERS). */
    private static Finding audio(String quizId) {
        return new Finding("data-coherence", "audio-matches", new FindingNodeRef(AuditTarget.QUIZ, quizId, quizId),
                FindingSeverity.MEDIUM, new FindingEvidence(List.of(new EvidencePart("Audio",
                        "I love a lot of vegetables."), new EvidencePart("Hoy", "I love vegetables.")),
                        "audio: \"I love a lot of vegetables.\"; hoy: \"I love vegetables.\". No se sabe cuál copia está"
                                + " bien: va a la mesa.", List.of()),
                FindingResolution.PANEL, AnalysisCost.INSTANT,
                new FindingIdentity("data-coherence|audio-matches|" + quizId + "|audio", "audio"));
    }

    /** data-coherence on the sentence production keeps (SAVED_SENTENCE_DIFFERS, PRODUCTION_SENTENCE). */
    private static Finding production(String quizId) {
        return new Finding("data-coherence", "saved-sentence", new FindingNodeRef(AuditTarget.QUIZ, quizId, quizId),
                FindingSeverity.MEDIUM, new FindingEvidence(List.of(new EvidencePart("Oración guardada (producción)",
                        "I love a lot of vegetables."), new EvidencePart("Hoy", "I love vegetables.")),
                        "oración guardada (producción): \"I love a lot of vegetables.\"; hoy: \"I love vegetables.\"."
                                + " Se vuelve a armar desde las partes.", List.of()),
                FindingResolution.RULE, AnalysisCost.INSTANT,
                new FindingIdentity("data-coherence|saved-sentence|" + quizId + "|produccion", "produccion"));
    }

    private static Finding finding(String rule, String marker, FindingSeverity severity, FindingResolution resolution,
            String observation) {
        return new Finding("data-coherence", rule, new FindingNodeRef(AuditTarget.QUIZ, VEGETABLES, VEGETABLES),
                severity, new FindingEvidence(List.of(new EvidencePart("Ejercicio", VEGETABLES)), observation, List.of()),
                resolution, AnalysisCost.INSTANT,
                new FindingIdentity("data-coherence|" + rule + "|" + VEGETABLES + "|" + marker, marker));
    }
}
