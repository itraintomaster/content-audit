package com.learney.contentaudit.revisiondomain.batchengine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.revisiondomain.batch.BatchChangeStep;
import com.learney.contentaudit.revisiondomain.panel.PanelCandidateKind;
import com.learney.contentaudit.revisiondomain.panelengine.MesaFixture;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class KeptInTuStepsTest {
    @Test
    @DisplayName("should save in the batch the change as the panel measured it: the old card in vos in tú, with the mini theory among its parts")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R027")
    public void shouldSaveInTheBatchTheChangeAsThePanelMeasuredIt() {
        // F-MESA-R027: the accepted candidate changes the translation of «We reside ___ London…»; its old card in vos
        // reaches the batch in tú, the same exercise the panel measured.
        CourseEntity course = TandaFixture.course();
        QuizTemplateEntity reside = TandaFixture.exercise(course, TandaFixture.RESIDE);
        reside.setMiniTheory("Usá $in$ con ciudades y fijate en la preposición.");

        List<BatchChangeStep> steps = BatchBook.stepsFor(course, MesaFixture.candidate(PanelCandidateKind.WHOLE_EXERCISE,
                MesaFixture.withTranslation(reside, "Vivimos en Londres con el abuelo.")),
                List.of(TandaFixture.RESIDE), TandaFixture.RESIDE_TOPIC);

        assertEquals(1, steps.size());
        QuizTemplateEntity after = steps.get(0).getAfter().getQuiz();
        assertEquals("Usa $in$ con ciudades y fíjate en la preposición.", after.getMiniTheory());
        assertEquals("Vivimos en Londres con el abuelo.", after.getTranslation());
        assertEquals(true, steps.get(0).getParts().containsAll(List.of(ExercisePart.TRANSLATION, ExercisePart.MINI_THEORY)),
                steps.get(0).getParts().toString());
    }
}
