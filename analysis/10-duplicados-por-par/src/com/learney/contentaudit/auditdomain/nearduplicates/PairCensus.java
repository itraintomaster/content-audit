package com.learney.contentaudit.auditdomain.nearduplicates;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learney.contentaudit.auditapplication.CourseToAuditableMapper;
import com.learney.contentaudit.auditdomain.AuditableCourse;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableMilestone;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.AuditableTopic;
import com.learney.contentaudit.auditdomain.CorrectorMatcher;
import com.learney.contentaudit.auditdomain.NlpToken;
import com.learney.contentaudit.auditdomain.NlpTokenizer;
import com.learney.contentaudit.auditdomain.QuizStudentViewRenderer;
import com.learney.contentaudit.auditdomain.correctormatch.DefaultCorrectorMatcher;
import com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer;
import com.learney.contentaudit.courseinfrastructure.CourseValidatorImpl;
import com.learney.contentaudit.courseinfrastructure.FileSystemCourseRepository;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.vocabularyinfrastructure.correctortable.FileSystemFormEquivalenceTableSource;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Census of the candidate pairs of near-duplicates, without asking any judge: every pair of the
 * same topic that asks the same (R001, production rules), whether the wildcard settles it, its
 * word similarity and the cosine of nomic-embed-text («clustering: » + finished exercise), as
 * SentenceEmbeddingEvaluator asks it. Writes ids and numbers only, never a text of the course.
 */
public class PairCensus {

    public static void main(String[] args) throws Exception {
        Path course = Paths.get(args[0]);
        Path shared = Paths.get(args[1]);
        Path out = Paths.get(args[2]);
        Path vectorCache = Paths.get(args[3]);

        CourseEntity entity = new FileSystemCourseRepository(new CourseValidatorImpl()).load(course);
        DefaultQuizSentenceConverter converter = DefaultQuizSentenceConverter.create();
        AuditableCourse auditable = new CourseToAuditableMapper(new NoTokens(), converter).map(entity);
        QuizStudentViewRenderer renderer = new DefaultQuizStudentViewRenderer(converter);
        CorrectorMatcher matcher = new DefaultCorrectorMatcher(
                new FileSystemFormEquivalenceTableSource(shared.resolve("corrector/formas-equivalentes.json")).load());
        DefaultNearDuplicateRules rules = new DefaultNearDuplicateRules(matcher, renderer);
        DefaultNearDuplicatesConfig config = new DefaultNearDuplicatesConfig();

        record Member(String level, String knowledgeId, int index, AuditableQuiz quiz, String finished) {}
        record Pair(Member kept, Member copy, String wildcard, double wordSimilarity) {}

        List<Pair> pairs = new ArrayList<>();
        Map<String, Integer> quizzesByLevel = new HashMap<>();
        Map<String, Integer> topicsByLevel = new HashMap<>();
        Map<String, Integer> sameTopicPairsByLevel = new HashMap<>();
        Map<Member, Boolean> needsVector = new IdentityHashMap<>();
        for (AuditableMilestone milestone : auditable.getMilestones()) {
            String level = milestone.getCode() != null ? milestone.getCode() : milestone.getLabel();
            for (AuditableTopic topic : milestone.getTopics()) {
                for (AuditableKnowledge knowledge : topic.getKnowledge()) {
                    List<Member> members = new ArrayList<>();
                    for (AuditableQuiz quiz : knowledge.getQuizzes()) {
                        String finished = DefaultNearDuplicateRules.finishedExercise(renderer, quiz);
                        if (finished == null || finished.isBlank()) {
                            continue;
                        }
                        members.add(new Member(level, knowledge.getId(), members.size(), quiz, finished));
                    }
                    quizzesByLevel.merge(level, members.size(), Integer::sum);
                    topicsByLevel.merge(level, 1, Integer::sum);
                    sameTopicPairsByLevel.merge(level, members.size() * (members.size() - 1) / 2, Integer::sum);
                    for (int j = 1; j < members.size(); j++) {
                        for (int i = 0; i < j; i++) {
                            Member kept = members.get(i);
                            Member copy = members.get(j);
                            if (!rules.sameRequest(kept.quiz(), copy.quiz())) {
                                continue;
                            }
                            String wildcard = rules.wildcardDifference(kept.quiz(), copy.quiz()).orElse(null);
                            pairs.add(new Pair(kept, copy, wildcard, rules.wordSimilarity(kept.quiz(), copy.quiz())));
                            if (wildcard == null) {
                                needsVector.put(kept, true);
                                needsVector.put(copy, true);
                            }
                        }
                    }
                }
            }
        }
        System.err.println("pairs asking the same: " + pairs.size() + "; members needing a vector: "
                + needsVector.size());

        // Vectors, cached by text so a second run asks nothing.
        ObjectMapper json = new ObjectMapper();
        Map<String, float[]> cache = new HashMap<>();
        if (Files.exists(vectorCache)) {
            JsonNode stored = json.readTree(vectorCache.toFile());
            stored.fields().forEachRemaining(entry -> {
                float[] vector = new float[entry.getValue().size()];
                for (int k = 0; k < vector.length; k++) {
                    vector[k] = (float) entry.getValue().get(k).asDouble();
                }
                cache.put(entry.getKey(), vector);
            });
        }
        HttpClient http = HttpClient.newHttpClient();
        int asked = 0;
        for (Member member : needsVector.keySet()) {
            String input = config.inputPrefix() + member.finished();
            if (cache.containsKey(input)) {
                continue;
            }
            ObjectNode body = json.createObjectNode();
            body.put("model", "nomic-embed-text");
            body.put("input", input);
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:11434/api/embed"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
            JsonNode answer = json.readTree(http.send(request, HttpResponse.BodyHandlers.ofString()).body());
            JsonNode values = answer.path("embeddings").path(0);
            float[] vector = new float[values.size()];
            for (int k = 0; k < vector.length; k++) {
                vector[k] = (float) values.get(k).asDouble();
            }
            cache.put(input, vector);
            asked++;
        }
        System.err.println("vectors asked now: " + asked + "; cached: " + cache.size());
        ObjectNode stored = json.createObjectNode();
        for (Map.Entry<String, float[]> entry : cache.entrySet()) {
            var array = stored.putArray(entry.getKey());
            for (float value : entry.getValue()) {
                array.add(value);
            }
        }
        json.writeValue(vectorCache.toFile(), stored);

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(out))) {
            writer.println("level\tknowledgeId\tkeptIndex\tcopyIndex\tkeptId\tcopyId\twildcard\twordSimilarity\tcosine");
            for (Pair pair : pairs) {
                double cosine = Double.NaN;
                if (pair.wildcard() == null) {
                    cosine = SentenceEmbeddingEvaluator.cosine(
                            cache.get(config.inputPrefix() + pair.kept().finished()),
                            cache.get(config.inputPrefix() + pair.copy().finished()));
                }
                writer.printf("%s\t%s\t%d\t%d\t%s\t%s\t%d\t%.6f\t%.6f%n", pair.kept().level(),
                        pair.kept().knowledgeId(), pair.kept().index(), pair.copy().index(), pair.kept().quiz().getId(),
                        pair.copy().quiz().getId(), pair.wildcard() != null ? 1 : 0, pair.wordSimilarity(), cosine);
            }
        }
        System.err.println("quizzes by level: " + quizzesByLevel);
        System.err.println("topics by level: " + topicsByLevel);
        System.err.println("same-topic pairs by level: " + sameTopicPairsByLevel);
    }

    /** near-duplicates reads no token: the mapper gets none. */
    static final class NoTokens implements NlpTokenizer {

        @Override
        public List<String> tokenize(String text) {
            return List.of();
        }

        @Override
        public int countTokens(String text) {
            return 0;
        }

        @Override
        public List<NlpToken> analyzeTokens(String text) {
            return List.of();
        }

        @Override
        public Map<String, List<NlpToken>> analyzeTokensBatch(List<String> sentences) {
            return Map.of();
        }
    }
}
