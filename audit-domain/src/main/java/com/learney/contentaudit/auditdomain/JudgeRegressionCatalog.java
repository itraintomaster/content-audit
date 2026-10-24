package com.learney.contentaudit.auditdomain;

import com.learney.contentaudit.auditdomain.judging.JudgeRegressionSuite;

/** Permanent, course-owned regression examples. Expected labels never enter judge input. */
public interface JudgeRegressionCatalog {
    JudgeRegressionSuite suite(String judgeName);
    static JudgeRegressionCatalog empty() { return judge -> JudgeRegressionSuite.empty(); }
}
