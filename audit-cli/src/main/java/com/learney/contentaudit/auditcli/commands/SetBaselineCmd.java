package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.auditcli.SetBaselineCommand;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/** FEAT-RONDA2, package F (hook F-06): {@code set-baseline}, over {@link SetBaselineCommand}. */
@Command(
        name = "set-baseline",
        description = "Fix an analysis of the history as the baseline every batch is compared with (F-PROYU-R006),"
                + " or show the one fixed.",
        mixinStandardHelpOptions = true,
        footer = {
                "",
                "Examples:",
                "  content-audit set-baseline 2026-09-30T11-54-02",
                "  content-audit set-baseline --show",
                "  content-audit set-baseline --show -f json",
        }
)
final class SetBaselineCmd implements Callable<Integer> {

    private final SetBaselineCommand delegate;

    @Parameters(index = "0", arity = "0..1", description = "The id of an analysis of the history.")
    private String auditId;

    @Option(names = "--show", description = "Show the baseline fixed instead of fixing one.")
    private boolean show;

    @Option(names = {"-f", "--format"}, description = "Output format of --show: text or json (default: ${DEFAULT-VALUE})",
            defaultValue = "text")
    private String format;

    SetBaselineCmd(SetBaselineCommand delegate) {
        this.delegate = delegate;
    }

    @Override
    public Integer call() {
        if (show) {
            return delegate.showBaseline(format);
        }
        if (auditId == null || auditId.isBlank()) {
            System.err.println("Error: set-baseline <auditId>, or set-baseline --show.");
            return 1;
        }
        return delegate.setBaseline(auditId);
    }
}
