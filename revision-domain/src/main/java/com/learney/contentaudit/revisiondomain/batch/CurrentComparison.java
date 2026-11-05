package com.learney.contentaudit.revisiondomain.batch;

import com.learney.contentaudit.revisiondomain.comparison.BatchComparison;
import com.learney.contentaudit.revisiondomain.nonregression.BatchGateVerdict;
import java.time.Instant;

/**
 * F-HOY-R001: the comparison of a batch as its current analysis leaves it -- the base, the start and the course with
 * everything it proposes, the same a closing compares -- and what the gate would say of it today. Nothing is closed,
 * given back, paid or saved to get it (F-HOY-R002).
 */
public record CurrentComparison(String batchId, int contentVersion, Instant computedAt, BatchComparison comparison,
        BatchGateVerdict gate) {
}
