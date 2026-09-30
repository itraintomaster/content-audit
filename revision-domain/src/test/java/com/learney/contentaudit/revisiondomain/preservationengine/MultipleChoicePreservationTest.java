package com.learney.contentaudit.revisiondomain.preservationengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.MilestoneEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceItemEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.RootNodeEntity;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.coursedomain.TopicEntity;
import com.learney.contentaudit.refinerdomain.DiagnosisKind;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.RevisionArtifact;
import com.learney.contentaudit.revisiondomain.RevisionArtifactStore;
import com.learney.contentaudit.revisiondomain.RevisionProposal;
import com.learney.contentaudit.revisiondomain.RevisionVerdict;
import com.learney.contentaudit.revisiondomain.preservation.CorrectionScope;
import com.learney.contentaudit.revisiondomain.preservation.PreservationViolation;
import com.learney.contentaudit.revisiondomain.preservation.RepairReport;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("multiple-choice")
class MultipleChoicePreservationTest {

    private static FormEntity form(String kind, List<String> gapOptions) {
        return new FormEntity(kind, 1.0, "", "", new ArrayList<>(List.of(
                new SentencePartEntity(SentencePartKind.TEXT, "She", null),
                new SentencePartEntity(SentencePartKind.CLOZE, "", gapOptions),
                new SentencePartEntity(SentencePartKind.TEXT, "English.", null))));
    }

    private static MultipleChoiceEntity choices(String correct) {
        List<MultipleChoiceItemEntity> items = new ArrayList<>();
        for (String label : List.of("am", "is")) {
            items.add(new MultipleChoiceItemEntity(label, label.equals(correct) ? 1.0 : 0.0, label));
        }
        return new MultipleChoiceEntity("SINGLE", items);
    }

    private static QuizTemplateEntity quiz(FormEntity form, Map<String, Object> unmodeled) {
        QuizTemplateEntity quiz = new QuizTemplateEntity();
        quiz.setId("quiz-1");
        quiz.setKind(form.getKind());
        quiz.setKnowledgeId("k1");
        quiz.setForm(form);
        quiz.setSentences(List.of("She is English."));
        quiz.setUnmodeledFields(unmodeled);
        return quiz;
    }

    private static Map<String, Object> backups() {
        Map<String, Object> unmodeled = new LinkedHashMap<>();
        unmodeled.put("formCloze", Map.of("kind", "CLOZE"));
        unmodeled.put("instructionsAnteriores", "Elegi la forma de be.");
        return unmodeled;
    }

    private static CourseEntity courseWith(QuizTemplateEntity quiz) {
        KnowledgeEntity knowledge = new KnowledgeEntity();
        knowledge.setId("k1");
        knowledge.setQuizTemplates(new ArrayList<>(List.of(quiz)));
        TopicEntity topic = new TopicEntity();
        topic.setKnowledges(List.of(knowledge));
        MilestoneEntity milestone = new MilestoneEntity();
        milestone.setTopics(List.of(topic));
        RootNodeEntity root = new RootNodeEntity();
        root.setMilestones(List.of(milestone));
        CourseEntity course = new CourseEntity();
        course.setRoot(root);
        return course;
    }

    private static DefaultPreservationRepair repairWithSnapshot(QuizTemplateEntity intact) {
        CourseElementSnapshot before = new CourseElementSnapshot(AuditTarget.QUIZ, "quiz-1", intact, null);
        RevisionProposal proposal = new RevisionProposal("p-1", "t-1", "plan", "audit",
                DiagnosisKind.QUIZ_INSTRUCTION, AuditTarget.QUIZ, "quiz-1", before, before, "old", "qicor",
                Instant.parse("2026-08-01T00:00:00Z"), null, null, null);
        RevisionArtifactStore store = mock(RevisionArtifactStore.class);
        when(store.list()).thenReturn(List.of(new RevisionArtifact(proposal, RevisionVerdict.APPROVED, null,
                null, Instant.parse("2026-08-01T00:05:00Z"), null, null, null)));
        CorrectionScope scope = mock(CorrectionScope.class);
        when(scope.changeableFields(DiagnosisKind.QUIZ_INSTRUCTION, AuditTarget.QUIZ)).thenReturn(Set.of());
        return new DefaultPreservationRepair(store, scope);
    }

    @Test
    @DisplayName("A correction that drops the unmodeled fields or changes the multiple-choice options is a preservation violation")
    void droppingNewFieldsIsAViolation() {
        FormEntity mc = form("MULTIPLE_CHOICE", null);
        mc.setMultipleChoice(choices("is"));
        QuizTemplateEntity before = quiz(mc, backups());

        FormEntity changed = new FormEntity(mc);
        changed.setMultipleChoice(choices("am"));
        QuizTemplateEntity after = quiz(changed, null);

        List<PreservationViolation> out = new ArrayList<>();
        CourseElementFieldDiff.diffQuiz(before, after, Set.of(), out);

        List<String> paths = out.stream().map(PreservationViolation::getPath).toList();
        assertTrue(paths.contains("unmodeledFields"), paths.toString());
        assertTrue(paths.contains("form.multipleChoice"), paths.toString());
    }

    @Test
    @DisplayName("A reference snapshot recorded before the model knew these fields is not taken as their absence")
    void unknownInReferenceIsNotAViolation() {
        QuizTemplateEntity oldSnapshot = quiz(form("CLOZE", List.of("is")), null);
        QuizTemplateEntity current = quiz(form("CLOZE", List.of("is")), backups());

        List<PreservationViolation> out = new ArrayList<>();
        CourseElementFieldDiff.diffQuiz(oldSnapshot, current, Set.of(), out);

        assertTrue(out.isEmpty(), out.toString());
    }

    @Test
    @DisplayName("repair never turns a multiple-choice quiz back into the CLOZE an old snapshot recorded")
    void repairLeavesMultipleChoiceAlone() {
        QuizTemplateEntity clozeEraSnapshot = quiz(form("CLOZE", List.of("is")), null);
        FormEntity mc = form("MULTIPLE_CHOICE", null);
        mc.setMultipleChoice(choices("is"));
        QuizTemplateEntity current = quiz(mc, backups());

        RepairReport report = repairWithSnapshot(clozeEraSnapshot).repair(courseWith(current));

        assertEquals(0, report.getElementsRepaired());
        assertEquals("MULTIPLE_CHOICE", current.getForm().getKind());
        assertNull(current.getForm().getSentenceParts().get(1).getOptions());
        assertEquals(choices("is"), current.getForm().getMultipleChoice());
        assertEquals(backups(), current.getUnmodeledFields());
    }

    @Test
    @DisplayName("repair does not wipe the unmodeled fields of a CLOZE quiz whose snapshot predates them")
    void repairKeepsUnmodeledFieldsOfCloze() {
        QuizTemplateEntity oldSnapshot = quiz(form("CLOZE", List.of("is")), null);
        QuizTemplateEntity current = quiz(form("CLOZE", List.of("is")), backups());

        repairWithSnapshot(oldSnapshot).repair(courseWith(current));

        assertEquals(backups(), current.getUnmodeledFields());
    }

    @Test
    @DisplayName("A multiple-choice quiz that lost its options is reported as unrepairable, never filled in")
    void multipleChoiceWithoutOptionsIsUnrepairable() {
        QuizTemplateEntity lost = quiz(form("MULTIPLE_CHOICE", null), null);
        RevisionArtifactStore store = mock(RevisionArtifactStore.class);
        when(store.list()).thenReturn(List.of());

        RepairReport report = new DefaultPreservationRepair(store, mock(CorrectionScope.class))
                .inspect(courseWith(lost));

        assertEquals(List.of("quiz-1"), report.getUnrepairable());
    }
}
