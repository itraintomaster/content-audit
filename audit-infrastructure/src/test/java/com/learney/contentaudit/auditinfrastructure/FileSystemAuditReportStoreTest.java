package com.learney.contentaudit.auditinfrastructure;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditReportSummary;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableMilestone;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.AuditableTopic;
import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.auditdomain.DefaultKnowledgeDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultQuizDiagnoses;
import com.learney.contentaudit.auditdomain.NlpToken;
import com.learney.contentaudit.auditdomain.SentenceLengthDiagnosis;
import com.learney.contentaudit.auditdomain.labs.LemmaPlacementDiagnosis;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip persistence test for {@link FileSystemAuditReportStore}.
 * Traceability: FEAT-STORE / F-STORE-R001
 */
class FileSystemAuditReportStoreTest {

    @Test
    @DisplayName("should save an AuditReport and load it back with identical content")
    @Tag("FEAT-STORE")
    void shouldSaveAndLoadWithIdenticalContent(@TempDir Path tempDir) {
        // Build a realistic AuditReport tree:
        // COURSE (entity=null) → MILESTONE → TOPIC → KNOWLEDGE → QUIZ

        // --- Quiz node (leaf) ---
        AuditableQuiz quiz = new AuditableQuiz(
                "The cat sat on the mat.",
                List.of(new NlpToken("cat", "cat", "NOUN", 500, false, false)),
                "quiz-1",
                "Quiz One",
                "Q1"
        );

        DefaultQuizDiagnoses quizDiagnoses = new DefaultQuizDiagnoses();
        SentenceLengthDiagnosis sentenceDiagnosis = new SentenceLengthDiagnosis(7, 5, 10, CefrLevel.A1, 0, 2);
        quizDiagnoses.setSentenceLengthDiagnosis(sentenceDiagnosis);

        LemmaPlacementDiagnosis quizLemmaDiagnosis = new LemmaPlacementDiagnosis(0, List.of());
        quizDiagnoses.setLemmaAbsenceDiagnosis(quizLemmaDiagnosis);

        Map<String, Double> quizScores = new HashMap<>();
        quizScores.put("sentence-length", 1.0);

        // Note: metadata uses only String values to ensure round-trip equality.
        // Map<String,Object> with complex types would deserialize as LinkedHashMap,
        // which would not equal the original value.
        Map<String, Object> quizMetadata = new HashMap<>();
        quizMetadata.put("tokenCount", "7");

        AuditNode quizNode = new AuditNode(quiz, AuditTarget.QUIZ, null, List.of(), quizScores, quizMetadata, quizDiagnoses);

        // --- Knowledge node ---
        AuditableKnowledge knowledge = new AuditableKnowledge(
                List.of(quiz),
                "Prepositions",
                "Complete the sentence with the correct preposition.",
                true,
                "know-1",
                "Knowledge One",
                "K1"
        );

        DefaultKnowledgeDiagnoses knowledgeDiagnoses = new DefaultKnowledgeDiagnoses();
        LemmaPlacementDiagnosis knowledgeLemmaDiagnosis = new LemmaPlacementDiagnosis(1, List.of());
        knowledgeDiagnoses.setLemmaAbsenceDiagnosis(knowledgeLemmaDiagnosis);

        Map<String, Double> knowledgeScores = new HashMap<>();
        knowledgeScores.put("sentence-length", 0.9);
        knowledgeScores.put("knowledge-title-length", 1.0);

        Map<String, Object> knowledgeMetadata = new HashMap<>();
        knowledgeMetadata.put("quizCount", "1");

        AuditNode knowledgeNode = new AuditNode(knowledge, AuditTarget.KNOWLEDGE, null,
                new ArrayList<>(List.of(quizNode)), knowledgeScores, knowledgeMetadata, knowledgeDiagnoses);
        quizNode.setParent(knowledgeNode);

        // --- Topic node ---
        AuditableTopic topic = new AuditableTopic(
                List.of(knowledge),
                "topic-1",
                "Topic One",
                "T1"
        );

        Map<String, Double> topicScores = new HashMap<>();
        topicScores.put("sentence-length", 0.85);

        AuditNode topicNode = new AuditNode(topic, AuditTarget.TOPIC, null,
                new ArrayList<>(List.of(knowledgeNode)), topicScores, new HashMap<>(), null);
        knowledgeNode.setParent(topicNode);

        // --- Milestone node ---
        AuditableMilestone milestone = new AuditableMilestone(
                List.of(topic),
                "ms-1",
                "Milestone One",
                "M1"
        );

        Map<String, Double> milestoneScores = new HashMap<>();
        milestoneScores.put("sentence-length", 0.8);

        AuditNode milestoneNode = new AuditNode(milestone, AuditTarget.MILESTONE, null,
                new ArrayList<>(List.of(topicNode)), milestoneScores, new HashMap<>(), null);
        topicNode.setParent(milestoneNode);

        // --- Course root node ---
        // The root (COURSE) node has entity=null per IAuditEngine convention —
        // AuditableCourse does not implement AuditableEntity.
        Map<String, Double> courseScores = new HashMap<>();
        courseScores.put("sentence-length", 0.8);

        AuditNode rootNode = new AuditNode(null, AuditTarget.COURSE, null,
                new ArrayList<>(List.of(milestoneNode)), courseScores, new HashMap<>(), null);
        milestoneNode.setParent(rootNode);

        AuditReport original = new AuditReport(rootNode);

        // --- Save and load ---
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(tempDir);
        String id = store.save(original);

        assertNotNull(id, "save() should return a non-null ID");
        assertFalse(id.isBlank(), "save() should return a non-blank ID");

        Optional<AuditReport> loadedOpt = store.load(id);
        assertTrue(loadedOpt.isPresent(), "load() should find the saved report");

        AuditReport loaded = loadedOpt.get();
        assertNotSame(original, loaded, "loaded report should be a different object instance");

        // --- Structural equality ---
        AuditNode loadedRoot = loaded.getRoot();
        assertNotNull(loadedRoot, "loaded root should not be null");
        assertNull(loadedRoot.getParent(), "root node parent should be null after rebuild");
        assertEquals(AuditTarget.COURSE, loadedRoot.getTarget());
        assertEquals(courseScores, loadedRoot.getScores());
        assertNull(loadedRoot.getEntity(), "root entity should be null (AuditableCourse is not AuditableEntity)");

        // Milestone child
        assertEquals(1, loadedRoot.getChildren().size());
        AuditNode loadedMilestone = loadedRoot.getChildren().get(0);
        assertEquals(AuditTarget.MILESTONE, loadedMilestone.getTarget());
        assertNotNull(loadedMilestone.getParent(), "milestone parent should be rebuilt");
        assertEquals(AuditTarget.COURSE, loadedMilestone.getParent().getTarget());
        assertEquals(milestoneScores, loadedMilestone.getScores());
        assertTrue(loadedMilestone.getEntity() instanceof AuditableMilestone,
                "milestone entity should be AuditableMilestone");
        AuditableMilestone loadedMilestoneEntity = (AuditableMilestone) loadedMilestone.getEntity();
        assertEquals("ms-1", loadedMilestoneEntity.getId());
        assertEquals("Milestone One", loadedMilestoneEntity.getLabel());
        assertEquals("M1", loadedMilestoneEntity.getCode());

        // Topic child
        assertEquals(1, loadedMilestone.getChildren().size());
        AuditNode loadedTopic = loadedMilestone.getChildren().get(0);
        assertEquals(AuditTarget.TOPIC, loadedTopic.getTarget());
        assertNotNull(loadedTopic.getParent());
        assertEquals(AuditTarget.MILESTONE, loadedTopic.getParent().getTarget());
        assertTrue(loadedTopic.getEntity() instanceof AuditableTopic);
        AuditableTopic loadedTopicEntity = (AuditableTopic) loadedTopic.getEntity();
        assertEquals("topic-1", loadedTopicEntity.getId());

        // Knowledge child
        assertEquals(1, loadedTopic.getChildren().size());
        AuditNode loadedKnowledge = loadedTopic.getChildren().get(0);
        assertEquals(AuditTarget.KNOWLEDGE, loadedKnowledge.getTarget());
        assertNotNull(loadedKnowledge.getParent());
        assertEquals(AuditTarget.TOPIC, loadedKnowledge.getParent().getTarget());
        assertEquals(knowledgeScores, loadedKnowledge.getScores());
        assertTrue(loadedKnowledge.getEntity() instanceof AuditableKnowledge);

        // AuditableKnowledge entity round-trip — including isSentence field
        AuditableKnowledge loadedKnowledgeEntity = (AuditableKnowledge) loadedKnowledge.getEntity();
        assertEquals("know-1", loadedKnowledgeEntity.getId());
        assertEquals("Prepositions", loadedKnowledgeEntity.getTitle());
        assertTrue(loadedKnowledgeEntity.isIsSentence(), "isSentence should round-trip as true");

        // Knowledge diagnoses round-trip
        assertNotNull(loadedKnowledge.getDiagnoses());
        assertTrue(loadedKnowledge.getDiagnoses() instanceof DefaultKnowledgeDiagnoses);
        DefaultKnowledgeDiagnoses loadedKD = (DefaultKnowledgeDiagnoses) loadedKnowledge.getDiagnoses();
        assertTrue(loadedKD.getLemmaAbsenceDiagnosis().isPresent());
        assertEquals(1, loadedKD.getLemmaAbsenceDiagnosis().get().getMisplacedLemmaCount());

        // Quiz leaf
        assertEquals(1, loadedKnowledge.getChildren().size());
        AuditNode loadedQuiz = loadedKnowledge.getChildren().get(0);
        assertEquals(AuditTarget.QUIZ, loadedQuiz.getTarget());
        assertNotNull(loadedQuiz.getParent());
        assertEquals(AuditTarget.KNOWLEDGE, loadedQuiz.getParent().getTarget());
        assertEquals(0, loadedQuiz.getChildren().size());
        assertEquals(quizScores, loadedQuiz.getScores());
        assertTrue(loadedQuiz.getEntity() instanceof AuditableQuiz);

        // AuditableQuiz entity round-trip
        AuditableQuiz loadedQuizEntity = (AuditableQuiz) loadedQuiz.getEntity();
        assertEquals("quiz-1", loadedQuizEntity.getId());
        assertEquals("Quiz One", loadedQuizEntity.getLabel());
        assertEquals("Q1", loadedQuizEntity.getCode());
        assertEquals("The cat sat on the mat.", loadedQuizEntity.getSentence());
        assertNotNull(loadedQuizEntity.getTokens());
        assertEquals(1, loadedQuizEntity.getTokens().size());

        // NlpToken round-trip
        NlpToken loadedToken = loadedQuizEntity.getTokens().get(0);
        assertEquals("cat", loadedToken.getText());
        assertEquals("cat", loadedToken.getLemma());
        assertEquals("NOUN", loadedToken.getPosTag());
        assertEquals(500, loadedToken.getFrequencyRank());
        assertFalse(loadedToken.isIsStop(), "isStop should round-trip as false");
        assertFalse(loadedToken.isIsPunct(), "isPunct should round-trip as false");

        // Quiz diagnoses round-trip
        assertNotNull(loadedQuiz.getDiagnoses());
        assertTrue(loadedQuiz.getDiagnoses() instanceof DefaultQuizDiagnoses);
        DefaultQuizDiagnoses loadedQD = (DefaultQuizDiagnoses) loadedQuiz.getDiagnoses();

        assertTrue(loadedQD.getSentenceLengthDiagnosis().isPresent());
        SentenceLengthDiagnosis loadedSLD = loadedQD.getSentenceLengthDiagnosis().get();
        assertEquals(7, loadedSLD.getTokenCount());
        assertEquals(5, loadedSLD.getTargetMin());
        assertEquals(10, loadedSLD.getTargetMax());
        assertEquals(CefrLevel.A1, loadedSLD.getCefrLevel());
        assertEquals(0, loadedSLD.getDelta());
        assertEquals(2, loadedSLD.getToleranceMargin());

        assertTrue(loadedQD.getLemmaAbsenceDiagnosis().isPresent());
        assertEquals(0, loadedQD.getLemmaAbsenceDiagnosis().get().getMisplacedLemmaCount());

        // Metadata round-trip (string values)
        assertEquals("7", loadedQuiz.getMetadata().get("tokenCount"));
        assertEquals("1", loadedKnowledge.getMetadata().get("quizCount"));
    }

    @Test
    @DisplayName("should return empty Optional when loading a non-existent ID")
    void shouldReturnEmptyForMissingId(@TempDir Path tempDir) {
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(tempDir);
        Optional<AuditReport> result = store.load("nonexistent-id");
        assertFalse(result.isPresent(), "load() should return empty for a missing ID");
    }

    @Test
    @DisplayName("should return empty Optional when loadLatest is called on an empty store")
    void shouldReturnEmptyForLatestWhenStoreIsEmpty(@TempDir Path tempDir) {
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(tempDir);
        Optional<AuditReport> result = store.loadLatest();
        assertFalse(result.isPresent(), "loadLatest() should return empty when no reports exist");
    }

    @Test
    @DisplayName("should return an empty list when listing an empty store")
    void shouldReturnEmptyListWhenNoReportsExist(@TempDir Path tempDir) {
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(tempDir);
        List<AuditReportSummary> summaries = store.list();
        assertNotNull(summaries);
        assertTrue(summaries.isEmpty(), "list() should return empty list when no reports exist");
    }

    @Test
    @DisplayName("should list saved reports with correct summary information")
    void shouldListSavedReportsWithSummaries(@TempDir Path tempDir) {
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(tempDir);

        Map<String, Double> scores = Map.of("sentence-length", 0.75);
        // Root node entity is null (COURSE level)
        AuditNode root = new AuditNode(null, AuditTarget.COURSE, null, List.of(), scores, Map.of(), null);
        AuditReport report = new AuditReport(root);

        String id = store.save(report);

        List<AuditReportSummary> summaries = store.list();
        assertEquals(1, summaries.size());
        assertEquals(id, summaries.get(0).getId());
        assertEquals(0.75, summaries.get(0).getOverallScore(), 1e-9);
    }

    @Test
    @DisplayName("should load the most recent report via loadLatest")
    void shouldLoadLatestReport(@TempDir Path tempDir) throws InterruptedException {
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(tempDir);

        AuditNode root1 = new AuditNode(null, AuditTarget.COURSE, null, List.of(),
                Map.of("sentence-length", 0.5), Map.of(), null);
        store.save(new AuditReport(root1));

        // Ensure second timestamp is different (filenames have second precision)
        Thread.sleep(1001);

        AuditNode root2 = new AuditNode(null, AuditTarget.COURSE, null, List.of(),
                Map.of("sentence-length", 0.9), Map.of(), null);
        store.save(new AuditReport(root2));

        Optional<AuditReport> latest = store.loadLatest();
        assertTrue(latest.isPresent());
        assertEquals(0.9, latest.get().getRoot().getScores().get("sentence-length"), 1e-9);
    }

}
