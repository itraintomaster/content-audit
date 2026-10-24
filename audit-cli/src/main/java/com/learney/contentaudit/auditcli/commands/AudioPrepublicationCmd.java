package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.revisiondomain.batch.AudioPrepublicationService;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import picocli.CommandLine.*;

@Command(name = "audio", mixinStandardHelpOptions = true,
        description = "Preparación de audios: inspecciona el registro de texto final, sin generar, consultar modelos ni publicar.")
public final class AudioPrepublicationCmd implements Callable<Integer> {
    private final Supplier<AudioPrepublicationService> service;
    @Option(names = "--request-json", required = true) String request;
    @Option(names = {"-f", "--format"}, defaultValue = "json") String format;
    @Spec Model.CommandSpec spec;
    public AudioPrepublicationCmd(Supplier<AudioPrepublicationService> service) { this.service = service; }
    @Override public Integer call() {
        try {
            if (!"json".equals(format)) throw new IllegalArgumentException("Esta operación devuelve JSON.");
            var input = CommandJson.read(request, AudioPrepublicationService.Request.class);
            spec.commandLine().getOut().println(CommandJson.writeCompact(service.get().execute(input)));
            return 0;
        } catch (IllegalArgumentException | IllegalStateException e) {
            spec.commandLine().getErr().println("Error: " + e.getMessage()); return 2;
        }
    }
}
