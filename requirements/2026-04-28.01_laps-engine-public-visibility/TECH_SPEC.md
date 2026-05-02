---
patch: F-LEPV
requirement: 2026-04-28.01_laps-engine-public-visibility
generated: 2026-04-28T00:00:00Z
---

# Tech Spec: LAPS engine public visibility

## Promote DefaultLemmaAbsenceProposalStrategyRegistry to a public class
The `engine` package inside `revision-domain` is `visibility: internal`, so by default every implementation in it is generated as a package-private (`final class …`) Java type. The composition root in `audit-cli/Main.java` (manual DI, no Spring — see project memory `feedback_no_spring.md`) instantiates this registry directly to wire the LAPS strategy map at startup, which is the same role `DefaultRevisionEngineFactory` already plays as a publicly-visible seam inside the same internal package. Marking the registry `visibility: "public"` opens the single Java class consumers need without exporting the package; sibling collaborators stay package-private and the engine graph stays encapsulated (P2, P5). A separate factory interface is unnecessary because the registry is already itself a seam — its `LemmaAbsenceProposalStrategyRegistryConfig` carrier already collects all wiring inputs and there is exactly one consumer (the composition root, P4).

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    packages:
      - name: "engine"
        _change: "modify"
        implementations:
          - name: "DefaultLemmaAbsenceProposalStrategyRegistry"
            _change: "modify"
            visibility: "public"
```

## Promote DefaultLemmaAbsenceProposalDeriver to a public class
Same constraint as the registry: the deriver is constructed at the composition root in `audit-cli/Main.java` so the manual DI graph can hand it the `QuizSentenceConverter` injection it needs. Without a public Java class, `audit-cli` cannot reference the type across the module boundary and compilation breaks (`is not public in ...engine; cannot be accessed from outside package`). The deriver is the deterministic counterpart of the strategy — a pure transformation from `(elementBefore, candidate)` to `elementAfter` — so it is already its own seam and does not need a separate factory interface to gate construction. Promoting only the deriver class to public keeps the rest of the `engine` package package-private, preserving the internal package's encapsulation (P5, P6).

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    packages:
      - name: "engine"
        _change: "modify"
        implementations:
          - name: "DefaultLemmaAbsenceProposalDeriver"
            _change: "modify"
            visibility: "public"
```
