package com.learney.contentaudit.agentruntimeinfrastructure.llmprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DefaultLocalSessionProviderCatalogTest {

    private final DefaultLocalSessionProviderCatalog catalog =
            new DefaultLocalSessionProviderCatalog();

    @Test
    @DisplayName("uses codex-cli and the local ChatGPT login as the default profile")
    void usesCodexCliAsDefaultProfile() {
        LocalSessionProviderProfile profile = catalog.defaultProfile();

        assertEquals("codex-cli", profile.getProviderName());
        assertEquals(List.of("codex", "login", "status"), profile.getAuthStatusCommand());
        assertEquals("codex login", profile.getLoginCommand());
    }

    @Test
    @DisplayName("keeps Claude available when the operator explicitly selects claude-cli")
    void findsExplicitClaudeProfile() {
        LocalSessionProviderProfile profile = catalog.find(" claude-cli ").orElseThrow();

        assertEquals("claude-cli", profile.getProviderName());
        assertEquals(List.of("claude", "auth", "status"), profile.getAuthStatusCommand());
        assertEquals("claude auth login", profile.getLoginCommand());
    }

    @Test
    @DisplayName("keeps Gemini available when the operator explicitly selects gemini-cli")
    void findsExplicitGeminiProfile() {
        LocalSessionProviderProfile profile = catalog.find(" gemini-cli ").orElseThrow();

        assertEquals("gemini-cli", profile.getProviderName());
        assertEquals(List.of("gemini", "--version"), profile.getAuthStatusCommand());
        assertEquals("gemini", profile.getLoginCommand());
    }

    @Test
    @DisplayName("keeps Antigravity available when the operator explicitly selects agy")
    void findsExplicitAgyProfile() {
        LocalSessionProviderProfile profile = catalog.find(" agy ").orElseThrow();

        assertEquals("agy", profile.getProviderName());
        assertEquals(List.of("agy", "--version"), profile.getAuthStatusCommand());
        assertEquals("agy", profile.getLoginCommand());
    }

    @Test
    @DisplayName("does not turn an unknown provider into the default profile")
    void returnsEmptyForUnknownProvider() {
        assertTrue(catalog.find("unknown-local-cli").isEmpty());
        assertTrue(catalog.find(null).isEmpty());
        assertTrue(catalog.find("   ").isEmpty());
    }
}
