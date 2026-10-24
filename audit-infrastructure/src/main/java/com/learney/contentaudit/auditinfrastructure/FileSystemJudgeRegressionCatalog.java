package com.learney.contentaudit.auditinfrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.auditdomain.JudgeRegressionCatalog;
import com.learney.contentaudit.auditdomain.judging.*;
import com.learney.contentaudit.revisiondomain.problems.*;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
    public record Catalog(List<JudgeRegressionCase> cases) {
        public Catalog { cases = List.copyOf(cases); }
    }
    public List<JudgeRegressionCase> sync() {
        return problems.exclusive(() -> {
            var cases = new ArrayList<>(read());
            for (var problem : problems.list()) merge(cases, problem);
            writeIfChanged(cases);
            return List.copyOf(cases);
        });
    }
    /** Called with the same course lock as a problem save; labels survive removal/reopening. */
    public void confirm(ContentProblem problem) {
        problems.exclusive(() -> {
            var cases = new ArrayList<>(read());
            merge(cases, problem); writeIfChanged(cases); return null;
        });
    }
    public List<String> judgeNames() { return sync().stream().map(JudgeRegressionCase::judgeName).distinct().sorted().toList(); }
    @Override public JudgeRegressionSuite suite(String judgeName) {
        var cases = sync().stream().filter(c -> c.judgeName().equals(judgeName))
                .sorted(Comparator.comparing(JudgeRegressionCase::id)).toList();
        return cases.isEmpty() ? JudgeRegressionSuite.empty()
                : new JudgeRegressionSuite(digest(canonical(cases)), cases);
    }
    private List<JudgeRegressionCase> read() {
        return JsonFiles.read(mapper, file, Catalog.class).map(Catalog::cases).orElse(List.of());
    }
    private void writeIfChanged(List<JudgeRegressionCase> cases) {
        if (!cases.equals(read())) JsonFiles.writeAtomically(mapper, file, new Catalog(cases));
    }
    private void merge(List<JudgeRegressionCase> stored, ContentProblem problem) {
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
                for (var prior : stored) if (prior.judgeName().equals(judge)
                        && prior.expectedDetected() != detected
                        && canonical(prior.snapshot().get("after")).equals(canonical(c.snapshot().get("after")))) {
                    throw new IllegalArgumentException("El caso " + c.id() + " de " + problem.id()
                            + " contradice la referencia permanente " + prior.id() + " de " + prior.problemId()
                            + " («" + prior.problemTitle() + "»): el mismo contenido ya tiene el resultado esperado opuesto. "
                            + "Revisá la referencia esperada antes de reclasificar ese contenido; no se guardó el cambio.");
                }
                // Include content and expectation in the identity: a later correction adds a new
                // reference instead of silently rewriting a previously mandatory case.
                String identity = problem.id() + "\n" + c.id() + "\n" + judge + "\n" + detected
                        + "\n" + reason + "\n" + canonical(c.snapshot().get("after"));
                String id = "REG-" + digest(identity).substring(7, 31);
                if (stored.stream().anyMatch(v -> v.id().equals(id))) continue;
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
            "pragmatism", "a1-didactic");
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
