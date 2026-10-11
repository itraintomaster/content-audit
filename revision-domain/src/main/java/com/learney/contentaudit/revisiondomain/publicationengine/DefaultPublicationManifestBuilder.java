package com.learney.contentaudit.revisiondomain.publicationengine;

import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.coursedomain.ProductionDocumentProjector;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.preservation.CorrectionScope;
import com.learney.contentaudit.revisiondomain.preservation.ProductionFieldMap;
import com.learney.contentaudit.revisiondomain.preservation.ProductionPlace;
import com.learney.contentaudit.revisiondomain.preservation.ScopedField;
import com.learney.contentaudit.revisiondomain.publication.ManifestCounts;
import com.learney.contentaudit.revisiondomain.publication.ManifestDocument;
import com.learney.contentaudit.revisiondomain.publication.ManifestField;
import com.learney.contentaudit.revisiondomain.publication.ManifestUnsupported;
import com.learney.contentaudit.revisiondomain.publication.PublicationManifest;
import com.learney.contentaudit.revisiondomain.publication.PublicationManifestBuilder;
import com.learney.contentaudit.revisiondomain.publication.PublicationManifestRequest;
import com.learney.contentaudit.revisiondomain.publication.PublishedElement;
import com.learney.contentaudit.revisiondomain.publication.PublishedStep;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * F-PUBPROD-R001 y R002: la lista sale de la misma tabla de partes a campos que decide qué puede cambiar una corrección
 * ({@link CorrectionScope}) y de dónde guarda producción cada campo ({@link ProductionFieldMap}). Los valores salen del
 * curso tal como lo guarda producción ({@link ProductionDocumentProjector}): el antes, del curso del que partió la
 * tanda; el después, del curso que se publica. Un campo que no cambió no está; un ejercicio cuyo tipo cambió no va, y
 * queda declarado aparte.
 */
public class DefaultPublicationManifestBuilder implements PublicationManifestBuilder {

    /** El orden en que la lista nombra las rutas, para que dos listas iguales se escriban igual. */
    private static final List<String> CANONICAL_ORDER = List.of("form.sentences", "translation", "instructions",
            "miniTheory", "form.sentenceParts", "form.selection", "form.items");

    private static final Comparator<String> BY_CANONICAL_ORDER = Comparator
            .comparingInt((String path) -> CANONICAL_ORDER.contains(path) ? CANONICAL_ORDER.indexOf(path)
                    : CANONICAL_ORDER.size())
            .thenComparing(Comparator.naturalOrder());

    private final CorrectionScope correctionScope;

    private final ProductionFieldMap fieldMap;

    private final ProductionDocumentProjector projector;

    public DefaultPublicationManifestBuilder(CorrectionScope correctionScope, ProductionFieldMap fieldMap,
            ProductionDocumentProjector projector) {
        this.correctionScope = Objects.requireNonNull(correctionScope, "correctionScope");
        this.fieldMap = Objects.requireNonNull(fieldMap, "fieldMap");
        this.projector = Objects.requireNonNull(projector, "projector");
    }

    @Override
    public PublicationManifest build(PublicationManifestRequest request) {
        List<ManifestDocument> documents = new ArrayList<>();
        List<ManifestUnsupported> unsupported = new ArrayList<>();
        for (PublishedElement element : request.elements()) {
            document(element, unsupported).ifPresent(documents::add);
        }
        Map<String, Integer> byPath = new LinkedHashMap<>();
        int fields = 0;
        List<String> paths = new ArrayList<>();
        for (ManifestDocument document : documents) {
            for (ManifestField field : document.fields()) {
                if (!paths.contains(field.path())) {
                    paths.add(field.path());
                }
                fields++;
            }
        }
        paths.sort(BY_CANONICAL_ORDER);
        for (String path : paths) {
            byPath.put(path, (int) documents.stream().flatMap(d -> d.fields().stream())
                    .filter(f -> path.equals(f.path())).count());
        }
        return new PublicationManifest(PublicationManifest.SCHEMA, request.batchId(), request.planId(),
                request.level(), request.publishedAt(), request.approvedBy(), request.source(), request.verdict(),
                request.reviews(), documents, unsupported, request.excluded(),
                new ManifestCounts(documents.size(), fields, byPath));
    }

    /** El documento de un ejercicio o tema, o ninguno si nada de lo que se lleva cambió o si su tipo cambió. */
    private Optional<ManifestDocument> document(PublishedElement element,
            List<ManifestUnsupported> unsupported) {
        if (element.before() == null || element.after() == null) {
            // Sin el antes o sin el después no hay con qué comparar-y-fijar: la lista no se deja (inv. 3).
            throw new IllegalArgumentException("falta el antes o el después de "
                    + (element.after() != null ? element.after().getNodeId()
                            : element.before() != null ? element.before().getNodeId() : "un ejercicio")
                    + " para armar la lista de publicación");
        }
        CourseElementSnapshot reference = element.after();
        AuditTarget target = reference.getNodeTarget() == AuditTarget.KNOWLEDGE ? AuditTarget.KNOWLEDGE
                : AuditTarget.QUIZ;
        String nodeId = reference.getNodeId();
        String collection = null;
        Set<String> writePaths = new LinkedHashSet<>();
        boolean held = false;
        List<String> changeIds = new ArrayList<>();
        for (PublishedStep step : element.steps()) {
            if (!changeIds.contains(step.changeId())) {
                changeIds.add(step.changeId());
            }
            for (ExercisePart part : ExercisePart.values()) {
                if (!step.parts().contains(part)) {
                    continue;
                }
                for (ScopedField field : correctionScope.changeableFields(Set.of(part), target)) {
                    ProductionPlace place = fieldMap.placeOf(field, target);
                    if (place instanceof ProductionPlace.Write write) {
                        collection = write.collection();
                        writePaths.addAll(write.mongoPaths());
                    } else if (place instanceof ProductionPlace.Unsupported notByScript
                            && changed(element, target, notByScript.detectionPaths())) {
                        held = true;
                        ManifestUnsupported entry = new ManifestUnsupported(step.changeId(), nodeId, part,
                                notByScript.reason());
                        if (!unsupported.contains(entry)) {
                            unsupported.add(entry);
                        }
                    }
                    // ProductionPlace.NotWritten: va por otro camino; la lista no lo lleva.
                }
            }
        }
        if (held || writePaths.isEmpty()) {
            return Optional.empty();
        }
        List<String> ordered = new ArrayList<>(writePaths);
        ordered.sort(BY_CANONICAL_ORDER);
        Map<String, Object> before = project(element.before(), target, ordered);
        Map<String, Object> after = project(element.after(), target, ordered);
        List<ManifestField> fields = new ArrayList<>();
        for (String path : ordered) {
            if (!Objects.equals(before.get(path), after.get(path))) {
                fields.add(new ManifestField(path, before.get(path), after.get(path)));
            }
        }
        if (fields.isEmpty()) {
            return Optional.empty();
        }
        String id = documentId(element.after(), target);
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("el ejercicio o tema " + nodeId
                    + " no tiene identificador de producción: la lista no se puede dejar");
        }
        List<String> proposalIds = element.proposalId() != null ? List.of(element.proposalId()) : List.of();
        return Optional.of(new ManifestDocument(collection, id, nodeId, changeIds, proposalIds,
                element.reviewStatus(), fields));
    }

    private boolean changed(PublishedElement element, AuditTarget target, List<String> paths) {
        return !Objects.equals(project(element.before(), target, paths), project(element.after(), target, paths));
    }

    private Map<String, Object> project(CourseElementSnapshot snapshot, AuditTarget target, List<String> paths) {
        if (target == AuditTarget.KNOWLEDGE) {
            return projector.project(snapshot.getKnowledge(), paths);
        }
        return projector.project(snapshot.getQuiz(), paths);
    }

    private String documentId(CourseElementSnapshot snapshot, AuditTarget target) {
        return target == AuditTarget.KNOWLEDGE ? projector.documentId(snapshot.getKnowledge())
                : projector.documentId(snapshot.getQuiz());
    }
}
