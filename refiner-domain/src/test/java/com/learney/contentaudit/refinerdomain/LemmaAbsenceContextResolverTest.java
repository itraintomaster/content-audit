package com.learney.contentaudit.refinerdomain;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableMilestone;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.AuditableTopic;
import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.auditdomain.NlpToken;
import com.learney.contentaudit.auditdomain.DefaultCourseDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultKnowledgeDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultLevelDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultQuizDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultTopicDiagnoses;
import com.learney.contentaudit.auditdomain.SentenceLengthDiagnosis;
import com.learney.contentaudit.auditdomain.coca.CocaProgressionDiagnosis;
import com.learney.contentaudit.auditdomain.coca.ImprovementDirective;
import com.learney.contentaudit.auditdomain.coca.ImprovementDirectiveType;
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

    /**
     * Builds a course node with a CocaProgressionDiagnosis carrying ENRICH directives for
     * the given cocaRank ranges. Used to set up deficient-band membership for R014/R015 tests.
     * Each pair (from, to) becomes one ENRICH directive for the level.
     */
    private AuditNode buildCourseNodeWithEnrichBands(String levelName, int[][] bands) {
        AuditNode node = buildCourseNode();
        List<ImprovementDirective> directives = new ArrayList<>();
        for (int[] band : bands) {
            directives.add(new ImprovementDirective(
                    ImprovementDirectiveType.ENRICH,
                    "band-" + band[0] + "-" + band[1],
                    levelName,
                    band[0], band[1],
                    0.05, 0.20));
        }
        CocaProgressionDiagnosis progression = new CocaProgressionDiagnosis(
                0.7, List.of(), directives);
        DefaultCourseDiagnoses courseDiagnoses = new DefaultCourseDiagnoses();
        courseDiagnoses.setCocaBucketsDiagnosis(progression);
        node.setDiagnoses(courseDiagnoses);
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
    @DisplayName("should rank suggestedLemmas into six first-match-wins tiers ordering underexposed-in-deficient-band then absent-in-deficient-band then underexposed then absent then exposed-scarce-in-deficient-band then exposed-scarce")
    @Tag("FEAT-LEMMA-SUGGESTIONS")
    @Tag("F-SLEM-R003")
    public void shouldRankSuggestedLemmasIntoSixFirstmatchwinsTiersOrderingUnderexposedInDeficientBandThenAbsentInDeficientBandThenUnderexposedThenAbsentThenExposedScarceInDeficientBandThenExposedScarce() {
        // Fixture: threshold N=3. ENRICH band for A1: cocaRank 1..500.
        // Tier 1: sub-expuesto (count 1..2) + banda deficitaria (cocaRank in 1..500) → "lemma-t1"
        // Tier 2: ausente (COMPLETELY_ABSENT) + banda deficitaria (cocaRank in 1..500) → "lemma-t2"
        // Tier 3: sub-expuesto (count 1..2) + fuera de banda → "lemma-t3"
        // Tier 4: ausente (COMPLETELY_ABSENT) + fuera de banda → "lemma-t4"
        // Tier 5: expuesto-escaso (count N=3..2N-1=5) + banda deficitaria → "lemma-t5" (includeExposed)
        // Tier 6: expuesto-escaso (count 3..5) + fuera de banda → "lemma-t6"
        // All COCA ranks shuffled to prove tier overrides COCA.

        int N = 3;
        // ENRICH band: 1..500
        AuditNode courseNode = buildCourseNodeWithEnrichBands("A1", new int[][]{{1, 500}});

        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-r003-v2", null, null,
                "La traduccion.", List.of("The sentence."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(
                List.of(), "Vocab", "Complete.", true, "know-r003-v2", null, null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-r003-v2", "Topic", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-r003-v2", "A1", null);

        // Absent lemmas from LemmaAbsenceLevelDiagnosis
        AbsentLemma t2Lemma = new AbsentLemma(
                new LemmaAndPos("lemma-t2", "NOUN"), CefrLevel.A1,
                AbsenceType.COMPLETELY_ABSENT, List.of(), PriorityLevel.HIGH, 200, null); // in band 1..500
        AbsentLemma t4Lemma = new AbsentLemma(
                new LemmaAndPos("lemma-t4", "NOUN"), CefrLevel.A1,
                AbsenceType.COMPLETELY_ABSENT, List.of(), PriorityLevel.HIGH, 900, null); // outside band
        AbsentLemma t1LemmaAbsence = new AbsentLemma(
                new LemmaAndPos("lemma-t1", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 400, null); // in band + underexposed
        AbsentLemma t3LemmaAbsence = new AbsentLemma(
                new LemmaAndPos("lemma-t3", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 700, null); // out of band + underexposed
        AbsentLemma t5LemmaAbsence = new AbsentLemma(
                new LemmaAndPos("lemma-t5", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 300, null); // in band + exposed-scarce
        AbsentLemma t6LemmaAbsence = new AbsentLemma(
                new LemmaAndPos("lemma-t6", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 800, null); // out of band + exposed-scarce

        LemmaAbsenceLevelDiagnosis absenceDiag = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 100, 6, 10.0, 90.0, 0.8, 0.7, 0.5, null,
                List.of(t2Lemma, t4Lemma, t1LemmaAbsence, t3LemmaAbsence, t5LemmaAbsence, t6LemmaAbsence),
                List.of(), 2, N, 5);

        // lemma-count signal:
        // t1: count=1 (1..N-1 = sub-expuesto)
        // t3: count=2 (1..N-1 = sub-expuesto, out of band)
        // t5: count=N=3 (expuesto-escaso: N..2N-1)
        // t6: count=4 (expuesto-escaso: 4..5 range, out of band)
        LemmaCountStats statsT1 = new LemmaCountStats(new LemmaAndPos("lemma-t1", "NOUN"), 1, 0.33, Optional.of(CefrLevel.A1));
        LemmaCountStats statsT3 = new LemmaCountStats(new LemmaAndPos("lemma-t3", "NOUN"), 2, 0.67, Optional.of(CefrLevel.A1));
        LemmaCountStats statsT5 = new LemmaCountStats(new LemmaAndPos("lemma-t5", "NOUN"), N, 1.0, Optional.of(CefrLevel.A1));
        LemmaCountStats statsT6 = new LemmaCountStats(new LemmaAndPos("lemma-t6", "NOUN"), 4, 1.0, Optional.of(CefrLevel.A1));

        LevelLemmaCountResult levelResult = new LevelLemmaCountResult(
                CefrLevel.A1, 0.5, N, List.of(statsT1, statsT3, statsT5, statsT6));
        LemmaCountLevelDiagnosis lemmaCountDiag = new LemmaCountLevelDiagnosis(levelResult);

        DefaultLevelDiagnoses milestoneDiag = new DefaultLevelDiagnoses();
        milestoneDiag.setLemmaAbsenceDiagnosis(absenceDiag);
        milestoneDiag.setLemmaCountDiagnosis(lemmaCountDiag);

        AuditNode milestoneNode = buildMilestoneNode(courseNode, milestone, milestoneDiag);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        MisplacedLemma ml = buildMisplacedLemma("negotiate", "VERB", CefrLevel.B1, CefrLevel.A1, 1000);
        buildQuizNode(knowledgeNode, quiz, buildQuizDiagnosesWithPlacement(List.of(ml)));

        AuditReport report = new AuditReport(courseNode);
        RefinementTask task = new RefinementTask(
                "task-r003-v2", AuditTarget.QUIZ, "quiz-r003-v2",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();
        Assertions.assertEquals(6, lemmas.size(), "Six tiers should produce 6 distinct entries");

        // Tier order: T1 → T2 → T3 → T4 → T5 → T6
        Assertions.assertEquals("lemma-t1", lemmas.get(0).getLemma(),
                "T1 (sub-expuesto + banda) must be first");
        Assertions.assertEquals("lemma-t2", lemmas.get(1).getLemma(),
                "T2 (ausente + banda) must be second — deliberate: banda > subtipo distinción");
        Assertions.assertEquals("lemma-t3", lemmas.get(2).getLemma(),
                "T3 (sub-expuesto, no banda) must be third");
        Assertions.assertEquals("lemma-t4", lemmas.get(3).getLemma(),
                "T4 (ausente, no banda) must be fourth");
        Assertions.assertEquals("lemma-t5", lemmas.get(4).getLemma(),
                "T5 (expuesto-escaso + banda) must be fifth");
        Assertions.assertEquals("lemma-t6", lemmas.get(5).getLemma(),
                "T6 (expuesto-escaso, no banda) must be last");

        // R013: T2 and T4 are COMPLETELY_ABSENT → triple must be null
        Assertions.assertNull(lemmas.get(1).getLemmaCount(), "T2 COMPLETELY_ABSENT lemmaCount must be null");
        Assertions.assertNull(lemmas.get(3).getLemmaCount(), "T4 COMPLETELY_ABSENT lemmaCount must be null");
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
    @DisplayName("should break ties within the same tier by ordering suggestedLemmas by ascending cocaRank as the final tiebreak")
    @Tag("FEAT-LEMMA-SUGGESTIONS")
    @Tag("F-SLEM-R005")
    public void shouldBreakTiesWithinTheSameTierByOrderingSuggestedLemmasByAscendingCocaRankAsTheFinalTiebreak() {
        // Fixture: 3 lemmas all in Tier 3 (sub-expuesto, no deficient band) with different cocaRanks.
        // cocaRanks: 500, 100, 300 → expected order: 100, 300, 500.
        // ENRICH band covers 1..200, so lemmas at rank 100, 300, 500 are: 100 in band, 300 out, 500 out.
        // But all have count=1 < threshold=3 (sub-expuesto). None have LEMMA_ABSENCE (pure underexposed).
        // Without LEMMA_ABSENCE and none in deficient band → Tier 3 for all.

        AuditNode courseNode = buildCourseNodeWithEnrichBands("A1", new int[][]{{1, 50}}); // band 1..50, so none qualify
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-r005a-v2", null, null, "Ella corre.", List.of("She runs."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Sports", "Complete.", true, "know-r005a-v2", null, null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-r005a-v2", "Activities", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-r005a-v2", "A1", null);

        // No LEMMA_ABSENCE for these lemmas — they are pure underexposed (sub-expuesto, no LEMMA_ABSENCE)
        // Include them only via LemmaCountLevelDiagnosis
        LemmaCountStats stats1 = new LemmaCountStats(new LemmaAndPos("lemma-coca-500", "NOUN"), 1, 0.33, Optional.of(CefrLevel.A1));
        LemmaCountStats stats2 = new LemmaCountStats(new LemmaAndPos("lemma-coca-100", "NOUN"), 1, 0.33, Optional.of(CefrLevel.A1));
        LemmaCountStats stats3 = new LemmaCountStats(new LemmaAndPos("lemma-coca-300", "NOUN"), 1, 0.33, Optional.of(CefrLevel.A1));

        // Provide cocaRank via AbsentLemma so the resolver can order by it
        AbsentLemma absLate500 = new AbsentLemma(
                new LemmaAndPos("lemma-coca-500", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 500, null);
        AbsentLemma absLate100 = new AbsentLemma(
                new LemmaAndPos("lemma-coca-100", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 100, null);
        AbsentLemma absLate300 = new AbsentLemma(
                new LemmaAndPos("lemma-coca-300", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 300, null);

        LemmaAbsenceLevelDiagnosis absenceDiag = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 100, 3, 10.0, 90.0, 0.8, 0.7, 0.5, null,
                List.of(absLate500, absLate100, absLate300),
                List.of(), 2, 3, 5);

        LevelLemmaCountResult levelResult = new LevelLemmaCountResult(
                CefrLevel.A1, 0.33, 3, List.of(stats1, stats2, stats3));
        LemmaCountLevelDiagnosis lemmaCountDiag = new LemmaCountLevelDiagnosis(levelResult);

        DefaultLevelDiagnoses milestoneDiag = new DefaultLevelDiagnoses();
        milestoneDiag.setLemmaAbsenceDiagnosis(absenceDiag);
        milestoneDiag.setLemmaCountDiagnosis(lemmaCountDiag);

        AuditNode milestoneNode = buildMilestoneNode(courseNode, milestone, milestoneDiag);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        MisplacedLemma ml = buildMisplacedLemma("run", "VERB", CefrLevel.A2, CefrLevel.A1, 300);
        buildQuizNode(knowledgeNode, quiz, buildQuizDiagnosesWithPlacement(List.of(ml)));

        AuditReport report = new AuditReport(courseNode);
        RefinementTask task = new RefinementTask(
                "task-r005a-v2", AuditTarget.QUIZ, "quiz-r005a-v2",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        // All in the same tier (T3: sub-expuesto, none in deficient band 1..50)
        // COCA ascending is the final tiebreak within a tier
        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();
        Assertions.assertEquals(3, lemmas.size());
        Assertions.assertEquals("lemma-coca-100", lemmas.get(0).getLemma(),
                "Lowest cocaRank (100) must be first — COCA asc is the final tiebreak within a tier (R005)");
        Assertions.assertEquals("lemma-coca-300", lemmas.get(1).getLemma(),
                "Middle cocaRank (300) must be second");
        Assertions.assertEquals("lemma-coca-500", lemmas.get(2).getLemma(),
                "Highest cocaRank (500) must be third");
    }

    @Test
    @DisplayName("should not use lemma-count exposure deficit magnitude as an intra-tier tiebreaker leaving COCA ascending as the only order within a tier")
    @Tag("FEAT-LEMMA-SUGGESTIONS")
    @Tag("F-SLEM-R005")
    public void shouldNotUseLemmacountExposureDeficitMagnitudeAsAnIntraTierTiebreakerLeavingCOCAAscendingAsTheOnlyOrderWithinATier() {
        // Fixture: 3 lemmas all in Tier 3 (sub-expuesto, no deficient band), same cocaRank=250,
        // but different deficit magnitudes: count=1 (deficit 2), count=0 (deficit 3), count=2 (deficit 1).
        // R005: deficit magnitude must NOT determine order; COCA asc is the only tiebreak.
        // Since COCA is equal, stable insertion order is acceptable — deficit-driven reordering is forbidden.

        AuditNode courseNode = buildCourseNodeWithEnrichBands("A1", new int[][]{{1, 10}}); // tiny band, none qualify
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-r005b-v2", null, null, "El gato.", List.of("The cat."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Animals", "Fill.", true, "know-r005b-v2", null, null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-r005b-v2", "Nature", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-r005b-v2", "A1", null);

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
                CefrLevel.A1, 100, 3, 10.0, 90.0, 0.8, 0.7, 0.5, null,
                List.of(absent1, absent2, absent3),
                List.of(), 2, 3, 5);

        // Counts: lemma-def2=1 (deficit 2), lemma-def3=0 (deficit 3), lemma-def1=2 (deficit 1)
        LemmaCountStats stats1 = new LemmaCountStats(new LemmaAndPos("lemma-def2", "NOUN"), 1, 0.33, Optional.of(CefrLevel.A1));
        LemmaCountStats stats2 = new LemmaCountStats(new LemmaAndPos("lemma-def3", "NOUN"), 0, 0.0, Optional.of(CefrLevel.A1));
        LemmaCountStats stats3 = new LemmaCountStats(new LemmaAndPos("lemma-def1", "NOUN"), 2, 0.67, Optional.of(CefrLevel.A1));

        LevelLemmaCountResult levelResult = new LevelLemmaCountResult(
                CefrLevel.A1, 0.33, 3, List.of(stats1, stats2, stats3));
        LemmaCountLevelDiagnosis lemmaCountDiag = new LemmaCountLevelDiagnosis(levelResult);

        DefaultLevelDiagnoses milestoneDiag = new DefaultLevelDiagnoses();
        milestoneDiag.setLemmaAbsenceDiagnosis(absenceDiag);
        milestoneDiag.setLemmaCountDiagnosis(lemmaCountDiag);

        AuditNode milestoneNode = buildMilestoneNode(courseNode, milestone, milestoneDiag);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        MisplacedLemma ml = buildMisplacedLemma("cat", "NOUN", CefrLevel.A2, CefrLevel.A1, 200);
        buildQuizNode(knowledgeNode, quiz, buildQuizDiagnosesWithPlacement(List.of(ml)));

        AuditReport report = new AuditReport(courseNode);
        RefinementTask task = new RefinementTask(
                "task-r005b-v2", AuditTarget.QUIZ, "quiz-r005b-v2",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();
        Assertions.assertEquals(3, lemmas.size(), "All 3 sub-expuesto lemmas must be in Tier 3");

        // R005: deficit magnitude must NOT reorder lemmas within a tier.
        // If deficit-driven, lemma-def3 (deficit=3) would jump to first.
        // With COCA asc as the only tiebreaker and all COCA=250, insertion order prevails (not deficit order).
        List<String> names = lemmas.stream().map(SuggestedLemma::getLemma).toList();
        Assertions.assertTrue(names.contains("lemma-def2"), "lemma-def2 must be present");
        Assertions.assertTrue(names.contains("lemma-def3"), "lemma-def3 must be present");
        Assertions.assertTrue(names.contains("lemma-def1"), "lemma-def1 must be present");
        // Key assertion: if deficit were used, lemma-def3 (highest deficit=3) would be first.
        // R005 forbids that — COCA is the only tiebreak.
        Assertions.assertNotEquals("lemma-def3", lemmas.get(0).getLemma(),
                "lemma-def3 (highest deficit) must NOT be first — exposure deficit magnitude is not a tiebreaker (R005)");
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
    @DisplayName("should place a COMPLETELY_ABSENT lemma behind an already-introduced underexposed lemma only when both share the same deficient-band state so tier2 stays behind tier1 and tier4 stays behind tier3")
    @Tag("FEAT-LEMMA-SUGGESTIONS")
    @Tag("F-SLEM-R007")
    public void shouldPlaceACOMPLETELYABSENTLemmaBehindAnAlreadyIntroducedUnderexposedLemmaOnlyWhenBothShareTheSameDeficientBandStateSoTier2StaysBehindTier1AndTier4StaysBehindTier3() {
        // R007 (relaxed): COMPLETELY_ABSENT goes behind sub-expuesto ONLY within the same band state.
        // Tier ordering: T1(under+band) > T2(absent+band) > T3(under,no-band) > T4(absent,no-band)
        //
        // Fixture:
        //   ENRICH band: cocaRank 51..100
        //   - "lemma-under-band"  sub-expuesto (count=1<3) + in band (rank=80)    → Tier 1
        //   - "lemma-absent-band" COMPLETELY_ABSENT          + in band (rank=60)   → Tier 2
        //   - "lemma-under-noband" sub-expuesto (count=2<3) + not in band (rank=200) → Tier 3
        //   - "lemma-absent-noband" COMPLETELY_ABSENT       + not in band (rank=50) → Tier 4
        //     (rank=50 is below the band floor of 51 → genuinely out of band)
        //
        // Expected order: T1(rank=80) → T2(rank=60) → T3(rank=200) → T4(rank=50)
        // Key R007 assertions:
        //   - T2 (absent+band, rank=60) BEATS T3 (under+no-band, rank=200) — absent+band outranks under+no-band
        //   - T4 (absent+no-band, rank=50) comes AFTER T3 (under+no-band, rank=200) — within no-band, under beats absent

        AuditNode courseNode = buildCourseNodeWithEnrichBands("A1", new int[][]{{51, 100}});
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-r007-v2", null, null, "Ella corre.", List.of("She runs."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Sports", "Complete.", true, "know-r007-v2", null, null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-r007-v2", "Activities", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-r007-v2", "A1", null);

        AbsentLemma underBand = new AbsentLemma(
                new LemmaAndPos("lemma-under-band", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 80, null);
        AbsentLemma absentBand = new AbsentLemma(
                new LemmaAndPos("lemma-absent-band", "NOUN"), CefrLevel.A1,
                AbsenceType.COMPLETELY_ABSENT, List.of(), PriorityLevel.HIGH, 60, null);
        AbsentLemma underNoBand = new AbsentLemma(
                new LemmaAndPos("lemma-under-noband", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 200, null);
        AbsentLemma absentNoBand = new AbsentLemma(
                new LemmaAndPos("lemma-absent-noband", "NOUN"), CefrLevel.A1,
                AbsenceType.COMPLETELY_ABSENT, List.of(), PriorityLevel.HIGH, 50, null);

        LemmaAbsenceLevelDiagnosis absenceDiag = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 100, 4, 10.0, 90.0, 0.8, 0.7, 0.5, null,
                List.of(underBand, absentBand, underNoBand, absentNoBand),
                List.of(), 2, 4, 6);

        LemmaCountStats statsUnderBand = new LemmaCountStats(
                new LemmaAndPos("lemma-under-band", "NOUN"), 1, 0.33, Optional.of(CefrLevel.A1));
        LemmaCountStats statsUnderNoBand = new LemmaCountStats(
                new LemmaAndPos("lemma-under-noband", "NOUN"), 2, 0.67, Optional.of(CefrLevel.A1));

        LevelLemmaCountResult levelResult = new LevelLemmaCountResult(
                CefrLevel.A1, 0.5, 3, List.of(statsUnderBand, statsUnderNoBand));
        LemmaCountLevelDiagnosis lemmaCountDiag = new LemmaCountLevelDiagnosis(levelResult);

        DefaultLevelDiagnoses milestoneDiag = new DefaultLevelDiagnoses();
        milestoneDiag.setLemmaAbsenceDiagnosis(absenceDiag);
        milestoneDiag.setLemmaCountDiagnosis(lemmaCountDiag);

        AuditNode milestoneNode = buildMilestoneNode(courseNode, milestone, milestoneDiag);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        buildQuizNode(knowledgeNode, quiz,
                buildQuizDiagnosesWithPlacement(List.of(buildMisplacedLemma("run", "VERB", CefrLevel.A2, CefrLevel.A1, 300))));

        AuditReport report = new AuditReport(courseNode);
        RefinementTask task = new RefinementTask(
                "task-r007-v2", AuditTarget.QUIZ, "quiz-r007-v2",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();
        Assertions.assertEquals(4, lemmas.size());

        // Tier order: T1 → T2 → T3 → T4
        Assertions.assertEquals("lemma-under-band", lemmas.get(0).getLemma(),
                "Tier 1 (sub-expuesto + in band, rank=80) must be first");
        Assertions.assertEquals("lemma-absent-band", lemmas.get(1).getLemma(),
                "Tier 2 (absent + in band, rank=60) must be second — absent+band beats under+no-band (R007)");
        Assertions.assertEquals("lemma-under-noband", lemmas.get(2).getLemma(),
                "Tier 3 (sub-expuesto, no band, rank=200) must be third");
        Assertions.assertEquals("lemma-absent-noband", lemmas.get(3).getLemma(),
                "Tier 4 (absent, no band, rank=50) must be last — within no-band, under beats absent (R007)");
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

    @Test
    @DisplayName("should place an absent lemma that belongs to a deficient band ahead of an underexposed lemma that belongs to no deficient band so tier2 outranks tier3")
    @Tag("FEAT-LEMMA-SUGGESTIONS")
    @Tag("F-SLEM-R014")
    public void shouldPlaceAnAbsentLemmaThatBelongsToADeficientBandAheadOfAnUnderexposedLemmaThatBelongsToNoDeficientBandSoTier2OutranksTier3() {
        // Fixture:
        //   ENRICH band: cocaRank 51..200
        //   "lemma-absent-band"  COMPLETELY_ABSENT + in band (rank=150) → Tier 2
        //   "lemma-under-noband" sub-expuesto (count=1<3) + not in band (rank=50) → Tier 3
        //     (rank=50 is below the band floor of 51 → genuinely out of band)
        //
        // Expected: T2 (absent+band) before T3 (under, no band)
        // This verifies that absent+band beats under+no-band — a core consequence of the 6-tier ranking.

        AuditNode courseNode = buildCourseNodeWithEnrichBands("A1", new int[][]{{51, 200}});
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-r014a", null, null, "Ella corre.", List.of("She runs."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Sports", "Complete.", true, "know-r014a", null, null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-r014a", "Activities", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-r014a", "A1", null);

        AbsentLemma absentBand = new AbsentLemma(
                new LemmaAndPos("lemma-absent-band", "NOUN"), CefrLevel.A1,
                AbsenceType.COMPLETELY_ABSENT, List.of(), PriorityLevel.HIGH, 150, null);
        AbsentLemma underNoBand = new AbsentLemma(
                new LemmaAndPos("lemma-under-noband", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 50, null);

        LemmaAbsenceLevelDiagnosis absenceDiag = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 100, 2, 10.0, 90.0, 0.8, 0.7, 0.5, null,
                List.of(absentBand, underNoBand),
                List.of(), 2, 2, 4);

        LemmaCountStats statsUnder = new LemmaCountStats(
                new LemmaAndPos("lemma-under-noband", "NOUN"), 1, 0.33, Optional.of(CefrLevel.A1));
        LevelLemmaCountResult levelResult = new LevelLemmaCountResult(
                CefrLevel.A1, 0.33, 3, List.of(statsUnder));
        LemmaCountLevelDiagnosis lemmaCountDiag = new LemmaCountLevelDiagnosis(levelResult);

        DefaultLevelDiagnoses milestoneDiag = new DefaultLevelDiagnoses();
        milestoneDiag.setLemmaAbsenceDiagnosis(absenceDiag);
        milestoneDiag.setLemmaCountDiagnosis(lemmaCountDiag);

        AuditNode milestoneNode = buildMilestoneNode(courseNode, milestone, milestoneDiag);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        buildQuizNode(knowledgeNode, quiz,
                buildQuizDiagnosesWithPlacement(List.of(buildMisplacedLemma("run", "VERB", CefrLevel.A2, CefrLevel.A1, 300))));

        AuditReport report = new AuditReport(courseNode);
        RefinementTask task = new RefinementTask(
                "task-r014a", AuditTarget.QUIZ, "quiz-r014a",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();
        Assertions.assertEquals(2, lemmas.size());
        Assertions.assertEquals("lemma-absent-band", lemmas.get(0).getLemma(),
                "Tier 2 (absent + in band, rank=150) must beat Tier 3 (under + no band, rank=50) — R014");
        Assertions.assertEquals("lemma-under-noband", lemmas.get(1).getLemma(),
                "Tier 3 (sub-expuesto, no band) must be second even though rank=50 is lower");
    }

    @Test
    @DisplayName("should classify a lemma with exposure exactly equal to the threshold N as exposed-but-scarce in the expanded band tiers five or six and never as underexposed")
    @Tag("FEAT-LEMMA-SUGGESTIONS")
    @Tag("F-SLEM-R014")
    public void shouldClassifyALemmaWithExposureExactlyEqualToTheThresholdNAsExposedbutscarceInTheExpandedBandTiersFiveOrSixAndNeverAsUnderexposed() {
        // R014 edge: count==N is the boundary between sub-expuesto (1..N-1) and expuesto-escaso (N..2N-1).
        // A lemma with count exactly equal to N must be classified as expuesto-escaso (Tier 5 or 6),
        // never as sub-expuesto (Tier 1 or 3). The threshold N itself is the exclusive boundary for underexposed.
        //
        // Fixture (threshold=3, so N=3):
        //   ENRICH band: cocaRank 1..500
        //   "lemma-at-n"     count=3 (== N) + in band (rank=200) → must be Tier 5 (exposed-scarce+band), NOT Tier 1
        //   "lemma-under-n"  count=2 (< N)  + in band (rank=100) → Tier 1 (sub-expuesto+band)
        //
        // Expected: lemma-under-n (T1) before lemma-at-n (T5 or T6)

        AuditNode courseNode = buildCourseNodeWithEnrichBands("A1", new int[][]{{1, 500}});
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-r014b", null, null, "Ella corre.", List.of("She runs."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Sports", "Complete.", true, "know-r014b", null, null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-r014b", "Activities", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-r014b", "A1", null);

        AbsentLemma lemmaAtN = new AbsentLemma(
                new LemmaAndPos("lemma-at-n", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 200, null);
        AbsentLemma lemmaUnderN = new AbsentLemma(
                new LemmaAndPos("lemma-under-n", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 100, null);

        LemmaAbsenceLevelDiagnosis absenceDiag = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 100, 2, 10.0, 90.0, 0.8, 0.7, 0.5, null,
                List.of(lemmaAtN, lemmaUnderN),
                List.of(), 2, 2, 4);

        // threshold=3, lemma-at-n has count=3 (==N), lemma-under-n has count=2 (<N)
        LemmaCountStats statsAtN = new LemmaCountStats(
                new LemmaAndPos("lemma-at-n", "NOUN"), 3, 1.0, Optional.of(CefrLevel.A1));
        LemmaCountStats statsUnderN = new LemmaCountStats(
                new LemmaAndPos("lemma-under-n", "NOUN"), 2, 0.67, Optional.of(CefrLevel.A1));

        LevelLemmaCountResult levelResult = new LevelLemmaCountResult(
                CefrLevel.A1, 0.83, 3, List.of(statsAtN, statsUnderN));
        LemmaCountLevelDiagnosis lemmaCountDiag = new LemmaCountLevelDiagnosis(levelResult);

        DefaultLevelDiagnoses milestoneDiag = new DefaultLevelDiagnoses();
        milestoneDiag.setLemmaAbsenceDiagnosis(absenceDiag);
        milestoneDiag.setLemmaCountDiagnosis(lemmaCountDiag);

        AuditNode milestoneNode = buildMilestoneNode(courseNode, milestone, milestoneDiag);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        buildQuizNode(knowledgeNode, quiz,
                buildQuizDiagnosesWithPlacement(List.of(buildMisplacedLemma("run", "VERB", CefrLevel.A2, CefrLevel.A1, 300))));

        AuditReport report = new AuditReport(courseNode);
        RefinementTask task = new RefinementTask(
                "task-r014b", AuditTarget.QUIZ, "quiz-r014b",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();
        Assertions.assertEquals(2, lemmas.size());

        // lemma-under-n (T1: sub-expuesto+band) must appear before lemma-at-n (T5: exposed-scarce+band)
        Assertions.assertEquals("lemma-under-n", lemmas.get(0).getLemma(),
                "Tier 1 (count=2 < N=3, sub-expuesto + in band) must be first");
        Assertions.assertEquals("lemma-at-n", lemmas.get(1).getLemma(),
                "Tier 5 (count=3 == N=3, exposed-but-scarce + in band) must be second — count==N is never underexposed (R014)");

        // Additional: lemma-at-n must be marked NOT underexposed (isUnderexposed=false)
        SuggestedLemma atN = lemmas.get(1);
        Assertions.assertNotNull(atN.getLemmaCount(), "lemma-at-n lemmaCount must be set (R013)");
        Assertions.assertEquals(3, atN.getLemmaCount(), "count==N must be 3");
        Assertions.assertEquals(3, atN.getLemmaCountThreshold(), "threshold must be 3");
        Assertions.assertFalse(atN.getIsUnderexposed(),
                "count==N is exposed-but-scarce, NOT underexposed — the threshold is exclusive (R014)");
    }

    @Test
    @DisplayName("should treat deficient-band membership as a binary key so two lemmas in different deficient bands share the same tier and resolve between themselves by ascending cocaRank")
    @Tag("FEAT-LEMMA-SUGGESTIONS")
    @Tag("F-SLEM-R014")
    public void shouldTreatDeficientbandMembershipAsABinaryKeySoTwoLemmasInDifferentDeficientBandsShareTheSameTierAndResolveBetweenThemselvesByAscendingCocaRank() {
        // R014: band membership is BINARY — a lemma either belongs to at least one deficient band or it doesn't.
        // Two lemmas in DIFFERENT deficient bands both get "in band" = true → same tier → COCA ascending resolves them.
        //
        // Fixture:
        //   Band A: cocaRank 1..100   (ENRICH)
        //   Band B: cocaRank 200..300 (ENRICH)
        //   "lemma-band-a"  sub-expuesto + in band A (rank=80)  → Tier 1
        //   "lemma-band-b"  sub-expuesto + in band B (rank=250) → Tier 1 (same tier, different band)
        //   They share Tier 1 → COCA ascending resolves → lemma-band-a (80) before lemma-band-b (250)

        AuditNode courseNode = buildCourseNodeWithEnrichBands("A1", new int[][]{{1, 100}, {200, 300}});
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-r014c", null, null, "Ella trabaja.", List.of("She works."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Work", "Complete.", true, "know-r014c", null, null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-r014c", "Daily Life", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-r014c", "A1", null);

        AbsentLemma lemmaBandA = new AbsentLemma(
                new LemmaAndPos("lemma-band-a", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 80, null);
        AbsentLemma lemmaBandB = new AbsentLemma(
                new LemmaAndPos("lemma-band-b", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 250, null);

        LemmaAbsenceLevelDiagnosis absenceDiag = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 100, 2, 10.0, 90.0, 0.8, 0.7, 0.5, null,
                List.of(lemmaBandA, lemmaBandB),
                List.of(), 2, 2, 4);

        // Both sub-expuesto: count=1 < threshold=3
        LemmaCountStats statsA = new LemmaCountStats(
                new LemmaAndPos("lemma-band-a", "NOUN"), 1, 0.33, Optional.of(CefrLevel.A1));
        LemmaCountStats statsB = new LemmaCountStats(
                new LemmaAndPos("lemma-band-b", "NOUN"), 1, 0.33, Optional.of(CefrLevel.A1));

        LevelLemmaCountResult levelResult = new LevelLemmaCountResult(
                CefrLevel.A1, 0.33, 3, List.of(statsA, statsB));
        LemmaCountLevelDiagnosis lemmaCountDiag = new LemmaCountLevelDiagnosis(levelResult);

        DefaultLevelDiagnoses milestoneDiag = new DefaultLevelDiagnoses();
        milestoneDiag.setLemmaAbsenceDiagnosis(absenceDiag);
        milestoneDiag.setLemmaCountDiagnosis(lemmaCountDiag);

        AuditNode milestoneNode = buildMilestoneNode(courseNode, milestone, milestoneDiag);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        buildQuizNode(knowledgeNode, quiz,
                buildQuizDiagnosesWithPlacement(List.of(buildMisplacedLemma("work", "VERB", CefrLevel.A2, CefrLevel.A1, 120))));

        AuditReport report = new AuditReport(courseNode);
        RefinementTask task = new RefinementTask(
                "task-r014c", AuditTarget.QUIZ, "quiz-r014c",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();
        Assertions.assertEquals(2, lemmas.size());

        // Both in Tier 1 (sub-expuesto + in a deficient band), COCA ascending resolves
        Assertions.assertEquals("lemma-band-a", lemmas.get(0).getLemma(),
                "lemma-band-a (rank=80, in band A) and lemma-band-b (rank=250, in band B) share Tier 1 — COCA asc resolves (R014)");
        Assertions.assertEquals("lemma-band-b", lemmas.get(1).getLemma(),
                "lemma-band-b (rank=250) must be second — different deficient bands share the same tier (R014)");
    }

    @Test
    @DisplayName("should always place exposed-but-scarce lemmas in tiers five and six after every macro-scarcity lemma in tiers one through four regardless of deficient-band membership")
    @Tag("FEAT-LEMMA-SUGGESTIONS")
    @Tag("F-SLEM-R014")
    public void shouldAlwaysPlaceExposedbutscarceLemmasInTiersFiveAndSixAfterEveryMacroscarcityLemmaInTiersOneThroughFourRegardlessOfDeficientbandMembership() {
        // R014: expuesto-escaso lemmas (Tier 5/6) are ALWAYS after macro-scarcity lemmas (Tier 1-4).
        // Even an exposed-scarce lemma IN a deficient band (Tier 5) must appear after an absent lemma
        // NOT in any deficient band (Tier 4). T5 < T4 in priority because Tier 4 is macro-scarcity.
        //
        // Fixture (threshold=3):
        //   ENRICH band: cocaRank 1..500
        //   "lemma-absent-noband"  COMPLETELY_ABSENT + not in band (rank=600) → Tier 4 (no band, absent)
        //   "lemma-scarce-band"    count=3 (==N=3, expuesto-escaso) + in band (rank=100)  → Tier 5
        //   "lemma-scarce-noband"  count=4 (N..2N-1=5, expuesto-escaso) + not in band (rank=200) → Tier 6
        //
        // Expected order: T4 → T5 → T6

        AuditNode courseNode = buildCourseNodeWithEnrichBands("A1", new int[][]{{1, 500}});
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-r014d", null, null, "Ella camina.", List.of("She walks."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Movement", "Complete.", true, "know-r014d", null, null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-r014d", "Body", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-r014d", "A1", null);

        AbsentLemma absentNoBand = new AbsentLemma(
                new LemmaAndPos("lemma-absent-noband", "NOUN"), CefrLevel.A1,
                AbsenceType.COMPLETELY_ABSENT, List.of(), PriorityLevel.HIGH, 600, null);
        AbsentLemma scarceBand = new AbsentLemma(
                new LemmaAndPos("lemma-scarce-band", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 100, null);
        AbsentLemma scarceNoBand = new AbsentLemma(
                new LemmaAndPos("lemma-scarce-noband", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 200, null);

        LemmaAbsenceLevelDiagnosis absenceDiag = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 100, 3, 10.0, 90.0, 0.8, 0.7, 0.5, null,
                List.of(absentNoBand, scarceBand, scarceNoBand),
                List.of(), 2, 3, 5);

        // threshold=3 (N=3, 2N-1=5)
        // lemma-scarce-band: count=3 (== N), in band → exposed-scarce+band (T5)
        // lemma-scarce-noband: count=4 (N..2N-1), not in band → exposed-scarce (T6)
        LemmaCountStats statsScarceBand = new LemmaCountStats(
                new LemmaAndPos("lemma-scarce-band", "NOUN"), 3, 1.0, Optional.of(CefrLevel.A1));
        LemmaCountStats statsScarceNoBand = new LemmaCountStats(
                new LemmaAndPos("lemma-scarce-noband", "NOUN"), 4, 1.33, Optional.of(CefrLevel.A1));

        LevelLemmaCountResult levelResult = new LevelLemmaCountResult(
                CefrLevel.A1, 1.17, 3, List.of(statsScarceBand, statsScarceNoBand));
        LemmaCountLevelDiagnosis lemmaCountDiag = new LemmaCountLevelDiagnosis(levelResult);

        DefaultLevelDiagnoses milestoneDiag = new DefaultLevelDiagnoses();
        milestoneDiag.setLemmaAbsenceDiagnosis(absenceDiag);
        milestoneDiag.setLemmaCountDiagnosis(lemmaCountDiag);

        AuditNode milestoneNode = buildMilestoneNode(courseNode, milestone, milestoneDiag);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        buildQuizNode(knowledgeNode, quiz,
                buildQuizDiagnosesWithPlacement(List.of(buildMisplacedLemma("walk", "VERB", CefrLevel.A2, CefrLevel.A1, 300))));

        AuditReport report = new AuditReport(courseNode);
        RefinementTask task = new RefinementTask(
                "task-r014d", AuditTarget.QUIZ, "quiz-r014d",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();
        Assertions.assertEquals(3, lemmas.size());

        // T4 (absent, no band) before T5 (scarce+band) before T6 (scarce, no band)
        Assertions.assertEquals("lemma-absent-noband", lemmas.get(0).getLemma(),
                "Tier 4 (absent, no band) must be before Tier 5 (exposed-scarce + band) — T5/T6 are always after T1-T4 (R014)");
        Assertions.assertEquals("lemma-scarce-band", lemmas.get(1).getLemma(),
                "Tier 5 (exposed-scarce + in band) must be second");
        Assertions.assertEquals("lemma-scarce-noband", lemmas.get(2).getLemma(),
                "Tier 6 (exposed-scarce, no band) must be last");
    }

    @Test
    @DisplayName("should degrade to tier order three then four then six with COCA ascending as the final tiebreak and without failing when the audit did not analyze COCA frequency bands so the band tiers stay empty")
    @Tag("FEAT-LEMMA-SUGGESTIONS")
    @Tag("F-SLEM-R015")
    public void shouldDegradeToTierOrderThreeThenFourThenSixWithCOCAAscendingAsTheFinalTiebreakAndWithoutFailingWhenTheAuditDidNotAnalyzeCOCAFrequencyBandsSoTheBandTiersStayEmpty() {
        // R015: when the audit has no CocaProgressionDiagnosis on the course node (no band analysis),
        // Tiers 1, 2, 5 are empty (no lemma qualifies for them) and the resolver degrades gracefully to:
        //   T3 (sub-expuesto) → T4 (absent) → T6 (exposed-scarce) with COCA asc as the final tiebreak.
        // No exception must be thrown.
        //
        // Fixture (threshold=3, NO band analysis — plain buildCourseNode with no CocaProgressionDiagnosis):
        //   "lemma-under-a" sub-expuesto count=1 (rank=300) → T3
        //   "lemma-under-b" sub-expuesto count=2 (rank=100) → T3 (COCA asc resolves: 100 before 300)
        //   "lemma-absent"  COMPLETELY_ABSENT        (rank=50)  → T4
        //   "lemma-scarce"  count=4 (N..2N-1=5)     (rank=150) → T6
        //
        // Expected: lemma-under-b (T3, rank=100), lemma-under-a (T3, rank=300), lemma-absent (T4, rank=50), lemma-scarce (T6, rank=150)

        AuditNode courseNode = buildCourseNode(); // NO CocaProgressionDiagnosis
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-r015", null, null, "Ella bebe.", List.of("She drinks."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Habits", "Complete.", true, "know-r015", null, null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-r015", "Daily Routine", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-r015", "A1", null);

        AbsentLemma underA = new AbsentLemma(
                new LemmaAndPos("lemma-under-a", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 300, null);
        AbsentLemma underB = new AbsentLemma(
                new LemmaAndPos("lemma-under-b", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 100, null);
        AbsentLemma absent = new AbsentLemma(
                new LemmaAndPos("lemma-absent", "NOUN"), CefrLevel.A1,
                AbsenceType.COMPLETELY_ABSENT, List.of(), PriorityLevel.HIGH, 50, null);
        AbsentLemma scarce = new AbsentLemma(
                new LemmaAndPos("lemma-scarce", "NOUN"), CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 150, null);

        LemmaAbsenceLevelDiagnosis absenceDiag = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 100, 4, 10.0, 90.0, 0.8, 0.7, 0.5, null,
                List.of(underA, underB, absent, scarce),
                List.of(), 2, 4, 6);

        // threshold=3 (N=3, 2N-1=5)
        LemmaCountStats statsUnderA = new LemmaCountStats(
                new LemmaAndPos("lemma-under-a", "NOUN"), 1, 0.33, Optional.of(CefrLevel.A1));
        LemmaCountStats statsUnderB = new LemmaCountStats(
                new LemmaAndPos("lemma-under-b", "NOUN"), 2, 0.67, Optional.of(CefrLevel.A1));
        LemmaCountStats statsScarce = new LemmaCountStats(
                new LemmaAndPos("lemma-scarce", "NOUN"), 4, 1.33, Optional.of(CefrLevel.A1));

        LevelLemmaCountResult levelResult = new LevelLemmaCountResult(
                CefrLevel.A1, 0.78, 3, List.of(statsUnderA, statsUnderB, statsScarce));
        LemmaCountLevelDiagnosis lemmaCountDiag = new LemmaCountLevelDiagnosis(levelResult);

        DefaultLevelDiagnoses milestoneDiag = new DefaultLevelDiagnoses();
        milestoneDiag.setLemmaAbsenceDiagnosis(absenceDiag);
        milestoneDiag.setLemmaCountDiagnosis(lemmaCountDiag);

        AuditNode milestoneNode = buildMilestoneNode(courseNode, milestone, milestoneDiag);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        buildQuizNode(knowledgeNode, quiz,
                buildQuizDiagnosesWithPlacement(List.of(buildMisplacedLemma("drink", "VERB", CefrLevel.A2, CefrLevel.A1, 400))));

        AuditReport report = new AuditReport(courseNode);
        RefinementTask task = new RefinementTask(
                "task-r015", AuditTarget.QUIZ, "quiz-r015",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        // Must not throw even though there is no CocaProgressionDiagnosis
        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        List<SuggestedLemma> lemmas = result.get().getSuggestedLemmas();
        Assertions.assertEquals(4, lemmas.size());

        // Degraded order: T3 → T3 → T4 → T6 (no T1/T2/T5 since no bands)
        // Within T3 (both sub-expuesto): COCA ascending → lemma-under-b (rank=100) before lemma-under-a (rank=300)
        Assertions.assertEquals("lemma-under-b", lemmas.get(0).getLemma(),
                "T3 with lower COCA rank (100) must be first — COCA asc within tier (R015)");
        Assertions.assertEquals("lemma-under-a", lemmas.get(1).getLemma(),
                "T3 with higher COCA rank (300) must be second");
        Assertions.assertEquals("lemma-absent", lemmas.get(2).getLemma(),
                "T4 (COMPLETELY_ABSENT, no bands) must be third — degrades to T3→T4→T6 (R015)");
        Assertions.assertEquals("lemma-scarce", lemmas.get(3).getLemma(),
                "T6 (exposed-scarce, no bands) must be last — T6 is the last tier in degraded mode (R015)");
    }

    @Test
    @DisplayName("should include in scarceContentWords only the current sentence content words whose lemma-count is strictly below the threshold and exclude those whose count is greater than or equal to the threshold")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R003b")
    public void shouldIncludeInScarceContentWordsOnlyTheCurrentSentenceContentWordsWhoseLemmacountIsStrictlyBelowTheThresholdAndExcludeThoseWhoseCountIsGreaterThanOrEqualToTheThreshold() {
        // R003b: scarceContentWords includes only content words (NOUN/VERB/ADJ/ADV) of the current
        // sentence whose lemma-count (global exposure in the milestone) is strictly < threshold.
        // Words with count >= threshold must NOT appear in scarceContentWords.
        //
        // Sentence NlpTokens:
        //   "run"  → VERB, lemma="run",  count=1  (<3) → scarce  ✓
        //   "fast" → ADV,  lemma="fast", count=4  (>=3) → NOT scarce ✗
        //   "the"  → DET,  lemma="the",  count=10 (>=3) → not content word, excluded ✗
        //   "dog"  → NOUN, lemma="dog",  count=2  (<3) → scarce ✓

        List<NlpToken> tokens = List.of(
                new NlpToken("the",  "the",  "DET",  null, true,  false),
                new NlpToken("dog",  "dog",  "NOUN", 200,  false, false),
                new NlpToken("runs", "run",  "VERB", 100,  false, false),
                new NlpToken("fast", "fast", "ADV",  300,  false, false)
        );

        AuditableQuiz quiz = new AuditableQuiz(tokens, "quiz-r003b-filter", null, null,
                "The dog runs fast", List.of("The dog runs fast."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Animals", "Complete.", true, "know-r003b-filter", null, null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-r003b-filter", "Nature", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-r003b-filter", "A1", null);

        AbsentLemma absentRun = new AbsentLemma(
                new LemmaAndPos("run", "VERB"), CefrLevel.A1,
                AbsenceType.COMPLETELY_ABSENT, List.of(), PriorityLevel.HIGH, 100, null);

        LemmaAbsenceLevelDiagnosis absenceDiag = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 100, 1, 10.0, 90.0, 0.8, 0.7, 0.5, null,
                List.of(absentRun), List.of(), 1, 1, 2);

        // threshold=3; run=1 (<3), dog=2 (<3), fast=4 (>=3), the=10 (>=3, also not content word)
        LemmaCountStats statsRun  = new LemmaCountStats(new LemmaAndPos("run",  "VERB"), 1,  0.33, Optional.of(CefrLevel.A1));
        LemmaCountStats statsDog  = new LemmaCountStats(new LemmaAndPos("dog",  "NOUN"), 2,  0.67, Optional.of(CefrLevel.A1));
        LemmaCountStats statsFast = new LemmaCountStats(new LemmaAndPos("fast", "ADV"),  4,  1.33, Optional.of(CefrLevel.A1));
        LemmaCountStats statsThe  = new LemmaCountStats(new LemmaAndPos("the",  "DET"),  10, 3.33, Optional.of(CefrLevel.A1));

        LevelLemmaCountResult levelResult = new LevelLemmaCountResult(
                CefrLevel.A1, 0.83, 3, List.of(statsRun, statsDog, statsFast, statsThe));
        LemmaCountLevelDiagnosis lemmaCountDiag = new LemmaCountLevelDiagnosis(levelResult);

        DefaultLevelDiagnoses milestoneDiag = new DefaultLevelDiagnoses();
        milestoneDiag.setLemmaAbsenceDiagnosis(absenceDiag);
        milestoneDiag.setLemmaCountDiagnosis(lemmaCountDiag);

        AuditNode milestoneNode = buildMilestoneNode(buildCourseNode(), milestone, milestoneDiag);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        buildQuizNode(knowledgeNode, quiz, buildQuizDiagnosesWithPlacement(List.of()));

        AuditReport report = new AuditReport(milestoneNode.getParent());
        RefinementTask task = new RefinementTask(
                "task-r003b-filter", AuditTarget.QUIZ, "quiz-r003b-filter",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        List<ScarceContentWord> scarce = result.get().getScarceContentWords();

        // Only run (VERB, count=1) and dog (NOUN, count=2) qualify — both < threshold=3
        Assertions.assertNotNull(scarce, "scarceContentWords must not be null");
        Assertions.assertEquals(2, scarce.size(),
                "Only 'run' and 'dog' (count < 3) must be included; 'fast' (count=4>=3) and 'the' (DET) are excluded (R003b)");

        List<String> scarcelemmas = scarce.stream().map(ScarceContentWord::getLemma).toList();
        Assertions.assertTrue(scarcelemmas.contains("run"), "'run' (count=1 < 3) must be scarce");
        Assertions.assertTrue(scarcelemmas.contains("dog"), "'dog' (count=2 < 3) must be scarce");
        Assertions.assertFalse(scarcelemmas.contains("fast"), "'fast' (count=4 >= 3) must NOT be scarce (R003b)");
        Assertions.assertFalse(scarcelemmas.contains("the"), "'the' (DET, not content word) must NOT appear (R003b)");
    }

    @Test
    @DisplayName("should populate each scarceContentWords entry with lemma pos count and threshold where count is the global exposure and threshold is the shared lemma-count threshold")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R003b")
    public void shouldPopulateEachScarceContentWordsEntryWithLemmaPosCountAndThresholdWhereCountIsTheGlobalExposureAndThresholdIsTheSharedLemmacountThreshold() {
        // R003b: each ScarceContentWord must carry:
        //   lemma  — the lemma text of the token
        //   pos    — the POS tag of the token
        //   count  — the global exposure (from LemmaCountStats.getCount())
        //   threshold — the shared lemma-count threshold (from LevelLemmaCountResult.getThreshold())
        //
        // Fixture: sentence has one scarce content word "cat" (NOUN, count=2, threshold=5)

        List<NlpToken> tokens = List.of(
                new NlpToken("the", "the",  "DET",  null, true,  false),
                new NlpToken("cat", "cat",  "NOUN", 150,  false, false)
        );

        AuditableQuiz quiz = new AuditableQuiz(tokens, "quiz-r003b-fields", null, null,
                "The cat", List.of("The cat."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Animals", "Complete.", true, "know-r003b-fields", null, null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-r003b-fields", "Nature", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-r003b-fields", "A1", null);

        AbsentLemma absentCat = new AbsentLemma(
                new LemmaAndPos("cat", "NOUN"), CefrLevel.A1,
                AbsenceType.COMPLETELY_ABSENT, List.of(), PriorityLevel.HIGH, 150, null);

        LemmaAbsenceLevelDiagnosis absenceDiag = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 100, 1, 10.0, 90.0, 0.8, 0.7, 0.5, null,
                List.of(absentCat), List.of(), 1, 1, 2);

        // threshold=5; cat=2 (<5) → scarce
        LemmaCountStats statsCat = new LemmaCountStats(
                new LemmaAndPos("cat", "NOUN"), 2, 0.40, Optional.of(CefrLevel.A1));

        LevelLemmaCountResult levelResult = new LevelLemmaCountResult(
                CefrLevel.A1, 0.40, 5, List.of(statsCat));
        LemmaCountLevelDiagnosis lemmaCountDiag = new LemmaCountLevelDiagnosis(levelResult);

        DefaultLevelDiagnoses milestoneDiag = new DefaultLevelDiagnoses();
        milestoneDiag.setLemmaAbsenceDiagnosis(absenceDiag);
        milestoneDiag.setLemmaCountDiagnosis(lemmaCountDiag);

        AuditNode milestoneNode = buildMilestoneNode(buildCourseNode(), milestone, milestoneDiag);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        buildQuizNode(knowledgeNode, quiz, buildQuizDiagnosesWithPlacement(List.of()));

        AuditReport report = new AuditReport(milestoneNode.getParent());
        RefinementTask task = new RefinementTask(
                "task-r003b-fields", AuditTarget.QUIZ, "quiz-r003b-fields",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        List<ScarceContentWord> scarce = result.get().getScarceContentWords();
        Assertions.assertNotNull(scarce);
        Assertions.assertEquals(1, scarce.size(), "Exactly one scarce content word ('cat') must be present");

        ScarceContentWord entry = scarce.get(0);
        Assertions.assertEquals("cat", entry.getLemma(), "lemma must be 'cat' (R003b)");
        Assertions.assertEquals("NOUN", entry.getPos(), "pos must be 'NOUN' (R003b)");
        Assertions.assertEquals(2, entry.getCount(), "count must be the global exposure = 2 (R003b)");
        Assertions.assertEquals(5, entry.getThreshold(), "threshold must be the shared lemma-count threshold = 5 (R003b)");
    }

    @Test
    @DisplayName("should return an empty scarceContentWords list when the audit did not include lemma-count so no exposure signal is available")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R003c")
    public void shouldReturnAnEmptyScarceContentWordsListWhenTheAuditDidNotIncludeLemmacountSoNoExposureSignalIsAvailable() {
        // R003c: when the milestone has no LemmaCountLevelDiagnosis (lemma-count analysis was not run),
        // scarceContentWords must be an empty list — not null, not a missing field — just empty.
        // The resolver must not throw when there is no exposure signal.
        //
        // Fixture: quiz has content-word tokens, but milestone has NO LemmaCountLevelDiagnosis.

        List<NlpToken> tokens = List.of(
                new NlpToken("the", "the",  "DET",  null, true,  false),
                new NlpToken("dog", "dog",  "NOUN", 200,  false, false),
                new NlpToken("barks", "bark", "VERB", 300, false, false)
        );

        AuditableQuiz quiz = new AuditableQuiz(tokens, "quiz-r003c-no-signal", null, null,
                "The dog barks", List.of("The dog barks."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Animals", "Complete.", true, "know-r003c-no-signal", null, null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-r003c-no-signal", "Nature", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-r003c-no-signal", "A1", null);

        AbsentLemma absentDog = new AbsentLemma(
                new LemmaAndPos("dog", "NOUN"), CefrLevel.A1,
                AbsenceType.COMPLETELY_ABSENT, List.of(), PriorityLevel.HIGH, 200, null);

        LemmaAbsenceLevelDiagnosis absenceDiag = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 100, 1, 10.0, 90.0, 0.8, 0.7, 0.5, null,
                List.of(absentDog), List.of(), 1, 1, 2);

        // NO lemma-count diagnosis set on the milestone
        DefaultLevelDiagnoses milestoneDiag = new DefaultLevelDiagnoses();
        milestoneDiag.setLemmaAbsenceDiagnosis(absenceDiag);
        // milestoneDiag.setLemmaCountDiagnosis(...) intentionally omitted

        AuditNode milestoneNode = buildMilestoneNode(buildCourseNode(), milestone, milestoneDiag);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        buildQuizNode(knowledgeNode, quiz, buildQuizDiagnosesWithPlacement(List.of()));

        AuditReport report = new AuditReport(milestoneNode.getParent());
        RefinementTask task = new RefinementTask(
                "task-r003c-no-signal", AuditTarget.QUIZ, "quiz-r003c-no-signal",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        List<ScarceContentWord> scarce = result.get().getScarceContentWords();
        Assertions.assertNotNull(scarce, "scarceContentWords must not be null when no signal is available (R003c)");
        Assertions.assertTrue(scarce.isEmpty(),
                "scarceContentWords must be empty when no LemmaCountLevelDiagnosis exists — no exposure signal (R003c)");
    }

    @Test
    @DisplayName("should return an empty scarceContentWords list when no content word of the current sentence is below the lemma-count threshold or the sentence has no content words")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R003c")
    public void shouldReturnAnEmptyScarceContentWordsListWhenNoContentWordOfTheCurrentSentenceIsBelowTheLemmacountThresholdOrTheSentenceHasNoContentWords() {
        // R003c: scarceContentWords must be empty when none of the sentence's content words
        // has count < threshold. Also empty if the sentence has no content words (only stop words / punctuation).
        //
        // Fixture A (all content words at or above threshold):
        //   Sentence: "The cat sleeps" — cat (NOUN, count=5>=3), sleeps/sleep (VERB, count=7>=3)
        //   Neither is scarce → empty scarceContentWords

        List<NlpToken> tokens = List.of(
                new NlpToken("The",    "the",   "DET",  null, true,  false),
                new NlpToken("cat",    "cat",   "NOUN", 150,  false, false),
                new NlpToken("sleeps", "sleep", "VERB", 200,  false, false)
        );

        AuditableQuiz quiz = new AuditableQuiz(tokens, "quiz-r003c-none-scarce", null, null,
                "The cat sleeps", List.of("The cat sleeps."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Animals", "Complete.", true, "know-r003c-none-scarce", null, null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-r003c-none-scarce", "Nature", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-r003c-none-scarce", "A1", null);

        AbsentLemma absentWord = new AbsentLemma(
                new LemmaAndPos("cat", "NOUN"), CefrLevel.A1,
                AbsenceType.COMPLETELY_ABSENT, List.of(), PriorityLevel.HIGH, 150, null);

        LemmaAbsenceLevelDiagnosis absenceDiag = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 100, 1, 10.0, 90.0, 0.8, 0.7, 0.5, null,
                List.of(absentWord), List.of(), 1, 1, 2);

        // threshold=3; cat=5 (>=3), sleep=7 (>=3) — neither is scarce
        LemmaCountStats statsCat   = new LemmaCountStats(new LemmaAndPos("cat",   "NOUN"), 5, 1.67, Optional.of(CefrLevel.A1));
        LemmaCountStats statsSleep = new LemmaCountStats(new LemmaAndPos("sleep", "VERB"), 7, 2.33, Optional.of(CefrLevel.A1));

        LevelLemmaCountResult levelResult = new LevelLemmaCountResult(
                CefrLevel.A1, 2.0, 3, List.of(statsCat, statsSleep));
        LemmaCountLevelDiagnosis lemmaCountDiag = new LemmaCountLevelDiagnosis(levelResult);

        DefaultLevelDiagnoses milestoneDiag = new DefaultLevelDiagnoses();
        milestoneDiag.setLemmaAbsenceDiagnosis(absenceDiag);
        milestoneDiag.setLemmaCountDiagnosis(lemmaCountDiag);

        AuditNode milestoneNode = buildMilestoneNode(buildCourseNode(), milestone, milestoneDiag);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        buildQuizNode(knowledgeNode, quiz, buildQuizDiagnosesWithPlacement(List.of()));

        AuditReport report = new AuditReport(milestoneNode.getParent());
        RefinementTask task = new RefinementTask(
                "task-r003c-none-scarce", AuditTarget.QUIZ, "quiz-r003c-none-scarce",
                "Quiz label", DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);

        Optional<LemmaAbsenceCorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isPresent());
        List<ScarceContentWord> scarce = result.get().getScarceContentWords();
        Assertions.assertNotNull(scarce, "scarceContentWords must not be null (R003c)");
        Assertions.assertTrue(scarce.isEmpty(),
                "scarceContentWords must be empty when all content words have count >= threshold (R003c)");
    }
}
