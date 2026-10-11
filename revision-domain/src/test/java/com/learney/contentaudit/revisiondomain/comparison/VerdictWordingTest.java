package com.learney.contentaudit.revisiondomain.comparison;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * F-PUBPROD-R005 inv. 5: «no se puede decidir» es sólo el veredicto que no se puede decidir; los pisos sin decidir de
 * otro veredicto se cuentan aparte, con el nombre de cada uno cuando el veredicto lo sabe.
 */
public class VerdictWordingTest {

    private static BatchVerdict verdict(VerdictKind kind, int undecided, String... analyzers) {
        List<RowSubject> named = new ArrayList<>();
        for (String analyzer : analyzers) {
            named.add(new RowSubject(analyzer, "Piso de " + analyzer));
        }
        return new BatchVerdict(kind, new ArrayList<>(), new ArrayList<>(), null, null,
                new UndecidableFloors(undecided, named));
    }

    @Test
    @DisplayName("should call 'no se puede decidir' only the verdict that cannot be decided, with its undecidable floors in parentheses")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R005")
    public void shouldCallNoSePuedeDecidirOnlyTheVerdictThatCannotBeDecided() {
        BatchVerdict undecidable = verdict(VerdictKind.UNDECIDABLE, 1, "lemma-absence");

        assertEquals("«no se puede decidir» (1 piso sin decidir: lemma-absence)", VerdictWording.quoted(undecidable));
        assertEquals("no se puede decidir (1 piso sin decidir: lemma-absence)", VerdictWording.plain(undecidable));
        assertEquals("«no se puede decidir»", VerdictWording.quoted(verdict(VerdictKind.UNDECIDABLE, 0)));
        assertFalse(VerdictWording.publishable(undecidable));
    }

    @Test
    @DisplayName("should name a failing verdict with undecidable floors as failing and count the floors besides, never as 'no se puede decidir'")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R005")
    public void shouldNameAFailingVerdictWithUndecidableFloorsAsFailingAndCountTheFloorsBesides() {
        BatchVerdict failing = verdict(VerdictKind.FAILS, 2, "lemma-absence", "hint-coherence");

        assertEquals("«FAILS», y además 2 pisos sin decidir: lemma-absence, hint-coherence",
                VerdictWording.quoted(failing));
        assertEquals("FAILS, y además 2 pisos sin decidir: lemma-absence, hint-coherence",
                VerdictWording.plain(failing));
        assertFalse(VerdictWording.quoted(failing).contains("no se puede decidir"));
        assertEquals("«FAILS»", VerdictWording.quoted(verdict(VerdictKind.FAILS, 0)));
        assertFalse(VerdictWording.publishable(failing));
    }

    @Test
    @DisplayName("should name a passing verdict with undecidable floors as passing but with the floors, and not let it publish")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R005")
    public void shouldNameAPassingVerdictWithUndecidableFloorsAsPassingButWithTheFloorsAndNotLetItPublish() {
        BatchVerdict passingWithFloor = verdict(VerdictKind.PASSES, 1, "lemma-absence");
        BatchVerdict passingWithUnnamedFloors = verdict(VerdictKind.PASSES, 3);

        assertEquals("«PASSES», pero con 1 piso sin decidir: lemma-absence", VerdictWording.quoted(passingWithFloor));
        assertEquals("PASSES, pero con 3 pisos sin decidir", VerdictWording.plain(passingWithUnnamedFloors));
        assertFalse(VerdictWording.publishable(passingWithFloor), "con un piso sin decidir no publica");
        assertFalse(VerdictWording.publishable(passingWithUnnamedFloors));
        assertTrue(VerdictWording.publishable(verdict(VerdictKind.PASSES, 0)), "sólo «pasa» sin pisos publica");
        assertEquals("«PASSES»", VerdictWording.quoted(verdict(VerdictKind.PASSES, 0)));
        assertEquals("«sin veredicto»", VerdictWording.quoted(null));
        assertFalse(VerdictWording.publishable(null));
    }

    @Test
    @DisplayName("should continue a phrase that passed with 'pero con' and one that did not pass with 'y además', and say nothing with no floor")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R005")
    public void shouldContinueAPhraseThatPassedWithPeroConAndOneThatDidNotWithYAdemas() {
        assertEquals(", pero con 2 pisos sin decidir", VerdictWording.floorsAfter(true, 2));
        assertEquals(", y además 1 piso sin decidir", VerdictWording.floorsAfter(false, 1));
        assertEquals("", VerdictWording.floorsAfter(true, 0));
    }
}
