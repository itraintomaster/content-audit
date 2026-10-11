package com.learney.contentaudit.revisiondomain.publication;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/**
 * F-PUBPROD-R003 y R004: las cuentas de la revisión de José —pasos aprobados, sin revisar y rechazados— y los cambios
 * que salieron por un rechazo, con los que salieron encadenados a ellos.
 */
@JsonPropertyOrder({"approved", "pending", "rejected", "droppedForReviewChangeIds", "collateralDroppedChangeIds"})
public record ReviewTally(int approved, int pending, int rejected, List<String> droppedForReviewChangeIds,
        List<String> collateralDroppedChangeIds) {
}
