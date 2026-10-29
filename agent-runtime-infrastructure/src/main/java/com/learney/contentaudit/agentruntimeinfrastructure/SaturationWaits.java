package com.learney.contentaudit.agentruntimeinfrastructure;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * F-SIGUE-R001: how a judge waits for a saturated model before it gives up on it. A saturation
 * («model is at capacity») passes by itself, unlike an exhausted quota or an expired session, so
 * the same exercise is asked again after each wait; only when the model is still saturated after
 * the last one does the judge become unavailable, as any other service failure. F-SIGUE-R004: the
 * corrector waits the same way before it gives up on a proposal.
 */
public final class SaturationWaits {

    /** What the waits sleep on; a test sleeps on nothing and records how long it was told. */
    @FunctionalInterface
    public interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    /** 30 seconds, then 1, 2, 4, 8 and 15 minutes: about half an hour in all. */
    public static final List<Duration> STANDARD = List.of(Duration.ofSeconds(30), Duration.ofMinutes(1),
            Duration.ofMinutes(2), Duration.ofMinutes(4), Duration.ofMinutes(8), Duration.ofMinutes(15));

    private final List<Duration> waits;

    private final Sleeper sleeper;

    public SaturationWaits(List<Duration> waits, Sleeper sleeper) {
        this.waits = List.copyOf(Objects.requireNonNull(waits, "waits"));
        this.sleeper = Objects.requireNonNull(sleeper, "sleeper");
    }

    /** F-SIGUE-R004: whether a failure says the model is saturated, as the judges read it («at capacity», «overloaded»). */
    public static boolean saturated(String failure) {
        String text = failure != null ? failure.toLowerCase(Locale.ROOT) : "";
        return text.contains("at capacity") || text.contains("overloaded");
    }

    public static SaturationWaits standard() {
        return new SaturationWaits(STANDARD, duration -> Thread.sleep(duration.toMillis()));
    }

    /**
     * Asks once and, while the answer is a saturation, waits and asks again. The last answer is
     * returned as it is: a judgment, another failure, or the saturation that outlasted every wait.
     */
    public <T> T consult(Supplier<T> once, Predicate<T> saturated) {
        T answer = once.get();
        for (Duration wait : waits) {
            if (!saturated.test(answer)) {
                return answer;
            }
            try {
                sleeper.sleep(wait);
            } catch (InterruptedException e) {
                // A cancelled run does not keep waiting: the saturation stays its answer.
                Thread.currentThread().interrupt();
                return answer;
            }
            answer = once.get();
        }
        return answer;
    }
}
