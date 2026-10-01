package com.learney.contentaudit.revisiondomain.quizinstruction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.learney.contentaudit.revisiondomain.StrategyId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * F-CALUM-R003 inv. 5. Written ahead with the name generate gives it: its declaration goes in the follow-up patch of
 * FEAT-CALUM (requirements/2026-10-01.01_consigna-que-ve-el-alumno/architectural_patch.yaml). Until that patch is
 * applied the class is not generated, so it carries no @Generated mark.
 */
public class QuizInstructionAgentStrategyTest {
    @Test
    @DisplayName("should identify itself as quiz-instruction-agent 1.1.0, the version whose correction complies with the instructions of the quiz copy")
    @Tag("FEAT-CALUM")
    @Tag("F-CALUM-R003")
    public void shouldIdentifyItselfAsQuizinstructionagent110TheVersionWhoseCorrectionCompliesWithTheInstructionsOfTheQuizCopy(
            ) {
        QuizInstructionAgentStrategy strategy =
                new QuizInstructionAgentStrategy(mock(QuizInstructionCandidateGenerator.class), "llm:anthropic");

        StrategyId id = strategy.id();

        assertEquals("quiz-instruction-agent", id.getName());
        assertEquals("1.1.0", id.getVersion(),
                "F-CALUM-R003 inv. 5: its input changed, so its proposals say they were made by 1.1.0");
        assertEquals("llm:anthropic", id.getProviderId());
    }
}
