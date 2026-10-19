package com.learney.contentaudit.auditdomain.judging;

/** F-ALDIA-R005 inv. 5: how many of a judge's verdicts in a state are abstentions for one reason, with its name. */
public record JudgeAbstentionGroup(JudgeAbstention reason, String reasonName, int count) {
}
