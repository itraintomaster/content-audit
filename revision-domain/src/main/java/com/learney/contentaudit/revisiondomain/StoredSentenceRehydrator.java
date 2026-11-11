package com.learney.contentaudit.revisiondomain;

import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.FormKind;
import com.learney.contentaudit.coursedomain.MultipleChoiceEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntities;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.coursedomain.SentenceToTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** Rebuilds the canonical stored sentence after a revision changes what the student completes. */
public final class StoredSentenceRehydrator {

    private static final Pattern HINT = Pattern.compile("\\([^)]*\\)");

    /**
     * F-TAREA-R006 inv. 4: a gap whose answer is a contraction («'s», «'ll», «'re», «n't» after «do»…) goes joined to
     * the word before it, as the student writes it: «It's going to rain soon.», never «It 's». A quote is not one.
     */
    private static final Pattern CONTRACTION = Pattern.compile("(?i)\\s+(['\u2019](?:s|re|ve|ll|d|m|t))(?=[\\s,.;:?!]|$)");

    private StoredSentenceRehydrator() {
    }

    /**
     * The exercise assembled with the first accepted CLOZE answer, or with the correct multiple-choice option.
     * Parenthesized hints are not part of the stored sentence. When the exercise gives a sentence to transform and the
     * student writes the whole sentence, the stored sentence is the answer alone, as the course keeps it (F-SMODE-R004).
     */
    public static List<String> rebuild(QuizTemplateEntity quiz) {
        if (quiz == null || quiz.getForm() == null || quiz.getForm().getSentenceParts() == null
                || quiz.getForm().getSentenceParts().isEmpty()) {
            return List.of();
        }
        String correctOption = correctOption(quiz);
        if (correctOption == null && SentenceToTransform.given(quiz)) {
            String answer = SentenceToTransform.answer(quiz.getForm().getSentenceParts());
            return answer.isEmpty() ? List.of() : List.of(answer);
        }
        StringBuilder sentence = new StringBuilder();
        for (SentencePartEntity part : quiz.getForm().getSentenceParts()) {
            if (part == null) {
                continue;
            }
            String fragment = null;
            if (part.getKind() == SentencePartKind.TEXT) {
                fragment = HINT.matcher(part.getText() != null ? part.getText() : "").replaceAll(" ");
            } else if (part.getKind() == SentencePartKind.CLOZE) {
                fragment = correctOption != null ? correctOption : firstAnswer(part);
            }
            if (fragment != null && !fragment.isBlank()) {
                sentence.append(' ').append(fragment);
            }
        }
        String canonical = CONTRACTION.matcher(sentence.toString().replaceAll("\\s+", " ")
                .replaceAll("\\s+([,.;:?!])", "$1")).replaceAll("$1").strip();
        return canonical.isEmpty() ? List.of() : List.of(canonical);
    }

    /**
     * F-TAREA-R006 inv. 4: whether a sentence built again says what the stored one says -- the same words, a contraction
     * written apart or together, other spaces --; then the stored one stays, with its other forms («It's going to rain
     * soon.» and «It is going to rain soon.»), and nothing the fix did not mean to change moves.
     */
    public static boolean saysTheSame(List<String> rebuilt, List<String> stored) {
        if (rebuilt == null || rebuilt.isEmpty() || stored == null || stored.isEmpty() || rebuilt.get(0) == null
                || stored.get(0) == null) {
            return false;
        }
        return written(rebuilt.get(0)).equals(written(stored.get(0)));
    }

    /**
     * Whether two lists of stored sentences are the same, sentence by sentence, but for how a contraction or a space is
     * written: «She 's not very happy.» is «She's not very happy.».
     */
    public static boolean writtenTheSame(List<String> first, List<String> second) {
        if (first == null || second == null || first.size() != second.size()) {
            return Objects.equals(first, second);
        }
        for (int i = 0; i < first.size(); i++) {
            if (first.get(i) == null || second.get(i) == null ? first.get(i) != second.get(i)
                    : !written(first.get(i)).equals(written(second.get(i)))) {
                return false;
            }
        }
        return true;
    }

    private static String written(String sentence) {
        return CONTRACTION.matcher(sentence.replaceAll("\\s+", " ").strip()).replaceAll("$1");
    }

    /**
     * F-DATOS-R001: the stored sentence a transformation should keep instead of the one it has, when the one it has is
     * not its answer alone -- the sentence to transform glued before it, «Mary is at home. Mary is always at home.» for
     * «Mary is always at home.». Empty when it is not a transformation, has no stored sentence or keeps it right.
     */
    public static Optional<List<String>> answerAloneInsteadOf(QuizTemplateEntity quiz) {
        if (quiz == null || quiz.getSentences() == null || quiz.getSentences().isEmpty() || correctOption(quiz) != null
                || !SentenceToTransform.rewritten(quiz)) {
            return Optional.empty();
        }
        List<String> alone = rebuild(quiz);
        return alone.isEmpty() || alone.equals(quiz.getSentences()) ? Optional.empty()
                : Optional.of(new ArrayList<>(alone));
    }

    private static String correctOption(QuizTemplateEntity quiz) {
        FormEntity form = quiz.getForm();
        if (QuizTemplateEntities.formKind(quiz) != FormKind.MULTIPLE_CHOICE
                && (form == null || form.getMultipleChoice() == null)) {
            return null;
        }
        MultipleChoiceEntity choice = form != null ? form.getMultipleChoice() : null;
        return choice != null ? choice.correctItem().map(item -> item.getLabel()).orElse(null) : null;
    }

    private static String firstAnswer(SentencePartEntity part) {
        if (part.getOptions() == null || part.getOptions().isEmpty() || part.getOptions().get(0) == null) {
            return "";
        }
        return part.getOptions().get(0).split("\\|", -1)[0].strip();
    }
}
