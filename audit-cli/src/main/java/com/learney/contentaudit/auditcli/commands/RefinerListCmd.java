package com.learney.contentaudit.auditcli.commands;

import com.learney.contentaudit.auditcli.RefinerListCommand;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.refinerdomain.RefinementPlan;
import com.learney.contentaudit.refinerdomain.RefinementPlanStore;
import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.refinerdomain.RefinementTaskStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "list",
    description = "List all tasks in a refinement plan.%n%nShows every task with its status,"
        + " priority, and target.%nIf no plan-id is provided, uses the latest plan.",
    mixinStandardHelpOptions = true
)
final class RefinerListCmd implements RefinerListCommand, Callable<Integer> {

    private final RefinementPlanStore refinementPlanStore;

    @Parameters(index = "0", arity = "0..1",
            description = "Plan ID. If omitted, uses the latest plan.")
    private String planId;

    @Option(names = {"-f", "--format"},
            description = "Output format: text, json, table (default: ${DEFAULT-VALUE})",
            defaultValue = "text")
    private String formatName;

    public RefinerListCmd(RefinementPlanStore refinementPlanStore) {
        this.refinementPlanStore = refinementPlanStore;
    }

    @Override
    public Integer call() {
        return listTasks(this.planId);
    }

    @Override
    public int listTasks(String planId) {
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
        List<RefinementTask> tasks = plan.getTasks() != null ? plan.getTasks() : List.of();

        return switch (formatName) {
            case "json" -> printJson(plan, tasks);
            case "table" -> printTable(plan, tasks);
            default -> printText(plan, tasks);
        };
    }

    private Integer printText(RefinementPlan plan, List<RefinementTask> tasks) {
        long pending = tasks.stream().filter(t -> t.getStatus() == RefinementTaskStatus.PENDING).count();
        long completed = tasks.stream().filter(t -> t.getStatus() == RefinementTaskStatus.COMPLETED).count();
        long skipped = tasks.stream().filter(t -> t.getStatus() == RefinementTaskStatus.SKIPPED).count();

        System.out.println("Plan: " + plan.getId() + " (from audit: " + sourceAuditLabel(plan) + ")");
        System.out.println("Tasks: " + tasks.size() + " ("
                + pending + " pending, " + completed + " completed, " + skipped + " skipped)");

        if (tasks.isEmpty()) {
            System.out.println("\nNo tasks in this plan.");
            return 0;
        }

        System.out.println();
        System.out.println(String.format("%-3s  %-10s  %-11s  %-23s  %s", "#", "Status", "Target", "Node", "Diagnosis"));
        System.out.println("───  ──────────  ───────────  ───────────────────────  ──────────────────────");

        for (RefinementTask t : tasks) {
            System.out.printf("%-3d  %-10s  %-11s  %-23s  %s%n",
                    t.getPriority(),
                    t.getStatus() != null ? t.getStatus().name() : "UNKNOWN",
                    targetLabel(t.getNodeTarget()),
                    truncate(t.getNodeLabel(), 23),
                    t.getDiagnosisKind());
        }
        return 0;
    }

    private Integer printTable(RefinementPlan plan, List<RefinementTask> tasks) {
        long pending = tasks.stream().filter(t -> t.getStatus() == RefinementTaskStatus.PENDING).count();
        long completed = tasks.stream().filter(t -> t.getStatus() == RefinementTaskStatus.COMPLETED).count();
        long skipped = tasks.stream().filter(t -> t.getStatus() == RefinementTaskStatus.SKIPPED).count();

        System.out.println("Plan: " + plan.getId() + "  |  Audit: " + sourceAuditLabel(plan)
                + "  |  " + tasks.size() + " tasks (" + pending + "P/" + completed + "C/" + skipped + "S)");
        if (tasks.isEmpty()) return 0;

        System.out.println();
        System.out.println("┌───┬──────────┬───────────┬─────────────────────────┬────────────────────────────────┐");
        System.out.println("│ # │ Status   │ Target    │ Node                    │ Diagnosis                      │");
        System.out.println("├───┼──────────┼───────────┼─────────────────────────┼────────────────────────────────┤");

        for (RefinementTask t : tasks) {
            System.out.printf("│ %-1d │ %-8s │ %-9s │ %-23s │ %-30s │%n",
                    t.getPriority(),
                    t.getStatus() != null ? t.getStatus().name() : "UNKNOWN",
                    targetLabel(t.getNodeTarget()),
                    truncate(t.getNodeLabel(), 23),
                    t.getDiagnosisKind());
        }
        System.out.println("└───┴──────────┴───────────┴─────────────────────────┴────────────────────────────────┘");
        return 0;
    }

    private Integer printJson(RefinementPlan plan, List<RefinementTask> tasks) {
        try {
            long pending = tasks.stream().filter(t -> t.getStatus() == RefinementTaskStatus.PENDING).count();
            long completed = tasks.stream().filter(t -> t.getStatus() == RefinementTaskStatus.COMPLETED).count();
            long skipped = tasks.stream().filter(t -> t.getStatus() == RefinementTaskStatus.SKIPPED).count();

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("planId", plan.getId());
            output.put("sourceAuditId", plan.getSourceAuditId());
            output.put("createdAt", plan.getCreatedAt() != null ? plan.getCreatedAt().toString() : null);
            output.put("summary", Map.of("total", tasks.size(), "pending", pending, "completed", completed, "skipped", skipped));
            output.put("tasks", tasks.stream().map(t -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", t.getId());
                m.put("priority", t.getPriority());
                m.put("status", t.getStatus() != null ? t.getStatus().name() : null);
                m.put("target", targetLabel(t.getNodeTarget()));
                m.put("nodeId", t.getNodeId());
                m.put("nodeLabel", t.getNodeLabel());
                m.put("diagnosis", t.getDiagnosisKind() != null ? t.getDiagnosisKind().name() : null);
                return m;
            }).collect(Collectors.toList()));

            ObjectMapper om = new ObjectMapper();
            om.registerModule(new JavaTimeModule());
            om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            om.enable(SerializationFeature.INDENT_OUTPUT);
            System.out.println(om.writeValueAsString(output));
            return 0;
        } catch (Exception e) {
            System.err.println("Error formatting JSON: " + e.getMessage());
            return 1;
        }
    }

    private static String sourceAuditLabel(RefinementPlan plan) {
        return plan.getSourceAuditId() != null && !plan.getSourceAuditId().isBlank()
                ? plan.getSourceAuditId() : "(latest)";
    }

    private static String targetLabel(AuditTarget target) {
        return target != null ? target.name() : "UNKNOWN";
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 1) + "\u2026";
    }
}
