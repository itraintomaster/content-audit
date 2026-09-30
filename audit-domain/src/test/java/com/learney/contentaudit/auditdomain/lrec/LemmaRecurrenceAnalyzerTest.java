package com.learney.contentaudit.auditdomain.lrec;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.NlpToken;
import com.learney.contentaudit.auditdomain.finding.FindingDraft;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.learney.contentaudit.auditdomain.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LemmaRecurrenceAnalyzerTest {

    @Mock private ContentWordFilter contentWordFilter;
    @Mock private LemmaRecurrenceConfig config;
    @Mock private IntervalCalculator intervalCalculator;
    @Mock private ExposureClassifier exposureClassifier;
    @InjectMocks private LemmaRecurrenceAnalyzer sut;

    private AuditNode makeNode(AuditTarget target, AuditableEntity entity) {
        AuditNode node = new AuditNode();
        node.setTarget(target);
        node.setEntity(entity);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        return node;
    }

    @Test
    @DisplayName("Given a LemmaRecurrenceAnalyzer, when getName is called, then returns lemma-recurrence")
    @Tag("F-LREC") @Tag("F-LREC-R015")
    public void givenALemmaRecurrenceAnalyzerWhenGetNameIsCalledThenReturnsLemmarecurrence() {
        assertEquals("lemma-recurrence", sut.getName());
    }

    @Test
    @DisplayName("Given a LemmaRecurrenceAnalyzer, when getTarget is called, then returns COURSE")
    @Tag("F-LREC") @Tag("F-LREC-R013")
    public void givenALemmaRecurrenceAnalyzerWhenGetTargetIsCalledThenReturnsCOURSE() {
        assertEquals(AuditTarget.COURSE, sut.getTarget());
    }

    @Test
    @DisplayName("Given no quizzes have been processed, when onCourseComplete is called, then score is 0")
    @Tag("F-LREC")
    public void givenNoQuizzesHaveBeenProcessedWhenOnCourseCompleteIsCalledThenScoreIsZero() {
        AuditNode root = makeNode(AuditTarget.COURSE, null);
        sut.onCourseComplete(root);
        assertEquals(0.0, root.getScores().getOrDefault("lemma-recurrence", 0.0));
    }

    @Test
    @DisplayName("Given an AuditableKnowledge, when onKnowledge is called, then completes without error")
    public void givenAnAuditableKnowledgeWhenOnKnowledgeIsCalledThenCompletesWithoutError() {
        AuditableKnowledge k = new AuditableKnowledge(null, "t", "i", true, "k1", "l", "c", null, null);
        assertDoesNotThrow(() -> sut.onKnowledge(makeNode(AuditTarget.KNOWLEDGE, k)));
    }

    @Test
    @DisplayName("Given an AuditableTopic, when onTopic is called, then completes without error")
    public void givenAnAuditableTopicWhenOnTopicIsCalledThenCompletesWithoutError() {
        AuditableTopic t = new AuditableTopic(null, "t1", "l", "c");
        assertDoesNotThrow(() -> sut.onTopic(makeNode(AuditTarget.TOPIC, t)));
    }

    @Test
    @DisplayName("Given an AuditableMilestone, when onMilestone is called, then completes without error")
    public void givenAnAuditableMilestoneWhenOnMilestoneIsCalledThenCompletesWithoutError() {
        AuditableMilestone m = new AuditableMilestone(null, "m1", "A1", "c");
        assertDoesNotThrow(() -> sut.onMilestone(makeNode(AuditTarget.MILESTONE, m)));
    }

    @Test
    @DisplayName("should assign a globally sequential position counter incremented by one per token traversed across every quiz in the course")
    @Tag("FEAT-LREC")
    @Tag("F-LREC-R001")
    public void shouldAssignAGloballySequentialPositionCounterIncrementedByOnePerTokenTraversedAcrossEveryQuizInTheCourse() {
        // R001: each token increments the global counter by 1, all tokens count (not just content words).
        // quiz1: [DET, NOUN] → global positions 1, 2; NOUN "cat" is content word → registered at pos 2
        // quiz2: [DET, NOUN] → global positions 3, 4; NOUN "cat" is content word → registered at pos 4
        // IntervalCalculator.calculateMeanInterval([2, 4]) called → positions are non-contiguous (global gap = 2)
        NlpToken detToken = new NlpToken("the", "the", "DET", 1, true, false);
        NlpToken catToken = new NlpToken("cat", "cat", "NOUN", 100, false, false);
        AuditableQuiz quiz1 = new AuditableQuiz(List.of(detToken, catToken), null, null, null, null, List.of("the cat"), null, null, null, null, null, null);
        AuditableQuiz quiz2 = new AuditableQuiz(List.of(detToken, catToken), null, null, null, null, List.of("the cat"), null, null, null, null, null, null);

        when(contentWordFilter.isContentWord(argThat(t -> t != null && "DET".equals(t.getPosTag())))).thenReturn(false);
        when(contentWordFilter.isContentWord(argThat(t -> t != null && "NOUN".equals(t.getPosTag())))).thenReturn(true);
        when(config.getTop()).thenReturn(2000);
        when(intervalCalculator.calculateMeanInterval(any())).thenReturn(2.0);
        when(intervalCalculator.calculateStdDevInterval(any(), anyDouble())).thenReturn(0.0);
        when(exposureClassifier.classify(anyDouble(), any())).thenReturn(ExposureStatus.NORMAL);

        AuditNode quizNode1 = makeNode(AuditTarget.QUIZ, new AuditableQuiz(quiz1.getTokens(), null, null, null, null, List.of("the cat"), null, null, null, null, null, null));
        AuditNode quizNode2 = makeNode(AuditTarget.QUIZ, new AuditableQuiz(quiz2.getTokens(), null, null, null, null, List.of("the cat"), null, null, null, null, null, null));
        AuditNode root = makeNode(AuditTarget.COURSE, null);

        sut.onQuiz(quizNode1);
        sut.onQuiz(quizNode2);

        // Capture positions passed to intervalCalculator to verify sequential global counter
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Integer>> posCaptor = ArgumentCaptor.forClass((Class) List.class);
        sut.onCourseComplete(root);
        verify(intervalCalculator, atLeastOnce()).calculateMeanInterval(posCaptor.capture());

        List<Integer> positions = posCaptor.getValue();
        assertEquals(2, positions.size(), "R001: 'cat' appears in 2 quizzes, so 2 positions");
        // Position gap = 2 because each quiz had 2 tokens (DET+NOUN), both counted in global counter
        // quiz1: DET→pos1, NOUN→pos2 (registered). quiz2: DET→pos3, NOUN→pos4 (registered)
        assertEquals(2, positions.get(0), "R001: first occurrence of 'cat' at global position 2 (after 1 DET)");
        assertEquals(4, positions.get(1), "R001: second occurrence at global position 4 (2 tokens per quiz)");
    }

    @Test
    @DisplayName("should register each content-word token under its lemma key in the lemma-positions map preserving the global position and grouping inflected forms by their lemmatized form")
    @Tag("FEAT-LREC")
    @Tag("F-LREC-R004")
    public void shouldRegisterEachContentwordTokenUnderItsLemmaKeyInTheLemmapositionsMapPreservingTheGlobalPositionAndGroupingInflectedFormsByTheirLemmatizedForm() {
        // R004: inflected forms "runs" and "running" both lemmatize to "run" → same lemma key.
        // Two quizzes, each with one inflected form of "run" → positions registered under lemma "run".
        // intervalCalculator called once for "run" with 2 positions.
        NlpToken runs = new NlpToken("runs", "run", "VERB", 200, false, false);
        NlpToken running = new NlpToken("running", "run", "VERB", 200, false, false);

        when(contentWordFilter.isContentWord(any())).thenReturn(true);
        when(config.getTop()).thenReturn(2000);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Integer>> posCaptor = ArgumentCaptor.forClass((Class) List.class);
        when(intervalCalculator.calculateMeanInterval(posCaptor.capture())).thenReturn(5.0);
        when(intervalCalculator.calculateStdDevInterval(any(), anyDouble())).thenReturn(0.0);
        when(exposureClassifier.classify(anyDouble(), any())).thenReturn(ExposureStatus.NORMAL);

        AuditNode quizNode1 = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(runs), null, null, null, null, List.of("runs"), null, null, null, null, null, null));
        AuditNode quizNode2 = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(running), null, null, null, null, List.of("running"), null, null, null, null, null, null));
        AuditNode root = makeNode(AuditTarget.COURSE, null);

        sut.onQuiz(quizNode1);
        sut.onQuiz(quizNode2);
        sut.onCourseComplete(root);

        // intervalCalculator called exactly once — both inflected forms group under lemma "run"
        verify(intervalCalculator, times(1)).calculateMeanInterval(any());
        List<Integer> positions = posCaptor.getValue();
        assertEquals(2, positions.size(), "R004: both inflected forms of 'run' registered under same lemma, 2 positions total");
        assertTrue(positions.get(0) < positions.get(1), "R004: positions ordered ascending");
    }

    @Test
    @DisplayName("should restrict the analysed lemmas to the top N most frequent ones according to the configured top parameter discarding the least frequent lemmas beyond that limit")
    @Tag("FEAT-LREC")
    @Tag("F-LREC-R005")
    public void shouldRestrictTheAnalysedLemmasToTheTopNMostFrequentOnesAccordingToTheConfiguredTopParameterDiscardingTheLeastFrequentLemmasBeyondThatLimit() {
        // R005: top=1 → only the most frequent lemma is analysed.
        // "cat" appears 3 times, "dog" appears 2 times → only "cat" is processed (top=1).
        NlpToken cat = new NlpToken("cat", "cat", "NOUN", 100, false, false);
        NlpToken dog = new NlpToken("dog", "dog", "NOUN", 80, false, false);

        when(contentWordFilter.isContentWord(any())).thenReturn(true);
        when(config.getTop()).thenReturn(1); // only top-1 lemma

        when(intervalCalculator.calculateMeanInterval(any())).thenReturn(10.0);
        when(intervalCalculator.calculateStdDevInterval(any(), anyDouble())).thenReturn(0.0);
        when(exposureClassifier.classify(anyDouble(), any())).thenReturn(ExposureStatus.NORMAL);

        // 3 "cat" occurrences, 2 "dog" occurrences
        for (int i = 0; i < 3; i++) {
            AuditNode qn = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(cat), null, null, null, null, List.of("cat"), null, null, null, null, null, null));
            sut.onQuiz(qn);
        }
        for (int i = 0; i < 2; i++) {
            AuditNode qn = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(dog), null, null, null, null, List.of("dog"), null, null, null, null, null, null));
            sut.onQuiz(qn);
        }

        AuditNode root = makeNode(AuditTarget.COURSE, null);
        sut.onCourseComplete(root);

        // top=1 → intervalCalculator called exactly once (for "cat" only, the most frequent)
        verify(intervalCalculator, times(1)).calculateMeanInterval(any());
    }

    @Test
    @DisplayName("should skip from the per-lemma interval calculation any lemma whose registered position list has fewer than two entries")
    @Tag("FEAT-LREC")
    @Tag("F-LREC-R006")
    public void shouldSkipFromThePerlemmaIntervalCalculationAnyLemmaWhoseRegisteredPositionListHasFewerThanTwoEntries() {
        // R006: a lemma with only 1 occurrence → no interval can be calculated → intervalCalculator NOT called.
        NlpToken cat = new NlpToken("cat", "cat", "NOUN", 100, false, false);

        when(contentWordFilter.isContentWord(any())).thenReturn(true);
        when(config.getTop()).thenReturn(2000);

        AuditNode quizNode = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(cat), null, null, null, null, List.of("cat"), null, null, null, null, null, null));
        AuditNode root = makeNode(AuditTarget.COURSE, null);

        sut.onQuiz(quizNode);
        sut.onCourseComplete(root);

        // "cat" has only 1 occurrence → cannot compute intervals → intervalCalculator never called
        verify(intervalCalculator, never()).calculateMeanInterval(any());
    }

    @Test
    @DisplayName("should classify each analysed lemma as NORMAL when overExposed < meanInterval <= subExposed as OVER_EXPOSED when meanInterval <= overExposed and as SUB_EXPOSED when meanInterval > subExposed using the configured thresholds")
    @Tag("FEAT-LREC")
    @Tag("F-LREC-R008")
    public void shouldClassifyEachAnalysedLemmaAsNORMALWhenOverExposedMeanIntervalSubExposedAsOVEREXPOSEDWhenMeanIntervalOverExposedAndAsSUBEXPOSEDWhenMeanIntervalSubExposedUsingTheConfiguredThresholds() {
        // R008: ExposureClassifier.classify(meanInterval, config) determines the status for each lemma.
        // The overall score = normalCount / totalCount.
        // Case: 1 lemma, meanInterval returned → classify returns OVER_EXPOSED → score = 0/1 = 0.0
        NlpToken cat = new NlpToken("cat", "cat", "NOUN", 100, false, false);

        when(contentWordFilter.isContentWord(any())).thenReturn(true);
        when(config.getTop()).thenReturn(2000);
        when(intervalCalculator.calculateMeanInterval(any())).thenReturn(30.0); // would be over-exposed (<=50)
        when(intervalCalculator.calculateStdDevInterval(any(), anyDouble())).thenReturn(0.0);
        // Classifier sees meanInterval=30.0 and returns OVER_EXPOSED
        when(exposureClassifier.classify(anyDouble(), any())).thenReturn(ExposureStatus.OVER_EXPOSED);

        AuditNode q1 = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(cat), null, null, null, null, List.of("cat"), null, null, null, null, null, null));
        AuditNode q2 = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(cat), null, null, null, null, List.of("cat"), null, null, null, null, null, null));
        AuditNode root = makeNode(AuditTarget.COURSE, null);

        sut.onQuiz(q1);
        sut.onQuiz(q2);
        sut.onCourseComplete(root);

        // exposureClassifier was called → classification drives the score
        verify(exposureClassifier, times(1)).classify(anyDouble(), any());
        // OVER_EXPOSED → 0 normal lemmas out of 1 → score = 0.0
        assertEquals(0.0, root.getScores().get("lemma-recurrence"), 0.001,
                "R008: OVER_EXPOSED classification → normalCount=0 → score=0.0");
    }

    @Test
    @DisplayName("should produce an ExposureSummary whose normalCount plus subExposedCount plus overExposedCount equals the total count of lemmas analysed")
    @Tag("FEAT-LREC")
    @Tag("F-LREC-R009")
    public void shouldProduceAnExposureSummaryWhoseNormalCountPlusSubExposedCountPlusOverExposedCountEqualsTheTotalCountOfLemmasAnalysed() {
        // R009: normalCount + subExposedCount + overExposedCount = totalCount (all analysed lemmas).
        // 3 lemmas: "cat" (NORMAL), "dog" (SUB_EXPOSED), "run" (OVER_EXPOSED) → score = 1/3 ≈ 0.33
        NlpToken cat = new NlpToken("cat", "cat", "NOUN", 100, false, false);
        NlpToken dog = new NlpToken("dog", "dog", "NOUN", 80, false, false);
        NlpToken run = new NlpToken("run", "run", "VERB", 200, false, false);

        when(contentWordFilter.isContentWord(any())).thenReturn(true);
        when(config.getTop()).thenReturn(2000);
        when(intervalCalculator.calculateMeanInterval(any())).thenReturn(100.0);
        when(intervalCalculator.calculateStdDevInterval(any(), anyDouble())).thenReturn(0.0);
        // Assign different statuses per lemma — use argument matching on meanInterval
        when(exposureClassifier.classify(eq(100.0), any()))
            .thenReturn(ExposureStatus.NORMAL, ExposureStatus.SUB_EXPOSED, ExposureStatus.OVER_EXPOSED);

        // Each lemma needs 2 occurrences to be analysed
        for (NlpToken t : List.of(cat, cat, dog, dog, run, run)) {
            AuditNode qn = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(t), null, null, null, null, List.of(t.getText()), null, null, null, null, null, null));
            sut.onQuiz(qn);
        }

        AuditNode root = makeNode(AuditTarget.COURSE, null);
        sut.onCourseComplete(root);

        // 3 lemmas analysed: 1 NORMAL, 1 SUB_EXPOSED, 1 OVER_EXPOSED
        // R009: 1+1+1 = 3 = totalCount ✓ — verified by score = normalCount/totalCount = 1/3
        double score = root.getScores().get("lemma-recurrence");
        assertEquals(0.33, score, 0.01, "R009: score = 1/3 ≈ 0.33 (1 NORMAL out of 3 analysed)");
    }

    @Test
    @DisplayName("should compute the overall lemma-recurrence score as normalCount divided by totalCount rounded to two decimals and return 0.0 when totalCount is zero")
    @Tag("FEAT-LREC")
    @Tag("F-LREC-R010")
    public void shouldComputeTheOverallLemmarecurrenceScoreAsNormalCountDividedByTotalCountRoundedToTwoDecimalsAndReturn00WhenTotalCountIsZero() {
        // R010: overallScore = normalCount / totalCount, rounded to 2 decimals; 0.0 when totalCount=0.

        // Case 1: 0 lemmas analysed (all have only 1 occurrence) → score = 0.0
        NlpToken cat = new NlpToken("cat", "cat", "NOUN", 100, false, false);
        when(contentWordFilter.isContentWord(any())).thenReturn(true);
        when(config.getTop()).thenReturn(2000);

        AuditNode q1 = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(cat), null, null, null, null, List.of("cat"), null, null, null, null, null, null));
        AuditNode root1 = makeNode(AuditTarget.COURSE, null);
        sut.onQuiz(q1);
        sut.onCourseComplete(root1);
        assertEquals(0.0, root1.getScores().get("lemma-recurrence"), 0.001,
                "R010: totalCount=0 (no lemma with >=2 occurrences) → score=0.0");

        // Case 2: 3 lemmas, 2 NORMAL → score = 2/3 = 0.67 (rounded to 2 decimals)
        // Need fresh analyzer — use new sut via field reset pattern via mocks
        // Instead, use a separate composer directly
        ContentWordFilter cwf2 = mock(ContentWordFilter.class);
        LemmaRecurrenceConfig cfg2 = mock(LemmaRecurrenceConfig.class);
        IntervalCalculator ic2 = mock(IntervalCalculator.class);
        ExposureClassifier ec2 = mock(ExposureClassifier.class);

        when(cwf2.isContentWord(any())).thenReturn(true);
        when(cfg2.getTop()).thenReturn(2000);
        when(ic2.calculateMeanInterval(any())).thenReturn(100.0);
        when(ic2.calculateStdDevInterval(any(), anyDouble())).thenReturn(0.0);
        when(ec2.classify(anyDouble(), any()))
            .thenReturn(ExposureStatus.NORMAL, ExposureStatus.NORMAL, ExposureStatus.SUB_EXPOSED);

        LemmaRecurrenceAnalyzer sut2 = new LemmaRecurrenceAnalyzer(cwf2, cfg2, ic2, ec2);

        NlpToken dog = new NlpToken("dog", "dog", "NOUN", 80, false, false);
        NlpToken run = new NlpToken("run", "run", "VERB", 200, false, false);
        for (NlpToken t : List.of(cat, cat, dog, dog, run, run)) {
            AuditNode qn = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(t), null, null, null, null, List.of(t.getText()), null, null, null, null, null, null));
            sut2.onQuiz(qn);
        }
        AuditNode root2 = makeNode(AuditTarget.COURSE, null);
        sut2.onCourseComplete(root2);

        assertEquals(0.67, root2.getScores().get("lemma-recurrence"), 0.01,
                "R010: 2 NORMAL / 3 total = 0.67 rounded to 2 decimals");
    }

    @Test
    @DisplayName("should produce identical overall score and exposure classification when two analysed lemmas share the same meanInterval but differ in stdDevInterval confirming stdDev is informative only")
    @Tag("FEAT-LREC")
    @Tag("F-LREC-R011")
    public void shouldProduceIdenticalOverallScoreAndExposureClassificationWhenTwoAnalysedLemmasShareTheSameMeanIntervalButDifferInStdDevIntervalConfirmingStdDevIsInformativeOnly() {
        // R011: stdDevInterval does NOT affect classification or score — only meanInterval does.
        // Two scenarios with same meanInterval but different stdDev → same score.

        // Scenario A: stdDev = 0.0
        ContentWordFilter cwfA = mock(ContentWordFilter.class);
        LemmaRecurrenceConfig cfgA = mock(LemmaRecurrenceConfig.class);
        IntervalCalculator icA = mock(IntervalCalculator.class);
        ExposureClassifier ecA = mock(ExposureClassifier.class);
        when(cwfA.isContentWord(any())).thenReturn(true);
        when(cfgA.getTop()).thenReturn(2000);
        when(icA.calculateMeanInterval(any())).thenReturn(200.0);
        when(icA.calculateStdDevInterval(any(), anyDouble())).thenReturn(0.0); // low stdDev
        when(ecA.classify(anyDouble(), any())).thenReturn(ExposureStatus.NORMAL);

        LemmaRecurrenceAnalyzer sutA = new LemmaRecurrenceAnalyzer(cwfA, cfgA, icA, ecA);
        NlpToken cat = new NlpToken("cat", "cat", "NOUN", 100, false, false);
        for (int i = 0; i < 2; i++) {
            AuditNode qn = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(cat), null, null, null, null, List.of("cat"), null, null, null, null, null, null));
            sutA.onQuiz(qn);
        }
        AuditNode rootA = makeNode(AuditTarget.COURSE, null);
        sutA.onCourseComplete(rootA);

        // Scenario B: stdDev = 150.0 (high variance), same meanInterval
        ContentWordFilter cwfB = mock(ContentWordFilter.class);
        LemmaRecurrenceConfig cfgB = mock(LemmaRecurrenceConfig.class);
        IntervalCalculator icB = mock(IntervalCalculator.class);
        ExposureClassifier ecB = mock(ExposureClassifier.class);
        when(cwfB.isContentWord(any())).thenReturn(true);
        when(cfgB.getTop()).thenReturn(2000);
        when(icB.calculateMeanInterval(any())).thenReturn(200.0);
        when(icB.calculateStdDevInterval(any(), anyDouble())).thenReturn(150.0); // high stdDev
        when(ecB.classify(anyDouble(), any())).thenReturn(ExposureStatus.NORMAL);

        LemmaRecurrenceAnalyzer sutB = new LemmaRecurrenceAnalyzer(cwfB, cfgB, icB, ecB);
        for (int i = 0; i < 2; i++) {
            AuditNode qn = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(cat), null, null, null, null, List.of("cat"), null, null, null, null, null, null));
            sutB.onQuiz(qn);
        }
        AuditNode rootB = makeNode(AuditTarget.COURSE, null);
        sutB.onCourseComplete(rootB);

        // Both scenarios produce identical score — stdDev is informative only
        assertEquals(rootA.getScores().get("lemma-recurrence"), rootB.getScores().get("lemma-recurrence"), 0.001,
                "R011: identical score regardless of stdDev when meanInterval is the same");
    }

    @Test
    @DisplayName("should populate every LemmaStats entry with lemma pos count meanInterval stdDevInterval exposureStatus and occurrencePositions matching the data captured for that lemma")
    @Tag("FEAT-LREC")
    @Tag("F-LREC-R012")
    public void shouldPopulateEveryLemmaStatsEntryWithLemmaPosCountMeanIntervalStdDevIntervalExposureStatusAndOccurrencePositionsMatchingTheDataCapturedForThatLemma() {
        // R012: each analysed lemma contributes lemma, pos, count, meanInterval, stdDev, status, positions.
        // Verifiable through mock interactions: intervalCalculator sees the correct positions and mean,
        // exposureClassifier sees the mean returned by intervalCalculator.
        NlpToken catNoun = new NlpToken("cats", "cat", "NOUN", 100, false, false);

        when(contentWordFilter.isContentWord(any())).thenReturn(true);
        when(config.getTop()).thenReturn(2000);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Integer>> positionsCaptor = ArgumentCaptor.forClass((Class) List.class);
        ArgumentCaptor<Double> meanCaptor = ArgumentCaptor.forClass(Double.class);

        when(intervalCalculator.calculateMeanInterval(positionsCaptor.capture())).thenReturn(250.0);
        when(intervalCalculator.calculateStdDevInterval(positionsCaptor.capture(), meanCaptor.capture())).thenReturn(15.0);
        when(exposureClassifier.classify(anyDouble(), any())).thenReturn(ExposureStatus.NORMAL);

        // "cat" appears twice → count=2, positions recorded
        AuditNode q1 = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(catNoun), null, null, null, null, List.of("cats"), null, null, null, null, null, null));
        AuditNode q2 = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(catNoun), null, null, null, null, List.of("cats"), null, null, null, null, null, null));
        AuditNode root = makeNode(AuditTarget.COURSE, null);

        sut.onQuiz(q1);
        sut.onQuiz(q2);
        sut.onCourseComplete(root);

        // R012: positions list size = count of occurrences (2)
        List<Integer> capturedPositions = positionsCaptor.getAllValues().get(0);
        assertEquals(2, capturedPositions.size(), "R012: occurrencePositions count matches lemma occurrence count");

        // R012: meanInterval from intervalCalculator is passed to stdDev and exposureClassifier
        assertEquals(250.0, meanCaptor.getValue(), 0.001, "R012: meanInterval passed to stdDev calculation");
        verify(exposureClassifier, times(1)).classify(eq(250.0), any());
    }

    @Test
    @DisplayName("should give its course finding the measure 8,0 %, the share of the most used lemmas that come back at a healthy interval, next to its goal")
    @Tag("FEAT-HALL")
    @Tag("F-HALL-R003")
    public void shouldGiveItsCourseFindingTheMeasure80TheShareOfTheMostUsedLemmasThatComeBackAtAHealthyIntervalNextToItsGoal() {
        // R003: on the 29/9 base 8 of its 100 most used lemmas come back at a healthy interval:
        // 8,0 %. Here 100 lemmas appear twice; the first 8 come back at a healthy interval.
        org.mockito.Mockito.lenient().when(config.getTop()).thenReturn(100);
        org.mockito.Mockito.lenient().when(config.getOverExposedThreshold()).thenReturn(2.0);
        org.mockito.Mockito.lenient().when(config.getSubExposedThreshold()).thenReturn(50.0);
        org.mockito.Mockito.lenient().when(contentWordFilter.isContentWord(org.mockito.ArgumentMatchers.any()))
                .thenReturn(true);
        org.mockito.Mockito.lenient().when(intervalCalculator.calculateMeanInterval(org.mockito.ArgumentMatchers.anyList()))
                .thenAnswer(invocation -> ((List<Integer>) invocation.getArgument(0)).get(0).doubleValue());
        org.mockito.Mockito.lenient().when(exposureClassifier.classify(org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.any())).thenAnswer(invocation ->
                (double) invocation.getArgument(0) <= 8 ? ExposureStatus.NORMAL : ExposureStatus.SUB_EXPOSED);
        List<NlpToken> words = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            words.add(new NlpToken("w" + i, "w" + i, "NOUN", 1, false, false));
        }
        AuditNode root = makeNode(AuditTarget.COURSE, null);
        for (String id : List.of("q1", "q2")) {
            sut.onQuiz(makeNode(AuditTarget.QUIZ, new AuditableQuiz(words, id, id, id, null, List.of(), null, null,
                    null, null, null, null)));
        }
        sut.onCourseComplete(root);

        List<FindingDraft> drafts = sut.findingsAt(root);

        assertEquals(0.08, root.getScores().get("lemma-recurrence"), 1e-9);
        assertEquals(1, drafts.size(), "one finding for the course");
        StringBuilder evidence = new StringBuilder();
        drafts.get(0).getEvidence().getExamined().forEach(p -> evidence.append(p.getLabel()).append(": ")
                .append(p.getText()).append('\n'));
        assertTrue(evidence.toString().contains("8,0 %"), "R003: the measure it took, 8,0 %: " + evidence);
        assertTrue(evidence.toString().contains("100"), "R003: of the 100 most used lemmas: " + evidence);
        assertTrue(evidence.toString().contains("más de 2 y hasta 50"),
                "R003: next to its goal, the healthy interval: " + evidence);
    }

    @Test
    @DisplayName("should give the course of 29/9 a lemma-recurrence score of 0.08, with 134 normal, 1,570 under-exposed and 46 over-exposed of its 1,750 returning lemmas")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R001")
    public void shouldGiveTheCourseOf299ALemmarecurrenceScoreOf008With134Normal1570UnderexposedAnd46OverexposedOfIts1750ReturningLemmas() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Test
    @DisplayName("should score the course as before, normal lemmas over analysed lemmas rounded to two decimals, while it also publishes its course diagnosis")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R001")
    public void shouldScoreTheCourseAsBeforeNormalLemmasOverAnalysedLemmasRoundedToTwoDecimalsWhileItAlsoPublishesItsCourseDiagnosis() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Test
    @DisplayName("should publish in the course diagnosis each analysed lemma with its count, mean interval, status, deviation, longest gap with its start and end levels and its levels, like 'movie': 90 times, every 1,005 words, under-exposed, longest gap 6,510 from A1 to A2")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R003")
    public void shouldPublishInTheCourseDiagnosisEachAnalysedLemmaWithItsCountMeanIntervalStatusDeviationLongestGapWithItsStartAndEndLevelsAndItsLevelsLikeMovie90TimesEvery1005WordsUnderexposedLongestGap6510FromA1ToA2() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Test
    @DisplayName("should take a lemma's longest gap as the largest interval between two consecutive occurrences, with the levels of the quizzes where it starts and ends, like 27,379 from B1 to B2 for 'suppose'")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R003")
    public void shouldTakeALemmasLongestGapAsTheLargestIntervalBetweenTwoConsecutiveOccurrencesWithTheLevelsOfTheQuizzesWhereItStartsAndEndsLike27379FromB1ToB2ForSuppose() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Test
    @DisplayName("should list the levels where a lemma appears from A1 to B2 without repeating them")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R003")
    public void shouldListTheLevelsWhereALemmaAppearsFromA1ToB2WithoutRepeatingThem() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Test
    @DisplayName("should publish the analysed lemmas from most to least frequent")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R003")
    public void shouldPublishTheAnalysedLemmasFromMostToLeastFrequent() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Test
    @DisplayName("should publish for each level the recurrence measured only with its quizzes and the same thresholds: returning lemmas, normal, under-exposed, over-exposed and the normal share")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R004")
    public void shouldPublishForEachLevelTheRecurrenceMeasuredOnlyWithItsQuizzesAndTheSameThresholdsReturningLemmasNormalUnderexposedOverexposedAndTheNormalShare() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Test
    @DisplayName("should close the sums of every level, so normal plus under-exposed plus over-exposed lemmas equal its returning lemmas")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R004")
    public void shouldCloseTheSumsOfEveryLevelSoNormalPlusUnderexposedPlusOverexposedLemmasEqualItsReturningLemmas() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Test
    @DisplayName("should publish for the course of 29/9 A1 450 returning lemmas with 178 normal (39.6%), A2 752 with 155 (20.6%), B1 1,139 with 161 (14.1%) and B2 819 with 165 (20.1%)")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R004")
    public void shouldPublishForTheCourseOf299A1450ReturningLemmasWith178Normal396A2752With155206B11139With161141AndB2819With165201() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Test
    @DisplayName("should keep the recurrence of each level as detail of the course diagnosis and never write a lemma-recurrence score on a milestone")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R004")
    public void shouldKeepTheRecurrenceOfEachLevelAsDetailOfTheCourseDiagnosisAndNeverWriteALemmarecurrenceScoreOnAMilestone() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Test
    @DisplayName("should leave in the course one finding per under-exposed or over-exposed lemma, of rule F-RECUR-R005, low severity, only ordering and instant cost, and none for a normal one")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R005")
    public void shouldLeaveInTheCourseOneFindingPerUnderexposedOrOverexposedLemmaOfRuleFRECURR005LowSeverityOnlyOrderingAndInstantCostAndNoneForANormalOne() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Test
    @DisplayName("should give as evidence the lemma, its times, how often it returns against the goal and its longest gap, like 'movie: 90 times, returns every 1,005 words (goal: more than 50 and up to 1,000); its longest gap, 6,510, goes from A1 to A2'")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R005")
    public void shouldGiveAsEvidenceTheLemmaItsTimesHowOftenItReturnsAgainstTheGoalAndItsLongestGapLikeMovie90TimesReturnsEvery1005WordsGoalMoreThan50AndUpTo1000ItsLongestGap6510GoesFromA1ToA2() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Test
    @DisplayName("should leave no finding when the course score is 1")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R005")
    public void shouldLeaveNoFindingWhenTheCourseScoreIs1() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Test
    @DisplayName("should leave no finding for a lemma seen only once, which lemma-recurrence does not analyse")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R005")
    public void shouldLeaveNoFindingForALemmaSeenOnlyOnceWhichLemmarecurrenceDoesNotAnalyse() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Test
    @DisplayName("should identify each finding by its lemma")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R005")
    public void shouldIdentifyEachFindingByItsLemma() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Test
    @DisplayName("should leave 1,616 findings in the course of 29/9, 1,570 for under-exposed and 46 for over-exposed lemmas")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R005")
    public void shouldLeave1616FindingsInTheCourseOf2991570ForUnderexposedAnd46ForOverexposedLemmas() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
