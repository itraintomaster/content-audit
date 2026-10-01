package com.learney.contentaudit.revisiondomain.panelengine;

import com.learney.contentaudit.auditdomain.AnalyzerDescriptor;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerPlanBinding;
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
import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.auditdomain.ContentAnalyzer;
import com.learney.contentaudit.auditdomain.CorrectorMatcher;
import com.learney.contentaudit.auditdomain.CourseMapper;
import com.learney.contentaudit.auditdomain.DefaultCourseDiagnoses;
import com.learney.contentaudit.auditdomain.EvaluationRunPolicy;
import com.learney.contentaudit.auditdomain.IAuditEngine;
import com.learney.contentaudit.auditdomain.IScoreAggregator;
import com.learney.contentaudit.auditdomain.IrregularFormsCatalog;
import com.learney.contentaudit.auditdomain.NlpToken;
import com.learney.contentaudit.auditdomain.NlpTokenizer;
import com.learney.contentaudit.auditdomain.SelfDescribingConfig;
import com.learney.contentaudit.auditdomain.SentenceLengthConfig;
import com.learney.contentaudit.auditdomain.TargetRange;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerFamily;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRuleCard;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.choicealternative.ChoiceAlternativeJudgeRun;
import com.learney.contentaudit.auditdomain.choicealternative.ChoiceAlternativeJudgeRunFactory;
import com.learney.contentaudit.auditdomain.choicealternative.ChoiceAlternativeJudgment;
import com.learney.contentaudit.auditdomain.choicealternative.ChoiceAlternativeJudgmentStatus;
import com.learney.contentaudit.auditdomain.correctormatch.GapContext;
import com.learney.contentaudit.auditdomain.finding.AnalysisCost;
import com.learney.contentaudit.auditdomain.finding.EvidencePart;
import com.learney.contentaudit.auditdomain.finding.FindingDraft;
import com.learney.contentaudit.auditdomain.finding.FindingEvidence;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.auditdomain.findingengine.DefaultAnalyzerCatalog;
import com.learney.contentaudit.auditdomain.findingengine.DefaultContextNumbersCalculator;
import com.learney.contentaudit.auditdomain.findingengine.DefaultFindingCollector;
import com.learney.contentaudit.auditdomain.hintcoherence.HintCoherenceAnalyzerProvider;
import com.learney.contentaudit.auditdomain.judging.EvaluatorCoverage;
import com.learney.contentaudit.auditdomain.judging.JudgeCostEstimate;
import com.learney.contentaudit.auditdomain.judging.JudgeCoverageDiagnosis;
import com.learney.contentaudit.auditdomain.judging.JudgeExamStanding;
import com.learney.contentaudit.auditdomain.judging.JudgeExamStatus;
import com.learney.contentaudit.auditdomain.judging.JudgeQuizTally;
import com.learney.contentaudit.auditdomain.lexicalflags.MisplacedWordFlag;
import com.learney.contentaudit.auditdomain.lexicalflags.SentenceLexicalEvaluator;
import com.learney.contentaudit.auditdomain.lexicalflags.SentenceLexicalFlags;
import com.learney.contentaudit.auditdomain.multiplechoicevalidity.MultipleChoiceValidityAnalyzerProvider;
import com.learney.contentaudit.auditdomain.offlevelwords.TaughtWords;
import com.learney.contentaudit.auditdomain.offlevelwords.TopicTeachingResolver;
import com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.MilestoneEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.TopicEntity;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationCoverage;
import com.learney.contentaudit.auditdomain.datacoherence.DefaultSpokenTextBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * The measurement of {@code analyze} without spaCy nor a model: the real engine, catalog, collector and calculator,
 * the real hint-coherence and multiple-choice-validity, and judges that keep their verdicts in a book by the
 * fingerprint of what they read -- each one consults only what the book does not have, within the budget its run
 * gives it, and says it in its coverage, as the judges of the catalog do.
 */
public final class MeterBench {

    /** What a judge reads of an exercise as the course shows it. */
    public static String statement(AuditableQuiz quiz) {
        return quiz.getSentences() != null && !quiz.getSentences().isEmpty() ? quiz.getSentences().get(0) : "";
    }

    /** The book of the verdicts: by evaluator and what it read. */
    public final Map<String, Boolean> book = new ConcurrentHashMap<>();

    /** Every consultation: «evaluator: what it read». */
    public final List<String> consultations = Collections.synchronizedList(new ArrayList<>());

    /** Every selection the engine got. */
    public final List<AnalyzerRunSelection> selections = Collections.synchronizedList(new ArrayList<>());

    public AuditReport lastReport;

    /** A text on which the judge from the English says no and the one from the translation says yes. */
    public volatile String disagreeOn;

    /** The judges whose version has not passed its exam. */
    public final Set<String> notApproved = ConcurrentHashMap.newKeySet();

    /** The lemmas that are red at A1, with their level. */
    public final Map<String, CefrLevel> levels = new LinkedHashMap<>(Map.of("grandmother", CefrLevel.A2,
            "grandfather", CefrLevel.A2, "regular", CefrLevel.A2));

    private final List<AnalyzerProvider> providers = new ArrayList<>();

    private final AuditEngine engine;

    public MeterBench() {
        DefaultQuizStudentViewRenderer renderer = new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create());
        providers.add(new HintCoherenceAnalyzerProvider(irregularForms(), renderer));
        providers.add(new MultipleChoiceValidityAnalyzerProvider(exactCorrector(), new ChoiceJudge(), renderer));
        providers.add(new Judge("translation-sense", List.of("translation-sense-from-english",
                "translation-sense-from-translation"), "same-meaning",
                quiz -> statement(quiz) + " || " + quiz.getTranslation(),
                (evaluator, read) -> read.contains("Me gustan mucho")
                        || disagreeOn != null && read.contains(disagreeOn) && evaluator.endsWith("english")));
        providers.add(new Judge("single-answer", List.of("single-answer"), "different-valid-answer",
                quiz -> quiz.getInstructions() + " || " + parts(quiz),
                (evaluator, read) -> read.contains("He ___")));
        providers.add(new Judge("quiz-instruction", List.of("quiz-instruction"), "instruction-fits",
                quiz -> quiz.getInstructions() + " || " + parts(quiz),
                (evaluator, read) -> false));
        AuditEngine real = new IAuditEngine(new IScoreAggregator(), new DefaultAnalyzerCatalog(providers),
                new DefaultFindingCollector(), new DefaultContextNumbersCalculator());
        this.engine = new AuditEngine() {
            @Override
            public AuditReport runAudit(AuditableCourse course) {
                return lastReport = real.runAudit(course);
            }

            @Override
            public AuditReport runAudit(AuditableCourse course, AnalyzerRunSelection selection) {
                selections.add(selection);
                return lastReport = real.runAudit(course, selection);
            }
        };
    }

    public static final List<String> INSTANT = List.of("hint-coherence", "multiple-choice-validity");

    public static final List<String> ALL = List.of("hint-coherence", "multiple-choice-validity", "quiz-instruction",
            "translation-sense", "single-answer");

    public AuditEngine engine() {
        return engine;
    }

    /** What the factory of the panel needs, with this bench as the measurement and the given proposer. */
    public com.learney.contentaudit.revisiondomain.panel.PanelRunnerConfig config(
            com.learney.contentaudit.revisiondomain.panel.PanelProposer proposer) {
        return new com.learney.contentaudit.revisiondomain.panel.PanelRunnerConfig(proposer, null, MesaFixture.corrections(),
                engine, mapper(), new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create()), tokenizer(),
                lexicalEvaluator(), teaching(), new DefaultSpokenTextBuilder(), lengthConfig());
    }

    public CatalogTopicMeter meter() {
        return new CatalogTopicMeter(engine, mapper(), tokenizer(), lexicalEvaluator(), teaching(),
                new DefaultSpokenTextBuilder(), lengthConfig());
    }

    /** The whole course mapped as analyze maps it, for the measurement of the course. */
    public AuditReport analyze(CourseEntity course, List<String> analyzers) {
        Map<String, EvaluationRunPolicy> policies = new LinkedHashMap<>();
        for (String name : analyzers) {
            policies.put(name, new EvaluationRunPolicy(0, false, null, null, null, 1));
        }
        return engine.runAudit(mapper().map(course), new AnalyzerRunSelection(analyzers, policies));
    }

    public static CourseEntity course(String milestoneId, CefrLevel level, KnowledgeEntity... topics) {
        TopicEntity topic = new TopicEntity("t-present", null, com.learney.contentaudit.coursedomain.NodeKind.TOPIC,
                "Present Simple", null, milestoneId, null, null, 0, null, new ArrayList<>(List.of(topics)));
        MilestoneEntity milestone = new MilestoneEntity(milestoneId, null,
                com.learney.contentaudit.coursedomain.NodeKind.MILESTONE, level.name(), null, "root", null, 0, null,
                new ArrayList<>(List.of(topic)));
        return new CourseEntity("curso", "curso", null, new com.learney.contentaudit.coursedomain.RootNodeEntity("root",
                null, com.learney.contentaudit.coursedomain.NodeKind.ROOT, "Curso", null,
                new ArrayList<>(List.of(milestone))), null);
    }

    // -------------------------------------------------------------------------
    // The judges
    // -------------------------------------------------------------------------

    private static String parts(AuditableQuiz quiz) {
        StringBuilder sb = new StringBuilder();
        for (var part : quiz.getSentenceParts() != null ? quiz.getSentenceParts()
                : List.<com.learney.contentaudit.coursedomain.SentencePartEntity>of()) {
            sb.append(' ').append(part.getKind() == com.learney.contentaudit.coursedomain.SentencePartKind.CLOZE
                    ? "___ " + part.getOptions() : part.getText());
        }
        return sb.toString().strip();
    }

    /** A judge of the catalog: what it reads, the verdict of each of its evaluators, the book. */
    final class Judge implements AnalyzerProvider {
        private final String name;

        private final List<String> evaluators;

        private final String rule;

        private final Function<AuditableQuiz, String> reads;

        private final java.util.function.BiFunction<String, String, Boolean> faulty;

        Judge(String name, List<String> evaluators, String rule, Function<AuditableQuiz, String> reads,
                java.util.function.BiFunction<String, String, Boolean> faulty) {
            this.name = name;
            this.evaluators = evaluators;
            this.rule = rule;
            this.reads = reads;
            this.faulty = faulty;
        }

        @Override
        public String analyzerName() {
            return name;
        }

        @Override
        public AnalyzerDescriptor describe() {
            return new AnalyzerDescriptor(name, "un juez", AuditTarget.QUIZ, "¿?", "el ejercicio",
                    List.of(new AnalyzerRuleCard(rule, "lo que marca", AnalysisCost.PAID_MODEL),
                            new AnalyzerRuleCard("judges-disagree", "los jueces no coinciden", AnalysisCost.PAID_MODEL)),
                    "0 ejercicios", AnalyzerFamily.ERRORS, List.of(AuditTarget.QUIZ), List.of(FindingResolution.PANEL),
                    AnalysisCost.PAID_MODEL, name, List.of());
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

            private final Map<String, int[]> byEvaluator = new LinkedHashMap<>();

            private final Map<AuditNode, List<Boolean>> verdicts = new IdentityHashMap<>();

            Run(int budget) {
                this.budget = budget;
            }

            @Override
            public Void onQuiz(AuditNode node) {
                if (!(node.getEntity() instanceof AuditableQuiz quiz) || quiz.getMultipleChoice() != null) {
                    return null;
                }
                reached++;
                String read = reads.apply(quiz);
                List<Boolean> found = new ArrayList<>();
                for (String evaluator : evaluators) {
                    int[] tally = byEvaluator.computeIfAbsent(evaluator, e -> new int[3]); // in run, reused, pending
                    String key = evaluator + ": " + read;
                    Boolean verdict = book.get(key);
                    if (verdict != null) {
                        tally[1]++;
                    } else if (budget > 0 && !notApproved.contains(name)) {
                        budget--;
                        verdict = faulty.apply(evaluator, read);
                        book.put(key, verdict);
                        consultations.add(key);
                        tally[0]++;
                    } else {
                        tally[2]++;
                    }
                    found.add(verdict);
                }
                if (found.contains(null)) {
                    node.getUnevaluatedBy().add(name);
                    return null;
                }
                evaluated++;
                verdicts.put(node, found);
                node.getScores().put(name, found.contains(Boolean.TRUE) ? 0.0 : 1.0);
                return null;
            }

            @Override
            public Void onCourseComplete(AuditNode root) {
                List<EvaluatorCoverage> coverages = new ArrayList<>();
                int pending = 0;
                for (Map.Entry<String, int[]> entry : byEvaluator.entrySet()) {
                    int[] t = entry.getValue();
                    pending += t[2];
                    coverages.add(new EvaluatorCoverage(entry.getKey(), new EvaluationCoverage(reached, t[0] + t[1], t[0],
                            t[1], t[2], 0), "v1"));
                }
                boolean approved = !notApproved.contains(name);
                ((DefaultCourseDiagnoses) root.getDiagnoses()).addJudgeCoverage(new JudgeCoverageDiagnosis(name,
                        AuditTarget.QUIZ, reached, evaluated, reached - evaluated, coverages,
                        new JudgeExamStanding(name, "v1", approved ? JudgeExamStatus.APPROVED : JudgeExamStatus.FAILED,
                                approved, approved ? null : "la versión v1 no aprobó su examen"),
                        new ArrayList<>(), approved ? null : "la versión v1 no aprobó su examen",
                        new JudgeCostEstimate(reached - evaluated, pending, 0, false, 0, 1, 0), new ArrayList<>(),
                        new JudgeQuizTally(reached, evaluated, reached - evaluated)));
                return null;
            }

            @Override
            public List<FindingDraft> findingsAt(AuditNode node) {
                List<Boolean> found = verdicts.get(node);
                if (found == null || !found.contains(Boolean.TRUE)) {
                    return List.of();
                }
                boolean split = found.contains(Boolean.FALSE);
                return List.of(new FindingDraft(split ? "judges-disagree" : rule, null, FindingSeverity.HIGH,
                        FindingResolution.PANEL, new FindingEvidence(List.of(new EvidencePart("Ejercicio",
                                statement((AuditableQuiz) node.getEntity()))), name + " lo marca", List.of())));
            }

            @Override
            public Void onKnowledge(AuditNode node) {
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

    /** The judge of «another option is also valid», with the same book: «Is it» also fits a child. */
    final class ChoiceJudge implements ChoiceAlternativeJudgeRunFactory {
        @Override
        public AnalyzerRuleCard judgeRule() {
            return new AnalyzerRuleCard("another-option-valid", "otra opción también vale", AnalysisCost.PAID_MODEL);
        }

        @Override
        public ChoiceAlternativeJudgeRun open(EvaluationRunPolicy policy) {
            int[] budget = {policy != null ? policy.getMaxNewEvaluations() : 0};
            int[] tally = new int[3];
            List<AuditNode> requested = new ArrayList<>();
            Map<AuditNode, Boolean> verdicts = new IdentityHashMap<>();
            return new ChoiceAlternativeJudgeRun() {
                @Override
                public void request(AuditNode quizNode) {
                    requested.add(quizNode);
                }

                @Override
                public void resolveAll() {
                    for (AuditNode node : requested) {
                        AuditableQuiz quiz = (AuditableQuiz) node.getEntity();
                        String read = "choice: " + statement(quiz) + " " + quiz.getMultipleChoice().getItems().stream()
                                .map(i -> i.getLabel()).toList();
                        Boolean verdict = book.get(read);
                        if (verdict != null) {
                            tally[1]++;
                        } else if (budget[0] > 0 && !notApproved.contains("multiple-choice-validity")) {
                            budget[0]--;
                            verdict = read.contains("Is it");
                            book.put(read, verdict);
                            consultations.add(read);
                            tally[0]++;
                        } else {
                            tally[2]++;
                        }
                        if (verdict != null) {
                            verdicts.put(node, verdict);
                        }
                    }
                }

                @Override
                public ChoiceAlternativeJudgment judgmentOf(AuditNode quizNode) {
                    Boolean verdict = verdicts.get(quizNode);
                    String quizId = quizNode.getEntity().getId();
                    if (verdict == null) {
                        return new ChoiceAlternativeJudgment(quizId, ChoiceAlternativeJudgmentStatus.PENDING, null,
                                List.of(), List.of(), null, null, false, "sin veredicto en el libro");
                    }
                    List<FindingDraft> findings = verdict ? List.of(new FindingDraft("another-option-valid", "is it",
                            FindingSeverity.BLOCKING, FindingResolution.PANEL, new FindingEvidence(List.of(
                                    new EvidencePart("Otra que vale", "Is it")), "«Is it» también vale", List.of())))
                            : List.of();
                    return new ChoiceAlternativeJudgment(quizId, ChoiceAlternativeJudgmentStatus.JUDGED, null, List.of(),
                            findings, verdict ? 0.0 : 1.0, "v1", tally[1] > 0, null);
                }

                @Override
                public JudgeCoverageDiagnosis coverage() {
                    int reached = requested.size();
                    int evaluated = verdicts.size();
                    return new JudgeCoverageDiagnosis("multiple-choice-validity", AuditTarget.QUIZ, reached, evaluated,
                            reached - evaluated, List.of(new EvaluatorCoverage("choice-alternative-judge",
                                    new EvaluationCoverage(reached, tally[0] + tally[1], tally[0], tally[1], tally[2], 0),
                                    "v1")), new JudgeExamStanding("multiple-choice-validity", "v1",
                                    JudgeExamStatus.APPROVED, true, null), new ArrayList<>(), null,
                            new JudgeCostEstimate(reached - evaluated, tally[2], 0, false, 0, 1, 0), new ArrayList<>(),
                            new JudgeQuizTally(reached, evaluated, reached - evaluated));
                }
            };
        }
    }

    // -------------------------------------------------------------------------
    // The ports without spaCy
    // -------------------------------------------------------------------------

    /** The course mapped as the real mapper does, with the tokens of a tokenizer that splits on spaces. */
    public CourseMapper mapper() {
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
                            quizzes.add(new AuditableQuiz(tokens(sentences.isEmpty() ? "" : sentences.get(0)), quiz.getId(),
                                    quiz.getTitle(), quiz.getCode(), quiz.getTranslation(), sentences, null,
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

    public static List<NlpToken> tokens(String sentence) {
        List<NlpToken> tokens = new ArrayList<>();
        for (String raw : sentence.replaceAll("([.?!,])", " $1").split("\\s+")) {
            if (raw.isEmpty()) {
                continue;
            }
            boolean punct = raw.matches("[.?!,]");
            String lemma = raw.toLowerCase(Locale.ROOT).replaceAll("s$", "");
            tokens.add(new NlpToken(raw, lemma, punct ? "PUNCT" : "X", null, false, punct));
        }
        return tokens;
    }

    public NlpTokenizer tokenizer() {
        return new NlpTokenizer() {
            @Override
            public List<String> tokenize(String text) {
                return tokens(text).stream().map(NlpToken::getText).toList();
            }

            @Override
            public int countTokens(String text) {
                return tokens(text).size();
            }

            @Override
            public List<NlpToken> analyzeTokens(String text) {
                return tokens(text);
            }

            @Override
            public Map<String, List<NlpToken>> analyzeTokensBatch(List<String> sentences) {
                Map<String, List<NlpToken>> batch = new LinkedHashMap<>();
                for (String sentence : sentences) {
                    batch.put(sentence, tokens(sentence));
                }
                return batch;
            }
        };
    }

    /** A word is red when its level is above the level of its sentence. */
    public SentenceLexicalEvaluator lexicalEvaluator() {
        return (tokens, level) -> {
            List<MisplacedWordFlag> misplaced = new ArrayList<>();
            for (NlpToken token : tokens) {
                CefrLevel its = levels.get(token.getLemma());
                if (its != null && its.ordinal() > level.ordinal()) {
                    misplaced.add(new MisplacedWordFlag(token.getLemma(), "NOUN", its, level));
                }
            }
            return new SentenceLexicalFlags(misplaced, new ArrayList<>());
        };
    }

    /** The topic teaches the words of its label: «Afirmativas regulares» teaches «regular». */
    public TopicTeachingResolver teaching() {
        return node -> new TaughtWords(node.getEntity().getId(), List.of("regular"), List.of());
    }

    public static SentenceLengthConfig lengthConfig() {
        return new SentenceLengthConfig() {
            @Override
            public Optional<TargetRange> getTargetRange(CefrLevel level) {
                return Optional.of(new TargetRange(level, 3, 8));
            }

            @Override
            public int getToleranceMargin() {
                return 2;
            }

            @Override
            public Map<String, Object> describe() {
                return Map.of("A1", "de 3 a 8");
            }
        };
    }

    private static IrregularFormsCatalog irregularForms() {
        return new IrregularFormsCatalog() {
            @Override
            public Set<String> formsOf(String baseWord) {
                return Set.of();
            }

            @Override
            public boolean isPastParticiple(String word) {
                return false;
            }
        };
    }

    private static CorrectorMatcher exactCorrector() {
        return new CorrectorMatcher() {
            @Override
            public Set<String> canonicalForms(String text) {
                return Set.of(text.toLowerCase(Locale.ROOT).strip());
            }

            @Override
            public boolean accepts(GapContext context, List<String> acceptedOptions, String written) {
                return acceptedOptions.stream().flatMap(o -> List.of(o.split("\\|")).stream())
                        .anyMatch(o -> o.strip().equalsIgnoreCase(written.strip()));
            }

            @Override
            public boolean sameAnswer(String first, String second) {
                return first.strip().equalsIgnoreCase(second.strip());
            }

            @Override
            public boolean sameAnswer(GapContext context, String first, String second) {
                return sameAnswer(first, second);
            }

            @Override
            public String tableVersion() {
                return "exacto";
            }
        };
    }
}
