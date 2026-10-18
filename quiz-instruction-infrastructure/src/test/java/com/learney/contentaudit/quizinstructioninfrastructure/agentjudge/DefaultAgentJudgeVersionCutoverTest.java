package com.learney.contentaudit.quizinstructioninfrastructure.agentjudge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learney.contentaudit.agentruntimeinfrastructure.AgentDefinitionLocator;
import com.learney.contentaudit.agentruntimeinfrastructure.AgentGraphRunnerConfig;
import com.learney.contentaudit.agentruntimeinfrastructure.graphexecution.DefaultAgentDefinitionLocatorFactory;
import com.learney.contentaudit.agentruntimeinfrastructure.llmprovider.LlmProviderClass;
import com.learney.contentaudit.agentruntimeinfrastructure.llmprovider.LlmProviderResolved;
import com.learney.contentaudit.evaluationledgerdomain.EvaluatorVersionEquivalence;
import com.learney.contentaudit.quizinstructioninfrastructure.AgentJudgeConfig;
import com.learney.contentaudit.quizinstructioninfrastructure.AgentJudgeSpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DefaultAgentJudgeVersionCutoverTest {

    private static final Instant CUTOVER = Instant.parse("2026-10-18T15:00:00Z");

    private static final Clock CLOCK = Clock.fixed(CUTOVER, ZoneOffset.UTC);

    private static final AgentJudgeSpec MINI_THEORY =
            new AgentJudgeSpec("mini-theory-judge", "miniTheoryVerdict", "juez de la miniteoría");

    private static final AgentJudgeConfig CONFIG =
            new AgentJudgeConfig("claude-cli", null, null, "claude-sonnet-4-5", 0.0, 120, "medium");

    private static final LlmProviderResolved CLAUDE_CLI =
            new LlmProviderResolved("claude-cli", LlmProviderClass.LOCAL_SESSION);

    private static AgentDefinitionLocator locatorOver(Path agentsDir) {
        return new DefaultAgentDefinitionLocatorFactory().create(new AgentGraphRunnerConfig(agentsDir, null));
    }

    private static AgentJudge judgeOver(AgentDefinitionLocator locator, AgentJudgeSpec spec) {
        return new AgentJudge(spec, CONFIG, null, new DefaultAgentJudgeVersionResolver(locator),
                (config, provider) -> null, CLAUDE_CLI, null);
    }

    @Test
    @DisplayName("should write once, for each judge, that the version counting its tests is the same version as the one counted now, and correct the definition that said they counted")
    @Tag("FEAT-ALDIA")
    @Tag("F-ALDIA-R002")
    public void shouldWriteOnceThatTheVersionCountingTestsIsTheSameAsTheCurrentOne(@TempDir Path agentsDir)
            throws IOException {
        Path miniTheory = DefaultAgentJudgeVersionResolverTest.writeFullJudge(agentsDir, "mini-theory-judge");
        DefaultAgentJudgeVersionResolverTest.writeFullJudge(agentsDir, "single-answer-judge");
        // A judge with nothing under eval/ and no paragraph to correct: its two counts are the same.
        Files.createDirectories(agentsDir.resolve("choice-quality-judge").resolve("nodes"));
        Files.writeString(agentsDir.resolve("choice-quality-judge").resolve("agent.yaml"), "name: choice-quality-judge\n");
        Files.writeString(agentsDir.resolve("choice-quality-judge").resolve("nodes").resolve("evaluate.md"), "Juzgá.");
        // The consigna judge is not given to the cut-over: its version keeps its own rule.
        DefaultAgentJudgeVersionResolverTest.writeFullJudge(agentsDir, "quiz-instruction-validator");
        AgentDefinitionLocator locator = locatorOver(agentsDir);
        String recordedUnder = DefaultAgentJudgeVersionResolver.versionCountingTests("mini-theory-judge", miniTheory)
                .orElseThrow();
        String definitionBefore = Files.readString(miniTheory.resolve("agent.yaml"));
        DefaultAgentJudgeVersionCutover cutover = new DefaultAgentJudgeVersionCutover(locator,
                Set.of("mini-theory-judge", "single-answer-judge", "choice-quality-judge"), CLOCK);

        List<EvaluatorVersionEquivalence> preview = cutover.preview();
        assertEquals(List.of("mini-theory-judge", "single-answer-judge"),
                preview.stream().map(EvaluatorVersionEquivalence::getEvaluatorId).toList(),
                "only the judges whose two counts differ get a line, and never the consigna judge");
        assertFalse(Files.exists(agentsDir.resolve(AgentJudgeVersionEquivalenceFile.FILE_NAME)),
                "the preview writes nothing");
        assertEquals(definitionBefore, Files.readString(miniTheory.resolve("agent.yaml")));
        assertTrue(cutover.recorded().isEmpty());

        List<EvaluatorVersionEquivalence> written = cutover.apply();

        assertEquals(preview, written, "it writes what it previewed");
        EvaluatorVersionEquivalence line = written.get(0);
        assertEquals(recordedUnder, line.getEarlierVersion(),
                "the earlier version is the one the exams were recorded under, tests included");
        String current = new DefaultAgentJudgeVersionResolver(locator).resolve(MINI_THEORY, CONFIG, CLAUDE_CLI)
                .orElseThrow();
        assertEquals(current, line.getCurrentVersion(), "the current one is the version measured from now on");
        assertNotEquals(line.getEarlierVersion(), line.getCurrentVersion());
        assertEquals(CUTOVER, line.getRecordedAt());
        assertTrue(line.getReason().contains("F-ALDIA-R001") && line.getReason().contains("texto de la definición"),
                "it says why, and that the definition was corrected: " + line.getReason());
        assertEquals(written, cutover.recorded(), "it stays written with judge, both versions, the moment and why");

        String definitionAfter = Files.readString(miniTheory.resolve("agent.yaml"));
        assertFalse(definitionAfter.contains("eval/ incluido"), definitionAfter);
        assertTrue(definitionAfter.contains("menos eval/"), definitionAfter);
        assertTrue(definitionAfter.startsWith("name: mini-theory-judge\ndescription: |\n"));
        assertTrue(definitionAfter.endsWith("kind: agent\nstrategy: graph\ninitial_node: evaluate\n"),
                "nothing outside the paragraph moves: " + definitionAfter);

        AgentJudge judge = judgeOver(locator, MINI_THEORY);
        assertEquals(current, judge.evaluatorVersion().orElseThrow());
        assertTrue(judge.compatibleEvaluatorVersions().containsAll(Set.of(current, recordedUnder)),
                "an exam and verdicts recorded under the earlier count are the current version's");
        assertEquals(List.of(line), judge.versionEquivalences());
    }

    @Test
    @DisplayName("should refuse a second cut-over and ignore any equivalence added after it")
    @Tag("FEAT-ALDIA")
    @Tag("F-ALDIA-R002")
    public void shouldRefuseASecondCutoverAndIgnoreEquivalencesAddedAfterIt(@TempDir Path agentsDir)
            throws IOException {
        Path miniTheory = DefaultAgentJudgeVersionResolverTest.writeFullJudge(agentsDir, "mini-theory-judge");
        AgentDefinitionLocator locator = locatorOver(agentsDir);
        DefaultAgentJudgeVersionCutover cutover =
                new DefaultAgentJudgeVersionCutover(locator, Set.of("mini-theory-judge"), CLOCK);
        EvaluatorVersionEquivalence first = cutover.apply().get(0);

        IllegalStateException refused = assertThrows(IllegalStateException.class, cutover::apply);
        assertTrue(refused.getMessage().contains("una sola vez") && refused.getMessage().contains("F-ALDIA-R002"),
                refused.getMessage());

        // A line added by hand later, making a version of another guide the same as the current one.
        String current = first.getCurrentVersion();
        Files.writeString(agentsDir.resolve(AgentJudgeVersionEquivalenceFile.FILE_NAME),
                "{\"agent\":\"mini-theory-judge\",\"earlierVersion\":\"otra-guia\",\"currentVersion\":\"" + current
                        + "\",\"recordedAt\":\"2026-10-25T10:00:00Z\",\"reason\":\"a mano\"}\n",
                StandardCharsets.UTF_8, StandardOpenOption.APPEND);

        AgentJudge judge = judgeOver(locator, MINI_THEORY);
        assertFalse(judge.compatibleEvaluatorVersions().contains("otra-guia"),
                "inv. 4: none is added after that day");
        assertEquals(List.of(first), judge.versionEquivalences());
        assertTrue(Files.readString(miniTheory.resolve("agent.yaml")).contains("menos eval/"));
    }

    @Test
    @DisplayName("should bridge only the change of count: another change since the cut-over, or a version older than the one of that day, is a version without the equivalence")
    @Tag("FEAT-ALDIA")
    @Tag("F-ALDIA-R002")
    public void shouldBridgeOnlyTheChangeOfCount(@TempDir Path agentsDir) throws IOException {
        Path miniTheory = DefaultAgentJudgeVersionResolverTest.writeFullJudge(agentsDir, "mini-theory-judge");
        String olderGuide = DefaultAgentJudgeVersionResolver.versionCountingTests("mini-theory-judge", miniTheory)
                .orElseThrow();
        Files.writeString(miniTheory.resolve("nodes").resolve("evaluate.md"), "Leé la teoría con otra guía.");
        AgentDefinitionLocator locator = locatorOver(agentsDir);
        EvaluatorVersionEquivalence line =
                new DefaultAgentJudgeVersionCutover(locator, Set.of("mini-theory-judge"), CLOCK).apply().get(0);

        AgentJudge judge = judgeOver(locator, MINI_THEORY);
        assertFalse(judge.compatibleEvaluatorVersions().contains(olderGuide),
                "inv. 5: a version from before that day's guide stays its own version");
        assertTrue(judge.compatibleEvaluatorVersions().contains(line.getEarlierVersion()));

        Files.writeString(miniTheory.resolve("nodes").resolve("evaluate.md"), "Y otra guía más, después del corte.");

        assertFalse(judge.compatibleEvaluatorVersions().contains(line.getEarlierVersion()),
                "inv. 2: another change is a new version, without the equivalence and with its own exam");
        assertTrue(judge.versionEquivalences().isEmpty());
    }

    @Test
    @DisplayName("should correct each wording of the paragraph about the version, re-wrapped with its indentation, and leave a definition without it untouched")
    @Tag("FEAT-ALDIA")
    @Tag("F-ALDIA-R002")
    public void shouldCorrectEachWordingOfTheParagraphAboutTheVersion() {
        String naturalness = "name: naturalness-yes-no\n"
                + "description: |\n"
                + "  Naturalidad por sí o no.\n"
                + "\n"
                + "  Todo archivo de esta carpeta, eval/ incluido, entra en la versión del juez\n"
                + "  (definición, modelo y proveedor). Tocar la guía, un script o una fixture es\n"
                + "  una versión nueva, que rinde su examen antes de consultar sobre el curso\n"
                + "  (F-MLOCAL-R006). La salida es determinista.\n"
                + "kind: agent\n";
        String corrected = DefaultAgentJudgeVersionCutover.correctedText(naturalness);
        assertTrue(corrected.startsWith("name: naturalness-yes-no\ndescription: |\n  Naturalidad por sí o no.\n\n"
                + "  Todo archivo de esta carpeta menos eval/ entra en la versión del juez:"), corrected);
        assertTrue(corrected.replace("\n  ", " ").contains("(F-MLOCAL-R006). La salida es determinista.\nkind: agent\n"),
                "the rest of the paragraph stays, and so does what follows it: " + corrected);
        assertFalse(corrected.contains("fixture"), corrected);
        for (String wrapped : corrected.split("\n")) {
            assertTrue(wrapped.length() <= 78, "re-wrapped: " + wrapped);
        }

        String choiceQuality = "name: choice-quality-judge\ndescription: |\n"
                + "  Calidad de los distractores.\n"
                + "  Todo archivo de esta carpeta integra la versión efectiva del juez.\n"
                + "kind: agent\n";
        String correctedQuality = DefaultAgentJudgeVersionCutover.correctedText(choiceQuality);
        assertTrue(correctedQuality.startsWith("name: choice-quality-judge\ndescription: |\n"
                + "  Calidad de los distractores.\n  Todo archivo de esta carpeta menos eval/"), correctedQuality);
        assertTrue(correctedQuality.replace("\n  ", " ").contains(
                "integra la versión efectiva del juez: las pruebas no corren al medir y no la cambian "
                        + "(F-ALDIA-R001).\nkind: agent\n"), correctedQuality);

        assertNull(DefaultAgentJudgeVersionCutover.correctedText("name: lemma-absence-agent\ndescription: |\n"
                + "  Busca lemas que faltan.\nkind: agent\n"), "nothing to correct");
    }
}
