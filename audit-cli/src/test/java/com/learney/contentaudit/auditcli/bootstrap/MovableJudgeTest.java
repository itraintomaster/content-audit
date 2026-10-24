package com.learney.contentaudit.auditcli.bootstrap;

import static org.junit.jupiter.api.Assertions.*;

import com.learney.contentaudit.agentruntimeinfrastructure.llmprovider.DefaultLlmProviderResolver;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationOutcome;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSubject;
import com.learney.contentaudit.evaluationledgerdomain.Evaluator;
import com.learney.contentaudit.quizinstructioninfrastructure.AgentJudgeConfig;
import com.learney.contentaudit.quizinstructioninfrastructure.QuizInstructionJudgeConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * F-CMOTOR-R001: a judge is moved only to a runtime of a subscription -- the local session, no key --, with that
 * runtime's provider, model and effort and without the address or the key of its own. Moving it consults nothing.
 */
@Tag("FEAT-CMOTOR")
@Tag("F-CMOTOR-R001")
class MovableJudgeTest {
    static final EvaluationRuntimeIdentity CHATGPT = EvaluationRuntimeIdentity.explicit("codex-cli", "gpt-6.1-sol", "medium");
    static final EvaluationRuntimeIdentity GEMINI = EvaluationRuntimeIdentity.explicit("agy", "gemini-3.8-flash-high", "high");

    final List<AgentJudgeConfig> built = new ArrayList<>();

    @Test
    @DisplayName("should build the judge again on a runtime of a subscription, with its provider, model and effort and without the key of its own")
    void movesToARuntimeOfASubscriptionWithoutTheKeyOfItsOwn() {
        MovableJudge<AgentJudgeConfig> own = judge(new AgentJudgeConfig("codex-cli", "https://own", "own-key",
                "gpt-6.1-sol", 0.2, 240, "medium"));

        Evaluator gemini = own.withRuntime(GEMINI).orElseThrow();

        assertEquals("mini-theory-judge", gemini.evaluatorId());
        assertEquals(GEMINI, gemini.runtimeIdentity());
        AgentJudgeConfig moved = built.get(built.size() - 1);
        assertEquals("agy", moved.getProviderName());
        assertEquals("gemini-3.8-flash-high", moved.getModelName());
        assertEquals("high", moved.getReasoningEffort());
        assertNull(moved.getApiKey(), "the key of the profile's provider never goes to another one");
        assertNull(moved.getBaseUrl());
        assertEquals(Double.valueOf(0.2), moved.getTemperature());
        assertEquals(Integer.valueOf(240), moved.getTimeoutSeconds());
    }

    @Test
    @DisplayName("should never move to a runtime that needs a key, nor to one without provenance")
    void neverMovesToARuntimeThatNeedsAKey() {
        MovableJudge<AgentJudgeConfig> own = judge(new AgentJudgeConfig("codex-cli", null, null, "gpt-6.1-sol", null,
                null, "medium"));

        assertEquals(Optional.empty(), own.withRuntime(EvaluationRuntimeIdentity.explicit("openai", "gpt-5", "high")));
        assertEquals(Optional.empty(), own.withRuntime(EvaluationRuntimeIdentity.legacyUnknown()));
        assertEquals(Optional.empty(), own.withRuntime(null));
    }

    @Test
    @DisplayName("should stay itself on its own runtime, and read its id without building anything")
    void staysItselfOnItsOwnRuntime() {
        MovableJudge<AgentJudgeConfig> own = judge(new AgentJudgeConfig("codex-cli", null, null, "gpt-6.1-sol", null,
                null, "medium"));
        assertEquals("mini-theory-judge", own.evaluatorId());
        assertTrue(built.isEmpty(), "reading the id builds nothing");

        assertSame(own, own.withRuntime(CHATGPT).orElseThrow());
    }

    @Test
    @DisplayName("should move a judge without effort with none, and the consigna judge keeping its agent")
    void movesARuntimeWithoutEffortAndTheConsignaJudge() {
        MovableJudge<AgentJudgeConfig> own = judge(new AgentJudgeConfig("codex-cli", null, null, "gpt-6.1-sol", null,
                null, "medium"));
        own.withRuntime(EvaluationRuntimeIdentity.explicit("claude-cli", "opus",
                EvaluationRuntimeIdentity.NOT_APPLICABLE)).orElseThrow().evaluatorVersion();
        assertNull(built.get(built.size() - 1).getReasoningEffort());

        List<QuizInstructionJudgeConfig> consignas = new ArrayList<>();
        MovableJudge<QuizInstructionJudgeConfig> consigna = MovableJudge.lazy("quiz-instruction-validator",
                new QuizInstructionJudgeConfig(null, null, "gpt-6.1-sol", null, null, "quiz-instruction-validator",
                        "codex-cli", "medium"),
                config -> {
                    consignas.add(config);
                    return fake("quiz-instruction-validator", config.getProviderName(), config.getModelName(),
                            config.getReasoningEffort());
                },
                QuizInstructionJudgeConfig::onRuntime, new DefaultLlmProviderResolver());
        consigna.withRuntime(GEMINI).orElseThrow().evaluatorVersion();
        assertEquals("quiz-instruction-validator", consignas.get(consignas.size() - 1).getAgentName());
        assertEquals("agy", consignas.get(consignas.size() - 1).getProviderName());
    }

    private MovableJudge<AgentJudgeConfig> judge(AgentJudgeConfig config) {
        return MovableJudge.lazy("mini-theory-judge", config, each -> {
            built.add(each);
            return fake("mini-theory-judge", each.getProviderName(), each.getModelName(), each.getReasoningEffort());
        }, AgentJudgeConfig::onRuntime, new DefaultLlmProviderResolver());
    }

    private static Evaluator fake(String id, String provider, String model, String effort) {
        return new Evaluator() {
            @Override
            public String evaluatorId() {
                return id;
            }

            @Override
            public EvaluationOutcome evaluate(EvaluationSubject subject) {
                throw new AssertionError("moving a judge consults nothing");
            }

            @Override
            public Optional<String> evaluatorVersion() {
                return Optional.of("v1");
            }

            @Override
            public EvaluationRuntimeIdentity runtimeIdentity() {
                return EvaluationRuntimeIdentity.explicit(provider, model,
                        effort != null ? effort : EvaluationRuntimeIdentity.NOT_APPLICABLE);
            }
        };
    }
}
