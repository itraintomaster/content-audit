package com.learney.contentaudit.revisiondomain.batchengine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.refinerdomain.*;
import com.learney.contentaudit.revisiondomain.batch.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class ContentCorrectionStageTest {
    static Finding finding(String rule) {
        return new Finding("data-coherence",rule,new FindingNodeRef(AuditTarget.QUIZ,TandaFixture.CHILD,"quiz"),
                FindingSeverity.MEDIUM,new FindingEvidence(List.of(),rule,List.of()),FindingResolution.RULE,null,
                new FindingIdentity("data:"+rule,rule));
    }
    @Test void historicalAudioDoesNotAppearResolvedOrLeakIntoCorrectorContext() {
        var audio = finding("audio-matches"); var content = finding("saved-sentence");
        var before = TandaFixture.report(TandaFixture.course(),List.of(audio,content),Map.of(),List.of("data-coherence"));
        var after = TandaFixture.report(TandaFixture.course(),List.of(content),Map.of(),List.of("data-coherence"));
        for (var report : List.of(before,after)) BatchBook.node(report,TandaFixture.CHILD).orElseThrow().getScores().put("data-coherence",0.0);
        var batch = TandaFixture.batch("batch"); batch.setContentVersion(2);
        var run = new SelectiveCorrection.Run("run","recorrect",null,null,1,1,"source",null,null,"audit",null,
                List.of(),List.of(TandaFixture.CHILD),1,1,0,0,0,0,"DONE","human",Instant.EPOCH,Instant.EPOCH,null,false,2,"after",null);
        var config = new BatchServiceConfig();
        var catalog = mock(AnalyzerCatalog.class); var descriptor = mock(AnalyzerDescriptor.class);
        when(descriptor.getCost()).thenReturn(AnalysisCost.INSTANT);
        when(catalog.find("data-coherence")).thenReturn(Optional.of(descriptor)); config.setAnalyzerCatalog(catalog);
        var compared = new DefaultQuizProblemComparison(config).compare(batch,run,before,after,true);
        var problems = compared.quizzes().get(0).rows();
        assertTrue(problems.stream().noneMatch(p -> "audio-matches".equals(p.rule())));
        assertEquals("PERSISTING",problems.stream().filter(p -> "saved-sentence".equals(p.rule())).findFirst().orElseThrow().status());
        var task = TandaFixture.covering("legacy",content,List.of(audio),TandaFixture.CHILD_TOPIC);
        assertEquals(List.of(content),DefaultQuizCorrectionService.contextFindings(before,task));
        assertEquals(List.of(audio),task.getOrigin().getCoveredFindings(),"History is preserved.");
    }
    @Test void legacyAudioTasksAreSkippedAndMixedTasksKeepOnlyTheirContentWork() {
        var audio = finding("audio-known"); var content = finding("saved-sentence");
        var pure = TandaFixture.task("audio-only",audio,TandaFixture.CHILD_TOPIC,CefrLevel.A1);
        var mixed = TandaFixture.covering("mixed",audio,List.of(content),TandaFixture.CHILD_TOPIC);
        assertTrue(ContentCorrectionTasks.audioOnly(pure));
        assertFalse(ContentCorrectionTasks.audioOnly(mixed));
        var tasks = ContentCorrectionTasks.content(List.of(pure,mixed));
        assertEquals(1,tasks.size()); assertEquals(content,tasks.get(0).getOrigin().getFinding());
        assertEquals(audio,mixed.getOrigin().getFinding());
    }
}
