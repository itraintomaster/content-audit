---
name: sentinel-feature
description: |
  Sentinel feature/requirement authoring. Use when creating or editing REQUIREMENT.md files with features, business rules, user journeys, and doubts.
---

# Sentinel Feature Authoring

You are helping author functional requirements for a Sentinel-governed project. Requirements are stored as **REQUIREMENT.md** files inside feature folders under `requirements/`.

## File Structure

```
requirements/
└── 2026-03-09.01_shopping-cart/
    ├── REQUIREMENT.md              # Source of truth (markdown + declarations)
    ├── requirement.yaml            # Auto-generated for backward compatibility
    ├── architectural_patch.yaml    # Architecture patch (if any)
    ├── references/                 # Supporting documents
    │   ├── api-spec.md
    │   └── wireframes.png
    └── attachments/                # Imported files
```

Feature folders are referenced in `sentinel.yaml`:

```yaml
definitions:
  - "requirements/2026-03-09.01_shopping-cart"
```

## REQUIREMENT.md Format

```markdown
---
feature:
  id: FEAT-CART
  code: F-CART
  name: Shopping Cart Management
  priority: critical
---

# Shopping Cart Management

Free-form prose: the system needs a shopping cart that allows
adding, removing, and modifying items before checkout.

See [API Specification](references/api-spec.md) for technical details.

## Business Rules

### Rule[F-CART-R001] - Items must have positive quantity
**Severity**: high | **Validation**: VALIDATED

When a user adds an item, the quantity must be >= 1.

**Error**: "Quantity must be at least 1"

### Rule[F-CART-R002] - Cart total reflects item prices
**Severity**: medium | **Validation**: AUTO_VALIDATED

The cart total is always the sum of (price x quantity) for all items.

## User Journeys

### Journey[F-CART-J001] - Add item to cart
**Validation**: VALIDATED

1. User browses product catalog
2. User clicks "Add to Cart"
3. System validates quantity
4. System updates cart total
5. User sees updated cart badge

## Open Questions

### Doubt[DOUBT-MAX-QTY] - Maximum quantity per item?
- [ ] Yes, limit to 99
- [ ] No maximum
- [x] Configurable per product

**Answer**: Configurable per product, default 99.

## Design Notes

Additional free-form prose, diagrams, decision tables, anything needed...
```

## Syntax Reference

### Frontmatter (YAML between `---`)

Required fields:
- `feature.id` — Unique identifier (e.g., `FEAT-CART`)
- `feature.name` — Human-readable name
- `feature.code` — Short code (e.g., `F-CART`)
- `feature.priority` — `critical` | `high` | `medium` | `low`

### Business Rules

```markdown
### Rule[ID] - Name
**Severity**: high | **Validation**: VALIDATED

Description text...

**Error**: "Error message shown to users"
```

- **Severity**: `BLOCKER` | `CRITICAL` | `MAJOR` | `MINOR` (or `high`/`medium`/`low`)
- **Validation**: `VALIDATED` (human-confirmed) | `AUTO_VALIDATED` (default) | `ASSUMPTION` (needs confirmation)
- **Error**: Optional error message in quotes

### User Journeys (Linear)

```markdown
### Journey[ID] - Name
**Validation**: VALIDATED

1. First step
2. Second step
3. Third step
```

Steps are numbered lists. Each step is a single line. Use this format for simple flows with no decisions.

### Flow-Based Journeys (Decision Graphs)

For journeys with **multiple possible outcomes** (bifurcations, error paths, alternative flows), use the `flow` format in `requirement.yaml` instead of linear `steps`. A flow is a directed acyclic graph (DAG) where:
- Each **node** is a business step (`action`)
- **Transitions** represent decisions or linear progression
- **Terminal nodes** mark the end of a path (`success` or `failure`)

**When to use `flow` vs `steps`:**
- Use `steps` for simple linear flows (no decisions)
- Use `flow` when the journey has bifurcations ("if X then Y, else Z")
- A journey MUST have either `steps` or `flow`, never both

**YAML syntax (in requirement.yaml):**

```yaml
journeys:
  - id: JOURNEY-BOOK
    name: Book a flight
    flow:
      - id: select_flight
        action: "Passenger selects a flight"
        then: check_availability

      - id: check_availability
        action: "System checks flight availability"
        outcomes:
          - when: "Seats available"
            then: create_booking
          - when: "No seats, over overbooking limit"
            then: reject_no_availability

      - id: create_booking
        action: "System creates the booking"
        gate: [RULE-ID-FORMAT]
        then: process_payment

      - id: process_payment
        action: "System charges payment"
        outcomes:
          - when: "Payment approved"
            then: send_confirmation
          - when: "Payment declined"
            then: booking_failed

      - id: send_confirmation
        action: "Confirmation email sent"
        result: success

      - id: reject_no_availability
        action: "No flights available"
        result: failure

      - id: booking_failed
        action: "Booking cancelled"
        result: failure
```

**Node fields:**

| Field | Description |
|-------|-------------|
| `id` | Unique identifier within the journey |
| `action` | Business-level description of the step |
| `then` | Next node ID (linear, no decision) |
| `outcomes` | List of `{when, then}` (decision with conditions) |
| `result` | Terminal: `"success"` or `"failure"` |
| `gate` | Optional: rule IDs validated at this step |

Each node has **exactly one** of: `then`, `outcomes`, or `result`.

**Path enumeration:** The framework enumerates all possible paths from the first node to terminal nodes. The booking example above produces 5 paths. The team selects which to test — coverage is selective, not exhaustive.

**Best practices for flow journeys:**
- Write `action` and `when` in **business language** (not technical jargon)
- Use `gate` to link rules that are validated at a step (informational, for traceability)
- Keep the graph focused — if it has more than ~10 nodes, consider splitting into sub-features
- The first node in the `flow` list is the entry point (no explicit `start` field needed)

### Doubts (Open Questions)

```markdown
### Doubt[ID] - Question text?
- [ ] Option A
- [ ] Option B
- [x] Selected option

**Answer**: Explicit answer text.
```

Options use checkbox syntax. `[x]` marks the selected option. `**Answer**` provides an explicit answer (takes precedence over checkbox selection).

### Free-Form Prose

Any text outside Rule/Journey/Doubt blocks is rendered as standard markdown. Use it for context, design notes, diagrams, tables, links to references, etc.

## Naming Conventions

- Feature IDs: `FEAT-<DOMAIN>` (e.g., `FEAT-CART`, `FEAT-BOOK-INT`)
- Rule IDs: `<FEAT-CODE>-R<NNN>` (e.g., `F-CART-R001`)
- Journey IDs: `<FEAT-CODE>-J<NNN>` (e.g., `F-CART-J001`)
- Doubt IDs: `DOUBT-<TOPIC>` (e.g., `DOUBT-MAX-QTY`)

## Traceability

Tests link back to rules via `@Tag` annotations in generated JUnit tests:

```java
@Tag("F-CART-R001")
@Test
void shouldRejectZeroQuantity() { ... }
```

The enforcement chain:
```
REQUIREMENT.md → Parser → RequirementsDefinition record
  → TestCoverageValidator (maps rules → tests via @Tag)
  → JunitGenerator (generates @Tag per rule)
  → ReportGenerator (coverage: NO_TESTS / DECLARED / FAILING / PASSING)
```

## Backward Compatibility

When Sentinel parses a `REQUIREMENT.md`, it auto-generates `requirement.yaml` in the same folder. Downstream tools (TestCoverageValidator, JunitGenerator) consume the YAML record. If only `requirement.yaml` exists (no REQUIREMENT.md), the old format is still supported.

## VS Code Experience

- Opening a `REQUIREMENT.md` file launches a **rich preview** with interactive blocks for rules, journeys, and doubts
- **Syntax Reference** button in the toolbar shows the format at a glance
- **Validation bar** shows real-time errors (missing frontmatter, no rules, duplicate IDs) and warnings (unresolved doubts, missing severity)
- **Requirement Structure** tree view in the Feature Hub sidebar shows rules, journeys, doubts with status icons
- Toggle between preview and source editing modes

## Best Practices

1. One feature per REQUIREMENT.md (maps to an Epic/PRD)
2. Rules should be atomic and independently testable
3. Journeys should describe the user flow, referencing which rules apply at each step
4. Use doubts to capture decisions that need human input — resolve them with `**Answer**` or `[x]`
5. Link severity to business impact, not technical complexity
6. Use the `references/` folder for supporting documents (API specs, wireframes, etc.)
7. Write rich prose context — the markdown is a living functional specification, not just a data file
