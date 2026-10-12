package com.learney.contentaudit.revisiondomain.conversion;

import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import java.util.List;

/** Local operations. Judging uses the existing, explicitly budgeted context-opinion flow. */
public final class MultipleChoiceConversion {
    private MultipleChoiceConversion() {}
    public static final String RULE = "cloze-to-multiple-choice";
    public static final List<String> JUDGES = List.of("multiple-choice-validity", "multiple-choice-quality");
    public record Request(String action, String batchId, String quizId, Integer contentVersion, String reviewer) {}
    public record Candidate(String quizId, String knowledgeId, String topic, String level, String source,
            int topicTotal, int multipleChoiceCount, List<String> options, String reason, QuizTemplateEntity before, QuizTemplateEntity after) {
        public Candidate { options = List.copyOf(options); }
        public boolean eligible() { return after != null; }
    }
    public record State(String batchId, int contentVersion, List<Candidate> candidates) {
        public State { candidates = List.copyOf(candidates); }
    }
}
