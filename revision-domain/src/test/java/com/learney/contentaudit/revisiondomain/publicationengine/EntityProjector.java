package com.learney.contentaudit.revisiondomain.publicationengine;

import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceItemEntity;
import com.learney.contentaudit.coursedomain.ProductionDocumentProjector;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * F-PUBPROD-R002: producción, campo por campo, leída de las entidades —lo mínimo para que la lista de publicación tenga
 * valores en las pruebas que no leen un curso del disco—.
 */
public class EntityProjector implements ProductionDocumentProjector {
    @Override
    public Map<String, Object> project(QuizTemplateEntity quiz, List<String> mongoPaths) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String path : mongoPaths) {
            values.put(path, switch (path) {
                case "translation" -> quiz.getTranslation();
                case "miniTheory" -> quiz.getMiniTheory();
                case "instructions" -> quiz.getInstructions();
                case "kind" -> quiz.getKind();
                case "form.kind" -> quiz.getForm() != null ? quiz.getForm().getKind() : null;
                case "form.sentences" -> quiz.getSentences() != null ? List.copyOf(quiz.getSentences())
                        : List.of();
                case "form.sentenceParts" -> parts(quiz);
                case "form.selection" -> quiz.getForm() != null && quiz.getForm().getMultipleChoice() != null
                        ? quiz.getForm().getMultipleChoice().getSelection() : null;
                case "form.items" -> items(quiz);
                default -> null;
            });
        }
        return values;
    }

    private static Object parts(QuizTemplateEntity quiz) {
        if (quiz.getForm() == null || quiz.getForm().getSentenceParts() == null) {
            return null;
        }
        List<Object> parts = new ArrayList<>();
        for (SentencePartEntity part : quiz.getForm().getSentenceParts()) {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("kind", part.getKind().name());
            value.put("text", part.getText() != null ? part.getText() : "");
            value.put("options", part.getOptions());
            parts.add(value);
        }
        return parts;
    }

    private static Object items(QuizTemplateEntity quiz) {
        if (quiz.getForm() == null || quiz.getForm().getMultipleChoice() == null) {
            return null;
        }
        List<Object> items = new ArrayList<>();
        for (MultipleChoiceItemEntity item : quiz.getForm().getMultipleChoice().getItems()) {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("id", item.getId());
            value.put("incidence", item.getIncidence());
            value.put("label", item.getLabel());
            items.add(value);
        }
        return items;
    }

    @Override
    public Map<String, Object> project(KnowledgeEntity knowledge, List<String> mongoPaths) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String path : mongoPaths) {
            values.put(path, "instructions".equals(path) ? knowledge.getInstructions() : null);
        }
        return values;
    }

    @Override
    public String documentId(QuizTemplateEntity quiz) {
        return quiz.getOidId() != null ? quiz.getOidId() : quiz.getId();
    }

    @Override
    public String documentId(KnowledgeEntity knowledge) {
        return knowledge.getId();
    }
}
