package com.learney.contentaudit.auditcli.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.learney.contentaudit.auditcli.RefinerNextCommand;
import com.learney.contentaudit.refinerdomain.RefinerEngine;
import com.learney.contentaudit.refinerdomain.RefinementPlan;
import com.learney.contentaudit.refinerdomain.RefinementPlanStore;
import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.refinerdomain.RefinementTaskStatus;
import com.learney.contentaudit.refinerdomain.DiagnosisKind;
import com.learney.contentaudit.auditdomain.AuditTarget;
import java.util.LinkedHashMap;
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

    public RefinerNextCmd(RefinementPlanStore refinementPlanStore, RefinerEngine refinerEngine) {
        this.refinementPlanStore = refinementPlanStore;
        this.refinerEngine = refinerEngine;
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

        return switch (formatName) {
            case "json" -> printJson(task, total);
            case "table" -> printTable(task, total);
            default -> printText(task, total);
        };
    }

    private Integer printText(RefinementTask task, int total) {
        System.out.println("Next task (#" + task.getPriority() + " of " + total + "):");
        System.out.println("  Target:    " + (task.getNodeTarget() != null ? task.getNodeTarget().name() : "UNKNOWN"));
        System.out.println("  Node:      " + task.getNodeLabel() + " (id: " + task.getNodeId() + ")");
        System.out.println("  Diagnosis: " + task.getDiagnosisKind());
        System.out.println("  Priority:  " + task.getPriority());
        System.out.println("  Status:    " + task.getStatus());
        return 0;
    }

    private Integer printTable(RefinementTask task, int total) {
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

    private Integer printJson(RefinementTask task, int total) {
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

            ObjectMapper om = new ObjectMapper();
            om.enable(SerializationFeature.INDENT_OUTPUT);
            System.out.println(om.writeValueAsString(output));
            return 0;
        } catch (Exception e) {
            System.err.println("Error formatting JSON: " + e.getMessage());
            return 1;
        }
    }
}
