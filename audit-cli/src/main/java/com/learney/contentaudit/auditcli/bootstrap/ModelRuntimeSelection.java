package com.learney.contentaudit.auditcli.bootstrap;

import java.util.Map;

/** Shared model and reasoning selection for every model-backed ContentAudit process. */
final class ModelRuntimeSelection {

    static final String KEY_MODEL = "CONTENT_AUDIT_MODEL";
    static final String KEY_REASONING_EFFORT = "CONTENT_AUDIT_REASONING_EFFORT";
    static final String CODEX_PROVIDER = "codex-cli";
    static final String DEFAULT_CODEX_MODEL = "gpt-6.1-sol";
    static final String DEFAULT_CODEX_REASONING_EFFORT = "medium";

    private ModelRuntimeSelection() {
    }

    static String model(Map<String, String> env, String specificKey, String effectiveProvider) {
        String specific = nonBlank(env.get(specificKey));
        if (specific != null) {
            return specific;
        }
        String shared = nonBlank(env.get(KEY_MODEL));
        if (shared != null) {
            return shared;
        }
        return isCodex(effectiveProvider) ? DEFAULT_CODEX_MODEL : null;
    }

    static String reasoningEffort(Map<String, String> env, String effectiveProvider) {
        String declared = nonBlank(env.get(KEY_REASONING_EFFORT));
        if (declared != null) {
            return declared;
        }
        return isCodex(effectiveProvider) ? DEFAULT_CODEX_REASONING_EFFORT : null;
    }

    static boolean isCodex(String effectiveProvider) {
        return CODEX_PROVIDER.equals(effectiveProvider);
    }

    static String nonBlank(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }
}
