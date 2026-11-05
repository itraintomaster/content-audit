package com.learney.contentaudit.revisiondomain.panelengine;

import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.refinerdomain.errortask.ErrorTaskBrief;
import com.learney.contentaudit.revisiondomain.nonregression.UnitMeasurement;
import com.learney.contentaudit.revisiondomain.panel.DidacticRepairBrief;
import java.util.ArrayList;
import java.util.List;

/**
 * F-MESA-R033: with a medium or heavy problem in the card, the panel touches only the card. A task of the card is one
 * that may touch only the mini theory of its exercise.
 */
final class TheCard {

    /** Why a light task of another part is not tried (inv. 1). */
    static final String ONLY_THE_CARD = "el problema está en la tarjeta: la mesa toca sólo la tarjeta";

    private TheCard() {
    }

    static boolean ofTheCard(ErrorTaskBrief brief) {
        return brief != null && !(brief instanceof DidacticRepairBrief) && brief.getEditScope() != null
                && List.of(ExercisePart.MINI_THEORY).equals(brief.getEditScope());
    }

    /**
     * Inv. 1: the light tasks of other parts of an exercise whose card has a medium or heavy task where it starts and
     * nothing medium or heavy left outside it; none otherwise. A card with only light problems does not outweigh a light
     * problem elsewhere: that is how «She ___ (sport) hats.» lost the change of its verb (C2 of A1, 4/11). An explicit
     * repair is never light.
     */
    static List<ErrorTaskBrief> lightOutside(List<ErrorTaskBrief> tasks, UnitMeasurement start) {
        if (tasks.stream().noneMatch(brief -> ofTheCard(brief) && !DefaultPanelDecisionScript.light(brief, start))) {
            return List.of();
        }
        List<ErrorTaskBrief> outside = new ArrayList<>();
        for (ErrorTaskBrief brief : tasks) {
            if (ofTheCard(brief)) {
                continue;
            }
            if (brief instanceof DidacticRepairBrief || !DefaultPanelDecisionScript.light(brief, start)) {
                return List.of();
            }
            outside.add(brief);
        }
        return outside;
    }

    /** Inv. 2: whether the proposer also delivers a candidate of only the card. */
    static boolean apart(List<ErrorTaskBrief> open) {
        return open.stream().anyMatch(TheCard::ofTheCard) && !open.stream().allMatch(TheCard::ofTheCard);
    }
}
