package com.learney.contentaudit.revisiondomain.publicationengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceItemEntity;
import com.learney.contentaudit.coursedomain.ProductionDocumentProjector;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.refinerdomain.DiagnosisKind;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.comparison.VerdictKind;
import com.learney.contentaudit.revisiondomain.preservation.CorrectionScope;
import com.learney.contentaudit.revisiondomain.preservation.FieldWithoutProductionPlaceException;
import com.learney.contentaudit.revisiondomain.preservation.ScopedField;
import com.learney.contentaudit.revisiondomain.preservationengine.DefaultCorrectionScope;
import com.learney.contentaudit.revisiondomain.preservationengine.DefaultProductionFieldMap;
import com.learney.contentaudit.revisiondomain.publication.DocumentReviewStatus;
import com.learney.contentaudit.revisiondomain.publication.ExclusionReason;
import com.learney.contentaudit.revisiondomain.publication.ManifestDocument;
import com.learney.contentaudit.revisiondomain.publication.ManifestExcluded;
import com.learney.contentaudit.revisiondomain.publication.ManifestField;
import com.learney.contentaudit.revisiondomain.publication.ManifestSource;
import com.learney.contentaudit.revisiondomain.publication.ManifestUnsupported;
import com.learney.contentaudit.revisiondomain.publication.ManifestVerdict;
import com.learney.contentaudit.revisiondomain.publication.PublicationManifest;
import com.learney.contentaudit.revisiondomain.publication.PublicationManifestRequest;
import com.learney.contentaudit.revisiondomain.publication.PublishedElement;
import com.learney.contentaudit.revisiondomain.publication.PublishedStep;
import com.learney.contentaudit.revisiondomain.publication.ReviewTally;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * F-PUBPROD-R001, R002 y R004: la lista lleva, por ejercicio o tema, sólo los campos que cambiaron —con su antes y su
 * después, como los guarda producción—, de la tabla de la corrección; un cambio de tipo de ejercicio queda aparte.
 */
public class DefaultPublicationManifestBuilderTest {

    /** Producción, campo por campo, leída de las entidades: lo mínimo para que la lista tenga valores. */
    private static final class EntityProjector implements ProductionDocumentProjector {
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

    private final DefaultPublicationManifestBuilder builder = new DefaultPublicationManifestBuilder(
            new DefaultCorrectionScope(), new DefaultProductionFieldMap(), new EntityProjector());

    private static QuizTemplateEntity quiz(String id, String translation, String miniTheory, String... texts) {
        QuizTemplateEntity quiz = new QuizTemplateEntity();
        quiz.setId(id);
        quiz.setKind("CLOZE");
        quiz.setTranslation(translation);
        quiz.setMiniTheory(miniTheory);
        quiz.setInstructions("Completá.");
        List<SentencePartEntity> parts = new ArrayList<>();
        for (String text : texts) {
            parts.add(text.startsWith("_") ? new SentencePartEntity(SentencePartKind.CLOZE, null,
                    List.of(text.substring(1))) : new SentencePartEntity(SentencePartKind.TEXT, text, null));
        }
        quiz.setForm(new FormEntity("CLOZE", 1.0, "Completá", "cloze", parts, null, null));
        quiz.setSentences(List.of(String.join("", texts).replace("_", "")));
        return quiz;
    }

    private static CourseElementSnapshot snapshot(QuizTemplateEntity quiz) {
        return new CourseElementSnapshot(AuditTarget.QUIZ, quiz.getId(), quiz, null);
    }

    private static PublishedStep step(String changeId, ExercisePart... parts) {
        return new PublishedStep(changeId, Set.of(parts));
    }

    private static PublicationManifestRequest request(List<PublishedElement> elements) {
        return new PublicationManifestRequest("tanda-A1", "plan-1", "A1", "2026-10-12T03:10:44Z", "José",
                new ManifestSource("audit-base", "audit-start", null),
                new ManifestVerdict(VerdictKind.PASSES, true, List.of(), List.of(), null),
                new ReviewTally(1, 1, 0, List.of(), List.of()), elements,
                List.of(new ManifestExcluded("rule-9", ExclusionReason.RULE_EXCLUDED)));
    }

    @Test
    @DisplayName("should list each changed exercise and topic with only the fields that changed, their before and after, the changes and proposals behind them, and count them by field")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R001")
    @Tag("F-PUBPROD-R004")
    public void shouldListEachChangedExerciseAndTopicWithOnlyTheFieldsThatChanged() {
        // El criterio de R001: la traducción y la miniteoría de un ejercicio y la consigna de un tema.
        QuizTemplateEntity before = quiz("q1", "Hace calor adentro.", "Clima.", "It's hot ", "_inside", ".");
        QuizTemplateEntity after = quiz("q1", "Hace calor aquí dentro.", "El clima.", "It's hot ", "_inside", ".");
        KnowledgeEntity topicBefore = new KnowledgeEntity();
        topicBefore.setId("k1");
        topicBefore.setInstructions("Elige la opcion correcta.");
        KnowledgeEntity topicAfter = new KnowledgeEntity();
        topicAfter.setId("k1");
        topicAfter.setInstructions("Elige la opción correcta.");

        PublicationManifest manifest = builder.build(request(List.of(
                new PublishedElement(snapshot(before), snapshot(after), "prop-1", DocumentReviewStatus.APPROVED,
                        List.of(step("rule-1", ExercisePart.TRANSLATION), step("task-2", ExercisePart.MINI_THEORY))),
                new PublishedElement(new CourseElementSnapshot(AuditTarget.KNOWLEDGE, "k1", null, topicBefore),
                        new CourseElementSnapshot(AuditTarget.KNOWLEDGE, "k1", null, topicAfter), "prop-2",
                        DocumentReviewStatus.PENDING, List.of(step("task-3", ExercisePart.TOPIC_INSTRUCTIONS))))));

        assertEquals(PublicationManifest.SCHEMA, manifest.schema());
        assertEquals("tanda-A1", manifest.batchId());
        assertEquals(2, manifest.documents().size());
        ManifestDocument exercise = manifest.documents().get(0);
        assertEquals("quizTemplates", exercise.collection());
        assertEquals("q1", exercise.id());
        assertEquals("q1", exercise.nodeId());
        assertEquals(List.of("rule-1", "task-2"), exercise.changeIds());
        assertEquals(List.of("prop-1"), exercise.proposalIds());
        assertEquals(DocumentReviewStatus.APPROVED, exercise.reviewStatus());
        assertEquals(List.of(new ManifestField("translation", "Hace calor adentro.", "Hace calor aquí dentro."),
                new ManifestField("miniTheory", "Clima.", "El clima.")), exercise.fields());
        ManifestDocument topic = manifest.documents().get(1);
        assertEquals("knowledges", topic.collection());
        assertEquals("k1", topic.id());
        assertEquals(DocumentReviewStatus.PENDING, topic.reviewStatus(), "el tema quedó sin revisar");
        assertEquals(List.of(new ManifestField("instructions", "Elige la opcion correcta.",
                "Elige la opción correcta.")), topic.fields());
        assertEquals(3, manifest.counts().fields());
        assertEquals(2, manifest.counts().documents());
        assertEquals(List.of("translation", "instructions", "miniTheory"),
                List.copyOf(manifest.counts().byPath().keySet()), "por campo, en el orden de la lista");
        assertEquals(List.of(new ManifestExcluded("rule-9", ExclusionReason.RULE_EXCLUDED)), manifest.excluded());
        assertTrue(manifest.unsupported().isEmpty());
    }

    @Test
    @DisplayName("should leave out a field its steps declare but that did not change, and an exercise with no changed field")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R001")
    public void shouldLeaveOutAFieldThatDidNotChangeAndAnExerciseWithNoChangedField() {
        QuizTemplateEntity before = quiz("q1", "Igual.", "Igual.", "I am ", "_at", " home.");
        QuizTemplateEntity after = quiz("q1", "Igual.", "Igual.", "She is ", "_at", " home.");
        QuizTemplateEntity untouched = quiz("q2", "Nada.", "Nada.", "Nothing ", "_here", ".");

        PublicationManifest manifest = builder.build(request(List.of(
                new PublishedElement(snapshot(before), snapshot(after), "prop-1", DocumentReviewStatus.PENDING,
                        List.of(step("rule-1", ExercisePart.STATEMENT, ExercisePart.STORED_SENTENCE,
                                ExercisePart.TRANSLATION, ExercisePart.AUDIO))),
                new PublishedElement(snapshot(untouched), snapshot(untouched), "prop-2",
                        DocumentReviewStatus.PENDING, List.of(step("rule-2", ExercisePart.TRANSLATION))))));

        assertEquals(1, manifest.documents().size(), "el ejercicio sin campos cambiados no está");
        List<ManifestField> fields = manifest.documents().get(0).fields();
        assertEquals(List.of("form.sentences", "form.sentenceParts"), fields.stream().map(ManifestField::path)
                .toList(), "la traducción no cambió y lo que se oye no va");
        assertEquals(List.of("I am at home."), fields.get(0).before());
        assertEquals(List.of("She is at home."), fields.get(0).after());
        @SuppressWarnings("unchecked")
        Map<String, Object> gap = (Map<String, Object>) ((List<?>) fields.get(1).after()).get(1);
        assertEquals("", gap.get("text"), "el hueco va con texto vacío");
        assertEquals(List.of("at"), gap.get("options"));
    }

    @Test
    @DisplayName("should carry a change of the multiple choice as the way of choosing and the options, whole and with numbers as numbers")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R002")
    public void shouldCarryAChangeOfTheMultipleChoiceWholeWithNumbersAsNumbers() {
        QuizTemplateEntity before = multipleChoice("q1", "SINGLE", "on");
        QuizTemplateEntity after = multipleChoice("q1", "SINGLE", "to");

        PublicationManifest manifest = builder.build(request(List.of(new PublishedElement(snapshot(before),
                snapshot(after), "prop-1", DocumentReviewStatus.APPROVED,
                List.of(step("rule-1", ExercisePart.OPTIONS))))));

        List<ManifestField> fields = manifest.documents().get(0).fields();
        assertEquals(List.of("form.items"), fields.stream().map(ManifestField::path).toList(),
                "la forma de elegir no cambió");
        @SuppressWarnings("unchecked")
        Map<String, Object> correct = (Map<String, Object>) ((List<?>) fields.get(0).after()).get(0);
        assertEquals(1.0, correct.get("incidence"));
        assertEquals(3, ((List<?>) fields.get(0).after()).size(), "las opciones van enteras");
    }

    @Test
    @DisplayName("should declare a change of the type of exercise apart, with the change, the exercise, the part and the reason, and leave that exercise out of the list")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R001")
    @Tag("F-PUBPROD-R002")
    public void shouldDeclareAChangeOfTheTypeOfExerciseApartAndLeaveThatExerciseOut() {
        QuizTemplateEntity before = quiz("q1", "Igual.", "Igual.", "I am ", "_at", " home.");
        QuizTemplateEntity after = multipleChoice("q1", "SINGLE", "on");
        after.setKind("MULTIPLE_CHOICE");
        after.getForm().setKind("MULTIPLE_CHOICE");
        QuizTemplateEntity sameKindBefore = multipleChoice("q2", "SINGLE", "on");
        QuizTemplateEntity sameKindAfter = multipleChoice("q2", "SINGLE", "to");

        PublicationManifest manifest = builder.build(request(List.of(
                new PublishedElement(snapshot(before), snapshot(after), "prop-1", DocumentReviewStatus.PENDING,
                        List.of(step("task-1", ExercisePart.FORM_KIND, ExercisePart.TRANSLATION))),
                new PublishedElement(snapshot(sameKindBefore), snapshot(sameKindAfter), "prop-2",
                        DocumentReviewStatus.PENDING, List.of(step("task-2", ExercisePart.FORM_KIND))))));

        assertEquals(List.of(new ManifestUnsupported("task-1", "q1", ExercisePart.FORM_KIND,
                "cambio de tipo de ejercicio: no se publica por script")), manifest.unsupported());
        assertEquals(List.of("q2"), manifest.documents().stream().map(ManifestDocument::id).toList(),
                "el ejercicio cuyo tipo cambió no va; el que declaró el tipo sin cambiarlo, sí");
        assertEquals(List.of("form.items"), manifest.documents().get(0).fields().stream().map(ManifestField::path)
                .toList());
    }

    @Test
    @DisplayName("should stop the list and name the field when the correction table declares a field with no place in production")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R001")
    public void shouldStopTheListAndNameTheFieldWithNoPlaceInProduction() {
        CorrectionScope withANewField = new CorrectionScope() {
            @Override
            public Set<ScopedField> changeableFields(DiagnosisKind kind, AuditTarget target) {
                return Set.of();
            }

            @Override
            public Set<ScopedField> changeableFields(Set<ExercisePart> parts, AuditTarget target) {
                return Set.of(new ScopedField("form.hint", null));
            }
        };
        DefaultPublicationManifestBuilder strict = new DefaultPublicationManifestBuilder(withANewField,
                new DefaultProductionFieldMap(), new EntityProjector());
        QuizTemplateEntity before = quiz("q1", "A.", "A.", "Hi.");
        QuizTemplateEntity after = quiz("q1", "B.", "A.", "Hi.");

        FieldWithoutProductionPlaceException stop = assertThrows(FieldWithoutProductionPlaceException.class,
                () -> strict.build(request(List.of(new PublishedElement(snapshot(before), snapshot(after), "prop-1",
                        DocumentReviewStatus.PENDING, List.of(step("rule-1", ExercisePart.HINT)))))));

        assertTrue(stop.getMessage().contains("form.hint"), stop.getMessage());
    }

    @Test
    @DisplayName("should refuse to build the list when an exercise lacks its before or its after")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R001")
    public void shouldRefuseToBuildTheListWhenAnExerciseLacksItsBeforeOrItsAfter() {
        QuizTemplateEntity after = quiz("q1", "B.", "A.", "Hi.");

        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                () -> builder.build(request(List.of(new PublishedElement(null, snapshot(after), "prop-1",
                        DocumentReviewStatus.PENDING, List.of(step("rule-1", ExercisePart.TRANSLATION)))))));

        assertTrue(refusal.getMessage().contains("q1"), refusal.getMessage());
    }

    private static QuizTemplateEntity multipleChoice(String id, String selection, String third) {
        QuizTemplateEntity quiz = new QuizTemplateEntity();
        quiz.setId(id);
        quiz.setKind("MULTIPLE_CHOICE");
        quiz.setTranslation("Igual.");
        quiz.setForm(new FormEntity("MULTIPLE_CHOICE", 1.0, "Elegí", "mc", List.of(
                new SentencePartEntity(SentencePartKind.TEXT, "I am ", null),
                new SentencePartEntity(SentencePartKind.CLOZE, null, null),
                new SentencePartEntity(SentencePartKind.TEXT, " home.", null)),
                new MultipleChoiceEntity(selection, List.of(new MultipleChoiceItemEntity("at", 1.0, "at"),
                        new MultipleChoiceItemEntity("in", 0.0, "in"),
                        new MultipleChoiceItemEntity(third, 0.0, third))), null));
        quiz.setSentences(List.of());
        return quiz;
    }
}
