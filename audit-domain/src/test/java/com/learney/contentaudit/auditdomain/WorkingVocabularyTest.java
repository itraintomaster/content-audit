package com.learney.contentaudit.auditdomain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.learney.contentaudit.auditdomain.coca.*;
import com.learney.contentaudit.auditdomain.labs.*;
import com.learney.contentaudit.auditdomain.lemmacount.*;
import com.learney.contentaudit.auditdomain.lrec.DefaultContentWordFilter;
import com.learney.contentaudit.auditdomain.findingengine.DefaultAnalyzerCatalog;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

/** No NLP server, judges or ledger: the existing lexical analyzers are the full-rebuild oracle. */
class WorkingVocabularyTest {
    private static final List<String> NAMES = List.of("lemma-count", "lemma-absence", "coca-buckets-distribution");

    @Test
    void deltasMatchFullRebuildAfterEveryReplacementIncludingRemovalAndThresholdCrossings() {
        IAuditEngine engine = engine();
        List<AuditableKnowledge> a1 = new ArrayList<>(List.of(knowledge("k1", quiz("q1", "cat", "cat")),
                knowledge("k2", quiz("q2", "cat"))));
        List<AuditableKnowledge> a2 = new ArrayList<>(List.of(knowledge("k3", quiz("q3", "dog", "be"))));
        AuditableCourse original = course(a1, a2);
        AuditReport base = full(engine, original);
        Object baseDiagnosis = base.getRoot().getDiagnoses();
        WorkingVocabulary working = engine.openWorkingVocabulary(original);
        assertEquivalent(full(engine, original), working);

        List<List<String>> changes = List.of(List.of("dog", "dog", "be"), List.of("cat", "dog"),
                List.of("cat"), List.of(), List.of("unknown", "run", "run#NOUN"), List.of("dog"));
        for (List<String> words : changes) {
            AuditableKnowledge changed = knowledge("k1", quiz("q1", words.toArray(String[]::new)));
            working.replaceKnowledge(changed);
            a1.set(0, changed);
            assertEquivalent(full(engine, course(a1, a2)), working);
        }
        working.replaceKnowledge(knowledge("k2")); // Last cat disappears, not a zero-count ghost.
        a1.set(1, knowledge("k2"));
        assertEquivalent(full(engine, course(a1, a2)), working);
        assertSame(baseDiagnosis, base.getRoot().getDiagnoses());
        assertEquals(2, count(base, "cat"));
        assertEquals(0, working.<Integer>query((report, version) -> count(report, "cat")).intValue());
    }

    @Test
    void identicalUpdatesAreIdempotentAndQueriesDoNotReprocessUnchangedQuizzes() {
        IAuditEngine engine = spy(engine());
        AuditableKnowledge k = knowledge("k1", quiz("q1", "cat"));
        WorkingVocabulary working = engine.openWorkingVocabulary(course(List.of(k), List.of()));
        assertEquals(0L, working.<Long>query((report, version) -> version).longValue());
        working.replaceKnowledge(k);
        assertEquals(0L, working.<Long>query((report, version) -> version).longValue());
        AuditableKnowledge changed = knowledge("k1", quiz("q1", "dog"));
        working.replaceKnowledge(changed);
        working.replaceKnowledge(changed);
        assertEquals(1L, working.<Long>query((report, version) -> version).longValue());
        for (int i = 0; i < 10; i++) assertEquals(1, working.<Integer>query((report, version) -> count(report, "dog")).intValue());
        verify(engine, never()).runAudit(any());
        verify(engine, never()).runAudit(any(), any());
    }

    @Test
    void rejectsInvalidIdentityBeforeChangingState() {
        WorkingVocabulary working = engine().openWorkingVocabulary(course(List.of(
                knowledge("k1", quiz("q1", "cat")), knowledge("k2", quiz("q2", "dog"))), List.of()));
        assertThrows(IllegalArgumentException.class, () -> working.replaceKnowledge(knowledge("missing")));
        assertThrows(IllegalArgumentException.class, () -> working.replaceKnowledge(knowledge("k1", quiz("q2", "run"))));
        assertThrows(IllegalArgumentException.class, () -> working.replaceKnowledge(
                knowledge("k1", quiz("q1", "cat"), quiz("q1", "dog"))));
        assertEquals(0L, working.<Long>query((report, version) -> version).longValue());
        assertEquals(1, working.<Integer>query((report, version) -> count(report, "cat")).intValue());
    }

    @Test
    void simultaneousTopicsConsolidateBothDeltasAndReadsObserveOneConsistentVersion() throws Exception {
        WorkingVocabulary working = engine().openWorkingVocabulary(course(List.of(
                knowledge("k1", quiz("q1", "cat")), knowledge("k2", quiz("q2", "cat"))), List.of()));
        CyclicBarrier start = new CyclicBarrier(3);
        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            Future<?> one = pool.submit(() -> { await(start); working.replaceKnowledge(knowledge("k1", quiz("q1", "dog"))); });
            Future<?> two = pool.submit(() -> { await(start); working.replaceKnowledge(knowledge("k2", quiz("q2", "dog"))); });
            Future<?> reader = pool.submit(() -> {
                await(start);
                for (int i = 0; i < 30; i++) working.query((report, version) -> {
                    assertEquals(2, count(report, "cat") + count(report, "dog"));
                    assertEquals(version.intValue(), count(report, "dog"));
                    return null;
                });
            });
            one.get(10, TimeUnit.SECONDS); two.get(10, TimeUnit.SECONDS); reader.get(10, TimeUnit.SECONDS);
            assertEquals(2, working.<Integer>query((report, version) -> count(report, "dog")).intValue());
        } finally { pool.shutdownNow(); }
    }

    @Test
    void randomUpdatesAndResumeMatchACompleteRebuild() {
        IAuditEngine engine = engine();
        List<AuditableKnowledge> state = new ArrayList<>(List.of(knowledge("k1", quiz("q1", "cat")),
                knowledge("k2", quiz("q2", "dog")), knowledge("k3", quiz("q3", "run"))));
        WorkingVocabulary working = engine.openWorkingVocabulary(course(state, List.of()));
        Random random = new Random(1048);
        String[] choices = {"cat", "dog", "be", "run", "run#NOUN", "unknown"};
        for (int i = 0; i < 40; i++) {
            int index = random.nextInt(state.size());
            List<String> words = new ArrayList<>();
            for (int n = random.nextInt(5); n > 0; n--) words.add(choices[random.nextInt(choices.length)]);
            AuditableKnowledge changed = knowledge("k" + (index + 1), quiz("q" + (index + 1), words.toArray(String[]::new)));
            state.set(index, changed);
            working.replaceKnowledge(changed);
            assertEquivalent(full(engine, course(state, List.of())), working);
        }
        WorkingVocabulary resumed = engine.openWorkingVocabulary(course(state, List.of()));
        assertEquivalent(full(engine, course(state, List.of())), resumed);
    }

    @Test
    void addedRemovedAndMovedQuizzesMatchAFullRebuildIncludingAnInitiallyEmptyTopic() {
        IAuditEngine engine = engine();
        List<AuditableKnowledge> state = new ArrayList<>(List.of(knowledge("k1"), knowledge("k2")));
        WorkingVocabulary working = engine.openWorkingVocabulary(course(state, List.of()));
        assertEquivalent(full(engine, course(state, List.of())), working);
        List<AuditableKnowledge> changes = List.of(
                knowledge("k1", quiz("q1", "cat"), quiz("q2", "cat", "dog")),
                knowledge("k1", quiz("q2", "dog")),
                knowledge("k2", quiz("q1", "cat")),
                knowledge("k1"),
                knowledge("k2"));
        for (AuditableKnowledge changed : changes) {
            working.replaceKnowledge(changed);
            state.set(changed.getId().equals("k1") ? 0 : 1, changed);
            assertEquivalent(full(engine, course(state, List.of())), working);
        }
        assertEquals(5L, working.<Long>query((report, version) -> version).longValue());
    }

    @Test
    void missingOrNonIncrementalAnalyzerStopsBeforeAnyContentIsProcessed() {
        AnalyzerCatalog catalog = mock(AnalyzerCatalog.class);
        AnalyzerProvider provider = mock(AnalyzerProvider.class);
        IncrementalContentAnalyzer first = mock(IncrementalContentAnalyzer.class);
        when(catalog.provider(NAMES.get(0))).thenReturn(Optional.of(provider));
        when(provider.create(null)).thenReturn(first);
        AuditableCourse course = course(List.of(knowledge("k1", quiz("q1", "cat"))), List.of());
        assertThrows(IllegalStateException.class, () -> new DefaultWorkingVocabulary(course, catalog));
        AnalyzerProvider unsupported = mock(AnalyzerProvider.class);
        when(catalog.provider(NAMES.get(1))).thenReturn(Optional.of(unsupported));
        when(unsupported.create(null)).thenReturn(mock(ContentAnalyzer.class));
        assertThrows(IllegalStateException.class, () -> new DefaultWorkingVocabulary(course, catalog));
        verifyNoInteractions(first);
    }

    static IAuditEngine engine() {
        return engine(AnalysisStrategy.LEVELS);
    }

    private static IAuditEngine engine(AnalysisStrategy strategy) {
        EvpCatalogPort evp = mock(EvpCatalogPort.class);
        Map<LemmaAndPos, CefrLevel> levels = Map.of(new LemmaAndPos("cat", "NOUN"), CefrLevel.A1,
                new LemmaAndPos("dog", "NOUN"), CefrLevel.A1, new LemmaAndPos("be", "AUX"), CefrLevel.A1,
                new LemmaAndPos("run", "VERB"), CefrLevel.A2, new LemmaAndPos("run", "NOUN"), CefrLevel.B1);
        when(evp.lookupLevel(any())).thenAnswer(i -> Optional.ofNullable(levels.get(i.getArgument(0))));
        when(evp.getExpectedLemmas(any())).thenAnswer(i -> {
            Set<LemmaAndPos> result = new HashSet<>();
            levels.forEach((lp, level) -> { if (level == i.getArgument(0)) result.add(lp); });
            return result;
        });
        when(evp.getCocaRank(any())).thenReturn(Optional.of(500));
        LemmaCountConfig count = mock(LemmaCountConfig.class);
        when(count.getThreshold()).thenReturn(2);
        when(count.getBands()).thenReturn(List.of(new LemmaCountBand("top1k", 1000, 2), new LemmaCountBand("rest", null, 3)));
        LemmaAbsenceConfig absence = mock(LemmaAbsenceConfig.class);
        when(absence.getLevelWeight(any())).thenReturn(1.0);
        when(absence.getCoverageTarget(any())).thenReturn(0.9);
        when(absence.getHighPriorityBound()).thenReturn(1000);
        when(absence.getHighReportLimit()).thenReturn(30);
        when(absence.getMediumReportLimit()).thenReturn(30);
        when(absence.getLowReportLimit()).thenReturn(30);
        CocaBucketsConfig coca = mock(CocaBucketsConfig.class);
        when(coca.getBandConfiguration()).thenReturn(new BandConfiguration(List.of(new FrequencyBand("common", 1, 1000),
                new FrequencyBand("rare", 1001, Integer.MAX_VALUE)), false));
        when(coca.getAnalysisStrategy()).thenReturn(strategy);
        List<BucketTarget> targets = List.of(new BucketTarget("common", 70, TargetKind.AT_LEAST),
                new BucketTarget("rare", 30, TargetKind.AT_MOST));
        when(coca.getTargetsForLevel(any())).thenReturn(targets);
        when(coca.getQuarterTargetsForLevel(any())).thenReturn(List.of(new QuarterBucketTargets(targets),
                new QuarterBucketTargets(targets), new QuarterBucketTargets(targets), new QuarterBucketTargets(targets)));
        List<AnalyzerProvider> providers = List.of(new LemmaCountAnalyzerProvider(evp, count),
                new LemmaAbsenceAnalyzerProvider(evp, absence, new DefaultSentenceLexicalScorer(evp,
                        new DefaultContentWordFilter(), absence)), new CocaBucketsAnalyzerProvider(mock(NlpTokenizer.class), coca));
        return new IAuditEngine(mock(ScoreAggregator.class), new DefaultAnalyzerCatalog(providers),
                mock(FindingCollector.class), mock(ContextNumbersCalculator.class));
    }

    @Test
    void deltasInEarlierTopicsPreserveQuarterAssignmentAndMatchTheFullAnalysis() {
        IAuditEngine engine = engine(AnalysisStrategy.QUARTERS);
        List<AuditableTopic> topics = new ArrayList<>();
        for (int i = 0; i < 8; i++) topics.add(new AuditableTopic(List.of(knowledge("k" + i, quiz("q" + i, "cat"))), "t" + i, "topic" + i, null));
        AuditableCourse course = new AuditableCourse(List.of(new AuditableMilestone(List.copyOf(topics), "m", "A1", null)));
        WorkingVocabulary working = engine.openWorkingVocabulary(course);
        for (int index : new int[] {0, 7, 3, 1, 5}) {
            var changed = knowledge("k" + index, quiz("q" + index, "dog", "dog", "run"));
            working.replaceKnowledge(changed);
            topics.set(index, new AuditableTopic(List.of(changed), "t" + index, "topic" + index, null));
            assertEquivalent(full(engine, new AuditableCourse(List.of(new AuditableMilestone(List.copyOf(topics), "m", "A1", null)))), working);
        }
    }

    @Test
    void onlyChangedQuizzesUpdateAccumulatorsAndRepeatedReadsReuseTheSameVersion() {
        AnalyzerCatalog catalog = mock(AnalyzerCatalog.class);
        List<IncrementalContentAnalyzer> analyzers = new ArrayList<>();
        for (String name : NAMES) {
            AnalyzerProvider provider = mock(AnalyzerProvider.class);
            IncrementalContentAnalyzer analyzer = mock(IncrementalContentAnalyzer.class);
            analyzers.add(analyzer);
            when(provider.create(null)).thenReturn(analyzer);
            when(catalog.provider(name)).thenReturn(Optional.of(provider));
        }
        WorkingVocabulary working = new DefaultWorkingVocabulary(course(List.of(knowledge("k1", quiz("q1", "cat"),
                quiz("q2", "dog"))), List.of()), catalog);
        working.query((report, version) -> null);
        analyzers.forEach(org.mockito.Mockito::clearInvocations);
        working.replaceKnowledge(knowledge("k1", quiz("q1", "run"), quiz("q2", "dog")));
        for (int i = 0; i < 10; i++) working.query((report, version) -> null);
        for (IncrementalContentAnalyzer analyzer : analyzers) {
            verify(analyzer, times(1)).onQuiz(argThat(node -> node.getEntity().getId().equals("q1")));
            verify(analyzer, times(1)).onCourseComplete(any());
            verifyNoMoreInteractions(analyzer);
        }
    }

    @Test
    void failedDeltaPoisonsTheSessionRatherThanServingPartialContext() {
        AnalyzerCatalog catalog = mock(AnalyzerCatalog.class);
        IncrementalContentAnalyzer failing = null;
        for (String name : NAMES) {
            AnalyzerProvider provider = mock(AnalyzerProvider.class);
            IncrementalContentAnalyzer analyzer = mock(IncrementalContentAnalyzer.class);
            when(provider.create(null)).thenReturn(analyzer);
            when(catalog.provider(name)).thenReturn(Optional.of(provider));
            failing = analyzer;
        }
        WorkingVocabulary working = new DefaultWorkingVocabulary(course(List.of(knowledge("k1", quiz("q1", "cat"))), List.of()), catalog);
        doThrow(new IllegalStateException("broken accumulator")).when(failing).onQuiz(any());
        assertThrows(IllegalStateException.class, () -> working.replaceKnowledge(knowledge("k1", quiz("q1", "dog"))));
        assertThrows(IllegalStateException.class, () -> working.query((report, version) -> null));
        assertThrows(IllegalStateException.class, () -> working.replaceKnowledge(knowledge("k1", quiz("q1", "cat"))));
    }

    private static AuditReport full(IAuditEngine engine, AuditableCourse course) {
        return engine.runAudit(course, new com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection(NAMES, Map.of()));
    }

    private static void assertEquivalent(AuditReport full, WorkingVocabulary working) {
        working.query((report, version) -> { compare(full.getRoot(), report.getRoot()); return null; });
    }

    private static void compare(AuditNode expected, AuditNode actual) {
        assertEquals(expected.getScores(), actual.getScores(), "scores of " + expected.getTarget());
        assertEquals(lexicalDiagnoses(expected.getDiagnoses()), lexicalDiagnoses(actual.getDiagnoses()),
                "diagnoses of " + expected.getTarget());
        assertEquals(expected.getChildren().size(), actual.getChildren().size());
        for (int i = 0; i < expected.getChildren().size(); i++) compare(expected.getChildren().get(i), actual.getChildren().get(i));
    }

    private static List<?> lexicalDiagnoses(NodeDiagnoses diagnoses) {
        // The containers are not value objects; their typed diagnoses are.
        if (diagnoses instanceof CourseDiagnoses d) return List.of(d.getLemmaCountDiagnosis(), d.getLemmaAbsenceDiagnosis(), d.getCocaBucketsDiagnosis());
        if (diagnoses instanceof LevelDiagnoses d) return List.of(d.getLemmaCountDiagnosis(), d.getLemmaAbsenceDiagnosis(), d.getCocaBucketsDiagnosis());
        if (diagnoses instanceof TopicDiagnoses d) return List.of(d.getLemmaAbsenceDiagnosis(), d.getCocaBucketsDiagnosis());
        if (diagnoses instanceof KnowledgeDiagnoses d) return List.of(d.getLemmaAbsenceDiagnosis());
        if (diagnoses instanceof QuizDiagnoses d) return List.of(d.getLemmaAbsenceDiagnosis());
        throw new AssertionError(diagnoses);
    }

    static int count(AuditReport report, String lemma) {
        return ((DefaultCourseDiagnoses) report.getRoot().getDiagnoses()).getLemmaCountDiagnosis().orElseThrow()
                .getResult().getExposures().stream().filter(e -> e.getLemmaAndPos().getLemma().equals(lemma))
                .mapToInt(LemmaExposure::getCount).sum();
    }

    static AuditableKnowledge knowledge(String id, AuditableQuiz... quizzes) {
        return new AuditableKnowledge(List.of(quizzes), id, "", true, id, id, null, null, "topic");
    }

    static AuditableQuiz quiz(String id, String... words) {
        List<NlpToken> tokens = new ArrayList<>();
        for (String word : words) {
            String[] parts = word.split("#");
            String pos = parts.length > 1 ? parts[1] : word.equals("be") ? "AUX" : word.equals("run") ? "VERB" : "NOUN";
            tokens.add(new NlpToken(parts[0], parts[0], pos, word.equals("dog") ? 2500 : 500, false, false));
        }
        return new AuditableQuiz(tokens, id, id, null, null, List.of(String.join(" ", words)), null, null, null, null, null, null);
    }

    static AuditableCourse course(List<AuditableKnowledge> a1, List<AuditableKnowledge> a2) {
        return new AuditableCourse(List.of(new AuditableMilestone(List.of(new AuditableTopic(List.copyOf(a1), "t1", "topic1", null)),
                "a1", "A1", null), new AuditableMilestone(List.of(new AuditableTopic(List.copyOf(a2), "t2", "topic2", null)), "a2", "A2", null)));
    }

    private static void await(CyclicBarrier barrier) {
        try { barrier.await(5, TimeUnit.SECONDS); } catch (Exception e) { throw new AssertionError(e); }
    }
}
