package com.learney.contentaudit.auditdomain.analysis;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.choicequality.ChoiceQualityAnalyzerProvider;
import com.learney.contentaudit.auditdomain.choicequality.ChoiceQualitySubjectBuilder;
import com.learney.contentaudit.auditdomain.judging.*;
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
                "multiple-choice-quality", qualityInputs::build,
                "quiz-instruction", new com.learney.contentaudit.auditdomain.quizinstructionengine.DefaultQuizInstructionSubjectBuilder(renderer)::build,
                "single-answer", new com.learney.contentaudit.auditdomain.singleanswerengine.DefaultSingleAnswerSubjectBuilder(renderer)::build,
                "translation-sense", new com.learney.contentaudit.auditdomain.translationsenseengine.DefaultTranslationSenseSubjectBuilder(renderer)::build,
                "multiple-choice-validity", new com.learney.contentaudit.auditdomain.choicealternativeengine.DefaultChoiceAlternativeSubjectBuilder(renderer)::build,
                "mini-theory", n -> new com.learney.contentaudit.auditdomain.minitheoryengine.DefaultMiniTheorySubjectBuilder(renderer)
                        .build(n.getTarget() == AuditTarget.KNOWLEDGE ? n : n.ancestor(AuditTarget.KNOWLEDGE).orElseThrow()));
    }
    @Override public Set<String> supportedAnalyzers() { return inputs.keySet(); }

    @Override public String fingerprint(String analyzer, AuditNode node) {
        if (!inputs.containsKey(analyzer))
            throw new IllegalArgumentException("Este analizador todavía no declara su huella de integración: " + analyzer);
        return fingerprinter.fingerprint(inputs.get(analyzer).apply(node).getContent());
    }

    @Override public AuditReport compose(AuditReport primary, List<AnalysisSupplement> supplements) {
        Objects.requireNonNull(primary, "Falta el análisis de partida");
        AuditNode root = copy(primary.getRoot(), null);
        Map<String, AuditNode> target = index(root);
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
                if (from == null || to == null || !evaluated(from, name)) continue;
                if (!Objects.equals(fingerprint(name, from), fingerprint(name, to))) continue;
                applied = true;
                to.getScores().put(name, from.getScores().get(name));
                to.getUnevaluatedBy().remove(name);
                to.getPartiallyEvaluatedBy().remove(name);
                to.getFindings().removeIf(f -> name.equals(f.getAnalyzer()));
                to.getFindings().addAll(from.getFindings().stream().filter(f -> name.equals(f.getAnalyzer())).toList());
                if (to.getDiagnoses() instanceof DefaultQuizDiagnoses d && from.getDiagnoses() instanceof QuizDiagnoses s) {
                    switch (name) {
                        case "multiple-choice-quality" -> d.setChoiceQualityDiagnosis(s.getChoiceQualityDiagnosis().orElse(null));
                        case "quiz-instruction" -> d.setQuizInstructionDiagnosis(s.getQuizInstructionDiagnosis().orElse(null));
                        case "single-answer" -> d.setSingleAnswerDiagnosis(s.getSingleAnswerDiagnosis().orElse(null));
                        case "translation-sense" -> d.setTranslationSenseDiagnosis(s.getTranslationSenseDiagnosis().orElse(null));
                        case "multiple-choice-validity" -> d.setMultipleChoiceValidityDiagnosis(s.getMultipleChoiceValidityDiagnosis().orElse(null));
                        case "mini-theory" -> d.setMiniTheoryDiagnosis(s.getMiniTheoryDiagnosis().orElse(null));
                        default -> throw new IllegalArgumentException("Sin diagnóstico de integración para " + name);
                    }
                }
                var origins = new LinkedHashMap<String, Object>();
                Object previous = to.getMetadata().get(PROVENANCE);
                if (previous instanceof Map<?, ?> map) map.forEach((k, v) -> origins.put(k.toString(), v));
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
                else if (!node.getUnevaluatedBy().contains(c.getJudgeName())) node.getUnevaluatedBy().add(c.getJudgeName());
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
        aggregator.aggregate(root);
        numbers.compute(root, catalog.list().stream().filter(c -> ran.contains(c.getName())).toList());
        return new AuditReport(root);
    }

    public static boolean evaluated(AuditNode n, String name) {
        return n.getScores() != null && n.getScores().containsKey(name)
                && !safe(n.getUnevaluatedBy()).contains(name) && !safe(n.getPartiallyEvaluatedBy()).contains(name);
    }
    public static String version(AuditNode n) {
        return n.getDiagnoses() instanceof QuizDiagnoses d
                ? d.getChoiceQualityDiagnosis().map(x -> x.evaluatorVersion()).orElse(null) : null;
    }
    public static String version(AuditNode node, String analyzer, AuditReport source) {
        Object origins = node.getMetadata() == null ? null : node.getMetadata().get(PROVENANCE);
        Object origin = origins instanceof Map<?,?> map ? map.get(analyzer) : null;
        if (origin instanceof Map<?,?> map && map.get("judgeVersion") instanceof String version) return version;
        if (ChoiceQualityAnalyzerProvider.NAME.equals(analyzer)) return version(node);
        Object versions = node.getMetadata() == null ? null : node.getMetadata().get("verdictVersions");
        if (versions instanceof Map<?,?> map && map.get(analyzer) instanceof String version) return version;
        if (source.getRoot().getDiagnoses() instanceof CourseDiagnoses d) {
            var versionsInRun = safe(d.getJudgeCoverages()).stream().filter(c -> analyzer.equals(c.getJudgeName()))
                    .flatMap(c -> safe(c.getVersions()).stream()).map(JudgeVersionStanding::getJudgeVersion).filter(Objects::nonNull).distinct().toList();
            if (versionsInRun.size() == 1) return versionsInRun.get(0);
        }
        return null;
    }
    public static boolean eligible(AuditNode node, String analyzer) {
        if (!(node.getEntity() instanceof AuditableQuiz q)) return false;
        return switch (analyzer) {
            case "multiple-choice-quality", "multiple-choice-validity" -> q.getMultipleChoice() != null;
            case "single-answer" -> q.getMultipleChoice() == null;
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
