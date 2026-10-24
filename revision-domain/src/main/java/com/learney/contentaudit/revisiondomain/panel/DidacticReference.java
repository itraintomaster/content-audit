package com.learney.contentaudit.revisiondomain.panel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;

/** Historical teaching intent, separate from the current editable content and from judge findings. */
public record DidacticReference(String sourceOperationId, int sourceContentVersion, String quizId,
        QuizTemplateEntity sourceQuiz, boolean restoreRequestFormula, String operatorInstruction, Provenance provenance) {
    public DidacticReference(String sourceOperationId, int sourceContentVersion, String quizId,
            QuizTemplateEntity sourceQuiz, boolean restoreRequestFormula, String operatorInstruction) {
        this(sourceOperationId, sourceContentVersion, quizId, sourceQuiz, restoreRequestFormula, operatorInstruction, null);
    }
    public DidacticReference(String sourceOperationId, int sourceContentVersion, String quizId, QuizTemplateEntity sourceQuiz) {
        this(sourceOperationId, sourceContentVersion, quizId, sourceQuiz, false, null);
    }
    private static final ObjectMapper JSON = new ObjectMapper();
    public DidacticReference { sourceQuiz = copy(sourceQuiz); }
    @Override public QuizTemplateEntity sourceQuiz() { return copy(sourceQuiz); }
    private static QuizTemplateEntity copy(QuizTemplateEntity quiz) {
        return quiz == null ? null : JSON.convertValue(quiz, QuizTemplateEntity.class);
    }
    /** The only traversed links are explicit predecessor links, ordered from the editable batch to the source. */
    public record Provenance(String sourceBatchId, String sourceOperationFingerprint, String snapshotFingerprint,
            java.util.List<AncestryStep> ancestry) {
        public Provenance { ancestry = ancestry == null ? java.util.List.of() : java.util.List.copyOf(ancestry); }
    }
    public record AncestryStep(String batchId, String parentBatchId, int parentContentVersion, String parentFingerprint) {}
}
