package com.learney.contentaudit.revisiondomain.preservationengine;

import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.MilestoneEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.RootNodeEntity;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.TopicEntity;
import com.learney.contentaudit.revisiondomain.preservation.AttributeState;
import com.learney.contentaudit.revisiondomain.preservation.PreservationViolation;
import com.learney.contentaudit.revisiondomain.preservation.ScopedField;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Shared complement-based field comparator for {@code QuizTemplateEntity}/{@code KnowledgeEntity}
 * leaves, used by both {@link DefaultPreservationCheck} (F-RPRES-R001/R002/R003/R004) and
 * {@link DefaultPreservationRepair} (F-RPRES-R005). Both share this exact comparator on purpose
 * (see architect decision on FEAT-RPRES): the check and the repair are the same rule applied
 * forward (guard before persisting) and backward (restore what a past revision dropped).
 *
 * <p>A field is a "violation" whenever it differs between {@code before} and {@code after} and
 * its path/elementKind is not declared changeable by the {@link ScopedField} set passed in — the
 * complement of the correction's declared scope (F-RPRES-R001).
 */
final class CourseElementFieldDiff {

    private CourseElementFieldDiff() {
    }

    static AttributeState classify(Object value) {
        if (value == null) {
            return AttributeState.ABSENT;
        }
        if (value instanceof String s) {
            return s.isEmpty() ? AttributeState.EMPTY_PRESENT : AttributeState.VALUE_PRESENT;
        }
        if (value instanceof Double d) {
            return d == 0.0 ? AttributeState.ABSENT : AttributeState.VALUE_PRESENT;
        }
        if (value instanceof Integer i) {
            return i == 0 ? AttributeState.ABSENT : AttributeState.VALUE_PRESENT;
        }
        if (value instanceof Boolean b) {
            return b ? AttributeState.VALUE_PRESENT : AttributeState.ABSENT;
        }
        if (value instanceof List<?> l) {
            return l.isEmpty() ? AttributeState.EMPTY_PRESENT : AttributeState.VALUE_PRESENT;
        }
        return AttributeState.VALUE_PRESENT;
    }

    private static String display(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static boolean isExempt(String path, String elementKind, Set<ScopedField> scope) {
        for (ScopedField field : scope) {
            if (!field.getPath().equals(path)) {
                continue;
            }
            if (field.getElementKind() == null) {
                return true;
            }
            if (elementKind != null && field.getElementKind().equals(elementKind)) {
                return true;
            }
        }
        return false;
    }

    private static void diffLeaf(String elementId, String scopePath, String reportPath, String elementKind,
            Object before, Object after, Set<ScopedField> scope, List<PreservationViolation> out) {
        if (isExempt(scopePath, elementKind, scope)) {
            return;
        }
        if (Objects.equals(before, after)) {
            return;
        }
        out.add(new PreservationViolation(elementId, reportPath, classify(before), classify(after),
                display(before), display(after)));
    }

    private static void diffLeaf(String elementId, String path, Object before, Object after,
            Set<ScopedField> scope, List<PreservationViolation> out) {
        diffLeaf(elementId, path, path, null, before, after, scope, out);
    }

    static void diffQuiz(QuizTemplateEntity before, QuizTemplateEntity after, Set<ScopedField> scope,
            List<PreservationViolation> out) {
        String id = before.getId();
        diffLeaf(id, "oidId", before.getOidId(), after.getOidId(), scope, out);
        diffLeaf(id, "kind", before.getKind(), after.getKind(), scope, out);
        diffLeaf(id, "knowledgeId", before.getKnowledgeId(), after.getKnowledgeId(), scope, out);
        diffLeaf(id, "title", before.getTitle(), after.getTitle(), scope, out);
        diffLeaf(id, "instructions", before.getInstructions(), after.getInstructions(), scope, out);
        diffLeaf(id, "translation", before.getTranslation(), after.getTranslation(), scope, out);
        diffLeaf(id, "theoryId", before.getTheoryId(), after.getTheoryId(), scope, out);
        diffLeaf(id, "topicName", before.getTopicName(), after.getTopicName(), scope, out);
        diffLeaf(id, "difficulty", before.getDifficulty(), after.getDifficulty(), scope, out);
        diffLeaf(id, "retries", before.getRetries(), after.getRetries(), scope, out);
        diffLeaf(id, "noScoreRetries", before.getNoScoreRetries(), after.getNoScoreRetries(), scope, out);
        diffLeaf(id, "code", before.getCode(), after.getCode(), scope, out);
        diffLeaf(id, "audioUrl", before.getAudioUrl(), after.getAudioUrl(), scope, out);
        diffLeaf(id, "imageUrl", before.getImageUrl(), after.getImageUrl(), scope, out);
        diffLeaf(id, "answerAudioUrl", before.getAnswerAudioUrl(), after.getAnswerAudioUrl(), scope, out);
        diffLeaf(id, "answerImageUrl", before.getAnswerImageUrl(), after.getAnswerImageUrl(), scope, out);
        diffLeaf(id, "miniTheory", before.getMiniTheory(), after.getMiniTheory(), scope, out);
        diffLeaf(id, "successMessage", before.getSuccessMessage(), after.getSuccessMessage(), scope, out);
        diffLeaf(id, "sentences", before.getSentences(), after.getSentences(), scope, out);

        FormEntity formBefore = before.getForm();
        FormEntity formAfter = after.getForm();
        if (formBefore == null && formAfter == null) {
            return;
        }
        if (formBefore == null || formAfter == null) {
            diffLeaf(id, "form", formBefore, formAfter, scope, out);
            return;
        }
        diffLeaf(id, "form.kind", formBefore.getKind(), formAfter.getKind(), scope, out);
        diffLeaf(id, "form.incidence", formBefore.getIncidence(), formAfter.getIncidence(), scope, out);
        diffLeaf(id, "form.label", formBefore.getLabel(), formAfter.getLabel(), scope, out);
        diffLeaf(id, "form.name", formBefore.getName(), formAfter.getName(), scope, out);
        diffSentenceParts(id, formBefore.getSentenceParts(), formAfter.getSentenceParts(), scope, out);
    }

    private static void diffSentenceParts(String elementId, List<SentencePartEntity> before,
            List<SentencePartEntity> after, Set<ScopedField> scope, List<PreservationViolation> out) {
        List<SentencePartEntity> b = before == null ? Collections.emptyList() : before;
        List<SentencePartEntity> a = after == null ? Collections.emptyList() : after;
        boolean structureExempt = isExempt("form.sentenceParts[]", null, scope);

        if (b.size() != a.size()) {
            if (!structureExempt) {
                out.add(new PreservationViolation(elementId, "form.sentenceParts[]", classify(b), classify(a),
                        "size=" + b.size(), "size=" + a.size()));
            }
            return;
        }

        for (int i = 0; i < b.size(); i++) {
            SentencePartEntity pb = b.get(i);
            SentencePartEntity pa = a.get(i);
            String elementKind = pb.getKind() != null
                    ? pb.getKind().name()
                    : (pa.getKind() != null ? pa.getKind().name() : null);
            diffLeaf(elementId, "form.sentenceParts[].text", "form.sentenceParts[" + i + "].text", elementKind,
                    pb.getText(), pa.getText(), scope, out);
            diffLeaf(elementId, "form.sentenceParts[].options", "form.sentenceParts[" + i + "].options", elementKind,
                    pb.getOptions(), pa.getOptions(), scope, out);
        }
    }

    static void diffKnowledgeOwnFields(KnowledgeEntity before, KnowledgeEntity after, Set<ScopedField> scope,
            List<PreservationViolation> out) {
        String id = before.getId();
        diffLeaf(id, "code", before.getCode(), after.getCode(), scope, out);
        diffLeaf(id, "kind", before.getKind(), after.getKind(), scope, out);
        diffLeaf(id, "label", before.getLabel(), after.getLabel(), scope, out);
        diffLeaf(id, "oldId", before.getOldId(), after.getOldId(), scope, out);
        diffLeaf(id, "parentId", before.getParentId(), after.getParentId(), scope, out);
        diffLeaf(id, "isRule", before.isIsRule(), after.isIsRule(), scope, out);
        diffLeaf(id, "instructions", before.getInstructions(), after.getInstructions(), scope, out);
        diffLeaf(id, "order", before.getOrder(), after.getOrder(), scope, out);
        diffLeaf(id, "slug", before.getSlug(), after.getSlug(), scope, out);
        diffLeaf(id, "sentenceMode", before.getSentenceMode(), after.getSentenceMode(), scope, out);
        // quizTemplates is walked separately by the caller (each quiz compared on its own).
    }

    /** Walks the whole course and indexes every knowledge by id, preserving encounter order. */
    static Map<String, KnowledgeEntity> collectKnowledges(CourseEntity course) {
        Map<String, KnowledgeEntity> result = new LinkedHashMap<>();
        if (course == null || course.getRoot() == null || course.getRoot().getMilestones() == null) {
            return result;
        }
        RootNodeEntity root = course.getRoot();
        for (MilestoneEntity milestone : root.getMilestones()) {
            if (milestone.getTopics() == null) {
                continue;
            }
            for (TopicEntity topic : milestone.getTopics()) {
                if (topic.getKnowledges() == null) {
                    continue;
                }
                for (KnowledgeEntity knowledge : topic.getKnowledges()) {
                    result.put(knowledge.getId(), knowledge);
                }
            }
        }
        return result;
    }

    /** Indexes a knowledge's own quizzes by id, preserving encounter order. */
    static Map<String, QuizTemplateEntity> indexQuizzes(KnowledgeEntity knowledge) {
        Map<String, QuizTemplateEntity> result = new LinkedHashMap<>();
        if (knowledge.getQuizTemplates() == null) {
            return result;
        }
        for (QuizTemplateEntity quiz : knowledge.getQuizTemplates()) {
            result.put(quiz.getId(), quiz);
        }
        return result;
    }
}
