package com.learney.contentaudit.auditinfrastructure;

import com.learney.contentaudit.auditdomain.judging.JudgePromotionRecord;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import java.nio.file.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class FileSystemJudgePromotionRegistryTest {
    @TempDir Path directory;
    final EvaluationRuntimeIdentity identity=EvaluationRuntimeIdentity.explicit("codex-cli","synthetic","medium");
    JudgePromotionRecord promotion(String version) {return new JudgePromotionRecord("naturalness",version,identity,"José","Acepto el piloto","approval.json","a".repeat(64),Instant.parse("2026-10-14T06:00:00Z"));}
    @Test void persistsSeparatelyAndDoesNotGrantToDifferentVersionOrRuntime() throws Exception {
        var store=new FileSystemJudgePromotionRegistry(directory);store.record(promotion("v1"));
        var reloaded=new FileSystemJudgePromotionRegistry(directory);
        assertEquals(promotion("v1"),reloaded.latest("naturalness","v1",identity).orElseThrow());
        assertTrue(reloaded.latest("naturalness","v2",identity).isEmpty());
        assertTrue(reloaded.latest("naturalness","v1",EvaluationRuntimeIdentity.explicit("codex-cli","other","medium")).isEmpty());
        assertFalse(Files.exists(directory.resolve(".content-audit/judge-exams")));
    }
    @Test void preservesOldLinesAndIgnoresAnIncompleteRecord() throws Exception {
        var store=new FileSystemJudgePromotionRegistry(directory);store.record(promotion("v1"));
        var file=directory.resolve(".content-audit/judge-promotions/naturalness.jsonl");String before=Files.readString(file);
        store.record(promotion("v2"));assertTrue(Files.readString(file).startsWith(before));
        Files.writeString(file,"{\"judgeName\":\"naturalness\"}\n",StandardOpenOption.APPEND);
        assertEquals(promotion("v1"),store.latest("naturalness","v1",identity).orElseThrow());
        assertTrue(store.latest("naturalness","v1",EvaluationRuntimeIdentity.legacyUnknown()).isEmpty());
    }
}
