package com.learney.contentaudit.revisioninfrastructure.minitheory;
import static org.junit.jupiter.api.Assertions.*;
import com.learney.contentaudit.revisiondomain.minitheorycorrection.MiniTheoryCorrection.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class MiniTheoryRewriterAdapterTest {
    @Test void asksForLiteralReuseWithHumanFeedbackAndRetainsEffectiveProvenance() {
        var adapter = new MiniTheoryRewriterAdapter(prompt -> {
            assertTrue(prompt.contains("You va con are")); assertTrue(prompt.contains("LITERALMENTE"));
            return "{\"text\":\"You va con are\",\"sourceId\":\"existing\"}";
        }, "codex-cli:gpt-6.1-sol; effort=medium; prompt=mini-feedback-v1");
        var result = adapter.rewrite(new RewriteInput("You va con are", "quiz", null, null,
                List.of(new Candidate("existing", "other", "You va con are", "COURSE", "UNREVIEWED"))));
        assertEquals("existing", result.sourceId()); assertTrue(result.provenance().contains("gpt-6.1-sol"));
    }
    @Test void unusableResponseIsRejectedWithoutRetries() {
        var adapter = new MiniTheoryRewriterAdapter(prompt -> "no es JSON", "test");
        assertThrows(IllegalArgumentException.class, () -> adapter.rewrite(new RewriteInput("context", "quiz", null, null, List.of())));
    }
}
