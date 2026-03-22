# Requirements & Architecture Patch Convention

Sentinel uses a structured folder convention to track feature requirements and their
architectural proposals. Each feature requirement lives in its own dated folder.

## Folder Structure

```
requirements/
  2026-02-19.01_user-registration/
    requirement.yaml          # Feature requirements (rules, journeys)
    architectural_patch.yaml  # Architecture proposal (added after analysis)
  2026-02-19.02_payment-processing/
    requirement.yaml
    architectural_patch.yaml
```

### Folder Naming Convention

Pattern: `YYYY-MM-DD.NN_kebab-case-name`

- **Date prefix**: `YYYY-MM-DD` — date the requirement was created
- **Counter**: `.NN` — two-digit incremental counter per day (01, 02, …)
- **Name**: `_kebab-case-feature-name` — lowercase, hyphens only

Examples:
- `2026-02-19.01_user-registration`
- `2026-02-19.02_payment-processing`
- `2026-02-20.01_order-management`

### sentinel.yaml Registration

Each requirement folder is registered individually in `sentinel.yaml`:

```yaml
definitions:
  - requirements/2026-02-19.01_user-registration/requirement.yaml
  - requirements/2026-02-19.02_payment-processing/requirement.yaml
```

---

## requirement.yaml Schema

```yaml
features:
  - id: "FEAT-USER-REG"            # Must start with FEAT-
    name: "User Registration"
    description: |
      Detailed functional description of the feature.
    code: "F-UR"                     # Auto-generated feature code
    rules:
      - id: "RULE-EMAIL-VALID"      # Must start with RULE-
        name: "Email must be valid"
        severity: high               # critical | high | medium | low
        description: "Email must match RFC 5322 format"
        errorMessage: "Invalid email format"
    journeys:
      - id: "JOURNEY-REGISTER"      # Must start with JOURNEY-
        name: "New user registration"
        steps:
          - "User opens registration page"
          - "User fills in email and password"
          - "System validates input"
          - "System creates account"
```

### Validation Rules

A `requirement.yaml` is valid when:

| Rule | Requirement |
|------|-------------|
| V1 | Folder name matches `\d{4}-\d{2}-\d{2}\.\d{2}_[a-z0-9-]+` |
| V4 | At least one feature is defined |
| V5 | Every feature has non-empty `id`, `name`, `description` |
| V6 | Every feature has at least one rule (rules are acceptance criteria) |
| V7 | Every rule has non-empty `id`, `name`, `description` |
| V8 | Every journey has at least one step |
| V9 | All IDs are unique within the file |
| V10 | Feature IDs follow `FEAT-*` pattern |
| V11 | Rule IDs follow `RULE-*` pattern |
| V12 | Journey IDs follow `JOURNEY-*` pattern |

Validate with: `sentinel requirement validate --file requirements/YYYY-MM-DD.NN_name/`

---

## architectural_patch.yaml Schema

The architecture patch is created by the architect after analysing the requirement.
It is a **subset** of `sentinel.yaml` containing only the elements relevant to the new feature.

```yaml
code: "FEAT-USER-REG"               # Feature ID this patch targets
description: |
  Adds UserRegistrationService in application layer.

modules:
  # Include existing elements only when they are DEPENDENCIES of new elements
  - name: "domain"
    models:
      - name: "User"                 # EXISTING — included as dependency
        fields:
          - { name: "id", type: "UUID" }
          - { name: "email", type: "String" }

  - name: "application"
    dependsOn: ["domain"]
    interfaces:
      - name: "UserRepository"       # NEW — outbound port
        stereotype: "OutboundPort"
        sealed: true
        exposes:
          - signature: "save(User u): void"
    implementations:
      - name: "UserRegistrationService"  # NEW — use case
        implements: ["UserRepository"]
        requiresInject:
          - { name: "userRepository", type: "UserRepository" }
        tests:
          - name: "registers user with valid email"
            traceability:
              feature: "FEAT-USER-REG"
              rule: "RULE-EMAIL-VALID"
              journey: "JOURNEY-REGISTER"
            invoke:
              method: "register"
              args: ["user@example.com", "password123"]
            assert:
              verifyCall: "userRepository.save"
```

### Key Rules for Patches

- **Additive only**: include only elements relevant to the new feature
- **Existing elements**: include them only if they are direct dependencies of new elements
- **Unrelated elements**: do NOT include them in the patch
- **Every test** must have a `traceability` block linking to the feature
- **Every rule** in the requirement must have at least one test in the patch

### Architecture Diff

Before applying a patch, view what is new vs. already in sentinel.yaml:

```bash
sentinel architecture diff --patch requirements/2026-02-19.01_user-registration/architectural_patch.yaml
```

Output classifies each element as `EXISTING`, `NEW`, or `MODIFIED`.

### Applying a Patch

```bash
sentinel patch apply --patch requirements/2026-02-19.01_user-registration/architectural_patch.yaml
```

This merges the patch into `sentinel.yaml`, runs `sentinel generate`, and produces an initial sentinel report.

---

## Feature Workflow Summary

```
1. Create requirement folder + requirement.yaml
   sentinel requirement validate --file requirements/YYYY-MM-DD.NN_name/

2. Architect writes architectural_patch.yaml in the same folder
   sentinel requirement coverage --file requirements/YYYY-MM-DD.NN_name/
   sentinel architecture diff --patch .../architectural_patch.yaml

3. Apply the patch to sentinel.yaml
   sentinel patch apply --patch .../architectural_patch.yaml

4. AI agent implements the generated stubs
   sentinel agent --feature FEAT-XXX

5. Check the sentinel report
   sentinel report generate
   # → .sentinel/sentinel-report.yaml
```

## Feature States (from sentinel-report.yaml)

| State | Condition |
|-------|-----------|
| `DRAFT` | `requirement.yaml` exists, no `architectural_patch.yaml` |
| `ANALYSED` | Patch exists and all rules have test declarations |
| `IMPLEMENTING` | Patch applied to sentinel.yaml, some elements missing or tests failing |
| `IMPLEMENTED` | All elements exist, all tests passing |
| `ERRORED` | Unexpected error during analysis or implementation |
