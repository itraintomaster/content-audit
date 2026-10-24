package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.auditdomain.datacoherence.DataCoherenceStage;
import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.refinerdomain.TaskOrigin;
import java.util.List;

/** Projects legacy task origins without rewriting their saved history or widening their edit contract. */
final class ContentCorrectionTasks {
    private ContentCorrectionTasks() {}
    static boolean audioOnly(RefinementTask task) {
        return task != null && task.getOrigin() != null
                && DataCoherenceStage.isAudioFinding(task.getOrigin().getFinding())
                && BatchBook.list(task.getOrigin().getCoveredFindings()).stream().allMatch(DataCoherenceStage::isAudioFinding);
    }
    static RefinementTask content(RefinementTask task) {
        if (task == null || task.getOrigin() == null || audioOnly(task)) return task;
        TaskOrigin source = task.getOrigin();
        var covered = BatchBook.list(source.getCoveredFindings()).stream()
                .filter(f -> !DataCoherenceStage.isAudioFinding(f)).toList();
        var finding = source.getFinding();
        if (DataCoherenceStage.isAudioFinding(finding)) finding = covered.get(0);
        var origin = new TaskOrigin(finding, source.getCefrLevel(), source.getKnowledgeId(),
                source.getKnowledgeLabel(), source.getReachedQuizIds(), covered);
        return new RefinementTask(task.getId(), task.getNodeTarget(), task.getNodeId(), task.getNodeLabel(),
                task.getDiagnosisKind(), task.getPriority(), task.getStatus(), origin);
    }
    static List<RefinementTask> content(List<RefinementTask> tasks) {
        return BatchBook.list(tasks).stream().filter(t -> !audioOnly(t)).map(ContentCorrectionTasks::content).toList();
    }
}
