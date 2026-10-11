package com.learney.contentaudit.auditinfrastructure;

import static org.junit.jupiter.api.Assertions.*;
import com.learney.contentaudit.revisiondomain.problems.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemProblemStoreTest {
    @TempDir Path course;
    @Test void separateInstancesShareCourseRecordsAndPermitOnlyOneConcurrentRevision() throws Exception {
        var a = new ProblemService(new FileSystemProblemStore(course), Clock.systemUTC());
        var b = new ProblemService(new FileSystemProblemStore(course), Clock.systemUTC());
        var p = a.create("Nombre", "Criterio", List.of(), List.of());
        assertEquals(p, b.get(p.id()));
        var pool = Executors.newFixedThreadPool(2);
        try {
            var start = new CountDownLatch(1);
            var tasks = List.<Callable<Boolean>>of(() -> edit(a, p, start), () -> edit(b, p, start));
            var futures = tasks.stream().map(pool::submit).toList(); start.countDown();
            int succeeded = 0; for (var f : futures) if (f.get()) succeeded++;
            assertEquals(1, succeeded); assertEquals(1, b.get(p.id()).revision());
            assertEquals(1, b.list().size());
        } finally { pool.shutdownNow(); }
    }
    boolean edit(ProblemService service, ContentProblem p, CountDownLatch start) throws Exception {
        start.await();
        try { service.edit(p.id(), 0, "Actualizado", null, null, null); return true; }
        catch (IllegalStateException stale) { return false; }
    }
    @Test void invalidIdsCannotEscapeCourseStorage() {
        assertThrows(IllegalArgumentException.class, () -> new FileSystemProblemStore(course).load("../../other"));
    }
}
