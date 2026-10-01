package com.learney.contentaudit.revisiondomain.trayengine;

import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.finding.AnalysisCost;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingEvidence;
import com.learney.contentaudit.auditdomain.finding.FindingIdentity;
import com.learney.contentaudit.auditdomain.finding.FindingNodeRef;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.MilestoneEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.TopicEntity;
import com.learney.contentaudit.revisiondomain.batch.BatchAdjustment;
import com.learney.contentaudit.revisiondomain.batch.BatchAdjustmentKind;
import com.learney.contentaudit.revisiondomain.batch.BatchChange;
import com.learney.contentaudit.revisiondomain.batch.BatchChangeOrigin;
import com.learney.contentaudit.revisiondomain.batch.BatchChangeStep;
import com.learney.contentaudit.revisiondomain.batch.BatchTaskOutcome;
import com.learney.contentaudit.revisiondomain.batch.BatchTaskOutcomeKind;
import com.learney.contentaudit.revisiondomain.batch.CorrectionBatch;
import com.learney.contentaudit.revisiondomain.panel.PanelDecision;
import com.learney.contentaudit.revisiondomain.panel.PanelOutcomeKind;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * What the tray reads of a batch, in one place, so the view and the answers read the same thing: the exercise of the
 * course, the decisions of the panel on an exercise that still wait for José, the exercises without winner, the rules
 * whose sample is still to approve, and the ids of the things of the tray.
 */
final class TrayBook {

    static final String BLIND = "ciega-";

    static final String TIE_ON_COURSE = "desempate-curso-";

    static final String TIE_ON_CANDIDATE = "desempate-";

    static final String NO_WINNER = "sin-ganador-";

    static final String RULE_SAMPLE = "muestra-";

    static final String RERECORD = "regrabar-";

    private TrayBook() {
    }

    static <T> List<T> list(List<T> values) {
        return values != null ? values : List.of();
    }

    // -------------------------------------------------------------------------
    // The course
    // -------------------------------------------------------------------------

    record Place(KnowledgeEntity knowledge, QuizTemplateEntity quiz) {
    }

    static Optional<Place> place(CourseEntity course, String quizId) {
        if (course == null || course.getRoot() == null || quizId == null) {
            return Optional.empty();
        }
        for (MilestoneEntity milestone : list(course.getRoot().getMilestones())) {
            for (TopicEntity topic : list(milestone.getTopics())) {
                for (KnowledgeEntity knowledge : list(topic.getKnowledges())) {
                    for (QuizTemplateEntity quiz : list(knowledge.getQuizTemplates())) {
                        if (quiz != null && quizId.equals(quiz.getId())) {
                            return Optional.of(new Place(knowledge, quiz));
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    // -------------------------------------------------------------------------
    // What waits for José
    // -------------------------------------------------------------------------

    static BatchTaskOutcomeKind kind(CorrectionBatch batch, String taskId) {
        for (BatchTaskOutcome outcome : list(batch.getOutcomes())) {
            if (outcome != null && Objects.equals(outcome.getTaskId(), taskId)) {
                return outcome.getKind();
            }
        }
        return BatchTaskOutcomeKind.PENDING;
    }

    static Optional<BatchTaskOutcome> outcome(CorrectionBatch batch, String taskId) {
        for (BatchTaskOutcome outcome : list(batch.getOutcomes())) {
            if (outcome != null && Objects.equals(outcome.getTaskId(), taskId)) {
                return Optional.of(outcome);
            }
        }
        return Optional.empty();
    }

    /** The latest decision of the panel on each exercise, in the order of the exercises. */
    static Map<String, PanelDecision> latestDecisions(CorrectionBatch batch) {
        Map<String, PanelDecision> latest = new LinkedHashMap<>();
        for (PanelDecision decision : list(batch.getPanelDecisions())) {
            if (decision == null) {
                continue;
            }
            for (String quizId : list(decision.getQuizIds())) {
                latest.remove(quizId);
                latest.put(quizId, decision);
            }
        }
        return latest;
    }

    /** The ties on a candidate whose tasks still wait for José. */
    static Map<String, PanelDecision> candidateTies(CorrectionBatch batch) {
        Map<String, PanelDecision> ties = new LinkedHashMap<>();
        for (Map.Entry<String, PanelDecision> entry : latestDecisions(batch).entrySet()) {
            PanelDecision decision = entry.getValue();
            if (decision.getOutcome() == PanelOutcomeKind.TIE && decision.getBestFailing() != null
                    && !list(decision.getTaskIds()).isEmpty()
                    && list(decision.getTaskIds()).stream().allMatch(t -> kind(batch, t) == BatchTaskOutcomeKind.TIE)) {
                ties.put(entry.getKey(), decision);
            }
        }
        return ties;
    }

    /** An exercise nobody fixed: its tasks, the decision of the panel that gave up and the change the gate returned. */
    record NoWinner(String quizId, List<String> taskIds, PanelDecision decision, BatchChange returned) {
    }

    /**
     * The exercises whose tasks ended without winner -- the panel gave up -- or returned without attempts -- the gate of
     * the batch returned their change the last time it could --, in the order of the tasks of the batch.
     */
    static List<NoWinner> noWinners(CorrectionBatch batch) {
        Map<String, PanelDecision> latest = latestDecisions(batch);
        Map<String, Set<String>> tasksByQuiz = new LinkedHashMap<>();
        Map<String, BatchChange> returnedByQuiz = new LinkedHashMap<>();
        for (String taskId : list(batch.getTaskIds())) {
            BatchTaskOutcomeKind kind = kind(batch, taskId);
            if (kind != BatchTaskOutcomeKind.NO_WINNER && kind != BatchTaskOutcomeKind.RETURNED_WITHOUT_ATTEMPTS) {
                continue;
            }
            String quizId = null;
            BatchChange returned = null;
            for (Map.Entry<String, PanelDecision> entry : latest.entrySet()) {
                if (list(entry.getValue().getTaskIds()).contains(taskId)) {
                    quizId = entry.getKey();
                }
            }
            if (kind == BatchTaskOutcomeKind.RETURNED_WITHOUT_ATTEMPTS || quizId == null) {
                for (BatchChange change : list(batch.getChanges())) {
                    if (change != null && change.getDroppedReason() != null && list(change.getTaskIds()).contains(taskId)
                            && !list(change.getSteps()).isEmpty()) {
                        returned = change;
                        quizId = change.getSteps().get(0).getQuizId();
                    }
                }
            }
            if (quizId == null) {
                continue;
            }
            tasksByQuiz.computeIfAbsent(quizId, q -> new LinkedHashSet<>()).add(taskId);
            if (returned != null) {
                returnedByQuiz.put(quizId, returned);
            }
        }
        List<NoWinner> noWinners = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : tasksByQuiz.entrySet()) {
            PanelDecision decision = latest.get(entry.getKey());
            if (decision != null && decision.getOutcome() != PanelOutcomeKind.NO_WINNER
                    && decision.getOutcome() != PanelOutcomeKind.TIE) {
                decision = null;
            }
            noWinners.add(new NoWinner(entry.getKey(), new ArrayList<>(entry.getValue()), decision,
                    returnedByQuiz.get(entry.getKey())));
        }
        return noWinners;
    }

    /** The rules with changes still in the batch whose sample José did not approve, in the order of their changes. */
    static List<String> rulesToSample(CorrectionBatch batch) {
        Set<String> approved = new LinkedHashSet<>();
        for (BatchAdjustment adjustment : list(batch.getAdjustments())) {
            if (adjustment != null && adjustment.getKind() == BatchAdjustmentKind.APPROVE_RULE_SAMPLE
                    && adjustment.getRuleId() != null) {
                approved.add(adjustment.getRuleId());
            }
        }
        Set<String> rules = new LinkedHashSet<>();
        for (BatchChange change : list(batch.getChanges())) {
            if (change != null && change.getDroppedReason() == null && change.getOrigin() == BatchChangeOrigin.RULE
                    && change.getRuleId() != null && !approved.contains(change.getRuleId())) {
                rules.add(change.getRuleId());
            }
        }
        return new ArrayList<>(rules);
    }

    static int changesOf(CorrectionBatch batch, String ruleId) {
        int count = 0;
        for (BatchChange change : list(batch.getChanges())) {
            if (change != null && change.getDroppedReason() == null && change.getOrigin() == BatchChangeOrigin.RULE
                    && Objects.equals(change.getRuleId(), ruleId)) {
                count++;
            }
        }
        return count;
    }

    /** The exercise as the changes still in the batch leave it, or empty when none touched it. */
    static Optional<QuizTemplateEntity> changed(CorrectionBatch batch, String quizId) {
        QuizTemplateEntity last = null;
        for (BatchChange change : list(batch.getChanges())) {
            if (change == null || change.getDroppedReason() != null) {
                continue;
            }
            for (BatchChangeStep step : list(change.getSteps())) {
                if (step != null && Objects.equals(step.getQuizId(), quizId) && step.getAfter() != null
                        && step.getAfter().getQuiz() != null) {
                    last = step.getAfter().getQuiz();
                }
            }
        }
        return Optional.ofNullable(last);
    }

    // -------------------------------------------------------------------------
    // What is remembered
    // -------------------------------------------------------------------------

    /**
     * The question of an exercise without winner, as a finding the memory can keep: the exercise and its tasks. It is
     * not shown; it names the question so the same one is not asked again while the exercise does not change.
     */
    static Finding noWinnerQuestion(String quizId, List<String> taskIds) {
        String key = "sin-ganador|" + quizId + "|" + String.join(",", new TreeSet<>(taskIds));
        return new Finding("bandeja", "sin-ganador", new FindingNodeRef(AuditTarget.QUIZ, quizId, quizId),
                FindingSeverity.MEDIUM, new FindingEvidence(new ArrayList<>(), "nadie lo arregló", new ArrayList<>()),
                FindingResolution.PANEL, AnalysisCost.INSTANT, new FindingIdentity(key, null));
    }

    /** The most severe of the findings, or null without any. */
    static FindingSeverity mostSevere(List<Finding> findings) {
        FindingSeverity most = null;
        for (Finding finding : list(findings)) {
            FindingSeverity severity = finding != null ? finding.getSeverity() : null;
            if (severity != null && (most == null || severity.ordinal() < most.ordinal())) {
                most = severity;
            }
        }
        return most;
    }
}
