package com.learney.contentaudit.auditcli.commands;

import static org.junit.jupiter.api.Assertions.*;

import com.learney.contentaudit.revisiondomain.changereview.ChangeReview;
import com.learney.contentaudit.revisiondomain.changereview.ChangeReview.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/** FEAT-MARCA: what `batch mark` asks and shows; the service is a fake. */
class ChangeReviewCmdTest {
    static final Result RESULT = new Result("tanda-A1", "0123456789abcdef", "codex-cli / gpt-test / medium", 5, 4, 1,
            0, null, null, List.of(
                    new Row("q-worse", "Respuestas cortas", State.MARKED, new Verdict("q-worse", Mark.WORSE,
                            List.of(new Problem(Where.EXERCISE, "No, I", "Se perdió el «No,».")), "Peor que producción."),
                            "0123456789abcdef", null, "antes", "ahora"),
                    new Row("q-doubt", "Will", State.OLDER_VERSION, new Verdict("q-doubt", Mark.DOUBTFUL,
                            List.of(new Problem(Where.INSTRUCTIONS, "sin plan previo", "Regala el caso.")), "Mejor, con un defecto."),
                            "fedcba9876543210", "Marca de una versión anterior del revisor.", "antes", "ahora"),
                    new Row("q-inherited", "Be: preguntas", State.MARKED, new Verdict("q-inherited", Mark.DOUBTFUL,
                            List.of(new Problem(Where.EXERCISE, "Where is he", "Pregunta con Where.", true)),
                            "Ya estaba así."), "0123456789abcdef", null, "antes", "ahora"),
                    new Row("q-none", "Will", State.UNMARKED, null, null, "Sin marca: falta consultar al revisor.", "antes", "ahora"),
                    new Row("q-good", "Will", State.MARKED, new Verdict("q-good", Mark.GOOD, List.of(), "Bien."),
                            "0123456789abcdef", null, "antes", "ahora")));

    @Test
    @DisplayName("should show what is worse first, then doubtful, then inherited, then without a mark and good last, each problem with its place and fragment and whether production had it, and say that without a budget it does not consult")
    @Tag("FEAT-MARCA")
    @Tag("F-MARCA-R005")
    @Tag("F-MARCA-R010")
    void shouldShowTheMarkedFirstWithTheirProblemsAndSayItDoesNotConsultWithoutABudget() {
        var out = new StringWriter();
        var command = new CommandLine(new ChangeReviewCmd(() -> request -> RESULT));
        command.setOut(new PrintWriter(out));

        assertEquals(0, command.execute("tanda-A1"));

        assertEquals("""
                Revisor de cambios · tanda tanda-A1 · versión 01234567 · codex-cli / gpt-test / medium
                5 ejercicios cambiados: 4 con marca de esta versión, 1 por pedir. Sin --budget no consulto.
                mal (1):
                  q-worse · Respuestas cortas: Peor que producción.
                    - ejercicio «No, I»: Se perdió el «No,».
                dudoso (1):
                  q-doubt · Will (versión anterior fedcba98): Mejor, con un defecto.
                    - consigna «sin plan previo»: Regala el caso.
                heredado (1):
                  q-inherited · Be: preguntas: Ya estaba así.
                    - ejercicio «Where is he»: Pregunta con Where. (ya estaba en producción)
                sin marca (1):
                  q-none · Will: Sin marca: falta consultar al revisor.
                bien (1):
                  q-good · Will
                """, out.toString());
    }

    @Test
    @DisplayName("should pass the batch, the exercises asked for, the budget and how many at once, and answer JSON when asked")
    @Tag("FEAT-MARCA")
    @Tag("F-MARCA-R003")
    void shouldPassTheBatchTheExercisesTheBudgetAndTheConcurrency() {
        var received = new AtomicReference<ChangeReview.Request>();
        var out = new StringWriter();
        var command = new CommandLine(new ChangeReviewCmd(() -> request -> { received.set(request); return RESULT; }));
        command.setOut(new PrintWriter(out));

        assertEquals(0, command.execute("tanda-A1", "--quiz", "q1", "--quiz", "q2", "--budget", "7", "--concurrency", "2",
                "-f", "json"));

        assertEquals(new ChangeReview.Request("tanda-A1", List.of("q1", "q2"), 7, 2), received.get());
        assertTrue(out.toString().replaceAll("\\s", "").contains("\"mark\":\"WORSE\""));
    }
}
