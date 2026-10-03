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
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.MilestoneEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.coursedomain.TopicEntity;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationCoverage;
import com.learney.contentaudit.revisiondomain.panelengine.MeterBench;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * The judges of the closing of a batch (FEAT-CJUEZ) without a model: the real engine, catalog, collector and calculator
 * over three judges that keep their verdicts in a book by what they read -- each one asks only what the book does not
 * have, within the budget its run gives it, and says so in its coverage, as the judges of the catalog do. Written in the
 * style of {@code MeterBench}, which the tests of the table use and which this one leaves alone.
 *
 * <p>The three judges, in the order of the catalog: {@code quiz-instruction} (by exercise, one evaluator),
 * {@code translation-sense} (by exercise, two evaluators -- one per direction) and {@code mini-theory} (by topic, one
 * evaluator). A judge in {@link #withoutApproval} has no exam approved: it opens with nothing to spend, though the run
 * gives it a budget, and its standing says why; it still reuses what the book holds.
 */
final class CjuezJourneyBench {

    static final String QUIZ_INSTRUCTION = "quiz-instruction";

    static final String TRANSLATION_SENSE = "translation-sense";

    static final String MINI_THEORY = "mini-theory";

    /** The judges, in the order of the catalog. */
    static final List<String> JUDGES = List.of(QUIZ_INSTRUCTION, TRANSLATION_SENSE, MINI_THEORY);

    static final String NOT_TAKEN_REASON = "sin examen: la versión vigente del juez no rindió el examen fijo";

    static final String FAILED_REASON = "reprobada: la versión vigente del juez reprobó el examen fijo";

    /** A query paid to a judge: which of its evaluators asked it, about which exercise or topic, and what it read. */
    record Consultation(String judge, String evaluator, String unitId, String read) {
    }

    /** The book of verdicts: «evaluator: what it read», for every verdict recorded. */
    final Set<String> book = ConcurrentHashMap.newKeySet();

    /** Every query paid, in the order they were paid. */
    final List<Consultation> consultations = Collections.synchronizedList(new ArrayList<>());

    /** Every selection the engine of the batch got, with its policies. */
    final List<AnalyzerRunSelection> selections = Collections.synchronizedList(new ArrayList<>());

    /** The judges whose version has no approved exam, with how its exam stands. */
    final Map<String, JudgeExamStatus> withoutApproval = new ConcurrentHashMap<>();

    private final AnalyzerCatalog catalog;

    private final AuditEngine real;

    private final AuditEngine engine;

    private final CourseMapper mapper = mapper();

    CjuezJourneyBench() {
        List<AnalyzerProvider> providers = new ArrayList<>();
        providers.add(new Judge(QUIZ_INSTRUCTION, "Consigna que no se cumple", AuditTarget.QUIZ,
                List.of("quiz-instruction"), "instruction-fits",
                node -> {
                    AuditableQuiz quiz = (AuditableQuiz) node.getEntity();
                    return above(node) + " >> " + quiz.getInstructions() + " || " + parts(quiz);
                }));
        providers.add(new Judge(TRANSLATION_SENSE, "Traducción que no corresponde", AuditTarget.QUIZ,
                List.of("translation-sense-from-english", "translation-sense-from-translation"), "same-meaning",
                node -> {
                    AuditableQuiz quiz = (AuditableQuiz) node.getEntity();
                    return above(node) + " >> " + parts(quiz) + " || " + quiz.getTranslation();
                }));
        providers.add(new Judge(MINI_THEORY, "Miniteoría con error", AuditTarget.KNOWLEDGE, List.of("mini-theory"),
                "theory-fits", CjuezJourneyBench::topic));
        this.catalog = new DefaultAnalyzerCatalog(providers);
        this.real = new IAuditEngine(new IScoreAggregator(), catalog, new DefaultFindingCollector(),
                new DefaultContextNumbersCalculator());
        this.engine = new AuditEngine() {
            @Override
            public AuditReport runAudit(AuditableCourse course) {
                return real.runAudit(course);
            }

            @Override
            public AuditReport runAudit(AuditableCourse course, AnalyzerRunSelection selection) {
                selections.add(selection);
                return real.runAudit(course, selection);
            }
        };
    }

    /** The engine the batch gets: the real one, which keeps every selection it is given. */
    AuditEngine engine() {
        return engine;
    }

    AnalyzerCatalog catalog() {
        return catalog;
    }

    CourseMapper courseMapper() {
        return mapper;
    }

    // -------------------------------------------------------------------------
    // The book and the bank
    // -------------------------------------------------------------------------

    /**
     * The book as an earlier analysis left it: the given judges, with all they need, over what the course says today.
     * What this run asks is not a query of the test: the bank starts empty.
     */
    void judgeCourse(CourseEntity course, String... judges) {
        Map<String, EvaluationRunPolicy> policies = new LinkedHashMap<>();
        for (String judge : judges) {
            policies.put(judge, new EvaluationRunPolicy(1_000_000, false, null, null, null, 1));
        }
        real.runAudit(mapper.map(course), new AnalyzerRunSelection(List.of(judges), policies));
        consultations.clear();
    }

    /** What the judge was asked, in order. */
    List<Consultation> consultationsOf(String judge) {
        List<Consultation> asked = new ArrayList<>();
        synchronized (consultations) {
            for (Consultation consultation : consultations) {
                if (consultation.judge().equals(judge)) {
                    asked.add(consultation);
                }
            }
        }
        return asked;
    }

    /** The exercises or topics the judge was asked about. */
    Set<String> unitsOf(String judge) {
        Set<String> units = new LinkedHashSet<>();
        for (Consultation consultation : consultationsOf(judge)) {
            units.add(consultation.unitId());
        }
        return units;
    }

    /**
     * The course measured with every judge at 0: each one only reuses what its book holds, as the projection of the batch
     * measures it. It never goes through the engine of the batch, so the selections that one keeps are only the ones the
     * batch asked.
     */
    AuditReport measure(CourseEntity course) {
        Map<String, EvaluationRunPolicy> policies = new LinkedHashMap<>();
        for (String judge : JUDGES) {
            policies.put(judge, new EvaluationRunPolicy(0, false, null, null, null, 1));
        }
        return real.runAudit(mapper.map(course), new AnalyzerRunSelection(JUDGES, policies));
    }

    // -------------------------------------------------------------------------
    // The judges
    // -------------------------------------------------------------------------

    /** What a judge by exercise reads of a gap exercise: its statement and its gap with the options. */
    private static String parts(AuditableQuiz quiz) {
        StringBuilder sb = new StringBuilder();
        for (SentencePartEntity part : quiz.getSentenceParts() != null ? quiz.getSentenceParts()
                : List.<SentencePartEntity>of()) {
            sb.append(' ').append(part.getKind() == SentencePartKind.CLOZE ? "___ " + part.getOptions() : part.getText());
        }
        return sb.toString().strip();
    }

    /**
     * What every judge reads above the exercise: the label of its level, of its topic and of the knowledge it belongs
     * to, and the instruction of that knowledge -- so a cut of the course that keeps the same ancestors reads the same
     * as the course, and one that loses a label does not.
     */
    private static String above(AuditNode node) {
        AuditNode knowledgeNode = node.getTarget() == AuditTarget.KNOWLEDGE ? node : node.getParent();
        AuditableKnowledge knowledge = (AuditableKnowledge) knowledgeNode.getEntity();
        StringBuilder sb = new StringBuilder();
        knowledgeNode.ancestor(AuditTarget.MILESTONE).ifPresent(level -> sb.append(level.getEntity().getLabel())
                .append(" | "));
        sb.append(knowledge.getTopicName()).append(" | ").append(knowledge.getLabel()).append(" | ")
                .append(knowledge.getInstructions());
        return sb.toString();
    }

    /** What the judge of mini theories reads: the whole topic, with what is above it, and every exercise with its mini theory. */
    private static String topic(AuditNode knowledgeNode) {
        StringBuilder sb = new StringBuilder(above(knowledgeNode));
        for (AuditNode quizNode : knowledgeNode.getChildren()) {
            AuditableQuiz quiz = (AuditableQuiz) quizNode.getEntity();
            sb.append(" ## ").append(parts(quiz)).append(" || ").append(quiz.getMiniTheory());
        }
        return sb.toString();
    }

    /** A judge of the catalog: what it reads, the evaluators that ask, the book and the coverage it leaves. */
    private final class Judge implements AnalyzerProvider {
        private final String name;

        private final String displayName;

        private final AuditTarget unit;

        private final List<String> evaluators;

        private final String rule;

        private final Function<AuditNode, String> reads;

        Judge(String name, String displayName, AuditTarget unit, List<String> evaluators, String rule,
                Function<AuditNode, String> reads) {
            this.name = name;
            this.displayName = displayName;
            this.unit = unit;
            this.evaluators = evaluators;
            this.rule = rule;
            this.reads = reads;
        }

        @Override
        public String analyzerName() {
            return name;
        }

        /** The card as the real ones: of the family of errors, a paid model, with its Spanish name. */
        @Override
        public AnalyzerDescriptor describe() {
            return new AnalyzerDescriptor(name, "un juez", AuditTarget.QUIZ, "¿?", "el ejercicio",
                    List.of(new AnalyzerRuleCard(rule, "lo que marca", AnalysisCost.PAID_MODEL)), "0 ejercicios",
                    AnalyzerFamily.ERRORS, List.of(AuditTarget.QUIZ), List.of(FindingResolution.PANEL),
                    AnalysisCost.PAID_MODEL, displayName, List.of(), null);
        }

        @Override
        public ContentAnalyzer create(EvaluationRunPolicy policy) {
            return new Run(policy != null ? policy.getMaxNewEvaluations() : 0);
        }

        @Override
        public Optional<AnalyzerPlanBinding> planBinding() {
            return Optional.empty();
        }

        @Override
        public Optional<SelfDescribingConfig> config() {
            return Optional.empty();
        }

        private final class Run implements ContentAnalyzer {
            private int budget;

            private int reached;

            private int evaluated;

            private int quizzesReached;

            private int quizzesEvaluated;

            /** Per evaluator: asked in this run, reused from the book, still pending. */
            private final Map<String, int[]> byEvaluator = new LinkedHashMap<>();

            Run(int budget) {
                this.budget = budget;
            }

            @Override
            public Void onQuiz(AuditNode node) {
                if (unit != AuditTarget.QUIZ || !(node.getEntity() instanceof AuditableQuiz quiz)
                        || quiz.getMultipleChoice() != null) {
                    return null;
                }
                judge(quiz.getId(), reads.apply(node), List.of(node));
                return null;
            }

            @Override
            public Void onKnowledge(AuditNode node) {
                if (unit != AuditTarget.KNOWLEDGE) {
                    return null;
                }
                judge(node.getEntity().getId(), reads.apply(node), node.getChildren());
                return null;
            }

            /** Each evaluator reuses the verdict of the book, or asks while the run has budget and its exam allows. */
            private void judge(String unitId, String read, List<AuditNode> exercises) {
                reached++;
                quizzesReached += exercises.size();
                boolean complete = true;
                for (String evaluator : evaluators) {
                    int[] tally = byEvaluator.computeIfAbsent(evaluator, e -> new int[3]);
                    String key = evaluator + ": " + read;
                    if (book.contains(key)) {
                        tally[1]++;
                    } else if (budget > 0 && !withoutApproval.containsKey(name)) {
                        budget--;
                        book.add(key);
                        consultations.add(new Consultation(name, evaluator, unitId, read));
                        tally[0]++;
                    } else {
                        tally[2]++;
                        complete = false;
                    }
                }
                for (AuditNode exercise : exercises) {
                    if (complete) {
                        exercise.getScores().put(name, 1.0);
                    } else {
                        exercise.getUnevaluatedBy().add(name);
                    }
                }
                if (complete) {
                    evaluated++;
                    quizzesEvaluated += exercises.size();
                }
            }

            @Override
            public Void onCourseComplete(AuditNode root) {
                List<EvaluatorCoverage> coverages = new ArrayList<>();
                int pending = 0;
                for (String evaluator : evaluators) {
                    int[] t = byEvaluator.getOrDefault(evaluator, new int[3]);
                    pending += t[2];
                    coverages.add(new EvaluatorCoverage(evaluator,
                            new EvaluationCoverage(reached, t[0] + t[1], t[0], t[1], t[2], 0), "v1"));
                }
                JudgeExamStatus status = withoutApproval.get(name);
                JudgeExamStanding standing = status == null
                        ? new JudgeExamStanding(name, "v1", JudgeExamStatus.APPROVED, true, "aprobado el 30/9")
                        : new JudgeExamStanding(name, "v1", status, false,
                                status == JudgeExamStatus.FAILED ? FAILED_REASON : NOT_TAKEN_REASON);
                ((DefaultCourseDiagnoses) root.getDiagnoses()).addJudgeCoverage(new JudgeCoverageDiagnosis(name, unit,
                        reached, evaluated, reached - evaluated, coverages, standing, new ArrayList<>(), null,
                        new JudgeCostEstimate(reached - evaluated, pending, 0, false, 0, 1, 0), new ArrayList<>(),
                        new JudgeQuizTally(quizzesReached, quizzesEvaluated, quizzesReached - quizzesEvaluated)));
                return null;
            }

            @Override
            public List<FindingDraft> findingsAt(AuditNode node) {
                return List.of();
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
            public String getName() {
                return name;
            }

            @Override
            public AuditTarget getTarget() {
                return AuditTarget.QUIZ;
            }

            @Override
            public String getDescription() {
                return "un juez";
            }
        }
    }

    // -------------------------------------------------------------------------
    // The course as the engine reads it, without spaCy
    // -------------------------------------------------------------------------

    /** The course mapped as the real mapper does, with the tokens of a tokenizer that splits on spaces. */
    private static CourseMapper mapper() {
        return course -> {
            List<AuditableMilestone> milestones = new ArrayList<>();
            for (MilestoneEntity milestone : course.getRoot().getMilestones()) {
                List<AuditableTopic> topics = new ArrayList<>();
                for (TopicEntity topic : milestone.getTopics()) {
                    List<AuditableKnowledge> knowledges = new ArrayList<>();
                    for (KnowledgeEntity knowledge : topic.getKnowledges()) {
                        List<AuditableQuiz> quizzes = new ArrayList<>();
                        for (QuizTemplateEntity quiz : knowledge.getQuizTemplates()) {
                            List<String> sentences = quiz.getSentences() != null ? quiz.getSentences()
                                    : DefaultQuizSentenceConverter.create().toPlainSentences(quiz.getForm(),
                                            knowledge.getSentenceMode());
                            quizzes.add(new AuditableQuiz(MeterBench.tokens(sentences.isEmpty() ? "" : sentences.get(0)),
                                    quiz.getId(), quiz.getTitle(), quiz.getCode(), quiz.getTranslation(), sentences, null,
                                    quiz.getInstructions(), quiz.getForm().getSentenceParts(),
                                    quiz.getForm().getMultipleChoice(), quiz.getMiniTheory(), quiz.getAnswerAudioUrl()));
                        }
                        knowledges.add(new AuditableKnowledge(quizzes, knowledge.getLabel(), knowledge.getInstructions(),
                                true, knowledge.getId(), knowledge.getLabel(), knowledge.getCode(),
                                knowledge.getSentenceMode(), topic.getLabel()));
                    }
                    topics.add(new AuditableTopic(knowledges, topic.getId(), topic.getLabel(), topic.getCode()));
                }
                milestones.add(new AuditableMilestone(topics, milestone.getId(), milestone.getLabel(), milestone.getCode()));
            }
            return new AuditableCourse(milestones);
        };
    }
}
