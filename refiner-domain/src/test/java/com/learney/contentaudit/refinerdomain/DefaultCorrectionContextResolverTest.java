package com.learney.contentaudit.refinerdomain;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableMilestone;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.AuditableTopic;
import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.auditdomain.DefaultKnowledgeDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultLevelDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultQuizDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultTopicDiagnoses;
import com.learney.contentaudit.auditdomain.SentenceLengthDiagnosis;
import com.learney.contentaudit.auditdomain.labs.AbsenceType;
import com.learney.contentaudit.auditdomain.labs.AbsentLemma;
import com.learney.contentaudit.auditdomain.labs.LemmaAbsenceLevelDiagnosis;
import com.learney.contentaudit.auditdomain.labs.LemmaAndPos;
import com.learney.contentaudit.auditdomain.labs.PriorityLevel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class DefaultCorrectionContextResolverTest {

    private DefaultCorrectionContextResolver sut;

    @BeforeEach
    void setUp() {
        sut = new DefaultCorrectionContextResolver();
    }

    // ------------------------------------------------------------------
    // Tree builder helpers
    // ------------------------------------------------------------------

    private AuditNode buildCourseNode() {
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.COURSE);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        return node;
    }

    private AuditNode buildMilestoneNode(AuditNode parent, AuditableMilestone milestone,
            DefaultLevelDiagnoses diagnoses) {
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.MILESTONE);
        node.setEntity(milestone);
        node.setParent(parent);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        node.setDiagnoses(diagnoses);
        parent.getChildren().add(node);
        return node;
    }

    private AuditNode buildTopicNode(AuditNode parent, AuditableTopic topic) {
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.TOPIC);
        node.setEntity(topic);
        node.setParent(parent);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        node.setDiagnoses(new DefaultTopicDiagnoses());
        parent.getChildren().add(node);
        return node;
    }

    private AuditNode buildKnowledgeNode(AuditNode parent, AuditableKnowledge knowledge) {
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.KNOWLEDGE);
        node.setEntity(knowledge);
        node.setParent(parent);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        node.setDiagnoses(new DefaultKnowledgeDiagnoses());
        parent.getChildren().add(node);
        return node;
    }

    private AuditNode buildQuizNode(AuditNode parent, AuditableQuiz quiz,
            DefaultQuizDiagnoses diagnoses) {
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.QUIZ);
        node.setEntity(quiz);
        node.setParent(parent);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        node.setDiagnoses(diagnoses);
        parent.getChildren().add(node);
        return node;
    }

    /**
     * Builds a full tree: COURSE → MILESTONE → TOPIC → KNOWLEDGE → QUIZ.
     * Returns an array: [courseNode, milestoneNode, topicNode, knowledgeNode, quizNode]
     */
    private AuditNode[] buildFullTree(String milestoneId,
            AuditableMilestone milestone, DefaultLevelDiagnoses milestoneDiagnoses,
            AuditableTopic topic,
            AuditableKnowledge knowledge,
            AuditableQuiz quiz, DefaultQuizDiagnoses quizDiagnoses) {
        AuditNode courseNode = buildCourseNode();
        AuditNode milestoneNode = buildMilestoneNode(courseNode, milestone, milestoneDiagnoses);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        AuditNode quizNode = buildQuizNode(knowledgeNode, quiz, quizDiagnoses);
        return new AuditNode[]{courseNode, milestoneNode, topicNode, knowledgeNode, quizNode};
    }

    private RefinementTask buildTask(String quizId) {
        return new RefinementTask(
                "task-1",
                AuditTarget.QUIZ,
                quizId,
                "Quiz label",
                DiagnosisKind.SENTENCE_LENGTH,
                1,
                RefinementTaskStatus.PENDING);
    }

    private SentenceLengthDiagnosis buildDiagnosis(int tokenCount, int targetMin, int targetMax,
            CefrLevel level, int delta) {
        return new SentenceLengthDiagnosis(tokenCount, targetMin, targetMax, level, delta, 3);
    }

    private DefaultQuizDiagnoses buildQuizDiagnoses(SentenceLengthDiagnosis sld) {
        DefaultQuizDiagnoses d = new DefaultQuizDiagnoses();
        d.setSentenceLengthDiagnosis(sld);
        return d;
    }

    private AbsentLemma buildAbsentLemma(String lemma, String pos, AbsenceType type, int cocaRank) {
        return new AbsentLemma(
                new LemmaAndPos(lemma, pos),
                CefrLevel.A1,
                type,
                List.of(),
                PriorityLevel.HIGH,
                cocaRank,
                null);
    }

    private DefaultLevelDiagnoses buildMilestoneDiagnoses(List<AbsentLemma> absentLemmas) {
        LemmaAbsenceLevelDiagnosis absenceDiagnosis = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 100, absentLemmas.size(), 10.0, 90.0,
                0.8, 0.7, 0.5, null, absentLemmas, List.of(), 2, 3, 5);
        DefaultLevelDiagnoses diagnoses = new DefaultLevelDiagnoses();
        diagnoses.setLemmaAbsenceDiagnosis(absenceDiagnosis);
        return diagnoses;
    }

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should resolve context with all fields populated from quiz diagnosis and ancestor entities")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R001")
    @Tag("F-RCSL-J001")
    public void shouldResolveContextWithAllFieldsPopulatedFromQuizDiagnosisAndAncestorEntities() {
        AuditableQuiz quiz = new AuditableQuiz("The cat sat.", List.of(), "quiz-1", null, null, "El gato se sentó.");
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Animals", "Complete the sentence.", true, "know-1", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-1", "Basic Animals", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-1", "A1", null);

        SentenceLengthDiagnosis sld = buildDiagnosis(3, 2, 5, CefrLevel.A1, 0);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnoses(sld);

        List<AbsentLemma> absentLemmas = List.of(buildAbsentLemma("cat", "NOUN", AbsenceType.COMPLETELY_ABSENT, 500));
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(absentLemmas);

        buildFullTree("ms-1", milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditNode[] tree = buildFullTree("ms-1",
                new AuditableMilestone(List.of(), "ms-1", "A1", null),
                milestoneDiag,
                new AuditableTopic(List.of(), "topic-1", "Basic Animals", null),
                new AuditableKnowledge(List.of(), "Animals", "Complete the sentence.", true, "know-1", null, null),
                new AuditableQuiz("The cat sat.", List.of(), "quiz-1", null, null, "El gato se sentó."),
                quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-1");

        Optional<SentenceLengthCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        SentenceLengthCorrectionContext ctx = result.get();
        Assertions.assertEquals("task-1", ctx.getTaskId());
        Assertions.assertEquals("The cat sat.", ctx.getSentence());
        Assertions.assertEquals("El gato se sentó.", ctx.getTranslation());
        Assertions.assertEquals("Animals", ctx.getKnowledgeTitle());
        Assertions.assertEquals("Complete the sentence.", ctx.getKnowledgeInstructions());
        Assertions.assertEquals("Basic Animals", ctx.getTopicLabel());
        Assertions.assertEquals(CefrLevel.A1, ctx.getCefrLevel());
        Assertions.assertEquals(3, ctx.getTokenCount());
        Assertions.assertEquals(2, ctx.getTargetMin());
        Assertions.assertEquals(5, ctx.getTargetMax());
        Assertions.assertEquals(0, ctx.getDelta());
        Assertions.assertNotNull(ctx.getSuggestedLemmas());
    }

    @Test
    @DisplayName("should populate sentence and translation from AuditableQuiz entity on the quiz node")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R001")
    public void shouldPopulateSentenceAndTranslationFromAuditableQuizEntityOnTheQuizNode() {
        AuditableQuiz quiz = new AuditableQuiz("I love learning.", List.of(), "quiz-2", null, null, "Me encanta aprender.");
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Feelings", "Complete.", true, "know-2", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-2", "Emotions", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-2", "A1", null);

        SentenceLengthDiagnosis sld = buildDiagnosis(3, 2, 5, CefrLevel.A1, 0);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnoses(sld);
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(List.of());

        AuditNode[] tree = buildFullTree("ms-2", milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-2");

        Optional<SentenceLengthCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("I love learning.", result.get().getSentence());
        Assertions.assertEquals("Me encanta aprender.", result.get().getTranslation());
    }

    @Test
    @DisplayName("should populate knowledgeTitle and knowledgeInstructions from AuditableKnowledge on knowledge ancestor")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R001")
    public void shouldPopulateKnowledgeTitleAndKnowledgeInstructionsFromAuditableKnowledgeOnKnowledgeAncestor(
            ) {
        AuditableQuiz quiz = new AuditableQuiz("He reads.", List.of(), "quiz-3", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Reading Skills", "Choose the correct word.", true, "know-3", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-3", "Skills", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-3", "A2", null);

        SentenceLengthDiagnosis sld = buildDiagnosis(2, 2, 5, CefrLevel.A2, 0);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnoses(sld);
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(List.of());

        AuditNode[] tree = buildFullTree("ms-3", milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-3");

        Optional<SentenceLengthCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("Reading Skills", result.get().getKnowledgeTitle());
        Assertions.assertEquals("Choose the correct word.", result.get().getKnowledgeInstructions());
    }

    @Test
    @DisplayName("should populate topicLabel from AuditableTopic on topic ancestor")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R001")
    public void shouldPopulateTopicLabelFromAuditableTopicOnTopicAncestor() {
        AuditableQuiz quiz = new AuditableQuiz("She sings.", List.of(), "quiz-4", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Music", "Complete.", true, "know-4", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-4", "Arts and Culture", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-4", "B1", null);

        SentenceLengthDiagnosis sld = buildDiagnosis(2, 2, 8, CefrLevel.B1, 0);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnoses(sld);
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(List.of());

        AuditNode[] tree = buildFullTree("ms-4", milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-4");

        Optional<SentenceLengthCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("Arts and Culture", result.get().getTopicLabel());
    }

    @Test
    @DisplayName("should populate cefrLevel tokenCount targetMin targetMax and delta from SentenceLengthDiagnosis")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R001")
    public void shouldPopulateCefrLevelTokenCountTargetMinTargetMaxAndDeltaFromSentenceLengthDiagnosis(
            ) {
        AuditableQuiz quiz = new AuditableQuiz("They were going.", List.of(), "quiz-5", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Verbs", "Complete.", true, "know-5", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-5", "Grammar", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-5", "B2", null);

        SentenceLengthDiagnosis sld = buildDiagnosis(11, 10, 15, CefrLevel.B2, 2);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnoses(sld);
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(List.of());

        AuditNode[] tree = buildFullTree("ms-5", milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-5");

        Optional<SentenceLengthCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        SentenceLengthCorrectionContext ctx = result.get();
        Assertions.assertEquals(CefrLevel.B2, ctx.getCefrLevel());
        Assertions.assertEquals(11, ctx.getTokenCount());
        Assertions.assertEquals(10, ctx.getTargetMin());
        Assertions.assertEquals(15, ctx.getTargetMax());
        Assertions.assertEquals(2, ctx.getDelta());
    }

    @Test
    @DisplayName("should return empty when quiz node is not found in the audit tree")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R002")
    public void shouldReturnEmptyWhenQuizNodeIsNotFoundInTheAuditTree() {
        AuditableQuiz quiz = new AuditableQuiz("Hello.", List.of(), "quiz-6", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete.", true, "know-6", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-6", "Social", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-6", "A1", null);

        SentenceLengthDiagnosis sld = buildDiagnosis(1, 1, 5, CefrLevel.A1, 0);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnoses(sld);
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(List.of());

        AuditNode[] tree = buildFullTree("ms-6", milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        // task points to a non-existent quiz ID
        RefinementTask task = buildTask("nonexistent-quiz-id");

        Optional<SentenceLengthCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty when task nodeTarget does not match any node target in the tree")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R002")
    public void shouldReturnEmptyWhenTaskNodeTargetDoesNotMatchAnyNodeTargetInTheTree() {
        AuditableQuiz quiz = new AuditableQuiz("Hello.", List.of(), "quiz-7", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete.", true, "know-7", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-7", "Social", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-7", "A1", null);

        SentenceLengthDiagnosis sld = buildDiagnosis(1, 1, 5, CefrLevel.A1, 0);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnoses(sld);
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(List.of());

        AuditNode[] tree = buildFullTree("ms-7", milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);

        // task has target KNOWLEDGE but id is the quiz's id — won't match QUIZ target
        RefinementTask task = new RefinementTask("task-7", AuditTarget.KNOWLEDGE, "quiz-7",
                "label", DiagnosisKind.SENTENCE_LENGTH, 1, RefinementTaskStatus.PENDING);

        Optional<SentenceLengthCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should locate the correct quiz node when multiple quiz nodes exist in the tree")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R002")
    public void shouldLocateTheCorrectQuizNodeWhenMultipleQuizNodesExistInTheTree() {
        // Build tree with two quizzes under same knowledge
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Mixed", "Do it.", true, "know-8", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-8", "Topic 8", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-8", "A1", null);

        AuditableQuiz quiz1 = new AuditableQuiz("First sentence.", List.of(), "quiz-8a", null, null, null);
        AuditableQuiz quiz2 = new AuditableQuiz("Second sentence.", List.of(), "quiz-8b", null, null, null);

        SentenceLengthDiagnosis sld1 = buildDiagnosis(2, 2, 5, CefrLevel.A1, 0);
        SentenceLengthDiagnosis sld2 = buildDiagnosis(2, 2, 5, CefrLevel.A1, 0);
        DefaultQuizDiagnoses diag1 = buildQuizDiagnoses(sld1);
        DefaultQuizDiagnoses diag2 = buildQuizDiagnoses(sld2);

        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(List.of());

        AuditNode courseNode = buildCourseNode();
        AuditNode milestoneNode = buildMilestoneNode(courseNode, milestone, milestoneDiag);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        buildQuizNode(knowledgeNode, quiz1, diag1);
        buildQuizNode(knowledgeNode, quiz2, diag2);

        AuditReport report = new AuditReport(courseNode);
        RefinementTask task = buildTask("quiz-8b");

        Optional<SentenceLengthCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("Second sentence.", result.get().getSentence());
    }

    @Test
    @DisplayName("should include only COMPLETELY_ABSENT and APPEARS_TOO_LATE lemmas and exclude APPEARS_TOO_EARLY")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R003")
    public void shouldIncludeOnlyCOMPLETELYABSENTAndAPPEARSTOOLATELemmasAndExcludeAPPEARSTOOEARLY(
            ) {
        AuditableQuiz quiz = new AuditableQuiz("A sentence.", List.of(), "quiz-9", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-9", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-9", "T9", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-9", "A1", null);

        SentenceLengthDiagnosis sld = buildDiagnosis(2, 2, 5, CefrLevel.A1, 0);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnoses(sld);

        List<AbsentLemma> absentLemmas = List.of(
                buildAbsentLemma("walk", "VERB", AbsenceType.COMPLETELY_ABSENT, 100),
                buildAbsentLemma("run", "VERB", AbsenceType.APPEARS_TOO_LATE, 200),
                buildAbsentLemma("fly", "VERB", AbsenceType.APPEARS_TOO_EARLY, 300));
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(absentLemmas);

        AuditNode[] tree = buildFullTree("ms-9", milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-9");

        Optional<SentenceLengthCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();
        Assertions.assertEquals(2, lemmas.size());
        Assertions.assertTrue(lemmas.stream().noneMatch(l -> l.getReason().equals("APPEARS_TOO_EARLY")));
        Assertions.assertTrue(lemmas.stream().anyMatch(l -> l.getLemma().equals("walk")));
        Assertions.assertTrue(lemmas.stream().anyMatch(l -> l.getLemma().equals("run")));
    }

    @Test
    @DisplayName("should order suggested lemmas by COCA rank ascending with lowest rank first")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R003")
    public void shouldOrderSuggestedLemmasByCOCARankAscendingWithLowestRankFirst() {
        AuditableQuiz quiz = new AuditableQuiz("A sentence.", List.of(), "quiz-10", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-10", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-10", "T10", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-10", "A1", null);

        SentenceLengthDiagnosis sld = buildDiagnosis(2, 2, 5, CefrLevel.A1, 0);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnoses(sld);

        List<AbsentLemma> absentLemmas = List.of(
                buildAbsentLemma("dog", "NOUN", AbsenceType.COMPLETELY_ABSENT, 500),
                buildAbsentLemma("cat", "NOUN", AbsenceType.COMPLETELY_ABSENT, 200),
                buildAbsentLemma("bird", "NOUN", AbsenceType.APPEARS_TOO_LATE, 350));
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(absentLemmas);

        AuditNode[] tree = buildFullTree("ms-10", milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-10");

        Optional<SentenceLengthCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();
        Assertions.assertEquals(3, lemmas.size());
        Assertions.assertEquals("cat", lemmas.get(0).getLemma());   // rank 200
        Assertions.assertEquals("bird", lemmas.get(1).getLemma());  // rank 350
        Assertions.assertEquals("dog", lemmas.get(2).getLemma());   // rank 500
    }

    @Test
    @DisplayName("should place lemmas without COCA rank after lemmas with COCA rank")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R003")
    public void shouldPlaceLemmasWithoutCOCARankAfterLemmasWithCOCARank() {
        AuditableQuiz quiz = new AuditableQuiz("A sentence.", List.of(), "quiz-11", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-11", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-11", "T11", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-11", "A1", null);

        SentenceLengthDiagnosis sld = buildDiagnosis(2, 2, 5, CefrLevel.A1, 0);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnoses(sld);

        // cocaRank=0 means no rank (sentinel value)
        List<AbsentLemma> absentLemmas = List.of(
                buildAbsentLemma("unknown", "NOUN", AbsenceType.COMPLETELY_ABSENT, 0),  // no rank
                buildAbsentLemma("table", "NOUN", AbsenceType.COMPLETELY_ABSENT, 150),   // has rank
                buildAbsentLemma("chair", "NOUN", AbsenceType.APPEARS_TOO_LATE, 0));     // no rank
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(absentLemmas);

        AuditNode[] tree = buildFullTree("ms-11", milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-11");

        Optional<SentenceLengthCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();
        Assertions.assertEquals(3, lemmas.size());
        // "table" has rank 150 → first
        Assertions.assertEquals("table", lemmas.get(0).getLemma());
        // lemmas with rank 0 come after
        Assertions.assertTrue(lemmas.get(1).getCocaRank() == 0 || lemmas.get(2).getCocaRank() == 0);
    }

    @Test
    @DisplayName("should map AbsentLemma fields to SuggestedLemma fields correctly")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R003")
    public void shouldMapAbsentLemmaFieldsToSuggestedLemmaFieldsCorrectly() {
        AuditableQuiz quiz = new AuditableQuiz("A sentence.", List.of(), "quiz-12", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-12", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-12", "T12", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-12", "A1", null);

        SentenceLengthDiagnosis sld = buildDiagnosis(2, 2, 5, CefrLevel.A1, 0);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnoses(sld);

        List<AbsentLemma> absentLemmas = List.of(
                buildAbsentLemma("house", "NOUN", AbsenceType.COMPLETELY_ABSENT, 250));
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(absentLemmas);

        AuditNode[] tree = buildFullTree("ms-12", milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-12");

        Optional<SentenceLengthCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();
        Assertions.assertEquals(1, lemmas.size());
        SuggestedLemma sl = lemmas.get(0);
        Assertions.assertEquals("house", sl.getLemma());
        Assertions.assertEquals("NOUN", sl.getPos());
        Assertions.assertEquals("COMPLETELY_ABSENT", sl.getReason());
        Assertions.assertEquals(250, sl.getCocaRank());
    }

    @Test
    @DisplayName("should return empty suggested lemmas when all absent lemmas are APPEARS_TOO_EARLY")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R003")
    public void shouldReturnEmptySuggestedLemmasWhenAllAbsentLemmasAreAPPEARSTOOEARLY() {
        AuditableQuiz quiz = new AuditableQuiz("A sentence.", List.of(), "quiz-13", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-13", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-13", "T13", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-13", "A1", null);

        SentenceLengthDiagnosis sld = buildDiagnosis(2, 2, 5, CefrLevel.A1, 0);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnoses(sld);

        List<AbsentLemma> absentLemmas = List.of(
                buildAbsentLemma("advanced", "ADJ", AbsenceType.APPEARS_TOO_EARLY, 1000),
                buildAbsentLemma("complex", "ADJ", AbsenceType.APPEARS_TOO_EARLY, 2000));
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(absentLemmas);

        AuditNode[] tree = buildFullTree("ms-13", milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-13");

        Optional<SentenceLengthCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertTrue(result.get().getSuggestedLemmas().isEmpty());
    }

    @Test
    @DisplayName("should return suggested lemmas from the milestone ancestor of the quiz node")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R003")
    public void shouldReturnSuggestedLemmasFromTheMilestoneAncestorOfTheQuizNode() {
        AuditableQuiz quiz = new AuditableQuiz("A sentence.", List.of(), "quiz-14", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-14", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-14", "T14", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-14", "A1", null);

        SentenceLengthDiagnosis sld = buildDiagnosis(2, 2, 5, CefrLevel.A1, 0);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnoses(sld);

        // The absent lemmas are attached to the MILESTONE ancestor
        List<AbsentLemma> absentLemmas = List.of(
                buildAbsentLemma("jump", "VERB", AbsenceType.COMPLETELY_ABSENT, 400));
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(absentLemmas);

        AuditNode[] tree = buildFullTree("ms-14", milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-14");

        Optional<SentenceLengthCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();
        Assertions.assertEquals(1, lemmas.size());
        Assertions.assertEquals("jump", lemmas.get(0).getLemma());
    }

    @Test
    @DisplayName("should return context with empty suggested lemmas when milestone has no LemmaAbsenceLevelDiagnosis")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R004")
    @Tag("F-RCSL-J003")
    public void shouldReturnContextWithEmptySuggestedLemmasWhenMilestoneHasNoLemmaAbsenceLevelDiagnosis(
            ) {
        AuditableQuiz quiz = new AuditableQuiz("A sentence.", List.of(), "quiz-15", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-15", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-15", "T15", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-15", "A1", null);

        SentenceLengthDiagnosis sld = buildDiagnosis(2, 2, 5, CefrLevel.A1, 0);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnoses(sld);

        // milestone has no LemmaAbsenceLevelDiagnosis (empty DefaultLevelDiagnoses)
        DefaultLevelDiagnoses milestoneDiag = new DefaultLevelDiagnoses();

        AuditNode[] tree = buildFullTree("ms-15", milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-15");

        Optional<SentenceLengthCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertTrue(result.get().getSuggestedLemmas().isEmpty());
    }

    @Test
    @DisplayName("should return context with empty suggested lemmas when milestone ancestor is not found")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R004")
    @Tag("F-RCSL-J003")
    public void shouldReturnContextWithEmptySuggestedLemmasWhenMilestoneAncestorIsNotFound() {
        // Build a shallow tree: COURSE → TOPIC → KNOWLEDGE → QUIZ (no MILESTONE level)
        AuditableQuiz quiz = new AuditableQuiz("A sentence.", List.of(), "quiz-16", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-16", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-16", "T16", null);

        SentenceLengthDiagnosis sld = buildDiagnosis(2, 2, 5, CefrLevel.A1, 0);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnoses(sld);

        AuditNode courseNode = buildCourseNode();
        AuditNode topicNode = buildTopicNode(courseNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        buildQuizNode(knowledgeNode, quiz, quizDiag);

        AuditReport report = new AuditReport(courseNode);
        RefinementTask task = buildTask("quiz-16");

        Optional<SentenceLengthCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertTrue(result.get().getSuggestedLemmas().isEmpty());
    }

    @Test
    @DisplayName("should return context with empty suggested lemmas when absent lemmas list is empty")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R004")
    @Tag("F-RCSL-J003")
    public void shouldReturnContextWithEmptySuggestedLemmasWhenAbsentLemmasListIsEmpty() {
        AuditableQuiz quiz = new AuditableQuiz("A sentence.", List.of(), "quiz-17", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-17", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-17", "T17", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-17", "A1", null);

        SentenceLengthDiagnosis sld = buildDiagnosis(2, 2, 5, CefrLevel.A1, 0);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnoses(sld);

        // empty absent lemmas list
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(List.of());

        AuditNode[] tree = buildFullTree("ms-17", milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-17");

        Optional<SentenceLengthCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertTrue(result.get().getSuggestedLemmas().isEmpty());
    }

    @Test
    @DisplayName("should limit suggested lemmas to 10 when more than 10 qualify after filtering")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R005")
    public void shouldLimitSuggestedLemmasTo10WhenMoreThan10QualifyAfterFiltering() {
        AuditableQuiz quiz = new AuditableQuiz("A sentence.", List.of(), "quiz-18", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-18", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-18", "T18", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-18", "A1", null);

        SentenceLengthDiagnosis sld = buildDiagnosis(2, 2, 5, CefrLevel.A1, 0);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnoses(sld);

        // Create 15 qualifying lemmas (COMPLETELY_ABSENT)
        List<AbsentLemma> absentLemmas = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            absentLemmas.add(buildAbsentLemma("word" + i, "NOUN", AbsenceType.COMPLETELY_ABSENT, i * 100));
        }
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(absentLemmas);

        AuditNode[] tree = buildFullTree("ms-18", milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-18");

        Optional<SentenceLengthCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(10, result.get().getSuggestedLemmas().size());
    }

    @Test
    @DisplayName("should resolve context with negative delta for a sentence shorter than target range")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R001")
    @Tag("F-RCSL-J002")
    public void shouldResolveContextWithNegativeDeltaForASentenceShorterThanTargetRange() {
        AuditableQuiz quiz = new AuditableQuiz("Go.", List.of(), "quiz-19", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-19", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-19", "T19", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-19", "A1", null);

        // tokenCount=1, targetMin=5 → delta=-4 (shorter than range)
        SentenceLengthDiagnosis sld = buildDiagnosis(1, 5, 8, CefrLevel.A1, -4);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnoses(sld);
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(List.of());

        AuditNode[] tree = buildFullTree("ms-19", milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-19");

        Optional<SentenceLengthCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(-4, result.get().getDelta());
        Assertions.assertEquals(1, result.get().getTokenCount());
    }

    @Test
    @DisplayName("should resolve context with zero delta when sentence is within target range")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R001")
    public void shouldResolveContextWithZeroDeltaWhenSentenceIsWithinTargetRange() {
        AuditableQuiz quiz = new AuditableQuiz("I like cats.", List.of(), "quiz-20", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-20", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-20", "T20", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-20", "A1", null);

        // tokenCount=3, within [2,5] → delta=0
        SentenceLengthDiagnosis sld = buildDiagnosis(3, 2, 5, CefrLevel.A1, 0);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnoses(sld);
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(List.of());

        AuditNode[] tree = buildFullTree("ms-20", milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-20");

        Optional<SentenceLengthCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(0, result.get().getDelta());
    }

    @Test
    @DisplayName("should set taskId from the RefinementTask id")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R001")
    public void shouldSetTaskIdFromTheRefinementTaskId() {
        AuditableQuiz quiz = new AuditableQuiz("A sentence.", List.of(), "quiz-21", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-21", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-21", "T21", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-21", "A1", null);

        SentenceLengthDiagnosis sld = buildDiagnosis(2, 2, 5, CefrLevel.A1, 0);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnoses(sld);
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(List.of());

        AuditNode[] tree = buildFullTree("ms-21", milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);

        RefinementTask task = new RefinementTask("my-specific-task-id", AuditTarget.QUIZ,
                "quiz-21", "label", DiagnosisKind.SENTENCE_LENGTH, 1, RefinementTaskStatus.PENDING);

        Optional<SentenceLengthCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("my-specific-task-id", result.get().getTaskId());
    }
}
