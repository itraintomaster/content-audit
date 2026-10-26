package com.learney.contentaudit.revisiondomain.panelengine;

import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * F-MESA-R021: what the student reads in Spanish goes in «tú», as 2.062 of the 2.102 instructions of A1 do («Crea un
 * adverbio…», «Usa $is$…»). A candidate that writes the instructions, the translation or the mini theory in «vos»
 * («Creá…», «usá», «añadí», «podés», «agregalo») is rejected before measuring. Only the text the candidate changed is
 * read: an old text it left as it was is not its doing.
 *
 * <p>Three forms tell «vos» apart. The present («podés», «decidís», «necesitás») anywhere, but not the future
 * («dormirás», «sabrás») nor the words that end the same way («más», «inglés», «cortés», «país»). The imperative with
 * its stress on the end («usá», «añadí», «poné») only in the instructions and the mini theory, and only where a clause
 * opens -- at the start, after a mark, after an English term or after «y» --, because in a translation «Escribí tres
 * oraciones» is the past, and «el verbo irá al final» is the future. And the imperative with a pronoun and no accent
 * («agregalo», «fijate», «preguntate»), which in «tú» carries one («agrégalo», «fíjate»).
 */
final class SpanishRegister {
    /** An English term between the native marks: the student reads it in English, so it is not Spanish. */
    private static final Pattern ENGLISH = Pattern.compile("\\$[^$]*\\$");
    private static final Pattern TOKEN = Pattern.compile("[\\p{L}]+|[.,;:!?¿¡()«»\"—–\\-]|\\$");
    private static final Pattern PRESENT = Pattern.compile("\\p{L}{2,}(ás|és|ís)");
    private static final Pattern IMPERATIVE = Pattern.compile("\\p{L}{2,}[áéí]");
    /** The future, which ends like the present or the imperative of «vos»: «dormirás», «sabrás», «irá», «tendré». */
    private static final Pattern FUTURE = Pattern.compile(".*(ar|er|ir|dr|br|rr)(á|é|ás)|har(á|é|ás)|dir(á|é|ás)");
    private static final Set<String> NOT_A_VERB = Set.of("más", "jamás", "atrás", "detrás", "demás", "además",
            "quizás", "estás", "estés", "inglés", "francés", "japonés", "portugués", "holandés", "escocés", "irlandés",
            "danés", "cortés", "descortés", "después", "través", "interés", "estrés", "revés", "país", "maíz", "anís",
            "parís", "tomás", "nicolás", "andrés", "bebés", "cafés", "sofás", "mamás", "papás", "está", "esté", "aquí",
            "allí", "allá", "acá", "ahí", "así", "qué", "sí", "mí", "té", "sé", "dé", "café", "mamá", "papá", "sofá",
            "bebé", "canadá", "panamá", "quizá", "puré");
    /** The imperative of «vos» with a pronoun, which «tú» writes with an accent: «agregalo» for «agrégalo». */
    private static final Set<String> WITH_PRONOUN = Set.of("agregalo", "agregala", "agregalos", "agregalas", "colocalo",
            "colocala", "colocalos", "colocalas", "preguntate", "fijate", "ponelo", "ponela", "decime", "decilo",
            "contame", "acordate", "usalo", "usala", "escribilo", "escribila", "completalo", "completala", "elegilo",
            "elegila", "cambialo", "cambiala", "pasalo", "pasala", "ponete", "mirala", "miralo", "anotalo", "anotala");
    /** Words after which a clause opens, as a mark does: «… y agregalo», «Con $he$ usá …». */
    private static final Set<String> OPENS = Set.of("y", "e", "o", "u", "luego", "después", "primero", "entonces",
            "ahora", "siempre", "también");

    private record Field(String name, Function<QuizTemplateEntity, String> text, boolean instructs) {
    }

    private static final List<Field> FIELDS = List.of(
            new Field("la consigna", QuizTemplateEntity::getInstructions, true),
            new Field("la miniteoría", QuizTemplateEntity::getMiniTheory, true),
            new Field("la traducción", QuizTemplateEntity::getTranslation, false));

    private SpanishRegister() {
    }

    /** The reasons to reject a candidate that writes in «vos» what it changed of the exercise. */
    static List<String> violations(QuizTemplateEntity startingPoint, QuizTemplateEntity proposed) {
        List<String> violations = new ArrayList<>();
        for (Field field : FIELDS) {
            String after = field.text().apply(proposed);
            if (!Objects.equals(field.text().apply(startingPoint), after)) {
                reason(field.name(), voseo(after, field.instructs())).ifPresent(violations::add);
            }
        }
        return violations;
    }

    /** The reason to reject a text the candidate wrote, if it is in «vos»: a new mini theory or topic instructions. */
    static java.util.Optional<String> violation(String what, String text) {
        return reason(what, voseo(text, true));
    }

    /** The words of the text that are in «vos», in their order, once each. */
    static List<String> voseo(String text, boolean instructs) {
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

    private static java.util.Optional<String> reason(String what, List<String> words) {
        if (words.isEmpty()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(what + " va de tú y dice «" + String.join("», «", words) + "», de vos");
    }
}
