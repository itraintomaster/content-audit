package com.learney.contentaudit.auditdomain.judgingengine;

import com.learney.contentaudit.auditdomain.JudgeExamRegistry;
import com.learney.contentaudit.auditdomain.judging.JudgeExamStanding;
import com.learney.contentaudit.auditdomain.judging.JudgeFreshnessCatalog;
import com.learney.contentaudit.auditdomain.judging.JudgeFreshnessReference;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * F-ALDIA-R003 and R004: today's judges as their standings say -- opening their sessions consults
 * nothing -- with the exam history of each for the runtimes they approved with and the names of
 * their versions. The standings are read once, the first time a judge is asked for.
 */
public final class DefaultJudgeFreshnessCatalog implements JudgeFreshnessCatalog {
    private final Supplier<Map<String, JudgeExamStanding>> standings;

    private final JudgeExamRegistry examRegistry;

    private final ZoneId zone;

    private final Clock clock;

    private Map<String, JudgeExamStanding> read;

    public DefaultJudgeFreshnessCatalog(Supplier<Map<String, JudgeExamStanding>> standings,
            JudgeExamRegistry examRegistry, ZoneId zone, Clock clock) {
        this.standings = standings;
        this.examRegistry = examRegistry;
        this.zone = zone;
        this.clock = clock;
    }

    @Override
    public synchronized Optional<JudgeFreshnessReference> reference(String judgeName) {
        if (read == null) {
            Map<String, JudgeExamStanding> today = standings != null ? standings.get() : null;
            read = today != null ? today : Map.of();
        }
        JudgeExamStanding standing = judgeName != null ? read.get(judgeName) : null;
        if (standing == null || standing.getJudgeVersion() == null) {
            return Optional.empty();
        }
        if (standing.getJudgeName() == null) {
            standing.setJudgeName(judgeName);
        }
        return Optional.of(JudgeFreshnessReference.of(standing,
                examRegistry != null ? examRegistry.history(judgeName) : null, zone, clock));
    }
}
