package com.learney.contentaudit.revisiondomain.panelengine;

import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.refinerdomain.RefinementTaskStatus;
import com.learney.contentaudit.refinerdomain.errortask.ErrorTaskBrief;
import com.learney.contentaudit.revisiondomain.nonregression.UnitMeasurement;
import com.learney.contentaudit.revisiondomain.panel.DidacticReference;
import com.learney.contentaudit.revisiondomain.panel.DidacticRepairBrief;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** F-MESA-R033: which tasks are of the card, which light ones go aside and when the card goes apart. */
class TheCardTest {
    private static final String Q = "67fab6d599301022953426d6";

    @Test
    @DisplayName("should leave aside only light tasks of other parts, and only while nothing medium or heavy is outside the card")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R033")
    void shouldLeaveAsideOnlyLightTasksOfOtherPartsWhileNothingMediumIsOutsideTheCard() {
        ErrorTaskBrief card = brief("t-card", finding("mini-theory", "insufficient-for-quiz", Q, FindingSeverity.HIGH));
        ErrorTaskBrief length = brief("t-length", finding("sentence-length", "longer-than-goal", Q, FindingSeverity.LOW),
                List.of(ExercisePart.STATEMENT, ExercisePart.ANSWERS));
        ErrorTaskBrief form = brief("t-form", finding("answer-completeness", "form-request", Q, FindingSeverity.LOW),
                List.of(ExercisePart.ANSWERS, ExercisePart.INSTRUCTIONS, ExercisePart.MINI_THEORY));
        ErrorTaskBrief giveaway = brief("t-giveaway", finding("answer-giveaway", "gives-away-the-decision", Q,
                FindingSeverity.MEDIUM), List.of(ExercisePart.INSTRUCTIONS, ExercisePart.STATEMENT));

        assertEquals(List.of(length, form), TheCard.lightOutside(List.of(card, length, form), new UnitMeasurement()),
                "a form request also lets touch the card, but it is not a task of the card");
        assertEquals(List.of(), TheCard.lightOutside(List.of(card, length, giveaway), new UnitMeasurement()),
                "a medium problem outside the card keeps every task");
        assertEquals(List.of(), TheCard.lightOutside(List.of(length, form), new UnitMeasurement()),
                "without a task of the card nothing goes aside");
    }

    @Test
    @DisplayName("should take a task light by the heaviest of its findings where it starts, not by how it was found")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R033")
    void shouldTakeATaskLightByItsHeaviestFindingWhereItStarts() {
        ErrorTaskBrief card = brief("t-card", finding("mini-theory", "insufficient-for-quiz", Q, FindingSeverity.HIGH));
        Finding wasLight = finding("a1-didactic", "a1-example-quality", Q, FindingSeverity.LOW);
        ErrorTaskBrief didactic = brief("t-didactic", wasLight, List.of(ExercisePart.STATEMENT));
        UnitMeasurement start = new UnitMeasurement();
        start.setFindings(new ArrayList<>(List.of(finding("a1-didactic", "a1-example-quality", Q, FindingSeverity.MEDIUM))));

        assertEquals(List.of(), TheCard.lightOutside(List.of(card, didactic), start), "it starts medium");
    }

    @Test
    @DisplayName("should never leave aside an explicit repair of a didactic contract")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R033")
    void shouldNeverLeaveAsideAnExplicitRepair() {
        // Inv. 3: a contract José asked to restore is an order, not a light finding.
        ErrorTaskBrief card = brief("t-card", finding("mini-theory", "insufficient-for-quiz", Q, FindingSeverity.HIGH));
        RefinementTask task = new RefinementTask("t-repair", AuditTarget.QUIZ, Q, "Restaurar", null, 0,
                RefinementTaskStatus.IN_BATCH, null);
        DidacticRepairBrief repair = new DidacticRepairBrief(task, new DidacticReference("op", 3, Q, null));

        assertFalse(TheCard.ofTheCard(repair));
        assertEquals(List.of(), TheCard.lightOutside(List.of(card, repair), new UnitMeasurement()));
    }

    @Test
    @DisplayName("should ask the card apart only when the card and another part both have tasks")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R033")
    void shouldAskTheCardApartOnlyWhenTheCardAndAnotherPartBothHaveTasks() {
        ErrorTaskBrief card = brief("t-card", finding("mini-theory", "insufficient-for-quiz", Q, FindingSeverity.HIGH));
        ErrorTaskBrief highlight = brief("t-highlight", finding("mini-theory", "missing-english-highlight", Q,
                FindingSeverity.LOW));
        ErrorTaskBrief giveaway = brief("t-giveaway", finding("answer-giveaway", "gives-away-the-decision", Q,
                FindingSeverity.MEDIUM), List.of(ExercisePart.INSTRUCTIONS, ExercisePart.STATEMENT));

        assertTrue(TheCard.apart(List.of(card, highlight, giveaway)));
        assertFalse(TheCard.apart(List.of(card, highlight)), "everything is of the card");
        assertFalse(TheCard.apart(List.of(giveaway)), "nothing is of the card");
    }

    private static Finding finding(String analyzer, String rule, String quizId, FindingSeverity severity) {
        return MesaFixture.finding(analyzer, rule, quizId, severity, FindingResolution.PANEL, null, rule);
    }
}
