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
import com.learney.contentaudit.auditdomain.AnalyzerCatalog;
import com.learney.contentaudit.auditdomain.AnalyzerDescriptor;
import com.learney.contentaudit.auditdomain.AnalyzerProvider;
import com.learney.contentaudit.auditdomain.IAuditEngine;
import com.learney.contentaudit.auditdomain.KnowledgeInstructionsLengthAnalyzerProvider;
import com.learney.contentaudit.auditdomain.KnowledgeTitleLengthAnalyzerProvider;
import com.learney.contentaudit.auditdomain.NlpTokenizer;
import com.learney.contentaudit.auditdomain.ScoreAggregator;
import com.learney.contentaudit.auditdomain.SentenceLengthAnalyzerProvider;
import com.learney.contentaudit.auditdomain.SentenceLengthConfig;
import com.learney.contentaudit.auditdomain.CocaBucketsConfig;
import com.learney.contentaudit.auditdomain.LemmaRecurrenceConfig;
import com.learney.contentaudit.auditdomain.LemmaAbsenceConfig;
import com.learney.contentaudit.auditdomain.SelfDescribingConfig;
import com.learney.contentaudit.auditdomain.coca.CocaBucketsAnalyzerProvider;
import com.learney.contentaudit.auditdomain.lrec.LemmaRecurrenceAnalyzerProvider;
import com.learney.contentaudit.auditdomain.lrec.DefaultContentWordFilter;
import com.learney.contentaudit.auditdomain.labs.LemmaAbsenceAnalyzerProvider;
import com.learney.contentaudit.auditdomain.lemmacount.LemmaCountAnalyzerProvider;
import com.learney.contentaudit.auditdomain.findingengine.DefaultAnalyzerCatalog;
import com.learney.contentaudit.auditdomain.findingengine.DefaultContextNumbersCalculator;
import com.learney.contentaudit.auditdomain.findingengine.DefaultFindingCollector;
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
import com.learney.contentaudit.revisiondomain.quizinstruction.QuizInstructionCandidateAssessor;
import com.learney.contentaudit.revisiondomain.quizinstruction.QuizInstructionCandidateAssessorConfig;
import com.learney.contentaudit.revisiondomain.engine.DefaultQuizInstructionCandidateAssessorFactory;
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
import com.learney.contentaudit.agentruntimeinfrastructure.llmprovider.DefaultLocalSessionProviderCatalog;
import com.learney.contentaudit.agentruntimeinfrastructure.llmprovider.LlmProviderResolver;
import com.learney.contentaudit.agentruntimeinfrastructure.llmprovider.DefaultLlmProviderResolver;
import com.learney.contentaudit.agentruntimeinfrastructure.llmprovider.LocalSessionProviderCatalog;
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
import com.learney.contentaudit.auditapplication.DefaultChoiceAlternativeConfig;
import com.learney.contentaudit.auditapplication.DefaultJudgeExamService;
import com.learney.contentaudit.auditapplication.DefaultJudgeRuntimeConfig;
import com.learney.contentaudit.auditapplication.DefaultMiniTheoryConfig;
import com.learney.contentaudit.auditapplication.DefaultSingleAnswerConfig;
import com.learney.contentaudit.auditapplication.DefaultTranslationSenseConfig;
import com.learney.contentaudit.auditapplication.JudgeExamService;
import com.learney.contentaudit.auditcli.bootstrap.DefaultAgentJudgeConfigResolver;
import com.learney.contentaudit.auditcli.formatting.LemmaRecurrenceDetailedFormatter;
import com.learney.contentaudit.auditdomain.AudioRecordingRegistry;
import com.learney.contentaudit.auditdomain.CorrectorMatcher;
import com.learney.contentaudit.auditdomain.FormEquivalenceTableSource;
import com.learney.contentaudit.auditdomain.IrregularFormsCatalog;
import com.learney.contentaudit.auditdomain.JudgeExamRegistry;
import com.learney.contentaudit.auditdomain.JudgeRuntimeConfig;
import com.learney.contentaudit.auditdomain.ProductionSentenceSource;
import com.learney.contentaudit.auditdomain.QuizStudentViewRenderer;
import com.learney.contentaudit.auditdomain.TopicStudentViewQuery;
import com.learney.contentaudit.auditdomain.answercompleteness.AnswerCompletenessAnalyzerProvider;
import com.learney.contentaudit.auditdomain.choicealternative.ChoiceAlternativeJudgeRunFactory;
import com.learney.contentaudit.auditdomain.choicealternativeengine.DefaultChoiceAlternativeJudgeRunFactory;
import com.learney.contentaudit.auditdomain.correctormatch.DefaultCorrectorMatcher;
import com.learney.contentaudit.auditdomain.correctormatch.GapContext;
import com.learney.contentaudit.auditdomain.datacoherence.AudioRecording;
import com.learney.contentaudit.auditdomain.datacoherence.DataCoherenceAnalyzerProvider;
import com.learney.contentaudit.auditdomain.exactduplicates.ExactDuplicatesAnalyzerProvider;
import com.learney.contentaudit.auditdomain.hintcoherence.HintCoherenceAnalyzerProvider;
import com.learney.contentaudit.auditdomain.judging.JudgeExamVerdictExporter;
import com.learney.contentaudit.auditdomain.judging.JudgeRuntime;
import com.learney.contentaudit.auditdomain.judgingengine.DefaultJudgeExamVerdictExporter;
import com.learney.contentaudit.auditdomain.judgingengine.DefaultJudgeRuntime;
import com.learney.contentaudit.auditdomain.minitheoryengine.MiniTheoryAnalyzerProvider;
import com.learney.contentaudit.auditdomain.multiplechoicevalidity.MultipleChoiceValidityAnalyzerProvider;
import com.learney.contentaudit.auditdomain.offlevelwords.DefaultTopicTeachingResolver;
import com.learney.contentaudit.auditdomain.offlevelwords.OffLevelWordsAnalyzerProvider;
import com.learney.contentaudit.auditdomain.offlevelwords.TopicTeachingResolver;
import com.learney.contentaudit.auditdomain.singleanswerengine.SingleAnswerAnalyzerProvider;
import com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer;
import com.learney.contentaudit.auditdomain.studentview.DefaultTopicStudentViewQuery;
import com.learney.contentaudit.auditdomain.translationsenseengine.TranslationSenseAnalyzerProvider;
import com.learney.contentaudit.auditinfrastructure.FileSystemJudgeExamRegistry;
import com.learney.contentaudit.auditinfrastructure.exercisecopies.FileSystemAudioRecordingRegistry;
import com.learney.contentaudit.auditinfrastructure.exercisecopies.FileSystemProductionSentenceSource;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationOutcome;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSubject;
import com.learney.contentaudit.evaluationledgerdomain.evaluationsession.ExamEvaluationSessionFactory;
import com.learney.contentaudit.quizinstructioninfrastructure.AgentJudgeConfig;
import com.learney.contentaudit.quizinstructioninfrastructure.AgentJudgeFactory;
import com.learney.contentaudit.quizinstructioninfrastructure.AgentJudgeSpec;
import com.learney.contentaudit.quizinstructioninfrastructure.JudgeProviderReadinessProbe;
import com.learney.contentaudit.quizinstructioninfrastructure.agentjudge.DefaultAgentJudgeFactory;
import com.learney.contentaudit.quizinstructioninfrastructure.agentjudge.DefaultJudgeProviderReadinessProbe;
import com.learney.contentaudit.quizinstructioninfrastructure.choicealternativeverdict.JacksonChoiceAlternativeVerdictReader;
import com.learney.contentaudit.quizinstructioninfrastructure.minitheoryverdict.JacksonMiniTheoryVerdictReader;
import com.learney.contentaudit.quizinstructioninfrastructure.singleanswerverdict.JacksonSingleAnswerVerdictReader;
import com.learney.contentaudit.quizinstructioninfrastructure.translationsenseverdict.JacksonTranslationSenseVerdictReader;
import com.learney.contentaudit.refinerdomain.topicwords.DefaultTopicWordLightsQuery;
import com.learney.contentaudit.refinerdomain.topicwords.TopicWordLightsQuery;
import com.learney.contentaudit.vocabularyinfrastructure.correctortable.FileSystemFormEquivalenceTableSource;
import com.learney.contentaudit.vocabularyinfrastructure.irregularforms.FileSystemIrregularFormsCatalog;
import com.learney.contentaudit.auditdomain.AnalyzerCorrection;
import com.learney.contentaudit.auditdomain.AnalyzerCorrectionCatalog;
import com.learney.contentaudit.auditdomain.RuleFix;
import com.learney.contentaudit.auditdomain.answercompleteness.AnswerCompletenessRuleFix;
import com.learney.contentaudit.auditdomain.datacoherence.DataCoherenceRuleFix;
import com.learney.contentaudit.auditdomain.finishedsentence.DefaultFinishedSentenceReader;
import com.learney.contentaudit.auditdomain.finishedsentence.FinishedSentenceReader;
import com.learney.contentaudit.auditdomain.grammar.GrammarAnalyzerProvider;
import com.learney.contentaudit.auditdomain.grammar.GrammarRuleFix;
import com.learney.contentaudit.auditdomain.hintcoherence.HintCoherenceRuleFix;
import com.learney.contentaudit.auditdomain.localtools.EmbeddingModel;
import com.learney.contentaudit.auditdomain.localtools.GrammarChecker;
import com.learney.contentaudit.auditdomain.localtools.LocalToolGate;
import com.learney.contentaudit.auditdomain.localtools.YesNoModel;
import com.learney.contentaudit.auditdomain.localtoolsengine.DefaultLocalToolGate;
import com.learney.contentaudit.auditdomain.multiplechoicevalidity.MultipleChoiceValidityRuleFix;
import com.learney.contentaudit.auditdomain.naturalness.NaturalnessAnalyzerProvider;
import com.learney.contentaudit.auditdomain.nearduplicates.NearDuplicatesAnalyzerProvider;
import com.learney.contentaudit.auditinfrastructure.FileSystemBlindSampleStore;
import com.learney.contentaudit.auditinfrastructure.FileSystemGoalDeclarationSource;
import com.learney.contentaudit.auditinfrastructure.FileSystemProjectionStore;
import com.learney.contentaudit.auditinfrastructure.FileSystemSealedGoalsStore;
import com.learney.contentaudit.auditinfrastructure.traystore.FileSystemTrayDecisionStore;
import com.learney.contentaudit.locallanguageinfrastructure.OllamaEndpoint;
import com.learney.contentaudit.locallanguageinfrastructure.OllamaModelFactory;
import com.learney.contentaudit.locallanguageinfrastructure.languagetool.LanguageToolGrammarChecker;
import com.learney.contentaudit.locallanguageinfrastructure.ollama.DefaultOllamaModelFactory;
import com.learney.contentaudit.revisiondomain.BlindSampleStore;
import com.learney.contentaudit.revisiondomain.GoalDeclarationSource;
import com.learney.contentaudit.revisiondomain.ProjectionStore;
import com.learney.contentaudit.revisiondomain.SealedGoalsStore;
import com.learney.contentaudit.revisiondomain.blindsample.BlindErrorRateService;
import com.learney.contentaudit.revisiondomain.blindsample.BlindSampleService;
import com.learney.contentaudit.revisiondomain.blindsample.SampleOutcomeSource;
import com.learney.contentaudit.revisiondomain.blindsampleengine.DefaultBlindErrorRateService;
import com.learney.contentaudit.revisiondomain.blindsampleengine.DefaultBlindSampleService;
import com.learney.contentaudit.revisiondomain.blindsampleengine.DefaultSampleOutcomeSource;
import com.learney.contentaudit.revisiondomain.blindsampleengine.DefaultSampleStatistics;
import com.learney.contentaudit.revisiondomain.blindsampleengine.SampleStatistics;
import com.learney.contentaudit.revisiondomain.comparison.BatchComparator;
import com.learney.contentaudit.revisiondomain.comparison.GoalBook;
import com.learney.contentaudit.revisiondomain.comparisonengine.DefaultBatchComparator;
import com.learney.contentaudit.revisiondomain.comparisonengine.DefaultGoalBook;
import com.learney.contentaudit.revisiondomain.comparisonengine.LeaveOneOutChangeAttributor;
import com.learney.contentaudit.revisiondomain.engine.DefaultCourseProjectorFactory;
import com.learney.contentaudit.revisiondomain.projection.CourseProjection;
import com.learney.contentaudit.revisiondomain.projection.CourseProjector;
import com.learney.contentaudit.revisiondomain.projection.CourseProjectorConfig;
import com.learney.contentaudit.revisiondomain.projection.ProjectionScope;
import com.learney.contentaudit.revisiondomain.tray.TrayDecisionStore;
import com.learney.contentaudit.auditdomain.BaselineStore;
import com.learney.contentaudit.auditdomain.answercompleteness.AnswerCompletenessCorrection;
import com.learney.contentaudit.auditdomain.correctionengine.DefaultAnalyzerCorrectionCatalog;
import com.learney.contentaudit.auditdomain.correctionengine.LemmaAbsenceCorrection;
import com.learney.contentaudit.auditdomain.correctionengine.QuizInstructionCorrection;
import com.learney.contentaudit.auditdomain.datacoherence.DataCoherenceCorrection;
import com.learney.contentaudit.auditdomain.exactduplicates.ExactDuplicatesCorrection;
import com.learney.contentaudit.auditdomain.grammar.GrammarCorrection;
import com.learney.contentaudit.auditdomain.hintcoherence.HintCoherenceCorrection;
import com.learney.contentaudit.auditdomain.minitheoryengine.MiniTheoryCorrection;
import com.learney.contentaudit.auditdomain.multiplechoicevalidity.MultipleChoiceValidityCorrection;
import com.learney.contentaudit.auditdomain.nearduplicates.NearDuplicatesCorrection;
import com.learney.contentaudit.auditdomain.offlevelwords.OffLevelWordsCorrection;
import com.learney.contentaudit.auditdomain.singleanswerengine.SingleAnswerCorrection;
import com.learney.contentaudit.auditdomain.translationsenseengine.TranslationSenseCorrection;
import com.learney.contentaudit.auditinfrastructure.FileSystemBaselineStore;
import com.learney.contentaudit.auditinfrastructure.batchstore.FileSystemBatchStore;
import com.learney.contentaudit.refinerdomain.errortask.DefaultErrorTaskBriefer;
import com.learney.contentaudit.refinerdomain.errortask.ErrorTaskBriefer;
import com.learney.contentaudit.revisiondomain.batch.BatchStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

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
        // A saved review needs no NLP process, model discovery or judge bootstrap.
        Path problemProject = Path.of(System.getProperty("contentaudit.project.dir", System.getProperty("user.dir")));
        Integer problemExit = ProblemCmd.execute(remainingArgs, baseDir, problemProject, System.out, System.err);
        if (problemExit != null) {
            if (problemExit != 0) System.exit(problemExit);
            return;
        }
        Integer miniExit = SavedMiniTheoryCorrectionCmd.execute(remainingArgs, baseDir, System.out, System.err);
        if (miniExit != null) {
            if (miniExit != 0) System.exit(miniExit);
            return;
        }
        Integer reviewExit = SavedBatchReviewCmd.execute(remainingArgs, baseDir, System.out, System.err);
        if (reviewExit != null) {
            if (reviewExit != 0) System.exit(reviewExit);
            return;
        }

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

        // FEAT-RONDA1 (F-RONDA1-R001, hook A-01): the one view of an exercise as the student sees
        // it. The new analyzers and judges, the digest of the report and get student-view read it.
        QuizStudentViewRenderer studentViewRenderer = new DefaultQuizStudentViewRenderer(quizSentenceConverter);

        CocaBucketsConfig cocaBucketsConfig = new DefaultCocaBucketsConfig();
        LemmaRecurrenceConfig lemmaRecurrenceConfig = new DefaultLemmaRecurrenceConfig();

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

        DefaultLemmaCountConfig lemmaCountConfig = (DefaultLemmaCountConfig) new DefaultLemmaCountConfigLoader().load(null);

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
        LocalSessionProviderCatalog localSessionProviderCatalog =
                new DefaultLocalSessionProviderCatalog();
        LlmProviderResolver llmProviderResolver =
                new DefaultLlmProviderResolver(localSessionProviderCatalog);

        QuizInstructionJudgeConfig quizInstructionJudgeConfig =
                QuizInstructionJudgeConfigResolverBootstrap.resolve(localSessionProviderCatalog);

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

        // FEAT-JEXAM (hook C-13, R011): the free check of the provider's session, the same for the
        // quiz-instruction judge and the four new ones. It asks the status command of the selected
        // provider (`codex login status` by default), which calls no model.
        JudgeProviderReadinessProbe judgeProviderReadinessProbe =
                new DefaultJudgeProviderReadinessProbe(localSessionProviderCatalog);

        QuizInstructionJudgeFactory quizInstructionJudgeFactory =
                new DefaultQuizInstructionJudgeFactory(llmProviderResolver, judgeProviderReadinessProbe);
        Evaluator quizInstructionJudge = quizInstructionJudgeFactory.create(
                quizInstructionJudgeConfig, quizInstructionAgentGraphRunner,
                quizInstructionAgentDefinitionLocator);

        ContentFingerprinter evaluationContentFingerprinter = new Sha256ContentFingerprinter();
        // The file ledger is also the timeline the judge runtime measures the rate with (C-12).
        FileSystemEvaluationLedger evaluationLedger = new FileSystemEvaluationLedger(baseDir);
        EvaluationSessionFactory evaluationSessionFactory =
                new DefaultEvaluationSessionFactory(evaluationLedger, evaluationContentFingerprinter);

        // ----------------------------------------------------------------
        // Step 4b': FEAT-RONDA1 -- the common runtime of the judges (hook C-12). It is the only
        // one that opens their sessions: the exam's brake and budget 0 without a request live
        // there (F-JEXAM-R003, R010).
        // ----------------------------------------------------------------
        EvaluationSessionFactory examSessionFactory =
                new ExamEvaluationSessionFactory(evaluationLedger, evaluationContentFingerprinter);
        JudgeExamRegistry judgeExamRegistry = new FileSystemJudgeExamRegistry(baseDir);
        JudgeRuntimeConfig judgeRuntimeConfig = new DefaultJudgeRuntimeConfig();
        JudgeRuntime judgeRuntime = new DefaultJudgeRuntime(evaluationSessionFactory, examSessionFactory,
                judgeExamRegistry, evaluationLedger, judgeRuntimeConfig);
        JudgeExamVerdictExporter judgeExamVerdictExporter = new DefaultJudgeExamVerdictExporter();

        QuizInstructionVerdictReader quizInstructionVerdictReader = new JacksonQuizInstructionVerdictReader();
        QuizInstructionConfig quizInstructionConfig = new DefaultQuizInstructionConfig(judgeRuntimeConfig);

        // Hook C-13 (FEAT-JEXAM): the quiz-instruction judge opens its session through the judges
        // runtime -- the exam's brake and budget 0 without a request (F-JEXAM-R003, R010); the
        // candidates of revise-instructions keep the course's session factory and its 500 below.
        // F-CALUM-R001: the judge reads the exercise as the student sees it, with the same render
        // as the other four judges.
        AnalyzerProvider quizInstructionProvider = new DefaultQuizInstructionAnalyzerFactory(
                quizInstructionJudge, quizInstructionVerdictReader, quizInstructionConfig, judgeRuntime,
                studentViewRenderer);

        // ----------------------------------------------------------------
        // Step 4b'': FEAT-RONDA1 -- what the analyzers of round 1 need (hooks A-06, C-14, C-16,
        // D-03). Everything is built here once; what reads a file or asks a factory for a judge
        // does it on first use, so a command that does not need it never pays for it and an
        // analyzer its package has not built yet breaks nothing.
        // ----------------------------------------------------------------
        Path sharedResources = Paths.get(projectRoot, "analysis/recursos-compartidos");
        IrregularFormsCatalog irregularForms =
                new FileSystemIrregularFormsCatalog(sharedResources.resolve("formas-irregulares.json"));
        FormEquivalenceTableSource formEquivalenceTableSource =
                new FileSystemFormEquivalenceTableSource(sharedResources.resolve("corrector/formas-equivalentes.json"));
        CorrectorMatcher correctorMatcher = lazyCorrectorMatcher(
                () -> new DefaultCorrectorMatcher(formEquivalenceTableSource.load()));

        // <db> is the folder of the course being analyzed (A-06): the default course's, since the
        // catalog is built before the command reads its arguments.
        Path courseDb = defaultCourseDb(projectRoot);
        Path recordingsPath = courseDb.resolve("grabaciones.json");
        // A registry that does not exist yet knows no key: get student-view then leaves the
        // recorded text empty instead of failing (F-RONDA1-R003 inv. 2). DATOS builds the file.
        AudioRecordingRegistry recordings = Files.exists(recordingsPath)
                ? new FileSystemAudioRecordingRegistry(recordingsPath)
                : emptyRecordingRegistry();
        ProductionSentenceSource productionSentences =
                new FileSystemProductionSentenceSource(courseDb.resolve("quiz-templates.json"));

        TopicTeachingResolver topicTeachingResolver = new DefaultTopicTeachingResolver();

        // Hook C-16: one configuration for the four new judges, CONTENT_AUDIT_JUDGE_* (codex-cli by
        // default); the quiz-instruction judge keeps CONTENT_AUDIT_QINST_JUDGE_*. Hook C-14: the
        // evaluators, with the runner and locator of the quiz-instruction judge.
        AgentJudgeConfig agentJudgeConfig =
                new DefaultAgentJudgeConfigResolver(localSessionProviderCatalog).resolve(System.getenv());
        AgentJudgeFactory agentJudgeFactory =
                new DefaultAgentJudgeFactory(llmProviderResolver, judgeProviderReadinessProbe);
        Evaluator fromEnglishJudge = lazyAgentJudge(agentJudgeFactory, agentJudgeConfig,
                new AgentJudgeSpec("translation-sense-from-english", "translationSenseVerdict",
                        "juez de traducción y sentido, desde el inglés"),
                quizInstructionAgentGraphRunner, quizInstructionAgentDefinitionLocator);
        Evaluator fromTranslationJudge = lazyAgentJudge(agentJudgeFactory, agentJudgeConfig,
                new AgentJudgeSpec("translation-sense-from-translation", "translationSenseVerdict",
                        "juez de traducción y sentido, desde la traducción"),
                quizInstructionAgentGraphRunner, quizInstructionAgentDefinitionLocator);
        Evaluator singleAnswerJudge = lazyAgentJudge(agentJudgeFactory, agentJudgeConfig,
                new AgentJudgeSpec("single-answer-judge", "singleAnswerVerdict", "juez de una sola respuesta"),
                quizInstructionAgentGraphRunner, quizInstructionAgentDefinitionLocator);
        Evaluator miniTheoryJudge = lazyAgentJudge(agentJudgeFactory, agentJudgeConfig,
                new AgentJudgeSpec("mini-theory-judge", "miniTheoryVerdict", "juez de la miniteoría"),
                quizInstructionAgentGraphRunner, quizInstructionAgentDefinitionLocator);
        Evaluator choiceAlternativeJudge = lazyAgentJudge(agentJudgeFactory, agentJudgeConfig,
                new AgentJudgeSpec("choice-alternative-judge", "choiceAlternativeVerdict",
                        "juez de otra opción que también vale"),
                quizInstructionAgentGraphRunner, quizInstructionAgentDefinitionLocator);
        Evaluator choiceQualityJudge = lazyAgentJudge(agentJudgeFactory, agentJudgeConfig,
                new AgentJudgeSpec("choice-quality-judge", "choiceQualityVerdict", "juez de calidad de distractores"),
                quizInstructionAgentGraphRunner, quizInstructionAgentDefinitionLocator);

        // Hook C-15: the judge of package C lives in Opción múltiple, never the factory "without
        // judge" (F-RONDA1 decision: one contract, in two steps).
        ChoiceAlternativeJudgeRunFactory choiceAlternativeJudgeRunFactory = new DefaultChoiceAlternativeJudgeRunFactory(
                "multiple-choice-validity", judgeRuntime, choiceAlternativeJudge,
                new JacksonChoiceAlternativeVerdictReader(), new DefaultChoiceAlternativeConfig(),
                studentViewRenderer, correctorMatcher);

        // F-NIVFR-R002: the same catalog as the scorer, for the level of a derived form it brings apart
        AnalyzerProvider offLevelWordsProvider = new OffLevelWordsAnalyzerProvider(
                sentenceLexicalScorer, lemmaAbsenceConfig, topicTeachingResolver, studentViewRenderer, evpCatalog);
        AnalyzerProvider hintCoherenceProvider = new HintCoherenceAnalyzerProvider(irregularForms, studentViewRenderer);
        AnalyzerProvider answerCompletenessProvider =
                new AnswerCompletenessAnalyzerProvider(correctorMatcher, irregularForms, studentViewRenderer);
        // A + C: Opción múltiple runs the judge of package C (hook C-15).
        AnalyzerProvider multipleChoiceValidityProvider = new MultipleChoiceValidityAnalyzerProvider(
                correctorMatcher, choiceAlternativeJudgeRunFactory, studentViewRenderer);
        AnalyzerProvider dataCoherenceProvider = new DataCoherenceAnalyzerProvider(
                correctorMatcher, recordings, productionSentences, studentViewRenderer);
        AnalyzerProvider exactDuplicatesProvider = new ExactDuplicatesAnalyzerProvider(correctorMatcher, studentViewRenderer);
        AnalyzerProvider translationSenseProvider = new TranslationSenseAnalyzerProvider(judgeRuntime,
                fromEnglishJudge, fromTranslationJudge, new JacksonTranslationSenseVerdictReader(),
                new DefaultTranslationSenseConfig(), studentViewRenderer);
        AnalyzerProvider singleAnswerProvider = new SingleAnswerAnalyzerProvider(judgeRuntime, singleAnswerJudge,
                new JacksonSingleAnswerVerdictReader(), new DefaultSingleAnswerConfig(), studentViewRenderer,
                correctorMatcher);
        AnalyzerProvider miniTheoryProvider = new MiniTheoryAnalyzerProvider(judgeRuntime, miniTheoryJudge,
                new JacksonMiniTheoryVerdictReader(), new DefaultMiniTheoryConfig(), studentViewRenderer);

        // ----------------------------------------------------------------
        // Step 4b''': FEAT-RONDA2 -- the local tools of package B (hook B-07), built once:
        // LanguageTool, Ollama (CONTENT_AUDIT_OLLAMA_URL, or this machine's; «off» turns the local
        // models off, to take the exam of the alternative), the gate that decides which tool each
        // run measures with -- over the judges' runtime --, the finished sentence of grammar and
        // naturalness, and the two alternatives with claude -p (hook B-21), asked of their factory
        // on first use like the judges of round 1.
        // ----------------------------------------------------------------
        GrammarChecker grammarChecker = new LanguageToolGrammarChecker();
        OllamaEndpoint ollamaEndpoint = new OllamaEndpoint(ollamaUrl(System.getenv("CONTENT_AUDIT_OLLAMA_URL")), 120);
        OllamaModelFactory ollamaModelFactory = new DefaultOllamaModelFactory();
        LocalToolGate localToolGate = new DefaultLocalToolGate(judgeRuntime);
        FinishedSentenceReader finishedSentenceReader = new DefaultFinishedSentenceReader();
        Evaluator naturalnessAlternative = lazyAgentJudge(agentJudgeFactory, agentJudgeConfig,
                new AgentJudgeSpec("naturalness-yes-no", "naturalnessAnswer", "Naturalidad (claude -p)"),
                quizInstructionAgentGraphRunner, quizInstructionAgentDefinitionLocator);
        Evaluator nearDuplicatesAlternative = lazyAgentJudge(agentJudgeFactory, agentJudgeConfig,
                new AgentJudgeSpec("near-duplicates-pair-judge", "sameExerciseVerdict",
                        "Duplicados parecidos (claude -p)"),
                quizInstructionAgentGraphRunner, quizInstructionAgentDefinitionLocator);
        // B · FEAT-MLOCAL: the two local models, over the same endpoint (one look at /api/tags per run).
        YesNoModel yesNoModel = ollamaModelFactory.yesNoModel(ollamaEndpoint, "llama3:8b");
        EmbeddingModel embeddingModel = ollamaModelFactory.embeddingModel(ollamaEndpoint, "nomic-embed-text");

        // ----------------------------------------------------------------
        // Step 4c: FEAT-HALL -- the catalog: one provider per analyzer, and the only list of
        // them. Its order is the order of every run and of the findings within a node
        // (F-HALL-R014); a new analyzer adds one line here (the third hook of the TECH_SPEC).
        // The catalog validates every card at startup (F-HALL-R006 inv. 1).
        //
        // FEAT-RONDA1 (F-RONDA1-R004): the seventeen of round 1, in this order. The analyzers of
        // packages A, C and D are built above, and all seventeen are on.
        // ----------------------------------------------------------------
        List<AnalyzerProvider> analyzerProviders = new ArrayList<>();
        analyzerProviders.add(new SentenceLengthAnalyzerProvider(nlpTokenizer, sentenceLengthConfig));     //  1

        analyzerProviders.add(new KnowledgeTitleLengthAnalyzerProvider());                                 //  2

        analyzerProviders.add(new KnowledgeInstructionsLengthAnalyzerProvider());                          //  3

        analyzerProviders.add(new CocaBucketsAnalyzerProvider(nlpTokenizer, cocaBucketsConfig));           //  4

        analyzerProviders.add(new LemmaRecurrenceAnalyzerProvider(lemmaRecurrenceConfig));                 //  5

        analyzerProviders.add(new LemmaAbsenceAnalyzerProvider(evpCatalog, lemmaAbsenceConfig, sentenceLexicalScorer)); //  6

        analyzerProviders.add(offLevelWordsProvider);                                                     //  7 · D · FEAT-NIVFR

        analyzerProviders.add(new LemmaCountAnalyzerProvider(evpCatalog, lemmaCountConfig));               //  8

        analyzerProviders.add(quizInstructionProvider);                                                   //  9

        analyzerProviders.add(hintCoherenceProvider);                                                     // 10 · A · FEAT-PISTA

        analyzerProviders.add(answerCompletenessProvider);                                                // 11 · A · FEAT-RESPC

        analyzerProviders.add(multipleChoiceValidityProvider);                                            // 12 · A · FEAT-OMVAL (+ C · FEAT-MCVALE)

        analyzerProviders.add(dataCoherenceProvider);                                                     // 13 · A · FEAT-DATOS

        analyzerProviders.add(exactDuplicatesProvider);                                                   // 14 · A · FEAT-DUPEX

        analyzerProviders.add(translationSenseProvider);          // 15 · C · FEAT-TRSEN

        analyzerProviders.add(singleAnswerProvider);              // 16 · C · FEAT-UNICA

        analyzerProviders.add(miniTheoryProvider);                // 17 · C · FEAT-MINIT

        // FEAT-RONDA2 (F-RONDA2-R001): the three of package B go after the seventeen, in this
        // order -- «Gramática», «Naturalidad» and «Duplicados parecidos».
        analyzerProviders.add(new GrammarAnalyzerProvider(grammarChecker, finishedSentenceReader,
                localToolGate));                                                              // 18 · B · FEAT-GRAMA
        analyzerProviders.add(new NaturalnessAnalyzerProvider(yesNoModel, naturalnessAlternative, judgeRuntime,
                finishedSentenceReader, localToolGate));                                      // 19 · B · FEAT-NATUR
        analyzerProviders.add(new NearDuplicatesAnalyzerProvider(embeddingModel, nearDuplicatesAlternative,
                judgeRuntime, correctorMatcher, studentViewRenderer, localToolGate));         // 20 · B · FEAT-DUPAR

        // Independent criterion: existing judges keep their fingerprints, approvals and budgets.
        analyzerProviders.add(new com.learney.contentaudit.auditdomain.choicequality.ChoiceQualityAnalyzerProvider(
                judgeRuntime, choiceQualityJudge,
                new com.learney.contentaudit.quizinstructioninfrastructure.choicequality.JacksonChoiceQualityVerdictReader(),
                studentViewRenderer));
        AnalyzerCatalog analyzerCatalog = new DefaultAnalyzerCatalog(analyzerProviders);

        ScoreAggregator scoreAggregator =
                new com.learney.contentaudit.auditdomain.labs.LemmaAbsenceScoreAggregator();
        IAuditEngine auditEngine = new IAuditEngine(scoreAggregator, analyzerCatalog,
                new DefaultFindingCollector(), new DefaultContextNumbersCalculator());

        DefaultAuditRunner auditRunner = new DefaultAuditRunner(
                courseRepository, courseToAuditableMapper, auditEngine, analyzerCatalog);

        // Hook C-12: the exam of the judges, over the same runner as analyze (F-JEXAM-R001).
        JudgeExamService judgeExamService = new DefaultJudgeExamService(auditRunner, analyzerCatalog,
                judgeExamVerdictExporter, judgeExamRegistry, judgeRuntime);

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

        DefaultAnalyzerRegistry analyzerRegistry = new DefaultAnalyzerRegistry(analyzerCatalog);
        AnalyzerStatsTransformer analyzerStatsTransformer = new DefaultAnalyzerStatsTransformer();

        Map<String, DetailedFormatter> detailedFormatters = new HashMap<>();
        detailedFormatters.put("lemma-absence", new LemmaAbsenceDetailedFormatter());
        detailedFormatters.put("coca-buckets-distribution", new CocaBucketsDetailedFormatter());
        // QuizInstructionDetailedFormatter is package-private (visibility escalated to
        // @architect, see its bootstrap's Javadoc); obtained via the same-package bridge.
        detailedFormatters.put("quiz-instruction", QuizInstructionDetailedFormatterBootstrap.create());
        // Hooks D-11 and D-15: lemma-recurrence shows its detail in analyze --detailed and in stats
        // analyzer. Turned on with FEAT-RECUR (package D), which builds the formatter.
        LemmaRecurrenceDetailedFormatter lemmaRecurrenceDetailedFormatter = new LemmaRecurrenceDetailedFormatter();
        Map<String, DetailedFormatter> statsDetailedFormatters = new HashMap<>();

        detailedFormatters.put("lemma-recurrence", lemmaRecurrenceDetailedFormatter);          // D · FEAT-RECUR

        statsDetailedFormatters.put("lemma-recurrence", lemmaRecurrenceDetailedFormatter);     // D · FEAT-RECUR

        // ----------------------------------------------------------------
        // Step 6: Persistence stores — all constructed with resolved baseDir
        // ----------------------------------------------------------------
        AuditReportStore auditReportStore = new FileSystemAuditReportStore(baseDir, studentViewRenderer);
        RefinementPlanStore refinementPlanStore = new FileSystemRefinementPlanStore(baseDir);
        RevisionArtifactStore revisionArtifactStore = new FileSystemRevisionArtifactStore(baseDir);
        ImpactPreviewStore impactPreviewStore = new FileSystemImpactPreviewStore(baseDir);
        ActiveAnalysisSelectionStore activeAnalysisSelectionStore =
                new FileSystemActiveAnalysisSelectionStore(baseDir);
        // FEAT-RONDA2 (hook F-06): the fixed baseline, apart from the active pair (F-PROYU-R006);
        // delete and prune never take it away.
        BaselineStore baselineStore = new FileSystemBaselineStore(baseDir);
        // FEAT-RONDA2 (hook E-09): the batches of corrections (FEAT-TANDA, package E), under --workdir.
        BatchStore batchStore = new FileSystemBatchStore(baseDir);

        // ----------------------------------------------------------------
        // Step 6b: FEAT-RONDA2 -- before and after, package F (hook F-06). The projection of the
        // course with its pending proposals is the one photo every comparison measures; it is
        // asked of its factory on first use, so no other command pays for it.
        // ----------------------------------------------------------------
        ProjectionStore projectionStore = new FileSystemProjectionStore(baseDir);
        GoalDeclarationSource goalDeclarationSource =
                new FileSystemGoalDeclarationSource(Paths.get(projectRoot, "analysis", "metas.json"));
        SealedGoalsStore sealedGoalsStore = new FileSystemSealedGoalsStore(baseDir);
        BlindSampleStore blindSampleStore = new FileSystemBlindSampleStore(baseDir);
        CourseProjector courseProjector = lazyCourseProjector(() -> new DefaultCourseProjectorFactory().create(
                new CourseProjectorConfig(activeAnalysisSelectionStore, auditReportStore, refinementPlanStore,
                        revisionArtifactStore, (CourseRepository) courseRepository, courseToAuditableMapper,
                        auditEngine)));
        GoalBook goalBook = new DefaultGoalBook(goalDeclarationSource, sealedGoalsStore, analyzerCatalog);
        SampleStatistics sampleStatistics = new DefaultSampleStatistics();
        SampleOutcomeSource sampleOutcomeSource = new DefaultSampleOutcomeSource(blindSampleStore, sampleStatistics);
        // Hook F-14: the comparison never returns a change for a word its topic teaches, as the panel. F-FILAS-R005:
        // the example of each row shows its exercise as the student sees it.
        BatchComparator batchComparator = new DefaultBatchComparator(courseProjector, baselineStore,
                auditReportStore, goalBook, analyzerCatalog, auditEngine, new LeaveOneOutChangeAttributor(auditEngine,
                        (completed, total) -> {
                            System.err.printf("@@CONTENT_AUDIT_PROGRESS {\"phase\":\"ATTRIBUTING_REGRESSIONS\","
                                    + "\"completed\":%d,\"total\":%d}%n", completed, total);
                            System.err.flush();
                        }),
                sampleOutcomeSource, topicTeachingResolver, studentViewRenderer);
        BlindSampleService blindSampleService = new DefaultBlindSampleService(courseProjector, batchComparator,
                goalBook, baselineStore, studentViewRenderer, blindSampleStore, sampleStatistics);
        BlindErrorRateService blindErrorRateService = new DefaultBlindErrorRateService(courseProjector,
                baselineStore, auditReportStore, studentViewRenderer, blindSampleStore, sampleStatistics);

        // ----------------------------------------------------------------
        // Step 6c: FEAT-RONDA2 -- correction by batches, package E (hook E-09): the fixes by rule
        // of A and of grammar (hooks E-06 and F-RONDA2-R009) and the decisions of the tray. The
        // panel, the batch and the tray are asked of their factories, with the baseline, the
        // comparator, the goal book and the blind sample above, when package E builds them.
        // ----------------------------------------------------------------
        List<RuleFix> ruleFixes = List.of(
                new HintCoherenceRuleFix(irregularForms),
                new AnswerCompletenessRuleFix(correctorMatcher),
                new MultipleChoiceValidityRuleFix(),
                new DataCoherenceRuleFix(quizSentenceConverter),
                new GrammarRuleFix());
        TrayDecisionStore trayDecisionStore = new FileSystemTrayDecisionStore(baseDir);
        // E · FEAT-RONDA2 (hook E-09): the batch is asked of its factory on first use; its collaborators are
        // put together in the block of E after the commands of F. reject and batch read it from here.
        java.util.concurrent.atomic.AtomicReference<Supplier<com.learney.contentaudit.revisiondomain.batch.BatchService>>
                batchFactory = new java.util.concurrent.atomic.AtomicReference<>();
        com.learney.contentaudit.revisiondomain.batch.BatchService batchService =
                lazyBatchService(() -> batchFactory.get().get());

        // ----------------------------------------------------------------
        // Step 7: Refiner + Revision engines
        // ----------------------------------------------------------------
        // FEAT-RONDA2 (hook E-09): what the fix of each analyzer of errors may touch, one
        // declaration per analyzer; the plan, get task and the batch read it here. The catalog
        // breaks at startup on a declaration of an analyzer it does not have, so the two of
        // package B join when B registers its providers above.
        List<AnalyzerCorrection> analyzerCorrections = new ArrayList<>(List.of(
                new QuizInstructionCorrection(),
                new LemmaAbsenceCorrection(),
                new HintCoherenceCorrection(),               // A (E-03)
                new AnswerCompletenessCorrection(),          // A (E-03)
                new MultipleChoiceValidityCorrection(),      // A (E-03)
                new com.learney.contentaudit.auditdomain.choicequality.ChoiceQualityCorrection(),
                new DataCoherenceCorrection(),               // A (E-03)
                new ExactDuplicatesCorrection(),             // A (E-03)
                new TranslationSenseCorrection(),            // C (E-04)
                new SingleAnswerCorrection(),                // C (E-04)
                new MiniTheoryCorrection(),                  // C (E-04)
                new OffLevelWordsCorrection()));             // D (E-05)
        for (AnalyzerCorrection ofPackageB : List.of(new GrammarCorrection(),        // B (F-RONDA2-R009)
                new NearDuplicatesCorrection())) {                                   // B (F-RONDA2-R009)
            if (analyzerCatalog.find(ofPackageB.analyzerName()).isPresent()) {
                analyzerCorrections.add(ofPackageB);
            }
        }
        AnalyzerCorrectionCatalog analyzerCorrectionCatalog =
                new DefaultAnalyzerCorrectionCatalog(analyzerCorrections, analyzerCatalog);
        RefinerEngine refinerEngine = new DefaultRefinerEngine(analyzerCatalog, analyzerCorrectionCatalog);
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
        // correction candidate generator, and the three judgment criteria:
        // FAITHFUL_TRANSLATION / SOLVABLE_SAME_KIND (non-vetoing, feed
        // judgmentEvaluators) and REAL_WORLD_SENSE (F-QICOR-R016 — absolute,
        // vetoing, its own mandatory CandidateCriteriaConfig slot).
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
                new DefaultQuizInstructionComplianceCheckerFactory(studentViewRenderer).create(
                        evaluationSessionFactory, quizInstructionJudge, quizInstructionVerdictReader,
                        new EvaluationBudget(quizInstructionConfig.getDefaultMaxNewEvaluations()));

        QuizInstructionCorrectionConfig quizInstructionCorrectionConfig =
                new EnvQuizInstructionCorrectionConfigResolver().resolve(System.getenv());

        QuizInstructionCandidateGenerator quizInstructionGenerator;
        String quizInstructionProviderId;
        List<CandidateCriterionEvaluator> judgmentEvaluators;
        CandidateCriterionEvaluator realWorldSenseEvaluator;

        if (lagenMode == LagenMode.CANNED) {
            quizInstructionGenerator = request -> new QuizInstructionGeneratorResponse(
                    request.getContext() != null ? request.getContext().getQuizSentence() : "",
                    request.getContext() != null ? request.getContext().getTranslation() : "",
                    "canned: sin cambios", null, 0);
            quizInstructionProviderId = "canned:fixed";
            judgmentEvaluators = List.of(
                    cannedPassingJudgmentEvaluator(CorrectionCriterion.FAITHFUL_TRANSLATION),
                    cannedPassingJudgmentEvaluator(CorrectionCriterion.SOLVABLE_SAME_KIND));
            // F-QICOR-R016: absolute criterion, own mandatory slot — never part of
            // judgmentEvaluators. Canned mode follows the same precedent as the other
            // two: always PASSED, no LLM contact.
            realWorldSenseEvaluator = cannedPassingJudgmentEvaluator(CorrectionCriterion.REAL_WORLD_SENSE);
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
                // F-QICOR-R016: same factory, indexed by criterion — different, mandatory
                // slot (absolute, vetoing; never part of judgmentEvaluators).
                realWorldSenseEvaluator = candidateJudgmentEvaluatorFactory.create(
                        quizInstructionLagenConfig, CorrectionCriterion.REAL_WORLD_SENSE);
            } else {
                final String deferredMsg = quizInstructionLagenSetupError;
                quizInstructionGenerator = request -> {
                    throw new ProposalStrategyFailedException(
                            "quiz-instruction-agent",
                            request.getContext() != null ? request.getContext().getTaskId() : null,
                            "INVALID_CONFIG: " + deferredMsg);
                };
                quizInstructionProviderId = "lagen:misconfigured";
                // Without a resolvable LLM config, the two non-vetoing judgment criteria
                // simply do not enter the active catalog (CandidateCriteriaConfig.judgmentEvaluators
                // doc); the deterministic criteria and INSTRUCTION_COMPLIANCE still run.
                judgmentEvaluators = List.of();
                // F-QICOR-R016's slot is mandatory and the criterion is absolute: it can
                // NEVER be left approving-by-default here the way the empty list above lets
                // the two non-vetoing criteria quietly disappear. With no resolvable judge,
                // report NOT_EVALUABLE naming this setup failure so every candidate stays
                // ineligible and the run ends in "correction not achieved" naming
                // REAL_WORLD_SENSE — never an unjudged sentence written to the course.
                realWorldSenseEvaluator = notEvaluableJudgmentEvaluator(
                        CorrectionCriterion.REAL_WORLD_SENSE, deferredMsg);
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
                auditReportStore, realWorldSenseEvaluator);
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

        // assess-candidate — F-QICOR-R015: consults the candidate-criteria catalog over a
        // candidate quiz sentence that never went through a proposal. Built from the exact
        // same seven collaborators as revisionEngineConfig above (candidateAssessor,
        // proposalDeriver, refinementPlanStore, auditReportStore, correctionContextResolver,
        // courseRepository), with elementLocator left null so the factory builds its own
        // DefaultCourseElementLocator internally (package-private on purpose — same pattern
        // as ConsolidatedViewBuilderConfig below), so the consultation and the correction
        // engine's own re-verification of a delivered candidate (F-QICOR-R012) can never
        // disagree about what a candidate measures.
        QuizInstructionCandidateAssessorConfig quizInstructionCandidateAssessorConfig =
                new QuizInstructionCandidateAssessorConfig(
                        candidateAssessor,
                        proposalDeriver,
                        refinementPlanStore,
                        auditReportStore,
                        correctionContextResolver,
                        (CourseRepository) courseRepository,
                        null); // factory creates DefaultCourseElementLocator internally (package-private)
        QuizInstructionCandidateAssessor quizInstructionCandidateAssessor =
                new DefaultQuizInstructionCandidateAssessorFactory()
                        .create(quizInstructionCandidateAssessorConfig);
        AssessCandidateCmd assessCandidateCmd = new AssessCandidateCmd(quizInstructionCandidateAssessor);

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
        ReviseCmd reviseCmd = new ReviseCmd(revisionEngine, refinementPlanStore, batchStore);
        cmd.addSubcommand("revise", new picocli.CommandLine(reviseCmd));

        // revise-instructions — F-QICOR-R008 (journey J002): bounded batch of
        // QUIZ_INSTRUCTION corrections over a plan. Its own verb, always explicit —
        // never triggered by "analyze" or by a plain "revise task <id>".
        cmd.addSubcommand("revise-instructions", new picocli.CommandLine(reviseInstructionsCmd));

        // assess-candidate — F-QICOR-R015: consult the candidate-criteria catalog over a
        // candidate quiz sentence without proposing it. Invoked by
        // .sentinel/agents/quiz-instruction-corrector/scripts/assess.sh once per attempt.
        cmd.addSubcommand("assess-candidate", new picocli.CommandLine(assessCandidateCmd));

        // approve / reject — only available when ProposalDecisionService could be constructed
        if (proposalDecisionService != null) {
            ApproveCmd approveCmd = new ApproveCmd(proposalDecisionService, batchStore);
            cmd.addSubcommand("approve", new picocli.CommandLine(approveCmd));

            // E · FEAT-BANDEJA (hook E-09): rejecting a proposal of the open batch takes its changes out of it.
            RejectCmd rejectCmd = new RejectCmd(proposalDecisionService, batchService);
            cmd.addSubcommand("reject", new picocli.CommandLine(rejectCmd));
        }

        // config
        ConfigAnalyzerCmd configAnalyzerCmd = new ConfigAnalyzerCmd(analyzerRegistry);
        cmd.addSubcommand("config", new picocli.CommandLine(configAnalyzerCmd));

        // stats
        StatsAnalyzerCmd statsAnalyzerCmd = new StatsAnalyzerCmd(
                analyzerRegistry, analyzerStatsTransformer, auditRunner, statsDetailedFormatters);
        cmd.addSubcommand("stats", new picocli.CommandLine(statsAnalyzerCmd));

        // get — inject baseDir for plan listing; revisionArtifactStore para proposals;
        // impactPreviewStore + formatter para el preview de impacto (F-PIPRE-R007);
        // suggestedLemmaQueryPort para F-CSLATDC-R001/R002
        LemmaAbsenceContextSuggestedLemmaQueryPort suggestedLemmaQueryPort =
                LemmaAbsenceContextSuggestedLemmaQueryPort.create(
                        lemmaAbsenceContextResolver, evpCatalog, lemmaCountConfig);
        // FEAT-RONDA1: get student-view (F-RONDA1-R003) reads the active analysis or the latest;
        // get word-lights (hooks D-05, D-06) asks the lights of FEAT-SEMAF (package D).
        TopicStudentViewQuery topicStudentViewQuery = new DefaultTopicStudentViewQuery(studentViewRenderer, recordings);
        TopicWordLightsQuery topicWordLightsQuery = new DefaultTopicWordLightsQuery(
                sentenceLexicalEvaluator, suggestedLemmaQueryPort, topicTeachingResolver);
        // FEAT-RONDA2 (hook E-07): get task of an error task shows its finding and what its fix
        // may touch.
        ErrorTaskBriefer errorTaskBriefer = new DefaultErrorTaskBriefer(
                analyzerCorrectionCatalog, auditNodeIndexFactory, studentViewRenderer);
        GetCmd getCmd = new GetCmd(auditReportStore, refinementPlanStore, analyzerRegistry,
                correctionContextResolver, impactPreviewStore, new DefaultImpactPreviewFormatter(),
                correctionContextJsonMapper, suggestedLemmaQueryPort, topicWordLightsQuery,
                topicStudentViewQuery, activeAnalysisSelectionStore, errorTaskBriefer);
        getCmd.setBaseDir(baseDir);
        getCmd.setRevisionArtifactStore(revisionArtifactStore);
        cmd.addSubcommand("get", new picocli.CommandLine(getCmd));

        // delete — inject baseDir for filesystem-level removal
        DeleteCmd deleteCmd = new DeleteCmd(auditReportStore, refinementPlanStore,
                activeAnalysisSelectionStore, baselineStore);
        deleteCmd.setBaseDir(baseDir);
        cmd.addSubcommand("delete", new picocli.CommandLine(deleteCmd));

        // prune — inject baseDir for filesystem-level removal
        PruneCmd pruneCmd = new PruneCmd(auditReportStore, refinementPlanStore,
                activeAnalysisSelectionStore, baselineStore);
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

        // exam -- hook C-18 (FEAT-JEXAM, package C): exam run / record / status, the fixed exam of the
        // judges. Without --budget, exam run is a rehearsal that pays nothing (F-JEXAM-R010).
        // F-JEXAM-R014: feed the command the already-resolved configs; it records the effective
        // provider/model/effort without re-reading environment variables or inventing defaults.
        ExamCmd.ExamProvenance quizInstructionExamProvenance = new ExamCmd.ExamProvenance(
                quizInstructionJudgeConfig.getProviderName(), quizInstructionJudgeConfig.getModelName(),
                quizInstructionJudgeConfig.getReasoningEffort());
        ExamCmd.ExamProvenance agentExamProvenance = new ExamCmd.ExamProvenance(
                agentJudgeConfig.getProviderName(), agentJudgeConfig.getModelName(),
                agentJudgeConfig.getReasoningEffort());
        Map<String, ExamCmd.ExamProvenance> examProvenance = Map.ofEntries(
                Map.entry("quiz-instruction", quizInstructionExamProvenance),
                Map.entry("translation-sense", agentExamProvenance),
                Map.entry("single-answer", agentExamProvenance),
                Map.entry("mini-theory", agentExamProvenance),
                Map.entry("multiple-choice-validity", agentExamProvenance),
                Map.entry("multiple-choice-quality", agentExamProvenance),
                Map.entry("naturalness", agentExamProvenance),
                Map.entry("near-duplicates", agentExamProvenance));
        // F-JEXAM-R015: exam status -f json asks the catalog who to read, only when it is used.
        cmd.addSubcommand("exam", new picocli.CommandLine(
                new ExamCmd(judgeExamService, examProvenance, () -> analyzerCatalog.list().stream()
                        .map(AnalyzerDescriptor::getName).toList())));   // C · FEAT-JEXAM

        // FEAT-RONDA2 (hooks F-06 and E-09): the commands of packages F and E, each registered by
        // its package with its picocli wrapper: set-baseline, save-projection, compare, get-goals
        // and set-goals, sample and error-rate (F, built here over Step 6b), and batch and tray
        // (E, over the batch and the tray of its factories).
        DefaultSetBaselineCommand setBaselineCommand = new DefaultSetBaselineCommand(baselineStore, auditReportStore);
        DefaultSaveProjectionCommand saveProjectionCommand =
                new DefaultSaveProjectionCommand(courseProjector, projectionStore);
        DefaultCompareCommand compareCommand = new DefaultCompareCommand(batchComparator,
                new com.learney.contentaudit.auditcli.formatting.DefaultBatchComparisonFormatter());
        DefaultGoalsCommand goalsCommand = new DefaultGoalsCommand(goalBook);
        DefaultBlindSampleCommand blindSampleCommand = new DefaultBlindSampleCommand(blindSampleService);
        DefaultErrorRateCommand errorRateCommand = new DefaultErrorRateCommand(blindErrorRateService);

        // F · FEAT-RONDA2 (hook F-06): the verbs of package F, each over its command above.
        cmd.addSubcommand("set-baseline", new picocli.CommandLine(new SetBaselineCmd(setBaselineCommand)));
        cmd.addSubcommand("save-projection", new picocli.CommandLine(new SaveProjectionCmd(saveProjectionCommand)));
        cmd.addSubcommand("compare", new picocli.CommandLine(new CompareCmd(compareCommand)));
        cmd.addSubcommand("get-goals", new picocli.CommandLine(new GoalsCmd.Get(goalsCommand)));
        cmd.addSubcommand("set-goals", new picocli.CommandLine(new GoalsCmd.Set(goalsCommand)));
        cmd.addSubcommand("sample", new picocli.CommandLine(new SampleCmd(blindSampleCommand)));
        cmd.addSubcommand("error-rate", new picocli.CommandLine(new ErrorRateCmd(errorRateCommand)));

        // E · FEAT-RONDA2 (hook E-09): the batch and the tray of package E, built on first use so no other
        // command pays for them. The panel takes its proposer from CONTENT_AUDIT_LAGEN_* only when a panel runs,
        // under an explicit --cap; the rules query nothing. The locator of the course and the table of parts to
        // fields go null: the factory of the batch builds the ones of revision-domain, as revise and approve do.
        com.learney.contentaudit.revisiondomain.panelengine.DefaultPanelRunnerFactory panelRunnerFactory =
                new com.learney.contentaudit.revisiondomain.panelengine.DefaultPanelRunnerFactory();
        final String batchCourse = CoursePathResolver.resolve(null);
        Supplier<com.learney.contentaudit.revisiondomain.panel.PanelRunnerConfig> panelConfig = () ->
                new com.learney.contentaudit.revisiondomain.panel.PanelRunnerConfig(panelProposer(llmProviderResolver),
                        analyzerCatalog, analyzerCorrectionCatalog, auditEngine, courseToAuditableMapper,
                        studentViewRenderer, nlpTokenizer, sentenceLexicalEvaluator, topicTeachingResolver,
                        new com.learney.contentaudit.auditdomain.datacoherence.DefaultSpokenTextBuilder(),
                        sentenceLengthConfig);
        Supplier<com.learney.contentaudit.revisiondomain.panel.PanelRunner> panelRunner = memoized(
                () -> panelRunnerFactory.create(panelConfig.get()));
        Supplier<com.learney.contentaudit.revisiondomain.batch.BatchServiceConfig> batchConfig = memoized(() -> {
            if (batchCourse == null) {
                throw new IllegalStateException("Falta la ruta del curso: CONTENT_AUDIT_CONTENT_FOLDER o .env.");
            }
            var config = new com.learney.contentaudit.revisiondomain.batch.BatchServiceConfig(batchStore, refinementPlanStore,
                            auditReportStore, activeAnalysisSelectionStore, revisionArtifactStore,
                            (CourseRepository) courseRepository, null, courseToAuditableMapper, auditEngine,
                            refinerEngine, errorTaskBriefer, analyzerCorrectionCatalog, ruleFixes, panelRunner.get(),
                            panelRunnerFactory.meter(panelConfig.get()), panelRunnerFactory, batchComparator,
                            baselineStore, blindSampleService, topicWordLightsQuery, null,
                            preservationFactory.createCheck(), Paths.get(batchCourse), goalBook, analyzerCatalog);
            config.setReviewCache(new com.learney.contentaudit.auditinfrastructure.FileSystemBatchReviewCache(baseDir));
            // F-PUBPROD-R002: la lista de publicación lleva cada campo como lo guarda producción, con la misma forma
            // con la que el curso se escribe en disco.
            config.setProductionDocumentProjector(courseRepository);
            return config;
        });
        batchFactory.set(memoized(() -> new com.learney.contentaudit.revisiondomain.batchengine.DefaultBatchServiceFactory().create(batchConfig.get())));
        com.learney.contentaudit.revisiondomain.tray.TrayService trayService = lazyTrayService(memoized(() ->
                new com.learney.contentaudit.revisiondomain.trayengine.DefaultTrayServiceFactory().create(
                        new com.learney.contentaudit.revisiondomain.tray.TrayServiceConfig(batchService, batchStore,
                                trayDecisionStore, blindSampleService, panelRunner.get(), panelRunnerFactory,
                                studentViewRenderer, evaluationContentFingerprinter, (CourseRepository) courseRepository,
                                batchCourse != null ? Paths.get(batchCourse) : null))));
        var batchCommand = batchCommandLine(batchService, memoized(() -> {
            if (batchCourse == null) throw new IllegalStateException("Falta la ruta del curso.");
            return new com.learney.contentaudit.revisiondomain.batchengine.DefaultMiniTheoryCorrectionServiceFactory().create(
                    batchStore, new com.learney.contentaudit.revisiondomain.batchengine.DefaultBatchReviewServiceFactory().create(
                            batchStore, revisionArtifactStore, refinementPlanStore,
                            new com.learney.contentaudit.auditinfrastructure.FileSystemBatchReviewCache(baseDir)),
                    revisionArtifactStore, courseRepository, Paths.get(batchCourse),
                    new com.learney.contentaudit.auditinfrastructure.minitheory.FileSystemMiniTheoryWorkspaceStore(baseDir),
                    courseToAuditableMapper, auditEngine, () -> {
                        LagenConfig selected = new DefaultLagenConfigResolver(llmProviderResolver).resolve(System.getenv());
                        return new com.learney.contentaudit.revisioninfrastructure.minitheory.DefaultMiniTheoryRewriterFactory()
                                .create(selected, System.getenv().getOrDefault("CONTENT_AUDIT_REASONING_EFFORT", "medium"));
                    });
        }));
        batchCommand.addSubcommand("selective", new SelectiveCorrectionCmd(memoized(() -> {
            var effective = new DefaultLagenConfigResolver(llmProviderResolver).resolve(System.getenv());
            String runtime = effective.getProviderName() + " / " + effective.getModelId() + " / "
                    + System.getenv().getOrDefault("CONTENT_AUDIT_REASONING_EFFORT", "medium");
            return new com.learney.contentaudit.revisiondomain.batchengine.DefaultSelectiveCorrectionServiceFactory().create(
                    batchConfig.get(), new com.learney.contentaudit.auditinfrastructure.minitheory.FileSystemMiniTheoryWorkspaceStore(baseDir)::fingerprint,
                    runtime);
        })));
        cmd.addSubcommand("batch", batchCommand);            // E · FEAT-TANDA
        cmd.addSubcommand("problem", new ProblemCmd(baseDir, problemProject, System.out));
        cmd.addSubcommand("tray", new picocli.CommandLine(new TrayCmd(trayService, quizSentenceConverter, batchStore))); // E · BANDEJA

        // ----------------------------------------------------------------
        // Step 9: Execute
        // ----------------------------------------------------------------
        int exitCode = cmd.execute(remainingArgs.toArray(new String[0]));
        System.exit(exitCode);
    }

    /** Keep the nested command registration shared with CLI routing regression tests. */
    static picocli.CommandLine batchCommandLine(
            com.learney.contentaudit.revisiondomain.batch.BatchService batchService,
            Supplier<com.learney.contentaudit.revisiondomain.minitheorycorrection.MiniTheoryCorrectionService> miniTheoryService) {
        var command = new picocli.CommandLine(new BatchCmd(batchService));
        command.addSubcommand("mini-theory", new MiniTheoryCorrectionCmd(miniTheoryService));
        return command;
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

    /**
     * F-QICOR-R016 fallback for {@code CandidateCriteriaConfig.realWorldSenseEvaluator} when no
     * LLM judge configuration is resolvable (the {@code quizInstructionLagenSetupError != null}
     * branch above). REAL_WORLD_SENSE is absolute and its slot is mandatory: it can never be left
     * null (the factory rejects the whole catalog with AbsoluteCriterionEvaluatorMissingException
     * instead) and it must never approve by omission the way an empty {@code judgmentEvaluators}
     * lets the two non-vetoing criteria quietly disappear. Always NOT_EVALUABLE, carrying the
     * setup failure as its reason, so a run without a working judge ends in "correction not
     * achieved" naming REAL_WORLD_SENSE instead of writing an unjudged sentence to the course.
     */
    private static CandidateCriterionEvaluator notEvaluableJudgmentEvaluator(
            CorrectionCriterion criterion, String reason) {
        return new CandidateCriterionEvaluator() {
            @Override
            public CorrectionCriterion criterion() {
                return criterion;
            }

            @Override
            public CriterionVerdict evaluate(
                    com.learney.contentaudit.revisiondomain.candidatecriteria.CandidateAssessmentInput input) {
                return new CriterionVerdict(criterion, CriterionOutcome.NOT_EVALUABLE,
                        "sin juez configurado: " + reason);
            }
        };
    }

    /**
     * FEAT-RONDA1 (hook A-06): the folder of the default course, where the recordings registry and
     * the production copy live next to it; {@code db/} of the project when none is configured.
     */
    private static Path defaultCourseDb(String projectRoot) {
        String course = CoursePathResolver.resolve(null);
        if (course != null) {
            Path parent = Paths.get(course).toAbsolutePath().normalize().getParent();
            if (parent != null) {
                return parent;
            }
        }
        return Paths.get(projectRoot, "db");
    }

    /** A recordings registry that knows no key, for when the registry file does not exist yet. */
    private static AudioRecordingRegistry emptyRecordingRegistry() {
        return new AudioRecordingRegistry() {
            @Override
            public Optional<AudioRecording> recordingOf(String audioKey) {
                return Optional.empty();
            }

            @Override
            public String version() {
                return "sin registro";
            }
        };
    }

    /**
     * The corrector's comparison, built on first use: loading its table reads a file only the
     * analyzers of round 1 need, so no other command reads it (hook A-06).
     */
    private static CorrectorMatcher lazyCorrectorMatcher(Supplier<CorrectorMatcher> factory) {
        return new CorrectorMatcher() {
            private CorrectorMatcher delegate;

            private synchronized CorrectorMatcher delegate() {
                if (delegate == null) {
                    delegate = factory.get();
                }
                return delegate;
            }

            @Override
            public Set<String> canonicalForms(String text) {
                return delegate().canonicalForms(text);
            }

            @Override
            public boolean accepts(GapContext context, List<String> acceptedOptions, String written) {
                return delegate().accepts(context, acceptedOptions, written);
            }

            @Override
            public boolean sameAnswer(String first, String second) {
                return delegate().sameAnswer(first, second);
            }

            @Override
            public boolean sameAnswer(GapContext context, String first, String second) {
                return delegate().sameAnswer(context, first, second);
            }

            @Override
            public String tableVersion() {
                return delegate().tableVersion();
            }
        };
    }

    /** Hook B-07: the address of Ollama, CONTENT_AUDIT_OLLAMA_URL or this machine's. */
    private static String ollamaUrl(String configured) {
        return configured != null && !configured.isBlank() ? configured.strip() : "http://127.0.0.1:11434";
    }

    /**
     * The projection of package F, asked of its factory on first use (hook F-06): only the
     * commands that compare or project build it.
     */
    private static CourseProjector lazyCourseProjector(Supplier<CourseProjector> factory) {
        return new CourseProjector() {
            private CourseProjector delegate;

            private synchronized CourseProjector delegate() {
                if (delegate == null) {
                    delegate = factory.get();
                }
                return delegate;
            }

            @Override
            public CourseProjection project(Path coursePath, ProjectionScope scope) {
                return delegate().project(coursePath, scope);
            }
        };
    }

    /**
     * A judge of round 1, asked of its factory on first use (hook C-14): its id is the spec's, so
     * reading it never builds the judge, and a run that does not ask the judge never builds it.
     */
    private static Evaluator lazyAgentJudge(AgentJudgeFactory factory, AgentJudgeConfig config,
            AgentJudgeSpec spec, AgentGraphRunner agentGraphRunner, AgentDefinitionLocator agentDefinitionLocator) {
        return new Evaluator() {
            private Evaluator delegate;

            private synchronized Evaluator delegate() {
                if (delegate == null) {
                    delegate = factory.create(spec, config, agentGraphRunner, agentDefinitionLocator);
                }
                return delegate;
            }

            @Override
            public String evaluatorId() {
                return spec.getEvaluatorId();
            }

            @Override
            public EvaluationOutcome evaluate(EvaluationSubject subject) {
                return delegate().evaluate(subject);
            }

            @Override
            public Optional<String> evaluatorVersion() {
                return delegate().evaluatorVersion();
            }

            @Override
            public EvaluationRuntimeIdentity runtimeIdentity() {
                return delegate().runtimeIdentity();
            }

            @Override
            public Set<String> compatibleEvaluatorVersions() {
                return delegate().compatibleEvaluatorVersions();
            }
        };
    }

    // ----------------------------------------------------------------
    // E · FEAT-RONDA2 (hook E-09): the batch and the tray, on first use
    // ----------------------------------------------------------------

    /** A value built once, the first time it is asked. */
    private static <T> Supplier<T> memoized(Supplier<T> factory) {
        return new Supplier<>() {
            private T value;

            @Override
            public synchronized T get() {
                if (value == null) {
                    value = factory.get();
                }
                return value;
            }
        };
    }

    /** The proposer of the panel from CONTENT_AUDIT_LAGEN_*, or one that says why it cannot propose. */
    private static com.learney.contentaudit.revisiondomain.panel.PanelProposer panelProposer(
            LlmProviderResolver llmProviderResolver) {
        try {
            LagenConfig config = new DefaultLagenConfigResolver(llmProviderResolver).resolve(System.getenv());
            return new com.learney.contentaudit.revisioninfrastructure.panelagent.DefaultPanelAgentProposerFactory()
                    .create(config);
        } catch (RuntimeException e) {
            String why = e.getMessage();
            return request -> {
                throw new IllegalStateException("La mesa no tiene proponente: " + why);
            };
        }
    }

    /** The batch of package E, asked of its factory on first use (hook E-09). */
    private static com.learney.contentaudit.revisiondomain.batch.BatchService lazyBatchService(
            Supplier<com.learney.contentaudit.revisiondomain.batch.BatchService> factory) {
        return new com.learney.contentaudit.revisiondomain.batch.BatchService() {
            @Override
            public com.learney.contentaudit.revisiondomain.batch.CorrectionBatch open(
                    com.learney.contentaudit.revisiondomain.batch.BatchOpenRequest request) {
                return factory.get().open(request);
            }

            @Override
            public Optional<com.learney.contentaudit.revisiondomain.batch.CorrectionBatch> current() {
                return factory.get().current();
            }

            @Override
            public com.learney.contentaudit.revisiondomain.batch.BatchEstimate estimate() {
                return factory.get().estimate();
            }

            @Override
            public com.learney.contentaudit.revisiondomain.batch.BatchReport run(
                    com.learney.contentaudit.revisiondomain.batch.BatchRunRequest request) {
                return factory.get().run(request);
            }

            @Override
            public com.learney.contentaudit.revisiondomain.batch.BatchClosing close(
                    com.learney.contentaudit.revisiondomain.batch.BatchCloseRequest request) {
                return factory.get().close(request);
            }

            @Override
            public com.learney.contentaudit.revisiondomain.batch.CorrectionBatch adjust(
                    com.learney.contentaudit.revisiondomain.batch.BatchAdjustment adjustment) {
                return factory.get().adjust(adjustment);
            }

            @Override
            public com.learney.contentaudit.revisiondomain.batch.BatchReport report(String batchId) {
                return factory.get().report(batchId);
            }

            @Override
            public com.learney.contentaudit.revisiondomain.batch.BatchReview review(String batchId) {
                return factory.get().review(batchId);
            }

            @Override
            public com.learney.contentaudit.revisiondomain.batch.BatchReview review(
                    com.learney.contentaudit.revisiondomain.batch.BatchReviewDecision decision) {
                return factory.get().review(decision);
            }

            @Override
            public com.learney.contentaudit.revisiondomain.batch.BatchPublication publish(
                    com.learney.contentaudit.revisiondomain.batch.BatchPublishRequest request) {
                return factory.get().publish(request);
            }
        };
    }

    /** The tray of package E, asked of its factory on first use (hook E-09). */
    private static com.learney.contentaudit.revisiondomain.tray.TrayService lazyTrayService(
            Supplier<com.learney.contentaudit.revisiondomain.tray.TrayService> factory) {
        return new com.learney.contentaudit.revisiondomain.tray.TrayService() {
            @Override
            public com.learney.contentaudit.revisiondomain.tray.Tray view() {
                return factory.get().view();
            }

            @Override
            public com.learney.contentaudit.revisiondomain.tray.Tray answer(
                    com.learney.contentaudit.revisiondomain.tray.TrayResponse response) {
                return factory.get().answer(response);
            }

            @Override
            public com.learney.contentaudit.revisiondomain.batch.BatchPublication publish(String approvedBy, String note) {
                return factory.get().publish(approvedBy, note);
            }
        };
    }
}
