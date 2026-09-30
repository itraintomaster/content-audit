package com.learney.contentaudit.courseinfrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.FormKind;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceItemEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntities;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The fixture holds two real quizzes of the 2026-09-29 production backup, written in the
 * writer's format: a MULTIPLE_CHOICE one (items, selection, formCloze, formAntesDeRevisar and two
 * backup instructions) and a CLOZE one with two backup fields of its own.
 *
 * <p>What stays here supports loading and saving; the round trip and the stray keys on a CLOZE
 * form are traced to FEAT-OPMUL in {@link FileSystemCourseRepositoryTest}.
 */
@Tag("multiple-choice")
class FileSystemCourseRepositoryMultipleChoiceTest {

    private static final String MULTIPLE_CHOICE_ID = "67fab6d59930102295342c7f";
    private static final String CLOZE_ID = "67fab6d59930102295341fa5";

    private final FileSystemCourseRepository repository =
            new FileSystemCourseRepository(new CourseValidatorImpl());

    private static Path fixture() throws URISyntaxException {
        return Path.of(FileSystemCourseRepositoryMultipleChoiceTest.class
                .getResource("/fixtures/multiple-choice-course").toURI());
    }

    private static Path copyFixture(Path target) throws IOException, URISyntaxException {
        Path source = fixture();
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                Path destination = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.copy(path, destination);
                }
            }
        }
        return target;
    }

    private static List<QuizTemplateEntity> quizzes(CourseEntity course) {
        KnowledgeEntity knowledge = course.getRoot().getMilestones().get(0).getTopics().get(0)
                .getKnowledges().get(0);
        return knowledge.getQuizTemplates();
    }

    private static QuizTemplateEntity quiz(CourseEntity course, String id) {
        return quizzes(course).stream().filter(q -> id.equals(q.getId())).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("A multiple-choice form loads with its selection and its options, the correct one included")
    void loadsMultipleChoicePayload() throws Exception {
        QuizTemplateEntity quiz = quiz(repository.load(fixture()), MULTIPLE_CHOICE_ID);

        assertEquals(FormKind.MULTIPLE_CHOICE, QuizTemplateEntities.formKind(quiz));
        assertEquals("SINGLE", quiz.getForm().getMultipleChoice().getSelection());
        List<MultipleChoiceItemEntity> items = quiz.getForm().getMultipleChoice().getItems();
        assertEquals(3, items.size());
        assertEquals(1, items.stream().filter(item -> item.getIncidence() > 0.0).count());
        assertTrue(quiz.getForm().getMultipleChoice().correctItem().isPresent());
        assertNull(quiz.getForm().getUnmodeledFields(),
                "selection and items are modeled, so nothing of the form is left unmodeled");
    }

    @Test
    @DisplayName("Fields the model does not interpret load into unmodeledFields in their original order")
    void loadsUnmodeledFieldsInOrder() throws Exception {
        CourseEntity course = repository.load(fixture());

        assertEquals(List.of("formCloze", "instructionsAnteriores", "instructionsAntesDeRevisar",
                        "formAntesDeRevisar"),
                List.copyOf(quiz(course, MULTIPLE_CHOICE_ID).getUnmodeledFields().keySet()));
        assertEquals(List.of("instructionsAntesDeShortForms", "miniTheoryAntesDeShortForms"),
                List.copyOf(quiz(course, CLOZE_ID).getUnmodeledFields().keySet()));
        assertNull(quiz(course, CLOZE_ID).getForm().getMultipleChoice());
    }

    @Test
    @DisplayName("A multiple-choice form writes sentences, selection and items in the on-disk order")
    @SuppressWarnings("unchecked")
    void writesMultipleChoiceKeysInOrder(@TempDir Path work) throws Exception {
        Path course = copyFixture(work.resolve("course"));
        CourseEntity loaded = repository.load(course);
        quiz(loaded, MULTIPLE_CHOICE_ID).setSentences(List.of("measured sentence"));
        repository.save(loaded, course);

        List<Map<String, Object>> written = new ObjectMapper().readValue(
                course.resolve("a1/topic/knowledge/quizzes.json").toFile(), List.class);
        Map<String, Object> form = (Map<String, Object>) written.stream()
                .filter(q -> MULTIPLE_CHOICE_ID.equals(q.get("id"))).findFirst().orElseThrow()
                .get("form");
        assertEquals(List.of("kind", "incidence", "label", "name", "sentenceParts", "sentences",
                "selection", "items"), List.copyOf(form.keySet()));
    }

    @Test
    @DisplayName("An unmodeled entry never overwrites a known key when the quiz is written")
    void unmodeledNeverOverwritesKnownKeys(@TempDir Path work) throws Exception {
        Path course = copyFixture(work.resolve("course"));
        CourseEntity loaded = repository.load(course);
        QuizTemplateEntity cloze = quiz(loaded, CLOZE_ID);
        Map<String, Object> unmodeled = new LinkedHashMap<>(cloze.getUnmodeledFields());
        unmodeled.put("title", "must not win");
        cloze.setUnmodeledFields(unmodeled);
        repository.save(loaded, course);

        assertEquals(cloze.getTitle(), quiz(repository.load(course), CLOZE_ID).getTitle());
    }
}
