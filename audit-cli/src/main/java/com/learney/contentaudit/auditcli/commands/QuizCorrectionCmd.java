package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.revisiondomain.batch.QuizCorrection;
import com.learney.contentaudit.revisiondomain.batch.QuizCorrectionService;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import picocli.CommandLine.*;

/** Explicit selected-quiz corrections, using all current judge findings in the same batch. */
@Command(name = "recorrect", mixinStandardHelpOptions = true,
        description = "Estima o vuelve a corregir quizzes seleccionados con todos sus problemas vigentes; no publica.")
public final class QuizCorrectionCmd implements Callable<Integer> {
    private final Supplier<QuizCorrectionService> service;
    @Option(names = "--request-json", required = true) String request;
    @Option(names = {"-f", "--format"}, defaultValue = "json") String format;
    @Spec Model.CommandSpec spec;

    public QuizCorrectionCmd(Supplier<QuizCorrectionService> service) { this.service = service; }

    @Override public Integer call() {
        try {
            if (!"json".equals(format)) throw new IllegalArgumentException("Esta operación devuelve JSON.");
            var input = CommandJson.read(request, QuizCorrection.Request.class);
            spec.commandLine().getOut().println(MiniTheoryCorrectionCmd.write(service.get().execute(input)));
            return 0;
        } catch (IllegalArgumentException | IllegalStateException e) {
            spec.commandLine().getErr().println("Error: " + e.getMessage());
            return 2;
        }
    }
}
