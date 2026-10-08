package com.learney.contentaudit.revisiondomain;

import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.FormKind;
import com.learney.contentaudit.coursedomain.MultipleChoiceEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntities;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import java.util.List;
import java.util.regex.Pattern;

/** Rebuilds the canonical stored sentence after a revision changes what the student completes. */
public final class StoredSentenceRehydrator {

    private static final Pattern HINT = Pattern.compile("\\([^)]*\\)");

    private StoredSentenceRehydrator() {
    }

    /**
     * The exercise assembled with the first accepted CLOZE answer, or with the correct multiple-choice option.
     * Parenthesized hints are not part of the stored sentence.
     */
    public static List<String> rebuild(QuizTemplateEntity quiz) {
        if (quiz == null || quiz.getForm() == null || quiz.getForm().getSentenceParts() == null
                || quiz.getForm().getSentenceParts().isEmpty()) {
            return List.of();
        }
        String correctOption = correctOption(quiz);
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
