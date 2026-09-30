package com.learney.contentaudit.coursedomain;

import java.util.Objects;

/**
 * What {@link QuizTemplateEntity} cannot carry itself: Sentinel regenerates the model from
 * {@code sentinel.yaml}, where a model is only its fields, so its derived reading and its copy
 * live here, written by hand (FEAT-OPMUL, option A).
 */
public final class QuizTemplateEntities {

    private QuizTemplateEntities() {
    }

    /**
     * The kind of a quiz: its form's, when the form says CLOZE or MULTIPLE_CHOICE, whatever the
     * quiz-level {@code kind} says; otherwise (no form, a form with no kind or with one the system
     * does not recognize) the quiz-level {@code kind} (F-OPMUL-R009). This is how correcting and
     * repairing tell a multiple-choice quiz apart; measuring reads only the form.
     */
    public static FormKind formKind(QuizTemplateEntity quiz) {
        if (quiz == null) {
            return FormKind.OTHER;
        }
        FormKind fromForm = FormEntities.formKind(quiz.getForm());
        return fromForm != FormKind.OTHER ? fromForm : FormKind.from(quiz.getKind());
    }

    /**
     * Shallow copy of every field. Code that needs "the same quiz with one thing changed" copies
     * with this and then sets that one thing, so no field is silently dropped. It goes through the
     * full constructor on purpose: a field added to the model breaks this call at compile time
     * instead of being left out of the copy.
     */
    public static QuizTemplateEntity copyOf(QuizTemplateEntity other) {
        Objects.requireNonNull(other, "quiz to copy");
        return new QuizTemplateEntity(other.getId(), other.getOidId(), other.getKind(),
                other.getKnowledgeId(), other.getTitle(), other.getInstructions(),
                other.getTranslation(), other.getTheoryId(), other.getTopicName(), other.getForm(),
                other.getDifficulty(), other.getRetries(), other.getNoScoreRetries(),
                other.getCode(), other.getAudioUrl(), other.getImageUrl(),
                other.getAnswerAudioUrl(), other.getAnswerImageUrl(), other.getMiniTheory(),
                other.getSuccessMessage(), other.getSentences(), other.getUnmodeledFields());
    }
}
