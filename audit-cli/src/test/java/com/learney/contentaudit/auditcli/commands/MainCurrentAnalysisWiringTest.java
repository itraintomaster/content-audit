package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.revisiondomain.projection.CourseProjector;
import com.learney.contentaudit.revisiondomain.projection.CourseProjection;
import com.learney.contentaudit.revisiondomain.projection.ProjectionScope;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MainCurrentAnalysisWiringTest {
    @Test void publishingThroughTheLazyAdapterUsesRecordedMeasurementsWithoutNormalRemeasurement() {
        var delegate=mock(CourseProjector.class);
        var approved=new AuditReport(); var projected=new AuditReport();
        var scope=new ProjectionScope(); var result=new CourseProjection(); var path=Path.of("course");
        when(delegate.projectWithMeasurements(path,scope,approved,projected)).thenReturn(result);
        var lazy=Main.lazyCourseProjector(()->delegate);
        assertSame(result,lazy.projectWithMeasurements(path,scope,approved,projected));
        verify(delegate).projectWithMeasurements(path,scope,approved,projected);
        verify(delegate,never()).project(any(),any());
    }
}
