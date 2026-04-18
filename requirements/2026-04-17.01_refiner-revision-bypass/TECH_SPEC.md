---
patch: ARCH-REV-DEDUPE
requirement: 2026-04-17.01_refiner-revision-bypass
generated: 2026-04-17T00:00:00Z
---

# Tech Spec: Revision Engine Dedupe

## Remove duplicate root-level implementations from revision-domain
The previous revision-bypass patch moved six engine-internal classes (`IdentityReviser`, `AutoApproveValidator`, `DefaultRevisionValidatorResult`, `DispatchingReviser`, `DefaultCourseElementLocator`, `DefaultRevisionEngine`) into the `engine` package for encapsulation, but the original root-level copies were never removed. As a result `sentinel generate` reports six `conflicts with root-level component of the same name` errors — each implementation is declared twice in the same module. The corrective action is to drop only the root-level copies; the package-scoped versions inside `engine` remain the single source of truth, preserving the intended encapsulation where only `DefaultRevisionEngineFactory` is the public entry point.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    implementations:
      - name: "IdentityReviser"
        _change: "delete"
      - name: "AutoApproveValidator"
        _change: "delete"
      - name: "DefaultRevisionValidatorResult"
        _change: "delete"
      - name: "DispatchingReviser"
        _change: "delete"
      - name: "DefaultCourseElementLocator"
        _change: "delete"
      - name: "DefaultRevisionEngine"
        _change: "delete"
```
