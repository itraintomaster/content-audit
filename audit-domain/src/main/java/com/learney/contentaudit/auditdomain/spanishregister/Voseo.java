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
 * translation «Escribí tres oraciones» is the past, and «el verbo irá al final» is the future. And the imperative with
 * a pronoun and no accent («agregalo», «fijate», «preguntate»), which in «tú» carries one («agrégalo», «fíjate»).
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
    /** Words after which a clause opens, as a mark does: «… y agregalo», «Con $he$ usá …». */
    private static final Set<String> OPENS = Set.of("y", "e", "o", "u", "luego", "después", "primero", "entonces",
            "ahora", "siempre", "también");

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
        while (tokens.find()) {
            String token = tokens.group();
            String word = token.toLowerCase(Locale.ROOT);
            if (!Character.isLetter(token.charAt(0))) {
                opens = true;
                continue;
            }
            if (WITH_PRONOUN.contains(word)
                    || !NOT_A_VERB.contains(word) && !FUTURE.matcher(word).matches()
                    && (PRESENT.matcher(word).matches() || instructs && opens && IMPERATIVE.matcher(word).matches())) {
                found.add(token);
            }
            opens = OPENS.contains(word);
        }
        return List.copyOf(found);
    }

}
