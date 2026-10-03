package com.learney.contentaudit.auditdomain.nearduplicates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableCourse;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableMilestone;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.AuditableTopic;
import com.learney.contentaudit.auditdomain.CorrectorMatcher;
import com.learney.contentaudit.auditdomain.CourseDiagnoses;
import com.learney.contentaudit.auditdomain.EvaluationRunPolicy;
import com.learney.contentaudit.auditdomain.EvaluationRunPurpose;
import com.learney.contentaudit.auditdomain.IAuditEngine;
import com.learney.contentaudit.auditdomain.IScoreAggregator;
import com.learney.contentaudit.auditdomain.JudgeExamRegistry;
import com.learney.contentaudit.auditdomain.JudgeRuntimeConfig;
import com.learney.contentaudit.auditdomain.QuizDiagnoses;
import com.learney.contentaudit.auditdomain.QuizStudentViewRenderer;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.contextnumbers.AnalyzerErrorCounts;
import com.learney.contentaudit.auditdomain.correctormatch.DefaultCorrectorMatcher;
import com.learney.contentaudit.auditdomain.correctormatch.DefaultCorrectorMatcherTest;
import com.learney.contentaudit.auditdomain.finding.AnalysisCost;
import com.learney.contentaudit.auditdomain.finding.EvidencePart;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingNodeRef;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.auditdomain.findingengine.DefaultAnalyzerCatalog;
import com.learney.contentaudit.auditdomain.findingengine.DefaultContextNumbersCalculator;
import com.learney.contentaudit.auditdomain.findingengine.DefaultFindingCollector;
import com.learney.contentaudit.auditdomain.judging.EvaluatorCoverage;
import com.learney.contentaudit.auditdomain.judging.JudgeCostEstimate;
import com.learney.contentaudit.auditdomain.judging.JudgeCoverageDiagnosis;
import com.learney.contentaudit.auditdomain.judging.JudgeExamRecord;
import com.learney.contentaudit.auditdomain.judging.JudgeExamStanding;
import com.learney.contentaudit.auditdomain.judging.JudgeExamStatus;
import com.learney.contentaudit.auditdomain.judging.JudgeRequestPlan;
import com.learney.contentaudit.auditdomain.judging.JudgeRuntime;
import com.learney.contentaudit.auditdomain.judging.JudgeSessions;
import com.learney.contentaudit.auditdomain.judging.JudgeUnitTally;
import com.learney.contentaudit.auditdomain.judgingengine.DefaultJudgeRuntime;
import com.learney.contentaudit.auditdomain.localtools.EmbeddingModel;
import com.learney.contentaudit.auditdomain.localtools.LocalToolRunDiagnosis;
import com.learney.contentaudit.auditdomain.localtools.LocalToolStatus;
import com.learney.contentaudit.auditdomain.localtools.SentenceEmbedding;
import com.learney.contentaudit.auditdomain.localtoolsengine.DefaultLocalToolGate;
import com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer;
import com.learney.contentaudit.coursedomain.MultipleChoiceEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceItemEntity;
import com.learney.contentaudit.coursedomain.SentenceMode;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationBudget;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationEmitted;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationFailureKind;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationKey;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationLedger;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationNotEmitted;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationOutcome;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRecord;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationResolutionKind;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSession;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSubject;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationTimeline;
import com.learney.contentaudit.evaluationledgerdomain.Evaluator;
import com.learney.contentaudit.evaluationledgerdomain.contentfingerprint.Sha256ContentFingerprinter;
import com.learney.contentaudit.evaluationledgerdomain.evaluationsession.DefaultEvaluationSessionFactory;
import com.learney.contentaudit.evaluationledgerdomain.evaluationsession.ExamEvaluationSessionFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import javax.annotation.processing.Generated;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Generated(
        value = "com.sentinel.SentinelEngine",
        comments = "Generated by Sentinel"
)
public class NearDuplicatesAnalyzerTest {
    @Test
    @DisplayName("should compare an exercise only with the earlier exercises of its own topic, never with an exercise of another topic")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R001")
    public void shouldCompareAnExerciseOnlyWithTheEarlierExercisesOfItsOwnTopicNeverWithAnExerciseOfAnotherTopic(
            ) {
        // «Where is he?» and «Where is she?» in two topics of the same name, and then in one, the other way round.
        AuditNode apart = World.withNomic().run(course(topic("Contraste inicial", whereHe()),
                topic("Contraste inicial", whereShe())), null);
        AuditNode together = World.withNomic().run(course(topic("Contraste inicial", whereShe(), whereHe())), null);

        assertEquals(List.of(), findings(quiz(apart, WHERE_SHE)), "never compared with another topic");
        assertEquals(1.0, score(apart, WHERE_HE));
        assertEquals(1.0, score(apart, WHERE_SHE));
        assertEquals(List.of(), findings(quiz(together, WHERE_SHE)), "the first of its topic has nothing before it");
        assertEquals(WHERE_SHE, only(quiz(together, WHERE_HE)).getIdentity().getMarker(),
                "only with the earlier one: «Where is he?» after «Where is she?» names it");
    }

    @Test
    @DisplayName("should leave the two exact «They weren't at the park.» of Práctica mixta to exact-duplicates and still mark both as near copies of «You weren't at the park.»")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R001")
    public void shouldLeaveTheTwoExactTheyWerentAtTheParkOfPrcticaMixtaToExactduplicatesAndStillMarkBothAsNearCopiesOfYouWerentAtThePark(
            ) {
        World world = World.withNomic();

        AuditNode root = world.run(course(topic("Práctica mixta", youPark(), theyPark(THEY_PARK),
                theyPark(THEY_PARK_AGAIN))), null);

        // …224b and …2260 are the same exercise: that pair is exact-duplicates', so each is a near copy of …2231.
        assertEquals(List.of(), findings(quiz(root, YOU_PARK)));
        assertEquals(YOU_PARK, only(quiz(root, THEY_PARK)).getIdentity().getMarker());
        assertEquals(YOU_PARK, only(quiz(root, THEY_PARK_AGAIN)).getIdentity().getMarker(), "never …224b");
        assertEquals("same-but-wildcard", only(quiz(root, THEY_PARK_AGAIN)).getRule());
        assertEquals("You → They", diagnosis(root, THEY_PARK_AGAIN).getDifference());
        assertEquals(List.of(), world.nomic.asked(), "the wildcard settles both pairs: no vector is asked");
    }

    @Test
    @DisplayName("should leave without finding the first exercise in the order of the topic and give the one after it a finding that names it, «Where is she?» naming «Where is he?» in Contraste inicial")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R003")
    public void shouldLeaveWithoutFindingTheFirstExerciseInTheOrderOfTheTopicAndGiveTheOneAfterItAFindingThatNamesItWhereIsSheNamingWhereIsHeInContrasteInicial(
            ) {
        AuditNode root = World.withNomic().run(course(topic("Contraste inicial", whereHe(), whereShe())), null);

        assertEquals(List.of(), findings(quiz(root, WHERE_HE)));
        assertEquals(1.0, score(root, WHERE_HE));
        Finding copy = only(quiz(root, WHERE_SHE));
        assertEquals(WHERE_HE, copy.getIdentity().getMarker());
        assertEquals(List.of(WHERE_HE), copy.getEvidence().getRelatedNodes().stream().map(FindingNodeRef::getNodeId)
                .toList(), "the exercise that stays is the other node of the finding");
    }

    @Test
    @DisplayName("should mark n minus 1 exercises in a group of n near duplicates, the two «Could I» after «A: Can I drive your car? B: No, you can't.» in Respuestas cortas, both naming it")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R003")
    public void shouldMarkNMinus1ExercisesInAGroupOfNNearDuplicatesTheTwoCouldIAfterACanIDriveYourCarBNoYouCantInRespuestasCortasBothNamingIt(
            ) {
        World world = World.withNomic();
        // «Could I have your car?» is a candidate of its vectors, 0.9565, and since 1/10 needs the yes of claude -p.
        world.claude.answers("A: Can I drive your car? B: No, you can't.", "A: Could I have your car? B: No, you can't.",
                true, "Piden lo mismo: permiso para usar el auto, y el alumno escribe «you can't» igual");
        world.claude.answers("A: Could I have your car? B: No, you can't.", "A: Could I drive your car? B: No, you can't.",
                true, "Piden lo mismo: permiso para usar el auto, y el alumno escribe «you can't» igual");

        AuditNode root = world.run(course(topic("Respuestas cortas", canDrive(), couldHave(), couldDrive())),
                budget(10));

        assertEquals(List.of(), findings(quiz(root, CAN_DRIVE)));
        assertEquals(CAN_DRIVE, only(quiz(root, COULD_HAVE)).getIdentity().getMarker());
        assertEquals("same-exercise", only(quiz(root, COULD_HAVE)).getRule(),
                "0.9565 with «Can I drive…», and claude -p says yes");
        assertEquals(CAN_DRIVE, only(quiz(root, COULD_DRIVE)).getIdentity().getMarker(), "the first, never …251d");
        assertEquals("same-but-wildcard", only(quiz(root, COULD_DRIVE)).getRule(), "Can → Could");
        assertEquals(2, errorCountsOf(root).getWithError(), "2 copies in a group of 3");
    }

    @Test
    @DisplayName("should give every copy a low finding for the panel that counts as an error and scores 0, and keep 1 in the exercise that stays")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R004")
    public void shouldGiveEveryCopyALowFindingForThePanelThatCountsAsAnErrorAndScores0AndKeep1InTheExerciseThatStays(
            ) {
        AuditNode root = World.withNomic().run(course(topic("Contraste inicial", whereHe(), whereShe())), null);

        Finding copy = only(quiz(root, WHERE_SHE));
        assertEquals(FindingSeverity.LOW, copy.getSeverity());
        assertEquals(FindingResolution.PANEL, copy.getResolution());
        assertEquals(0.0, score(root, WHERE_SHE));
        assertEquals(1.0, score(root, WHERE_HE));
        assertEquals(1, errorCountsOf(root).getWithError(), "it counts as an error, it does not only rank");
    }

    @Test
    @DisplayName("should show as evidence both exercises as the student sees them and «Casi igual a …208f del mismo tema: cambia he por she», never the internal format of the exercise")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R005")
    public void shouldShowAsEvidenceBothExercisesAsTheStudentSeesThemAndCasiIgualA208fDelMismoTemaCambiaHePorSheNeverTheInternalFormatOfTheExercise(
            ) {
        AuditNode root = World.withNomic().run(course(topic("Contraste inicial", whereHe(), whereShe())), null);

        Finding copy = only(quiz(root, WHERE_SHE));
        assertEquals("Where ___ (she / be)? (present simple)", part(copy, "Copia"));
        assertEquals("Where ___ (he / be)? (present simple)", part(copy, "Queda"));
        assertEquals("Where is she?", part(copy, "Copia terminada"));
        assertEquals("Where is he?", part(copy, "Queda terminado"));
        assertEquals("Casi igual a …208f del mismo tema: cambia he por she", part(copy, "Casi igual"));
        for (EvidencePart part : copy.getEvidence().getExamined()) {
            assertFalse(part.getText().contains("[") || part.getText().contains("____"), part.getText());
        }
        assertTrue(copy.getEvidence().getObservation().startsWith(
                "Where ___ (she / be)? (present simple) · casi igual a …208f del mismo tema: cambia he por she."),
                copy.getEvidence().getObservation());
    }

    @Test
    @DisplayName("should say in the evidence of a pair judged by claude -p «Para claude -p es el mismo ejercicio que …2725: cambian 2 de 7 palabras»")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R005")
    public void shouldSayInTheEvidenceOfAPairJudgedByClaudePParaClaudePEsElMismoEjercicioQue2725Cambian2De7Palabras(
            ) {
        World world = World.withoutOllama();
        world.claude.answers("Offer: I'll wash the dishes for you.", "Offer: I'll wash the dishes.", true,
                "Piden lo mismo: «'ll wash» en una oferta; sólo sobra «for you»");

        AuditNode root = world.run(course(topic("Funciones mixtas", washForYou(), wash())), budget(10));

        Finding copy = only(quiz(root, WASH));
        assertEquals("Para claude -p es el mismo ejercicio que …2725: cambian 2 de 7 palabras", part(copy, "Casi igual"));
        assertEquals("Piden lo mismo: «'ll wash» en una oferta; sólo sobra «for you»",
                part(copy, "Razón de claude -p"));
        assertEquals("Offer: I ___ (wash) the dishes.", part(copy, "Copia"));
    }

    @Test
    @DisplayName("should mark the same copies when the same examined version takes the exam twice, without a single new consultation")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R007")
    public void shouldMarkTheSameCopiesWhenTheSameExaminedVersionTakesTheExamTwiceWithoutASingleNewConsultation(
            ) {
        World world = World.notExamined();
        world.claude.answers("They cooked lunch for each other.", "They prepared lunch for each other.", true,
                "Cambia sólo el verbo, cooked por prepared: el alumno escribe «each other» en la misma oración");
        world.claude.answers("A: Can I drive your car? B: No, you can't.", "A: Could I have your car? B: No, you can't.",
                true, "Piden lo mismo: permiso para usar el auto, y el alumno escribe «you can't» igual");
        world.claude.answers("A: Could I have your car? B: No, you can't.", "A: Could I drive your car? B: No, you can't.",
                true, "Piden lo mismo: permiso para usar el auto, y el alumno escribe «you can't» igual");
        // The exam asks only the topics of its file -- here two of the three -- and publishes without approval.
        EvaluationRunPolicy exam = new EvaluationRunPolicy(100, false, null, Set.of(PREPARED, COULD_HAVE, COULD_DRIVE),
                EvaluationRunPurpose.EXAM, 1);

        AuditNode first = world.run(examCourse(), exam);
        List<String> asked = List.copyOf(world.nomic.asked());
        int consultations = world.claude.calls();
        AuditNode second = world.run(examCourse(), exam);

        assertEquals(Map.of(PREPARED, COOKED, COULD_HAVE, CAN_DRIVE, COULD_DRIVE, CAN_DRIVE), copies(first));
        assertEquals(copies(first), copies(second));
        assertEquals(5, asked.size(), "two vectors of Each other and three of Respuestas cortas");
        assertEquals(asked, world.nomic.asked(), "the second exam asks nothing new of nomic-embed-text");
        assertTrue(consultations > 0, "the first exam asked claude -p");
        assertEquals(consultations, world.claude.calls(), "nor of claude -p");
        assertNull(quiz(first, WHERE_SHE).getScores().get("near-duplicates"),
                "a topic outside the file is neither asked nor counted");
    }

    @Test
    @DisplayName("should ask claude -p only about candidates that the wildcard does not settle and whose word similarity reaches 0.6, as «I am sorry.» and «I am hot.», 0.67, and not about «That is his car.» and «That car is his.», 0.50")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R008")
    public void shouldAskClaudePOnlyAboutCandidatesThatTheWildcardDoesNotSettleAndWhoseWordSimilarityReaches06AsIAmSorryAndIAmHot067AndNotAboutThatIsHisCarAndThatCarIsHis050(
            ) {
        World world = World.withoutOllama();
        world.claude.answers("I am sorry.", "I am hot.", false, "Dos oraciones distintas: sorry y hot");

        AuditNode root = world.run(course(topic("Afirmativas con be", sorry(), hot()),
                topic("Adjetivo o pronombre", hisCar(), carHis()), topic("Contraste inicial", whereHe(), whereShe())),
                budget(10));

        assertEquals(List.of("I am sorry. || I am hot."), world.claude.pairs(),
                "0.67: asked; the car, 0.50, and the wildcard pair are not");
        assertEquals(1.0, score(root, CAR_HIS));
        assertFalse(halfway(root, CAR_HIS), "a pair that is never asked leaves nothing missing");
        assertEquals("same-but-wildcard", only(quiz(root, WHERE_SHE)).getRule());
    }

    @Test
    @DisplayName("should make a near duplicate of a candidate that claude -p calls the same exercise, «Offer: I'll wash the dishes.» after «Offer: I'll wash the dishes for you.», with the same finding as any other")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R008")
    public void shouldMakeANearDuplicateOfACandidateThatClaudePCallsTheSameExerciseOfferIllWashTheDishesAfterOfferIllWashTheDishesForYouWithTheSameFindingAsAnyOther(
            ) {
        World world = World.withoutOllama();
        world.claude.answers("Offer: I'll wash the dishes for you.", "Offer: I'll wash the dishes.", true,
                "Piden lo mismo: «'ll wash» en una oferta; sólo sobra «for you»");

        AuditNode root = world.run(course(topic("Funciones mixtas", washForYou(), wash())), budget(10));

        Finding copy = only(quiz(root, WASH));
        assertEquals("same-exercise", copy.getRule());
        assertEquals(WASH_FOR_YOU, copy.getIdentity().getMarker());
        assertEquals(FindingSeverity.LOW, copy.getSeverity());
        assertEquals(FindingResolution.PANEL, copy.getResolution());
        assertEquals(0.0, score(root, WASH));
        assertEquals(1.0, score(root, WASH_FOR_YOU));
        NearDuplicateDiagnosis diagnosis = diagnosis(root, WASH);
        assertEquals(NearDuplicateCondition.JUDGED, diagnosis.getCondition());
        assertEquals(1 - 2 / 7.0, diagnosis.getWordSimilarity(), 1e-9);
        assertEquals("Piden lo mismo: «'ll wash» en una oferta; sólo sobra «for you»", diagnosis.getJudgeReason());
    }

    @Test
    @DisplayName("should discard a candidate that claude -p calls a different exercise, «I am hot.» after «I am sorry.»")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R008")
    public void shouldDiscardACandidateThatClaudePCallsADifferentExerciseIAmHotAfterIAmSorry() {
        World world = World.withoutOllama();
        world.claude.answers("I am sorry.", "I am hot.", false, "Dos oraciones distintas: sorry y hot");

        AuditNode root = world.run(course(topic("Afirmativas con be", sorry(), hot())), budget(10));

        assertEquals(List.of(), findings(quiz(root, HOT)));
        assertEquals(1.0, score(root, HOT));
        assertFalse(halfway(root, HOT), "judged: nothing is missing");
        assertTrue(((QuizDiagnoses) quiz(root, HOT).getDiagnoses()).getNearDuplicateDiagnosis().isEmpty());
        assertEquals(1, world.claude.calls());
    }

    @Test
    @DisplayName("should run the wildcard without the local model, marking «Where is she?» after «Where is he?» without Ollama")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R008")
    public void shouldRunTheWildcardWithoutTheLocalModelMarkingWhereIsSheAfterWhereIsHeWithoutOllama(
            ) {
        World world = World.withoutOllama();

        AuditNode root = world.run(course(topic("Contraste inicial", whereHe(), whereShe())), null);

        assertEquals("same-but-wildcard", only(quiz(root, WHERE_SHE)).getRule());
        assertEquals(WHERE_HE, only(quiz(root, WHERE_SHE)).getIdentity().getMarker());
        assertFalse(halfway(root, WHERE_SHE));
        assertEquals(0, world.claude.calls(), "the wildcard asks no one");
        LocalToolRunDiagnosis run = runOf(root);
        assertTrue(run.isAlternative());
        assertTrue(run.isPublished());
        assertNull(run.getReason(), "nothing is missing");
    }

    @Test
    @DisplayName("should leave half evaluated an exercise with a candidate left unjudged for lack of budget, keeping its wildcard findings and saying how many pairs are missing")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R008")
    public void shouldLeaveHalfEvaluatedAnExerciseWithACandidateLeftUnjudgedForLackOfBudgetKeepingItsWildcardFindingsAndSayingHowManyPairsAreMissing(
            ) {
        World world = World.withoutOllama();

        // Without --budget near-duplicates=N: «Are we happy?» against the other two, 0.625 and 0.875, waits.
        AuditNode root = world.run(course(topic("Respuestas cortas", weHappy(), theyReady(), weReady())), null);

        AuditNode weReady = quiz(root, WE_READY);
        assertEquals("same-but-wildcard", only(weReady).getRule(), "its wildcard finding counts");
        assertEquals(THEY_READY, only(weReady).getIdentity().getMarker());
        assertEquals(0.0, score(root, WE_READY));
        assertTrue(halfway(root, WE_READY), "its pair with «Are we happy?» comes first and is missing");
        assertTrue(halfway(root, THEY_READY));
        assertEquals(1.0, score(root, THEY_READY));
        assertFalse(halfway(root, WE_HAPPY), "the first of the topic has nothing before it");
        assertEquals(0, world.claude.calls());
        assertEquals("falta el tope: la corrida no pide --budget near-duplicates=N, así que no se le pregunta nada "
                + "nuevo a claude -p: quedan 2 pares sin juzgar", runOf(root).getReason());
        assertEquals(2, coverageOf(root).getCost().getPendingConsultations());
    }

    @Test
    @DisplayName("should ask claude -p once per candidate pair and stop at the budget of --budget near-duplicates=N")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R008")
    public void shouldAskClaudePOncePerCandidatePairAndStopAtTheBudgetOfBudgetNearduplicatesN() {
        World world = World.withoutOllama();
        world.claude.answers("A: Are we happy? B: Yes, we are.", "A: Are they ready? B: Yes, they are.", false,
                "Otra pregunta: happy y ready");
        world.claude.answers("A: Are we happy? B: Yes, we are.", "A: Are we ready? B: Yes, we are.", false,
                "Otra pregunta: happy y ready");

        AuditNode one = world.run(course(topic("Respuestas cortas", weHappy(), theyReady(), weReady())), budget(1));

        assertEquals(List.of("A: Are we happy? B: Yes, we are. || A: Are they ready? B: Yes, they are."),
                world.claude.pairs(), "--budget near-duplicates=1: the first pair in the order of the topic");
        assertFalse(halfway(one, THEY_READY));
        assertTrue(halfway(one, WE_READY));
        assertEquals("se acabó el tope de near-duplicates: quedan 1 par sin juzgar", runOf(one).getReason());

        AuditNode all = world.run(course(topic("Respuestas cortas", weHappy(), theyReady(), weReady())), budget(10));

        assertEquals(2, world.claude.calls(), "one consultation per candidate pair: the first is reused, the wildcard "
                + "pair is never asked");
        assertFalse(halfway(all, WE_READY));
        assertEquals(THEY_READY, only(quiz(all, WE_READY)).getIdentity().getMarker());
    }

    // -------------------------------------------------------------------------
    // The exercises of the course of 29/9
    // -------------------------------------------------------------------------

    static final String WHERE_HE = "67fab6d5993010229534208f";

    static final String WHERE_SHE = "67fab6d59930102295342097";

    static final String YOU_NOW = "67fab6d59930102295341ea1";

    static final String THEY_NOW = "67fab6d59930102295341eac";

    static final String CLEAN = "67fab6d599301022953441cb";

    static final String CLEANING = "67fab6d599301022953441cc";

    static final String DIDNT_HAVE = "67fab6d59930102295342b6b";

    static final String DONT_HAVE = "67fab6d59930102295342b7f";

    static final String COOKED = "67fab6d599301022953446ac";

    static final String PREPARED = "67fab6d599301022953446b0";

    static final String HEARD_NEWS = "67fab6d59930102295343083";

    static final String HEARD_MY_NEWS = "67fab6d599301022953430a5";

    static final String CAN_DRIVE = "67fab6d5993010229534251c";

    static final String COULD_HAVE = "67fab6d5993010229534251d";

    static final String COULD_DRIVE = "67fab6d5993010229534251e";

    static final String YOU_PARK = "67fab6d59930102295342231";

    static final String THEY_PARK = "67fab6d5993010229534224b";

    static final String THEY_PARK_AGAIN = "67fab6d59930102295342260";

    static final String SORRY = "67fab6d59930102295341e61";

    static final String HOT = "67fab6d59930102295341e64";

    static final String WE_HAPPY = "67fab6d59930102295341f5b";

    static final String THEY_READY = "67fab6d59930102295341f5e";

    static final String WE_READY = "67fab6d599301022953425e8";

    static final String HIS_CAR = "67fab6d599301022953423f9";

    static final String CAR_HIS = "67fab6d599301022953423fe";

    static final String WASH_FOR_YOU = "67fab6d59930102295342725";

    static final String WASH = "67fab6d59930102295342fd5";

    static final String BEEN_TO_ROME = "67fab6d5993010229534355e";

    static final String VISITED_ROME = "67fab6d59930102295344242";

    /** …208f, Contraste inicial (A1). */
    static AuditableQuiz whereHe() {
        return cloze(WHERE_HE, "Contraste inicial", text("Where"), gap("is he"), text("(he / be)? (present simple)"));
    }

    /** …2097, Contraste inicial (A1). */
    static AuditableQuiz whereShe() {
        return cloze(WHERE_SHE, "Contraste inicial", text("Where"), gap("is she"), text("(she / be)? (present simple)"));
    }

    /** …1ea1, Preguntas wh con be (A1). */
    static AuditableQuiz youNow() {
        return cloze(YOU_NOW, "Preguntas wh con be", text("Where"), gap("are you"), text("now? (You)"));
    }

    /** …1eac, Preguntas wh con be (A1). */
    static AuditableQuiz theyNow() {
        return cloze(THEY_NOW, "Preguntas wh con be", text("Where"), gap("are they"), text("now? (They)"));
    }

    /** …41cb, Acción completa o parcial (B2). */
    static AuditableQuiz clean() {
        return cloze(CLEAN, "Acción completa o parcial", text("I observed him"), gap("clean"),
                text("(clean) the room. (The whole action.)"));
    }

    /** …41cc, Acción completa o parcial (B2). */
    static AuditableQuiz cleaning() {
        return cloze(CLEANING, "Acción completa o parcial", text("I observed him"), gap("cleaning"),
                text("(clean) the room. (Part of the action.)"));
    }

    /** …2b6b, Cuantificadores: con o sin of (A2). */
    static AuditableQuiz didntHave() {
        return mc(DIDNT_HAVE, "Cuantificadores: con o sin of", List.of(text("I didn't have"), gap(), text("time.")),
                item("much", 1.0), item("much of", 0.0));
    }

    /** …2b7f, Cuantificadores: con o sin of (A2). */
    static AuditableQuiz dontHave() {
        return mc(DONT_HAVE, "Cuantificadores: con o sin of", List.of(text("I don't have"), gap(), text("time.")),
                item("much", 1.0), item("much of", 0.0));
    }

    /** …46ac, Each other o reflexivos (B1). */
    static AuditableQuiz cooked() {
        return mc(COOKED, "Each other o reflexivos", List.of(text("They cooked lunch for"), gap(),
                text(". (Amanda cooked lunch for Luke and Luke cooked lunch for Amanda.)")), reflexives());
    }

    /** …46b0, Each other o reflexivos (B1). */
    static AuditableQuiz prepared() {
        return mc(PREPARED, "Each other o reflexivos", List.of(text("They prepared lunch for"), gap(),
                text(". (Julie prepared lunch for David and David prepared lunch for Julie.)")), reflexives());
    }

    /** …3083, Participios irregulares: repaso 1 (B1). */
    static AuditableQuiz heardNews() {
        return cloze(HEARD_NEWS, "Participios irregulares: repaso 1", text(""), gap("Have you heard"),
                text("(you / hear) the news?"));
    }

    /** …30a5, Participios irregulares: repaso 1 (B1). */
    static AuditableQuiz heardMyNews() {
        return cloze(HEARD_MY_NEWS, "Participios irregulares: repaso 1", text(""), gap("Have you heard"),
                text("(you / hear) about my news?"));
    }

    /** …251c, Respuestas cortas (A1), with can and could. */
    static AuditableQuiz canDrive() {
        return mc(CAN_DRIVE, "Respuestas cortas", List.of(text("A: Can I drive your car? B: No,"), gap(), text(".")),
                shortAnswers());
    }

    /** …251d, Respuestas cortas (A1), with can and could. */
    static AuditableQuiz couldHave() {
        return mc(COULD_HAVE, "Respuestas cortas", List.of(text("A: Could I have your car? B: No,"), gap(), text(".")),
                shortAnswers());
    }

    /** …251e, Respuestas cortas (A1), with can and could. */
    static AuditableQuiz couldDrive() {
        return mc(COULD_DRIVE, "Respuestas cortas", List.of(text("A: Could I drive your car? B: No,"), gap(),
                text(".")), shortAnswers());
    }

    /** …2231, Práctica mixta (A1). */
    static AuditableQuiz youPark() {
        return cloze(YOU_PARK, "Práctica mixta", text("You"), gap("weren't"), text("(not / be) at the park."));
    }

    /** …224b and …2260, Práctica mixta (A1): the same exercise twice. */
    static AuditableQuiz theyPark(String id) {
        return cloze(id, "Práctica mixta", text("They"), gap("weren't"), text("(not / be) at the park."));
    }

    /** …1e61, Afirmativas con be (A1). */
    static AuditableQuiz sorry() {
        return mc(SORRY, "Afirmativas con be", List.of(text("I"), gap(), text("sorry.")), item("am", 1.0),
                item("are", 0.0), item("is", 0.0));
    }

    /** …1e64, Afirmativas con be (A1). */
    static AuditableQuiz hot() {
        return mc(HOT, "Afirmativas con be", List.of(text("I"), gap(), text("hot.")), item("am", 1.0),
                item("are", 0.0), item("is", 0.0));
    }

    /** …1f5b, Respuestas cortas (A1), present simple. */
    static AuditableQuiz weHappy() {
        return cloze(WE_HAPPY, "Respuestas cortas", text("A: Are we happy? B: Yes, we"), gap("are"), text("."));
    }

    /** …1f5e, Respuestas cortas (A1), present simple. */
    static AuditableQuiz theyReady() {
        return cloze(THEY_READY, "Respuestas cortas", text("A: Are they ready? B: Yes, they"), gap("are"), text("."));
    }

    /** …25e8, Respuestas cortas (A1), present simple. */
    static AuditableQuiz weReady() {
        return cloze(WE_READY, "Respuestas cortas", text("A: Are we ready? B: Yes, we"), gap("are"), text("."));
    }

    /** …23f9, Adjetivo o pronombre (A1). */
    static AuditableQuiz hisCar() {
        return cloze(HIS_CAR, "Adjetivo o pronombre", text("That is (his / his)"), gap("his"), text("car."));
    }

    /** …23fe, Adjetivo o pronombre (A1). */
    static AuditableQuiz carHis() {
        return cloze(CAR_HIS, "Adjetivo o pronombre", text("That car is (his / his)"), gap("his"), text("."));
    }

    /** …2725, Funciones mixtas (A2). */
    static AuditableQuiz washForYou() {
        return cloze(WASH_FOR_YOU, "Funciones mixtas", text("Offer: I"), gap("'ll wash|will wash"),
                text("(wash) the dishes for you."));
    }

    /** …2fd5, Funciones mixtas (A2). */
    static AuditableQuiz wash() {
        return cloze(WASH, "Funciones mixtas", text("Offer: I"), gap("'ll wash|will wash"), text("(wash) the dishes."));
    }

    /** …355e, Montañas y océanos (B1): «-», no article. */
    static AuditableQuiz beenToRome() {
        return mc(BEEN_TO_ROME, "Montañas y océanos", List.of(text("Have you ever been to"), gap(), text("Rome?")),
                item("-", 1.0), item("the", 0.0));
    }

    /** …4242, Montañas y océanos (B1): «-», no article. */
    static AuditableQuiz visitedRome() {
        return mc(VISITED_ROME, "Montañas y océanos", List.of(text("Have you ever visited"), gap(), text("Rome?")),
                item("-", 1.0), item("the", 0.0));
    }

    private static MultipleChoiceItemEntity[] reflexives() {
        return new MultipleChoiceItemEntity[] {item("each other", 1.0), item("ourselves", 0.0),
                item("themselves", 0.0), item("yourselves", 0.0)};
    }

    private static MultipleChoiceItemEntity[] shortAnswers() {
        return new MultipleChoiceItemEntity[] {item("he can", 0.0), item("he can't", 0.0), item("i can", 0.0),
                item("i can't", 0.0), item("you can", 0.0), item("you can't", 1.0)};
    }

    static SentencePartEntity text(String text) {
        return new SentencePartEntity(SentencePartKind.TEXT, text, null);
    }

    static SentencePartEntity gap(String... options) {
        return new SentencePartEntity(SentencePartKind.CLOZE, "", options.length == 0 ? null : List.of(options));
    }

    static MultipleChoiceItemEntity item(String label, double incidence) {
        return new MultipleChoiceItemEntity(label, incidence, label);
    }

    static AuditableQuiz cloze(String id, String title, SentencePartEntity... parts) {
        return new AuditableQuiz(List.of(), id, title, "", null, List.of(), null, "", List.of(parts), null, null, null);
    }

    static AuditableQuiz mc(String id, String title, List<SentencePartEntity> parts, MultipleChoiceItemEntity... items) {
        return new AuditableQuiz(List.of(), id, title, "", null, List.of(), null, "", parts,
                new MultipleChoiceEntity("SINGLE", List.of(items)), null, null);
    }

    /** A topic of A1 with these exercises, in this order; its id is its first exercise's. */
    static AuditableKnowledge topic(String title, AuditableQuiz... quizzes) {
        return new AuditableKnowledge(List.of(quizzes), title, "Completa.", true, "k-" + quizzes[0].getId(), title, "K",
                SentenceMode.FILL, "Área");
    }

    static AuditableCourse course(AuditableKnowledge... topics) {
        return new AuditableCourse(List.of(new AuditableMilestone(List.of(new AuditableTopic(List.of(topics), "t-A1",
                "Área", "T")), "m-A1", "A1", "A1")));
    }

    /** Three topics; the exam of the tests declares exercises of the last two. */
    static AuditableCourse examCourse() {
        return course(topic("Contraste inicial", whereHe(), whereShe()),
                topic("Each other o reflexivos", cooked(), prepared()),
                topic("Respuestas cortas", canDrive(), couldHave(), couldDrive()));
    }

    /** --budget near-duplicates=N. */
    static EvaluationRunPolicy budget(int consultations) {
        return new EvaluationRunPolicy(consultations, false, null, null, null, null);
    }

    /** The corrector in production since 1/10/2026, with its table. */
    static CorrectorMatcher corrector() {
        return new DefaultCorrectorMatcher(DefaultCorrectorMatcherTest.productionTable());
    }

    static QuizStudentViewRenderer renderer() {
        return new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create());
    }

    // -------------------------------------------------------------------------
    // The world: nomic-embed-text and claude -p recorded, the judges' runtime over a ledger in memory
    // -------------------------------------------------------------------------

    /** The examined version of the vectors alone, nomic-embed-text with «clustering: »: the ledger stores them under it. */
    static final String NOMIC_EXAMINED = "nomic-embed-text@0a109f422b47 · clustering-1";

    /** The version the pair judge states: its definition, model and provider. */
    static final String CLAUDE_VERSION = "c3a9e1f07b5d42c8a6e0f9d8c7b6a5948372615043f2e1d0c9b8a7f6e5d4c3b2";

    /** The version with Ollama, in its three pieces: the vectors, the candidate similarity and 12 hex of the judge's. */
    static final String WITH_OLLAMA_EXAMINED =
            "nomic-embed-text@0a109f422b47 · clustering-1 · candidatos-0,80 · juez-c3a9e1f07b5d";

    /** The version without Ollama: the word similarity and the first 12 hex of the judge's. */
    static final String WITHOUT_OLLAMA_EXAMINED = "palabras-0,60 · juez-c3a9e1f07b5d";

    /** The evaluator of the vectors in the ledger: «near-duplicates · » and the version of the vectors alone. */
    static final String VECTORS_ID = "near-duplicates · " + NOMIC_EXAMINED;

    /** The evaluator of the pair judge: the same with Ollama and without it. */
    static final String JUDGE_ID = "near-duplicates-same-exercise";

    static final class World {
        final Ledger ledger;
        final Exams exams;
        final Timeline timeline;
        final RecordedNomic nomic;
        final RecordedClaude claude;
        final DefaultJudgeRuntime runtime;
        final RecordingRuntime recording;
        final DefaultLocalToolGate gate;
        final NearDuplicatesAnalyzerProvider provider;

        private World(boolean ollama, boolean judgeStatesVersion) {
            this(ollama, new Ledger(), new Exams(), new Timeline(), new RecordedClaude(judgeStatesVersion));
        }

        private World(boolean ollama, Ledger ledger, Exams exams, Timeline timeline, RecordedClaude claude) {
            this.ledger = ledger;
            this.exams = exams;
            this.timeline = timeline;
            this.nomic = new RecordedNomic(ollama);
            this.claude = claude;
            Sha256ContentFingerprinter fingerprinter = new Sha256ContentFingerprinter();
            this.runtime = new DefaultJudgeRuntime(new DefaultEvaluationSessionFactory(ledger, fingerprinter),
                    new ExamEvaluationSessionFactory(ledger, fingerprinter), exams, timeline, new Judges());
            this.recording = new RecordingRuntime(runtime);
            this.gate = new DefaultLocalToolGate(runtime);
            this.provider = new NearDuplicatesAnalyzerProvider(nomic, claude, recording, corrector(), renderer(), gate);
        }

        /** nomic-embed-text in Ollama, whose version with the judge, in its three pieces, approved its exam. */
        static World withNomic() {
            World world = new World(true, true);
            world.exams.approve(WITH_OLLAMA_EXAMINED);
            return world;
        }

        /** nomic-embed-text in Ollama, without exam. */
        static World notExamined() {
            return new World(true, true);
        }

        /** nomic-embed-text in Ollama and a pair judge that states no version: its definition or configuration is missing. */
        static World withNomicAndAJudgeWithoutVersion() {
            return new World(true, false);
        }

        /** Ollama off: the alternative with claude -p, whose version, word similarity and judge, approved its exam. */
        static World withoutOllama() {
            World world = new World(false, true);
            world.exams.approve(WITHOUT_OLLAMA_EXAMINED);
            return world;
        }

        /** The same ledger, exams and claude -p with Ollama on or off: what an earlier run left is what this one finds. */
        World turned(boolean ollama) {
            return new World(ollama, ledger, exams, timeline, claude);
        }

        /** analyze asking only near-duplicates, with this policy (null: the run asks it nothing). */
        AuditNode run(AuditableCourse course, EvaluationRunPolicy policy) {
            Map<String, EvaluationRunPolicy> policies = policy != null ? Map.of("near-duplicates", policy) : Map.of();
            IAuditEngine engine = new IAuditEngine(new IScoreAggregator(), new DefaultAnalyzerCatalog(List.of(provider)),
                    new DefaultFindingCollector(), new DefaultContextNumbersCalculator());
            return engine.runAudit(course, new AnalyzerRunSelection(List.of("near-duplicates"), policies)).getRoot();
        }
    }

    /**
     * nomic-embed-text with the vectors of the finished exercises of the tests: for each group of a
     * topic, vectors whose cosines are the ones the prototype recorded (dup/pares.json); any other
     * exercise gets one at a right angle to all. It writes down what it is asked.
     */
    static final class RecordedNomic implements EmbeddingModel {
        static final int DIMENSIONS = 64;
        private final boolean available;
        private final Map<String, double[]> vectors = new LinkedHashMap<>();
        private int nextDimension;
        private final List<String> asked = new ArrayList<>();

        RecordedNomic(boolean available) {
            this.available = available;
            recorded(List.of("They cooked lunch for each other.", "They prepared lunch for each other."), 0.9759);
            recorded(List.of("Have you heard the news?", "Have you heard about my news?"), 0.9431);
            recorded(List.of("A: Can I drive your car? B: No, you can't.", "A: Could I have your car? B: No, you can't.",
                    "A: Could I drive your car? B: No, you can't."), 0.9565, 0.9972, 0.9583);
            recorded(List.of("A: Are we happy? B: Yes, we are.", "A: Are they ready? B: Yes, they are.",
                    "A: Are we ready? B: Yes, we are."), 0.8216, 0.8666, 0.965);
            recorded(List.of("I am sorry.", "I am hot."), 0.7103);
            recorded(List.of("That is his car.", "That car is his."), 0.9711);
            recorded(List.of("Offer: I'll wash the dishes for you.", "Offer: I'll wash the dishes."), 0.9937);
            recorded(List.of("Have you ever been to Rome?", "Have you ever visited Rome?"), 0.9898);
        }

        /** Vectors for these texts with these cosines, row by row of the upper triangle (Cholesky). */
        void recorded(List<String> texts, double... cosines) {
            int n = texts.size();
            double[][] gram = new double[n][n];
            int k = 0;
            for (int i = 0; i < n; i++) {
                gram[i][i] = 1.0;
                for (int j = i + 1; j < n; j++) {
                    gram[i][j] = cosines[k];
                    gram[j][i] = cosines[k];
                    k++;
                }
            }
            double[][] lower = new double[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j <= i; j++) {
                    double sum = gram[i][j];
                    for (int m = 0; m < j; m++) {
                        sum -= lower[i][m] * lower[j][m];
                    }
                    lower[i][j] = i == j ? Math.sqrt(sum) : sum / lower[j][j];
                }
            }
            for (int i = 0; i < n; i++) {
                double[] vector = new double[DIMENSIONS];
                for (int j = 0; j <= i; j++) {
                    vector[nextDimension + j] = lower[i][j];
                }
                vectors.put(texts.get(i), vector);
            }
            nextDimension += n;
        }

        @Override
        public LocalToolStatus status() {
            return available
                    ? new LocalToolStatus("nomic-embed-text en Ollama", "nomic-embed-text@0a109f422b47", true, null,
                            "ollama pull nomic-embed-text")
                    : new LocalToolStatus("nomic-embed-text en Ollama", null, false,
                            "CONTENT_AUDIT_OLLAMA_URL=off: los modelos locales están apagados a pedido",
                            "ollama pull nomic-embed-text");
        }

        @Override
        public synchronized SentenceEmbedding embed(String text) {
            if (!available) {
                throw new AssertionError("Ollama is off: nothing is asked");
            }
            asked.add(text);
            String exercise = text.startsWith("clustering: ") ? text.substring("clustering: ".length()) : text;
            double[] vector = vectors.get(exercise);
            if (vector == null) {
                vector = new double[DIMENSIONS];
                vector[nextDimension++] = 1.0;
                vectors.put(exercise, vector);
            }
            List<Double> values = new ArrayList<>();
            for (double value : vector) {
                values.add(value);
            }
            return new SentenceEmbedding(values);
        }

        synchronized List<String> asked() {
            return new ArrayList<>(asked);
        }
    }

    /**
     * The pair judge with a recorded verdict per pair; it counts its consultations and answers nothing that serves
     * to a pair it has no verdict for. It may state no version.
     */
    static final class RecordedClaude implements Evaluator {
        private final boolean statesVersion;
        private final Map<String, String> verdicts = new LinkedHashMap<>();
        private final List<String> pairs = new ArrayList<>();
        final List<EvaluationSubject> subjects = new ArrayList<>();

        RecordedClaude() {
            this(true);
        }

        RecordedClaude(boolean statesVersion) {
            this.statesVersion = statesVersion;
        }

        void answers(String kept, String copy, boolean sameExercise, String reason) {
            verdicts.put(kept + " || " + copy, "{\"sameExercise\": " + sameExercise + ", \"reason\": \""
                    + reason.replace("\"", "\\\"") + "\"}");
        }

        @Override
        public String evaluatorId() {
            return JUDGE_ID;
        }

        @Override
        public synchronized EvaluationOutcome evaluate(EvaluationSubject subject) {
            subjects.add(subject);
            String pair = solved(subject.getContent().get("first")) + " || " + solved(subject.getContent().get("second"));
            pairs.add(pair);
            String verdict = verdicts.get(pair);
            return verdict != null
                    ? new EvaluationEmitted(verdict)
                    : new EvaluationNotEmitted(EvaluationFailureKind.OUTPUT_UNUSABLE, "sin veredicto grabado");
        }

        @Override
        public Optional<String> evaluatorVersion() {
            return statesVersion ? Optional.of(CLAUDE_VERSION) : Optional.empty();
        }

        synchronized int calls() {
            return subjects.size();
        }

        synchronized List<String> pairs() {
            return new ArrayList<>(pairs);
        }

        /** The exercise solved, the second line of what claude -p reads. */
        static String solved(String exercise) {
            int at = exercise != null ? exercise.indexOf("\nResuelto: ") : -1;
            return at >= 0 ? exercise.substring(at + "\nResuelto: ".length()) : exercise;
        }
    }

    /** The judges' runtime, writing down each open: what the provider decided for the run. */
    static final class RecordingRuntime implements JudgeRuntime {
        final JudgeRuntime delegate;
        final List<String> judges = new ArrayList<>();
        final List<List<String>> evaluators = new ArrayList<>();
        final List<EvaluationRunPolicy> policies = new ArrayList<>();
        final List<JudgeSessions> opened = new ArrayList<>();

        RecordingRuntime(JudgeRuntime delegate) {
            this.delegate = delegate;
        }

        @Override
        public JudgeSessions open(String judgeName, List<Evaluator> list, EvaluationRunPolicy policy) {
            judges.add(judgeName);
            evaluators.add(list.stream().map(Evaluator::evaluatorId).toList());
            policies.add(policy);
            JudgeSessions sessions = delegate.open(judgeName, list, policy);
            opened.add(sessions);
            return sessions;
        }

        @Override
        public JudgeRequestPlan plan(List<AuditNode> units, EvaluationRunPolicy policy) {
            return delegate.plan(units, policy);
        }

        @Override
        public JudgeCoverageDiagnosis coverage(JudgeSessions sessions, JudgeUnitTally tally) {
            return delegate.coverage(sessions, tally);
        }

        @Override
        public JudgeExamStanding standing(String judgeName, String judgeVersion) {
            return delegate.standing(judgeName, judgeVersion);
        }
    }

    /** The times at which each evaluator recorded its verdicts: what the runtime measures its rate with. */
    static final class Timeline implements EvaluationTimeline {
        final Map<String, List<Instant>> times = new LinkedHashMap<>();

        @Override
        public List<Instant> recordedTimes(String evaluatorId) {
            return times.getOrDefault(evaluatorId, List.of());
        }
    }

    /** The ledger in memory: the latest with a key wins. */
    static final class Ledger implements EvaluationLedger {
        private final List<EvaluationRecord> records = new ArrayList<>();

        @Override
        public synchronized void append(EvaluationRecord record) {
            records.add(record);
        }

        @Override
        public synchronized Optional<EvaluationRecord> findLatest(EvaluationKey key) {
            EvaluationRecord latest = null;
            for (EvaluationRecord record : records) {
                if (record.getKey().equals(key)) {
                    latest = record;
                }
            }
            return Optional.ofNullable(latest);
        }

        @Override
        public synchronized List<EvaluationRecord> history(EvaluationKey key) {
            return records.stream().filter(r -> r.getKey().equals(key)).toList();
        }

        synchronized List<EvaluationRecord> all() {
            return new ArrayList<>(records);
        }
    }

    /** The judges' registry in memory: approved versions of near-duplicates. */
    static final class Exams implements JudgeExamRegistry {
        private final List<JudgeExamRecord> records = new ArrayList<>();

        void approve(String version) {
            records.add(new JudgeExamRecord("near-duplicates", version, JudgeExamStatus.APPROVED, 6, 0, List.of(),
                    List.of(), true, "2026-09-29", "medir.py", Instant.parse("2026-10-01T12:00:00Z")));
        }

        @Override
        public void record(JudgeExamRecord record) {
            records.add(record);
        }

        @Override
        public Optional<JudgeExamRecord> latest(String judgeName, String judgeVersion) {
            JudgeExamRecord latest = null;
            for (JudgeExamRecord record : records) {
                if (record.getJudgeName().equals(judgeName) && record.getJudgeVersion().equals(judgeVersion)) {
                    latest = record;
                }
            }
            return Optional.ofNullable(latest);
        }

        @Override
        public List<JudgeExamRecord> history(String judgeName) {
            return records.stream().filter(r -> r.getJudgeName().equals(judgeName)).toList();
        }
    }

    static final class Judges implements JudgeRuntimeConfig {
        @Override
        public int getMaxConcurrentEvaluations() {
            return 4;
        }

        @Override
        public int getDefaultMaxNewEvaluations(String judgeName) {
            return 0;
        }

        @Override
        public double getDefaultRatePerHour(String judgeName) {
            return 189.0;
        }

        @Override
        public Map<String, Object> describe() {
            return Map.of();
        }
    }

    // -------------------------------------------------------------------------
    // Reading the report
    // -------------------------------------------------------------------------

    static AuditNode quiz(AuditNode root, String id) {
        List<AuditNode> found = new ArrayList<>();
        walk(root, node -> {
            if (node.getTarget() == AuditTarget.QUIZ && id.equals(node.getEntity().getId())) {
                found.add(node);
            }
        });
        if (found.isEmpty()) {
            throw new AssertionError("no exercise " + id);
        }
        return found.get(0);
    }

    static void walk(AuditNode node, Consumer<AuditNode> visit) {
        visit.accept(node);
        if (node.getChildren() != null) {
            for (AuditNode child : node.getChildren()) {
                walk(child, visit);
            }
        }
    }

    static Double score(AuditNode root, String id) {
        return quiz(root, id).getScores().get("near-duplicates");
    }

    static boolean halfway(AuditNode root, String id) {
        List<String> partially = quiz(root, id).getPartiallyEvaluatedBy();
        return partially != null && partially.contains("near-duplicates");
    }

    static List<Finding> findings(AuditNode node) {
        List<Finding> findings = new ArrayList<>();
        if (node.getFindings() != null) {
            for (Finding finding : node.getFindings()) {
                if ("near-duplicates".equals(finding.getAnalyzer())) {
                    findings.add(finding);
                }
            }
        }
        return findings;
    }

    static Finding only(AuditNode node) {
        List<Finding> findings = findings(node);
        assertEquals(1, findings.size(), findings.toString());
        return findings.get(0);
    }

    /** The rule of every finding of near-duplicates in the report, in the order of the course. */
    static List<String> rules(AuditNode root) {
        List<String> rules = new ArrayList<>();
        walk(root, node -> {
            for (Finding finding : findings(node)) {
                rules.add(finding.getRule());
            }
        });
        return rules;
    }

    /** What was asked of a recorded evaluator, in a fixed order: the consultations of a run may be concurrent. */
    static List<String> sorted(List<String> asked) {
        return asked.stream().sorted().toList();
    }

    /** Each copy of the run with the exercise that stays, by their ids. */
    static Map<String, String> copies(AuditNode root) {
        Map<String, String> copies = new TreeMap<>();
        walk(root, node -> {
            if (node.getTarget() == AuditTarget.QUIZ) {
                for (Finding finding : findings(node)) {
                    copies.put(node.getEntity().getId(), finding.getIdentity().getMarker());
                }
            }
        });
        return copies;
    }

    static String part(Finding finding, String label) {
        for (EvidencePart part : finding.getEvidence().getExamined()) {
            if (part.getLabel().equals(label)) {
                return part.getText();
            }
        }
        return null;
    }

    static NearDuplicateDiagnosis diagnosis(AuditNode root, String id) {
        return ((QuizDiagnoses) quiz(root, id).getDiagnoses()).getNearDuplicateDiagnosis()
                .orElseThrow(() -> new AssertionError("no diagnosis of near-duplicates on " + id));
    }

    static LocalToolRunDiagnosis runOf(AuditNode root) {
        return ((CourseDiagnoses) root.getDiagnoses()).getLocalToolRuns().stream()
                .filter(run -> "near-duplicates".equals(run.getAnalyzer()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no row of the run of near-duplicates"));
    }

    static JudgeCoverageDiagnosis coverageOf(AuditNode root) {
        return ((CourseDiagnoses) root.getDiagnoses()).getJudgeCoverages().stream()
                .filter(coverage -> "near-duplicates".equals(coverage.getJudgeName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no coverage of near-duplicates"));
    }

    static AnalyzerErrorCounts errorCountsOf(AuditNode node) {
        for (AnalyzerErrorCounts counts : node.getNumbers().getErrors().getAnalyzers()) {
            if ("near-duplicates".equals(counts.getAnalyzer())) {
                return counts;
            }
        }
        throw new AssertionError("no error counts of near-duplicates");
    }

    @Test
    @DisplayName("should make a near duplicate of a candidate of its vectors that claude -p calls the same exercise, «They prepared lunch for each other.» after «They cooked lunch for each other.», 0.98, with the rule same-exercise")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R002")
    public void shouldMakeANearDuplicateOfACandidateOfItsVectorsThatClaudePCallsTheSameExerciseTheyPreparedLunchForEachOtherAfterTheyCookedLunchForEachOther098WithTheRuleSameexercise(
            ) {
        World world = World.withNomic();
        world.claude.answers("They cooked lunch for each other.", "They prepared lunch for each other.", true,
                "Cambia sólo el verbo, cooked por prepared: el alumno escribe «each other» en la misma oración");

        AuditNode root = world.run(course(topic("Each other o reflexivos", cooked(), prepared())), budget(10));

        Finding copy = only(quiz(root, PREPARED));
        assertEquals("same-exercise", copy.getRule(), "the wildcard does not settle them: cooked and prepared");
        assertEquals(COOKED, copy.getIdentity().getMarker());
        NearDuplicateDiagnosis diagnosis = diagnosis(root, PREPARED);
        assertEquals(NearDuplicateCondition.JUDGED, diagnosis.getCondition());
        assertEquals(0.9759, diagnosis.getSimilarity(), 1e-5, "the cosine of the vectors that proposed the pair");
        assertEquals("Cambia sólo el verbo, cooked por prepared: el alumno escribe «each other» en la misma oración",
                diagnosis.getJudgeReason());
        assertEquals(List.of(), findings(quiz(root, COOKED)), "the first of the topic stays");
        assertEquals(1.0, score(root, COOKED));
        assertEquals(0.0, score(root, PREPARED));
        assertEquals(List.of("They cooked lunch for each other. || They prepared lunch for each other."),
                world.claude.pairs());
        assertEquals(List.of("clustering: They cooked lunch for each other.",
                "clustering: They prepared lunch for each other."), sorted(world.nomic.asked()));
    }

    @Test
    @DisplayName("should mark the same copies in two runs of the same course, reusing every vector and every verdict of claude -p without asking either again")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R003")
    public void shouldMarkTheSameCopiesInTwoRunsOfTheSameCourseReusingEveryVectorAndEveryVerdictOfClaudePWithoutAskingEitherAgain(
            ) {
        World world = World.withNomic();
        world.claude.answers("A: Can I drive your car? B: No, you can't.", "A: Could I have your car? B: No, you can't.",
                true, "Piden lo mismo: permiso para usar el auto, y el alumno escribe «you can't» igual");
        world.claude.answers("A: Could I have your car? B: No, you can't.", "A: Could I drive your car? B: No, you can't.",
                true, "Piden lo mismo: permiso para usar el auto, y el alumno escribe «you can't» igual");

        AuditNode first = world.run(course(topic("Respuestas cortas", canDrive(), couldHave(), couldDrive())),
                budget(10));
        List<String> vectors = List.copyOf(world.nomic.asked());
        int consultations = world.claude.calls();
        AuditNode second = world.run(course(topic("Respuestas cortas", canDrive(), couldHave(), couldDrive())),
                budget(10));

        assertEquals(Map.of(COULD_HAVE, CAN_DRIVE, COULD_DRIVE, CAN_DRIVE), copies(first));
        assertEquals(copies(first), copies(second));
        assertEquals(3, vectors.size());
        assertEquals(vectors, world.nomic.asked(), "the second run reuses every vector");
        assertTrue(consultations > 0, "the first run asked claude -p");
        assertEquals(consultations, world.claude.calls(), "and every verdict of claude -p");
    }

    @Test
    @DisplayName("should give each finding the cost of the rule that found it, instant for same-but-wildcard and paid model for same-exercise with Ollama and without it, and none to similar-candidates, which only proposes pairs")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R004")
    public void shouldGiveEachFindingTheCostOfTheRuleThatFoundItInstantForSamebutwildcardAndPaidModelForSameexerciseWithOllamaAndWithoutItAndNoneToSimilarcandidatesWhichOnlyProposesPairs(
            ) {
        World withOllama = World.withNomic();
        withOllama.claude.answers("They cooked lunch for each other.", "They prepared lunch for each other.", true,
                "Cambia sólo el verbo, cooked por prepared: el alumno escribe «each other» en la misma oración");
        withOllama.claude.answers("That is his car.", "That car is his.", false,
                "Contrastan adjetivo y pronombre: el alumno escribe «his» con otro papel");
        AuditNode proposed = withOllama.run(course(topic("Contraste inicial", whereHe(), whereShe()),
                topic("Each other o reflexivos", cooked(), prepared()), topic("Adjetivo o pronombre", hisCar(), carHis())),
                budget(10));
        World alternative = World.withoutOllama();
        alternative.claude.answers("Offer: I'll wash the dishes for you.", "Offer: I'll wash the dishes.", true,
                "Piden lo mismo: «'ll wash» en una oferta; sólo sobra «for you»");
        AuditNode withoutOllama = alternative.run(course(topic("Funciones mixtas", washForYou(), wash())), budget(10));

        assertEquals(AnalysisCost.INSTANT, only(quiz(proposed, WHERE_SHE)).getCost());
        assertEquals(AnalysisCost.PAID_MODEL, only(quiz(proposed, PREPARED)).getCost(), "with Ollama too");
        assertEquals(AnalysisCost.PAID_MODEL, only(quiz(withoutOllama, WASH)).getCost(), "and without it");
        assertEquals(List.of("same-but-wildcard", "same-exercise"), rules(proposed),
                "«That car is his.», 0.97, is proposed by its vectors and gives no finding: that rule only proposes");
        assertEquals(List.of("same-exercise"), rules(withoutOllama));
    }

    @Test
    @DisplayName("should give a pair that the wildcard settles the instant rule same-but-wildcard without asking claude -p, even when its vectors reach 0.80, «A: Are we ready? B: Yes, we are.» after «A: Are they ready? B: Yes, they are.»")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R004")
    public void shouldGiveAPairThatTheWildcardSettlesTheInstantRuleSamebutwildcardWithoutAskingClaudePEvenWhenItsVectorsReach080AAreWeReadyBYesWeAreAfterAAreTheyReadyBYesTheyAre(
            ) {
        World world = World.withNomic();
        world.claude.answers("A: Are we happy? B: Yes, we are.", "A: Are they ready? B: Yes, they are.", false,
                "Otra pregunta: happy y ready");
        world.claude.answers("A: Are we happy? B: Yes, we are.", "A: Are we ready? B: Yes, we are.", false,
                "Otra pregunta: happy y ready");

        // «Are they ready?» and «Are we ready?» reach 0.965 with their vectors, but the wildcard settles them first.
        AuditNode root = world.run(course(topic("Respuestas cortas", weHappy(), theyReady(), weReady())), budget(10));

        Finding copy = only(quiz(root, WE_READY));
        assertEquals("same-but-wildcard", copy.getRule());
        assertEquals(AnalysisCost.INSTANT, copy.getCost());
        assertEquals(THEY_READY, copy.getIdentity().getMarker());
        NearDuplicateDiagnosis diagnosis = diagnosis(root, WE_READY);
        assertEquals(NearDuplicateCondition.WILDCARD, diagnosis.getCondition());
        assertEquals("they → we", diagnosis.getDifference());
        assertEquals(List.of("A: Are we happy? B: Yes, we are. || A: Are they ready? B: Yes, they are.",
                "A: Are we happy? B: Yes, we are. || A: Are we ready? B: Yes, we are."), sorted(world.claude.pairs()),
                "the two candidates of «Are we happy?»: the wildcard pair is never asked");
        assertEquals(List.of(), findings(quiz(root, THEY_READY)), "claude -p says it is another question");
    }

    @Test
    @DisplayName("should say in the evidence of a pair that its vectors proposed and claude -p judged «Para claude -p es el mismo ejercicio que …355e: parecido 0,99» and the reason claude -p gave")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R005")
    public void shouldSayInTheEvidenceOfAPairThatItsVectorsProposedAndClaudePJudgedParaClaudePEsElMismoEjercicioQue355eParecido099AndTheReasonClaudePGave(
            ) {
        World world = World.withNomic();
        world.claude.answers("Have you ever been to Rome?", "Have you ever visited Rome?", true,
                "La misma idea con otras palabras: «been to» por «visited», y el alumno elige «-» igual");

        AuditNode root = world.run(course(topic("Montañas y océanos", beenToRome(), visitedRome())), budget(10));

        Finding copy = only(quiz(root, VISITED_ROME));
        assertEquals("Para claude -p es el mismo ejercicio que …355e: parecido 0,99", part(copy, "Casi igual"));
        assertEquals("La misma idea con otras palabras: «been to» por «visited», y el alumno elige «-» igual",
                part(copy, "Razón de claude -p"));
        assertEquals("Have you ever visited Rome?", part(copy, "Copia terminada"));
        assertEquals("Have you ever been to Rome?", part(copy, "Queda terminado"));
        for (EvidencePart part : copy.getEvidence().getExamined()) {
            assertFalse(part.getText().contains("[") || part.getText().contains("____"), part.getText());
        }
    }

    @Test
    @DisplayName("should ask claude -p in an exam run with Ollama only about the candidates whose copy is an exercise of the exam's file, even when the exercise that stays is not in it")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R007")
    public void shouldAskClaudePInAnExamRunWithOllamaOnlyAboutTheCandidatesWhoseCopyIsAnExerciseOfTheExamsFileEvenWhenTheExerciseThatStaysIsNotInIt(
            ) {
        World world = World.notExamined();
        world.claude.answers("They cooked lunch for each other.", "They prepared lunch for each other.", true,
                "Cambia sólo el verbo, cooked por prepared: el alumno escribe «each other» en la misma oración");
        world.claude.answers("Offer: I'll wash the dishes for you.", "Offer: I'll wash the dishes.", true,
                "Piden lo mismo: «'ll wash» en una oferta; sólo sobra «for you»");
        // The file has «They prepared…», a copy whose exercise that stays is not in it, and «…for you.», the
        // exercise that stays of a copy that is outside it.
        EvaluationRunPolicy exam = new EvaluationRunPolicy(100, false, null, Set.of(PREPARED, WASH_FOR_YOU),
                EvaluationRunPurpose.EXAM, 1);

        AuditNode root = world.run(course(topic("Each other o reflexivos", cooked(), prepared()),
                topic("Funciones mixtas", washForYou(), wash())), exam);

        assertEquals(List.of("They cooked lunch for each other. || They prepared lunch for each other."),
                world.claude.pairs(), "its copy is in the file, though «They cooked…», the one that stays, is not");
        assertEquals(Map.of(PREPARED, COOKED), copies(root), "«…the dishes.» is outside the file: its pair is not asked");
        assertNull(score(root, WASH));
        assertEquals(List.of("clustering: They cooked lunch for each other.",
                "clustering: They prepared lunch for each other."), sorted(world.nomic.asked()),
                "and no vector is asked for a pair whose copy is outside the file");
        assertTrue(runOf(root).isPublished(), "the exam run publishes what it measures");
    }

    @Test
    @DisplayName("should ask claude -p in an exam run without Ollama only about the candidates whose copy is an exercise of the exam's file")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R007")
    public void shouldAskClaudePInAnExamRunWithoutOllamaOnlyAboutTheCandidatesWhoseCopyIsAnExerciseOfTheExamsFile(
            ) {
        World world = World.withoutOllama();
        world.claude.answers("Offer: I'll wash the dishes for you.", "Offer: I'll wash the dishes.", true,
                "Piden lo mismo: «'ll wash» en una oferta; sólo sobra «for you»");
        world.claude.answers("Have you ever been to Rome?", "Have you ever visited Rome?", true,
                "La misma idea con otras palabras: «been to» por «visited», y el alumno elige «-» igual");
        world.claude.answers("I am sorry.", "I am hot.", false, "Dos oraciones distintas: sorry y hot");
        // The file has «…the dishes.», a copy, and «Have you ever been to Rome?», the exercise that stays of a copy
        // that is outside it. «I am hot.» is in a topic the file does not reach.
        EvaluationRunPolicy exam = new EvaluationRunPolicy(100, false, null, Set.of(WASH, BEEN_TO_ROME),
                EvaluationRunPurpose.EXAM, 1);

        AuditNode root = world.run(course(topic("Funciones mixtas", washForYou(), wash()),
                topic("Montañas y océanos", beenToRome(), visitedRome()), topic("Afirmativas con be", sorry(), hot())),
                exam);

        assertEquals(List.of("Offer: I'll wash the dishes for you. || Offer: I'll wash the dishes."),
                world.claude.pairs(), "only the pair whose copy is in the file");
        assertEquals(Map.of(WASH, WASH_FOR_YOU), copies(root), "«Have you ever visited Rome?» is outside: no finding");
    }

    @Test
    @DisplayName("should give an exercise outside the exam's file neither score nor finding in an exam run, nor count the pairs whose copy it is among the consultations the exam says are missing")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R007")
    public void shouldGiveAnExerciseOutsideTheExamsFileNeitherScoreNorFindingInAnExamRunNorCountThePairsWhoseCopyItIsAmongTheConsultationsTheExamSaysAreMissing(
            ) {
        World world = World.withoutOllama();
        // An exam without budget is a rehearsal: it consults nothing and says how many consultations are missing.
        EvaluationRunPolicy exam = new EvaluationRunPolicy(0, false, null, Set.of(WASH), EvaluationRunPurpose.EXAM, 1);

        AuditNode root = world.run(course(topic("Funciones mixtas", washForYou(), wash()),
                topic("Montañas y océanos", beenToRome(), visitedRome()), topic("Afirmativas con be", sorry(), hot())),
                exam);

        assertTrue(halfway(root, WASH), "its pair with «…for you.» is in the file and nobody judged it");
        assertEquals(1.0, score(root, WASH));
        for (String outside : List.of(WASH_FOR_YOU, BEEN_TO_ROME, VISITED_ROME, SORRY, HOT)) {
            assertNull(score(root, outside), outside);
            assertEquals(List.of(), findings(quiz(root, outside)), outside);
        }
        assertEquals(1, coverageOf(root).getCost().getPendingConsultations(),
                "the pairs of «Have you ever visited Rome?» and «I am hot.» are not counted: their copy is outside");
        assertTrue(runOf(root).getReason().endsWith("quedan 1 par sin juzgar"), runOf(root).getReason());
        assertEquals(0, world.claude.calls());
    }

    @Test
    @DisplayName("should ask claude -p, with Ollama, only about the candidates that the wildcard does not settle and whose vectors reach 0.80, as «Have you heard about my news?» after «Have you heard the news?», 0.94, and not about «I am hot.» after «I am sorry.», 0.71")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R009")
    public void shouldAskClaudePWithOllamaOnlyAboutTheCandidatesThatTheWildcardDoesNotSettleAndWhoseVectorsReach080AsHaveYouHeardAboutMyNewsAfterHaveYouHeardTheNews094AndNotAboutIAmHotAfterIAmSorry071(
            ) {
        World world = World.withNomic();
        world.claude.answers("Have you heard the news?", "Have you heard about my news?", false,
                "Otra oración: «about my news» cambia lo que se cuenta, aunque el alumno escriba lo mismo");

        AuditNode root = world.run(course(topic("Participios irregulares: repaso 1", heardNews(), heardMyNews()),
                topic("Afirmativas con be", sorry(), hot()), topic("Contraste inicial", whereHe(), whereShe())),
                budget(10));

        assertEquals(List.of("Have you heard the news? || Have you heard about my news?"), world.claude.pairs(),
                "0.94 reaches 0.80: asked; «I am hot.», 0.71, and the wildcard pair are not");
        assertEquals(List.of("clustering: Have you heard about my news?", "clustering: Have you heard the news?",
                "clustering: I am hot.", "clustering: I am sorry."), sorted(world.nomic.asked()),
                "the vectors of every pair the wildcard does not settle, and none of «Where is he?»");
        assertEquals(List.of(), findings(quiz(root, HEARD_MY_NEWS)), "0.94 alone marks nothing: claude -p said no");
        assertEquals(1.0, score(root, HEARD_MY_NEWS));
        assertFalse(halfway(root, HEARD_MY_NEWS), "judged: nothing is missing");
        assertEquals(1.0, score(root, HOT));
        assertFalse(halfway(root, HOT), "below 0.80 there is no candidate, so nothing is missing");
        assertEquals("same-but-wildcard", only(quiz(root, WHERE_SHE)).getRule());
    }

    @Test
    @DisplayName("should not mark, with Ollama, a candidate whose vectors reach 0.97 when claude -p calls it a different exercise, «That car is his.» after «That is his car.»")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R009")
    public void shouldNotMarkWithOllamaACandidateWhoseVectorsReach097WhenClaudePCallsItADifferentExerciseThatCarIsHisAfterThatIsHisCar(
            ) {
        World world = World.withNomic();
        world.claude.answers("That is his car.", "That car is his.", false,
                "Contrastan adjetivo y pronombre: el alumno escribe «his» con otro papel");

        AuditNode root = world.run(course(topic("Adjetivo o pronombre", hisCar(), carHis())), budget(10));

        assertEquals(List.of("That is his car. || That car is his."), world.claude.pairs(),
                "its vectors, 0.97, propose the pair and claude -p decides it");
        assertEquals(List.of(), findings(quiz(root, CAR_HIS)), "the similarity alone marks nothing");
        assertEquals(1.0, score(root, CAR_HIS));
        assertEquals(1.0, score(root, HIS_CAR));
        assertFalse(halfway(root, CAR_HIS), "judged: nothing is missing");
        assertTrue(((QuizDiagnoses) quiz(root, CAR_HIS).getDiagnoses()).getNearDuplicateDiagnosis().isEmpty());
        assertEquals(0, errorCountsOf(root).getWithError());
    }

    @Test
    @DisplayName("should ask nomic-embed-text for every vector and stop only claude -p at the budget of --budget near-duplicates=1")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R009")
    public void shouldAskNomicembedtextForEveryVectorAndStopOnlyClaudePAtTheBudgetOfBudgetNearduplicates1(
            ) {
        World world = World.withNomic();
        world.claude.answers("A: Are we happy? B: Yes, we are.", "A: Are they ready? B: Yes, they are.", false,
                "Otra pregunta: happy y ready");
        world.claude.answers("A: Are we happy? B: Yes, we are.", "A: Are we ready? B: Yes, we are.", false,
                "Otra pregunta: happy y ready");

        AuditNode root = world.run(course(topic("Respuestas cortas", weHappy(), theyReady(), weReady())), budget(1));

        assertEquals(List.of("clustering: A: Are they ready? B: Yes, they are.",
                "clustering: A: Are we happy? B: Yes, we are.", "clustering: A: Are we ready? B: Yes, we are."),
                sorted(world.nomic.asked()), "--budget near-duplicates=1 does not reach the vectors: every one is asked");
        assertEquals(List.of("A: Are we happy? B: Yes, we are. || A: Are they ready? B: Yes, they are."),
                world.claude.pairs(), "claude -p stops at the budget: the first pair in the order of the topic");
        assertFalse(halfway(root, THEY_READY));
        assertTrue(halfway(root, WE_READY), "its pair with «Are we happy?» is the one the budget left");
        String reason = runOf(root).getReason();
        assertTrue(reason.startsWith("se acabó el tope"), reason);
        assertTrue(reason.endsWith("quedan 1 par sin juzgar"), reason);
    }

    @Test
    @DisplayName("should leave half evaluated, with Ollama and without --budget near-duplicates=N, an exercise with a candidate of its vectors, consulting no claude -p, keeping its wildcard findings and saying «falta el tope» with the pairs left to judge")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R009")
    public void shouldLeaveHalfEvaluatedWithOllamaAndWithoutBudgetNearduplicatesNAnExerciseWithACandidateOfItsVectorsConsultingNoClaudePKeepingItsWildcardFindingsAndSayingFaltaElTopeWithThePairsLeftToJudge(
            ) {
        World world = World.withNomic();

        // Without --budget near-duplicates=N: «Are we happy?» against the other two, 0.82 and 0.87, waits.
        AuditNode root = world.run(course(topic("Respuestas cortas", weHappy(), theyReady(), weReady())), null);

        AuditNode weReady = quiz(root, WE_READY);
        assertEquals("same-but-wildcard", only(weReady).getRule(), "its wildcard finding counts");
        assertEquals(THEY_READY, only(weReady).getIdentity().getMarker());
        assertEquals(0.0, score(root, WE_READY));
        assertTrue(halfway(root, WE_READY), "its pair with «Are we happy?» comes first and is missing");
        assertTrue(halfway(root, THEY_READY));
        assertEquals(1.0, score(root, THEY_READY));
        assertFalse(halfway(root, WE_HAPPY), "the first of the topic has nothing before it");
        assertEquals(3, world.nomic.asked().size(), "the vectors have no limit: they are asked without --budget");
        assertEquals(0, world.claude.calls());
        String reason = runOf(root).getReason();
        assertTrue(reason.startsWith("falta el tope"), reason);
        assertTrue(reason.endsWith("quedan 2 pares sin juzgar"), reason);
        assertEquals(2, coverageOf(root).getCost().getPendingConsultations());
    }

    @Test
    @DisplayName("should leave half evaluated, with Ollama, an exercise whose candidate got no usable answer from claude -p, keeping its wildcard findings and saying how many pairs are left to judge")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R009")
    public void shouldLeaveHalfEvaluatedWithOllamaAnExerciseWhoseCandidateGotNoUsableAnswerFromClaudePKeepingItsWildcardFindingsAndSayingHowManyPairsAreLeftToJudge(
            ) {
        World world = World.withNomic();
        // Only the pair of «Are they ready?» has an answer: for the pair of «Are we ready?» none serves.
        world.claude.answers("A: Are we happy? B: Yes, we are.", "A: Are they ready? B: Yes, they are.", false,
                "Otra pregunta: happy y ready");

        AuditNode root = world.run(course(topic("Respuestas cortas", weHappy(), theyReady(), weReady())), budget(10));

        AuditNode weReady = quiz(root, WE_READY);
        assertEquals("same-but-wildcard", only(weReady).getRule(), "its wildcard finding counts");
        assertEquals(THEY_READY, only(weReady).getIdentity().getMarker());
        assertEquals(0.0, score(root, WE_READY));
        assertTrue(halfway(root, WE_READY), "its pair with «Are we happy?» got no answer that serves");
        assertFalse(halfway(root, THEY_READY), "its only candidate was judged");
        assertEquals(Set.of("A: Are we happy? B: Yes, we are. || A: Are they ready? B: Yes, they are.",
                "A: Are we happy? B: Yes, we are. || A: Are we ready? B: Yes, we are."), Set.copyOf(world.claude.pairs()),
                "claude -p was asked about both candidates");
        assertTrue(runOf(root).getReason().contains("1 par"), "the report says how many pairs are left: "
                + runOf(root).getReason());
    }

    @Test
    @DisplayName("should declare as missing for near-duplicates only the pairs left for claude -p, with their hours at the rate of the judge one at a time and four at a time, never the vectors of nomic-embed-text")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R009")
    public void shouldDeclareAsMissingForNearduplicatesOnlyThePairsLeftForClaudePWithTheirHoursAtTheRateOfTheJudgeOneAtATimeAndFourAtATimeNeverTheVectorsOfNomicembedtext(
            ) {
        World world = World.withNomic();
        Instant start = Instant.parse("2026-10-02T10:00:00Z");
        // claude -p answered 20 seconds apart, 180 an hour; the vectors of nomic-embed-text, a second apart.
        world.timeline.times.put(JUDGE_ID, List.of(start, start.plusSeconds(20), start.plusSeconds(40),
                start.plusSeconds(60), start.plusSeconds(80), start.plusSeconds(100)));
        List<Instant> vectors = new ArrayList<>();
        for (int second = 0; second < 40; second++) {
            vectors.add(start.plusSeconds(second));
        }
        world.timeline.times.put(VECTORS_ID, vectors);

        AuditNode root = world.run(course(topic("Respuestas cortas", weHappy(), theyReady(), weReady())), null);

        JudgeCoverageDiagnosis coverage = coverageOf(root);
        assertEquals(List.of(JUDGE_ID), coverage.getEvaluators().stream().map(EvaluatorCoverage::getEvaluatorId).toList(),
                "the coverage is the one of claude -p alone");
        JudgeCostEstimate cost = coverage.getCost();
        assertEquals(2, cost.getPendingConsultations(), "the two pairs left for claude -p, not the vectors");
        assertEquals(180.0, cost.getRatePerHour(), 1e-6, "at the rate of the judge, not mixed with the vectors'");
        assertTrue(cost.isRateMeasured());
        assertEquals(2 / 180.0, cost.getHoursOneAtATime(), 1e-9);
        assertEquals(4, cost.getMaxConcurrentEvaluations());
        assertEquals(2 / 180.0 / 4, cost.getHoursConcurrent(), 1e-9);
        assertEquals(0, world.claude.calls());
    }

    @Test
    @DisplayName("should evaluate whole a topic outside the set that a run declares for claude -p when its vectors stay below 0.80, «I am hot.» after «I am sorry.», since the vectors always run")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R009")
    public void shouldEvaluateWholeATopicOutsideTheSetThatARunDeclaresForClaudePWhenItsVectorsStayBelow080IAmHotAfterIAmSorrySinceTheVectorsAlwaysRun(
            ) {
        World world = World.withNomic();
        world.claude.answers("Offer: I'll wash the dishes for you.", "Offer: I'll wash the dishes.", true,
                "Piden lo mismo: «'ll wash» en una oferta; sólo sobra «for you»");
        // The run declares the two exercises of Funciones mixtas for claude -p, as a re-evaluation of that pair.
        EvaluationRunPolicy declared = new EvaluationRunPolicy(10, false, null, Set.of(WASH_FOR_YOU, WASH),
                EvaluationRunPurpose.COURSE, null);

        AuditNode root = world.run(course(topic("Funciones mixtas", washForYou(), wash()),
                topic("Afirmativas con be", sorry(), hot())), declared);

        assertEquals(List.of("Offer: I'll wash the dishes for you. || Offer: I'll wash the dishes."), world.claude.pairs(),
                "the set chooses what is asked to claude -p again");
        assertEquals(1.0, score(root, HOT));
        assertFalse(halfway(root, HOT), "its pair stays at 0.71, below 0.80: nothing is missing");
        assertEquals(List.of(), findings(quiz(root, HOT)));
        assertEquals(List.of("clustering: I am hot.", "clustering: I am sorry.",
                "clustering: Offer: I'll wash the dishes for you.", "clustering: Offer: I'll wash the dishes."),
                sorted(world.nomic.asked()), "the set never chooses the vectors: the topic outside it is asked too");
    }

    @Test
    @DisplayName("should leave all of near-duplicates unevaluated, the wildcard included, while the version with Ollama, named by its three pieces, has no approved exam, even if nomic-embed-text with its prefix alone approved one")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R010")
    public void shouldLeaveAllOfNearduplicatesUnevaluatedTheWildcardIncludedWhileTheVersionWithOllamaNamedByItsThreePiecesHasNoApprovedExamEvenIfNomicembedtextWithItsPrefixAloneApprovedOne(
            ) {
        World world = World.notExamined();
        // The exam of 30/9 approved nomic-embed-text with its prefix alone: no candidate threshold, no judge.
        world.exams.approve(NOMIC_EXAMINED);

        AuditNode root = world.run(course(topic("Contraste inicial", whereHe(), whereShe()),
                topic("Each other o reflexivos", cooked(), prepared())), budget(10));

        for (String id : List.of(WHERE_HE, WHERE_SHE, COOKED, PREPARED)) {
            AuditNode quiz = quiz(root, id);
            assertNull(quiz.getScores().get("near-duplicates"), id);
            assertTrue(quiz.getUnevaluatedBy().contains("near-duplicates"), id);
            assertEquals(List.of(), findings(quiz), id);
        }
        assertEquals(List.of(), world.nomic.asked(), "nothing is asked for a version that does not publish");
        assertEquals(0, world.claude.calls());
        LocalToolRunDiagnosis run = runOf(root);
        assertFalse(run.isPublished());
        assertEquals(WITH_OLLAMA_EXAMINED, run.getExaminedVersion());
        assertEquals(JudgeExamStatus.NOT_TAKEN, run.getExamState());
        assertTrue(run.getReason().startsWith("sin examen"), run.getReason());
    }

    @Test
    @DisplayName("should not ask claude -p again, with Ollama, about a pair it already judged without Ollama, «Offer: I'll wash the dishes.» after «Offer: I'll wash the dishes for you.»")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R010")
    public void shouldNotAskClaudePAgainWithOllamaAboutAPairItAlreadyJudgedWithoutOllamaOfferIllWashTheDishesAfterOfferIllWashTheDishesForYou(
            ) {
        World without = World.withoutOllama();
        without.exams.approve(WITH_OLLAMA_EXAMINED);
        without.claude.answers("Offer: I'll wash the dishes for you.", "Offer: I'll wash the dishes.", true,
                "Piden lo mismo: «'ll wash» en una oferta; sólo sobra «for you»");
        without.run(course(topic("Funciones mixtas", washForYou(), wash())), budget(10));
        assertEquals(1, without.claude.calls(), "claude -p judged the pair once, without Ollama");

        // The same ledger, the same exams and the same claude -p, with Ollama on.
        World with = without.turned(true);
        AuditNode root = with.run(course(topic("Funciones mixtas", washForYou(), wash())), budget(10));

        assertEquals(1, with.claude.calls(), "its verdict is reused: not a single new consultation");
        Finding copy = only(quiz(root, WASH));
        assertEquals("same-exercise", copy.getRule());
        assertEquals(WASH_FOR_YOU, copy.getIdentity().getMarker());
        assertEquals(0.9937, diagnosis(root, WASH).getSimilarity(), 1e-5, "now it was proposed by its vectors");
    }

    @Test
    @DisplayName("should reuse, with Ollama, the vectors that the version of 30/9 recorded, without asking nomic-embed-text again, since neither the candidate threshold nor the judge enters their record")
    @Tag("FEAT-MLOCAL")
    @Tag("F-MLOCAL-R002")
    public void shouldReuseWithOllamaTheVectorsThatTheVersionOf309RecordedWithoutAskingNomicembedtextAgainSinceNeitherTheCandidateThresholdNorTheJudgeEntersTheirRecord(
            ) {
        World world = World.withNomic();
        // What the version of 30/9 left in the ledger: the vector of each exercise, under the version of the vectors
        // alone, «nomic-embed-text@0a109f422b47 · clustering-1», without a threshold or a judge in it.
        EvaluationSession recorded = new DefaultEvaluationSessionFactory(world.ledger, new Sha256ContentFingerprinter())
                .open(new EvaluationBudget(10), new SentenceEmbeddingEvaluator(new RecordedNomic(true),
                        new DefaultNearDuplicatesConfig(), NOMIC_EXAMINED));
        assertEquals(EvaluationResolutionKind.EVALUATED, recorded.resolve(new EvaluationSubject(COOKED,
                Map.of("exercise", "They cooked lunch for each other."))).getKind());
        assertEquals(EvaluationResolutionKind.EVALUATED, recorded.resolve(new EvaluationSubject(PREPARED,
                Map.of("exercise", "They prepared lunch for each other."))).getKind());
        world.claude.answers("They cooked lunch for each other.", "They prepared lunch for each other.", true,
                "Cambia sólo el verbo, cooked por prepared: el alumno escribe «each other» en la misma oración");

        AuditNode root = world.run(course(topic("Each other o reflexivos", cooked(), prepared())), budget(10));

        assertEquals(List.of(), world.nomic.asked(), "every vector is reused: nomic-embed-text is not asked again");
        assertEquals("same-exercise", only(quiz(root, PREPARED)).getRule());
        assertEquals(0.9759, diagnosis(root, PREPARED).getSimilarity(), 1e-5,
                "the cosine is the one of the recorded vectors");
        assertEquals(1, world.claude.calls(), "claude -p is asked, within its budget: what is reused is the vectors");
    }
}
