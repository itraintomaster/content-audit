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
import com.learney.contentaudit.evaluationledgerdomain.EvaluationEmitted;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationFailureKind;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationKey;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationLedger;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationNotEmitted;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationOutcome;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRecord;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSubject;
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
    @DisplayName("should mark as near duplicates two candidates whose vectors reach 0.95, «They prepared lunch for each other.» after «They cooked lunch for each other.», 0.98")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R002")
    public void shouldMarkAsNearDuplicatesTwoCandidatesWhoseVectorsReach095TheyPreparedLunchForEachOtherAfterTheyCookedLunchForEachOther098(
            ) {
        World world = World.withNomic();

        AuditNode root = world.run(course(topic("Each other o reflexivos", cooked(), prepared())), null);

        Finding copy = only(quiz(root, PREPARED));
        assertEquals("same-meaning", copy.getRule());
        assertEquals(COOKED, copy.getIdentity().getMarker());
        NearDuplicateDiagnosis diagnosis = diagnosis(root, PREPARED);
        assertEquals(NearDuplicateCondition.SIMILARITY, diagnosis.getCondition());
        assertEquals(0.9759, diagnosis.getSimilarity(), 1e-5);
        assertNull(diagnosis.getDifference(), "the wildcard does not settle them: cooked and prepared");
        assertEquals(List.of("clustering: They cooked lunch for each other.",
                "clustering: They prepared lunch for each other."), world.nomic.asked());
    }

    @Test
    @DisplayName("should not mark a candidate whose vectors stay below 0.95 and that the wildcard does not settle, «Have you heard about my news?» after «Have you heard the news?», 0.94")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R002")
    public void shouldNotMarkACandidateWhoseVectorsStayBelow095AndThatTheWildcardDoesNotSettleHaveYouHeardAboutMyNewsAfterHaveYouHeardTheNews094(
            ) {
        World world = World.withNomic();

        AuditNode root = world.run(course(topic("Participios irregulares: repaso 1", heardNews(), heardMyNews())), null);

        assertEquals(List.of(), findings(quiz(root, HEARD_MY_NEWS)));
        assertEquals(1.0, score(root, HEARD_MY_NEWS));
        assertFalse(halfway(root, HEARD_MY_NEWS), "both vectors came: it is evaluated whole");
        assertEquals(2, world.nomic.asked().size());
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
        AuditNode root = World.withNomic().run(course(topic("Respuestas cortas", canDrive(), couldHave(), couldDrive())),
                null);

        assertEquals(List.of(), findings(quiz(root, CAN_DRIVE)));
        assertEquals(CAN_DRIVE, only(quiz(root, COULD_HAVE)).getIdentity().getMarker());
        assertEquals("same-meaning", only(quiz(root, COULD_HAVE)).getRule(), "0.9565 with «Can I drive…»");
        assertEquals(CAN_DRIVE, only(quiz(root, COULD_DRIVE)).getIdentity().getMarker(), "the first, never …251d");
        assertEquals("same-but-wildcard", only(quiz(root, COULD_DRIVE)).getRule(), "Can → Could");
        assertEquals(2, errorCountsOf(root).getWithError(), "2 copies in a group of 3");
    }

    @Test
    @DisplayName("should mark the same copies in two runs of the same course, reusing every vector without asking nomic-embed-text again")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R003")
    public void shouldMarkTheSameCopiesInTwoRunsOfTheSameCourseReusingEveryVectorWithoutAskingNomicembedtextAgain(
            ) {
        World world = World.withNomic();

        AuditNode first = world.run(course(topic("Respuestas cortas", canDrive(), couldHave(), couldDrive())), null);
        List<String> asked = List.copyOf(world.nomic.asked());
        AuditNode second = world.run(course(topic("Respuestas cortas", canDrive(), couldHave(), couldDrive())), null);

        assertEquals(Map.of(COULD_HAVE, CAN_DRIVE, COULD_DRIVE, CAN_DRIVE), copies(first));
        assertEquals(copies(first), copies(second));
        assertEquals(3, asked.size());
        assertEquals(asked, world.nomic.asked(), "the second run reuses every vector");
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
    @DisplayName("should give each finding the cost of the rule that found it: instant for same-but-wildcard, local model for same-meaning and paid model for same-exercise")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R004")
    public void shouldGiveEachFindingTheCostOfTheRuleThatFoundItInstantForSamebutwildcardLocalModelForSamemeaningAndPaidModelForSameexercise(
            ) {
        AuditNode withOllama = World.withNomic().run(course(topic("Contraste inicial", whereHe(), whereShe()),
                topic("Each other o reflexivos", cooked(), prepared())), null);
        World alternative = World.withoutOllama();
        alternative.claude.answers("Offer: I'll wash the dishes for you.", "Offer: I'll wash the dishes.", true,
                "Piden lo mismo: «'ll wash» en una oferta; sólo sobra «for you»");
        AuditNode withoutOllama = alternative.run(course(topic("Funciones mixtas", washForYou(), wash())), budget(10));

        assertEquals(AnalysisCost.INSTANT, only(quiz(withOllama, WHERE_SHE)).getCost());
        assertEquals(AnalysisCost.LOCAL_MODEL, only(quiz(withOllama, PREPARED)).getCost());
        assertEquals(AnalysisCost.PAID_MODEL, only(quiz(withoutOllama, WASH)).getCost());
    }

    @Test
    @DisplayName("should give a pair found both by the wildcard and by its vectors the instant rule same-but-wildcard, «A: Are we ready? B: Yes, we are.» after «A: Are they ready? B: Yes, they are.»")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R004")
    public void shouldGiveAPairFoundBothByTheWildcardAndByItsVectorsTheInstantRuleSamebutwildcardAAreWeReadyBYesWeAreAfterAAreTheyReadyBYesTheyAre(
            ) {
        World world = World.withNomic();

        // «Are we happy?» before them asks for the three vectors: …1f5e and …25e8 also reach 0.965.
        AuditNode root = world.run(course(topic("Respuestas cortas", weHappy(), theyReady(), weReady())), null);

        Finding copy = only(quiz(root, WE_READY));
        assertEquals("same-but-wildcard", copy.getRule());
        assertEquals(AnalysisCost.INSTANT, copy.getCost());
        assertEquals(THEY_READY, copy.getIdentity().getMarker());
        NearDuplicateDiagnosis diagnosis = diagnosis(root, WE_READY);
        assertEquals(NearDuplicateCondition.WILDCARD, diagnosis.getCondition());
        assertEquals("they → we", diagnosis.getDifference());
        assertEquals(0.965, diagnosis.getSimilarity(), 1e-5, "its vectors reach 0.95 as well");
        assertEquals(3, world.nomic.asked().size());
        assertEquals(List.of(), findings(quiz(root, THEY_READY)), "0.82 with «Are we happy?»");
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
    @DisplayName("should say in the evidence of a pair found by its vectors «Dice lo mismo que …46ac: parecido 0,98»")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R005")
    public void shouldSayInTheEvidenceOfAPairFoundByItsVectorsDiceLoMismoQue46acParecido098() {
        AuditNode root = World.withNomic().run(course(topic("Each other o reflexivos", cooked(), prepared())), null);

        Finding copy = only(quiz(root, PREPARED));
        assertEquals("Dice lo mismo que …46ac: parecido 0,98", part(copy, "Casi igual"));
        assertEquals("They prepared lunch for ___ . (Julie prepared lunch for David and David prepared lunch for Julie.) "
                + "(each other · ourselves · themselves · yourselves)", part(copy, "Copia"));
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
    @DisplayName("should leave all of near-duplicates unevaluated, the wildcard included, while the examined version of nomic-embed-text with its prefix has no approved exam")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R007")
    public void shouldLeaveAllOfNearduplicatesUnevaluatedTheWildcardIncludedWhileTheExaminedVersionOfNomicembedtextWithItsPrefixHasNoApprovedExam(
            ) {
        World world = World.notExamined();

        AuditNode root = world.run(course(topic("Contraste inicial", whereHe(), whereShe()),
                topic("Each other o reflexivos", cooked(), prepared())), null);

        for (String id : List.of(WHERE_HE, WHERE_SHE, COOKED, PREPARED)) {
            AuditNode quiz = quiz(root, id);
            assertNull(quiz.getScores().get("near-duplicates"), id);
            assertTrue(quiz.getUnevaluatedBy().contains("near-duplicates"), id);
            assertEquals(List.of(), findings(quiz), id);
        }
        assertEquals(List.of(), world.nomic.asked(), "nothing is asked for a version that does not publish");
        LocalToolRunDiagnosis run = runOf(root);
        assertFalse(run.isPublished());
        assertEquals(NOMIC_EXAMINED, run.getExaminedVersion());
        assertEquals(JudgeExamStatus.NOT_TAKEN, run.getExamState());
        assertTrue(run.getReason().startsWith("sin examen"), run.getReason());
    }

    @Test
    @DisplayName("should mark the same copies when the same examined version takes the exam twice, without a single new consultation")
    @Tag("FEAT-DUPAR")
    @Tag("F-DUPAR-R007")
    public void shouldMarkTheSameCopiesWhenTheSameExaminedVersionTakesTheExamTwiceWithoutASingleNewConsultation(
            ) {
        World world = World.notExamined();
        // The exam asks only the topics of its file -- here two of the three -- and publishes without approval.
        EvaluationRunPolicy exam = new EvaluationRunPolicy(100, false, null, Set.of(PREPARED, COULD_HAVE, COULD_DRIVE),
                EvaluationRunPurpose.EXAM, 1);

        AuditNode first = world.run(examCourse(), exam);
        List<String> asked = List.copyOf(world.nomic.asked());
        AuditNode second = world.run(examCourse(), exam);

        assertEquals(Map.of(PREPARED, COOKED, COULD_HAVE, CAN_DRIVE, COULD_DRIVE, CAN_DRIVE), copies(first));
        assertEquals(copies(first), copies(second));
        assertEquals(5, asked.size(), "two vectors of Each other and three of Respuestas cortas");
        assertEquals(asked, world.nomic.asked(), "the second exam asks nothing new");
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

    /** The examined version of nomic-embed-text in Ollama with the prefix «clustering: ». */
    static final String NOMIC_EXAMINED = "nomic-embed-text@0a109f422b47 · clustering-1";

    /** The version the agent near-duplicates-same-exercise states: its definition, model and provider. */
    static final String CLAUDE_VERSION = "c3a9e1f07b5d42c8a6e0f9d8c7b6a5948372615043f2e1d0c9b8a7f6e5d4c3b2";

    static final class World {
        final Ledger ledger = new Ledger();
        final Exams exams = new Exams();
        final RecordedNomic nomic;
        final RecordedClaude claude = new RecordedClaude();
        final DefaultJudgeRuntime runtime;
        final RecordingRuntime recording;
        final DefaultLocalToolGate gate;
        final NearDuplicatesAnalyzerProvider provider;

        private World(boolean ollama) {
            this.nomic = new RecordedNomic(ollama);
            Sha256ContentFingerprinter fingerprinter = new Sha256ContentFingerprinter();
            this.runtime = new DefaultJudgeRuntime(new DefaultEvaluationSessionFactory(ledger, fingerprinter),
                    new ExamEvaluationSessionFactory(ledger, fingerprinter), exams, evaluatorId -> List.of(),
                    new Judges());
            this.recording = new RecordingRuntime(runtime);
            this.gate = new DefaultLocalToolGate(runtime);
            this.provider = new NearDuplicatesAnalyzerProvider(nomic, claude, recording, corrector(), renderer(), gate);
        }

        /** nomic-embed-text in Ollama, whose examined version approved its exam. */
        static World withNomic() {
            World world = new World(true);
            world.exams.approve(NOMIC_EXAMINED);
            return world;
        }

        /** nomic-embed-text in Ollama, without exam. */
        static World notExamined() {
            return new World(true);
        }

        /** Ollama off: the alternative with claude -p, whose version approved its exam. */
        static World withoutOllama() {
            World world = new World(false);
            world.exams.approve(CLAUDE_VERSION);
            return world;
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

    /** The agent near-duplicates-same-exercise with a recorded verdict per pair; it counts its consultations. */
    static final class RecordedClaude implements Evaluator {
        private final Map<String, String> verdicts = new LinkedHashMap<>();
        private final List<String> pairs = new ArrayList<>();
        final List<EvaluationSubject> subjects = new ArrayList<>();

        void answers(String kept, String copy, boolean sameExercise, String reason) {
            verdicts.put(kept + " || " + copy, "{\"sameExercise\": " + sameExercise + ", \"reason\": \""
                    + reason.replace("\"", "\\\"") + "\"}");
        }

        @Override
        public String evaluatorId() {
            return "near-duplicates-same-exercise";
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
            return Optional.of(CLAUDE_VERSION);
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
}
