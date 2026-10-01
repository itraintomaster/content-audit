package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.auditcli.BlindSampleCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/** FEAT-RONDA2, package F (hook F-06): {@code sample draw|show|answer|close}, over {@link BlindSampleCommand}. */
@Command(
        name = "sample",
        description = "The blind sample of a batch (F-CIEGA).%n%n"
                + "  sample draw <course> [--proposal <id>]... [--seed N]%n"
                + "  sample show <sampleId>%n"
                + "  sample answer <sampleId> <pair> <A|B|EQUAL|BOTH_WRONG> --reviewer <name>%n"
                + "  sample close <sampleId>%n%n"
                + "Answers, without minding case: A, B, EQUAL (iguales) or BOTH_WRONG (ambos).",
        mixinStandardHelpOptions = true,
        footer = {
                "",
                "Examples:",
                "  content-audit sample draw db/english-course --seed 20261001",
                "  content-audit sample show muestra-2026-10-01T10-00-00",
                "  content-audit sample answer muestra-2026-10-01T10-00-00 1 B --reviewer José",
                "  content-audit sample close muestra-2026-10-01T10-00-00",
        }
)
final class SampleCmd implements Callable<Integer> {

    private final BlindSampleCommand delegate;

    @Parameters(index = "0", description = "draw, show, answer or close")
    private String action;

    @Parameters(index = "1..*", arity = "0..*", description = "The course (draw) or the sample id, and for answer the"
            + " pair and the answer.")
    private List<String> args = new ArrayList<>();

    @Option(names = "--proposal", description = "A pending proposal of the batch (repeatable).")
    private List<String> proposalIds = new ArrayList<>();

    @Option(names = "--seed", description = "The seed to draw with; a new one, published, when none is given.")
    private Long seed;

    @Option(names = "--reviewer", description = "Who answers.")
    private String reviewer;

    @Option(names = {"-f", "--format"}, description = "Output format: text or json (default: ${DEFAULT-VALUE})",
            defaultValue = "text")
    private String format;

    SampleCmd(BlindSampleCommand delegate) {
        this.delegate = delegate;
    }

    @Override
    public Integer call() {
        if (action == null) {
            System.err.println("Error: sample needs draw, show, answer or close. See content-audit sample --help.");
            return 2;
        }
        return switch (action) {
            case "draw" -> args.size() == 1 ? delegate.draw(args.get(0), proposalIds, seed, format)
                    : usage("sample draw <course> [--proposal <id>]... [--seed N]");
            case "show" -> args.size() == 1 ? delegate.show(args.get(0), format) : usage("sample show <sampleId>");
            case "answer" -> answer();
            case "close" -> args.size() == 1 ? delegate.close(args.get(0), format) : usage("sample close <sampleId>");
            default -> {
                System.err.println("Error: unknown sample action '" + action + "'. Use draw, show, answer or close.");
                yield 2;
            }
        };
    }

    private Integer answer() {
        if (args.size() != 3 || reviewer == null || reviewer.isBlank()) {
            return usage("sample answer <sampleId> <pair> <A|B|EQUAL|BOTH_WRONG> --reviewer <name>");
        }
        int pair;
        try {
            pair = Integer.parseInt(args.get(1));
        } catch (NumberFormatException e) {
            System.err.println("Error: the pair is a number, got '" + args.get(1) + "'.");
            return 1;
        }
        return delegate.answer(args.get(0), pair, args.get(2), reviewer);
    }

    private static Integer usage(String usage) {
        System.err.println("Error: " + usage);
        return 2;
    }
}
