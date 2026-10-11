package com.learney.contentaudit.revisiondomain.publicationengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceItemEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.refinerdomain.DiagnosisKind;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.comparison.VerdictKind;
import com.learney.contentaudit.revisiondomain.preservation.CorrectionScope;
import com.learney.contentaudit.revisiondomain.preservation.ScopedField;
import com.learney.contentaudit.revisiondomain.preservationengine.DefaultCorrectionScope;
import com.learney.contentaudit.revisiondomain.publication.DocumentReviewStatus;
import com.learney.contentaudit.revisiondomain.publication.ExclusionReason;
import com.learney.contentaudit.revisiondomain.publication.FieldWithoutProductionPlaceException;
import com.learney.contentaudit.revisiondomain.publication.ManifestDocument;
import com.learney.contentaudit.revisiondomain.publication.ManifestExcluded;
import com.learney.contentaudit.revisiondomain.publication.ManifestField;
import com.learney.contentaudit.revisiondomain.publication.ManifestSource;
import com.learney.contentaudit.revisiondomain.publication.ManifestUnsupported;
import com.learney.contentaudit.revisiondomain.publication.ManifestVerdict;
import com.learney.contentaudit.revisiondomain.publication.PublicationManifest;
import com.learney.contentaudit.revisiondomain.publication.PublicationManifestRequest;
import com.learney.contentaudit.revisiondomain.publication.PublishedElement;
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

    private static Map.Entry<String, Set<ExercisePart>> step(String changeId, ExercisePart... parts) {
        return Map.entry(changeId, Set.of(parts));
    }

    /** Las partes de cada cambio, en el orden de la tanda. */
    @SafeVarargs
    private static Map<String, Set<ExercisePart>> steps(Map.Entry<String, Set<ExercisePart>>... steps) {
        Map<String, Set<ExercisePart>> byChange = new LinkedHashMap<>();
        for (Map.Entry<String, Set<ExercisePart>> step : steps) {
            byChange.put(step.getKey(), step.getValue());
        }
        return byChange;
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
                        steps(step("rule-1", ExercisePart.TRANSLATION), step("task-2", ExercisePart.MINI_THEORY))),
                new PublishedElement(new CourseElementSnapshot(AuditTarget.KNOWLEDGE, "k1", null, topicBefore),
                        new CourseElementSnapshot(AuditTarget.KNOWLEDGE, "k1", null, topicAfter), "prop-2",
                        DocumentReviewStatus.PENDING, steps(step("task-3", ExercisePart.TOPIC_INSTRUCTIONS))))));

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
                        steps(step("rule-1", ExercisePart.STATEMENT, ExercisePart.STORED_SENTENCE,
                                ExercisePart.TRANSLATION, ExercisePart.AUDIO))),
                new PublishedElement(snapshot(untouched), snapshot(untouched), "prop-2",
                        DocumentReviewStatus.PENDING, steps(step("rule-2", ExercisePart.TRANSLATION))))));

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
    @DisplayName("should carry a change of the text of an option of the multiple choice as the options, whole and with numbers as numbers")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R002")
    public void shouldCarryAChangeOfTheMultipleChoiceWholeWithNumbersAsNumbers() {
        QuizTemplateEntity before = multipleChoice("q1", "SINGLE", option("at", 1.0, "at"), option("in", 0.0, "in"),
                option("on", 0.0, "on"));
        QuizTemplateEntity after = multipleChoice("q1", "SINGLE", option("at", 1.0, "at"), option("in", 0.0, "in"),
                option("on", 0.0, "to"));

        PublicationManifest manifest = builder.build(request(List.of(new PublishedElement(snapshot(before),
                snapshot(after), "prop-1", DocumentReviewStatus.APPROVED,
                steps(step("rule-1", ExercisePart.OPTIONS))))));

        List<ManifestField> fields = manifest.documents().get(0).fields();
        assertEquals(List.of("form.items"), fields.stream().map(ManifestField::path).toList(),
                "la forma de elegir no cambió");
        @SuppressWarnings("unchecked")
        Map<String, Object> correct = (Map<String, Object>) ((List<?>) fields.get(0).after()).get(0);
        assertEquals(1.0, correct.get("incidence"));
        assertEquals(3, ((List<?>) fields.get(0).after()).size(), "las opciones van enteras");
        @SuppressWarnings("unchecked")
        Map<String, Object> changed = (Map<String, Object>) ((List<?>) fields.get(0).after()).get(2);
        assertEquals("on", changed.get("id"), "el identificador de la opción no cambia");
        assertEquals("to", changed.get("label"));
        assertTrue(manifest.unsupported().isEmpty());
    }

    @Test
    @DisplayName("should declare apart, and leave out of the list, a change of which option is correct, of the way of choosing, of the number of options or of the identifier of an option")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R001")
    @Tag("F-PUBPROD-R002")
    public void shouldDeclareApartAChangeOfTheMultipleChoiceBeyondTheTextOfItsOptions() {
        QuizTemplateEntity anotherCorrect = multipleChoice("q1", "SINGLE", option("at", 0.0, "at"),
                option("in", 1.0, "in"), option("on", 0.0, "on"));
        QuizTemplateEntity anotherWay = multipleChoice("q2", "MULTIPLE", option("at", 1.0, "at"),
                option("in", 0.0, "in"), option("on", 0.0, "on"));
        QuizTemplateEntity oneMore = multipleChoice("q3", "SINGLE", option("at", 1.0, "at"), option("in", 0.0, "in"),
                option("on", 0.0, "on"), option("to", 0.0, "to"));
        QuizTemplateEntity anotherId = multipleChoice("q4", "SINGLE", option("at", 1.0, "at"),
                option("in", 0.0, "in"), option("to", 0.0, "to"));
        List<PublishedElement> elements = new ArrayList<>();
        int n = 1;
        for (QuizTemplateEntity after : List.of(anotherCorrect, anotherWay, oneMore, anotherId)) {
            QuizTemplateEntity before = multipleChoice(after.getId(), "SINGLE", option("at", 1.0, "at"),
                    option("in", 0.0, "in"), option("on", 0.0, "on"));
            elements.add(new PublishedElement(snapshot(before), snapshot(after), "prop-" + n,
                    DocumentReviewStatus.PENDING, steps(step("task-" + n, ExercisePart.OPTIONS,
                            ExercisePart.TRANSLATION))));
            n++;
        }

        PublicationManifest manifest = builder.build(request(elements));

        assertTrue(manifest.documents().isEmpty(), "ninguno de los cuatro va en la lista: " + manifest.documents());
        assertEquals(List.of("q1", "q2", "q3", "q4"), manifest.unsupported().stream().map(ManifestUnsupported::nodeId)
                .toList());
        for (ManifestUnsupported held : manifest.unsupported()) {
            assertEquals(ExercisePart.OPTIONS, held.part());
            assertTrue(held.reason().contains("sólo se publica el texto de cada opción"), held.reason());
            assertTrue(held.reason().contains("no se publica por script"), held.reason());
        }
        assertEquals(0, manifest.counts().documents());
        assertEquals(0, manifest.counts().fields());
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
        QuizTemplateEntity sameKindAfter = multipleChoice("q2", "SINGLE", option("at", 1.0, "at"),
                option("in", 0.0, "in"), option("on", 0.0, "to"));

        PublicationManifest manifest = builder.build(request(List.of(
                new PublishedElement(snapshot(before), snapshot(after), "prop-1", DocumentReviewStatus.PENDING,
                        steps(step("task-1", ExercisePart.FORM_KIND, ExercisePart.TRANSLATION))),
                new PublishedElement(snapshot(sameKindBefore), snapshot(sameKindAfter), "prop-2",
                        DocumentReviewStatus.PENDING, steps(step("task-2", ExercisePart.FORM_KIND))))));

        assertEquals(List.of(new ManifestUnsupported("task-1", "q1", ExercisePart.FORM_KIND,
                "cambio de tipo de ejercicio: no se publica por script")), manifest.unsupported());
        assertEquals(List.of("q2"), manifest.documents().stream().map(ManifestDocument::id).toList(),
                "el ejercicio cuyo tipo cambió no va; el que declaró el tipo sin cambiarlo, sí");
        assertEquals(List.of("form.items"), manifest.documents().get(0).fields().stream().map(ManifestField::path)
                .toList());
    }

    @Test
    @DisplayName("should never write the title of an exercise nor the name of a topic, even when the correction that changed them declares them")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R002")
    public void shouldNeverWriteTheTitleOfAnExerciseNorTheNameOfATopic() {
        // Un ejercicio entero corregido cuyo título también cambió.
        QuizTemplateEntity before = quiz("q1", "Hace calor.", "Clima.", "It's hot ", "_inside", ".");
        before.setTitle("El clima");
        QuizTemplateEntity after = quiz("q1", "Hace calor adentro.", "Clima.", "It's hot ", "_inside", ".");
        after.setTitle("El clima en casa");
        // Un tema cuyo nombre y consigna cambió una corrección de título, que declara el nombre, la consigna y el
        // título de sus ejercicios; y otro al que sólo le cambió el nombre.
        CorrectionScope titleCorrection = new CorrectionScope() {
            @Override
            public Set<ScopedField> changeableFields(DiagnosisKind kind, AuditTarget target) {
                return Set.of();
            }

            @Override
            public Set<ScopedField> changeableFields(Set<ExercisePart> parts, AuditTarget target) {
                return target == AuditTarget.KNOWLEDGE ? new DefaultCorrectionScope().changeableFields(
                        DiagnosisKind.KNOWLEDGE_TITLE_LENGTH, AuditTarget.KNOWLEDGE)
                        : new DefaultCorrectionScope().changeableFields(parts, target);
            }
        };
        DefaultPublicationManifestBuilder withTitles = new DefaultPublicationManifestBuilder(titleCorrection,
                new DefaultProductionFieldMap(), new EntityProjector());
        KnowledgeEntity topicBefore = topic("k1", "El clima", "Elige la opcion correcta.");
        KnowledgeEntity topicAfter = topic("k1", "El clima en casa", "Elige la opción correcta.");
        KnowledgeEntity onlyNameBefore = topic("k2", "Rutinas", "Completá.");
        KnowledgeEntity onlyNameAfter = topic("k2", "Rutinas de la mañana", "Completá.");

        PublicationManifest manifest = withTitles.build(request(List.of(
                new PublishedElement(snapshot(before), snapshot(after), "prop-1", DocumentReviewStatus.PENDING,
                        steps(step("task-1", ExercisePart.WHOLE_EXERCISE))),
                new PublishedElement(new CourseElementSnapshot(AuditTarget.KNOWLEDGE, "k1", null, topicBefore),
                        new CourseElementSnapshot(AuditTarget.KNOWLEDGE, "k1", null, topicAfter), "prop-2",
                        DocumentReviewStatus.PENDING, steps(step("task-2", ExercisePart.TOPIC_INSTRUCTIONS))),
                new PublishedElement(new CourseElementSnapshot(AuditTarget.KNOWLEDGE, "k2", null, onlyNameBefore),
                        new CourseElementSnapshot(AuditTarget.KNOWLEDGE, "k2", null, onlyNameAfter), "prop-3",
                        DocumentReviewStatus.PENDING, steps(step("task-3", ExercisePart.TOPIC_INSTRUCTIONS))))));

        assertEquals(List.of("q1", "k1"), manifest.documents().stream().map(ManifestDocument::id).toList(),
                "el tema al que sólo le cambió el nombre no va en la lista");
        assertEquals(List.of("translation"), manifest.documents().get(0).fields().stream().map(ManifestField::path)
                .toList(), "del ejercicio va la traducción, no el título");
        assertEquals(List.of(new ManifestField("instructions", "Elige la opcion correcta.",
                "Elige la opción correcta.")), manifest.documents().get(1).fields(), "del tema va la consigna");
        for (ManifestDocument document : manifest.documents()) {
            for (ManifestField field : document.fields()) {
                assertFalse(String.valueOf(field.after()).contains("en casa"),
                        "ni el título ni el nombre nuevos viajan, en ninguna ruta: " + field);
            }
        }
        assertEquals(Set.of("translation", "instructions"), manifest.counts().byPath().keySet());
        assertTrue(manifest.unsupported().isEmpty(), "no es algo que falte publicar: sigue su camino por plan");
    }

    private static KnowledgeEntity topic(String id, String label, String instructions) {
        KnowledgeEntity topic = new KnowledgeEntity();
        topic.setId(id);
        topic.setLabel(label);
        topic.setInstructions(instructions);
        return topic;
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
                        DocumentReviewStatus.PENDING, steps(step("rule-1", ExercisePart.HINT)))))));

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
                        DocumentReviewStatus.PENDING, steps(step("rule-1", ExercisePart.TRANSLATION)))))));

        assertTrue(refusal.getMessage().contains("q1"), refusal.getMessage());
    }

    private static QuizTemplateEntity multipleChoice(String id, String selection, String third) {
        return multipleChoice(id, selection, option("at", 1.0, "at"), option("in", 0.0, "in"),
                option(third, 0.0, third));
    }

    private static MultipleChoiceItemEntity option(String id, double incidence, String label) {
        return new MultipleChoiceItemEntity(id, incidence, label);
    }

    private static QuizTemplateEntity multipleChoice(String id, String selection, MultipleChoiceItemEntity... items) {
        QuizTemplateEntity quiz = new QuizTemplateEntity();
        quiz.setId(id);
        quiz.setKind("MULTIPLE_CHOICE");
        quiz.setTranslation("Igual.");
        quiz.setForm(new FormEntity("MULTIPLE_CHOICE", 1.0, "Elegí", "mc", List.of(
                new SentencePartEntity(SentencePartKind.TEXT, "I am ", null),
                new SentencePartEntity(SentencePartKind.CLOZE, null, null),
                new SentencePartEntity(SentencePartKind.TEXT, " home.", null)),
                new MultipleChoiceEntity(selection, List.of(items)), null));
        quiz.setSentences(List.of());
        return quiz;
    }
}
