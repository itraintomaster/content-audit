package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.revisiondomain.projection.CourseProjector;
import com.learney.contentaudit.revisiondomain.projection.CourseProjection;
import com.learney.contentaudit.revisiondomain.projection.ProjectionScope;
import com.learney.contentaudit.coursedomain.CourseEntity;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MainCurrentAnalysisWiringTest {
    @Test void publishingThroughTheLazyAdapterUsesRecordedMeasurementsWithoutNormalRemeasurement() {
        var delegate=mock(CourseProjector.class);
        var approved=new AuditReport(); var projected=new AuditReport();
        var scope=new ProjectionScope(); var result=new CourseProjection(); var start=new CourseEntity();
        when(delegate.projectWithMeasurements(start,scope,approved,projected)).thenReturn(result);
        var lazy=Main.lazyCourseProjector(()->delegate);
        assertSame(result,lazy.projectWithMeasurements(start,scope,approved,projected));
        verify(delegate).projectWithMeasurements(start,scope,approved,projected);
        verify(delegate,never()).project(any(),any());
    }
}
