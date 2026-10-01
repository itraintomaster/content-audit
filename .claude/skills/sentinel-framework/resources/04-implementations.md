# Implementations (Adapters)

Implementations are the classes that the AI agent writes. Sentinel generates **stubs** with
`UnsupportedOperationException` — the agent must fill in the logic to satisfy the generated tests.

## Key Rules

1. **Implement exactly the interfaces** listed in `implements` — no more, no less
2. **Use constructor injection** from `requiresInject` — never use `new` for dependencies
3. **Apply framework annotations** based on `types` (e.g., `Repository` -> `@Repository`)
4. **Make all generated tests pass** — the tests define the behavioral contract
5. **Do NOT modify generated test files** — they are owned by Sentinel

## Framework Type Mapping

| Sentinel Type | Spring Boot | Jakarta EE |
|---------------|-------------|------------|
| `Component` | `@Component` | `@Named` |
| `Service` | `@Service` | `@Stateless` |
| `Repository` | `@Repository` | `@Repository` |
| `RestController` | `@RestController` | `@Path` |
| `UseCase` | *(none)* | *(none)* |

## Smart Merge Behavior

When you run `sentinel generate` after modifying an implementation:
- If the file has `@Generated` → fully regenerated (stub)
- If the file has NO `@Generated` → **smart merge**: new methods are added as stubs, existing methods preserved
- The generator never overwrites your implementation code once you start coding

## Declared Implementations

### Module: audit-domain

#### IAuditEngine

**Package:** `com.learney.contentaudit.auditdomain`

**Implements:** AuditEngine

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `scoreAggregator` | `ScoreAggregator` |
| `analyzerCatalog` | `AnalyzerCatalog` |
| `findingCollector` | `FindingCollector` |
| `contextNumbersCalculator` | `ContextNumbersCalculator` |

**Generated constructor:**
```java
public IAuditEngine(ScoreAggregator scoreAggregator, AnalyzerCatalog analyzerCatalog, FindingCollector findingCollector, ContextNumbersCalculator contextNumbersCalculator) {
    this.scoreAggregator = scoreAggregator;
    this.analyzerCatalog = analyzerCatalog;
    this.findingCollector = findingCollector;
    this.contextNumbersCalculator = contextNumbersCalculator;
}
```

#### KnowledgeTitleLengthAnalyzer

**Package:** `com.learney.contentaudit.auditdomain`

**Implements:** ContentAnalyzer

**Tests that must pass:** 4

- should score 0.5 for title of weighted length 28.5 [FEAT-KTLEN/F-KTLEN-R003]
- should score 0.0 for title of weighted length 29 [FEAT-KTLEN/F-KTLEN-R003]
- should score 0.0 for title of weighted length 35 [FEAT-KTLEN/F-KTLEN-R003]
- should score 0.0 for title well beyond limit at weighted length 70 [FEAT-KTLEN/F-KTLEN-R003]

#### KnowledgeInstructionsLengthAnalyzer

**Package:** `com.learney.contentaudit.auditdomain`

**Implements:** ContentAnalyzer

**Tests that must pass:** 8

- should score 1.0 for instructions exactly at soft limit of 70 weighted chars [FEAT-KTLEN/F-KTLEN-R005]
- should score 1.0 for instructions of 30 weighted chars within soft limit [FEAT-KTLEN/F-KTLEN-R006]
- should score 0.5 for instructions of 71 weighted chars just above soft limit [FEAT-KTLEN/F-KTLEN-R005]
- should score 0.5 for instructions exactly at hard limit of 100 weighted chars [FEAT-KTLEN/F-KTLEN-R005]
- should score 0.5 for instructions of 85 weighted chars between soft and hard limits [FEAT-KTLEN/F-KTLEN-R006]
- should score 0.0 for instructions of 101 weighted chars just above hard limit [FEAT-KTLEN/F-KTLEN-R005]
- should score 0.0 for instructions of 200 weighted chars well above hard limit [FEAT-KTLEN/F-KTLEN-R006]
- should use weighted character length not plain string length for scoring instructions [FEAT-KTLEN/F-KTLEN-R005]

#### SentenceLengthAnalyzer

**Package:** `com.learney.contentaudit.auditdomain`

**Implements:** ContentAnalyzer

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `nlpTokenizer` | `NlpTokenizer` |
| `config` | `SentenceLengthConfig` |

**Generated constructor:**
```java
public SentenceLengthAnalyzer(NlpTokenizer nlpTokenizer, SentenceLengthConfig config) {
    this.nlpTokenizer = nlpTokenizer;
    this.config = config;
}
```

#### IScoreAggregator

**Package:** `com.learney.contentaudit.auditdomain`

**Implements:** ScoreAggregator

#### SentenceLengthAnalyzerProvider

**Package:** `com.learney.contentaudit.auditdomain`

**Implements:** AnalyzerProvider

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `nlpTokenizer` | `NlpTokenizer` |
| `config` | `SentenceLengthConfig` |

**Generated constructor:**
```java
public SentenceLengthAnalyzerProvider(NlpTokenizer nlpTokenizer, SentenceLengthConfig config) {
    this.nlpTokenizer = nlpTokenizer;
    this.config = config;
}
```

#### KnowledgeTitleLengthAnalyzerProvider

**Package:** `com.learney.contentaudit.auditdomain`

**Implements:** AnalyzerProvider

#### KnowledgeInstructionsLengthAnalyzerProvider

**Package:** `com.learney.contentaudit.auditdomain`

**Implements:** AnalyzerProvider

#### CocaBucketsAnalyzer (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Implements:** ContentAnalyzer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `nlpTokenizer` | `NlpTokenizer` |
| `cocaBucketsConfig` | `CocaBucketsConfig` |
| `tokenClassifier` | `TokenClassifier` |
| `progressionEvaluator` | `ProgressionEvaluator` |
| `improvementPlanner` | `ImprovementPlanner` |

#### CocaTokenAccumulationAggregator (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Implements:** ScoreAggregator

#### DefaultTokenClassifier (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Implements:** TokenClassifier

#### DefaultProgressionEvaluator (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Implements:** ProgressionEvaluator

#### DefaultImprovementPlanner (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Implements:** ImprovementPlanner

#### CocaBucketsAnalyzerProvider (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Implements:** AnalyzerProvider

**Constructor dependencies:**

| Name | Type |
|------|------|
| `nlpTokenizer` | `NlpTokenizer` |
| `cocaBucketsConfig` | `CocaBucketsConfig` |

#### LemmaRecurrenceAnalyzer (package: lrec)

**Package:** `com.learney.contentaudit.auditdomain.lrec`
**Visibility:** internal
**Implements:** ContentAnalyzer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `contentWordFilter` | `ContentWordFilter` |
| `lemmaRecurrenceConfig` | `LemmaRecurrenceConfig` |
| `intervalCalculator` | `IntervalCalculator` |
| `exposureClassifier` | `ExposureClassifier` |

#### DefaultContentWordFilter (package: lrec)

**Package:** `com.learney.contentaudit.auditdomain.lrec`
**Visibility:** internal
**Implements:** ContentWordFilter

**Tests:** 6

#### DefaultIntervalCalculator (package: lrec)

**Package:** `com.learney.contentaudit.auditdomain.lrec`
**Visibility:** internal
**Implements:** IntervalCalculator

**Tests:** 4

#### DefaultExposureClassifier (package: lrec)

**Package:** `com.learney.contentaudit.auditdomain.lrec`
**Visibility:** internal
**Implements:** ExposureClassifier

#### LemmaRecurrenceAnalyzerProvider (package: lrec)

**Package:** `com.learney.contentaudit.auditdomain.lrec`
**Visibility:** internal
**Implements:** AnalyzerProvider

**Constructor dependencies:**

| Name | Type |
|------|------|
| `lemmaRecurrenceConfig` | `LemmaRecurrenceConfig` |

#### LemmaByLevelAbsenceAnalyzer (package: labs)

**Package:** `com.learney.contentaudit.auditdomain.labs`
**Visibility:** internal
**Implements:** ContentAnalyzer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `evpCatalogPort` | `EvpCatalogPort` |
| `contentWordFilter` | `ContentWordFilter` |
| `lemmaAbsenceConfig` | `LemmaAbsenceConfig` |
| `sentenceLexicalScorer` | `SentenceLexicalScorer` |

#### LemmaAbsenceScoreAggregator (package: labs)

**Package:** `com.learney.contentaudit.auditdomain.labs`
**Visibility:** internal
**Implements:** ScoreAggregator

#### DefaultSentenceLexicalScorer (package: labs)

**Package:** `com.learney.contentaudit.auditdomain.labs`
**Visibility:** internal
**Implements:** SentenceLexicalScorer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `evpCatalogPort` | `EvpCatalogPort` |
| `contentWordFilter` | `ContentWordFilter` |
| `lemmaAbsenceConfig` | `LemmaAbsenceConfig` |

#### LemmaAbsenceAnalyzerProvider (package: labs)

**Package:** `com.learney.contentaudit.auditdomain.labs`
**Visibility:** internal
**Implements:** AnalyzerProvider

**Constructor dependencies:**

| Name | Type |
|------|------|
| `evpCatalogPort` | `EvpCatalogPort` |
| `lemmaAbsenceConfig` | `LemmaAbsenceConfig` |
| `sentenceLexicalScorer` | `SentenceLexicalScorer` |

#### DefaultAuditNodeIndexFactory (package: auditnodeindex)

**Package:** `com.learney.contentaudit.auditdomain.auditnodeindex`
**Visibility:** internal
**Implements:** AuditNodeIndexFactory

#### MapAuditNodeIndex (package: auditnodeindex)

**Package:** `com.learney.contentaudit.auditdomain.auditnodeindex`
**Visibility:** internal
**Implements:** AuditNodeIndex

#### EvpThenNlpLemmaCefrLevelResolver (package: lemmacount)

**Package:** `com.learney.contentaudit.auditdomain.lemmacount`
**Visibility:** internal
**Implements:** LemmaCefrLevelResolver

**Constructor dependencies:**

| Name | Type |
|------|------|
| `evpCatalogPort` | `EvpCatalogPort` |

#### LemmaCountAnalyzer (package: lemmacount)

**Package:** `com.learney.contentaudit.auditdomain.lemmacount`
**Visibility:** internal
**Implements:** ContentAnalyzer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `contentWordFilter` | `ContentWordFilter` |
| `lemmaCefrLevelResolver` | `LemmaCefrLevelResolver` |
| `lemmaCountConfig` | `LemmaCountConfig` |

#### LemmaCountAnalyzerProvider (package: lemmacount)

**Package:** `com.learney.contentaudit.auditdomain.lemmacount`
**Visibility:** internal
**Implements:** AnalyzerProvider

**Constructor dependencies:**

| Name | Type |
|------|------|
| `evpCatalogPort` | `EvpCatalogPort` |
| `lemmaCountConfig` | `LemmaCountConfig` |

#### DefaultSentenceLexicalEvaluator (package: lexicalflags)

**Package:** `com.learney.contentaudit.auditdomain.lexicalflags`
**Visibility:** public
**Implements:** SentenceLexicalEvaluator

**Constructor dependencies:**

| Name | Type |
|------|------|
| `sentenceLexicalScorer` | `SentenceLexicalScorer` |

#### DefaultQuizInstructionAnalyzerFactory (package: quizinstructionengine)

**Package:** `com.learney.contentaudit.auditdomain.quizinstructionengine`
**Visibility:** public
**Implements:** AnalyzerProvider

**Constructor dependencies:**

| Name | Type |
|------|------|
| `evaluator` | `Evaluator` |
| `verdictReader` | `QuizInstructionVerdictReader` |
| `config` | `QuizInstructionConfig` |
| `runtime` | `JudgeRuntime` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |

#### QuizInstructionAnalyzer (package: quizinstructionengine)

**Package:** `com.learney.contentaudit.auditdomain.quizinstructionengine`
**Visibility:** public
**Implements:** ContentAnalyzer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `session` | `EvaluationSession` |
| `subjectBuilder` | `QuizInstructionSubjectBuilder` |
| `scorer` | `QuizInstructionScorer` |
| `scopeMatcher` | `QuizInstructionScopeMatcher` |
| `verdictReader` | `QuizInstructionVerdictReader` |
| `policy` | `EvaluationRunPolicy` |
| `opened` | `JudgeSessions` |
| `runtime` | `JudgeRuntime` |

#### DefaultQuizInstructionSubjectBuilder (package: quizinstructionengine)

**Package:** `com.learney.contentaudit.auditdomain.quizinstructionengine`
**Visibility:** public
**Implements:** QuizInstructionSubjectBuilder

**Constructor dependencies:**

| Name | Type |
|------|------|
| `studentViewRenderer` | `QuizStudentViewRenderer` |

#### DefaultQuizInstructionScorer (package: quizinstructionengine)

**Package:** `com.learney.contentaudit.auditdomain.quizinstructionengine`
**Visibility:** public
**Implements:** QuizInstructionScorer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `config` | `QuizInstructionConfig` |

#### DefaultQuizInstructionScopeMatcher (package: quizinstructionengine)

**Package:** `com.learney.contentaudit.auditdomain.quizinstructionengine`
**Visibility:** public
**Implements:** QuizInstructionScopeMatcher

#### DefaultQuizInstructionComplianceCheckerFactory (package: quizinstructionengine)

**Package:** `com.learney.contentaudit.auditdomain.quizinstructionengine`
**Visibility:** public
**Implements:** QuizInstructionComplianceCheckerFactory

**Constructor dependencies:**

| Name | Type |
|------|------|
| `studentViewRenderer` | `QuizStudentViewRenderer` |

#### DefaultQuizInstructionSubjectViewFactory (package: quizinstructionengine)

**Package:** `com.learney.contentaudit.auditdomain.quizinstructionengine`
**Visibility:** public
**Implements:** QuizInstructionSubjectViewFactory

#### LedgerBackedQuizInstructionComplianceChecker (package: quizinstructionengine)

**Package:** `com.learney.contentaudit.auditdomain.quizinstructionengine`
**Visibility:** public
**Implements:** QuizInstructionComplianceChecker

**Constructor dependencies:**

| Name | Type |
|------|------|
| `session` | `EvaluationSession` |
| `subjectBuilder` | `QuizInstructionSubjectBuilder` |
| `verdictReader` | `QuizInstructionVerdictReader` |

#### DefaultAnalyzerCatalog (package: findingengine)

**Package:** `com.learney.contentaudit.auditdomain.findingengine`
**Visibility:** public
**Implements:** AnalyzerCatalog

**Constructor dependencies:**

| Name | Type |
|------|------|
| `providers` | `List<AnalyzerProvider>` |

#### DefaultFindingCollector (package: findingengine)

**Package:** `com.learney.contentaudit.auditdomain.findingengine`
**Visibility:** public
**Implements:** FindingCollector

#### DefaultContextNumbersCalculator (package: findingengine)

**Package:** `com.learney.contentaudit.auditdomain.findingengine`
**Visibility:** public
**Implements:** ContextNumbersCalculator

#### DefaultQuizStudentViewRenderer (package: studentview)

**Package:** `com.learney.contentaudit.auditdomain.studentview`
**Visibility:** public
**Implements:** QuizStudentViewRenderer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `quizSentenceConverter` | `QuizSentenceConverter` |

#### DefaultTopicStudentViewQuery (package: studentview)

**Package:** `com.learney.contentaudit.auditdomain.studentview`
**Visibility:** public
**Implements:** TopicStudentViewQuery

**Constructor dependencies:**

| Name | Type |
|------|------|
| `studentViewRenderer` | `QuizStudentViewRenderer` |
| `recordings` | `AudioRecordingRegistry` |

#### HintCoherenceAnalyzerProvider (package: hintcoherence)

**Package:** `com.learney.contentaudit.auditdomain.hintcoherence`
**Visibility:** public
**Implements:** AnalyzerProvider

**Constructor dependencies:**

| Name | Type |
|------|------|
| `irregularForms` | `IrregularFormsCatalog` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |

#### HintCoherenceAnalyzer (package: hintcoherence)

**Package:** `com.learney.contentaudit.auditdomain.hintcoherence`
**Visibility:** public
**Implements:** ContentAnalyzer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `hintReader` | `HintReader` |
| `answerCheck` | `HintAnswerCheck` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |

#### DefaultHintReader (package: hintcoherence)

**Package:** `com.learney.contentaudit.auditdomain.hintcoherence`
**Visibility:** public
**Implements:** HintReader

#### DefaultHintAnswerCheck (package: hintcoherence)

**Package:** `com.learney.contentaudit.auditdomain.hintcoherence`
**Visibility:** public
**Implements:** HintAnswerCheck

**Constructor dependencies:**

| Name | Type |
|------|------|
| `irregularForms` | `IrregularFormsCatalog` |

#### HintCoherenceCorrection (package: hintcoherence)

**Package:** `com.learney.contentaudit.auditdomain.hintcoherence`
**Visibility:** public
**Implements:** AnalyzerCorrection

#### HintCoherenceRuleFix (package: hintcoherence)

**Package:** `com.learney.contentaudit.auditdomain.hintcoherence`
**Visibility:** public
**Implements:** RuleFix

**Constructor dependencies:**

| Name | Type |
|------|------|
| `irregularForms` | `IrregularFormsCatalog` |

#### DefaultCorrectorMatcher (package: correctormatch)

**Package:** `com.learney.contentaudit.auditdomain.correctormatch`
**Visibility:** public
**Implements:** CorrectorMatcher

**Constructor dependencies:**

| Name | Type |
|------|------|
| `table` | `FormEquivalenceTable` |

#### AnswerCompletenessAnalyzerProvider (package: answercompleteness)

**Package:** `com.learney.contentaudit.auditdomain.answercompleteness`
**Visibility:** public
**Implements:** AnalyzerProvider

**Constructor dependencies:**

| Name | Type |
|------|------|
| `correctorMatcher` | `CorrectorMatcher` |
| `irregularForms` | `IrregularFormsCatalog` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |

#### AnswerCompletenessAnalyzer (package: answercompleteness)

**Package:** `com.learney.contentaudit.auditdomain.answercompleteness`
**Visibility:** public
**Implements:** ContentAnalyzer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `correctorMatcher` | `CorrectorMatcher` |
| `formGenerator` | `EquivalentFormGenerator` |
| `requestDetector` | `FormRequestDetector` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |

#### DefaultEquivalentFormGenerator (package: answercompleteness)

**Package:** `com.learney.contentaudit.auditdomain.answercompleteness`
**Visibility:** public
**Implements:** EquivalentFormGenerator

**Constructor dependencies:**

| Name | Type |
|------|------|
| `irregularForms` | `IrregularFormsCatalog` |

#### DefaultFormRequestDetector (package: answercompleteness)

**Package:** `com.learney.contentaudit.auditdomain.answercompleteness`
**Visibility:** public
**Implements:** FormRequestDetector

#### AnswerCompletenessCorrection (package: answercompleteness)

**Package:** `com.learney.contentaudit.auditdomain.answercompleteness`
**Visibility:** public
**Implements:** AnalyzerCorrection

#### AnswerCompletenessRuleFix (package: answercompleteness)

**Package:** `com.learney.contentaudit.auditdomain.answercompleteness`
**Visibility:** public
**Implements:** RuleFix

**Constructor dependencies:**

| Name | Type |
|------|------|
| `correctorMatcher` | `CorrectorMatcher` |

#### MultipleChoiceValidityAnalyzerProvider (package: multiplechoicevalidity)

**Package:** `com.learney.contentaudit.auditdomain.multiplechoicevalidity`
**Visibility:** public
**Implements:** AnalyzerProvider

**Constructor dependencies:**

| Name | Type |
|------|------|
| `correctorMatcher` | `CorrectorMatcher` |
| `judgeRunFactory` | `ChoiceAlternativeJudgeRunFactory` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |

#### MultipleChoiceValidityAnalyzer (package: multiplechoicevalidity)

**Package:** `com.learney.contentaudit.auditdomain.multiplechoicevalidity`
**Visibility:** public
**Implements:** ContentAnalyzer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `optionRules` | `OptionRuleCheck` |
| `judgeRun` | `ChoiceAlternativeJudgeRun` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |

#### DefaultOptionRuleCheck (package: multiplechoicevalidity)

**Package:** `com.learney.contentaudit.auditdomain.multiplechoicevalidity`
**Visibility:** public
**Implements:** OptionRuleCheck

**Constructor dependencies:**

| Name | Type |
|------|------|
| `correctorMatcher` | `CorrectorMatcher` |

#### UnavailableChoiceAlternativeJudgeRunFactory (package: multiplechoicevalidity)

**Package:** `com.learney.contentaudit.auditdomain.multiplechoicevalidity`
**Visibility:** public
**Implements:** ChoiceAlternativeJudgeRunFactory

#### MultipleChoiceValidityCorrection (package: multiplechoicevalidity)

**Package:** `com.learney.contentaudit.auditdomain.multiplechoicevalidity`
**Visibility:** public
**Implements:** AnalyzerCorrection

#### MultipleChoiceValidityRuleFix (package: multiplechoicevalidity)

**Package:** `com.learney.contentaudit.auditdomain.multiplechoicevalidity`
**Visibility:** public
**Implements:** RuleFix

#### DataCoherenceAnalyzerProvider (package: datacoherence)

**Package:** `com.learney.contentaudit.auditdomain.datacoherence`
**Visibility:** public
**Implements:** AnalyzerProvider

**Constructor dependencies:**

| Name | Type |
|------|------|
| `correctorMatcher` | `CorrectorMatcher` |
| `recordings` | `AudioRecordingRegistry` |
| `productionSentences` | `ProductionSentenceSource` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |

#### DataCoherenceAnalyzer (package: datacoherence)

**Package:** `com.learney.contentaudit.auditdomain.datacoherence`
**Visibility:** public
**Implements:** ContentAnalyzer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `correctorMatcher` | `CorrectorMatcher` |
| `recordings` | `AudioRecordingRegistry` |
| `productionSentences` | `ProductionSentenceSource` |
| `spokenTextBuilder` | `SpokenTextBuilder` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |

#### DefaultSpokenTextBuilder (package: datacoherence)

**Package:** `com.learney.contentaudit.auditdomain.datacoherence`
**Visibility:** public
**Implements:** SpokenTextBuilder

#### DataCoherenceCorrection (package: datacoherence)

**Package:** `com.learney.contentaudit.auditdomain.datacoherence`
**Visibility:** public
**Implements:** AnalyzerCorrection

#### DataCoherenceRuleFix (package: datacoherence)

**Package:** `com.learney.contentaudit.auditdomain.datacoherence`
**Visibility:** public
**Implements:** RuleFix

**Constructor dependencies:**

| Name | Type |
|------|------|
| `quizSentenceConverter` | `QuizSentenceConverter` |

#### ExactDuplicatesAnalyzerProvider (package: exactduplicates)

**Package:** `com.learney.contentaudit.auditdomain.exactduplicates`
**Visibility:** public
**Implements:** AnalyzerProvider

**Constructor dependencies:**

| Name | Type |
|------|------|
| `correctorMatcher` | `CorrectorMatcher` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |

#### ExactDuplicatesAnalyzer (package: exactduplicates)

**Package:** `com.learney.contentaudit.auditdomain.exactduplicates`
**Visibility:** public
**Implements:** ContentAnalyzer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `correctorMatcher` | `CorrectorMatcher` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |

#### ExactDuplicatesCorrection (package: exactduplicates)

**Package:** `com.learney.contentaudit.auditdomain.exactduplicates`
**Visibility:** public
**Implements:** AnalyzerCorrection

#### DefaultJudgeRuntime (package: judgingengine)

**Package:** `com.learney.contentaudit.auditdomain.judgingengine`
**Visibility:** public
**Implements:** JudgeRuntime

**Constructor dependencies:**

| Name | Type |
|------|------|
| `courseSessions` | `EvaluationSessionFactory` |
| `examSessions` | `EvaluationSessionFactory` |
| `examRegistry` | `JudgeExamRegistry` |
| `timeline` | `EvaluationTimeline` |
| `config` | `JudgeRuntimeConfig` |

#### DefaultJudgeExamVerdictExporter (package: judgingengine)

**Package:** `com.learney.contentaudit.auditdomain.judgingengine`
**Visibility:** public
**Implements:** JudgeExamVerdictExporter

#### TranslationSenseAnalyzerProvider (package: translationsenseengine)

**Package:** `com.learney.contentaudit.auditdomain.translationsenseengine`
**Visibility:** public
**Implements:** AnalyzerProvider

**Constructor dependencies:**

| Name | Type |
|------|------|
| `runtime` | `JudgeRuntime` |
| `fromEnglishJudge` | `Evaluator` |
| `fromTranslationJudge` | `Evaluator` |
| `verdictReader` | `TranslationSenseVerdictReader` |
| `config` | `TranslationSenseConfig` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |

#### TranslationSenseAnalyzer (package: translationsenseengine)

**Package:** `com.learney.contentaudit.auditdomain.translationsenseengine`
**Visibility:** public
**Implements:** ContentAnalyzer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `opened` | `JudgeSessions` |
| `runtime` | `JudgeRuntime` |
| `subjectBuilder` | `TranslationSenseSubjectBuilder` |
| `combiner` | `TranslationSenseCombiner` |
| `verdictReader` | `TranslationSenseVerdictReader` |

#### DefaultTranslationSenseSubjectBuilder (package: translationsenseengine)

**Package:** `com.learney.contentaudit.auditdomain.translationsenseengine`
**Visibility:** public
**Implements:** TranslationSenseSubjectBuilder

**Constructor dependencies:**

| Name | Type |
|------|------|
| `studentViewRenderer` | `QuizStudentViewRenderer` |

#### DefaultTranslationSenseCombiner (package: translationsenseengine)

**Package:** `com.learney.contentaudit.auditdomain.translationsenseengine`
**Visibility:** public
**Implements:** TranslationSenseCombiner

**Constructor dependencies:**

| Name | Type |
|------|------|
| `config` | `TranslationSenseConfig` |

#### TranslationSenseCorrection (package: translationsenseengine)

**Package:** `com.learney.contentaudit.auditdomain.translationsenseengine`
**Visibility:** public
**Implements:** AnalyzerCorrection

#### SingleAnswerAnalyzerProvider (package: singleanswerengine)

**Package:** `com.learney.contentaudit.auditdomain.singleanswerengine`
**Visibility:** public
**Implements:** AnalyzerProvider

**Constructor dependencies:**

| Name | Type |
|------|------|
| `runtime` | `JudgeRuntime` |
| `judge` | `Evaluator` |
| `verdictReader` | `SingleAnswerVerdictReader` |
| `config` | `SingleAnswerConfig` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |
| `correctorMatcher` | `CorrectorMatcher` |

#### SingleAnswerAnalyzer (package: singleanswerengine)

**Package:** `com.learney.contentaudit.auditdomain.singleanswerengine`
**Visibility:** public
**Implements:** ContentAnalyzer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `opened` | `JudgeSessions` |
| `runtime` | `JudgeRuntime` |
| `subjectBuilder` | `SingleAnswerSubjectBuilder` |
| `classifier` | `SingleAnswerClassifier` |
| `verdictReader` | `SingleAnswerVerdictReader` |

#### DefaultSingleAnswerSubjectBuilder (package: singleanswerengine)

**Package:** `com.learney.contentaudit.auditdomain.singleanswerengine`
**Visibility:** public
**Implements:** SingleAnswerSubjectBuilder

**Constructor dependencies:**

| Name | Type |
|------|------|
| `studentViewRenderer` | `QuizStudentViewRenderer` |

#### DefaultSingleAnswerClassifier (package: singleanswerengine)

**Package:** `com.learney.contentaudit.auditdomain.singleanswerengine`
**Visibility:** public
**Implements:** SingleAnswerClassifier

**Constructor dependencies:**

| Name | Type |
|------|------|
| `correctorMatcher` | `CorrectorMatcher` |
| `config` | `SingleAnswerConfig` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |

#### SingleAnswerCorrection (package: singleanswerengine)

**Package:** `com.learney.contentaudit.auditdomain.singleanswerengine`
**Visibility:** public
**Implements:** AnalyzerCorrection

#### MiniTheoryAnalyzerProvider (package: minitheoryengine)

**Package:** `com.learney.contentaudit.auditdomain.minitheoryengine`
**Visibility:** public
**Implements:** AnalyzerProvider

**Constructor dependencies:**

| Name | Type |
|------|------|
| `runtime` | `JudgeRuntime` |
| `judge` | `Evaluator` |
| `verdictReader` | `MiniTheoryVerdictReader` |
| `config` | `MiniTheoryConfig` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |

#### MiniTheoryAnalyzer (package: minitheoryengine)

**Package:** `com.learney.contentaudit.auditdomain.minitheoryengine`
**Visibility:** public
**Implements:** ContentAnalyzer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `opened` | `JudgeSessions` |
| `runtime` | `JudgeRuntime` |
| `subjectBuilder` | `MiniTheorySubjectBuilder` |
| `distributor` | `MiniTheoryDistributor` |
| `verdictReader` | `MiniTheoryVerdictReader` |

#### DefaultMiniTheorySubjectBuilder (package: minitheoryengine)

**Package:** `com.learney.contentaudit.auditdomain.minitheoryengine`
**Visibility:** public
**Implements:** MiniTheorySubjectBuilder

**Constructor dependencies:**

| Name | Type |
|------|------|
| `studentViewRenderer` | `QuizStudentViewRenderer` |

#### DefaultMiniTheoryDistributor (package: minitheoryengine)

**Package:** `com.learney.contentaudit.auditdomain.minitheoryengine`
**Visibility:** public
**Implements:** MiniTheoryDistributor

**Constructor dependencies:**

| Name | Type |
|------|------|
| `config` | `MiniTheoryConfig` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |

#### MiniTheoryCorrection (package: minitheoryengine)

**Package:** `com.learney.contentaudit.auditdomain.minitheoryengine`
**Visibility:** public
**Implements:** AnalyzerCorrection

#### DefaultChoiceAlternativeJudgeRunFactory (package: choicealternativeengine)

**Package:** `com.learney.contentaudit.auditdomain.choicealternativeengine`
**Visibility:** public
**Implements:** ChoiceAlternativeJudgeRunFactory

**Constructor dependencies:**

| Name | Type |
|------|------|
| `hostAnalyzerName` | `String` |
| `runtime` | `JudgeRuntime` |
| `judge` | `Evaluator` |
| `verdictReader` | `ChoiceAlternativeVerdictReader` |
| `config` | `ChoiceAlternativeConfig` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |
| `correctorMatcher` | `CorrectorMatcher` |

#### LedgerBackedChoiceAlternativeJudgeRun (package: choicealternativeengine)

**Package:** `com.learney.contentaudit.auditdomain.choicealternativeengine`
**Visibility:** public
**Implements:** ChoiceAlternativeJudgeRun

**Constructor dependencies:**

| Name | Type |
|------|------|
| `opened` | `JudgeSessions` |
| `runtime` | `JudgeRuntime` |
| `subjectBuilder` | `ChoiceAlternativeSubjectBuilder` |
| `classifier` | `ChoiceAlternativeClassifier` |
| `verdictReader` | `ChoiceAlternativeVerdictReader` |

#### DefaultChoiceAlternativeSubjectBuilder (package: choicealternativeengine)

**Package:** `com.learney.contentaudit.auditdomain.choicealternativeengine`
**Visibility:** public
**Implements:** ChoiceAlternativeSubjectBuilder

**Constructor dependencies:**

| Name | Type |
|------|------|
| `studentViewRenderer` | `QuizStudentViewRenderer` |

#### DefaultChoiceAlternativeClassifier (package: choicealternativeengine)

**Package:** `com.learney.contentaudit.auditdomain.choicealternativeengine`
**Visibility:** public
**Implements:** ChoiceAlternativeClassifier

**Constructor dependencies:**

| Name | Type |
|------|------|
| `correctorMatcher` | `CorrectorMatcher` |
| `config` | `ChoiceAlternativeConfig` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |

#### DefaultTopicTeachingResolver (package: offlevelwords)

**Package:** `com.learney.contentaudit.auditdomain.offlevelwords`
**Visibility:** public
**Implements:** TopicTeachingResolver

#### OffLevelWordsAnalyzerProvider (package: offlevelwords)

**Package:** `com.learney.contentaudit.auditdomain.offlevelwords`
**Visibility:** public
**Implements:** AnalyzerProvider

**Constructor dependencies:**

| Name | Type |
|------|------|
| `sentenceLexicalScorer` | `SentenceLexicalScorer` |
| `lemmaAbsenceConfig` | `LemmaAbsenceConfig` |
| `topicTeachingResolver` | `TopicTeachingResolver` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |
| `evpCatalogPort` | `EvpCatalogPort` |

#### OffLevelWordsAnalyzer (package: offlevelwords)

**Package:** `com.learney.contentaudit.auditdomain.offlevelwords`
**Visibility:** public
**Implements:** ContentAnalyzer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `sentenceLexicalScorer` | `SentenceLexicalScorer` |
| `lemmaAbsenceConfig` | `LemmaAbsenceConfig` |
| `topicTeachingResolver` | `TopicTeachingResolver` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |
| `evpCatalogPort` | `EvpCatalogPort` |

#### OffLevelWordsCorrection (package: offlevelwords)

**Package:** `com.learney.contentaudit.auditdomain.offlevelwords`
**Visibility:** public
**Implements:** AnalyzerCorrection

#### DefaultLocalToolGate (package: localtoolsengine)

**Package:** `com.learney.contentaudit.auditdomain.localtoolsengine`
**Visibility:** public
**Implements:** LocalToolGate

**Constructor dependencies:**

| Name | Type |
|------|------|
| `judgeRuntime` | `JudgeRuntime` |

#### DefaultFinishedSentenceReader (package: finishedsentence)

**Package:** `com.learney.contentaudit.auditdomain.finishedsentence`
**Visibility:** public
**Implements:** FinishedSentenceReader

#### GrammarAnalyzerProvider (package: grammar)

**Package:** `com.learney.contentaudit.auditdomain.grammar`
**Visibility:** public
**Implements:** AnalyzerProvider

**Constructor dependencies:**

| Name | Type |
|------|------|
| `grammarChecker` | `GrammarChecker` |
| `sentenceReader` | `FinishedSentenceReader` |
| `localToolGate` | `LocalToolGate` |

#### GrammarAnalyzer (package: grammar)

**Package:** `com.learney.contentaudit.auditdomain.grammar`
**Visibility:** public
**Implements:** ContentAnalyzer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `grammarChecker` | `GrammarChecker` |
| `sentenceReader` | `FinishedSentenceReader` |
| `matchFilter` | `GrammarMatchFilter` |
| `finalPunctuation` | `FinalPunctuationCheck` |
| `issueClassifier` | `GrammarIssueClassifier` |
| `gateDecision` | `LocalToolGateDecision` |

#### DefaultGrammarMatchFilter (package: grammar)

**Package:** `com.learney.contentaudit.auditdomain.grammar`
**Visibility:** public
**Implements:** GrammarMatchFilter

**Constructor dependencies:**

| Name | Type |
|------|------|
| `config` | `GrammarConfig` |

#### DefaultFinalPunctuationCheck (package: grammar)

**Package:** `com.learney.contentaudit.auditdomain.grammar`
**Visibility:** public
**Implements:** FinalPunctuationCheck

#### DefaultGrammarIssueClassifier (package: grammar)

**Package:** `com.learney.contentaudit.auditdomain.grammar`
**Visibility:** public
**Implements:** GrammarIssueClassifier

#### DefaultGrammarConfig (package: grammar)

**Package:** `com.learney.contentaudit.auditdomain.grammar`
**Visibility:** public
**Implements:** GrammarConfig

#### GrammarCorrection (package: grammar)

**Package:** `com.learney.contentaudit.auditdomain.grammar`
**Visibility:** public
**Implements:** AnalyzerCorrection

#### GrammarRuleFix (package: grammar)

**Package:** `com.learney.contentaudit.auditdomain.grammar`
**Visibility:** public
**Implements:** RuleFix

#### NaturalnessAnalyzerProvider (package: naturalness)

**Package:** `com.learney.contentaudit.auditdomain.naturalness`
**Visibility:** public
**Implements:** AnalyzerProvider

**Constructor dependencies:**

| Name | Type |
|------|------|
| `yesNoModel` | `YesNoModel` |
| `alternativeJudge` | `Evaluator` |
| `judgeRuntime` | `JudgeRuntime` |
| `sentenceReader` | `FinishedSentenceReader` |
| `localToolGate` | `LocalToolGate` |

#### NaturalnessAnalyzer (package: naturalness)

**Package:** `com.learney.contentaudit.auditdomain.naturalness`
**Visibility:** public
**Implements:** ContentAnalyzer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `opened` | `JudgeSessions` |
| `runtime` | `JudgeRuntime` |
| `sentenceReader` | `FinishedSentenceReader` |
| `scorer` | `NaturalnessScorer` |
| `config` | `NaturalnessConfig` |
| `gateDecision` | `LocalToolGateDecision` |

#### NaturalnessEvaluator (package: naturalness)

**Package:** `com.learney.contentaudit.auditdomain.naturalness`
**Visibility:** public
**Implements:** Evaluator

**Constructor dependencies:**

| Name | Type |
|------|------|
| `yesNoModel` | `YesNoModel` |
| `config` | `NaturalnessConfig` |
| `examinedVersion` | `String` |

#### DefaultNaturalnessScorer (package: naturalness)

**Package:** `com.learney.contentaudit.auditdomain.naturalness`
**Visibility:** public
**Implements:** NaturalnessScorer

#### DefaultNaturalnessConfig (package: naturalness)

**Package:** `com.learney.contentaudit.auditdomain.naturalness`
**Visibility:** public
**Implements:** NaturalnessConfig

#### NearDuplicatesAnalyzerProvider (package: nearduplicates)

**Package:** `com.learney.contentaudit.auditdomain.nearduplicates`
**Visibility:** public
**Implements:** AnalyzerProvider

**Constructor dependencies:**

| Name | Type |
|------|------|
| `embeddingModel` | `EmbeddingModel` |
| `alternativeJudge` | `Evaluator` |
| `judgeRuntime` | `JudgeRuntime` |
| `correctorMatcher` | `CorrectorMatcher` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |
| `localToolGate` | `LocalToolGate` |

#### NearDuplicatesAnalyzer (package: nearduplicates)

**Package:** `com.learney.contentaudit.auditdomain.nearduplicates`
**Visibility:** public
**Implements:** ContentAnalyzer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `opened` | `JudgeSessions` |
| `runtime` | `JudgeRuntime` |
| `rules` | `NearDuplicateRules` |
| `question` | `SameExerciseQuestion` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |
| `config` | `NearDuplicatesConfig` |
| `gateDecision` | `LocalToolGateDecision` |

#### SentenceEmbeddingEvaluator (package: nearduplicates)

**Package:** `com.learney.contentaudit.auditdomain.nearduplicates`
**Visibility:** public
**Implements:** Evaluator

**Constructor dependencies:**

| Name | Type |
|------|------|
| `embeddingModel` | `EmbeddingModel` |
| `config` | `NearDuplicatesConfig` |
| `examinedVersion` | `String` |

#### DefaultNearDuplicateRules (package: nearduplicates)

**Package:** `com.learney.contentaudit.auditdomain.nearduplicates`
**Visibility:** public
**Implements:** NearDuplicateRules

**Constructor dependencies:**

| Name | Type |
|------|------|
| `correctorMatcher` | `CorrectorMatcher` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |

#### DefaultNearDuplicatesConfig (package: nearduplicates)

**Package:** `com.learney.contentaudit.auditdomain.nearduplicates`
**Visibility:** public
**Implements:** NearDuplicatesConfig

#### DefaultSameExerciseQuestion (package: nearduplicates)

**Package:** `com.learney.contentaudit.auditdomain.nearduplicates`
**Visibility:** public
**Implements:** SameExerciseQuestion

**Constructor dependencies:**

| Name | Type |
|------|------|
| `studentViewRenderer` | `QuizStudentViewRenderer` |

#### NearDuplicatesCorrection (package: nearduplicates)

**Package:** `com.learney.contentaudit.auditdomain.nearduplicates`
**Visibility:** public
**Implements:** AnalyzerCorrection

#### DefaultAnalyzerCorrectionCatalog (package: correctionengine)

**Package:** `com.learney.contentaudit.auditdomain.correctionengine`
**Visibility:** public
**Implements:** AnalyzerCorrectionCatalog

**Constructor dependencies:**

| Name | Type |
|------|------|
| `corrections` | `List<AnalyzerCorrection>` |
| `analyzerCatalog` | `AnalyzerCatalog` |

#### QuizInstructionCorrection (package: correctionengine)

**Package:** `com.learney.contentaudit.auditdomain.correctionengine`
**Visibility:** public
**Implements:** AnalyzerCorrection

#### LemmaAbsenceCorrection (package: correctionengine)

**Package:** `com.learney.contentaudit.auditdomain.correctionengine`
**Visibility:** public
**Implements:** AnalyzerCorrection

### Module: course-domain

#### DefaultQuizSentenceConverter (package: quizsentenceengine)

**Package:** `com.learney.contentaudit.coursedomain.quizsentenceengine`
**Visibility:** internal
**Implements:** QuizSentenceConverter

**Constructor dependencies:**

| Name | Type |
|------|------|
| `serializer` | `QuizSentenceSerializer` |
| `parser` | `QuizSentenceParser` |
| `plainDeriver` | `PlainSentenceDeriver` |

#### QuizSentenceSerializer (package: quizsentenceengine)

**Package:** `com.learney.contentaudit.coursedomain.quizsentenceengine`
**Visibility:** internal
**Implements:** 

#### QuizSentenceParser (package: quizsentenceengine)

**Package:** `com.learney.contentaudit.coursedomain.quizsentenceengine`
**Visibility:** internal
**Implements:** 

#### PlainSentenceDeriver (package: quizsentenceengine)

**Package:** `com.learney.contentaudit.coursedomain.quizsentenceengine`
**Visibility:** internal
**Implements:** 

**Constructor dependencies:**

| Name | Type |
|------|------|
| `whitespaceNormalizer` | `WhitespaceNormalizer` |

#### WhitespaceNormalizer (package: quizsentenceengine)

**Package:** `com.learney.contentaudit.coursedomain.quizsentenceengine`
**Visibility:** internal
**Implements:** 

### Module: refiner-domain

#### SentenceLengthContextResolver

**Package:** `com.learney.contentaudit.refinerdomain`

**Implements:** CorrectionContextResolver

#### LemmaAbsenceContextResolver

**Package:** `com.learney.contentaudit.refinerdomain`

**Implements:** CorrectionContextResolver

#### DispatchingCorrectionContextResolver

**Package:** `com.learney.contentaudit.refinerdomain`

**Implements:** CorrectionContextResolver

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `sentenceLengthResolver` | `SentenceLengthContextResolver` |
| `lemmaAbsenceResolver` | `LemmaAbsenceContextResolver` |
| `knowledgeTitleResolver` | `KnowledgeTitleContextResolver` |
| `quizInstructionResolver` | `QuizInstructionContextResolver` |

**Generated constructor:**
```java
public DispatchingCorrectionContextResolver(SentenceLengthContextResolver sentenceLengthResolver, LemmaAbsenceContextResolver lemmaAbsenceResolver, KnowledgeTitleContextResolver knowledgeTitleResolver, QuizInstructionContextResolver quizInstructionResolver) {
    this.sentenceLengthResolver = sentenceLengthResolver;
    this.lemmaAbsenceResolver = lemmaAbsenceResolver;
    this.knowledgeTitleResolver = knowledgeTitleResolver;
    this.quizInstructionResolver = quizInstructionResolver;
}
```

#### DefaultRefinerEngine

**Package:** `com.learney.contentaudit.refinerdomain`

**Implements:** RefinerEngine

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `analyzerCatalog` | `AnalyzerCatalog` |
| `analyzerCorrectionCatalog` | `AnalyzerCorrectionCatalog` |

**Generated constructor:**
```java
public DefaultRefinerEngine(AnalyzerCatalog analyzerCatalog, AnalyzerCorrectionCatalog analyzerCorrectionCatalog) {
    this.analyzerCatalog = analyzerCatalog;
    this.analyzerCorrectionCatalog = analyzerCorrectionCatalog;
}
```

#### KnowledgeTitleContextResolver

**Package:** `com.learney.contentaudit.refinerdomain`

**Implements:** CorrectionContextResolver

#### QuizInstructionContextResolver

**Package:** `com.learney.contentaudit.refinerdomain`

**Implements:** CorrectionContextResolver

#### LemmaAbsenceContextSuggestedLemmaQueryPort (package: lemmasuggestion)

**Package:** `com.learney.contentaudit.refinerdomain.lemmasuggestion`
**Visibility:** internal
**Implements:** SuggestedLemmaQueryPort

**Constructor dependencies:**

| Name | Type |
|------|------|
| `lemmaAbsenceContextResolver` | `CorrectionContextResolver<LemmaAbsenceCorrectionContext>` |
| `underexposedLemmaInventory` | `LeveledLemmaInventory` |
| `levelWideLemmaInventory` | `LeveledLemmaInventory` |

#### BoundSuggestedLemmaQuerySession (package: lemmasuggestion)

**Package:** `com.learney.contentaudit.refinerdomain.lemmasuggestion`
**Visibility:** internal
**Implements:** SuggestedLemmaQuerySession

**Constructor dependencies:**

| Name | Type |
|------|------|
| `queryPort` | `SuggestedLemmaQueryPort` |
| `report` | `AuditReport` |
| `task` | `RefinementTask` |

#### DefaultSuggestedLemmaQuerySessionFactory (package: lemmasuggestion)

**Package:** `com.learney.contentaudit.refinerdomain.lemmasuggestion`
**Visibility:** internal
**Implements:** SuggestedLemmaQuerySessionFactory

**Constructor dependencies:**

| Name | Type |
|------|------|
| `queryPort` | `SuggestedLemmaQueryPort` |
| `auditReportStore` | `AuditReportStore` |

#### UnderexposedLemmaInventory (package: lemmasuggestion)

**Package:** `com.learney.contentaudit.refinerdomain.lemmasuggestion`
**Visibility:** internal
**Implements:** LeveledLemmaInventory

**Constructor dependencies:**

| Name | Type |
|------|------|
| `lemmaAbsenceContextResolver` | `CorrectionContextResolver<LemmaAbsenceCorrectionContext>` |

#### LevelWideLemmaInventory (package: lemmasuggestion)

**Package:** `com.learney.contentaudit.refinerdomain.lemmasuggestion`
**Visibility:** internal
**Implements:** LeveledLemmaInventory

**Constructor dependencies:**

| Name | Type |
|------|------|
| `evpCatalogPort` | `EvpCatalogPort` |
| `lemmaCountConfig` | `LemmaCountConfig` |

#### DefaultTopicWordLightsQuery (package: topicwords)

**Package:** `com.learney.contentaudit.refinerdomain.topicwords`
**Visibility:** public
**Implements:** TopicWordLightsQuery

**Constructor dependencies:**

| Name | Type |
|------|------|
| `sentenceLexicalEvaluator` | `SentenceLexicalEvaluator` |
| `suggestedLemmaQueryPort` | `SuggestedLemmaQueryPort` |
| `topicTeachingResolver` | `TopicTeachingResolver` |

#### DefaultErrorTaskBriefer (package: errortask)

**Package:** `com.learney.contentaudit.refinerdomain.errortask`
**Visibility:** public
**Implements:** ErrorTaskBriefer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `analyzerCorrectionCatalog` | `AnalyzerCorrectionCatalog` |
| `auditNodeIndexFactory` | `AuditNodeIndexFactory` |
| `quizStudentViewRenderer` | `QuizStudentViewRenderer` |

### Module: audit-application

#### CourseToAuditableMapper

**Package:** `com.learney.contentaudit.auditapplication`

**Implements:** CourseMapper

**Framework types:** Component

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `nlpTokenizer` | `NlpTokenizer` |
| `quizSentenceConverter` | `QuizSentenceConverter` |

**Generated constructor:**
```java
public CourseToAuditableMapper(NlpTokenizer nlpTokenizer, QuizSentenceConverter quizSentenceConverter) {
    this.nlpTokenizer = nlpTokenizer;
    this.quizSentenceConverter = quizSentenceConverter;
}
```

#### DefaultSentenceLengthConfig

**Package:** `com.learney.contentaudit.auditapplication`

**Implements:** SentenceLengthConfig

**Framework types:** Component

#### DefaultAuditRunner

**Package:** `com.learney.contentaudit.auditapplication`

**Implements:** AuditRunner

**Framework types:** Service

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `courseRepository` | `CourseRepository` |
| `courseToAuditableMapper` | `CourseToAuditableMapper` |
| `auditEngine` | `AuditEngine` |
| `analyzerCatalog` | `AnalyzerCatalog` |

**Generated constructor:**
```java
public DefaultAuditRunner(CourseRepository courseRepository, CourseToAuditableMapper courseToAuditableMapper, AuditEngine auditEngine, AnalyzerCatalog analyzerCatalog) {
    this.courseRepository = courseRepository;
    this.courseToAuditableMapper = courseToAuditableMapper;
    this.auditEngine = auditEngine;
    this.analyzerCatalog = analyzerCatalog;
}
```

#### DefaultCocaBucketsConfig

**Package:** `com.learney.contentaudit.auditapplication`

**Implements:** CocaBucketsConfig

**Framework types:** Component

#### DefaultLemmaRecurrenceConfig

**Package:** `com.learney.contentaudit.auditapplication`

**Implements:** LemmaRecurrenceConfig

**Framework types:** Component

#### DefaultLemmaAbsenceConfig

**Package:** `com.learney.contentaudit.auditapplication`

**Implements:** LemmaAbsenceConfig

**Framework types:** Component

**Tests that must pass:** 9

- should have alert thresholds non-decreasing from high to low priority [FEAT-LABS/F-LABS-R014]
- should enforce zero tolerance for high priority alert threshold [FEAT-LABS/F-LABS-R014]
- should enforce A1 zero tolerance with both absolute and percentage thresholds at zero [FEAT-LABS/F-LABS-R021]
- should have discount per level that limits max penalty to 0.3 for three-level distance [FEAT-LABS/F-LABS-R018]
- should return non-negative values for all thresholds and bounds [FEAT-LABS/F-LABS-R021]
- should return positive report limits for all priority levels [FEAT-LABS/F-LABS-R026]
- should return percentage thresholds between 0 and 100 for all levels [FEAT-LABS/F-LABS-R021]
- should return positive level weights for all CEFR levels [FEAT-LABS/F-LABS-R024]
- should return discount per level between 0 exclusive and 1 exclusive [FEAT-LABS/F-LABS-R018]

#### DefaultAnalyzerRegistry

**Package:** `com.learney.contentaudit.auditapplication`

**Implements:** AnalyzerRegistry

**Framework types:** Component

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `analyzerCatalog` | `AnalyzerCatalog` |

**Generated constructor:**
```java
public DefaultAnalyzerRegistry(AnalyzerCatalog analyzerCatalog) {
    this.analyzerCatalog = analyzerCatalog;
}
```

#### DefaultLemmaCountConfig

**Package:** `com.learney.contentaudit.auditapplication`

**Implements:** LemmaCountConfig

**Framework types:** Component

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `threshold` | `int` |
| `bands` | `List<LemmaCountBand>` |

**Generated constructor:**
```java
public DefaultLemmaCountConfig(int threshold, List<LemmaCountBand> bands) {
    this.threshold = threshold;
    this.bands = bands;
}
```

#### DefaultLemmaCountConfigLoader

**Package:** `com.learney.contentaudit.auditapplication`

**Implements:** LemmaCountConfigLoader

**Framework types:** Component

#### DefaultQuizInstructionConfig

**Package:** `com.learney.contentaudit.auditapplication`

**Implements:** QuizInstructionConfig

**Framework types:** Component

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `judgeRuntimeConfig` | `JudgeRuntimeConfig` |

**Generated constructor:**
```java
public DefaultQuizInstructionConfig(JudgeRuntimeConfig judgeRuntimeConfig) {
    this.judgeRuntimeConfig = judgeRuntimeConfig;
}
```

#### DefaultJudgeExamService

**Package:** `com.learney.contentaudit.auditapplication`

**Implements:** JudgeExamService

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `auditRunner` | `AuditRunner` |
| `analyzerCatalog` | `AnalyzerCatalog` |
| `exporter` | `JudgeExamVerdictExporter` |
| `examRegistry` | `JudgeExamRegistry` |
| `runtime` | `JudgeRuntime` |

**Generated constructor:**
```java
public DefaultJudgeExamService(AuditRunner auditRunner, AnalyzerCatalog analyzerCatalog, JudgeExamVerdictExporter exporter, JudgeExamRegistry examRegistry, JudgeRuntime runtime) {
    this.auditRunner = auditRunner;
    this.analyzerCatalog = analyzerCatalog;
    this.exporter = exporter;
    this.examRegistry = examRegistry;
    this.runtime = runtime;
}
```

#### DefaultJudgeRuntimeConfig

**Package:** `com.learney.contentaudit.auditapplication`

**Implements:** JudgeRuntimeConfig

**Framework types:** Component

#### DefaultTranslationSenseConfig

**Package:** `com.learney.contentaudit.auditapplication`

**Implements:** TranslationSenseConfig

**Framework types:** Component

#### DefaultSingleAnswerConfig

**Package:** `com.learney.contentaudit.auditapplication`

**Implements:** SingleAnswerConfig

**Framework types:** Component

#### DefaultMiniTheoryConfig

**Package:** `com.learney.contentaudit.auditapplication`

**Implements:** MiniTheoryConfig

**Framework types:** Component

#### DefaultChoiceAlternativeConfig

**Package:** `com.learney.contentaudit.auditapplication`

**Implements:** ChoiceAlternativeConfig

**Framework types:** Component

### Module: course-infrastructure

#### FileSystemCourseRepository

**Package:** `com.learney.contentaudit.courseinfrastructure`

**Implements:** CourseRepository

**Framework types:** Repository

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `courseValidator` | `CourseValidator` |

**Generated constructor:**
```java
public FileSystemCourseRepository(CourseValidator courseValidator) {
    this.courseValidator = courseValidator;
}
```

**Tests that must pass:** 28

- Given a valid course entity, when save is called, then validator is invoked and no exception is thrown [F-COURSE/F-COURSE-R014]
- Given an invalid course entity, when save is called, then validator throws CourseValidationException [F-COURSE/F-COURSE-R014]
- Given a course entity with validator passing, when save is called, then no exception is thrown [F-COURSE]
- Given a null course entity, when save is called, then an exception is thrown [F-COURSE/F-COURSE-R009]
- Given validator rejects course with duplicate IDs, when save is called, then CourseValidationException propagates [F-COURSE/F-COURSE-R006]
- Given validator rejects course with broken parent references, when save is called, then CourseValidationException propagates [F-COURSE/F-COURSE-R008]
- Given validator rejects milestone with no topics, when save is called, then CourseValidationException propagates [F-COURSE/F-COURSE-R015]
- Given a valid course directory, when load is called, then returns CourseEntity with 5-level hierarchy including ROOT node [F-COURSE/F-COURSE-R001]
- Given a course with ordered milestones and topics, when load is called, then child order matches parent children lists [F-COURSE/F-COURSE-R002]
- Given a loaded course, when saved and reloaded, then the result is semantically equivalent to the original [F-COURSE/F-COURSE-R003]
- Given a course directory with quiz templates, when load is called, then every knowledge has at least one quiz template [F-COURSE/F-COURSE-R004]
- Given a course directory with consistent IDs, when load is called, then all child ID references resolve to existing entities [F-COURSE/F-COURSE-R005]
- Given a course directory with unique IDs, when load is called, then no duplicate IDs exist across any hierarchy level [F-COURSE/F-COURSE-R006]
- Given a valid directory structure, when load is called, then each directory level contains its expected descriptor file [F-COURSE/F-COURSE-R007]
- Given a loaded course, when inspecting parent references, then each child parentId matches its actual parent id [F-COURSE/F-COURSE-R008]
- Given a course with all required fields populated, when load is called, then all mandatory fields are non-null [F-COURSE/F-COURSE-R009]
- Given a course with empty string and null field values, when saved and reloaded, then empty and null values are preserved exactly [F-COURSE/F-COURSE-R010]
- Given quiz templates with dual id and oidId fields, when load is called, then both fields contain the same value [F-COURSE/F-COURSE-R011]
- Given quiz templates with numberDouble format values, when saved to JSON, then numeric fields preserve MongoDB Extended JSON format [F-COURSE/F-COURSE-R012]
- Given a loaded course, when inspecting order fields, then milestones topics and knowledges have sequential 1-based order within their parent [F-COURSE/F-COURSE-R013]
- Given a milestone with empty children list, when load is called, then validation rejects the course with a descriptive error [F-COURSE/F-COURSE-R015]
- Given entities with labels, when saved to disk, then directory names are deterministic slugs derived from the labels [F-COURSE/F-COURSE-R016]
- Given a course directory with full hierarchy, when loading step by step, then all levels are resolved with correct hierarchy order and validation [F-COURSE]
- Given a course in memory, when saving to a target directory, then the directory structure and JSON files are written correctly [F-COURSE]
- Given a course loaded from files, when saved to a new directory and reloaded, then the reloaded course equals the original [F-COURSE]
- Given a loaded course, when navigating from ROOT to milestones to topics to knowledges to quizzes, then each level is accessible and correctly ordered [F-COURSE]
- Given a loaded course, when a knowledge label is modified and the course is saved and reloaded, then the change is reflected and unmodified data remains intact [F-COURSE]
- Given a nonexistent path or missing descriptor or malformed JSON, when load is called, then a descriptive error is thrown and no partial course is returned [F-COURSE]

### Module: audit-cli

#### Main (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** 

#### ContentAuditCmd (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** 

#### AnalyzeCmd (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** AnalyzeCommand

**Constructor dependencies:**

| Name | Type |
|------|------|
| `auditRunner` | `AuditRunner` |
| `formatterRegistry` | `FormatterRegistry` |
| `viewModelTransformer` | `ReportViewModelTransformer` |
| `rawReportFormatter` | `RawReportFormatter` |
| `drillDownResolver` | `DrillDownResolver` |
| `detailedFormatters` | `Map<String,DetailedFormatter>` |
| `auditReportStore` | `AuditReportStore` |
| `reevaluationQuizSetResolver` | `ReevaluationQuizSetResolver` |

#### GetCmd (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** GetCommand

**Constructor dependencies:**

| Name | Type |
|------|------|
| `auditReportStore` | `AuditReportStore` |
| `refinementPlanStore` | `RefinementPlanStore` |
| `analyzerRegistry` | `AnalyzerRegistry` |
| `correctionContextResolver` | `CorrectionContextResolver` |
| `revisionArtifactStore` | `RevisionArtifactStore` |
| `impactPreviewStore` | `ImpactPreviewStore` |
| `impactPreviewFormatter` | `ImpactPreviewFormatter` |
| `correctionContextJsonMapper` | `CorrectionContextJsonMapper` |
| `suggestedLemmaQueryPort` | `SuggestedLemmaQueryPort` |
| `topicWordLightsQuery` | `TopicWordLightsQuery` |
| `topicStudentViewQuery` | `TopicStudentViewQuery` |
| `activeAnalysisSelectionStore` | `ActiveAnalysisSelectionStore` |
| `errorTaskBriefer` | `ErrorTaskBriefer` |

#### DeleteCmd (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** DeleteCommand

**Constructor dependencies:**

| Name | Type |
|------|------|
| `auditReportStore` | `AuditReportStore` |
| `refinementPlanStore` | `RefinementPlanStore` |
| `activeAnalysisSelectionStore` | `ActiveAnalysisSelectionStore` |
| `baselineStore` | `BaselineStore` |

#### PruneCmd (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** PruneCommand

**Constructor dependencies:**

| Name | Type |
|------|------|
| `auditReportStore` | `AuditReportStore` |
| `refinementPlanStore` | `RefinementPlanStore` |
| `activeAnalysisSelectionStore` | `ActiveAnalysisSelectionStore` |
| `baselineStore` | `BaselineStore` |

#### PlanCmd (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** PlanCommand

**Constructor dependencies:**

| Name | Type |
|------|------|
| `auditReportStore` | `AuditReportStore` |
| `refinerEngine` | `RefinerEngine` |
| `refinementPlanStore` | `RefinementPlanStore` |
| `ephemeralPlanRenderer` | `EphemeralPlanRenderer` |

#### ReviseCmd (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** ReviseCommand

**Constructor dependencies:**

| Name | Type |
|------|------|
| `revisionEngine` | `RevisionEngine` |
| `refinementPlanStore` | `RefinementPlanStore` |
| `batchStore` | `BatchStore` |

#### ConfigAnalyzerCmd (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** ConfigAnalyzerCommand

**Constructor dependencies:**

| Name | Type |
|------|------|
| `analyzerRegistry` | `AnalyzerRegistry` |

#### StatsAnalyzerCmd (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** StatsAnalyzerCommand

**Constructor dependencies:**

| Name | Type |
|------|------|
| `analyzerRegistry` | `AnalyzerRegistry` |
| `analyzerStatsTransformer` | `AnalyzerStatsTransformer` |
| `auditRunner` | `AuditRunner` |
| `detailedFormatters` | `Map<String,DetailedFormatter>` |

#### ApproveCmd (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** ApproveCommand

**Constructor dependencies:**

| Name | Type |
|------|------|
| `decisionService` | `ProposalDecisionService` |
| `batchStore` | `BatchStore` |

#### RejectCmd (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** RejectCommand

**Constructor dependencies:**

| Name | Type |
|------|------|
| `decisionService` | `ProposalDecisionService` |
| `batchService` | `BatchService` |

#### DefaultGetConsolidatedCommand (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** GetConsolidatedCommand

**Constructor dependencies:**

| Name | Type |
|------|------|
| `consolidatedViewBuilder` | `ConsolidatedViewBuilder` |
| `formatter` | `ConsolidatedViewFormatter` |

#### DefaultSetActiveAnalysisCommand (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** SetActiveAnalysisCommand

**Constructor dependencies:**

| Name | Type |
|------|------|
| `activeAnalysisSelectionStore` | `ActiveAnalysisSelectionStore` |

#### DefaultEphemeralPlanRenderer (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** EphemeralPlanRenderer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `auditNodeIndexFactory` | `AuditNodeIndexFactory` |
| `correctionContextResolver` | `CorrectionContextResolver` |
| `correctionContextJsonMapper` | `CorrectionContextJsonMapper` |

#### DefaultCorrectionContextJsonMapper (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** CorrectionContextJsonMapper

#### LexisCmd (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** LexisCommand

**Constructor dependencies:**

| Name | Type |
|------|------|
| `quizSentenceConverter` | `QuizSentenceConverter` |
| `nlpTokenizer` | `NlpTokenizer` |
| `sentenceLexicalEvaluator` | `SentenceLexicalEvaluator` |

#### RepairCmd (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** RepairCommand

**Constructor dependencies:**

| Name | Type |
|------|------|
| `preservationRepair` | `PreservationRepair` |
| `courseRepository` | `CourseRepository` |

#### ReviseInstructionsCmd (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** ReviseInstructionsCommand

**Constructor dependencies:**

| Name | Type |
|------|------|
| `correctionRunner` | `QuizInstructionCorrectionRunner` |
| `refinementPlanStore` | `RefinementPlanStore` |
| `runFormatter` | `QuizInstructionCorrectionRunFormatter` |

#### AssessCandidateCmd (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** AssessCandidateCommand

**Constructor dependencies:**

| Name | Type |
|------|------|
| `candidateAssessor` | `QuizInstructionCandidateAssessor` |

#### ExamCmd (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** ExamCommand

**Constructor dependencies:**

| Name | Type |
|------|------|
| `examService` | `JudgeExamService` |

#### DefaultSetBaselineCommand (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** SetBaselineCommand

**Constructor dependencies:**

| Name | Type |
|------|------|
| `baselineStore` | `BaselineStore` |
| `auditReportStore` | `AuditReportStore` |

#### DefaultSaveProjectionCommand (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** SaveProjectionCommand

**Constructor dependencies:**

| Name | Type |
|------|------|
| `courseProjector` | `CourseProjector` |
| `projectionStore` | `ProjectionStore` |

#### DefaultCompareCommand (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** CompareCommand

**Constructor dependencies:**

| Name | Type |
|------|------|
| `batchComparator` | `BatchComparator` |
| `formatter` | `BatchComparisonFormatter` |

#### DefaultGoalsCommand (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** GoalsCommand

**Constructor dependencies:**

| Name | Type |
|------|------|
| `goalBook` | `GoalBook` |

#### DefaultBlindSampleCommand (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** BlindSampleCommand

**Constructor dependencies:**

| Name | Type |
|------|------|
| `blindSampleService` | `BlindSampleService` |

#### DefaultErrorRateCommand (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** ErrorRateCommand

**Constructor dependencies:**

| Name | Type |
|------|------|
| `blindErrorRateService` | `BlindErrorRateService` |

#### BatchCmd (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** BatchCommand

**Constructor dependencies:**

| Name | Type |
|------|------|
| `batchService` | `BatchService` |

#### TrayCmd (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** internal
**Implements:** TrayCommand

**Constructor dependencies:**

| Name | Type |
|------|------|
| `trayService` | `TrayService` |
| `quizSentenceConverter` | `QuizSentenceConverter` |

#### TextReportFormatter (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Implements:** ReportFormatter

**Constructor dependencies:**

| Name | Type |
|------|------|
| `drillDownResolver` | `DrillDownResolver` |

#### JsonReportFormatter (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Implements:** ReportFormatter

**Constructor dependencies:**

| Name | Type |
|------|------|
| `drillDownResolver` | `DrillDownResolver` |

#### TableReportFormatter (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Implements:** ReportFormatter

**Constructor dependencies:**

| Name | Type |
|------|------|
| `drillDownResolver` | `DrillDownResolver` |

#### DefaultFormatterRegistry (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Implements:** FormatterRegistry

**Constructor dependencies:**

| Name | Type |
|------|------|
| `formatters` | `Map<String, ReportFormatter>` |

#### DefaultReportViewModelTransformer (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Implements:** ReportViewModelTransformer

#### RawJsonReportFormatter (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Implements:** RawReportFormatter

#### DefaultDrillDownResolver (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Implements:** DrillDownResolver

#### DefaultAnalyzerStatsTransformer (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Implements:** AnalyzerStatsTransformer

#### LemmaAbsenceDetailedFormatter (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Implements:** DetailedFormatter

#### CocaBucketsDetailedFormatter (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Implements:** DetailedFormatter

#### DefaultImpactPreviewFormatter (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Implements:** ImpactPreviewFormatter

#### DefaultConsolidatedViewFormatter (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Implements:** ConsolidatedViewFormatter

#### QuizInstructionDetailedFormatter (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Implements:** DetailedFormatter

#### DefaultQuizInstructionCorrectionRunFormatter (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Implements:** QuizInstructionCorrectionRunFormatter

#### LemmaRecurrenceDetailedFormatter (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Implements:** DetailedFormatter

#### DefaultBatchComparisonFormatter (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Implements:** BatchComparisonFormatter

#### DefaultWorkdirResolver (package: bootstrap)

**Package:** `com.learney.contentaudit.auditcli.bootstrap`
**Visibility:** internal
**Implements:** WorkdirResolver

#### DefaultApprovalModeResolver (package: bootstrap)

**Package:** `com.learney.contentaudit.auditcli.bootstrap`
**Visibility:** internal
**Implements:** ApprovalModeResolver

#### DefaultProposalStrategySelector (package: bootstrap)

**Package:** `com.learney.contentaudit.auditcli.bootstrap`
**Visibility:** internal
**Implements:** ProposalStrategySelector

#### DefaultLagenModeResolver (package: bootstrap)

**Package:** `com.learney.contentaudit.auditcli.bootstrap`
**Visibility:** internal
**Implements:** LagenModeResolver

#### DefaultLagenConfigResolver (package: bootstrap)

**Package:** `com.learney.contentaudit.auditcli.bootstrap`
**Visibility:** internal
**Implements:** LagenConfigResolver

**Constructor dependencies:**

| Name | Type |
|------|------|
| `llmProviderResolver` | `LlmProviderResolver` |

#### DefaultQuizInstructionJudgeConfigResolver (package: bootstrap)

**Package:** `com.learney.contentaudit.auditcli.bootstrap`
**Visibility:** internal
**Implements:** QuizInstructionJudgeConfigResolver

#### EnvQuizInstructionCorrectionConfigResolver (package: bootstrap)

**Package:** `com.learney.contentaudit.auditcli.bootstrap`
**Visibility:** internal
**Implements:** QuizInstructionCorrectionConfigResolver

#### DefaultReevaluationQuizSetResolver (package: bootstrap)

**Package:** `com.learney.contentaudit.auditcli.bootstrap`
**Visibility:** internal
**Implements:** ReevaluationQuizSetResolver

#### DefaultAgentJudgeConfigResolver (package: bootstrap)

**Package:** `com.learney.contentaudit.auditcli.bootstrap`
**Visibility:** internal
**Implements:** AgentJudgeConfigResolver

### Module: nlp-infrastructure

#### SpacyNlpTokenizerFactory (package: spacy)

**Package:** `com.learney.contentaudit.nlpinfrastructure.spacy`
**Visibility:** public
**Implements:** NlpTokenizerFactory

#### SpacyNlpTokenizer (package: spacy)

**Package:** `com.learney.contentaudit.nlpinfrastructure.spacy`
**Visibility:** public
**Implements:** NlpTokenizer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `processRunner` | `SpacyProcessRunner` |
| `resultParser` | `SpacyResultParser` |

#### SpacyProcessRunner (package: spacy)

**Package:** `com.learney.contentaudit.nlpinfrastructure.spacy`
**Visibility:** public
**Implements:** 

**Constructor dependencies:**

| Name | Type |
|------|------|
| `config` | `NlpTokenizerConfig` |

#### SpacyResultParser (package: spacy)

**Package:** `com.learney.contentaudit.nlpinfrastructure.spacy`
**Visibility:** public
**Implements:** 

#### CachedNlpTokenizer (package: spacy)

**Package:** `com.learney.contentaudit.nlpinfrastructure.spacy`
**Visibility:** public
**Implements:** NlpTokenizer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `delegate` | `NlpTokenizer` |

### Module: vocabulary-infrastructure

#### FileSystemEvpCatalog (package: evp)

**Package:** `com.learney.contentaudit.vocabularyinfrastructure.evp`
**Visibility:** internal
**Implements:** EvpCatalogPort

#### FileSystemIrregularFormsCatalog (package: irregularforms)

**Package:** `com.learney.contentaudit.vocabularyinfrastructure.irregularforms`
**Visibility:** internal
**Implements:** IrregularFormsCatalog

**Constructor dependencies:**

| Name | Type |
|------|------|
| `catalogPath` | `Path` |

#### FileSystemFormEquivalenceTableSource (package: correctortable)

**Package:** `com.learney.contentaudit.vocabularyinfrastructure.correctortable`
**Visibility:** internal
**Implements:** FormEquivalenceTableSource

**Constructor dependencies:**

| Name | Type |
|------|------|
| `tablePath` | `Path` |

### Module: audit-infrastructure

#### FileSystemAuditReportStore

**Package:** `com.learney.contentaudit.auditinfrastructure`

**Implements:** AuditReportStore

**Framework types:** Repository

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `baseDir` | `Path` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |

**Generated constructor:**
```java
public FileSystemAuditReportStore(Path baseDir, QuizStudentViewRenderer studentViewRenderer) {
    this.baseDir = baseDir;
    this.studentViewRenderer = studentViewRenderer;
}
```

#### FileSystemRefinementPlanStore

**Package:** `com.learney.contentaudit.auditinfrastructure`

**Implements:** RefinementPlanStore

**Framework types:** Repository

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `baseDir` | `Path` |

**Generated constructor:**
```java
public FileSystemRefinementPlanStore(Path baseDir) {
    this.baseDir = baseDir;
}
```

#### FileSystemRevisionArtifactStore

**Package:** `com.learney.contentaudit.auditinfrastructure`

**Implements:** RevisionArtifactStore

**Framework types:** Repository

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `baseDir` | `Path` |

**Generated constructor:**
```java
public FileSystemRevisionArtifactStore(Path baseDir) {
    this.baseDir = baseDir;
}
```

#### FileSystemImpactPreviewStore

**Package:** `com.learney.contentaudit.auditinfrastructure`

**Implements:** ImpactPreviewStore

**Framework types:** Repository

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `baseDir` | `Path` |

**Generated constructor:**
```java
public FileSystemImpactPreviewStore(Path baseDir) {
    this.baseDir = baseDir;
}
```

#### FileSystemActiveAnalysisSelectionStore

**Package:** `com.learney.contentaudit.auditinfrastructure`

**Implements:** ActiveAnalysisSelectionStore

**Framework types:** Repository

#### FileSystemQuizInstructionCorrectionRunStore

**Package:** `com.learney.contentaudit.auditinfrastructure`

**Implements:** QuizInstructionCorrectionRunStore

#### FileSystemJudgeExamRegistry

**Package:** `com.learney.contentaudit.auditinfrastructure`

**Implements:** JudgeExamRegistry

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `baseDir` | `Path` |

**Generated constructor:**
```java
public FileSystemJudgeExamRegistry(Path baseDir) {
    this.baseDir = baseDir;
}
```

#### FileSystemBaselineStore

**Package:** `com.learney.contentaudit.auditinfrastructure`

**Implements:** BaselineStore

**Framework types:** Repository

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `baseDir` | `Path` |

**Generated constructor:**
```java
public FileSystemBaselineStore(Path baseDir) {
    this.baseDir = baseDir;
}
```

#### FileSystemProjectionStore

**Package:** `com.learney.contentaudit.auditinfrastructure`

**Implements:** ProjectionStore

**Framework types:** Repository

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `baseDir` | `Path` |

**Generated constructor:**
```java
public FileSystemProjectionStore(Path baseDir) {
    this.baseDir = baseDir;
}
```

#### FileSystemGoalDeclarationSource

**Package:** `com.learney.contentaudit.auditinfrastructure`

**Implements:** GoalDeclarationSource

**Framework types:** Repository

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `declarationFile` | `Path` |

**Generated constructor:**
```java
public FileSystemGoalDeclarationSource(Path declarationFile) {
    this.declarationFile = declarationFile;
}
```

#### FileSystemSealedGoalsStore

**Package:** `com.learney.contentaudit.auditinfrastructure`

**Implements:** SealedGoalsStore

**Framework types:** Repository

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `baseDir` | `Path` |

**Generated constructor:**
```java
public FileSystemSealedGoalsStore(Path baseDir) {
    this.baseDir = baseDir;
}
```

#### FileSystemBlindSampleStore

**Package:** `com.learney.contentaudit.auditinfrastructure`

**Implements:** BlindSampleStore

**Framework types:** Repository

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `baseDir` | `Path` |

**Generated constructor:**
```java
public FileSystemBlindSampleStore(Path baseDir) {
    this.baseDir = baseDir;
}
```

#### FileSystemAudioRecordingRegistry (package: exercisecopies)

**Package:** `com.learney.contentaudit.auditinfrastructure.exercisecopies`
**Visibility:** internal
**Implements:** AudioRecordingRegistry

**Constructor dependencies:**

| Name | Type |
|------|------|
| `registryPath` | `Path` |

#### FileSystemProductionSentenceSource (package: exercisecopies)

**Package:** `com.learney.contentaudit.auditinfrastructure.exercisecopies`
**Visibility:** internal
**Implements:** ProductionSentenceSource

**Constructor dependencies:**

| Name | Type |
|------|------|
| `exportPath` | `Path` |

#### FileSystemBatchStore (package: batchstore)

**Package:** `com.learney.contentaudit.auditinfrastructure.batchstore`
**Visibility:** internal
**Implements:** BatchStore

**Constructor dependencies:**

| Name | Type |
|------|------|
| `baseDir` | `Path` |

#### FileSystemTrayDecisionStore (package: traystore)

**Package:** `com.learney.contentaudit.auditinfrastructure.traystore`
**Visibility:** internal
**Implements:** TrayDecisionStore

**Constructor dependencies:**

| Name | Type |
|------|------|
| `baseDir` | `Path` |

### Module: revision-domain

#### DefaultRevisionEngineFactory (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** RevisionEngineFactory

#### IdentityReviser (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** Reviser

#### AutoApproveValidator (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** RevisionValidator

#### DefaultRevisionValidatorResult (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** RevisionValidatorResult

**Constructor dependencies:**

| Name | Type |
|------|------|
| `verdict` | `RevisionVerdict` |
| `rejectionReason` | `Optional<String>` |

#### DispatchingReviser (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** Reviser

**Constructor dependencies:**

| Name | Type |
|------|------|
| `byKind` | `Map<DiagnosisKind,Reviser>` |
| `fallback` | `IdentityReviser` |
| `strategyRegistry` | `LemmaAbsenceProposalStrategyRegistry` |
| `deriver` | `LemmaAbsenceProposalDeriver` |

#### DefaultCourseElementLocator (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** CourseElementLocator

#### DefaultRevisionEngine (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** RevisionEngine

**Constructor dependencies:**

| Name | Type |
|------|------|
| `refinementPlanStore` | `RefinementPlanStore` |
| `auditReportStore` | `AuditReportStore` |
| `contextResolver` | `CorrectionContextResolver<CorrectionContext>` |
| `reviser` | `Reviser` |
| `validator` | `RevisionValidator` |
| `artifactStore` | `RevisionArtifactStore` |
| `courseRepository` | `CourseRepository` |
| `elementLocator` | `CourseElementLocator` |
| `impactPreviewComputer` | `ImpactPreviewComputer` |
| `impactPreviewStore` | `ImpactPreviewStore` |
| `correctionContextOverrideParser` | `CorrectionContextOverrideParser` |

#### HumanApprovalValidator (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** RevisionValidator

#### DefaultRevisionValidatorFactory (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** RevisionValidatorFactory

#### DefaultProposalDecisionServiceFactory (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** ProposalDecisionServiceFactory

#### DefaultProposalDecisionService (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** ProposalDecisionService

**Constructor dependencies:**

| Name | Type |
|------|------|
| `artifactStore` | `RevisionArtifactStore` |
| `courseRepository` | `CourseRepository` |
| `elementLocator` | `CourseElementLocator` |
| `refinementPlanStore` | `RefinementPlanStore` |
| `preservationCheck` | `PreservationCheck` |

#### DefaultLemmaAbsenceProposalStrategyRegistry (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** LemmaAbsenceProposalStrategyRegistry

**Constructor dependencies:**

| Name | Type |
|------|------|
| `config` | `LemmaAbsenceProposalStrategyRegistryConfig` |

#### DefaultLemmaAbsenceProposalDeriver (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** LemmaAbsenceProposalDeriver

**Constructor dependencies:**

| Name | Type |
|------|------|
| `quizSentenceConverter` | `QuizSentenceConverter` |

#### DefaultImpactPreviewComputer (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** ImpactPreviewComputer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `auditReportStore` | `AuditReportStore` |
| `pendingApplier` | `PendingApplier` |
| `projectionMeasurer` | `ProjectionMeasurer` |

#### DefaultConsolidatedViewBuilderFactory (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** ConsolidatedViewBuilderFactory

#### DefaultConsolidatedViewBuilder (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** ConsolidatedViewBuilder

**Constructor dependencies:**

| Name | Type |
|------|------|
| `nodeFieldDiffer` | `NodeFieldDiffer` |
| `courseProjector` | `CourseProjector` |

#### KnowledgeTitleReviser (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** Reviser

**Constructor dependencies:**

| Name | Type |
|------|------|
| `strategyRegistry` | `KnowledgeTitleProposalStrategyRegistry` |
| `deriver` | `KnowledgeTitleProposalDeriver` |

#### DefaultKnowledgeTitleProposalStrategyRegistry (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** KnowledgeTitleProposalStrategyRegistry

**Constructor dependencies:**

| Name | Type |
|------|------|
| `config` | `KnowledgeTitleProposalStrategyRegistryConfig` |

#### DefaultKnowledgeTitleProposalDeriver (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** KnowledgeTitleProposalDeriver

#### QuizInstructionReviser (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** Reviser

**Constructor dependencies:**

| Name | Type |
|------|------|
| `strategyRegistry` | `QuizInstructionProposalStrategyRegistry` |
| `deriver` | `LemmaAbsenceProposalDeriver` |
| `complianceChecker` | `QuizInstructionComplianceChecker` |
| `subjectViewFactory` | `QuizInstructionSubjectViewFactory` |
| `config` | `QuizInstructionCorrectionConfig` |
| `candidateAssessor` | `QuizInstructionCandidateAssessor` |

#### DefaultQuizInstructionProposalStrategyRegistry (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** QuizInstructionProposalStrategyRegistry

**Constructor dependencies:**

| Name | Type |
|------|------|
| `config` | `QuizInstructionProposalStrategyRegistryConfig` |

#### DefaultQuizInstructionCorrectionRunner (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** QuizInstructionCorrectionRunner

**Constructor dependencies:**

| Name | Type |
|------|------|
| `revisionEngine` | `RevisionEngine` |
| `refinementPlanStore` | `RefinementPlanStore` |
| `runStore` | `QuizInstructionCorrectionRunStore` |
| `config` | `QuizInstructionCorrectionConfig` |

#### DefaultQuizInstructionCorrectionRunnerFactory (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** QuizInstructionCorrectionRunnerFactory

#### DefaultQuizInstructionCandidateAssessor (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** QuizInstructionCandidateAssessor

**Constructor dependencies:**

| Name | Type |
|------|------|
| `assessor` | `CandidateAssessor` |
| `deriver` | `LemmaAbsenceProposalDeriver` |
| `refinementPlanStore` | `RefinementPlanStore` |
| `auditReportStore` | `AuditReportStore` |
| `contextResolver` | `CorrectionContextResolver<CorrectionContext>` |
| `courseRepository` | `CourseRepository` |
| `elementLocator` | `CourseElementLocator` |

#### DefaultQuizInstructionCandidateAssessorFactory (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** QuizInstructionCandidateAssessorFactory

#### DefaultCourseProjectorFactory (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** CourseProjectorFactory

#### DefaultCourseProjector (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** CourseProjector

**Constructor dependencies:**

| Name | Type |
|------|------|
| `activeAnalysisSelectionStore` | `ActiveAnalysisSelectionStore` |
| `auditReportStore` | `AuditReportStore` |
| `refinementPlanStore` | `RefinementPlanStore` |
| `revisionArtifactStore` | `RevisionArtifactStore` |
| `courseRepository` | `CourseRepository` |
| `pendingApplier` | `PendingApplier` |
| `projectionMeasurer` | `ProjectionMeasurer` |

#### DefaultPendingApplier (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** PendingApplier

**Constructor dependencies:**

| Name | Type |
|------|------|
| `courseElementLocator` | `CourseElementLocator` |

#### DefaultProjectionMeasurer (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Implements:** ProjectionMeasurer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `courseMapper` | `CourseMapper` |
| `auditEngine` | `AuditEngine` |

#### LemmaAbsenceMvpStrategy (package: lemmaabsence)

**Package:** `com.learney.contentaudit.revisiondomain.lemmaabsence`
**Visibility:** public
**Implements:** LemmaAbsenceProposalStrategy

**Constructor dependencies:**

| Name | Type |
|------|------|
| `generator` | `LemmaAbsenceQuizCandidateGenerator` |
| `providerId` | `String` |

#### CannedLemmaAbsenceQuizCandidateGenerator (package: lemmaabsence)

**Package:** `com.learney.contentaudit.revisiondomain.lemmaabsence`
**Visibility:** public
**Implements:** LemmaAbsenceQuizCandidateGenerator

**Constructor dependencies:**

| Name | Type |
|------|------|
| `cannedQuizSentence` | `String` |
| `cannedTranslation` | `String` |

#### RecursiveNodeFieldDiffer (package: fielddiff)

**Package:** `com.learney.contentaudit.revisiondomain.fielddiff`
**Visibility:** internal
**Implements:** NodeFieldDiffer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `exclusionRegistry` | `FieldExclusionRegistry` |
| `identityRegistry` | `ListIdentityRegistry` |

#### DefaultFieldExclusionRegistry (package: fielddiff)

**Package:** `com.learney.contentaudit.revisiondomain.fielddiff`
**Visibility:** internal
**Implements:** FieldExclusionRegistry

#### DefaultListIdentityRegistry (package: fielddiff)

**Package:** `com.learney.contentaudit.revisiondomain.fielddiff`
**Visibility:** internal
**Implements:** ListIdentityRegistry

#### DefaultNodeFieldDifferFactory (package: fielddiff)

**Package:** `com.learney.contentaudit.revisiondomain.fielddiff`
**Visibility:** internal
**Implements:** NodeFieldDifferFactory

#### DefaultCorrectionContextOverrideParser (package: contextoverride)

**Package:** `com.learney.contentaudit.revisiondomain.contextoverride`
**Visibility:** internal
**Implements:** CorrectionContextOverrideParser

**Constructor dependencies:**

| Name | Type |
|------|------|
| `objectMapper` | `ObjectMapper` |
| `validators` | `Map<DiagnosisKind,CorrectionContextStructuralValidator>` |

#### LemmaAbsenceContextStructuralValidator (package: contextoverride)

**Package:** `com.learney.contentaudit.revisiondomain.contextoverride`
**Visibility:** internal
**Implements:** CorrectionContextStructuralValidator

**Constructor dependencies:**

| Name | Type |
|------|------|
| `objectMapper` | `ObjectMapper` |

#### SentenceLengthContextStructuralValidator (package: contextoverride)

**Package:** `com.learney.contentaudit.revisiondomain.contextoverride`
**Visibility:** internal
**Implements:** CorrectionContextStructuralValidator

**Constructor dependencies:**

| Name | Type |
|------|------|
| `objectMapper` | `ObjectMapper` |

#### KnowledgeTitleAgentStrategy (package: knowledgetitle)

**Package:** `com.learney.contentaudit.revisiondomain.knowledgetitle`
**Visibility:** public
**Implements:** KnowledgeTitleProposalStrategy

**Constructor dependencies:**

| Name | Type |
|------|------|
| `generator` | `KnowledgeTitleCandidateGenerator` |
| `providerId` | `String` |

#### DefaultCorrectionScope (package: preservationengine)

**Package:** `com.learney.contentaudit.revisiondomain.preservationengine`
**Visibility:** internal
**Implements:** CorrectionScope

#### DefaultPreservationCheck (package: preservationengine)

**Package:** `com.learney.contentaudit.revisiondomain.preservationengine`
**Visibility:** internal
**Implements:** PreservationCheck

**Constructor dependencies:**

| Name | Type |
|------|------|
| `scope` | `CorrectionScope` |

#### DefaultPreservationRepair (package: preservationengine)

**Package:** `com.learney.contentaudit.revisiondomain.preservationengine`
**Visibility:** internal
**Implements:** PreservationRepair

**Constructor dependencies:**

| Name | Type |
|------|------|
| `artifactStore` | `RevisionArtifactStore` |
| `scope` | `CorrectionScope` |

#### DefaultPreservationFactory (package: preservationengine)

**Package:** `com.learney.contentaudit.revisiondomain.preservationengine`
**Visibility:** internal
**Implements:** PreservationFactory

#### DefaultCandidateCriteriaFactory (package: candidatecriteriaengine)

**Package:** `com.learney.contentaudit.revisiondomain.candidatecriteriaengine`
**Visibility:** internal
**Implements:** CandidateCriteriaFactory

#### DefaultCandidateAssessor (package: candidatecriteriaengine)

**Package:** `com.learney.contentaudit.revisiondomain.candidatecriteriaengine`
**Visibility:** internal
**Implements:** CandidateAssessor

**Constructor dependencies:**

| Name | Type |
|------|------|
| `evaluatorsByCriterion` | `Map<CorrectionCriterion,CandidateCriterionEvaluator>` |
| `lengthMeter` | `CandidateLengthMeter` |
| `ranking` | `CandidateRanking` |

#### ScopedEditCriterion (package: candidatecriteriaengine)

**Package:** `com.learney.contentaudit.revisiondomain.candidatecriteriaengine`
**Visibility:** internal
**Implements:** CandidateCriterionEvaluator

#### LengthInRangeCriterion (package: candidatecriteriaengine)

**Package:** `com.learney.contentaudit.revisiondomain.candidatecriteriaengine`
**Visibility:** internal
**Implements:** CandidateCriterionEvaluator

**Constructor dependencies:**

| Name | Type |
|------|------|
| `lengthMeter` | `CandidateLengthMeter` |

#### LevelVocabularyCriterion (package: candidatecriteriaengine)

**Package:** `com.learney.contentaudit.revisiondomain.candidatecriteriaengine`
**Visibility:** internal
**Implements:** CandidateCriterionEvaluator

**Constructor dependencies:**

| Name | Type |
|------|------|
| `lexicalEvaluator` | `SentenceLexicalEvaluator` |
| `tokenizer` | `NlpTokenizer` |

#### DistinctnessCriterion (package: candidatecriteriaengine)

**Package:** `com.learney.contentaudit.revisiondomain.candidatecriteriaengine`
**Visibility:** internal
**Implements:** CandidateCriterionEvaluator

**Constructor dependencies:**

| Name | Type |
|------|------|
| `quizCorpus` | `CourseQuizCorpus` |
| `siblingSimilarityThreshold` | `double` |
| `courseSimilarityThreshold` | `double` |
| `courseWideDistinctness` | `boolean` |

#### InstructionComplianceCriterion (package: candidatecriteriaengine)

**Package:** `com.learney.contentaudit.revisiondomain.candidatecriteriaengine`
**Visibility:** internal
**Implements:** CandidateCriterionEvaluator

**Constructor dependencies:**

| Name | Type |
|------|------|
| `complianceChecker` | `QuizInstructionComplianceChecker` |
| `subjectViewFactory` | `QuizInstructionSubjectViewFactory` |

#### AuditReportQuizCorpus (package: candidatecriteriaengine)

**Package:** `com.learney.contentaudit.revisiondomain.candidatecriteriaengine`
**Visibility:** internal
**Implements:** CourseQuizCorpus

**Constructor dependencies:**

| Name | Type |
|------|------|
| `auditReportStore` | `AuditReportStore` |

#### DefaultCandidateLengthMeter (package: candidatecriteriaengine)

**Package:** `com.learney.contentaudit.revisiondomain.candidatecriteriaengine`
**Visibility:** internal
**Implements:** CandidateLengthMeter

**Constructor dependencies:**

| Name | Type |
|------|------|
| `tokenizer` | `NlpTokenizer` |
| `sentenceLengthConfig` | `SentenceLengthConfig` |

#### DefaultCandidateRanking (package: candidatecriteriaengine)

**Package:** `com.learney.contentaudit.revisiondomain.candidatecriteriaengine`
**Visibility:** internal
**Implements:** CandidateRanking

#### QuizInstructionAgentStrategy (package: quizinstruction)

**Package:** `com.learney.contentaudit.revisiondomain.quizinstruction`
**Visibility:** public
**Implements:** QuizInstructionProposalStrategy

**Constructor dependencies:**

| Name | Type |
|------|------|
| `generator` | `QuizInstructionCandidateGenerator` |
| `providerId` | `String` |

#### DefaultBatchComparator (package: comparisonengine)

**Package:** `com.learney.contentaudit.revisiondomain.comparisonengine`
**Visibility:** public
**Implements:** BatchComparator

**Constructor dependencies:**

| Name | Type |
|------|------|
| `courseProjector` | `CourseProjector` |
| `baselineStore` | `BaselineStore` |
| `auditReportStore` | `AuditReportStore` |
| `goalBook` | `GoalBook` |
| `analyzerCatalog` | `AnalyzerCatalog` |
| `auditEngine` | `AuditEngine` |
| `changeAttributor` | `ChangeAttributor` |
| `sampleOutcomeSource` | `SampleOutcomeSource` |
| `topicTeachingResolver` | `TopicTeachingResolver` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |

#### DefaultGoalBook (package: comparisonengine)

**Package:** `com.learney.contentaudit.revisiondomain.comparisonengine`
**Visibility:** public
**Implements:** GoalBook

**Constructor dependencies:**

| Name | Type |
|------|------|
| `declarationSource` | `GoalDeclarationSource` |
| `sealedGoalsStore` | `SealedGoalsStore` |
| `analyzerCatalog` | `AnalyzerCatalog` |

#### LeaveOneOutChangeAttributor (package: comparisonengine)

**Package:** `com.learney.contentaudit.revisiondomain.comparisonengine`
**Visibility:** public
**Implements:** ChangeAttributor

**Constructor dependencies:**

| Name | Type |
|------|------|
| `auditEngine` | `AuditEngine` |

#### DefaultSampleStatistics (package: blindsampleengine)

**Package:** `com.learney.contentaudit.revisiondomain.blindsampleengine`
**Visibility:** public
**Implements:** SampleStatistics

#### DefaultBlindSampleService (package: blindsampleengine)

**Package:** `com.learney.contentaudit.revisiondomain.blindsampleengine`
**Visibility:** public
**Implements:** BlindSampleService

**Constructor dependencies:**

| Name | Type |
|------|------|
| `courseProjector` | `CourseProjector` |
| `batchComparator` | `BatchComparator` |
| `goalBook` | `GoalBook` |
| `baselineStore` | `BaselineStore` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |
| `store` | `BlindSampleStore` |
| `statistics` | `SampleStatistics` |

#### DefaultSampleOutcomeSource (package: blindsampleengine)

**Package:** `com.learney.contentaudit.revisiondomain.blindsampleengine`
**Visibility:** public
**Implements:** SampleOutcomeSource

**Constructor dependencies:**

| Name | Type |
|------|------|
| `store` | `BlindSampleStore` |
| `statistics` | `SampleStatistics` |

#### DefaultBlindErrorRateService (package: blindsampleengine)

**Package:** `com.learney.contentaudit.revisiondomain.blindsampleengine`
**Visibility:** public
**Implements:** BlindErrorRateService

**Constructor dependencies:**

| Name | Type |
|------|------|
| `courseProjector` | `CourseProjector` |
| `baselineStore` | `BaselineStore` |
| `auditReportStore` | `AuditReportStore` |
| `studentViewRenderer` | `QuizStudentViewRenderer` |
| `store` | `BlindSampleStore` |
| `statistics` | `SampleStatistics` |

#### DefaultGateFactory (package: nonregressionengine)

**Package:** `com.learney.contentaudit.revisiondomain.nonregressionengine`
**Visibility:** internal
**Implements:** GateFactory

#### DefaultChangeGate (package: nonregressionengine)

**Package:** `com.learney.contentaudit.revisiondomain.nonregressionengine`
**Visibility:** internal
**Implements:** ChangeGate

#### DefaultBatchGate (package: nonregressionengine)

**Package:** `com.learney.contentaudit.revisiondomain.nonregressionengine`
**Visibility:** internal
**Implements:** BatchGate

#### ScriptedPanelProposer (package: panel)

**Package:** `com.learney.contentaudit.revisiondomain.panel`
**Visibility:** public
**Implements:** PanelProposer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `script` | `Map<String,List<PanelProposal>>` |

#### DefaultPanelRunnerFactory (package: panelengine)

**Package:** `com.learney.contentaudit.revisiondomain.panelengine`
**Visibility:** internal
**Implements:** PanelRunnerFactory

#### DefaultPanelRunner (package: panelengine)

**Package:** `com.learney.contentaudit.revisiondomain.panelengine`
**Visibility:** internal
**Implements:** PanelRunner

**Constructor dependencies:**

| Name | Type |
|------|------|
| `topicPanel` | `TopicPanel` |

#### DefaultTopicPanel (package: panelengine)

**Package:** `com.learney.contentaudit.revisiondomain.panelengine`
**Visibility:** internal
**Implements:** TopicPanel

**Constructor dependencies:**

| Name | Type |
|------|------|
| `proposer` | `PanelProposer` |
| `meter` | `TopicMeter` |
| `screen` | `CandidateScreen` |
| `script` | `PanelDecisionScript` |
| `changeGate` | `ChangeGate` |
| `quizStudentViewRenderer` | `QuizStudentViewRenderer` |

#### CatalogTopicMeter (package: panelengine)

**Package:** `com.learney.contentaudit.revisiondomain.panelengine`
**Visibility:** internal
**Implements:** TopicMeter

**Constructor dependencies:**

| Name | Type |
|------|------|
| `auditEngine` | `AuditEngine` |
| `courseMapper` | `CourseMapper` |
| `nlpTokenizer` | `NlpTokenizer` |
| `lexicalEvaluator` | `SentenceLexicalEvaluator` |
| `topicTeachingResolver` | `TopicTeachingResolver` |
| `spokenTextBuilder` | `SpokenTextBuilder` |
| `sentenceLengthConfig` | `SentenceLengthConfig` |

#### DefaultCandidateScreen (package: panelengine)

**Package:** `com.learney.contentaudit.revisiondomain.panelengine`
**Visibility:** internal
**Implements:** CandidateScreen

**Constructor dependencies:**

| Name | Type |
|------|------|
| `correctionCatalog` | `AnalyzerCorrectionCatalog` |

#### DefaultPanelDecisionScript (package: panelengine)

**Package:** `com.learney.contentaudit.revisiondomain.panelengine`
**Visibility:** internal
**Implements:** PanelDecisionScript

**Constructor dependencies:**

| Name | Type |
|------|------|
| `correctionCatalog` | `AnalyzerCorrectionCatalog` |

#### CappedQueryAllowance (package: panelengine)

**Package:** `com.learney.contentaudit.revisiondomain.panelengine`
**Visibility:** internal
**Implements:** QueryAllowance

**Constructor dependencies:**

| Name | Type |
|------|------|
| `cap` | `Integer` |
| `queriesAtOnce` | `int` |

#### DefaultBatchServiceFactory (package: batchengine)

**Package:** `com.learney.contentaudit.revisiondomain.batchengine`
**Visibility:** internal
**Implements:** BatchServiceFactory

#### DefaultBatchService (package: batchengine)

**Package:** `com.learney.contentaudit.revisiondomain.batchengine`
**Visibility:** internal
**Implements:** BatchService

**Constructor dependencies:**

| Name | Type |
|------|------|
| `batchStore` | `BatchStore` |
| `refinementPlanStore` | `RefinementPlanStore` |
| `auditReportStore` | `AuditReportStore` |
| `activeAnalysisSelectionStore` | `ActiveAnalysisSelectionStore` |
| `baselineStore` | `BaselineStore` |
| `courseRepository` | `CourseRepository` |
| `panelRunnerFactory` | `PanelRunnerFactory` |
| `briefer` | `ErrorTaskBriefer` |
| `taskSelector` | `BatchTaskSelector` |
| `costEstimator` | `BatchCostEstimator` |
| `ruleStage` | `RuleStage` |
| `panelStage` | `PanelStage` |
| `changeLedger` | `BatchChangeLedger` |
| `closer` | `BatchCloser` |
| `publisher` | `BatchPublisher` |
| `reportBuilder` | `BatchReportBuilder` |
| `coursePath` | `Path` |
| `goalBook` | `GoalBook` |

#### DefaultBatchTaskSelector (package: batchengine)

**Package:** `com.learney.contentaudit.revisiondomain.batchengine`
**Visibility:** internal
**Implements:** BatchTaskSelector

**Constructor dependencies:**

| Name | Type |
|------|------|
| `artifactStore` | `RevisionArtifactStore` |
| `correctionCatalog` | `AnalyzerCorrectionCatalog` |

#### DefaultBatchCostEstimator (package: batchengine)

**Package:** `com.learney.contentaudit.revisiondomain.batchengine`
**Visibility:** internal
**Implements:** BatchCostEstimator

#### DefaultRuleStage (package: batchengine)

**Package:** `com.learney.contentaudit.revisiondomain.batchengine`
**Visibility:** internal
**Implements:** RuleStage

**Constructor dependencies:**

| Name | Type |
|------|------|
| `ruleFixes` | `List<RuleFix>` |
| `topicMeter` | `TopicMeter` |
| `changeGate` | `ChangeGate` |
| `changeLedger` | `BatchChangeLedger` |
| `batchStore` | `BatchStore` |

#### DefaultPanelStage (package: batchengine)

**Package:** `com.learney.contentaudit.revisiondomain.batchengine`
**Visibility:** internal
**Implements:** PanelStage

**Constructor dependencies:**

| Name | Type |
|------|------|
| `panelRunner` | `PanelRunner` |
| `briefer` | `ErrorTaskBriefer` |
| `correctionCatalog` | `AnalyzerCorrectionCatalog` |
| `wordLightsQuery` | `TopicWordLightsQuery` |
| `changeLedger` | `BatchChangeLedger` |
| `batchStore` | `BatchStore` |

#### DefaultBatchChangeLedger (package: batchengine)

**Package:** `com.learney.contentaudit.revisiondomain.batchengine`
**Visibility:** internal
**Implements:** BatchChangeLedger

**Constructor dependencies:**

| Name | Type |
|------|------|
| `artifactStore` | `RevisionArtifactStore` |
| `elementLocator` | `CourseElementLocator` |

#### DefaultBatchCloser (package: batchengine)

**Package:** `com.learney.contentaudit.revisiondomain.batchengine`
**Visibility:** internal
**Implements:** BatchCloser

**Constructor dependencies:**

| Name | Type |
|------|------|
| `batchComparator` | `BatchComparator` |
| `batchGate` | `BatchGate` |
| `blindSampleService` | `BlindSampleService` |
| `changeLedger` | `BatchChangeLedger` |
| `batchStore` | `BatchStore` |
| `coursePath` | `Path` |

#### DefaultBatchPublisher (package: batchengine)

**Package:** `com.learney.contentaudit.revisiondomain.batchengine`
**Visibility:** internal
**Implements:** BatchPublisher

**Constructor dependencies:**

| Name | Type |
|------|------|
| `changeLedger` | `BatchChangeLedger` |
| `artifactStore` | `RevisionArtifactStore` |
| `courseRepository` | `CourseRepository` |
| `courseMapper` | `CourseMapper` |
| `auditEngine` | `AuditEngine` |
| `auditReportStore` | `AuditReportStore` |
| `refinerEngine` | `RefinerEngine` |
| `refinementPlanStore` | `RefinementPlanStore` |
| `activeAnalysisSelectionStore` | `ActiveAnalysisSelectionStore` |
| `batchComparator` | `BatchComparator` |
| `correctionScope` | `CorrectionScope` |
| `preservationCheck` | `PreservationCheck` |
| `coursePath` | `Path` |

#### DefaultBatchReportBuilder (package: batchengine)

**Package:** `com.learney.contentaudit.revisiondomain.batchengine`
**Visibility:** internal
**Implements:** BatchReportBuilder

#### DefaultTrayServiceFactory (package: trayengine)

**Package:** `com.learney.contentaudit.revisiondomain.trayengine`
**Visibility:** internal
**Implements:** TrayServiceFactory

#### DefaultTrayService (package: trayengine)

**Package:** `com.learney.contentaudit.revisiondomain.trayengine`
**Visibility:** internal
**Implements:** TrayService

**Constructor dependencies:**

| Name | Type |
|------|------|
| `batchService` | `BatchService` |
| `batchStore` | `BatchStore` |
| `blindSampleService` | `BlindSampleService` |
| `panelRunner` | `PanelRunner` |
| `panelRunnerFactory` | `PanelRunnerFactory` |
| `assembler` | `TrayAssembler` |
| `memory` | `DecisionMemory` |
| `courseRepository` | `CourseRepository` |
| `coursePath` | `Path` |

#### DefaultTrayAssembler (package: trayengine)

**Package:** `com.learney.contentaudit.revisiondomain.trayengine`
**Visibility:** internal
**Implements:** TrayAssembler

**Constructor dependencies:**

| Name | Type |
|------|------|
| `ruleSampleDrawer` | `RuleSampleDrawer` |
| `memory` | `DecisionMemory` |
| `quizStudentViewRenderer` | `QuizStudentViewRenderer` |

#### SeededRuleSampleDrawer (package: trayengine)

**Package:** `com.learney.contentaudit.revisiondomain.trayengine`
**Visibility:** internal
**Implements:** RuleSampleDrawer

#### FingerprintDecisionMemory (package: trayengine)

**Package:** `com.learney.contentaudit.revisiondomain.trayengine`
**Visibility:** internal
**Implements:** DecisionMemory

**Constructor dependencies:**

| Name | Type |
|------|------|
| `decisionStore` | `TrayDecisionStore` |
| `contentFingerprinter` | `ContentFingerprinter` |

### Module: revision-infrastructure

#### DefaultLemmaAbsenceAgentGeneratorFactory (package: lemmaabsenceagent)

**Package:** `com.learney.contentaudit.revisioninfrastructure.lemmaabsenceagent`
**Visibility:** internal
**Implements:** LemmaAbsenceAgentGeneratorFactory

#### LemmaAbsenceAgentGenerator (package: lemmaabsenceagent)

**Package:** `com.learney.contentaudit.revisioninfrastructure.lemmaabsenceagent`
**Visibility:** internal
**Implements:** LemmaAbsenceQuizCandidateGenerator

**Constructor dependencies:**

| Name | Type |
|------|------|
| `config` | `LagenConfig` |
| `agentRuntimeLauncher` | `AgentRuntimeLauncher` |
| `sessionFactory` | `SuggestedLemmaQuerySessionFactory` |
| `errorClassifier` | `AgentRuntimeErrorClassifier` |
| `candidateParser` | `AgentCandidateParser` |
| `chatModel` | `ChatModel` |
| `strategyName` | `String` |

#### DefaultSuggestedLemmaAgentTool (package: lemmaabsenceagent)

**Package:** `com.learney.contentaudit.revisioninfrastructure.lemmaabsenceagent`
**Visibility:** internal
**Implements:** SuggestedLemmaAgentTool

**Constructor dependencies:**

| Name | Type |
|------|------|
| `session` | `SuggestedLemmaQuerySession` |

#### DefaultAgentRuntimeErrorClassifier (package: lemmaabsenceagent)

**Package:** `com.learney.contentaudit.revisioninfrastructure.lemmaabsenceagent`
**Visibility:** internal
**Implements:** AgentRuntimeErrorClassifier

#### DefaultAgentCandidateParser (package: lemmaabsenceagent)

**Package:** `com.learney.contentaudit.revisioninfrastructure.lemmaabsenceagent`
**Visibility:** internal
**Implements:** AgentCandidateParser

#### SharedRuntimeAgentLauncher (package: lemmaabsenceagent)

**Package:** `com.learney.contentaudit.revisioninfrastructure.lemmaabsenceagent`
**Visibility:** internal
**Implements:** AgentRuntimeLauncher

**Constructor dependencies:**

| Name | Type |
|------|------|
| `agentGraphRunner` | `AgentGraphRunner` |

#### DefaultKnowledgeTitleAgentGeneratorFactory (package: knowledgetitleagent)

**Package:** `com.learney.contentaudit.revisioninfrastructure.knowledgetitleagent`
**Visibility:** public
**Implements:** KnowledgeTitleAgentGeneratorFactory

#### KnowledgeTitleAgentGenerator (package: knowledgetitleagent)

**Package:** `com.learney.contentaudit.revisioninfrastructure.knowledgetitleagent`
**Visibility:** public
**Implements:** KnowledgeTitleCandidateGenerator

**Constructor dependencies:**

| Name | Type |
|------|------|
| `agentRuntimeLauncher` | `KnowledgeTitleAgentRuntimeLauncher` |
| `candidateParser` | `KnowledgeTitleAgentCandidateParser` |
| `errorClassifier` | `KnowledgeTitleAgentErrorClassifier` |
| `chatModel` | `ChatModel` |
| `strategyName` | `String` |
| `config` | `LagenConfig` |
| `projectRoot` | `Path` |

#### DefaultKnowledgeTitleAgentCandidateParser (package: knowledgetitleagent)

**Package:** `com.learney.contentaudit.revisioninfrastructure.knowledgetitleagent`
**Visibility:** public
**Implements:** KnowledgeTitleAgentCandidateParser

#### DefaultKnowledgeTitleAgentErrorClassifier (package: knowledgetitleagent)

**Package:** `com.learney.contentaudit.revisioninfrastructure.knowledgetitleagent`
**Visibility:** public
**Implements:** KnowledgeTitleAgentErrorClassifier

#### SharedRuntimeKnowledgeTitleLauncher (package: knowledgetitleagent)

**Package:** `com.learney.contentaudit.revisioninfrastructure.knowledgetitleagent`
**Visibility:** public
**Implements:** KnowledgeTitleAgentRuntimeLauncher

**Constructor dependencies:**

| Name | Type |
|------|------|
| `agentGraphRunner` | `AgentGraphRunner` |

#### DefaultQuizInstructionAgentGeneratorFactory (package: quizinstructionagent)

**Package:** `com.learney.contentaudit.revisioninfrastructure.quizinstructionagent`
**Visibility:** public
**Implements:** QuizInstructionAgentGeneratorFactory

#### QuizInstructionAgentGenerator (package: quizinstructionagent)

**Package:** `com.learney.contentaudit.revisioninfrastructure.quizinstructionagent`
**Visibility:** public
**Implements:** QuizInstructionCandidateGenerator

**Constructor dependencies:**

| Name | Type |
|------|------|
| `agentRuntimeLauncher` | `QuizInstructionAgentRuntimeLauncher` |
| `candidateParser` | `QuizInstructionAgentCandidateParser` |
| `errorClassifier` | `QuizInstructionAgentErrorClassifier` |
| `chatModel` | `ChatModel` |
| `config` | `LagenConfig` |
| `projectRoot` | `Path` |

#### DefaultQuizInstructionAgentCandidateParser (package: quizinstructionagent)

**Package:** `com.learney.contentaudit.revisioninfrastructure.quizinstructionagent`
**Visibility:** public
**Implements:** QuizInstructionAgentCandidateParser

#### DefaultQuizInstructionAgentErrorClassifier (package: quizinstructionagent)

**Package:** `com.learney.contentaudit.revisioninfrastructure.quizinstructionagent`
**Visibility:** public
**Implements:** QuizInstructionAgentErrorClassifier

#### SharedRuntimeQuizInstructionLauncher (package: quizinstructionagent)

**Package:** `com.learney.contentaudit.revisioninfrastructure.quizinstructionagent`
**Visibility:** public
**Implements:** QuizInstructionAgentRuntimeLauncher

**Constructor dependencies:**

| Name | Type |
|------|------|
| `agentGraphRunner` | `AgentGraphRunner` |

#### DefaultCandidateJudgmentEvaluatorFactory (package: candidatejudgment)

**Package:** `com.learney.contentaudit.revisioninfrastructure.candidatejudgment`
**Visibility:** public
**Implements:** CandidateJudgmentEvaluatorFactory

#### AgentCandidateJudgmentEvaluator (package: candidatejudgment)

**Package:** `com.learney.contentaudit.revisioninfrastructure.candidatejudgment`
**Visibility:** public
**Implements:** CandidateCriterionEvaluator

**Constructor dependencies:**

| Name | Type |
|------|------|
| `criterion` | `CorrectionCriterion` |
| `agentRuntimeLauncher` | `CandidateJudgmentRuntimeLauncher` |
| `verdictParser` | `CandidateJudgmentVerdictParser` |
| `chatModel` | `ChatModel` |
| `config` | `LagenConfig` |

#### DefaultCandidateJudgmentVerdictParser (package: candidatejudgment)

**Package:** `com.learney.contentaudit.revisioninfrastructure.candidatejudgment`
**Visibility:** public
**Implements:** CandidateJudgmentVerdictParser

#### SharedRuntimeCandidateJudgmentLauncher (package: candidatejudgment)

**Package:** `com.learney.contentaudit.revisioninfrastructure.candidatejudgment`
**Visibility:** public
**Implements:** CandidateJudgmentRuntimeLauncher

**Constructor dependencies:**

| Name | Type |
|------|------|
| `agentGraphRunner` | `AgentGraphRunner` |

#### DefaultPanelAgentProposerFactory (package: panelagent)

**Package:** `com.learney.contentaudit.revisioninfrastructure.panelagent`
**Visibility:** public
**Implements:** PanelAgentProposerFactory

#### AgentPanelProposer (package: panelagent)

**Package:** `com.learney.contentaudit.revisioninfrastructure.panelagent`
**Visibility:** public
**Implements:** PanelProposer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `agentRuntimeLauncher` | `PanelAgentRuntimeLauncher` |
| `proposalParser` | `PanelAgentProposalParser` |
| `chatModel` | `ChatModel` |
| `config` | `LagenConfig` |
| `projectRoot` | `Path` |

#### DefaultPanelAgentProposalParser (package: panelagent)

**Package:** `com.learney.contentaudit.revisioninfrastructure.panelagent`
**Visibility:** public
**Implements:** PanelAgentProposalParser

#### SharedRuntimePanelAgentLauncher (package: panelagent)

**Package:** `com.learney.contentaudit.revisioninfrastructure.panelagent`
**Visibility:** public
**Implements:** PanelAgentRuntimeLauncher

**Constructor dependencies:**

| Name | Type |
|------|------|
| `agentGraphRunner` | `AgentGraphRunner` |

### Module: evaluation-ledger-domain

#### DefaultEvaluationSessionFactory (package: evaluationsession)

**Package:** `com.learney.contentaudit.evaluationledgerdomain.evaluationsession`
**Visibility:** public
**Implements:** EvaluationSessionFactory

**Constructor dependencies:**

| Name | Type |
|------|------|
| `ledger` | `EvaluationLedger` |
| `fingerprinter` | `ContentFingerprinter` |

#### DefaultEvaluationSession (package: evaluationsession)

**Package:** `com.learney.contentaudit.evaluationledgerdomain.evaluationsession`
**Visibility:** public
**Implements:** EvaluationSession

**Constructor dependencies:**

| Name | Type |
|------|------|
| `ledger` | `EvaluationLedger` |
| `fingerprinter` | `ContentFingerprinter` |
| `evaluator` | `Evaluator` |
| `budget` | `EvaluationBudget` |

#### ExamEvaluationSessionFactory (package: evaluationsession)

**Package:** `com.learney.contentaudit.evaluationledgerdomain.evaluationsession`
**Visibility:** public
**Implements:** EvaluationSessionFactory

**Constructor dependencies:**

| Name | Type |
|------|------|
| `ledger` | `EvaluationLedger` |
| `fingerprinter` | `ContentFingerprinter` |

#### ExamScopedEvaluationLedger (package: evaluationsession)

**Package:** `com.learney.contentaudit.evaluationledgerdomain.evaluationsession`
**Visibility:** public
**Implements:** EvaluationLedger

**Constructor dependencies:**

| Name | Type |
|------|------|
| `ledger` | `EvaluationLedger` |
| `pinnedVersion` | `String` |

#### Sha256ContentFingerprinter (package: contentfingerprint)

**Package:** `com.learney.contentaudit.evaluationledgerdomain.contentfingerprint`
**Visibility:** public
**Implements:** ContentFingerprinter

### Module: evaluation-ledger-infrastructure

#### FileSystemEvaluationLedger

**Package:** `com.learney.contentaudit.evaluationledgerinfrastructure`

**Implements:** EvaluationLedger, EvaluationTimeline

**Framework types:** Repository

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `baseDir` | `Path` |

**Generated constructor:**
```java
public FileSystemEvaluationLedger(Path baseDir) {
    this.baseDir = baseDir;
}
```

### Module: agent-runtime-infrastructure

#### DefaultAgentGraphRunnerFactory (package: graphexecution)

**Package:** `com.learney.contentaudit.agentruntimeinfrastructure.graphexecution`
**Visibility:** public
**Implements:** AgentGraphRunnerFactory

#### DefaultAgentGraphRunner (package: graphexecution)

**Package:** `com.learney.contentaudit.agentruntimeinfrastructure.graphexecution`
**Visibility:** public
**Implements:** AgentGraphRunner

**Constructor dependencies:**

| Name | Type |
|------|------|
| `config` | `AgentGraphRunnerConfig` |
| `agentDefinitionLocator` | `AgentDefinitionLocator` |

#### DefaultAgentDefinitionLocatorFactory (package: graphexecution)

**Package:** `com.learney.contentaudit.agentruntimeinfrastructure.graphexecution`
**Visibility:** public
**Implements:** AgentDefinitionLocatorFactory

#### DefaultAgentDefinitionLocator (package: graphexecution)

**Package:** `com.learney.contentaudit.agentruntimeinfrastructure.graphexecution`
**Visibility:** public
**Implements:** AgentDefinitionLocator

**Constructor dependencies:**

| Name | Type |
|------|------|
| `config` | `AgentGraphRunnerConfig` |

#### DefaultLlmProviderResolver (package: llmprovider)

**Package:** `com.learney.contentaudit.agentruntimeinfrastructure.llmprovider`
**Visibility:** public
**Implements:** LlmProviderResolver

### Module: quiz-instruction-infrastructure

#### DefaultQuizInstructionJudgeFactory (package: instructionjudge)

**Package:** `com.learney.contentaudit.quizinstructioninfrastructure.instructionjudge`
**Visibility:** public
**Implements:** QuizInstructionJudgeFactory

**Constructor dependencies:**

| Name | Type |
|------|------|
| `llmProviderResolver` | `LlmProviderResolver` |
| `readinessProbe` | `JudgeProviderReadinessProbe` |

#### QuizInstructionAgentJudge (package: instructionjudge)

**Package:** `com.learney.contentaudit.quizinstructioninfrastructure.instructionjudge`
**Visibility:** public
**Implements:** Evaluator

**Constructor dependencies:**

| Name | Type |
|------|------|
| `config` | `QuizInstructionJudgeConfig` |
| `agentGraphRunner` | `AgentGraphRunner` |
| `versionResolver` | `QuizInstructionJudgeVersionResolver` |
| `chatModelFactory` | `JudgeChatModelFactory` |
| `provider` | `LlmProviderResolved` |
| `readinessProbe` | `JudgeProviderReadinessProbe` |

#### DefaultQuizInstructionJudgeVersionResolver (package: instructionjudge)

**Package:** `com.learney.contentaudit.quizinstructioninfrastructure.instructionjudge`
**Visibility:** public
**Implements:** QuizInstructionJudgeVersionResolver

**Constructor dependencies:**

| Name | Type |
|------|------|
| `agentDefinitionLocator` | `AgentDefinitionLocator` |

#### SentinelAgentsJudgeChatModelFactory (package: instructionjudge)

**Package:** `com.learney.contentaudit.quizinstructioninfrastructure.instructionjudge`
**Visibility:** public
**Implements:** JudgeChatModelFactory

#### JacksonQuizInstructionVerdictReader (package: instructionverdict)

**Package:** `com.learney.contentaudit.quizinstructioninfrastructure.instructionverdict`
**Visibility:** public
**Implements:** QuizInstructionVerdictReader

#### DefaultAgentJudgeFactory (package: agentjudge)

**Package:** `com.learney.contentaudit.quizinstructioninfrastructure.agentjudge`
**Visibility:** public
**Implements:** AgentJudgeFactory

**Constructor dependencies:**

| Name | Type |
|------|------|
| `llmProviderResolver` | `LlmProviderResolver` |
| `readinessProbe` | `JudgeProviderReadinessProbe` |

#### AgentJudge (package: agentjudge)

**Package:** `com.learney.contentaudit.quizinstructioninfrastructure.agentjudge`
**Visibility:** public
**Implements:** Evaluator

**Constructor dependencies:**

| Name | Type |
|------|------|
| `spec` | `AgentJudgeSpec` |
| `config` | `AgentJudgeConfig` |
| `agentGraphRunner` | `AgentGraphRunner` |
| `versionResolver` | `AgentJudgeVersionResolver` |
| `chatModelFactory` | `AgentJudgeChatModelFactory` |
| `provider` | `LlmProviderResolved` |
| `readinessProbe` | `JudgeProviderReadinessProbe` |

#### DefaultAgentJudgeVersionResolver (package: agentjudge)

**Package:** `com.learney.contentaudit.quizinstructioninfrastructure.agentjudge`
**Visibility:** public
**Implements:** AgentJudgeVersionResolver

**Constructor dependencies:**

| Name | Type |
|------|------|
| `agentDefinitionLocator` | `AgentDefinitionLocator` |

#### SentinelAgentsAgentJudgeChatModelFactory (package: agentjudge)

**Package:** `com.learney.contentaudit.quizinstructioninfrastructure.agentjudge`
**Visibility:** public
**Implements:** AgentJudgeChatModelFactory

#### DefaultJudgeProviderReadinessProbe (package: agentjudge)

**Package:** `com.learney.contentaudit.quizinstructioninfrastructure.agentjudge`
**Visibility:** public
**Implements:** JudgeProviderReadinessProbe

**Constructor dependencies:**

| Name | Type |
|------|------|
| `authStatusCommand` | `List<String>` |

#### JacksonTranslationSenseVerdictReader (package: translationsenseverdict)

**Package:** `com.learney.contentaudit.quizinstructioninfrastructure.translationsenseverdict`
**Visibility:** public
**Implements:** TranslationSenseVerdictReader

#### JacksonSingleAnswerVerdictReader (package: singleanswerverdict)

**Package:** `com.learney.contentaudit.quizinstructioninfrastructure.singleanswerverdict`
**Visibility:** public
**Implements:** SingleAnswerVerdictReader

#### JacksonMiniTheoryVerdictReader (package: minitheoryverdict)

**Package:** `com.learney.contentaudit.quizinstructioninfrastructure.minitheoryverdict`
**Visibility:** public
**Implements:** MiniTheoryVerdictReader

#### JacksonChoiceAlternativeVerdictReader (package: choicealternativeverdict)

**Package:** `com.learney.contentaudit.quizinstructioninfrastructure.choicealternativeverdict`
**Visibility:** public
**Implements:** ChoiceAlternativeVerdictReader

### Module: local-language-infrastructure

#### LanguageToolGrammarChecker (package: languagetool)

**Package:** `com.learney.contentaudit.locallanguageinfrastructure.languagetool`
**Visibility:** public
**Implements:** GrammarChecker

#### DefaultOllamaModelFactory (package: ollama)

**Package:** `com.learney.contentaudit.locallanguageinfrastructure.ollama`
**Visibility:** public
**Implements:** OllamaModelFactory

#### HttpOllamaApi (package: ollama)

**Package:** `com.learney.contentaudit.locallanguageinfrastructure.ollama`
**Visibility:** public
**Implements:** OllamaApi

**Constructor dependencies:**

| Name | Type |
|------|------|
| `endpoint` | `OllamaEndpoint` |

#### OllamaYesNoModel (package: ollama)

**Package:** `com.learney.contentaudit.locallanguageinfrastructure.ollama`
**Visibility:** public
**Implements:** YesNoModel

**Constructor dependencies:**

| Name | Type |
|------|------|
| `api` | `OllamaApi` |
| `model` | `String` |

#### OllamaEmbeddingModel (package: ollama)

**Package:** `com.learney.contentaudit.locallanguageinfrastructure.ollama`
**Visibility:** public
**Implements:** EmbeddingModel

**Constructor dependencies:**

| Name | Type |
|------|------|
| `api` | `OllamaApi` |
| `model` | `String` |

