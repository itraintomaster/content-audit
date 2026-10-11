package com.learney.contentaudit.revisiondomain.publication;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Un campo que cambió, con la ruta de producción y su valor de antes y de después en JSON simple (F-PUBPROD-R002,
 * inv. 3). El antes es el del curso del que partió la tanda; el después, el del curso que se publica.
 */
@JsonPropertyOrder({"path", "before", "after"})
public record ManifestField(String path, Object before, Object after) {
}
