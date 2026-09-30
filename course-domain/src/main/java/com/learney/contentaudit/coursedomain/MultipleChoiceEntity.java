package com.learney.contentaudit.coursedomain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The multiple-choice payload of a form: the selection mode ({@code form.selection}, only
 * {@code "SINGLE"} observed; kept as the raw string because nothing branches on it) and the
 * options the student chooses from ({@code form.items}).
 *
 * <p>In a multiple-choice form the gap is a CLOZE sentence part with no options: the answer lives
 * here, in the item with incidence greater than zero.
 */
public class MultipleChoiceEntity {
    private String selection;

    private List<MultipleChoiceItemEntity> items;

    public MultipleChoiceEntity() {
    }

    public MultipleChoiceEntity(String selection, List<MultipleChoiceItemEntity> items) {
        this.selection = selection;
        this.items = items;
    }

    public String getSelection() {
        return this.selection;
    }

    public void setSelection(String selection) {
        this.selection = selection;
    }

    public List<MultipleChoiceItemEntity> getItems() {
        return this.items;
    }

    public void setItems(List<MultipleChoiceItemEntity> items) {
        this.items = items;
    }

    /**
     * The option marked as correct (incidence greater than zero). The course always has exactly
     * one; if a form ever carried more, the first one wins.
     */
    public Optional<MultipleChoiceItemEntity> correctItem() {
        if (items == null) {
            return Optional.empty();
        }
        return items.stream().filter(item -> item.getIncidence() > 0.0).findFirst();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MultipleChoiceEntity that = (MultipleChoiceEntity) o;
        return Objects.equals(this.selection, that.selection)
                    && Objects.equals(this.items, that.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(selection, items);
    }
}
