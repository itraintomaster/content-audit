package com.learney.contentaudit.revisiondomain.publicationengine;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * F-PUBPROD-R002 inv. 4: una opción múltiple bien armada, como la guarda producción —un solo hueco sin respuestas
 * escritas; de dos a ocho opciones, cada una con texto e identificador, sin repetir; una sola correcta, y elección de
 * una sola opción—. Mira el formulario proyectado ({@code form.sentenceParts}, {@code form.items},
 * {@code form.selection}), que es lo que viaja a producción.
 */
final class WellFormedMultipleChoice {

    private static final String GAP = "CLOZE";

    private static final String SINGLE = "SINGLE";

    private static final int FEWEST = 2;

    private static final int MOST = 8;

    private WellFormedMultipleChoice() {
    }

    /** Qué le falta para estar bien armada, o vacío si lo está. */
    static Optional<String> problem(Object parts, Object items, Object selection) {
        Optional<String> gap = gapProblem(parts);
        if (gap.isPresent()) {
            return gap;
        }
        if (!SINGLE.equals(selection)) {
            return Optional.of("elige más de una (" + selection + "); producción elige una sola");
        }
        return itemsProblem(items);
    }

    private static Optional<String> gapProblem(Object parts) {
        List<?> list = parts instanceof List<?> given ? given : List.of();
        long gaps = list.stream().filter(WellFormedMultipleChoice::isGap).count();
        if (gaps != 1) {
            return Optional.of("tiene " + gaps + (gaps == 1 ? " hueco" : " huecos") + "; lleva uno");
        }
        boolean written = list.stream().filter(WellFormedMultipleChoice::isGap)
                .anyMatch(part -> ((Map<?, ?>) part).get("options") instanceof List<?> answers && !answers.isEmpty());
        return written ? Optional.of("el hueco trae respuestas escritas") : Optional.empty();
    }

    private static boolean isGap(Object part) {
        return part instanceof Map<?, ?> map && GAP.equals(map.get("kind"));
    }

    private static Optional<String> itemsProblem(Object items) {
        if (!(items instanceof List<?> list)) {
            return Optional.of("no tiene opciones");
        }
        if (list.size() < FEWEST || list.size() > MOST) {
            return Optional.of("tiene " + list.size() + (list.size() == 1 ? " opción" : " opciones")
                    + "; van de dos a ocho");
        }
        Set<String> ids = new HashSet<>();
        Set<String> labels = new HashSet<>();
        int correct = 0;
        for (Object item : list) {
            Optional<String> wrong = itemProblem(item, ids, labels);
            if (wrong.isPresent()) {
                return wrong;
            }
            correct += ((Number) ((Map<?, ?>) item).get("incidence")).doubleValue() == 1.0 ? 1 : 0;
        }
        return correct == 1 ? Optional.empty()
                : Optional.of("tiene " + correct + (correct == 1 ? " correcta" : " correctas") + "; lleva una");
    }

    /** Una opción con texto, identificador propio, texto propio e incidencia 0 o 1; anota los suyos en los vistos. */
    private static Optional<String> itemProblem(Object item, Set<String> ids, Set<String> labels) {
        if (!(item instanceof Map<?, ?> option) || !(option.get("label") instanceof String label) || label.isBlank()) {
            return Optional.of("una opción no tiene texto");
        }
        if (!(option.get("id") instanceof String id) || id.isBlank() || !ids.add(id)) {
            return Optional.of("la opción «" + label.strip() + "» no tiene un identificador propio");
        }
        if (!labels.add(label.strip().replace('’', '\'').toLowerCase(Locale.ROOT))) {
            return Optional.of("repite la opción «" + label.strip() + "»");
        }
        return worthZeroOrOne(option.get("incidence")) ? Optional.empty()
                : Optional.of("la opción «" + label.strip() + "» vale " + option.get("incidence")
                        + "; cada una vale 0 o 1");
    }

    private static boolean worthZeroOrOne(Object incidence) {
        return incidence instanceof Number value && (value.doubleValue() == 0.0 || value.doubleValue() == 1.0);
    }
}
