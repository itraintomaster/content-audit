package com.learney.contentaudit.revisiondomain.preservation;

import com.learney.contentaudit.auditdomain.AuditTarget;
import java.util.Set;

/**
 * F-PUBPROD-R002: dónde guarda producción cada campo de la tabla de la corrección ({@link CorrectionScope},
 * F-TAREA-R006). Un campo sin lugar declarado no se adivina: {@link #placeOf} lanza
 * {@link FieldWithoutProductionPlaceException} y la publicación se frena nombrándolo (F-PUBPROD-R001, inv. 5).
 */
public interface ProductionFieldMap {

    /** La colección de los ejercicios en producción. */
    String QUIZ_COLLECTION = "quizTemplates";

    /** La colección de los temas en producción. */
    String KNOWLEDGE_COLLECTION = "knowledges";

    /**
     * F-PUBPROD-R002, inv. 1: el identificador, el tipo de ejercicio y el tipo, la incidencia, la etiqueta y el nombre
     * del formulario nunca se escriben.
     */
    Set<String> FORBIDDEN_PATHS = Set.of("_id", "kind", "form.kind", "form.incidence", "form.label", "form.name");

    ProductionPlace placeOf(ScopedField field, AuditTarget target);
}
