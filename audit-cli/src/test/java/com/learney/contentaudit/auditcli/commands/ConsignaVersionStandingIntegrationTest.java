package com.learney.contentaudit.auditcli.commands;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.agentruntimeinfrastructure.AgentDefinitionFound;
import com.learney.contentaudit.agentruntimeinfrastructure.AgentDefinitionLocation;
import com.learney.contentaudit.agentruntimeinfrastructure.AgentDefinitionLocator;
import com.learney.contentaudit.agentruntimeinfrastructure.AgentGraphRunner;
import com.learney.contentaudit.agentruntimeinfrastructure.llmprovider.DefaultLlmProviderResolver;
import com.learney.contentaudit.auditapplication.DefaultJudgeRuntimeConfig;
import com.learney.contentaudit.auditdomain.judging.JudgeExamRecord;
import com.learney.contentaudit.auditdomain.judging.JudgeExamStanding;
import com.learney.contentaudit.auditdomain.judging.JudgeExamStatus;
import com.learney.contentaudit.auditdomain.judgingengine.DefaultJudgeRuntime;
import com.learney.contentaudit.auditinfrastructure.FileSystemJudgeExamRegistry;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import com.learney.contentaudit.evaluationledgerdomain.Evaluator;
import com.learney.contentaudit.quizinstructioninfrastructure.JudgeProviderReadinessProbe;
import com.learney.contentaudit.quizinstructioninfrastructure.QuizInstructionJudgeConfig;
import com.learney.contentaudit.quizinstructioninfrastructure.instructionjudge.DefaultQuizInstructionJudgeFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Real version resolver, equivalence reader, exam registry and standing; never invokes a model. */
@Tag("F-ALDIA-R001")
@Tag("F-ALDIA-R002")
@Tag("F-JEXAM-R003")
class ConsignaVersionStandingIntegrationTest {
    private static final String AGENT = "quiz-instruction-validator";
    private static final String JUDGE = "quiz-instruction";
    private static final Instant CUTOVER = Instant.parse("2026-10-23T00:00:00Z");
    private static final EvaluationRuntimeIdentity APPROVED_RUNTIME =
            EvaluationRuntimeIdentity.explicit("codex-cli", "synthetic-model", "medium");

    @Test
    void approvalSurvivesEditingAddingAndDeletingEvalFilesWithoutRewritingHistory(@TempDir Path work)
            throws Exception {
        Fixture fixture = new Fixture(work);
        Evaluator judge = fixture.judge(APPROVED_RUNTIME);
        fixture.assertApproved(judge);

        Files.writeString(fixture.definition.resolve("eval/fixtures.json"), "[{\"case\":2}]");
        Files.writeString(fixture.definition.resolve("eval/run_evals.py"), "print('different evaluation reader')\n");
        fixture.assertApproved(judge);

        Files.createDirectories(fixture.definition.resolve("eval/readers"));
        Files.writeString(fixture.definition.resolve("eval/readers/student_view.py"), "def read(): return 'options'\n");
        Files.writeString(fixture.definition.resolve("eval/test_student_view.py"), "def test_view(): assert True\n");
        fixture.assertApproved(fixture.judge(APPROVED_RUNTIME));

        Files.delete(fixture.definition.resolve("eval/fixtures.json"));
        Files.delete(fixture.definition.resolve("eval/run_evals.py"));
        fixture.assertApproved(judge);
        fixture.assertHistoryUntouchedAndNoConsultations();
    }

    @ParameterizedTest
    @ValueSource(strings = {"nodes/evaluate.md", "scripts/gate.py", "schema/verdict.schema.json", "agent.yaml"})
    void effectiveProcessChangesLoseTheOldApprovalAndCannotAddALaterEquivalence(String changedFile,
            @TempDir Path work) throws Exception {
        Fixture fixture = new Fixture(work);
        fixture.assertApproved(fixture.judge(APPROVED_RUNTIME));

        Path changed = fixture.definition.resolve(changedFile);
        Files.writeString(changed, Files.readString(changed) + "\nchanged effective process\n");
        Evaluator changedJudge = fixture.judge(APPROVED_RUNTIME);
        String changedVersion = changedJudge.evaluatorVersion().orElseThrow();
        assertNotEquals(fixture.currentVersion, changedVersion);
        fixture.assertUnapproved(changedJudge);
        assertFalse(changedJudge.compatibleEvaluatorVersions().contains(fixture.previousVersion));
        assertTrue(changedJudge.versionEquivalences().isEmpty());

        // The original migration is a single batch, not permission to approve future prompts.
        fixture.appendEquivalence(changedVersion, CUTOVER.plusSeconds(60));
        fixture.assertUnapproved(changedJudge);
        assertFalse(changedJudge.compatibleEvaluatorVersions().contains(fixture.previousVersion));
        assertTrue(changedJudge.versionEquivalences().isEmpty());
        fixture.assertHistoryUntouchedAndNoConsultations();
    }

    @ParameterizedTest
    @CsvSource({
            "claude-cli, synthetic-model, medium",
            "codex-cli, different-model, medium",
            "codex-cli, synthetic-model, high"
    })
    @Tag("F-JEXAM-R014")
    void anotherProviderModelOrReasoningDoesNotInheritAnApprovedRuntime(String provider, String model,
            String reasoning, @TempDir Path work) throws Exception {
        Fixture fixture = new Fixture(work);
        fixture.assertApproved(fixture.judge(APPROVED_RUNTIME));

        var otherRuntime = EvaluationRuntimeIdentity.explicit(provider, model, reasoning);
        Evaluator otherJudge = fixture.judge(otherRuntime);
        assertEquals(fixture.currentVersion, otherJudge.evaluatorVersion().orElseThrow(),
                "the process is unchanged; runtime identity is a separate gate");
        assertEquals(otherRuntime, otherJudge.runtimeIdentity());
        assertTrue(otherJudge.compatibleEvaluatorVersions().contains(fixture.previousVersion),
                "process compatibility must not by itself approve a different runtime");
        fixture.assertUnapproved(otherJudge);
        fixture.assertHistoryUntouchedAndNoConsultations();
    }

    private static final class Fixture {
        private final Path definition;
        private final Path equivalences;
        private final Path historyDirectory;
        private final AgentGraphRunner runner = mock(AgentGraphRunner.class);
        private final JudgeProviderReadinessProbe readiness = mock(JudgeProviderReadinessProbe.class);
        private final FileSystemJudgeExamRegistry registry;
        private final DefaultJudgeRuntime runtime;
        private final String previousVersion;
        private final String currentVersion;
        private final Map<String, String> historyBefore;

        private Fixture(Path work) throws Exception {
            Path agents = Files.createDirectories(work.resolve("agents"));
            definition = Files.createDirectories(agents.resolve(AGENT));
            write("agent.yaml", "name: quiz-instruction-validator\nkind: agent\nstrategy: graph\ninitial_node: evaluate\n");
            write("nodes/evaluate.md", "Check whether the exercise follows its instruction.\n");
            write("scripts/gate.py", "def accept(verdict): return verdict['valid']\n");
            write("schema/verdict.schema.json", "{\"type\":\"object\",\"required\":[\"valid\"]}\n");
            write("eval/fixtures.json", "[{\"case\":1}]\n");
            write("eval/run_evals.py", "print('evaluation reader')\n");
            previousVersion = historicalProcessVersion(definition);
            currentVersion = judge(APPROVED_RUNTIME).evaluatorVersion().orElseThrow();
            assertNotEquals(previousVersion, currentVersion,
                    "the migration removes eval/ from the prior process digest");
            equivalences = agents.resolve("quiz-instruction-version-equivalences.jsonl");
            historyDirectory = Files.createDirectories(work.resolve("exam-history"));
            registry = new FileSystemJudgeExamRegistry(historyDirectory);
            registry.record(new JudgeExamRecord(JUDGE, previousVersion, JudgeExamStatus.APPROVED,
                    1, 0, List.of(), List.of(), true, "synthetic-photo", "synthetic-review",
                    "codex-cli", "synthetic-model", "medium", CUTOVER.minusSeconds(3600)));
            historyBefore = fileContents(historyDirectory);
            assertFalse(historyBefore.isEmpty(), "the approval is actually persisted");
            runtime = new DefaultJudgeRuntime(null, null, registry, evaluatorId -> List.of(),
                    new DefaultJudgeRuntimeConfig());
            assertUnapproved(judge(APPROVED_RUNTIME));
            appendEquivalence(currentVersion, CUTOVER);
        }

        private void write(String relative, String text) throws IOException {
            Path path = definition.resolve(relative);
            Files.createDirectories(path.getParent());
            Files.writeString(path, text);
        }

        private Evaluator judge(EvaluationRuntimeIdentity identity) {
            AgentDefinitionLocator locator = new AgentDefinitionLocator() {
                @Override public AgentDefinitionLocation locate(String agentName) {
                    assertEquals(AGENT, agentName);
                    return new AgentDefinitionFound(definition);
                }
                @Override public AgentDefinitionLocation locateUnderProjectRoot(String projectRoot, String agentName) {
                    return locate(agentName);
                }
            };
            var config = new QuizInstructionJudgeConfig(null, null, identity.getModelName(), null, null,
                    AGENT, identity.getProviderName(), identity.getReasoningEffort());
            return new DefaultQuizInstructionJudgeFactory(new DefaultLlmProviderResolver(), readiness)
                    .create(config, runner, locator);
        }

        private void appendEquivalence(String current, Instant recordedAt) throws IOException {
            String line = new ObjectMapper().writeValueAsString(Map.of(
                    "agent", AGENT, "earlierVersion", previousVersion, "currentVersion", current,
                    "recordedAt", recordedAt.toString(), "reason", "Exclude eval only; the effective process is unchanged."));
            Files.writeString(equivalences, line + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }

        private JudgeExamStanding standing(Evaluator judge) {
            return runtime.standing(JUDGE, judge.evaluatorVersion().orElseThrow(), judge.runtimeIdentity(),
                    judge.compatibleEvaluatorVersions());
        }

        private void assertApproved(Evaluator judge) {
            assertEquals(currentVersion, judge.evaluatorVersion().orElseThrow());
            assertTrue(judge.compatibleEvaluatorVersions().containsAll(List.of(currentVersion, previousVersion)));
            assertEquals(1, judge.versionEquivalences().size());
            var equivalence = judge.versionEquivalences().get(0);
            assertEquals(AGENT, equivalence.getEvaluatorId());
            assertEquals(previousVersion, equivalence.getEarlierVersion());
            assertEquals(currentVersion, equivalence.getCurrentVersion());
            assertEquals(CUTOVER, equivalence.getRecordedAt());
            JudgeExamStanding standing = standing(judge);
            assertEquals(JudgeExamStatus.APPROVED, standing.getStatus());
            assertTrue(standing.isConsultsOnCourse());
            assertEquals(currentVersion, standing.getJudgeVersion());
            assertEquals(APPROVED_RUNTIME, standing.getRuntimeIdentity());
        }

        private void assertUnapproved(Evaluator judge) {
            JudgeExamStanding standing = standing(judge);
            assertEquals(JudgeExamStatus.NOT_TAKEN, standing.getStatus());
            assertFalse(standing.isConsultsOnCourse());
        }

        private void assertHistoryUntouchedAndNoConsultations() throws IOException {
            assertEquals(historyBefore, fileContents(historyDirectory));
            var history = registry.history(JUDGE);
            assertEquals(1, history.size());
            assertEquals(previousVersion, history.get(0).getJudgeVersion());
            assertEquals(JudgeExamStatus.APPROVED, history.get(0).getStatus());
            assertEquals(APPROVED_RUNTIME, history.get(0).getRuntimeIdentity());
            verifyNoInteractions(runner, readiness);
        }
    }

    /** Independent reproduction of the old process digest, which counted every file under the agent. */
    private static String historicalProcessVersion(Path definition) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        digest.update(("agent:" + AGENT + "\n").getBytes(StandardCharsets.UTF_8));
        try (var files = Files.walk(definition)) {
            for (Path file : files.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> definition.relativize(path).toString())).toList()) {
                digest.update(definition.relativize(file).toString().getBytes(StandardCharsets.UTF_8));
                digest.update(Files.readAllBytes(file));
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static Map<String, String> fileContents(Path directory) throws IOException {
        Map<String, String> contents = new LinkedHashMap<>();
        try (var files = Files.walk(directory)) {
            for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                contents.put(directory.relativize(file).toString(), Base64.getEncoder().encodeToString(Files.readAllBytes(file)));
            }
        }
        return contents;
    }
}
