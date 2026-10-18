package com.learney.contentaudit.auditdomain;

import com.learney.contentaudit.auditdomain.answercompleteness.AnswerCompletenessDiagnosis;
import com.learney.contentaudit.auditdomain.datacoherence.DataCoherenceDiagnosis;
import com.learney.contentaudit.auditdomain.exactduplicates.ExactDuplicateDiagnosis;
import com.learney.contentaudit.auditdomain.grammar.GrammarDiagnosis;
import com.learney.contentaudit.auditdomain.hintcoherence.HintCoherenceDiagnosis;
import com.learney.contentaudit.auditdomain.labs.LemmaPlacementDiagnosis;
import com.learney.contentaudit.auditdomain.minitheory.MiniTheoryDiagnosis;
import com.learney.contentaudit.auditdomain.multiplechoicevalidity.MultipleChoiceValidityDiagnosis;
import com.learney.contentaudit.auditdomain.naturalness.NaturalnessDiagnosis;
import com.learney.contentaudit.auditdomain.nearduplicates.NearDuplicateDiagnosis;
import com.learney.contentaudit.auditdomain.offlevelwords.OffLevelWordsDiagnosis;
import com.learney.contentaudit.auditdomain.quizinstruction.QuizInstructionDiagnosis;
import com.learney.contentaudit.auditdomain.singleanswer.SingleAnswerDiagnosis;
import com.learney.contentaudit.auditdomain.translationsense.TranslationSenseDiagnosis;
import java.util.Optional;

public final class DefaultQuizDiagnoses implements QuizDiagnoses {
    private com.learney.contentaudit.auditdomain.pragmatism.PragmatismDiagnosis pragmatismDiagnosis;

    @Override
    public Optional<com.learney.contentaudit.auditdomain.pragmatism.PragmatismDiagnosis> getPragmatismDiagnosis() {
        return Optional.ofNullable(pragmatismDiagnosis);
    }

    public void setPragmatismDiagnosis(com.learney.contentaudit.auditdomain.pragmatism.PragmatismDiagnosis diagnosis) {
        this.pragmatismDiagnosis = diagnosis;
    }

    private com.learney.contentaudit.auditdomain.choicequality.ChoiceQualityDiagnosis choiceQualityDiagnosis;

    @Override
    public Optional<com.learney.contentaudit.auditdomain.choicequality.ChoiceQualityDiagnosis> getChoiceQualityDiagnosis() {
        return Optional.ofNullable(choiceQualityDiagnosis);
    }

    public void setChoiceQualityDiagnosis(com.learney.contentaudit.auditdomain.choicequality.ChoiceQualityDiagnosis diagnosis) {
        this.choiceQualityDiagnosis = diagnosis;
    }

    private LemmaPlacementDiagnosis lemmaAbsenceDiagnosis;

    private SentenceLengthDiagnosis sentenceLengthDiagnosis;

    private QuizInstructionDiagnosis quizInstructionDiagnosis;

    // FEAT-RONDA1: the slots of round 1, one per analyzer (hooks A-04, C-05, D-02).

    private HintCoherenceDiagnosis hintCoherenceDiagnosis;

    private AnswerCompletenessDiagnosis answerCompletenessDiagnosis;

    private MultipleChoiceValidityDiagnosis multipleChoiceValidityDiagnosis;

    private DataCoherenceDiagnosis dataCoherenceDiagnosis;

    private ExactDuplicateDiagnosis exactDuplicateDiagnosis;

    private TranslationSenseDiagnosis translationSenseDiagnosis;

    private SingleAnswerDiagnosis singleAnswerDiagnosis;

    private MiniTheoryDiagnosis miniTheoryDiagnosis;

    private OffLevelWordsDiagnosis offLevelWordsDiagnosis;

    // FEAT-RONDA2: the slots of package B, one per analyzer (hook B-02).

    private GrammarDiagnosis grammarDiagnosis;

    private NaturalnessDiagnosis naturalnessDiagnosis;

    private NearDuplicateDiagnosis nearDuplicateDiagnosis;

    @Override
    public Optional<LemmaPlacementDiagnosis> getLemmaAbsenceDiagnosis() {
        return Optional.ofNullable(lemmaAbsenceDiagnosis);
    }

    public void setLemmaAbsenceDiagnosis(LemmaPlacementDiagnosis diagnosis) {
        this.lemmaAbsenceDiagnosis = diagnosis;
    }

    @Override
    public Optional<SentenceLengthDiagnosis> getSentenceLengthDiagnosis() {
        return Optional.ofNullable(sentenceLengthDiagnosis);
    }

    public void setSentenceLengthDiagnosis(SentenceLengthDiagnosis diagnosis) {
        this.sentenceLengthDiagnosis = diagnosis;
    }

    @Override
    public Optional<QuizInstructionDiagnosis> getQuizInstructionDiagnosis() {
        return Optional.ofNullable(quizInstructionDiagnosis);
    }

    public void setQuizInstructionDiagnosis(QuizInstructionDiagnosis diagnosis) {
        this.quizInstructionDiagnosis = diagnosis;
    }

    @Override
    public Optional<HintCoherenceDiagnosis> getHintCoherenceDiagnosis() {
        return Optional.ofNullable(hintCoherenceDiagnosis);
    }

    public void setHintCoherenceDiagnosis(HintCoherenceDiagnosis diagnosis) {
        this.hintCoherenceDiagnosis = diagnosis;
    }

    @Override
    public Optional<AnswerCompletenessDiagnosis> getAnswerCompletenessDiagnosis() {
        return Optional.ofNullable(answerCompletenessDiagnosis);
    }

    public void setAnswerCompletenessDiagnosis(AnswerCompletenessDiagnosis diagnosis) {
        this.answerCompletenessDiagnosis = diagnosis;
    }

    @Override
    public Optional<MultipleChoiceValidityDiagnosis> getMultipleChoiceValidityDiagnosis() {
        return Optional.ofNullable(multipleChoiceValidityDiagnosis);
    }

    public void setMultipleChoiceValidityDiagnosis(MultipleChoiceValidityDiagnosis diagnosis) {
        this.multipleChoiceValidityDiagnosis = diagnosis;
    }

    @Override
    public Optional<DataCoherenceDiagnosis> getDataCoherenceDiagnosis() {
        return Optional.ofNullable(dataCoherenceDiagnosis);
    }

    public void setDataCoherenceDiagnosis(DataCoherenceDiagnosis diagnosis) {
        this.dataCoherenceDiagnosis = diagnosis;
    }

    @Override
    public Optional<ExactDuplicateDiagnosis> getExactDuplicateDiagnosis() {
        return Optional.ofNullable(exactDuplicateDiagnosis);
    }

    public void setExactDuplicateDiagnosis(ExactDuplicateDiagnosis diagnosis) {
        this.exactDuplicateDiagnosis = diagnosis;
    }

    @Override
    public Optional<TranslationSenseDiagnosis> getTranslationSenseDiagnosis() {
        return Optional.ofNullable(translationSenseDiagnosis);
    }

    public void setTranslationSenseDiagnosis(TranslationSenseDiagnosis diagnosis) {
        this.translationSenseDiagnosis = diagnosis;
    }

    @Override
    public Optional<SingleAnswerDiagnosis> getSingleAnswerDiagnosis() {
        return Optional.ofNullable(singleAnswerDiagnosis);
    }

    public void setSingleAnswerDiagnosis(SingleAnswerDiagnosis diagnosis) {
        this.singleAnswerDiagnosis = diagnosis;
    }

    @Override
    public Optional<MiniTheoryDiagnosis> getMiniTheoryDiagnosis() {
        return Optional.ofNullable(miniTheoryDiagnosis);
    }

    public void setMiniTheoryDiagnosis(MiniTheoryDiagnosis diagnosis) {
        this.miniTheoryDiagnosis = diagnosis;
    }

    @Override
    public Optional<OffLevelWordsDiagnosis> getOffLevelWordsDiagnosis() {
        return Optional.ofNullable(offLevelWordsDiagnosis);
    }

    public void setOffLevelWordsDiagnosis(OffLevelWordsDiagnosis diagnosis) {
        this.offLevelWordsDiagnosis = diagnosis;
    }

    @Override
    public Optional<GrammarDiagnosis> getGrammarDiagnosis() {
        return Optional.ofNullable(grammarDiagnosis);
    }

    public void setGrammarDiagnosis(GrammarDiagnosis diagnosis) {
        this.grammarDiagnosis = diagnosis;
    }

    @Override
    public Optional<NaturalnessDiagnosis> getNaturalnessDiagnosis() {
        return Optional.ofNullable(naturalnessDiagnosis);
    }

    public void setNaturalnessDiagnosis(NaturalnessDiagnosis diagnosis) {
        this.naturalnessDiagnosis = diagnosis;
    }

    @Override
    public Optional<NearDuplicateDiagnosis> getNearDuplicateDiagnosis() {
        return Optional.ofNullable(nearDuplicateDiagnosis);
    }

    public void setNearDuplicateDiagnosis(NearDuplicateDiagnosis diagnosis) {
        this.nearDuplicateDiagnosis = diagnosis;
    }
}
