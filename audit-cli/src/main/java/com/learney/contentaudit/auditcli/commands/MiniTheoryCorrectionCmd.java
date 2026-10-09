package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.revisiondomain.minitheorycorrection.*;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import picocli.CommandLine.*;

@Command(name = "mini-theory", description = "Borradores dirigidos dentro de Correcciones; nunca publica ni crea otra corrección.")
public final class MiniTheoryCorrectionCmd implements Callable<Integer> {
    private final Supplier<MiniTheoryCorrectionService> service;
    @Option(names = "--request-json", required = true) String request;
    @Option(names = {"-f", "--format"}, defaultValue = "json") String format;
    @Spec Model.CommandSpec spec;
    public MiniTheoryCorrectionCmd(Supplier<MiniTheoryCorrectionService> service) { this.service = service; }
    @Override public Integer call() {
        try {
            if (!"json".equals(format)) throw new IllegalArgumentException("Esta operación devuelve JSON.");
            var input = CommandJson.read(request, MiniTheoryCorrection.Request.class);
            var result = service.get().execute(input);
            spec.commandLine().getOut().println(write(result));
            return 0;
        } catch (IllegalArgumentException | IllegalStateException e) {
            spec.commandLine().getErr().println("Error: " + e.getMessage()); return 2;
        }
    }
    static String write(Object state) {
        try { return com.learney.contentaudit.auditinfrastructure.AuditReportObjectMapper.create().writeValueAsString(state); }
        catch (com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException("No se pudo devolver el estado de miniteoría.", e); }
    }
}
