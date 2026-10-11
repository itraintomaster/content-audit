package com.learney.contentaudit.revisiondomain.publication;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * De dónde sale la tanda: la línea base, el análisis del que partió y el análisis publicado. El publicado se conoce
 * recién después de escribir el curso, así que la lista lo deja vacío: queda en la publicación de la tanda.
 */
@JsonPropertyOrder({"baselineAuditId", "startAuditId", "publishedAuditId"})
public record ManifestSource(String baselineAuditId, String startAuditId, String publishedAuditId) {
}
