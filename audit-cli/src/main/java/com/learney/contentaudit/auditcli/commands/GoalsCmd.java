package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.auditcli.GoalsCommand;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** FEAT-RONDA2, package F (hook F-06): {@code get-goals} and {@code set-goals}, over {@link GoalsCommand}. */
final class GoalsCmd {
    private GoalsCmd() {
    }

    @Command(
            name = "get-goals",
            description = "List the effective goals: the declared ones and the floor of its family for every analyzer"
                    + " the declaration does not name, each with its origin (F-METAS-R005).",
            mixinStandardHelpOptions = true,
            footer = {"", "Examples:", "  content-audit get-goals", "  content-audit get-goals -f json"}
    )
    static final class Get implements Callable<Integer> {
        private final GoalsCommand delegate;

        @Option(names = {"-f", "--format"}, description = "Output format: text or json (default: ${DEFAULT-VALUE})",
                defaultValue = "text")
        private String format;

        Get(GoalsCommand delegate) {
            this.delegate = delegate;
        }

        @Override
        public Integer call() {
            return delegate.listGoals(format);
        }
    }

    @Command(
            name = "set-goals",
            description = "Seal the effective goals before a batch, whole and with their fingerprint (F-METAS-R006).",
            mixinStandardHelpOptions = true,
            footer = {"", "Examples:", "  content-audit set-goals"}
    )
    static final class Set implements Callable<Integer> {
        private final GoalsCommand delegate;

        @Option(names = {"-f", "--format"}, description = "Output format: text or json (default: ${DEFAULT-VALUE})",
                defaultValue = "text")
        private String format;

        Set(GoalsCommand delegate) {
            this.delegate = delegate;
        }

        @Override
        public Integer call() {
            return delegate.setGoals(format);
        }
    }
}
