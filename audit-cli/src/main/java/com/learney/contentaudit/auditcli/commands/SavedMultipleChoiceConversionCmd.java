package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.auditinfrastructure.FileSystemRefinementPlanStore;
import com.learney.contentaudit.auditinfrastructure.FileSystemRevisionArtifactStore;
import com.learney.contentaudit.auditinfrastructure.batchstore.FileSystemBatchStore;
import com.learney.contentaudit.courseinfrastructure.CourseValidatorImpl;
import com.learney.contentaudit.courseinfrastructure.FileSystemCourseRepository;
import com.learney.contentaudit.revisiondomain.batchengine.DefaultMultipleChoiceConversionServiceFactory;
import com.learney.contentaudit.revisiondomain.conversion.MultipleChoiceConversionService;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import picocli.CommandLine;

/** Entirely local bootstrap: no judge runtime, model discovery or NLP process, including help/errors. */
final class SavedMultipleChoiceConversionCmd {
    static Integer execute(List<String> args, Path directory, PrintStream out, PrintStream err) {
        if (args.size() < 2 || !args.get(0).equals("batch") || !args.get(1).equals("conversion")) return null;
        var command = new CommandLine(new MultipleChoiceConversionCmd(() -> create(directory)));
        command.setOut(new PrintWriter(out, true)); command.setErr(new PrintWriter(err, true));
        return command.execute(args.subList(2, args.size()).toArray(String[]::new));
    }
    static MultipleChoiceConversionService create(Path directory) {
        String path = CoursePathResolver.resolve(null);
        if (path == null) throw new IllegalArgumentException("Falta la ruta del curso.");
        return new DefaultMultipleChoiceConversionServiceFactory().create(new FileSystemBatchStore(directory),
                new FileSystemRefinementPlanStore(directory), new FileSystemRevisionArtifactStore(directory),
                new FileSystemCourseRepository(new CourseValidatorImpl()), Path.of(path));
    }
}
