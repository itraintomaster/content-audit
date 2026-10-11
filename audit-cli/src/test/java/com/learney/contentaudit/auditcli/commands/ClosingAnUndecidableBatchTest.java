package com.learney.contentaudit.auditcli.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learney.contentaudit.revisiondomain.batch.BatchClosing;
import com.learney.contentaudit.revisiondomain.batch.BatchService;
import com.learney.contentaudit.revisiondomain.comparison.BatchVerdict;
import com.learney.contentaudit.revisiondomain.comparison.RowSubject;
import com.learney.contentaudit.revisiondomain.comparison.UndecidableFloors;
import com.learney.contentaudit.revisiondomain.comparison.VerdictKind;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/**
 * F-PUBPROD-R005 desde la línea de comandos: al cerrar con «no se puede decidir» la salida no dice que la tanda pasa,
 * dice que no se puede decidir y termina con error, con el cierre hecho. Con pisos sin decidir bajo otro veredicto, los
 * cuenta aparte, sin llamarlo «no se puede decidir», y también termina con error.
 */
public class ClosingAnUndecidableBatchTest {

    private record Run(int exit, String out, String err) {
    }

    /**
     * El servicio de la tanda de {@link BatchCmdTest}, con el veredicto del cierre cambiado por {@code verdict}: la
     * compuerta sigue pasando, como pasa cuando ningún piso se rompe.
     */
    private static BatchService closingWith(BatchCmdTest.Service service, BatchVerdict verdict) {
        return (BatchService) Proxy.newProxyInstance(BatchService.class.getClassLoader(),
                new Class<?>[] {BatchService.class}, (proxy, method, args) -> {
                    Object answer;
                    try {
                        answer = method.invoke(service, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                    if (answer instanceof BatchClosing closing) {
                        closing.getComparison().setVerdict(verdict);
                    }
                    return answer;
                });
    }

    private static Run run(BatchService service, String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
        try {
            int exit = new CommandLine(new BatchCmd(service)).execute(args);
            return new Run(exit, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    private static BatchVerdict verdict(VerdictKind kind, int undecided, String detail) {
        List<RowSubject> floors = new ArrayList<>();
        for (int i = 0; i < undecided; i++) {
            floors.add(new RowSubject(i == 0 ? "lemma-absence" : "hint-coherence",
                    i == 0 ? "Palabras del nivel" : "Pista coherente"));
        }
        return new BatchVerdict(kind, new ArrayList<>(), new ArrayList<>(), null, detail,
                new UndecidableFloors(undecided, floors));
    }

    @Test
    @DisplayName("should say with 'batch close' that it cannot be decided whether the batch worsens the course, never that it passes, and exit with 1 after closing, when the verdict cannot be decided although the gate passes")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R005")
    public void shouldSayWithBatchCloseThatItCannotBeDecidedAndExitWith1WhenTheVerdictCannotBeDecided() {
        // R005 inv. 4: F no puede decidir —un juez sin aprobar dejó un piso sin decidir— y la compuerta de «no
        // empeorar» pasa porque no se rompió nada. La salida no dice que la tanda pasa: dice que no se puede decidir,
        // copia lo que dice F y termina con error; el cierre se hizo igual.
        BatchCmdTest.Service service = new BatchCmdTest.Service();

        Run run = run(closingWith(service, verdict(VerdictKind.UNDECIDABLE, 1,
                "ningún piso falla, pero 1 piso sin decidir; juez sin aprobar: Consigna que no se cumple")), "close");

        assertEquals(1, run.exit(), run.out() + run.err());
        assertEquals(1, service.closeRequests.size(), "el cierre se hizo");
        assertTrue(run.out().contains("No se puede decidir si la tanda empeora el curso: 1 piso sin decidir"),
                run.out());
        assertTrue(run.out().contains("Veredicto de la comparación: no se puede decidir (1 piso sin decidir:"
                + " lemma-absence) — ningún piso falla, pero 1 piso sin decidir; juez sin aprobar: Consigna que no se"
                + " cumple"), run.out());
        assertFalse(run.out().contains("pasa ("), "no dice que la tanda pasa: " + run.out());
        assertFalse(run.out().contains("UNDECIDABLE"), "lo dice con palabras: " + run.out());
        assertFalse(run.out().contains("PASSES"), run.out());
    }

    @Test
    @DisplayName("should not say with 'batch close' that the batch passes, and exit with 1, when the verdict passes but keeps undecidable floors")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R005")
    public void shouldNotSayWithBatchCloseThatTheBatchPassesWhenTheVerdictPassesButKeepsUndecidableFloors() {
        // R005 inv. 2 y 5: «pasa» con pisos sin decidir tampoco es «pasa»: se nombran cuántos y cuáles, y la salida es
        // de error. Pero no se lo llama «no se puede decidir»: eso es sólo el veredicto que no se puede decidir.
        BatchCmdTest.Service service = new BatchCmdTest.Service();

        Run run = run(closingWith(service, verdict(VerdictKind.PASSES, 2, null)), "close");

        assertEquals(1, run.exit(), run.out() + run.err());
        assertEquals(1, service.closeRequests.size());
        assertTrue(run.out().contains("La compuerta de «no empeorar» pasa, pero con 2 pisos sin decidir (1 vuelta)."),
                run.out());
        assertTrue(run.out().contains("Veredicto de la comparación: PASSES, pero con 2 pisos sin decidir:"
                + " lemma-absence, hint-coherence"), run.out());
        assertFalse(run.out().contains("pasa ("), run.out());
        assertFalse(run.out().contains("No se puede decidir"), run.out());
        assertFalse(run.out().contains("no se puede decidir"), run.out());
    }

    @Test
    @DisplayName("should name with 'batch close' a failing verdict that keeps undecidable floors as failing, counting the floors, never as 'no se puede decidir', and exit with 1")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R005")
    public void shouldNameWithBatchCloseAFailingVerdictThatKeepsUndecidableFloorsAsFailing() {
        // R005 inv. 5: F dice «no pasa» —un piso se rompe— y además deja un piso sin decidir. Se sabe que la tanda
        // empeora el curso: la salida lo nombra «FAILS» y cuenta el piso aparte; nunca «no se puede decidir».
        BatchCmdTest.Service service = new BatchCmdTest.Service();

        Run run = run(closingWith(service, verdict(VerdictKind.FAILS, 1, "Falla: vuelve la respuesta única.")),
                "close");

        assertEquals(1, run.exit(), run.out() + run.err());
        assertEquals(1, service.closeRequests.size(), "el cierre se hizo");
        assertTrue(run.out().contains("Veredicto de la comparación: FAILS, y además 1 piso sin decidir: lemma-absence"
                + " — Falla: vuelve la respuesta única."), run.out());
        assertFalse(run.out().contains("No se puede decidir"), run.out());
        assertFalse(run.out().contains("no se puede decidir"), run.out());
    }

    @Test
    @DisplayName("should keep saying with 'batch close' that the gate passes, and exit with 0, when the verdict passes with no undecidable floor")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R005")
    public void shouldKeepSayingWithBatchCloseThatTheGatePassesWhenTheVerdictPassesWithNoUndecidableFloor() {
        // R005: sólo «pasa» sin pisos sin decidir es «pasa»; ese cierre se sigue leyendo como hasta ahora.
        BatchCmdTest.Service service = new BatchCmdTest.Service();

        Run run = run(closingWith(service, verdict(VerdictKind.PASSES, 0, null)), "close");

        assertEquals(0, run.exit(), run.out() + run.err());
        assertTrue(run.out().contains("La compuerta de «no empeorar» pasa (1 vuelta)."), run.out());
        assertFalse(run.out().contains("No se puede decidir"), run.out());
    }
}
