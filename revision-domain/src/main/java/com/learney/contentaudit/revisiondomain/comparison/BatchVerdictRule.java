package com.learney.contentaudit.revisiondomain.comparison;

import com.learney.contentaudit.revisiondomain.blindsample.SampleOutcome;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * F-METAS-R008: the verdict of a batch from what its comparison found -- the floors, the changes that return and the
 * blind sample. A change José keeps as his exception (F-BANDEJA-R003, R011) does not return, whatever it gained: the
 * verdict lists it apart, as the gate of «no empeorar» does (F-NOEMP-R005). The comparison of a batch decides it with
 * the exceptions of its scope; a verdict saved before -- the one of a closing -- is read again with the exceptions the
 * batch has now ({@link #withExceptions}), so the tray and publishing say what F says today.
 */
public final class BatchVerdictRule {

    /** How many ids a detail names before saying how many more there are. */
    private static final int LISTED_IDS = 10;

    private BatchVerdictRule() {
    }

    /**
     * «no pasa» with the broken floors, the changes that return and a blind sample that misses its goal; else «no se
     * puede decidir» with the floors that cannot be decided and the changes an analyzer left unevaluated; else «falta la
     * muestra ciega» while it is missing or open; else «pasa». José's exceptions that gained something are listed apart,
     * whatever the verdict.
     */
    public static BatchVerdict decide(List<ComparisonRow> rows, List<FloorBreach> breaches, List<ChangeOutcome> outcomes,
            Collection<String> exceptions, UndecidableFloors undecided, SampleOutcome sample, String scopeId) {
        Set<String> excepted = new HashSet<>(list(exceptions));
        List<String> broken = new ArrayList<>();
        for (FloorBreach breach : list(breaches)) {
            if (breach != null && breach.getGoalId() != null && !broken.contains(breach.getGoalId())) {
                broken.add(breach.getGoalId());
            }
        }
        List<String> returning = new ArrayList<>();
        List<String> excused = new ArrayList<>();
        List<String> undecidedChanges = new ArrayList<>();
        for (ChangeOutcome outcome : list(outcomes)) {
            boolean exception = excepted.contains(outcome.getProposalId());
            if (exception && (outcome.isReturns() || !list(outcome.getNewFindings()).isEmpty())
                    && !excused.contains(outcome.getProposalId())) {
                excused.add(outcome.getProposalId());
            }
            if (outcome.isReturns() && !exception) {
                returning.add(outcome.getProposalId());
            } else if (!list(outcome.getUndecidedAnalyzers()).isEmpty()) {
                undecidedChanges.add(outcome.getProposalId());
            }
        }
        return verdict(broken, returning, excused, undecidedChanges, undecidableFloorIds(rows), undecided, sample,
                scopeId);
    }

    /**
     * F-BANDEJA-R011: a verdict already given, read with the exceptions the batch has now. The changes it returned that
     * José keeps as his exception no longer return; everything else -- the broken floors, the floors that cannot be
     * decided, the blind sample as it was then -- stays as the comparison found it, and so does what the detail said
     * after the verdict. A verdict that returned none of his exceptions is given back as it is.
     */
    public static BatchVerdict withExceptions(BatchComparison comparison, Collection<String> exceptions) {
        BatchVerdict given = comparison != null ? comparison.getVerdict() : null;
        Set<String> excepted = new HashSet<>(list(exceptions));
        if (given == null || list(given.getReturningProposalIds()).stream().noneMatch(excepted::contains)) {
            return given;
        }
        List<String> returning = new ArrayList<>();
        List<String> excused = new ArrayList<>();
        for (String id : list(given.getReturningProposalIds())) {
            if (!excepted.contains(id)) {
                returning.add(id);
            } else if (!excused.contains(id)) {
                excused.add(id);
            }
        }
        List<String> undecidedBefore = new ArrayList<>();
        List<String> undecidedNow = new ArrayList<>();
        for (ChangeOutcome outcome : list(comparison.getChanges())) {
            if (list(outcome.getUndecidedAnalyzers()).isEmpty()) {
                continue;
            }
            if (!given.getReturningProposalIds().contains(outcome.getProposalId())) {
                undecidedBefore.add(outcome.getProposalId());
            }
            if (!returning.contains(outcome.getProposalId())) {
                undecidedNow.add(outcome.getProposalId());
            }
        }
        List<String> broken = new ArrayList<>(list(given.getBrokenFloorGoalIds()));
        List<String> undecidable = undecidableFloorIds(comparison.getRows());
        String said = verdict(broken, list(given.getReturningProposalIds()), List.of(), undecidedBefore, undecidable,
                given.getUndecidableFloors(), given.getSample(), comparison.getScopeId()).getDetail();
        BatchVerdict now = verdict(broken, returning, excused, undecidedNow, undecidable, given.getUndecidableFloors(),
                given.getSample(), comparison.getScopeId());
        String detail = given.getDetail();
        if (detail != null && said != null && detail.startsWith(said)) {
            now.setDetail(now.getDetail() + detail.substring(said.length()));
        }
        return now;
    }

    /** The floors among the rows that cannot be decided, once each, in the order of the rows. */
    public static List<String> undecidableFloorIds(List<ComparisonRow> rows) {
        List<String> ids = new ArrayList<>();
        for (ComparisonRow row : list(rows)) {
            if (row != null && row.getFloor() != null && row.getFloor().getStatus() == GoalStatus.UNDECIDABLE
                    && !ids.contains(row.getFloor().getGoalId())) {
                ids.add(row.getFloor().getGoalId());
            }
        }
        return ids;
    }

    /** Up to ten ids, and how many more there are. */
    public static String listed(List<String> ids) {
        if (ids.size() <= LISTED_IDS) {
            return String.join(", ", ids);
        }
        return String.join(", ", ids.subList(0, LISTED_IDS)) + " y " + (ids.size() - LISTED_IDS) + " más";
    }

    private static BatchVerdict verdict(List<String> broken, List<String> returning, List<String> excused,
            List<String> undecidedChanges, List<String> undecidableFloors, UndecidableFloors undecided,
            SampleOutcome sample, String scopeId) {
        BatchVerdict verdict = kindAndDetail(broken, returning, undecidedChanges, undecidableFloors, undecided, sample,
                scopeId);
        if (!excused.isEmpty()) {
            verdict.setExceptionProposalIds(new ArrayList<>(excused));
            verdict.setDetail(verdict.getDetail() + " Por excepción de José, sin volver: "
                    + (excused.size() == 1 ? "1 cambio (" : excused.size() + " cambios (") + listed(excused) + ").");
        }
        return verdict;
    }

    private static BatchVerdict kindAndDetail(List<String> broken, List<String> returning, List<String> undecidedChanges,
            List<String> undecidableFloors, UndecidableFloors undecided, SampleOutcome sample, String scopeId) {
        boolean sampleFails = sample != null && sample.isClosed() && !sample.isMeetsGoal();
        if (!broken.isEmpty() || !returning.isEmpty() || sampleFails) {
            List<String> parts = new ArrayList<>();
            if (!returning.isEmpty()) {
                parts.add((returning.size() == 1 ? "vuelve 1 cambio: " : "vuelven " + returning.size() + " cambios: ")
                        + listed(returning));
            }
            if (!broken.isEmpty()) {
                parts.add((broken.size() == 1 ? "1 piso roto: " : broken.size() + " pisos rotos: ") + listed(broken));
            }
            if (sampleFails) {
                parts.add("la muestra ciega " + sample.getSampleId() + " no cumple su meta"
                        + (sample.getDetail() != null && !sample.getDetail().isBlank() ? " (" + sample.getDetail() + ")"
                                : ""));
            }
            return new BatchVerdict(VerdictKind.FAILS, broken, returning, sample, "No pasa: " + String.join("; ", parts),
                    undecided);
        }
        if (!undecidableFloors.isEmpty() || !undecidedChanges.isEmpty()) {
            List<String> parts = new ArrayList<>();
            if (!undecidableFloors.isEmpty()) {
                parts.add((undecidableFloors.size() == 1 ? "1 piso sin decidir: "
                        : undecidableFloors.size() + " pisos sin decidir: ") + listed(undecidableFloors));
            }
            if (!undecidedChanges.isEmpty()) {
                parts.add((undecidedChanges.size() == 1 ? "1 cambio" : undecidedChanges.size() + " cambios")
                        + " sin evaluar por algún analizador: " + listed(undecidedChanges));
            }
            return new BatchVerdict(VerdictKind.UNDECIDABLE, broken, returning, sample,
                    "No se puede decidir: ningún piso falla, pero " + String.join("; ", parts), undecided);
        }
        if (sample == null) {
            return new BatchVerdict(VerdictKind.BLIND_SAMPLE_MISSING, broken, returning, null,
                    "Falta la muestra ciega: los pisos se cumplen y no vuelve ningún cambio, pero no hay muestra de '"
                            + scopeId + "'.", undecided);
        }
        if (!sample.isClosed()) {
            return new BatchVerdict(VerdictKind.BLIND_SAMPLE_MISSING, broken, returning, sample,
                    "Falta la muestra ciega: los pisos se cumplen y no vuelve ningún cambio, pero la muestra "
                            + sample.getSampleId() + " sigue abierta (" + sample.getAnswered() + " pares respondidos).",
                    undecided);
        }
        return new BatchVerdict(VerdictKind.PASSES, broken, returning, sample,
                "Pasa: se cumplen los pisos, no vuelve ningún cambio y la muestra ciega " + sample.getSampleId()
                        + " cumple su meta.", undecided);
    }

    private static <T> List<T> list(Collection<T> values) {
        return values != null ? new ArrayList<>(values) : new ArrayList<>();
    }
}
