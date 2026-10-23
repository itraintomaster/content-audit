package com.learney.contentaudit.auditcli.commands;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.courseinfrastructure.CourseValidatorImpl;
import com.learney.contentaudit.courseinfrastructure.FileSystemCourseRepository;
import com.learney.contentaudit.refinerdomain.RefinementPlanStore;
import com.learney.contentaudit.revisiondomain.*;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.batchengine.DefaultMultipleChoiceConversionServiceFactory;
import com.learney.contentaudit.revisiondomain.comparison.VerdictKind;
import com.learney.contentaudit.revisiondomain.conversion.MultipleChoiceConversion;
import com.learney.contentaudit.revisiondomain.engine.DefaultCourseElementLocator;
import com.learney.contentaudit.revisiondomain.preservationengine.DefaultCorrectionScope;
import com.learney.contentaudit.revisiondomain.publication.*;
import com.learney.contentaudit.revisiondomain.publicationengine.DefaultProductionFieldMap;
import com.learney.contentaudit.revisiondomain.publicationengine.DefaultPublicationManifestBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

/** Crosses the public conversion service, saved proposal, course adapter and publication projector. */
class MultipleChoiceProductionContractTest {
    private static final String QUIZ = "500000000000000000000001";
    private static final String KNOWLEDGE = "400000000000000000000001";
    private static final List<String> SENTENCES = List.of("Do you know many people?");
    private final FileSystemCourseRepository repository = new FileSystemCourseRepository(new CourseValidatorImpl());
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    @Test
    void existingChoiceConversionRefusesMultipleStoredVariantsWithoutWritingThem(@TempDir Path work) throws Exception {
        var sourceCourse = course();
        var locator = new DefaultCourseElementLocator();
        var original = locator.snapshot(sourceCourse, AuditTarget.QUIZ, QUIZ).orElseThrow().getQuiz();
        var variants = List.of("Do you know many people?", "Do you know a lot of people?");
        original.setSentences(variants);
        original.getForm().getSentenceParts().get(1).setOptions(List.of("many", "a lot of"));
        repository.save(sourceCourse, work);
        Path file = work.resolve("a1/determiners/much-many/quizzes.json");
        byte[] before = Files.readAllBytes(file);
        var batch = new CorrectionBatch();
        batch.setId("batch"); batch.setPlanId("plan"); batch.setStatus(BatchStatus.OPEN);
        batch.setLevel(CefrLevel.A1); batch.setContentVersion(1);
        var batches = mock(BatchStore.class);
        when(batches.load("batch")).thenReturn(Optional.of(batch));
        when(batches.list()).thenReturn(List.of(batch));
        var artifacts = mock(RevisionArtifactStore.class);
        var service = new DefaultMultipleChoiceConversionServiceFactory().create(batches,
                mock(RefinementPlanStore.class), artifacts, repository, work);

        var candidate = service.execute(new MultipleChoiceConversion.Request("read", "batch", QUIZ, null, null))
                .candidates().get(0);
        assertFalse(candidate.eligible());
        assertThrows(IllegalArgumentException.class, () -> service.execute(
                new MultipleChoiceConversion.Request("suggest", "batch", QUIZ, 1, "test")));
        verify(artifacts, never()).save(any());
        assertArrayEquals(before, Files.readAllBytes(file));
        var reread = locator.snapshot(repository.load(work), AuditTarget.QUIZ, QUIZ).orElseThrow().getQuiz();
        assertEquals("CLOZE", reread.getKind());
        assertEquals(variants, reread.getSentences());
    }

    @Test
    void existingChoicesReachProductionWithOrderedOptionsAndEveryStoredSentence(@TempDir Path work) throws Exception {
        Path source = work.resolve("source");
        repository.save(course(), source);
        CourseEntity beforeCourse = repository.load(source);
        var locator = new DefaultCourseElementLocator();
        var before = locator.snapshot(beforeCourse, AuditTarget.QUIZ, QUIZ).orElseThrow();
        var batch = new CorrectionBatch();
        batch.setId("batch"); batch.setPlanId("plan"); batch.setStatus(BatchStatus.OPEN);
        batch.setLevel(CefrLevel.A1); batch.setContentVersion(1);
        var batches = mock(BatchStore.class);
        when(batches.load("batch")).thenReturn(Optional.of(batch));
        when(batches.list()).thenReturn(List.of(batch));
        var plans = mock(RefinementPlanStore.class);
        when(plans.load("plan")).thenReturn(Optional.empty());
        var artifacts = mock(RevisionArtifactStore.class);
        var service = new DefaultMultipleChoiceConversionServiceFactory()
                .create(batches, plans, artifacts, repository, source);

        var candidates = service.execute(new MultipleChoiceConversion.Request("read", "batch", QUIZ, null, null));
        assertTrue(candidates.candidates().get(0).eligible(), candidates.candidates().get(0).reason());
        service.execute(new MultipleChoiceConversion.Request("suggest", "batch", QUIZ, 1, "test"));
        var captured = ArgumentCaptor.forClass(RevisionArtifact.class);
        verify(artifacts).save(captured.capture());
        // The persisted proposal uses the entity representation; reading it must keep its complete payload.
        var proposal = json.readValue(json.writeValueAsBytes(captured.getValue()), RevisionArtifact.class).getProposal();
        var after = proposal.getElementAfter();
        var choice = after.getQuiz().getForm().getMultipleChoice();
        assertEquals(List.of("many", "much"), choice.getItems().stream().map(MultipleChoiceItemEntity::getId).toList());
        assertEquals(List.of("many", "much"), choice.getItems().stream().map(MultipleChoiceItemEntity::getLabel).toList());
        assertEquals(List.of(1.0, 0.0), choice.getItems().stream().map(MultipleChoiceItemEntity::getIncidence).toList());
        assertEquals("many", choice.correctItem().orElseThrow().getLabel());
        assertEquals("SINGLE", choice.getSelection());
        assertEquals(SENTENCES, after.getQuiz().getSentences());

        Path output = work.resolve("converted");
        repository.save(locator.replace(beforeCourse, after), output);
        var reloaded = locator.snapshot(repository.load(output), AuditTarget.QUIZ, QUIZ).orElseThrow();
        assertEquals(choice, reloaded.getQuiz().getForm().getMultipleChoice());
        assertEquals(SENTENCES, reloaded.getQuiz().getSentences());
        var storedForm = json.readTree(output.resolve("a1/determiners/much-many/quizzes.json").toFile()).get(0).path("form");
        assertFalse(storedForm.has("multipleChoice"));
        assertEquals("SINGLE", storedForm.path("selection").asText());
        assertEquals(List.of("many", "much"), json.convertValue(storedForm.path("items").findValues("id"), List.class));
        assertEquals(SENTENCES, json.convertValue(storedForm.path("sentences"), List.class));

        var production = repository.project(reloaded.getQuiz(), List.of("form"));
        var form = (Map<?, ?>) production.get("form");
        assertFalse(form.containsKey("multipleChoice"));
        assertEquals("SINGLE", form.get("selection"));
        assertEquals(List.of(Map.of("id", "many", "label", "many", "incidence", 1.0),
                Map.of("id", "much", "label", "much", "incidence", 0.0)), form.get("items"));
        assertEquals(SENTENCES, form.get("sentences"));

        var manifest = new DefaultPublicationManifestBuilder(new DefaultCorrectionScope(),
                new DefaultProductionFieldMap(), repository).build(new PublicationManifestRequest(
                "batch", "plan", "A1", "2026-10-23T00:00:00Z", "test",
                new ManifestSource("baseline", "start", null),
                new ManifestVerdict(VerdictKind.PASSES, true, List.of(), List.of(), null),
                new ReviewTally(1, 0, 0, List.of(), List.of()), List.of(new PublishedElement(before, reloaded,
                proposal.getProposalId(), DocumentReviewStatus.APPROVED,
                Map.of("conversion", Set.of(ExercisePart.FORM_KIND, ExercisePart.OPTIONS, ExercisePart.STATEMENT,
                        ExercisePart.ANSWERS, ExercisePart.INSTRUCTIONS, ExercisePart.STORED_SENTENCE)))), List.of()));
        assertTrue(manifest.unsupported().isEmpty());
        assertEquals(form, manifest.documents().get(0).fields().stream().filter(f -> f.path().equals("form"))
                .findFirst().orElseThrow().after());
    }

    private static CourseEntity course() {
        var quiz = new QuizTemplateEntity(QUIZ, QUIZ, "CLOZE", KNOWLEDGE, "Much or many",
                "Escribe la palabra correcta.", "¿Conoces a mucha gente?", "many", "Determiners",
                new FormEntity("CLOZE", 1, "", "", List.of(
                        new SentencePartEntity(SentencePartKind.TEXT, "Do you know", null),
                        new SentencePartEntity(SentencePartKind.CLOZE, "", List.of("many")),
                        new SentencePartEntity(SentencePartKind.TEXT, "people? (many / much)", null)), null, null),
                0, 0, 0, "", "", "", "audio", "", "Countable nouns.", "", SENTENCES, null);
        String rootId = "100000000000000000000001", milestoneId = "200000000000000000000001";
        String topicId = "300000000000000000000001";
        var knowledge = new KnowledgeEntity(KNOWLEDGE, "many", NodeKind.KNOWLEDGE, "Much or many", "old-k",
                topicId, true, "Elige.", 1, "much-many", List.of(quiz), SentenceMode.FILL);
        var topic = new TopicEntity(topicId, "determiners", NodeKind.TOPIC, "Determiners", "old-t", milestoneId,
                List.of(KNOWLEDGE), List.of(KNOWLEDGE), 1, "determiners", List.of(knowledge));
        var milestone = new MilestoneEntity(milestoneId, "A1", NodeKind.MILESTONE, "A1", "old-m", rootId,
                List.of(topicId), 1, "a1", List.of(topic));
        return new CourseEntity("000000000000000000000001", "Course", List.of(KNOWLEDGE),
                new RootNodeEntity(rootId, "course", NodeKind.ROOT, "Course", List.of(milestoneId), List.of(milestone)), "course");
    }
}
