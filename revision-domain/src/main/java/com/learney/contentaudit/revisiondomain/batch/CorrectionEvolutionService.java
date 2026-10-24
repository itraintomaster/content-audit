package com.learney.contentaudit.revisiondomain.batch;
public interface CorrectionEvolutionService {
    CorrectionEvolution.View execute(CorrectionEvolution.Request request);
}
