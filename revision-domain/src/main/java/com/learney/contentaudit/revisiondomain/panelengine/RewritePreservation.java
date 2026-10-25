package com.learney.contentaudit.revisiondomain.panelengine;

import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.SentenceMode;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * F-MESA-R019: an exercise of a rewrite topic stays one. When its starting point gives the sentence to transform before
 * the gap and the student writes the whole answer in it, the candidate does the same: «That house belongs to Mary. ___»
 * may not become «That is ___ (Mary) house.», where the student no longer transforms anything.
 */
final class RewritePreservation {
    /** The end of a sentence: its mark, the quotes or brackets that close it, and a space. */
    private static final Pattern SENTENCE_END = Pattern.compile("[.?!…][\"»”')]*\\s");
    private static final Pattern HINT = Pattern.compile("\\([^)]*\\)");
    /** What may sit between the sentence to transform and the gap, or after the gap: arrows, marks and spaces. */
    private static final Pattern FILLER = Pattern.compile("[\\s→⇒=>:\\-–—.,;!?…\"»”')]*");
    /** Between the sentence and the gap there may also be the label that says what to do: «Play the radio. Polite: ___». */
    private static final Pattern LABEL_AND_FILLER = Pattern.compile("(?:\\p{L}+(?:\\s+\\p{L}+)?\\s*:)?" + FILLER.pattern());

    private RewritePreservation() {
    }

    static List<String> violations(QuizTemplateEntity startingPoint, QuizTemplateEntity proposed, SentenceMode mode) {
        if (mode != SentenceMode.REWRITE || !transforms(startingPoint) || transforms(proposed)) {
            return List.of();
        }
        String given = "deja de ser de transformar: el punto de partida da «" + source(startingPoint)
                + "» y el alumno escribe la oración entera transformada; ";
        List<SentencePartEntity> parts = parts(proposed);
        int gap = firstGap(parts);
        boolean anySentence = gap >= 0 && lastSentenceEnd(before(parts, gap)) >= 0;
        return List.of(given + (!anySentence
                ? "el candidato no da una oración de partida y deja un hueco para completar"
                : "el candidato deja texto fijo alrededor del hueco y el alumno ya no escribe la oración entera"));
    }

    /** It gives a sentence before the gap, and from there on there are only gaps and marks: the answer is whole. */
    static boolean transforms(QuizTemplateEntity quiz) {
        List<SentencePartEntity> parts = parts(quiz);
        int firstGap = firstGap(parts);
        if (firstGap < 0) {
            return false;
        }
        String before = before(parts, firstGap);
        int end = lastSentenceEnd(before);
        boolean sentenceBefore = end >= 0 && LABEL_AND_FILLER.matcher(before.substring(end).trim()).matches();
        // A sentence that lost only its period is still there when the gap opens a whole sentence: R018 says the rest.
        if (!sentenceBefore && (before.isBlank() || !opensASentence(parts.get(firstGap)))) {
            return false;
        }
        for (int i = firstGap; i < parts.size(); i++) {
            SentencePartEntity part = parts.get(i);
            if (part != null && part.getKind() != SentencePartKind.CLOZE && !FILLER.matcher(clean(part.getText())).matches()) {
                return false;
            }
        }
        return true;
    }

    /** Every answer of the gap starts with a capital and ends with its mark: the student writes a whole sentence. */
    private static boolean opensASentence(SentencePartEntity gap) {
        List<String> options = gap.getOptions();
        if (options == null || options.isEmpty()) {
            return false;
        }
        for (String option : options) {
            String answer = option == null ? "" : option.replaceFirst("^[\\s\"«“¿¡']+", "");
            if (answer.isEmpty() || !Character.isUpperCase(answer.codePointAt(0))
                    || !SENTENCE_END.matcher(answer.strip() + " ").find()) {
                return false;
            }
        }
        return true;
    }

    /** The sentences before the gap, as the student reads them, without the hints and the arrow. */
    private static String source(QuizTemplateEntity quiz) {
        List<SentencePartEntity> parts = parts(quiz);
        int firstGap = firstGap(parts);
        String before = firstGap < 0 ? "" : before(parts, firstGap);
        int end = lastSentenceEnd(before);
        return (end < 0 ? before : before.substring(0, end)).replaceAll("\\s+", " ").trim();
    }

    private static int lastSentenceEnd(String text) {
        Matcher matcher = SENTENCE_END.matcher(text + " ");
        int end = -1;
        while (matcher.find()) {
            end = Math.min(matcher.end(), text.length());
        }
        return end;
    }

    private static String before(List<SentencePartEntity> parts, int firstGap) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < firstGap; i++) {
            if (parts.get(i) != null) {
                text.append(clean(parts.get(i).getText())).append(' ');
            }
        }
        return text.toString();
    }

    private static int firstGap(List<SentencePartEntity> parts) {
        for (int i = 0; i < parts.size(); i++) {
            if (parts.get(i) != null && parts.get(i).getKind() == SentencePartKind.CLOZE) {
                return i;
            }
        }
        return -1;
    }

    private static String clean(String text) {
        return text == null ? "" : HINT.matcher(text).replaceAll(" ");
    }

    private static List<SentencePartEntity> parts(QuizTemplateEntity quiz) {
        return quiz.getForm() != null && quiz.getForm().getSentenceParts() != null ? quiz.getForm().getSentenceParts()
                : List.of();
    }
}
