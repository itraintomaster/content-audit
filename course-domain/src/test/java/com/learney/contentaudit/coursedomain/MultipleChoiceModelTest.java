package com.learney.contentaudit.coursedomain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("multiple-choice")
class MultipleChoiceModelTest {

    private static MultipleChoiceEntity choices(String correct, String... wrong) {
        List<MultipleChoiceItemEntity> items = new java.util.ArrayList<>();
        for (String label : wrong) {
            items.add(new MultipleChoiceItemEntity(label, 0.0, label));
        }
        items.add(new MultipleChoiceItemEntity(correct, 1.0, correct));
        return new MultipleChoiceEntity("SINGLE", items);
    }

    private static FormEntity multipleChoiceForm() {
        FormEntity form = new FormEntity("MULTIPLE_CHOICE", 1.0, "", "", List.of(
                new SentencePartEntity(SentencePartKind.TEXT, "She", null),
                new SentencePartEntity(SentencePartKind.CLOZE, "", null),
                new SentencePartEntity(SentencePartKind.TEXT, "English.", null)));
        form.setMultipleChoice(choices("is", "am", "are"));
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("futureKey", "kept");
        form.setUnmodeledFields(extra);
        return form;
    }

    @Test
    @DisplayName("FormKind reads CLOZE and MULTIPLE_CHOICE and maps anything else, null included, to OTHER")
    void formKindMapsRawStrings() {
        assertEquals(FormKind.CLOZE, FormKind.from("CLOZE"));
        assertEquals(FormKind.MULTIPLE_CHOICE, FormKind.from("MULTIPLE_CHOICE"));
        assertEquals(FormKind.OTHER, FormKind.from("ORDERING"));
        assertEquals(FormKind.OTHER, FormKind.from(null));
    }

    @Test
    @DisplayName("correctItem returns the option with incidence greater than zero, and empty when there is none")
    void correctItemIsTheOneWithIncidence() {
        assertEquals("is", choices("is", "am", "are").correctItem().orElseThrow().getLabel());
        MultipleChoiceEntity none = new MultipleChoiceEntity("SINGLE",
                List.of(new MultipleChoiceItemEntity("am", 0.0, "am")));
        assertTrue(none.correctItem().isEmpty());
        assertTrue(new MultipleChoiceEntity("SINGLE", null).correctItem().isEmpty());
    }

    @Test
    @DisplayName("The form copy constructor carries every field, the multiple-choice payload and the unmodeled fields included")
    void formCopyCarriesEveryField() {
        FormEntity original = multipleChoiceForm();
        FormEntity copy = new FormEntity(original);
        assertEquals(original, copy);
        assertSame(original.getMultipleChoice(), copy.getMultipleChoice());
        assertSame(original.getUnmodeledFields(), copy.getUnmodeledFields());
        assertEquals(FormKind.MULTIPLE_CHOICE, copy.formKind());
    }

    @Test
    @DisplayName("The quiz copy constructor carries every field, the unmodeled fields included")
    void quizCopyCarriesEveryField() {
        QuizTemplateEntity original = new QuizTemplateEntity("q1", "q1", "MULTIPLE_CHOICE", "k1",
                "title", "instructions", "translation", "theory", "topic", multipleChoiceForm(),
                0.0, 1.0, 2.0, "code", "audio", "image", "answerAudio", "answerImage",
                "mini", "success", List.of("She is English."));
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("instructionsAnteriores", "old");
        extra.put("formCloze", Map.of("kind", "CLOZE"));
        original.setUnmodeledFields(extra);

        QuizTemplateEntity copy = new QuizTemplateEntity(original);

        assertEquals(original, copy);
        assertEquals(List.of("instructionsAnteriores", "formCloze"),
                List.copyOf(copy.getUnmodeledFields().keySet()));
    }

    @Test
    @DisplayName("Equality takes the multiple-choice payload and the unmodeled fields into account")
    void equalityIncludesNewFields() {
        FormEntity a = multipleChoiceForm();
        FormEntity b = new FormEntity(a);
        b.setMultipleChoice(choices("am", "is", "are"));
        assertNotEquals(a, b);

        FormEntity c = new FormEntity(a);
        c.setUnmodeledFields(Map.of("other", 1));
        assertNotEquals(a, c);
    }

    @Test
    @DisplayName("A quiz reads its kind from the form and falls back to the quiz-level kind when the form has none")
    void quizFormKindFallsBackToQuizKind() {
        QuizTemplateEntity quiz = new QuizTemplateEntity();
        quiz.setKind("MULTIPLE_CHOICE");
        quiz.setForm(new FormEntity(null, 1.0, "", "", List.of()));
        assertEquals(FormKind.MULTIPLE_CHOICE, quiz.formKind());

        quiz.setForm(new FormEntity("CLOZE", 1.0, "", "", List.of()));
        assertEquals(FormKind.CLOZE, quiz.formKind());

        QuizTemplateEntity noForm = new QuizTemplateEntity();
        assertEquals(FormKind.OTHER, noForm.formKind());
    }
}
