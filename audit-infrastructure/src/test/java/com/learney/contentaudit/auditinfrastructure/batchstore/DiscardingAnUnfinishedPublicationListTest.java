package com.learney.contentaudit.auditinfrastructure.batchstore;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.revisiondomain.batch.BatchStatus;
import com.learney.contentaudit.revisiondomain.batch.CorrectionBatch;
import com.learney.contentaudit.revisiondomain.comparison.VerdictKind;
import com.learney.contentaudit.revisiondomain.publication.ManifestCounts;
import com.learney.contentaudit.revisiondomain.publication.ManifestSource;
import com.learney.contentaudit.revisiondomain.publication.ManifestVerdict;
import com.learney.contentaudit.revisiondomain.publication.PublicationManifest;
import com.learney.contentaudit.revisiondomain.publication.ReviewTally;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * F-PUBPROD-R010: la lista que dejó un intento de publicar que no terminó se borra del disco; la de una tanda publicada,
 * nunca.
 */
public class DiscardingAnUnfinishedPublicationListTest {
    private static final String ID = "tanda-A1-20261011T120000Z";

    @TempDir
    Path workdir;

    @Test
    @DisplayName("should discard the list an unfinished attempt left, with its folder, and refuse to discard the list of a published batch")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R010")
    public void shouldDiscardTheListAnUnfinishedAttemptLeftAndNeverTheListOfAPublishedBatch() throws Exception {
        FileSystemBatchStore store = new FileSystemBatchStore(workdir);
        CorrectionBatch batch = batch();
        store.save(batch);
        store.savePublicationManifest(ID, list("el intento que no terminó"));
        Path file = workdir.resolve(".content-audit/batch-publications/" + ID + "/manifest.json");
        assertTrue(Files.isRegularFile(file));

        store.discardPublicationManifest(ID);

        assertFalse(Files.exists(file), "la lista del intento se borra");
        assertFalse(Files.exists(file.getParent()), "y su carpeta, que quedó vacía");
        assertTrue(store.publicationManifest(ID).isEmpty());
        assertDoesNotThrow(() -> store.discardPublicationManifest(ID), "sin lista, no hace nada");

        store.savePublicationManifest(ID, list("la publicada"));
        batch.setStatus(BatchStatus.PUBLISHED);
        store.save(batch);
        byte[] published = Files.readAllBytes(file);

        IllegalStateException refusal = assertThrows(IllegalStateException.class,
                () -> store.discardPublicationManifest(ID));

        assertTrue(refusal.getMessage().contains("ya está publicada: su lista de publicación no se borra"),
                refusal.getMessage());
        assertEquals(new String(published), new String(Files.readAllBytes(file)), "la lista publicada sigue igual");
        assertThrows(IllegalArgumentException.class, () -> store.discardPublicationManifest("../afuera"));
    }

    private static CorrectionBatch batch() {
        CorrectionBatch batch = new CorrectionBatch();
        batch.setId(ID);
        batch.setLevel(CefrLevel.A1);
        batch.setStatus(BatchStatus.CLOSED);
        batch.setPlanId("plan-20260929");
        batch.setOpenedAt(Instant.parse("2026-10-11T12:00:00Z"));
        return batch;
    }

    private static PublicationManifest list(String detail) {
        return new PublicationManifest(PublicationManifest.SCHEMA, ID, "plan-20260929", "A1", "2026-10-12T03:10:44Z",
                "José", new ManifestSource("audit-20260929", "audit-20261010", null),
                new ManifestVerdict(VerdictKind.PASSES, true, List.of(), List.of(), detail),
                new ReviewTally(0, 0, 0, List.of(), List.of()), List.of(), List.of(), List.of(),
                new ManifestCounts(0, 0, Map.of()));
    }
}
