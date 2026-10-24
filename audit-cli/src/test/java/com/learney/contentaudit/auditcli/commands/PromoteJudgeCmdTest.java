package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.auditdomain.JudgePromotionRegistry;
import com.learney.contentaudit.auditdomain.JudgeExamRegistry;
import com.learney.contentaudit.auditdomain.JudgeRegressionCatalog;
import com.learney.contentaudit.auditdomain.judging.*;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PromoteJudgeCmdTest {
    @TempDir Path directory;
    @Test void recordsHumanDecisionWithEvidenceAndEffectiveIdentity() throws Exception {
        var store=mock(JudgePromotionRegistry.class);when(store.latest(any(),any(),any())).thenReturn(Optional.empty());
        var identity=EvaluationRuntimeIdentity.explicit("codex-cli","synthetic","medium");
        var standing=new JudgeExamStanding("naturalness","v1",JudgeExamStatus.NOT_TAKEN,false,"No exam",identity);
        var file=directory.resolve("approval.json");Files.writeString(file,"{\"decision\":\"promote\"}");
        var cli=new CommandLine(new PromoteJudgeCmd(store,()->Map.of("naturalness",standing)));
        assertEquals(0,cli.execute("naturalness","--expected-version","v1","--reviewer","José","--evidence",file.toString(),"--reason","Acepto el piloto"));
        verify(store).record(argThat(r->r.judgeVersion().equals("v1") && r.runtimeIdentity().equals(identity) && r.evidenceSha256().length()==64 && r.reviewer().equals("José")));
    }
    @Test void staleVersionOrMissingEvidenceNeverPromotes() throws Exception {
        var store=mock(JudgePromotionRegistry.class);var identity=EvaluationRuntimeIdentity.explicit("codex-cli","synthetic","medium");
        var standing=new JudgeExamStanding("naturalness","v2",JudgeExamStatus.NOT_TAKEN,false,"No exam",identity);
        var cli=new CommandLine(new PromoteJudgeCmd(store,()->Map.of("naturalness",standing)));
        var missing=directory.resolve("missing.json");
        assertEquals(2,cli.execute("naturalness","--expected-version","v1","--reviewer","José","--evidence",missing.toString(),"--reason","Acepto el piloto"));
        assertEquals(2,cli.execute("naturalness","--expected-version","v2","--reviewer","José","--evidence",missing.toString(),"--reason","Acepto el piloto"));
        verify(store,never()).record(any());
    }

    @Test void acceptsOnlyTheCompleteExactExceptionAndKeepsTheFailedMeasurement() throws Exception {
        var fixture=new ExceptionFixture();
        assertEquals(0,fixture.run(fixture.scope()));
        verify(fixture.store).record(argThat(r -> r.examException()!=null
                && r.coversExamException(fixture.measured,fixture.suite)
                && r.examException().acceptedMismatchedCaseIds().equals(List.of("quiz-watch"))));
        assertEquals(JudgeExamStatus.FAILED,fixture.measured.getStatus());
        assertFalse(fixture.measured.isGoalMet());
        assertEquals(62,fixture.measured.getCasesJudged());
        assertEquals(List.of("quiz-watch"),fixture.measured.getMismatchedCaseIds());
        verify(fixture.exams,never()).record(any());
    }

    @Test void permanentSuiteCannotBePromotedWithMissingOrDifferentScope() throws Exception {
        for(var invalid : List.of(
                List.<String>of(), List.of("--exam-fingerprint","exam-1"),
                changedScope("--exam-fingerprint","different"),
                changedScope("--regression-suite-fingerprint","different"),
                changedScope("--exam-recorded-at","2026-10-21T19:00:00Z"),
                changedScope("--exam-recorded-at","invalid"),
                changedScope("--accept-regression-case","REG-other"),
                changedScope("--accept-case","quiz-other"),
                changedScope("--accept-case","quiz-watch,quiz-other"))) {
            var fixture=new ExceptionFixture();
            assertEquals(2,fixture.run(invalid),invalid.toString());
            verify(fixture.store,never()).record(any());
        }
    }

    @Test void newerOrUnmeasuredExamCannotUseAnOldAcceptance() throws Exception {
        var newer=new ExceptionFixture();
        newer.measured.setRecordedAt(Instant.parse("2026-10-22T19:00:01Z"));
        assertEquals(2,newer.run(newer.scope()));
        verify(newer.store,never()).record(any());
        var taken=new ExceptionFixture();
        taken.measured.setStatus(JudgeExamStatus.TAKEN);
        assertEquals(2,taken.run(taken.scope()));
        verify(taken.store,never()).record(any());
    }

    @Test void repeatingTheSameDecisionIsIdempotentButDoesNotBorrowAGenericPromotion() throws Exception {
        var fixture=new ExceptionFixture();
        var captured=org.mockito.ArgumentCaptor.forClass(JudgePromotionRecord.class);
        assertEquals(0,fixture.run(fixture.scope()));
        verify(fixture.store).record(captured.capture());
        var prior=captured.getValue();
        clearInvocations(fixture.store);
        when(fixture.store.latest(any(),any(),any())).thenReturn(Optional.of(prior));
        assertEquals(0,fixture.run(fixture.scope()));
        verify(fixture.store,never()).record(any());
        var generic=new JudgePromotionRecord(prior.judgeName(),prior.judgeVersion(),prior.runtimeIdentity(),
                prior.reviewer(),prior.reason(),prior.evidenceFile(),prior.evidenceSha256(),prior.promotedAt());
        when(fixture.store.latest(any(),any(),any())).thenReturn(Optional.of(generic));
        assertEquals(0,fixture.run(fixture.scope()));
        verify(fixture.store).record(argThat(r -> r.examException()!=null));
    }

    @Test void acceptsExactHistoricalDiscrepanciesWithoutInventingPermanentRegressionIds() throws Exception {
        var fixture = new HistoricalExceptionFixture();
        assertEquals(0, fixture.run(fixture.scope()));
        verify(fixture.store).record(argThat(r -> r.examException() != null
                && r.examException().acceptedRegressionCaseIds().isEmpty()
                && r.examException().acceptedMismatchedCaseIds().equals(List.of("quiz-a", "quiz-b"))
                && r.coversExamException(fixture.measured, fixture.suite)));
        assertEquals(JudgeExamStatus.FAILED, fixture.measured.getStatus());
        assertEquals(63, fixture.measured.getCasesJudged());
        verify(fixture.exams, never()).record(any());
    }

    @Test void historicalExceptionStillRequiresExactMeasuredCasesAndCannotCoverANewSuite() throws Exception {
        for (var changed : List.of("--exam-fingerprint", "--regression-suite-fingerprint", "--accept-case")) {
            var fixture = new HistoricalExceptionFixture();
            var scope = new ArrayList<>(fixture.scope());
            scope.set(scope.indexOf(changed) + 1, "foreign");
            assertEquals(2, fixture.run(scope));
            verify(fixture.store, never()).record(any());
        }
        var unmeasured = new HistoricalExceptionFixture();
        unmeasured.measured.setStatus(JudgeExamStatus.TAKEN);
        assertEquals(2, unmeasured.run(unmeasured.scope()));
        verify(unmeasured.store, never()).record(any());
        var missing = new HistoricalExceptionFixture();
        when(missing.exams.latest(any(), any(), any())).thenReturn(Optional.empty());
        assertEquals(2, missing.run(missing.scope()));
        verify(missing.store, never()).record(any());
        var changedSuite = new HistoricalExceptionFixture();
        changedSuite.suite = new JudgeRegressionSuite("new-suite", List.of(new JudgeRegressionCase(
                "REG-new", "single-answer", "problem", "Title", "case", "quiz-new", true,
                "Problem", "Editorial reference", Map.of(), Instant.EPOCH)));
        assertEquals(2, changedSuite.run(changedSuite.scope()));
        verify(changedSuite.store, never()).record(any());
    }

    @Test void historicalExceptionRequiresExplicitNoneAndRejectsPermanentIdsAlongsideIt() throws Exception {
        var missing = new HistoricalExceptionFixture();
        var omitted = new ArrayList<>(missing.scope());
        int index = omitted.indexOf("--regression-suite-fingerprint");
        omitted.remove(index + 1); omitted.remove(index);
        assertEquals(2, missing.run(omitted));
        verify(missing.store, never()).record(any());
        var conflicting = new HistoricalExceptionFixture();
        var withPermanent = new ArrayList<>(conflicting.scope());
        withPermanent.addAll(List.of("--accept-regression-case", "REG-invented"));
        assertEquals(2, conflicting.run(withPermanent));
        verify(conflicting.store, never()).record(any());
        var permanent = new ExceptionFixture();
        assertEquals(2, permanent.run(changedScope("--regression-suite-fingerprint", "none")));
        verify(permanent.store, never()).record(any());
    }

    private class HistoricalExceptionFixture {
        final JudgePromotionRegistry store = mock(JudgePromotionRegistry.class);
        final JudgeExamRegistry exams = mock(JudgeExamRegistry.class);
        final EvaluationRuntimeIdentity identity = EvaluationRuntimeIdentity.explicit("codex-cli", "synthetic", "medium");
        JudgeRegressionSuite suite = JudgeRegressionSuite.empty();
        final JudgeExamRecord measured = new JudgeExamRecord("single-answer", "v1", JudgeExamStatus.FAILED,
                63, 0, List.of(), List.of("quiz-a", "quiz-b"), false, "photo", "fixed-meter",
                Instant.parse("2026-10-22T19:00:00Z"));
        final Path evidence;
        HistoricalExceptionFixture() throws Exception {
            evidence = Files.createTempFile(directory, "approval-historical-", ".json");
            Files.writeString(evidence, "{\"decision\":\"test fixture accepts quiz-a and quiz-b only\"}");
            measured.setRuntimeIdentity(identity);
            measured.setExamFingerprint("exam-historical");
            measured.setRegressionSuiteFingerprint(suite.fingerprint());
            measured.setRegressionCaseIds(List.of());
            measured.setRegressionMismatchedCaseIds(List.of());
            when(exams.latest("single-answer", "v1", identity)).thenReturn(Optional.of(measured));
            when(store.latest(any(), any(), any())).thenReturn(Optional.empty());
        }
        List<String> scope() {
            return List.of("--exam-fingerprint", "exam-historical", "--regression-suite-fingerprint",
                    "none", "--exam-recorded-at", "2026-10-22T19:00:00Z",
                    "--accept-case", "quiz-a,quiz-b");
        }
        int run(List<String> scope) {
            var args = new ArrayList<>(List.of("single-answer", "--expected-version", "v1", "--reviewer", "Reviewer",
                    "--evidence", evidence.toString(), "--reason", "Accept only the test fixture discrepancies"));
            args.addAll(scope);
            var standing = new JudgeExamStanding("single-answer", "v1", JudgeExamStatus.FAILED, false, "Discrepancy", identity);
            return new CommandLine(new PromoteJudgeCmd(store, () -> Map.of("single-answer", standing), exams, ignored -> suite))
                    .execute(args.toArray(String[]::new));
        }
    }

    private static List<String> changedScope(String option,String value) {
        var result=new ArrayList<>(ExceptionFixture.scope());
        result.set(result.indexOf(option)+1,value);
        return result;
    }

    private class ExceptionFixture {
        final JudgePromotionRegistry store=mock(JudgePromotionRegistry.class);
        final JudgeExamRegistry exams=mock(JudgeExamRegistry.class);
        final EvaluationRuntimeIdentity identity=EvaluationRuntimeIdentity.explicit("codex-cli","synthetic","medium");
        final JudgeRegressionSuite suite=new JudgeRegressionSuite("suite-1",List.of(
                new JudgeRegressionCase("REG-watch","mini-theory","PRB-test","Resaltado","watch",
                        "quiz-watch",false,"Resaltado completo","Referencia editorial",Map.of(),Instant.EPOCH)));
        final JudgeExamRecord measured=new JudgeExamRecord("mini-theory","v1",JudgeExamStatus.FAILED,
                62,0,List.of(),List.of("quiz-watch"),false,"photo","fixed-meter",Instant.parse("2026-10-22T19:00:00Z"));
        final Path evidence;
        ExceptionFixture() throws Exception {
            evidence=Files.createTempFile(directory,"approval-",".json");
            Files.writeString(evidence,"{\"decision\":\"accept watch only; preserve 61/62\"}");
            measured.setRuntimeIdentity(identity);
            measured.setExamFingerprint("exam-1");
            measured.setRegressionSuiteFingerprint("suite-1");
            measured.setRegressionCaseIds(List.of("REG-watch"));
            measured.setRegressionMismatchedCaseIds(List.of("REG-watch"));
            when(exams.latest("mini-theory","v1",identity)).thenReturn(Optional.of(measured));
            when(store.latest(any(),any(),any())).thenReturn(Optional.empty());
        }
        static List<String> scope() {
            return List.of("--exam-fingerprint","exam-1","--regression-suite-fingerprint","suite-1",
                    "--exam-recorded-at","2026-10-22T19:00:00Z","--accept-regression-case","REG-watch","--accept-case","quiz-watch");
        }
        int run(List<String> scope) {
            var args=new ArrayList<>(List.of("mini-theory","--expected-version","v1","--reviewer","José",
                    "--evidence",evidence.toString(),"--reason","Acepto watch; conserva 61/62"));
            args.addAll(scope);
            var standing=new JudgeExamStanding("mini-theory","v1",JudgeExamStatus.FAILED,false,"Discrepancia",identity);
            JudgeRegressionCatalog catalog=ignored -> suite;
            return new CommandLine(new PromoteJudgeCmd(store,()->Map.of("mini-theory",standing),exams,catalog))
                    .execute(args.toArray(String[]::new));
        }
    }
}
