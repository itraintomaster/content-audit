package com.learney.contentaudit.revisioninfrastructure.preliminary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.coursedomain.CourseRepository;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.batch.ContextOpinion.PreliminaryOpinion;
import com.learney.contentaudit.revisiondomain.batch.PreliminaryOpinionSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Imports exact ledger lines and the frozen quizzes those evaluations read. */
final class FileSystemPreliminaryOpinionSource implements PreliminaryOpinionSource {
    private static final Map<String, String> EVALUATORS = Map.of(
            "naturalness", "native-usage-judge",
            "a1-didactic", "a1-didactic-judge",
            "pragmatism", "pragmatism-judge");
    private final CourseRepository courses;
    private final ObjectMapper json = new ObjectMapper();

    FileSystemPreliminaryOpinionSource(CourseRepository courses) {
        this.courses = Objects.requireNonNull(courses);
    }

    @Override public List<PreliminaryOpinion> load(String sourceRef) {
        if (sourceRef == null || sourceRef.isBlank()) throw new IllegalArgumentException("Falta la fuente de opiniones.");
        Path folder = Path.of(sourceRef).toAbsolutePath().normalize();
        try {
            JsonNode manifest = json.readTree(folder.resolve("photo-manifest.json").toFile());
            var targets = new LinkedHashSet<String>();
            JsonNode declared = manifest.get("targets");
            if (declared == null || !declared.isArray()) throw new IllegalArgumentException("La fuente no identifica sus quizzes.");
            for (JsonNode id : declared) {
                if (!id.isTextual() || id.asText().isBlank() || !targets.add(id.asText()))
                    throw new IllegalArgumentException("La fuente contiene quizzes vacíos o duplicados.");
            }
            if (targets.isEmpty()) throw new IllegalArgumentException("La fuente no contiene quizzes.");
            var photo = courses.load(folder.resolve("course"));
            var snapshots = new LinkedHashMap<String, QuizTemplateEntity>();
            for (var milestone : photo.getRoot().getMilestones()) for (var topic : milestone.getTopics())
                for (var knowledge : topic.getKnowledges()) for (var quiz : knowledge.getQuizTemplates())
                    if (targets.contains(quiz.getId())) snapshots.put(quiz.getId(), quiz);
            if (!snapshots.keySet().equals(targets)) throw new IllegalArgumentException("La foto no contiene todos los quizzes de la fuente.");
            var result = new ArrayList<PreliminaryOpinion>();
            for (String analyzer : List.of("naturalness", "a1-didactic", "pragmatism")) {
                String evaluator = EVALUATORS.get(analyzer);
                JsonNode metrics = json.readTree(folder.resolve(analyzer + "-metrics.json").toFile());
                String version = required(metrics, "judgeVersion");
                JsonNode identity = metrics.get("runtime");
                if (identity == null || !"EXPLICIT".equals(required(identity, "kind")))
                    throw new IllegalArgumentException("La fuente no declara un proveedor efectivo.");
                var runtime = EvaluationRuntimeIdentity.explicit(required(identity, "providerName"),
                        required(identity, "modelName"), required(identity, "reasoningEffort"));
                Path ledger = folder.resolve("trial-workdir/.content-audit/evaluations/exam_" + evaluator + ".jsonl");
                var latest = new LinkedHashMap<String, PreliminaryOpinion>();
                for (String line : Files.readAllLines(ledger, StandardCharsets.UTF_8)) {
                    if (line.isBlank()) continue;
                    JsonNode record = json.readTree(line);
                    String quizId = required(record, "subjectRef");
                    if (!targets.contains(quizId) || !version.equals(required(record, "evaluatorVersion"))) continue;
                    if (!("exam:" + evaluator).equals(required(record, "evaluatorId"))
                            || !"EXPLICIT".equals(required(record, "runtimeKind"))
                            || !runtime.getProviderName().equals(required(record, "providerName"))
                            || !runtime.getModelName().equals(required(record, "modelName"))
                            || !runtime.getReasoningEffort().equals(required(record, "reasoningEffort")))
                        throw new IllegalArgumentException("La procedencia del registro no coincide con su ejecución.");
                    String payload = required(record, "payload");
                    if (!json.readTree(payload).isObject()) throw new IllegalArgumentException("El veredicto no es un objeto JSON.");
                    latest.put(quizId, new PreliminaryOpinion(quizId, analyzer, "exam:" + evaluator, version, runtime,
                            "EXAM", required(record, "contentFingerprint"), payload,
                            Instant.parse(required(record, "recordedAt")), ledger.toString(), sha256(line),
                            new CourseElementSnapshot(AuditTarget.QUIZ, quizId, snapshots.get(quizId), null)));
                }
                if (!latest.keySet().equals(targets)) throw new IllegalArgumentException("Faltan veredictos de " + analyzer + " en la fuente.");
                for (String quizId : targets) result.add(latest.get(quizId));
            }
            return List.copyOf(result);
        } catch (IOException error) {
            throw new IllegalArgumentException("No se pudo leer la fuente de opiniones preliminares: " + error.getMessage(), error);
        }
    }

    private static String required(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank())
            throw new IllegalArgumentException("Falta " + field + " en la procedencia de la fuente.");
        return value.asText();
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }
}
