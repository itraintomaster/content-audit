package com.learney.contentaudit.auditdomain.nearduplicates;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.ContentAnalyzer;
import com.learney.contentaudit.auditdomain.DefaultCourseDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultQuizDiagnoses;
import com.learney.contentaudit.auditdomain.EvaluationRunPolicy;
import com.learney.contentaudit.auditdomain.EvaluationRunPurpose;
import com.learney.contentaudit.auditdomain.QuizStudentViewRenderer;
import com.learney.contentaudit.auditdomain.finding.EvidencePart;
import com.learney.contentaudit.auditdomain.finding.FindingDraft;
import com.learney.contentaudit.auditdomain.finding.FindingEvidence;
import com.learney.contentaudit.auditdomain.finding.FindingNodeRef;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.auditdomain.judging.JudgeExamStatus;
import com.learney.contentaudit.auditdomain.judging.JudgeQuizTally;
import com.learney.contentaudit.auditdomain.judging.JudgeRequestPlan;
import com.learney.contentaudit.auditdomain.judging.JudgeRuntime;
import com.learney.contentaudit.auditdomain.judging.JudgeSessions;
import com.learney.contentaudit.auditdomain.judging.JudgeUnitTally;
import com.learney.contentaudit.auditdomain.localtools.LocalToolGateDecision;
import com.learney.contentaudit.auditdomain.localtools.LocalToolRunDiagnosis;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRequest;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRequestMode;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationResolution;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationResolutionKind;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSession;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSubject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.processing.Generated;

/**
 * FEAT-DUPAR: do two exercises of the same topic ask almost the same? An errors analyzer that
 * compares the siblings of each topic, like exact-duplicates, and marks each near copy of the one
 * before it.
 *
 * <ul>
 *   <li>R001: in {@link #onKnowledge}, which the engine calls with the exercises of the topic in the
 *       order of the course, each exercise is paired with the ones before it that ask the same and
 *       are not exact duplicates; the wildcard and the word similarity of each pair are read
 *       there.</li>
 *   <li>R002: a pair is a near duplicate if the wildcard settles it or if it is a candidate of the
 *       pair judge and claude -p says that, for the student, it is the same exercise. No similarity
 *       marks by itself: with nomic-embed-text, the cosine of the vectors (0.80 or more, R009) and,
 *       without Ollama, the word similarity (0.6 or more, R008) only choose which pairs are asked.
 *       If the wildcard settles a pair, nobody is asked: it depends on no model.</li>
 *   <li>The consultations are asked in batches when the course closes, through the judges' runtime
 *       (FEAT-JEXAM): with Ollama, first a vector per exercise of the pairs the wildcard does not
 *       settle, by the session of the vectors, and then a judgment per candidate, by the session of
 *       the judge, the last one; without it, only the judgments. The plan by topic, so a declared
 *       set and the exam bring whole topics, {@code resolveAll}, the coverage and the cost of what
 *       is missing are the runtime's. The modes of the plan are the judge's: the vectors are always
 *       reused or computed, so a declared set never leaves a topic halfway for lack of them
 *       (R009 inv. 3), and the coverage is asked with the judge's session alone, so the vectors
 *       neither enter in what is pending nor in the rate (R009 inv. 4).</li>
 *   <li>R007: in an exam run the file of the exam is the declared set; only the pairs whose copy is
 *       one of its exercises are asked, vectors included, and the exercises outside it get neither
 *       score nor finding, are not halfway and are not counted.</li>
 *   <li>R003 and R004: the first exercise of the topic with which an exercise is near equal is the
 *       one that stays; the copy scores 0 with one low finding for the panel, whose identity is its
 *       rule and the exercise that stays, and the rest score 1.</li>
 *   <li>R008 inv. 3 and 4, R009 inv. 4: the wildcard runs always; an exercise with a pair the budget
 *       or a failure left without an answer, or without one of its vectors, is evaluated halfway,
 *       and the run's row says how many are missing. F-MLOCAL-R006, R010 inv. 3: a version without
 *       an approved exam, or with a judge that states no version, publishes nothing, the wildcard
 *       included; the exam run always publishes, but not without the version of the judge.</li>
 * </ul>
 */
@Generated(
        value = "com.sentinel.SentinelEngine",
        comments = "Generated by Sentinel"
)
class NearDuplicatesAnalyzer implements ContentAnalyzer {

    static final String ANALYZER_NAME = "near-duplicates";

    static final String DISPLAY_NAME = "Ejercicio casi repetido";

    static final String DESCRIPTION = "Compara los ejercicios de cada tema que piden la misma respuesta y marca cada "
            + "copia casi igual del que viene antes: otro pronombre, otro demostrativo, otro posesivo u otro tiempo "
            + "del auxiliar, o la misma idea con otras palabras";

    /** R002 inv. 1: equal under the wildcard. */
    static final String RULE_SAME_BUT_WILDCARD = "same-but-wildcard";

    /** R006: with Ollama the vectors propose the pairs from 0.80; a rule of the card that never marks. */
    static final String RULE_SIMILAR_CANDIDATES = "similar-candidates";

    /** R002 inv. 2: claude -p says it is the same exercise, with Ollama or without it. */
    static final String RULE_SAME_EXERCISE = "same-exercise";

    static final String REASON_NO_TOOL = "sin nomic-embed-text ni claude -p: ";

    static final String REASON_NO_BUDGET = "falta el tope: la corrida no pide --budget near-duplicates=N, así que no "
            + "se le pregunta nada nuevo a claude -p";

    static final String REASON_STOPPED = " dejó de responder: ";

    /**
     * SIMILARITY is the condition of the version of 30/9, whose similarity marked by itself: it stays in
     * the enum to read its reports and no version produces it any more (R009).
     */
    private static final String NEVER_PRODUCED = "SIMILARITY ya no la produce ninguna versión: los vectores sólo "
            + "proponen candidatos";

    /** The tool of the judge in the reasons of the run. */
    private static final String JUDGE_TOOL = "claude -p";

    private static final Locale SPANISH = Locale.forLanguageTag("es-AR");

    private final JudgeSessions opened;

    private final JudgeRuntime runtime;

    private final NearDuplicateRules rules;

    private final SameExerciseQuestion question;

    private final QuizStudentViewRenderer studentViewRenderer;

    private final NearDuplicatesConfig config;

    private final LocalToolGateDecision gateDecision;

    /** The topics reached, in the order of the course, with their exercises and their pairs. */
    private final List<Topic> topics = new ArrayList<>();

    /** The finding of each copy, by node. */
    private final Map<AuditNode, FindingDraft> findings = new IdentityHashMap<>();

    public NearDuplicatesAnalyzer(JudgeSessions opened, JudgeRuntime runtime,
            NearDuplicateRules rules, SameExerciseQuestion question,
            QuizStudentViewRenderer studentViewRenderer, NearDuplicatesConfig config,
            LocalToolGateDecision gateDecision) {
        this.opened = opened;
        this.runtime = runtime;
        this.rules = rules;
        this.question = question;
        this.studentViewRenderer = studentViewRenderer;
        this.config = config;
        this.gateDecision = gateDecision;
    }

    /** R001: the exercises of the topic with a finished exercise, and each pair that asks the same. */
    @Override
    public Void onKnowledge(AuditNode node) {
        if (node == null || node.getChildren() == null) {
            return null;
        }
        List<Member> members = new ArrayList<>();
        for (AuditNode child : node.getChildren()) {
            if (child == null || !(child.getEntity() instanceof AuditableQuiz quiz)) {
                continue;
            }
            String finished = DefaultNearDuplicateRules.finishedExercise(studentViewRenderer, quiz);
            if (finished == null || finished.isBlank()) {
                continue; // a multiple choice without a correct option is FEAT-OMVAL's
            }
            members.add(new Member(child, quiz, finished));
        }
        if (members.isEmpty()) {
            return null;
        }
        Topic topic = new Topic(node, members);
        for (int j = 1; j < members.size(); j++) {
            Member copy = members.get(j);
            for (int i = 0; i < j; i++) {
                Member kept = members.get(i);
                if (!rules.sameRequest(kept.quiz, copy.quiz)) {
                    continue;
                }
                topic.add(new Pair(kept, copy, rules.wildcardDifference(kept.quiz, copy.quiz).orElse(null),
                        rules.wordSimilarity(kept.quiz, copy.quiz)));
            }
        }
        topics.add(topic);
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
        boolean local = localRoute();
        EvaluationRunPolicy policy = opened != null ? opened.getEffectivePolicy() : null;
        boolean examRun = policy != null && policy.getPurpose() == EvaluationRunPurpose.EXAM;
        boolean judgeVersioned = opened != null && opened.getJudgeVersion() != null;
        // R010 inv. 3: a judge that states no version leaves the version with Ollama unnamed -- it
        // publishes nothing, the wildcard included, in the exam run too.
        boolean publishes = local
                ? gateDecision.isPublishes() && judgeVersioned
                : judgeVersioned
                        && (examRun || (opened.getStanding() != null && opened.getStanding().isConsultsOnCourse()));
        String examinedVersion = local
                ? gateDecision.getExaminedVersion()
                : (opened != null ? opened.getJudgeVersion() : null);

        Set<String> examFile = examRun
                ? (policy.getReevaluationSubjectIds() != null ? policy.getReevaluationSubjectIds() : Set.of())
                : null;
        JudgeRequestPlan plan = runtime != null ? runtime.plan(unitsOf(topics), policy) : null;
        List<Reach> reaches = reachesOf(plan, examFile);
        List<EvaluationSession> sessions = opened != null && opened.getSessions() != null
                ? opened.getSessions()
                : List.of();
        // The judge is always the last session; with Ollama the first one is the vectors'.
        EvaluationSession judgeSession = sessions.isEmpty() ? null : sessions.get(sessions.size() - 1);
        EvaluationSession vectorSession = local && sessions.size() > 1 ? sessions.get(0) : null;

        Answers answers = new Answers();
        if (publishes) {
            if (local) {
                askVectors(reaches, vectorSession, vectorsAtATime(policy), answers);
                compareVectors(reaches, answers);
            }
            askJudge(reaches, local, judgeSession, atATime(policy), answers);
        }

        Tally tally = publishAll(reaches, publishes, local);

        if (rootNode == null) {
            return null;
        }
        if (!(rootNode.getDiagnoses() instanceof DefaultCourseDiagnoses)) {
            rootNode.setDiagnoses(new DefaultCourseDiagnoses());
        }
        DefaultCourseDiagnoses course = (DefaultCourseDiagnoses) rootNode.getDiagnoses();
        if (opened != null && runtime != null) {
            JudgeUnitTally judged = new JudgeUnitTally(AuditTarget.KNOWLEDGE, reaches.size(), tally.topicsEvaluated,
                    tally.topicsNotEvaluated, new TreeMap<>(answers.byVersion),
                    plan != null ? plan.getUnmatchedDeclaredIds() : null,
                    new JudgeQuizTally(tally.quizzesReached, tally.quizzesEvaluated, tally.quizzesNotEvaluated));
            course.addJudgeCoverage(runtime.coverage(judgeOnly(judgeSession), judged));
        }
        JudgeExamStatus examState = local
                ? gateDecision.getExamState()
                : (opened != null && opened.getStanding() != null ? opened.getStanding().getStatus() : null);
        course.addLocalToolRun(new LocalToolRunDiagnosis(ANALYZER_NAME,
                gateDecision != null ? gateDecision.getStatus() : null, examinedVersion, examState, publishes,
                reasonOf(local, publishes, tally.quizzesNotEvaluated, policy, vectorSession, judgeSession, answers),
                !local));
        return null;
    }

    @Override
    public String getName() {
        return ANALYZER_NAME;
    }

    @Override
    public AuditTarget getTarget() {
        return AuditTarget.QUIZ;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    /** R003 to R005: the finding of a copy, low, for the panel, naming the exercise that stays. */
    @Override
    public List<FindingDraft> findingsAt(AuditNode node) {
        FindingDraft draft = node != null ? findings.get(node) : null;
        return draft != null ? List.of(draft) : List.of();
    }

    // -------------------------------------------------------------------------
    // What the run reaches, and the two batches of consultations
    // -------------------------------------------------------------------------

    /** The nodes of the topics reached, which the runtime plans. */
    private static List<AuditNode> unitsOf(List<Topic> topics) {
        List<AuditNode> units = new ArrayList<>();
        for (Topic topic : topics) {
            units.add(topic.node);
        }
        return units;
    }

    /**
     * What the run reaches of each topic, in the order of the plan, each with the mode the plan gives
     * it. In an exam run (R007) only the exercises of the file and the pairs whose copy is one of
     * them count; a topic with none of them is neither asked nor counted.
     */
    private List<Reach> reachesOf(JudgeRequestPlan plan, Set<String> examFile) {
        Map<AuditNode, Topic> byNode = new IdentityHashMap<>();
        for (Topic topic : topics) {
            byNode.put(topic.node, topic);
        }
        List<AuditNode> planned = plan != null && plan.getUnits() != null ? plan.getUnits() : unitsOf(topics);
        List<EvaluationRequestMode> modes = plan != null && plan.getModes() != null ? plan.getModes() : List.of();
        List<Reach> reaches = new ArrayList<>();
        for (int t = 0; t < planned.size(); t++) {
            Topic topic = byNode.get(planned.get(t));
            if (topic == null) {
                continue;
            }
            EvaluationRequestMode mode = t < modes.size() && modes.get(t) != null
                    ? modes.get(t)
                    : EvaluationRequestMode.REUSE_OR_EVALUATE;
            Reach reach = new Reach(topic, mode, examFile);
            if (!reach.counted.isEmpty()) {
                reaches.add(reach);
            }
        }
        return reaches;
    }

    /**
     * R003, R004, R008 inv. 4: each exercise the run counts scores and carries its finding, or is left
     * halfway with the findings it has; if nothing is published -- the version has no approved exam, or
     * its judge states no version --, every exercise is left not evaluated, the wildcard included. Counts
     * the exercises and the topics for the coverage of the judge.
     */
    private Tally publishAll(List<Reach> reaches, boolean publishes, boolean local) {
        Tally tally = new Tally();
        for (Reach reach : reaches) {
            tally.quizzesReached += reach.counted.size();
            if (!publishes) {
                for (Member member : reach.counted) {
                    markUnevaluated(member.node);
                }
                tally.quizzesNotEvaluated += reach.counted.size();
                tally.topicsNotEvaluated++;
                continue;
            }
            boolean whole = true;
            for (Member copy : reach.counted) {
                boolean halfway = false;
                Pair near = null;
                for (Pair pair : reach.topic.pairsOf(copy)) {
                    Decision decision = decide(pair, local);
                    if (decision == Decision.UNRESOLVED) {
                        halfway = true;
                    } else if (decision == Decision.NEAR) {
                        near = pair;
                        break;
                    }
                }
                publish(copy, near, halfway);
                if (halfway) {
                    whole = false;
                    tally.quizzesNotEvaluated++;
                } else {
                    tally.quizzesEvaluated++;
                }
            }
            if (whole) {
                tally.topicsEvaluated++;
            } else {
                tally.topicsNotEvaluated++;
            }
        }
        return tally;
    }

    /**
     * R009, first batch: the vector of each exercise of a pair the wildcard does not settle, by the
     * session of the vectors, all together. Whatever the plan says, always to reuse or compute: the
     * modes of the plan are the judge's.
     */
    private void askVectors(List<Reach> reaches, EvaluationSession vectorSession, int maxConcurrent,
            Answers answers) {
        List<EvaluationRequest> requests = new ArrayList<>();
        List<Member> asked = new ArrayList<>();
        for (Reach reach : reaches) {
            Set<Member> needed = new HashSet<>();
            for (Pair pair : reach.pairs) {
                if (pair.wildcard == null) {
                    needed.add(pair.kept);
                    needed.add(pair.copy);
                }
            }
            for (Member member : reach.topic.members) {
                if (needed.contains(member)) {
                    requests.add(new EvaluationRequest(new EvaluationSubject(member.quizId(),
                            Map.of(SentenceEmbeddingEvaluator.EXERCISE, member.finished)),
                            EvaluationRequestMode.REUSE_OR_EVALUATE));
                    asked.add(member);
                }
            }
        }
        List<EvaluationResolution> resolutions = resolveAll(vectorSession, requests, maxConcurrent);
        for (int k = 0; k < asked.size(); k++) {
            EvaluationResolution resolution = k < resolutions.size() ? resolutions.get(k) : null;
            if (resolution == null || resolution.getKind() == EvaluationResolutionKind.PENDING) {
                answers.vectorsPending++;
            } else if (resolution.getKind() == EvaluationResolutionKind.FAILED) {
                answers.vectorsFailed++;
            } else {
                Optional<float[]> vector = SentenceEmbeddingEvaluator.decode(resolution.getPayload());
                if (vector.isPresent()) {
                    asked.get(k).vector = vector.get();
                } else {
                    answers.vectorsUnreadable++;
                }
            }
        }
    }

    /**
     * R009 inv. 1: the cosine of the two vectors of each pair the wildcard does not settle, which
     * says whether it is a candidate of the judge; with a vector missing -- or two that cannot be
     * compared -- the pair has none and stays unresolved.
     */
    private void compareVectors(List<Reach> reaches, Answers answers) {
        for (Reach reach : reaches) {
            for (Pair pair : reach.pairs) {
                if (pair.wildcard != null || pair.kept.vector == null || pair.copy.vector == null) {
                    continue;
                }
                double cosine = SentenceEmbeddingEvaluator.cosine(pair.kept.vector, pair.copy.vector);
                if (Double.isNaN(cosine)) {
                    answers.uncomparable++;
                } else {
                    pair.similarity = cosine;
                }
            }
        }
    }

    /**
     * R009 and R008, second batch: a judgment per candidate, by the session of the judge, all
     * together, with the mode the plan gives its topic.
     */
    private void askJudge(List<Reach> reaches, boolean local, EvaluationSession judgeSession, int maxConcurrent,
            Answers answers) {
        List<EvaluationRequest> requests = new ArrayList<>();
        List<Pair> asked = new ArrayList<>();
        for (Reach reach : reaches) {
            for (Pair pair : reach.pairs) {
                if (asksJudge(pair, local)) {
                    requests.add(new EvaluationRequest(question.subjectOf(pair.kept.quiz, pair.copy.quiz),
                            reach.mode));
                    asked.add(pair);
                }
            }
        }
        List<EvaluationResolution> resolutions = resolveAll(judgeSession, requests, maxConcurrent);
        for (int k = 0; k < asked.size(); k++) {
            EvaluationResolution resolution = k < resolutions.size() ? resolutions.get(k) : null;
            if (resolution == null || resolution.getKind() == EvaluationResolutionKind.PENDING) {
                answers.pending++;
                continue;
            }
            if (resolution.getKind() == EvaluationResolutionKind.FAILED) {
                answers.failed++;
                continue;
            }
            Optional<SameExerciseVerdict> verdict = question.read(resolution.getPayload());
            if (verdict.isEmpty()) {
                answers.unreadable++;
                continue;
            }
            asked.get(k).verdict = verdict.get();
            if (resolution.getEvaluatorVersion() != null) {
                answers.byVersion.merge(resolution.getEvaluatorVersion(), 1, Integer::sum);
            }
        }
    }

    /** A session that cannot resolve leaves every consultation without an answer, never the run. */
    private static List<EvaluationResolution> resolveAll(EvaluationSession session, List<EvaluationRequest> requests,
            int maxConcurrent) {
        if (session == null || requests.isEmpty()) {
            return List.of();
        }
        try {
            List<EvaluationResolution> resolutions = session.resolveAll(requests, maxConcurrent);
            return resolutions != null ? resolutions : List.of();
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    /** The consultations to the judge at a time: the run's, or one. */
    private static int atATime(EvaluationRunPolicy policy) {
        return policy != null && policy.getMaxConcurrentEvaluations() != null
                ? Math.max(1, policy.getMaxConcurrentEvaluations())
                : 1;
    }

    /**
     * The vectors at a time: those of the policy the gate decided -- the run's or, without a request,
     * one at a time (hook B-06) --, and the judge's when it brings none.
     */
    private int vectorsAtATime(EvaluationRunPolicy policy) {
        EvaluationRunPolicy local = gateDecision != null ? gateDecision.getLocalPolicy() : null;
        return local != null && local.getMaxConcurrentEvaluations() != null
                ? Math.max(1, local.getMaxConcurrentEvaluations())
                : atATime(policy);
    }

    /**
     * R009 inv. 4: the sessions the coverage is asked with, the judge's alone -- its evaluator id, its
     * version, its standing and its policy --: the runtime adds up what is pending in all its
     * sessions and measures the rate with all its evaluators, and the vectors, some 4,600 in a minute,
     * would turn the hours that claude -p needs into minutes.
     */
    private JudgeSessions judgeOnly(EvaluationSession judgeSession) {
        List<String> ids = opened.getEvaluatorIds();
        return new JudgeSessions(opened.getJudgeName(), opened.getJudgeVersion(),
                ids != null && !ids.isEmpty() ? List.of(ids.get(ids.size() - 1)) : List.of(),
                judgeSession != null ? List.of(judgeSession) : List.of(), opened.getStanding(),
                opened.getEffectivePolicy());
    }

    // -------------------------------------------------------------------------
    // The decision of each pair
    // -------------------------------------------------------------------------

    /** With the local model available the vectors propose the candidates; without it, the words do. */
    private boolean localRoute() {
        return gateDecision != null && gateDecision.getStatus() != null && gateDecision.getStatus().isAvailable();
    }

    /**
     * R009 inv. 1, R008 inv. 1: a pair the wildcard does not settle is a candidate of the judge if,
     * with Ollama, the cosine of its vectors reaches the candidate similarity and, without it, its
     * word similarity reaches 0.6.
     */
    private boolean asksJudge(Pair pair, boolean local) {
        if (pair.wildcard != null) {
            return false;
        }
        return local
                ? pair.similarity != null && pair.similarity >= config.candidateSimilarity() - 1e-9
                : pair.wordSimilarity >= config.candidateWordSimilarity() - 1e-9;
    }

    /**
     * R002: the wildcard wins without asking; a candidate is a near duplicate if the judge says so and
     * is discarded if it says no, whatever its similarity; a pair without a verdict -- or, with Ollama,
     * without one of its vectors -- is unresolved.
     */
    private Decision decide(Pair pair, boolean local) {
        if (pair.wildcard != null) {
            pair.condition = NearDuplicateCondition.WILDCARD;
            return Decision.NEAR;
        }
        if (local && pair.similarity == null) {
            return Decision.UNRESOLVED;
        }
        if (!asksJudge(pair, local)) {
            return Decision.NOT;
        }
        if (pair.verdict == null) {
            return Decision.UNRESOLVED;
        }
        if (pair.verdict.isSameExercise()) {
            pair.condition = NearDuplicateCondition.JUDGED;
            return Decision.NEAR;
        }
        return Decision.NOT;
    }

    /** R003, R004: the copy scores 0 with its finding; the rest 1. Halfway keeps both. */
    private void publish(Member member, Pair near, boolean halfway) {
        AuditNode node = member.node;
        if (node.getScores() == null) {
            node.setScores(new LinkedHashMap<>());
        }
        node.getScores().put(ANALYZER_NAME, near != null ? 0.0 : 1.0);
        if (near != null) {
            if (!(node.getDiagnoses() instanceof DefaultQuizDiagnoses)) {
                node.setDiagnoses(new DefaultQuizDiagnoses());
            }
            ((DefaultQuizDiagnoses) node.getDiagnoses()).setNearDuplicateDiagnosis(new NearDuplicateDiagnosis(
                    near.kept.quizId(), near.condition, near.wildcard,
                    near.similarity != null ? near.similarity : 0.0, near.wordSimilarity,
                    near.verdict != null ? near.verdict.getReason() : null, member.finished, near.kept.finished, 0.0));
            findings.put(node, finding(near));
        }
        if (halfway) {
            if (node.getPartiallyEvaluatedBy() == null) {
                node.setPartiallyEvaluatedBy(new ArrayList<>());
            }
            if (!node.getPartiallyEvaluatedBy().contains(ANALYZER_NAME)) {
                node.getPartiallyEvaluatedBy().add(ANALYZER_NAME);
            }
        }
    }

    /**
     * R005: the copy and the exercise that stays as the student sees them, their finished
     * exercises, and a line with what changes, how much they look alike or how many words change,
     * with the reason of claude -p. The exercise that stays is the other node of the finding.
     */
    private FindingDraft finding(Pair pair) {
        String line = line(pair);
        String copySeen = DefaultSameExerciseQuestion.asSeen(studentViewRenderer, pair.copy.quiz);
        List<EvidencePart> examined = new ArrayList<>();
        examined.add(new EvidencePart("Copia", copySeen));
        examined.add(new EvidencePart("Queda", DefaultSameExerciseQuestion.asSeen(studentViewRenderer, pair.kept.quiz)));
        examined.add(new EvidencePart("Copia terminada", pair.copy.finished));
        examined.add(new EvidencePart("Queda terminado", pair.kept.finished));
        examined.add(new EvidencePart("Casi igual", line));
        if (pair.condition == NearDuplicateCondition.JUDGED && pair.verdict != null
                && pair.verdict.getReason() != null && !pair.verdict.getReason().isBlank()) {
            examined.add(new EvidencePart("Razón de claude -p", pair.verdict.getReason()));
        }
        AuditableQuiz kept = pair.kept.quiz;
        List<FindingNodeRef> related = List.of(new FindingNodeRef(AuditTarget.QUIZ, kept.getId(),
                kept.getLabel() != null ? kept.getLabel() : kept.getId()));
        String observation = copySeen + " · " + line.substring(0, 1).toLowerCase(SPANISH) + line.substring(1)
                + ". El alumno contesta dos veces casi lo mismo: se reemplaza en la mesa por un ejercicio nuevo con "
                + "palabras del semáforo del tema.";
        return new FindingDraft(ruleOf(pair.condition), pair.kept.quizId(), FindingSeverity.LOW,
                FindingResolution.PANEL, new FindingEvidence(examined, observation, related));
    }

    /**
     * «Casi igual a …208f del mismo tema: cambia he por she» for the wildcard; for the judge, with
     * Ollama, the similarity of the vectors that proposed the pair, «Para claude -p es el mismo
     * ejercicio que …355e: parecido 0,87», and without it how many words change, «… cambian 2 de 6
     * palabras».
     */
    private String line(Pair pair) {
        String kept = shortId(pair.kept.quizId());
        return switch (pair.condition) {
            case WILDCARD -> "Casi igual a " + kept + " del mismo tema: cambia " + spoken(pair.wildcard);
            case JUDGED -> "Para claude -p es el mismo ejercicio que " + kept + ": " + (pair.similarity != null
                    ? "parecido " + String.format(SPANISH, "%.2f", pair.similarity)
                    : changedWords(pair));
            case SIMILARITY -> throw new IllegalStateException(NEVER_PRODUCED);
        };
    }

    /** «cambian 2 de 6 palabras»: the words to change, add or remove, over the longer exercise. */
    private static String changedWords(Pair pair) {
        List<String> copyWords = lower(DefaultNearDuplicateRules.words(pair.copy.finished));
        List<String> keptWords = lower(DefaultNearDuplicateRules.words(pair.kept.finished));
        int changed = DefaultNearDuplicateRules.editDistance(keptWords, copyWords);
        int longer = Math.max(copyWords.size(), keptWords.size());
        return (changed == 1 ? "cambia 1 de " : "cambian " + changed + " de ") + longer + " palabras";
    }

    private static String ruleOf(NearDuplicateCondition condition) {
        return switch (condition) {
            case WILDCARD -> RULE_SAME_BUT_WILDCARD;
            case JUDGED -> RULE_SAME_EXERCISE;
            case SIMILARITY -> throw new IllegalStateException(NEVER_PRODUCED);
        };
    }

    /** «he → she» read aloud: «he por she»; several, «You por I, wanted por want y this por that». */
    static String spoken(String difference) {
        String[] changes = difference.split(", ");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < changes.length; i++) {
            if (i > 0) {
                out.append(i == changes.length - 1 ? " y " : ", ");
            }
            out.append(changes[i].replace(" → ", " por "));
        }
        return out.toString();
    }

    private static String shortId(String id) {
        return id == null ? "" : id.length() <= 4 ? id : "…" + id.substring(id.length() - 4);
    }

    private static List<String> lower(List<String> words) {
        List<String> lower = new ArrayList<>(words.size());
        for (String word : words) {
            lower.add(word.toLowerCase(Locale.ROOT).replace('’', '\''));
        }
        return lower;
    }

    // -------------------------------------------------------------------------
    // Why something stayed not evaluated or halfway
    // -------------------------------------------------------------------------

    /**
     * Why something stayed not evaluated or halfway, in the order of F-MLOCAL-R003, R006 and R008:
     * first why nothing was published; then, with Ollama, what was missing of the vectors, which come
     * first; and then what was missing of the judge.
     */
    private String reasonOf(boolean local, boolean publishes, int notWhole, EvaluationRunPolicy policy,
            EvaluationSession vectorSession, EvaluationSession judgeSession, Answers answers) {
        if (notWhole == 0) {
            return null;
        }
        if (!publishes) {
            return unpublishedReason(local);
        }
        if (local && answers.missingVectors() > 0) {
            return vectorsReason(vectorSession, answers);
        }
        return judgeReason(policy, judgeSession, answers);
    }

    /**
     * Nothing was published: without a version for the judge, the reason of the judge --
     * with Ollama (R010 inv. 3), or after the missing tools without it --; otherwise the standing of the
     * version that did not publish, the version of the whole step with Ollama.
     */
    private String unpublishedReason(boolean local) {
        boolean judgeVersioned = opened != null && opened.getJudgeVersion() != null;
        String judgeReason = opened != null && opened.getStanding() != null ? opened.getStanding().getReason() : null;
        if (local) {
            return !judgeVersioned && judgeReason != null ? judgeReason : gateDecision.getReason();
        }
        if (!judgeVersioned) {
            return REASON_NO_TOOL + (gateDecision != null ? gateDecision.getReason() : "")
                    + (judgeReason != null ? "; claude -p: " + judgeReason : "");
        }
        return judgeReason;
    }

    /**
     * What the judge left without a verdict: why, and how many pairs are left to judge -- those with
     * no answer, the ones whose consultation failed and the ones whose verdict cannot be read: the
     * pairs the coverage declares as missing (R009 inv. 4).
     */
    private String judgeReason(EvaluationRunPolicy policy, EvaluationSession judgeSession, Answers answers) {
        int unjudged = answers.unjudged();
        String missing = unjudged + (unjudged == 1 ? " par sin juzgar" : " pares sin juzgar");
        Optional<String> stopped = judgeSession != null ? judgeSession.unavailableReason() : Optional.empty();
        if (stopped != null && stopped.isPresent()) {
            return JUDGE_TOOL + REASON_STOPPED + stopped.get() + "; quedan " + missing;
        }
        if (answers.pending > 0) {
            if (policy != null && policy.getMaxNewEvaluations() == 0) {
                return REASON_NO_BUDGET + ": quedan " + missing;
            }
            return "se acabó el tope de near-duplicates: quedan " + missing;
        }
        if (answers.failed > 0) {
            return "falló la consulta de " + answers.failed + (answers.failed == 1 ? " par" : " pares") + "; quedan "
                    + missing;
        }
        return answers.unreadable + (answers.unreadable == 1 ? " par con un veredicto que no se puede leer"
                : " pares con un veredicto que no se puede leer") + "; quedan " + missing;
    }

    /** What the local model left without an answer: the vectors, and why. Apart from the pairs of the judge. */
    private String vectorsReason(EvaluationSession vectorSession, Answers answers) {
        String missing = answers.vectorsPending + " vectores sin pedir";
        Optional<String> stopped = vectorSession != null ? vectorSession.unavailableReason() : Optional.empty();
        if (stopped != null && stopped.isPresent()) {
            String tool = gateDecision.getStatus() != null && gateDecision.getStatus().getTool() != null
                    ? gateDecision.getStatus().getTool()
                    : "el modelo local";
            return tool + REASON_STOPPED + stopped.get() + "; quedan " + missing;
        }
        if (answers.vectorsPending > 0) {
            return "quedan " + missing;
        }
        if (answers.vectorsFailed > 0) {
            return "falló la consulta de " + answers.vectorsFailed + " vectores";
        }
        if (answers.vectorsUnreadable > 0) {
            return answers.vectorsUnreadable + " vectores que no se pueden leer";
        }
        return answers.uncomparable + " pares con vectores que no se pueden comparar";
    }

    /** F-HALL-R008: reached but not evaluated -- without score, diagnosis nor finding. */
    private void markUnevaluated(AuditNode node) {
        if (node.getScores() != null) {
            node.getScores().remove(ANALYZER_NAME);
        }
        findings.remove(node);
        if (node.getUnevaluatedBy() == null) {
            node.setUnevaluatedBy(new ArrayList<>());
        }
        if (!node.getUnevaluatedBy().contains(ANALYZER_NAME)) {
            node.getUnevaluatedBy().add(ANALYZER_NAME);
        }
    }

    // -------------------------------------------------------------------------
    // What a run gathers
    // -------------------------------------------------------------------------

    private enum Decision {
        NEAR, NOT, UNRESOLVED
    }

    /** A topic reached: its node, its exercises in order and its pairs, the copy's earlier ones first. */
    private static final class Topic {

        private final AuditNode node;

        private final List<Member> members;

        private final List<Pair> pairs = new ArrayList<>();

        private final Map<Member, List<Pair>> byCopy = new IdentityHashMap<>();

        Topic(AuditNode node, List<Member> members) {
            this.node = node;
            this.members = members;
        }

        void add(Pair pair) {
            pairs.add(pair);
            byCopy.computeIfAbsent(pair.copy, member -> new ArrayList<>()).add(pair);
        }

        List<Pair> pairsOf(Member copy) {
            return byCopy.getOrDefault(copy, List.of());
        }
    }

    /**
     * What a run reaches of a topic, with the mode the plan gives it: the exercises it counts and the
     * pairs whose copy is one of them. Everything of the topic, or in an exam run (R007) what is in the
     * file; the exercise that stays of a pair may be outside it, and its vector is asked all the same.
     */
    private static final class Reach {

        private final Topic topic;

        private final EvaluationRequestMode mode;

        private final List<Member> counted = new ArrayList<>();

        private final List<Pair> pairs = new ArrayList<>();

        Reach(Topic topic, EvaluationRequestMode mode, Set<String> examFile) {
            this.topic = topic;
            this.mode = mode;
            for (Member member : topic.members) {
                if (inFile(member, examFile)) {
                    counted.add(member);
                }
            }
            for (Pair pair : topic.pairs) {
                if (inFile(pair.copy, examFile)) {
                    pairs.add(pair);
                }
            }
        }

        /** Without a file, everything counts. */
        private static boolean inFile(Member member, Set<String> examFile) {
            return examFile == null || (member.quizId() != null && examFile.contains(member.quizId()));
        }
    }

    /** An exercise of a topic: its node, its finished exercise and, with Ollama, its vector. */
    private static final class Member {

        private final AuditNode node;

        private final AuditableQuiz quiz;

        private final String finished;

        private float[] vector;

        Member(AuditNode node, AuditableQuiz quiz, String finished) {
            this.node = node;
            this.quiz = quiz;
            this.finished = finished;
        }

        String quizId() {
            return quiz.getId();
        }
    }

    /** Two exercises of a topic that ask the same: the one before, the one after, and what decides them. */
    private static final class Pair {

        private final Member kept;

        private final Member copy;

        private final String wildcard;

        private final double wordSimilarity;

        /** With Ollama, the cosine of the vectors of a pair the wildcard does not settle; null if none. */
        private Double similarity;

        private SameExerciseVerdict verdict;

        private NearDuplicateCondition condition;

        Pair(Member kept, Member copy, String wildcard, double wordSimilarity) {
            this.kept = kept;
            this.copy = copy;
            this.wildcard = wildcard;
            this.wordSimilarity = wordSimilarity;
        }
    }

    /** What publishing counted: the exercises the run reached and how they were left, and the topics. */
    private static final class Tally {

        private int topicsEvaluated;

        private int topicsNotEvaluated;

        private int quizzesReached;

        private int quizzesEvaluated;

        private int quizzesNotEvaluated;
    }

    /** What the consultations brought: the judge's, per pair, and the vectors' apart. */
    private static final class Answers {

        private int pending;

        private int failed;

        private int unreadable;

        private int vectorsPending;

        private int vectorsFailed;

        private int vectorsUnreadable;

        /** Pairs whose two vectors came but cannot be compared. */
        private int uncomparable;

        /** The verdicts of the judge, by the version each was recorded with: never the vectors'. */
        private final Map<String, Integer> byVersion = new LinkedHashMap<>();

        /** The pairs asked of the judge that ended without a verdict. */
        int unjudged() {
            return pending + failed + unreadable;
        }

        int missingVectors() {
            return vectorsPending + vectorsFailed + vectorsUnreadable + uncomparable;
        }
    }
}
