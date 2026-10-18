package com.learney.contentaudit.quizinstructioninfrastructure;

import com.learney.contentaudit.evaluationledgerdomain.EvaluatorVersionEquivalence;
import java.util.List;

/**
 * F-ALDIA-R002: the one moment the tests under {@code eval/} leave the count of a judge's version.
 * For each judge, the version as it was counted (tests included) and the one counted now are the
 * same version, written once with the judge, both versions, the moment and why.
 */
public interface AgentJudgeVersionCutover {
    /** What {@link #apply()} would write, judge by judge, without writing anything. */
    List<EvaluatorVersionEquivalence> preview();

    /**
     * Writes the equivalences and corrects the definitions whose text said the tests counted.
     * Refuses when it already ran: the equivalence is once and none is added after (inv. 4).
     */
    List<EvaluatorVersionEquivalence> apply();

    /** What the cut-over wrote, empty when it has not run. */
    List<EvaluatorVersionEquivalence> recorded();
}
