package com.learney.contentaudit.auditapplication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.CourseRepository;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefaultAuditRunnerTest {

    @Mock private CourseRepository courseRepository;
    @Mock private CourseToAuditableMapper courseToAuditableMapper;
    @Mock private AuditEngine auditEngine;
    @Mock private ScoreAggregator scoreAggregator;

    private DefaultAuditRunner sut;
    private final Path coursePath = Path.of("/test/course.json");
    private final CourseEntity courseEntity = new CourseEntity();
    private final AuditableCourse auditableCourse = new AuditableCourse(List.of());
    private final AuditReport auditReport = new AuditReport(new AuditNode());

    @BeforeEach
    void setUp() {
        sut = new DefaultAuditRunner(courseRepository, courseToAuditableMapper, auditEngine,
                List.of(), scoreAggregator);
    }

    @Test
    @DisplayName("Given a valid course path, when runAudit is called, then returns the audit report from the full chain")
    @Tag("F-CLI")
    @Tag("F-CLI-R001")
    @Tag("F-CLI-J001")
    public void givenAValidCoursePathWhenRunAuditIsCalledThenReturnsTheAuditReportFromTheFullChain() {
        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(auditableCourse);
        when(auditEngine.runAudit(auditableCourse)).thenReturn(auditReport);

        AuditReport result = sut.runAudit(coursePath, null);

        assertSame(auditReport, result);
    }

    @Test
    @DisplayName("Given a valid course path, when runAudit is called, then courseRepository load is invoked with the path")
    @Tag("F-CLI")
    @Tag("F-CLI-R001")
    public void givenAValidCoursePathWhenRunAuditIsCalledThenCourseRepositoryLoadIsInvokedWithThePath() {
        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(auditableCourse);
        when(auditEngine.runAudit(auditableCourse)).thenReturn(auditReport);

        sut.runAudit(coursePath, null);

        verify(courseRepository).load(coursePath);
    }

    @Test
    @DisplayName("Given a valid course path, when runAudit is called, then courseToAuditableMapper map is invoked with the loaded entity")
    @Tag("F-CLI")
    @Tag("F-CLI-R001")
    public void givenAValidCoursePathWhenRunAuditIsCalledThenCourseToAuditableMapperMapIsInvokedWithTheLoadedEntity() {
        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(auditableCourse);
        when(auditEngine.runAudit(auditableCourse)).thenReturn(auditReport);

        sut.runAudit(coursePath, null);

        verify(courseToAuditableMapper).map(courseEntity);
    }

    @Test
    @DisplayName("Given a valid course path, when runAudit is called, then contentAudit audit is invoked with the mapped auditable course")
    @Tag("F-CLI")
    @Tag("F-CLI-R001")
    public void givenAValidCoursePathWhenRunAuditIsCalledThenContentAuditAuditIsInvokedWithTheMappedAuditableCourse() {
        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(auditableCourse);
        when(auditEngine.runAudit(auditableCourse)).thenReturn(auditReport);

        sut.runAudit(coursePath, null);

        verify(auditEngine).runAudit(auditableCourse);
    }

    @Test
    @DisplayName("Given courseRepository throws an exception, when runAudit is called, then the exception propagates")
    @Tag("F-CLI")
    @Tag("F-CLI-R001")
    public void givenCourseRepositoryThrowsAnExceptionWhenRunAuditIsCalledThenTheExceptionPropagates() {
        when(courseRepository.load(coursePath)).thenThrow(new RuntimeException("load failed"));

        assertThrows(RuntimeException.class, () -> sut.runAudit(coursePath, null));
    }

    @Test
    @DisplayName("Given courseToAuditableMapper throws an exception, when runAudit is called, then the exception propagates")
    @Tag("F-CLI")
    @Tag("F-CLI-R001")
    public void givenCourseToAuditableMapperThrowsAnExceptionWhenRunAuditIsCalledThenTheExceptionPropagates() {
        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenThrow(new RuntimeException("map failed"));

        assertThrows(RuntimeException.class, () -> sut.runAudit(coursePath, null));
    }

    @Test
    @DisplayName("Given contentAudit throws an exception, when runAudit is called, then the exception propagates")
    @Tag("F-CLI")
    @Tag("F-CLI-R001")
    public void givenContentAuditThrowsAnExceptionWhenRunAuditIsCalledThenTheExceptionPropagates() {
        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(auditableCourse);
        when(auditEngine.runAudit(auditableCourse)).thenThrow(new RuntimeException("audit failed"));

        assertThrows(RuntimeException.class, () -> sut.runAudit(coursePath, null));
    }

    @Test
    @DisplayName("Given a course with no milestones, when runAudit is called, then returns the report from contentAudit")
    @Tag("F-CLI")
    @Tag("F-CLI-R001")
    public void givenACourseWithNoMilestonesWhenRunAuditIsCalledThenReturnsTheReportFromContentAudit() {
        AuditableCourse emptyCourse = new AuditableCourse(List.of());
        when(courseRepository.load(coursePath)).thenReturn(courseEntity);
        when(courseToAuditableMapper.map(courseEntity)).thenReturn(emptyCourse);
        when(auditEngine.runAudit(emptyCourse)).thenReturn(auditReport);

        AuditReport result = sut.runAudit(coursePath, null);

        assertSame(auditReport, result);
    }
}
