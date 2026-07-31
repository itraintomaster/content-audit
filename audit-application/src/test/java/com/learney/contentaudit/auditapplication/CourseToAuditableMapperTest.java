package com.learney.contentaudit.auditapplication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.learney.contentaudit.auditdomain.AuditableCourse;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableMilestone;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.AuditableTopic;
import com.learney.contentaudit.auditdomain.NlpToken;
import com.learney.contentaudit.auditdomain.NlpTokenizer;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.coursedomain.quizsentence.QuizSentenceConverter;
import com.learney.contentaudit.coursedomain.quizsentence.QuizSentenceSerializationException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourseToAuditableMapperTest {

    @Test
    @DisplayName("Given a course with quizzes, when map is called, then analyzeTokensBatch is invoked and returns an AuditableCourse")
    @Tag("F-NLP")
    @Tag("F-NLP-R010")
    public void givenACourseWithQuizzesWhenMapIsCalledThenAnalyzeTokensBatchIsInvokedAndReturnsAnAuditableCourse() {
        NlpTokenizer nlpTokenizer = mock(NlpTokenizer.class);
        NlpToken token = new NlpToken("cat", "cat", "NOUN", 0, false, false);
        when(nlpTokenizer.analyzeTokensBatch(anyList()))
                .thenReturn(Map.of("cat", List.of(token)));

        SentencePartEntity part = new SentencePartEntity(SentencePartKind.TEXT, "cat", null);
        FormEntity form = new FormEntity(null, 0, null, null, List.of(part));
        QuizTemplateEntity qt = new QuizTemplateEntity();
        qt.setId("q1");
        qt.setForm(form);
        qt.setSentences(List.of("cat"));
        KnowledgeEntity ke = new KnowledgeEntity();
        ke.setId("k1");
        ke.setLabel("label");
        ke.setQuizTemplates(List.of(qt));
        TopicEntity te = new TopicEntity();
        te.setId("t1");
        te.setLabel("label");
        te.setKnowledges(List.of(ke));
        MilestoneEntity me = new MilestoneEntity();
        me.setId("m1");
        me.setLabel("A1");
        me.setTopics(List.of(te));
        RootNodeEntity root = new RootNodeEntity();
        root.setMilestones(List.of(me));
        CourseEntity course = new CourseEntity();
        course.setRoot(root);

        QuizSentenceConverter quizSentenceConverter = mock(QuizSentenceConverter.class);
        when(quizSentenceConverter.serialize(form)).thenReturn("cat");
        CourseToAuditableMapper mapper = new CourseToAuditableMapper(nlpTokenizer, quizSentenceConverter);
        AuditableCourse result = mapper.map(course);

        verify(nlpTokenizer).analyzeTokensBatch(anyList());
        assertNotNull(result);
        assertEquals(1, result.getMilestones().size());
    }

    @Test
    @DisplayName("Given a course with no milestones, when map is called, then returns an AuditableCourse without error")
    @Tag("F-NLP")
    @Tag("F-NLP-R010")
    public void givenACourseWithNoMilestonesWhenMapIsCalledThenReturnsAnAuditableCourseWithoutError() {
        NlpTokenizer nlpTokenizer = mock(NlpTokenizer.class);
        when(nlpTokenizer.analyzeTokensBatch(anyList())).thenReturn(Map.of());

        CourseEntity course = new CourseEntity();
        course.setRoot(new RootNodeEntity());

        QuizSentenceConverter quizSentenceConverter = mock(QuizSentenceConverter.class);
        CourseToAuditableMapper mapper = new CourseToAuditableMapper(nlpTokenizer, quizSentenceConverter);
        AuditableCourse result = mapper.map(course);

        assertNotNull(result);
        assertTrue(result.getMilestones().isEmpty());
    }

    @Test
    @DisplayName("Given nlpTokenizer throws exception during batch processing, when map is called, then exception propagates")
    @Tag("F-NLP")
    @Tag("F-NLP-R008")
    public void givenNlpTokenizerThrowsExceptionDuringBatchProcessingWhenMapIsCalledThenExceptionPropagates() {
        NlpTokenizer nlpTokenizer = mock(NlpTokenizer.class);
        when(nlpTokenizer.analyzeTokensBatch(anyList()))
                .thenThrow(new RuntimeException("NLP failure"));

        SentencePartEntity part = new SentencePartEntity(SentencePartKind.TEXT, "cat", null);
        FormEntity form = new FormEntity(null, 0, null, null, List.of(part));
        QuizTemplateEntity qt = new QuizTemplateEntity();
        qt.setId("q1");
        qt.setForm(form);
        qt.setSentences(List.of("cat"));
        KnowledgeEntity ke = new KnowledgeEntity();
        ke.setId("k1");
        ke.setQuizTemplates(List.of(qt));
        TopicEntity te = new TopicEntity();
        te.setId("t1");
        te.setKnowledges(List.of(ke));
        MilestoneEntity me = new MilestoneEntity();
        me.setId("m1");
        me.setTopics(List.of(te));
        RootNodeEntity root = new RootNodeEntity();
        root.setMilestones(List.of(me));
        CourseEntity course = new CourseEntity();
        course.setRoot(root);

        QuizSentenceConverter quizSentenceConverter = mock(QuizSentenceConverter.class);
        when(quizSentenceConverter.serialize(form)).thenReturn("cat");
        CourseToAuditableMapper mapper = new CourseToAuditableMapper(nlpTokenizer, quizSentenceConverter);
        assertThrows(RuntimeException.class, () -> mapper.map(course));
    }

    // -----------------------------------------------------------------------
    // FEAT-RCLAQS tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("should stamp quizSentence on AuditableQuiz by invoking QuizSentenceConverter.serialize on the same FormEntity used for sentences")
    @Tag("FEAT-RCLAQS")
    @Tag("F-RCLAQS-R002")
    public void shouldStampQuizSentenceOnAuditableQuizByInvokingQuizSentenceConverterserializeOnTheSameFormEntityUsedForSentences() {
        // R002: the mapper must invoke QuizSentenceConverter.serialize(form) to derive
        // quizSentence and stamp the result on AuditableQuiz. This ensures the DSL value
        // is produced by the canonical converter, not reimplemented elsewhere.
        NlpTokenizer nlpTokenizer = mock(NlpTokenizer.class);
        QuizSentenceConverter quizSentenceConverter = mock(QuizSentenceConverter.class);

        SentencePartEntity textPart = new SentencePartEntity(SentencePartKind.TEXT, "He", null);
        SentencePartEntity clozePart = new SentencePartEntity(SentencePartKind.CLOZE, null, List.of("is|'s"));
        SentencePartEntity tailPart = new SentencePartEntity(SentencePartKind.TEXT, " (to be) great.", null);
        FormEntity form = new FormEntity(null, 0, null, null, List.of(textPart, clozePart, tailPart));

        QuizTemplateEntity qt = new QuizTemplateEntity();
        qt.setId("q-rclaqs-r002a");
        qt.setForm(form);
        qt.setSentences(List.of("He is great.", "He 's great."));
        KnowledgeEntity ke = new KnowledgeEntity();
        ke.setId("k-rclaqs-r002a");
        ke.setLabel("label");
        ke.setQuizTemplates(List.of(qt));
        TopicEntity te = new TopicEntity();
        te.setId("t-rclaqs-r002a");
        te.setLabel("label");
        te.setKnowledges(List.of(ke));
        MilestoneEntity me = new MilestoneEntity();
        me.setId("m-rclaqs-r002a");
        me.setLabel("A1");
        me.setTopics(List.of(te));
        RootNodeEntity root = new RootNodeEntity();
        root.setMilestones(List.of(me));
        CourseEntity course = new CourseEntity();
        course.setRoot(root);

        String expectedDsl = "He ____ [is|'s] (to be) great.";
        when(quizSentenceConverter.serialize(form)).thenReturn(expectedDsl);
        when(nlpTokenizer.analyzeTokensBatch(anyList())).thenReturn(Map.of());

        CourseToAuditableMapper mapper = new CourseToAuditableMapper(nlpTokenizer, quizSentenceConverter);
        AuditableCourse result = mapper.map(course);

        // R002: serialize must have been invoked to produce the DSL
        verify(quizSentenceConverter, times(1)).serialize(form);

        // R002: the AuditableQuiz must carry the serialized DSL from the converter
        AuditableQuiz auditableQuiz = result.getMilestones().get(0)
                .getTopics().get(0)
                .getKnowledge().get(0)
                .getQuizzes().get(0);
        assertEquals(expectedDsl, auditableQuiz.getQuizSentence(),
                "quizSentence must be the result of QuizSentenceConverter.serialize(form)");
    }

    @Test
    @DisplayName("should invoke QuizSentenceConverter.serialize exactly once per quiz in the same pass that populates sentences")
    @Tag("FEAT-RCLAQS")
    @Tag("F-RCLAQS-R002")
    public void shouldInvokeQuizSentenceConverterserializeExactlyOncePerQuizInTheSamePassThatPopulatesSentences() {
        // R002 + R027: serialize and toPlainSentences must both be called exactly once per quiz
        // on the same FormEntity (same pass). With two quizzes: 2 serialize calls + 2
        // toPlainSentences calls, all form-scoped.
        NlpTokenizer nlpTokenizer = mock(NlpTokenizer.class);
        QuizSentenceConverter quizSentenceConverter = mock(QuizSentenceConverter.class);

        SentencePartEntity part1 = new SentencePartEntity(SentencePartKind.TEXT, "The cat sits.", null);
        FormEntity form1 = new FormEntity(null, 0, null, null, List.of(part1));
        QuizTemplateEntity qt1 = new QuizTemplateEntity();
        qt1.setId("q-rclaqs-r002b-1");
        qt1.setForm(form1);
        qt1.setSentences(List.of("The cat sits."));

        SentencePartEntity part2 = new SentencePartEntity(SentencePartKind.TEXT, "The dog runs.", null);
        FormEntity form2 = new FormEntity(null, 0, null, null, List.of(part2));
        QuizTemplateEntity qt2 = new QuizTemplateEntity();
        qt2.setId("q-rclaqs-r002b-2");
        qt2.setForm(form2);
        qt2.setSentences(List.of("The dog runs."));

        KnowledgeEntity ke = new KnowledgeEntity();
        ke.setId("k-rclaqs-r002b");
        ke.setLabel("Animals");
        ke.setQuizTemplates(List.of(qt1, qt2));
        TopicEntity te = new TopicEntity();
        te.setId("t-rclaqs-r002b");
        te.setLabel("Animals");
        te.setKnowledges(List.of(ke));
        MilestoneEntity me = new MilestoneEntity();
        me.setId("m-rclaqs-r002b");
        me.setLabel("A1");
        me.setTopics(List.of(te));
        RootNodeEntity root = new RootNodeEntity();
        root.setMilestones(List.of(me));
        CourseEntity course = new CourseEntity();
        course.setRoot(root);

        when(quizSentenceConverter.serialize(form1)).thenReturn("The cat sits.");
        when(quizSentenceConverter.serialize(form2)).thenReturn("The dog runs.");
        when(nlpTokenizer.analyzeTokensBatch(anyList())).thenReturn(Map.of());

        CourseToAuditableMapper mapper = new CourseToAuditableMapper(nlpTokenizer, quizSentenceConverter);
        AuditableCourse result = mapper.map(course);

        // R002: serialize called exactly once per quiz — no extra calls
        verify(quizSentenceConverter, times(1)).serialize(form1);
        verify(quizSentenceConverter, times(1)).serialize(form2);

        // Both quizzes carry their respective quizSentence values
        List<AuditableQuiz> quizzes = result.getMilestones().get(0)
                .getTopics().get(0)
                .getKnowledge().get(0)
                .getQuizzes();
        assertEquals("The cat sits.", quizzes.get(0).getQuizSentence());
        assertEquals("The dog runs.", quizzes.get(1).getQuizSentence());
    }

    @Test
    @DisplayName("should stamp sentences[0] and quizSentence from the same FormEntity so plain sentence and DSL describe the same derivation step")
    @Tag("FEAT-RCLAQS")
    @Tag("F-RCLAQS-R003")
    public void shouldStampSentences0AndQuizSentenceFromTheSameFormEntitySoPlainSentenceAndDSLDescribeTheSameDerivationStep() {
        // R003: quizSentence and sentences both derive from the same FormEntity in the same
        // mapper pass. This structural test verifies that serialize(form) and
        // toPlainSentences(form) are both called with the same FormEntity reference,
        // ensuring the consistency invariant is structural rather than coincidental.
        NlpTokenizer nlpTokenizer = mock(NlpTokenizer.class);
        QuizSentenceConverter quizSentenceConverter = mock(QuizSentenceConverter.class);

        SentencePartEntity textPart = new SentencePartEntity(SentencePartKind.TEXT, "She sings.", null);
        FormEntity form = new FormEntity(null, 0, null, null, List.of(textPart));

        QuizTemplateEntity qt = new QuizTemplateEntity();
        qt.setId("q-rclaqs-r003");
        qt.setForm(form);
        qt.setSentences(List.of("She sings."));
        KnowledgeEntity ke = new KnowledgeEntity();
        ke.setId("k-rclaqs-r003");
        ke.setLabel("Music");
        ke.setQuizTemplates(List.of(qt));
        TopicEntity te = new TopicEntity();
        te.setId("t-rclaqs-r003");
        te.setLabel("Arts");
        te.setKnowledges(List.of(ke));
        MilestoneEntity me = new MilestoneEntity();
        me.setId("m-rclaqs-r003");
        me.setLabel("A1");
        me.setTopics(List.of(te));
        RootNodeEntity root = new RootNodeEntity();
        root.setMilestones(List.of(me));
        CourseEntity course = new CourseEntity();
        course.setRoot(root);

        String dsl = "She sings.";
        List<String> plains = List.of("She sings.");
        when(quizSentenceConverter.serialize(form)).thenReturn(dsl);
        when(nlpTokenizer.analyzeTokensBatch(anyList())).thenReturn(Map.of());

        CourseToAuditableMapper mapper = new CourseToAuditableMapper(nlpTokenizer, quizSentenceConverter);
        AuditableCourse result = mapper.map(course);

        // R003: serialize invoked once — sentences come from the entity, not from toPlainSentences
        verify(quizSentenceConverter, times(1)).serialize(form);

        // Both outputs are present on the same AuditableQuiz
        AuditableQuiz auditableQuiz = result.getMilestones().get(0)
                .getTopics().get(0)
                .getKnowledge().get(0)
                .getQuizzes().get(0);
        assertEquals(dsl, auditableQuiz.getQuizSentence());
        assertEquals(plains, auditableQuiz.getSentences());
    }

    @Test
    @DisplayName("should fail atomically without a partial quizSentence when serialize throws for a TEXT form with non-empty options")
    @Tag("FEAT-RCLAQS")
    @Tag("F-RCLAQS-R004")
    public void shouldFailAtomicallyWithoutAPartialQuizSentenceWhenSerializeThrowsForATEXTFormWithNonemptyOptions() {
        // R004: if QuizSentenceConverter.serialize throws QuizSentenceSerializationException
        // (e.g., TEXT part has non-empty options — FEAT-QSENT R003 violation), the mapper
        // must propagate that exception atomically. No partial AuditableQuiz is produced.
        NlpTokenizer nlpTokenizer = mock(NlpTokenizer.class);
        QuizSentenceConverter quizSentenceConverter = mock(QuizSentenceConverter.class);

        // Invalid: TEXT part with non-empty options violates FEAT-QSENT R003
        SentencePartEntity invalidTextPart = new SentencePartEntity(SentencePartKind.TEXT, "He is", List.of("invalid-option"));
        FormEntity invalidForm = new FormEntity(null, 0, null, null, List.of(invalidTextPart));

        QuizTemplateEntity qt = new QuizTemplateEntity();
        qt.setId("q-rclaqs-r004-text");
        qt.setForm(invalidForm);
        KnowledgeEntity ke = new KnowledgeEntity();
        ke.setId("k-rclaqs-r004");
        ke.setLabel("Grammar");
        ke.setQuizTemplates(List.of(qt));
        TopicEntity te = new TopicEntity();
        te.setId("t-rclaqs-r004");
        te.setLabel("Grammar");
        te.setKnowledges(List.of(ke));
        MilestoneEntity me = new MilestoneEntity();
        me.setId("m-rclaqs-r004");
        me.setLabel("A1");
        me.setTopics(List.of(te));
        RootNodeEntity root = new RootNodeEntity();
        root.setMilestones(List.of(me));
        CourseEntity course = new CourseEntity();
        course.setRoot(root);

        // The converter throws because TEXT with non-empty options is invalid (FEAT-QSENT R003)
        QuizSentenceSerializationException serializationException =
                new QuizSentenceSerializationException("TEXT part must not have options", "0");
        when(quizSentenceConverter.serialize(invalidForm)).thenThrow(serializationException);

        CourseToAuditableMapper mapper = new CourseToAuditableMapper(nlpTokenizer, quizSentenceConverter);

        // R004: the exception must propagate atomically — no partial result
        assertThrows(QuizSentenceSerializationException.class, () -> mapper.map(course),
                "QuizSentenceSerializationException must propagate when TEXT has non-empty options");
    }

    @Test
    @DisplayName("should fail atomically without a partial quizSentence when serialize throws for a CLOZE form with null or empty options")
    @Tag("FEAT-RCLAQS")
    @Tag("F-RCLAQS-R004")
    public void shouldFailAtomicallyWithoutAPartialQuizSentenceWhenSerializeThrowsForACLOZEFormWithNullOrEmptyOptions() {
        // R004: CLOZE with null or empty options violates FEAT-QSENT R004. The converter
        // throws QuizSentenceSerializationException; the mapper must propagate it atomically.
        NlpTokenizer nlpTokenizer = mock(NlpTokenizer.class);
        QuizSentenceConverter quizSentenceConverter = mock(QuizSentenceConverter.class);

        // Invalid: CLOZE with null options violates FEAT-QSENT R004
        SentencePartEntity clozePart = new SentencePartEntity(SentencePartKind.CLOZE, null, null);
        FormEntity invalidForm = new FormEntity(null, 0, null, null, List.of(clozePart));

        QuizTemplateEntity qt = new QuizTemplateEntity();
        qt.setId("q-rclaqs-r004-cloze");
        qt.setForm(invalidForm);
        KnowledgeEntity ke = new KnowledgeEntity();
        ke.setId("k-rclaqs-r004b");
        ke.setLabel("Grammar");
        ke.setQuizTemplates(List.of(qt));
        TopicEntity te = new TopicEntity();
        te.setId("t-rclaqs-r004b");
        te.setLabel("Grammar");
        te.setKnowledges(List.of(ke));
        MilestoneEntity me = new MilestoneEntity();
        me.setId("m-rclaqs-r004b");
        me.setLabel("A1");
        me.setTopics(List.of(te));
        RootNodeEntity root = new RootNodeEntity();
        root.setMilestones(List.of(me));
        CourseEntity course = new CourseEntity();
        course.setRoot(root);

        // The converter throws because CLOZE without options is invalid (FEAT-QSENT R004)
        QuizSentenceSerializationException serializationException =
                new QuizSentenceSerializationException("CLOZE part must have at least one option", "0");
        when(quizSentenceConverter.serialize(invalidForm)).thenThrow(serializationException);

        CourseToAuditableMapper mapper = new CourseToAuditableMapper(nlpTokenizer, quizSentenceConverter);

        // R004: exception propagates atomically
        assertThrows(QuizSentenceSerializationException.class, () -> mapper.map(course),
                "QuizSentenceSerializationException must propagate when CLOZE has no options");
    }

    @Test
    @DisplayName("should propagate QuizSentenceSerializationException and not emit the AuditableQuiz when the original FormEntity violates FEAT-QSENT invariants")
    @Tag("FEAT-RCLAQS")
    @Tag("F-RCLAQS-R004")
    public void shouldPropagateQuizSentenceSerializationExceptionAndNotEmitTheAuditableQuizWhenTheOriginalFormEntityViolatesFEATQSENTInvariants() {
        // R004: when serialize throws on an invalid FormEntity, the mapping is aborted entirely.
        // The quiz never enters the AuditableCourse. No partial structure is returned.
        // This verifies the atomic failure semantics: either everything succeeds or nothing does.
        NlpTokenizer nlpTokenizer = mock(NlpTokenizer.class);
        QuizSentenceConverter quizSentenceConverter = mock(QuizSentenceConverter.class);

        SentencePartEntity validPart = new SentencePartEntity(SentencePartKind.TEXT, "She runs.", null);
        FormEntity validForm = new FormEntity(null, 0, null, null, List.of(validPart));

        // CLOZE with empty options list — invalid per FEAT-QSENT R004
        SentencePartEntity invalidClozePart = new SentencePartEntity(SentencePartKind.CLOZE, null, List.of());
        FormEntity invalidForm = new FormEntity(null, 0, null, null, List.of(invalidClozePart));

        QuizTemplateEntity qtValid = new QuizTemplateEntity();
        qtValid.setId("q-rclaqs-r004c-valid");
        qtValid.setForm(validForm);
        qtValid.setSentences(List.of("She runs."));

        QuizTemplateEntity qtInvalid = new QuizTemplateEntity();
        qtInvalid.setId("q-rclaqs-r004c-invalid");
        qtInvalid.setForm(invalidForm);

        KnowledgeEntity ke = new KnowledgeEntity();
        ke.setId("k-rclaqs-r004c");
        ke.setLabel("Mixed");
        ke.setQuizTemplates(List.of(qtValid, qtInvalid));
        TopicEntity te = new TopicEntity();
        te.setId("t-rclaqs-r004c");
        te.setLabel("Mixed");
        te.setKnowledges(List.of(ke));
        MilestoneEntity me = new MilestoneEntity();
        me.setId("m-rclaqs-r004c");
        me.setLabel("A1");
        me.setTopics(List.of(te));
        RootNodeEntity root = new RootNodeEntity();
        root.setMilestones(List.of(me));
        CourseEntity course = new CourseEntity();
        course.setRoot(root);

        when(quizSentenceConverter.serialize(validForm)).thenReturn("She runs.");
        QuizSentenceSerializationException ex =
                new QuizSentenceSerializationException("CLOZE must have at least one option", "0");
        when(quizSentenceConverter.serialize(invalidForm)).thenThrow(ex);

        CourseToAuditableMapper mapper = new CourseToAuditableMapper(nlpTokenizer, quizSentenceConverter);

        // R004: the exception propagates — no AuditableCourse is returned, not even partially
        assertThrows(QuizSentenceSerializationException.class, () -> mapper.map(course),
                "Atomic failure: exception from invalid FormEntity prevents AuditableCourse production");
    }

    @Test
    @DisplayName("Given two AuditableCourses produced by mapping a course that has knowledge quizzes containing sentences with enriched-token-requiring vocabulary (frequency-ranked words via SpaCy) when AuditEngine runs both audits then every AuditableQuiz produced by the mapper carries a List<NlpToken> populated by analyzeTokensBatch covering tokenization lemmatization POS tagging frequency rank stop-word and punctuation flags so downstream analyzers can read enriched tokens without re-tokenizing")
    @Tag("FEAT-NLP")
    @Tag("F-NLP-R010")
    @Tag("F-NLP-J001")
    public void givenTwoAuditableCoursesProducedByMappingACourseThatHasKnowledgeQuizzesContainingSentencesWithEnrichedtokenrequiringVocabularyFrequencyrankedWordsViaSpaCyWhenAuditEngineRunsBothAuditsThenEveryAuditableQuizProducedByTheMapperCarriesAListNlpTokenPopulatedByAnalyzeTokensBatchCoveringTokenizationLemmatizationPOSTaggingFrequencyRankStopwordAndPunctuationFlagsSoDownstreamAnalyzersCanReadEnrichedTokensWithoutRetokenizing() {
        // R010: tokens are generated ONCE during mapping via analyzeTokensBatch and stored
        // in AuditableQuiz.getTokens() — downstream analyzers read pre-computed tokens, no re-tokenizing.
        // J001 step 5-6: tokenizer returns enriched tokens (lemma, posTag, frequencyRank, isStop, isPunct).

        // Two enriched tokens representing frequency-ranked vocabulary
        NlpToken runToken = new NlpToken("running", "run", "VERB", 150, false, false);
        NlpToken sheToken = new NlpToken("She", "she", "PRON", 12, true, false);

        NlpTokenizer nlpTokenizer = mock(NlpTokenizer.class);
        // Both mapping calls return the same enriched batch result
        when(nlpTokenizer.analyzeTokensBatch(anyList()))
                .thenReturn(Map.of("She was running", List.of(sheToken, runToken)));

        QuizSentenceConverter quizSentenceConverter = mock(QuizSentenceConverter.class);

        // Build a course with one quiz whose sentence contains frequency-ranked vocabulary
        SentencePartEntity part = new SentencePartEntity(SentencePartKind.TEXT, "She was running", null);
        FormEntity form = new FormEntity(null, 0, null, null, List.of(part));
        QuizTemplateEntity qt = new QuizTemplateEntity();
        qt.setId("q1");
        qt.setForm(form);
        qt.setSentences(List.of("She was running"));
        when(quizSentenceConverter.serialize(form)).thenReturn("She was running");
        KnowledgeEntity ke = new KnowledgeEntity();
        ke.setId("k1");
        ke.setLabel("Present Continuous");
        ke.setQuizTemplates(List.of(qt));
        TopicEntity te = new TopicEntity();
        te.setId("t1");
        te.setLabel("Verbs");
        te.setKnowledges(List.of(ke));
        MilestoneEntity me = new MilestoneEntity();
        me.setId("m1");
        me.setLabel("A2");
        me.setTopics(List.of(te));
        RootNodeEntity root = new RootNodeEntity();
        root.setMilestones(List.of(me));
        CourseEntity course = new CourseEntity();
        course.setRoot(root);

        CourseToAuditableMapper mapper = new CourseToAuditableMapper(nlpTokenizer, quizSentenceConverter);

        // Map the same course twice — two independent AuditableCourse instances
        AuditableCourse first = mapper.map(course);
        AuditableCourse second = mapper.map(course);

        // Both mappings must have invoked analyzeTokensBatch (R010: tokens generated at mapping time)
        verify(nlpTokenizer, times(2)).analyzeTokensBatch(anyList());

        // Verify that every AuditableQuiz in both results carries the enriched token list
        for (AuditableCourse auditableCourse : List.of(first, second)) {
            AuditableMilestone milestone = auditableCourse.getMilestones().get(0);
            AuditableTopic topic = milestone.getTopics().get(0);
            AuditableKnowledge knowledge = topic.getKnowledge().get(0);
            AuditableQuiz quiz = knowledge.getQuizzes().get(0);

            List<NlpToken> tokens = quiz.getTokens();
            assertNotNull(tokens, "R010: AuditableQuiz.getTokens() must not be null after mapping");
            assertFalse(tokens.isEmpty(), "R010: AuditableQuiz must carry the enriched token list");

            // Verify enriched fields are present on the tokens (lemma, posTag, frequencyRank, isStop, isPunct)
            NlpToken verbToken = tokens.stream()
                    .filter(t -> "run".equals(t.getLemma()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(verbToken, "R010: token with lemma 'run' must be present from enriched batch");
            assertEquals("VERB", verbToken.getPosTag(), "R010: POS tag must be populated from enriched tokenization");
            assertEquals(150, verbToken.getFrequencyRank(), "R010: frequencyRank must be populated from COCA frequency data");
            assertFalse(verbToken.isIsStop(), "R010: isStop flag must be set from enriched tokenization");
            assertFalse(verbToken.isIsPunct(), "R010: isPunct flag must be set from enriched tokenization");

            NlpToken pronToken = tokens.stream()
                    .filter(t -> "she".equals(t.getLemma()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(pronToken, "R010: token with lemma 'she' must be present from enriched batch");
            assertTrue(pronToken.isIsStop(), "R010: stop-word flag must be populated from enriched tokenization");
        }
    }

    @Test
    @DisplayName("should propagate QuizTemplateEntity sentences verbatim to AuditableQuiz sentences without modification")
    @Tag("FEAT-DBSENT")
    @Tag("F-DBSENT-R002")
    public void shouldPropagateQuizTemplateEntitySentencesVerbatimToAuditableQuizSentencesWithoutModification() {
        // R002: sentences come from QuizTemplateEntity.sentences, propagated verbatim — no derivation.
        NlpTokenizer nlpTokenizer = mock(NlpTokenizer.class);
        QuizSentenceConverter quizSentenceConverter = mock(QuizSentenceConverter.class);

        List<String> persistedSentences = List.of("The dogs are barking loudly.");

        SentencePartEntity part = new SentencePartEntity(SentencePartKind.TEXT, "The dogs are barking loudly.", null);
        FormEntity form = new FormEntity(null, 0, null, null, List.of(part));

        QuizTemplateEntity qt = new QuizTemplateEntity();
        qt.setId("q-dbsent-r002a");
        qt.setForm(form);
        qt.setSentences(persistedSentences);

        KnowledgeEntity ke = new KnowledgeEntity();
        ke.setId("k-dbsent-r002a");
        ke.setLabel("Present Continuous");
        ke.setQuizTemplates(List.of(qt));
        TopicEntity te = new TopicEntity();
        te.setId("t-dbsent-r002a");
        te.setLabel("Verbs");
        te.setKnowledges(List.of(ke));
        MilestoneEntity me = new MilestoneEntity();
        me.setId("m-dbsent-r002a");
        me.setLabel("A1");
        me.setTopics(List.of(te));
        RootNodeEntity root = new RootNodeEntity();
        root.setMilestones(List.of(me));
        CourseEntity course = new CourseEntity();
        course.setRoot(root);

        // serialize() is still invoked for the quizSentence DSL (FEAT-RCLAQS-R002)
        when(quizSentenceConverter.serialize(form)).thenReturn("The dogs are barking loudly.");
        when(nlpTokenizer.analyzeTokensBatch(anyList())).thenReturn(Map.of());

        CourseToAuditableMapper mapper = new CourseToAuditableMapper(nlpTokenizer, quizSentenceConverter);
        AuditableCourse result = mapper.map(course);

        AuditableQuiz auditableQuiz = result.getMilestones().get(0)
                .getTopics().get(0)
                .getKnowledge().get(0)
                .getQuizzes().get(0);

        // R002: sentences must be the pre-computed list from the entity — no modification
        assertEquals(persistedSentences, auditableQuiz.getSentences(),
                "R002: AuditableQuiz.sentences must equal QuizTemplateEntity.sentences verbatim");
    }

    @Test
    @DisplayName("should not invoke QuizSentenceConverter.toPlainSentences for any quiz of the audited course")
    @Tag("FEAT-DBSENT")
    @Tag("F-DBSENT-R002")
    public void shouldNotInvokeQuizSentenceConvertertoPlainSentencesForAnyQuizOfTheAuditedCourse() {
        // R002: toPlainSentences is no longer invoked during the audit mapping pass.
        // serialize() is still called per quiz (FEAT-RCLAQS-R002), so verifyNoInteractions is wrong.
        NlpTokenizer nlpTokenizer = mock(NlpTokenizer.class);
        QuizSentenceConverter quizSentenceConverter = mock(QuizSentenceConverter.class);

        SentencePartEntity part1 = new SentencePartEntity(SentencePartKind.TEXT, "The cat sat.", null);
        FormEntity form1 = new FormEntity(null, 0, null, null, List.of(part1));
        QuizTemplateEntity qt1 = new QuizTemplateEntity();
        qt1.setId("q-dbsent-r002b-1");
        qt1.setForm(form1);
        qt1.setSentences(List.of("The cat sat."));

        SentencePartEntity part2 = new SentencePartEntity(SentencePartKind.TEXT, "The dog ran.", null);
        FormEntity form2 = new FormEntity(null, 0, null, null, List.of(part2));
        QuizTemplateEntity qt2 = new QuizTemplateEntity();
        qt2.setId("q-dbsent-r002b-2");
        qt2.setForm(form2);
        qt2.setSentences(List.of("The dog ran."));

        KnowledgeEntity ke = new KnowledgeEntity();
        ke.setId("k-dbsent-r002b");
        ke.setLabel("Simple Past");
        ke.setQuizTemplates(List.of(qt1, qt2));
        TopicEntity te = new TopicEntity();
        te.setId("t-dbsent-r002b");
        te.setLabel("Verbs");
        te.setKnowledges(List.of(ke));
        MilestoneEntity me = new MilestoneEntity();
        me.setId("m-dbsent-r002b");
        me.setLabel("A1");
        me.setTopics(List.of(te));
        RootNodeEntity root = new RootNodeEntity();
        root.setMilestones(List.of(me));
        CourseEntity course = new CourseEntity();
        course.setRoot(root);

        // serialize() must still be stubbed — it is invoked per quiz for the DSL (FEAT-RCLAQS-R002)
        when(quizSentenceConverter.serialize(form1)).thenReturn("The cat sat.");
        when(quizSentenceConverter.serialize(form2)).thenReturn("The dog ran.");
        when(nlpTokenizer.analyzeTokensBatch(anyList())).thenReturn(Map.of());

        CourseToAuditableMapper mapper = new CourseToAuditableMapper(nlpTokenizer, quizSentenceConverter);
        mapper.map(course);

        // R002: toPlainSentences must never be invoked for any quiz of the audited course
        verify(quizSentenceConverter, never()).toPlainSentences(any());
    }

    @Test
    @DisplayName("should stamp the canonical sentence at index 0 of QuizTemplateEntity sentences as the NLP batch key for the AuditableQuiz tokens")
    @Tag("FEAT-DBSENT")
    @Tag("F-DBSENT-R002")
    public void shouldStampTheCanonicalSentenceAtIndex0OfQuizTemplateEntitySentencesAsTheNLPBatchKeyForTheAuditableQuizTokens() {
        // R002: the NLP tokenization batch is keyed on sentences.get(0) from the entity,
        // not on any string derived from sentenceParts at runtime.
        NlpTokenizer nlpTokenizer = mock(NlpTokenizer.class);
        QuizSentenceConverter quizSentenceConverter = mock(QuizSentenceConverter.class);

        String canonicalSentence = "The dogs are barking loudly.";
        List<String> persistedSentences = List.of(canonicalSentence, "The dogs bark loudly.");

        NlpToken dogToken = new NlpToken("dogs", "dog", "NOUN", 300, false, false);
        NlpToken barkToken = new NlpToken("barking", "bark", "VERB", 850, false, false);

        SentencePartEntity part = new SentencePartEntity(SentencePartKind.TEXT, "The dogs / bark / loudly.", null);
        FormEntity form = new FormEntity(null, 0, null, null, List.of(part));

        QuizTemplateEntity qt = new QuizTemplateEntity();
        qt.setId("q-dbsent-r002c");
        qt.setForm(form);
        qt.setSentences(persistedSentences);

        KnowledgeEntity ke = new KnowledgeEntity();
        ke.setId("k-dbsent-r002c");
        ke.setLabel("Present Continuous");
        ke.setQuizTemplates(List.of(qt));
        TopicEntity te = new TopicEntity();
        te.setId("t-dbsent-r002c");
        te.setLabel("Verbs");
        te.setKnowledges(List.of(ke));
        MilestoneEntity me = new MilestoneEntity();
        me.setId("m-dbsent-r002c");
        me.setLabel("B1");
        me.setTopics(List.of(te));
        RootNodeEntity root = new RootNodeEntity();
        root.setMilestones(List.of(me));
        CourseEntity course = new CourseEntity();
        course.setRoot(root);

        // NLP batch is called with the canonical sentence (index 0 of entity sentences).
        when(nlpTokenizer.analyzeTokensBatch(List.of(canonicalSentence)))
                .thenReturn(Map.of(canonicalSentence, List.of(dogToken, barkToken)));
        // serialize() is still invoked for the DSL (FEAT-RCLAQS-R002)
        when(quizSentenceConverter.serialize(form)).thenReturn("The dogs / bark / loudly.");

        CourseToAuditableMapper mapper = new CourseToAuditableMapper(nlpTokenizer, quizSentenceConverter);
        AuditableCourse result = mapper.map(course);

        AuditableQuiz auditableQuiz = result.getMilestones().get(0)
                .getTopics().get(0)
                .getKnowledge().get(0)
                .getQuizzes().get(0);

        // R002: tokens must come from the batch keyed on sentences.get(0)
        assertEquals(List.of(dogToken, barkToken), auditableQuiz.getTokens(),
                "R002: tokens must be those returned for sentences.get(0) as the NLP batch key");

        // R002: analyzeTokensBatch must have been invoked with sentences.get(0) as the key, not a derived string
        verify(nlpTokenizer).analyzeTokensBatch(List.of(canonicalSentence));
    }

    @Test
    @DisplayName("should carry every sentence part and every accepted option of the quiz template into the auditable quiz")
    @Tag("FEAT-QINST")
    @Tag("F-QINST-R009")
    public void shouldCarryEverySentencePartAndEveryAcceptedOptionOfTheQuizTemplateIntoTheAuditableQuiz() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Test
    @DisplayName("should carry the knowledge instructions and the topic name into the auditable knowledge")
    @Tag("FEAT-QINST")
    @Tag("F-QINST-R009")
    public void shouldCarryTheKnowledgeInstructionsAndTheTopicNameIntoTheAuditableKnowledge() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
