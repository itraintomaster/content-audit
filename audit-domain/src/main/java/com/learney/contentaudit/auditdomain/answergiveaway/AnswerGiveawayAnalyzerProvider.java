package com.learney.contentaudit.auditdomain.answergiveaway;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.catalog.*;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.auditdomain.judging.JudgeRuntime;
import com.learney.contentaudit.evaluationledgerdomain.Evaluator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * FEAT-REGALA: the judge «no regala la respuesta», with its own identity, ledger, exam and budget. It reaches every
 * exercise, of every type and level; what it finds is corrected at the panel (F-REGALA-R003).
 */
public final class AnswerGiveawayAnalyzerProvider implements AnalyzerProvider {
    public static final String NAME = "answer-giveaway";
    public static final String LABEL = "No regala la respuesta";
    static final String RULE = "gives-away-the-decision";
    private final JudgeRuntime runtime;
    private final Evaluator evaluator;
    private final AnswerGiveawayVerdictReader reader;
    private final AnswerGiveawaySubjectBuilder builder;

    public AnswerGiveawayAnalyzerProvider(JudgeRuntime runtime, Evaluator evaluator, AnswerGiveawayVerdictReader reader,
            QuizStudentViewRenderer renderer) {
        this.runtime = Objects.requireNonNull(runtime); this.evaluator = evaluator;
        this.reader = Objects.requireNonNull(reader); this.builder = new AnswerGiveawaySubjectBuilder(renderer);
    }
    @Override public String analyzerName() { return NAME; }
    @Override public AnalyzerDescriptor describe() {
        return new AnalyzerDescriptor(NAME, LABEL, AuditTarget.QUIZ,
                "¿Algo que el alumno lee fuera de la oración le hace la decisión que el tema le pide practicar?",
                "Nombre del tema, consigna del ejercicio, enunciado con su pista y opciones, como los ve el alumno antes de "
                        + "contestar, y las respuestas aceptadas para saber cuál es la respuesta. No lee traducción, "
                        + "miniteoría ni audio. La pista corta al lado del hueco y las opciones las miran otros jueces.",
                List.of(new AnalyzerRuleCard(RULE, "La consigna o una indicación del enunciado hacen toda la decisión "
                        + "(alta) o una parte (media)", AnalysisCost.PAID_MODEL)),
                "0 ejercicios cuya consigna regala la respuesta",
                AnalyzerFamily.ERRORS, List.of(AuditTarget.QUIZ), List.of(FindingResolution.PANEL),
                AnalysisCost.PAID_MODEL, LABEL, List.of(),
                "Ejercicios cuya consigna, o una indicación escrita en el enunciado, le hace al alumno la decisión que "
                        + "el tema practica: le dice la respuesta o qué caso de la regla aplica.");
    }
    @Override public ContentAnalyzer create(EvaluationRunPolicy policy) {
        return new AnswerGiveawayAnalyzer(runtime.open(NAME, evaluator == null ? List.of() : List.of(evaluator), policy),
                runtime, builder, reader);
    }
    /** F-REGALA-R003: what it finds becomes an ANSWER_GIVEAWAY task on the exercise. */
    @Override public Optional<AnalyzerPlanBinding> planBinding() {
        return Optional.of(new AnalyzerPlanBinding(AnswerGiveawayCorrection.TASK_KIND, List.of(AuditTarget.QUIZ)));
    }
    @Override public Optional<SelfDescribingConfig> config() { return Optional.empty(); }
}
