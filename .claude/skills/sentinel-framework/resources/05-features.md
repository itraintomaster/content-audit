# Features & Business Rules

Features define the business requirements that the system must satisfy. Each feature contains
business rules (constraints) and user journeys (workflows). Tests can link back to features
via **traceability**, enabling requirements-to-test coverage tracking.

## Feature Hierarchy

```
Feature (id, name, description, code)
├── BusinessRule (id, name, severity, description, errorMessage)
└── UserJourney (id, name, steps[])
```

## Traceability

Tests can reference features, rules, and journeys via the `traceability` block:

```yaml
tests:
  - name: "Email must be valid"
    traceability:
      feature: "FEAT-001"
      rule: "RULE-001"
      journey: "JOURNEY-001"
```

This generates `@Tag("FEAT-001")` and `@Tag("RULE-001")` annotations on the test method,
enabling JUnit 5 tag-based filtering (e.g., run only tests for `FEAT-001`).

## Severity Levels

| Severity | Meaning |
|----------|---------|
| `critical` | System cannot function without this rule |
| `high` | Major functionality affected |
| `medium` | Important but not blocking |
| `low` | Nice-to-have validation |

## Declared Features

No features declared in the current definition.

Features live in requirement folders and are referenced from `sentinel.yaml`:

```yaml
definitions:
  - requirements/2026-02-19.01_my-feature/REQUIREMENT.md
```

See resource `11-requirements.md` for the full requirement folder convention.
