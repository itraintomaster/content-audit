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

    @Test void asksForLiteralReuseWithHumanFeedbackAndRetainsEffectiveProvenance() {
        var adapter = new MiniTheoryRewriterAdapter(prompt -> {
            assertTrue(prompt.contains("You va con are")); assertTrue(prompt.contains("LITERALMENTE"));
            return "{\"text\":\"You va con are\",\"sourceId\":\"existing\"}";
        }, "codex-cli:gpt-6.1-sol; effort=medium; prompt=mini-feedback-v1");
        var result = adapter.rewrite(new RewriteInput("You va con are", "quiz", null, null,
                List.of(new Candidate("existing", "other", "You va con are", "COURSE", "UNREVIEWED"))));
        assertEquals("existing", result.sourceId()); assertTrue(result.provenance().contains("gpt-6.1-sol"));
    }
    @Test void unusableResponseIsRejectedWithoutRetries() {
        var adapter = new MiniTheoryRewriterAdapter(prompt -> "no es JSON", "test");
        assertThrows(IllegalArgumentException.class, () -> adapter.rewrite(new RewriteInput("context", "quiz", null, null, List.of())));
    }
}
