package com.learney.contentaudit.revisiondomain.publication;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * De dónde sale la tanda: la línea base, el análisis del que partió y el análisis publicado. El publicado se conoce
 * recién después de escribir el curso, así que la lista lo deja vacío: queda en la publicación de la tanda.
 *
 * <p>{@code publishedAuditId} va siempre {@code null} y aun así no se saca: el esquema 1 de la lista, el que lee el
 * paso a producción, declara las tres claves de {@code source}, y una lista sin ella no lo cumple. El valor de verdad
 * queda en {@code BatchPublication.publishedAuditId}.
 */
@JsonPropertyOrder({"baselineAuditId", "startAuditId", "publishedAuditId"})
public record ManifestSource(String baselineAuditId, String startAuditId, String publishedAuditId) {
}
