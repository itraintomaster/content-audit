package com.learney.contentaudit.coursedomain;

import java.util.Objects;

/**
 * One option of a multiple-choice form ({@code form.items[]}): its id, the label the student
 * sees, and its incidence — the correct option is the one with incidence greater than zero.
 */
public class MultipleChoiceItemEntity {
    private String id;

    private double incidence;

    private String label;

    public MultipleChoiceItemEntity() {
    }

    public MultipleChoiceItemEntity(String id, double incidence, String label) {
        this.id = id;
        this.incidence = incidence;
        this.label = label;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getIncidence() {
        return this.incidence;
    }

    public void setIncidence(double incidence) {
        this.incidence = incidence;
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MultipleChoiceItemEntity that = (MultipleChoiceItemEntity) o;
        return Objects.equals(this.id, that.id)
                    && Objects.equals(this.incidence, that.incidence)
                    && Objects.equals(this.label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, incidence, label);
    }
}
