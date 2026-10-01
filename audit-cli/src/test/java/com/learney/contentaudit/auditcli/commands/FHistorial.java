package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableMilestone;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.AuditableTopic;
import com.learney.contentaudit.auditdomain.contextnumbers.ContextNumbers;
import com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer;
import com.learney.contentaudit.auditinfrastructure.FileSystemActiveAnalysisSelectionStore;
import com.learney.contentaudit.auditinfrastructure.FileSystemAuditReportStore;
import com.learney.contentaudit.auditinfrastructure.FileSystemBaselineStore;
import com.learney.contentaudit.auditinfrastructure.FileSystemBlindSampleStore;
import com.learney.contentaudit.auditinfrastructure.FileSystemProjectionStore;
import com.learney.contentaudit.auditinfrastructure.FileSystemSealedGoalsStore;
import com.learney.contentaudit.coursedomain.SentenceMode;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.revisiondomain.consolidatedview.ConsolidatedViewAvailability;
import com.learney.contentaudit.revisiondomain.consolidatedview.NonApplicablePending;
import com.learney.contentaudit.revisiondomain.projection.CourseProjection;
import com.learney.contentaudit.revisiondomain.projection.ProjectedChange;
import com.learney.contentaudit.auditdomain.AuditableEntity;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * The working directory of the commands of package F in a test: the history of analyses, the baseline, the sealed
 * goals, the saved projections, the active pair and the blind samples, each in its real file under
 * {@code .content-audit/}, as {@code Main} builds them -- and what a command prints.
 */
final class FHistorial {
    final Path dir;
    final FileSystemAuditReportStore audits;
    final FileSystemBaselineStore baselines;
    final FileSystemSealedGoalsStore sealedGoals;
    final FileSystemProjectionStore projections;
    final FileSystemActiveAnalysisSelectionStore active;
    final FileSystemBlindSampleStore samples;

    FHistorial(Path dir) {
        this.dir = dir;
        this.audits = new FileSystemAuditReportStore(dir, new DefaultQuizStudentViewRenderer(
                DefaultQuizSentenceConverter.create()));
        this.baselines = new FileSystemBaselineStore(dir);
        this.sealedGoals = new FileSystemSealedGoalsStore(dir);
        this.projections = new FileSystemProjectionStore(dir);
        this.active = new FileSystemActiveAnalysisSelectionStore(dir);
        this.samples = new FileSystemBlindSampleStore(dir);
    }

    /** An analysis of the history with this id, saved as {@code analyze} saves it. */
    String analysis(String id, String marker) {
        String saved = audits.save(report(marker));
        move(".content-audit/audits/audit-" + saved + ".json", ".content-audit/audits/audit-" + id + ".json");
        move(".content-audit/audit-digests/audit-" + saved + ".json",
                ".content-audit/audit-digests/audit-" + id + ".json");
        return id;
    }

    /** A projection of an active pair, available, whose photo with the pending proposals carries the marker. */
    static CourseProjection projection(String auditId, String planId, String marker, List<String> applied,
            List<NonApplicablePending> notApplicable) {
        List<ProjectedChange> changes = new ArrayList<>();
        for (String proposal : applied) {
            changes.add(new ProjectedChange(proposal, AuditTarget.QUIZ, "67fab6d59930102295341ecd", true));
        }
        return new CourseProjection(auditId, planId, planId, ConsolidatedViewAvailability.AVAILABLE, null,
                report("analisis"), report("analisis"), report(marker), changes, new ArrayList<>(notApplicable));
    }

    /**
     * A small course as the engine reports it -- A1, Present Simple, «Afirmativas regulares» and «I ___ (love)
     * vegetables.» --, with the vocabulary of 29/9 and a marker in its root that says which photo it is.
     */
    static AuditReport report(String marker) {
        AuditNode root = node(null, AuditTarget.COURSE, null);
        root.getMetadata().put("marca", marker);
        root.setNumbers(new ContextNumbers(0.7392, new ArrayList<>(), null, List.of()));
        AuditNode level = node(new AuditableMilestone(new ArrayList<>(), "6814dafa7d73e7209a13d382", "A1", "A1"),
                AuditTarget.MILESTONE, root);
        AuditNode topic = node(new AuditableTopic(new ArrayList<>(), "6814dafa7d73e7209a13d383", "Present Simple",
                "a1.01"), AuditTarget.TOPIC, level);
        AuditNode knowledge = node(new AuditableKnowledge(new ArrayList<>(), "Afirmativas regulares",
                "Escribe la forma correcta.", true, "6814dafa7d73e7209a13d389", "Afirmativas regulares",
                "6814dafa7d73e7209a13d389", SentenceMode.FILL, "Present Simple"), AuditTarget.KNOWLEDGE, topic);
        node(new AuditableQuiz(List.of(), "67fab6d59930102295341ecd", "I ___ (love) vegetables.",
                "67fab6d59930102295341ecd", "Me encantan las verduras.", List.of("I love vegetables."),
                "I love vegetables.", "Escribe la forma correcta.", List.of(), null, null, null), AuditTarget.QUIZ,
                knowledge);
        return new AuditReport(root);
    }

    private static AuditNode node(AuditableEntity entity, AuditTarget target, AuditNode parent) {
        AuditNode node = new AuditNode(entity, target, parent, new ArrayList<>(), new LinkedHashMap<>(),
                new LinkedHashMap<>(), null);
        if (parent != null) {
            parent.getChildren().add(node);
        }
        return node;
    }

    /** Every file under the working directory with its bytes, to see that nothing changed. */
    Map<String, byte[]> files() {
        Map<String, byte[]> files = new TreeMap<>();
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path file : walk.filter(Files::isRegularFile).toList()) {
                files.put(dir.relativize(file).toString(), Files.readAllBytes(file));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return files;
    }

    /** The names of the files of a directory of {@code .content-audit/}, sorted; none when it does not exist. */
    List<String> names(String subdir) {
        Path path = dir.resolve(".content-audit").resolve(subdir);
        if (!Files.isDirectory(path)) {
            return List.of();
        }
        try (Stream<Path> list = Files.list(path)) {
            return list.map(p -> p.getFileName().toString()).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void move(String from, String to) {
        try {
            Path source = dir.resolve(from);
            if (Files.exists(source)) {
                Files.move(source, dir.resolve(to));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** What a command printed, and how it ended. */
    record Salida(int exit, String out, String err) {
    }

    static Salida run(Supplier<Integer> command) {
        PrintStream out = System.out;
        PrintStream err = System.err;
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outBytes, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(errBytes, true, StandardCharsets.UTF_8));
        try {
            Integer exit = command.get();
            return new Salida(exit != null ? exit : -1, outBytes.toString(StandardCharsets.UTF_8),
                    errBytes.toString(StandardCharsets.UTF_8));
        } finally {
            System.setOut(out);
            System.setErr(err);
        }
    }
}
