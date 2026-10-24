package com.learney.contentaudit.auditdomain.datacoherence;

import com.learney.contentaudit.auditdomain.finding.Finding;
import java.util.List;

/** Audio is checked against the final text only when preparing publication. */
public enum DataCoherenceStage {
    CONTENT,
    PREPUBLICATION;

    public static boolean isAudioIssue(CopyMismatch issue) {
        if (issue == null) return false;
        if (issue.getCopy() == ExerciseCopy.AUDIO) return true;
        return issue.getKind() != null && switch (issue.getKind()) {
            case AUDIO_DIFFERS, SLASH_IN_AUDIO, NO_AUDIO_KEY, SHARED_AUDIO_KEY, UNKNOWN_RECORDING -> true;
            default -> false;
        };
    }

    /** Recognizes audio evidence without hiding the answer-slash rule that shares its name. */
    public static boolean isAudioFinding(Finding finding) {
        if (finding == null || !"data-coherence".equals(finding.getAnalyzer())) return false;
        if ("audio-matches".equals(finding.getRule()) || "audio-known".equals(finding.getRule())) return true;
        if (!"no-slash".equals(finding.getRule())) return false;
        String marker = finding.getIdentity() == null ? null : finding.getIdentity().getMarker();
        if (marker != null && marker.startsWith("audio")) return true;
        return finding.getEvidence() != null
                && (finding.getEvidence().getExamined() == null ? List.<com.learney.contentaudit.auditdomain.finding.EvidencePart>of()
                        : finding.getEvidence().getExamined()).stream()
                        .anyMatch(part -> part != null && "Audio".equalsIgnoreCase(part.getLabel()));
    }
}
