package com.learney.contentaudit.revisiondomain.panelengine;

import com.learney.contentaudit.coursedomain.MultipleChoiceItemEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * F-MESA-R018: a measure is not won by taking punctuation out or by breaking the line. Compared with its starting
 * point, a candidate may not put a line break in what the student reads, take out the mark that separated two words it
 * keeps together, or glue a sentence to the one before it without the mark that separated them. A whole sentence may go,
 * with its mark; a line break the exercise already had may stay.
 */
final class PunctuationPreservation {
    private static final Pattern LINE_BREAK = Pattern.compile("[\\n\\r\\t\\u000B\\f\\u0085\\u2028\\u2029]");
    private static final Pattern HINT = Pattern.compile("\\([^)]*\\)");
    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}'’]+|[,;:.!?…]+");
    private static final Pattern MARK = Pattern.compile("[,;:.!?…]+");
    /** A mark after which a capital opens a sentence. */
    private static final Pattern BOUNDARY = Pattern.compile("[;:.!?…]+");

    private record Field(String name, Function<QuizTemplateEntity, String> text) {
    }

    /** What the student reads or hears, each where a line break would show. */
    private static final List<Field> FIELDS = List.of(
            new Field("el enunciado", PunctuationPreservation::statement),
            new Field("las respuestas", PunctuationPreservation::answers),
            new Field("las opciones", PunctuationPreservation::options),
            new Field("la traducción", QuizTemplateEntity::getTranslation),
            new Field("las oraciones guardadas", q -> q.getSentences() != null ? String.join(" ", q.getSentences()) : null));

    private PunctuationPreservation() {
    }

    static List<String> violations(QuizTemplateEntity startingPoint, QuizTemplateEntity proposed) {
        List<String> violations = new ArrayList<>();
        for (Field field : FIELDS) {
            if (breaks(field.text().apply(proposed)) > breaks(field.text().apply(startingPoint))) {
                violations.add("mete un salto de línea en " + field.name());
            }
        }
        List<String> was = tokens(read(startingPoint));
        List<String> is = tokens(read(proposed));
        Set<String> together = new LinkedHashSet<>();
        Set<String> separated = new LinkedHashSet<>();
        for (int i = 0; i + 1 < is.size(); i++) {
            together.add(key(is.get(i), is.get(i + 1)));
            if (i + 2 < is.size()) {
                separated.add(key(is.get(i), is.get(i + 1), is.get(i + 2)));
            }
        }
        Set<String> reported = new LinkedHashSet<>();
        Set<String> lostMarks = new LinkedHashSet<>();
        for (int i = 1; i + 1 < was.size(); i++) {
            String left = was.get(i - 1);
            String mark = was.get(i);
            String right = was.get(i + 1);
            if (!isMark(mark) || isMark(left) || isMark(right)) {
                continue;
            }
            if (together.contains(key(left, right)) && !separated.contains(key(left, mark, right))) {
                lostMarks.add("saca " + markName(mark) + " entre «" + left + "» y «" + right + "» del enunciado");
                reported.add(right);
            }
        }
        violations.addAll(lostMarks);
        Set<String> startsBefore = starts(was);
        startsBefore.removeAll(midSentence(was));
        for (String word : midSentence(is)) {
            if (startsBefore.contains(word) && !reported.contains(word)) {
                violations.add("pega la oración que empieza en «" + word + "» a la anterior, sin el signo que las separaba");
            }
        }
        return violations;
    }

    // -------------------------------------------------------------------------

    /** The statement as the student reads it once solved: its text without hints, with the answer in each gap. */
    private static String read(QuizTemplateEntity quiz) {
        String correct = TopicState.correctOption(quiz).orElse(null);
        StringBuilder sb = new StringBuilder();
        for (SentencePartEntity part : TopicState.partsOf(quiz)) {
            if (part == null) {
                continue;
            }
            if (part.getKind() == SentencePartKind.CLOZE) {
                sb.append(' ').append(correct != null ? correct : TopicState.firstAnswer(part));
            } else if (part.getText() != null) {
                sb.append(' ').append(HINT.matcher(part.getText()).replaceAll(" "));
            }
        }
        return sb.toString();
    }

    private static String statement(QuizTemplateEntity quiz) {
        StringBuilder sb = new StringBuilder();
        for (SentencePartEntity part : TopicState.partsOf(quiz)) {
            if (part != null && part.getKind() == SentencePartKind.TEXT && part.getText() != null) {
                sb.append(part.getText());
            }
        }
        return sb.toString();
    }

    private static String answers(QuizTemplateEntity quiz) {
        List<String> answers = new ArrayList<>();
        for (SentencePartEntity part : TopicState.partsOf(quiz)) {
            if (part != null && part.getOptions() != null) {
                answers.addAll(part.getOptions().stream().filter(o -> o != null).toList());
            }
        }
        return String.join(" ", answers);
    }

    private static String options(QuizTemplateEntity quiz) {
        if (quiz.getForm() == null || quiz.getForm().getMultipleChoice() == null
                || quiz.getForm().getMultipleChoice().getItems() == null) {
            return null;
        }
        return String.join(" ", quiz.getForm().getMultipleChoice().getItems().stream()
                .filter(i -> i != null && i.getLabel() != null).map(MultipleChoiceItemEntity::getLabel).toList());
    }

    private static int breaks(String text) {
        if (text == null) {
            return 0;
        }
        int count = 0;
        Matcher m = LINE_BREAK.matcher(text);
        while (m.find()) {
            count++;
        }
        return count;
    }

    private static List<String> tokens(String text) {
        List<String> tokens = new ArrayList<>();
        Matcher m = TOKEN.matcher(text);
        while (m.find()) {
            tokens.add(m.group());
        }
        return tokens;
    }

    /** Capitalized words right after a mark that ends a sentence: where a sentence opens. */
    private static Set<String> starts(List<String> tokens) {
        Set<String> starts = new LinkedHashSet<>();
        for (int i = 1; i < tokens.size(); i++) {
            if (BOUNDARY.matcher(tokens.get(i - 1)).matches() && capitalized(tokens.get(i))) {
                starts.add(tokens.get(i));
            }
        }
        return starts;
    }

    /** Capitalized words right after another word: a name, or a sentence glued to the one before. */
    private static Set<String> midSentence(List<String> tokens) {
        Set<String> mid = new LinkedHashSet<>();
        for (int i = 1; i < tokens.size(); i++) {
            if (!isMark(tokens.get(i - 1)) && capitalized(tokens.get(i))) {
                mid.add(tokens.get(i));
            }
        }
        return mid;
    }

    private static boolean capitalized(String word) {
        // «I» and its contractions go in capitals anywhere in the sentence.
        return !isMark(word) && Character.isUpperCase(word.codePointAt(0)) && !word.equals("I")
                && !word.startsWith("I'") && !word.startsWith("I’");
    }

    private static boolean isMark(String token) {
        return MARK.matcher(token).matches();
    }

    private static String key(String... tokens) {
        return String.join("\u0000", tokens).toLowerCase(Locale.ROOT);
    }

    private static String markName(String mark) {
        return switch (mark) {
            case "." -> "el punto";
            case "," -> "la coma";
            case ";" -> "el punto y coma";
            case ":" -> "los dos puntos";
            case "?" -> "el signo de pregunta";
            case "!" -> "el signo de exclamación";
            case "...", "…" -> "los puntos suspensivos";
            default -> "el signo «" + mark + "»";
        };
    }
}
