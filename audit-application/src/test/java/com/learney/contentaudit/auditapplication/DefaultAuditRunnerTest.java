package com.learney.contentaudit.auditapplication;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.learney.contentaudit.auditdomain.catalog.UnknownAnalyzerException;
import com.learney.contentaudit.auditdomain.contextnumbers.AnalyzerScore;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.findingengine.DefaultFindingCollector;
import com.learney.contentaudit.auditdomain.labs.DefaultSentenceLexicalScorer;
import com.learney.contentaudit.auditdomain.lrec.DefaultContentWordFilter;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.courseinfrastructure.CourseValidatorImpl;
import com.learney.contentaudit.courseinfrastructure.FileSystemCourseRepository;
import com.learney.contentaudit.vocabularyinfrastructure.evp.FileSystemEvpCatalog;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.coca.CocaBucketsAnalyzerProvider;
import com.learney.contentaudit.auditdomain.finding.FindingDraft;
import com.learney.contentaudit.auditdomain.findingengine.DefaultAnalyzerCatalog;
import com.learney.contentaudit.auditdomain.findingengine.DefaultContextNumbersCalculator;
import com.learney.contentaudit.auditdomain.labs.LemmaAbsenceAnalyzerProvider;
import com.learney.contentaudit.auditdomain.lemmacount.LemmaCountAnalyzerProvider;
import com.learney.contentaudit.auditdomain.lrec.LemmaRecurrenceAnalyzerProvider;
import com.learney.contentaudit.auditdomain.quizinstructionengine.DefaultQuizInstructionAnalyzerFactory;
import com.learney.contentaudit.auditdomain.lemmacount.LemmaCountCourseDiagnosis;
import com.learney.contentaudit.auditdomain.lemmacount.LemmaCountLevelDiagnosis;
import com.learney.contentaudit.auditdomain.lemmacount.LemmaCountResult;
import com.learney.contentaudit.auditdomain.lemmacount.LevelLemmaCountResult;
import com.learney.contentaudit.auditdomain.quizinstruction.InstructionSeverity;
import com.learney.contentaudit.auditdomain.quizinstruction.QuizInstructionCoverageDiagnosis;
import com.learney.contentaudit.auditdomain.quizinstruction.QuizInstructionDiagnosis;
import com.learney.contentaudit.auditdomain.quizinstruction.QuizInstructionVerdict;
import static org.mockito.Mockito.lenient;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.CourseRepository;
import com.learney.contentaudit.evaluationledgerdomain.ContentFingerprinter;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationBudget;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationCoverage;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationEmitted;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationKey;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationLedger;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationOutcome;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRecord;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSession;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSessionFactory;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSubject;
import com.learney.contentaudit.evaluationledgerdomain.Evaluator;
import com.learney.contentaudit.evaluationledgerdomain.evaluationsession.DefaultEvaluationSessionFactory;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefaultAuditRunnerTest {

    // Role A/B (F-QINST-R015): the name under which the analyzer is selected/excluded/
    // budgeted -- what factory.analyzerName() returns, and the exact key used both in
    // AuditRunRequest.excludedAnalyzers and in AuditRunRequest.analyzerPolicies. This is
    // the name the audit report publishes.
    private static final String ANALYZER_NAME = "quiz-instruction";

    // Role C (F-QINST-R015): the judge's own identity in the evaluation ledger --
    // deliberately a DIFFERENT literal from ANALYZER_NAME. Collapsing both roles into a
    // single string is exactly the blind spot that let production filter/budget by the
    // wrong identity while 22 tests stayed green: with only one string, any identity the
    // runner reads produces the same result and the test cannot distinguish right from
    // wrong.
    private static final String EVALUATOR_ID = "quiz-instruction-validator";

    @Mock private CourseRepository courseRepository;
    @Mock private CourseToAuditableMapper courseToAuditableMapper;
    @Mock private AuditEngine auditEngine;

    private DefaultAuditRunner sut;
    private final Path coursePath = Path.of("/test/course.json");
    private final CourseEntity courseEntity = new CourseEntity();
    private final AuditableCourse auditableCourse = new AuditableCourse(List.of());
    private final AuditReport auditReport = new AuditReport(new AuditNode());

    @BeforeEach
    void setUp() {
        sut = new DefaultAuditRunner(courseRepository, courseToAuditableMapper, auditEngine, realCatalog());
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

        AuditReport result = sut.runAudit(coursePath, (Set<String>) null);

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

        sut.runAudit(coursePath, (Set<String>) null);

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

        sut.runAudit(coursePath, (Set<String>) null);

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

        sut.runAudit(coursePath, (Set<String>) null);

        verify(auditEngine).runAudit(auditableCourse);
    }

    @Test
    @DisplayName("Given courseRepository throws an exception, when runAudit is called, then the exception propagates")
    @Tag("F-CLI")
    @Tag("F-CLI-R001")
    public void givenCourseRepositoryThrowsAnExceptionWhenRunAuditIsCalledThenTheExceptionPropagates() {
        when(courseRepository.load(coursePath)).thenThrow(new RuntimeException("load failed"));

        assertThrows(RuntimeException.class, () -> sut.runAudit(coursePath, (Set<String>) null));
    }

    @Test
    @DisplayName("Given courseToAuditableMapper throws an exception, when runAudit is called, then the exception propagates")
    @Tag("F-CLI")
    @Tag("F-CLI-R001")
    public void givenCourseToAuditableMapperThrowsAnExceptionWhenRunAuditIsCalledThenTheExceptionPropagates() {
        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenThrow(new RuntimeException("map failed"));

        assertThrows(RuntimeException.class, () -> sut.runAudit(coursePath, (Set<String>) null));
    }

    @Test
    @DisplayName("Given contentAudit throws an exception, when runAudit is called, then the exception propagates")
    @Tag("F-CLI")
    @Tag("F-CLI-R001")
    public void givenContentAuditThrowsAnExceptionWhenRunAuditIsCalledThenTheExceptionPropagates() {
        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(auditableCourse);
        when(auditEngine.runAudit(auditableCourse)).thenThrow(new RuntimeException("audit failed"));

        assertThrows(RuntimeException.class, () -> sut.runAudit(coursePath, (Set<String>) null));
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

        AuditReport result = sut.runAudit(coursePath, (Set<String>) null);

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

        AuditReport result = sut.runAudit(coursePath, (Set<String>) null);

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
        AuditReport result = sut.runAudit(coursePath, (Set<String>) null);

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
        // FEAT-HALL: an instant analyzer asked by name runs as a selection of the engine
        lenient().when(auditEngine.runAudit(any(AuditableCourse.class), any(AnalyzerRunSelection.class)))
                .thenReturn(reportWithLemmaCount);

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

    @Test
    @DisplayName("should include the quiz instruction analysis when the audit request says nothing about analyzers")
    @Tag("FEAT-QINST")
    @Tag("F-QINST-R001")
    public void shouldIncludeTheQuizInstructionAnalysisWhenTheAuditRequestSaysNothingAboutAnalyzers() {
        // R001: the quiz instruction analysis is part of the audit like any other analysis --
        // it must run without the user having to ask for it. With a request that says nothing
        // about analyzers, the run must still build the quiz instruction analyzer from its
        // provider (the observable proof that it participated in this audit).
        AnalyzerProvider quizInstructionFactory = judgeProvider();
        when(quizInstructionFactory.create(any())).thenReturn(mock(ContentAnalyzer.class));

        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(courseWithQuizzes("quiz-1"));

        DefaultAuditRunner runner = runnerWith(quizInstructionFactory);

        AuditRunRequest request = new AuditRunRequest(null, null, null);
        runner.runAudit(coursePath, request);

        verify(quizInstructionFactory).create(any());
    }

    @Test
    @DisplayName("should report quiz instruction score, diagnoses and coverage on an audit asked with no options")
    @Tag("FEAT-QINST")
    @Tag("F-QINST-R001")
    public void shouldReportQuizInstructionScoreDiagnosesAndCoverageOnAnAuditAskedWithNoOptions() {
        // R001: an audit requested with no options must come back with the three things the
        // quiz instruction analysis produces -- score, diagnosis and coverage -- not just proof
        // that the analyzer was built.
        QuizInstructionVerdict verdict = new QuizInstructionVerdict(
                true, 0.9, InstructionSeverity.NONE, "Cumple la consigna", List.of(), List.of());
        QuizInstructionDiagnosis quizDiagnosis = new QuizInstructionDiagnosis(verdict, 1.0, false);
        EvaluationCoverage coverage = new EvaluationCoverage(1, 1, 1, 0, 0, 0);
        QuizInstructionCoverageDiagnosis coverageDiagnosis =
                new QuizInstructionCoverageDiagnosis(coverage, "v1", List.of(), null);

        ContentAnalyzer quizInstructionAnalyzer = mock(ContentAnalyzer.class);
        doAnswer(invocation -> {
            AuditNode node = invocation.getArgument(0);
            ((DefaultQuizDiagnoses) node.getDiagnoses()).setQuizInstructionDiagnosis(quizDiagnosis);
            node.getScores().put("quiz-instruction", 1.0);
            return null;
        }).when(quizInstructionAnalyzer).onQuiz(any());
        doAnswer(invocation -> {
            AuditNode node = invocation.getArgument(0);
            ((DefaultCourseDiagnoses) node.getDiagnoses()).setQuizInstructionCoverage(coverageDiagnosis);
            return null;
        }).when(quizInstructionAnalyzer).onCourseComplete(any());

        AnalyzerProvider quizInstructionFactory = judgeProvider();
        when(quizInstructionFactory.create(any())).thenReturn(quizInstructionAnalyzer);

        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(courseWithQuizzes("quiz-1"));

        DefaultAuditRunner runner = runnerWith(quizInstructionFactory);

        AuditRunRequest request = new AuditRunRequest(null, null, null);
        AuditReport result = runner.runAudit(coursePath, request);

        AuditNode quiz = firstQuiz(result);
        QuizDiagnoses resultQuizDiagnoses = (QuizDiagnoses) quiz.getDiagnoses();
        assertTrue(resultQuizDiagnoses.getQuizInstructionDiagnosis().isPresent(),
                "R001: the quiz node must carry the quiz instruction diagnosis when the audit runs with no options");
        assertEquals(1.0, resultQuizDiagnoses.getQuizInstructionDiagnosis().get().getScore(),
                "R001: the diagnosis must carry the score derived from the verdict");
        assertEquals(1.0, quiz.getScores().get("quiz-instruction"),
                "R001: the generic per-analyzer scores map must also carry the quiz instruction score");

        CourseDiagnoses resultCourseDiagnoses = (CourseDiagnoses) result.getRoot().getDiagnoses();
        assertTrue(resultCourseDiagnoses.getQuizInstructionCoverage().isPresent(),
                "R001: the report must declare the quiz instruction coverage when the audit runs with no options");
        assertEquals(coverage, resultCourseDiagnoses.getQuizInstructionCoverage().get().getCoverage(),
                "R001: the declared coverage must be the one produced by this run");
    }

    @Test
    @DisplayName("should not build the quiz instruction analyzer when the request excludes it")
    @Tag("FEAT-QINST")
    @Tag("F-QINST-R011")
    public void shouldNotBuildTheQuizInstructionAnalyzerWhenTheRequestExcludesIt() {
        // R011: an explicit exclusion must stop the analyzer from ever being built -- excluded
        // means "do not run", not "run and find nothing".
        AnalyzerProvider quizInstructionFactory = judgeProvider();

        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(courseWithQuizzes("quiz-1"));

        DefaultAuditRunner runner = runnerWith(quizInstructionFactory);

        AuditRunRequest request = new AuditRunRequest(null, Set.of("quiz-instruction"), null);
        runner.runAudit(coursePath, request);

        verify(quizInstructionFactory, never()).create(any());
    }

    @Test
    @DisplayName("should leave the report without quiz instruction score, diagnosis or coverage when the analysis is excluded")
    @Tag("FEAT-QINST")
    @Tag("F-QINST-R011")
    public void shouldLeaveTheReportWithoutQuizInstructionScoreDiagnosisOrCoverageWhenTheAnalysisIsExcluded() {
        // R011: excluded means the analysis does not appear in the report at all -- which is
        // different from appearing with zero coverage. Zero coverage would mean "I ran and
        // could not evaluate anything"; excluded means "you did not ask me to run". The
        // Optionals must come back empty, never populated with zeros.
        AnalyzerProvider quizInstructionFactory = judgeProvider();

        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(courseWithQuizzes("quiz-1"));

        DefaultAuditRunner runner = runnerWith(quizInstructionFactory);

        AuditRunRequest request = new AuditRunRequest(null, Set.of("quiz-instruction"), null);
        AuditReport result = runner.runAudit(coursePath, request);

        verify(quizInstructionFactory, never()).create(any());

        AuditNode quiz = firstQuiz(result);
        QuizDiagnoses resultQuizDiagnoses = (QuizDiagnoses) quiz.getDiagnoses();
        assertTrue(resultQuizDiagnoses.getQuizInstructionDiagnosis().isEmpty(),
                "R011: excluded analysis must leave no quiz instruction diagnosis on the quiz node");
        assertTrue(quiz.getScores().isEmpty(),
                "R011: excluded analysis must leave no quiz instruction score on the quiz node");

        CourseDiagnoses resultCourseDiagnoses = (CourseDiagnoses) result.getRoot().getDiagnoses();
        assertTrue(resultCourseDiagnoses.getQuizInstructionCoverage().isEmpty(),
                "R011: excluded analysis must leave no quiz instruction coverage on the report -- not even zero coverage");
    }

    @Test
    @DisplayName("should not run the quiz instruction analysis when the request narrows the audit to other analyzers")
    @Tag("FEAT-QINST")
    @Tag("F-QINST-R011")
    public void shouldNotRunTheQuizInstructionAnalysisWhenTheRequestNarrowsTheAuditToOtherAnalyzers() {
        // R011: asking for an audit narrowed to other analyzers has the same effect as
        // excluding the quiz instruction analysis explicitly -- it must not be built either.
        AnalyzerProvider quizInstructionFactory = judgeProvider();

        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(courseWithQuizzes("quiz-1"));

        DefaultAuditRunner runner = runnerWith(sentenceLengthStandIn(1.0), quizInstructionFactory);

        AuditRunRequest request = new AuditRunRequest(Set.of("sentence-length"), null, null);
        runner.runAudit(coursePath, request);

        verify(quizInstructionFactory, never()).create(any());
    }

    @Test
    @DisplayName("should finish the audit and keep the results of the other analyzers when the quiz instruction judge fails")
    @Tag("FEAT-QINST")
    @Tag("F-QINST-R007")
    public void shouldFinishTheAuditAndKeepTheResultsOfTheOtherAnalyzersWhenTheQuizInstructionJudgeFails() {
        // R007: a judge failure mid-run must not abort the audit -- the other analyzers, which
        // do not depend on the judge, must keep their results intact in the returned report.
        // Since FEAT-HALL the engine runs the judge with the other analyzers and isolates its
        // failure, so this goes through the real engine.
        ContentAnalyzer quizInstructionAnalyzer = mock(ContentAnalyzer.class);
        doThrow(new RuntimeException("quiz instruction judge unavailable"))
                .when(quizInstructionAnalyzer).onQuiz(any());
        lenient().doThrow(new RuntimeException("quiz instruction judge unavailable"))
                .when(quizInstructionAnalyzer).onCourseComplete(any());

        AnalyzerProvider quizInstructionFactory = judgeProvider();
        when(quizInstructionFactory.create(any())).thenReturn(quizInstructionAnalyzer);

        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(courseWithQuizzes("quiz-1"));

        DefaultAuditRunner runner = runnerWith(sentenceLengthStandIn(0.8), quizInstructionFactory);

        AuditRunRequest request = new AuditRunRequest(null, null, null);

        AuditReport result = assertDoesNotThrow(() -> runner.runAudit(coursePath, request),
                "R007: a judge failure must not abort the audit");

        assertEquals(0.8, firstQuiz(result).getScores().get("sentence-length"),
                "R007: scores from other analyzers must remain intact after a quiz instruction judge failure");
    }

    @Test
    @DisplayName("should hand the quiz instruction analyzer the run policy the request declared for its judge")
    @Tag("FEAT-QINST")
    @Tag("F-QINST-R006")
    public void shouldHandTheQuizInstructionAnalyzerTheRunPolicyTheRequestDeclaredForItsJudge() {
        // R006: each run bounds new judge queries through a configurable policy; the run must
        // hand the exact policy the request declared for this evaluator to its provider.
        AnalyzerProvider quizInstructionFactory = judgeProvider();
        when(quizInstructionFactory.create(any())).thenReturn(mock(ContentAnalyzer.class));

        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(courseWithQuizzes("quiz-1"));

        DefaultAuditRunner runner = runnerWith(quizInstructionFactory);

        EvaluationRunPolicy policy = new EvaluationRunPolicy(200, false, null, null);
        AuditRunRequest request = new AuditRunRequest(null, null, Map.of("quiz-instruction", policy));

        runner.runAudit(coursePath, request);

        verify(quizInstructionFactory).create(policy);
    }

    @Test
    @DisplayName("should not run the quiz instruction analysis when the run excludes it by the very name the report publishes, while the judge behind it answers to a different name")
    @Tag("FEAT-QINST")
    @Tag("F-QINST-R015")
    public void shouldNotRunTheQuizInstructionAnalysisWhenTheRunExcludesItByTheVeryNameTheReportPublishesWhileTheJudgeBehindItAnswersToADifferentName() {
        // R015: exclusion must be keyed by the name the report publishes (ANALYZER_NAME).
        // The judge wired behind this analysis answers to a DIFFERENT identity
        // (EVALUATOR_ID) on purpose -- if the run ever confused the two (the exact
        // blind spot this rule exists to close), excluding by ANALYZER_NAME would fail
        // to stop anything and the judge would still be consulted.
        FakeQuizInstructionEvaluationLedger ledger = new FakeQuizInstructionEvaluationLedger();
        FakeQuizInstructionContentFingerprinter fingerprinter = new FakeQuizInstructionContentFingerprinter();
        EvaluationSessionFactory sessionFactory = new DefaultEvaluationSessionFactory(ledger, fingerprinter);
        FakeQuizInstructionEvaluator evaluator = new FakeQuizInstructionEvaluator(EVALUATOR_ID, "v1");

        AnalyzerProvider quizInstructionFactory = judgeProvider();
        lenient().when(quizInstructionFactory.create(any())).thenAnswer(invocation -> {
            EvaluationRunPolicy policy = invocation.getArgument(0);
            EvaluationRunPolicy effectivePolicy = policy != null ? policy : new EvaluationRunPolicy(500, false, null, null);
            EvaluationSession session = sessionFactory.open(quizInstructionBudgetFrom(effectivePolicy), evaluator);
            return quizInstructionSessionBackedAnalyzer(session, effectivePolicy);
        });

        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(courseWithQuizzes("quiz-1"));

        DefaultAuditRunner runner = runnerWith(quizInstructionFactory);

        AuditRunRequest request = new AuditRunRequest(null, Set.of(ANALYZER_NAME), null);
        runner.runAudit(coursePath, request);

        verify(quizInstructionFactory, never()).create(any());
        assertTrue(evaluator.getEvaluatedSubjects().isEmpty(),
                "R015: excluding by the name the report publishes must stop the judge from ever being consulted, "
                + "even though the judge answers to a different identity (EVALUATOR_ID) than that name");
    }

    @Test
    @DisplayName("should cap the judge queries at the number requested for the name the report publishes instead of falling back to the default cap, while the judge behind it answers to a different name")
    @Tag("FEAT-QINST")
    @Tag("F-QINST-R015")
    public void shouldCapTheJudgeQueriesAtTheNumberRequestedForTheNameTheReportPublishesInsteadOfFallingBackToTheDefaultCapWhileTheJudgeBehindItAnswersToADifferentName() {
        // R015: the cap declared for ANALYZER_NAME (the name the report publishes) must be
        // the one applied -- not the analyzer's default (500). Three quizzes reach the
        // judge but the request caps new queries at 2: if the run looked up the policy
        // under any identity other than ANALYZER_NAME, it would find nothing and fall back
        // to the default, and all three quizzes would be consulted instead of two.
        FakeQuizInstructionEvaluationLedger ledger = new FakeQuizInstructionEvaluationLedger();
        FakeQuizInstructionContentFingerprinter fingerprinter = new FakeQuizInstructionContentFingerprinter();
        EvaluationSessionFactory sessionFactory = new DefaultEvaluationSessionFactory(ledger, fingerprinter);
        FakeQuizInstructionEvaluator evaluator = new FakeQuizInstructionEvaluator(EVALUATOR_ID, "v1");

        AnalyzerProvider quizInstructionFactory = judgeProvider();
        when(quizInstructionFactory.create(any())).thenAnswer(invocation -> {
            EvaluationRunPolicy policy = invocation.getArgument(0);
            EvaluationSession session = sessionFactory.open(quizInstructionBudgetFrom(policy), evaluator);
            return quizInstructionSessionBackedAnalyzer(session, policy);
        });

        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(courseWithQuizzes("quiz-1", "quiz-2", "quiz-3"));

        DefaultAuditRunner runner = runnerWith(quizInstructionFactory);

        EvaluationRunPolicy cappedPolicy = new EvaluationRunPolicy(2, false, null, null);
        AuditRunRequest request = new AuditRunRequest(null, null, Map.of(ANALYZER_NAME, cappedPolicy));
        runner.runAudit(coursePath, request);

        verify(quizInstructionFactory).create(cappedPolicy);
        assertEquals(2, evaluator.getEvaluatedSubjects().size(),
                "R015: the run must cap new judge queries at the number requested for the name the report "
                + "publishes (2 of 3 quizzes) instead of falling back to the analyzer's default cap");
    }

    @Test
    @DisplayName("should re evaluate quizzes that already had a verdict when re evaluation is requested for the name the report publishes, while the judge behind it answers to a different name")
    @Tag("FEAT-QINST")
    @Tag("F-QINST-R015")
    public void shouldReEvaluateQuizzesThatAlreadyHadAVerdictWhenReEvaluationIsRequestedForTheNameTheReportPublishesWhileTheJudgeBehindItAnswersToADifferentName() {
        // R015: an explicit re-evaluation request keyed by ANALYZER_NAME must reach the
        // judge again for a quiz that already has a current verdict -- even though the
        // judge's own identity in the ledger (EVALUATOR_ID) is a different string. If the
        // run looked up the reevaluation policy under any name other than the one the
        // report publishes, the request would silently find nothing and every verdict
        // would just be reused.
        FakeQuizInstructionEvaluationLedger ledger = new FakeQuizInstructionEvaluationLedger();
        FakeQuizInstructionContentFingerprinter fingerprinter = new FakeQuizInstructionContentFingerprinter();
        EvaluationSessionFactory sessionFactory = new DefaultEvaluationSessionFactory(ledger, fingerprinter);
        FakeQuizInstructionEvaluator evaluator = new FakeQuizInstructionEvaluator(EVALUATOR_ID, "v1");

        String fingerprint = fingerprinter.fingerprint(quizInstructionSubjectContent("quiz-1"));
        EvaluationKey currentKey = new EvaluationKey(EVALUATOR_ID, fingerprint);
        ledger.seed(new EvaluationRecord(currentKey, "{\"compliant\":true}", "quiz-1", Instant.now(), "v1"));

        AnalyzerProvider quizInstructionFactory = judgeProvider();
        when(quizInstructionFactory.create(any())).thenAnswer(invocation -> {
            EvaluationRunPolicy policy = invocation.getArgument(0);
            EvaluationSession session = sessionFactory.open(quizInstructionBudgetFrom(policy), evaluator);
            return quizInstructionSessionBackedAnalyzer(session, policy);
        });

        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(courseWithQuizzes("quiz-1"));

        DefaultAuditRunner runner = runnerWith(quizInstructionFactory);

        EvaluationRunPolicy reevaluationPolicy = new EvaluationRunPolicy(500, true, null, null);
        AuditRunRequest request = new AuditRunRequest(null, null, Map.of(ANALYZER_NAME, reevaluationPolicy));
        runner.runAudit(coursePath, request);

        verify(quizInstructionFactory).create(reevaluationPolicy);
        assertEquals(1, evaluator.getEvaluatedSubjects().size(),
                "R015: re-evaluation requested for the name the report publishes must reach the judge again "
                + "despite a current verdict already existing");
        assertEquals(2, ledger.history(currentKey).size(),
                "R015: re-evaluation must not destroy the previous verdict -- both must coexist as history");
    }

    // -----------------------------------------------------------------------
    // FEAT-HALL: the runner delegates every run to the engine, which builds each analyzer
    // of the run from its provider in the catalog -- the judge included. These fixtures wire
    // a real engine with the given providers; the analyzers here are stand-ins, so their
    // findings are not collected.
    // -----------------------------------------------------------------------

    private DefaultAuditRunner runnerWith(AnalyzerProvider... providers) {
        AnalyzerCatalog catalog = new DefaultAnalyzerCatalog(List.of(providers));
        IAuditEngine engine = new IAuditEngine(new IScoreAggregator(), catalog, (root, analyzer, card) -> { },
                new DefaultContextNumbersCalculator());
        return new DefaultAuditRunner(courseRepository, courseToAuditableMapper, engine, catalog);
    }

    /** The judge's provider under the name the report publishes, with the judge's real card. */
    private static AnalyzerProvider judgeProvider() {
        AnalyzerDescriptor card = new DefaultQuizInstructionAnalyzerFactory(null, null, null,
                mock(QuizInstructionConfig.class)).describe();
        AnalyzerProvider provider = mock(AnalyzerProvider.class);
        lenient().when(provider.analyzerName()).thenReturn(ANALYZER_NAME);
        lenient().when(provider.describe()).thenReturn(card);
        return provider;
    }

    /** A sentence-length stand-in, with the real card, that scores every quiz with the given value. */
    private static AnalyzerProvider sentenceLengthStandIn(double score) {
        AnalyzerDescriptor card = new SentenceLengthAnalyzerProvider(null, mock(SentenceLengthConfig.class)).describe();
        ContentAnalyzer analyzer = new ContentAnalyzer() {
            @Override
            public Void onQuiz(AuditNode node) {
                node.getScores().put("sentence-length", score);
                return null;
            }

            @Override
            public Void onKnowledge(AuditNode node) {
                return null;
            }

            @Override
            public Void onMilestone(AuditNode node) {
                return null;
            }

            @Override
            public Void onTopic(AuditNode node) {
                return null;
            }

            @Override
            public Void onCourseComplete(AuditNode rootNode) {
                return null;
            }

            @Override
            public String getName() {
                return "sentence-length";
            }

            @Override
            public AuditTarget getTarget() {
                return AuditTarget.QUIZ;
            }

            @Override
            public String getDescription() {
                return "sentence-length stand-in";
            }

            @Override
            public List<FindingDraft> findingsAt(AuditNode node) {
                return List.of();
            }
        };
        AnalyzerProvider provider = mock(AnalyzerProvider.class);
        lenient().when(provider.analyzerName()).thenReturn("sentence-length");
        lenient().when(provider.describe()).thenReturn(card);
        lenient().when(provider.create(any())).thenReturn(analyzer);
        return provider;
    }

    /** One A1 milestone, one topic and one knowledge with these quizzes. */
    private static AuditableCourse courseWithQuizzes(String... quizIds) {
        List<AuditableQuiz> quizzes = new ArrayList<>();
        for (String quizId : quizIds) {
            quizzes.add(new AuditableQuiz(List.of(), quizId, "label", "code", null,
                    List.of("She is happy."), null, null, List.of(), null));
        }
        AuditableKnowledge knowledge = new AuditableKnowledge(quizzes, "Knowledge", "Instructions", true,
                "k1", "Knowledge", "K1", null, "Topic");
        AuditableTopic topic = new AuditableTopic(List.of(knowledge), "t1", "Topic", "T1");
        return new AuditableCourse(List.of(new AuditableMilestone(List.of(topic), "m1", "A1", "A1")));
    }

    private static AuditNode firstQuiz(AuditReport report) {
        return report.getRoot().getChildren().get(0).getChildren().get(0).getChildren().get(0).getChildren().get(0);
    }

    /** The eight real providers, as Main registers them; mocked configurations feed their cards. */
    private static AnalyzerCatalog realCatalog() {
        return new DefaultAnalyzerCatalog(List.of(
                new SentenceLengthAnalyzerProvider(null, mock(SentenceLengthConfig.class)),
                new KnowledgeTitleLengthAnalyzerProvider(),
                new KnowledgeInstructionsLengthAnalyzerProvider(),
                new CocaBucketsAnalyzerProvider(null, mock(CocaBucketsConfig.class)),
                new LemmaRecurrenceAnalyzerProvider(mock(LemmaRecurrenceConfig.class)),
                new LemmaAbsenceAnalyzerProvider(null, mock(LemmaAbsenceConfig.class), null),
                new LemmaCountAnalyzerProvider(null, mock(LemmaCountConfig.class)),
                new DefaultQuizInstructionAnalyzerFactory(null, null, null, mock(QuizInstructionConfig.class))));
    }

    // -----------------------------------------------------------------------
    // Fixtures shared across the F-QINST-R015 tests
    // -----------------------------------------------------------------------

    private static Map<String, String> quizInstructionSubjectContent(String quizId) {
        return Map.of("marker", quizId);
    }

    private static EvaluationBudget quizInstructionBudgetFrom(EvaluationRunPolicy policy) {
        return new EvaluationBudget(policy.getMaxNewEvaluations());
    }

    /**
     * A minimal ContentAnalyzer that delegates each quiz to a real EvaluationSession, so
     * these tests exercise the actual reuse/budget/version machinery (FEAT-EVCOST) instead
     * of a fully-mocked analyzer -- necessary to observe the cap and the re-evaluation
     * behavior for real. Only the judge (Evaluator) and the ledger are faked.
     */
    private static ContentAnalyzer quizInstructionSessionBackedAnalyzer(EvaluationSession session,
            EvaluationRunPolicy policy) {
        return new ContentAnalyzer() {
            @Override
            public Void onQuiz(AuditNode node) {
                String subjectRef = node.getEntity() != null ? node.getEntity().getId() : null;
                EvaluationSubject subject = new EvaluationSubject(subjectRef, quizInstructionSubjectContent(subjectRef));
                if (policy != null && policy.isReevaluate()) {
                    session.resolveForced(subject);
                } else {
                    session.resolve(subject);
                }
                return null;
            }

            @Override
            public Void onKnowledge(AuditNode node) {
                return null;
            }

            @Override
            public Void onMilestone(AuditNode node) {
                return null;
            }

            @Override
            public Void onTopic(AuditNode node) {
                return null;
            }

            @Override
            public Void onCourseComplete(AuditNode rootNode) {
                return null;
            }

            @Override
            public String getName() {
                return ANALYZER_NAME;
            }

            @Override
            public AuditTarget getTarget() {
                return AuditTarget.QUIZ;
            }

            @Override
            public String getDescription() {
                return "fake quiz instruction analyzer for F-QINST-R015 tests";
            }

            @Override
            public List<FindingDraft> findingsAt(AuditNode node) {
                return List.of();
            }
        };
    }

    /** Deterministic fake fingerprinter: same content map always yields the same fingerprint. */
    private static final class FakeQuizInstructionContentFingerprinter implements ContentFingerprinter {
        @Override
        public String fingerprint(Map<String, String> content) {
            return new TreeMap<>(content).toString();
        }
    }

    /** In-memory fake ledger -- the real FileSystemEvaluationLedger lives in a module that
     * audit-application does not depend on. */
    private static final class FakeQuizInstructionEvaluationLedger implements EvaluationLedger {
        private final List<EvaluationRecord> records = new ArrayList<>();

        @Override
        public Optional<EvaluationRecord> findLatest(EvaluationKey key) {
            return records.stream().filter(r -> r.getKey().equals(key)).findFirst();
        }

        @Override
        public void append(EvaluationRecord record) {
            records.add(record);
        }

        @Override
        public List<EvaluationRecord> history(EvaluationKey key) {
            List<EvaluationRecord> result = new ArrayList<>();
            for (EvaluationRecord record : records) {
                if (record.getKey().equals(key)) {
                    result.add(record);
                }
            }
            return result;
        }

        private void seed(EvaluationRecord record) {
            records.add(record);
        }
    }

    /** Fake judge -- never calls a real LLM. Records every subject it was asked to judge,
     * under an identity (EVALUATOR_ID) deliberately distinct from ANALYZER_NAME. */
    private static final class FakeQuizInstructionEvaluator implements Evaluator {
        private final String id;
        private final String version;
        private final List<EvaluationSubject> evaluatedSubjects = new ArrayList<>();

        private FakeQuizInstructionEvaluator(String id, String version) {
            this.id = id;
            this.version = version;
        }

        @Override
        public String evaluatorId() {
            return id;
        }

        @Override
        public Optional<String> evaluatorVersion() {
            return Optional.ofNullable(version);
        }

        @Override
        public EvaluationOutcome evaluate(EvaluationSubject subject) {
            evaluatedSubjects.add(subject);
            return new EvaluationEmitted("{\"compliant\":true}");
        }

        private List<EvaluationSubject> getEvaluatedSubjects() {
            return evaluatedSubjects;
        }
    }

    @Test
    @DisplayName("should run exactly sentence-length when a run asks only for it, leaving no score, finding or number of the other six classic analyzers")
    @Tag("FEAT-HALL")
    @Tag("F-HALL-R012")
    public void shouldRunExactlySentencelengthWhenARunAsksOnlyForItLeavingNoScoreFindingOrNumberOfTheOtherSixClassicAnalyzers() {
        // R012: asking for sentence-length runs exactly it -- before the contract the selection
        // only filtered the judges and the seven classics ran anyway.
        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(courseWithQuizzes("q1", "q2"));
        DefaultAuditRunner runner = realRunnerWith(classicProviders(mock(EvpCatalogPort.class)));

        AuditReport report = runner.runAudit(coursePath, new AuditRunRequest(Set.of("sentence-length"), null, null));

        walkTree(report.getRoot(), node -> {
            assertTrue(Set.of("sentence-length").containsAll(node.getScores().keySet()),
                    "R012: no score of another analyzer: " + node.getScores());
            node.getFindings().forEach(f -> assertEquals("sentence-length", f.getAnalyzer(), "R012: no finding of another"));
            assertEquals(Set.of("sentence-length"), node.getNumbers().getAnalyzerScores().stream()
                    .map(AnalyzerScore::getAnalyzer).collect(java.util.stream.Collectors.toSet()),
                    "R012: no number of another");
        });
        assertFalse(firstQuiz(report).getFindings().isEmpty(), "its own findings are there");
    }

    @Test
    @DisplayName("should run exactly quiz-instruction when a run asks only for it, with its findings, numbers and coverage and nothing from the seven classic analyzers")
    @Tag("FEAT-HALL")
    @Tag("F-HALL-R012")
    public void shouldRunExactlyQuizinstructionWhenARunAsksOnlyForItWithItsFindingsNumbersAndCoverageAndNothingFromTheSevenClassicAnalyzers() {
        // R012: asking for the judge runs exactly the judge, with its findings, its numbers and its
        // coverage -- and nothing of the seven classics.
        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        // Each quiz with its own content: the judge reuses a verdict for identical content (F-EVCOST-R001).
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(courseWithDistinctQuizzes("q-breach", "q-ok"));
        List<AnalyzerProvider> providers = new ArrayList<>(classicProviders(mock(EvpCatalogPort.class)));
        providers.add(breachingJudge());
        DefaultAuditRunner runner = realRunnerWith(providers);

        AuditReport report = runner.runAudit(coursePath, new AuditRunRequest(Set.of("quiz-instruction"), null, null));

        walkTree(report.getRoot(), node -> {
            assertTrue(Set.of("quiz-instruction").containsAll(node.getScores().keySet()),
                    "R012: nothing of the seven classics: " + node.getScores());
            node.getFindings().forEach(f -> assertEquals("quiz-instruction", f.getAnalyzer()));
        });
        AuditNode breach = firstQuiz(report);
        assertEquals(1, breach.getFindings().size(), "R012: its findings");
        assertEquals(1, report.getRoot().getNumbers().getErrors().getWithAnyError(), "R012: its numbers");
        assertTrue(((CourseDiagnoses) report.getRoot().getDiagnoses()).getQuizInstructionCoverage().isPresent(),
                "R012: its coverage");
    }

    @Test
    @DisplayName("should reject before loading the course a run that asks for quiz-instructions, a name get analyzers does not list, with the message that points to get analyzers")
    @Tag("FEAT-HALL")
    @Tag("F-HALL-R012")
    public void shouldRejectBeforeLoadingTheCourseARunThatAsksForQuizinstructionsANameGetAnalyzersDoesNotListWithTheMessageThatPointsToGetAnalyzers() {
        // R012: a name that get analyzers does not list rejects the run before the course is loaded
        // (loading it tokenizes it with spaCy), with the message of F-CLIRV-R016.
        DefaultAuditRunner runner = new DefaultAuditRunner(courseRepository, courseToAuditableMapper, auditEngine,
                realCatalog());

        UnknownAnalyzerException rejected = assertThrows(UnknownAnalyzerException.class,
                () -> runner.runAudit(coursePath, new AuditRunRequest(Set.of("quiz-instructions"), null, null)));

        assertEquals("Analyzer 'quiz-instructions' not found. Run 'content-audit get analyzers' to see available analyzers.",
                rejected.getMessage());
        verify(courseRepository, never()).load(any());
        verifyNoInteractions(auditEngine);
    }

    @Test
    @DisplayName("should reject before loading the course a run that excludes a name get analyzers does not list, instead of ignoring it")
    @Tag("FEAT-HALL")
    @Tag("F-HALL-R012")
    public void shouldRejectBeforeLoadingTheCourseARunThatExcludesANameGetAnalyzersDoesNotListInsteadOfIgnoringIt() {
        // R012: excluding a name the catalog does not have is rejected too, instead of ignored.
        DefaultAuditRunner runner = new DefaultAuditRunner(courseRepository, courseToAuditableMapper, auditEngine,
                realCatalog());

        UnknownAnalyzerException rejected = assertThrows(UnknownAnalyzerException.class,
                () -> runner.runAudit(coursePath, new AuditRunRequest(null, Set.of("quiz-instructions"), null)));

        assertEquals("quiz-instructions", rejected.getAnalyzerName());
        verify(courseRepository, never()).load(any());
        verifyNoInteractions(auditEngine);
    }

    @Test
    @DisplayName("should publish on the 29/9 course, with the seven classic analyzers, the same score of each one on each of its 11.760 nodes and the same typed diagnoses as the analysis 2026-09-30T11-54-02")
    @Tag("FEAT-HALL")
    @Tag("F-HALL-R013")
    public void shouldPublishOnThe299CourseWithTheSevenClassicAnalyzersTheSameScoreOfEachOneOnEachOfIts11760NodesAndTheSameTypedDiagnosesAsTheAnalysis20260930T115402() {
        // R013: the course of the 29/9 base (db/english-course), the spaCy tokens it had then and the
        // real configurations and EVP catalog give, node by node, the scores and the typed
        // diagnoses of the analysis 2026-09-30T11-54-02 (fixtures made by scripts/fhall_base_fixtures.py).
        CourseToAuditableMapper mapper = new CourseToAuditableMapper(new RecordedTokenizer(),
                DefaultQuizSentenceConverter.create());
        EvpCatalogPort evp = new FileSystemEvpCatalog(Path.of("../analysis/recursos-compartidos/enriched_vocabulary_catalog.json"));
        List<AnalyzerProvider> classics = classicProviders(evp);
        AnalyzerCatalog catalog = new DefaultAnalyzerCatalog(classics);
        DefaultAuditRunner runner = new DefaultAuditRunner(new FileSystemCourseRepository(new CourseValidatorImpl()), mapper,
                new IAuditEngine(new IScoreAggregator(), catalog, new DefaultFindingCollector(),
                        new DefaultContextNumbersCalculator()), catalog);

        AuditReport report = runner.runAudit(Path.of("../db/english-course"), (Set<String>) null);

        List<String> expectedNodes = readFixture("nodes.jsonl.gz");
        List<String> expectedDiagnoses = readFixture("diagnoses.jsonl.gz");
        List<AuditNode> nodes = new ArrayList<>();
        walkTree(report.getRoot(), nodes::add);
        assertEquals(11760, nodes.size(), "R013: the 11.760 nodes of the 29/9 course");
        assertEquals(expectedNodes.size(), nodes.size());
        ObjectMapper json = diagnosesMapper();
        for (int i = 0; i < nodes.size(); i++) {
            AuditNode node = nodes.get(i);
            JsonNode expected = parse(json, expectedNodes.get(i));
            String id = node.getEntity() != null ? node.getEntity().getId() : "root";
            assertEquals(expected.get(0).asText() + " " + expected.get(1).asText(), node.getTarget() + " " + id,
                    "the same node in the same place");
            Map<String, Double> expectedScores = new LinkedHashMap<>();
            expected.get(2).fields().forEachRemaining(e -> expectedScores.put(e.getKey(), e.getValue().asDouble()));
            assertEquals(expectedScores, node.getScores(), "R013: the same score of each analyzer on " + id);
            assertEquals(new ArrayList<>(expectedScores.keySet()), new ArrayList<>(node.getScores().keySet()),
                    "and in the same order on " + id);
            assertEquals(parse(json, expectedDiagnoses.get(i)), diagnosesTree(json, node),
                    "R013: the same typed diagnoses on " + id);
        }
        assertEquals(new HashSet<>(List.of("sentence-length", "knowledge-title-length", "knowledge-instructions-length",
                "coca-buckets-distribution", "lemma-recurrence", "lemma-absence", "lemma-count")),
                new HashSet<>(report.getRoot().getNumbers().getAnalyzerScores().stream().map(AnalyzerScore::getAnalyzer)
                        .toList()), "the seven classic analyzers ran");
    }

    // -----------------------------------------------------------------------
    // FEAT-HALL: the seven classics with the configurations the code applies, the real engine
    // with the real collector, and the 29/9 base
    // -----------------------------------------------------------------------

    private DefaultAuditRunner realRunnerWith(List<AnalyzerProvider> providers) {
        AnalyzerCatalog catalog = new DefaultAnalyzerCatalog(providers);
        return new DefaultAuditRunner(courseRepository, courseToAuditableMapper, new IAuditEngine(new IScoreAggregator(),
                catalog, new DefaultFindingCollector(), new DefaultContextNumbersCalculator()), catalog);
    }

    /** The seven classic providers as Main builds them, over the given EVP catalog. */
    private static List<AnalyzerProvider> classicProviders(EvpCatalogPort evp) {
        DefaultLemmaAbsenceConfig absence = new DefaultLemmaAbsenceConfig();
        return List.of(
                new SentenceLengthAnalyzerProvider(null, new DefaultSentenceLengthConfig()),
                new KnowledgeTitleLengthAnalyzerProvider(),
                new KnowledgeInstructionsLengthAnalyzerProvider(),
                new CocaBucketsAnalyzerProvider(null, new DefaultCocaBucketsConfig()),
                new LemmaRecurrenceAnalyzerProvider(new DefaultLemmaRecurrenceConfig()),
                new LemmaAbsenceAnalyzerProvider(evp, absence,
                        new DefaultSentenceLexicalScorer(evp, new DefaultContentWordFilter(), absence)),
                new LemmaCountAnalyzerProvider(evp, new DefaultLemmaCountConfigLoader().load(null)));
    }

    /** The real judge factory whose judge finds a major breach in q-breach and nothing in any other quiz. */
    private static AnalyzerProvider breachingJudge() {
        Evaluator evaluator = new Evaluator() {
            @Override
            public String evaluatorId() {
                return EVALUATOR_ID;
            }

            @Override
            public Optional<String> evaluatorVersion() {
                return Optional.of("v1");
            }

            @Override
            public EvaluationOutcome evaluate(EvaluationSubject subject) {
                return new EvaluationEmitted("q-breach".equals(subject.getSubjectRef()) ? "breach" : "ok");
            }
        };
        QuizInstructionVerdictReader reader = payload -> "breach".equals(payload)
                ? new QuizInstructionVerdict(false, 0.9, InstructionSeverity.MAJOR, "Incumple la consigna",
                        List.of(new com.learney.contentaudit.auditdomain.quizinstruction.InstructionViolation(
                                "TENSE", "Pasado", "goes", "Esta en presente")), List.of())
                : new QuizInstructionVerdict(true, 0.9, InstructionSeverity.NONE, "Cumple", List.of(), List.of());
        return new DefaultQuizInstructionAnalyzerFactory(new DefaultEvaluationSessionFactory(
                new FakeQuizInstructionEvaluationLedger(), new FakeQuizInstructionContentFingerprinter()), evaluator,
                reader, new DefaultQuizInstructionConfig());
    }

    private static void walkTree(AuditNode node, Consumer<AuditNode> action) {
        action.accept(node);
        if (node.getChildren() != null) {
            node.getChildren().forEach(child -> walkTree(child, action));
        }
    }

    private static List<String> readFixture(String name) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(
                DefaultAuditRunnerTest.class.getResourceAsStream("/fhall-base-2026-09-30/" + name)),
                StandardCharsets.UTF_8))) {
            return reader.lines().toList();
        } catch (IOException e) {
            throw new AssertionError("fixture " + name + " unreadable", e);
        }
    }

    private static JsonNode parse(ObjectMapper json, String line) {
        try {
            return json.readTree(line);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    /** The typed diagnoses serialized as the report store does: polymorphic, with Optionals unwrapped. */
    private static ObjectMapper diagnosesMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.addMixIn(NodeDiagnoses.class, NodeDiagnosesMixin.class);
        return mapper;
    }

    private static JsonNode diagnosesTree(ObjectMapper json, AuditNode node) {
        try {
            return json.readTree(json.writerFor(NodeDiagnoses.class).writeValueAsString(node.getDiagnoses()));
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = DefaultCourseDiagnoses.class, name = "CourseDiagnoses"),
        @JsonSubTypes.Type(value = DefaultLevelDiagnoses.class, name = "LevelDiagnoses"),
        @JsonSubTypes.Type(value = DefaultTopicDiagnoses.class, name = "TopicDiagnoses"),
        @JsonSubTypes.Type(value = DefaultKnowledgeDiagnoses.class, name = "KnowledgeDiagnoses"),
        @JsonSubTypes.Type(value = DefaultQuizDiagnoses.class, name = "QuizDiagnoses")
    })
    abstract static class NodeDiagnosesMixin {
    }

    /** The spaCy tokens each canonical sentence of the 29/9 course had in the analysis 2026-09-30T11-54-02. */
    private static final class RecordedTokenizer implements NlpTokenizer {
        private final Map<String, List<NlpToken>> bySentence = new java.util.HashMap<>();

        RecordedTokenizer() {
            ObjectMapper json = new ObjectMapper();
            List<NlpToken> table = new ArrayList<>();
            for (String line : readFixture("tokens.jsonl.gz")) {
                JsonNode t = parse(json, line);
                table.add(new NlpToken(t.get(0).asText(), t.get(1).asText(), t.get(2).asText(),
                        t.get(3).isNull() ? null : t.get(3).asInt(), t.get(4).asBoolean(), t.get(5).asBoolean()));
            }
            for (String line : readFixture("sentences.jsonl.gz")) {
                JsonNode s = parse(json, line);
                List<NlpToken> tokens = new ArrayList<>();
                s.get(1).forEach(index -> tokens.add(table.get(index.asInt())));
                bySentence.put(s.get(0).asText(), tokens);
            }
        }

        @Override
        public Map<String, List<NlpToken>> analyzeTokensBatch(List<String> sentences) {
            Map<String, List<NlpToken>> result = new java.util.HashMap<>();
            for (String sentence : sentences) {
                List<NlpToken> tokens = bySentence.get(sentence);
                if (tokens == null) {
                    throw new AssertionError("a sentence the 29/9 course did not have: " + sentence);
                }
                result.put(sentence, tokens);
            }
            return result;
        }

        @Override
        public List<String> tokenize(String text) {
            throw new UnsupportedOperationException("the audit tokenizes in batch");
        }

        @Override
        public int countTokens(String text) {
            throw new UnsupportedOperationException("the audit tokenizes in batch");
        }

        @Override
        public List<NlpToken> analyzeTokens(String text) {
            throw new UnsupportedOperationException("the audit tokenizes in batch");
        }
    }

    /** Like courseWithQuizzes, but each quiz with its own text, so the judge sees different content. */
    private static AuditableCourse courseWithDistinctQuizzes(String... quizIds) {
        List<AuditableQuiz> quizzes = new ArrayList<>();
        for (String quizId : quizIds) {
            quizzes.add(new AuditableQuiz(List.of(), quizId, "label", "code", null, List.of("She is " + quizId + "."),
                    null, null, List.of(new com.learney.contentaudit.coursedomain.SentencePartEntity(
                            com.learney.contentaudit.coursedomain.SentencePartKind.TEXT, "She is " + quizId, null)), null));
        }
        AuditableKnowledge knowledge = new AuditableKnowledge(quizzes, "Knowledge", "Instructions", true,
                "k1", "Knowledge", "K1", null, "Topic");
        AuditableTopic topic = new AuditableTopic(List.of(knowledge), "t1", "Topic", "T1");
        return new AuditableCourse(List.of(new AuditableMilestone(List.of(topic), "m1", "A1", "A1")));
    }
}
