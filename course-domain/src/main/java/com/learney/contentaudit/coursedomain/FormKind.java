package com.learney.contentaudit.coursedomain;

/**
 * Typed reading of a form's raw {@code kind} string.
 *
 * <p>The raw string stays untouched in {@link FormEntity#getKind()} — it is the discriminator the
 * backend uses and it must round-trip byte for byte. This enum is how content-audit decides what
 * to do with a form. An unrecognized value is not an error: it maps to {@link #OTHER}, which the
 * pipeline treats like a CLOZE, exactly as before multiple choice was modeled.
 */
public enum FormKind {
    CLOZE,
    MULTIPLE_CHOICE,
    OTHER;

    public static FormKind from(String raw) {
        if ("CLOZE".equals(raw)) {
            return CLOZE;
        }
        if ("MULTIPLE_CHOICE".equals(raw)) {
            return MULTIPLE_CHOICE;
        }
        return OTHER;
    }
}
