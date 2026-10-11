package com.learney.contentaudit.revisiondomain.preservationengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.refinerdomain.DiagnosisKind;
import com.learney.contentaudit.revisiondomain.preservation.CorrectionScope;
import com.learney.contentaudit.revisiondomain.preservation.FieldWithoutProductionPlaceException;
import com.learney.contentaudit.revisiondomain.preservation.ProductionFieldMap;
import com.learney.contentaudit.revisiondomain.preservation.ProductionPlace;
import com.learney.contentaudit.revisiondomain.preservation.ScopedField;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * F-PUBPROD-R001 (inv. 5) y R002: cada campo que la tabla de la corrección puede declarar tiene un lugar en producción,
 * y ninguno cae en una ruta prohibida.
 */
public class DefaultProductionFieldMapTest {

    private final CorrectionScope scope = new DefaultCorrectionScope();

    private final ProductionFieldMap map = new DefaultProductionFieldMap();

    /** Todos los campos que la corrección puede declarar hoy, de a una parte y por tipo de diagnóstico. */
    private Map<ScopedField, AuditTarget> everyDeclarableField() {
        Map<ScopedField, AuditTarget> fields = new LinkedHashMap<>();
        for (AuditTarget target : List.of(AuditTarget.QUIZ, AuditTarget.KNOWLEDGE)) {
            for (ExercisePart part : ExercisePart.values()) {
                for (ScopedField field : scope.changeableFields(Set.of(part), target)) {
                    fields.put(field, target);
                }
            }
            for (DiagnosisKind kind : DiagnosisKind.values()) {
                for (ScopedField field : scope.changeableFields(kind, target)) {
                    fields.put(field, target);
                }
            }
        }
        return fields;
    }

    @Test
    @DisplayName("should give a production place to every field the correction table can declare, for every part and every diagnosis kind")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R001")
    @Tag("F-PUBPROD-R002")
    public void shouldGiveAProductionPlaceToEveryFieldTheCorrectionTableCanDeclareForEveryPartAndEveryDiagnosisKind() {
        Map<ScopedField, AuditTarget> fields = everyDeclarableField();

        assertFalse(fields.isEmpty());
        for (Map.Entry<ScopedField, AuditTarget> field : fields.entrySet()) {
            ProductionPlace place = map.placeOf(field.getKey(), field.getValue());
            assertTrue(place != null, "sin lugar: " + field.getKey().getPath());
        }
        // Las partes juntas no declaran nada que las partes de a una no declaren.
        Set<ScopedField> all = new LinkedHashSet<>(scope.changeableFields(EnumSet.allOf(ExercisePart.class),
                AuditTarget.QUIZ));
        all.removeAll(fields.keySet());
        assertTrue(all.isEmpty(), "campos sin revisar: " + all);
    }

    @Test
    @DisplayName("should never write the identifier, the exercise type or the type, incidence, label or name of the form, and write exercises and topics in their own collections")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R002")
    public void shouldNeverWriteTheIdentifierTheExerciseTypeOrTheTypeIncidenceLabelOrNameOfTheForm() {
        for (Map.Entry<ScopedField, AuditTarget> field : everyDeclarableField().entrySet()) {
            ProductionPlace place = map.placeOf(field.getKey(), field.getValue());
            if (place instanceof ProductionPlace.Write write) {
                for (String path : write.mongoPaths()) {
                    assertFalse(ProductionFieldMap.FORBIDDEN_PATHS.contains(path), path);
                }
                String expected = field.getValue() == AuditTarget.QUIZ ? "quizTemplates" : "knowledges";
                assertEquals(expected, write.collection(), field.getKey().getPath());
            }
        }
        assertEquals(Set.of("_id", "kind", "form.kind", "form.incidence", "form.label", "form.name"),
                ProductionFieldMap.FORBIDDEN_PATHS);
        assertThrows(IllegalArgumentException.class,
                () -> new ProductionPlace.Write("quizTemplates", List.of("form.incidence")));
        assertThrows(IllegalArgumentException.class,
                () -> new ProductionPlace.Write("users", List.of("translation")));
    }

    @Test
    @DisplayName("should write each field where production keeps it: the stored sentence, translation, instructions, mini theory, the whole statement parts, the whole multiple choice and the topic instructions")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R002")
    public void shouldWriteEachFieldWhereProductionKeepsIt() {
        assertWrites("quizTemplates", List.of("form.sentences"), new ScopedField("sentences", null), AuditTarget.QUIZ);
        assertWrites("quizTemplates", List.of("translation"), new ScopedField("translation", null), AuditTarget.QUIZ);
        assertWrites("quizTemplates", List.of("instructions"), new ScopedField("instructions", null),
                AuditTarget.QUIZ);
        assertWrites("quizTemplates", List.of("miniTheory"), new ScopedField("miniTheory", null), AuditTarget.QUIZ);
        assertWrites("quizTemplates", List.of("form.sentenceParts"), new ScopedField("form.sentenceParts[]", null),
                AuditTarget.QUIZ);
        assertWrites("quizTemplates", List.of("form.sentenceParts"),
                new ScopedField("form.sentenceParts[].text", "TEXT"), AuditTarget.QUIZ);
        assertWrites("quizTemplates", List.of("form.sentenceParts"),
                new ScopedField("form.sentenceParts[].options", "CLOZE"), AuditTarget.QUIZ);
        assertWrites("quizTemplates", List.of("form.selection", "form.items"),
                new ScopedField("form.multipleChoice", null), AuditTarget.QUIZ);
        assertWrites("knowledges", List.of("instructions"), new ScopedField("instructions", null),
                AuditTarget.KNOWLEDGE);
    }

    @Test
    @DisplayName("should leave the titles and the topic name to the plan path and what is heard to the re-recording, and hold an exercise type change as not publishable by script")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R001")
    @Tag("F-PUBPROD-R002")
    public void shouldLeaveTitlesToThePlanPathAudioToReRecordingAndHoldAnExerciseTypeChange() {
        assertInstanceOf(ProductionPlace.NotWritten.class,
                map.placeOf(new ScopedField("label", null), AuditTarget.KNOWLEDGE));
        assertInstanceOf(ProductionPlace.NotWritten.class,
                map.placeOf(new ScopedField("quizTemplates[].title", null), AuditTarget.KNOWLEDGE));
        ProductionPlace audio = map.placeOf(new ScopedField("(audio)", null), AuditTarget.QUIZ);
        assertInstanceOf(ProductionPlace.NotWritten.class, audio);
        assertTrue(((ProductionPlace.NotWritten) audio).reason().contains("F-TANDA-R012"));

        for (String path : List.of("kind", "form.kind")) {
            ProductionPlace place = map.placeOf(new ScopedField(path, null), AuditTarget.QUIZ);
            ProductionPlace.Unsupported unsupported = assertInstanceOf(ProductionPlace.Unsupported.class, place);
            assertEquals(List.of(path), unsupported.detectionPaths());
            assertTrue(unsupported.reason().contains("no se publica por script"), unsupported.reason());
        }
    }

    @Test
    @DisplayName("should refuse a field with no declared production place, naming it, instead of guessing where it goes")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R001")
    public void shouldRefuseAFieldWithNoDeclaredProductionPlaceNamingIt() {
        FieldWithoutProductionPlaceException unknown = assertThrows(FieldWithoutProductionPlaceException.class,
                () -> map.placeOf(new ScopedField("form.hint", null), AuditTarget.QUIZ));
        assertTrue(unknown.getMessage().contains("form.hint"), unknown.getMessage());

        // La fila es exacta: la misma ruta con otro tipo de parte, o en otro destino, no tiene lugar.
        assertThrows(FieldWithoutProductionPlaceException.class,
                () -> map.placeOf(new ScopedField("form.sentenceParts[].text", null), AuditTarget.QUIZ));
        assertThrows(FieldWithoutProductionPlaceException.class,
                () -> map.placeOf(new ScopedField("translation", null), AuditTarget.KNOWLEDGE));
        assertThrows(FieldWithoutProductionPlaceException.class,
                () -> map.placeOf(new ScopedField("instructions", null), AuditTarget.TOPIC));
    }

    private void assertWrites(String collection, List<String> paths, ScopedField field, AuditTarget target) {
        ProductionPlace.Write write = assertInstanceOf(ProductionPlace.Write.class, map.placeOf(field, target));
        assertEquals(collection, write.collection());
        assertEquals(paths, write.mongoPaths());
    }
}
