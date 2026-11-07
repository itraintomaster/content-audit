package com.learney.contentaudit.revisiondomain.batchengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.coursedomain.FormEntities;
import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.FormKind;
import com.learney.contentaudit.coursedomain.MultipleChoiceEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceItemEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.revisiondomain.panel.PanelCandidate;
import java.util.ArrayList;
import java.util.List;

/**
 * F-BANDEJA-R003: el arreglo que escribe José se arma sobre el ejercicio como está en la tanda. Su línea dice las partes
 * del enunciado y las respuestas de cada hueco; la clase del formulario, su incidencia, sus nombres y el texto de cada
 * hueco quedan como los tiene el ejercicio, y un hueco nuevo nace como sus hermanos (F-RPRES-R006). Un ejercicio de
 * opción múltiple conserva sus opciones, salvo que José escriba las suyas: lo que José escribe en el hueco dice cuál de
 * ellas es la correcta.
 */
final class JosesFix {

    private static final ObjectMapper JSON = new ObjectMapper();

    private JosesFix() {
    }

    /**
     * El arreglo sobre {@code exercise}. En uno de opción múltiple, las opciones son las que José escribió o, si no
     * escribió ninguna, las del ejercicio, y la respuesta escrita tiene que ser una de ellas.
     *
     * @throws IllegalArgumentException si la respuesta del arreglo de un ejercicio de opción múltiple no es una de sus
     *         opciones, o si el arreglo trae opciones y el ejercicio no es de opción múltiple
     */
    static PanelCandidate restedOn(PanelCandidate fix, QuizTemplateEntity exercise) {
        if (fix == null || fix.getQuiz() == null || fix.getQuiz().getForm() == null || exercise == null
                || exercise.getForm() == null) {
            return fix;
        }
        boolean choice = FormEntities.formKind(exercise.getForm()) == FormKind.MULTIPLE_CHOICE;
        List<String> offered = offered(fix.getQuiz().getForm().getMultipleChoice());
        if (!choice && offered != null) {
            throw new IllegalArgumentException("El ejercicio no es de opción múltiple: el arreglo no puede traer opciones («"
                    + String.join("», «", offered) + "»).");
        }
        List<SentencePartEntity> base = exercise.getForm().getSentenceParts() != null
                ? exercise.getForm().getSentenceParts() : List.of();
        List<SentencePartEntity> parts = new ArrayList<>();
        List<String> answers = new ArrayList<>();
        List<SentencePartEntity> written = fix.getQuiz().getForm().getSentenceParts() != null
                ? fix.getQuiz().getForm().getSentenceParts() : List.of();
        for (int i = 0; i < written.size(); i++) {
            SentencePartEntity part = written.get(i);
            if (part == null || part.getKind() != SentencePartKind.CLOZE) {
                parts.add(part);
                continue;
            }
            if (part.getOptions() != null) {
                answers.addAll(part.getOptions());
            }
            parts.add(new SentencePartEntity(SentencePartKind.CLOZE, blankText(i, base),
                    choice ? blankOptions(i, base) : part.getOptions()));
        }
        FormEntity form = FormEntities.copyOf(exercise.getForm());
        form.setSentenceParts(parts);
        if (choice) {
            form.setMultipleChoice(marked(exercise.getForm().getMultipleChoice(), offered, answers));
        }
        QuizTemplateEntity quiz = JSON.convertValue(fix.getQuiz(), QuizTemplateEntity.class);
        quiz.setForm(form);
        return new PanelCandidate(fix.getKind(), quiz, fix.getMiniTheoryText(), fix.getTopicInstructions(),
                fix.getRationale());
    }

    /**
     * Las opciones del arreglo, en su orden y con sus nombres -- las de José si escribió alguna, si no las del ejercicio --:
     * la que José escribió en el hueco, correcta; las demás, no. Una opción que el ejercicio ya tenía conserva su id.
     */
    private static MultipleChoiceEntity marked(MultipleChoiceEntity options, List<String> offered, List<String> answers) {
        List<MultipleChoiceItemEntity> own = options != null && options.getItems() != null ? options.getItems()
                : List.of();
        List<MultipleChoiceItemEntity> items = offered == null ? own : offered.stream()
                .map(label -> own.stream().filter(item -> item != null && label.equals(item.getLabel())).findFirst()
                        .orElse(new MultipleChoiceItemEntity(label, 0.0, label))).toList();
        String answer = answers.size() == 1 ? answers.get(0).strip() : null;
        if (answer == null || items.stream().noneMatch(item -> item != null && answer.equals(item.getLabel()))) {
            throw new IllegalArgumentException("El ejercicio es de opción múltiple: entre corchetes va cuál de sus opciones"
                    + " es la correcta (" + String.join(" · ", items.stream().filter(java.util.Objects::nonNull)
                            .map(MultipleChoiceItemEntity::getLabel).toList()) + "), y el arreglo dice "
                    + (answers.isEmpty() ? "ninguna" : "«" + String.join("», «", answers) + "»") + ".");
        }
        List<MultipleChoiceItemEntity> marked = new ArrayList<>();
        for (MultipleChoiceItemEntity item : items) {
            marked.add(item == null ? null : new MultipleChoiceItemEntity(item.getId(),
                    answer.equals(item.getLabel()) ? 1.0 : 0.0, item.getLabel()));
        }
        return new MultipleChoiceEntity(options != null ? options.getSelection() : null, marked);
    }

    /** Las opciones que José escribió en su arreglo, o null si no escribió ninguna. */
    private static List<String> offered(MultipleChoiceEntity written) {
        List<String> labels = written == null || written.getItems() == null ? List.of() : written.getItems().stream()
                .filter(item -> item != null && item.getLabel() != null && !item.getLabel().isBlank())
                .map(item -> item.getLabel().strip()).toList();
        return labels.isEmpty() ? null : labels;
    }

    /** El texto del hueco en la misma posición del ejercicio; si ahí no hay hueco, el de su primer hueco; si no, vacío. */
    private static String blankText(int index, List<SentencePartEntity> base) {
        if (index < base.size() && base.get(index) != null && base.get(index).getKind() == SentencePartKind.CLOZE
                && base.get(index).getText() != null) {
            return base.get(index).getText();
        }
        return base.stream().filter(p -> p != null && p.getKind() == SentencePartKind.CLOZE && p.getText() != null)
                .map(SentencePartEntity::getText).findFirst().orElse("");
    }

    /** Las respuestas del hueco en la misma posición del ejercicio; si ahí no hay hueco, las de su primer hueco. */
    private static List<String> blankOptions(int index, List<SentencePartEntity> base) {
        if (index < base.size() && base.get(index) != null && base.get(index).getKind() == SentencePartKind.CLOZE) {
            return base.get(index).getOptions();
        }
        return base.stream().filter(p -> p != null && p.getKind() == SentencePartKind.CLOZE).findFirst()
                .map(SentencePartEntity::getOptions).orElse(null);
    }
}
