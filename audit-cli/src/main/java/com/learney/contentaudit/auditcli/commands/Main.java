package com.learney.contentaudit.auditcli.commands;
import javax.annotation.processing.Generated;

import com.learney.contentaudit.auditapplication.CourseToAuditableMapper;
import com.learney.contentaudit.auditapplication.DefaultAuditRunner;
import com.learney.contentaudit.auditapplication.DefaultSentenceLengthConfig;
import com.learney.contentaudit.auditapplication.DefaultAnalyzerRegistry;
import com.learney.contentaudit.auditapplication.DefaultCocaBucketsConfig;
import com.learney.contentaudit.auditapplication.DefaultLemmaRecurrenceConfig;
import com.learney.contentaudit.auditapplication.DefaultLemmaAbsenceConfig;
import com.learney.contentaudit.auditapplication.DefaultLemmaCountConfig;
import com.learney.contentaudit.auditapplication.DefaultLemmaCountConfigLoader;
import com.learney.contentaudit.auditdomain.ContentAnalyzer;
import com.learney.contentaudit.auditdomain.IAuditEngine;
import com.learney.contentaudit.auditdomain.IScoreAggregator;
import com.learney.contentaudit.auditdomain.KnowledgeInstructionsLengthAnalyzer;
import com.learney.contentaudit.auditdomain.KnowledgeTitleLengthAnalyzer;
import com.learney.contentaudit.auditdomain.NlpTokenizer;
import com.learney.contentaudit.auditdomain.ScoreAggregator;
import com.learney.contentaudit.auditdomain.SentenceLengthAnalyzer;
import com.learney.contentaudit.auditdomain.SentenceLengthConfig;
import com.learney.contentaudit.auditdomain.CocaBucketsConfig;
import com.learney.contentaudit.auditdomain.LemmaRecurrenceConfig;
import com.learney.contentaudit.auditdomain.LemmaAbsenceConfig;
import com.learney.contentaudit.auditdomain.SelfDescribingConfig;
import com.learney.contentaudit.auditdomain.coca.CocaBucketsAnalyzer;
import com.learney.contentaudit.auditdomain.coca.DefaultTokenClassifier;
import com.learney.contentaudit.auditdomain.coca.DefaultProgressionEvaluator;
import com.learney.contentaudit.auditdomain.coca.DefaultImprovementPlanner;
import com.learney.contentaudit.auditdomain.lrec.LemmaRecurrenceAnalyzer;
import com.learney.contentaudit.auditdomain.lrec.DefaultContentWordFilter;
import com.learney.contentaudit.auditdomain.lrec.DefaultIntervalCalculator;
import com.learney.contentaudit.auditdomain.lrec.DefaultExposureClassifier;
import com.learney.contentaudit.auditdomain.labs.LemmaByLevelAbsenceAnalyzer;
import com.learney.contentaudit.auditdomain.lemmacount.EvpThenNlpLemmaCefrLevelResolver;
import com.learney.contentaudit.auditdomain.lemmacount.LemmaCountAnalyzer;
import com.learney.contentaudit.vocabularyinfrastructure.evp.FileSystemEvpCatalog;
import com.learney.contentaudit.nlpinfrastructure.NlpTokenizerConfig;
import com.learney.contentaudit.nlpinfrastructure.spacy.SpacyNlpTokenizerFactory;
import com.learney.contentaudit.coursedomain.CourseValidator;
import com.learney.contentaudit.coursedomain.CourseRepository;
import com.learney.contentaudit.coursedomain.quizsentence.QuizSentenceConverter;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.courseinfrastructure.CourseValidatorImpl;
import com.learney.contentaudit.courseinfrastructure.FileSystemCourseRepository;
import com.learney.contentaudit.auditdomain.AuditReportStore;
import com.learney.contentaudit.auditinfrastructure.FileSystemAuditReportStore;
import com.learney.contentaudit.auditinfrastructure.FileSystemRefinementPlanStore;
import com.learney.contentaudit.auditinfrastructure.FileSystemRevisionArtifactStore;
import com.learney.contentaudit.refinerdomain.CorrectionContextResolver;
import com.learney.contentaudit.refinerdomain.DispatchingCorrectionContextResolver;
import com.learney.contentaudit.refinerdomain.KnowledgeTitleContextResolver;
import com.learney.contentaudit.refinerdomain.LemmaAbsenceContextResolver;
import com.learney.contentaudit.refinerdomain.QuizInstructionContextResolver;
import com.learney.contentaudit.refinerdomain.SentenceLengthContextResolver;
import com.learney.contentaudit.refinerdomain.DefaultRefinerEngine;
import com.learney.contentaudit.refinerdomain.RefinerEngine;
import com.learney.contentaudit.refinerdomain.RefinementPlanStore;
import com.learney.contentaudit.refinerdomain.DiagnosisKind;
import com.learney.contentaudit.refinerdomain.lemmasuggestion.LemmaAbsenceContextSuggestedLemmaQueryPort;
import com.learney.contentaudit.revisiondomain.LemmaAbsenceProposalDeriver;
import com.learney.contentaudit.revisiondomain.LemmaAbsenceProposalStrategyRegistry;
import com.learney.contentaudit.revisiondomain.KnowledgeTitleProposalDeriver;
import com.learney.contentaudit.revisiondomain.KnowledgeTitleProposalStrategyRegistry;
import com.learney.contentaudit.revisiondomain.ProposalStrategyFailedException;
import com.learney.contentaudit.revisiondomain.RevisionEngine;
import com.learney.contentaudit.revisiondomain.RevisionEngineConfig;
import com.learney.contentaudit.revisiondomain.Reviser;
import com.learney.contentaudit.revisiondomain.RevisionArtifactStore;
import com.learney.contentaudit.revisiondomain.StrategyId;
import com.learney.contentaudit.revisiondomain.engine.DefaultLemmaAbsenceProposalDeriver;
import com.learney.contentaudit.revisiondomain.engine.DefaultKnowledgeTitleProposalDeriver;
import com.learney.contentaudit.revisiondomain.engine.DefaultKnowledgeTitleProposalStrategyRegistry;
import com.learney.contentaudit.revisiondomain.engine.DefaultLemmaAbsenceProposalStrategyRegistry;
import com.learney.contentaudit.revisiondomain.engine.DefaultQuizInstructionProposalStrategyRegistry;
import com.learney.contentaudit.revisiondomain.engine.DefaultQuizInstructionCorrectionRunnerFactory;
import com.learney.contentaudit.revisiondomain.engine.QuizInstructionProposalStrategyRegistryConfig;
import com.learney.contentaudit.revisiondomain.QuizInstructionProposalStrategyRegistry;
import com.learney.contentaudit.revisiondomain.QuizInstructionCorrectionRunner;
import com.learney.contentaudit.revisiondomain.QuizInstructionCorrectionRunStore;
import com.learney.contentaudit.revisiondomain.quizinstruction.QuizInstructionCandidateGenerator;
import com.learney.contentaudit.revisiondomain.quizinstruction.QuizInstructionCorrectionConfig;
import com.learney.contentaudit.revisiondomain.quizinstruction.QuizInstructionGeneratorResponse;
import com.learney.contentaudit.revisiondomain.quizinstruction.QuizInstructionAgentStrategy;
import com.learney.contentaudit.revisiondomain.candidatecriteria.CandidateAssessor;
import com.learney.contentaudit.revisiondomain.candidatecriteria.CandidateCriteriaConfig;
import com.learney.contentaudit.revisiondomain.candidatecriteria.CandidateCriterionEvaluator;
import com.learney.contentaudit.revisiondomain.candidatecriteria.CorrectionCriterion;
import com.learney.contentaudit.revisiondomain.candidatecriteria.CriterionOutcome;
import com.learney.contentaudit.revisiondomain.candidatecriteria.CriterionVerdict;
import com.learney.contentaudit.revisiondomain.candidatecriteriaengine.DefaultCandidateCriteriaFactory;
import com.learney.contentaudit.auditdomain.quizinstruction.QuizInstructionComplianceChecker;
import com.learney.contentaudit.auditdomain.quizinstruction.QuizInstructionSubjectViewFactory;
import com.learney.contentaudit.auditdomain.quizinstructionengine.DefaultQuizInstructionComplianceCheckerFactory;
import com.learney.contentaudit.auditdomain.quizinstructionengine.DefaultQuizInstructionSubjectViewFactory;
import com.learney.contentaudit.auditinfrastructure.FileSystemQuizInstructionCorrectionRunStore;
import com.learney.contentaudit.auditcli.bootstrap.EnvQuizInstructionCorrectionConfigResolver;
import com.learney.contentaudit.auditcli.formatting.QuizInstructionCorrectionRunFormatter;
import com.learney.contentaudit.auditcli.formatting.DefaultQuizInstructionCorrectionRunFormatter;
import com.learney.contentaudit.revisioninfrastructure.quizinstructionagent.QuizInstructionAgentGeneratorFactory;
import com.learney.contentaudit.revisioninfrastructure.quizinstructionagent.DefaultQuizInstructionAgentGeneratorFactory;
import com.learney.contentaudit.revisioninfrastructure.candidatejudgment.CandidateJudgmentEvaluatorFactory;
import com.learney.contentaudit.revisioninfrastructure.candidatejudgment.DefaultCandidateJudgmentEvaluatorFactory;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationBudget;
import com.learney.contentaudit.revisiondomain.contextoverride.DefaultCorrectionContextOverrideParser;
import com.learney.contentaudit.revisiondomain.engine.DefaultRevisionEngineFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.revisiondomain.engine.DefaultProposalDecisionServiceFactory;
import com.learney.contentaudit.revisiondomain.engine.DefaultRevisionValidatorFactory;
import com.learney.contentaudit.revisiondomain.engine.KnowledgeTitleProposalStrategyRegistryConfig;
import com.learney.contentaudit.revisiondomain.engine.LemmaAbsenceProposalStrategyRegistryConfig;
import com.learney.contentaudit.revisiondomain.lemmaabsence.CannedLemmaAbsenceQuizCandidateGenerator;
import com.learney.contentaudit.revisiondomain.lemmaabsence.LemmaAbsenceMvpStrategy;
import com.learney.contentaudit.revisiondomain.lemmaabsence.LemmaAbsenceQuizCandidateGenerator;
import com.learney.contentaudit.revisiondomain.knowledgetitle.KnowledgeTitleAgentStrategy;
import com.learney.contentaudit.revisiondomain.knowledgetitle.KnowledgeTitleCandidateGenerator;
import com.learney.contentaudit.revisiondomain.knowledgetitle.KnowledgeTitleGeneratorResponse;
import com.learney.contentaudit.revisioninfrastructure.lagen.LagenConfig;
import com.learney.contentaudit.revisioninfrastructure.lagen.LemmaAbsenceAgentGeneratorFactory;
import com.learney.contentaudit.revisioninfrastructure.lemmaabsenceagent.DefaultLemmaAbsenceAgentGeneratorFactory;
import com.learney.contentaudit.revisioninfrastructure.knowledgetitleagent.DefaultKnowledgeTitleAgentGeneratorFactory;
import com.learney.contentaudit.revisioninfrastructure.knowledgetitleagent.KnowledgeTitleAgentGeneratorFactory;
import com.learney.contentaudit.auditcli.LagenMode;
import com.learney.contentaudit.auditcli.bootstrap.DefaultLagenModeResolver;
import com.learney.contentaudit.auditcli.bootstrap.DefaultLagenConfigResolver;
import com.learney.contentaudit.auditcli.bootstrap.InvalidLagenModeException;
import com.learney.contentaudit.auditcli.bootstrap.InvalidLagenConfigException;
import com.learney.contentaudit.auditcli.bootstrap.MissingLagenProviderInputException;
import com.learney.contentaudit.auditcli.bootstrap.UnsupportedLagenProviderException;
import com.learney.contentaudit.revisiondomain.ApprovalMode;
import com.learney.contentaudit.revisiondomain.ProposalDecisionService;
import com.learney.contentaudit.revisiondomain.RevisionValidator;
import com.learney.contentaudit.auditcli.bootstrap.DefaultApprovalModeResolver;
import com.learney.contentaudit.auditcli.bootstrap.DefaultProposalStrategySelector;
import com.learney.contentaudit.auditcli.bootstrap.DefaultWorkdirResolver;
import com.learney.contentaudit.auditcli.bootstrap.InvalidApprovalModeException;
import com.learney.contentaudit.auditcli.bootstrap.InvalidProposalStrategyException;
import com.learney.contentaudit.auditcli.formatting.AnalyzerStatsTransformer;
import com.learney.contentaudit.auditcli.formatting.CocaBucketsDetailedFormatter;
import com.learney.contentaudit.auditcli.formatting.DefaultAnalyzerStatsTransformer;
import com.learney.contentaudit.auditcli.formatting.DefaultDrillDownResolver;
import com.learney.contentaudit.auditcli.formatting.DefaultFormatterRegistry;
import com.learney.contentaudit.auditcli.formatting.DetailedFormatter;
import com.learney.contentaudit.auditcli.formatting.DrillDownResolver;
import com.learney.contentaudit.auditcli.formatting.JsonReportFormatter;
import com.learney.contentaudit.auditcli.formatting.LemmaAbsenceDetailedFormatter;
import com.learney.contentaudit.auditcli.formatting.QuizInstructionDetailedFormatterBootstrap;
import com.learney.contentaudit.auditcli.formatting.RawReportFormatter;
import com.learney.contentaudit.auditcli.formatting.RawJsonReportFormatter;
import com.learney.contentaudit.auditcli.formatting.ReportFormatter;
import com.learney.contentaudit.auditcli.formatting.ReportViewModelTransformer;
import com.learney.contentaudit.auditcli.formatting.DefaultReportViewModelTransformer;
import com.learney.contentaudit.auditcli.formatting.TableReportFormatter;
import com.learney.contentaudit.auditcli.formatting.DefaultImpactPreviewFormatter;
import com.learney.contentaudit.auditcli.formatting.TextReportFormatter;
import com.learney.contentaudit.auditinfrastructure.FileSystemActiveAnalysisSelectionStore;
import com.learney.contentaudit.auditinfrastructure.FileSystemImpactPreviewStore;
import com.learney.contentaudit.auditdomain.ActiveAnalysisSelectionStore;
import com.learney.contentaudit.revisiondomain.ImpactPreviewStore;
import com.learney.contentaudit.revisiondomain.ConsolidatedViewBuilderConfig;
import com.learney.contentaudit.revisiondomain.consolidatedview.ConsolidatedViewBuilder;
import com.learney.contentaudit.revisiondomain.consolidatedview.NodeFieldDiffer;
import com.learney.contentaudit.revisiondomain.engine.DefaultConsolidatedViewBuilderFactory;
import com.learney.contentaudit.revisiondomain.fielddiff.DefaultNodeFieldDifferFactory;
import com.learney.contentaudit.auditcli.formatting.DefaultConsolidatedViewFormatter;
import com.learney.contentaudit.revisiondomain.PreservationFactory;
import com.learney.contentaudit.revisiondomain.preservation.PreservationRepair;
import com.learney.contentaudit.revisiondomain.preservationengine.DefaultPreservationFactory;
import com.learney.contentaudit.auditdomain.EvaluationAnalyzerFactory;
import com.learney.contentaudit.auditdomain.QuizInstructionConfig;
import com.learney.contentaudit.auditdomain.QuizInstructionVerdictReader;
import com.learney.contentaudit.auditdomain.quizinstructionengine.DefaultQuizInstructionAnalyzerFactory;
import com.learney.contentaudit.auditcli.bootstrap.QuizInstructionJudgeConfigResolverBootstrap;
import com.learney.contentaudit.auditcli.bootstrap.DefaultReevaluationQuizSetResolver;
import com.learney.contentaudit.agentruntimeinfrastructure.AgentDefinitionLocator;
import com.learney.contentaudit.agentruntimeinfrastructure.AgentGraphRunner;
import com.learney.contentaudit.agentruntimeinfrastructure.AgentGraphRunnerConfig;
import com.learney.contentaudit.agentruntimeinfrastructure.graphexecution.DefaultAgentDefinitionLocatorFactory;
import com.learney.contentaudit.agentruntimeinfrastructure.graphexecution.DefaultAgentGraphRunnerFactory;
import com.learney.contentaudit.agentruntimeinfrastructure.llmprovider.LlmProviderResolver;
import com.learney.contentaudit.agentruntimeinfrastructure.llmprovider.DefaultLlmProviderResolver;
import com.learney.contentaudit.quizinstructioninfrastructure.QuizInstructionJudgeConfig;
import com.learney.contentaudit.quizinstructioninfrastructure.QuizInstructionJudgeFactory;
import com.learney.contentaudit.quizinstructioninfrastructure.instructionjudge.DefaultQuizInstructionJudgeFactory;
import com.learney.contentaudit.quizinstructioninfrastructure.instructionverdict.JacksonQuizInstructionVerdictReader;
import com.learney.contentaudit.evaluationledgerdomain.Evaluator;
import com.learney.contentaudit.evaluationledgerdomain.ContentFingerprinter;
import com.learney.contentaudit.evaluationledgerdomain.contentfingerprint.Sha256ContentFingerprinter;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationLedger;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSessionFactory;
import com.learney.contentaudit.evaluationledgerdomain.evaluationsession.DefaultEvaluationSessionFactory;
import com.learney.contentaudit.evaluationledgerinfrastructure.FileSystemEvaluationLedger;
import com.learney.contentaudit.auditapplication.DefaultQuizInstructionConfig;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CLI entry point. Resolves the workdir, manually assembles all dependencies,
 * and runs the command via picocli.
 *
 * The --workdir flag is parsed manually before picocli runs, as it must be
 * resolved before any FileSystem*Store is constructed (R017, Q5 architectural decision).
 */
class Main {

    public static void main(String[] args) {
        // ----------------------------------------------------------------
        // Step 1: Parse --workdir from args manually, before picocli.
        // Strip it from the args array before passing to picocli.
        // ----------------------------------------------------------------
        String workdirFlagValue = null;
        List<String> remainingArgs = new ArrayList<>(Arrays.asList(args));
        for (int i = 0; i < remainingArgs.size(); i++) {
            String arg = remainingArgs.get(i);
            if ("--workdir".equals(arg)) {
                if (i + 1 < remainingArgs.size()) {
                    workdirFlagValue = remainingArgs.get(i + 1);
                    remainingArgs.remove(i + 1);
                    remainingArgs.remove(i);
                }
                break;
            } else if (arg.startsWith("--workdir=")) {
                workdirFlagValue = arg.substring("--workdir=".length());
                remainingArgs.remove(i);
                break;
            }
        }

        // ----------------------------------------------------------------
        // Step 2a: Resolve CONTENT_AUDIT_APPROVAL_MODE env var — fail fast if invalid (R005)
        // ----------------------------------------------------------------
        ApprovalMode approvalMode;
        try {
            String approvalModeEnv = System.getenv("CONTENT_AUDIT_APPROVAL_MODE");
            approvalMode = new DefaultApprovalModeResolver().resolve(approvalModeEnv);
        } catch (InvalidApprovalModeException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
            return;
        }

        // ----------------------------------------------------------------
        // Step 2b: Resolve CONTENT_AUDIT_LAGEN_MODE (F-LAGEN-R013).
        // LAGEN config validation (F-LAGEN-R014) is deferred to the LAPS wiring
        // block below so commands that don't exercise the LLM adapter
        // (delete plan, get task, audit run, etc.) don't require LAGEN env vars.
        // ----------------------------------------------------------------
        LagenMode lagenMode;
        try {
            String lagenModeEnv = System.getenv("CONTENT_AUDIT_LAGEN_MODE");
            lagenMode = new DefaultLagenModeResolver().resolve(lagenModeEnv);
        } catch (InvalidLagenModeException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
            return;
        }

        // ----------------------------------------------------------------
        // Step 2: Resolve workdir — fail fast if an explicit override is invalid (R017)
        // ----------------------------------------------------------------
        Path baseDir;
        try {
            DefaultWorkdirResolver workdirResolver = new DefaultWorkdirResolver();
            baseDir = workdirResolver.resolve(workdirFlagValue);
        } catch (com.learney.contentaudit.auditcli.bootstrap.InvalidWorkdirException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
            return; // unreachable, but makes the compiler happy
        }

        // ----------------------------------------------------------------
        // Step 3: Infrastructure layer
        // ----------------------------------------------------------------
        CourseValidator courseValidator = new CourseValidatorImpl();
        FileSystemCourseRepository courseRepository = new FileSystemCourseRepository(courseValidator);

        String projectRoot = System.getProperty("user.dir");
        NlpTokenizerConfig nlpConfig = new NlpTokenizerConfig(
                projectRoot + "/analysis/recursos-compartidos/spacy/sample_processor.py",
                projectRoot + "/../ittm/pipeline/src/main/resources/vocabulary/lemmas_20k_words.txt",
                300
        );
        NlpTokenizer nlpTokenizer = new SpacyNlpTokenizerFactory().create(nlpConfig);

        // ----------------------------------------------------------------
        // Step 4: Application layer
        // ----------------------------------------------------------------
        SentenceLengthConfig sentenceLengthConfig = new DefaultSentenceLengthConfig();
        QuizSentenceConverter quizSentenceConverter = DefaultQuizSentenceConverter.create();
        CourseToAuditableMapper courseToAuditableMapper = new CourseToAuditableMapper(nlpTokenizer, quizSentenceConverter);

        SentenceLengthAnalyzer sentenceLengthAnalyzer = new SentenceLengthAnalyzer(nlpTokenizer, sentenceLengthConfig);
        KnowledgeTitleLengthAnalyzer knowledgeTitleLengthAnalyzer = new KnowledgeTitleLengthAnalyzer();
        KnowledgeInstructionsLengthAnalyzer knowledgeInstructionsLengthAnalyzer =
                new KnowledgeInstructionsLengthAnalyzer();

        CocaBucketsConfig cocaBucketsConfig = new DefaultCocaBucketsConfig();
        CocaBucketsAnalyzer cocaBucketsAnalyzer = new CocaBucketsAnalyzer(
                nlpTokenizer, cocaBucketsConfig,
                new DefaultTokenClassifier(),
                new DefaultProgressionEvaluator(),
                new DefaultImprovementPlanner()
        );

        LemmaRecurrenceConfig lemmaRecurrenceConfig = new DefaultLemmaRecurrenceConfig();
        LemmaRecurrenceAnalyzer lemmaRecurrenceAnalyzer = new LemmaRecurrenceAnalyzer(
                new DefaultContentWordFilter(), lemmaRecurrenceConfig,
                new DefaultIntervalCalculator(), new DefaultExposureClassifier()
        );

        LemmaAbsenceConfig lemmaAbsenceConfig = new DefaultLemmaAbsenceConfig();
        Path evpCatalogPath = Paths.get(projectRoot,
                "analysis/recursos-compartidos/enriched_vocabulary_catalog.json");
        FileSystemEvpCatalog evpCatalog = new FileSystemEvpCatalog(evpCatalogPath);
        // FEAT-CLEX: el emparejamiento lexico por oracion (F-LABS-R017-R020/R033-R036/R045)
        // vive en SentenceLexicalScorer, unica fuente de verdad compartida por el analyzer
        // (Grupo D) y por la consulta lexica del CLI (F-CLEX-R006).
        com.learney.contentaudit.auditdomain.labs.SentenceLexicalScorer sentenceLexicalScorer =
                new com.learney.contentaudit.auditdomain.labs.DefaultSentenceLexicalScorer(
                        evpCatalog, new DefaultContentWordFilter(), lemmaAbsenceConfig);
        LemmaByLevelAbsenceAnalyzer lemmaAbsenceAnalyzer = new LemmaByLevelAbsenceAnalyzer(
                evpCatalog, new DefaultContentWordFilter(), lemmaAbsenceConfig, sentenceLexicalScorer);

        DefaultLemmaCountConfig lemmaCountConfig = (DefaultLemmaCountConfig) new DefaultLemmaCountConfigLoader().load(null);
        LemmaCountAnalyzer lemmaCountAnalyzer = new LemmaCountAnalyzer(
                new DefaultContentWordFilter(),
                new EvpThenNlpLemmaCefrLevelResolver(evpCatalog),
                lemmaCountConfig);

        List<ContentAnalyzer> contentAnalyzers = List.of(
                sentenceLengthAnalyzer,
                knowledgeTitleLengthAnalyzer,
                knowledgeInstructionsLengthAnalyzer,
                cocaBucketsAnalyzer,
                lemmaRecurrenceAnalyzer,
                lemmaAbsenceAnalyzer,
                lemmaCountAnalyzer
        );

        ScoreAggregator scoreAggregator =
                new com.learney.contentaudit.auditdomain.labs.LemmaAbsenceScoreAggregator();
        IAuditEngine auditEngine = new IAuditEngine(contentAnalyzers, scoreAggregator);

        // ----------------------------------------------------------------
        // Step 4b: FEAT-QINST / FEAT-EVCOST wiring — quiz instruction compliance
        // analyzer, backed by the general reusable-evaluation-result infrastructure.
        //
        // Config resolution -> judge factory -> evaluator -> filesystem ledger ->
        // session factory -> analyzer factory. F-QINST-R007: an absent or invalid
        // judge configuration must never fail audit bootstrap -- someone without
        // CONTENT_AUDIT_QINST_JUDGE_* configured must still be able to run `analyze`
        // and see every other analyzer. DefaultQuizInstructionJudgeFactory already
        // hands back an "unavailable" evaluator in that case (never throws), so this
        // wiring preserves that guarantee end to end.
        //
        // QuizInstructionJudgeConfigResolverBootstrap is a hand-written (non-@Generated)
        // bridge, not the resolver itself: sentinel.yaml currently declares
        // QuizInstructionJudgeConfigResolver/DefaultQuizInstructionJudgeConfigResolver
        // package-private (visibility: internal), unlike every sibling resolver used
        // below (DefaultWorkdirResolver, DefaultApprovalModeResolver, etc.), all of which
        // are public and constructed directly here. Escalated to @architect to align the
        // visibility; see the bridge's Javadoc for detail.
        // ----------------------------------------------------------------
        // ARCH-LAGEN-R015: single source of the "what does this provider need" criterion
        // (F-QINST-R016 / F-LAGEN-R015) -- one instance, shared by the quiz-instruction
        // judge below and by every LAGEN config resolution (LAPS and quiz-instruction
        // correction), so the two consumers can never apply two divergent readings of
        // the same rule.
        LlmProviderResolver llmProviderResolver = new DefaultLlmProviderResolver();

        QuizInstructionJudgeConfig quizInstructionJudgeConfig =
                QuizInstructionJudgeConfigResolverBootstrap.resolve();

        // R017 / --workdir: this config feeds both the agent-definition locator and the
        // graph runner for the quiz-instruction-validator judge (analyze's own analyzer
        // AND F-QICOR-R009's revalidation reuse the very same Evaluator below). Neither
        // this judge's inputs (EvaluationSubject.getContent(), fixed at five keys per
        // F-QINST-R009's fingerprint-stability contract) nor the candidate-judgment
        // evaluator's inputs populate "project_root", so — unlike quiz-instruction-corrector
        // and knowledge-title-agent, which thread baseDir per-call via that input key —
        // these two must get agentsBaseDir/runsBaseDir explicitly from baseDir here,
        // or they silently fall back to the process CWD instead of --workdir.
        AgentGraphRunnerConfig quizInstructionAgentGraphRunnerConfig = new AgentGraphRunnerConfig(
                baseDir.resolve(".sentinel/agents"), baseDir.resolve(".sentinel/runs"));
        AgentGraphRunner quizInstructionAgentGraphRunner =
                new DefaultAgentGraphRunnerFactory().create(quizInstructionAgentGraphRunnerConfig);
        AgentDefinitionLocator quizInstructionAgentDefinitionLocator =
                new DefaultAgentDefinitionLocatorFactory().create(quizInstructionAgentGraphRunnerConfig);

        QuizInstructionJudgeFactory quizInstructionJudgeFactory =
                new DefaultQuizInstructionJudgeFactory(llmProviderResolver);
        Evaluator quizInstructionJudge = quizInstructionJudgeFactory.create(
                quizInstructionJudgeConfig, quizInstructionAgentGraphRunner,
                quizInstructionAgentDefinitionLocator);

        ContentFingerprinter evaluationContentFingerprinter = new Sha256ContentFingerprinter();
        EvaluationLedger evaluationLedger = new FileSystemEvaluationLedger(baseDir);
        EvaluationSessionFactory evaluationSessionFactory =
                new DefaultEvaluationSessionFactory(evaluationLedger, evaluationContentFingerprinter);

        QuizInstructionVerdictReader quizInstructionVerdictReader = new JacksonQuizInstructionVerdictReader();
        QuizInstructionConfig quizInstructionConfig = new DefaultQuizInstructionConfig();

        EvaluationAnalyzerFactory quizInstructionAnalyzerFactory = new DefaultQuizInstructionAnalyzerFactory(
                evaluationSessionFactory, quizInstructionJudge, quizInstructionVerdictReader, quizInstructionConfig);

        List<EvaluationAnalyzerFactory> evaluationAnalyzerFactories =
                List.of(quizInstructionAnalyzerFactory);

        DefaultAuditRunner auditRunner = new DefaultAuditRunner(
                courseRepository, courseToAuditableMapper, auditEngine,
                contentAnalyzers, scoreAggregator, evaluationAnalyzerFactories);

        // ----------------------------------------------------------------
        // Step 5: CLI formatting
        // ----------------------------------------------------------------
        DrillDownResolver drillDownResolver = new DefaultDrillDownResolver();
        Map<String, ReportFormatter> formatters = new HashMap<>();
        formatters.put("text", new TextReportFormatter(drillDownResolver));
        formatters.put("json", new JsonReportFormatter(drillDownResolver));
        formatters.put("table", new TableReportFormatter(drillDownResolver));

        DefaultFormatterRegistry formatterRegistry = new DefaultFormatterRegistry(formatters);
        ReportViewModelTransformer viewModelTransformer = new DefaultReportViewModelTransformer();
        RawReportFormatter rawReportFormatter = new RawJsonReportFormatter();

        List<SelfDescribingConfig> describableConfigs = List.of(
                (SelfDescribingConfig) sentenceLengthConfig,
                (SelfDescribingConfig) cocaBucketsConfig,
                (SelfDescribingConfig) lemmaRecurrenceConfig,
                (SelfDescribingConfig) lemmaAbsenceConfig,
                (SelfDescribingConfig) lemmaCountConfig);
        DefaultAnalyzerRegistry analyzerRegistry = new DefaultAnalyzerRegistry(
                contentAnalyzers, describableConfigs);
        AnalyzerStatsTransformer analyzerStatsTransformer = new DefaultAnalyzerStatsTransformer();

        Map<String, DetailedFormatter> detailedFormatters = new HashMap<>();
        detailedFormatters.put("lemma-absence", new LemmaAbsenceDetailedFormatter());
        detailedFormatters.put("coca-buckets-distribution", new CocaBucketsDetailedFormatter());
        // QuizInstructionDetailedFormatter is package-private (visibility escalated to
        // @architect, see its bootstrap's Javadoc); obtained via the same-package bridge.
        detailedFormatters.put("quiz-instruction", QuizInstructionDetailedFormatterBootstrap.create());

        // ----------------------------------------------------------------
        // Step 6: Persistence stores — all constructed with resolved baseDir
        // ----------------------------------------------------------------
        AuditReportStore auditReportStore = new FileSystemAuditReportStore(baseDir);
        RefinementPlanStore refinementPlanStore = new FileSystemRefinementPlanStore(baseDir);
        RevisionArtifactStore revisionArtifactStore = new FileSystemRevisionArtifactStore(baseDir);
        ImpactPreviewStore impactPreviewStore = new FileSystemImpactPreviewStore(baseDir);
        ActiveAnalysisSelectionStore activeAnalysisSelectionStore =
                new FileSystemActiveAnalysisSelectionStore(baseDir);

        // ----------------------------------------------------------------
        // Step 7: Refiner + Revision engines
        // ----------------------------------------------------------------
        RefinerEngine refinerEngine = new DefaultRefinerEngine();
        SentenceLengthContextResolver sentenceLengthContextResolver = new SentenceLengthContextResolver();
        LemmaAbsenceContextResolver lemmaAbsenceContextResolver = new LemmaAbsenceContextResolver();
        KnowledgeTitleContextResolver knowledgeTitleContextResolver = new KnowledgeTitleContextResolver();
        QuizInstructionContextResolver quizInstructionContextResolver = new QuizInstructionContextResolver();
        CorrectionContextResolver correctionContextResolver = new DispatchingCorrectionContextResolver(
                sentenceLengthContextResolver, lemmaAbsenceContextResolver, knowledgeTitleContextResolver,
                quizInstructionContextResolver);

        RevisionValidator revisionValidator = new DefaultRevisionValidatorFactory().create(approvalMode);

        // ----------------------------------------------------------------
        // Step 7b: LAPS strategy wiring (F-LAPS, F-LAGEN)
        // ----------------------------------------------------------------
        boolean lapsDisabled = "1".equals(System.getenv("CONTENT_AUDIT_LAPS_DISABLE"));

        LemmaAbsenceQuizCandidateGenerator generator;
        String providerId;
        KnowledgeTitleCandidateGenerator knowledgeTitleGenerator;
        String knowledgeTitleProviderId;

        if (lagenMode == LagenMode.CANNED) {
            // Canned mode: deterministic fixture candidate, no LLM contact (F-LAGEN-R012)
            generator = new CannedLemmaAbsenceQuizCandidateGenerator(
                    "She ____ [walks|runs] to school.",
                    "Ella camina a la escuela.");
            providerId = "canned:fixed";
            knowledgeTitleGenerator = ctx -> new KnowledgeTitleGeneratorResponse(
                    "Canned Knowledge Title",
                    "Completa la actividad.");
            knowledgeTitleProviderId = "canned:fixed";
        } else {
            // LLM mode: sentinel-agent backed by LangChain4j (F-LASAG-R001/R002).
            // F-LAGEN-R014: if config or providerId resolution fails, defer the failure
            // to the moment the operator actually runs `revise task` so other commands
            // (analyze, get, delete, plan, stats, ...) don't require LAGEN env vars.
            LagenConfig lagenConfig = null;
            String lagenSetupError = null;
            try {
                lagenConfig = new DefaultLagenConfigResolver(llmProviderResolver).resolve(System.getenv());
            } catch (InvalidLagenConfigException | MissingLagenProviderInputException
                    | UnsupportedLagenProviderException e) {
                // F-LAGEN-R015: the criterion now rejects with two dedicated types (missing
                // input / unsupported provider) alongside the pre-existing parse-failure
                // exception -- all three defer the same way (R014), so all three are caught here.
                lagenSetupError = e.getMessage();
            }

            if (lagenSetupError == null) {
                // Build a session factory that binds a fresh SuggestedLemmaQuerySession
                // with the AuditReport and RefinementTask on each generate() call.
                LemmaAbsenceContextSuggestedLemmaQueryPort agentLemmaQueryPort =
                        LemmaAbsenceContextSuggestedLemmaQueryPort.create(
                                lemmaAbsenceContextResolver, evpCatalog, lemmaCountConfig);
                com.learney.contentaudit.refinerdomain.lemmasuggestion.DefaultSuggestedLemmaQuerySessionFactory sessionFactory =
                        new com.learney.contentaudit.refinerdomain.lemmasuggestion.DefaultSuggestedLemmaQuerySessionFactory(
                                agentLemmaQueryPort, auditReportStore);

                LemmaAbsenceAgentGeneratorFactory factory = new DefaultLemmaAbsenceAgentGeneratorFactory();
                try {
                    providerId = factory.providerIdFor(lagenConfig);
                    generator = factory.create(lagenConfig, sessionFactory);

                    KnowledgeTitleAgentGeneratorFactory knowledgeTitleFactory =
                            new DefaultKnowledgeTitleAgentGeneratorFactory(baseDir);
                    knowledgeTitleProviderId = knowledgeTitleFactory.providerIdFor(lagenConfig);
                    knowledgeTitleGenerator = knowledgeTitleFactory.create(lagenConfig);
                } catch (com.learney.contentaudit.revisioninfrastructure.lagen.InvalidProviderIdException e) {
                    lagenSetupError = e.getMessage();
                    providerId = "lagen:misconfigured";
                    final String deferredMsg = lagenSetupError;
                    generator = ctx -> {
                        throw new ProposalStrategyFailedException(
                                "lemma-absence-llm", ctx.getTaskId(), "INVALID_CONFIG: " + deferredMsg);
                    };
                    knowledgeTitleProviderId = "lagen:misconfigured";
                    knowledgeTitleGenerator = ctx -> {
                        throw new ProposalStrategyFailedException(
                                "knowledge-title-agent", ctx.getTaskId(), "INVALID_CONFIG: " + deferredMsg);
                    };
                }
            } else {
                providerId = "lagen:misconfigured";
                final String deferredMsg = lagenSetupError;
                generator = ctx -> {
                    throw new ProposalStrategyFailedException(
                            "lemma-absence-llm", ctx.getTaskId(), "INVALID_CONFIG: " + deferredMsg);
                };
                knowledgeTitleProviderId = "lagen:misconfigured";
                knowledgeTitleGenerator = ctx -> {
                    throw new ProposalStrategyFailedException(
                            "knowledge-title-agent", ctx.getTaskId(), "INVALID_CONFIG: " + deferredMsg);
                };
            }
        }

        LemmaAbsenceMvpStrategy mvpStrategy = new LemmaAbsenceMvpStrategy(generator, providerId);
        KnowledgeTitleAgentStrategy knowledgeTitleStrategy =
                new KnowledgeTitleAgentStrategy(knowledgeTitleGenerator, knowledgeTitleProviderId);

        LemmaAbsenceProposalStrategyRegistryConfig registryConfig =
                new LemmaAbsenceProposalStrategyRegistryConfig(List.of(mvpStrategy), null);
        KnowledgeTitleProposalStrategyRegistryConfig knowledgeTitleRegistryConfig =
                new KnowledgeTitleProposalStrategyRegistryConfig(
                        List.of(knowledgeTitleStrategy), "knowledge-title-agent");

        LemmaAbsenceProposalStrategyRegistry strategyRegistry = null;
        KnowledgeTitleProposalStrategyRegistry knowledgeTitleStrategyRegistry = null;
        if (!lapsDisabled) {
            // Resolve active strategy name via env var (DOUBT-STRATEGY-SELECTION -> Option A)
            String lapsStrategyEnv = System.getenv("CONTENT_AUDIT_LAPS_STRATEGY");
            String activeStrategyName;
            try {
                // Build a temporary registry (activeName=null) just for validation by the selector
                LemmaAbsenceProposalStrategyRegistry tempRegistry =
                        new DefaultLemmaAbsenceProposalStrategyRegistry(registryConfig);
                activeStrategyName = new DefaultProposalStrategySelector().select(lapsStrategyEnv, tempRegistry);
            } catch (InvalidProposalStrategyException e) {
                System.err.println("Error: " + e.getMessage());
                System.exit(1);
                return;
            }

            // Build final registry with resolved active name
            LemmaAbsenceProposalStrategyRegistryConfig finalRegistryConfig =
                    new LemmaAbsenceProposalStrategyRegistryConfig(List.of(mvpStrategy), activeStrategyName);
            strategyRegistry = new DefaultLemmaAbsenceProposalStrategyRegistry(finalRegistryConfig);
            knowledgeTitleStrategyRegistry =
                    new DefaultKnowledgeTitleProposalStrategyRegistry(knowledgeTitleRegistryConfig);
        }

        // ----------------------------------------------------------------
        // Step 7c: FEAT-QICOR wiring — candidate criteria catalog, the same
        // quiz-instruction compliance checker the judge uses (R005/R009), the
        // correction candidate generator, and the two judgment criteria
        // (FAITHFUL_TRANSLATION, SOLVABLE_SAME_KIND).
        // ----------------------------------------------------------------
        com.learney.contentaudit.auditdomain.lexicalflags.SentenceLexicalEvaluator sentenceLexicalEvaluator =
                new com.learney.contentaudit.auditdomain.lexicalflags.DefaultSentenceLexicalEvaluator(
                        sentenceLexicalScorer);

        QuizInstructionSubjectViewFactory quizInstructionSubjectViewFactory =
                new DefaultQuizInstructionSubjectViewFactory();
        // R005/R009: same evaluationSessionFactory + quizInstructionJudge + verdictReader
        // already wired for the audit's own quiz-instruction analyzer (Step 4b above), so
        // the verdict this checker produces or reuses is exactly what the next audit would.
        QuizInstructionComplianceChecker quizInstructionComplianceChecker =
                new DefaultQuizInstructionComplianceCheckerFactory().create(
                        evaluationSessionFactory, quizInstructionJudge, quizInstructionVerdictReader,
                        new EvaluationBudget(quizInstructionConfig.getDefaultMaxNewEvaluations()));

        QuizInstructionCorrectionConfig quizInstructionCorrectionConfig =
                new EnvQuizInstructionCorrectionConfigResolver().resolve(System.getenv());

        QuizInstructionCandidateGenerator quizInstructionGenerator;
        String quizInstructionProviderId;
        List<CandidateCriterionEvaluator> judgmentEvaluators;

        if (lagenMode == LagenMode.CANNED) {
            quizInstructionGenerator = request -> new QuizInstructionGeneratorResponse(
                    request.getContext() != null ? request.getContext().getQuizSentence() : "",
                    request.getContext() != null ? request.getContext().getTranslation() : "",
                    "canned: sin cambios");
            quizInstructionProviderId = "canned:fixed";
            judgmentEvaluators = List.of(
                    cannedPassingJudgmentEvaluator(CorrectionCriterion.FAITHFUL_TRANSLATION),
                    cannedPassingJudgmentEvaluator(CorrectionCriterion.SOLVABLE_SAME_KIND));
        } else {
            // F-LAGEN-R014 precedent: config resolution failures defer to the moment the
            // correction actually runs, exactly like LAPS/knowledge-title above, so other
            // commands never require CONTENT_AUDIT_LAGEN_* to be set.
            LagenConfig quizInstructionLagenConfig = null;
            String quizInstructionLagenSetupError = null;
            try {
                quizInstructionLagenConfig =
                        new DefaultLagenConfigResolver(llmProviderResolver).resolve(System.getenv());
            } catch (InvalidLagenConfigException | MissingLagenProviderInputException
                    | UnsupportedLagenProviderException e) {
                quizInstructionLagenSetupError = e.getMessage();
            }

            if (quizInstructionLagenSetupError == null) {
                QuizInstructionAgentGeneratorFactory quizInstructionAgentGeneratorFactory =
                        new DefaultQuizInstructionAgentGeneratorFactory(baseDir);
                quizInstructionGenerator =
                        quizInstructionAgentGeneratorFactory.create(quizInstructionLagenConfig);
                quizInstructionProviderId =
                        quizInstructionAgentGeneratorFactory.providerIdFor(quizInstructionLagenConfig);

                CandidateJudgmentEvaluatorFactory candidateJudgmentEvaluatorFactory =
                        new DefaultCandidateJudgmentEvaluatorFactory(baseDir);
                judgmentEvaluators = List.of(
                        candidateJudgmentEvaluatorFactory.create(
                                quizInstructionLagenConfig, CorrectionCriterion.FAITHFUL_TRANSLATION),
                        candidateJudgmentEvaluatorFactory.create(
                                quizInstructionLagenConfig, CorrectionCriterion.SOLVABLE_SAME_KIND));
            } else {
                final String deferredMsg = quizInstructionLagenSetupError;
                quizInstructionGenerator = request -> {
                    throw new ProposalStrategyFailedException(
                            "quiz-instruction-agent",
                            request.getContext() != null ? request.getContext().getTaskId() : null,
                            "INVALID_CONFIG: " + deferredMsg);
                };
                quizInstructionProviderId = "lagen:misconfigured";
                // Without a resolvable LLM config, the two judgment criteria simply do not
                // enter the active catalog (CandidateCriteriaConfig.judgmentEvaluators doc);
                // the deterministic criteria and INSTRUCTION_COMPLIANCE still run.
                judgmentEvaluators = List.of();
            }
        }

        QuizInstructionAgentStrategy quizInstructionStrategy =
                new QuizInstructionAgentStrategy(quizInstructionGenerator, quizInstructionProviderId);
        QuizInstructionProposalStrategyRegistryConfig quizInstructionRegistryConfig =
                new QuizInstructionProposalStrategyRegistryConfig(
                        List.of(quizInstructionStrategy), "quiz-instruction-agent");
        QuizInstructionProposalStrategyRegistry quizInstructionStrategyRegistry =
                new DefaultQuizInstructionProposalStrategyRegistry(quizInstructionRegistryConfig);

        // DOUBT-DISTINCION-ALCANCE / DOUBT-PRESUPUESTO-CORRECCION: thresholds left null so
        // DefaultCandidateCriteriaFactory substitutes its own defaults.
        CandidateCriteriaConfig candidateCriteriaConfig = new CandidateCriteriaConfig(
                nlpTokenizer,
                sentenceLengthConfig,
                sentenceLexicalEvaluator,
                quizInstructionComplianceChecker,
                quizInstructionSubjectViewFactory,
                judgmentEvaluators,
                null,
                null,
                null,
                auditReportStore);
        CandidateAssessor candidateAssessor =
                new DefaultCandidateCriteriaFactory().create(candidateCriteriaConfig);

        LemmaAbsenceProposalDeriver proposalDeriver =
                new DefaultLemmaAbsenceProposalDeriver(quizSentenceConverter);
        KnowledgeTitleProposalDeriver knowledgeTitleProposalDeriver =
                new DefaultKnowledgeTitleProposalDeriver();

        RevisionEngineConfig revisionEngineConfig = new RevisionEngineConfig();
        revisionEngineConfig.setRevisers(new HashMap<DiagnosisKind, Reviser>());
        revisionEngineConfig.setArtifactStore(revisionArtifactStore);
        revisionEngineConfig.setCourseRepository((CourseRepository) courseRepository);
        revisionEngineConfig.setRefinementPlanStore(refinementPlanStore);
        revisionEngineConfig.setAuditReportStore(auditReportStore);
        revisionEngineConfig.setContextResolver(correctionContextResolver);
        revisionEngineConfig.setValidator(revisionValidator);
        revisionEngineConfig.setLemmaAbsenceStrategyRegistry(strategyRegistry);
        revisionEngineConfig.setLemmaAbsenceProposalDeriver(proposalDeriver);
        revisionEngineConfig.setKnowledgeTitleStrategyRegistry(knowledgeTitleStrategyRegistry);
        revisionEngineConfig.setKnowledgeTitleProposalDeriver(knowledgeTitleProposalDeriver);
        revisionEngineConfig.setCourseMapper(courseToAuditableMapper);
        revisionEngineConfig.setAuditEngine(auditEngine);
        revisionEngineConfig.setImpactPreviewStore(impactPreviewStore);
        revisionEngineConfig.setCorrectionContextOverrideParser(
                DefaultCorrectionContextOverrideParser.withDefaultValidators(new ObjectMapper()));
        revisionEngineConfig.setQuizInstructionStrategyRegistry(quizInstructionStrategyRegistry);
        revisionEngineConfig.setCandidateAssessor(candidateAssessor);
        revisionEngineConfig.setQuizInstructionComplianceChecker(quizInstructionComplianceChecker);
        revisionEngineConfig.setQuizInstructionSubjectViewFactory(quizInstructionSubjectViewFactory);
        revisionEngineConfig.setQuizInstructionCorrectionConfig(quizInstructionCorrectionConfig);
        RevisionEngine revisionEngine = new DefaultRevisionEngineFactory().create(revisionEngineConfig);

        // F-QICOR-R008 (journey J002): the bounded batch runner over QUIZ_INSTRUCTION
        // tasks, its filesystem report store, and its text formatter. Registered below as
        // the "revise-instructions" subcommand (Step 8) — its own verb, not a flag on
        // "revise": correcting consigna is always an explicit operator action.
        QuizInstructionCorrectionRunStore quizInstructionCorrectionRunStore =
                new FileSystemQuizInstructionCorrectionRunStore(baseDir);
        QuizInstructionCorrectionRunner quizInstructionCorrectionRunner =
                new DefaultQuizInstructionCorrectionRunnerFactory().create(
                        revisionEngineConfig, quizInstructionCorrectionRunStore,
                        quizInstructionCorrectionConfig);
        QuizInstructionCorrectionRunFormatter quizInstructionCorrectionRunFormatter =
                new DefaultQuizInstructionCorrectionRunFormatter();
        ReviseInstructionsCmd reviseInstructionsCmd = new ReviseInstructionsCmd(
                quizInstructionCorrectionRunner, refinementPlanStore,
                quizInstructionCorrectionRunFormatter);

        ProposalDecisionService proposalDecisionService;
        try {
            proposalDecisionService =
                    new DefaultProposalDecisionServiceFactory().create(revisionEngineConfig);
        } catch (UnsupportedOperationException e) {
            // DefaultProposalDecisionServiceFactory not yet implemented; approve/reject will not work
            proposalDecisionService = null;
        }

        // ----------------------------------------------------------------
        // Step 8: Build the new FLAT command tree (R020: no refiner/analyzer groups)
        // ----------------------------------------------------------------
        ContentAuditCmd rootCmd = new ContentAuditCmd();
        picocli.CommandLine cmd = new picocli.CommandLine(rootCmd);

        // analyze
        cmd.addSubcommand("analyze", new picocli.CommandLine(
                new AnalyzeCmd(auditRunner, formatterRegistry, viewModelTransformer,
                        rawReportFormatter, drillDownResolver, detailedFormatters,
                        auditReportStore, CoursePathResolver::resolve,
                        new DefaultReevaluationQuizSetResolver())));

        // plan
        com.learney.contentaudit.auditdomain.AuditNodeIndexFactory auditNodeIndexFactory =
                new com.learney.contentaudit.auditdomain.auditnodeindex.DefaultAuditNodeIndexFactory();
        CorrectionContextJsonMapper correctionContextJsonMapper = new DefaultCorrectionContextJsonMapper();
        EphemeralPlanRenderer ephemeralPlanRenderer = new DefaultEphemeralPlanRenderer(
                auditNodeIndexFactory, correctionContextResolver, correctionContextJsonMapper);
        cmd.addSubcommand("plan", new picocli.CommandLine(
                new PlanCmd(auditReportStore, refinerEngine, refinementPlanStore, ephemeralPlanRenderer)));

        // revise
        ReviseCmd reviseCmd = new ReviseCmd(revisionEngine, refinementPlanStore);
        cmd.addSubcommand("revise", new picocli.CommandLine(reviseCmd));

        // revise-instructions — F-QICOR-R008 (journey J002): bounded batch of
        // QUIZ_INSTRUCTION corrections over a plan. Its own verb, always explicit —
        // never triggered by "analyze" or by a plain "revise task <id>".
        cmd.addSubcommand("revise-instructions", new picocli.CommandLine(reviseInstructionsCmd));

        // approve / reject — only available when ProposalDecisionService could be constructed
        if (proposalDecisionService != null) {
            ApproveCmd approveCmd = new ApproveCmd(proposalDecisionService);
            cmd.addSubcommand("approve", new picocli.CommandLine(approveCmd));

            RejectCmd rejectCmd = new RejectCmd(proposalDecisionService);
            cmd.addSubcommand("reject", new picocli.CommandLine(rejectCmd));
        }

        // config
        ConfigAnalyzerCmd configAnalyzerCmd = new ConfigAnalyzerCmd(analyzerRegistry);
        cmd.addSubcommand("config", new picocli.CommandLine(configAnalyzerCmd));

        // stats
        StatsAnalyzerCmd statsAnalyzerCmd = new StatsAnalyzerCmd(
                analyzerRegistry, analyzerStatsTransformer, auditRunner);
        cmd.addSubcommand("stats", new picocli.CommandLine(statsAnalyzerCmd));

        // get — inject baseDir for plan listing; revisionArtifactStore para proposals;
        // impactPreviewStore + formatter para el preview de impacto (F-PIPRE-R007);
        // suggestedLemmaQueryPort para F-CSLATDC-R001/R002
        LemmaAbsenceContextSuggestedLemmaQueryPort suggestedLemmaQueryPort =
                LemmaAbsenceContextSuggestedLemmaQueryPort.create(
                        lemmaAbsenceContextResolver, evpCatalog, lemmaCountConfig);
        GetCmd getCmd = new GetCmd(auditReportStore, refinementPlanStore, analyzerRegistry,
                correctionContextResolver, impactPreviewStore, new DefaultImpactPreviewFormatter(),
                correctionContextJsonMapper, suggestedLemmaQueryPort);
        getCmd.setBaseDir(baseDir);
        getCmd.setRevisionArtifactStore(revisionArtifactStore);
        cmd.addSubcommand("get", new picocli.CommandLine(getCmd));

        // delete — inject baseDir for filesystem-level removal
        DeleteCmd deleteCmd = new DeleteCmd(auditReportStore, refinementPlanStore);
        deleteCmd.setBaseDir(baseDir);
        cmd.addSubcommand("delete", new picocli.CommandLine(deleteCmd));

        // prune — inject baseDir for filesystem-level removal
        PruneCmd pruneCmd = new PruneCmd(auditReportStore, refinementPlanStore);
        pruneCmd.setBaseDir(baseDir);
        cmd.addSubcommand("prune", new picocli.CommandLine(pruneCmd));

        // set-active — write/clear the active (auditId, planId) pair (F-CDIFF-R001, R002)
        DefaultSetActiveAnalysisCommand setActiveCmd = new DefaultSetActiveAnalysisCommand(activeAnalysisSelectionStore);
        cmd.addSubcommand("set-active", new picocli.CommandLine(new SetActiveCmd(setActiveCmd)));

        // tokens — count NLP tokens of a quizSentence DSL using the SAME spaCy pipeline as the
        // audit (parse -> toPlainSentences -> NlpTokenizer.countTokens). Reuses the already-wired
        // converter + tokenizer so the lemma-absence-agent length eval measures candidates exactly
        // as the audit does. Hand-wired helper (not a governed feature) — see TokensCmd Javadoc.
        cmd.addSubcommand("tokens", new picocli.CommandLine(
                new TokensCmd(quizSentenceConverter, nlpTokenizer, sentenceLengthConfig)));

        // lexis — F-CLEX: consulta lexica de solo lectura oracion+nivel -> palabras
        // flaggeadas (mal ubicadas / fuera de catalogo), reutilizando el mismo pipeline
        // lexico del audit via SentenceLexicalEvaluator (que delega en sentenceLexicalScorer,
        // el mismo motor que ya usa lemmaAbsenceAnalyzer). sentenceLexicalEvaluator was
        // already built in Step 7c above for FEAT-QICOR's LevelVocabularyCriterion; reused
        // here rather than built twice.
        cmd.addSubcommand("lexis", new picocli.CommandLine(
                new LexisCmd(quizSentenceConverter, nlpTokenizer, sentenceLexicalEvaluator)));

        // get-consolidated — build and format the consolidated view (F-CDIFF-R001, R013 detail 3)
        NodeFieldDiffer nodeFieldDiffer = new DefaultNodeFieldDifferFactory().create();
        ConsolidatedViewBuilderConfig builderConfig = new ConsolidatedViewBuilderConfig(
                activeAnalysisSelectionStore,
                auditReportStore,
                refinementPlanStore,
                revisionArtifactStore,
                (com.learney.contentaudit.coursedomain.CourseRepository) courseRepository,
                null, // factory creates DefaultCourseElementLocator internally (package-private)
                courseToAuditableMapper,
                auditEngine,
                nodeFieldDiffer);
        ConsolidatedViewBuilder consolidatedViewBuilder =
                new DefaultConsolidatedViewBuilderFactory().create(builderConfig);
        DefaultGetConsolidatedCommand getConsolidatedCmd = new DefaultGetConsolidatedCommand(
                consolidatedViewBuilder, new DefaultConsolidatedViewFormatter());
        cmd.addSubcommand("get-consolidated", new picocli.CommandLine(new GetConsolidatedCmd(getConsolidatedCmd)));

        // repair — F-RPRES-R005: restores attributes degraded by past revisions, using the
        // same CorrectionScope the preservation guard applies going forward, so both
        // directions stay the same rule (architect decision, decisions.md 2026-07-29).
        PreservationFactory preservationFactory = new DefaultPreservationFactory();
        PreservationRepair preservationRepair = preservationFactory.createRepair(revisionArtifactStore);
        cmd.addSubcommand("repair", new picocli.CommandLine(
                new RepairCmd(preservationRepair, (CourseRepository) courseRepository)));

        // ----------------------------------------------------------------
        // Step 9: Execute
        // ----------------------------------------------------------------
        int exitCode = cmd.execute(remainingArgs.toArray(new String[0]));
        System.exit(exitCode);
    }

    /**
     * FEAT-QICOR canned-mode judgment evaluator: always PASSED, mirroring the canned
     * candidate generator/knowledge-title generator above (LagenMode.CANNED — deterministic,
     * no LLM contact). CandidateCriterionEvaluator has two abstract methods, so it cannot be
     * a lambda.
     */
    private static CandidateCriterionEvaluator cannedPassingJudgmentEvaluator(
            CorrectionCriterion criterion) {
        return new CandidateCriterionEvaluator() {
            @Override
            public CorrectionCriterion criterion() {
                return criterion;
            }

            @Override
            public CriterionVerdict evaluate(
                    com.learney.contentaudit.revisiondomain.candidatecriteria.CandidateAssessmentInput input) {
                return new CriterionVerdict(criterion, CriterionOutcome.PASSED,
                        "canned: siempre aprueba");
            }
        };
    }
}
