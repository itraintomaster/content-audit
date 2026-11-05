package com.learney.contentaudit.revisiondomain.batchengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.coursedomain.FormEntities;
import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.FormKind;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.revisiondomain.panel.PanelCandidate;
import java.util.ArrayList;
import java.util.List;

/**
 * F-BANDEJA-R003: el arreglo que escribe José se arma sobre el ejercicio como está en la tanda. Su línea dice las partes
 * del enunciado y las respuestas de cada hueco; la clase del formulario, su incidencia, sus nombres y el texto de cada
 * hueco quedan como los tiene el ejercicio, y un hueco nuevo nace como sus hermanos (F-RPRES-R006).
 */
final class JosesFix {

    private static final ObjectMapper JSON = new ObjectMapper();

    private JosesFix() {
    }

    /**
     * El arreglo sobre {@code exercise}. Un ejercicio de opción múltiple no tiene notación del curso: el arreglo queda como
     * vino.
     */
    static PanelCandidate restedOn(PanelCandidate fix, QuizTemplateEntity exercise) {
        if (fix == null || fix.getQuiz() == null || fix.getQuiz().getForm() == null || exercise == null
                || exercise.getForm() == null || FormEntities.formKind(exercise.getForm()) == FormKind.MULTIPLE_CHOICE) {
            return fix;
        }
        List<SentencePartEntity> base = exercise.getForm().getSentenceParts() != null
                ? exercise.getForm().getSentenceParts() : List.of();
        List<SentencePartEntity> parts = new ArrayList<>();
        List<SentencePartEntity> written = fix.getQuiz().getForm().getSentenceParts() != null
                ? fix.getQuiz().getForm().getSentenceParts() : List.of();
        for (int i = 0; i < written.size(); i++) {
            SentencePartEntity part = written.get(i);
            parts.add(part != null && part.getKind() == SentencePartKind.CLOZE
                    ? new SentencePartEntity(SentencePartKind.CLOZE, blankText(i, base), part.getOptions()) : part);
        }
        FormEntity form = FormEntities.copyOf(exercise.getForm());
        form.setSentenceParts(parts);
        QuizTemplateEntity quiz = JSON.convertValue(fix.getQuiz(), QuizTemplateEntity.class);
        quiz.setForm(form);
        return new PanelCandidate(fix.getKind(), quiz, fix.getMiniTheoryText(), fix.getTopicInstructions(),
                fix.getRationale());
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
}
