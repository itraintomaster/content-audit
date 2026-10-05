package com.learney.contentaudit.evaluationledgerdomain;

import java.beans.ConstructorProperties;
import java.beans.Transient;
import java.util.Objects;

/** Provider, exact model and reasoning effort that effectively produced an evaluation. */
public final class EvaluationRuntimeIdentity {

    public static final String NOT_APPLICABLE = "NOT_APPLICABLE";
    public static final String LEGACY_UNKNOWN = "LEGACY_UNKNOWN";

    private final EvaluationRuntimeKind kind;
    private final String providerName;
    private final String modelName;
    private final String reasoningEffort;

    @ConstructorProperties({"kind", "providerName", "modelName", "reasoningEffort"})
    public EvaluationRuntimeIdentity(EvaluationRuntimeKind kind, String providerName,
            String modelName, String reasoningEffort) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.providerName = requireValue(providerName, "providerName");
        this.modelName = requireValue(modelName, "modelName");
        this.reasoningEffort = requireValue(reasoningEffort, "reasoningEffort");
    }

    public static EvaluationRuntimeIdentity explicit(String providerName, String modelName,
            String reasoningEffort) {
        return new EvaluationRuntimeIdentity(EvaluationRuntimeKind.EXPLICIT, providerName,
                modelName, reasoningEffort);
    }

    public static EvaluationRuntimeIdentity legacyUnknown() {
        return new EvaluationRuntimeIdentity(EvaluationRuntimeKind.LEGACY_UNKNOWN,
                LEGACY_UNKNOWN, LEGACY_UNKNOWN, LEGACY_UNKNOWN);
    }

    public EvaluationRuntimeKind getKind() {
        return kind;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getModelName() {
        return modelName;
    }

    public String getReasoningEffort() {
        return reasoningEffort;
    }

    @Transient
    public boolean isExplicit() {
        return kind == EvaluationRuntimeKind.EXPLICIT;
    }

    private static String requireValue(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must be explicit and non-blank");
        }
        return value.trim();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EvaluationRuntimeIdentity that)) return false;
        return kind == that.kind
                && providerName.equals(that.providerName)
                && modelName.equals(that.modelName)
                && reasoningEffort.equals(that.reasoningEffort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, providerName, modelName, reasoningEffort);
    }

    @Override
    public String toString() {
        return kind + "[provider=" + providerName + ", model=" + modelName
                + ", reasoning=" + reasoningEffort + "]";
    }
}
