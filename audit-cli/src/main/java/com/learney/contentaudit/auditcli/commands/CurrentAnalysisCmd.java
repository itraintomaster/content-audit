package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.revisiondomain.batch.CurrentAnalysisService;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import picocli.CommandLine.*;

@Command(name = "analysis", mixinStandardHelpOptions = true,
        description = "Análisis vigente por estado: leer, integrar, reemplazar o quitar corridas guardadas, sin consultas.")
public final class CurrentAnalysisCmd implements Callable<Integer> {
    private final Supplier<CurrentAnalysisService> service;
    @Option(names = "--request-json", required = true) String request;
    @Option(names = {"-f", "--format"}, defaultValue = "json") String format;
    @Spec Model.CommandSpec spec;
    public CurrentAnalysisCmd(Supplier<CurrentAnalysisService> service) { this.service = service; }
    @Override public Integer call() {
        try {
            if (!"json".equals(format)) throw new IllegalArgumentException("Esta operación devuelve JSON.");
            var input = CommandJson.read(request, CurrentAnalysisService.Request.class);
            spec.commandLine().getOut().println(CommandJson.writeCompact(service.get().execute(input)));
            return 0;
        } catch (IllegalArgumentException | IllegalStateException e) {
            spec.commandLine().getErr().println("Error: " + e.getMessage()); return 2;
        }
    }
}
