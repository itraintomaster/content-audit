package com.learney.contentaudit.revisiondomain.publication;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Map;

/** Cuántos ejercicios y temas lleva la lista, cuántos campos, y cuántos de cada ruta. */
@JsonPropertyOrder({"documents", "fields", "byPath"})
public record ManifestCounts(int documents, int fields, Map<String, Integer> byPath) {
}
