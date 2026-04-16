package com.learney.contentaudit.auditcli.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.learney.contentaudit.auditcli.RefinerNextCommand;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditReportStore;
import com.learney.contentaudit.refinerdomain.CorrectionContextResolver;
import com.learney.contentaudit.refinerdomain.RefinerEngine;
import com.learney.contentaudit.refinerdomain.RefinementPlan;
import com.learney.contentaudit.refinerdomain.RefinementPlanStore;
import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.refinerdomain.RefinementTaskStatus;
import com.learney.contentaudit.refinerdomain.DiagnosisKind;
import com.learney.contentaudit.refinerdomain.LemmaAbsenceCorrectionContext;
import com.learney.contentaudit.refinerdomain.MisplacedLemmaContext;
import com.learney.contentaudit.refinerdomain.SentenceLengthCorrectionContext;
import com.learney.contentaudit.refinerdomain.SuggestedLemma;
import com.learney.contentaudit.auditdomain.AuditTarget;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "next",
    description = "Show the next pending refinement task.%n%nDisplays the highest-priority task"
        + " that hasn't been completed yet.%nIf no plan-id is provided, uses the latest plan.",
    mixinStandardHelpOptions = true
)
final class RefinerNextCmd implements RefinerNextCommand, Callable<Integer> {

    private final RefinementPlanStore refinementPlanStore;
    private final RefinerEngine refinerEngine;
    private final AuditReportStore auditReportStore;
    private final CorrectionContextResolver correctionContextResolver;

    @Parameters(index = "0", arity = "0..1",
            description = "Plan ID. If omitted, uses the latest plan.")
    private String planId;

    @Option(names = {"-f", "--format"},
            description = "Output format: text, json, table (default: ${DEFAULT-VALUE})",
            defaultValue = "text")
    private String formatName;

    @Option(names = {"-t", "--target"},
            description = "Filter by target level: QUIZ, KNOWLEDGE, MILESTONE, COURSE")
    private String targetFilter;

    @Option(names = {"-d", "--diagnosis"},
            description = "Filter by diagnosis: SENTENCE_LENGTH, LEMMA_ABSENCE, COCA_BUCKETS, etc.")
    private String diagnosisFilter;

    public RefinerNextCmd(RefinementPlanStore refinementPlanStore, RefinerEngine refinerEngine,
            AuditReportStore auditReportStore, CorrectionContextResolver correctionContextResolver) {
        this.refinementPlanStore = refinementPlanStore;
        this.refinerEngine = refinerEngine;
        this.auditReportStore = auditReportStore;
        this.correctionContextResolver = correctionContextResolver;
    }

    @Override
    public Integer call() {
        return next(this.planId);
    }

    @Override
    public int next(String planId) {
        Optional<RefinementPlan> planOpt;
        if (planId == null || planId.isBlank()) {
            planOpt = refinementPlanStore.loadLatest();
            if (planOpt.isEmpty()) {
                System.err.println("Error: no plans found. Run 'content-audit refiner plan' first.");
                return 1;
            }
        } else {
            planOpt = refinementPlanStore.load(planId);
            if (planOpt.isEmpty()) {
                System.err.println("Error: plan not found: " + planId);
                return 1;
            }
        }

        RefinementPlan plan = planOpt.get();
        int total = plan.getTasks() != null ? plan.getTasks().size() : 0;

        // Parse filters
        AuditTarget targetEnum = null;
        if (targetFilter != null) {
            try {
                targetEnum = AuditTarget.valueOf(targetFilter.toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("Error: invalid target '" + targetFilter
                        + "'. Valid values: QUIZ, KNOWLEDGE, TOPIC, MILESTONE, COURSE");
                return 1;
            }
        }
        DiagnosisKind diagnosisEnum = null;
        if (diagnosisFilter != null) {
            try {
                diagnosisEnum = DiagnosisKind.valueOf(diagnosisFilter.toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("Error: invalid diagnosis '" + diagnosisFilter
                        + "'. Valid values: SENTENCE_LENGTH, LEMMA_ABSENCE, COCA_BUCKETS, "
                        + "LEMMA_RECURRENCE, KNOWLEDGE_TITLE_LENGTH, KNOWLEDGE_INSTRUCTIONS_LENGTH");
                return 1;
            }
        }

        // Find next pending task matching filters
        final AuditTarget ft = targetEnum;
        final DiagnosisKind fd = diagnosisEnum;
        Optional<RefinementTask> nextTask = plan.getTasks() == null
                ? Optional.empty()
                : plan.getTasks().stream()
                    .filter(t -> t.getStatus() == RefinementTaskStatus.PENDING)
                    .filter(t -> ft == null || t.getNodeTarget() == ft)
                    .filter(t -> fd == null || t.getDiagnosisKind() == fd)
                    .findFirst();

        if (nextTask.isEmpty()) {
            String filterDesc = buildFilterDesc(ft, fd);
            if ("json".equals(formatName)) {
                System.out.println("{\"completed\": true, \"message\": \"No pending tasks"
                        + filterDesc + "\"}");
            } else {
                System.out.println("No pending tasks" + filterDesc + ".");
            }
            return 0;
        }

        RefinementTask task = nextTask.get();

        // Resolve correction context for supported diagnosis kinds (R006, R007)
        SentenceLengthCorrectionContext slContext = null;
        LemmaAbsenceCorrectionContext laContext = null;
        String correctionContextError = null;
        boolean needsContext = task.getDiagnosisKind() == DiagnosisKind.SENTENCE_LENGTH
                || task.getDiagnosisKind() == DiagnosisKind.LEMMA_ABSENCE;
        if (needsContext) {
            String sourceAuditId = plan.getSourceAuditId();
            if (sourceAuditId == null || sourceAuditId.isBlank()) {
                correctionContextError = "no sourceAuditId on plan";
            } else {
                Optional<AuditReport> reportOpt = auditReportStore.load(sourceAuditId);
                if (reportOpt.isEmpty()) {
                    correctionContextError = "audit report '" + sourceAuditId + "' not found";
                } else {
                    Optional<?> contextOpt =
                            correctionContextResolver.resolve(reportOpt.get(), task);
                    if (contextOpt.isPresent() && contextOpt.get() instanceof SentenceLengthCorrectionContext) {
                        slContext = (SentenceLengthCorrectionContext) contextOpt.get();
                    } else if (contextOpt.isPresent() && contextOpt.get() instanceof LemmaAbsenceCorrectionContext) {
                        laContext = (LemmaAbsenceCorrectionContext) contextOpt.get();
                    } else if (contextOpt.isEmpty()) {
                        correctionContextError = "context could not be resolved for task " + task.getId();
                    } else {
                        correctionContextError = "unexpected context type for task " + task.getId();
                    }
                }
            }
        }

        return switch (formatName) {
            case "json" -> printJson(task, total, slContext, laContext, correctionContextError);
            case "table" -> printTable(task, total, slContext, laContext, correctionContextError);
            default -> printText(task, total, slContext, laContext, correctionContextError);
        };
    }

    private Integer printText(RefinementTask task, int total,
            SentenceLengthCorrectionContext slContext, LemmaAbsenceCorrectionContext laContext,
            String correctionContextError) {
        System.out.println("Next task (#" + task.getPriority() + " of " + total + "):");
        System.out.println("  Target:    " + (task.getNodeTarget() != null ? task.getNodeTarget().name() : "UNKNOWN"));
        System.out.println("  Node:      " + task.getNodeLabel() + " (id: " + task.getNodeId() + ")");
        System.out.println("  Diagnosis: " + task.getDiagnosisKind());
        System.out.println("  Priority:  " + task.getPriority());
        System.out.println("  Status:    " + task.getStatus());
        if (task.getDiagnosisKind() == DiagnosisKind.SENTENCE_LENGTH) {
            System.out.println();
            if (slContext != null) {
                printSentenceLengthContextText(slContext);
            } else {
                System.out.println("  Correction context: not available ("
                        + (correctionContextError != null ? correctionContextError : "unknown reason") + ")");
            }
        } else if (task.getDiagnosisKind() == DiagnosisKind.LEMMA_ABSENCE) {
            System.out.println();
            if (laContext != null) {
                printLemmaAbsenceContextText(laContext);
            } else {
                System.out.println("  Correction context: not available ("
                        + (correctionContextError != null ? correctionContextError : "unknown reason") + ")");
            }
        }
        return 0;
    }

    private void printSentenceLengthContextText(SentenceLengthCorrectionContext ctx) {
        System.out.println("  Correction context:");
        System.out.println("    Sentence:     " + nullToEmpty(ctx.getSentence()));
        System.out.println("    Translation:  " + nullToEmpty(ctx.getTranslation()));
        System.out.println("    Knowledge:    " + nullToEmpty(ctx.getKnowledgeTitle()));
        System.out.println("    Instructions: " + nullToEmpty(ctx.getKnowledgeInstructions()));
        System.out.println("    Topic:        " + nullToEmpty(ctx.getTopicLabel()));
        System.out.println("    CEFR Level:   " + (ctx.getCefrLevel() != null ? ctx.getCefrLevel().name() : ""));
        String deltaStr = ctx.getDelta() > 0 ? "+" + ctx.getDelta()
                : (ctx.getDelta() < 0 ? String.valueOf(ctx.getDelta()) : "0");
        System.out.println("    Tokens:       " + ctx.getTokenCount()
                + " (target: " + ctx.getTargetMin() + "-" + ctx.getTargetMax()
                + ", delta: " + deltaStr + ")");
        System.out.println("    Suggested lemmas:");
        List<SuggestedLemma> lemmas = ctx.getSuggestedLemmas();
        if (lemmas == null || lemmas.isEmpty()) {
            System.out.println("      (none available)");
        } else {
            for (int i = 0; i < lemmas.size(); i++) {
                SuggestedLemma l = lemmas.get(i);
                String cocaPart = l.getCocaRank() > 0 ? " [COCA #" + l.getCocaRank() + "]" : "";
                System.out.println("      " + (i + 1) + ". " + nullToEmpty(l.getLemma())
                        + " (" + nullToEmpty(l.getPos()) + ") - " + nullToEmpty(l.getReason())
                        + cocaPart);
            }
        }
    }

    private void printLemmaAbsenceContextText(LemmaAbsenceCorrectionContext ctx) {
        System.out.println("  Correction context:");
        System.out.println("    Sentence:     " + nullToEmpty(ctx.getSentence()));
        System.out.println("    Translation:  " + nullToEmpty(ctx.getTranslation()));
        System.out.println("    Knowledge:    " + nullToEmpty(ctx.getKnowledgeTitle()));
        System.out.println("    Instructions: " + nullToEmpty(ctx.getKnowledgeInstructions()));
        System.out.println("    Topic:        " + nullToEmpty(ctx.getTopicLabel()));
        System.out.println("    CEFR Level:   " + (ctx.getCefrLevel() != null ? ctx.getCefrLevel().name() : ""));
        System.out.println("    Misplaced lemmas:");
        List<MisplacedLemmaContext> misplaced = ctx.getMisplacedLemmas();
        if (misplaced == null || misplaced.isEmpty()) {
            System.out.println("      (none available)");
        } else {
            for (int i = 0; i < misplaced.size(); i++) {
                MisplacedLemmaContext m = misplaced.get(i);
                String cocaPart = m.getCocaRank() > 0 ? " [COCA #" + m.getCocaRank() + "]" : "";
                System.out.println("      " + (i + 1) + ". " + nullToEmpty(m.getLemma())
                        + " (" + nullToEmpty(m.getPos()) + ") - expected "
                        + (m.getExpectedLevel() != null ? m.getExpectedLevel().name() : "")
                        + ", found in "
                        + (m.getQuizLevel() != null ? m.getQuizLevel().name() : "")
                        + cocaPart);
            }
        }
        System.out.println("    Suggested replacements:");
        List<SuggestedLemma> lemmas = ctx.getSuggestedLemmas();
        if (lemmas == null || lemmas.isEmpty()) {
            System.out.println("      (none available)");
        } else {
            for (int i = 0; i < lemmas.size(); i++) {
                SuggestedLemma l = lemmas.get(i);
                String cocaPart = l.getCocaRank() > 0 ? " [COCA #" + l.getCocaRank() + "]" : "";
                System.out.println("      " + (i + 1) + ". " + nullToEmpty(l.getLemma())
                        + " (" + nullToEmpty(l.getPos()) + ") - " + nullToEmpty(l.getReason())
                        + cocaPart);
            }
        }
    }

    private Integer printTable(RefinementTask task, int total,
            SentenceLengthCorrectionContext slContext, LemmaAbsenceCorrectionContext laContext,
            String correctionContextError) {
        System.out.println("Next task (#" + task.getPriority() + " of " + total + ")");
        System.out.println();
        System.out.println("┌──────────────┬──────────────────────────────────────────┐");
        System.out.printf("│ Target       │ %-40s │%n", task.getNodeTarget() != null ? task.getNodeTarget().name() : "UNKNOWN");
        System.out.printf("│ Node         │ %-40s │%n", task.getNodeLabel());
        System.out.printf("│ Node ID      │ %-40s │%n", task.getNodeId());
        System.out.printf("│ Diagnosis    │ %-40s │%n", task.getDiagnosisKind());
        System.out.printf("│ Priority     │ %-40d │%n", task.getPriority());
        System.out.printf("│ Status       │ %-40s │%n", task.getStatus());
        System.out.println("└──────────────┴──────────────────────────────────────────┘");
        if (task.getDiagnosisKind() == DiagnosisKind.SENTENCE_LENGTH) {
            System.out.println();
            if (slContext != null) {
                printSentenceLengthContextText(slContext);
            } else {
                System.out.println("  Correction context: not available ("
                        + (correctionContextError != null ? correctionContextError : "unknown reason") + ")");
            }
        } else if (task.getDiagnosisKind() == DiagnosisKind.LEMMA_ABSENCE) {
            System.out.println();
            if (laContext != null) {
                printLemmaAbsenceContextText(laContext);
            } else {
                System.out.println("  Correction context: not available ("
                        + (correctionContextError != null ? correctionContextError : "unknown reason") + ")");
            }
        }
        return 0;
    }

    private static String buildFilterDesc(AuditTarget target, DiagnosisKind diagnosis) {
        if (target == null && diagnosis == null) return "";
        StringBuilder sb = new StringBuilder(" matching");
        if (target != null) sb.append(" target=").append(target.name());
        if (diagnosis != null) {
            if (target != null) sb.append(",");
            sb.append(" diagnosis=").append(diagnosis.name());
        }
        return sb.toString();
    }

    private Integer printJson(RefinementTask task, int total,
            SentenceLengthCorrectionContext slContext, LemmaAbsenceCorrectionContext laContext,
            String correctionContextError) {
        try {
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("taskNumber", task.getPriority());
            output.put("totalTasks", total);
            output.put("id", task.getId());
            output.put("target", task.getNodeTarget() != null ? task.getNodeTarget().name() : null);
            output.put("nodeId", task.getNodeId());
            output.put("nodeLabel", task.getNodeLabel());
            output.put("diagnosis", task.getDiagnosisKind() != null ? task.getDiagnosisKind().name() : null);
            output.put("priority", task.getPriority());
            output.put("status", task.getStatus() != null ? task.getStatus().name() : null);

            if (task.getDiagnosisKind() == DiagnosisKind.SENTENCE_LENGTH) {
                if (slContext != null) {
                    output.put("correctionContext", buildSentenceLengthContextMap(slContext));
                } else {
                    output.put("correctionContext", null);
                    output.put("correctionContextError",
                            correctionContextError != null ? correctionContextError : "unknown reason");
                }
            } else if (task.getDiagnosisKind() == DiagnosisKind.LEMMA_ABSENCE) {
                if (laContext != null) {
                    output.put("correctionContext", buildLemmaAbsenceContextMap(laContext));
                } else {
                    output.put("correctionContext", null);
                    output.put("correctionContextError",
                            correctionContextError != null ? correctionContextError : "unknown reason");
                }
            }

            ObjectMapper om = new ObjectMapper();
            om.enable(SerializationFeature.INDENT_OUTPUT);
            System.out.println(om.writeValueAsString(output));
            return 0;
        } catch (Exception e) {
            System.err.println("Error formatting JSON: " + e.getMessage());
            return 1;
        }
    }

    private Map<String, Object> buildSentenceLengthContextMap(SentenceLengthCorrectionContext ctx) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("sentence", ctx.getSentence());
        map.put("translation", ctx.getTranslation());
        map.put("knowledgeTitle", ctx.getKnowledgeTitle());
        map.put("knowledgeInstructions", ctx.getKnowledgeInstructions());
        map.put("topicLabel", ctx.getTopicLabel());
        map.put("cefrLevel", ctx.getCefrLevel() != null ? ctx.getCefrLevel().name() : null);
        map.put("tokenCount", ctx.getTokenCount());
        Map<String, Object> targetRange = new LinkedHashMap<>();
        targetRange.put("min", ctx.getTargetMin());
        targetRange.put("max", ctx.getTargetMax());
        map.put("targetRange", targetRange);
        map.put("delta", ctx.getDelta());
        List<SuggestedLemma> lemmas = ctx.getSuggestedLemmas();
        if (lemmas == null) {
            map.put("suggestedLemmas", List.of());
        } else {
            List<Map<String, Object>> lemmaList = lemmas.stream()
                    .map(l -> {
                        Map<String, Object> lm = new LinkedHashMap<>();
                        lm.put("lemma", l.getLemma());
                        lm.put("pos", l.getPos());
                        lm.put("reason", l.getReason());
                        lm.put("cocaRank", l.getCocaRank());
                        return lm;
                    })
                    .toList();
            map.put("suggestedLemmas", lemmaList);
        }
        return map;
    }

    private Map<String, Object> buildLemmaAbsenceContextMap(LemmaAbsenceCorrectionContext ctx) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("sentence", ctx.getSentence());
        map.put("translation", ctx.getTranslation());
        map.put("knowledgeTitle", ctx.getKnowledgeTitle());
        map.put("knowledgeInstructions", ctx.getKnowledgeInstructions());
        map.put("topicLabel", ctx.getTopicLabel());
        map.put("cefrLevel", ctx.getCefrLevel() != null ? ctx.getCefrLevel().name() : null);
        List<MisplacedLemmaContext> misplaced = ctx.getMisplacedLemmas();
        if (misplaced == null) {
            map.put("misplacedLemmas", List.of());
        } else {
            List<Map<String, Object>> misplacedList = misplaced.stream()
                    .map(m -> {
                        Map<String, Object> mm = new LinkedHashMap<>();
                        mm.put("lemma", m.getLemma());
                        mm.put("pos", m.getPos());
                        mm.put("expectedLevel", m.getExpectedLevel() != null ? m.getExpectedLevel().name() : null);
                        mm.put("quizLevel", m.getQuizLevel() != null ? m.getQuizLevel().name() : null);
                        mm.put("cocaRank", m.getCocaRank());
                        return mm;
                    })
                    .toList();
            map.put("misplacedLemmas", misplacedList);
        }
        List<SuggestedLemma> lemmas = ctx.getSuggestedLemmas();
        if (lemmas == null) {
            map.put("suggestedLemmas", List.of());
        } else {
            List<Map<String, Object>> lemmaList = lemmas.stream()
                    .map(l -> {
                        Map<String, Object> lm = new LinkedHashMap<>();
                        lm.put("lemma", l.getLemma());
                        lm.put("pos", l.getPos());
                        lm.put("reason", l.getReason());
                        lm.put("cocaRank", l.getCocaRank());
                        return lm;
                    })
                    .toList();
            map.put("suggestedLemmas", lemmaList);
        }
        return map;
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
