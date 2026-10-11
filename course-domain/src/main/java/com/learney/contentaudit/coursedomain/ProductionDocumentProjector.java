package com.learney.contentaudit.coursedomain;

import java.util.List;
import java.util.Map;

/**
 * F-PUBPROD-R001 y R002: cómo guarda producción un ejercicio o un tema, campo por campo. Devuelve, para cada ruta
 * pedida (por ejemplo {@code form.sentenceParts}), el valor tal como lo guarda el curso en el disco, en JSON simple: los
 * números como números (nunca {@code {"$numberDouble": …}}), el texto de una parte del enunciado sin texto como texto
 * vacío, y la oración guardada ausente como lista vacía. Una ruta que el documento no tiene vale {@code null}.
 *
 * <p>La lista de publicación lleva estos valores como el antes y el después: el paso a producción nunca vuelve a leer el
 * curso del disco.
 */
public interface ProductionDocumentProjector {

    /** Los valores de {@code mongoPaths} del ejercicio, en ese orden. */
    Map<String, Object> project(QuizTemplateEntity quiz, List<String> mongoPaths);

    /** Los valores de {@code mongoPaths} del tema, en ese orden. */
    Map<String, Object> project(KnowledgeEntity knowledge, List<String> mongoPaths);

    /** El identificador del ejercicio en producción, en hexadecimal. */
    String documentId(QuizTemplateEntity quiz);

    /** El identificador del tema en producción, en hexadecimal. */
    String documentId(KnowledgeEntity knowledge);
}
