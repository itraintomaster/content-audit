package com.learney.contentaudit.auditcli.commands;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

@Command(
    name = "refiner",
    description = "Refinement workflow commands.%n%n"
        + "Generate refinement plans from audit results and navigate%n"
        + "through prioritized improvement tasks.",
    mixinStandardHelpOptions = true,
    subcommands = {CommandLine.HelpCommand.class}
)
public class RefinerCmd implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}
