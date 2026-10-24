package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.revisiondomain.batch.CorrectionEvolution;
import com.learney.contentaudit.revisiondomain.batch.CorrectionEvolutionService;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/** Explicit version creation is separate from correction, measurement, and publication. */
@Command(name = "evolution", mixinStandardHelpOptions = true,
        description = "Consulta Base y correcciones, o prepara y crea la siguiente sin publicar ni consultar modelos.")
public final class CorrectionEvolutionCmd implements Callable<Integer> {
    private final Supplier<CorrectionEvolutionService> service;
    @Option(names = "--request-json", required = true) String request;
    @Option(names = {"-f", "--format"}, defaultValue = "json") String format;
    @Spec Model.CommandSpec spec;

    public CorrectionEvolutionCmd(Supplier<CorrectionEvolutionService> service) { this.service = service; }

    @Override public Integer call() {
        try {
            if (!"json".equals(format)) throw new IllegalArgumentException("Esta operación devuelve JSON.");
            var input = CommandJson.read(request, CorrectionEvolution.Request.class);
            spec.commandLine().getOut().println(CommandJson.writeCompact(service.get().execute(input)));
            return 0;
        } catch (IllegalArgumentException | IllegalStateException e) {
            spec.commandLine().getErr().println("Error: " + e.getMessage());
            return 2;
        }
    }
}
