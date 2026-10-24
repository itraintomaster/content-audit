package com.learney.contentaudit.revisiondomain.batchengine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.batch.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;

class DidacticReferencesTest {
    final CourseEntity course = TandaFixture.course();
    final CorrectionBatch parent = TandaFixture.batch("c1");
    final CorrectionBatch child = TandaFixture.batch("c2");
    final BatchStore store = mock(BatchStore.class);
    final BatchChangeLedger ledger = mock(BatchChangeLedger.class);
    final Map<String, CorrectionBatch> batches = new HashMap<>();
    final Map<String, CourseEntity> contents = new HashMap<>();
    String quiz;
    final Instant completed = Instant.parse("2026-10-24T03:00:00Z");

    @BeforeEach void setup() {
        quiz = course.getRoot().getMilestones().get(0).getTopics().get(0).getKnowledges().get(0).getQuizTemplates().get(0).getId();
        parent.setContentVersion(11); parent.setCorrectionRevision(1);
        child.setContentVersion(3); child.setCorrectionRevision(2); child.setOpenedAt(completed.plusSeconds(10));
        var before = new CourseElementSnapshot(AuditTarget.QUIZ, quiz, TandaFixture.exercise(course, quiz), null);
        parent.setComparisonBaselines(List.of(new QuizCorrection.ComparisonBaseline("source-op", 10, quiz, before, before)));
        parent.setSelectiveRuns(List.of(run(10, 11, completed)));
        contents.put("c1", course); contents.put("c2", course);
        batches.put("c1", parent); batches.put("c2", child);
        when(store.load(anyString())).thenAnswer(i -> Optional.ofNullable(batches.get(i.getArgument(0))));
        when(ledger.composedCourse(any(), any())).thenAnswer(i -> contents.get(i.getArgument(0, CorrectionBatch.class).getId()));
        link(child, parent, course);
    }

    private static String stamp(CourseEntity course) {
        try { return new ObjectMapper().writeValueAsString(course); }
        catch (Exception e) { throw new AssertionError(e); }
    }
    private void link(CorrectionBatch next, CorrectionBatch previous, CourseEntity source) {
        next.setEvolution(new CorrectionEvolution.Lineage(previous.getId(), previous.getContentVersion(), 0,
                "evolve-" + next.getId(), stamp(source), course, source, List.of()));
    }
    private SelectiveCorrection.Run run(int before, int after, Instant finished) {
        return new SelectiveCorrection.Run("source-op", "recorrect", "all", null, 1, before, "source-fingerprint",
                null, "fake-runtime", "audit", "plan", List.of(), List.of(quiz), 1000, 1, 1, 1, 1, 0,
                "DONE", "Jose", completed.minusSeconds(5), finished, null, false, after, "result", "fake-runtime");
    }
    private Map<String, com.learney.contentaudit.revisiondomain.panel.DidacticReference> resolve() {
        return DidacticReferences.resolve(child, "source-op", List.of(quiz), false, store, ledger, DidacticReferencesTest::stamp);
    }

    @Test void resolvesOnlyTheSealedAncestorAndRetainsImmutableIdentitiesAndSnapshotHash() throws Exception {
        var reference = resolve().get(quiz);
        assertEquals(10, reference.sourceContentVersion());
        assertEquals("c1", reference.provenance().sourceBatchId());
        assertEquals("source-fingerprint", reference.provenance().sourceOperationFingerprint());
        assertTrue(reference.provenance().snapshotFingerprint().matches("[0-9a-f]{64}"));
        assertEquals("c2", reference.provenance().ancestry().get(0).batchId());
        assertEquals("c1", reference.provenance().ancestry().get(0).parentBatchId());
        assertEquals(11, reference.provenance().ancestry().get(0).parentContentVersion());
        assertEquals(stamp(course), reference.provenance().ancestry().get(0).parentFingerprint());
        assertThrows(UnsupportedOperationException.class, () -> reference.provenance().ancestry().clear());
        String oldTitle = reference.sourceQuiz().getTitle();
        reference.sourceQuiz().setTitle("external mutation");
        assertEquals(oldTitle, reference.sourceQuiz().getTitle());
        var json = new ObjectMapper();
        var loaded = json.readValue(json.writeValueAsString(reference), reference.getClass());
        assertEquals(reference, loaded);
        verify(store, never()).list(); verify(store, never()).save(any());
    }

    @Test void aGrandparentIsReachableOnlyThroughBothExplicitSeals() {
        var latest = TandaFixture.batch("c3"); latest.setContentVersion(2); latest.setOpenedAt(completed.plusSeconds(20));
        contents.put("c3", course); link(latest, child, course);
        var reference = DidacticReferences.resolve(latest, "source-op", List.of(quiz), false, store, ledger,
                DidacticReferencesTest::stamp).get(quiz);
        assertEquals(List.of("c3", "c2"), reference.provenance().ancestry().stream().map(s -> s.batchId()).toList());
        verify(store, never()).list();
    }

    @Test void anUnrelatedBatchIsNeverSearchedAndMissingOrCyclicParentsAreRejected() {
        child.setEvolution(null);
        assertThrows(IllegalArgumentException.class, this::resolve);
        verify(store, never()).load("c1"); verify(store, never()).list();
        link(child, parent, course); batches.remove("c1");
        assertThrows(IllegalArgumentException.class, this::resolve);
        batches.put("c1", parent); link(parent, child, course);
        assertThrows(IllegalArgumentException.class, this::resolve);
    }

    @Test void duplicateOperationIdentitiesAcrossAncestorsAreRejected() {
        child.setSelectiveRuns(parent.getSelectiveRuns());
        assertThrows(IllegalArgumentException.class, this::resolve);
    }

    @Test void changedVersionCheckpointOrActualParentContentCannotServeAsAnAncestor() {
        parent.setContentVersion(12); assertThrows(IllegalArgumentException.class, this::resolve);
        parent.setContentVersion(11);
        var changed = CorrectionEvolution.copyCourse(course); TandaFixture.exercise(changed, quiz).setTitle("changed");
        contents.put("c1", changed); assertThrows(IllegalArgumentException.class, this::resolve);
        contents.put("c1", course);
        child.setEvolution(new CorrectionEvolution.Lineage("c1", 11, 0, "evolve", "wrong-fingerprint", course, course, List.of()));
        assertThrows(IllegalArgumentException.class, this::resolve);
    }

    @Test void sourceAfterTheCheckpointOrOutsideItsQuizScopeIsRejected() {
        parent.setSelectiveRuns(List.of(run(10, 12, completed)));
        assertThrows(IllegalArgumentException.class, this::resolve);
        parent.setSelectiveRuns(List.of(run(10, 11, child.getOpenedAt().plusSeconds(1))));
        assertThrows(IllegalArgumentException.class, this::resolve);
        parent.setSelectiveRuns(List.of(run(10, 11, completed)));
        var reduced = CorrectionEvolution.copyCourse(course);
        var knowledge = reduced.getRoot().getMilestones().get(0).getTopics().get(0).getKnowledges().get(0);
        knowledge.setQuizTemplates(knowledge.getQuizTemplates().stream().filter(q -> !quiz.equals(q.getId())).toList());
        contents.put("c1", reduced); link(child, parent, reduced);
        assertThrows(IllegalArgumentException.class, this::resolve);
    }

    @Test void aChangedHistoricalSnapshotChangesTheHashWithoutChangingTheEditableBase() {
        var original = resolve().get(quiz);
        var baseline = parent.getComparisonBaselines().get(0);
        var changed = baseline.before(); changed.getQuiz().setInstructions("Changed historical instructions");
        parent.setComparisonBaselines(List.of(new QuizCorrection.ComparisonBaseline("source-op", 10, quiz, changed, baseline.original())));
        assertNotEquals(original.provenance().snapshotFingerprint(), resolve().get(quiz).provenance().snapshotFingerprint());
        assertEquals(3, child.getContentVersion()); assertEquals(11, parent.getContentVersion());
        verify(store, never()).save(any());
    }
}
