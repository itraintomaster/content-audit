package com.learney.contentaudit.revisioninfrastructure.panelagent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditEngine;
import com.learney.contentaudit.auditdomain.CourseMapper;
import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.courseinfrastructure.CourseValidatorImpl;
import com.learney.contentaudit.courseinfrastructure.FileSystemCourseRepository;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.engine.DefaultCourseElementLocator;
import com.learney.contentaudit.revisiondomain.panel.PanelCandidateKind;
import com.learney.contentaudit.revisiondomain.panel.*;
import com.learney.contentaudit.revisiondomain.panelengine.DefaultPanelRunnerFactory;
import com.sentinel.agents.framework.state.ArtifactPointer;
import com.sentinel.agents.framework.state.RunState;
import com.sentinel.agents.framework.state.RunStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.io.TempDir;

/** Uses a recorded agent answer: the parser and persistence are real, no agent or model runs. */
class PanelMultipleChoiceProductionContractTest {
    private static final String QUIZ = "500000000000000000000001";
    private static final String KNOWLEDGE = "400000000000000000000001";

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void parsedConversionKeepsCanonicalDataOrRejectsWithoutTruncatingVariants(boolean multipleVariants, @TempDir Path work)
            throws Exception {
        Path answer = work.resolve("panel.json");
        Files.writeString(answer, """
                {"candidates":[{"kind":"MULTIPLE_CHOICE_CONVERSION", "instructions":"Elige la opción correcta.",
                  "sentenceParts":[{"kind":"TEXT","text":"You"},{"kind":"CLOZE"},
                                   {"kind":"TEXT","text":"work at the bank."}],
                  "options":[{"label":"aren't","correct":false},{"label":"don't","correct":true},
                             {"label":"doesn't","correct":false}]}]}
                """);
        var run = RunState.empty().withRunStatus(RunStatus.COMPLETED).putArtifact("panelProposal",
                new ArtifactPointer("panelProposal", "panelProposal", answer, Instant.now(), "fixture", "hash"));
        var request = new PanelRequest("batch", CefrLevel.A1, KNOWLEDGE, QUIZ,
                List.of(), List.of(), null, null, 1, List.of(), false);
        var proposal = new DefaultPanelAgentProposalParser().parse(run, request);
        assertTrue(proposal.isAvailable());
        var candidate = proposal.getCandidates().get(0);
        assertEquals(PanelCandidateKind.MULTIPLE_CHOICE_CONVERSION, candidate.getKind());
        var choice = candidate.getQuiz().getForm().getMultipleChoice();
        assertEquals(List.of("aren't", "don't", "doesn't"), choice.getItems().stream().map(MultipleChoiceItemEntity::getId).toList());
        assertEquals(List.of("aren't", "don't", "doesn't"), choice.getItems().stream().map(MultipleChoiceItemEntity::getLabel).toList());
        assertEquals(List.of(0.0, 1.0, 0.0), choice.getItems().stream().map(MultipleChoiceItemEntity::getIncidence).toList());
        assertEquals("don't", choice.correctItem().orElseThrow().getLabel());
        assertEquals("SINGLE", choice.getSelection());

        var repository = new FileSystemCourseRepository(new CourseValidatorImpl());
        var locator = new DefaultCourseElementLocator();
        var course = course();
        var original = locator.snapshot(course, AuditTarget.QUIZ, QUIZ).orElseThrow().getQuiz();
        if (multipleVariants) {
            original.setSentences(List.of("You don't work at the bank.", "You do not work at the bank."));
        }
        var originalSentences = List.copyOf(original.getSentences());
        var vocabulary = mock(PanelVocabularyContext.class);
        var config = new PanelRunnerConfig();
        config.setProposer(panelRequest -> new DefaultPanelAgentProposalParser().parse(run, panelRequest));
        // Only the external audit boundary is simulated; candidate application and screening remain real.
        config.setCourseMapper(mock(CourseMapper.class));
        config.setAuditEngine(mock(AuditEngine.class));
        var factory = new DefaultPanelRunnerFactory();
        var topic = course.getRoot().getMilestones().get(0).getTopics().get(0).getKnowledges().get(0);
        var topicWork = new TopicWork("batch", CefrLevel.A1, "m", "t", topic, SentenceMode.FILL, List.of(),
                null, new ConversionOrder(KNOWLEDGE, FormKind.MULTIPLE_CHOICE, 3), Map.of(), List.of());
        var results = factory.create(config).runTopics(List.of(topicWork), new PanelRunSettings(1, 1, 1, List.of()),
                factory.allowance(1, 1), decision -> {}, vocabulary);
        var decision = results.get(0).getDecisions().get(0);
        if (multipleVariants) {
            assertEquals(PanelOutcomeKind.NO_WINNER, decision.getOutcome());
            assertNull(decision.getAccepted());
            assertTrue(decision.getRejections().stream().flatMap(rejection -> rejection.getReasons().stream())
                    .anyMatch(reason -> reason.contains("oraciones guardadas")));
            verify(vocabulary, never()).accepted(any(), any());
            assertEquals(originalSentences, original.getSentences());
            assertEquals("CLOZE", original.getKind());
            return;
        }
        assertEquals(PanelOutcomeKind.ACCEPTED, decision.getOutcome(), decision.getRejections().toString());
        var accepted = ArgumentCaptor.forClass(KnowledgeEntity.class);
        verify(vocabulary).accepted(any(), accepted.capture());
        var after = accepted.getValue().getQuizTemplates().get(0);
        assertEquals(originalSentences, after.getSentences());
        repository.save(locator.replace(course, new CourseElementSnapshot(AuditTarget.QUIZ, QUIZ, after, null)), work.resolve("course"));
        var reread = locator.snapshot(repository.load(work.resolve("course")), AuditTarget.QUIZ, QUIZ).orElseThrow().getQuiz();
        assertEquals(choice, reread.getForm().getMultipleChoice());
        assertEquals(original.getSentences(), reread.getSentences());
        assertEquals("audio", reread.getAnswerAudioUrl());

        var json = new ObjectMapper();
        var stored = json.readTree(work.resolve("course/a1/determiners/much-many/quizzes.json").toFile()).get(0).path("form");
        assertFalse(stored.has("multipleChoice"));
        assertEquals("SINGLE", stored.path("selection").asText());
        assertEquals(List.of("aren't", "don't", "doesn't"), json.convertValue(stored.path("items").findValues("id"), List.class));
        assertEquals(List.of("You don't work at the bank."), json.convertValue(stored.path("sentences"), List.class));
        var form = (Map<?, ?>) repository.project(reread, List.of("form")).get("form");
        assertFalse(form.containsKey("multipleChoice"));
        assertEquals(List.of(Map.of("id", "aren't", "label", "aren't", "incidence", 0.0),
                Map.of("id", "don't", "label", "don't", "incidence", 1.0),
                Map.of("id", "doesn't", "label", "doesn't", "incidence", 0.0)), form.get("items"));
        assertEquals("SINGLE", form.get("selection"));
        assertEquals(original.getSentences(), form.get("sentences"));
    }

    private static CourseEntity course() {
        var quiz = new QuizTemplateEntity(QUIZ, QUIZ, "CLOZE", KNOWLEDGE, "Much or many", "Completa.",
                "¿Conoces a mucha gente?", "don't", "Determiners",
                new FormEntity("CLOZE", 1, "", "", List.of(
                        new SentencePartEntity(SentencePartKind.TEXT, "You", null),
                        new SentencePartEntity(SentencePartKind.CLOZE, "", List.of("don't")),
                        new SentencePartEntity(SentencePartKind.TEXT, "work at the bank.", null)), null, null),
                0, 0, 0, "", "", "", "audio", "", "Countable nouns.", "", List.of("You don't work at the bank."), null);
        String rootId = "100000000000000000000001", milestoneId = "200000000000000000000001";
        String topicId = "300000000000000000000001";
        var knowledge = new KnowledgeEntity(KNOWLEDGE, "don't", NodeKind.KNOWLEDGE, "Much or many", "old-k",
                topicId, true, "Elige.", 1, "much-many", List.of(quiz), SentenceMode.FILL);
        var topic = new TopicEntity(topicId, "determiners", NodeKind.TOPIC, "Determiners", "old-t", milestoneId,
                List.of(KNOWLEDGE), List.of(KNOWLEDGE), 1, "determiners", List.of(knowledge));
        var milestone = new MilestoneEntity(milestoneId, "A1", NodeKind.MILESTONE, "A1", "old-m", rootId,
                List.of(topicId), 1, "a1", List.of(topic));
        return new CourseEntity("000000000000000000000001", "Course", List.of(KNOWLEDGE),
                new RootNodeEntity(rootId, "course", NodeKind.ROOT, "Course", List.of(milestoneId), List.of(milestone)), "course");
    }
}
