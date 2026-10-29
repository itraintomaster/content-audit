package com.learney.contentaudit.auditdomain.spanishregister;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class VoseoTest {
    @Test
    @DisplayName("should read as vos the pronoun, a form the table knows though it ends like a future, and an imperative after an English subject without its marks")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R021")
    void shouldReadAsVosThePronounAFormTheTableKnowsAndAnImperativeAfterAnEnglishSubject() {
        // F-MESA-R021: cards of the C2 of A1 (28/10/2026) the detector let pass, and two of production.
        assertEquals(List.of("vos"), Voseo.in(
                "Cuando te preguntan $Can you…?$ (¿Puedes…?), respondes sobre vos con $I$.", true));
        assertEquals(List.of("vos", "pagás"), Voseo.in("$rent from$ es alquilarle A alguien (vos pagás).", true));
        assertEquals(List.of("esperás"), Voseo.in("Usa $wait for$ para decir a quién esperás.", true));
        assertEquals(List.of("preparás"), Voseo.in("$train for$: para qué te preparás.", true));
        assertEquals(List.of("Separá"), Voseo.in("Separá las palabras con un espacio.", true));
        assertEquals(List.of("usá"), Voseo.in("Para preguntar en pasado, pon $was$ antes del sujeto. Con he usá $was$.", true));
        assertEquals(List.of("usá"), Voseo.in("Para negar, con I, he, she, it usá $was not$.", true));
    }

    @Test
    @DisplayName("should still leave the future, the past of yo after an English subject and the past in the middle of a sentence")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R021")
    void shouldStillLeaveTheFutureAndThePast() {
        // F-MESA-R021: «esperarás» is the future; «Le pedí» and «I compré» are the past of «yo».
        assertEquals(List.of(), Voseo.in("Mañana esperarás el autobús.", false));
        assertEquals(List.of(), Voseo.in("$I asked my mum for a pen$ significa «Le pedí un bolígrafo a mi mamá».", true));
        assertEquals(List.of(), Voseo.in("$I bought it$: I compré eso.", true));
    }
}
