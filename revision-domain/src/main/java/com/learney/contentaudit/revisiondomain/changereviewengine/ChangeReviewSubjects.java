package com.learney.contentaudit.revisiondomain.changereviewengine;

import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.QuizStudentViewRenderer;
import com.learney.contentaudit.auditdomain.studentview.QuizStudentView;
import com.learney.contentaudit.auditdomain.studentview.StudentGapView;
import com.learney.contentaudit.auditdomain.studentview.StudentOptionView;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.MilestoneEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.TopicEntity;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSubject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * F-MARCA-R001: each exercise the batch left different from production, both sides whole -- the instructions, the
 * exercise as the student sees it with its hints, the options, the accepted answers, the translation and the mini
 * theory -- with the level, the unit, the name of the topic, its instructions and its sentence mode. One subject per
 * exercise, in the order of the course; an exercise equal to production is not read.
 */
final class ChangeReviewSubjects {
    static final String CONTRACT = "change-review-input-v1";
    static final String NOT_IN_PRODUCTION = "(No existía en producción.)";
    private final QuizStudentViewRenderer renderer;

    ChangeReviewSubjects(QuizStudentViewRenderer renderer) { this.renderer = Objects.requireNonNull(renderer); }

    /** An exercise to read: its subject, its topic and both sides as the reviewer reads them. */
    record Changed(EvaluationSubject subject, String topic, String before, String after) {
        String quizId() { return subject.getSubjectRef(); }
    }

    List<Changed> changed(CourseEntity production, CourseEntity now, Collection<String> only) {
        Map<String, QuizTemplateEntity> before = new LinkedHashMap<>();
        walk(production, (milestone, topic, knowledge, quiz) -> before.putIfAbsent(quiz.getId(), quiz));
        Set<String> asked = only == null ? Set.of() : Set.copyOf(only);
        List<Changed> changed = new ArrayList<>();
        walk(now, (milestone, topic, knowledge, quiz) -> {
            if (!asked.isEmpty() && !asked.contains(quiz.getId())) return;
            String after = side(quiz);
            QuizTemplateEntity old = before.get(quiz.getId());
            String was = old == null ? NOT_IN_PRODUCTION : side(old);
            if (was.equals(after)) return;
            var content = new LinkedHashMap<String, String>();
            content.put("inputContract", CONTRACT);
            content.put("quizId", quiz.getId());
            content.put("level", text(milestone.getLabel()).strip());
            content.put("unit", text(topic.getLabel()));
            content.put("topic", text(knowledge.getLabel()));
            content.put("topicInstructions", text(knowledge.getInstructions()));
            content.put("sentenceMode", knowledge.getSentenceMode() == null ? "" : knowledge.getSentenceMode().name());
            content.put("before", was);
            content.put("after", after);
            changed.add(new Changed(new EvaluationSubject(quiz.getId(), content), text(knowledge.getLabel()), was, after));
        });
        return changed;
    }

    /** One side as the reviewer reads it: what the student sees before answering, the answers, then what comes after. */
    String side(QuizTemplateEntity quiz) {
        FormEntity form = quiz.getForm();
        var auditable = new AuditableQuiz();
        auditable.setId(quiz.getId());
        auditable.setLabel(quiz.getTitle());
        auditable.setInstructions(quiz.getInstructions());
        auditable.setTranslation(quiz.getTranslation());
        auditable.setMiniTheory(quiz.getMiniTheory());
        auditable.setSentenceParts(form == null ? null : form.getSentenceParts());
        auditable.setMultipleChoice(form == null ? null : form.getMultipleChoice());
        QuizStudentView view = renderer.render(auditable);
        List<StudentOptionView> options = view == null || view.getOptions() == null ? List.of()
                : view.getOptions().stream().filter(Objects::nonNull).toList();
        var lines = new ArrayList<String>();
        lines.add("Consigna del ejercicio: " + orElse(quiz.getInstructions(), "(sin consigna propia)"));
        lines.add("Ejercicio, como lo ve el alumno antes de contestar: " + orElse(view == null ? null : view.getStatement(), "(vacío)"));
        lines.add("Opciones que ve el alumno: " + (options.isEmpty() ? "(no es de opción múltiple)"
                : String.join(" / ", options.stream().map(o -> text(o.getLabel())).toList())));
        lines.add("Respuestas que acepta el corrector: " + orElse(accepted(view == null ? null : view.getGaps(), options), "(ninguna)"));
        lines.add("Traducción, que el alumno ve al corregir: " + orElse(quiz.getTranslation(), "(sin traducción)"));
        lines.add("Miniteoría, que el alumno ve al corregir: " + orElse(quiz.getMiniTheory(), "(sin miniteoría)"));
        return String.join("\n", lines);
    }

    /** In a multiple choice, the correct options; otherwise one entry per gap, «Hueco N: a | b». */
    private static String accepted(List<StudentGapView> gaps, List<StudentOptionView> options) {
        if (!options.isEmpty())
            return "Opción correcta: " + String.join(" | ", options.stream().filter(StudentOptionView::isCorrect)
                    .map(o -> text(o.getLabel())).toList());
        var entries = new ArrayList<String>();
        if (gaps != null) for (int i = 0; i < gaps.size(); i++) {
            var gap = gaps.get(i);
            var answers = gap == null || gap.getAcceptedAnswers() == null ? List.<String>of() : gap.getAcceptedAnswers();
            if (!answers.isEmpty()) entries.add("Hueco " + (i + 1) + ": " + String.join(" | ", answers));
        }
        return String.join("; ", entries);
    }

    private interface Visit { void quiz(MilestoneEntity milestone, TopicEntity topic, KnowledgeEntity knowledge, QuizTemplateEntity quiz); }

    private static void walk(CourseEntity course, Visit visit) {
        if (course == null || course.getRoot() == null || course.getRoot().getMilestones() == null) return;
        for (MilestoneEntity milestone : course.getRoot().getMilestones()) {
            if (milestone == null || milestone.getTopics() == null) continue;
            for (TopicEntity topic : milestone.getTopics()) {
                if (topic == null || topic.getKnowledges() == null) continue;
                for (KnowledgeEntity knowledge : topic.getKnowledges()) {
                    if (knowledge == null || knowledge.getQuizTemplates() == null) continue;
                    for (QuizTemplateEntity quiz : knowledge.getQuizTemplates())
                        if (quiz != null && quiz.getId() != null) visit.quiz(milestone, topic, knowledge, quiz);
                }
            }
        }
    }

    private static String orElse(String value, String otherwise) { return value == null || value.isBlank() ? otherwise : value; }

    private static String text(String value) { return value == null ? "" : value; }
}
