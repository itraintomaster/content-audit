package com.learney.contentaudit.revisiondomain.publication;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/**
 * F-PUBPROD-R001: la lista de publicación de una tanda, esquema 1. Es lo único que el paso a producción toma
 * (F-PUBPROD-R006): por cada ejercicio o tema que cambió, los campos que cambiaron con su antes y su después. El orden
 * de las claves es parte del contrato con el paso a producción.
 */
@JsonPropertyOrder({"schema", "batchId", "planId", "level", "publishedAt", "approvedBy", "source", "verdict",
        "reviews", "documents", "unsupported", "excluded", "counts"})
public record PublicationManifest(int schema, String batchId, String planId, String level, String publishedAt,
        String approvedBy, ManifestSource source, ManifestVerdict verdict, ReviewTally reviews,
        List<ManifestDocument> documents, List<ManifestUnsupported> unsupported, List<ManifestExcluded> excluded,
        ManifestCounts counts) {

    /** La versión del esquema que escribe esta lista. */
    public static final int SCHEMA = 1;
}
