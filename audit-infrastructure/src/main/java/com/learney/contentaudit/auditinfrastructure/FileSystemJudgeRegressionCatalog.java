package com.learney.contentaudit.auditinfrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.auditdomain.JudgeRegressionCatalog;
import com.learney.contentaudit.auditdomain.judging.*;
import com.learney.contentaudit.revisiondomain.problems.*;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/** An additive catalog under the course, independent of transient campaigns and judge versions. */
public final class FileSystemJudgeRegressionCatalog implements JudgeRegressionCatalog {
    private final FileSystemProblemStore problems;
    private final Path file;
    private final Path course;
    private final ObjectMapper mapper = JsonFiles.mapper();
    public FileSystemJudgeRegressionCatalog(Path course) {
        problems = new FileSystemProblemStore(course);
        file = problems.directory().getParent().resolve("judge-regressions/catalog.json");
        this.course = problems.directory().getParent().getParent();
    }
    /**
     * F-JEXAM-R021: José's new rule changed what a permanent case expects. The former reference stays in
     * {@code cases} as history; only its replacement is required. Kept apart from the cases so that the
     * suites of the other judges keep their fingerprint.
     */
    public record Rectification(String previousId, String replacementId, String judgeName, String rule,
            String decidedBy, String words, String recordedBy, Instant decidedAt) {}
    public record Catalog(List<JudgeRegressionCase> cases, List<Rectification> rectifications) {
        public Catalog {
            cases = List.copyOf(cases);
            rectifications = rectifications == null ? List.of() : List.copyOf(rectifications);
        }
    }
    /** Every reference ever confirmed, the rectified ones included. */
    public List<JudgeRegressionCase> sync() { return synced().cases(); }
    private Catalog synced() {
        return problems.exclusive(() -> {
            var catalog = read();
            var cases = new ArrayList<>(catalog.cases());
            for (var problem : problems.list()) merge(cases, rectified(catalog), problem);
            var merged = new Catalog(cases, catalog.rectifications());
            writeIfChanged(merged);
            return merged;
        });
    }
    /** Called with the same course lock as a problem save; labels survive removal/reopening. */
    public void confirm(ContentProblem problem) {
        problems.exclusive(() -> {
            var catalog = read();
            var cases = new ArrayList<>(catalog.cases());
            merge(cases, rectified(catalog), problem);
            writeIfChanged(new Catalog(cases, catalog.rectifications())); return null;
        });
    }
    public List<String> judgeNames() { return sync().stream().map(JudgeRegressionCase::judgeName).distinct().sorted().toList(); }
    @Override public JudgeRegressionSuite suite(String judgeName) {
        var catalog = synced();
        var history = rectified(catalog);
        var cases = catalog.cases().stream().filter(c -> c.judgeName().equals(judgeName) && !history.contains(c.id()))
                .sorted(Comparator.comparing(JudgeRegressionCase::id)).toList();
        return cases.isEmpty() ? JudgeRegressionSuite.empty()
                : new JudgeRegressionSuite(digest(canonical(cases)), cases);
    }
    public List<Rectification> rectifications(String judgeName) {
        return synced().rectifications().stream().filter(r -> r.judgeName().equals(judgeName)).toList();
    }
    /**
     * F-JEXAM-R021: a new rule of José turns a permanent reference around. The frozen exercise does not change;
     * the expectation, its reason and its source do, and the former reference is kept as history.
     */
    public JudgeRegressionCase rectify(String id, boolean expectedDetected, String reason, String rule,
            String decidedBy, String words, String recordedBy, Instant at) {
        String why = required(reason, "Decí qué tiene que contestar el juez con la regla nueva.");
        if (rule == null || !rule.matches("F-[A-Z0-9]+-R\\d{3}"))
            throw new IllegalArgumentException("Nombrá la regla nueva que cambia lo esperado, como F-MINIT-R009.");
        String who = required(decidedBy, "Decí quién decidió el cambio: sólo José cambia una referencia.");
        String said = required(words, "Citá las palabras con las que José lo decidió.");
        String by = recordedBy == null || recordedBy.isBlank() ? who : recordedBy.strip();
        Objects.requireNonNull(at);
        return problems.exclusive(() -> {
            var catalog = synced();
            var prior = catalog.cases().stream().filter(c -> c.id().equals(id)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No existe la referencia permanente " + id + "."));
            var done = catalog.rectifications().stream().filter(r -> r.previousId().equals(id)).findFirst();
            if (done.isPresent()) throw new IllegalArgumentException("La referencia " + id + " ya se rectificó: la vigente es "
                    + done.get().replacementId() + ".");
            if (prior.expectedDetected() == expectedDetected) throw new IllegalArgumentException("La referencia " + id
                    + " ya espera eso: rectificar es cambiar el resultado esperado.");
            String date = java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy")
                    .format(at.atZone(java.time.ZoneId.of("America/Argentina/Buenos_Aires")));
            String source = "Rectificada el " + date + " por " + rule + ": decisión de " + who + " («" + said + "»)"
                    + (by.equals(who) ? "" : ", registrada por " + by) + ". Antes esperaba "
                    + (prior.expectedDetected() ? "«tiene un problema»" : "«está bien»") + " (" + prior.id() + ").";
            String newId = "REG-" + digest(prior.id() + "\nrectificada\n" + expectedDetected + "\n" + why + "\n" + rule)
                    .substring(7, 31);
            var replacement = new JudgeRegressionCase(newId, prior.judgeName(), prior.problemId(), prior.problemTitle(),
                    prior.problemCaseId(), prior.quizId(), expectedDetected, why, source, prior.snapshot(), at);
            var cases = new ArrayList<>(catalog.cases()); cases.add(replacement);
            var rectifications = new ArrayList<>(catalog.rectifications());
            rectifications.add(new Rectification(prior.id(), newId, prior.judgeName(), rule, who, said, by, at));
            writeIfChanged(new Catalog(cases, rectifications));
            return replacement;
        });
    }
    private static String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.strip();
    }
    private static Set<String> rectified(Catalog catalog) {
        var ids = new HashSet<String>();
        for (var r : catalog.rectifications()) ids.add(r.previousId());
        return ids;
    }
    private Catalog read() {
        return JsonFiles.read(mapper, file, Catalog.class).orElse(new Catalog(List.of(), List.of()));
    }
    private void writeIfChanged(Catalog catalog) {
        if (!catalog.equals(read())) JsonFiles.writeAtomically(mapper, file, catalog);
    }
    private void merge(List<JudgeRegressionCase> stored, Set<String> rectified, ContentProblem problem) {
        if (problem.status() == ContentProblem.Status.COLLECTING || problem.criterion().isBlank()) return;
        var judges = judgesOf(problem);
        // Editing a case invalidates its delivery. Preserve its previously established owner
        // so reopening the problem cannot silently bypass a contradictory expectation.
        if (judges.isEmpty()) stored.stream().filter(c -> c.problemId().equals(problem.id()))
                .map(JudgeRegressionCase::judgeName).forEach(judges::add);
        if (judges.isEmpty()) return; // A correction-only problem does not redefine a judge.
        for (var c : problem.cases()) {
            if (c.use() == ContentProblem.Use.EXAMPLE) continue;
            if (!(c.snapshot().get("after") instanceof Map<?, ?> after)
                    || !(after.get("quiz") instanceof Map<?, ?>))
                throw new IllegalArgumentException("El caso " + c.id() + " de " + problem.id()
                        + " necesita su ejercicio congelado (after.quiz) antes de confirmar el problema.");
            String reason = c.expected().isBlank() ? problem.criterion() : c.expected();
            boolean detected = c.use() == ContentProblem.Use.EXAM;
            for (String judge : judges) {
                // Include content and expectation in the identity: a later correction adds a new
                // reference instead of silently rewriting a previously mandatory case.
                String identity = problem.id() + "\n" + c.id() + "\n" + judge + "\n" + detected
                        + "\n" + reason + "\n" + canonical(c.snapshot().get("after"));
                String id = "REG-" + digest(identity).substring(7, 31);
                // Already incorporated, maybe rectified since (F-JEXAM-R021): the case brings nothing new.
                if (stored.stream().anyMatch(v -> v.id().equals(id))) continue;
                for (var prior : stored) if (prior.judgeName().equals(judge) && !rectified.contains(prior.id())
                        && prior.expectedDetected() != detected
                        && canonical(prior.snapshot().get("after")).equals(canonical(c.snapshot().get("after")))) {
                    throw new IllegalArgumentException("El caso " + c.id() + " de " + problem.id()
                            + " contradice la referencia permanente " + prior.id() + " de " + prior.problemId()
                            + " («" + prior.problemTitle() + "»): el mismo contenido ya tiene el resultado esperado opuesto. "
                            + "Revisá la referencia esperada antes de reclasificar ese contenido; no se guardó el cambio.");
                }
                var snapshot = new LinkedHashMap<String, Object>(c.snapshot());
                if (!snapshot.containsKey("examContext")) snapshot.put("examContext", contextOf(c));
                stored.add(new JudgeRegressionCase(id, judge, problem.id(), problem.title(), c.id(), c.quizId(),
                        detected, reason, "Criterio y clasificación guardados en el problema " + problem.id()
                                + "; referencia de comparación",
                        snapshot, problem.updatedAt()));
            }
        }
    }
    private static final Set<String> JUDGES = Set.of("quiz-instruction", "hint-coherence", "answer-completeness",
            "multiple-choice-validity", "data-coherence", "exact-duplicates", "translation-sense", "single-answer",
            "mini-theory", "off-level-words", "grammar", "naturalness", "near-duplicates", "multiple-choice-quality",
            "pragmatism", "a1-didactic", "answer-giveaway");
    static Set<String> judgesOf(ContentProblem p) {
        Object explicit = p.delivery().get("judgeName");
        if (explicit instanceof String name && !name.isBlank()) {
            if (!JUDGES.contains(name)) throw new IllegalArgumentException("Juez desconocido en el problema " + p.id() + ": " + name);
            return Set.of(name);
        }
        var judges = new LinkedHashSet<String>();
        for (String failure : p.failures()) {
            for (String judge : JUDGES) if (failure.equals(judge) || failure.startsWith(judge + ":")
                    || failure.startsWith(judge + ".")) judges.add(judge);
        }
        if (!judges.isEmpty()) return judges;
        // An edited field does not identify the business rule that is wrong. For example,
        // an answer-separator correction is not evidence of a missing valid answer.
        // Only an explicit judge or a finding owned by that judge can define its exam.
        boolean judgeChange = p.delivery().get("changes") instanceof List<?> changes && changes.stream()
                .anyMatch(v -> v instanceof Map<?, ?> m && "judge".equals(m.get("scope")));
        if (judges.isEmpty() && judgeChange) throw new IllegalArgumentException("El problema " + p.id()
                + " cambia un juez: indicá delivery.judgeName para incorporar sus casos al examen permanente.");
        return judges;
    }
    private Map<String, Object> contextOf(ContentProblem.Case c) {
        var context = new LinkedHashMap<String, Object>();
        @SuppressWarnings("unchecked") var after = (Map<String, Object>) c.snapshot().get("after");
        @SuppressWarnings("unchecked") var quiz = (Map<String, Object>) after.get("quiz");
        for (String name : List.of("title", "topicName", "instructions", "sentenceMode")) {
            if (quiz.containsKey(name)) context.put(name.equals("instructions") ? "knowledgeInstructions" : name, quiz.get(name));
        }
        // Capture the level once while the original exercise still exists. The frozen quiz keeps
        // the actual judged content; live metadata supplies only context absent from old snapshots.
        try (var paths = java.nio.file.Files.walk(course, 4)) {
            for (Path quizzes : paths.filter(path -> path.getFileName().toString().equals("quizzes.json"))
                    .sorted().toList()) {
                var rows = mapper.readTree(quizzes.toFile());
                boolean found = false;
                for (var row : rows) if (c.quizId().equals(row.path("id").asText())
                        || c.quizId().equals(row.path("_id").path("$oid").asText())) { found = true; break; }
                if (!found) continue;
                Path milestone = quizzes.getParent().getParent().getParent().resolve("_milestone.json");
                if (java.nio.file.Files.isRegularFile(milestone)) {
                    String level = mapper.readTree(milestone.toFile()).path("label").asText();
                    if (level.matches("[ABC][12]")) context.put("level", level);
                }
                Path knowledge = quizzes.getParent().resolve("_knowledge.json");
                if (java.nio.file.Files.isRegularFile(knowledge)) {
                    var data = mapper.readTree(knowledge.toFile());
                    context.put("isRule", data.path("isRule").asBoolean(true));
                    context.put("knowledgeInstructions", data.path("instructions").asText(""));
                    if (data.hasNonNull("label")) context.put("title", data.path("label").asText());
                    else if (data.hasNonNull("title")) context.put("title", data.path("title").asText());
                    if (data.hasNonNull("sentenceMode")) context.put("sentenceMode", data.path("sentenceMode").asText());
                }
                Path topic = quizzes.getParent().getParent().resolve("_topic.json");
                if (java.nio.file.Files.isRegularFile(topic)) {
                    var data = mapper.readTree(topic.toFile());
                    if (data.hasNonNull("label")) context.put("topicName", data.path("label").asText());
                    else if (data.hasNonNull("title")) context.put("topicName", data.path("title").asText());
                }
                break;
            }
        } catch (java.io.IOException e) { throw new java.io.UncheckedIOException("No se pudo congelar el contexto de " + c.quizId(), e); }
        return context;
    }
    private static String canonical(Object value) {
        if (value instanceof Map<?, ?> map) {
            var sorted = new TreeMap<String, Object>(); map.forEach((k, v) -> sorted.put(String.valueOf(k), v));
            return sorted.entrySet().stream().map(e -> canonical(e.getKey()) + ":" + canonical(e.getValue()))
                    .collect(java.util.stream.Collectors.joining(",", "{", "}"));
        }
        if (value instanceof List<?> list) return list.stream().map(FileSystemJudgeRegressionCatalog::canonical)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        try { return JsonFiles.mapper().writeValueAsString(value); }
        catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
    }
    private static String digest(String value) {
        try { return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
