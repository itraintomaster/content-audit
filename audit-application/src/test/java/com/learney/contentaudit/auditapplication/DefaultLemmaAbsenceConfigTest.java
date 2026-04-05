package com.learney.contentaudit.auditapplication;

import static org.junit.jupiter.api.Assertions.*;

import com.learney.contentaudit.auditdomain.CefrLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class DefaultLemmaAbsenceConfigTest {

    private final DefaultLemmaAbsenceConfig config = new DefaultLemmaAbsenceConfig();

    @Test
    @DisplayName("should return absolute threshold 0 for A1")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R021")
    public void shouldReturnAbsoluteThreshold0ForA1() {
        assertEquals(0, config.getAbsoluteThreshold(CefrLevel.A1));
    }

    @Test
    @DisplayName("should return absolute threshold 2 for A2")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R021")
    public void shouldReturnAbsoluteThreshold2ForA2() {
        assertEquals(2, config.getAbsoluteThreshold(CefrLevel.A2));
    }

    @Test
    @DisplayName("should return absolute threshold 5 for B1")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R021")
    public void shouldReturnAbsoluteThreshold5ForB1() {
        assertEquals(5, config.getAbsoluteThreshold(CefrLevel.B1));
    }

    @Test
    @DisplayName("should return absolute threshold 8 for B2")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R021")
    public void shouldReturnAbsoluteThreshold8ForB2() {
        assertEquals(8, config.getAbsoluteThreshold(CefrLevel.B2));
    }

    @Test
    @DisplayName("should return percentage threshold 0.0 for A1")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R021")
    public void shouldReturnPercentageThreshold00ForA1() {
        assertEquals(0.0, config.getPercentageThreshold(CefrLevel.A1), 0.001);
    }

    @Test
    @DisplayName("should return percentage threshold 5.0 for A2")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R021")
    public void shouldReturnPercentageThreshold50ForA2() {
        assertEquals(5.0, config.getPercentageThreshold(CefrLevel.A2), 0.001);
    }

    @Test
    @DisplayName("should return percentage threshold 10.0 for B1")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R021")
    public void shouldReturnPercentageThreshold100ForB1() {
        assertEquals(10.0, config.getPercentageThreshold(CefrLevel.B1), 0.001);
    }

    @Test
    @DisplayName("should return percentage threshold 15.0 for B2")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R021")
    public void shouldReturnPercentageThreshold150ForB2() {
        assertEquals(15.0, config.getPercentageThreshold(CefrLevel.B2), 0.001);
    }

    @Test
    @DisplayName("should return level weight 2.0 for A1")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R024")
    public void shouldReturnLevelWeight20ForA1() {
        assertEquals(2.0, config.getLevelWeight(CefrLevel.A1), 0.001);
    }

    @Test
    @DisplayName("should return level weight 2.0 for A2")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R024")
    public void shouldReturnLevelWeight20ForA2() {
        assertEquals(2.0, config.getLevelWeight(CefrLevel.A2), 0.001);
    }

    @Test
    @DisplayName("should return level weight 1.0 for B1")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R024")
    public void shouldReturnLevelWeight10ForB1() {
        assertEquals(1.0, config.getLevelWeight(CefrLevel.B1), 0.001);
    }

    @Test
    @DisplayName("should return level weight 1.0 for B2")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R024")
    public void shouldReturnLevelWeight10ForB2() {
        assertEquals(1.0, config.getLevelWeight(CefrLevel.B2), 0.001);
    }

    @Test
    @DisplayName("should return high priority bound of 1000")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R011")
    public void shouldReturnHighPriorityBoundOf1000() {
        assertEquals(1000, config.getHighPriorityBound());
    }

    @Test
    @DisplayName("should return medium priority bound of 3000")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R011")
    public void shouldReturnMediumPriorityBoundOf3000() {
        assertEquals(3000, config.getMediumPriorityBound());
    }

    @Test
    @DisplayName("should return low priority bound of 5000")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R011")
    public void shouldReturnLowPriorityBoundOf5000() {
        assertEquals(5000, config.getLowPriorityBound());
    }

    @Test
    @DisplayName("should return high priority alert threshold of 0")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R014")
    public void shouldReturnHighPriorityAlertThresholdOf0() {
        assertEquals(0, config.getHighPriorityAlertThreshold());
    }

    @Test
    @DisplayName("should return medium priority alert threshold of 3")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R014")
    public void shouldReturnMediumPriorityAlertThresholdOf3() {
        assertEquals(3, config.getMediumPriorityAlertThreshold());
    }

    @Test
    @DisplayName("should return low priority alert threshold of 10")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R014")
    public void shouldReturnLowPriorityAlertThresholdOf10() {
        assertEquals(10, config.getLowPriorityAlertThreshold());
    }

    @Test
    @DisplayName("should return critical absence threshold of 10")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R025")
    public void shouldReturnCriticalAbsenceThresholdOf10() {
        assertEquals(10, config.getCriticalAbsenceThreshold());
    }

    @Test
    @DisplayName("should return acceptable absence threshold of 5")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R025")
    public void shouldReturnAcceptableAbsenceThresholdOf5() {
        assertEquals(5, config.getAcceptableAbsenceThreshold());
    }

    @Test
    @DisplayName("should return high report limit of 20")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R026")
    public void shouldReturnHighReportLimitOf20() {
        assertEquals(20, config.getHighReportLimit());
    }

    @Test
    @DisplayName("should return medium report limit of 30")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R026")
    public void shouldReturnMediumReportLimitOf30() {
        assertEquals(30, config.getMediumReportLimit());
    }

    @Test
    @DisplayName("should return low report limit of 50")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R026")
    public void shouldReturnLowReportLimitOf50() {
        assertEquals(50, config.getLowReportLimit());
    }

    @Test
    @DisplayName("should return discount per level of 0.1")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R018")
    public void shouldReturnDiscountPerLevelOf01() {
        assertEquals(0.1, config.getDiscountPerLevel(), 0.001);
    }

    @Test
    @DisplayName("should have absolute thresholds increasing from A1 to B2")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R021")
    public void shouldHaveAbsoluteThresholdsIncreasingFromA1ToB2() {
        assertTrue(config.getAbsoluteThreshold(CefrLevel.A1) < config.getAbsoluteThreshold(CefrLevel.A2));
        assertTrue(config.getAbsoluteThreshold(CefrLevel.A2) < config.getAbsoluteThreshold(CefrLevel.B1));
        assertTrue(config.getAbsoluteThreshold(CefrLevel.B1) < config.getAbsoluteThreshold(CefrLevel.B2));
    }

    @Test
    @DisplayName("should have percentage thresholds increasing from A1 to B2")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R021")
    public void shouldHavePercentageThresholdsIncreasingFromA1ToB2() {
        assertTrue(config.getPercentageThreshold(CefrLevel.A1) < config.getPercentageThreshold(CefrLevel.A2));
        assertTrue(config.getPercentageThreshold(CefrLevel.A2) < config.getPercentageThreshold(CefrLevel.B1));
        assertTrue(config.getPercentageThreshold(CefrLevel.B1) < config.getPercentageThreshold(CefrLevel.B2));
    }

    @Test
    @DisplayName("should have priority bounds ordered high less than medium less than low")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R011")
    public void shouldHavePriorityBoundsOrderedHighLessThanMediumLessThanLow() {
        assertTrue(config.getHighPriorityBound() < config.getMediumPriorityBound());
        assertTrue(config.getMediumPriorityBound() < config.getLowPriorityBound());
    }

    @Test
    @DisplayName("should weight critical levels A1 and A2 higher than B1 and B2")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R024")
    public void shouldWeightCriticalLevelsA1AndA2HigherThanB1AndB2() {
        assertTrue(config.getLevelWeight(CefrLevel.A1) > config.getLevelWeight(CefrLevel.B1));
        assertTrue(config.getLevelWeight(CefrLevel.A2) > config.getLevelWeight(CefrLevel.B2));
    }

    @Test
    @DisplayName("should have report limits increasing from high to low priority")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R026")
    public void shouldHaveReportLimitsIncreasingFromHighToLowPriority() {
        assertTrue(config.getHighReportLimit() < config.getMediumReportLimit());
        assertTrue(config.getMediumReportLimit() < config.getLowReportLimit());
    }

    @Test
    @DisplayName("should have critical absence threshold greater than acceptable absence threshold")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R025")
    public void shouldHaveCriticalAbsenceThresholdGreaterThanAcceptableAbsenceThreshold() {
        assertTrue(config.getCriticalAbsenceThreshold() > config.getAcceptableAbsenceThreshold());
    }

    @Test
    @DisplayName("should have alert thresholds non-decreasing from high to low priority")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R014")
    public void shouldHaveAlertThresholdsNondecreasingFromHighToLowPriority() {
        assertTrue(config.getHighPriorityAlertThreshold() <= config.getMediumPriorityAlertThreshold());
        assertTrue(config.getMediumPriorityAlertThreshold() <= config.getLowPriorityAlertThreshold());
    }

    @Test
    @DisplayName("should enforce zero tolerance for high priority alert threshold")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R014")
    public void shouldEnforceZeroToleranceForHighPriorityAlertThreshold() {
        assertEquals(0, config.getHighPriorityAlertThreshold());
    }

    @Test
    @DisplayName("should enforce A1 zero tolerance with both absolute and percentage thresholds at zero")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R021")
    public void shouldEnforceA1ZeroToleranceWithBothAbsoluteAndPercentageThresholdsAtZero() {
        assertEquals(0, config.getAbsoluteThreshold(CefrLevel.A1));
        assertEquals(0.0, config.getPercentageThreshold(CefrLevel.A1), 0.001);
    }

    @Test
    @DisplayName("should have discount per level that limits max penalty to 0.3 for three-level distance")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R018")
    public void shouldHaveDiscountPerLevelThatLimitsMaxPenaltyTo03ForThreelevelDistance() {
        // Max distance is 3 (A1 to B2). discount=0.1 * 3 = 0.3
        assertEquals(0.3, config.getDiscountPerLevel() * 3, 0.001);
    }

    @Test
    @DisplayName("should return non-negative values for all thresholds and bounds")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R021")
    public void shouldReturnNonnegativeValuesForAllThresholdsAndBounds() {
        for (CefrLevel level : CefrLevel.values()) {
            assertTrue(config.getAbsoluteThreshold(level) >= 0);
            assertTrue(config.getPercentageThreshold(level) >= 0.0);
        }
        assertTrue(config.getHighPriorityBound() >= 0);
        assertTrue(config.getMediumPriorityBound() >= 0);
        assertTrue(config.getLowPriorityBound() >= 0);
        assertTrue(config.getCriticalAbsenceThreshold() >= 0);
        assertTrue(config.getAcceptableAbsenceThreshold() >= 0);
    }

    @Test
    @DisplayName("should return positive report limits for all priority levels")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R026")
    public void shouldReturnPositiveReportLimitsForAllPriorityLevels() {
        assertTrue(config.getHighReportLimit() > 0);
        assertTrue(config.getMediumReportLimit() > 0);
        assertTrue(config.getLowReportLimit() > 0);
    }

    @Test
    @DisplayName("should return percentage thresholds between 0 and 100 for all levels")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R021")
    public void shouldReturnPercentageThresholdsBetween0And100ForAllLevels() {
        for (CefrLevel level : CefrLevel.values()) {
            double pct = config.getPercentageThreshold(level);
            assertTrue(pct >= 0.0 && pct <= 100.0,
                    "Percentage threshold for " + level + " should be between 0 and 100, was " + pct);
        }
    }

    @Test
    @DisplayName("should return positive level weights for all CEFR levels")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R024")
    public void shouldReturnPositiveLevelWeightsForAllCEFRLevels() {
        for (CefrLevel level : CefrLevel.values()) {
            assertTrue(config.getLevelWeight(level) > 0.0);
        }
    }

    @Test
    @DisplayName("should return discount per level between 0 exclusive and 1 exclusive")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R018")
    public void shouldReturnDiscountPerLevelBetween0ExclusiveAnd1Exclusive() {
        double discount = config.getDiscountPerLevel();
        assertTrue(discount > 0.0 && discount < 1.0);
    }

    @Test
    @DisplayName("should return coverage target 0.95 for A1")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R032")
    public void shouldReturnCoverageTarget095ForA1() {
        assertEquals(0.95, config.getCoverageTarget(CefrLevel.A1), 0.001);
    }

    @Test
    @DisplayName("should return coverage target 0.85 for A2")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R032")
    public void shouldReturnCoverageTarget085ForA2() {
        assertEquals(0.85, config.getCoverageTarget(CefrLevel.A2), 0.001);
    }

    @Test
    @DisplayName("should return coverage target 0.70 for B1")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R032")
    public void shouldReturnCoverageTarget070ForB1() {
        assertEquals(0.70, config.getCoverageTarget(CefrLevel.B1), 0.001);
    }

    @Test
    @DisplayName("should return coverage target 0.55 for B2")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R032")
    public void shouldReturnCoverageTarget055ForB2() {
        assertEquals(0.55, config.getCoverageTarget(CefrLevel.B2), 0.001);
    }

    @Test
    @DisplayName("should have coverage targets decreasing from A1 to B2")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R032")
    public void shouldHaveCoverageTargetsDecreasingFromA1ToB2() {
        assertTrue(config.getCoverageTarget(CefrLevel.A1) > config.getCoverageTarget(CefrLevel.A2));
        assertTrue(config.getCoverageTarget(CefrLevel.A2) > config.getCoverageTarget(CefrLevel.B1));
        assertTrue(config.getCoverageTarget(CefrLevel.B1) > config.getCoverageTarget(CefrLevel.B2));
    }

    @Test
    @DisplayName("should return coverage targets between 0 and 1 for all levels")
    @Tag("FEAT-LABS")
    @Tag("F-LABS-R032")
    public void shouldReturnCoverageTargetsBetween0And1ForAllLevels() {
        for (CefrLevel level : CefrLevel.values()) {
            double target = config.getCoverageTarget(level);
            assertTrue(target > 0.0 && target <= 1.0,
                    "Coverage target for " + level + " should be between 0 and 1, was " + target);
        }
    }
}
