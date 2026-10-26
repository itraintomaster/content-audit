package com.learney.contentaudit.revisiondomain.panelengine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class SpanishRegisterTest {
    @Test
    @DisplayName("should find the vos of the cards and instructions of A1 written by the first correction")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R021")
    void shouldFindTheVosOfTheCardsAndInstructionsOfA1() {
        // F-MESA-R021: the most frequent forms of the 507 cards and 82 instructions in vos of A1 after C1.
        assertEquals(List.of("Usá"), SpanishRegister.voseo("Usá $is$ con $she$.", true));
        assertEquals(List.of("usá"), SpanishRegister.voseo("Con $he$ usá $is$.", true));
        assertEquals(List.of("Elegí"), SpanishRegister.voseo("Elegí la opción correcta.", true));
        assertEquals(List.of("Mirá", "parás"), SpanishRegister.voseo("Mirá: si parás un tiempo, va en continuo.", true));
        assertEquals(List.of("elegí", "Poné"), SpanishRegister.voseo("Con la pista (She), elegí $Is she$. Poné el signo.",
                true));
        assertEquals(List.of("decidís", "podés"), SpanishRegister.voseo(
                "Si decidís usar $can$, podés pedir permiso.", true));
        assertEquals(List.of("Colocá", "colocalo"), SpanishRegister.voseo(
                "Colocá el adverbio en su lugar y colocalo antes del verbo.", true));
        assertEquals(List.of("preguntate"), SpanishRegister.voseo("Antes de elegir, preguntate quién hace la acción.",
                true));
        assertEquals(List.of("necesitás"), SpanishRegister.voseo("¿Necesitás ayuda?", false).stream()
                .map(String::toLowerCase).toList());
    }

    @Test
    @DisplayName("should not take for vos the tú of the course, the past, the future or the words that only end like vos")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R021")
    void shouldNotTakeForVosTheTuThePastTheFutureOrWordsThatOnlyEndLikeVos() {
        // F-MESA-R021: what the student reads in tú, and the endings of the translations of A1 that are not vos.
        assertEquals(List.of(), SpanishRegister.voseo(
                "Crea un adverbio a partir del adjetivo que está entre paréntesis y añádelo a la oración.", true));
        assertEquals(List.of(), SpanishRegister.voseo("Usa $is$ con $she$; después, el verbo irá al final.", true));
        assertEquals(List.of(), SpanishRegister.voseo("La etiqueta $Polite$ pide el pedido cortés con $Could$.", true));
        assertEquals(List.of(), SpanishRegister.voseo("Escribí tres oraciones en dos minutos.", false));
        assertEquals(List.of(), SpanishRegister.voseo("Mañana dibujará una casa y sabrás la verdad.", false));
        assertEquals(List.of(), SpanishRegister.voseo("Mary está en casa. Él habla inglés y francés en París.", false));
        assertEquals(List.of(), SpanishRegister.voseo("¿Me prestas tu libro? Sí, aquí está, en el país de mamá.", false));
        assertEquals(List.of(), SpanishRegister.voseo("José habla con Inés de sus esquís. Así se dice en inglés.", true));
        assertEquals(List.of(), SpanishRegister.voseo("Mañana será lunes y te dará tiempo; irás al cine y verás.", true));
        assertEquals(List.of(), SpanishRegister.voseo("En afirmativo: $I heard a funny noise.$ = Escuché un ruido raro.",
                true));
        assertEquals(List.of(), SpanishRegister.voseo("Sin preposición: I bought jeans (Compré jeans el invierno pasado).",
                true));
    }
}
