package com.learney.contentaudit.revisiondomain.preservationengine;

import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.revisiondomain.preservation.FieldWithoutProductionPlaceException;
import com.learney.contentaudit.revisiondomain.preservation.ProductionFieldMap;
import com.learney.contentaudit.revisiondomain.preservation.ProductionPlace;
import com.learney.contentaudit.revisiondomain.preservation.ScopedField;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * F-PUBPROD-R002: la tabla de los campos de la corrección ({@link DefaultCorrectionScope}) a producción. Cada fila es
 * exacta —ruta, tipo de parte y destino—: un campo que la corrección empiece a declarar sin su fila acá no tiene lugar y
 * frena la publicación (F-PUBPROD-R001, inv. 5).
 */
public class DefaultProductionFieldMap implements ProductionFieldMap {

    static final String BY_PLAN = "viaja por el camino por plan";

    static final String RE_RECORDING = "se regraba (F-TANDA-R012)";

    static final String EXERCISE_TYPE_CHANGE = "cambio de tipo de ejercicio: no se publica por script";

    private record Key(String path, String elementKind, AuditTarget target) {
    }

    private static final Map<Key, ProductionPlace> PLACES;

    static {
        Map<Key, ProductionPlace> places = new LinkedHashMap<>();
        ProductionPlace sentenceParts = quiz("form.sentenceParts");
        // Del ejercicio.
        places.put(new Key("sentences", null, AuditTarget.QUIZ), quiz("form.sentences"));
        places.put(new Key("translation", null, AuditTarget.QUIZ), quiz("translation"));
        places.put(new Key("instructions", null, AuditTarget.QUIZ), quiz("instructions"));
        places.put(new Key("miniTheory", null, AuditTarget.QUIZ), quiz("miniTheory"));
        // Las partes del enunciado viajan enteras: su composición, el texto de las partes de texto y las respuestas de
        // los huecos son el mismo arreglo en producción.
        places.put(new Key("form.sentenceParts[]", null, AuditTarget.QUIZ), sentenceParts);
        places.put(new Key("form.sentenceParts[].text", "TEXT", AuditTarget.QUIZ), sentenceParts);
        places.put(new Key("form.sentenceParts[].options", "CLOZE", AuditTarget.QUIZ), sentenceParts);
        // La opción múltiple viaja entera: la forma de elegir y las opciones.
        places.put(new Key("form.multipleChoice", null, AuditTarget.QUIZ), quiz("form.selection", "form.items"));
        places.put(new Key(DefaultCorrectionScope.AUDIO_PERMISSION, null, AuditTarget.QUIZ),
                new ProductionPlace.NotWritten(RE_RECORDING));
        places.put(new Key("kind", null, AuditTarget.QUIZ),
                new ProductionPlace.Unsupported(EXERCISE_TYPE_CHANGE, List.of("kind")));
        places.put(new Key("form.kind", null, AuditTarget.QUIZ),
                new ProductionPlace.Unsupported(EXERCISE_TYPE_CHANGE, List.of("form.kind")));
        // Del tema.
        places.put(new Key("instructions", null, AuditTarget.KNOWLEDGE),
                new ProductionPlace.Write(KNOWLEDGE_COLLECTION, List.of("instructions")));
        places.put(new Key("label", null, AuditTarget.KNOWLEDGE), new ProductionPlace.NotWritten(BY_PLAN));
        places.put(new Key("quizTemplates[].title", null, AuditTarget.KNOWLEDGE),
                new ProductionPlace.NotWritten(BY_PLAN));
        PLACES = Collections.unmodifiableMap(places);
    }

    private static ProductionPlace quiz(String... mongoPaths) {
        return new ProductionPlace.Write(QUIZ_COLLECTION, List.of(mongoPaths));
    }

    @Override
    public ProductionPlace placeOf(ScopedField field, AuditTarget target) {
        if (field == null || target == null) {
            throw new FieldWithoutProductionPlaceException(field, target);
        }
        ProductionPlace place = PLACES.get(new Key(field.getPath(), field.getElementKind(), target));
        if (place == null) {
            throw new FieldWithoutProductionPlaceException(field, target);
        }
        return place;
    }
}
