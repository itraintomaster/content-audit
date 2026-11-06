package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.auditinfrastructure.batchstore.FileSystemBatchStore;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.RootNodeEntity;
import com.learney.contentaudit.courseinfrastructure.FileSystemCourseRepository;
import com.learney.contentaudit.courseinfrastructure.CourseValidatorImpl;
import com.learney.contentaudit.revisiondomain.engine.DefaultCourseElementLocator;
import java.nio.file.Path;
import java.util.List;

/** Writes the base and today's course of a batch, cut to its level, as two course folders. Reads only. */
public class ExportPhotos {
    public static void main(String[] args) {
        var store = new FileSystemBatchStore(Path.of(args[0]));
        var batch = store.load(args[1]).orElseThrow();
        CourseEntity base = batch.getBaseCourseSnapshot() != null ? batch.getBaseCourseSnapshot()
                : batch.getEvolution().baseCourse();
        CourseEntity today = new DefaultBatchChangeLedger(null, new DefaultCourseElementLocator()).composedCourse(batch, base);
        var repo = new FileSystemCourseRepository(new CourseValidatorImpl());
        repo.save(cut(base, batch.getLevel().name()), Path.of(args[2]));
        repo.save(cut(today, batch.getLevel().name()), Path.of(args[3]));
        System.out.println("ok " + batch.getId() + " v" + batch.getContentVersion());
    }

    static CourseEntity cut(CourseEntity course, String level) {
        var kept0 = course.getRoot().getMilestones().stream().filter(m -> level.equalsIgnoreCase(m.getLabel())).toList();
        var present = new java.util.HashSet<String>();
        for (var m : kept0) for (var t : m.getTopics()) for (var k : t.getKnowledges()) present.add(k.getId());
        var result = new CourseEntity(course.getId(), course.getTitle(),
                course.getKnowledgeIds().stream().filter(present::contains).toList(), null, course.getSlug());
        var root = new RootNodeEntity();
        root.setId(course.getRoot().getId());
        root.setLabel(course.getRoot().getLabel());
        root.setCode(course.getRoot().getCode());
        root.setKind(course.getRoot().getKind());
        var kept = course.getRoot().getMilestones().stream().filter(m -> level.equalsIgnoreCase(m.getLabel())).toList();
        root.setMilestones(kept);
        root.setChildren(kept.stream().map(m -> m.getId()).toList());
        result.setRoot(root);
        return result;
    }
}
