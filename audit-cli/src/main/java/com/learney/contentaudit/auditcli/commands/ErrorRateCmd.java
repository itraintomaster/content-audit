package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.auditcli.ErrorRateCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/** FEAT-RONDA2, package F (hook F-06): {@code error-rate draw|show|mark|close}, over {@link ErrorRateCommand}. */
@Command(
        name = "error-rate",
        description = "The blind error rate of a level or of the course (F-CIEGA-R006).%n%n"
                + "  error-rate draw <course> <A1|A2|B1|B2|curso> <n> [--seed N]%n"
                + "  error-rate show <sampleId>%n"
                + "  error-rate mark <sampleId> <item> <si|no> --reviewer <name>%n"
                + "  error-rate close <sampleId>",
        mixinStandardHelpOptions = true,
        footer = {
                "",
                "Examples:",
                "  content-audit error-rate draw db/english-course curso 400 --seed 7",
                "  content-audit error-rate mark tasa-2026-10-01T10-00-00 1 si --reviewer tester",
                "  content-audit error-rate close tasa-2026-10-01T10-00-00",
        }
)
final class ErrorRateCmd implements Callable<Integer> {

    private final ErrorRateCommand delegate;

    @Parameters(index = "0", description = "draw, show, mark or close")
    private String action;

    @Parameters(index = "1..*", arity = "0..*", description = "The course, the level and n (draw), or the sample id,"
            + " and for mark the item and si|no.")
    private List<String> args = new ArrayList<>();

    @Option(names = "--seed", description = "The seed to draw with; a new one, published, when none is given.")
    private Long seed;

    @Option(names = "--reviewer", description = "Who marks.")
    private String reviewer;

    @Option(names = {"-f", "--format"}, description = "Output format: text or json (default: ${DEFAULT-VALUE})",
            defaultValue = "text")
    private String format;

    ErrorRateCmd(ErrorRateCommand delegate) {
        this.delegate = delegate;
    }

    @Override
    public Integer call() {
        if (action == null) {
            System.err.println("Error: error-rate needs draw, show, mark or close. See content-audit error-rate --help.");
            return 2;
        }
        return switch (action) {
            case "draw" -> draw();
            case "show" -> args.size() == 1 ? delegate.show(args.get(0), format) : usage("error-rate show <sampleId>");
            case "mark" -> mark();
            case "close" -> args.size() == 1 ? delegate.close(args.get(0), format)
                    : usage("error-rate close <sampleId>");
            default -> {
                System.err.println("Error: unknown error-rate action '" + action + "'. Use draw, show, mark or close.");
                yield 2;
            }
        };
    }

    private Integer draw() {
        if (args.size() != 3) {
            return usage("error-rate draw <course> <A1|A2|B1|B2|curso> <n> [--seed N]");
        }
        int size;
        try {
            size = Integer.parseInt(args.get(2));
        } catch (NumberFormatException e) {
            System.err.println("Error: n is a number, got '" + args.get(2) + "'.");
            return 1;
        }
        return delegate.draw(args.get(0), args.get(1), size, seed, format);
    }

    private Integer mark() {
        if (args.size() != 3 || reviewer == null || reviewer.isBlank()) {
            return usage("error-rate mark <sampleId> <item> <si|no> --reviewer <name>");
        }
        int item;
        try {
            item = Integer.parseInt(args.get(1));
        } catch (NumberFormatException e) {
            System.err.println("Error: the item is a number, got '" + args.get(1) + "'.");
            return 1;
        }
        String mark = args.get(2).trim().toLowerCase(Locale.ROOT);
        boolean hasError;
        if (List.of("si", "sí", "yes", "error").contains(mark)) {
            hasError = true;
        } else if (List.of("no", "ok").contains(mark)) {
            hasError = false;
        } else {
            System.err.println("Error: mark an item with si (it has an error) or no, got '" + args.get(2) + "'.");
            return 1;
        }
        return delegate.mark(args.get(0), item, hasError, reviewer);
    }

    private static Integer usage(String usage) {
        System.err.println("Error: " + usage);
        return 2;
    }
}
