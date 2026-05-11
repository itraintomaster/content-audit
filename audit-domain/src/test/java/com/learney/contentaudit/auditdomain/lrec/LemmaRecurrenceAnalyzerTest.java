package com.learney.contentaudit.auditdomain.lrec;

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
        AuditableKnowledge k = new AuditableKnowledge(null, "t", "i", true, "k1", "l", "c");
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
        AuditableQuiz quiz1 = new AuditableQuiz(List.of(detToken, catToken), null, null, null, null, List.of("the cat"), null);
        AuditableQuiz quiz2 = new AuditableQuiz(List.of(detToken, catToken), null, null, null, null, List.of("the cat"), null);

        when(contentWordFilter.isContentWord(argThat(t -> t != null && "DET".equals(t.getPosTag())))).thenReturn(false);
        when(contentWordFilter.isContentWord(argThat(t -> t != null && "NOUN".equals(t.getPosTag())))).thenReturn(true);
        when(config.getTop()).thenReturn(2000);
        when(intervalCalculator.calculateMeanInterval(any())).thenReturn(2.0);
        when(intervalCalculator.calculateStdDevInterval(any(), anyDouble())).thenReturn(0.0);
        when(exposureClassifier.classify(anyDouble(), any())).thenReturn(ExposureStatus.NORMAL);

        AuditNode quizNode1 = makeNode(AuditTarget.QUIZ, new AuditableQuiz(quiz1.getTokens(), null, null, null, null, List.of("the cat"), null));
        AuditNode quizNode2 = makeNode(AuditTarget.QUIZ, new AuditableQuiz(quiz2.getTokens(), null, null, null, null, List.of("the cat"), null));
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

        AuditNode quizNode1 = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(runs), null, null, null, null, List.of("runs"), null));
        AuditNode quizNode2 = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(running), null, null, null, null, List.of("running"), null));
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
            AuditNode qn = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(cat), null, null, null, null, List.of("cat"), null));
            sut.onQuiz(qn);
        }
        for (int i = 0; i < 2; i++) {
            AuditNode qn = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(dog), null, null, null, null, List.of("dog"), null));
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

        AuditNode quizNode = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(cat), null, null, null, null, List.of("cat"), null));
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

        AuditNode q1 = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(cat), null, null, null, null, List.of("cat"), null));
        AuditNode q2 = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(cat), null, null, null, null, List.of("cat"), null));
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
            AuditNode qn = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(t), null, null, null, null, List.of(t.getText()), null));
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

        AuditNode q1 = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(cat), null, null, null, null, List.of("cat"), null));
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
            AuditNode qn = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(t), null, null, null, null, List.of(t.getText()), null));
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
            AuditNode qn = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(cat), null, null, null, null, List.of("cat"), null));
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
            AuditNode qn = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(cat), null, null, null, null, List.of("cat"), null));
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
        AuditNode q1 = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(catNoun), null, null, null, null, List.of("cats"), null));
        AuditNode q2 = makeNode(AuditTarget.QUIZ, new AuditableQuiz(List.of(catNoun), null, null, null, null, List.of("cats"), null));
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
}
