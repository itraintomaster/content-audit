package com.learney.contentaudit.revisiondomain.panelengine;

import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.refinerdomain.topicwords.TopicWordLights;
import com.learney.contentaudit.refinerdomain.topicwords.TopicWordLightsQuery;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;

class PanelVocabularyContextsTest {
    @Test
    void opensFromComposedCourseAndMapsOnlyAcceptedTopicWithoutRunningAnAudit() {
        AuditEngine engine = mock(AuditEngine.class);
        CourseMapper mapper = mock(CourseMapper.class);
        CourseEntity composed = new CourseEntity();
        AuditableCourse mapped = new AuditableCourse(List.of());
        when(mapper.map(composed)).thenReturn(mapped);
        var work = work(afirmativas(), List.of());
        var accepted = afirmativas();
        AuditableKnowledge changed = new AuditableKnowledge();
        when(mapper.map(argThat(c -> c != null && c != composed))).thenReturn(new AuditableCourse(List.of(
                new AuditableMilestone(List.of(new AuditableTopic(List.of(changed), "t", "t", null)), "m", "A1", null))));
        AtomicInteger updates = new AtomicInteger();
        AuditReport provisional = new AuditReport(new AuditNode());
        WorkingVocabulary working = new WorkingVocabulary() {
            @Override public void replaceKnowledge(AuditableKnowledge knowledge) {
                assertSame(changed, knowledge);
                updates.incrementAndGet();
            }
            @Override public <T> T query(BiFunction<AuditReport, Long, T> callback) {
                return callback.apply(provisional, (long) updates.get());
            }
        };
        when(engine.openWorkingVocabulary(mapped)).thenReturn(working);
        TopicWordLightsQuery query = (report, auditId, knowledge, criteria) -> {
            assertSame(provisional, report);
            assertEquals("base-audit", auditId);
            assertEquals(work.getKnowledge().getId(), knowledge);
            return new TopicWordLights();
        };

        var context = PanelVocabularyContexts.open(engine, mapper, composed, query, "base-audit");
        TopicWordLights first = context.lightsFor(work);
        context.accepted(work, accepted);
        TopicWordLights second = context.lightsFor(work);
        assertEquals(0L, first.getWorkingVersion());
        assertEquals(1L, second.getWorkingVersion());
        verify(engine).openWorkingVocabulary(mapped);
        verifyNoMoreInteractions(engine); // Not even a partial runAudit per accepted proposal.
        var captured = org.mockito.ArgumentCaptor.forClass(CourseEntity.class);
        verify(mapper, times(2)).map(captured.capture());
        assertSame(composed, captured.getAllValues().get(0));
        assertEquals(List.of(accepted), captured.getAllValues().get(1).getRoot().getMilestones().get(0)
                .getTopics().get(0).getKnowledges());
    }
}
