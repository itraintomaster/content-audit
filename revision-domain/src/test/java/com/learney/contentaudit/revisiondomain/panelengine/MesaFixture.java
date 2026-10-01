package com.learney.contentaudit.revisiondomain.panelengine;

import com.learney.contentaudit.auditdomain.AnalyzerCorrection;
import com.learney.contentaudit.auditdomain.AnalyzerCorrectionCatalog;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.auditdomain.QuizStudentViewRenderer;
import com.learney.contentaudit.auditdomain.answercompleteness.AnswerCompletenessCorrection;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditdomain.correctionengine.DefaultAnalyzerCorrectionCatalog;
import com.learney.contentaudit.auditdomain.correctionengine.LemmaAbsenceCorrection;
import com.learney.contentaudit.auditdomain.correctionengine.QuizInstructionCorrection;
import com.learney.contentaudit.auditdomain.datacoherence.DataCoherenceCorrection;
import com.learney.contentaudit.auditdomain.datacoherence.DefaultSpokenTextBuilder;
import com.learney.contentaudit.auditdomain.exactduplicates.ExactDuplicatesCorrection;
import com.learney.contentaudit.auditdomain.finding.AnalysisCost;
import com.learney.contentaudit.auditdomain.finding.EvidencePart;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingEvidence;
import com.learney.contentaudit.auditdomain.finding.FindingIdentity;
import com.learney.contentaudit.auditdomain.finding.FindingNodeRef;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.auditdomain.grammar.GrammarCorrection;
import com.learney.contentaudit.auditdomain.hintcoherence.HintCoherenceCorrection;
import com.learney.contentaudit.auditdomain.minitheoryengine.MiniTheoryCorrection;
import com.learney.contentaudit.auditdomain.multiplechoicevalidity.MultipleChoiceValidityCorrection;
import com.learney.contentaudit.auditdomain.nearduplicates.NearDuplicatesCorrection;
import com.learney.contentaudit.auditdomain.offlevelwords.OffLevelWordsCorrection;
import com.learney.contentaudit.auditdomain.singleanswerengine.SingleAnswerCorrection;
import com.learney.contentaudit.auditdomain.studentview.QuizStudentView;
import com.learney.contentaudit.auditdomain.translationsenseengine.TranslationSenseCorrection;
import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceItemEntity;
import com.learney.contentaudit.coursedomain.NodeKind;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.SentenceMode;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.refinerdomain.DiagnosisKind;
import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.refinerdomain.RefinementTaskStatus;
import com.learney.contentaudit.refinerdomain.TaskOrigin;
import com.learney.contentaudit.refinerdomain.errortask.ErrorTaskBrief;
import com.learney.contentaudit.revisiondomain.nonregression.LengthDeclaration;
import com.learney.contentaudit.revisiondomain.nonregression.RedWord;
import com.learney.contentaudit.revisiondomain.nonregression.SpokenText;
import com.learney.contentaudit.revisiondomain.nonregression.UnitMeasurement;
import com.learney.contentaudit.revisiondomain.nonregressionengine.DefaultGateFactory;
import com.learney.contentaudit.revisiondomain.panel.CandidateRejection;
import com.learney.contentaudit.revisiondomain.panel.ConversionOrder;
import com.learney.contentaudit.revisiondomain.panel.PanelCandidate;
import com.learney.contentaudit.revisiondomain.panel.PanelCandidateKind;
import com.learney.contentaudit.revisiondomain.panel.PanelDecision;
import com.learney.contentaudit.revisiondomain.panel.PanelDecisionSink;
import com.learney.contentaudit.revisiondomain.panel.PanelProposal;
import com.learney.contentaudit.revisiondomain.panel.PanelProposer;
import com.learney.contentaudit.revisiondomain.panel.PanelRequest;
import com.learney.contentaudit.revisiondomain.panel.PanelRunSettings;
import com.learney.contentaudit.revisiondomain.panel.QueryAllowance;
import com.learney.contentaudit.revisiondomain.panel.TopicMeter;
import com.learney.contentaudit.revisiondomain.panel.TopicWork;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The panel of «Afirmativas regulares» (A1, 29/9) and its neighbours, without a model: exercises written as the course
 * has them, findings as the analyzers give them, briefs as FEAT-TAREA hands them, a proposer that reads a script and a
 * meter that answers with rules -- so every test of FEAT-MESA reads like the case it comes from.
 */
public final class MesaFixture {

    public static final String BATCH = "tanda-A1-20261001T120000Z";

    /** «Afirmativas regulares», A1 (Present Simple). */
    public static final String AFIRMATIVAS = "6814dafa7d73e7209a13d389";

    /** Exercise 11 of Afirmativas regulares: «I ___ a lot of vegetables.» */
    public static final String VEGETABLES = "67fab6d59930102295341ecd";

    /** «He ___ (come) every year.», which «My grandmother ___ …» and «Winter ___ …» try to fix. */
    public static final String COME = "67fab6d59930102295341ec6";

    /** «Contraste inicial» (A1): nine exercises show the mini theory of the state verbs. */
    public static final String CONTRASTE = "67fab6d5993010229534207c";

    /** «Contracciones» (A1, Present Simple), which José orders to pass to multiple choice. */
    public static final String CONTRACCIONES = "6814dafa7d73e7209a13d3c1";

    public static final String STATE_VERBS = "Los verbos de estado (like, love, want) casi nunca van en -ing.";

    public static final List<String> ANALYZERS = List.of("sentence-length", "lemma-absence", "quiz-instruction",
            "hint-coherence", "answer-completeness", "multiple-choice-validity", "data-coherence", "exact-duplicates",
            "translation-sense", "single-answer", "mini-theory");

    private MesaFixture() {
    }

    // -------------------------------------------------------------------------
    // Exercises and topics
    // -------------------------------------------------------------------------

    public static SentencePartEntity text(String text) {
        return new SentencePartEntity(SentencePartKind.TEXT, text, null);
    }

    public static SentencePartEntity cloze(String... options) {
        return new SentencePartEntity(SentencePartKind.CLOZE, "", new ArrayList<>(Arrays.asList(options)));
    }

    /** A write-in exercise with its stored sentence built from its parts with the first answer, without hints. */
    public static QuizTemplateEntity quiz(String id, String translation, SentencePartEntity... parts) {
        FormEntity form = new FormEntity("CLOZE", 1.0, "", "", new ArrayList<>(Arrays.asList(parts)), null, null);
        QuizTemplateEntity quiz = new QuizTemplateEntity(id, id, "CLOZE", AFIRMATIVAS, "Afirmativas regulares",
                "Completa con el verbo en presente simple.", translation, null, "Present Simple", form, 0, 0, 0, null, null,
                null, audioKey(id), null, null, null, null, null);
        quiz.setSentences(new ArrayList<>(List.of(stored(quiz))));
        return quiz;
    }

    /** A multiple choice: the statement after the gap, the correct option first in the arguments, then the others. */
    public static QuizTemplateEntity choice(String id, String afterGap, String correct, String... others) {
        List<MultipleChoiceItemEntity> items = new ArrayList<>();
        items.add(new MultipleChoiceItemEntity(correct, 1.0, correct));
        for (String other : others) {
            items.add(new MultipleChoiceItemEntity(other, 0.0, other));
        }
        FormEntity form = new FormEntity("MULTIPLE_CHOICE", 1.0, "", "", new ArrayList<>(List.of(
                new SentencePartEntity(SentencePartKind.CLOZE, "", null), text(afterGap))),
                new MultipleChoiceEntity("SINGLE", items), null);
        QuizTemplateEntity quiz = new QuizTemplateEntity(id, id, "MULTIPLE_CHOICE", AFIRMATIVAS, "label",
                "Elige la opción correcta.", null, null, "Present Simple", form, 0, 0, 0, null, null, null, audioKey(id),
                null, null, null, null, null);
        quiz.setSentences(new ArrayList<>(List.of(stored(quiz))));
        return quiz;
    }

    public static String audioKey(String id) {
        return "A1.02.01." + id.substring(id.length() - 2);
    }

    /** The stored sentence of an exercise: its parts with the first answer or the correct option, without hints. */
    public static String stored(QuizTemplateEntity quiz) {
        String correct = TopicState.correctOption(quiz).orElse(null);
        StringBuilder sb = new StringBuilder();
        for (SentencePartEntity part : quiz.getForm().getSentenceParts()) {
            if (part.getKind() == SentencePartKind.CLOZE) {
                sb.append(' ').append(correct != null ? correct : TopicState.firstAnswer(part));
            } else {
                sb.append(' ').append(part.getText().replaceAll("\\([^)]*\\)", " "));
            }
        }
        return sb.toString().replaceAll("\\s+", " ").replaceAll("\\s+([.?!,])", "$1").strip();
    }

    public static KnowledgeEntity topic(String id, String label, List<QuizTemplateEntity> quizzes) {
        return new KnowledgeEntity(id, null, NodeKind.KNOWLEDGE, label, null, "t-present", false,
                "Usa el presente simple.", 0, null, new ArrayList<>(quizzes), SentenceMode.FILL);
    }

    /**
     * The twenty exercises of «Afirmativas regulares»: the eleventh is «I ___ a lot of vegetables.» without a hint, and
     * «My grandmother ___ (come) every year.» is the sixth.
     */
    public static KnowledgeEntity afirmativas() {
        List<QuizTemplateEntity> quizzes = new ArrayList<>();
        String[][] rows = {
                {"1ec1", "She", "works", "(work) in a bank.", "Ella trabaja en un banco."},
                {"1ec2", "They", "play", "(play) football on Sundays.", "Ellos juegan al fútbol los domingos."},
                {"1ec3", "He", "reads", "(read) the news every morning.", "Él lee las noticias cada mañana."},
                {"1ec4", "It", "comes", "(come) every year.", "Viene cada año."},
                {"1ec5", "He", "leaves", "(leave) the bedroom.", "Él sale del dormitorio."},
                {"1ec6", "He", "comes", "(come) every year.", "Él viene cada año."},
                {"1ec7", "We", "live", "(live) in a small town.", "Vivimos en un pueblo pequeño."},
                {"1ec8", "You", "eat", "(eat) a lot of fruit.", "Comes mucha fruta."},
                {"1ec9", "I", "drink", "(drink) tea in the morning.", "Tomo té por la mañana."},
                {"1eca", "She", "speaks", "(speak) three languages.", "Ella habla tres idiomas."},
                {"1ecd", "I", "love", "a lot of vegetables.", "Me gustan mucho las verduras."},
                {"1ece", "My dog", "sleeps", "(sleep) all day.", "Mi perro duerme todo el día."},
                {"1ecf", "They", "watch", "(watch) TV at night.", "Ellos miran televisión de noche."},
                {"1ed0", "He", "cooks", "(cook) dinner on Fridays.", "Él cocina la cena los viernes."},
                {"1ed1", "We", "walk", "(walk) to school.", "Caminamos a la escuela."},
                {"1ed2", "The shop", "opens", "(open) at nine.", "La tienda abre a las nueve."},
                {"1ed3", "I", "write", "(write) emails every day.", "Escribo correos todos los días."},
                {"1ed4", "She", "sings", "(sing) in a choir.", "Ella canta en un coro."},
                {"1ed5", "My parents", "travel", "(travel) in summer.", "Mis padres viajan en verano."},
                {"1ed6", "The bus", "stops", "(stop) here.", "El autobús para aquí."}};
        for (String[] row : rows) {
            quizzes.add(quiz("67fab6d5993010229534" + row[0], row[4], text(row[1]), cloze(row[2]), text(row[3])));
        }
        return topic(AFIRMATIVAS, "Afirmativas regulares", quizzes);
    }

    /** «Contraste inicial»: nine exercises show the mini theory of the state verbs, the tenth another one. */
    public static KnowledgeEntity contraste() {
        List<QuizTemplateEntity> quizzes = new ArrayList<>();
        String[][] rows = {
                {"20a1", "I", "am reading", "(read) a book now."}, {"20a2", "She", "likes", "(like) chocolate."},
                {"20a3", "They", "are playing", "(play) outside now."}, {"20a4", "He", "wants", "(want) a new bike."},
                {"20a5", "We", "are eating", "(eat) lunch now."}, {"20a6", "I", "love", "(love) my family."},
                {"20a7", "You", "are sleeping", "(sleep) now."}, {"20a8", "She", "needs", "(need) help."},
                {"20a9", "It", "is raining", "(rain) now."}, {"20aa", "He", "works", "(work) every day."}};
        for (int i = 0; i < rows.length; i++) {
            String[] row = rows[i];
            QuizTemplateEntity quiz = quiz("67fab6d5993010229534" + row[0], null, text(row[1]), cloze(row[2]),
                    text(row[3]));
            quiz.setKnowledgeId(CONTRASTE);
            quiz.setMiniTheory(i < 9 ? STATE_VERBS : "El presente simple habla de rutinas.");
            quizzes.add(quiz);
        }
        return topic(CONTRASTE, "Contraste inicial", quizzes);
    }

    public static List<String> stateVerbExercises() {
        List<String> ids = new ArrayList<>();
        for (QuizTemplateEntity quiz : contraste().getQuizTemplates()) {
            if (STATE_VERBS.equals(quiz.getMiniTheory())) {
                ids.add(quiz.getId());
            }
        }
        return ids;
    }

    /** «Contracciones»: three write-in exercises that ask for the short form of what is between parentheses. */
    public static KnowledgeEntity contracciones() {
        List<QuizTemplateEntity> quizzes = new ArrayList<>();
        quizzes.add(quiz("67fab6d59930102295343101", "No trabajas en el banco.", text("You"), cloze("don't"),
                text("(do not) work at the bank.")));
        quizzes.add(quiz("67fab6d59930102295343102", "Ella no está muy feliz.", text("She"), cloze("'s not|isn't"),
                text("(is not) very happy.")));
        quizzes.add(quiz("67fab6d59930102295343103", "Ellos no viven aquí.", text("They"), cloze("don't"),
                text("(do not) live here.")));
        for (QuizTemplateEntity quiz : quizzes) {
            quiz.setKnowledgeId(CONTRACCIONES);
            quiz.setInstructions("Escribe la $short form$ de lo que está entre paréntesis.");
        }
        KnowledgeEntity topic = topic(CONTRACCIONES, "Contracciones", quizzes);
        topic.setInstructions("Escribe la $short form$ de lo que está entre paréntesis.");
        return topic;
    }

    /** A conversion of a write-in exercise: the statement without the hint, the correct option and two others. */
    public static PanelCandidate conversion(QuizTemplateEntity quiz, String correct, String... others) {
        List<MultipleChoiceItemEntity> items = new ArrayList<>();
        items.add(new MultipleChoiceItemEntity(correct, 1.0, correct));
        for (String other : others) {
            items.add(new MultipleChoiceItemEntity(other, 0.0, other));
        }
        QuizTemplateEntity converted = TopicState.copy(quiz);
        converted.setKind("MULTIPLE_CHOICE");
        converted.getForm().setKind("MULTIPLE_CHOICE");
        List<SentencePartEntity> parts = new ArrayList<>();
        for (SentencePartEntity part : quiz.getForm().getSentenceParts()) {
            parts.add(part.getKind() == SentencePartKind.CLOZE ? new SentencePartEntity(SentencePartKind.CLOZE, "", null)
                    : text(part.getText()));
        }
        converted.getForm().setSentenceParts(parts);
        converted.getForm().setMultipleChoice(new MultipleChoiceEntity("SINGLE", items));
        converted.setInstructions("Elige la opción correcta.");
        converted.setSentences(new ArrayList<>(List.of(stored(converted))));
        return new PanelCandidate(PanelCandidateKind.MULTIPLE_CHOICE_CONVERSION, converted, null,
                "Elige la opción correcta para completar cada oración.", "pasa a opción múltiple");
    }

    public static QuizTemplateEntity exercise(KnowledgeEntity topic, String quizId) {
        return TopicState.quiz(topic, quizId).orElseThrow();
    }

    /** The exercise with what changes: a copy, its stored sentence built again. */
    public static QuizTemplateEntity rewritten(QuizTemplateEntity quiz, java.util.function.Consumer<QuizTemplateEntity> edit) {
        QuizTemplateEntity copy = TopicState.copy(quiz);
        edit.accept(copy);
        copy.setSentences(new ArrayList<>(List.of(stored(copy))));
        return copy;
    }

    public static QuizTemplateEntity withParts(QuizTemplateEntity quiz, SentencePartEntity... parts) {
        return rewritten(quiz, q -> q.getForm().setSentenceParts(new ArrayList<>(Arrays.asList(parts))));
    }

    public static QuizTemplateEntity withTranslation(QuizTemplateEntity quiz, String translation) {
        QuizTemplateEntity copy = TopicState.copy(quiz);
        copy.setTranslation(translation);
        return copy;
    }

    // -------------------------------------------------------------------------
    // Findings and tasks
    // -------------------------------------------------------------------------

    public static Finding finding(String analyzer, String rule, String quizId, FindingSeverity severity,
            FindingResolution resolution, String marker, String observation) {
        return new Finding(analyzer, rule, new FindingNodeRef(AuditTarget.QUIZ, quizId, quizId), severity,
                new FindingEvidence(new ArrayList<>(List.of(new EvidencePart("Ejercicio", quizId))), observation,
                        new ArrayList<>()), resolution, AnalysisCost.INSTANT,
                new FindingIdentity(analyzer + "|" + rule + "|" + quizId + (marker != null ? "|" + marker : ""), marker));
    }

    public static Finding topicFinding(String analyzer, String rule, String knowledgeId, FindingSeverity severity,
            String marker, String observation) {
        return new Finding(analyzer, rule, new FindingNodeRef(AuditTarget.KNOWLEDGE, knowledgeId, knowledgeId), severity,
                new FindingEvidence(new ArrayList<>(), observation, new ArrayList<>()), FindingResolution.PANEL,
                AnalysisCost.PAID_MODEL, new FindingIdentity(analyzer + "|" + rule + "|" + knowledgeId
                        + (marker != null ? "|" + marker : ""), marker));
    }

    /** The three findings of «I ___ a lot of vegetables.»: its missing hint, another answer that fits and its translation. */
    public static Finding missingHint() {
        return finding("hint-coherence", "missing-hint", VEGETABLES, FindingSeverity.HIGH, FindingResolution.RULE, null,
                "no lleva pista y 19 de 20 ejercicios del tema la llevan");
    }

    public static Finding anotherAnswer() {
        return finding("single-answer", "different-valid-answer", VEGETABLES, FindingSeverity.BLOCKING,
                FindingResolution.PANEL, null, "«like» también encaja y el corrector la rechaza");
    }

    public static Finding translationDiffers() {
        return finding("translation-sense", "same-meaning", VEGETABLES, FindingSeverity.HIGH, FindingResolution.PANEL,
                null, "«Me gustan mucho las verduras.» no dice «I love a lot of vegetables.»");
    }

    public static ErrorTaskBrief brief(String taskId, Finding finding) {
        return brief(taskId, finding, scopeOf(finding));
    }

    public static ErrorTaskBrief brief(String taskId, Finding finding, List<ExercisePart> scope) {
        String quizId = finding.getNode().getNodeId();
        RefinementTask task = new RefinementTask(taskId, AuditTarget.QUIZ, quizId, quizId, kindOf(finding.getAnalyzer()),
                1, RefinementTaskStatus.IN_BATCH, new TaskOrigin(finding, CefrLevel.A1, AFIRMATIVAS,
                        "Afirmativas regulares", new ArrayList<>(List.of(quizId)), new ArrayList<>()));
        return new ErrorTaskBrief(task, finding, new ArrayList<>(), new ArrayList<>(scope), !scope.isEmpty(), null,
                new ArrayList<>());
    }

    /** A task of the whole topic, as the mini theory, that reaches the given exercises. */
    public static ErrorTaskBrief topicBrief(String taskId, Finding finding, List<String> reached) {
        RefinementTask task = new RefinementTask(taskId, AuditTarget.KNOWLEDGE, finding.getNode().getNodeId(),
                "Contraste inicial", kindOf(finding.getAnalyzer()), 1, RefinementTaskStatus.IN_BATCH,
                new TaskOrigin(finding, CefrLevel.A1, finding.getNode().getNodeId(), "Contraste inicial",
                        new ArrayList<>(reached), new ArrayList<>()));
        return new ErrorTaskBrief(task, finding, new ArrayList<>(), new ArrayList<>(List.of(ExercisePart.MINI_THEORY)),
                true, null, new ArrayList<>());
    }

    public static List<ExercisePart> scopeOf(Finding finding) {
        return corrections().find(finding.getAnalyzer()).map(c -> c.editScope(finding.getResolution()))
                .orElse(List.of());
    }

    private static DiagnosisKind kindOf(String analyzer) {
        try {
            return DiagnosisKind.valueOf(analyzer.toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return null; // an analyzer without a kind of task of its own
        }
    }

    public static AnalyzerCorrectionCatalog corrections() {
        List<AnalyzerCorrection> declared = List.of(new QuizInstructionCorrection(), new LemmaAbsenceCorrection(),
                new HintCoherenceCorrection(), new AnswerCompletenessCorrection(), new MultipleChoiceValidityCorrection(),
                new DataCoherenceCorrection(), new ExactDuplicatesCorrection(), new TranslationSenseCorrection(),
                new SingleAnswerCorrection(), new MiniTheoryCorrection(), new OffLevelWordsCorrection(),
                new GrammarCorrection(), new NearDuplicatesCorrection());
        return new DefaultAnalyzerCorrectionCatalog(declared, null);
    }

    // -------------------------------------------------------------------------
    // The work of a topic and the panel
    // -------------------------------------------------------------------------

    public static TopicWork work(KnowledgeEntity topic, List<ErrorTaskBrief> briefs) {
        return work(topic, briefs, null, Map.of(), List.of());
    }

    public static TopicWork work(KnowledgeEntity topic, List<ErrorTaskBrief> briefs, ConversionOrder conversion,
            Map<String, Integer> attemptsUsed, List<CandidateRejection> carried) {
        return new TopicWork(BATCH, CefrLevel.A1, "m-a1", "t-present", topic, SentenceMode.FILL, new ArrayList<>(briefs),
                null, conversion, new HashMap<>(attemptsUsed), new ArrayList<>(carried));
    }

    public static PanelRunSettings settings() {
        return new PanelRunSettings(4, 4, 3, ANALYZERS);
    }

    public static DefaultTopicPanel panel(PanelProposer proposer, TopicMeter meter) {
        AnalyzerCorrectionCatalog corrections = corrections();
        return new DefaultTopicPanel(proposer, meter, new DefaultCandidateScreen(corrections),
                new DefaultPanelDecisionScript(corrections), new DefaultGateFactory().changeGate(), renderer())
                .withCorrections(corrections);
    }

    public static QueryAllowance allowance(Integer cap, int queriesAtOnce) {
        return new DefaultPanelRunnerFactory().allowance(cap, queriesAtOnce);
    }

    /** The statement as the student sees it: the gap as «___», hints kept. */
    public static QuizStudentViewRenderer renderer() {
        return new QuizStudentViewRenderer() {
            @Override
            public QuizStudentView render(AuditableQuiz quiz) {
                StringBuilder sb = new StringBuilder();
                for (SentencePartEntity part : quiz.getSentenceParts() != null ? quiz.getSentenceParts()
                        : List.<SentencePartEntity>of()) {
                    sb.append(' ').append(part.getKind() == SentencePartKind.CLOZE ? "___" : part.getText());
                }
                return new QuizStudentView(sb.toString().strip(), new ArrayList<>(), new ArrayList<>(),
                        quiz.getInstructions(), quiz.getTranslation(), quiz.getMiniTheory());
            }

            @Override
            public String finishedSentence(AuditableQuiz quiz, SentenceMode mode) {
                return quiz.getSentences() != null && !quiz.getSentences().isEmpty() ? quiz.getSentences().get(0) : null;
            }
        };
    }

    public static PanelCandidate candidate(PanelCandidateKind kind, QuizTemplateEntity quiz) {
        return new PanelCandidate(kind, quiz, null, null, "porque sí");
    }

    public static PanelProposal proposal(PanelCandidate... candidates) {
        return new PanelProposal(new ArrayList<>(Arrays.asList(candidates)), "guion", true, null);
    }

    /** The script of a scripted proposer: per exercise, one proposal per attempt. */
    public static Map<String, List<PanelProposal>> script(Object... keysAndProposals) {
        Map<String, List<PanelProposal>> script = new LinkedHashMap<>();
        for (int i = 0; i < keysAndProposals.length; i += 2) {
            script.computeIfAbsent((String) keysAndProposals[i], k -> new ArrayList<>())
                    .add((PanelProposal) keysAndProposals[i + 1]);
        }
        return script;
    }

    /** A proposer that answers from a script and keeps every request it got. */
    public static final class RecordingProposer implements PanelProposer {
        private final PanelProposer delegate;

        public final List<PanelRequest> requests = Collections.synchronizedList(new ArrayList<>());

        public RecordingProposer(Map<String, List<PanelProposal>> script) {
            this.delegate = new com.learney.contentaudit.revisiondomain.panel.ScriptedPanelProposer(script);
        }

        public RecordingProposer(PanelProposer delegate) {
            this.delegate = delegate;
        }

        @Override
        public PanelProposal propose(PanelRequest request) {
            requests.add(request);
            return delegate.propose(request);
        }
    }

    /** The decisions as they reached the batch. */
    public static final class RecordingSink implements PanelDecisionSink {
        public final List<PanelDecision> decisions = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void accept(PanelDecision decision) {
            decisions.add(decision);
        }

        public PanelDecision only() {
            if (decisions.size() != 1) {
                throw new AssertionError("expected one decision and got " + decisions);
            }
            return decisions.get(0);
        }
    }

    // -------------------------------------------------------------------------
    // The meter: the analyzers as rules over the topic as it is
    // -------------------------------------------------------------------------

    /**
     * A meter that answers what the analyzers would: each rule gives the findings of one analyzer for one exercise of
     * the topic as it stands, and only the analyzers asked for are measured. It keeps every measurement asked, and it
     * can play judges: a judge consults once per exercise content it has not seen, taking its turn on the allowance.
     */
    public static final class RulesMeter implements TopicMeter {
        private final Map<String, BiFunction<KnowledgeEntity, QuizTemplateEntity, List<Finding>>> rules =
                new LinkedHashMap<>();

        private final List<Function<QuizTemplateEntity, List<RedWord>>> redWords = new ArrayList<>();

        private final Set<String> judges = new HashSet<>();

        private final Set<String> seen = Collections.synchronizedSet(new HashSet<>());

        public final List<List<String>> analyzersAsked = Collections.synchronizedList(new ArrayList<>());

        public final List<KnowledgeEntity> statesMeasured = Collections.synchronizedList(new ArrayList<>());

        public final AtomicInteger consultations = new AtomicInteger();

        public final AtomicInteger inFlight = new AtomicInteger();

        public final AtomicInteger maxInFlight = new AtomicInteger();

        /** The consultation number at which the service fails, or 0 when it never does. */
        public int failAt;

        public long latencyMillis;

        public RulesMeter rule(String analyzer, BiFunction<KnowledgeEntity, QuizTemplateEntity, List<Finding>> rule) {
            rules.merge(analyzer, rule, (a, b) -> (topic, quiz) -> {
                List<Finding> both = new ArrayList<>(a.apply(topic, quiz));
                both.addAll(b.apply(topic, quiz));
                return both;
            });
            return this;
        }

        public RulesMeter judge(String analyzer, BiFunction<KnowledgeEntity, QuizTemplateEntity, List<Finding>> rule) {
            judges.add(analyzer);
            return rule(analyzer, rule);
        }

        public RulesMeter redWord(Function<QuizTemplateEntity, List<RedWord>> rule) {
            redWords.add(rule);
            return this;
        }

        @Override
        public UnitMeasurement measure(TopicWork work, KnowledgeEntity knowledgeState, List<String> analyzers,
                QueryAllowance allowance) {
            analyzersAsked.add(analyzers != null ? new ArrayList<>(analyzers) : null);
            statesMeasured.add(knowledgeState);
            List<Finding> findings = new ArrayList<>();
            List<RedWord> red = new ArrayList<>();
            List<SpokenText> spoken = new ArrayList<>();
            List<LengthDeclaration> lengths = new ArrayList<>();
            for (QuizTemplateEntity quiz : knowledgeState.getQuizTemplates()) {
                for (Map.Entry<String, BiFunction<KnowledgeEntity, QuizTemplateEntity, List<Finding>>> rule
                        : rules.entrySet()) {
                    if (analyzers != null && !analyzers.isEmpty() && !analyzers.contains(rule.getKey())) {
                        continue;
                    }
                    if (judges.contains(rule.getKey()) && !consult(rule.getKey(), quiz, allowance)) {
                        continue;
                    }
                    findings.addAll(rule.getValue().apply(knowledgeState, quiz));
                }
                for (Function<QuizTemplateEntity, List<RedWord>> rule : redWords) {
                    red.addAll(rule.apply(quiz));
                }
                spoken.add(new SpokenText(quiz.getId(), quiz.getAnswerAudioUrl(), new ArrayList<>(
                        new DefaultSpokenTextBuilder().spokenTexts(TopicState.auditable(quiz), SentenceMode.FILL))));
                String sentence = quiz.getSentences() != null && !quiz.getSentences().isEmpty()
                        ? quiz.getSentences().get(0) : stored(quiz);
                int tokens = sentence.split("\\s+").length;
                lengths.add(new LengthDeclaration(quiz.getId(), tokens, 3, 8, tokens >= 3 && tokens <= 8));
            }
            return new UnitMeasurement(CefrLevel.A1, findings, new ArrayList<>(), red, spoken, lengths);
        }

        /** A judge consults once per content it has not seen; false when the allowance did not let it. */
        private boolean consult(String judge, QuizTemplateEntity quiz, QueryAllowance allowance) {
            String fingerprint = judge + "|" + quiz.getId() + "|" + stored(quiz) + "|" + quiz.getTranslation();
            if (seen.contains(fingerprint)) {
                return true;
            }
            if (!Allowances.reserve(allowance, 1)) {
                return false;
            }
            int now = inFlight.incrementAndGet();
            maxInFlight.accumulateAndGet(now, Math::max);
            try {
                if (latencyMillis > 0) {
                    Thread.sleep(latencyMillis);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            int number = consultations.incrementAndGet();
            inFlight.decrementAndGet();
            if (failAt > 0 && number >= failAt) {
                allowance.settle(1, 0);
                allowance.trip("el servicio del modelo no responde");
                return false;
            }
            seen.add(fingerprint);
            allowance.settle(1, 1);
            return true;
        }

        public static List<Finding> none() {
            return new ArrayList<>();
        }
    }

    // -------------------------------------------------------------------------
    // The story of «I ___ a lot of vegetables.»
    // -------------------------------------------------------------------------

    /** Whether the exercise carries a hint between parentheses. */
    public static boolean hinted(QuizTemplateEntity quiz) {
        for (SentencePartEntity part : quiz.getForm().getSentenceParts()) {
            if (part.getKind() == SentencePartKind.TEXT && part.getText() != null && part.getText().contains("(")) {
                return true;
            }
        }
        return false;
    }

    /**
     * The analyzers of the vegetables: hint-coherence and single-answer mark it while it has no hint; translation-sense
     * while the translation does not say the English, and it finds «Me encantan las verduras.» unnatural.
     */
    public static RulesMeter vegetablesMeter() {
        return new RulesMeter()
                .rule("hint-coherence", (topic, quiz) -> quiz.getId().equals(VEGETABLES) && !hinted(quiz)
                        ? List.of(missingHint()) : RulesMeter.none())
                .rule("single-answer", (topic, quiz) -> quiz.getId().equals(VEGETABLES) && !hinted(quiz)
                        ? List.of(anotherAnswer()) : RulesMeter.none())
                .rule("translation-sense", (topic, quiz) -> {
                    if (!quiz.getId().equals(VEGETABLES)) {
                        return RulesMeter.none();
                    }
                    if ("Me encantan las verduras.".equals(quiz.getTranslation())
                            && stored(quiz).equals("I love a lot of vegetables.")) {
                        return List.of(finding("translation-sense", "natural", VEGETABLES, FindingSeverity.HIGH,
                                FindingResolution.PANEL, null, "«Me encantan las verduras.» no suena natural para "
                                        + "«a lot of»"));
                    }
                    return Objects.equals(quiz.getTranslation(), "Me gustan mucho las verduras.")
                            && stored(quiz).equals("I love a lot of vegetables.")
                            ? List.of(translationDiffers()) : RulesMeter.none();
                });
    }

    public static <T> Set<T> setOf(List<T> list) {
        return new HashSet<>(list);
    }
}
