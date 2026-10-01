package com.learney.contentaudit.auditcli.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.auditcli.formatting.DefaultConsolidatedViewFormatter;
import com.learney.contentaudit.auditdomain.offlevelwords.DefaultTopicTeachingResolver;
import com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer;
import com.learney.contentaudit.auditinfrastructure.FileSystemBlindSampleStore;
import com.learney.contentaudit.auditinfrastructure.FileSystemGoalDeclarationSource;
import com.learney.contentaudit.auditinfrastructure.FileSystemSealedGoalsStore;
import com.learney.contentaudit.revisiondomain.BlindSampleStore;
import com.learney.contentaudit.revisiondomain.SealedGoalsStore;
import com.learney.contentaudit.revisiondomain.blindsample.BlindSampleService;
import com.learney.contentaudit.revisiondomain.blindsampleengine.DefaultBlindSampleService;
import com.learney.contentaudit.revisiondomain.blindsampleengine.DefaultSampleOutcomeSource;
import com.learney.contentaudit.revisiondomain.blindsampleengine.DefaultSampleStatistics;
import com.learney.contentaudit.revisiondomain.comparison.BatchComparator;
import com.learney.contentaudit.revisiondomain.comparison.GoalBook;
import com.learney.contentaudit.revisiondomain.comparisonengine.DefaultBatchComparator;
import com.learney.contentaudit.revisiondomain.comparisonengine.DefaultGoalBook;
import com.learney.contentaudit.revisiondomain.comparisonengine.LeaveOneOutChangeAttributor;
import com.learney.contentaudit.auditdomain.auditnodeindex.DefaultAuditNodeIndexFactory;
import com.learney.contentaudit.auditdomain.correctionengine.DefaultAnalyzerCorrectionCatalog;
import com.learney.contentaudit.auditdomain.correctionengine.LemmaAbsenceCorrection;
import com.learney.contentaudit.auditdomain.correctionengine.QuizInstructionCorrection;
import com.learney.contentaudit.refinerdomain.CorrectionContextResolver;
import com.learney.contentaudit.refinerdomain.DefaultRefinerEngine;
import com.learney.contentaudit.refinerdomain.DispatchingCorrectionContextResolver;
import com.learney.contentaudit.refinerdomain.KnowledgeTitleContextResolver;
import com.learney.contentaudit.refinerdomain.LemmaAbsenceContextResolver;
import com.learney.contentaudit.refinerdomain.QuizInstructionContextResolver;
import com.learney.contentaudit.refinerdomain.RefinerEngine;
import com.learney.contentaudit.refinerdomain.SentenceLengthContextResolver;
import com.learney.contentaudit.revisiondomain.projection.CourseProjection;
import org.junit.jupiter.api.io.TempDir;
import com.learney.contentaudit.auditapplication.CourseToAuditableMapper;
import com.learney.contentaudit.auditapplication.DefaultLemmaCountConfigLoader;
import com.learney.contentaudit.auditdomain.ActiveAnalysisSelection;
import com.learney.contentaudit.auditdomain.ActiveAnalysisSelectionStore;
import com.learney.contentaudit.auditdomain.AnalyzerCatalog;
import com.learney.contentaudit.auditdomain.AnalyzerDescriptor;
import com.learney.contentaudit.auditdomain.AnalyzerProvider;
import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditReportStore;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.Baseline;
import com.learney.contentaudit.auditdomain.BaselineStore;
import com.learney.contentaudit.auditdomain.IAuditEngine;
import com.learney.contentaudit.auditdomain.NlpToken;
import com.learney.contentaudit.auditdomain.NlpTokenizer;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerMeasure;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.findingengine.DefaultAnalyzerCatalog;
import com.learney.contentaudit.auditdomain.findingengine.DefaultContextNumbersCalculator;
import com.learney.contentaudit.auditdomain.findingengine.DefaultFindingCollector;
import com.learney.contentaudit.auditdomain.labs.LemmaAbsenceScoreAggregator;
import com.learney.contentaudit.auditdomain.lemmacount.LemmaCountAnalyzerProvider;
import com.learney.contentaudit.auditinfrastructure.FileSystemActiveAnalysisSelectionStore;
import com.learney.contentaudit.auditinfrastructure.FileSystemAuditReportStore;
import com.learney.contentaudit.auditinfrastructure.FileSystemBaselineStore;
import com.learney.contentaudit.auditinfrastructure.FileSystemProjectionStore;
import com.learney.contentaudit.auditinfrastructure.FileSystemRefinementPlanStore;
import com.learney.contentaudit.auditinfrastructure.FileSystemRevisionArtifactStore;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.CourseRepository;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.MilestoneEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.coursedomain.TopicEntity;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.courseinfrastructure.CourseValidatorImpl;
import com.learney.contentaudit.courseinfrastructure.FileSystemCourseRepository;
import com.learney.contentaudit.refinerdomain.DiagnosisKind;
import com.learney.contentaudit.refinerdomain.RefinementPlan;
import com.learney.contentaudit.refinerdomain.RefinementPlanStore;
import com.learney.contentaudit.revisiondomain.ConsolidatedViewBuilderConfig;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.ProjectionStore;
import com.learney.contentaudit.revisiondomain.RevisionArtifact;
import com.learney.contentaudit.revisiondomain.RevisionArtifactStore;
import com.learney.contentaudit.revisiondomain.RevisionOutcomeKind;
import com.learney.contentaudit.revisiondomain.RevisionProposal;
import com.learney.contentaudit.revisiondomain.RevisionVerdict;
import com.learney.contentaudit.revisiondomain.consolidatedview.ConsolidatedViewBuilder;
import com.learney.contentaudit.revisiondomain.engine.DefaultConsolidatedViewBuilderFactory;
import com.learney.contentaudit.revisiondomain.engine.DefaultCourseProjectorFactory;
import com.learney.contentaudit.revisiondomain.fielddiff.DefaultNodeFieldDifferFactory;
import com.learney.contentaudit.revisiondomain.projection.CourseProjector;
import com.learney.contentaudit.revisiondomain.projection.CourseProjectorConfig;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.processing.Generated;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@Generated(
        value = "com.sentinel.SentinelEngine",
        comments = "Generated by Sentinel — journey test stubs"
)
@Tag("F-PROYU")
@Tag("F-PROYU-J001")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FProyuJ001JourneyTest {
    @Test
    @Order(1)
    @Tag("path-1")
    @DisplayName("path-1: El operador pide la vista consolidada... → Cada foto trae los mismos números, ha... [Cada pendiente coincide con su nodo en el curso con lo aprobado] → El operador guarda la proyección y pi... → get audits lista los mismos análisis,... → success")
    public void path1_cadaPendienteCoincideConSuNodoEnElCursoConLoAprobado_success() {
        // The 29/9 base as the active pair and as the baseline; its plan with the four pending proposals of
        // «Afirmativas regulares», each one matching its node.
        Proyeccion299.Workdir work = new Proyeccion299.Workdir(tmp.resolve("trabajo"));
        String audit = work.analyze("2026-09-30T11-54-02", Proyeccion299.BANDS_OF_29_9);
        String plan = work.plan("2026-09-30T11-54-12", audit);
        CourseEntity course = Proyeccion299.repository().load(Proyeccion299.COURSE);
        List<QuizTemplateEntity> written = new ArrayList<>();
        for (String id : List.of(Proyeccion299.GRANDMOTHER, Proyeccion299.OURSELVES, Proyeccion299.VEGETABLES,
                Proyeccion299.HATS)) {
            QuizTemplateEntity before = Proyeccion299.quiz(course, id);
            QuizTemplateEntity after = Proyeccion299.fixed(before);
            work.seed(Proyeccion299.pending("p-" + id, plan, audit, before, after, Proyeccion299.createdAt(id)));
            written.add(after);
        }
        work.activate(audit, plan);
        work.fixBaseline(audit);
        Map<String, String> historyBefore = work.hashes(".content-audit/audits");
        Map<String, String> plansBefore = work.hashes(".content-audit/plans");

        // pedir: the consolidated view of the active pair, whose plan has the four pending proposals.
        AnalyzeCmdTest.Base299.Captured asked = Proyeccion299.capture(() -> new DefaultGetConsolidatedCommand(
                work.consolidated(Proyeccion299.BANDS_OF_29_9), new DefaultConsolidatedViewFormatter())
                .getConsolidated(Proyeccion299.COURSE.toString(), "json"));
        assertEquals(0, asked.exit(), asked.err());
        assertTrue(asked.out().contains("\"consolidatedAvailability\" : \"AVAILABLE\""), asked.out());
        assertFalse(asked.out().contains("NOT_APPLICABLE"), "R002: the four enter: " + asked.out());

        // proyectar: each photo brings what analyze gives on the course with those proposals written, with the
        // analyzers of the baseline (F-PROYU-R001, R003).
        CourseProjection projection = work.projector(Proyeccion299.BANDS_OF_29_9).project(Proyeccion299.COURSE, null);
        AuditReport written299 = Proyeccion299.analyze(Proyeccion299.courseWith(tmp.resolve("copia"), written),
                Proyeccion299.BANDS_OF_29_9);
        assertEquals(Proyeccion299.vocabulary(written299), Proyeccion299.vocabulary(projection.getProjected()),
                "R001: every node, the course included, as analyze on the copy with the four written");
        assertEquals("73,92", Proyeccion299.percent(projection.getProjected().getRoot().getNumbers()
                .getVocabularyScore()), "R001: the course at 73,92 %");
        assertEquals(List.of("96,54", "94,16", "79,45", "63,51"), Proyeccion299.levels(projection.getProjected()),
                "R001: the levels, not 92,38 in A1");
        assertEquals(0.9, Proyeccion299.node(projection.getProjected(), Proyeccion299.GRANDMOTHER).getScores()
                .get("lemma-absence"), 1e-9, "R001: «My grandmother ___ (come) every year.» from 1,0 to 0,9");
        assertEquals(List.of(), projection.getNonApplicablePendings(), "R002: the four enter");
        assertEquals(4, projection.getChanges().stream().filter(c -> c.isPending()).count(), "R002: four applied");
        AuditReport baseline = work.audits.load(audit).orElseThrow();
        assertEquals(Proyeccion299.measured(baseline), Proyeccion299.measured(projection.getApproved()),
                "R003: the photo with what is approved measures with the analyzers of the baseline");
        assertEquals(Proyeccion299.measured(baseline), Proyeccion299.measured(projection.getProjected()),
                "R003: and so does the photo with the pending proposals");

        // guardar: the operator saves the projection and asks an ephemeral plan with its id (F-PROYU-R005).
        AnalyzeCmdTest.Base299.Captured saved = Proyeccion299.capture(() -> new DefaultSaveProjectionCommand(
                work.projector(Proyeccion299.BANDS_OF_29_9), work.projections)
                .saveProjection(Proyeccion299.COURSE.toString(), null, Map.of(), "text"));
        assertEquals(0, saved.exit(), saved.err());
        String projectionId = projectionId(saved.out());
        AnalyzeCmdTest.Base299.Captured planned = AnalyzeCmdTest.Base299.run(planCmd(work), "--audit", projectionId,
                "--storage", "EPHEMERAL", "--with-correction-context", "-f", "json");
        assertEquals(0, planned.exit(), planned.err());
        assertTrue(planned.out().contains(projectionId), "R005: the plan comes from the projection: " + planned.out());
        assertEquals(plansBefore, work.hashes(".content-audit/plans"), "R005: the ephemeral plan writes no plan");

        // historial: get audits lists the same analyses; the latest, the active pair and the baseline stay; prune
        // audits does not count the projection (F-PROYU-R004, R006).
        assertEquals(historyBefore, work.hashes(".content-audit/audits"), "R004: no analysis was written or changed");
        assertEquals(List.of(audit), work.audits.list().stream().map(a -> a.getId()).toList(), "R004: get audits");
        assertEquals(Proyeccion299.vocabulary(baseline), Proyeccion299.vocabulary(work.audits.loadLatest().orElseThrow()),
                "R004: the latest is still the base");
        assertEquals(audit, work.active.read().orElseThrow().getAuditId(), "R006: the active analysis stays");
        assertEquals(audit, work.baseline.read().orElseThrow().getAuditId(), "R006: the baseline stays");
        PruneCmd prune = new PruneCmd(work.audits, work.plans, work.active, work.baseline);
        prune.setBaseDir(work.dir);
        AnalyzeCmdTest.Base299.Captured pruned = AnalyzeCmdTest.Base299.run(prune, "audits", "--keep", "1");
        assertEquals(0, pruned.exit(), pruned.err());
        assertEquals(historyBefore, work.hashes(".content-audit/audits"), "R004: prune audits removed nothing");
        assertTrue(work.projections.load(projectionId).isPresent(), "R004: nor the projection, which it does not count");
    }

    @Test
    @Order(2)
    @Tag("path-2")
    @DisplayName("path-2: El operador pide la vista consolidada... → La pendiente queda declarada no aplic... [Una pendiente no coincide con su nodo porque otra anterior ya lo cambió] → Cada foto trae los mismos números, ha... → El operador guarda la proyección y pi... → get audits lista los mismos análisis,... → success")
    public void path2_unaPendienteNoCoincideConSuNodoPorqueOtraAnteriorYaLoCambi_success() {
        // The four pending proposals, and a fifth on «It ___ (come) every year.», newer, made on the same exercise
        // before the first changed it: «Winter ___ (come) every year.».
        Proyeccion299.Workdir work = new Proyeccion299.Workdir(tmp.resolve("trabajo"));
        String audit = work.analyze("2026-09-30T11-54-02", Proyeccion299.BANDS_OF_29_9);
        String plan = work.plan("2026-09-30T11-54-12", audit);
        CourseEntity course = Proyeccion299.repository().load(Proyeccion299.COURSE);
        List<QuizTemplateEntity> written = new ArrayList<>();
        for (String id : List.of(Proyeccion299.GRANDMOTHER, Proyeccion299.OURSELVES, Proyeccion299.VEGETABLES,
                Proyeccion299.HATS)) {
            QuizTemplateEntity before = Proyeccion299.quiz(course, id);
            QuizTemplateEntity after = Proyeccion299.fixed(before);
            work.seed(Proyeccion299.pending("p-" + id, plan, audit, before, after, Proyeccion299.createdAt(id)));
            written.add(after);
        }
        QuizTemplateEntity original = Proyeccion299.quiz(course, Proyeccion299.GRANDMOTHER);
        QuizTemplateEntity winter = Proyeccion299.rewritten(original, "Winter", "comes", "(come) every year.",
                "Winter comes every year.", "El invierno llega cada año.");
        work.seed(Proyeccion299.pending("p-winter", plan, audit, original, winter,
                Instant.parse("2026-09-30T12:00:05Z")));
        work.activate(audit, plan);
        work.fixBaseline(audit);
        Map<String, String> historyBefore = work.hashes(".content-audit/audits");

        // pedir
        AnalyzeCmdTest.Base299.Captured asked = Proyeccion299.capture(() -> new DefaultGetConsolidatedCommand(
                work.consolidated(Proyeccion299.BANDS_OF_29_9), new DefaultConsolidatedViewFormatter())
                .getConsolidated(Proyeccion299.COURSE.toString(), "json"));
        assertEquals(0, asked.exit(), asked.err());

        // declarar: the newer one does not match the node the older one already changed; it is declared not
        // applicable with its cause, in the view and in the projection, and enters no photo (F-PROYU-R002).
        assertTrue(asked.out().contains("p-winter") && asked.out().contains("ELEMENT_BEFORE_MISMATCH"),
                "R002: the view declares it with its cause: " + asked.out());
        CourseProjection projection = work.projector(Proyeccion299.BANDS_OF_29_9).project(Proyeccion299.COURSE, null);
        assertEquals(List.of("p-winter"), projection.getNonApplicablePendings().stream()
                .map(n -> n.getProposalId()).toList(), "R002: only the newer one is not applicable");
        assertEquals("ELEMENT_BEFORE_MISMATCH", projection.getNonApplicablePendings().get(0).getReason().name(),
                "R002: with its cause");
        assertFalse(projection.getChanges().stream().anyMatch(c -> "p-winter".equals(c.getProposalId())),
                "R002: it is not among the applied");

        // proyectar: the numbers are those of the course with the four written, and never with «Winter».
        AuditReport written299 = Proyeccion299.analyze(Proyeccion299.courseWith(tmp.resolve("copia"), written),
                Proyeccion299.BANDS_OF_29_9);
        assertEquals(Proyeccion299.vocabulary(written299), Proyeccion299.vocabulary(projection.getProjected()),
                "R001: every node, as analyze on the copy with the four written and not the fifth");
        assertEquals(0.9, Proyeccion299.node(projection.getProjected(), Proyeccion299.GRANDMOTHER).getScores()
                .get("lemma-absence"), 1e-9, "R002: the exercise is «My grandmother…», the older one");

        // guardar: the saved projection declares the same not applicable one.
        AnalyzeCmdTest.Base299.Captured saved = Proyeccion299.capture(() -> new DefaultSaveProjectionCommand(
                work.projector(Proyeccion299.BANDS_OF_29_9), work.projections)
                .saveProjection(Proyeccion299.COURSE.toString(), null, Map.of(), "text"));
        assertEquals(0, saved.exit(), saved.err());
        assertTrue(saved.out().contains("p-winter") && saved.out().contains("ELEMENT_BEFORE_MISMATCH"),
                "R002: save-projection prints it with its cause: " + saved.out());
        String projectionId = projectionId(saved.out());
        AnalyzeCmdTest.Base299.Captured planned = AnalyzeCmdTest.Base299.run(planCmd(work), "--audit", projectionId,
                "--storage", "EPHEMERAL", "--with-correction-context", "-f", "json");
        assertEquals(0, planned.exit(), planned.err());

        // historial
        assertEquals(historyBefore, work.hashes(".content-audit/audits"), "R004: no analysis was written or changed");
        assertEquals(List.of(audit), work.audits.list().stream().map(a -> a.getId()).toList(), "R004: get audits");
        assertEquals(audit, work.active.read().orElseThrow().getAuditId(), "R006: the active analysis stays");
        assertEquals(audit, work.baseline.read().orElseThrow().getAuditId(), "R006: the baseline stays");
    }

    @TempDir
    Path tmp;

    /** The id save-projection printed. */
    private static String projectionId(String out) {
        java.util.regex.Matcher id = java.util.regex.Pattern.compile("proyeccion-[^\\s,;]+").matcher(out);
        assertTrue(id.find(), "save-projection printed the id: " + out);
        return id.group();
    }

    /** plan as Main builds it, over the stores of the working directory. */
    private static PlanCmd planCmd(Proyeccion299.Workdir work) {
        AnalyzerCatalog catalog = Proyeccion299.catalog(Proyeccion299.BANDS_OF_29_9);
        RefinerEngine refiner = new DefaultRefinerEngine(catalog, new DefaultAnalyzerCorrectionCatalog(
                List.of(new QuizInstructionCorrection(), new LemmaAbsenceCorrection()), catalog));
        CorrectionContextResolver contexts = new DispatchingCorrectionContextResolver(
                new SentenceLengthContextResolver(), new LemmaAbsenceContextResolver(),
                new KnowledgeTitleContextResolver(), new QuizInstructionContextResolver());
        EphemeralPlanRenderer renderer = new DefaultEphemeralPlanRenderer(new DefaultAuditNodeIndexFactory(), contexts,
                new DefaultCorrectionContextJsonMapper());
        return new PlanCmd(work.audits, refiner, work.plans, renderer);
    }

    // -----------------------------------------------------------------------
    // Package F over the 29/9 base, shared by the journeys of PROYU, METAS, CIEGA and CERO
    // -----------------------------------------------------------------------

    /**
     * Package F over the 29/9 base, wired as Main wires it: the course of the repository (db/english-course,
     * identical to the photo of 29/9), the seven classic analyzers as the analysis 2026-09-30T11-54-02 ran them
     * (lemma-count at «4 para todas», or at today's 8 · 6 · 4 · 3), the stores of a working directory, the
     * projector of its factory and the commands. Only the boundaries are faked: spaCy, by the tokens it gave each
     * sentence of the 29/9 analysis ({@link AnalyzeCmdTest.Base299}) plus the four sentences the proposals of
     * «Afirmativas regulares» write, as spaCy 3.8.13 (en_core_web_sm) with the COCA list gave them on 1/10.
     */
    static final class Proyeccion299 {
        static final Path COURSE = AnalyzeCmdTest.Base299.COURSE;
        /** The four levels in the order of the course: A1, A2, B1 and B2. */
        static final List<String> LEVELS = AnalyzeCmdTest.Base299.LEVELS;
        /** «It ___ (come) every year.» becomes «My grandmother ___ (come) every year.»: grandmother is A2. */
        static final String GRANDMOTHER = "67fab6d59930102295341ec4";
        /** «We ___ (know) ourselves.» becomes «We ___ (know) each other.» */
        static final String OURSELVES = "67fab6d59930102295341ecb";
        /** «I ___ a lot of vegetables.» becomes «I ___ (love) vegetables.», «Me encantan las verduras.» */
        static final String VEGETABLES = "67fab6d59930102295341ecd";
        /** «She ___ (sport) hats.» becomes «She ___ (wear) hats.» */
        static final String HATS = "67fab6d59930102295341ed6";
        /** The knowledge of the four: «Afirmativas regulares» (A1 · Present Simple). */
        static final String REGULAR_AFFIRMATIVES = "6814dafa7d73e7209a13d389";
        /** An A1 sentence of A1 words, too long for A1: only sentence-length marks it, and length never returns. */
        static final String LONG_VEGETABLES =
                "I love vegetables and fruit, and I eat them with my mother and my father every day.";
        /** «4 para todas», the lemma-count of the analysis 2026-09-30T11-54-02. */
        static final String BANDS_OF_29_9 = "4";
        /** Today's lemma-count, 8 · 6 · 4 · 3. */
        static final String TODAYS_BANDS = null;

        private static final ObjectMapper JSON = new ObjectMapper();

        private static final Map<String, Fix> FIXES = Map.of(
                GRANDMOTHER, new Fix("My grandmother", "comes", "(come) every year.", "My grandmother comes every year.",
                        "Mi abuela viene cada año.", Instant.parse("2026-09-30T12:00:01Z")),
                OURSELVES, new Fix("We", "know", "(know) each other.", "We know each other.", "Nos conocemos.",
                        Instant.parse("2026-09-30T12:00:02Z")),
                VEGETABLES, new Fix("I", "love", "(love) vegetables.", "I love vegetables.", "Me encantan las verduras.",
                        Instant.parse("2026-09-30T12:00:03Z")),
                HATS, new Fix("She", "wears", "(wear) hats.", "She wears hats.", "Ella usa sombreros.",
                        Instant.parse("2026-09-30T12:00:04Z")));

        /** What spaCy gives the four new sentences: text, lemma, POS, COCA rank, stop word, punctuation. */
        private static final Map<String, List<NlpToken>> NEW_SENTENCES = Map.of(
                "My grandmother comes every year.", List.of(token("My", "my", "PRON", 36, true),
                        token("grandmother", "grandmother", "NOUN", 2822, false), token("comes", "come", "VERB", 61, false),
                        token("every", "every", "DET", 165, true), token("year", "year", "NOUN", 64, false), period()),
                "We know each other.", List.of(token("We", "we", "PRON", 21, true),
                        token("know", "know", "VERB", 39, false), token("each", "each", "DET", 234, true),
                        token("other", "other", "ADJ", 74, true), period()),
                "I love vegetables.", List.of(token("I", "I", "PRON", 8, true), token("love", "love", "VERB", 233, false),
                        token("vegetables", "vegetable", "NOUN", 2422, false), period()),
                "She wears hats.", List.of(token("She", "she", "PRON", 35, true), token("wears", "wear", "VERB", 542, false),
                        token("hats", "hat", "NOUN", 2028, false), period()),
                "Winter comes every year.", List.of(token("Winter", "winter", "NOUN", 1540, false),
                        token("comes", "come", "VERB", 61, false), token("every", "every", "DET", 165, true),
                        token("year", "year", "NOUN", 64, false), period()),
                LONG_VEGETABLES, List.of(token("I", "I", "PRON", 8, true), token("love", "love", "VERB", 233, false),
                        token("vegetables", "vegetable", "NOUN", 2422, false), token("and", "and", "CCONJ", 3, true),
                        token("fruit", "fruit", "NOUN", 1947, false), comma(), token("and", "and", "CCONJ", 3, true),
                        token("I", "I", "PRON", 8, true), token("eat", "eat", "VERB", 476, false),
                        token("them", "they", "PRON", 66, true), token("with", "with", "ADP", 17, true),
                        token("my", "my", "PRON", 36, true), token("mother", "mother", "NOUN", 279, false),
                        token("and", "and", "CCONJ", 3, true), token("my", "my", "PRON", 36, true),
                        token("father", "father", "NOUN", 317, false), token("every", "every", "DET", 165, true),
                        token("day", "day", "NOUN", 96, false), period()));

        private Proyeccion299() {
        }

        private static NlpToken token(String text, String lemma, String pos, int rank, boolean stop) {
            return new NlpToken(text, lemma, pos, rank, stop, false);
        }

        private static NlpToken period() {
            return new NlpToken(".", ".", "PUNCT", null, false, true);
        }

        private static NlpToken comma() {
            return new NlpToken(",", ",", "PUNCT", null, false, true);
        }

        /** The recorded tokens of the 29/9 course plus the four sentences the proposals write. */
        static NlpTokenizer tokenizer() {
            NlpTokenizer recorded = AnalyzeCmdTest.Base299.tokenizer();
            return new NlpTokenizer() {
                @Override
                public Map<String, List<NlpToken>> analyzeTokensBatch(List<String> sentences) {
                    List<String> known = new ArrayList<>();
                    Map<String, List<NlpToken>> result = new HashMap<>();
                    for (String sentence : sentences) {
                        if (NEW_SENTENCES.containsKey(sentence)) {
                            result.put(sentence, NEW_SENTENCES.get(sentence));
                        } else {
                            known.add(sentence);
                        }
                    }
                    result.putAll(recorded.analyzeTokensBatch(known));
                    return result;
                }

                @Override
                public List<String> tokenize(String text) {
                    return recorded.tokenize(text);
                }

                @Override
                public int countTokens(String text) {
                    return recorded.countTokens(text);
                }

                @Override
                public List<NlpToken> analyzeTokens(String text) {
                    return recorded.analyzeTokens(text);
                }
            };
        }

        static CourseToAuditableMapper mapper() {
            return new CourseToAuditableMapper(tokenizer(), DefaultQuizSentenceConverter.create());
        }

        static CourseRepository repository() {
            return new FileSystemCourseRepository(new CourseValidatorImpl());
        }

        /** The seven classic providers, as Main builds them, with lemma-count at the threshold given. */
        static AnalyzerCatalog catalog(String lemmaCount) {
            List<AnalyzerProvider> providers = new ArrayList<>(AnalyzeCmdTest.Base299.classicProviders().subList(0, 6));
            providers.add(new LemmaCountAnalyzerProvider(AnalyzeCmdTest.Base299.evp(),
                    new DefaultLemmaCountConfigLoader().load(lemmaCount)));
            return new DefaultAnalyzerCatalog(providers);
        }

        static IAuditEngine engine(AnalyzerCatalog catalog) {
            return new IAuditEngine(new LemmaAbsenceScoreAggregator(), catalog, new DefaultFindingCollector(),
                    new DefaultContextNumbersCalculator());
        }

        /** analyze of a course on disk with every analyzer of the catalog, by a fresh catalog and engine. */
        static AuditReport analyze(Path course, String lemmaCount) {
            AnalyzerCatalog catalog = catalog(lemmaCount);
            List<String> names = catalog.list().stream().map(AnalyzerDescriptor::getName).toList();
            return engine(catalog).runAudit(mapper().map(repository().load(course)),
                    new AnalyzerRunSelection(names, Map.of()));
        }

        /** The quiz as the course on disk has it. */
        static QuizTemplateEntity quiz(CourseEntity course, String id) {
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
            throw new AssertionError("no quiz " + id + " in the course");
        }

        /** The quiz with the fix of the 077: its sentence parts, its sentence and its translation. */
        static QuizTemplateEntity fixed(QuizTemplateEntity before) {
            Fix fix = FIXES.get(before.getId());
            return rewritten(before, fix.before(), fix.answer(), fix.after(), fix.sentence(), fix.translation());
        }

        /** The quiz rewritten as «head ___ tail», the gap accepting the answer, with its sentence and translation. */
        static QuizTemplateEntity rewritten(QuizTemplateEntity before, String head, String answer, String tail,
                String sentence, String translation) {
            QuizTemplateEntity after = JSON.convertValue(before, QuizTemplateEntity.class);
            after.getForm().setSentenceParts(List.of(
                    new SentencePartEntity(SentencePartKind.TEXT, head, null),
                    new SentencePartEntity(SentencePartKind.CLOZE, "", List.of(answer)),
                    new SentencePartEntity(SentencePartKind.TEXT, tail, null)));
            after.setSentences(List.of(sentence));
            after.setTranslation(translation);
            return after;
        }

        static Instant createdAt(String quizId) {
            return FIXES.get(quizId).createdAt();
        }

        /** A pending proposal of the plan, as revise leaves it, from the quiz before to the quiz after. */
        static RevisionArtifact pending(String proposalId, String planId, String auditId, QuizTemplateEntity before,
                QuizTemplateEntity after, Instant createdAt) {
            RevisionProposal proposal = new RevisionProposal(proposalId, "task-" + proposalId, planId, auditId,
                    DiagnosisKind.LEMMA_ABSENCE, AuditTarget.QUIZ, before.getId(),
                    new CourseElementSnapshot(AuditTarget.QUIZ, before.getId(), before, null),
                    new CourseElementSnapshot(AuditTarget.QUIZ, after.getId(), after, null),
                    "el arreglo del 077", "prueba", createdAt, null, List.of(), null);
            return new RevisionArtifact(proposal, RevisionVerdict.PENDING_APPROVAL, null,
                    RevisionOutcomeKind.PENDING_APPROVAL_PERSISTED, null, null, null, null);
        }

        /** A copy of the course with the quizzes replaced, written as approve writes it. */
        static Path courseWith(Path dir, List<QuizTemplateEntity> replacements) {
            CourseEntity course = repository().load(COURSE);
            Map<String, QuizTemplateEntity> byId = new LinkedHashMap<>();
            replacements.forEach(q -> byId.put(q.getId(), q));
            for (MilestoneEntity level : course.getRoot().getMilestones()) {
                for (TopicEntity topic : level.getTopics()) {
                    for (KnowledgeEntity knowledge : topic.getKnowledges()) {
                        List<QuizTemplateEntity> quizzes = new ArrayList<>();
                        for (QuizTemplateEntity quiz : knowledge.getQuizTemplates()) {
                            quizzes.add(byId.getOrDefault(quiz.getId(), quiz));
                        }
                        knowledge.setQuizTemplates(quizzes);
                    }
                }
            }
            repository().save(course, dir);
            return dir;
        }

        /** The node of the report whose entity has the id, or the course for "root". */
        static AuditNode node(AuditReport report, String id) {
            return "root".equals(id) ? report.getRoot() : AnalyzeCmdTest.Base299.node(report, id);
        }

        /** The vocabulary score of every level, in percent with two decimals and a decimal comma. */
        static List<String> levels(AuditReport report) {
            List<String> result = new ArrayList<>();
            for (String level : LEVELS) {
                result.add(percent(node(report, level).getNumbers().getVocabularyScore()));
            }
            return result;
        }

        static String percent(Double score) {
            return String.format(java.util.Locale.ROOT, "%.2f", score * 100).replace('.', ',');
        }

        /** The vocabulary score of every node, by node key, as the report published it. */
        static Map<String, Double> vocabulary(AuditReport report) {
            Map<String, Double> result = new LinkedHashMap<>();
            AnalyzeCmdTest.Base299.walk(report.getRoot(), n -> result.put(AnalyzeCmdTest.Base299.key(n),
                    n.getNumbers() != null ? n.getNumbers().getVocabularyScore() : null));
            return result;
        }

        static List<String> measured(AuditReport report) {
            return report.getRoot().getMeasures().stream().map(AnalyzerMeasure::getAnalyzer).toList();
        }

        /** What a command printed and returned, called directly. */
        static AnalyzeCmdTest.Base299.Captured capture(Supplier<Integer> call) {
            PrintStream out = System.out;
            PrintStream err = System.err;
            ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
            ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outBytes, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(errBytes, true, StandardCharsets.UTF_8));
            try {
                int exit = call.get();
                return new AnalyzeCmdTest.Base299.Captured(exit, outBytes.toString(StandardCharsets.UTF_8),
                        errBytes.toString(StandardCharsets.UTF_8));
            } finally {
                System.setOut(out);
                System.setErr(err);
            }
        }

        /** A working directory of content-audit: its stores, as Main builds them over it. */
        static final class Workdir {
            final Path dir;
            final AuditReportStore audits;
            final RefinementPlanStore plans;
            final RevisionArtifactStore artifacts;
            final ActiveAnalysisSelectionStore active;
            final BaselineStore baseline;
            final ProjectionStore projections;
            final SealedGoalsStore sealedGoals;
            final BlindSampleStore samples;
            /** The declaration of goals, as Main reads it from analysis/metas.json. */
            final Path goalsFile;

            Workdir(Path dir) {
                this.dir = dir;
                this.audits = new FileSystemAuditReportStore(dir, null);
                this.plans = new FileSystemRefinementPlanStore(dir);
                this.artifacts = new FileSystemRevisionArtifactStore(dir);
                this.active = new FileSystemActiveAnalysisSelectionStore(dir);
                this.baseline = new FileSystemBaselineStore(dir);
                this.projections = new FileSystemProjectionStore(dir);
                this.sealedGoals = new FileSystemSealedGoalsStore(dir);
                this.samples = new FileSystemBlindSampleStore(dir);
                this.goalsFile = dir.resolve("analysis").resolve("metas.json");
            }

            /** Writes the declaration of goals the working directory reads. */
            void declareGoals(String json) {
                try {
                    Files.createDirectories(goalsFile.getParent());
                    Files.writeString(goalsFile, json, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            GoalBook goalBook(String lemmaCount) {
                return new DefaultGoalBook(new FileSystemGoalDeclarationSource(goalsFile), sealedGoals,
                        catalog(lemmaCount));
            }

            /** The comparison as Main wires it, measuring with the threshold given. */
            BatchComparator comparator(String lemmaCount) {
                AnalyzerCatalog catalog = catalog(lemmaCount);
                IAuditEngine engine = engine(catalog);
                CourseProjector projector = new DefaultCourseProjectorFactory().create(new CourseProjectorConfig(active,
                        audits, plans, artifacts, repository(), mapper(), engine));
                return new DefaultBatchComparator(projector, baseline, audits,
                        new DefaultGoalBook(new FileSystemGoalDeclarationSource(goalsFile), sealedGoals, catalog),
                        catalog, engine, new LeaveOneOutChangeAttributor(engine),
                        new DefaultSampleOutcomeSource(samples, new DefaultSampleStatistics()),
                        new DefaultTopicTeachingResolver());
            }

            /** The blind sample as Main wires it, over the same comparison. */
            BlindSampleService blindSample(String lemmaCount) {
                return new DefaultBlindSampleService(projector(lemmaCount), comparator(lemmaCount),
                        goalBook(lemmaCount), baseline,
                        new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create()), samples,
                        new DefaultSampleStatistics());
            }

            /** Saves the analysis of the course with the threshold given, under the id given. */
            String analyze(String id, String lemmaCount) {
                return save(id, Proyeccion299.analyze(COURSE, lemmaCount));
            }

            /** Saves a report under the id given: the store names it by the second it was saved. */
            String save(String id, AuditReport report) {
                String saved = audits.save(report);
                try {
                    for (String sub : List.of(".content-audit/audits", ".content-audit/audit-digests")) {
                        Path from = dir.resolve(sub).resolve("audit-" + saved + ".json");
                        if (Files.exists(from)) {
                            Files.move(from, dir.resolve(sub).resolve("audit-" + id + ".json"));
                        }
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return id;
            }

            /** A plan of the analysis, as plan leaves it; the proposals are its artifacts. */
            String plan(String planId, String auditId) {
                plans.save(new RefinementPlan(planId, auditId, Instant.parse("2026-09-30T11:54:12Z"), List.of()));
                return planId;
            }

            void seed(RevisionArtifact... pending) {
                for (RevisionArtifact artifact : pending) {
                    artifacts.save(artifact);
                }
            }

            void activate(String auditId, String planId) {
                active.write(new ActiveAnalysisSelection(auditId, planId));
            }

            void fixBaseline(String auditId) {
                baseline.write(new Baseline(auditId, Instant.parse("2026-10-01T03:00:00Z")));
            }

            CourseProjector projector(String lemmaCount) {
                return new DefaultCourseProjectorFactory().create(new CourseProjectorConfig(active, audits, plans,
                        artifacts, repository(), mapper(), engine(catalog(lemmaCount))));
            }

            ConsolidatedViewBuilder consolidated(String lemmaCount) {
                return new DefaultConsolidatedViewBuilderFactory().create(new ConsolidatedViewBuilderConfig(active,
                        audits, plans, artifacts, repository(), null, mapper(), engine(catalog(lemmaCount)),
                        new DefaultNodeFieldDifferFactory().create()));
            }

            Map<String, String> hashes(String sub) {
                return AnalyzeCmdTest.Base299.hashes(dir.resolve(sub));
            }

            boolean exists(String sub) {
                return Files.exists(dir.resolve(sub));
            }
        }

        private record Fix(String before, String answer, String after, String sentence, String translation,
                Instant createdAt) {
        }
    }
}
