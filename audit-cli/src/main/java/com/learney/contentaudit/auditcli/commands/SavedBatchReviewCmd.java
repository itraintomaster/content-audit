package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.auditinfrastructure.FileSystemBatchReviewCache;
import com.learney.contentaudit.auditinfrastructure.FileSystemRefinementPlanStore;
import com.learney.contentaudit.auditinfrastructure.FileSystemRevisionArtifactStore;
import com.learney.contentaudit.auditinfrastructure.batchstore.FileSystemBatchStore;
import com.learney.contentaudit.revisiondomain.batch.BatchReviewService;
import com.learney.contentaudit.revisiondomain.batch.BatchReviewDecision;
import com.learney.contentaudit.revisiondomain.batch.NoOpenBatchException;
import com.learney.contentaudit.revisiondomain.batchengine.DefaultBatchReviewServiceFactory;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

/** Saved-review-only bootstrap. All other commands use the normal parser and services. */
final class SavedBatchReviewCmd {
    private SavedBatchReviewCmd() {}

    static Integer execute(List<String> args, Path baseDir, PrintStream output, PrintStream error) {
        if (!matches(args)) return null;
        var service = new DefaultBatchReviewServiceFactory().create(new FileSystemBatchStore(baseDir),
                new FileSystemRevisionArtifactStore(baseDir), new FileSystemRefinementPlanStore(baseDir),
                new FileSystemBatchReviewCache(baseDir));
        return execute(args, service, output, error);
    }

    static Integer execute(List<String> args, BatchReviewService service, PrintStream output, PrintStream error) {
        if (!matches(args)) return null;
        try {
            var review = args.size() == 6
                    ? service.decide(CommandJson.read(args.get(3), BatchReviewDecision.class))
                    : service.read(args.size() == 5 ? args.get(2) : null);
            output.println(CommandJson.writeCompact(review));
            return 0;
        } catch (IllegalArgumentException | NoOpenBatchException e) {
            error.println("Error: " + e.getMessage());
            return 2;
        }
    }

    private static boolean matches(List<String> args) {
        // Only the UI's exact review forms. Every other command, option or help uses the full parser.
        if (args.size() < 4 || args.size() > 6 || !args.get(0).equals("batch") || !args.get(1).equals("review")) {
            return false;
        }
        int formatAt = args.size() - 2;
        if (!(args.get(formatAt).equals("-f") || args.get(formatAt).equals("--format"))
                || !args.get(formatAt + 1).equals("json")) return false;
        if (args.size() == 6) return args.get(2).equals("--decision-json");
        String batchId = args.size() == 5 ? args.get(2) : null;
        if (batchId != null && !batchId.matches("[A-Za-z0-9][A-Za-z0-9._-]*")) return false;
        return true;
    }

}
