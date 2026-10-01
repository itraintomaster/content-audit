package com.learney.contentaudit.revisiondomain.comparisonengine;

import com.learney.contentaudit.auditdomain.AnalyzerCatalog;
import com.learney.contentaudit.auditdomain.AnalyzerDescriptor;
import com.learney.contentaudit.auditdomain.AuditEngine;
import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditReportStore;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableCourse;
import com.learney.contentaudit.auditdomain.Baseline;
import com.learney.contentaudit.auditdomain.BaselineStore;
import com.learney.contentaudit.auditdomain.EvaluationRunPolicy;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerFamily;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerMeasure;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.contextnumbers.AnalyzerErrorCounts;
import com.learney.contentaudit.auditdomain.contextnumbers.AnalyzerScore;
import com.learney.contentaudit.auditdomain.contextnumbers.ContextNumbers;
import com.learney.contentaudit.auditdomain.finding.EvidencePart;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingNodeRef;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.auditdomain.offlevelwords.TaughtWords;
import com.learney.contentaudit.auditdomain.offlevelwords.TopicTeachingResolver;
import com.learney.contentaudit.revisiondomain.blindsample.SampleOutcome;
import com.learney.contentaudit.revisiondomain.blindsample.SampleOutcomeSource;
import com.learney.contentaudit.revisiondomain.comparison.BatchComparator;
import com.learney.contentaudit.revisiondomain.comparison.BatchComparison;
import com.learney.contentaudit.revisiondomain.comparison.BatchVerdict;
import com.learney.contentaudit.revisiondomain.comparison.ChangeOutcome;
import com.learney.contentaudit.revisiondomain.comparison.ComparisonRow;
import com.learney.contentaudit.revisiondomain.comparison.EffectiveGoals;
import com.learney.contentaudit.revisiondomain.comparison.FloorBreach;
import com.learney.contentaudit.revisiondomain.comparison.Goal;
import com.learney.contentaudit.revisiondomain.comparison.GoalBook;
import com.learney.contentaudit.revisiondomain.comparison.GoalKind;
import com.learney.contentaudit.revisiondomain.comparison.GoalReference;
import com.learney.contentaudit.revisiondomain.comparison.GoalStatus;
import com.learney.contentaudit.revisiondomain.comparison.GoalUnit;
import com.learney.contentaudit.revisiondomain.comparison.MeasureChange;
import com.learney.contentaudit.revisiondomain.comparison.RowGoalStatus;
import com.learney.contentaudit.revisiondomain.comparison.RowValue;
import com.learney.contentaudit.revisiondomain.comparison.SealedGoals;
import com.learney.contentaudit.revisiondomain.comparison.VerdictKind;
import com.learney.contentaudit.revisiondomain.consolidatedview.ConsolidatedViewAvailability;
import com.learney.contentaudit.revisiondomain.consolidatedview.ConsolidatedViewUnavailability;
import com.learney.contentaudit.revisiondomain.consolidatedview.ConsolidatedViewUnavailabilityReason;
import com.learney.contentaudit.revisiondomain.projection.CourseProjection;
import com.learney.contentaudit.revisiondomain.projection.CourseProjector;
import com.learney.contentaudit.revisiondomain.projection.ProjectedChange;
import com.learney.contentaudit.revisiondomain.projection.ProjectionScope;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.Generated;

/**
 * The before and after of a batch against the baseline (F-METAS-R001 to R009), put together without computing a
 * number again: the projection gives the batch start (the active analysis with what is approved) and the batch
 * applied; the baseline is the analysis José fixed, read from its own record. Every knowledge, topic, level and the
 * course gets a row per analyzer that ran -- plus «vocabulario» and «algún error» -- with the three published
 * numbers, the change against the baseline and the status of its floor and of its progress goal. A changed node
 * that gains a finding that counts returns; a broken floor of a level or of the course names the changes that
 * break it; a measure that changed since the baseline is shown apart and the batch is counted from the baseline
 * measured again with it. The verdict reads the floors, what returns and the blind sample, never a progress goal.
 * Comparing writes nothing.
 */
@Generated(
        value = "com.sentinel.SentinelEngine",
        comments = "Generated by Sentinel"
)
public class DefaultBatchComparator implements BatchComparator {
    /** The length is declared and counts only in the floor of the level: it never returns a change (José, 11/8). */
    static final Set<String> LENGTH_ANALYZERS = Set.of("sentence-length", "knowledge-title-length",
            "knowledge-instructions-length");

    /** The part of the evidence of a vocabulary finding that names its word. */
    static final String WORD_PART = "Palabra";

    private static final double EPSILON = 1e-9;

    private static final int LISTED_IDS = 10;

    private final CourseProjector courseProjector;

    private final BaselineStore baselineStore;

    private final AuditReportStore auditReportStore;

    private final GoalBook goalBook;

    private final AnalyzerCatalog analyzerCatalog;

    private final AuditEngine auditEngine;

    private final ChangeAttributor changeAttributor;

    private final SampleOutcomeSource sampleOutcomeSource;

    private final TopicTeachingResolver topicTeachingResolver;

    public DefaultBatchComparator(CourseProjector courseProjector, BaselineStore baselineStore,
            AuditReportStore auditReportStore, GoalBook goalBook, AnalyzerCatalog analyzerCatalog,
            AuditEngine auditEngine, ChangeAttributor changeAttributor,
            SampleOutcomeSource sampleOutcomeSource, TopicTeachingResolver topicTeachingResolver) {
        this.courseProjector = courseProjector;
        this.baselineStore = baselineStore;
        this.auditReportStore = auditReportStore;
        this.goalBook = goalBook;
        this.analyzerCatalog = analyzerCatalog;
        this.auditEngine = auditEngine;
        this.changeAttributor = changeAttributor;
        this.sampleOutcomeSource = sampleOutcomeSource;
        this.topicTeachingResolver = topicTeachingResolver;
    }

    @Override
    public BatchComparison compare(Path coursePath, ProjectionScope scope) {
        EffectiveGoals goals = goalBook.effective();
        Optional<SealedGoals> sealed = goalBook.sealed();
        BatchComparison comparison = new BatchComparison();
        comparison.setComputedAt(Instant.now());
        comparison.setGoals(goals);
        comparison.setGoalsSealedAt(sealed.map(SealedGoals::getSealedAt).orElse(null));
        comparison.setGoalChanges(GoalChanges.between(sealed.map(SealedGoals::getGoals).orElse(null), goals));
        comparison.setMeasureChanges(new ArrayList<>());
        comparison.setAppliedProposalIds(new ArrayList<>());
        comparison.setNonApplicablePendings(new ArrayList<>());
        comparison.setRows(new ArrayList<>());
        comparison.setChanges(new ArrayList<>());
        comparison.setBreaches(new ArrayList<>());

        Baseline baseline = baselineStore.read().orElse(null);
        if (baseline == null || baseline.getAuditId() == null || baseline.getAuditId().isBlank()) {
            return unavailable(comparison, ConsolidatedViewUnavailabilityReason.OTHER,
                    "No hay línea base fijada: fijala con 'set-baseline <id>' antes de comparar.");
        }
        comparison.setBaselineAuditId(baseline.getAuditId());
        AuditReport baselineReport = auditReportStore.load(baseline.getAuditId()).orElse(null);
        if (baselineReport == null || baselineReport.getRoot() == null) {
            return unavailable(comparison, ConsolidatedViewUnavailabilityReason.OTHER,
                    "La línea base '" + baseline.getAuditId() + "' no está en el historial de análisis.");
        }

        CourseProjection projection = courseProjector.project(coursePath, scope);
        comparison.setActiveAuditId(projection.getActiveAuditId());
        comparison.setActivePlanId(projection.getActivePlanId());
        comparison.setScopeId(projection.getScopeId());
        if (projection.getNonApplicablePendings() != null) {
            comparison.setNonApplicablePendings(new ArrayList<>(projection.getNonApplicablePendings()));
        }
        if (projection.getAvailability() != ConsolidatedViewAvailability.AVAILABLE
                || projection.getApproved() == null || projection.getProjected() == null) {
            ConsolidatedViewUnavailability why = projection.getUnavailability() != null
                    ? projection.getUnavailability()
                    : new ConsolidatedViewUnavailability(ConsolidatedViewUnavailabilityReason.REAGGREGATION_FAILED,
                            "La proyección no está disponible.");
            return unavailable(comparison, why.getReason(), why.getDetail());
        }
        comparison.setAvailability(ConsolidatedViewAvailability.AVAILABLE);
        List<ProjectedChange> batch = pendingChanges(projection);
        List<String> applied = new ArrayList<>();
        for (ProjectedChange change : batch) {
            applied.add(change.getProposalId());
        }
        comparison.setAppliedProposalIds(applied);

        Map<String, AnalyzerFamily> subjects = subjects(baselineReport, projection.getProjected());
        Map<String, AuditNode> baselineIndex = ReportTree.index(baselineReport);
        Map<String, AuditNode> startIndex = ReportTree.index(projection.getApproved());
        Map<String, AuditNode> projectedIndex = ReportTree.index(projection.getProjected());

        // R007: a measure that is not the one of the baseline is shown apart, and the batch is counted from the
        // baseline measured again with it -- the same course of its report, one run.
        List<MeasureChange> measureChanges = measureChanges(baselineReport, projection.getProjected());
        comparison.setMeasureChanges(measureChanges);
        Set<String> remeasuredSubjects = new HashSet<>();
        Map<String, AuditNode> remeasuredIndex = null;
        if (!measureChanges.isEmpty()) {
            for (MeasureChange change : measureChanges) {
                remeasuredSubjects.add(change.getAnalyzer());
                AnalyzerFamily family = familyOf(change.getAnalyzer(), subjects, baselineReport);
                remeasuredSubjects.add(family == AnalyzerFamily.ERRORS ? DefaultGoalBook.ANY_ERROR
                        : DefaultGoalBook.VOCABULARY);
            }
            AuditReport remeasured = remeasure(baselineReport, scope);
            remeasuredIndex = remeasured != null ? ReportTree.index(remeasured) : null;
        }

        // R001, R002: the rows, with their goals.
        Map<String, Map<String, Integer>> changedUnevaluated = changedUnevaluated(batch, projectedIndex, subjects);
        List<Goal> goalList = goals != null && goals.getGoals() != null ? goals.getGoals() : List.of();
        List<ComparisonRow> rows = new ArrayList<>();
        for (AuditNode node : projectedIndex.values()) {
            if (node.getTarget() == null || node.getTarget() == AuditTarget.QUIZ) {
                continue;
            }
            String id = ReportTree.idOf(node);
            String key = ReportTree.key(node.getTarget(), id);
            for (Map.Entry<String, AnalyzerFamily> subject : subjects.entrySet()) {
                ComparisonRow row = row(subject.getKey(), subject.getValue(), node, id, baselineIndex.get(key),
                        startIndex.get(key), remeasuredSubjects.contains(subject.getKey()), remeasuredIndex, key,
                        changedUnevaluated.getOrDefault(key, Map.of()).getOrDefault(subject.getKey(), 0), goalList);
                if (row != null) {
                    rows.add(row);
                }
            }
        }
        comparison.setRows(rows);

        // R003: what returns.
        List<ChangeOutcome> outcomes = outcomes(batch, startIndex, projectedIndex, projection, subjects);
        comparison.setChanges(outcomes);

        // R004, R005: the broken floors, and who breaks a level or the course.
        List<FloorBreach> breaches = new ArrayList<>();
        List<FloorBreach> toAttribute = new ArrayList<>();
        for (ComparisonRow row : rows) {
            if (row.getFloor() != null && row.getFloor().getStatus() == GoalStatus.NOT_MET) {
                FloorBreach breach = new FloorBreach(row.getFloor().getGoalId(), row, new ArrayList<>());
                breaches.add(breach);
                if (row.getNodeTarget() == AuditTarget.MILESTONE || row.getNodeTarget() == AuditTarget.COURSE) {
                    toAttribute.add(breach);
                }
            }
        }
        if (!toAttribute.isEmpty() && !batch.isEmpty() && changeAttributor != null) {
            List<FloorBreach> attributed = changeAttributor.attribute(projection, toAttribute);
            if (attributed != null) {
                for (int i = 0; i < toAttribute.size() && i < attributed.size(); i++) {
                    if (attributed.get(i) != null && attributed.get(i).getAttributions() != null) {
                        toAttribute.get(i).setAttributions(new ArrayList<>(attributed.get(i).getAttributions()));
                    }
                }
            }
        }
        comparison.setBreaches(breaches);

        // R008: the verdict.
        comparison.setVerdict(verdict(rows, breaches, outcomes, projection.getScopeId()));
        return comparison;
    }

    // -------------------------------------------------------------------------
    // Unavailable
    // -------------------------------------------------------------------------

    private static BatchComparison unavailable(BatchComparison comparison, ConsolidatedViewUnavailabilityReason reason,
            String detail) {
        comparison.setAvailability(ConsolidatedViewAvailability.UNAVAILABLE);
        comparison.setUnavailability(new ConsolidatedViewUnavailability(reason, detail));
        comparison.setVerdict(new BatchVerdict(VerdictKind.UNDECIDABLE, new ArrayList<>(), new ArrayList<>(), null,
                "No se puede decidir: " + detail));
        return comparison;
    }

    // -------------------------------------------------------------------------
    // Subjects
    // -------------------------------------------------------------------------

    /**
     * Every analyzer that ran in the baseline or in the projection, in the order of the catalog, with its family;
     * then «vocabulario» and «algún error». An analyzer that ran only in one of them still has its rows: the
     * other side is empty, never zero.
     */
    private Map<String, AnalyzerFamily> subjects(AuditReport baseline, AuditReport projected) {
        Set<String> ran = new LinkedHashSet<>(ranIn(baseline));
        ran.addAll(ranIn(projected));
        Map<String, AnalyzerFamily> subjects = new LinkedHashMap<>();
        for (AnalyzerDescriptor card : analyzerCatalog.list()) {
            if (card != null && card.getName() != null && card.getFamily() != null && ran.contains(card.getName())) {
                subjects.put(card.getName(), card.getFamily());
            }
        }
        for (String name : ran) {
            if (subjects.containsKey(name)) {
                continue;
            }
            AnalyzerFamily family = ReportTree.publishedFamily(projected, name);
            if (family == null) {
                family = ReportTree.publishedFamily(baseline, name);
            }
            if (family != null) {
                subjects.put(name, family);
            }
        }
        subjects.put(DefaultGoalBook.VOCABULARY, AnalyzerFamily.VOCABULARY);
        subjects.put(DefaultGoalBook.ANY_ERROR, AnalyzerFamily.ERRORS);
        return subjects;
    }

    /** The analyzers that ran in a report: its measures or, for a report without them, its published numbers. */
    private static List<String> ranIn(AuditReport report) {
        List<String> names = ReportTree.measuredAnalyzers(report);
        if (!names.isEmpty() || report == null || report.getRoot() == null) {
            return names;
        }
        ContextNumbers numbers = report.getRoot().getNumbers();
        if (numbers != null && numbers.getAnalyzerScores() != null) {
            for (AnalyzerScore score : numbers.getAnalyzerScores()) {
                if (score.getAnalyzer() != null && !names.contains(score.getAnalyzer())) {
                    names.add(score.getAnalyzer());
                }
            }
        }
        if (numbers != null && numbers.getErrors() != null && numbers.getErrors().getAnalyzers() != null) {
            for (AnalyzerErrorCounts counts : numbers.getErrors().getAnalyzers()) {
                if (counts.getAnalyzer() != null && !names.contains(counts.getAnalyzer())) {
                    names.add(counts.getAnalyzer());
                }
            }
        }
        return names;
    }

    private AnalyzerFamily familyOf(String analyzer, Map<String, AnalyzerFamily> subjects, AuditReport report) {
        if (analyzer == null) {
            return null;
        }
        AnalyzerFamily family = subjects.get(analyzer);
        if (family != null) {
            return family;
        }
        Optional<AnalyzerDescriptor> card = analyzerCatalog.find(analyzer);
        if (card != null && card.isPresent() && card.get().getFamily() != null) {
            return card.get().getFamily();
        }
        return ReportTree.publishedFamily(report, analyzer);
    }

    // -------------------------------------------------------------------------
    // Measure changes (R007)
    // -------------------------------------------------------------------------

    /**
     * The measures of the baseline that are not the ones of the projection: the fingerprint at the root of each
     * report -- or the catalog's today, for a projection that keeps none. A baseline without measures was saved
     * before the contract and is compared as it is.
     */
    private List<MeasureChange> measureChanges(AuditReport baseline, AuditReport projected) {
        List<MeasureChange> changes = new ArrayList<>();
        Map<String, AnalyzerMeasure> before = ReportTree.measures(baseline);
        Map<String, AnalyzerMeasure> now = ReportTree.measures(projected);
        for (Map.Entry<String, AnalyzerMeasure> entry : before.entrySet()) {
            AnalyzerMeasure current = now.isEmpty()
                    ? analyzerCatalog.measure(entry.getKey()).orElse(null)
                    : now.get(entry.getKey());
            if (current == null) {
                continue;
            }
            AnalyzerMeasure old = entry.getValue();
            if (!Objects.equals(old.getFingerprint(), current.getFingerprint())) {
                changes.add(new MeasureChange(entry.getKey(), old.getFingerprint(), current.getFingerprint(),
                        old.getConfig(), current.getConfig()));
            }
        }
        return changes;
    }

    /**
     * The baseline measured again with today's measures, on the course of its own report -- the same exercises,
     * the same tokens -- with the analyzers it ran. Null when its report cannot be rebuilt or measured.
     */
    private AuditReport remeasure(AuditReport baseline, ProjectionScope scope) {
        AuditableCourse course = ReportTree.course(baseline, Map.of());
        if (course == null) {
            return null;
        }
        List<String> analyzers = ReportTree.measuredAnalyzers(baseline);
        Map<String, EvaluationRunPolicy> policies = new LinkedHashMap<>();
        Map<String, Integer> budgets = scope != null ? scope.getAnalyzerBudgets() : null;
        if (budgets != null) {
            for (String analyzer : analyzers) {
                Integer budget = budgets.get(analyzer);
                if (budget != null) {
                    policies.put(analyzer, new EvaluationRunPolicy(budget, false, null, null, null, null));
                }
            }
        }
        try {
            return auditEngine.runAudit(course, new AnalyzerRunSelection(analyzers, policies));
        } catch (RuntimeException e) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Rows (R001, R002)
    // -------------------------------------------------------------------------

    private ComparisonRow row(String subject, AnalyzerFamily family, AuditNode node, String id, AuditNode baselineNode,
            AuditNode startNode, boolean measureChanged, Map<String, AuditNode> remeasuredIndex, String key,
            int changedUnevaluated, List<Goal> goals) {
        ReportTree.Value baseline = ReportTree.value(baselineNode, subject, family);
        ReportTree.Value start = ReportTree.value(startNode, subject, family);
        ReportTree.Value projected = ReportTree.value(node, subject, family);
        if (baseline == null && start == null && projected == null) {
            return null;
        }
        ReportTree.Value remeasured = measureChanged && remeasuredIndex != null
                ? ReportTree.value(remeasuredIndex.get(key), subject, family) : null;
        Values values = new Values(family, baseline, remeasured, start, projected, measureChanged,
                changedUnevaluated);
        GoalUnit unit = family == AnalyzerFamily.VOCABULARY ? GoalUnit.POINTS : GoalUnit.EXERCISES;
        ReportTree.Value reference = values.reference();
        Double change = reference != null && reference.value() != null && projected != null
                && projected.value() != null ? delta(projected.value(), reference.value(), unit) : null;
        ComparisonRow row = new ComparisonRow(subject, family, node.getTarget(), id, ReportTree.labelOf(node),
                rowValue(baseline), rowValue(remeasured), rowValue(start), rowValue(projected), change, unit, null,
                null);
        Goal floor = goalFor(goals, GoalKind.FLOOR, subject, node.getTarget(), id);
        if (floor != null) {
            row.setFloor(new RowGoalStatus(floor.getId(), floor.getThreshold(), floor.getUnit(), floor.getReference(),
                    status(floor, values)));
        }
        Goal progress = goalFor(goals, GoalKind.PROGRESS, subject, node.getTarget(), id);
        if (progress != null) {
            row.setProgress(new RowGoalStatus(progress.getId(), progress.getThreshold(), progress.getUnit(),
                    progress.getReference(), status(progress, values)));
        }
        return row;
    }

    /** The numbers a goal is decided on: the three photos, the baseline measured again and what stays open. */
    record Values(AnalyzerFamily family, ReportTree.Value baseline, ReportTree.Value remeasured,
            ReportTree.Value start, ReportTree.Value projected, boolean measureChanged, int changedUnevaluated) {
        /** The baseline the batch is counted from: measured again when its measure changed. */
        ReportTree.Value reference() {
            return measureChanged ? remeasured : baseline;
        }
    }

    private static RowValue rowValue(ReportTree.Value value) {
        return value == null ? null : new RowValue(value.value(), value.share(), value.notEvaluated());
    }

    /** The change in the unit of the row: points of a score, exercises of a count. */
    private static double delta(double now, double before, GoalUnit unit) {
        return unit == GoalUnit.POINTS ? (now - before) * 100.0 : now - before;
    }

    /** The goal of a row: one for its node over one for every node of its type, in the order of the goals. */
    private static Goal goalFor(List<Goal> goals, GoalKind kind, String subject, AuditTarget target, String id) {
        Goal general = null;
        for (Goal goal : goals) {
            if (goal == null || goal.getKind() != kind || !subject.equals(goal.getSubject())
                    || goal.getNodeTarget() != target) {
                continue;
            }
            if (goal.getNodeId() != null) {
                if (goal.getNodeId().equals(id)) {
                    return goal;
                }
            } else if (general == null) {
                general = goal;
            }
        }
        return general;
    }

    /**
     * R002: met, not met, or undecidable when exercises left unevaluated could change it. A score must not drop
     * more than the threshold in points against its reference; a count must not grow more than the threshold:
     * if the evaluated exercises already pass it the goal is not met, if counting every open exercise as an
     * error it still holds it is met, and otherwise it cannot be decided. Against the batch start the open
     * exercises are the ones the batch changed and an analyzer left unevaluated -- an exercise the batch did
     * not touch cannot gain an error --; against the baseline or as a value, every one left unevaluated.
     */
    static GoalStatus status(Goal goal, Values values) {
        ReportTree.Value projected = values.projected();
        if (projected == null || projected.value() == null) {
            return GoalStatus.UNDECIDABLE;
        }
        GoalReference reference = goal.getReference() != null ? goal.getReference() : GoalReference.ABSOLUTE;
        ReportTree.Value from;
        switch (reference) {
            case BASELINE -> {
                if (values.measureChanged() && values.remeasured() == null) {
                    return GoalStatus.UNDECIDABLE;
                }
                from = values.reference();
                if (from == null || from.value() == null) {
                    return GoalStatus.UNDECIDABLE;
                }
            }
            case BATCH_START -> {
                from = values.start();
                if (from == null || from.value() == null) {
                    return GoalStatus.UNDECIDABLE;
                }
            }
            default -> from = null;
        }
        double threshold = goal.getThreshold();
        GoalUnit unit = goal.getUnit() != null ? goal.getUnit()
                : values.family() == AnalyzerFamily.VOCABULARY ? GoalUnit.POINTS : GoalUnit.EXERCISES;
        int open = reference == GoalReference.BATCH_START ? values.changedUnevaluated() : projected.notEvaluated();
        switch (unit) {
            case POINTS -> {
                if (from == null) {
                    return projected.value() * 100.0 >= threshold - EPSILON ? GoalStatus.MET : GoalStatus.NOT_MET;
                }
                double drop = (projected.value() - from.value()) * 100.0;
                return drop >= -threshold - EPSILON ? GoalStatus.MET : GoalStatus.NOT_MET;
            }
            case PERCENT -> {
                int total = projected.total();
                if (total <= 0) {
                    return projected.value() <= EPSILON ? GoalStatus.MET : GoalStatus.UNDECIDABLE;
                }
                double share = projected.value() / total * 100.0;
                double worst = (projected.value() + open) / total * 100.0;
                double base = 0.0;
                if (from != null) {
                    base = from.share() != null ? from.share() * 100.0
                            : from.total() > 0 ? from.value() / from.total() * 100.0 : 0.0;
                }
                if (share - base > threshold + EPSILON) {
                    return GoalStatus.NOT_MET;
                }
                return worst - base <= threshold + EPSILON ? GoalStatus.MET : GoalStatus.UNDECIDABLE;
            }
            default -> {
                double growth = projected.value() - (from != null ? from.value() : 0.0);
                if (growth > threshold + EPSILON) {
                    return GoalStatus.NOT_MET;
                }
                return growth + open <= threshold + EPSILON ? GoalStatus.MET : GoalStatus.UNDECIDABLE;
            }
        }
    }

    /**
     * How many exercises the batch changed and an analyzer left unevaluated in the projection, per context node
     * and analyzer -- and per «algún error», when any analyzer of the errors family left it open.
     */
    private Map<String, Map<String, Integer>> changedUnevaluated(List<ProjectedChange> batch,
            Map<String, AuditNode> projectedIndex, Map<String, AnalyzerFamily> subjects) {
        Map<String, Map<String, Integer>> counts = new HashMap<>();
        Set<String> seen = new HashSet<>();
        for (ProjectedChange change : batch) {
            AuditNode node = projectedIndex.get(ReportTree.key(change.getNodeTarget(), change.getNodeId()));
            if (node == null) {
                continue;
            }
            for (AuditNode quiz : quizzesUnder(node)) {
                if (!seen.add(ReportTree.idOf(quiz))) {
                    continue;
                }
                Set<String> open = new LinkedHashSet<>(listOrEmpty(quiz.getUnevaluatedBy()));
                quiz.ancestor(AuditTarget.KNOWLEDGE).ifPresent(k -> open.addAll(listOrEmpty(k.getUnevaluatedBy())));
                if (open.isEmpty()) {
                    continue;
                }
                boolean anyError = false;
                for (String analyzer : open) {
                    if (familyOf(analyzer, subjects, null) == AnalyzerFamily.ERRORS) {
                        anyError = true;
                    }
                }
                for (AuditNode up = quiz.getParent(); up != null; up = up.getParent()) {
                    Map<String, Integer> byAnalyzer = counts.computeIfAbsent(
                            ReportTree.key(up.getTarget(), ReportTree.idOf(up)), k -> new HashMap<>());
                    for (String analyzer : open) {
                        byAnalyzer.merge(analyzer, 1, Integer::sum);
                    }
                    if (anyError) {
                        byAnalyzer.merge(DefaultGoalBook.ANY_ERROR, 1, Integer::sum);
                    }
                }
            }
        }
        return counts;
    }

    // -------------------------------------------------------------------------
    // What returns (R003)
    // -------------------------------------------------------------------------

    /**
     * Per change of the batch: the findings of its node -- and of what hangs from it -- at the batch start and in
     * the projection, matched by identity. It returns if it gained one that counts, or if another node gained one
     * that counts and names it as related; what disappeared is fixed; an analyzer that left it unevaluated in the
     * projection leaves it undecided for that analyzer. The new findings it lists are the ones that make it
     * return: its reasons.
     */
    private List<ChangeOutcome> outcomes(List<ProjectedChange> batch, Map<String, AuditNode> startIndex,
            Map<String, AuditNode> projectedIndex, CourseProjection projection, Map<String, AnalyzerFamily> subjects) {
        List<ChangeOutcome> outcomes = new ArrayList<>();
        if (batch.isEmpty()) {
            return outcomes;
        }
        Set<String> startKeys = new HashSet<>();
        for (Finding finding : findingsUnder(projection.getApproved().getRoot())) {
            startKeys.add(identity(finding));
        }
        Map<String, List<Finding>> gainedNaming = new HashMap<>();
        for (Finding finding : findingsUnder(projection.getProjected().getRoot())) {
            if (finding.getEvidence() == null || finding.getEvidence().getRelatedNodes() == null
                    || startKeys.contains(identity(finding))) {
                continue;
            }
            for (FindingNodeRef related : finding.getEvidence().getRelatedNodes()) {
                if (related != null && related.getNodeId() != null) {
                    gainedNaming.computeIfAbsent(related.getNodeId(), k -> new ArrayList<>()).add(finding);
                }
            }
        }
        Map<String, TaughtWords> taught = new HashMap<>();
        for (ProjectedChange change : batch) {
            String key = ReportTree.key(change.getNodeTarget(), change.getNodeId());
            AuditNode before = startIndex.get(key);
            AuditNode after = projectedIndex.get(key);
            List<Finding> beforeFindings = findingsUnder(before);
            List<Finding> afterFindings = findingsUnder(after);
            Set<String> beforeKeys = new HashSet<>();
            for (Finding finding : beforeFindings) {
                beforeKeys.add(identity(finding));
            }
            Set<String> afterKeys = new HashSet<>();
            for (Finding finding : afterFindings) {
                afterKeys.add(identity(finding));
            }
            List<Finding> gained = new ArrayList<>();
            Set<String> gainedKeys = new HashSet<>();
            for (Finding finding : afterFindings) {
                if (!beforeKeys.contains(identity(finding)) && counts(finding, projectedIndex, subjects, taught)
                        && gainedKeys.add(identity(finding))) {
                    gained.add(finding);
                }
            }
            for (String id : idsUnder(after, change.getNodeId())) {
                for (Finding finding : gainedNaming.getOrDefault(id, List.of())) {
                    if (counts(finding, projectedIndex, subjects, taught) && gainedKeys.add(identity(finding))) {
                        gained.add(finding);
                    }
                }
            }
            List<Finding> fixed = new ArrayList<>();
            for (Finding finding : beforeFindings) {
                if (!afterKeys.contains(identity(finding))) {
                    fixed.add(finding);
                }
            }
            Set<String> undecided = new LinkedHashSet<>();
            for (AuditNode node : nodesUnder(after)) {
                undecided.addAll(listOrEmpty(node.getUnevaluatedBy()));
            }
            outcomes.add(new ChangeOutcome(change.getProposalId(), change.getNodeTarget(), change.getNodeId(),
                    !gained.isEmpty(), gained, fixed, new ArrayList<>(undecided)));
        }
        return outcomes;
    }

    /**
     * A finding counts if it is of the errors family and does more than order, or of the vocabulary family with
     * medium severity or more -- a word of another level or out of the catalog -- that is not the length and not
     * a word the topic teaches (F-SEMAF-R004, through the same resolver as the table of E).
     */
    private boolean counts(Finding finding, Map<String, AuditNode> projectedIndex, Map<String, AnalyzerFamily> subjects,
            Map<String, TaughtWords> taught) {
        AnalyzerFamily family = familyOf(finding.getAnalyzer(), subjects, null);
        if (family == AnalyzerFamily.ERRORS) {
            return finding.getResolution() != FindingResolution.RANK_ONLY;
        }
        if (family != AnalyzerFamily.VOCABULARY) {
            return false;
        }
        if (LENGTH_ANALYZERS.contains(finding.getAnalyzer())) {
            return false;
        }
        if (finding.getSeverity() == null || finding.getSeverity() == FindingSeverity.LOW) {
            return false;
        }
        String word = wordOf(finding);
        return word == null || !taughtByItsTopic(finding, word, projectedIndex, taught);
    }

    /** The word a vocabulary finding marks: the part «Palabra» of its evidence, or its marker. */
    static String wordOf(Finding finding) {
        if (finding.getEvidence() != null && finding.getEvidence().getExamined() != null) {
            for (EvidencePart part : finding.getEvidence().getExamined()) {
                if (part != null && WORD_PART.equals(part.getLabel()) && part.getText() != null
                        && !part.getText().isBlank()) {
                    return part.getText().trim();
                }
            }
        }
        String marker = finding.getIdentity() != null ? finding.getIdentity().getMarker() : null;
        if (marker == null || marker.isBlank()) {
            return null;
        }
        int bar = marker.indexOf('|');
        return bar > 0 ? marker.substring(0, bar) : marker;
    }

    private boolean taughtByItsTopic(Finding finding, String word, Map<String, AuditNode> projectedIndex,
            Map<String, TaughtWords> taught) {
        if (topicTeachingResolver == null || finding.getNode() == null) {
            return false;
        }
        AuditNode node = projectedIndex.get(ReportTree.key(finding.getNode().getTarget(), finding.getNode().getNodeId()));
        if (node == null) {
            return false;
        }
        AuditNode knowledge = node.getTarget() == AuditTarget.KNOWLEDGE ? node
                : node.ancestor(AuditTarget.KNOWLEDGE).orElse(null);
        if (knowledge == null) {
            return false;
        }
        String id = ReportTree.idOf(knowledge);
        TaughtWords words = id != null
                ? taught.computeIfAbsent(id, k -> topicTeachingResolver.taughtWords(knowledge))
                : topicTeachingResolver.taughtWords(knowledge);
        if (words == null) {
            return false;
        }
        String lower = word.toLowerCase(Locale.ROOT);
        for (String term : listOrEmpty(words.getTitleTerms())) {
            if (term != null && term.toLowerCase(Locale.ROOT).equals(lower)) {
                return true;
            }
        }
        for (String answer : listOrEmpty(words.getFrequentAnswers())) {
            if (answer != null && answer.toLowerCase(Locale.ROOT).equals(lower)) {
                return true;
            }
        }
        return false;
    }

    /** F-HALL-R014: analyzer, rule, node and what it marks. */
    static String identity(Finding finding) {
        if (finding.getIdentity() != null && finding.getIdentity().getKey() != null) {
            return finding.getIdentity().getKey();
        }
        String nodeId = finding.getNode() != null ? finding.getNode().getNodeId() : null;
        String marker = finding.getIdentity() != null ? finding.getIdentity().getMarker() : null;
        return finding.getAnalyzer() + "|" + finding.getRule() + "|" + nodeId + (marker != null ? "|" + marker : "");
    }

    // -------------------------------------------------------------------------
    // Verdict (R008)
    // -------------------------------------------------------------------------

    private BatchVerdict verdict(List<ComparisonRow> rows, List<FloorBreach> breaches, List<ChangeOutcome> outcomes,
            String scopeId) {
        List<String> broken = new ArrayList<>();
        for (FloorBreach breach : breaches) {
            if (breach.getGoalId() != null && !broken.contains(breach.getGoalId())) {
                broken.add(breach.getGoalId());
            }
        }
        List<String> returning = new ArrayList<>();
        List<String> undecidedChanges = new ArrayList<>();
        for (ChangeOutcome outcome : outcomes) {
            if (outcome.isReturns()) {
                returning.add(outcome.getProposalId());
            } else if (outcome.getUndecidedAnalyzers() != null && !outcome.getUndecidedAnalyzers().isEmpty()) {
                undecidedChanges.add(outcome.getProposalId());
            }
        }
        List<String> undecidableFloors = new ArrayList<>();
        for (ComparisonRow row : rows) {
            if (row.getFloor() != null && row.getFloor().getStatus() == GoalStatus.UNDECIDABLE
                    && !undecidableFloors.contains(row.getFloor().getGoalId())) {
                undecidableFloors.add(row.getFloor().getGoalId());
            }
        }
        SampleOutcome sample = sampleOutcomeSource != null ? sampleOutcomeSource.outcomeFor(scopeId).orElse(null) : null;
        boolean sampleFails = sample != null && sample.isClosed() && !sample.isMeetsGoal();

        if (!broken.isEmpty() || !returning.isEmpty() || sampleFails) {
            List<String> parts = new ArrayList<>();
            if (!returning.isEmpty()) {
                parts.add((returning.size() == 1 ? "vuelve 1 cambio: " : "vuelven " + returning.size() + " cambios: ")
                        + listed(returning));
            }
            if (!broken.isEmpty()) {
                parts.add((broken.size() == 1 ? "1 piso roto: " : broken.size() + " pisos rotos: ") + listed(broken));
            }
            if (sampleFails) {
                parts.add("la muestra ciega " + sample.getSampleId() + " no cumple su meta"
                        + (sample.getDetail() != null && !sample.getDetail().isBlank() ? " (" + sample.getDetail() + ")"
                                : ""));
            }
            return new BatchVerdict(VerdictKind.FAILS, broken, returning, sample, "No pasa: " + String.join("; ", parts));
        }
        if (!undecidableFloors.isEmpty() || !undecidedChanges.isEmpty()) {
            List<String> parts = new ArrayList<>();
            if (!undecidableFloors.isEmpty()) {
                parts.add((undecidableFloors.size() == 1 ? "1 piso sin decidir: "
                        : undecidableFloors.size() + " pisos sin decidir: ") + listed(undecidableFloors));
            }
            if (!undecidedChanges.isEmpty()) {
                parts.add((undecidedChanges.size() == 1 ? "1 cambio" : undecidedChanges.size() + " cambios")
                        + " sin evaluar por algún analizador: " + listed(undecidedChanges));
            }
            return new BatchVerdict(VerdictKind.UNDECIDABLE, broken, returning, sample,
                    "No se puede decidir: ningún piso falla, pero " + String.join("; ", parts));
        }
        if (sample == null) {
            return new BatchVerdict(VerdictKind.BLIND_SAMPLE_MISSING, broken, returning, null,
                    "Falta la muestra ciega: los pisos se cumplen y no vuelve ningún cambio, pero no hay muestra de '"
                            + scopeId + "'.");
        }
        if (!sample.isClosed()) {
            return new BatchVerdict(VerdictKind.BLIND_SAMPLE_MISSING, broken, returning, sample,
                    "Falta la muestra ciega: los pisos se cumplen y no vuelve ningún cambio, pero la muestra "
                            + sample.getSampleId() + " sigue abierta (" + sample.getAnswered() + " pares respondidos).");
        }
        return new BatchVerdict(VerdictKind.PASSES, broken, returning, sample,
                "Pasa: se cumplen los pisos, no vuelve ningún cambio y la muestra ciega " + sample.getSampleId()
                        + " cumple su meta.");
    }

    private static String listed(List<String> ids) {
        if (ids.size() <= LISTED_IDS) {
            return String.join(", ", ids);
        }
        return String.join(", ", ids.subList(0, LISTED_IDS)) + " y " + (ids.size() - LISTED_IDS) + " más";
    }

    // -------------------------------------------------------------------------
    // Trees
    // -------------------------------------------------------------------------

    /** The pending changes the projection applied: the batch, in the order they were applied. */
    static List<ProjectedChange> pendingChanges(CourseProjection projection) {
        List<ProjectedChange> pending = new ArrayList<>();
        if (projection.getChanges() != null) {
            for (ProjectedChange change : projection.getChanges()) {
                if (change != null && change.isPending() && change.getNodeTarget() != null
                        && change.getNodeId() != null) {
                    pending.add(change);
                }
            }
        }
        return pending;
    }

    private static List<AuditNode> nodesUnder(AuditNode node) {
        List<AuditNode> nodes = new ArrayList<>();
        if (node != null) {
            collect(node, nodes);
        }
        return nodes;
    }

    private static void collect(AuditNode node, List<AuditNode> nodes) {
        nodes.add(node);
        if (node.getChildren() != null) {
            for (AuditNode child : node.getChildren()) {
                if (child != null) {
                    collect(child, nodes);
                }
            }
        }
    }

    private static List<AuditNode> quizzesUnder(AuditNode node) {
        List<AuditNode> quizzes = new ArrayList<>();
        for (AuditNode each : nodesUnder(node)) {
            if (each.getTarget() == AuditTarget.QUIZ) {
                quizzes.add(each);
            }
        }
        return quizzes;
    }

    private static List<Finding> findingsUnder(AuditNode node) {
        List<Finding> findings = new ArrayList<>();
        for (AuditNode each : nodesUnder(node)) {
            if (each.getFindings() != null) {
                for (Finding finding : each.getFindings()) {
                    if (finding != null) {
                        findings.add(finding);
                    }
                }
            }
        }
        return findings;
    }

    private static Set<String> idsUnder(AuditNode node, String fallback) {
        Set<String> ids = new LinkedHashSet<>();
        if (fallback != null) {
            ids.add(fallback);
        }
        for (AuditNode each : nodesUnder(node)) {
            String id = ReportTree.idOf(each);
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    private static <T> List<T> listOrEmpty(List<T> list) {
        return list != null ? list : List.of();
    }
}
