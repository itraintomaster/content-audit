package com.learney.contentaudit.auditcli.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learney.contentaudit.auditdomain.judging.JudgeRegressionCase;
import com.learney.contentaudit.auditinfrastructure.FileSystemJudgeRegressionCatalog;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.*;

/** Builds immutable exam inputs without starting NLP, discovering models or querying a judge. */
@Command(name = "exam", mixinStandardHelpOptions = true)
final class PermanentExamCmd implements Callable<Integer> {
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules()
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final Path project;
    private final PrintStream output;
    @Parameters(index = "0") String action;
    @Parameters(index = "1", arity = "0..1") String judge;
    @Option(names = "--course") Path course;
    @Option(names = "--photo") Path photo;
    @Option(names = "--cases") Path baseCases;
    @Option(names = "--gold") Path baseGold;
    @Option(names = "--out") Path destination;
    @Option(names = {"-f", "--format"}, defaultValue = "json") String format;

    PermanentExamCmd(Path project, PrintStream output) { this.project = project; this.output = output; }

    static Integer execute(List<String> args, Path project, PrintStream output, PrintStream errors) {
        if (args.size() < 2 || !args.get(0).equals("exam")
                || !Set.of("regressions", "prepare").contains(args.get(1))) return null;
        var cli = new CommandLine(new PermanentExamCmd(project, output));
        cli.setOut(new PrintWriter(output, true)); cli.setErr(new PrintWriter(errors, true));
        cli.setExecutionExceptionHandler((error, command, parsed) -> {
            errors.println("Error: " + error.getMessage()); return 2;
        });
        return cli.execute(args.subList(1, args.size()).toArray(String[]::new));
    }

    /** Prepared exams remember which course owns their reference, even though they run a photo. */
    static Path referenceCourse(List<String> args, Path fallback) {
        if (args.isEmpty()) return fallback;
        if (!args.get(0).equals("exam")) {
            for (int i = 1; i < args.size(); i++) {
                if (args.get(i).startsWith("--course=")) return Path.of(args.get(i).substring(9));
                if (args.get(i).equals("--course") && i + 1 < args.size()) return Path.of(args.get(i + 1));
            }
            if (args.get(0).equals("analyze") && args.size() > 1 && !args.get(1).startsWith("-")) return Path.of(args.get(1));
            return fallback;
        }
        if (args.size() < 2) return fallback;
        Path file = null;
        if (args.get(1).equals("run")) {
            for (int i = 2; i < args.size(); i++) {
                if (args.get(i).startsWith("--cases=")) file = Path.of(args.get(i).substring(8));
                else if (args.get(i).equals("--cases") && i + 1 < args.size()) file = Path.of(args.get(i + 1));
            }
        } else if (args.get(1).equals("record") && args.size() > 2) file = Path.of(args.get(2));
        if (file == null || !Files.isRegularFile(file)) return fallback;
        try {
            JsonNode data = JSON.readTree(file.toFile());
            Set<String> owners = new LinkedHashSet<>();
            if (data.isArray()) {
                for (JsonNode row : data) if (row.hasNonNull("regressionCoursePath")) owners.add(row.path("regressionCoursePath").asText());
            } else if (data.hasNonNull("regressionCoursePath")) owners.add(data.path("regressionCoursePath").asText());
            if (owners.size() > 1) throw new IllegalArgumentException("El examen mezcla referencias de cursos distintos.");
            return owners.isEmpty() ? fallback : Path.of(owners.iterator().next());
        } catch (IOException error) { throw new IllegalArgumentException("No se pudo leer el origen del examen: " + error.getMessage(), error); }
    }

    @Override public Integer call() throws Exception {
        if (course == null) {
            String resolved = CoursePathResolver.resolve(null);
            course = resolved == null ? project.resolve("db/english-course") : Path.of(resolved);
        }
        if (!Files.isDirectory(course)) throw new IllegalArgumentException("No existe la carpeta del curso.");
        if (judge != null && !judge.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,99}"))
            throw new IllegalArgumentException("Nombre de juez inválido.");
        var catalog = new FileSystemJudgeRegressionCatalog(course);
        if (action.equals("regressions")) {
            var judges = new ArrayList<Map<String, Object>>();
            for (String name : judge == null ? catalog.judgeNames() : List.of(judge)) {
                var suite = catalog.suite(name);
                var entry = new LinkedHashMap<String, Object>();
                entry.put("judgeName", name); entry.put("suiteVersion", suite.fingerprint());
                entry.put("cases", suite.cases()); judges.add(entry);
            }
            output.println(CommandJson.writeCompact(Map.of("judges", judges)));
        } else {
            if (judge == null || photo == null || baseCases == null || baseGold == null || destination == null)
                throw new IllegalArgumentException("exam prepare <juez> --course <curso> --photo <foto histórica> "
                        + "--cases <casos históricos> --gold <referencia> --out <carpeta nueva>");
            output.println(CommandJson.writeCompact(prepare(catalog)));
        }
        return 0;
    }

    private Map<String, Object> prepare(FileSystemJudgeRegressionCatalog catalog) throws Exception {
        var suite = catalog.suite(judge);
        JsonNode sourceCases = JSON.readTree(baseCases.toFile());
        JsonNode sourceGold = JSON.readTree(baseGold.toFile());
        if (!sourceCases.isArray() || !sourceGold.isObject() || !sourceGold.path("cases").isArray())
            throw new IllegalArgumentException("El examen histórico necesita una lista de casos y una referencia con cases.");
        if (sourceGold.path("schemaVersion").asInt() != 4)
            throw new IllegalArgumentException("La referencia histórica debe tener schemaVersion 4.");
        // Validate the entire reference before creating an output directory or copying a photo.
        var expectations = new LinkedHashMap<String, ObjectNode>();
        for (JudgeRegressionCase regression : suite.cases()) {
            ObjectNode expectation = examExpectation(regression);
            if (expectation != null) expectations.put(regression.id(), expectation);
        }
        var input = new TreeMap<String, String>();
        input.put("suite", suite.fingerprint());
        input.put("formatVersion", "permanent-exam-v2");
        input.put("cases", digest(Files.readAllBytes(baseCases)));
        input.put("gold", digest(Files.readAllBytes(baseGold)));
        // The photo is content-addressed, too. A change cannot reuse derived files or an earlier result.
        try (var files = Files.walk(photo)) {
            for (Path file : files.filter(Files::isRegularFile).filter(file -> courseFile(photo, file)).sorted().toList())
                input.put("photo/" + photo.relativize(file), digest(Files.readAllBytes(file)));
        }
        String preparationVersion = digest(JSON.writeValueAsBytes(input));
        Path out = destination.toAbsolutePath().normalize();
        Path manifest = out.resolve("preparation.json");
        if (Files.exists(manifest)) {
            JsonNode previous = JSON.readTree(manifest.toFile());
            if (!preparationVersion.equals(previous.path("preparationVersion").asText()))
                throw new IllegalArgumentException("La carpeta pertenece a otra edición del examen. Usá una carpeta nueva.");
            return JSON.convertValue(previous, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        }
        if (Files.exists(out)) {
            try (var entries = Files.list(out)) {
                if (entries.findAny().isPresent()) throw new IllegalArgumentException("La carpeta de preparación ya tiene archivos.");
            }
        }
        Files.createDirectories(out);
        Path frozen = out.resolve("course");
        copyPhoto(photo, frozen);
        ArrayNode cases = JSON.createArrayNode();
        String owner = course.toRealPath().toString();
        for (JsonNode original : sourceCases) {
            ObjectNode entry = original.isObject() ? original.deepCopy() : JSON.createObjectNode().put("ejercicio", original.asText());
            entry.put("regressionCoursePath", owner); entry.put("examFingerprint", preparationVersion); cases.add(entry);
        }
        ObjectNode gold = sourceGold.deepCopy();
        ArrayNode goldCases = (ArrayNode) gold.get("cases");
        var regressionIds = new ArrayList<String>();
        var quizIds = new LinkedHashMap<String, String>();
        var known = new LinkedHashSet<String>();
        for (JsonNode c : cases) known.add(c.isTextual() ? c.asText() : c.path("ejercicio").asText());
        for (JudgeRegressionCase regression : suite.cases()) {
            String executionId = hex(regression.id()).substring(0, 24);
            if (!known.add(executionId)) throw new IllegalArgumentException("Identidad de caso repetida: " + executionId);
            JsonNode snapshot = JSON.valueToTree(regression.snapshot());
            ObjectNode quiz = quizOf(snapshot);
            String level = levelOf(snapshot, regression.quizId());
            appendQuiz(frozen, quiz, snapshot.path("examContext"), level, executionId);
            ObjectNode c = cases.addObject();
            c.put("ejercicio", executionId);
            c.put("regressionCaseId", regression.id());
            c.put("regressionSuiteFingerprint", suite.fingerprint());
            c.put("regressionCoursePath", owner);
            c.put("examFingerprint", preparationVersion);
            origin(c, regression);
            ObjectNode g = goldCases.addObject();
            ObjectNode expectation = expectations.get(regression.id());
            g.put("ejercicio", executionId);
            g.put("categoria", expectation == null ? "REGRESSION" : expectation.path("categoria").asText());
            g.put("nivel", level);
            g.put("quiz", sentence(quiz));
            g.put("miniTheory", quiz.path("miniTheory").asText(""));
            g.put("fundamento", regression.expectedReason());
            g.put("expectedSource", regression.expectedSource());
            g.put("regressionCaseId", regression.id());
            g.put("required", true);
            g.put("verdictOnly", expectation == null);
            g.putObject("revision").put("puntua", true);
            var expected = g.putObject("esperado");
            expected.put("resultado", regression.expectedDetected() ? "FAILS" : "PASSES");
            if (expectation == null) {
                expected.putNull("gravedad"); expected.putArray("reglas");
            } else {
                expected.set("gravedad", expectation.get("gravedad"));
                expected.set("reglas", expectation.get("reglas"));
                g.set("expectedProblem", expectation.get("expectedProblem"));
                g.set("referenceStatus", expectation.get("referenceStatus"));
            }
            origin(g, regression);
            regressionIds.add(regression.id()); quizIds.put(regression.id(), executionId);
        }
        gold.put("expectedCases", cases.size());
        JsonNode expectedConsultations = sourceGold.get("expectedConsultations");
        if (expectedConsultations != null && expectedConsultations.isIntegralNumber() && expectedConsultations.asLong(-1) >= 0)
            gold.put("expectedConsultations", expectedConsultations.asLong() + suite.cases().size());
        int scored = 0;
        for (JsonNode c : goldCases) if (c.path("revision").path("puntua").asBoolean()) scored++;
        gold.put("scoredCases", scored);
        gold.put("frozenCourse", frozen.toString());
        gold.put("regressionSuiteFingerprint", suite.fingerprint());
        gold.put("regressionCoursePath", owner);
        gold.put("examFingerprint", preparationVersion);
        gold.set("regressionCaseIds", JSON.valueToTree(regressionIds));
        gold.set("regressionQuizIds", JSON.valueToTree(quizIds));
        Path casesPath = out.resolve("cases.json"), goldPath = out.resolve("gold.json");
        write(casesPath, cases); write(goldPath, gold);
        var execution = new LinkedHashMap<String, Object>();
        execution.put("type", "exam"); execution.put("mode", "historical");
        execution.put("casesPath", casesPath.toString()); execution.put("goldPath", goldPath.toString());
        execution.put("coursePath", frozen.toString());
        execution.put("regressionSuiteFingerprint", suite.fingerprint());
        var result = new LinkedHashMap<String, Object>();
        result.put("judgeName", judge); result.put("suiteVersion", suite.fingerprint());
        result.put("preparationVersion", preparationVersion); result.put("casesCount", cases.size());
        result.put("regressionCases", suite.cases()); result.put("execution", execution);
        write(manifest, result); // Written last: an incomplete preparation is never reused.
        return result;
    }

    private static ObjectNode examExpectation(JudgeRegressionCase regression) {
        JsonNode snapshot = JSON.valueToTree(regression.snapshot());
        if (!snapshot.has("examExpectation")) return null;
        JsonNode expectation = snapshot.get("examExpectation");
        String prefix = "El caso " + regression.id() + " tiene examExpectation inválida: ";
        if (!(expectation instanceof ObjectNode expected))
            throw new IllegalArgumentException(prefix + "se necesita un objeto.");
        String outcome = regression.expectedDetected() ? "FAILS" : "PASSES";
        if (!expected.path("resultado").isTextual() || !outcome.equals(expected.path("resultado").asText()))
            throw new IllegalArgumentException(prefix + "resultado contradice expectedDetected.");
        JsonNode severity = expected.get("gravedad");
        if (severity == null || !(severity.isNull() || (severity.isTextual()
                && Set.of("LOW", "MEDIUM", "HIGH").contains(severity.asText()))))
            throw new IllegalArgumentException(prefix + "gravedad debe ser LOW, MEDIUM, HIGH o null.");
        JsonNode rules = expected.path("reglas");
        if (!rules.isArray()) throw new IllegalArgumentException(prefix + "reglas debe ser una lista.");
        var seenRules = new HashSet<String>();
        for (JsonNode rule : rules) {
            if (!rule.isTextual() || !rule.asText().matches("[A-Za-z0-9][A-Za-z0-9._-]*")
                    || !seenRules.add(rule.asText()))
                throw new IllegalArgumentException(prefix + "reglas contiene un identificador inválido o repetido.");
        }
        JsonNode problem = expected.get("expectedProblem");
        boolean healthyNull = problem != null && problem.isNull() && !regression.expectedDetected();
        if (problem == null || !(healthyNull || (problem.isTextual() && problem.asText().matches("[A-Z][A-Z0-9_]*"))))
            throw new IllegalArgumentException(prefix + "expectedProblem debe identificar el problema esperado.");
        if (regression.expectedDetected() ? severity.isNull() || rules.isEmpty() || "NONE".equals(problem.asText())
                : !severity.isNull() || !rules.isEmpty() || !(healthyNull || "NONE".equals(problem.asText())))
            throw new IllegalArgumentException(prefix + "gravedad, reglas y expectedProblem contradicen resultado.");
        if (!expected.path("categoria").isTextual() || expected.path("categoria").asText().isBlank())
            throw new IllegalArgumentException(prefix + "categoria debe ser un texto no vacío.");
        if (!"EDITORIAL".equals(expected.path("referenceStatus").asText()))
            throw new IllegalArgumentException(prefix + "referenceStatus debe ser EDITORIAL.");
        if (regression.expectedSource() == null || regression.expectedSource().isBlank())
            throw new IllegalArgumentException(prefix + "se necesita expectedSource explícito.");
        return expected;
    }

    private static void origin(ObjectNode node, JudgeRegressionCase c) {
        node.put("problemId", c.problemId()); node.put("problemTitle", c.problemTitle());
        node.put("problemCaseId", c.problemCaseId()); node.put("sourceQuizId", c.quizId());
    }
    private static boolean courseFile(Path courseRoot, Path file) {
        String name = file.getFileName().toString();
        if (!Set.of("_course.json", "_milestone.json", "_topic.json", "_knowledge.json", "quizzes.json").contains(name)) return false;
        // Campaigns and prepared photos can themselves live under .content-audit. Exclude only
        // metadata inside this course, never an ancestor of the course being copied or hashed.
        for (Path component : courseRoot.relativize(file)) {
            if (component.toString().equals(".content-audit")) return false;
        }
        return true;
    }
    private static void copyPhoto(Path source, Path target) throws IOException {
        if (!Files.isRegularFile(source.resolve("_course.json"))) throw new IllegalArgumentException("La foto no tiene _course.json.");
        try (var files = Files.walk(source)) {
            for (Path file : files.filter(Files::isRegularFile).filter(file -> courseFile(source, file)).toList()) {
                Path dest = target.resolve(source.relativize(file));
                Files.createDirectories(dest.getParent()); Files.copy(file, dest);
            }
        }
    }
    private static ObjectNode quizOf(JsonNode snapshot) {
        JsonNode quiz = snapshot.path("after").path("quiz");
        if (!quiz.isObject()) throw new IllegalArgumentException("Un caso permanente no tiene su after.quiz congelado.");
        return quiz.deepCopy();
    }
    private String levelOf(JsonNode snapshot, String sourceId) throws IOException {
        String level = snapshot.path("examContext").path("level").asText();
        if (level.matches("(?i)A1|A2|B1|B2|C1|C2")) return level.toUpperCase(Locale.ROOT);
        try (var paths = Files.walk(course)) {
            for (Path quizzes : paths.filter(p -> p.getFileName().toString().equals("quizzes.json"))
                    .filter(Files::isRegularFile).filter(file -> courseFile(course, file)).toList()) {
                for (JsonNode q : JSON.readTree(quizzes.toFile())) {
                    if (sourceId.equals(q.path("id").asText()) || sourceId.equals(q.path("_id").path("$oid").asText())) {
                        Path milestone = quizzes.getParent().getParent().getParent().resolve("_milestone.json");
                        String found = JSON.readTree(milestone.toFile()).path("label").asText();
                        if (found.matches("(?i)A1|A2|B1|B2|C1|C2")) return found.toUpperCase(Locale.ROOT);
                    }
                }
            }
        }
        throw new IllegalArgumentException("El caso " + sourceId + " no tiene nivel congelado; completá su contexto antes de incorporarlo.");
    }
    private static void appendQuiz(Path course, ObjectNode quiz, JsonNode context, String level, String quizId) throws Exception {
        Path milestoneDir = course.resolve(level.toLowerCase(Locale.ROOT));
        Path milestonePath = milestoneDir.resolve("_milestone.json");
        ObjectNode root = (ObjectNode) JSON.readTree(course.resolve("_course.json").toFile());
        ObjectNode milestone;
        if (Files.exists(milestonePath)) milestone = (ObjectNode) JSON.readTree(milestonePath.toFile());
        else {
            String id = hex("milestone/" + level).substring(0, 24);
            milestone = node(id, root.path("root").path("_id").path("$oid").asText(), "MILESTONE", level);
            array((ObjectNode) root.get("root"), "children").add(oid(id));
        }
        String topicId = hex("topic/" + quizId).substring(0, 24);
        String knowledgeId = hex("knowledge/" + quizId).substring(0, 24);
        String topicName = context.path("topicName").asText(quiz.path("topicName").asText(""));
        ObjectNode topic = node(topicId, milestone.path("_id").path("$oid").asText(), "TOPIC", topicName);
        topic.putArray("ruleIds").add(oid(knowledgeId));
        ObjectNode knowledge = node(knowledgeId, topicId, "KNOWLEDGE", context.path("title").asText(quiz.path("title").asText("")));
        knowledge.put("isRule", context.path("isRule").asBoolean(true));
        knowledge.put("instructions", context.path("knowledgeInstructions").asText(quiz.path("instructions").asText("")));
        knowledge.put("sentenceMode", context.path("sentenceMode").asText("FILL"));
        quiz.put("id", quizId); quiz.set("_id", oid(quizId)); quiz.set("knowledgeId", oid(knowledgeId));
        quiz.remove(List.of("oidId", "unmodeledFields"));
        ObjectNode form = (ObjectNode) quiz.get("form");
        if (form == null) throw new IllegalArgumentException("El caso " + quizId + " no tiene form.");
        if (form.get("multipleChoice") instanceof ObjectNode mc) {
            if (mc.has("selection")) form.set("selection", mc.get("selection"));
            if (mc.has("items")) form.set("items", mc.get("items"));
            form.remove("multipleChoice");
        }
        if (quiz.has("sentences")) { form.set("sentences", quiz.get("sentences")); quiz.remove("sentences"); }
        array(milestone, "children").add(oid(topicId)); array(root, "knowledgeIds").add(oid(knowledgeId));
        Path topicDir = milestoneDir.resolve("regression-" + quizId);
        Path knowledgeDir = topicDir.resolve("exercise");
        write(course.resolve("_course.json"), root); write(milestonePath, milestone);
        write(topicDir.resolve("_topic.json"), topic); write(knowledgeDir.resolve("_knowledge.json"), knowledge);
        ArrayNode quizzes = JSON.createArrayNode(); quizzes.add(quiz); write(knowledgeDir.resolve("quizzes.json"), quizzes);
    }
    private static ObjectNode node(String id, String parent, String kind, String label) {
        ObjectNode node = JSON.createObjectNode(); node.set("_id", oid(id)); node.set("parentId", oid(parent));
        node.put("kind", kind); node.put("label", label); node.put("code", id); node.put("oldId", id);
        node.putNull("children"); return node;
    }
    private static ObjectNode oid(String id) { return JSON.createObjectNode().put("$oid", id); }
    private static ArrayNode array(ObjectNode node, String key) {
        return node.get(key) instanceof ArrayNode a ? a : node.putArray(key);
    }
    private static String sentence(JsonNode quiz) {
        if (quiz.path("sentences").isArray() && !quiz.path("sentences").isEmpty()) return quiz.path("sentences").get(0).asText();
        StringBuilder text = new StringBuilder();
        for (JsonNode part : quiz.path("form").path("sentenceParts"))
            text.append(part.path("kind").asText().equals("CLOZE") ? " ___ " : part.path("text").asText());
        return text.toString().replaceAll("\\s+", " ").trim();
    }
    private static void write(Path file, Object value) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(value) + "\n");
    }
    private static String hex(String value) throws Exception { return digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)).substring(7); }
    private static String digest(byte[] value) throws Exception { return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value)); }
}
