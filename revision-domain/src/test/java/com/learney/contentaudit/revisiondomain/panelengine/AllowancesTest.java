package com.learney.contentaudit.revisiondomain.panelengine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.learney.contentaudit.revisiondomain.panel.QueryAllowance;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("F-MESA-R010")
class AllowancesTest {
    @Test void waitingPanelGetsItsTurnAfterMoreThanTenMinutesOfOtherPanelsProgress() {
        var shared = new CappedQueryAllowance(10, 1);
        var other = new Allowances.Counting(shared);
        var waiting = new Allowances.Counting(new Allowances.Counting(shared));
        assertTrue(other.tryReserve(1));
        var now = new AtomicLong();
        var turns = new AtomicInteger();

        assertTrue(Allowances.reserve(waiting, 1, false, now::get, () -> {
            now.addAndGet(TimeUnit.MINUTES.toNanos(3));
            other.settle(1, 1);
            if (turns.incrementAndGet() < 6) assertTrue(other.tryReserve(1));
        }), "healthy competing queries must not trip the whole batch");

        assertEquals(TimeUnit.MINUTES.toNanos(18), now.get());
        assertEquals(6, other.spent());
        assertEquals(0, waiting.spent(), "progress belongs to the shared allowance, not the waiting panel");
        assertTrue(shared.tripped().isEmpty());
        waiting.settle(1, 1);
        assertEquals(7, shared.spent());
        assertEquals(1, waiting.spent());
        assertEquals(0, shared.inFlight());
    }

    @Test void noSharedProgressForTenMinutesTripsAndNeverStartsAnotherQuery() {
        var shared = new CappedQueryAllowance(10, 1);
        assertTrue(shared.tryReserve(1));
        var now = new AtomicLong();

        assertFalse(Allowances.reserve(new Allowances.Counting(shared), 1, true, now::get,
                () -> now.addAndGet(TimeUnit.MINUTES.toNanos(11))));

        assertTrue(shared.tripped().orElseThrow().contains("10 minutos"));
        assertEquals(0, shared.spent());
        assertEquals(1, shared.inFlight(), "only the original outstanding reservation exists");
        shared.settle(1, 1);
        assertFalse(shared.tryReserve(1));
    }

    @Test void preexistingTripStopsBeforeReservationOrWait() {
        QueryAllowance allowance = mock(QueryAllowance.class);
        when(allowance.tripped()).thenReturn(Optional.of("service unavailable"));
        assertFalse(Allowances.reserve(allowance, 1, false, () -> 0,
                () -> fail("a tripped batch must not wait")));
        verify(allowance, never()).tryReserve(anyInt());
        verify(allowance, never()).tryReserveSequential(anyInt());
    }

    @Test void interruptionStopsWaitingPreservesFlagAndTripsSharedAllowance() {
        var shared = new CappedQueryAllowance(10, 1);
        assertTrue(shared.tryReserve(1));
        try {
            assertFalse(Allowances.reserve(new Allowances.Counting(shared), 1, false, () -> 0,
                    () -> { throw new InterruptedException("cancelled"); }));
            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals(Optional.of("se interrumpió la espera de consultas"), shared.tripped());
            shared.settle(1, 1);
            assertFalse(shared.tryReserve(1));
        } finally {
            Thread.interrupted();
        }
    }

    @Test void competingProgressCannotExceedTheOriginalBudget() {
        var shared = new CappedQueryAllowance(2, 1);
        assertTrue(shared.tryReserve(1));
        var now = new AtomicLong();
        assertTrue(Allowances.reserve(new Allowances.Counting(shared), 1, false, now::get, () -> {
            now.addAndGet(TimeUnit.MINUTES.toNanos(1));
            shared.settle(1, 1);
        }));
        shared.settle(1, 1);
        assertEquals(2, shared.spent());
        assertFalse(Allowances.reserve(shared, 1, false, now::get,
                () -> fail("an exhausted budget must not wait")));
        assertEquals(Optional.of("se llegó al tope de 2 consultas de la tanda"), shared.tripped());
    }
}
