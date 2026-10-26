package com.learney.contentaudit.revisiondomain.changereview;

import java.util.List;
import java.util.Objects;

/**
 * FEAT-MARCA: a reading of each exercise a batch changed, production beside now, that marks it without deciding
 * anything. What it reads, what it answers and what a run of it shows.
 */
public final class ChangeReview {
    private ChangeReview() {}

    /** F-MARCA-R002: GOOD would be published as it is; DOUBTFUL is better than production with a defect; WORSE is worse or broken. */
    public enum Mark { GOOD, DOUBTFUL, WORSE }

    /** Where a problem is, in either side. */
    public enum Where { INSTRUCTIONS, EXERCISE, ANSWERS, OPTIONS, TRANSLATION, MINI_THEORY }

    /**
     * MARKED by today's reviewer; OLDER_VERSION marked by an earlier version, shown but asked again with a budget;
     * UNMARKED never read, or read before the exercise changed again; UNUSABLE answered with something that cannot be
     * used (F-MARCA-R004).
     */
    public enum State { MARKED, OLDER_VERSION, UNMARKED, UNUSABLE }

    /**
     * Without a budget nothing is consulted (F-MARCA-R003); {@code quizIds} empty means every exercise the batch
     * changed (F-MARCA-R005).
     */
    public record Request(String batchId, List<String> quizIds, Integer budget, Integer concurrency) {
        public Request {
            quizIds = quizIds == null ? List.of() : List.copyOf(quizIds);
        }
    }

    public record Problem(Where where, String fragment, String what) {
        public Problem {
            Objects.requireNonNull(where, "where");
            if (fragment == null || fragment.isBlank()) throw new IllegalArgumentException("Problema sin fragmento");
            if (what == null || what.isBlank()) throw new IllegalArgumentException("Problema sin qué pasa");
        }
    }

    /** F-MARCA-R002: GOOD carries no problem; DOUBTFUL and WORSE carry at least one. */
    public record Verdict(String quizId, Mark mark, List<Problem> problems, String reason) {
        public Verdict {
            if (quizId == null || quizId.isBlank()) throw new IllegalArgumentException("Marca sin ejercicio");
            Objects.requireNonNull(mark, "mark");
            problems = problems == null ? List.of() : List.copyOf(problems);
            if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Marca sin razón");
            if (mark == Mark.GOOD && !problems.isEmpty())
                throw new IllegalArgumentException("Una marca «bien» no trae problemas");
            if (mark != Mark.GOOD && problems.isEmpty())
                throw new IllegalArgumentException("Una marca «dudoso» o «mal» trae al menos un problema");
        }
    }

    /** One exercise of the batch: its topic, both sides as read, and its mark if it has one. */
    public record Row(String quizId, String topic, State state, Verdict verdict, String reviewerVersion,
            String detail, String before, String after) {}

    /**
     * {@code changed} exercises in the request, {@code upToDate} of them marked by today's reviewer, {@code pending}
     * still to be asked, {@code spent} queries of this run.
     */
    public record Result(String batchId, String reviewerVersion, String runtime, int changed, int upToDate,
            int pending, int spent, Integer budget, String unavailableReason, List<Row> rows) {
        public Result {
            rows = rows == null ? List.of() : List.copyOf(rows);
        }
    }
}
