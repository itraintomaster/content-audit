package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.auditdomain.AnalyzerCatalog;
import com.learney.contentaudit.auditdomain.AnalyzerDescriptor;
import com.learney.contentaudit.auditdomain.AnalyzerProvider;
import com.learney.contentaudit.auditdomain.AuditEngine;
import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableCourse;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableMilestone;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.AuditableTopic;
import com.learney.contentaudit.auditdomain.ContentAnalyzer;
import com.learney.contentaudit.auditdomain.CourseMapper;
import com.learney.contentaudit.auditdomain.DefaultCourseDiagnoses;
import com.learney.contentaudit.auditdomain.EvaluationRunPolicy;
import com.learney.contentaudit.auditdomain.IAuditEngine;
import com.learney.contentaudit.auditdomain.IScoreAggregator;
import com.learney.contentaudit.auditdomain.SelfDescribingConfig;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerFamily;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerMeasure;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerPlanBinding;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRuleCard;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.finding.AnalysisCost;
import com.learney.contentaudit.auditdomain.finding.FindingDraft;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.findingengine.DefaultAnalyzerCatalog;
import com.learney.contentaudit.auditdomain.findingengine.DefaultContextNumbersCalculator;
import com.learney.contentaudit.auditdomain.findingengine.DefaultFindingCollector;
import com.learney.contentaudit.auditdomain.judging.EvaluatorCoverage;
import com.learney.contentaudit.auditdomain.judging.JudgeCostEstimate;
import com.learney.contentaudit.auditdomain.judging.JudgeCoverageDiagnosis;
import com.learney.contentaudit.auditdomain.judging.JudgeExamStanding;
import com.learney.contentaudit.auditdomain.judging.JudgeExamStatus;
import com.learney.contentaudit.auditdomain.judging.JudgeQuizTally;
import com.learney.contentaudit.auditdomain.localtools.LocalToolCard;
import com.learney.contentaudit.auditdomain.localtools.LocalToolRunDiagnosis;
import com.learney.contentaudit.auditdomain.localtools.LocalToolStatus;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.MilestoneEntity;
import com.learney.contentaudit.coursedomain.NodeKind;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.RootNodeEntity;
import com.learney.contentaudit.coursedomain.SentenceMode;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.coursedomain.TopicEntity;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationCoverage;
import com.learney.contentaudit.revisiondomain.batch.BatchProposalRef;
import com.learney.contentaudit.revisiondomain.batch.BatchSettings;
import com.learney.contentaudit.revisiondomain.batch.CorrectionBatch;
import com.learney.contentaudit.revisiondomain.panelengine.MeterBench;
import com.learney.contentaudit.revisiondomain.panelengine.MesaFixture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The judges' pass of the batch without a model nor spaCy: the real engine over a catalog whose judges keep their
 * verdicts in a ledger by what each one reads -- the exercise with the labels of its level and topic, or the whole topic
 * for mini-theory --, each one consulting only what the ledger does not have, within the budget its run gives it, and
 * saying it in its coverage, as the judges of the catalog do. It brings the cards of the real judges (Spanish names,
 * evaluators, cost, local tools), the exam each one stands on, a service that fails, Ollama there or not for
 * naturalness, the courses the cases of FEAT-CJUEZ start from, and the record of what the pass asked of the engine and
 * of what was consulted.
 */
final class JudgePassBench {

    public static final String HINT_COHERENCE = "hint-coherence";

    public static final String QUIZ_INSTRUCTION = "quiz-instruction";

    public static final String MULTIPLE_CHOICE = "multiple-choice-validity";

    public static final String TRANSLATION_SENSE = "translation-sense";

    public static final String SINGLE_ANSWER = "single-answer";

    public static final String MINI_THEORY = "mini-theory";

    public static final String GRAMMAR = "grammar";

    public static final String NATURALNESS = "naturalness";

    /** The analyzers of the catalog, in the order of the catalog. */
    public static final List<String> CATALOG = List.of(HINT_COHERENCE, QUIZ_INSTRUCTION, MULTIPLE_CHOICE,
            TRANSLATION_SENSE, SINGLE_ANSWER, MINI_THEORY, GRAMMAR, NATURALNESS);

    /** The two evaluators of translation-sense: two queries for each exercise. */
    public static final List<String> TRANSLATION_EVALUATORS = List.of("translation-sense-from-english",
            "translation-sense-from-translation");

    /** The queries at once of the batches this bench builds. */
    public static final int QUERIES_AT_ONCE = 3;

    // The exercises and topics of the course the cases start from.

    public static final String TEMA_AFIRMATIVAS = "tema-afirmativas";

    public static final String TEMA_CONTRASTE = "tema-contraste";

    public static final String TEMA_CONTRACCIONES = "tema-contracciones";

    public static final String TEMA_PASADO = "tema-pasado";

    public static final String AFIRM_1 = "afirm-1";

    public static final String AFIRM_2 = "afirm-2";

    public static final String AFIRM_3 = "afirm-3";

    public static final String AFIRM_4 = "afirm-4";

    public static final String CONTR_1 = "contr-1";

    public static final String CONTR_2 = "contr-2";

    public static final String CONTR_3 = "contr-3";

    public static final String CONTR_MC = "contr-mc";

    public static final String CONTRAC_1 = "contrac-1";

    public static final String CONTRAC_2 = "contrac-2";

    public static final String CONTRAC_3 = "contrac-3";

    public static final String PAST_1 = "past-1";

    public static final String PAST_2 = "past-2";

    /** The ledger: the verdict of each evaluator by what it read, as «evaluator :: what it read». */
    public final Map<String, Boolean> book = new ConcurrentHashMap<>();

    /** Every query a judge started, in order, the failed ones too. */
    public final List<Query> queries = Collections.synchronizedList(new ArrayList<>());

    /** Every run the pass asked of the engine, with the selection and the course it gave. */
    public final List<Run> runs = Collections.synchronizedList(new ArrayList<>());

    private final Map<String, Exam> exams = new ConcurrentHashMap<>();

    private final Map<String, String> servicesDown = new ConcurrentHashMap<>();

    /** By judge, the versions its coverage says its verdicts came from, on every cut (F-ALDIA-R005). */
    public final Map<String, List<com.learney.contentaudit.auditdomain.judging.JudgeVersionStanding>> versionsSeen =
            new ConcurrentHashMap<>();

    /**
     * By judge, the exercises where it gives a verdict without a score: «revisar», as naturalness leaves it, pending
     * in its coverage (F-ALDIA-R003 inv. 6).
     */
    public final Map<String, Set<String>> abstainOn = new ConcurrentHashMap<>();

    private volatile boolean ollama = true;

    private final Map<String, Judge> judges = new LinkedHashMap<>();

    private final DefaultAnalyzerCatalog catalog;

    private final AuditEngine real;

    private final AuditEngine engine;

    private static final CourseMapper MAPPER = new MeterBench().mapper();

    /** A query a judge started: what it read and whether it came back with a verdict. */
    public record Query(String judge, String evaluator, String unitId, String read, boolean answered) {
        public String key() {
            return evaluator + " :: " + read;
        }
    }

    /** A run of the engine: the analyzers and policies it got, and the course. */
    public record Run(AnalyzerRunSelection selection, AuditableCourse course) {
    }

    /** What a judge reads: the labels of the level and topic above the exercise, the topic, and the exercise. */
    record Seen(AuditableMilestone level, AuditableTopic topic, AuditableKnowledge tema, AuditableQuiz quiz) {
        static Seen of(AuditNode node) {
            AuditNode temaNode = node.getTarget() == AuditTarget.KNOWLEDGE ? node : node.getParent();
            AuditNode topicNode = temaNode.getParent();
            AuditNode levelNode = topicNode.getParent();
            return new Seen((AuditableMilestone) levelNode.getEntity(), (AuditableTopic) topicNode.getEntity(),
                    (AuditableKnowledge) temaNode.getEntity(),
                    node.getTarget() == AuditTarget.QUIZ ? (AuditableQuiz) node.getEntity() : null);
        }
    }

    private record Exam(JudgeExamStatus status, String reason) {
        boolean approved() {
            return status == JudgeExamStatus.APPROVED;
        }
    }

    public JudgePassBench() {
        List<AnalyzerProvider> providers = new ArrayList<>();
        providers.add(new InstantRules(HINT_COHERENCE, "Pista que no cuadra", "missing-hint"));
        providers.add(register(new Judge(QUIZ_INSTRUCTION, "Consigna que no se cumple", AuditTarget.QUIZ,
                List.of("quiz-instruction"), List.of(rule("instruction-fits", AnalysisCost.PAID_MODEL)), List.of(),
                false, quiz -> quiz.getMultipleChoice() == null,
                seen -> where(seen) + " :: " + seen.quiz().getInstructions() + " || " + parts(seen.quiz()))));
        providers.add(register(new Judge(MULTIPLE_CHOICE, "Opción múltiple mal armada", AuditTarget.QUIZ,
                List.of("choice-alternative-judge"), List.of(rule("another-option-valid", AnalysisCost.PAID_MODEL)),
                List.of(), false, quiz -> quiz.getMultipleChoice() != null,
                seen -> where(seen) + " :: " + statement(seen.quiz()) + " " + seen.quiz().getMultipleChoice().getItems()
                        .stream().map(item -> item.getLabel()).toList())));
        providers.add(register(new Judge(TRANSLATION_SENSE, "Traducción que no corresponde", AuditTarget.QUIZ,
                TRANSLATION_EVALUATORS, List.of(rule("same-meaning", AnalysisCost.PAID_MODEL)), List.of(), false,
                quiz -> quiz.getMultipleChoice() == null,
                seen -> where(seen) + " :: " + statement(seen.quiz()) + " || " + seen.quiz().getTranslation())));
        providers.add(register(new Judge(SINGLE_ANSWER, "Más de una respuesta válida", AuditTarget.QUIZ,
                List.of("single-answer"), List.of(rule("different-valid-answer", AnalysisCost.PAID_MODEL)), List.of(),
                false, quiz -> quiz.getMultipleChoice() == null,
                seen -> where(seen) + " :: " + seen.quiz().getInstructions() + " || " + parts(seen.quiz()))));
        providers.add(register(new Judge(MINI_THEORY, "Miniteoría que no cuadra", AuditTarget.KNOWLEDGE,
                List.of("mini-theory"), List.of(rule("theory-fits", AnalysisCost.PAID_MODEL)), List.of(), false,
                quiz -> true, JudgePassBench::wholeTopic)));
        providers.add(new LanguageTool(GRAMMAR, "Error de gramática"));
        providers.add(register(new Judge(NATURALNESS, "Oración poco natural", AuditTarget.QUIZ,
                List.of("naturalness"), List.of(rule("unlikely-sentence", AnalysisCost.LOCAL_MODEL),
                        rule("native-says-no", AnalysisCost.PAID_MODEL)), naturalnessTools(), true,
                quiz -> quiz.getMultipleChoice() == null,
                seen -> where(seen) + " :: " + statement(seen.quiz()))));
        this.catalog = new DefaultAnalyzerCatalog(providers);
        this.real = new IAuditEngine(new IScoreAggregator(), catalog, new DefaultFindingCollector(),
                new DefaultContextNumbersCalculator());
        this.engine = new AuditEngine() {
            @Override
            public AuditReport runAudit(AuditableCourse course) {
                throw new AssertionError("the pass always names the judges it runs");
            }

            @Override
            public AuditReport runAudit(AuditableCourse course, AnalyzerRunSelection selection) {
                runs.add(new Run(selection, course));
                return real.runAudit(course, selection);
            }
        };
    }

    private Judge register(Judge judge) {
        judges.put(judge.name, judge);
        return judge;
    }

    // -------------------------------------------------------------------------
    // What the pass is given
    // -------------------------------------------------------------------------

    public CourseMapper mapper() {
        return MAPPER;
    }

    /**
     * The same catalog with the card of one judge without its Spanish name -- the catalog of the engine refuses such a
     * card at startup, but the pass only reads the interface and has to name that judge by its unique name.
     */
    public AnalyzerCatalog catalogWithoutDisplayName(String analyzer) {
        return new AnalyzerCatalog() {
            @Override
            public List<AnalyzerDescriptor> list() {
                List<AnalyzerDescriptor> cards = new ArrayList<>();
                for (AnalyzerDescriptor card : catalog.list()) {
                    cards.add(card.getName().equals(analyzer) ? withoutDisplayName(card) : card);
                }
                return cards;
            }

            @Override
            public Optional<AnalyzerDescriptor> find(String analyzerName) {
                return catalog.find(analyzerName).map(card -> card.getName().equals(analyzer) ? withoutDisplayName(card) : card);
            }

            @Override
            public Optional<AnalyzerProvider> provider(String analyzerName) {
                return catalog.provider(analyzerName);
            }

            @Override
            public Optional<AnalyzerPlanBinding> planBinding(String analyzerName) {
                return catalog.planBinding(analyzerName);
            }

            @Override
            public Optional<AnalyzerMeasure> measure(String analyzerName) {
                return catalog.measure(analyzerName);
            }
        };
    }

    private static AnalyzerDescriptor withoutDisplayName(AnalyzerDescriptor card) {
        return new AnalyzerDescriptor(card.getName(), card.getDescription(), card.getTarget(), card.getQuestion(),
                card.getReads(), card.getRules(), card.getGoal(), card.getFamily(), card.getEvaluatedTargets(),
                card.getResolutions(), card.getCost(), null, card.getTools(), card.getExplanation());
    }

    /** The pass under test, over the engine, the mapper and the catalog of this bench. */
    public DefaultBatchJudgePass pass() {
        return new DefaultBatchJudgePass(engine, MAPPER, catalog);
    }

    /** The pass under test, marking what each judge reuses against today's judge (F-ALDIA-R005). */
    public DefaultBatchJudgePass pass(com.learney.contentaudit.auditdomain.judging.JudgeFreshnessCatalog freshness) {
        return new DefaultBatchJudgePass(engine, MAPPER, catalog, freshness);
    }

    /** The pass under test, over this engine and mapper and the given catalog. */
    public DefaultBatchJudgePass pass(AnalyzerCatalog seenByThePass) {
        return new DefaultBatchJudgePass(engine, MAPPER, seenByThePass);
    }

    // -------------------------------------------------------------------------
    // What the judges stand on
    // -------------------------------------------------------------------------

    /** The version of the judge has no approved exam: it reuses what the ledger has and consults nothing. */
    public void withoutApprovedExam(String judge, JudgeExamStatus status, String reason) {
        exams.put(judge, new Exam(status, reason));
    }

    /** The service behind the judge fails: its first query of a run comes back without a verdict, for this reason. */
    public void serviceDown(String judge, String reason) {
        servicesDown.put(judge, reason);
    }

    /** Ollama is not there: naturalness goes by its claude -p alternative and is paid like any other judge. */
    public void naturalnessGoesByClaude() {
        this.ollama = false;
    }

    private Exam exam(String judge) {
        return exams.getOrDefault(judge, new Exam(JudgeExamStatus.APPROVED, "aprobado el 30/9"));
    }

    // -------------------------------------------------------------------------
    // What happened
    // -------------------------------------------------------------------------

    /** The queries of the judge that came back with a verdict, as «evaluator :: what it read», in order. */
    public List<String> consulted(String judge) {
        List<String> keys = new ArrayList<>();
        synchronized (queries) {
            for (Query query : queries) {
                if (query.judge().equals(judge) && query.answered()) {
                    keys.add(query.key());
                }
            }
        }
        Collections.sort(keys);
        return keys;
    }

    /** Every query the judge started, those that came back without a verdict too. */
    public List<Query> attempts(String judge) {
        List<Query> started = new ArrayList<>();
        synchronized (queries) {
            for (Query query : queries) {
                if (query.judge().equals(judge)) {
                    started.add(query);
                }
            }
        }
        return started;
    }

    /** The most queries any run of the engine allowed the judge; 0 if none ever gave it a budget. */
    public int maxBudgetGiven(String judge) {
        int max = 0;
        synchronized (runs) {
            for (Run run : runs) {
                AnalyzerRunSelection selection = run.selection();
                if (selection.getAnalyzers() != null && selection.getAnalyzers().contains(judge)
                        && selection.getPolicies() != null && selection.getPolicies().get(judge) != null) {
                    max = Math.max(max, selection.getPolicies().get(judge).getMaxNewEvaluations());
                }
            }
        }
        return max;
    }

    /** What the judge reads of the unit (an exercise, or a topic for mini-theory) as the course has it. */
    public String readOf(String judge, CourseEntity course, String unitId) {
        Judge reader = judges.get(judge);
        for (AuditableMilestone level : MAPPER.map(course).getMilestones()) {
            for (AuditableTopic topic : level.getTopics()) {
                for (AuditableKnowledge tema : topic.getKnowledge()) {
                    if (reader.unit == AuditTarget.KNOWLEDGE && tema.getId().equals(unitId)) {
                        return reader.fingerprint.apply(new Seen(level, topic, tema, null));
                    }
                    for (AuditableQuiz quiz : tema.getQuizzes()) {
                        if (reader.unit == AuditTarget.QUIZ && quiz.getId().equals(unitId)) {
                            return reader.fingerprint.apply(new Seen(level, topic, tema, quiz));
                        }
                    }
                }
            }
        }
        throw new AssertionError("the course has no unit " + unitId + " for " + judge);
    }

    /** What each evaluator of the judge asks about each unit as the course has it: «evaluator :: what it reads». */
    public List<String> keys(String judge, CourseEntity course, String... unitIds) {
        List<String> keys = new ArrayList<>();
        for (String unitId : unitIds) {
            String read = readOf(judge, course, unitId);
            for (String evaluator : judges.get(judge).evaluators) {
                keys.add(evaluator + " :: " + read);
            }
        }
        return keys;
    }

    /**
     * An earlier analysis of the whole course that paid these judges all they needed: their verdicts stay in the ledger;
     * the queries and runs the tests read start after it.
     */
    public void earlierAudit(CourseEntity course, String... judgeNames) {
        Map<String, EvaluationRunPolicy> policies = new LinkedHashMap<>();
        for (String judge : judgeNames) {
            policies.put(judge, new EvaluationRunPolicy(Integer.MAX_VALUE, false, null, null, null, 1));
        }
        real.runAudit(MAPPER.map(course), new AnalyzerRunSelection(List.of(judgeNames), policies));
        queries.clear();
    }

    /**
     * What the comparison sees of the judge in the whole course: its coverage when the course is analyzed with a budget
     * of 0, which only reuses what the ledger has.
     */
    public JudgeCoverageDiagnosis wholeCourseCoverage(CourseEntity course, String judge) {
        AuditReport report = real.runAudit(MAPPER.map(course), new AnalyzerRunSelection(List.of(judge),
                Map.of(judge, new EvaluationRunPolicy(0, false, null, null, null, 1))));
        for (JudgeCoverageDiagnosis coverage : ((DefaultCourseDiagnoses) report.getRoot().getDiagnoses())
                .getJudgeCoverages()) {
            if (coverage.getJudgeName().equals(judge)) {
                return coverage;
            }
        }
        throw new AssertionError("the analysis left no coverage of " + judge);
    }

    /** For each exercise of the course, the labels above it and the instruction of its topic, as a judge sees them. */
    public static Map<String, List<String>> ancestors(AuditableCourse course) {
        Map<String, List<String>> ancestors = new LinkedHashMap<>();
        for (AuditableMilestone level : course.getMilestones()) {
            for (AuditableTopic topic : level.getTopics()) {
                for (AuditableKnowledge tema : topic.getKnowledge()) {
                    for (AuditableQuiz quiz : tema.getQuizzes()) {
                        ancestors.put(quiz.getId(), List.of(level.getLabel(), topic.getLabel(), tema.getLabel(),
                                String.valueOf(tema.getInstructions())));
                    }
                }
            }
        }
        return ancestors;
    }

    /** The translations the course gives the exercise: none if the exercise is not in it. */
    public static List<String> translationsOf(AuditableCourse course, String quizId) {
        List<String> translations = new ArrayList<>();
        for (AuditableMilestone level : course.getMilestones()) {
            for (AuditableTopic topic : level.getTopics()) {
                for (AuditableKnowledge tema : topic.getKnowledge()) {
                    for (AuditableQuiz quiz : tema.getQuizzes()) {
                        if (quiz.getId().equals(quizId)) {
                            translations.add(quiz.getTranslation());
                        }
                    }
                }
            }
        }
        return translations;
    }

    /** The exercise of the course with that id. */
    public static QuizTemplateEntity exercise(CourseEntity course, String quizId) {
        for (MilestoneEntity level : course.getRoot().getMilestones()) {
            for (TopicEntity topic : level.getTopics()) {
                for (KnowledgeEntity tema : topic.getKnowledges()) {
                    for (QuizTemplateEntity quiz : tema.getQuizTemplates()) {
                        if (quiz.getId().equals(quizId)) {
                            return quiz;
                        }
                    }
                }
            }
        }
        throw new AssertionError("the course has no exercise " + quizId);
    }

    // -------------------------------------------------------------------------
    // The batch
    // -------------------------------------------------------------------------

    /**
     * An open batch of A1 with these analyzers and a pending proposal for each of the nodes -- an exercise, or a topic
     * for its instruction.
     */
    public static CorrectionBatch batch(List<String> analyzers, List<String> changedNodes) {
        CorrectionBatch batch = TandaFixture.batch(MesaFixture.BATCH);
        batch.setAnalyzers(new ArrayList<>(analyzers));
        batch.setSettings(new BatchSettings(2, QUERIES_AT_ONCE, 3));
        List<BatchProposalRef> proposals = new ArrayList<>();
        for (String node : changedNodes) {
            proposals.add(new BatchProposalRef(node, "prop-" + node));
        }
        batch.setQuizProposals(proposals);
        return batch;
    }

    // -------------------------------------------------------------------------
    // The courses
    // -------------------------------------------------------------------------

    /**
     * The course the cases start from: A1 with «Present Simple» -- «Afirmativas regulares» (4 exercises), «Contraste
     * inicial» (3 and a multiple choice) and «Contracciones» (3) -- and A2 with «Past Simple» (2).
     */
    public static CourseEntity startCourse() {
        return course(Set.of(), Map.of());
    }

    /**
     * The course after the batch: the given exercises written again -- their English, their translation and, in the
     * multiple choice, their options -- and the given topics with their new instruction, which their exercises take too.
     */
    public static CourseEntity composedCourse(Set<String> revised, Map<String, String> newInstructions) {
        return course(revised, newInstructions);
    }

    private static CourseEntity course(Set<String> revised, Map<String, String> newInstructions) {
        KnowledgeEntity afirmativas = tema(TEMA_AFIRMATIVAS, "t-present", "Afirmativas regulares",
                "Usa el presente simple en afirmativas.", newInstructions, List.of(
                        gap(TEMA_AFIRMATIVAS, AFIRM_1, "She", "works", "(work) in a bank.", "Ella trabaja en un banco.", revised),
                        gap(TEMA_AFIRMATIVAS, AFIRM_2, "They", "play", "(play) football on Sundays.",
                                "Ellos juegan al fútbol los domingos.", revised),
                        gap(TEMA_AFIRMATIVAS, AFIRM_3, "He", "reads", "(read) the news every morning.",
                                "Él lee las noticias cada mañana.", revised),
                        gap(TEMA_AFIRMATIVAS, AFIRM_4, "It", "comes", "(come) every year.", "Viene cada año.", revised)));
        KnowledgeEntity contraste = tema(TEMA_CONTRASTE, "t-present", "Contraste inicial",
                "Elige entre el presente simple y el continuo.", newInstructions, List.of(
                        gap(TEMA_CONTRASTE, CONTR_1, "I", "am reading", "(read) a book now.",
                                "Estoy leyendo un libro ahora.", revised),
                        gap(TEMA_CONTRASTE, CONTR_2, "She", "likes", "(like) chocolate.",
                                "A ella le gusta el chocolate.", revised),
                        gap(TEMA_CONTRASTE, CONTR_3, "They", "are playing", "(play) outside now.",
                                "Ellos están jugando afuera ahora.", revised),
                        choice(TEMA_CONTRASTE, CONTR_MC, revised.contains(CONTR_MC))));
        KnowledgeEntity contracciones = tema(TEMA_CONTRACCIONES, "t-present", "Contracciones",
                "Escribe la forma corta de lo que está entre paréntesis.", newInstructions, List.of(
                        gap(TEMA_CONTRACCIONES, CONTRAC_1, "You", "don't", "(do not) work at the bank.",
                                "No trabajas en el banco.", revised),
                        gap(TEMA_CONTRACCIONES, CONTRAC_2, "She", "'s not|isn't", "(is not) very happy.",
                                "Ella no está muy feliz.", revised),
                        gap(TEMA_CONTRACCIONES, CONTRAC_3, "They", "don't", "(do not) live here.",
                                "Ellos no viven aquí.", revised)));
        KnowledgeEntity pasado = tema(TEMA_PASADO, "t-past", "Past Simple", "Usa el pasado simple.", newInstructions, List.of(
                gap(TEMA_PASADO, PAST_1, "I", "went", "(go) to the cinema.", "Fui al cine.", revised),
                gap(TEMA_PASADO, PAST_2, "She", "visited", "(visit) her aunt.", "Ella visitó a su tía.", revised)));
        TopicEntity present = new TopicEntity("t-present", null, NodeKind.TOPIC, "Present Simple", null, "m-a1", null, null,
                0, null, new ArrayList<>(List.of(afirmativas, contraste, contracciones)));
        TopicEntity past = new TopicEntity("t-past", null, NodeKind.TOPIC, "Past Simple", null, "m-a2", null, null, 0, null,
                new ArrayList<>(List.of(pasado)));
        MilestoneEntity a1 = new MilestoneEntity("m-a1", null, NodeKind.MILESTONE, "A1", null, "root", null, 0, null,
                new ArrayList<>(List.of(present)));
        MilestoneEntity a2 = new MilestoneEntity("m-a2", null, NodeKind.MILESTONE, "A2", null, "root", null, 1, null,
                new ArrayList<>(List.of(past)));
        return new CourseEntity("curso", "Learney", null, new RootNodeEntity("root", null, NodeKind.ROOT, "Curso", null,
                new ArrayList<>(List.of(a1, a2))), null);
    }

    /**
     * A course of A1 with {@code temas} topics of {@code perTema} write-in exercises, each of them different from every
     * other; with {@code revised} every exercise is written again, so none is the one the start course has.
     */
    public static CourseEntity largeCourse(int temas, int perTema, boolean revised) {
        List<KnowledgeEntity> knowledges = new ArrayList<>();
        for (int t = 1; t <= temas; t++) {
            List<QuizTemplateEntity> quizzes = new ArrayList<>();
            for (int i = 1; i <= perTema; i++) {
                String id = largeExerciseId(t, i);
                QuizTemplateEntity quiz = MesaFixture.quiz(id, "Frase " + t + "." + i + (revised ? " revisada." : "."),
                        MesaFixture.text("Subject " + t + "." + i), MesaFixture.cloze("works"),
                        MesaFixture.text(revised ? "(work) harder." : "(work) hard."));
                quiz.setKnowledgeId(largeTemaId(t));
                quizzes.add(quiz);
            }
            knowledges.add(new KnowledgeEntity(largeTemaId(t), null, NodeKind.KNOWLEDGE, "Tema " + t, null, "t-present",
                    false, "Instrucción del tema " + t + ".", 0, null, quizzes, SentenceMode.FILL));
        }
        TopicEntity present = new TopicEntity("t-present", null, NodeKind.TOPIC, "Present Simple", null, "m-a1", null, null,
                0, null, new ArrayList<>(knowledges));
        MilestoneEntity a1 = new MilestoneEntity("m-a1", null, NodeKind.MILESTONE, "A1", null, "root", null, 0, null,
                new ArrayList<>(List.of(present)));
        return new CourseEntity("curso", "Learney", null, new RootNodeEntity("root", null, NodeKind.ROOT, "Curso", null,
                new ArrayList<>(List.of(a1))), null);
    }

    /** The ids of every exercise of {@link #largeCourse}. */
    public static List<String> largeExerciseIds(int temas, int perTema) {
        List<String> ids = new ArrayList<>();
        for (int t = 1; t <= temas; t++) {
            for (int i = 1; i <= perTema; i++) {
                ids.add(largeExerciseId(t, i));
            }
        }
        return ids;
    }

    private static String largeExerciseId(int tema, int index) {
        return String.format("e-%03d-%d", tema, index);
    }

    private static String largeTemaId(int tema) {
        return String.format("tema-%03d", tema);
    }

    /** A write-in exercise of a topic; written again, its English and its translation are others. */
    private static QuizTemplateEntity gap(String temaId, String id, String lead, String answer, String tail,
            String translation, Set<String> revised) {
        boolean again = revised.contains(id);
        QuizTemplateEntity quiz = MesaFixture.quiz(id, again ? translation.replace(".", " hoy.") : translation,
                MesaFixture.text(lead), MesaFixture.cloze(answer),
                MesaFixture.text(again ? tail.replace(".", " today.") : tail));
        quiz.setKnowledgeId(temaId);
        return quiz;
    }

    /** A multiple choice of a topic; written again, it asks about another person. */
    private static QuizTemplateEntity choice(String temaId, String id, boolean again) {
        QuizTemplateEntity quiz = again ? MesaFixture.choice(id, "a teacher? (She)", "is she", "am i", "are you")
                : MesaFixture.choice(id, "a child? (He)", "is he", "am i", "are you");
        quiz.setKnowledgeId(temaId);
        return quiz;
    }

    /** A topic; with a new instruction, its exercises carry the same one, as the data of the course has them. */
    private static KnowledgeEntity tema(String id, String parentId, String label, String instructions,
            Map<String, String> newInstructions, List<QuizTemplateEntity> quizzes) {
        String current = newInstructions.getOrDefault(id, instructions);
        if (newInstructions.containsKey(id)) {
            for (QuizTemplateEntity quiz : quizzes) {
                quiz.setInstructions(current);
            }
        }
        return new KnowledgeEntity(id, null, NodeKind.KNOWLEDGE, label, null, parentId, false, current, 0, null,
                new ArrayList<>(quizzes), SentenceMode.FILL);
    }

    // -------------------------------------------------------------------------
    // What each judge reads
    // -------------------------------------------------------------------------

    /** What an exercise says first, as the course shows it. */
    private static String statement(AuditableQuiz quiz) {
        return quiz.getSentences() != null && !quiz.getSentences().isEmpty() ? quiz.getSentences().get(0) : "";
    }

    private static String parts(AuditableQuiz quiz) {
        StringBuilder sb = new StringBuilder();
        for (SentencePartEntity part : quiz.getSentenceParts() != null ? quiz.getSentenceParts()
                : List.<SentencePartEntity>of()) {
            sb.append(' ').append(part.getKind() == SentencePartKind.CLOZE ? "___ " + part.getOptions() : part.getText());
        }
        return sb.toString().strip();
    }

    /** The level, the topic and the labels and instruction of the topic above what a judge reads. */
    private static String where(Seen seen) {
        return seen.level().getLabel() + " > " + seen.topic().getLabel() + " > " + seen.tema().getLabel() + " ("
                + seen.tema().getInstructions() + ")";
    }

    /** The topic whole: its level, its label and instruction, and every exercise it has, with its mini theory. */
    private static String wholeTopic(Seen seen) {
        StringBuilder sb = new StringBuilder(where(seen));
        for (AuditableQuiz quiz : seen.tema().getQuizzes()) {
            sb.append(" :: ").append(statement(quiz)).append(" || ").append(quiz.getTranslation()).append(" || ")
                    .append(quiz.getMiniTheory());
        }
        return sb.toString();
    }

    private static AnalyzerRuleCard rule(String id, AnalysisCost cost) {
        return new AnalyzerRuleCard(id, "lo que marca " + id, cost);
    }

    private static List<LocalToolCard> naturalnessTools() {
        return List.of(
                new LocalToolCard(new LocalToolStatus("Ollama", "llama3:8b", true, null, null),
                        "llama3:8b · native-yes-no-1", JudgeExamStatus.APPROVED, "aprobado el 30/9", false),
                new LocalToolCard(new LocalToolStatus("claude -p", "claude-cli", true, null, null), "claude-cli",
                        JudgeExamStatus.APPROVED, "aprobado el 30/9", true));
    }

    // -------------------------------------------------------------------------
    // The analyzers
    // -------------------------------------------------------------------------

    /** A card as the catalog asks it: complete, with the cost of its dearest rule. */
    private static AnalyzerDescriptor card(String name, String displayName, AuditTarget target,
            List<AnalyzerRuleCard> rules, List<LocalToolCard> tools) {
        AnalysisCost cost = AnalysisCost.INSTANT;
        for (AnalyzerRuleCard rule : rules) {
            if (rule.getCost().ordinal() > cost.ordinal()) {
                cost = rule.getCost();
            }
        }
        return new AnalyzerDescriptor(name, "un analizador", target, "¿?", "lo que lee", rules, "0 marcas",
                AnalyzerFamily.ERRORS, List.of(target), List.of(FindingResolution.PANEL), cost, displayName, tools, null);
    }

    /** What every analyzer of the bench does not do: it leaves no findings and touches nothing it does not read. */
    private abstract static class Silent implements ContentAnalyzer {
        private final String name;

        private final AuditTarget target;

        Silent(String name, AuditTarget target) {
            this.name = name;
            this.target = target;
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
        public List<FindingDraft> findingsAt(AuditNode node) {
            return List.of();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public AuditTarget getTarget() {
            return target;
        }

        @Override
        public String getDescription() {
            return "un analizador";
        }
    }

    /** An analyzer of instant rules, as hint-coherence: no model, no ledger, no coverage. */
    private final class InstantRules implements AnalyzerProvider {
        private final String name;

        private final String displayName;

        private final String rule;

        InstantRules(String name, String displayName, String rule) {
            this.name = name;
            this.displayName = displayName;
            this.rule = rule;
        }

        @Override
        public String analyzerName() {
            return name;
        }

        @Override
        public AnalyzerDescriptor describe() {
            return card(name, displayName, AuditTarget.QUIZ, List.of(rule(rule, AnalysisCost.INSTANT)), List.of());
        }

        @Override
        public ContentAnalyzer create(EvaluationRunPolicy policy) {
            return new Silent(name, AuditTarget.QUIZ) {
                @Override
                public Void onQuiz(AuditNode node) {
                    node.getScores().put(name, 1.0);
                    return null;
                }
            };
        }

        @Override
        public Optional<AnalyzerPlanBinding> planBinding() {
            return Optional.empty();
        }

        @Override
        public Optional<SelfDescribingConfig> config() {
            return Optional.empty();
        }
    }

    /**
     * A local tool without a judge's coverage, as grammar: it reads every exercise it is given, publishes the run of its
     * tool with its exam, and leaves a score on what it evaluated or marks the exercise «not evaluated by» it when its
     * exam is not approved. Its card brings the tool, so it is a judge by the card.
     */
    private final class LanguageTool implements AnalyzerProvider {
        private final String name;

        private final String displayName;

        LanguageTool(String name, String displayName) {
            this.name = name;
            this.displayName = displayName;
        }

        @Override
        public String analyzerName() {
            return name;
        }

        @Override
        public AnalyzerDescriptor describe() {
            return card(name, displayName, AuditTarget.QUIZ, List.of(rule("english-error", AnalysisCost.INSTANT)),
                    List.of(new LocalToolCard(new LocalToolStatus("LanguageTool", "6.8", true, null, null),
                            "LanguageTool 6.8 · filtro-1", JudgeExamStatus.APPROVED, "aprobado el 30/9", false)));
        }

        @Override
        public ContentAnalyzer create(EvaluationRunPolicy policy) {
            Exam exam = exam(name);
            return new Silent(name, AuditTarget.QUIZ) {
                @Override
                public Void onQuiz(AuditNode node) {
                    if (exam.approved()) {
                        node.getScores().put(name, 1.0);
                    } else {
                        node.getUnevaluatedBy().add(name);
                    }
                    return null;
                }

                @Override
                public Void onCourseComplete(AuditNode root) {
                    ((DefaultCourseDiagnoses) root.getDiagnoses()).addLocalToolRun(new LocalToolRunDiagnosis(name,
                            new LocalToolStatus("LanguageTool", "6.8", true, null, null), "LanguageTool 6.8 · filtro-1",
                            exam.status(), exam.approved(), exam.approved() ? null : exam.reason(), false));
                    return null;
                }
            };
        }

        @Override
        public Optional<AnalyzerPlanBinding> planBinding() {
            return Optional.empty();
        }

        @Override
        public Optional<SelfDescribingConfig> config() {
            return Optional.empty();
        }
    }

    /**
     * A judge of the catalog: what it reads, the evaluators it asks, the ledger and the exam it stands on. In its run it
     * reuses what the ledger has, consults what it does not within the budget of its policy and only when its exam is
     * approved, stops asking after the failure of its service, and publishes its coverage on the root. Naturalness
     * publishes besides the run of its local tool; with Ollama, a run without a policy asks without limit, as a model of
     * this machine does.
     */
    private final class Judge implements AnalyzerProvider {
        private final String name;

        private final String displayName;

        private final AuditTarget unit;

        private final List<String> evaluators;

        private final List<AnalyzerRuleCard> rules;

        private final List<LocalToolCard> tools;

        private final boolean local;

        private final Predicate<AuditableQuiz> reads;

        private final Function<Seen, String> fingerprint;

        Judge(String name, String displayName, AuditTarget unit, List<String> evaluators, List<AnalyzerRuleCard> rules,
                List<LocalToolCard> tools, boolean local, Predicate<AuditableQuiz> reads,
                Function<Seen, String> fingerprint) {
            this.name = name;
            this.displayName = displayName;
            this.unit = unit;
            this.evaluators = evaluators;
            this.rules = rules;
            this.tools = tools;
            this.local = local;
            this.reads = reads;
            this.fingerprint = fingerprint;
        }

        @Override
        public String analyzerName() {
            return name;
        }

        @Override
        public AnalyzerDescriptor describe() {
            return card(name, displayName, unit, rules, tools);
        }

        @Override
        public ContentAnalyzer create(EvaluationRunPolicy policy) {
            return new Run(policy);
        }

        @Override
        public Optional<AnalyzerPlanBinding> planBinding() {
            return Optional.empty();
        }

        @Override
        public Optional<SelfDescribingConfig> config() {
            return Optional.empty();
        }

        private final class Run extends Silent {
            private int budget;

            private final int maxConcurrent;

            private int reached;

            private int evaluated;

            private int quizzesReached;

            private int quizzesEvaluated;

            private boolean down;

            private String unavailableReason;

            /** By evaluator: queries paid, verdicts reused, still pending, failed. */
            private final Map<String, int[]> tallies = new LinkedHashMap<>();

            Run(EvaluationRunPolicy policy) {
                super(name, unit);
                this.budget = policy != null ? Math.max(0, policy.getMaxNewEvaluations())
                        : (local && ollama ? Integer.MAX_VALUE : 0);
                this.maxConcurrent = policy != null && policy.getMaxConcurrentEvaluations() != null
                        ? policy.getMaxConcurrentEvaluations() : 1;
                for (String evaluator : evaluators) {
                    tallies.put(evaluator, new int[4]);
                }
            }

            @Override
            public Void onQuiz(AuditNode node) {
                if (unit != AuditTarget.QUIZ || !(node.getEntity() instanceof AuditableQuiz quiz) || !reads.test(quiz)) {
                    return null;
                }
                reached++;
                boolean judged = consult(quiz.getId(), Seen.of(node));
                if (abstainOn.getOrDefault(name, Set.of()).contains(quiz.getId())) { // as reused from the ledger
                    abstain(node);
                } else {
                    mark(node, judged);
                }
                return null;
            }

            /** A verdict saved without a score: the judge leaves it for a review, and its coverage counts it pending. */
            private void abstain(AuditNode node) {
                if (node.getMetadata() == null) {
                    node.setMetadata(new LinkedHashMap<>());
                }
                node.getMetadata().put(name + "InputContract",
                        com.learney.contentaudit.auditdomain.englishquality.EnglishQualitySubjectBuilder.CONTRACT);
                node.getMetadata().put(name + "Version", "v3");
                node.getMetadata().put("verdictVersions", Map.of(name, "v3"));
                node.getMetadata().put(name + "Verdict", Map.of("assessments", List.of(Map.of("status", "REVIEW"))));
                node.getUnevaluatedBy().add(name);
            }

            @Override
            public Void onKnowledge(AuditNode node) {
                if (unit != AuditTarget.KNOWLEDGE) {
                    return null;
                }
                AuditableKnowledge tema = (AuditableKnowledge) node.getEntity();
                reached++;
                quizzesReached += tema.getQuizzes().size();
                if (consult(tema.getId(), Seen.of(node))) {
                    quizzesEvaluated += tema.getQuizzes().size();
                    mark(node, true);
                } else {
                    mark(node, false);
                }
                return null;
            }

            private void mark(AuditNode node, boolean judged) {
                if (judged) {
                    evaluated++;
                    node.getScores().put(name, 1.0);
                } else {
                    node.getUnevaluatedBy().add(name);
                }
            }

            /** Every evaluator gives its verdict on the unit: from the ledger, or asked while it may; false if one is left. */
            private boolean consult(String unitId, Seen seen) {
                String read = fingerprint.apply(seen);
                boolean complete = true;
                for (String evaluator : evaluators) {
                    int[] tally = tallies.get(evaluator);
                    String key = evaluator + " :: " + read;
                    if (book.containsKey(key)) {
                        tally[1]++;
                    } else if (!down && budget > 0 && exam(name).approved()) {
                        budget--;
                        if (servicesDown.containsKey(name)) {
                            down = true;
                            unavailableReason = servicesDown.get(name);
                            tally[3]++;
                            queries.add(new Query(name, evaluator, unitId, read, false));
                            complete = false;
                        } else {
                            book.put(key, Boolean.FALSE);
                            queries.add(new Query(name, evaluator, unitId, read, true));
                            tally[0]++;
                        }
                    } else {
                        tally[2]++;
                        complete = false;
                    }
                }
                return complete;
            }

            @Override
            public Void onCourseComplete(AuditNode root) {
                List<EvaluatorCoverage> coverages = new ArrayList<>();
                int missing = 0;
                for (Map.Entry<String, int[]> entry : tallies.entrySet()) {
                    int[] t = entry.getValue();
                    missing += t[2] + t[3];
                    coverages.add(new EvaluatorCoverage(entry.getKey(),
                            new EvaluationCoverage(reached, t[0] + t[1], t[0], t[1], t[2], t[3]), "v3"));
                }
                Exam exam = exam(name);
                DefaultCourseDiagnoses diagnoses = (DefaultCourseDiagnoses) root.getDiagnoses();
                diagnoses.addJudgeCoverage(new JudgeCoverageDiagnosis(name, unit, reached, evaluated, reached - evaluated,
                        coverages, new JudgeExamStanding(name, "v3", exam.status(), exam.approved(), exam.reason()),
                        new ArrayList<>(versionsSeen.getOrDefault(name, List.of())), unavailableReason,
                        new JudgeCostEstimate(reached - evaluated, missing, 0, false, 0, maxConcurrent, 0),
                        new ArrayList<>(),
                        unit == AuditTarget.QUIZ ? new JudgeQuizTally(reached, evaluated, reached - evaluated)
                                : new JudgeQuizTally(quizzesReached, quizzesEvaluated, quizzesReached - quizzesEvaluated)));
                if (local) {
                    diagnoses.addLocalToolRun(ollama
                            ? new LocalToolRunDiagnosis(name, new LocalToolStatus("Ollama", "llama3:8b", true, null, null),
                                    "llama3:8b · native-yes-no-1", exam.status(), exam.approved(),
                                    exam.approved() ? null : exam.reason(), false)
                            : new LocalToolRunDiagnosis(name, new LocalToolStatus("Ollama", null, false,
                                    "no responde en el puerto 11434", "ollama serve"), null, null, false,
                                    "sin herramienta: Ollama no está disponible; se instala con: ollama serve", true));
                }
                return null;
            }
        }
    }
}
