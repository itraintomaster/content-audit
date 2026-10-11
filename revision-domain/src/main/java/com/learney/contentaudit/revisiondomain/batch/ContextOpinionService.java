package com.learney.contentaudit.revisiondomain.batch;

public interface ContextOpinionService {
    ContextOpinion.State execute(ContextOpinion.Request request);
}
