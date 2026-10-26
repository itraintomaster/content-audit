package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.revisiondomain.changereview.ChangeReview;
import com.learney.contentaudit.revisiondomain.changereview.ChangeReviewService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** FEAT-MARCA: the reviewer's marks over what a batch changed; without --budget it never consults. */
@Command(name = "mark", mixinStandardHelpOptions = true,
        description = "Marca bien, dudoso o mal cada ejercicio que la tanda cambió, con producción al lado. Sin --budget "
                + "no consulta: muestra las marcas que hay y cuántas consultas faltan. No cambia ninguna decisión.")
public final class ChangeReviewCmd implements Callable<Integer> {
    private final Supplier<ChangeReviewService> service;
    @Parameters(index = "0", description = "La tanda.") String batchId;
    @Option(names = "--quiz", description = "Sólo estos ejercicios; se repite.") List<String> quizIds = new ArrayList<>();
    @Option(names = "--budget", description = "Tope de consultas nuevas al revisor.") Integer budget;
    @Option(names = "--concurrency", description = "Consultas a la vez (4 si no se dice).") Integer concurrency;
    @Option(names = {"-f", "--format"}, defaultValue = "text") String format;
    @Spec Model.CommandSpec spec;

    public ChangeReviewCmd(Supplier<ChangeReviewService> service) { this.service = service; }

    @Override public Integer call() {
        try {
            var result = service.get().review(new ChangeReview.Request(batchId, quizIds, budget, concurrency));
            var out = spec.commandLine().getOut();
            out.print(CommandJson.isJson(format) ? CommandJson.write(result) + "\n" : text(result));
            // The process ends with System.exit: what is not flushed is lost.
            out.flush();
            return 0;
        } catch (IllegalArgumentException | IllegalStateException e) {
            spec.commandLine().getErr().println("Error: " + e.getMessage());
            return 2;
        }
    }

    static String text(ChangeReview.Result result) {
        StringBuilder out = new StringBuilder();
        out.append("Revisor de cambios · tanda ").append(result.batchId()).append(" · versión ")
                .append(shortVersion(result.reviewerVersion())).append(result.runtime() == null ? "" : " · " + result.runtime())
                .append('\n');
        out.append(result.changed()).append(result.changed() == 1 ? " ejercicio cambiado: " : " ejercicios cambiados: ")
                .append(result.upToDate()).append(" con marca de esta versión, ").append(result.pending())
                .append(" por pedir.");
        out.append(result.budget() == null ? " Sin --budget no consulto.\n"
                : " Esta vez: " + result.spent() + (result.spent() == 1 ? " consulta" : " consultas") + " de un tope de "
                        + result.budget() + ".\n");
        if (result.unavailableReason() != null) out.append("El revisor no está disponible: ").append(result.unavailableReason()).append('\n');
        group(out, result.rows(), "mal", ChangeReview.Mark.WORSE);
        group(out, result.rows(), "dudoso", ChangeReview.Mark.DOUBTFUL);
        List<ChangeReview.Row> unmarked = result.rows().stream().filter(r -> r.verdict() == null).toList();
        if (!unmarked.isEmpty()) {
            out.append("sin marca (").append(unmarked.size()).append("):\n");
            for (ChangeReview.Row row : unmarked)
                out.append("  ").append(row.quizId()).append(" · ").append(row.topic()).append(": ").append(row.detail()).append('\n');
        }
        List<ChangeReview.Row> good = result.rows().stream()
                .filter(r -> r.verdict() != null && r.verdict().mark() == ChangeReview.Mark.GOOD).toList();
        if (!good.isEmpty()) {
            out.append("bien (").append(good.size()).append("):\n");
            for (ChangeReview.Row row : good)
                out.append("  ").append(row.quizId()).append(" · ").append(row.topic()).append(older(row)).append('\n');
        }
        return out.toString();
    }

    private static void group(StringBuilder out, List<ChangeReview.Row> rows, String name, ChangeReview.Mark mark) {
        List<ChangeReview.Row> marked = rows.stream().filter(r -> r.verdict() != null && r.verdict().mark() == mark).toList();
        if (marked.isEmpty()) return;
        out.append(name).append(" (").append(marked.size()).append("):\n");
        for (ChangeReview.Row row : marked) {
            out.append("  ").append(row.quizId()).append(" · ").append(row.topic()).append(older(row)).append(": ")
                    .append(row.verdict().reason()).append('\n');
            for (ChangeReview.Problem problem : row.verdict().problems())
                out.append("    - ").append(where(problem.where())).append(" «").append(problem.fragment()).append("»: ")
                        .append(problem.what()).append('\n');
        }
    }

    private static String older(ChangeReview.Row row) {
        return row.state() == ChangeReview.State.OLDER_VERSION ? " (versión anterior " + shortVersion(row.reviewerVersion()) + ")" : "";
    }

    private static String where(ChangeReview.Where where) {
        return switch (where) {
            case INSTRUCTIONS -> "consigna";
            case EXERCISE -> "ejercicio";
            case ANSWERS -> "respuestas";
            case OPTIONS -> "opciones";
            case TRANSLATION -> "traducción";
            case MINI_THEORY -> "miniteoría";
        };
    }

    private static String shortVersion(String version) {
        return version == null ? "desconocida" : version.length() > 8 ? version.substring(0, 8) : version;
    }
}
