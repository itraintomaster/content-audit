package com.learney.contentaudit.auditcli.commands;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditinfrastructure.FileSystemRevisionArtifactStore;
import com.learney.contentaudit.auditinfrastructure.batchstore.FileSystemBatchStore;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.RevisionArtifact;
import com.learney.contentaudit.revisiondomain.RevisionProposal;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.problems.ContentProblem;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FEAT-GUARDA: what the dashboard asks the CLI when it saves -- a decision with {@code --changed-only}, a case with
 * {@code problem annotate}, its undo with {@code --with-decisions} -- reads only the exercises it names and answers only
 * what it changed. A campaign of two exercises, «quiz» and «other», and a course for the problems.
 */
@Tag("FEAT-GUARDA")
class SavingWithoutRereadingCmdTest {
    @TempDir Path root;
    Path work, course;
    final ByteArrayOutputStream output = new ByteArrayOutputStream(), errors = new ByteArrayOutputStream();

    @BeforeEach void campaign() throws Exception {
        work = Files.createDirectory(root.resolve("campaign"));
        course = Files.createDirectory(root.resolve("course"));
        var batch = new CorrectionBatch();
        batch.setId("batch");
        batch.setPlanId("plan");
        batch.setStatus(BatchStatus.OPEN);
        batch.setContentVersion(1);
        batch.setChanges(List.of(change("change", "quiz"), change("other-change", "other")));
        batch.setQuizProposals(List.of(new BatchProposalRef("quiz", "proposal"),
                new BatchProposalRef("other", "other-proposal")));
        proposal("quiz", "proposal");
        proposal("other", "other-proposal");
        new FileSystemBatchStore(work).save(batch);
    }

    @Test
    @Tag("F-GUARDA-R002")
    @DisplayName("should answer a decision with --changed-only with the batch, its saved decisions and only the decided correction, as the whole review shows it")
    void aDecisionWithChangedOnlyAnswersOnlyTheDecidedCorrection() {
        var item = review().items().get(0);

        assertEquals(0, decide(item, BatchReviewDecision.Status.APPROVED, true), errors.toString());

        var answer = CommandJson.read(output.toString(), Map.class);
        assertEquals(Map.of("batchId", "batch", "correctionRevision", 1, "contentVersion", 1, "status", "OPEN",
                "reviewCount", 1), withoutItems(answer));
        var answered = CommandJson.read(output.toString(), BatchReviewSlice.class).items();
        assertEquals(List.of(review().items().get(0)), answered, "the decided correction, as the whole review shows it");
        assertEquals(BatchReviewDecision.Status.APPROVED, answered.get(0).status());
        assertTrue(output.size() < 4_000, "a few KB, not the whole review: " + output.size());
    }

    @Test
    @Tag("F-GUARDA-R001")
    @DisplayName("should save a decision with --changed-only without reading the other batches of the campaign")
    void aDecisionWithChangedOnlyDoesNotReadTheOtherBatches() throws Exception {
        var item = review().items().get(0);
        // Another batch of the campaign that cannot be read: reading the whole review again would fail on it.
        Files.writeString(work.resolve(".content-audit/batches/older.json"), "{ not a batch");

        assertEquals(0, decide(item, BatchReviewDecision.Status.REJECTED, true), errors.toString());
        assertEquals(1, new FileSystemBatchStore(work).reviews("batch").size());
        assertThrows(RuntimeException.class, () -> decide(item, BatchReviewDecision.Status.PENDING, false),
                "the whole answer still reads every batch, as before");
    }

    @Test
    @Tag("F-GUARDA-R004")
    @DisplayName("should save the same decision file with the short answer as with the whole one")
    void theShortAnswerSavesTheSameDecisionFile() throws Exception {
        var item = review().items().get(0);
        assertEquals(0, decide(item, BatchReviewDecision.Status.REJECTED, true), errors.toString());
        assertEquals(0, decide(item, BatchReviewDecision.Status.REJECTED, false), errors.toString());

        var files = new ArrayList<Map<?, ?>>();
        try (var listed = Files.list(work.resolve(".content-audit/batch-reviews/batch"))) {
            for (Path file : listed.toList()) files.add(CommandJson.read(Files.readString(file), Map.class));
        }
        assertEquals(2, files.size());
        assertEquals(files.get(0).keySet(), files.get(1).keySet());
        for (var key : files.get(0).keySet()) {
            if (!key.equals("decidedAt")) assertEquals(files.get(0).get(key), files.get(1).get(key), "" + key);
        }
    }

    @Test
    @Tag("F-GUARDA-R002")
    @DisplayName("should take --changed-only only in the form the dashboard sends it; any other order goes through the normal parser")
    void changedOnlyIsTakenOnlyInTheDashboardsForm() {
        var service = mock(BatchReviewService.class);
        var decision = new BatchReviewDecision("batch", List.of(), BatchReviewDecision.Status.APPROVED, "José", null);
        when(service.save(decision)).thenReturn(new BatchReviewSlice("batch", 1, 1, BatchStatus.OPEN, 1, List.of()));
        String json = CommandJson.writeCompact(decision);
        var out = new PrintStream(output);

        assertEquals(0, SavedBatchReviewCmd.execute(List.of("batch", "review", "--decision-json", json,
                "--changed-only", "-f", "json"), service, out, System.err));
        verify(service).save(decision);
        verify(service, never()).decide(any());
        assertNull(SavedBatchReviewCmd.execute(List.of("batch", "review", "--changed-only", "--decision-json", json,
                "-f", "json"), service, out, System.err));
        assertNull(SavedBatchReviewCmd.execute(List.of("batch", "review", "batch", "--changed-only", "-f", "json"),
                service, out, System.err));
    }

    @Test
    @Tag("F-GUARDA-R003")
    @DisplayName("should annotate a case reading only the corrections of its exercise, and answer what the rejection changed")
    void annotatingReadsOnlyItsExerciseAndAnswersTheRejection() throws Exception {
        int read = review().contentVersion();
        Path cache = work.resolve(".content-audit/batch-review-cache/batch-batch.json");
        Files.deleteIfExists(cache);
        Files.writeString(work.resolve(".content-audit/batches/older.json"), "{ not a batch");

        var answer = annotate("--content-version", "" + read, "--correction-revision", "1");

        assertFalse(Files.exists(cache), "annotating no longer reads (and caches) the whole review");
        @SuppressWarnings("unchecked") var decisions = (List<Map<String, Object>>) answer.get("decisions");
        assertEquals(1, decisions.size());
        var rejected = CommandJson.read(CommandJson.writeCompact(decisions.get(0)), BatchReviewSlice.class);
        assertEquals(List.of("change"), rejected.items().stream().map(BatchReview.Item::changeId).toList(),
                "only the annotated exercise, not «other»");
        assertEquals(BatchReviewDecision.Status.REJECTED, rejected.items().get(0).status());
        assertTrue(rejected.items().get(0).reviewer().startsWith("Problemas:PRB-"));
        Files.delete(work.resolve(".content-audit/batches/older.json"));
        assertEquals(rejected.items().get(0), review().items().get(0));
        assertEquals(1, rejected.reviewCount());
    }

    @Test
    @Tag("F-GUARDA-R004")
    @DisplayName("should freeze in the case the same corrections, before and after as the whole review had them")
    void theCaseFreezesTheSameCorrectionsAsTheWholeReview() {
        var whole = review();
        var expected = CommandJson.read(CommandJson.writeCompact(List.of(whole.items().get(0))), List.class);

        var answer = annotate("--content-version", "" + whole.contentVersion(), "--correction-revision",
                "" + whole.correctionRevision());

        var problem = CommandJson.read(CommandJson.writeCompact(answer.get("problem")), ContentProblem.class);
        var snapshot = problem.cases().get(0).snapshot();
        assertEquals(expected, snapshot.get("items"));
        assertEquals(CommandJson.read(CommandJson.writeCompact(whole.items().get(0).before()), Map.class),
                snapshot.get("before"));
        assertEquals(CommandJson.read(CommandJson.writeCompact(whole.items().get(0).after()), Map.class),
                snapshot.get("after"));
        assertEquals(true, snapshot.get("rejectOnAnnotation"));
        assertEquals(whole.contentVersion(), problem.cases().get(0).contentVersion());
        assertEquals(whole.correctionRevision(), problem.cases().get(0).correctionRevision());
    }

    @Test
    @Tag("F-GUARDA-R003")
    @DisplayName("should undo answering the problem alone, as always, and with --with-decisions also what it gave back")
    void undoAnswersTheProblemAloneOrWithWhatItGaveBack() {
        var whole = review();
        var first = annotate("--content-version", "" + whole.contentVersion(), "--correction-revision", "1");
        var problem = CommandJson.read(CommandJson.writeCompact(first.get("problem")), ContentProblem.class);

        assertEquals(0, problem("undo", problem.id(), "--revision", "" + problem.revision(), "--case-id",
                problem.cases().get(0).id(), "--with-decisions"), errors.toString());

        var answer = CommandJson.read(output.toString(), Map.class);
        assertEquals(java.util.Set.of("problem", "decisions"), answer.keySet());
        @SuppressWarnings("unchecked") var decisions = (List<Map<String, Object>>) answer.get("decisions");
        var back = CommandJson.read(CommandJson.writeCompact(decisions.get(0)), BatchReviewSlice.class);
        assertEquals(BatchReviewDecision.Status.PENDING, back.items().get(0).status());
        assertEquals("Deshacer problema", back.items().get(0).reviewer());
        assertEquals(back.items().get(0), review().items().get(0));

        var again = annotate("--content-version", "" + whole.contentVersion(), "--correction-revision", "1");
        var annotated = CommandJson.read(CommandJson.writeCompact(again.get("problem")), ContentProblem.class);
        assertEquals(0, problem("undo", annotated.id(), "--revision", "" + annotated.revision(), "--case-id",
                annotated.cases().get(0).id()), errors.toString());
        assertEquals(annotated.id(), CommandJson.read(output.toString(), ContentProblem.class).id(),
                "without the flag, the problem alone, as the terminal has always read it");
    }

    @Test
    @Tag("F-GUARDA-R005")
    @DisplayName("should refuse to annotate a batch that changed or to reject in a published one, without writing a case")
    void annotatingStillRefusesAChangedOrPublishedBatch() {
        assertEquals(2, problem("annotate", "--title", "Nombre", "--batch", "batch", "--quiz", "quiz",
                "--content-version", "9", "--correction-revision", "1"));
        assertTrue(errors.toString().contains("La tanda cambió"), errors.toString());
        var batch = new FileSystemBatchStore(work).load("batch").orElseThrow();
        batch.setStatus(BatchStatus.PUBLISHED);
        new FileSystemBatchStore(work).save(batch);
        assertEquals(2, problem("annotate", "--title", "Nombre", "--batch", "batch", "--quiz", "quiz",
                "--content-version", "1", "--correction-revision", "1"));
        assertTrue(errors.toString().contains("publicada"), errors.toString());
        assertTrue(listProblems().isEmpty(), "no case was written");
    }

    private List<Path> listProblems() {
        try (var listed = Files.list(course.resolve(".content-audit/problems"))) {
            return listed.filter(p -> p.toString().endsWith(".json")).toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<?, ?> annotate(String... version) {
        var args = new ArrayList<>(List.of("annotate", "--batch", "batch", "--quiz", "quiz", "--title",
                "Teoría incompleta", "--criterion", "Explicar el contraste", "--comment", "Mi observación"));
        args.addAll(List.of(version));
        assertEquals(0, problem(args.toArray(String[]::new)), errors.toString());
        return CommandJson.read(output.toString(), Map.class);
    }

    private int problem(String... args) {
        output.reset(); errors.reset();
        var all = new ArrayList<>(List.of("problem"));
        all.addAll(List.of(args));
        all.addAll(List.of("--course", course.toString()));
        return ProblemCmd.execute(all, work, root, new PrintStream(output), new PrintStream(errors));
    }

    private int decide(BatchReview.Item item, BatchReviewDecision.Status status, boolean changedOnly) {
        var decision = new BatchReviewDecision("batch", List.of(new BatchReviewDecision.Selection(item.changeId(),
                item.step(), item.before(), item.after())), status, "Operador UI", null);
        // The decoded JSON maps the dashboard sends back to the CLI.
        String payload = CommandJson.writeCompact(CommandJson.read(CommandJson.writeCompact(decision), Map.class));
        var args = new ArrayList<>(List.of("batch", "review", "--decision-json", payload));
        if (changedOnly) args.add("--changed-only");
        args.addAll(List.of("-f", "json"));
        output.reset(); errors.reset();
        return SavedBatchReviewCmd.execute(args, work, new PrintStream(output), new PrintStream(errors));
    }

    private BatchReview review() {
        var bytes = new ByteArrayOutputStream();
        assertEquals(0, SavedBatchReviewCmd.execute(List.of("batch", "review", "batch", "-f", "json"), work,
                new PrintStream(bytes), System.err));
        return CommandJson.read(bytes.toString(), BatchReview.class);
    }

    private static Map<Object, Object> withoutItems(Map<?, ?> answer) {
        var copy = new java.util.LinkedHashMap<Object, Object>(answer);
        copy.remove("items");
        return copy;
    }

    private void proposal(String quiz, String id) {
        var proposal = new RevisionProposal();
        proposal.setProposalId(id);
        proposal.setPlanId("plan");
        proposal.setNodeTarget(AuditTarget.QUIZ);
        proposal.setNodeId(quiz);
        proposal.setElementBefore(snapshot(quiz, "before"));
        proposal.setElementAfter(snapshot(quiz, "after"));
        var artifact = new RevisionArtifact();
        artifact.setProposal(proposal);
        new FileSystemRevisionArtifactStore(work).save(artifact);
    }

    private static BatchChange change(String id, String quiz) {
        var change = new BatchChange();
        change.setChangeId(id);
        change.setSteps(List.of(new BatchChangeStep(quiz, List.of(ExercisePart.MINI_THEORY), snapshot(quiz, "after"))));
        return change;
    }

    private static CourseElementSnapshot snapshot(String quiz, String theory) {
        var snapshot = CommandJsonTest.snapshot(theory);
        snapshot.getQuiz().setId(quiz);
        return new CourseElementSnapshot(AuditTarget.QUIZ, quiz, snapshot.getQuiz(), null);
    }
}
