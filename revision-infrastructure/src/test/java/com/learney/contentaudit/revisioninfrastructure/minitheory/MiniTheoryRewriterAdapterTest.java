package com.learney.contentaudit.revisioninfrastructure.minitheory;
import static org.junit.jupiter.api.Assertions.*;
import com.learney.contentaudit.revisiondomain.minitheorycorrection.MiniTheoryCorrection.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class MiniTheoryRewriterAdapterTest {
    @Test
    @Tag("F-MINIT-R007")
    void sendsAbstractionPolicyBeforeReuseAndKeepsTheConcreteQuizAsSeparateData() {
        var calls = new AtomicInteger();
        var quiz = new com.learney.contentaudit.coursedomain.QuizTemplateEntity();
        quiz.setId("isolated-quiz");
        quiz.setMiniTheory("Para preguntar con she y climb, copiá toda la oración.");
        quiz.setSentences(List.of("Does she climb the hill?"));
        var source = new Candidate("incidental-source", "other-quiz", quiz.getMiniTheory(), "COURSE", "UNREVIEWED");
        var adapter = new MiniTheoryRewriterAdapter(prompt -> {
            calls.incrementAndGet();
            String guide = prompt.substring(0, prompt.indexOf("FEEDBACK HUMANO:"));
            assertTrue(guide.contains("Abstraé sólo el léxico incidental"));
            assertTrue(guide.contains("Si especifica de más o usa jerga teórica, no la reutilices literalmente"));
            assertTrue(guide.contains("Evitá estrictamente el metalenguaje gramatical abstracto y la jerga teórica"));
            assertTrue(guide.contains("lenguaje cotidiano, intuitivo y funcional"));
            assertTrue(guide.contains("léxica, semántica o una excepción"));
            assertTrue(guide.contains("variantes aceptadas"));
            assertTrue(guide.contains("No nombres el léxico incidental ni siquiera como ejemplo"));
            assertTrue(guide.contains("sujeto nominal sólo aporta"));
            assertTrue(guide.contains("Usá un único esquema"));
            assertTrue(guide.contains("No amplíes el alcance al abstraer las palabras"));
            assertTrue(guide.indexOf("pautas de abstracción y concisión") < guide.indexOf("LITERALMENTE"));
            assertFalse(guide.contains("climb"));
            assertFalse(guide.contains("isolated-quiz"));
            assertTrue(prompt.contains("Does she climb the hill?"));
            assertTrue(prompt.contains("incidental-source"));
            return "{\"text\":\"Con $does$, el verbo va en forma base.\",\"sourceId\":null}";
        }, "test; prompt=" + MiniTheoryRewriterAdapter.PROMPT_VERSION);
        var result = adapter.rewrite(new RewriteInput("Explicá la estructura sin copiar el verbo.",
                quiz.getId(), null, quiz, List.of(source)));
        assertEquals(1, calls.get(), "sin reintentos implícitos ni consultas adicionales");
        assertNull(result.sourceId());
        assertTrue(result.provenance().contains(MiniTheoryRewriterAdapter.PROMPT_VERSION));
        assertEquals("Para preguntar con she y climb, copiá toda la oración.", quiz.getMiniTheory());
        assertEquals(List.of("Does she climb the hill?"), quiz.getSentences());
    }

    @Test
    @Tag("F-MINIT-R007")
    void instructsNativeHighlightingForEachOccurrenceBeforeChoosingLiteralReuse() {
        var adapter = new MiniTheoryRewriterAdapter(prompt -> {
            String guide = prompt.substring(0, prompt.indexOf("FEEDBACK HUMANO:"));
            assertTrue(guide.contains("cada aparición"));
            assertTrue(guide.contains("palabras y formas inglesas clave"));
            assertTrue(guide.contains("$...$"));
            assertTrue(guide.contains("$Does$ + sujeto + verbo base + complemento"));
            assertTrue(guide.contains("$he$, $she$, $it$"));
            assertTrue(guide.contains("$ch$, $-es$, $watch -> watches$"));
            assertTrue(guide.contains("etiquetas españolas"));
            assertTrue(guide.contains("No encierres el párrafo completo"));
            assertTrue(guide.contains("Markdown ni HTML como sustitutos"));
            assertTrue(guide.contains("pares cerrados de $...$"));
            assertTrue(guide.contains("backticks no sustituyen"));
            assertTrue(guide.contains("no agregues inglés sólo para resaltarlo"));
            assertTrue(guide.contains("Conservá las marcas válidas sin duplicarlas"));
            assertTrue(guide.indexOf("cada aparición") < guide.indexOf("LITERALMENTE"));
            assertTrue(guide.contains("Si falla el resaltado, no la reutilices literalmente"));
            assertTrue(guide.contains("hacé sólo el cambio mínimo de formato"));
            assertTrue(guide.contains("Si cambia el texto respecto del candidato, devolvé sourceId null"));
            return "{\"text\":\"Con $does$, el verbo va en forma base.\",\"sourceId\":null}";
        }, "test; prompt=" + MiniTheoryRewriterAdapter.PROMPT_VERSION);

        adapter.rewrite(new RewriteInput("Resaltá el inglés clave.", "quiz", null, null, List.of()));
        assertEquals("mini-feedback-v5", MiniTheoryRewriterAdapter.PROMPT_VERSION);
    }

    @Test
    @Tag("F-MINIT-R007")
    void prioritizesOriginalWordingForHighlightOnlyFeedbackOverDifferentlyWordedEquivalentCandidate() {
        String original = "Con I se usa am en presente afirmativo.";
        String corrected = "Con $I$ se usa $am$ en presente afirmativo.";
        String equivalent = "En presente afirmativo, $I$ lleva $am$.";
        var quiz = new com.learney.contentaudit.coursedomain.QuizTemplateEntity();
        quiz.setId("original-wording-quiz");
        quiz.setMiniTheory(original);
        var source = new Candidate("equivalent-source", "other-quiz", equivalent, "COURSE", "UNREVIEWED");
        var adapter = new MiniTheoryRewriterAdapter(prompt -> {
            String guide = prompt.substring(0, prompt.indexOf("FEEDBACK HUMANO:"));
            assertTrue(guide.contains("Sólo si el pedido o diagnóstico es corregir el resaltado"));
            assertTrue(guide.contains("no hay otro defecto ni un pedido de reformulación"));
            assertTrue(guide.contains("priorizá conservar la redacción original"));
            assertTrue(guide.contains("no elijas un candidato equivalente con otra redacción"));
            assertTrue(guide.contains("coincide literalmente con la reparación del original"));
            assertTrue(guide.contains("si no coincide, devolvé sourceId null"));
            assertTrue(guide.contains("Para otros pedidos, como acortar o reformular"));
            assertTrue(guide.indexOf("priorizá conservar la redacción original") < guide.indexOf("LITERALMENTE"));
            assertTrue(prompt.contains(original));
            assertTrue(prompt.contains(equivalent));
            return "{\"text\":\"" + corrected + "\",\"sourceId\":null}";
        }, "test; prompt=" + MiniTheoryRewriterAdapter.PROMPT_VERSION);

        // The supplied response checks the prompt contract and transport, not model adherence to the policy.
        var result = adapter.rewrite(new RewriteInput("Corregí únicamente el resaltado de I y am.",
                quiz.getId(), null, quiz, List.of(source)));

        assertEquals(corrected, result.text());
        assertNull(result.sourceId());
        assertEquals(original, quiz.getMiniTheory());
        assertEquals(equivalent, source.text());
    }

    @Test
    @Tag("F-MINIT-R007")
    void transportsFormattingOnlyModelResponseWithoutMutatingQuizOrCandidate() {
        String original = "Para preguntar: Does + sujeto + verbo base + complemento. Con does, el verbo va en forma base.";
        String corrected = "Para preguntar: $Does$ + sujeto + verbo base + complemento. Con $does$, el verbo va en forma base.";
        var quiz = new com.learney.contentaudit.coursedomain.QuizTemplateEntity();
        quiz.setId("formatting-quiz");
        quiz.setMiniTheory(original);
        quiz.setSentences(List.of("Does she climb the hill?"));
        var source = new Candidate("unformatted-source", "other-quiz", original, "COURSE", "UNREVIEWED");
        var calls = new AtomicInteger();
        var adapter = new MiniTheoryRewriterAdapter(prompt -> {
            calls.incrementAndGet();
            assertTrue(prompt.contains(original));
            assertTrue(prompt.contains("unformatted-source"));
            return "{\"text\":\"" + corrected + "\",\"sourceId\":null}";
        }, "test; prompt=" + MiniTheoryRewriterAdapter.PROMPT_VERSION);

        // The supplied response tests transport and preservation, not the model's linguistic judgment.
        var result = adapter.rewrite(new RewriteInput("Faltan marcas nativas en las dos apariciones de does.",
                quiz.getId(), null, quiz, List.of(source)));

        assertEquals(corrected, result.text());
        assertNull(result.sourceId());
        assertEquals(1, calls.get());
        assertEquals(original, quiz.getMiniTheory());
        assertEquals(List.of("Does she climb the hill?"), quiz.getSentences());
        assertEquals(new Candidate("unformatted-source", "other-quiz", original, "COURSE", "UNREVIEWED"), source);
    }

    @Test void transportsLiteralReuseWithValidMarksAndRetainsEffectiveProvenance() {
        var adapter = new MiniTheoryRewriterAdapter(prompt -> {
            assertTrue(prompt.contains("$You$ va con $are$")); assertTrue(prompt.contains("LITERALMENTE"));
            return "{\"text\":\"$You$ va con $are$\",\"sourceId\":\"existing\"}";
        }, "codex-cli:gpt-6.1-sol; effort=medium; prompt=" + MiniTheoryRewriterAdapter.PROMPT_VERSION);
        var result = adapter.rewrite(new RewriteInput("You va con are", "quiz", null, null,
                List.of(new Candidate("existing", "other", "$You$ va con $are$", "COURSE", "UNREVIEWED"))));
        assertEquals("$You$ va con $are$", result.text());
        assertEquals("existing", result.sourceId()); assertTrue(result.provenance().contains("gpt-6.1-sol"));
    }
    @Test void unusableResponseIsRejectedWithoutRetries() {
        var adapter = new MiniTheoryRewriterAdapter(prompt -> "no es JSON", "test");
        assertThrows(IllegalArgumentException.class, () -> adapter.rewrite(new RewriteInput("context", "quiz", null, null, List.of())));
    }
}
