package com.learney.contentaudit.agentruntimeinfrastructure.llmprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DefaultLlmProviderResolverTest {

    private final DefaultLlmProviderResolver resolver = new DefaultLlmProviderResolver(
            new DefaultLocalSessionProviderCatalog());

    @Test
    @DisplayName("resolves an absent or blank provider to codex-cli")
    void resolvesAbsentOrBlankProviderToCodexCli() {
        for (String declared : new String[] {null, "", "   "}) {
            LlmProviderResolved resolved = assertInstanceOf(LlmProviderResolved.class,
                    resolver.resolve(declared, null, "endpoint"));

            assertEquals("codex-cli", resolved.getProviderName());
            assertEquals(LlmProviderClass.LOCAL_SESSION, resolved.getProviderClass());
        }
    }

    @Test
    @DisplayName("preserves explicitly selected Claude instead of replacing it with Codex")
    void preservesExplicitClaudeSelection() {
        LlmProviderResolved resolved = assertInstanceOf(LlmProviderResolved.class,
                resolver.resolve(" claude-cli ", null, "endpoint"));

        assertEquals("claude-cli", resolved.getProviderName());
        assertEquals(LlmProviderClass.LOCAL_SESSION, resolved.getProviderClass());
    }

    @Test
    @DisplayName("rejects an unsupported explicit provider without falling back to codex-cli")
    void rejectsUnsupportedExplicitProviderWithoutFallback() {
        LlmProviderRejected rejected = assertInstanceOf(LlmProviderRejected.class,
                resolver.resolve("unsupported:provider", null, "endpoint"));

        assertEquals("unsupported:provider", rejected.getProviderName());
        assertEquals(LlmProviderRejectionKind.UNSUPPORTED_PROVIDER, rejected.getKind());
    }

    @Test
    @DisplayName("reports the missing endpoint for an unknown explicit provider without falling back")
    void reportsMissingEndpointForUnknownExplicitProviderWithoutFallback() {
        LlmProviderRejected rejected = assertInstanceOf(LlmProviderRejected.class,
                resolver.resolve("unknown-provider", null, "endpoint"));

        assertEquals("unknown-provider", rejected.getProviderName());
        assertEquals("endpoint", rejected.getMissingInput());
        assertEquals(LlmProviderRejectionKind.MISSING_REQUIRED_INPUT, rejected.getKind());
    }
}
