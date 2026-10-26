package com.learney.contentaudit.auditdomain.spanishregister;

import com.learney.contentaudit.auditdomain.AnalyzerProvider;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.ContentAnalyzer;
import com.learney.contentaudit.auditdomain.EvaluationRunPolicy;
import com.learney.contentaudit.auditdomain.AnalyzerDescriptor;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerFamily;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerPlanBinding;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRuleCard;
import com.learney.contentaudit.auditdomain.SelfDescribingConfig;
import com.learney.contentaudit.auditdomain.finding.AnalysisCost;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import java.util.List;
import java.util.Optional;

/**
 * F-MESA-R025: spanish-register in the catalog, an errors analyzer that evaluates exercises, resolves at the panel and
 * costs instant. Its findings become SPANISH_REGISTER tasks on the exercise.
 */
public class SpanishRegisterAnalyzerProvider implements AnalyzerProvider {
    static final String TASK_KIND = "SPANISH_REGISTER";

    @Override
    public String analyzerName() {
        return SpanishRegisterAnalyzer.ANALYZER_NAME;
    }

    @Override
    public AnalyzerDescriptor describe() {
        return new AnalyzerDescriptor(
                SpanishRegisterAnalyzer.ANALYZER_NAME,
                SpanishRegisterAnalyzer.DESCRIPTION,
                AuditTarget.QUIZ,
                "¿Lo que lee el alumno en español va de tú?",
                "La consigna, la miniteoría y la traducción de cada ejercicio, sin los términos en inglés entre $…$",
                List.of(new AnalyzerRuleCard(SpanishRegisterAnalyzer.RULE_TU,
                        "F-MESA-R025: la consigna, la miniteoría y la traducción van de tú, como las consignas del curso",
                        AnalysisCost.INSTANT)),
                "0 textos de vos: todo lo que lee el alumno va de tú",
                AnalyzerFamily.ERRORS,
                List.of(AuditTarget.QUIZ),
                List.of(FindingResolution.PANEL),
                AnalysisCost.INSTANT,
                "Texto de vos", List.of(),
                "Consignas, miniteorías y traducciones escritas de vos («Usá», «podés», «agregalo») donde el curso habla de"
                        + " tú («Usa», «puedes», «agrégalo»).");
    }

    @Override
    public ContentAnalyzer create(EvaluationRunPolicy policy) {
        return new SpanishRegisterAnalyzer();
    }

    /** Its findings become SPANISH_REGISTER tasks on the exercise. */
    @Override
    public Optional<AnalyzerPlanBinding> planBinding() {
        return Optional.of(new AnalyzerPlanBinding(TASK_KIND, List.of(AuditTarget.QUIZ)));
    }

    @Override
    public Optional<SelfDescribingConfig> config() {
        return Optional.empty();
    }
}
