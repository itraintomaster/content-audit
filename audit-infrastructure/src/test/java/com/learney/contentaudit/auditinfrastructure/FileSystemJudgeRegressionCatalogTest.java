package com.learney.contentaudit.auditinfrastructure;

import com.learney.contentaudit.revisiondomain.problems.*;
import com.learney.contentaudit.auditdomain.judging.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static com.learney.contentaudit.revisiondomain.problems.ContentProblem.*;

class FileSystemJudgeRegressionCatalogTest {
    @TempDir Path course;
    final Instant at = Instant.parse("2026-10-21T00:00:00Z");
    ContentProblem.Case example(String id, Use use) {
        var quiz = Map.<String, Object>of("id", "quiz", "miniTheory", use == Use.GOOD ? "$it$ lleva $is$." : "$is$ va con $it$.", "instructions", "Elige be.");
        return new ContentProblem.Case(id, "quiz", "batch", "/tmp/work", 1, 1, use, "", "El sujeto lleva el verbo",
                Map.of("after", Map.of("quiz", quiz), "examContext", Map.of("level", "A1")), at);
    }
    ContentProblem problem(Status status, List<ContentProblem.Case> cases) {
        return new ContentProblem("PRB-test", "Mini-teoría no muy precisa", "El sujeto lleva el verbo", List.of(),
                List.of("miniTheory"), status, 0, cases, List.of(), Map.of("judgeName", "mini-theory"), at, at);
    }
    @Test void confirmationAutomaticallyAddsFrozenCasesAndControlsOnceAndNeverDeletesHistory() {
        var store = new FileSystemProblemStore(course);
        var collecting = problem(Status.COLLECTING, List.of(example("bad", Use.EXAM), example("good", Use.GOOD)));
        store.save(collecting, -1);
        var catalog = new FileSystemJudgeRegressionCatalog(course);
        assertTrue(catalog.suite("mini-theory").cases().isEmpty());
        var service = new ProblemService(store, Clock.fixed(at, ZoneOffset.UTC));
        var confirmed = service.update(collecting.id(), 0, Status.READY, "confirmar", "", null, Actor.OPERATOR);
        var suite = catalog.suite("mini-theory");
        assertEquals(2, suite.cases().size());
        assertEquals(1, suite.cases().stream().filter(JudgeRegressionCase::expectedDetected).count());
        assertEquals(Set.of("PRB-test"), suite.cases().stream().map(JudgeRegressionCase::problemId).collect(java.util.stream.Collectors.toSet()));
        assertTrue(suite.cases().get(0).expectedSource().contains("referencia de comparación"));
        assertEquals(suite, new FileSystemJudgeRegressionCatalog(course).suite("mini-theory"));
        service.removeCases(confirmed.id(), confirmed.revision(), Set.of("bad", "good"));
        assertEquals(suite, catalog.suite("mini-theory"));
        assertTrue(catalog.suite("naturalness").cases().isEmpty());
    }
    @Test void changedContentAddsANewMandatoryReference() {
        var catalog = new FileSystemJudgeRegressionCatalog(course);
        var original = problem(Status.WORKING, List.of(example("bad", Use.EXAM)));
        catalog.confirm(original);
        String fingerprint = catalog.suite("mini-theory").fingerprint();
        var c = example("bad", Use.EXAM);
        var revised = new LinkedHashMap<String, Object>(c.snapshot());
        revised.put("after", Map.of("quiz", Map.of("id", "quiz", "miniTheory", "$are$ va con $they$.")));
        var changed = new ContentProblem.Case(c.id(), c.quizId(), c.batchId(), c.workdir(), 1, 1, c.use(), "", c.expected(), revised, at);
        catalog.confirm(problem(Status.WORKING, List.of(changed)));
        assertEquals(2, catalog.suite("mini-theory").cases().size());
        assertNotEquals(fingerprint, catalog.suite("mini-theory").fingerprint());
    }
    @Test void contradictoryReclassificationIsRejectedWithoutSavingTheProblemOrCatalog() {
        var store = new FileSystemProblemStore(course);
        var original = problem(Status.WORKING, List.of(example("bad", Use.EXAM)));
        store.save(original, -1);
        var catalog = new FileSystemJudgeRegressionCatalog(course);
        var before = catalog.suite("mini-theory");
        var service = new ProblemService(store, Clock.fixed(at, ZoneOffset.UTC));
        var rejected = assertThrows(IllegalArgumentException.class,
                () -> service.use(original.id(), 0, "bad", Use.GOOD, Actor.OPERATOR));
        assertTrue(rejected.getMessage().contains("referencia permanente"));
        assertTrue(rejected.getMessage().contains(original.id()));
        assertEquals(original, store.load(original.id()).orElseThrow());
        assertEquals(before, catalog.suite("mini-theory"));
    }

    @Test void explicitJudgeIdentityPreventsAddingTheSameExpectedFailureToOtherJudges() {
        var source = problem(Status.READY, List.of(example("bad", Use.EXAM)));
        var p = new ContentProblem(source.id(), source.title(), source.criterion(), source.failures(), List.of("miniTheory", "translation"),
                source.status(), 0, source.cases(), source.activity(), Map.of("judgeName", "mini-theory"), at, at);
        var catalog = new FileSystemJudgeRegressionCatalog(course); catalog.confirm(p);
        assertEquals(List.of("mini-theory"), catalog.judgeNames());
    }
    @Test void aCorrectionOfAnswersDoesNotInventASingleAnswerExamReference() {
        var p = new ContentProblem("PRB-separator", "El separador de respuestas está mal", "Cada alternativa es completa",
                List.of(), List.of("statement", "hint", "answers"), Status.FOR_TESTING, 0,
                List.of(example("separator", Use.EXAM)), List.of(),
                Map.of("changes", List.of(Map.of("scope", "corrector"), Map.of("scope", "rule")),
                        "judgesEvaluated", false), at, at);
        var store = new FileSystemProblemStore(course);
        store.save(p, -1);
        assertEquals(p, store.load(p.id()).orElseThrow());
        assertTrue(new FileSystemJudgeRegressionCatalog(course).judgeNames().isEmpty());
    }
    @Test void theEditedPartsAloneDoNotDetermineWhichJudgeMustRejectACase() {
        var source = problem(Status.READY, List.of(example("bad", Use.EXAM)));
        var p = new ContentProblem(source.id(), source.title(), source.criterion(), List.of(),
                List.of("miniTheory", "instructions", "translation", "answers", "options"), source.status(),
                0, source.cases(), List.of(), Map.of(), at, at);
        assertTrue(FileSystemJudgeRegressionCatalog.judgesOf(p).isEmpty());
        var linked = new ContentProblem(source.id(), source.title(), source.criterion(), List.of("mini-theory:overstated-for-quiz"),
                p.parts(), source.status(), 0, source.cases(), List.of(), Map.of(), at, at);
        assertEquals(Set.of("mini-theory"), FileSystemJudgeRegressionCatalog.judgesOf(linked));
    }
    @Test void aJudgeChangeWithoutAnIdentifiedJudgeRequiresAnExplicitRelationship() {
        var source = problem(Status.READY, List.of(example("bad", Use.EXAM)));
        var p = new ContentProblem(source.id(), source.title(), source.criterion(), List.of(), source.parts(),
                source.status(), 0, source.cases(), List.of(),
                Map.of("changes", List.of(Map.of("scope", "judge"))), at, at);
        var error = assertThrows(IllegalArgumentException.class, () -> new FileSystemProblemStore(course).save(p, -1));
        assertTrue(error.getMessage().contains("delivery.judgeName"));
        assertTrue(new FileSystemProblemStore(course).list().isEmpty());
    }
    @Test void confirmationNeedsFrozenExerciseAndDoesNotUseModelVotesForExpectation() {
        var bad = new ContentProblem.Case("bad", "quiz", "batch", "/tmp", 1, 1, Use.EXAM, "", "", Map.of("evaluation", Map.of("observado", Map.of("resultado", "FAILS"))), at);
        var store = new FileSystemProblemStore(course);
        assertThrows(IllegalArgumentException.class, () -> store.save(problem(Status.READY, List.of(bad)), -1));
        assertTrue(store.list().isEmpty());
        var healthy = example("good", Use.GOOD);
        var snapshot = new LinkedHashMap<String, Object>(healthy.snapshot()); snapshot.put("evaluation", Map.of("observado", Map.of("resultado", "FAILS")));
        var control = new ContentProblem.Case(healthy.id(), healthy.quizId(), healthy.batchId(), healthy.workdir(), 1, 1, Use.GOOD, "", "Sano por referencia del operador", snapshot, at);
        store.save(problem(Status.READY, List.of(control)), -1);
        assertFalse(new FileSystemJudgeRegressionCatalog(course).suite("mini-theory").cases().get(0).expectedDetected());
    }
    @Test void takenFingerprintAndTrustedRegressionFailuresSurviveProcessRestarts() {
        var registry = new FileSystemJudgeExamRegistry(course);
        var taken = new JudgeExamRecord("mini-theory", "v1", JudgeExamStatus.TAKEN, 42, 0, List.of(), List.of(), false, "photo", null, at);
        taken.setExamFingerprint("sha256:prepared");
        taken.setRegressionSuiteFingerprint("sha256:suite");
        taken.setRegressionCaseIds(List.of("REG-a", "REG-b"));
        taken.setRegressionMismatchedCaseIds(List.of("REG-b"));
        registry.record(taken);
        var loaded = new FileSystemJudgeExamRegistry(course).history("mini-theory").get(0);
        assertEquals(taken, loaded);
        assertEquals(List.of("REG-b"), loaded.getRegressionMismatchedCaseIds());
    }
    @Test void capturesMissingLevelBeforeTheOriginalCourseIsRemoved() throws Exception {
        Path knowledge = course.resolve("a1/topic/knowledge"); Files.createDirectories(knowledge);
        Files.writeString(course.resolve("a1/_milestone.json"), "{\"label\":\"A1\"}");
        Files.writeString(knowledge.resolve("quizzes.json"), "[{\"id\":\"quiz\"}]");
        Files.writeString(knowledge.resolve("_knowledge.json"), "{\"label\":\"Título del conocimiento\",\"instructions\":\"Consigna del conocimiento\",\"sentenceMode\":\"FILL\",\"isRule\":false}");
        Files.writeString(knowledge.getParent().resolve("_topic.json"), "{\"label\":\"Tema original\"}");
        var c = example("bad", Use.EXAM);
        var frozen = new ContentProblem.Case(c.id(), c.quizId(), c.batchId(), c.workdir(), 1, 1, c.use(), "", c.expected(), Map.of("after", c.snapshot().get("after")), at);
        var catalog = new FileSystemJudgeRegressionCatalog(course); catalog.confirm(problem(Status.READY, List.of(frozen)));
        Files.delete(knowledge.resolve("quizzes.json"));
        var context = (Map<?, ?>) catalog.suite("mini-theory").cases().get(0).snapshot().get("examContext");
        assertEquals("A1", context.get("level"));
        assertEquals(false, context.get("isRule"));
        assertEquals("Consigna del conocimiento", context.get("knowledgeInstructions"));
        assertEquals("Título del conocimiento", context.get("title"));
        assertEquals("Tema original", context.get("topicName"));
        var quiz = (Map<?, ?>) ((Map<?, ?>) catalog.suite("mini-theory").cases().get(0).snapshot().get("after")).get("quiz");
        assertEquals("Elige be.", quiz.get("instructions"));
    }
}
