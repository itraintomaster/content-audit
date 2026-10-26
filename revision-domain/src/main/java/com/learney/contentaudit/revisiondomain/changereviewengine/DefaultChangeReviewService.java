package com.learney.contentaudit.revisiondomain.changereviewengine;

import com.learney.contentaudit.evaluationledgerdomain.EvaluationBudget;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationCoverage;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRequest;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRequestMode;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationResolution;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationResolutionKind;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSession;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSessionFactory;
import com.learney.contentaudit.evaluationledgerdomain.Evaluator;
import com.learney.contentaudit.revisiondomain.batch.BatchReview;
import com.learney.contentaudit.revisiondomain.batch.BatchReviewService;
import com.learney.contentaudit.revisiondomain.changereview.ChangeReview;
import com.learney.contentaudit.revisiondomain.changereview.ChangeReviewService;
import com.learney.contentaudit.revisiondomain.changereview.ChangeReviewVerdictReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * FEAT-MARCA: reads what the batch changed against the course it started from, shows the marks the ledger keeps and,
 * only with a budget, asks the reviewer for the ones that are missing or of an earlier version. It reads the review of
 * the batch and never saves anything of it.
 */
final class DefaultChangeReviewService implements ChangeReviewService {
    static final int DEFAULT_CONCURRENCY = 4;
    private final BatchReviewService reviews;
    private final EvaluationSessionFactory sessions;
    private final Evaluator reviewer;
    private final ChangeReviewVerdictReader reader;
    private final ChangeReviewSubjects subjects;

    DefaultChangeReviewService(BatchReviewService reviews, EvaluationSessionFactory sessions, Evaluator reviewer,
            ChangeReviewVerdictReader reader, ChangeReviewSubjects subjects) {
        this.reviews = Objects.requireNonNull(reviews);
        this.sessions = Objects.requireNonNull(sessions);
        this.reviewer = Objects.requireNonNull(reviewer);
        this.reader = Objects.requireNonNull(reader);
        this.subjects = Objects.requireNonNull(subjects);
    }

    @Override
    public ChangeReview.Result review(ChangeReview.Request request) {
        if (request == null || request.batchId() == null || request.batchId().isBlank())
            throw new IllegalArgumentException("Falta la tanda.");
        if (request.budget() != null && request.budget() < 0)
            throw new IllegalArgumentException("El tope de consultas no puede ser negativo.");
        BatchReview review = reviews.read(request.batchId());
        // F-MARCA-R001: production is the course the correction started from, not the previous batch.
        List<ChangeReviewSubjects.Changed> changed = subjects.changed(review.baseCourse(), review.course(), request.quizIds());
        String version = reviewer.evaluatorVersion().orElse(null);

        // F-MARCA-R003: what the ledger keeps is shown without consulting anybody.
        EvaluationSession saved = sessions.open(new EvaluationBudget(0), reviewer);
        List<EvaluationResolution> answers = new ArrayList<>(saved.resolveAll(changed.stream()
                .map(each -> new EvaluationRequest(each.subject(), EvaluationRequestMode.REUSE_ONLY)).toList(), 1));
        List<ChangeReview.Row> rows = new ArrayList<>();
        for (int i = 0; i < changed.size(); i++) rows.add(row(changed.get(i), answer(answers, i), version));

        int spent = 0;
        String unavailable = null;
        int budget = request.budget() == null ? 0 : request.budget();
        List<Integer> missing = IntStream.range(0, rows.size())
                .filter(i -> rows.get(i).state() != ChangeReview.State.MARKED).boxed().toList();
        if (budget > 0 && !missing.isEmpty()) {
            EvaluationSession session = sessions.open(new EvaluationBudget(budget), reviewer);
            // A mark of an earlier version, or one that cannot be used, is asked again; the rest, for the first time.
            List<EvaluationResolution> fresh = session.resolveAll(missing.stream().map(i -> new EvaluationRequest(
                    changed.get(i).subject(), answer(answers, i) != null
                            && answer(answers, i).getKind() == EvaluationResolutionKind.REUSED
                            ? EvaluationRequestMode.FORCE : EvaluationRequestMode.REUSE_OR_EVALUATE)).toList(),
                    request.concurrency() == null ? DEFAULT_CONCURRENCY : Math.max(1, request.concurrency()));
            for (int j = 0; j < missing.size(); j++) {
                EvaluationResolution answer = answer(fresh, j);
                // Out of budget, or the service unavailable: what was shown stays as it was.
                if (answer == null || answer.getKind() == EvaluationResolutionKind.PENDING) continue;
                int i = missing.get(j);
                rows.set(i, row(changed.get(i), answer, version));
            }
            EvaluationCoverage coverage = session.coverage();
            spent = coverage == null ? 0 : coverage.getEvaluatedInRun() + coverage.getFailed();
            unavailable = session.unavailableReason().orElse(null);
        }

        // F-MARCA-R005: WORSE, DOUBTFUL, inherited, without a mark, GOOD; the order of the course within each.
        List<ChangeReview.Row> ordered = IntStream.range(0, rows.size()).boxed()
                .sorted(Comparator.comparingInt((Integer i) -> group(rows.get(i))).thenComparingInt(i -> i))
                .map(rows::get).toList();
        int upToDate = (int) rows.stream().filter(r -> r.state() == ChangeReview.State.MARKED).count();
        return new ChangeReview.Result(request.batchId(), version, runtime(reviewer.runtimeIdentity()),
                rows.size(), upToDate, rows.size() - upToDate, spent, request.budget(), unavailable, ordered);
    }

    /** F-MARCA-R002 and R004: a mark only when the answer is of this exercise and its fragments are in its sides. */
    private ChangeReview.Row row(ChangeReviewSubjects.Changed changed, EvaluationResolution answer, String version) {
        if (answer == null || answer.getKind() == EvaluationResolutionKind.PENDING)
            return row(changed, ChangeReview.State.UNMARKED, null, null, "Sin marca: falta consultar al revisor.");
        if (answer.getKind() == EvaluationResolutionKind.FAILED)
            return row(changed, ChangeReview.State.UNUSABLE, null, null, answer.getFailure() == null
                    ? "El revisor no dejó una marca." : "El revisor no dejó una marca: " + answer.getFailure().getReason());
        ChangeReview.Verdict verdict;
        try {
            verdict = reader.read(answer.getPayload());
            if (!changed.quizId().equals(verdict.quizId()))
                throw new IllegalArgumentException("la marca es de otro ejercicio (" + verdict.quizId() + ")");
            List<ChangeReview.Problem> problems = new ArrayList<>();
            for (ChangeReview.Problem problem : verdict.problems()) {
                String fragment = problem.fragment().strip();
                if (!changed.before().contains(fragment) && !changed.after().contains(fragment))
                    throw new IllegalArgumentException("el fragmento «" + fragment + "» no está en ninguno de los dos lados");
                // F-MARCA-R010: what is not in production as it is cannot have been there already.
                problems.add(problem.inherited() && !changed.before().contains(fragment) ? problem.asNew() : problem);
            }
            verdict = new ChangeReview.Verdict(verdict.quizId(), verdict.mark(), problems, verdict.reason());
        } catch (RuntimeException unusable) {
            return row(changed, ChangeReview.State.UNUSABLE, null, answer.getEvaluatorVersion(),
                    "La marca no se puede usar: " + unusable.getMessage());
        }
        boolean today = version != null && version.equals(answer.getEvaluatorVersion());
        return row(changed, today ? ChangeReview.State.MARKED : ChangeReview.State.OLDER_VERSION, verdict,
                answer.getEvaluatorVersion(), today ? null : "Marca de una versión anterior del revisor.");
    }

    private static ChangeReview.Row row(ChangeReviewSubjects.Changed changed, ChangeReview.State state,
            ChangeReview.Verdict verdict, String version, String detail) {
        return new ChangeReview.Row(changed.quizId(), changed.topic(), state, verdict, version, detail,
                changed.before(), changed.after());
    }

    private static int group(ChangeReview.Row row) {
        if (row.verdict() == null) return 3;
        return switch (row.verdict().mark()) {
            case WORSE -> 0;
            case DOUBTFUL -> row.verdict().inheritedOnly() ? 2 : 1;
            case GOOD -> 4;
        };
    }

    private static EvaluationResolution answer(List<EvaluationResolution> answers, int i) {
        return answers != null && i < answers.size() ? answers.get(i) : null;
    }

    private static String runtime(EvaluationRuntimeIdentity runtime) {
        return runtime == null ? null : runtime.getProviderName() + " / " + runtime.getModelName() + " / "
                + runtime.getReasoningEffort();
    }
}
