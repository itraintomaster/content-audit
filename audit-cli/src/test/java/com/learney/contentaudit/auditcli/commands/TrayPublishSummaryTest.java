package com.learney.contentaudit.auditcli.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.revisiondomain.batch.BatchPublication;
import com.learney.contentaudit.revisiondomain.batch.BatchStatus;
import com.learney.contentaudit.revisiondomain.batch.BatchStore;
import com.learney.contentaudit.revisiondomain.batch.CorrectionBatch;
import com.learney.contentaudit.revisiondomain.comparison.VerdictKind;
import com.learney.contentaudit.revisiondomain.publication.ManifestCounts;
import com.learney.contentaudit.revisiondomain.publication.ManifestSource;
import com.learney.contentaudit.revisiondomain.publication.ManifestUnsupported;
import com.learney.contentaudit.revisiondomain.publication.ManifestVerdict;
import com.learney.contentaudit.revisiondomain.publication.PublicationManifest;
import com.learney.contentaudit.revisiondomain.publication.ReviewTally;
import com.learney.contentaudit.revisiondomain.tray.TrayService;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Lo que `tray publish` le muestra a José de una tanda publicada (F-PUBPROD R001, R004 y R005): con qué veredicto y
 * compuerta se publicó, las cuentas de la revisión con lo que quedó afuera, y la lista —dónde quedó, su huella, qué
 * lleva y qué no tiene lugar en producción—.
 */
public class TrayPublishSummaryTest {

    private static final String BATCH = "tanda-A1-20261011T030000Z";

    private static final String DIGEST = "35ebddf49dca87164550faf68201de16833990a3dce765d5e7db397c11ae7368";

    private static final String LOCATION = "/curso/.content-audit/batch-publications/" + BATCH + "/manifest.json";

    /** La bandeja que publica y devuelve {@code publication}. */
    private static TrayService publishing(BatchPublication publication) {
        return (TrayService) Proxy.newProxyInstance(TrayService.class.getClassLoader(),
                new Class<?>[] {TrayService.class}, (proxy, method, args) -> {
                    if (method.getName().equals("publish")) {
                        return publication;
                    }
                    throw new AssertionError("la bandeja sólo publica: " + method.getName());
                });
    }

    /** Las tandas guardadas: la última publicada, con su lista si la tiene. */
    private static final class Saved implements BatchStore {
        private final CorrectionBatch latest;

        private final PublicationManifest manifest;

        Saved(CorrectionBatch latest, PublicationManifest manifest) {
            this.latest = latest;
            this.manifest = manifest;
        }

        @Override
        public void save(CorrectionBatch batch) {
            throw new AssertionError("mostrar la publicación no guarda nada");
        }

        @Override
        public Optional<CorrectionBatch> load(String batchId) {
            return Optional.ofNullable(latest).filter(batch -> batch.getId().equals(batchId));
        }

        @Override
        public Optional<CorrectionBatch> open() {
            return Optional.empty();
        }

        @Override
        public List<CorrectionBatch> list() {
            return latest != null ? List.of(latest) : List.of();
        }

        @Override
        public Optional<CorrectionBatch> latestPublished() {
            return Optional.ofNullable(latest);
        }

        @Override
        public Optional<PublicationManifest> publicationManifest(String batchId) {
            return Optional.ofNullable(manifest).filter(m -> BATCH.equals(batchId));
        }

        @Override
        public Optional<String> publicationManifestLocation(String batchId) {
            return Optional.ofNullable(manifest).filter(m -> BATCH.equals(batchId)).map(m -> LOCATION);
        }
    }

    private static TandaCli.Run run(TrayService tray, BatchStore saved, String... args) {
        return TandaCli.run(new TrayCmd(tray, DefaultQuizSentenceConverter.create(), saved), args);
    }

    /** Una publicación de hoy: dos pasos aprobados, tres sin revisar, uno rechazado, con su encadenado. */
    private static BatchPublication publication(String digest) {
        BatchPublication publication = new BatchPublication(Instant.parse("2026-10-11T03:10:44Z"), "José", null,
                "audit-publicado", "plan-publicado", new ArrayList<>(List.of("p-1", "p-2")),
                new ArrayList<>(List.of("t-pista")), new ArrayList<>(List.of("t-come")), new ArrayList<>());
        publication.setManifestDigest(digest);
        publication.setVerdictKind(VerdictKind.PASSES);
        publication.setGatePassed(true);
        publication.setUndecidableFloors(new ArrayList<>());
        publication.setStepsApproved(2);
        publication.setStepsPending(3);
        publication.setStepsRejected(1);
        publication.setChangesLeftOutRejected(new ArrayList<>(List.of("mesa-67fab6d59930102295341ec6")));
        publication.setChangesLeftOutCollateral(new ArrayList<>(List.of("regla-translation-sense-67fab6d59930102295341ec6")));
        return publication;
    }

    private static CorrectionBatch published(BatchPublication publication) {
        CorrectionBatch batch = new CorrectionBatch();
        batch.setId(BATCH);
        batch.setStatus(BatchStatus.PUBLISHED);
        batch.setPublication(publication);
        return batch;
    }

    /** La lista de la tanda: dos documentos, tres campos, y un cambio de tipo de ejercicio sin lugar en producción. */
    private static PublicationManifest manifest() {
        return new PublicationManifest(PublicationManifest.SCHEMA, BATCH, "plan-20260929", "A1",
                "2026-10-11T03:10:44Z", "José", new ManifestSource("audit-20260929", "audit-20260929", null),
                new ManifestVerdict(VerdictKind.PASSES, true, List.of(), List.of(), null),
                new ReviewTally(2, 3, 1, List.of("mesa-67fab6d59930102295341ec6"),
                        List.of("regla-translation-sense-67fab6d59930102295341ec6")),
                List.of(), List.of(new ManifestUnsupported("mesa-67fab6d59930102295341ec9", "67fab6d59930102295341ec9",
                        ExercisePart.FORM_KIND, "cambio de tipo de ejercicio: no se publica por script")),
                List.of(), new ManifestCounts(2, 3, Map.of("form.sentenceParts", 2, "translation", 1)));
    }

    @Test
    @DisplayName("should print with 'tray publish' the verdict and the gate, the counts of the review with the changes left out, where the publication list is with its digest, what it carries and what has no place in production")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R001")
    @Tag("F-PUBPROD-R004")
    @Tag("F-PUBPROD-R005")
    public void shouldPrintWithTrayPublishTheVerdictTheCountsTheLeftOutChangesAndThePublicationList() {
        // R005 inv. 3: la publicación dice con qué veredicto y compuerta se publicó. R004: cuántos pasos aprobó José,
        // cuántos viajaron sin revisar y cuántos rechazó, con los cambios que quedaron afuera por eso y los encadenados.
        // R001: dónde quedó la lista, su huella, cuántos documentos y campos lleva y, aparte, lo que no tiene lugar en
        // producción.
        BatchPublication publication = publication(DIGEST);

        TandaCli.Run run = run(publishing(publication), new Saved(published(publication), manifest()), "publish");

        assertEquals(0, run.exit(), run.err());
        assertEquals("Publicada por José: entraron 2 propuestas; tareas completadas 1, reabiertas 1; audios por regrabar"
                + " 0. Análisis publicado: audit-publicado (plan plan-publicado).\n"
                + "Veredicto de F: pasa; compuerta de «no empeorar»: pasa.\n"
                + "Revisión de Correcciones: 2 pasos aprobados, 3 sin revisar (viajan igual), 1 rechazado (queda"
                + " afuera).\n"
                + "Afuera por rechazo: mesa-67fab6d59930102295341ec6.\n"
                + "Afuera encadenados (tocaban lo mismo en el mismo ejercicio que un rechazado):"
                + " regla-translation-sense-67fab6d59930102295341ec6.\n"
                + "Lista de publicación: " + LOCATION + " (huella sha256 " + DIGEST + ").\n"
                + "En la lista: 2 documentos, 3 campos; sin lugar en producción: 1:\n"
                + "  mesa-67fab6d59930102295341ec9 en 67fab6d59930102295341ec9 (FORM_KIND): cambio de tipo de ejercicio:"
                + " no se publica por script\n"
                + "Llevar el curso a producción y grabar los audios siguen siendo pasos de José; a producción sólo va lo"
                + " que dice la lista.\n", run.out());
    }

    @Test
    @DisplayName("should not name with 'tray publish' a publication list that is not the one of the batch just published, nor counts a batch published before there were lists does not have")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R001")
    public void shouldNotNameAListThatIsNotTheOneOfTheBatchJustPublished() {
        // R001: la lista se nombra sólo si es la de esta tanda —la misma huella—; si no, se dice la huella sola. Una
        // tanda publicada antes de que hubiera lista no muestra cuentas ni lista que no tiene.
        BatchPublication publication = publication(DIGEST);

        TandaCli.Run another = run(publishing(publication), new Saved(published(publication(
                "0000000000000000000000000000000000000000000000000000000000000000")), manifest()), "publish");

        assertEquals(0, another.exit(), another.err());
        assertTrue(another.out().contains("Lista de publicación: (huella sha256 " + DIGEST + ").\n"), another.out());
        assertFalse(another.out().contains(LOCATION), another.out());
        assertFalse(another.out().contains("En la lista:"), another.out());

        BatchPublication old = new BatchPublication(Instant.parse("2026-09-01T10:00:00Z"), "José", null, "audit-viejo",
                "plan-viejo", new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        TandaCli.Run before = run(publishing(old), new Saved(published(old), null), "publish");

        assertEquals(0, before.exit(), before.err());
        assertEquals("Publicada por José: entraron 0 propuestas; tareas completadas 0, reabiertas 0; audios por regrabar"
                + " 0. Análisis publicado: audit-viejo (plan plan-viejo).\n"
                + "Llevar el curso a producción y grabar los audios siguen siendo pasos de José; a producción sólo va lo"
                + " que dice la lista.\n", before.out());
    }
}
