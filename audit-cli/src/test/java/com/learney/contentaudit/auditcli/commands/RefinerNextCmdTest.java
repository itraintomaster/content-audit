package com.learney.contentaudit.auditcli.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditReportStore;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.refinerdomain.CorrectionContextResolver;
import com.learney.contentaudit.refinerdomain.DiagnosisKind;
import com.learney.contentaudit.refinerdomain.RefinerEngine;
import com.learney.contentaudit.refinerdomain.RefinementPlan;
import com.learney.contentaudit.refinerdomain.RefinementPlanStore;
import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.refinerdomain.RefinementTaskStatus;
import com.learney.contentaudit.refinerdomain.LemmaAbsenceCorrectionContext;
import com.learney.contentaudit.refinerdomain.MisplacedLemmaContext;
import com.learney.contentaudit.refinerdomain.SentenceLengthCorrectionContext;
import com.learney.contentaudit.refinerdomain.SuggestedLemma;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class RefinerNextCmdTest {

    // ── Helper methods ──────────────────────────────────────────────────────

    private RefinerNextCmd buildCmd(RefinementPlanStore planStore, RefinerEngine refinerEngine,
            AuditReportStore reportStore, CorrectionContextResolver resolver, String format)
            throws Exception {
        RefinerNextCmd cmd = new RefinerNextCmd(planStore, refinerEngine, reportStore, resolver);
        Field formatField = RefinerNextCmd.class.getDeclaredField("formatName");
        formatField.setAccessible(true);
        formatField.set(cmd, format);
        return cmd;
    }

    private RefinementTask buildTask(String id, DiagnosisKind diagnosisKind) {
        return new RefinementTask(id, AuditTarget.QUIZ, "quiz-node-001", "Quiz 1 - L1.T1.K1",
                diagnosisKind, 1, RefinementTaskStatus.PENDING);
    }

    private RefinementPlan buildPlan(String sourceAuditId, RefinementTask task) {
        return new RefinementPlan("plan-001", sourceAuditId, Instant.now(), List.of(task));
    }

    @Test
    @DisplayName("should resolve correction context when task diagnosis is SENTENCE_LENGTH")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R006")
    @Tag("F-RCSL-J001")
    public void shouldResolveCorrectionContextWhenTaskDiagnosisIsSENTENCELENGTH() throws Exception {
        // Arrange
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-001", DiagnosisKind.SENTENCE_LENGTH);
        RefinementPlan plan = buildPlan("audit-001", task);

        AuditReport report = new AuditReport(null);
        SentenceLengthCorrectionContext context = new SentenceLengthCorrectionContext(
                "task-001", "She plays tennis every day.", "Ella juega tenis todos los dias.",
                "Present Simple Affirmative", "Write the affirmative form.", "Present Simple",
                CefrLevel.A1, 15, 5, 8, 7, List.of());

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));
        when(reportStore.load("audit-001")).thenReturn(Optional.of(report));
        when(resolver.resolve(report, task)).thenReturn(Optional.of(context));

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "text");

        // Act
        cmd.next("plan-001");

        // Assert — the resolver was called because the task is SENTENCE_LENGTH
        verify(resolver).resolve(report, task);
    }

    @Test
    @DisplayName("should not resolve correction context when task diagnosis is COCA_BUCKETS")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R006")
    public void shouldNotResolveCorrectionContextWhenTaskDiagnosisIsCOCABUCKETS() throws Exception {
        // Arrange
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-003", DiagnosisKind.COCA_BUCKETS);
        RefinementPlan plan = buildPlan("audit-001", task);

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "text");

        // Act
        cmd.next("plan-001");

        // Assert — neither the report store nor the resolver should be consulted for non-SENTENCE_LENGTH tasks
        verify(reportStore, never()).load(any());
        verify(resolver, never()).resolve(any(), any());
    }

    @Test
    @DisplayName("should include correctionContext object in JSON when context resolves successfully")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R007")
    @Tag("F-RCSL-J001")
    public void shouldIncludeCorrectionContextObjectInJSONWhenContextResolvesSuccessfully()
            throws Exception {
        // Arrange
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-014", DiagnosisKind.SENTENCE_LENGTH);
        RefinementPlan plan = buildPlan("audit-001", task);
        AuditReport report = new AuditReport(null);
        // tokenCount=15, targetMin=5, targetMax=8, delta=7 (15 exceeds max 8 by 7)
        SentenceLengthCorrectionContext context = new SentenceLengthCorrectionContext(
                "task-014",
                "She plays tennis every afternoon with her friends",
                "Ella juega tenis todas las tardes con sus amigas",
                "Affirmative sentences in the present simple",
                "Escribe la forma afirmativa",
                "Present Simple",
                CefrLevel.A1, 15, 5, 8, 7, List.of());

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));
        when(reportStore.load("audit-001")).thenReturn(Optional.of(report));
        when(resolver.resolve(report, task)).thenReturn(Optional.of(context));

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "json");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            // Act
            cmd.next("plan-001");
        } finally {
            System.setOut(originalOut);
        }

        // Assert — correctionContext must be a non-null JSON object; no error field present
        JsonNode root = new ObjectMapper().readTree(outContent.toString());
        assertTrue(root.has("correctionContext"),
                "JSON output must contain a correctionContext field");
        assertFalse(root.get("correctionContext").isNull(),
                "correctionContext must not be null when context resolves successfully");
        assertTrue(root.get("correctionContext").isObject(),
                "correctionContext must be a JSON object");
        assertFalse(root.has("correctionContextError"),
                "correctionContextError must not be present when context resolves successfully");
    }

    @Test
    @DisplayName("should include sentence translation knowledgeTitle knowledgeInstructions topicLabel and cefrLevel in JSON correctionContext")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R007")
    public void shouldIncludeSentenceTranslationKnowledgeTitleKnowledgeInstructionsTopicLabelAndCefrLevelInJSONCorrectionContext(
            ) throws Exception {
        // Arrange
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-014", DiagnosisKind.SENTENCE_LENGTH);
        RefinementPlan plan = buildPlan("audit-001", task);
        AuditReport report = new AuditReport(null);
        SentenceLengthCorrectionContext context = new SentenceLengthCorrectionContext(
                "task-014",
                "She plays tennis every afternoon with her friends",
                "Ella juega tenis todas las tardes con sus amigas",
                "Affirmative sentences in the present simple",
                "Escribe la forma afirmativa",
                "Present Simple",
                CefrLevel.A1, 15, 5, 8, 7, List.of());

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));
        when(reportStore.load("audit-001")).thenReturn(Optional.of(report));
        when(resolver.resolve(report, task)).thenReturn(Optional.of(context));

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "json");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            // Act
            cmd.next("plan-001");
        } finally {
            System.setOut(originalOut);
        }

        // Assert — all string fields from R001/R007 are present in correctionContext
        JsonNode ctx = new ObjectMapper().readTree(outContent.toString()).get("correctionContext");
        assertEquals("She plays tennis every afternoon with her friends",
                ctx.get("sentence").asText());
        assertEquals("Ella juega tenis todas las tardes con sus amigas",
                ctx.get("translation").asText());
        assertEquals("Affirmative sentences in the present simple",
                ctx.get("knowledgeTitle").asText());
        assertEquals("Escribe la forma afirmativa",
                ctx.get("knowledgeInstructions").asText());
        assertEquals("Present Simple", ctx.get("topicLabel").asText());
        assertEquals("A1", ctx.get("cefrLevel").asText());
    }

    @Test
    @DisplayName("should include targetRange with min and max and delta in JSON correctionContext")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R007")
    public void shouldIncludeTargetRangeWithMinAndMaxAndDeltaInJSONCorrectionContext() throws Exception {
        // Arrange
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-014", DiagnosisKind.SENTENCE_LENGTH);
        RefinementPlan plan = buildPlan("audit-001", task);
        AuditReport report = new AuditReport(null);
        // tokenCount=15, targetMin=5, targetMax=8, delta=7: sentence has 15 tokens, max is 8, so 7 above
        SentenceLengthCorrectionContext context = new SentenceLengthCorrectionContext(
                "task-014",
                "She plays tennis every afternoon with her friends",
                "Ella juega tenis todas las tardes con sus amigas",
                "Affirmative sentences in the present simple",
                "Escribe la forma afirmativa",
                "Present Simple",
                CefrLevel.A1, 15, 5, 8, 7, List.of());

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));
        when(reportStore.load("audit-001")).thenReturn(Optional.of(report));
        when(resolver.resolve(report, task)).thenReturn(Optional.of(context));

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "json");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            // Act
            cmd.next("plan-001");
        } finally {
            System.setOut(originalOut);
        }

        // Assert — tokenCount, delta, and nested targetRange{min,max} match the fixture values
        JsonNode ctx = new ObjectMapper().readTree(outContent.toString()).get("correctionContext");
        assertEquals(15, ctx.get("tokenCount").asInt());
        assertEquals(7, ctx.get("delta").asInt());
        JsonNode targetRange = ctx.get("targetRange");
        assertNotNull(targetRange, "targetRange must be present in correctionContext");
        assertTrue(targetRange.isObject(), "targetRange must be a JSON object");
        assertEquals(5, targetRange.get("min").asInt());
        assertEquals(8, targetRange.get("max").asInt());
    }

    @Test
    @DisplayName("should include suggestedLemmas array with lemma pos reason and cocaRank in JSON correctionContext")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R007")
    public void shouldIncludeSuggestedLemmasArrayWithLemmaPosReasonAndCocaRankInJSONCorrectionContext(
            ) throws Exception {
        // Arrange
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-014", DiagnosisKind.SENTENCE_LENGTH);
        RefinementPlan plan = buildPlan("audit-001", task);
        AuditReport report = new AuditReport(null);
        List<SuggestedLemma> lemmas = List.of(
                new SuggestedLemma("like", "VERB", "COMPLETELY_ABSENT", 52),
                new SuggestedLemma("want", "VERB", "APPEARS_TOO_LATE", 89),
                new SuggestedLemma("big", "ADJ", "COMPLETELY_ABSENT", 201));
        SentenceLengthCorrectionContext context = new SentenceLengthCorrectionContext(
                "task-014",
                "She plays tennis every afternoon with her friends",
                "Ella juega tenis todas las tardes con sus amigas",
                "Affirmative sentences in the present simple",
                "Escribe la forma afirmativa",
                "Present Simple",
                CefrLevel.A1, 15, 5, 8, 7, lemmas);

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));
        when(reportStore.load("audit-001")).thenReturn(Optional.of(report));
        when(resolver.resolve(report, task)).thenReturn(Optional.of(context));

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "json");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            // Act
            cmd.next("plan-001");
        } finally {
            System.setOut(originalOut);
        }

        // Assert — suggestedLemmas array has 3 elements matching the R007 example exactly
        JsonNode suggestedLemmas = new ObjectMapper().readTree(outContent.toString())
                .get("correctionContext").get("suggestedLemmas");
        assertNotNull(suggestedLemmas, "suggestedLemmas must be present");
        assertTrue(suggestedLemmas.isArray(), "suggestedLemmas must be a JSON array");
        assertEquals(3, suggestedLemmas.size());

        JsonNode first = suggestedLemmas.get(0);
        assertEquals("like", first.get("lemma").asText());
        assertEquals("VERB", first.get("pos").asText());
        assertEquals("COMPLETELY_ABSENT", first.get("reason").asText());
        assertEquals(52, first.get("cocaRank").asInt());

        JsonNode second = suggestedLemmas.get(1);
        assertEquals("want", second.get("lemma").asText());
        assertEquals("VERB", second.get("pos").asText());
        assertEquals("APPEARS_TOO_LATE", second.get("reason").asText());
        assertEquals(89, second.get("cocaRank").asInt());

        JsonNode third = suggestedLemmas.get(2);
        assertEquals("big", third.get("lemma").asText());
        assertEquals("ADJ", third.get("pos").asText());
        assertEquals("COMPLETELY_ABSENT", third.get("reason").asText());
        assertEquals(201, third.get("cocaRank").asInt());
    }

    @Test
    @DisplayName("should set correctionContext to null and include correctionContextError when audit report not found")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R007")
    @Tag("F-RCSL-J001")
    public void shouldSetCorrectionContextToNullAndIncludeCorrectionContextErrorWhenAuditReportNotFound(
            ) throws Exception {
        // Arrange
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-014", DiagnosisKind.SENTENCE_LENGTH);
        // Plan refers to "audit-missing" which does not exist in the store
        RefinementPlan plan = buildPlan("audit-missing", task);

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));
        when(reportStore.load("audit-missing")).thenReturn(Optional.empty());

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "json");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            // Act
            cmd.next("plan-001");
        } finally {
            System.setOut(originalOut);
        }

        // Assert — correctionContext is null and correctionContextError carries the reason
        JsonNode root = new ObjectMapper().readTree(outContent.toString());
        assertTrue(root.has("correctionContext"),
                "correctionContext field must be present even when null");
        assertTrue(root.get("correctionContext").isNull(),
                "correctionContext must be null when audit report is not found");
        assertTrue(root.has("correctionContextError"),
                "correctionContextError must be present when audit report is not found");
        assertFalse(root.get("correctionContextError").asText().isBlank(),
                "correctionContextError message must not be blank");
    }

    @Test
    @DisplayName("should set correctionContext to null and include correctionContextError when sourceAuditId is blank")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R007")
    public void shouldSetCorrectionContextToNullAndIncludeCorrectionContextErrorWhenSourceAuditIdIsBlank(
            ) throws Exception {
        // Arrange
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-014", DiagnosisKind.SENTENCE_LENGTH);
        // Plan with blank sourceAuditId — no audit report can be loaded
        RefinementPlan plan = buildPlan("", task);

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "json");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            // Act
            cmd.next("plan-001");
        } finally {
            System.setOut(originalOut);
        }

        // Assert — correctionContext is null and correctionContextError explains the blank sourceAuditId
        JsonNode root = new ObjectMapper().readTree(outContent.toString());
        assertTrue(root.has("correctionContext"),
                "correctionContext field must be present even when null");
        assertTrue(root.get("correctionContext").isNull(),
                "correctionContext must be null when sourceAuditId is blank");
        assertTrue(root.has("correctionContextError"),
                "correctionContextError must be present when sourceAuditId is blank");
        assertFalse(root.get("correctionContextError").asText().isBlank(),
                "correctionContextError message must not be blank");
    }

    @Test
    @DisplayName("should set correctionContext to null and include correctionContextError when resolver returns empty")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R007")
    public void shouldSetCorrectionContextToNullAndIncludeCorrectionContextErrorWhenResolverReturnsEmpty(
            ) throws Exception {
        // Arrange
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-014", DiagnosisKind.SENTENCE_LENGTH);
        RefinementPlan plan = buildPlan("audit-001", task);
        AuditReport report = new AuditReport(null);

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));
        when(reportStore.load("audit-001")).thenReturn(Optional.of(report));
        // Resolver cannot build the context for this task
        when(resolver.resolve(report, task)).thenReturn(Optional.empty());

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "json");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            // Act
            cmd.next("plan-001");
        } finally {
            System.setOut(originalOut);
        }

        // Assert — correctionContext is null and correctionContextError carries the reason
        JsonNode root = new ObjectMapper().readTree(outContent.toString());
        assertTrue(root.has("correctionContext"),
                "correctionContext field must be present even when null");
        assertTrue(root.get("correctionContext").isNull(),
                "correctionContext must be null when resolver returns empty");
        assertTrue(root.has("correctionContextError"),
                "correctionContextError must be present when resolver returns empty");
        assertFalse(root.get("correctionContextError").asText().isBlank(),
                "correctionContextError message must not be blank");
    }

    @Test
    @DisplayName("should output empty suggestedLemmas array in JSON when context has no suggested lemmas")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R007")
    @Tag("F-RCSL-J003")
    public void shouldOutputEmptySuggestedLemmasArrayInJSONWhenContextHasNoSuggestedLemmas()
            throws Exception {
        // Arrange
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-014", DiagnosisKind.SENTENCE_LENGTH);
        RefinementPlan plan = buildPlan("audit-001", task);
        AuditReport report = new AuditReport(null);
        // Context with empty suggested lemmas list (R004 scenario: no lemmas available)
        SentenceLengthCorrectionContext context = new SentenceLengthCorrectionContext(
                "task-014",
                "She plays tennis every afternoon with her friends",
                "Ella juega tenis todas las tardes con sus amigas",
                "Affirmative sentences in the present simple",
                "Escribe la forma afirmativa",
                "Present Simple",
                CefrLevel.A1, 15, 5, 8, 7, List.of());

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));
        when(reportStore.load("audit-001")).thenReturn(Optional.of(report));
        when(resolver.resolve(report, task)).thenReturn(Optional.of(context));

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "json");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            // Act
            cmd.next("plan-001");
        } finally {
            System.setOut(originalOut);
        }

        // Assert — suggestedLemmas is an empty array, not null or absent
        JsonNode suggestedLemmas = new ObjectMapper().readTree(outContent.toString())
                .get("correctionContext").get("suggestedLemmas");
        assertNotNull(suggestedLemmas, "suggestedLemmas must be present in correctionContext");
        assertTrue(suggestedLemmas.isArray(), "suggestedLemmas must be a JSON array");
        assertEquals(0, suggestedLemmas.size(),
                "suggestedLemmas array must be empty when context has no suggested lemmas");
    }

    @Test
    @DisplayName("should print correction context section with all fields in text format")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R008")
    @Tag("F-RCSL-J001")
    public void shouldPrintCorrectionContextSectionWithAllFieldsInTextFormat() throws Exception {
        // Arrange
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-014", DiagnosisKind.SENTENCE_LENGTH);
        RefinementPlan plan = buildPlan("audit-001", task);
        AuditReport report = new AuditReport(null);

        SentenceLengthCorrectionContext context = new SentenceLengthCorrectionContext(
                "task-014",
                "She plays tennis every afternoon with her friends",
                "Ella juega tenis todas las tardes con sus amigas",
                "Affirmative sentences in the present simple",
                "Escribe la forma afirmativa",
                "Present Simple",
                CefrLevel.A1,
                15, 5, 8, 7,
                List.of());

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));
        when(reportStore.load("audit-001")).thenReturn(Optional.of(report));
        when(resolver.resolve(report, task)).thenReturn(Optional.of(context));

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "text");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            // Act
            cmd.next("plan-001");
        } finally {
            System.setOut(originalOut);
        }

        // Assert — all labeled fields from R008 format example are present
        String output = outContent.toString();
        assertTrue(output.contains("Correction context:"), "output missing 'Correction context:' header");
        assertTrue(output.contains("Sentence:"), "output missing 'Sentence:' label");
        assertTrue(output.contains("She plays tennis every afternoon with her friends"),
                "output missing sentence value");
        assertTrue(output.contains("Translation:"), "output missing 'Translation:' label");
        assertTrue(output.contains("Ella juega tenis todas las tardes con sus amigas"),
                "output missing translation value");
        assertTrue(output.contains("Knowledge:"), "output missing 'Knowledge:' label");
        assertTrue(output.contains("Affirmative sentences in the present simple"),
                "output missing knowledgeTitle value");
        assertTrue(output.contains("Instructions:"), "output missing 'Instructions:' label");
        assertTrue(output.contains("Escribe la forma afirmativa"), "output missing instructions value");
        assertTrue(output.contains("Topic:"), "output missing 'Topic:' label");
        assertTrue(output.contains("Present Simple"), "output missing topicLabel value");
        assertTrue(output.contains("CEFR Level:"), "output missing 'CEFR Level:' label");
        assertTrue(output.contains("A1"), "output missing cefrLevel value");
        assertTrue(output.contains("Tokens:"), "output missing 'Tokens:' label");
        assertTrue(output.contains("15 (target: 5-8, delta: +7)"), "output missing tokens line");
        assertTrue(output.contains("Suggested lemmas:"), "output missing 'Suggested lemmas:' label");
    }

    @Test
    @DisplayName("should print numbered suggested lemmas list with lemma pos reason and COCA rank in text format")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R008")
    public void shouldPrintNumberedSuggestedLemmasListWithLemmaPosReasonAndCOCARankInTextFormat() throws Exception {
        // Arrange
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-014", DiagnosisKind.SENTENCE_LENGTH);
        RefinementPlan plan = buildPlan("audit-001", task);
        AuditReport report = new AuditReport(null);

        List<SuggestedLemma> lemmas = List.of(
                new SuggestedLemma("like", "VERB", "COMPLETELY_ABSENT", 52),
                new SuggestedLemma("want", "VERB", "APPEARS_TOO_LATE", 89),
                new SuggestedLemma("big", "ADJ", "COMPLETELY_ABSENT", 201));

        SentenceLengthCorrectionContext context = new SentenceLengthCorrectionContext(
                "task-014",
                "She plays tennis every afternoon with her friends",
                "Ella juega tenis todas las tardes con sus amigas",
                "Affirmative sentences in the present simple",
                "Escribe la forma afirmativa",
                "Present Simple",
                CefrLevel.A1,
                15, 5, 8, 7,
                lemmas);

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));
        when(reportStore.load("audit-001")).thenReturn(Optional.of(report));
        when(resolver.resolve(report, task)).thenReturn(Optional.of(context));

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "text");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            // Act
            cmd.next("plan-001");
        } finally {
            System.setOut(originalOut);
        }

        // Assert — each lemma appears numbered with lemma, POS, reason, and COCA rank
        String output = outContent.toString();
        assertTrue(output.contains("1. like (VERB) - COMPLETELY_ABSENT [COCA #52]"),
                "output missing first numbered lemma entry");
        assertTrue(output.contains("2. want (VERB) - APPEARS_TOO_LATE [COCA #89]"),
                "output missing second numbered lemma entry");
        assertTrue(output.contains("3. big (ADJ) - COMPLETELY_ABSENT [COCA #201]"),
                "output missing third numbered lemma entry");
    }

    @Test
    @DisplayName("should print none available for suggested lemmas when list is empty in text format")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R008")
    @Tag("F-RCSL-J003")
    public void shouldPrintNoneAvailableForSuggestedLemmasWhenListIsEmptyInTextFormat() throws Exception {
        // Arrange
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-014", DiagnosisKind.SENTENCE_LENGTH);
        RefinementPlan plan = buildPlan("audit-001", task);
        AuditReport report = new AuditReport(null);

        SentenceLengthCorrectionContext context = new SentenceLengthCorrectionContext(
                "task-014",
                "She plays tennis every afternoon with her friends",
                "Ella juega tenis todas las tardes con sus amigas",
                "Affirmative sentences in the present simple",
                "Escribe la forma afirmativa",
                "Present Simple",
                CefrLevel.A1,
                15, 5, 8, 7,
                List.of());

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));
        when(reportStore.load("audit-001")).thenReturn(Optional.of(report));
        when(resolver.resolve(report, task)).thenReturn(Optional.of(context));

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "text");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            // Act
            cmd.next("plan-001");
        } finally {
            System.setOut(originalOut);
        }

        // Assert — "(none available)" appears under "Suggested lemmas:" when list is empty
        String output = outContent.toString();
        assertTrue(output.contains("Suggested lemmas:"), "output missing 'Suggested lemmas:' label");
        assertTrue(output.contains("(none available)"),
                "output should show '(none available)' when suggested lemmas list is empty");
    }

    @Test
    @DisplayName("should format positive delta with plus sign in text output tokens line")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R008")
    @Tag("F-RCSL-J001")
    public void shouldFormatPositiveDeltaWithPlusSignInTextOutputTokensLine() throws Exception {
        // Arrange — tokenCount=15, range=[5,8], delta=+7 (sentence is 7 tokens above maximum)
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-014", DiagnosisKind.SENTENCE_LENGTH);
        RefinementPlan plan = buildPlan("audit-001", task);
        AuditReport report = new AuditReport(null);

        SentenceLengthCorrectionContext context = new SentenceLengthCorrectionContext(
                "task-014",
                "She plays tennis every afternoon with her friends",
                "Ella juega tenis todas las tardes con sus amigas",
                "Affirmative sentences in the present simple",
                "Escribe la forma afirmativa",
                "Present Simple",
                CefrLevel.A1,
                15, 5, 8, 7,
                List.of());

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));
        when(reportStore.load("audit-001")).thenReturn(Optional.of(report));
        when(resolver.resolve(report, task)).thenReturn(Optional.of(context));

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "text");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            // Act
            cmd.next("plan-001");
        } finally {
            System.setOut(originalOut);
        }

        // Assert — positive delta (7) is prefixed with "+" in the tokens line
        String output = outContent.toString();
        assertTrue(output.contains("delta: +7"),
                "positive delta must be rendered with a '+' prefix, but got: " + output);
    }

    @Test
    @DisplayName("should format negative delta without plus sign in text output tokens line")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R008")
    @Tag("F-RCSL-J002")
    public void shouldFormatNegativeDeltaWithoutPlusSignInTextOutputTokensLine() throws Exception {
        // Arrange — tokenCount=5, range=[8,12], delta=-3 (sentence is 3 tokens below minimum)
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-014", DiagnosisKind.SENTENCE_LENGTH);
        RefinementPlan plan = buildPlan("audit-001", task);
        AuditReport report = new AuditReport(null);

        SentenceLengthCorrectionContext context = new SentenceLengthCorrectionContext(
                "task-014",
                "She plays",
                "Ella juega",
                "Affirmative sentences in the present simple",
                "Escribe la forma afirmativa",
                "Present Simple",
                CefrLevel.A1,
                5, 8, 12, -3,
                List.of());

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));
        when(reportStore.load("audit-001")).thenReturn(Optional.of(report));
        when(resolver.resolve(report, task)).thenReturn(Optional.of(context));

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "text");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            // Act
            cmd.next("plan-001");
        } finally {
            System.setOut(originalOut);
        }

        // Assert — negative delta (-3) is rendered with "-" but without "+" prefix
        String output = outContent.toString();
        assertTrue(output.contains("delta: -3"),
                "negative delta must be rendered without a '+' prefix, but got: " + output);
        assertFalse(output.contains("delta: +-3"),
                "negative delta must not have a '+' prefix, but got: " + output);
    }

    @Test
    @DisplayName("should print not available message with error reason when context cannot be built in text format")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R008")
    public void shouldPrintNotAvailableMessageWithErrorReasonWhenContextCannotBeBuiltInTextFormat() throws Exception {
        // Arrange — resolver returns empty so context cannot be built
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-014", DiagnosisKind.SENTENCE_LENGTH);
        RefinementPlan plan = buildPlan("audit-001", task);
        AuditReport report = new AuditReport(null);

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));
        when(reportStore.load("audit-001")).thenReturn(Optional.of(report));
        when(resolver.resolve(report, task)).thenReturn(Optional.empty());

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "text");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            // Act
            cmd.next("plan-001");
        } finally {
            System.setOut(originalOut);
        }

        // Assert — "not available" message with error reason is shown instead of context section
        String output = outContent.toString();
        assertTrue(output.contains("Correction context: not available"),
                "output should show 'Correction context: not available' when context cannot be built, but got: " + output);
        assertTrue(output.contains("context could not be resolved for task task-014"),
                "output should include the error reason, but got: " + output);
    }

    @Test
    @DisplayName("should print correction context section in table format for SENTENCE_LENGTH task")
    @Tag("FEAT-RCSL")
    @Tag("F-RCSL-R008")
    public void shouldPrintCorrectionContextSectionInTableFormatForSENTENCELENGTHTask() throws Exception {
        // Arrange — format is "table"; the correction context section should still appear
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-014", DiagnosisKind.SENTENCE_LENGTH);
        RefinementPlan plan = buildPlan("audit-001", task);
        AuditReport report = new AuditReport(null);

        SentenceLengthCorrectionContext context = new SentenceLengthCorrectionContext(
                "task-014",
                "She plays tennis every afternoon with her friends",
                "Ella juega tenis todas las tardes con sus amigas",
                "Affirmative sentences in the present simple",
                "Escribe la forma afirmativa",
                "Present Simple",
                CefrLevel.A1,
                15, 5, 8, 7,
                List.of());

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));
        when(reportStore.load("audit-001")).thenReturn(Optional.of(report));
        when(resolver.resolve(report, task)).thenReturn(Optional.of(context));

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "table");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            // Act
            cmd.next("plan-001");
        } finally {
            System.setOut(originalOut);
        }

        // Assert — table header is rendered and correction context section follows it
        String output = outContent.toString();
        assertTrue(output.contains("SENTENCE_LENGTH"),
                "table output should include diagnosis kind, but got: " + output);
        assertTrue(output.contains("Correction context:"),
                "table format should include correction context section, but got: " + output);
        assertTrue(output.contains("Sentence:"),
                "table format correction context should include 'Sentence:' label, but got: " + output);
        assertTrue(output.contains("She plays tennis every afternoon with her friends"),
                "table format correction context should include sentence value, but got: " + output);
        assertTrue(output.contains("15 (target: 5-8, delta: +7)"),
                "table format correction context should include tokens line, but got: " + output);
    }

    @Test
    @DisplayName("should resolve correction context when task diagnosis is LEMMA_ABSENCE")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R007")
    public void shouldResolveCorrectionContextWhenTaskDiagnosisIsLEMMAABSENCE() throws Exception {
        // Arrange
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-014", DiagnosisKind.LEMMA_ABSENCE);
        RefinementPlan plan = buildPlan("audit-001", task);
        AuditReport report = new AuditReport(null);

        List<MisplacedLemmaContext> misplaced = List.of(
                new MisplacedLemmaContext("negotiate", "VERB", CefrLevel.B2, CefrLevel.A1, 2840));
        LemmaAbsenceCorrectionContext context = new LemmaAbsenceCorrectionContext(
                "task-014",
                "She needs to negotiate the contract before Friday",
                "Ella necesita negociar el contrato antes del viernes",
                "Affirmative sentences in the present simple",
                "Escribe la forma afirmativa",
                "Present Simple",
                CefrLevel.A1,
                misplaced,
                List.of());

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));
        when(reportStore.load("audit-001")).thenReturn(Optional.of(report));
        when(resolver.resolve(report, task)).thenReturn(Optional.of(context));

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "text");

        // Act
        cmd.next("plan-001");

        // Assert — the resolver was called because the task is LEMMA_ABSENCE
        verify(resolver).resolve(report, task);
    }

    @Test
    @DisplayName("should include correctionContext with misplacedLemmas array in JSON for LEMMA_ABSENCE task")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R008")
    public void shouldIncludeCorrectionContextWithMisplacedLemmasArrayInJSONForLEMMAABSENCETask() throws Exception {
        // Arrange
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-014", DiagnosisKind.LEMMA_ABSENCE);
        RefinementPlan plan = buildPlan("audit-001", task);
        AuditReport report = new AuditReport(null);

        List<MisplacedLemmaContext> misplaced = List.of(
                new MisplacedLemmaContext("negotiate", "VERB", CefrLevel.B2, CefrLevel.A1, 2840),
                new MisplacedLemmaContext("contract", "NOUN", CefrLevel.B1, CefrLevel.A1, 1205));
        LemmaAbsenceCorrectionContext context = new LemmaAbsenceCorrectionContext(
                "task-014",
                "She needs to negotiate the contract before Friday",
                "Ella necesita negociar el contrato antes del viernes",
                "Affirmative sentences in the present simple",
                "Escribe la forma afirmativa",
                "Present Simple",
                CefrLevel.A1,
                misplaced,
                List.of());

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));
        when(reportStore.load("audit-001")).thenReturn(Optional.of(report));
        when(resolver.resolve(report, task)).thenReturn(Optional.of(context));

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "json");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            // Act
            cmd.next("plan-001");
        } finally {
            System.setOut(originalOut);
        }

        // Assert — correctionContext is a non-null object and misplacedLemmas is a non-null array
        JsonNode root = new ObjectMapper().readTree(outContent.toString());
        assertTrue(root.has("correctionContext"), "JSON output must contain a correctionContext field");
        assertFalse(root.get("correctionContext").isNull(), "correctionContext must not be null");
        JsonNode misplacedLemmas = root.get("correctionContext").get("misplacedLemmas");
        assertNotNull(misplacedLemmas, "misplacedLemmas must be present in correctionContext");
        assertTrue(misplacedLemmas.isArray(), "misplacedLemmas must be a JSON array");
        assertEquals(2, misplacedLemmas.size(), "misplacedLemmas array must have 2 elements");
    }

    @Test
    @DisplayName("should include misplacedLemmas with lemma pos expectedLevel quizLevel and cocaRank in JSON correctionContext")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R008")
    public void shouldIncludeMisplacedLemmasWithLemmaPosExpectedLevelQuizLevelAndCocaRankInJSONCorrectionContext() throws Exception {
        // Arrange — two misplaced lemmas matching the R008 example exactly
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-014", DiagnosisKind.LEMMA_ABSENCE);
        RefinementPlan plan = buildPlan("audit-001", task);
        AuditReport report = new AuditReport(null);

        List<MisplacedLemmaContext> misplaced = List.of(
                new MisplacedLemmaContext("negotiate", "VERB", CefrLevel.B2, CefrLevel.A1, 2840),
                new MisplacedLemmaContext("contract", "NOUN", CefrLevel.B1, CefrLevel.A1, 1205));
        LemmaAbsenceCorrectionContext context = new LemmaAbsenceCorrectionContext(
                "task-014",
                "She needs to negotiate the contract before Friday",
                "Ella necesita negociar el contrato antes del viernes",
                "Affirmative sentences in the present simple",
                "Escribe la forma afirmativa",
                "Present Simple",
                CefrLevel.A1,
                misplaced,
                List.of());

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));
        when(reportStore.load("audit-001")).thenReturn(Optional.of(report));
        when(resolver.resolve(report, task)).thenReturn(Optional.of(context));

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "json");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            // Act
            cmd.next("plan-001");
        } finally {
            System.setOut(originalOut);
        }

        // Assert — each misplacedLemma entry has lemma, pos, expectedLevel, quizLevel, cocaRank
        JsonNode misplacedLemmas = new ObjectMapper().readTree(outContent.toString())
                .get("correctionContext").get("misplacedLemmas");

        JsonNode first = misplacedLemmas.get(0);
        assertEquals("negotiate", first.get("lemma").asText());
        assertEquals("VERB", first.get("pos").asText());
        assertEquals("B2", first.get("expectedLevel").asText());
        assertEquals("A1", first.get("quizLevel").asText());
        assertEquals(2840, first.get("cocaRank").asInt());

        JsonNode second = misplacedLemmas.get(1);
        assertEquals("contract", second.get("lemma").asText());
        assertEquals("NOUN", second.get("pos").asText());
        assertEquals("B1", second.get("expectedLevel").asText());
        assertEquals("A1", second.get("quizLevel").asText());
        assertEquals(1205, second.get("cocaRank").asInt());
    }

    @Test
    @DisplayName("should include suggestedLemmas array in JSON correctionContext for LEMMA_ABSENCE task")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R008")
    public void shouldIncludeSuggestedLemmasArrayInJSONCorrectionContextForLEMMAABSENCETask() throws Exception {
        // Arrange — three suggested lemmas matching the R008 example
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-014", DiagnosisKind.LEMMA_ABSENCE);
        RefinementPlan plan = buildPlan("audit-001", task);
        AuditReport report = new AuditReport(null);

        List<MisplacedLemmaContext> misplaced = List.of(
                new MisplacedLemmaContext("negotiate", "VERB", CefrLevel.B2, CefrLevel.A1, 2840));
        List<SuggestedLemma> suggested = List.of(
                new SuggestedLemma("like", "VERB", "COMPLETELY_ABSENT", 52),
                new SuggestedLemma("want", "VERB", "APPEARS_TOO_LATE", 89),
                new SuggestedLemma("big", "ADJ", "COMPLETELY_ABSENT", 201));
        LemmaAbsenceCorrectionContext context = new LemmaAbsenceCorrectionContext(
                "task-014",
                "She needs to negotiate the contract before Friday",
                "Ella necesita negociar el contrato antes del viernes",
                "Affirmative sentences in the present simple",
                "Escribe la forma afirmativa",
                "Present Simple",
                CefrLevel.A1,
                misplaced,
                suggested);

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));
        when(reportStore.load("audit-001")).thenReturn(Optional.of(report));
        when(resolver.resolve(report, task)).thenReturn(Optional.of(context));

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "json");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            // Act
            cmd.next("plan-001");
        } finally {
            System.setOut(originalOut);
        }

        // Assert — suggestedLemmas array has 3 elements with lemma, pos, reason, cocaRank
        JsonNode suggestedLemmas = new ObjectMapper().readTree(outContent.toString())
                .get("correctionContext").get("suggestedLemmas");
        assertNotNull(suggestedLemmas, "suggestedLemmas must be present in correctionContext");
        assertTrue(suggestedLemmas.isArray(), "suggestedLemmas must be a JSON array");
        assertEquals(3, suggestedLemmas.size());

        JsonNode first = suggestedLemmas.get(0);
        assertEquals("like", first.get("lemma").asText());
        assertEquals("VERB", first.get("pos").asText());
        assertEquals("COMPLETELY_ABSENT", first.get("reason").asText());
        assertEquals(52, first.get("cocaRank").asInt());

        JsonNode second = suggestedLemmas.get(1);
        assertEquals("want", second.get("lemma").asText());
        assertEquals("APPEARS_TOO_LATE", second.get("reason").asText());

        JsonNode third = suggestedLemmas.get(2);
        assertEquals("big", third.get("lemma").asText());
        assertEquals("ADJ", third.get("pos").asText());
        assertEquals(201, third.get("cocaRank").asInt());
    }

    @Test
    @DisplayName("should set correctionContext to null and include correctionContextError in JSON for LEMMA_ABSENCE when resolver returns empty")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R008")
    public void shouldSetCorrectionContextToNullAndIncludeCorrectionContextErrorInJSONForLEMMAABSENCEWhenResolverReturnsEmpty() throws Exception {
        // Arrange — resolver cannot build the LEMMA_ABSENCE context (e.g., no placement diagnosis)
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-014", DiagnosisKind.LEMMA_ABSENCE);
        RefinementPlan plan = buildPlan("audit-001", task);
        AuditReport report = new AuditReport(null);

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));
        when(reportStore.load("audit-001")).thenReturn(Optional.of(report));
        when(resolver.resolve(report, task)).thenReturn(Optional.empty());

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "json");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            // Act
            cmd.next("plan-001");
        } finally {
            System.setOut(originalOut);
        }

        // Assert — correctionContext is null and correctionContextError carries the reason
        JsonNode root = new ObjectMapper().readTree(outContent.toString());
        assertTrue(root.has("correctionContext"), "correctionContext field must be present even when null");
        assertTrue(root.get("correctionContext").isNull(),
                "correctionContext must be null when resolver returns empty for LEMMA_ABSENCE");
        assertTrue(root.has("correctionContextError"),
                "correctionContextError must be present when resolver returns empty");
        assertFalse(root.get("correctionContextError").asText().isBlank(),
                "correctionContextError message must not be blank");
    }

    @Test
    @DisplayName("should print correction context section with misplaced lemmas in text format for LEMMA_ABSENCE task")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R009")
    public void shouldPrintCorrectionContextSectionWithMisplacedLemmasInTextFormatForLEMMAABSENCETask() throws Exception {
        // Arrange — full correction context for a LEMMA_ABSENCE task
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-014", DiagnosisKind.LEMMA_ABSENCE);
        RefinementPlan plan = buildPlan("audit-001", task);
        AuditReport report = new AuditReport(null);

        List<MisplacedLemmaContext> misplaced = List.of(
                new MisplacedLemmaContext("negotiate", "VERB", CefrLevel.B2, CefrLevel.A1, 2840));
        LemmaAbsenceCorrectionContext context = new LemmaAbsenceCorrectionContext(
                "task-014",
                "She needs to negotiate the contract before Friday",
                "Ella necesita negociar el contrato antes del viernes",
                "Affirmative sentences in the present simple",
                "Escribe la forma afirmativa",
                "Present Simple",
                CefrLevel.A1,
                misplaced,
                List.of());

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));
        when(reportStore.load("audit-001")).thenReturn(Optional.of(report));
        when(resolver.resolve(report, task)).thenReturn(Optional.of(context));

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "text");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            // Act
            cmd.next("plan-001");
        } finally {
            System.setOut(originalOut);
        }

        // Assert — all labeled fields from R009 format example are present
        String output = outContent.toString();
        assertTrue(output.contains("Correction context:"), "output missing 'Correction context:' header");
        assertTrue(output.contains("Sentence:"), "output missing 'Sentence:' label");
        assertTrue(output.contains("She needs to negotiate the contract before Friday"),
                "output missing sentence value");
        assertTrue(output.contains("Translation:"), "output missing 'Translation:' label");
        assertTrue(output.contains("Ella necesita negociar el contrato antes del viernes"),
                "output missing translation value");
        assertTrue(output.contains("Knowledge:"), "output missing 'Knowledge:' label");
        assertTrue(output.contains("Affirmative sentences in the present simple"),
                "output missing knowledgeTitle value");
        assertTrue(output.contains("Instructions:"), "output missing 'Instructions:' label");
        assertTrue(output.contains("Escribe la forma afirmativa"), "output missing instructions value");
        assertTrue(output.contains("Topic:"), "output missing 'Topic:' label");
        assertTrue(output.contains("Present Simple"), "output missing topicLabel value");
        assertTrue(output.contains("CEFR Level:"), "output missing 'CEFR Level:' label");
        assertTrue(output.contains("A1"), "output missing cefrLevel value");
        assertTrue(output.contains("Misplaced lemmas:"), "output missing 'Misplaced lemmas:' label");
    }

    @Test
    @DisplayName("should print numbered misplaced lemmas list with expected and found levels in text format")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R009")
    public void shouldPrintNumberedMisplacedLemmasListWithExpectedAndFoundLevelsInTextFormat() throws Exception {
        // Arrange — two misplaced lemmas matching the R009 example exactly
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-014", DiagnosisKind.LEMMA_ABSENCE);
        RefinementPlan plan = buildPlan("audit-001", task);
        AuditReport report = new AuditReport(null);

        List<MisplacedLemmaContext> misplaced = List.of(
                new MisplacedLemmaContext("negotiate", "VERB", CefrLevel.B2, CefrLevel.A1, 2840),
                new MisplacedLemmaContext("contract", "NOUN", CefrLevel.B1, CefrLevel.A1, 1205));
        LemmaAbsenceCorrectionContext context = new LemmaAbsenceCorrectionContext(
                "task-014",
                "She needs to negotiate the contract before Friday",
                "Ella necesita negociar el contrato antes del viernes",
                "Affirmative sentences in the present simple",
                "Escribe la forma afirmativa",
                "Present Simple",
                CefrLevel.A1,
                misplaced,
                List.of());

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));
        when(reportStore.load("audit-001")).thenReturn(Optional.of(report));
        when(resolver.resolve(report, task)).thenReturn(Optional.of(context));

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "text");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            // Act
            cmd.next("plan-001");
        } finally {
            System.setOut(originalOut);
        }

        // Assert — each misplaced lemma appears numbered with lemma, POS, expectedLevel, quizLevel, COCA rank
        // R009 format: "1. negotiate (VERB) - expected B2, found in A1 [COCA #2840]"
        String output = outContent.toString();
        assertTrue(output.contains("1. negotiate (VERB) - expected B2, found in A1 [COCA #2840]"),
                "output missing first numbered misplaced lemma entry, but got: " + output);
        assertTrue(output.contains("2. contract (NOUN) - expected B1, found in A1 [COCA #1205]"),
                "output missing second numbered misplaced lemma entry, but got: " + output);
    }

    @Test
    @DisplayName("should print suggested replacements in text format for LEMMA_ABSENCE task")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R009")
    public void shouldPrintSuggestedReplacementsInTextFormatForLEMMAABSENCETask() throws Exception {
        // Arrange — three suggested replacements matching the R009 example exactly
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-014", DiagnosisKind.LEMMA_ABSENCE);
        RefinementPlan plan = buildPlan("audit-001", task);
        AuditReport report = new AuditReport(null);

        List<MisplacedLemmaContext> misplaced = List.of(
                new MisplacedLemmaContext("negotiate", "VERB", CefrLevel.B2, CefrLevel.A1, 2840));
        List<SuggestedLemma> suggested = List.of(
                new SuggestedLemma("like", "VERB", "COMPLETELY_ABSENT", 52),
                new SuggestedLemma("want", "VERB", "APPEARS_TOO_LATE", 89),
                new SuggestedLemma("big", "ADJ", "COMPLETELY_ABSENT", 201));
        LemmaAbsenceCorrectionContext context = new LemmaAbsenceCorrectionContext(
                "task-014",
                "She needs to negotiate the contract before Friday",
                "Ella necesita negociar el contrato antes del viernes",
                "Affirmative sentences in the present simple",
                "Escribe la forma afirmativa",
                "Present Simple",
                CefrLevel.A1,
                misplaced,
                suggested);

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));
        when(reportStore.load("audit-001")).thenReturn(Optional.of(report));
        when(resolver.resolve(report, task)).thenReturn(Optional.of(context));

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "text");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            // Act
            cmd.next("plan-001");
        } finally {
            System.setOut(originalOut);
        }

        // Assert — "Suggested replacements:" section with numbered entries per R009 format
        // R009 format: "1. like (VERB) - COMPLETELY_ABSENT [COCA #52]"
        String output = outContent.toString();
        assertTrue(output.contains("Suggested replacements:"), "output missing 'Suggested replacements:' label");
        assertTrue(output.contains("1. like (VERB) - COMPLETELY_ABSENT [COCA #52]"),
                "output missing first suggested replacement entry, but got: " + output);
        assertTrue(output.contains("2. want (VERB) - APPEARS_TOO_LATE [COCA #89]"),
                "output missing second suggested replacement entry, but got: " + output);
        assertTrue(output.contains("3. big (ADJ) - COMPLETELY_ABSENT [COCA #201]"),
                "output missing third suggested replacement entry, but got: " + output);
    }

    @Test
    @DisplayName("should print none available for suggested replacements when list is empty in text format for LEMMA_ABSENCE")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R009")
    public void shouldPrintNoneAvailableForSuggestedReplacementsWhenListIsEmptyInTextFormatForLEMMAABSENCE() throws Exception {
        // Arrange — context with no suggested replacements (R004c scenario)
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-014", DiagnosisKind.LEMMA_ABSENCE);
        RefinementPlan plan = buildPlan("audit-001", task);
        AuditReport report = new AuditReport(null);

        List<MisplacedLemmaContext> misplaced = List.of(
                new MisplacedLemmaContext("negotiate", "VERB", CefrLevel.B2, CefrLevel.A1, 2840));
        LemmaAbsenceCorrectionContext context = new LemmaAbsenceCorrectionContext(
                "task-014",
                "She needs to negotiate the contract before Friday",
                "Ella necesita negociar el contrato antes del viernes",
                "Affirmative sentences in the present simple",
                "Escribe la forma afirmativa",
                "Present Simple",
                CefrLevel.A1,
                misplaced,
                List.of());

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));
        when(reportStore.load("audit-001")).thenReturn(Optional.of(report));
        when(resolver.resolve(report, task)).thenReturn(Optional.of(context));

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "text");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            // Act
            cmd.next("plan-001");
        } finally {
            System.setOut(originalOut);
        }

        // Assert — "(none available)" appears under "Suggested replacements:" when list is empty
        String output = outContent.toString();
        assertTrue(output.contains("Suggested replacements:"), "output missing 'Suggested replacements:' label");
        assertTrue(output.contains("(none available)"),
                "output should show '(none available)' when suggested replacements list is empty, but got: " + output);
    }

    @Test
    @DisplayName("should print not available message when LEMMA_ABSENCE context cannot be built in text format")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R009")
    public void shouldPrintNotAvailableMessageWhenLEMMAABSENCEContextCannotBeBuiltInTextFormat() throws Exception {
        // Arrange — resolver returns empty (e.g., placement diagnosis not available per R006)
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-014", DiagnosisKind.LEMMA_ABSENCE);
        RefinementPlan plan = buildPlan("audit-001", task);
        AuditReport report = new AuditReport(null);

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));
        when(reportStore.load("audit-001")).thenReturn(Optional.of(report));
        when(resolver.resolve(report, task)).thenReturn(Optional.empty());

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "text");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            // Act
            cmd.next("plan-001");
        } finally {
            System.setOut(originalOut);
        }

        // Assert — "not available" message is shown instead of context section
        String output = outContent.toString();
        assertTrue(output.contains("Correction context: not available"),
                "output should show 'Correction context: not available' when context cannot be built, but got: " + output);
        assertTrue(output.contains("context could not be resolved for task task-014"),
                "output should include the error reason with the task id, but got: " + output);
    }

    @Test
    @DisplayName("should include sentence translation knowledgeTitle knowledgeInstructions topicLabel and cefrLevel in JSON correctionContext for LEMMA_ABSENCE")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R008")
    public void shouldIncludeSentenceTranslationKnowledgeTitleKnowledgeInstructionsTopicLabelAndCefrLevelInJSONCorrectionContextForLEMMAABSENCE() throws Exception {
        // Arrange — full context matching the R008 example for LEMMA_ABSENCE
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        RefinerEngine refinerEngine = mock(RefinerEngine.class);
        AuditReportStore reportStore = mock(AuditReportStore.class);
        CorrectionContextResolver resolver = mock(CorrectionContextResolver.class);

        RefinementTask task = buildTask("task-014", DiagnosisKind.LEMMA_ABSENCE);
        RefinementPlan plan = buildPlan("audit-001", task);
        AuditReport report = new AuditReport(null);

        List<MisplacedLemmaContext> misplaced = List.of(
                new MisplacedLemmaContext("negotiate", "VERB", CefrLevel.B2, CefrLevel.A1, 2840));
        LemmaAbsenceCorrectionContext context = new LemmaAbsenceCorrectionContext(
                "task-014",
                "She needs to negotiate the contract before Friday",
                "Ella necesita negociar el contrato antes del viernes",
                "Affirmative sentences in the present simple",
                "Escribe la forma afirmativa",
                "Present Simple",
                CefrLevel.A1,
                misplaced,
                List.of());

        when(planStore.load("plan-001")).thenReturn(Optional.of(plan));
        when(reportStore.load("audit-001")).thenReturn(Optional.of(report));
        when(resolver.resolve(report, task)).thenReturn(Optional.of(context));

        RefinerNextCmd cmd = buildCmd(planStore, refinerEngine, reportStore, resolver, "json");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            // Act
            cmd.next("plan-001");
        } finally {
            System.setOut(originalOut);
        }

        // Assert — all string fields from R003/R008 are present in correctionContext
        JsonNode ctx = new ObjectMapper().readTree(outContent.toString()).get("correctionContext");
        assertEquals("She needs to negotiate the contract before Friday", ctx.get("sentence").asText());
        assertEquals("Ella necesita negociar el contrato antes del viernes", ctx.get("translation").asText());
        assertEquals("Affirmative sentences in the present simple", ctx.get("knowledgeTitle").asText());
        assertEquals("Escribe la forma afirmativa", ctx.get("knowledgeInstructions").asText());
        assertEquals("Present Simple", ctx.get("topicLabel").asText());
        assertEquals("A1", ctx.get("cefrLevel").asText());
    }
}
