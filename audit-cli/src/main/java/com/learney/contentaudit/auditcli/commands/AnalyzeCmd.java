package com.learney.contentaudit.auditcli.commands;
import javax.annotation.processing.Generated;

import com.learney.contentaudit.auditapplication.AuditRunner;
import com.learney.contentaudit.auditcli.AnalyzeCommand;
import com.learney.contentaudit.auditcli.formatting.DetailedFormatter;
import com.learney.contentaudit.auditcli.formatting.DrillDownResolver;
import com.learney.contentaudit.auditcli.formatting.DrillDownScope;
import com.learney.contentaudit.auditcli.formatting.FormatterRegistry;
import com.learney.contentaudit.auditcli.formatting.RawReportFormatter;
import com.learney.contentaudit.auditcli.formatting.ReportFormatter;
import com.learney.contentaudit.auditcli.formatting.ReportViewModel;
import com.learney.contentaudit.auditcli.formatting.ReportViewModelTransformer;
import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditReportStore;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
        name = "analyze",
        description = "Run a content audit on a course folder.%n%n"
                + "Executes all registered analyzers (or a filtered subset) and displays%n"
                + "scores per level. Use drill-down options to explore specific levels,%n"
                + "topics, or knowledge items.",
        mixinStandardHelpOptions = true,
        footer = {
                "",
                "Examples:",
                "  # Full audit with default text format",
                "  content-audit analyze db/english-course",
                "",
                "  # Drill into a specific CEFR level",
                "  content-audit analyze db/english-course --level A1",
                "",
                "  # Drill into a topic within a level",
                "  content-audit analyze db/english-course -l A1 -t \"Present Simple\"",
                "",
                "  # Drill into a knowledge item",
                "  content-audit analyze db/english-course -l A1 -t \"Present Simple\" -k \"Affirmative\"",
                "",
                "  # Use table format for a compact view",
                "  content-audit analyze db/english-course -f table",
                "",
                "  # Run only specific analyzers",
                "  content-audit analyze db/english-course --analyzers sentence-length,coca-buckets-distribution",
                "",
                "  # Export full raw audit data as JSON",
                "  content-audit analyze db/english-course -f raw",
                "",
                "  # Use env var for course path",
                "  CONTENT_AUDIT_CONTENT_FOLDER=db/english-course content-audit analyze",
        }
)
final class AnalyzeCmd implements AnalyzeCommand, Callable<Integer> {
    private final AuditRunner auditRunner;

    private final FormatterRegistry formatterRegistry;

    private final ReportViewModelTransformer viewModelTransformer;

    private final RawReportFormatter rawReportFormatter;

    private final DrillDownResolver drillDownResolver;

    private final Map<String, DetailedFormatter> detailedFormatters;

    private final AuditReportStore auditReportStore;

    @Parameters(index = "0", description = "Path to the course directory. "
            + "Defaults to CONTENT_AUDIT_CONTENT_FOLDER env var or .env file.",
            arity = "0..1")
    private String coursePath;

    @Option(names = {"-f", "--format"},
            description = "Output format: text, json, table, raw (default: ${DEFAULT-VALUE})",
            defaultValue = "text")
    private String formatName;

    @Option(names = {"-l", "--level"},
            description = "Drill into a CEFR level (e.g. A1, A2, B1, B2).")
    private String level;

    @Option(names = {"-t", "--topic"},
            description = "Drill into a topic within the selected level. Requires --level.")
    private String topic;

    @Option(names = {"-k", "--knowledge"},
            description = "Drill into a knowledge item. Requires --level and --topic.")
    private String knowledge;

    @Option(names = {"--analyzers"},
            description = "Comma-separated list of analyzer names to run.",
            split = ",")
    private List<String> analyzerFilter;

    @Option(names = {"--detailed"},
            description = "Show detailed analyzer output with metadata. Requires a single --analyzers value.")
    private boolean detailed;

    public AnalyzeCmd(AuditRunner auditRunner, FormatterRegistry formatterRegistry,
            ReportViewModelTransformer viewModelTransformer, RawReportFormatter rawReportFormatter,
            DrillDownResolver drillDownResolver,
            Map<String, DetailedFormatter> detailedFormatters,
            AuditReportStore auditReportStore) {
        this.auditRunner = auditRunner;
        this.formatterRegistry = formatterRegistry;
        this.viewModelTransformer = viewModelTransformer;
        this.rawReportFormatter = rawReportFormatter;
        this.drillDownResolver = drillDownResolver;
        this.detailedFormatters = detailedFormatters;
        this.auditReportStore = auditReportStore;
    }

    @Override
    public Integer call() {
        return analyze(this.coursePath, this.formatName, this.level, this.topic, this.knowledge, this.analyzerFilter, this.detailed);
    }

    @Override
    public int analyze(String coursePath, String format, String level, String topic, String knowledge, List<String> analyzers, boolean detailed) {
        try {
            if (topic != null && level == null) {
                System.err.println("Error: --topic requires --level");
                return 1;
            }
            if (knowledge != null && topic == null) {
                System.err.println("Error: --knowledge requires --level and --topic");
                return 1;
            }

            String resolvedPath = CoursePathResolver.resolve(coursePath);
            if (resolvedPath == null) {
                System.err.println("Error: missing course path. Provide it as argument or set CONTENT_AUDIT_CONTENT_FOLDER.");
                return 1;
            }

            // Detailed mode: bypass the report pipeline, use raw ScoredItems
            if (detailed) {
                if (analyzers == null || analyzers.size() != 1) {
                    System.err.println("Error: --detailed requires exactly one --analyzers value");
                    return 1;
                }
                String analyzerName = analyzers.get(0);
                DetailedFormatter detailedFormatter = detailedFormatters.get(analyzerName);
                if (detailedFormatter == null) {
                    System.err.println("No detailed view available for: " + analyzerName);
                    return 1;
                }
                AuditNode rootNode = auditRunner.runDetailedAudit(
                        Path.of(resolvedPath), analyzerName);
                System.out.println(detailedFormatter.format(analyzerName, rootNode, format));
                return 0;
            }

            Set<String> names = analyzers != null
                    ? new LinkedHashSet<>(analyzers)
                    : null;

            AuditReport report = auditRunner.runAudit(Path.of(resolvedPath), names);

            // Persist the audit report
            String auditId = auditReportStore.save(report);
            System.err.println("[Audit saved: " + auditId + "]");

            if ("raw".equals(format)) {
                System.out.println(rawReportFormatter.format(report));
                return 0;
            }

            ReportFormatter formatter = formatterRegistry.getFormatter(format);
            if (formatter == null) {
                System.err.println("Formato no soportado: " + format);
                return 1;
            }

            ReportViewModel viewModel = viewModelTransformer.transform(report);
            DrillDownScope scope = new DrillDownScope(
                    Optional.ofNullable(level),
                    Optional.ofNullable(topic),
                    Optional.ofNullable(knowledge)
            );
            System.out.println(formatter.format(viewModel, scope));
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        } catch (RuntimeException e) {
            System.err.println("Error running audit: " + e.getMessage());
            return 1;
        }
    }

}
