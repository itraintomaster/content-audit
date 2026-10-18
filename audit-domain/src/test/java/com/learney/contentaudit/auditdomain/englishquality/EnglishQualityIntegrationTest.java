package com.learney.contentaudit.auditdomain.englishquality;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.analysis.*;
import com.learney.contentaudit.auditdomain.catalog.*;
import com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.evaluationledgerdomain.ContentFingerprinter;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnglishQualityIntegrationTest {
    @Test void pendingSupplementReplacesOldApprovalAndRebuildsCountsFromSelectedVerdicts() {
        var n=EnglishQualityTest.node("q","A1","I eat.");var root=n.ancestor(AuditTarget.COURSE).orElseThrow();
        n.getScores().put("naturalness",1.0);n.getScores().put("a1-didactic",1.0);
        root.getMetadata().put("naturalnessCounts",Map.of("PASS",99));
        var s=EnglishQualityTest.node("q","A1","I eat.");var source=s.ancestor(AuditTarget.COURSE).orElseThrow();
        s.getMetadata().put("naturalnessVerdict",Map.of("assessments",List.of(Map.of("status","REVIEW"))));
        s.getMetadata().put("a1-didacticVerdict",new A1DidacticVerdict("q",A1DidacticVerdict.Status.REVIEW,"Simple",List.of(),null,"Falta recorrido","Revisar enseñanza previa",List.of(),null,null));
        for (String name : List.of("naturalness","a1-didactic")) {
            s.getMetadata().put(name+"InputContract",EnglishQualitySubjectBuilder.CONTRACT);
            s.getMetadata().put(name+"Version","current");s.getUnevaluatedBy().add(name);
        }
        var catalog=mock(AnalyzerCatalog.class);when(catalog.list()).thenReturn(List.of());
        var hashes=mock(ContentFingerprinter.class);when(hashes.fingerprint(anyMap())).thenAnswer(call->call.getArgument(0).toString());
        var composer=new DefaultAnalysisComposer(catalog,mock(ScoreAggregator.class),mock(ContextNumbersCalculator.class),
                new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create()),hashes);
        var supplements=List.of("naturalness","a1-didactic").stream().map(name->new AnalysisSupplement("run","audit",name,"test",Instant.EPOCH,Set.of("q"),new AuditReport(source))).toList();
        var result=composer.compose(new AuditReport(root),supplements);var selected=DefaultAnalysisComposer.index(result.getRoot()).get("q");
        assertTrue(selected.getScores().isEmpty());assertTrue(selected.getUnevaluatedBy().containsAll(List.of("naturalness","a1-didactic")));
        assertEquals(Map.of("REVIEW",1),result.getRoot().getMetadata().get("naturalnessCounts"));
        assertEquals(Map.of("REVIEW",1),result.getRoot().getMetadata().get("a1-didacticCounts"));
        assertEquals("current",DefaultAnalysisComposer.version(selected,"a1-didactic",result));
        assertEquals(1.0,n.getScores().get("a1-didactic"));
    }
    @Test void modernSupplementInvalidatesLegacyConstantApprovalAndKeepsSeparateVerdicts() {
        var a=EnglishQualityTest.node("a","A1","I eat.");var b=EnglishQualityTest.node("b","A1","I drink.");
        var root=a.ancestor(AuditTarget.COURSE).orElseThrow();var other=b.ancestor(AuditTarget.MILESTONE).orElseThrow();other.setParent(root);root.getChildren().add(other);
        a.getScores().put("naturalness",1.0);b.getScores().put("naturalness",1.0);
        var sa=EnglishQualityTest.node("a","A1","I eat.");var sb=EnglishQualityTest.node("b","A1","I drink.");
        var source=sa.ancestor(AuditTarget.COURSE).orElseThrow();var sibling=sb.ancestor(AuditTarget.MILESTONE).orElseThrow();sibling.setParent(source);source.getChildren().add(sibling);
        sa.getScores().put("naturalness",0.0);sa.getMetadata().put("naturalnessInputContract",EnglishQualitySubjectBuilder.CONTRACT);
        sa.getMetadata().put("naturalnessVerdict",new NativeUsageVerdict(List.of(EnglishQualityTest.a("I eat.",NativeUsageVerdict.Status.FAIL)),"Test verdict"));
        sa.getMetadata().put("naturalnessVersion","current");
        var catalog=mock(AnalyzerCatalog.class);when(catalog.list()).thenReturn(List.of());
        var hashes=mock(ContentFingerprinter.class);when(hashes.fingerprint(anyMap())).thenAnswer(call->call.getArgument(0).toString());
        var composer=new DefaultAnalysisComposer(catalog,mock(ScoreAggregator.class),mock(ContextNumbersCalculator.class),
                new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create()),hashes);
        var result=composer.compose(new AuditReport(root),List.of(new AnalysisSupplement("run","audit","naturalness","test",Instant.EPOCH,Set.of("a"),new AuditReport(source))));
        var index=DefaultAnalysisComposer.index(result.getRoot());
        assertEquals(0.0,index.get("a").getScores().get("naturalness"));
        assertTrue(index.get("a").getMetadata().containsKey("naturalnessVerdict"));
        assertFalse(index.get("b").getScores().containsKey("naturalness"));assertTrue(index.get("b").getUnevaluatedBy().contains("naturalness"));
        assertEquals(1.0,b.getScores().get("naturalness"),"Historical input report remains unchanged");
        assertTrue(composer.supportedAnalyzers().containsAll(Set.of("naturalness","a1-didactic")));
    }
}
