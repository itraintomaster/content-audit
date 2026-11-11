package com.learney.contentaudit.revisiondomain.batchengine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.revisiondomain.StoredSentenceRehydrator;
import com.learney.contentaudit.revisiondomain.batch.BatchChangeStep;
import com.learney.contentaudit.revisiondomain.panel.PanelCandidateKind;
import com.learney.contentaudit.revisiondomain.panelengine.MesaFixture;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * F-TAREA-R006: lo que un arreglo no declara queda como estaba, también al armar la tanda. Los casos salen de publicar
 * la C2 de A1 en una copia (11/11/2026).
 */
@Tag("FEAT-TAREA")
@Tag("F-TAREA-R006")
class WhatAStepDoesNotDeclareTest {

    /** «It ___ (be) soon.» with the answers of ...215c and its two stored sentences, as production keeps them. */
    private static QuizTemplateEntity rain(String hint) {
        QuizTemplateEntity quiz = new QuizTemplateEntity();
        quiz.setId("67fab6d5993010229534215c");
        quiz.setKind("CLOZE");
        quiz.setForm(new FormEntity("CLOZE", 1, "", "", new ArrayList<>(List.of(
                new SentencePartEntity(SentencePartKind.TEXT, "It", null),
                new SentencePartEntity(SentencePartKind.CLOZE, "", List.of("'s going to rain|is going to rain")),
                new SentencePartEntity(SentencePartKind.TEXT, hint + " soon.", null))), null, Map.of()));
        quiz.setSentences(new ArrayList<>(List.of("It's going to rain soon.", "It is going to rain soon.")));
        return quiz;
    }

    @Test
    @DisplayName("should keep the stored sentence when a fix changes only the hint")
    void shouldKeepTheStoredSentenceWhenAFixChangesOnlyTheHint() {
        // inv. 1: la regla de la pista pasó «(be)» a «(be / rain)» en ...215c y el armado de la tanda volvió a armar la
        // oración guardada: quedó «It 's going to rain soon.», sin la forma larga. La pista no es parte de la oración.
        QuizTemplateEntity current = rain("(be)");
        QuizTemplateEntity after = rain("(be / rain)");
        after.setSentences(new ArrayList<>(List.of("It 's going to rain soon.")));

        QuizTemplateEntity composed = BatchBook.apply(current, after, BatchBook.fields(List.of(ExercisePart.HINT)));

        assertEquals("(be / rain) soon.", composed.getForm().getSentenceParts().get(2).getText());
        assertEquals(List.of("It's going to rain soon.", "It is going to rain soon."), composed.getSentences());
    }

    @Test
    @DisplayName("should build the stored sentence again when a fix changes the answers, with its contraction joined")
    void shouldBuildTheStoredSentenceAgainWhenAFixChangesTheAnswersWithItsContractionJoined() {
        // inv. 4: con otra respuesta la oración se arma de nuevo, y «'s» va pegado a la palabra de antes: 78 ejercicios
        // de la C2 tenían «I 'll stay here.», «They 're late for the game.».
        QuizTemplateEntity current = rain("(be)");
        QuizTemplateEntity after = rain("(be)");
        after.getForm().getSentenceParts().set(1,
                new SentencePartEntity(SentencePartKind.CLOZE, "", List.of("'s going to snow|is going to snow")));

        QuizTemplateEntity composed = BatchBook.apply(current, after, BatchBook.fields(List.of(ExercisePart.ANSWERS)));

        assertEquals(List.of("It's going to snow soon."), composed.getSentences());
        assertEquals(List.of("I'll stay here."), StoredSentenceRehydrator.rebuild(withParts(
                text("I"), cloze("'ll"), text("(will) stay here."))));
        assertEquals(List.of("We’re not ready."), StoredSentenceRehydrator.rebuild(withParts(
                text("We"), cloze("’re not"), text("ready."))));
        assertEquals(List.of("You don't swim."), StoredSentenceRehydrator.rebuild(withParts(
                text("You don"), cloze("'t"), text("swim."))));
        assertEquals(List.of("He said 'hello' to me."), StoredSentenceRehydrator.rebuild(withParts(
                text("He said"), cloze("'hello'"), text("to me."))), "una comilla no es una contracción");
    }

    @Test
    @DisplayName("should keep the stored sentence with its other forms when a new answer leaves it saying the same")
    void shouldKeepTheStoredSentenceWithItsOtherFormsWhenANewAnswerLeavesItSayingTheSame() {
        // inv. 4: agregar una respuesta que no es la primera no cambia lo que dice la oración: quedan sus dos formas
        // (la C2 dejaba sólo «It 's going to rain soon.»). Una oración que producción guarda con la contracción
        // aparte, «I 'm great.», tampoco se reescribe: dice lo mismo, y el arreglo no la declara.
        QuizTemplateEntity current = rain("(be)");
        QuizTemplateEntity after = rain("(be)");
        after.getForm().getSentenceParts().set(1, new SentencePartEntity(SentencePartKind.CLOZE, "",
                List.of("'s going to rain|is going to rain|'s gonna rain")));
        QuizTemplateEntity great = withParts(text("I"), cloze("'m"), text("(be) great."));
        great.setSentences(new ArrayList<>(List.of("I 'm great.")));
        QuizTemplateEntity greater = withParts(text("I"), cloze("'m|am"), text("(be) great."));

        assertEquals(List.of("It's going to rain soon.", "It is going to rain soon."), BatchBook.apply(current, after,
                BatchBook.fields(List.of(ExercisePart.ANSWERS))).getSentences());
        assertEquals(List.of("I 'm great."), BatchBook.apply(great, greater,
                BatchBook.fields(List.of(ExercisePart.ANSWERS))).getSentences());
    }

    @Test
    @DisplayName("should keep the text of the statement as it was when the panel sends it back with spaces at its edges")
    void shouldKeepTheTextOfTheStatementAsItWasWhenThePanelSendsItBackWithSpacesAtItsEdges() {
        // inv. 3: la mesa agregó «Today it's hot.» a ...241d y devolvió el enunciado como «(hot / today) »: el paso
        // declaraba sólo las respuestas, pero guardaba también ese espacio. Lo que no declara queda como estaba.
        CourseEntity course = TandaFixture.course();
        QuizTemplateEntity vegetables = TandaFixture.exercise(course, MesaFixture.VEGETABLES);
        QuizTemplateEntity candidate = BatchBook.copy(vegetables);
        candidate.getForm().setSentenceParts(new ArrayList<>(List.of(text("I "), cloze("love|adore"),
                text(" a lot of vegetables. "))));

        List<BatchChangeStep> steps = BatchBook.stepsFor(course, MesaFixture.candidate(PanelCandidateKind.WHOLE_EXERCISE,
                candidate), List.of(MesaFixture.VEGETABLES), null);

        assertEquals(1, steps.size());
        assertEquals(List.of(ExercisePart.ANSWERS), steps.get(0).getParts());
        assertEquals(vegetables.getForm().getSentenceParts().get(0).getText(),
                steps.get(0).getAfter().getQuiz().getForm().getSentenceParts().get(0).getText());
        assertEquals(vegetables.getForm().getSentenceParts().get(2).getText(),
                steps.get(0).getAfter().getQuiz().getForm().getSentenceParts().get(2).getText());
        assertEquals(List.of("love|adore"), steps.get(0).getAfter().getQuiz().getForm().getSentenceParts().get(1)
                .getOptions());
    }

    @Test
    @DisplayName("should take the new text of the statement without spaces at its edges")
    void shouldTakeTheNewTextOfTheStatementWithoutSpacesAtItsEdges() {
        // inv. 3: un enunciado que sí cambia entra como lo guarda producción, sin espacios en los bordes.
        CourseEntity course = TandaFixture.course();
        QuizTemplateEntity vegetables = TandaFixture.exercise(course, MesaFixture.VEGETABLES);
        QuizTemplateEntity candidate = BatchBook.copy(vegetables);
        candidate.getForm().setSentenceParts(new ArrayList<>(List.of(text("I "), cloze("love"),
                text(" vegetables. "))));

        List<BatchChangeStep> steps = BatchBook.stepsFor(course, MesaFixture.candidate(PanelCandidateKind.WHOLE_EXERCISE,
                candidate), List.of(MesaFixture.VEGETABLES), null);

        assertEquals(1, steps.size());
        assertEquals(List.of("I", "", "vegetables."), steps.get(0).getAfter().getQuiz().getForm().getSentenceParts()
                .stream().map(SentencePartEntity::getText).toList());
    }

    private static SentencePartEntity text(String text) {
        return new SentencePartEntity(SentencePartKind.TEXT, text, null);
    }

    private static SentencePartEntity cloze(String... answers) {
        return new SentencePartEntity(SentencePartKind.CLOZE, "", List.of(answers));
    }

    private static QuizTemplateEntity withParts(SentencePartEntity... parts) {
        QuizTemplateEntity quiz = new QuizTemplateEntity();
        quiz.setId("q");
        quiz.setKind("CLOZE");
        quiz.setForm(new FormEntity("CLOZE", 1, "", "", new ArrayList<>(List.of(parts)), null, Map.of()));
        return quiz;
    }
}
