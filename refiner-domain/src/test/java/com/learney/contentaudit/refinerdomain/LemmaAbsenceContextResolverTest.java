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
import com.learney.contentaudit.auditdomain.labs.AbsenceType;
import com.learney.contentaudit.auditdomain.labs.AbsentLemma;
import com.learney.contentaudit.auditdomain.labs.LemmaAbsenceLevelDiagnosis;
import com.learney.contentaudit.auditdomain.labs.LemmaAndPos;
import com.learney.contentaudit.auditdomain.labs.LemmaPlacementDiagnosis;
import com.learney.contentaudit.auditdomain.labs.MisplacedLemma;
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

public class LemmaAbsenceContextResolverTest {

    private LemmaAbsenceContextResolver sut;

    @BeforeEach
    void setUp() {
        sut = new LemmaAbsenceContextResolver();
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

    private AuditNode[] buildFullTree(AuditableMilestone milestone,
            DefaultLevelDiagnoses milestoneDiagnoses,
            AuditableTopic topic,
            AuditableKnowledge knowledge,
            AuditableQuiz quiz,
            DefaultQuizDiagnoses quizDiagnoses) {
        AuditNode courseNode = buildCourseNode();
        AuditNode milestoneNode = buildMilestoneNode(courseNode, milestone, milestoneDiagnoses);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        AuditNode quizNode = buildQuizNode(knowledgeNode, quiz, quizDiagnoses);
        return new AuditNode[]{courseNode, milestoneNode, topicNode, knowledgeNode, quizNode};
    }

    private RefinementTask buildTask(String quizId) {
        return new RefinementTask(
                "task-la-1",
                AuditTarget.QUIZ,
                quizId,
                "Quiz label",
                DiagnosisKind.LEMMA_ABSENCE,
                1,
                RefinementTaskStatus.PENDING);
    }

    private DefaultQuizDiagnoses buildQuizDiagnosesWithPlacement(
            List<MisplacedLemma> misplacedLemmas) {
        DefaultQuizDiagnoses d = new DefaultQuizDiagnoses();
        d.setLemmaAbsenceDiagnosis(new LemmaPlacementDiagnosis(misplacedLemmas.size(), misplacedLemmas));
        return d;
    }

    private DefaultQuizDiagnoses buildQuizDiagnosesNoPlacement() {
        return new DefaultQuizDiagnoses();
    }

    private DefaultLevelDiagnoses buildMilestoneDiagnoses(CefrLevel level,
            List<AbsentLemma> absentLemmas) {
        LemmaAbsenceLevelDiagnosis absenceDiagnosis = new LemmaAbsenceLevelDiagnosis(
                level, 100, absentLemmas.size(), 10.0, 90.0,
                0.8, 0.7, 0.5, null, absentLemmas, List.of(), 2, 3, 5);
        DefaultLevelDiagnoses diagnoses = new DefaultLevelDiagnoses();
        diagnoses.setLemmaAbsenceDiagnosis(absenceDiagnosis);
        return diagnoses;
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

    private MisplacedLemma buildMisplacedLemma(String lemma, String pos, CefrLevel expectedLevel,
            CefrLevel foundInLevel, int cocaRank) {
        return new MisplacedLemma(
                new LemmaAndPos(lemma, pos),
                expectedLevel,
                foundInLevel,
                AbsenceType.APPEARS_TOO_LATE,
                cocaRank,
                null);
    }

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should resolve context with all fields populated from quiz diagnosis and ancestor entities")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R003")
    public void shouldResolveContextWithAllFieldsPopulatedFromQuizDiagnosisAndAncestorEntities() {
        AuditableQuiz quiz = new AuditableQuiz("The cat sat.", List.of(), "quiz-la-1", null, null, "El gato se sentó.");
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Animals", "Complete the sentence.", true, "know-la-1", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-la-1", "Basic Animals", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-la-1", "A1", null);

        MisplacedLemma ml = buildMisplacedLemma("cat", "NOUN", CefrLevel.A2, CefrLevel.A1, 300);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of(ml));
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, List.of());

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-la-1");

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        LemmaAbsenceCorrectionContext ctx = result.get();
        Assertions.assertEquals("task-la-1", ctx.getTaskId());
        Assertions.assertEquals("The cat sat.", ctx.getSentence());
        Assertions.assertEquals("El gato se sentó.", ctx.getTranslation());
        Assertions.assertEquals("Animals", ctx.getKnowledgeTitle());
        Assertions.assertEquals("Complete the sentence.", ctx.getKnowledgeInstructions());
        Assertions.assertEquals("Basic Animals", ctx.getTopicLabel());
        Assertions.assertNotNull(ctx.getMisplacedLemmas());
        Assertions.assertNotNull(ctx.getSuggestedLemmas());
    }

    @Test
    @DisplayName("should populate sentence and translation from AuditableQuiz entity on the quiz node")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R003")
    public void shouldPopulateSentenceAndTranslationFromAuditableQuizEntityOnTheQuizNode() {
        AuditableQuiz quiz = new AuditableQuiz("I love learning.", List.of(), "quiz-la-2", null, null, "Me encanta aprender.");
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Feelings", "Complete.", true, "know-la-2", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-la-2", "Emotions", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-la-2", "A1", null);

        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of());
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, List.of());

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-la-2");

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("I love learning.", result.get().getSentence());
        Assertions.assertEquals("Me encanta aprender.", result.get().getTranslation());
    }

    @Test
    @DisplayName("should populate knowledgeTitle and knowledgeInstructions from AuditableKnowledge on knowledge ancestor")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R003")
    public void shouldPopulateKnowledgeTitleAndKnowledgeInstructionsFromAuditableKnowledgeOnKnowledgeAncestor(
            ) {
        AuditableQuiz quiz = new AuditableQuiz("He reads.", List.of(), "quiz-la-3", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Reading Skills", "Choose the correct word.", true, "know-la-3", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-la-3", "Skills", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-la-3", "A2", null);

        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of());
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A2, List.of());

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-la-3");

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("Reading Skills", result.get().getKnowledgeTitle());
        Assertions.assertEquals("Choose the correct word.", result.get().getKnowledgeInstructions());
    }

    @Test
    @DisplayName("should populate topicLabel from AuditableTopic on topic ancestor")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R003")
    public void shouldPopulateTopicLabelFromAuditableTopicOnTopicAncestor() {
        AuditableQuiz quiz = new AuditableQuiz("She sings.", List.of(), "quiz-la-4", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Music", "Complete.", true, "know-la-4", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-la-4", "Arts and Culture", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-la-4", "B1", null);

        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of());
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.B1, List.of());

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-la-4");

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("Arts and Culture", result.get().getTopicLabel());
    }

    @Test
    @DisplayName("should populate cefrLevel from milestone ancestor")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R003")
    public void shouldPopulateCefrLevelFromMilestoneAncestor() {
        AuditableQuiz quiz = new AuditableQuiz("They run fast.", List.of(), "quiz-la-5", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Sports", "Complete.", true, "know-la-5", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-la-5", "Activities", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-la-5", "B2", null);

        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of());
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.B2, List.of());

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-la-5");

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(CefrLevel.B2, result.get().getCefrLevel());
    }

    @Test
    @DisplayName("should populate misplacedLemmas from LemmaPlacementDiagnosis on quiz node")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R004")
    public void shouldPopulateMisplacedLemmasFromLemmaPlacementDiagnosisOnQuizNode() {
        AuditableQuiz quiz = new AuditableQuiz("A sentence.", List.of(), "quiz-la-6", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-6", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-la-6", "T6", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-la-6", "A1", null);

        MisplacedLemma ml1 = buildMisplacedLemma("dog", "NOUN", CefrLevel.A2, CefrLevel.A1, 200);
        MisplacedLemma ml2 = buildMisplacedLemma("run", "VERB", CefrLevel.A2, CefrLevel.A1, 400);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of(ml1, ml2));
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, List.of());

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-la-6");

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        List<MisplacedLemmaContext> misplacedLemmas = result.get().getMisplacedLemmas();
        Assertions.assertNotNull(misplacedLemmas);
        Assertions.assertEquals(2, misplacedLemmas.size());
    }

    @Test
    @DisplayName("should map MisplacedLemma fields to MisplacedLemmaContext fields correctly")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R004")
    public void shouldMapMisplacedLemmaFieldsToMisplacedLemmaContextFieldsCorrectly() {
        AuditableQuiz quiz = new AuditableQuiz("A sentence.", List.of(), "quiz-la-7", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-7", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-la-7", "T7", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-la-7", "A1", null);

        MisplacedLemma ml = buildMisplacedLemma("house", "NOUN", CefrLevel.A2, CefrLevel.A1, 350);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of(ml));
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, List.of());

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-la-7");

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        MisplacedLemmaContext ctx = result.get().getMisplacedLemmas().get(0);
        Assertions.assertEquals("house", ctx.getLemma());
        Assertions.assertEquals("NOUN", ctx.getPos());
        Assertions.assertEquals(350, ctx.getCocaRank());
    }

    @Test
    @DisplayName("should include expectedLevel and quizLevel in each MisplacedLemmaContext entry")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R004")
    public void shouldIncludeExpectedLevelAndQuizLevelInEachMisplacedLemmaContextEntry() {
        AuditableQuiz quiz = new AuditableQuiz("A sentence.", List.of(), "quiz-la-8", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-8", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-la-8", "T8", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-la-8", "A1", null);

        // expectedLevel=A2, foundInLevel=A1 (the quiz's CEFR level from milestone)
        MisplacedLemma ml = buildMisplacedLemma("book", "NOUN", CefrLevel.A2, CefrLevel.A1, 100);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of(ml));
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, List.of());

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-la-8");

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        MisplacedLemmaContext ctx = result.get().getMisplacedLemmas().get(0);
        Assertions.assertEquals(CefrLevel.A2, ctx.getExpectedLevel());
        // quizLevel comes from milestone's cefrLevel (A1 in this case)
        Assertions.assertEquals(CefrLevel.A1, ctx.getQuizLevel());
    }

    @Test
    @DisplayName("should include cocaRank as null in MisplacedLemmaContext when not available")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R004")
    public void shouldIncludeCocaRankAsNullInMisplacedLemmaContextWhenNotAvailable() {
        AuditableQuiz quiz = new AuditableQuiz("A sentence.", List.of(), "quiz-la-9", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-9", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-la-9", "T9", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-la-9", "A1", null);

        // cocaRank=0 means not available (sentinel value for primitive int)
        MisplacedLemma ml = buildMisplacedLemma("unknown", "NOUN", CefrLevel.A2, CefrLevel.A1, 0);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of(ml));
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, List.of());

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-la-9");

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        MisplacedLemmaContext ctx = result.get().getMisplacedLemmas().get(0);
        // cocaRank 0 means not available
        Assertions.assertEquals(0, ctx.getCocaRank());
    }

    @Test
    @DisplayName("should return empty when quiz node is not found in the audit tree")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R005")
    public void shouldReturnEmptyWhenQuizNodeIsNotFoundInTheAuditTree() {
        AuditableQuiz quiz = new AuditableQuiz("Hello.", List.of(), "quiz-la-10", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete.", true, "know-la-10", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-la-10", "Social", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-la-10", "A1", null);

        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of());
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, List.of());

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("nonexistent-quiz-id");

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should return empty when task nodeTarget does not match any node target in the tree")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R005")
    public void shouldReturnEmptyWhenTaskNodeTargetDoesNotMatchAnyNodeTargetInTheTree() {
        AuditableQuiz quiz = new AuditableQuiz("Hello.", List.of(), "quiz-la-11", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-11", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-la-11", "T11", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-la-11", "A1", null);

        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of());
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, List.of());

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);

        // task has target KNOWLEDGE but id is the quiz's id — won't match QUIZ target
        RefinementTask task = new RefinementTask("task-la-11", AuditTarget.KNOWLEDGE, "quiz-la-11",
                "label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should locate the correct quiz node when multiple quiz nodes exist in the tree")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R005")
    public void shouldLocateTheCorrectQuizNodeWhenMultipleQuizNodesExistInTheTree() {
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Mixed", "Do it.", true, "know-la-12", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-la-12", "Topic 12", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-la-12", "A1", null);

        AuditableQuiz quiz1 = new AuditableQuiz("First sentence.", List.of(), "quiz-la-12a", null, null, null);
        AuditableQuiz quiz2 = new AuditableQuiz("Second sentence.", List.of(), "quiz-la-12b", null, null, null);

        MisplacedLemma ml = buildMisplacedLemma("tree", "NOUN", CefrLevel.A2, CefrLevel.A1, 600);
        DefaultQuizDiagnoses diag1 = buildQuizDiagnosesWithPlacement(List.of());
        DefaultQuizDiagnoses diag2 = buildQuizDiagnosesWithPlacement(List.of(ml));

        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, List.of());

        AuditNode courseNode = buildCourseNode();
        AuditNode milestoneNode = buildMilestoneNode(courseNode, milestone, milestoneDiag);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        buildQuizNode(knowledgeNode, quiz1, diag1);
        buildQuizNode(knowledgeNode, quiz2, diag2);

        AuditReport report = new AuditReport(courseNode);
        RefinementTask task = buildTask("quiz-la-12b");

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("Second sentence.", result.get().getSentence());
        Assertions.assertEquals(1, result.get().getMisplacedLemmas().size());
    }

    @Test
    @DisplayName("should return empty when quiz node has no LemmaPlacementDiagnosis")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R006")
    public void shouldReturnEmptyWhenQuizNodeHasNoLemmaPlacementDiagnosis() {
        AuditableQuiz quiz = new AuditableQuiz("A sentence.", List.of(), "quiz-la-13", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-13", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-la-13", "T13", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-la-13", "A1", null);

        // Quiz has no LemmaPlacementDiagnosis
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesNoPlacement();
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, List.of());

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-la-13");

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should include only COMPLETELY_ABSENT and APPEARS_TOO_LATE lemmas in suggestedLemmas and exclude APPEARS_TOO_EARLY")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R004b")
    public void shouldIncludeOnlyCOMPLETELYABSENTAndAPPEARSTOOLATELemmasInSuggestedLemmasAndExcludeAPPEARSTOOEARLY(
            ) {
        AuditableQuiz quiz = new AuditableQuiz("A sentence.", List.of(), "quiz-la-14", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-14", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-la-14", "T14", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-la-14", "A1", null);

        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of());
        List<AbsentLemma> absentLemmas = List.of(
                buildAbsentLemma("walk", "VERB", AbsenceType.COMPLETELY_ABSENT, 100),
                buildAbsentLemma("run", "VERB", AbsenceType.APPEARS_TOO_LATE, 200),
                buildAbsentLemma("fly", "VERB", AbsenceType.APPEARS_TOO_EARLY, 300));
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, absentLemmas);

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-la-14");

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();
        Assertions.assertEquals(2, lemmas.size());
        Assertions.assertTrue(lemmas.stream().noneMatch(l -> l.getReason().equals("APPEARS_TOO_EARLY")));
        Assertions.assertTrue(lemmas.stream().anyMatch(l -> l.getLemma().equals("walk")));
        Assertions.assertTrue(lemmas.stream().anyMatch(l -> l.getLemma().equals("run")));
    }

    @Test
    @DisplayName("should order suggested lemmas by COCA rank ascending with lowest rank first")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R004b")
    public void shouldOrderSuggestedLemmasByCOCARankAscendingWithLowestRankFirst() {
        AuditableQuiz quiz = new AuditableQuiz("A sentence.", List.of(), "quiz-la-15", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-15", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-la-15", "T15", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-la-15", "A1", null);

        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of());
        List<AbsentLemma> absentLemmas = List.of(
                buildAbsentLemma("dog", "NOUN", AbsenceType.COMPLETELY_ABSENT, 500),
                buildAbsentLemma("cat", "NOUN", AbsenceType.COMPLETELY_ABSENT, 200),
                buildAbsentLemma("bird", "NOUN", AbsenceType.APPEARS_TOO_LATE, 350));
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, absentLemmas);

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-la-15");

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();
        Assertions.assertEquals(3, lemmas.size());
        Assertions.assertEquals("cat", lemmas.get(0).getLemma());   // rank 200
        Assertions.assertEquals("bird", lemmas.get(1).getLemma());  // rank 350
        Assertions.assertEquals("dog", lemmas.get(2).getLemma());   // rank 500
    }

    @Test
    @DisplayName("should place lemmas without COCA rank after lemmas with COCA rank in suggestedLemmas")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R004b")
    public void shouldPlaceLemmasWithoutCOCARankAfterLemmasWithCOCARankInSuggestedLemmas() {
        AuditableQuiz quiz = new AuditableQuiz("A sentence.", List.of(), "quiz-la-16", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-16", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-la-16", "T16", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-la-16", "A1", null);

        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of());
        // cocaRank=0 means no rank
        List<AbsentLemma> absentLemmas = List.of(
                buildAbsentLemma("unknown", "NOUN", AbsenceType.COMPLETELY_ABSENT, 0),
                buildAbsentLemma("table", "NOUN", AbsenceType.COMPLETELY_ABSENT, 150),
                buildAbsentLemma("chair", "NOUN", AbsenceType.APPEARS_TOO_LATE, 0));
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, absentLemmas);

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-la-16");

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();
        Assertions.assertEquals(3, lemmas.size());
        Assertions.assertEquals("table", lemmas.get(0).getLemma());
        Assertions.assertTrue(lemmas.get(1).getCocaRank() == 0 || lemmas.get(2).getCocaRank() == 0);
    }

    @Test
    @DisplayName("should map AbsentLemma fields to SuggestedLemma fields correctly")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R004b")
    public void shouldMapAbsentLemmaFieldsToSuggestedLemmaFieldsCorrectly() {
        AuditableQuiz quiz = new AuditableQuiz("A sentence.", List.of(), "quiz-la-17", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-17", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-la-17", "T17", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-la-17", "A1", null);

        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of());
        List<AbsentLemma> absentLemmas = List.of(
                buildAbsentLemma("house", "NOUN", AbsenceType.COMPLETELY_ABSENT, 250));
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, absentLemmas);

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-la-17");

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

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
    @DisplayName("should limit suggested lemmas to 10 when more than 10 qualify after filtering")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R004b")
    public void shouldLimitSuggestedLemmasTo10WhenMoreThan10QualifyAfterFiltering() {
        AuditableQuiz quiz = new AuditableQuiz("A sentence.", List.of(), "quiz-la-18", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-18", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-la-18", "T18", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-la-18", "A1", null);

        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of());
        List<AbsentLemma> absentLemmas = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            absentLemmas.add(buildAbsentLemma("word" + i, "NOUN", AbsenceType.COMPLETELY_ABSENT, i * 100));
        }
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, absentLemmas);

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-la-18");

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(10, result.get().getSuggestedLemmas().size());
    }

    @Test
    @DisplayName("should return context with empty suggested lemmas when milestone has no LemmaAbsenceLevelDiagnosis")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R004c")
    public void shouldReturnContextWithEmptySuggestedLemmasWhenMilestoneHasNoLemmaAbsenceLevelDiagnosis(
            ) {
        AuditableQuiz quiz = new AuditableQuiz("A sentence.", List.of(), "quiz-la-19", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-19", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-la-19", "T19", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-la-19", "A1", null);

        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of());
        // Milestone has no LemmaAbsenceLevelDiagnosis
        DefaultLevelDiagnoses milestoneDiag = new DefaultLevelDiagnoses();

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-la-19");

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertTrue(result.get().getSuggestedLemmas().isEmpty());
    }

    @Test
    @DisplayName("should return context with empty suggested lemmas when milestone ancestor is not found")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R004c")
    public void shouldReturnContextWithEmptySuggestedLemmasWhenMilestoneAncestorIsNotFound() {
        // Build a shallow tree: COURSE → TOPIC → KNOWLEDGE → QUIZ (no MILESTONE level)
        AuditableQuiz quiz = new AuditableQuiz("A sentence.", List.of(), "quiz-la-20", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-20", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-la-20", "T20", null);

        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of());

        AuditNode courseNode = buildCourseNode();
        AuditNode topicNode = buildTopicNode(courseNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        buildQuizNode(knowledgeNode, quiz, quizDiag);

        AuditReport report = new AuditReport(courseNode);
        RefinementTask task = buildTask("quiz-la-20");

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertTrue(result.get().getSuggestedLemmas().isEmpty());
    }

    @Test
    @DisplayName("should return context with empty suggested lemmas when all absent lemmas are APPEARS_TOO_EARLY")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R004c")
    public void shouldReturnContextWithEmptySuggestedLemmasWhenAllAbsentLemmasAreAPPEARSTOOEARLY() {
        AuditableQuiz quiz = new AuditableQuiz("A sentence.", List.of(), "quiz-la-21", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-21", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-la-21", "T21", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-la-21", "A1", null);

        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of());
        List<AbsentLemma> absentLemmas = List.of(
                buildAbsentLemma("advanced", "ADJ", AbsenceType.APPEARS_TOO_EARLY, 1000),
                buildAbsentLemma("complex", "ADJ", AbsenceType.APPEARS_TOO_EARLY, 2000));
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, absentLemmas);

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-la-21");

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertTrue(result.get().getSuggestedLemmas().isEmpty());
    }

    @Test
    @DisplayName("should set taskId from the RefinementTask id")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R003")
    public void shouldSetTaskIdFromTheRefinementTaskId() {
        AuditableQuiz quiz = new AuditableQuiz("A sentence.", List.of(), "quiz-la-22", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-22", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-la-22", "T22", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-la-22", "A1", null);

        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of());
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, List.of());

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);

        RefinementTask task = new RefinementTask("my-specific-task-id-la", AuditTarget.QUIZ,
                "quiz-la-22", "label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("my-specific-task-id-la", result.get().getTaskId());
    }
}
