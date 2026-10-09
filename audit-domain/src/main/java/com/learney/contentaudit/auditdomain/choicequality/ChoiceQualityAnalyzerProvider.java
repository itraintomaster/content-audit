package com.learney.contentaudit.auditdomain.choicequality;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.catalog.*;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.auditdomain.judging.JudgeRuntime;
import com.learney.contentaudit.evaluationledgerdomain.Evaluator;
import java.util.List;
import java.util.Optional;

/** Separate name, version, exam and budget; never changes the validity judge's identity. */
public final class ChoiceQualityAnalyzerProvider implements AnalyzerProvider {
    public static final String NAME = "multiple-choice-quality";
    private final JudgeRuntime runtime;
    private final Evaluator evaluator;
    private final ChoiceQualityVerdictReader reader;
    private final QuizStudentViewRenderer renderer;

    public ChoiceQualityAnalyzerProvider(JudgeRuntime runtime, Evaluator evaluator,
            ChoiceQualityVerdictReader reader, QuizStudentViewRenderer renderer) {
        this.runtime = runtime;
        this.evaluator = evaluator;
        this.reader = reader;
        this.renderer = renderer;
    }
    @Override public String analyzerName() { return NAME; }
    @Override public AnalyzerDescriptor describe() {
        return new AnalyzerDescriptor(NAME, "Revisa si los distractores practican el objetivo del tema sin regalar la respuesta",
                AuditTarget.QUIZ, "¿Las opciones exigen entender la regla y representan errores esperables del nivel?",
                "Nivel y tema para situar el objetivo; título, consigna, enunciado con pista y opciones como los ve el alumno. "
                        + "Nunca traducción, miniteoría ni audio posteriores. Si falta contexto, no inventa un objetivo.",
                List.of(
                        new AnalyzerRuleCard("answer-cue", "Una pista permite acertar sin practicar el objetivo", AnalysisCost.PAID_MODEL),
                        new AnalyzerRuleCard("irrelevant-distractors", "Las incorrectas no ponen a prueba el objetivo", AnalysisCost.PAID_MODEL),
                        new AnalyzerRuleCard("implausible-distractors", "Las incorrectas no representan confusiones plausibles del nivel", AnalysisCost.PAID_MODEL)),
                "0 ejercicios con distractores que regalen la respuesta o no practiquen el objetivo",
                AnalyzerFamily.ERRORS, List.of(AuditTarget.QUIZ), List.of(FindingResolution.PANEL),
                AnalysisCost.PAID_MODEL, "Calidad de distractores", List.of(),
                "Opción múltiple que se acierta por una pista o cuyas opciones incorrectas no practican el objetivo pedagógico.");
    }
    @Override public ContentAnalyzer create(EvaluationRunPolicy policy) {
        return new ChoiceQualityAnalyzer(runtime.open(NAME, evaluator != null ? List.of(evaluator) : List.of(), policy),
                runtime, new ChoiceQualitySubjectBuilder(renderer), reader);
    }
    @Override public Optional<AnalyzerPlanBinding> planBinding() {
        return Optional.of(new AnalyzerPlanBinding("MULTIPLE_CHOICE_VALIDITY", List.of(AuditTarget.QUIZ)));
    }
    @Override public Optional<SelfDescribingConfig> config() { return Optional.empty(); }
}
