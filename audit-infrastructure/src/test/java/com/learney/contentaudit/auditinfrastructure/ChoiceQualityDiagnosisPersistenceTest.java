package com.learney.contentaudit.auditinfrastructure;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.choicequality.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChoiceQualityDiagnosisPersistenceTest {
    @Test void roundTripsUnknownContextWithVersionAndReuseWithoutLosingOtherDiagnoses() throws Exception {
        var diagnoses = new DefaultQuizDiagnoses();
        var expected = new ChoiceQualityDiagnosis(new ChoiceQualityVerdict(ChoiceQualityVerdict.Status.NEEDS_CONTEXT,
                "", false, List.of(), "Falta contexto visible"), "quality-v1", true);
        diagnoses.setChoiceQualityDiagnosis(expected);
        var mapper = AuditReportObjectMapper.create();
        var loaded = (DefaultQuizDiagnoses) mapper.readValue(mapper.writeValueAsString(diagnoses), NodeDiagnoses.class);
        assertEquals(expected, loaded.getChoiceQualityDiagnosis().orElseThrow());
    }
    @Test void oldStoredReportsRemainReadableWithoutTheNewSlot() throws Exception {
        var mapper = AuditReportObjectMapper.create();
        var loaded = (DefaultQuizDiagnoses) mapper.readValue("{\"@type\":\"QuizDiagnoses\"}", NodeDiagnoses.class);
        assertTrue(loaded.getChoiceQualityDiagnosis().isEmpty());
    }
}
