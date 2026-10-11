package com.learney.contentaudit.auditcli.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learney.contentaudit.auditdomain.studentview.QuizStudentView;
import com.learney.contentaudit.auditdomain.studentview.StudentGapView;
import com.learney.contentaudit.auditdomain.studentview.StudentOptionView;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.revisiondomain.batch.BatchPublication;
import com.learney.contentaudit.revisiondomain.blindsample.BlindPairView;
import com.learney.contentaudit.revisiondomain.blindsample.StudentSide;
import com.learney.contentaudit.revisiondomain.tray.Tray;
import com.learney.contentaudit.revisiondomain.tray.TrayAnswer;
import com.learney.contentaudit.revisiondomain.tray.TrayItem;
import com.learney.contentaudit.revisiondomain.tray.TrayItemKind;
import com.learney.contentaudit.revisiondomain.tray.TrayResponse;
import com.learney.contentaudit.revisiondomain.tray.TrayService;
import com.learney.contentaudit.revisiondomain.tray.TraySamplePair;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/**
 * F-BANDEJA-R001 on the text of {@code tray show}: every exercise of the tray as the student sees it. The control of
 * package E over the copy of 29/9 showed why: the rules of A1 change the answers a gap takes, the options or the hint,
 * not the sentence, and a tray that printed only the sentence showed José 100 pairs that looked the same.
 */
class TrayCmdStudentViewIntegrationTest {

    @Test
    @DisplayName("should show in the sample of a rule the answers each gap takes, the options and the instructions, before and after")
    @Tag("FEAT-BANDEJA")
    @Tag("F-BANDEJA-R001")
    void shouldShowTheWholeExerciseBeforeAndAfterInTheSampleOfARule() {
        QuizStudentView before = new QuizStudentView("I ___ (be) happy today.",
                List.of(new StudentGapView(0, "(be)", List.of("am"), "am")), List.of(),
                "Completa con el verbo 'be'.", "Hoy estoy feliz.", "Con $I$ el be es $am$.");
        QuizStudentView after = new QuizStudentView("I ___ (be) happy today.",
                List.of(new StudentGapView(0, "(be)", List.of("am", "'m"), "am")), List.of(),
                "Completa con el verbo 'be'.", "Hoy estoy feliz.", "Con $I$ el be es $am$.");
        QuizStudentView options = new QuizStudentView("___ he a boy?", List.of(),
                List.of(new StudentOptionView("Is", true), new StudentOptionView("Are", false)),
                "Elige el comienzo correcto.", "¿Él es un niño?", null);
        TrayItem sample = new TrayItem("muestra-answer-completeness", TrayItemKind.RULE_SAMPLE, null, null, null,
                "La regla answer-completeness cambió 2 ejercicios; mirá todos: ¿está bien?",
                List.of(TrayAnswer.RULE_OK, TrayAnswer.DROP_CHANGE, TrayAnswer.DROP_RULE), null, null, List.of(),
                List.of(), "answer-completeness", List.of(
                        new TraySamplePair("regla-answer-completeness-q1", "q1", before, after),
                        new TraySamplePair("regla-answer-completeness-q2", "q2", options, options)),
                null, List.of(), null);

        String out = show(new Tray("tanda-A1-20261001T120000Z", false, List.of(sample), false, List.of()));

        assertTrue(out.contains("   regla-answer-completeness-q1\n"
                + "     antes: I ___ (be) happy today. · Hoy estoy feliz.\n"
                + "            consigna: Completa con el verbo 'be'.\n"
                + "            hueco 1: am\n"
                + "            miniteoría: Con $I$ el be es $am$.\n"
                + "     después: I ___ (be) happy today. · Hoy estoy feliz.\n"
                + "              consigna: Completa con el verbo 'be'.\n"
                + "              hueco 1: am | 'm\n"
                + "              miniteoría: Con $I$ el be es $am$.\n"), out);
        assertTrue(out.contains("            opciones: Is · Are\n"), "the options of a multiple choice:\n" + out);
    }

    @Test
    @DisplayName("should show both sides of a pair of the blind sample as the student sees them, without the answers each gap takes")
    @Tag("FEAT-BANDEJA")
    @Tag("F-BANDEJA-R001")
    @Tag("F-BANDEJA-R005")
    void shouldShowBothSidesOfABlindPairAsTheStudentSeesThem() {
        // Pair 9 of the control: the rule of hint-coherence changed the hint, not the sentence after the gap.
        StudentSide a = new StudentSide(new QuizStudentView("___ (I / am) below you?",
                List.of(new StudentGapView(0, "(I / am)", List.of("Am I"), "Am I")), List.of(),
                "Formula preguntas de sí/no en 'present continuous'.", "¿Estoy debajo de ti?",
                "Con $I$ el be es $am$: $Am I$ ...?"), "Am I below you?");
        StudentSide b = new StudentSide(new QuizStudentView("___ below you?",
                List.of(new StudentGapView(0, null, List.of("Am I"), "Am I")), List.of(),
                "Formula preguntas de sí/no en 'present continuous'.", "¿Estoy debajo de ti?",
                "Con $I$ el be es $am$: $Am I$ ...?"), "Am I below you?");
        TrayItem blind = new TrayItem("ciega-muestra-1", TrayItemKind.BLIND_SAMPLE, null, null, null,
                "¿Cuál de los dos está mejor? Respondé cada par: A, B, iguales o los dos mal.",
                List.of(TrayAnswer.BLIND_PAIR), null, null, List.of(), List.of(), null, List.of(), "muestra-1",
                List.of(new BlindPairView(9, a, b)), null);

        String out = show(new Tray("tanda-A1-20261001T120000Z", true, List.of(blind), false,
                List.of("falta la muestra ciega: respondé sus 100 pares; se cierra con el último")));

        assertTrue(out.contains("   par 9\n"
                + "     A · ___ (I / am) below you? · ¿Estoy debajo de ti?\n"
                + "         consigna: Formula preguntas de sí/no en 'present continuous'.\n"
                + "         miniteoría: Con $I$ el be es $am$: $Am I$ ...?\n"
                + "         respondido: Am I below you?\n"
                + "     B · ___ below you? · ¿Estoy debajo de ti?\n"
                + "         consigna: Formula preguntas de sí/no en 'present continuous'.\n"
                + "         miniteoría: Con $I$ el be es $am$: $Am I$ ...?\n"
                + "         respondido: Am I below you?\n"), out);
        assertFalse(out.contains("hueco"), "the blind sample shows no answer key:\n" + out);
    }

    private static String show(Tray tray) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        try {
            int exit = new CommandLine(new TrayCmd(new Fixed(tray), DefaultQuizSentenceConverter.create(), null))
                    .execute("show");
            assertEquals(0, exit);
        } finally {
            System.setOut(original);
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    /** A tray that only shows. */
    private record Fixed(Tray tray) implements TrayService {
        @Override
        public Tray view() {
            return tray;
        }

        @Override
        public Tray answer(TrayResponse response) {
            throw new UnsupportedOperationException("only shown");
        }

        @Override
        public BatchPublication publish(String approvedBy, String note) {
            throw new UnsupportedOperationException("only shown");
        }
    }
}
