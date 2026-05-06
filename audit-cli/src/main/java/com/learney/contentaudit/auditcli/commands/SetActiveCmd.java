package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.auditcli.SetActiveAnalysisCommand;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "set-active",
        description = "Set or clear the active analysis pair (auditId, planId) for the consolidated view.",
        mixinStandardHelpOptions = true,
        footer = {
                "",
                "Examples:",
                "  # Set active analysis",
                "  content-audit set-active --audit 2026-05-01T10-00-00 --plan 2026-05-01T10-05-00",
                "",
                "  # Clear active analysis",
                "  content-audit set-active --clear",
        }
)
final class SetActiveCmd implements Callable<Integer> {

    private final SetActiveAnalysisCommand delegate;

    @Option(names = {"--audit"}, description = "Audit id to set as active.")
    private String auditId;

    @Option(names = {"--plan"}, description = "Plan id to set as active.")
    private String planId;

    @Option(names = {"--clear"}, description = "Clear the active analysis selection.")
    private boolean clear;

    SetActiveCmd(SetActiveAnalysisCommand delegate) {
        this.delegate = delegate;
    }

    @Override
    public Integer call() {
        if (clear) {
            return delegate.setActive(null, null);
        }
        if (auditId == null || planId == null) {
            System.err.println("Error: --audit and --plan are required (or use --clear to unset).");
            return 1;
        }
        return delegate.setActive(auditId, planId);
    }
}
