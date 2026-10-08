package com.learney.contentaudit.revisiondomain.panelengine;

import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.coursedomain.FormEntities;
import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.FormKind;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceItemEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntities;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.refinerdomain.errortask.ErrorTaskBrief;
import com.learney.contentaudit.revisiondomain.StoredSentenceRehydrator;
import com.learney.contentaudit.revisiondomain.panel.PanelCandidate;
import com.learney.contentaudit.revisiondomain.panel.PanelCandidateKind;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The topic a panel works on, as the batch leaves it: copied so nothing the panel tries reaches the course
 * (F-MESA-R009), with each accepted candidate applied so the later ones see it (F-MESA-R001 inv. 3), and the readings of
 * an exercise the screen and the script compare -- which parts a candidate changes and what the student hears.
 */
final class TopicState {

    private static final Pattern HINT = Pattern.compile("\\([^)]*\\)");

    private TopicState() {
    }

    // -------------------------------------------------------------------------
    // Copies
    // -------------------------------------------------------------------------

    /** The topic with its own list of exercises, each one a copy with its own parts and options. */
    static KnowledgeEntity copy(KnowledgeEntity topic) {
        List<QuizTemplateEntity> quizzes = new ArrayList<>();
        for (QuizTemplateEntity quiz : topic.getQuizTemplates() != null ? topic.getQuizTemplates()
                : List.<QuizTemplateEntity>of()) {
            quizzes.add(quiz == null ? null : copy(quiz));
        }
        return new KnowledgeEntity(topic.getId(), topic.getCode(), topic.getKind(), topic.getLabel(), topic.getOldId(),
                topic.getParentId(), topic.isIsRule(), topic.getInstructions(), topic.getOrder(), topic.getSlug(),
                quizzes, topic.getSentenceMode());
    }

    static QuizTemplateEntity copy(QuizTemplateEntity quiz) {
        QuizTemplateEntity copy = QuizTemplateEntities.copyOf(quiz);
        if (quiz.getForm() != null) {
            FormEntity form = FormEntities.copyOf(quiz.getForm());
            if (form.getSentenceParts() != null) {
                List<SentencePartEntity> parts = new ArrayList<>();
                for (SentencePartEntity part : form.getSentenceParts()) {
                    parts.add(part == null ? null : new SentencePartEntity(part.getKind(), part.getText(),
                            part.getOptions() != null ? new ArrayList<>(part.getOptions()) : null));
                }
                form.setSentenceParts(parts);
            }
            if (form.getMultipleChoice() != null) {
                List<MultipleChoiceItemEntity> items = new ArrayList<>();
                for (MultipleChoiceItemEntity item : form.getMultipleChoice().getItems() != null
                        ? form.getMultipleChoice().getItems() : List.<MultipleChoiceItemEntity>of()) {
                    items.add(item == null ? null
                            : new MultipleChoiceItemEntity(item.getId(), item.getIncidence(), item.getLabel()));
                }
                form.setMultipleChoice(new MultipleChoiceEntity(form.getMultipleChoice().getSelection(), items));
            }
            copy.setForm(form);
        }
        if (quiz.getSentences() != null) {
            copy.setSentences(new ArrayList<>(quiz.getSentences()));
        }
        return copy;
    }

    static Optional<QuizTemplateEntity> quiz(KnowledgeEntity topic, String quizId) {
        if (topic == null || topic.getQuizTemplates() == null || quizId == null) {
            return Optional.empty();
        }
        for (QuizTemplateEntity quiz : topic.getQuizTemplates()) {
            if (quiz != null && quizId.equals(quiz.getId())) {
                return Optional.of(quiz);
            }
        }
        return Optional.empty();
    }

    // -------------------------------------------------------------------------
    // Applying a candidate
    // -------------------------------------------------------------------------

    /**
     * The topic with the candidate applied: the exercise it rewrites in its place, the new mini theory only in its
     * explicit quiz (F-MESA-R012), the new instructions of the topic (F-MESA-R015). A rewritten exercise whose parts
     * moved and whose stored sentence did not loses it, so the measurement builds it again from the parts.
     */
    static KnowledgeEntity apply(KnowledgeEntity topic, String quizId, PanelCandidate candidate,
            List<ErrorTaskBrief> briefs) {
        KnowledgeEntity applied = copy(topic);
        if (candidate == null) {
            return applied;
        }
        if (candidate.getKind() == PanelCandidateKind.MINI_THEORY) {
            for (QuizTemplateEntity quiz : applied.getQuizTemplates()) {
                if (quiz != null && Objects.equals(quiz.getId(), quizId)) {
                    quiz.setMiniTheory(candidate.getMiniTheoryText());
                }
            }
        } else if (candidate.getQuiz() != null) {
            String target = quizId != null ? quizId : candidate.getQuiz().getId();
            List<QuizTemplateEntity> quizzes = applied.getQuizTemplates();
            for (int i = 0; i < quizzes.size(); i++) {
                QuizTemplateEntity old = quizzes.get(i);
                if (old != null && Objects.equals(old.getId(), target)) {
                    quizzes.set(i, merged(old, candidate.getQuiz()));
                }
            }
        }
        if (candidate.getTopicInstructions() != null) {
            applied.setInstructions(candidate.getTopicInstructions());
        }
        return applied;
    }

    /**
     * The exercise as a candidate leaves it: what the candidate says of its content -- the form, the kind, the
     * translation, the instructions, the mini theory, the stored sentence -- over the exercise as it was, which keeps
     * everything else (its ids, its audio key, what the system does not model). A candidate whose parts moved and whose
     * stored sentence did not loses it, so the measurement builds it again from the parts.
     */
    static QuizTemplateEntity merged(QuizTemplateEntity old, QuizTemplateEntity candidate) {
        QuizTemplateEntity rewritten = copy(old);
        if (candidate == null) {
            return rewritten;
        }
        QuizTemplateEntity proposed = copy(candidate);
        if (proposed.getForm() != null) {
            rewritten.setForm(proposed.getForm());
        }
        if (proposed.getKind() != null) {
            rewritten.setKind(proposed.getKind());
        }
        if (proposed.getTranslation() != null) {
            rewritten.setTranslation(proposed.getTranslation());
        }
        if (proposed.getInstructions() != null) {
            rewritten.setInstructions(proposed.getInstructions());
        }
        if (proposed.getMiniTheory() != null) {
            rewritten.setMiniTheory(proposed.getMiniTheory());
        }
        boolean sentenceInputsMoved = !Objects.equals(partsOf(old), partsOf(rewritten))
                || isMultipleChoice(old) != isMultipleChoice(rewritten)
                || !Objects.equals(correctOption(old), correctOption(rewritten));
        if (sentenceInputsMoved) {
            rewritten.setSentences(StoredSentenceRehydrator.rebuild(rewritten));
        } else if (proposed.getSentences() != null) {
            rewritten.setSentences(new ArrayList<>(proposed.getSentences()));
        } else {
            rewritten.setSentences(old.getSentences() != null ? new ArrayList<>(old.getSentences()) : null);
        }
        return rewritten;
    }

    // -------------------------------------------------------------------------
    // Readings
    // -------------------------------------------------------------------------

    /** The exercise as the analyzers read it, without tokens: what the student view renders. */
    static AuditableQuiz auditable(QuizTemplateEntity quiz) {
        FormEntity form = quiz.getForm();
        return new AuditableQuiz(List.of(), quiz.getId(), quiz.getTitle(), quiz.getCode(), quiz.getTranslation(),
                quiz.getSentences(), null, quiz.getInstructions(), form != null ? form.getSentenceParts() : null,
                form != null ? form.getMultipleChoice() : null, quiz.getMiniTheory(), quiz.getAnswerAudioUrl());
    }

    /** What the student hears: the statement without hints, with the first answer or the correct option, compacted. */
    static String heard(QuizTemplateEntity quiz) {
        if (quiz == null || quiz.getForm() == null || quiz.getForm().getSentenceParts() == null) {
            return "";
        }
        String correct = correctOption(quiz).orElse(null);
        StringBuilder sb = new StringBuilder();
        for (SentencePartEntity part : quiz.getForm().getSentenceParts()) {
            if (part == null) {
                continue;
            }
            if (part.getKind() == SentencePartKind.CLOZE) {
                sb.append(' ').append(correct != null ? correct : firstAnswer(part));
            } else if (part.getText() != null) {
                sb.append(' ').append(HINT.matcher(part.getText()).replaceAll(" "));
            }
        }
        return sb.toString().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9' ]", " ").replaceAll("\\s+", " ").strip();
    }

    static String firstAnswer(SentencePartEntity part) {
        if (part.getOptions() == null || part.getOptions().isEmpty() || part.getOptions().get(0) == null) {
            return "";
        }
        return part.getOptions().get(0).split("[|/]", -1)[0].strip();
    }

    /** The first accepted answer of the first gap of a write-in exercise. */
    static String firstAnswer(QuizTemplateEntity quiz) {
        for (SentencePartEntity part : partsOf(quiz)) {
            if (part != null && part.getKind() == SentencePartKind.CLOZE && part.getOptions() != null
                    && !part.getOptions().isEmpty()) {
                return firstAnswer(part);
            }
        }
        return "";
    }

    static Optional<String> correctOption(QuizTemplateEntity quiz) {
        MultipleChoiceEntity choice = quiz.getForm() != null ? quiz.getForm().getMultipleChoice() : null;
        if (choice == null || choice.getItems() == null) {
            return Optional.empty();
        }
        return choice.correctItem().map(MultipleChoiceItemEntity::getLabel);
    }

    static boolean isMultipleChoice(QuizTemplateEntity quiz) {
        return quiz != null && (QuizTemplateEntities.formKind(quiz) == FormKind.MULTIPLE_CHOICE
                || quiz.getForm() != null && quiz.getForm().getMultipleChoice() != null);
    }

    static List<SentencePartEntity> partsOf(QuizTemplateEntity quiz) {
        return quiz != null && quiz.getForm() != null && quiz.getForm().getSentenceParts() != null
                ? quiz.getForm().getSentenceParts() : List.of();
    }

    /**
     * The parts of the exercise a candidate changes, as FEAT-TAREA names them: the statement (its text without hints),
     * the hint, the accepted answers, the options, the kind of form, the translation, the mini theory, the instructions,
     * the stored sentence and what is heard.
     */
    static Set<ExercisePart> changedParts(QuizTemplateEntity before, QuizTemplateEntity after) {
        Set<ExercisePart> changed = EnumSet.noneOf(ExercisePart.class);
        if (!Objects.equals(statementText(before), statementText(after))) {
            changed.add(ExercisePart.STATEMENT);
        }
        if (!Objects.equals(hints(before), hints(after))) {
            changed.add(ExercisePart.HINT);
        }
        if (!Objects.equals(answers(before), answers(after))) {
            changed.add(ExercisePart.ANSWERS);
        }
        if (!Objects.equals(options(before), options(after))) {
            changed.add(ExercisePart.OPTIONS);
        }
        if (isMultipleChoice(before) != isMultipleChoice(after)
                || !Objects.equals(before.getKind(), after.getKind())) {
            changed.add(ExercisePart.FORM_KIND);
        }
        if (!Objects.equals(before.getTranslation(), after.getTranslation())) {
            changed.add(ExercisePart.TRANSLATION);
        }
        if (!Objects.equals(before.getMiniTheory(), after.getMiniTheory())) {
            changed.add(ExercisePart.MINI_THEORY);
        }
        if (!Objects.equals(before.getInstructions(), after.getInstructions())) {
            changed.add(ExercisePart.INSTRUCTIONS);
        }
        if (after.getSentences() != null && !Objects.equals(before.getSentences(), after.getSentences())) {
            changed.add(ExercisePart.STORED_SENTENCE);
        }
        if (!Objects.equals(heard(before), heard(after))
                || !Objects.equals(before.getAnswerAudioUrl(), after.getAnswerAudioUrl())) {
            changed.add(ExercisePart.AUDIO);
        }
        return changed;
    }

    private static List<String> statementText(QuizTemplateEntity quiz) {
        List<String> texts = new ArrayList<>();
        for (SentencePartEntity part : partsOf(quiz)) {
            if (part != null && part.getKind() == SentencePartKind.TEXT) {
                texts.add(HINT.matcher(part.getText() != null ? part.getText() : "").replaceAll(" ")
                        .replaceAll("\\s+", " ").strip());
            } else if (part != null) {
                texts.add("___");
            }
        }
        return texts;
    }

    private static List<String> hints(QuizTemplateEntity quiz) {
        List<String> hints = new ArrayList<>();
        for (SentencePartEntity part : partsOf(quiz)) {
            if (part != null && part.getKind() == SentencePartKind.TEXT && part.getText() != null) {
                java.util.regex.Matcher m = HINT.matcher(part.getText());
                while (m.find()) {
                    hints.add(m.group());
                }
            }
        }
        return hints;
    }

    private static List<List<String>> answers(QuizTemplateEntity quiz) {
        List<List<String>> answers = new ArrayList<>();
        for (SentencePartEntity part : partsOf(quiz)) {
            if (part != null && part.getKind() == SentencePartKind.CLOZE) {
                answers.add(part.getOptions() != null ? part.getOptions() : List.of());
            }
        }
        return answers;
    }

    private static List<String> options(QuizTemplateEntity quiz) {
        List<String> options = new ArrayList<>();
        MultipleChoiceEntity choice = quiz.getForm() != null ? quiz.getForm().getMultipleChoice() : null;
        if (choice != null && choice.getItems() != null) {
            for (MultipleChoiceItemEntity item : choice.getItems()) {
                options.add(item == null ? null : item.getLabel() + (item.getIncidence() > 0.0 ? " *" : ""));
            }
        }
        return options;
    }
}
