package com.learney.contentaudit.auditdomain.datacoherence;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.analysis.DefaultAnalysisComposer;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.auditdomain.findingengine.*;
import com.learney.contentaudit.coursedomain.SentenceMode;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataCoherenceStageTest {
    @Test void correctionDoesNotReadTheAudioRegistryAndStillChecksTextAndAnswerSlashes() {
        var recordings = mock(AudioRecordingRegistry.class);
        var stale = DataCoherenceAnalyzerTest.quiz("stale", "shared", List.of("Old text."),
                DataCoherenceAnalyzerTest.text("You"), DataCoherenceAnalyzerTest.cloze("see"),
                DataCoherenceAnalyzerTest.text("cats."));
        var slash = DataCoherenceAnalyzerTest.quiz("slash", "shared", List.of("You see cats."),
                DataCoherenceAnalyzerTest.text("You"), DataCoherenceAnalyzerTest.cloze("see/saw"),
                DataCoherenceAnalyzerTest.text("cats."));
        var noKey = DataCoherenceAnalyzerTest.youSeeCats(); noKey.setAnswerAudioKey(null);
        var report = run(new DataCoherenceAnalyzerProvider(DataCoherenceAnalyzerTest.productionCorrector(),
                recordings, id -> Optional.empty(), DataCoherenceAnalyzerTest.renderer()), List.of(stale, slash, noKey));
        verifyNoInteractions(recordings);
        var nodes = DefaultAnalysisComposer.index(report.getRoot());
        assertEquals(List.of("saved-sentence"), nodes.get("stale").getFindings().stream().map(Finding::getRule).toList());
        assertEquals(List.of("no-slash"), nodes.get("slash").getFindings().stream().map(Finding::getRule).toList());
        assertEquals(FindingSeverity.BLOCKING, nodes.get("slash").getFindings().get(0).getSeverity());
        assertTrue(nodes.get(noKey.getId()).getFindings().isEmpty());
        assertEquals(1.0, nodes.get(noKey.getId()).getScores().get("data-coherence"));
        assertEquals("CONTENT", nodes.get("stale").getMetadata().get("dataCoherenceStage"));
    }

    @Test void prepublicationChecksMismatchSlashMissingUnknownAndSharedAudio() {
        var copies = new DataCoherenceAnalyzerTest.Copies()
                .recording("shared", "Different sentence.").recording("slash", "You see/saw cats.");
        var quizzes = new ArrayList<AuditableQuiz>();
        for (String key : Arrays.asList("shared", "shared", "slash", "unknown", null)) {
            var quiz = DataCoherenceAnalyzerTest.youSeeCats();
            quiz.setId("q" + quizzes.size()); quiz.setAnswerAudioKey(key); quizzes.add(quiz);
        }
        var provider = new DataCoherenceAnalyzerProvider(DataCoherenceAnalyzerTest.productionCorrector(), copies,
                copies, DataCoherenceAnalyzerTest.renderer(), DataCoherenceStage.PREPUBLICATION);
        var report = run(provider, quizzes);
        var issues = DefaultAnalysisComposer.index(report.getRoot()).values().stream()
                .filter(n -> n.getDiagnoses() instanceof QuizDiagnoses).flatMap(n -> ((QuizDiagnoses)n.getDiagnoses())
                        .getDataCoherenceDiagnosis().stream()).flatMap(d -> d.getIssues().stream())
                .map(CopyMismatch::getKind).toList();
        assertTrue(issues.containsAll(List.of(CopyMismatchKind.AUDIO_DIFFERS, CopyMismatchKind.SLASH_IN_AUDIO,
                CopyMismatchKind.UNKNOWN_RECORDING, CopyMismatchKind.NO_AUDIO_KEY, CopyMismatchKind.SHARED_AUDIO_KEY)));
        assertTrue(DefaultAnalysisComposer.index(report.getRoot()).values().stream().flatMap(n -> n.getFindings().stream())
                .allMatch(DataCoherenceStage::isAudioFinding));
    }

    @Test void changingStageChangesTheMeasurementFingerprintAndDeclaredRules() {
        var copies = new DataCoherenceAnalyzerTest.Copies();
        var content = provider(copies, DataCoherenceStage.CONTENT);
        var prepub = provider(copies, DataCoherenceStage.PREPUBLICATION);
        assertEquals(List.of("saved-sentence", "no-slash", "answer-translation"), content.describe().getRules().stream().map(r -> r.getId()).toList());
        assertEquals(5, prepub.describe().getRules().size());
        assertNotEquals(new DefaultAnalyzerCatalog(List.of(content)).measure("data-coherence").orElseThrow().getFingerprint(),
                new DefaultAnalyzerCatalog(List.of(prepub)).measure("data-coherence").orElseThrow().getFingerprint());
        assertEquals("PREPUBLICATION", prepub.config().orElseThrow().describe().get("stage"));
    }

    @Test void historicalProjectionRemovesOnlyAudioAndRebuildsScoresAndCountsWithoutMutatingTheSource() {
        var copies = new DataCoherenceAnalyzerTest.Copies().recording("bad", "Another sentence.");
        var onlyAudio = DataCoherenceAnalyzerTest.youSeeCats(); onlyAudio.setId("audio"); onlyAudio.setAnswerAudioKey("bad");
        var textual = DataCoherenceAnalyzerTest.youSeeCats(); textual.setId("text"); textual.setAnswerAudioKey("bad");
        textual.setSentences(List.of("Old text."));
        var slash = DataCoherenceAnalyzerTest.quiz("answer", "bad", List.of("You see cats."),
                DataCoherenceAnalyzerTest.text("You"), DataCoherenceAnalyzerTest.cloze("see/saw"),
                DataCoherenceAnalyzerTest.text("cats."));
        var original = run(provider(copies, DataCoherenceStage.PREPUBLICATION), List.of(onlyAudio, textual, slash));
        var before = DefaultAnalysisComposer.index(original.getRoot());
        before.get("audio").getPartiallyEvaluatedBy().add("other-control");
        before.get("audio").getMetadata().put("analysisProvenance", Map.of("other-control", Map.of("judgeVersion", "historic")));
        var storedMeasures = new ArrayList<>(before.get("audio").getMeasures());
        int sourceFindings = before.get("audio").getFindings().size();
        var composer = new DefaultAnalysisComposer(new DefaultAnalyzerCatalog(List.of(provider(copies, DataCoherenceStage.CONTENT))),
                new IScoreAggregator(), new DefaultContextNumbersCalculator(), DataCoherenceAnalyzerTest.renderer(), Object::toString);
        var projected = composer.contentOnly(original);
        var after = DefaultAnalysisComposer.index(projected.getRoot());
        assertNotSame(original, projected);
        assertEquals(sourceFindings, before.get("audio").getFindings().size());
        assertEquals(0.0, before.get("audio").getScores().get("data-coherence"));
        assertTrue(after.get("audio").getFindings().isEmpty());
        assertEquals(1.0, after.get("audio").getScores().get("data-coherence"));
        assertEquals(List.of("saved-sentence"), after.get("text").getFindings().stream().map(Finding::getRule).toList());
        assertEquals(List.of("no-slash"), after.get("answer").getFindings().stream().map(Finding::getRule).toList());
        assertEquals(1.0 / 3, projected.getRoot().getScores().get("data-coherence"), 1e-9);
        assertEquals(2, projected.getRoot().getNumbers().getErrors().getWithAnyError());
        assertEquals(3, original.getRoot().getNumbers().getErrors().getWithAnyError());
        assertEquals(storedMeasures, after.get("audio").getMeasures(), "projection cannot invent a new measurement");
        assertEquals(before.get("audio").getMetadata().get("analysisProvenance"), after.get("audio").getMetadata().get("analysisProvenance"));
        assertEquals(List.of("other-control"), after.get("audio").getPartiallyEvaluatedBy());
        assertEquals("CONTENT_WITHOUT_AUDIO_V1", projected.getRoot().getMetadata().get("analysisProjection"));
        assertEquals(1.0 / 3, composer.contentOnly(projected).getRoot().getScores().get("data-coherence"), 1e-9);
    }

    @Test void historicalAudioSlashIsRemovedWhileAnAnswerSlashAndUnrelatedAudioRuleSurvive() {
        var audio = new Finding(); audio.setAnalyzer("data-coherence"); audio.setRule("no-slash");
        audio.setIdentity(new FindingIdentity("key", "audio"));
        assertTrue(DataCoherenceStage.isAudioFinding(audio));
        audio.setIdentity(new FindingIdentity("key", "respuesta"));
        assertFalse(DataCoherenceStage.isAudioFinding(audio));
        audio.setIdentity(null);
        audio.setEvidence(new FindingEvidence(List.of(new EvidencePart("Audio", "a/b")), "reads slash", List.of()));
        assertTrue(DataCoherenceStage.isAudioFinding(audio));
        audio.setAnalyzer("another-analyzer"); audio.setRule("audio-known");
        assertFalse(DataCoherenceStage.isAudioFinding(audio));
    }

    @Test void projectionPreservesOpaqueHistoricalScoresAndPendingCoverage() {
        var copies = new DataCoherenceAnalyzerTest.Copies().recording("A1.01.06.01", "You see cats.");
        var source = run(provider(copies, DataCoherenceStage.PREPUBLICATION), List.of(DataCoherenceAnalyzerTest.youSeeCats()));
        var quiz = DefaultAnalysisComposer.index(source.getRoot()).get("67fab6d59930102295341ec3");
        ((DefaultQuizDiagnoses)quiz.getDiagnoses()).setDataCoherenceDiagnosis(null);
        quiz.getScores().put("data-coherence", 0.0);
        quiz.getUnevaluatedBy().add("data-coherence");
        var composer = new DefaultAnalysisComposer(new DefaultAnalyzerCatalog(List.of(provider(copies, DataCoherenceStage.CONTENT))),
                new IScoreAggregator(), new DefaultContextNumbersCalculator(), DataCoherenceAnalyzerTest.renderer(), Object::toString);
        var projected = composer.contentOnly(source);
        var after = DefaultAnalysisComposer.index(projected.getRoot()).get(quiz.getEntity().getId());
        assertEquals(0.0, after.getScores().get("data-coherence"), "no evidence to reinterpret an opaque score");
        assertEquals(List.of("data-coherence"), after.getUnevaluatedBy());
        assertEquals(1, projected.getRoot().getNumbers().getErrors().getNotFullyEvaluated());
        assertTrue(((QuizDiagnoses)after.getDiagnoses()).getDataCoherenceDiagnosis().isEmpty());
    }

    private static DataCoherenceAnalyzerProvider provider(DataCoherenceAnalyzerTest.Copies copies, DataCoherenceStage stage) {
        return new DataCoherenceAnalyzerProvider(DataCoherenceAnalyzerTest.productionCorrector(), copies, copies,
                DataCoherenceAnalyzerTest.renderer(), stage);
    }
    private static AuditReport run(DataCoherenceAnalyzerProvider provider, List<AuditableQuiz> quizzes) {
        var knowledge = new AuditableKnowledge(quizzes, "Practice", null, true, "k", "Practice", "k", SentenceMode.FILL, "Topic");
        var course = new AuditableCourse(List.of(new AuditableMilestone(List.of(new AuditableTopic(List.of(knowledge),
                "t", "Topic", "t")), "A1", "A1", "A1")));
        return new IAuditEngine(new IScoreAggregator(), new DefaultAnalyzerCatalog(List.of(provider)),
                new DefaultFindingCollector(), new DefaultContextNumbersCalculator()).runAudit(course,
                new AnalyzerRunSelection(List.of("data-coherence"), Map.of()));
    }
}
