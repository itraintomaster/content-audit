package com.learney.contentaudit.revisiondomain.panelengine;

import java.util.List;
import java.util.Map;

/**
 * F-MESA-R006: what a candidate did to each task of its exercise, against its starting point. A task is resolved when
 * none of its findings is marked any more; improved when it is not, but one of them is gone or lighter and none is
 * heavier, or its length is closer to the range; open otherwise. {@code improvements} says, by improved task, its measure
 * before and after; {@code open}, by task still open, how it measures now.
 */
record TaskProgress(List<String> resolved, List<String> improved, Map<String, String> improvements,
        Map<String, String> open) {

    static final TaskProgress NONE = new TaskProgress(List.of(), List.of(), Map.of(), Map.of());

    boolean better() {
        return !resolved.isEmpty() || !improved.isEmpty();
    }
}
