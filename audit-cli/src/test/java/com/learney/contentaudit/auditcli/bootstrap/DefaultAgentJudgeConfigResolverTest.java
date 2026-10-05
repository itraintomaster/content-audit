package com.learney.contentaudit.auditcli.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.learney.contentaudit.agentruntimeinfrastructure.llmprovider.DefaultLocalSessionProviderCatalog;
import com.learney.contentaudit.quizinstructioninfrastructure.AgentJudgeConfig;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DefaultAgentJudgeConfigResolverTest {

    private final DefaultAgentJudgeConfigResolver resolver =
            new DefaultAgentJudgeConfigResolver(new DefaultLocalSessionProviderCatalog());

    @Test
    @DisplayName("defaults the shared judges to codex-cli when no provider is configured")
    void defaultsSharedJudgesToCodexCli() {
        AgentJudgeConfig config = resolver.resolve(Map.of());

        assertEquals("codex-cli", config.getProviderName());
        assertEquals("gpt-6.1-sol", config.getModelName());
        assertEquals("medium", config.getReasoningEffort());
    }

    @Test
    @DisplayName("treats a blank provider as absent and uses codex-cli")
    void treatsBlankProviderAsAbsent() {
        AgentJudgeConfig config = resolver.resolve(Map.of(
                DefaultAgentJudgeConfigResolver.KEY_PROVIDER, "   "));

        assertEquals("codex-cli", config.getProviderName());
    }

    @Test
    @DisplayName("preserves an explicitly selected provider and model")
    void preservesExplicitProviderAndModel() {
        AgentJudgeConfig config = resolver.resolve(Map.of(
                DefaultAgentJudgeConfigResolver.KEY_PROVIDER, " claude-cli ",
                DefaultAgentJudgeConfigResolver.KEY_MODEL, " claude-sonnet-4-5 "));

        assertEquals("claude-cli", config.getProviderName());
        assertEquals("claude-sonnet-4-5", config.getModelName());
    }

    @Test
    @DisplayName("uses the judge model before the shared model and keeps reasoning selectable")
    void usesSpecificModelBeforeSharedModel() {
        AgentJudgeConfig config = resolver.resolve(Map.of(
                DefaultAgentJudgeConfigResolver.KEY_MODEL, "judge-model",
                DefaultAgentJudgeConfigResolver.KEY_SHARED_MODEL, "shared-model",
                DefaultAgentJudgeConfigResolver.KEY_REASONING_EFFORT, " high "));

        assertEquals("judge-model", config.getModelName());
        assertEquals("high", config.getReasoningEffort());
    }

    @Test
    @DisplayName("uses the shared model before the Codex default")
    void usesSharedModelBeforeCodexDefault() {
        AgentJudgeConfig config = resolver.resolve(Map.of(
                DefaultAgentJudgeConfigResolver.KEY_SHARED_MODEL, "shared-model"));

        assertEquals("shared-model", config.getModelName());
    }

    @Test
    @DisplayName("does not invent a Codex model or reasoning default for another provider")
    void doesNotInventCodexDefaultsForAnotherProvider() {
        AgentJudgeConfig config = resolver.resolve(Map.of(
                DefaultAgentJudgeConfigResolver.KEY_PROVIDER, "claude-cli"));

        assertEquals("claude-cli", config.getProviderName());
        org.junit.jupiter.api.Assertions.assertNull(config.getModelName());
        org.junit.jupiter.api.Assertions.assertNull(config.getReasoningEffort());
    }
}
