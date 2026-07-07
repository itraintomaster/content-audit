package com.learney.contentaudit.refinerdomain;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.auditdomain.CourseDiagnoses;
import com.learney.contentaudit.auditdomain.NodeDiagnoses;
import com.learney.contentaudit.auditdomain.coca.CocaProgressionDiagnosis;
import com.learney.contentaudit.auditdomain.coca.ImprovementDirective;
import com.learney.contentaudit.auditdomain.coca.ImprovementDirectiveType;
import com.learney.contentaudit.auditdomain.lemmacount.LemmaCountStats;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Stateless helper for the six-tier priority ranking of suggested lemmas.
 *
 * Implements F-SLEM-R003, R005, R007, R014, R015: first-match-wins on six tiers
 * (sub-exposed+band, absent+band, sub-exposed, absent, exposed-scarce+band, exposed-scarce),
 * with COCA ascending as the final tiebreak within each tier.
 *
 * Shared by LemmaAbsenceContextResolver and LemmaAbsenceContextSuggestedLemmaQueryPort to
 * keep the tier logic DRY. Both callers own the responsibility of supplying the correct
 * effective CefrLevel (the one used to look up the ENRICH bands for the quiz/query level).
 */
public class SuggestedLemmaTierRanker {

    // Six-tier priority constants for F-SLEM-R003 / R014 ranking (first-match-wins)
    public static final int TIER_1_SUBEXPOSED_BAND = 1; // sub-exposed (1..N-1) + deficient band
    public static final int TIER_2_ABSENT_BAND     = 2; // absent (no exposure) + deficient band
    public static final int TIER_3_SUBEXPOSED      = 3; // sub-exposed (1..N-1), no band
    public static final int TIER_4_ABSENT          = 4; // absent (no exposure), no band
    public static final int TIER_5_SCARCE_BAND     = 5; // exposed-scarce (N..2N-1) + deficient band
    public static final int TIER_6_SCARCE          = 6; // exposed-scarce (N..2N-1), no band

    // Prevent instantiation — all methods are static
    private SuggestedLemmaTierRanker() {}

    /**
     * Computes the 6-tier priority for a non-COMPLETELY_ABSENT lemma given its
     * lemma-count stats, threshold N, COCA rank, and deficient ENRICH bands.
     *
     * F-SLEM-R014: sub-exposed = count in 1..N-1; exposed-scarce = count >= N.
     * When stats are null the lemma has no observable exposure — treated as absent.
     */
    public static int computeTier(LemmaCountStats stats, Integer threshold,
            Integer cocaRank, List<int[]> enrichBands) {
        boolean inBand = isInEnrichBand(cocaRank, enrichBands);
        if (stats == null) {
            // No stats → treat as absent (no observable exposure)
            return inBand ? TIER_2_ABSENT_BAND : TIER_4_ABSENT;
        }
        if (isSubExposed(stats, threshold)) {
            return inBand ? TIER_1_SUBEXPOSED_BAND : TIER_3_SUBEXPOSED;
        }
        // score >= 1.0 or count >= N → exposed-scarce
        return inBand ? TIER_5_SCARCE_BAND : TIER_6_SCARCE;
    }

    /**
     * Computes the tier for a SuggestedLemma whose exposure state is encoded in the
     * isUnderexposed flag (populated by the level-wide inventory and the resolver).
     *
     * Convention used here:
     *   Boolean.TRUE  → sub-exposed (T1 or T3)
     *   Boolean.FALSE → exposed-scarce (T5 or T6)
     *   null          → absent (T2 or T4)
     *
     * This is the tier entry-point for the query port, which operates on already-built
     * SuggestedLemma objects rather than raw LemmaCountStats.
     */
    public static int tierForSuggestedLemma(SuggestedLemma sl, List<int[]> enrichBands) {
        Boolean isUnderexposed = sl.getIsUnderexposed();
        Integer cocaRank = sl.getCocaRank();
        boolean inBand = isInEnrichBand(cocaRank, enrichBands);

        if (Boolean.TRUE.equals(isUnderexposed)) {
            return inBand ? TIER_1_SUBEXPOSED_BAND : TIER_3_SUBEXPOSED;
        }
        if (Boolean.FALSE.equals(isUnderexposed)) {
            return inBand ? TIER_5_SCARCE_BAND : TIER_6_SCARCE;
        }
        // null → absent
        return inBand ? TIER_2_ABSENT_BAND : TIER_4_ABSENT;
    }

    /**
     * Returns a comparator for SuggestedLemma that applies the 6-tier ranking with
     * COCA ascending as the final tiebreak within each tier (F-SLEM-R005).
     *
     * When enrichBands is empty (no band analysis), tiers 1, 2 and 5 stay empty and
     * the same comparator degrades to 3 → 4 → 6 (F-SLEM-R015).
     */
    public static Comparator<SuggestedLemma> tierComparator(List<int[]> enrichBands) {
        return Comparator
                .comparingInt((SuggestedLemma sl) -> tierForSuggestedLemma(sl, enrichBands))
                .thenComparingInt(sl ->
                        sl.getCocaRank() == null || sl.getCocaRank() == 0
                                ? Integer.MAX_VALUE : sl.getCocaRank());
    }

    /**
     * Returns true if the lemma is sub-exposed: count is in [1, N-1].
     * When threshold is null, uses score < 1.0 as proxy (R014 fallback).
     */
    public static boolean isSubExposed(LemmaCountStats stats, Integer threshold) {
        if (stats == null) {
            return false;
        }
        if (threshold != null) {
            return stats.getCount() >= 1 && stats.getCount() < threshold;
        }
        // Fallback: score < 1.0 means sub-exposed
        return stats.getScore() < 1.0;
    }

    /**
     * Extracts the ENRICH frequency bands for the given CEFR level from the course root node.
     * Filters directives of type ENRICH whose levelName matches the level name (level-wide rows
     * only, not quarterly rows containing " Q"). Returns an empty list when no bands are
     * available (F-SLEM-R015 degradation).
     */
    public static List<int[]> extractEnrichBands(AuditNode courseRoot, CefrLevel level) {
        if (courseRoot == null || level == null) {
            return Collections.emptyList();
        }
        NodeDiagnoses courseDiag = courseRoot.getDiagnoses();
        if (!(courseDiag instanceof CourseDiagnoses)) {
            return Collections.emptyList();
        }
        Optional<CocaProgressionDiagnosis> maybeProg =
                ((CourseDiagnoses) courseDiag).getCocaBucketsDiagnosis();
        if (maybeProg.isEmpty()) {
            return Collections.emptyList();
        }
        List<ImprovementDirective> directives = maybeProg.get().getImprovementDirectives();
        if (directives == null) {
            return Collections.emptyList();
        }
        String levelName = level.name();
        List<int[]> bands = new ArrayList<>();
        for (ImprovementDirective d : directives) {
            if (d.getType() == ImprovementDirectiveType.ENRICH
                    && levelName.equals(d.getLevelName())
                    && !d.getLevelName().contains(" Q")) {
                bands.add(new int[]{d.getFrequencyRangeFrom(), d.getFrequencyRangeTo()});
            }
        }
        return bands;
    }

    /**
     * Returns true if the cocaRank falls within any of the given ENRICH bands (inclusive).
     * Null or zero cocaRank is never in a band.
     */
    public static boolean isInEnrichBand(Integer cocaRank, List<int[]> bands) {
        if (cocaRank == null || cocaRank == 0 || bands == null || bands.isEmpty()) {
            return false;
        }
        for (int[] band : bands) {
            if (cocaRank >= band[0] && cocaRank <= band[1]) {
                return true;
            }
        }
        return false;
    }
}
