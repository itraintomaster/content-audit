package com.learney.contentaudit.auditdomain.englishquality;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.finishedsentence.DefaultFinishedSentenceReader;
import com.learney.contentaudit.auditdomain.judging.JudgeExamRecord;
import com.learney.contentaudit.auditdomain.judging.JudgeExamStatus;
import com.learney.contentaudit.auditdomain.judgingengine.DefaultJudgeRuntime;
import com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.evaluationledgerdomain.*;
import com.learney.contentaudit.evaluationledgerdomain.contentfingerprint.Sha256ContentFingerprinter;
import com.learney.contentaudit.evaluationledgerdomain.evaluationsession.DefaultEvaluationSessionFactory;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnglishQualityScopedConsultationsTest {
    @Test
    void naturalnessAndDidacticOnlyConsultSelectedQuizzesAndReuseThemOnTheNextMeasurement() {
        for (var kind : EnglishQualityAnalyzerProvider.Kind.values()) {
            var records = new HashMap<EvaluationKey, EvaluationRecord>();
            var ledger = mock(EvaluationLedger.class);
            when(ledger.findLatest(any())).thenAnswer(call -> Optional.ofNullable(records.get(call.getArgument(0))));
            when(ledger.history(any())).thenAnswer(call -> {
                var record = records.get(call.getArgument(0));
                return record == null ? List.of() : List.of(record);
            });
            doAnswer(call -> {
                EvaluationRecord record = call.getArgument(0);
                records.put(record.getKey(), record);
                return null;
            }).when(ledger).append(any());
            var sessions = new DefaultEvaluationSessionFactory(ledger, new Sha256ContentFingerprinter());
            var exams = mock(JudgeExamRegistry.class);
            when(exams.latest(eq(kind.name), eq("v-scoped"))).thenReturn(Optional.of(new JudgeExamRecord(
                    kind.name, "v-scoped", JudgeExamStatus.APPROVED, 1, 0, List.of(), List.of(), true,
                    "fixture", "test", Instant.EPOCH)));
            var config = mock(JudgeRuntimeConfig.class);
            when(config.getMaxConcurrentEvaluations()).thenReturn(1);
            var runtime = new DefaultJudgeRuntime(sessions, sessions, exams, id -> List.of(), config);
            var asked = new ArrayList<String>();
            Evaluator evaluator = new Evaluator() {
                @Override public String evaluatorId() { return kind.name + "-fixture"; }
                @Override public Optional<String> evaluatorVersion() { return Optional.of("v-scoped"); }
                @Override public EvaluationOutcome evaluate(EvaluationSubject subject) {
                    asked.add(subject.getSubjectRef());
                    return new EvaluationEmitted(subject.getSubjectRef());
                }
            };
            var reader = mock(EnglishQualityVerdictReader.class);
            when(reader.readNaturalness("selected")).thenReturn(new NativeUsageVerdict(List.of(
                    EnglishQualityTest.a("I drink.", NativeUsageVerdict.Status.PASS)), "Fixture"));
            when(reader.readDidactic("selected")).thenReturn(new A1DidacticVerdict("selected",
                    A1DidacticVerdict.Status.SUITABLE, "Present simple", List.of(), null,
                    "Fixture", null, List.of(), null, null));
            var provider = new EnglishQualityAnalyzerProvider(kind, runtime, evaluator, reader,
                    new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create()), new DefaultFinishedSentenceReader());
            var policy = new EvaluationRunPolicy(10, false, null, null, EvaluationRunPurpose.COURSE, 1);
            policy.setNewEvaluationSubjectIds(Set.of("selected"));

            for (int run = 0; run < 2; run++) {
                var before = EnglishQualityTest.node("neighbor-before", "A1", "I eat.");
                var selected = EnglishQualityTest.node("selected", "A1", "I drink.");
                var after = EnglishQualityTest.node("neighbor-after", "A1", "I walk.");
                var analyzer = provider.create(policy);
                for (var node : List.of(before, selected, after)) analyzer.onQuiz(node);
                var root = before.ancestor(AuditTarget.COURSE).orElseThrow();
                analyzer.onCourseComplete(root);
                assertEquals(List.of("selected"), asked, kind.name + ": only the first selected measurement spends");
                assertEquals(1.0, selected.getScores().get(kind.name));
                for (var neighbor : List.of(before, after)) {
                    assertFalse(neighbor.getScores().containsKey(kind.name));
                    assertTrue(neighbor.getUnevaluatedBy().contains(kind.name));
                }
                var coverage = ((DefaultCourseDiagnoses) root.getDiagnoses()).getJudgeCoverages().get(0);
                assertEquals(3, coverage.getReached());
                assertEquals(2, coverage.getNotEvaluated());
                assertEquals(run == 0 ? 1 : 0, coverage.getEvaluators().get(0).getCoverage().getEvaluatedInRun());
            }
        }
    }
}
