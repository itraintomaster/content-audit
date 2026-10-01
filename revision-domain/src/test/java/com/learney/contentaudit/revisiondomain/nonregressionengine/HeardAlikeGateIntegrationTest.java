package com.learney.contentaudit.revisiondomain.nonregressionengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.revisiondomain.nonregression.AudioRerecordRequest;
import com.learney.contentaudit.revisiondomain.nonregression.ChangeGateVerdict;
import com.learney.contentaudit.revisiondomain.nonregression.ChangeProbe;
import com.learney.contentaudit.revisiondomain.nonregression.SpokenText;
import com.learney.contentaudit.revisiondomain.nonregression.UnitMeasurement;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * F-NOEMP-R004, inv. 1, with what the control of package E over the copy of 29/9 found: the rule of
 * multiple-choice-validity capitalizes the options of 75 exercises of A1, and the gate asked to re-record 62 audios
 * whose recordings, in 61 of them, already said the capitalized sentence. What the audio says changes when the words
 * heard change; capitals and spaces are not heard.
 */
public class HeardAlikeGateIntegrationTest {

    private static final String CHILD = "67fab6d59930102295341e87";
    private static final String CHILD_AUDIO = "A1.01.03.01";

    @Test
    @DisplayName("should ask no re-recording of an exercise whose options a rule only capitalizes, «is he a child?» → «Is he a child?»")
    @Tag("FEAT-NOEMP")
    @Tag("F-NOEMP-R004")
    public void shouldAskNoRerecordingOfAnExerciseWhoseOptionsARuleOnlyCapitalizes() {
        ChangeGateVerdict verdict = check("is he a child?", "Is he a child?");

        assertTrue(verdict.isPasses(), verdict.getReasons().toString());
        assertTrue(verdict.getRerecords().isEmpty(), "capitals are not heard: " + verdict.getRerecords());
        assertTrue(check("Many of  these mobiles are new.", "many of these mobiles are new.").getRerecords().isEmpty(),
                "nor are the spaces");
        assertTrue(check("I don’t surf.", "I don't surf.").getRerecords().isEmpty(),
                "nor the shape of the apostrophe");
    }

    @Test
    @DisplayName("should still ask to re-record what is heard differently: a contraction, a word, the sign that ends the sentence")
    @Tag("FEAT-NOEMP")
    @Tag("F-NOEMP-R004")
    public void shouldStillAskToRerecordWhatIsHeardDifferently() {
        for (String[] change : List.of(new String[] {"He is a child.", "He's a child."},
                new String[] {"Is he a child?", "Is she a child?"},
                new String[] {"Is he a child.", "Is he a child?"})) {
            ChangeGateVerdict verdict = check(change[0], change[1]);

            assertEquals(1, verdict.getRerecords().size(), change[0] + " → " + change[1]);
            AudioRerecordRequest request = verdict.getRerecords().get(0);
            assertEquals(CHILD_AUDIO, request.getAudioKey());
            assertEquals(List.of(change[1]), request.getNewTexts(), "recorded with what the exercise says now");
        }
    }

    private static ChangeGateVerdict check(String before, String after) {
        return new DefaultGateFactory().changeGate().check(new ChangeProbe(List.of(CHILD), false, unit(before),
                unit(after), List.of()));
    }

    private static UnitMeasurement unit(String spoken) {
        return new UnitMeasurement(CefrLevel.A1, List.of(), List.of(), List.of(),
                List.of(new SpokenText(CHILD, CHILD_AUDIO, List.of(spoken))), List.of());
    }
}
