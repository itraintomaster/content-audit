package com.learney.contentaudit.auditdomain.analysis;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.choicequality.ChoiceQualityAnalyzerProvider;
import com.learney.contentaudit.auditdomain.choicequality.ChoiceQualitySubjectBuilder;
import com.learney.contentaudit.auditdomain.judging.*;
import com.learney.contentaudit.auditdomain.datacoherence.DataCoherenceDiagnosis;
import com.learney.contentaudit.auditdomain.datacoherence.DataCoherenceStage;
import com.learney.contentaudit.evaluationledgerdomain.ContentFingerprinter;
import java.util.*;

/** Uses the same subject builder as the judge, including level, title, instructions and correct options. */
public final class DefaultAnalysisComposer implements AnalysisComposer {
    public static final String PROVENANCE = "analysisProvenance";
    private final AnalyzerCatalog catalog;
    private final ScoreAggregator aggregator;
    private final ContextNumbersCalculator numbers;
    private final ChoiceQualitySubjectBuilder qualityInputs;
    private final ContentFingerprinter fingerprinter;
    private final Map<String, java.util.function.Function<AuditNode, com.learney.contentaudit.evaluationledgerdomain.EvaluationSubject>> inputs;

    public DefaultAnalysisComposer(AnalyzerCatalog catalog, ScoreAggregator aggregator,
            ContextNumbersCalculator numbers, QuizStudentViewRenderer renderer, ContentFingerprinter fingerprinter) {
        this.catalog = Objects.requireNonNull(catalog);
        this.aggregator = Objects.requireNonNull(aggregator);
        this.numbers = Objects.requireNonNull(numbers);
        this.qualityInputs = new ChoiceQualitySubjectBuilder(Objects.requireNonNull(renderer));
        this.fingerprinter = Objects.requireNonNull(fingerprinter);
        inputs = Map.of(
                "naturalness", new com.learney.contentaudit.auditdomain.englishquality.EnglishQualitySubjectBuilder(renderer,
                        new com.learney.contentaudit.auditdomain.finishedsentence.DefaultFinishedSentenceReader())::build,
                "a1-didactic", new com.learney.contentaudit.auditdomain.englishquality.EnglishQualitySubjectBuilder(renderer,
                        new com.learney.contentaudit.auditdomain.finishedsentence.DefaultFinishedSentenceReader())::build,
                "pragmatism", new com.learney.contentaudit.auditdomain.pragmatism.PragmatismSubjectBuilder(renderer)::build,
                "multiple-choice-quality", qualityInputs::build,
                "quiz-instruction", new com.learney.contentaudit.auditdomain.quizinstructionengine.DefaultQuizInstructionSubjectBuilder(renderer)::build,
                "single-answer", new com.learney.contentaudit.auditdomain.singleanswerengine.DefaultSingleAnswerSubjectBuilder(renderer)::build,
                "translation-sense", new com.learney.contentaudit.auditdomain.translationsenseengine.DefaultTranslationSenseSubjectBuilder(renderer)::build,
                "multiple-choice-validity", new com.learney.contentaudit.auditdomain.choicealternativeengine.DefaultChoiceAlternativeSubjectBuilder(renderer)::build,
                "near-duplicates", new com.learney.contentaudit.auditdomain.nearduplicates.NearDuplicatesSubjectBuilder(renderer)::build,
                "mini-theory", n -> new com.learney.contentaudit.auditdomain.minitheoryengine.DefaultMiniTheorySubjectBuilder(renderer)
                        .build(n.getTarget() == AuditTarget.KNOWLEDGE ? n : n.ancestor(AuditTarget.KNOWLEDGE).orElseThrow()));
    }
    @Override public Set<String> supportedAnalyzers() { return inputs.keySet(); }

    @Override public AuditReport contentOnly(AuditReport report) {
        Objects.requireNonNull(report, "Falta el análisis a proyectar");
        AuditNode root = copy(report.getRoot(), null);
        Set<String> ran = new LinkedHashSet<>();
        for (AuditNode node : index(root).values()) {
            boolean removedAudio = node.getFindings().removeIf(DataCoherenceStage::isAudioFinding);
            if (node.getDiagnoses() instanceof DefaultQuizDiagnoses diagnoses) {
                diagnoses.getDataCoherenceDiagnosis().ifPresent(saved -> {
                    if (saved.getIssues() == null) return;
                    var textual = safe(saved.getIssues()).stream()
                            .filter(issue -> issue != null && !DataCoherenceStage.isAudioIssue(issue)).toList();
                    double score = textual.isEmpty() && node.getFindings().stream()
                            .noneMatch(f -> "data-coherence".equals(f.getAnalyzer())) ? 1.0 : 0.0;
                    diagnoses.setDataCoherenceDiagnosis(new DataCoherenceDiagnosis(textual, null, score));
                    // Do not promote a historical unevaluated or partial measurement to an approval.
                    if (node.getScores().containsKey("data-coherence"))
                        node.getScores().put("data-coherence", score);
                });
                if (removedAudio && node.getScores().containsKey("data-coherence")
                        && diagnoses.getDataCoherenceDiagnosis().isEmpty()) {
                    node.getScores().put("data-coherence", node.getFindings().stream()
                            .anyMatch(f -> "data-coherence".equals(f.getAnalyzer())) ? 0.0 : 1.0);
                }
            }
            if (node.getTarget() != AuditTarget.QUIZ) node.getScores().remove("data-coherence");
            node.getMetadata().put("dataCoherenceStage", DataCoherenceStage.CONTENT.name());
            ran.addAll(node.getScores().keySet().stream().filter(name -> !name.contains("/")).toList());
            ran.addAll(node.getUnevaluatedBy());
            ran.addAll(node.getPartiallyEvaluatedBy());
        }
        // This labels a view, not a new evaluation. Original versions, measures and provenance survive.
        root.getMetadata().put("analysisProjection", "CONTENT_WITHOUT_AUDIO_V1");
        aggregator.aggregate(root);
        numbers.compute(root, catalog.list().stream().filter(card -> ran.contains(card.getName())).toList());
        return new AuditReport(root);
    }

    @Override public String fingerprint(String analyzer, AuditNode node) {
        if (!inputs.containsKey(analyzer))
            throw new IllegalArgumentException("Este analizador todavía no declara su huella de integración: " + analyzer);
        return fingerprinter.fingerprint(inputs.get(analyzer).apply(node).getContent());
    }

    @Override public AuditReport compose(AuditReport primary, List<AnalysisSupplement> supplements) {
        return compose(primary, supplements, false);
    }

    @Override public AuditReport preserveCompatibleOpinions(AuditReport current, AuditReport previous,
            String previousAuditId) {
        Objects.requireNonNull(current, "Falta el análisis del contenido actual");
        Objects.requireNonNull(previous, "Falta el análisis guardado");
        var currentNodes = index(current.getRoot());
        var previousNodes = index(previous.getRoot());
        var saved = new ArrayList<AnalysisSupplement>();
        for (String analyzer : supportedAnalyzers()) {
            var missing = new LinkedHashSet<String>();
            for (var entry : previousNodes.entrySet()) {
                AuditNode from = entry.getValue(), to = currentNodes.get(entry.getKey());
                if (to != null && eligible(from, analyzer) && eligible(to, analyzer)
                        && hasStoredOpinion(from, analyzer) && !hasStoredOpinion(to, analyzer)) {
                    missing.add(entry.getKey());
                }
            }
            if (!missing.isEmpty()) saved.add(new AnalysisSupplement(null, previousAuditId, analyzer, null, null, missing, previous));
        }
        // This is the same measurement, not a new integration or a new timestamp. The ordinary
        // fingerprint check below includes all topic inputs for topic-scoped judges.
        return compose(current, saved, true);
    }

    private AuditReport compose(AuditReport primary, List<AnalysisSupplement> supplements,
            boolean preserveProvenance) {
        Objects.requireNonNull(primary, "Falta el análisis de partida");
        AuditNode root = copy(primary.getRoot(), null);
        Map<String, AuditNode> target = index(root);
        // A structured naturalness supplement must not count the historical constant
        // ranking score as approval by the new criterion on untouched exercises.
        boolean replacesNaturalness = !preserveProvenance
                && safe(supplements).stream().anyMatch(s -> "naturalness".equals(s.analyzer()));
        if (replacesNaturalness) for (AuditNode n : target.values()) {
            if (n.getTarget() == AuditTarget.QUIZ && n.getScores().containsKey("naturalness")
                    && !com.learney.contentaudit.auditdomain.englishquality.EnglishQualitySubjectBuilder.CONTRACT
                            .equals(n.getMetadata().get("naturalnessInputContract"))) {
                n.getScores().remove("naturalness");
                n.getFindings().removeIf(f -> "naturalness".equals(f.getAnalyzer()));
                n.getPartiallyEvaluatedBy().remove("naturalness");
                if (n.getDiagnoses() instanceof DefaultQuizDiagnoses d) d.setNaturalnessDiagnosis(null);
                if (!n.getUnevaluatedBy().contains("naturalness")) n.getUnevaluatedBy().add("naturalness");
            }
        }
        Set<String> ran = new LinkedHashSet<>();
        for (AuditNode n : target.values()) {
            ran.addAll(n.getScores().keySet().stream().filter(k -> !k.contains("/")).toList());
            ran.addAll(n.getUnevaluatedBy());
            ran.addAll(n.getPartiallyEvaluatedBy());
        }
        Map<String, JudgeCoverageDiagnosis> coverages = new LinkedHashMap<>();
        if (root.getDiagnoses() instanceof CourseDiagnoses d)
            for (var c : safe(d.getJudgeCoverages())) coverages.put(c.getJudgeName(), c);
        for (var supplement : safe(supplements)) {
            String name = supplement.analyzer();
            Map<String, AuditNode> source = index(supplement.report().getRoot());
            boolean applied = false;
            for (String id : supplement.nodeIds()) {
                AuditNode from = source.get(id), to = target.get(id);
                if (from == null || to == null || !hasStoredOpinion(from, name)) continue;
                if (!Objects.equals(fingerprint(name, from), fingerprint(name, to))) continue;
                applied = true;
                to.getScores().remove(name);
                if (from.getScores().get(name) != null) to.getScores().put(name, from.getScores().get(name));
                to.getUnevaluatedBy().remove(name);
                to.getPartiallyEvaluatedBy().remove(name);
                if (from.getUnevaluatedBy().contains(name)) to.getUnevaluatedBy().add(name);
                if (from.getPartiallyEvaluatedBy().contains(name)) to.getPartiallyEvaluatedBy().add(name);
                to.getFindings().removeIf(f -> name.equals(f.getAnalyzer()));
                to.getFindings().addAll(from.getFindings().stream().filter(f -> name.equals(f.getAnalyzer())).toList());
                if (to.getDiagnoses() instanceof DefaultQuizDiagnoses d && from.getDiagnoses() instanceof QuizDiagnoses s) {
                    switch (name) {
                        case "naturalness", "a1-didactic" -> {
                            if ("naturalness".equals(name)) d.setNaturalnessDiagnosis(null);
                            to.getMetadata().remove(name + "OutputIssue");
                            for (String suffix : List.of("Verdict", "Version", "Reused", "InputContract")) {
                                Object value = from.getMetadata().get(name + suffix);
                                to.getMetadata().remove(name + suffix);
                                if (value != null) to.getMetadata().put(name + suffix, value);
                            }
                            var versions = new LinkedHashMap<String, Object>();
                            if (to.getMetadata().get("verdictVersions") instanceof Map<?,?> prior)
                                prior.forEach((key,value) -> { if (!name.equals(key.toString())) versions.put(key.toString(),value); });
                            if (from.getMetadata().get(name + "Version") != null) versions.put(name,from.getMetadata().get(name + "Version"));
                            to.getMetadata().put("verdictVersions",versions);
                        }
                        case "pragmatism" -> d.setPragmatismDiagnosis(s.getPragmatismDiagnosis().orElse(null));
                        case "multiple-choice-quality" -> d.setChoiceQualityDiagnosis(s.getChoiceQualityDiagnosis().orElse(null));
                        case "quiz-instruction" -> d.setQuizInstructionDiagnosis(s.getQuizInstructionDiagnosis().orElse(null));
                        case "single-answer" -> d.setSingleAnswerDiagnosis(s.getSingleAnswerDiagnosis().orElse(null));
                        case "translation-sense" -> d.setTranslationSenseDiagnosis(s.getTranslationSenseDiagnosis().orElse(null));
                        case "multiple-choice-validity" -> d.setMultipleChoiceValidityDiagnosis(s.getMultipleChoiceValidityDiagnosis().orElse(null));
                        case "mini-theory" -> d.setMiniTheoryDiagnosis(s.getMiniTheoryDiagnosis().orElse(null));
                        case "near-duplicates" -> d.setNearDuplicateDiagnosis(s.getNearDuplicateDiagnosis().orElse(null));
                        default -> throw new IllegalArgumentException("Sin diagnóstico de integración para " + name);
                    }
                }
                var origins = new LinkedHashMap<String, Object>();
                Object previous = to.getMetadata().get(PROVENANCE);
                if (previous instanceof Map<?, ?> map) map.forEach((k, v) -> origins.put(k.toString(), v));
                if (preserveProvenance) {
                    Object sourceOrigins = from.getMetadata().get(PROVENANCE);
                    Object sourceOrigin = sourceOrigins instanceof Map<?, ?> map ? map.get(name) : null;
                    if (sourceOrigin != null) origins.put(name, sourceOrigin);
                    else {
                        // Older reports stamp versions on coverage instead of per-quiz provenance.
                        // Retain that historical identity; never substitute today's judge or time.
                        var historical = new LinkedHashMap<String, Object>();
                        historical.put("auditId", supplement.auditId());
                        historical.put("judgeVersion", version(from, name, supplement.report()));
                        historical.put("runtime", historicalRuntime(supplement.report(), name));
                        historical.put("integrated", false);
                        historical.put("fingerprint", fingerprint(name, from));
                        origins.put(name, historical);
                    }
                    to.getMetadata().put(PROVENANCE, origins);
                    continue;
                }
                var origin = new LinkedHashMap<String, Object>();
                origin.put("runId", supplement.runId()); origin.put("auditId", supplement.auditId());
                origin.put("runtime", supplement.runtime()); origin.put("measuredAt", supplement.measuredAt());
                origin.put("fingerprint", fingerprint(name, from)); origin.put("integrated", true);
                origin.put("judgeVersion", version(from, name, supplement.report()));
                origins.put(name, origin); to.getMetadata().put(PROVENANCE, origins);
            }
            if (applied) {
                ran.add(name);
                // Aggregated scores are rebuilt from the selected quiz results, never copied from a partial run.
                for (AuditNode node : target.values()) if (node.getTarget() != AuditTarget.QUIZ) node.getScores().remove(name);
                if (supplement.report().getRoot().getDiagnoses() instanceof CourseDiagnoses d)
                    safe(d.getJudgeCoverages()).stream().filter(c -> name.equals(c.getJudgeName())).findFirst()
                            .ifPresent(c -> coverages.put(name, c));
            }
        }
        var diagnosis = new DefaultCourseDiagnoses();
        // Only coverage is replaced; primary course diagnoses remain available on the copied root.
        if (root.getDiagnoses() instanceof DefaultCourseDiagnoses d) diagnosis = d;
        diagnosis.setJudgeCoverages(new ArrayList<>());
        for (var c : coverages.values()) {
            if (!inputs.containsKey(c.getJudgeName())) { diagnosis.addJudgeCoverage(c); continue; }
            int reached = 0, evaluated = 0;
            var versions = new LinkedHashMap<String, Integer>();
            var topicUnits = new LinkedHashMap<String, Boolean>();
            var versionUnits = new HashSet<String>();
            for (var node : target.values()) if (eligible(node, c.getJudgeName())) {
                reached++;
                String unit = c.getUnit() == AuditTarget.KNOWLEDGE
                        ? node.ancestor(AuditTarget.KNOWLEDGE).map(n -> n.getEntity().getId()).orElse(node.getEntity().getId())
                        : node.getEntity().getId();
                topicUnits.merge(unit, evaluated(node, c.getJudgeName()), (a,b) -> a && b);
                if (evaluated(node, c.getJudgeName())) {
                    evaluated++;
                    String version = version(node, c.getJudgeName(), primary);
                    if (version != null && versionUnits.add(unit + "|" + version)) versions.merge(version, 1, Integer::sum);
                }
                else if (!node.getUnevaluatedBy().contains(c.getJudgeName())
                        && !node.getPartiallyEvaluatedBy().contains(c.getJudgeName())
                        && !structuredVerdict(node,c.getJudgeName())) node.getUnevaluatedBy().add(c.getJudgeName());
            }
            int evaluatedUnits = (int) topicUnits.values().stream().filter(Boolean::booleanValue).count();
            diagnosis.addJudgeCoverage(new JudgeCoverageDiagnosis(c.getJudgeName(), c.getUnit(), topicUnits.size(), evaluatedUnits,
                    topicUnits.size() - evaluatedUnits, List.of(), c.getStanding(), versions.entrySet().stream().map(e -> {
                        var saved = safe(c.getVersions()).stream().filter(v -> e.getKey().equals(v.getJudgeVersion())).findFirst();
                        return new JudgeVersionStanding(e.getKey(), e.getValue(), saved.map(JudgeVersionStanding::getExamStatus).orElse(null),
                                saved.map(JudgeVersionStanding::getRuntimeIdentity).orElse(null));
                    }).toList(), c.getUnavailableReason(), null,
                    List.of(), new JudgeQuizTally(reached, evaluated, reached - evaluated)));
        }
        root.setDiagnoses(diagnosis);
        for (String name : List.of("naturalness", "a1-didactic")) if (ran.contains(name))
            rebuildStructuredCounts(root,target.values(),name);
        aggregator.aggregate(root);
        numbers.compute(root, catalog.list().stream().filter(c -> ran.contains(c.getName())).toList());
        return new AuditReport(root);
    }

    private static Object historicalRuntime(AuditReport report, String analyzer) {
        if (report.getRoot().getDiagnoses() instanceof CourseDiagnoses diagnoses) {
            var recorded = safe(diagnoses.getJudgeCoverages()).stream()
                    .filter(coverage -> analyzer.equals(coverage.getJudgeName()))
                    .flatMap(coverage -> safe(coverage.getVersions()).stream())
                    .filter(version -> version.getVerdictCount() > 0)
                    .map(JudgeVersionStanding::getRuntimeIdentity).distinct().toList();
            if (recorded.size() == 1) return recorded.get(0).toString();
        }
        return null;
    }

    private static boolean structuredVerdict(AuditNode node, String name) {
        if ("pragmatism".equals(name) && node.getDiagnoses() instanceof QuizDiagnoses diagnoses)
            return diagnoses.getPragmatismDiagnosis().map(d -> d.verdict() != null).orElse(false);
        return ("naturalness".equals(name) || "a1-didactic".equals(name))
                && com.learney.contentaudit.auditdomain.englishquality.EnglishQualitySubjectBuilder.CONTRACT
                    .equals(node.getMetadata().get(name + "InputContract"))
                && node.getMetadata().get(name + "Verdict") != null;
    }
    private static void rebuildStructuredCounts(AuditNode root, Collection<AuditNode> nodes, String name) {
        var counts = new LinkedHashMap<String,Integer>();
        for (var node : nodes) if (eligible(node,name)) {
            Object verdict = node.getMetadata().get(name + "Verdict");
            if (!structuredVerdict(node,name)) { counts.merge("PENDING_PROVIDER_OR_BUDGET",1,Integer::sum); continue; }
            if (verdict instanceof com.learney.contentaudit.auditdomain.englishquality.NativeUsageVerdict nativeVerdict)
                nativeVerdict.assessments().forEach(a -> counts.merge(a.status().name(),1,Integer::sum));
            else if (verdict instanceof com.learney.contentaudit.auditdomain.englishquality.A1DidacticVerdict didacticVerdict)
                counts.merge(didacticVerdict.status().name(),1,Integer::sum);
            else if (verdict instanceof Map<?,?> saved) {
                if ("naturalness".equals(name) && saved.get("assessments") instanceof List<?> assessments)
                    for (Object item : assessments) if (item instanceof Map<?,?> a && a.get("status") != null)
                        counts.merge(a.get("status").toString(),1,Integer::sum);
                if ("a1-didactic".equals(name) && saved.get("status") != null)
                    counts.merge(saved.get("status").toString(),1,Integer::sum);
            }
        }
        root.getMetadata().put(name + "Counts",counts);
    }

    public static boolean evaluated(AuditNode n, String name) {
        return n.getScores() != null && n.getScores().containsKey(name)
                && !safe(n.getUnevaluatedBy()).contains(name) && !safe(n.getPartiallyEvaluatedBy()).contains(name);
    }
    /** A saved abstention is integrable evidence, while remaining unscored and pending editorial review. */
    public static boolean hasStoredOpinion(AuditNode node, String analyzer) {
        return evaluated(node, analyzer) || structuredVerdict(node, analyzer);
    }
    /**
     * F-ALDIA-R003 inv. 6: why a saved verdict carries no score -- the judge said it does not apply, that it lacks
     * context or that it needs a review --, or empty when it scored or there is no verdict. A verdict without a score
     * and without a known reason is left for review.
     */
    public static Optional<JudgeAbstention> abstention(AuditNode node, String analyzer) {
        if (node == null || evaluated(node, analyzer) || !structuredVerdict(node, analyzer)) return Optional.empty();
        var statuses = new HashSet<String>();
        if ("pragmatism".equals(analyzer) && node.getDiagnoses() instanceof QuizDiagnoses d)
            d.getPragmatismDiagnosis().ifPresent(p -> p.verdict().assessments().forEach(a -> statuses.add(a.status().name())));
        Object verdict = node.getMetadata() == null ? null : node.getMetadata().get(analyzer + "Verdict");
        if (verdict instanceof com.learney.contentaudit.auditdomain.englishquality.NativeUsageVerdict v)
            v.assessments().forEach(a -> statuses.add(a.status().name()));
        else if (verdict instanceof com.learney.contentaudit.auditdomain.englishquality.A1DidacticVerdict v)
            statuses.add(v.status().name());
        else if (verdict instanceof Map<?,?> saved) {
            if (saved.get("status") != null) statuses.add(saved.get("status").toString());
            if (saved.get("assessments") instanceof List<?> assessments)
                for (Object item : assessments) if (item instanceof Map<?,?> a && a.get("status") != null) statuses.add(a.get("status").toString());
        }
        // What can be fixed outside the judge goes first: a missing context, then a review.
        for (var reason : List.of(JudgeAbstention.NEEDS_CONTEXT, JudgeAbstention.REVIEW, JudgeAbstention.NOT_APPLICABLE))
            if (statuses.contains(reason.name())) return Optional.of(reason);
        return Optional.of(JudgeAbstention.REVIEW);
    }
    /**
     * F-ALDIA-R003 inv. 6: the saved abstentions that a judge of exercises left pending in {@code report}. Its coverage
     * counts as evaluated only what it closed, so these are measured although the coverage says not evaluated.
     */
    public static int pendingAbstentions(AuditReport report, String analyzer) {
        if (report == null || report.getRoot() == null) return 0;
        return (int) index(report.getRoot()).values().stream().filter(n -> n.getTarget() == AuditTarget.QUIZ
                && (safe(n.getUnevaluatedBy()).contains(analyzer) || safe(n.getPartiallyEvaluatedBy()).contains(analyzer))
                && abstention(n, analyzer).isPresent()).count();
    }
    public static String version(AuditNode n) {
        return n.getDiagnoses() instanceof QuizDiagnoses d
                ? d.getChoiceQualityDiagnosis().map(x -> x.evaluatorVersion()).orElse(null) : null;
    }
    public static String version(AuditNode node, String analyzer, AuditReport source) {
        Object origins = node.getMetadata() == null ? null : node.getMetadata().get(PROVENANCE);
        Object origin = origins instanceof Map<?,?> map ? map.get(analyzer) : null;
        if (origin instanceof Map<?,?> map && map.containsKey("judgeVersion"))
            return map.get("judgeVersion") instanceof String version ? version : null;
        if (ChoiceQualityAnalyzerProvider.NAME.equals(analyzer)) return version(node);
        if ("pragmatism".equals(analyzer) && node.getDiagnoses() instanceof QuizDiagnoses d)
            return d.getPragmatismDiagnosis().map(x -> x.evaluatorVersion()).orElse(null);
        Object recordedVersion = node.getMetadata() == null ? null : node.getMetadata().get(analyzer + "Version");
        if (recordedVersion instanceof String version) return version;
        Object versions = node.getMetadata() == null ? null : node.getMetadata().get("verdictVersions");
        if (versions instanceof Map<?,?> map && map.get(analyzer) instanceof String version) return version;
        if (source.getRoot().getDiagnoses() instanceof CourseDiagnoses d) {
            var versionsInRun = safe(d.getJudgeCoverages()).stream().filter(c -> analyzer.equals(c.getJudgeName()))
                    .flatMap(c -> safe(c.getVersions()).stream()).filter(v -> v.getVerdictCount() > 0)
                    .map(JudgeVersionStanding::getJudgeVersion).filter(Objects::nonNull).distinct().toList();
            if (versionsInRun.size() == 1) return versionsInRun.get(0);
        }
        return null;
    }
    public static boolean eligible(AuditNode node, String analyzer) {
        if (!(node.getEntity() instanceof AuditableQuiz q)) return false;
        return switch (analyzer) {
            case "a1-didactic" -> "A1".equalsIgnoreCase(com.learney.contentaudit.auditdomain.englishquality.EnglishQualitySubjectBuilder.level(node));
            case "multiple-choice-quality", "multiple-choice-validity" -> q.getMultipleChoice() != null;
            case "single-answer", "hint-coherence", "answer-completeness" -> q.getMultipleChoice() == null;
            case "mini-theory" -> node.ancestor(AuditTarget.KNOWLEDGE).stream().flatMap(k -> safe(k.getChildren()).stream())
                    .anyMatch(n -> n.getEntity() instanceof AuditableQuiz quiz && quiz.getMiniTheory() != null && !quiz.getMiniTheory().isBlank());
            default -> true;
        };
    }
    public static Map<String, AuditNode> index(AuditNode root) {
        var result = new LinkedHashMap<String, AuditNode>(); index(root, result); return result;
    }
    private static void index(AuditNode n, Map<String, AuditNode> result) {
        if (n == null) return;
        result.put(n.getEntity() == null ? "root" : n.getEntity().getId(), n);
        for (var child : safe(n.getChildren())) index(child, result);
    }
    private static AuditNode copy(AuditNode n, AuditNode parent) {
        var result = new AuditNode(n.getEntity(), n.getTarget(), parent, new ArrayList<>(),
                new LinkedHashMap<>(n.getScores() == null ? Map.of() : n.getScores()),
                new LinkedHashMap<>(n.getMetadata() == null ? Map.of() : n.getMetadata()), copyDiagnoses(n.getDiagnoses()));
        if (result.getDiagnoses() == null && n.getTarget() == AuditTarget.QUIZ) result.setDiagnoses(new DefaultQuizDiagnoses());
        result.setFindings(new ArrayList<>(safe(n.getFindings())));
        result.setUnevaluatedBy(new ArrayList<>(safe(n.getUnevaluatedBy())));
        result.setPartiallyEvaluatedBy(new ArrayList<>(safe(n.getPartiallyEvaluatedBy())));
        result.setMeasures(new ArrayList<>(safe(n.getMeasures())));
        for (var child : safe(n.getChildren())) result.getChildren().add(copy(child, result));
        return result;
    }
    private static NodeDiagnoses copyDiagnoses(NodeDiagnoses source) {
        if (source instanceof QuizDiagnoses s) {
            var d = new DefaultQuizDiagnoses();
            d.setPragmatismDiagnosis(s.getPragmatismDiagnosis().orElse(null));
            d.setChoiceQualityDiagnosis(s.getChoiceQualityDiagnosis().orElse(null));
            d.setSentenceLengthDiagnosis(s.getSentenceLengthDiagnosis().orElse(null));
            d.setLemmaAbsenceDiagnosis(s.getLemmaAbsenceDiagnosis().orElse(null));
            d.setQuizInstructionDiagnosis(s.getQuizInstructionDiagnosis().orElse(null));
            d.setHintCoherenceDiagnosis(s.getHintCoherenceDiagnosis().orElse(null));
            d.setAnswerCompletenessDiagnosis(s.getAnswerCompletenessDiagnosis().orElse(null));
            d.setMultipleChoiceValidityDiagnosis(s.getMultipleChoiceValidityDiagnosis().orElse(null));
            d.setDataCoherenceDiagnosis(s.getDataCoherenceDiagnosis().orElse(null));
            d.setExactDuplicateDiagnosis(s.getExactDuplicateDiagnosis().orElse(null));
            d.setTranslationSenseDiagnosis(s.getTranslationSenseDiagnosis().orElse(null));
            d.setSingleAnswerDiagnosis(s.getSingleAnswerDiagnosis().orElse(null));
            d.setMiniTheoryDiagnosis(s.getMiniTheoryDiagnosis().orElse(null));
            d.setOffLevelWordsDiagnosis(s.getOffLevelWordsDiagnosis().orElse(null));
            d.setGrammarDiagnosis(s.getGrammarDiagnosis().orElse(null));
            d.setNaturalnessDiagnosis(s.getNaturalnessDiagnosis().orElse(null));
            d.setNearDuplicateDiagnosis(s.getNearDuplicateDiagnosis().orElse(null));
            return d;
        }
        if (source instanceof CourseDiagnoses s) {
            var d = new DefaultCourseDiagnoses();
            d.setLemmaAbsenceDiagnosis(s.getLemmaAbsenceDiagnosis().orElse(null));
            d.setCocaBucketsDiagnosis(s.getCocaBucketsDiagnosis().orElse(null));
            d.setLemmaCountDiagnosis(s.getLemmaCountDiagnosis().orElse(null));
            d.setQuizInstructionCoverage(s.getQuizInstructionCoverage().orElse(null));
            d.setLemmaRecurrenceDiagnosis(s.getLemmaRecurrenceDiagnosis().orElse(null));
            d.setLocalToolRuns(new ArrayList<>(safe(s.getLocalToolRuns())));
            d.setNaturalnessQueue(s.getNaturalnessQueue().orElse(null));
            d.setJudgeCoverages(new ArrayList<>(safe(s.getJudgeCoverages())));
            return d;
        }
        return source;
    }
    private static <T> List<T> safe(List<T> list) { return list == null ? List.of() : list; }
}
