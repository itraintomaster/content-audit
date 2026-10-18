package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.auditdomain.JudgePromotionRegistry;
import com.learney.contentaudit.auditdomain.judging.*;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PromoteJudgeCmdTest {
    @TempDir Path directory;
    @Test void recordsHumanDecisionWithEvidenceAndEffectiveIdentity() throws Exception {
        var store=mock(JudgePromotionRegistry.class);when(store.latest(any(),any(),any())).thenReturn(Optional.empty());
        var identity=EvaluationRuntimeIdentity.explicit("codex-cli","synthetic","medium");
        var standing=new JudgeExamStanding("naturalness","v1",JudgeExamStatus.NOT_TAKEN,false,"No exam",identity);
        var file=directory.resolve("approval.json");Files.writeString(file,"{\"decision\":\"promote\"}");
        var cli=new CommandLine(new PromoteJudgeCmd(store,()->Map.of("naturalness",standing)));
        assertEquals(0,cli.execute("naturalness","--expected-version","v1","--reviewer","José","--evidence",file.toString(),"--reason","Acepto el piloto"));
        verify(store).record(argThat(r->r.judgeVersion().equals("v1") && r.runtimeIdentity().equals(identity) && r.evidenceSha256().length()==64 && r.reviewer().equals("José")));
    }
    @Test void staleVersionOrMissingEvidenceNeverPromotes() throws Exception {
        var store=mock(JudgePromotionRegistry.class);var identity=EvaluationRuntimeIdentity.explicit("codex-cli","synthetic","medium");
        var standing=new JudgeExamStanding("naturalness","v2",JudgeExamStatus.NOT_TAKEN,false,"No exam",identity);
        var cli=new CommandLine(new PromoteJudgeCmd(store,()->Map.of("naturalness",standing)));
        var missing=directory.resolve("missing.json");
        assertEquals(2,cli.execute("naturalness","--expected-version","v1","--reviewer","José","--evidence",missing.toString(),"--reason","Acepto el piloto"));
        assertEquals(2,cli.execute("naturalness","--expected-version","v2","--reviewer","José","--evidence",missing.toString(),"--reason","Acepto el piloto"));
        verify(store,never()).record(any());
    }
}
