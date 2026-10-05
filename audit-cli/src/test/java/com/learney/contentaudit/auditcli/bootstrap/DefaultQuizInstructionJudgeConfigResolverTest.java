package com.learney.contentaudit.auditcli.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.learney.contentaudit.agentruntimeinfrastructure.llmprovider.DefaultLocalSessionProviderCatalog;
import com.learney.contentaudit.quizinstructioninfrastructure.QuizInstructionJudgeConfig;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultQuizInstructionJudgeConfigResolverTest {

    private final DefaultQuizInstructionJudgeConfigResolver resolver =
            new DefaultQuizInstructionJudgeConfigResolver(new DefaultLocalSessionProviderCatalog());

    @Test
    void defaultsCodexToTheSelectedModelAndMediumReasoning() {
        QuizInstructionJudgeConfig config = resolver.resolve(Map.of());

        assertEquals("codex-cli", config.getProviderName());
        assertEquals("gpt-6.1-sol", config.getModelName());
        assertEquals("medium", config.getReasoningEffort());
    }

    @Test
    void specificModelWinsThenSharedModelAndReasoningRemainsSelectable() {
        QuizInstructionJudgeConfig specific = resolver.resolve(Map.of(
                DefaultQuizInstructionJudgeConfigResolver.KEY_MODEL, "qinst-model",
                DefaultQuizInstructionJudgeConfigResolver.KEY_SHARED_MODEL, "shared-model",
                DefaultQuizInstructionJudgeConfigResolver.KEY_REASONING_EFFORT, "high"));
        QuizInstructionJudgeConfig shared = resolver.resolve(Map.of(
                DefaultQuizInstructionJudgeConfigResolver.KEY_SHARED_MODEL, "shared-model"));

        assertEquals("qinst-model", specific.getModelName());
        assertEquals("high", specific.getReasoningEffort());
        assertEquals("shared-model", shared.getModelName());
    }

    @Test
    void anotherProviderWithoutAModelGetsNoCodexDefaults() {
        QuizInstructionJudgeConfig config = resolver.resolve(Map.of(
                DefaultQuizInstructionJudgeConfigResolver.KEY_PROVIDER, "claude-cli"));

        assertEquals("claude-cli", config.getProviderName());
        assertNull(config.getModelName());
        assertNull(config.getReasoningEffort());
    }
}
