package com.learney.contentaudit.revisiondomain.minitheorycorrection;

import com.learney.contentaudit.coursedomain.CourseEntity;
import java.util.function.Supplier;

public interface MiniTheoryWorkspaceStore {
    MiniTheoryCorrection.Workspace load(String batchId);
    void save(String batchId, MiniTheoryCorrection.Workspace workspace);
    String fingerprint(CourseEntity course);
    String fingerprintIdentity(Object identity);
    /** Cross-process exclusive writer; a second writer fails rather than overwriting a running operation. */
    <T> T exclusive(String batchId, Supplier<T> operation);
}
