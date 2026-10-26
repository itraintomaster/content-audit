package com.learney.contentaudit.auditdomain.answergiveaway;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.auditdomain.findingengine.DefaultFindingCollector;
import com.learney.contentaudit.auditdomain.judging.*;
import com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.evaluationledgerdomain.*;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** FEAT-REGALA: the judge «no regala la respuesta», with invented exercises; the exam ones never enter here. */
class AnswerGiveawayAnalyzerTest {
    static final String NAME = AnswerGiveawayAnalyzerProvider.NAME;
    static final String THIRSTY = "Completa con will; acabas de decidirlo, no lo tenías planeado.";

    @Test
    @DisplayName("should read the name of the topic, the instructions and the statement with its hint as the student sees them, and the accepted answers, but not the translation, the mini theory or the audio")
    @Tag("FEAT-REGALA")
    @Tag("F-REGALA-R001")
    void shouldReadWhatTheStudentSeesBeforeAnsweringAndTheAcceptedAnswersButNotWhatComesAfter() {
        AuditNode node = cloze("q", THIRSTY);
        var builder = new AnswerGiveawaySubjectBuilder(renderer());

        Map<String, String> content = builder.build(node).getContent();

        assertEquals("A1", content.get("level"));
        assertEquals("El futuro", content.get("topic"));
        assertEquals("Will para decidir en el momento", content.get("title"));
        assertEquals(THIRSTY, content.get("instructions"));
        assertEquals("FILL", content.get("sentenceMode"));
        assertEquals("I'm thirsty. I ___ (get) some water.", content.get("exercise"));
        assertEquals("Hueco 1: 'll get | will get", content.get("acceptedAnswers"));
        assertEquals("", content.get("visibleChoices"));
        var quiz = (AuditableQuiz) node.getEntity();
        quiz.setTranslation("Tengo sed. Voy a buscar agua.");
        quiz.setMiniTheory("Usá $will$ para lo que decidís al hablar.");
        quiz.setAnswerAudioKey("other.mp3");
        assertEquals(content, builder.build(node).getContent(), "what comes after answering is not read");
        quiz.setInstructions("Completa con will.");
        assertNotEquals(content, builder.build(node).getContent(), "the instructions are read");
    }

    @Test
    @DisplayName("should reach a multiple choice too, with the options the student sees and which one is correct")
    @Tag("FEAT-REGALA")
    @Tag("F-REGALA-R001")
    void shouldReachAMultipleChoiceWithTheOptionsTheStudentSeesAndWhichOneIsCorrect() {
        AuditNode node = multipleChoice("m", "Elige la pregunta correcta.");
        var f = fixture(List.of(node), List.of(result("ok")), ignored -> ok("m"));

        f.analyzer.onQuiz(node);
        f.analyzer.onCourseComplete(f.root);

        Map<String, String> content = new AnswerGiveawaySubjectBuilder(renderer()).build(node).getContent();
        assertEquals("- Is she\n- She is\n- Are she", content.get("visibleChoices"));
        assertEquals("Opción correcta: Is she", content.get("acceptedAnswers"));
        verify(f.session).resolveAll(argThat(requests -> requests.size() == 1), anyInt());
        assertEquals(1.0, node.getScores().get(NAME));
    }

    @Test
    @DisplayName("should mark with the exact fragment instructions that give away the whole decision, HIGH, for the panel")
    @Tag("FEAT-REGALA")
    @Tag("F-REGALA-R002")
    @Tag("F-REGALA-R003")
    void shouldMarkWithTheExactFragmentInstructionsThatGiveAwayTheWholeDecisionAsHighForThePanel() {
        AuditNode node = cloze("q", THIRSTY);
        var f = fixture(List.of(node), List.of(result("gives")), ignored -> givesAway("q",
                AnswerGiveawayVerdict.Where.INSTRUCTIONS, "acabas de decidirlo, no lo tenías planeado",
                AnswerGiveawayVerdict.Extent.WHOLE));
        f.root.setChildren(List.of(node));

        f.analyzer.onQuiz(node);
        f.analyzer.onCourseComplete(f.root);
        new DefaultFindingCollector().collect(f.root, f.analyzer, provider(f.runtime).describe());

        assertEquals(0.0, node.getScores().get(NAME));
        assertFalse(node.getUnevaluatedBy().contains(NAME));
        Finding finding = node.getFindings().get(0);
        assertEquals("gives-away-the-decision", finding.getRule());
        assertEquals("acabas de decidirlo, no lo tenías planeado", finding.getIdentity().getMarker());
        assertEquals(FindingSeverity.HIGH, finding.getSeverity());
        assertEquals(FindingResolution.PANEL, finding.getResolution());
        assertTrue(finding.getEvidence().getObservation().contains("Propuesta: poné el contexto en la oración"),
                finding.getEvidence().getObservation());
        assertEquals("v1", node.getMetadata().get(NAME + "Version"));
        assertEquals(AnswerGiveawaySubjectBuilder.CONTRACT, node.getMetadata().get(NAME + "InputContract"));
    }

    @Test
    @DisplayName("should mark a direction written in the statement that gives away a part of the decision, MEDIUM")
    @Tag("FEAT-REGALA")
    @Tag("F-REGALA-R002")
    @Tag("F-REGALA-R003")
    void shouldMarkADirectionWrittenInTheStatementThatGivesAwayAPartAsMedium() {
        AuditNode node = quiz("q", "Completa la oración.", List.of(text("She ___"), cloze("doesn't like"),
                text("fish. (Usa doesn't con she.)")), null);
        var f = fixture(List.of(node), List.of(result("part")), ignored -> givesAway("q",
                AnswerGiveawayVerdict.Where.STATEMENT, "(Usa doesn't con she.)", AnswerGiveawayVerdict.Extent.PART));
        f.root.setChildren(List.of(node));

        f.analyzer.onQuiz(node);
        f.analyzer.onCourseComplete(f.root);
        new DefaultFindingCollector().collect(f.root, f.analyzer, provider(f.runtime).describe());

        assertEquals(FindingSeverity.MEDIUM, node.getFindings().get(0).getSeverity());
        assertEquals("(Usa doesn't con she.)", node.getFindings().get(0).getIdentity().getMarker());
    }

    @Test
    @DisplayName("should pass instructions that only name the task, with score 1 and no finding")
    @Tag("FEAT-REGALA")
    @Tag("F-REGALA-R002")
    void shouldPassInstructionsThatOnlyNameTheTask() {
        AuditNode node = cloze("q", "Completa con la forma correcta.");
        var f = fixture(List.of(node), List.of(result("ok")), ignored -> ok("q"));

        f.analyzer.onQuiz(node);
        f.analyzer.onCourseComplete(f.root);

        assertEquals(1.0, node.getScores().get(NAME));
        assertEquals(List.of(), f.analyzer.findingsAt(node));
        var tally = ArgumentCaptor.forClass(JudgeUnitTally.class);
        verify(f.runtime).coverage(any(), tally.capture());
        assertEquals(1, tally.getValue().getEvaluated());
    }

    @Test
    @DisplayName("should not use a verdict whose fragment is not, as it is, in the field it names, nor one of another exercise")
    @Tag("FEAT-REGALA")
    @Tag("F-REGALA-R002")
    void shouldNotUseAVerdictWhoseFragmentIsNotInTheFieldItNamesNorOneOfAnotherExercise() {
        for (AnswerGiveawayVerdict verdict : List.of(
                givesAway("q", AnswerGiveawayVerdict.Where.INSTRUCTIONS, "usa will porque lo decidís ahora",
                        AnswerGiveawayVerdict.Extent.WHOLE),
                givesAway("q", AnswerGiveawayVerdict.Where.STATEMENT, "acabas de decidirlo",
                        AnswerGiveawayVerdict.Extent.WHOLE),
                ok("other"))) {
            AuditNode node = cloze("q", THIRSTY);
            node.getScores().put(NAME, 1.0);
            var f = fixture(List.of(node), List.of(result("x")), ignored -> verdict);

            f.analyzer.onQuiz(node);
            f.analyzer.onCourseComplete(f.root);

            assertFalse(node.getScores().containsKey(NAME), "an old score is not kept: " + verdict);
            assertTrue(node.getUnevaluatedBy().contains(NAME));
            assertEquals(List.of(), f.analyzer.findingsAt(node));
            assertNotNull(node.getMetadata().get(NAME + "OutputIssue"));
        }
    }

    @Test
    @DisplayName("should leave a REVIEW pending and give no score to a NOT_APPLICABLE, nor approve what was not judged")
    @Tag("FEAT-REGALA")
    @Tag("F-REGALA-R002")
    void shouldLeaveAReviewPendingAndGiveNoScoreToANotApplicableNorApproveWhatWasNotJudged() {
        AuditNode review = cloze("r", THIRSTY);
        AuditNode notApplicable = cloze("n", THIRSTY);
        AuditNode unjudged = cloze("u", THIRSTY);
        var f = fixture(List.of(review, notApplicable, unjudged), List.of(result("r"), result("n")),
                payload -> payload.equals("r")
                        ? new AnswerGiveawayVerdict("r", AnswerGiveawayVerdict.Status.REVIEW, "Elegir will o going to",
                                null, null, null, "Falta el nombre del tema", null, List.of())
                        : new AnswerGiveawayVerdict("n", AnswerGiveawayVerdict.Status.NOT_APPLICABLE, null, null, null,
                                null, "Sólo copia la oración", null, List.of()));

        for (AuditNode node : List.of(review, notApplicable, unjudged)) f.analyzer.onQuiz(node);
        f.analyzer.onCourseComplete(f.root);

        assertFalse(review.getScores().containsKey(NAME));
        assertTrue(review.getUnevaluatedBy().contains(NAME));
        assertFalse(notApplicable.getScores().containsKey(NAME));
        assertFalse(notApplicable.getUnevaluatedBy().contains(NAME));
        assertFalse(unjudged.getScores().containsKey(NAME));
        assertTrue(unjudged.getUnevaluatedBy().contains(NAME));
    }

    @Test
    @DisplayName("should open an ANSWER_GIVEAWAY task the panel corrects touching the instructions and the example, never the mini theory, measured before correcting")
    @Tag("FEAT-REGALA")
    @Tag("F-REGALA-R003")
    void shouldOpenATaskThePanelCorrectsTouchingTheInstructionsAndTheExampleNeverTheMiniTheory() {
        var runtime = mock(JudgeRuntime.class);
        var evaluator = mock(Evaluator.class);
        var policy = new EvaluationRunPolicy();
        var provider = provider(runtime, evaluator);
        var correction = new AnswerGiveawayCorrection();

        provider.create(policy);

        verify(runtime).open(NAME, List.of(evaluator), policy);
        assertEquals("ANSWER_GIVEAWAY", provider.planBinding().orElseThrow().getTaskKind());
        assertEquals(List.of(FindingResolution.PANEL), provider.describe().getResolutions());
        assertEquals(NAME, correction.analyzerName());
        assertTrue(correction.measuredBeforeCorrecting());
        List<ExercisePart> scope = correction.editScope(FindingResolution.PANEL);
        assertTrue(scope.containsAll(List.of(ExercisePart.INSTRUCTIONS, ExercisePart.STATEMENT, ExercisePart.HINT,
                ExercisePart.ANSWERS, ExercisePart.OPTIONS, ExercisePart.TRANSLATION)), scope.toString());
        assertFalse(scope.contains(ExercisePart.MINI_THEORY));
        assertFalse(scope.contains(ExercisePart.TOPIC_INSTRUCTIONS));
    }

    // -------------------------------------------------------------------------

    record Fixture(AnswerGiveawayAnalyzer analyzer, JudgeRuntime runtime, EvaluationSession session, AuditNode root) { }

    static Fixture fixture(List<AuditNode> units, List<EvaluationResolution> resolutions, AnswerGiveawayVerdictReader reader) {
        var runtime = mock(JudgeRuntime.class);
        var session = mock(EvaluationSession.class);
        var policy = new EvaluationRunPolicy();
        var opened = new JudgeSessions(NAME, "v1", List.of("answer-giveaway-judge"), List.of(session), null, policy);
        when(runtime.plan(anyList(), eq(policy))).thenReturn(new JudgeRequestPlan(units,
                Collections.nCopies(units.size(), EvaluationRequestMode.REUSE_OR_EVALUATE), List.of()));
        when(session.resolveAll(anyList(), anyInt())).thenReturn(resolutions);
        var root = new AuditNode();
        root.setTarget(AuditTarget.COURSE);
        return new Fixture(new AnswerGiveawayAnalyzer(opened, runtime, new AnswerGiveawaySubjectBuilder(renderer()), reader),
                runtime, session, root);
    }

    static AnswerGiveawayAnalyzerProvider provider(JudgeRuntime runtime) { return provider(runtime, null); }

    static AnswerGiveawayAnalyzerProvider provider(JudgeRuntime runtime, Evaluator evaluator) {
        return new AnswerGiveawayAnalyzerProvider(runtime, evaluator, ignored -> null, renderer());
    }

    static AnswerGiveawayVerdict ok(String quizId) {
        return new AnswerGiveawayVerdict(quizId, AnswerGiveawayVerdict.Status.OK, "Elegir la forma", null, null, null,
                "Nombra la tarea", null, List.of());
    }

    static AnswerGiveawayVerdict givesAway(String quizId, AnswerGiveawayVerdict.Where where, String fragment,
            AnswerGiveawayVerdict.Extent extent) {
        return new AnswerGiveawayVerdict(quizId, AnswerGiveawayVerdict.Status.GIVES_AWAY,
                "Decidir si es una decisión del momento o un plan", where, fragment, extent,
                "Le dice qué caso de la regla aplica", "poné el contexto en la oración", List.of());
    }

    static EvaluationResolution result(String payload) {
        return new EvaluationResolution(EvaluationResolutionKind.EVALUATED, payload, "v1", null);
    }

    static QuizStudentViewRenderer renderer() { return new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create()); }

    static AuditNode cloze(String id, String instructions) {
        return quiz(id, instructions, List.of(text("I'm thirsty. I"), cloze("'ll get|will get"), text("(get) some water.")), null);
    }

    static AuditNode multipleChoice(String id, String instructions) {
        return quiz(id, instructions, List.of(cloze("Is she"), text("happy?")), new MultipleChoiceEntity("SINGLE",
                List.of(new MultipleChoiceItemEntity("1", 1.0, "Is she"), new MultipleChoiceItemEntity("2", 0.0, "She is"),
                        new MultipleChoiceItemEntity("3", 0.0, "Are she"))));
    }

    static SentencePartEntity text(String text) { return new SentencePartEntity(SentencePartKind.TEXT, text, null); }

    static SentencePartEntity cloze(String answers) {
        return new SentencePartEntity(SentencePartKind.CLOZE, "", List.of(answers.split("\\|")));
    }

    /** A1 → «El futuro» → a FILL topic → the exercise, so the level, the unit and the mode are read from above. */
    static AuditNode quiz(String id, String instructions, List<SentencePartEntity> parts, MultipleChoiceEntity choice) {
        var root = new AuditNode(null, AuditTarget.COURSE, null, new ArrayList<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), null);
        var milestone = child(root, new AuditableMilestone(List.of(), "m", "A1", "A1"), AuditTarget.MILESTONE);
        var topicEntity = new AuditableTopic();
        topicEntity.setLabel("El futuro");
        var topic = child(milestone, topicEntity, AuditTarget.TOPIC);
        var knowledgeEntity = new AuditableKnowledge();
        knowledgeEntity.setLabel("Will para decidir en el momento");
        knowledgeEntity.setSentenceMode(SentenceMode.FILL);
        var knowledge = child(topic, knowledgeEntity, AuditTarget.KNOWLEDGE);
        var quiz = new AuditableQuiz();
        quiz.setId(id);
        quiz.setLabel("Will para decidir en el momento");
        quiz.setInstructions(instructions);
        quiz.setSentenceParts(parts);
        quiz.setMultipleChoice(choice);
        var node = new AuditNode(quiz, AuditTarget.QUIZ, knowledge, new ArrayList<>(), new LinkedHashMap<>(),
                new LinkedHashMap<>(), new DefaultQuizDiagnoses());
        knowledge.getChildren().add(node);
        return node;
    }

    private static AuditNode child(AuditNode parent, AuditableEntity entity, AuditTarget target) {
        var node = new AuditNode(entity, target, parent, new ArrayList<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), null);
        parent.getChildren().add(node);
        return node;
    }
}
