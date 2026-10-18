package com.learney.contentaudit.quizinstructioninfrastructure.agentjudge;

import com.learney.contentaudit.agentruntimeinfrastructure.AgentDefinitionFound;
import com.learney.contentaudit.agentruntimeinfrastructure.AgentDefinitionLocation;
import com.learney.contentaudit.agentruntimeinfrastructure.AgentDefinitionLocator;
import com.learney.contentaudit.evaluationledgerdomain.EvaluatorVersionEquivalence;
import com.learney.contentaudit.quizinstructioninfrastructure.AgentJudgeVersionCutover;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * F-ALDIA-R002 over the agent definitions as they are on disk the day it runs, uncommitted changes
 * included: for each judge it digests the folder as the version was counted until then (tests
 * included), corrects the text of the definition that said the tests counted, and digests it as it
 * is counted now. Only a judge whose two versions differ gets a line. The consigna judge is not
 * among the judges it is given: its version keeps its own rule (F-ALDIA-R001 inv. 4).
 */
public class DefaultAgentJudgeVersionCutover implements AgentJudgeVersionCutover {
    static final String DEFINITION_FILE = "agent.yaml";

    static final String REASON = "Las pruebas de eval/ dejaron de contar para la versión del juez "
            + "(F-ALDIA-R001): la versión que las contaba es la misma (F-ALDIA-R002).";

    static final String REASON_WITH_TEXT = REASON
            + " Se corrigió el texto de la definición, que decía que contaban.";

    /** The three wordings the definitions carry today, and what each says from now on. */
    private static final Map<String, String> TEXT_CORRECTIONS = new LinkedHashMap<>();

    static {
        String now = "Todo archivo de esta carpeta menos eval/ entra en la versión del juez: las pruebas "
                + "no corren al medir y no la cambian (F-ALDIA-R001). Tocar la guía, el formato, un "
                + "script o la definición es una versión nueva,";
        TEXT_CORRECTIONS.put("Todo archivo de esta carpeta, eval/ incluido, entra en la versión del juez "
                + "(la del juez de consigna: definición, modelo y proveedor). Tocar la guía, un script o "
                + "una fixture es una versión nueva,", now);
        TEXT_CORRECTIONS.put("Todo archivo de esta carpeta, eval/ incluido, entra en la versión del juez "
                + "(definición, modelo y proveedor). Tocar la guía, un script o una fixture es una "
                + "versión nueva,", now);
        TEXT_CORRECTIONS.put("Todo archivo de esta carpeta integra la versión efectiva del juez.",
                "Todo archivo de esta carpeta menos eval/ integra la versión efectiva del juez: las "
                        + "pruebas no corren al medir y no la cambian (F-ALDIA-R001).");
    }

    private static final String PARAGRAPH_START = "Todo archivo de esta carpeta";

    private static final int WIDTH = 78;

    private final AgentDefinitionLocator agentDefinitionLocator;

    private final Set<String> judgeAgents;

    private final Clock clock;

    private final AgentJudgeVersionEquivalenceFile equivalences = new AgentJudgeVersionEquivalenceFile();

    public DefaultAgentJudgeVersionCutover(AgentDefinitionLocator agentDefinitionLocator,
            Set<String> judgeAgents, Clock clock) {
        this.agentDefinitionLocator = agentDefinitionLocator;
        this.judgeAgents = new TreeSet<>(judgeAgents);
        this.clock = clock;
    }

    @Override
    public List<EvaluatorVersionEquivalence> preview() {
        return plan(now()).stream().map(Step::equivalence).toList();
    }

    @Override
    public List<EvaluatorVersionEquivalence> apply() {
        Path agentsDir = agentsDirectory();
        if (equivalences.exists(agentsDir)) {
            throw new IllegalStateException("La equivalencia entre la versión que contaba las pruebas y la "
                    + "de ahora ya se anotó (" + agentsDir.resolve(AgentJudgeVersionEquivalenceFile.FILE_NAME)
                    + "): es una sola vez y no se agrega otra después (F-ALDIA-R002 inv. 4).");
        }
        List<Step> steps = plan(now());
        List<EvaluatorVersionEquivalence> written = steps.stream().map(Step::equivalence).toList();
        if (written.isEmpty()) {
            // Nothing differs: there is no equivalence to write, and writing none keeps nothing.
            return written;
        }
        try {
            // The equivalences first: if correcting a text failed after, the judge would show a
            // version without exam -- visible and safe -- instead of a corrected definition whose
            // earlier version can no longer be computed.
            equivalences.writeOnce(agentsDir, written);
            for (Step step : steps) {
                if (step.correctedDefinition() != null) {
                    Files.write(step.directory().resolve(DEFINITION_FILE), step.correctedDefinition());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo anotar la equivalencia en " + agentsDir, e);
        }
        return written;
    }

    @Override
    public List<EvaluatorVersionEquivalence> recorded() {
        return equivalences.read(agentsDirectory());
    }

    private List<Step> plan(Instant recordedAt) {
        List<Step> steps = new ArrayList<>();
        for (String agentName : judgeAgents) {
            AgentDefinitionLocation location = agentDefinitionLocator.locate(agentName);
            if (!(location instanceof AgentDefinitionFound found) || found.getDirectory() == null) {
                continue;
            }
            Path directory = found.getDirectory();
            Optional<String> earlier = DefaultAgentJudgeVersionResolver.versionCountingTests(agentName, directory);
            byte[] corrected = correctedDefinition(directory.resolve(DEFINITION_FILE));
            Optional<String> current = DefaultAgentJudgeVersionResolver.processVersion(agentName, directory,
                    corrected != null ? Map.of(DEFINITION_FILE, corrected) : Map.of());
            if (earlier.isEmpty() || current.isEmpty() || earlier.get().equals(current.get())) {
                continue;
            }
            steps.add(new Step(directory, corrected, new EvaluatorVersionEquivalence(agentName,
                    earlier.get(), current.get(), recordedAt, corrected != null ? REASON_WITH_TEXT : REASON)));
        }
        return steps;
    }

    /**
     * The definition with its paragraph about the version saying what is counted now, or null when
     * it does not carry one of the known wordings. Only that paragraph changes, re-wrapped with the
     * same indentation; every other byte stays.
     */
    static byte[] correctedDefinition(Path definition) {
        String text;
        try {
            text = Files.readString(definition, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
        String corrected = correctedText(text);
        return corrected != null ? corrected.getBytes(StandardCharsets.UTF_8) : null;
    }

    static String correctedText(String text) {
        List<String> lines = new ArrayList<>(List.of(text.split("\n", -1)));
        for (int start = 0; start < lines.size(); start++) {
            String line = lines.get(start);
            String indent = line.substring(0, line.length() - line.stripLeading().length());
            if (indent.isEmpty() || !line.stripLeading().startsWith(PARAGRAPH_START)) {
                continue;
            }
            int end = start + 1;
            while (end < lines.size() && !lines.get(end).isBlank() && lines.get(end).startsWith(indent)
                    && !Character.isWhitespace(lines.get(end).charAt(indent.length()))) {
                end++;
            }
            String paragraph = String.join(" ", lines.subList(start, end).stream().map(String::strip).toList());
            String rewritten = paragraph;
            for (Map.Entry<String, String> correction : TEXT_CORRECTIONS.entrySet()) {
                rewritten = rewritten.replace(correction.getKey(), correction.getValue());
            }
            if (rewritten.equals(paragraph)) {
                continue;
            }
            List<String> wrapped = wrap(rewritten, indent);
            List<String> result = new ArrayList<>(lines.subList(0, start));
            result.addAll(wrapped);
            result.addAll(lines.subList(end, lines.size()));
            return String.join("\n", result);
        }
        return null;
    }

    private static List<String> wrap(String paragraph, String indent) {
        List<String> wrapped = new ArrayList<>();
        StringBuilder line = new StringBuilder(indent);
        for (String word : paragraph.split(" ")) {
            if (line.length() > indent.length() && line.length() + 1 + word.length() > WIDTH) {
                wrapped.add(line.toString());
                line = new StringBuilder(indent);
            }
            if (line.length() > indent.length()) {
                line.append(' ');
            }
            line.append(word);
        }
        wrapped.add(line.toString());
        return wrapped;
    }

    private Path agentsDirectory() {
        return agentDefinitionLocator.agentsDirectory().orElseThrow(() -> new IllegalStateException(
                "No se sabe en qué carpeta están las definiciones de los jueces: no hay dónde anotar "
                        + "la equivalencia (F-ALDIA-R002)."));
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.SECONDS);
    }

    private record Step(Path directory, byte[] correctedDefinition, EvaluatorVersionEquivalence equivalence) {
    }
}
