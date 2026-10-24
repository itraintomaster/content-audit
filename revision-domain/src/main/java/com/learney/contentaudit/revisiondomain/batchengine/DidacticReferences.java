package com.learney.contentaudit.revisiondomain.batchengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.panel.DidacticReference;
import com.learney.contentaudit.revisiondomain.panel.DidacticReference.AncestryStep;
import com.learney.contentaudit.revisiondomain.panel.DidacticReference.Provenance;
import java.util.*;
import java.util.function.Function;

/** Resolves exact saved before-snapshots, walking only explicit, sealed predecessor links. */
final class DidacticReferences {
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    private DidacticReferences() {}
    private record Ancestor(CorrectionBatch batch, List<CorrectionBatch> children, List<AncestryStep> path) {}
    static Map<String, DidacticReference> resolve(CorrectionBatch batch, String sourceOperationId, List<String> quizzes,
            boolean restoreRequestFormula, BatchStore store, BatchChangeLedger ledger, Function<CourseEntity, String> fingerprint) {
        if (sourceOperationId == null) return Map.of();
        List<Ancestor> chain = ancestors(batch, store, ledger, fingerprint);
        var owners = chain.stream().filter(a -> BatchBook.list(a.batch().getSelectiveRuns()).stream()
                .anyMatch(r -> sourceOperationId.equals(r.id()))).toList();
        if (owners.size() != 1) throw new IllegalArgumentException(
                "La referencia didáctica debe identificar una única pasada de esta corrección o de sus antecesoras explícitas.");
        Ancestor owner = owners.get(0);
        CorrectionBatch source = owner.batch();
        var runs = BatchBook.list(source.getSelectiveRuns()).stream().filter(r -> sourceOperationId.equals(r.id())).toList();
        if (runs.size() != 1 || runs.get(0).finishedAt() == null
                || !"recorrect".equals(runs.get(0).action())
                || !Set.of("DONE", "PARTIAL").contains(runs.get(0).status())
                || runs.get(0).contentVersion() < 1
                || runs.get(0).resultContentVersion() < runs.get(0).contentVersion()
                || runs.get(0).resultContentVersion() > source.getContentVersion())
            throw new IllegalArgumentException("La referencia didáctica debe ser una pasada terminada anterior al contenido conservado.");
        var run = runs.get(0);
        for (CorrectionBatch child : owner.children()) {
            if (child.getOpenedAt() != null && run.finishedAt().isAfter(child.getOpenedAt()))
                throw new IllegalArgumentException("La pasada de referencia es posterior a un eslabón de la corrección.");
            for (String id : quizzes) if (BatchBook.place(child.getEvolution().sourceCourse(), id).isEmpty())
                throw new IllegalArgumentException("El quiz no forma parte del contenido heredado por " + child.getId() + ": " + id);
        }
        var result = new LinkedHashMap<String, DidacticReference>();
        for (String id : quizzes) {
            var snapshots = BatchBook.list(source.getComparisonBaselines()).stream()
                    .filter(s -> sourceOperationId.equals(s.operationId()) && id.equals(s.quizId())).toList();
            if (!BatchBook.list(run.quizIds()).contains(id) || snapshots.size() != 1
                    || snapshots.get(0).sourceContentVersion() != run.contentVersion()
                    || snapshots.get(0).before() == null || snapshots.get(0).before().getQuiz() == null
                    || !id.equals(snapshots.get(0).before().getQuiz().getId())
                    || !id.equals(snapshots.get(0).before().getNodeId())
                    || snapshots.get(0).before().getNodeTarget() != com.learney.contentaudit.auditdomain.AuditTarget.QUIZ)
                throw new IllegalArgumentException("Falta una referencia didáctica exacta y única para el quiz " + id);
            var quiz = snapshots.get(0).before().getQuiz();
            var reference = com.learney.contentaudit.revisiondomain.panelengine.DidacticContracts.reference(
                    sourceOperationId, run.contentVersion(), id, quiz, restoreRequestFormula);
            result.put(id, new DidacticReference(reference.sourceOperationId(), reference.sourceContentVersion(), id,
                    quiz, reference.restoreRequestFormula(), reference.operatorInstruction(),
                    new Provenance(source.getId(), run.fingerprint(), hash(quiz), owner.path())));
        }
        return Map.copyOf(result);
    }

    private static List<Ancestor> ancestors(CorrectionBatch selected, BatchStore store, BatchChangeLedger ledger,
            Function<CourseEntity, String> fingerprint) {
        var result = new ArrayList<Ancestor>();
        var children = new ArrayList<CorrectionBatch>();
        var path = new ArrayList<AncestryStep>();
        var visited = new HashSet<String>();
        CorrectionBatch cursor = selected;
        while (true) {
            if (cursor.getId() == null || !visited.add(cursor.getId()))
                throw new IllegalArgumentException("La cadena de correcciones contiene un ciclo o una identidad inválida.");
            result.add(new Ancestor(cursor, List.copyOf(children), List.copyOf(path)));
            var link = cursor.getEvolution();
            if (link == null) return List.copyOf(result);
            if (link.parentBatchId() == null || visited.contains(link.parentBatchId()))
                throw new IllegalArgumentException("La cadena de correcciones contiene un ciclo o un padre inválido.");
            CorrectionBatch parent = store.load(link.parentBatchId()).orElseThrow(() ->
                    new IllegalArgumentException("Falta la corrección antecesora " + link.parentBatchId()));
            if (!link.parentBatchId().equals(parent.getId()) || parent.getLevel() != cursor.getLevel()
                    || parent.getContentVersion() != link.parentContentVersion()
                    || link.parentFingerprint() == null || link.parentFingerprint().isBlank()
                    || link.baseCourse() == null || link.sourceCourse() == null
                    || !link.parentFingerprint().equals(fingerprint.apply(link.sourceCourse()))
                    || !link.parentFingerprint().equals(fingerprint.apply(ledger.composedCourse(parent, link.baseCourse()))))
                throw new IllegalArgumentException("La referencia antecesora no coincide con la versión y huella conservadas por " + cursor.getId());
            path.add(new AncestryStep(cursor.getId(), parent.getId(), link.parentContentVersion(), link.parentFingerprint()));
            children.add(cursor);
            cursor = parent;
        }
    }

    private static String hash(Object value) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(JSON.writeValueAsBytes(value)));
        } catch (java.io.IOException | java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("No se pudo conservar la huella del contrato didáctico.", e);
        }
    }
}
