package com.learney.contentaudit.auditdomain.standardenglish;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * F-UNICA-R009 and F-MESA-R030: the forms the course never accepts as an answer, because they are not standard English.
 * In the C1 of A1 (7-9/10) the panel accepted «ain't» in 14 exercises after the single-answer judge said that «No, he
 * ain't» is natural «en variedades coloquiales»; it also proposed «amn't». The list is closed and short: a form that is
 * informal but standard, as the contractions, is not here.
 */
public final class NonStandardForms {

    /** A word, the straight and the curved apostrophe inside it. */
    private static final Pattern WORD = Pattern.compile("[A-Za-z]+(?:['’][A-Za-z]+)*");

    private static final List<String> FORMS = List.of("ain't", "amn't", "gonna", "wanna", "gotta", "innit");

    private NonStandardForms() {
    }

    /** The non-standard forms the text has, in their order and lower case, each once. */
    public static List<String> in(String text) {
        List<String> found = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return found;
        }
        Matcher m = WORD.matcher(text);
        while (m.find()) {
            String word = m.group().toLowerCase(Locale.ROOT).replace('’', '\'');
            if (FORMS.contains(word) && !found.contains(word)) {
                found.add(word);
            }
        }
        return found;
    }

    /** Whether the text has a non-standard form. */
    public static boolean has(String text) {
        return !in(text).isEmpty();
    }
}
