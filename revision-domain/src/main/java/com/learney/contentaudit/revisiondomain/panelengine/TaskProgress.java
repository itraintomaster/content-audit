package com.learney.contentaudit.revisiondomain.panelengine;

import java.util.List;
import java.util.Map;

/**
 * F-MESA-R006: what a candidate did to each task of its exercise, against its starting point. A task is resolved when
 * none of its findings is marked any more; improved when it is not, but one of them is gone or lighter and none is
 * heavier, or its length is closer to the range; open otherwise. {@code improvements} says, by improved task, its measure
 * before and after; {@code open}, by task still open, how it measures now. {@code followed}, the resolved tasks of the
 * production copy that only follow their exercise (F-MESA-R026), which do not make a candidate better.
 */
record TaskProgress(List<String> resolved, List<String> improved, Map<String, String> improvements,
        Map<String, String> open, List<String> followed) {

    static final TaskProgress NONE = new TaskProgress(List.of(), List.of(), Map.of(), Map.of(), List.of());

    boolean better() {
        return !improved.isEmpty() || resolvesOwn();
    }

    /** Whether the candidate resolved a task by itself, not only by its production copy following it. */
    boolean resolvesOwn() {
        return resolved.stream().anyMatch(task -> !followed.contains(task));
    }
}
