package com.learney.contentaudit.revisiondomain.panelengine;

import com.learney.contentaudit.revisiondomain.panel.QueryAllowance;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * How a panel or a meter takes its turn on the shared allowance (F-MESA-R010): it waits while the queries in flight are
 * at their limit, and gives up as soon as the allowance is tripped -- a service failure or the cap of the batch. It also
 * counts what one panel spends, for its decisions and its topic.
 */
final class Allowances {

    /** How long a panel waits for a turn before taking the allowance as stuck. */
    static final long WAIT_LIMIT_NANOS = TimeUnit.MINUTES.toNanos(10);

    private Allowances() {
    }

    /** Reserves n queries, waiting for its turn; false when the allowance is (or gets) tripped. */
    static boolean reserve(QueryAllowance allowance, int queries) {
        if (allowance == null) {
            return true;
        }
        long deadline = System.nanoTime() + WAIT_LIMIT_NANOS;
        while (true) {
            if (allowance.tripped().isPresent()) {
                return false;
            }
            if (allowance.tryReserve(queries)) {
                return true;
            }
            if (allowance.tripped().isPresent()) {
                return false;
            }
            if (System.nanoTime() > deadline) {
                allowance.trip("no se liberó el cupo de consultas en " + TimeUnit.NANOSECONDS.toMinutes(WAIT_LIMIT_NANOS)
                        + " minutos");
                return false;
            }
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                allowance.trip("se interrumpió la espera de consultas");
                return false;
            }
        }
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
        public void settle(int reserved, int spent) {
            synchronized (this) {
                this.spent += Math.max(0, spent);
            }
            if (shared != null) {
                shared.settle(reserved, spent);
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
