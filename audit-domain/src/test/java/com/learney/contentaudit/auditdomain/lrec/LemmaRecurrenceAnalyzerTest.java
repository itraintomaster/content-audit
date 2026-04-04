package com.learney.contentaudit.auditdomain.lrec;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.learney.contentaudit.auditdomain.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LemmaRecurrenceAnalyzerTest {

    @Mock private ContentWordFilter contentWordFilter;
    @Mock private LemmaRecurrenceConfig config;
    @Mock private IntervalCalculator intervalCalculator;
    @Mock private ExposureClassifier exposureClassifier;
    @InjectMocks private LemmaRecurrenceAnalyzer sut;

    private AuditNode makeNode(AuditTarget target, AuditableEntity entity) {
        AuditNode node = new AuditNode();
        node.setTarget(target);
        node.setEntity(entity);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        return node;
    }

    @Test
    @DisplayName("Given a LemmaRecurrenceAnalyzer, when getName is called, then returns lemma-recurrence")
    @Tag("F-LREC") @Tag("F-LREC-R015")
    public void givenALemmaRecurrenceAnalyzerWhenGetNameIsCalledThenReturnsLemmarecurrence() {
        assertEquals("lemma-recurrence", sut.getName());
    }

    @Test
    @DisplayName("Given a LemmaRecurrenceAnalyzer, when getTarget is called, then returns COURSE")
    @Tag("F-LREC") @Tag("F-LREC-R013")
    public void givenALemmaRecurrenceAnalyzerWhenGetTargetIsCalledThenReturnsCOURSE() {
        assertEquals(AuditTarget.COURSE, sut.getTarget());
    }

    @Test
    @DisplayName("Given no quizzes have been processed, when onCourseComplete is called, then score is 0")
    @Tag("F-LREC")
    public void givenNoQuizzesHaveBeenProcessedWhenOnCourseCompleteIsCalledThenScoreIsZero() {
        AuditNode root = makeNode(AuditTarget.COURSE, null);
        sut.onCourseComplete(root);
        assertEquals(0.0, root.getScores().getOrDefault("lemma-recurrence", 0.0));
    }

    @Test
    @DisplayName("Given an AuditableKnowledge, when onKnowledge is called, then completes without error")
    public void givenAnAuditableKnowledgeWhenOnKnowledgeIsCalledThenCompletesWithoutError() {
        AuditableKnowledge k = new AuditableKnowledge(null, "t", "i", true, "k1", "l", "c");
        assertDoesNotThrow(() -> sut.onKnowledge(makeNode(AuditTarget.KNOWLEDGE, k)));
    }

    @Test
    @DisplayName("Given an AuditableTopic, when onTopic is called, then completes without error")
    public void givenAnAuditableTopicWhenOnTopicIsCalledThenCompletesWithoutError() {
        AuditableTopic t = new AuditableTopic(null, "t1", "l", "c");
        assertDoesNotThrow(() -> sut.onTopic(makeNode(AuditTarget.TOPIC, t)));
    }

    @Test
    @DisplayName("Given an AuditableMilestone, when onMilestone is called, then completes without error")
    public void givenAnAuditableMilestoneWhenOnMilestoneIsCalledThenCompletesWithoutError() {
        AuditableMilestone m = new AuditableMilestone(null, "m1", "A1", "c");
        assertDoesNotThrow(() -> sut.onMilestone(makeNode(AuditTarget.MILESTONE, m)));
    }
}
