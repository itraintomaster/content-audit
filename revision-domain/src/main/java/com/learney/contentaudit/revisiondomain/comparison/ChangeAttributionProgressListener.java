package com.learney.contentaudit.revisiondomain.comparison;

/**
 * Observes the expensive leave-one-out attribution of broken aggregate floors.
 *
 * <p>The comparison remains independent of any UI or persistence mechanism: the
 * composition root may publish this progress to a CLI, a file or nowhere. A
 * listener must not perform business work and its failures must never change a
 * comparison result.</p>
 */
@FunctionalInterface
public interface ChangeAttributionProgressListener {
    void changed(int completed, int total);
}
