package com.learney.contentaudit.revisiondomain.changereviewengine;

import static org.junit.jupiter.api.Assertions.*;

import com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.evaluationledgerdomain.*;
import com.learney.contentaudit.evaluationledgerdomain.evaluationsession.DefaultEvaluationSessionFactory;
import com.learney.contentaudit.revisiondomain.batch.BatchChangeStep;
import com.learney.contentaudit.revisiondomain.batch.BatchReview;
import com.learney.contentaudit.revisiondomain.batch.BatchReviewDecision;
import com.learney.contentaudit.revisiondomain.batch.BatchReviewService;
import com.learney.contentaudit.revisiondomain.batch.BatchReviewSlice;
import com.learney.contentaudit.revisiondomain.changereview.ChangeReview;
import com.learney.contentaudit.revisiondomain.changereview.ChangeReview.Mark;
import com.learney.contentaudit.revisiondomain.changereview.ChangeReview.State;
import com.learney.contentaudit.revisiondomain.changereview.ChangeReview.Where;
import com.learney.contentaudit.revisiondomain.changereview.ChangeReviewVerdictReader;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** FEAT-MARCA: the reviewer over an invented batch, with a real session over a ledger in memory and a fake reviewer. */
class DefaultChangeReviewServiceTest {
    static final String BATCH = "tanda-A1-prueba";

    @Test
    @DisplayName("should read each changed exercise whole, production beside now, with the level, the unit, the topic, its instructions and its sentence mode, once per exercise")
    @Tag("FEAT-MARCA")
    @Tag("F-MARCA-R001")
    void shouldReadEachChangedExerciseWholeWithProductionBesideAndItsTopicOncePerExercise() {
        Bench bench = new Bench();
        bench.now.quiz("hungry").setInstructions("Escribe la respuesta corta.");
        bench.now.quiz("hungry").getForm().setSentenceParts(cloze("Are you hungry? I ", "'m not"));
        bench.now.quiz("hungry").setTranslation("¿Tenés hambre? No.");
        bench.now.quiz("hungry").setMiniTheory("La respuesta corta repite el auxiliar.");
        bench.now.quiz("pick").getForm().getMultipleChoice().getItems().get(1).setLabel("Do she like tea?");
        bench.items = List.of(item("hungry", "c1"), item("hungry", "c2"), item("pick", "c3"));

        ChangeReview.Result result = bench.service().review(new ChangeReview.Request(BATCH, List.of(), 5, 1));

        assertEquals(List.of("hungry", "pick"), bench.reviewer.asked, "one reading per exercise, in the order of the course");
        Map<String, String> read = bench.reviewer.read.get("hungry");
        assertEquals("A1", read.get("level"));
        assertEquals("Unidad 1", read.get("unit"));
        assertEquals("Respuestas cortas", read.get("topic"));
        assertEquals("Completa con la respuesta corta.", read.get("topicInstructions"));
        assertEquals("FILL", read.get("sentenceMode"));
        assertEquals(String.join("\n",
                "Consigna del ejercicio: (sin consigna propia)",
                "Ejercicio, como lo ve el alumno antes de contestar: Are you hungry? No, I ___ .",
                "Opciones que ve el alumno: (no es de opción múltiple)",
                "Respuestas que acepta el corrector: Hueco 1: 'm not",
                "Traducción, que el alumno ve al corregir: ¿Tenés hambre? No, no tengo.",
                "Miniteoría, que el alumno ve al corregir: (sin miniteoría)"), read.get("before"));
        assertEquals(String.join("\n",
                "Consigna del ejercicio: Escribe la respuesta corta.",
                "Ejercicio, como lo ve el alumno antes de contestar: Are you hungry? I ___ .",
                "Opciones que ve el alumno: (no es de opción múltiple)",
                "Respuestas que acepta el corrector: Hueco 1: 'm not",
                "Traducción, que el alumno ve al corregir: ¿Tenés hambre? No.",
                "Miniteoría, que el alumno ve al corregir: La respuesta corta repite el auxiliar."), read.get("after"));
        assertTrue(bench.reviewer.read.get("pick").get("before").contains(
                "Opciones que ve el alumno: Does she like tea? / Is she like tea?"), "the options of a multiple choice");
        assertTrue(bench.reviewer.read.get("pick").get("after").contains(
                "Opciones que ve el alumno: Does she like tea? / Do she like tea?\n"
                        + "Respuestas que acepta el corrector: Opción correcta: Does she like tea?"));
        assertEquals(2, result.changed());
    }

    @Test
    @DisplayName("should not read an exercise that stayed as in production, and take production from the course the correction started from")
    @Tag("FEAT-MARCA")
    @Tag("F-MARCA-R001")
    void shouldNotReadAnExerciseEqualToProductionAndTakeProductionFromTheStartingCourse() {
        Bench bench = new Bench();
        bench.now.quiz("hungry").setTranslation("¿Tenés hambre? No, para nada.");
        // Production is the base of the batch; an exercise new to the course is read against nothing.
        bench.production.knowledge().getQuizTemplates().removeIf(q -> q.getId().equals("pick"));

        ChangeReview.Result result = bench.service().review(new ChangeReview.Request(BATCH, List.of(), 5, 1));

        assertEquals(List.of("hungry", "pick"), bench.reviewer.asked, "«tired» stayed as in production and is not read");
        assertTrue(bench.reviewer.read.get("hungry").get("before").contains("¿Tenés hambre? No, no tengo."));
        assertEquals(ChangeReviewSubjects.NOT_IN_PRODUCTION, bench.reviewer.read.get("pick").get("before"));
        assertEquals(2, result.changed());
    }

    @Test
    @DisplayName("should mark good, doubtful or worse, each problem where it is with its fragment as it is, and change no decision of the batch")
    @Tag("FEAT-MARCA")
    @Tag("F-MARCA-R002")
    void shouldMarkGoodDoubtfulOrWorseWithEachProblemWhereItIsAndChangeNoDecision() {
        Bench bench = new Bench().changeAll();
        bench.reviewer.answers.put("hungry", subject -> mark("hungry", "WORSE",
                problem("EXERCISE", "No, I", "Se perdió el «No,» de la respuesta corta.")));
        bench.reviewer.answers.put("tired", subject -> mark("tired", "DOUBTFUL",
                problem("TRANSLATION", "No, en absoluto.", "La traducción dice más que el inglés.")));
        bench.reviewer.answers.put("pick", subject -> mark("pick", "GOOD"));

        ChangeReview.Result result = bench.service().review(new ChangeReview.Request(BATCH, List.of(), 5, 1));

        ChangeReview.Row worse = row(result, "hungry");
        assertEquals(State.MARKED, worse.state());
        assertEquals(Mark.WORSE, worse.verdict().mark());
        assertEquals(List.of(new ChangeReview.Problem(Where.EXERCISE, "No, I", "Se perdió el «No,» de la respuesta corta.")),
                worse.verdict().problems());
        assertEquals(Mark.DOUBTFUL, row(result, "tired").verdict().mark());
        assertEquals(Mark.GOOD, row(result, "pick").verdict().mark());
        assertTrue(row(result, "pick").verdict().problems().isEmpty());
        assertEquals(List.of("read"), bench.reviews.calls, "it only reads the batch: no decision, closing or publication");
    }

    @Test
    @DisplayName("should not consult without a budget, and say how many exercises changed, how many have today's mark and how many queries are missing")
    @Tag("FEAT-MARCA")
    @Tag("F-MARCA-R003")
    void shouldNotConsultWithoutABudgetAndSayWhatIsMissing() {
        Bench bench = new Bench().changeAll();
        bench.service().review(new ChangeReview.Request(BATCH, List.of("hungry"), 1, 1));
        bench.reviewer.asked.clear();

        ChangeReview.Result result = bench.service().review(new ChangeReview.Request(BATCH, List.of(), null, 1));

        assertTrue(bench.reviewer.asked.isEmpty(), "without a budget nobody is consulted");
        assertEquals(3, result.changed());
        assertEquals(1, result.upToDate());
        assertEquals(2, result.pending());
        assertEquals(0, result.spent());
        assertEquals(State.MARKED, row(result, "hungry").state(), "the marks there are, are shown");
        assertEquals(State.UNMARKED, row(result, "tired").state());
    }

    @Test
    @DisplayName("should consult at most the budget, save each mark with the reviewer's version and engine, and not pay again for what today's reviewer marked")
    @Tag("FEAT-MARCA")
    @Tag("F-MARCA-R003")
    void shouldConsultAtMostTheBudgetAndNotPayAgainForWhatTodaysReviewerMarked() {
        Bench bench = new Bench().changeAll();

        ChangeReview.Result first = bench.service().review(new ChangeReview.Request(BATCH, List.of(), 2, 1));

        assertEquals(2, bench.reviewer.asked.size());
        assertEquals(2, first.spent());
        assertEquals(1, first.pending());
        assertEquals("v1", first.reviewerVersion());
        assertEquals("codex-cli / gpt-test / medium", first.runtime());
        assertTrue(bench.ledger.records.stream().allMatch(r -> "v1".equals(r.getEvaluatorVersion())
                && r.getKey().getRuntimeIdentity().equals(Reviewer.RUNTIME)), "saved with its version and its engine");

        ChangeReview.Result second = bench.service().review(new ChangeReview.Request(BATCH, List.of(), 5, 1));

        assertEquals(3, bench.reviewer.asked.size(), "only the one that was missing is asked");
        assertEquals(1, second.spent());
        assertEquals(3, second.upToDate());
        assertEquals(0, second.pending());
    }

    @Test
    @DisplayName("should show a mark of an earlier version as such, and ask it again only with a budget")
    @Tag("FEAT-MARCA")
    @Tag("F-MARCA-R003")
    void shouldShowAMarkOfAnEarlierVersionAndAskItAgainOnlyWithABudget() {
        Bench bench = new Bench().changeAll();
        bench.service().review(new ChangeReview.Request(BATCH, List.of("hungry"), 1, 1));
        bench.reviewer.version = "v2";
        bench.reviewer.answers.put("hungry", subject -> mark("hungry", "DOUBTFUL",
                problem("TRANSLATION", "¿Tenés hambre? No, no tengo.", "Dice más que el inglés.")));

        ChangeReview.Result shown = bench.service().review(new ChangeReview.Request(BATCH, List.of("hungry"), null, 1));

        assertEquals(State.OLDER_VERSION, row(shown, "hungry").state());
        assertEquals(Mark.GOOD, row(shown, "hungry").verdict().mark(), "the old mark is shown");
        assertEquals("v1", row(shown, "hungry").reviewerVersion());
        assertEquals(1, shown.pending());
        assertEquals(1, bench.reviewer.asked.size());

        ChangeReview.Result asked = bench.service().review(new ChangeReview.Request(BATCH, List.of("hungry"), 1, 1));

        assertEquals(2, bench.reviewer.asked.size());
        assertEquals(State.MARKED, row(asked, "hungry").state());
        assertEquals(Mark.DOUBTFUL, row(asked, "hungry").verdict().mark());
        assertEquals("v2", row(asked, "hungry").reviewerVersion());
    }

    @Test
    @DisplayName("should leave unmarked, with the reason, a mark of another exercise, one whose fragment is in neither side and one that cannot be read")
    @Tag("FEAT-MARCA")
    @Tag("F-MARCA-R004")
    void shouldLeaveUnmarkedAMarkOfAnotherExerciseAForeignFragmentAndAnUnreadableOne() {
        Bench bench = new Bench().changeAll();
        bench.reviewer.answers.put("hungry", subject -> mark("tired", "GOOD"));
        bench.reviewer.answers.put("tired", subject -> mark("tired", "DOUBTFUL",
                problem("EXERCISE", "She is sleepy.", "No está en el ejercicio.")));
        bench.reviewer.answers.put("pick", subject -> "{\"quizId\": \"pick\", \"mark\": \"GOOD\"");

        ChangeReview.Result result = bench.service().review(new ChangeReview.Request(BATCH, List.of(), 5, 1));

        for (String quiz : List.of("hungry", "tired", "pick")) {
            assertEquals(State.UNUSABLE, row(result, quiz).state(), quiz);
            assertNull(row(result, quiz).verdict(), "without a mark it is never good: " + quiz);
        }
        assertTrue(row(result, "hungry").detail().contains("otro ejercicio"));
        assertTrue(row(result, "tired").detail().contains("«She is sleepy.» no está en ninguno de los dos lados"));
        assertTrue(row(result, "pick").detail().contains("ilegible"));
        assertEquals(0, result.upToDate());
        assertEquals(3, result.pending(), "they are asked again with a budget");
    }

    @Test
    @DisplayName("should leave unmarked an exercise that changed again after its mark, because what the reviewer read is no longer there")
    @Tag("FEAT-MARCA")
    @Tag("F-MARCA-R004")
    void shouldLeaveUnmarkedAnExerciseThatChangedAgainAfterItsMark() {
        Bench bench = new Bench().changeAll();
        bench.service().review(new ChangeReview.Request(BATCH, List.of(), 5, 1));
        bench.now.quiz("hungry").setTranslation("¿Tenés hambre? Para nada.");

        ChangeReview.Result result = bench.service().review(new ChangeReview.Request(BATCH, List.of(), null, 1));

        assertEquals(State.UNMARKED, row(result, "hungry").state());
        assertNull(row(result, "hungry").verdict());
        assertEquals(State.MARKED, row(result, "tired").state());
        assertEquals(2, result.upToDate());
    }

    @Test
    @DisplayName("should list worse first, then doubtful, then the unmarked and good last, each group in the order of the course")
    @Tag("FEAT-MARCA")
    @Tag("F-MARCA-R005")
    void shouldListWorseThenDoubtfulThenUnmarkedThenGoodInTheOrderOfTheCourse() {
        Bench bench = new Bench().changeAll();
        bench.now.knowledge().getQuizTemplates().add(exercise("late", "Are you late? No, I ___ .", "'m not",
                "¿Llegás tarde? No."));
        bench.reviewer.answers.put("hungry", subject -> mark("hungry", "GOOD"));
        bench.reviewer.answers.put("tired", subject -> mark("tired", "DOUBTFUL",
                problem("TRANSLATION", "No, en absoluto.", "Dice más que el inglés.")));
        bench.reviewer.answers.put("pick", subject -> mark("pick", "WORSE",
                problem("OPTIONS", "Do she like tea?", "Hay dos opciones incorrectas iguales.")));

        ChangeReview.Result result = bench.service().review(new ChangeReview.Request(BATCH, List.of(), 3, 1));

        assertEquals(List.of("pick", "tired", "late", "hungry"), result.rows().stream().map(ChangeReview.Row::quizId).toList());
    }

    @Test
    @DisplayName("should read only the exercises asked for, when some are asked for")
    @Tag("FEAT-MARCA")
    @Tag("F-MARCA-R005")
    void shouldReadOnlyTheExercisesAskedFor() {
        Bench bench = new Bench().changeAll();

        ChangeReview.Result result = bench.service().review(new ChangeReview.Request(BATCH, List.of("pick", "tired"), 5, 1));

        assertEquals(List.of("tired", "pick"), bench.reviewer.asked);
        assertEquals(2, result.changed());
    }

    // ---------------------------------------------------------------- the bench

    static ChangeReview.Row row(ChangeReview.Result result, String quizId) {
        return result.rows().stream().filter(r -> r.quizId().equals(quizId)).findFirst().orElseThrow();
    }

    static String mark(String quizId, String mark, String... problems) {
        return "{\"quizId\":\"" + quizId + "\",\"mark\":\"" + mark + "\",\"problems\":[" + String.join(",", problems)
                + "],\"reason\":\"Comparado con producción.\"}";
    }

    static String problem(String where, String fragment, String what) {
        return "{\"where\":\"" + where + "\",\"fragment\":\"" + fragment + "\",\"what\":\"" + what + "\"}";
    }

    static BatchReview.Item item(String quizId, String changeId) {
        var step = new BatchChangeStep();
        step.setQuizId(quizId);
        return new BatchReview.Item(changeId, step, null, null, "panel", null, BatchReviewDecision.Status.PENDING, null,
                null, List.of(), null);
    }

    static List<SentencePartEntity> cloze(String text, String answer) {
        return new ArrayList<>(List.of(new SentencePartEntity(SentencePartKind.TEXT, text, null),
                new SentencePartEntity(SentencePartKind.CLOZE, null, new ArrayList<>(List.of(answer))),
                new SentencePartEntity(SentencePartKind.TEXT, ".", null)));
    }

    static QuizTemplateEntity exercise(String id, String statement, String answer, String translation) {
        var quiz = new QuizTemplateEntity();
        quiz.setId(id);
        quiz.setTitle("Respuestas cortas");
        quiz.setTranslation(translation);
        var form = new FormEntity();
        form.setKind("CLOZE");
        form.setSentenceParts(cloze(statement.substring(0, statement.indexOf("___")), answer));
        quiz.setForm(form);
        return quiz;
    }

    static QuizTemplateEntity choice(String id) {
        var quiz = new QuizTemplateEntity();
        quiz.setId(id);
        quiz.setTitle("Respuestas cortas");
        quiz.setInstructions("Elige la pregunta correcta.");
        quiz.setTranslation("¿A ella le gusta el té?");
        var form = new FormEntity();
        form.setKind("MULTIPLE_CHOICE");
        form.setMultipleChoice(new MultipleChoiceEntity("SINGLE", new ArrayList<>(List.of(
                new MultipleChoiceItemEntity("a", 1.0, "Does she like tea?"),
                new MultipleChoiceItemEntity("b", 0.0, "Is she like tea?")))));
        quiz.setForm(form);
        return quiz;
    }

    /** A course of one milestone, one unit and one topic with three exercises. */
    static final class Course {
        final CourseEntity entity = new CourseEntity();

        Course() {
            var knowledge = new KnowledgeEntity();
            knowledge.setId("k");
            knowledge.setLabel("Respuestas cortas");
            knowledge.setInstructions("Completa con la respuesta corta.");
            knowledge.setSentenceMode(SentenceMode.FILL);
            knowledge.setQuizTemplates(new ArrayList<>(List.of(
                    exercise("hungry", "Are you hungry? No, I ___ .", "'m not", "¿Tenés hambre? No, no tengo."),
                    exercise("tired", "Is she tired? No, she ___ .", "isn't", "¿Está cansada? No."),
                    choice("pick"))));
            var topic = new TopicEntity();
            topic.setId("t");
            topic.setLabel("Unidad 1");
            topic.setKnowledges(new ArrayList<>(List.of(knowledge)));
            var milestone = new MilestoneEntity();
            milestone.setId("m");
            milestone.setLabel("A1 ");
            milestone.setTopics(new ArrayList<>(List.of(topic)));
            var root = new RootNodeEntity();
            root.setMilestones(new ArrayList<>(List.of(milestone)));
            entity.setId("course");
            entity.setRoot(root);
        }

        KnowledgeEntity knowledge() {
            return entity.getRoot().getMilestones().get(0).getTopics().get(0).getKnowledges().get(0);
        }

        QuizTemplateEntity quiz(String id) {
            return knowledge().getQuizTemplates().stream().filter(q -> q.getId().equals(id)).findFirst().orElseThrow();
        }
    }

    static final class Bench {
        final Course production = new Course();
        final Course now = new Course();
        final Reviewer reviewer = new Reviewer();
        final Ledger ledger = new Ledger();
        final Reviews reviews = new Reviews(this);
        List<BatchReview.Item> items = List.of();

        /** Each exercise changed a little; the answers below keep their fragments in their sides. */
        Bench changeAll() {
            now.quiz("hungry").setTranslation("¿Tenés hambre? No, para nada.");
            now.quiz("tired").setTranslation("¿Está cansada? No, en absoluto.");
            now.quiz("pick").getForm().getMultipleChoice().getItems().get(1).setLabel("Do she like tea?");
            return this;
        }

        DefaultChangeReviewService service() {
            return (DefaultChangeReviewService) new DefaultChangeReviewServiceFactory().create(reviews,
                    new DefaultEvaluationSessionFactory(ledger, content -> content.toString()), reviewer, new Reader(),
                    new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create()));
        }
    }

    /** Only {@link #read} is allowed: the reviewer never decides anything of the batch. */
    static final class Reviews implements BatchReviewService {
        final Bench bench;
        final List<String> calls = new ArrayList<>();

        Reviews(Bench bench) { this.bench = bench; }

        @Override public BatchReview read(String batchId) {
            calls.add("read");
            assertEquals(BATCH, batchId);
            return new BatchReview(BATCH, 2, 7, null, List.of(), List.of(), bench.items, List.of(), List.of(), List.of(),
                    List.of(), List.of(), false, null, bench.now.entity, bench.production.entity);
        }
        @Override public BatchReview decide(BatchReviewDecision decision) { calls.add("decide"); throw new AssertionError(); }
        @Override public BatchReviewSlice save(BatchReviewDecision decision) { calls.add("save"); throw new AssertionError(); }
        @Override public BatchReviewSlice slice(String batchId, Collection<String> quizIds) { calls.add("slice"); throw new AssertionError(); }
    }

    /** A reviewer that answers «good» unless told otherwise, and remembers what it read. */
    static final class Reviewer implements Evaluator {
        static final EvaluationRuntimeIdentity RUNTIME = EvaluationRuntimeIdentity.explicit("codex-cli", "gpt-test", "medium");
        final List<String> asked = Collections.synchronizedList(new ArrayList<>());
        final Map<String, Map<String, String>> read = new LinkedHashMap<>();
        final Map<String, Function<EvaluationSubject, String>> answers = new HashMap<>();
        String version = "v1";

        @Override public String evaluatorId() { return "change-reviewer"; }
        @Override public Optional<String> evaluatorVersion() { return Optional.of(version); }
        @Override public EvaluationRuntimeIdentity runtimeIdentity() { return RUNTIME; }
        @Override public synchronized EvaluationOutcome evaluate(EvaluationSubject subject) {
            asked.add(subject.getSubjectRef());
            read.put(subject.getSubjectRef(), subject.getContent());
            return new EvaluationEmitted(answers.getOrDefault(subject.getSubjectRef(), s -> mark(s.getSubjectRef(), "GOOD"))
                    .apply(subject));
        }
    }

    static final class Ledger implements EvaluationLedger {
        final List<EvaluationRecord> records = Collections.synchronizedList(new ArrayList<>());

        @Override public void append(EvaluationRecord record) { records.add(record); }
        @Override public Optional<EvaluationRecord> findLatest(EvaluationKey key) {
            List<EvaluationRecord> history = history(key);
            return history.isEmpty() ? Optional.empty() : Optional.of(history.get(history.size() - 1));
        }
        @Override public List<EvaluationRecord> history(EvaluationKey key) {
            synchronized (records) { return records.stream().filter(r -> r.getKey().equals(key)).toList(); }
        }
    }

    /** The shape the reviewer answers, read without Jackson: what matters here is what the service does with it. */
    static final class Reader implements ChangeReviewVerdictReader {
        @Override public ChangeReview.Verdict read(String payload) {
            try {
                var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(payload);
                var problems = new ArrayList<ChangeReview.Problem>();
                for (var p : json.get("problems"))
                    problems.add(new ChangeReview.Problem(Where.valueOf(p.get("where").asText()), p.get("fragment").asText(),
                            p.get("what").asText()));
                return new ChangeReview.Verdict(json.get("quizId").asText(), Mark.valueOf(json.get("mark").asText()),
                        problems, json.get("reason").asText());
            } catch (Exception e) {
                throw new IllegalArgumentException("Marca del revisor ilegible: " + e.getMessage(), e);
            }
        }
    }
}
