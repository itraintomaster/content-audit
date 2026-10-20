package com.learney.contentaudit.revisiondomain.batch;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * F-CORTE-R002 and R005: which process runs a measurement, and whether a recorded one still runs. A process is
 * the same one only with the same number and start: the operating system reuses numbers.
 */
public interface RunningProcesses {
    long currentPid();

    Instant currentStartedAt();

    boolean running(long pid, Instant startedAt);

    /** The processes of this machine, as the operating system reports them. */
    static RunningProcesses ofThisMachine() {
        return new RunningProcesses() {
            @Override public long currentPid() { return ProcessHandle.current().pid(); }
            @Override public Instant currentStartedAt() { return ProcessHandle.current().info().startInstant().orElse(null); }
            @Override public boolean running(long pid, Instant startedAt) {
                return ProcessHandle.of(pid).filter(ProcessHandle::isAlive)
                        // Without a recorded start, a live number is taken as still running: never reconcile a live run.
                        .map(h -> startedAt == null || h.info().startInstant()
                                .map(s -> Objects.equals(s.truncatedTo(ChronoUnit.MILLIS), startedAt.truncatedTo(ChronoUnit.MILLIS)))
                                .orElse(true))
                        .orElse(false);
            }
        };
    }
}
