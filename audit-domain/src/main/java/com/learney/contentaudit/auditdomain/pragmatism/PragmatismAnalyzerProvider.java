package com.learney.contentaudit.auditdomain.pragmatism;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.catalog.*;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.auditdomain.judging.JudgeRuntime;
import com.learney.contentaudit.evaluationledgerdomain.Evaluator;
import java.util.List;
import java.util.Optional;

/** Separate identity, ledger, exam and budget; this judge flags review, never rewrites a quiz. */
public final class PragmatismAnalyzerProvider implements AnalyzerProvider {
    public static final String NAME = "pragmatism";
    private final JudgeRuntime runtime;
    private final Evaluator evaluator;
    private final PragmatismVerdictReader reader;
    private final QuizStudentViewRenderer renderer;
    public PragmatismAnalyzerProvider(JudgeRuntime runtime, Evaluator evaluator,
            PragmatismVerdictReader reader, QuizStudentViewRenderer renderer) {
        this.runtime = runtime; this.evaluator = evaluator; this.reader = reader; this.renderer = renderer;
    }
    @Override public String analyzerName() { return NAME; }
    @Override public AnalyzerDescriptor describe() {
        return new AnalyzerDescriptor(NAME, "Revisa si lo dicho tiene un uso comunicativo plausible en el contexto mostrado",
                AuditTarget.QUIZ, "¿Alguien tendría un motivo razonable para decir esto en una situación compatible con lo visible?",
                "Oraciones terminadas aceptadas, tema, título y consigna visibles. No lee miniteoría, traducción ni distractores. "
                        + "No juzga gramática, naturalidad, nivel léxico ni frecuencia estadística.",
                List.of(new AnalyzerRuleCard("incoherent-scenario", "La situación literal carece de sentido", AnalysisCost.PAID_MODEL),
                        new AnalyzerRuleCard("unlicensed-scenario", "Sólo funciona inventando una ficción o personificación ausente", AnalysisCost.PAID_MODEL),
                        new AnalyzerRuleCard("discourse-mismatch", "Las intervenciones no cumplen una función comunicativa coherente", AnalysisCost.PAID_MODEL)),
                "0 oraciones señaladas por falta de uso comunicativo plausible",
                AnalyzerFamily.ERRORS, List.of(AuditTarget.QUIZ), List.of(FindingResolution.RANK_ONLY),
                AnalysisCost.PAID_MODEL, "Pragmatismo comunicativo", List.of(),
                "Oración sin uso comunicativo plausible; señal para revisión independiente, sin corrección automática.");
    }
    @Override public ContentAnalyzer create(EvaluationRunPolicy policy) {
        return new PragmatismAnalyzer(runtime.open(NAME, evaluator == null ? List.of() : List.of(evaluator), policy),
                runtime, new PragmatismSubjectBuilder(renderer), reader);
    }
    @Override public Optional<AnalyzerPlanBinding> planBinding() { return Optional.empty(); }
    @Override public Optional<SelfDescribingConfig> config() { return Optional.empty(); }
}
