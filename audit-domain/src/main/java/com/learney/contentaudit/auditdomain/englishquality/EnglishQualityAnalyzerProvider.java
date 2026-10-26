package com.learney.contentaudit.auditdomain.englishquality;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.catalog.*;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.auditdomain.finishedsentence.FinishedSentenceReader;
import com.learney.contentaudit.auditdomain.judging.JudgeRuntime;
import com.learney.contentaudit.evaluationledgerdomain.Evaluator;
import java.util.*;

/** Two independent budgets, ledgers, versions and diagnoses; no automatic course edits. */
public final class EnglishQualityAnalyzerProvider implements AnalyzerProvider {
    public enum Kind {
        NATURALNESS("naturalness", "Naturalidad idiomática"),
        A1_DIDACTIC("a1-didactic", "Calidad didáctica A1");
        public final String name; public final String label;
        Kind(String name, String label) { this.name = name; this.label = label; }
    }
    private final Kind kind;
    private final JudgeRuntime runtime;
    private final Evaluator evaluator;
    private final EnglishQualityVerdictReader reader;
    private final EnglishQualitySubjectBuilder builder;
    public EnglishQualityAnalyzerProvider(Kind kind, JudgeRuntime runtime, Evaluator evaluator,
            EnglishQualityVerdictReader reader, QuizStudentViewRenderer renderer, FinishedSentenceReader sentences) {
        this.kind = Objects.requireNonNull(kind); this.runtime = Objects.requireNonNull(runtime);
        this.evaluator = evaluator; this.reader = Objects.requireNonNull(reader);
        this.builder = new EnglishQualitySubjectBuilder(renderer, sentences);
    }
    @Override public String analyzerName() { return kind.name; }
    @Override public AnalyzerDescriptor describe() {
        var nativeForm = kind == Kind.NATURALNESS;
        return new AnalyzerDescriptor(kind.name, kind.label, AuditTarget.QUIZ,
                nativeForm ? "¿Esta formulación expresa la idea de manera idiomática?" : "¿Es un ejemplo claro para el objetivo y recorrido de A1?",
                "Todas las variantes reconstruidas, estímulo y opciones visibles, objetivo y orden documentado del curso. Sin filtro POS, miniteoría ni traducción posterior.",
                List.of(new AnalyzerRuleCard(nativeForm ? "unidiomatic-formulation" : "a1-example-quality",
                        nativeForm ? "Causa lingüística y fragmento exacto; no preferencias de estilo" : "Claridad, alineación y dificultad incidental; no invalida respuestas válidas", AnalysisCost.PAID_MODEL)),
                nativeForm ? "0 variantes con defecto de formulación" : "0 ejemplos con mejoras didácticas pendientes",
                AnalyzerFamily.ERRORS, List.of(AuditTarget.QUIZ), List.of(FindingResolution.PANEL),
                AnalysisCost.PAID_MODEL, kind.label, List.of(),
                "Resultado estructurado independiente. El puntaje refleja ausencia de defectos propios del juez, no exactitud ni confianza; pendientes y N/A se informan aparte. Derivaciones a otros jueces conservan su dueño y no bajan este puntaje. Los defectos didácticos afectan al ejemplo, no invalidan su inglés ni una respuesta aceptada. Hallazgos para revisión en panel, sin reescritura automática.");
    }
    @Override public ContentAnalyzer create(EvaluationRunPolicy policy) {
        return new EnglishQualityAnalyzer(kind, runtime.open(kind.name, evaluator == null ? List.of() : List.of(evaluator), policy), runtime, builder, reader);
    }
    /**
     * F-MESA-R024: what the judge of A1 finds in an example becomes an A1_DIDACTIC task on the exercise; naturalness stays
     * context for the one who proposes.
     */
    @Override public Optional<AnalyzerPlanBinding> planBinding() {
        return kind == Kind.A1_DIDACTIC ? Optional.of(new AnalyzerPlanBinding(A1DidacticCorrection.TASK_KIND,
                List.of(AuditTarget.QUIZ))) : Optional.empty();
    }
    @Override public Optional<SelfDescribingConfig> config() { return Optional.empty(); }
}
