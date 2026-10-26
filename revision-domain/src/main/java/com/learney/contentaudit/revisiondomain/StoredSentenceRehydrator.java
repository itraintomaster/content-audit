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
import java.util.Optional;
import java.util.regex.Pattern;

/** Rebuilds the canonical stored sentence after a revision changes what the student completes. */
public final class StoredSentenceRehydrator {

    private static final Pattern HINT = Pattern.compile("\\([^)]*\\)");

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
        String canonical = sentence.toString().replaceAll("\\s+", " ")
                .replaceAll("\\s+([,.;:?!])", "$1").strip();
        return canonical.isEmpty() ? List.of() : List.of(canonical);
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
