package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.auditinfrastructure.*;
import com.learney.contentaudit.auditinfrastructure.batchstore.FileSystemBatchStore;
import com.learney.contentaudit.auditinfrastructure.minitheory.FileSystemMiniTheoryWorkspaceStore;
import com.learney.contentaudit.courseinfrastructure.FileSystemCourseRepository;
import com.learney.contentaudit.courseinfrastructure.CourseValidatorImpl;
import com.learney.contentaudit.revisiondomain.batchengine.*;
import com.learney.contentaudit.revisiondomain.minitheorycorrection.MiniTheoryCorrection.Request;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

/** Local editing/read bootstrap: no models, NLP, exams or audit engine are even constructed. */
final class SavedMiniTheoryCorrectionCmd {
    private SavedMiniTheoryCorrectionCmd() {}
    static Integer execute(List<String> args, Path directory, PrintStream out, PrintStream err) {
        if (args.size() != 6 || !args.get(0).equals("batch") || !args.get(1).equals("mini-theory")
                || !args.get(2).equals("--request-json") || !args.get(4).equals("-f") || !args.get(5).equals("json")) return null;
        Request request;
        try { request = CommandJson.read(args.get(3), Request.class); }
        catch (IllegalArgumentException e) { err.println(e.getMessage()); return 2; }
        if (request.action() == null) { err.println("Falta la operación de miniteoría."); return 2; }
        if (!List.of("read", "save", "feedback", "commit").contains(request.action())) return null;
        try {
            String path = CoursePathResolver.resolve(null);
            if (path == null) throw new IllegalArgumentException("Falta la ruta del curso.");
            var batches = new FileSystemBatchStore(directory);
            var artifacts = new FileSystemRevisionArtifactStore(directory);
            var review = new DefaultBatchReviewServiceFactory().create(batches, artifacts,
                    new FileSystemRefinementPlanStore(directory), new FileSystemBatchReviewCache(directory));
            var service = new DefaultMiniTheoryCorrectionServiceFactory().create(batches, review, artifacts,
                    new FileSystemCourseRepository(new CourseValidatorImpl()), Path.of(path),
                    new FileSystemMiniTheoryWorkspaceStore(directory), null, null,
                    () -> { throw new IllegalStateException("Esta operación local no usa un agente."); });
            out.println(MiniTheoryCorrectionCmd.write(service.execute(request))); return 0;
        } catch (IllegalArgumentException | IllegalStateException e) { err.println("Error: " + e.getMessage()); return 2; }
    }
}
