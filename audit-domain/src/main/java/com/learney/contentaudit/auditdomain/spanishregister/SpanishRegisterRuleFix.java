package com.learney.contentaudit.auditdomain.spanishregister;

import com.learney.contentaudit.auditdomain.RuleFix;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingIdentity;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.rulefix.RuleFixRequest;
import com.learney.contentaudit.auditdomain.rulefix.RuleFixResult;
import com.learney.contentaudit.coursedomain.QuizTemplateEntities;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * F-MESA-R032: the instructions, the mini theory or the translation in «vos» written in «tú» by the table of
 * {@link Tuteo}, without the panel. Each finding names its text by its marker; it is fixed only when the text is left
 * with no word in «vos», and the other texts and the English of the exercise stay as they were.
 */
public final class SpanishRegisterRuleFix implements RuleFix {
    private record Text(String marker, String name, Function<QuizTemplateEntity, String> get,
            BiConsumer<QuizTemplateEntity, String> set, boolean instructs) {
    }

    private static final List<Text> TEXTS = List.of(
            new Text("consigna", "la consigna", QuizTemplateEntity::getInstructions, QuizTemplateEntity::setInstructions,
                    true),
            new Text("miniteoria", "la miniteoría", QuizTemplateEntity::getMiniTheory, QuizTemplateEntity::setMiniTheory,
                    true),
            new Text("traduccion", "la traducción", QuizTemplateEntity::getTranslation, QuizTemplateEntity::setTranslation,
                    false));

    @Override
    public String analyzerName() {
        return SpanishRegisterAnalyzer.ANALYZER_NAME;
    }

    @Override
    public RuleFixResult fix(RuleFixRequest request) {
        QuizTemplateEntity quiz = request.getQuiz();
        List<FindingIdentity> fixed = new ArrayList<>();
        List<FindingIdentity> declined = new ArrayList<>();
        if (quiz == null) {
            for (Finding finding : mine(request)) {
                declined.add(finding.getIdentity());
            }
            return new RuleFixResult(null, fixed, declined, "no hay ejercicio", List.of());
        }
        QuizTemplateEntity changed = QuizTemplateEntities.copyOf(quiz);
        List<String> written = new ArrayList<>();
        List<String> left = new ArrayList<>();
        for (Finding finding : mine(request)) {
            Text text = TEXTS.stream().filter(t -> t.marker().equals(finding.getIdentity().getMarker())).findFirst()
                    .orElse(null);
            String before = text != null ? text.get().apply(quiz) : null;
            String after = text != null ? Tuteo.of(before, text.instructs()) : null;
            if (text == null || Objects.equals(before, after) || !Voseo.in(after, text.instructs()).isEmpty()) {
                declined.add(finding.getIdentity());
                left.add(text != null ? text.name() : String.valueOf(finding.getIdentity().getMarker()));
                continue;
            }
            text.set().accept(changed, after);
            fixed.add(finding.getIdentity());
            written.add(text.name());
        }
        String detail = (written.isEmpty() ? "" : "de tú por la tabla: " + String.join(", ", written))
                + (left.isEmpty() ? "" : (written.isEmpty() ? "" : "; ") + "la tabla no la deja de tú: "
                        + String.join(", ", left));
        return new RuleFixResult(fixed.isEmpty() ? quiz : changed, fixed, declined, detail, List.of());
    }

    private List<Finding> mine(RuleFixRequest request) {
        List<Finding> mine = new ArrayList<>();
        for (Finding finding : request.getFindings() != null ? request.getFindings() : List.<Finding>of()) {
            if (finding != null && analyzerName().equals(finding.getAnalyzer())
                    && finding.getResolution() == FindingResolution.RULE && finding.getIdentity() != null) {
                mine.add(finding);
            }
        }
        return mine;
    }
}
