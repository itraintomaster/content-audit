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
import com.learney.contentaudit.auditdomain.lemmacount.LemmaCountLevelDiagnosis;
import com.learney.contentaudit.auditdomain.lemmacount.LemmaCountStats;
import com.learney.contentaudit.auditdomain.lemmacount.LevelLemmaCountResult;
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Animals", "Complete the sentence.", true, "know-la-1", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Feelings", "Complete.", true, "know-la-2", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Reading Skills", "Choose the correct word.", true, "know-la-3", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Music", "Complete.", true, "know-la-4", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Sports", "Complete.", true, "know-la-5", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-6", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-7", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-8", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-9", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete.", true, "know-la-10", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-11", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Mixed", "Do it.", true, "know-la-12", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-13", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-14", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-15", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-16", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-17", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-18", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-19", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-20", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-21", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Present Simple", "Complete.", true, "know-rclaqs-01", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "To Be", "Complete.", true, "know-rclaqs-02", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Animals", "Complete.", true, "know-rclaqs-03", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-la-22", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-rclalen-01", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-rclalen-02", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-rclalen-03", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-rclalen-04", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-rclalen-05", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-rclalen-06", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-rclalen-07", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-rclalen-08", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Mixed", "Do it.", true, "know-rclalen-09", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Grammar", "Complete.", true, "know-rclalen-10", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "D.", true, "know-rclalen-11", null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Music Theory", "Complete the blank.", true, "know-rclalen-12", null, null, null);
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

    @Test
    @DisplayName("should populate suggestedLemmas with lemmaCount lemmaCountThreshold and isUnderexposed fields when the audit report carries lemma-count diagnoses for the resolved level")
    @Tag("FEAT-LEMMA-SUGGESTIONS")
    @Tag("F-SLEM-R002")
    public void shouldPopulateSuggestedLemmasWithLemmaCountLemmaCountThresholdAndIsUnderexposedFieldsWhenTheAuditReportCarriesLemmacountDiagnosesForTheResolvedLevel() {
        // Arrange: a quiz at A1 with one APPEARS_TOO_LATE absent lemma "lemma-la".
        // The audit report carries lemma-count signal for A1 with count=2, threshold=5.
        // Expected: suggestedLemmas has one entry with lemmaCount=2, lemmaCountThreshold=5, isUnderexposed=true.

        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-r002a", null, null, "El sol.", List.of("The sun."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Nature", "Fill.", true, "know-r002a", null, null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-r002a", "Environment", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-r002a", "A1", null);

        AbsentLemma absentLate = new AbsentLemma(
                new LemmaAndPos("lemma-la", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 300, null);

        LemmaAbsenceLevelDiagnosis absenceDiag = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 100, 1, 10.0, 90.0,
                0.8, 0.7, 0.5, null,
                List.of(absentLate), List.of(), 2, 3, 5);

        // count=2, threshold=5 → underexposed (2 < 5)
        LemmaCountStats stats = new LemmaCountStats(
                new LemmaAndPos("lemma-la", "NOUN"), 2, 0.4, Optional.of(CefrLevel.A1));
        LevelLemmaCountResult levelResult = new LevelLemmaCountResult(
                CefrLevel.A1, 0.4, 1, List.of(stats));
        LemmaCountLevelDiagnosis lemmaCountDiag = new LemmaCountLevelDiagnosis(levelResult);

        DefaultLevelDiagnoses milestoneDiag = new DefaultLevelDiagnoses();
        milestoneDiag.setLemmaAbsenceDiagnosis(absenceDiag);
        milestoneDiag.setLemmaCountDiagnosis(lemmaCountDiag);

        MisplacedLemma ml = buildMisplacedLemma("sun", "NOUN", CefrLevel.A2, CefrLevel.A1, 150);
        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz,
                buildQuizDiagnosesWithPlacement(List.of(ml)));
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = new RefinementTask(
                "task-r002a", AuditTarget.QUIZ, "quiz-r002a",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        // Act
        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        // Assert: R002 — lemmaCount, lemmaCountThreshold, isUnderexposed are populated
        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();
        Assertions.assertFalse(lemmas.isEmpty(), "suggestedLemmas must not be empty");

        SuggestedLemma sl = lemmas.stream()
                .filter(l -> "lemma-la".equals(l.getLemma()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("lemma-la must be in suggestedLemmas"));

        Assertions.assertNotNull(sl.getLemmaCount(),
                "lemmaCount must be populated when lemma-count signal is available (R002)");
        Assertions.assertEquals(2, sl.getLemmaCount(),
                "lemmaCount must match observed count from LemmaCountStats");
        Assertions.assertNotNull(sl.getLemmaCountThreshold(),
                "lemmaCountThreshold must be populated when lemma-count signal is available (R002)");
        Assertions.assertEquals(5, sl.getLemmaCountThreshold(),
                "lemmaCountThreshold must match the threshold from the level result");
        Assertions.assertNotNull(sl.getIsUnderexposed(),
                "isUnderexposed must be populated when lemma-count signal is available (R002)");
        Assertions.assertTrue(sl.getIsUnderexposed(),
                "isUnderexposed must be true when count(2) < threshold(5)");
    }

    @Test
    @DisplayName("should include in suggestedLemmas a lemma flagged as underexposed by lemma-count even when that lemma has no LEMMA_ABSENCE diagnosis on the quiz node")
    @Tag("FEAT-LEMMA-SUGGESTIONS")
    @Tag("F-SLEM-R002")
    public void shouldIncludeInSuggestedLemmasALemmaFlaggedAsUnderexposedByLemmacountEvenWhenThatLemmaHasNoLEMMAABSENCEDiagnosisOnTheQuizNode() {
        // Arrange: quiz at A1 has one APPEARS_TOO_LATE absent lemma ("lemma-with-absence").
        // The lemma-count signal at A1 also includes "lemma-pure-under" with count=1 < threshold=3,
        // but "lemma-pure-under" has NO entry in LemmaAbsenceLevelDiagnosis.
        // R002: the list must include "lemma-pure-under" even though it has no LEMMA_ABSENCE diagnosis.

        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-r002b", null, null, "La luna.", List.of("The moon."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Space", "Fill.", true, "know-r002b", null, null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-r002b", "Universe", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-r002b", "A1", null);

        AbsentLemma absentWithAbsence = new AbsentLemma(
                new LemmaAndPos("lemma-with-absence", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 200, null);

        LemmaAbsenceLevelDiagnosis absenceDiag = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 100, 1, 10.0, 90.0,
                0.8, 0.7, 0.5, null,
                List.of(absentWithAbsence), List.of(), 2, 3, 5);

        // "lemma-pure-under" has NO LEMMA_ABSENCE but IS underexposed (count=1 < threshold=3)
        LemmaCountStats statsWithAbsence = new LemmaCountStats(
                new LemmaAndPos("lemma-with-absence", "NOUN"), 1, 0.33, Optional.of(CefrLevel.A1));
        LemmaCountStats statsPureUnder = new LemmaCountStats(
                new LemmaAndPos("lemma-pure-under", "NOUN"), 1, 0.33, Optional.of(CefrLevel.A1));

        LevelLemmaCountResult levelResult = new LevelLemmaCountResult(
                CefrLevel.A1, 0.33, 2, List.of(statsWithAbsence, statsPureUnder));
        LemmaCountLevelDiagnosis lemmaCountDiag = new LemmaCountLevelDiagnosis(levelResult);

        DefaultLevelDiagnoses milestoneDiag = new DefaultLevelDiagnoses();
        milestoneDiag.setLemmaAbsenceDiagnosis(absenceDiag);
        milestoneDiag.setLemmaCountDiagnosis(lemmaCountDiag);

        MisplacedLemma ml = buildMisplacedLemma("moon", "NOUN", CefrLevel.A2, CefrLevel.A1, 250);
        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz,
                buildQuizDiagnosesWithPlacement(List.of(ml)));
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = new RefinementTask(
                "task-r002b", AuditTarget.QUIZ, "quiz-r002b",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        // Act
        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        // Assert: R002 — "lemma-pure-under" must appear in suggestedLemmas even without LEMMA_ABSENCE
        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();

        boolean pureUnderPresent = lemmas.stream()
                .anyMatch(l -> "lemma-pure-under".equals(l.getLemma()));
        Assertions.assertTrue(pureUnderPresent,
                "lemma-pure-under must appear in suggestedLemmas based on lemma-count underexposure alone (R002) — no LEMMA_ABSENCE required");

        // R002: the pure-underexposed lemma must also have the triple informed
        SuggestedLemma pureUnder = lemmas.stream()
                .filter(l -> "lemma-pure-under".equals(l.getLemma()))
                .findFirst()
                .orElseThrow();
        Assertions.assertNotNull(pureUnder.getLemmaCount());
        Assertions.assertNotNull(pureUnder.getLemmaCountThreshold());
        Assertions.assertNotNull(pureUnder.getIsUnderexposed());
        Assertions.assertTrue(pureUnder.getIsUnderexposed());
    }

    @Test
    @DisplayName("should rank suggestedLemmas in the priority order group1 underexposed APPEARS_TOO_LATE then group2 underexposed APPEARS_TOO_EARLY then group3 underexposed without LEMMA_ABSENCE then group4 underexposed other LEMMA_ABSENCE then group5 LEMMA_ABSENCE without underexposure with COMPLETELY_ABSENT last")
    @Tag("FEAT-LEMMA-SUGGESTIONS")
    @Tag("F-SLEM-R003")
    public void shouldRankSuggestedLemmasInThePriorityOrderGroup1UnderexposedAPPEARSTOOLATEThenGroup2UnderexposedAPPEARSTOOEARLYThenGroup3UnderexposedWithoutLEMMAABSENCEThenGroup4UnderexposedOtherLEMMAABSENCEThenGroup5LEMMAABSENCEWithoutUnderexposureWithCOMPLETELYABSENTLast() {
        // Arrange: threshold = 3 (any count < 3 is underexposed)
        //
        // COCA ranks are deliberately shuffled across groups to prove group priority
        // overrides COCA rank ordering:
        //   - Group 1 ("lemma-g1-late"):  APPEARS_TOO_LATE + underexposed (count=1)  cocaRank=800
        //   - Group 2 ("lemma-g2-early"): APPEARS_TOO_EARLY + underexposed at A2 (count=1) cocaRank=200
        //   - Group 3 ("lemma-g3-pure"):  no LEMMA_ABSENCE + underexposed (count=2)  cocaRank=500
        //   - Group 5a ("lemma-g5a-late"): APPEARS_TOO_LATE, NOT underexposed (count=5) cocaRank=100
        //   - Group 5b ("lemma-g5b-absent"): COMPLETELY_ABSENT (R007: triple null)   cocaRank=50
        //
        // Expected order: g1-late → g2-early → g3-pure → g5a-late → g5b-absent

        int threshold = 3;

        // Quiz is in level A1
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-r003", null, null, "La traduccion.", List.of("The sentence."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Vocab", "Complete.", true, "know-r003", null, null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-r003", "Topic R003", null);
        AuditableMilestone milestoneA1 = new AuditableMilestone(List.of(), "ms-r003-a1", "A1", null);
        AuditableMilestone milestoneA2 = new AuditableMilestone(List.of(), "ms-r003-a2", "A2", null);

        // Absent lemmas at A1 level (from LemmaAbsenceLevelDiagnosis):
        // g1: APPEARS_TOO_LATE + underexposed → Group 1 (cocaRank=800 to prove group beats COCA)
        AbsentLemma absentG1 = new AbsentLemma(
                new LemmaAndPos("lemma-g1-late", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 800, null);
        // g2: APPEARS_TOO_EARLY → target level is A2; count at A2=1 < 3 → Group 2 (cocaRank=200)
        AbsentLemma absentG2 = new AbsentLemma(
                new LemmaAndPos("lemma-g2-early", "NOUN"), CefrLevel.A2,
                AbsenceType.APPEARS_TOO_EARLY, List.of(CefrLevel.A1), PriorityLevel.HIGH, 200, null);
        // g5a: APPEARS_TOO_LATE + NOT underexposed (count=5 >= 3) → Group 5 (cocaRank=100)
        AbsentLemma absentG5a = new AbsentLemma(
                new LemmaAndPos("lemma-g5a-late", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 100, null);
        // g5b: COMPLETELY_ABSENT → always last, triple null (R007) (cocaRank=50)
        AbsentLemma absentG5b = new AbsentLemma(
                new LemmaAndPos("lemma-g5b-absent", "NOUN"), CefrLevel.A1,
                AbsenceType.COMPLETELY_ABSENT, List.of(), PriorityLevel.HIGH, 50, null);

        LemmaAbsenceLevelDiagnosis absenceDiagA1 = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 100, 4, 10.0, 90.0,
                0.8, 0.7, 0.5, null,
                List.of(absentG1, absentG2, absentG5a, absentG5b),
                List.of(), 2, 3, 5);

        // LemmaCountLevelDiagnosis at A1:
        // g1-late: count=1 < 3 (underexposed)
        // g3-pure: count=2 < 3 (underexposed, no LEMMA_ABSENCE)
        // g5a-late: count=5 >= 3 (NOT underexposed)
        LemmaCountStats statsG1 = new LemmaCountStats(new LemmaAndPos("lemma-g1-late", "NOUN"), 1, 0.33, Optional.of(CefrLevel.A1));
        LemmaCountStats statsG3 = new LemmaCountStats(new LemmaAndPos("lemma-g3-pure", "NOUN"), 2, 0.67, Optional.of(CefrLevel.A1));
        LemmaCountStats statsG5a = new LemmaCountStats(new LemmaAndPos("lemma-g5a-late", "NOUN"), 5, 1.0, Optional.of(CefrLevel.A1));

        LevelLemmaCountResult levelResultA1 = new LevelLemmaCountResult(
                CefrLevel.A1, 0.5, 3, List.of(statsG1, statsG3, statsG5a));
        LemmaCountLevelDiagnosis lemmaCountDiagA1 = new LemmaCountLevelDiagnosis(levelResultA1);

        DefaultLevelDiagnoses milestoneDiagA1 = new DefaultLevelDiagnoses();
        milestoneDiagA1.setLemmaAbsenceDiagnosis(absenceDiagA1);
        milestoneDiagA1.setLemmaCountDiagnosis(lemmaCountDiagA1);

        // LemmaCountLevelDiagnosis at A2:
        // g2-early: count=1 < 3 at A2 (underexposed at target level — R004)
        LemmaCountStats statsG2atA2 = new LemmaCountStats(new LemmaAndPos("lemma-g2-early", "NOUN"), 1, 0.33, Optional.of(CefrLevel.A2));
        LevelLemmaCountResult levelResultA2 = new LevelLemmaCountResult(
                CefrLevel.A2, 0.33, 1, List.of(statsG2atA2));
        LemmaCountLevelDiagnosis lemmaCountDiagA2 = new LemmaCountLevelDiagnosis(levelResultA2);

        DefaultLevelDiagnoses milestoneDiagA2 = new DefaultLevelDiagnoses();
        milestoneDiagA2.setLemmaCountDiagnosis(lemmaCountDiagA2);

        // Build tree: COURSE → [MILESTONE(A1) → TOPIC → KNOWLEDGE → QUIZ, MILESTONE(A2)]
        AuditNode courseNode = buildCourseNode();
        AuditNode milestoneNodeA1 = buildMilestoneNode(courseNode, milestoneA1, milestoneDiagA1);
        AuditNode topicNode = buildTopicNode(milestoneNodeA1, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        MisplacedLemma ml = buildMisplacedLemma("walk", "VERB", CefrLevel.A2, CefrLevel.A1, 300);
        DefaultQuizDiagnoses quizDiag = buildQuizDiagnosesWithPlacement(List.of(ml));
        buildQuizNode(knowledgeNode, quiz, quizDiag);
        // A2 milestone (sibling) carries lemma-count for target-level evaluation (R004)
        buildMilestoneNode(courseNode, milestoneA2, milestoneDiagA2);

        AuditReport report = new AuditReport(courseNode);
        RefinementTask task = new RefinementTask(
                "task-r003", AuditTarget.QUIZ, "quiz-r003",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        // Act
        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        // Assert: present and has the right number of lemmas (5 distinct entries)
        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();
        Assertions.assertEquals(5, lemmas.size(),
                "Expected 5 suggested lemmas covering all priority groups");

        // Group 1 (priority 1): underexposed APPEARS_TOO_LATE
        Assertions.assertEquals("lemma-g1-late", lemmas.get(0).getLemma(),
                "Group 1 (underexposed APPEARS_TOO_LATE) must be first regardless of high cocaRank");

        // Group 2 (priority 2): underexposed APPEARS_TOO_EARLY at target level
        Assertions.assertEquals("lemma-g2-early", lemmas.get(1).getLemma(),
                "Group 2 (underexposed APPEARS_TOO_EARLY at target level) must be second");

        // Group 3 (priority 3): underexposed without LEMMA_ABSENCE
        Assertions.assertEquals("lemma-g3-pure", lemmas.get(2).getLemma(),
                "Group 3 (underexposed, no LEMMA_ABSENCE) must be third");

        // Group 5a (priority 5): APPEARS_TOO_LATE without underexposure
        Assertions.assertEquals("lemma-g5a-late", lemmas.get(3).getLemma(),
                "Group 5a (APPEARS_TOO_LATE, not underexposed) must be fourth despite lowest cocaRank");

        // Group 5b (priority 5, last): COMPLETELY_ABSENT (R007)
        Assertions.assertEquals("lemma-g5b-absent", lemmas.get(4).getLemma(),
                "Group 5b (COMPLETELY_ABSENT) must be last (R007)");

        // R007 + R013: COMPLETELY_ABSENT triple must be all null (not applicable)
        Assertions.assertNull(lemmas.get(4).getLemmaCount(),
                "COMPLETELY_ABSENT lemmaCount must be null (R007 + R013)");
        Assertions.assertNull(lemmas.get(4).getLemmaCountThreshold(),
                "COMPLETELY_ABSENT lemmaCountThreshold must be null (R007 + R013)");
        Assertions.assertNull(lemmas.get(4).getIsUnderexposed(),
                "COMPLETELY_ABSENT isUnderexposed must be null (R007 + R013)");

        // R013: underexposed lemmas (g1, g2, g3) must have the triple all informed and consistent
        SuggestedLemma g1 = lemmas.get(0);
        Assertions.assertNotNull(g1.getLemmaCount(), "Group 1 lemmaCount must be informed");
        Assertions.assertNotNull(g1.getLemmaCountThreshold(), "Group 1 lemmaCountThreshold must be informed");
        Assertions.assertNotNull(g1.getIsUnderexposed(), "Group 1 isUnderexposed must be informed");
        Assertions.assertTrue(g1.getIsUnderexposed(),
                "Group 1 isUnderexposed must be true (count=1 < threshold=3)");
    }

    @Test
    @DisplayName("should evaluate APPEARS_TOO_EARLY underexposure against the lema's real target level not against the level where the lema currently appears")
    @Tag("FEAT-LEMMA-SUGGESTIONS")
    @Tag("F-SLEM-R004")
    public void shouldEvaluateAPPEARSTOOEARLYUnderexposureAgainstTheLemasRealTargetLevelNotAgainstTheLevelWhereTheLemaCurrentlyAppears() {
        // Arrange: quiz is at A1 (current level). Lemma "lemma-early" has APPEARS_TOO_EARLY
        // meaning it appears at A1 but its real target (expected) level is A2.
        //
        // At A1: count=5 >= threshold=3 → NOT underexposed (if evaluated at wrong level)
        // At A2: count=1 < threshold=3 → underexposed (correct target level)
        //
        // R004 says: underexposure must be evaluated at the TARGET level (A2), not the current (A1).
        // Expected: the lemma is classified as underexposed (Group 2 — underexposed APPEARS_TOO_EARLY)
        // because the A2 signal shows count=1 < 3.

        int threshold = 3;

        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-r004", null, null, "El libro.", List.of("The book."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Education", "Fill.", true, "know-r004", null, null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-r004", "School", null);
        AuditableMilestone milestoneA1 = new AuditableMilestone(List.of(), "ms-r004-a1", "A1", null);
        AuditableMilestone milestoneA2 = new AuditableMilestone(List.of(), "ms-r004-a2", "A2", null);

        // "lemma-early": APPEARS_TOO_EARLY at A1, expectedLevel=A2 → target is A2
        AbsentLemma absentEarly = new AbsentLemma(
                new LemmaAndPos("lemma-early", "NOUN"), CefrLevel.A2,
                AbsenceType.APPEARS_TOO_EARLY, List.of(CefrLevel.A1), PriorityLevel.HIGH, 400, null);

        LemmaAbsenceLevelDiagnosis absenceDiagA1 = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 100, 1, 10.0, 90.0,
                0.8, 0.7, 0.5, null,
                List.of(absentEarly),
                List.of(), 2, 3, 5);

        // A1 lemma-count: count=5 >= threshold=3 → not underexposed at A1 (wrong level)
        LemmaCountStats statsAtA1 = new LemmaCountStats(
                new LemmaAndPos("lemma-early", "NOUN"), 5, 1.0, Optional.of(CefrLevel.A1));
        LevelLemmaCountResult levelResultA1 = new LevelLemmaCountResult(
                CefrLevel.A1, 1.0, 1, List.of(statsAtA1));
        LemmaCountLevelDiagnosis lemmaCountDiagA1 = new LemmaCountLevelDiagnosis(levelResultA1);

        DefaultLevelDiagnoses milestoneDiagA1 = new DefaultLevelDiagnoses();
        milestoneDiagA1.setLemmaAbsenceDiagnosis(absenceDiagA1);
        milestoneDiagA1.setLemmaCountDiagnosis(lemmaCountDiagA1);

        // A2 lemma-count: count=1 < threshold=3 → underexposed at A2 (correct target level)
        LemmaCountStats statsAtA2 = new LemmaCountStats(
                new LemmaAndPos("lemma-early", "NOUN"), 1, 0.33, Optional.of(CefrLevel.A2));
        LevelLemmaCountResult levelResultA2 = new LevelLemmaCountResult(
                CefrLevel.A2, 0.33, 1, List.of(statsAtA2));
        LemmaCountLevelDiagnosis lemmaCountDiagA2 = new LemmaCountLevelDiagnosis(levelResultA2);

        DefaultLevelDiagnoses milestoneDiagA2 = new DefaultLevelDiagnoses();
        milestoneDiagA2.setLemmaCountDiagnosis(lemmaCountDiagA2);

        // Build tree: COURSE → [MILESTONE(A1) → TOPIC → KNOWLEDGE → QUIZ, MILESTONE(A2)]
        AuditNode courseNode = buildCourseNode();
        AuditNode milestoneNodeA1 = buildMilestoneNode(courseNode, milestoneA1, milestoneDiagA1);
        AuditNode topicNode = buildTopicNode(milestoneNodeA1, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        MisplacedLemma ml = buildMisplacedLemma("book", "NOUN", CefrLevel.A2, CefrLevel.A1, 200);
        buildQuizNode(knowledgeNode, quiz, buildQuizDiagnosesWithPlacement(List.of(ml)));
        buildMilestoneNode(courseNode, milestoneA2, milestoneDiagA2);

        AuditReport report = new AuditReport(courseNode);
        RefinementTask task = new RefinementTask(
                "task-r004", AuditTarget.QUIZ, "quiz-r004",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        // Act
        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        // Assert
        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();
        Assertions.assertEquals(1, lemmas.size());

        SuggestedLemma sl = lemmas.get(0);
        Assertions.assertEquals("lemma-early", sl.getLemma());

        // R004: evaluated at A2 (target level) → count=1 < threshold=3 → underexposed
        Assertions.assertNotNull(sl.getIsUnderexposed(), "isUnderexposed must be informed (A2 signal available)");
        Assertions.assertTrue(sl.getIsUnderexposed(),
                "lemma-early must be underexposed at A2 (count=1 < threshold=3) — must evaluate at target level (R004)");
        Assertions.assertEquals(1, sl.getLemmaCount(),
                "lemmaCount must reflect count at A2 target level (count=1), not A1 current level (count=5)");
    }

    @Test
    @DisplayName("should break ties within the same priority group by ordering suggestedLemmas by ascending cocaRank")
    @Tag("FEAT-LEMMA-SUGGESTIONS")
    @Tag("F-SLEM-R005")
    public void shouldBreakTiesWithinTheSamePriorityGroupByOrderingSuggestedLemmasByAscendingCocaRank() {
        // Arrange: 3 lemmas all in the same priority group (Group 1: underexposed APPEARS_TOO_LATE)
        // with different cocaRanks. They should be ordered by COCA ascending within the group.
        // cocaRanks: 500, 100, 300 → expected order: 100, 300, 500

        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-r005a", null, null, "Ella corre.", List.of("She runs."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Sports", "Complete.", true, "know-r005a", null, null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-r005a", "Activities", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-r005a", "A1", null);

        // All three are APPEARS_TOO_LATE + underexposed (count=1 < threshold=3) → same group 1
        AbsentLemma absent1 = new AbsentLemma(
                new LemmaAndPos("lemma-coca-500", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 500, null);
        AbsentLemma absent2 = new AbsentLemma(
                new LemmaAndPos("lemma-coca-100", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 100, null);
        AbsentLemma absent3 = new AbsentLemma(
                new LemmaAndPos("lemma-coca-300", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 300, null);

        LemmaAbsenceLevelDiagnosis absenceDiag = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 100, 3, 10.0, 90.0,
                0.8, 0.7, 0.5, null,
                List.of(absent1, absent2, absent3),
                List.of(), 2, 3, 5);

        // All three underexposed (count=1 < threshold=3)
        LemmaCountStats stats1 = new LemmaCountStats(new LemmaAndPos("lemma-coca-500", "NOUN"), 1, 0.33, Optional.of(CefrLevel.A1));
        LemmaCountStats stats2 = new LemmaCountStats(new LemmaAndPos("lemma-coca-100", "NOUN"), 1, 0.33, Optional.of(CefrLevel.A1));
        LemmaCountStats stats3 = new LemmaCountStats(new LemmaAndPos("lemma-coca-300", "NOUN"), 1, 0.33, Optional.of(CefrLevel.A1));

        LevelLemmaCountResult levelResult = new LevelLemmaCountResult(
                CefrLevel.A1, 0.33, 3, List.of(stats1, stats2, stats3));
        LemmaCountLevelDiagnosis lemmaCountDiag = new LemmaCountLevelDiagnosis(levelResult);

        DefaultLevelDiagnoses milestoneDiag = new DefaultLevelDiagnoses();
        milestoneDiag.setLemmaAbsenceDiagnosis(absenceDiag);
        milestoneDiag.setLemmaCountDiagnosis(lemmaCountDiag);

        MisplacedLemma ml = buildMisplacedLemma("run", "VERB", CefrLevel.A2, CefrLevel.A1, 300);
        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz,
                buildQuizDiagnosesWithPlacement(List.of(ml)));
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = new RefinementTask(
                "task-r005a", AuditTarget.QUIZ, "quiz-r005a",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        // Act
        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        // Assert: all in group 1, ordered by COCA ascending
        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();
        Assertions.assertEquals(3, lemmas.size());
        Assertions.assertEquals("lemma-coca-100", lemmas.get(0).getLemma(),
                "Lowest cocaRank (100) must be first within the same priority group");
        Assertions.assertEquals("lemma-coca-300", lemmas.get(1).getLemma(),
                "Middle cocaRank (300) must be second");
        Assertions.assertEquals("lemma-coca-500", lemmas.get(2).getLemma(),
                "Highest cocaRank (500) must be third");
    }

    @Test
    @DisplayName("should not use exposure deficit magnitude as tiebreaker within the same priority group leaving COCA ascending as the only intra-group order")
    @Tag("FEAT-LEMMA-SUGGESTIONS")
    @Tag("F-SLEM-R005")
    public void shouldNotUseExposureDeficitMagnitudeAsTiebreakerWithinTheSamePriorityGroupLeavingCOCAAscendingAsTheOnlyIntragroupOrder() {
        // Arrange: 3 lemmas all in Group 1 (underexposed APPEARS_TOO_LATE) with the same cocaRank (250)
        // but different deficit magnitudes (count=1, count=0, count=2 — deficits of 2, 3, 1 against threshold=3).
        // Expected: order is stable by COCA (all equal) — deficit must NOT determine the order.
        // We verify that the first and last elements are not sorted by deficit.
        // Since COCA is equal, any stable order is acceptable; the rule forbids deficit-based reordering.

        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-r005b", null, null, "El gato.", List.of("The cat."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Animals", "Fill.", true, "know-r005b", null, null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-r005b", "Nature", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-r005b", "A1", null);

        // All same cocaRank=250, all APPEARS_TOO_LATE + underexposed — different counts (deficit magnitudes)
        AbsentLemma absent1 = new AbsentLemma(
                new LemmaAndPos("lemma-def2", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 250, null);
        AbsentLemma absent2 = new AbsentLemma(
                new LemmaAndPos("lemma-def3", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 250, null);
        AbsentLemma absent3 = new AbsentLemma(
                new LemmaAndPos("lemma-def1", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 250, null);

        LemmaAbsenceLevelDiagnosis absenceDiag = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 100, 3, 10.0, 90.0,
                0.8, 0.7, 0.5, null,
                List.of(absent1, absent2, absent3),
                List.of(), 2, 3, 5);

        // Counts: lemma-def2=1 (deficit 2), lemma-def3=0 (deficit 3), lemma-def1=2 (deficit 1)
        // threshold=3, all underexposed
        LemmaCountStats stats1 = new LemmaCountStats(new LemmaAndPos("lemma-def2", "NOUN"), 1, 0.33, Optional.of(CefrLevel.A1));
        LemmaCountStats stats2 = new LemmaCountStats(new LemmaAndPos("lemma-def3", "NOUN"), 0, 0.0, Optional.of(CefrLevel.A1));
        LemmaCountStats stats3 = new LemmaCountStats(new LemmaAndPos("lemma-def1", "NOUN"), 2, 0.67, Optional.of(CefrLevel.A1));

        LevelLemmaCountResult levelResult = new LevelLemmaCountResult(
                CefrLevel.A1, 0.33, 3, List.of(stats1, stats2, stats3));
        LemmaCountLevelDiagnosis lemmaCountDiag = new LemmaCountLevelDiagnosis(levelResult);

        DefaultLevelDiagnoses milestoneDiag = new DefaultLevelDiagnoses();
        milestoneDiag.setLemmaAbsenceDiagnosis(absenceDiag);
        milestoneDiag.setLemmaCountDiagnosis(lemmaCountDiag);

        MisplacedLemma ml = buildMisplacedLemma("cat", "NOUN", CefrLevel.A2, CefrLevel.A1, 200);
        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz,
                buildQuizDiagnosesWithPlacement(List.of(ml)));
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = new RefinementTask(
                "task-r005b", AuditTarget.QUIZ, "quiz-r005b",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        // Act
        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        // Assert: all 3 lemmas present (all underexposed, all in group 1)
        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();
        Assertions.assertEquals(3, lemmas.size(), "All 3 underexposed APPEARS_TOO_LATE lemmas must be included");

        // R005: deficit must NOT determine order; all have same cocaRank → any order is stable
        // The critical check: if deficit were used, lemma-def3 (deficit=3) would be first.
        // R005 forbids that. Instead, since COCA is equal, stable insertion order is acceptable.
        // We verify the 3 lemmas are all present (order is indeterminate but deficit-driven would place lemma-def3 first)
        List<String> names = lemmas.stream().map(SuggestedLemma::getLemma).toList();
        Assertions.assertTrue(names.contains("lemma-def2"), "lemma-def2 must be present");
        Assertions.assertTrue(names.contains("lemma-def3"), "lemma-def3 must be present");
        Assertions.assertTrue(names.contains("lemma-def1"), "lemma-def1 must be present");
        // R005 key assertion: lemma-def3 (highest deficit=3) must NOT be first if COCA is equal
        // (if it were, it would indicate deficit-based ordering, which R005 forbids)
        // Since all COCA ranks are equal (250), the tiebreaker is NOT deficit — lemma-def3 must NOT jump to front
        // In a deficit-first approach, order would be: def3, def2, def1 (deficit 3 > 2 > 1)
        // The rule says COCA ascending is the ONLY tiebreaker — with equal COCA, stable order (not deficit)
        Assertions.assertNotEquals("lemma-def3", lemmas.get(0).getLemma(),
                "lemma-def3 (highest deficit magnitude) must NOT be first — deficit is not a tiebreaker (R005)");
    }

    @Test
    @DisplayName("should apply the existing suggestedLemmas size limit only after the lemma-count ranking has been computed so the cap never increases the maximum observable list size")
    @Tag("FEAT-LEMMA-SUGGESTIONS")
    @Tag("F-SLEM-R006")
    public void shouldApplyTheExistingSuggestedLemmasSizeLimitOnlyAfterTheLemmacountRankingHasBeenComputedSoTheCapNeverIncreasesTheMaximumObservableListSize() {
        // Arrange: 15 lemmas all COMPLETELY_ABSENT (group 5) plus 3 lemmas that are underexposed
        // APPEARS_TOO_LATE (group 1). After ranking: group 1 entries come first.
        // After applying the cap (10 max), only the top 10 by priority should remain.
        // The underexposed group-1 lemmas must be kept; the rest come from group 5.
        // This verifies: (1) cap does not exceed 10, (2) cap is applied after ranking so
        // the 3 group-1 lemmas are preserved over group-5 lemmas.

        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-r006", null, null, "El perro.", List.of("The dog."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Animals", "Fill.", true, "know-r006", null, null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-r006", "Nature", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-r006", "A1", null);

        List<AbsentLemma> absentLemmas = new ArrayList<>();
        // 3 underexposed APPEARS_TOO_LATE → group 1 (priority highest)
        for (int i = 1; i <= 3; i++) {
            absentLemmas.add(new AbsentLemma(
                    new LemmaAndPos("lemma-under-" + i, "NOUN"), CefrLevel.A1,
                    AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, i * 100, null));
        }
        // 15 COMPLETELY_ABSENT → group 5 (lowest priority)
        for (int i = 1; i <= 15; i++) {
            absentLemmas.add(new AbsentLemma(
                    new LemmaAndPos("lemma-absent-" + i, "NOUN"), CefrLevel.A1,
                    AbsenceType.COMPLETELY_ABSENT, List.of(), PriorityLevel.HIGH, i * 50, null));
        }

        LemmaAbsenceLevelDiagnosis absenceDiag = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 100, 18, 10.0, 90.0,
                0.8, 0.7, 0.5, null,
                absentLemmas, List.of(), 2, 3, 5);

        // lemma-under-1, -2, -3 all underexposed (count=1 < threshold=3)
        List<LemmaCountStats> statsList = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            statsList.add(new LemmaCountStats(
                    new LemmaAndPos("lemma-under-" + i, "NOUN"), 1, 0.33, Optional.of(CefrLevel.A1)));
        }
        LevelLemmaCountResult levelResult = new LevelLemmaCountResult(
                CefrLevel.A1, 0.33, 3, statsList);
        LemmaCountLevelDiagnosis lemmaCountDiag = new LemmaCountLevelDiagnosis(levelResult);

        DefaultLevelDiagnoses milestoneDiag = new DefaultLevelDiagnoses();
        milestoneDiag.setLemmaAbsenceDiagnosis(absenceDiag);
        milestoneDiag.setLemmaCountDiagnosis(lemmaCountDiag);

        MisplacedLemma ml = buildMisplacedLemma("dog", "NOUN", CefrLevel.A2, CefrLevel.A1, 200);
        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz,
                buildQuizDiagnosesWithPlacement(List.of(ml)));
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = new RefinementTask(
                "task-r006", AuditTarget.QUIZ, "quiz-r006",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        // Act
        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        // Assert
        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();

        // R006: cap must still be 10 (not increased by lemma-count ranking)
        Assertions.assertEquals(10, lemmas.size(),
                "suggestedLemmas must be capped at 10 after lemma-count ranking (R006)");

        // R006: cap applied AFTER ranking — the 3 underexposed lemmas must be in the top 10
        // (if cap were applied before ranking, they might be cut out by the 10-limit)
        List<String> names = lemmas.stream().map(SuggestedLemma::getLemma).toList();
        Assertions.assertTrue(names.contains("lemma-under-1"),
                "Underexposed group-1 lemma must survive the cap (cap applied after ranking)");
        Assertions.assertTrue(names.contains("lemma-under-2"),
                "Underexposed group-1 lemma must survive the cap (cap applied after ranking)");
        Assertions.assertTrue(names.contains("lemma-under-3"),
                "Underexposed group-1 lemma must survive the cap (cap applied after ranking)");
    }

    @Test
    @DisplayName("should place every COMPLETELY_ABSENT lemma behind every already-introduced underexposed lemma in suggestedLemmas regardless of cocaRank")
    @Tag("FEAT-LEMMA-SUGGESTIONS")
    @Tag("F-SLEM-R007")
    public void shouldPlaceEveryCOMPLETELYABSENTLemmaBehindEveryAlreadyintroducedUnderexposedLemmaInSuggestedLemmasRegardlessOfCocaRank() {
        // Arrange:
        // - "lemma-absent-low" COMPLETELY_ABSENT, cocaRank=50  (very low COCA — should still be last)
        // - "lemma-under-high" APPEARS_TOO_LATE + underexposed, cocaRank=900 (high COCA — must come first)
        // threshold=3, count of underexposed lemma = 1 < 3

        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-r007", null, null, "Ella corre.", List.of("She runs."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Sports", "Complete.", true, "know-r007", null, null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-r007", "Activities", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-r007", "A1", null);

        AbsentLemma absentAbsent = new AbsentLemma(
                new LemmaAndPos("lemma-absent-low", "NOUN"), CefrLevel.A1,
                AbsenceType.COMPLETELY_ABSENT, List.of(), PriorityLevel.HIGH, 50, null);
        AbsentLemma absentUnder = new AbsentLemma(
                new LemmaAndPos("lemma-under-high", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 900, null);

        LemmaAbsenceLevelDiagnosis absenceDiag = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 100, 2, 10.0, 90.0,
                0.8, 0.7, 0.5, null,
                List.of(absentAbsent, absentUnder),
                List.of(), 2, 3, 5);

        // lemma-under-high is in lemma-count with count=1 < threshold=3 → underexposed
        LemmaCountStats statsUnder = new LemmaCountStats(
                new LemmaAndPos("lemma-under-high", "NOUN"), 1, 0.33, Optional.of(CefrLevel.A1));
        LevelLemmaCountResult levelResult = new LevelLemmaCountResult(
                CefrLevel.A1, 0.5, 1, List.of(statsUnder));
        LemmaCountLevelDiagnosis lemmaCountDiag = new LemmaCountLevelDiagnosis(levelResult);

        DefaultLevelDiagnoses milestoneDiag = new DefaultLevelDiagnoses();
        milestoneDiag.setLemmaAbsenceDiagnosis(absenceDiag);
        milestoneDiag.setLemmaCountDiagnosis(lemmaCountDiag);

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz,
                buildQuizDiagnosesWithPlacement(List.of(buildMisplacedLemma("run", "VERB", CefrLevel.A2, CefrLevel.A1, 300))));
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = new RefinementTask(
                "task-r007", AuditTarget.QUIZ, "quiz-r007",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        // Act
        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        // Assert
        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();
        Assertions.assertEquals(2, lemmas.size());

        // R007: underexposed lemma (Group 1 — APPEARS_TOO_LATE + underexposed) must come first,
        // even though it has a higher cocaRank (900) than the COMPLETELY_ABSENT lemma (50)
        Assertions.assertEquals("lemma-under-high", lemmas.get(0).getLemma(),
                "Underexposed APPEARS_TOO_LATE must come before COMPLETELY_ABSENT regardless of cocaRank");
        Assertions.assertEquals("lemma-absent-low", lemmas.get(1).getLemma(),
                "COMPLETELY_ABSENT must be last even with lowest cocaRank");

        // R007 + R013: COMPLETELY_ABSENT triple must all be null (signal not applicable)
        SuggestedLemma absentEntry = lemmas.get(1);
        Assertions.assertNull(absentEntry.getLemmaCount(),
                "COMPLETELY_ABSENT lemmaCount must be null — signal not applicable (R007 + R013)");
        Assertions.assertNull(absentEntry.getLemmaCountThreshold(),
                "COMPLETELY_ABSENT lemmaCountThreshold must be null — signal not applicable (R007 + R013)");
        Assertions.assertNull(absentEntry.getIsUnderexposed(),
                "COMPLETELY_ABSENT isUnderexposed must be null — signal not applicable (R007 + R013)");
    }

    @Test
    @DisplayName("should still return suggestedLemmas for a LEMMA_ABSENCE task when the audit report has no lemma-count signal leaving lemmaCount lemmaCountThreshold and isUnderexposed marked as unavailable and not throwing")
    @Tag("FEAT-LEMMA-SUGGESTIONS")
    @Tag("F-SLEM-R012")
    public void shouldStillReturnSuggestedLemmasForALEMMAABSENCETaskWhenTheAuditReportHasNoLemmacountSignalLeavingLemmaCountLemmaCountThresholdAndIsUnderexposedMarkedAsUnavailableAndNotThrowing() {
        // Arrange: audit report with LEMMA_ABSENCE diagnosis but NO lemma-count diagnosis
        // on any node. The milestone has LemmaAbsenceLevelDiagnosis but no LemmaCountLevelDiagnosis.
        // R012: suggestedLemmas must still be returned (no exception), and for every element
        // the triple (lemmaCount, lemmaCountThreshold, isUnderexposed) must be null (all uninformed).

        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-r012", null, null, "El mar.", List.of("The sea."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Geography", "Fill.", true, "know-r012", null, null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-r012", "Environment", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-r012", "B1", null);

        AbsentLemma absentLate = new AbsentLemma(
                new LemmaAndPos("lemma-r012-late", "NOUN"), CefrLevel.B1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 300, null);
        AbsentLemma absentCompletely = new AbsentLemma(
                new LemmaAndPos("lemma-r012-absent", "NOUN"), CefrLevel.B1,
                AbsenceType.COMPLETELY_ABSENT, List.of(), PriorityLevel.HIGH, 100, null);

        LemmaAbsenceLevelDiagnosis absenceDiag = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.B1, 100, 2, 10.0, 90.0,
                0.8, 0.7, 0.5, null,
                List.of(absentLate, absentCompletely),
                List.of(), 2, 3, 5);

        // NO lemma-count diagnosis set on the milestone — signal unavailable
        DefaultLevelDiagnoses milestoneDiag = new DefaultLevelDiagnoses();
        milestoneDiag.setLemmaAbsenceDiagnosis(absenceDiag);
        // milestoneDiag.setLemmaCountDiagnosis(...) — intentionally omitted

        MisplacedLemma ml = buildMisplacedLemma("sea", "NOUN", CefrLevel.B2, CefrLevel.B1, 500);
        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz,
                buildQuizDiagnosesWithPlacement(List.of(ml)));
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = new RefinementTask(
                "task-r012", AuditTarget.QUIZ, "quiz-r012",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        // Act: must NOT throw
        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        // Assert: R012 — context is returned, suggestedLemmas is non-empty, no exception thrown
        Assertions.assertTrue(result.isPresent(),
                "resolve must return a present context even without lemma-count signal (R012)");
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();
        Assertions.assertFalse(lemmas.isEmpty(),
                "suggestedLemmas must not be empty when absence signal is available but lemma-count is not (R012)");

        // R012 + R013: when no lemma-count signal, all three values must be null (not informed) for ALL elements
        for (SuggestedLemma sl : lemmas) {
            Assertions.assertNull(sl.getLemmaCount(),
                    "lemmaCount must be null when no lemma-count signal available (R012 + R013): " + sl.getLemma());
            Assertions.assertNull(sl.getLemmaCountThreshold(),
                    "lemmaCountThreshold must be null when no lemma-count signal available (R012 + R013): " + sl.getLemma());
            Assertions.assertNull(sl.getIsUnderexposed(),
                    "isUnderexposed must be null when no lemma-count signal available (R012 + R013): " + sl.getLemma());
        }
    }

    @Test
    @DisplayName("should emit every suggestedLemmas element with the three lemma-count fields either all informed or all uninformed never in a mixed state")
    @Tag("FEAT-LEMMA-SUGGESTIONS")
    @Tag("F-SLEM-R013")
    public void shouldEmitEverySuggestedLemmasElementWithTheThreeLemmacountFieldsEitherAllInformedOrAllUninformedNeverInAMixedState() {
        // Arrange: audit with lemma-count signal at A1.
        // - "lemma-informed": APPEARS_TOO_LATE + in lemma-count → triple must all be non-null
        // - "lemma-absent": COMPLETELY_ABSENT → triple must all be null (R007 + R013)
        // Verifies the "all-or-nothing" block invariant of R013.

        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-r013a", null, null, "La casa.", List.of("The house."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Housing", "Fill.", true, "know-r013a", null, null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-r013a", "Home", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-r013a", "A1", null);

        AbsentLemma absentInformed = new AbsentLemma(
                new LemmaAndPos("lemma-informed", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 400, null);
        AbsentLemma absentCompletely = new AbsentLemma(
                new LemmaAndPos("lemma-absent", "NOUN"), CefrLevel.A1,
                AbsenceType.COMPLETELY_ABSENT, List.of(), PriorityLevel.HIGH, 100, null);

        LemmaAbsenceLevelDiagnosis absenceDiag = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 100, 2, 10.0, 90.0,
                0.8, 0.7, 0.5, null,
                List.of(absentInformed, absentCompletely),
                List.of(), 2, 3, 5);

        // Only "lemma-informed" appears in lemma-count (underexposed: count=1 < threshold=3)
        LemmaCountStats statsInformed = new LemmaCountStats(
                new LemmaAndPos("lemma-informed", "NOUN"), 1, 0.33, Optional.of(CefrLevel.A1));
        LevelLemmaCountResult levelResult = new LevelLemmaCountResult(
                CefrLevel.A1, 0.33, 1, List.of(statsInformed));
        LemmaCountLevelDiagnosis lemmaCountDiag = new LemmaCountLevelDiagnosis(levelResult);

        DefaultLevelDiagnoses milestoneDiag = new DefaultLevelDiagnoses();
        milestoneDiag.setLemmaAbsenceDiagnosis(absenceDiag);
        milestoneDiag.setLemmaCountDiagnosis(lemmaCountDiag);

        MisplacedLemma ml = buildMisplacedLemma("house", "NOUN", CefrLevel.A2, CefrLevel.A1, 200);
        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz,
                buildQuizDiagnosesWithPlacement(List.of(ml)));
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = new RefinementTask(
                "task-r013a", AuditTarget.QUIZ, "quiz-r013a",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        // Act
        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        // Assert: R013 — no mixed states allowed
        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();
        Assertions.assertEquals(2, lemmas.size());

        for (SuggestedLemma sl : lemmas) {
            boolean allInformed = sl.getLemmaCount() != null
                    && sl.getLemmaCountThreshold() != null
                    && sl.getIsUnderexposed() != null;
            boolean allUninformed = sl.getLemmaCount() == null
                    && sl.getLemmaCountThreshold() == null
                    && sl.getIsUnderexposed() == null;
            Assertions.assertTrue(allInformed || allUninformed,
                    "SuggestedLemma '" + sl.getLemma() + "' must have all-or-nothing triple (R013), "
                    + "but got lemmaCount=" + sl.getLemmaCount()
                    + " lemmaCountThreshold=" + sl.getLemmaCountThreshold()
                    + " isUnderexposed=" + sl.getIsUnderexposed());
        }

        // lemma-informed: triple must be all non-null
        SuggestedLemma informed = lemmas.stream()
                .filter(l -> "lemma-informed".equals(l.getLemma()))
                .findFirst()
                .orElseThrow();
        Assertions.assertNotNull(informed.getLemmaCount(), "lemma-informed lemmaCount must be non-null");
        Assertions.assertNotNull(informed.getLemmaCountThreshold(), "lemma-informed lemmaCountThreshold must be non-null");
        Assertions.assertNotNull(informed.getIsUnderexposed(), "lemma-informed isUnderexposed must be non-null");

        // lemma-absent: triple must be all null (COMPLETELY_ABSENT, R007 + R013)
        SuggestedLemma absent = lemmas.stream()
                .filter(l -> "lemma-absent".equals(l.getLemma()))
                .findFirst()
                .orElseThrow();
        Assertions.assertNull(absent.getLemmaCount(), "COMPLETELY_ABSENT lemmaCount must be null (R007 + R013)");
        Assertions.assertNull(absent.getLemmaCountThreshold(), "COMPLETELY_ABSENT lemmaCountThreshold must be null (R007 + R013)");
        Assertions.assertNull(absent.getIsUnderexposed(), "COMPLETELY_ABSENT isUnderexposed must be null (R007 + R013)");
    }

    @Test
    @DisplayName("should emit suggestedLemmas elements whose informed lemma-count triple is internally consistent with isUnderexposed equal to lemmaCount less than lemmaCountThreshold and no negative values")
    @Tag("FEAT-LEMMA-SUGGESTIONS")
    @Tag("F-SLEM-R013")
    public void shouldEmitSuggestedLemmasElementsWhoseInformedLemmacountTripleIsInternallyConsistentWithIsUnderexposedEqualToLemmaCountLessThanLemmaCountThresholdAndNoNegativeValues() {
        // Arrange: two lemmas with lemma-count signal:
        // - "lemma-under": count=2, threshold=5 → isUnderexposed should be true (2 < 5)
        // - "lemma-ok": count=6, threshold=5 → isUnderexposed should be false (6 >= 5, not underexposed)
        // R013: isUnderexposed must equal (lemmaCount < lemmaCountThreshold), both non-negative.

        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-r013b", null, null, "El agua.", List.of("The water."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Nature", "Fill.", true, "know-r013b", null, null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-r013b", "Elements", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-r013b", "A1", null);

        AbsentLemma absentUnder = new AbsentLemma(
                new LemmaAndPos("lemma-under", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 300, null);
        AbsentLemma absentOk = new AbsentLemma(
                new LemmaAndPos("lemma-ok", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 700, null);

        LemmaAbsenceLevelDiagnosis absenceDiag = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 100, 2, 10.0, 90.0,
                0.8, 0.7, 0.5, null,
                List.of(absentUnder, absentOk),
                List.of(), 2, 3, 5);

        // count=2, threshold=5 → underexposed
        LemmaCountStats statsUnder = new LemmaCountStats(
                new LemmaAndPos("lemma-under", "NOUN"), 2, 0.4, Optional.of(CefrLevel.A1));
        // count=6, threshold=5 → NOT underexposed (score would be 1.0 since min(6/5, 1)=1.0)
        LemmaCountStats statsOk = new LemmaCountStats(
                new LemmaAndPos("lemma-ok", "NOUN"), 6, 1.0, Optional.of(CefrLevel.A1));

        LevelLemmaCountResult levelResult = new LevelLemmaCountResult(
                CefrLevel.A1, 0.7, 2, List.of(statsUnder, statsOk));
        LemmaCountLevelDiagnosis lemmaCountDiag = new LemmaCountLevelDiagnosis(levelResult);

        DefaultLevelDiagnoses milestoneDiag = new DefaultLevelDiagnoses();
        milestoneDiag.setLemmaAbsenceDiagnosis(absenceDiag);
        milestoneDiag.setLemmaCountDiagnosis(lemmaCountDiag);

        MisplacedLemma ml = buildMisplacedLemma("water", "NOUN", CefrLevel.A2, CefrLevel.A1, 100);
        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz,
                buildQuizDiagnosesWithPlacement(List.of(ml)));
        AuditReport report = new AuditReport(tree[0]);
        RefinementTask task = new RefinementTask(
                "task-r013b", AuditTarget.QUIZ, "quiz-r013b",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        // Act
        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        // Assert: R013 — triple must be internally consistent
        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();
        Assertions.assertFalse(lemmas.isEmpty());

        for (SuggestedLemma sl : lemmas) {
            if (sl.getLemmaCount() != null) {
                // If informed: values must be non-negative and isUnderexposed consistent
                Assertions.assertTrue(sl.getLemmaCount() >= 0,
                        "lemmaCount must be non-negative (R013): " + sl.getLemma());
                Assertions.assertTrue(sl.getLemmaCountThreshold() >= 0,
                        "lemmaCountThreshold must be non-negative (R013): " + sl.getLemma());
                boolean expectedUnderexposed = sl.getLemmaCount() < sl.getLemmaCountThreshold();
                Assertions.assertEquals(expectedUnderexposed, sl.getIsUnderexposed(),
                        "isUnderexposed must equal (lemmaCount < lemmaCountThreshold) for: " + sl.getLemma()
                        + " [count=" + sl.getLemmaCount() + " threshold=" + sl.getLemmaCountThreshold() + "]");
            }
        }

        // Specific checks for our two lemmas
        SuggestedLemma slUnder = lemmas.stream()
                .filter(l -> "lemma-under".equals(l.getLemma()))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals(2, slUnder.getLemmaCount());
        Assertions.assertEquals(5, slUnder.getLemmaCountThreshold());
        Assertions.assertTrue(slUnder.getIsUnderexposed(), "count=2 < threshold=5 → underexposed");

        SuggestedLemma slOk = lemmas.stream()
                .filter(l -> "lemma-ok".equals(l.getLemma()))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals(6, slOk.getLemmaCount());
        Assertions.assertEquals(5, slOk.getLemmaCountThreshold());
        Assertions.assertFalse(slOk.getIsUnderexposed(), "count=6 >= threshold=5 → NOT underexposed");
    }
}
