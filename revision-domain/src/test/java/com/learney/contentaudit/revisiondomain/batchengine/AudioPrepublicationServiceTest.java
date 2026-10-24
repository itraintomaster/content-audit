package com.learney.contentaudit.revisiondomain.batchengine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.analysis.DefaultAnalysisComposer;
import com.learney.contentaudit.auditdomain.correctormatch.DefaultCorrectorMatcher;
import com.learney.contentaudit.auditdomain.correctormatch.FormEquivalenceTable;
import com.learney.contentaudit.auditdomain.datacoherence.*;
import com.learney.contentaudit.auditdomain.findingengine.*;
import com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.batch.AudioPrepublicationService.*;
import com.learney.contentaudit.revisiondomain.nonregression.AudioRerecordRequest;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;

class AudioPrepublicationServiceTest {
    final CourseEntity source = TandaFixture.course();
    final CourseEntity current = CorrectionEvolution.copyCourse(source);
    final CorrectionBatch batch = TandaFixture.batch("c2");
    final BatchServiceConfig config = new BatchServiceConfig();
    final BatchStore store = mock(BatchStore.class);
    final CourseRepository repository = mock(CourseRepository.class);
    final AuditEngine engine = mock(AuditEngine.class);
    final AuditEngine contentEngine = mock(AuditEngine.class);
    final BatchChangeLedger ledger = mock(BatchChangeLedger.class);
    final AtomicReference<CourseEntity> lastMapped = new AtomicReference<>();
    DefaultAudioPrepublicationService service;
    boolean prepubStage = true;
    boolean partial = false;
    final Set<String> mismatchedAudio = new HashSet<>();
    String quiz;
    @BeforeEach void setup() {
        quiz = source.getRoot().getMilestones().get(0).getTopics().get(0).getKnowledges().get(0).getQuizTemplates().get(0).getId();
        batch.setContentVersion(3); batch.setCorrectionRevision(2);
        when(store.load("c2")).thenReturn(Optional.of(batch));
        when(repository.load(any())).thenReturn(source);
        when(ledger.composedCourse(batch,source)).thenReturn(current);
        config.setBatchStore(store); config.setCourseRepository(repository); config.setCoursePath(TandaFixture.COURSE_PATH);
        config.setAuditPrepublicationEngine(engine); config.setAuditEngine(contentEngine);
        config.setCourseMapper(course -> {
            lastMapped.set(course);
            return new AuditableCourse(TandaFixture.report(course,List.of(),Map.of(),List.of()).getRoot().getChildren().stream()
                    .map(n -> (AuditableMilestone)n.getEntity()).toList());
        });
        when(engine.runAudit(any(),any())).thenAnswer(i -> {
            var report = TandaFixture.report(lastMapped.get(),List.of(),Map.of(),List.of("data-coherence"));
            for (var node : BatchBook.nodes(report)) if (node.getTarget() == AuditTarget.QUIZ) {
                node.getScores().put("data-coherence",1.0);
                node.getMetadata().put("dataCoherenceStage",prepubStage ? "PREPUBLICATION" : "CONTENT");
                if (partial) node.setPartiallyEvaluatedBy(List.of("data-coherence"));
                if (mismatchedAudio.contains(node.getEntity().getId())) {
                    node.getScores().put("data-coherence",0.0);
                    node.getFindings().add(QuizCorrectionServiceTest.finding("data-coherence","audio-matches",node.getEntity().getId()));
                }
            }
            return report;
        });
        service = new DefaultAudioPrepublicationService(config,ledger,c -> Integer.toString(c.hashCode()));
    }
    @Test void repeatedReadsAreIdempotentAndNeverGeneratePublishOrSpendQueries() {
        var first = service.execute(new Request("read","c2"));
        assertEquals(first,service.execute(new Request("read","c2")));
        assertEquals("PREPUBLICATION",first.stage()); assertEquals(0,first.spent());
        assertEquals(3,first.contentVersion()); assertEquals(first.total(),first.recordTextMatches());
        assertTrue(first.limitations().stream().anyMatch(s -> s.contains("sonido real")));
        verify(store,never()).save(any()); verify(repository,never()).save(any(),any());
        verifyNoInteractions(contentEngine);
        verify(engine,times(2)).runAudit(any(),argThat(s -> s.getAnalyzers().equals(List.of("data-coherence"))
                && s.getPolicies().get("data-coherence").getMaxNewEvaluations()==0));
    }
    @Test void spokenChangesCreatePendingRequestWhenRecordedTextDoesNotMatch() {
        mismatchedAudio.add(quiz);
        var changed = TandaFixture.exercise(current,quiz);
        changed.getForm().setSentenceParts(List.of(new SentencePartEntity(SentencePartKind.TEXT,"She",null),new SentencePartEntity(SentencePartKind.CLOZE,null,List.of("swims")),new SentencePartEntity(SentencePartKind.TEXT,".",null)));
        var result = service.execute(new Request("read","c2"));
        var entry = result.entries().stream().filter(e -> quiz.equals(e.quizId())).findFirst().orElseThrow();
        assertEquals("PENDING_GENERATION",entry.status()); assertEquals(List.of("She swims."),entry.finalTexts());
        assertEquals(entry.finalTexts(),entry.rerecord().getNewTexts()); assertFalse(entry.findings().isEmpty());
        assertEquals(1,result.pending());
        var unchanged = service.execute(new Request("read","c2")); assertEquals(result,unchanged);
        batch.setContentVersion(4);
        changed.getForm().setSentenceParts(List.of(new SentencePartEntity(SentencePartKind.TEXT,"She",null),new SentencePartEntity(SentencePartKind.CLOZE,null,List.of("runs")),new SentencePartEntity(SentencePartKind.TEXT,".",null)));
        var next = service.execute(new Request("read","c2"));
        assertEquals(4,next.contentVersion()); assertNotEquals(result.contentFingerprint(),next.contentFingerprint());
        assertEquals(List.of("She runs."),next.entries().stream().filter(e -> quiz.equals(e.quizId())).findFirst().orElseThrow().finalTexts());
    }
    @Test void pendingDeclarationsSurviveAContentProjectionAndAreRefreshedToFinalSpokenText() {
        mismatchedAudio.add(quiz);
        String audio = TandaFixture.exercise(current,quiz).getAnswerAudioUrl();
        batch.setPendingRerecords(List.of(new AudioRerecordRequest(quiz,audio,List.of("old text"))));
        var entry = service.execute(new Request("read","c2")).entries().stream().filter(e -> quiz.equals(e.quizId())).findFirst().orElseThrow();
        assertEquals("PENDING_GENERATION",entry.status());
        assertEquals(entry.finalTexts(),entry.rerecord().getNewTexts());
        assertNotEquals(List.of("old text"),entry.finalTexts());
    }
    @Test void wrongStageOrIncompleteCoverageCannotBecomeAudioApproval() {
        prepubStage = false;
        var wrong = service.execute(new Request("read","c2"));
        assertEquals(0,wrong.recordTextMatches()); assertEquals(wrong.total(),wrong.recordTextProblems());
        prepubStage = true; partial = true;
        var pending = service.execute(new Request("read","c2"));
        assertEquals(0,pending.recordTextMatches()); assertEquals(pending.total(),pending.recordTextProblems());
    }
    @Test void writeActionsAreRejectedBeforeAnInspectionRuns() {
        assertThrows(IllegalArgumentException.class,() -> service.execute(new Request("generate","c2")));
        assertThrows(IllegalArgumentException.class,() -> service.execute(new Request("publish","c2")));
        verifyNoInteractions(engine,contentEngine); verify(store,never()).save(any());
    }
    @Test void finalRegistryMatchDoesNotRenewAnOldPendingRequestOrMutateItsHistory() {
        String audio = TandaFixture.exercise(current,quiz).getAnswerAudioUrl();
        var old = new AudioRerecordRequest(quiz,audio,List.of("old text"));
        batch.setPendingRerecords(List.of(old));
        // The registry has since been updated to the current final text; the read is complete.
        var entry = service.execute(new Request("read","c2")).entries().stream()
                .filter(e -> quiz.equals(e.quizId())).findFirst().orElseThrow();
        assertEquals("TEXT_RECORD_MATCHES",entry.status()); assertNull(entry.rerecord());
        assertEquals(List.of(old),batch.getPendingRerecords());
        assertEquals(List.of("old text"),old.getNewTexts());
        assertTrue(entry.reasons().stream().anyMatch(s -> s.contains("no comprueba el sonido")));
        verify(store,never()).save(any()); verifyNoInteractions(contentEngine);
    }
    @Test void matchingFinalRegistryAlsoOverridesADerivedTextChange() {
        var changed = TandaFixture.exercise(current,quiz);
        changed.getForm().setSentenceParts(List.of(new SentencePartEntity(SentencePartKind.TEXT,"She",null),
                new SentencePartEntity(SentencePartKind.CLOZE,null,List.of("swims")),new SentencePartEntity(SentencePartKind.TEXT,".",null)));
        var entry = service.execute(new Request("read","c2")).entries().stream()
                .filter(e -> quiz.equals(e.quizId())).findFirst().orElseThrow();
        assertEquals("TEXT_RECORD_MATCHES",entry.status()); assertNull(entry.rerecord());
        assertEquals(List.of("She swims."),entry.finalTexts());
    }
    @Test void sharedAudioKeyAcrossLevelsIsDetectedBeforeSelectingTheCorrectionLevel() {
        String shared = "shared-key-between-a1-a2";
        TandaFixture.exercise(current,quiz).setAnswerAudioUrl(shared);
        TandaFixture.exercise(current,TandaFixture.PAST).setAnswerAudioUrl(shared);
        var a1 = TandaFixture.auditable(TandaFixture.exercise(current,quiz));
        var recorded = new DefaultSpokenTextBuilder().spokenTexts(a1,SentenceMode.FILL).get(0);
        var registry = new AudioRecordingRegistry() {
            @Override public Optional<AudioRecording> recordingOf(String key) {
                return Optional.of(new AudioRecording(key,recorded,"local test registry"));
            }
            @Override public String version() { return "local-test"; }
        };
        var renderer = new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create());
        var provider = new DataCoherenceAnalyzerProvider(
                new DefaultCorrectorMatcher(new FormEquivalenceTable("test",List.of(),List.of(),false,List.of())),
                registry,id -> Optional.empty(),renderer,DataCoherenceStage.PREPUBLICATION);
        config.setAuditPrepublicationEngine(new IAuditEngine(new IScoreAggregator(),
                new DefaultAnalyzerCatalog(List.of(provider)),new DefaultFindingCollector(),new DefaultContextNumbersCalculator()));
        var result = service.execute(new Request("read","c2"));
        assertTrue(result.entries().stream().noneMatch(e -> TandaFixture.PAST.equals(e.quizId())),"A2 remains outside the selected correction rows.");
        var entry = result.entries().stream().filter(e -> quiz.equals(e.quizId())).findFirst().orElseThrow();
        assertEquals("TEXT_RECORD_PROBLEM",entry.status());
        assertTrue(entry.findings().stream().anyMatch(f -> "audio-known".equals(f.getRule())),"The shared key must not look unique after an A1 cut.");
        verify(store,never()).save(any()); verify(repository,never()).save(any(),any());
    }

    @Test void partialHistoricalCheckpointCannotCertifyGlobalAudioKeyUniquenessOrMixInLiveContent() {
        var partialCourse = DefaultCurrentAnalysisService.cut(source,CefrLevel.A1);
        batch.setBaseCourseSnapshot(partialCourse);
        when(ledger.composedCourse(eq(batch),any())).thenReturn(partialCourse);
        var result = service.execute(new Request("read","c2"));
        assertEquals(0,result.recordTextMatches()); assertEquals(result.total(),result.recordTextProblems());
        assertTrue(result.entries().stream().noneMatch(e -> TandaFixture.PAST.equals(e.quizId())));
        assertTrue(result.limitations().stream().anyMatch(s -> s.contains("foto histórica no incluye todo el curso")));
        assertEquals(partialCourse,batch.getBaseCourseSnapshot());
        verify(store,never()).save(any()); verify(repository,never()).save(any(),any());
    }

}
