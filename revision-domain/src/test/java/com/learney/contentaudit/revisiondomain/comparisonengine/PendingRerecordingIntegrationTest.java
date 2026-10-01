package com.learney.contentaudit.revisiondomain.comparisonengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.learney.contentaudit.auditdomain.AnalyzerProvider;
import com.learney.contentaudit.auditdomain.AudioRecordingRegistry;
import com.learney.contentaudit.auditdomain.AuditEngine;
import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditReportStore;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.Baseline;
import com.learney.contentaudit.auditdomain.BaselineStore;
import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.auditdomain.IAuditEngine;
import com.learney.contentaudit.auditdomain.IScoreAggregator;
import com.learney.contentaudit.auditdomain.ProductionSentenceSource;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.contextnumbers.AnalyzerErrorCounts;
import com.learney.contentaudit.auditdomain.contextnumbers.ErrorCounts;
import com.learney.contentaudit.auditdomain.correctormatch.DefaultCorrectorMatcher;
import com.learney.contentaudit.auditdomain.correctormatch.FormEquivalenceTable;
import com.learney.contentaudit.auditdomain.datacoherence.AudioRecording;
import com.learney.contentaudit.auditdomain.datacoherence.DataCoherenceAnalyzerProvider;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.findingengine.DefaultAnalyzerCatalog;
import com.learney.contentaudit.auditdomain.findingengine.DefaultContextNumbersCalculator;
import com.learney.contentaudit.auditdomain.findingengine.DefaultFindingCollector;
import com.learney.contentaudit.auditdomain.offlevelwords.DefaultTopicTeachingResolver;
import com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.revisiondomain.SealedGoalsStore;
import com.learney.contentaudit.revisiondomain.comparison.BatchComparison;
import com.learney.contentaudit.revisiondomain.comparison.ChangeAttribution;
import com.learney.contentaudit.revisiondomain.comparison.ChangeOutcome;
import com.learney.contentaudit.revisiondomain.comparison.ComparisonRow;
import com.learney.contentaudit.revisiondomain.comparison.FloorBreach;
import com.learney.contentaudit.revisiondomain.comparison.GoalDeclaration;
import com.learney.contentaudit.revisiondomain.comparison.GoalStatus;
import com.learney.contentaudit.revisiondomain.comparison.SealedGoals;
import com.learney.contentaudit.revisiondomain.comparison.VerdictKind;
import com.learney.contentaudit.revisiondomain.consolidatedview.ConsolidatedViewAvailability;
import com.learney.contentaudit.revisiondomain.panelengine.MesaFixture;
import com.learney.contentaudit.revisiondomain.panelengine.MeterBench;
import com.learney.contentaudit.revisiondomain.projection.CourseProjection;
import com.learney.contentaudit.revisiondomain.projection.ProjectedChange;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * The audio follows the exercise (orchestrator, 1/10). In the control of F the fix of the vegetables -- «I ___ a lot of
 * vegetables.» rewritten as «I ___ vegetables.» -- did not pass: data-coherence marked that its audio and the sentence
 * production keeps still say «I love a lot of vegetables.», and that raised by one the exercises with an error of its
 * topic, of A1 and of the course. Those copies are pending -- the audio until José records it with the new text, the
 * sentence until the course goes to production --: the comparison of the batch says so and does not count them, while
 * what the change really breaks still counts.
 *
 * <p>Everything is measured by the real engine with the real data-coherence, over the twenty exercises of «Afirmativas
 * regulares», each recorded and kept in production with what it says at the batch start.
 */
public class PendingRerecordingIntegrationTest {

    static final String A1 = "6814dafa7d73e7209a13d382";
    static final String VEGETABLES = "67fab6d59930102295341ecd";
    static final String FRUIT = "67fab6d59930102295341ec8";
    static final String P_VEGETABLES = "tanda-a1-vegetables";
    static final String P_FRUIT = "tanda-a1-fruit";
    static final String ANY_ERROR = "algún error";
    static final String DATA_COHERENCE = "data-coherence";

    @Test
    @DisplayName("should not count the audio and the production sentence the vegetables fix leaves saying «I love a lot of vegetables.», and say them pending")
    @Tag("FEAT-NOEMP")
    @Tag("F-NOEMP-R004")
    @Tag("F-TANDA-R012")
    public void shouldNotCountTheAudioAndTheProductionSentenceTheVegetablesFixLeavesSayingTheOldSentenceAndSayThemPending() {
        Copias copias = new Copias();
        KnowledgeEntity start = MesaFixture.afirmativas();
        copias.recordAndPublish(start);
        QuizTemplateEntity vegetables = MesaFixture.exercise(start, VEGETABLES);
        KnowledgeEntity batch = copias.with(start, MesaFixture.withParts(vegetables, MesaFixture.text("I"),
                MesaFixture.cloze("love"), MesaFixture.text("vegetables.")));

        AuditReport before = copias.measure(start);
        AuditReport after = copias.measure(batch);
        assertTrue(findings(before, VEGETABLES).isEmpty(), "at the start every copy says what the exercise says");
        assertEquals(List.of("data-coherence|audio-matches|" + VEGETABLES + "|audio",
                        "data-coherence|saved-sentence|" + VEGETABLES + "|produccion"),
                findings(after, VEGETABLES).stream().map(DefaultBatchComparator::identity).toList(),
                "the real data-coherence marks the audio and the production sentence of the rewritten exercise");
        assertEquals(1, anyError(after, A1), "without settling them, A1 would gain an exercise with an error");

        BatchComparison comparison = copias.compare(before, after, cambio(P_VEGETABLES, VEGETABLES));

        for (String node : List.of(A1, MesaFixture.AFIRMATIVAS, ReportTree.COURSE_ID)) {
            assertEquals(0.0, row(comparison, ANY_ERROR, node).getProjected().getValue(),
                    "the floor of errors does not go up in " + node);
            assertEquals(0.0, row(comparison, DATA_COHERENCE, node).getProjected().getValue(),
                    "nor the exercises with an error of data-coherence in " + node);
        }
        ChangeOutcome change = change(comparison, P_VEGETABLES);
        assertFalse(change.isReturns(), "the change is not returned for copies that follow it");
        assertTrue(change.getNewFindings().isEmpty(), "they are not reasons: " + change.getNewFindings());
        assertTrue(comparison.getBreaches().isEmpty(), "no floor breaks: " + comparison.getBreaches());
        assertEquals(GoalStatus.MET, row(comparison, ANY_ERROR, A1).getFloor().getStatus());
        assertEquals(VerdictKind.BLIND_SAMPLE_MISSING, comparison.getVerdict().getKind(),
                "the floors hold and nothing returns: what is missing is the blind sample");
        assertTrue(comparison.getVerdict().getDetail().contains(
                "Pendiente de regrabar, sin contar como error: 1 audio (" + VEGETABLES + ")."),
                comparison.getVerdict().getDetail());
        assertTrue(comparison.getVerdict().getDetail().contains(
                "Por llevar a producción, sin contar como error: 1 oración guardada (" + VEGETABLES + ")."),
                comparison.getVerdict().getDetail());
        for (Finding finding : findings(after, VEGETABLES)) {
            assertEquals(FindingResolution.RANK_ONLY, finding.getResolution(),
                    "they stay in the projection, without counting: " + DefaultBatchComparator.identity(finding));
        }
        AnalyzerErrorCounts dataCoherence = errorCounts(after, A1, DATA_COHERENCE);
        assertEquals(0, dataCoherence.getWithError());
        assertEquals(1, dataCoherence.getMarked(), "the exercise is marked, as what only orders");
        assertEquals(0, errors(after, A1).getBySeverity().getMedium(), "nor counted by its severity");
    }

    @Test
    @DisplayName("should still return a change for what it really breaks, and name only it for the broken floor of A1, with its pending copies counted in no variant")
    @Tag("FEAT-NOEMP")
    @Tag("F-NOEMP-R004")
    @Tag("F-METAS-R004")
    public void shouldStillReturnAChangeForWhatItReallyBreaksAndNameOnlyItForTheBrokenFloorOfA1WithItsPendingCopiesCountedInNoVariant() {
        Copias copias = new Copias();
        KnowledgeEntity start = MesaFixture.afirmativas();
        copias.recordAndPublish(start);
        QuizTemplateEntity vegetables = MesaFixture.withParts(MesaFixture.exercise(start, VEGETABLES),
                MesaFixture.text("I"), MesaFixture.cloze("love"), MesaFixture.text("vegetables."));
        // «You ___ a lot of fruit.» rewritten as «You ___ fruit.» without building its sentence again: the tree of the
        // course goes on saying «You eat a lot of fruit.» -- that is the change's, and counts.
        QuizTemplateEntity fruit = MesaFixture.exercise(start, FRUIT);
        QuizTemplateEntity staleTree = MesaFixture.withParts(fruit, MesaFixture.text("You"), MesaFixture.cloze("eat"),
                MesaFixture.text("(eat) fruit."));
        staleTree.setSentences(new ArrayList<>(fruit.getSentences()));
        KnowledgeEntity batch = copias.with(copias.with(start, vegetables), staleTree);

        AuditReport before = copias.measure(start);
        AuditReport after = copias.measure(batch);
        assertEquals(2, anyError(after, A1), "without settling: the vegetables and the fruit");

        BatchComparison comparison = copias.compare(before, after, cambio(P_VEGETABLES, VEGETABLES),
                cambio(P_FRUIT, FRUIT));

        assertEquals(1.0, row(comparison, ANY_ERROR, A1).getProjected().getValue(), "only the fruit has an error");
        assertEquals(1.0, row(comparison, DATA_COHERENCE, A1).getProjected().getValue());
        ChangeOutcome fruitChange = change(comparison, P_FRUIT);
        assertTrue(fruitChange.isReturns(), "the stale tree sentence is the change's");
        assertEquals(List.of("data-coherence|saved-sentence|" + FRUIT + "|arbol"),
                fruitChange.getNewFindings().stream().map(DefaultBatchComparator::identity).toList(),
                "its reason is the tree sentence, not its audio nor its production sentence");
        assertFalse(change(comparison, P_VEGETABLES).isReturns());
        assertEquals(List.of(P_FRUIT), comparison.getVerdict().getReturningProposalIds());
        assertTrue(comparison.getVerdict().getDetail().contains("2 audios (" + VEGETABLES + ", " + FRUIT + ")"),
                comparison.getVerdict().getDetail());

        FloorBreach a1 = null;
        for (FloorBreach breach : comparison.getBreaches()) {
            if (breach.getRow() != null && ANY_ERROR.equals(breach.getRow().getSubject())
                    && A1.equals(breach.getRow().getNodeId())) {
                a1 = breach;
            }
        }
        assertNotNull(a1, "«algún error» in A1 breaks its floor 0: " + comparison.getBreaches());
        List<ChangeAttribution> named = a1.getAttributions();
        assertEquals(List.of(P_FRUIT), named.stream().map(ChangeAttribution::getProposalId).toList(),
                "R004: the fruit breaks it by itself; the vegetables, with its copies pending, does not move it");
        assertEquals(1.0, named.get(0).getEffect(), 1e-9);
        assertEquals(2, copias.variants, "one engine run per change of the batch");
    }

    // =========================================================================

    static ProjectedChange cambio(String proposalId, String quizId) {
        return new ProjectedChange(proposalId, AuditTarget.QUIZ, quizId, true);
    }

    static ComparisonRow row(BatchComparison comparison, String subject, String nodeId) {
        for (ComparisonRow row : comparison.getRows()) {
            if (subject.equals(row.getSubject()) && nodeId.equals(row.getNodeId())) {
                return row;
            }
        }
        throw new AssertionError("no row of " + subject + " in " + nodeId + ": " + comparison.getRows().stream()
                .map(r -> r.getSubject() + "@" + r.getNodeId()).toList());
    }

    static ChangeOutcome change(BatchComparison comparison, String proposalId) {
        for (ChangeOutcome outcome : comparison.getChanges()) {
            if (proposalId.equals(outcome.getProposalId())) {
                return outcome;
            }
        }
        throw new AssertionError("no change " + proposalId);
    }

    static List<Finding> findings(AuditReport report, String quizId) {
        AuditNode node = ReportTree.index(report).get(ReportTree.key(AuditTarget.QUIZ, quizId));
        List<Finding> findings = new ArrayList<>();
        for (Finding finding : node.getFindings()) {
            if (DATA_COHERENCE.equals(finding.getAnalyzer())) {
                findings.add(finding);
            }
        }
        findings.sort(java.util.Comparator.comparing(DefaultBatchComparator::identity));
        return findings;
    }

    static ErrorCounts errors(AuditReport report, String milestoneId) {
        return ReportTree.index(report).get(ReportTree.key(AuditTarget.MILESTONE, milestoneId)).getNumbers().getErrors();
    }

    static int anyError(AuditReport report, String milestoneId) {
        return errors(report, milestoneId).getWithAnyError();
    }

    static AnalyzerErrorCounts errorCounts(AuditReport report, String milestoneId, String analyzer) {
        for (AnalyzerErrorCounts counts : errors(report, milestoneId).getAnalyzers()) {
            if (analyzer.equals(counts.getAnalyzer())) {
                return counts;
            }
        }
        throw new AssertionError("no counts of " + analyzer);
    }

    /**
     * The copies of the course outside it -- the registry of recordings and the sentences production keeps -- and the
     * real engine with the real data-coherence over them; the comparator of F with its real attributor.
     */
    static final class Copias {
        final Map<String, String> recorded = new LinkedHashMap<>();
        final Map<String, String> production = new LinkedHashMap<>();
        final MeterBench bench = new MeterBench();
        final AuditEngine engine;
        int variants;

        Copias() {
            AudioRecordingRegistry recordings = new AudioRecordingRegistry() {
                @Override
                public Optional<AudioRecording> recordingOf(String audioKey) {
                    return Optional.ofNullable(recorded.get(audioKey)).map(text -> new AudioRecording(audioKey, text,
                            "registro de prueba"));
                }

                @Override
                public String version() {
                    return "registro de prueba";
                }
            };
            ProductionSentenceSource kept = quizId -> Optional.ofNullable(production.get(quizId)).map(List::of);
            List<AnalyzerProvider> providers = List.of(new DataCoherenceAnalyzerProvider(
                    new DefaultCorrectorMatcher(new FormEquivalenceTable("prueba", List.of(), List.of(), false,
                            List.of())),
                    recordings, kept, new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create())));
            AuditEngine real = new IAuditEngine(new IScoreAggregator(), new DefaultAnalyzerCatalog(providers),
                    new DefaultFindingCollector(), new DefaultContextNumbersCalculator());
            this.catalog = new DefaultAnalyzerCatalog(providers);
            this.engine = new AuditEngine() {
                @Override
                public AuditReport runAudit(com.learney.contentaudit.auditdomain.AuditableCourse course) {
                    variants++;
                    return real.runAudit(course);
                }

                @Override
                public AuditReport runAudit(com.learney.contentaudit.auditdomain.AuditableCourse course,
                        AnalyzerRunSelection selection) {
                    variants++;
                    return real.runAudit(course, selection);
                }
            };
        }

        final DefaultAnalyzerCatalog catalog;

        /** Each exercise recorded and kept in production with what it says. */
        void recordAndPublish(KnowledgeEntity topic) {
            for (QuizTemplateEntity quiz : topic.getQuizTemplates()) {
                recorded.put(quiz.getAnswerAudioUrl(), MesaFixture.stored(quiz));
                production.put(quiz.getId(), MesaFixture.stored(quiz));
            }
        }

        /** The topic with an exercise as the batch leaves it. */
        KnowledgeEntity with(KnowledgeEntity topic, QuizTemplateEntity changed) {
            List<QuizTemplateEntity> quizzes = new ArrayList<>();
            for (QuizTemplateEntity quiz : topic.getQuizTemplates()) {
                quizzes.add(quiz.getId().equals(changed.getId()) ? changed : quiz);
            }
            return MesaFixture.topic(topic.getId(), topic.getLabel(), quizzes);
        }

        AuditReport measure(KnowledgeEntity topic) {
            CourseEntity course = MeterBench.course(A1, CefrLevel.A1, topic);
            AuditReport report = engine.runAudit(bench.mapper().map(course),
                    new AnalyzerRunSelection(List.of(DATA_COHERENCE), Map.of()));
            variants = 0;
            return report;
        }

        BatchComparison compare(AuditReport start, AuditReport projected, ProjectedChange... changes) {
            BaselineStore baselines = mock(BaselineStore.class);
            when(baselines.read()).thenReturn(Optional.of(new Baseline("base", Instant.parse("2026-10-01T09:00:00Z"))));
            AuditReportStore reports = mock(AuditReportStore.class);
            when(reports.load("base")).thenReturn(Optional.of(start));
            CourseProjection projection = new CourseProjection("auditoria", "plan", "tanda-a1",
                    ConsolidatedViewAvailability.AVAILABLE, null, start, start, projected, List.of(changes), List.of());
            List<SealedGoals> sealed = new ArrayList<>();
            SealedGoalsStore sealedStore = new SealedGoalsStore() {
                @Override
                public Optional<SealedGoals> read() {
                    return sealed.isEmpty() ? Optional.empty() : Optional.of(sealed.get(sealed.size() - 1));
                }

                @Override
                public void write(SealedGoals goals) {
                    sealed.add(goals);
                }
            };
            GoalDeclaration declaration = new GoalDeclaration(List.of(),
                    List.of(DefaultBatchComparatorTest.ANY_ERROR_IN_A1_ZERO), null);
            DefaultBatchComparator comparator = new DefaultBatchComparator((course, scope) -> projection, baselines,
                    reports, new DefaultGoalBook(() -> declaration, sealedStore, catalog), catalog, engine,
                    new LeaveOneOutChangeAttributor(engine), scope -> Optional.empty(),
                    new DefaultTopicTeachingResolver(), new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create()));
            return comparator.compare(Path.of("db/english-course"), null);
        }
    }
}
