package com.learney.contentaudit.revisiondomain.problems;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import static com.learney.contentaudit.revisiondomain.problems.ContentProblem.*;

/** Problems never call judges, change a course or publish. The operator alone resolves them. */
public final class ProblemService {
    private final ProblemStore store;
    private final Clock clock;
    public ProblemService(ProblemStore store, Clock clock) { this.store = store; this.clock = clock; }

    public ContentProblem create(String title, String criterion, List<String> failures, List<String> parts) {
        Instant now = clock.instant();
        var p = new ContentProblem("PRB-" + UUID.randomUUID().toString().substring(0, 8), title, criterion,
                failures, parts, Status.COLLECTING, 0, List.of(), List.of(), Map.of(), now, now);
        store.save(p, -1); return p;
    }
    public ContentProblem get(String id) {
        return store.load(id).orElseThrow(() -> new IllegalArgumentException("No existe el problema «" + id + "»."));
    }
    public List<ContentProblem> list() { return store.list(); }

    public ContentProblem edit(String id, long revision, String title, String criterion,
            List<String> failures, List<String> parts) {
        var p = current(id, revision);
        return save(p, title == null ? p.title() : title, criterion == null ? p.criterion() : criterion,
                failures == null ? p.failures() : failures, parts == null ? p.parts() : parts,
                Status.COLLECTING, p.cases(), Map.of(), Actor.OPERATOR, "ficha", "Criterio actualizado");
    }
    public ContentProblem addCases(String id, long revision, List<Case> additions) {
        var p = current(id, revision);
        if (additions == null || additions.isEmpty()) throw new IllegalArgumentException("Elegí al menos un caso.");
        var cases = new ArrayList<>(p.cases());
        var ids = new HashSet<String>(); cases.forEach(c -> ids.add(c.id()));
        for (var c : additions) if (ids.add(c.id())) cases.add(c);
        if (cases.size() == p.cases().size()) return p;
        // A new observation invalidates the old delivery and reopens a resolved problem.
        int count = cases.size() - p.cases().size();
        return save(p, p.title(), p.criterion(), p.failures(), p.parts(), Status.COLLECTING,
                cases, Map.of(), Actor.OPERATOR, "casos", count == 1 ? "1 caso nuevo" : count + " casos nuevos");
    }
    public ContentProblem removeCases(String id, long revision, Set<String> ids) {
        var p = current(id, revision);
        var cases = p.cases().stream().filter(c -> !ids.contains(c.id())).toList();
        return save(p, p.title(), p.criterion(), p.failures(), p.parts(), Status.COLLECTING,
                cases, Map.of(), Actor.OPERATOR, "deshacer", "Casos retirados por el operador");
    }
    public ContentProblem rollbackAnnotation(ContentProblem before, long revision) {
        var current = current(before.id(), revision);
        return save(current, before.title(), before.criterion(), before.failures(), before.parts(),
                before.status(), before.cases(), before.delivery(), Actor.OPERATOR,
                "anotacion-fallida", "Se conservó la ficha anterior");
    }
    public ContentProblem use(String id, long revision, String caseId, Use use, Actor actor) {
        var p = current(id, revision);
        if (actor == Actor.AGENT && (use == Use.GOOD || p.cases().stream()
                .anyMatch(c -> c.id().equals(caseId) && c.use() == Use.GOOD))) {
            throw new IllegalArgumentException("Solo el operador decide qué casos están bien.");
        }
        if (p.cases().stream().noneMatch(c -> c.id().equals(caseId))) throw new IllegalArgumentException("No existe ese caso.");
        Status status = p.status() == Status.FOR_TESTING || p.status() == Status.RESOLVED
                ? (actor == Actor.AGENT ? Status.WORKING : Status.COLLECTING) : p.status();
        return save(p, p.title(), p.criterion(), p.failures(), p.parts(), status,
                p.cases().stream().map(c -> c.id().equals(caseId) ? c.withUse(use) : c).toList(),
                Map.of(), actor, "uso", "Caso " + caseId + ": " + use);
    }
    public ContentProblem update(String id, long revision, Status status, String step, String note,
            Map<String, Object> delivery, Actor actor) {
        var p = current(id, revision);
        if (actor == Actor.AGENT && status != Status.WORKING && status != Status.FOR_TESTING) {
            throw new IllegalArgumentException("El agente registra En trabajo o Para probar; Resuelto lo decide el operador.");
        }
        if (status != Status.COLLECTING && (p.criterion().isBlank() || p.cases().isEmpty())) {
            throw new IllegalArgumentException("Completá el criterio y al menos un caso antes de pasar al agente.");
        }
        var result = delivery == null ? p.delivery() : validateDelivery(p, delivery);
        if (status == Status.FOR_TESTING && result.isEmpty()) {
            throw new IllegalArgumentException("Para probar necesita los cambios, versiones y resultados de la entrega.");
        }
        if (status == Status.FOR_TESTING) {
            if (p.status() != Status.WORKING && p.status() != Status.FOR_TESTING)
                throw new IllegalArgumentException("El agente debe registrar En trabajo antes de entregar Para probar.");
            verifyPassingDelivery(p, result);
        }
        if (status == Status.RESOLVED && p.status() != Status.FOR_TESTING) {
            throw new IllegalArgumentException("Primero debe haber una entrega Para probar.");
        }
        return save(p, p.title(), p.criterion(), p.failures(), p.parts(), status, p.cases(),
                result, actor, required(step, "Indicá en qué paso está el trabajo."), note == null ? "" : note);
    }

    private Map<String, Object> validateDelivery(ContentProblem p, Map<String, Object> data) {
        if (!(data.get("changes") instanceof List<?> changes) || changes.isEmpty()
                || !(data.get("results") instanceof List<?> results) || results.isEmpty()) {
            throw new IllegalArgumentException("La entrega necesita changes y results, ambos con contenido.");
        }
        var ids = new HashSet<String>(); p.cases().forEach(c -> ids.add(c.id()));
        for (var raw : changes) {
            if (!(raw instanceof Map<?, ?> c)) throw new IllegalArgumentException("Cambio inválido.");
            for (String key : List.of("path", "version", "summary", "scope")) {
                if (!(c.get(key) instanceof String text) || text.isBlank())
                    throw new IllegalArgumentException("Cada cambio necesita " + key + ".");
            }
            if (!List.of("judge", "corrector", "rule").contains(c.get("scope")))
                throw new IllegalArgumentException("scope debe ser judge, corrector o rule.");
        }
        var seen = new HashSet<String>();
        for (var raw : results) {
            if (!(raw instanceof Map<?, ?> r) || !(r.get("caseId") instanceof String id)
                    || !ids.contains(id) || !seen.add(id)) throw new IllegalArgumentException("Resultado de un caso desconocido o repetido.");
            if (!(r.get("detected") instanceof Boolean) && !(r.get("fixed") instanceof Boolean)) {
                throw new IllegalArgumentException("Cada resultado debe indicar detected o fixed.");
            }
            if (!(r.get("version") instanceof String version) || version.isBlank()) {
                throw new IllegalArgumentException("Cada resultado necesita la versión efectiva que se probó.");
            }
            if (!(r.get("modelConsulted") instanceof Boolean))
                throw new IllegalArgumentException("Indicá explícitamente modelConsulted en cada resultado.");
            if (Boolean.TRUE.equals(r.get("modelConsulted"))) {
                for (String key : List.of("provider", "model", "runtimeVersion")) {
                    if (!(r.get(key) instanceof String text) || text.isBlank())
                        throw new IllegalArgumentException("Falta la procedencia efectiva: " + key);
                }
            }
        }
        return ContentProblem.freeze(data);
    }
    private void verifyPassingDelivery(ContentProblem p, Map<String, Object> data) {
        var results = (List<?>) data.get("results");
        boolean judge = ((List<?>) data.get("changes")).stream()
                .anyMatch(c -> "judge".equals(((Map<?, ?>) c).get("scope")));
        Map<String, Map<?, ?>> byCase = new HashMap<>();
        for (var raw : results) {
            var r = (Map<?, ?>) raw;
            byCase.put((String) r.get("caseId"), r);
            var c = p.cases().stream().filter(v -> v.id().equals(r.get("caseId"))).findFirst().orElseThrow();
            if (Boolean.FALSE.equals(r.get("fixed")) || (r.get("detected") instanceof Boolean detected
                    && detected != (c.use() != Use.GOOD))) {
                throw new IllegalArgumentException("La evaluación falló. Guardá el resultado En trabajo.");
            }
        }
        if (judge) {
            boolean positive = false, negative = false;
            for (var c : p.cases()) if (c.use() != Use.EXAMPLE) {
                var r = byCase.get(c.id());
                if (r == null || !(r.get("detected") instanceof Boolean))
                    throw new IllegalArgumentException("El juez debe evaluar todos los casos Examen y Está bien.");
                positive |= c.use() == Use.EXAM; negative |= c.use() == Use.GOOD;
            }
            if (!positive || !negative) throw new IllegalArgumentException("El examen del juez necesita positivos y controles Está bien.");
        }
    }
    private ContentProblem current(String id, long revision) {
        var p = get(id);
        if (p.revision() != revision) throw new IllegalStateException("El problema cambió. Volvé a leer la ficha.");
        return p;
    }
    private ContentProblem save(ContentProblem p, String title, String criterion, List<String> failures,
            List<String> parts, Status status, List<Case> cases, Map<String, Object> delivery,
            Actor actor, String step, String note) {
        var activity = new ArrayList<>(p.activity());
        Instant now = clock.instant(); activity.add(new Activity(now, actor, step, note, status));
        var updated = new ContentProblem(p.id(), title, criterion, failures, parts, status,
                p.revision() + 1, cases, activity, delivery, p.createdAt(), now);
        store.save(updated, p.revision()); return updated;
    }
}
