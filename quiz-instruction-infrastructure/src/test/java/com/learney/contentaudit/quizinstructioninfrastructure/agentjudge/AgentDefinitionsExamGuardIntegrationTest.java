package com.learney.contentaudit.quizinstructioninfrastructure.agentjudge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The exam guard. A judge takes a fixed exam, and its practice is measured on `escribir/`, before it judges the course:
 * an exercise of that exam that its definition already shows, with its verdict, measures nothing. So no agent
 * definition under {@code .sentinel/agents/} -- guide, nodes, eval fixtures, scripts -- may name an exam exercise by
 * its id or by «…xxxx», nor carry its statement, finished sentence, translation, options or mini-theory, nor even
 * five English words in a row that, in the whole course, only exam exercises have. The mini-theory judge takes whole
 * topics: its own folder may not show any exercise of a topic of its exam either.
 *
 * <p>The list of what is guarded is {@code agentguard/examen.json}, ids only, made by
 * {@code scripts/armar_guardia_del_examen.py} from the exam of brief 077. The texts come from the course in
 * {@code db/english-course}, the 29/9 base the exam was drawn from. No model is ever consulted.
 */
class AgentDefinitionsExamGuardIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern FULL_ID = Pattern.compile("[0-9a-f]{24}");
    private static final Pattern SHORT_ID = Pattern.compile("(?:…|\\.\\.\\.)\\s?([0-9a-f]{4})\\b");
    private static final Pattern SPACE_BEFORE_APOSTROPHE = Pattern.compile("\\s+'");
    private static final Pattern SPACES = Pattern.compile("\\s+");
    private static final Pattern WORD = Pattern.compile("[a-z0-9']+");
    private static final int GRAM = 5;
    private static final int MIN_TEXT = 14;
    private static final int MIN_MINI_THEORY = 25;
    private static final String MINI_THEORY_JUDGE = "mini-theory-judge";

    private static Path root;
    private static Guarded guarded;

    @BeforeAll
    static void load() throws IOException {
        root = projectRoot();
        guarded = Guarded.of(readExam(), Course.read(root.resolve("db").resolve("english-course")));
    }

    @Test
    @DisplayName("should keep every exam exercise, and every topic of the mini-theory exam, out of every agent definition")
    void shouldKeepTheExamOutOfEveryAgentDefinition() throws IOException {
        List<String> violations = scan(root.resolve(".sentinel").resolve("agents"), guarded);

        assertTrue(violations.isEmpty(), "Una definición de agente trae ejercicios del examen; reemplazalos por "
                + "ejercicios fuera del examen (los que no cuentan: escribir/ y el examen fijo del 077):\n"
                + String.join("\n", violations));
    }

    @Test
    @DisplayName("should catch an exam exercise named by its id, by «…xxxx», by its text or by five of its words")
    void shouldCatchAnExamExerciseHoweverItIsNamed(@TempDir Path agents) throws IOException {
        // An exam exercise whose «…xxxx» names only itself and that has a run of words only exam exercises have.
        String examId = guarded.examIds.stream()
                .filter(id -> id.equals(guarded.examIdBySuffix.get(id.substring(20))))
                .filter(id -> guarded.examGrams.containsValue(id))
                .findFirst().orElseThrow();
        String fragment = guarded.examGrams.entrySet().stream().filter(e -> e.getValue().equals(examId))
                .map(Map.Entry::getKey).findFirst().orElseThrow();
        Course.Quiz quiz = guarded.course.quizzes.get(examId);
        Path folder = Files.createDirectories(agents.resolve("some-judge").resolve("nodes"));
        Files.writeString(folder.resolve("a.md"), "Ver " + examId + ".", StandardCharsets.UTF_8);
        Files.writeString(folder.resolve("b.md"), "Como el de las verduras (…" + examId.substring(20) + ").",
                StandardCharsets.UTF_8);
        String longest = quiz.texts().stream().filter(t -> normalized(t, true).length() >= MIN_TEXT)
                .findFirst().orElseThrow();
        Files.writeString(folder.resolve("c.md"), "Ejemplo: «" + longest.toUpperCase(Locale.ROOT) + "»",
                StandardCharsets.UTF_8);
        Files.writeString(folder.resolve("d.md"), "Una parte, sin el resto: «… " + fragment + " …»",
                StandardCharsets.UTF_8);

        List<String> violations = scan(agents, guarded);

        assertEquals(Set.of("some-judge/nodes/a.md", "some-judge/nodes/b.md", "some-judge/nodes/c.md",
                        "some-judge/nodes/d.md"),
                violations.stream().map(v -> v.split(":")[0]).collect(Collectors.toSet()),
                "each way of naming the exercise is caught: " + violations);
        assertTrue(violations.stream().filter(v -> !v.startsWith("some-judge/nodes/c.md"))
                .allMatch(v -> v.contains(examId)), violations.toString());
        assertTrue(violations.stream().anyMatch(v -> v.startsWith("some-judge/nodes/d.md") && v.contains(fragment)),
                "five words in a row that only exam exercises have are caught on their own: " + violations);
    }

    @Test
    @DisplayName("should keep out of the mini-theory judge any exercise of a topic of its exam, and only out of it")
    void shouldKeepTheTopicsOfItsExamOutOfTheMiniTheoryJudgeOnly(@TempDir Path agents) throws IOException {
        Course.Quiz notAnExamCase = guarded.course.quizzes.values().stream()
                .filter(q -> guarded.miniTheoryTopics.contains(q.topicId()) && !guarded.examIds.contains(q.id()))
                .filter(q -> q.texts().stream().anyMatch(t -> normalized(t, true).length() >= MIN_TEXT))
                .findFirst().orElseThrow();
        String text = notAnExamCase.texts().stream().filter(t -> normalized(t, true).length() >= MIN_TEXT)
                .findFirst().orElseThrow();
        for (String judge : List.of(MINI_THEORY_JUDGE, "translation-sense-from-english")) {
            Path nodes = Files.createDirectories(agents.resolve(judge).resolve("nodes"));
            Files.writeString(nodes.resolve("evaluate.md"), "Ejemplo: «" + text + "»", StandardCharsets.UTF_8);
        }

        List<String> violations = scan(agents, guarded);

        assertFalse(violations.isEmpty(), "the mini-theory judge cannot show an exercise of its exam topics");
        assertTrue(violations.stream().allMatch(v -> v.startsWith(MINI_THEORY_JUDGE + "/")),
                "another judge may show it, if it is not an exam case itself: " + violations);
    }

    @Test
    @DisplayName("should find in the course every exercise and topic it guards")
    void shouldFindInTheCourseEverythingItGuards() {
        assertFalse(guarded.examIds.isEmpty(), "agentguard/examen.json lists no exam case");
        Set<String> missing = new TreeSet<>(guarded.examIds);
        missing.removeAll(guarded.course.quizzes.keySet());
        assertTrue(missing.isEmpty(), "exam cases that are not in db/english-course: " + missing);
        Set<String> topics = guarded.course.quizzes.values().stream().map(Course.Quiz::topicId)
                .collect(Collectors.toSet());
        Set<String> missingTopics = new TreeSet<>(guarded.miniTheoryTopics);
        missingTopics.removeAll(topics);
        assertTrue(missingTopics.isEmpty(), "mini-theory exam topics that are not in the course: " + missingTopics);
    }

    // -------------------------------------------------------------------------------------------------------------

    /** Every violation under an agents folder, as «agent/relative/path: what and which exercise». */
    static List<String> scan(Path agentsDir, Guarded g) throws IOException {
        List<String> violations = new ArrayList<>();
        if (!Files.isDirectory(agentsDir)) {
            return violations;
        }
        List<Path> agents;
        try (Stream<Path> s = Files.list(agentsDir)) {
            agents = s.filter(Files::isDirectory).sorted().toList();
        }
        for (Path agent : agents) {
            boolean miniTheory = agent.getFileName().toString().equals(MINI_THEORY_JUDGE);
            List<Path> files;
            try (Stream<Path> s = Files.walk(agent)) {
                files = s.filter(Files::isRegularFile).sorted().toList();
            }
            for (Path file : files) {
                String where = agentsDir.relativize(file).toString();
                String raw = readLenient(file);
                Matcher full = FULL_ID.matcher(raw);
                while (full.find()) {
                    String id = full.group();
                    if (g.examIds.contains(id)) {
                        violations.add(where + ": id del examen " + id);
                    } else if (g.miniTheoryTopics.contains(id)) {
                        violations.add(where + ": tema del examen de miniteoría " + id);
                    } else if (miniTheory && g.miniTheoryTopicQuizIds.contains(id)) {
                        violations.add(where + ": ejercicio de un tema del examen de miniteoría " + id);
                    }
                }
                Matcher shortId = SHORT_ID.matcher(raw);
                while (shortId.find()) {
                    String exam = g.examIdBySuffix.get(shortId.group(1));
                    String topicQuiz = miniTheory ? g.miniTheoryQuizIdBySuffix.get(shortId.group(1)) : null;
                    if (exam != null) {
                        violations.add(where + ": «…" + shortId.group(1) + "» nombra al ejercicio del examen " + exam);
                    } else if (topicQuiz != null) {
                        violations.add(where + ": «…" + shortId.group(1)
                                + "» nombra un ejercicio de un tema del examen de miniteoría " + topicQuiz);
                    }
                }
                String text = normalized(raw, false);
                g.examTexts.forEach((t, id) -> {
                    if (text.contains(t)) {
                        violations.add(where + ": texto del ejercicio del examen " + id + ": «" + t + "»");
                    }
                });
                g.miniTheories.forEach((t, id) -> {
                    if (text.contains(t)) {
                        violations.add(where + ": miniteoría del ejercicio " + id + ": «" + t + "»");
                    }
                });
                Map<String, String> exclusive = miniTheory ? g.examOrTopicGrams : g.examGrams;
                Set<String> seenGrams = new TreeSet<>();
                for (String gram : grams(text)) {
                    String id = exclusive.get(gram);
                    if (id != null && seenGrams.add(gram)) {
                        violations.add(where + ": «" + gram + "» sólo aparece en el curso en ejercicios del examen ("
                                + id + ")");
                    }
                }
                if (miniTheory) {
                    g.miniTheoryTopicTexts.forEach((t, id) -> {
                        if (text.contains(t)) {
                            violations.add(where + ": texto del ejercicio " + id
                                    + ", de un tema del examen de miniteoría: «" + t + "»");
                        }
                    });
                }
            }
        }
        return violations;
    }

    /** Lower case, straight quotes, «I 'm» as «I'm», one space between words; trimmed of « .?!» when {@code ends}. */
    static String normalized(String s, boolean ends) {
        String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFKC)
                .replace('’', '\'').replace('‘', '\'').replace('“', '"').replace('”', '"')
                .toLowerCase(Locale.ROOT);
        n = SPACE_BEFORE_APOSTROPHE.matcher(n).replaceAll("'");
        n = SPACES.matcher(n).replaceAll(" ").strip();
        if (ends) {
            int from = 0;
            int to = n.length();
            while (from < to && " .?!".indexOf(n.charAt(from)) >= 0) {
                from++;
            }
            while (to > from && " .?!".indexOf(n.charAt(to - 1)) >= 0) {
                to--;
            }
            n = n.substring(from, to);
        }
        return n;
    }

    /** Every run of {@value #GRAM} words of an already normalized text. */
    static Set<String> grams(String normalizedText) {
        List<String> words = new ArrayList<>();
        Matcher m = WORD.matcher(normalizedText);
        while (m.find()) {
            words.add(m.group());
        }
        Set<String> grams = new HashSet<>();
        for (int i = 0; i + GRAM <= words.size(); i++) {
            grams.add(String.join(" ", words.subList(i, i + GRAM)));
        }
        return grams;
    }

    private static String readLenient(Path file) throws IOException {
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .decode(java.nio.ByteBuffer.wrap(Files.readAllBytes(file))).toString();
    }

    private static JsonNode readExam() throws IOException {
        try (InputStream in = AgentDefinitionsExamGuardIntegrationTest.class
                .getResourceAsStream("/agentguard/examen.json")) {
            if (in == null) {
                throw new IllegalStateException("agentguard/examen.json is missing from the test resources");
            }
            return JSON.readTree(in);
        }
    }

    /** The module runs from its own folder; the project root is the first parent with the agents and the course. */
    private static Path projectRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            if (Files.isDirectory(dir.resolve(".sentinel").resolve("agents"))
                    && Files.isDirectory(dir.resolve("db").resolve("english-course"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("no project root with .sentinel/agents and db/english-course above "
                + Path.of("").toAbsolutePath());
    }

    /** What may not appear, with the texts of each guarded exercise. */
    record Guarded(Course course, Set<String> examIds, Set<String> miniTheoryTopics,
            Set<String> miniTheoryTopicQuizIds, Map<String, String> examIdBySuffix,
            Map<String, String> miniTheoryQuizIdBySuffix, Map<String, String> examTexts,
            Map<String, String> miniTheoryTopicTexts, Map<String, String> miniTheories,
            Map<String, String> examGrams, Map<String, String> examOrTopicGrams) {

        static Guarded of(JsonNode exam, Course course) {
            Set<String> examIds = new TreeSet<>();
            exam.path("casos").forEach(n -> examIds.add(n.asText()));
            Set<String> topics = new TreeSet<>();
            exam.path("temasDeMiniteoria").forEach(n -> topics.add(n.asText()));
            Set<String> topicQuizIds = new TreeSet<>();
            for (Course.Quiz q : course.quizzes.values()) {
                if (topics.contains(q.topicId())) {
                    topicQuizIds.add(q.id());
                }
            }
            Map<String, String> examBySuffix = new HashMap<>();
            examIds.forEach(id -> examBySuffix.putIfAbsent(id.substring(20), id));
            Map<String, String> topicBySuffix = new HashMap<>();
            topicQuizIds.forEach(id -> topicBySuffix.putIfAbsent(id.substring(20), id));
            Map<String, String> examTexts = texts(course, examIds);
            Map<String, String> topicTexts = texts(course, topicQuizIds);
            Map<String, String> miniTheories = new LinkedHashMap<>();
            Set<String> withMiniTheory = new HashSet<>(examIds);
            withMiniTheory.addAll(topicQuizIds);
            for (String id : new TreeSet<>(withMiniTheory)) {
                Course.Quiz q = course.quizzes.get(id);
                if (q != null) {
                    String m = normalized(q.miniTheory(), false);
                    if (m.length() >= MIN_MINI_THEORY) {
                        miniTheories.putIfAbsent(m, id);
                    }
                }
            }
            Set<String> examOrTopic = new HashSet<>(examIds);
            examOrTopic.addAll(topicQuizIds);
            return new Guarded(course, examIds, topics, topicQuizIds, examBySuffix, topicBySuffix, examTexts,
                    topicTexts, miniTheories, exclusiveGrams(course, examIds), exclusiveGrams(course, examOrTopic));
        }

        /** The English runs of words that, in the whole course, only these exercises have, with one of them. */
        private static Map<String, String> exclusiveGrams(Course course, Set<String> ids) {
            Map<String, String> owner = new HashMap<>();
            Set<String> shared = new HashSet<>();
            for (Course.Quiz q : course.quizzes.values()) {
                boolean guardedQuiz = ids.contains(q.id());
                for (String english : q.english()) {
                    for (String gram : grams(normalized(english, false))) {
                        if (!guardedQuiz) {
                            shared.add(gram);
                        } else {
                            owner.putIfAbsent(gram, q.id());
                        }
                    }
                }
            }
            owner.keySet().removeAll(shared);
            return owner;
        }

        private static Map<String, String> texts(Course course, Set<String> ids) {
            Map<String, String> texts = new LinkedHashMap<>();
            for (String id : ids) {
                Course.Quiz q = course.quizzes.get(id);
                if (q == null) {
                    continue;
                }
                for (String t : q.texts()) {
                    String n = normalized(t, true);
                    if (n.length() >= MIN_TEXT) {
                        texts.putIfAbsent(n, id);
                    }
                }
            }
            return texts;
        }
    }

    /** The course of {@code db/english-course}: every quiz with what a definition could copy from it. */
    static final class Course {

        final Map<String, Quiz> quizzes = new LinkedHashMap<>();

        /** {@code texts}: everything a definition could copy; {@code english}: the English part of it. */
        record Quiz(String id, String topicId, String miniTheory, List<String> texts, List<String> english) {
        }

        static Course read(Path courseDir) throws IOException {
            Course course = new Course();
            List<Path> files;
            try (Stream<Path> s = Files.walk(courseDir)) {
                files = s.filter(p -> p.getFileName().toString().equals("quizzes.json")).sorted().toList();
            }
            for (Path file : files) {
                for (JsonNode q : JSON.readTree(file.toFile())) {
                    String id = q.path("id").asText(null);
                    if (id == null) {
                        continue;
                    }
                    JsonNode knowledge = q.path("knowledgeId");
                    String topic = knowledge.isObject() ? knowledge.path("$oid").asText(null) : knowledge.asText(null);
                    List<String> english = new ArrayList<>();
                    q.path("form").path("sentences").forEach(n -> english.add(n.asText()));
                    List<String> statement = new ArrayList<>();
                    for (JsonNode part : q.path("form").path("sentenceParts")) {
                        boolean isText = "TEXT".equals(part.path("kind").asText());
                        String partText = part.path("text").isTextual() ? part.path("text").asText() : null;
                        statement.add(isText && partText != null && !partText.isEmpty() ? partText : "___");
                        if (!isText) {
                            part.path("options").forEach(o -> english.add(o.asText()));
                        }
                    }
                    english.add(String.join(" ", statement));
                    List<String> texts = new ArrayList<>(english);
                    texts.add(q.path("translation").asText(""));
                    course.quizzes.put(id, new Quiz(id, topic, q.path("miniTheory").asText(""), List.copyOf(texts),
                            List.copyOf(english)));
                }
            }
            return course;
        }
    }
}
