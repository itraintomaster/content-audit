package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.revisiondomain.batch.BatchReview;
import java.util.List;

/**
 * F-TANDA-R007 y F-PUBPROD-R003: cómo está hoy la revisión de José sobre los pasos activos de la tanda. Cada paso trae
 * su decisión vigente —la última, y sólo si sigue coincidiendo con el antes y el después del paso; si no, sin revisar—.
 * Aparte, los rechazos vencidos: la última decisión sobre un paso es un rechazo, pero el contenido cambió después y ya
 * no coincide.
 *
 * @param items los pasos activos, en el orden de la tanda, con su decisión vigente
 * @param staleRejections los pasos activos cuya última decisión es un rechazo que ya no coincide con el contenido
 */
record ReviewStanding(List<BatchReview.Item> items, List<StaleRejection> staleRejections) {

    ReviewStanding {
        items = List.copyOf(items);
        staleRejections = List.copyOf(staleRejections);
    }

    /** Un rechazo de José que quedó sobre un contenido que cambió después. */
    record StaleRejection(String changeId, String nodeId) {
    }
}
