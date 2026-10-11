package com.learney.contentaudit.revisiondomain.trayengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learney.contentaudit.revisiondomain.batch.BatchClosing;
import com.learney.contentaudit.revisiondomain.batch.BatchNotPublishableException;
import com.learney.contentaudit.revisiondomain.batch.BatchPublication;
import com.learney.contentaudit.revisiondomain.batch.BatchStatus;
import com.learney.contentaudit.revisiondomain.batchengine.TandaFixture;
import com.learney.contentaudit.revisiondomain.comparison.BatchComparison;
import com.learney.contentaudit.revisiondomain.comparison.BatchVerdict;
import com.learney.contentaudit.revisiondomain.comparison.RowSubject;
import com.learney.contentaudit.revisiondomain.comparison.UndecidableFloors;
import com.learney.contentaudit.revisiondomain.comparison.VerdictKind;
import com.learney.contentaudit.revisiondomain.projection.ProjectionScope;
import com.learney.contentaudit.revisiondomain.publication.PublicationManifest;
import com.learney.contentaudit.revisiondomain.tray.TrayAnswer;
import com.learney.contentaudit.revisiondomain.tray.TrayResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * F-BANDEJA-R008 con F-PUBPROD-R001 y R005: el OK de la bandeja sólo se acepta con «pasa» —«no se puede decidir» no
 * alcanza, aunque la compuerta pase— y, aceptado, deja la lista de lo que puede llegar a producción con su huella.
 */
public class PublishingFromTheTrayTest {

    private static final String RULE_ITEM = "muestra-hint-coherence";

    /** La bandeja con la muestra ciega contestada y la muestra de la regla aprobada: lista para el OK. */
    private static BandejaFixture.Bandeja ready() {
        BandejaFixture.Bandeja bandeja = new BandejaFixture.Bandeja();
        bandeja.answerTheBlindSample();
        bandeja.tray.answer(new TrayResponse(RULE_ITEM, TrayAnswer.RULE_OK, null, null, null, null, null, "José"));
        return bandeja;
    }

    private static BatchComparison undecidable(ProjectionScope scope, VerdictKind kind, String detail) {
        BatchComparison comparison = TandaFixture.comparison(scope, List.of(), kind);
        comparison.setVerdict(new BatchVerdict(kind, new ArrayList<>(), new ArrayList<>(), null, detail,
                new UndecidableFloors(1, List.of(new RowSubject("lemma-absence", "Palabras del nivel")))));
        return comparison;
    }

    private static BatchClosing lastClosing(BandejaFixture.Bandeja bandeja) {
        List<BatchClosing> closings = bandeja.saved().getClosings();
        return closings.get(closings.size() - 1);
    }

    @Test
    @DisplayName("should refuse the OK with «no se puede decidir» when the verdict cannot be decided or keeps an undecidable floor, although the gate passes, writing nothing")
    @Tag("FEAT-BANDEJA")
    @Tag("F-BANDEJA-R008")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R005")
    public void shouldRefuseTheOKWithNoSePuedeDecidirAlthoughTheGatePasses() {
        // R005 inv. 1 y 2: con todas las decisiones de José, F no puede decidir: ningún piso falla, pero uno queda sin
        // decidir. La compuerta de «no empeorar» pasa igual —ningún piso se rompe—, pero el OK se niega, la negativa
        // dice «no se puede decidir» con lo que dice F, y no se escribe nada.
        BandejaFixture.Bandeja bandeja = ready();
        bandeja.comparator.answer = scope -> undecidable(scope, VerdictKind.UNDECIDABLE,
                "ningún piso falla, pero 1 piso sin decidir; juez sin aprobar: Consigna que no se cumple");

        BatchNotPublishableException refused = assertThrows(BatchNotPublishableException.class,
                () -> bandeja.tray.publish("José", null));

        assertTrue(refused.getMessage().contains("el veredicto de F es «no se puede decidir» (1 piso sin decidir), no"
                + " «pasa»"), refused.getMessage());
        assertTrue(refused.getMessage().contains("juez sin aprobar: Consigna que no se cumple"),
                "copia lo que dice F: " + refused.getMessage());
        assertFalse(refused.getMessage().contains("compuerta"), refused.getMessage());
        assertTrue(lastClosing(bandeja).getGate().isPasses(), "la compuerta pasa: hacen falta las dos");
        assertTrue(bandeja.courses.written.isEmpty(), "no se escribe nada");
        assertTrue(bandeja.batches.manifests.isEmpty(), "no queda lista");
        assertFalse(bandeja.saved().getStatus() == BatchStatus.PUBLISHED);
        assertNull(bandeja.saved().getPublication());

        // «pasa» con un piso sin decidir tampoco es «pasa».
        BandejaFixture.Bandeja floor = ready();
        floor.comparator.answer = scope -> undecidable(scope, VerdictKind.PASSES, null);

        BatchNotPublishableException byFloor = assertThrows(BatchNotPublishableException.class,
                () -> floor.tray.publish("José", null));

        assertTrue(byFloor.getMessage().contains("«no se puede decidir» (1 piso sin decidir)"), byFloor.getMessage());
        assertTrue(floor.courses.written.isEmpty());
        assertTrue(floor.batches.manifests.isEmpty());
    }

    @Test
    @DisplayName("should leave, with the accepted OK, the publication list of the batch and record its digest, the verdict and the gate")
    @Tag("FEAT-BANDEJA")
    @Tag("F-BANDEJA-R008")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R001")
    @Tag("F-PUBPROD-R005")
    public void shouldLeaveWithTheAcceptedOKThePublicationListOfTheBatchAndRecordItsDigest() {
        // F-BANDEJA-R008 y F-PUBPROD-R001 inv. 4: el OK aceptado publica la tanda y deja su lista; la publicación
        // registra la huella de la lista, el veredicto «pasa», que la compuerta pasó y ningún piso sin decidir.
        BandejaFixture.Bandeja bandeja = ready();

        BatchPublication publication = bandeja.tray.publish("José", "la tanda de A1");

        assertEquals(BatchStatus.PUBLISHED, bandeja.saved().getStatus());
        assertEquals(bandeja.batches.digests.get(BandejaFixture.BATCH), publication.getManifestDigest());
        assertEquals(VerdictKind.PASSES, publication.getVerdictKind());
        assertEquals(Boolean.TRUE, publication.getGatePassed());
        assertEquals(List.of(), publication.getUndecidableFloors());
        PublicationManifest list = bandeja.batches.publicationManifest(BandejaFixture.BATCH).orElseThrow();
        assertEquals(BandejaFixture.BATCH, list.batchId());
        assertEquals("José", list.approvedBy());
        assertEquals(VerdictKind.PASSES, list.verdict().kind());
        assertTrue(list.verdict().gatePassed());
        assertEquals(List.of(), list.verdict().undecidableFloors());
        assertFalse(list.documents().isEmpty(), "lleva lo que cambió la tanda");
        assertEquals(list.documents().size(), list.counts().documents());
    }
}
