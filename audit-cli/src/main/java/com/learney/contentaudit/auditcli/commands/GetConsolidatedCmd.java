package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.auditcli.GetConsolidatedCommand;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
        name = "get-consolidated",
        description = "Show the consolidated view (baseline + accepted + pending proposals) for the active analysis.",
        mixinStandardHelpOptions = true,
        footer = {
                "",
                "Examples:",
                "  # Show consolidated view for the active analysis",
                "  content-audit get-consolidated db/english-course",
                "",
                "  # JSON format",
                "  content-audit get-consolidated db/english-course --format json",
                "",
                "  # With a budget of new consultations for an analyzer (F-PROYU-R003)",
                "  content-audit get-consolidated db/english-course --budget quiz-instruction=50",
        }
)
final class GetConsolidatedCmd implements Callable<Integer> {

    private final GetConsolidatedCommand delegate;

    @Parameters(index = "0", description = "Path to the course directory.")
    private String coursePath;

    @Option(names = {"-f", "--format"},
            description = "Output format (default: ${DEFAULT-VALUE})",
            defaultValue = "json")
    private String format;

    // FEAT-RONDA2, package F (hook F-06): the budget per analyzer of the projection.
    @Option(names = "--budget", description = "New consultations of an analyzer, as name=N (repeatable).")
    private java.util.Map<String, Integer> budgets = new java.util.LinkedHashMap<>();

    GetConsolidatedCmd(GetConsolidatedCommand delegate) {
        this.delegate = delegate;
    }

    @Override
    public Integer call() {
        if (budgets != null && !budgets.isEmpty()) {
            return delegate.getConsolidated(coursePath, format, budgets);
        }
        return delegate.getConsolidated(coursePath, format);
    }
}
