package com.learney.contentaudit.auditcli.commands;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.*;
import com.learney.contentaudit.auditinfrastructure.FileSystemProblemStore;
import com.learney.contentaudit.courseinfrastructure.CourseValidatorImpl;
import com.learney.contentaudit.courseinfrastructure.FileSystemCourseRepository;
import com.learney.contentaudit.revisiondomain.problems.ContentProblem;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PermanentExamCmdTest {
    @TempDir Path root;
    private final ObjectMapper json = new ObjectMapper();
    private static final String QUIZ = "111111111111111111111115";
    private static final String ROOT = "111111111111111111111111";
    private static final String MILESTONE = "111111111111111111111112";
    private static final String TOPIC = "111111111111111111111113";
    private static final String KNOWLEDGE = "111111111111111111111114";
    private Path source() throws Exception {
        Path source = root.resolve("source");
        var rootNode = new LinkedHashMap<>(node(ROOT, ROOT, "ROOT", "English", Map.of("children", List.of(oid(MILESTONE)))));
        rootNode.put("parentId", null);
        write(source.resolve("_course.json"), Map.of("_id", oid(ROOT), "title", "English", "knowledgeIds", List.of(oid(KNOWLEDGE)),
                "root", rootNode));
        write(source.resolve("a1/_milestone.json"), node(MILESTONE, ROOT, "MILESTONE", "A1", Map.of("children", List.of(oid(TOPIC)))));
        write(source.resolve("a1/topic/_topic.json"), node(TOPIC, MILESTONE, "TOPIC", "Present Simple", Map.of("ruleIds", List.of(oid(KNOWLEDGE)))));
        write(source.resolve("a1/topic/knowledge/_knowledge.json"), node(KNOWLEDGE, TOPIC, "KNOWLEDGE", "Be", Map.of("instructions", "Choose be", "isRule", true, "sentenceMode", "FILL")));
        var quiz = quiz(); quiz.put("_id", oid(QUIZ)); quiz.put("knowledgeId", oid(KNOWLEDGE));
        write(source.resolve("a1/topic/knowledge/quizzes.json"), List.of(quiz));
        write(root.resolve("cases.json"), List.of(Map.of("ejercicio", QUIZ)));
        write(root.resolve("gold.json"), Map.of("schemaVersion", 4, "expectedCases", 1, "scoredCases", 1,
                "cases", List.of(Map.of("ejercicio", QUIZ, "categoria", "SANO", "revision", Map.of("puntua", true),
                        "esperado", Map.of("resultado", "PASSES", "reglas", List.of())))));
        return source;
    }
    private Map<String, Object> quiz() {
        var q = new LinkedHashMap<String, Object>();
        q.put("id", QUIZ); q.put("kind", "CLOZE"); q.put("title", "Be"); q.put("topicName", "Present Simple");
        q.put("instructions", "Choose be"); q.put("miniTheory", "is va con she");
        q.put("form", Map.of("kind", "CLOZE", "incidence", 1.0, "sentenceParts", List.of(
                Map.of("kind", "TEXT", "text", "She "), Map.of("kind", "CLOZE", "text", "", "options", List.of("is")),
                Map.of("kind", "TEXT", "text", " English."))));
        return q;
    }
    private void confirm(Path source) {
        var snapshot = Map.<String, Object>of("after", Map.of("quiz", quiz()));
        var c = new ContentProblem.Case("CASE-1", QUIZ, "batch", root.toString(), 1, 1,
                ContentProblem.Use.EXAM, "", "SUBJECT_FIRST_EXPECTATION", snapshot, Instant.EPOCH);
        var p = new ContentProblem("PRB-confirmed", "PROBLEM_TITLE_SHOULD_NOT_ENTER_PROMPT", "SUBJECT_FIRST_EXPECTATION",
                List.of(), List.of("miniTheory"), ContentProblem.Status.WORKING, 0, List.of(c), List.of(), Map.of("judgeName", "mini-theory"), Instant.EPOCH, Instant.EPOCH);
        new FileSystemProblemStore(source).save(p, -1);
    }
    private record Result(int code, String out, String err) {}
    private Result run(String... args) {
        var out = new ByteArrayOutputStream(); var err = new ByteArrayOutputStream();
        int code = PermanentExamCmd.execute(List.of(args), root, new PrintStream(out), new PrintStream(err));
        return new Result(code, out.toString(), err.toString());
    }
    @Test void appendsFrozenProblemCaseAndKeepsOracleAndProblemMetadataOutsideJudgePhoto() throws Exception {
        Path source = source(); confirm(source); Path dest = root.resolve("prepared");
        Result r = prepare(source, dest); assertEquals(0, r.code(), r.err());
        JsonNode manifest = json.readTree(r.out());
        JsonNode cases = json.readTree(dest.resolve("cases.json").toFile());
        assertEquals(2, cases.size());
        assertEquals("PRB-confirmed", cases.get(1).path("problemId").asText());
        assertFalse(cases.get(1).has("esperado"));
        assertFalse(cases.get(1).has("expectedDetected"));
        assertNotEquals(QUIZ, cases.get(1).path("ejercicio").asText(), "A frozen variant gets its own execution id even when the baseline contains that quiz.");
        JsonNode gold = json.readTree(dest.resolve("gold.json").toFile());
        assertFalse(gold.has("expectedConsultations"), "An absent historical consultation estimate must not be invented.");
        assertTrue(gold.path("cases").get(1).path("required").asBoolean());
        assertTrue(gold.path("cases").get(1).path("verdictOnly").asBoolean());
        assertEquals("FAILS", gold.path("cases").get(1).path("esperado").path("resultado").asText());
        try (var files = Files.walk(dest.resolve("course"))) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String body = Files.readString(file);
                assertFalse(body.contains("SUBJECT_FIRST_EXPECTATION"), file.toString());
                assertFalse(body.contains("PROBLEM_TITLE_SHOULD_NOT_ENTER_PROMPT"), file.toString());
                assertFalse(body.contains("PRB-confirmed"), file.toString());
            }
        }
        assertDoesNotThrow(() -> new FileSystemCourseRepository(new CourseValidatorImpl()).load(dest.resolve("course")),
                "The prepared photo must use exactly the normal course reader.");
        long modified = Files.getLastModifiedTime(dest.resolve("cases.json")).toMillis();
        Result same = prepare(source, dest); assertEquals(0, same.code(), same.err());
        assertEquals(manifest, json.readTree(same.out()));
        assertEquals(modified, Files.getLastModifiedTime(dest.resolve("cases.json")).toMillis());
    }
    @Test void preparesACourseWhoseParentIsNamedContentAuditAndExcludesItsOwnRuntimeMetadata() throws Exception {
        Path ordinary = source();
        Path nested = root.resolve(".content-audit/campaigns/frozen/course");
        Files.createDirectories(nested.getParent()); Files.move(ordinary, nested); confirm(nested);
        write(nested.resolve(".content-audit/runtime/quizzes.json"), Map.of("runtimeMetadata", true));
        Path dest = root.resolve(".content-audit/prepared-exams/mini-theory");
        Result r = prepare(nested, dest);
        assertEquals(0, r.code(), r.err());
        assertTrue(Files.isRegularFile(dest.resolve("course/_course.json")));
        assertFalse(Files.exists(dest.resolve("course/.content-audit")));
        assertEquals(2, json.readTree(dest.resolve("cases.json").toFile()).size());
        assertDoesNotThrow(() -> new FileSystemCourseRepository(new CourseValidatorImpl()).load(dest.resolve("course")));
        Result repeat = prepare(nested, dest); assertEquals(0, repeat.code(), repeat.err());
        assertEquals(json.readTree(r.out()), json.readTree(repeat.out()));
        write(nested.resolve(".content-audit/runtime/quizzes.json"), Map.of("runtimeMetadata", "changed"));
        assertEquals(0, prepare(nested, dest).code(), "Runtime metadata does not change the exam edition.");
        Path historicalQuizzes = nested.resolve("a1/topic/knowledge/quizzes.json");
        Files.writeString(historicalQuizzes, Files.readString(historicalQuizzes) + " ");
        Result changedPhoto = prepare(nested, dest);
        assertEquals(2, changedPhoto.code()); assertTrue(changedPhoto.err().contains("otra edición"), changedPhoto.err());
    }
    @Test void readingEmptyCatalogDoesNotNeedAConfiguredModel() throws Exception {
        Path source = source();
        Result r = run("exam", "regressions", "mini-theory", "--course", source.toString());
        assertEquals(0, r.code(), r.err());
        JsonNode suite = json.readTree(r.out()).path("judges").get(0);
        assertEquals(0, suite.path("cases").size()); assertTrue(suite.path("suiteVersion").isNull());
    }
    @Test void addsEachIsolatedRegressionSubjectToDeclaredHistoricalConsultations() throws Exception {
        Path source = source();
        var gold = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(root.resolve("gold.json").toFile());
        gold.put("expectedConsultations", 1); write(root.resolve("gold.json"), gold);
        catalogReference(source, true, expectation("FAILS", "LOW", List.of("missing-english-highlight"), "MISSING_ENGLISH_HIGHLIGHT"));
        Path catalogPath = source.resolve(".content-audit/judge-regressions/catalog.json");
        var catalog = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(catalogPath.toFile());
        var references = (com.fasterxml.jackson.databind.node.ArrayNode) catalog.get("cases");
        for (int i = 2; i <= 3; i++) {
            var copy = (com.fasterxml.jackson.databind.node.ObjectNode) references.get(0).deepCopy();
            copy.put("id", "REG-exact-" + i); copy.put("problemCaseId", "CASE-exact-" + i); references.add(copy);
        }
        write(catalogPath, catalog);
        Path dest = root.resolve("prepared-consultations");
        Result result = prepare(source, dest);
        assertEquals(0, result.code(), result.err());
        JsonNode preparedGold = json.readTree(dest.resolve("gold.json").toFile());
        assertEquals(4, preparedGold.path("expectedConsultations").asInt());
        assertEquals(4, preparedGold.path("expectedCases").asInt());
    }
    @Test void refusesReusingDerivedFilesForChangedHistoricalInput() throws Exception {
        Path source = source(); confirm(source); Path dest = root.resolve("prepared");
        assertEquals(0, prepare(source, dest).code());
        Files.writeString(root.resolve("gold.json"), Files.readString(root.resolve("gold.json")) + " ");
        Result r = prepare(source, dest);
        assertEquals(2, r.code()); assertTrue(r.err().contains("otra edición"), r.err());
    }
    @Test void preparedExamUsesItsOwningCourseWhenRunningOrRecording() throws Exception {
        Path source = source(); confirm(source); Path dest = root.resolve("prepared");
        assertEquals(0, prepare(source, dest).code());
        assertEquals(source.toRealPath(), PermanentExamCmd.referenceCourse(List.of("exam", "run", "mini-theory",
                "--cases=" + dest.resolve("cases.json")), root));
        Path measured = root.resolve("measured.json"); write(measured, Map.of("regressionCoursePath", source.toRealPath().toString()));
        assertEquals(source.toRealPath(), PermanentExamCmd.referenceCourse(List.of("exam", "record", measured.toString()), root));
        assertEquals(source, PermanentExamCmd.referenceCourse(List.of("analyze", source.toString()), root));
        assertEquals(source, PermanentExamCmd.referenceCourse(List.of("batch", "close", "--course=" + source), root));
    }
    @Test void preservesExactLowSeverityEditorialReferenceWithoutLeakingItToJudgeInputs() throws Exception {
        assertExactReference(true, "LOW", List.of("missing-english-highlight"), "MISSING_ENGLISH_HIGHLIGHT");
    }
    @Test void preservesExactHighSeverityEditorialReferenceWithoutLeakingItToJudgeInputs() throws Exception {
        assertExactReference(true, "HIGH", List.of("false-rule"), "FALSE_RULE");
    }
    @Test void preservesExactHealthyEditorialReferenceWithoutInventingSeverityOrRules() throws Exception {
        assertExactReference(false, null, List.of(), "NONE");
    }
    @Test void preservesExplicitNullProblemInHealthyCatalogReference() throws Exception {
        assertExactReference(false, null, List.of(), null);
    }
    private void assertExactReference(boolean detected, String severity, List<String> rules, String problem) throws Exception {
        Path source = source();
        var expectation = expectation(detected ? "FAILS" : "PASSES", severity, rules, problem);
        catalogReference(source, detected, expectation);
        Path dest = root.resolve("prepared-exact");
        Result result = prepare(source, dest);
        assertEquals(0, result.code(), result.err());
        JsonNode actual = json.readTree(dest.resolve("gold.json").toFile()).path("cases").get(1);
        assertFalse(actual.path("verdictOnly").asBoolean(true));
        assertEquals(json.valueToTree(expectation.get("resultado")), actual.path("esperado").path("resultado"));
        assertEquals(json.valueToTree(severity), actual.path("esperado").path("gravedad"));
        assertEquals(json.valueToTree(rules), actual.path("esperado").path("reglas"));
        assertEquals(json.valueToTree(problem), actual.path("expectedProblem"));
        assertEquals("ORACLE_CATEGORY", actual.path("categoria").asText());
        assertEquals("EDITORIAL", actual.path("referenceStatus").asText());
        assertEquals("Referencia editorial del asistente; no certificación humana", actual.path("expectedSource").asText());
        assertTrue(actual.path("required").asBoolean());
        assertTrue(actual.path("revision").path("puntua").asBoolean());
        List<Path> inputs = new ArrayList<>();
        inputs.add(dest.resolve("cases.json"));
        try (var files = Files.walk(dest.resolve("course"))) { inputs.addAll(files.filter(Files::isRegularFile).toList()); }
        for (Path input : inputs) {
            String content = Files.readString(input);
            for (String oracle : List.of("examExpectation", "expectedProblem", "referenceStatus", "expectedSource", "ORACLE_CATEGORY", "ORACLE_REASON"))
                assertFalse(content.contains(oracle), input + " must not contain " + oracle);
            for (String rule : rules) assertFalse(content.contains(rule), input + " must not contain expected rules");
        }
        assertDoesNotThrow(() -> new FileSystemCourseRepository(new CourseValidatorImpl()).load(dest.resolve("course")));
    }
    @Test void rejectsContradictoryOrMalformedExactReferencesBeforeMaterializingAnything() throws Exception {
        Path source = source();
        var valid = expectation("FAILS", "LOW", List.of("missing-english-highlight"), "MISSING_ENGLISH_HIGHLIGHT");
        var invalid = new ArrayList<Map<String, Object>>();
        for (var badField : List.of(Map.entry("resultado", "PASSES"), Map.entry("gravedad", "URGENT"),
                Map.entry("reglas", List.of("")), Map.entry("reglas", List.of(7)),
                Map.entry("reglas", List.of("false-rule", "false-rule")), Map.entry("reglas", "false-rule"),
                Map.entry("expectedProblem", "NONE"), Map.entry("categoria", " "), Map.entry("referenceStatus", "HUMAN"))) {
            var malformed = new LinkedHashMap<String, Object>(valid);
            malformed.put(badField.getKey(), badField.getValue()); invalid.add(malformed);
        }
        var nullSeverity = new LinkedHashMap<String, Object>(valid); nullSeverity.put("gravedad", null); invalid.add(nullSeverity);
        var missingSeverity = new LinkedHashMap<String, Object>(valid); missingSeverity.remove("gravedad"); invalid.add(missingSeverity);
        var noRules = new LinkedHashMap<String, Object>(valid); noRules.put("reglas", List.of()); invalid.add(noRules);
        var nullProblem = new LinkedHashMap<String, Object>(valid); nullProblem.put("expectedProblem", null); invalid.add(nullProblem);
        for (int i = 0; i < invalid.size(); i++) {
            catalogReference(source, true, invalid.get(i));
            Path dest = root.resolve("invalid-" + i);
            Result result = prepare(source, dest);
            assertEquals(2, result.code(), "Invalid expectation " + invalid.get(i));
            assertTrue(result.err().contains("examExpectation inválida"), result.err());
            assertFalse(Files.exists(dest), "An invalid reference must not leave a partially prepared exam");
        }
    }
    @Test void rejectsHealthyReferenceThatContainsAFindingBeforeMaterializingAnything() throws Exception {
        Path source = source();
        catalogReference(source, false, expectation("PASSES", "LOW", List.of("missing-english-highlight"), "MISSING_ENGLISH_HIGHLIGHT"));
        Path dest = root.resolve("invalid-healthy");
        Result result = prepare(source, dest);
        assertEquals(2, result.code());
        assertTrue(result.err().contains("contradicen resultado"), result.err());
        assertFalse(Files.exists(dest));
    }
    @Test void rejectsAbsentOrBlankHealthyProblemBeforeMaterializingAnything() throws Exception {
        Path source = source();
        var absent = expectation("PASSES", null, List.of(), null); absent.remove("expectedProblem");
        var blank = expectation("PASSES", null, List.of(), " ");
        int index = 0;
        for (var invalid : List.of(absent, blank)) {
            catalogReference(source, false, invalid);
            Path dest = root.resolve("invalid-healthy-problem-" + index++);
            Result result = prepare(source, dest);
            assertEquals(2, result.code());
            assertTrue(result.err().contains("expectedProblem"), result.err());
            assertFalse(Files.exists(dest));
        }
    }
    private Map<String, Object> expectation(String outcome, String severity, Object rules, String problem) {
        var expectation = new LinkedHashMap<String, Object>();
        expectation.put("resultado", outcome); expectation.put("gravedad", severity); expectation.put("reglas", rules);
        expectation.put("expectedProblem", problem); expectation.put("categoria", "ORACLE_CATEGORY");
        expectation.put("referenceStatus", "EDITORIAL");
        return expectation;
    }
    private void catalogReference(Path source, boolean detected, Map<String, Object> expectation) throws IOException {
        var reference = new LinkedHashMap<String, Object>();
        reference.put("id", "REG-exact"); reference.put("judgeName", "mini-theory"); reference.put("problemId", "PRB-exact");
        reference.put("problemTitle", "Highlighted English"); reference.put("problemCaseId", "CASE-exact");
        reference.put("quizId", QUIZ); reference.put("expectedDetected", detected); reference.put("expectedReason", "ORACLE_REASON");
        reference.put("expectedSource", "Referencia editorial del asistente; no certificación humana");
        reference.put("confirmedAt", Instant.EPOCH.toString());
        reference.put("snapshot", Map.of("after", Map.of("quiz", quiz()), "examContext", Map.of("level", "A1"),
                "examExpectation", expectation));
        write(source.resolve(".content-audit/judge-regressions/catalog.json"), Map.of("cases", List.of(reference)));
    }
    private Result prepare(Path source, Path dest) {
        return run("exam", "prepare", "mini-theory", "--course", source.toString(), "--photo", source.toString(),
                "--cases", root.resolve("cases.json").toString(), "--gold", root.resolve("gold.json").toString(), "--out", dest.toString());
    }
    private void write(Path path, Object value) throws IOException {
        Files.createDirectories(path.getParent()); Files.writeString(path, json.writeValueAsString(value));
    }
    private static Map<String, String> oid(String id) { return Map.of("$oid", id); }
    private static Map<String, Object> node(String id, String parent, String kind, String label, Map<String, Object> rest) {
        var node = new LinkedHashMap<String, Object>(); node.put("_id", oid(id)); node.put("parentId", oid(parent));
        node.put("kind", kind); node.put("label", label); node.put("code", id); node.put("oldId", id); node.putAll(rest); return node;
    }
}
