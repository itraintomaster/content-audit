package com.learney.contentaudit.auditdomain.englishquality;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.finishedsentence.*;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.auditdomain.findingengine.*;
import com.learney.contentaudit.auditdomain.judging.*;
import com.learney.contentaudit.auditdomain.studentview.*;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.evaluationledgerdomain.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnglishQualityTest {
    final EnglishQualitySubjectBuilder builder=new EnglishQualitySubjectBuilder(
            new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create()),new DefaultFinishedSentenceReader());
    static AuditNode node(String id,String level,String... variants) {
        var root=new AuditNode(null,AuditTarget.COURSE,null,new ArrayList<>(),new LinkedHashMap<>(),new LinkedHashMap<>(),new DefaultCourseDiagnoses());
        var m=new AuditableMilestone();m.setId("m"+id);m.setLabel(level);
        var milestone=new AuditNode(m,AuditTarget.MILESTONE,root,new ArrayList<>(),new LinkedHashMap<>(),new LinkedHashMap<>(),null);root.getChildren().add(milestone);
        var t=new AuditableTopic();t.setId("t"+id);t.setLabel("Present Simple");
        var topic=new AuditNode(t,AuditTarget.TOPIC,milestone,new ArrayList<>(),new LinkedHashMap<>(),new LinkedHashMap<>(),null);milestone.getChildren().add(topic);
        var k=new AuditableKnowledge();k.setId("k"+id);k.setLabel("Questions");k.setSentenceMode(SentenceMode.REWRITE);
        var knowledge=new AuditNode(k,AuditTarget.KNOWLEDGE,topic,new ArrayList<>(),new LinkedHashMap<>(),new LinkedHashMap<>(),null);topic.getChildren().add(knowledge);
        var q=new AuditableQuiz();q.setId(id);q.setLabel("Questions");q.setInstructions("Escribe la oración.");q.setSentences(List.of(variants));
        q.setSentenceParts(List.of(new SentencePartEntity(SentencePartKind.CLOZE,"",List.of(variants))));
        var node=new AuditNode(q,AuditTarget.QUIZ,knowledge,new ArrayList<>(),new LinkedHashMap<>(),new LinkedHashMap<>(),new DefaultQuizDiagnoses());knowledge.getChildren().add(node);return node;
    }
    static NativeUsageVerdict.Assessment a(String sentence,NativeUsageVerdict.Status status) {
        return new NativeUsageVerdict.Assessment(sentence,NativeUsageVerdict.Scope.SENTENCE,status,"Significado concreto",
                status==NativeUsageVerdict.Status.FAIL?sentence:null,status==NativeUsageVerdict.Status.FAIL?NativeUsageVerdict.Issue.GRAMMAR:null,
                status==NativeUsageVerdict.Status.FAIL?FindingSeverity.HIGH:null,"Razón lingüística",
                status==NativeUsageVerdict.Status.REVIEW?"Comprobar lectura":null,null,null);
    }
    @Test void doesNotDropCommunicativeMaterialWithoutVerbPosAndReadsEveryVariant() {
        for(var s:List.of("She sports hats.","My father's reading now.","The boys worried about their problem.","What about those things?","I really like pizza."))
            assertEquals(List.of(s),builder.sentences(node("n","A1",s)));
        var n=node("forms","A1","I haven't any money now.","I have not any money now.");
        assertEquals(2,builder.sentences(n).size());
        assertTrue(builder.build(n).getContent().get("acceptedSentences").contains("I have not any money now."));
    }
    @Test void acceptedClozeOptionIsNotInventedAsVisibleAdverbCue() {
        var n=node("breakfast","A1","Peter always has breakfast at home.");var q=(AuditableQuiz)n.getEntity();
        q.setSentenceParts(List.of(new SentencePartEntity(SentencePartKind.TEXT,"Peter",null),
                new SentencePartEntity(SentencePartKind.CLOZE,"",List.of("always")),new SentencePartEntity(SentencePartKind.TEXT,"has breakfast at home.",null)));
        var c=builder.build(n).getContent();
        assertEquals("Peter ___ has breakfast at home.",c.get("exercise"));assertEquals("[]",c.get("visibleChoices"));
        assertTrue(c.get("renderCompleteness").contains("not visible clues"));
    }
    @Test void readsAllPositiveIncidenceChoicesWithoutJudgingDistractors() {
        var n=node("choices","A1","I eat.");var q=(AuditableQuiz)n.getEntity();
        q.setMultipleChoice(new MultipleChoiceEntity("SINGLE",List.of(
                new MultipleChoiceItemEntity("first",1.0,"I eat."),
                new MultipleChoiceItemEntity("second",0.5,"I drink."),
                new MultipleChoiceItemEntity("wrong",0.0,"I wrong."))));
        assertEquals(List.of("I eat.","I drink."),builder.sentences(n));
        assertTrue(builder.build(n).getContent().get("visibleChoices").contains("I wrong."));
        assertFalse(builder.build(n).getContent().get("acceptedSentences").contains("I wrong."));
    }
    @Test void neverApprovesAnImplicitlyTruncatedCombinationSet() {
        var n=node("many","A1","I eat.");var q=(AuditableQuiz)n.getEntity();
        q.setSentenceParts(List.of(new SentencePartEntity(SentencePartKind.CLOZE,"",java.util.stream.IntStream.range(0,65).mapToObj(i->"word"+i).toList())));
        assertThrows(IllegalArgumentException.class,()->builder.build(n));
    }
    @Test void invalidFailureAndUncertainAsApprovalAreRejected() {
        assertThrows(IllegalArgumentException.class,()->new NativeUsageVerdict.Assessment("I eat.",NativeUsageVerdict.Scope.SENTENCE,
                NativeUsageVerdict.Status.FAIL,"Comer","not in sentence",NativeUsageVerdict.Issue.GRAMMAR,FindingSeverity.HIGH,"Razón",null,null,null));
        assertThrows(IllegalArgumentException.class,()->new NativeUsageVerdict.Assessment("I eat.",NativeUsageVerdict.Scope.SENTENCE,
                NativeUsageVerdict.Status.REVIEW,null,null,null,null,"Falta evidencia",null,null,null));
        assertThrows(IllegalArgumentException.class,()->new NativeUsageVerdict(List.of(a("I eat.",NativeUsageVerdict.Status.PASS),a("I eat.",NativeUsageVerdict.Status.PASS)),"R"));
        assertThrows(IllegalArgumentException.class,()->new A1DidacticVerdict("q",A1DidacticVerdict.Status.SUITABLE,"Objetivo",List.of(),null,"R",null,List.of(),"Cambiar",null));
    }
    @Test void equivalentContractionsMustNotReceiveConflictingVerdicts() {
        assertThrows(IllegalArgumentException.class,()->EnglishQualityAnalyzer.validateEquivalentVariants(new NativeUsageVerdict(List.of(
                a("I'll close the window.",NativeUsageVerdict.Status.PASS),a("I will close the window.",NativeUsageVerdict.Status.FAIL)),"R")));
        assertDoesNotThrow(()->EnglishQualityAnalyzer.validateEquivalentVariants(new NativeUsageVerdict(List.of(
                a("I haven't any money now.",NativeUsageVerdict.Status.PASS),a("I have not any money now.",NativeUsageVerdict.Status.PASS)),"R")));
    }
    @Test void reviewCanRetainLiteralEvidenceWithoutClaimingAnError() {
        var assessment = new NativeUsageVerdict.Assessment("How many is it?", NativeUsageVerdict.Scope.SENTENCE,
                NativeUsageVerdict.Status.REVIEW, "Preguntar por un total", "How many is it", null, null,
                "Falta referencia", "Precisar si it es un total", null, null);
        assertEquals(NativeUsageVerdict.Status.REVIEW, assessment.status());
        assertNull(assessment.issue()); assertNull(assessment.severity());
        assertThrows(IllegalArgumentException.class, () -> new NativeUsageVerdict.Assessment("How many is it?",
                NativeUsageVerdict.Scope.SENTENCE, NativeUsageVerdict.Status.REVIEW, null, "missing", null, null,
                "Falta referencia", "Comprobar", null, null));
    }
    record Fixture(EnglishQualityAnalyzer analyzer,EvaluationSession session,AuditNode root) {}
    Fixture fixture(EnglishQualityAnalyzerProvider.Kind kind,List<AuditNode> nodes,EnglishQualityVerdictReader reader) {
        var runtime=mock(JudgeRuntime.class);var opened=mock(JudgeSessions.class);var session=mock(EvaluationSession.class);
        when(opened.getSessions()).thenReturn(List.of(session));
        when(runtime.plan(anyList(),any())).thenReturn(new JudgeRequestPlan(nodes,Collections.nCopies(nodes.size(),EvaluationRequestMode.REUSE_OR_EVALUATE),List.of()));
        when(session.resolveAll(anyList(),anyInt())).thenReturn(nodes.stream().map(n->new EvaluationResolution(EvaluationResolutionKind.EVALUATED,n.getEntity().getId(),"new-version",null)).toList());
        var root=nodes.get(0);while(root.getParent()!=null)root=root.getParent();
        return new Fixture(new EnglishQualityAnalyzer(kind,opened,runtime,builder,reader),session,root);
    }
    @Test void scoresFollowVerdictsAndPendingNeverTurnsIntoConstantHundredPercent() {
        var p=node("pass","A1","I eat.");var f=node("fail","A1","I wrong.");var r=node("review","A1","I think.");
        var reader=mock(EnglishQualityVerdictReader.class);
        when(reader.readNaturalness("pass")).thenReturn(new NativeUsageVerdict(List.of(a("I eat.",NativeUsageVerdict.Status.PASS)),"R"));
        when(reader.readNaturalness("fail")).thenReturn(new NativeUsageVerdict(List.of(a("I wrong.",NativeUsageVerdict.Status.FAIL)),"R"));
        when(reader.readNaturalness("review")).thenReturn(new NativeUsageVerdict(List.of(a("I think.",NativeUsageVerdict.Status.REVIEW)),"R"));
        var fixture=fixture(EnglishQualityAnalyzerProvider.Kind.NATURALNESS,List.of(p,f,r),reader);
        for(var n:List.of(p,f,r))fixture.analyzer.onQuiz(n);
        fixture.analyzer.onCourseComplete(fixture.root);fixture.analyzer.onCourseComplete(fixture.root);
        assertEquals(1.0,p.getScores().get("naturalness"));assertEquals(0.0,f.getScores().get("naturalness"));
        assertFalse(r.getScores().containsKey("naturalness"));assertTrue(r.getUnevaluatedBy().contains("naturalness"));
        assertEquals(1,fixture.analyzer.findingsAt(f).size());verify(fixture.session,times(1)).resolveAll(anyList(),anyInt());
        collect(fixture.analyzer,f,EnglishQualityAnalyzerProvider.Kind.NATURALNESS);
        assertEquals(FindingResolution.PANEL,f.getFindings().get(0).getResolution());
    }
    @Test void missingAcceptedVariantInvalidatesEntireOutputAndLeavesNoApproval() {
        var n=node("q","A1","I am here.","I'm here.");var reader=mock(EnglishQualityVerdictReader.class);
        when(reader.readNaturalness(anyString())).thenReturn(new NativeUsageVerdict(List.of(a("I am here.",NativeUsageVerdict.Status.PASS)),"R"));
        var f=fixture(EnglishQualityAnalyzerProvider.Kind.NATURALNESS,List.of(n),reader);f.analyzer.onQuiz(n);f.analyzer.onCourseComplete(f.root);
        assertFalse(n.getScores().containsKey("naturalness"));assertTrue(n.getUnevaluatedBy().contains("naturalness"));assertTrue(n.getMetadata().containsKey("naturalnessOutputIssue"));
    }
    @Test void didacticImprovementDoesNotInvalidateNaturalnessAndHandoffIsNotDuplicated() {
        var n=node("q","A1","I eat.");n.getScores().put("naturalness",1.0);var reader=mock(EnglishQualityVerdictReader.class);
        when(reader.readDidactic(anyString())).thenReturn(new A1DidacticVerdict("q",A1DidacticVerdict.Status.IMPROVE,"Simple",List.of("I eat."),null,"Mejorar escena",null,List.of(A1DidacticVerdict.Owner.PRAGMATISM),"Cambiar escena",null));
        var f=fixture(EnglishQualityAnalyzerProvider.Kind.A1_DIDACTIC,List.of(n),reader);f.analyzer.onQuiz(n);f.analyzer.onCourseComplete(f.root);
        assertEquals(1.0,n.getScores().get("naturalness"));assertEquals(1.0,n.getScores().get("a1-didactic"));assertTrue(f.analyzer.findingsAt(n).isEmpty());
        collect(f.analyzer,n,EnglishQualityAnalyzerProvider.Kind.A1_DIDACTIC);
        assertTrue(n.getFindings().isEmpty());
    }
    @Test void didacticOwnDefectReachesCollectorWithoutChangingAcceptedEnglish() {
        var n=node("q","A1","I eat.");n.getScores().put("naturalness",1.0);var reader=mock(EnglishQualityVerdictReader.class);
        when(reader.readDidactic(anyString())).thenReturn(new A1DidacticVerdict("q",A1DidacticVerdict.Status.IMPROVE,"Simple",List.of("I eat."),A1DidacticVerdict.Issue.OPAQUE_EXAMPLE,"Aclarar contexto",null,List.of(),"Añadir contexto",null));
        var f=fixture(EnglishQualityAnalyzerProvider.Kind.A1_DIDACTIC,List.of(n),reader);f.analyzer.onQuiz(n);f.analyzer.onCourseComplete(f.root);
        collect(f.analyzer,n,EnglishQualityAnalyzerProvider.Kind.A1_DIDACTIC);
        assertEquals(1.0,n.getScores().get("naturalness"));assertEquals(0.0,n.getScores().get("a1-didactic"));
        assertEquals("a1-didactic",n.getFindings().get(0).getAnalyzer());assertEquals(FindingResolution.PANEL,n.getFindings().get(0).getResolution());
    }
    @Test void didacticDefectInVisibleStimulusCanHaveNoAffectedAcceptedSentence() {
        var n=node("q","A1","Don't take the wrong train.");var reader=mock(EnglishQualityVerdictReader.class);
        var verdict=new A1DidacticVerdict("q",A1DidacticVerdict.Status.IMPROVE,"Imperativos",List.of(),
                A1DidacticVerdict.Issue.INCIDENTAL_STRUCTURE,"El estímulo introduce un modal no enseñado",null,
                List.of(),"Cambiar sólo el estímulo",null);
        when(reader.readDidactic(anyString())).thenReturn(verdict);
        var f=fixture(EnglishQualityAnalyzerProvider.Kind.A1_DIDACTIC,List.of(n),reader);
        f.analyzer.onQuiz(n);f.analyzer.onCourseComplete(f.root);
        assertDoesNotThrow(()->collect(f.analyzer,n,EnglishQualityAnalyzerProvider.Kind.A1_DIDACTIC));
        assertEquals(0.0,n.getScores().get("a1-didactic"));
        assertSame(verdict,n.getMetadata().get("a1-didacticVerdict"));
        assertTrue(verdict.affectedSentences().isEmpty());
        assertEquals(builder.build(n).getContent().get("exercise"),n.getFindings().get(0).getIdentity().getMarker());
    }
    @Test void findingOmitsAbsentOptionalInstructionWithoutDiscardingDefect() {
        var n=node("q","A1","I eat.");((AuditableQuiz)n.getEntity()).setInstructions("");
        var reader=mock(EnglishQualityVerdictReader.class);
        when(reader.readNaturalness(anyString())).thenReturn(new NativeUsageVerdict(List.of(a("I eat.",NativeUsageVerdict.Status.FAIL)),"R"));
        var f=fixture(EnglishQualityAnalyzerProvider.Kind.NATURALNESS,List.of(n),reader);
        f.analyzer.onQuiz(n);f.analyzer.onCourseComplete(f.root);
        assertDoesNotThrow(()->collect(f.analyzer,n,EnglishQualityAnalyzerProvider.Kind.NATURALNESS));
        assertEquals(0.0,n.getScores().get("naturalness"));
        assertFalse(n.getFindings().isEmpty());
        assertTrue(n.getFindings().get(0).getEvidence().getExamined().stream()
                .allMatch(part->part.getText()!=null && !part.getText().isBlank()));
    }
    void collect(ContentAnalyzer analyzer,AuditNode n,EnglishQualityAnalyzerProvider.Kind kind) {
        var provider=new EnglishQualityAnalyzerProvider(kind,mock(JudgeRuntime.class),null,mock(EnglishQualityVerdictReader.class),
                new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create()),new DefaultFinishedSentenceReader());
        var collector=new DefaultFindingCollector();
        collector.collect(n,analyzer,provider.describe());
    }
    @Test void didacticDoesNotReachA2() {
        var n=node("a2","A2","I eat.");var runtime=mock(JudgeRuntime.class);
        when(runtime.plan(anyList(),any())).thenAnswer(call -> {
            assertTrue(((List<?>)call.getArgument(0)).isEmpty());
            return new JudgeRequestPlan(List.of(),List.of(),List.of());
        });
        var analyzer=new EnglishQualityAnalyzer(EnglishQualityAnalyzerProvider.Kind.A1_DIDACTIC,null,runtime,builder,mock(EnglishQualityVerdictReader.class));
        analyzer.onQuiz(n);analyzer.onCourseComplete(n);
        verify(runtime).plan(anyList(),any());assertFalse(n.getScores().containsKey("a1-didactic"));
    }
}
