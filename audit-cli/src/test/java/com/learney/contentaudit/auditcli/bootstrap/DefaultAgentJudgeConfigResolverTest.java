package com.learney.contentaudit.auditcli.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
        assertNull(config.getModelName(), "the model remains selectable by the runtime default");
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
}
