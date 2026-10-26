package com.learney.contentaudit.revisiondomain.panelengine;

import java.util.List;
import java.util.Map;

/**
 * F-MESA-R006: what a candidate did to each task of its exercise, against its starting point. A task is resolved when
 * none of its findings is marked any more; improved when it is not, but one of them is gone or lighter and none is
 * heavier, or its length is closer to the range; open otherwise. {@code improvements} says, by improved task, its measure
 * before and after; {@code open}, by task still open, how it measures now. {@code followed}, the resolved tasks of the
 * production copy that only follow their exercise (F-MESA-R026), which do not make a candidate better; {@code length},
 * the tasks of the length, which do not make it better either (F-MESA-R029).
 */
record TaskProgress(List<String> resolved, List<String> improved, Map<String, String> improvements,
        Map<String, String> open, List<String> followed, List<String> length) {

    static final TaskProgress NONE = new TaskProgress(List.of(), List.of(), Map.of(), Map.of(), List.of(), List.of());

    boolean better() {
        return improved.stream().anyMatch(task -> !length.contains(task)) || resolvesOwn();
    }

    /** Whether the candidate resolved by itself a task that is not of the length, not only by its production copy. */
    boolean resolvesOwn() {
        return resolved.stream().anyMatch(task -> !followed.contains(task) && !length.contains(task));
    }

    /** Whether all it did was shorten: some task of the length resolved or improved, and nothing else. */
    boolean onlyShortens() {
        return !better() && (resolved.stream().anyMatch(length::contains) || improved.stream().anyMatch(length::contains));
    }
}
