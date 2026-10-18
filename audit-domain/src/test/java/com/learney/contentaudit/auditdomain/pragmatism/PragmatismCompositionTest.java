package com.learney.contentaudit.auditdomain.pragmatism;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.analysis.*;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.auditdomain.findingengine.DefaultAnalyzerCatalog;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PragmatismCompositionTest {
    @Test void aSupplementPreservesItsOwnDiagnosisVersionAndUnrelatedScores(){
        var primaryNode=PragmatismAnalyzerTest.node("q","I need a clean towel.");primaryNode.getScores().put("naturalness",.8);
        var measured=PragmatismAnalyzerTest.node("q","I need a clean towel.");measured.getScores().put("pragmatism",1.0);
        var diagnosis=new PragmatismDiagnosis(PragmatismAnalyzerTest.verdict("I need a clean towel.",PragmatismVerdict.Status.PASS),"prag-v2",false);
        ((DefaultQuizDiagnoses)measured.getDiagnoses()).setPragmatismDiagnosis(diagnosis);
        var composer=composer();
        var result=composer.compose(report(primaryNode),List.of(new AnalysisSupplement("run","audit","pragmatism","test-runtime",Instant.EPOCH,Set.of("q"),report(measured))));
        var combined=DefaultAnalysisComposer.index(result.getRoot()).get("q");
        assertEquals(1.0,combined.getScores().get("pragmatism"));assertEquals(.8,combined.getScores().get("naturalness"));
        assertEquals(diagnosis,((QuizDiagnoses)combined.getDiagnoses()).getPragmatismDiagnosis().orElseThrow());
        assertEquals("prag-v2",DefaultAnalysisComposer.version(combined,"pragmatism",result));
        assertFalse(primaryNode.getScores().containsKey("pragmatism"));
        var copied=composer.compose(result,List.of());
        assertEquals(diagnosis,((QuizDiagnoses)DefaultAnalysisComposer.index(copied.getRoot()).get("q").getDiagnoses()).getPragmatismDiagnosis().orElseThrow());
    }
    @Test void changedVisibleContextCannotReceiveAStalePragmaticPass(){
        var primary=PragmatismAnalyzerTest.node("q","The lamp swallowed a calendar.");
        var measured=PragmatismAnalyzerTest.node("q","The lamp swallowed a calendar.");
        ((AuditableQuiz)measured.getEntity()).setInstructions("Contá una escena de una lámpara parlante en un cuento.");
        measured.getScores().put("pragmatism",1.0);
        var result=composer().compose(report(primary),List.of(new AnalysisSupplement("r","a","pragmatism","test",Instant.EPOCH,Set.of("q"),report(measured))));
        assertFalse(DefaultAnalysisComposer.index(result.getRoot()).get("q").getScores().containsKey("pragmatism"));
    }
    static DefaultAnalysisComposer composer(){
        var provider=new PragmatismAnalyzerProvider(mock(com.learney.contentaudit.auditdomain.judging.JudgeRuntime.class),null,ignored->null,PragmatismAnalyzerTest.renderer());
        return new DefaultAnalysisComposer(new DefaultAnalyzerCatalog(List.of(provider)),mock(ScoreAggregator.class),mock(ContextNumbersCalculator.class),
                PragmatismAnalyzerTest.renderer(),content->content.toString());
    }
    static AuditReport report(AuditNode quiz){
        var root=new AuditNode(null,AuditTarget.COURSE,null,new ArrayList<>(List.of(quiz)),new LinkedHashMap<>(),new LinkedHashMap<>(),new DefaultCourseDiagnoses());
        quiz.setParent(root);return new AuditReport(root);
    }
}
