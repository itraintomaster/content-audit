package com.learney.contentaudit.auditdomain.spanishregister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class TuteoTest {
    @Test
    @DisplayName("should write in tú the instructions and the cards in vos of A1, with their stress and their capital")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R027")
    public void shouldWriteInTuTheInstructionsAndTheCardsInVosOfA1() {
        // F-MESA-R027: the real texts of the first correction and of production.
        assertEquals("Crea un adverbio a partir del adjetivo y agrégalo a la oración.",
                Tuteo.of("Creá un adverbio a partir del adjetivo y agregalo a la oración.", true));
        assertEquals("Si es posible, coloca el adverbio en el medio de la oración. Si no es posible, colócalo al final.",
                Tuteo.of("Si es posible, colocá el adverbio en el medio de la oración. Si no es posible, colocalo al final.",
                        true));
        assertEquals("Elige la palabra correcta.", Tuteo.of("Elegí la palabra correcta.", true));
        assertEquals("Si puedes contarlo, es contable. $water$, $work$, $food$ y $music$ no tienen plural.",
                Tuteo.of("Si podés contarlo, es contable. $water$, $work$, $food$ y $music$ no tienen plural.", true));
        assertEquals("Pon $not$ después de $be$ y mantén el verbo; fíjate en el sujeto.",
                Tuteo.of("Poné $not$ después de $be$ y mantené el verbo; fijate en el sujeto.", true));
        assertEquals("¿Me prestas tu libro?", Tuteo.of("¿Me prestás tu libro?", false));
    }

    @Test
    @DisplayName("should leave as they were the English between marks, the past, the future, the words that only end like vos and a word the table does not know")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R027")
    public void shouldLeaveTheEnglishThePastTheFutureAndAnUnknownWordAsTheyWere() {
        // F-MESA-R027: only the words in vos the table knows change.
        assertEquals("Escribí tres oraciones.", Tuteo.of("Escribí tres oraciones.", false));
        assertEquals("Mañana dibujará una casa y sabrás la verdad en inglés.",
                Tuteo.of("Mañana dibujará una casa y sabrás la verdad en inglés.", false));
        assertEquals("Usa $usá$ como pista.", Tuteo.of("Usá $usá$ como pista.", true));
        assertEquals("Si zarpás temprano, usa $early$.", Tuteo.of("Si zarpás temprano, usá $early$.", true));
        assertEquals(null, Tuteo.of(null, true));
    }

    @Test
    @DisplayName("should tell a text that only went from vos to tú from one that changed something else")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R027")
    public void shouldTellATextThatOnlyWentFromVosToTuFromOneThatChangedSomethingElse() {
        // F-MESA-R027: the scope of the tasks and the screen of R021 read it.
        assertTrue(Tuteo.onlyInTu("Usá $is$ con $she$.", "Usa $is$ con $she$.", true));
        assertFalse(Tuteo.onlyInTu("Usá $is$ con $she$.", "Usa $is$ con $he$.", true));
        assertFalse(Tuteo.onlyInTu("Usa $is$ con $she$.", "Usa $is$ con $she$.", true));
    }
}
