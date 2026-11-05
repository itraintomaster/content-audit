package com.learney.contentaudit.revisiondomain.comparison;

import com.learney.contentaudit.auditdomain.AuditReport;
import java.util.Set;

/**
 * What the comparison of a batch reads already measured (F-HOY, F-MIDIO): the base, the start and the course with every
 * proposal, each one only with what can be compared. {@code projectedAlone} is the course with every proposal as it was
 * measured, for the judges in {@code onlyToday}, which the base never measured (F-MIDIO-R005). {@code startNotComparable}
 * names the judges the start measured with another version or did not measure, which are not compared against it
 * (F-MIDIO-R002).
 */
public record MeasuredReports(AuditReport baseline, AuditReport approved, AuditReport projected,
        AuditReport projectedAlone, Set<String> onlyToday, Set<String> startNotComparable) {
    public MeasuredReports {
        onlyToday = onlyToday == null ? Set.of() : Set.copyOf(onlyToday);
        startNotComparable = startNotComparable == null ? Set.of() : Set.copyOf(startNotComparable);
    }

    public MeasuredReports(AuditReport baseline, AuditReport approved, AuditReport projected) {
        this(baseline, approved, projected, null, Set.of(), Set.of());
    }
}
