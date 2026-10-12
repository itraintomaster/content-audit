package com.learney.contentaudit.auditcli.commands;

import static org.junit.jupiter.api.Assertions.*;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditinfrastructure.*;
import com.learney.contentaudit.auditinfrastructure.batchstore.FileSystemBatchStore;
import com.learney.contentaudit.revisiondomain.*;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.problems.ContentProblem;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class ProblemCmdTest {
    @TempDir Path root;
    Path work, course;
    final ByteArrayOutputStream output = new ByteArrayOutputStream(), errors = new ByteArrayOutputStream();
    @BeforeEach void setup() throws Exception {
        work = Files.createDirectory(root.resolve("campaign")); course = Files.createDirectory(root.resolve("course"));
        var proposal = new RevisionProposal(); proposal.setProposalId("proposal"); proposal.setPlanId("plan");
        proposal.setNodeTarget(AuditTarget.QUIZ); proposal.setNodeId("quiz");
        proposal.setElementBefore(CommandJsonTest.snapshot("before")); proposal.setElementAfter(CommandJsonTest.snapshot("after"));
        var artifact = new RevisionArtifact(); artifact.setProposal(proposal); new FileSystemRevisionArtifactStore(work).save(artifact);
        var change = new BatchChange(); change.setChangeId("change");
        change.setSteps(List.of(new BatchChangeStep("quiz", List.of(ExercisePart.MINI_THEORY), proposal.getElementAfter())));
        var batch = new CorrectionBatch(); batch.setId("batch"); batch.setPlanId("plan"); batch.setStatus(BatchStatus.RUNNING);
        batch.setChanges(List.of(change)); batch.setQuizProposals(List.of(new BatchProposalRef("quiz", "proposal")));
        new FileSystemBatchStore(work).save(batch);
    }
    int run(String... args) {
        output.reset(); errors.reset(); var all = new ArrayList<>(List.of("problem")); all.addAll(List.of(args));
        all.addAll(List.of("--course", course.toString()));
        return ProblemCmd.execute(all, work, root, new PrintStream(output), new PrintStream(errors));
    }
    BatchReview review() {
        var bytes = new ByteArrayOutputStream();
        assertEquals(0, SavedBatchReviewCmd.execute(List.of("batch", "review", "-f", "json"), work, new PrintStream(bytes), System.err));
        return CommandJson.read(bytes.toString(), BatchReview.class);
    }
    ContentProblem annotate(String... extra) {
        var read = review(); var args = new ArrayList<>(List.of("annotate", "--batch", "batch", "--quiz", "quiz", "--title", "Teoría incompleta",
                "--criterion", "Explicar el contraste", "--part", "miniTheory", "--comment", "Mi observación", "--expected", "Contraste explícito",
                "--content-version", "" + read.contentVersion(), "--correction-revision", "" + read.correctionRevision())); args.addAll(List.of(extra));
        assertEquals(0, run(args.toArray(String[]::new)), errors.toString());
        var json = CommandJson.read(output.toString(), Map.class);
        return CommandJson.read(CommandJson.writeCompact(json.get("problem")), ContentProblem.class);
    }
    @Test void annotationFreezesAllContentRejectsWithoutRewritingCheckpointAndCanUndo() throws Exception {
        String checkpoint = Files.readString(work.resolve(".content-audit/batches/batch.json"));
        var original = review().items().get(0); var p = annotate();
        assertEquals(BatchReviewDecision.Status.REJECTED, review().items().get(0).status());
        assertEquals(checkpoint, Files.readString(work.resolve(".content-audit/batches/batch.json")));
        assertTrue(CommandJson.write(p).contains("legacyNull")); assertTrue(CommandJson.write(p).contains("\"options\" : null"));
        assertEquals(0, run("undo", p.id(), "--revision", "" + p.revision(), "--case-id", p.cases().get(0).id()), errors.toString());
        assertEquals(original.status(), review().items().get(0).status());
        assertTrue(new FileSystemProblemStore(course).load(p.id()).orElseThrow().cases().isEmpty());
    }
    @Test void keepCorrectionAndGoodControlsNeverReject() {
        annotate("--keep-correction"); assertEquals(BatchReviewDecision.Status.PENDING, review().items().get(0).status());
        annotate("--usage", "good"); assertEquals(BatchReviewDecision.Status.PENDING, review().items().get(0).status());
    }
    @Test void anotherQuizNeedsOnlyTheExistingProblemAndItsFrozenContent() throws Exception {
        var p = annotate();
        var before = CommandJsonTest.snapshot("before-2"); before.getQuiz().setId("quiz-2");
        var after = CommandJsonTest.snapshot("after-2"); after.getQuiz().setId("quiz-2");
        before = new CourseElementSnapshot(AuditTarget.QUIZ, "quiz-2", before.getQuiz(), null);
        after = new CourseElementSnapshot(AuditTarget.QUIZ, "quiz-2", after.getQuiz(), null);
        var proposal = new RevisionProposal(); proposal.setProposalId("proposal-2"); proposal.setPlanId("plan");
        proposal.setNodeTarget(AuditTarget.QUIZ); proposal.setNodeId("quiz-2");
        proposal.setElementBefore(before); proposal.setElementAfter(after);
        var artifact = new RevisionArtifact(); artifact.setProposal(proposal); new FileSystemRevisionArtifactStore(work).save(artifact);
        var other = new BatchChange(); other.setChangeId("other");
        other.setSteps(List.of(new BatchChangeStep("quiz-2", List.of(ExercisePart.MINI_THEORY), after)));
        var batch = new FileSystemBatchStore(work).load("batch").orElseThrow();
        var changes = new ArrayList<>(batch.getChanges()); changes.add(other); batch.setChanges(changes);
        batch.setQuizProposals(List.of(new BatchProposalRef("quiz", "proposal"), new BatchProposalRef("quiz-2", "proposal-2")));
        new FileSystemBatchStore(work).save(batch);
        var review = review();
        assertEquals(0, run("annotate", p.id(), "--revision", "" + p.revision(), "--batch", "batch", "--quiz", "quiz-2",
                "--content-version", "" + review.contentVersion(), "--correction-revision", "" + review.correctionRevision()), errors.toString());
        var saved = new FileSystemProblemStore(course).load(p.id()).orElseThrow();
        assertEquals(p.criterion(), saved.criterion()); assertEquals(2, saved.cases().size());
        assertEquals("", saved.cases().get(1).comment());
        assertEquals(0, run("request", saved.id()));
        String request = (String) CommandJson.read(output.toString(), Map.class).get("text");
        assertTrue(request.contains("Explicar el contraste")); assertTrue(request.contains("quiz-2"));
        assertTrue(request.contains("La descripción general se aplica a todos los quizzes"));
    }
    @Test void undoRefusesALaterHumanDecisionAndKeepsTheFrozenCase() {
        var p = annotate(); var i = review().items().get(0);
        var decision = new BatchReviewDecision("batch", List.of(new BatchReviewDecision.Selection(i.changeId(), i.step(), i.before(), i.after())),
                BatchReviewDecision.Status.APPROVED, "Operador", null);
        assertEquals(0, SavedBatchReviewCmd.execute(List.of("batch", "review", "--decision-json", CommandJson.writeCompact(decision), "-f", "json"), work, new PrintStream(new ByteArrayOutputStream()), System.err));
        assertEquals(2, run("undo", p.id(), "--revision", "" + p.revision(), "--case-id", p.cases().get(0).id()));
        assertEquals(BatchReviewDecision.Status.APPROVED, review().items().get(0).status());
        assertEquals(1, new FileSystemProblemStore(course).load(p.id()).orElseThrow().cases().size());
    }
    @Test void staleBatchCannotCreateProblemAndRepeatAnnotationIsIdempotent() {
        assertEquals(2, run("annotate", "--title", "Nombre", "--batch", "batch", "--quiz", "quiz", "--content-version", "999", "--correction-revision", "999"));
        assertTrue(new FileSystemProblemStore(course).list().isEmpty());
        var p = annotate(); var read = review();
        assertEquals(0, run("annotate", p.id(), "--revision", "" + p.revision(), "--batch", "batch", "--quiz", "quiz",
                "--content-version", "" + read.contentVersion(), "--correction-revision", "" + read.correctionRevision()));
        assertEquals(1, new FileSystemProblemStore(course).load(p.id()).orElseThrow().cases().size());
    }
    @Test void requestIncludesEveryCaseCurrentGuidesAndExactProgressCommandsButExcludesJudgeEvalFiles() throws Exception {
        var guide = root.resolve(".sentinel/agents/mini-theory-judge/nodes/general.md"); Files.createDirectories(guide.getParent()); Files.writeString(guide, "GUÍA GENERAL V2");
        var eval = guide.getParent().getParent().resolve("eval/cases.json"); Files.createDirectories(eval.getParent()); Files.writeString(eval, "SECRET_EVAL_DATA");
        var p = annotate();
        assertEquals(0, run("edit", p.id(), "--revision", "" + p.revision(), "--clear-parts"));
        p = new FileSystemProblemStore(course).load(p.id()).orElseThrow(); assertTrue(p.parts().isEmpty());
        assertEquals(0, run("request", p.id()));
        String request = (String) CommandJson.read(output.toString(), Map.class).get("text");
        for (String text : List.of("before", "after", "Mi observación", "Contraste explícito", "GUÍA GENERAL V2", "--state en-trabajo", "--state para-probar", "--revision " + p.revision(), course.toString())) assertTrue(request.contains(text), text);
        assertFalse(request.contains("SECRET_EVAL_DATA"));
        assertTrue(request.contains("sha256:"));
        assertTrue(request.contains("Sentinel no gobierna este repositorio"));
        work = Files.createDirectory(root.resolve("other-campaign"));
        assertEquals(0, run("show", p.id())); assertEquals(p, CommandJson.read(output.toString(), ContentProblem.class));
    }
    @Test void searchGroupsMatchingTextWithoutModelsAndMalformedRegexReturnsAnError() {
        var read = review(); assertEquals(0, run("search", "--batch", read.batchId(), "--search", "AFTER"));
        assertEquals(1, ((List<?>) CommandJson.read(output.toString(), Map.class).get("groups")).size());
        assertEquals(2, run("search", "--batch", "batch", "--search", "[", "--regex"));
    }
}
