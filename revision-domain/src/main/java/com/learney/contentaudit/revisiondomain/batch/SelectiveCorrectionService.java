package com.learney.contentaudit.revisiondomain.batch;

public interface SelectiveCorrectionService {
    SelectiveCorrection.State execute(SelectiveCorrection.Request request);
}

