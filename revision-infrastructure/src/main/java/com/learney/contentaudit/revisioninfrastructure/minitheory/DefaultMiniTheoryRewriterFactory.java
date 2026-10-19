package com.learney.contentaudit.revisioninfrastructure.minitheory;

import com.learney.contentaudit.revisiondomain.minitheorycorrection.MiniTheoryRewriter;
import com.learney.contentaudit.revisioninfrastructure.lagen.LagenConfig;
import com.sentinel.agents.analyst.LlmFactory;
import com.sentinel.agents.framework.runner.FrameworkLlmListener;
import java.util.List;

/** Dedicated, tool-free rewrite. Construction is lazy and never selects a fallback provider. */
public final class DefaultMiniTheoryRewriterFactory {
    public MiniTheoryRewriter create(LagenConfig config, String reasoningEffort) {
        if (config.getProviderName() == null || config.getModelId() == null) {
            throw new IllegalArgumentException("Elegí proveedor y modelo antes de reescribir.");
        }
        var model = LlmFactory.build(new LlmFactory.LlmConfig(config.getProviderName(), config.getModelId(),
                config.getEndpoint(), config.getApiKey(), config.getMaxTokens() == null ? 4096 : config.getMaxTokens(),
                config.getTemperature() == null ? 0.7 : config.getTemperature()), List.of(new FrameworkLlmListener()));
        return new MiniTheoryRewriterAdapter(model::chat,
                config.getProviderName() + ":" + config.getModelId() + "; effort=" + reasoningEffort
                        + "; prompt=" + MiniTheoryRewriterAdapter.PROMPT_VERSION);
    }
}
