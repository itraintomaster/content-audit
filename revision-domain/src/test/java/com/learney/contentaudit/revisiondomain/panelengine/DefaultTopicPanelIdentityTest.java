package com.learney.contentaudit.revisiondomain.panelengine;

import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.revisiondomain.panel.PanelCandidateKind;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.List;

class DefaultTopicPanelIdentityTest {
    @Test
    @Tag("F-MESA-R003")
    void attachesPersistentIdsInViewOrderEvenWhenTwoQuizViewsAreIdentical() {
        var topic = afirmativas();
        var first = TopicState.copy(exercise(topic, VEGETABLES));
        first.setId("q-first");
        var second = TopicState.copy(first);
        second.setId("q-second");
        topic.setQuizTemplates(List.of(first, second));
        var issue = finding("translation-sense", "same-meaning", "q-second", FindingSeverity.HIGH,
                FindingResolution.PANEL, null, "la traducción no coincide");
        var fixed = withTranslation(second, "Traducción corregida.");
        var proposer = new RecordingProposer(script("q-second",
                proposal(candidate(PanelCandidateKind.TRANSLATION_ONLY, fixed))));
        var meter = new RulesMeter().rule("translation-sense", (knowledge, quiz) ->
                "q-second".equals(quiz.getId()) && !"Traducción corregida.".equals(quiz.getTranslation())
                        ? List.of(issue) : RulesMeter.none());

        panel(proposer, meter).run(work(topic, List.of(brief("t-translation", issue))),
                settings(), allowance(null, 2), new RecordingSink());

        assertEquals(1, proposer.requests.size());
        var request = proposer.requests.get(0);
        assertEquals("q-second", request.getQuizId());
        assertEquals(List.of("q-first", "q-second"), request.getTopicQuizIds());
        assertEquals(request.getTopicView().get(0), request.getTopicView().get(1),
                "identity travels separately from identical student content");
    }
}
