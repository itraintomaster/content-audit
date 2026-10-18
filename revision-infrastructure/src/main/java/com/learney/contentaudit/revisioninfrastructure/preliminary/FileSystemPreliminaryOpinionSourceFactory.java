package com.learney.contentaudit.revisioninfrastructure.preliminary;

import com.learney.contentaudit.coursedomain.CourseRepository;
import com.learney.contentaudit.revisiondomain.batch.PreliminaryOpinionSource;

/** Reads existing experimental evaluations; never invokes or approves a judge. */
public final class FileSystemPreliminaryOpinionSourceFactory {
    private FileSystemPreliminaryOpinionSourceFactory() {}

    public static PreliminaryOpinionSource create(CourseRepository courses) {
        return new FileSystemPreliminaryOpinionSource(courses);
    }
}
