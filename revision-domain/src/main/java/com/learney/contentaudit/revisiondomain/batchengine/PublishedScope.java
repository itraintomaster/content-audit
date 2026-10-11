package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.batch.BatchChange;
import com.learney.contentaudit.revisiondomain.batch.BatchChangeStep;
import com.learney.contentaudit.revisiondomain.batch.CorrectionBatch;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * F-TANDA-R009 y F-PUBPROD-R001: lo que los pasos que quedan en la tanda declaran sobre un ejercicio o un tema —las
 * partes, por cambio y todas juntas, y si el nodo es un ejercicio o un tema—. Es lo mismo que mira la preservación al
 * publicar y lo que arma la lista: los dos leen las partes de la misma manera.
 */
final class PublishedScope {

    private final String nodeId;

    private final AuditTarget target;

    private final Map<String, EnumSet<ExercisePart>> partsByChange;

    private PublishedScope(String nodeId, AuditTarget target, Map<String, EnumSet<ExercisePart>> partsByChange) {
        this.nodeId = nodeId;
        this.target = target;
        this.partsByChange = partsByChange;
    }

    /**
     * Las partes que declaran los pasos de los cambios activos sobre {@code nodeId}. La oración guardada y lo que se oye
     * siguen a lo que dice el ejercicio: un paso que toca el enunciado, las respuestas, las opciones o el tipo de
     * ejercicio también los declara.
     */
    static PublishedScope of(CorrectionBatch batch, String nodeId) {
        Map<String, EnumSet<ExercisePart>> byChange = new LinkedHashMap<>();
        AuditTarget target = AuditTarget.QUIZ;
        for (BatchChange change : BatchBook.list(batch.getChanges())) {
            if (change == null || change.getDroppedReason() != null) {
                continue;
            }
            for (BatchChangeStep step : BatchBook.list(change.getSteps())) {
                if (step == null || !Objects.equals(step.getQuizId(), nodeId)) {
                    continue;
                }
                EnumSet<ExercisePart> parts = byChange.computeIfAbsent(change.getChangeId(),
                        id -> EnumSet.noneOf(ExercisePart.class));
                parts.addAll(BatchBook.list(step.getParts()));
                if (step.getAfter() != null && step.getAfter().getNodeTarget() == AuditTarget.KNOWLEDGE) {
                    target = AuditTarget.KNOWLEDGE;
                }
            }
        }
        for (EnumSet<ExercisePart> parts : byChange.values()) {
            withWhatFollowsTheExercise(parts);
        }
        return new PublishedScope(nodeId, target, byChange);
    }

    private static void withWhatFollowsTheExercise(Set<ExercisePart> parts) {
        if (parts.contains(ExercisePart.STATEMENT) || parts.contains(ExercisePart.ANSWERS)
                || parts.contains(ExercisePart.OPTIONS) || parts.contains(ExercisePart.FORM_KIND)) {
            parts.add(ExercisePart.STORED_SENTENCE); // la oración guardada sigue a lo que dice el ejercicio
            parts.add(ExercisePart.AUDIO);
        }
    }

    String nodeId() {
        return nodeId;
    }

    AuditTarget target() {
        return target;
    }

    /** Las partes de cada cambio activo que tiene pasos sobre el nodo, en el orden de la tanda. */
    Map<String, Set<ExercisePart>> partsByChange() {
        Map<String, Set<ExercisePart>> copy = new LinkedHashMap<>();
        partsByChange.forEach((changeId, parts) -> copy.put(changeId,
                Collections.unmodifiableSet(EnumSet.copyOf(parts))));
        return Collections.unmodifiableMap(copy);
    }

    /** Todas las partes declaradas sobre el nodo. */
    Set<ExercisePart> parts() {
        Set<ExercisePart> all = EnumSet.noneOf(ExercisePart.class);
        partsByChange.values().forEach(all::addAll);
        return all;
    }

    /** El nodo tal como está en {@code course}, o vacío si ahí no está. */
    Optional<CourseElementSnapshot> snapshot(CourseEntity course) {
        Optional<BatchBook.Place> place = BatchBook.place(course, nodeId);
        if (place.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(target == AuditTarget.KNOWLEDGE
                ? new CourseElementSnapshot(AuditTarget.KNOWLEDGE, nodeId, null, place.get().knowledge())
                : new CourseElementSnapshot(AuditTarget.QUIZ, nodeId, place.get().quiz(), null));
    }
}
