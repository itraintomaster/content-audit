package com.learney.contentaudit.revisiondomain.panelengine;

import com.learney.contentaudit.revisiondomain.panel.QueryAllowance;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * How a panel or a meter takes its turn on the shared allowance (F-MESA-R010): it waits while the queries in flight are
 * at their limit, and gives up as soon as the allowance is tripped -- a service failure or the cap of the batch. It also
 * counts what one panel spends, for its decisions and its topic.
 */
final class Allowances {

    /** How long a waiting panel tolerates no shared consultation progress before treating the allowance as stuck. */
    static final long WAIT_LIMIT_NANOS = TimeUnit.MINUTES.toNanos(10);

    private Allowances() {
    }

    /** Reserves n queries, waiting for its turn; false when the allowance is (or gets) tripped. */
    static boolean reserve(QueryAllowance allowance, int queries) {
        return reserve(allowance, queries, false);
    }

    static boolean reserveSequential(QueryAllowance allowance, int queries) {
        return reserve(allowance, queries, true);
    }

    private static boolean reserve(QueryAllowance allowance, int queries, boolean sequential) {
        return reserve(allowance, queries, sequential, System::nanoTime, () -> Thread.sleep(2));
    }

    @FunctionalInterface
    interface Waiter {
        void pause() throws InterruptedException;
    }

    /** Package-local clock and pause seam keeps the ten-minute watchdog testable without waiting. */
    static boolean reserve(QueryAllowance allowance, int queries, boolean sequential, LongSupplier nanoTime,
            Waiter waiter) {
        if (allowance == null) {
            return true;
        }
        long lastProgressAt = nanoTime.getAsLong();
        int lastSpent = sharedSpent(allowance);
        while (true) {
            if (allowance.tripped().isPresent()) {
                return false;
            }
            if (sequential ? allowance.tryReserveSequential(queries) : allowance.tryReserve(queries)) {
                return true;
            }
            if (allowance.tripped().isPresent()) {
                return false;
            }
            long now = nanoTime.getAsLong();
            int spent = sharedSpent(allowance);
            if (spent > lastSpent) {
                lastSpent = spent;
                lastProgressAt = now;
            }
            if (now - lastProgressAt > WAIT_LIMIT_NANOS) {
                allowance.trip("no se registró progreso de consultas en " + TimeUnit.NANOSECONDS.toMinutes(WAIT_LIMIT_NANOS)
                        + " minutos");
                return false;
            }
            try {
                waiter.pause();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                allowance.trip("se interrumpió la espera de consultas");
                return false;
            }
        }
    }

    /** A waiting panel's own spend is unchanged while other panels complete their queries. */
    private static int sharedSpent(QueryAllowance allowance) {
        while (allowance instanceof Counting counting && counting.shared != null) {
            allowance = counting.shared;
        }
        return allowance.spent();
    }

    static boolean tripped(QueryAllowance allowance) {
        return allowance != null && allowance.tripped().isPresent();
    }

    /** The shared allowance as one panel sees it: the same limits and breaker, and what this panel spent. */
    static final class Counting implements QueryAllowance {
        private final QueryAllowance shared;

        private int spent;

        Counting(QueryAllowance shared) {
            this.shared = shared;
        }

        @Override
        public boolean tryReserve(int queries) {
            return shared == null || shared.tryReserve(queries);
        }

        @Override
        public boolean tryReserveSequential(int queries) {
            return shared == null || shared.tryReserveSequential(queries);
        }

        @Override
        public void settle(int reserved, int spent) {
            synchronized (this) {
                this.spent += Math.max(0, spent);
            }
            if (shared != null) {
                shared.settle(reserved, spent);
            }
        }

        @Override
        public void settleSequential(int reserved, int spent) {
            synchronized (this) {
                this.spent += Math.max(0, spent);
            }
            if (shared != null) {
                shared.settleSequential(reserved, spent);
            }
        }

        @Override
        public void trip(String cause) {
            if (shared != null) {
                shared.trip(cause);
            }
        }

        @Override
        public Optional<String> tripped() {
            return shared != null ? shared.tripped() : Optional.empty();
        }

        @Override
        public synchronized int spent() {
            return spent;
        }
    }
}
