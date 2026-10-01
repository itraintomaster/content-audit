package com.learney.contentaudit.revisiondomain.batchengine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerFamily;
import com.learney.contentaudit.revisiondomain.batch.AnalyzerLevelErrors;
import com.learney.contentaudit.revisiondomain.batch.BatchClosing;
import com.learney.contentaudit.revisiondomain.batch.BatchReport;
import com.learney.contentaudit.revisiondomain.batch.CorrectionBatch;
import com.learney.contentaudit.revisiondomain.comparison.BatchComparison;
import com.learney.contentaudit.revisiondomain.comparison.ComparisonRow;
import com.learney.contentaudit.revisiondomain.comparison.GoalUnit;
import com.learney.contentaudit.revisiondomain.comparison.RowValue;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * F-TANDA-R010, per analyzer and level, with what the control of package E over the copy of 29/9 showed: F compares
 * the four levels, and the report of the batch of A1 called every one of them «A1» -- the 306 exercises with an error
 * of A2 read as if they were A1's.
 */
public class BatchReportLevelsIntegrationTest {

    @Test
    @DisplayName("should give each row of the report the level of its own milestone, not the level of the batch")
    @Tag("FEAT-TANDA")
    @Tag("F-TANDA-R010")
    public void shouldGiveEachRowTheLevelOfItsOwnMilestone() {
        CorrectionBatch batch = TandaFixture.batch("tanda-a1");
        BatchComparison comparison = TandaFixture.passing(null, List.of());
        comparison.setRows(new ArrayList<>(List.of(
                milestone("algún error", "m-a1", "A1", 711.0, 116.0),
                milestone("algún error", "m-a2", "A2", 306.0, 306.0),
                milestone("algún error", "m-b1", "b1", 246.0, 246.0),
                milestone("algún error", "m-extra", "Repaso", 4.0, 4.0))));
        batch.setClosings(new ArrayList<>(List.of(new BatchClosing(TandaFixture.OPENED_AT, null, comparison, null,
                List.of(), 1))));

        BatchReport report = new DefaultBatchReportBuilder().build(batch);

        assertEquals(List.of(new AnalyzerLevelErrors("algún error", CefrLevel.A1, 711, 116),
                        new AnalyzerLevelErrors("algún error", CefrLevel.A2, 306, 306),
                        new AnalyzerLevelErrors("algún error", CefrLevel.B1, 246, 246)),
                report.getErrorsByAnalyzer(), "a milestone that is not a level is not a row of this list");
    }

    private static ComparisonRow milestone(String subject, String id, String label, double before, double after) {
        return new ComparisonRow(subject, AnalyzerFamily.ERRORS, AuditTarget.MILESTONE, id, label,
                new RowValue(before, null, 0, true), null, new RowValue(before, null, 0, true), new RowValue(after, null, 0, true),
                after - before, GoalUnit.EXERCISES, null, null, null, null, null, null, null);
    }
}
