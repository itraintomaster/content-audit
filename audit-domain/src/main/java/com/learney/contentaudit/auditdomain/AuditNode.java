package com.learney.contentaudit.auditdomain;

import com.learney.contentaudit.auditdomain.contextnumbers.ContextNumbers;
import com.learney.contentaudit.auditdomain.finding.Finding;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
public class AuditNode {
    private AuditableEntity entity;

    private AuditTarget target;

    private AuditNode parent;

    private List<AuditNode> children;

    private Map<String, Double> scores;

    private Map<String, Object> metadata;

    private NodeDiagnoses diagnoses;

    // F-HALL-R001: the findings the engine stamped on this node, in catalog order of their
    // analyzer, then by rule and marker (F-HALL-R014).
    private List<Finding> findings = new ArrayList<>();

    // F-HALL-R008/R009/R010: the numbers the engine published for this node, computed once.
    private ContextNumbers numbers;

    // F-HALL-R008: analyzers that reached this node and could not evaluate it (the judge's
    // pending or failed quiz, or a model-backed analyzer whose failure the engine isolated).
    private List<String> unevaluatedBy = new ArrayList<>();

    public AuditNode() {
    }

    public AuditNode(AuditableEntity entity, AuditTarget target, AuditNode parent,
            List<AuditNode> children, Map<String, Double> scores, Map<String, Object> metadata,
            NodeDiagnoses diagnoses) {
        this.entity = entity;
        this.target = target;
        this.parent = parent;
        this.children = children;
        this.scores = scores;
        this.metadata = metadata;
        this.diagnoses = diagnoses;
    }

    public AuditableEntity getEntity() {
        return this.entity;
    }

    public void setEntity(AuditableEntity entity) {
        this.entity = entity;
    }

    public AuditTarget getTarget() {
        return this.target;
    }

    public void setTarget(AuditTarget target) {
        this.target = target;
    }

    public AuditNode getParent() {
        return this.parent;
    }

    public void setParent(AuditNode parent) {
        this.parent = parent;
    }

    public List<AuditNode> getChildren() {
        return this.children;
    }

    public void setChildren(List<AuditNode> children) {
        this.children = children;
    }

    public Map<String, Double> getScores() {
        return this.scores;
    }

    public void setScores(Map<String, Double> scores) {
        this.scores = scores;
    }

    public Map<String, Object> getMetadata() {
        return this.metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public NodeDiagnoses getDiagnoses() {
        return this.diagnoses;
    }

    public void setDiagnoses(NodeDiagnoses diagnoses) {
        this.diagnoses = diagnoses;
    }

    public List<Finding> getFindings() {
        return this.findings;
    }

    public void setFindings(List<Finding> findings) {
        this.findings = findings;
    }

    public ContextNumbers getNumbers() {
        return this.numbers;
    }

    public void setNumbers(ContextNumbers numbers) {
        this.numbers = numbers;
    }

    public List<String> getUnevaluatedBy() {
        return this.unevaluatedBy;
    }

    public void setUnevaluatedBy(List<String> unevaluatedBy) {
        this.unevaluatedBy = unevaluatedBy;
    }

    public Optional<AuditNode> ancestor(AuditTarget level) {
        AuditNode current = this.parent;
        while (current != null) {
            if (current.getTarget() == level) {
                return Optional.of(current);
            }
            current = current.getParent();
        }
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditNode that = (AuditNode) o;
        return Objects.equals(this.entity, that.entity)
                    && Objects.equals(this.target, that.target)
                    && Objects.equals(this.parent, that.parent)
                    && Objects.equals(this.children, that.children)
                    && Objects.equals(this.scores, that.scores)
                    && Objects.equals(this.metadata, that.metadata)
                    && Objects.equals(this.diagnoses, that.diagnoses)
                    && Objects.equals(this.findings, that.findings)
                    && Objects.equals(this.numbers, that.numbers)
                    && Objects.equals(this.unevaluatedBy, that.unevaluatedBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity, target, parent, children, scores, metadata, diagnoses,
                findings, numbers, unevaluatedBy);
    }
}
