package com.learney.contentaudit.quizinstructioninfrastructure.choicequality;

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
class ChoiceQualityPromptDeliveryTest {
    @Test void sendsAllVisibleInputsToTheModelNotJustTheirNames(@TempDir Path runs) {
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.isDirectory(root.resolve(".sentinel/agents/choice-quality-judge"))) {
            root = root.getParent();
        }
        assertNotNull(root, "judge definition must be available to the test");
        List<ChatRequest> requests = new ArrayList<>();
        ChatModel offlineModel = new ChatModel() {
            @Override public ChatResponse doChat(ChatRequest request) {
                requests.add(request);
                return ChatResponse.builder().aiMessage(AiMessage.from("""
                        {"status":"PASS","objective":"Inversión con be","introductory":false,
                         "issues":[],"rationale":"Practica el objetivo"}
                        """)).build();
            }
        };
        Map<String, String> inputs = Map.of(
                "level", "A1",
                "topic", "Preguntas con be · tema visible",
                "title", "Inversión en presente simple · título visible",
                "instructions", "Forma una pregunta invirtiendo sujeto y be en presente simple.",
                "exercise", "___ thirsty? (she)",
                "options", "- She is\n- Are she\n- Is she (correcta)\n- Am she");
        var runner = new DefaultAgentGraphRunnerFactory().create(
                new AgentGraphRunnerConfig(root.resolve(".sentinel/agents"), runs));
        assertDoesNotThrow(() -> runner.run("choice-quality-judge", inputs, offlineModel, Map.of()));
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
