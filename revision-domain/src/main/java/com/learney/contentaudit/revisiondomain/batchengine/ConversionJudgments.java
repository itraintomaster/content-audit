package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.comparison.ChangeOutcome;
import com.learney.contentaudit.revisiondomain.conversion.MultipleChoiceConversion;
import java.util.*;

/**
 * Only complete judgments of the exact content that reaches production count; absent findings never imply approval.
 */
final class ConversionJudgments {
    static boolean conversion(BatchReview.Item item) {
        return item.ruleId() != null && item.ruleId().startsWith(MultipleChoiceConversion.RULE + ":");
    }
    static void requireApproved(CorrectionBatch batch, BatchReview.Item item) {
        if (!conversion(item) || item.after().getQuiz().getClozeOriginal() == null
                || !"MULTIPLE_CHOICE".equals(item.after().getQuiz().getForm().getKind())) return;
        Map<String, ChangeOutcome> outcomes = new HashMap<>();
        for (BatchClosing closing : BatchBook.list(batch.getClosings())) {
            if (closing.getContentVersion() != batch.getContentVersion() || closing.getComparison() == null
                    || !BatchBook.list(closing.getProposals()).stream().anyMatch(p ->
                        reaches(p.getElementAfter(), item))) continue;
            for (ChangeOutcome outcome : BatchBook.list(closing.getComparison().getChanges()))
                if (item.step().getQuizId().equals(outcome.getNodeId()))
                    for (String judge : MultipleChoiceConversion.JUDGES) outcomes.put(judge, outcome);
        }
        for (ContextOpinion.Run run : BatchBook.list(batch.getOpinionRuns())) {
            if (run.contentVersion() != batch.getContentVersion() || !BatchBook.list(run.proposals()).stream()
                    .anyMatch(p -> reaches(p.getElementAfter(), item))) continue;
            for (ContextOpinion.Result result : BatchBook.list(run.results()))
                if (result.outcome() != null && item.step().getQuizId().equals(result.outcome().getNodeId()))
                    outcomes.put(result.analyzer(), result.outcome());
        }
        for (String judge : MultipleChoiceConversion.JUDGES) {
            ChangeOutcome result = outcomes.get(judge);
            if (result == null || !BatchBook.list(result.getEvaluatedAnalyzers()).contains(judge)
                    || result.getAfterFindings() == null || BatchBook.list(result.getUndecidedAnalyzers()).contains(judge)
                    || result.getAfterFindings().stream().anyMatch(f -> judge.equals(f.getAnalyzer())))
                throw new IllegalArgumentException("La conversión necesita la aprobación vigente de " + judge + "; pedí su opinión antes de aprobar.");
        }
    }

    /**
     * F-CONVERSION-R002: whether a measured proposal is the converted exercise as it reaches production -- the same
     * exercise, still a multiple choice that keeps the cloze original of the conversion. A later correction of the same
     * exercise may have changed it after converting it: the judges approve that version, the one that is published, and
     * the content version of the batch says it is today's.
     */
    private static boolean reaches(com.learney.contentaudit.revisiondomain.CourseElementSnapshot measured,
            BatchReview.Item item) {
        var quiz = measured != null ? measured.getQuiz() : null;
        var converted = item.after().getQuiz();
        return quiz != null && Objects.equals(measured.getNodeId(), item.step().getQuizId())
                && quiz.getForm() != null && "MULTIPLE_CHOICE".equals(quiz.getForm().getKind())
                && Objects.equals(quiz.getClozeOriginal(), converted.getClozeOriginal());
    }
}
