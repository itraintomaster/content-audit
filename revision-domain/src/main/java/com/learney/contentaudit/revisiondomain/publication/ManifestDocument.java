package com.learney.contentaudit.revisiondomain.publication;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/**
 * Un ejercicio ({@code quizTemplates}) o un tema ({@code knowledges}) que cambió: su identificador en producción, el
 * nodo de la tanda, los cambios y propuestas que lo originaron, si todos sus pasos están aprobados y los campos que
 * cambiaron.
 */
@JsonPropertyOrder({"collection", "id", "nodeId", "changeIds", "proposalIds", "reviewStatus", "fields"})
public record ManifestDocument(String collection, String id, String nodeId, List<String> changeIds,
        List<String> proposalIds, DocumentReviewStatus reviewStatus, List<ManifestField> fields) {
}
