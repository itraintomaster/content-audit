package com.learney.contentaudit.revisiondomain.publication;

import java.util.List;

/** Lo que hace falta para armar la lista: la tanda, el veredicto, las cuentas, lo que cambió y lo que quedó afuera. */
public record PublicationManifestRequest(String batchId, String planId, String level, String publishedAt,
        String approvedBy, ManifestSource source, ManifestVerdict verdict, ReviewTally reviews,
        List<PublishedElement> elements, List<ManifestExcluded> excluded) {
    public PublicationManifestRequest {
        elements = List.copyOf(elements);
        excluded = List.copyOf(excluded);
    }
}
