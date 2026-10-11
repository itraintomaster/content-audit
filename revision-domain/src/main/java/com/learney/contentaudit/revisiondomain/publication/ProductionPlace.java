package com.learney.contentaudit.revisiondomain.publication;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * F-PUBPROD-R002: qué hace producción con un campo que una corrección puede cambiar. Hay tres respuestas y ninguna más:
 * se escribe en estas rutas de esta colección ({@link Write}); no se lleva por este camino, con su motivo
 * ({@link NotWritten}); o no se puede llevar por script y retiene el ejercicio entero ({@link Unsupported}).
 */
public sealed interface ProductionPlace permits ProductionPlace.Write, ProductionPlace.NotWritten,
        ProductionPlace.Unsupported {

    /** Las colecciones de producción que la lista puede nombrar (esquema 1). */
    Set<String> COLLECTIONS = Set.of(ProductionFieldMap.QUIZ_COLLECTION, ProductionFieldMap.KNOWLEDGE_COLLECTION);

    /**
     * El campo se escribe en {@code mongoPaths} de {@code collection}. Nunca en una ruta prohibida
     * ({@link ProductionFieldMap#FORBIDDEN_PATHS}): armar uno así es un error, no una decisión.
     */
    record Write(String collection, List<String> mongoPaths) implements ProductionPlace {
        public Write {
            Objects.requireNonNull(collection, "collection");
            if (!COLLECTIONS.contains(collection)) {
                throw new IllegalArgumentException("colección de producción desconocida: " + collection);
            }
            mongoPaths = List.copyOf(Objects.requireNonNull(mongoPaths, "mongoPaths"));
            if (mongoPaths.isEmpty()) {
                throw new IllegalArgumentException("un campo que se escribe necesita al menos una ruta");
            }
            for (String path : mongoPaths) {
                if (ProductionFieldMap.FORBIDDEN_PATHS.contains(path)) {
                    throw new IllegalArgumentException("la ruta «" + path + "» nunca se escribe en producción");
                }
            }
        }
    }

    /** El campo no se lleva por la lista: va por otro camino, que {@code reason} nombra. */
    record NotWritten(String reason) implements ProductionPlace {
        public NotWritten {
            Objects.requireNonNull(reason, "reason");
        }
    }

    /**
     * El campo cambió y no se puede llevar por script ({@code reason}); {@code detectionPaths} son las rutas que dicen si
     * cambió, que se leen pero nunca se escriben. El ejercicio que lo cambió no va en la lista (F-PUBPROD-R001, inv. 6).
     */
    record Unsupported(String reason, List<String> detectionPaths) implements ProductionPlace {
        public Unsupported {
            Objects.requireNonNull(reason, "reason");
            detectionPaths = List.copyOf(Objects.requireNonNull(detectionPaths, "detectionPaths"));
            if (detectionPaths.isEmpty()) {
                throw new IllegalArgumentException("un campo sin lugar necesita al menos una ruta para ver si cambió");
            }
        }
    }
}
