package com.learney.contentaudit.revisiondomain.batch;

import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.revisiondomain.nonregression.AudioRerecordRequest;
import java.util.List;

/** Inspects the recorded text for the final correction. Does not generate, listen to, or publish audio. */
public interface AudioPrepublicationService {
    View execute(Request request);
    record Request(String action, String batchId) {}
    record Entry(String quizId, String audioKey, List<String> finalTexts, String status,
            List<String> reasons, List<Finding> findings, AudioRerecordRequest rerecord) {}
    record View(String batchId, int contentVersion, String contentFingerprint, String stage, int spent,
            int total, int pending, int recordTextMatches, int recordTextProblems,
            List<Entry> entries, List<String> limitations) {}
}
