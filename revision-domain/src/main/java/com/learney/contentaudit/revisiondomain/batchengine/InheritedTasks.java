package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.refinerdomain.RefinementPlan;
import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.refinerdomain.RefinementTaskStatus;
import com.learney.contentaudit.revisiondomain.batch.BatchChange;
import com.learney.contentaudit.revisiondomain.batch.BatchChangeStep;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * F-MOTIVO-R001: the tasks behind an inherited change go with it. A successor gets a plan of its own, numbered again
 * from task-001, so the predecessor's task ids name other exercises there. Each inherited task is copied at the end of
 * the new plan under an id that plan does not use yet, and the change points to the copy.
 */
final class InheritedTasks {
    private static final Pattern NUMBERED = Pattern.compile("task-(\\d+)");

    /**
     * What carrying did: changes that now point to copies, copies made, ids no predecessor could resolve, and the
     * copy of each predecessor's id (the nearest predecessor's, when two of them used the same id).
     */
    record Carried(int changes, int tasks, int unresolved, Map<String, String> renamed) {}

    private InheritedTasks() {}

    /**
     * Copies into {@code to} the tasks each change took from its predecessors, and points the change to the copies.
     * {@code predecessors} are their plans, the nearest first: an id is read in the first one where it is about the
     * change's exercise, so a predecessor that inherited without carrying still resolves through its own. {@code
     * fromPredecessor} says, by change id, which of its task ids came from a predecessor; a change it does not name
     * keeps its ids. An id no predecessor resolves is not copied and is counted as unresolved: F-MOTIVO-R002 ignores
     * it anyway.
     */
    static Carried carry(List<RefinementPlan> predecessors, RefinementPlan to, List<BatchChange> changes,
            Map<String, Set<String>> fromPredecessor) {
        List<Map<String, RefinementTask>> plans = predecessors.stream().map(BatchBook::tasksById).toList();
        if (to.getTasks() == null) to.setTasks(new ArrayList<>());
        int next = nextNumber(to);
        Map<String, String> copies = new HashMap<>();
        Map<String, String> renamed = new LinkedHashMap<>();
        int carried = 0, unresolved = 0;
        for (BatchChange change : changes) {
            if (change == null) continue;
            Set<String> inherited = fromPredecessor.getOrDefault(change.getChangeId(), Set.of());
            if (inherited.isEmpty()) continue;
            List<String> ids = new ArrayList<>();
            boolean moved = false;
            for (String id : BatchBook.list(change.getTaskIds())) {
                RefinementTask task = null;
                int plan = -1;
                for (int k = 0; inherited.contains(id) && task == null && k < plans.size(); k++) {
                    RefinementTask candidate = plans.get(k).get(id);
                    if (candidate != null && belongs(candidate, change)) { task = candidate; plan = k; }
                }
                if (task == null) {
                    if (inherited.contains(id)) unresolved++;
                    ids.add(id);
                    continue;
                }
                // One task can be the reason of several changes: they share its copy.
                String key = plan + "\u0000" + id;
                String copy = copies.get(key);
                if (copy == null) {
                    copy = String.format("task-%03d", next++);
                    to.getTasks().add(new RefinementTask(copy, task.getNodeTarget(), task.getNodeId(), task.getNodeLabel(),
                            task.getDiagnosisKind(), task.getPriority(), RefinementTaskStatus.IN_BATCH, task.getOrigin()));
                    copies.put(key, copy);
                    renamed.putIfAbsent(id, copy);
                }
                ids.add(copy);
                moved = true;
            }
            if (moved) {
                change.setTaskIds(ids);
                carried++;
            }
        }
        return new Carried(carried, copies.size(), unresolved, renamed);
    }

    /**
     * F-MOTIVO-R002: whether a task speaks about the exercise a change edits: the task is about one of its exercises,
     * about its topic, or reaches one of its exercises. Without a change's exercises there is nothing to tell, and the
     * task is taken as it comes.
     */
    static boolean belongs(RefinementTask task, BatchChange change) {
        return belongs(task, quizzesOf(change), change.getKnowledgeId());
    }

    /**
     * {@link #belongs(RefinementTask, BatchChange)} for the exercises and the topic of something that is not a change.
     * A task that does not say what it is about is taken as it comes too.
     */
    static boolean belongs(RefinementTask task, Set<String> quizzes, String knowledgeId) {
        if (quizzes.isEmpty() || task == null || task.getNodeId() == null) return true;
        if (task.getNodeId() != null && quizzes.contains(task.getNodeId())) return true;
        if (task.getNodeTarget() != AuditTarget.QUIZ && task.getNodeId() != null
                && task.getNodeId().equals(knowledgeId)) return true;
        return task.getOrigin() != null
                && BatchBook.list(task.getOrigin().getReachedQuizIds()).stream().anyMatch(quizzes::contains);
    }

    private static Set<String> quizzesOf(BatchChange change) {
        Set<String> quizzes = new LinkedHashSet<>();
        for (BatchChangeStep step : BatchBook.list(change.getSteps()))
            if (step != null && step.getQuizId() != null) quizzes.add(step.getQuizId());
        return quizzes;
    }

    private static int nextNumber(RefinementPlan plan) {
        int max = 0;
        for (RefinementTask task : BatchBook.list(plan.getTasks())) {
            if (task == null || task.getId() == null) continue;
            Matcher m = NUMBERED.matcher(task.getId());
            if (m.matches()) max = Math.max(max, Integer.parseInt(m.group(1)));
        }
        return max + 1;
    }
}
