package com.learney.contentaudit.revisiondomain.publication;

/** Por qué un cambio quedó afuera de la tanda. */
public enum ExclusionReason {
    /** José lo rechazó en la revisión (F-PUBPROD-R003). */
    REVIEW_REJECTED,

    /** Salió encadenado con un rechazado: tocó lo mismo en el mismo ejercicio (F-PUBPROD-R003, inv. 1). */
    COLLATERAL,

    /** José sacó su regla al publicar, desde la bandeja. */
    RULE_EXCLUDED,

    /** José lo sacó antes de publicar (F-TANDA-R008), solo o con su regla, o salió encadenado con eso. */
    DROPPED_BY_JOSE
}
