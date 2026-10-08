package com.learney.contentaudit.auditdomain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

/** Uses the same lexical analyzers and tree builder as a final audit, but no judges or findings. */
final class DefaultWorkingVocabulary implements WorkingVocabulary {
    private static final List<String> ANALYZERS = List.of("lemma-count", "lemma-absence", "coca-buckets-distribution");
    private final AuditReport report;
    private final List<IncrementalContentAnalyzer> analyzers = new ArrayList<>();
    private final Map<String, AuditNode> knowledges = new HashMap<>();
    private final Map<String, String> quizOwners = new HashMap<>();
    private long version;
    private boolean dirty = true;
    private RuntimeException failure;

    DefaultWorkingVocabulary(AuditableCourse composedCourse, AnalyzerCatalog catalog) {
        // Fail before processing any content when the catalog cannot support a correct context.
        for (String name : ANALYZERS) {
            ContentAnalyzer analyzer = catalog.provider(name).orElseThrow(() ->
                    new IllegalStateException("Falta " + name + " para el contexto léxico")).create(null);
            if (!(analyzer instanceof IncrementalContentAnalyzer incremental)) {
                throw new IllegalStateException(name + " no admite deltas léxicos");
            }
            analyzers.add(incremental);
        }
        report = new AuditReport(IAuditEngine.buildTree(composedCourse));
        IAuditEngine.visit(report.getRoot(), node -> {
            if (node.getTarget() == AuditTarget.KNOWLEDGE) {
                String id = Objects.requireNonNull(node.getEntity().getId(), "knowledge id");
                if (knowledges.putIfAbsent(id, node) != null) throw new IllegalArgumentException("Tema duplicado: " + id);
            } else if (node.getTarget() == AuditTarget.QUIZ) {
                String id = Objects.requireNonNull(node.getEntity().getId(), "quiz id");
                String owner = node.getParent().getEntity().getId();
                if (quizOwners.putIfAbsent(id, owner) != null) throw new IllegalArgumentException("Ejercicio duplicado: " + id);
            }
        });
        for (IncrementalContentAnalyzer analyzer : analyzers) {
            IAuditEngine.visit(report.getRoot(), node -> IAuditEngine.notify(analyzer, node));
        }
    }

    @Override
    public synchronized void replaceKnowledge(AuditableKnowledge changed) {
        ensureHealthy();
        Objects.requireNonNull(changed, "knowledge");
        AuditNode knowledge = knowledges.get(changed.getId());
        if (knowledge == null) throw new IllegalArgumentException("Tema no presente: " + changed.getId());
        Set<String> ids = new HashSet<>();
        for (AuditableQuiz quiz : changed.getQuizzes()) {
            String id = Objects.requireNonNull(quiz.getId(), "quiz id");
            String owner = quizOwners.get(id);
            if (!ids.add(id) || owner != null && !owner.equals(changed.getId())) {
                throw new IllegalArgumentException("Ejercicio duplicado: " + id);
            }
        }
        if (changed.equals(knowledge.getEntity())) return;
        try {
            Map<String, AuditNode> previous = new LinkedHashMap<>();
            knowledge.getChildren().forEach(node -> previous.put(node.getEntity().getId(), node));
            List<AuditNode> children = new ArrayList<>();
            for (AuditableQuiz quiz : changed.getQuizzes()) {
                AuditNode old = previous.remove(quiz.getId());
                if (old != null && quiz.equals(old.getEntity())) {
                    children.add(old);
                } else {
                    AuditNode node = IAuditEngine.quizNode(quiz, knowledge);
                    // Each analyzer upserts, removing the old contribution before adding this one.
                    analyzers.forEach(analyzer -> analyzer.onQuiz(node));
                    children.add(node);
                }
                quizOwners.put(quiz.getId(), changed.getId());
            }
            for (String id : previous.keySet()) {
                analyzers.forEach(analyzer -> analyzer.removeQuiz(id));
                quizOwners.remove(id);
            }
            knowledge.setEntity(changed);
            knowledge.setChildren(children);
            version++;
            dirty = true;
        } catch (RuntimeException e) {
            failure = e; // Never serve a partial delta. A new run reconstructs from durable proposals.
            throw e;
        }
    }

    @Override
    public synchronized <T> T query(BiFunction<AuditReport, Long, T> query) {
        ensureHealthy();
        if (dirty) {
            try {
                // No stale scores/diagnoses when a formerly nonempty topic becomes empty.
                reset(report.getRoot());
                IAuditEngine.visit(report.getRoot(), DefaultWorkingVocabulary::reset);
                analyzers.forEach(analyzer -> analyzer.onCourseComplete(report.getRoot()));
                dirty = false;
            } catch (RuntimeException e) {
                failure = e;
                throw e;
            }
        }
        return query.apply(report, version);
    }

    private static void reset(AuditNode node) {
        node.getScores().clear();
        node.setDiagnoses(IAuditEngine.freshDiagnoses(node.getTarget()));
    }

    private void ensureHealthy() {
        if (failure != null) throw new IllegalStateException("El contexto léxico falló; reconstruí desde la tanda guardada", failure);
    }
}
