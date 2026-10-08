package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.auditdomain.AuditEngine;
import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditReportStore;
import com.learney.contentaudit.auditdomain.AuditReportSummary;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableCourse;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableMilestone;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.AuditableTopic;
import com.learney.contentaudit.auditdomain.Baseline;
import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.auditdomain.DefaultCourseDiagnoses;
import com.learney.contentaudit.auditdomain.IrregularFormsCatalog;
import com.learney.contentaudit.auditdomain.NlpToken;
import com.learney.contentaudit.auditdomain.RuleFix;
import com.learney.contentaudit.auditdomain.WorkingVocabulary;
import com.learney.contentaudit.auditdomain.answercompleteness.AnswerCompletenessCorrection;
import com.learney.contentaudit.auditdomain.auditnodeindex.DefaultAuditNodeIndexFactory;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerMeasure;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.contextnumbers.AuditDigest;
import com.learney.contentaudit.auditdomain.correctionengine.DefaultAnalyzerCorrectionCatalog;
import com.learney.contentaudit.auditdomain.correctionengine.LemmaAbsenceCorrection;
import com.learney.contentaudit.auditdomain.correctionengine.QuizInstructionCorrection;
import com.learney.contentaudit.auditdomain.datacoherence.DataCoherenceCorrection;
import com.learney.contentaudit.auditdomain.datacoherence.DefaultSpokenTextBuilder;
import com.learney.contentaudit.auditdomain.finding.AnalysisCost;
import com.learney.contentaudit.auditdomain.finding.EvidencePart;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingEvidence;
import com.learney.contentaudit.auditdomain.finding.FindingIdentity;
import com.learney.contentaudit.auditdomain.finding.FindingNodeRef;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.auditdomain.hintcoherence.HintCoherenceCorrection;
import com.learney.contentaudit.auditdomain.hintcoherence.HintCoherenceRuleFix;
import com.learney.contentaudit.auditdomain.singleanswerengine.SingleAnswerCorrection;
import com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer;
import com.learney.contentaudit.auditdomain.translationsenseengine.TranslationSenseCorrection;
import com.learney.contentaudit.auditinfrastructure.FileSystemActiveAnalysisSelectionStore;
import com.learney.contentaudit.auditinfrastructure.FileSystemBaselineStore;
import com.learney.contentaudit.auditinfrastructure.FileSystemRevisionArtifactStore;
import com.learney.contentaudit.auditinfrastructure.batchstore.FileSystemBatchStore;
import com.learney.contentaudit.auditinfrastructure.traystore.FileSystemTrayDecisionStore;
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
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.evaluationledgerdomain.contentfingerprint.Sha256ContentFingerprinter;
import com.learney.contentaudit.refinerdomain.DiagnosisKind;
import com.learney.contentaudit.refinerdomain.PlanFindingTally;
import com.learney.contentaudit.refinerdomain.RefinementPlan;
import com.learney.contentaudit.refinerdomain.RefinementPlanStore;
import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.refinerdomain.RefinementTaskStatus;
import com.learney.contentaudit.refinerdomain.RefinerEngine;
import com.learney.contentaudit.refinerdomain.TaskOrigin;
import com.learney.contentaudit.refinerdomain.UnconvertedScoreCount;
import com.learney.contentaudit.refinerdomain.errortask.DefaultErrorTaskBriefer;
import com.learney.contentaudit.refinerdomain.topicwords.TopicWordLights;
import com.learney.contentaudit.revisiondomain.batch.BatchService;
import com.learney.contentaudit.revisiondomain.batch.BatchServiceConfig;
import com.learney.contentaudit.revisiondomain.batch.BatchStore;
import com.learney.contentaudit.revisiondomain.batchengine.DefaultBatchServiceFactory;
import com.learney.contentaudit.revisiondomain.blindsample.BlindPairView;
import com.learney.contentaudit.revisiondomain.blindsample.BlindSample;
import com.learney.contentaudit.revisiondomain.blindsample.BlindSampleResult;
import com.learney.contentaudit.revisiondomain.blindsample.BlindSampleService;
import com.learney.contentaudit.revisiondomain.blindsample.PairAnswer;
import com.learney.contentaudit.revisiondomain.blindsample.StudentSide;
import com.learney.contentaudit.revisiondomain.comparison.BatchComparator;
import com.learney.contentaudit.revisiondomain.comparison.BatchComparison;
import com.learney.contentaudit.revisiondomain.comparison.BatchVerdict;
import com.learney.contentaudit.revisiondomain.comparison.ChangeOutcome;
import com.learney.contentaudit.revisiondomain.comparison.EffectiveGoals;
import com.learney.contentaudit.revisiondomain.comparison.GoalBook;
import com.learney.contentaudit.revisiondomain.comparison.SealedGoals;
import com.learney.contentaudit.revisiondomain.comparison.VerdictKind;
import com.learney.contentaudit.revisiondomain.nonregression.ChangeGateVerdict;
import com.learney.contentaudit.revisiondomain.nonregression.SpokenText;
import com.learney.contentaudit.revisiondomain.nonregression.UnitMeasurement;
import com.learney.contentaudit.revisiondomain.panel.PanelCandidate;
import com.learney.contentaudit.revisiondomain.panel.PanelCandidateKind;
import com.learney.contentaudit.revisiondomain.panel.PanelDecision;
import com.learney.contentaudit.revisiondomain.panel.PanelDecisionSink;
import com.learney.contentaudit.revisiondomain.panel.PanelOutcomeKind;
import com.learney.contentaudit.revisiondomain.panel.PanelRunSettings;
import com.learney.contentaudit.revisiondomain.panel.PanelRunner;
import com.learney.contentaudit.revisiondomain.panel.PanelTopicResult;
import com.learney.contentaudit.revisiondomain.panel.PanelVocabularyContext;
import com.learney.contentaudit.revisiondomain.panel.QueryAllowance;
import com.learney.contentaudit.revisiondomain.panel.TopicMeter;
import com.learney.contentaudit.revisiondomain.panel.TopicWork;
import com.learney.contentaudit.revisiondomain.panelengine.DefaultPanelRunnerFactory;
import com.learney.contentaudit.revisiondomain.preservationengine.DefaultPreservationFactory;
import com.learney.contentaudit.revisiondomain.projection.ProjectionScope;
import com.learney.contentaudit.revisiondomain.tray.TrayService;
import com.learney.contentaudit.revisiondomain.trayengine.DefaultTrayServiceFactory;
import com.learney.contentaudit.revisiondomain.tray.TrayServiceConfig;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import picocli.CommandLine;

/**
 * A batch of A1 from the command line, without a model: «Afirmativas regulares» with four exercises -- «I ___ a lot of
 * vegetables.» without its hint, «He ___ (come) every year.» and «I ___ (drink) tea in the morning.» with another
 * answer that fits, and «You ___ (eat) a lot of fruit.», on which the two judges of translation do not agree --; its
 * analysis, its plan and its baseline; the batches and the decisions of the tray on files under a working directory;
 * the real service of the batch and of the tray from their factories, with the real rule of the hint, the real gate,
 * ledger, closer and publisher; and, by hand, the panel (it rewrites «come» as «Winter» and finds no winner for the
 * tea), the meter, F's comparison, its blind sample and its book of goals.
 */
final class TandaCli {

    static final String A1 = "6814dafa7d73e7209a13d382";
    static final String TOPIC = "6814dafa7d73e7209a13d383";
    static final String AFIRMATIVAS = "6814dafa7d73e7209a13d389";
    static final String VEGETABLES = "67fab6d59930102295341ecd";
    static final String COME = "67fab6d59930102295341ec6";
    static final String TEA = "67fab6d59930102295341ec9";
    static final String FRUIT = "67fab6d59930102295341ec8";
    static final String BASELINE = "audit-20260929";
    static final String PLAN = "plan-20260929";
    static final String SAMPLE = "muestra-tanda-A1";

    final Path workdir;
    final CourseEntity course = course();
    final Courses courses = new Courses(course);
    final BatchStore batches;
    final Reports reports = new Reports();
    final Plans plans = new Plans();
    final FileSystemRevisionArtifactStore artifacts;
    final FileSystemActiveAnalysisSelectionStore active;
    final FileSystemBaselineStore baselines;
    final FileSystemTrayDecisionStore decisions;
    final Panel panel = new Panel();
    final Comparison comparison = new Comparison();
    final Blind blind = new Blind();
    final List<AnalyzerRunSelection> published = new ArrayList<>();
    final List<AuditableCourse> vocabularySeeds = new ArrayList<>();
    final BatchService batchService;
    final TrayService trayService;

    TandaCli(Path workdir) {
        this.workdir = workdir;
        this.batches = new FileSystemBatchStore(workdir);
        this.artifacts = new FileSystemRevisionArtifactStore(workdir);
        this.active = new FileSystemActiveAnalysisSelectionStore(workdir);
        this.baselines = new FileSystemBaselineStore(workdir);
        this.decisions = new FileSystemTrayDecisionStore(workdir);
        reports.saved.put(BASELINE, report(course, List.of(missingHint(), comeAnswer(), teaAnswer(), fruitTie())));
        plans.saved.put(PLAN, new RefinementPlan(PLAN, BASELINE, Instant.parse("2026-09-29T12:00:00Z"), new ArrayList<>(
                List.of(task("t-pista", missingHint(), DiagnosisKind.HINT_COHERENCE),
                        task("t-come", comeAnswer(), DiagnosisKind.SINGLE_ANSWER),
                        task("t-tea", teaAnswer(), DiagnosisKind.SINGLE_ANSWER)))));
        baselines.write(new Baseline(BASELINE, Instant.parse("2026-09-29T08:00:00Z")));

        DefaultAnalyzerCorrectionCatalog corrections = new DefaultAnalyzerCorrectionCatalog(List.of(
                new QuizInstructionCorrection(), new LemmaAbsenceCorrection(), new HintCoherenceCorrection(),
                new AnswerCompletenessCorrection(), new DataCoherenceCorrection(), new SingleAnswerCorrection(),
                new TranslationSenseCorrection()), null);
        DefaultQuizStudentViewRenderer renderer = new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create());
        AuditEngine engine = new AuditEngine() {
            @Override
            public AuditReport runAudit(AuditableCourse auditable) {
                throw new AssertionError("the published course is analyzed with the analyzers of the batch");
            }

            @Override
            public AuditReport runAudit(AuditableCourse auditable, AnalyzerRunSelection selection) {
                published.add(selection);
                return report(courses.course, List.of(teaAnswer(), fruitTie()));
            }

            @Override
            public WorkingVocabulary openWorkingVocabulary(AuditableCourse auditable) {
                vocabularySeeds.add(auditable);
                // This CLI fixture has a scripted panel, not lexical analyzers. The domain tests
                // cover deltas; this double verifies that the factory actually supplies context.
                return new WorkingVocabulary() {
                    @Override
                    public void replaceKnowledge(AuditableKnowledge knowledge) {
                        throw new AssertionError("the scripted panel does not calculate vocabulary deltas");
                    }

                    @Override
                    public <T> T query(BiFunction<AuditReport, Long, T> query) {
                        return query.apply(reports.saved.get(BASELINE), 0L);
                    }
                };
            }
        };
        RefinerEngine refiner = new RefinerEngine() {
            @Override
            public RefinementPlan plan(com.learney.contentaudit.auditdomain.AuditReport report, String auditId) {
                return new RefinementPlan(null, auditId, Instant.parse("2026-10-01T18:00:00Z"), new ArrayList<>());
            }

            @Override
            public Optional<RefinementTask> nextTask(RefinementPlan plan) {
                return Optional.empty();
            }

            @Override
            public List<UnconvertedScoreCount> unconvertedScores(com.learney.contentaudit.auditdomain.AuditReport report) {
                return List.of();
            }

            @Override
            public List<PlanFindingTally> findingTally(com.learney.contentaudit.auditdomain.AuditReport report,
                    RefinementPlan plan) {
                return List.of();
            }
        };
        List<RuleFix> fixes = List.of(new HintCoherenceRuleFix(new IrregularFormsCatalog() {
            @Override
            public Set<String> formsOf(String baseWord) {
                return Set.of();
            }

            @Override
            public boolean isPastParticiple(String word) {
                return false;
            }
        }));
        // The locator of the course and the table of parts to fields go null, as in Main: the factory builds the ones
        // of revision-domain.
        batchService = new DefaultBatchServiceFactory().create(new BatchServiceConfig(batches, plans, reports, active,
                artifacts, courses, null, value -> new AuditableCourse(report(value, List.of()).getRoot().getChildren()
                        .stream().map(node -> (AuditableMilestone) node.getEntity()).toList()), engine, refiner,
                new DefaultErrorTaskBriefer(corrections, new DefaultAuditNodeIndexFactory(), renderer), corrections,
                fixes, panel, new Meter(), new DefaultPanelRunnerFactory(), comparison, baselines, blind,
                (report, auditId, knowledgeId, criteria) -> new TopicWordLights(auditId, knowledgeId,
                        "Afirmativas regulares", CefrLevel.A1, List.of(), List.of()), null,
                new DefaultPreservationFactory().createCheck(), workdir.resolve("english-course.json"), new Goals(),
                null));
        trayService = new DefaultTrayServiceFactory().create(new TrayServiceConfig(batchService, batches, decisions,
                blind, panel, new DefaultPanelRunnerFactory(), renderer, new Sha256ContentFingerprinter(), courses,
                workdir.resolve("english-course.json")));
    }

    // -------------------------------------------------------------------------
    // The command line
    // -------------------------------------------------------------------------

    record Run(int exit, String out, String err) {
    }

    Run batch(String... args) {
        return run(new BatchCmd(batchService), args);
    }

    Run tray(String... args) {
        return run(new TrayCmd(trayService, DefaultQuizSentenceConverter.create()), args);
    }

    private static Run run(Object command, String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
        try {
            int exit = new CommandLine(command).execute(args);
            return new Run(exit, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    // -------------------------------------------------------------------------
    // The course, its analysis and its plan
    // -------------------------------------------------------------------------

    static SentencePartEntity text(String text) {
        return new SentencePartEntity(SentencePartKind.TEXT, text, null);
    }

    static SentencePartEntity cloze(String... options) {
        return new SentencePartEntity(SentencePartKind.CLOZE, "", new ArrayList<>(List.of(options)));
    }

    static QuizTemplateEntity quiz(String id, String translation, String sentence, SentencePartEntity... parts) {
        FormEntity form = new FormEntity("CLOZE", 1.0, "", "", new ArrayList<>(List.of(parts)), null, null);
        QuizTemplateEntity quiz = new QuizTemplateEntity(id, id, "CLOZE", AFIRMATIVAS, "Afirmativas regulares",
                "Completa con el verbo en presente simple.", translation, null, "Present Simple", form, 0, 0, 0, null,
                null, null, "A1.02.01." + id.substring(id.length() - 2), null, null, null, null, null);
        quiz.setSentences(new ArrayList<>(List.of(sentence)));
        return quiz;
    }

    static CourseEntity course() {
        List<QuizTemplateEntity> quizzes = new ArrayList<>(List.of(
                quiz(VEGETABLES, "Me gustan mucho las verduras.", "I love a lot of vegetables.", text("I"), cloze("love"),
                        text("a lot of vegetables.")),
                quiz(COME, "Él viene cada año.", "He comes every year.", text("He"), cloze("comes"),
                        text("(come) every year.")),
                quiz(TEA, "Tomo té por la mañana.", "I drink tea in the morning.", text("I"), cloze("drink"),
                        text("(drink) tea in the morning.")),
                quiz(FRUIT, "Comes mucha fruta.", "You eat a lot of fruit.", text("You"), cloze("eat"),
                        text("(eat) a lot of fruit."))));
        KnowledgeEntity afirmativas = new KnowledgeEntity(AFIRMATIVAS, null, NodeKind.KNOWLEDGE, "Afirmativas regulares",
                null, TOPIC, false, "Usa el presente simple.", 0, null, quizzes, SentenceMode.FILL);
        TopicEntity present = new TopicEntity(TOPIC, null, NodeKind.TOPIC, "Present Simple", null, A1, null, null, 0, null,
                new ArrayList<>(List.of(afirmativas)));
        MilestoneEntity a1 = new MilestoneEntity(A1, null, NodeKind.MILESTONE, "A1", null, "root", null, 0, null,
                new ArrayList<>(List.of(present)));
        return new CourseEntity("curso", "Learney", null, new RootNodeEntity("root", null, NodeKind.ROOT, "Curso", null,
                new ArrayList<>(List.of(a1))), null);
    }

    static Finding finding(String analyzer, String rule, String quizId, FindingSeverity severity,
            FindingResolution resolution, String observation, EvidencePart... parts) {
        return new Finding(analyzer, rule, new FindingNodeRef(AuditTarget.QUIZ, quizId, quizId), severity,
                new FindingEvidence(new ArrayList<>(List.of(parts)), observation, new ArrayList<>()), resolution,
                AnalysisCost.INSTANT, new FindingIdentity(analyzer + "|" + rule + "|" + quizId, null));
    }

    static Finding missingHint() {
        return finding("hint-coherence", "missing-hint", VEGETABLES, FindingSeverity.HIGH, FindingResolution.RULE,
                "no lleva pista y los otros ejercicios del tema la llevan");
    }

    static Finding comeAnswer() {
        return finding("single-answer", "different-valid-answer", COME, FindingSeverity.BLOCKING,
                FindingResolution.PANEL, "«goes» también encaja");
    }

    static Finding teaAnswer() {
        return finding("single-answer", "different-valid-answer", TEA, FindingSeverity.BLOCKING,
                FindingResolution.PANEL, "«have» también encaja");
    }

    static Finding fruitTie() {
        return finding("translation-sense", "judges-disagree", FRUIT, FindingSeverity.MEDIUM, FindingResolution.RANK_ONLY,
                "los dos jueces de traducción y sentido no coinciden",
                new EvidencePart("Juez desde el inglés", "sí: «Comes mucha fruta.» dice lo mismo"),
                new EvidencePart("Juez desde la traducción", "no: «mucha fruta» no dice «a lot of»"));
    }

    static RefinementTask task(String id, Finding finding, DiagnosisKind kind) {
        String quizId = finding.getNode().getNodeId();
        return new RefinementTask(id, AuditTarget.QUIZ, quizId, quizId, kind, 1, RefinementTaskStatus.PENDING,
                new TaskOrigin(finding, CefrLevel.A1, AFIRMATIVAS, "Afirmativas regulares",
                        new ArrayList<>(List.of(quizId)), new ArrayList<>()));
    }

    /** The analysis of the course: each exercise with its findings and its tokens, the measure of its analyzers. */
    static AuditReport report(CourseEntity course, List<Finding> findings) {
        Map<String, List<Finding>> byNode = new HashMap<>();
        for (Finding finding : findings) {
            byNode.computeIfAbsent(finding.getNode().getNodeId(), k -> new ArrayList<>()).add(finding);
        }
        AuditNode root = node(null, AuditTarget.COURSE, null);
        root.setDiagnoses(new DefaultCourseDiagnoses());
        for (MilestoneEntity milestone : course.getRoot().getMilestones()) {
            AuditNode milestoneNode = node(null, AuditTarget.MILESTONE, root);
            List<AuditableTopic> topics = new ArrayList<>();
            for (TopicEntity topic : milestone.getTopics()) {
                AuditNode topicNode = node(null, AuditTarget.TOPIC, milestoneNode);
                List<AuditableKnowledge> knowledges = new ArrayList<>();
                for (KnowledgeEntity knowledge : topic.getKnowledges()) {
                    AuditNode knowledgeNode = node(null, AuditTarget.KNOWLEDGE, topicNode);
                    List<AuditableQuiz> quizzes = new ArrayList<>();
                    for (QuizTemplateEntity quiz : knowledge.getQuizTemplates()) {
                        AuditableQuiz auditable = new AuditableQuiz(tokens(quiz), quiz.getId(), quiz.getTitle(),
                                quiz.getCode(), quiz.getTranslation(), quiz.getSentences(), null, quiz.getInstructions(),
                                quiz.getForm().getSentenceParts(), quiz.getForm().getMultipleChoice(),
                                quiz.getMiniTheory(), quiz.getAnswerAudioUrl());
                        quizzes.add(auditable);
                        AuditNode quizNode = node(auditable, AuditTarget.QUIZ, knowledgeNode);
                        quizNode.setFindings(new ArrayList<>(byNode.getOrDefault(quiz.getId(), List.of())));
                    }
                    AuditableKnowledge auditable = new AuditableKnowledge(quizzes, knowledge.getLabel(),
                            knowledge.getInstructions(), true, knowledge.getId(), knowledge.getLabel(), knowledge.getCode(),
                            knowledge.getSentenceMode(), topic.getLabel());
                    knowledgeNode.setEntity(auditable);
                    knowledges.add(auditable);
                }
                AuditableTopic auditable = new AuditableTopic(knowledges, topic.getId(), topic.getLabel(), topic.getCode());
                topicNode.setEntity(auditable);
                topics.add(auditable);
            }
            milestoneNode.setEntity(new AuditableMilestone(topics, milestone.getId(), milestone.getLabel(),
                    milestone.getCode()));
        }
        List<AnalyzerMeasure> measures = new ArrayList<>();
        for (String analyzer : List.of("hint-coherence", "single-answer", "translation-sense")) {
            measures.add(new AnalyzerMeasure(analyzer, "huella-" + analyzer, new LinkedHashMap<>()));
        }
        root.setMeasures(measures);
        return new AuditReport(root);
    }

    private static AuditNode node(com.learney.contentaudit.auditdomain.AuditableEntity entity, AuditTarget target,
            AuditNode parent) {
        AuditNode node = new AuditNode(entity, target, parent, new ArrayList<>(), new LinkedHashMap<>(),
                new LinkedHashMap<>(), null);
        if (parent != null) {
            parent.getChildren().add(node);
        }
        return node;
    }

    /** The tokens of the saved sentence: the vegetables as spaCy reads them, the rest split by spaces. */
    static List<NlpToken> tokens(QuizTemplateEntity quiz) {
        List<NlpToken> tokens = new ArrayList<>();
        if (VEGETABLES.equals(quiz.getId())) {
            for (String spec : "I|I|PRON love|love|VERB a|a|DET lot|lot|NOUN of|of|ADP vegetables|vegetable|NOUN .|.|PUNCT"
                    .split(" ")) {
                String[] f = spec.split("\\|");
                tokens.add(new NlpToken(f[0], f[1], f[2], null, false, f[2].equals("PUNCT")));
            }
            return tokens;
        }
        String sentence = quiz.getSentences() != null && !quiz.getSentences().isEmpty() ? quiz.getSentences().get(0) : "";
        for (String word : sentence.replaceAll("([.?!,])", " $1").split("\\s+")) {
            if (!word.isEmpty()) {
                boolean punct = word.matches("[.?!,]");
                tokens.add(new NlpToken(word, word.toLowerCase(), punct ? "PUNCT" : "X", null, false, punct));
            }
        }
        return tokens;
    }

    // -------------------------------------------------------------------------
    // By hand: the panel, the meter, F's comparison, its blind sample, its goals, the course and two stores
    // -------------------------------------------------------------------------

    /** The panel: «He ___ (come)» becomes «Winter ___ (come) every year.»; nobody fixes the tea. One query each. */
    static final class Panel implements PanelRunner {
        final List<String> topics = new ArrayList<>();
        final List<TopicWordLights> contexts = new ArrayList<>();

        @Override
        public List<PanelTopicResult> runTopics(List<TopicWork> works, PanelRunSettings settings,
                QueryAllowance allowance, PanelDecisionSink sink, PanelVocabularyContext vocabulary) {
            if (vocabulary == null) throw new AssertionError("the factory must pass the provisional context");
            for (TopicWork work : works) contexts.add(vocabulary.lightsFor(work));
            return runTopics(works, settings, allowance, sink);
        }

        @Override
        public List<PanelTopicResult> runTopics(List<TopicWork> works, PanelRunSettings settings,
                QueryAllowance allowance, PanelDecisionSink sink) {
            List<PanelTopicResult> results = new ArrayList<>();
            for (TopicWork work : works) {
                topics.add(work.getKnowledge().getId());
                List<PanelDecision> decided = new ArrayList<>();
                for (var brief : work.getBriefs()) {
                    String quizId = brief.getTask().getNodeId();
                    String taskId = brief.getTask().getId();
                    PanelDecision decision;
                    if (allowance == null || allowance.tripped().isPresent() || !allowance.tryReserve(1)) {
                        decision = new PanelDecision(AFIRMATIVAS, List.of(quizId), List.of(taskId),
                                PanelOutcomeKind.NOT_ATTEMPTED, null, null, List.of(), 0, List.of(), null, null, List.of(), 0);
                    } else {
                        allowance.settle(1, 1);
                        QuizTemplateEntity quiz = work.getKnowledge().getQuizTemplates().stream()
                                .filter(q -> q.getId().equals(quizId)).findFirst().orElseThrow();
                        if (COME.equals(quizId)) {
                            QuizTemplateEntity winter = edited(quiz, "Winter", "Winter comes every year.");
                            decision = new PanelDecision(AFIRMATIVAS, List.of(quizId), List.of(taskId),
                                    PanelOutcomeKind.ACCEPTED, new PanelCandidate(PanelCandidateKind.ENGLISH_CHANGE, winter,
                                            null, null, "«He» sin antecedente"),
                                    new ChangeGateVerdict(true, List.of(), List.of(), List.of(), List.of(), List.of(),
                                            List.of()), List.of(taskId), 1, List.of(), null, null, List.of(), 1);
                        } else {
                            decision = new PanelDecision(AFIRMATIVAS, List.of(quizId), List.of(taskId),
                                    PanelOutcomeKind.NO_WINNER, null, null, List.of(), 3, List.of(), new PanelCandidate(
                                            PanelCandidateKind.ENGLISH_CHANGE, edited(quiz, "I", "I drink tea every morning."),
                                            null, null, "una sola respuesta"), null, List.of(), 1);
                        }
                    }
                    sink.accept(decision);
                    decided.add(decision);
                }
                results.add(new PanelTopicResult(work.getKnowledge().getId(), decided, 0, false));
            }
            return results;
        }

        @Override
        public PanelDecision assessGiven(TopicWork work, String quizId, PanelCandidate candidate,
                PanelRunSettings settings, QueryAllowance allowance) {
            throw new AssertionError("José writes no fix in these journeys");
        }

        private static QuizTemplateEntity edited(QuizTemplateEntity quiz, String subject, String sentence) {
            QuizTemplateEntity copy = com.learney.contentaudit.coursedomain.QuizTemplateEntities.copyOf(quiz);
            FormEntity form = com.learney.contentaudit.coursedomain.FormEntities.copyOf(quiz.getForm());
            List<SentencePartEntity> parts = new ArrayList<>();
            for (SentencePartEntity part : quiz.getForm().getSentenceParts()) {
                parts.add(new SentencePartEntity(part.getKind(), part.getText(), part.getOptions() != null
                        ? new ArrayList<>(part.getOptions()) : null));
            }
            parts.set(0, text(subject));
            form.setSentenceParts(parts);
            copy.setForm(form);
            copy.setSentences(new ArrayList<>(List.of(sentence)));
            return copy;
        }
    }

    /** The meter of the panel without an engine: what each exercise says aloud, and no finding. */
    static final class Meter implements TopicMeter {
        @Override
        public UnitMeasurement measure(TopicWork work, KnowledgeEntity knowledgeState, List<String> analyzers,
                QueryAllowance allowance) {
            List<SpokenText> spoken = new ArrayList<>();
            for (QuizTemplateEntity quiz : knowledgeState.getQuizTemplates()) {
                AuditableQuiz auditable = new AuditableQuiz(tokens(quiz), quiz.getId(), quiz.getTitle(), quiz.getCode(),
                        quiz.getTranslation(), quiz.getSentences(), null, quiz.getInstructions(),
                        quiz.getForm().getSentenceParts(), quiz.getForm().getMultipleChoice(), quiz.getMiniTheory(),
                        quiz.getAnswerAudioUrl());
                spoken.add(new SpokenText(quiz.getId(), quiz.getAnswerAudioUrl(), new ArrayList<>(
                        new DefaultSpokenTextBuilder().spokenTexts(auditable, knowledgeState.getSentenceMode()))));
            }
            return new UnitMeasurement(work.getLevel(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), spoken,
                    new ArrayList<>());
        }
    }

    /** F's comparison, by hand: each change of the scope fixes the findings of its exercise, and the batch passes. */
    static final class Comparison implements BatchComparator {
        final List<ProjectionScope> scopes = new ArrayList<>();

        @Override
        public BatchComparison compare(Path coursePath, ProjectionScope scope) {
            scopes.add(scope);
            List<ChangeOutcome> outcomes = new ArrayList<>();
            for (String proposalId : scope.getProposalIds()) {
                String quizId = proposalId.substring(proposalId.lastIndexOf('-') + 1);
                List<Finding> fixed = new ArrayList<>();
                for (Finding finding : List.of(missingHint(), comeAnswer(), teaAnswer())) {
                    if (quizId.equals(finding.getNode().getNodeId())) {
                        fixed.add(finding);
                    }
                }
                outcomes.add(new ChangeOutcome(proposalId, AuditTarget.QUIZ, quizId, false, new ArrayList<>(), fixed,
                        new ArrayList<>()));
            }
            BatchComparison comparison = new BatchComparison();
            comparison.setScopeId(scope.getScopeId());
            comparison.setAppliedProposalIds(new ArrayList<>(scope.getProposalIds()));
            comparison.setChanges(outcomes);
            comparison.setRows(new ArrayList<>());
            comparison.setBreaches(new ArrayList<>());
            comparison.setVerdict(new BatchVerdict(VerdictKind.PASSES, new ArrayList<>(), new ArrayList<>(), null,
                    "Pasa: se cumplen los pisos y no vuelve ningún cambio.", null));
            return comparison;
        }
    }

    /** F's blind sample, by hand: two pairs. */
    static final class Blind implements BlindSampleService {
        final List<String> answers = new ArrayList<>();
        boolean closed;

        @Override
        public BlindSample draw(Path coursePath, ProjectionScope scope, Long seed) {
            BlindSample sample = new BlindSample();
            sample.setSampleId(SAMPLE);
            sample.setScopeId(scope.getScopeId());
            return sample;
        }

        @Override
        public List<BlindPairView> pairs(String sampleId) {
            return List.of(new BlindPairView(1, new StudentSide(null, null), new StudentSide(null, null)),
                    new BlindPairView(2, new StudentSide(null, null), new StudentSide(null, null)));
        }

        @Override
        public void answer(String sampleId, int pairIndex, PairAnswer answer, String reviewer) {
            answers.add(pairIndex + " " + answer);
        }

        @Override
        public BlindSampleResult close(String sampleId) {
            closed = true;
            return result(sampleId).orElseThrow();
        }

        @Override
        public Optional<BlindSampleResult> result(String sampleId) {
            return closed ? Optional.of(new BlindSampleResult(sampleId, null, "José", answers.size(), null, null, null,
                    true, List.of(), null, null, List.of())) : Optional.empty();
        }
    }

    /** F's book of goals: none declared, sealed when asked. */
    static final class Goals implements GoalBook {
        final List<SealedGoals> seals = new ArrayList<>();

        @Override
        public EffectiveGoals effective() {
            return new EffectiveGoals("huella-metas", new ArrayList<>(), null);
        }

        @Override
        public SealedGoals seal() {
            SealedGoals sealed = new SealedGoals(Instant.now(), effective());
            seals.add(sealed);
            return sealed;
        }

        @Override
        public Optional<SealedGoals> sealed() {
            return seals.isEmpty() ? Optional.empty() : Optional.of(seals.get(seals.size() - 1));
        }
    }

    /** The course of content-audit at its path: what it holds and every time it is written. */
    static final class Courses implements CourseRepository {
        CourseEntity course;
        final List<CourseEntity> written = new ArrayList<>();

        Courses(CourseEntity course) {
            this.course = course;
        }

        @Override
        public CourseEntity load(Path path) {
            return course;
        }

        @Override
        public void save(CourseEntity saved, Path path) {
            written.add(saved);
            course = saved;
        }
    }

    /** The analyses, in memory: each new one with an id of its own. */
    static final class Reports implements AuditReportStore {
        final Map<String, AuditReport> saved = new LinkedHashMap<>();

        @Override
        public String save(AuditReport report) {
            String id = "audit-publicado-" + saved.size();
            saved.put(id, report);
            return id;
        }

        @Override
        public Optional<AuditReport> load(String id) {
            return Optional.ofNullable(saved.get(id));
        }

        @Override
        public Optional<AuditReport> loadLatest() {
            return saved.values().stream().reduce((a, b) -> b);
        }

        @Override
        public List<AuditReportSummary> list() {
            return new ArrayList<>();
        }

        @Override
        public Optional<AuditDigest> loadDigest(String id) {
            return Optional.empty();
        }
    }

    /** The plans, in memory. */
    static final class Plans implements RefinementPlanStore {
        final Map<String, RefinementPlan> saved = new LinkedHashMap<>();

        @Override
        public String save(RefinementPlan plan) {
            if (plan.getId() == null) {
                plan.setId("plan-publicado-" + saved.size());
            }
            saved.put(plan.getId(), plan);
            return plan.getId();
        }

        @Override
        public Optional<RefinementPlan> load(String id) {
            return Optional.ofNullable(saved.get(id));
        }

        @Override
        public Optional<RefinementPlan> loadLatest() {
            return saved.values().stream().reduce((a, b) -> b);
        }
    }
}
