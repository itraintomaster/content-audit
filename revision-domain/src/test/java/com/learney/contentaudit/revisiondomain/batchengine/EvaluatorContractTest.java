package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.auditdomain.AnalyzerDescriptor;
import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerFamily;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingIdentity;
import com.learney.contentaudit.auditdomain.finding.FindingNodeRef;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.auditdomain.findingengine.DefaultContextNumbersCalculator;
import com.learney.contentaudit.auditdomain.judging.JudgeAbstention;
import com.learney.contentaudit.auditdomain.judging.JudgeVerdictFreshness;
import com.learney.contentaudit.revisiondomain.batch.CurrentAnalysisService.Assessment;
import com.learney.contentaudit.revisiondomain.batch.CurrentAnalysisService.ChangeVerdict;
import com.learney.contentaudit.revisiondomain.batch.CurrentAnalysisService.Route;
import com.learney.contentaudit.revisiondomain.batch.CurrentAnalysisService.Standing;
import com.learney.contentaudit.revisiondomain.changereview.ChangeReview;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** F-CONTR: the five steps, the reviewer, the route and the verdict of a change, each with its own case. */
class EvaluatorContractTest {
    static final String JUDGE = "naturalness", TRANSLATION = "translation-sense", ANALYZER = "grammar",
            CHOICE = "multiple-choice-validity", COHERENCE = "data-coherence", PAIRS = "near-duplicates";
    final Map<String, Map<String, Map<String, Object>>> marks = new HashMap<>();
    int ids;

    /** An exercise to complete, alone in its topic, with what each analyzer left on it and its numbers counted. */
    AuditNode exercise(Map<String, Double> scores, List<Finding> findings, List<String> unevaluated, List<String> partial) {
        var course = new AuditNode(null, AuditTarget.COURSE, null, new ArrayList<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), null);
        var topic = new AuditNode(null, AuditTarget.KNOWLEDGE, course, new ArrayList<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), null);
        course.getChildren().add(topic);
        var quiz = new AuditableQuiz();
        quiz.setId("q" + ids++);
        var node = new AuditNode(quiz, AuditTarget.QUIZ, topic, new ArrayList<>(), new LinkedHashMap<>(scores), new LinkedHashMap<>(), null);
        node.setFindings(new ArrayList<>(findings));
        node.setUnevaluatedBy(new ArrayList<>(unevaluated));
        node.setPartiallyEvaluatedBy(new ArrayList<>(partial));
        topic.getChildren().add(node);
        new DefaultContextNumbersCalculator().compute(course, List.of(card(ANALYZER), card(COHERENCE), card(PAIRS), card(JUDGE), card(TRANSLATION)));
        return node;
    }
    AuditNode scored(String analyzer, Finding... findings) { return exercise(Map.of(analyzer, findings.length == 0 ? 1. : 0.), List.of(findings), List.of(), List.of()); }
    static AnalyzerDescriptor card(String name) {
        var card = new AnalyzerDescriptor();
        card.setName(name); card.setFamily(AnalyzerFamily.ERRORS); card.setEvaluatedTargets(List.of(AuditTarget.QUIZ));
        return card;
    }
    static Finding finding(String analyzer, String rule, FindingSeverity severity) {
        var f = new Finding(); f.setAnalyzer(analyzer); f.setRule(rule); f.setSeverity(severity);
        f.setResolution(FindingResolution.PANEL); f.setNode(new FindingNodeRef(AuditTarget.QUIZ, "q", "quiz"));
        return f;
    }
    void mark(AuditNode node, String judge, JudgeVerdictFreshness freshness, JudgeAbstention abstention) {
        var shown = new HashMap<String, Object>(); shown.put("freshness", freshness); shown.put("abstention", abstention);
        marks.computeIfAbsent(node.getEntity().getId(), k -> new HashMap<>()).put(judge, shown);
    }
    EvaluatorContract.Reading reading(AuditNode... nodes) {
        var map = new LinkedHashMap<String, AuditNode>();
        for (var node : nodes) map.put(node.getEntity().getId(), node);
        return new EvaluatorContract.Reading(map, Set.of(JUDGE, TRANSLATION), marks, EvaluatorContract.measured(map.values()));
    }
    Assessment assess(String evaluator, AuditNode node) { return EvaluatorContract.assess(evaluator, node, reading(node)); }
    static Assessment a(Standing standing, String cause) { return new Assessment(standing, cause); }

    @Tag("F-CONTR-R001")
    @Test void eachStepDecidesInItsOrderForAJudge() {
        var choice = scored(JUDGE);
        assertEquals(a(Standing.NOT_APPLICABLE, "not-eligible"), assess(CHOICE, choice), "1: multiple choice does not apply to a gap");
        assertEquals(a(Standing.MISSING, "no-opinion"), assess(JUDGE, scored(JUDGE)), "2: without an opinion");
        var older = scored(JUDGE, finding(JUDGE, "unnatural", FindingSeverity.HIGH));
        mark(older, JUDGE, JudgeVerdictFreshness.FROM_BEFORE, null);
        assertEquals(a(Standing.MISSING, "older-version"), assess(JUDGE, older), "3: an earlier version decides nothing, not even a finding");
        var olderAbstention = scored(JUDGE);
        mark(olderAbstention, JUDGE, JudgeVerdictFreshness.FROM_BEFORE, JudgeAbstention.NOT_APPLICABLE);
        assertEquals(a(Standing.MISSING, "older-version"), assess(JUDGE, olderAbstention), "3 before 4");
        var notApplicable = scored(JUDGE);
        mark(notApplicable, JUDGE, JudgeVerdictFreshness.UP_TO_DATE, JudgeAbstention.NOT_APPLICABLE);
        assertEquals(a(Standing.NOT_APPLICABLE, "abstained-not-applicable"), assess(JUDGE, notApplicable), "4: it says it does not apply");
        var review = scored(JUDGE);
        mark(review, JUDGE, JudgeVerdictFreshness.UP_TO_DATE, JudgeAbstention.REVIEW);
        assertEquals(a(Standing.MISSING, "abstained-review"), assess(JUDGE, review), "4: it asks for a review");
        var context = scored(JUDGE);
        mark(context, JUDGE, JudgeVerdictFreshness.UP_TO_DATE, JudgeAbstention.NEEDS_CONTEXT);
        assertEquals(a(Standing.MISSING, "abstained-needs-context"), assess(JUDGE, context), "4: it lacks context");
        var clean = scored(JUDGE);
        mark(clean, JUDGE, JudgeVerdictFreshness.UP_TO_DATE, null);
        assertEquals(a(Standing.PASSES, "clean"), assess(JUDGE, clean), "5: nothing");
        var medium = scored(JUDGE, finding(JUDGE, "unnatural", FindingSeverity.MEDIUM));
        mark(medium, JUDGE, JudgeVerdictFreshness.UP_TO_DATE, null);
        assertEquals(a(Standing.FAILS, "finding"), assess(JUDGE, medium), "5: from medium up");
    }

    @Tag("F-CONTR-R001")
    @Test void eachStepDecidesInItsOrderForAnAnalyzer() {
        var other = scored(ANALYZER);
        assertEquals(a(Standing.MISSING, "not-measured"), assess("spanish-register", other), "2: never measured in this state");
        var reached = scored(ANALYZER);
        var notReached = exercise(Map.of(), List.of(), List.of(), List.of());
        assertEquals(a(Standing.NOT_APPLICABLE, "not-reached"),
                EvaluatorContract.assess(ANALYZER, notReached, reading(reached, notReached)), "1: measured, but not on this exercise");
        assertEquals(a(Standing.MISSING, "not-evaluated"), assess(ANALYZER, exercise(Map.of(), List.of(), List.of(ANALYZER), List.of())),
                "2: left unevaluated");
        assertEquals(a(Standing.MISSING, "not-evaluated"),
                assess(ANALYZER, exercise(Map.of(ANALYZER, 1.), List.of(), List.of(), List.of(ANALYZER))), "2: evaluated halfway");
        assertEquals(a(Standing.PASSES, "clean"), assess(ANALYZER, scored(ANALYZER)), "5: nothing");
        assertEquals(a(Standing.FAILS, "finding"), assess(ANALYZER, scored(ANALYZER, finding(ANALYZER, "agreement", FindingSeverity.HIGH))),
                "5: from medium up");
    }

    @Tag("F-CONTR-R002")
    @Test void theLightPassesWhatOnlyRanksCountsAndTheProductionSentenceIsNotTheExercises() {
        assertEquals(a(Standing.PASSES, "minor"), assess(ANALYZER, scored(ANALYZER, finding(ANALYZER, "agreement", FindingSeverity.LOW))),
                "a light finding passes and is still there");
        var light = scored(JUDGE, finding(JUDGE, "unnatural", FindingSeverity.LOW));
        mark(light, JUDGE, JudgeVerdictFreshness.UP_TO_DATE, null);
        assertEquals(a(Standing.PASSES, "minor"), assess(JUDGE, light), "nor does a judge's");
        var ranks = finding(ANALYZER, "offers-another", FindingSeverity.MEDIUM);
        ranks.setResolution(FindingResolution.RANK_ONLY);
        assertEquals(Standing.FAILS, assess(ANALYZER, scored(ANALYZER, ranks)).standing(), "what only ranks counts by its severity");
        var production = finding(COHERENCE, "saved-sentence", FindingSeverity.HIGH);
        production.setIdentity(new FindingIdentity("data-coherence|saved-sentence|q|produccion", "produccion"));
        assertEquals(a(Standing.PASSES, "clean"), assess(COHERENCE, scored(COHERENCE, production)), "publishing fixes the production sentence");
        var tree = finding(COHERENCE, "saved-sentence", FindingSeverity.HIGH);
        tree.setIdentity(new FindingIdentity("data-coherence|saved-sentence|q|arbol", "arbol"));
        assertEquals(Standing.FAILS, assess(COHERENCE, scored(COHERENCE, tree)).standing(), "the exercise's own sentence does stop it");
        assertEquals(Standing.FAILS, assess(ANALYZER, scored(ANALYZER, finding(ANALYZER, "agreement", null))).standing(),
                "a finding without severity is not assumed light");
    }

    @Tag("F-CONTR-R003")
    @Test void aDisagreementWaitsUnlessTheSameEvaluatorAlreadyFails() {
        var disagree = finding(TRANSLATION, "judges-disagree", FindingSeverity.HIGH);
        var alone = scored(TRANSLATION, disagree);
        mark(alone, TRANSLATION, JudgeVerdictFreshness.UP_TO_DATE, null);
        assertEquals(a(Standing.MISSING, "judges-disagree"), assess(TRANSLATION, alone));
        var withLight = scored(TRANSLATION, disagree, finding(TRANSLATION, "loose", FindingSeverity.LOW));
        mark(withLight, TRANSLATION, JudgeVerdictFreshness.UP_TO_DATE, null);
        assertEquals(a(Standing.MISSING, "judges-disagree"), assess(TRANSLATION, withLight));
        var withMedium = scored(TRANSLATION, disagree, finding(TRANSLATION, "wrong-sense", FindingSeverity.MEDIUM));
        mark(withMedium, TRANSLATION, JudgeVerdictFreshness.UP_TO_DATE, null);
        assertEquals(a(Standing.FAILS, "finding"), assess(TRANSLATION, withMedium));
    }

    @Tag("F-CONTR-R003")
    @Test void aPaidEvaluatorWithoutAJudgeOfTodayCountsAnyVersion() {
        var pairs = scored(PAIRS, finding(PAIRS, "same-exercise", FindingSeverity.LOW));
        pairs.getMetadata().put(PAIRS + "Version", "juez-729229ed1d8d");
        assertEquals(a(Standing.PASSES, "minor"), assess(PAIRS, pairs), "no version of today to mark it against");
    }

    static ChangeReview.Row row(ChangeReview.State state, ChangeReview.Verdict verdict) {
        return new ChangeReview.Row("q", "tema", state, verdict, "v", null, "antes", "ahora");
    }
    static ChangeReview.Verdict verdict(ChangeReview.Mark mark, boolean inherited) {
        return new ChangeReview.Verdict("q", mark, mark == ChangeReview.Mark.GOOD ? List.of()
                : List.of(new ChangeReview.Problem(ChangeReview.Where.EXERCISE, "ahora", "algo", inherited)), "razón");
    }

    @Tag("F-CONTR-R004")
    @Test void theReviewerKeepsTheSameContract() {
        assertEquals(a(Standing.NOT_APPLICABLE, "unchanged"), EvaluatorContract.reviewer(null));
        assertEquals(a(Standing.MISSING, "unmarked"), EvaluatorContract.reviewer(row(ChangeReview.State.UNMARKED, null)));
        assertEquals(a(Standing.MISSING, "unusable"), EvaluatorContract.reviewer(row(ChangeReview.State.UNUSABLE, null)));
        assertEquals(a(Standing.PASSES, "good"), EvaluatorContract.reviewer(row(ChangeReview.State.MARKED, verdict(ChangeReview.Mark.GOOD, false))));
        assertEquals(a(Standing.PASSES, "doubtful"), EvaluatorContract.reviewer(row(ChangeReview.State.MARKED, verdict(ChangeReview.Mark.DOUBTFUL, false))));
        assertEquals(a(Standing.FAILS, "inherited"), EvaluatorContract.reviewer(row(ChangeReview.State.MARKED, verdict(ChangeReview.Mark.DOUBTFUL, true))));
        assertEquals(a(Standing.FAILS, "worse"), EvaluatorContract.reviewer(row(ChangeReview.State.MARKED, verdict(ChangeReview.Mark.WORSE, false))));
        assertEquals(a(Standing.FAILS, "worse"), EvaluatorContract.reviewer(row(ChangeReview.State.OLDER_VERSION, verdict(ChangeReview.Mark.WORSE, false))),
                "a mark of an earlier version counts too");
        assertEquals(a(Standing.FAILS, "inherited"), EvaluatorContract.reviewerInProduction(row(ChangeReview.State.MARKED, verdict(ChangeReview.Mark.DOUBTFUL, true))),
                "production already had it");
        assertEquals(a(Standing.NOT_APPLICABLE, "production"), EvaluatorContract.reviewerInProduction(row(ChangeReview.State.MARKED, verdict(ChangeReview.Mark.WORSE, false))));
    }

    @Tag("F-CONTR-R005")
    @Test void oneRouteDecidesTheList() {
        var na = a(Standing.NOT_APPLICABLE, "x"); var missing = a(Standing.MISSING, "x");
        var passes = a(Standing.PASSES, "x"); var fails = a(Standing.FAILS, "x");
        assertEquals(Route.HAS_A_PROBLEM, EvaluatorContract.route(List.of(passes, missing, fails)), "a failure beats a missing opinion");
        assertEquals(Route.OPINIONS_MISSING, EvaluatorContract.route(List.of(passes, na, missing)));
        assertEquals(Route.ALL_PASS, EvaluatorContract.route(List.of(passes, na)));
        assertEquals(Route.ALL_PASS, EvaluatorContract.route(List.of(na)), "nobody applying is nobody objecting");
    }

    @Tag("F-CONTR-R006")
    @Test void theChangeVerdictFollowsTheSameRoute() {
        var fails = a(Standing.FAILS, "x");
        Map<String, Assessment> now = Map.of(JUDGE, fails);
        assertEquals(ChangeVerdict.WORSENED, EvaluatorContract.change(Route.HAS_A_PROBLEM, now, Map.of(JUDGE, a(Standing.PASSES, "x"))));
        assertEquals(ChangeVerdict.WORSENED, EvaluatorContract.change(Route.HAS_A_PROBLEM, now, Map.of(JUDGE, a(Standing.NOT_APPLICABLE, "x"))));
        assertEquals(ChangeVerdict.NOT_SHOWN_FIXED, EvaluatorContract.change(Route.HAS_A_PROBLEM, now, Map.of(JUDGE, a(Standing.MISSING, "x"))),
                "production without an opinion does not prove it got worse");
        assertEquals(ChangeVerdict.NOT_SHOWN_FIXED, EvaluatorContract.change(Route.HAS_A_PROBLEM, now, Map.of(JUDGE, fails)), "it was already there");
        assertEquals(ChangeVerdict.NOT_MEASURED, EvaluatorContract.change(Route.OPINIONS_MISSING, Map.of(JUDGE, a(Standing.MISSING, "x")), Map.of()));
        assertEquals(ChangeVerdict.CONSIDERED_GOOD, EvaluatorContract.change(Route.ALL_PASS, Map.of(JUDGE, a(Standing.PASSES, "x")), Map.of()));
    }

    @Tag("F-CONTR-R006")
    @Test void theReviewerWorsensWithWorseAndNotWithInheritedAndAnUnchangedExerciseHasNoVerdict() {
        var worse = scored(ANALYZER); var inherited = scored(ANALYZER); var unchanged = scored(ANALYZER);
        var production = reading(scored(ANALYZER), scored(ANALYZER), scored(ANALYZER));
        var now = reading(worse, inherited, unchanged);
        Map<String, ChangeReview.Row> reviewed = new HashMap<>();
        reviewed.put(worse.getEntity().getId(), row(ChangeReview.State.MARKED, verdict(ChangeReview.Mark.WORSE, false)));
        reviewed.put(inherited.getEntity().getId(), row(ChangeReview.State.MARKED, verdict(ChangeReview.Mark.DOUBTFUL, true)));
        var result = EvaluatorContract.evaluate(List.of(ANALYZER), production, now, reviewed);
        assertEquals(ChangeVerdict.WORSENED, result.get(worse.getEntity().getId()).change());
        assertEquals(Route.HAS_A_PROBLEM, result.get(inherited.getEntity().getId()).route());
        assertEquals(ChangeVerdict.NOT_SHOWN_FIXED, result.get(inherited.getEntity().getId()).change(), "the problem was already in production");
        var same = result.get(unchanged.getEntity().getId());
        assertNull(same.change(), "an exercise the batch did not change has no verdict");
        assertTrue(same.before().isEmpty());
        assertEquals(a(Standing.NOT_APPLICABLE, "unchanged"), same.now().get(EvaluatorContract.REVIEWER));
        assertEquals(Route.ALL_PASS, same.route());
        assertTrue(EvaluatorContract.evaluate(List.of(ANALYZER), production, now, null).values().stream()
                .allMatch(e -> e.change() == null && !e.now().containsKey(EvaluatorContract.REVIEWER)), "without the reviewer, no verdict");
    }
}
