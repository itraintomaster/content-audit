package com.learney.contentaudit.revisioninfrastructure.preliminary;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.coursedomain.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemPreliminaryOpinionSourceTest {
    @TempDir Path folder;
    private final ObjectMapper json = new ObjectMapper();

    @Test void importsOriginalExamPayloadProvenanceAndPhotoWithoutInvokingAJudge() throws Exception {
        var repository = source();
        var opinions = FileSystemPreliminaryOpinionSourceFactory.create(repository).load(folder.toString());
        assertEquals(3, opinions.size());
        var naturalness = opinions.stream().filter(o -> o.analyzer().equals("naturalness")).findFirst().orElseThrow();
        assertEquals("exam:native-usage-judge", naturalness.evaluatorId());
        assertEquals("EXAM", naturalness.namespace());
        assertEquals("{\"assessments\":[{\"status\":\"PASS\"}],\"rationale\":\"exact source\"}", naturalness.payload());
        assertEquals("codex-cli", naturalness.runtime().getProviderName());
        assertEquals("q1", naturalness.snapshot().getQuiz().getId());
        assertEquals(64, naturalness.sourceRecordHash().length());
        verify(repository).load(folder.resolve("course"));
        verifyNoMoreInteractions(repository);
    }

    @Test void rejectsMetricsThatDoNotMatchTheEffectiveRecordProvider() throws Exception {
        var repository = source();
        Path ledger = folder.resolve("trial-workdir/.content-audit/evaluations/exam_native-usage-judge.jsonl");
        Files.writeString(ledger, Files.readString(ledger).replace("codex-cli", "other-provider"));
        var error = assertThrows(IllegalArgumentException.class,
                () -> FileSystemPreliminaryOpinionSourceFactory.create(repository).load(folder.toString()));
        assertTrue(error.getMessage().contains("procedencia"));
    }

    @Test void rejectsARecordFromTheCourseNamespaceInsteadOfExam() throws Exception {
        var repository = source();
        Path ledger = folder.resolve("trial-workdir/.content-audit/evaluations/exam_native-usage-judge.jsonl");
        Files.writeString(ledger, Files.readString(ledger).replace("exam:native-usage-judge", "native-usage-judge"));
        assertThrows(IllegalArgumentException.class,
                () -> FileSystemPreliminaryOpinionSourceFactory.create(repository).load(folder.toString()));
    }

    @Test void refusesAnIncompleteOpinionSet() throws Exception {
        var repository = source();
        Files.writeString(folder.resolve("trial-workdir/.content-audit/evaluations/exam_a1-didactic-judge.jsonl"), "");
        var error = assertThrows(IllegalArgumentException.class,
                () -> FileSystemPreliminaryOpinionSourceFactory.create(repository).load(folder.toString()));
        assertTrue(error.getMessage().contains("Faltan veredictos"));
    }

    private CourseRepository source() throws Exception {
        var quiz = new QuizTemplateEntity(); quiz.setId("q1"); quiz.setKnowledgeId("k1");
        var knowledge = new KnowledgeEntity(); knowledge.setQuizTemplates(List.of(quiz));
        var topic = new TopicEntity(); topic.setKnowledges(List.of(knowledge));
        var milestone = new MilestoneEntity(); milestone.setTopics(List.of(topic));
        var root = new RootNodeEntity(); root.setMilestones(List.of(milestone));
        var course = new CourseEntity(); course.setRoot(root);
        var repository = mock(CourseRepository.class);
        when(repository.load(folder.resolve("course"))).thenReturn(course);
        json.writeValue(folder.resolve("photo-manifest.json").toFile(), Map.of("targets", List.of("q1")));
        Path ledgers = folder.resolve("trial-workdir/.content-audit/evaluations"); Files.createDirectories(ledgers);
        for (var pair : Map.of("naturalness", "native-usage-judge", "a1-didactic", "a1-didactic-judge", "pragmatism", "pragmatism-judge").entrySet()) {
            var runtime = Map.of("kind", "EXPLICIT", "providerName", "codex-cli", "modelName", "gpt-6.1-sol", "reasoningEffort", "medium");
            json.writeValue(folder.resolve(pair.getKey() + "-metrics.json").toFile(), Map.of("judgeVersion", "v1", "runtime", runtime));
            String payload = "{\"assessments\":[{\"status\":\"PASS\"}],\"rationale\":\"exact source\"}";
            var record = Map.of("subjectRef", "q1", "evaluatorVersion", "v1", "evaluatorId", "exam:" + pair.getValue(),
                    "runtimeKind", "EXPLICIT", "providerName", "codex-cli", "modelName", "gpt-6.1-sol", "reasoningEffort", "medium",
                    "payload", payload, "contentFingerprint", "source-fingerprint", "recordedAt", "2026-10-14T10:00:00Z");
            Files.writeString(ledgers.resolve("exam_" + pair.getValue() + ".jsonl"), json.writeValueAsString(record) + "\n");
        }
        return repository;
    }
}
