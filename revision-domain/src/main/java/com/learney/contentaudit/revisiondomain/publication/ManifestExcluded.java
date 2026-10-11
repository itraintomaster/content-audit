package com.learney.contentaudit.revisiondomain.publication;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** Un cambio que quedó afuera de la tanda, con su motivo. */
@JsonPropertyOrder({"changeId", "reason"})
public record ManifestExcluded(String changeId, ExclusionReason reason) {
}
