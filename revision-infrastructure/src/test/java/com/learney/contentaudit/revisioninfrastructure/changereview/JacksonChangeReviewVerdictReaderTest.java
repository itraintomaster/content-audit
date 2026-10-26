package com.learney.contentaudit.revisioninfrastructure.changereview;

import static org.junit.jupiter.api.Assertions.*;

import com.learney.contentaudit.revisiondomain.changereview.ChangeReview;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** FEAT-MARCA: what the reviewer answers, read strictly. */
class JacksonChangeReviewVerdictReaderTest {
    private final JacksonChangeReviewVerdictReader reader = new JacksonChangeReviewVerdictReader();

    @Test
    @DisplayName("should read a good mark without problems, and a doubtful or worse one with each problem where it is")
    @Tag("FEAT-MARCA")
    @Tag("F-MARCA-R002")
    void shouldReadAGoodMarkWithoutProblemsAndADoubtfulOneWithItsProblems() {
        var good = reader.read("{\"quizId\":\"q\",\"mark\":\"GOOD\",\"problems\":[],\"reason\":\"Arregla la traducción.\"}");
        var doubtful = reader.read("{\"quizId\":\"q\",\"mark\":\"DOUBTFUL\",\"problems\":[{\"where\":\"MINI_THEORY\","
                + "\"fragment\":\"En este ejercicio\",\"what\":\"La regla pasó a ser una orden.\"}],\"reason\":\"Mejor, con un defecto.\"}");

        assertEquals(new ChangeReview.Verdict("q", ChangeReview.Mark.GOOD, List.of(), "Arregla la traducción."), good);
        assertEquals(ChangeReview.Mark.DOUBTFUL, doubtful.mark());
        assertEquals(List.of(new ChangeReview.Problem(ChangeReview.Where.MINI_THEORY, "En este ejercicio",
                "La regla pasó a ser una orden.")), doubtful.problems());
    }

    @Test
    @DisplayName("should not read a good mark with problems, nor a doubtful or worse one without any")
    @Tag("FEAT-MARCA")
    @Tag("F-MARCA-R002")
    void shouldNotReadAGoodMarkWithProblemsNorADoubtfulOneWithout() {
        assertThrows(IllegalArgumentException.class, () -> reader.read("{\"quizId\":\"q\",\"mark\":\"GOOD\",\"problems\":"
                + "[{\"where\":\"EXERCISE\",\"fragment\":\"I am\",\"what\":\"x\"}],\"reason\":\"r\"}"));
        assertThrows(IllegalArgumentException.class, () -> reader.read(
                "{\"quizId\":\"q\",\"mark\":\"WORSE\",\"problems\":[],\"reason\":\"r\"}"));
    }

    @Test
    @DisplayName("should not read an answer with an unknown field, mark or place, a missing field or a repeated key")
    @Tag("FEAT-MARCA")
    @Tag("F-MARCA-R004")
    void shouldNotReadAnAnswerWithAnUnknownFieldMarkOrPlace() {
        for (String payload : List.of(
                "{\"quizId\":\"q\",\"mark\":\"GOOD\",\"problems\":[],\"reason\":\"r\",\"extra\":1}",
                "{\"quizId\":\"q\",\"mark\":\"FINE\",\"problems\":[],\"reason\":\"r\"}",
                "{\"quizId\":\"q\",\"mark\":\"DOUBTFUL\",\"problems\":[{\"where\":\"AUDIO\",\"fragment\":\"a\",\"what\":\"b\"}],\"reason\":\"r\"}",
                "{\"quizId\":\"q\",\"mark\":\"GOOD\",\"problems\":[]}",
                "{\"quizId\":\"q\",\"quizId\":\"p\",\"mark\":\"GOOD\",\"problems\":[],\"reason\":\"r\"}",
                "{\"quizId\":\"q\",\"mark\":\"GOOD\",\"problems\":[],\"reason\":\"r\"} tail"))
            assertThrows(IllegalArgumentException.class, () -> reader.read(payload), payload);
    }
}
