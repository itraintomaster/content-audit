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
        when(quizSentenceConverter.toPlainSentences(any())).thenReturn(List.of("cat"));
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
        when(quizSentenceConverter.toPlainSentences(any())).thenReturn(List.of("cat"));
        CourseToAuditableMapper mapper = new CourseToAuditableMapper(nlpTokenizer, quizSentenceConverter);
        assertThrows(RuntimeException.class, () -> mapper.map(course));
    }

    @Test
    @DisplayName("should emit the canonical first sub-variant rather than the raw pipe literal in the stamped sentences")
    @Tag("FEAT-QSENT")
    @Tag("F-QSENT-R026")
    public void shouldEmitTheCanonicalFirstSubvariantRatherThanTheRawPipeLiteralInTheStampedSentences() {
        // Arrange
        NlpTokenizer nlpTokenizer = mock(NlpTokenizer.class);
        QuizSentenceConverter quizSentenceConverter = mock(QuizSentenceConverter.class);

        // CLOZE with pipe-separated variants: options = ["is|'s"]
        // The old buggy buildSentence would emit "He is|'s great." (raw pipe literal).
        // R026 + R018: the converter must resolve to the canonical sub-variant "is".
        // We stub toPlainSentences to return the corrected result.
        SentencePartEntity textPart = new SentencePartEntity(SentencePartKind.TEXT, "He", null);
        SentencePartEntity clozePart = new SentencePartEntity(SentencePartKind.CLOZE, null, List.of("is|'s"));
        SentencePartEntity tailPart = new SentencePartEntity(SentencePartKind.TEXT, " great.", null);
        FormEntity form = new FormEntity(null, 0, null, null, List.of(textPart, clozePart, tailPart));

        QuizTemplateEntity qt = new QuizTemplateEntity();
        qt.setId("q1");
        qt.setForm(form);
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

        // Converter returns the canonical "is" form — not the raw "is|'s" literal
        List<String> expectedSentences = List.of("He is great.", "He 's great.");
        when(quizSentenceConverter.toPlainSentences(form)).thenReturn(expectedSentences);
        when(nlpTokenizer.analyzeTokensBatch(anyList())).thenReturn(Map.of());

        // Act
        CourseToAuditableMapper mapper = new CourseToAuditableMapper(nlpTokenizer, quizSentenceConverter);
        AuditableCourse result = mapper.map(course);

        // Assert: sentences on the AuditableQuiz come from the converter (canonical first, no pipe literal)
        AuditableQuiz auditableQuiz = result.getMilestones().get(0)
                .getTopics().get(0)
                .getKnowledge().get(0)
                .getQuizzes().get(0);
        assertEquals(expectedSentences, auditableQuiz.getSentences());
    }

    @Test
    @DisplayName("should strip hints from the sentences stamped on AuditableQuiz")
    @Tag("FEAT-QSENT")
    @Tag("F-QSENT-R026")
    public void shouldStripHintsFromTheSentencesStampedOnAuditableQuiz() {
        // Arrange
        NlpTokenizer nlpTokenizer = mock(NlpTokenizer.class);
        QuizSentenceConverter quizSentenceConverter = mock(QuizSentenceConverter.class);

        // TEXT with pedagogical hint "(to be)", CLOZE with "is", trailing text.
        // The old buggy buildSentence would emit "He is (to be) great." — hint not removed.
        // R026 + R019: the converter must strip the hint, producing "He is great.".
        SentencePartEntity textPart = new SentencePartEntity(SentencePartKind.TEXT, "He", null);
        SentencePartEntity clozePart = new SentencePartEntity(SentencePartKind.CLOZE, null, List.of("is"));
        SentencePartEntity tailPart = new SentencePartEntity(SentencePartKind.TEXT, " (to be) great.", null);
        FormEntity form = new FormEntity(null, 0, null, null, List.of(textPart, clozePart, tailPart));

        QuizTemplateEntity qt = new QuizTemplateEntity();
        qt.setId("q2");
        qt.setForm(form);
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

        // Converter returns hint-free plain sentence
        List<String> expectedSentences = List.of("He is great.");
        when(quizSentenceConverter.toPlainSentences(form)).thenReturn(expectedSentences);
        when(nlpTokenizer.analyzeTokensBatch(anyList())).thenReturn(Map.of());

        // Act
        CourseToAuditableMapper mapper = new CourseToAuditableMapper(nlpTokenizer, quizSentenceConverter);
        AuditableCourse result = mapper.map(course);

        // Assert: sentences contain no hint — the "(to be)" is absent
        AuditableQuiz auditableQuiz = result.getMilestones().get(0)
                .getTopics().get(0)
                .getKnowledge().get(0)
                .getQuizzes().get(0);
        assertEquals(expectedSentences, auditableQuiz.getSentences());
        assertFalse(
                auditableQuiz.getSentences().stream().anyMatch(s -> s.contains("(to be)")),
                "Plain sentences must not contain pedagogical hints");
    }

    @Test
    @DisplayName("should invoke QuizSentenceConverter exactly once per quiz and stamp the list eagerly on AuditableQuiz")
    @Tag("FEAT-QSENT")
    @Tag("F-QSENT-R027")
    public void shouldInvokeQuizSentenceConverterExactlyOncePerQuizAndStampTheListEagerlyOnAuditableQuiz() {
        // Arrange
        NlpTokenizer nlpTokenizer = mock(NlpTokenizer.class);
        QuizSentenceConverter quizSentenceConverter = mock(QuizSentenceConverter.class);

        // Two quizzes in the same knowledge node
        SentencePartEntity part1 = new SentencePartEntity(SentencePartKind.TEXT, "cat", null);
        FormEntity form1 = new FormEntity(null, 0, null, null, List.of(part1));
        QuizTemplateEntity qt1 = new QuizTemplateEntity();
        qt1.setId("q1");
        qt1.setForm(form1);

        SentencePartEntity part2 = new SentencePartEntity(SentencePartKind.TEXT, "dog", null);
        FormEntity form2 = new FormEntity(null, 0, null, null, List.of(part2));
        QuizTemplateEntity qt2 = new QuizTemplateEntity();
        qt2.setId("q2");
        qt2.setForm(form2);

        KnowledgeEntity ke = new KnowledgeEntity();
        ke.setId("k1");
        ke.setLabel("label");
        ke.setQuizTemplates(List.of(qt1, qt2));
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

        List<String> sentencesForQ1 = List.of("The cat sat.");
        List<String> sentencesForQ2 = List.of("The dog ran.");
        String dslForQ1 = "The cat sat.";
        String dslForQ2 = "The dog ran.";
        when(quizSentenceConverter.serialize(form1)).thenReturn(dslForQ1);
        when(quizSentenceConverter.serialize(form2)).thenReturn(dslForQ2);
        when(quizSentenceConverter.toPlainSentences(form1)).thenReturn(sentencesForQ1);
        when(quizSentenceConverter.toPlainSentences(form2)).thenReturn(sentencesForQ2);
        when(nlpTokenizer.analyzeTokensBatch(anyList())).thenReturn(Map.of());

        // Act
        CourseToAuditableMapper mapper = new CourseToAuditableMapper(nlpTokenizer, quizSentenceConverter);
        AuditableCourse result = mapper.map(course);

        // Assert: serialize() and toPlainSentences() each invoked exactly once per quiz in a single
        // pass — FEAT-RCLAQS R002 extends R027: both DSL and plain sentences derive from the same
        // single invocation window, so no quiz is processed more than once by the converter.
        verify(quizSentenceConverter, times(1)).serialize(form1);
        verify(quizSentenceConverter, times(1)).serialize(form2);
        verify(quizSentenceConverter, times(1)).toPlainSentences(form1);
        verify(quizSentenceConverter, times(1)).toPlainSentences(form2);
        verifyNoMoreInteractions(quizSentenceConverter);

        // Assert: each AuditableQuiz carries the eagerly stamped list and DSL from its own converter call
        List<AuditableQuiz> quizzes = result.getMilestones().get(0)
                .getTopics().get(0)
                .getKnowledge().get(0)
                .getQuizzes();
        assertEquals(2, quizzes.size());
        assertEquals(sentencesForQ1, quizzes.get(0).getSentences());
        assertEquals(sentencesForQ2, quizzes.get(1).getSentences());
        assertEquals(dslForQ1, quizzes.get(0).getQuizSentence());
        assertEquals(dslForQ2, quizzes.get(1).getQuizSentence());
    }

    // -----------------------------------------------------------------------
    // FEAT-RCLAQS tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("should stamp quizSentence via QuizSentenceConverter serialize on AuditableQuiz")
    @Tag("FEAT-RCLAQS")
    @Tag("F-RCLAQS-R002")
    public void shouldStampQuizSentenceViaQuizSentenceConverterSerializeOnAuditableQuiz() {
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
        when(quizSentenceConverter.toPlainSentences(form)).thenReturn(List.of("He is great.", "He 's great."));
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
    @DisplayName("should invoke serialize once per quiz in same pass as toPlainSentences")
    @Tag("FEAT-RCLAQS")
    @Tag("F-RCLAQS-R002")
    public void shouldInvokeSerializeOncePerQuizInSamePassAsToPlainSentences() {
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

        SentencePartEntity part2 = new SentencePartEntity(SentencePartKind.TEXT, "The dog runs.", null);
        FormEntity form2 = new FormEntity(null, 0, null, null, List.of(part2));
        QuizTemplateEntity qt2 = new QuizTemplateEntity();
        qt2.setId("q-rclaqs-r002b-2");
        qt2.setForm(form2);

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
        when(quizSentenceConverter.toPlainSentences(form1)).thenReturn(List.of("The cat sits."));
        when(quizSentenceConverter.toPlainSentences(form2)).thenReturn(List.of("The dog runs."));
        when(nlpTokenizer.analyzeTokensBatch(anyList())).thenReturn(Map.of());

        CourseToAuditableMapper mapper = new CourseToAuditableMapper(nlpTokenizer, quizSentenceConverter);
        AuditableCourse result = mapper.map(course);

        // R002: serialize called exactly once per quiz — no extra calls
        verify(quizSentenceConverter, times(1)).serialize(form1);
        verify(quizSentenceConverter, times(1)).serialize(form2);

        // R027: toPlainSentences also called exactly once per quiz (existing invariant)
        verify(quizSentenceConverter, times(1)).toPlainSentences(form1);
        verify(quizSentenceConverter, times(1)).toPlainSentences(form2);

        // Both quizzes carry their respective quizSentence values
        List<AuditableQuiz> quizzes = result.getMilestones().get(0)
                .getTopics().get(0)
                .getKnowledge().get(0)
                .getQuizzes();
        assertEquals("The cat sits.", quizzes.get(0).getQuizSentence());
        assertEquals("The dog runs.", quizzes.get(1).getQuizSentence());
    }

    @Test
    @DisplayName("should stamp quizSentence and sentences from same FormEntity in same pass")
    @Tag("FEAT-RCLAQS")
    @Tag("F-RCLAQS-R003")
    public void shouldStampQuizSentenceAndSentencesFromSameFormEntityInSamePass() {
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
        when(quizSentenceConverter.toPlainSentences(form)).thenReturn(plains);
        when(nlpTokenizer.analyzeTokensBatch(anyList())).thenReturn(Map.of());

        CourseToAuditableMapper mapper = new CourseToAuditableMapper(nlpTokenizer, quizSentenceConverter);
        AuditableCourse result = mapper.map(course);

        // R003 structural: both methods invoked with same FormEntity — same pass, same origin
        verify(quizSentenceConverter, times(1)).serialize(form);
        verify(quizSentenceConverter, times(1)).toPlainSentences(form);

        // Both outputs are present on the same AuditableQuiz
        AuditableQuiz auditableQuiz = result.getMilestones().get(0)
                .getTopics().get(0)
                .getKnowledge().get(0)
                .getQuizzes().get(0);
        assertEquals(dsl, auditableQuiz.getQuizSentence());
        assertEquals(plains, auditableQuiz.getSentences());
    }

    @Test
    @DisplayName("should propagate QuizSentenceSerializationException when TEXT part has options")
    @Tag("FEAT-RCLAQS")
    @Tag("F-RCLAQS-R004")
    public void shouldPropagateQuizSentenceSerializationExceptionWhenTextPartHasOptions() {
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
    @DisplayName("should propagate QuizSentenceSerializationException when CLOZE part has no options")
    @Tag("FEAT-RCLAQS")
    @Tag("F-RCLAQS-R004")
    public void shouldPropagateQuizSentenceSerializationExceptionWhenClozePartHasNoOptions() {
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
    @DisplayName("should propagate QuizSentenceSerializationException preventing any AuditableCourse production")
    @Tag("FEAT-RCLAQS")
    @Tag("F-RCLAQS-R004")
    public void shouldPropagateQuizSentenceSerializationExceptionPreventingAnyAuditableCourseProduction() {
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
        when(quizSentenceConverter.toPlainSentences(validForm)).thenReturn(List.of("She runs."));
        QuizSentenceSerializationException ex =
                new QuizSentenceSerializationException("CLOZE must have at least one option", "0");
        when(quizSentenceConverter.serialize(invalidForm)).thenThrow(ex);

        CourseToAuditableMapper mapper = new CourseToAuditableMapper(nlpTokenizer, quizSentenceConverter);

        // R004: the exception propagates — no AuditableCourse is returned, not even partially
        assertThrows(QuizSentenceSerializationException.class, () -> mapper.map(course),
                "Atomic failure: exception from invalid FormEntity prevents AuditableCourse production");
    }
}
