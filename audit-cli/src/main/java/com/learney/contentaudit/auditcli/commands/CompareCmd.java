package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.auditcli.CompareCommand;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/** FEAT-RONDA2, package F (hook F-06): {@code compare}, over {@link CompareCommand}. */
@Command(
        name = "compare",
        description = "Compare a batch with the baseline: the before and after of every analyzer in each knowledge,"
                + " topic, level and the course, the goals, what returns and the verdict (F-METAS). Writes nothing.",
        mixinStandardHelpOptions = true,
        footer = {
                "",
                "Examples:",
                "  content-audit compare db/english-course",
                "  content-audit compare db/english-course --proposal 2026-09-30T12-00-00-task-042 -f json",
                "  content-audit compare db/english-course --budget quiz-instruction=50",
        }
)
final class CompareCmd implements Callable<Integer> {

    private final CompareCommand delegate;

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

    CompareCmd(CompareCommand delegate) {
        this.delegate = delegate;
    }

    @Override
    public Integer call() {
        return delegate.compare(coursePath, proposalIds, budgets, format);
    }
}
