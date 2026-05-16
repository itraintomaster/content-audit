package com.learney.contentaudit.auditapplication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.lemmacount.LemmaCountCourseDiagnosis;
import com.learney.contentaudit.auditdomain.lemmacount.LemmaCountLevelDiagnosis;
import com.learney.contentaudit.auditdomain.lemmacount.LemmaCountResult;
import com.learney.contentaudit.auditdomain.lemmacount.LevelLemmaCountResult;
import static org.mockito.Mockito.lenient;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.CourseRepository;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefaultAuditRunnerTest {

    @Mock private CourseRepository courseRepository;
    @Mock private CourseToAuditableMapper courseToAuditableMapper;
    @Mock private AuditEngine auditEngine;
    @Mock private ScoreAggregator scoreAggregator;

    private DefaultAuditRunner sut;
    private final Path coursePath = Path.of("/test/course.json");
    private final CourseEntity courseEntity = new CourseEntity();
    private final AuditableCourse auditableCourse = new AuditableCourse(List.of());
    private final AuditReport auditReport = new AuditReport(new AuditNode());

    @BeforeEach
    void setUp() {
        sut = new DefaultAuditRunner(courseRepository, courseToAuditableMapper, auditEngine,
                List.of(), scoreAggregator);
    }

    @Test
    @DisplayName("Given a valid course path, when runAudit is called, then returns the audit report from the full chain")
    @Tag("F-CLI")
    @Tag("F-CLI-R001")
    @Tag("F-CLI-J001")
    public void givenAValidCoursePathWhenRunAuditIsCalledThenReturnsTheAuditReportFromTheFullChain() {
        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(auditableCourse);
        when(auditEngine.runAudit(auditableCourse)).thenReturn(auditReport);

        AuditReport result = sut.runAudit(coursePath, null);

        assertSame(auditReport, result);
    }

    @Test
    @DisplayName("Given a valid course path, when runAudit is called, then courseRepository load is invoked with the path")
    @Tag("F-CLI")
    @Tag("F-CLI-R001")
    public void givenAValidCoursePathWhenRunAuditIsCalledThenCourseRepositoryLoadIsInvokedWithThePath() {
        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(auditableCourse);
        when(auditEngine.runAudit(auditableCourse)).thenReturn(auditReport);

        sut.runAudit(coursePath, null);

        verify(courseRepository).load(coursePath);
    }

    @Test
    @DisplayName("Given a valid course path, when runAudit is called, then courseToAuditableMapper map is invoked with the loaded entity")
    @Tag("F-CLI")
    @Tag("F-CLI-R001")
    public void givenAValidCoursePathWhenRunAuditIsCalledThenCourseToAuditableMapperMapIsInvokedWithTheLoadedEntity() {
        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(auditableCourse);
        when(auditEngine.runAudit(auditableCourse)).thenReturn(auditReport);

        sut.runAudit(coursePath, null);

        verify(courseToAuditableMapper).map(courseEntity);
    }

    @Test
    @DisplayName("Given a valid course path, when runAudit is called, then contentAudit audit is invoked with the mapped auditable course")
    @Tag("F-CLI")
    @Tag("F-CLI-R001")
    public void givenAValidCoursePathWhenRunAuditIsCalledThenContentAuditAuditIsInvokedWithTheMappedAuditableCourse() {
        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(auditableCourse);
        when(auditEngine.runAudit(auditableCourse)).thenReturn(auditReport);

        sut.runAudit(coursePath, null);

        verify(auditEngine).runAudit(auditableCourse);
    }

    @Test
    @DisplayName("Given courseRepository throws an exception, when runAudit is called, then the exception propagates")
    @Tag("F-CLI")
    @Tag("F-CLI-R001")
    public void givenCourseRepositoryThrowsAnExceptionWhenRunAuditIsCalledThenTheExceptionPropagates() {
        when(courseRepository.load(coursePath)).thenThrow(new RuntimeException("load failed"));

        assertThrows(RuntimeException.class, () -> sut.runAudit(coursePath, null));
    }

    @Test
    @DisplayName("Given courseToAuditableMapper throws an exception, when runAudit is called, then the exception propagates")
    @Tag("F-CLI")
    @Tag("F-CLI-R001")
    public void givenCourseToAuditableMapperThrowsAnExceptionWhenRunAuditIsCalledThenTheExceptionPropagates() {
        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenThrow(new RuntimeException("map failed"));

        assertThrows(RuntimeException.class, () -> sut.runAudit(coursePath, null));
    }

    @Test
    @DisplayName("Given contentAudit throws an exception, when runAudit is called, then the exception propagates")
    @Tag("F-CLI")
    @Tag("F-CLI-R001")
    public void givenContentAuditThrowsAnExceptionWhenRunAuditIsCalledThenTheExceptionPropagates() {
        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(auditableCourse);
        when(auditEngine.runAudit(auditableCourse)).thenThrow(new RuntimeException("audit failed"));

        assertThrows(RuntimeException.class, () -> sut.runAudit(coursePath, null));
    }

    @Test
    @DisplayName("Given a course with no milestones, when runAudit is called, then returns the report from contentAudit")
    @Tag("F-CLI")
    @Tag("F-CLI-R001")
    public void givenACourseWithNoMilestonesWhenRunAuditIsCalledThenReturnsTheReportFromContentAudit() {
        AuditableCourse emptyCourse = new AuditableCourse(List.of());
        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(emptyCourse);
        when(auditEngine.runAudit(emptyCourse)).thenReturn(auditReport);

        AuditReport result = sut.runAudit(coursePath, null);

        assertSame(auditReport, result);
    }

    @Test
    @DisplayName("should expose a public runAudit(Path coursePath) method on the AuditRunner contract that returns the AuditReport produced after loading the course mapping it to AuditableCourse and running the audit engine")
    @Tag("FEAT-CLI")
    @Tag("F-CLI-R005")
    public void shouldExposeAPublicRunAuditPathCoursePathMethodOnTheAuditRunnerContractThatReturnsTheAuditReportProducedAfterLoadingTheCourseMappingItToAuditableCourseAndRunningTheAuditEngine() {
        // R005: AuditRunner.runAudit(Path) is the single public entry point for the full pipeline.
        // Verify: load course → map to AuditableCourse → run audit engine → return report.
        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(auditableCourse);
        when(auditEngine.runAudit(auditableCourse)).thenReturn(auditReport);

        AuditReport result = sut.runAudit(coursePath, null);

        assertSame(auditReport, result, "R005: runAudit must return the AuditReport produced by the engine");
        verify(courseRepository).load(coursePath);
        verify(courseToAuditableMapper).map(courseEntity);
        verify(auditEngine).runAudit(auditableCourse);
    }

    @Test
    @DisplayName("should expose lemma-count diagnoses in the AuditReport when runAudit is invoked with both lemma-count and lemma-absence analyzers so downstream consumers can read the underexposure signal per level")
    @Tag("FEAT-LEMMA-SUGGESTIONS")
    @Tag("F-SLEM-R001")
    public void shouldExposeLemmacountDiagnosesInTheAuditReportWhenRunAuditIsInvokedWithBothLemmacountAndLemmaabsenceAnalyzersSoDownstreamConsumersCanReadTheUnderexposureSignalPerLevel() {
        // R001: when runAudit is executed including "lemma-count" (with or without "lemma-absence"),
        // the resulting AuditReport must carry the lemma-count signal so downstream consumers
        // (e.g., LemmaAbsenceContextResolver) can read underexposure per level.
        //
        // Observable behavior: the AuditReport returned by DefaultAuditRunner preserves the
        // lemma-count signal produced by the engine. The test verifies that DefaultAuditRunner
        // passes through the engine result without stripping the LemmaCountCourseDiagnosis.
        //
        // Setup: use analyzerNames=null (fallback to injected auditEngine) so that the mocked
        // auditEngine is called. The mocked engine returns a pre-built report with lemma-count
        // signal, and we assert the signal is preserved in the returned AuditReport.

        // Build an AuditReport with lemma-count signal on the root node
        AuditNode rootNode = new AuditNode();
        rootNode.setTarget(AuditTarget.COURSE);
        rootNode.setChildren(new ArrayList<>());
        rootNode.setScores(new LinkedHashMap<>());
        rootNode.setMetadata(new LinkedHashMap<>());

        LevelLemmaCountResult levelResult = new LevelLemmaCountResult(
                CefrLevel.A1, 0.5, 5, List.of());
        LemmaCountResult lemmaCountResult = new LemmaCountResult(
                3, Optional.of(0.5), List.of(levelResult), List.of());
        LemmaCountCourseDiagnosis lemmaCountCourseDiagnosis = new LemmaCountCourseDiagnosis(lemmaCountResult);

        DefaultCourseDiagnoses courseDiagnoses = new DefaultCourseDiagnoses();
        courseDiagnoses.setLemmaCountDiagnosis(lemmaCountCourseDiagnosis);
        rootNode.setDiagnoses(courseDiagnoses);

        AuditReport reportWithLemmaCount = new AuditReport(rootNode);

        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(auditableCourse);
        // Engine (injected mock) returns the pre-built report with lemma-count signal
        when(auditEngine.runAudit(auditableCourse)).thenReturn(reportWithLemmaCount);

        // Act: null analyzerNames → DefaultAuditRunner uses the injected auditEngine directly
        // (represents the case where all registered analyzers, including lemma-count, are run)
        AuditReport result = sut.runAudit(coursePath, null);

        // Assert: R001 — the returned report exposes the lemma-count diagnosis
        assertNotNull(result, "runAudit must return a non-null AuditReport");
        assertNotNull(result.getRoot(), "AuditReport root must not be null");
        assertNotNull(result.getRoot().getDiagnoses(),
                "Root node diagnoses must not be null when lemma-count was included");
        assertTrue(result.getRoot().getDiagnoses() instanceof CourseDiagnoses,
                "Root node diagnoses must implement CourseDiagnoses for a course-level node");

        CourseDiagnoses rootDiagnoses = (CourseDiagnoses) result.getRoot().getDiagnoses();
        assertTrue(rootDiagnoses.getLemmaCountDiagnosis().isPresent(),
                "R001: AuditReport must carry lemma-count diagnosis when lemma-count analyzer was included");
        assertEquals(3, rootDiagnoses.getLemmaCountDiagnosis().get().getResult().getThresholdN(),
                "R001: lemma-count threshold must be accessible from the AuditReport root diagnoses");
    }

    @Test
    @DisplayName("should expose lemma-count diagnoses in the AuditReport when runDetailedAudit is invoked with both lemma-count and lemma-absence analyzers so downstream consumers can read the underexposure signal per level")
    @Tag("FEAT-LEMMA-SUGGESTIONS")
    @Tag("F-SLEM-R001")
    public void shouldExposeLemmacountDiagnosesInTheAuditReportWhenRunDetailedAuditIsInvokedWithBothLemmacountAndLemmaabsenceAnalyzersSoDownstreamConsumersCanReadTheUnderexposureSignalPerLevel() {
        // R001: when runDetailedAudit is executed including "lemma-count", the resulting AuditNode
        // must carry the lemma-count signal so downstream consumers can read underexposure per level.
        //
        // Observable behavior: DefaultAuditRunner.runDetailedAudit returns an AuditNode. The test
        // verifies that the returned node (or its subtree) carries the lemma-count signal produced
        // by the engine.
        //
        // Setup: the injected auditEngine returns a pre-built AuditReport. DefaultAuditRunner's
        // runDetailedAudit calls the engine and returns a node from the result. We verify that
        // the signal is accessible in the returned node structure.

        // Build an AuditReport where the root carries lemma-count at course level
        // and a milestone node carries lemma-count at level diagnosis
        AuditNode milestoneNode = new AuditNode();
        milestoneNode.setTarget(AuditTarget.MILESTONE);
        milestoneNode.setChildren(new ArrayList<>());
        milestoneNode.setScores(new LinkedHashMap<>());
        milestoneNode.setMetadata(new LinkedHashMap<>());

        LevelLemmaCountResult levelResult = new LevelLemmaCountResult(
                CefrLevel.B1, 0.4, 8, List.of());
        LemmaCountLevelDiagnosis levelDiagnosis = new LemmaCountLevelDiagnosis(levelResult);
        DefaultLevelDiagnoses levelDiagnoses = new DefaultLevelDiagnoses();
        levelDiagnoses.setLemmaCountDiagnosis(levelDiagnosis);
        milestoneNode.setDiagnoses(levelDiagnoses);

        AuditNode rootNode = new AuditNode();
        rootNode.setTarget(AuditTarget.COURSE);
        List<AuditNode> children = new ArrayList<>();
        children.add(milestoneNode);
        milestoneNode.setParent(rootNode);
        rootNode.setChildren(children);
        rootNode.setScores(new LinkedHashMap<>());
        rootNode.setMetadata(new LinkedHashMap<>());

        // Also set course-level lemma-count on root
        LemmaCountCourseDiagnosis courseCountDiag = new LemmaCountCourseDiagnosis(
                new LemmaCountResult(3, Optional.of(0.4), List.of(levelResult), List.of()));
        DefaultCourseDiagnoses courseDiag = new DefaultCourseDiagnoses();
        courseDiag.setLemmaCountDiagnosis(courseCountDiag);
        rootNode.setDiagnoses(courseDiag);

        AuditReport reportWithLemmaCount = new AuditReport(rootNode);

        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(auditableCourse);
        // Use lenient to allow either engine call pattern
        lenient().when(auditEngine.runAudit(any(AuditableCourse.class))).thenReturn(reportWithLemmaCount);
        lenient().when(auditEngine.runAudit(auditableCourse)).thenReturn(reportWithLemmaCount);

        // Act: invoke runDetailedAudit with "lemma-count" as the analyzer name
        AuditNode result = sut.runDetailedAudit(coursePath, "lemma-count");

        // Assert: R001 — the returned AuditNode (or its subtree) carries the lemma-count signal
        assertNotNull(result,
                "R001: runDetailedAudit must return a non-null AuditNode when lemma-count analyzer is included");
        // Check that lemma-count diagnoses are accessible in the returned subtree
        assertTrue(hasLemmaCountDiagnosis(result),
                "R001: runDetailedAudit must return an AuditNode whose subtree carries the lemma-count diagnosis "
                + "signal — downstream consumers need this signal to identify underexposed lemmas per level");
    }

    /**
     * Recursively checks if any AuditNode in the tree has a LemmaCountLevelDiagnosis or
     * LemmaCountCourseDiagnosis set.
     */
    private boolean hasLemmaCountDiagnosis(AuditNode node) {
        if (node == null) return false;
        NodeDiagnoses diag = node.getDiagnoses();
        if (diag instanceof LevelDiagnoses ld && ld.getLemmaCountDiagnosis().isPresent()) return true;
        if (diag instanceof CourseDiagnoses cd && cd.getLemmaCountDiagnosis().isPresent()) return true;
        if (node.getChildren() != null) {
            for (AuditNode child : node.getChildren()) {
                if (hasLemmaCountDiagnosis(child)) return true;
            }
        }
        return false;
    }
}
