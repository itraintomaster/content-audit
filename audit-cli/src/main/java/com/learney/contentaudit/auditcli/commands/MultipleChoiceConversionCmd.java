package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.revisiondomain.conversion.MultipleChoiceConversion;
import com.learney.contentaudit.revisiondomain.conversion.MultipleChoiceConversionService;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import picocli.CommandLine.*;

@Command(name = "conversion", mixinStandardHelpOptions = true,
        description = "Sugiere opción múltiple con opciones existentes o restaura el cloze guardado. No consulta modelos.")
public final class MultipleChoiceConversionCmd implements Callable<Integer> {
    private final Supplier<MultipleChoiceConversionService> service;
    @Option(names = "--request-json", required = true) String request;
    @Option(names = {"-f", "--format"}, defaultValue = "json") String format;
    @Spec Model.CommandSpec spec;
    public MultipleChoiceConversionCmd(Supplier<MultipleChoiceConversionService> service) { this.service = service; }
    @Override public Integer call() {
        try {
            if (!"json".equals(format)) throw new IllegalArgumentException("Esta operación devuelve JSON.");
            var input = CommandJson.read(request, MultipleChoiceConversion.Request.class);
            spec.commandLine().getOut().println(CommandJson.writeCompact(service.get().execute(input)));
            return 0;
        } catch (IllegalArgumentException | IllegalStateException e) {
            spec.commandLine().getErr().println("Error: " + e.getMessage()); return 2;
        }
    }
}
