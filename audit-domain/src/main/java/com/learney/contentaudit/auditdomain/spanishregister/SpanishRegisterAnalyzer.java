package com.learney.contentaudit.auditdomain.spanishregister;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.ContentAnalyzer;
import com.learney.contentaudit.auditdomain.finding.EvidencePart;
import com.learney.contentaudit.auditdomain.finding.FindingDraft;
import com.learney.contentaudit.auditdomain.finding.FindingEvidence;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * F-MESA-R025: does what the student reads in Spanish go in «tú»? An errors analyzer over the instructions, the mini
 * theory and the translation of every exercise: each one with words in «vos» is a medium finding, which the panel
 * writes again in «tú». With one the exercise scores 0, with none 1.
 *
 * <p>F-MESA-R032: a text the table of {@link Tuteo} leaves wholly in «tú» is fixed by rule, without the panel.
 */
class SpanishRegisterAnalyzer implements ContentAnalyzer {

    static final String ANALYZER_NAME = "spanish-register";

    static final String DESCRIPTION = "Lee la consigna, la miniteoría y la traducción de cada ejercicio y marca las que"
            + " están de vos";

    /** R025: what the student reads goes in «tú». */
    static final String RULE_TU = "tu-register";

    private record Field(String label, String marker, Function<AuditableQuiz, String> text, boolean instructs) {
    }

    private static final List<Field> FIELDS = List.of(
            new Field("Consigna", "consigna", AuditableQuiz::getInstructions, true),
            new Field("Miniteoría", "miniteoria", AuditableQuiz::getMiniTheory, true),
            new Field("Traducción", "traduccion", AuditableQuiz::getTranslation, false));

    /** The findings of each exercise of this run. */
    private final Map<AuditNode, List<FindingDraft>> found = new IdentityHashMap<>();

    @Override
    public Void onQuiz(AuditNode node) {
        if (node == null || !(node.getEntity() instanceof AuditableQuiz quiz)) {
            return null;
        }
        List<FindingDraft> drafts = new ArrayList<>();
        for (Field field : FIELDS) {
            String text = field.text().apply(quiz);
            List<String> words = Voseo.in(text, field.instructs());
            if (words.isEmpty()) {
                continue;
            }
            String said = "«" + String.join("», «", words) + "»";
            boolean byRule = Voseo.in(Tuteo.of(text, field.instructs()), field.instructs()).isEmpty();
            drafts.add(new FindingDraft(RULE_TU, field.marker(), FindingSeverity.MEDIUM,
                    byRule ? FindingResolution.RULE : FindingResolution.PANEL,
                    new FindingEvidence(List.of(new EvidencePart(field.label(), text), new EvidencePart("De vos", said)),
                            "la " + field.label().toLowerCase() + " va de tú y dice " + said + ", de vos: «" + text
                                    + "». Se escribe de tú, como las consignas del curso.", List.of())));
        }
        found.put(node, List.copyOf(drafts));
        node.getScores().put(ANALYZER_NAME, drafts.isEmpty() ? 1.0 : 0.0);
        return null;
    }

    @Override
    public Void onKnowledge(AuditNode node) {
        return null;
    }

    @Override
    public Void onMilestone(AuditNode node) {
        return null;
    }

    @Override
    public Void onTopic(AuditNode node) {
        return null;
    }

    @Override
    public Void onCourseComplete(AuditNode rootNode) {
        return null;
    }

    @Override
    public String getName() {
        return ANALYZER_NAME;
    }

    @Override
    public AuditTarget getTarget() {
        return AuditTarget.QUIZ;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public List<FindingDraft> findingsAt(AuditNode node) {
        return node == null ? List.of() : found.getOrDefault(node, List.of());
    }
}
