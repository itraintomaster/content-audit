package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.FormEntities;
import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.MilestoneEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceItemEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntities;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.TopicEntity;
import com.learney.contentaudit.refinerdomain.RefinementPlan;
import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.revisiondomain.StoredSentenceRehydrator;
import com.learney.contentaudit.revisiondomain.batch.BatchChange;
import com.learney.contentaudit.revisiondomain.batch.BatchChangeStep;
import com.learney.contentaudit.revisiondomain.batch.BatchTaskOutcome;
import com.learney.contentaudit.revisiondomain.batch.BatchTaskOutcomeKind;
import com.learney.contentaudit.revisiondomain.batch.CorrectionBatch;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * What every piece of the batch reads the same way: the outcome of each task, the exercise and the topic of the course,
 * the node of an analysis, and the fields of an exercise each part of FEAT-TAREA names -- the ledger composes and drops by
 * those fields, so two changes overlap when they touch the same field, not only the same name.
 */
final class BatchBook {

    private BatchBook() {
    }

    // -------------------------------------------------------------------------
    // Lists that may come null from a store
    // -------------------------------------------------------------------------

    static <T> List<T> list(List<T> values) {
        return values != null ? values : new ArrayList<>();
    }

    static <T> List<T> copy(List<T> values) {
        return values != null ? new ArrayList<>(values) : new ArrayList<>();
    }

    /** Missing ordinals in batches written before the iterative UI belong to the first correction. */
    static int correctionRevision(CorrectionBatch batch) {
        return batch != null && batch.getCorrectionRevision() > 0 ? batch.getCorrectionRevision() : 1;
    }

    // -------------------------------------------------------------------------
    // Outcomes
    // -------------------------------------------------------------------------

    static Optional<BatchTaskOutcome> outcome(CorrectionBatch batch, String taskId) {
        for (BatchTaskOutcome outcome : list(batch.getOutcomes())) {
            if (outcome != null && Objects.equals(outcome.getTaskId(), taskId)) {
                return Optional.of(outcome);
            }
        }
        return Optional.empty();
    }

    static BatchTaskOutcomeKind kind(CorrectionBatch batch, String taskId) {
        return outcome(batch, taskId).map(BatchTaskOutcome::getKind).orElse(BatchTaskOutcomeKind.PENDING);
    }

    /** Each task has one outcome at a time: this one replaces the one it had, keeping its attempts when none is given. */
    static void setOutcome(CorrectionBatch batch, String taskId, BatchTaskOutcomeKind kind, String changeId,
            Integer attempts, String detail) {
        List<BatchTaskOutcome> outcomes = copy(batch.getOutcomes());
        int previous = 0;
        for (int i = 0; i < outcomes.size(); i++) {
            BatchTaskOutcome outcome = outcomes.get(i);
            if (outcome != null && Objects.equals(outcome.getTaskId(), taskId)) {
                previous = outcome.getAttempts();
                outcomes.remove(i);
                break;
            }
        }
        outcomes.add(new BatchTaskOutcome(taskId, kind, changeId, attempts != null ? attempts : previous, detail));
        outcomes.sort((a, b) -> taskOrder(batch, a.getTaskId()) - taskOrder(batch, b.getTaskId()));
        batch.setOutcomes(outcomes);
    }

    private static int taskOrder(CorrectionBatch batch, String taskId) {
        int index = list(batch.getTaskIds()).indexOf(taskId);
        return index >= 0 ? index : Integer.MAX_VALUE;
    }

    // -------------------------------------------------------------------------
    // The plan
    // -------------------------------------------------------------------------

    static Map<String, RefinementTask> tasksById(RefinementPlan plan) {
        Map<String, RefinementTask> byId = new LinkedHashMap<>();
        for (RefinementTask task : plan != null ? list(plan.getTasks()) : List.<RefinementTask>of()) {
            if (task != null && task.getId() != null) {
                byId.put(task.getId(), task);
            }
        }
        return byId;
    }

    static Optional<Finding> findingOf(RefinementTask task) {
        return task != null && task.getOrigin() != null ? Optional.ofNullable(task.getOrigin().getFinding())
                : Optional.empty();
    }

    /** The exercises a task is about: its exercise, or the ones its task of the topic reaches. */
    static List<String> quizzesOf(RefinementTask task) {
        if (task == null) {
            return List.of();
        }
        if (task.getNodeTarget() == AuditTarget.QUIZ) {
            return List.of(task.getNodeId());
        }
        return task.getOrigin() != null ? list(task.getOrigin().getReachedQuizIds()) : List.of();
    }

    static String knowledgeOf(RefinementTask task) {
        if (task == null) {
            return null;
        }
        if (task.getOrigin() != null && task.getOrigin().getKnowledgeId() != null) {
            return task.getOrigin().getKnowledgeId();
        }
        return task.getNodeTarget() == AuditTarget.KNOWLEDGE ? task.getNodeId() : null;
    }

    // -------------------------------------------------------------------------
    // The course
    // -------------------------------------------------------------------------

    /** Where an exercise or a topic lives: its level, its topic group, its topic and, for an exercise, itself. */
    record Place(MilestoneEntity milestone, TopicEntity topic, KnowledgeEntity knowledge, QuizTemplateEntity quiz) {
    }

    static Optional<Place> place(CourseEntity course, String nodeId) {
        if (course == null || course.getRoot() == null || nodeId == null) {
            return Optional.empty();
        }
        for (MilestoneEntity milestone : list(course.getRoot().getMilestones())) {
            for (TopicEntity topic : list(milestone.getTopics())) {
                for (KnowledgeEntity knowledge : list(topic.getKnowledges())) {
                    if (nodeId.equals(knowledge.getId())) {
                        return Optional.of(new Place(milestone, topic, knowledge, null));
                    }
                    for (QuizTemplateEntity quiz : list(knowledge.getQuizTemplates())) {
                        if (quiz != null && nodeId.equals(quiz.getId())) {
                            return Optional.of(new Place(milestone, topic, knowledge, quiz));
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    // -------------------------------------------------------------------------
    // The analysis
    // -------------------------------------------------------------------------

    static Optional<AuditNode> node(AuditReport report, String nodeId) {
        if (report == null || report.getRoot() == null || nodeId == null) {
            return Optional.empty();
        }
        List<AuditNode> pending = new ArrayList<>(List.of(report.getRoot()));
        while (!pending.isEmpty()) {
            AuditNode node = pending.remove(pending.size() - 1);
            if (node.getEntity() != null && nodeId.equals(node.getEntity().getId())) {
                return Optional.of(node);
            }
            if (node.getChildren() != null) {
                pending.addAll(node.getChildren());
            }
        }
        return Optional.empty();
    }

    static List<AuditNode> nodes(AuditReport report) {
        List<AuditNode> all = new ArrayList<>();
        if (report == null || report.getRoot() == null) {
            return all;
        }
        List<AuditNode> pending = new ArrayList<>(List.of(report.getRoot()));
        while (!pending.isEmpty()) {
            AuditNode node = pending.remove(0);
            all.add(node);
            if (node.getChildren() != null) {
                pending.addAll(node.getChildren());
            }
        }
        return all;
    }

    static String identity(Finding finding) {
        return finding.getIdentity() != null && finding.getIdentity().getKey() != null ? finding.getIdentity().getKey()
                : finding.getAnalyzer() + "|" + finding.getRule() + "|"
                        + (finding.getNode() != null ? finding.getNode().getNodeId() : "");
    }

    // -------------------------------------------------------------------------
    // The fields of an exercise, by part
    // -------------------------------------------------------------------------

    /** The fields of the entity a part names. */
    enum Field { PARTS, CHOICE, KIND, TRANSLATION, MINI_THEORY, INSTRUCTIONS, SENTENCES, TOPIC_INSTRUCTIONS }

    static Set<Field> fields(List<ExercisePart> parts) {
        Set<Field> fields = EnumSet.noneOf(Field.class);
        for (ExercisePart part : parts != null ? parts : List.<ExercisePart>of()) {
            switch (part) {
                case STATEMENT, HINT, ANSWERS -> {
                    fields.add(Field.PARTS);
                    fields.add(Field.SENTENCES);
                }
                case OPTIONS -> {
                    fields.add(Field.CHOICE);
                    fields.add(Field.SENTENCES);
                }
                case FORM_KIND -> {
                    fields.add(Field.KIND);
                    fields.add(Field.PARTS);
                    fields.add(Field.CHOICE);
                    fields.add(Field.SENTENCES);
                }
                case TRANSLATION -> fields.add(Field.TRANSLATION);
                case MINI_THEORY -> fields.add(Field.MINI_THEORY);
                case INSTRUCTIONS -> fields.add(Field.INSTRUCTIONS);
                case STORED_SENTENCE -> fields.add(Field.SENTENCES);
                case TOPIC_INSTRUCTIONS -> fields.add(Field.TOPIC_INSTRUCTIONS);
                case WHOLE_EXERCISE -> fields.addAll(EnumSet.complementOf(EnumSet.of(Field.TOPIC_INSTRUCTIONS)));
                case AUDIO -> {
                    // what is heard is a request to record, never a field of the exercise
                }
            }
        }
        return fields;
    }

    /** The exercise with the fields of the step taken from its «after». */
    static QuizTemplateEntity apply(QuizTemplateEntity current, QuizTemplateEntity after, Set<Field> fields) {
        QuizTemplateEntity result = copy(current);
        if (after == null) {
            return result;
        }
        if (fields.contains(Field.KIND)) {
            result.setKind(after.getKind());
        }
        if (fields.contains(Field.PARTS) || fields.contains(Field.CHOICE) || fields.contains(Field.KIND)) {
            FormEntity form = result.getForm() != null ? FormEntities.copyOf(result.getForm()) : new FormEntity();
            FormEntity source = after.getForm();
            if (source != null) {
                if (fields.contains(Field.PARTS)) {
                    form.setSentenceParts(copyParts(source.getSentenceParts()));
                }
                if (fields.contains(Field.CHOICE)) {
                    form.setMultipleChoice(copyChoice(source.getMultipleChoice()));
                }
                if (fields.contains(Field.KIND)) {
                    form.setKind(source.getKind());
                }
            }
            result.setForm(form);
        }
        if (fields.contains(Field.TRANSLATION)) {
            result.setTranslation(after.getTranslation());
        }
        if (fields.contains(Field.MINI_THEORY)) {
            result.setMiniTheory(after.getMiniTheory());
        }
        if (fields.contains(Field.INSTRUCTIONS)) {
            result.setInstructions(after.getInstructions());
        }
        boolean sentenceInputsChanged = fields.contains(Field.PARTS) || fields.contains(Field.CHOICE)
                || fields.contains(Field.KIND);
        if (sentenceInputsChanged) {
            result.setSentences(StoredSentenceRehydrator.rebuild(result));
        } else if (fields.contains(Field.SENTENCES)) {
            result.setSentences(after.getSentences() != null ? new ArrayList<>(after.getSentences()) : null);
        }
        return result;
    }

    static QuizTemplateEntity copy(QuizTemplateEntity quiz) {
        if (quiz == null) {
            return null;
        }
        QuizTemplateEntity copy = QuizTemplateEntities.copyOf(quiz);
        if (quiz.getForm() != null) {
            FormEntity form = FormEntities.copyOf(quiz.getForm());
            form.setSentenceParts(copyParts(form.getSentenceParts()));
            form.setMultipleChoice(copyChoice(form.getMultipleChoice()));
            copy.setForm(form);
        }
        if (quiz.getSentences() != null) {
            copy.setSentences(new ArrayList<>(quiz.getSentences()));
        }
        return copy;
    }

    static List<SentencePartEntity> copyParts(List<SentencePartEntity> parts) {
        if (parts == null) {
            return null;
        }
        List<SentencePartEntity> copy = new ArrayList<>();
        for (SentencePartEntity part : parts) {
            copy.add(part == null ? null : new SentencePartEntity(part.getKind(), part.getText(),
                    part.getOptions() != null ? new ArrayList<>(part.getOptions()) : null));
        }
        return copy;
    }

    static MultipleChoiceEntity copyChoice(MultipleChoiceEntity choice) {
        if (choice == null) {
            return null;
        }
        List<MultipleChoiceItemEntity> items = new ArrayList<>();
        for (MultipleChoiceItemEntity item : list(choice.getItems())) {
            items.add(item == null ? null : new MultipleChoiceItemEntity(item.getId(), item.getIncidence(), item.getLabel()));
        }
        return new MultipleChoiceEntity(choice.getSelection(), items);
    }

    /** The steps of the batch on one node, of the changes still in, in the order they were recorded. */
    static List<BatchChangeStep> stepsOn(CorrectionBatch batch, String nodeId) {
        List<BatchChangeStep> steps = new ArrayList<>();
        for (BatchChange change : list(batch.getChanges())) {
            if (change == null || change.getDroppedReason() != null) {
                continue;
            }
            for (BatchChangeStep step : list(change.getSteps())) {
                if (step != null && Objects.equals(step.getQuizId(), nodeId)) {
                    steps.add(step);
                }
            }
        }
        return steps;
    }

    // -------------------------------------------------------------------------
    // What a change touched, and a candidate over the exercise as it was
    // -------------------------------------------------------------------------

    private static final java.util.regex.Pattern HINT = java.util.regex.Pattern.compile("\\([^)]*\\)");

    /** The parts of the exercise that differ, as FEAT-TAREA names them. */
    static List<ExercisePart> changedParts(QuizTemplateEntity before, QuizTemplateEntity after) {
        List<ExercisePart> parts = new ArrayList<>();
        if (before == null || after == null) {
            return parts;
        }
        if (!Objects.equals(statement(before), statement(after))) {
            parts.add(ExercisePart.STATEMENT);
        }
        if (!Objects.equals(hints(before), hints(after))) {
            parts.add(ExercisePart.HINT);
        }
        if (!Objects.equals(answers(before), answers(after))) {
            parts.add(ExercisePart.ANSWERS);
        }
        if (!Objects.equals(options(before), options(after))) {
            parts.add(ExercisePart.OPTIONS);
        }
        if (!Objects.equals(before.getKind(), after.getKind())
                || !Objects.equals(before.getForm() != null ? before.getForm().getKind() : null,
                        after.getForm() != null ? after.getForm().getKind() : null)) {
            parts.add(ExercisePart.FORM_KIND);
        }
        if (!Objects.equals(before.getTranslation(), after.getTranslation())) {
            parts.add(ExercisePart.TRANSLATION);
        }
        if (!Objects.equals(before.getMiniTheory(), after.getMiniTheory())) {
            parts.add(ExercisePart.MINI_THEORY);
        }
        if (!Objects.equals(before.getInstructions(), after.getInstructions())) {
            parts.add(ExercisePart.INSTRUCTIONS);
        }
        if (!Objects.equals(before.getSentences(), after.getSentences())) {
            parts.add(ExercisePart.STORED_SENTENCE);
        }
        return parts;
    }

    private static List<String> statement(QuizTemplateEntity quiz) {
        List<String> texts = new ArrayList<>();
        for (SentencePartEntity part : quiz.getForm() != null ? list(quiz.getForm().getSentenceParts())
                : List.<SentencePartEntity>of()) {
            texts.add(part == null ? null : part.getKind() == com.learney.contentaudit.coursedomain.SentencePartKind.TEXT
                    ? HINT.matcher(part.getText() != null ? part.getText() : "").replaceAll(" ").replaceAll("\\s+", " ")
                            .strip()
                    : "___");
        }
        return texts;
    }

    private static List<String> hints(QuizTemplateEntity quiz) {
        List<String> hints = new ArrayList<>();
        for (SentencePartEntity part : quiz.getForm() != null ? list(quiz.getForm().getSentenceParts())
                : List.<SentencePartEntity>of()) {
            if (part != null && part.getText() != null) {
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
        for (SentencePartEntity part : quiz.getForm() != null ? list(quiz.getForm().getSentenceParts())
                : List.<SentencePartEntity>of()) {
            if (part != null && part.getKind() == com.learney.contentaudit.coursedomain.SentencePartKind.CLOZE) {
                answers.add(part.getOptions() != null ? part.getOptions() : List.of());
            }
        }
        return answers;
    }

    private static List<String> options(QuizTemplateEntity quiz) {
        List<String> options = new ArrayList<>();
        MultipleChoiceEntity choice = quiz.getForm() != null ? quiz.getForm().getMultipleChoice() : null;
        for (MultipleChoiceItemEntity item : choice != null ? list(choice.getItems())
                : List.<MultipleChoiceItemEntity>of()) {
            options.add(item == null ? null : item.getLabel() + (item.getIncidence() > 0.0 ? " *" : ""));
        }
        return options;
    }

    /**
     * The exercise as a candidate leaves it: what the candidate says of its content over the exercise as it was, which
     * keeps the rest. Parts that moved without their stored sentence lose it, so it is built again from the parts.
     */
    static QuizTemplateEntity merged(QuizTemplateEntity old, QuizTemplateEntity candidate) {
        QuizTemplateEntity rewritten = copy(old);
        if (candidate == null || rewritten == null) {
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
        boolean sentenceInputsMoved = !Objects.equals(old.getForm() != null ? old.getForm().getSentenceParts() : null,
                rewritten.getForm() != null ? rewritten.getForm().getSentenceParts() : null)
                || !Objects.equals(old.getForm() != null ? old.getForm().getMultipleChoice() : null,
                        rewritten.getForm() != null ? rewritten.getForm().getMultipleChoice() : null)
                || !Objects.equals(old.getKind(), rewritten.getKind())
                || !Objects.equals(old.getForm() != null ? old.getForm().getKind() : null,
                        rewritten.getForm() != null ? rewritten.getForm().getKind() : null);
        if (sentenceInputsMoved) {
            rewritten.setSentences(StoredSentenceRehydrator.rebuild(rewritten));
        } else if (proposed.getSentences() != null && !Objects.equals(old.getSentences(), proposed.getSentences())) {
            rewritten.setSentences(new ArrayList<>(proposed.getSentences()));
        }
        return rewritten;
    }

    // -------------------------------------------------------------------------
    // An allowance that lets no query start: the rules only reuse what the judges recorded
    // -------------------------------------------------------------------------

    static final class NoQueries implements com.learney.contentaudit.revisiondomain.panel.QueryAllowance {
        private final String why;

        NoQueries(String why) {
            this.why = why;
        }

        @Override
        public boolean tryReserve(int queries) {
            return false;
        }

        @Override
        public void settle(int reserved, int spent) {
        }

        @Override
        public void trip(String cause) {
        }

        @Override
        public Optional<String> tripped() {
            return Optional.of(why);
        }

        @Override
        public int spent() {
            return 0;
        }
    }

    // -------------------------------------------------------------------------
    // A candidate as steps of a change
    // -------------------------------------------------------------------------

    /**
     * A candidate as steps on what it reaches, over the course as the batch leaves it: the new mini theory on each
     * exercise it reaches, the exercise it rewrites with the parts that moved, the instructions of the topic.
     */
    static List<BatchChangeStep> stepsFor(CourseEntity composed,
            com.learney.contentaudit.revisiondomain.panel.PanelCandidate candidate, List<String> quizIds,
            String knowledgeId) {
        List<BatchChangeStep> steps = new ArrayList<>();
        if (candidate == null) {
            return steps;
        }
        if (candidate.getKind() == com.learney.contentaudit.revisiondomain.panel.PanelCandidateKind.MINI_THEORY) {
            for (String quizId : list(quizIds)) {
                Optional<Place> place = place(composed, quizId);
                if (place.isPresent() && place.get().quiz() != null) {
                    QuizTemplateEntity after = copy(place.get().quiz());
                    after.setMiniTheory(candidate.getMiniTheoryText());
                    steps.add(new BatchChangeStep(quizId, new ArrayList<>(List.of(ExercisePart.MINI_THEORY)),
                            new com.learney.contentaudit.revisiondomain.CourseElementSnapshot(AuditTarget.QUIZ, quizId,
                                    after, null)));
                }
            }
        } else if (candidate.getQuiz() != null) {
            for (String quizId : list(quizIds)) {
                Optional<Place> place = place(composed, quizId);
                if (place.isPresent() && place.get().quiz() != null) {
                    QuizTemplateEntity before = place.get().quiz();
                    QuizTemplateEntity after = merged(before, candidate.getQuiz());
                    List<ExercisePart> parts = changedParts(before, after);
                    if (!parts.isEmpty()) {
                        steps.add(new BatchChangeStep(quizId, new ArrayList<>(parts),
                                new com.learney.contentaudit.revisiondomain.CourseElementSnapshot(AuditTarget.QUIZ, quizId,
                                        after, null)));
                    }
                }
            }
        }
        if (candidate.getTopicInstructions() != null && knowledgeId != null) {
            Optional<Place> place = place(composed, knowledgeId);
            if (place.isPresent()) {
                KnowledgeEntity knowledge = place.get().knowledge();
                KnowledgeEntity after = new KnowledgeEntity(knowledge.getId(), knowledge.getCode(), knowledge.getKind(),
                        knowledge.getLabel(), knowledge.getOldId(), knowledge.getParentId(), knowledge.isIsRule(),
                        candidate.getTopicInstructions(), knowledge.getOrder(), knowledge.getSlug(),
                        knowledge.getQuizTemplates(), knowledge.getSentenceMode());
                steps.add(new BatchChangeStep(knowledge.getId(), new ArrayList<>(List.of(ExercisePart.TOPIC_INSTRUCTIONS)),
                        new com.learney.contentaudit.revisiondomain.CourseElementSnapshot(AuditTarget.KNOWLEDGE,
                                knowledge.getId(), null, after)));
            }
        }
        return steps;
    }

    /** An id for a change not taken yet in the batch: the given one, or with a number. */
    static String freeChangeId(CorrectionBatch batch, String wanted) {
        String id = wanted;
        int suffix = 2;
        while (true) {
            String candidate = id;
            if (list(batch.getChanges()).stream().noneMatch(c -> c != null && candidate.equals(c.getChangeId()))) {
                return id;
            }
            id = wanted + "-" + suffix++;
        }
    }
}
