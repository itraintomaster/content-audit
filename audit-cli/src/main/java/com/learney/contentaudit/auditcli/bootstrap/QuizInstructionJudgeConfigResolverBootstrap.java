package com.learney.contentaudit.auditcli.bootstrap;

import com.learney.contentaudit.quizinstructioninfrastructure.QuizInstructionJudgeConfig;

/**
 * Hand-written composition-root bridge (not a Sentinel-governed contract, no
 * {@code @Generated} annotation) that exposes {@link
 * DefaultQuizInstructionJudgeConfigResolver#resolve()} to callers outside the
 * {@code bootstrap} package -- namely {@code Main}, in the {@code commands} package.
 *
 * <p><b>Why this exists</b>: {@code sentinel.yaml} declares the {@code
 * QuizInstructionJudgeConfigResolver} interface with {@code visibility: internal} and
 * its implementation package-private by default, unlike every sibling resolver in this
 * same package -- {@code DefaultWorkdirResolver}, {@code DefaultApprovalModeResolver},
 * {@code DefaultProposalStrategySelector}, {@code DefaultLagenModeResolver}, {@code
 * DefaultLagenConfigResolver} -- all of which are {@code visibility: public} and are
 * instantiated directly by {@code Main}. That asymmetry makes {@code
 * DefaultQuizInstructionJudgeConfigResolver} unreachable from {@code Main} today
 * (verified: a direct reference fails to compile with "is not public ... cannot be
 * accessed from outside package").
 *
 * <p>This has been escalated to {@code @architect} to align the visibility of both the
 * interface and the implementation with their siblings. Once that patch lands, {@code
 * Main} should call the resolver directly and this bridge (and this whole file) should
 * be deleted. Until then, this bridge lets {@code Main} obtain a real, environment-resolved
 * {@link QuizInstructionJudgeConfig} without duplicating the resolver's env-var contract
 * (the {@code CONTENT_AUDIT_QINST_JUDGE_*} keys) a second time and without touching the
 * generated file.
 */
public final class QuizInstructionJudgeConfigResolverBootstrap {

    private QuizInstructionJudgeConfigResolverBootstrap() {
    }

    public static QuizInstructionJudgeConfig resolve() {
        return new DefaultQuizInstructionJudgeConfigResolver().resolve();
    }
}
