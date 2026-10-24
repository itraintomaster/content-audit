package com.learney.contentaudit.revisiondomain.panelengine;

import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.SentenceMode;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Preserves an explicit request selector in the can/could exercise, without inferring one from an answer. */
final class RequestFormPreservation {
    private static final Pattern CAN = Pattern.compile("\\bcan\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern COULD = Pattern.compile("\\bcould\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CUE = Pattern.compile("(?:^\\s*|[.!?]\\s+)(polite|normal)(?:\\s+request)?\\s*:", Pattern.CASE_INSENSITIVE);
    private static final Pattern HINT = Pattern.compile("\\([^)]*\\)");
    private static final Pattern WORD = Pattern.compile("[a-z]+(?:'[a-z]+)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern INTERCHANGEABLE = Pattern.compile("\\b(?:indistintamente|intercambiables|interchangeably)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PERMISSION = Pattern.compile("^\\s*(?:usa|usá|utiliza|utilizá|emplea|empleá|puedes\\s+usar|podés\\s+usar|se\\s+pueden\\s+usar|use|you\\s+(?:may|can)\\s+use)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern NEGATION = Pattern.compile("\\b(?:no|not|never|nunca|jamás|tampoco|dont|cannot|cant)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern EXCEPTION = Pattern.compile("\\b(?:pero|but|except|excepto|salvo|unless|only|solo|sólo|si|if|cuando|when)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern POLITE = Pattern.compile("\\b(?:polite|cortés|corteses)\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern NORMAL = Pattern.compile("\\b(?:normal|normales)\\b", Pattern.CASE_INSENSITIVE);
    private static final String OTHER = "(otra forma)";

    private RequestFormPreservation() {
    }

    static List<String> verify(QuizTemplateEntity original, QuizTemplateEntity proposed, String topicInstructions,
            SentenceMode mode) {
        String instructions = original.getInstructions();
        if (instructions == null || instructions.isBlank()) instructions = topicInstructions;
        if (instructions == null || !CAN.matcher(instructions).find() || !COULD.matcher(instructions).find()) return List.of();
        Set<String> selectors = selectors(original);
        // No selector means no inferred obligation; conflicting original selectors need editorial review.
        if (selectors.size() != 1) return List.of();
        String selector = selectors.iterator().next();
        String expected = selector.equals("polite") ? "could" : "can";
        String label = selector.equals("polite") ? "Polite request" : "Normal request";
        boolean interchangeable = allowsBothForms(instructions, selector);
        List<String> violations = new ArrayList<>();
        if (!selectors.equals(selectors(proposed))) {
            violations.add("no conserva la indicación «" + label + "»" + (interchangeable
                    ? " del ejercicio original" : ": debe seguir pidiendo «" + expected + "»"));
        }
        // Without a mode the prompt/answer boundary is unknown; the explicit selector still stays protected.
        if (!interchangeable && mode != null) {
            Set<String> forms = answerForms(proposed, mode);
            if (!forms.equals(Set.of(expected))) {
                violations.add("la indicación «" + label + "» pide «" + expected
                        + "»; las respuestas propuestas usan " + String.join(", ", forms));
            }
        }
        return violations;
    }

    /** Used only for the separate explicit operator order to close a visible request formula. */
    static String requestModal(QuizTemplateEntity source) {
        String instructions = source.getInstructions();
        var selected = selectors(source);
        if (instructions == null || !CAN.matcher(instructions).find() || !COULD.matcher(instructions).find() || selected.size() != 1) return null;
        return selected.contains("polite") ? "Could" : "Can";
    }

    static List<String> closedFormulaViolations(QuizTemplateEntity proposed, String modal, String directive, SentenceMode mode) {
        List<String> violations = new ArrayList<>();
        if (proposed.getInstructions() == null || !proposed.getInstructions().contains(directive))
            violations.add("el encargo pide mostrar en la consigna local: «" + directive + "»");
        if (mode == null || !answerForms(proposed, mode).equals(Set.of(modal.toLowerCase(Locale.ROOT))))
            violations.add("la fórmula explícita pide comenzar la respuesta con «" + modal + "»");
        var choices = new ArrayList<String>();
        var form = proposed.getForm();
        if (form != null && form.getMultipleChoice() != null && form.getMultipleChoice().getItems() != null) {
            choices.addAll(form.getMultipleChoice().getItems().stream().filter(i -> i != null && i.getIncidence() > 0)
                    .map(i -> i.getLabel()).toList());
            if (choices.size() != 1 || !singleVariant(choices.get(0))) violations.add("el encargo de fórmula cerrada pide una sola respuesta aceptada, sin variantes");
        } else {
            boolean gap = false;
            for (var part : parts(proposed)) if (part != null) {
                if (part.getKind() == SentencePartKind.CLOZE) {
                    gap = true;
                    if (part.getOptions() == null || part.getOptions().size() != 1 || !singleVariant(part.getOptions().get(0)))
                        violations.add("el encargo de fórmula cerrada pide una sola respuesta por hueco, sin variantes opcionales");
                    if (part.getOptions() != null) choices.addAll(part.getOptions());
                } else if (gap || mode == SentenceMode.FILL) choices.add(part.getText());
            }
            if (!gap) violations.add("falta la respuesta para comprobar la fórmula cerrada");
        }
        if (choices.stream().filter(java.util.Objects::nonNull).anyMatch(t -> Pattern.compile("\\bplease\\b", Pattern.CASE_INSENSITIVE).matcher(t).find()))
            violations.add("el encargo explícito de esta fórmula excluye please y otras adiciones opcionales");
        return violations;
    }

    private static boolean singleVariant(String answer) {
        return answer != null && !answer.isBlank() && answer.split("[|/]", -1).length == 1;
    }

    /** A positive original instruction can explicitly allow either form; a candidate cannot grant itself permission. */
    private static boolean allowsBothForms(String instructions, String selector) {
        // An exception is not an unconditional permission. Leave its interpretation to the judges/editor.
        if (EXCEPTION.matcher(instructions).find()) return false;
        List<String> clauses = new ArrayList<>();
        for (String raw : instructions.split("[.;!?\\n]")) {
            String clause = raw.replace("'", "").replace("\"", "").replace("$", "");
            boolean can = CAN.matcher(clause).find(), could = COULD.matcher(clause).find();
            if (can != could && (POLITE.matcher(clause).find() || NORMAL.matcher(clause).find())) return false;
            clauses.add(clause);
        }
        for (String clause : clauses) {
            boolean polite = POLITE.matcher(clause).find(), normal = NORMAL.matcher(clause).find();
            if (selector.equals("polite") ? normal && !polite : polite && !normal) continue;
            if (!NEGATION.matcher(clause).find() && PERMISSION.matcher(clause).find()
                    && CAN.matcher(clause).find() && COULD.matcher(clause).find()
                    && INTERCHANGEABLE.matcher(clause).find()) return true;
        }
        return false;
    }

    private static Set<String> selectors(QuizTemplateEntity quiz) {
        Set<String> selectors = new LinkedHashSet<>();
        for (SentencePartEntity part : parts(quiz)) {
            if (part == null || part.getKind() != SentencePartKind.TEXT || part.getText() == null) continue;
            Matcher matcher = CUE.matcher(part.getText());
            if (matcher.find()) selectors.add(matcher.group(1).toLowerCase(Locale.ROOT));
        }
        return selectors;
    }

    /**
     * Only the leading form of the answer matters. A rewrite's preceding prompt is not part of the answer;
     * a fill-in keeps its fixed prefix. Once the leading word is known, later gaps cannot turn a lexical «can» into
     * a modal. Keeping only unfinished prefixes avoids enumerating the Cartesian product of all accepted answers.
     */
    private static Set<String> answerForms(QuizTemplateEntity quiz, SentenceMode mode) {
        List<SentencePartEntity> parts = parts(quiz);
        List<String> correctOptions = quiz.getForm() != null && quiz.getForm().getMultipleChoice() != null
                && quiz.getForm().getMultipleChoice().getItems() != null
                ? quiz.getForm().getMultipleChoice().getItems().stream()
                        .filter(item -> item != null && item.getIncidence() > 0.0).map(item -> item.getLabel()).toList()
                : List.of();
        int firstGap = 0;
        StringBuilder prefix = new StringBuilder();
        while (firstGap < parts.size() && (parts.get(firstGap) == null || parts.get(firstGap).getKind() != SentencePartKind.CLOZE)) {
            SentencePartEntity part = parts.get(firstGap++);
            if (part != null && part.getText() != null) prefix.append(' ').append(CUE.matcher(part.getText()).replaceFirst(""));
        }
        String fixed = mode == SentenceMode.REWRITE ? "" : HINT.matcher(prefix.toString()).replaceAll(" ");
        Set<String> forms = new LinkedHashSet<>();
        Set<String> unfinished = advance(Set.of(""), List.of(fixed), forms);
        if (firstGap == parts.size() && !correctOptions.isEmpty()) unfinished = advance(unfinished, correctOptions, forms);
        for (int i = firstGap; i < parts.size() && !unfinished.isEmpty(); i++) {
            SentencePartEntity part = parts.get(i);
            if (part == null) continue;
            List<String> choices = part.getKind() == SentencePartKind.CLOZE
                    ? !correctOptions.isEmpty() ? correctOptions : part.getOptions()
                    : List.of(HINT.matcher(part.getText() != null ? part.getText() : "").replaceAll(" "));
            unfinished = advance(unfinished, choices, forms);
        }
        if (!unfinished.isEmpty() || forms.isEmpty()) forms.add(OTHER);
        return forms;
    }

    private static Set<String> advance(Set<String> prefixes, List<String> choices, Set<String> forms) {
        Set<String> unfinished = new LinkedHashSet<>();
        for (String prefix : prefixes) {
            for (String choice : choices != null && !choices.isEmpty() ? choices : List.of("")) {
                for (String variant : (choice != null ? choice : "").split("[|/]", -1)) {
                    String joined = prefix + " " + variant;
                    Matcher words = WORD.matcher(joined);
                    String leading = null;
                    while (words.find()) {
                        String word = words.group().toLowerCase(Locale.ROOT);
                        if (!word.equals("please")) { leading = word; break; }
                    }
                    if (leading == null) unfinished.add(joined);
                    else forms.add(leading.equals("can") || leading.equals("could") ? leading : OTHER);
                }
            }
        }
        return unfinished;
    }

    private static List<SentencePartEntity> parts(QuizTemplateEntity quiz) {
        return quiz.getForm() != null && quiz.getForm().getSentenceParts() != null ? quiz.getForm().getSentenceParts() : List.of();
    }
}
