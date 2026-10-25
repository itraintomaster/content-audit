package com.learney.contentaudit.revisiondomain.batchengine;

import static org.junit.jupiter.api.Assertions.*;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.analysis.DefaultAnalysisComposer;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** F-OPINA-R001: the list of what is missing, as the analysis read and «Completar opiniones» count it. */
class MissingOpinionsTest {
    final AuditReport report = TandaFixture.report(TandaFixture.course(), List.of(), Map.of(), List.of());
    final Map<String, AuditNode> index = DefaultAnalysisComposer.index(report.getRoot());

    @Test @Tag("FEAT-OPINA") @Tag("F-OPINA-R001")
    void aPairIsMissingWhereTheJudgeAppliesAndLeftNoOpinion() {
        index.get(TandaFixture.CHILD).getScores().put("naturalness", 1.0);

        var missing = MissingOpinions.of(report, List.of("naturalness", "multiple-choice-quality"));

        assertFalse(missing.get("naturalness").contains(TandaFixture.CHILD), "A verdict with a score is an opinion");
        assertTrue(missing.get("naturalness").contains(TandaFixture.RESIDE));
        // The judge of options applies only to the exercise with options.
        assertEquals(List.of(TandaFixture.CHILD), missing.get("multiple-choice-quality"));
        assertEquals(missing.get("naturalness").stream().sorted().toList(), missing.get("naturalness"));
    }

    @Test @Tag("FEAT-OPINA") @Tag("F-OPINA-R001")
    void aSavedAbstentionIsNotMissing() {
        var abstained = index.get(TandaFixture.RESIDE);
        abstained.getUnevaluatedBy().add("naturalness");
        abstained.getMetadata().put("naturalnessInputContract", "english-quality-input-v2");
        abstained.getMetadata().put("naturalnessVersion", "v1");
        abstained.getMetadata().put("naturalnessVerdict", Map.of("assessments", List.of(Map.of("status", "NOT_APPLICABLE"))));

        assertFalse(MissingOpinions.of(report, List.of("naturalness")).get("naturalness").contains(TandaFixture.RESIDE));
    }

    @Test @Tag("FEAT-OPINA") @Tag("F-OPINA-R001")
    void aVerdictOfAnEarlierVersionIsNotMissing() {
        var old = index.get(TandaFixture.RESIDE);
        old.getScores().put("naturalness", 1.0);
        old.getMetadata().put("naturalnessVersion", "a-version-before-today");

        assertFalse(MissingOpinions.of(report, List.of("naturalness")).get("naturalness").contains(TandaFixture.RESIDE));
    }

    @Test @Tag("FEAT-OPINA") @Tag("F-OPINA-R001")
    void onlyTheJudgesAskedForAreCountedAndNoneIsListedEmpty() {
        index.get(TandaFixture.CHILD).getScores().put("multiple-choice-quality", 1.0);

        var missing = MissingOpinions.of(report, List.of("multiple-choice-quality"));

        assertTrue(missing.isEmpty(), "Nothing missing leaves no entry: " + missing);
        assertTrue(MissingOpinions.of(null, List.of("naturalness")).isEmpty());
    }
}
