package com.learney.contentaudit.revisiondomain.panelengine;

import com.learney.contentaudit.auditdomain.spanishregister.Voseo;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * F-MESA-R021: what the student reads in Spanish goes in «tú», as 2.062 of the 2.102 instructions of A1 do («Crea un
 * adverbio…», «Usa $is$…»). A candidate that writes the instructions, the translation or the mini theory in «vos»
 * («Creá…», «usá», «añadí», «podés», «agregalo») is rejected before measuring. Only the text the candidate changed is
 * read: an old text it left as it was is not its doing; {@code spanish-register} finds those (F-MESA-R025).
 *
 * <p>{@link Voseo} tells it apart.
 */
final class SpanishRegister {
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
        return Voseo.in(text, instructs);
    }

    private static java.util.Optional<String> reason(String what, List<String> words) {
        if (words.isEmpty()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(what + " va de tú y dice «" + String.join("», «", words) + "», de vos");
    }
}
