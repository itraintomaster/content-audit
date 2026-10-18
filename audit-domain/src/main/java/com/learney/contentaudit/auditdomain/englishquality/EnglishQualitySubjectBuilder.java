package com.learney.contentaudit.auditdomain.englishquality;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.finishedsentence.*;
import com.learney.contentaudit.coursedomain.SentenceMode;
import com.learney.contentaudit.coursedomain.MultipleChoiceEntity;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSubject;
import java.util.*;
import java.util.stream.Collectors;

/** Exact completed variants, visible options, and course order. Never a POS-based scope gate. */
public final class EnglishQualitySubjectBuilder {
    public static final String CONTRACT = "english-quality-input-v2";
    private final QuizStudentViewRenderer renderer;
    private final FinishedSentenceReader sentences;
    public EnglishQualitySubjectBuilder(QuizStudentViewRenderer renderer, FinishedSentenceReader sentences) {
        this.renderer = Objects.requireNonNull(renderer); this.sentences = Objects.requireNonNull(sentences);
    }
    public List<String> sentences(AuditNode node) {
        var quiz = (AuditableQuiz) node.getEntity();
        List<FinishedSentence> reconstructed;
        if (quiz.getMultipleChoice() != null && quiz.getMultipleChoice().getItems() != null) {
            // The legacy reader selects one correctItem. Judge every accepted choice,
            // including a malformed SINGLE-choice exercise with several correct items.
            reconstructed = new ArrayList<>();
            for (var item : quiz.getMultipleChoice().getItems()) {
                if (item == null || item.getIncidence() <= 0.0) continue;
                var single = new AuditableQuiz(quiz.getTokens(), quiz.getId(), quiz.getLabel(), quiz.getCode(),
                        quiz.getTranslation(), quiz.getSentences(), quiz.getQuizSentence(), quiz.getInstructions(),
                        quiz.getSentenceParts(), new MultipleChoiceEntity(quiz.getMultipleChoice().getSelection(), List.of(item)),
                        quiz.getMiniTheory(), quiz.getAnswerAudioKey());
                var read = sentences.read(single, mode(node));
                if (read != null) reconstructed.addAll(read);
            }
        } else {
            long combinations = 1;
            if (quiz.getSentenceParts() != null) for (var part : quiz.getSentenceParts()) {
                if (part != null && part.getKind() == com.learney.contentaudit.coursedomain.SentencePartKind.CLOZE
                        && part.getOptions() != null) {
                    combinations *= Math.max(1, part.getOptions().size());
                    if (combinations > 64) throw new IllegalArgumentException("Más de 64 variantes: no declarar evaluación completa de una muestra truncada");
                }
            }
            reconstructed = sentences.read(quiz, mode(node));
        }
        if (reconstructed == null || reconstructed.isEmpty())
            throw new IllegalArgumentException("Falta reconstrucción de las variantes aceptadas");
        return reconstructed.stream().map(FinishedSentence::getText).filter(Objects::nonNull)
                .filter(s -> !s.isBlank()).distinct().toList();
    }
    public EvaluationSubject build(AuditNode node) {
        if (node == null || !(node.getEntity() instanceof AuditableQuiz quiz))
            throw new IllegalArgumentException("El sujeto debe ser un ejercicio");
        var accepted = sentences(node);
        if (accepted.isEmpty()) throw new IllegalArgumentException("Sin variantes aceptadas");
        var view = renderer.render(quiz);
        if (view == null || view.getStatement() == null) throw new IllegalArgumentException("Falta vista del alumno");
        var c = new LinkedHashMap<String,String>();
        c.put("inputContract", CONTRACT);
        c.put("quizId", quiz.getId());
        c.put("level", level(node)); c.put("topic", label(node, AuditTarget.TOPIC));
        c.put("title", text(quiz.getLabel())); c.put("instructions", text(quiz.getInstructions()));
        c.put("exercise", view.getStatement()); c.put("sentenceMode", mode(node).name());
        c.put("targetStructure", label(node, AuditTarget.TOPIC) + " / " + text(quiz.getLabel()));
        // Only actual multiple-choice labels are visible choices. CLOZE options are accepted
        // answers, not evidence that the student sees the expected adverb (e.g. P59).
        var choices = quiz.getMultipleChoice() == null || quiz.getMultipleChoice().getItems() == null
                ? List.<String>of() : quiz.getMultipleChoice().getItems().stream().filter(Objects::nonNull)
                    .map(item -> item.getLabel()).filter(Objects::nonNull).toList();
        c.put("visibleChoices", array(choices));
        c.put("renderCompleteness", "CANONICAL_STUDENT_VIEW; cloze options are not visible clues; absent rewrite cue remains unknown");
        c.put("acceptedSentences", array(accepted));
        c.put("languagePolicy", "Standard British and American English; contemporary neutral model examples; valid formal variants remain valid with register notes");
        c.put("documentedPriorLearning", array(priorTitles(node)));
        c.put("curriculumOutline", array(outline(node)));
        return new EvaluationSubject(quiz.getId(), c);
    }
    public static String level(AuditNode n) { return label(n, AuditTarget.MILESTONE).strip(); }
    private static String label(AuditNode n, AuditTarget target) {
        return n.ancestor(target).map(AuditNode::getEntity)
                .map(e -> e.getLabel() != null && !e.getLabel().isBlank() ? e.getLabel() : e.getCode())
                .map(EnglishQualitySubjectBuilder::text).orElse("");
    }
    private static SentenceMode mode(AuditNode n) {
        return n.ancestor(AuditTarget.KNOWLEDGE).map(AuditNode::getEntity).filter(AuditableKnowledge.class::isInstance)
                .map(AuditableKnowledge.class::cast).map(AuditableKnowledge::getSentenceMode).orElse(SentenceMode.FILL);
    }
    private static List<String> priorTitles(AuditNode n) {
        var milestone = n.ancestor(AuditTarget.MILESTONE).orElse(null);
        var knowledge = n.ancestor(AuditTarget.KNOWLEDGE).orElse(null);
        if (milestone == null || knowledge == null) return List.of();
        var previous = new ArrayList<String>();
        for (var t : safe(milestone.getChildren())) {
            for (var k : safe(t.getChildren())) {
                if (k == knowledge) return previous;
                previous.add(text(t.getEntity().getLabel()) + " / " + text(k.getEntity().getLabel()));
            }
        }
        return previous;
    }
    private static List<String> outline(AuditNode n) {
        while (n.getParent() != null) n = n.getParent();
        var result = new ArrayList<String>();
        for (var m : safe(n.getChildren())) for (var t : safe(m.getChildren()))
            result.add(text(m.getEntity().getLabel()) + " / " + text(t.getEntity().getLabel()));
        return result;
    }
    private static <T> List<T> safe(List<T> list) { return list == null ? List.of() : list; }
    private static String text(String s) { return s == null ? "" : s; }
    public static String array(List<String> list) { return list.stream().map(EnglishQualitySubjectBuilder::quote).collect(Collectors.joining(",", "[", "]")); }
    private static String quote(String s) {
        var out = new StringBuilder("\"");
        for (char c : s.toCharArray()) switch (c) {
            case '"' -> out.append("\\\""); case '\\' -> out.append("\\\\");
            case '\n' -> out.append("\\n"); case '\r' -> out.append("\\r"); case '\t' -> out.append("\\t");
            default -> { if (c < 32) out.append(String.format("\\u%04x", (int)c)); else out.append(c); }
        }
        return out.append('"').toString();
    }
}
