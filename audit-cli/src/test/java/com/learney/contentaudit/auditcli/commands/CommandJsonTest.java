package com.learney.contentaudit.auditcli.commands;

import static org.junit.jupiter.api.Assertions.*;

import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.batch.BatchChangeStep;
import com.learney.contentaudit.revisiondomain.batch.BatchReviewDecision;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CommandJsonTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preservesExplicitNullsInNestedCourseContentWithoutAddingEmptyBeanProperties(boolean compact) {
        Function<Object, String> write = compact ? CommandJson::writeCompact : CommandJson::write;
        var snapshot = snapshot("before");
        String json = write.apply(snapshot);

        assertEquals(snapshot, CommandJson.read(json, CourseElementSnapshot.class));
        assertTrue(json.contains("\"options\""), "An explicit null inside a map is course content");
        assertFalse(json.contains("\"knowledge\""), "An absent typed property stays omitted");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void reviewSelectionSurvivesTheUiJsonRoundTripExactly(boolean compact) {
        Function<Object, String> write = compact ? CommandJson::writeCompact : CommandJson::write;
        var before = snapshot("before");
        var after = snapshot("after");
        var step = new BatchChangeStep("quiz", List.of(ExercisePart.MINI_THEORY), after);
        var decision = new BatchReviewDecision("batch", List.of(
                new BatchReviewDecision.Selection("change", step, before, after)),
                BatchReviewDecision.Status.APPROVED, "Operador UI", null);

        // The UI forwards decoded maps, rather than rebuilding course entities.
        var uiPayload = CommandJson.read(write.apply(decision), Map.class);
        assertEquals(decision, CommandJson.read(write.apply(uiPayload), BatchReviewDecision.class));
    }

    static CourseElementSnapshot snapshot(String theory) {
        var part = new LinkedHashMap<String, Object>();
        part.put("kind", "TEXT");
        part.put("text", "She");
        part.put("options", null);
        var unknown = new LinkedHashMap<String, Object>();
        unknown.put("formCloze", Map.of("sentenceParts", List.of(part),
                "incidence", Map.of("$numberDouble", "1.0")));
        unknown.put("legacyNull", null);
        var quiz = new QuizTemplateEntity();
        quiz.setId("quiz");
        quiz.setMiniTheory(theory);
        quiz.setTranslation("Ella es inglesa.");
        quiz.setUnmodeledFields(unknown);
        var form = new FormEntity();
        form.setUnmodeledFields(unknown);
        quiz.setForm(form);
        return new CourseElementSnapshot(AuditTarget.QUIZ, "quiz", quiz, null);
    }
}
