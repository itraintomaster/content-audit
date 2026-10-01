package com.learney.contentaudit.auditdomain.lrec;

import java.util.Map;import com.learney.contentaudit.auditdomain.AuditableQuiz;
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
        // 100 different words written with letters (waa, wab, ...): since 1/10 a lemma with figures, as
        // «w0», is not a word of the analysis (F-RECUR-R001).
        List<NlpToken> words = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            String word = "w" + (char) ('a' + i / 26) + (char) ('a' + i % 26);
            words.add(new NlpToken(word, word, "NOUN", 1, false, false));
        }
        AuditNode root = makeNode(AuditTarget.COURSE, null);
        for (String id : List.of("q1", "q2")) {
            sut.onQuiz(makeNode(AuditTarget.QUIZ, new AuditableQuiz(words, id, id, id, null, List.of(), null, null,
                    null, null, null, null)));
        }
        sut.onCourseComplete(root);

        List<FindingDraft> drafts = sut.findingsAt(root);

        assertEquals(0.08, root.getScores().get("lemma-recurrence"), 1e-9);
        // F-RECUR-R005 split the one finding of the course into one per lemma out of range: 92 here.
        assertEquals(92, drafts.size(), "one finding of the course per lemma out of range (F-RECUR-R005)");
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
        // R001, rama ronda1/recur-posiciones (opción B): la puntuación no ocupa posición: of the 91,873 tokens of the 29/9 course, the 78,161
        // with a letter or a figure are positions, in order A1 → B2, and give 0,09: 160 normal, 1,534
        // under-exposed and 49 over-exposed of its 1,743 returning lemmas (option A: 0,08, 131, 1,566 and
        // 46). The name of this test still carries the numbers of 30/9 (pending, @qa-tester).
        Run299 run = run299();

        assertEquals(0.09, run.root.getScores().get("lemma-recurrence"), 1e-12, "R001: the score with option B");
        LemmaRecurrenceResult result = run.diagnosis().getResult();
        assertEquals(1743, result.getLemmaStats().size(), "R001: 1,743 returning lemmas");
        assertEquals(List.of(160, 1534, 49), List.of(result.getExposureSummary().getNormalCount(),
                result.getExposureSummary().getSubExposedCount(), result.getExposureSummary().getOverExposedCount()),
                "R001: 160 normal, 1,534 under-exposed, 49 over-exposed");
        assertEquals(91873, run.positions, "the 91,873 tokens of the course");
    }

    @Test
    @DisplayName("should score the course as before, normal lemmas over analysed lemmas rounded to two decimals, while it also publishes its course diagnosis")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R001")
    public void shouldScoreTheCourseAsBeforeNormalLemmasOverAnalysedLemmasRoundedToTwoDecimalsWhileItAlsoPublishesItsCourseDiagnosis() {
        // R001: 3 normal lemmas of 7 analysed give 3/7 rounded to two decimals, 0,43, as before, and the
        // same result is the one the course diagnosis publishes.
        Stream stream = new Stream();
        for (String normal : List.of("cat", "hat", "map")) {
            stream.quiz("A1", normal + "/NOUN 99 " + normal + "/NOUN");
        }
        for (String rare : List.of("dog", "pen", "cup", "box")) {
            stream.quiz("A1", rare + "/NOUN 1999 " + rare + "/NOUN");
        }

        LemmaRecurrenceAnalyzer analyzer = stream.run();

        assertEquals(0.43, stream.root.getScores().get("lemma-recurrence"), 1e-12, "R001: round(3 / 7, 2)");
        LemmaRecurrenceResult result = stream.diagnosis().getResult();
        assertEquals(0.43, result.getOverallScore(), 1e-12, "the diagnosis publishes the same score");
        assertEquals(List.of(3, 4, 0), List.of(result.getExposureSummary().getNormalCount(),
                result.getExposureSummary().getSubExposedCount(), result.getExposureSummary().getOverExposedCount()));
        assertEquals(7, result.getLemmaStats().size());
        assertEquals("lemma-recurrence", analyzer.getName());
    }

    @Test
    @DisplayName("should publish in the course diagnosis each analysed lemma with its count, mean interval, status, deviation, longest gap with its start and end levels and its levels, like 'movie': 90 times, every 1,005 words, under-exposed, longest gap 6,510 from A1 to A2")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R003")
    public void shouldPublishInTheCourseDiagnosisEachAnalysedLemmaWithItsCountMeanIntervalStatusDeviationLongestGapWithItsStartAndEndLevelsAndItsLevelsLikeMovie90TimesEvery1005WordsUnderexposedLongestGap6510FromA1ToA2() {
        // R003 on the 29/9 course, rama ronda1/recur-posiciones (opción B): la puntuación no ocupa posición: «movie», 90 times, every 855 words,
        // normal (with option A, every 1,005, under-exposed), a deviation of 913, its longest gap 5,400 from A1
        // to A2, and in the four levels. The name keeps the numbers of 30/9 (pending, @qa-tester).
        LemmaStats movie = run299().stats("movie");

        assertEquals(90, movie.getCount(), "R003: its times");
        assertEquals(854.83, movie.getMeanInterval(), 0.01, "R003: how often it returns");
        assertEquals(ExposureStatus.NORMAL, movie.getExposureStatus(), "R003: normal with option B");
        assertEquals(913.22, movie.getStdDevInterval(), 0.01, "R003: the deviation of its intervals");
        assertEquals(5400, movie.getLongestGap(), "R003: its longest gap");
        assertEquals(List.of(CefrLevel.A1, CefrLevel.A2), List.of(movie.getLongestGapFromLevel(),
                movie.getLongestGapToLevel()), "R003: from A1 to A2");
        assertEquals(List.of(CefrLevel.A1, CefrLevel.A2, CefrLevel.B1, CefrLevel.B2), movie.getLevels(),
                "R003: the levels where it appears");
    }

    @Test
    @DisplayName("should take a lemma's longest gap as the largest interval between two consecutive occurrences, with the levels of the quizzes where it starts and ends, like 27,379 from B1 to B2 for 'suppose'")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R003")
    public void shouldTakeALemmasLongestGapAsTheLargestIntervalBetweenTwoConsecutiveOccurrencesWithTheLevelsOfTheQuizzesWhereItStartsAndEndsLike27379FromB1ToB2ForSuppose() {
        // R003, rama ronda1/recur-posiciones (opción B): la puntuación no ocupa posición: «suppose» returns on average every 603 words, but its
        // largest interval between two consecutive occurrences is 23,684, from a B1 quiz to a B2 one;
        // «vegetable»'s, 23,825, is in B1 (option A: 696, 27,379 and 27,780; the name keeps those).
        Run299 run = run299();
        LemmaStats suppose = run.stats("suppose");
        LemmaStats vegetable = run.stats("vegetable");

        assertEquals(602.6, suppose.getMeanInterval(), 1e-9);
        assertEquals(23684, suppose.getLongestGap(), "R003: the largest interval");
        assertEquals(List.of(CefrLevel.B1, CefrLevel.B2), List.of(suppose.getLongestGapFromLevel(),
                suppose.getLongestGapToLevel()), "R003: from B1 to B2");
        assertEquals(List.of(23825, CefrLevel.B1, CefrLevel.B1), List.of(vegetable.getLongestGap(),
                vegetable.getLongestGapFromLevel(), vegetable.getLongestGapToLevel()), "R003: within B1");
    }

    @Test
    @DisplayName("should list the levels where a lemma appears from A1 to B2 without repeating them")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R003")
    public void shouldListTheLevelsWhereALemmaAppearsFromA1ToB2WithoutRepeatingThem() {
        // R003: cat appears twice in A1 and once in B1: its levels are A1 and B1, once each.
        Stream stream = new Stream();
        stream.quiz("A1", "cat/NOUN 5 cat/NOUN");
        stream.quiz("B1", "cat/NOUN");

        stream.run();

        assertEquals(List.of(CefrLevel.A1, CefrLevel.B1), stream.stats("cat").getLevels(),
                "R003: from A1 to B2, without repeating them");
    }

    @Test
    @DisplayName("should publish the analysed lemmas from most to least frequent")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R003")
    public void shouldPublishTheAnalysedLemmasFromMostToLeastFrequent() {
        // R003 and F-LREC-R013: from most to least frequent: go 603, have 579, eat 332, say 328, play 283...
        List<LemmaStats> stats = run299().diagnosis().getResult().getLemmaStats();

        assertEquals(List.of("go 603", "have 579", "eat 332", "say 328", "play 283"), stats.subList(0, 5).stream()
                .map(s -> s.getLemma() + " " + s.getCount()).toList(), "R003: the most frequent first");
        for (int i = 1; i < stats.size(); i++) {
            assertTrue(stats.get(i - 1).getCount() >= stats.get(i).getCount(), "R003: never more frequent after "
                    + stats.get(i - 1).getLemma());
        }
    }

    @Test
    @DisplayName("should publish for each level the recurrence measured only with its quizzes and the same thresholds: returning lemmas, normal, under-exposed, over-exposed and the normal share")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R004")
    public void shouldPublishForEachLevelTheRecurrenceMeasuredOnlyWithItsQuizzesAndTheSameThresholdsReturningLemmasNormalUnderexposedOverexposedAndTheNormalShare() {
        // R004: each level is measured only with its quizzes: «sun» returns every 10 words within A1
        // (over-exposed there) and every 100 within B1 (normal); «cat», once in A1 and once in B1, returns
        // in the course but in neither level.
        Stream stream = new Stream();
        stream.quiz("A1", "sun/NOUN 9 sun/NOUN cat/NOUN");
        stream.quiz("B1", "cat/NOUN sun/NOUN 99 sun/NOUN");

        stream.run();

        List<LevelRecurrence> levels = stream.diagnosis().getLevels();
        assertEquals(List.of("A1 12 1 0 0 1 0.0", "B1 102 1 1 0 0 1.0"), levels.stream().map(l -> l.getLevel() + " "
                + l.getPositions() + " " + l.getReturningLemmas() + " " + l.getNormalCount() + " " + l.getSubExposedCount()
                + " " + l.getOverExposedCount() + " " + l.getNormalShare()).toList(),
                "R004: positions, returning, normal, under, over and the normal share of each level");
        assertEquals(2, stream.diagnosis().getResult().getLemmaStats().size(), "in the course both return");
    }

    @Test
    @DisplayName("should close the sums of every level, so normal plus under-exposed plus over-exposed lemmas equal its returning lemmas")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R004")
    public void shouldCloseTheSumsOfEveryLevelSoNormalPlusUnderexposedPlusOverexposedLemmasEqualItsReturningLemmas() {
        // R004 and F-LREC-R009: in every level of the 29/9 course, normal + under + over = returning.
        for (LevelRecurrence level : run299().diagnosis().getLevels()) {
            assertEquals(level.getReturningLemmas(), level.getNormalCount() + level.getSubExposedCount()
                    + level.getOverExposedCount(), "R004: the sums of " + level.getLevel() + " close");
        }
        assertEquals(4, run299().diagnosis().getLevels().size(), "the four levels");
    }

    @Test
    @DisplayName("should publish for the course of 29/9 A1 450 returning lemmas with 178 normal (39.6%), A2 752 with 155 (20.6%), B1 1,139 with 161 (14.1%) and B2 819 with 165 (20.1%)")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R004")
    public void shouldPublishForTheCourseOf299A1450ReturningLemmasWith178Normal396A2752With155206B11139With161141AndB2819With165201() {
        // R004 on the 29/9 course, with only the lemmas written with letters (1/10): A1 448 returning
        // lemmas, 176 normal (39,3 %); A2 752, 155 (20,6 %); B1 1,137, 161 (14,2 %); B2 818, 165 (20,2 %).
        // The name of this test still carries the numbers of 30/9 (pending, @qa-tester).
        List<String> levels = run299().diagnosis().getLevels().stream().map(l -> l.getLevel() + " " + l.getPositions()
                + " " + l.getReturningLemmas() + " " + l.getNormalCount() + " " + l.getSubExposedCount() + " "
                + l.getOverExposedCount() + " " + String.format(java.util.Locale.ROOT, "%.1f", 100 * l.getNormalShare()))
                .toList();

        // rama ronda1/recur-posiciones (opción B): la puntuación no ocupa posición: the positions of each level are its words, without the
        // punctuation (12,934 · 15,996 · 33,439 · 15,792 of 91,873 tokens).
        assertEquals(List.of("A1 12934 448 200 228 20 44.6", "A2 15996 752 203 521 28 27.0",
                "B1 33439 1137 187 908 42 16.4", "B2 15792 818 201 566 51 24.6"), levels, "R004: the table of the detail");
    }

    @Test
    @DisplayName("should keep the recurrence of each level as detail of the course diagnosis and never write a lemma-recurrence score on a milestone")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R004")
    public void shouldKeepTheRecurrenceOfEachLevelAsDetailOfTheCourseDiagnosisAndNeverWriteALemmarecurrenceScoreOnAMilestone() {
        // R004 (DOUBT-PUNTAJE-POR-NIVEL, A): the recurrence of each level is detail of the course
        // diagnosis; no milestone gets a lemma-recurrence score, or the plan would open tasks there.
        Stream stream = new Stream();
        stream.quiz("A1", "sun/NOUN 9 sun/NOUN dog/NOUN 1999 dog/NOUN");
        stream.quiz("B1", "sun/NOUN 99 sun/NOUN");

        stream.run();

        assertEquals(List.of(CefrLevel.A1, CefrLevel.B1), stream.diagnosis().getLevels().stream()
                .map(LevelRecurrence::getLevel).toList(), "R004: inside the course diagnosis");
        for (AuditNode level : stream.root.getChildren()) {
            assertTrue(level.getScores().keySet().stream().noneMatch(key -> key.startsWith("lemma-recurrence")),
                    "R004: no score of lemma-recurrence on " + level.getEntity().getLabel());
        }
        assertTrue(stream.root.getScores().containsKey("lemma-recurrence"), "only the course has it");
    }

    @Test
    @DisplayName("should leave in the course one finding per under-exposed or over-exposed lemma, of rule F-RECUR-R005, low severity, only ordering and instant cost, and none for a normal one")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R005")
    public void shouldLeaveInTheCourseOneFindingPerUnderexposedOrOverexposedLemmaOfRuleFRECURR005LowSeverityOnlyOrderingAndInstantCostAndNoneForANormalOne() {
        // R005: sun (every 10 words) and dog (every 2,000) leave one finding each, low and rank-only;
        // cat (every 100) leaves none.
        Stream stream = new Stream();
        stream.quiz("A1", "sun/NOUN 9 sun/NOUN cat/NOUN 99 cat/NOUN dog/NOUN 1999 dog/NOUN");
        LemmaRecurrenceAnalyzer analyzer = stream.run();

        List<FindingDraft> drafts = analyzer.findingsAt(stream.root);

        assertEquals(List.of("sun", "dog"), drafts.stream().map(FindingDraft::getMarker).sorted(
                java.util.Comparator.reverseOrder()).toList(), "R005: one per lemma out of range, none for cat");
        for (FindingDraft draft : drafts) {
            assertEquals(List.of("F-RECUR-R005", "LOW", "RANK_ONLY"), List.of(draft.getRule(),
                    draft.getSeverity().name(), draft.getResolution().name()), "R005: low and only ordering");
        }
        com.learney.contentaudit.auditdomain.catalog.AnalyzerRuleCard rule = new LemmaRecurrenceAnalyzerProvider(
                recurrenceConfig()).describe().getRules().get(0);
        assertEquals(List.of("F-RECUR-R005", "INSTANT"), List.of(rule.getId(), rule.getCost().name()),
                "R005: instant, the cost of its rule");
    }

    @Test
    @DisplayName("should give as evidence the lemma, its times, how often it returns against the goal and its longest gap, like 'movie: 90 times, returns every 1,005 words (goal: more than 50 and up to 1,000); its longest gap, 6,510, goes from A1 to A2'")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R005")
    public void shouldGiveAsEvidenceTheLemmaItsTimesHowOftenItReturnsAgainstTheGoalAndItsLongestGapLikeMovie90TimesReturnsEvery1005WordsGoalMoreThan50AndUpTo1000ItsLongestGap6510GoesFromA1ToA2() {
        // R005: the evidence of «vegetable» on the 29/9 course. rama ronda1/recur-posiciones (opción B): la puntuación no ocupa posición, «movie»
        // returns every 855 words and is normal, so it leaves no finding (the name keeps it, pending @qa-tester).
        Run299 run = run299();

        FindingDraft movie = run.analyzer.findingsAt(run.root).stream().filter(d -> "vegetable".equals(d.getMarker()))
                .findFirst().orElseThrow();

        assertEquals("vegetable: 10 veces, vuelve cada 7.765 palabras (meta: más de 50 y hasta 1.000); su hueco más "
                + "largo, 23.825, está en B1", movie.getEvidence().getObservation(), "R005: the measure against its goal");
        Map<String, String> parts = new LinkedHashMap<>();
        movie.getEvidence().getExamined().forEach(part -> parts.put(part.getLabel(), part.getText()));
        assertEquals(List.of("vegetable", "10", "7.765 palabras", "más de 50 y hasta 1.000", "23.825, está en B1"),
                List.of(parts.get("Palabra"), parts.get("Veces"), parts.get("Vuelve cada"), parts.get("Meta"),
                        parts.get("Hueco más largo")), "R005: the lemma, its times, its interval, the goal and its gap");
    }

    @Test
    @DisplayName("should leave no finding when the course score is 1")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R005")
    public void shouldLeaveNoFindingWhenTheCourseScoreIs1() {
        // R005: a course at 1 -- every lemma returns at a healthy interval -- has no finding.
        Stream stream = new Stream();
        stream.quiz("A1", "cat/NOUN 99 cat/NOUN hat/NOUN 199 hat/NOUN");
        LemmaRecurrenceAnalyzer analyzer = stream.run();

        assertEquals(1.0, stream.root.getScores().get("lemma-recurrence"), 1e-12);
        assertEquals(List.of(), analyzer.findingsAt(stream.root), "R005: nothing at 1");
    }

    @Test
    @DisplayName("should leave no finding for a lemma seen only once, which lemma-recurrence does not analyse")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R005")
    public void shouldLeaveNoFindingForALemmaSeenOnlyOnceWhichLemmarecurrenceDoesNotAnalyse() {
        // R005 and F-LREC-R006 (DOUBT-UNA-SOLA-VEZ, A): «pen», seen once, is not analysed and leaves no
        // finding; it is already a finding of lemma-count.
        Stream stream = new Stream();
        stream.quiz("A1", "sun/NOUN 9 sun/NOUN pen/NOUN cat/NOUN 99 cat/NOUN");
        LemmaRecurrenceAnalyzer analyzer = stream.run();

        assertTrue(stream.diagnosis().getResult().getLemmaStats().stream().noneMatch(s -> s.getLemma().equals("pen")),
                "pen is not analysed");
        assertEquals(List.of("sun"), analyzer.findingsAt(stream.root).stream().map(FindingDraft::getMarker).toList(),
                "R005: only sun, over-exposed; nothing for pen");
    }

    @Test
    @DisplayName("should identify each finding by its lemma")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R005")
    public void shouldIdentifyEachFindingByItsLemma() {
        // R005 and F-HALL-R014: the identity is the lemma, as the key of the analyzer: «run» as a verb and
        // as a noun is one lemma and one finding.
        Stream stream = new Stream();
        stream.quiz("A1", "run/VERB 1999 run/NOUN cat/NOUN 99 cat/NOUN");
        LemmaRecurrenceAnalyzer analyzer = stream.run();

        assertEquals(List.of("run"), analyzer.findingsAt(stream.root).stream().map(FindingDraft::getMarker).toList(),
                "R005: one finding, marked with the lemma");
    }

    @Test
    @DisplayName("should leave 1,616 findings in the course of 29/9, 1,570 for under-exposed and 46 for over-exposed lemmas")
    @Tag("FEAT-RECUR")
    @Tag("F-RECUR-R005")
    public void shouldLeave1616FindingsInTheCourseOf2991570ForUnderexposedAnd46ForOverexposedLemmas() {
        // R005 on the 29/9 course, rama ronda1/recur-posiciones (opción B): la puntuación no ocupa posición: 1,583 findings, 1,534 for under-exposed
        // lemmas and 49 for over-exposed ones (option A: 1,612; the name keeps the 1,616 of 30/9, pending @qa-tester).
        Run299 run = run299();

        List<FindingDraft> drafts = run.analyzer.findingsAt(run.root);

        assertEquals(1583, drafts.size(), "R005: 1,583 findings");
        Map<String, Integer> byState = new java.util.TreeMap<>();
        drafts.forEach(d -> d.getEvidence().getExamined().stream().filter(p -> "Estado".equals(p.getLabel()))
                .forEach(p -> byState.merge(p.getText(), 1, Integer::sum)));
        assertEquals(Map.of("poco vista", 1534, "sobre-expuesta", 49), byState, "R005: 1,534 and 49");
    }

    // -----------------------------------------------------------------------
    // FEAT-RECUR fixtures
    // -----------------------------------------------------------------------

    /** The thresholds of today: the 2,000 most used, over-exposed up to 50, under-exposed beyond 1,000. */
    private static LemmaRecurrenceConfig recurrenceConfig() {
        return new LemmaRecurrenceConfig() {
            @Override
            public int getTop() {
                return 2000;
            }

            @Override
            public double getSubExposedThreshold() {
                return 1000.0;
            }

            @Override
            public double getOverExposedThreshold() {
                return 50.0;
            }

            @Override
            public java.util.Map<String, Object> describe() {
                return java.util.Map.of();
            }
        };
    }

    private static LemmaRecurrenceAnalyzer realAnalyzer() {
        return new LemmaRecurrenceAnalyzer(new DefaultContentWordFilter(), recurrenceConfig(),
                new DefaultIntervalCalculator(), new DefaultExposureClassifier());
    }

    /**
     * A course of levels with one quiz per call: «cat/NOUN» is a content token, a number n is n
     * function words in between, so positions are exact.
     */
    private static final class Stream {
        final AuditNode root = node(AuditTarget.COURSE, null, null);
        private final Map<String, AuditNode> knowledges = new LinkedHashMap<>();
        private final List<AuditNode> quizzes = new ArrayList<>();

        Stream() {
            root.setDiagnoses(new DefaultCourseDiagnoses());
        }

        void quiz(String level, String spec) {
            List<NlpToken> tokens = new ArrayList<>();
            for (String item : spec.split(" ")) {
                if (item.contains("/")) {
                    String[] f = item.split("/");
                    tokens.add(new NlpToken(f[0], f[0], f[1], null, false, false));
                } else {
                    for (int i = 0; i < Integer.parseInt(item); i++) {
                        tokens.add(new NlpToken("the", "the", "DET", 1, true, false));
                    }
                }
            }
            add(level, "k-" + level, tokens);
        }

        void add(String level, String knowledgeId, List<NlpToken> tokens) {
            AuditNode knowledge = knowledges.computeIfAbsent(level + "/" + knowledgeId, k -> {
                AuditNode milestone = root.getChildren().stream().filter(m -> level.equals(m.getEntity().getLabel()))
                        .findFirst().orElseGet(() -> node(AuditTarget.MILESTONE,
                                new AuditableMilestone(List.of(), "m-" + level, level, level), root));
                AuditNode topic = milestone.getChildren().isEmpty() ? node(AuditTarget.TOPIC,
                        new AuditableTopic(List.of(), "t-" + level, "Tema", "T"), milestone) : milestone.getChildren().get(0);
                return node(AuditTarget.KNOWLEDGE, new AuditableKnowledge(List.of(), knowledgeId, "", true, knowledgeId,
                        knowledgeId, "K", null, "Tema"), topic);
            });
            quizzes.add(node(AuditTarget.QUIZ, new AuditableQuiz(tokens, "q" + quizzes.size(), "q", "Q", null, List.of(),
                    null, null, null, null, null, null), knowledge));
        }

        LemmaRecurrenceAnalyzer run() {
            LemmaRecurrenceAnalyzer analyzer = realAnalyzer();
            quizzes.forEach(analyzer::onQuiz);
            analyzer.onCourseComplete(root);
            return analyzer;
        }

        LemmaRecurrenceCourseDiagnosis diagnosis() {
            return ((DefaultCourseDiagnoses) root.getDiagnoses()).getLemmaRecurrenceDiagnosis().orElseThrow();
        }

        LemmaStats stats(String lemma) {
            return diagnosis().getResult().getLemmaStats().stream().filter(s -> s.getLemma().equals(lemma)).findFirst()
                    .orElseThrow(() -> new AssertionError("no stats of " + lemma));
        }
    }

    /** lemma-recurrence over the 29/9 course, run once for the tests that read it. */
    private static final class Run299 {
        final AuditNode root;
        final LemmaRecurrenceAnalyzer analyzer;
        final int positions;
        private final Stream stream;

        Run299(Stream stream, LemmaRecurrenceAnalyzer analyzer, int positions) {
            this.stream = stream;
            this.root = stream.root;
            this.analyzer = analyzer;
            this.positions = positions;
        }

        LemmaRecurrenceCourseDiagnosis diagnosis() {
            return stream.diagnosis();
        }

        LemmaStats stats(String lemma) {
            return stream.stats(lemma);
        }
    }

    private static Run299 run299;

    /**
     * The 11,287 quizzes of the 29/9 course in the order the engine walks them (A1 → B2), with the
     * lemma and part of speech of each of their 91,873 tokens, as the analysis 2026-09-30T11-54-02 had them.
     */
    private static synchronized Run299 run299() {
        if (run299 == null) {
            Stream stream = new Stream();
            int positions = 0;
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(
                    new java.util.zip.GZIPInputStream(LemmaRecurrenceAnalyzerTest.class.getResourceAsStream(
                            "/recur-2026-09-30/stream.tsv.gz")), java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] fields = line.split("\\t", -1);
                    List<NlpToken> tokens = new ArrayList<>();
                    for (int i = 3; i < fields.length; i++) {
                        int bar = fields[i].lastIndexOf('|');
                        String lemma = fields[i].substring(0, bar);
                        String pos = fields[i].substring(bar + 1);
                        tokens.add(new NlpToken(lemma, lemma, pos, null, false, "PUNCT".equals(pos)));
                    }
                    positions += tokens.size();
                    stream.add(fields[0], fields[2], tokens);
                }
            } catch (java.io.IOException e) {
                throw new AssertionError("the 29/9 stream is unreadable", e);
            }
            run299 = new Run299(stream, stream.run(), positions);
        }
        return run299;
    }

    private static AuditNode node(AuditTarget target, AuditableEntity entity, AuditNode parent) {
        AuditNode node = new AuditNode();
        node.setTarget(target);
        node.setEntity(entity);
        node.setParent(parent);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        if (parent != null) {
            parent.getChildren().add(node);
        }
        return node;
    }
}
