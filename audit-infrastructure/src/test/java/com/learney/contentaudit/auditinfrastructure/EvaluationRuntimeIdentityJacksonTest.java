package com.learney.contentaudit.auditinfrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import org.junit.jupiter.api.Test;

class EvaluationRuntimeIdentityJacksonTest {

    @Test
    void preservesTheExactRuntimeThroughJacksonReportRoundTrip() throws Exception {
        ObjectMapper json = new ObjectMapper();
        EvaluationRuntimeIdentity runtime =
                EvaluationRuntimeIdentity.explicit("codex-cli", "gpt-6.1-sol", "medium");

        String stored = json.writeValueAsString(runtime);
        EvaluationRuntimeIdentity loaded = json.readValue(stored, EvaluationRuntimeIdentity.class);

        assertEquals(runtime, loaded);
    }
}
