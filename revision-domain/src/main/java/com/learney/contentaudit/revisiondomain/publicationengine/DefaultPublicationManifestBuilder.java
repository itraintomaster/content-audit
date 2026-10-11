package com.learney.contentaudit.revisiondomain.publicationengine;

import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.coursedomain.FormKind;
import com.learney.contentaudit.coursedomain.ProductionDocumentProjector;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.preservation.CorrectionScope;
import com.learney.contentaudit.revisiondomain.preservation.ScopedField;
import com.learney.contentaudit.revisiondomain.publication.ManifestCounts;
import com.learney.contentaudit.revisiondomain.publication.ManifestDocument;
import com.learney.contentaudit.revisiondomain.publication.ManifestField;
import com.learney.contentaudit.revisiondomain.publication.ManifestUnsupported;
import com.learney.contentaudit.revisiondomain.publication.ProductionFieldMap;
import com.learney.contentaudit.revisiondomain.publication.ProductionPlace;
import com.learney.contentaudit.revisiondomain.publication.PublicationManifest;
import com.learney.contentaudit.revisiondomain.publication.PublicationManifestBuilder;
import com.learney.contentaudit.revisiondomain.publication.PublicationManifestRequest;
import com.learney.contentaudit.revisiondomain.publication.PublishedElement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * F-PUBPROD-R001 y R002: la lista sale de la misma tabla de partes a campos que decide qué puede cambiar una corrección
 * ({@link CorrectionScope}) y de dónde guarda producción cada campo ({@link ProductionFieldMap}). Los valores salen del
 * curso tal como lo guarda producción ({@link ProductionDocumentProjector}): el antes, del curso del que partió la
 * tanda; el después, del curso que se publica. Un campo que no cambió no está; un ejercicio cuyo tipo cambió no va, y
 * queda declarado aparte. De un ejercicio de opción múltiple —lo dice su propio tipo— sólo viajan, de su formulario, el
 * texto de cada opción (R002 inv. 4): si cambió cualquier otra cosa del formulario tampoco va, y queda declarado aparte;
 * su traducción, su consigna y su miniteoría viajan como las de cualquier ejercicio.
 */
public class DefaultPublicationManifestBuilder implements PublicationManifestBuilder {

    /** El orden en que la lista nombra las rutas, para que dos listas iguales se escriban igual. */
    private static final List<String> CANONICAL_ORDER = List.of("form.sentences", "translation", "instructions",
            "miniTheory", "form.sentenceParts", "form.selection", "form.items");

    private static final Comparator<String> BY_CANONICAL_ORDER = Comparator
            .comparingInt((String path) -> CANONICAL_ORDER.contains(path) ? CANONICAL_ORDER.indexOf(path)
                    : CANONICAL_ORDER.size())
            .thenComparing(Comparator.naturalOrder());

    /** Las dos rutas de la opción múltiple en producción. */
    private static final String SELECTION = "form.selection";

    private static final String ITEMS = "form.items";

    /** El formulario en producción: de una opción múltiple, sólo el texto de sus opciones se publica por script. */
    private static final String FORM = "form.";

    /** La etiqueta de una opción: lo único de la opción múltiple que se publica por script. */
    private static final String LABEL = "label";

    static final String MULTIPLE_CHOICE_BEYOND_LABEL = "opción múltiple: sólo se publica el texto de cada opción; "
            + "cambiar el enunciado, la oración guardada, cuál es la correcta, la forma de elegir o las opciones no se "
            + "publica por script";

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
        requireOrigin(request);
        List<ManifestDocument> documents = new ArrayList<>();
        List<ManifestUnsupported> unsupported = new ArrayList<>();
        for (PublishedElement element : request.elements()) {
            document(element, unsupported).ifPresent(documents::add);
        }
        Map<String, Integer> byPath = new TreeMap<>(BY_CANONICAL_ORDER);
        int fields = 0;
        for (ManifestDocument document : documents) {
            for (ManifestField field : document.fields()) {
                byPath.merge(field.path(), 1, Integer::sum);
                fields++;
            }
        }
        return new PublicationManifest(PublicationManifest.SCHEMA, request.batchId(), request.planId(),
                request.level(), request.publishedAt(), request.approvedBy(), request.source(), request.verdict(),
                request.reviews(), documents, unsupported, request.excluded(),
                new ManifestCounts(documents.size(), fields, byPath));
    }

    /**
     * R001 inv. 7: la lista dice de qué tanda, plan y nivel sale, cuándo se publicó y quién la aprobó. Sin alguno de esos
     * datos el paso a producción no sabría qué está escribiendo: la lista no se arma y la negativa nombra lo que falta.
     */
    private static void requireOrigin(PublicationManifestRequest request) {
        List<String> missing = new ArrayList<>();
        if (blank(request.batchId())) {
            missing.add("la tanda");
        }
        if (blank(request.planId())) {
            missing.add("el plan");
        }
        if (blank(request.level())) {
            missing.add("el nivel");
        }
        if (blank(request.publishedAt())) {
            missing.add("cuándo se publica");
        }
        if (blank(request.approvedBy())) {
            missing.add("quién la aprobó");
        }
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("a la lista de publicación le falta " + String.join(", ", missing)
                    + ": sin eso el paso a producción no sabe qué escribe");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
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
        // De cada ruta, el primer cambio que la declaró y con qué parte: a ése se le atribuye si no se puede publicar.
        Map<String, ManifestUnsupported> declaredBy = new LinkedHashMap<>();
        boolean held = false;
        for (Map.Entry<String, Set<ExercisePart>> step : element.partsByChange().entrySet()) {
            String changeId = step.getKey();
            for (ExercisePart part : ExercisePart.values()) {
                if (!step.getValue().contains(part)) {
                    continue;
                }
                for (ScopedField field : correctionScope.changeableFields(Set.of(part), target)) {
                    ProductionPlace place = fieldMap.placeOf(field, target);
                    if (place instanceof ProductionPlace.Write write) {
                        collection = write.collection();
                        writePaths.addAll(write.mongoPaths());
                        for (String path : write.mongoPaths()) {
                            declaredBy.putIfAbsent(path, new ManifestUnsupported(changeId, nodeId, part,
                                    MULTIPLE_CHOICE_BEYOND_LABEL));
                        }
                    } else if (place instanceof ProductionPlace.Unsupported notByScript
                            && changed(element, target, notByScript.detectionPaths())) {
                        held = true;
                        ManifestUnsupported entry = new ManifestUnsupported(changeId, nodeId, part,
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
        boolean multipleChoice = target == AuditTarget.QUIZ && (multipleChoice(element.before())
                || multipleChoice(element.after()));
        List<ManifestField> fields = new ArrayList<>();
        List<ManifestUnsupported> notByScript = new ArrayList<>();
        for (String path : ordered) {
            if (Objects.equals(before.get(path), after.get(path))) {
                continue;
            }
            boolean multipleChoicePath = SELECTION.equals(path) || ITEMS.equals(path);
            if ((multipleChoice && path.startsWith(FORM)) || multipleChoicePath) {
                if (!ITEMS.equals(path) || !onlyLabelsChanged(before.get(ITEMS), after.get(ITEMS))) {
                    ManifestUnsupported entry = declaredBy.get(path);
                    if (!notByScript.contains(entry)) {
                        notByScript.add(entry);
                    }
                    continue;
                }
            }
            fields.add(new ManifestField(path, before.get(path), after.get(path)));
        }
        if (!notByScript.isEmpty()) {
            // R002 inv. 4: el ejercicio no va, y cada cambio que tocó lo que no se publica queda declarado aparte.
            for (ManifestUnsupported entry : notByScript) {
                if (!unsupported.contains(entry)) {
                    unsupported.add(entry);
                }
            }
            return Optional.empty();
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
        List<String> changeIds = new ArrayList<>(element.partsByChange().keySet());
        return Optional.of(new ManifestDocument(collection, id, nodeId, changeIds, proposalIds,
                element.reviewStatus(), fields));
    }

    /** R002 inv. 4: si el ejercicio es de opción múltiple, por su propio tipo o por el de su formulario. */
    private static boolean multipleChoice(CourseElementSnapshot snapshot) {
        QuizTemplateEntity quiz = snapshot.getQuiz();
        return quiz != null && (FormKind.from(quiz.getKind()) == FormKind.MULTIPLE_CHOICE
                || (quiz.getForm() != null && FormKind.from(quiz.getForm().getKind()) == FormKind.MULTIPLE_CHOICE));
    }

    /**
     * R002 inv. 4: la misma cantidad de opciones y cada opción igual salvo su etiqueta. El identificador de una opción
     * es la etiqueta original en minúscula y la incidencia marca la correcta: ninguno de los dos viaja por script.
     */
    private static boolean onlyLabelsChanged(Object itemsBefore, Object itemsAfter) {
        if (Objects.equals(itemsBefore, itemsAfter)) {
            return true;
        }
        if (!(itemsBefore instanceof List<?> oldItems) || !(itemsAfter instanceof List<?> newItems)
                || oldItems.size() != newItems.size()) {
            return false;
        }
        for (int i = 0; i < oldItems.size(); i++) {
            if (!(oldItems.get(i) instanceof Map<?, ?> oldItem) || !(newItems.get(i) instanceof Map<?, ?> newItem)) {
                return false;
            }
            Map<Object, Object> oldRest = new LinkedHashMap<>(oldItem);
            Map<Object, Object> newRest = new LinkedHashMap<>(newItem);
            oldRest.remove(LABEL);
            newRest.remove(LABEL);
            if (!oldRest.equals(newRest)) {
                return false;
            }
        }
        return true;
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
