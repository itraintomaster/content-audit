package com.learney.contentaudit.revisiondomain.panelengine;

import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.*;
import static org.junit.jupiter.api.Assertions.*;

import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.coursedomain.FormKind;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.SentenceMode;
import com.learney.contentaudit.revisiondomain.panel.ConversionOrder;
import com.learney.contentaudit.revisiondomain.panel.PanelCandidateKind;
import com.learney.contentaudit.revisiondomain.panel.PanelOutcomeKind;
import com.learney.contentaudit.revisiondomain.panel.TopicWork;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Regression from the accepted v10-to-v11 proposal, reproduced without any model. */
class RequestFormPreservationTest {
    private static final String Q = "67fab6d59930102295342dda";
    private static final String INSTRUCTIONS = "Transforma las oraciones para que incluyan 'could' o 'can'.";
    private static final String ORIGINAL_CARD = "$Can$ para el pedido normal, $Could$ para el cortés. Después, verbo base.";
    private static final List<String> SIXTEEN_ANSWERS = List.of(
            "Could you get me some tea?", "Could you please get me some tea?", "Could you get me some tea, please?",
            "Please could you get me some tea?", "Can you get me some tea?", "Can you please get me some tea?",
            "Can you get me some tea, please?", "Please can you get me some tea?", "Could you get some tea for me?",
            "Could you please get some tea for me?", "Could you get some tea for me, please?",
            "Please could you get some tea for me?", "Can you get some tea for me?",
            "Can you please get some tea for me?", "Can you get some tea for me, please?",
            "Please can you get some tea for me?");

    @Test
    void rejectsTheSavedProposalThatDropsTheSelectorAndAddsSixteenAnswersDespiteWholeExercisePermission() {
        QuizTemplateEntity original = original("Polite request:");
        List<String> violations = screen(original, savedBadProposal(original));
        assertTrue(violations.stream().anyMatch(s -> s.contains("indicación") && s.contains("Polite")), violations.toString());
        assertTrue(violations.stream().anyMatch(s -> s.contains("could") && s.contains("can")), violations.toString());
    }

    @Test
    void preservesTheExplicitClassEvenWhenEveryProposedAnswerUsesTheRightModal() {
        for (String prefix : List.of("", "Normal request:", "Normal:")) {
            QuizTemplateEntity original = original("Polite request:");
            assertFalse(screen(original, rewritten(original, prefix, "Could you get me some tea?")).isEmpty(), prefix);
        }
    }

    @Test
    void checksBothExplicitSelectorsIncludingShortLabelsAndPolitePrefixes() {
        for (String prefix : List.of("Polite request:", "Polite:", "  pOlItE REQUEST :")) {
            QuizTemplateEntity original = original(prefix);
            assertFalse(screen(original, rewritten(original, prefix, "Please, can you get me some tea?")).isEmpty(), prefix);
        }
        for (String prefix : List.of("Normal request:", "Normal:", "  NORMAL :")) {
            QuizTemplateEntity original = original(prefix);
            assertFalse(screen(original, rewritten(original, prefix, "Could you get me some tea?")).isEmpty(), prefix);
            assertEquals(List.of(), screen(original, rewritten(original, prefix, "Can you get me some tea?")), prefix);
        }
    }

    @Test
    void allowsLexicalRepairsTranslationAndNewHintsWhileKeepingTheRequestedForm() {
        QuizTemplateEntity original = original("Polite request:");
        QuizTemplateEntity repaired = rewritten(original, "Polite:", "Could you get me some tea?",
                "Could you please get some tea for me?", "Please could you get me some tea?");
        repaired.getForm().getSentenceParts().add(text("(get)"));
        repaired.setTranslation("¿Podrías conseguirme un poco de té?");
        assertEquals(List.of(), screen(original, repaired));
    }

    @Test
    void aRewriteStimulusWithoutFinalPunctuationDoesNotBecomePartOfTheAnswer() {
        QuizTemplateEntity original = original("Polite request:");
        QuizTemplateEntity repaired = withParts(original, text("Polite request: I want you to get me some tea"),
                cloze("Could you get me some tea?"));
        // No request-form violation: the stimulus did not become part of the answer. Its lost period is the only
        // reason left, since F-MESA-R018 does not let a candidate take it out.
        assertEquals(List.of("saca el punto entre «tea» y «Could» del enunciado"), screen(original, repaired));
        QuizTemplateEntity wrongForm = withParts(original, text("Polite request: I want you to get me some tea"),
                cloze("Can you get me some tea?"));
        assertFalse(screen(original, wrongForm).isEmpty());
    }

    @Test
    void doesNotInventNormalRequestsFromAnAbsentCueOrApplyToAnUnrelatedInstruction() {
        QuizTemplateEntity noCue = original("");
        assertEquals(List.of(), screen(noCue, rewritten(noCue, "", "Could you get me some tea?", "Can you get me some tea?")));
        QuizTemplateEntity differentLesson = original("Polite request:");
        differentLesson.setInstructions("Completa con un verbo de movimiento.");
        assertEquals(List.of(), screen(differentLesson, rewritten(differentLesson, "", "Can you get me some tea?")));
    }

    @Test
    void honorsAnOriginalExplicitPermissionToUseBothFormsButStillPreservesTheVisibleSelector() {
        for (String instructions : List.of("Usa can o could indistintamente para hacer pedidos corteses.",
                "Puedes usar could o can indistintamente en este ejercicio.",
                "Use can or could interchangeably to make polite requests.")) {
            QuizTemplateEntity original = original("Polite request:");
            original.setInstructions(instructions);
            assertEquals(List.of(), screen(original, rewritten(original, "Polite:", "Can you get me some tea?", "Could you get me some tea?")), instructions);
            assertTrue(screen(original, rewritten(original, "", "Can you get me some tea?")).stream()
                    .anyMatch(s -> s.contains("indicación")), instructions);
        }
    }

    @Test
    void neitherANegatedPermissionNorNewCandidateInstructionsCanDisableTheRequestedModal() {
        for (String instructions : List.of("No uses can o could indistintamente en estos pedidos.",
                "No puedes usar can o could indistintamente.", "Do not use can or could interchangeably.")) {
            QuizTemplateEntity original = original("Polite request:");
            original.setInstructions(instructions);
            assertFalse(screen(original, rewritten(original, "Polite:", "Can you get me some tea?")).isEmpty(), instructions);
        }
        QuizTemplateEntity original = original("Polite request:");
        QuizTemplateEntity proposed = rewritten(original, "Polite:", "Can you get me some tea?");
        proposed.setInstructions("Usa can o could indistintamente para hacer pedidos corteses.");
        assertFalse(screen(original, proposed).isEmpty());
    }

    @Test
    void aSelectorSpecificRestrictionTakesPrecedenceOverAGeneralPermission() {
        for (String instructions : List.of(
                "Usa can o could indistintamente para pedidos normales, pero could para Polite request.",
                "Usa can o could indistintamente. Para Polite request usa could.",
                "Usa can o could indistintamente para pedidos normales.",
                "Use can or could interchangeably, except use could for polite requests.")) {
            QuizTemplateEntity original = original("Polite request:");
            original.setInstructions(instructions);
            assertFalse(screen(original, rewritten(original, "Polite:", "Can you get me some tea?")).isEmpty(), instructions);
            assertEquals(List.of(), screen(original, rewritten(original, "Polite:", "Could you get me some tea?")), instructions);
        }
    }

    @Test
    void readsTheLocalInstructionAndUsesTheTopicOnlyWhenItIsMissing() {
        QuizTemplateEntity original = original("Polite request:");
        TopicWork work = workFor(original);
        work.getKnowledge().setInstructions(INSTRUCTIONS);
        original.setInstructions(null);
        assertFalse(new DefaultCandidateScreen(corrections()).screen(work, Q,
                candidate(PanelCandidateKind.WHOLE_EXERCISE, rewritten(original, "", "Could you get me some tea?"))).isEmpty());
        original.setInstructions("Completa con un verbo de movimiento.");
        assertEquals(List.of(), new DefaultCandidateScreen(corrections()).screen(work, Q,
                candidate(PanelCandidateKind.WHOLE_EXERCISE, rewritten(original, "", "Can you get me some tea?"))));
    }

    @Test
    void checksTheModalPositionWithoutConfusingALexicalCanInALaterGap() {
        QuizTemplateEntity original = original("Polite request:");
        QuizTemplateEntity modalGap = withParts(original, text("Polite request:"), cloze("Could", "Can"), text("you bring me some tea?"));
        assertFalse(screen(original, modalGap, SentenceMode.FILL).isEmpty());
        QuizTemplateEntity lexicalCan = withParts(original, text("Polite request: Could you bring me a"), cloze("can"), text("?"));
        assertEquals(List.of(), screen(original, lexicalCan, SentenceMode.FILL));
        QuizTemplateEntity splitModal = withParts(original, text("Polite request:"), cloze("Please"), cloze("could", "can"), text("you get me some tea?"));
        assertFalse(screen(original, splitModal, SentenceMode.FILL).isEmpty());
    }

    @Test
    void aConversionCannotDiscardTheSelectorOrMakeTheOtherModalCorrect() {
        QuizTemplateEntity original = original("Polite request:");
        TopicWork work = workFor(original);
        work.setConversion(new ConversionOrder(work.getKnowledge().getId(), FormKind.MULTIPLE_CHOICE, 3));
        QuizTemplateEntity proposed = choice(Q, "I want you to get me some tea.", "Can you get me some tea?", "Could you get me some tea?", "Get me some tea.");
        List<String> violations = new DefaultCandidateScreen(corrections()).screen(work, Q,
                candidate(PanelCandidateKind.MULTIPLE_CHOICE_CONVERSION, proposed));
        assertTrue(violations.stream().anyMatch(s -> s.contains("indicación")), violations.toString());
        assertTrue(violations.stream().anyMatch(s -> s.contains("could") && s.contains("can")), violations.toString());
    }

    @Test
    void retriesBeforeMeasuringTheBadProposalEvenWhenTheSimulatedJudgesWouldAcceptIt() {
        QuizTemplateEntity original = original("Polite request:");
        QuizTemplateEntity bad = savedBadProposal(original);
        QuizTemplateEntity good = rewritten(original, "Polite request:", "Could you get me some tea?", "Could you please get me some tea?");
        TopicWork work = workFor(original);
        Finding problem = work.getBriefs().get(0).getFinding();
        RecordingProposer proposer = new RecordingProposer(script(Q,
                proposal(candidate(PanelCandidateKind.WHOLE_EXERCISE, bad)),
                Q, proposal(candidate(PanelCandidateKind.WHOLE_EXERCISE, good))));
        RulesMeter meter = new RulesMeter().rule("single-answer", (topic, quiz) ->
                quiz.getForm().getSentenceParts().stream().anyMatch(p -> p.getOptions() != null
                        && p.getOptions().contains("Could you bring me some tea?")) ? List.of(problem) : RulesMeter.none());
        RecordingSink sink = new RecordingSink();

        panel(proposer, meter).run(work, settings(), allowance(2, 1), sink);

        assertEquals(PanelOutcomeKind.ACCEPTED, sink.only().getOutcome());
        assertEquals(2, sink.only().getAttempts());
        assertEquals(2, proposer.requests.size());
        assertTrue(proposer.requests.get(1).getPreviousRejections().stream().flatMap(r -> r.getReasons().stream())
                .anyMatch(s -> s.contains("indicación")));
        assertTrue(meter.statesMeasured.stream().map(t -> exercise(t, Q)).allMatch(q ->
                q.getForm().getSentenceParts().get(0).getText().startsWith("Polite request:")),
                "the proposal that lost the selector is never sent to a judge");
        assertEquals(ORIGINAL_CARD, original.getMiniTheory());
        assertEquals(List.of("Could you bring me some tea?"), original.getForm().getSentenceParts().get(1).getOptions());
    }

    @Test void recognizesTheSavedRadioSelectorAfterTheStimulus() {
        var before = withParts(original(""), text("Play the radio. Polite:"), cloze("Could you play the radio?"));
        assertFalse(screen(before, withParts(before, text("Play the radio."), cloze("Can you play the radio?"))).isEmpty());
        assertEquals(List.of(), screen(before, withParts(before, text("Play the radio. Polite:"), cloze("Could you turn on the radio?"))));
    }

    @Test void restoresHistoricalIntentWithoutInventingJudgeFindingsOrResettingCurrentLexicalRepairs() {
        var historical = original("Polite request:");
        var current = savedBadProposal(historical);
        current.setTranslation("¿Podrías conseguirme un poco de té?");
        TopicWork work = historicalWork(historical, current);
        var good = rewritten(current, "Polite:", "Could you get me some tea?");
        good.setMiniTheory("$Could$ + sujeto + verbo base para el pedido cortés de este ejercicio.");
        var proposer = new RecordingProposer(script(Q,
                proposal(candidate(PanelCandidateKind.WHOLE_EXERCISE, current)),
                Q, proposal(candidate(PanelCandidateKind.WHOLE_EXERCISE, good))));
        var meter = new RulesMeter(); // Neither before nor after has a judge finding.
        var sink = new RecordingSink();
        panel(proposer, meter).run(work, settings(), allowance(2, 1), sink);
        assertEquals(PanelOutcomeKind.ACCEPTED, sink.only().getOutcome());
        assertEquals(List.of("restore-contract"), sink.only().getResolvedTaskIds());
        assertEquals(2, proposer.requests.size());
        assertNotNull(proposer.requests.get(0).getDidacticReference());
        assertTrue(proposer.requests.get(0).getBriefs().stream().allMatch(b -> b.getFinding() == null));
        assertEquals("¿Podrías conseguirme un poco de té?", sink.only().getAccepted().getQuiz().getTranslation());
        assertEquals(List.of("Could you get me some tea?"), sink.only().getAccepted().getQuiz().getForm().getSentenceParts().get(1).getOptions());
        assertEquals(SIXTEEN_ANSWERS, current.getForm().getSentenceParts().get(1).getOptions(), "editable source remains intact");
        historical.setMiniTheory("mutated externally");
        assertEquals(ORIGINAL_CARD, work.getDidacticReferences().get(Q).sourceQuiz().getMiniTheory());
    }

    @Test void historicalRestorationStillMustPassTheMeasuredNonRegressionGate() {
        var historical = original("Polite:"); var current = savedBadProposal(historical);
        var work = historicalWork(historical, current);
        var good = rewritten(current, "Polite:", "Could you get me some tea?");
        var regression = finding("quiz-instruction", "new-error", Q, FindingSeverity.HIGH, FindingResolution.PANEL, "bad", "new regression");
        var meter = new RulesMeter().rule("quiz-instruction", (topic, q) -> q.getForm().getSentenceParts().get(0).getText().startsWith("Polite:")
                ? List.of(regression) : RulesMeter.none());
        var sink = new RecordingSink();
        var proposer = new RecordingProposer(script(Q, proposal(candidate(PanelCandidateKind.WHOLE_EXERCISE, good))));
        panel(proposer, meter).run(work, settings(), allowance(1, 1), sink);
        assertNotEquals(PanelOutcomeKind.ACCEPTED, sink.only().getOutcome());
    }

    @Test void miniatureOnlyAndChangedSelectorCannotBypassTheHistoricalContract() {
        var historical = original("Polite:"); var current = savedBadProposal(historical);
        var work = historicalWork(historical, current);
        var screen = new DefaultCandidateScreen(corrections());
        var onlyCard = new com.learney.contentaudit.revisiondomain.panel.PanelCandidate(PanelCandidateKind.MINI_THEORY, null,
                "Nueva tarjeta", null, null);
        assertFalse(screen.screen(work, Q, onlyCard).isEmpty());
        // A wrong current selector must not become the authority over the historical contract.
        work.getKnowledge().setQuizTemplates(List.of(rewritten(current, "Normal:", "Can you get me some tea?")));
        assertEquals(List.of(), screen.screen(work, Q, candidate(PanelCandidateKind.WHOLE_EXERCISE,
                rewritten(current, "Polite:", "Could you get me some tea?"))));
    }

    @Test void anExplicitClosedFormulaRequiresVisibleInstructionsAndOneAnswerWithoutOptionalPlease() {
        var historical = withParts(original(""), text("Play the radio. Polite:"), cloze("Could you play the radio?"));
        var reference = DidacticContracts.reference("source", 10, Q, historical, true);
        var current = withParts(historical, text("Play the radio. Polite:"), cloze("Could you play the radio?"));
        assertFalse(DidacticContracts.violations(reference, current, null, SentenceMode.REWRITE).isEmpty(),
                "even the unchanged radio needs the newly requested visible formula");
        var good = withParts(current, text("Play the radio. Polite:"), cloze("Could you play the radio?"));
        good.setInstructions(INSTRUCTIONS + " " + reference.operatorInstruction());
        assertEquals(List.of(), DidacticContracts.violations(reference, good, null, SentenceMode.REWRITE));
        var two = withParts(good, text("Play the radio. Polite:"), cloze("Could you play the radio?", "Could you please play the radio?"));
        assertFalse(DidacticContracts.violations(reference, two, null, SentenceMode.REWRITE).isEmpty());
        var please = withParts(good, text("Play the radio. Polite:"), cloze("Could you please play the radio?"));
        assertTrue(DidacticContracts.violations(reference, please, null, SentenceMode.REWRITE).stream().anyMatch(v -> v.contains("please")));
        for (String variant : List.of("Could you play the radio?|Could you turn on the radio?",
                "Could you play the radio?/Could you turn on the radio?", "Could you play the radio?|", "")) {
            var disguised = withParts(good, text("Play the radio. Polite:"), cloze(variant));
            assertFalse(DidacticContracts.violations(reference, disguised, null, SentenceMode.REWRITE).isEmpty(), variant);
        }
        assertNull(DidacticContracts.reference("source", 10, Q, original(""), true).operatorInstruction(), "no inferred Normal by absence");
        assertNull(DidacticContracts.reference("source", 10, Q, historical, false).operatorInstruction(), "reference alone never closes a formula");
    }

    @Test void aClosedFormulaOrderCanEditLocalInstructionsAndMustPassAllNormalPanelChecks() {
        var historical = original("Polite:"); var current = savedBadProposal(historical);
        var reference = DidacticContracts.reference("source", 10, Q, historical, true);
        var work = historicalWork(historical, current);
        work.setDidacticReferences(java.util.Map.of(Q, reference));
        work.setBriefs(List.of(new com.learney.contentaudit.revisiondomain.panel.DidacticRepairBrief(work.getBriefs().get(0).getTask(), reference)));
        var good = rewritten(current, "Polite:", "Could you get me some tea?");
        good.setInstructions(INSTRUCTIONS + " " + reference.operatorInstruction());
        var sink = new RecordingSink();
        panel(new RecordingProposer(script(Q, proposal(candidate(PanelCandidateKind.WHOLE_EXERCISE, good)))), new RulesMeter())
                .run(work, settings(), allowance(1, 1), sink);
        assertEquals(PanelOutcomeKind.ACCEPTED, sink.only().getOutcome());
        assertEquals(good.getInstructions(), sink.only().getAccepted().getQuiz().getInstructions());
    }

    private static TopicWork historicalWork(QuizTemplateEntity historical, QuizTemplateEntity current) {
        TopicWork work = workFor(current);
        var reference = new com.learney.contentaudit.revisiondomain.panel.DidacticReference("previous-pass", 10, Q, historical);
        var task = new com.learney.contentaudit.refinerdomain.RefinementTask("restore-contract",
                com.learney.contentaudit.auditdomain.AuditTarget.QUIZ, Q, "Restaurar contrato",
                com.learney.contentaudit.refinerdomain.DiagnosisKind.QUIZ_INSTRUCTION, 0,
                com.learney.contentaudit.refinerdomain.RefinementTaskStatus.IN_BATCH, null);
        work.setBriefs(List.of(new com.learney.contentaudit.revisiondomain.panel.DidacticRepairBrief(task, reference)));
        work.setDidacticReferences(java.util.Map.of(Q, reference));
        return work;
    }

    private static QuizTemplateEntity original(String prefix) {
        QuizTemplateEntity quiz = quiz(Q, "¿Podrías traerme un poco de té?",
                text((prefix + " I want you to get me some tea.").strip()), cloze("Could you bring me some tea?"), text(""));
        quiz.setInstructions(INSTRUCTIONS);
        quiz.setMiniTheory(ORIGINAL_CARD);
        return quiz;
    }

    private static QuizTemplateEntity rewritten(QuizTemplateEntity original, String prefix, String... answers) {
        return withParts(original, text((prefix + " I want you to get me some tea.").strip()), cloze(answers));
    }

    private static QuizTemplateEntity savedBadProposal(QuizTemplateEntity original) {
        QuizTemplateEntity bad = rewritten(original, "", SIXTEEN_ANSWERS.toArray(String[]::new));
        bad.setMiniTheory("$Could$ expresa mayor cortesía que $Can$ al hacer un pedido. Ambos permiten pedidos corteses; se puede añadir $please$.");
        return bad;
    }

    private static TopicWork workFor(QuizTemplateEntity original) {
        KnowledgeEntity topic = topic("6814dafa7d73e7209a13d3e4", "Pedidos: formal o informal", new ArrayList<>(List.of(original)));
        Finding finding = finding("single-answer", "different-valid-answer", Q, FindingSeverity.BLOCKING,
                FindingResolution.PANEL, "Could you get me some tea?", "Otra respuesta aceptable");
        TopicWork work = work(topic, List.of(brief("request-form-regression", finding, List.of(ExercisePart.WHOLE_EXERCISE, ExercisePart.MINI_THEORY))));
        work.setSentenceMode(SentenceMode.REWRITE);
        return work;
    }

    private static List<String> screen(QuizTemplateEntity original, QuizTemplateEntity proposed) {
        return screen(original, proposed, SentenceMode.REWRITE);
    }

    private static List<String> screen(QuizTemplateEntity original, QuizTemplateEntity proposed, SentenceMode mode) {
        TopicWork work = workFor(original);
        work.setSentenceMode(mode);
        return new DefaultCandidateScreen(corrections()).screen(work, Q, candidate(PanelCandidateKind.WHOLE_EXERCISE, proposed));
    }
}
