package com.learney.contentaudit.auditinfrastructure;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditReportSummary;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.refinerdomain.RefinementPlan;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * F-PUBPROD-R010 inv. 4: el análisis y el plan del curso publicado que guardó un intento de publicar que no terminó se
 * borran del disco, para que el último análisis y el último plan vuelvan a ser los de antes; lo que ya estaba guardado
 * no se borra ni queda pisado.
 */
public class DiscardingWhatAnUnfinishedPublicationSavedTest {

    private static final DateTimeFormatter ID_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss")
            .withZone(ZoneOffset.UTC);

    @TempDir
    Path workdir;

    private Path audits() {
        return workdir.resolve(".content-audit/audits");
    }

    private Path digests() {
        return workdir.resolve(".content-audit/audit-digests");
    }

    private Path plans() {
        return workdir.resolve(".content-audit/plans");
    }

    private Path reportFile(String id) {
        return audits().resolve("audit-" + id + ".json");
    }

    private Path digestFile(String id) {
        return digests().resolve("audit-" + id + ".json");
    }

    private Path planFile(String id) {
        return plans().resolve("plan-" + id + ".json");
    }

    private static AuditReport report(double score) {
        return new AuditReport(new AuditNode(null, AuditTarget.COURSE, null, List.of(),
                Map.of("sentence-length", score), Map.of(), null));
    }

    private static double score(AuditReport report) {
        return report.getRoot().getScores().get("sentence-length");
    }

    private static RefinementPlan plan(String id) {
        return new RefinementPlan(id, "audit-" + id, Instant.parse("2026-10-11T12:00:00Z"), new ArrayList<>());
    }

    private static List<String> names(Path dir) throws Exception {
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(f -> f.getFileName().toString()).sorted().toList();
        }
    }

    @Test
    @DisplayName("should delete the report and the digest of the discarded analysis, so that neither the history nor the latest analysis find it, and leave the analysis of before as it was")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R010")
    public void shouldDeleteEverythingOfTheDiscardedAnalysisAndLeaveTheOneOfBefore() throws Exception {
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(workdir, null);
        String before = store.save(report(0.5));
        String attempt = store.save(report(0.9));
        assertNotEquals(before, attempt, "dos guardados nunca comparten id, aunque caigan en el mismo segundo");
        assertEquals(0.9, score(store.loadLatest().orElseThrow()), 1e-9);
        byte[] reportBefore = Files.readAllBytes(reportFile(before));
        byte[] digestBefore = Files.readAllBytes(digestFile(before));

        store.discard(attempt);

        assertFalse(Files.exists(reportFile(attempt)), "el análisis del intento se borra");
        assertFalse(Files.exists(digestFile(attempt)), "y su resumen");
        assertTrue(store.load(attempt).isEmpty());
        assertTrue(store.loadDigest(attempt).isEmpty());
        assertEquals(List.of(before), store.list().stream().map(AuditReportSummary::getId).toList(),
                "el historial no lo lista");
        assertEquals(0.5, score(store.loadLatest().orElseThrow()), 1e-9, "el último análisis vuelve a ser el de antes");
        assertArrayEquals(reportBefore, Files.readAllBytes(reportFile(before)), "el de antes queda igual");
        assertArrayEquals(digestBefore, Files.readAllBytes(digestFile(before)));
        assertDoesNotThrow(() -> store.discard(attempt), "descartar lo que ya no está no hace nada");
        assertDoesNotThrow(() -> store.discard("2020-01-01T00-00-00"), "ni lo que nunca estuvo");
        assertThrows(IllegalArgumentException.class, () -> store.discard("../afuera"));
        assertEquals(List.of("audit-" + before + ".json"), names(audits()));
        assertEquals(List.of("audit-" + before + ".json"), names(digests()));
    }

    @Test
    @DisplayName("should give a save an id no analysis of before has, neither as a report nor as a digest, so that discarding it never deletes nor overwrites one")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R010")
    public void shouldNeverGiveASaveTheIdOfAnAnalysisOfBefore() throws Exception {
        // Ocupa los once segundos que vienen: los pares con el análisis y su resumen, los impares sólo con el resumen.
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(workdir, null);
        String first = store.save(report(0.5));
        Instant from = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Map<Path, byte[]> occupied = new LinkedHashMap<>();
        List<String> occupiedIds = new ArrayList<>(List.of(first));
        occupied.put(reportFile(first), Files.readAllBytes(reportFile(first)));
        occupied.put(digestFile(first), Files.readAllBytes(digestFile(first)));
        for (int second = 0; second <= 10; second++) {
            String id = ID_FORMAT.format(from.plusSeconds(second));
            if (occupiedIds.contains(id)) {
                continue;
            }
            occupiedIds.add(id);
            Files.copy(digestFile(first), digestFile(id));
            occupied.put(digestFile(id), Files.readAllBytes(digestFile(id)));
            if (second % 2 == 0) {
                Files.copy(reportFile(first), reportFile(id));
                occupied.put(reportFile(id), Files.readAllBytes(reportFile(id)));
            }
        }

        String attempt = store.save(report(0.9));

        assertFalse(occupiedIds.contains(attempt), attempt + " ya estaba ocupado");
        assertEquals(0.9, score(store.load(attempt).orElseThrow()), 1e-9);
        store.discard(attempt);
        for (Map.Entry<Path, byte[]> file : occupied.entrySet()) {
            assertArrayEquals(file.getValue(), Files.readAllBytes(file.getKey()), file.getKey() + " no se pisa ni se borra");
        }
    }

    @Test
    @DisplayName("should leave nothing of a save that fails halfway, so that the latest analysis is still the one of before")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R010")
    public void shouldLeaveNothingOfASaveThatFailsHalfway() throws Exception {
        // El análisis se escribe y su resumen no: donde va la carpeta de resúmenes hay un archivo.
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(workdir, null);
        String before = store.save(report(0.5));
        Path digestsDir = digests();
        Path keptAside = workdir.resolve("resúmenes-aparte");
        Files.move(digestsDir, keptAside);
        Files.writeString(digestsDir, "no es una carpeta");

        assertThrows(AuditPersistenceException.class, () -> store.save(report(0.9)));

        assertEquals(List.of("audit-" + before + ".json"), names(audits()), "del guardado fallido no queda nada");
        assertEquals(0.5, score(store.loadLatest().orElseThrow()), 1e-9, "el último análisis sigue siendo el de antes");
    }

    @Test
    @DisplayName("should say which analysis could not be discarded and which file stayed when its report cannot be deleted")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R010")
    public void shouldSayWhichAnalysisAndFileStayedWhenItCannotBeDeleted() throws Exception {
        FileSystemAuditReportStore store = new FileSystemAuditReportStore(workdir, null);
        String id = "2026-10-11T12-00-00";
        Files.createDirectories(reportFile(id));
        Files.writeString(reportFile(id).resolve("adentro.txt"), "algo");

        AuditPersistenceException failed = assertThrows(AuditPersistenceException.class, () -> store.discard(id));

        assertTrue(failed.getMessage().startsWith("No se pudo descartar el análisis " + id + ": no se pudo borrar "
                + reportFile(id)), failed.getMessage());
    }

    @Test
    @DisplayName("should delete the file of the discarded plan, so that the latest plan is the one of before again, and leave the plan of before as it was")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R010")
    public void shouldDeleteTheDiscardedPlanAndLeaveTheOneOfBefore() throws Exception {
        FileSystemRefinementPlanStore store = new FileSystemRefinementPlanStore(workdir);
        String before = store.save(plan("2026-09-29T12-00-00"));
        String attempt = store.save(plan("2026-10-11T12-00-00"));
        assertEquals(attempt, store.loadLatest().orElseThrow().getId());
        byte[] planBefore = Files.readAllBytes(planFile(before));

        store.discard(attempt);

        assertFalse(Files.exists(planFile(attempt)), "el plan del intento se borra");
        assertTrue(store.load(attempt).isEmpty());
        assertEquals(before, store.loadLatest().orElseThrow().getId(), "el último plan vuelve a ser el de antes");
        assertArrayEquals(planBefore, Files.readAllBytes(planFile(before)), "el de antes queda igual");
        assertEquals(List.of("plan-" + before + ".json"), names(plans()));
        assertDoesNotThrow(() -> store.discard(attempt), "descartar lo que ya no está no hace nada");
        assertThrows(IllegalArgumentException.class, () -> store.discard("../afuera"));

        Files.createDirectories(planFile("2026-10-12T12-00-00"));
        Files.writeString(planFile("2026-10-12T12-00-00").resolve("adentro.txt"), "algo");
        AuditPersistenceException failed = assertThrows(AuditPersistenceException.class,
                () -> store.discard("2026-10-12T12-00-00"));
        assertTrue(failed.getMessage().startsWith("No se pudo descartar el plan 2026-10-12T12-00-00: no se pudo borrar "
                + planFile("2026-10-12T12-00-00")), failed.getMessage());
    }
}
