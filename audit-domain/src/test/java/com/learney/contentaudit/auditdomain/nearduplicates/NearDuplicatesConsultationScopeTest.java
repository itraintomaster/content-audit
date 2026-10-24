package com.learney.contentaudit.auditdomain.nearduplicates;

import static com.learney.contentaudit.auditdomain.nearduplicates.NearDuplicatesAnalyzerTest.*;
import static org.junit.jupiter.api.Assertions.*;

import com.learney.contentaudit.auditdomain.AuditableCourse;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.EvaluationRunPolicy;
import com.learney.contentaudit.auditdomain.EvaluationRunPurpose;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("F-DUPAR-R001")
@Tag("F-DUPAR-R009")
class NearDuplicatesConsultationScopeTest {
    private static final String FIRST = "scope-first", SELECTED = "scope-selected", LAST = "scope-last";
    private static final String FIRST_TEXT = "Offer: I'll wash the dishes for you.";
    private static final String SELECTED_TEXT = "Offer: I'll wash the dishes.";
    private static final String LAST_TEXT = "Offer: I'll wash the dishes today.";

    @Test void selectingTheMiddleQuizQueriesBothEndpointRolesAndLeavesTheOtherPairPending() {
        var world = scopedWorld(false);
        var content = threeQuizzes();

        var root = world.run(content, restricted(Set.of(SELECTED)));

        assertEquals(Set.of(FIRST + "~" + SELECTED, SELECTED + "~" + LAST), askedPairs(world),
                "the selected quiz is copy in one pair and kept in the other");
        assertEquals(3, quiz(root, SELECTED).getParent().getChildren().size(), "neighbors remain in the report");
        assertTrue(halfway(root, LAST), "the unqueried pair of two other neighbors remains pending");
        assertFalse(halfway(root, SELECTED), "the selected quiz's own earlier pair was evaluated");
        assertEquals(1, coverageOf(root).getCost().getPendingConsultations());
        var selectedAsKept = world.claude.subjects.stream()
                .filter(s -> (SELECTED + "~" + LAST).equals(s.getSubjectRef())).findFirst().orElseThrow();
        assertTrue(selectedAsKept.getContent().get("second").contains(LAST_TEXT),
                "the full neighboring exercise remains in the selected pair's context");
    }

    @Test void restrictingNewQueriesReusesCurrentVerdictsWithoutReevaluatingTheSelectedPair() {
        var world = scopedWorld(false);
        world.run(threeQuizzes(), budget(20));
        assertEquals(3, world.claude.calls());

        var root = world.run(threeQuizzes(), restricted(Set.of(SELECTED)));
        world.run(threeQuizzes(), restricted(Set.of(SELECTED)));

        assertEquals(3, world.claude.calls(), "current fingerprints are reused inside and outside the scope");
        assertFalse(halfway(root, LAST), "the other-neighbor pair keeps its cached verdict");
        assertEquals(0, coverageOf(root).getCost().getPendingConsultations());
    }

    @Test void scopeOverridesForcedReevaluationForPairsOfTwoOtherNeighbors() {
        var world = scopedWorld(false);
        world.run(threeQuizzes(), budget(20));
        int prior = world.claude.calls();
        var policy = new EvaluationRunPolicy(20, false, null, Set.of(FIRST, SELECTED, LAST),
                EvaluationRunPurpose.REANALYSIS, 1);
        policy.setNewEvaluationSubjectIds(Set.of(SELECTED));

        var root = world.run(threeQuizzes(), policy);

        assertEquals(List.of(FIRST + "~" + SELECTED, SELECTED + "~" + LAST),
                world.claude.subjects.subList(prior, world.claude.calls()).stream()
                        .map(s -> s.getSubjectRef()).toList());
        assertFalse(halfway(root, LAST), "the nonselected pair reused its current verdict despite FORCE");
    }

    @Test void anEmptyScopeMakesEveryPairCacheOnlyEvenWithForcedReevaluation() {
        var world = scopedWorld(false);
        var policy = new EvaluationRunPolicy(20, false, null, Set.of(FIRST, SELECTED, LAST),
                EvaluationRunPurpose.REANALYSIS, 1);
        policy.setNewEvaluationSubjectIds(Set.of());

        var root = world.run(threeQuizzes(), policy);

        assertEquals(0, world.claude.calls());
        assertTrue(halfway(root, SELECTED));
        assertTrue(halfway(root, LAST));
        assertEquals(3, coverageOf(root).getCost().getPendingConsultations());
    }

    @Test void vectorsAreComputedOnlyForEndpointsNeededBySelectedPairsAndReuseTheRest() {
        var world = scopedWorld(true);
        world.nomic.recorded(List.of(FIRST_TEXT, SELECTED_TEXT, LAST_TEXT), 0.95, 0.95, 0.95);
        var content = course(topic("Ofertas", first(), selected(), last()),
                topic("Afirmativas con be", sorry(), hot()));

        var root = world.run(content, restricted(Set.of(SELECTED)));

        assertEquals(sorted(List.of("clustering: " + FIRST_TEXT, "clustering: " + SELECTED_TEXT,
                "clustering: " + LAST_TEXT)), sorted(world.nomic.asked()),
                "both endpoints are context for eligible pairs, the other topic is cache-only");
        assertEquals(Set.of(FIRST + "~" + SELECTED, SELECTED + "~" + LAST), askedPairs(world));
        assertTrue(halfway(root, HOT), "missing unrelated vectors cannot fabricate a complete evaluation");
        assertTrue(halfway(root, LAST), "available vectors do not authorize the unrelated pair's judge");
        int vectors = world.nomic.asked().size(), judgments = world.claude.calls();
        world.run(content, restricted(Set.of(SELECTED)));
        assertEquals(vectors, world.nomic.asked().size());
        assertEquals(judgments, world.claude.calls(), "repeated scoped reads reuse selected pairs too");
    }

    @Test void emptyScopeReusesRecordedVectorsAndJudgmentsWithoutNewLocalOrPaidCalls() {
        var world = scopedWorld(true);
        world.nomic.recorded(List.of(FIRST_TEXT, SELECTED_TEXT, LAST_TEXT), 0.95, 0.95, 0.95);
        world.run(threeQuizzes(), budget(20));
        int vectors = world.nomic.asked().size(), judgments = world.claude.calls();

        var root = world.run(threeQuizzes(), restricted(Set.of()));

        assertEquals(vectors, world.nomic.asked().size());
        assertEquals(judgments, world.claude.calls());
        assertFalse(halfway(root, LAST));
    }

    @Test void vectorPolicyRetainsTheNewConsultationRestrictionWhenRemovingItsBudgetLimit() {
        var world = scopedWorld(true);
        var policy = restricted(Set.of(SELECTED));
        world.provider.create(policy);
        int index = world.recording.evaluators.indexOf(List.of(VECTORS_ID));

        assertEquals(Integer.MAX_VALUE, world.recording.policies.get(index).getMaxNewEvaluations());
        assertEquals(Set.of(SELECTED), world.recording.policies.get(index).getNewEvaluationSubjectIds());
        assertEquals(Set.of(SELECTED), world.recording.opened.get(index).getEffectivePolicy().getNewEvaluationSubjectIds());
    }

    private static World scopedWorld(boolean local) {
        var world = local ? World.withNomic() : World.withoutOllama();
        world.claude.answers(FIRST_TEXT, SELECTED_TEXT, false, "Distintos ejercicios");
        world.claude.answers(FIRST_TEXT, LAST_TEXT, false, "Distintos ejercicios");
        world.claude.answers(SELECTED_TEXT, LAST_TEXT, false, "Distintos ejercicios");
        return world;
    }

    private static EvaluationRunPolicy restricted(Set<String> ids) {
        var policy = budget(20);
        policy.setNewEvaluationSubjectIds(ids);
        return policy;
    }

    private static Set<String> askedPairs(World world) {
        return world.claude.subjects.stream().map(s -> s.getSubjectRef()).collect(java.util.stream.Collectors.toSet());
    }

    private static AuditableCourse threeQuizzes() { return course(topic("Ofertas", first(), selected(), last())); }
    private static AuditableQuiz first() { return offer(FIRST, "(wash) the dishes for you."); }
    private static AuditableQuiz selected() { return offer(SELECTED, "(wash) the dishes."); }
    private static AuditableQuiz last() { return offer(LAST, "(wash) the dishes today."); }
    private static AuditableQuiz offer(String id, String ending) {
        return cloze(id, "Ofertas", text("Offer: I"), gap("'ll wash|will wash"), text(ending));
    }
}
