package com.learney.contentaudit.revisiondomain.panelengine;

import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.SentenceMode;
import com.learney.contentaudit.revisiondomain.SentenceToTransform;
import java.util.List;

/**
 * F-MESA-R019: an exercise of a rewrite topic stays one. When its starting point gives the sentence to transform before
 * the gap and the student writes the whole answer in it, the candidate does the same: «That house belongs to Mary. ___»
 * may not become «That is ___ (Mary) house.», where the student no longer transforms anything.
 */
final class RewritePreservation {

    private RewritePreservation() {
    }

    static List<String> violations(QuizTemplateEntity startingPoint, QuizTemplateEntity proposed, SentenceMode mode) {
        if (mode != SentenceMode.REWRITE || !transforms(startingPoint) || transforms(proposed)) {
            return List.of();
        }
        String given = "deja de ser de transformar: el punto de partida da «" + SentenceToTransform.source(startingPoint)
                + "» y el alumno escribe la oración entera transformada; ";
        return List.of(given + (!SentenceToTransform.sentenceBeforeTheGap(proposed)
                ? "el candidato no da una oración de partida y deja un hueco para completar"
                : "el candidato deja texto fijo alrededor del hueco y el alumno ya no escribe la oración entera"));
    }

    /** It gives a sentence before the gap, and from there on there are only gaps and marks: the answer is whole. */
    static boolean transforms(QuizTemplateEntity quiz) {
        return SentenceToTransform.given(quiz);
    }
}
