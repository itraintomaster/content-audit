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

    private DefaultQuizDiagnoses buildQuizDiagnosesWithPlacementAndLength(
            List<MisplacedLemma> misplacedLemmas,
            int tokenCount, int targetMin, int targetMax, int delta) {
        DefaultQuizDiagnoses d = buildQuizDiagnosesWithPlacement(misplacedLemmas);
        SentenceLengthDiagnosis sld = new SentenceLengthDiagnosis(
                tokenCount, targetMin, targetMax, CefrLevel.A1, delta, 0);
        d.setSentenceLengthDiagnosis(sld);
        return d;
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
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-la-1", null, null, "El gato se sentó.", List.of("The cat sat."), null);
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
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-la-2", null, null, "Me encanta aprender.", List.of("I love learning."), null);
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
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-la-3", null, null, null, List.of("He reads."), null);
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
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-la-4", null, null, null, List.of("She sings."), null);
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
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-la-5", null, null, null, List.of("They run fast."), null);
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
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-la-6", null, null, null, List.of("A sentence."), null);
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
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-la-7", null, null, null, List.of("A sentence."), null);
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
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-la-8", null, null, null, List.of("A sentence."), null);
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
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-la-9", null, null, null, List.of("A sentence."), null);
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
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-la-10", null, null, null, List.of("Hello."), null);
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
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-la-11", null, null, null, List.of("Hello."), null);
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

        AuditableQuiz quiz1 = new AuditableQuiz(List.of(), "quiz-la-12a", null, null, null, List.of("First sentence."), null);
        AuditableQuiz quiz2 = new AuditableQuiz(List.of(), "quiz-la-12b", null, null, null, List.of("Second sentence."), null);

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
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-la-13", null, null, null, List.of("A sentence."), null);
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
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-la-14", null, null, null, List.of("A sentence."), null);
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
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-la-15", null, null, null, List.of("A sentence."), null);
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
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-la-16", null, null, null, List.of("A sentence."), null);
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
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-la-17", null, null, null, List.of("A sentence."), null);
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
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-la-18", null, null, null, List.of("A sentence."), null);
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
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-la-19", null, null, null, List.of("A sentence."), null);
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
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-la-20", null, null, null, List.of("A sentence."), null);
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
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-la-21", null, null, null, List.of("A sentence."), null);
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

    // ------------------------------------------------------------------
    // FEAT-RCLAQS tests
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should populate quizSentence on the LEMMA_ABSENCE correction context from the AuditableQuiz carrier")
    @Tag("FEAT-RCLAQS")
    @Tag("F-RCLAQS-R001")
    public void shouldPopulateQuizSentenceOnTheLEMMAABSENCECorrectionContextFromTheAuditableQuizCarrier() {
        // R001: the resolver must copy the quizSentence field from the AuditableQuiz
        // carrier into the LemmaAbsenceCorrectionContext. The field was stamped at
        // mapping time by CourseToAuditableMapper via QuizSentenceConverter.serialize().
        String expectedQuizSentence = "She needs to ____ [negotiate|discuss] (to negotiate) the contract before Friday.";
        AuditableQuiz quiz = new AuditableQuiz(
                List.of(), "quiz-rclaqs-01", null, null,
                "Ella necesita negociar el contrato.",
                List.of("She needs to negotiate the contract."),
                expectedQuizSentence);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Present Simple", "Complete.", true, "know-rclaqs-01", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-rclaqs-01", "Present Simple", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-rclaqs-01", "A1", null);

        MisplacedLemma ml = buildMisplacedLemma("negotiate", "VERB", CefrLevel.B2, CefrLevel.A1, 2840);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of(ml));
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, List.of());

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = new RefinementTask(
                "task-rclaqs-01", AuditTarget.QUIZ, "quiz-rclaqs-01",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        // R001: quizSentence field must be populated from the carrier
        Assertions.assertEquals(expectedQuizSentence, result.get().getQuizSentence());
    }

    @Test
    @DisplayName("should copy AuditableQuiz.quizSentence verbatim without recomputing the DSL in the resolver")
    @Tag("FEAT-RCLAQS")
    @Tag("F-RCLAQS-R002")
    public void shouldCopyAuditableQuizquizSentenceVerbatimWithoutRecomputingTheDSLInTheResolver() {
        // R002: the resolver is a pure read from the carrier — it must NOT recompute the
        // DSL or call any converter. The value must be whatever was stamped on AuditableQuiz
        // at mapping time, copied verbatim. We verify this by stamping a sentinel DSL value
        // and asserting the resolver returns exactly that string, demonstrating no intermediate
        // transformation or recomputation took place.
        String carrierStampedValue = "He ____ [is|'s] (to be) great.";
        AuditableQuiz quiz = new AuditableQuiz(
                List.of(), "quiz-rclaqs-02", null, null,
                "El es genial.",
                List.of("He is great."),
                carrierStampedValue);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "To Be", "Complete.", true, "know-rclaqs-02", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-rclaqs-02", "Grammar", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-rclaqs-02", "A1", null);

        MisplacedLemma ml = buildMisplacedLemma("great", "ADJ", CefrLevel.A2, CefrLevel.A1, 100);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of(ml));
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, List.of());

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = new RefinementTask(
                "task-rclaqs-02", AuditTarget.QUIZ, "quiz-rclaqs-02",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        // R002: resolver must copy verbatim — no recomputation, no converter invocation
        Assertions.assertEquals(carrierStampedValue, result.get().getQuizSentence(),
                "quizSentence must be copied verbatim from AuditableQuiz carrier without recomputation");
    }

    @Test
    @DisplayName("should emit sentence and quizSentence that originate from the same AuditableQuiz so both fields describe the same quiz")
    @Tag("FEAT-RCLAQS")
    @Tag("F-RCLAQS-R003")
    public void shouldEmitSentenceAndQuizSentenceThatOriginateFromTheSameAuditableQuizSoBothFieldsDescribeTheSameQuiz() {
        // R003: both quizSentence and sentence in the produced CorrectionContext must
        // correspond to the same quiz. The resolver reads both from the same AuditableQuiz
        // node; there is no scenario where they could diverge if the carrier is correctly
        // populated. We assert both fields match the same-quiz data.
        String quizSentence = "The cat ____ [sat|sits] (to sit) on the mat.";
        String plainSentence = "The cat sat on the mat.";
        AuditableQuiz quiz = new AuditableQuiz(
                List.of(), "quiz-rclaqs-03", null, null,
                "El gato se sentó en la estera.",
                List.of(plainSentence),
                quizSentence);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Animals", "Complete.", true, "know-rclaqs-03", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-rclaqs-03", "Pets", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-rclaqs-03", "A1", null);

        MisplacedLemma ml = buildMisplacedLemma("mat", "NOUN", CefrLevel.A2, CefrLevel.A1, 500);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of(ml));
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, List.of());

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = new RefinementTask(
                "task-rclaqs-03", AuditTarget.QUIZ, "quiz-rclaqs-03",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        LemmaAbsenceCorrectionContext ctx = result.get();
        // R003: both fields come from the same quiz — they must coexist correctly
        Assertions.assertEquals(plainSentence, ctx.getSentence(),
                "sentence must be the plain sentence from the AuditableQuiz");
        Assertions.assertEquals(quizSentence, ctx.getQuizSentence(),
                "quizSentence must be the DSL stamped on the same AuditableQuiz");
        // The pair (sentence, quizSentence) belongs to the same quiz — verify both are non-null
        Assertions.assertNotNull(ctx.getSentence());
        Assertions.assertNotNull(ctx.getQuizSentence());
    }

    @Test
    @DisplayName("should set taskId from the RefinementTask id")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R003")
    public void shouldSetTaskIdFromTheRefinementTaskId() {
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-la-22", null, null, null, List.of("A sentence."), null);
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

    @Test
    @DisplayName("should populate tokenCount on the correction context from SentenceLengthDiagnosis on the quiz node")
    @Tag("FEAT-RCLALEN")
    @Tag("F-RCLALEN-R001")
    public void shouldPopulateTokenCountOnTheCorrectionContextFromSentenceLengthDiagnosisOnTheQuizNode() {
        // Arrange: quiz node with LemmaPlacementDiagnosis AND SentenceLengthDiagnosis (tokenCount=10)
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-rclalen-01", null, null, "La traduccion.", List.of("The sentence."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-rclalen-01", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-rclalen-01", "T01", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-rclalen-01", "A1", null);

        MisplacedLemma ml = buildMisplacedLemma("negotiate", "VERB", CefrLevel.B2, CefrLevel.A1, 2840);
        // tokenCount=10, targetMin=5, targetMax=8, delta=2 (exceeds range)
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacementAndLength(List.of(ml), 10, 5, 8, 2);
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, List.of());

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-rclalen-01");

        // Act
        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        // Assert: tokenCount must be copied from SentenceLengthDiagnosis
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(Integer.valueOf(10), result.get().getTokenCount());
    }

    @Test
    @DisplayName("should populate targetMin and targetMax on the correction context from SentenceLengthDiagnosis on the quiz node")
    @Tag("FEAT-RCLALEN")
    @Tag("F-RCLALEN-R001")
    public void shouldPopulateTargetMinAndTargetMaxOnTheCorrectionContextFromSentenceLengthDiagnosisOnTheQuizNode() {
        // Arrange: quiz node with SentenceLengthDiagnosis reporting targetMin=5, targetMax=8
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-rclalen-02", null, null, "La traduccion.", List.of("The sentence."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-rclalen-02", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-rclalen-02", "T02", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-rclalen-02", "A1", null);

        MisplacedLemma ml = buildMisplacedLemma("contract", "NOUN", CefrLevel.B1, CefrLevel.A1, 1205);
        // tokenCount=6 is within range [5,8], delta=0
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacementAndLength(List.of(ml), 6, 5, 8, 0);
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, List.of());

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-rclalen-02");

        // Act
        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        // Assert: targetMin and targetMax must be copied from SentenceLengthDiagnosis
        Assertions.assertTrue(result.isPresent());
        LemmaAbsenceCorrectionContext ctx = result.get();
        Assertions.assertEquals(Integer.valueOf(5), ctx.getTargetMin());
        Assertions.assertEquals(Integer.valueOf(8), ctx.getTargetMax());
    }

    @Test
    @DisplayName("should populate delta on the correction context from SentenceLengthDiagnosis on the quiz node")
    @Tag("FEAT-RCLALEN")
    @Tag("F-RCLALEN-R001")
    public void shouldPopulateDeltaOnTheCorrectionContextFromSentenceLengthDiagnosisOnTheQuizNode() {
        // Arrange: SentenceLengthDiagnosis with delta=-2 (sentence too short)
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-rclalen-03", null, null, "La oracion.", List.of("The sentence."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-rclalen-03", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-rclalen-03", "T03", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-rclalen-03", "A1", null);

        MisplacedLemma ml = buildMisplacedLemma("like", "VERB", CefrLevel.A2, CefrLevel.A1, 52);
        // tokenCount=3, targetMin=5, targetMax=8, delta=-2 (below range)
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacementAndLength(List.of(ml), 3, 5, 8, -2);
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, List.of());

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-rclalen-03");

        // Act
        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        // Assert: delta must be copied from SentenceLengthDiagnosis
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(Integer.valueOf(-2), result.get().getDelta());
    }

    @Test
    @DisplayName("should populate lengthDirection on the correction context as a non-null enum value derived by the resolver")
    @Tag("FEAT-RCLALEN")
    @Tag("F-RCLALEN-R001")
    public void shouldPopulateLengthDirectionOnTheCorrectionContextAsANonnullEnumValueDerivedByTheResolver() {
        // Arrange: any valid SentenceLengthDiagnosis — verifies that lengthDirection is always set (non-null)
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-rclalen-04", null, null, "La oracion.", List.of("The sentence."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-rclalen-04", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-rclalen-04", "T04", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-rclalen-04", "A1", null);

        MisplacedLemma ml = buildMisplacedLemma("see", "VERB", CefrLevel.A2, CefrLevel.A1, 75);
        // delta=2 (exceeds range) -> SHORTEN
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacementAndLength(List.of(ml), 10, 5, 8, 2);
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, List.of());

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-rclalen-04");

        // Act
        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        // Assert: lengthDirection is always non-null and is a valid LengthDirection value
        Assertions.assertTrue(result.isPresent());
        LengthDirection lengthDirection = result.get().getLengthDirection();
        Assertions.assertNotNull(lengthDirection);
        Assertions.assertEquals(LengthDirection.SHORTEN, lengthDirection);
    }

    @Test
    @DisplayName("should set lengthDirection to SHORTEN when delta is greater than zero")
    @Tag("FEAT-RCLALEN")
    @Tag("F-RCLALEN-R002")
    public void shouldSetLengthDirectionToSHORTENWhenDeltaIsGreaterThanZero() {
        // Arrange: sentence has 10 tokens in range [5,8], so delta=+2 (exceeds max)
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-rclalen-05", null, null, "La oracion larga.", List.of("The longer sentence today."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-rclalen-05", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-rclalen-05", "T05", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-rclalen-05", "A1", null);

        MisplacedLemma ml = buildMisplacedLemma("negotiate", "VERB", CefrLevel.B2, CefrLevel.A1, 2840);
        // delta=2 > 0 => SHORTEN
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacementAndLength(List.of(ml), 10, 5, 8, 2);
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, List.of());

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-rclalen-05");

        // Act
        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        // Assert: delta > 0 maps to SHORTEN
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(LengthDirection.SHORTEN, result.get().getLengthDirection());
    }

    @Test
    @DisplayName("should set lengthDirection to LENGTHEN when delta is less than zero")
    @Tag("FEAT-RCLALEN")
    @Tag("F-RCLALEN-R002")
    public void shouldSetLengthDirectionToLENGTHENWhenDeltaIsLessThanZero() {
        // Arrange: sentence has 3 tokens in range [5,8], so delta=-2 (below min)
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-rclalen-06", null, null, "La oracion.", List.of("Short sentence."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-rclalen-06", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-rclalen-06", "T06", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-rclalen-06", "A1", null);

        MisplacedLemma ml = buildMisplacedLemma("walk", "VERB", CefrLevel.A2, CefrLevel.A1, 300);
        // delta=-2 < 0 => LENGTHEN
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacementAndLength(List.of(ml), 3, 5, 8, -2);
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, List.of());

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-rclalen-06");

        // Act
        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        // Assert: delta < 0 maps to LENGTHEN
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(LengthDirection.LENGTHEN, result.get().getLengthDirection());
    }

    @Test
    @DisplayName("should set lengthDirection to KEEP_SAME when delta is exactly zero")
    @Tag("FEAT-RCLALEN")
    @Tag("F-RCLALEN-R002")
    public void shouldSetLengthDirectionToKEEPSAMEWhenDeltaIsExactlyZero() {
        // Arrange: sentence has 6 tokens in range [5,8], so delta=0 (within range)
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-rclalen-07", null, null, "La oracion exacta.", List.of("She loves the book."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-rclalen-07", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-rclalen-07", "T07", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-rclalen-07", "A1", null);

        MisplacedLemma ml = buildMisplacedLemma("book", "NOUN", CefrLevel.A2, CefrLevel.A1, 100);
        // tokenCount=6, targetMin=5, targetMax=8, delta=0 => KEEP_SAME
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacementAndLength(List.of(ml), 6, 5, 8, 0);
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, List.of());

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-rclalen-07");

        // Act
        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        // Assert: delta == 0 maps to KEEP_SAME
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(LengthDirection.KEEP_SAME, result.get().getLengthDirection());
    }

    @Test
    @DisplayName("should set lengthDirection to UNKNOWN when SentenceLengthDiagnosis is not available on the quiz node")
    @Tag("FEAT-RCLALEN")
    @Tag("F-RCLALEN-R002")
    public void shouldSetLengthDirectionToUNKNOWNWhenSentenceLengthDiagnosisIsNotAvailableOnTheQuizNode() {
        // Arrange: quiz node with LemmaPlacementDiagnosis but NO SentenceLengthDiagnosis
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-rclalen-08", null, null, "La oracion.", List.of("The sentence."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-rclalen-08", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-rclalen-08", "T08", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-rclalen-08", "A1", null);

        MisplacedLemma ml = buildMisplacedLemma("sit", "VERB", CefrLevel.A2, CefrLevel.A1, 400);
        // No SentenceLengthDiagnosis on the quiz node
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of(ml));
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, List.of());

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-rclalen-08");

        // Act
        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        // Assert: absent SentenceLengthDiagnosis => UNKNOWN is the sole discriminator
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(LengthDirection.UNKNOWN, result.get().getLengthDirection());
    }

    @Test
    @DisplayName("should read SentenceLengthDiagnosis from the same quiz node already used to obtain LemmaPlacementDiagnosis")
    @Tag("FEAT-RCLALEN")
    @Tag("F-RCLALEN-R003")
    public void shouldReadSentenceLengthDiagnosisFromTheSameQuizNodeAlreadyUsedToObtainLemmaPlacementDiagnosis() {
        // Arrange: two quiz nodes in the same tree. Only the target quiz carries SentenceLengthDiagnosis.
        // The other quiz has placement but no length — if the resolver used the wrong node, it would
        // return UNKNOWN instead of SHORTEN for the target quiz.
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Mixed", "Do it.", true, "know-rclalen-09", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-rclalen-09", "T09", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-rclalen-09", "A1", null);

        AuditableQuiz quiz1 = new AuditableQuiz(List.of(), "quiz-rclalen-09a", null, null, "Otra oracion.", List.of("Another sentence."), null);
        AuditableQuiz quiz2 = new AuditableQuiz(List.of(), "quiz-rclalen-09b", null, null, "La oracion objetivo.", List.of("The target sentence with extra words."), null);

        MisplacedLemma ml1 = buildMisplacedLemma("other", "ADJ", CefrLevel.A2, CefrLevel.A1, 600);
        MisplacedLemma ml2 = buildMisplacedLemma("extra", "ADJ", CefrLevel.B1, CefrLevel.A1, 800);

        // quiz1: has placement but NO SentenceLengthDiagnosis
        DefaultQuizDiagnoses diag1 = buildQuizDiagnosesWithPlacement(List.of(ml1));
        // quiz2 (target): has both placement AND SentenceLengthDiagnosis with delta=3 => SHORTEN
        DefaultQuizDiagnoses diag2 = buildQuizDiagnosesWithPlacementAndLength(List.of(ml2), 11, 5, 8, 3);

        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, List.of());

        AuditNode courseNode = buildCourseNode();
        AuditNode milestoneNode = buildMilestoneNode(courseNode, milestone, milestoneDiag);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        buildQuizNode(knowledgeNode, quiz1, diag1);
        buildQuizNode(knowledgeNode, quiz2, diag2);

        AuditReport report = new AuditReport(courseNode);
        RefinementTask task = buildTask("quiz-rclalen-09b");

        // Act
        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        // Assert: SentenceLengthDiagnosis was read from quiz-rclalen-09b (same node as LemmaPlacementDiagnosis)
        Assertions.assertTrue(result.isPresent());
        LemmaAbsenceCorrectionContext ctx = result.get();
        Assertions.assertEquals(LengthDirection.SHORTEN, ctx.getLengthDirection());
        Assertions.assertEquals(Integer.valueOf(11), ctx.getTokenCount());
        Assertions.assertEquals(Integer.valueOf(3), ctx.getDelta());
    }

    @Test
    @DisplayName("should not introduce any new traversal of the audit tree to read SentenceLengthDiagnosis")
    @Tag("FEAT-RCLALEN")
    @Tag("F-RCLALEN-R003")
    public void shouldNotIntroduceAnyNewTraversalOfTheAuditTreeToReadSentenceLengthDiagnosis() {
        // Arrange: two quiz nodes under the same knowledge. The task targets quiz-rclalen-10b.
        // quiz-rclalen-10a has a SentenceLengthDiagnosis with delta=5 (SHORTEN).
        // quiz-rclalen-10b has its own SentenceLengthDiagnosis with delta=-1 (LENGTHEN).
        // If the resolver traversed the tree a second time (or used the wrong node), it could
        // pick up the wrong SentenceLengthDiagnosis. The assertion checks that the result
        // reflects the diagnosis from the node identified by the task's nodeId.
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Grammar", "Complete.", true, "know-rclalen-10", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-rclalen-10", "T10", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-rclalen-10", "A1", null);

        AuditableQuiz quiz1 = new AuditableQuiz(List.of(), "quiz-rclalen-10a", null, null, "Primera.", List.of("First sentence longer than allowed."), null);
        AuditableQuiz quiz2 = new AuditableQuiz(List.of(), "quiz-rclalen-10b", null, null, "Segunda.", List.of("Short."), null);

        MisplacedLemma ml1 = buildMisplacedLemma("longer", "ADJ", CefrLevel.B1, CefrLevel.A1, 700);
        MisplacedLemma ml2 = buildMisplacedLemma("short", "ADJ", CefrLevel.A2, CefrLevel.A1, 300);

        // quiz1: delta=5 => SHORTEN (wrong node for the task)
        DefaultQuizDiagnoses diag1 = buildQuizDiagnosesWithPlacementAndLength(List.of(ml1), 13, 5, 8, 5);
        // quiz2 (target): delta=-1 => LENGTHEN
        DefaultQuizDiagnoses diag2 = buildQuizDiagnosesWithPlacementAndLength(List.of(ml2), 4, 5, 8, -1);

        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, List.of());

        AuditNode courseNode = buildCourseNode();
        AuditNode milestoneNode = buildMilestoneNode(courseNode, milestone, milestoneDiag);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        buildQuizNode(knowledgeNode, quiz1, diag1);
        buildQuizNode(knowledgeNode, quiz2, diag2);

        AuditReport report = new AuditReport(courseNode);
        RefinementTask task = buildTask("quiz-rclalen-10b");

        // Act
        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        // Assert: values come from quiz-rclalen-10b, not from quiz-rclalen-10a
        Assertions.assertTrue(result.isPresent());
        LemmaAbsenceCorrectionContext ctx = result.get();
        Assertions.assertEquals(LengthDirection.LENGTHEN, ctx.getLengthDirection());
        Assertions.assertEquals(Integer.valueOf(4), ctx.getTokenCount());
        Assertions.assertEquals(Integer.valueOf(-1), ctx.getDelta());
    }

    @Test
    @DisplayName("should still produce a correction context when LemmaPlacementDiagnosis is present but SentenceLengthDiagnosis is absent on the quiz node")
    @Tag("FEAT-RCLALEN")
    @Tag("F-RCLALEN-R004")
    public void shouldStillProduceACorrectionContextWhenLemmaPlacementDiagnosisIsPresentButSentenceLengthDiagnosisIsAbsentOnTheQuizNode() {
        // Arrange: quiz node has LemmaPlacementDiagnosis but NO SentenceLengthDiagnosis
        // (e.g. quiz excluded from sentence-length analyzer by F-DSLEN-R002)
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-rclalen-11", null, null, "La oracion sin longitud.", List.of("The sentence without length."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-rclalen-11", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-rclalen-11", "T11", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-rclalen-11", "A1", null);

        MisplacedLemma ml = buildMisplacedLemma("length", "NOUN", CefrLevel.B1, CefrLevel.A1, 900);
        // Only LemmaPlacementDiagnosis — no SentenceLengthDiagnosis set
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of(ml));
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A1, List.of());

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-rclalen-11");

        // Act
        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        // Assert: resolver must not fail — it returns a present context with UNKNOWN as the discriminator
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(LengthDirection.UNKNOWN, result.get().getLengthDirection());
    }

    @Test
    @DisplayName("should leave non-length fields of the correction context populated normally when SentenceLengthDiagnosis is absent")
    @Tag("FEAT-RCLALEN")
    @Tag("F-RCLALEN-R004")
    public void shouldLeaveNonlengthFieldsOfTheCorrectionContextPopulatedNormallyWhenSentenceLengthDiagnosisIsAbsent() {
        // Arrange: quiz node with LemmaPlacementDiagnosis but NO SentenceLengthDiagnosis.
        // The non-length fields (sentence, translation, knowledgeTitle, etc.) must be populated
        // normally as per F-RCLA-R003 — length absence is an enricher absence, not a context failure.
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-rclalen-12", null, null, "Ella canta.", List.of("She sings."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Music Theory", "Complete the blank.", true, "know-rclalen-12", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-rclalen-12", "Arts", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-rclalen-12", "A2", null);

        MisplacedLemma ml = buildMisplacedLemma("sing", "VERB", CefrLevel.B1, CefrLevel.A2, 450);
        // Only LemmaPlacementDiagnosis — no SentenceLengthDiagnosis
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of(ml));
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnoses(CefrLevel.A2, List.of());

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = buildTask("quiz-rclalen-12");

        // Act
        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        // Assert: all non-length fields are populated normally despite absent SentenceLengthDiagnosis
        Assertions.assertTrue(result.isPresent());
        LemmaAbsenceCorrectionContext ctx = result.get();
        Assertions.assertEquals("She sings.", ctx.getSentence());
        Assertions.assertEquals("Ella canta.", ctx.getTranslation());
        Assertions.assertEquals("Music Theory", ctx.getKnowledgeTitle());
        Assertions.assertEquals("Complete the blank.", ctx.getKnowledgeInstructions());
        Assertions.assertEquals("Arts", ctx.getTopicLabel());
        Assertions.assertEquals(CefrLevel.A2, ctx.getCefrLevel());
        Assertions.assertNotNull(ctx.getMisplacedLemmas());
        Assertions.assertFalse(ctx.getMisplacedLemmas().isEmpty());
        // Length fields: discriminator is UNKNOWN, other length fields are placeholder (not asserted here per TECH_SPEC)
        Assertions.assertEquals(LengthDirection.UNKNOWN, ctx.getLengthDirection());
    }
}
