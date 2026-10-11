package com.learney.contentaudit.revisiondomain.trayengine;

import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.cloze;
import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.text;
import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.withParts;

import com.learney.contentaudit.auditdomain.AuditEngine;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableCourse;
import com.learney.contentaudit.auditdomain.Baseline;
import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditdomain.finding.AnalysisCost;
import com.learney.contentaudit.auditdomain.finding.EvidencePart;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingEvidence;
import com.learney.contentaudit.auditdomain.finding.FindingIdentity;
import com.learney.contentaudit.auditdomain.finding.FindingNodeRef;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.evaluationledgerdomain.contentfingerprint.Sha256ContentFingerprinter;
import com.learney.contentaudit.refinerdomain.PlanFindingTally;
import com.learney.contentaudit.refinerdomain.RefinementPlan;
import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.refinerdomain.RefinerEngine;
import com.learney.contentaudit.refinerdomain.UnconvertedScoreCount;
import com.learney.contentaudit.revisiondomain.batch.BatchChange;
import com.learney.contentaudit.revisiondomain.batch.BatchChangeOrigin;
import com.learney.contentaudit.revisiondomain.batch.BatchClosing;
import com.learney.contentaudit.revisiondomain.batch.BatchService;
import com.learney.contentaudit.revisiondomain.batch.BatchServiceConfig;
import com.learney.contentaudit.revisiondomain.batch.BatchStatus;
import com.learney.contentaudit.revisiondomain.batch.BatchTaskOutcomeKind;
import com.learney.contentaudit.revisiondomain.batch.CorrectionBatch;
import com.learney.contentaudit.revisiondomain.batchengine.DefaultBatchServiceFactory;
import com.learney.contentaudit.revisiondomain.batchengine.TandaFixture;
import com.learney.contentaudit.revisiondomain.blindsample.BlindPairView;
import com.learney.contentaudit.revisiondomain.blindsample.BlindSample;
import com.learney.contentaudit.revisiondomain.blindsample.BlindSampleResult;
import com.learney.contentaudit.revisiondomain.blindsample.BlindSampleService;
import com.learney.contentaudit.revisiondomain.blindsample.PairAnswer;
import com.learney.contentaudit.revisiondomain.blindsample.StudentSide;
import com.learney.contentaudit.revisiondomain.comparison.VerdictKind;
import com.learney.contentaudit.revisiondomain.nonregression.AudioRerecordRequest;
import com.learney.contentaudit.revisiondomain.nonregression.BatchGateVerdict;
import com.learney.contentaudit.revisiondomain.nonregression.ChangeGateVerdict;
import com.learney.contentaudit.revisiondomain.nonregressionengine.DefaultGateFactory;
import com.learney.contentaudit.revisiondomain.panel.CandidateRejection;
import com.learney.contentaudit.revisiondomain.panel.PanelCandidate;
import com.learney.contentaudit.revisiondomain.panel.PanelCandidateKind;
import com.learney.contentaudit.revisiondomain.panel.PanelDecision;
import com.learney.contentaudit.revisiondomain.panel.PanelOutcomeKind;
import com.learney.contentaudit.revisiondomain.panelengine.DefaultPanelRunnerFactory;
import com.learney.contentaudit.revisiondomain.panelengine.MesaFixture;
import com.learney.contentaudit.revisiondomain.preservationengine.DefaultPreservationFactory;
import com.learney.contentaudit.revisiondomain.tray.RememberedDecision;
import com.learney.contentaudit.revisiondomain.tray.TrayDecisionStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The tray of a closed batch of A1, without a model, a file or F: the course of {@link TandaFixture} with «I learned ___
 * (me / myself) to drive a car.»; a batch with a rule of the hint over two exercises, the vegetables accepted by the
 * panel on their own -- with their request to record «I love vegetables.» --, «Winter ___ (come) every year.» in a tie of
 * the judges of translation, «I ___ (drink) tea in the morning.» without winner after three attempts, a tie of the
 * judges on «You ___ (eat) a lot of fruit.» of the course, and the audio of «I learned myself to drive a car.» that a
 * published batch left to record; F's blind sample of three pairs, by hand; José's decisions in memory.
 */
public final class BandejaFixture {

    public static final String BATCH = "tanda-A1-20261001T120000Z";

    public static final String SAMPLE = "muestra-tanda-A1-1";

    public static final String VEGETABLES = MesaFixture.VEGETABLES;

    public static final String COME = MesaFixture.COME;

    public static final String FRUIT = "67fab6d59930102295341ec8";

    public static final String TEA = "67fab6d59930102295341ec9";

    public static final String WORK = "67fab6d59930102295341ec1";

    public static final String HINT_RULE = "hint-coherence";

    public static final Instant CLOSED_AT = Instant.parse("2026-10-01T14:00:00Z");

    private BandejaFixture() {
    }

    // -------------------------------------------------------------------------
    // The findings
    // -------------------------------------------------------------------------

    /** The judges of translation-sense disagree on «You ___ (eat) a lot of fruit.» / «Comes mucha fruta.». */
    public static Finding courseTie() {
        return new Finding("translation-sense", "judges-disagree", new FindingNodeRef(AuditTarget.QUIZ, FRUIT, FRUIT),
                FindingSeverity.MEDIUM, new FindingEvidence(new ArrayList<>(List.of(
                        new EvidencePart("Ejercicio", "You eat a lot of fruit. · Comes mucha fruta."),
                        new EvidencePart("Juez desde el inglés", "sí: «Comes mucha fruta.» dice lo mismo"),
                        new EvidencePart("Juez desde la traducción", "no: «mucha fruta» no dice «a lot of»"))),
                        "los dos jueces de traducción y sentido no coinciden", new ArrayList<>()),
                FindingResolution.RANK_ONLY, AnalysisCost.PAID_MODEL,
                new FindingIdentity("translation-sense|judges-disagree|" + FRUIT, null));
    }

    /** The judges disagree on the candidate of the panel for «He ___ (come) every year.». */
    public static Finding candidateTie() {
        return new Finding("translation-sense", "judges-disagree", new FindingNodeRef(AuditTarget.QUIZ, COME, COME),
                FindingSeverity.HIGH, new FindingEvidence(new ArrayList<>(List.of(
                        new EvidencePart("Juez desde el inglés", "no: «Viene cada año.» no dice «Winter»"),
                        new EvidencePart("Juez desde la traducción", "sí"))),
                        "los dos jueces de traducción y sentido no coinciden", new ArrayList<>()),
                FindingResolution.RANK_ONLY, AnalysisCost.PAID_MODEL,
                new FindingIdentity("translation-sense|judges-disagree|" + COME, null));
    }

    /** «She ___ (work) in a bank.»: its hint does not say its subject. */
    public static Finding workHint() {
        return MesaFixture.finding("hint-coherence", "hint-subject", WORK, FindingSeverity.HIGH, FindingResolution.RULE,
                "she", "la pista no dice el sujeto");
    }

    public static Finding comeAnswer() {
        return MesaFixture.finding("single-answer", "different-valid-answer", COME, FindingSeverity.BLOCKING,
                FindingResolution.PANEL, null, "«goes» también encaja");
    }

    public static Finding teaAnswer() {
        return MesaFixture.finding("single-answer", "different-valid-answer", TEA, FindingSeverity.BLOCKING,
                FindingResolution.PANEL, null, "«have» también encaja");
    }

    /** What the best candidate for the tea did not pass: it duplicates another exercise. */
    public static Finding teaDuplicate() {
        return MesaFixture.finding("exact-duplicates", "same-exercise", TEA, FindingSeverity.HIGH, FindingResolution.RULE,
                null, "queda igual a «I drink tea every morning.» de A2");
    }

    // -------------------------------------------------------------------------
    // The course and the batch
    // -------------------------------------------------------------------------

    public static CourseEntity course() {
        return TandaFixture.withLearned(TandaFixture.course());
    }

    public static PanelCandidate winter(CourseEntity course) {
        return new PanelCandidate(PanelCandidateKind.ENGLISH_CHANGE, withParts(TandaFixture.exercise(course, COME),
                text("Winter"), cloze("comes"), text("(come) every year.")), null, null, "«He» sin antecedente");
    }

    public static PanelCandidate teaEveryMorning(CourseEntity course) {
        return new PanelCandidate(PanelCandidateKind.ENGLISH_CHANGE, withParts(TandaFixture.exercise(course, TEA),
                text("I"), cloze("drink"), text("(drink) tea every morning.")), null, null, "una sola respuesta");
    }

    public static AudioRerecordRequest vegetablesAudio() {
        return new AudioRerecordRequest(VEGETABLES, MesaFixture.audioKey(VEGETABLES), List.of("I love vegetables."));
    }

    public static AudioRerecordRequest learnedAudio() {
        return new AudioRerecordRequest(TandaFixture.LEARNED, MesaFixture.audioKey(TandaFixture.LEARNED),
                List.of("I learned myself to drive a car."));
    }

    public static RefinementPlan plan() {
        return TandaFixture.plan(
                TandaFixture.task("t-pista", MesaFixture.missingHint(), MesaFixture.AFIRMATIVAS, CefrLevel.A1),
                TandaFixture.task("t-respuesta", MesaFixture.anotherAnswer(), MesaFixture.AFIRMATIVAS, CefrLevel.A1),
                TandaFixture.task("t-come", comeAnswer(), MesaFixture.AFIRMATIVAS, CefrLevel.A1),
                TandaFixture.task("t-tea", teaAnswer(), MesaFixture.AFIRMATIVAS, CefrLevel.A1),
                TandaFixture.task("t-work", workHint(), MesaFixture.AFIRMATIVAS, CefrLevel.A1));
    }

    /**
     * The batch as the panels and the gate left it: closed, with its blind sample drawn and open.
     */
    public static CorrectionBatch batch(CourseEntity course, TandaFixture.Artifacts artifacts) {
        CorrectionBatch batch = TandaFixture.batch(BATCH, "t-pista", "t-respuesta", "t-come", "t-tea", "t-work");
        QuizTemplateEntity vegetables = TandaFixture.exercise(course, VEGETABLES);
        QuizTemplateEntity hinted = TandaFixture.edited(vegetables,
                q -> q.getForm().getSentenceParts().get(2).setText("(love) a lot of vegetables."));
        QuizTemplateEntity work = TandaFixture.edited(TandaFixture.exercise(course, WORK),
                q -> q.getForm().getSentenceParts().get(2).setText("(she / work) in a bank."));
        BatchChange hint = TandaFixture.change("regla-hint-coherence-" + VEGETABLES, BatchChangeOrigin.RULE, HINT_RULE,
                List.of("t-pista"), TandaFixture.step(hinted, ExercisePart.HINT));
        BatchChange workHint = TandaFixture.change("regla-hint-coherence-" + WORK, BatchChangeOrigin.RULE, HINT_RULE,
                List.of("t-work"), TandaFixture.step(work, ExercisePart.HINT));
        QuizTemplateEntity rewritten = withParts(hinted, text("I"), cloze("love"), text("(love) vegetables."));
        BatchChange panel = TandaFixture.change("mesa-" + VEGETABLES, BatchChangeOrigin.PANEL, null, List.of("t-respuesta"),
                TandaFixture.step(rewritten, ExercisePart.STATEMENT));
        panel.setGateVerdict(new ChangeGateVerdict(true, List.of(), List.of(), List.of(), List.of(vegetablesAudio()),
                List.of(), List.of()));
        TandaFixture.recorded(batch, course, artifacts, hint, workHint, panel);
        TandaFixture.outcome(batch, "t-pista", BatchTaskOutcomeKind.RESOLVED_BY_RULE, hint.getChangeId(), 0, null);
        TandaFixture.outcome(batch, "t-work", BatchTaskOutcomeKind.RESOLVED_BY_RULE, workHint.getChangeId(), 0, null);
        TandaFixture.outcome(batch, "t-respuesta", BatchTaskOutcomeKind.RESOLVED_BY_PANEL, panel.getChangeId(), 1, null);

        PanelCandidate winter = winter(course);
        PanelDecision accepted = new PanelDecision(MesaFixture.AFIRMATIVAS, List.of(VEGETABLES), List.of("t-respuesta"),
                PanelOutcomeKind.ACCEPTED, new PanelCandidate(PanelCandidateKind.ENGLISH_CHANGE, rewritten, null, null,
                        "sin «a lot of»"), panel.getGateVerdict(), List.of("t-respuesta"), 1, List.of(), null, null,
                List.of(), 5);
        PanelDecision tie = new PanelDecision(MesaFixture.AFIRMATIVAS, List.of(COME), List.of("t-come"),
                PanelOutcomeKind.TIE, null, null, List.of(), 1, List.of(), winter,
                new ChangeGateVerdict(true, List.of(), List.of(), List.of(), List.of(new AudioRerecordRequest(COME,
                        MesaFixture.audioKey(COME), List.of("Winter comes every year."))), List.of(), List.of()),
                List.of(candidateTie()), 5);
        PanelDecision noWinner = new PanelDecision(MesaFixture.AFIRMATIVAS, List.of(TEA), List.of("t-tea"),
                PanelOutcomeKind.NO_WINNER, null, null, List.of(), 3, List.of(
                        new CandidateRejection(1, PanelCandidateKind.ENGLISH_CHANGE, List.of("«have» sigue encajando")),
                        new CandidateRejection(2, PanelCandidateKind.WHOLE_EXERCISE, List.of("la traducción no dice lo"
                                + " mismo")),
                        new CandidateRejection(3, PanelCandidateKind.ENGLISH_CHANGE, List.of("F-NOEMP-R001: error nuevo"
                                + " de exact-duplicates"))),
                teaEveryMorning(course), new ChangeGateVerdict(false, List.of(teaDuplicate()), List.of(), List.of(),
                        List.of(), List.of(), List.of("F-NOEMP-R001: error nuevo de exact-duplicates (same-exercise)")),
                List.of(), 15);
        batch.setPanelDecisions(new ArrayList<>(List.of(accepted, tie, noWinner)));
        TandaFixture.outcome(batch, "t-come", BatchTaskOutcomeKind.TIE, null, 1, null);
        TandaFixture.outcome(batch, "t-tea", BatchTaskOutcomeKind.NO_WINNER, null, 3, "nadie lo arregló");
        batch.setCourseTies(new ArrayList<>(List.of(courseTie())));
        batch.setPendingRerecords(new ArrayList<>(List.of(learnedAudio())));
        batch.setBlindSampleId(SAMPLE);
        batch.setQueryCap(40);
        batch.setQueriesSpent(25);
        batch.setStatus(BatchStatus.CLOSED);
        batch.setClosings(new ArrayList<>(List.of(new BatchClosing(CLOSED_AT, null,
                TandaFixture.comparison(null, List.of(), VerdictKind.BLIND_SAMPLE_MISSING),
                new BatchGateVerdict(true, VerdictKind.BLIND_SAMPLE_MISSING, List.of(), List.of(), List.of(), List.of(),
                        List.of(), List.of()), List.of(), 1, null, null))));
        return batch;
    }

    // -------------------------------------------------------------------------
    // F's blind sample, José's memory and the renderer
    // -------------------------------------------------------------------------

    /** F's blind sample by hand: three pairs, the answers it got and whether it was closed. */
    public static final class Ciega implements BlindSampleService {
        public final List<BlindPairView> pairs = new ArrayList<>();

        public final List<String> answers = new ArrayList<>();

        public final List<String> drawn = new ArrayList<>();

        public boolean closed;

        public Ciega(CourseEntity course) {
            DefaultQuizStudentViewRenderer renderer = renderer();
            int index = 1;
            for (String quizId : List.of(VEGETABLES, WORK, COME)) {
                QuizTemplateEntity quiz = TandaFixture.exercise(course, quizId);
                pairs.add(new BlindPairView(index++, new StudentSide(renderer.render(TandaFixture.auditable(quiz)), null),
                        new StudentSide(renderer.render(TandaFixture.auditable(quiz)), null)));
            }
        }

        @Override
        public BlindSample draw(java.nio.file.Path coursePath,
                com.learney.contentaudit.revisiondomain.projection.ProjectionScope scope, Long seed) {
            drawn.add(scope != null ? scope.getScopeId() : null);
            BlindSample sample = new BlindSample();
            sample.setSampleId(SAMPLE);
            return sample;
        }

        @Override
        public List<BlindPairView> pairs(String sampleId) {
            return pairs;
        }

        @Override
        public void answer(String sampleId, int pairIndex, PairAnswer answer, String reviewer) {
            answers.add(sampleId + " " + pairIndex + " " + answer + " " + reviewer);
        }

        @Override
        public BlindSampleResult close(String sampleId) {
            closed = true;
            return result(sampleId).orElseThrow();
        }

        @Override
        public Optional<BlindSampleResult> result(String sampleId) {
            return closed ? Optional.of(new BlindSampleResult(sampleId, BATCH, "José", answers.size(), null, null, null,
                    true, List.of(), null, null, List.of())) : Optional.empty();
        }
    }

    /** José's decisions in memory, as the file keeps them. */
    public static final class Decisions implements TrayDecisionStore {
        public final List<RememberedDecision> remembered = new ArrayList<>();

        @Override
        public void remember(RememberedDecision decision) {
            remembered.add(decision);
        }

        @Override
        public Optional<RememberedDecision> recall(String identityKey, String marker, String contentFingerprint) {
            RememberedDecision latest = null;
            for (RememberedDecision decision : remembered) {
                if (Objects.equals(decision.getIdentityKey(), identityKey) && Objects.equals(decision.getMarker(), marker)
                        && Objects.equals(decision.getContentFingerprint(), contentFingerprint)) {
                    latest = decision;
                }
            }
            return Optional.ofNullable(latest);
        }
    }

    public static DefaultQuizStudentViewRenderer renderer() {
        return new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create());
    }

    public static FingerprintDecisionMemory memory(TrayDecisionStore store) {
        return new FingerprintDecisionMemory(store, new Sha256ContentFingerprinter());
    }

    public static DefaultTrayAssembler assembler(TrayDecisionStore store) {
        return new DefaultTrayAssembler(new SeededRuleSampleDrawer(), memory(store), renderer());
    }

    // -------------------------------------------------------------------------
    // The tray over the real batch
    // -------------------------------------------------------------------------

    /**
     * The tray service over the real service of the batch -- its ledger, its gate, its closer and its publisher -- with
     * the panel, F's comparison, its blind sample and its book of goals by hand.
     */
    public static final class Bandeja {
        public final CourseEntity course = course();

        public final TandaFixture.Courses courses = new TandaFixture.Courses(course);

        public final TandaFixture.Artifacts artifacts = new TandaFixture.Artifacts();

        public final TandaFixture.Batches batches = new TandaFixture.Batches();

        public final TandaFixture.Plans plans = new TandaFixture.Plans(plan());

        public final TandaFixture.Reports reports = new TandaFixture.Reports();

        public final TandaFixture.ActivePair active = new TandaFixture.ActivePair();

        public final TandaFixture.Panels panels = new TandaFixture.Panels();

        public final TandaFixture.Comparator comparator = new TandaFixture.Comparator();

        public final Ciega ciega = new Ciega(course);

        public final Decisions decisions = new Decisions();

        /** What the published analysis still marks. */
        public List<Finding> remaining = new ArrayList<>();

        public final CorrectionBatch batch;

        public final BatchService batchService;

        public final DefaultTrayService tray;

        public Bandeja() {
            reports.put(TandaFixture.BASELINE, TandaFixture.report(course, List.of(MesaFixture.missingHint(),
                    MesaFixture.anotherAnswer(), comeAnswer(), teaAnswer(), workHint(), courseTie()), Map.of(),
                    MesaFixture.ANALYZERS));
            reports.put(TandaFixture.START_AUDIT, reports.load(TandaFixture.BASELINE).orElseThrow());
            batch = batch(course, artifacts);
            batches.save(batch);
            // F's comparison by hand: every change of the scope fixes the findings of its exercise.
            comparator.answer = scope -> {
                List<com.learney.contentaudit.revisiondomain.comparison.ChangeOutcome> outcomes = new ArrayList<>();
                for (String proposalId : scope.getProposalIds()) {
                    String quizId = proposalId.substring(BATCH.length() + 1);
                    outcomes.add(new com.learney.contentaudit.revisiondomain.comparison.ChangeOutcome(proposalId,
                            AuditTarget.QUIZ, quizId, false, new ArrayList<>(), fixedOn(quizId), new ArrayList<>()));
                }
                return TandaFixture.passing(scope, outcomes);
            };
            AuditEngine engine = new AuditEngine() {
                @Override
                public AuditReport runAudit(AuditableCourse auditable) {
                    throw new AssertionError("the published course is analyzed with the analyzers of the batch");
                }

                @Override
                public AuditReport runAudit(AuditableCourse auditable, AnalyzerRunSelection selection) {
                    return TandaFixture.report(courses.course, remaining, Map.of(), MesaFixture.ANALYZERS);
                }
            };
            RefinerEngine refiner = new RefinerEngine() {
                @Override
                public RefinementPlan plan(AuditReport report, String auditId) {
                    return new RefinementPlan(null, auditId, Instant.parse("2026-10-01T18:00:00Z"), new ArrayList<>());
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
            BatchServiceConfig config = new BatchServiceConfig(batches, plans, reports, active, artifacts, courses,
                    TandaFixture.locator(), auditable -> new AuditableCourse(List.of()), engine, refiner,
                    TandaFixture.briefer(), MesaFixture.corrections(), List.of(), panels, new TandaFixture.Meter(),
                    new DefaultPanelRunnerFactory(), comparator,
                    new TandaFixture.Baselines(new Baseline(TandaFixture.BASELINE, Instant.parse("2026-09-29T08:00:00Z"))),
                    ciega, null, TandaFixture.correctionScope(), new DefaultPreservationFactory().createCheck(),
                    TandaFixture.COURSE_PATH, new TandaFixture.Goals(), null);
            config.setProductionDocumentProjector(
                    new com.learney.contentaudit.revisiondomain.publicationengine.EntityProjector());
            batchService = new DefaultBatchServiceFactory().create(config);
            tray = new DefaultTrayService(batchService, batches, ciega, panels, new DefaultPanelRunnerFactory(),
                    assembler(decisions), memory(decisions), courses, TandaFixture.COURSE_PATH);
        }

        /** The findings a change of the exercise fixes. */
        static List<Finding> fixedOn(String quizId) {
            List<Finding> fixed = new ArrayList<>();
            for (Finding finding : List.of(MesaFixture.missingHint(), MesaFixture.anotherAnswer(), comeAnswer(), teaAnswer(),
                    workHint())) {
                if (quizId.equals(finding.getNode().getNodeId())) {
                    fixed.add(finding);
                }
            }
            return fixed;
        }

        /** The batch as its store has it now. */
        public CorrectionBatch saved() {
            return batches.load(BATCH).orElseThrow();
        }

        /** José answers every pair of the blind sample, and the last one closes it. */
        public void answerTheBlindSample() {
            for (BlindPairView pair : ciega.pairs) {
                tray.answer(new com.learney.contentaudit.revisiondomain.tray.TrayResponse(BandejaFixture.BLIND_ITEM,
                        com.learney.contentaudit.revisiondomain.tray.TrayAnswer.BLIND_PAIR, pair.getIndex(),
                        PairAnswer.B_BETTER, null, null, null, "José"));
            }
        }

        public DefaultGateFactory gates() {
            return new DefaultGateFactory();
        }
    }

    public static final String BLIND_ITEM = "ciega-" + SAMPLE;
}
