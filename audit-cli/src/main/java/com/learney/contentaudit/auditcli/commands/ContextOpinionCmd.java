package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.revisiondomain.batch.*;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import picocli.CommandLine.*;

@Command(name = "opinion", mixinStandardHelpOptions = true,
        description = "Opinión acotada a ejercicios y jueces; estima gratis, mide con tope, no cierra ni publica.")
public final class ContextOpinionCmd implements Callable<Integer> {
    private final Supplier<ContextOpinionService> service;
    @Option(names = "--request-json", required = true) String request;
    @Option(names = {"-f", "--format"}, defaultValue = "json") String format;
    @Spec Model.CommandSpec spec;
    public ContextOpinionCmd(Supplier<ContextOpinionService> service) { this.service = service; }
    @Override public Integer call() {
        try {
            if (!"json".equals(format)) throw new IllegalArgumentException("Esta operación devuelve JSON.");
            var input = CommandJson.read(request, ContextOpinion.Request.class);
            spec.commandLine().getOut().println(MiniTheoryCorrectionCmd.write(service.get().execute(input)));
            return 0;
        } catch (IllegalArgumentException | IllegalStateException e) {
            spec.commandLine().getErr().println("Error: " + e.getMessage()); return 2;
        }
    }
}
