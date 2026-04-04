package com.learney.contentaudit.auditcli;

import static org.junit.jupiter.api.Assertions.*;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.labs.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class LemmaAbsenceDetailedFormatterTest {

    private LemmaAbsenceDetailedFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new LemmaAbsenceDetailedFormatter();
    }

    // ── Helper builders ──────────────────────────────────────────────────────

    /** Build an AuditNode with initialized collections and optional entity/parent. */
    private AuditNode makeNode(AuditTarget target, AuditableEntity entity, AuditNode parent) {
        AuditNode node = new AuditNode();
        node.setTarget(target);
        node.setEntity(entity);
        node.setParent(parent);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        if (parent != null) parent.getChildren().add(node);
        return node;
    }

    private AuditableMilestone milestone(String id, String label) {
        return new AuditableMilestone(List.of(), id, label, null);
    }

    private AuditableTopic topic(String id, String label) {
        return new AuditableTopic(List.of(), id, label, null);
    }

    private AuditableKnowledge knowledge(String id, String label) {
        return new AuditableKnowledge(List.of(), "title", null, true, id, label, null);
    }

    private AuditableQuiz quiz(String id) {
        return new AuditableQuiz("sentence", List.of(), id, "label", null);
    }

    /**
     * Build a minimal course tree:
     *   COURSE
     *     MILESTONE (A1)
     *       TOPIC (t1)
     *         KNOWLEDGE (k1)
     *           QUIZ (q1)
     */
    private AuditNode buildFullTree() {
        AuditNode course = makeNode(AuditTarget.COURSE, null, null);
        course.getScores().put("lemma-absence", 0.75);

        AuditNode milestoneNode = makeNode(AuditTarget.MILESTONE, milestone("m1", "A1"), course);
        milestoneNode.getScores().put("lemma-absence", 0.80);

        AuditNode topicNode = makeNode(AuditTarget.TOPIC, topic("t1", "Grammar Topic"), milestoneNode);
        topicNode.getScores().put("lemma-absence", 0.70);

        AuditNode knowledgeNode = makeNode(AuditTarget.KNOWLEDGE, knowledge("k1", "Verb to be"), topicNode);
        knowledgeNode.getScores().put("lemma-absence", 0.65);

        AuditNode quizNode = makeNode(AuditTarget.QUIZ, quiz("q1"), knowledgeNode);
        quizNode.getScores().put("lemma-absence", 0.60);

        return course;
    }

    private DefaultCourseDiagnoses courseWithAssessment(AbsenceAssessment assessment) {
        DefaultCourseDiagnoses cd = new DefaultCourseDiagnoses();
        LemmaAbsenceCourseDiagnosis diag = new LemmaAbsenceCourseDiagnosis(assessment);
        cd.setLemmaAbsenceDiagnosis(diag);
        return cd;
    }

    private DefaultLevelDiagnoses levelWithDiagnosis(int totalExpected, int totalAbsent,
            double absencePercentage, double coverageTarget,
            double completelyAbsentScore, double tooLateScore, double tooEarlyScore,
            List<AbsentLemma> absentLemmas) {
        DefaultLevelDiagnoses ld = new DefaultLevelDiagnoses();
        LemmaAbsenceLevelDiagnosis diag = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, totalExpected, totalAbsent, absencePercentage, coverageTarget,
                completelyAbsentScore, tooLateScore, tooEarlyScore,
                AbsenceAssessment.ACCEPTABLE, absentLemmas, List.of(),
                0, 0, 0);
        ld.setLemmaAbsenceDiagnosis(diag);
        return ld;
    }

    private DefaultTopicDiagnoses topicWithMisplaced(int count, List<MisplacedLemma> lemmas) {
        DefaultTopicDiagnoses td = new DefaultTopicDiagnoses();
        LemmaPlacementDiagnosis diag = new LemmaPlacementDiagnosis(count, lemmas);
        td.setLemmaAbsenceDiagnosis(diag);
        return td;
    }

    private DefaultKnowledgeDiagnoses knowledgeWithMisplaced(int count, List<MisplacedLemma> lemmas) {
        DefaultKnowledgeDiagnoses kd = new DefaultKnowledgeDiagnoses();
        LemmaPlacementDiagnosis diag = new LemmaPlacementDiagnosis(count, lemmas);
        kd.setLemmaAbsenceDiagnosis(diag);
        return kd;
    }

    private DefaultQuizDiagnoses quizWithMisplaced(int count, List<MisplacedLemma> lemmas) {
        DefaultQuizDiagnoses qd = new DefaultQuizDiagnoses();
        LemmaPlacementDiagnosis diag = new LemmaPlacementDiagnosis(count, lemmas);
        qd.setLemmaAbsenceDiagnosis(diag);
        return qd;
    }

    private AbsentLemma makeAbsentLemma(String lemma, String pos, AbsenceType type, PriorityLevel priority) {
        return new AbsentLemma(new LemmaAndPos(lemma, pos), CefrLevel.A1, type, List.of(), priority, 500, null);
    }

    private MisplacedLemma makeMisplacedLemma(String lemma, String pos, CefrLevel expectedLevel) {
        return new MisplacedLemma(new LemmaAndPos(lemma, pos), expectedLevel, CefrLevel.B1,
                AbsenceType.APPEARS_TOO_LATE, 1500, null);
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("should format text output from typed diagnoses matching previous metadata-based output")
    @Tag("FEAT-DLABS")
    @Tag("F-DLABS-R013")
    @Tag("F-DLABS-J003")
    public void shouldFormatTextOutputFromTypedDiagnosesMatchingPreviousMetadatabasedOutput() {
        AuditNode course = buildFullTree();

        // Set course-level diagnosis
        course.setDiagnoses(courseWithAssessment(AbsenceAssessment.NEEDS_IMPROVEMENT));

        // Set milestone-level diagnosis
        AuditNode milestoneNode = course.getChildren().get(0);
        AbsentLemma absentLemma = makeAbsentLemma("go", "VERB", AbsenceType.COMPLETELY_ABSENT, PriorityLevel.HIGH);
        milestoneNode.setDiagnoses(levelWithDiagnosis(
                100, 10, 10.0, 0.90, 0.85, 0.80, 0.75, List.of(absentLemma)));

        // Set topic-level diagnosis
        AuditNode topicNode = milestoneNode.getChildren().get(0);
        MisplacedLemma misplacedLemma = makeMisplacedLemma("run", "VERB", CefrLevel.A1);
        topicNode.setDiagnoses(topicWithMisplaced(1, List.of(misplacedLemma)));

        // Set knowledge-level diagnosis
        AuditNode knowledgeNode = topicNode.getChildren().get(0);
        knowledgeNode.setDiagnoses(knowledgeWithMisplaced(1, List.of(misplacedLemma)));

        String result = formatter.format("lemma-absence", course, "text");

        assertNotNull(result);
        assertTrue(result.contains("NEEDS_IMPROVEMENT"), "Assessment should appear in text output");
        assertTrue(result.contains("go"), "Absent lemma should appear in text output");
        assertTrue(result.contains("COMPLETELY_ABSENT"), "Absence type should appear in text output");
        assertTrue(result.contains("HIGH"), "Priority level should appear in text output");
        assertTrue(result.contains("run"), "Misplaced lemma should appear in text output");
        assertTrue(result.contains("Lemma Absence Analysis"), "Header should appear in text output");
        assertTrue(result.contains("A1"), "Level label should appear in text output");
    }

    @Test
    @DisplayName("should format json output from typed diagnoses matching previous metadata-based output")
    @Tag("FEAT-DLABS")
    @Tag("F-DLABS-R013")
    @Tag("F-DLABS-J003")
    public void shouldFormatJsonOutputFromTypedDiagnosesMatchingPreviousMetadatabasedOutput() {
        AuditNode course = buildFullTree();

        course.setDiagnoses(courseWithAssessment(AbsenceAssessment.ACCEPTABLE));

        AuditNode milestoneNode = course.getChildren().get(0);
        AbsentLemma absentLemma = makeAbsentLemma("have", "VERB", AbsenceType.APPEARS_TOO_LATE, PriorityLevel.MEDIUM);
        milestoneNode.setDiagnoses(levelWithDiagnosis(
                50, 5, 10.0, 0.85, 0.90, 0.70, 0.80, List.of(absentLemma)));

        AuditNode topicNode = milestoneNode.getChildren().get(0);
        MisplacedLemma misplacedLemma = makeMisplacedLemma("make", "VERB", CefrLevel.A1);
        topicNode.setDiagnoses(topicWithMisplaced(1, List.of(misplacedLemma)));

        AuditNode knowledgeNode = topicNode.getChildren().get(0);
        knowledgeNode.setDiagnoses(knowledgeWithMisplaced(1, List.of(misplacedLemma)));

        AuditNode quizNode = knowledgeNode.getChildren().get(0);
        quizNode.setDiagnoses(quizWithMisplaced(0, List.of()));

        String result = formatter.format("lemma-absence", course, "json");

        assertNotNull(result);
        assertTrue(result.contains("\"score\":"), "JSON should contain score field");
        assertTrue(result.contains("\"assessment\":"), "JSON should contain assessment field");
        assertTrue(result.contains("ACCEPTABLE"), "Assessment value should appear in JSON");
        assertTrue(result.contains("\"levels\":"), "JSON should contain levels array");
        assertTrue(result.contains("\"coverageTarget\":"), "JSON should contain coverageTarget");
        assertTrue(result.contains("\"totalExpected\": 50"), "JSON should contain totalExpected");
        assertTrue(result.contains("\"totalAbsent\": 5"), "JSON should contain totalAbsent");
        assertTrue(result.contains("have"), "Absent lemma should appear in JSON");
        assertTrue(result.contains("APPEARS_TOO_LATE"), "Absence type should appear in JSON");
        assertTrue(result.contains("make"), "Misplaced lemma should appear in JSON");
        assertTrue(result.contains("\"topics\":"), "JSON should contain topics");
        assertTrue(result.contains("\"knowledges\":"), "JSON should contain knowledges");
        assertTrue(result.contains("\"quizzes\":"), "JSON should contain quizzes");
    }

    @Test
    @DisplayName("should format table output from typed diagnoses matching previous metadata-based output")
    @Tag("FEAT-DLABS")
    @Tag("F-DLABS-R013")
    @Tag("F-DLABS-J003")
    public void shouldFormatTableOutputFromTypedDiagnosesMatchingPreviousMetadatabasedOutput() {
        AuditNode course = buildFullTree();

        course.setDiagnoses(courseWithAssessment(AbsenceAssessment.OPTIMAL));

        AuditNode milestoneNode = course.getChildren().get(0);
        milestoneNode.setDiagnoses(levelWithDiagnosis(
                200, 3, 1.5, 0.95, 0.98, 0.97, 0.96, List.of()));

        AuditNode topicNode = milestoneNode.getChildren().get(0);
        MisplacedLemma misplacedLemma = makeMisplacedLemma("be", "VERB", CefrLevel.A1);
        topicNode.setDiagnoses(topicWithMisplaced(2, List.of(misplacedLemma, misplacedLemma)));

        String result = formatter.format("lemma-absence", course, "table");

        assertNotNull(result);
        assertTrue(result.contains("OPTIMAL"), "Assessment should appear in table output");
        assertTrue(result.contains("Level"), "Table header should appear");
        assertTrue(result.contains("Score"), "Table header should appear");
        assertTrue(result.contains("A1"), "Level label should appear in table");
        // Topic with misplaced count shown as label(count) format
        assertTrue(result.contains("Grammar Topic(2)"), "Topic misplaced count should appear in table");
    }

    @Test
    @DisplayName("should read typed diagnoses from course milestone and quiz nodes for formatting")
    @Tag("FEAT-DLABS")
    @Tag("F-DLABS-R004, F-DLABS-R005, F-DLABS-R009")
    @Tag("F-DLABS-J003")
    public void shouldReadTypedDiagnosesFromCourseMilestoneAndQuizNodesForFormatting() {
        AuditNode course = buildFullTree();

        // Course-level diagnoses
        DefaultCourseDiagnoses cd = courseWithAssessment(AbsenceAssessment.NEEDS_IMPROVEMENT);
        course.setDiagnoses(cd);

        // Milestone-level diagnoses
        AuditNode milestoneNode = course.getChildren().get(0);
        AbsentLemma absentLemma = makeAbsentLemma("take", "VERB", AbsenceType.COMPLETELY_ABSENT, PriorityLevel.LOW);
        DefaultLevelDiagnoses ld = levelWithDiagnosis(300, 30, 10.0, 0.80, 0.75, 0.70, 0.65, List.of(absentLemma));
        milestoneNode.setDiagnoses(ld);

        // Quiz-level diagnoses
        AuditNode topicNode = milestoneNode.getChildren().get(0);
        topicNode.setDiagnoses(topicWithMisplaced(0, List.of()));

        AuditNode knowledgeNode = topicNode.getChildren().get(0);
        knowledgeNode.setDiagnoses(knowledgeWithMisplaced(0, List.of()));

        AuditNode quizNode = knowledgeNode.getChildren().get(0);
        MisplacedLemma quizMisplaced = makeMisplacedLemma("know", "VERB", CefrLevel.A1);
        quizNode.setDiagnoses(quizWithMisplaced(1, List.of(quizMisplaced)));

        // Format as JSON to verify all levels are read
        String jsonResult = formatter.format("lemma-absence", course, "json");

        // Verify course-level assessment was read from CourseDiagnoses
        assertTrue(jsonResult.contains("NEEDS_IMPROVEMENT"), "Course assessment should be read from typed diagnoses");
        // Verify milestone-level data was read from LevelDiagnoses
        assertTrue(jsonResult.contains("\"totalExpected\": 300"), "totalExpected should come from LevelDiagnoses");
        assertTrue(jsonResult.contains("\"totalAbsent\": 30"), "totalAbsent should come from LevelDiagnoses");
        assertTrue(jsonResult.contains("take"), "Absent lemma should come from LevelDiagnoses.absentLemmas");
        // Verify quiz-level misplaced lemmas were read from QuizDiagnoses
        assertTrue(jsonResult.contains("know"), "Misplaced lemma at quiz level should come from QuizDiagnoses");
    }

    @Test
    @DisplayName("should handle missing diagnosis gracefully when analyzer did not produce results")
    @Tag("FEAT-DLABS")
    @Tag("F-DLABS-R003")
    @Tag("F-DLABS-J003")
    public void shouldHandleMissingDiagnosisGracefullyWhenAnalyzerDidNotProduceResults() {
        // Build a tree with NO diagnoses set on any node
        AuditNode course = buildFullTree();
        // diagnoses remain null on all nodes

        // Should not throw, should produce output with defaults (0 scores, N/A assessment)
        assertDoesNotThrow(() -> {
            String textResult = formatter.format("lemma-absence", course, "text");
            assertNotNull(textResult);
            assertTrue(textResult.contains("N/A"), "Missing course diagnosis should produce N/A assessment");
        });

        assertDoesNotThrow(() -> {
            String jsonResult = formatter.format("lemma-absence", course, "json");
            assertNotNull(jsonResult);
            assertTrue(jsonResult.contains("\"assessment\": null"), "Missing course diagnosis should produce null assessment in JSON");
        });

        assertDoesNotThrow(() -> {
            String tableResult = formatter.format("lemma-absence", course, "table");
            assertNotNull(tableResult);
            assertTrue(tableResult.contains("N/A"), "Missing course diagnosis should produce N/A in table");
        });
    }

    @Test
    @DisplayName("should navigate from quiz node to milestone ancestor to access level diagnosis")
    @Tag("FEAT-DLABS")
    @Tag("F-DLABS-R011, F-DLABS-R012")
    @Tag("F-DLABS-J002")
    public void shouldNavigateFromQuizNodeToMilestoneAncestorToAccessLevelDiagnosis() {
        // Build a tree: COURSE > MILESTONE > TOPIC > KNOWLEDGE > QUIZ
        AuditNode course = makeNode(AuditTarget.COURSE, null, null);
        AuditNode milestoneNode = makeNode(AuditTarget.MILESTONE, milestone("m1", "A1"), course);
        AuditNode topicNode = makeNode(AuditTarget.TOPIC, topic("t1", "Topic"), milestoneNode);
        AuditNode knowledgeNode = makeNode(AuditTarget.KNOWLEDGE, knowledge("k1", "Knowledge"), topicNode);
        AuditNode quizNode = makeNode(AuditTarget.QUIZ, quiz("q1"), knowledgeNode);

        // Set level diagnosis on milestone
        DefaultLevelDiagnoses ld = new DefaultLevelDiagnoses();
        LemmaAbsenceLevelDiagnosis diag = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 100, 10, 10.0, 0.90, 0.85, 0.80, 0.75,
                AbsenceAssessment.ACCEPTABLE, List.of(), List.of(), 0, 0, 0);
        ld.setLemmaAbsenceDiagnosis(diag);
        milestoneNode.setDiagnoses(ld);

        // Navigate from quiz to milestone ancestor
        Optional<AuditNode> ancestorOpt = quizNode.ancestor(AuditTarget.MILESTONE);

        assertTrue(ancestorOpt.isPresent(), "Quiz node should be able to navigate to milestone ancestor");
        AuditNode foundMilestone = ancestorOpt.get();
        assertEquals(AuditTarget.MILESTONE, foundMilestone.getTarget());

        // Verify the milestone has level diagnoses accessible
        assertTrue(foundMilestone.getDiagnoses() instanceof LevelDiagnoses,
                "Found milestone should have LevelDiagnoses");
        LevelDiagnoses levelDiag = (LevelDiagnoses) foundMilestone.getDiagnoses();
        assertTrue(levelDiag.getLemmaAbsenceDiagnosis().isPresent(),
                "Level diagnosis should be present on found milestone");
        assertEquals(100, levelDiag.getLemmaAbsenceDiagnosis().get().getTotalExpected(),
                "Total expected should be accessible via ancestor navigation");
    }

    @Test
    @DisplayName("should return empty when navigating to nonexistent ancestor level")
    @Tag("FEAT-DLABS")
    @Tag("F-DLABS-R011")
    @Tag("F-DLABS-J002")
    public void shouldReturnEmptyWhenNavigatingToNonexistentAncestorLevel() {
        // Build a shallow tree: COURSE > MILESTONE (no topic/knowledge/quiz)
        AuditNode course = makeNode(AuditTarget.COURSE, null, null);
        AuditNode milestoneNode = makeNode(AuditTarget.MILESTONE, milestone("m1", "A1"), course);

        // Navigate from milestone to a KNOWLEDGE ancestor — should not exist
        Optional<AuditNode> knowledgeAncestor = milestoneNode.ancestor(AuditTarget.KNOWLEDGE);
        assertFalse(knowledgeAncestor.isPresent(),
                "Navigating to a nonexistent ancestor level should return empty Optional");

        // Navigate from course (root, no parent) to any level — should also return empty
        Optional<AuditNode> topicAncestor = course.ancestor(AuditTarget.TOPIC);
        assertFalse(topicAncestor.isPresent(),
                "Navigating from root node should return empty Optional");

        // Navigate from milestone to QUIZ level — also nonexistent
        Optional<AuditNode> quizAncestor = milestoneNode.ancestor(AuditTarget.QUIZ);
        assertFalse(quizAncestor.isPresent(),
                "Navigating to quiz ancestor from milestone should return empty Optional");
    }
}
