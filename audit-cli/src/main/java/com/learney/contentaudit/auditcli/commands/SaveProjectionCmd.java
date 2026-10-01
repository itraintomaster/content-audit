package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.auditcli.SaveProjectionCommand;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/** FEAT-RONDA2, package F (hook F-06): {@code save-projection}, over {@link SaveProjectionCommand}. */
@Command(
        name = "save-projection",
        description = "Save the photo of the course with the pending proposals of the active pair, under an id that"
                + " 'plan --audit' accepts (F-PROYU-R005). The history of analyses does not change.",
        mixinStandardHelpOptions = true,
        footer = {
                "",
                "Examples:",
                "  content-audit save-projection db/english-course",
                "  content-audit save-projection db/english-course --proposal 2026-09-30T12-00-00-task-042",
                "  content-audit save-projection db/english-course --budget quiz-instruction=50",
        }
)
final class SaveProjectionCmd implements Callable<Integer> {

    private final SaveProjectionCommand delegate;

    @Parameters(index = "0", description = "Path to the course directory.")
    private String coursePath;

    @Option(names = "--proposal", description = "A pending proposal of the batch (repeatable); every one of the"
            + " active plan when none is named.")
    private List<String> proposalIds = new ArrayList<>();

    @Option(names = "--budget", description = "New consultations of an analyzer, as name=N (repeatable).")
    private Map<String, Integer> budgets = new LinkedHashMap<>();

    @Option(names = {"-f", "--format"}, description = "Output format: text or json (default: ${DEFAULT-VALUE})",
            defaultValue = "text")
    private String format;

    SaveProjectionCmd(SaveProjectionCommand delegate) {
        this.delegate = delegate;
    }

    @Override
    public Integer call() {
        return delegate.saveProjection(coursePath, proposalIds, budgets, format);
    }
}
