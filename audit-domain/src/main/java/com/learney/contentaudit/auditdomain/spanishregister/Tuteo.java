package com.learney.contentaudit.auditdomain.spanishregister;

import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * F-MESA-R027: an old text in «vos» goes in «tú» by rule. Each word {@link Voseo} finds and this table knows is written
 * as the course writes it, with its stress («agregalo» → «agrégalo», «poné» → «pon», «podés» → «puedes»); a word the
 * table does not know stays, and an English term between «$» is never touched.
 *
 * <p>The table holds the forms of the 699 published exercises and of the first correction of A1 (25/10/2026), and the
 * common verbs of the instructions and the cards. The pronoun «vos» goes by where it stands: «contigo» after «con»,
 * «ti» after another preposition («respondes sobre ti») and «tú» elsewhere («(tú pagas)»).
 */
public final class Tuteo {
    private static final Pattern ENGLISH = Pattern.compile("\\$[^$]*\\$");

    private static final Pattern WITH_YOU = Pattern.compile("(?<!\\p{L})([Cc])on vos(?!\\p{L})");

    private static final Pattern AFTER_PREPOSITION = Pattern.compile(
            "(?<!\\p{L})(a|ante|de|desde|en|hacia|para|por|sin|sobre|tras) vos(?!\\p{L})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern SUBJECT = Pattern.compile("(?<!\\p{L})([Vv])os(?!\\p{L})");

    private static final Map<String, String> TU = new HashMap<>();

    static {
        // The present: «vos podés» is «tú puedes».
        put("hablás hablas", "preferís prefieres", "usás usas", "lográs logras", "decidís decides", "querés quieres",
                "preguntás preguntas", "repetís repites", "sentís sientes", "podés puedes", "movés mueves", "decís dices",
                "creés crees", "remarcás remarcas", "sumás sumas", "tratás tratas", "contás cuentas", "pagás pagas",
                "escribís escribes", "dejás dejas", "alquilás alquilas", "soñás sueñas", "leés lees", "hacés haces",
                "elegís eliges", "bailás bailas", "enseñás enseñas", "presentás presentas", "escapás escapas",
                "pedís pides", "mandás mandas", "vivís vives", "sabés sabes", "convertís conviertes", "mirás miras",
                "parás paras", "respondés respondes", "tenés tienes", "escuchás escuchas", "conducís conduces",
                "recibís recibes", "viajás viajas", "buscás buscas", "pensás piensas", "necesitás necesitas",
                "prestás prestas", "comés comes", "venís vienes", "salís sales", "ponés pones", "traés traes",
                "entendés entiendes", "empezás empiezas", "jugás juegas", "volvés vuelves", "dormís duermes",
                "cerrás cierras", "perdés pierdes", "encontrás encuentras", "recordás recuerdas", "probás pruebas",
                "mostrás muestras", "conocés conoces", "parecés pareces", "ofrecés ofreces", "llegás llegas",
                "trabajás trabajas", "estudiás estudias", "llamás llamas", "comprás compras", "cocinás cocinas",
                "caminás caminas", "corrés corres", "aprendés aprendes", "comprendés comprendes", "ayudás ayudas",
                "olvidás olvidas", "terminás terminas", "esperás esperas", "preparás preparas", "completás completas",
                "colocás colocas", "agregás agregas", "cambiás cambias", "notás notas", "describís describes",
                "explicás explicas", "contestás contestas", "practicás practicas", "imaginás imaginas",
                "vendés vendes", "abrís abres", "subís subes", "seguís sigues", "servís sirves", "mentís mientes",
                "medís mides", "sugerís sugieres", "referís refieres", "gustás gustas", "invitás invitas",
                "visitás visitas");
        // The imperative: «vos usá» is «tú usa».
        put("usá usa", "elegí elige", "añadí añade", "colocá coloca", "completá completa", "poné pon",
                "agregá agrega", "creá crea", "repetí repite", "escribí escribe", "quitá quita", "contestá contesta",
                "cambiá cambia", "sumá suma", "reemplazá reemplaza", "mantené mantén", "mirá mira", "duplicá duplica",
                "empezá empieza", "contraé contrae", "clasificá clasifica", "respondé responde", "dejá deja",
                "buscá busca", "preguntá pregunta", "probá prueba", "fijá fija", "decí di", "hacé haz", "tené ten",
                "vení ven", "salí sal", "leé lee", "traducí traduce", "traé trae", "pensá piensa", "recordá recuerda",
                "mostrá muestra", "volvé vuelve", "cerrá cierra", "contá cuenta", "jugá juega", "seguí sigue",
                "pedí pide", "corregí corrige", "compará compara", "observá observa", "notá nota", "marcá marca",
                "separá separa", "ordená ordena", "transformá transforma", "convertí convierte", "formá forma",
                "armá arma", "revisá revisa", "prestá presta", "tomá toma", "sacá saca", "llená llena", "uní une",
                "escuchá escucha", "pronunciá pronuncia", "practicá practica", "imaginá imagina", "describí describe",
                "explicá explica", "indicá indica", "señalá señala", "subrayá subraya", "relacioná relaciona",
                "conjugá conjuga", "negá niega", "afirmá afirma", "decidí decide", "pasá pasa", "anotá anota",
                "agrupá agrupa", "unificá unifica", "evitá evita");
        // The imperative with a pronoun, which «tú» writes with a stress.
        put("agregalo agrégalo", "agregala agrégala", "agregalos agrégalos", "agregalas agrégalas",
                "colocalo colócalo", "colocala colócala", "colocalos colócalos", "colocalas colócalas",
                "preguntate pregúntate", "fijate fíjate", "ponelo ponlo", "ponela ponla", "decime dime", "decilo dilo",
                "contame cuéntame", "acordate acuérdate", "usalo úsalo", "usala úsala", "escribilo escríbelo",
                "escribila escríbela", "completalo complétalo", "completala complétala", "elegilo elígelo",
                "elegila elígela", "cambialo cámbialo", "cambiala cámbiala", "pasalo pásalo", "pasala pásala",
                "ponete ponte", "mirala mírala", "miralo míralo", "anotalo anótalo", "anotala anótala");
    }

    private Tuteo() {
    }

    private static void put(String... pairs) {
        for (String pair : pairs) {
            String[] words = pair.split(" ");
            TU.put(words[0], words[1]);
        }
    }

    /** Whether the table writes this word of «vos» in «tú»: {@link Voseo} reads «esperás» as «vos», not as a future. */
    static boolean knows(String word) {
        return word != null && TU.containsKey(word.toLowerCase(Locale.ROOT));
    }

    /** The text with each word in «vos» the table knows written in «tú»; the rest of the text as it was. */
    public static String of(String text, boolean instructs) {
        if (text == null || text.isBlank()) {
            return text;
        }
        Map<String, String> replacements = new HashMap<>();
        boolean pronoun = false;
        for (String word : Voseo.in(text, instructs)) {
            if (Voseo.PRONOUN.equals(word.toLowerCase(Locale.ROOT))) {
                pronoun = true;
                continue;
            }
            String tu = TU.get(word.toLowerCase(Locale.ROOT));
            if (tu != null) {
                replacements.put(word, Character.isUpperCase(word.charAt(0))
                        ? Character.toUpperCase(tu.charAt(0)) + tu.substring(1) : tu);
            }
        }
        if (replacements.isEmpty() && !pronoun) {
            return text;
        }
        StringBuilder result = new StringBuilder();
        Matcher english = ENGLISH.matcher(text);
        int from = 0;
        while (english.find()) {
            result.append(inTu(text.substring(from, english.start()), replacements, pronoun)).append(english.group());
            from = english.end();
        }
        return result.append(inTu(text.substring(from), replacements, pronoun)).toString();
    }

    /**
     * The exercise a candidate left with the texts it did not change -- the instructions, the mini theory and the
     * translation it brought as they were, or did not bring -- written in «tú». What the candidate wrote is its own.
     */
    public static void keptInTu(QuizTemplateEntity old, QuizTemplateEntity candidate, QuizTemplateEntity rewritten) {
        if (old == null || candidate == null || rewritten == null) {
            return;
        }
        kept(old, candidate, rewritten, QuizTemplateEntity::getInstructions, QuizTemplateEntity::setInstructions, true);
        kept(old, candidate, rewritten, QuizTemplateEntity::getMiniTheory, QuizTemplateEntity::setMiniTheory, true);
        kept(old, candidate, rewritten, QuizTemplateEntity::getTranslation, QuizTemplateEntity::setTranslation, false);
    }

    /** Whether a text differs from the old one only by the old one written in «tú». */
    public static boolean onlyInTu(String old, String now, boolean instructs) {
        return old != null && !Objects.equals(old, now) && Objects.equals(of(old, instructs), now);
    }

    private static void kept(QuizTemplateEntity old, QuizTemplateEntity candidate, QuizTemplateEntity rewritten,
            Function<QuizTemplateEntity, String> text, BiConsumer<QuizTemplateEntity, String> set, boolean instructs) {
        String brought = text.apply(candidate);
        if (brought == null || Objects.equals(brought, text.apply(old))) {
            set.accept(rewritten, of(text.apply(old), instructs));
        }
    }

    private static String inTu(String spanish, Map<String, String> replacements, boolean pronoun) {
        String result = spanish;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replaceAll("(?<!\\p{L})" + Pattern.quote(entry.getKey()) + "(?!\\p{L})",
                    Matcher.quoteReplacement(entry.getValue()));
        }
        if (pronoun) {
            result = WITH_YOU.matcher(result).replaceAll(m -> m.group(1).equals("C") ? "Contigo" : "contigo");
            result = AFTER_PREPOSITION.matcher(result).replaceAll("$1 ti");
            result = SUBJECT.matcher(result).replaceAll(m -> m.group(1).equals("V") ? "Tú" : "tú");
        }
        return result;
    }
}
