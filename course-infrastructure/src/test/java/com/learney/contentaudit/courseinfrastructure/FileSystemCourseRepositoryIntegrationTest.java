package com.learney.contentaudit.courseinfrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.CourseValidationException;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.MilestoneEntity;
import com.learney.contentaudit.coursedomain.NodeKind;
import com.learney.contentaudit.coursedomain.TopicEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for FileSystemCourseRepository.
 *
 * These tests use real JSON fixture data written to a @TempDir and the real
 * CourseValidatorImpl — no mocks. Each test sets up its own fixture structure.
 *
 * IDs follow the pattern used by the real course data:
 *   ROOT id:        "100000000000000000000001"
 *   Milestone id:   "200000000000000000000001" / "200000000000000000000002"
 *   Topic id:       "300000000000000000000001" / "300000000000000000000002"
 *   Knowledge id:   "400000000000000000000001" / "400000000000000000000002"
 *   Quiz id:        "500000000000000000000001"
 */
class FileSystemCourseRepositoryIntegrationTest {

    // -------------------------------------------------------------------------
    // IDs used across fixtures (kept as constants for readability)
    // -------------------------------------------------------------------------

    static final String COURSE_ID  = "000000000000000000000000";
    static final String ROOT_ID    = "100000000000000000000001";
    static final String M1_ID      = "200000000000000000000001";
    static final String M2_ID      = "200000000000000000000002";
    static final String T1_ID      = "300000000000000000000001";
    static final String T2_ID      = "300000000000000000000002";
    static final String K1_ID      = "400000000000000000000001";
    static final String K2_ID      = "400000000000000000000002";
    static final String Q1_ID      = "500000000000000000000001";
    static final String Q2_ID      = "500000000000000000000002";

    static final String M1_OLD_ID  = "11111111-1111-1111-1111-111111111111";
    static final String M2_OLD_ID  = "22222222-2222-2222-2222-222222222222";
    static final String T1_OLD_ID  = "33333333-3333-3333-3333-333333333333";
    static final String T2_OLD_ID  = "44444444-4444-4444-4444-444444444444";
    static final String K1_OLD_ID  = "55555555-5555-5555-5555-555555555555";
    static final String K2_OLD_ID  = "66666666-6666-6666-6666-666666666666";

    private FileSystemCourseRepository sut;

    @BeforeEach
    void setUp() {
        sut = new FileSystemCourseRepository(new CourseValidatorImpl());
    }

    // =========================================================================
    // Helper: build a minimal but valid single-milestone course fixture
    // =========================================================================

    /**
     * Writes a complete, valid course fixture under {@code root}.
     * Structure: Course -> ROOT -> 1 Milestone -> 1 Topic -> 1 Knowledge -> 1 Quiz.
     */
    private void writeMinimalCourse(Path root) throws IOException {
        writeCourseJson(root, COURSE_ID, "test-course",
                List.of(K1_ID), ROOT_ID, List.of(M1_ID));

        Path m1Dir = root.resolve("a1");
        Files.createDirectories(m1Dir);
        writeMilestoneJson(m1Dir, M1_ID, "A1", M1_OLD_ID, ROOT_ID, List.of(T1_ID));

        Path t1Dir = m1Dir.resolve("present-simple");
        Files.createDirectories(t1Dir);
        writeTopicJson(t1Dir, T1_ID, "Present Simple", T1_OLD_ID, M1_ID, List.of(K1_ID));

        Path k1Dir = t1Dir.resolve("affirmative-sentences");
        Files.createDirectories(k1Dir);
        writeKnowledgeJson(k1Dir, K1_ID, "Affirmative Sentences", K1_OLD_ID, T1_ID,
                "Escribe la forma afirmativa.");
        writeQuizzesJson(k1Dir, List.of(quizEntry(Q1_ID, K1_ID, "Affirmative Sentences",
                "Escribe la forma afirmativa.", "She plays", "a1.01.Present_Simple", "Present Simple")));
    }

    /**
     * Writes a two-milestone course:
     *   ROOT -> [M1, M2]
     *   M1 -> [T1] -> [K1] -> [Q1]
     *   M2 -> [T2] -> [K2] -> [Q2]
     */
    private void writeTwoMilestoneCourse(Path root) throws IOException {
        writeCourseJson(root, COURSE_ID, "test-course",
                List.of(K1_ID, K2_ID), ROOT_ID, List.of(M1_ID, M2_ID));

        // Milestone 1
        Path m1Dir = root.resolve("a1");
        Files.createDirectories(m1Dir);
        writeMilestoneJson(m1Dir, M1_ID, "A1", M1_OLD_ID, ROOT_ID, List.of(T1_ID));
        Path t1Dir = m1Dir.resolve("present-simple");
        Files.createDirectories(t1Dir);
        writeTopicJson(t1Dir, T1_ID, "Present Simple", T1_OLD_ID, M1_ID, List.of(K1_ID));
        Path k1Dir = t1Dir.resolve("affirmative-sentences");
        Files.createDirectories(k1Dir);
        writeKnowledgeJson(k1Dir, K1_ID, "Affirmative Sentences", K1_OLD_ID, T1_ID,
                "Escribe la forma afirmativa.");
        writeQuizzesJson(k1Dir, List.of(quizEntry(Q1_ID, K1_ID, "Affirmative Sentences",
                "Escribe la forma afirmativa.", "She plays tennis.", "a1.01.Present_Simple", "Present Simple")));

        // Milestone 2
        Path m2Dir = root.resolve("a2");
        Files.createDirectories(m2Dir);
        writeMilestoneJson(m2Dir, M2_ID, "A2", M2_OLD_ID, ROOT_ID, List.of(T2_ID));
        Path t2Dir = m2Dir.resolve("modal-verbs");
        Files.createDirectories(t2Dir);
        writeTopicJson(t2Dir, T2_ID, "Modal Verbs", T2_OLD_ID, M2_ID, List.of(K2_ID));
        Path k2Dir = t2Dir.resolve("can-and-could");
        Files.createDirectories(k2Dir);
        writeKnowledgeJson(k2Dir, K2_ID, "Can and Could", K2_OLD_ID, T2_ID,
                "Usa can o could.");
        writeQuizzesJson(k2Dir, List.of(quizEntry(Q2_ID, K2_ID, "Can and Could",
                "Usa can o could.", "She can swim.", "a2.01.Modal_Verbs", "Modal Verbs")));
    }

    // -------------------------------------------------------------------------
    // Low-level JSON writers
    // -------------------------------------------------------------------------

    private void writeCourseJson(Path courseDir, String id, String title,
            List<String> knowledgeIds, String rootId, List<String> rootChildren) throws IOException {
        var json = new java.util.LinkedHashMap<String, Object>();
        json.put("_id", oid(id));
        json.put("title", title);
        json.put("knowledgeIds", oidList(knowledgeIds));

        var rootJson = new java.util.LinkedHashMap<String, Object>();
        rootJson.put("_id", oid(rootId));
        rootJson.put("code", "root");
        rootJson.put("kind", "ROOT");
        rootJson.put("label", null);
        rootJson.put("children", oidList(rootChildren));
        json.put("root", rootJson);

        writeJson(courseDir.resolve("_course.json"), json);
    }

    private void writeMilestoneJson(Path dir, String id, String label, String oldId,
            String parentId, List<String> children) throws IOException {
        var json = new java.util.LinkedHashMap<String, Object>();
        json.put("_id", oid(id));
        json.put("children", oidList(children));
        json.put("code", id);
        json.put("kind", "MILESTONE");
        json.put("label", label);
        json.put("oldId", oldId);
        json.put("parentId", oid(parentId));
        writeJson(dir.resolve("_milestone.json"), json);
    }

    private void writeTopicJson(Path dir, String id, String label, String oldId,
            String parentId, List<String> ruleIds) throws IOException {
        var json = new java.util.LinkedHashMap<String, Object>();
        json.put("_id", oid(id));
        json.put("children", null);   // R010: always null, must be preserved
        json.put("code", id);
        json.put("kind", "TOPIC");
        json.put("label", label);
        json.put("oldId", oldId);
        json.put("parentId", oid(parentId));
        json.put("ruleIds", oidList(ruleIds));
        writeJson(dir.resolve("_topic.json"), json);
    }

    private void writeKnowledgeJson(Path dir, String id, String label, String oldId,
            String parentId, String instructions) throws IOException {
        var json = new java.util.LinkedHashMap<String, Object>();
        json.put("_id", oid(id));
        json.put("code", id);
        json.put("isRule", true);
        json.put("kind", "KNOWLEDGE");
        json.put("label", label);
        json.put("oldId", oldId);
        json.put("parentId", oid(parentId));
        json.put("instructions", instructions);
        writeJson(dir.resolve("_knowledge.json"), json);
    }

    private void writeQuizzesJson(Path dir, List<Map<String, Object>> quizzes) throws IOException {
        writeJson(dir.resolve("quizzes.json"), quizzes);
    }

    private Map<String, Object> quizEntry(String id, String knowledgeId, String title,
            String instructions, String translation, String theoryId, String topicName) {
        var q = new java.util.LinkedHashMap<String, Object>();
        q.put("_id", oid(id));
        q.put("id", id);
        q.put("kind", "CLOZE");
        q.put("knowledgeId", oid(knowledgeId));
        q.put("title", title);
        q.put("instructions", instructions);
        q.put("translation", translation);
        q.put("theoryId", theoryId);
        q.put("topicName", topicName);
        q.put("difficulty", numberDouble("0.0"));
        q.put("retries", numberDouble("0.0"));
        q.put("noScoreRetries", numberDouble("0.0"));
        q.put("code", "");
        q.put("audioUrl", "");
        q.put("imageUrl", "");
        q.put("answerAudioUrl", "A1.01.01.01");
        q.put("answerImageUrl", "");
        q.put("miniTheory", "");
        q.put("successMessage", "");
        q.put("form", formEntry());
        return q;
    }

    private Map<String, Object> formEntry() {
        var form = new java.util.LinkedHashMap<String, Object>();
        form.put("kind", "CLOZE");
        form.put("incidence", numberDouble("1.0"));
        form.put("label", "");
        form.put("name", "");
        var part1 = new java.util.LinkedHashMap<String, Object>();
        part1.put("kind", "TEXT");
        part1.put("text", "She ");
        part1.put("options", null);
        var part2 = new java.util.LinkedHashMap<String, Object>();
        part2.put("kind", "CLOZE");
        part2.put("text", "");
        part2.put("options", List.of("plays"));
        form.put("sentenceParts", List.of(part1, part2));
        return form;
    }

    // -------------------------------------------------------------------------
    // MongoDB Extended JSON wrapper helpers
    // -------------------------------------------------------------------------

    private Map<String, String> oid(String id) {
        if (id == null) return null;
        return Map.of("$oid", id);
    }

    private List<Map<String, String>> oidList(List<String> ids) {
        if (ids == null) return null;
        return ids.stream().map(this::oid).toList();
    }

    private Map<String, String> numberDouble(String value) {
        return Map.of("$numberDouble", value);
    }

    private void writeJson(Path file, Object obj) throws IOException {
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(file.toFile(), obj);
    }

    // =========================================================================
    // R001 — Load returns 5-level hierarchy with ROOT node
    // =========================================================================

    @Test
    @DisplayName("R001: Load returns 5-level hierarchy with ROOT node")
    @Tag("F-COURSE")
    void r001_loadReturns5LevelHierarchyWithRootNode(@TempDir Path tempDir) throws IOException {
        writeMinimalCourse(tempDir);

        CourseEntity course = sut.load(tempDir);

        // Level 1: CourseEntity
        assertNotNull(course, "CourseEntity must not be null");
        assertEquals(COURSE_ID, course.getId());

        // Level 2: ROOT node
        assertNotNull(course.getRoot(), "ROOT node must not be null");
        assertEquals(NodeKind.ROOT, course.getRoot().getKind());
        assertEquals(ROOT_ID, course.getRoot().getId());

        // Level 3: Milestone
        List<MilestoneEntity> milestones = course.getRoot().getMilestones();
        assertNotNull(milestones);
        assertEquals(1, milestones.size());
        assertEquals(M1_ID, milestones.get(0).getId());
        assertEquals(NodeKind.MILESTONE, milestones.get(0).getKind());

        // Level 4: Topic
        List<TopicEntity> topics = milestones.get(0).getTopics();
        assertNotNull(topics);
        assertEquals(1, topics.size());
        assertEquals(T1_ID, topics.get(0).getId());
        assertEquals(NodeKind.TOPIC, topics.get(0).getKind());

        // Level 5: Knowledge
        List<KnowledgeEntity> knowledges = topics.get(0).getKnowledges();
        assertNotNull(knowledges);
        assertEquals(1, knowledges.size());
        assertEquals(K1_ID, knowledges.get(0).getId());
        assertEquals(NodeKind.KNOWLEDGE, knowledges.get(0).getKind());

        // Level 6 (quiz templates as leaf): they belong to the knowledge
        assertFalse(knowledges.get(0).getQuizTemplates().isEmpty(),
                "Knowledge must have at least one quiz template");
        assertEquals(Q1_ID, knowledges.get(0).getQuizTemplates().get(0).getId());
    }

    // =========================================================================
    // R002 — Child order matches parent children lists
    // =========================================================================

    @Test
    @DisplayName("R002: Child order matches parent children lists")
    @Tag("F-COURSE")
    void r002_childOrderMatchesParentChildrenLists(@TempDir Path tempDir) throws IOException {
        writeTwoMilestoneCourse(tempDir);

        CourseEntity course = sut.load(tempDir);

        List<MilestoneEntity> milestones = course.getRoot().getMilestones();
        assertEquals(2, milestones.size());

        // ROOT.children is [M1_ID, M2_ID] → milestones must appear in that order
        assertEquals(M1_ID, milestones.get(0).getId(), "First milestone must be M1");
        assertEquals(M2_ID, milestones.get(1).getId(), "Second milestone must be M2");

        // M1.children is [T1_ID] → first topic of M1 must be T1
        assertEquals(T1_ID, milestones.get(0).getTopics().get(0).getId());

        // M2.children is [T2_ID] → first topic of M2 must be T2
        assertEquals(T2_ID, milestones.get(1).getTopics().get(0).getId());

        // T1.ruleIds is [K1_ID] → first knowledge of T1 must be K1
        assertEquals(K1_ID, milestones.get(0).getTopics().get(0).getKnowledges().get(0).getId());
    }

    // =========================================================================
    // R003 — Read-write semantic idempotency (load → save → load → assertEquals)
    // =========================================================================

    @Test
    @DisplayName("R003: Read-write semantic idempotency (load -> save -> load -> assertEquals)")
    @Tag("F-COURSE")
    void r003_readWriteSemanticIdempotency(@TempDir Path tempDir) throws IOException {
        writeMinimalCourse(tempDir);

        // First load
        CourseEntity original = sut.load(tempDir);

        // Save to a new directory
        Path savedDir = tempDir.resolve("saved");
        sut.save(original, savedDir);

        // Second load from the saved output
        CourseEntity reloaded = sut.load(savedDir);

        // The reloaded entity must be semantically equivalent to the original
        assertEquals(original.getId(), reloaded.getId());
        assertEquals(original.getTitle(), reloaded.getTitle());
        assertEquals(original.getKnowledgeIds(), reloaded.getKnowledgeIds());

        // ROOT
        assertEquals(original.getRoot().getId(), reloaded.getRoot().getId());
        assertEquals(original.getRoot().getCode(), reloaded.getRoot().getCode());
        assertEquals(original.getRoot().getChildren(), reloaded.getRoot().getChildren());

        // Milestone
        MilestoneEntity om = original.getRoot().getMilestones().get(0);
        MilestoneEntity rm = reloaded.getRoot().getMilestones().get(0);
        assertEquals(om.getId(), rm.getId());
        assertEquals(om.getLabel(), rm.getLabel());
        assertEquals(om.getOldId(), rm.getOldId());
        assertEquals(om.getParentId(), rm.getParentId());
        assertEquals(om.getChildren(), rm.getChildren());

        // Topic
        TopicEntity ot = om.getTopics().get(0);
        TopicEntity rt = rm.getTopics().get(0);
        assertEquals(ot.getId(), rt.getId());
        assertEquals(ot.getLabel(), rt.getLabel());
        assertNull(rt.getChildren(), "Topic.children must remain null after roundtrip (R010)");
        assertEquals(ot.getRuleIds(), rt.getRuleIds());

        // Knowledge
        KnowledgeEntity ok = ot.getKnowledges().get(0);
        KnowledgeEntity rk = rt.getKnowledges().get(0);
        assertEquals(ok.getId(), rk.getId());
        assertEquals(ok.getLabel(), rk.getLabel());
        assertEquals(ok.getInstructions(), rk.getInstructions());

        // Quiz
        assertEquals(ok.getQuizTemplates().get(0).getId(),
                rk.getQuizTemplates().get(0).getId());
    }

    // =========================================================================
    // R004 — Every knowledge has quiz templates
    // =========================================================================

    @Test
    @DisplayName("R004: Every knowledge has at least one quiz template")
    @Tag("F-COURSE")
    void r004_everyKnowledgeHasQuizTemplates(@TempDir Path tempDir) throws IOException {
        writeMinimalCourse(tempDir);

        CourseEntity course = sut.load(tempDir);

        for (MilestoneEntity m : course.getRoot().getMilestones()) {
            for (TopicEntity t : m.getTopics()) {
                for (KnowledgeEntity k : t.getKnowledges()) {
                    assertNotNull(k.getQuizTemplates(),
                            "quizTemplates must not be null for knowledge " + k.getId());
                    assertFalse(k.getQuizTemplates().isEmpty(),
                            "Knowledge '" + k.getLabel() + "' (" + k.getId() + ") must have at least one quiz template");
                }
            }
        }
    }

    @Test
    @DisplayName("R004: Knowledge with no quizzes.json triggers validation exception")
    @Tag("F-COURSE")
    void r004_knowledgeWithNoQuizzesRejectsCourse(@TempDir Path tempDir) throws IOException {
        writeMinimalCourse(tempDir);
        // Delete the quizzes.json to produce an empty quiz list
        Files.delete(tempDir.resolve("a1/present-simple/affirmative-sentences/quizzes.json"));

        assertThrows(CourseValidationException.class, () -> sut.load(tempDir),
                "Validation must reject a knowledge with no quiz templates");
    }

    // =========================================================================
    // R005 — All child ID references resolve to existing entities
    // =========================================================================

    @Test
    @DisplayName("R005: All child ID references resolve to existing entities")
    @Tag("F-COURSE")
    void r005_allChildIdReferencesResolve(@TempDir Path tempDir) throws IOException {
        writeMinimalCourse(tempDir);
        CourseEntity course = sut.load(tempDir);

        // ROOT.children must match actual loaded milestone IDs
        Set<String> rootChildrenSet = new HashSet<>(course.getRoot().getChildren());
        for (MilestoneEntity m : course.getRoot().getMilestones()) {
            assertTrue(rootChildrenSet.contains(m.getId()),
                    "Milestone ID " + m.getId() + " must be in ROOT.children");
        }

        // Milestone.children must match actual loaded topic IDs
        for (MilestoneEntity m : course.getRoot().getMilestones()) {
            Set<String> mChildrenSet = new HashSet<>(m.getChildren());
            for (TopicEntity t : m.getTopics()) {
                assertTrue(mChildrenSet.contains(t.getId()),
                        "Topic ID " + t.getId() + " must be in Milestone.children");
            }
        }

        // Topic.ruleIds must match actual loaded knowledge IDs
        for (MilestoneEntity m : course.getRoot().getMilestones()) {
            for (TopicEntity t : m.getTopics()) {
                Set<String> ruleIdsSet = new HashSet<>(t.getRuleIds());
                for (KnowledgeEntity k : t.getKnowledges()) {
                    assertTrue(ruleIdsSet.contains(k.getId()),
                            "Knowledge ID " + k.getId() + " must be in Topic.ruleIds");
                }
            }
        }
    }

    @Test
    @DisplayName("R005: Broken child reference in ROOT.children causes exception during load")
    @Tag("F-COURSE")
    void r005_brokenChildReferenceThrowsOnLoad(@TempDir Path tempDir) throws IOException {
        // Write ROOT with a reference to a milestone ID that has no directory
        String fakeMilestoneId = "999999999999999999999999";
        writeCourseJson(tempDir, COURSE_ID, "test-course",
                List.of(), ROOT_ID, List.of(fakeMilestoneId));
        // Don't create any milestone directory

        assertThrows(CourseValidationException.class, () -> sut.load(tempDir),
                "Load must fail when ROOT.children references a non-existent milestone");
    }

    // =========================================================================
    // R006 — No duplicate IDs across hierarchy
    // =========================================================================

    @Test
    @DisplayName("R006: No duplicate IDs exist across any hierarchy level")
    @Tag("F-COURSE")
    void r006_noDuplicateIdsAcrossHierarchy(@TempDir Path tempDir) throws IOException {
        writeTwoMilestoneCourse(tempDir);
        CourseEntity course = sut.load(tempDir);

        Set<String> seenIds = new HashSet<>();
        seenIds.add(course.getId());

        String rootId = course.getRoot().getId();
        assertTrue(seenIds.add(rootId), "ROOT id must be unique: " + rootId);

        for (MilestoneEntity m : course.getRoot().getMilestones()) {
            assertTrue(seenIds.add(m.getId()), "Duplicate milestone ID: " + m.getId());
            for (TopicEntity t : m.getTopics()) {
                assertTrue(seenIds.add(t.getId()), "Duplicate topic ID: " + t.getId());
                for (KnowledgeEntity k : t.getKnowledges()) {
                    assertTrue(seenIds.add(k.getId()), "Duplicate knowledge ID: " + k.getId());
                }
            }
        }
    }

    // =========================================================================
    // R007 — Directory-entity descriptor correspondence (also .DS_Store etc ignored)
    // =========================================================================

    @Test
    @DisplayName("R007: Each directory level contains its expected descriptor file; non-descriptor files ignored")
    @Tag("F-COURSE")
    void r007_directoryDescriptorCorrespondenceAndIgnoresUnknownFiles(@TempDir Path tempDir)
            throws IOException {
        writeMinimalCourse(tempDir);

        // Plant OS-level noise that must be silently ignored
        Files.writeString(tempDir.resolve(".DS_Store"), "noise");
        Files.writeString(tempDir.resolve("a1/.DS_Store"), "noise");
        Files.writeString(tempDir.resolve("a1/present-simple/.DS_Store"), "noise");
        Files.writeString(tempDir.resolve("a1/present-simple/affirmative-sentences/.DS_Store"), "noise");

        // Also plant a directory without any descriptor at the milestone level; must be ignored
        Path strangerDir = tempDir.resolve("a1/orphan-dir");
        Files.createDirectories(strangerDir);
        // No _topic.json inside — should be silently skipped

        // Load must succeed and return a well-formed course
        CourseEntity course = assertDoesNotThrow(() -> sut.load(tempDir));
        assertEquals(1, course.getRoot().getMilestones().size());
        assertEquals(1, course.getRoot().getMilestones().get(0).getTopics().size());

        // Verify that the required descriptor files exist after writing
        Path dest = tempDir.resolve("dest");
        sut.save(course, dest);
        assertTrue(Files.exists(dest.resolve("_course.json")));
        assertTrue(Files.exists(dest.resolve("a1/_milestone.json")));
        assertTrue(Files.exists(dest.resolve("a1/present-simple/_topic.json")));
        assertTrue(Files.exists(dest.resolve("a1/present-simple/affirmative-sentences/_knowledge.json")));
        assertTrue(Files.exists(dest.resolve("a1/present-simple/affirmative-sentences/quizzes.json")));
    }

    // =========================================================================
    // R008 — Parent-child referential integrity (parentId matches actual parent)
    // =========================================================================

    @Test
    @DisplayName("R008: Each child's parentId matches its actual parent id")
    @Tag("F-COURSE")
    void r008_parentChildReferentialIntegrity(@TempDir Path tempDir) throws IOException {
        writeTwoMilestoneCourse(tempDir);
        CourseEntity course = sut.load(tempDir);

        String rootId = course.getRoot().getId();
        for (MilestoneEntity m : course.getRoot().getMilestones()) {
            assertEquals(rootId, m.getParentId(),
                    "Milestone " + m.getId() + " parentId must equal ROOT id");
            for (TopicEntity t : m.getTopics()) {
                assertEquals(m.getId(), t.getParentId(),
                        "Topic " + t.getId() + " parentId must equal Milestone id");
                for (KnowledgeEntity k : t.getKnowledges()) {
                    assertEquals(t.getId(), k.getParentId(),
                            "Knowledge " + k.getId() + " parentId must equal Topic id");
                    for (var quiz : k.getQuizTemplates()) {
                        assertEquals(k.getId(), quiz.getKnowledgeId(),
                                "Quiz " + quiz.getId() + " knowledgeId must equal Knowledge id");
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("R008: Wrong parentId in milestone JSON causes validation exception on load")
    @Tag("F-COURSE")
    void r008_wrongParentIdThrowsOnLoad(@TempDir Path tempDir) throws IOException {
        // Write ROOT pointing to M1_ID, but milestone has parentId pointing to a wrong parent
        writeCourseJson(tempDir, COURSE_ID, "test-course",
                List.of(K1_ID), ROOT_ID, List.of(M1_ID));

        String wrongParentId = "999999999999999999999998";
        Path m1Dir = tempDir.resolve("a1");
        Files.createDirectories(m1Dir);
        // Use a parentId that does NOT match ROOT_ID
        writeMilestoneJson(m1Dir, M1_ID, "A1", M1_OLD_ID, wrongParentId, List.of(T1_ID));
        Path t1Dir = m1Dir.resolve("present-simple");
        Files.createDirectories(t1Dir);
        writeTopicJson(t1Dir, T1_ID, "Present Simple", T1_OLD_ID, M1_ID, List.of(K1_ID));
        Path k1Dir = t1Dir.resolve("affirmative-sentences");
        Files.createDirectories(k1Dir);
        writeKnowledgeJson(k1Dir, K1_ID, "Affirmative Sentences", K1_OLD_ID, T1_ID, "Instrucciones.");
        writeQuizzesJson(k1Dir, List.of(quizEntry(Q1_ID, K1_ID, "Title", "Inst.", "Trans.",
                "a1.01.Foo", "Present Simple")));

        assertThrows(CourseValidationException.class, () -> sut.load(tempDir),
                "Wrong milestone parentId must trigger validation exception");
    }

    // =========================================================================
    // R009 — All mandatory fields non-null after load
    // =========================================================================

    @Test
    @DisplayName("R009: All mandatory fields are non-null after load")
    @Tag("F-COURSE")
    void r009_allMandatoryFieldsNonNullAfterLoad(@TempDir Path tempDir) throws IOException {
        writeTwoMilestoneCourse(tempDir);
        CourseEntity course = sut.load(tempDir);

        // Course
        assertNotNull(course.getId(), "Course.id");
        assertNotNull(course.getTitle(), "Course.title");
        assertNotNull(course.getKnowledgeIds(), "Course.knowledgeIds");

        // ROOT
        assertNotNull(course.getRoot().getId(), "ROOT.id");
        assertNotNull(course.getRoot().getCode(), "ROOT.code");
        assertNotNull(course.getRoot().getKind(), "ROOT.kind");
        assertNotNull(course.getRoot().getChildren(), "ROOT.children");
        assertFalse(course.getRoot().getChildren().isEmpty(), "ROOT.children must be non-empty");

        for (MilestoneEntity m : course.getRoot().getMilestones()) {
            assertNotNull(m.getId(), "Milestone.id");
            assertNotNull(m.getCode(), "Milestone.code");
            assertNotNull(m.getKind(), "Milestone.kind");
            assertNotNull(m.getLabel(), "Milestone.label");
            assertNotNull(m.getOldId(), "Milestone.oldId");
            assertNotNull(m.getParentId(), "Milestone.parentId");
            assertNotNull(m.getChildren(), "Milestone.children");
            assertFalse(m.getChildren().isEmpty(), "Milestone.children must not be empty");

            for (TopicEntity t : m.getTopics()) {
                assertNotNull(t.getId(), "Topic.id");
                assertNotNull(t.getCode(), "Topic.code");
                assertNotNull(t.getKind(), "Topic.kind");
                assertNotNull(t.getLabel(), "Topic.label");
                assertNotNull(t.getOldId(), "Topic.oldId");
                assertNotNull(t.getParentId(), "Topic.parentId");
                assertNotNull(t.getRuleIds(), "Topic.ruleIds");
                assertFalse(t.getRuleIds().isEmpty(), "Topic.ruleIds must not be empty");

                for (KnowledgeEntity k : t.getKnowledges()) {
                    assertNotNull(k.getId(), "Knowledge.id");
                    assertNotNull(k.getCode(), "Knowledge.code");
                    assertNotNull(k.getKind(), "Knowledge.kind");
                    assertNotNull(k.getLabel(), "Knowledge.label");
                    assertNotNull(k.getOldId(), "Knowledge.oldId");
                    assertNotNull(k.getParentId(), "Knowledge.parentId");
                }
            }
        }
    }

    // =========================================================================
    // R010 — Empty/null value preservation through save/load roundtrip
    // =========================================================================

    @Test
    @DisplayName("R010: Empty and null field values are preserved exactly through save/load roundtrip")
    @Tag("F-COURSE")
    void r010_emptyAndNullValuePreservation(@TempDir Path tempDir) throws IOException {
        writeMinimalCourse(tempDir);

        // Verify Topic.children is null before roundtrip
        CourseEntity original = sut.load(tempDir);
        TopicEntity originalTopic = original.getRoot().getMilestones().get(0).getTopics().get(0);
        assertNull(originalTopic.getChildren(), "Topic.children must be null before roundtrip");

        // Save and reload
        Path dest = tempDir.resolve("dest");
        sut.save(original, dest);
        CourseEntity reloaded = sut.load(dest);

        // Topic.children must still be null after roundtrip (R010)
        TopicEntity reloadedTopic = reloaded.getRoot().getMilestones().get(0).getTopics().get(0);
        assertNull(reloadedTopic.getChildren(), "Topic.children must remain null after roundtrip");

        // Quiz empty string fields must be preserved
        var originalQuiz = original.getRoot().getMilestones().get(0).getTopics().get(0)
                .getKnowledges().get(0).getQuizTemplates().get(0);
        var reloadedQuiz = reloaded.getRoot().getMilestones().get(0).getTopics().get(0)
                .getKnowledges().get(0).getQuizTemplates().get(0);
        assertEquals(originalQuiz.getCode(), reloadedQuiz.getCode(),
                "Quiz.code empty string must be preserved");
        assertEquals(originalQuiz.getAudioUrl(), reloadedQuiz.getAudioUrl(),
                "Quiz.audioUrl empty string must be preserved");
        assertEquals(originalQuiz.getMiniTheory(), reloadedQuiz.getMiniTheory(),
                "Quiz.miniTheory empty string must be preserved");
        assertEquals(originalQuiz.getSuccessMessage(), reloadedQuiz.getSuccessMessage(),
                "Quiz.successMessage empty string must be preserved");
    }

    // =========================================================================
    // R011 — Dual id/oidId consistency in QuizTemplate
    // =========================================================================

    @Test
    @DisplayName("R011: QuizTemplate id and oidId both contain the same value")
    @Tag("F-COURSE")
    void r011_dualIdOidIdConsistency(@TempDir Path tempDir) throws IOException {
        writeMinimalCourse(tempDir);
        CourseEntity course = sut.load(tempDir);

        for (MilestoneEntity m : course.getRoot().getMilestones()) {
            for (TopicEntity t : m.getTopics()) {
                for (KnowledgeEntity k : t.getKnowledges()) {
                    for (var quiz : k.getQuizTemplates()) {
                        assertNotNull(quiz.getId(), "Quiz.id must not be null");
                        assertNotNull(quiz.getOidId(), "Quiz.oidId must not be null");
                        assertEquals(quiz.getId(), quiz.getOidId(),
                                "Quiz.id and Quiz.oidId must contain the same value for quiz " + quiz.getId());
                    }
                }
            }
        }
    }

    // =========================================================================
    // R012 — MongoDB $numberDouble format preservation in saved JSON
    // =========================================================================

    @Test
    @DisplayName("R012: Numeric fields in quiz templates use $numberDouble format in saved JSON")
    @Tag("F-COURSE")
    @SuppressWarnings("unchecked")
    void r012_mongoDbNumberDoubleFormatInSavedJson(@TempDir Path tempDir) throws IOException {
        writeMinimalCourse(tempDir);
        CourseEntity course = sut.load(tempDir);

        Path dest = tempDir.resolve("dest");
        sut.save(course, dest);

        // Read the written quizzes.json raw and assert the numeric format
        Path quizzesFile = dest.resolve("a1/present-simple/affirmative-sentences/quizzes.json");
        assertTrue(Files.exists(quizzesFile), "quizzes.json must exist after save");

        List<Map<String, Object>> quizzesJson =
                new ObjectMapper().readValue(quizzesFile.toFile(), List.class);
        assertFalse(quizzesJson.isEmpty());

        Map<String, Object> quiz = quizzesJson.get(0);

        // difficulty, retries, noScoreRetries must be objects with "$numberDouble" key
        assertMongoNumberDouble(quiz.get("difficulty"), "difficulty");
        assertMongoNumberDouble(quiz.get("retries"), "retries");
        assertMongoNumberDouble(quiz.get("noScoreRetries"), "noScoreRetries");

        // form.incidence must also be $numberDouble
        Map<String, Object> form = (Map<String, Object>) quiz.get("form");
        assertNotNull(form);
        assertMongoNumberDouble(form.get("incidence"), "form.incidence");
    }

    @SuppressWarnings("unchecked")
    private void assertMongoNumberDouble(Object value, String fieldName) {
        assertNotNull(value, fieldName + " must not be null");
        assertInstanceOf(Map.class, value, fieldName + " must be a $numberDouble object");
        Map<String, Object> map = (Map<String, Object>) value;
        assertTrue(map.containsKey("$numberDouble"),
                fieldName + " must have a '$numberDouble' key; got: " + map);
    }

    // =========================================================================
    // R013 — Sequential 1-based order fields
    // =========================================================================

    @Test
    @DisplayName("R013: Milestones, topics, and knowledges have sequential 1-based order within their parent")
    @Tag("F-COURSE")
    void r013_sequential1BasedOrderFields(@TempDir Path tempDir) throws IOException {
        writeTwoMilestoneCourse(tempDir);
        CourseEntity course = sut.load(tempDir);

        List<MilestoneEntity> milestones = course.getRoot().getMilestones();

        // Milestones are ordered 1, 2, ...
        for (int i = 0; i < milestones.size(); i++) {
            assertEquals(i + 1, milestones.get(i).getOrder(),
                    "Milestone at index " + i + " must have order " + (i + 1));
        }

        // Topics within each milestone
        for (MilestoneEntity m : milestones) {
            List<TopicEntity> topics = m.getTopics();
            for (int i = 0; i < topics.size(); i++) {
                assertEquals(i + 1, topics.get(i).getOrder(),
                        "Topic at index " + i + " in milestone " + m.getId()
                                + " must have order " + (i + 1));
            }

            // Knowledges within each topic
            for (TopicEntity t : topics) {
                List<KnowledgeEntity> knowledges = t.getKnowledges();
                for (int i = 0; i < knowledges.size(); i++) {
                    assertEquals(i + 1, knowledges.get(i).getOrder(),
                            "Knowledge at index " + i + " in topic " + t.getId()
                                    + " must have order " + (i + 1));
                }
            }
        }
    }

    // =========================================================================
    // R015 — Empty branches rejected (milestone with no topic dirs)
    // =========================================================================

    @Test
    @DisplayName("R015: Milestone with no topic directories is rejected by validation")
    @Tag("F-COURSE")
    void r015_emptyMilestoneRejected(@TempDir Path tempDir) throws IOException {
        // Write a milestone that lists T1_ID in children, but no topic directory exists
        writeCourseJson(tempDir, COURSE_ID, "test-course", List.of(), ROOT_ID, List.of(M1_ID));
        Path m1Dir = tempDir.resolve("a1");
        Files.createDirectories(m1Dir);
        // Milestone declares T1_ID as child but there is no topic directory at all
        writeMilestoneJson(m1Dir, M1_ID, "A1", M1_OLD_ID, ROOT_ID, List.of(T1_ID));
        // Intentionally do NOT create any topic subdirectory

        assertThrows(CourseValidationException.class, () -> sut.load(tempDir),
                "Milestone with missing topic directory must throw CourseValidationException");
    }

    // =========================================================================
    // R016 — Deterministic slug generation from label on save
    // =========================================================================

    @Test
    @DisplayName("R016: Deterministic slug generation from label — entities saved to correct directory names")
    @Tag("F-COURSE")
    void r016_deterministicSlugGenerationFromLabel(@TempDir Path tempDir) throws IOException {
        // Build a course in memory with no slug set (slug is null; save must generate from label)
        var quiz = new com.learney.contentaudit.coursedomain.QuizTemplateEntity(
                Q1_ID, Q1_ID, "CLOZE", K1_ID, "Present Simple",
                "Escribe la forma afirmativa.", "Ella juega",
                "a1.01.Present_Simple", "Present Simple",
                new com.learney.contentaudit.coursedomain.FormEntity(
                        "CLOZE", 1.0, "", "",
                        List.of(
                                new com.learney.contentaudit.coursedomain.SentencePartEntity(
                                        com.learney.contentaudit.coursedomain.SentencePartKind.TEXT, "She ", null),
                                new com.learney.contentaudit.coursedomain.SentencePartEntity(
                                        com.learney.contentaudit.coursedomain.SentencePartKind.CLOZE, "", List.of("plays"))
                        )
                ),
                0.0, 0.0, 0.0, "", "", "", "A1.01.01.01", "", "", ""
        );

        var knowledge = new KnowledgeEntity(K1_ID, K1_ID, NodeKind.KNOWLEDGE,
                "Affirmative Sentences", K1_OLD_ID, T1_ID, true,
                "Escribe la forma afirmativa.", 1, null, List.of(quiz));

        var topic = new TopicEntity(T1_ID, T1_ID, NodeKind.TOPIC,
                "Present Simple", T1_OLD_ID, M1_ID,
                null, List.of(K1_ID), 1, null, List.of(knowledge));

        var milestone = new MilestoneEntity(M1_ID, M1_ID, NodeKind.MILESTONE,
                "A1", M1_OLD_ID, ROOT_ID, List.of(T1_ID), 1, null, List.of(topic));

        var root = new com.learney.contentaudit.coursedomain.RootNodeEntity(
                ROOT_ID, "root", NodeKind.ROOT, null,
                List.of(M1_ID), List.of(milestone));

        var course = new CourseEntity(COURSE_ID, "test-course", List.of(K1_ID), root, "test-course");

        sut.save(course, tempDir);

        // Slugs must be derived from labels: "A1" -> "a1", "Present Simple" -> "present-simple",
        // "Affirmative Sentences" -> "affirmative-sentences"
        assertTrue(Files.isDirectory(tempDir.resolve("a1")),
                "Milestone 'A1' must produce directory 'a1'");
        assertTrue(Files.isDirectory(tempDir.resolve("a1/present-simple")),
                "Topic 'Present Simple' must produce directory 'present-simple'");
        assertTrue(Files.isDirectory(tempDir.resolve("a1/present-simple/affirmative-sentences")),
                "Knowledge 'Affirmative Sentences' must produce directory 'affirmative-sentences'");

        // Verify slug algorithm: generateSlug is package-private and testable
        assertEquals("a1", FileSystemCourseRepository.generateSlug("A1"));
        assertEquals("present-simple", FileSystemCourseRepository.generateSlug("Present Simple"));
        assertEquals("affirmative-sentences",
                FileSystemCourseRepository.generateSlug("Affirmative Sentences"));
        assertEquals("good-or-well", FileSystemCourseRepository.generateSlug("Good or Well?"));
        assertEquals("adjectives-and-adverbs",
                FileSystemCourseRepository.generateSlug("Adjectives and Adverbs"));
    }

    // =========================================================================
    // J001 — Load full course from files with hierarchy and order verification
    // =========================================================================

    @Test
    @DisplayName("J001: Load full course from files — hierarchy and order verified at every level")
    @Tag("F-COURSE")
    void j001_loadFullCourseFromFilesWithHierarchyAndOrderVerification(@TempDir Path tempDir)
            throws IOException {
        writeTwoMilestoneCourse(tempDir);

        CourseEntity course = sut.load(tempDir);

        // Verify the root and title
        assertEquals(COURSE_ID, course.getId());
        assertEquals("test-course", course.getTitle());
        assertEquals(NodeKind.ROOT, course.getRoot().getKind());
        assertEquals("root", course.getRoot().getCode());

        // Verify milestone count and order
        List<MilestoneEntity> milestones = course.getRoot().getMilestones();
        assertEquals(2, milestones.size());
        assertEquals("A1", milestones.get(0).getLabel());
        assertEquals(1, milestones.get(0).getOrder());
        assertEquals("A2", milestones.get(1).getLabel());
        assertEquals(2, milestones.get(1).getOrder());

        // M1 → T1 → K1 → Q1
        TopicEntity t1 = milestones.get(0).getTopics().get(0);
        assertEquals("Present Simple", t1.getLabel());
        assertEquals(1, t1.getOrder());
        KnowledgeEntity k1 = t1.getKnowledges().get(0);
        assertEquals("Affirmative Sentences", k1.getLabel());
        assertEquals(1, k1.getOrder());
        assertEquals(Q1_ID, k1.getQuizTemplates().get(0).getId());

        // M2 → T2 → K2 → Q2
        TopicEntity t2 = milestones.get(1).getTopics().get(0);
        assertEquals("Modal Verbs", t2.getLabel());
        assertEquals(1, t2.getOrder());
        KnowledgeEntity k2 = t2.getKnowledges().get(0);
        assertEquals("Can and Could", k2.getLabel());
        assertEquals(1, k2.getOrder());
        assertEquals(Q2_ID, k2.getQuizTemplates().get(0).getId());
    }

    // =========================================================================
    // J002 — Save course to files, verify directory structure and JSON files exist
    // =========================================================================

    @Test
    @DisplayName("J002: Save course to files — directory structure and JSON files are written correctly")
    @Tag("F-COURSE")
    void j002_saveCourseToFilesVerifyDirectoryStructureAndJsonFilesExist(@TempDir Path tempDir)
            throws IOException {
        writeMinimalCourse(tempDir);
        CourseEntity course = sut.load(tempDir);

        Path dest = tempDir.resolve("output");
        sut.save(course, dest);

        // Required files must exist at each level
        assertTrue(Files.exists(dest.resolve("_course.json")),
                "_course.json must be written at root");
        assertTrue(Files.isDirectory(dest.resolve("a1")),
                "Milestone directory 'a1' must exist");
        assertTrue(Files.exists(dest.resolve("a1/_milestone.json")),
                "_milestone.json must exist in milestone directory");
        assertTrue(Files.isDirectory(dest.resolve("a1/present-simple")),
                "Topic directory 'present-simple' must exist");
        assertTrue(Files.exists(dest.resolve("a1/present-simple/_topic.json")),
                "_topic.json must exist in topic directory");
        assertTrue(Files.isDirectory(dest.resolve("a1/present-simple/affirmative-sentences")),
                "Knowledge directory must exist");
        assertTrue(Files.exists(dest.resolve("a1/present-simple/affirmative-sentences/_knowledge.json")),
                "_knowledge.json must exist");
        assertTrue(Files.exists(dest.resolve("a1/present-simple/affirmative-sentences/quizzes.json")),
                "quizzes.json must exist");

        // The _course.json must contain the root node embedded
        @SuppressWarnings("unchecked")
        Map<String, Object> courseJson = new ObjectMapper()
                .readValue(dest.resolve("_course.json").toFile(), Map.class);
        assertTrue(courseJson.containsKey("root"), "_course.json must contain embedded ROOT node");
        assertTrue(courseJson.containsKey("knowledgeIds"), "_course.json must contain knowledgeIds");
    }

    // =========================================================================
    // J003 — Roundtrip idempotency (load → save → reload → assert equal)
    // =========================================================================

    @Test
    @DisplayName("J003: Roundtrip idempotency — load -> save -> reload -> assert equal")
    @Tag("F-COURSE")
    void j003_roundtripIdempotency(@TempDir Path tempDir) throws IOException {
        writeTwoMilestoneCourse(tempDir);

        CourseEntity first = sut.load(tempDir);

        Path dest = tempDir.resolve("copy");
        sut.save(first, dest);
        CourseEntity second = sut.load(dest);

        // Structural equality
        assertEquals(first.getId(), second.getId());
        assertEquals(first.getTitle(), second.getTitle());
        assertEquals(first.getKnowledgeIds().size(), second.getKnowledgeIds().size());
        assertEquals(first.getRoot().getId(), second.getRoot().getId());
        assertEquals(first.getRoot().getChildren(), second.getRoot().getChildren());

        int mCount = first.getRoot().getMilestones().size();
        assertEquals(mCount, second.getRoot().getMilestones().size());

        for (int i = 0; i < mCount; i++) {
            MilestoneEntity m1 = first.getRoot().getMilestones().get(i);
            MilestoneEntity m2 = second.getRoot().getMilestones().get(i);
            assertEquals(m1.getId(), m2.getId());
            assertEquals(m1.getLabel(), m2.getLabel());
            assertEquals(m1.getChildren(), m2.getChildren());
            assertEquals(m1.getOrder(), m2.getOrder());

            int tCount = m1.getTopics().size();
            for (int j = 0; j < tCount; j++) {
                TopicEntity t1 = m1.getTopics().get(j);
                TopicEntity t2 = m2.getTopics().get(j);
                assertEquals(t1.getId(), t2.getId());
                assertNull(t2.getChildren(), "Topic.children must stay null after roundtrip");
                assertEquals(t1.getRuleIds(), t2.getRuleIds());
                assertEquals(t1.getOrder(), t2.getOrder());

                int kCount = t1.getKnowledges().size();
                for (int k = 0; k < kCount; k++) {
                    KnowledgeEntity ke1 = t1.getKnowledges().get(k);
                    KnowledgeEntity ke2 = t2.getKnowledges().get(k);
                    assertEquals(ke1.getId(), ke2.getId());
                    assertEquals(ke1.getLabel(), ke2.getLabel());
                    assertEquals(ke1.getInstructions(), ke2.getInstructions());
                    assertEquals(ke1.getOrder(), ke2.getOrder());
                    assertEquals(ke1.getQuizTemplates().size(), ke2.getQuizTemplates().size());
                }
            }
        }
    }

    // =========================================================================
    // J004 — Navigate hierarchy in memory
    // =========================================================================

    @Test
    @DisplayName("J004: Navigate hierarchy in memory — ROOT to milestones to topics to knowledges to quizzes")
    @Tag("F-COURSE")
    void j004_navigateHierarchyInMemory(@TempDir Path tempDir) throws IOException {
        writeTwoMilestoneCourse(tempDir);
        CourseEntity course = sut.load(tempDir);

        // Navigate down: Course → ROOT
        assertNotNull(course.getRoot());
        List<MilestoneEntity> milestones = course.getRoot().getMilestones();
        assertFalse(milestones.isEmpty(), "ROOT must have milestones");

        // ROOT → Milestones (ordered)
        assertEquals(2, milestones.size());
        assertEquals(1, milestones.get(0).getOrder());
        assertEquals(2, milestones.get(1).getOrder());

        // Milestone → Topics (ordered)
        for (MilestoneEntity m : milestones) {
            assertFalse(m.getTopics().isEmpty(), "Milestone must have topics");
            for (TopicEntity t : m.getTopics()) {
                assertTrue(t.getOrder() >= 1, "Topic order must be >= 1");

                // Topic → Knowledges (ordered)
                assertFalse(t.getKnowledges().isEmpty(), "Topic must have knowledges");
                for (KnowledgeEntity k : t.getKnowledges()) {
                    assertTrue(k.getOrder() >= 1, "Knowledge order must be >= 1");

                    // Knowledge → Quiz templates
                    assertFalse(k.getQuizTemplates().isEmpty(), "Knowledge must have quiz templates");

                    // Navigate back via parentId
                    assertEquals(t.getId(), k.getParentId(), "k.parentId must match t.id");
                }
                assertEquals(m.getId(), t.getParentId(), "t.parentId must match m.id");
            }
            assertEquals(course.getRoot().getId(), m.getParentId(), "m.parentId must match ROOT.id");
        }
    }

    // =========================================================================
    // J005 — Modify knowledge label, save, reload, verify change without side effects
    // =========================================================================

    @Test
    @DisplayName("J005: Modify knowledge label, save, reload — change is reflected, other data intact")
    @Tag("F-COURSE")
    void j005_modifyKnowledgeLabelSaveReloadVerifyChange(@TempDir Path tempDir) throws IOException {
        writeTwoMilestoneCourse(tempDir);
        CourseEntity course = sut.load(tempDir);

        // Capture unmodified second milestone data for side-effect check
        MilestoneEntity m2Before = course.getRoot().getMilestones().get(1);
        String m2LabelBefore = m2Before.getLabel();
        String k2LabelBefore = m2Before.getTopics().get(0).getKnowledges().get(0).getLabel();

        // Modify the label of the first knowledge in the first milestone
        KnowledgeEntity k1 = course.getRoot().getMilestones().get(0)
                .getTopics().get(0).getKnowledges().get(0);
        String originalLabel = k1.getLabel();
        String modifiedLabel = "MODIFIED: " + originalLabel;
        k1.setLabel(modifiedLabel);

        // Save and reload
        Path dest = tempDir.resolve("modified");
        sut.save(course, dest);
        CourseEntity reloaded = sut.load(dest);

        // The modified knowledge must reflect the new label
        KnowledgeEntity reloadedK1 = reloaded.getRoot().getMilestones().get(0)
                .getTopics().get(0).getKnowledges().get(0);
        assertEquals(modifiedLabel, reloadedK1.getLabel(),
                "Modified knowledge label must persist after save/reload");

        // Unmodified entities in M2 must remain intact (no side effects)
        MilestoneEntity m2After = reloaded.getRoot().getMilestones().get(1);
        assertEquals(m2LabelBefore, m2After.getLabel(),
                "M2 label must not be affected by modification to M1");
        assertEquals(k2LabelBefore, m2After.getTopics().get(0).getKnowledges().get(0).getLabel(),
                "K2 label must not be affected by modification to K1");
    }

    // =========================================================================
    // J006 — Error handling (nonexistent path, missing descriptor, malformed JSON)
    // =========================================================================

    @Test
    @DisplayName("J006: Nonexistent path, missing descriptor, or malformed JSON throw descriptive exception")
    @Tag("F-COURSE")
    void j006_errorHandlingNonexistentPathMissingDescriptorMalformedJson(@TempDir Path tempDir)
            throws IOException {

        // 1. Nonexistent path
        Path nonexistent = tempDir.resolve("does-not-exist");
        CourseValidationException ex1 = assertThrows(CourseValidationException.class,
                () -> sut.load(nonexistent),
                "Nonexistent path must throw CourseValidationException");
        assertNotNull(ex1.getDetail(), "Exception detail must be set for nonexistent path");

        // 2. Directory exists but has no _course.json
        Path noCourseJson = tempDir.resolve("no-descriptor");
        Files.createDirectories(noCourseJson);
        CourseValidationException ex2 = assertThrows(CourseValidationException.class,
                () -> sut.load(noCourseJson),
                "Missing _course.json must throw CourseValidationException");
        assertTrue(ex2.getDetail().contains("_course.json"),
                "Error detail must mention missing file; got: " + ex2.getDetail());

        // 3. Malformed JSON in _course.json
        Path malformedDir = tempDir.resolve("malformed");
        Files.createDirectories(malformedDir);
        Files.writeString(malformedDir.resolve("_course.json"), "{ this is not valid json !!!}");
        CourseValidationException ex3 = assertThrows(CourseValidationException.class,
                () -> sut.load(malformedDir),
                "Malformed JSON must throw CourseValidationException");
        assertNotNull(ex3.getMessage(), "Exception message must not be null for malformed JSON");

        // In all three cases, no partial course entity must be returned (all throw)
        // (already guaranteed by the assertThrows calls above)
    }
}
