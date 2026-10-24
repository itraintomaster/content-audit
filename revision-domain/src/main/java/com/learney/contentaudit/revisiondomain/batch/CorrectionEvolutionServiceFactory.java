package com.learney.contentaudit.revisiondomain.batch;
import com.learney.contentaudit.coursedomain.CourseEntity;
import java.util.function.Function;
public interface CorrectionEvolutionServiceFactory {
    CorrectionEvolutionService create(BatchServiceConfig config);
    CorrectionEvolutionService create(BatchServiceConfig config, Function<CourseEntity, String> fingerprint);
}
