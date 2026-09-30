package com.learney.contentaudit.coursedomain;

import java.util.Objects;

/**
 * What {@link FormEntity} cannot carry itself: Sentinel regenerates the model from
 * {@code sentinel.yaml}, where a model is only its fields, so its derived reading and its copy
 * live here, written by hand (FEAT-OPMUL, option A).
 */
public final class FormEntities {

    private FormEntities() {
    }

    /**
     * Typed reading of {@link FormEntity#getKind()}; {@link FormKind#OTHER} when there is no form.
     * The raw string stays untouched on the model: it has to round-trip byte for byte.
     */
    public static FormKind formKind(FormEntity form) {
        return form == null ? FormKind.OTHER : FormKind.from(form.getKind());
    }

    /**
     * Shallow copy of every field. Code that needs "the same form with one thing changed" copies
     * with this and then sets that one thing, so no field is silently dropped. It goes through the
     * full constructor on purpose: a field added to the model breaks this call at compile time
     * instead of being left out of the copy.
     */
    public static FormEntity copyOf(FormEntity other) {
        Objects.requireNonNull(other, "form to copy");
        return new FormEntity(other.getKind(), other.getIncidence(), other.getLabel(),
                other.getName(), other.getSentenceParts(), other.getMultipleChoice(),
                other.getUnmodeledFields());
    }
}
