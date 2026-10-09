import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.learney.contentaudit.agentruntimeinfrastructure.*;
import com.learney.contentaudit.agentruntimeinfrastructure.graphexecution.*;
import com.learney.contentaudit.agentruntimeinfrastructure.llmprovider.*;
import com.learney.contentaudit.auditapplication.DefaultJudgeRuntimeConfig;
import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.auditnodeindex.DefaultAuditNodeIndexFactory;
import com.learney.contentaudit.auditdomain.catalog.*;
import com.learney.contentaudit.auditdomain.choicequality.*;
import com.learney.contentaudit.auditdomain.correctionengine.DefaultAnalyzerCorrectionCatalog;
import com.learney.contentaudit.auditdomain.correctormatch.DefaultCorrectorMatcher;
import com.learney.contentaudit.auditdomain.multiplechoicevalidity.MultipleChoiceValidityAnalyzerProvider;
import com.learney.contentaudit.vocabularyinfrastructure.correctortable.FileSystemFormEquivalenceTableSource;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.auditdomain.findingengine.DefaultFindingCollector;
import com.learney.contentaudit.auditdomain.judging.*;
import com.learney.contentaudit.auditdomain.judgingengine.DefaultJudgeRuntime;
import com.learney.contentaudit.auditdomain.studentview.*;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.evaluationledgerdomain.*;
import com.learney.contentaudit.evaluationledgerdomain.contentfingerprint.Sha256ContentFingerprinter;
import com.learney.contentaudit.evaluationledgerdomain.evaluationsession.*;
import com.learney.contentaudit.evaluationledgerinfrastructure.FileSystemEvaluationLedger;
import com.learney.contentaudit.quizinstructioninfrastructure.*;
import com.learney.contentaudit.quizinstructioninfrastructure.agentjudge.*;
import com.learney.contentaudit.quizinstructioninfrastructure.choicequality.JacksonChoiceQualityVerdictReader;
import com.learney.contentaudit.refinerdomain.*;
import com.learney.contentaudit.refinerdomain.errortask.*;
import com.learney.contentaudit.revisiondomain.panel.*;
import com.learney.contentaudit.revisioninfrastructure.lagen.LagenConfig;
import com.learney.contentaudit.revisioninfrastructure.panelagent.DefaultPanelAgentProposerFactory;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;

/** Isolated integration harness, not a course correction or an official exam approval. */
public class ChoiceCorrectionCycle {
    static final ObjectMapper JSON = new ObjectMapper();
    static final String EXPERIMENT = "choice-correction-cycle-20261009-v2";
    static final String QUALITY = "choice-quality-judge", ALTERNATIVE = "choice-alternative-judge";
    static final String MODEL = "gpt-6.1-sol";
    final Path root, experiment;
    final JsonNode corpus;
    final QuizStudentViewRenderer renderer = new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create());
    final AgentGraphRunner runner;
    final AgentDefinitionLocator locator;
    final Evaluator quality, alternative;
    final FileSystemEvaluationLedger ledger;
    final JudgeRuntime runtime;

    ChoiceCorrectionCycle(Path root, Path experiment) throws Exception {
        this.root = root.toRealPath(); this.experiment = experiment.toRealPath();
        corpus = JSON.readTree(experiment.resolve("cases.json").toFile());
        var config = new AgentGraphRunnerConfig(root.resolve(".sentinel/agents"), experiment.resolve("runs"));
        runner = new DefaultAgentGraphRunnerFactory().create(config);
        locator = new DefaultAgentDefinitionLocatorFactory().create(config);
        var factory = new DefaultAgentJudgeFactory(new DefaultLlmProviderResolver(), new DefaultJudgeProviderReadinessProbe(List.of("codex", "login", "status")));
        var llm = new AgentJudgeConfig("codex-cli", null, null, MODEL, 0.0, 180, "medium");
        quality = factory.create(new AgentJudgeSpec(QUALITY, "choiceQualityVerdict", "calidad"), llm, runner, locator);
        alternative = factory.create(new AgentJudgeSpec(ALTERNATIVE, "choiceAlternativeVerdict", "respuesta única"), llm, runner, locator);
        ledger = new FileSystemEvaluationLedger(experiment.resolve("ledger"));
        var fingerprinter = new Sha256ContentFingerprinter();
        var sessions = new ExamEvaluationSessionFactory(ledger, fingerprinter);
        JudgeExamRegistry readOnly = new JudgeExamRegistry() {
            public void record(JudgeExamRecord record) { throw new IllegalStateException("No se autoriza aprobar exámenes"); }
            public Optional<JudgeExamRecord> latest(String name, String version) { return Optional.empty(); }
            public List<JudgeExamRecord> history(String name) { return List.of(); }
        };
        runtime = new DefaultJudgeRuntime(sessions, sessions, readOnly, ledger, new DefaultJudgeRuntimeConfig());
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) throw new IllegalArgumentException("mode project-root experiment-dir [case-number]");
        var app = new ChoiceCorrectionCycle(Path.of(args[1]), Path.of(args[2]));
        switch (args[0]) {
            case "preflight" -> app.preflight();
            case "selftest" -> app.selftest();
            case "case" -> app.runCase(Integer.parseInt(args[3]));
            default -> throw new IllegalArgumentException("Unknown mode");
        }
    }

    void preflight() throws Exception {
        if (corpus.path("cases").size() != 16) throw new IllegalStateException("Se requieren 16 casos");
        var out = JSON.createObjectNode();
        out.put("experimentId", EXPERIMENT).put("provider", "codex-cli").put("model", MODEL).put("reasoningEffort", "medium");
        out.put("qualityVersion", quality.evaluatorVersion().orElseThrow());
        out.put("alternativeVersion", alternative.evaluatorVersion().orElseThrow());
        out.put("correctorDefinitionSha256", folderHash(root.resolve(".sentinel/agents/panel-proposer")));
        out.put("casesSha256", hash(Files.readAllBytes(experiment.resolve("cases.json"))));
        out.put("harnessSha256", hash(Files.readAllBytes(root.resolve("scripts/exams/ChoiceCorrectionCycle.java"))));
        out.put("cappedRuntimeSha256", hash(Files.readAllBytes(root.resolve("scripts/exams/capped-codex"))));
        out.put("correctorTableSha256", hash(Files.readAllBytes(root.resolve("analysis/recursos-compartidos/corrector/formas-equivalentes.json"))));
        var inputs = out.putArray("subjects");
        int defects = 0;
        for (var c : corpus.path("cases")) {
            if (c.path("expected").asText().equals("FAIL")) defects++;
            var tree = tree(c, null);
            var subject = new ChoiceQualitySubjectBuilder(renderer).build(tree.quiz());
            if (!subject.getContent().get("exercise").replaceAll("\\s+([.;?!,])", "$1").equals(c.path("exercise").asText())) throw new IllegalStateException("Renderer changed case " + c.path("caseNumber") + ": " + subject.getContent().get("exercise"));
            inputs.add(JSON.valueToTree(subject));
        }
        if (defects != 9) throw new IllegalStateException("El plan autoriza 9 defectos");
        write(experiment.resolve("preflight.json"), out);
        System.out.println("PREFLIGHT: 16 casos; 9 defectos; 7 controles; versiones explícitas; cero consultas.");
    }

    void verifyFrozen() throws Exception {
        var saved = JSON.readTree(experiment.resolve("preflight.json").toFile());
        if (!saved.path("qualityVersion").asText().equals(quality.evaluatorVersion().orElseThrow())
                || !saved.path("alternativeVersion").asText().equals(alternative.evaluatorVersion().orElseThrow())
                || !saved.path("correctorDefinitionSha256").asText().equals(folderHash(root.resolve(".sentinel/agents/panel-proposer")))
                || !saved.path("casesSha256").asText().equals(hash(Files.readAllBytes(experiment.resolve("cases.json"))))
                || !saved.path("harnessSha256").asText().equals(hash(Files.readAllBytes(root.resolve("scripts/exams/ChoiceCorrectionCycle.java"))))
                || !saved.path("cappedRuntimeSha256").asText().equals(hash(Files.readAllBytes(root.resolve("scripts/exams/capped-codex"))))
                || !saved.path("correctorTableSha256").asText().equals(hash(Files.readAllBytes(root.resolve("analysis/recursos-compartidos/corrector/formas-equivalentes.json")))))
            throw new IllegalStateException("Cambió la versión o el corpus congelado; no se consulta");
        var auth = JSON.readTree(experiment.resolve("authorization.json").toFile());
        if (!EXPERIMENT.equals(auth.path("experimentId").asText()) || auth.path("physicalCallCap").asInt() != 50
                || !"sí".equals(auth.path("userReply").asText())) throw new IllegalStateException("Autorización inválida");
        if (!"50".equals(System.getenv("CONTENT_AUDIT_EXAM_PHYSICAL_CAP"))
                || !experiment.equals(Path.of(System.getenv("CONTENT_AUDIT_EXAM_CAP_DIR")).toRealPath())
                || !"medium".equals(System.getenv("CONTENT_AUDIT_REASONING_EFFORT"))) throw new IllegalStateException("Runtime fuera del plan");
    }

    void runCase(int number) throws Exception {
        verifyFrozen();
        if (number < 1 || number > 16) throw new IllegalArgumentException("Caso inválido");
        var c = corpus.path("cases").get(number - 1);
        var dir = experiment.resolve("case-" + String.format("%02d", number)); Files.createDirectories(dir);
        try (var channel = FileChannel.open(dir.resolve("worker.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = channel.tryLock()) {
            if (lock == null) throw new IllegalStateException("Otro proceso evalúa este caso");
            if (Files.exists(dir.resolve("result.json"))) { System.out.println("REUSED case " + number); return; }
            var row = JSON.createObjectNode().put("caseNumber", number).put("id", c.path("id").asText());
            row.set("original", c);
            var before = tree(c, null);
            quality(before, dir, "initial");
            var verdict = diagnosis(before.quiz());
            row.set("initial", JSON.valueToTree(verdict));
            String initial = verdict != null ? verdict.status().name() : "UNAVAILABLE";
            row.put("initialStatusMatches", initial.equals(c.path("expected").asText()));
            if (c.path("expected").asText().equals("PASS")) {
                row.put("result", initial.equals("PASS") ? "CONTROL_PRESERVED" : "FALSE_POSITIVE_OR_UNAVAILABLE");
                finish(dir, row); return;
            }
            if (!initial.equals("FAIL")) {
                row.put("result", "DEFECT_NOT_DETECTED"); finish(dir, row); return;
            }
            var provider = provider(quality);
            var catalog = catalog(provider);
            var corrections = new DefaultAnalyzerCorrectionCatalog(List.of(new ChoiceQualityCorrection()), catalog);
            var report = new AuditReport(before.root());
            var plan = new DefaultRefinerEngine(catalog, corrections).plan(report, EXPERIMENT);
            var briefer = new DefaultErrorTaskBriefer(corrections, new DefaultAuditNodeIndexFactory(), renderer);
            List<ErrorTaskBrief> briefs = new ArrayList<>();
            for (var task : plan.getTasks()) briefs.add(briefer.brief(report, task).orElseThrow());
            if (briefs.isEmpty()) throw new IllegalStateException("No se fabrican tareas si el análisis no las genera");
            var request = new PanelRequest(EXPERIMENT, CefrLevel.valueOf(c.path("level").asText()),
                    "synthetic-topic-" + number, c.path("id").asText(), List.of(renderer.render((AuditableQuiz) before.quiz().getEntity())),
                    briefs, null, null, 1, List.of(), false);
            row.set("findings", JSON.valueToTree(before.quiz().getFindings()));
            row.set("tasksDelivered", JSON.valueToTree(request));
            write(dir.resolve("corrector-request.json"), JSON.valueToTree(request));
            PanelProposal proposal;
            Path proposalPath = dir.resolve("proposal.json");
            if (Files.exists(proposalPath)) proposal = JSON.readValue(proposalPath.toFile(), PanelProposal.class);
            else {
                start(dir, "corrector");
                var config = new LagenConfig("codex-cli", MODEL, null, null, 0.7, 4096, Duration.ofSeconds(180), 0, 0);
                proposal = new DefaultPanelAgentProposerFactory().create(config).propose(request);
                write(proposalPath, JSON.valueToTree(proposal));
            }
            row.set("proposal", JSON.valueToTree(proposal));
            if (!proposal.isAvailable() || proposal.getCandidates() == null || proposal.getCandidates().size() != 1) {
                row.put("result", "CORRECTOR_UNAVAILABLE_OR_NOT_ONE_CANDIDATE"); finish(dir, row); return;
            }
            var candidate = proposal.getCandidates().get(0);
            var after = tree(c, candidate);
            var violations = screen(before, after, candidate, briefs);
            row.set("localViolations", JSON.valueToTree(violations));
            row.set("after", JSON.valueToTree(new ChoiceQualitySubjectBuilder(renderer).build(after.quiz()).getContent()));
            if (!violations.isEmpty()) { row.put("result", "LOCAL_CONSTRAINT_FAILURE"); finish(dir, row); return; }
            quality(after, dir, "after-quality");
            row.set("afterQuality", JSON.valueToTree(diagnosis(after.quiz())));
            var ambiguity = alternative(after, dir);
            row.set("afterAlternative", ambiguity);
            boolean qualityPass = diagnosis(after.quiz()) != null && diagnosis(after.quiz()).status() == ChoiceQualityVerdict.Status.PASS;
            boolean unique = ambiguity.path("correctIsValid").asBoolean(false) && ambiguity.path("validAlternatives").isArray()
                    && ambiguity.path("validAlternatives").isEmpty();
            row.put("automaticCyclePass", qualityPass && unique);
            row.put("result", qualityPass && unique ? "AUTOMATIC_CHECKS_PASS_REQUIRES_CONTENT_REVIEW" : "CORRECTION_NOT_VALIDATED");
            finish(dir, row);
        }
    }

    void quality(Tree tree, Path dir, String stage) throws Exception {
        boolean started = Files.exists(dir.resolve(stage + ".started.json"));
        var subject = new ChoiceQualitySubjectBuilder(renderer).build(tree.quiz());
        if (!started) start(dir, stage);
        write(dir.resolve(stage + "-subject.json"), JSON.valueToTree(subject));
        var provider = provider(quality);
        var policy = new EvaluationRunPolicy(started ? 0 : 1, false, null, Set.of(subject.getSubjectRef()), EvaluationRunPurpose.EXAM, 1);
        var analyzer = provider.create(policy);
        analyzer.onQuiz(tree.quiz()); analyzer.onCourseComplete(tree.root());
        new DefaultFindingCollector().collect(tree.root(), analyzer, provider.describe());
        if (diagnosis(tree.quiz()) == null || tree.quiz().getUnevaluatedBy().contains(ChoiceQualityAnalyzerProvider.NAME))
            throw new IllegalStateException("Sin juicio consolidado en " + stage + "; no se repite un lanzamiento incierto");
        write(dir.resolve(stage + ".json"), JSON.valueToTree(diagnosis(tree.quiz())));
    }

    JsonNode alternative(Tree tree, Path dir) throws Exception {
        var qualitySubject = new ChoiceQualitySubjectBuilder(renderer).build(tree.quiz());
        Map<String, String> visible = new LinkedHashMap<>();
        for (String key : List.of("title", "instructions", "exercise", "options")) visible.put(key, qualitySubject.getContent().get(key));
        var subject = new EvaluationSubject(qualitySubject.getSubjectRef(), visible);
        boolean started = Files.exists(dir.resolve("after-alternative.started.json"));
        if (!started) start(dir, "after-alternative");
        write(dir.resolve("after-alternative-subject.json"), JSON.valueToTree(subject));
        var session = new ExamEvaluationSessionFactory(ledger, new Sha256ContentFingerprinter()).open(new EvaluationBudget(started ? 0 : 1), alternative);
        var resolution = started ? session.resolveWithoutConsulting(subject) : session.resolve(subject);
        if (resolution.getKind() != EvaluationResolutionKind.EVALUATED && resolution.getKind() != EvaluationResolutionKind.REUSED)
            throw new IllegalStateException("Sin juicio consolidado de respuesta única; no se repite");
        var result = JSON.readTree(resolution.getPayload()); write(dir.resolve("after-alternative.json"), result); return result;
    }

    ChoiceQualityAnalyzerProvider provider(Evaluator evaluator) {
        return new ChoiceQualityAnalyzerProvider(runtime, evaluator, new JacksonChoiceQualityVerdictReader(), renderer);
    }
    static AnalyzerCatalog catalog(AnalyzerProvider provider) {
        return new AnalyzerCatalog() {
            public List<AnalyzerDescriptor> list() { return List.of(provider.describe()); }
            public Optional<AnalyzerDescriptor> find(String name) { return name.equals(provider.analyzerName()) ? Optional.of(provider.describe()) : Optional.empty(); }
            public Optional<AnalyzerProvider> provider(String name) { return name.equals(provider.analyzerName()) ? Optional.of(provider) : Optional.empty(); }
            public Optional<AnalyzerPlanBinding> planBinding(String name) { return provider.planBinding(); }
            public Optional<AnalyzerMeasure> measure(String name) { return Optional.empty(); }
        };
    }
    ChoiceQualityVerdict diagnosis(AuditNode node) {
        if (node.getDiagnoses() instanceof DefaultQuizDiagnoses d && d.getChoiceQualityDiagnosis().isPresent())
            return d.getChoiceQualityDiagnosis().orElseThrow().verdict();
        return null;
    }
    record Tree(AuditNode root, AuditNode quiz) {}
    Tree tree(JsonNode c, PanelCandidate patch) throws Exception {
        AuditableQuiz q = new AuditableQuiz();
        q.setId(c.path("id").asText()); q.setLabel(c.path("title").asText()); q.setInstructions(c.path("instructions").asText());
        q.setTranslation(""); q.setMiniTheory(""); q.setTokens(List.of()); q.setSentences(List.of());
        var parts = new ArrayList<SentencePartEntity>();
        String[] sides = c.path("exercise").asText().split("___", -1);
        if (sides.length != 2) throw new IllegalStateException("Fixture necesita un hueco");
        parts.add(new SentencePartEntity(SentencePartKind.TEXT, sides[0], null));
        parts.add(new SentencePartEntity(SentencePartKind.CLOZE, "", null));
        parts.add(new SentencePartEntity(SentencePartKind.TEXT, sides[1], null)); q.setSentenceParts(parts);
        var items = new ArrayList<MultipleChoiceItemEntity>();
        for (var option : c.path("options")) items.add(new MultipleChoiceItemEntity(option.asText(), option.asText().equals(c.path("correct").asText()) ? 1.0 : 0.0, option.asText()));
        q.setMultipleChoice(new MultipleChoiceEntity("SINGLE", items));
        if (patch != null && patch.getQuiz() != null) {
            var p = patch.getQuiz();
            if (p.getInstructions() != null) q.setInstructions(p.getInstructions());
            if (p.getTranslation() != null) q.setTranslation(p.getTranslation());
            if (p.getMiniTheory() != null) q.setMiniTheory(p.getMiniTheory());
            if (p.getForm() != null) { q.setSentenceParts(p.getForm().getSentenceParts()); q.setMultipleChoice(p.getForm().getMultipleChoice()); }
        }
        var root = node(null, AuditTarget.COURSE, null);
        var milestone = node(new AuditableMilestone(List.of(), "synthetic-level", c.path("level").asText(), c.path("level").asText()), AuditTarget.MILESTONE, root);
        var knowledge = node(new AuditableKnowledge(List.of(q), c.path("topic").asText(), c.path("instructions").asText(), false,
                "synthetic-topic-" + c.path("caseNumber").asInt(), c.path("topic").asText(), "synthetic", SentenceMode.FILL, c.path("topic").asText()), AuditTarget.KNOWLEDGE, milestone);
        return new Tree(root, node(q, AuditTarget.QUIZ, knowledge));
    }
    static AuditNode node(AuditableEntity entity, AuditTarget target, AuditNode parent) {
        var node = new AuditNode(entity, target, parent, new ArrayList<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), null);
        if (parent != null) parent.getChildren().add(node); return node;
    }
    List<String> screen(Tree before, Tree after, PanelCandidate patch, List<ErrorTaskBrief> briefs) {
        var failures = new ArrayList<String>();
        var q = (AuditableQuiz) before.quiz().getEntity(); var p = (AuditableQuiz) after.quiz().getEntity();
        if (patch.getKind() != PanelCandidateKind.WHOLE_EXERCISE) failures.add("Sólo se autoriza candidato de ejercicio completo");
        if (patch.getQuiz() == null || patch.getQuiz().getId() != null && !patch.getQuiz().getId().equals(q.getId())) failures.add("Identidad distinta");
        if (p.getMultipleChoice() == null || p.getMultipleChoice().getItems().size() != q.getMultipleChoice().getItems().size()) failures.add("Cambió la cantidad o el tipo de opciones");
        else {
            var choices = p.getMultipleChoice().getItems();
            if (choices.stream().filter(i -> i.getIncidence() > 0).count() != 1) failures.add("No hay una sola clave marcada");
            Set<String> labels = new HashSet<>();
            for (var i : choices) if (i.getLabel() == null || i.getLabel().isBlank() || !labels.add(i.getLabel().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip())) failures.add("Opción vacía o duplicada formal");
        }
        if (p.getSentenceParts() == null || p.getSentenceParts().stream().filter(s -> s.getKind() == SentencePartKind.CLOZE).count() != 1) failures.add("Debe conservar un hueco");
        if (!Objects.equals(q.getMiniTheory(), p.getMiniTheory()) || patch.getMiniTheoryText() != null) failures.add("Miniteoría fuera de alcance");
        if (patch.getTopicInstructions() != null) failures.add("Consigna del tema fuera de alcance");
        Set<com.learney.contentaudit.auditdomain.correction.ExercisePart> scope = new HashSet<>();
        for (var brief : briefs) scope.addAll(brief.getEditScope());
        if (scope.isEmpty()) failures.add("Sin alcance declarado");
        String oldStatement = renderer.render(q).getStatement(), newStatement = renderer.render(p).getStatement();
        if (!Objects.equals(q.getInstructions(), p.getInstructions()) && !scope.contains(com.learney.contentaudit.auditdomain.correction.ExercisePart.INSTRUCTIONS)) failures.add("Consigna fuera de alcance");
        if (!Objects.equals(q.getTranslation(), p.getTranslation()) && !scope.contains(com.learney.contentaudit.auditdomain.correction.ExercisePart.TRANSLATION)) failures.add("Traducción fuera de alcance");
        if (!Objects.equals(q.getMultipleChoice(), p.getMultipleChoice()) && !scope.contains(com.learney.contentaudit.auditdomain.correction.ExercisePart.OPTIONS)) failures.add("Opciones fuera de alcance");
        if (!oldStatement.equals(newStatement) && !scope.contains(com.learney.contentaudit.auditdomain.correction.ExercisePart.STATEMENT)) failures.add("Enunciado fuera de alcance");
        for (String subject : List.of("she", "it", "you")) if (oldStatement.contains("(" + subject + ")") && !newStatement.contains("(" + subject + ")")
                && !q.getInstructions().contains("reconoce la opción")) failures.add("Se eliminó la pista necesaria del sujeto");
        if (Objects.equals(q.getMultipleChoice(), p.getMultipleChoice()) && Objects.equals(q.getInstructions(), p.getInstructions()) && oldStatement.equals(newStatement)) failures.add("No cambió el ejercicio visible");
        if (p.getSentenceParts() != null && p.getMultipleChoice() != null) {
            var localProvider = new MultipleChoiceValidityAnalyzerProvider(new DefaultCorrectorMatcher(new FileSystemFormEquivalenceTableSource(
                    root.resolve("analysis/recursos-compartidos/corrector/formas-equivalentes.json")).load()), null, renderer);
            var local = localProvider.create(new EvaluationRunPolicy(0, false, null, Set.of(), EvaluationRunPurpose.EXAM, 1));
            local.onQuiz(after.quiz()); local.onCourseComplete(after.root());
            new DefaultFindingCollector().collect(after.root(), local, localProvider.describe());
            for (var finding : after.quiz().getFindings()) failures.add("Control formal local: " + finding.getRule());
        }
        return failures;
    }

    void selftest() throws Exception {
        var c = corpus.path("cases").get(0);
        var tree = tree(c, null);
        Evaluator fake = new Evaluator() {
            public String evaluatorId() { return "offline-cycle-quality"; }
            public Optional<String> evaluatorVersion() { return Optional.of("offline-test-v1"); }
            public EvaluationOutcome evaluate(EvaluationSubject subject) {
                return new EvaluationEmitted("{\"status\":\"FAIL\",\"objective\":\"Inversión con be\",\"introductory\":false,\"issues\":[{\"kind\":\"ANSWER_CUE\",\"severity\":\"HIGH\",\"labels\":[\"Am I\",\"Are we\",\"Is it\"],\"reason\":\"Conservar la pista; las opciones cambian el sujeto.\",\"misconception\":\"Inversión y concordancia\"}],\"rationale\":\"El conjunto permite buscar she\"}");
            }
        };
        var provider = provider(fake);
        var analyzer = provider.create(new EvaluationRunPolicy(1, false, null, Set.of(c.path("id").asText()), EvaluationRunPurpose.EXAM, 1));
        analyzer.onQuiz(tree.quiz()); analyzer.onCourseComplete(tree.root());
        new DefaultFindingCollector().collect(tree.root(), analyzer, provider.describe());
        if (tree.quiz().getFindings().size() != 1) throw new AssertionError("No native finding");
        var catalog = catalog(provider);
        var corrections = new DefaultAnalyzerCorrectionCatalog(List.of(new ChoiceQualityCorrection()), catalog);
        var report = new AuditReport(tree.root());
        var plan = new DefaultRefinerEngine(catalog, corrections).plan(report, "offline-test");
        if (plan.getTasks().size() != 1) throw new AssertionError("No native task");
        var brief = new DefaultErrorTaskBriefer(corrections, new DefaultAuditNodeIndexFactory(), renderer).brief(report, plan.getTasks().get(0)).orElseThrow();
        if (!brief.getFinding().getEvidence().getObservation().contains("Conservar la pista")) throw new AssertionError("Evidence lost");
        if (!brief.getEditScope().contains(com.learney.contentaudit.auditdomain.correction.ExercisePart.OPTIONS)) throw new AssertionError("Scope lost");
        var subject = new ChoiceQualitySubjectBuilder(renderer).build(tree.quiz()).getContent();
        if (subject.keySet().size() != 6 || subject.containsKey("expected") || subject.containsKey("miniTheory")) throw new AssertionError("Blind judge context leak");
        var quiz = (AuditableQuiz) tree.quiz().getEntity();
        var template = new QuizTemplateEntity();
        template.setForm(new FormEntity("MULTIPLE_CHOICE", 1.0, "", "", quiz.getSentenceParts(),
                new MultipleChoiceEntity("SINGLE", List.of(new MultipleChoiceItemEntity("She is", 0, "She is"),
                new MultipleChoiceItemEntity("Are she", 0, "Are she"), new MultipleChoiceItemEntity("Is she", 1, "Is she"),
                new MultipleChoiceItemEntity("Am she", 0, "Am she"))), null));
        var candidate = new PanelCandidate(PanelCandidateKind.WHOLE_EXERCISE, template, null, null, "offline");
        if (!screen(tree, tree(c, candidate), candidate, List.of(brief)).isEmpty()) throw new AssertionError("Valid repair rejected");
        template.setMiniTheory("new text");
        if (screen(tree, tree(c, candidate), candidate, List.of(brief)).stream().noneMatch(s -> s.contains("Miniteoría"))) throw new AssertionError("Scope guard missing");
        template.setMiniTheory(null);
        template.getForm().getMultipleChoice().getItems().get(0).setLabel("is she");
        if (screen(tree, tree(c, candidate), candidate, List.of(brief)).stream().noneMatch(s -> s.contains("duplicada"))) throw new AssertionError("Duplicate guard missing");
        System.out.println("SELFTEST PASSED: native finding/task, scope, evidence, blind context, valid repair, out-of-scope and duplicate rejection. No model.");
    }

    static void finish(Path dir, ObjectNode row) throws Exception {
        row.put("finishedAt", Instant.now().toString()); write(dir.resolve("result.json"), row);
        System.out.println("FINISHED case " + row.path("caseNumber") + ": " + row.path("result").asText());
    }
    static void start(Path dir, String stage) throws Exception {
        Path path = dir.resolve(stage + ".started.json");
        if (Files.exists(path)) throw new IllegalStateException("Etapa incierta ya iniciada: " + stage + "; no se relanza");
        write(path, JSON.createObjectNode().put("stage", stage).put("startedAt", Instant.now().toString()));
    }
    static void write(Path path, JsonNode json) throws Exception {
        Files.createDirectories(path.getParent());
        byte[] bytes = (JSON.writerWithDefaultPrettyPrinter().writeValueAsString(json) + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try (var channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            var buffer = ByteBuffer.wrap(bytes); while (buffer.hasRemaining()) channel.write(buffer); channel.force(true);
        }
    }
    static String hash(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    static String folderHash(Path folder) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        try (var stream = Files.walk(folder)) {
            for (var file : stream.filter(Files::isRegularFile).filter(p -> !p.toString().contains("__pycache__")).sorted().toList()) {
                digest.update(folder.relativize(file).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                digest.update((byte) 0); digest.update(Files.readAllBytes(file));
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
