package com.learney.contentaudit.courseinfrastructure;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.ProductionDocumentProjector;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * F-PUBPROD-R002 (inv. 3): el antes y el después de la lista salen del curso tal como lo guarda producción, en JSON
 * simple. El curso de prueba tiene dos ejercicios reales del respaldo de producción del 29/9: uno de opción múltiple y
 * uno de huecos.
 */
class FileSystemCourseRepositoryProjectionTest {

    private static final String MULTIPLE_CHOICE_ID = "67fab6d59930102295342c7f";
    private static final String CLOZE_ID = "67fab6d59930102295341fa5";

    private final FileSystemCourseRepository repository = new FileSystemCourseRepository(new CourseValidatorImpl());

    private final ProductionDocumentProjector projector = repository;

    private static Path fixture() throws URISyntaxException {
        return Path.of(FileSystemCourseRepositoryProjectionTest.class.getResource("/fixtures/multiple-choice-course")
                .toURI());
    }

    private static KnowledgeEntity knowledge(CourseEntity course) {
        return course.getRoot().getMilestones().get(0).getTopics().get(0).getKnowledges().get(0);
    }

    private static QuizTemplateEntity quiz(CourseEntity course, String id) {
        return knowledge(course).getQuizTemplates().stream().filter(q -> id.equals(q.getId())).findFirst()
                .orElseThrow();
    }

    @Test
    @DisplayName("should project the multiple choice whole and with numbers as numbers, a part with no text as an empty text and an absent stored sentence as an empty list")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R002")
    void shouldProjectTheMultipleChoiceWholeWithNumbersAsNumbers() throws Exception {
        QuizTemplateEntity quiz = quiz(repository.load(fixture()), MULTIPLE_CHOICE_ID);

        Map<String, Object> values = projector.project(quiz, List.of("form.selection", "form.items",
                "form.sentenceParts", "form.sentences"));

        assertEquals(List.of("form.selection", "form.items", "form.sentenceParts", "form.sentences"),
                List.copyOf(values.keySet()));
        assertEquals("SINGLE", values.get("form.selection"));
        List<?> items = (List<?>) values.get("form.items");
        assertEquals(3, items.size());
        Map<String, Object> correct = new LinkedHashMap<>();
        correct.put("id", "at");
        correct.put("incidence", 1.0);
        correct.put("label", "at");
        assertEquals(correct, items.get(1));
        assertTrue(((Map<?, ?>) items.get(0)).get("incidence") instanceof Double);
        List<?> parts = (List<?>) values.get("form.sentenceParts");
        Map<String, Object> gap = new LinkedHashMap<>();
        gap.put("kind", "CLOZE");
        gap.put("text", "");
        gap.put("options", null);
        assertEquals(gap, parts.get(1));
        assertEquals(List.of(), values.get("form.sentences"));
    }

    @Test
    @DisplayName("should project the texts of an exercise, give a missing path as null and the exercise identifier in hexadecimal")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R002")
    void shouldProjectTheTextsOfAnExercise() throws Exception {
        QuizTemplateEntity quiz = quiz(repository.load(fixture()), CLOZE_ID);

        Map<String, Object> values = projector.project(quiz, List.of("translation", "form.sentences", "form.nada"));

        assertEquals("Hace calor adentro.", values.get("translation"));
        assertEquals(List.of("It's hot inside."), values.get("form.sentences"));
        assertTrue(values.containsKey("form.nada"));
        assertNull(values.get("form.nada"));
        assertEquals(CLOZE_ID, projector.documentId(quiz));
    }

    @Test
    @DisplayName("should give the text of a part as an empty text when the course has none")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R002")
    void shouldGiveTheTextOfAPartAsAnEmptyTextWhenTheCourseHasNone() throws Exception {
        QuizTemplateEntity quiz = quiz(repository.load(fixture()), CLOZE_ID);
        quiz.getForm().getSentenceParts().get(1).setText(null);

        List<?> parts = (List<?>) projector.project(quiz, List.of("form.sentenceParts")).get("form.sentenceParts");

        assertEquals("", ((Map<?, ?>) parts.get(1)).get("text"));
        assertEquals(List.of("'s"), ((Map<?, ?>) parts.get(1)).get("options"));
        assertNull(quiz.getForm().getSentenceParts().get(1).getText(), "proyectar no cambia el curso");
    }

    @Test
    @DisplayName("should project the instructions of a topic and its identifier")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R002")
    void shouldProjectTheInstructionsOfATopic() throws Exception {
        KnowledgeEntity topic = knowledge(repository.load(fixture()));

        Map<String, Object> values = projector.project(topic, List.of("instructions"));

        assertEquals(Map.of("instructions", "Elige la opcion correcta."), values);
        assertEquals("400000000000000000000001", projector.documentId(topic));
    }

    @Test
    @DisplayName("should keep writing the course byte for byte after projecting it")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R002")
    void shouldKeepWritingTheCourseByteForByteAfterProjectingIt(@TempDir Path target) throws Exception {
        CourseEntity course = repository.load(fixture());
        for (QuizTemplateEntity quiz : knowledge(course).getQuizTemplates()) {
            projector.project(quiz, List.of("form.sentenceParts", "form.items", "form.sentences"));
        }
        projector.project(knowledge(course), List.of("instructions"));

        repository.save(course, target);

        Path relative = Path.of("a1", "topic", "knowledge");
        for (String file : List.of("quizzes.json", "_knowledge.json")) {
            assertArrayEquals(Files.readAllBytes(fixture().resolve(relative).resolve(file)),
                    Files.readAllBytes(target.resolve(relative).resolve(file)), file);
        }
    }
}
