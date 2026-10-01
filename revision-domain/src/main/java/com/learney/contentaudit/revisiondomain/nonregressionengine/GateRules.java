package com.learney.contentaudit.revisiondomain.nonregressionengine;

import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import java.util.Locale;
import java.util.Set;

/**
 * What counts for the two gates, in one place. A finding counts as an error (F-HALL-R008) when it does not only rank and
 * its analyzer is not one of the seven of vocabulary; a vocabulary finding of medium severity or more is a red word --
 * a word of another level or out of the catalog, the one of the light of the topic (F-NOEMP-R002, F-METAS-R003) --; a
 * length is declared and never returns a change (F-NOEMP-R003, José 11/8). The audio that no longer matches an exercise
 * listed for re-recording is the mark {@code audio…} of data-coherence, the same rule its declaration of correction
 * reads ({@code DataCoherenceCorrection.concernsAudio}).
 */
final class GateRules {

    /** The analyzers of the vocabulary family (the rest of the catalog is the errors family). */
    static final Set<String> VOCABULARY = Set.of("sentence-length", "knowledge-title-length",
            "knowledge-instructions-length", "coca-buckets-distribution", "lemma-absence", "lemma-count",
            "lemma-recurrence");

    static final Set<String> LENGTH = Set.of("sentence-length", "knowledge-title-length",
            "knowledge-instructions-length");

    static final double VOCABULARY_TOLERANCE = 0.5;

    private GateRules() {
    }

    static boolean countsAsError(Finding finding) {
        return finding != null && finding.getAnalyzer() != null && !VOCABULARY.contains(finding.getAnalyzer())
                && finding.getResolution() != null && finding.getResolution() != FindingResolution.RANK_ONLY;
    }

    static boolean isRedWord(Finding finding) {
        if (finding == null || finding.getAnalyzer() == null || !VOCABULARY.contains(finding.getAnalyzer())
                || LENGTH.contains(finding.getAnalyzer()) || finding.getSeverity() == null) {
            return false;
        }
        return finding.getSeverity() == FindingSeverity.MEDIUM || finding.getSeverity() == FindingSeverity.HIGH
                || finding.getSeverity() == FindingSeverity.BLOCKING;
    }

    static boolean isLength(Finding finding) {
        return finding != null && LENGTH.contains(finding.getAnalyzer());
    }

    static boolean isAudioToRerecord(Finding finding) {
        String marker = finding != null && finding.getIdentity() != null ? finding.getIdentity().getMarker() : null;
        return finding != null && "data-coherence".equals(finding.getAnalyzer()) && marker != null
                && marker.startsWith("audio");
    }

    static String identity(Finding finding) {
        return finding.getIdentity() != null && finding.getIdentity().getKey() != null ? finding.getIdentity().getKey()
                : finding.getAnalyzer() + "|" + finding.getRule();
    }

    /** A number as José reads it: one decimal with a comma when it has one, an integer when it is whole. */
    static String number(Double value) {
        if (value == null) {
            return "—";
        }
        if (value == Math.rint(value) && Math.abs(value) < 1e9) {
            return String.valueOf((long) Math.rint(value));
        }
        String text = String.format(Locale.ROOT, "%.3f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
        return text.replace('.', ',');
    }
}
