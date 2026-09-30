package com.learney.contentaudit.auditcli.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.learney.contentaudit.auditapplication.AuditRunRequest;
import com.learney.contentaudit.auditapplication.AuditRunner;
import com.learney.contentaudit.auditcli.AnalyzeOptions;
import com.learney.contentaudit.auditcli.bootstrap.ReevaluationQuizSetResolver;
import com.learney.contentaudit.auditcli.bootstrap.UnreadableQuizSetOriginException;
import com.learney.contentaudit.auditcli.formatting.DefaultDrillDownResolver;
import com.learney.contentaudit.auditcli.formatting.DetailedFormatter;
import com.learney.contentaudit.auditcli.formatting.DrillDownResolver;
import com.learney.contentaudit.auditcli.formatting.FormatterRegistry;
import com.learney.contentaudit.auditcli.formatting.RawReportFormatter;
import com.learney.contentaudit.auditcli.formatting.ReportFormatter;
import com.learney.contentaudit.auditcli.formatting.ReportViewModel;
import com.learney.contentaudit.auditcli.formatting.ReportViewModelTransformer;
import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditReportStore;
import com.learney.contentaudit.auditdomain.EvaluationRunPolicy;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.auditapplication.CourseToAuditableMapper;
import com.learney.contentaudit.auditapplication.DefaultAnalyzerRegistry;
import com.learney.contentaudit.auditapplication.DefaultAuditRunner;
import com.learney.contentaudit.auditapplication.DefaultCocaBucketsConfig;
import com.learney.contentaudit.auditapplication.DefaultLemmaAbsenceConfig;
import com.learney.contentaudit.auditapplication.DefaultLemmaCountConfigLoader;
import com.learney.contentaudit.auditapplication.DefaultLemmaRecurrenceConfig;
import com.learney.contentaudit.auditapplication.DefaultQuizInstructionConfig;
import com.learney.contentaudit.auditapplication.DefaultSentenceLengthConfig;
import com.learney.contentaudit.auditcli.formatting.DefaultFormatterRegistry;
import com.learney.contentaudit.auditcli.formatting.DefaultReportViewModelTransformer;
import com.learney.contentaudit.auditcli.formatting.JsonReportFormatter;
import com.learney.contentaudit.auditcli.formatting.RawJsonReportFormatter;
import com.learney.contentaudit.auditcli.formatting.TableReportFormatter;
import com.learney.contentaudit.auditcli.formatting.TextReportFormatter;
import com.learney.contentaudit.auditdomain.AnalyzerCatalog;
import com.learney.contentaudit.auditdomain.AnalyzerDescriptor;
import com.learney.contentaudit.auditdomain.AnalyzerProvider;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableCourse;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableMilestone;
import com.learney.contentaudit.auditdomain.AuditableTopic;
import com.learney.contentaudit.auditdomain.ContentAnalyzer;
import com.learney.contentaudit.auditdomain.EvpCatalogPort;
import com.learney.contentaudit.auditdomain.IAuditEngine;
import com.learney.contentaudit.auditdomain.KnowledgeInstructionsLengthAnalyzerProvider;
import com.learney.contentaudit.auditdomain.KnowledgeTitleLengthAnalyzerProvider;
import com.learney.contentaudit.auditdomain.NlpToken;
import com.learney.contentaudit.auditdomain.NlpTokenizer;
import com.learney.contentaudit.auditdomain.SelfDescribingConfig;
import com.learney.contentaudit.auditdomain.SentenceLengthAnalyzerProvider;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerFamily;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerPlanBinding;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRuleCard;
import com.learney.contentaudit.auditdomain.coca.CocaBucketsAnalyzerProvider;
import com.learney.contentaudit.auditdomain.contextnumbers.AnalyzerErrorCounts;
import com.learney.contentaudit.auditdomain.finding.AnalysisCost;
import com.learney.contentaudit.auditdomain.finding.EvidencePart;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingDraft;
import com.learney.contentaudit.auditdomain.finding.FindingEvidence;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.auditdomain.findingengine.DefaultAnalyzerCatalog;
import com.learney.contentaudit.auditdomain.findingengine.DefaultContextNumbersCalculator;
import com.learney.contentaudit.auditdomain.findingengine.DefaultFindingCollector;
import com.learney.contentaudit.auditdomain.labs.DefaultSentenceLexicalScorer;
import com.learney.contentaudit.auditdomain.labs.LemmaAbsenceAnalyzerProvider;
import com.learney.contentaudit.auditdomain.labs.LemmaAbsenceScoreAggregator;
import com.learney.contentaudit.auditdomain.lemmacount.LemmaCountAnalyzerProvider;
import com.learney.contentaudit.auditdomain.lrec.DefaultContentWordFilter;
import com.learney.contentaudit.auditdomain.lrec.LemmaRecurrenceAnalyzerProvider;
import com.learney.contentaudit.auditdomain.quizinstructionengine.DefaultQuizInstructionAnalyzerFactory;
import com.learney.contentaudit.auditinfrastructure.FileSystemAuditReportStore;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.CourseRepository;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.courseinfrastructure.CourseValidatorImpl;
import com.learney.contentaudit.courseinfrastructure.FileSystemCourseRepository;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationEmitted;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationKey;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationLedger;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationOutcome;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRecord;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSubject;
import com.learney.contentaudit.evaluationledgerdomain.Evaluator;
import com.learney.contentaudit.evaluationledgerdomain.contentfingerprint.Sha256ContentFingerprinter;
import com.learney.contentaudit.evaluationledgerdomain.evaluationsession.DefaultEvaluationSessionFactory;
import com.learney.contentaudit.evaluationledgerinfrastructure.FileSystemEvaluationLedger;
import com.learney.contentaudit.quizinstructioninfrastructure.instructionverdict.JacksonQuizInstructionVerdictReader;
import com.learney.contentaudit.refinerdomain.CorrectionContextResolver;
import com.learney.contentaudit.refinerdomain.RefinementPlanStore;
import com.learney.contentaudit.refinerdomain.SuggestedLemmaQueryPort;
import com.learney.contentaudit.revisiondomain.RevisionArtifactStore;
import com.learney.contentaudit.vocabularyinfrastructure.evp.FileSystemEvpCatalog;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;

@Generated(
        value = "com.sentinel.SentinelEngine",
        comments = "Generated by Sentinel"
)
public class AnalyzeCmdTest {
    @Test
    @DisplayName("should be registered as a top-level subcommand of content-audit (verb relocated, semantics unchanged)")
    @Tag("FEAT-CLIRV")
    @Tag("F-CLIRV-R013")
    public void shouldBeRegisteredAsAToplevelSubcommandOfContentauditVerbRelocatedSemanticsUnchanged(
            ) {
        Command annotation = AnalyzeCmd.class.getAnnotation(Command.class);
        assertNotNull(annotation, "AnalyzeCmd must carry @Command for picocli registration");
        assertEquals("analyze", annotation.name(),
                "AnalyzeCmd must be addressable as the top-level verb 'analyze'");
    }

    @Test
    @DisplayName("should print an error and return non-zero exit code when invoked without a course path argument and no CONTENT_AUDIT_CONTENT_FOLDER env var is set")
    @Tag("FEAT-CLI")
    @Tag("F-CLI-R002")
    @Tag("F-CLI-J002")
    public void shouldPrintAnErrorAndReturnNonzeroExitCodeWhenInvokedWithoutACoursePathArgumentAndNoCONTENTAUDITCONTENTFOLDEREnvVarIsSet() {
        // R002: resolver returns null (no path, no env var) → non-zero exit, error message on stderr.
        // Inject a resolver that always returns null to isolate from the ambient env var.
        AuditRunner auditRunner = mock(AuditRunner.class);
        FormatterRegistry formatterRegistry = mock(FormatterRegistry.class);
        ReportViewModelTransformer viewModelTransformer = mock(ReportViewModelTransformer.class);
        RawReportFormatter rawReportFormatter = mock(RawReportFormatter.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        DrillDownResolver drillDownResolver = new DefaultDrillDownResolver();
        Map<String, DetailedFormatter> detailedFormatters = new HashMap<>();

        AnalyzeCmd sut = new AnalyzeCmd(auditRunner, formatterRegistry, viewModelTransformer,
                rawReportFormatter, drillDownResolver, detailedFormatters, auditReportStore,
                ignored -> null, mock(ReevaluationQuizSetResolver.class));

        ByteArrayOutputStream errCapture = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errCapture));
        int exitCode;
        try {
            exitCode = sut.analyze(null, "text", null, null, null, null, false);
        } finally {
            System.setErr(originalErr);
        }

        assertNotEquals(0, exitCode, "R002: missing course path must produce non-zero exit code");
        String errOutput = errCapture.toString();
        assertTrue(errOutput.contains("Error") || errOutput.contains("missing") || errOutput.contains("course"),
                "R002: error message must appear on stderr; got: " + errOutput);
    }

    @Test
    @DisplayName("should print an error and return non-zero exit code when invoked with a course path that does not exist on disk")
    @Tag("FEAT-CLI")
    @Tag("F-CLI-R002")
    @Tag("F-CLI-J002")
    public void shouldPrintAnErrorAndReturnNonzeroExitCodeWhenInvokedWithACoursePathThatDoesNotExistOnDisk() {
        // R002: non-existent path → auditRunner.runAudit throws RuntimeException → non-zero exit
        AuditRunner auditRunner = mock(AuditRunner.class);
        when(auditRunner.runAudit(any(Path.class), ArgumentMatchers.<Set<String>>any())).thenThrow(
                new RuntimeException("File not found: /nonexistent/course"));

        FormatterRegistry formatterRegistry = mock(FormatterRegistry.class);
        ReportViewModelTransformer viewModelTransformer = mock(ReportViewModelTransformer.class);
        RawReportFormatter rawReportFormatter = mock(RawReportFormatter.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        DrillDownResolver drillDownResolver = new DefaultDrillDownResolver();
        Map<String, DetailedFormatter> detailedFormatters = new HashMap<>();

        AnalyzeCmd sut = new AnalyzeCmd(auditRunner, formatterRegistry, viewModelTransformer,
                rawReportFormatter, drillDownResolver, detailedFormatters, auditReportStore,
                CoursePathResolver::resolve, mock(ReevaluationQuizSetResolver.class));

        ByteArrayOutputStream errCapture = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errCapture));
        int exitCode;
        try {
            exitCode = sut.analyze("/nonexistent/course", "text", null, null, null, null, false);
        } finally {
            System.setErr(originalErr);
        }

        assertNotEquals(0, exitCode, "R002: non-existent path must produce non-zero exit code");
        String errOutput = errCapture.toString();
        assertTrue(errOutput.length() > 0, "R002: error message must appear on stderr");
    }

    @Test
    @DisplayName("should produce a plain-text formatted summary on stdout when invoked without a --format option using text as the default format")
    @Tag("FEAT-CLI")
    @Tag("F-CLI-R003")
    public void shouldProduceAPlaintextFormattedSummaryOnStdoutWhenInvokedWithoutAFormatOptionUsingTextAsTheDefaultFormat() {
        // R003: default format is "text" — formatter for "text" key is invoked and output goes to stdout
        AuditRunner auditRunner = mock(AuditRunner.class);
        AuditReport report = new AuditReport(new AuditNode());
        when(auditRunner.runAudit(any(Path.class), ArgumentMatchers.<Set<String>>any())).thenReturn(report);

        ReportFormatter textFormatter = mock(ReportFormatter.class);
        when(textFormatter.format(any(), any())).thenReturn("PLAIN TEXT REPORT");

        FormatterRegistry formatterRegistry = mock(FormatterRegistry.class);
        when(formatterRegistry.getFormatter("text")).thenReturn(textFormatter);

        ReportViewModel viewModel = mock(ReportViewModel.class);
        ReportViewModelTransformer viewModelTransformer = mock(ReportViewModelTransformer.class);
        when(viewModelTransformer.transform(report)).thenReturn(viewModel);

        RawReportFormatter rawReportFormatter = mock(RawReportFormatter.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        when(auditReportStore.save(any())).thenReturn("audit-123");

        DrillDownResolver drillDownResolver = new DefaultDrillDownResolver();
        Map<String, DetailedFormatter> detailedFormatters = new HashMap<>();

        AnalyzeCmd sut = new AnalyzeCmd(auditRunner, formatterRegistry, viewModelTransformer,
                rawReportFormatter, drillDownResolver, detailedFormatters, auditReportStore,
                CoursePathResolver::resolve, mock(ReevaluationQuizSetResolver.class));

        ByteArrayOutputStream outCapture = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outCapture));
        int exitCode;
        try {
            exitCode = sut.analyze("/some/valid/course", "text", null, null, null, null, false);
        } finally {
            System.setOut(originalOut);
        }

        assertEquals(0, exitCode, "R003: successful text-format audit must return exit code 0");
        verify(formatterRegistry).getFormatter("text");
        String output = outCapture.toString();
        assertTrue(output.contains("PLAIN TEXT REPORT"), "R003: text formatter output must appear on stdout");
    }

    @Test
    @DisplayName("should produce a JSON-formatted summary on stdout when invoked with --format json")
    @Tag("FEAT-CLI")
    @Tag("F-CLI-R003")
    public void shouldProduceAJSONformattedSummaryOnStdoutWhenInvokedWithFormatJson() {
        // R003: --format json → formatter for "json" key is invoked and output goes to stdout
        AuditRunner auditRunner = mock(AuditRunner.class);
        AuditReport report = new AuditReport(new AuditNode());
        when(auditRunner.runAudit(any(Path.class), ArgumentMatchers.<Set<String>>any())).thenReturn(report);

        ReportFormatter jsonFormatter = mock(ReportFormatter.class);
        when(jsonFormatter.format(any(), any())).thenReturn("{\"score\": 0.85}");

        FormatterRegistry formatterRegistry = mock(FormatterRegistry.class);
        when(formatterRegistry.getFormatter("json")).thenReturn(jsonFormatter);

        ReportViewModel viewModel = mock(ReportViewModel.class);
        ReportViewModelTransformer viewModelTransformer = mock(ReportViewModelTransformer.class);
        when(viewModelTransformer.transform(report)).thenReturn(viewModel);

        RawReportFormatter rawReportFormatter = mock(RawReportFormatter.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        when(auditReportStore.save(any())).thenReturn("audit-456");

        DrillDownResolver drillDownResolver = new DefaultDrillDownResolver();
        Map<String, DetailedFormatter> detailedFormatters = new HashMap<>();

        AnalyzeCmd sut = new AnalyzeCmd(auditRunner, formatterRegistry, viewModelTransformer,
                rawReportFormatter, drillDownResolver, detailedFormatters, auditReportStore,
                CoursePathResolver::resolve, mock(ReevaluationQuizSetResolver.class));

        ByteArrayOutputStream outCapture = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outCapture));
        int exitCode;
        try {
            exitCode = sut.analyze("/some/valid/course", "json", null, null, null, null, false);
        } finally {
            System.setOut(originalOut);
        }

        assertEquals(0, exitCode, "R003: successful json-format audit must return exit code 0");
        verify(formatterRegistry).getFormatter("json");
        String output = outCapture.toString();
        assertTrue(output.contains("{\"score\": 0.85}"), "R003: json formatter output must appear on stdout");
    }

    @Test
    @DisplayName("should return exit code zero when the audit completes successfully regardless of the overall score produced")
    @Tag("FEAT-CLI")
    @Tag("F-CLI-R004")
    public void shouldReturnExitCodeZeroWhenTheAuditCompletesSuccessfullyRegardlessOfTheOverallScoreProduced() {
        // R004: successful audit → exit code 0, regardless of score (even if score is 0.0)
        AuditRunner auditRunner = mock(AuditRunner.class);
        AuditReport report = new AuditReport(new AuditNode());
        when(auditRunner.runAudit(any(Path.class), ArgumentMatchers.<Set<String>>any())).thenReturn(report);

        ReportFormatter textFormatter = mock(ReportFormatter.class);
        when(textFormatter.format(any(), any())).thenReturn("Score: 0.00 (very poor)");

        FormatterRegistry formatterRegistry = mock(FormatterRegistry.class);
        when(formatterRegistry.getFormatter("text")).thenReturn(textFormatter);

        ReportViewModelTransformer viewModelTransformer = mock(ReportViewModelTransformer.class);
        when(viewModelTransformer.transform(report)).thenReturn(mock(ReportViewModel.class));

        RawReportFormatter rawReportFormatter = mock(RawReportFormatter.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        when(auditReportStore.save(any())).thenReturn("audit-789");

        DrillDownResolver drillDownResolver = new DefaultDrillDownResolver();
        Map<String, DetailedFormatter> detailedFormatters = new HashMap<>();

        AnalyzeCmd sut = new AnalyzeCmd(auditRunner, formatterRegistry, viewModelTransformer,
                rawReportFormatter, drillDownResolver, detailedFormatters, auditReportStore,
                CoursePathResolver::resolve, mock(ReevaluationQuizSetResolver.class));

        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        int exitCode;
        try {
            exitCode = sut.analyze("/valid/course", "text", null, null, null, null, false);
        } finally {
            System.setOut(originalOut);
        }

        assertEquals(0, exitCode, "R004: exit code must be 0 on success regardless of score value");
    }

    @Test
    @DisplayName("should return a non-zero exit code when AuditRunner throws a RuntimeException during the audit pipeline and print a descriptive error message to stderr")
    @Tag("FEAT-CLI")
    @Tag("F-CLI-R004")
    @Tag("F-CLI-J003")
    public void shouldReturnANonzeroExitCodeWhenAuditRunnerThrowsARuntimeExceptionDuringTheAuditPipelineAndPrintADescriptiveErrorMessageToStderr() {
        // R004 + J003: RuntimeException in audit pipeline → non-zero exit + error message on stderr
        AuditRunner auditRunner = mock(AuditRunner.class);
        when(auditRunner.runAudit(any(Path.class), ArgumentMatchers.<Set<String>>any())).thenThrow(
                new RuntimeException("malformed JSON in course file"));

        FormatterRegistry formatterRegistry = mock(FormatterRegistry.class);
        ReportViewModelTransformer viewModelTransformer = mock(ReportViewModelTransformer.class);
        RawReportFormatter rawReportFormatter = mock(RawReportFormatter.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        DrillDownResolver drillDownResolver = new DefaultDrillDownResolver();
        Map<String, DetailedFormatter> detailedFormatters = new HashMap<>();

        AnalyzeCmd sut = new AnalyzeCmd(auditRunner, formatterRegistry, viewModelTransformer,
                rawReportFormatter, drillDownResolver, detailedFormatters, auditReportStore,
                CoursePathResolver::resolve, mock(ReevaluationQuizSetResolver.class));

        ByteArrayOutputStream errCapture = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errCapture));
        int exitCode;
        try {
            exitCode = sut.analyze("/valid/but/broken/course", "text", null, null, null, null, false);
        } finally {
            System.setErr(originalErr);
        }

        assertNotEquals(0, exitCode, "R004: RuntimeException in audit pipeline must produce non-zero exit code");
        String errOutput = errCapture.toString();
        assertTrue(errOutput.length() > 0,
                "R004: a descriptive error message must appear on stderr when the audit pipeline fails");
        assertTrue(errOutput.contains("Error") || errOutput.contains("malformed") || errOutput.contains("audit"),
                "R004: stderr must contain a descriptive message; got: " + errOutput);
    }

    @Test
    @DisplayName("should exclude the quiz instruction analysis from the run when the user asks to exclude analyzers")
    @Tag("FEAT-QINST")
    @Tag("F-QINST-R011")
    public void shouldExcludeTheQuizInstructionAnalysisFromTheRunWhenTheUserAsksToExcludeAnalyzers() {
        // R011: the user can explicitly exclude the quiz-instruction analysis from a run;
        // the exclusion must be carried into the AuditRunRequest handed to the runner.
        AuditRunner auditRunner = mock(AuditRunner.class);
        AuditReport report = new AuditReport(new AuditNode());
        when(auditRunner.runAudit(any(Path.class), any(AuditRunRequest.class))).thenReturn(report);

        ReportFormatter textFormatter = mock(ReportFormatter.class);
        when(textFormatter.format(any(), any())).thenReturn("REPORT");
        FormatterRegistry formatterRegistry = mock(FormatterRegistry.class);
        when(formatterRegistry.getFormatter("text")).thenReturn(textFormatter);

        ReportViewModelTransformer viewModelTransformer = mock(ReportViewModelTransformer.class);
        when(viewModelTransformer.transform(report)).thenReturn(mock(ReportViewModel.class));

        RawReportFormatter rawReportFormatter = mock(RawReportFormatter.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        when(auditReportStore.save(any())).thenReturn("audit-qinst-r011");

        DrillDownResolver drillDownResolver = new DefaultDrillDownResolver();
        Map<String, DetailedFormatter> detailedFormatters = new HashMap<>();

        AnalyzeCmd sut = new AnalyzeCmd(auditRunner, formatterRegistry, viewModelTransformer,
                rawReportFormatter, drillDownResolver, detailedFormatters, auditReportStore,
                CoursePathResolver::resolve, mock(ReevaluationQuizSetResolver.class));

        AnalyzeOptions options = new AnalyzeOptions("text", null, null, null, null,
                List.of("quiz-instruction"), false, null, null, null, null);

        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        int exitCode;
        try {
            exitCode = sut.analyze("/valid/course", options);
        } finally {
            System.setOut(originalOut);
        }

        assertEquals(0, exitCode, "R011: excluding an analyzer must not fail the audit");
        ArgumentCaptor<AuditRunRequest> captor = ArgumentCaptor.forClass(AuditRunRequest.class);
        verify(auditRunner).runAudit(any(Path.class), captor.capture());
        AuditRunRequest request = captor.getValue();
        assertNotNull(request.getExcludedAnalyzers(),
                "R011: excluded analyzers must be carried into the run request");
        assertTrue(request.getExcludedAnalyzers().contains("quiz-instruction"),
                "R011: quiz-instruction must be excluded when the user asks to exclude it");
    }

    @Test
    @DisplayName("should carry the instruction budget the user asked for into the run policy of the judge")
    @Tag("FEAT-QINST")
    @Tag("F-QINST-R006")
    public void shouldCarryTheInstructionBudgetTheUserAskedForIntoTheRunPolicyOfTheJudge() {
        // R006: --instruction-budget caps the new judge queries for this run; the value
        // must reach the judge's run policy inside the AuditRunRequest.
        AuditRunner auditRunner = mock(AuditRunner.class);
        AuditReport report = new AuditReport(new AuditNode());
        when(auditRunner.runAudit(any(Path.class), any(AuditRunRequest.class))).thenReturn(report);

        ReportFormatter textFormatter = mock(ReportFormatter.class);
        when(textFormatter.format(any(), any())).thenReturn("REPORT");
        FormatterRegistry formatterRegistry = mock(FormatterRegistry.class);
        when(formatterRegistry.getFormatter("text")).thenReturn(textFormatter);

        ReportViewModelTransformer viewModelTransformer = mock(ReportViewModelTransformer.class);
        when(viewModelTransformer.transform(report)).thenReturn(mock(ReportViewModel.class));

        RawReportFormatter rawReportFormatter = mock(RawReportFormatter.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        when(auditReportStore.save(any())).thenReturn("audit-qinst-r006");

        DrillDownResolver drillDownResolver = new DefaultDrillDownResolver();
        Map<String, DetailedFormatter> detailedFormatters = new HashMap<>();

        AnalyzeCmd sut = new AnalyzeCmd(auditRunner, formatterRegistry, viewModelTransformer,
                rawReportFormatter, drillDownResolver, detailedFormatters, auditReportStore,
                CoursePathResolver::resolve, mock(ReevaluationQuizSetResolver.class));

        AnalyzeOptions options = new AnalyzeOptions("text", null, null, null, null,
                null, false, 250, null, null, null);

        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        int exitCode;
        try {
            exitCode = sut.analyze("/valid/course", options);
        } finally {
            System.setOut(originalOut);
        }

        assertEquals(0, exitCode, "R006: a requested instruction budget must not fail the audit");
        ArgumentCaptor<AuditRunRequest> captor = ArgumentCaptor.forClass(AuditRunRequest.class);
        verify(auditRunner).runAudit(any(Path.class), captor.capture());
        AuditRunRequest request = captor.getValue();
        assertNotNull(request.getAnalyzerPolicies(),
                "R006: the run policy map must be present when a budget is requested");
        EvaluationRunPolicy policy = request.getAnalyzerPolicies().get("quiz-instruction");
        assertNotNull(policy, "R006: the run policy for quiz-instruction must be present when a budget is requested");
        assertEquals(250, policy.getMaxNewEvaluations(),
                "R006: the requested instruction budget must be carried into the run policy of the judge");
    }

    @Test
    @DisplayName("should ask for re evaluation with its scope when the user requests to judge again")
    @Tag("FEAT-QINST")
    @Tag("F-QINST-R012")
    public void shouldAskForReEvaluationWithItsScopeWhenTheUserRequestsToJudgeAgain() {
        // R012: re-evaluation is explicit and can be scoped; it must never be a side effect
        // of any other option, and its scope must reach the judge's run policy.
        AuditRunner auditRunner = mock(AuditRunner.class);
        AuditReport report = new AuditReport(new AuditNode());
        when(auditRunner.runAudit(any(Path.class), any(AuditRunRequest.class))).thenReturn(report);

        ReportFormatter textFormatter = mock(ReportFormatter.class);
        when(textFormatter.format(any(), any())).thenReturn("REPORT");
        FormatterRegistry formatterRegistry = mock(FormatterRegistry.class);
        when(formatterRegistry.getFormatter("text")).thenReturn(textFormatter);

        ReportViewModelTransformer viewModelTransformer = mock(ReportViewModelTransformer.class);
        when(viewModelTransformer.transform(report)).thenReturn(mock(ReportViewModel.class));

        RawReportFormatter rawReportFormatter = mock(RawReportFormatter.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        when(auditReportStore.save(any())).thenReturn("audit-qinst-r012");

        DrillDownResolver drillDownResolver = new DefaultDrillDownResolver();
        Map<String, DetailedFormatter> detailedFormatters = new HashMap<>();

        AnalyzeCmd sut = new AnalyzeCmd(auditRunner, formatterRegistry, viewModelTransformer,
                rawReportFormatter, drillDownResolver, detailedFormatters, auditReportStore,
                CoursePathResolver::resolve, mock(ReevaluationQuizSetResolver.class));

        AnalyzeOptions options = new AnalyzeOptions("text", null, null, null, null,
                null, false, null, "knowledge:K-042", null, null);

        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        int exitCode;
        try {
            exitCode = sut.analyze("/valid/course", options);
        } finally {
            System.setOut(originalOut);
        }

        assertEquals(0, exitCode, "R012: a requested re-evaluation must not fail the audit");
        ArgumentCaptor<AuditRunRequest> captor = ArgumentCaptor.forClass(AuditRunRequest.class);
        verify(auditRunner).runAudit(any(Path.class), captor.capture());
        AuditRunRequest request = captor.getValue();
        assertNotNull(request.getAnalyzerPolicies(),
                "R012: the run policy map must be present when re-evaluation is requested");
        EvaluationRunPolicy policy = request.getAnalyzerPolicies().get("quiz-instruction");
        assertNotNull(policy, "R012: the run policy for quiz-instruction must be present when re-evaluation is requested");
        assertTrue(policy.isReevaluate(),
                "R012: re-evaluation must be requested explicitly in the run policy of the judge");
        assertEquals("knowledge:K-042", policy.getReevaluationScope(),
                "R012: the scope of the re-evaluation must be carried into the run policy");
    }

    @Test
    @DisplayName("should neither exclude nor narrow the quiz instruction analysis when the user passes no analyzer option")
    @Tag("FEAT-QINST")
    @Tag("F-QINST-R001")
    public void shouldNeitherExcludeNorNarrowTheQuizInstructionAnalysisWhenTheUserPassesNoAnalyzerOption() {
        // R001: the quiz-instruction analysis runs on every audit unless explicitly excluded;
        // with no analyzer-related option, the run request must neither exclude it nor narrow
        // its run policy (no budget override, no re-evaluation request).
        AuditRunner auditRunner = mock(AuditRunner.class);
        AuditReport report = new AuditReport(new AuditNode());
        when(auditRunner.runAudit(any(Path.class), any(AuditRunRequest.class))).thenReturn(report);

        ReportFormatter textFormatter = mock(ReportFormatter.class);
        when(textFormatter.format(any(), any())).thenReturn("REPORT");
        FormatterRegistry formatterRegistry = mock(FormatterRegistry.class);
        when(formatterRegistry.getFormatter("text")).thenReturn(textFormatter);

        ReportViewModelTransformer viewModelTransformer = mock(ReportViewModelTransformer.class);
        when(viewModelTransformer.transform(report)).thenReturn(mock(ReportViewModel.class));

        RawReportFormatter rawReportFormatter = mock(RawReportFormatter.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        when(auditReportStore.save(any())).thenReturn("audit-qinst-r001");

        DrillDownResolver drillDownResolver = new DefaultDrillDownResolver();
        Map<String, DetailedFormatter> detailedFormatters = new HashMap<>();

        AnalyzeCmd sut = new AnalyzeCmd(auditRunner, formatterRegistry, viewModelTransformer,
                rawReportFormatter, drillDownResolver, detailedFormatters, auditReportStore,
                CoursePathResolver::resolve, mock(ReevaluationQuizSetResolver.class));

        AnalyzeOptions options = new AnalyzeOptions("text", null, null, null, null,
                null, false, null, null, null, null);

        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        int exitCode;
        try {
            exitCode = sut.analyze("/valid/course", options);
        } finally {
            System.setOut(originalOut);
        }

        assertEquals(0, exitCode, "R001: an audit with no analyzer option must succeed");
        ArgumentCaptor<AuditRunRequest> captor = ArgumentCaptor.forClass(AuditRunRequest.class);
        verify(auditRunner).runAudit(any(Path.class), captor.capture());
        AuditRunRequest request = captor.getValue();

        boolean excluded = request.getExcludedAnalyzers() != null
                && request.getExcludedAnalyzers().contains("quiz-instruction");
        assertFalse(excluded,
                "R001: quiz-instruction must not be excluded when the user passes no analyzer option");

        boolean narrowed = request.getAnalyzerPolicies() != null
                && request.getAnalyzerPolicies().containsKey("quiz-instruction");
        assertFalse(narrowed,
                "R001: quiz-instruction's run policy must not be narrowed when the user passes no analyzer option");
    }

    @Test
    @DisplayName("should carry into the instruction run policy the set of quizzes the user declared, as the only re evaluation scope of the run")
    @Tag("FEAT-QINST")
    @Tag("F-QINST-R018")
    public void shouldCarryIntoTheInstructionRunPolicyTheSetOfQuizzesTheUserDeclaredAsTheOnlyReEvaluationScopeOfTheRun() {
        // R018: the set of exercise ids the user declares in the request must reach the
        // judge's run policy as its re-evaluation scope, and as the ONLY one — a declared
        // set IS the re-evaluation request, so it must not also carry an area/total
        // re-evaluation flag (F-QINST-R012 stays a separate, mutually exclusive form).
        AuditRunner auditRunner = mock(AuditRunner.class);
        AuditReport report = new AuditReport(new AuditNode());
        when(auditRunner.runAudit(any(Path.class), any(AuditRunRequest.class))).thenReturn(report);

        ReportFormatter textFormatter = mock(ReportFormatter.class);
        when(textFormatter.format(any(), any())).thenReturn("REPORT");
        FormatterRegistry formatterRegistry = mock(FormatterRegistry.class);
        when(formatterRegistry.getFormatter("text")).thenReturn(textFormatter);

        ReportViewModelTransformer viewModelTransformer = mock(ReportViewModelTransformer.class);
        when(viewModelTransformer.transform(report)).thenReturn(mock(ReportViewModel.class));

        RawReportFormatter rawReportFormatter = mock(RawReportFormatter.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        when(auditReportStore.save(any())).thenReturn("audit-qinst-r018");

        DrillDownResolver drillDownResolver = new DefaultDrillDownResolver();
        Map<String, DetailedFormatter> detailedFormatters = new HashMap<>();
        ReevaluationQuizSetResolver reevaluationQuizSetResolver = mock(ReevaluationQuizSetResolver.class);

        AnalyzeCmd sut = new AnalyzeCmd(auditRunner, formatterRegistry, viewModelTransformer,
                rawReportFormatter, drillDownResolver, detailedFormatters, auditReportStore,
                CoursePathResolver::resolve, reevaluationQuizSetResolver);

        Set<String> declaredQuizIds = Set.of("Q-101", "Q-202", "Q-303");
        AnalyzeOptions options = new AnalyzeOptions("text", null, null, null, null,
                null, false, null, null, declaredQuizIds, null);

        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        int exitCode;
        try {
            exitCode = sut.analyze("/valid/course", options);
        } finally {
            System.setOut(originalOut);
        }

        assertEquals(0, exitCode, "R018: a declared re-evaluation set must not fail the audit");
        ArgumentCaptor<AuditRunRequest> captor = ArgumentCaptor.forClass(AuditRunRequest.class);
        verify(auditRunner).runAudit(any(Path.class), captor.capture());
        AuditRunRequest request = captor.getValue();
        assertNotNull(request.getAnalyzerPolicies(),
                "R018: the run policy map must be present when a set of quizzes is declared");
        EvaluationRunPolicy policy = request.getAnalyzerPolicies().get("quiz-instruction");
        assertNotNull(policy,
                "R018: the run policy for quiz-instruction must be present when a set of quizzes is declared");
        assertEquals(declaredQuizIds, policy.getReevaluationSubjectIds(),
                "R018: the declared set of quizzes must be carried into the run policy of the judge");
        assertFalse(policy.isReevaluate(),
                "R018: a declared set is itself the re-evaluation request; it must not also carry an area/total re-evaluation flag");
        assertNull(policy.getReevaluationScope(),
                "R018: a declared set must be the only re-evaluation scope, not combined with an area scope");
    }

    @Test
    @DisplayName("should not run the audit at all, and should report the origin it could not read, when the request names an unreadable origin for the set of quizzes")
    @Tag("FEAT-QINST")
    @Tag("F-QINST-R019")
    public void shouldNotRunTheAuditAtAllAndShouldReportTheOriginItCouldNotReadWhenTheRequestNamesAnUnreadableOriginForTheSetOfQuizzes() {
        // R019 invariant 2: an origin the request names for the set of quizzes, that the
        // system cannot read, must reject the request before the audit ever runs, naming
        // the origin. This is the half of the invariant the resolver alone cannot observe
        // (that no audit runs at all), so it is exercised here through the full command,
        // via the CLI option that names the origin.
        AuditRunner auditRunner = mock(AuditRunner.class);
        FormatterRegistry formatterRegistry = mock(FormatterRegistry.class);
        ReportViewModelTransformer viewModelTransformer = mock(ReportViewModelTransformer.class);
        RawReportFormatter rawReportFormatter = mock(RawReportFormatter.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        DrillDownResolver drillDownResolver = new DefaultDrillDownResolver();
        Map<String, DetailedFormatter> detailedFormatters = new HashMap<>();

        String unreadableOrigin = "/no/such/origin/quiz-ids.txt";
        ReevaluationQuizSetResolver reevaluationQuizSetResolver = mock(ReevaluationQuizSetResolver.class);
        when(reevaluationQuizSetResolver.resolve(any(), any())).thenThrow(
                new UnreadableQuizSetOriginException(unreadableOrigin, "No such file or directory"));

        AnalyzeCmd sut = new AnalyzeCmd(auditRunner, formatterRegistry, viewModelTransformer,
                rawReportFormatter, drillDownResolver, detailedFormatters, auditReportStore,
                CoursePathResolver::resolve, reevaluationQuizSetResolver);

        ByteArrayOutputStream errCapture = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errCapture));
        int exitCode;
        try {
            exitCode = new CommandLine(sut).execute("/valid/course",
                    "--reevaluate-instruction-quizzes-file", unreadableOrigin);
        } finally {
            System.setErr(originalErr);
        }

        assertNotEquals(0, exitCode, "R019: an unreadable origin must produce a non-zero exit code");
        verifyNoInteractions(auditRunner);
        String errOutput = errCapture.toString();
        assertTrue(errOutput.contains(unreadableOrigin),
                "R019: the error must name the origin that could not be read; got: " + errOutput);
    }

    @Test
    @DisplayName("should print that analyzer quiz-instructions was not found with the hint to run content-audit get analyzers, exit non-zero and save no analysis when asked to run a name the catalog does not have")
    @Tag("FEAT-HALL")
    @Tag("F-HALL-R012")
    public void shouldPrintThatAnalyzerQuizinstructionsWasNotFoundWithTheHintToRunContentauditGetAnalyzersExitNonzeroAndSaveNoAnalysisWhenAskedToRunANameTheCatalogDoesNotHave() {
        // R012 (F-CLIRV-R016): quiz-instructions, one letter off the name get analyzers lists, is
        // rejected before the course is loaded -- loading it tokenizes it with spaCy -- with the
        // message that points to get analyzers, a non-zero exit and no analysis saved.
        CourseRepository courseRepository = spy(new FileSystemCourseRepository(new CourseValidatorImpl()));
        AnalyzerCatalog catalog = Base299.mainCatalog(new Base299.FakeJudge(), new Base299.MapLedger());
        AuditReportStore store = mock(AuditReportStore.class);
        AnalyzeCmd sut = Base299.analyzeCmd(Base299.runner(catalog, courseRepository, Base299.mapper()), store);

        Base299.Captured run = Base299.run(sut, Base299.COURSE.toString(), "--analyzers", "quiz-instructions");

        assertNotEquals(0, run.exit(), "R012: the run is rejected");
        assertTrue(run.err().contains("Analyzer 'quiz-instructions' not found. "
                + "Run 'content-audit get analyzers' to see available analyzers."),
                "R012: the message that points to get analyzers; got: " + run.err());
        verify(courseRepository, never()).load(any());
        verifyNoInteractions(store);
        assertTrue(catalog.find("quiz-instruction").isPresent(), "the name get analyzers lists is quiz-instruction");
    }

    @Test
    @DisplayName("should cap the judge queries with --budget quiz-instruction=N, the name get analyzers lists, as --instruction-budget does")
    @Tag("FEAT-HALL")
    @Tag("F-HALL-R005")
    public void shouldCapTheJudgeQueriesWithBudgetQuizinstructionNTheNameGetAnalyzersListsAsInstructionbudgetDoes() {
        // R005: the judge's budget is set with its name -- the one get analyzers lists -- and caps its
        // queries on the 29/9 course exactly as --instruction-budget does: the same three queries,
        // about the same quizzes, and the same numbers, with the rest declared not evaluated.
        Base299.FakeJudge byName = new Base299.FakeJudge(Base299.FIRST_QUIZ);
        Base299.FakeJudge byShortcut = new Base299.FakeJudge(Base299.FIRST_QUIZ);
        AnalyzerCatalog nameCatalog = Base299.mainCatalog(byName, new Base299.MapLedger());
        AnalyzerCatalog shortcutCatalog = Base299.mainCatalog(byShortcut, new Base299.MapLedger());
        AuditReportStore nameStore = mock(AuditReportStore.class);
        AuditReportStore shortcutStore = mock(AuditReportStore.class);

        Base299.Captured withName = Base299.run(Base299.analyzeCmd(Base299.runner(nameCatalog), nameStore),
                Base299.COURSE.toString(), "--analyzers", "quiz-instruction", "--budget", "quiz-instruction=3",
                "-f", "raw");
        Base299.Captured withShortcut = Base299.run(Base299.analyzeCmd(Base299.runner(shortcutCatalog), shortcutStore),
                Base299.COURSE.toString(), "--analyzers", "quiz-instruction", "--instruction-budget", "3", "-f", "raw");

        assertEquals(0, withName.exit(), withName.err());
        assertEquals(0, withShortcut.exit(), withShortcut.err());
        assertTrue(nameCatalog.list().stream().anyMatch(card -> "quiz-instruction".equals(card.getName())),
                "the budget names the analyzer as get analyzers lists it");
        assertEquals(3, byName.asked().size(), "R005: --budget quiz-instruction=3 caps the judge at 3 queries");
        assertEquals(byShortcut.asked(), byName.asked(), "R005: about the same quizzes as --instruction-budget 3");
        AuditReport named = savedReport(nameStore);
        AuditReport shortcut = savedReport(shortcutStore);
        AnalyzerErrorCounts judged = Base299.countsOf(named.getRoot(), Base299.JUDGE);
        assertEquals(3, judged.getEvaluated(), "R005: three quizzes judged");
        assertEquals(Base299.QUIZZES - 3, judged.getNotEvaluated(), "and the rest declared not evaluated");
        assertEquals(1, judged.getWithError(), "the breach the judge found in the first quiz");
        assertEquals(shortcut.getRoot().getNumbers(), named.getRoot().getNumbers(),
                "R005: the same numbers as --instruction-budget 3");
    }

    @Test
    @DisplayName("should leave the 886 files of the 29/9 course identical byte for byte after analyzing it with any selection of analyzers, such as all of them, sentence-length alone or quiz-instruction alone")
    @Tag("FEAT-HALL")
    @Tag("F-HALL-R015")
    public void shouldLeaveThe886FilesOfThe299CourseIdenticalByteForByteAfterAnalyzingItWithAnySelectionOfAnalyzersSuchAsAllOfThemSentencelengthAloneOrQuizinstructionAlone(
            @TempDir Path tempDir) {
        // R015: analyzing never touches the course -- the 886 files of the 29/9 course stay identical,
        // byte for byte, after a run of all the analyzers, of sentence-length alone and of
        // quiz-instruction alone. A copy is analyzed, so that a failure never damages the real one.
        Path course = tempDir.resolve("english-course");
        Base299.copyTree(Base299.COURSE, course);
        Map<String, String> before = Base299.hashes(course);
        assertEquals(Base299.FILES, before.size(), "the 886 files of the 29/9 course");
        AuditReportStore store = mock(AuditReportStore.class);
        AnalyzeCmd sut = Base299.analyzeCmd(Base299.runner(Base299.mainCatalog(
                new Base299.FakeJudge(Base299.FIRST_QUIZ), new Base299.MapLedger())), store);

        for (List<String> selection : List.of(List.<String>of(), List.of("--analyzers", "sentence-length"),
                List.of("--analyzers", "quiz-instruction"))) {
            List<String> args = new ArrayList<>(List.of(course.toString(), "-f", "raw"));
            args.addAll(selection);

            Base299.Captured run = Base299.run(sut, args.toArray(String[]::new));

            assertEquals(0, run.exit(), run.err());
            assertEquals(before, Base299.hashes(course),
                    "R015: the course is identical after analyzing it with " + (selection.isEmpty() ? "all of them" : selection));
        }
        ArgumentCaptor<AuditReport> analyses = ArgumentCaptor.forClass(AuditReport.class);
        verify(store, org.mockito.Mockito.times(3)).save(analyses.capture());
        List<Set<String>> ran = analyses.getAllValues().stream()
                .map(report -> report.getRoot().getNumbers().getAnalyzerScores().stream()
                        .map(score -> score.getAnalyzer()).collect(java.util.stream.Collectors.toSet()))
                .toList();
        Set<String> all = new java.util.HashSet<>(Base299.CLASSIC);
        all.add(Base299.JUDGE);
        assertEquals(List.of(all, Set.of("sentence-length"), Set.of(Base299.JUDGE)), ran,
                "the three selections ran over the whole course: all of them, sentence-length alone, quiz-instruction alone");
    }

    @Test
    @DisplayName("should write in a run nothing but its analysis and, when the judge ran, its new verdicts in the evaluation ledger")
    @Tag("FEAT-HALL")
    @Tag("F-HALL-R015")
    public void shouldWriteInARunNothingButItsAnalysisAndWhenTheJudgeRanItsNewVerdictsInTheEvaluationLedger(
            @TempDir Path tempDir) throws IOException {
        // R015: a run writes its analysis -- the report and its digest -- and, when the judge ran, the
        // verdicts it paid for, appended to the ledger; nothing else, and nothing the working
        // directory already had changes. The course: the quizzes of the first theme of the 29/9
        // course, «Afirmativas con be», with their real text.
        AuditableCourse firstTheme = Base299.firstTheme();
        Path withJudge = tempDir.resolve("with-judge");
        Path withoutJudge = tempDir.resolve("without-judge");
        for (Path workdir : List.of(withJudge, withoutJudge)) {
            earlierWork(workdir, firstTheme);
        }
        Path ledger = Path.of(".content-audit/evaluations/" + Base299.FakeJudge.ID + ".jsonl");
        Map<String, String> beforeWithJudge = Base299.hashes(withJudge);
        byte[] ledgerBefore = Files.readAllBytes(withJudge.resolve(ledger));
        Map<String, String> beforeWithoutJudge = Base299.hashes(withoutJudge);

        Base299.FakeJudge judge = new Base299.FakeJudge();
        Base299.Captured judged = Base299.run(firstThemeAnalyze(withJudge, judge, firstTheme), "english-course",
                "--analyzers", "sentence-length,quiz-instruction", "--budget", "quiz-instruction=2", "-f", "raw");
        Base299.Captured unjudged = Base299.run(firstThemeAnalyze(withoutJudge, new Base299.FakeJudge(), firstTheme),
                "english-course", "--analyzers", "sentence-length", "-f", "raw");

        assertEquals(0, judged.exit(), judged.err());
        assertEquals(0, unjudged.exit(), unjudged.err());
        String judgedId = Base299.savedId(judged);
        String unjudgedId = Base299.savedId(unjudged);
        Map<String, String> afterWithJudge = Base299.hashes(withJudge);
        Map<String, String> afterWithoutJudge = Base299.hashes(withoutJudge);
        assertEquals(Set.of(".content-audit/audits/audit-" + judgedId + ".json",
                ".content-audit/audit-digests/audit-" + judgedId + ".json"),
                difference(afterWithJudge.keySet(), beforeWithJudge.keySet()),
                "R015: the run with the judge adds its analysis and nothing else");
        assertEquals(Set.of(".content-audit/audits/audit-" + unjudgedId + ".json",
                ".content-audit/audit-digests/audit-" + unjudgedId + ".json"),
                difference(afterWithoutJudge.keySet(), beforeWithoutJudge.keySet()),
                "R015: the run without the judge adds its analysis and nothing else");
        beforeWithJudge.forEach((file, hash) -> {
            if (!file.equals(ledger.toString())) {
                assertEquals(hash, afterWithJudge.get(file), "R015: " + file + " stays as it was");
            }
        });
        beforeWithoutJudge.forEach((file, hash) -> assertEquals(hash, afterWithoutJudge.get(file),
                "R015: " + file + " stays as it was, the ledger included: the judge did not run"));
        byte[] ledgerAfter = Files.readAllBytes(withJudge.resolve(ledger));
        assertArrayEquals(ledgerBefore, Arrays.copyOf(ledgerAfter, ledgerBefore.length),
                "R015: the verdicts already recorded stay as they were");
        String appended = new String(ledgerAfter, ledgerBefore.length, ledgerAfter.length - ledgerBefore.length,
                StandardCharsets.UTF_8);
        assertEquals(2, judge.asked().size(), "the two queries the budget allowed");
        assertEquals(2, appended.lines().count(), "R015: the ledger only grew by the two new verdicts");
    }

    private static AuditReport savedReport(AuditReportStore store) {
        ArgumentCaptor<AuditReport> saved = ArgumentCaptor.forClass(AuditReport.class);
        verify(store).save(saved.capture());
        return saved.getValue();
    }

    private static Set<String> difference(Set<String> after, Set<String> before) {
        Set<String> added = new java.util.TreeSet<>(after);
        added.removeAll(before);
        return added;
    }

    /** analyze over the first theme of the 29/9 course, writing in the given working directory. */
    private static AnalyzeCmd firstThemeAnalyze(Path workdir, Base299.FakeJudge judge, AuditableCourse firstTheme) {
        CourseRepository repository = mock(CourseRepository.class);
        CourseEntity entity = new CourseEntity();
        when(repository.load(any())).thenReturn(entity);
        CourseToAuditableMapper mapper = mock(CourseToAuditableMapper.class);
        when(mapper.map(entity)).thenReturn(firstTheme);
        AnalyzerCatalog catalog = Base299.mainCatalog(judge, new FileSystemEvaluationLedger(workdir));
        return Base299.analyzeCmd(Base299.runner(catalog, repository, mapper), new FileSystemAuditReportStore(workdir));
    }

    /** What the working directory already has: an analysis of the day before and the verdict it paid for. */
    private static void earlierWork(Path workdir, AuditableCourse firstTheme) throws IOException {
        Base299.Captured earlier = Base299.run(firstThemeAnalyze(workdir, new Base299.FakeJudge(), firstTheme),
                "english-course", "--analyzers", "quiz-instruction", "--budget", "quiz-instruction=1", "-f", "raw");
        assertEquals(0, earlier.exit(), earlier.err());
        String id = Base299.savedId(earlier);
        for (String dir : List.of(".content-audit/audits", ".content-audit/audit-digests")) {
            Files.move(workdir.resolve(dir).resolve("audit-" + id + ".json"),
                    workdir.resolve(dir).resolve("audit-2026-09-29T10-00-00.json"));
        }
    }

    // -----------------------------------------------------------------------
    // FEAT-HALL: the 29/9 base through the command line
    // -----------------------------------------------------------------------

    /**
     * The 29/9 base for the FEAT-HALL tests of the command line: the course of the repository
     * (db/english-course, 886 files) through what Main wires -- catalog, runner, engine, collector,
     * calculator, commands and stores. Only the boundaries are faked: spaCy, by the tokens it gave
     * each sentence in the analysis 2026-09-30T11-54-02 (the fixtures of audit-application, made by
     * scripts/fhall_base_fixtures.py), and the paid judge, by {@link FakeJudge}.
     */
    static final class Base299 {
        static final Path COURSE = Path.of("../db/english-course");
        static final Path FIXTURES = Path.of("../audit-application/src/test/resources/fhall-base-2026-09-30");
        static final Path PLAN = Path.of("../refiner-domain/src/test/resources/fhall-base-2026-09-30/plan.tsv.gz");
        static final Path EVP = Path.of("../analysis/recursos-compartidos/enriched_vocabulary_catalog.json");
        static final int FILES = 886;
        static final int NODES = 11760;
        static final int QUIZZES = 11287;
        /** The first quiz of the course: A1, Present Simple, «She __ English.» («Ella es inglesa.»). */
        static final String FIRST_QUIZ = "67fab6d59930102295341e5f";
        static final String JUDGE = "quiz-instruction";
        /** The seven classic analyzers, in the order of the catalog. */
        static final List<String> CLASSIC = List.of("sentence-length", "knowledge-title-length",
                "knowledge-instructions-length", "coca-buckets-distribution", "lemma-recurrence", "lemma-absence",
                "lemma-count");
        /** The four levels, in the order of the course: A1, A2, B1 and B2. */
        static final List<String> LEVELS = List.of("6814dafa7d73e7209a13d382", "6814dafa7d73e7209a13d218",
                "6814dafa7d73e7209a13d2b5", "6814dafa7d73e7209a13d3ef");

        private static NlpTokenizer tokenizer;

        private static EvpCatalogPort evp;

        private Base299() {
        }

        static synchronized NlpTokenizer tokenizer() {
            if (tokenizer == null) {
                tokenizer = new RecordedTokenizer();
            }
            return tokenizer;
        }

        static synchronized EvpCatalogPort evp() {
            if (evp == null) {
                evp = new FileSystemEvpCatalog(EVP);
            }
            return evp;
        }

        /** The seven classic providers, as Main builds them. */
        static List<AnalyzerProvider> classicProviders() {
            DefaultLemmaAbsenceConfig absence = new DefaultLemmaAbsenceConfig();
            return List.of(
                    new SentenceLengthAnalyzerProvider(tokenizer(), new DefaultSentenceLengthConfig()),
                    new KnowledgeTitleLengthAnalyzerProvider(),
                    new KnowledgeInstructionsLengthAnalyzerProvider(),
                    new CocaBucketsAnalyzerProvider(tokenizer(), new DefaultCocaBucketsConfig()),
                    new LemmaRecurrenceAnalyzerProvider(new DefaultLemmaRecurrenceConfig()),
                    new LemmaAbsenceAnalyzerProvider(evp(), absence,
                            new DefaultSentenceLexicalScorer(evp(), new DefaultContentWordFilter(), absence)),
                    new LemmaCountAnalyzerProvider(evp(), new DefaultLemmaCountConfigLoader().load(null)));
        }

        /** The judge as Main builds it -- its factory, session and verdict reader -- over a fake judge. */
        static AnalyzerProvider judge(FakeJudge judge, EvaluationLedger ledger) {
            return new DefaultQuizInstructionAnalyzerFactory(
                    new DefaultEvaluationSessionFactory(ledger, new Sha256ContentFingerprinter()), judge,
                    new JacksonQuizInstructionVerdictReader(), new DefaultQuizInstructionConfig());
        }

        /** Main's eight providers, in its order. */
        static List<AnalyzerProvider> mainProviders(FakeJudge judge, EvaluationLedger ledger) {
            List<AnalyzerProvider> providers = new ArrayList<>(classicProviders());
            providers.add(judge(judge, ledger));
            return providers;
        }

        static AnalyzerCatalog mainCatalog(FakeJudge judge, EvaluationLedger ledger) {
            return new DefaultAnalyzerCatalog(mainProviders(judge, ledger));
        }

        static CourseToAuditableMapper mapper() {
            return new CourseToAuditableMapper(tokenizer(), DefaultQuizSentenceConverter.create());
        }

        /** The runner Main builds, reading the course from disk. */
        static DefaultAuditRunner runner(AnalyzerCatalog catalog) {
            return runner(catalog, new FileSystemCourseRepository(new CourseValidatorImpl()), mapper());
        }

        static DefaultAuditRunner runner(AnalyzerCatalog catalog, CourseRepository repository,
                CourseToAuditableMapper mapper) {
            return new DefaultAuditRunner(repository, mapper, new IAuditEngine(new LemmaAbsenceScoreAggregator(),
                    catalog, new DefaultFindingCollector(), new DefaultContextNumbersCalculator()), catalog);
        }

        /** analyze as Main builds it. */
        static AnalyzeCmd analyzeCmd(AuditRunner runner, AuditReportStore store) {
            DrillDownResolver drillDown = new DefaultDrillDownResolver();
            Map<String, ReportFormatter> formatters = new HashMap<>();
            formatters.put("text", new TextReportFormatter(drillDown));
            formatters.put("json", new JsonReportFormatter(drillDown));
            formatters.put("table", new TableReportFormatter(drillDown));
            return new AnalyzeCmd(runner, new DefaultFormatterRegistry(formatters),
                    new DefaultReportViewModelTransformer(), new RawJsonReportFormatter(), drillDown,
                    new HashMap<>(), store, CoursePathResolver::resolve, mock(ReevaluationQuizSetResolver.class));
        }

        /** get as Main builds it, for what these tests read with it: analyses and analyzers. */
        static GetCmd getCmd(AuditReportStore store, AnalyzerCatalog catalog) {
            GetCmd get = new GetCmd(store, mock(RefinementPlanStore.class), new DefaultAnalyzerRegistry(catalog),
                    mock(CorrectionContextResolver.class), null, null, new DefaultCorrectionContextJsonMapper(),
                    mock(SuggestedLemmaQueryPort.class));
            get.setRevisionArtifactStore(mock(RevisionArtifactStore.class));
            return get;
        }

        /** Runs a command as the command line does, through picocli, and keeps what it printed. */
        static Captured run(Object command, String... args) {
            PrintStream out = System.out;
            PrintStream err = System.err;
            ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
            ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outBytes, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(errBytes, true, StandardCharsets.UTF_8));
            try {
                int exit = new CommandLine(command).execute(args);
                return new Captured(exit, outBytes.toString(StandardCharsets.UTF_8),
                        errBytes.toString(StandardCharsets.UTF_8));
            } finally {
                System.setOut(out);
                System.setErr(err);
            }
        }

        /** The id analyze printed when it saved the analysis. */
        static String savedId(Captured run) {
            Matcher saved = Pattern.compile("\\[Audit saved: ([^\\]]+)\\]").matcher(run.err());
            assertTrue(saved.find(), "analyze saved its analysis: " + run.err());
            return saved.group(1);
        }

        /** The quizzes of the first theme of the course («Afirmativas con be», A1), with their real text and tokens. */
        static AuditableCourse firstTheme() {
            AuditableCourse course = mapper().map(new FileSystemCourseRepository(new CourseValidatorImpl()).load(COURSE));
            AuditableMilestone level = course.getMilestones().get(0);
            AuditableTopic topic = level.getTopics().get(0);
            AuditableKnowledge theme = topic.getKnowledge().get(0);
            topic.setKnowledge(List.of(theme));
            level.setTopics(List.of(topic));
            course.setMilestones(List.of(level));
            return course;
        }

        /**
         * The vocabulary score each node had before the contract -- what analyze showed: the plain
         * average of the node's analyzer keys, without the sub-metrics -- in the analysis
         * 2026-09-30T11-54-02, by node key.
         */
        static Map<String, Double> preContractVocabulary() {
            ObjectMapper json = new ObjectMapper();
            Map<String, Double> result = new LinkedHashMap<>();
            for (String line : readFixture(FIXTURES.resolve("nodes.jsonl.gz"))) {
                JsonNode row = tree(json, line);
                double sum = 0;
                int count = 0;
                for (Iterator<Map.Entry<String, JsonNode>> it = row.get(2).fields(); it.hasNext(); ) {
                    Map.Entry<String, JsonNode> score = it.next();
                    if (!score.getKey().contains("/")) {
                        sum += score.getValue().asDouble();
                        count++;
                    }
                }
                result.put(row.get(0).asText() + " " + row.get(1).asText(), sum / count);
            }
            return result;
        }

        /** The tasks of the plan 2026-09-30T11-54-12, made before the contract from that analysis. */
        static List<String> preContractPlan() {
            return readFixture(PLAN);
        }

        static String key(AuditNode node) {
            return node.getTarget() + " " + (node.getEntity() != null ? node.getEntity().getId() : "root");
        }

        static void walk(AuditNode node, Consumer<AuditNode> action) {
            action.accept(node);
            if (node.getChildren() != null) {
                node.getChildren().forEach(child -> walk(child, action));
            }
        }

        static AuditNode node(AuditReport report, String id) {
            List<AuditNode> found = new ArrayList<>();
            walk(report.getRoot(), n -> {
                if (n.getEntity() != null && id.equals(n.getEntity().getId())) {
                    found.add(n);
                }
            });
            assertEquals(1, found.size(), "node " + id);
            return found.get(0);
        }

        /** The node and every node above it: its theme, topic, level and the course. */
        static List<AuditNode> upwards(AuditNode node) {
            List<AuditNode> chain = new ArrayList<>();
            for (AuditNode n = node; n != null; n = n.getParent()) {
                chain.add(n);
            }
            return chain;
        }

        static List<Finding> findings(AuditReport report) {
            List<Finding> all = new ArrayList<>();
            walk(report.getRoot(), n -> all.addAll(n.getFindings()));
            return all;
        }

        static AnalyzerErrorCounts countsOf(AuditNode node, String analyzer) {
            return node.getNumbers().getErrors().getAnalyzers().stream()
                    .filter(c -> analyzer.equals(c.getAnalyzer()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no counts of " + analyzer + " on " + key(node)));
        }

        static void copyTree(Path from, Path to) {
            try (Stream<Path> paths = Files.walk(from)) {
                for (Path path : paths.toList()) {
                    Path target = to.resolve(from.relativize(path).toString());
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(target);
                    } else {
                        Files.copy(path, target);
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        /** Every file under the directory, by its relative path, with the SHA-256 of its bytes. */
        static Map<String, String> hashes(Path dir) {
            Map<String, String> result = new TreeMap<>();
            if (!Files.exists(dir)) {
                return result;
            }
            try (Stream<Path> paths = Files.walk(dir)) {
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                for (Path path : paths.filter(Files::isRegularFile).toList()) {
                    result.put(dir.relativize(path).toString(), HexFormat.of().formatHex(sha.digest(Files.readAllBytes(path))));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new AssertionError(e);
            }
            return result;
        }

        static List<String> readFixture(Path file) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new GZIPInputStream(Files.newInputStream(file)), StandardCharsets.UTF_8))) {
                return reader.lines().toList();
            } catch (IOException e) {
                throw new UncheckedIOException("fixture " + file + " unreadable", e);
            }
        }

        private static JsonNode tree(ObjectMapper json, String line) {
            try {
                return json.readTree(line);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        /** What a command exited with and printed. */
        record Captured(int exit, String out, String err) {
        }

        /**
         * The paid judge, faked -- the only part of the judge that is: it finds a major breach in
         * the quizzes it is given and passes every other one, with payloads shaped as the real
         * judge's (verdict.schema.json), and keeps the quizzes it was asked about.
         */
        static final class FakeJudge implements Evaluator {
            static final String ID = "quiz-instruction-validator";

            static final String BREACH = "{\"schemaVersion\":\"1.0\",\"followsInstructions\":false,\"confidence\":0.9,"
                    + "\"severity\":\"major\",\"reason\":\"La respuesta aceptada no es la forma que pide la consigna.\","
                    + "\"violations\":[{\"code\":\"WRONG_FORM\",\"constraint\":\"La forma que pide la consigna\","
                    + "\"evidence\":\"La respuesta aceptada\",\"explanation\":\"No es la forma que pide la consigna.\"}],"
                    + "\"checkedConstraints\":[]}";

            static final String COMPLIANT = "{\"schemaVersion\":\"1.0\",\"followsInstructions\":true,\"confidence\":0.92,"
                    + "\"severity\":\"none\",\"reason\":\"Cumple la consigna.\",\"violations\":[],\"checkedConstraints\":[]}";

            private final Set<String> breaching;

            private final List<String> asked = new ArrayList<>();

            FakeJudge(String... breaching) {
                this.breaching = Set.of(breaching);
            }

            List<String> asked() {
                return asked;
            }

            @Override
            public String evaluatorId() {
                return ID;
            }

            @Override
            public Optional<String> evaluatorVersion() {
                return Optional.of("2026-09-30");
            }

            @Override
            public EvaluationOutcome evaluate(EvaluationSubject subject) {
                asked.add(subject.getSubjectRef());
                return new EvaluationEmitted(breaching.contains(subject.getSubjectRef()) ? BREACH : COMPLIANT);
            }
        }

        /** The ledger kept in memory, for the runs whose verdicts are not what the test reads. */
        static final class MapLedger implements EvaluationLedger {
            private final Map<EvaluationKey, List<EvaluationRecord>> records = new HashMap<>();

            @Override
            public Optional<EvaluationRecord> findLatest(EvaluationKey key) {
                List<EvaluationRecord> recorded = records.get(key);
                return recorded == null ? Optional.empty() : Optional.of(recorded.get(recorded.size() - 1));
            }

            @Override
            public void append(EvaluationRecord record) {
                records.computeIfAbsent(record.getKey(), k -> new ArrayList<>()).add(record);
            }

            @Override
            public List<EvaluationRecord> history(EvaluationKey key) {
                return new ArrayList<>(records.getOrDefault(key, List.of()));
            }
        }

        /** The spaCy tokens each canonical sentence of the 29/9 course had in the analysis 2026-09-30T11-54-02. */
        static final class RecordedTokenizer implements NlpTokenizer {
            private final Map<String, List<NlpToken>> bySentence = new HashMap<>();

            RecordedTokenizer() {
                ObjectMapper json = new ObjectMapper();
                List<NlpToken> table = new ArrayList<>();
                for (String line : readFixture(FIXTURES.resolve("tokens.jsonl.gz"))) {
                    JsonNode t = tree(json, line);
                    table.add(new NlpToken(t.get(0).asText(), t.get(1).asText(), t.get(2).asText(),
                            t.get(3).isNull() ? null : t.get(3).asInt(), t.get(4).asBoolean(), t.get(5).asBoolean()));
                }
                for (String line : readFixture(FIXTURES.resolve("sentences.jsonl.gz"))) {
                    JsonNode s = tree(json, line);
                    List<NlpToken> tokens = new ArrayList<>();
                    s.get(1).forEach(index -> tokens.add(table.get(index.asInt())));
                    bySentence.put(s.get(0).asText(), tokens);
                }
            }

            @Override
            public Map<String, List<NlpToken>> analyzeTokensBatch(List<String> sentences) {
                Map<String, List<NlpToken>> result = new HashMap<>();
                for (String sentence : sentences) {
                    List<NlpToken> tokens = bySentence.get(sentence);
                    if (tokens == null) {
                        throw new AssertionError("a sentence the 29/9 course did not have: " + sentence);
                    }
                    result.put(sentence, tokens);
                }
                return result;
            }

            @Override
            public List<String> tokenize(String text) {
                throw new UnsupportedOperationException("the audit tokenizes in batch");
            }

            @Override
            public int countTokens(String text) {
                throw new UnsupportedOperationException("the audit tokenizes in batch");
            }

            @Override
            public List<NlpToken> analyzeTokens(String text) {
                throw new UnsupportedOperationException("the audit tokenizes in batch");
            }
        }

        /**
         * Stands in for a rule-based errors analyzer as package D will add them -- instant, errors
         * family, on the quiz -- since the judge is the only errors analyzer today. It breaks its
         * rule in the quizzes it is given, marks without counting as an error the ones it only
         * ranks, and leaves every other quiz at 1. Like a new analyzer package E has not wired yet,
         * its card declares no task kind.
         */
        static final class RuleStandIn implements AnalyzerProvider {
            static final String NAME = "rule-stand-in";

            static final String BREAKS = "breaks-rule";

            static final String RANKS = "ranks-only";

            private final Set<String> breaking;

            private final Set<String> ranked;

            RuleStandIn(Set<String> breaking, Set<String> ranked) {
                this.breaking = breaking;
                this.ranked = ranked;
            }

            @Override
            public String analyzerName() {
                return NAME;
            }

            @Override
            public AnalyzerDescriptor describe() {
                return new AnalyzerDescriptor(NAME, "Stand-in of a rule-based errors analyzer", AuditTarget.QUIZ,
                        "¿El ejercicio rompe la regla?", "La oración del ejercicio",
                        List.of(new AnalyzerRuleCard(BREAKS, "El ejercicio rompe la regla", AnalysisCost.INSTANT),
                                new AnalyzerRuleCard(RANKS, "El ejercicio baja en la lista, sin error",
                                        AnalysisCost.INSTANT)),
                        "Ningún ejercicio rompe la regla", AnalyzerFamily.ERRORS, List.of(AuditTarget.QUIZ),
                        List.of(FindingResolution.RULE, FindingResolution.RANK_ONLY), AnalysisCost.INSTANT);
            }

            @Override
            public ContentAnalyzer create(EvaluationRunPolicy policy) {
                return new ContentAnalyzer() {
                    @Override
                    public Void onQuiz(AuditNode node) {
                        node.getScores().put(NAME, breaking.contains(node.getEntity().getId()) ? 0.0 : 1.0);
                        return null;
                    }

                    @Override
                    public Void onKnowledge(AuditNode node) {
                        return null;
                    }

                    @Override
                    public Void onMilestone(AuditNode node) {
                        return null;
                    }

                    @Override
                    public Void onTopic(AuditNode node) {
                        return null;
                    }

                    @Override
                    public Void onCourseComplete(AuditNode rootNode) {
                        return null;
                    }

                    @Override
                    public String getName() {
                        return NAME;
                    }

                    @Override
                    public AuditTarget getTarget() {
                        return AuditTarget.QUIZ;
                    }

                    @Override
                    public String getDescription() {
                        return "Stand-in of a rule-based errors analyzer";
                    }

                    @Override
                    public List<FindingDraft> findingsAt(AuditNode node) {
                        String id = node.getEntity().getId();
                        if (breaking.contains(id)) {
                            return List.of(draft(BREAKS, FindingSeverity.MEDIUM, FindingResolution.RULE, id,
                                    "Rompe la regla"));
                        }
                        if (ranked.contains(id)) {
                            return List.of(draft(RANKS, FindingSeverity.LOW, FindingResolution.RANK_ONLY, id,
                                    "Baja en la lista, sin error"));
                        }
                        return List.of();
                    }
                };
            }

            private static FindingDraft draft(String rule, FindingSeverity severity, FindingResolution resolution,
                    String quizId, String observation) {
                return new FindingDraft(rule, null, severity, resolution,
                        new FindingEvidence(List.of(new EvidencePart("Ejercicio", quizId)), observation, List.of()));
            }

            @Override
            public Optional<AnalyzerPlanBinding> planBinding() {
                return Optional.empty();
            }

            @Override
            public Optional<SelfDescribingConfig> config() {
                return Optional.empty();
            }
        }
    }
}
