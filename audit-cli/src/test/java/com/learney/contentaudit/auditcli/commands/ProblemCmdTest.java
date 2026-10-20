package com.learney.contentaudit.auditcli.commands;

import static org.junit.jupiter.api.Assertions.*;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditinfrastructure.*;
import com.learney.contentaudit.auditinfrastructure.batchstore.FileSystemBatchStore;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.courseinfrastructure.CourseValidatorImpl;
import com.learney.contentaudit.courseinfrastructure.FileSystemCourseRepository;
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
    @Test void inspectionShowsCurrentContentAndRefusesFilesOutsideTheProject() throws Exception {
        var p = annotate();
        Path guide = Files.writeString(root.resolve("guide.md"), "Regla actual\n");
        var delivery = Map.of("changes", List.of(
                Map.of("path", guide.toString(), "version", "sha256:old", "summary", "Guía", "scope", "judge"),
                Map.of("path", root.resolve("../outside.md").toString(), "version", "old", "summary", "Fuera", "scope", "judge")),
                "results", List.of(Map.of("caseId", p.cases().get(0).id(), "fixed", false, "version", "v1", "modelConsulted", false)));
        Path artifact = Files.writeString(root.resolve("delivery.json"), CommandJson.writeCompact(delivery));
        assertEquals(0, run("update", p.id(), "--revision", "" + p.revision(), "--state", "en-trabajo",
                "--step", "guia", "--artifacts", artifact.toString()), errors.toString());
        var before = new FileSystemProblemStore(course).load(p.id()).orElseThrow();
        assertEquals(0, run("inspect", p.id()), errors.toString());
        var inspected = new com.fasterxml.jackson.databind.ObjectMapper().readTree(output.toString());
        var changes = inspected.path("changes");
        assertEquals("Regla actual\n", changes.get(0).path("after").asText());
        assertFalse(changes.get(0).path("matchesDelivery").asBoolean());
        assertFalse(changes.get(1).path("available").asBoolean());
        assertFalse(changes.get(1).has("after"));
        assertEquals(before, new FileSystemProblemStore(course).load(p.id()).orElseThrow());
    }

    @Test void inspectionIncludesJudgeGateScriptsAsTextWithoutExecutingThem() throws Exception {
        var p = annotate();
        Path gate = root.resolve(".sentinel/agents/mini-theory-judge/scripts/gate_verdict.sh");
        Files.createDirectories(gate.getParent());
        String source = "#!/bin/sh\nexit 73\n";
        Files.writeString(gate, source);
        String version = "sha256:" + java.util.HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(gate)));
        var delivery = Map.of("changes", List.of(Map.of("path", gate.toString(), "version", version,
                        "summary", "Validación del juez", "scope", "judge")),
                "results", List.of(Map.of("caseId", p.cases().get(0).id(), "fixed", true,
                        "version", "v1", "modelConsulted", false)));
        Path artifact = Files.writeString(root.resolve("script-delivery.json"), CommandJson.writeCompact(delivery));
        assertEquals(0, run("update", p.id(), "--revision", "" + p.revision(), "--state", "en-trabajo",
                "--step", "gate", "--artifacts", artifact.toString()), errors.toString());
        assertEquals(0, run("inspect", p.id()), errors.toString());
        var change = new com.fasterxml.jackson.databind.ObjectMapper().readTree(output.toString()).path("changes").get(0);
        assertTrue(change.path("available").asBoolean());
        assertTrue(change.path("matchesDelivery").asBoolean());
        assertEquals(source, change.path("after").asText());
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

    // ── F-NADIE-R005: a problem nobody objected to, also in an exercise without changes ──────────────

    /** The course on disk with an exercise that no correction of the batch touches. */
    void courseWith(String quizId) {
        var quiz = new QuizTemplateEntity(); quiz.setId(quizId); quiz.setOidId(quizId); quiz.setKind("CLOZE");
        quiz.setKnowledgeId("knowledge"); quiz.setTitle("Negativas con be"); quiz.setInstructions("Elige la forma negativa.");
        quiz.setTranslation("No hace calor."); quiz.setMiniTheory("Para negar, agregamos not."); quiz.setSentences(List.of("It is not hot."));
        quiz.setForm(new FormEntity("CLOZE", 1.0, "", "", List.of(new SentencePartEntity(SentencePartKind.TEXT, "It ", null),
                new SentencePartEntity(SentencePartKind.CLOZE, "", List.of("is not")), new SentencePartEntity(SentencePartKind.TEXT, " hot.", null)), null, null));
        var knowledge = new KnowledgeEntity(); knowledge.setId("knowledge"); knowledge.setCode("knowledge"); knowledge.setKind(NodeKind.KNOWLEDGE);
        knowledge.setLabel("Negativas con be"); knowledge.setOldId("old-knowledge"); knowledge.setParentId("topic"); knowledge.setIsRule(true);
        knowledge.setInstructions("Elige la forma negativa."); knowledge.setOrder(1); knowledge.setSlug("negativas-con-be"); knowledge.setQuizTemplates(List.of(quiz));
        var topic = new TopicEntity(); topic.setId("topic"); topic.setCode("topic"); topic.setKind(NodeKind.TOPIC); topic.setLabel("Be");
        topic.setOldId("old-topic"); topic.setParentId("milestone"); topic.setRuleIds(List.of("knowledge")); topic.setOrder(1); topic.setSlug("be");
        topic.setKnowledges(List.of(knowledge));
        var milestone = new MilestoneEntity(); milestone.setId("milestone"); milestone.setCode("milestone"); milestone.setKind(NodeKind.MILESTONE);
        milestone.setLabel("A1"); milestone.setOldId("old-milestone"); milestone.setParentId("root"); milestone.setChildren(List.of("topic"));
        milestone.setOrder(1); milestone.setSlug("a1"); milestone.setTopics(List.of(topic));
        var top = new RootNodeEntity(); top.setId("root"); top.setCode("root"); top.setKind(NodeKind.ROOT); top.setChildren(List.of("milestone")); top.setMilestones(List.of(milestone));
        var entity = new CourseEntity(); entity.setId("000000000000000000000001"); entity.setTitle("english-course"); entity.setKnowledgeIds(List.of("knowledge"));
        entity.setRoot(top); entity.setSlug("english-course");
        new FileSystemCourseRepository(new CourseValidatorImpl()).save(entity, course);
    }
    /** What each judge said of [quiz] in Corrección 1, as the dashboard reads it: two missed it, two are only named. */
    static String said(String quiz) {
        return CommandJson.writeCompact(Map.of("state", "correction", "stateLabel", "Corrección 1", "quizzes", Map.of(quiz, Map.of(
                "judges", List.of(
                        Map.of("judge", "quiz-instruction", "name", "Consigna que no se cumple", "status", "UP_TO_DATE"),
                        Map.of("judge", "mini-theory", "name", "Miniteoría con error", "status", "EARLIER", "detail", "versión del 5/10"),
                        Map.of("judge", "pragmatism", "name", "Pragmatismo comunicativo", "status", "ABSTAINED", "detail", "no aplica"),
                        Map.of("judge", "a1-didactic", "name", "Calidad didáctica A1", "status", "NOT_MEASURED")),
                "analyzers", List.of(Map.of("analyzer", "sentence-length", "name", "Oraciones con el largo de su nivel"))))));
    }
    List<String> unchanged(String quiz, String... extra) {
        var read = review(); var args = new ArrayList<>(List.of("annotate", "--batch", "batch", "--quiz", quiz,
                "--content-version", "" + read.contentVersion(), "--correction-revision", "" + read.correctionRevision()));
        args.addAll(List.of(extra)); return args;
    }
    ContentProblem problemOf(String json) {
        return CommandJson.read(CommandJson.writeCompact(CommandJson.read(json, Map.class).get("problem")), ContentProblem.class);
    }
    @Test @Tag("F-NADIE-R005") void anExerciseWithoutChangesIsFrozenAsTheCourseHasItWithItsStateAndTheJudgesThatMissedIt() {
        courseWith("quiet");
        assertEquals(0, run(unchanged("quiet", "--title", "Negación sin contraste", "--criterion", "No contrasta", "--keep-correction",
                "--verdicts", said("quiet")).toArray(String[]::new)), errors.toString());
        assertEquals(false, CommandJson.read(output.toString(), Map.class).get("rejected"));
        var c = problemOf(output.toString()).cases().get(0);
        assertEquals("quiet", c.quizId()); assertEquals("batch", c.batchId());
        assertEquals(List.of(), c.snapshot().get("items"));
        assertEquals(c.snapshot().get("before"), c.snapshot().get("after"));
        assertEquals("Para negar, agregamos not.", ((Map<?, ?>) ((Map<?, ?>) c.snapshot().get("after")).get("quiz")).get("miniTheory"));
        var nobody = (Map<?, ?>) c.snapshot().get("nobodyObjects");
        assertEquals("correction", nobody.get("state")); assertEquals("Corrección 1", nobody.get("stateLabel"));
        // Those that said «sin error» missed it; the abstention and the one not measured are only named.
        assertEquals(List.of("quiz-instruction", "mini-theory"), nobody.get("missedBy"));
        var judges = (List<?>) nobody.get("judges");
        assertEquals(List.of("UP_TO_DATE", "EARLIER", "ABSTAINED", "NOT_MEASURED"), judges.stream().map(j -> ((Map<?, ?>) j).get("status")).toList());
        assertEquals("versión del 5/10", ((Map<?, ?>) judges.get(1)).get("detail")); assertEquals("no aplica", ((Map<?, ?>) judges.get(2)).get("detail"));
        assertEquals(List.of(Map.of("analyzer", "sentence-length", "name", "Oraciones con el largo de su nivel")), nobody.get("analyzers"));
        assertEquals(BatchReviewDecision.Status.PENDING, review().items().get(0).status(), "no decision changes");
    }
    @Test @Tag("F-NADIE-R005") void anExerciseWithoutChangesIsAnnotatedOnceAlsoWithThePublishedBatchAndUndoTakesItOut() {
        courseWith("quiet");
        var first = unchanged("quiet", "--title", "Negación sin contraste", "--criterion", "No contrasta", "--verdicts", said("quiet"));
        var again = unchanged("quiet", "--verdicts", said("quiet"));
        var store = new FileSystemBatchStore(work); var published = store.load("batch").orElseThrow();
        published.setStatus(BatchStatus.PUBLISHED); store.save(published);
        // Nothing to reject: no --keep-correction is needed.
        assertEquals(0, run(first.toArray(String[]::new)), errors.toString());
        var p = problemOf(output.toString());
        again.addAll(List.of(p.id(), "--revision", "" + p.revision()));
        assertEquals(0, run(again.toArray(String[]::new)), errors.toString());
        assertEquals(List.of(), CommandJson.read(output.toString(), Map.class).get("newCaseIds"));
        assertEquals(1, new FileSystemProblemStore(course).load(p.id()).orElseThrow().cases().size());
        assertEquals(0, run("undo", p.id(), "--revision", "" + p.revision(), "--case-id", p.cases().get(0).id()), errors.toString());
        assertTrue(new FileSystemProblemStore(course).load(p.id()).orElseThrow().cases().isEmpty());
    }
    @Test @Tag("F-NADIE-R005") void anExerciseWithoutCorrectionIsNotAnnotatedWithoutWhatEachJudgeSaid() {
        courseWith("quiet");
        String[] base = {"--title", "Negación sin contraste", "--criterion", "No contrasta"};
        assertEquals(2, run(unchanged("quiet", base).toArray(String[]::new)));
        assertTrue(errors.toString().contains("no tiene una corrección"), errors.toString());
        var noneToday = new ArrayList<>(unchanged("quiet", base)); noneToday.addAll(List.of("--verdicts", said("quiet").replace("UP_TO_DATE", "EARLIER")));
        assertEquals(2, run(noneToday.toArray(String[]::new))); assertTrue(errors.toString().contains("versión de hoy"), errors.toString());
        var unknown = new ArrayList<>(unchanged("quiet", base)); unknown.addAll(List.of("--verdicts", said("quiet").replace("ABSTAINED", "MAYBE")));
        assertEquals(2, run(unknown.toArray(String[]::new)));
        var other = new ArrayList<>(unchanged("quiet", base)); other.addAll(List.of("--verdicts", said("other")));
        assertEquals(2, run(other.toArray(String[]::new)));
        assertTrue(new FileSystemProblemStore(course).list().isEmpty());
    }
    @Test @Tag("F-NADIE-R005") void aChangedExerciseKeepsItsCorrectionAndAlsoWhatEachJudgeSaidOfTheAfter() {
        var c = annotate("--verdicts", said("quiz")).cases().get(0);
        assertEquals(1, ((List<?>) c.snapshot().get("items")).size());
        assertNotEquals(c.snapshot().get("before"), c.snapshot().get("after"));
        assertEquals(List.of("quiz-instruction", "mini-theory"), ((Map<?, ?>) c.snapshot().get("nobodyObjects")).get("missedBy"));
        assertEquals(BatchReviewDecision.Status.REJECTED, review().items().get(0).status());
    }
    @Test @Tag("F-NADIE-R005") void theRequestBringsTheGuidesOfTheJudgesThatMissedItAndNotOfTheOnesThatDidNotJudge() throws Exception {
        for (var guide : Map.of("mini-theory-judge", "GUÍA MINITEORÍA", "quiz-instruction-validator", "GUÍA CONSIGNA",
                "pragmatism-judge", "GUÍA PRAGMATISMO", "a1-didactic-judge", "GUÍA DIDÁCTICA").entrySet()) {
            var file = root.resolve(".sentinel/agents/" + guide.getKey() + "/nodes/general.md");
            Files.createDirectories(file.getParent()); Files.writeString(file, guide.getValue());
        }
        courseWith("quiet");
        assertEquals(0, run(unchanged("quiet", "--title", "Negación sin contraste", "--criterion", "No contrasta",
                "--verdicts", said("quiet")).toArray(String[]::new)), errors.toString());
        var p = problemOf(output.toString()); assertTrue(p.parts().isEmpty());
        assertEquals(0, run("request", p.id()), errors.toString());
        String request = (String) CommandJson.read(output.toString(), Map.class).get("text");
        for (String text : List.of("GUÍA MINITEORÍA", "GUÍA CONSIGNA", "Un caso con nobodyObjects", "missedBy", "Corrección 1", "Para negar, agregamos not."))
            assertTrue(request.contains(text), text);
        assertFalse(request.contains("GUÍA PRAGMATISMO")); assertFalse(request.contains("GUÍA DIDÁCTICA"));
    }
    @Test void searchGroupsMatchingTextWithoutModelsAndMalformedRegexReturnsAnError() {
        var read = review(); assertEquals(0, run("search", "--batch", read.batchId(), "--search", "AFTER"));
        assertEquals(1, ((List<?>) CommandJson.read(output.toString(), Map.class).get("groups")).size());
        assertEquals(2, run("search", "--batch", "batch", "--search", "[", "--regex"));
    }
}
