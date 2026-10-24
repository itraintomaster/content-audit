package com.learney.contentaudit.revisiondomain.batch;

public interface QuizCorrectionService {
    QuizCorrection.State execute(QuizCorrection.Request request);
}
