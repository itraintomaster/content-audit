package com.learney.contentaudit.auditinfrastructure;

import com.learney.contentaudit.auditdomain.catalog.AnalyzerMeasure;
import com.learney.contentaudit.auditdomain.contextnumbers.AuditDigest;
import com.learney.contentaudit.auditdomain.contextnumbers.DigestNode;
import com.learney.contentaudit.auditdomain.contextnumbers.ErrorCounts;
import com.learney.contentaudit.auditdomain.contextnumbers.SeverityCounts;
import com.learney.contentaudit.auditdomain.finding.AnalysisCost;
import com.learney.contentaudit.auditdomain.finding.EvidencePart;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingEvidence;
import com.learney.contentaudit.auditdomain.finding.FindingIdentity;
import com.learney.contentaudit.auditdomain.finding.FindingNodeRef;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import java.nio.file.Files;
import java.util.LinkedHashMap;

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
import com.learney.contentaudit.auditdomain.DefaultCourseDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultQuizDiagnoses;
import com.learney.contentaudit.auditdomain.NlpToken;
import com.learney.contentaudit.auditdomain.SentenceLengthDiagnosis;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerFamily;
import com.learney.contentaudit.auditdomain.contextnumbers.AnalyzerScore;
import com.learney.contentaudit.auditdomain.contextnumbers.ContextNumbers;
import com.learney.contentaudit.auditdomain.labs.LemmaPlacementDiagnosis;
import com.learney.contentaudit.auditdomain.judging.JudgeCostEstimate;
import com.learney.contentaudit.auditdomain.judging.JudgeCoverageDiagnosis;
import com.learney.contentaudit.auditdomain.judging.JudgeExamStanding;
import com.learney.contentaudit.auditdomain.judging.JudgeExamStatus;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;

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
    void shouldSaveAnAuditReportAndLoadItBackWithIdenticalContent(@TempDir Path tempDir) {
        // Build a realistic AuditReport tree:
        // COURSE (entity=null) → MILESTONE → TOPIC → KNOWLEDGE → QUIZ

        // --- Quiz node (leaf) ---
        AuditableQuiz quiz = new AuditableQuiz(List.of(new NlpToken("cat", "cat", "NOUN", 500, false, false)), "quiz-1", "Quiz One", "Q1", null, List.of("The cat sat on the mat."), "The cat sat on the mat.", null, null, null, null, null);

        DefaultQuizDiagnoses quizDiagnoses = new DefaultQuizDiagnoses();
        SentenceLengthDiagnosis sentenceDiagnosis = new SentenceLengthDiagnosis(7, 5, 10, CefrLevel.A1, 0, 2);
        quizDiagnoses.setSentenceLengthDiagnosis(sentenceDiagnosis);

        LemmaPlacementDiagnosis quizLemmaDiagnosis = new LemmaPlacementDiagnosis(0, List.of(), 0, List.of());
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
        , null, null);

        DefaultKnowledgeDiagnoses knowledgeDiagnoses = new DefaultKnowledgeDiagnoses();
        LemmaPlacementDiagnosis knowledgeLemmaDiagnosis = new LemmaPlacementDiagnosis(1, List.of(), 0, List.of());
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
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(tempDir, null);
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
        assertEquals(List.of("The cat sat on the mat."), loadedQuizEntity.getSentences());
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
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(tempDir, null);
        Optional<AuditReport> result = store.load("nonexistent-id");
        assertFalse(result.isPresent(), "load() should return empty for a missing ID");
    }

    @Test
    @DisplayName("should return empty Optional when loadLatest is called on an empty store")
    void shouldReturnEmptyForLatestWhenStoreIsEmpty(@TempDir Path tempDir) {
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(tempDir, null);
        Optional<AuditReport> result = store.loadLatest();
        assertFalse(result.isPresent(), "loadLatest() should return empty when no reports exist");
    }

    @Test
    @DisplayName("should return an empty list when listing an empty store")
    void shouldReturnEmptyListWhenNoReportsExist(@TempDir Path tempDir) {
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(tempDir, null);
        List<AuditReportSummary> summaries = store.list();
        assertNotNull(summaries);
        assertTrue(summaries.isEmpty(), "list() should return empty list when no reports exist");
    }

    @Test
    @DisplayName("should list saved reports with correct summary information")
    void shouldListSavedReportsWithSummaries(@TempDir Path tempDir) {
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(tempDir, null);

        Map<String, Double> scores = Map.of("sentence-length", 0.75);
        // Root node entity is null (COURSE level)
        AuditNode root = new AuditNode(null, AuditTarget.COURSE, null, List.of(), scores, Map.of(), null);
        // FEAT-HALL (F-HALL-R010): the summary shows the vocabulary score the engine published
        // on the course, as every report the engine produces carries it.
        root.setNumbers(new ContextNumbers(0.75,
                List.of(new AnalyzerScore("sentence-length", AnalyzerFamily.VOCABULARY, 0.75, List.of())), null, null));
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
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(tempDir, null);

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

    @Test
    @DisplayName("should list each saved analysis with the vocabulary score published on its course, 73,9 % for the 29/9 base, instead of 73,4 %, the average of its eleven course keys")
    @Tag("FEAT-HALL")
    @Tag("F-HALL-R010")
    public void shouldListEachSavedAnalysisWithTheVocabularyScorePublishedOnItsCourse739ForThe299BaseInsteadOf734TheAverageOfItsElevenCourseKeys(
            @TempDir Path tempDir) {
        // R010: the listing shows the number the engine published on the course -- 73,9 % on the
        // 29/9 base -- and not the 73,4 % of averaging its eleven course keys, quarters included.
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(tempDir, null);
        Map<String, Double> courseKeys = new LinkedHashMap<>();
        courseKeys.put("coca-buckets-distribution", 0.7253788624322802);
        courseKeys.put("lemma-recurrence", 0.08);
        courseKeys.put("lemma-absence", 0.89250300362255);
        courseKeys.put("lemma-count", 0.6698448144961168);
        courseKeys.put("coca-buckets-distribution/Q1", 0.7673328547283127);
        courseKeys.put("coca-buckets-distribution/Q2", 0.7684543932356559);
        courseKeys.put("coca-buckets-distribution/Q3", 0.6915373831741857);
        courseKeys.put("coca-buckets-distribution/Q4", 0.6741908185909666);
        courseKeys.put("knowledge-title-length", 0.9877878929349517);
        courseKeys.put("knowledge-instructions-length", 0.9546429877621389);
        courseKeys.put("sentence-length", 0.8644799618316434);
        AuditNode root = new AuditNode(null, AuditTarget.COURSE, null, List.of(), courseKeys, Map.of(), null);
        root.setNumbers(new ContextNumbers(0.7392339318685259, List.of(), null, null));
        String id = store.save(new AuditReport(root));

        List<AuditReportSummary> summaries = store.list();

        assertEquals(1, summaries.size(), "the digest, in its own directory, is not taken for another analysis");
        assertEquals(id, summaries.get(0).getId());
        assertEquals(0.7392339318685259, summaries.get(0).getOverallScore(), 1e-12,
                "R010: 73,9 %, the vocabulary score published on the course");
        double elevenKeys = courseKeys.values().stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        assertEquals(0.734, elevenKeys, 5e-4, "the old number averaged the eleven keys");
    }

    @Test
    @DisplayName("should save with each analysis a digest without the course entities whose numbers, findings and unevaluated analyzers are copied from the report and not recomputed, and load it by the analysis id")
    @Tag("FEAT-HALL")
    @Tag("F-HALL-R010")
    public void shouldSaveWithEachAnalysisADigestWithoutTheCourseEntitiesWhoseNumbersFindingsAndUnevaluatedAnalyzersAreCopiedFromTheReportAndNotRecomputedAndLoadItByTheAnalysisId(
            @TempDir Path tempDir) throws Exception {
        // R010: whoever draws reads the digest, not the 146 MB report: ids, labels, numbers,
        // findings and what was left unevaluated -- copied, never recomputed -- and no entity.
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(tempDir, null);
        AuditableQuiz quiz = new AuditableQuiz(List.of(new NlpToken("wallet", "wallet", "NOUN", 9000, false, false)),
                "67fab6d599301022953425b1", "Where is my wallet?", "Q1", "¿Dónde está mi billetera?",
                List.of("Where is my wallet?"), "Where is my [wallet]?", "Completa", List.of(), null, null, null);
        AuditNode quizNode = new AuditNode(quiz, AuditTarget.QUIZ, null, List.of(), Map.of("lemma-absence", 0.9),
                Map.of(), null);
        Finding wallet = new Finding("lemma-absence", "misplaced-word", new FindingNodeRef(AuditTarget.QUIZ,
                "67fab6d599301022953425b1", "Where is my wallet?"), FindingSeverity.MEDIUM,
                new FindingEvidence(List.of(new EvidencePart("Palabra", "wallet")), "wallet es de A2", List.of()),
                FindingResolution.PANEL, AnalysisCost.INSTANT,
                new FindingIdentity("lemma-absence|misplaced-word|67fab6d599301022953425b1|wallet", "wallet"));
        quizNode.setFindings(List.of(wallet));
        quizNode.setUnevaluatedBy(List.of("quiz-instruction"));
        // Deliberately not what the scores would give: the digest copies, it never recomputes.
        ContextNumbers published = new ContextNumbers(0.123, List.of(), new ErrorCounts(1, 0, 0.0,
                new SeverityCounts(0, 0, 0, 0), 1, List.of()), null);
        quizNode.setNumbers(published);
        AuditNode root = new AuditNode(null, AuditTarget.COURSE, null, List.of(quizNode), Map.of("lemma-absence", 0.9),
                Map.of(), null);
        root.setNumbers(new ContextNumbers(0.456, List.of(), null, null));
        quizNode.setParent(root);

        String id = store.save(new AuditReport(root));
        AuditDigest digest = store.loadDigest(id).orElseThrow(() -> new AssertionError("the digest of " + id));

        assertEquals(id, digest.getAuditId());
        DigestNode course = digest.getRoot();
        assertEquals(AuditTarget.COURSE, course.getTarget());
        assertEquals(0.456, course.getNumbers().getVocabularyScore(), 1e-12, "R010: copied from the report");
        DigestNode digestQuiz = course.getChildren().get(0);
        assertEquals("67fab6d599301022953425b1", digestQuiz.getNodeId());
        assertEquals("Where is my wallet?", digestQuiz.getLabel());
        assertEquals(published, digestQuiz.getNumbers(), "R010: the numbers, as the engine published them");
        assertEquals(List.of(wallet), digestQuiz.getFindings(), "the findings of the node");
        assertEquals(List.of("quiz-instruction"), digestQuiz.getUnevaluatedBy(), "what was left unevaluated");
        try (var files = Files.list(tempDir.resolve(".content-audit/audit-digests"))) {
            String json = Files.readString(files.findFirst().orElseThrow());
            assertFalse(json.contains("¿Dónde está mi billetera?") || json.contains("sentenceParts")
                    || json.contains("\"tokens\""), "the digest carries no course entity");
        }
    }

    @Test
    @DisplayName("should keep judge coverage, exam standing and explicit runtime outside node score maps")
    @Tag("F-JEXAM-R014")
    void shouldKeepJudgeCoverageExamStandingAndExplicitRuntimeOutsideNodeScoreMaps(@TempDir Path tempDir) {
        EvaluationRuntimeIdentity codex = EvaluationRuntimeIdentity.explicit("codex-cli", "gpt-6.1-sol", "high");
        JudgeExamStanding standing = new JudgeExamStanding("quiz-instruction", "process-v2",
                JudgeExamStatus.APPROVED, true, "approved for this exact runtime", codex,
                java.util.Set.of("process-v2", "legacy-runtime-coupled-v1"));
        JudgeCoverageDiagnosis coverage = new JudgeCoverageDiagnosis("quiz-instruction", AuditTarget.QUIZ,
                12, 9, 3, List.of(), standing, List.of(), null,
                new JudgeCostEstimate(3, 3, 18.0, true, 0.5, 3, 0.2), List.of(), null);
        DefaultCourseDiagnoses diagnoses = new DefaultCourseDiagnoses();
        diagnoses.addJudgeCoverage(coverage);
        AuditNode root = new AuditNode(null, AuditTarget.COURSE, null, List.of(),
                Map.of("lemma-count", 0.91), Map.of(), diagnoses);
        root.setNumbers(new ContextNumbers(0.91, List.of(), null, null));
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(tempDir, null);

        String id = store.save(new AuditReport(root));
        AuditDigest digest = store.loadDigest(id).orElseThrow();

        assertEquals(0.91, digest.getRoot().getNumbers().getVocabularyScore(), 1e-12);
        assertFalse(digest.getRoot().getNumbers().toString().contains("APPROVED"),
                "exam standing is not smuggled into the numeric score map");
        assertEquals(1, digest.getJudgeCoverages().size());
        JudgeCoverageDiagnosis loaded = digest.getJudgeCoverages().get(0);
        assertEquals(List.of(12, 9, 3), List.of(loaded.getReached(), loaded.getEvaluated(), loaded.getNotEvaluated()));
        assertEquals(JudgeExamStatus.APPROVED, loaded.getStanding().getStatus());
        assertEquals(codex, loaded.getStanding().getRuntimeIdentity());
        assertEquals(java.util.Set.of("process-v2", "legacy-runtime-coupled-v1"),
                loaded.getStanding().getCompatibleJudgeVersions());
    }

    @Test
    @DisplayName("should label each exercise of the digest with its statement as the student sees it, «You ___ (see) cats.» and not «Afirmativas regulares»")
    @Tag("FEAT-RONDA1")
    @Tag("F-RONDA1-R002")
    public void shouldLabelEachExerciseOfTheDigestWithItsStatementAsTheStudentSeesItYouSeeCatsAndNotAfirmativasRegulares(
            @TempDir Path tempDir) {
        // R002: in the digest each exercise is named by its statement as the student sees it
        // (F-RONDA1-R001): the 20 of Afirmativas regulares no longer all read «Afirmativas regulares».
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(tempDir, studentViewRenderer());
        String id = store.save(afirmativasRegulares());

        DigestNode knowledge = digestKnowledge(store.loadDigest(id).orElseThrow());

        assertEquals(List.of("You ___ (see) cats.", "It ___ (come) every year."),
                knowledge.getChildren().stream().map(DigestNode::getLabel).toList(),
                "R002: «You ___ (see) cats.», and not «Afirmativas regulares»");
        assertEquals(List.of("67fab6d59930102295341ec3", "67fab6d59930102295341ec4"),
                knowledge.getChildren().stream().map(DigestNode::getNodeId).toList(), "the same exercises");
    }

    @Test
    @DisplayName("should keep in the digest the labels of the course, the levels, the topic groups and the topics")
    @Tag("FEAT-RONDA1")
    @Tag("F-RONDA1-R002")
    public void shouldKeepInTheDigestTheLabelsOfTheCourseTheLevelsTheTopicGroupsAndTheTopics(@TempDir Path tempDir) {
        // R002: only the exercises change their label; the course, the level, the topic group
        // and the topic keep theirs.
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(tempDir, studentViewRenderer());
        String id = store.save(afirmativasRegulares());

        DigestNode course = store.loadDigest(id).orElseThrow().getRoot();
        DigestNode level = course.getChildren().get(0);
        DigestNode topic = level.getChildren().get(0);
        DigestNode knowledge = topic.getChildren().get(0);

        assertEquals(List.of("Curso", "A1", "Present Simple", "Afirmativas regulares"),
                List.of(course.getLabel(), level.getLabel(), topic.getLabel(), knowledge.getLabel()));
    }

    @Test
    @DisplayName("should keep «Afirmativas regulares» as the exercise label of the saved report, the title the quiz instruction judge reads")
    @Tag("FEAT-RONDA1")
    @Tag("F-RONDA1-R002")
    public void shouldKeepAfirmativasRegularesAsTheExerciseLabelOfTheSavedReportTheTitleTheQuizInstructionJudgeReads(
            @TempDir Path tempDir) throws Exception {
        // R002: the saved report keeps the label of today, the title the quiz-instruction judge
        // reads: changing it would charge its verdicts again. Only the digest names by statement.
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(tempDir, studentViewRenderer());
        String id = store.save(afirmativasRegulares());

        AuditReport loaded = store.load(id).orElseThrow();
        AuditNode quiz = loaded.getRoot().getChildren().get(0).getChildren().get(0).getChildren().get(0)
                .getChildren().get(0);
        assertEquals("Afirmativas regulares", quiz.getEntity().getLabel(), "R002: the report keeps the title");
        String report = Files.readString(tempDir.resolve(".content-audit/audits/audit-" + id + ".json"));
        assertFalse(report.contains("You ___ (see) cats."), "the statement is not written into the report");
    }

    @Test
    @DisplayName("should copy into the digest the analyzers that evaluated each node halfway")
    @Tag("FEAT-RONDA1")
    @Tag("F-RONDA1-R006")
    public void shouldCopyIntoTheDigestTheAnalyzersThatEvaluatedEachNodeHalfway(@TempDir Path tempDir) {
        // R006: the digest copies, like the unevaluated ones, the analyzers that evaluated a node
        // halfway -- «___ a child? (He)» with its judge pending -- and the report keeps them too.
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(tempDir, studentViewRenderer());
        AuditReport report = afirmativasRegulares();
        AuditNode knowledge = report.getRoot().getChildren().get(0).getChildren().get(0).getChildren().get(0);
        AuditNode first = knowledge.getChildren().get(0);
        first.setPartiallyEvaluatedBy(new ArrayList<>(List.of("multiple-choice-validity")));

        String id = store.save(report);

        DigestNode digestKnowledge = digestKnowledge(store.loadDigest(id).orElseThrow());
        assertEquals(List.of("multiple-choice-validity"), digestKnowledge.getChildren().get(0).getPartiallyEvaluatedBy(),
                "R006: copied into the digest");
        assertEquals(List.of(), digestKnowledge.getChildren().get(1).getPartiallyEvaluatedBy(), "and none where there is none");
        assertEquals(List.of("multiple-choice-validity"), store.load(id).orElseThrow().getRoot().getChildren().get(0)
                .getChildren().get(0).getChildren().get(0).getChildren().get(0).getPartiallyEvaluatedBy(),
                "the report keeps it");
    }

    // ------------------------------------------------------------------
    // FEAT-RONDA1: A1 > Present Simple > Afirmativas regulares, two of its exercises
    // ------------------------------------------------------------------

    private static com.learney.contentaudit.auditdomain.QuizStudentViewRenderer studentViewRenderer() {
        return new com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer(
                com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter.create());
    }

    private static AuditReport afirmativasRegulares() {
        AuditNode root = new AuditNode(null, AuditTarget.COURSE, null, new ArrayList<>(), new LinkedHashMap<>(),
                new LinkedHashMap<>(), null);
        AuditNode level = child(root, AuditTarget.MILESTONE,
                new AuditableMilestone(List.of(), "6814dafa7d73e7209a13d382", "A1", "A1"));
        AuditNode topic = child(level, AuditTarget.TOPIC,
                new AuditableTopic(List.of(), "t-present-simple", "Present Simple", "T"));
        AuditNode knowledge = child(topic, AuditTarget.KNOWLEDGE, new AuditableKnowledge(List.of(),
                "Afirmativas regulares", "Escribe la forma correcta del verbo.", true, "6814dafa7d73e7209a13d389",
                "Afirmativas regulares", "K", com.learney.contentaudit.coursedomain.SentenceMode.FILL, "Present Simple"));
        child(knowledge, AuditTarget.QUIZ, afirmativa("67fab6d59930102295341ec3", "You", "see", "(see) cats.",
                "You see cats."));
        child(knowledge, AuditTarget.QUIZ, afirmativa("67fab6d59930102295341ec4", "It", "comes",
                "(come) every year.", "It comes every year."));
        return new AuditReport(root);
    }

    private static AuditableQuiz afirmativa(String id, String subject, String answer, String rest, String sentence) {
        List<com.learney.contentaudit.coursedomain.SentencePartEntity> parts = List.of(
                new com.learney.contentaudit.coursedomain.SentencePartEntity(
                        com.learney.contentaudit.coursedomain.SentencePartKind.TEXT, subject, null),
                new com.learney.contentaudit.coursedomain.SentencePartEntity(
                        com.learney.contentaudit.coursedomain.SentencePartKind.CLOZE, "", List.of(answer)),
                new com.learney.contentaudit.coursedomain.SentencePartEntity(
                        com.learney.contentaudit.coursedomain.SentencePartKind.TEXT, rest, null));
        return new AuditableQuiz(List.of(), id, "Afirmativas regulares", "", null, List.of(sentence),
                subject + " ____ [" + answer + "] " + rest, "Escribe la forma afirmativa en presente simple.", parts,
                null, "Con he, she, it el verbo lleva $-s$. Con I, you, we, they va en forma base.", "A1.01.06.01");
    }

    private static AuditNode child(AuditNode parent, AuditTarget target,
            com.learney.contentaudit.auditdomain.AuditableEntity entity) {
        AuditNode node = new AuditNode(entity, target, parent, new ArrayList<>(), new LinkedHashMap<>(),
                new LinkedHashMap<>(), null);
        parent.getChildren().add(node);
        return node;
    }

    private static DigestNode digestKnowledge(AuditDigest digest) {
        return digest.getRoot().getChildren().get(0).getChildren().get(0).getChildren().get(0);
    }

    @Test
    @DisplayName("should keep the measures of the root when a report is saved and loaded again")
    @Tag("FEAT-RONDA2")
    @Tag("F-RONDA2-R004")
    public void shouldKeepTheMeasuresOfTheRootWhenAReportIsSavedAndLoadedAgain(@TempDir Path tempDir) throws Exception {
        // F-RONDA2-R004: the root of the report keeps the measure of each analyzer that ran, in
        // catalog order, with its configuration; the stored report gives them back as they were.
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(tempDir, studentViewRenderer());
        AuditReport report = afirmativasRegulares();
        Map<String, Object> bands = new LinkedHashMap<>();
        bands.put("analyzerName", "lemma-count");
        bands.put("threshold", 4);
        bands.put("bands", List.of(Map.of("name", "1.000 más comunes", "maxCocaRank", 1000, "threshold", 8),
                Map.of("name", "hasta 2.000", "maxCocaRank", 2000, "threshold", 6)));
        List<AnalyzerMeasure> measures = List.of(
                new AnalyzerMeasure("sentence-length", "1".repeat(64), Map.of("analyzerName", "sentence-length",
                        "toleranceMargin", 5)),
                new AnalyzerMeasure("lemma-count", "2".repeat(64), bands),
                new AnalyzerMeasure("hint-coherence", "3".repeat(64), Map.of()));
        report.getRoot().setMeasures(measures);

        AuditReport loaded = store.load(store.save(report)).orElseThrow();

        assertEquals(measures, loaded.getRoot().getMeasures(), "R004: the measures of the root, as they were");
        assertEmptyBelow(loaded.getRoot());
    }

    @Test
    @DisplayName("should load a report saved before round 2 with an empty list of measures, without inventing them")
    @Tag("FEAT-RONDA2")
    @Tag("F-RONDA2-R004")
    public void shouldLoadAReportSavedBeforeRound2WithAnEmptyListOfMeasuresWithoutInventingThem(@TempDir Path tempDir) throws Exception {
        // F-RONDA2-R004 inv. 4: a report stored before the rule has no measures; it is read with the
        // list empty, on the root and on every node -- nothing invents its measure.
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(tempDir, studentViewRenderer());
        String id = store.save(afirmativasRegulares());
        Path file = tempDir.resolve(".content-audit/audits/audit-" + id + ".json");
        com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode stored = json.readTree(file.toFile());
        withoutMeasures(stored);
        json.writeValue(file.toFile(), stored);
        assertFalse(Files.readString(file).contains("\"measures\""), "a report of before round 2");

        AuditReport loaded = store.load(id).orElseThrow();

        assertEquals(List.of(), loaded.getRoot().getMeasures(), "R004 inv. 4: an empty list on the root");
        assertEmptyBelow(loaded.getRoot());
        assertEquals("Afirmativas regulares", loaded.getRoot().getChildren().get(0).getChildren().get(0).getChildren().get(0)
                .getEntity().getLabel(), "the rest of the report is read as before");
    }

    @Test
    @DisplayName("should leave a saved projection out of list and loadLatest even when it was saved after the last analysis")
    @Tag("FEAT-PROYU")
    @Tag("F-PROYU-R004")
    public void shouldLeaveASavedProjectionOutOfListAndLoadLatestEvenWhenItWasSavedAfterTheLastAnalysis(
            @TempDir Path tempDir) {
        // R004: the base of 29/9 is the last analysis of the history; the projection of the four
        // proposals of «Afirmativas regulares» is saved after it and is still not an analysis:
        // get audits does not list it and the latest analysis is still the base.
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(tempDir, null);
        String base = store.save(FileSystemProjectionStoreTest.report(0.7392, 1.0));
        String projectionId = new FileSystemProjectionStore(tempDir)
                .save(FileSystemProjectionStoreTest.fourProposals(0.7391, 0.9)).getProjectionId();

        assertEquals(List.of(base), store.list().stream().map(AuditReportSummary::getId).toList(),
                "R004: get audits lists the same analyses, without the projection");
        AuditReport latest = store.loadLatest().orElseThrow();
        assertEquals(0.7392, latest.getRoot().getNumbers().getVocabularyScore(), 1e-12,
                "R004: the latest analysis is still the base, not the projection saved after it");
        assertEquals(1.0, FileSystemProjectionStoreTest.quiz(latest).getScores().get("lemma-absence"), 1e-12);
        assertEquals(Optional.empty(), store.loadDigest(projectionId), "a projection has no digest of an analysis");
    }

    @Test
    @DisplayName("should load a saved projection by its id when no analysis has that id, so plan --audit and the suggested words query accept it as their source analysis")
    @Tag("FEAT-PROYU")
    @Tag("F-PROYU-R005")
    public void shouldLoadASavedProjectionByItsIdWhenNoAnalysisHasThatIdSoPlanAuditAndTheSuggestedWordsQueryAcceptItAsTheirSourceAnalysis(
            @TempDir Path tempDir) {
        // R005 (hook F-05): plan --audit and the suggested words load their source analysis with
        // AuditReportStore.load; given the id of a saved projection, the store gives the photo with
        // the pending proposals, and an analysis id still gives that analysis.
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(tempDir, null);
        String base = store.save(FileSystemProjectionStoreTest.report(0.7392, 1.0));
        String projectionId = new FileSystemProjectionStore(tempDir)
                .save(FileSystemProjectionStoreTest.fourProposals(0.7391, 0.9)).getProjectionId();

        AuditReport projected = store.load(projectionId).orElseThrow(
                () -> new AssertionError("R005: the id of the saved projection is accepted as a source analysis"));

        assertEquals(0.7391, projected.getRoot().getNumbers().getVocabularyScore(), 1e-12, "the projected photo");
        AuditNode grandmother = FileSystemProjectionStoreTest.quiz(projected);
        assertEquals(0.9, grandmother.getScores().get("lemma-absence"), 1e-12,
                "«My grandmother ___ (come) every year.» as the pending proposal wrote it");
        assertEquals(AuditTarget.KNOWLEDGE, grandmother.getParent().getTarget(), "with its tree whole");
        assertEquals(0.7392, store.load(base).orElseThrow().getRoot().getNumbers().getVocabularyScore(), 1e-12,
                "an analysis id still loads that analysis");
        assertEquals(Optional.empty(), store.load("2026-01-01T00-00-00"), "an id that is neither loads nothing");
    }

    /** F-RONDA2-R004 inv. 4: only the root keeps measures. */
    private static void assertEmptyBelow(AuditNode root) {
        for (AuditNode child : root.getChildren()) {
            assertEquals(List.of(), child.getMeasures(), "no measures below the root");
            assertEmptyBelow(child);
        }
    }

    /** The stored tree as a report of before round 2 had it: without the field of the measures. */
    private static void withoutMeasures(com.fasterxml.jackson.databind.JsonNode node) {
        if (node instanceof com.fasterxml.jackson.databind.node.ObjectNode object) {
            object.remove("measures");
        }
        node.forEach(FileSystemAuditReportStoreTest::withoutMeasures);
    }
}
