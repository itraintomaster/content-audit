package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.auditdomain.NlpTokenizer;
import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.SentenceMode;
import com.learney.contentaudit.coursedomain.quizsentence.QuizSentenceConverter;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Cuenta los tokens NLP de un quizSentence DSL usando EXACTAMENTE el mismo camino
 * que el audit de sentence-length: {@code parse(dsl) -> toPlainSentences(canonical)
 * -> NlpTokenizer.countTokens(sentence)}.
 *
 * <p>Existe para que el eval de longitud del lemma-absence-agent (un script bash del
 * grafo declarativo) mida el candidato con el MISMO tokenizer spaCy que produjo el
 * {@code current token count}/{@code target range} del audit, en vez de aproximar con
 * un conteo de palabras en bash (que no cuenta, p.ej., el guion de "smoke - smoking"
 * como token y por eso difería del audit). Reusa las instancias de {@code Main}
 * ({@link QuizSentenceConverter} + {@link NlpTokenizer}), así la config spaCy
 * (sample_processor, lemmas, batch) es idéntica → el conteo es idéntico.
 *
 * <p>Es un helper de ejecución del CLI: NO está declarado en sentinel.yaml (no es un
 * feature gobernado), solo cablea componentes ya gobernados — mismo criterio que
 * {@code SuggestedLemmaReactTool}.
 *
 * <p>Salida: el entero del conteo en stdout (una línea). Error de parseo / DSL inválido
 * → mensaje a stderr y código de salida 2.
 */
@Command(
        name = "tokens",
        description = "Count NLP tokens of a quizSentence DSL using the same spaCy pipeline as the audit.",
        mixinStandardHelpOptions = true,
        footer = {
                "",
                "Examples:",
                "  content-audit tokens --quiz-sentence \"smoke - ____ [smoking]\"",
                "  content-audit tokens --quiz-sentence \"She ____ [walks] to school.\" --mode FILL",
        }
)
final class TokensCmd implements Callable<Integer> {

    private final QuizSentenceConverter quizSentenceConverter;
    private final NlpTokenizer nlpTokenizer;

    @Option(names = {"--quiz-sentence", "-q"}, required = true,
            description = "The quizSentence DSL to measure (e.g. \"She ____ [walks] to school.\").")
    private String quizSentence;

    @Option(names = {"--mode", "-m"},
            description = "Sentence mode: FILL (default) or REWRITE. Mirrors the knowledge's sentenceMode.")
    private String mode;

    TokensCmd(QuizSentenceConverter quizSentenceConverter, NlpTokenizer nlpTokenizer) {
        this.quizSentenceConverter = quizSentenceConverter;
        this.nlpTokenizer = nlpTokenizer;
    }

    @Override
    public Integer call() {
        if (quizSentence == null || quizSentence.isBlank()) {
            System.err.println("Error: --quiz-sentence is required and must be non-blank.");
            return 2;
        }

        SentenceMode sentenceMode = SentenceMode.FILL;
        if (mode != null && !mode.isBlank()) {
            try {
                sentenceMode = SentenceMode.valueOf(mode.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("Error: invalid --mode '" + mode + "' (expected FILL or REWRITE).");
                return 2;
            }
        }

        FormEntity form;
        try {
            form = quizSentenceConverter.parse(quizSentence);
        } catch (RuntimeException e) {
            System.err.println("Error: could not parse quizSentence DSL: " + e.getMessage());
            return 2;
        }

        List<String> sentences;
        try {
            sentences = quizSentenceConverter.toPlainSentences(form, sentenceMode);
        } catch (RuntimeException e) {
            System.err.println("Error: could not render plain sentence: " + e.getMessage());
            return 2;
        }

        // Canonical variant (index 0) is what the audit tokenizes (CourseToAuditableMapper).
        String canonical = (sentences == null || sentences.isEmpty()) ? "" : sentences.get(0);
        // Echo the rendered sentence to stderr for debuggability; stdout stays clean (just the int).
        System.err.println("[tokens] rendered: " + canonical);

        int count = nlpTokenizer.countTokens(canonical);
        System.out.println(count);
        return 0;
    }
}
