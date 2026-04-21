package com.learney.contentaudit.auditcli.commands;
import javax.annotation.processing.Generated;

import com.learney.contentaudit.auditapplication.CourseToAuditableMapper;
import com.learney.contentaudit.auditapplication.DefaultAuditRunner;
import com.learney.contentaudit.auditapplication.DefaultSentenceLengthConfig;
import com.learney.contentaudit.auditapplication.DefaultAnalyzerRegistry;
import com.learney.contentaudit.auditapplication.DefaultCocaBucketsConfig;
import com.learney.contentaudit.auditapplication.DefaultLemmaRecurrenceConfig;
import com.learney.contentaudit.auditapplication.DefaultLemmaAbsenceConfig;
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
import com.learney.contentaudit.vocabularyinfrastructure.evp.FileSystemEvpCatalog;
import com.learney.contentaudit.nlpinfrastructure.NlpTokenizerConfig;
import com.learney.contentaudit.nlpinfrastructure.spacy.SpacyNlpTokenizerFactory;
import com.learney.contentaudit.coursedomain.CourseValidator;
import com.learney.contentaudit.coursedomain.CourseRepository;
import com.learney.contentaudit.courseinfrastructure.CourseValidatorImpl;
import com.learney.contentaudit.courseinfrastructure.FileSystemCourseRepository;
import com.learney.contentaudit.auditdomain.AuditReportStore;
import com.learney.contentaudit.auditinfrastructure.FileSystemAuditReportStore;
import com.learney.contentaudit.auditinfrastructure.FileSystemRefinementPlanStore;
import com.learney.contentaudit.auditinfrastructure.FileSystemRevisionArtifactStore;
import com.learney.contentaudit.refinerdomain.CorrectionContextResolver;
import com.learney.contentaudit.refinerdomain.DispatchingCorrectionContextResolver;
import com.learney.contentaudit.refinerdomain.LemmaAbsenceContextResolver;
import com.learney.contentaudit.refinerdomain.SentenceLengthContextResolver;
import com.learney.contentaudit.refinerdomain.DefaultRefinerEngine;
import com.learney.contentaudit.refinerdomain.RefinerEngine;
import com.learney.contentaudit.refinerdomain.RefinementPlanStore;
import com.learney.contentaudit.refinerdomain.DiagnosisKind;
import com.learney.contentaudit.revisiondomain.RevisionEngine;
import com.learney.contentaudit.revisiondomain.RevisionEngineConfig;
import com.learney.contentaudit.revisiondomain.Reviser;
import com.learney.contentaudit.revisiondomain.RevisionArtifactStore;
import com.learney.contentaudit.revisiondomain.engine.DefaultRevisionEngineFactory;
import com.learney.contentaudit.revisiondomain.engine.DefaultProposalDecisionServiceFactory;
import com.learney.contentaudit.revisiondomain.engine.DefaultRevisionValidatorFactory;
import com.learney.contentaudit.revisiondomain.ApprovalMode;
import com.learney.contentaudit.revisiondomain.ProposalDecisionService;
import com.learney.contentaudit.revisiondomain.RevisionValidator;
import com.learney.contentaudit.auditcli.bootstrap.DefaultApprovalModeResolver;
import com.learney.contentaudit.auditcli.bootstrap.DefaultWorkdirResolver;
import com.learney.contentaudit.auditcli.bootstrap.InvalidApprovalModeException;
import com.learney.contentaudit.auditcli.formatting.AnalyzerStatsTransformer;
import com.learney.contentaudit.auditcli.formatting.CocaBucketsDetailedFormatter;
import com.learney.contentaudit.auditcli.formatting.DefaultAnalyzerStatsTransformer;
import com.learney.contentaudit.auditcli.formatting.DefaultDrillDownResolver;
import com.learney.contentaudit.auditcli.formatting.DefaultFormatterRegistry;
import com.learney.contentaudit.auditcli.formatting.DetailedFormatter;
import com.learney.contentaudit.auditcli.formatting.DrillDownResolver;
import com.learney.contentaudit.auditcli.formatting.JsonReportFormatter;
import com.learney.contentaudit.auditcli.formatting.LemmaAbsenceDetailedFormatter;
import com.learney.contentaudit.auditcli.formatting.RawReportFormatter;
import com.learney.contentaudit.auditcli.formatting.RawJsonReportFormatter;
import com.learney.contentaudit.auditcli.formatting.ReportFormatter;
import com.learney.contentaudit.auditcli.formatting.ReportViewModelTransformer;
import com.learney.contentaudit.auditcli.formatting.DefaultReportViewModelTransformer;
import com.learney.contentaudit.auditcli.formatting.TableReportFormatter;
import com.learney.contentaudit.auditcli.formatting.TextReportFormatter;
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
        CourseToAuditableMapper courseToAuditableMapper = new CourseToAuditableMapper(nlpTokenizer);

        SentenceLengthAnalyzer sentenceLengthAnalyzer = new SentenceLengthAnalyzer(sentenceLengthConfig);
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
        LemmaByLevelAbsenceAnalyzer lemmaAbsenceAnalyzer = new LemmaByLevelAbsenceAnalyzer(
                evpCatalog, new DefaultContentWordFilter(), lemmaAbsenceConfig);

        List<ContentAnalyzer> contentAnalyzers = List.of(
                sentenceLengthAnalyzer,
                knowledgeTitleLengthAnalyzer,
                knowledgeInstructionsLengthAnalyzer,
                cocaBucketsAnalyzer,
                lemmaRecurrenceAnalyzer,
                lemmaAbsenceAnalyzer
        );

        ScoreAggregator scoreAggregator =
                new com.learney.contentaudit.auditdomain.labs.LemmaAbsenceScoreAggregator();
        IAuditEngine auditEngine = new IAuditEngine(contentAnalyzers, scoreAggregator);

        DefaultAuditRunner auditRunner = new DefaultAuditRunner(
                courseRepository, courseToAuditableMapper, auditEngine,
                contentAnalyzers, scoreAggregator);

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
                (SelfDescribingConfig) lemmaAbsenceConfig);
        DefaultAnalyzerRegistry analyzerRegistry = new DefaultAnalyzerRegistry(
                contentAnalyzers, describableConfigs);
        AnalyzerStatsTransformer analyzerStatsTransformer = new DefaultAnalyzerStatsTransformer();

        Map<String, DetailedFormatter> detailedFormatters = new HashMap<>();
        detailedFormatters.put("lemma-absence", new LemmaAbsenceDetailedFormatter());
        detailedFormatters.put("coca-buckets-distribution", new CocaBucketsDetailedFormatter());

        // ----------------------------------------------------------------
        // Step 6: Persistence stores — all constructed with resolved baseDir
        // ----------------------------------------------------------------
        AuditReportStore auditReportStore = new FileSystemAuditReportStore(baseDir);
        RefinementPlanStore refinementPlanStore = new FileSystemRefinementPlanStore(baseDir);
        RevisionArtifactStore revisionArtifactStore = new FileSystemRevisionArtifactStore(baseDir);

        // ----------------------------------------------------------------
        // Step 7: Refiner + Revision engines
        // ----------------------------------------------------------------
        RefinerEngine refinerEngine = new DefaultRefinerEngine();
        SentenceLengthContextResolver sentenceLengthContextResolver = new SentenceLengthContextResolver();
        LemmaAbsenceContextResolver lemmaAbsenceContextResolver = new LemmaAbsenceContextResolver();
        CorrectionContextResolver correctionContextResolver = new DispatchingCorrectionContextResolver(
                sentenceLengthContextResolver, lemmaAbsenceContextResolver);

        RevisionValidator revisionValidator = new DefaultRevisionValidatorFactory().create(approvalMode);

        RevisionEngineConfig revisionEngineConfig = new RevisionEngineConfig();
        revisionEngineConfig.setRevisers(new HashMap<DiagnosisKind, Reviser>());
        revisionEngineConfig.setArtifactStore(revisionArtifactStore);
        revisionEngineConfig.setCourseRepository((CourseRepository) courseRepository);
        revisionEngineConfig.setRefinementPlanStore(refinementPlanStore);
        revisionEngineConfig.setAuditReportStore(auditReportStore);
        revisionEngineConfig.setContextResolver(correctionContextResolver);
        revisionEngineConfig.setValidator(revisionValidator);
        RevisionEngine revisionEngine = new DefaultRevisionEngineFactory().create(revisionEngineConfig);

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
                        auditReportStore)));

        // plan
        cmd.addSubcommand("plan", new picocli.CommandLine(
                new PlanCmd(auditReportStore, refinerEngine, refinementPlanStore)));

        // revise
        ReviseCmd reviseCmd = new ReviseCmd(revisionEngine, refinementPlanStore);
        cmd.addSubcommand("revise", new picocli.CommandLine(reviseCmd));

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

        // get — inject baseDir for plan listing and path-based operations; revisionArtifactStore for proposals
        GetCmd getCmd = new GetCmd(auditReportStore, refinementPlanStore, analyzerRegistry,
                correctionContextResolver);
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

        // ----------------------------------------------------------------
        // Step 9: Execute
        // ----------------------------------------------------------------
        int exitCode = cmd.execute(remainingArgs.toArray(new String[0]));
        System.exit(exitCode);
    }
}
