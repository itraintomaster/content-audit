package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.revisiondomain.StoredSentenceRehydrator;
import com.learney.contentaudit.revisiondomain.conversion.MultipleChoiceConversion.Candidate;
import java.util.*;
import java.util.regex.Pattern;

/** Pure and conservative: no model, mutation or invented option. The stored order is the judged order. */
final class ExistingChoiceConversion {
    private static final Pattern HINT = Pattern.compile("\\(([^()]*)\\)");
    private static final Pattern WRITE = Pattern.compile("(?iu)\\b(escribe|escribí|escriba|escribir|completa|completá|complete|completar|rellena|rellená|write|fill in|complete)\\b");

    static Candidate inspect(BatchBook.Place place, boolean manual) {
        QuizTemplateEntity q = place.quiz();
        String reason = null;
        List<String> labels = List.of();
        String source = null;
        FormEntity form = q.getForm();
        List<SentencePartEntity> parts = form == null ? List.of() : BatchBook.list(form.getSentenceParts());
        List<SentencePartEntity> gaps = parts.stream().filter(Objects::nonNull)
                .filter(p -> p.getKind() == SentencePartKind.CLOZE).toList();
        if (QuizTemplateEntities.formKind(q) != FormKind.CLOZE) reason = "Sólo se convierte un cloze.";
        else if (q.getClozeOriginal() != null) reason = "Ya conserva una conversión anterior; no se reemplaza la copia original.";
        else if (place.knowledge().getSentenceMode() == SentenceMode.REWRITE) reason = "Es reescribir, no elegir.";
        else if (gaps.size() != 1) reason = "Tiene " + gaps.size() + " huecos; opción múltiple elige uno solo.";
        String answer = gaps.size() == 1 && gaps.get(0).getOptions() != null && !gaps.get(0).getOptions().isEmpty()
                ? Objects.toString(gaps.get(0).getOptions().get(0), "").split("\\|", -1)[0].strip() : "";
        if (reason == null && (answer.isBlank() || answer.matches(".*[.!?].*")
                || parts.stream().noneMatch(p -> p != null && p.getKind() == SentencePartKind.TEXT && p.getText() != null && !p.getText().isBlank())))
            reason = "La respuesta es una oración entera o está vacía; no es elegir una palabra.";
        boolean hasBaseHint = false;
        if (reason == null) {
            List<List<String>> explicit = new ArrayList<>();
            for (SentencePartEntity part : parts) {
                if (part == null || part.getKind() != SentencePartKind.TEXT) continue;
                var matcher = HINT.matcher(Objects.toString(part.getText(), ""));
                while (matcher.find()) {
                    if (!matcher.group(1).contains("/")) { hasBaseHint = true; continue; }
                    List<String> options = Arrays.stream(matcher.group(1).split("/", -1)).map(String::strip).toList();
                    if (options.stream().anyMatch(l -> key(l).equals(key(answer)))) explicit.add(options);
                    else hasBaseHint = true;
                }
            }
            // Consignas that enumerate literal alternatives, e.g. «your / yours».
            if (explicit.isEmpty()) {
                String instructions = Objects.toString(q.getInstructions(), "");
                var matcher = Pattern.compile("[«\"“]([^»\"”]+/[^»\"”]+)[»\"”]").matcher(instructions);
                while (matcher.find()) {
                    List<String> options = Arrays.stream(matcher.group(1).split("/", -1)).map(String::strip).toList();
                    if (options.stream().anyMatch(l -> key(l).equals(key(answer)))) explicit.add(options);
                }
            }
            if (explicit.size() > 1) reason = "La pista nombra más de un conjunto de opciones.";
            else if (explicit.size() == 1) { labels = explicit.get(0); source = "EXERCISE"; }
            else {
                List<List<String>> siblings = new ArrayList<>();
                for (QuizTemplateEntity sibling : BatchBook.list(place.knowledge().getQuizTemplates())) {
                    if (sibling == null || QuizTemplateEntities.formKind(sibling) != FormKind.MULTIPLE_CHOICE
                            || sibling.getForm().getMultipleChoice() == null) continue;
                    var options = BatchBook.list(sibling.getForm().getMultipleChoice().getItems()).stream()
                            .filter(Objects::nonNull).map(MultipleChoiceItemEntity::getLabel).toList();
                    if (options.stream().anyMatch(l -> key(l).equals(key(answer)))) siblings.add(options);
                }
                if (!siblings.isEmpty()) {
                    Set<String> keys = keys(siblings.get(0));
                    if (siblings.stream().anyMatch(l -> !keys(l).equals(keys))) reason = "Los hermanos ofrecen conjuntos distintos; no se adivina cuál usar.";
                    else { labels = siblings.get(0); source = "SIBLINGS"; }
                }
            }
            if (reason == null && labels.isEmpty()) reason = "No trae opciones ni las tienen sus hermanos. Inventar opciones está pendiente de un examen.";
            if (reason == null && !manual && hasBaseHint) reason = "La pista pide producir una forma; sólo se convierte a pedido.";
            if (reason == null && (labels.size() < 2 || labels.size() > 8 || labels.stream().anyMatch(l -> l == null || l.isBlank()))) reason = "Las opciones deben ser entre dos y ocho textos completos.";
            if (reason == null && keys(labels).size() != labels.size()) reason = "La pista repite una opción; corregila antes.";
            if (reason == null && !StoredSentenceRehydrator.rebuild(q).equals(q.getSentences())) reason = "La oración guardada no coincide con la respuesta actual; corregila antes.";
        }
        QuizTemplateEntity after = null;
        if (reason == null) {
            after = BatchBook.copy(q);
            after.setClozeOriginal(BatchBook.copy(q));
            after.setKind("MULTIPLE_CHOICE");
            after.getForm().setKind("MULTIPLE_CHOICE");
            List<MultipleChoiceItemEntity> items = new ArrayList<>();
            for (String label : labels) items.add(new MultipleChoiceItemEntity(label, key(label).equals(key(answer)) ? 1.0 : 0.0, label));
            after.getForm().setMultipleChoice(new MultipleChoiceEntity("SINGLE", items));
            List<SentencePartEntity> clean = new ArrayList<>();
            for (SentencePartEntity part : parts) {
                if (part.getKind() == SentencePartKind.CLOZE) clean.add(new SentencePartEntity(part.getKind(), part.getText(), null));
                else {
                    var matcher = HINT.matcher(Objects.toString(part.getText(), ""));
                    StringBuilder text = new StringBuilder();
                    while (matcher.find()) {
                        List<String> hinted = Arrays.stream(matcher.group(1).split("/", -1)).map(String::strip).toList();
                        matcher.appendReplacement(text, keys(hinted).equals(keys(labels)) ? "" : java.util.regex.Matcher.quoteReplacement(matcher.group()));
                    }
                    matcher.appendTail(text);
                    clean.add(new SentencePartEntity(part.getKind(), text.toString().replaceAll("\\s+", " ").strip(), part.getOptions()));
                }
            }
            after.getForm().setSentenceParts(clean);
            String instructions = q.getInstructions();
            if (instructions == null || instructions.isBlank()) instructions = place.knowledge().getInstructions();
            after.setInstructions(WRITE.matcher(Objects.toString(instructions, "")).replaceAll("Elige"));
        }
        return new Candidate(q.getId(), place.knowledge().getId(), place.knowledge().getLabel(),
                place.milestone().getLabel(), source, BatchBook.list(place.knowledge().getQuizTemplates()).size(),
                (int) BatchBook.list(place.knowledge().getQuizTemplates()).stream().filter(Objects::nonNull)
                        .filter(sibling -> QuizTemplateEntities.formKind(sibling) == FormKind.MULTIPLE_CHOICE).count(), labels, reason, BatchBook.copy(q), after);
    }
    private static String key(String value) { return Objects.toString(value, "").strip().replace('’', '\'').toLowerCase(Locale.ROOT); }
    private static Set<String> keys(List<String> values) { Set<String> keys = new LinkedHashSet<>(); values.forEach(v -> keys.add(key(v))); return keys; }
}
