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

    GetConsolidatedCmd(GetConsolidatedCommand delegate) {
        this.delegate = delegate;
    }

    @Override
    public Integer call() {
        return delegate.getConsolidated(coursePath, format);
    }
}
