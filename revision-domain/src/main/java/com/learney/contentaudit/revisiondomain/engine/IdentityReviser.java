package com.learney.contentaudit.revisiondomain.engine;
import javax.annotation.processing.Generated;

import com.learney.contentaudit.refinerdomain.CorrectionContext;
import com.learney.contentaudit.refinerdomain.DiagnosisKind;
import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.Reviser;
import com.learney.contentaudit.revisiondomain.RevisionProposal;
import java.time.Instant;

class IdentityReviser implements Reviser {

    private static final String RATIONALE = "bypass: identity revision";
    private static final String REVISER_KIND = "bypass";

    @Override
    public RevisionProposal propose(RefinementTask task, CorrectionContext context,
            CourseElementSnapshot before) {
        Instant now = Instant.now();
        String proposalId = task.getId() + "-" + now.toEpochMilli();
        String planId = "bypass";
        String sourceAuditId = "bypass";
        return new RevisionProposal(
                proposalId,
                task.getId(),
                planId,
                sourceAuditId,
                task.getDiagnosisKind(),
                task.getNodeTarget(),
                task.getNodeId(),
                before,
                before,
                RATIONALE,
                REVISER_KIND,
                now,
                null);
    }

    @Override
    public boolean handles(DiagnosisKind kind) {
        return true;
    }

    @Override
    public String reviserKind() {
        return REVISER_KIND;
    }
}
