package com.learney.contentaudit.revisiondomain.panelengine;

import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.*;
import static org.junit.jupiter.api.Assertions.*;

import com.learney.contentaudit.coursedomain.FormKind;
import com.learney.contentaudit.revisiondomain.panel.ConversionOrder;
import com.learney.contentaudit.revisiondomain.panel.PanelOutcomeKind;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class MultipleChoiceSentencePreservationTest {
    @Test
    @Tag("F-CONVERSION-R003")
    void panelRejectsAConversionThatWouldDropAStoredSentenceVariant() {
        var topic = contracciones();
        var original = topic.getQuizTemplates().get(0);
        topic.setQuizTemplates(List.of(original));
        var sentences = List.of("You don't work at the bank.", "You do not work at the bank.");
        original.setSentences(sentences);
        original.getForm().getSentenceParts().get(1).setOptions(List.of("don't", "do not"));
        var candidate = conversion(original, "don't", "doesn't", "aren't");
        var proposer = new RecordingProposer(script(original.getId(), proposal(candidate),
                original.getId(), proposal(candidate), original.getId(), proposal(candidate)));
        var sink = new RecordingSink();

        panel(proposer, new RulesMeter()).run(work(topic, List.of(),
                new ConversionOrder(CONTRACCIONES, FormKind.MULTIPLE_CHOICE, 3), Map.of(), List.of()),
                settings(), allowance(null, 4), sink);

        assertEquals(PanelOutcomeKind.NO_WINNER, sink.only().getOutcome());
        assertNull(sink.only().getAccepted());
        assertTrue(sink.only().getRejections().stream().flatMap(r -> r.getReasons().stream())
                .anyMatch(reason -> reason.contains("oraciones guardadas")));
        assertEquals(sentences, original.getSentences());
        assertEquals("CLOZE", original.getForm().getKind());
    }
}
