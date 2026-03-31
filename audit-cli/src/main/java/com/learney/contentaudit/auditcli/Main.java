package com.learney.contentaudit.auditcli;

import com.learney.contentaudit.auditapplication.CourseToAuditableMapper;
import com.learney.contentaudit.auditapplication.DefaultAuditRunner;
import com.learney.contentaudit.auditapplication.DefaultSentenceLengthConfig;
import com.learney.contentaudit.auditdomain.ContentAnalyzer;
import com.learney.contentaudit.auditdomain.IAuditEngine;
import com.learney.contentaudit.auditdomain.IContentAudit;
import com.learney.contentaudit.auditdomain.IScoreAggregator;
import com.learney.contentaudit.auditdomain.KnowledgeInstructionsLengthAnalyzer;
import com.learney.contentaudit.auditdomain.KnowledgeTitleLengthAnalyzer;
import com.learney.contentaudit.auditdomain.NlpTokenizer;
import com.learney.contentaudit.auditdomain.ScoreAggregator;
import com.learney.contentaudit.auditdomain.SentenceLengthAnalyzer;
import com.learney.contentaudit.auditdomain.SentenceLengthConfig;
import com.learney.contentaudit.auditdomain.CocaBucketsConfig;
import com.learney.contentaudit.auditdomain.coca.CocaBucketsAnalyzer;
import com.learney.contentaudit.auditdomain.coca.DefaultTokenClassifier;
import com.learney.contentaudit.auditdomain.coca.DefaultProgressionEvaluator;
import com.learney.contentaudit.auditdomain.coca.DefaultImprovementPlanner;
import com.learney.contentaudit.auditapplication.DefaultCocaBucketsConfig;
import com.learney.contentaudit.auditapplication.DefaultLemmaRecurrenceConfig;
import com.learney.contentaudit.auditapplication.DefaultLemmaAbsenceConfig;
import com.learney.contentaudit.auditdomain.LemmaRecurrenceConfig;
import com.learney.contentaudit.auditdomain.LemmaAbsenceConfig;
import com.learney.contentaudit.auditdomain.lrec.LemmaRecurrenceAnalyzer;
import com.learney.contentaudit.auditdomain.lrec.DefaultContentWordFilter;
import com.learney.contentaudit.auditdomain.lrec.DefaultIntervalCalculator;
import com.learney.contentaudit.auditdomain.lrec.DefaultExposureClassifier;
import com.learney.contentaudit.auditdomain.labs.LemmaByLevelAbsenceAnalyzer;
import com.learney.contentaudit.vocabularyinfrastructure.evp.FileSystemEvpCatalog;
import com.learney.contentaudit.nlpinfrastructure.NlpTokenizerConfig;
import com.learney.contentaudit.nlpinfrastructure.spacy.SpacyNlpTokenizerFactory;
import com.learney.contentaudit.coursedomain.CourseValidator;
import com.learney.contentaudit.courseinfrastructure.CourseValidatorImpl;
import com.learney.contentaudit.auditapplication.DefaultAnalyzerRegistry;
import com.learney.contentaudit.auditdomain.SelfDescribingConfig;
import com.learney.contentaudit.courseinfrastructure.FileSystemCourseRepository;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CLI entry point. Manually assembles all dependencies and runs the audit.
 */
public class Main {

    public static void main(String[] args) {
        // Infrastructure layer
        CourseValidator courseValidator = new CourseValidatorImpl();
        FileSystemCourseRepository courseRepository = new FileSystemCourseRepository(courseValidator);

        // NLP tokenizer via SpaCy factory
        // Resolve paths relative to project root
        String projectRoot = System.getProperty("user.dir");
        NlpTokenizerConfig nlpConfig = new NlpTokenizerConfig(
                projectRoot + "/analysis/recursos-compartidos/spacy/sample_processor.py",
                projectRoot + "/../ittm/pipeline/src/main/resources/vocabulary/lemmas_20k_words.txt",
                300  // timeout 5 minutes
        );
        NlpTokenizer nlpTokenizer = new SpacyNlpTokenizerFactory().create(nlpConfig);

        // Application layer
        SentenceLengthConfig sentenceLengthConfig = new DefaultSentenceLengthConfig();
        CourseToAuditableMapper courseToAuditableMapper = new CourseToAuditableMapper(nlpTokenizer);

        // Domain: analyzers
        SentenceLengthAnalyzer sentenceLengthAnalyzer = new SentenceLengthAnalyzer(sentenceLengthConfig);
        KnowledgeTitleLengthAnalyzer knowledgeTitleLengthAnalyzer = new KnowledgeTitleLengthAnalyzer();
        KnowledgeInstructionsLengthAnalyzer knowledgeInstructionsLengthAnalyzer =
                new KnowledgeInstructionsLengthAnalyzer();

        // COCA Buckets Distribution analyzer
        CocaBucketsConfig cocaBucketsConfig = new DefaultCocaBucketsConfig();
        CocaBucketsAnalyzer cocaBucketsAnalyzer = new CocaBucketsAnalyzer(
                nlpTokenizer,
                cocaBucketsConfig,
                new DefaultTokenClassifier(),
                new DefaultProgressionEvaluator(),
                new DefaultImprovementPlanner()
        );

        // Lemma Recurrence analyzer
        LemmaRecurrenceConfig lemmaRecurrenceConfig = new DefaultLemmaRecurrenceConfig();
        LemmaRecurrenceAnalyzer lemmaRecurrenceAnalyzer = new LemmaRecurrenceAnalyzer(
                new DefaultContentWordFilter(),
                lemmaRecurrenceConfig,
                new DefaultIntervalCalculator(),
                new DefaultExposureClassifier()
        );

        // Lemma Absence analyzer
        LemmaAbsenceConfig lemmaAbsenceConfig = new DefaultLemmaAbsenceConfig();
        Path evpCatalogPath = Paths.get(projectRoot, "analysis/recursos-compartidos/enriched_vocabulary_catalog.json");
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

        // Domain: aggregator and engine
        ScoreAggregator scoreAggregator = new IScoreAggregator();
        IAuditEngine auditEngine = new IAuditEngine(contentAnalyzers, scoreAggregator);
        IContentAudit contentAudit = new IContentAudit(auditEngine);

        // Application: runner (with analyzer list for filtering support)
        DefaultAuditRunner auditRunner = new DefaultAuditRunner(
                courseRepository, courseToAuditableMapper, contentAudit,
                contentAnalyzers, scoreAggregator);

        // CLI layer: formatters, transformer, and registry
        DrillDownResolver drillDownResolver = new DefaultDrillDownResolver();
        Map<String, ReportFormatter> formatters = new HashMap<>();
        formatters.put("text", new TextReportFormatter(drillDownResolver));
        formatters.put("json", new JsonReportFormatter(drillDownResolver));
        formatters.put("table", new TableReportFormatter(drillDownResolver));

        DefaultFormatterRegistry formatterRegistry = new DefaultFormatterRegistry(formatters);
        ReportViewModelTransformer viewModelTransformer = new DefaultReportViewModelTransformer();
        RawReportFormatter rawReportFormatter = new RawJsonReportFormatter();

        // Analyzer introspection
        List<SelfDescribingConfig> describableConfigs = List.of(
                (SelfDescribingConfig) sentenceLengthConfig,
                (SelfDescribingConfig) cocaBucketsConfig,
                (SelfDescribingConfig) lemmaRecurrenceConfig,
                (SelfDescribingConfig) lemmaAbsenceConfig);
        DefaultAnalyzerRegistry analyzerRegistry = new DefaultAnalyzerRegistry(
                contentAnalyzers, describableConfigs);
        AnalyzerStatsTransformer analyzerStatsTransformer = new DefaultAnalyzerStatsTransformer();

        // Wire picocli command tree
        ContentAuditCmd rootCmd = new ContentAuditCmd();
        picocli.CommandLine cmd = new picocli.CommandLine(rootCmd);

        cmd.addSubcommand("analyze", new picocli.CommandLine(
                new AnalyzeCmd(auditRunner, formatterRegistry, viewModelTransformer, rawReportFormatter)));

        picocli.CommandLine analyzerGroup = new picocli.CommandLine(new AnalyzerCmd());
        analyzerGroup.addSubcommand("list", new picocli.CommandLine(
                new AnalyzerListCmd(analyzerRegistry)));
        analyzerGroup.addSubcommand("config", new picocli.CommandLine(
                new AnalyzerConfigCmd(analyzerRegistry)));
        analyzerGroup.addSubcommand("stats", new picocli.CommandLine(
                new AnalyzerStatsCmd(analyzerRegistry, analyzerStatsTransformer, auditRunner)));
        cmd.addSubcommand("analyzer", analyzerGroup);

        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }
}
