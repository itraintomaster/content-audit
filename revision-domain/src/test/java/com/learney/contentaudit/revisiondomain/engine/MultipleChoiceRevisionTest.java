package com.learney.contentaudit.revisiondomain.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditReportStore;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.CourseRepository;
import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.MilestoneEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceItemEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.RootNodeEntity;
import com.learney.contentaudit.coursedomain.SentenceMode;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.coursedomain.TopicEntity;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.refinerdomain.CorrectionContext;
import com.learney.contentaudit.refinerdomain.CorrectionContextResolver;
import com.learney.contentaudit.refinerdomain.DiagnosisKind;
import com.learney.contentaudit.refinerdomain.QuizInstructionCorrectionContext;
import com.learney.contentaudit.refinerdomain.RefinementPlan;
import com.learney.contentaudit.refinerdomain.RefinementPlanStore;
import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.refinerdomain.RefinementTaskStatus;
import com.learney.contentaudit.revisiondomain.CorrectionContextOverrideParser;
import com.learney.contentaudit.revisiondomain.CourseElementLocator;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.ImpactPreviewStore;
import com.learney.contentaudit.revisiondomain.LemmaAbsenceProposalDeriver;
import com.learney.contentaudit.revisiondomain.LemmaAbsenceQuizCandidate;
import com.learney.contentaudit.revisiondomain.ProposalDecisionOutcome;
import com.learney.contentaudit.revisiondomain.ProposalDecisionOutcomeKind;
import com.learney.contentaudit.revisiondomain.QuizInstructionCorrectionRunStore;
import com.learney.contentaudit.revisiondomain.RevisionArtifact;
import com.learney.contentaudit.revisiondomain.RevisionArtifactStore;
import com.learney.contentaudit.revisiondomain.RevisionEngine;
import com.learney.contentaudit.revisiondomain.RevisionOutcome;
import com.learney.contentaudit.revisiondomain.RevisionOutcomeKind;
import com.learney.contentaudit.revisiondomain.RevisionProposal;
import com.learney.contentaudit.revisiondomain.RevisionValidator;
import com.learney.contentaudit.revisiondomain.RevisionVerdict;
import com.learney.contentaudit.revisiondomain.Reviser;
import com.learney.contentaudit.revisiondomain.candidatecriteria.CandidateAssessor;
import com.learney.contentaudit.revisiondomain.impactpreview.ImpactPreviewComputer;
import com.learney.contentaudit.revisiondomain.preservation.PreservationCheck;
import com.learney.contentaudit.revisiondomain.quizinstruction.CandidateAssessmentUnavailableException;
import com.learney.contentaudit.revisiondomain.quizinstruction.QuizInstructionCorrectionConfig;
import com.learney.contentaudit.revisiondomain.quizinstruction.QuizInstructionCorrectionRunReport;
import com.learney.contentaudit.revisiondomain.quizinstruction.QuizInstructionCorrectionRunRequest;
import com.learney.contentaudit.revisiondomain.quizinstruction.QuizInstructionTaskOutcomeKind;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** revise, the batch run, assess-candidate and approve never correct, convert nor drop a multiple-choice quiz. */
@Tag("multiple-choice")
class MultipleChoiceRevisionTest {

    private static final String PLAN_ID = "plan-mc";
    private static final Path COURSE_PATH = Path.of("db/english-course");

    private static FormEntity multipleChoiceForm() {
        FormEntity form = new FormEntity("MULTIPLE_CHOICE", 1.0, "", "", List.of(
                new SentencePartEntity(SentencePartKind.TEXT, "She", null),
                new SentencePartEntity(SentencePartKind.CLOZE, "", null),
                new SentencePartEntity(SentencePartKind.TEXT, "English.", null)));
        form.setMultipleChoice(new MultipleChoiceEntity("SINGLE", List.of(
                new MultipleChoiceItemEntity("am", 0.0, "am"),
                new MultipleChoiceItemEntity("is", 1.0, "is"))));
        return form;
    }

    private static QuizTemplateEntity quiz(String id, FormEntity form) {
        QuizTemplateEntity quiz = new QuizTemplateEntity();
        quiz.setId(id);
        quiz.setOidId(id);
        quiz.setKind(form.getKind());
        quiz.setKnowledgeId("k1");
        quiz.setTitle("old title");
        quiz.setForm(form);
        quiz.setSentences(List.of("She is English."));
        Map<String, Object> unmodeled = new LinkedHashMap<>();
        unmodeled.put("formCloze", Map.of("kind", "CLOZE"));
        unmodeled.put("instructionsAnteriores", "Elegi la forma de be.");
        quiz.setUnmodeledFields(unmodeled);
        return quiz;
    }

    private static RefinementTask task(String id, String nodeId, DiagnosisKind kind) {
        return new RefinementTask(id, AuditTarget.QUIZ, nodeId, "Be", kind, 1, RefinementTaskStatus.PENDING);
    }

    @Test
    @DisplayName("revise on a multiple-choice quiz stops before any reviser runs, writes nothing but the plan and leaves the task SKIPPED")
    @SuppressWarnings("unchecked")
    void reviseRejectsMultipleChoice() {
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        AuditReportStore auditStore = mock(AuditReportStore.class);
        CorrectionContextResolver<CorrectionContext> resolver = mock(CorrectionContextResolver.class);
        Reviser reviser = mock(Reviser.class);
        RevisionArtifactStore artifactStore = mock(RevisionArtifactStore.class);
        CourseRepository courseRepository = mock(CourseRepository.class);
        CourseElementLocator locator = mock(CourseElementLocator.class);

        RefinementTask task = task("task-1", "mc-1", DiagnosisKind.LEMMA_ABSENCE);
        RefinementPlan plan = new RefinementPlan(PLAN_ID, "audit-mc", Instant.now(), List.of(task));
        AuditReport report = mock(AuditReport.class);
        CourseEntity course = mock(CourseEntity.class);
        when(planStore.load(PLAN_ID)).thenReturn(Optional.of(plan));
        when(auditStore.load("audit-mc")).thenReturn(Optional.of(report));
        when(resolver.resolve(report, task)).thenReturn(Optional.of(mock(CorrectionContext.class)));
        when(courseRepository.load(COURSE_PATH)).thenReturn(course);
        when(locator.snapshot(course, AuditTarget.QUIZ, "mc-1")).thenReturn(Optional.of(
                new CourseElementSnapshot(AuditTarget.QUIZ, "mc-1", quiz("mc-1", multipleChoiceForm()), null)));

        DefaultRevisionEngine engine = new DefaultRevisionEngine(planStore, auditStore, resolver, reviser,
                mock(RevisionValidator.class), artifactStore, courseRepository, locator,
                mock(ImpactPreviewComputer.class), mock(ImpactPreviewStore.class),
                mock(CorrectionContextOverrideParser.class));

        RevisionOutcome outcome = engine.revise(PLAN_ID, "task-1", COURSE_PATH, null);

        assertEquals(RevisionOutcomeKind.MULTIPLE_CHOICE_UNSUPPORTED, outcome.getKind());
        verifyNoInteractions(reviser);
        verify(courseRepository, never()).save(any(), any());
        verify(artifactStore, never()).save(any());
        ArgumentCaptor<RefinementPlan> saved = ArgumentCaptor.forClass(RefinementPlan.class);
        verify(planStore).save(saved.capture());
        assertEquals(RefinementTaskStatus.SKIPPED, saved.getValue().getTasks().get(0).getStatus());
    }

    @Test
    @DisplayName("The batch run counts a multiple-choice task apart and its own end-of-run save keeps it SKIPPED")
    void batchRunKeepsMultipleChoiceSkipped() {
        RevisionEngine engine = mock(RevisionEngine.class);
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        QuizInstructionCorrectionRunStore runStore = mock(QuizInstructionCorrectionRunStore.class);
        RefinementTask multipleChoice = task("t-mc", "mc-1", DiagnosisKind.QUIZ_INSTRUCTION);
        RefinementTask stale = task("t-stale", "cloze-1", DiagnosisKind.QUIZ_INSTRUCTION);
        when(planStore.load(PLAN_ID)).thenReturn(Optional.of(
                new RefinementPlan(PLAN_ID, "audit-mc", Instant.now(), List.of(multipleChoice, stale))));
        when(engine.revise(eq(PLAN_ID), eq("t-mc"), any(), any())).thenReturn(
                new RevisionOutcome(RevisionOutcomeKind.MULTIPLE_CHOICE_UNSUPPORTED, null, "es de opcion multiple"));
        when(engine.revise(eq(PLAN_ID), eq("t-stale"), any(), any())).thenReturn(
                new RevisionOutcome(RevisionOutcomeKind.DIAGNOSIS_NOT_SUSTAINED, null, "ya cumple"));
        when(runStore.save(any())).thenReturn("run-1");

        QuizInstructionCorrectionRunReport report = new DefaultQuizInstructionCorrectionRunner(
                engine, planStore, runStore, new QuizInstructionCorrectionConfig(3, 20))
                .run(new QuizInstructionCorrectionRunRequest(PLAN_ID, COURSE_PATH, 5));

        assertEquals(1, report.getMultipleChoiceUnsupported());
        assertEquals(0, report.getFailed());
        assertEquals(QuizInstructionTaskOutcomeKind.MULTIPLE_CHOICE_UNSUPPORTED, report.getOutcomes().get(0).getKind());
        ArgumentCaptor<RefinementPlan> saved = ArgumentCaptor.forClass(RefinementPlan.class);
        verify(planStore).save(saved.capture());
        assertEquals(RefinementTaskStatus.SKIPPED, saved.getValue().getTasks().get(0).getStatus(),
                "the runner's own save must not put the multiple-choice task back to PENDING");
        assertEquals(RefinementTaskStatus.STALE, saved.getValue().getTasks().get(1).getStatus());
    }

    @Test
    @DisplayName("assess-candidate reports the consultation unavailable for a multiple-choice quiz and never judges a candidate")
    @SuppressWarnings("unchecked")
    void assessCandidateRefusesMultipleChoice() {
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        AuditReportStore auditStore = mock(AuditReportStore.class);
        CorrectionContextResolver<CorrectionContext> resolver = mock(CorrectionContextResolver.class);
        CourseRepository courseRepository = mock(CourseRepository.class);
        CourseElementLocator locator = mock(CourseElementLocator.class);
        CandidateAssessor assessor = mock(CandidateAssessor.class);
        LemmaAbsenceProposalDeriver deriver = mock(LemmaAbsenceProposalDeriver.class);

        RefinementTask task = task("task-1", "mc-1", DiagnosisKind.QUIZ_INSTRUCTION);
        AuditReport report = mock(AuditReport.class);
        CourseEntity course = mock(CourseEntity.class);
        when(planStore.load(PLAN_ID)).thenReturn(Optional.of(
                new RefinementPlan(PLAN_ID, "audit-mc", Instant.now(), List.of(task))));
        when(auditStore.load("audit-mc")).thenReturn(Optional.of(report));
        when(resolver.resolve(report, task)).thenReturn(Optional.of(new QuizInstructionCorrectionContext()));
        when(courseRepository.load(COURSE_PATH)).thenReturn(course);
        when(locator.snapshot(course, AuditTarget.QUIZ, "mc-1")).thenReturn(Optional.of(
                new CourseElementSnapshot(AuditTarget.QUIZ, "mc-1", quiz("mc-1", multipleChoiceForm()), null)));

        DefaultQuizInstructionCandidateAssessor candidateAssessor = new DefaultQuizInstructionCandidateAssessor(
                assessor, deriver, planStore, auditStore, resolver, courseRepository, locator);

        CandidateAssessmentUnavailableException refused = assertThrows(CandidateAssessmentUnavailableException.class,
                () -> candidateAssessor.assessTask(PLAN_ID, "task-1", COURSE_PATH,
                        "She ____ [was] English.", "Ella era inglesa."));
        assertTrue(refused.getReason().contains("opcion multiple"), refused.getReason());
        verifyNoInteractions(assessor, deriver);
    }

    @Test
    @DisplayName("approve refuses a proposal on a quiz that is multiple choice in today's course, even if the proposal saw it as CLOZE")
    void approveRefusesMultipleChoiceTarget() {
        RevisionArtifactStore artifactStore = mock(RevisionArtifactStore.class);
        CourseRepository courseRepository = mock(CourseRepository.class);
        CourseElementLocator locator = mock(CourseElementLocator.class);
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        PreservationCheck preservationCheck = mock(PreservationCheck.class);

        FormEntity oldCloze = new FormEntity("CLOZE", 1.0, "", "", List.of(
                new SentencePartEntity(SentencePartKind.TEXT, "She", null),
                new SentencePartEntity(SentencePartKind.CLOZE, "", List.of("was")),
                new SentencePartEntity(SentencePartKind.TEXT, "English.", null)));
        CourseElementSnapshot after = new CourseElementSnapshot(AuditTarget.QUIZ, "mc-1", quiz("mc-1", oldCloze), null);
        RevisionProposal proposal = new RevisionProposal("p-1", "task-1", PLAN_ID, "audit-mc",
                DiagnosisKind.LEMMA_ABSENCE, AuditTarget.QUIZ, "mc-1", after, after, "old", "lemma-absence-llm",
                Instant.now(), null, null, null);
        RevisionArtifact artifact = new RevisionArtifact(proposal, RevisionVerdict.PENDING_APPROVAL, null,
                RevisionOutcomeKind.PENDING_APPROVAL_PERSISTED, null, null, null, null);
        CourseEntity course = mock(CourseEntity.class);
        when(artifactStore.findByProposalId(eq("p-1"), any())).thenReturn(Optional.of(artifact));
        when(planStore.load(PLAN_ID)).thenReturn(Optional.of(
                new RefinementPlan(PLAN_ID, "audit-mc", Instant.now(), List.of())));
        when(courseRepository.load(COURSE_PATH)).thenReturn(course);
        when(locator.snapshot(course, AuditTarget.QUIZ, "mc-1")).thenReturn(Optional.of(
                new CourseElementSnapshot(AuditTarget.QUIZ, "mc-1", quiz("mc-1", multipleChoiceForm()), null)));

        ProposalDecisionOutcome outcome = new DefaultProposalDecisionService(
                artifactStore, courseRepository, locator, planStore, preservationCheck)
                .approve("p-1", Optional.empty(), Optional.empty(), COURSE_PATH);

        assertEquals(ProposalDecisionOutcomeKind.MULTIPLE_CHOICE_UNSUPPORTED, outcome.getKind());
        verify(courseRepository, never()).save(any(), any());
        verify(artifactStore, never()).save(any());
        verify(locator, never()).replace(any(), any());
    }

    @Test
    @DisplayName("Aligning quiz titles changes only the title: a multiple-choice sibling keeps its options and its unmodeled fields")
    void alignQuizTitlesKeepsEverythingButTheTitle() {
        QuizTemplateEntity multipleChoice = quiz("mc-1", multipleChoiceForm());
        KnowledgeEntity knowledge = new KnowledgeEntity();
        knowledge.setId("k1");
        knowledge.setLabel("Afirmativas con $be$");
        knowledge.setQuizTemplates(List.of(multipleChoice));
        TopicEntity topic = new TopicEntity();
        topic.setKnowledges(List.of(knowledge));
        MilestoneEntity milestone = new MilestoneEntity();
        milestone.setTopics(List.of(topic));
        RootNodeEntity root = new RootNodeEntity();
        root.setMilestones(List.of(milestone));
        CourseEntity course = new CourseEntity();
        course.setRoot(root);

        CourseEntity aligned = new DefaultCourseElementLocator().alignQuizTitles(course, "k1");

        QuizTemplateEntity alignedQuiz = aligned.getRoot().getMilestones().get(0).getTopics().get(0)
                .getKnowledges().get(0).getQuizTemplates().get(0);
        assertEquals("Afirmativas con $be$", alignedQuiz.getTitle());
        assertSame(multipleChoice.getForm(), alignedQuiz.getForm());
        assertEquals(multipleChoice.getUnmodeledFields(), alignedQuiz.getUnmodeledFields());
        alignedQuiz.setTitle(multipleChoice.getTitle());
        assertEquals(multipleChoice, alignedQuiz, "nothing but the title may change");
    }

    @Test
    @DisplayName("A lexical correction of a CLOZE quiz keeps the fields the model does not interpret")
    void lemmaAbsenceDerivationKeepsUnmodeledFields() {
        FormEntity cloze = new FormEntity("CLOZE", 1.0, "", "", List.of(
                new SentencePartEntity(SentencePartKind.TEXT, "She", null),
                new SentencePartEntity(SentencePartKind.CLOZE, "", List.of("is")),
                new SentencePartEntity(SentencePartKind.TEXT, "English.", null)));
        Map<String, Object> formExtra = new LinkedHashMap<>();
        formExtra.put("futureKey", "kept");
        cloze.setUnmodeledFields(formExtra);
        QuizTemplateEntity before = quiz("cloze-1", cloze);
        CourseElementSnapshot snapshot = new CourseElementSnapshot(AuditTarget.QUIZ, "cloze-1", before, null);

        CourseElementSnapshot after = new DefaultLemmaAbsenceProposalDeriver(DefaultQuizSentenceConverter.create())
                .derive(snapshot, new LemmaAbsenceQuizCandidate("She ____ [is] Irish.", "Ella es irlandesa."),
                        SentenceMode.FILL);

        QuizTemplateEntity afterQuiz = after.getQuiz();
        assertEquals(before.getUnmodeledFields(), afterQuiz.getUnmodeledFields());
        assertEquals(formExtra, afterQuiz.getForm().getUnmodeledFields());
        assertEquals(List.of("She is Irish."), afterQuiz.getSentences());
        assertEquals("Ella es irlandesa.", afterQuiz.getTranslation());
    }
}
