package com.learney.contentaudit.revisiondomain.panelengine;

import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.SentenceMode;
import com.learney.contentaudit.revisiondomain.panel.DidacticReference;
import java.util.List;

/** The deterministic part of the historical contract; its remaining teaching context is supplied to the proposer. */
public final class DidacticContracts {
    private DidacticContracts() {}
    public static String taskId(String operationId, String quizId) { return "didactic-contract-" + operationId + "-" + quizId; }
    public static DidacticReference reference(String sourceOperationId, int version, String quizId,
            QuizTemplateEntity source, boolean restoreRequestFormula) {
        String modal = restoreRequestFormula && source != null ? RequestFormPreservation.requestModal(source) : null;
        String directive = modal == null ? null : "Usá exactamente: " + modal
                + " + sujeto + verbo base + complemento + ?. No agregues please ni palabras opcionales.";
        return new DidacticReference(sourceOperationId, version, quizId, source, restoreRequestFormula, directive);
    }
    public static List<String> violations(DidacticReference reference, QuizTemplateEntity proposed,
            String instructions, SentenceMode mode) {
        if (reference == null) return List.of();
        if (reference.sourceQuiz() == null || proposed == null) return List.of("falta el ejercicio para contrastar el contrato didáctico");
        var result = new java.util.ArrayList<>(RequestFormPreservation.verify(reference.sourceQuiz(), proposed, instructions, mode));
        String directive = reference.operatorInstruction();
        if (directive != null) result.addAll(RequestFormPreservation.closedFormulaViolations(proposed,
                RequestFormPreservation.requestModal(reference.sourceQuiz()), directive, mode));
        return List.copyOf(result);
    }
}
