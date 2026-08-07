package com.learney.contentaudit.auditcli.commands;
import com.learney.contentaudit.auditcli.bootstrap.ReevaluationQuizSetResolver;
import com.learney.contentaudit.auditcli.AnalyzeOptions;
import javax.annotation.processing.Generated;

import com.learney.contentaudit.auditapplication.AuditRunRequest;
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
import com.learney.contentaudit.auditdomain.EvaluationRunPolicy;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;
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
class AnalyzeCmd implements AnalyzeCommand, Callable<Integer> {

    // F-QINST-R011/R006/R012: analyzer name under which DefaultAuditRunner looks up
    // the per-run EvaluationRunPolicy (DefaultQuizInstructionAnalyzerFactory.analyzerName()
    // is this same name, the one shown in the report -- not the judge's evaluatorId).
    private static final String QUIZ_INSTRUCTION_ANALYZER_NAME = "quiz-instruction";

    private final AuditRunner auditRunner;

    private final FormatterRegistry formatterRegistry;

    private final ReportViewModelTransformer viewModelTransformer;

    private final RawReportFormatter rawReportFormatter;

    private final DrillDownResolver drillDownResolver;

    private final Map<String, DetailedFormatter> detailedFormatters;

    private final AuditReportStore auditReportStore;

    private final Function<String, String> pathResolver;

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

    @Option(names = {"--exclude-analyzers"},
            description = "Comma-separated list of analyzer names to exclude from the run "
                    + "(e.g. quiz-instruction, to skip the costly quiz-instruction judge).",
            split = ",")
    private List<String> excludeAnalyzers;

    @Option(names = {"--instruction-budget"},
            description = "Maximum number of new quiz-instruction judge queries for this run. "
                    + "Already-emitted verdicts are reused for free and never count against this budget.")
    private Integer instructionBudget;

    @Option(names = {"--reevaluate-instructions"},
            description = "Explicitly re-judge quiz-instruction verdicts that are already valid. "
                    + "With no value, re-evaluates the whole course; with a knowledge id, narrows "
                    + "the re-evaluation to that knowledge's quizzes only. Still respects "
                    + "--instruction-budget.",
            arity = "0..1",
            fallbackValue = "")
    private String reevaluateInstructions;

    @Option(names = {"--reevaluate-instruction-quizzes"},
            description = "Comma-separated set of quiz ids to explicitly re-judge, declared inline "
                    + "(F-QINST-R018/R019). A declared set IS the re-evaluation request: it narrows "
                    + "the run to exactly these quizzes instead of an area or the whole course, and "
                    + "is mutually exclusive with both --reevaluate-instructions and "
                    + "--reevaluate-instruction-quizzes-file. Still respects --instruction-budget, "
                    + "which is spent on the declared set first.")
    private String reevaluateInstructionQuizzes;

    @Option(names = {"--reevaluate-instruction-quizzes-file"},
            description = "Path to a file listing (comma-separated) the set of quiz ids to "
                    + "explicitly re-judge (F-QINST-R018/R019), for sets too large to enumerate "
                    + "inline. Mutually exclusive with --reevaluate-instruction-quizzes.")
    private String reevaluateInstructionQuizzesFile;

private final ReevaluationQuizSetResolver reevaluationQuizSetResolver;

public AnalyzeCmd(AuditRunner auditRunner, FormatterRegistry formatterRegistry, ReportViewModelTransformer viewModelTransformer, RawReportFormatter rawReportFormatter, DrillDownResolver drillDownResolver, Map<String, DetailedFormatter> detailedFormatters, AuditReportStore auditReportStore, Function<String, String> pathResolver, ReevaluationQuizSetResolver reevaluationQuizSetResolver) {
    this.auditRunner = auditRunner;
    this.formatterRegistry = formatterRegistry;
    this.viewModelTransformer = viewModelTransformer;
    this.rawReportFormatter = rawReportFormatter;
    this.drillDownResolver = drillDownResolver;
    this.detailedFormatters = detailedFormatters;
    this.auditReportStore = auditReportStore;
    this.pathResolver = pathResolver;
    this.reevaluationQuizSetResolver = reevaluationQuizSetResolver;
}

    @Override
    public Integer call() {
        try {
            // F-QINST-R018/R019: resolution (and any I/O it needs to read an external
            // origin) happens here, in call() -- analyze(path, options) stays free of
            // I/O and takes the set already resolved. Absent BOTH options, no set is
            // declared at all: this must stay null, never an empty set, or an ordinary
            // audit that never asked for a re-evaluation would be rejected downstream
            // as an empty declared set (F-QINST-R018 inv.3).
            Set<String> reevaluateInstructionQuizIds =
                    this.reevaluateInstructionQuizzes != null || this.reevaluateInstructionQuizzesFile != null
                            ? reevaluationQuizSetResolver.resolve(
                                    this.reevaluateInstructionQuizzes, this.reevaluateInstructionQuizzesFile)
                            : null;

            AnalyzeOptions options = new AnalyzeOptions(this.formatName, this.level, this.topic,
                    this.knowledge, this.analyzerFilter, this.excludeAnalyzers, this.detailed,
                    this.instructionBudget, this.reevaluateInstructions, reevaluateInstructionQuizIds);
            return analyze(this.coursePath, options);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        } catch (RuntimeException e) {
            System.err.println("Error running audit: " + e.getMessage());
            return 1;
        }
    }

    @Override
    public Integer analyze(String coursePath, String format, String level, String topic, String knowledge, List<String> analyzers, boolean detailed) {
        try {
            if (topic != null && level == null) {
                System.err.println("Error: --topic requires --level");
                return 1;
            }
            if (knowledge != null && topic == null) {
                System.err.println("Error: --knowledge requires --level and --topic");
                return 1;
            }

            String resolvedPath = pathResolver.apply(coursePath);
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

    @Override
    public Integer analyze(String coursePath, AnalyzeOptions options) {
        try {
            String level = options.getLevel();
            String topic = options.getTopic();
            String knowledge = options.getKnowledge();
            String format = options.getFormat();

            if (topic != null && level == null) {
                System.err.println("Error: --topic requires --level");
                return 1;
            }
            if (knowledge != null && topic == null) {
                System.err.println("Error: --knowledge requires --level and --topic");
                return 1;
            }

            String resolvedPath = pathResolver.apply(coursePath);
            if (resolvedPath == null) {
                System.err.println("Error: missing course path. Provide it as argument or set CONTENT_AUDIT_CONTENT_FOLDER.");
                return 1;
            }

            List<String> analyzers = options.getAnalyzers();

            // Detailed mode: bypass the report pipeline, use raw ScoredItems
            if (options.isDetailed()) {
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
                // La petición completa, no sólo el nombre: así el presupuesto y la
                // re-evaluación significan lo mismo en la vista detallada que en la
                // corrida normal (F-QINST-R006/R015). Con el nombre suelto, un
                // --instruction-budget 2 corría con el default de 500.
                AuditNode rootNode = auditRunner.runDetailedAudit(
                        Path.of(resolvedPath), toAuditRunRequest(options));
                System.out.println(detailedFormatter.format(analyzerName, rootNode, format));
                return 0;
            }

            AuditRunRequest request = toAuditRunRequest(options);

            AuditReport report = auditRunner.runAudit(Path.of(resolvedPath), request);

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

    /**
     * Translates the CLI-facing {@link AnalyzeOptions} into the {@link AuditRunRequest}
     * the audit pipeline understands.
     *
     * <ul>
     *   <li>F-QINST-R001: with no analyzer-related option at all, neither
     *       {@code excludedAnalyzers} nor the quiz-instruction run policy are populated,
     *       so the analysis runs unrestricted like any other.</li>
     *   <li>F-QINST-R011: {@code --exclude-analyzers} is carried verbatim into
     *       {@code excludedAnalyzers}.</li>
     *   <li>F-QINST-R006/R012: {@code --instruction-budget} and
     *       {@code --reevaluate-instructions[=SCOPE]} are only ever asked by the user for
     *       the quiz-instruction judge, so they are translated into that analyzer's
     *       {@link EvaluationRunPolicy} entry; absent both, no policy entry is created.</li>
     * </ul>
     */
    private AuditRunRequest toAuditRunRequest(AnalyzeOptions options) {
        List<String> analyzers = options.getAnalyzers();
        Set<String> includedNames = analyzers != null
                ? new LinkedHashSet<>(analyzers)
                : null;

        List<String> excludeAnalyzers = options.getExcludeAnalyzers();
        Set<String> excludedNames = excludeAnalyzers != null
                ? new LinkedHashSet<>(excludeAnalyzers)
                : null;

        Integer instructionBudget = options.getInstructionBudget();
        String reevaluateInstructions = options.getReevaluateInstructions();
        Set<String> reevaluateInstructionQuizIds = options.getReevaluateInstructionQuizIds();

        Map<String, EvaluationRunPolicy> analyzerPolicies = null;
        if (instructionBudget != null || reevaluateInstructions != null || reevaluateInstructionQuizIds != null) {
            EvaluationRunPolicy policy = new EvaluationRunPolicy();
            if (instructionBudget != null) {
                policy.setMaxNewEvaluations(instructionBudget);
            }
            if (reevaluateInstructions != null) {
                policy.setReevaluate(true);
                // An empty scope (--reevaluate-instructions with no value) means "the whole
                // course"; QuizInstructionScopeMatcher treats a null scope as unrestricted.
                policy.setReevaluationScope(reevaluateInstructions.isEmpty() ? null : reevaluateInstructions);
            }
            if (reevaluateInstructionQuizIds != null) {
                // F-QINST-R018: carried verbatim, never combined with reevaluate/scope above
                // by this translation -- if the user also passed --reevaluate-instructions,
                // the resulting policy legitimately declares both at once, and
                // DefaultQuizInstructionAnalyzerFactory.create() is the one that rejects that
                // combination (AmbiguousReevaluationScopeException) before opening the
                // session. Picking one here instead would hide the ambiguity from the user.
                policy.setReevaluationSubjectIds(reevaluateInstructionQuizIds);
            }
            analyzerPolicies = new HashMap<>();
            analyzerPolicies.put(QUIZ_INSTRUCTION_ANALYZER_NAME, policy);
        }

        return new AuditRunRequest(includedNames, excludedNames, analyzerPolicies);
    }

}
