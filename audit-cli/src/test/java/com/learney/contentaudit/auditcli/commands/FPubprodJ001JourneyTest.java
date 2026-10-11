package com.learney.contentaudit.auditcli.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.refinerdomain.RefinementTaskStatus;
import com.learney.contentaudit.revisiondomain.batch.BatchChange;
import com.learney.contentaudit.revisiondomain.batch.BatchReview;
import com.learney.contentaudit.revisiondomain.batch.BatchReviewDecision;
import com.learney.contentaudit.revisiondomain.batch.BatchStatus;
import com.learney.contentaudit.revisiondomain.batch.CorrectionBatch;
import com.learney.contentaudit.revisiondomain.comparison.BatchVerdict;
import com.learney.contentaudit.revisiondomain.comparison.RowSubject;
import com.learney.contentaudit.revisiondomain.comparison.UndecidableFloors;
import com.learney.contentaudit.revisiondomain.comparison.VerdictKind;
import com.learney.contentaudit.revisiondomain.projection.ProjectionScope;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;

/**
 * F-PUBPROD-J001 de punta a punta, desde la línea de comandos y con la tanda, sus decisiones y su lista en archivos:
 * José aprobó unos pasos y rechazó otros en Correcciones (`batch review --decision-json`, como el tablero) y publica la
 * tanda cerrada desde la bandeja (`tray publish`).
 */
@Tag("F-PUBPROD")
@Tag("F-PUBPROD-J001")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FPubprodJ001JourneyTest {

    /** Rutas que la lista nunca lleva (F-PUBPROD-R002 inv. 2). */
    private static final Set<String> NEVER = Set.of("_id", "kind", "form.kind", "form.incidence", "form.label",
            "form.name");

    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir
    Path workdir;

    @Test
    @Order(1)
    @Tag("path-1")
    @DisplayName("path-1: José aprobó unos pasos y rechazó otro... [Un rechazo quedó sobre un contenido que cambió después] → La publicación se niega y nombra el ca... → failure")
    public void path1_unRechazoQuedoSobreUnContenidoQueCambioDespues_failure() {
        // J001, camino 1: José rechazó la mesa del ejercicio de «come» mirando otra versión del ejercicio: su decisión
        // quedó guardada, pero lo que vio ya no es lo de hoy. La publicación se niega nombrando el cambio y el ejercicio;
        // el curso no se toca, no queda lista y el cambio sigue en la tanda hasta que José lo vuelva a mirar.
        TandaCli cli = readyToPublish(workdir);
        String batchId = cli.batches.open().orElseThrow().getId();
        String come = changeId(cli, "mesa-");
        BatchReview.Item item = items(cli, come).get(0);
        cli.batches.saveReview(new BatchReviewDecision(batchId, List.of(new BatchReviewDecision.Selection(come,
                item.step(), item.after(), item.before())), BatchReviewDecision.Status.REJECTED, "José",
                Instant.parse("2026-10-01T14:00:00Z")));

        TandaCli.Run publish = cli.tray("publish");

        assertEquals(1, publish.exit(), publish.out() + publish.err());
        assertTrue(publish.err().contains("José rechazó un contenido que cambió después (" + come + " en "
                + TandaCli.COME + "): hay que volver a mirarlo antes de publicar"), publish.err());
        assertTrue(cli.courses.written.isEmpty(), "el curso no se toca");
        assertFalse(Files.exists(manifestFile(batchId)), "no queda lista");
        CorrectionBatch batch = cli.batches.load(batchId).orElseThrow();
        assertNotEquals(BatchStatus.PUBLISHED, batch.getStatus());
        assertNull(batch.getPublication());
        assertNull(change(batch, come).getDroppedReason(), "sigue en la tanda");
    }

    @Test
    @Order(2)
    @Tag("path-2")
    @DisplayName("path-2: José aprobó unos pasos y rechazó otro... [La compuerta o el veredicto no dan «pasa»] → La publicación se niega y nombra el ve... → failure")
    public void path2_laCompuertaOElVeredictoNoDanPasa_failure() {
        // J001, camino 2: José rechazó la mesa de «come». Sin ella, F ya no da «pasa»: la publicación se niega nombrando
        // el veredicto, no saca nada y no toca el curso. Y si F no puede decidir —un piso sin decidir— tampoco publica,
        // aunque la compuerta de «no empeorar» pase.
        TandaCli cli = readyToPublish(workdir);
        String batchId = cli.batches.open().orElseThrow().getId();
        String come = changeId(cli, "mesa-");
        decide(cli, come, BatchReviewDecision.Status.REJECTED);
        cli.comparison.verdict = scope -> touches(scope, TandaCli.COME) ? passes()
                : new BatchVerdict(VerdictKind.FAILS, new ArrayList<>(), new ArrayList<>(), null,
                        "Falla: sin la mesa vuelve la respuesta única de «come».", null);

        TandaCli.Run withoutTheRejected = cli.tray("publish");

        assertEquals(1, withoutTheRejected.exit(), withoutTheRejected.out() + withoutTheRejected.err());
        assertTrue(withoutTheRejected.err().contains("el veredicto de F es «FAILS», no «pasa»: Falla: sin la mesa"),
                withoutTheRejected.err());
        assertTrue(cli.courses.written.isEmpty(), "el curso no se toca");
        assertFalse(Files.exists(manifestFile(batchId)), "no queda lista");
        CorrectionBatch batch = cli.batches.load(batchId).orElseThrow();
        assertNull(change(batch, come).getDroppedReason(), "no saca nada");
        assertNull(batch.getPublication());

        cli.comparison.verdict = scope -> new BatchVerdict(VerdictKind.UNDECIDABLE, new ArrayList<>(),
                new ArrayList<>(), null, "ningún piso falla, pero 1 piso sin decidir",
                new UndecidableFloors(1, List.of(new RowSubject("lemma-absence", "Palabras del nivel"))));

        TandaCli.Run undecidable = cli.tray("publish");

        assertEquals(1, undecidable.exit(), undecidable.out() + undecidable.err());
        assertTrue(undecidable.err().contains("el veredicto de F es «no se puede decidir» (1 piso sin decidir), no"
                + " «pasa»: ningún piso falla, pero 1 piso sin decidir"), undecidable.err());
        assertTrue(cli.courses.written.isEmpty(), "el curso no se toca");
        assertFalse(Files.exists(manifestFile(batchId)), "no queda lista");
    }

    @Test
    @Order(3)
    @Tag("path-3")
    @DisplayName("path-3: José aprobó unos pasos y rechazó otro... [Los rechazos están vigentes y la tanda pasa] → Salen los cambios rechazados con sus ... [Sigue dando «pasa»] → La tanda deja la lista con el antes y... → success")
    public void path3_losRechazosEstanVigentesYLaTandaPasa_success() throws Exception {
        // J001, camino 3: José aprobó la pista de las verduras y rechazó la mesa de «come». Al publicar, la mesa sale, su
        // tarea vuelve a pendiente y la tanda se compara de nuevo sin ella; sigue dando «pasa», así que deja la lista
        // —cada campo con su antes y su después, las cuentas de la revisión— con su huella en la tanda, y escribe el curso.
        TandaCli cli = readyToPublish(workdir);
        String hint = changeId(cli, "regla-hint-coherence-");
        String come = changeId(cli, "mesa-");
        decide(cli, hint, BatchReviewDecision.Status.APPROVED);
        decide(cli, come, BatchReviewDecision.Status.REJECTED);
        // Sin la mesa, el análisis del curso publicado sigue marcando «come».
        cli.remaining = List.of(TandaCli.teaAnswer(), TandaCli.fruitTie(), TandaCli.comeAnswer());

        TandaCli.Run publish = cli.tray("publish", "--note", "la tanda de A1 sin la mesa");

        assertEquals(0, publish.exit(), publish.out() + publish.err());

        // Sale la mesa rechazada: «come» queda como estaba, su tarea vuelve a pendiente, la pista entra.
        assertEquals(1, cli.courses.written.size(), "el curso se escribe una vez");
        assertEquals("He", exercise(cli, TandaCli.COME).getForm().getSentenceParts().get(0).getText(),
                "sin la mesa");
        assertEquals("(love) a lot of vegetables.", exercise(cli, TandaCli.VEGETABLES).getForm().getSentenceParts()
                .get(2).getText(), "con la pista aprobada");
        assertEquals(RefinementTaskStatus.PENDING, cli.plans.load(TandaCli.PLAN).orElseThrow().getTasks().stream()
                .filter(t -> t.getId().equals("t-come")).findFirst().orElseThrow().getStatus(), "su tarea, pendiente");
        assertTrue(cli.batches.latestPublished().orElseThrow().getPublication().getReopenedTaskIds()
                .contains("t-come"));
        ProjectionScope last = cli.comparison.scopes.get(cli.comparison.scopes.size() - 1);
        assertFalse(touches(last, TandaCli.COME), "comparada de nuevo sin la mesa: " + last.getProposalIds());
        assertTrue(touches(last, TandaCli.VEGETABLES), last.getProposalIds().toString());
        CorrectionBatch published = cli.batches.latestPublished().orElseThrow();
        assertEquals(BatchStatus.PUBLISHED, published.getStatus());
        assertEquals("rechazado al revisar", change(published, come).getDroppedReason());

        // La lista quedó en su archivo, y la tanda guardada registra su huella, el veredicto y la compuerta, con las
        // claves que lee el paso a producción.
        Path manifest = manifestFile(published.getId());
        byte[] bytes = Files.readAllBytes(manifest);
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        JsonNode saved = JSON.readTree(workdir.resolve(".content-audit/batches").resolve(published.getId() + ".json")
                .toFile());
        assertEquals("PUBLISHED", saved.path("status").asText());
        assertEquals(TandaCli.PLAN, saved.path("planId").asText());
        JsonNode publication = saved.path("publication");
        assertEquals(digest, publication.path("manifestDigest").asText(), "la huella de los bytes de la lista");
        assertEquals("PASSES", publication.path("verdictKind").asText());
        assertTrue(publication.path("gatePassed").asBoolean());
        assertTrue(publication.path("undecidableFloors").isArray() && publication.path("undecidableFloors").isEmpty(),
                publication.toString());

        JsonNode list = JSON.readTree(bytes);
        assertEquals(1, list.path("schema").asInt());
        assertEquals(published.getId(), list.path("batchId").asText());
        assertEquals("PASSES", list.path("verdict").path("kind").asText());
        assertTrue(list.path("verdict").path("gatePassed").asBoolean());
        assertTrue(list.path("verdict").path("undecidableFloors").isEmpty());
        assertEquals(1, list.path("reviews").path("approved").asInt());
        assertEquals(0, list.path("reviews").path("pending").asInt());
        assertEquals(1, list.path("reviews").path("rejected").asInt());
        assertEquals(come, list.path("reviews").path("droppedForReviewChangeIds").path(0).asText());
        assertEquals(1, list.path("excluded").size(), list.path("excluded").toString());
        assertEquals(come, list.path("excluded").path(0).path("changeId").asText());
        assertEquals("REVIEW_REJECTED", list.path("excluded").path(0).path("reason").asText());

        // Un solo ejercicio cambió: las verduras, con todo lo suyo aprobado; lo que no cambió no está.
        assertEquals(1, list.path("documents").size(), list.path("documents").toString());
        JsonNode vegetables = list.path("documents").path(0);
        assertEquals("quizTemplates", vegetables.path("collection").asText());
        assertEquals(TandaCli.VEGETABLES, vegetables.path("id").asText());
        assertEquals("APPROVED", vegetables.path("reviewStatus").asText());
        assertEquals(hint, vegetables.path("changeIds").path(0).asText());
        JsonNode parts = null;
        int fields = 0;
        for (JsonNode field : vegetables.path("fields")) {
            fields++;
            assertFalse(NEVER.contains(field.path("path").asText()), "nunca " + field.path("path").asText());
            assertNotEquals(field.path("before"), field.path("after"), "sólo lo que cambió: " + field);
            if (field.path("path").asText().equals("form.sentenceParts")) {
                parts = field;
            }
        }
        assertTrue(parts != null, "las partes del ejercicio: " + vegetables);
        assertEquals("a lot of vegetables.", parts.path("before").path(2).path("text").asText());
        assertEquals("(love) a lot of vegetables.", parts.path("after").path(2).path("text").asText(),
                "el después es lo del curso publicado");
        assertEquals(1, list.path("counts").path("documents").asInt());
        assertEquals(fields, list.path("counts").path("fields").asInt());
        assertTrue(list.path("unsupported").isEmpty());

        // Lo que José lee al publicar.
        String out = publish.out();
        assertTrue(out.startsWith("Publicada por José: entraron 1 propuestas"), out);
        assertTrue(out.contains("Veredicto de F: pasa; compuerta de «no empeorar»: pasa.\n"), out);
        assertTrue(out.contains("Revisión de Correcciones: 1 paso aprobado, 0 sin revisar (viajan igual), 1 rechazado"
                + " (queda afuera).\n"), out);
        assertTrue(out.contains("Afuera por rechazo: " + come + ".\n"), out);
        assertTrue(out.contains("Lista de publicación: " + manifest + " (huella sha256 " + digest + ").\n"), out);
        assertTrue(out.contains("En la lista: 1 documento, " + fields + (fields == 1 ? " campo" : " campos")
                + "; sin lugar en producción: 0.\n"), out);
        assertTrue(out.contains("a producción sólo va lo que dice la lista."), out);
    }

    /**
     * La tanda de A1 abierta, corrida con tope y cerrada desde la línea de comandos, y la bandeja lista para el OK: la
     * muestra ciega contestada y la muestra de la regla de la pista aprobada.
     */
    private static TandaCli readyToPublish(Path workdir) {
        TandaCli cli = new TandaCli(workdir);
        assertEquals(0, cli.batch("open", "A1").exit());
        assertEquals(0, cli.batch("run", "--cap", "10").exit());
        TandaCli.Run close = cli.batch("close");
        assertEquals(0, close.exit(), close.out() + close.err());
        assertEquals(0, cli.tray("answer", "ciega-" + TandaCli.SAMPLE, "A", "--pair", "1").exit());
        assertEquals(0, cli.tray("answer", "ciega-" + TandaCli.SAMPLE, "B", "--pair", "2").exit());
        assertEquals(0, cli.tray("answer", "muestra-hint-coherence", "RULE_OK").exit());
        return cli;
    }

    /** Lo que Correcciones lee de la tanda: `batch review`, como lo lee el tablero. */
    private static List<BatchReview.Item> items(TandaCli cli, String changeId) {
        TandaCli.Run run = cli.batch("review");
        assertEquals(0, run.exit(), run.err());
        List<BatchReview.Item> items = CommandJson.read(run.out().trim(), BatchReview.class).items().stream()
                .filter(item -> item.changeId().equals(changeId)).toList();
        assertFalse(items.isEmpty(), "ningún paso de " + changeId + " para decidir");
        return items;
    }

    /** José decide en Correcciones todos los pasos de un cambio, sobre el contenido que lee hoy. */
    private static void decide(TandaCli cli, String changeId, BatchReviewDecision.Status status) {
        List<BatchReviewDecision.Selection> selections = items(cli, changeId).stream()
                .map(item -> new BatchReviewDecision.Selection(item.changeId(), item.step(), item.before(),
                        item.after())).toList();
        String batchId = cli.batches.open().orElseThrow().getId();
        TandaCli.Run run = cli.batch("review", "--decision-json", CommandJson.write(new BatchReviewDecision(batchId,
                selections, status, "José", null)));
        assertEquals(0, run.exit(), run.out() + run.err());
    }

    private static String changeId(TandaCli cli, String prefix) {
        return cli.batches.open().orElseThrow().getChanges().stream().map(BatchChange::getChangeId)
                .filter(id -> id.startsWith(prefix)).findFirst().orElseThrow();
    }

    private static BatchChange change(CorrectionBatch batch, String changeId) {
        return batch.getChanges().stream().filter(change -> change.getChangeId().equals(changeId)).findFirst()
                .orElseThrow();
    }

    private static boolean touches(ProjectionScope scope, String quizId) {
        return scope.getProposalIds().stream().anyMatch(id -> id.endsWith(quizId));
    }

    private static BatchVerdict passes() {
        return new BatchVerdict(VerdictKind.PASSES, new ArrayList<>(), new ArrayList<>(), null,
                "Pasa: se cumplen los pisos y no vuelve ningún cambio.", null);
    }

    private Path manifestFile(String batchId) {
        return workdir.resolve(".content-audit/batch-publications").resolve(batchId).resolve("manifest.json");
    }

    private static QuizTemplateEntity exercise(TandaCli cli, String quizId) {
        return cli.courses.written.get(cli.courses.written.size() - 1).getRoot().getMilestones().get(0).getTopics()
                .get(0).getKnowledges().get(0).getQuizTemplates().stream().filter(q -> q.getId().equals(quizId))
                .findFirst().orElseThrow();
    }
}
