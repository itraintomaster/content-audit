package com.learney.contentaudit.revisiondomain.batchengine;

import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.cloze;
import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.text;
import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.withParts;
import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.withTranslation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.auditdomain.ActiveAnalysisSelection;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.refinerdomain.RefinementTaskStatus;
import com.learney.contentaudit.revisiondomain.RevisionArtifact;
import com.learney.contentaudit.revisiondomain.RevisionVerdict;
import com.learney.contentaudit.revisiondomain.batch.BatchAdjustment;
import com.learney.contentaudit.revisiondomain.batch.BatchAdjustmentKind;
import com.learney.contentaudit.revisiondomain.batch.BatchChange;
import com.learney.contentaudit.revisiondomain.batch.BatchChangeOrigin;
import com.learney.contentaudit.revisiondomain.batch.BatchNotPublishableException;
import com.learney.contentaudit.revisiondomain.batch.BatchProposalRef;
import com.learney.contentaudit.revisiondomain.batch.BatchPublication;
import com.learney.contentaudit.revisiondomain.batch.BatchPublishRequest;
import com.learney.contentaudit.revisiondomain.batch.BatchReviewDecision;
import com.learney.contentaudit.revisiondomain.batch.BatchStatus;
import com.learney.contentaudit.revisiondomain.batch.BatchTaskOutcomeKind;
import com.learney.contentaudit.revisiondomain.batch.CorrectionBatch;
import com.learney.contentaudit.revisiondomain.comparison.BatchComparison;
import com.learney.contentaudit.revisiondomain.comparison.BatchVerdict;
import com.learney.contentaudit.revisiondomain.comparison.RowSubject;
import com.learney.contentaudit.revisiondomain.comparison.UndecidableFloors;
import com.learney.contentaudit.revisiondomain.comparison.VerdictKind;
import com.learney.contentaudit.revisiondomain.nonregression.BatchGateVerdict;
import com.learney.contentaudit.revisiondomain.panelengine.MesaFixture;
import com.learney.contentaudit.revisiondomain.publication.DocumentReviewStatus;
import com.learney.contentaudit.revisiondomain.publication.ExclusionReason;
import com.learney.contentaudit.revisiondomain.publication.ManifestDocument;
import com.learney.contentaudit.revisiondomain.publication.ManifestExcluded;
import com.learney.contentaudit.revisiondomain.publication.ManifestField;
import com.learney.contentaudit.revisiondomain.publication.PublicationManifest;
import com.learney.contentaudit.revisiondomain.publication.ReviewTally;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * F-PUBPROD-R001, R003, R004 y R005, con F-TANDA-R008 y R009: publicar una tanda que José revisó en Correcciones. Lo que
 * rechazó sale entero, con lo que va con él; lo demás viaja y se cuenta; sólo «pasa» publica; y la lista de lo que puede
 * llegar a producción queda guardada, con su huella, antes de escribir el curso.
 */
public class PublishingReviewedBatchTest {

    /** El panel saca «a lot of» de las verduras. */
    private static final String V1 = "mesa-" + MesaFixture.VEGETABLES;

    /** Una regla que, después, vuelve a tocar el enunciado de las verduras. */
    private static final String V2 = "regla-single-answer-" + MesaFixture.VEGETABLES;

    /** Una regla que, después, toca sólo la traducción de las verduras. */
    private static final String T = "regla-translation-sense-" + MesaFixture.VEGETABLES;

    /** El panel corrige a la abuela. */
    private static final String C = "mesa-" + MesaFixture.COME;

    private static final Instant REVIEWED_AT = TandaFixture.OPENED_AT.plusSeconds(600);

    private static BatchPublishRequest ok() {
        return new BatchPublishRequest("José", null, List.of());
    }

    private static QuizTemplateEntity loved(Publishing p) {
        return withParts(TandaFixture.exercise(p.start, MesaFixture.VEGETABLES), text("I"), cloze("love"),
                text("vegetables."));
    }

    private static QuizTemplateEntity liked(Publishing p) {
        return withParts(TandaFixture.exercise(p.start, MesaFixture.VEGETABLES), text("I"), cloze("like"),
                text("vegetables."));
    }

    private static QuizTemplateEntity translated(Publishing p) {
        return withTranslation(TandaFixture.exercise(p.start, MesaFixture.VEGETABLES), "Me encantan las verduras.");
    }

    private static QuizTemplateEntity comes(Publishing p) {
        return withParts(TandaFixture.exercise(p.start, MesaFixture.COME), text("My grandmother"), cloze("comes"),
                text("(come) every year."));
    }

    private static void add(Publishing p, CorrectionBatch batch, String changeId, BatchChangeOrigin origin,
            String ruleId, String taskId, QuizTemplateEntity after, ExercisePart part, BatchTaskOutcomeKind kind) {
        p.record(batch, TandaFixture.change(changeId, origin, ruleId, List.of(taskId), TandaFixture.step(after, part)));
        BatchBook.setOutcome(batch, taskId, kind, changeId, 1, null);
    }

    /**
     * Una tanda de A1 con tres cambios: las verduras sin «a lot of» (V1), una regla que después vuelve a tocar su
     * enunciado (V2) y la abuela (C). Todavía sin cerrar.
     */
    private static CorrectionBatch threeChanges(Publishing p) {
        CorrectionBatch batch = p.batch("t-respuesta", "t-traduccion", "t-come");
        add(p, batch, V1, BatchChangeOrigin.PANEL, null, "t-respuesta", loved(p), ExercisePart.STATEMENT,
                BatchTaskOutcomeKind.RESOLVED_BY_PANEL);
        add(p, batch, V2, BatchChangeOrigin.RULE, "single-answer", "t-traduccion", liked(p), ExercisePart.STATEMENT,
                BatchTaskOutcomeKind.RESOLVED_BY_RULE);
        add(p, batch, C, BatchChangeOrigin.PANEL, null, "t-come", comes(p), ExercisePart.STATEMENT,
                BatchTaskOutcomeKind.RESOLVED_BY_PANEL);
        return batch;
    }

    /** La misma tanda, con V1 rechazado y C aprobado por José, y cerrada con «pasa». */
    private static CorrectionBatch reviewedAndClosed(Publishing p) {
        CorrectionBatch batch = threeChanges(p);
        p.decide(batch, V1, BatchReviewDecision.Status.REJECTED, REVIEWED_AT);
        p.decide(batch, C, BatchReviewDecision.Status.APPROVED, REVIEWED_AT);
        p.close(batch);
        return batch;
    }

    private static BatchChange change(CorrectionBatch batch, String changeId) {
        return batch.getChanges().stream().filter(c -> changeId.equals(c.getChangeId())).findFirst()
                .orElseThrow(() -> new AssertionError("no change " + changeId));
    }

    private static RefinementTaskStatus status(Publishing p, String taskId) {
        return p.plans.load(TandaFixture.PLAN).orElseThrow().getTasks().stream()
                .filter(t -> t.getId().equals(taskId)).findFirst().orElseThrow().getStatus();
    }

    private static List<String> proposalIds(CorrectionBatch batch) {
        return batch.getQuizProposals().stream().map(BatchProposalRef::getProposalId).toList();
    }

    private static PublicationManifest manifest(Publishing p, CorrectionBatch batch) {
        return p.batches.publicationManifest(batch.getId()).orElseThrow(() -> new AssertionError("no list"));
    }

    private static ManifestDocument document(PublicationManifest list, String nodeId) {
        return list.documents().stream().filter(d -> nodeId.equals(d.nodeId())).findFirst()
                .orElseThrow(() -> new AssertionError("no document of " + nodeId + " in the list"));
    }

    private static String json(Object value) {
        try {
            return new ObjectMapper().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new AssertionError(e);
        }
    }

    /** Lo que la tanda era antes de publicar: qué cambios están afuera, sus propuestas, sus tareas y sus artefactos. */
    private static String photo(Publishing p, CorrectionBatch batch) {
        List<String> rows = new ArrayList<>();
        for (BatchChange change : batch.getChanges()) {
            rows.add(change.getChangeId() + "=" + change.getDroppedReason());
        }
        rows.add("propuestas=" + proposalIds(batch));
        batch.getOutcomes().forEach(o -> rows.add(o.getTaskId() + "=" + o.getKind()));
        for (BatchProposalRef ref : batch.getQuizProposals()) {
            RevisionArtifact artifact = p.artifacts.load(TandaFixture.PLAN, ref.getProposalId()).orElseThrow();
            rows.add(ref.getProposalId() + "=" + artifact.getVerdict() + "/" + json(artifact.getProposal()
                    .getElementAfter()));
        }
        return String.join("\n", rows);
    }

    // -------------------------------------------------------------------------
    // F-PUBPROD-R003: lo que José rechazó no se publica
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should leave out whole every change José rejected, with the later changes of the same exercise that touched the same, put their tasks back to pending, compare again and publish the rest with «pasa»")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R003")
    @Tag("FEAT-TANDA")
    @Tag("F-TANDA-R008")
    @Tag("F-TANDA-R009")
    public void shouldLeaveOutWholeEveryChangeJoseRejectedWithItsChainedChangesAndPublishTheRestComparedAgain() {
        // R003 inv. 1 a 3: José rechazó las verduras del panel (V1) y aprobó la abuela (C). Al publicar, V1 sale
        // como si José lo sacara (F-TANDA-R008): con él sale V2, que después tocó el mismo enunciado, y las tareas de
        // los dos vuelven a pendiente. Como salió algo, la tanda se compara de nuevo, sin las verduras, y publica con
        // «pasa»: el curso escrito tiene la abuela corregida y las verduras como estaban.
        Publishing p = new Publishing();
        p.remaining = List.of(MesaFixture.anotherAnswer(), MesaFixture.translationDiffers());
        CorrectionBatch batch = reviewedAndClosed(p);

        CorrectionBatch published = p.publisher().publish(batch, ok(), p.start);

        assertSame(batch, published);
        assertEquals(BatchStatus.PUBLISHED, batch.getStatus());
        assertEquals(DefaultBatchPublisher.REJECTED_IN_REVIEW, change(batch, V1).getDroppedReason());
        assertTrue(change(batch, V2).getDroppedReason().startsWith("sale con " + V1 + ":"),
                change(batch, V2).getDroppedReason());
        assertNull(change(batch, C).getDroppedReason(), "lo aprobado viaja");
        assertEquals(BatchTaskOutcomeKind.PENDING, BatchBook.kind(batch, "t-respuesta"));
        assertEquals(BatchTaskOutcomeKind.PENDING, BatchBook.kind(batch, "t-traduccion"));
        assertEquals(RefinementTaskStatus.PENDING, status(p, "t-respuesta"), "su hallazgo sigue");
        assertEquals(RefinementTaskStatus.PENDING, status(p, "t-traduccion"));
        assertEquals(RefinementTaskStatus.COMPLETED, status(p, "t-come"));

        assertEquals(1, p.comparator.scopes.size(), "se comparó de nuevo, una vez");
        assertEquals(List.of(Publishing.P_COME), p.comparator.scopes.get(0).getProposalIds(), "sin las verduras");
        assertEquals(1, p.courses.written.size());
        CourseEntity written = p.courses.written.get(0);
        assertEquals(json(TandaFixture.exercise(p.start, MesaFixture.VEGETABLES)),
                json(TandaFixture.exercise(written, MesaFixture.VEGETABLES)), "las verduras, como estaban");
        assertEquals(comes(p).getSentences(), TandaFixture.exercise(written, MesaFixture.COME).getSentences());
        assertEquals(RevisionVerdict.REJECTED, p.artifacts.load(TandaFixture.PLAN, Publishing.P_VEGETABLES)
                .orElseThrow().getVerdict(), "la propuesta de las verduras quedó rechazada");

        // R003 inv. 2: la publicación y la lista nombran el rechazado y el que salió con él.
        BatchPublication publication = batch.getPublication();
        assertEquals(List.of(Publishing.P_COME), publication.getAppliedProposalIds());
        assertEquals(List.of(V1), publication.getChangesLeftOutRejected());
        assertEquals(List.of(V2), publication.getChangesLeftOutChained());
        assertEquals(1, publication.getStepsRejected());
        assertEquals(1, publication.getStepsApproved());
        assertEquals(0, publication.getStepsUnreviewed());
        PublicationManifest list = manifest(p, batch);
        assertEquals(List.of(new ManifestExcluded(V1, ExclusionReason.REVIEW_REJECTED),
                new ManifestExcluded(V2, ExclusionReason.COLLATERAL)), list.excluded());
        assertEquals(new ReviewTally(1, 0, 1, List.of(V1), List.of(V2)), list.reviews());
        assertEquals(1, list.documents().size(), "sólo la abuela: las verduras no cambian");
        ManifestDocument come = list.documents().get(0);
        assertEquals(MesaFixture.COME, come.nodeId());
        assertEquals(List.of(C), come.changeIds());
        assertEquals(List.of(Publishing.P_COME), come.proposalIds());
        assertEquals(DocumentReviewStatus.APPROVED, come.reviewStatus());
    }

    @Test
    @DisplayName("should refuse, leaving nothing out, when the batch compared again without what José rejected no longer gives «pasa»")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R003")
    @Tag("F-PUBPROD-R005")
    public void shouldRefuseLeavingNothingOutWhenTheBatchComparedAgainWithoutTheRejectedNoLongerGivesPasa() {
        // R003 inv. 3 y 6: sin las verduras, F ya no da «pasa». La publicación se niega nombrando el veredicto, el curso
        // no se toca, no queda lista y la tanda queda como estaba: V1 y V2 adentro, sus tareas resueltas y la propuesta
        // de las verduras pendiente de aprobación, con su después.
        Publishing p = new Publishing();
        CorrectionBatch batch = reviewedAndClosed(p);
        String before = photo(p, batch);
        p.comparator.answer = scope -> TandaFixture.comparison(scope, List.of(), VerdictKind.FAILS);

        BatchNotPublishableException refused = assertThrows(BatchNotPublishableException.class,
                () -> p.publisher().publish(batch, ok(), p.start));

        assertTrue(refused.getMessage().contains("el veredicto de F es «FAILS», no «pasa»"), refused.getMessage());
        assertEquals(1, p.comparator.scopes.size(), "se comparó sin las verduras");
        assertTrue(p.courses.written.isEmpty(), "el curso no se toca");
        assertTrue(p.batches.manifests.isEmpty(), "no queda lista");
        assertEquals(before, photo(p, batch), "nada queda sacado");
        assertNull(change(batch, V1).getDroppedReason());
        assertEquals(RevisionVerdict.PENDING_APPROVAL, p.artifacts.load(TandaFixture.PLAN, Publishing.P_VEGETABLES)
                .orElseThrow().getVerdict());
        assertEquals(BatchStatus.CLOSED, batch.getStatus());
        assertNull(batch.getPublication());
    }

    @Test
    @DisplayName("should refuse naming the change and the exercise when José rejected a content that changed afterwards")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R003")
    @Tag("FEAT-TANDA")
    @Tag("F-TANDA-R007")
    public void shouldRefuseNamingTheChangeAndTheExerciseWhenJoseRejectedAContentThatChangedAfterwards() {
        // R003 inv. 4: José rechazó la traducción de las verduras (T) cuando venía después del enunciado nuevo (V1).
        // Después sacó V1: el antes de T ya no es el que José vio, y su rechazo no coincide. Publicar se niega y lo
        // nombra, hasta que José lo vuelva a mirar; T no sale por un rechazo que ya no es de este contenido.
        Publishing p = new Publishing();
        CorrectionBatch batch = p.batch("t-respuesta", "t-traduccion");
        add(p, batch, V1, BatchChangeOrigin.PANEL, null, "t-respuesta", loved(p), ExercisePart.STATEMENT,
                BatchTaskOutcomeKind.RESOLVED_BY_PANEL);
        add(p, batch, T, BatchChangeOrigin.RULE, "translation-sense", "t-traduccion", translated(p),
                ExercisePart.TRANSLATION, BatchTaskOutcomeKind.RESOLVED_BY_RULE);
        p.decide(batch, T, BatchReviewDecision.Status.REJECTED, REVIEWED_AT);
        p.ledger().drop(batch, V1, "lo sacó José", p.start);
        p.close(batch);

        BatchNotPublishableException refused = assertThrows(BatchNotPublishableException.class,
                () -> p.publisher().publish(batch, ok(), p.start));

        assertTrue(refused.getMessage().contains("José rechazó un contenido que cambió después (" + T + " en "
                + MesaFixture.VEGETABLES + ")"), refused.getMessage());
        assertTrue(refused.getMessage().contains("hay que volver a mirarlo antes de publicar"), refused.getMessage());
        assertNull(change(batch, T).getDroppedReason(), "T sigue en la tanda");
        assertTrue(p.courses.written.isEmpty());
        assertTrue(p.batches.manifests.isEmpty());
        assertTrue(p.comparator.scopes.isEmpty(), "no llega a comparar");

        // Con el rechazo dado sobre el contenido de hoy, T sale y la tanda publica.
        p.decide(batch, T, BatchReviewDecision.Status.REJECTED, REVIEWED_AT.plusSeconds(60));
        p.publisher().publish(batch, ok(), p.start);
        assertEquals(DefaultBatchPublisher.REJECTED_IN_REVIEW, change(batch, T).getDroppedReason());
        assertEquals(BatchStatus.PUBLISHED, batch.getStatus());
    }

    @Test
    @DisplayName("should refuse to publish, writing nothing, when José's decisions cannot be read")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R003")
    public void shouldRefuseToPublishWritingNothingWhenJoseDecisionsCannotBeRead() {
        // R003 inv. 5: sin las decisiones de José no se sabe qué rechazó: no se publica, ni con un error al leerlas ni
        // sin respuesta.
        for (java.util.function.Function<CorrectionBatch, ReviewStanding> unreadable : List.<java.util.function.Function<
                CorrectionBatch, ReviewStanding>>of(b -> {
                    throw new IllegalStateException("el registro de decisiones está roto");
                }, b -> null)) {
            Publishing p = new Publishing();
            p.reviews = unreadable;
            CorrectionBatch batch = p.closedBatch();
            String before = photo(p, batch);

            BatchNotPublishableException refused = assertThrows(BatchNotPublishableException.class,
                    () -> p.publisher().publish(batch, ok(), p.start));

            assertTrue(refused.getMessage().contains("no se pudieron leer las decisiones de la revisión"),
                    refused.getMessage());
            assertTrue(refused.getMessage().contains("sin ellas no se publica"), refused.getMessage());
            assertTrue(p.courses.written.isEmpty());
            assertTrue(p.batches.manifests.isEmpty());
            assertEquals(before, photo(p, batch));
        }
        Publishing unwired = new Publishing();
        CorrectionBatch batch = unwired.closedBatch();
        BatchNotPublishableException refused = assertThrows(BatchNotPublishableException.class,
                () -> unwired.publisher().withReviews(null).publish(batch, ok(), unwired.start));
        assertTrue(refused.getMessage().contains("no hay de dónde leer las decisiones de la revisión"),
                refused.getMessage());
        assertTrue(unwired.courses.written.isEmpty());
    }

    @Test
    @DisplayName("should leave nothing out when the publication does not end: neither when the list cannot be saved nor when the course cannot be written")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R003")
    @Tag("F-PUBPROD-R001")
    public void shouldLeaveNothingOutWhenThePublicationDoesNotEnd() {
        // R003 inv. 6 y R001 inv. 3: la lista no se puede guardar. No se publica, el curso no se toca y lo que
        // publicar ya había sacado —V1 rechazado, V2 con él— vuelve a estar como estaba, con sus propuestas.
        Publishing full = new Publishing();
        full.batches = new TandaFixture.Batches() {
            @Override
            public synchronized String savePublicationManifest(String batchId, PublicationManifest manifest) {
                throw new IllegalStateException("el disco está lleno");
            }
        };
        CorrectionBatch batch = reviewedAndClosed(full);
        String before = photo(full, batch);

        BatchNotPublishableException refused = assertThrows(BatchNotPublishableException.class,
                () -> full.publisher().publish(batch, ok(), full.start));

        assertTrue(refused.getMessage().contains("la lista de publicación no se pudo guardar: el disco está lleno"),
                refused.getMessage());
        assertTrue(full.courses.written.isEmpty(), "el curso no se toca");
        assertEquals(before, photo(full, batch), "nada queda sacado");
        assertEquals(RevisionVerdict.PENDING_APPROVAL, full.artifacts.load(TandaFixture.PLAN, Publishing.P_VEGETABLES)
                .orElseThrow().getVerdict());
        assertNull(batch.getPublication());

        // Escribir el curso falla: el error sube tal cual, el curso se vuelve a escribir como estaba (pudo quedar a
        // medio escribir), la lista de ese intento se borra y la tanda queda como estaba (R010).
        Publishing broken = new Publishing();
        broken.courses.failure = new IllegalStateException("no se pudo escribir el curso");
        CorrectionBatch other = reviewedAndClosed(broken);
        String otherBefore = photo(broken, other);

        IllegalStateException failed = assertThrows(IllegalStateException.class,
                () -> broken.publisher().publish(other, ok(), broken.start));

        assertEquals("no se pudo escribir el curso", failed.getMessage());
        assertSame(broken.start, broken.courses.course, "el curso vuelve a ser el de partida");
        assertTrue(broken.batches.publicationManifest(other.getId()).isEmpty(), "sin la lista de ese intento");
        assertEquals(otherBefore, photo(broken, other), "nada queda sacado");
        assertEquals(BatchStatus.CLOSED, other.getStatus());
        assertNull(other.getPublication());

        // La lista no se puede armar: tampoco se publica.
        Publishing unbuildable = new Publishing();
        CorrectionBatch third = reviewedAndClosed(unbuildable);
        String thirdBefore = photo(unbuildable, third);
        BatchNotPublishableException notBuilt = assertThrows(BatchNotPublishableException.class,
                () -> unbuildable.publisher().withPublicationList(unbuildable.batches, request -> {
                    throw new IllegalArgumentException("el ejercicio no tiene identificador de producción");
                }).publish(third, ok(), unbuildable.start));
        assertTrue(notBuilt.getMessage().contains("la lista de publicación no se puede armar: el ejercicio no tiene"
                + " identificador de producción"), notBuilt.getMessage());
        assertTrue(unbuildable.courses.written.isEmpty());
        assertEquals(thirdBefore, photo(unbuildable, third));
    }

    // -------------------------------------------------------------------------
    // F-PUBPROD-R004: lo que nadie revisó viaja y se cuenta
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should publish the approved and the unreviewed, count one approved, one unreviewed and one rejected, and say per exercise whether every step is approved")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R004")
    public void shouldPublishTheApprovedAndTheUnreviewedAndCountEachAndSayPerExerciseWhetherEveryStepIsApproved() {
        // R004 inv. 1, 2 y 4: tres pasos. José aprobó el enunciado de las verduras (V1), rechazó su traducción (T) y
        // no miró la abuela (C). La traducción no toca lo mismo que el enunciado: sale sola. Viajan V1 y C; la
        // publicación y la lista cuentan 1 aprobado, 1 sin revisar y 1 rechazado; las verduras van como aprobadas y la
        // abuela como sin revisar. Ningún ejercicio va como rechazado.
        Publishing p = new Publishing();
        CorrectionBatch batch = p.batch("t-respuesta", "t-traduccion", "t-come");
        add(p, batch, V1, BatchChangeOrigin.PANEL, null, "t-respuesta", loved(p), ExercisePart.STATEMENT,
                BatchTaskOutcomeKind.RESOLVED_BY_PANEL);
        add(p, batch, T, BatchChangeOrigin.RULE, "translation-sense", "t-traduccion", translated(p),
                ExercisePart.TRANSLATION, BatchTaskOutcomeKind.RESOLVED_BY_RULE);
        add(p, batch, C, BatchChangeOrigin.PANEL, null, "t-come", comes(p), ExercisePart.STATEMENT,
                BatchTaskOutcomeKind.RESOLVED_BY_PANEL);
        p.decide(batch, V1, BatchReviewDecision.Status.APPROVED, REVIEWED_AT);
        p.decide(batch, T, BatchReviewDecision.Status.REJECTED, REVIEWED_AT);
        p.close(batch);

        p.publisher().publish(batch, ok(), p.start);

        BatchPublication publication = batch.getPublication();
        assertEquals(1, publication.getStepsApproved());
        assertEquals(1, publication.getStepsUnreviewed());
        assertEquals(1, publication.getStepsRejected());
        assertEquals(List.of(T), publication.getChangesLeftOutRejected());
        assertEquals(List.of(), publication.getChangesLeftOutChained());
        assertNull(change(batch, V1).getDroppedReason());
        assertNull(change(batch, C).getDroppedReason(), "lo que nadie revisó viaja");
        CourseEntity written = p.courses.written.get(0);
        assertEquals(loved(p).getSentences(), TandaFixture.exercise(written, MesaFixture.VEGETABLES).getSentences());
        assertEquals(TandaFixture.exercise(p.start, MesaFixture.VEGETABLES).getTranslation(),
                TandaFixture.exercise(written, MesaFixture.VEGETABLES).getTranslation(), "la traducción rechazada no");
        assertEquals(comes(p).getSentences(), TandaFixture.exercise(written, MesaFixture.COME).getSentences());

        PublicationManifest list = manifest(p, batch);
        assertEquals(new ReviewTally(1, 1, 1, List.of(T), List.of()), list.reviews());
        assertEquals(2, list.documents().size());
        ManifestDocument vegetables = document(list, MesaFixture.VEGETABLES);
        assertEquals(DocumentReviewStatus.APPROVED, vegetables.reviewStatus(), "todos sus pasos, aprobados");
        assertEquals(List.of(V1), vegetables.changeIds());
        assertFalse(vegetables.fields().stream().map(ManifestField::path).toList().contains("translation"),
                "la traducción rechazada no viaja");
        assertEquals(DocumentReviewStatus.PENDING, document(list, MesaFixture.COME).reviewStatus(),
                "la abuela, sin revisar");
        assertTrue(list.documents().stream().allMatch(d -> d.reviewStatus() == DocumentReviewStatus.APPROVED
                || d.reviewStatus() == DocumentReviewStatus.PENDING), "ninguno como rechazado");
        assertEquals(List.of(new ManifestExcluded(T, ExclusionReason.REVIEW_REJECTED)), list.excluded());
    }

    @Test
    @DisplayName("should count as unreviewed an approved step whose before changed when a rejected change left")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R004")
    @Tag("FEAT-TANDA")
    @Tag("F-TANDA-R007")
    public void shouldCountAsUnreviewedAnApprovedStepWhoseBeforeChangedWhenARejectedChangeLeft() {
        // R004 inv. 3: José rechazó el enunciado nuevo de las verduras (V1) y aprobó la traducción que venía después
        // (T). Al salir V1, el antes de T ya no es el que José aprobó: viaja, pero como sin revisar, y las verduras van
        // en la lista como «sin revisar» (F-TANDA-R007: una aprobación no se hereda).
        Publishing p = new Publishing();
        CorrectionBatch batch = p.batch("t-respuesta", "t-traduccion");
        add(p, batch, V1, BatchChangeOrigin.PANEL, null, "t-respuesta", loved(p), ExercisePart.STATEMENT,
                BatchTaskOutcomeKind.RESOLVED_BY_PANEL);
        add(p, batch, T, BatchChangeOrigin.RULE, "translation-sense", "t-traduccion", translated(p),
                ExercisePart.TRANSLATION, BatchTaskOutcomeKind.RESOLVED_BY_RULE);
        p.decide(batch, V1, BatchReviewDecision.Status.REJECTED, REVIEWED_AT);
        p.decide(batch, T, BatchReviewDecision.Status.APPROVED, REVIEWED_AT);
        p.close(batch);

        p.publisher().publish(batch, ok(), p.start);

        assertNull(change(batch, T).getDroppedReason(), "la traducción no toca lo mismo: se queda");
        BatchPublication publication = batch.getPublication();
        assertEquals(0, publication.getStepsApproved());
        assertEquals(1, publication.getStepsUnreviewed());
        assertEquals(1, publication.getStepsRejected());
        PublicationManifest list = manifest(p, batch);
        assertEquals(1, list.documents().size());
        ManifestDocument vegetables = list.documents().get(0);
        assertEquals(DocumentReviewStatus.PENDING, vegetables.reviewStatus());
        assertEquals(List.of(new ManifestField("translation",
                TandaFixture.exercise(p.start, MesaFixture.VEGETABLES).getTranslation(), "Me encantan las verduras.")),
                vegetables.fields(), "sólo la traducción: el enunciado rechazado no viaja");
    }

    // -------------------------------------------------------------------------
    // F-PUBPROD-R005: sólo «pasa» publica
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should refuse with «no se puede decidir» when the batch compared again cannot be decided, although the gate of the last closing passed")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R005")
    @Tag("FEAT-TANDA")
    @Tag("F-TANDA-R009")
    public void shouldRefuseWithNoSePuedeDecidirWhenTheBatchComparedAgainCannotBeDecided() {
        // R005 inv. 1 y 2: José ajustó la tanda después de su cierre. Comparada de nuevo, no se puede decidir: no se
        // publica aunque la compuerta del último cierre pase, y la negativa lo dice así, con el detalle de F.
        Publishing p = new Publishing();
        CorrectionBatch batch = p.closedBatch();
        batch.setAdjustments(new ArrayList<>(List.of(new BatchAdjustment(BatchAdjustmentKind.DROP_CHANGE,
                MesaFixture.COME, List.of(), "mesa-otro", null, null, null, "no", "José",
                Publishing.CLOSED_AT.plusSeconds(60)))));
        p.comparator.answer = scope -> {
            BatchComparison comparison = TandaFixture.comparison(scope, List.of(), VerdictKind.UNDECIDABLE);
            comparison.setVerdict(new BatchVerdict(VerdictKind.UNDECIDABLE, new ArrayList<>(), new ArrayList<>(), null,
                    "ningún piso falla, pero 1 piso sin decidir", new UndecidableFloors(1, List.of(
                            new RowSubject("lemma-absence", "Palabras del nivel")))));
            return comparison;
        };
        assertTrue(batch.getClosings().get(0).getGate().isPasses(), "la compuerta del último cierre pasa");

        BatchNotPublishableException refused = assertThrows(BatchNotPublishableException.class,
                () -> p.publisher().publish(batch, ok(), p.start));

        assertTrue(refused.getMessage().contains("el veredicto de F es «no se puede decidir», no «pasa»: ningún piso"
                + " falla, pero 1 piso sin decidir"), refused.getMessage());
        assertFalse(refused.getMessage().contains("UNDECIDABLE"), refused.getMessage());
        assertTrue(p.courses.written.isEmpty());
        assertTrue(p.batches.manifests.isEmpty());
        assertNull(batch.getPublication());
    }

    @Test
    @DisplayName("should refuse to publish a verdict «pasa» that still has an undecidable floor, naming it")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R005")
    public void shouldRefuseToPublishAVerdictPasaThatStillHasAnUndecidableFloorNamingIt() {
        // R005 inv. 1: el último cierre dice «pasa», pero con un piso sin decidir —el de las palabras del nivel—. Eso
        // no es «pasa»: no se publica y la negativa nombra el piso.
        Publishing p = new Publishing();
        CorrectionBatch batch = p.closedBatch();
        batch.getClosings().get(0).getComparison().setVerdict(new BatchVerdict(VerdictKind.PASSES, new ArrayList<>(),
                new ArrayList<>(), null, null, new UndecidableFloors(1, List.of(new RowSubject("lemma-absence",
                        "Palabras del nivel")))));

        BatchNotPublishableException refused = assertThrows(BatchNotPublishableException.class,
                () -> p.publisher().publish(batch, ok(), p.start));

        assertTrue(refused.getMessage().contains("no se puede decidir: 1 piso sin decidir (lemma-absence)"),
                refused.getMessage());
        assertTrue(p.comparator.scopes.isEmpty(), "nada cambió desde el cierre: no se compara de nuevo");
        assertTrue(p.courses.written.isEmpty());
        assertTrue(p.batches.manifests.isEmpty());
    }

    @Test
    @DisplayName("should refuse naming the gate when the gate of the last closing does not pass or is missing, before leaving anything out or comparing")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R005")
    @Tag("FEAT-TANDA")
    @Tag("F-TANDA-R009")
    public void shouldRefuseNamingTheGateWhenTheGateOfTheLastClosingDoesNotPassOrIsMissing() {
        // R005 inv. 2: la compuerta de «no empeorar» del último cierre no pasa. La negativa la nombra con sus motivos,
        // y no llega a sacar el rechazo de José ni a comparar: la tanda queda como estaba.
        Publishing p = new Publishing();
        CorrectionBatch batch = reviewedAndClosed(p);
        batch.getClosings().get(0).setGate(new BatchGateVerdict(false, VerdictKind.PASSES, List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of("F-NOEMP-R007: cambió fuera de la tanda: "
                        + TandaFixture.CHILD)));
        String before = photo(p, batch);

        BatchNotPublishableException refused = assertThrows(BatchNotPublishableException.class,
                () -> p.publisher().publish(batch, ok(), p.start));

        assertTrue(refused.getMessage().contains("la compuerta de «no empeorar» del último cierre no pasa: "
                + "F-NOEMP-R007: cambió fuera de la tanda: " + TandaFixture.CHILD), refused.getMessage());
        assertEquals(before, photo(p, batch));
        assertTrue(p.comparator.scopes.isEmpty());
        assertTrue(p.courses.written.isEmpty());
        assertTrue(p.batches.manifests.isEmpty());

        // Sin compuerta, tampoco; y sin cierre, menos.
        batch.getClosings().get(0).setGate(null);
        BatchNotPublishableException noGate = assertThrows(BatchNotPublishableException.class,
                () -> p.publisher().publish(batch, ok(), p.start));
        assertTrue(noGate.getMessage().contains("la compuerta de «no empeorar» del último cierre no está"),
                noGate.getMessage());
        batch.setClosings(new ArrayList<>());
        BatchNotPublishableException notClosed = assertThrows(BatchNotPublishableException.class,
                () -> p.publisher().publish(batch, ok(), p.start));
        assertTrue(notClosed.getMessage().contains("la tanda no se cerró"), notClosed.getMessage());
        assertTrue(p.courses.written.isEmpty());
    }

    // -------------------------------------------------------------------------
    // F-PUBPROD-R001: la lista, antes del curso
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should save the publication list before writing the course and record its digest, the verdict «pasa», the gate that passed and no undecidable floor")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R001")
    @Tag("F-PUBPROD-R005")
    @Tag("FEAT-TANDA")
    @Tag("F-TANDA-R009")
    public void shouldSaveThePublicationListBeforeWritingTheCourseAndRecordItsDigest() throws Exception {
        // R001 inv. 1, 2 y 4, y R005 inv. 3: una tanda sin rechazos. La lista queda guardada cuando el curso todavía no
        // se escribió; la publicación registra su huella —la del archivo exacto—, el veredicto «pasa», que la compuerta
        // pasó y ningún piso sin decidir, y la lista dice lo mismo. Lleva las verduras con el antes del curso del que
        // partió la tanda y el después del curso que se publica; la abuela, que José sacó, figura como sacada por él.
        // Sin rechazos y sin cambios desde el cierre, no se compara de nuevo (R003 inv. 7).
        Publishing p = new Publishing();
        List<Integer> writtenWhenListed = new ArrayList<>();
        p.batches = new TandaFixture.Batches() {
            @Override
            public synchronized String savePublicationManifest(String batchId, PublicationManifest manifest) {
                writtenWhenListed.add(p.courses.written.size());
                return super.savePublicationManifest(batchId, manifest);
            }
        };
        CorrectionBatch batch = p.closedBatch();

        p.publisher().publish(batch, new BatchPublishRequest("José", "la primera tanda", List.of()), p.start);

        assertEquals(List.of(0), writtenWhenListed, "la lista, una vez y antes del curso");
        assertEquals(1, p.courses.written.size());
        BatchPublication publication = batch.getPublication();
        byte[] bytes = p.batches.manifests.get(batch.getId());
        assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),
                publication.getManifestDigest(), "la huella del archivo exacto");
        assertEquals(VerdictKind.PASSES, publication.getVerdictKind());
        assertEquals(Boolean.TRUE, publication.getGatePassed());
        assertEquals(List.of(), publication.getUndecidableFloors());
        assertTrue(p.comparator.scopes.isEmpty(), "sin rechazos ni cambios desde el cierre: no se compara");

        PublicationManifest list = manifest(p, batch);
        assertEquals(PublicationManifest.SCHEMA, list.schema());
        assertEquals(batch.getId(), list.batchId());
        assertEquals(TandaFixture.PLAN, list.planId());
        assertEquals("A1", list.level());
        assertEquals("José", list.approvedBy());
        assertEquals(publication.getPublishedAt().toString(), list.publishedAt(), "el mismo momento");
        assertEquals(TandaFixture.BASELINE, list.source().baselineAuditId());
        assertEquals(TandaFixture.START_AUDIT, list.source().startAuditId());
        assertEquals(VerdictKind.PASSES, list.verdict().kind());
        assertTrue(list.verdict().gatePassed());
        assertEquals(List.of(), list.verdict().undecidableFloors());
        assertEquals(new ReviewTally(0, 1, 0, List.of(), List.of()), list.reviews());
        assertEquals(List.of(new ManifestExcluded(C, ExclusionReason.DROPPED_BY_JOSE)), list.excluded());
        assertEquals(1, list.documents().size());
        ManifestDocument vegetables = list.documents().get(0);
        assertEquals(MesaFixture.VEGETABLES, vegetables.nodeId());
        assertEquals(DocumentReviewStatus.PENDING, vegetables.reviewStatus(), "nadie lo revisó: viaja sin revisar");
        ManifestField sentences = vegetables.fields().stream().filter(f -> "form.sentences".equals(f.path()))
                .findFirst().orElseThrow(() -> new AssertionError("form.sentences: " + vegetables.fields()));
        assertEquals(TandaFixture.exercise(p.start, MesaFixture.VEGETABLES).getSentences(), sentences.before());
        assertEquals(List.of("I love vegetables."), sentences.after());
        assertEquals(TandaFixture.exercise(p.courses.written.get(0), MesaFixture.VEGETABLES).getSentences(),
                sentences.after(), "el después es el del curso que se publica");
        assertEquals(vegetables.fields().size(), list.counts().fields());
        assertEquals(1, list.counts().documents());
    }

    @Test
    @DisplayName("should name in the list the changes of a rule José left out when publishing, with the reason «regla que José sacó»")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R001")
    public void shouldNameInTheListTheChangesOfARuleJoseLeftOutWhenPublishing() {
        // R001 (detalle): los cambios que quedaron afuera van con su motivo. José sacó la regla de la traducción al
        // publicar: su cambio va como regla excluida, y la traducción no viaja.
        Publishing p = new Publishing();
        CorrectionBatch batch = p.closedBatch();
        batch.getTaskIds().add("t-traduccion");
        add(p, batch, T, BatchChangeOrigin.RULE, "translation-sense", "t-traduccion", translated(p),
                ExercisePart.TRANSLATION, BatchTaskOutcomeKind.RESOLVED_BY_RULE);
        p.close(batch);

        p.publisher().publish(batch, new BatchPublishRequest("José", null, List.of("translation-sense")), p.start);

        PublicationManifest list = manifest(p, batch);
        assertEquals(List.of(new ManifestExcluded(C, ExclusionReason.DROPPED_BY_JOSE),
                new ManifestExcluded(T, ExclusionReason.RULE_EXCLUDED)), list.excluded());
        assertTrue(list.documents().stream().flatMap(d -> d.fields().stream()).noneMatch(f -> "translation"
                .equals(f.path())), "la traducción de la regla sacada no viaja");
        assertEquals(TandaFixture.exercise(p.start, MesaFixture.VEGETABLES).getTranslation(),
                TandaFixture.exercise(p.courses.written.get(0), MesaFixture.VEGETABLES).getTranslation());
    }

    @Test
    @DisplayName("should refuse to publish when it has nowhere to leave the publication list or nothing to build it with")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R001")
    public void shouldRefuseToPublishWhenItHasNowhereToLeaveTheListOrNothingToBuildItWith() {
        // R001 inv. 3: sin dónde dejar la lista o sin con qué armarla, no se publica y el curso no se toca.
        Publishing p = new Publishing();
        CorrectionBatch batch = p.closedBatch();

        BatchNotPublishableException refused = assertThrows(BatchNotPublishableException.class,
                () -> p.publisher().withPublicationList(p.batches, null).publish(batch, ok(), p.start));
        assertTrue(refused.getMessage().contains("falta con qué armar la lista de publicación"), refused.getMessage());
        BatchNotPublishableException nowhere = assertThrows(BatchNotPublishableException.class,
                () -> p.publisher().withPublicationList(null, request -> {
                    throw new AssertionError("no se arma una lista que no se puede guardar");
                }).publish(batch, ok(), p.start));
        assertTrue(nowhere.getMessage().contains("falta con qué armar la lista de publicación"), nowhere.getMessage());
        assertTrue(p.courses.written.isEmpty());
        assertEquals(BatchStatus.CLOSED, batch.getStatus());

        // Una lista sin huella tampoco sirve.
        Publishing blank = new Publishing();
        blank.batches = new TandaFixture.Batches() {
            @Override
            public synchronized String savePublicationManifest(String batchId, PublicationManifest manifest) {
                return " ";
            }
        };
        CorrectionBatch other = blank.closedBatch();
        BatchNotPublishableException noDigest = assertThrows(BatchNotPublishableException.class,
                () -> blank.publisher().publish(other, ok(), blank.start));
        assertTrue(noDigest.getMessage().contains("la lista de publicación quedó sin huella"), noDigest.getMessage());
        assertTrue(blank.courses.written.isEmpty());
    }

    // -------------------------------------------------------------------------
    // F-PUBPROD-R010: una publicación que no termina no deja nada a medias
    // -------------------------------------------------------------------------

    /** El análisis vigente antes de publicar. */
    private static final ActiveAnalysisSelection ACTIVE_BEFORE = new ActiveAnalysisSelection("audit-de-partida",
            TandaFixture.PLAN);

    /** Cómo están las tareas del plan de la tanda. */
    private static Map<String, RefinementTaskStatus> statuses(Publishing p) {
        Map<String, RefinementTaskStatus> statuses = new LinkedHashMap<>();
        for (RefinementTask task : p.plans.load(TandaFixture.PLAN).orElseThrow().getTasks()) {
            statuses.put(task.getId(), task.getStatus());
        }
        return statuses;
    }

    /** Una tanda que José revisó, cerrada con «pasa», con el análisis vigente de partida. */
    private static CorrectionBatch readyToPublish(Publishing p) {
        p.active.selection = ACTIVE_BEFORE;
        return reviewedAndClosed(p);
    }

    /** Guardar la tanda como publicada falla una vez; antes, si se pide, deja que algo más falle. */
    private static TandaFixture.Batches failingToSavePublished(Runnable alsoBreak) {
        return new TandaFixture.Batches() {
            private boolean failed;

            @Override
            public synchronized void save(CorrectionBatch batch) {
                if (!failed && batch.getStatus() == BatchStatus.PUBLISHED) {
                    failed = true;
                    alsoBreak.run();
                    throw new IllegalStateException("no se pudo guardar la tanda");
                }
                super.save(batch);
            }
        };
    }

    /** Todo como antes de publicar: el curso, la lista, las propuestas, las tareas, el análisis vigente y la tanda. */
    private static void assertNothingHalfDone(Publishing p, CorrectionBatch batch, String before,
            Map<String, RefinementTaskStatus> tasksBefore) {
        assertSame(p.start, p.courses.course, "el curso vuelve a ser el de partida");
        assertSame(p.start, p.courses.written.get(p.courses.written.size() - 1));
        assertTrue(p.batches.publicationManifest(batch.getId()).isEmpty(), "sin la lista de ese intento");
        assertEquals(before, photo(p, batch), "las propuestas, los cambios sacados y las tareas de la tanda como estaban");
        assertEquals(RevisionVerdict.PENDING_APPROVAL, p.artifacts.load(TandaFixture.PLAN, Publishing.P_COME)
                .orElseThrow().getVerdict(), "ninguna propuesta queda aplicada");
        assertEquals(tasksBefore, statuses(p), "las tareas del plan como estaban");
        assertSame(ACTIVE_BEFORE, p.active.selection, "el análisis vigente sigue siendo el de antes");
        assertEquals(BatchStatus.CLOSED, batch.getStatus(), "la tanda no queda publicada");
        assertNull(batch.getPublication());
        assertTrue(p.batches.load(batch.getId()).map(b -> b.getStatus() != BatchStatus.PUBLISHED).orElse(true));
    }

    @Test
    @DisplayName("should put the course, the list of the attempt, the proposals, the tasks and the batch back as they were when the analysis of the published course cannot be saved, and throw the failure as it came")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R010")
    @Tag("F-PUBPROD-R003")
    public void shouldPutEverythingBackWhenTheAnalysisOfThePublishedCourseCannotBeSaved() {
        // R010 inv. 1 a 5: el curso ya se escribió y la lista ya se guardó cuando falla guardar el análisis del curso
        // publicado. El curso vuelve al de partida, la lista de ese intento se borra, V1 vuelve a estar en la tanda
        // con su propuesta, la abuela no queda aplicada y la tanda no queda publicada. La falla sube tal cual.
        Publishing p = new Publishing();
        CorrectionBatch batch = readyToPublish(p);
        String before = photo(p, batch);
        Map<String, RefinementTaskStatus> tasksBefore = statuses(p);
        p.reports.failure = new IllegalStateException("no se pudo guardar el análisis");

        IllegalStateException failed = assertThrows(IllegalStateException.class,
                () -> p.publisher().publish(batch, ok(), p.start));

        assertEquals("no se pudo guardar el análisis", failed.getMessage(), "la falla sube tal cual");
        assertEquals(2, p.courses.written.size(), "el curso se escribió y se volvió a escribir como estaba");
        assertNothingHalfDone(p, batch, before, tasksBefore);
    }

    @Test
    @DisplayName("should put everything back, the tasks of the plan and the active analysis included, when saving the batch as published fails, so it is never published")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R010")
    public void shouldPutEverythingBackWhenSavingTheBatchAsPublishedFails() {
        // R010 inv. 1 a 5: falla lo último, guardar la tanda como publicada; para entonces las tareas del plan ya se
        // asentaron y el análisis vigente ya se movió. Todo vuelve; el análisis del curso publicado queda guardado,
        // pero no es el vigente.
        Publishing p = new Publishing();
        p.batches = failingToSavePublished(() -> {
        });
        CorrectionBatch batch = readyToPublish(p);
        String before = photo(p, batch);
        Map<String, RefinementTaskStatus> tasksBefore = statuses(p);

        IllegalStateException failed = assertThrows(IllegalStateException.class,
                () -> p.publisher().publish(batch, ok(), p.start));

        assertEquals("no se pudo guardar la tanda", failed.getMessage(), "la falla sube tal cual");
        assertFalse(p.reports.saved.isEmpty(), "el análisis del curso publicado se alcanzó a guardar");
        assertNothingHalfDone(p, batch, before, tasksBefore);
    }

    @Test
    @DisplayName("should publish again after a failure and leave the same list as a publication that never failed")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R010")
    @Tag("F-PUBPROD-R001")
    public void shouldPublishAgainAfterAFailureAndLeaveTheSameListAsAPublicationThatNeverFailed() {
        // R010 inv. 7: después de que guardar la tanda fallara, publicarla otra vez publica, y su lista lleva los mismos
        // ejercicios, campos, cuentas y cambios afuera que la de una publicación que nunca falló.
        Publishing retried = new Publishing();
        retried.batches = failingToSavePublished(() -> {
        });
        CorrectionBatch batch = readyToPublish(retried);
        assertThrows(IllegalStateException.class, () -> retried.publisher().publish(batch, ok(), retried.start));

        CorrectionBatch published = retried.publisher().publish(batch, ok(), retried.start);

        Publishing straight = new Publishing();
        CorrectionBatch same = readyToPublish(straight);
        straight.publisher().publish(same, ok(), straight.start);
        assertEquals(BatchStatus.PUBLISHED, published.getStatus());
        assertSame(published, retried.batches.load(batch.getId()).orElseThrow(), "la tanda quedó guardada");
        assertEquals(retried.batches.digests.get(batch.getId()), published.getPublication().getManifestDigest());
        PublicationManifest again = manifest(retried, batch);
        PublicationManifest never = manifest(straight, same);
        assertEquals(json(never.documents()), json(again.documents()), "los mismos ejercicios y campos");
        assertEquals(json(never.excluded()), json(again.excluded()), "los mismos cambios afuera");
        assertEquals(never.reviews(), again.reviews());
        assertEquals(never.counts(), again.counts());
        assertEquals(json(straight.courses.course), json(retried.courses.course), "el mismo curso");
    }

    @Test
    @DisplayName("should say what was left half done, with the original failure as its cause, when the course cannot be written back")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R010")
    public void shouldSayWhatWasLeftHalfDoneWhenTheCourseCannotBeWrittenBack() {
        // R010 inv. 6: falla guardar la tanda y, al volver atrás, tampoco se puede volver a escribir el curso. La falla
        // dice que el curso quedó a medio hacer, con la original como causa; lo demás vuelve igual.
        Publishing p = new Publishing();
        p.batches = failingToSavePublished(() -> p.courses.failure = new IllegalStateException("el disco se llenó"));
        CorrectionBatch batch = readyToPublish(p);
        String before = photo(p, batch);

        IllegalStateException failed = assertThrows(IllegalStateException.class,
                () -> p.publisher().publish(batch, ok(), p.start));

        assertTrue(failed.getMessage().contains("La publicación de la tanda " + batch.getId()
                + " no terminó (no se pudo guardar la tanda) y no se pudo deshacer del todo: el curso en "
                + TandaFixture.COURSE_PATH + " quedó como lo escribió esta publicación, o a medio escribir (el disco se"
                + " llenó)"), failed.getMessage());
        assertEquals("no se pudo guardar la tanda", failed.getCause().getMessage());
        assertEquals(1, failed.getSuppressed().length);
        assertTrue(p.batches.publicationManifest(batch.getId()).isEmpty(), "la lista de ese intento se borró igual");
        assertEquals(before, photo(p, batch));
        assertSame(ACTIVE_BEFORE, p.active.selection);
        assertNull(batch.getPublication());
    }
}
