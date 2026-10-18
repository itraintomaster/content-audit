package com.learney.contentaudit.revisiondomain.comparisonengine;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.judging.JudgeExamStatus;
import com.learney.contentaudit.auditdomain.localtools.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UnapprovedJudgesPromotionTest {
    AuditReport report(JudgeExamStatus state) {
        var diagnoses=new DefaultCourseDiagnoses();
        diagnoses.setLocalToolRuns(List.of(new LocalToolRunDiagnosis("grammar",new LocalToolStatus("tool","v1",true,null,null),
                "tool v1",state,true,null,false)));
        return new AuditReport(new AuditNode(null,AuditTarget.COURSE,null,new ArrayList<>(),new LinkedHashMap<>(),new LinkedHashMap<>(),diagnoses));
    }
    @Test void promotedStatusAllowsCorrectionGateWhileFailedStatusStillBlocks() {
        assertTrue(UnapprovedJudges.of(report(JudgeExamStatus.PROMOTED)).isEmpty());
        assertFalse(UnapprovedJudges.of(report(JudgeExamStatus.FAILED)).isEmpty());
    }
}
