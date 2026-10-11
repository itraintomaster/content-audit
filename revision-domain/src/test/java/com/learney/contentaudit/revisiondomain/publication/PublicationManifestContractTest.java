package com.learney.contentaudit.revisiondomain.publication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.revisiondomain.comparison.VerdictKind;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * F-PUBPROD-R001 y R002: la lista de publicación es el contrato con el paso a producción. El golden compartido
 * ({@code publication/manifest-golden.json}) es el mismo que lee el lado del paso a producción: si una clave cambia de
 * nombre o de lugar, esta prueba falla antes de que lo note producción.
 */
public class PublicationManifestContractTest {

    private static final String GOLDEN = "/publication/manifest-golden.json";

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode golden() throws IOException {
        try (InputStream in = PublicationManifestContractTest.class.getResourceAsStream(GOLDEN)) {
            return mapper.readTree(in);
        }
    }

    private static Map<String, Object> part(String kind, String text) {
        return part(kind, text, null);
    }

    private static Map<String, Object> part(String kind, String text, List<String> options) {
        Map<String, Object> part = new LinkedHashMap<>();
        part.put("kind", kind);
        part.put("text", text);
        part.put("options", options);
        return part;
    }

    private static Map<String, Object> item(String id, double incidence) {
        return item(id, incidence, id);
    }

    private static Map<String, Object> item(String id, double incidence, String label) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("incidence", incidence);
        item.put("label", label);
        return item;
    }

    /** La misma lista que el golden, armada con los registros de la publicación. */
    private static PublicationManifest theGoldenList() {
        Map<String, Integer> byPath = new LinkedHashMap<>();
        byPath.put("translation", 2);
        byPath.put("instructions", 1);
        byPath.put("miniTheory", 1);
        byPath.put("form.sentenceParts", 1);
        byPath.put("form.items", 1);
        return new PublicationManifest(PublicationManifest.SCHEMA, "tanda-A1-20261011T120000Z", "plan-20260929", "A1",
                "2026-10-12T03:10:44Z", "José",
                new ManifestSource("audit-20260929", "audit-20261010", null),
                new ManifestVerdict(VerdictKind.PASSES, true, List.of(), List.of(), null),
                new ReviewTally(1, 2, 1, List.of("rule-3"), List.of("task-9")),
                List.of(
                        new ManifestDocument(ProductionFieldMap.QUIZ_COLLECTION, "67fab6d59930102295341fa5",
                                "67fab6d59930102295341fa5", List.of("rule-1"), List.of("prop-1"),
                                DocumentReviewStatus.APPROVED, List.of(
                                        new ManifestField("translation", "Hace calor adentro.",
                                                "Hace calor aquí dentro."),
                                        new ManifestField("miniTheory", "Usamos «it's» para el clima.",
                                                "Usamos «it's» para hablar del clima."))),
                        new ManifestDocument(ProductionFieldMap.QUIZ_COLLECTION, "67fab6d59930102295342c7f",
                                "67fab6d59930102295342c7f", List.of("task-7"), List.of("prop-2"),
                                DocumentReviewStatus.PENDING, List.of(
                                        new ManifestField("form.sentenceParts",
                                                List.of(part("TEXT", "I am "), part("CLOZE", "", List.of("at")),
                                                        part("TEXT", " home.")),
                                                List.of(part("TEXT", "She is "), part("CLOZE", "", List.of("at")),
                                                        part("TEXT", " home."))))),
                        new ManifestDocument(ProductionFieldMap.QUIZ_COLLECTION, "67fab6d59930102295342c80",
                                "67fab6d59930102295342c80", List.of("task-12"), List.of("prop-4"),
                                DocumentReviewStatus.PENDING, List.of(
                                        new ManifestField("translation", "Estoy en casa.", "Estoy en mi casa."),
                                        new ManifestField("form.items",
                                                List.of(item("in", 0.0), item("at", 1.0), item("on", 0.0)),
                                                List.of(item("in", 0.0), item("at", 1.0), item("on", 0.0, "to"))))),
                        new ManifestDocument(ProductionFieldMap.KNOWLEDGE_COLLECTION, "400000000000000000000001",
                                "400000000000000000000001", List.of("task-8"), List.of("prop-3"),
                                DocumentReviewStatus.PENDING, List.of(new ManifestField("instructions",
                                        "Elige la opcion correcta.", "Elige la opción correcta.")))),
                List.of(new ManifestUnsupported("task-10", "67fab6d59930102295341fb0", ExercisePart.FORM_KIND,
                        "cambio de tipo de ejercicio: no se publica por script"),
                        new ManifestUnsupported("task-13", "67fab6d59930102295342c81", ExercisePart.STATEMENT,
                                "opción múltiple: sólo se publica el texto de cada opción; cambiar el enunciado, la "
                                        + "oración guardada, cuál es la correcta, la forma de elegir o las opciones no "
                                        + "se publica por script")),
                List.of(new ManifestExcluded("rule-3", ExclusionReason.REVIEW_REJECTED),
                        new ManifestExcluded("task-9", ExclusionReason.COLLATERAL),
                        new ManifestExcluded("rule-4", ExclusionReason.RULE_EXCLUDED),
                        new ManifestExcluded("task-11", ExclusionReason.DROPPED_BY_JOSE)),
                new ManifestCounts(4, 6, byPath));
    }

    /** Igualdad de árbol que además exige el mismo orden de claves en cada objeto. */
    private static void assertSameTree(JsonNode expected, JsonNode actual, String where) {
        if (expected.isObject()) {
            assertTrue(actual.isObject(), where + " debería ser un objeto");
            List<String> expectedKeys = new ArrayList<>();
            expected.fieldNames().forEachRemaining(expectedKeys::add);
            List<String> actualKeys = new ArrayList<>();
            actual.fieldNames().forEachRemaining(actualKeys::add);
            assertEquals(expectedKeys, actualKeys, "las claves de " + where + ", en orden");
            for (String key : expectedKeys) {
                assertSameTree(expected.get(key), actual.get(key), where + "." + key);
            }
        } else if (expected.isArray()) {
            assertTrue(actual.isArray(), where + " debería ser una lista");
            assertEquals(expected.size(), actual.size(), "el largo de " + where);
            Iterator<JsonNode> e = expected.elements();
            Iterator<JsonNode> a = actual.elements();
            for (int i = 0; e.hasNext(); i++) {
                assertSameTree(e.next(), a.next(), where + "[" + i + "]");
            }
        } else {
            assertEquals(expected, actual, where);
        }
    }

    @Test
    @DisplayName("should write the publication list with exactly the keys, the order and the values of the shared golden")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R001")
    @Tag("F-PUBPROD-R002")
    public void shouldWriteThePublicationListWithExactlyTheKeysTheOrderAndTheValuesOfTheSharedGolden()
            throws IOException {
        JsonNode written = mapper.readTree(mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsBytes(theGoldenList()));

        assertSameTree(golden(), written, "la lista");
        assertEquals(List.of("schema", "batchId", "planId", "level", "publishedAt", "approvedBy", "source", "verdict",
                "reviews", "documents", "unsupported", "excluded", "counts"), keys(written));
        assertTrue(written.get("documents").get(2).get("fields").get(1).get("after").get(1).get("incidence")
                .isDouble(), "la incidencia va como número");
        assertEquals("", written.get("documents").get(1).get("fields").get(0).get("after").get(1).get("text")
                .asText(), "el texto vacío va como texto vacío");
    }

    @Test
    @DisplayName("should read the shared golden back into the same list and write it again unchanged")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R001")
    public void shouldReadTheSharedGoldenBackIntoTheSameListAndWriteItAgainUnchanged() throws IOException {
        PublicationManifest read;
        try (InputStream in = PublicationManifestContractTest.class.getResourceAsStream(GOLDEN)) {
            read = mapper.readValue(in, PublicationManifest.class);
        }

        assertEquals(theGoldenList(), read);
        assertSameTree(golden(), mapper.readTree(mapper.writeValueAsBytes(read)), "la lista releída");
    }

    @Test
    @DisplayName("should only know the two collections of production, the two review states of a document and the four reasons a change is left out")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R001")
    @Tag("F-PUBPROD-R002")
    public void shouldOnlyKnowTheTwoCollectionsTheTwoReviewStatesAndTheFourReasons() throws IOException {
        assertEquals("quizTemplates", ProductionFieldMap.QUIZ_COLLECTION);
        assertEquals("knowledges", ProductionFieldMap.KNOWLEDGE_COLLECTION);
        assertEquals(List.of("APPROVED", "PENDING"),
                Arrays.stream(DocumentReviewStatus.values()).map(Enum::name).toList(),
                "un documento rechazado nunca está en la lista");
        assertEquals(List.of("REVIEW_REJECTED", "COLLATERAL", "RULE_EXCLUDED", "DROPPED_BY_JOSE"),
                Arrays.stream(ExclusionReason.values()).map(Enum::name).toList());
        for (JsonNode document : golden().get("documents")) {
            assertTrue(Set.of("quizTemplates", "knowledges").contains(document.get("collection").asText()));
            assertTrue(Set.of("APPROVED", "PENDING").contains(document.get("reviewStatus").asText()));
        }
    }

    @Test
    @DisplayName("should keep the shared golden publishable: verdict «pasa» with no undecidable floor, counts that add up against its documents, a multiple choice that carries only the text of its options among its form, and what does not travel declared apart")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R001")
    @Tag("F-PUBPROD-R002")
    @Tag("F-PUBPROD-R005")
    public void shouldKeepTheSharedGoldenPublishable() throws IOException {
        JsonNode golden = golden();

        assertEquals("PASSES", golden.get("verdict").get("kind").asText());
        assertTrue(golden.get("verdict").get("gatePassed").asBoolean());
        assertTrue(golden.get("verdict").get("undecidableFloors").isArray()
                && golden.get("verdict").get("undecidableFloors").isEmpty(), "sólo se publica con «pasa»: []");
        int fields = 0;
        Map<String, Integer> byPath = new LinkedHashMap<>();
        for (JsonNode document : golden.get("documents")) {
            List<String> formPaths = new ArrayList<>();
            for (JsonNode field : document.get("fields")) {
                if (field.get("path").asText().startsWith("form.")) {
                    formPaths.add(field.get("path").asText());
                }
            }
            if (formPaths.contains("form.items")) {
                assertEquals(List.of("form.items"), formPaths,
                        "de una opción múltiple sólo viaja el texto de sus opciones, nada más de su formulario");
            }
            for (JsonNode field : document.get("fields")) {
                fields++;
                byPath.merge(field.get("path").asText(), 1, Integer::sum);
                if ("form.selection".equals(field.get("path").asText())) {
                    throw new AssertionError("la forma de elegir no se publica por script");
                }
                if ("form.items".equals(field.get("path").asText())) {
                    JsonNode before = field.get("before");
                    JsonNode after = field.get("after");
                    assertEquals(before.size(), after.size(), "la misma cantidad de opciones");
                    for (int i = 0; i < before.size(); i++) {
                        assertEquals(before.get(i).get("id"), after.get(i).get("id"), "el mismo identificador");
                        assertEquals(before.get(i).get("incidence"), after.get(i).get("incidence"),
                                "la misma correcta");
                    }
                }
            }
        }
        for (JsonNode held : golden.get("unsupported")) {
            assertTrue(held.get("reason").asText().endsWith("no se publica por script"), held.toString());
        }
        JsonNode counts = golden.get("counts");
        assertEquals(golden.get("documents").size(), counts.get("documents").asInt());
        assertEquals(fields, counts.get("fields").asInt());
        Map<String, Integer> written = new LinkedHashMap<>();
        counts.get("byPath").fields().forEachRemaining(e -> written.put(e.getKey(), e.getValue().asInt()));
        assertEquals(byPath, written);
    }

    private static List<String> keys(JsonNode node) {
        List<String> keys = new ArrayList<>();
        node.fieldNames().forEachRemaining(keys::add);
        return keys;
    }
}
