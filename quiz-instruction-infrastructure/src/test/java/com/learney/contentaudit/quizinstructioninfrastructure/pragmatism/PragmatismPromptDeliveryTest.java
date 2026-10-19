package com.learney.contentaudit.quizinstructioninfrastructure.pragmatism;

import com.learney.contentaudit.agentruntimeinfrastructure.AgentGraphRunnerConfig;
import com.learney.contentaudit.agentruntimeinfrastructure.graphexecution.DefaultAgentGraphRunnerFactory;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Runs the real graph and prompt renderer against an in-memory model, never Codex. */
class PragmatismPromptDeliveryTest {
    @Test void sendsAllVisibleInputsToTheModelNotJustTheirNames(@TempDir Path runs) {
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.isDirectory(root.resolve(".sentinel/agents/pragmatism-judge"))) {
            root = root.getParent();
        }
        assertNotNull(root, "judge definition must be available to the test");
        List<ChatRequest> requests = new ArrayList<>();
        ChatModel offlineModel = new ChatModel() {
            @Override public ChatResponse doChat(ChatRequest request) {
                requests.add(request);
                return ChatResponse.builder().aiMessage(AiMessage.from("""
                        {"assessments":[{"sentence":"I need a clean towel.","status":"PASS","communicativePurpose":"Pedir una toalla",
                         "plausibleUse":"Pedirla después de bañarse","issue":null,"severity":null,"reason":"Petición cotidiana","neededContext":null}],
                         "rationale":"Uso comunicativo plausible"}
                        """)).build();
            }
        };
        Map<String, String> inputs = Map.of(
                "level", "A1", "topic", "Peticiones domésticas",
                "title", "Pedir un objeto", "instructions", "Completa una petición cotidiana.",
                "exercise", "I need a clean ___.", "acceptedSentences", "[\"I need a clean towel.\"]");
        var runner = new DefaultAgentGraphRunnerFactory().create(
                new AgentGraphRunnerConfig(root.resolve(".sentinel/agents"), runs));
        var state = assertDoesNotThrow(() -> runner.run("pragmatism-judge", inputs, offlineModel, Map.of()));
        assertNotNull(state.artifacts().get("pragmatismVerdict"), "the graph must emit the validated artifact");
        String payload = assertDoesNotThrow(() -> Files.readString(state.artifacts().get("pragmatismVerdict").location()));
        assertEquals(com.learney.contentaudit.auditdomain.pragmatism.PragmatismVerdict.Status.PASS,
                new JacksonPragmatismVerdictReader().read(payload).assessments().get(0).status());
        assertEquals(1, requests.size(), "well-formed output must not retry the fake model");
        String delivered = requests.get(0).messages().stream()
                .filter(UserMessage.class::isInstance).map(UserMessage.class::cast)
                .map(UserMessage::singleText).collect(Collectors.joining("\n"));
        inputs.forEach((key, value) -> {
            assertTrue(delivered.contains(value), "missing rendered input: " + key);
            assertFalse(delivered.contains("{" + key + "}"), "unresolved placeholder: " + key);
        });
    }
}
