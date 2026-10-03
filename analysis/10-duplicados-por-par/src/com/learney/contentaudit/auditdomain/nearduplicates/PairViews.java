package com.learney.contentaudit.auditdomain.nearduplicates;

import com.learney.contentaudit.auditapplication.CourseToAuditableMapper;
import com.learney.contentaudit.auditdomain.AuditableCourse;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableMilestone;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.AuditableTopic;
import com.learney.contentaudit.auditdomain.QuizStudentViewRenderer;
import com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer;
import com.learney.contentaudit.courseinfrastructure.CourseValidatorImpl;
import com.learney.contentaudit.courseinfrastructure.FileSystemCourseRepository;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSubject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** What the judge would read for each sampled pair: DefaultSameExerciseQuestion.subjectOf, as is. */
public class PairViews {

    public static void main(String[] args) throws Exception {
        Path course = Paths.get(args[0]);
        Path sample = Paths.get(args[1]);
        CourseEntity entity = new FileSystemCourseRepository(new CourseValidatorImpl()).load(course);
        DefaultQuizSentenceConverter converter = DefaultQuizSentenceConverter.create();
        AuditableCourse auditable = new CourseToAuditableMapper(new PairCensus.NoTokens(), converter).map(entity);
        QuizStudentViewRenderer renderer = new DefaultQuizStudentViewRenderer(converter);
        DefaultSameExerciseQuestion question = new DefaultSameExerciseQuestion(renderer);
        Map<String, AuditableQuiz> byId = new HashMap<>();
        for (AuditableMilestone milestone : auditable.getMilestones()) {
            for (AuditableTopic topic : milestone.getTopics()) {
                for (AuditableKnowledge knowledge : topic.getKnowledge()) {
                    for (AuditableQuiz quiz : knowledge.getQuizzes()) {
                        byId.put(quiz.getId(), quiz);
                    }
                }
            }
        }
        List<String> lines = Files.readAllLines(sample);
        for (String line : lines.subList(1, lines.size())) {
            String[] f = line.split("\t");
            EvaluationSubject subject = question.subjectOf(byId.get(f[3]), byId.get(f[4]));
            Map<String, String> content = subject.getContent();
            System.out.println("=== " + f[0] + " " + f[1] + " cos " + f[5] + " ws " + f[6] + " · " + f[3] + " / " + f[4]);
            System.out.println("Tema: " + content.get("title"));
            System.out.println("1) " + content.get("first").replace("\n", "\n   "));
            System.out.println("2) " + content.get("second").replace("\n", "\n   "));
        }
    }
}
