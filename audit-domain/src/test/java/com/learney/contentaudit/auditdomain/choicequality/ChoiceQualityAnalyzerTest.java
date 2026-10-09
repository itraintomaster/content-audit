package com.learney.contentaudit.auditdomain.choicequality;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.auditdomain.judging.*;
import com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.evaluationledgerdomain.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ChoiceQualityAnalyzerTest {
    static final String NAME = ChoiceQualityAnalyzerProvider.NAME;
    static final ChoiceQualityVerdict PASS = new ChoiceQualityVerdict(ChoiceQualityVerdict.Status.PASS,
            "Inversión de be en presente simple", false, List.of(), "Practica errores de concordancia y orden.");
    static final ChoiceQualityVerdict FAIL = new ChoiceQualityVerdict(ChoiceQualityVerdict.Status.FAIL,
            "Inversión de be en presente simple", false, List.of(new ChoiceQualityVerdict.Issue(
            ChoiceQualityVerdict.Kind.ANSWER_CUE, FindingSeverity.HIGH, List.of("Is he", "Am I"),
            "La pista he permite elegir sin invertir be.", "Orden de be y concordancia con he.")), "La pista regala la respuesta.");
    static final ChoiceQualityVerdict UNKNOWN = new ChoiceQualityVerdict(ChoiceQualityVerdict.Status.NEEDS_CONTEXT,
            "", false, List.of(), "Falta una consigna con objetivo.");

    @Test void batchesOnlyMultipleChoiceAndKeepsValidityUntouched() {
        var fail = node("q1"); var pass = node("q2"); var nonChoice = node("q3");
        ((AuditableQuiz) nonChoice.getEntity()).setMultipleChoice(null);
        fail.getScores().put("multiple-choice-validity", 0.7);
        var fixture = fixture(List.of(fail, pass), List.of(result("fail", false), result("pass", true)));
        fixture.analyzer.onQuiz(fail); fixture.analyzer.onQuiz(fail);
        fixture.analyzer.onQuiz(nonChoice); fixture.analyzer.onQuiz(pass);
        fixture.analyzer.onCourseComplete(fixture.root); fixture.analyzer.onCourseComplete(fixture.root);
        verify(fixture.session, times(1)).resolveAll(anyList(), eq(2));
        assertEquals(0.3, fail.getScores().get(NAME));
        assertEquals(0.7, fail.getScores().get("multiple-choice-validity"));
        assertEquals(1.0, pass.getScores().get(NAME));
        assertFalse(nonChoice.getScores().containsKey(NAME));
        var finding = fixture.analyzer.findingsAt(fail).get(0);
        assertEquals("answer-cue", finding.getRule());
        assertEquals(FindingResolution.PANEL, finding.getResolution());
        assertTrue(finding.getEvidence().getObservation().contains("he"));
        assertTrue(((DefaultQuizDiagnoses) pass.getDiagnoses()).getChoiceQualityDiagnosis().orElseThrow().reused());
        var tally = ArgumentCaptor.forClass(JudgeUnitTally.class);
        verify(fixture.runtime).coverage(any(), tally.capture());
        assertEquals(2, tally.getValue().getEvaluated());
        assertEquals(Map.of("quality-v1", 2), tally.getValue().getVerdictsByVersion());
    }

    @Test void unknownContextRetainsRationaleButNeverPassesCoverage() {
        var node = node("q1");
        var fixture = fixture(List.of(node), List.of(result("unknown", false)));
        fixture.analyzer.onQuiz(node); fixture.analyzer.onCourseComplete(fixture.root);
        assertFalse(node.getScores().containsKey(NAME));
        assertTrue(node.getUnevaluatedBy().contains(NAME));
        assertTrue(fixture.analyzer.findingsAt(node).isEmpty());
        assertEquals(UNKNOWN, ((DefaultQuizDiagnoses) node.getDiagnoses()).getChoiceQualityDiagnosis().orElseThrow().verdict());
        var tally = ArgumentCaptor.forClass(JudgeUnitTally.class);
        verify(fixture.runtime).coverage(any(), tally.capture());
        assertEquals(0, tally.getValue().getEvaluated());
        assertEquals(1, tally.getValue().getNotEvaluated());
    }

    @Test void missingAndMalformedVerdictsRemoveStaleScoresAndDiagnosis() {
        var bad = node("bad"); var missing = node("missing");
        for (var node : List.of(bad, missing)) {
            node.getScores().put(NAME, 1.0);
            ((DefaultQuizDiagnoses) node.getDiagnoses()).setChoiceQualityDiagnosis(new ChoiceQualityDiagnosis(PASS, "old", true));
        }
        var fixture = fixture(List.of(bad, missing), List.of(result("broken", false)));
        fixture.analyzer.onQuiz(bad); fixture.analyzer.onQuiz(missing); fixture.analyzer.onCourseComplete(fixture.root);
        for (var node : List.of(bad, missing)) {
            assertFalse(node.getScores().containsKey(NAME));
            assertTrue(node.getUnevaluatedBy().contains(NAME));
            assertTrue(((DefaultQuizDiagnoses) node.getDiagnoses()).getChoiceQualityDiagnosis().isEmpty());
        }
    }

    @Test void anIssueCannotNameAnOptionThatDoesNotExist() {
        var node = node("q1");
        var fixture = fixture(List.of(node), List.of(result("foreign", false)));
        fixture.analyzer.onQuiz(node); fixture.analyzer.onCourseComplete(fixture.root);
        assertTrue(node.getUnevaluatedBy().contains(NAME));
        assertFalse(node.getScores().containsKey(NAME));
    }

    @Test void unavailableOrBlockedSessionIsNotInventedAsPass() {
        var node = node("q1"); var fixture = fixture(List.of(node), List.of());
        when(fixture.session.resolveAll(anyList(), anyInt())).thenThrow(new IllegalStateException("No budget/exam"));
        fixture.analyzer.onQuiz(node); fixture.analyzer.onCourseComplete(fixture.root);
        assertTrue(node.getUnevaluatedBy().contains(NAME));
        assertTrue(fixture.analyzer.findingsAt(node).isEmpty());
    }

    @Test void visibleContextChangesIdentityButLaterExplanationsDoNot() {
        var node = node("q1"); var builder = new ChoiceQualitySubjectBuilder(renderer());
        var original = builder.build(node).getContent();
        var quiz = (AuditableQuiz) node.getEntity();
        quiz.setMiniTheory("Otra explicación"); quiz.setTranslation("Otra traducción"); quiz.setAnswerAudioKey("otra.mp3");
        assertEquals(original, builder.build(node).getContent());
        quiz.setInstructions("Reconoce solamente el sujeto indicado.");
        assertNotEquals(original, builder.build(node).getContent());
        assertEquals("A1", original.get("level"));
        assertEquals("Be: preguntas yes/no", original.get("topic"));
        assertEquals(Set.of("level", "topic", "title", "instructions", "exercise", "options"), original.keySet());
        assertThrows(IllegalArgumentException.class, () -> builder.build(new AuditNode()));
    }

    @Test void providerOpensSeparateBudgetAndPanelCanClarifyOnlyVisibleContext() {
        var runtime = mock(JudgeRuntime.class); var evaluator = mock(Evaluator.class); var policy = new EvaluationRunPolicy();
        var provider = new ChoiceQualityAnalyzerProvider(runtime, evaluator, ignored -> PASS, renderer());
        provider.create(policy);
        verify(runtime).open(NAME, List.of(evaluator), policy);
        assertEquals("MULTIPLE_CHOICE_VALIDITY", provider.planBinding().orElseThrow().getTaskKind());
        assertEquals(AnalysisCost.PAID_MODEL, provider.describe().getCost());
        var correction = new ChoiceQualityCorrection();
        assertTrue(correction.editScope(FindingResolution.PANEL).containsAll(List.of(ExercisePart.OPTIONS, ExercisePart.INSTRUCTIONS)));
        assertFalse(correction.editScope(FindingResolution.PANEL).contains(ExercisePart.MINI_THEORY));
        assertTrue(correction.editScope(FindingResolution.RULE).isEmpty());
    }

    private record Fixture(ChoiceQualityAnalyzer analyzer, JudgeRuntime runtime, EvaluationSession session, AuditNode root) {}
    private static Fixture fixture(List<AuditNode> units, List<EvaluationResolution> resolutions) {
        var runtime = mock(JudgeRuntime.class); var session = mock(EvaluationSession.class);
        var policy = new EvaluationRunPolicy(); policy.setMaxConcurrentEvaluations(2);
        var opened = new JudgeSessions(NAME, "quality-v1", List.of("quality"), List.of(session), null, policy);
        when(runtime.plan(anyList(), eq(policy))).thenReturn(new JudgeRequestPlan(units,
                Collections.nCopies(units.size(), EvaluationRequestMode.REUSE_OR_EVALUATE), List.of()));
        when(session.resolveAll(anyList(), anyInt())).thenReturn(resolutions);
        ChoiceQualityVerdictReader reader = payload -> switch (payload) {
            case "pass" -> PASS; case "fail" -> FAIL; case "unknown" -> UNKNOWN;
            case "foreign" -> new ChoiceQualityVerdict(ChoiceQualityVerdict.Status.FAIL, "Objetivo", false,
                    List.of(new ChoiceQualityVerdict.Issue(ChoiceQualityVerdict.Kind.ANSWER_CUE,
                    FindingSeverity.HIGH, List.of("No existe"), "Razón", "Confusión")), "Razón");
            default -> throw new IllegalArgumentException("bad JSON");
        };
        var root = new AuditNode(); root.setTarget(AuditTarget.COURSE);
        return new Fixture(new ChoiceQualityAnalyzer(opened, runtime, new ChoiceQualitySubjectBuilder(renderer()), reader), runtime, session, root);
    }
    private static EvaluationResolution result(String payload, boolean reused) {
        return new EvaluationResolution(reused ? EvaluationResolutionKind.REUSED : EvaluationResolutionKind.EVALUATED, payload, "quality-v1", null);
    }
    private static QuizStudentViewRenderer renderer() {
        return new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create());
    }
    private static AuditNode node(String id) {
        var quiz = new AuditableQuiz(List.of(), id, "Be: preguntas yes/no", "A1-BE", "¿Él es un niño?",
                List.of("Is he a child?"), null, "Forma preguntas en presente simple invirtiendo be y el sujeto.",
                List.of(new SentencePartEntity(SentencePartKind.CLOZE, "", null),
                        new SentencePartEntity(SentencePartKind.TEXT, "a child? (he)", null)),
                new MultipleChoiceEntity("SINGLE", List.of(new MultipleChoiceItemEntity("i1", 0.0, "Am I"),
                        new MultipleChoiceItemEntity("i2", 1.0, "Is he"))), "Oculta", "audio.mp3");
        var milestone = new AuditableMilestone(); milestone.setLabel("A1");
        var milestoneNode = new AuditNode(); milestoneNode.setTarget(AuditTarget.MILESTONE); milestoneNode.setEntity(milestone);
        var topic = new AuditableKnowledge(); topic.setTitle("Be: preguntas yes/no");
        var parent = new AuditNode(); parent.setTarget(AuditTarget.KNOWLEDGE); parent.setEntity(topic); parent.setParent(milestoneNode);
        return new AuditNode(quiz, AuditTarget.QUIZ, parent, List.of(), new LinkedHashMap<>(), new LinkedHashMap<>(), new DefaultQuizDiagnoses());
    }
}
