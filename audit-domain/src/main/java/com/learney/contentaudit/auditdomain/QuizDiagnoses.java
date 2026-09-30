package com.learney.contentaudit.auditdomain;

import com.learney.contentaudit.auditdomain.answercompleteness.AnswerCompletenessDiagnosis;
import com.learney.contentaudit.auditdomain.datacoherence.DataCoherenceDiagnosis;
import com.learney.contentaudit.auditdomain.exactduplicates.ExactDuplicateDiagnosis;
import com.learney.contentaudit.auditdomain.hintcoherence.HintCoherenceDiagnosis;
import com.learney.contentaudit.auditdomain.labs.LemmaPlacementDiagnosis;
import com.learney.contentaudit.auditdomain.minitheory.MiniTheoryDiagnosis;
import com.learney.contentaudit.auditdomain.multiplechoicevalidity.MultipleChoiceValidityDiagnosis;
import com.learney.contentaudit.auditdomain.offlevelwords.OffLevelWordsDiagnosis;
import com.learney.contentaudit.auditdomain.quizinstruction.QuizInstructionDiagnosis;
import com.learney.contentaudit.auditdomain.singleanswer.SingleAnswerDiagnosis;
import com.learney.contentaudit.auditdomain.translationsense.TranslationSenseDiagnosis;
import java.util.Optional;

/**
 * The typed place of every analyzer that diagnoses a quiz (DLABS): one slot per analyzer, never
 * a map. FEAT-RONDA1 adds the nine slots of round 1 (hooks A-04, C-05 and D-02); each analyzer
 * writes only its own and a slot nobody wrote stays empty.
 */
public non-sealed interface QuizDiagnoses extends NodeDiagnoses {
    Optional<LemmaPlacementDiagnosis> getLemmaAbsenceDiagnosis();
    Optional<SentenceLengthDiagnosis> getSentenceLengthDiagnosis();
    Optional<QuizInstructionDiagnosis> getQuizInstructionDiagnosis();
    Optional<HintCoherenceDiagnosis> getHintCoherenceDiagnosis();
    Optional<AnswerCompletenessDiagnosis> getAnswerCompletenessDiagnosis();
    Optional<MultipleChoiceValidityDiagnosis> getMultipleChoiceValidityDiagnosis();
    Optional<DataCoherenceDiagnosis> getDataCoherenceDiagnosis();
    Optional<ExactDuplicateDiagnosis> getExactDuplicateDiagnosis();
    Optional<TranslationSenseDiagnosis> getTranslationSenseDiagnosis();
    Optional<SingleAnswerDiagnosis> getSingleAnswerDiagnosis();
    Optional<MiniTheoryDiagnosis> getMiniTheoryDiagnosis();
    Optional<OffLevelWordsDiagnosis> getOffLevelWordsDiagnosis();
}
