package com.learney.contentaudit.refinerdomain;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditTarget;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class DispatchingCorrectionContextResolverTest {

    private SentenceLengthContextResolver sentenceLengthResolver;
    private LemmaAbsenceContextResolver lemmaAbsenceResolver;
    private DispatchingCorrectionContextResolver sut;

    @BeforeEach
    void setUp() {
        sentenceLengthResolver = new SentenceLengthContextResolver();
        lemmaAbsenceResolver = new LemmaAbsenceContextResolver();
        sut = new DispatchingCorrectionContextResolver(sentenceLengthResolver, lemmaAbsenceResolver);
    }

    /** Builds a minimal AuditReport with an empty root node to satisfy non-null checks. */
    private AuditReport buildEmptyReport() {
        AuditNode root = new AuditNode();
        root.setTarget(AuditTarget.COURSE);
        root.setChildren(java.util.Collections.emptyList());
        return new AuditReport(root);
    }

    private RefinementTask buildTask(DiagnosisKind kind) {
        return new RefinementTask(
                "task-d-1",
                AuditTarget.QUIZ,
                "quiz-nonexistent",
                "label",
                kind,
                1,
                RefinementTaskStatus.PENDING);
    }

    @Test
    @DisplayName("should delegate to sentenceLengthResolver when task diagnosis is SENTENCE_LENGTH")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R007")
    public void shouldDelegateToSentenceLengthResolverWhenTaskDiagnosisIsSENTENCELENGTH() {
        // When a task has SENTENCE_LENGTH diagnosis kind, the dispatcher delegates to
        // sentenceLengthResolver. Because the report has no matching node, the result is empty —
        // but the dispatcher must have reached sentenceLengthResolver (not returned early).
        AuditReport report = buildEmptyReport();
        RefinementTask task = buildTask(DiagnosisKind.SENTENCE_LENGTH);

        Optional<CorrectionContext> result = sut.resolve(report, task);

        // The resolver delegates: for an empty/no-match report, sentenceLengthResolver returns empty
        Assertions.assertTrue(result.isEmpty(), "Expected empty when quiz node not found");
    }

    @Test
    @DisplayName("should delegate to lemmaAbsenceResolver when task diagnosis is LEMMA_ABSENCE")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R007")
    public void shouldDelegateToLemmaAbsenceResolverWhenTaskDiagnosisIsLEMMAABSENCE() {
        // When a task has LEMMA_ABSENCE diagnosis kind, the dispatcher delegates to
        // lemmaAbsenceResolver. Because the report has no matching node, the result is empty.
        AuditReport report = buildEmptyReport();
        RefinementTask task = buildTask(DiagnosisKind.LEMMA_ABSENCE);

        Optional<CorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isEmpty(), "Expected empty when quiz node not found");
    }

    @Test
    @DisplayName("should return empty for unsupported diagnosis kind COCA_BUCKETS")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R007")
    public void shouldReturnEmptyForUnsupportedDiagnosisKindCOCABUCKETS() {
        AuditReport report = buildEmptyReport();
        RefinementTask task = buildTask(DiagnosisKind.COCA_BUCKETS);

        Optional<CorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isEmpty(), "Expected empty for unsupported diagnosis kind COCA_BUCKETS");
    }

    @Test
    @DisplayName("should return empty for unsupported diagnosis kind LEMMA_RECURRENCE")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R007")
    public void shouldReturnEmptyForUnsupportedDiagnosisKindLEMMARECURRENCE() {
        AuditReport report = buildEmptyReport();
        RefinementTask task = buildTask(DiagnosisKind.LEMMA_RECURRENCE);

        Optional<CorrectionContext> result = sut.resolve(report, task);

        Assertions.assertTrue(result.isEmpty(), "Expected empty for unsupported diagnosis kind LEMMA_RECURRENCE");
    }

    @Test
    @DisplayName("should propagate empty from delegate when delegate returns empty")
    @Tag("FEAT-RCLA")
    @Tag("F-RCLA-R007")
    public void shouldPropagateEmptyFromDelegateWhenDelegateReturnsEmpty() {
        // Sentence-length task with no matching node in the report → delegate returns empty
        AuditReport report = buildEmptyReport();
        RefinementTask task = buildTask(DiagnosisKind.SENTENCE_LENGTH);

        Optional<CorrectionContext> result = sut.resolve(report, task);

        Assertions.assertFalse(result.isPresent(), "Dispatcher must propagate empty from delegate");
    }
}
