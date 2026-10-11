package com.learney.contentaudit.revisiondomain.publication;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;

/**
 * F-PUBPROD-R001, inv. 6: lo que cambió y no se lleva a producción por este camino, con el cambio, el nodo, la parte y
 * el motivo. El ejercicio no va en la lista.
 */
@JsonPropertyOrder({"changeId", "nodeId", "part", "reason"})
public record ManifestUnsupported(String changeId, String nodeId, ExercisePart part, String reason) {
}
