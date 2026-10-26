package com.learney.contentaudit.revisiondomain.panelengine;

import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * F-MESA-R022: a sentence shortened only because it is long loses words and gains none. When the only tasks that let
 * the candidate touch the sentence are of its length, what the student reads and writes -- the statement without its
 * hints and the first answer of each gap -- may only lose words, in the order they were: «He went to the bar after he
 * left work.» may not become «He left the bar after he ate lunch.». The case and the marks may change.
 */
final class LengthByRemoval {
    static final String LENGTH = "sentence-length";
    private static final Pattern HINT = Pattern.compile("\\([^)]*\\)");
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+(?:['’][\\p{L}]+)*");

    private LengthByRemoval() {
    }

    static List<String> violations(QuizTemplateEntity startingPoint, QuizTemplateEntity proposed) {
        List<String> before = words(startingPoint);
        List<String> after = words(proposed);
        if (after.equals(before)) {
            return List.of();
        }
        // A word it puts, or puts more times than the sentence had it.
        Map<String, Integer> left = new HashMap<>();
        before.forEach(word -> left.merge(word, 1, Integer::sum));
        Set<String> put = new LinkedHashSet<>();
        for (String word : after) {
            if (left.merge(word, -1, Integer::sum) < 0) {
                put.add(word);
            }
        }
        if (!put.isEmpty()) {
            return List.of("por el largo sólo se sacan palabras, y el candidato pone «" + String.join("», «", put) + "»");
        }
        // The same words, fewer, but in another order.
        int at = 0;
        for (String word : after) {
            int found = before.subList(at, before.size()).indexOf(word);
            if (found < 0) {
                return List.of("por el largo sólo se sacan palabras, y el candidato cambia su orden");
            }
            at += found + 1;
        }
        return List.of();
    }

    /** What the student reads and writes, word by word, in lower case: the statement without hints and the answers. */
    static List<String> words(QuizTemplateEntity quiz) {
        List<String> words = new ArrayList<>();
        List<SentencePartEntity> parts = quiz != null && quiz.getForm() != null && quiz.getForm().getSentenceParts() != null
                ? quiz.getForm().getSentenceParts() : List.of();
        for (SentencePartEntity part : parts) {
            if (part == null) {
                continue;
            }
            String text = part.getKind() == SentencePartKind.CLOZE
                    ? part.getOptions() == null || part.getOptions().isEmpty() || part.getOptions().get(0) == null ? ""
                    : part.getOptions().get(0).split("\\|", -1)[0]
                    : HINT.matcher(part.getText() != null ? part.getText() : "").replaceAll(" ");
            Matcher matcher = WORD.matcher(text.replace('’', '\''));
            while (matcher.find()) {
                words.add(matcher.group().toLowerCase(Locale.ROOT));
            }
        }
        return words;
    }
}
