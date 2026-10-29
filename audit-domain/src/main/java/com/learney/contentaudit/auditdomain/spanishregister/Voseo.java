package com.learney.contentaudit.auditdomain.spanishregister;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * F-MESA-R021 and R025: the words of a Spanish text that are in «vos», where what the student reads goes in «tú», as
 * 2.062 of the 2.102 instructions of A1 do («Crea un adverbio…», «Usa $is$…»).
 *
 * <p>Three forms tell «vos» apart. The present («podés», «decidís», «necesitás») anywhere, but not the future
 * («dormirás», «sabrás») nor the words that end the same way («más», «inglés», «cortés», «país»). The imperative with
 * its stress on the end («usá», «añadí», «poné») only in what instructs -- the instructions and the mini theory --, and
 * only where a clause opens -- at the start, after a mark, after an English term or after «y» --, because in a
 * translation «Escribí tres oraciones» is the past, and «el verbo irá al final» is the future. One ending in «é» or «í»
 * («poné», «elegí») is written as the past of «yo» («compré», «escuché»), so it counts only where a sentence or a clause
 * opens -- at the start, after «.», «,», «;», «:» or «y» --, not after an English example or a parenthesis, where a card
 * translates it: «$I heard a funny noise.$ = Escuché un ruido raro.». And the imperative with a pronoun and no accent
 * («agregalo», «fijate», «preguntate»), which in «tú» carries one («agrégalo», «fíjate»).
 *
 * <p>The pronoun «vos» is «vos» too («respondes sobre vos»). A form the table of {@link Tuteo} knows is «vos» though it
 * ends like a future («esperás», «cerrá», «separá»), and an English subject written without its marks opens a clause as
 * a marked one does: «Con he usá $was$».
 */
public final class Voseo {
    /** An English term between the native marks: the student reads it in English, so it is not Spanish. */
    private static final Pattern ENGLISH = Pattern.compile("\\$[^$]*\\$");
    private static final Pattern TOKEN = Pattern.compile("[\\p{L}]+|[.,;:!?¿¡()«»\"—–\\-]|\\$");
    private static final Pattern PRESENT = Pattern.compile("\\p{L}{2,}(ás|és|ís)");
    private static final Pattern IMPERATIVE = Pattern.compile("\\p{L}{2,}[áéí]");
    /**
     * The future, which ends like the present or the imperative of «vos»: «dormirás», «sabrás», «tendré». Its infinitive
     * has two letters or more before «ar», «er» or «ir», so «mirá» and «parás» stay «vos»; the short ones («dará»,
     * «será», «irá») are words that are not a verb of «vos».
     */
    private static final Pattern FUTURE = Pattern.compile(".{2,}(ar|er|ir)(á|é|ás)|.*(dr|br|rr)(á|é|ás)|har(á|é|ás)"
            + "|dir(á|é|ás)");
    private static final Set<String> NOT_A_VERB = Set.of("más", "jamás", "atrás", "detrás", "demás", "además",
            "quizás", "estás", "estés", "inglés", "francés", "japonés", "portugués", "holandés", "escocés", "irlandés",
            "danés", "cortés", "descortés", "después", "través", "interés", "estrés", "revés", "país", "maíz", "anís",
            "parís", "tomás", "nicolás", "andrés", "bebés", "cafés", "sofás", "mamás", "papás", "está", "esté", "aquí",
            "allí", "allá", "acá", "ahí", "así", "qué", "sí", "mí", "té", "sé", "dé", "café", "mamá", "papá", "sofá",
            "bebé", "canadá", "panamá", "quizá", "puré", "josé", "inés", "moisés", "finlandés", "tailandés",
            "neozelandés", "marqués", "ciprés", "compás", "esquís", "rubís", "ajís", "dará", "darás", "daré", "irá",
            "irás", "iré", "verá", "verás", "veré", "será", "serás", "seré");
    /** The imperative of «vos» with a pronoun, which «tú» writes with an accent: «agregalo» for «agrégalo». */
    private static final Set<String> WITH_PRONOUN = Set.of("agregalo", "agregala", "agregalos", "agregalas", "colocalo",
            "colocala", "colocalos", "colocalas", "preguntate", "fijate", "ponelo", "ponela", "decime", "decilo",
            "contame", "acordate", "usalo", "usala", "escribilo", "escribila", "completalo", "completala", "elegilo",
            "elegila", "cambialo", "cambiala", "pasalo", "pasala", "ponete", "mirala", "miralo", "anotalo", "anotala");
    /** The marks after which a sentence or a clause opens. */
    private static final Set<String> CLAUSE_MARKS = Set.of(".", ",", ";", ":", "!", "?", "¿", "¡");
    /** Words after which a clause opens, as a mark does: «… y agregalo», «Con $he$ usá …». */
    private static final Set<String> OPENS = Set.of("y", "e", "o", "u", "luego", "después", "primero", "entonces",
            "ahora", "siempre", "también");
    /** An English subject a card writes without its marks: «con I, he, she, it usá $was not$». */
    private static final Set<String> ENGLISH_SUBJECTS = Set.of("i", "you", "he", "she", "it", "we", "they");
    /** The pronoun of «vos», which in «tú» is «tú», «ti» or «contigo». */
    static final String PRONOUN = "vos";

    private Voseo() {
    }

    /** The words of the text that are in «vos», in their order, once each. */
    public static List<String> in(String text, boolean instructs) {
        Set<String> found = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return List.of();
        }
        Matcher tokens = TOKEN.matcher(ENGLISH.matcher(text).replaceAll(" \\$ "));
        boolean opens = true;
        boolean clause = true;
        while (tokens.find()) {
            String token = tokens.group();
            String word = token.toLowerCase(Locale.ROOT);
            if (!Character.isLetter(token.charAt(0))) {
                opens = true;
                clause = CLAUSE_MARKS.contains(token);
                continue;
            }
            boolean imperative = instructs && IMPERATIVE.matcher(word).matches() && (word.endsWith("á") ? opens : clause);
            boolean future = FUTURE.matcher(word).matches() && !Tuteo.knows(word);
            if (WITH_PRONOUN.contains(word) || PRONOUN.equals(word)
                    || !NOT_A_VERB.contains(word) && !future
                    && (PRESENT.matcher(word).matches() || imperative)) {
                found.add(token);
            }
            if (ENGLISH_SUBJECTS.contains(word)) {
                // As after an English term between marks: an «á» opens there, an «é» or «í» may be its translation.
                opens = true;
                clause = false;
                continue;
            }
            opens = OPENS.contains(word);
            clause = opens;
        }
        return List.copyOf(found);
    }

}
