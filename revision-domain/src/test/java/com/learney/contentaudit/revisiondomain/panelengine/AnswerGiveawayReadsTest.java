package com.learney.contentaudit.revisiondomain.panelengine;

import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import java.util.EnumSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnswerGiveawayReadsTest {

    @Test
    @DisplayName("should measure with the judge «no regala» every candidate that changes the instructions, the statement, the hint, the answers or the options, whatever its task, and not one that changes only the translation or the mini theory")
    @Tag("FEAT-REGALA")
    @Tag("F-REGALA-R004")
    void shouldMeasureWithTheGiveawayJudgeEveryCandidateThatChangesWhatTheStudentReadsBeforeAnswering() {
        for (ExercisePart part : EnumSet.of(ExercisePart.INSTRUCTIONS, ExercisePart.STATEMENT, ExercisePart.HINT,
                ExercisePart.ANSWERS, ExercisePart.OPTIONS)) {
            assertTrue(AnalyzerReads.readsAny("answer-giveaway", EnumSet.of(part)), part.name());
        }
        assertFalse(AnalyzerReads.readsAny("answer-giveaway", EnumSet.of(ExercisePart.TRANSLATION)));
        assertFalse(AnalyzerReads.readsAny("answer-giveaway", EnumSet.of(ExercisePart.MINI_THEORY)));
        assertFalse(AnalyzerReads.readsAny("answer-giveaway", EnumSet.of(ExercisePart.TOPIC_INSTRUCTIONS)));
    }
}
