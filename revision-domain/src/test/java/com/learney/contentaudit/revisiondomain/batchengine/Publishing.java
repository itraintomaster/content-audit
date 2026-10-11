package com.learney.contentaudit.revisiondomain.batchengine;

import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.cloze;
import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.text;
import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.withParts;

import com.learney.contentaudit.auditdomain.AuditEngine;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditableCourse;
import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.refinerdomain.PlanFindingTally;
import com.learney.contentaudit.refinerdomain.RefinementPlan;
import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.refinerdomain.RefinerEngine;
import com.learney.contentaudit.refinerdomain.UnconvertedScoreCount;
import com.learney.contentaudit.revisiondomain.batch.BatchChange;
import com.learney.contentaudit.revisiondomain.batch.BatchChangeOrigin;
import com.learney.contentaudit.revisiondomain.batch.BatchClosing;
import com.learney.contentaudit.revisiondomain.batch.BatchReview;
import com.learney.contentaudit.revisiondomain.batch.BatchReviewDecision;
import com.learney.contentaudit.revisiondomain.batch.BatchStatus;
import com.learney.contentaudit.revisiondomain.batch.BatchStore;
import com.learney.contentaudit.revisiondomain.batch.BatchTaskOutcomeKind;
import com.learney.contentaudit.revisiondomain.batch.CorrectionBatch;
import com.learney.contentaudit.revisiondomain.comparison.VerdictKind;
import com.learney.contentaudit.revisiondomain.nonregression.AudioRerecordRequest;
import com.learney.contentaudit.revisiondomain.nonregression.BatchGateVerdict;
import com.learney.contentaudit.revisiondomain.nonregression.ChangeGateVerdict;
import com.learney.contentaudit.revisiondomain.panelengine.MesaFixture;
import com.learney.contentaudit.revisiondomain.preservationengine.DefaultPreservationFactory;
import com.learney.contentaudit.revisiondomain.publication.PublicationManifestBuilder;
import com.learney.contentaudit.revisiondomain.publicationengine.DefaultProductionFieldMap;
import com.learney.contentaudit.revisiondomain.publicationengine.DefaultPublicationManifestBuilder;
import com.learney.contentaudit.revisiondomain.publicationengine.EntityProjector;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The publisher over the course in memory: its stores, an engine that measures the course it is given, the real check,
 * José's review read as the tray reads it (F-PUBPROD-R003) and the publication list built with the real table
 * (F-PUBPROD-R001).
 */
final class Publishing {
    static final String P_VEGETABLES = "tanda-a1-" + MesaFixture.VEGETABLES;

    static final String P_COME = "tanda-a1-" + MesaFixture.COME;

    static final Instant CLOSED_AT = TandaFixture.OPENED_AT.plusSeconds(3600);

    static final AudioRerecordRequest VEGETABLES_AUDIO = new AudioRerecordRequest(MesaFixture.VEGETABLES,
            MesaFixture.audioKey(MesaFixture.VEGETABLES), List.of("I love vegetables."));

    static final Finding LEMMA = MesaFixture.finding("lemma-absence", "misplaced-word", TandaFixture.RESIDE,
            FindingSeverity.MEDIUM, FindingResolution.PANEL, "reside", "«reside» no es de A1");

    static final Finding OFF_LEVEL = MesaFixture.finding("off-level-words", "far-above-level", TandaFixture.RESIDE,
            FindingSeverity.HIGH, FindingResolution.PANEL, "reside", "«reside» es de C1");

    final CourseEntity start;

    final TandaFixture.Courses courses;

    final TandaFixture.Artifacts artifacts = new TandaFixture.Artifacts();

    final TandaFixture.Reports reports = new TandaFixture.Reports();

    final TandaFixture.ActivePair active = new TandaFixture.ActivePair();

    final TandaFixture.Comparator comparator = new TandaFixture.Comparator();

    /** Where José's decisions and the publication list live. */
    TandaFixture.Batches batches = new TandaFixture.Batches();

    /** How the publisher reads José's review: as the tray reads it, unless a test says otherwise. */
    java.util.function.Function<CorrectionBatch, ReviewStanding> reviews;

    /** What builds the publication list: the real table, unless a test says otherwise. */
    PublicationManifestBuilder manifestBuilder = new DefaultPublicationManifestBuilder(TandaFixture.correctionScope(),
            new DefaultProductionFieldMap(), new EntityProjector());

    final TandaFixture.Plans plans = new TandaFixture.Plans(TandaFixture.plan(
            TandaFixture.task("t-pista", MesaFixture.missingHint(), MesaFixture.AFIRMATIVAS, CefrLevel.A1),
            TandaFixture.task("t-respuesta", MesaFixture.anotherAnswer(), MesaFixture.AFIRMATIVAS, CefrLevel.A1),
            TandaFixture.task("t-traduccion", MesaFixture.translationDiffers(), MesaFixture.AFIRMATIVAS, CefrLevel.A1),
            TandaFixture.covering("t-reside", LEMMA, List.of(OFF_LEVEL), TandaFixture.RESIDE_TOPIC),
            TandaFixture.task("t-come", MesaFixture.finding("single-answer", "different-valid-answer",
                    MesaFixture.COME, FindingSeverity.BLOCKING, FindingResolution.PANEL, null, "«goes» encaja"),
                    MesaFixture.AFIRMATIVAS, CefrLevel.A1),
            TandaFixture.task("t-audio", TandaFixture.audioOfLearned(), TandaFixture.REFLEXIVE, CefrLevel.A1)));

    final List<CourseEntity> mapped = new ArrayList<>();

    final List<AnalyzerRunSelection> selections = new ArrayList<>();

    /** What the published analysis still marks. */
    List<Finding> remaining = new ArrayList<>();

    /** The id the plan of the published analysis comes with; none, unless a test says otherwise: the store gives one. */
    String newPlanId;

    Publishing() {
        this(TandaFixture.course());
    }

    Publishing(CourseEntity start) {
        this.start = start;
        this.courses = new TandaFixture.Courses(start);
    }

    AuditReport measure(CourseEntity course) {
        return TandaFixture.report(course, remaining, Map.of(), MesaFixture.ANALYZERS);
    }

    DefaultBatchChangeLedger ledger() {
        return new DefaultBatchChangeLedger(artifacts, TandaFixture.locator());
    }

    DefaultBatchPublisher publisher() {
        return publisher(reviews != null ? reviews : review()::standing, batches, manifestBuilder);
    }

    /** The publisher wired with exactly these: where it reads the review, where it saves, and what builds the list. */
    DefaultBatchPublisher publisher(java.util.function.Function<CorrectionBatch, ReviewStanding> reviews,
            BatchStore store, PublicationManifestBuilder builder) {
        AuditEngine engine = new AuditEngine() {
            @Override
            public AuditReport runAudit(AuditableCourse course) {
                throw new AssertionError("the published course is analyzed with the analyzers of the batch");
            }

            @Override
            public AuditReport runAudit(AuditableCourse course, AnalyzerRunSelection selection) {
                selections.add(selection);
                return measure(mapped.get(mapped.size() - 1));
            }
        };
        RefinerEngine refiner = new RefinerEngine() {
            @Override
            public RefinementPlan plan(AuditReport report, String auditId) {
                return new RefinementPlan(newPlanId, auditId, Instant.parse("2026-10-01T18:00:00Z"), new ArrayList<>());
            }

            @Override
            public Optional<RefinementTask> nextTask(RefinementPlan plan) {
                return Optional.empty();
            }

            @Override
            public List<UnconvertedScoreCount> unconvertedScores(AuditReport report) {
                return List.of();
            }

            @Override
            public List<PlanFindingTally> findingTally(AuditReport report, RefinementPlan plan) {
                return List.of();
            }
        };
        return new DefaultBatchPublisher(ledger(), artifacts, courses, course -> {
            mapped.add(course);
            return new AuditableCourse(List.of());
        }, engine, reports, refiner, plans, active, comparator, TandaFixture.correctionScope(),
                new DefaultPreservationFactory().createCheck(), TandaFixture.COURSE_PATH, reviews, store, builder);
    }

    /** José's review of the batch, as Correcciones reads it. */
    DefaultBatchReview review() {
        return new DefaultBatchReview(batches, artifacts, plans, new DefaultBatchReportBuilder());
    }

    /** José decides every step of a change, on the content as the batch has it now. */
    void decide(CorrectionBatch batch, String changeId, BatchReviewDecision.Status status, Instant at) {
        List<BatchReviewDecision.Selection> selections = new ArrayList<>();
        for (BatchReview.Item item : review().standing(batch).items()) {
            if (item.changeId().equals(changeId)) {
                selections.add(new BatchReviewDecision.Selection(item.changeId(), item.step(), item.before(),
                        item.after()));
            }
        }
        if (selections.isEmpty()) {
            throw new AssertionError("no active step of " + changeId + " to decide");
        }
        batches.saveReview(new BatchReviewDecision(batch.getId(), selections, status, "José", at));
    }

    CorrectionBatch batch(String... taskIds) {
        CorrectionBatch batch = TandaFixture.batch("tanda-a1", taskIds);
        batch.setStatus(BatchStatus.RUNNING);
        return batch;
    }

    void record(CorrectionBatch batch, BatchChange change) {
        ledger().record(batch, change, start);
    }

    void close(CorrectionBatch batch) {
        batch.setClosings(new ArrayList<>(List.of(new BatchClosing(CLOSED_AT, DefaultBatchCloser.scope(batch),
                TandaFixture.passing(DefaultBatchCloser.scope(batch), List.of()), new BatchGateVerdict(true,
                VerdictKind.PASSES, List.of(), List.of(), List.of(), List.of(), List.of(), List.of()), List.of(), 1,
                null, null))));
        batch.setStatus(BatchStatus.CLOSED);
    }

    /**
     * A closed batch that passed: the vegetables without «a lot of», with their request to record, and the
     * grandmother, which José dropped before the closing.
     */
    CorrectionBatch closedBatch() {
        CorrectionBatch batch = batch("t-respuesta", "t-come");
        BatchChange vegetables = TandaFixture.change("mesa-" + MesaFixture.VEGETABLES, BatchChangeOrigin.PANEL, null,
                List.of("t-respuesta"), TandaFixture.step(withParts(TandaFixture.exercise(start,
                        MesaFixture.VEGETABLES), text("I"), cloze("love"), text("vegetables.")),
                        ExercisePart.STATEMENT));
        vegetables.setGateVerdict(new ChangeGateVerdict(true, List.of(), List.of(), List.of(),
                List.of(VEGETABLES_AUDIO), List.of(), List.of()));
        record(batch, vegetables);
        BatchBook.setOutcome(batch, "t-respuesta", BatchTaskOutcomeKind.RESOLVED_BY_PANEL, vegetables.getChangeId(), 1,
                null);
        BatchChange come = TandaFixture.change("mesa-" + MesaFixture.COME, BatchChangeOrigin.PANEL, null,
                List.of("t-come"), TandaFixture.step(withParts(TandaFixture.exercise(start, MesaFixture.COME),
                        text("My grandmother"), cloze("comes"), text("(come) every year.")), ExercisePart.STATEMENT));
        come.setGateVerdict(new ChangeGateVerdict(true, List.of(), List.of(), List.of(), List.of(
                new AudioRerecordRequest(MesaFixture.COME, MesaFixture.audioKey(MesaFixture.COME),
                        List.of("My grandmother comes every year."))), List.of(), List.of()));
        record(batch, come);
        ledger().drop(batch, come.getChangeId(), "lo sacó José", start);
        close(batch);
        return batch;
    }
}
