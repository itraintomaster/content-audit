package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.revisiondomain.batch.BatchChange;
import com.learney.contentaudit.revisiondomain.batch.BatchChangeOrigin;
import com.learney.contentaudit.revisiondomain.batch.BatchReview;
import com.learney.contentaudit.revisiondomain.batch.BatchReviewDecision;
import com.learney.contentaudit.revisiondomain.batch.CorrectionBatch;
import com.learney.contentaudit.revisiondomain.publication.ExclusionReason;
import com.learney.contentaudit.revisiondomain.publication.ManifestExcluded;
import com.learney.contentaudit.revisiondomain.publication.ReviewTally;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * F-PUBPROD-R001, R003 y R004: lo que queda afuera al publicar, y por qué. Arranca con lo que José ya había sacado antes
 * de publicar; después saca, por el libro de cambios, los cambios de las reglas que José sacó al publicar y cada cambio
 * con un paso que José rechazó —entero, con lo que va con él—, y nombra cada cambio que salió con su motivo. De ahí salen
 * las cuentas de la revisión y los cambios afuera que lleva la lista.
 */
final class PublicationExclusions {

    /** El motivo con el que sale de la tanda un cambio con un paso que José rechazó. */
    static final String REJECTED_IN_REVIEW = "rechazado al revisar";

    private final CorrectionBatch batch;

    private final BatchChangeLedger ledger;

    private final CourseEntity startCourse;

    /** Cada cambio afuera, con su motivo, en el orden en que salió. */
    private final Map<String, ExclusionReason> excluded = new LinkedHashMap<>();

    /** Los cambios con un paso que José rechazó en alguna lectura de la revisión. */
    private final Set<String> rejected = new LinkedHashSet<>();

    /** Los pasos que José rechazó —cambio y ejercicio—, para contarlos una vez. */
    private final Set<String> rejectedSteps = new LinkedHashSet<>();

    /**
     * Lo afuera de {@code batch} antes de publicar —lo que José sacó, no lo que devolvió la compuerta— y los rechazos de
     * la revisión tal como está ahora, antes de que salga nada.
     */
    PublicationExclusions(CorrectionBatch batch, BatchChangeLedger ledger, CourseEntity startCourse,
            ReviewStanding standing) {
        this.batch = batch;
        this.ledger = ledger;
        this.startCourse = startCourse;
        for (String changeId : droppedByJose(batch)) {
            excluded.put(changeId, ExclusionReason.DROPPED_BY_JOSE);
        }
        readRejections(standing);
    }

    /** Saca los cambios de la regla que José sacó al publicar; dice si salió alguno. */
    boolean leaveOutRule(String ruleId) {
        boolean dropped = false;
        for (BatchChange change : BatchBook.activeChanges(batch)) {
            if (change.getOrigin() == BatchChangeOrigin.RULE && Objects.equals(change.getRuleId(), ruleId)) {
                dropped |= leaveOut(change.getChangeId(), "José sacó la regla " + ruleId + " al publicar",
                        ExclusionReason.RULE_EXCLUDED);
            }
        }
        return dropped;
    }

    /**
     * Lee los rechazos de la revisión como está ahora y saca cada cambio que sigue en la tanda con un paso rechazado;
     * dice si salió alguno. Hay que volver a llamarlo con la revisión releída hasta que diga que no.
     */
    boolean leaveOutRejected(ReviewStanding standing) {
        readRejections(standing);
        boolean dropped = false;
        for (BatchChange change : BatchBook.activeChanges(batch)) {
            if (rejected.contains(change.getChangeId())) {
                dropped |= leaveOut(change.getChangeId(), REJECTED_IN_REVIEW, ExclusionReason.REVIEW_REJECTED);
            }
        }
        return dropped;
    }

    /** Las cuentas de la revisión: aprobados y pendientes entre lo que viaja, rechazados entre lo que salió. */
    ReviewTally tally(ReviewStanding standing) {
        int approved = 0;
        int pending = 0;
        for (BatchReview.Item item : standing.items()) {
            if (item.status() == BatchReviewDecision.Status.APPROVED) {
                approved++;
            } else {
                pending++;
            }
        }
        List<String> forReview = new ArrayList<>();
        List<String> collateral = new ArrayList<>();
        excluded.forEach((changeId, reason) -> {
            if (reason == ExclusionReason.REVIEW_REJECTED) {
                forReview.add(changeId);
            } else if (reason == ExclusionReason.COLLATERAL) {
                collateral.add(changeId);
            }
        });
        return new ReviewTally(approved, pending, rejectedSteps.size(), forReview, collateral);
    }

    /** Los cambios afuera que nombra la lista, con su motivo, en el orden en que salieron. */
    List<ManifestExcluded> leftOut() {
        List<ManifestExcluded> leftOut = new ArrayList<>();
        excluded.forEach((changeId, reason) -> leftOut.add(new ManifestExcluded(changeId, reason)));
        return leftOut;
    }

    private void readRejections(ReviewStanding standing) {
        for (BatchReview.Item item : standing.items()) {
            if (item.status() == BatchReviewDecision.Status.REJECTED) {
                rejected.add(item.changeId());
                rejectedSteps.add(item.changeId() + "\u0000" + (item.step() != null ? item.step().getQuizId() : null));
            }
        }
    }

    /**
     * Saca un cambio por el libro de cambios —con los cambios posteriores del mismo ejercicio que tocaron lo mismo, y sus
     * tareas de vuelta a pendientes— y nombra cada cambio que salió: rechazado si José lo rechazó, {@code why} el que se
     * pidió, colaterales los demás. Un cambio que ya salió con otro no se vuelve a sacar; dice si lo sacó.
     */
    private boolean leaveOut(String changeId, String reason, ExclusionReason why) {
        Set<String> before = activeChangeIds();
        if (!before.contains(changeId)) {
            return false;
        }
        ledger.drop(batch, changeId, reason, startCourse);
        Set<String> after = activeChangeIds();
        for (String id : before) {
            if (!after.contains(id)) {
                excluded.put(id, rejected.contains(id) ? ExclusionReason.REVIEW_REJECTED
                        : id.equals(changeId) ? why : ExclusionReason.COLLATERAL);
            }
        }
        return true;
    }

    private Set<String> activeChangeIds() {
        Set<String> active = new LinkedHashSet<>();
        for (BatchChange change : BatchBook.activeChanges(batch)) {
            active.add(change.getChangeId());
        }
        return active;
    }

    /**
     * Los cambios que José sacó antes de publicar, con los que salieron con ellos: todo lo que ya está afuera menos lo
     * que la compuerta de la tanda devolvió en su cierre y lo que salió con eso.
     */
    private static List<String> droppedByJose(CorrectionBatch batch) {
        Set<String> returnedByGate = new LinkedHashSet<>();
        List<String> jose = new ArrayList<>();
        for (BatchChange change : BatchBook.list(batch.getChanges())) {
            if (change == null || change.getDroppedReason() == null) {
                continue;
            }
            String reason = change.getDroppedReason();
            String chainedWith = DefaultBatchChangeLedger.chainedWith(reason);
            if (reason.startsWith(DefaultBatchCloser.RETURNED_BY_GATE)
                    || (chainedWith != null && returnedByGate.contains(chainedWith))) {
                returnedByGate.add(change.getChangeId());
            } else {
                jose.add(change.getChangeId());
            }
        }
        return jose;
    }
}
