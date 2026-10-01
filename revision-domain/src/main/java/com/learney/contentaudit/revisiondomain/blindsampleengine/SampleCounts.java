package com.learney.contentaudit.revisiondomain.blindsampleengine;

import com.learney.contentaudit.revisiondomain.blindsample.BlindPair;
import com.learney.contentaudit.revisiondomain.blindsample.BlindSample;
import com.learney.contentaudit.revisiondomain.blindsample.BlindSide;
import com.learney.contentaudit.revisiondomain.blindsample.PairAnswer;
import com.learney.contentaudit.revisiondomain.blindsample.PairAnswerRecord;
import com.learney.contentaudit.revisiondomain.blindsample.ProportionEstimate;
import com.learney.contentaudit.revisiondomain.comparison.BlindSampleGoal;
import com.learney.contentaudit.revisiondomain.comparison.SampleCriterion;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * The one count of a blind sample, shared by the service that closes it and the reader the verdict of a batch
 * uses (F-CIEGA-R004, R005): the last answer of a reviewer to each pair, read against the side of the after --
 * the after chosen is better, the before chosen is worse, equal counts with better, both wrong apart --, and
 * whether the goal of the sample is met by the criterion it declares.
 */
final class SampleCounts {
    private SampleCounts() {
    }

    /** What one reviewer answered, over the pairs he answered. */
    record Tally(int answered, int better, int equal, int worse, int bothWrong) {
        int betterOrEqual() {
            return better + equal;
        }
    }

    /** The last answer of a reviewer to each pair, by pair. */
    static Map<Integer, PairAnswer> answersOf(BlindSample sample, String reviewer) {
        Map<Integer, PairAnswer> answers = new TreeMap<>();
        if (sample.getAnswers() == null || reviewer == null) {
            return answers;
        }
        for (PairAnswerRecord record : sample.getAnswers()) {
            if (record != null && record.getAnswer() != null && reviewer.equals(record.getReviewer())) {
                answers.put(record.getPairIndex(), record.getAnswer());
            }
        }
        return answers;
    }

    static Tally tally(BlindSample sample, String reviewer) {
        Map<Integer, PairAnswer> answers = answersOf(sample, reviewer);
        int better = 0;
        int equal = 0;
        int worse = 0;
        int bothWrong = 0;
        for (Map.Entry<Integer, PairAnswer> answer : answers.entrySet()) {
            BlindPair pair = pair(sample, answer.getKey());
            if (pair == null) {
                continue;
            }
            switch (answer.getValue()) {
                case EQUAL -> equal++;
                case BOTH_WRONG -> bothWrong++;
                case A_BETTER -> {
                    if (pair.getAfterSide() == BlindSide.A) {
                        better++;
                    } else {
                        worse++;
                    }
                }
                case B_BETTER -> {
                    if (pair.getAfterSide() == BlindSide.B) {
                        better++;
                    } else {
                        worse++;
                    }
                }
            }
        }
        return new Tally(better + equal + worse + bothWrong, better, equal, worse, bothWrong);
    }

    static BlindPair pair(BlindSample sample, int index) {
        if (sample.getPairs() == null) {
            return null;
        }
        for (BlindPair pair : sample.getPairs()) {
            if (pair != null && pair.getIndex() == index) {
                return pair;
            }
        }
        return null;
    }

    /**
     * The parts of the goal that are not met, in words; none when it is met. Counted: the share of better or
     * equal and of worse against the goal's; on the bound: the lower bound of better or equal and the one-sided
     * upper bound of worse at 95 %.
     */
    static List<String> unmetParts(Tally tally, BlindSampleGoal goal, SampleStatistics statistics) {
        List<String> unmet = new ArrayList<>();
        if (goal == null) {
            return unmet;
        }
        String responsible = goal.getResponsible() != null ? goal.getResponsible() : "el responsable";
        if (tally.answered() == 0) {
            unmet.add("ningún par respondido por " + responsible);
            return unmet;
        }
        double minShare = goal.getSize() > 0 ? (double) goal.getMinBetterOrEqual() / goal.getSize() : 0.0;
        double maxWorseShare = goal.getSize() > 0 ? (double) goal.getMaxWorse() / goal.getSize() : 0.0;
        ProportionEstimate betterOrEqual = statistics.proportion(tally.betterOrEqual(), tally.answered());
        ProportionEstimate worse = statistics.proportion(tally.worse(), tally.answered());
        if (goal.getCriterion() == SampleCriterion.BOUND) {
            if (betterOrEqual.getLower95() < minShare - 1e-12) {
                unmet.add("cota de mejor o igual: " + percent(betterOrEqual.getLower95()) + ", la meta es al menos "
                        + percent(minShare));
            }
            if (worse.getUpperOneSided95() > maxWorseShare + 1e-12) {
                unmet.add("cota de peores: " + percent(worse.getUpperOneSided95()) + ", la meta es a lo sumo "
                        + percent(maxWorseShare));
            }
        } else {
            if ((long) tally.betterOrEqual() * goal.getSize() < (long) goal.getMinBetterOrEqual() * tally.answered()) {
                unmet.add("mejor o igual: " + tally.betterOrEqual() + " de " + tally.answered()
                        + ", la meta es al menos " + goal.getMinBetterOrEqual() + " de " + goal.getSize());
            }
            if ((long) tally.worse() * goal.getSize() > (long) goal.getMaxWorse() * tally.answered()) {
                unmet.add("peores: " + tally.worse() + " de " + tally.answered() + ", la meta es a lo sumo "
                        + goal.getMaxWorse() + " de " + goal.getSize());
            }
        }
        return unmet;
    }

    /** «95 mejor o igual, 1 peor y 4 con error en los dos, de 100 respondidos». */
    static String summary(Tally tally) {
        return tally.betterOrEqual() + " mejor o igual, " + tally.worse() + " peor y " + tally.bothWrong()
                + " con error en los dos, de " + tally.answered() + " respondidos";
    }

    /** A share as a percentage with one decimal and a decimal comma: 3,0 %. */
    static String percent(double share) {
        DecimalFormat format = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("es-AR")));
        return format.format(share * 100.0) + " %";
    }
}
