package com.learney.contentaudit.auditdomain.judgingengine;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.judging.*;
import com.learney.contentaudit.evaluationledgerdomain.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JudgePromotionRuntimeTest {
    final EvaluationRuntimeIdentity identity=EvaluationRuntimeIdentity.explicit("codex-cli","synthetic","medium");
    final Instant at=Instant.parse("2026-10-14T06:00:00Z");
    final JudgePromotionRecord decision=new JudgePromotionRecord("naturalness","v1",identity,"José","Acepto el piloto","approval.json","a".repeat(64),at);
    final JudgeExamRegistry exams=mock(JudgeExamRegistry.class);
    final JudgePromotionRegistry promotions=mock(JudgePromotionRegistry.class);
    final EvaluationSessionFactory courses=mock(EvaluationSessionFactory.class);
    final JudgeRuntimeConfig config=mock(JudgeRuntimeConfig.class);
    DefaultJudgeRuntime runtime() {
        when(exams.latestCompatible(anyString(),any(),any())).thenReturn(Optional.empty());
        when(promotions.latest(anyString(),anyString(),any())).thenAnswer(c ->
                "naturalness".equals(c.getArgument(0)) && "v1".equals(c.getArgument(1)) && identity.equals(c.getArgument(2)) ? Optional.of(decision) : Optional.empty());
        when(courses.open(any(),any())).thenReturn(mock(EvaluationSession.class));when(config.getMaxConcurrentEvaluations()).thenReturn(1);
        return new DefaultJudgeRuntime(courses,mock(EvaluationSessionFactory.class),exams,mock(EvaluationTimeline.class),config,promotions);
    }
    @Test void operatorPromotionAllowsOnlyRequestedBudgetAndKeepsExamHistoryUntouched() {
        var runtime=runtime();var evaluator=mock(Evaluator.class);when(evaluator.evaluatorId()).thenReturn("native");
        when(evaluator.evaluatorVersion()).thenReturn(Optional.of("v1"));when(evaluator.runtimeIdentity()).thenReturn(identity);
        var asked=runtime.open("naturalness",List.of(evaluator),new EvaluationRunPolicy(3,false,null,null,EvaluationRunPurpose.COURSE,1));
        assertEquals(JudgeExamStatus.PROMOTED,asked.getStanding().getStatus());assertTrue(asked.getStanding().isConsultsOnCourse());
        assertEquals(3,asked.getEffectivePolicy().getMaxNewEvaluations());
        assertEquals(0,runtime.open("naturalness",List.of(evaluator),null).getEffectivePolicy().getMaxNewEvaluations());
        assertEquals(0,runtime.open("naturalness",List.of(evaluator),new EvaluationRunPolicy(3,false,null,null,EvaluationRunPurpose.READING,1)).getEffectivePolicy().getMaxNewEvaluations());
        verify(exams,never()).record(any());verify(evaluator,never()).evaluate(any());
    }
    @Test void changedVersionModelOrLegacyIdentityCannotInheritPromotionEvenThroughAliases() {
        var runtime=runtime();
        assertFalse(runtime.standing("naturalness","v2",identity,Set.of("v1","v2")).isConsultsOnCourse());
        assertFalse(runtime.standing("naturalness","v1",EvaluationRuntimeIdentity.explicit("codex-cli","other","medium")).isConsultsOnCourse());
        assertFalse(runtime.standing("naturalness","v1",EvaluationRuntimeIdentity.legacyUnknown()).isConsultsOnCourse());
        assertFalse(runtime.standing("a1-didactic","v1",identity).isConsultsOnCourse());
    }
    @Test void laterMeasuredFailureSupersedesPromotionButRehearsalDoesNot() {
        var runtime=runtime();var measured=new JudgeExamRecord("naturalness","v1",JudgeExamStatus.FAILED,1,0,List.of(),List.of(),false,"photo","reviewer",at.plusSeconds(1));
        when(exams.latestCompatible(anyString(),any(),any())).thenReturn(Optional.of(measured));
        assertEquals(JudgeExamStatus.FAILED,runtime.standing("naturalness","v1",identity).getStatus());
        measured.setStatus(JudgeExamStatus.TAKEN);
        assertEquals(JudgeExamStatus.PROMOTED,runtime.standing("naturalness","v1",identity).getStatus());
    }
}
