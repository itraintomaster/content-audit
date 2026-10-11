package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.auditdomain.ActiveAnalysisSelection;
import com.learney.contentaudit.auditdomain.ActiveAnalysisSelectionStore;
import com.learney.contentaudit.auditdomain.AuditEngine;
import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditReportStore;
import com.learney.contentaudit.auditdomain.CourseMapper;
import com.learney.contentaudit.auditdomain.EvaluationRunPolicy;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.CourseRepository;
import com.learney.contentaudit.refinerdomain.RefinementPlan;
import com.learney.contentaudit.refinerdomain.RefinementPlanStore;
import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.refinerdomain.RefinementTaskStatus;
import com.learney.contentaudit.refinerdomain.RefinerEngine;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.RevisionArtifact;
import com.learney.contentaudit.revisiondomain.RevisionArtifactStore;
import com.learney.contentaudit.revisiondomain.RevisionOutcomeKind;
import com.learney.contentaudit.revisiondomain.RevisionVerdict;
import com.learney.contentaudit.revisiondomain.batch.BatchChange;
import com.learney.contentaudit.revisiondomain.batch.BatchChangeOrigin;
import com.learney.contentaudit.revisiondomain.batch.BatchChangeStep;
import com.learney.contentaudit.revisiondomain.batch.BatchClosing;
import com.learney.contentaudit.revisiondomain.batch.BatchNotPublishableException;
import com.learney.contentaudit.revisiondomain.batch.BatchProposalRef;
import com.learney.contentaudit.revisiondomain.batch.BatchPublication;
import com.learney.contentaudit.revisiondomain.batch.BatchPublishRequest;
import com.learney.contentaudit.revisiondomain.batch.BatchReview;
import com.learney.contentaudit.revisiondomain.batch.BatchReviewDecision;
import com.learney.contentaudit.revisiondomain.batch.BatchStatus;
import com.learney.contentaudit.revisiondomain.batch.BatchStore;
import com.learney.contentaudit.revisiondomain.batch.BatchTaskOutcome;
import com.learney.contentaudit.revisiondomain.batch.BatchTaskOutcomeKind;
import com.learney.contentaudit.revisiondomain.batch.CorrectionBatch;
import com.learney.contentaudit.revisiondomain.comparison.BatchComparator;
import com.learney.contentaudit.revisiondomain.comparison.BatchComparison;
import com.learney.contentaudit.revisiondomain.comparison.BatchVerdict;
import com.learney.contentaudit.revisiondomain.comparison.RowSubject;
import com.learney.contentaudit.revisiondomain.comparison.UndecidableFloors;
import com.learney.contentaudit.revisiondomain.comparison.VerdictKind;
import com.learney.contentaudit.revisiondomain.nonregression.AudioRerecordRequest;
import com.learney.contentaudit.revisiondomain.nonregression.BatchGateVerdict;
import com.learney.contentaudit.revisiondomain.preservation.CorrectionScope;
import com.learney.contentaudit.revisiondomain.preservation.PreservationCheck;
import com.learney.contentaudit.revisiondomain.preservation.PreservationViolation;
import com.learney.contentaudit.revisiondomain.preservation.ScopedField;
import com.learney.contentaudit.revisiondomain.publication.DocumentReviewStatus;
import com.learney.contentaudit.revisiondomain.publication.ExclusionReason;
import com.learney.contentaudit.revisiondomain.publication.ManifestExcluded;
import com.learney.contentaudit.revisiondomain.publication.ManifestSource;
import com.learney.contentaudit.revisiondomain.publication.ManifestVerdict;
import com.learney.contentaudit.revisiondomain.publication.PublicationManifest;
import com.learney.contentaudit.revisiondomain.publication.PublicationManifestBuilder;
import com.learney.contentaudit.revisiondomain.publication.PublicationManifestRequest;
import com.learney.contentaudit.revisiondomain.publication.PublishedElement;
import com.learney.contentaudit.revisiondomain.publication.PublishedStep;
import com.learney.contentaudit.revisiondomain.publication.ReviewTally;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.processing.Generated;

/**
 * F-TANDA-R009: publishing once, with preservation and without production. It needs the verdict «pasa» of F with José's
 * decisions already in: if the batch changed since its last closing, it compares again first. It writes the course once
 * -- the composition of each exercise, checking for each one that only what its steps declare changed --, marks the
 * proposals applied, analyzes the written course with the same analyzers (the judges reuse everything) and saves it: it
 * must give what the recalculation gave. It builds its plan, settles the tasks -- completed when their finding no longer
 * appears, no longer holding, or pending again when undecided --, moves the active pair to that analysis, which is where
 * the next batch opens, and leaves the baseline where it was. It never writes production: taking the course there and
 * recording the audios are José's steps; the re-recordings stay pending in the publication.
 */
@Generated(
        value = "com.sentinel.SentinelEngine",
        comments = "Generated by Sentinel"
)
class DefaultBatchPublisher implements BatchPublisher {
    private final BatchChangeLedger changeLedger;

    private final RevisionArtifactStore artifactStore;

    private final CourseRepository courseRepository;

    private final CourseMapper courseMapper;

    private final AuditEngine auditEngine;

    private final AuditReportStore auditReportStore;

    private final RefinerEngine refinerEngine;

    private final RefinementPlanStore refinementPlanStore;

    private final ActiveAnalysisSelectionStore activeAnalysisSelectionStore;

    private final BatchComparator batchComparator;

    private final CorrectionScope correctionScope;

    private final PreservationCheck preservationCheck;

    private final Path coursePath;

    private Function<CorrectionBatch, ReviewStanding> reviews;

    private BatchStore batchStore;

    private PublicationManifestBuilder manifestBuilder;

    public DefaultBatchPublisher(BatchChangeLedger changeLedger,
            RevisionArtifactStore artifactStore, CourseRepository courseRepository,
            CourseMapper courseMapper, AuditEngine auditEngine, AuditReportStore auditReportStore,
            RefinerEngine refinerEngine, RefinementPlanStore refinementPlanStore,
            ActiveAnalysisSelectionStore activeAnalysisSelectionStore,
            BatchComparator batchComparator, CorrectionScope correctionScope,
            PreservationCheck preservationCheck, Path coursePath) {
        this.changeLedger = changeLedger;
        this.artifactStore = artifactStore;
        this.courseRepository = courseRepository;
        this.courseMapper = courseMapper;
        this.auditEngine = auditEngine;
        this.auditReportStore = auditReportStore;
        this.refinerEngine = refinerEngine;
        this.refinementPlanStore = refinementPlanStore;
        this.activeAnalysisSelectionStore = activeAnalysisSelectionStore;
        this.batchComparator = batchComparator;
        this.correctionScope = correctionScope;
        this.preservationCheck = preservationCheck;
        this.coursePath = coursePath;
    }

    /** F-PUBPROD-R003: de dónde lee la revisión vigente de José. Sin ella no se publica. */
    DefaultBatchPublisher withReviews(Function<CorrectionBatch, ReviewStanding> reviews) {
        this.reviews = reviews;
        return this;
    }

    /** F-PUBPROD-R001: dónde deja la lista de publicación y con qué la arma. Sin ellos no se publica. */
    DefaultBatchPublisher withPublicationList(BatchStore batchStore, PublicationManifestBuilder manifestBuilder) {
        this.batchStore = batchStore;
        this.manifestBuilder = manifestBuilder;
        return this;
    }

    @Override
    public CorrectionBatch publish(CorrectionBatch batch, BatchPublishRequest request,
            CourseEntity startCourse) {
        if (reviews == null) {
            throw new BatchNotPublishableException(batch.getId(),
                    "no hay de dónde leer las decisiones de la revisión: sin ellas no se publica");
        }
        if (batchStore == null || manifestBuilder == null) {
            throw new BatchNotPublishableException(batch.getId(), "falta con qué armar la lista de publicación");
        }
        // F-PUBPROD-R005: the gate of «no empeorar» of the last closing must pass; the verdict is checked below.
        List<BatchClosing> closings = BatchBook.list(batch.getClosings());
        BatchClosing last = closings.isEmpty() ? null : closings.get(closings.size() - 1);
        if (last == null) {
            throw new BatchNotPublishableException(batch.getId(), "la tanda no se cerró: falta la comparación de F");
        }
        BatchGateVerdict gate = last.getGate();
        if (gate == null || !gate.isPasses()) {
            List<String> reasons = gate != null ? BatchBook.list(gate.getReasons()) : List.of();
            throw new BatchNotPublishableException(batch.getId(), "la compuerta de «no empeorar» del último cierre "
                    + (gate == null ? "no está" : "no pasa") + (reasons.isEmpty() ? "" : ": " + String.join("; ", reasons)));
        }
        Instant publishedAt = Instant.now();
        // F-PUBPROD-R003 inv. 6: if the publication does not end, nothing is left out -- the batch and its proposals
        // stay as they were.
        Undo undo = new Undo(batch, artifactStore);
        Prepared prepared;
        try {
            prepared = prepare(batch, request, startCourse, last, publishedAt);
            courseRepository.save(prepared.composed(), coursePath);
        } catch (RuntimeException e) {
            undo.restore();
            throw e;
        }

        // The proposals applied.
        List<String> applied = new ArrayList<>();
        for (BatchProposalRef ref : BatchBook.list(batch.getQuizProposals())) {
            Optional<RevisionArtifact> artifact = artifactStore != null
                    ? artifactStore.load(batch.getPlanId(), ref.getProposalId()) : Optional.empty();
            if (artifact.isPresent()) {
                RevisionArtifact done = artifact.get();
                done.setVerdict(RevisionVerdict.APPROVED);
                done.setOutcome(RevisionOutcomeKind.APPROVED_APPLIED);
                done.setDecidedAt(Instant.now());
                done.setDecisionNote("Publicada con la tanda " + batch.getId()
                        + (request != null && request.getApprovedBy() != null ? " por " + request.getApprovedBy() : ""));
                artifactStore.save(done);
            }
            applied.add(ref.getProposalId());
        }

        // The written course analyzed with the same analyzers, its plan, and the active pair moved.
        AuditReport published = auditEngine.runAudit(courseMapper.map(courseRepository.load(coursePath)),
                selection(batch));
        String auditId = auditReportStore.save(published);
        RefinementPlan plan = refinerEngine.plan(published, auditId);
        String planId = refinementPlanStore.save(plan);
        Set<String> stillThere = new HashSet<>();
        for (AuditNode node : BatchBook.nodes(published)) {
            for (Finding finding : BatchBook.list(node.getFindings())) {
                stillThere.add(BatchBook.identity(finding));
            }
        }
        List<String> completed = new ArrayList<>();
        List<String> reopened = new ArrayList<>();
        settleTasks(batch, stillThere, completed, reopened);
        if (activeAnalysisSelectionStore != null) {
            activeAnalysisSelectionStore.write(new ActiveAnalysisSelection(auditId, planId));
        }
        BatchPublication publication = new BatchPublication(publishedAt,
                request != null ? request.getApprovedBy() : null, request != null ? request.getNote() : null, auditId,
                planId, applied, completed, reopened, rerecords(batch));
        ReviewTally tally = prepared.tally();
        publication.setManifestDigest(prepared.manifestDigest());
        publication.setVerdictKind(VerdictKind.PASSES);
        publication.setGatePassed(Boolean.TRUE);
        publication.setUndecidableFloors(new ArrayList<>());
        publication.setStepsApproved(tally.approved());
        publication.setStepsUnreviewed(tally.pending());
        publication.setStepsRejected(tally.rejected());
        publication.setChangesLeftOutRejected(new ArrayList<>(tally.droppedForReviewChangeIds()));
        publication.setChangesLeftOutChained(new ArrayList<>(tally.collateralDroppedChangeIds()));
        batch.setPublication(publication);
        batch.setStatus(BatchStatus.PUBLISHED);
        return batch;
    }

    /** What publishing decided before writing the course: the course to write, the list's digest and the counts. */
    private record Prepared(CourseEntity composed, String manifestDigest, ReviewTally tally) {
    }

    /**
     * Everything up to the list, in memory but for the list and the recomposed proposals: what José left out, what he
     * rejected and what goes with it; the verdict compared again if anything changed; the composed course checked
     * exercise by exercise; and the list, saved with its digest before the course is written.
     */
    private Prepared prepare(CorrectionBatch batch, BatchPublishRequest request, CourseEntity startCourse,
            BatchClosing last, Instant publishedAt) {
        // What José already took out before publishing, named in the list (the gate's returns are not his).
        Map<String, ExclusionReason> excluded = new LinkedHashMap<>();
        for (String changeId : droppedByJose(batch)) {
            excluded.put(changeId, ExclusionReason.DROPPED_BY_JOSE);
        }
        // F-PUBPROD-R003: José's review as it stands before anything leaves.
        ReviewStanding standing = standing(batch);
        Set<String> rejected = new LinkedHashSet<>();
        Set<String> rejectedSteps = new LinkedHashSet<>();
        collectRejected(standing, rejected, rejectedSteps);

        // Rules José left out before the OK go first, through the ledger.
        boolean dropped = false;
        for (String ruleId : request != null ? BatchBook.list(request.getExcludedRuleIds()) : List.<String>of()) {
            for (BatchChange change : BatchBook.copy(batch.getChanges())) {
                if (change != null && change.getDroppedReason() == null && change.getOrigin() == BatchChangeOrigin.RULE
                        && Objects.equals(change.getRuleId(), ruleId)) {
                    leaveOut(batch, change.getChangeId(), "José sacó la regla " + ruleId + " al publicar",
                            ExclusionReason.RULE_EXCLUDED, rejected, excluded, startCourse);
                    dropped = true;
                }
            }
        }
        // Then every change with a step José rejected, whole, with what goes with it, until nothing rejected is left.
        standing = standing(batch);
        while (true) {
            collectRejected(standing, rejected, rejectedSteps);
            List<String> toLeaveOut = new ArrayList<>();
            for (BatchChange change : BatchBook.list(batch.getChanges())) {
                if (change != null && change.getDroppedReason() == null && rejected.contains(change.getChangeId())) {
                    toLeaveOut.add(change.getChangeId());
                }
            }
            if (toLeaveOut.isEmpty()) {
                break;
            }
            for (String changeId : toLeaveOut) {
                if (activeChangeIds(batch).contains(changeId)) {
                    leaveOut(batch, changeId, REJECTED_IN_REVIEW, ExclusionReason.REVIEW_REJECTED, rejected, excluded,
                            startCourse);
                    dropped = true;
                }
            }
            standing = standing(batch);
        }
        // A rejection on a content that changed afterwards stops the publication until José looks at it again.
        Set<String> active = activeChangeIds(batch);
        List<String> stale = new ArrayList<>();
        for (ReviewStanding.StaleRejection rejection : standing.staleRejections()) {
            String named = rejection.changeId() + " en " + rejection.nodeId();
            if (active.contains(rejection.changeId()) && !stale.contains(named)) {
                stale.add(named);
            }
        }
        if (!stale.isEmpty()) {
            throw new BatchNotPublishableException(batch.getId(), "José rechazó un contenido que cambió después ("
                    + String.join(", ", stale) + "): hay que volver a mirarlo antes de publicar");
        }

        // F-PUBPROD-R005: the verdict of F, compared again when the batch changed since its last closing.
        BatchComparison comparison = last.getComparison();
        if (dropped || changedSince(batch, last)) {
            comparison = batchComparator.compare(coursePath, DefaultBatchCloser.scope(batch));
        }
        BatchVerdict verdict = comparison != null ? comparison.getVerdict() : null;
        VerdictKind kind = verdict != null ? verdict.getKind() : null;
        String detail = verdict != null && verdict.getDetail() != null ? ": " + verdict.getDetail() : "";
        if (kind != VerdictKind.PASSES) {
            throw new BatchNotPublishableException(batch.getId(), "el veredicto de F es «" + verdictName(kind)
                    + "», no «pasa»" + detail);
        }
        List<String> undecided = undecidableFloors(verdict);
        if (!undecided.isEmpty()) {
            throw new BatchNotPublishableException(batch.getId(), "no se puede decidir: " + undecided.size()
                    + (undecided.size() == 1 ? " piso sin decidir (" : " pisos sin decidir (")
                    + String.join(", ", undecided) + ")" + detail);
        }

        // The course composed with every accepted change left in the batch, and no other, checked exercise by exercise.
        CourseEntity composed = changeLedger.composedCourse(batch, startCourse);
        List<String> violations = new ArrayList<>();
        for (BatchProposalRef ref : BatchBook.list(batch.getQuizProposals())) {
            violations.addAll(preserved(batch, ref, startCourse, withOnly(batch, ref.getQuizId(), startCourse)));
        }
        if (!violations.isEmpty()) {
            throw new BatchNotPublishableException(batch.getId(), "cambió lo que sus pasos no declaran: "
                    + String.join("; ", violations));
        }

        // F-PUBPROD-R001 and R004: the list -- what may reach production, with its before and after -- before the
        // course.
        ReviewTally tally = tally(standing, rejectedSteps, excluded);
        List<PublishedElement> elements = new ArrayList<>();
        for (BatchProposalRef ref : BatchBook.list(batch.getQuizProposals())) {
            PublishedScope scope = PublishedScope.of(batch, ref.getQuizId());
            if (scope.partsByChange().isEmpty()) {
                continue;
            }
            List<PublishedStep> steps = new ArrayList<>();
            scope.partsByChange().forEach((changeId, parts) -> steps.add(new PublishedStep(changeId, parts)));
            elements.add(new PublishedElement(scope.snapshot(startCourse).orElse(null),
                    scope.snapshot(composed).orElse(null), ref.getProposalId(),
                    reviewStatus(standing, ref.getQuizId()), steps));
        }
        List<ManifestExcluded> leftOut = new ArrayList<>();
        excluded.forEach((changeId, reason) -> leftOut.add(new ManifestExcluded(changeId, reason)));
        PublicationManifestRequest manifestRequest = new PublicationManifestRequest(batch.getId(), batch.getPlanId(),
                batch.getLevel() != null ? batch.getLevel().name() : null, publishedAt.toString(),
                request != null ? request.getApprovedBy() : null,
                new ManifestSource(batch.getBaselineAuditId(), batch.getStartAuditId(), null),
                new ManifestVerdict(VerdictKind.PASSES, true, List.of(),
                        new ArrayList<>(BatchBook.list(verdict.getBrokenFloorGoalIds())), verdict.getDetail()),
                tally, elements, leftOut);
        PublicationManifest manifest;
        try {
            manifest = manifestBuilder.build(manifestRequest);
        } catch (RuntimeException e) {
            throw new BatchNotPublishableException(batch.getId(), "la lista de publicación no se puede armar: "
                    + message(e));
        }
        String digest;
        try {
            digest = batchStore.savePublicationManifest(batch.getId(), manifest);
        } catch (RuntimeException e) {
            throw new BatchNotPublishableException(batch.getId(), "la lista de publicación no se pudo guardar: "
                    + message(e));
        }
        if (digest == null || digest.isBlank()) {
            throw new BatchNotPublishableException(batch.getId(), "la lista de publicación quedó sin huella");
        }
        return new Prepared(composed, digest, tally);
    }

    // -------------------------------------------------------------------------
    // F-PUBPROD-R003 and R004: José's review when publishing
    // -------------------------------------------------------------------------

    /** The reason a change leaves the batch when José rejected one of its steps. */
    static final String REJECTED_IN_REVIEW = "rechazado al revisar";

    /** José's review as it stands; if it cannot be read, nothing is published (R003 inv. 5). */
    private ReviewStanding standing(CorrectionBatch batch) {
        ReviewStanding standing;
        try {
            standing = reviews.apply(batch);
        } catch (RuntimeException e) {
            throw new BatchNotPublishableException(batch.getId(), "no se pudieron leer las decisiones de la revisión ("
                    + message(e) + "): sin ellas no se publica");
        }
        if (standing == null) {
            throw new BatchNotPublishableException(batch.getId(),
                    "no se pudieron leer las decisiones de la revisión: sin ellas no se publica");
        }
        return standing;
    }

    private static void collectRejected(ReviewStanding standing, Set<String> rejected, Set<String> rejectedSteps) {
        for (BatchReview.Item item : standing.items()) {
            if (item.status() == BatchReviewDecision.Status.REJECTED) {
                rejected.add(item.changeId());
                rejectedSteps.add(item.changeId() + "\u0000" + (item.step() != null ? item.step().getQuizId() : null));
            }
        }
    }

    /**
     * Takes a change out through the ledger -- with the later changes of the same exercise that touched the same, and
     * their tasks back to pending -- and names in the list every change that left: rejected when José rejected it,
     * {@code why} the one asked, chained the others.
     */
    private void leaveOut(CorrectionBatch batch, String changeId, String reason, ExclusionReason why,
            Set<String> rejected, Map<String, ExclusionReason> excluded, CourseEntity startCourse) {
        Set<String> before = activeChangeIds(batch);
        changeLedger.drop(batch, changeId, reason, startCourse);
        Set<String> after = activeChangeIds(batch);
        for (String id : before) {
            if (!after.contains(id)) {
                excluded.put(id, rejected.contains(id) ? ExclusionReason.REVIEW_REJECTED
                        : id.equals(changeId) ? why : ExclusionReason.COLLATERAL);
            }
        }
    }

    private static Set<String> activeChangeIds(CorrectionBatch batch) {
        Set<String> active = new LinkedHashSet<>();
        for (BatchChange change : BatchBook.list(batch.getChanges())) {
            if (change != null && change.getDroppedReason() == null) {
                active.add(change.getChangeId());
            }
        }
        return active;
    }

    /**
     * The changes José took out before publishing, with the ones that went with them: every change already out but
     * the ones the gate of the batch returned at its closing, and the ones chained with those.
     */
    private static List<String> droppedByJose(CorrectionBatch batch) {
        Set<String> returnedByGate = new LinkedHashSet<>();
        List<String> jose = new ArrayList<>();
        for (BatchChange change : BatchBook.list(batch.getChanges())) {
            if (change == null || change.getDroppedReason() == null) {
                continue;
            }
            String reason = change.getDroppedReason();
            String chainedWith = DefaultBatchChangeLedger.chainedWith(reason);
            if (reason.startsWith(DefaultBatchCloser.RETURNED_BY_GATE)
                    || (chainedWith != null && returnedByGate.contains(chainedWith))) {
                returnedByGate.add(change.getChangeId());
            } else {
                jose.add(change.getChangeId());
            }
        }
        return jose;
    }

    /** Whether every active step of the node is approved as it stands, or something travels without review. */
    private static DocumentReviewStatus reviewStatus(ReviewStanding standing, String nodeId) {
        boolean any = false;
        for (BatchReview.Item item : standing.items()) {
            if (item.step() == null || !Objects.equals(item.step().getQuizId(), nodeId)) {
                continue;
            }
            any = true;
            if (item.status() != BatchReviewDecision.Status.APPROVED) {
                return DocumentReviewStatus.PENDING;
            }
        }
        return any ? DocumentReviewStatus.APPROVED : DocumentReviewStatus.PENDING;
    }

    /** The counts of the review: approved and unreviewed among what travels, rejected among what left. */
    private static ReviewTally tally(ReviewStanding standing, Set<String> rejectedSteps,
            Map<String, ExclusionReason> excluded) {
        int approved = 0;
        int pending = 0;
        for (BatchReview.Item item : standing.items()) {
            if (item.status() == BatchReviewDecision.Status.APPROVED) {
                approved++;
            } else {
                pending++;
            }
        }
        List<String> forReview = new ArrayList<>();
        List<String> collateral = new ArrayList<>();
        excluded.forEach((changeId, reason) -> {
            if (reason == ExclusionReason.REVIEW_REJECTED) {
                forReview.add(changeId);
            } else if (reason == ExclusionReason.COLLATERAL) {
                collateral.add(changeId);
            }
        });
        return new ReviewTally(approved, pending, rejectedSteps.size(), forReview, collateral);
    }

    // -------------------------------------------------------------------------
    // F-PUBPROD-R005: only «pasa» publishes
    // -------------------------------------------------------------------------

    private static String verdictName(VerdictKind kind) {
        if (kind == null) {
            return "sin veredicto";
        }
        return kind == VerdictKind.UNDECIDABLE ? "no se puede decidir" : kind.name();
    }

    /** The floors F could not decide, by analyzer when it names them. */
    private static List<String> undecidableFloors(BatchVerdict verdict) {
        UndecidableFloors floors = verdict != null ? verdict.getUndecidableFloors() : null;
        if (floors == null || floors.getCount() <= 0) {
            return List.of();
        }
        List<String> named = new ArrayList<>();
        for (RowSubject subject : BatchBook.list(floors.getAnalyzers())) {
            if (subject != null && subject.getSubject() != null && !named.contains(subject.getSubject())) {
                named.add(subject.getSubject());
            }
        }
        while (named.size() < floors.getCount()) {
            named.add("piso " + (named.size() + 1));
        }
        return named;
    }

    private static String message(RuntimeException e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }

    /**
     * What publishing changes before writing the course, kept to put it back if it does not end: which changes are
     * out, the proposals of the batch, the outcomes of its tasks, and the saved proposals (copied, since a store in
     * memory may hand back the very instance it keeps).
     */
    private static final class Undo {
        private final CorrectionBatch batch;

        private final RevisionArtifactStore artifactStore;

        private final Map<BatchChange, String> droppedReasons = new IdentityHashMap<>();

        private final List<BatchProposalRef> quizProposals;

        private final List<BatchTaskOutcome> outcomes;

        private final List<RevisionArtifact> artifacts = new ArrayList<>();

        Undo(CorrectionBatch batch, RevisionArtifactStore artifactStore) {
            this.batch = batch;
            this.artifactStore = artifactStore;
            for (BatchChange change : BatchBook.list(batch.getChanges())) {
                if (change != null) {
                    droppedReasons.put(change, change.getDroppedReason());
                }
            }
            this.quizProposals = batch.getQuizProposals() != null ? new ArrayList<>(batch.getQuizProposals()) : null;
            this.outcomes = batch.getOutcomes() != null ? new ArrayList<>(batch.getOutcomes()) : null;
            if (artifactStore != null) {
                for (BatchProposalRef ref : BatchBook.list(batch.getQuizProposals())) {
                    artifactStore.load(batch.getPlanId(), ref.getProposalId()).ifPresent(a -> artifacts.add(
                            new RevisionArtifact(a.getProposal(), a.getVerdict(), a.getRejectionReason(),
                                    a.getOutcome(), a.getDecidedAt(), a.getDecisionNote(), a.getContextSource(),
                                    a.getContextOverridePayload())));
                }
            }
        }

        void restore() {
            droppedReasons.forEach(BatchChange::setDroppedReason);
            batch.setQuizProposals(quizProposals);
            batch.setOutcomes(outcomes);
            if (artifactStore != null) {
                for (RevisionArtifact artifact : artifacts) {
                    artifactStore.save(artifact);
                }
            }
        }
    }

    // -------------------------------------------------------------------------

    /** Whether José changed the batch after its last closing: a change or an adjustment later than it. */
    private static boolean changedSince(CorrectionBatch batch, BatchClosing closing) {
        Instant closedAt = closing.getClosedAt();
        if (closedAt == null) {
            return true;
        }
        for (BatchChange change : BatchBook.list(batch.getChanges())) {
            if (change != null && change.getCreatedAt() != null && change.getCreatedAt().isAfter(closedAt)) {
                return true;
            }
        }
        return BatchBook.list(batch.getAdjustments()).stream().anyMatch(a -> a != null && a.getDecidedAt() != null
                && a.getDecidedAt().isAfter(closedAt));
    }

    /**
     * The course as the batch opened with only this node as the batch leaves it: what each exercise may change is checked
     * against a course where nothing else moved, so the changes of the other exercises are not read as its own.
     */
    private CourseEntity withOnly(CorrectionBatch batch, String nodeId, CourseEntity startCourse) {
        List<BatchChange> mine = new ArrayList<>();
        for (BatchChange change : BatchBook.list(batch.getChanges())) {
            if (change == null || change.getDroppedReason() != null) {
                continue;
            }
            List<BatchChangeStep> steps = new ArrayList<>();
            for (BatchChangeStep step : BatchBook.list(change.getSteps())) {
                if (step != null && Objects.equals(step.getQuizId(), nodeId)) {
                    steps.add(step);
                }
            }
            if (!steps.isEmpty()) {
                mine.add(new BatchChange(change.getChangeId(), change.getOrigin(), change.getRuleId(),
                        change.getKnowledgeId(), change.getTaskIds(), steps, change.getGateVerdict(),
                        change.isJoseException(), change.getCreatedAt(), null));
            }
        }
        CorrectionBatch view = new CorrectionBatch();
        view.setId(batch.getId());
        view.setPlanId(batch.getPlanId());
        view.setChanges(mine);
        return changeLedger.composedCourse(view, startCourse);
    }

    /** F-TANDA-R009: only what the steps of the exercise declare changed in it. */
    private List<String> preserved(CorrectionBatch batch, BatchProposalRef ref, CourseEntity before, CourseEntity after) {
        List<String> violations = new ArrayList<>();
        if (preservationCheck == null || correctionScope == null) {
            return violations;
        }
        PublishedScope scope = PublishedScope.of(batch, ref.getQuizId());
        Optional<CourseElementSnapshot> revised = scope.snapshot(after);
        if (revised.isEmpty()) {
            return violations;
        }
        Set<ScopedField> changeable = correctionScope.changeableFields(scope.parts(), scope.target());
        for (PreservationViolation violation : BatchBook.list(preservationCheck.verifyWithin(before, after,
                revised.get(), changeable))) {
            violations.add(ref.getQuizId() + " " + violation.getPath());
        }
        return violations;
    }

    private static AnalyzerRunSelection selection(CorrectionBatch batch) {
        List<String> analyzers = BatchBook.list(batch.getAnalyzers());
        if (analyzers.isEmpty()) {
            return new AnalyzerRunSelection(null, null);
        }
        Map<String, EvaluationRunPolicy> policies = new LinkedHashMap<>();
        for (String analyzer : analyzers) {
            policies.put(analyzer, new EvaluationRunPolicy(0, false, null, null, null, 1));
        }
        return new AnalyzerRunSelection(new ArrayList<>(analyzers), policies);
    }

    /** The tasks leave «in batch»: completed when their finding is gone, no longer holding, or pending when undecided. */
    private void settleTasks(CorrectionBatch batch, Set<String> stillThere, List<String> completed,
            List<String> reopened) {
        Optional<RefinementPlan> plan = refinementPlanStore != null && batch.getPlanId() != null
                ? refinementPlanStore.load(batch.getPlanId()) : Optional.empty();
        Map<String, RefinementTask> tasks = plan.map(BatchBook::tasksById).orElse(Map.of());
        for (String taskId : BatchBook.list(batch.getTaskIds())) {
            RefinementTask task = tasks.get(taskId);
            Optional<Finding> finding = BatchBook.findingOf(task);
            BatchTaskOutcomeKind kind = BatchBook.kind(batch, taskId);
            boolean gone = finding.isPresent() && !stillThere.contains(BatchBook.identity(finding.get()));
            if (task != null) {
                for (Finding covered : task.getOrigin() != null ? BatchBook.list(task.getOrigin().getCoveredFindings())
                        : List.<Finding>of()) {
                    gone &= !stillThere.contains(BatchBook.identity(covered));
                }
            }
            RefinementTaskStatus status;
            if (gone) {
                status = RefinementTaskStatus.COMPLETED;
                completed.add(taskId);
            } else if (kind == BatchTaskOutcomeKind.NO_LONGER_HOLDS) {
                status = RefinementTaskStatus.STALE;
                completed.add(taskId);
            } else {
                status = RefinementTaskStatus.PENDING;
                reopened.add(taskId);
            }
            if (task != null) {
                task.setStatus(status);
            }
        }
        if (plan.isPresent()) {
            refinementPlanStore.save(plan.get());
        }
    }

    /** The audios the batch asked to record again, its accepted changes and its rule fixes, by key. */
    private static List<AudioRerecordRequest> rerecords(CorrectionBatch batch) {
        Map<String, AudioRerecordRequest> byKey = new LinkedHashMap<>();
        for (BatchChange change : BatchBook.list(batch.getChanges())) {
            if (change == null || change.getDroppedReason() != null || change.getGateVerdict() == null) {
                continue;
            }
            for (AudioRerecordRequest request : BatchBook.list(change.getGateVerdict().getRerecords())) {
                byKey.put(request.getQuizId() + "\u0000" + request.getAudioKey(), request);
            }
        }
        return new ArrayList<>(new LinkedHashSet<>(byKey.values()));
    }
}
