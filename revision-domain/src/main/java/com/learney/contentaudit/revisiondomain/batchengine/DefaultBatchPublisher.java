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
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
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
import com.learney.contentaudit.revisiondomain.comparison.VerdictKind;
import com.learney.contentaudit.revisiondomain.comparison.VerdictWording;
import com.learney.contentaudit.revisiondomain.nonregression.AudioRerecordRequest;
import com.learney.contentaudit.revisiondomain.nonregression.BatchGateVerdict;
import com.learney.contentaudit.revisiondomain.preservation.CorrectionScope;
import com.learney.contentaudit.revisiondomain.preservation.PreservationCheck;
import com.learney.contentaudit.revisiondomain.preservation.PreservationViolation;
import com.learney.contentaudit.revisiondomain.preservation.ScopedField;
import com.learney.contentaudit.revisiondomain.publication.DocumentReviewStatus;
import com.learney.contentaudit.revisiondomain.publication.ManifestSource;
import com.learney.contentaudit.revisiondomain.publication.ManifestVerdict;
import com.learney.contentaudit.revisiondomain.publication.PublicationManifest;
import com.learney.contentaudit.revisiondomain.publication.PublicationManifestBuilder;
import com.learney.contentaudit.revisiondomain.publication.PublicationManifestRequest;
import com.learney.contentaudit.revisiondomain.publication.PublishedElement;
import com.learney.contentaudit.revisiondomain.publication.ReviewTally;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
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
    private com.learney.contentaudit.revisiondomain.batch.CurrentAnalysisService currentAnalysis;

    DefaultBatchPublisher withCurrentAnalysis(com.learney.contentaudit.revisiondomain.batch.CurrentAnalysisService service) {
        this.currentAnalysis = service; return this;
    }
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

    /** F-PUBPROD-R003: de dónde lee la revisión vigente de José, la misma que muestra la bandeja. */
    private final Function<CorrectionBatch, ReviewStanding> reviews;

    /** F-PUBPROD-R001: dónde queda la lista de publicación, y la tanda. */
    private final BatchStore batchStore;

    /**
     * F-PUBPROD-R001: con qué se arma la lista de publicación. Puede faltar: la configuración del servicio de tandas no
     * está obligada a traer el curso como lo guarda producción —abrir, correr, cerrar o revisar una tanda no lo usan—, y
     * sin él no hay lista. Entonces publicar se niega, con la tanda y el curso sin tocar.
     */
    private final PublicationManifestBuilder manifestBuilder;

    public DefaultBatchPublisher(BatchChangeLedger changeLedger,
            RevisionArtifactStore artifactStore, CourseRepository courseRepository,
            CourseMapper courseMapper, AuditEngine auditEngine, AuditReportStore auditReportStore,
            RefinerEngine refinerEngine, RefinementPlanStore refinementPlanStore,
            ActiveAnalysisSelectionStore activeAnalysisSelectionStore,
            BatchComparator batchComparator, CorrectionScope correctionScope,
            PreservationCheck preservationCheck, Path coursePath,
            Function<CorrectionBatch, ReviewStanding> reviews, BatchStore batchStore,
            PublicationManifestBuilder manifestBuilder) {
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
        this.reviews = Objects.requireNonNull(reviews,
                "publicar necesita de dónde leer las decisiones de la revisión: sin ellas no se publica");
        this.batchStore = Objects.requireNonNull(batchStore,
                "publicar necesita dónde guardar la tanda y su lista de publicación");
        this.manifestBuilder = manifestBuilder;
    }

    @Override
    public CorrectionBatch publish(CorrectionBatch batch, BatchPublishRequest request,
            CourseEntity startCourse) {
        if (currentAnalysis != null) return batchStore.withAnalysisLock(batch.getId(), () -> {
            CorrectionBatch latest = batchStore.load(batch.getId()).orElseThrow();
            if (latest.getContentVersion() != batch.getContentVersion() || latest.getStatus() != BatchStatus.CLOSED)
                throw new BatchNotPublishableException(batch.getId(), "la tanda cambió antes de publicar: volvé a leer");
            return publishLocked(latest, request, startCourse);
        });
        return publishLocked(batch, request, startCourse);
    }

    private CorrectionBatch publishLocked(CorrectionBatch batch, BatchPublishRequest request, CourseEntity startCourse) {
        if (manifestBuilder == null) {
            throw new BatchNotPublishableException(batch.getId(), "falta con qué armar la lista de publicación");
        }
        batchStore.requireEditableContent(batch);
        if ((batch.getEvolution() != null || batch.getBaseCourseSnapshot() != null)
                && !Objects.equals(startCourse, courseRepository.load(coursePath)))
            throw new BatchNotPublishableException(batch.getId(),
                    "el curso publicado cambió desde la base de esta corrección: revisá la evolución antes de integrar");
        BatchPublishRequest asked = request != null ? request : new BatchPublishRequest(null, null, List.of());
        // F-PUBPROD-R005: the gate of «no empeorar» of the last closing must pass; the verdict is checked below.
        List<BatchClosing> closings = BatchBook.list(batch.getClosings());
        BatchClosing last = closings.isEmpty() ? null : closings.get(closings.size() - 1);
        if (last == null) {
            throw new BatchNotPublishableException(batch.getId(), "la tanda no se cerró: falta la comparación de F");
        }
        BatchGateVerdict gate = last.getGate();
        if (currentAnalysis == null && (gate == null || !gate.isPasses())) {
            List<String> reasons = gate != null ? BatchBook.list(gate.getReasons()) : List.of();
            throw new BatchNotPublishableException(batch.getId(), "la compuerta de «no empeorar» del último cierre "
                    + (gate == null ? "no está" : "no pasa") + (reasons.isEmpty() ? "" : ": " + String.join("; ", reasons)));
        }
        Instant publishedAt = Instant.now();
        // F-PUBPROD-R003 inv. 6 and R010: if the publication does not end, nothing is left half done -- the course, the
        // list of this attempt, the proposals, the tasks of the plan, the active analysis and the batch go back to how
        // they were, and the analysis and the plan of the published course it saved are discarded.
        Undo undo = new Undo(batch, startCourse);
        try {
            if (batch.getBaseCourseSnapshot() == null && batch.getEvolution() == null)
                batch.setBaseCourseSnapshot(startCourse);
            Prepared prepared = prepare(batch, asked, startCourse, last, publishedAt);
            String digest = saveManifest(batch, prepared.manifest(), undo);
            undo.courseWritten = true;
            courseRepository.save(prepared.composed(), coursePath);

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
                            + (asked.getApprovedBy() != null ? " por " + asked.getApprovedBy() : ""));
                    artifactStore.save(done);
                }
                applied.add(ref.getProposalId());
            }

            // The written course analyzed with the same analyzers, its plan, and the active pair moved. The analysis and
            // the plan this attempt saves are noted as they are saved, to discard them if it does not end (R010 inv. 4).
            AuditReport published = currentAnalysis != null
                    ? currentAnalysis.report(batch, "correction", courseRepository.load(coursePath))
                    : auditEngine.runAudit(courseMapper.map(courseRepository.load(coursePath)), selection(batch));
            String auditId = auditReportStore.save(published);
            undo.savedAuditId = auditId;
            RefinementPlan plan = refinerEngine.plan(published, auditId);
            undo.savingPlan(plan.getId());
            String planId = refinementPlanStore.save(plan);
            undo.savedPlanId = planId;
            Set<String> stillThere = new HashSet<>();
            for (AuditNode node : BatchBook.nodes(published)) {
                for (Finding finding : BatchBook.list(node.getFindings())) {
                    stillThere.add(BatchBook.identity(finding));
                }
            }
            List<String> completed = new ArrayList<>();
            List<String> reopened = new ArrayList<>();
            settleTasks(batch, stillThere, completed, reopened, undo);
            if (activeAnalysisSelectionStore != null) {
                undo.previousActive = activeAnalysisSelectionStore.read();
                undo.activeMoved = true;
                activeAnalysisSelectionStore.write(new ActiveAnalysisSelection(auditId, planId));
            }
            BatchPublication publication = new BatchPublication(publishedAt, asked.getApprovedBy(), asked.getNote(),
                    auditId, planId, applied, completed, reopened, rerecords(batch));
            ReviewTally tally = prepared.tally();
            publication.setManifestDigest(digest);
            publication.setVerdictKind(VerdictKind.PASSES);
            publication.setGatePassed(Boolean.TRUE);
            publication.setUndecidableFloors(new ArrayList<>());
            publication.setStepsApproved(tally.approved());
            publication.setStepsPending(tally.pending());
            publication.setStepsRejected(tally.rejected());
            publication.setChangesLeftOutRejected(new ArrayList<>(tally.droppedForReviewChangeIds()));
            publication.setChangesLeftOutCollateral(new ArrayList<>(tally.collateralDroppedChangeIds()));
            batch.setPublication(publication);
            batch.setStatus(BatchStatus.PUBLISHED);
            // The batch is saved last: once it says «publicada», the publication ended.
            batchStore.save(batch);
            return batch;
        } catch (RuntimeException e) {
            throw undo.restore(e);
        }
    }

    /**
     * F-PUBPROD-R001: the list saved before the course, with its digest. What was there before -- the list of an earlier
     * attempt that did not end -- is kept to put it back if this attempt does not end either.
     */
    private String saveManifest(CorrectionBatch batch, PublicationManifest manifest, Undo undo) {
        String digest;
        try {
            undo.previousManifest = batchStore.publicationManifest(batch.getId());
            undo.manifestWritten = true;
            digest = batchStore.savePublicationManifest(batch.getId(), manifest);
        } catch (RuntimeException e) {
            throw new BatchNotPublishableException(batch.getId(), "la lista de publicación no se pudo guardar: "
                    + message(e));
        }
        if (digest == null || digest.isBlank()) {
            throw new BatchNotPublishableException(batch.getId(), "la lista de publicación quedó sin huella");
        }
        return digest;
    }

    /** What publishing decided before writing anything but the ledger: the course to write, its list and the counts. */
    private record Prepared(CourseEntity composed, PublicationManifest manifest, ReviewTally tally) {
    }

    /**
     * Everything up to the list, in memory but for the recomposed proposals: what José left out, what he rejected and
     * what goes with it; the verdict compared again if anything changed; the composed course checked exercise by
     * exercise; and the list, built.
     */
    private Prepared prepare(CorrectionBatch batch, BatchPublishRequest request, CourseEntity startCourse,
            BatchClosing last, Instant publishedAt) {
        // F-PUBPROD-R003: José's review as it stands before anything leaves, and what he already took out.
        ReviewStanding standing = standing(batch);
        for (var item : standing.items()) {
            if (ConversionJudgments.conversion(item) && item.status() != BatchReviewDecision.Status.REJECTED) {
                if (item.status() != BatchReviewDecision.Status.APPROVED)
                    throw new BatchNotPublishableException(batch.getId(), "la conversión necesita aprobación explícita");
                ConversionJudgments.requireApproved(batch, item);
            }
        }
        PublicationExclusions exclusions = new PublicationExclusions(batch, changeLedger, startCourse, standing);

        // Rules José left out before the OK go first, through the ledger.
        boolean dropped = false;
        for (String ruleId : BatchBook.list(request.getExcludedRuleIds())) {
            dropped |= exclusions.leaveOutRule(ruleId);
        }
        if (dropped) {
            standing = standing(batch);
        }
        // Then every change with a step José rejected, whole, with what goes with it, until nothing rejected is left.
        while (exclusions.leaveOutRejected(standing)) {
            dropped = true;
            standing = standing(batch);
        }
        // A rejection on a content that changed afterwards stops the publication until José looks at it again (the
        // review reads only the changes still in the batch).
        List<String> stale = new ArrayList<>();
        for (ReviewStanding.StaleRejection rejection : standing.staleRejections()) {
            String named = rejection.changeId() + " en " + rejection.nodeId();
            if (!stale.contains(named)) {
                stale.add(named);
            }
        }
        if (!stale.isEmpty()) {
            throw new BatchNotPublishableException(batch.getId(), "José rechazó un contenido que cambió después ("
                    + String.join(", ", stale) + "): hay que volver a mirarlo antes de publicar");
        }

        // F-PUBPROD-R005: the verdict of F, compared again when the batch changed since its last closing.
        BatchComparison comparison = last.getComparison();
        if (currentAnalysis != null) {
            var measured = currentAnalysis.comparisonReports(batch, startCourse, changeLedger.composedCourse(batch, startCourse));
            comparison = batchComparator.compareMeasured(coursePath, DefaultBatchCloser.scope(batch),
                    measured.baseline(), measured.approved(), measured.projected());
            var tasks = refinementPlanStore.load(batch.getPlanId()).map(BatchBook::tasksById).orElse(Map.of());
            var currentGate = new com.learney.contentaudit.revisiondomain.nonregressionengine.DefaultGateFactory().batchGate()
                    .evaluate(comparison, DefaultBatchCloser.changedOutside(batch, startCourse,
                            changeLedger.composedCourse(batch, startCourse), tasks, batchStore), DefaultBatchCloser.exceptions(batch));
            if (!currentGate.isPasses()) throw new BatchNotPublishableException(batch.getId(),
                    "la compuerta sobre el análisis vigente no pasa: " + String.join("; ", BatchBook.list(currentGate.getReasons())));
        } else if (dropped || changedSince(batch, last)) {
            comparison = batchComparator.compare(coursePath, DefaultBatchCloser.scope(batch));
        }
        BatchVerdict verdict = comparison != null ? comparison.getVerdict() : null;
        if (!VerdictWording.publishable(verdict)) {
            throw new BatchNotPublishableException(batch.getId(), "el veredicto de F es "
                    + VerdictWording.quoted(verdict) + ", no «pasa»"
                    + (verdict != null && verdict.getDetail() != null ? ": " + verdict.getDetail() : ""));
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
        ReviewTally tally = exclusions.tally(standing);
        List<PublishedElement> elements = new ArrayList<>();
        for (BatchProposalRef ref : BatchBook.list(batch.getQuizProposals())) {
            DeclaredParts declared = DeclaredParts.of(batch, ref.getQuizId());
            Map<String, Set<ExercisePart>> partsByChange = declared.partsByChange();
            if (partsByChange.isEmpty()) {
                continue;
            }
            elements.add(new PublishedElement(declared.snapshot(startCourse).orElse(null),
                    declared.snapshot(composed).orElse(null), ref.getProposalId(),
                    reviewStatus(standing, ref.getQuizId()), partsByChange));
        }
        PublicationManifestRequest manifestRequest = new PublicationManifestRequest(batch.getId(), batch.getPlanId(),
                batch.getLevel() != null ? batch.getLevel().name() : null, publishedAt.toString(),
                request.getApprovedBy(), new ManifestSource(batch.getBaselineAuditId(), batch.getStartAuditId(), null),
                new ManifestVerdict(VerdictKind.PASSES, true, List.of(),
                        new ArrayList<>(BatchBook.list(verdict.getBrokenFloorGoalIds())), verdict.getDetail()),
                tally, elements, exclusions.leftOut());
        PublicationManifest manifest;
        try {
            manifest = manifestBuilder.build(manifestRequest);
        } catch (RuntimeException e) {
            throw new BatchNotPublishableException(batch.getId(), "la lista de publicación no se puede armar: "
                    + message(e));
        }
        return new Prepared(composed, manifest, tally);
    }

    // -------------------------------------------------------------------------
    // F-PUBPROD-R003 and R004: José's review when publishing
    // -------------------------------------------------------------------------

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

    /** Whether every active step of the node is approved as it stands, or something travels pending review. */
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

    private static String message(RuntimeException e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }

    /**
     * F-PUBPROD-R003 inv. 6 and R010: everything publishing changes, kept to put it back if it does not end -- which
     * changes are out, the proposals of the batch, the outcomes of its tasks, its state and publication, and the saved
     * proposals (copied, since a store in memory may hand back the very instance it keeps) -- and what it wrote, as it
     * writes it: the list of this attempt, the course, the analysis and the plan of the published course, the tasks of
     * the plan and the active analysis. The analysis and the plan it saved are discarded, so that the next command that
     * looks for «the latest» finds the one before; a plan that already had the id of the new one is put back instead,
     * and a save of an analysis always gives a new id. While the active analysis could not be put back, they stay: it
     * still names them.
     */
    private final class Undo {
        private final CorrectionBatch batch;
        private final CourseEntity baseCourseSnapshot;

        private final CourseEntity startCourse;

        private final Map<BatchChange, String> droppedReasons = new IdentityHashMap<>();

        private final List<BatchProposalRef> quizProposals;

        private final List<BatchTaskOutcome> outcomes;

        private final BatchStatus status;

        private final BatchPublication publication;

        private final List<RevisionArtifact> artifacts = new ArrayList<>();

        /** Whether this attempt wrote its list, and the list that was there before, if any. */
        private boolean manifestWritten;

        private Optional<PublicationManifest> previousManifest = Optional.empty();

        /** Whether this attempt wrote the course (or started to). */
        private boolean courseWritten;

        /** The analysis of the published course this attempt saved. */
        private String savedAuditId;

        /** The plan of the published course this attempt saves or saved, and the plan that had its id before, if any. */
        private String savedPlanId;

        private Optional<RefinementPlan> previousPlan = Optional.empty();

        /** The status each task of the batch plan had before this attempt settled it. */
        private final Map<String, RefinementTaskStatus> taskStatuses = new LinkedHashMap<>();

        private String settledPlanId;

        /** Whether this attempt moved the active analysis, and where it was. */
        private boolean activeMoved;

        private Optional<ActiveAnalysisSelection> previousActive = Optional.empty();

        Undo(CorrectionBatch batch, CourseEntity startCourse) {
            this.batch = batch;
            this.baseCourseSnapshot = batch.getBaseCourseSnapshot();
            this.startCourse = startCourse;
            for (BatchChange change : BatchBook.list(batch.getChanges())) {
                if (change != null) {
                    droppedReasons.put(change, change.getDroppedReason());
                }
            }
            this.quizProposals = batch.getQuizProposals() != null ? new ArrayList<>(batch.getQuizProposals()) : null;
            this.outcomes = batch.getOutcomes() != null ? new ArrayList<>(batch.getOutcomes()) : null;
            this.status = batch.getStatus();
            this.publication = batch.getPublication();
            if (artifactStore != null) {
                for (BatchProposalRef ref : BatchBook.list(batch.getQuizProposals())) {
                    artifactStore.load(batch.getPlanId(), ref.getProposalId()).ifPresent(a -> artifacts.add(
                            new RevisionArtifact(a.getProposal(), a.getVerdict(), a.getRejectionReason(),
                                    a.getOutcome(), a.getDecidedAt(), a.getDecisionNote(), a.getContextSource(),
                                    a.getContextOverridePayload())));
                }
            }
        }

        /**
         * Notes the plan about to be saved before saving it -- so that a save that fails halfway is discarded too -- with
         * the plan that already had that id, if any, to put it back instead of discarding it.
         */
        void savingPlan(String planId) {
            if (planId != null) {
                previousPlan = refinementPlanStore.load(planId);
                savedPlanId = planId;
            }
        }

        /**
         * Puts everything back, last written first, and gives what to throw: the failure as it came when everything went
         * back, or one that names what was left half done -- with the failure as its cause -- when something could not.
         */
        RuntimeException restore(RuntimeException failure) {
            List<String> halfDone = new ArrayList<>();
            List<RuntimeException> undoFailures = new ArrayList<>();
            boolean activeBack = true;
            if (activeMoved) {
                activeBack = step(halfDone, undoFailures, "el análisis vigente quedó en el del curso publicado", () -> {
                    if (previousActive.isPresent()) {
                        activeAnalysisSelectionStore.write(previousActive.get());
                    } else {
                        activeAnalysisSelectionStore.clear();
                    }
                });
            }
            if (settledPlanId != null) {
                step(halfDone, undoFailures, "las tareas del plan " + settledPlanId + " quedaron como publicadas", () ->
                        refinementPlanStore.load(settledPlanId).ifPresent(plan -> {
                            for (RefinementTask task : BatchBook.list(plan.getTasks())) {
                                if (task != null && taskStatuses.containsKey(task.getId())) {
                                    task.setStatus(taskStatuses.get(task.getId()));
                                }
                            }
                            refinementPlanStore.save(plan);
                        }));
            }
            if (!activeBack) {
                // The active analysis still names the analysis and the plan of the published course: they stay.
                halfDone.add("el análisis " + savedAuditId + " y el plan " + savedPlanId + " del curso publicado"
                        + " quedaron guardados, porque el análisis vigente todavía los nombra");
            } else {
                if (savedPlanId != null) {
                    String planId = savedPlanId;
                    step(halfDone, undoFailures, previousPlan.isPresent()
                            ? "el plan " + planId + " quedó como el del curso publicado y no como estaba"
                            : "el plan " + planId + " del curso publicado quedó guardado", () -> {
                                if (previousPlan.isPresent()) {
                                    refinementPlanStore.save(previousPlan.get());
                                } else {
                                    refinementPlanStore.discard(planId);
                                }
                            });
                }
                if (savedAuditId != null) {
                    String auditId = savedAuditId;
                    step(halfDone, undoFailures, "el análisis " + auditId + " del curso publicado quedó guardado",
                            () -> auditReportStore.discard(auditId));
                }
            }
            if (courseWritten) {
                step(halfDone, undoFailures, "el curso en " + coursePath + " quedó como lo escribió esta publicación, o a"
                        + " medio escribir", () -> courseRepository.save(startCourse, coursePath));
            }
            if (manifestWritten) {
                step(halfDone, undoFailures, "la lista de publicación de este intento quedó guardada", () -> {
                    if (previousManifest.isPresent()) {
                        batchStore.savePublicationManifest(batch.getId(), previousManifest.get());
                    } else {
                        batchStore.discardPublicationManifest(batch.getId());
                    }
                });
            }
            if (artifactStore != null) {
                step(halfDone, undoFailures, "las propuestas de la tanda quedaron como aplicadas o recompuestas", () -> {
                    for (RevisionArtifact artifact : artifacts) {
                        artifactStore.save(artifact);
                    }
                });
            }
            droppedReasons.forEach(BatchChange::setDroppedReason);
            batch.setQuizProposals(quizProposals);
            batch.setOutcomes(outcomes);
            batch.setPublication(publication);
            batch.setBaseCourseSnapshot(baseCourseSnapshot);
            batch.setStatus(status);
            if (halfDone.isEmpty()) {
                return failure;
            }
            IllegalStateException unfinished = new IllegalStateException("La publicación de la tanda " + batch.getId()
                    + " no terminó (" + message(failure) + ") y no se pudo deshacer del todo: "
                    + String.join("; ", halfDone) + ". Hay que dejarlo como estaba a mano antes de volver a publicar.",
                    failure);
            undoFailures.forEach(unfinished::addSuppressed);
            return unfinished;
        }

        /** Runs one step of putting back; when it fails, notes what was left and gives false. */
        private static boolean step(List<String> halfDone, List<RuntimeException> undoFailures, String whatIsLeft,
                Runnable action) {
            try {
                action.run();
                return true;
            } catch (RuntimeException e) {
                halfDone.add(whatIsLeft + " (" + message(e) + ")");
                undoFailures.add(e);
                return false;
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
        for (BatchChange change : BatchBook.activeChanges(batch)) {
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
        DeclaredParts declared = DeclaredParts.of(batch, ref.getQuizId());
        Optional<CourseElementSnapshot> revised = declared.snapshot(after);
        if (revised.isEmpty()) {
            return violations;
        }
        Set<ScopedField> changeable = correctionScope.changeableFields(declared.parts(), declared.target());
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
            List<String> reopened, Undo undo) {
        Optional<RefinementPlan> plan = refinementPlanStore != null && batch.getPlanId() != null
                ? refinementPlanStore.load(batch.getPlanId()) : Optional.empty();
        Map<String, RefinementTask> tasks = plan.map(BatchBook::tasksById).orElse(Map.of());
        if (plan.isPresent()) {
            undo.settledPlanId = batch.getPlanId();
            tasks.forEach((taskId, task) -> undo.taskStatuses.put(taskId, task.getStatus()));
        }
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
        for (BatchChange change : BatchBook.activeChanges(batch)) {
            if (change.getGateVerdict() == null) {
                continue;
            }
            for (AudioRerecordRequest request : BatchBook.list(change.getGateVerdict().getRerecords())) {
                byKey.put(request.getQuizId() + "\u0000" + request.getAudioKey(), request);
            }
        }
        return new ArrayList<>(new LinkedHashSet<>(byKey.values()));
    }
}
