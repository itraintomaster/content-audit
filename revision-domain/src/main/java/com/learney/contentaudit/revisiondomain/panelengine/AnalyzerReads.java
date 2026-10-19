package com.learney.contentaudit.revisiondomain.panelengine;

import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * F-MESA-R002: what each analyzer of the catalog reads of an exercise, in the parts FEAT-TAREA names, so the panel
 * measures an exercise again only with the analyzers that read something its tasks may change. It is what each card
 * says in «Lee» ({@code AnalyzerDescriptor.reads}), written as parts; an analyzer this table does not know reads
 * everything, so it is always measured.
 */
final class AnalyzerReads {

    private static final Set<ExercisePart> SENTENCE = EnumSet.of(ExercisePart.STATEMENT, ExercisePart.ANSWERS,
            ExercisePart.OPTIONS, ExercisePart.STORED_SENTENCE, ExercisePart.FORM_KIND);

    private static final Map<String, Set<ExercisePart>> READS = Map.ofEntries(
            Map.entry("sentence-length", SENTENCE),
            Map.entry("coca-buckets-distribution", SENTENCE),
            Map.entry("lemma-recurrence", SENTENCE),
            Map.entry("lemma-absence", SENTENCE),
            Map.entry("lemma-count", SENTENCE),
            Map.entry("off-level-words", SENTENCE),
            Map.entry("knowledge-title-length", EnumSet.noneOf(ExercisePart.class)),
            Map.entry("knowledge-instructions-length", EnumSet.of(ExercisePart.TOPIC_INSTRUCTIONS)),
            Map.entry("quiz-instruction", EnumSet.of(ExercisePart.INSTRUCTIONS, ExercisePart.TOPIC_INSTRUCTIONS,
                    ExercisePart.STATEMENT, ExercisePart.HINT, ExercisePart.ANSWERS, ExercisePart.OPTIONS,
                    ExercisePart.FORM_KIND)),
            Map.entry("hint-coherence", EnumSet.of(ExercisePart.HINT, ExercisePart.ANSWERS, ExercisePart.STATEMENT,
                    ExercisePart.STORED_SENTENCE, ExercisePart.FORM_KIND)),
            Map.entry("answer-completeness", EnumSet.of(ExercisePart.ANSWERS, ExercisePart.STATEMENT,
                    ExercisePart.INSTRUCTIONS, ExercisePart.MINI_THEORY, ExercisePart.FORM_KIND)),
            Map.entry("multiple-choice-validity", EnumSet.of(ExercisePart.OPTIONS, ExercisePart.STATEMENT,
                    ExercisePart.HINT, ExercisePart.INSTRUCTIONS, ExercisePart.FORM_KIND)),
            Map.entry("multiple-choice-quality", EnumSet.of(ExercisePart.OPTIONS, ExercisePart.STATEMENT,
                    ExercisePart.HINT, ExercisePart.INSTRUCTIONS, ExercisePart.FORM_KIND)),
            Map.entry("data-coherence", EnumSet.of(ExercisePart.STATEMENT, ExercisePart.ANSWERS, ExercisePart.OPTIONS,
                    ExercisePart.STORED_SENTENCE, ExercisePart.AUDIO, ExercisePart.FORM_KIND)),
            Map.entry("exact-duplicates", EnumSet.of(ExercisePart.STATEMENT, ExercisePart.HINT, ExercisePart.ANSWERS,
                    ExercisePart.OPTIONS, ExercisePart.FORM_KIND)),
            Map.entry("near-duplicates", EnumSet.of(ExercisePart.STATEMENT, ExercisePart.ANSWERS,
                    ExercisePart.OPTIONS, ExercisePart.FORM_KIND)),
            Map.entry("translation-sense", EnumSet.of(ExercisePart.TRANSLATION, ExercisePart.STATEMENT,
                    ExercisePart.ANSWERS, ExercisePart.OPTIONS, ExercisePart.STORED_SENTENCE, ExercisePart.FORM_KIND)),
            Map.entry("single-answer", EnumSet.of(ExercisePart.STATEMENT, ExercisePart.HINT, ExercisePart.ANSWERS,
                    ExercisePart.INSTRUCTIONS, ExercisePart.TOPIC_INSTRUCTIONS, ExercisePart.FORM_KIND)),
            // The card is judged against the exercise: changing the gap, hint or instructions can make
            // an unchanged card insufficient. Check that while the panel still has a chance to repair it.
            Map.entry("mini-theory", EnumSet.of(ExercisePart.MINI_THEORY, ExercisePart.STATEMENT,
                    ExercisePart.HINT, ExercisePart.ANSWERS, ExercisePart.OPTIONS, ExercisePart.INSTRUCTIONS,
                    ExercisePart.TOPIC_INSTRUCTIONS, ExercisePart.TRANSLATION, ExercisePart.FORM_KIND)),
            Map.entry("grammar", EnumSet.of(ExercisePart.STATEMENT, ExercisePart.ANSWERS, ExercisePart.OPTIONS,
                    ExercisePart.STORED_SENTENCE, ExercisePart.FORM_KIND)),
            Map.entry("naturalness", EnumSet.of(ExercisePart.STATEMENT, ExercisePart.ANSWERS, ExercisePart.OPTIONS,
                    ExercisePart.STORED_SENTENCE, ExercisePart.FORM_KIND)));

    private AnalyzerReads() {
    }

    /** What the analyzer reads, or empty when this table does not know it. */
    static Optional<Set<ExercisePart>> of(String analyzer) {
        return Optional.ofNullable(READS.get(analyzer));
    }

    /** Whether the analyzer reads one of these parts; an analyzer the table does not know reads everything. */
    static boolean readsAny(String analyzer, Set<ExercisePart> parts) {
        if (parts.contains(ExercisePart.WHOLE_EXERCISE)) {
            return true;
        }
        Set<ExercisePart> reads = READS.get(analyzer);
        if (reads == null) {
            return true;
        }
        for (ExercisePart part : parts) {
            if (reads.contains(part)) {
                return true;
            }
        }
        return false;
    }
}
