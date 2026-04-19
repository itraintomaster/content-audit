---
patch: FEAT-CLIRV
requirement: 2026-04-19.01_cli-resource-verb-restructure
generated: 2026-04-19T02:00:00Z
---

# Tech Spec: CLI restructure to kubectl-style verb-resource grammar

This spec describes how the `audit-cli` module is restructured to support the kubectl-style `<verb> <resource>` surface defined by FEAT-CLIRV. All changes are confined to `audit-cli`: domain modules, application, and infrastructure adapters are untouched (R019, R020). The patch is one-shot — the old `Refiner*` and `Analyzer*` command interfaces and their picocli implementations are removed without shims, and the new flat verb-shaped contracts are introduced alongside a workdir resolver consumed by `Main`.

## Extend `GetTasksFilter` with `target` and `diagnosisKind` to restore the dropped `refiner next` filters

A migration gap surfaced after the kubectl-style restructure landed: the old `refiner next` command exposed `--target <enum>` (`-t`) and `--diagnosis <enum>` (`-d`) filters that narrow the candidate task set by `nodeTarget` and `diagnosisKind`. The first cut of `GetTasksFilter` carried only `planId`, `status`, `sortByPriority`, and `limit`, so the analyst's update to R008 (which restored `--target` and `--diagnosis` as flags on `get tasks`) had no carrier to reach `GetCmd.get()`. Two `Optional<>` fields plug the gap directly. The fields are typed as `Optional<AuditTarget>` and `Optional<DiagnosisKind>` rather than `Optional<String>` because both enums are already cross-module-importable from `audit-cli` (the module already depends on `audit-domain` for `AuditTarget` and on `refiner-domain` for `DiagnosisKind`) — so no new dependency edges are needed and the case-insensitive parsing required by R008 happens at the picocli boundary, not inside the dispatcher. The `GetCommand.get(resource, name, GetTasksFilter)` signature is unchanged: adding fields to the carrier doesn't touch the contract.

```architecture
modules:
  - name: audit-cli
    _change: modify
    models:
      - name: GetTasksFilter
        _change: modify
        fields:
          - { name: target, type: "Optional<AuditTarget>", _change: add, description: "Restrict to tasks whose nodeTarget matches this AuditTarget enum value; absent = no filter (R008)" }
          - { name: diagnosisKind, type: "Optional<DiagnosisKind>", _change: add, description: "Restrict to tasks whose diagnosisKind matches this DiagnosisKind enum value; absent = no filter (R008)" }
```
