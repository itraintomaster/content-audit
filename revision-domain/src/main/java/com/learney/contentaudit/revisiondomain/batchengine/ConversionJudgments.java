package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.comparison.ChangeOutcome;
import com.learney.contentaudit.revisiondomain.conversion.MultipleChoiceConversion;
import java.util.*;

/** Only complete judgments of the exact saved content count; absent findings never imply approval. */
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
                        item.after().equals(p.getElementAfter()) && item.before().equals(p.getElementBefore()))) continue;
            for (ChangeOutcome outcome : BatchBook.list(closing.getComparison().getChanges()))
                if (item.step().getQuizId().equals(outcome.getNodeId()))
                    for (String judge : MultipleChoiceConversion.JUDGES) outcomes.put(judge, outcome);
        }
        for (ContextOpinion.Run run : BatchBook.list(batch.getOpinionRuns())) {
            if (run.contentVersion() != batch.getContentVersion() || !BatchBook.list(run.proposals()).stream()
                    .anyMatch(p -> item.after().equals(p.getElementAfter()) && item.before().equals(p.getElementBefore()))) continue;
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
}
