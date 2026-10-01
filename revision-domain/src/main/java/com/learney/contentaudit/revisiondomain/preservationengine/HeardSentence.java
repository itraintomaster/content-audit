package com.learney.contentaudit.revisiondomain.preservationengine;

import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceItemEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import java.util.List;
import java.util.Locale;

/**
 * What is heard of an exercise, as near as the preservation can tell without the audio registry (F-NOEMP-R004): the
 * text of its TEXT parts without the hint between parentheses, the first answer of each gap -- before a «|» or a «/»,
 * since the audio says one form, never the bar -- and, in a multiple choice, the correct option. The comparison ignores
 * capitals and punctuation: a capital letter or a final mark is written, not heard.
 */
final class HeardSentence {

    private HeardSentence() {
    }

    static String of(QuizTemplateEntity quiz) {
        FormEntity form = quiz != null ? quiz.getForm() : null;
        if (form == null || form.getSentenceParts() == null) {
            return "";
        }
        StringBuilder heard = new StringBuilder();
        for (SentencePartEntity part : form.getSentenceParts()) {
            if (part == null || part.getKind() == null) {
                continue;
            }
            String piece;
            if (part.getKind() == SentencePartKind.CLOZE) {
                piece = firstAnswer(part.getOptions());
                if (piece.isEmpty() && form.getMultipleChoice() != null) {
                    piece = form.getMultipleChoice().correctItem().map(MultipleChoiceItemEntity::getLabel).orElse("");
                }
            } else {
                piece = part.getText() != null ? part.getText().replaceAll("\\([^)]*\\)", " ") : "";
            }
            if (!piece.isBlank()) {
                if (heard.length() > 0) {
                    heard.append(' ');
                }
                heard.append(piece.strip());
            }
        }
        return heard.toString().replaceAll("\\s+", " ").strip();
    }

    static String normalized(String heard) {
        if (heard == null) {
            return "";
        }
        return heard.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}' ]", " ").replaceAll("\\s+", " ").strip();
    }

    private static String firstAnswer(List<String> options) {
        if (options == null || options.isEmpty() || options.get(0) == null) {
            return "";
        }
        String first = options.get(0);
        int bar = first.indexOf('|');
        if (bar >= 0) {
            first = first.substring(0, bar);
        }
        int slash = first.indexOf('/');
        if (slash >= 0) {
            first = first.substring(0, slash);
        }
        return first.strip();
    }
}
