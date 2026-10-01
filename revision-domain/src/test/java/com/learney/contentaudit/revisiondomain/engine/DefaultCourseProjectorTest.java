package com.learney.contentaudit.revisiondomain.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.learney.contentaudit.auditdomain.ActiveAnalysisSelection;
import com.learney.contentaudit.auditdomain.ActiveAnalysisSelectionStore;
import com.learney.contentaudit.auditdomain.AnalyzerDescriptor;
import com.learney.contentaudit.auditdomain.AnalyzerProvider;
import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditReportStore;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableCourse;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableMilestone;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.AuditableTopic;
import com.learney.contentaudit.auditdomain.ContentAnalyzer;
import com.learney.contentaudit.auditdomain.CourseMapper;
import com.learney.contentaudit.auditdomain.DefaultTopicDiagnoses;
import com.learney.contentaudit.auditdomain.EvaluationRunPolicy;
import com.learney.contentaudit.auditdomain.IAuditEngine;
import com.learney.contentaudit.auditdomain.IScoreAggregator;
import com.learney.contentaudit.auditdomain.SelfDescribingConfig;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerFamily;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerPlanBinding;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRuleCard;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.coca.BucketSummary;
import com.learney.contentaudit.auditdomain.coca.CocaBucketsTopicDiagnosis;
import com.learney.contentaudit.auditdomain.finding.AnalysisCost;
import com.learney.contentaudit.auditdomain.finding.EvidencePart;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingDraft;
import com.learney.contentaudit.auditdomain.finding.FindingEvidence;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.auditdomain.findingengine.DefaultAnalyzerCatalog;
import com.learney.contentaudit.auditdomain.findingengine.DefaultContextNumbersCalculator;
import com.learney.contentaudit.auditdomain.findingengine.DefaultFindingCollector;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.CourseRepository;
import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.MilestoneEntity;
import com.learney.contentaudit.coursedomain.NodeKind;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.RootNodeEntity;
import com.learney.contentaudit.coursedomain.SentenceMode;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.coursedomain.TopicEntity;
import com.learney.contentaudit.refinerdomain.DiagnosisKind;
import com.learney.contentaudit.refinerdomain.RefinementPlan;
import com.learney.contentaudit.refinerdomain.RefinementPlanStore;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.RevisionArtifact;
import com.learney.contentaudit.revisiondomain.RevisionArtifactStore;
import com.learney.contentaudit.revisiondomain.RevisionOutcomeKind;
import com.learney.contentaudit.revisiondomain.RevisionProposal;
import com.learney.contentaudit.revisiondomain.RevisionVerdict;
import com.learney.contentaudit.revisiondomain.consolidatedview.ConsolidatedViewAvailability;
import com.learney.contentaudit.revisiondomain.consolidatedview.NonApplicablePending;
import com.learney.contentaudit.revisiondomain.consolidatedview.NonApplicablePendingReason;
import com.learney.contentaudit.revisiondomain.projection.CourseProjection;
import com.learney.contentaudit.revisiondomain.projection.ProjectedChange;
import com.learney.contentaudit.revisiondomain.projection.ProjectionScope;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.mockito.ArgumentCaptor;

import javax.annotation.processing.Generated;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Generated(
        value = "com.sentinel.SentinelEngine",
        comments = "Generated by Sentinel"
)
public class DefaultCourseProjectorTest {
    @Test
    @DisplayName("should give the photo with the four pending proposals of «Afirmativas regulares» the numbers of analyze on the copy with them written, 73.92% in the course and 96.54, 94.16, 79.45 and 63.51 in the levels, and not 92.38 in A1")
    @Tag("FEAT-PROYU")
    @Tag("F-PROYU-R001")
    public void shouldGiveThePhotoWithTheFourPendingProposalsOfAfirmativasRegularesTheNumbersOfAnalyzeOnTheCopyWithThemWritten7392InTheCourseAnd965494167945And6351InTheLevelsAndNot9238InA1(
            ) {
        // R001: with the four proposals of «Afirmativas regulares» pending, every node of the photo with them --
        // the course and its four levels included -- has what analyze gives on a copy of the course where they
        // were written as approve writes them. The 29/9 numbers (73,92 % and 96,54 · 94,16 · 79,45 · 63,51) are
        // fixed on the real course by FProyuJ001JourneyTest; here the same property on the course of the fixture.
        Curso.Mesa mesa = new Curso.Mesa(Curso.course());
        mesa.pending(Curso.GRANDMOTHER, Curso.OURSELVES, Curso.VEGETABLES, Curso.HATS);

        CourseProjection projection = mesa.projector().project(Curso.COURSE, null);

        CourseEntity written = Curso.written(Curso.course(), Curso.after(Curso.GRANDMOTHER), Curso.after(Curso.OURSELVES),
                Curso.after(Curso.VEGETABLES), Curso.after(Curso.HATS));
        AuditReport analyzed = Curso.analyze(written);
        assertEquals(ConsolidatedViewAvailability.AVAILABLE, projection.getAvailability());
        assertEquals(Curso.vocabulary(analyzed), Curso.vocabulary(projection.getProjected()),
                "R001: every node, the course included, as analyze on the copy with the four written");
        assertEquals(Curso.percent(Curso.node(analyzed, "root").getNumbers().getVocabularyScore()),
                Curso.percent(Curso.node(projection.getProjected(), "root").getNumbers().getVocabularyScore()),
                "R001: the course");
        for (String level : Curso.LEVELS) {
            assertEquals(Curso.node(analyzed, level).getNumbers().getVocabularyScore(),
                    Curso.node(projection.getProjected(), level).getNumbers().getVocabularyScore(),
                    "R001: level " + level);
        }
        assertEquals(Curso.findings(analyzed), Curso.findings(projection.getProjected()), "R001: the same findings");
    }

    @Test
    @DisplayName("should lower «My grandmother ___ (come) every year.» from 1.0 to 0.9 in lemma-absence in the photo with pending proposals, with grandmother (A2) as an off-level word, and leave every topic as it was")
    @Tag("FEAT-PROYU")
    @Tag("F-PROYU-R001")
    public void shouldLowerMyGrandmotherComeEveryYearFrom10To09InLemmaabsenceInThePhotoWithPendingProposalsWithGrandmotherA2AsAnOfflevelWordAndLeaveEveryTopicAsItWas(
            ) {
        // R001: «It ___ (come) every year.» becomes «My grandmother ___ (come) every year.»: in the photo with the
        // pending proposals lemma-absence gives it 0,9 with grandmother (A2) as a word of another level, and every
        // topic keeps its COCA tokens; only the topic of the proposals moves, and only in lemma-absence.
        Curso.Mesa mesa = new Curso.Mesa(Curso.course());
        mesa.pending(Curso.GRANDMOTHER, Curso.OURSELVES, Curso.VEGETABLES, Curso.HATS);

        CourseProjection projection = mesa.projector().project(Curso.COURSE, null);

        AuditNode before = Curso.node(projection.getApproved(), Curso.GRANDMOTHER);
        AuditNode after = Curso.node(projection.getProjected(), Curso.GRANDMOTHER);
        assertEquals(1.0, before.getScores().get(Curso.LEMMA_ABSENCE), 1e-12, "R001: 1,0 with what is approved");
        assertEquals(0.9, after.getScores().get(Curso.LEMMA_ABSENCE), 1e-12, "R001: 0,9 with the pending proposals");
        Finding offLevel = after.getFindings().stream().filter(f -> Curso.LEMMA_ABSENCE.equals(f.getAnalyzer()))
                .findFirst().orElseThrow(() -> new AssertionError("R001: the word of another level is a finding"));
        assertEquals("grandmother", offLevel.getIdentity().getMarker(), "R001: grandmother");
        assertTrue(offLevel.getEvidence().getExamined().contains(new EvidencePart("Nivel de la palabra", "A2")),
                "R001: grandmother is A2");
        AuditReport written = Curso.analyze(Curso.written(Curso.course(), Curso.after(Curso.GRANDMOTHER),
                Curso.after(Curso.OURSELVES), Curso.after(Curso.VEGETABLES), Curso.after(Curso.HATS)));
        for (String topic : Curso.TOPICS) {
            assertEquals(Curso.cocaTokens(projection.getOriginal(), topic), Curso.cocaTokens(projection.getApproved(), topic),
                    "R001: topic " + topic + " keeps its COCA tokens with what is approved");
            if (Curso.PRESENT_SIMPLE.equals(topic)) {
                assertEquals(Curso.cocaTokens(written, topic), Curso.cocaTokens(projection.getProjected(), topic),
                        "R001: the topic of the proposals counts the words of the sentences they write, never 0");
            } else {
                assertEquals(Curso.cocaTokens(projection.getOriginal(), topic), Curso.cocaTokens(projection.getProjected(), topic),
                        "R001: every other topic keeps its COCA tokens with the pending proposals");
                assertEquals(Curso.node(projection.getApproved(), topic).getScores(),
                        Curso.node(projection.getProjected(), topic).getScores(), "R001: topic " + topic + " as it was");
            }
        }
    }

    @Test
    @DisplayName("should give the photo with the approved proposals, when nothing is approved, the numbers, findings and diagnoses of the active analysis, the COCA tokens of the 55 topics included")
    @Tag("FEAT-PROYU")
    @Tag("F-PROYU-R001")
    public void shouldGiveThePhotoWithTheApprovedProposalsWhenNothingIsApprovedTheNumbersFindingsAndDiagnosesOfTheActiveAnalysisTheCOCATokensOfThe55TopicsIncluded(
            ) {
        // R001: nothing is approved, so the photo with what is approved measures the course of the active analysis
        // and gives, in every node, its numbers, findings and diagnoses -- the COCA tokens of every topic included.
        Curso.Mesa mesa = new Curso.Mesa(Curso.course());
        mesa.pending(Curso.GRANDMOTHER);

        CourseProjection projection = mesa.projector().project(Curso.COURSE, null);

        assertEquals(Curso.vocabulary(projection.getOriginal()), Curso.vocabulary(projection.getApproved()),
                "R001: the numbers of the active analysis, in every node");
        assertEquals(Curso.findings(projection.getOriginal()), Curso.findings(projection.getApproved()),
                "R001: its findings");
        for (String topic : Curso.TOPICS) {
            assertEquals(Curso.cocaTokens(projection.getOriginal(), topic), Curso.cocaTokens(projection.getApproved(), topic),
                    "R001: the COCA tokens of topic " + topic);
            assertTrue(Curso.cocaTokens(projection.getApproved(), topic) > 0, "R001: a topic with its tokens, never 0");
        }
        assertEquals(List.of(), projection.getChanges().stream().filter(c -> !c.isPending()).toList(),
                "nothing approved");
    }

    @Test
    @DisplayName("should declare in the projection the pending proposals it applied and those not applicable with their causes, the same lists its numbers come from")
    @Tag("FEAT-PROYU")
    @Tag("F-PROYU-R002")
    public void shouldDeclareInTheProjectionThePendingProposalsItAppliedAndThoseNotApplicableWithTheirCausesTheSameListsItsNumbersComeFrom(
            ) {
        // R002: the four pending proposals enter; «Winter ___ (come) every year.», newer and made on the same exercise
        // before «My grandmother…» changed it, does not; nor one on an exercise that is no longer in the course. The
        // projection declares both lists, and its numbers come from exactly the four.
        Curso.Mesa mesa = new Curso.Mesa(Curso.course());
        mesa.pending(Curso.GRANDMOTHER, Curso.OURSELVES, Curso.VEGETABLES, Curso.HATS);
        mesa.artifact(Curso.pending("p-winter", Curso.before(Curso.GRANDMOTHER), Curso.winter(),
                Instant.parse("2026-09-30T12:00:05Z")));
        QuizTemplateEntity gone = Curso.quiz("67fab6d59930102295349999", Curso.REGULAR, "I", "walk", "(walk) to school.",
                "I walk to school.", "Camino a la escuela.");
        mesa.artifact(Curso.pending("p-gone", gone, gone, Instant.parse("2026-09-30T12:00:06Z")));

        CourseProjection projection = mesa.projector().project(Curso.COURSE, null);

        assertEquals(List.of("p-" + Curso.GRANDMOTHER, "p-" + Curso.OURSELVES, "p-" + Curso.VEGETABLES, "p-" + Curso.HATS),
                projection.getChanges().stream().filter(ProjectedChange::isPending).map(ProjectedChange::getProposalId)
                        .toList(), "R002: the applied ones, in creation order");
        assertEquals(List.of("p-winter", "p-gone"), projection.getNonApplicablePendings().stream()
                .map(NonApplicablePending::getProposalId).toList(), "R002: the not applicable ones");
        assertEquals(NonApplicablePendingReason.ELEMENT_BEFORE_MISMATCH, projection.getNonApplicablePendings().get(0).getReason(),
                "R002: the newer one, with its cause");
        assertEquals(NonApplicablePendingReason.NODE_ABSENT, projection.getNonApplicablePendings().get(1).getReason(),
                "R002: the one whose node is gone, with its cause");
        AuditReport fromTheFour = Curso.analyze(Curso.written(Curso.course(), Curso.after(Curso.GRANDMOTHER),
                Curso.after(Curso.OURSELVES), Curso.after(Curso.VEGETABLES), Curso.after(Curso.HATS)));
        assertEquals(Curso.vocabulary(fromTheFour), Curso.vocabulary(projection.getProjected()),
                "R002: the numbers come from the four applied, and only from them");
    }

    @Test
    @DisplayName("should project only the pending proposals a batch names in its scope, every pending proposal of the active plan when the scope names none, and declare not applicable an id that is not a pending proposal of the active plan")
    @Tag("FEAT-PROYU")
    @Tag("F-PROYU-R002")
    public void shouldProjectOnlyThePendingProposalsABatchNamesInItsScopeEveryPendingProposalOfTheActivePlanWhenTheScopeNamesNoneAndDeclareNotApplicableAnIdThatIsNotAPendingProposalOfTheActivePlan(
            ) {
        // R002: a batch names its pending proposals in its scope; without a batch, every pending proposal of the
        // active plan is projected; an id the scope names that is not a pending proposal of the plan is declared.
        Curso.Mesa mesa = new Curso.Mesa(Curso.course());
        mesa.pending(Curso.GRANDMOTHER, Curso.OURSELVES, Curso.VEGETABLES, Curso.HATS);

        CourseProjection batch = mesa.projector().project(Curso.COURSE,
                new ProjectionScope("tanda-1", List.of("p-" + Curso.VEGETABLES, "p-otra"), Map.of()));
        CourseProjection whole = mesa.projector().project(Curso.COURSE, null);

        assertEquals(List.of("p-" + Curso.VEGETABLES), batch.getChanges().stream().filter(ProjectedChange::isPending)
                .map(ProjectedChange::getProposalId).toList(), "R002: only the pending proposal the batch names");
        assertEquals("tanda-1", batch.getScopeId(), "the projection of the batch carries its id");
        assertEquals(List.of("p-otra"), batch.getNonApplicablePendings().stream().map(NonApplicablePending::getProposalId)
                .toList(), "R002: an id that is not a pending proposal of the active plan");
        assertEquals(NonApplicablePendingReason.OTHER, batch.getNonApplicablePendings().get(0).getReason());
        assertTrue(batch.getNonApplicablePendings().get(0).getDetail().contains(Curso.PLAN),
                "with its cause: " + batch.getNonApplicablePendings().get(0).getDetail());
        assertEquals(1.0, Curso.node(batch.getProjected(), Curso.GRANDMOTHER).getScores().get(Curso.LEMMA_ABSENCE), 1e-12,
                "R002: «grandmother», not in the batch, is not projected");
        assertEquals(4, whole.getChanges().stream().filter(ProjectedChange::isPending).count(),
                "R002: without a batch, every pending proposal of the active plan");
        assertEquals(Curso.PLAN, whole.getScopeId(), "without a batch, the scope is the active plan");
    }

    @Test
    @DisplayName("should measure the photo with the approved proposals and the photo with the pending ones with the same analyzers and the same budgets of the scope")
    @Tag("FEAT-PROYU")
    @Tag("F-PROYU-R003")
    public void shouldMeasureThePhotoWithTheApprovedProposalsAndThePhotoWithThePendingOnesWithTheSameAnalyzersAndTheSameBudgetsOfTheScope(
            ) {
        // R003: both photos are measured with the analysis the projection starts from -- so with its analyzers --
        // and with the budgets of the scope.
        Curso.Mesa mesa = new Curso.Mesa(Curso.course());
        mesa.pending(Curso.GRANDMOTHER);
        ProjectionMeasurer measurer = mock(ProjectionMeasurer.class);
        when(measurer.measure(any(), any(), any())).thenReturn(mesa.active, Curso.analyze(Curso.course()));
        Map<String, Integer> budgets = Map.of("quiz-instruction", 3);

        mesa.projector(measurer).project(Curso.COURSE, new ProjectionScope("tanda-1", null, budgets));

        ArgumentCaptor<CourseEntity> courses = ArgumentCaptor.forClass(CourseEntity.class);
        ArgumentCaptor<AuditReport> starts = ArgumentCaptor.forClass(AuditReport.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Integer>> caps = ArgumentCaptor.forClass(Map.class);
        verify(measurer, times(2)).measure(courses.capture(), starts.capture(), caps.capture());
        assertSame(mesa.active, starts.getAllValues().get(0), "R003: the photo with what is approved, from the active analysis");
        assertSame(mesa.active, starts.getAllValues().get(1), "R003: and the photo with the pending ones, from the same");
        assertEquals(List.of(budgets, budgets), caps.getAllValues(), "R003: both with the budgets of the scope");
        assertEquals("It", Curso.find(courses.getAllValues().get(0), Curso.GRANDMOTHER).getForm().getSentenceParts().get(0).getText(),
                "the first, the course with what is approved");
        assertEquals("My grandmother", Curso.find(courses.getAllValues().get(1), Curso.GRANDMOTHER).getForm()
                .getSentenceParts().get(0).getText(), "the second, with the pending proposal written");
    }

    @Test
    @DisplayName("should start the projection from the active analysis and its plan, never from the latest analysis of the history nor from the baseline")
    @Tag("FEAT-PROYU")
    @Tag("F-PROYU-R006")
    public void shouldStartTheProjectionFromTheActiveAnalysisAndItsPlanNeverFromTheLatestAnalysisOfTheHistoryNorFromTheBaseline(
            ) {
        // R006: the history has a newer analysis than the active one and the baseline is another one; the projection
        // starts from the active pair and nothing else.
        Curso.Mesa mesa = new Curso.Mesa(Curso.course());
        mesa.pending(Curso.GRANDMOTHER);
        AuditReport latest = Curso.analyze(Curso.written(Curso.course(), Curso.after(Curso.HATS)));
        when(mesa.audits.loadLatest()).thenReturn(Optional.of(latest));
        when(mesa.audits.load(Curso.BASELINE)).thenReturn(Optional.of(latest));

        CourseProjection projection = mesa.projector().project(Curso.COURSE, null);

        assertEquals(Curso.AUDIT, projection.getActiveAuditId(), "R006: the active analysis");
        assertEquals(Curso.PLAN, projection.getActivePlanId(), "R006: and its plan");
        assertSame(mesa.active, projection.getOriginal(), "R006: the original photo is the active analysis");
        verify(mesa.audits, never()).loadLatest();
        verify(mesa.audits, never()).load(Curso.BASELINE);
        verify(mesa.plans).load(Curso.PLAN);
        verify(mesa.artifacts).listByPlan(Curso.PLAN);
    }

    @Test
    @DisplayName("should give the photo with pending proposals the numbers of a new invocation on that course, whether or not the photo with the approved ones was measured before in the same process")
    @Tag("FEAT-CERO")
    @Tag("F-CERO-R001")
    public void shouldGiveThePhotoWithPendingProposalsTheNumbersOfANewInvocationOnThatCourseWhetherOrNotThePhotoWithTheApprovedOnesWasMeasuredBeforeInTheSameProcess(
            ) {
        // F-CERO-R001: the projector measures the photo with what is approved and then, in the same process, the one
        // with the pending proposals; the second gives in every node what a new invocation gives on that course.
        // The analyzers of the fixture keep what they count, as the four of 29/9 did: only a new analyzer per run
        // keeps the second photo clean.
        Curso.Mesa mesa = new Curso.Mesa(Curso.course());
        mesa.pending(Curso.GRANDMOTHER, Curso.OURSELVES, Curso.VEGETABLES, Curso.HATS);

        CourseProjection projection = mesa.projector().project(Curso.COURSE, null);

        AuditReport newInvocation = Curso.analyze(Curso.written(Curso.course(), Curso.after(Curso.GRANDMOTHER),
                Curso.after(Curso.OURSELVES), Curso.after(Curso.VEGETABLES), Curso.after(Curso.HATS)));
        assertEquals(Curso.vocabulary(newInvocation), Curso.vocabulary(projection.getProjected()),
                "R001: the photo with the pending proposals, as a new invocation, in every node");
        for (String topic : Curso.TOPICS) {
            assertEquals(Curso.cocaTokens(newInvocation, topic), Curso.cocaTokens(projection.getProjected(), topic),
                    "R001: topic " + topic + ", counted from zero");
        }
    }

    @Test
    @DisplayName("should keep the COCA diagnosis of the 55 topics in the photo with the approved proposals after measuring the photo with the pending ones")
    @Tag("FEAT-CERO")
    @Tag("F-CERO-R002")
    public void shouldKeepTheCOCADiagnosisOfThe55TopicsInThePhotoWithTheApprovedProposalsAfterMeasuringThePhotoWithThePendingOnes(
            ) {
        // F-CERO-R002: measuring the photo with the pending proposals does not write on the photo with what is
        // approved: every topic of it keeps the COCA diagnosis it had, the tokens and the bands.
        Curso.Mesa mesa = new Curso.Mesa(Curso.course());
        mesa.pending(Curso.GRANDMOTHER, Curso.OURSELVES, Curso.VEGETABLES, Curso.HATS);
        AuditReport alone = Curso.analyze(Curso.course());

        CourseProjection projection = mesa.projector().project(Curso.COURSE, null);

        assertNotSame(projection.getApproved(), projection.getProjected(), "two photos, two reports");
        for (String topic : Curso.TOPICS) {
            assertEquals(Curso.cocaTokens(alone, topic), Curso.cocaTokens(projection.getApproved(), topic),
                    "R002: topic " + topic + " keeps its COCA tokens");
            assertEquals(Curso.cocaBands(alone, topic), Curso.cocaBands(projection.getApproved(), topic),
                    "R002: and its bands");
        }
    }

    /**
     * A small course with the exercises of «Afirmativas regulares» (A1 · Present Simple) the 077 fixed, with the
     * ids of the 29/9 course, and the stores of an active pair over it. Its two analyzers are doubles that keep
     * what they count, as the four analyzers of 29/9 did: «lemma-absence» marks a word of another level
     * (grandmother is A2) and «coca-buckets-distribution» counts the words of every topic. The engine, the
     * aggregation, the numbers and the findings are the real ones.
     */
    static final class Curso {
        static final String AUDIT = "2026-09-30T11-54-02";
        static final String PLAN = "2026-09-30T11-54-12";
        static final String BASELINE = "2026-09-29T08-00-00";
        static final Path COURSE = Path.of("db/english-course");
        static final String A1 = "6814dafa7d73e7209a13d382";
        static final String A2 = "6814dafa7d73e7209a13d218";
        static final String B1 = "6814dafa7d73e7209a13d2b5";
        static final String B2 = "6814dafa7d73e7209a13d3ef";
        static final List<String> LEVELS = List.of(A1, A2, B1, B2);
        static final String PRESENT_SIMPLE = "6814dafa7d73e7209a13d383";
        static final String A2_TOPIC = "6814dafa7d73e7209a13d219";
        static final String B1_TOPIC = "6814dafa7d73e7209a13d2b6";
        static final String PASSIVE = "6814dafa7d73e7209a13d3f0";
        static final List<String> TOPICS = List.of(PRESENT_SIMPLE, A2_TOPIC, B1_TOPIC, PASSIVE);
        /** «Afirmativas con $be$», the first knowledge of the course. */
        static final String WITH_BE = "6814dafa7d73e7209a13d384";
        /** «Afirmativas regulares», with 20 exercises. */
        static final String REGULAR = "6814dafa7d73e7209a13d389";
        static final String A2_KNOWLEDGE = "6814dafa7d73e7209a13d21a";
        static final String B1_KNOWLEDGE = "6814dafa7d73e7209a13d2b7";
        static final String PASSIVE_KNOWLEDGE = "6814dafa7d73e7209a13d3f1";
        static final String GRANDMOTHER = "67fab6d59930102295341ec4";
        static final String OURSELVES = "67fab6d59930102295341ecb";
        static final String VEGETABLES = "67fab6d59930102295341ecd";
        static final String HATS = "67fab6d59930102295341ed6";
        static final String B2_QUIZ = "67fab6d59930102295345b01";
        static final String LEMMA_ABSENCE = "lemma-absence";
        static final String COCA = "coca-buckets-distribution";
        /** The words of another level for an exercise of A1, with their level. */
        static final Map<String, String> OFF_LEVEL = Map.of("grandmother", "A2");

        private Curso() {
        }

        static CourseEntity course() {
            List<QuizTemplateEntity> regular = new ArrayList<>(List.of(
                    quiz(GRANDMOTHER, REGULAR, "It", "comes", "(come) every year.", "It comes every year.", "Viene cada año."),
                    quiz(OURSELVES, REGULAR, "We", "know", "(know) ourselves.", "We know ourselves.", "Nosotros nos conocemos."),
                    quiz(VEGETABLES, REGULAR, "I", "love", "a lot of vegetables.", "I love a lot of vegetables.",
                            "Me gustan mucho las verduras."),
                    quiz(HATS, REGULAR, "She", "sports", "(sport) hats.", "She sports hats.", "Ella luce sombreros.")));
            String[][] more = {{"I", "work", "(work) every day."}, {"You", "play", "(play) football."},
                    {"They", "live", "(live) here."}, {"We", "cook", "(cook) dinner."}, {"He", "runs", "(run) fast."},
                    {"She", "reads", "(read) books."}, {"I", "drink", "(drink) water."}, {"You", "sing", "(sing) well."},
                    {"They", "walk", "(walk) home."}, {"We", "watch", "(watch) films."}, {"He", "eats", "(eat) apples."},
                    {"She", "writes", "(write) letters."}, {"I", "open", "(open) the door."}, {"You", "close", "(close) the box."},
                    {"They", "help", "(help) people."}, {"We", "learn", "(learn) English."}};
            for (int i = 0; i < more.length; i++) {
                String id = String.format(Locale.ROOT, "67fab6d59930102295342%03d", i);
                String sentence = more[i][0] + " " + more[i][1] + " " + more[i][2].replaceAll("\\([^)]*\\) ", "");
                regular.add(quiz(id, REGULAR, more[i][0], more[i][1], more[i][2], sentence, "Oración " + i + "."));
            }
            KnowledgeEntity withBe = knowledge(WITH_BE, "Afirmativas con $be$", List.of(
                    quiz("67fab6d59930102295341e5f", WITH_BE, "She", "is", "English.", "She is English.", "Ella es inglesa."),
                    quiz("67fab6d59930102295341e60", WITH_BE, "I", "am", "a student.", "I am a student.", "Soy estudiante.")));
            KnowledgeEntity regularK = knowledge(REGULAR, "Afirmativas regulares", regular);
            return new CourseEntity("english-course", "English", List.of(), new RootNodeEntity("root", "R", NodeKind.ROOT,
                    "English", List.of(), List.of(
                            level(A1, "A1", topic(PRESENT_SIMPLE, "Present Simple", withBe, regularK)),
                            level(A2, "A2", topic(A2_TOPIC, "Past Simple", knowledge(A2_KNOWLEDGE, "Pasado regular",
                                    List.of(quiz("67fab6d59930102295343a01", A2_KNOWLEDGE, "I", "worked", "(work) yesterday.",
                                            "I worked yesterday.", "Trabajé ayer."))))),
                            level(B1, "B1", topic(B1_TOPIC, "Present Perfect", knowledge(B1_KNOWLEDGE, "Experiencias",
                                    List.of(quiz("67fab6d59930102295344b01", B1_KNOWLEDGE, "I", "have seen", "(see) it.",
                                            "I have seen it.", "Lo he visto."))))),
                            level(B2, "B2", topic(PASSIVE, "Passive", knowledge(PASSIVE_KNOWLEDGE, "Pasiva",
                                    List.of(quiz(B2_QUIZ, PASSIVE_KNOWLEDGE, "The house", "was built", "(build) in 1990.",
                                            "The house was built in 1990.", "La casa fue construida en 1990."))))))),
                    "english");
        }

        static QuizTemplateEntity quiz(String id, String knowledgeId, String head, String answer, String tail,
                String sentence, String translation) {
            FormEntity form = new FormEntity("CLOZE", 1.0, "", "", List.of(
                    new SentencePartEntity(SentencePartKind.TEXT, head, null),
                    new SentencePartEntity(SentencePartKind.CLOZE, "", List.of(answer)),
                    new SentencePartEntity(SentencePartKind.TEXT, tail, null)), null, null);
            return new QuizTemplateEntity(id, id, "CLOZE", knowledgeId, "Afirmativas regulares",
                    "Escribe la forma afirmativa en presente simple.", translation, "a1.01", "Present Simple", form, 0.0,
                    0.0, 0.0, "", "", "", "A1.01", "",
                    "Con he, she, it el verbo lleva $-s$. Con I, you, we, they va en forma base.", "",
                    List.of(sentence), null);
        }

        private static KnowledgeEntity knowledge(String id, String label, List<QuizTemplateEntity> quizzes) {
            return new KnowledgeEntity(id, id, NodeKind.KNOWLEDGE, label, id, null, false,
                    "Escribe la forma correcta.", 0, id, new ArrayList<>(quizzes), SentenceMode.FILL);
        }

        private static TopicEntity topic(String id, String label, KnowledgeEntity... knowledges) {
            return new TopicEntity(id, id, NodeKind.TOPIC, label, id, null, List.of(), List.of(), 0, id,
                    new ArrayList<>(List.of(knowledges)));
        }

        private static MilestoneEntity level(String id, String label, TopicEntity... topics) {
            return new MilestoneEntity(id, label, NodeKind.MILESTONE, label, id, "root", List.of(), 0, label,
                    new ArrayList<>(List.of(topics)));
        }

        /** The exercise as the course has it. */
        static QuizTemplateEntity before(String id) {
            return find(course(), id);
        }

        /** The exercise with the fix of the 077. */
        static QuizTemplateEntity after(String id) {
            QuizTemplateEntity before = before(id);
            return switch (id) {
                case GRANDMOTHER -> quiz(id, REGULAR, "My grandmother", "comes", "(come) every year.",
                        "My grandmother comes every year.", "Mi abuela viene cada año.");
                case OURSELVES -> quiz(id, REGULAR, "We", "know", "(know) each other.", "We know each other.", "Nos conocemos.");
                case VEGETABLES -> quiz(id, REGULAR, "I", "love", "(love) vegetables.", "I love vegetables.",
                        "Me encantan las verduras.");
                case HATS -> quiz(id, REGULAR, "She", "wears", "(wear) hats.", "She wears hats.", "Ella usa sombreros.");
                default -> before;
            };
        }

        /** «Winter ___ (come) every year.», made on «It ___ (come) every year.» as the course has it. */
        static QuizTemplateEntity winter() {
            return quiz(GRANDMOTHER, REGULAR, "Winter", "comes", "(come) every year.", "Winter comes every year.",
                    "El invierno llega cada año.");
        }

        static QuizTemplateEntity find(CourseEntity course, String id) {
            for (MilestoneEntity level : course.getRoot().getMilestones()) {
                for (TopicEntity topic : level.getTopics()) {
                    for (KnowledgeEntity knowledge : topic.getKnowledges()) {
                        for (QuizTemplateEntity quiz : knowledge.getQuizTemplates()) {
                            if (id.equals(quiz.getId())) {
                                return quiz;
                            }
                        }
                    }
                }
            }
            throw new AssertionError("no exercise " + id);
        }

        static RevisionProposal proposal(String proposalId, QuizTemplateEntity before, QuizTemplateEntity after,
                Instant createdAt, DiagnosisKind kind) {
            return new RevisionProposal(proposalId, "task-" + proposalId, PLAN, AUDIT, kind, AuditTarget.QUIZ,
                    before.getId(), new CourseElementSnapshot(AuditTarget.QUIZ, before.getId(), before, null),
                    new CourseElementSnapshot(AuditTarget.QUIZ, after.getId(), after, null), "el arreglo del 077",
                    "prueba", createdAt, null, List.of(), null);
        }

        static RevisionArtifact pending(String proposalId, QuizTemplateEntity before, QuizTemplateEntity after,
                Instant createdAt) {
            return new RevisionArtifact(proposal(proposalId, before, after, createdAt, DiagnosisKind.LEMMA_ABSENCE),
                    RevisionVerdict.PENDING_APPROVAL, null, RevisionOutcomeKind.PENDING_APPROVAL_PERSISTED, null, null,
                    null, null);
        }

        static RevisionArtifact approved(String proposalId, QuizTemplateEntity before, QuizTemplateEntity after,
                Instant createdAt) {
            return new RevisionArtifact(proposal(proposalId, before, after, createdAt, DiagnosisKind.LEMMA_ABSENCE),
                    RevisionVerdict.APPROVED, null, RevisionOutcomeKind.APPROVED_APPLIED, createdAt, null, null, null);
        }

        /** The course with the exercises written as approve writes them. */
        static CourseEntity written(CourseEntity course, QuizTemplateEntity... quizzes) {
            DefaultCourseElementLocator locator = new DefaultCourseElementLocator();
            CourseEntity current = course;
            for (QuizTemplateEntity quiz : quizzes) {
                current = locator.replace(current, new CourseElementSnapshot(AuditTarget.QUIZ, quiz.getId(), quiz, null));
            }
            return current;
        }

        /** The course as the engine reads it: the entities of the course, with their sentences. */
        static CourseMapper mapper() {
            return course -> {
                List<AuditableMilestone> levels = new ArrayList<>();
                for (MilestoneEntity level : course.getRoot().getMilestones()) {
                    List<AuditableTopic> topics = new ArrayList<>();
                    for (TopicEntity topic : level.getTopics()) {
                        List<AuditableKnowledge> knowledges = new ArrayList<>();
                        for (KnowledgeEntity knowledge : topic.getKnowledges()) {
                            List<AuditableQuiz> quizzes = new ArrayList<>();
                            for (QuizTemplateEntity quiz : knowledge.getQuizTemplates()) {
                                quizzes.add(new AuditableQuiz(List.of(), quiz.getId(), quiz.getTitle(), quiz.getCode(),
                                        quiz.getTranslation(), quiz.getSentences(), null, quiz.getInstructions(),
                                        quiz.getForm().getSentenceParts(), null, quiz.getMiniTheory(), quiz.getAnswerAudioUrl()));
                            }
                            knowledges.add(new AuditableKnowledge(quizzes, knowledge.getLabel(), knowledge.getInstructions(),
                                    true, knowledge.getId(), knowledge.getLabel(), knowledge.getCode(),
                                    knowledge.getSentenceMode(), topic.getLabel()));
                        }
                        topics.add(new AuditableTopic(knowledges, topic.getId(), topic.getLabel(), topic.getCode()));
                    }
                    levels.add(new AuditableMilestone(topics, level.getId(), level.getLabel(), level.getCode()));
                }
                return new AuditableCourse(levels);
            };
        }

        /** The two analyzers of the fixture, as a catalog builds them: a new one for every run. */
        static List<AnalyzerProvider> providers() {
            return List.of(lemmaAbsence(), coca());
        }

        static IAuditEngine engine(List<AnalyzerProvider> providers) {
            return new IAuditEngine(new IScoreAggregator(), new DefaultAnalyzerCatalog(providers),
                    new DefaultFindingCollector(), new DefaultContextNumbersCalculator());
        }

        static IAuditEngine engine() {
            return engine(providers());
        }

        /** analyze of the course with every analyzer of the fixture, in a new invocation. */
        static AuditReport analyze(CourseEntity course) {
            return engine().runAudit(mapper().map(course), new AnalyzerRunSelection(List.of(LEMMA_ABSENCE, COCA), Map.of()));
        }

        static AnalyzerProvider lemmaAbsence() {
            AnalyzerDescriptor card = new AnalyzerDescriptor(LEMMA_ABSENCE, "Palabras de otro nivel (doble de prueba)",
                    AuditTarget.QUIZ, "¿Usa palabras de su nivel?", "Las palabras de la oración",
                    List.of(new AnalyzerRuleCard("misplaced-word", "Una palabra de otro nivel", AnalysisCost.INSTANT)),
                    "Ninguna palabra de otro nivel", AnalyzerFamily.VOCABULARY, List.of(AuditTarget.QUIZ),
                    List.of(FindingResolution.PANEL), AnalysisCost.INSTANT, "Ausencias (prueba)", List.of(), null);
            return provider(card, () -> new Analyzer(card) {
                @Override
                public Void onQuiz(AuditNode node) {
                    node.getScores().put(LEMMA_ABSENCE, 1.0 - 0.1 * offLevel(node).size());
                    return null;
                }

                @Override
                public List<FindingDraft> findingsAt(AuditNode node) {
                    List<FindingDraft> drafts = new ArrayList<>();
                    for (String word : offLevel(node)) {
                        drafts.add(new FindingDraft("misplaced-word", word, FindingSeverity.MEDIUM, FindingResolution.PANEL,
                                new FindingEvidence(List.of(new EvidencePart("Oración", sentence(node)),
                                        new EvidencePart("Palabra", word),
                                        new EvidencePart("Nivel de la palabra", OFF_LEVEL.get(word)),
                                        new EvidencePart("Nivel del ejercicio", "A1")),
                                        word + " es una palabra de " + OFF_LEVEL.get(word) + " en un ejercicio de A1",
                                        List.of())));
                    }
                    return drafts;
                }
            });
        }

        /**
         * Counts the words of every topic and writes them as its COCA diagnosis when the course closes, keeping the
         * topics it saw as CocaBucketsAnalyzer did on 29/9: an analyzer used for two runs would write the second
         * count over the topics of the first report.
         */
        static AnalyzerProvider coca() {
            AnalyzerDescriptor card = new AnalyzerDescriptor(COCA, "Palabras por banda (doble de prueba)",
                    AuditTarget.TOPIC, "¿Cuántas palabras tiene cada topic?", "Las palabras de cada topic",
                    List.of(new AnalyzerRuleCard("topic-words", "Las palabras del topic", AnalysisCost.INSTANT)),
                    "Las bandas de cada topic", AnalyzerFamily.VOCABULARY, List.of(AuditTarget.TOPIC),
                    List.of(FindingResolution.RANK_ONLY), AnalysisCost.INSTANT, "Bandas (prueba)", List.of(), null);
            return provider(card, () -> new Analyzer(card) {
                private final List<AuditNode> topics = new ArrayList<>();
                private final Map<String, Integer> words = new LinkedHashMap<>();

                @Override
                public Void onTopic(AuditNode node) {
                    topics.add(node);
                    return null;
                }

                @Override
                public Void onQuiz(AuditNode node) {
                    AuditNode topic = node.ancestor(AuditTarget.TOPIC).orElseThrow();
                    words.merge(topic.getEntity().getId(), sentence(node).split("\\s+").length, Integer::sum);
                    return null;
                }

                @Override
                public Void onCourseComplete(AuditNode root) {
                    for (AuditNode topic : topics) {
                        int count = words.getOrDefault(topic.getEntity().getId(), 0);
                        ((DefaultTopicDiagnoses) topic.getDiagnoses()).setCocaBucketsDiagnosis(new CocaBucketsTopicDiagnosis(
                                count, List.of(new BucketSummary("top1k", count, 1.0))));
                        topic.getScores().put(COCA, 1.0);
                    }
                    return null;
                }
            });
        }

        private static AnalyzerProvider provider(AnalyzerDescriptor card, java.util.function.Supplier<ContentAnalyzer> fresh) {
            return new AnalyzerProvider() {
                @Override
                public String analyzerName() {
                    return card.getName();
                }

                @Override
                public AnalyzerDescriptor describe() {
                    return card;
                }

                @Override
                public ContentAnalyzer create(EvaluationRunPolicy policy) {
                    return fresh.get();
                }

                @Override
                public Optional<AnalyzerPlanBinding> planBinding() {
                    return Optional.empty();
                }

                @Override
                public Optional<SelfDescribingConfig> config() {
                    return Optional.empty();
                }
            };
        }

        /** An analyzer that does nothing but what it overrides. */
        abstract static class Analyzer implements ContentAnalyzer {
            private final AnalyzerDescriptor card;

            Analyzer(AnalyzerDescriptor card) {
                this.card = card;
            }

            @Override
            public Void onKnowledge(AuditNode node) {
                return null;
            }

            @Override
            public Void onQuiz(AuditNode node) {
                return null;
            }

            @Override
            public Void onMilestone(AuditNode node) {
                return null;
            }

            @Override
            public Void onTopic(AuditNode node) {
                return null;
            }

            @Override
            public Void onCourseComplete(AuditNode rootNode) {
                return null;
            }

            @Override
            public String getName() {
                return card.getName();
            }

            @Override
            public AuditTarget getTarget() {
                return card.getTarget();
            }

            @Override
            public String getDescription() {
                return card.getDescription();
            }

            @Override
            public List<FindingDraft> findingsAt(AuditNode node) {
                return List.of();
            }
        }

        static String sentence(AuditNode quizNode) {
            AuditableQuiz quiz = (AuditableQuiz) quizNode.getEntity();
            return quiz.getSentences().isEmpty() ? "" : quiz.getSentences().get(0);
        }

        static List<String> offLevel(AuditNode quizNode) {
            List<String> words = new ArrayList<>();
            for (String word : sentence(quizNode).toLowerCase(Locale.ROOT).split("[^a-z]+")) {
                if (OFF_LEVEL.containsKey(word) && !words.contains(word)) {
                    words.add(word);
                }
            }
            return words;
        }

        /** The node of the report with the id of its entity, or the course for "root". */
        static AuditNode node(AuditReport report, String id) {
            List<AuditNode> found = new ArrayList<>();
            walk(report.getRoot(), n -> {
                String nodeId = n.getEntity() != null ? n.getEntity().getId() : "root";
                if (id.equals(nodeId)) {
                    found.add(n);
                }
            });
            assertEquals(1, found.size(), "node " + id);
            return found.get(0);
        }

        static void walk(AuditNode node, Consumer<AuditNode> action) {
            action.accept(node);
            if (node.getChildren() != null) {
                node.getChildren().forEach(child -> walk(child, action));
            }
        }

        /** The published vocabulary score and the scores of every node, by node. */
        static Map<String, String> vocabulary(AuditReport report) {
            Map<String, String> result = new TreeMap<>();
            walk(report.getRoot(), n -> result.put(n.getTarget() + " " + (n.getEntity() != null ? n.getEntity().getId() : "root"),
                    (n.getNumbers() != null ? n.getNumbers().getVocabularyScore() : null) + " " + new TreeMap<>(n.getScores())));
            return result;
        }

        /** The identity of every finding of the report. */
        static List<String> findings(AuditReport report) {
            List<String> keys = new ArrayList<>();
            walk(report.getRoot(), n -> n.getFindings().forEach(f -> keys.add(f.getIdentity().getKey())));
            return keys;
        }

        static int cocaTokens(AuditReport report, String topicId) {
            return ((DefaultTopicDiagnoses) node(report, topicId).getDiagnoses()).getCocaBucketsDiagnosis()
                    .map(CocaBucketsTopicDiagnosis::getTotalTokens).orElse(0);
        }

        static List<Integer> cocaBands(AuditReport report, String topicId) {
            return ((DefaultTopicDiagnoses) node(report, topicId).getDiagnoses()).getCocaBucketsDiagnosis()
                    .map(d -> d.getBuckets().stream().map(BucketSummary::getCount).toList()).orElse(List.of());
        }

        static String percent(Double score) {
            return String.format(Locale.ROOT, "%.2f", score * 100).replace('.', ',');
        }

        /** The stores of an active pair over the course: the analysis, its plan and the proposals of the plan. */
        static final class Mesa {
            final ActiveAnalysisSelectionStore selection = mock(ActiveAnalysisSelectionStore.class);
            final AuditReportStore audits = mock(AuditReportStore.class);
            final RefinementPlanStore plans = mock(RefinementPlanStore.class);
            final RevisionArtifactStore artifacts = mock(RevisionArtifactStore.class);
            final CourseRepository repository = mock(CourseRepository.class);
            final List<RevisionArtifact> proposals = new ArrayList<>();
            final AuditReport active;
            private int created;

            Mesa(CourseEntity course) {
                this.active = analyze(course);
                when(selection.read()).thenReturn(Optional.of(new ActiveAnalysisSelection(AUDIT, PLAN)));
                when(audits.load(AUDIT)).thenReturn(Optional.of(active));
                when(plans.load(PLAN)).thenReturn(Optional.of(new RefinementPlan(PLAN, AUDIT,
                        Instant.parse("2026-09-30T11:54:12Z"), List.of())));
                when(artifacts.listByPlan(PLAN)).thenReturn(proposals);
                when(repository.load(COURSE)).thenReturn(course);
            }

            /** The fixes of the 077 on these exercises, pending, in this order of creation. */
            void pending(String... quizIds) {
                for (String id : quizIds) {
                    proposals.add(Curso.pending("p-" + id, before(id), after(id),
                            Instant.parse("2026-09-30T12:00:00Z").plusSeconds(++created)));
                }
            }

            void artifact(RevisionArtifact artifact) {
                proposals.add(artifact);
            }

            DefaultCourseProjector projector() {
                return projector(new DefaultProjectionMeasurer(mapper(), engine()));
            }

            DefaultCourseProjector projector(ProjectionMeasurer measurer) {
                return new DefaultCourseProjector(selection, audits, plans, artifacts, repository,
                        new DefaultPendingApplier(new DefaultCourseElementLocator()), measurer);
            }
        }
    }
}
