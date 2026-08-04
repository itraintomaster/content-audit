package com.learney.contentaudit.auditcli.formatting;

/**
 * Hand-written composition-root bridge (not a Sentinel-governed contract, no
 * {@code @Generated} annotation) that exposes a {@link QuizInstructionDetailedFormatter}
 * instance to callers outside the {@code formatting} package -- namely {@code Main}, in
 * the {@code commands} package.
 *
 * <p><b>Why this exists</b>: {@code sentinel.yaml} leaves {@code
 * QuizInstructionDetailedFormatter}'s {@code visibility} undeclared, so it falls back to
 * the {@code formatting} package's {@code internal} default and generates as
 * package-private, unlike its siblings {@code LemmaAbsenceDetailedFormatter} and {@code
 * CocaBucketsDetailedFormatter} -- both explicitly {@code visibility: "public"} in the
 * same package -- which {@code Main} constructs directly. That asymmetry makes {@code
 * QuizInstructionDetailedFormatter} unreachable from {@code Main} today (verified: a
 * direct reference fails to compile with "is not public ... cannot be accessed from
 * outside package").
 *
 * <p>This has been escalated to {@code @architect} to align {@code
 * QuizInstructionDetailedFormatter}'s visibility with its siblings (same escalation
 * pattern already applied to {@code QuizInstructionJudgeConfigResolverBootstrap}). Once
 * that patch lands, {@code Main} should construct {@code QuizInstructionDetailedFormatter}
 * directly and this bridge should be deleted.
 */
public final class QuizInstructionDetailedFormatterBootstrap {

    private QuizInstructionDetailedFormatterBootstrap() {
    }

    public static DetailedFormatter create() {
        return new QuizInstructionDetailedFormatter();
    }
}
