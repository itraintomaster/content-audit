package com.learney.contentaudit.revisiondomain.batchengine;

import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.choice;
import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.cloze;
import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.quiz;
import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.text;
import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.topic;

import com.learney.contentaudit.auditdomain.ActiveAnalysisSelection;
import com.learney.contentaudit.auditdomain.ActiveAnalysisSelectionStore;
import com.learney.contentaudit.auditdomain.AnalyzerCorrectionCatalog;
import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditReportStore;
import com.learney.contentaudit.auditdomain.AuditReportSummary;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableMilestone;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.AuditableTopic;
import com.learney.contentaudit.auditdomain.Baseline;
import com.learney.contentaudit.auditdomain.BaselineStore;
import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.auditdomain.DefaultCourseDiagnoses;
import com.learney.contentaudit.auditdomain.NlpToken;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerMeasure;
import com.learney.contentaudit.auditdomain.contextnumbers.AuditDigest;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.CourseRepository;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.MilestoneEntity;
import com.learney.contentaudit.coursedomain.NodeKind;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.RootNodeEntity;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.coursedomain.TopicEntity;
import com.learney.contentaudit.refinerdomain.DiagnosisKind;
import com.learney.contentaudit.refinerdomain.RefinementPlan;
import com.learney.contentaudit.refinerdomain.RefinementPlanStore;
import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.refinerdomain.RefinementTaskStatus;
import com.learney.contentaudit.refinerdomain.TaskOrigin;
import com.learney.contentaudit.refinerdomain.errortask.ErrorTaskBrief;
import com.learney.contentaudit.refinerdomain.errortask.ErrorTaskBriefer;
import com.learney.contentaudit.revisiondomain.CourseElementLocator;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.RevisionArtifact;
import com.learney.contentaudit.revisiondomain.RevisionArtifactStore;
import com.learney.contentaudit.revisiondomain.RevisionVerdict;
import com.learney.contentaudit.revisiondomain.batch.BatchChange;
import com.learney.contentaudit.revisiondomain.batch.BatchChangeOrigin;
import com.learney.contentaudit.revisiondomain.batch.BatchChangeStep;
import com.learney.contentaudit.revisiondomain.batch.BatchSettings;
import com.learney.contentaudit.revisiondomain.batch.BatchStatus;
import com.learney.contentaudit.revisiondomain.batch.BatchStore;
import com.learney.contentaudit.revisiondomain.batch.CorrectionBatch;
import com.learney.contentaudit.revisiondomain.blindsample.BlindPairView;
import com.learney.contentaudit.revisiondomain.blindsample.BlindSample;
import com.learney.contentaudit.revisiondomain.blindsample.BlindSampleResult;
import com.learney.contentaudit.revisiondomain.blindsample.BlindSampleService;
import com.learney.contentaudit.revisiondomain.blindsample.PairAnswer;
import com.learney.contentaudit.revisiondomain.comparison.BatchComparator;
import com.learney.contentaudit.revisiondomain.comparison.BatchComparison;
import com.learney.contentaudit.revisiondomain.comparison.BatchVerdict;
import com.learney.contentaudit.revisiondomain.comparison.ChangeOutcome;
import com.learney.contentaudit.revisiondomain.comparison.EffectiveGoals;
import com.learney.contentaudit.revisiondomain.comparison.Goal;
import com.learney.contentaudit.revisiondomain.comparison.GoalBook;
import com.learney.contentaudit.revisiondomain.comparison.GoalKind;
import com.learney.contentaudit.revisiondomain.comparison.GoalOrigin;
import com.learney.contentaudit.revisiondomain.comparison.GoalReference;
import com.learney.contentaudit.revisiondomain.comparison.GoalUnit;
import com.learney.contentaudit.revisiondomain.comparison.SealedGoals;
import com.learney.contentaudit.revisiondomain.comparison.VerdictKind;
import com.learney.contentaudit.revisiondomain.panel.PanelDecision;
import com.learney.contentaudit.revisiondomain.panel.PanelDecisionSink;
import com.learney.contentaudit.revisiondomain.panel.PanelCandidate;
import com.learney.contentaudit.revisiondomain.panel.PanelOutcomeKind;
import com.learney.contentaudit.revisiondomain.panel.PanelRunSettings;
import com.learney.contentaudit.revisiondomain.panel.PanelRunner;
import com.learney.contentaudit.revisiondomain.panel.PanelTopicResult;
import com.learney.contentaudit.revisiondomain.panel.QueryAllowance;
import com.learney.contentaudit.revisiondomain.panel.TopicWork;
import com.learney.contentaudit.revisiondomain.panelengine.MesaFixture;
import com.learney.contentaudit.revisiondomain.projection.ProjectionScope;
import com.learney.contentaudit.auditdomain.datacoherence.DefaultSpokenTextBuilder;
import com.learney.contentaudit.revisiondomain.nonregression.SpokenText;
import com.learney.contentaudit.revisiondomain.nonregression.UnitMeasurement;
import com.learney.contentaudit.revisiondomain.panel.TopicMeter;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * A batch of A1 on the course of 29/9 without a model, a file or F: the course in memory with «Afirmativas regulares»,
 * «Contraste inicial», «Contracciones», «Be: preguntas yes/no» and the topic of reside; its analysis as an audit tree
 * with the findings each test gives; the plan with tasks as FEAT-TAREA makes them; stores in memory; and F's
 * comparator, blind sample and book of goals written by hand.
 */
public final class TandaFixture {

    public static final Path COURSE_PATH = Path.of("/copia/del/curso");

    public static final String CHILD = "67fab6d59930102295341e87";

    public static final String CHILD_TOPIC = "6814dafa7d73e7209a13d360";

    public static final String RESIDE = "67fab6d59930102295342f01";

    public static final String RESIDE_TOPIC = "6814dafa7d73e7209a13d3d0";

    public static final String PAST = "67fab6d59930102295345001";

    public static final String PAST_TOPIC = "6814dafa7d73e7209a13d400";

    public static final String BASELINE = "audit-20260929";

    public static final String PLAN = "plan-20260929";

    private TandaFixture() {
    }

    // -------------------------------------------------------------------------
    // The course and its analysis
    // -------------------------------------------------------------------------

    public static CourseEntity course() {
        KnowledgeEntity child = topic(CHILD_TOPIC, "Be: preguntas yes/no", new ArrayList<>(List.of(
                choice(CHILD, "a child? (He)", "is he", "am i", "are you"))));
        child.getQuizTemplates().get(0).setKnowledgeId(CHILD_TOPIC);
        QuizTemplateEntity reside = quiz(RESIDE, "Vivimos en Londres con nuestro abuelo.", text("We reside"), cloze("in"),
                text("London with our granddad."));
        reside.setKnowledgeId(RESIDE_TOPIC);
        KnowledgeEntity resideTopic = topic(RESIDE_TOPIC, "Preposiciones de lugar", new ArrayList<>(List.of(reside)));
        TopicEntity present = new TopicEntity("t-present", null, NodeKind.TOPIC, "Present Simple", null, "m-a1", null, null,
                0, null, new ArrayList<>(List.of(MesaFixture.afirmativas(), MesaFixture.contraste(),
                        MesaFixture.contracciones(), child, resideTopic)));
        MilestoneEntity a1 = new MilestoneEntity("m-a1", null, NodeKind.MILESTONE, "A1", null, "root", null, 0, null,
                new ArrayList<>(List.of(present)));
        QuizTemplateEntity went = quiz(PAST, "Fui al cine.", text("I"), cloze("went"), text("(go) to the cinema."));
        went.setKnowledgeId(PAST_TOPIC);
        TopicEntity past = new TopicEntity("t-past", null, NodeKind.TOPIC, "Past Simple", null, "m-a2", null, null, 0, null,
                new ArrayList<>(List.of(topic(PAST_TOPIC, "Past Simple", new ArrayList<>(List.of(went))))));
        MilestoneEntity a2 = new MilestoneEntity("m-a2", null, NodeKind.MILESTONE, "A2", null, "root", null, 1, null,
                new ArrayList<>(List.of(past)));
        return new CourseEntity("curso", "Learney", null, new RootNodeEntity("root", null, NodeKind.ROOT, "Curso", null,
                new ArrayList<>(List.of(a1, a2))), null);
    }

    /** The analysis of the course: each node with the findings and the analyzers that left it unevaluated. */
    public static AuditReport report(CourseEntity course, List<Finding> findings, Map<String, List<String>> unevaluated,
            List<String> analyzers) {
        Map<String, List<Finding>> byNode = new HashMap<>();
        for (Finding finding : findings) {
            byNode.computeIfAbsent(finding.getNode().getNodeId(), k -> new ArrayList<>()).add(finding);
        }
        AuditNode root = node(null, AuditTarget.COURSE, null);
        root.setDiagnoses(new DefaultCourseDiagnoses());
        for (MilestoneEntity milestone : course.getRoot().getMilestones()) {
            List<AuditableTopic> topics = new ArrayList<>();
            AuditNode milestoneNode = node(null, AuditTarget.MILESTONE, root);
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
                        quizNode.setUnevaluatedBy(new ArrayList<>(unevaluated.getOrDefault(quiz.getId(), List.of())));
                    }
                    AuditableKnowledge auditable = new AuditableKnowledge(quizzes, knowledge.getLabel(),
                            knowledge.getInstructions(), true, knowledge.getId(), knowledge.getLabel(), knowledge.getCode(),
                            knowledge.getSentenceMode(), topic.getLabel());
                    knowledgeNode.setEntity(auditable);
                    knowledgeNode.setFindings(new ArrayList<>(byNode.getOrDefault(knowledge.getId(), List.of())));
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
        for (String analyzer : analyzers) {
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

    /** The tokens of the stored sentence: the vegetables as spaCy reads them, the rest split by spaces. */
    static List<NlpToken> tokens(QuizTemplateEntity quiz) {
        if (MesaFixture.VEGETABLES.equals(quiz.getId())) {
            List<NlpToken> tokens = new ArrayList<>();
            for (String spec : "I|I|PRON love|love|VERB a|a|DET lot|lot|NOUN of|of|ADP vegetables|vegetable|NOUN .|.|PUNCT"
                    .split(" ")) {
                String[] f = spec.split("\\|");
                tokens.add(new NlpToken(f[0], f[1], f[2], null, false, f[2].equals("PUNCT")));
            }
            return tokens;
        }
        String sentence = quiz.getSentences() != null && !quiz.getSentences().isEmpty() ? quiz.getSentences().get(0) : "";
        List<NlpToken> tokens = new ArrayList<>();
        for (String word : sentence.replaceAll("([.?!,])", " $1").split("\\s+")) {
            if (!word.isEmpty()) {
                boolean punct = word.matches("[.?!,]");
                tokens.add(new NlpToken(word, word.toLowerCase(), punct ? "PUNCT" : "X", null, false, punct));
            }
        }
        return tokens;
    }

    // -------------------------------------------------------------------------
    // The plan
    // -------------------------------------------------------------------------

    /** A task of an exercise as FEAT-TAREA makes it, with its finding as origin. */
    public static RefinementTask task(String id, Finding finding, String knowledgeId, CefrLevel level) {
        String quizId = finding.getNode().getNodeId();
        DiagnosisKind kind;
        try {
            kind = DiagnosisKind.valueOf(finding.getAnalyzer().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException e) {
            kind = null;
        }
        return new RefinementTask(id, finding.getNode().getTarget(), quizId, quizId, kind, 1, RefinementTaskStatus.PENDING,
                new TaskOrigin(finding, level, knowledgeId, knowledgeId, new ArrayList<>(List.of(quizId)),
                        new ArrayList<>()));
    }

    /** A task of the whole topic, as the mini theory, that reaches the given exercises. */
    public static RefinementTask topicTask(String id, Finding finding, String knowledgeId, List<String> reached) {
        return new RefinementTask(id, AuditTarget.KNOWLEDGE, knowledgeId, knowledgeId, DiagnosisKind.MINI_THEORY, 1,
                RefinementTaskStatus.PENDING, new TaskOrigin(finding, CefrLevel.A1, knowledgeId, knowledgeId,
                        new ArrayList<>(reached), new ArrayList<>()));
    }

    /** The LEMMA_ABSENCE task that covers findings of off-level-words. */
    public static RefinementTask covering(String id, Finding lemma, List<Finding> covered, String knowledgeId) {
        RefinementTask task = task(id, lemma, knowledgeId, CefrLevel.A1);
        task.setDiagnosisKind(DiagnosisKind.LEMMA_ABSENCE);
        task.getOrigin().setCoveredFindings(new ArrayList<>(covered));
        return task;
    }

    /** A task of the vocabulary of its own, without a finding of origin: SENTENCE_LENGTH. */
    public static RefinementTask lengthTask(String id, String quizId) {
        return new RefinementTask(id, AuditTarget.QUIZ, quizId, quizId, DiagnosisKind.SENTENCE_LENGTH, 1,
                RefinementTaskStatus.PENDING, null);
    }

    public static RefinementPlan plan(RefinementTask... tasks) {
        return new RefinementPlan(PLAN, BASELINE, Instant.parse("2026-09-29T12:00:00Z"), new ArrayList<>(List.of(tasks)));
    }

    // -------------------------------------------------------------------------
    // Stores in memory
    // -------------------------------------------------------------------------

    public static class Batches implements BatchStore {
        public final Map<String, CorrectionBatch> saved = new LinkedHashMap<>();

        public int saves;

        @Override
        public synchronized void save(CorrectionBatch batch) {
            saves++;
            saved.put(batch.getId(), batch);
        }

        @Override
        public synchronized Optional<CorrectionBatch> load(String batchId) {
            return Optional.ofNullable(saved.get(batchId));
        }

        @Override
        public synchronized Optional<CorrectionBatch> open() {
            return saved.values().stream().filter(b -> b.getStatus() != BatchStatus.PUBLISHED).reduce((a, b) -> b);
        }

        @Override
        public synchronized List<CorrectionBatch> list() {
            return new ArrayList<>(saved.values());
        }

        @Override
        public synchronized Optional<CorrectionBatch> latestPublished() {
            return saved.values().stream().filter(b -> b.getStatus() == BatchStatus.PUBLISHED && b.getPublication() != null)
                    .reduce((a, b) -> b);
        }
    }

    public static final class Plans implements RefinementPlanStore {
        public final Map<String, RefinementPlan> saved = new LinkedHashMap<>();

        public Plans(RefinementPlan... plans) {
            for (RefinementPlan plan : plans) {
                saved.put(plan.getId(), plan);
            }
        }

        @Override
        public String save(RefinementPlan plan) {
            if (plan.getId() == null) {
                plan.setId("plan-" + (saved.size() + 1));
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

    public static final class Reports implements AuditReportStore {
        public final Map<String, AuditReport> saved = new LinkedHashMap<>();

        public Reports put(String id, AuditReport report) {
            saved.put(id, report);
            return this;
        }

        @Override
        public String save(AuditReport report) {
            String id = "audit-publicado-" + (saved.size() + 1);
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

    public static final class Artifacts implements RevisionArtifactStore {
        public final Map<String, RevisionArtifact> saved = new LinkedHashMap<>();

        @Override
        public String save(RevisionArtifact artifact) {
            saved.put(artifact.getProposal().getPlanId() + "/" + artifact.getProposal().getProposalId(), artifact);
            return artifact.getProposal().getProposalId();
        }

        @Override
        public Optional<RevisionArtifact> load(String planId, String proposalId) {
            return Optional.ofNullable(saved.get(planId + "/" + proposalId));
        }

        @Override
        public List<RevisionArtifact> listByPlan(String planId) {
            return saved.values().stream().filter(a -> Objects.equals(a.getProposal().getPlanId(), planId)).toList();
        }

        @Override
        public Optional<RevisionArtifact> findByProposalId(String proposalId, Optional<String> planId) {
            return saved.values().stream().filter(a -> Objects.equals(a.getProposal().getProposalId(), proposalId))
                    .findFirst();
        }

        @Override
        public boolean hasPendingProposalForTask(String planId, String taskId) {
            return saved.values().stream().anyMatch(a -> Objects.equals(a.getProposal().getPlanId(), planId)
                    && Objects.equals(a.getProposal().getTaskId(), taskId) && a.getVerdict() == RevisionVerdict.PENDING_APPROVAL);
        }

        @Override
        public List<RevisionArtifact> list() {
            return new ArrayList<>(saved.values());
        }

        public List<RevisionArtifact> pending() {
            return saved.values().stream().filter(a -> a.getVerdict() == RevisionVerdict.PENDING_APPROVAL).toList();
        }
    }

    public static final class ActivePair implements ActiveAnalysisSelectionStore {
        public ActiveAnalysisSelection selection;

        @Override
        public Optional<ActiveAnalysisSelection> read() {
            return Optional.ofNullable(selection);
        }

        @Override
        public void write(ActiveAnalysisSelection selection) {
            this.selection = selection;
        }

        @Override
        public void clear() {
            selection = null;
        }
    }

    public static final class Baselines implements BaselineStore {
        public Baseline baseline;

        public Baselines(Baseline baseline) {
            this.baseline = baseline;
        }

        @Override
        public Optional<Baseline> read() {
            return Optional.ofNullable(baseline);
        }

        @Override
        public void write(Baseline baseline) {
            this.baseline = baseline;
        }
    }

    /** The course of content-audit at its path: what it holds and every time it is written. */
    public static final class Courses implements CourseRepository {
        public CourseEntity course;

        public final List<CourseEntity> written = new ArrayList<>();

        public final List<Path> paths = new ArrayList<>();

        public Courses(CourseEntity course) {
            this.course = course;
        }

        @Override
        public CourseEntity load(Path path) {
            return course;
        }

        @Override
        public void save(CourseEntity course, Path path) {
            written.add(course);
            paths.add(path);
            this.course = course;
        }
    }

    /** F's book of goals: the vocabulary floor of 0.5 points, sealed when asked. */
    public static final class Goals implements GoalBook {
        public final List<SealedGoals> seals = new ArrayList<>();

        public static final Goal VOCABULARY_FLOOR = new Goal("piso-vocabulario", GoalKind.FLOOR, "vocabulario",
                AuditTarget.COURSE, "root", 0.5, GoalUnit.POINTS, GoalReference.BASELINE, GoalOrigin.FAMILY_DEFAULT);

        @Override
        public EffectiveGoals effective() {
            return new EffectiveGoals("huella-metas", new ArrayList<>(List.of(VOCABULARY_FLOOR)), null);
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

    /** F's comparison, written by hand: what each scope gives. */
    public static final class Comparator implements BatchComparator {
        public final List<ProjectionScope> scopes = new ArrayList<>();

        public final List<Path> paths = new ArrayList<>();

        public Function<ProjectionScope, BatchComparison> answer = scope -> passing(scope, List.of());

        @Override
        public BatchComparison compare(Path coursePath, ProjectionScope scope) {
            paths.add(coursePath);
            scopes.add(scope);
            return answer.apply(scope);
        }
    }

    public static BatchComparison passing(ProjectionScope scope, List<ChangeOutcome> changes) {
        return comparison(scope, changes, VerdictKind.PASSES);
    }

    public static BatchComparison comparison(ProjectionScope scope, List<ChangeOutcome> changes, VerdictKind kind) {
        BatchComparison comparison = new BatchComparison();
        comparison.setScopeId(scope != null ? scope.getScopeId() : null);
        comparison.setAppliedProposalIds(scope != null ? new ArrayList<>(scope.getProposalIds()) : new ArrayList<>());
        comparison.setChanges(new ArrayList<>(changes));
        comparison.setRows(new ArrayList<>());
        comparison.setBreaches(new ArrayList<>());
        comparison.setVerdict(new BatchVerdict(kind, new ArrayList<>(), new ArrayList<>(), null,
                kind == VerdictKind.PASSES ? null : "falta la muestra ciega", null));
        return comparison;
    }

    /** F's blind sample: the scope it was drawn on. */
    public static final class Samples implements BlindSampleService {
        public final List<ProjectionScope> drawn = new ArrayList<>();

        @Override
        public BlindSample draw(Path coursePath, ProjectionScope scope, Long seed) {
            drawn.add(scope);
            BlindSample sample = new BlindSample();
            sample.setSampleId("muestra-" + drawn.size());
            sample.setScopeId(scope.getScopeId());
            return sample;
        }

        @Override
        public List<BlindPairView> pairs(String sampleId) {
            return new ArrayList<>();
        }

        @Override
        public void answer(String sampleId, int pairIndex, PairAnswer answer, String reviewer) {
        }

        @Override
        public BlindSampleResult close(String sampleId) {
            return null;
        }

        @Override
        public Optional<BlindSampleResult> result(String sampleId) {
            return Optional.empty();
        }
    }

    /** The briefs as FEAT-TAREA gives them: the finding, what it covers and what its analyzer lets it touch. */
    public static ErrorTaskBriefer briefer() {
        AnalyzerCorrectionCatalog corrections = MesaFixture.corrections();
        return (report, task) -> {
            if (task == null || task.getOrigin() == null || task.getOrigin().getFinding() == null) {
                return Optional.empty();
            }
            Finding finding = task.getOrigin().getFinding();
            List<ExercisePart> scope = corrections.find(finding.getAnalyzer()).map(c -> c.editScope(finding.getResolution()))
                    .orElse(List.of());
            return Optional.of(new ErrorTaskBrief(task, finding, new ArrayList<>(task.getOrigin().getCoveredFindings()),
                    new ArrayList<>(scope), !scope.isEmpty(), null, new ArrayList<>()));
        };
    }

    /** A locator on the course in memory, which never touches the course it is given. */
    public static CourseElementLocator locator() {
        return new CourseElementLocator() {
            @Override
            public Optional<CourseElementSnapshot> snapshot(CourseEntity course, AuditTarget target, String nodeId) {
                Optional<BatchBook.Place> place = BatchBook.place(course, nodeId);
                if (place.isEmpty()) {
                    return Optional.empty();
                }
                if (target == AuditTarget.KNOWLEDGE && place.get().quiz() == null) {
                    return Optional.of(new CourseElementSnapshot(AuditTarget.KNOWLEDGE, nodeId, null, place.get().knowledge()));
                }
                if (target == AuditTarget.QUIZ && place.get().quiz() != null) {
                    return Optional.of(new CourseElementSnapshot(AuditTarget.QUIZ, nodeId,
                            BatchBook.copy(place.get().quiz()), null));
                }
                return Optional.empty();
            }

            @Override
            public CourseEntity replace(CourseEntity course, CourseElementSnapshot replacement) {
                List<MilestoneEntity> milestones = new ArrayList<>();
                for (MilestoneEntity milestone : course.getRoot().getMilestones()) {
                    List<TopicEntity> topics = new ArrayList<>();
                    for (TopicEntity topic : milestone.getTopics()) {
                        List<KnowledgeEntity> knowledges = new ArrayList<>();
                        for (KnowledgeEntity knowledge : topic.getKnowledges()) {
                            KnowledgeEntity kept = knowledge;
                            if (replacement.getNodeTarget() == AuditTarget.KNOWLEDGE
                                    && knowledge.getId().equals(replacement.getNodeId())) {
                                kept = new KnowledgeEntity(knowledge.getId(), knowledge.getCode(), knowledge.getKind(),
                                        knowledge.getLabel(), knowledge.getOldId(), knowledge.getParentId(),
                                        knowledge.isIsRule(), replacement.getKnowledge().getInstructions(),
                                        knowledge.getOrder(), knowledge.getSlug(), knowledge.getQuizTemplates(),
                                        knowledge.getSentenceMode());
                            } else if (replacement.getNodeTarget() == AuditTarget.QUIZ && knowledge.getQuizTemplates()
                                    .stream().anyMatch(q -> q.getId().equals(replacement.getNodeId()))) {
                                List<QuizTemplateEntity> quizzes = new ArrayList<>();
                                for (QuizTemplateEntity quiz : knowledge.getQuizTemplates()) {
                                    quizzes.add(quiz.getId().equals(replacement.getNodeId()) ? replacement.getQuiz() : quiz);
                                }
                                kept = new KnowledgeEntity(knowledge.getId(), knowledge.getCode(), knowledge.getKind(),
                                        knowledge.getLabel(), knowledge.getOldId(), knowledge.getParentId(),
                                        knowledge.isIsRule(), knowledge.getInstructions(), knowledge.getOrder(),
                                        knowledge.getSlug(), quizzes, knowledge.getSentenceMode());
                            }
                            knowledges.add(kept);
                        }
                        topics.add(new TopicEntity(topic.getId(), topic.getCode(), topic.getKind(), topic.getLabel(),
                                topic.getOldId(), topic.getParentId(), topic.getChildren(), topic.getRuleIds(),
                                topic.getOrder(), topic.getSlug(), knowledges));
                    }
                    milestones.add(new MilestoneEntity(milestone.getId(), milestone.getCode(), milestone.getKind(),
                            milestone.getLabel(), milestone.getOldId(), milestone.getParentId(), milestone.getChildren(),
                            milestone.getOrder(), milestone.getSlug(), topics));
                }
                RootNodeEntity root = course.getRoot();
                return new CourseEntity(course.getId(), course.getTitle(), course.getKnowledgeIds(),
                        new RootNodeEntity(root.getId(), root.getCode(), root.getKind(), root.getLabel(),
                                root.getChildren(), milestones), course.getSlug());
            }

            @Override
            public CourseEntity alignQuizTitles(CourseEntity course, String knowledgeId) {
                return course;
            }
        };
    }

    /** A panel runner written by hand: what each topic decides, every work and setting it got, its queries spent. */
    public static final class Panels implements PanelRunner {
        public final List<TopicWork> works = Collections.synchronizedList(new ArrayList<>());

        public final List<PanelRunSettings> settings = new ArrayList<>();

        public Function<TopicWork, List<PanelDecision>> decide = work -> new ArrayList<>();

        /** Each decision of a topic costs one query of the allowance; without a turn it ends not attempted. */
        public boolean spendOnePerDecision = true;

        /** The run is cut -- the process dies -- after this many topics; never when negative. */
        public int cutAfterTopics = -1;

        /** The service of the models fails after this many topics of a run: the allowance trips with the cause. */
        public int tripAfterTopics = -1;

        public String tripCause = "el servicio de modelos no respondió (529)";

        @Override
        public List<PanelTopicResult> runTopics(List<TopicWork> topics, PanelRunSettings settings, QueryAllowance allowance,
                PanelDecisionSink sink) {
            this.settings.add(settings);
            List<PanelTopicResult> results = new ArrayList<>();
            int done = 0;
            for (TopicWork work : topics) {
                if (tripAfterTopics >= 0 && done++ == tripAfterTopics && allowance != null) {
                    allowance.trip(tripCause);
                }
                if (cutAfterTopics >= 0 && works.size() >= cutAfterTopics) {
                    throw new IllegalStateException("se cortó la tanda después de " + cutAfterTopics + " temas");
                }
                works.add(work);
                List<PanelDecision> decided = new ArrayList<>();
                for (PanelDecision decision : decide.apply(work)) {
                    PanelDecision given = decision;
                    if (spendOnePerDecision && allowance != null) {
                        if (allowance.tripped().isPresent() || !allowance.tryReserve(1)) {
                            given = new PanelDecision(decision.getKnowledgeId(), decision.getQuizIds(), decision.getTaskIds(),
                                    PanelOutcomeKind.NOT_ATTEMPTED, null, null, new ArrayList<>(), 0, new ArrayList<>(), null,
                                    null, new ArrayList<>(), 0);
                        } else {
                            allowance.settle(1, 1);
                        }
                    }
                    sink.accept(given);
                    decided.add(given);
                }
                results.add(new PanelTopicResult(work.getKnowledge().getId(), decided, 0, false));
            }
            return results;
        }

        /** Every candidate given to measure and judge, with its work. */
        public final List<TopicWork> assessedWorks = new ArrayList<>();

        public final List<PanelCandidate> assessed = new ArrayList<>();

        /** What the panel says of a candidate given to it: accepted, resolving every task of its work, by default. */
        public java.util.function.BiFunction<TopicWork, PanelCandidate, PanelDecision> assess = (work, candidate) -> {
            List<String> tasks = new ArrayList<>();
            for (com.learney.contentaudit.refinerdomain.errortask.ErrorTaskBrief brief : work.getBriefs()) {
                tasks.add(brief.getTask().getId());
            }
            return new PanelDecision(work.getKnowledge().getId(), new ArrayList<>(List.of(candidate.getQuiz() != null
                    ? candidate.getQuiz().getId() : work.getKnowledge().getId())), tasks, PanelOutcomeKind.ACCEPTED,
                    candidate, null, new ArrayList<>(tasks), 1, new ArrayList<>(), null, null, new ArrayList<>(), 0);
        };

        @Override
        public PanelDecision assessGiven(TopicWork work, String quizId, PanelCandidate candidate, PanelRunSettings settings,
                QueryAllowance allowance) {
            assessedWorks.add(work);
            assessed.add(candidate);
            return assess.apply(work, candidate);
        }
    }

    public static PanelDecision decision(String knowledgeId, String quizId, List<String> taskIds, PanelOutcomeKind outcome,
            PanelCandidate accepted, List<String> resolved, int attempts) {
        return new PanelDecision(knowledgeId, quizId != null ? new ArrayList<>(List.of(quizId)) : new ArrayList<>(),
                new ArrayList<>(taskIds), outcome, accepted, null, new ArrayList<>(resolved), attempts, new ArrayList<>(), null,
                null, new ArrayList<>(), 0);
    }

    public static SentencePartEntity gap() {
        return new SentencePartEntity(SentencePartKind.CLOZE, "", null);
    }

    // -------------------------------------------------------------------------
    // A batch and its changes
    // -------------------------------------------------------------------------

    public static final Instant OPENED_AT = Instant.parse("2026-10-01T12:00:00Z");

    public static final String START_AUDIT = "audit-20261001";

    /** An open batch of A1 over the plan, with the given tasks. */
    public static CorrectionBatch batch(String id, String... taskIds) {
        return new CorrectionBatch(id, CefrLevel.A1, BatchStatus.OPEN, BASELINE, START_AUDIT, PLAN,
                new ArrayList<>(MesaFixture.ANALYZERS), new BatchSettings(2, 1, 3), new ArrayList<>(),
                new ArrayList<>(List.of(taskIds)), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), null, null, 0, OPENED_AT, null, new ArrayList<>());
    }

    /** A change of the batch, with its steps in the order they were taken. */
    public static BatchChange change(String changeId, BatchChangeOrigin origin, String ruleId, List<String> taskIds,
            BatchChangeStep... steps) {
        return new BatchChange(changeId, origin, ruleId, null, new ArrayList<>(taskIds),
                new ArrayList<>(List.of(steps)), null, false, OPENED_AT, null);
    }

    /** A step: the exercise as the change leaves it and the parts it touched. */
    public static BatchChangeStep step(QuizTemplateEntity after, ExercisePart... parts) {
        return new BatchChangeStep(after.getId(), new ArrayList<>(List.of(parts)),
                new CourseElementSnapshot(AuditTarget.QUIZ, after.getId(), after, null));
    }

    /** The exercise of the course with that id, as the course has it. */
    public static QuizTemplateEntity exercise(CourseEntity course, String quizId) {
        return BatchBook.place(course, quizId).map(BatchBook.Place::quiz)
                .orElseThrow(() -> new AssertionError("no exercise " + quizId));
    }

    /** A copy of the exercise with what changes. */
    public static QuizTemplateEntity edited(QuizTemplateEntity quiz,
            java.util.function.Consumer<QuizTemplateEntity> edit) {
        QuizTemplateEntity copy = BatchBook.copy(quiz);
        edit.accept(copy);
        return copy;
    }

    // -------------------------------------------------------------------------
    // The meter of the panel, by hand
    // -------------------------------------------------------------------------

    /**
     * The meter of the panel without an engine: what each exercise says aloud, as the real builder says it, and the
     * findings a test gives for the topic as it is measured; every call is kept with the allowance it got.
     */
    public static final class Meter implements TopicMeter {
        public final List<KnowledgeEntity> measured = new ArrayList<>();

        public final List<QueryAllowance> allowances = new ArrayList<>();

        public Function<KnowledgeEntity, List<Finding>> findings = topic -> new ArrayList<>();

        @Override
        public UnitMeasurement measure(TopicWork work, KnowledgeEntity knowledgeState, List<String> analyzers,
                QueryAllowance allowance) {
            measured.add(knowledgeState);
            allowances.add(allowance);
            List<SpokenText> spoken = new ArrayList<>();
            for (QuizTemplateEntity quiz : knowledgeState.getQuizTemplates()) {
                spoken.add(new SpokenText(quiz.getId(), quiz.getAnswerAudioUrl(), new ArrayList<>(
                        new DefaultSpokenTextBuilder().spokenTexts(auditable(quiz), knowledgeState.getSentenceMode()))));
            }
            return new UnitMeasurement(work.getLevel(), new ArrayList<>(findings.apply(knowledgeState)), new ArrayList<>(),
                    new ArrayList<>(), spoken, new ArrayList<>());
        }
    }

    /** The exercise as the analysis reads it. */
    public static AuditableQuiz auditable(QuizTemplateEntity quiz) {
        return new AuditableQuiz(tokens(quiz), quiz.getId(), quiz.getTitle(), quiz.getCode(), quiz.getTranslation(),
                quiz.getSentences(), null, quiz.getInstructions(), quiz.getForm().getSentenceParts(),
                quiz.getForm().getMultipleChoice(), quiz.getMiniTheory(), quiz.getAnswerAudioUrl());
    }

    // -------------------------------------------------------------------------
    // «I learned ___ (me / myself) to drive a car.»
    // -------------------------------------------------------------------------

    public static final String LEARNED = "67fab6d5993010229534399b";

    public static final String REFLEXIVE = "6814dafa7d73e7209a13d3e0";

    /** The course with «I learned ___ (me / myself) to drive a car.» in a topic of A1. */
    public static CourseEntity withLearned(CourseEntity course) {
        QuizTemplateEntity learned = quiz(LEARNED, "Aprendí a manejar solo.", text("I learned"), cloze("myself"),
                text("(me / myself) to drive a car."));
        learned.setKnowledgeId(REFLEXIVE);
        course.getRoot().getMilestones().get(0).getTopics().get(0).getKnowledges()
                .add(topic(REFLEXIVE, "Pronombres reflexivos", new ArrayList<>(List.of(learned))));
        return course;
    }

    /** data-coherence on its audio, which reads the «/» (SLASH_IN_AUDIO): it is recorded again, by rule. */
    public static Finding audioOfLearned() {
        return MesaFixture.finding("data-coherence", "no-slash", LEARNED, com.learney.contentaudit.auditdomain.finding
                        .FindingSeverity.MEDIUM, com.learney.contentaudit.auditdomain.finding.FindingResolution.RULE,
                "audio", "audio: «I learned me / myself to drive a car.»; hoy: «I learned myself to drive a car.». El"
                        + " audio lee la barra: se regraba con el texto de hoy.");
    }

    /** The real table of parts to fields, the one the factory of the batch builds. */
    public static com.learney.contentaudit.revisiondomain.preservation.CorrectionScope correctionScope() {
        return new com.learney.contentaudit.revisiondomain.preservationengine.DefaultCorrectionScope();
    }

    /** The changes recorded in the batch through the real ledger, with their pending proposals and artifacts. */
    public static CorrectionBatch recorded(CorrectionBatch batch, CourseEntity course, RevisionArtifactStore artifacts,
            BatchChange... changes) {
        DefaultBatchChangeLedger ledger = new DefaultBatchChangeLedger(artifacts, locator());
        for (BatchChange change : changes) {
            ledger.record(batch, change, course);
        }
        return batch;
    }

    /** An outcome of a task, as the stages leave it. */
    public static void outcome(CorrectionBatch batch, String taskId, com.learney.contentaudit.revisiondomain.batch
            .BatchTaskOutcomeKind kind, String changeId, int attempts, String detail) {
        BatchBook.setOutcome(batch, taskId, kind, changeId, attempts, detail);
    }
}
