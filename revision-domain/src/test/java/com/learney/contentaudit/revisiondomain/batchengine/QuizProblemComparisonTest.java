package com.learney.contentaudit.revisiondomain.batchengine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.analysis.DefaultAnalysisComposer;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.batch.QuizCorrection.*;
import com.learney.contentaudit.refinerdomain.*;
import com.learney.contentaudit.auditdomain.judging.*;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerMeasure;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;

class QuizProblemComparisonTest {
    final String quiz = TandaFixture.RESIDE;
    final CorrectionBatch batch = TandaFixture.batch("batch", "task");
    final BatchServiceConfig config = new BatchServiceConfig();
    final CurrentAnalysisService current = mock(CurrentAnalysisService.class);
    final AnalyzerCatalog catalog = mock(AnalyzerCatalog.class);
    final SelectiveCorrection.Run run = new SelectiveCorrection.Run("operation", "recorrect", "all-current-judges", null,
            1, 10, "before-fp", "all", "runtime", "before-audit", "plan", List.of("task"), List.of(quiz),
            1000, 1000, 9, 1, 0, 0, "DONE", "Jose", Instant.EPOCH, Instant.EPOCH, null, false, 11, "after-fp", "runtime");

    @BeforeEach void setup() {
        batch.setContentVersion(11); batch.setAnalyzers(List.of("hint-coherence", "single-answer"));
        config.setCurrentAnalysisService(current); config.setAnalyzerCatalog(catalog);
        when(current.supportedAnalyzers()).thenReturn(Set.of("single-answer"));
        var nativeControl = new AnalyzerDescriptor(); nativeControl.setCost(AnalysisCost.INSTANT);
        when(catalog.find("hint-coherence")).thenReturn(Optional.of(nativeControl));
    }
    Finding finding(String analyzer, String rule, FindingSeverity severity, String marker, String observation) {
        return new Finding(analyzer, rule, new FindingNodeRef(AuditTarget.QUIZ, quiz, quiz), severity,
                new FindingEvidence(List.of(new EvidencePart("Ejercicio", "Some exercise")), observation, List.of()),
                FindingResolution.PANEL, null, new FindingIdentity(analyzer + "|" + rule + "|" + quiz + "|" + marker, marker));
    }
    AuditReport report(List<Finding> findings) {
        var report = TandaFixture.report(TandaFixture.course(), findings, Map.of(), List.of());
        var node = node(report);
        node.getScores().putAll(Map.of("hint-coherence", 1.0, "single-answer", findings.isEmpty() ? 1.0 : 0.0));
        report.getRoot().setMeasures(List.of(new AnalyzerMeasure("hint-coherence", "hint-config-v1", Map.of()),
                new AnalyzerMeasure("single-answer", "single-answer-config-v1", Map.of())));
        provenance(report, "v1", "EXPLICIT[provider=codex-cli, model=model, reasoning=medium]");
        return report;
    }
    AuditNode node(AuditReport report) { return DefaultAnalysisComposer.index(report.getRoot()).get(quiz); }
    void provenance(AuditReport report, String version, String runtime) {
        node(report).getMetadata().put("verdictVersions", Map.of("single-answer", version));
        node(report).getMetadata().put("analysisProvenance", Map.of("single-answer", Map.of("runtime", runtime)));
    }
    Comparison compare(AuditReport before, AuditReport after) {
        return new DefaultQuizProblemComparison(config).compare(batch, run, before, after, true);
    }
    ProblemEvolution row(Comparison comparison, String rule) {
        return comparison.quizzes().get(0).rows().stream().filter(r -> Objects.equals(rule, r.rule())).findFirst().orElseThrow();
    }

    @Test void nativeAnalyzerResolutionRequiresAnExplicitCompleteAfterMeasurement() {
        var before = report(List.of(finding("hint-coherence", "missing-hint", FindingSeverity.HIGH, "hint", "Missing hint")));
        var after = report(List.of());
        assertEquals("RESOLVED", row(compare(before, after), "missing-hint").status());
        node(after).getScores().remove("hint-coherence");
        assertEquals("PENDING", row(compare(before, after), "missing-hint").status());
    }
    @Test void absentFindingsDuringPartialCoverageNeverBecomeResolved() {
        var before = report(List.of(finding("single-answer", "unique-answer", FindingSeverity.BLOCKING, "gap1", "An alternative")));
        var after = report(List.of());
        node(after).setPartiallyEvaluatedBy(List.of("single-answer"));
        var row = row(compare(before, after), "unique-answer");
        assertEquals("PENDING", row.status()); assertEquals("PARTIAL", row.afterCoverage());
        assertEquals(FindingSeverity.BLOCKING, row.beforeSeverity()); assertNull(row.afterSeverity());
    }
    @Test void changedVersionAndChangedRuntimeAreEachNotComparable() {
        var before = report(List.of(finding("single-answer", "unique-answer", FindingSeverity.HIGH, "gap1", "Before")));
        for (String[] provenance : List.of(new String[]{"v2", "EXPLICIT[provider=codex-cli, model=model, reasoning=medium]"},
                new String[]{"v1", "EXPLICIT[provider=another, model=other, reasoning=medium]"})) {
            var after = report(List.of()); provenance(after, provenance[0], provenance[1]);
            var row = row(compare(before, after), "unique-answer");
            assertEquals("PENDING", row.status()); assertTrue(row.reason().contains("versiones o los motores"));
        }
    }
    @Test void missingRuntimeDoesNotCertifyAResolutionDespiteMatchingVersions() {
        var before = report(List.of(finding("single-answer", "unique-answer", FindingSeverity.HIGH, "gap1", "Before")));
        var after = report(List.of()); node(after).getMetadata().remove("analysisProvenance");
        assertEquals("PENDING", row(compare(before, after), "unique-answer").status());
    }
    @Test void textChangesDoNotTurnTheSameRuleIntoAFalseResolutionAndNewError() {
        var before = report(List.of(finding("single-answer", "unique-answer", FindingSeverity.HIGH, "gap1", "First wording")));
        var after = report(List.of(finding("single-answer", "unique-answer", FindingSeverity.HIGH, "gap1", "Completely different explanation")));
        var rows = compare(before, after).quizzes().get(0).rows();
        assertEquals(1, rows.size()); assertEquals("PERSISTING", rows.get(0).status());
        assertEquals("First wording", rows.get(0).beforeFindings().get(0).getEvidence().getObservation());
        assertEquals("Completely different explanation", rows.get(0).afterFindings().get(0).getEvidence().getObservation());
    }
    @Test void higherSeverityIsAggravatedAndLowerSeverityIsImprovedWithProblemRemaining() {
        var low = report(List.of(finding("single-answer", "unique-answer", FindingSeverity.LOW, "gap1", "Before")));
        var high = report(List.of(finding("single-answer", "unique-answer", FindingSeverity.BLOCKING, "gap1", "After")));
        assertEquals("AGGRAVATED", row(compare(low, high), "unique-answer").status());
        assertEquals("IMPROVED", row(compare(high, low), "unique-answer").status());
    }
    @Test void multipleSameRuleFindingsRemainGroupedWithEveryEvidenceAndNoInventedPairing() {
        var f1 = finding("single-answer", "unique-answer", FindingSeverity.HIGH, "gap1", "One");
        var f2 = finding("single-answer", "unique-answer", FindingSeverity.HIGH, "gap2", "Two");
        f2.setIdentity(f1.getIdentity()); // Historical data did not distinguish the two findings.
        var before = report(List.of(f1, f2)); var after = report(List.of(finding("single-answer", "unique-answer", FindingSeverity.HIGH, "gap3", "Three")));
        var row = row(compare(before, after), "unique-answer");
        assertEquals("IMPROVED", row.status()); assertEquals(2, row.beforeFindings().size()); assertEquals(1, row.afterFindings().size());
        assertTrue(row.reason().contains("grupo de hallazgos")); assertNotEquals("RESOLVED", row.status());
    }
    @Test void newProblemNeedsCompleteComparableBeforeCoverage() {
        var before = report(List.of()); var after = report(List.of(finding("single-answer", "new-rule", FindingSeverity.HIGH, "gap", "New")));
        assertEquals("NEW", row(compare(before, after), "new-rule").status());
        node(before).setUnevaluatedBy(List.of("single-answer"));
        assertEquals("PENDING", row(compare(before, after), "new-rule").status());
    }
    @Test void pendingControlWithoutAnyFindingsStillAppearsAndCleanControlsDoNot() {
        var before = report(List.of()); var after = report(List.of());
        assertTrue(compare(before, after).quizzes().get(0).rows().isEmpty());
        node(after).setUnevaluatedBy(List.of("single-answer"));
        var row = row(compare(before, after), null);
        assertEquals("single-answer", row.analyzer()); assertEquals("PENDING", row.status());
        assertTrue(row.beforeFindings().isEmpty()); assertTrue(row.afterFindings().isEmpty());
    }
    @Test void missingHistoricalReportKeepsBeforeUnknownAndNeverClaimsNewProblems() {
        var after = report(List.of(finding("single-answer", "unique-answer", FindingSeverity.HIGH, "gap", "After")));
        var comparison = compare(null, after); var row = row(comparison, "unique-answer");
        assertEquals("PENDING", row.status()); assertEquals("UNKNOWN", row.beforeCoverage());
        assertTrue(comparison.warnings().get(0).contains("Falta el análisis"));
    }
    @Test void doesNotIncludeAnotherQuizInTheSelectedOperation() {
        var foreign = finding("single-answer", "foreign-rule", FindingSeverity.HIGH, "gap", "Foreign");
        foreign.getNode().setNodeId(TandaFixture.CHILD);
        var comparison = compare(report(List.of(foreign)), report(List.of(foreign)));
        assertEquals(List.of(quiz), comparison.quizzes().stream().map(QuizEvolution::quizId).toList());
        assertTrue(comparison.quizzes().get(0).rows().isEmpty());
    }

    @Test void unknownRuntimeCannotBorrowStandingOrBecomeComparableByMatchingUnknownStrings() {
        var before = report(List.of(finding("single-answer", "unique-answer", FindingSeverity.HIGH, "gap", "Before")));
        for (Object unknown : Arrays.asList(null, EvaluationRuntimeIdentity.legacyUnknown().toString())) {
            var after = report(List.of());
            var origin = new HashMap<String, Object>(); origin.put("runtime", unknown);
            node(after).getMetadata().put("analysisProvenance", Map.of("single-answer", origin));
            var coverage = new JudgeCoverageDiagnosis(); coverage.setJudgeName("single-answer");
            var standing = new JudgeExamStanding(); standing.setRuntimeIdentity(EvaluationRuntimeIdentity.explicit("codex-cli", "model", "medium"));
            coverage.setStanding(standing);
            ((DefaultCourseDiagnoses) after.getRoot().getDiagnoses()).setJudgeCoverages(List.of(coverage));
            assertEquals("PENDING", row(compare(before, after), "unique-answer").status());
        }
    }
    @Test void mixedKnownAndUnknownSeverityCannotProveTheMaximumSeverityImproved() {
        var before = report(List.of(finding("single-answer", "unique-answer", FindingSeverity.HIGH, "gap", "Before")));
        var one = finding("single-answer", "unique-answer", FindingSeverity.MEDIUM, "gap", "After one");
        var unknown = finding("single-answer", "unique-answer", null, "gap", "After unknown");
        var row = row(compare(before, report(List.of(one, unknown))), "unique-answer");
        assertEquals("PENDING", row.status()); assertEquals(2, row.afterFindings().size());
    }
    @Test void distinctStableFindingIdentitiesProduceResolvedAndNewEvenUnderTheSameRule() {
        var before = report(List.of(finding("hint-coherence", "missing-hint", FindingSeverity.HIGH, "old-location", "Old")));
        var after = report(List.of(finding("hint-coherence", "missing-hint", FindingSeverity.HIGH, "new-location", "New")));
        assertEquals(Set.of("RESOLVED", "NEW"), compare(before, after).quizzes().get(0).rows().stream()
                .map(ProblemEvolution::status).collect(java.util.stream.Collectors.toSet()));
    }
    @Test void taskOriginRetainsAParentFindingOnlyForItsExplicitlyReachedQuiz() {
        var finding = finding("hint-coherence", "context-rule", FindingSeverity.HIGH, "context", "Context");
        finding.setNode(new FindingNodeRef(AuditTarget.KNOWLEDGE, TandaFixture.RESIDE_TOPIC, "Topic"));
        var task = new RefinementTask(); task.setId("task");
        var origin = new TaskOrigin(); origin.setFinding(finding); origin.setReachedQuizIds(List.of(quiz)); task.setOrigin(origin);
        var plans = mock(RefinementPlanStore.class); config.setRefinementPlanStore(plans);
        when(plans.load("plan")).thenReturn(Optional.of(TandaFixture.plan(task)));
        var before = report(List.of(finding)); var after = report(List.of());
        DefaultAnalysisComposer.index(before.getRoot()).get(TandaFixture.RESIDE_TOPIC).getScores().put("hint-coherence", 0.0);
        DefaultAnalysisComposer.index(after.getRoot()).get(TandaFixture.RESIDE_TOPIC).getScores().put("hint-coherence", 1.0);
        assertEquals("RESOLVED", row(compare(before, after), "context-rule").status());
    }
    @Test void measuredCoverageCanSupplyRuntimeOnlyWhenTheMatchingVersionHasOneKnownRuntime() {
        var before = report(List.of(finding("single-answer", "unique-answer", FindingSeverity.HIGH, "gap", "Before")));
        var after = report(List.of()); node(after).getMetadata().remove("analysisProvenance");
        var coverage = new JudgeCoverageDiagnosis(); coverage.setJudgeName("single-answer");
        var actual = EvaluationRuntimeIdentity.explicit("codex-cli", "model", "medium");
        coverage.setVersions(List.of(new JudgeVersionStanding("v1", 1, JudgeExamStatus.APPROVED, actual)));
        ((DefaultCourseDiagnoses) after.getRoot().getDiagnoses()).setJudgeCoverages(List.of(coverage));
        assertEquals("RESOLVED", row(compare(before, after), "unique-answer").status());
        coverage.setVersions(List.of(new JudgeVersionStanding("v1", 0, JudgeExamStatus.APPROVED, actual)));
        assertEquals("PENDING", row(compare(before, after), "unique-answer").status());
        coverage.setVersions(List.of(new JudgeVersionStanding("v1", 1, JudgeExamStatus.APPROVED, actual),
                new JudgeVersionStanding("v1", 1, JudgeExamStatus.APPROVED, EvaluationRuntimeIdentity.explicit("codex-cli", "other", "high"))));
        assertEquals("PENDING", row(compare(before, after), "unique-answer").status());
    }
    @Test void twoMatchingLegacyUnknownRuntimesStillDoNotProveComparableEvaluations() {
        var before = report(List.of(finding("single-answer", "unique-answer", FindingSeverity.HIGH, "gap", "Before")));
        var after = report(List.of());
        provenance(before, "v1", EvaluationRuntimeIdentity.legacyUnknown().toString());
        provenance(after, "v1", EvaluationRuntimeIdentity.legacyUnknown().toString());
        assertEquals("PENDING", row(compare(before, after), "unique-answer").status());
    }
    @Test void changedAnalyzerMeasureIsNotMistakenForImprovementOfTheExercise() {
        var before = report(List.of(finding("hint-coherence", "missing-hint", FindingSeverity.HIGH, "hint", "Before")));
        var after = report(List.of());
        after.getRoot().setMeasures(List.of(new AnalyzerMeasure("hint-coherence", "hint-config-v2", Map.of("threshold", 0.9))));
        assertEquals("PENDING", row(compare(before, after), "missing-hint").status());
    }
    @Test void missingOneOrBothAnalyzerMeasuresCannotCertifyAResolution() {
        var before = report(List.of(finding("hint-coherence", "missing-hint", FindingSeverity.HIGH, "hint", "Before")));
        var after = report(List.of());
        before.getRoot().setMeasures(List.of());
        assertEquals("PENDING", row(compare(before, after), "missing-hint").status());
        after.getRoot().setMeasures(List.of());
        assertEquals("PENDING", row(compare(before, after), "missing-hint").status());
    }
    @Test void parentAndCourseControlsWithoutRelevantFindingsDoNotCreatePendingRowsForEveryQuiz() {
        batch.setAnalyzers(List.of("hint-coherence", "single-answer", "knowledge-title-length", "coca-buckets-distribution"));
        var before = report(List.of()); var after = report(List.of());
        for (var report : List.of(before, after)) {
            DefaultAnalysisComposer.index(report.getRoot()).get(TandaFixture.RESIDE_TOPIC)
                    .getScores().put("knowledge-title-length", 1.0);
            report.getRoot().getScores().put("coca-buckets-distribution", 1.0);
        }
        assertTrue(compare(before, after).quizzes().get(0).rows().isEmpty(),
                "batch-wide analyzers are not evidence that this quiz was awaiting their verdict");
    }
}
