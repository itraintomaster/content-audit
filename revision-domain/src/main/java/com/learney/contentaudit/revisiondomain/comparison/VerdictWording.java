package com.learney.contentaudit.revisiondomain.comparison;

import java.util.ArrayList;
import java.util.List;

/**
 * F-PUBPROD-R005: cómo se nombra el veredicto de F en las negativas de publicar y en la salida del cierre, en un solo
 * lugar. «no se puede decidir» es sólo el veredicto que no se puede decidir; los demás se nombran por lo que son, y los
 * pisos sin decidir se dicen aparte: un «FAILS» con pisos sin decidir sigue siendo «FAILS», y además los cuenta; un
 * «PASSES» con pisos sin decidir los cuenta y no publica.
 */
public final class VerdictWording {
    private VerdictWording() {
    }

    /** El nombre del veredicto: «no se puede decidir» sólo cuando no se puede decidir; «sin veredicto» si no hay. */
    public static String name(VerdictKind kind) {
        if (kind == null) {
            return "sin veredicto";
        }
        return kind == VerdictKind.UNDECIDABLE ? "no se puede decidir" : kind.name();
    }

    /** Cuántos pisos dejó sin decidir el veredicto. */
    public static int undecidedFloors(BatchVerdict verdict) {
        return verdict != null && verdict.getUndecidableFloors() != null
                ? Math.max(0, verdict.getUndecidableFloors().getCount()) : 0;
    }

    /** «1 piso sin decidir», «2 pisos sin decidir». */
    public static String floors(int count) {
        return count + (count == 1 ? " piso sin decidir" : " pisos sin decidir");
    }

    /**
     * Cómo sigue una frase que pasó, o que no pasó, cuando además quedan pisos sin decidir: «, pero con 2 pisos sin
     * decidir» después de algo que pasa; «, y además 2 pisos sin decidir» después de algo que no pasa. Vacío si no
     * queda ninguno.
     */
    public static String floorsAfter(boolean passed, int count) {
        if (count <= 0) {
            return "";
        }
        return (passed ? ", pero con " : ", y además ") + floors(count);
    }

    /**
     * El veredicto entre comillas, con sus pisos sin decidir dichos según el veredicto y nombrados por analizador
     * cuando el veredicto los nombra: «no se puede decidir» (1 piso sin decidir: lemma-absence); «FAILS», y además 2
     * pisos sin decidir; «PASSES», pero con 1 piso sin decidir: lemma-absence.
     */
    public static String quoted(BatchVerdict verdict) {
        return "«" + name(kind(verdict)) + "»" + floorsNote(verdict);
    }

    /** Lo mismo, sin comillas: para la línea del veredicto de la comparación. */
    public static String plain(BatchVerdict verdict) {
        return name(kind(verdict)) + floorsNote(verdict);
    }

    /** Si el veredicto deja publicar: «pasa» y ningún piso sin decidir. */
    public static boolean publishable(BatchVerdict verdict) {
        return kind(verdict) == VerdictKind.PASSES && undecidedFloors(verdict) == 0;
    }

    private static String floorsNote(BatchVerdict verdict) {
        int count = undecidedFloors(verdict);
        if (count == 0) {
            return "";
        }
        List<String> named = named(verdict.getUndecidableFloors());
        String names = named.isEmpty() ? "" : ": " + String.join(", ", named);
        VerdictKind kind = kind(verdict);
        if (kind == VerdictKind.FAILS || kind == VerdictKind.PASSES) {
            return floorsAfter(kind == VerdictKind.PASSES, count) + names;
        }
        return " (" + floors(count) + names + ")";
    }

    /** Los analizadores de los pisos sin decidir, sin repetir, cuando el veredicto los nombra. */
    private static List<String> named(UndecidableFloors floors) {
        List<String> named = new ArrayList<>();
        if (floors.getAnalyzers() == null) {
            return named;
        }
        for (RowSubject subject : floors.getAnalyzers()) {
            if (subject != null && subject.getSubject() != null && !subject.getSubject().isBlank()
                    && !named.contains(subject.getSubject())) {
                named.add(subject.getSubject());
            }
        }
        return named;
    }

    private static VerdictKind kind(BatchVerdict verdict) {
        return verdict != null ? verdict.getKind() : null;
    }
}
