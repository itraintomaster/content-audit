package com.learney.contentaudit.auditdomain.pragmatism;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.auditdomain.findingengine.DefaultFindingCollector;
import com.learney.contentaudit.auditdomain.judging.*;
import com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.evaluationledgerdomain.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PragmatismAnalyzerTest {
    static final String NAME = PragmatismAnalyzerProvider.NAME;
    static PragmatismVerdict verdict(String sentence, PragmatismVerdict.Status status) {
        return new PragmatismVerdict(List.of(new PragmatismVerdict.Assessment(sentence, status,
                status == PragmatismVerdict.Status.NOT_APPLICABLE ? "" : "Describir una situación",
                status == PragmatismVerdict.Status.PASS ? "Informar sobre lo observado" : null,
                status == PragmatismVerdict.Status.FAIL ? PragmatismVerdict.Issue.INCOHERENT_SCENARIO : null,
                status == PragmatismVerdict.Status.FAIL ? FindingSeverity.HIGH : null,
                "Razón concreta", status == PragmatismVerdict.Status.NEEDS_CONTEXT ? "Identificar el referente de la metáfora" : null)), "Razón general");
    }
    @Test void flagsIndependentReviewWithoutChangingNaturalnessAndUsesOneBatch() {
        var a=node("a", "The lamp swallowed a calendar."); var b=node("b", "I need a clean towel.");
        a.getScores().put("naturalness", 1.0);
        var f=fixture(List.of(a,b),List.of(result("fail",false),result("pass",true)),
                payload -> verdict(payload.equals("fail") ? "The lamp swallowed a calendar." : "I need a clean towel.",
                        payload.equals("fail") ? PragmatismVerdict.Status.FAIL : PragmatismVerdict.Status.PASS));
        f.analyzer.onQuiz(a);f.analyzer.onQuiz(a);f.analyzer.onQuiz(b);
        f.analyzer.onCourseComplete(f.root);f.analyzer.onCourseComplete(f.root);
        verify(f.session,times(1)).resolveAll(anyList(),eq(2));
        assertEquals(1.0,a.getScores().get("naturalness"));assertEquals(1.0,a.getScores().get(NAME));
        assertEquals(1.0,b.getScores().get(NAME));
        assertEquals("incoherent-scenario",f.analyzer.findingsAt(a).get(0).getRule());
        assertEquals(FindingResolution.RANK_ONLY,f.analyzer.findingsAt(a).get(0).getResolution());
        assertTrue(((DefaultQuizDiagnoses)b.getDiagnoses()).getPragmatismDiagnosis().orElseThrow().reused());
        var tally=ArgumentCaptor.forClass(JudgeUnitTally.class);verify(f.runtime).coverage(any(),tally.capture());
        assertEquals(2,tally.getValue().getEvaluated());assertEquals(Map.of("prag-v1",2),tally.getValue().getVerdictsByVersion());
    }
    @Test void rankOnlyAlertSurvivesTheRealCollectorWithoutLoweringTheScore() {
        var n=node("q", "The lamp swallowed a calendar.");
        var f=fixture(List.of(n),List.of(result("fail",true)),
                ignored -> verdict("The lamp swallowed a calendar.",PragmatismVerdict.Status.FAIL));
        f.root.setChildren(List.of(n));
        f.analyzer.onQuiz(n);f.analyzer.onCourseComplete(f.root);
        var card=new PragmatismAnalyzerProvider(f.runtime,null,ignored -> null,renderer()).describe();
        assertDoesNotThrow(() -> new DefaultFindingCollector().collect(f.root,f.analyzer,card));
        assertEquals(1.0,n.getScores().get(NAME));
        assertEquals(1,n.getFindings().size());
        assertEquals(FindingResolution.RANK_ONLY,n.getFindings().get(0).getResolution());
        assertEquals(FindingSeverity.HIGH,n.getFindings().get(0).getSeverity());
        assertEquals(PragmatismVerdict.Status.FAIL,
                ((DefaultQuizDiagnoses)n.getDiagnoses()).getPragmatismDiagnosis().orElseThrow()
                    .verdict().assessments().get(0).status());
    }
    @Test void abstentionAndNonSentenceArePreservedWithoutPassingScoresOrCoverage() {
        for(var status:List.of(PragmatismVerdict.Status.NEEDS_CONTEXT,PragmatismVerdict.Status.NOT_APPLICABLE)){
            var n=node("q","Silver silence.");n.getScores().put(NAME,1.0);
            var f=fixture(List.of(n),List.of(result("unknown",false)),ignored -> verdict("Silver silence.",status));
            f.analyzer.onQuiz(n);f.analyzer.onCourseComplete(f.root);
            assertFalse(n.getScores().containsKey(NAME));assertTrue(n.getUnevaluatedBy().contains(NAME));
            assertEquals(status,((DefaultQuizDiagnoses)n.getDiagnoses()).getPragmatismDiagnosis().orElseThrow().verdict().assessments().get(0).status());
            var tally=ArgumentCaptor.forClass(JudgeUnitTally.class);verify(f.runtime).coverage(any(),tally.capture());
            assertEquals(0,tally.getValue().getEvaluated());
        }
    }
    @Test void everyAcceptedVariantMustBeAssessedAndForeignSentencesNeverPass() {
        var n=node("q","I need a clean towel.");((AuditableQuiz)n.getEntity()).setSentences(List.of("I need a clean towel.","I need a dry towel."));
        for(String sentence:List.of("I need a clean towel.","Not received.")){
            var f=fixture(List.of(n),List.of(result("bad",false)),ignored -> verdict(sentence,PragmatismVerdict.Status.PASS));
            f.analyzer.onQuiz(n);f.analyzer.onCourseComplete(f.root);
            assertTrue(n.getUnevaluatedBy().contains(NAME));assertFalse(n.getScores().containsKey(NAME));
            assertTrue(((DefaultQuizDiagnoses)n.getDiagnoses()).getPragmatismDiagnosis().isEmpty());
        }
    }
    @Test void noBudgetMissingOrMalformedOutputCannotReuseAnOldPass() {
        var n=node("q","I need a clean towel.");n.getScores().put(NAME,1.0);
        ((DefaultQuizDiagnoses)n.getDiagnoses()).setPragmatismDiagnosis(new PragmatismDiagnosis(verdict("I need a clean towel.",PragmatismVerdict.Status.PASS),"old",true));
        var f=fixture(List.of(n),List.of(result("broken",false)),ignored -> {throw new IllegalArgumentException("Invalid JSON");});
        f.analyzer.onQuiz(n);f.analyzer.onCourseComplete(f.root);
        assertFalse(n.getScores().containsKey(NAME));assertTrue(n.getUnevaluatedBy().contains(NAME));
        assertTrue(((DefaultQuizDiagnoses)n.getDiagnoses()).getPragmatismDiagnosis().isEmpty());
        var missing=fixture(List.of(n),List.of(),ignored -> {throw new AssertionError("Missing output must not be read");});
        missing.analyzer.onQuiz(n);missing.analyzer.onCourseComplete(missing.root);assertFalse(n.getScores().containsKey(NAME));
    }
    @Test void visibleContextAndAllAcceptedSentencesAffectIdentityButLaterFeedbackDoesNot() {
        var n=node("q","A: \"Ready?\"\nB: Yes.");var builder=new PragmatismSubjectBuilder(renderer());var original=builder.build(n).getContent();
        var q=(AuditableQuiz)n.getEntity();q.setTranslation("Otra traducción");q.setMiniTheory("Otra teoría");q.setAnswerAudioKey("other.mp3");
        assertEquals(original,builder.build(n).getContent());
        assertEquals("[\"A: \\\"Ready?\\\"\\nB: Yes.\"]",original.get("acceptedSentences"));
        q.setInstructions("Un cuento sobre objetos que hablan.");assertNotEquals(original,builder.build(n).getContent());
        q.setSentences(List.of());assertThrows(IllegalArgumentException.class,()->builder.build(n));
    }
    @Test void providerHasItsOwnExamAndBudgetAndDoesNotMakeAutomaticCorrectionTasks() {
        var runtime=mock(JudgeRuntime.class);var evaluator=mock(Evaluator.class);var policy=new EvaluationRunPolicy();
        var provider=new PragmatismAnalyzerProvider(runtime,evaluator,ignored -> null,renderer());provider.create(policy);
        verify(runtime).open(NAME,List.of(evaluator),policy);assertTrue(provider.planBinding().isEmpty());
        assertEquals("Pragmatismo comunicativo",provider.describe().getDisplayName());
    }
    record Fixture(PragmatismAnalyzer analyzer,JudgeRuntime runtime,EvaluationSession session,AuditNode root){}
    static Fixture fixture(List<AuditNode> units,List<EvaluationResolution> resolutions,PragmatismVerdictReader reader){
        var runtime=mock(JudgeRuntime.class);var session=mock(EvaluationSession.class);var policy=new EvaluationRunPolicy();policy.setMaxConcurrentEvaluations(2);
        var opened=new JudgeSessions(NAME,"prag-v1",List.of("pragmatism-judge"),List.of(session),null,policy);
        when(runtime.plan(anyList(),eq(policy))).thenReturn(new JudgeRequestPlan(units,Collections.nCopies(units.size(),EvaluationRequestMode.REUSE_OR_EVALUATE),List.of()));
        when(session.resolveAll(anyList(),anyInt())).thenReturn(resolutions);
        var root=new AuditNode();root.setTarget(AuditTarget.COURSE);
        return new Fixture(new PragmatismAnalyzer(opened,runtime,new PragmatismSubjectBuilder(renderer()),reader),runtime,session,root);
    }
    static EvaluationResolution result(String payload,boolean reused){return new EvaluationResolution(reused?EvaluationResolutionKind.REUSED:EvaluationResolutionKind.EVALUATED,payload,"prag-v1",null);}
    static QuizStudentViewRenderer renderer(){return new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create());}
    static AuditNode node(String id,String sentence){
        var quiz=new AuditableQuiz();quiz.setId(id);quiz.setLabel("Descripciones");quiz.setInstructions("Completa la descripción.");quiz.setSentences(List.of(sentence));
        quiz.setSentenceParts(List.of(new SentencePartEntity(SentencePartKind.CLOZE,"",List.of(sentence))));
        return new AuditNode(quiz,AuditTarget.QUIZ,null,List.of(),new LinkedHashMap<>(),new LinkedHashMap<>(),new DefaultQuizDiagnoses());
    }
}
