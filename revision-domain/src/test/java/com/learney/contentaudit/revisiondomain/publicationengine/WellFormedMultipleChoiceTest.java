package com.learney.contentaudit.revisiondomain.publicationengine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** F-PUBPROD-R002 inv. 4: qué le falta a una opción múltiple, como la guarda producción, para estar bien armada. */
@Tag("FEAT-PUBPROD")
@Tag("F-PUBPROD-R002")
class WellFormedMultipleChoiceTest {

    private static final List<Map<String, Object>> PARTS = List.of(part("TEXT", null), part("CLOZE", null),
            part("TEXT", null));

    private static Map<String, Object> part(String kind, List<String> options) {
        Map<String, Object> part = new java.util.HashMap<>();
        part.put("kind", kind);
        part.put("text", "");
        part.put("options", options);
        return part;
    }

    private static Map<String, Object> item(String id, Object incidence, String label) {
        return Map.of("id", id, "incidence", incidence, "label", label);
    }

    private static List<Map<String, Object>> items(Map<String, Object>... items) {
        return new ArrayList<>(List.of(items));
    }

    @Test
    @DisplayName("should take as well formed the multiple choice production keeps: one gap, two to eight options, one correct, one choice")
    void shouldTakeAsWellFormedTheMultipleChoiceProductionKeeps() {
        // «___ ready? (I)» con «Am I / Are I / Is I», como quedó en la C2 de A1.
        assertEquals(Optional.empty(), WellFormedMultipleChoice.problem(PARTS, items(item("Am I", 1.0, "Am I"),
                item("Are I", 0.0, "Are I"), item("Is I", 0, "Is I")), "SINGLE"));
    }

    @Test
    @DisplayName("should say what a multiple choice lacks to be well formed")
    void shouldSayWhatAMultipleChoiceLacksToBeWellFormed() {
        assertEquals(Optional.of("el hueco trae respuestas escritas"), WellFormedMultipleChoice.problem(
                List.of(part("TEXT", null), part("CLOZE", List.of("am"))), items(item("am", 1.0, "am"),
                        item("is", 0.0, "is")), "SINGLE"));
        assertEquals(Optional.of("tiene 0 huecos; lleva uno"), WellFormedMultipleChoice.problem(
                List.of(part("TEXT", null)), items(item("am", 1.0, "am"), item("is", 0.0, "is")), "SINGLE"));
        assertEquals(Optional.of("no tiene opciones"), WellFormedMultipleChoice.problem(PARTS, null, "SINGLE"));
        assertEquals(Optional.of("la opción «Is» no tiene un identificador propio"), WellFormedMultipleChoice.problem(
                PARTS, items(item("am", 1.0, "am"), item("am", 0.0, "Is")), "SINGLE"));
        assertEquals(Optional.of("la opción «is» vale 0.5; cada una vale 0 o 1"), WellFormedMultipleChoice.problem(
                PARTS, items(item("am", 1.0, "am"), item("is", 0.5, "is")), "SINGLE"));
        assertEquals(Optional.of("tiene 0 correctas; lleva una"), WellFormedMultipleChoice.problem(PARTS,
                items(item("am", 0.0, "am"), item("is", 0.0, "is")), "SINGLE"));
        assertEquals(Optional.of("tiene 9 opciones; van de dos a ocho"), WellFormedMultipleChoice.problem(PARTS,
                items(item("a", 1.0, "a"), item("b", 0.0, "b"), item("c", 0.0, "c"), item("d", 0.0, "d"),
                        item("e", 0.0, "e"), item("f", 0.0, "f"), item("g", 0.0, "g"), item("h", 0.0, "h"),
                        item("i", 0.0, "i")), "SINGLE"));
        assertEquals(Optional.of("repite la opción «I’m»"), WellFormedMultipleChoice.problem(PARTS,
                items(item("I'm", 1.0, "I'm"), item("I’m", 0.0, "I’m")), "SINGLE"));
    }
}
