package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.auditdomain.NlpTokenizer;
import com.learney.contentaudit.auditdomain.SentenceLengthConfig;
import com.learney.contentaudit.auditdomain.TargetRange;
import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.SentenceMode;
import com.learney.contentaudit.coursedomain.quizsentence.QuizSentenceConverter;
import java.util.List;
import java.util.Optional;
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
 * <p>Salida: el entero del conteo en stdout (una línea). Con {@code --level A1} la salida es
 * en cambio una línea JSON {@code {"tokens":N,"min":A,"max":B}} con el rango de longitud objetivo
 * del nivel, reusado de {@link SentenceLengthConfig} (la misma fuente que el audit), para que el
 * eval de longitud pueda exigir no-regresión contra el rango del nivel aunque el audit no haya
 * marcado problema de longitud. Error de parseo / DSL inválido → mensaje a stderr y código de salida 2.
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
    private final SentenceLengthConfig sentenceLengthConfig;

    @Option(names = {"--quiz-sentence", "-q"}, required = true,
            description = "The quizSentence DSL to measure (e.g. \"She ____ [walks] to school.\").")
    private String quizSentence;

    @Option(names = {"--mode", "-m"},
            description = "Sentence mode: FILL (default) or REWRITE. Mirrors the knowledge's sentenceMode.")
    private String mode;

    @Option(names = {"--level", "-l"},
            description = "CEFR level (A1/A2/B1/B2). When given, stdout becomes JSON "
                    + "{\"tokens\":N,\"min\":A,\"max\":B} carrying the level's target sentence-length "
                    + "range (reused from the audit's SentenceLengthConfig); without it, stdout is the bare count.")
    private String level;

    TokensCmd(QuizSentenceConverter quizSentenceConverter, NlpTokenizer nlpTokenizer,
              SentenceLengthConfig sentenceLengthConfig) {
        this.quizSentenceConverter = quizSentenceConverter;
        this.nlpTokenizer = nlpTokenizer;
        this.sentenceLengthConfig = sentenceLengthConfig;
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

        // Without --level: emit the bare token count (backward-compatible).
        // With --level: also resolve the level's target sentence-length range from the SAME
        // SentenceLengthConfig the audit uses, and emit a single JSON line, so the length eval
        // can enforce non-regression against the level range even when the audit did not flag a
        // length problem (lengthGuidance == "none").
        if (level == null || level.isBlank()) {
            System.out.println(count);
            return 0;
        }

        Integer min = null;
        Integer max = null;
        CefrLevel cefrLevel = null;
        try {
            cefrLevel = CefrLevel.valueOf(level.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("[tokens] warning: invalid --level '" + level
                    + "' (expected A1/A2/B1/B2); emitting null range.");
        }
        if (cefrLevel != null) {
            Optional<TargetRange> range = sentenceLengthConfig.getTargetRange(cefrLevel);
            if (range.isPresent()) {
                min = range.get().getMinTokens();
                max = range.get().getMaxTokens();
            } else {
                System.err.println("[tokens] warning: no target range configured for level "
                        + cefrLevel + "; emitting null range.");
            }
        }
        System.out.println("{\"tokens\":" + count
                + ",\"min\":" + (min == null ? "null" : min)
                + ",\"max\":" + (max == null ? "null" : max) + "}");
        return 0;
    }
}
