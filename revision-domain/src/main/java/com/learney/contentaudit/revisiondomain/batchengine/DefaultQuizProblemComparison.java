package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.analysis.DefaultAnalysisComposer;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.auditdomain.judging.JudgeFreshnessReference;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.batch.QuizCorrection.*;
import java.util.*;

/** Compares recorded observations. Missing coverage is never evidence that a problem disappeared. */
final class DefaultQuizProblemComparison {
    private final BatchServiceConfig config;
    DefaultQuizProblemComparison(BatchServiceConfig config) { this.config = config; }

    Comparison compare(CorrectionBatch batch, SelectiveCorrection.Run run, AuditReport before, AuditReport after,
            boolean exactResult) {
        var warnings = new ArrayList<String>();
        if (before == null) warnings.add("Falta el análisis guardado antes de esta pasada; no se pueden confirmar mejoras.");
        if (!exactResult) warnings.add(run.finishedAt() == null
                ? "La pasada todavía no terminó: la comparación de problemas queda pendiente."
                : "El contenido actual ya no coincide con el resultado de esta pasada; no se atribuyen cambios posteriores a ella.");
        warnings.add("Los controles sin cobertura completa, sin una medida comparable o con distinta versión o motor quedan pendientes de comparación.");
        warnings.add("Los hallazgos con identidad estable se comparan individualmente; si su identidad falta o se repite, se agrupan por regla conservando todos sus motivos.");
        if (exactResult) warnings.add("El antes es la foto guardada de esta pasada; el después usa los resultados disponibles para ese contenido, sin nuevas consultas.");
        var a = index(before); var b = index(after);
        var reached = taskReach(run);
        var quizzes = new ArrayList<QuizEvolution>();
        for (String id : safe(run.quizIds())) {
            var left = groups(before, id, reached); var right = groups(after, id, reached);
            var keys = new TreeSet<Group>(); keys.addAll(left.keySet()); keys.addAll(right.keySet());
            var rows = new ArrayList<ProblemEvolution>();
            var represented = new HashSet<String>();
            for (Group key : keys) {
                represented.add(key.analyzer());
                var old = left.getOrDefault(key, List.of()); var now = right.getOrDefault(key, List.of());
                if (uniqueIdentities(old) && uniqueIdentities(now)) {
                    var identities = new TreeSet<String>();
                    old.forEach(f -> identities.add(identity(f))); now.forEach(f -> identities.add(identity(f)));
                    for (String identity : identities) rows.add(row(key,
                            old.stream().filter(f -> identity.equals(identity(f))).toList(),
                            now.stream().filter(f -> identity.equals(identity(f))).toList(),
                            a.get(key.nodeId()), b.get(key.nodeId()), before, after));
                } else rows.add(row(key, old, now, a.get(key.nodeId()), b.get(key.nodeId()), before, after));
            }
            // A control with no findings can still be awaiting a verdict. It must remain visible.
            // A batch can also run controls of topics or the whole course. Only an
            // observation on this quiz proves that it was awaiting this control.
            // Relevant parent findings already enter through the groups above.
            var controls = new TreeSet<String>();
            controls.addAll(controls(a.get(id))); controls.addAll(controls(b.get(id)));
            for (String analyzer : controls) if (!represented.contains(analyzer)) {
                var ac = coverage(a.get(id), analyzer); var bc = coverage(b.get(id), analyzer);
                if ("INAPPLICABLE".equals(ac) && "INAPPLICABLE".equals(bc)) continue;
                var control = row(new Group(analyzer, null, id), List.of(), List.of(), a.get(id), b.get(id), before, after);
                if (!"CLEAR".equals(control.status())) rows.add(control);
            }
            rows.sort(Comparator.comparingInt((ProblemEvolution row) -> rank(row.status()))
                    .thenComparing(ProblemEvolution::analyzer).thenComparing(r -> Objects.toString(r.rule(), "")));
            quizzes.add(new QuizEvolution(id, List.copyOf(rows)));
        }
        return new Comparison(batch.getId(), run.id(), run.contentVersion(), run.resultContentVersion(),
                batch.getContentVersion(), exactResult, run.auditId(), "Antes de esta pasada · v" + run.contentVersion(),
                exactResult ? "Propuesta de esta pasada · v" + run.resultContentVersion()
                        : "Resultado de esta pasada sin comparación disponible", List.copyOf(warnings), List.copyOf(quizzes));
    }

    private ProblemEvolution row(Group key, List<Finding> before, List<Finding> after,
            AuditNode a, AuditNode b, AuditReport beforeReport, AuditReport afterReport) {
        String ac = coverage(a, key.analyzer()), bc = coverage(b, key.analyzer());
        String av = version(a, key.analyzer(), beforeReport), bv = version(b, key.analyzer(), afterReport);
        String ar = runtime(a, key.analyzer(), beforeReport), br = runtime(b, key.analyzer(), afterReport);
        String am = measure(beforeReport, key.analyzer()), bm = measure(afterReport, key.analyzer());
        FindingSeverity old = severity(before), now = severity(after);
        boolean judge = judge(key.analyzer(), beforeReport, afterReport, av, bv, ar, br);
        String status, reason;
        if (!"EVALUATED".equals(ac) || !"EVALUATED".equals(bc)) {
            status = "PENDING"; reason = "Falta una medición completa de este control en ambos lados; la ausencia de hallazgos no confirma una mejora.";
        } else if (am == null || bm == null || !am.equals(bm)) {
            status = "PENDING"; reason = am == null || bm == null
                    ? "No quedó registrada la medida de este control en ambos lados; no se puede confirmar que usó las mismas reglas."
                    : "Cambió la medida de este control (reglas, configuración o herramienta); no se atribuye esa diferencia a la propuesta.";
        } else if ((judge && (av == null || bv == null || ar == null || br == null))
                || ((av != null || bv != null) && !Objects.equals(av, bv))
                || ((ar != null || br != null) && !Objects.equals(ar, br))) {
            status = "PENDING"; reason = "Las versiones o los motores de este juez no coinciden o no están registrados; los resultados no son comparables.";
        } else if (before.isEmpty() && !after.isEmpty()) {
            status = "NEW"; reason = "Esta regla no señalaba problemas antes y ahora sí, con medición completa y comparable.";
        } else if (!before.isEmpty() && after.isEmpty()) {
            status = "RESOLVED"; reason = "Este control volvió a evaluar el contenido y ya no señala problemas de esta regla.";
        } else if (before.isEmpty()) {
            status = "CLEAR"; reason = "Sin problemas en ambos lados.";
        } else if (before.stream().anyMatch(f -> f.getSeverity() == null) || after.stream().anyMatch(f -> f.getSeverity() == null)) {
            status = "PENDING"; reason = "La gravedad de los hallazgos no quedó registrada en ambos lados.";
        } else if (severityRank(now) > severityRank(old)) {
            status = "AGGRAVATED"; reason = "La gravedad máxima de esta regla aumentó.";
        } else if (severityRank(now) < severityRank(old)) {
            status = "IMPROVED"; reason = "La gravedad máxima bajó, pero esta regla todavía señala problemas.";
        } else if (after.size() > before.size()) {
            status = "AGGRAVATED"; reason = "Esta regla señala más problemas con la misma gravedad máxima.";
        } else if (after.size() < before.size()) {
            status = "IMPROVED"; reason = "Esta regla señala menos problemas, aunque todavía quedan observaciones.";
        } else {
            status = "PERSISTING"; reason = "Esta regla sigue señalando problemas con la misma gravedad máxima; sus motivos pueden haber cambiado.";
        }
        if (before.size() > 1 || after.size() > 1)
            reason += " Comparación del grupo de hallazgos; no se emparejan motivos por su texto.";
        return new ProblemEvolution(key.analyzer(), key.rule(), status, reason, old, now,
                List.copyOf(before), List.copyOf(after), ac, bc, av, bv, ar, br, am, bm);
    }

    private static Map<String, AuditNode> index(AuditReport report) {
        return report == null || report.getRoot() == null ? Map.of() : DefaultAnalysisComposer.index(report.getRoot());
    }
    private record Group(String analyzer, String rule, String nodeId) implements Comparable<Group> {
        @Override public int compareTo(Group other) {
            return Comparator.comparing(Group::analyzer).thenComparing(g -> Objects.toString(g.rule(), ""))
                    .thenComparing(g -> Objects.toString(g.nodeId(), "")).compare(this, other);
        }
    }
    private static Map<Group, List<Finding>> groups(AuditReport report, String quizId, Map<String, Set<String>> reached) {
        var result = new TreeMap<Group, List<Finding>>();
        for (AuditNode node : index(report).values()) for (Finding finding : safe(node.getFindings())) {
            if (com.learney.contentaudit.auditdomain.datacoherence.DataCoherenceStage.isAudioFinding(finding)) continue;
            if (finding == null || finding.getAnalyzer() == null || finding.getRule() == null || finding.getNode() == null) continue;
            boolean own = finding.getNode().getTarget() == AuditTarget.QUIZ && quizId.equals(finding.getNode().getNodeId());
            boolean related = finding.getEvidence() != null && safe(finding.getEvidence().getRelatedNodes()).stream()
                    .anyMatch(n -> n != null && n.getTarget() == AuditTarget.QUIZ && quizId.equals(n.getNodeId()));
            boolean reachedByTask = reached.getOrDefault(reachKey(finding), Set.of()).contains(quizId);
            if (!own && !related && !reachedByTask) continue;
            var group = new Group(finding.getAnalyzer(), finding.getRule(), finding.getNode().getNodeId());
            var values = result.computeIfAbsent(group, ignored -> new ArrayList<>());
            if (!values.contains(finding)) values.add(finding);
        }
        return result;
    }
    private Map<String, Set<String>> taskReach(SelectiveCorrection.Run run) {
        var result = new HashMap<String, Set<String>>();
        if (config.getRefinementPlanStore() == null || run.planId() == null) return result;
        config.getRefinementPlanStore().load(run.planId()).ifPresent(plan -> {
            var taskIds = new HashSet<>(safe(run.taskIds()));
            for (var task : safe(plan.getTasks())) if (taskIds.contains(task.getId()) && task.getOrigin() != null) {
                var findings = new ArrayList<>(safe(task.getOrigin().getCoveredFindings()));
                if (task.getOrigin().getFinding() != null) findings.add(task.getOrigin().getFinding());
                var quizzes = new HashSet<>(safe(task.getOrigin().getReachedQuizIds()));
                if (task.getNodeTarget() == AuditTarget.QUIZ && task.getNodeId() != null) quizzes.add(task.getNodeId());
                quizzes.retainAll(safe(run.quizIds()));
                for (var finding : findings) result.computeIfAbsent(reachKey(finding), ignored -> new HashSet<>()).addAll(quizzes);
            }
        });
        return result;
    }
    private static String reachKey(Finding finding) {
        if (identity(finding) != null) return identity(finding);
        return finding.getAnalyzer() + "|" + finding.getRule() + "|"
                + (finding.getNode() == null ? "" : finding.getNode().getNodeId());
    }
    private static String identity(Finding finding) {
        return finding.getIdentity() == null ? null : blank(finding.getIdentity().getKey());
    }
    private static boolean uniqueIdentities(List<Finding> findings) {
        var seen = new HashSet<String>();
        for (var finding : findings) if (identity(finding) == null || !seen.add(identity(finding))) return false;
        return true;
    }
    private static Set<String> controls(AuditNode node) {
        var names = new HashSet<String>();
        if (node != null) {
            if (node.getScores() != null) names.addAll(node.getScores().keySet());
            names.addAll(safe(node.getUnevaluatedBy())); names.addAll(safe(node.getPartiallyEvaluatedBy()));
        }
        names.remove(null); return names;
    }
    private static String coverage(AuditNode node, String analyzer) {
        if (node == null) return "UNKNOWN";
        if (safe(node.getPartiallyEvaluatedBy()).contains(analyzer)) return "PARTIAL";
        if (safe(node.getUnevaluatedBy()).contains(analyzer) || DefaultAnalysisComposer.abstention(node, analyzer).isPresent()) return "PENDING";
        if (node.getScores() != null && node.getScores().get(analyzer) != null
                && Double.isFinite(node.getScores().get(analyzer))) return "EVALUATED";
        if (node.getTarget() == AuditTarget.QUIZ && !DefaultAnalysisComposer.eligible(node, analyzer)) return "INAPPLICABLE";
        return "UNKNOWN";
    }
    private boolean judge(String name, AuditReport before, AuditReport after, String av, String bv, String ar, String br) {
        if (config.getAnalyzerCatalog() != null && config.getAnalyzerCatalog().find(name)
                .map(d -> d.getCost() == AnalysisCost.INSTANT).orElse(false)) return false;
        if (av != null || bv != null || ar != null || br != null) return true;
        if (hasJudge(before, name) || hasJudge(after, name)) return true;
        if (config.getCurrentAnalysisService() != null && config.getCurrentAnalysisService().supportedAnalyzers().contains(name)) return true;
        // Only a declared deterministic analyzer may compare without evaluator provenance.
        return config.getAnalyzerCatalog() == null || config.getAnalyzerCatalog().find(name)
                .map(d -> d.getCost() != AnalysisCost.INSTANT).orElse(true);
    }
    private static boolean hasJudge(AuditReport report, String name) {
        return report != null && report.getRoot().getDiagnoses() instanceof CourseDiagnoses d
                && safe(d.getJudgeCoverages()).stream().anyMatch(c -> name.equals(c.getJudgeName()));
    }
    private static String version(AuditNode node, String name, AuditReport report) {
        return node == null || report == null ? null : blank(DefaultAnalysisComposer.version(node, name, report));
    }
    private static String measure(AuditReport report, String name) {
        if (report == null || report.getRoot() == null) return null;
        var measures = safe(report.getRoot().getMeasures()).stream().filter(m -> name.equals(m.getAnalyzer())).toList();
        if (measures.isEmpty() || measures.stream().anyMatch(m -> blank(m.getFingerprint()) == null)) return null;
        var fingerprints = measures.stream().map(m -> m.getFingerprint()).distinct().toList();
        return fingerprints.size() == 1 ? fingerprints.get(0) : null;
    }
    private static String runtime(AuditNode node, String name, AuditReport report) {
        if (node == null || report == null) return null;
        Object origins = node.getMetadata() == null ? null : node.getMetadata().get(DefaultAnalysisComposer.PROVENANCE);
        Object origin = origins instanceof Map<?, ?> map ? map.get(name) : null;
        // An explicit unknown origin must remain unknown; standing only grants permission to consult.
        if (origin instanceof Map<?, ?> map) return explicitRuntime(map.get("runtime"));
        if (report.getRoot().getDiagnoses() instanceof CourseDiagnoses d) {
            String version = version(node, name, report);
            if (version == null) return null;
            var measured = safe(d.getJudgeCoverages()).stream().filter(c -> name.equals(c.getJudgeName()))
                    .flatMap(c -> safe(c.getVersions()).stream())
                    .filter(v -> v.getVerdictCount() > 0 && version.equals(v.getJudgeVersion()))
                    .map(v -> v.getRuntimeIdentity()).collect(java.util.stream.Collectors.toSet());
            if (measured.size() == 1) return explicitRuntime(measured.iterator().next());
        }
        return null;
    }
    private static String explicitRuntime(Object value) {
        EvaluationRuntimeIdentity runtime = JudgeFreshnessReference.runtimeOf(value);
        return runtime != null && runtime.isExplicit() ? runtime.toString() : null;
    }
    private static String blank(String value) { return value == null || value.isBlank() ? null : value; }
    private static FindingSeverity severity(List<Finding> findings) {
        return findings.stream().map(Finding::getSeverity).filter(Objects::nonNull)
                .max(Comparator.comparingInt(DefaultQuizProblemComparison::severityRank)).orElse(null);
    }
    private static int severityRank(FindingSeverity severity) {
        if (severity == null) return 0;
        return switch (severity) { case LOW -> 1; case MEDIUM -> 2; case HIGH -> 3; case BLOCKING -> 4; };
    }
    private static int rank(String status) {
        return List.of("NEW", "AGGRAVATED", "PENDING", "PERSISTING", "IMPROVED", "RESOLVED").indexOf(status);
    }
    private static <T> List<T> safe(List<T> values) { return values == null ? List.of() : values; }
}
