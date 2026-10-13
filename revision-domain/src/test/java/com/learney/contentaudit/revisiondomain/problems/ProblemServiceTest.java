package com.learney.contentaudit.revisiondomain.problems;

import static org.junit.jupiter.api.Assertions.*;
import static com.learney.contentaudit.revisiondomain.problems.ContentProblem.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ProblemServiceTest {
    final Map<String, ContentProblem> data = new HashMap<>();
    final Clock clock = Clock.fixed(Instant.parse("2026-10-11T12:00:00Z"), ZoneOffset.UTC);
    final ProblemService service = new ProblemService(new ProblemStore() {
        public Optional<ContentProblem> load(String id) { return Optional.ofNullable(data.get(id)); }
        public List<ContentProblem> list() { return List.copyOf(data.values()); }
        public void save(ContentProblem p, long revision) {
            if (data.containsKey(p.id()) && data.get(p.id()).revision() != revision) throw new IllegalStateException();
            data.put(p.id(), p);
        }
    }, clock);
    ContentProblem withCases() {
        var p = service.create("Regla incompleta", "Debe explicar el contraste", List.of(), List.of("miniTheory"));
        return service.addCases(p.id(), 0, List.of(example("bad", Use.EXAM), example("good", Use.GOOD)));
    }
    Case example(String id, Use use) {
        return new Case(id, "quiz-" + id, "batch", "/tmp/work", 1, 1, use, "Comentario", "Esperado",
                Map.of("before", Map.of("miniTheory", "Antes"), "after", Map.of("miniTheory", "Después")), clock.instant());
    }
    Map<String, Object> result(String id, boolean detected) {
        return Map.of("caseId", id, "detected", detected, "version", "guide-v2", "modelConsulted", false);
    }
    Map<String, Object> delivery(List<?> results) {
        return Map.of("changes", List.of(Map.of("path", "judge/guide.md", "version", "guide-v2", "summary", "Contraste general", "scope", "judge")), "results", results);
    }
    @Test void operatorCanChooseAMeasuredHealthyControlWithoutRetiringTheDelivery() {
        var p = withCases();
        p = service.update(p.id(), p.revision(), Status.WORKING, "entrega", "",
                delivery(List.of(result("bad", true), result("good", false))), Actor.AGENT);
        var snapshot = new LinkedHashMap<>(example("extra", Use.GOOD).snapshot());
        snapshot.put("evaluation", Map.of("observado", Map.of("resultado", "PASSES")));
        var control = new Case("extra", "quiz-extra", "batch", "/tmp/work", 1, 1,
                Use.GOOD, "Elegido por el operador", "Está bien", snapshot, clock.instant());
        var measured = delivery(List.of(result("bad", true), result("good", false), result("extra", false)));
        var before = p;
        assertThrows(IllegalArgumentException.class, () -> service.selectMeasuredControl(
                before.id(), before.revision(), control, measured, Actor.AGENT));
        var selected = service.selectMeasuredControl(p.id(), p.revision(), control, measured, Actor.OPERATOR);
        assertEquals(Status.WORKING, selected.status());
        assertEquals(p.delivery().get("changes"), selected.delivery().get("changes"));
        assertEquals(3, selected.cases().size());
        var testing = service.update(p.id(), selected.revision(), Status.FOR_TESTING, "probar", "", null, Actor.OPERATOR);
        assertEquals(Status.RESOLVED, service.update(p.id(), testing.revision(), Status.RESOLVED,
                "resuelto", "", null, Actor.OPERATOR).status());
    }

    @Test void aControlCannotHideAFailingCaseOrIntroduceAnotherRuntime() {
        var p = withCases();
        p = service.update(p.id(), p.revision(), Status.WORKING, "entrega", "",
                delivery(List.of(result("bad", true), result("good", false))), Actor.AGENT);
        var snapshot = Map.<String, Object>of("evaluation", Map.of("observado", Map.of("resultado", "PASSES")));
        var control = new Case("extra", "quiz-extra", "batch", "/tmp", 1, 1,
                Use.GOOD, "", "", snapshot, clock.instant());
        var before = p;
        assertThrows(IllegalArgumentException.class, () -> service.selectMeasuredControl(before.id(), before.revision(),
                control, delivery(List.of(result("bad", false), result("good", false), result("extra", false))), Actor.OPERATOR));
        var other = new LinkedHashMap<>(result("extra", false)); other.put("version", "other");
        assertThrows(IllegalArgumentException.class, () -> service.selectMeasuredControl(before.id(), before.revision(),
                control, delivery(List.of(result("bad", true), result("good", false), other)), Actor.OPERATOR));
    }
    @Test void passingJudgeNeedsAllExamCasesAndGoodControlsAndOnlyOperatorCanResolve() {
        var p = withCases();
        var incomplete = delivery(List.of(result("bad", true)));
        assertThrows(IllegalArgumentException.class, () -> service.update(p.id(), p.revision(), Status.FOR_TESTING, "entrega", "", incomplete, Actor.AGENT));
        var failed = delivery(List.of(result("bad", true), result("good", true)));
        assertThrows(IllegalArgumentException.class, () -> service.update(p.id(), p.revision(), Status.FOR_TESTING, "entrega", "", failed, Actor.AGENT));
        var working = service.update(p.id(), p.revision(), Status.WORKING, "examen", "Falso positivo", failed, Actor.AGENT);
        var testing = service.update(p.id(), working.revision(), Status.FOR_TESTING, "entrega", "", delivery(List.of(result("bad", true), result("good", false))), Actor.AGENT);
        assertThrows(IllegalArgumentException.class, () -> service.update(p.id(), testing.revision(), Status.RESOLVED, "cierre", "", null, Actor.AGENT));
        var resolved = service.update(p.id(), testing.revision(), Status.RESOLVED, "cierre", "Revisado", null, Actor.OPERATOR);
        var reopened = service.addCases(p.id(), resolved.revision(), List.of(example("new", Use.EXAM)));
        assertEquals(Status.COLLECTING, reopened.status());
        assertTrue(reopened.delivery().isEmpty());
        assertEquals(3, reopened.cases().size());
    }
    @Test void staleWritesCannotOverwriteAndAgentCannotRedefineNegativeControls() {
        var p = withCases();
        assertThrows(IllegalStateException.class, () -> service.edit(p.id(), 0, "Otro", null, null, null));
        assertThrows(IllegalArgumentException.class, () -> service.use(p.id(), p.revision(), "good", Use.EXAM, Actor.AGENT));
        assertThrows(IllegalArgumentException.class, () -> service.use(p.id(), p.revision(), "bad", Use.GOOD, Actor.AGENT));
        var changed = service.use(p.id(), p.revision(), "bad", Use.EXAMPLE, Actor.AGENT);
        assertEquals(p.cases().get(0).snapshot(), changed.cases().get(0).snapshot());
    }
    @Test void failedAnnotationRestoresThePreviousStatusAndDeliveryWithoutLosingCases() {
        var p = withCases();
        p = service.update(p.id(), p.revision(), Status.WORKING, "trabajo", "", null, Actor.AGENT);
        p = service.update(p.id(), p.revision(), Status.FOR_TESTING, "entrega", "", delivery(List.of(result("bad", true), result("good", false))), Actor.AGENT);
        var before = service.update(p.id(), p.revision(), Status.RESOLVED, "cierre", "", null, Actor.OPERATOR);
        var annotated = service.addCases(p.id(), before.revision(), List.of(example("new", Use.EXAM)));
        var restored = service.rollbackAnnotation(before, annotated.revision());
        assertEquals(before.status(), restored.status()); assertEquals(before.delivery(), restored.delivery());
        assertEquals(before.cases(), restored.cases()); assertEquals(annotated.revision() + 1, restored.revision());
    }
    @Test void caseSnapshotDeeplyFreezesMutableJsonAndExplicitNulls() {
        var part = new LinkedHashMap<String, Object>(); part.put("text", "Original"); part.put("options", null);
        var input = new LinkedHashMap<String, Object>(); input.put("parts", new ArrayList<>(List.of(part)));
        var c = new Case("case", "quiz", "batch", "/tmp", 0, 0, Use.EXAM, "", "", input, clock.instant());
        part.put("text", "Changed"); input.clear();
        var frozen = (Map<?, ?>) ((List<?>) c.snapshot().get("parts")).get(0);
        assertEquals("Original", frozen.get("text")); assertTrue(frozen.containsKey("options"));
        assertThrows(UnsupportedOperationException.class, () -> c.snapshot().put("other", "value"));
    }
    @Test void effectiveModelProvenanceIsRequiredWhenModelsWereConsulted() {
        var p = withCases();
        var r = new HashMap<>(result("bad", true)); r.put("modelConsulted", true);
        assertThrows(IllegalArgumentException.class, () -> service.update(p.id(), p.revision(), Status.WORKING, "medir", "", delivery(List.of(r)), Actor.AGENT));
        r.putAll(Map.of("provider", "codex-cli", "model", "effective-model", "runtimeVersion", "effective-runtime"));
        assertDoesNotThrow(() -> service.update(p.id(), p.revision(), Status.WORKING, "medir", "", delivery(List.of(r)), Actor.AGENT));
    }
}
