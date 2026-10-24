package com.learney.contentaudit.revisiondomain.batchengine;

import static org.junit.jupiter.api.Assertions.*;
import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.contextnumbers.*;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.revisiondomain.batch.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class EvolutionProjectionTest {
    @Test void comparisonSummaryKeepsSelectedStateAndLevelButNeverCopiesFullEvidenceOrProvenance() {
        var course = TandaFixture.course();
        String quiz = course.getRoot().getMilestones().get(0).getTopics().get(0).getKnowledges().get(0).getQuizTemplates().get(0).getId();
        String other = course.getRoot().getMilestones().get(1).getTopics().get(0).getKnowledges().get(0).getQuizTemplates().get(0).getId();
        var finding = new Finding("judge","problem",new FindingNodeRef(AuditTarget.QUIZ,quiz,"Quiz"),FindingSeverity.HIGH,
                new FindingEvidence(List.of(new EvidencePart()),"Detailed observation",List.of()),FindingResolution.PANEL,
                null,new FindingIdentity("stable-key","marker"));
        var first = new DigestNode(quiz,AuditTarget.QUIZ,"Quiz",null,List.of(finding),List.of(),List.of(),List.of());
        var second = new DigestNode(other,AuditTarget.QUIZ,"Other level",null,List.of(finding),List.of(),List.of(),List.of());
        var root = new DigestNode(course.getId(),AuditTarget.COURSE,"Course",null,List.of(),List.of(),List.of(first,second),List.of());
        var digest = new AuditDigest("audit",root);
        var origin = new LinkedHashMap<String,Object>(); origin.put("judgeVersion","v1"); origin.put("runtime","provider/model");
        origin.put("studentView",Map.of("large","full quiz")); origin.put("history",List.of("full run history"));
        Map<String,Map<String,Object>> provenance = Map.of(quiz,Map.of("judge",origin),other,Map.of("judge",origin));
        Map<String,Map<String,Map<String,Object>>> marks = Map.of(quiz,Map.of("judge",Map.of(
                "judgeVersion","v1","runtimeName","provider/model","abstention",false,"fullVerdict","large evidence")));
        var base = new CurrentAnalysisService.State("base","Base","fingerprint",digest,Map.of(),provenance,List.of(),Map.of(),"A0",Map.of(),List.of(),marks);
        var correction = new CurrentAnalysisService.State("correction","Corrección 1","fingerprint",digest,Map.of(),provenance,List.of(),Map.of(),"A1",Map.of(),List.of(),marks);
        var source = new CurrentAnalysisService.View("batch",1,3,List.of(base,correction),List.of(),List.of(),List.of("judge"));
        var summary = EvolutionProjection.analysis(source,"correction",DefaultCurrentAnalysisService.cut(course,CefrLevel.A1));
        assertEquals(1,summary.states().size()); assertEquals("correction",summary.states().get(0).id());
        var state = summary.states().get(0); assertEquals(1,state.digest().getRoot().getChildren().size());
        assertEquals(quiz,state.digest().getRoot().getChildren().get(0).getNodeId());
        var shortFinding = state.digest().getRoot().getChildren().get(0).getFindings().get(0);
        assertTrue(shortFinding.getEvidence().getExamined().isEmpty());
        assertEquals("stable-key",shortFinding.getIdentity().getKey());
        assertEquals("Detailed observation",shortFinding.getEvidence().getObservation());
        assertEquals(Set.of("judgeVersion","runtime"),((Map<?,?>)state.provenance().get(quiz).get("judge")).keySet());
        assertFalse(state.provenance().containsKey(other)); assertFalse(state.verdictMarks().get(quiz).get("judge").containsKey("fullVerdict"));
        assertEquals(1,finding.getEvidence().getExamined().size(),"Original APIs retain their full evidence");
        assertEquals(2,source.states().size()); assertTrue(summary.looseRuns().isEmpty()); assertTrue(summary.history().isEmpty());
    }
}
