---
description: >
  Flow-based journey DSL reference. Use when writing journeys with decisions,
  bifurcations, error paths, or alternative outcomes. Provides the complete
  YAML syntax for FlowNode, Outcome, gate, and path enumeration.
---

<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->

# Flow-Based Journey DSL Reference

Flow-based journeys are the **preferred format** for user journeys. They model the journey as a directed acyclic graph (DAG) where each node is a business step and transitions represent decisions.

**Use `flow` by default.** Only fall back to linear `steps` for trivially simple journeys with zero decisions (e.g., viewing a page).

## YAML Syntax

```yaml
journeys:
  - id: JOURNEY-BOOK
    name: Book a flight
    flow:
      # Linear step — always goes to the next node
      - id: select_flight
        action: "Passenger selects a flight"
        then: check_availability

      # Decision — path depends on a business condition
      - id: check_availability
        action: "System checks flight availability"
        outcomes:
          - when: "Seats available"
            then: create_booking
          - when: "No seats, within overbooking limit"
            then: create_booking
          - when: "No seats, over overbooking limit"
            then: reject_no_availability

      # Gate — rules validated at this step (informational)
      - id: create_booking
        action: "System creates the booking"
        gate: [RULE-ID-FORMAT]
        then: process_payment

      - id: process_payment
        action: "System charges payment"
        gate: [RULE-PAYMENT-ONCE]
        outcomes:
          - when: "Payment approved"
            then: send_confirmation
          - when: "Payment declined"
            then: booking_failed

      # Terminal nodes — end of the journey
      - id: send_confirmation
        action: "Confirmation email sent to passenger"
        result: success

      - id: reject_no_availability
        action: "System informs passenger: no flights available"
        result: failure

      - id: booking_failed
        action: "Booking cancelled due to payment failure"
        result: failure
```

## Node Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | **Required.** Unique identifier within the journey (snake_case). |
| `action` | String | **Required.** Business-level description of the step. |
| `then` | String | Next node ID. **Linear** — no decision. |
| `outcomes` | List | **Decision** — list of `{when, then}` transitions. |
| `result` | String | **Terminal** — `"success"` or `"failure"`. |
| `gate` | List | Optional. Rule IDs validated at this step. |

**Each node has exactly ONE of:** `then`, `outcomes`, or `result`.

## Node Types

### Linear node (`then`)
Always progresses to a single next node. No decision involved.
```yaml
- id: create_order
  action: "System creates the order"
  then: process_payment
```

### Decision node (`outcomes`)
The path depends on a business condition. Each `when` describes a scenario.
```yaml
- id: check_stock
  action: "System checks inventory"
  outcomes:
    - when: "Item in stock"
      then: reserve_item
    - when: "Out of stock"
      then: notify_backorder
```

### Terminal node (`result`)
End of the journey. Value is `"success"` or `"failure"`.
```yaml
- id: order_confirmed
  action: "Confirmation email sent"
  result: success
```

### Gate (rule validation)
Optional annotation linking rule IDs to a step. Does NOT create bifurcations — it is informational, for traceability and visualization. The rule's own unit tests cover the validation logic.
```yaml
- id: validate_input
  action: "System validates booking ID"
  gate: [RULE-ID-FORMAT, RULE-DATE-RANGE]
  then: next_step
```

## Path Enumeration

The framework automatically enumerates all possible paths from the first node to each terminal node. Each path is identified as `path-1`, `path-2`, etc.

For the booking example above, 5 paths are generated:

| Path | Route | Result |
|------|-------|--------|
| path-1 | select → check[available] → book → payment[approved] → confirm | success |
| path-2 | select → check[available] → book → payment[declined] → failed | failure |
| path-3 | select → check[overbooking] → book → payment[approved] → confirm | success |
| path-4 | select → check[overbooking] → book → payment[declined] → failed | failure |
| path-5 | select → check[over limit] → reject | failure |

**Not all paths need a dedicated test.** The team selects which paths to test based on risk. Rules have their own unit tests, and testing one path transitively validates shared nodes.

## Validation Rules

The framework validates flow graphs automatically:

- **V-MBT-02**: All `then` references must point to existing nodes
- **V-MBT-03**: All nodes must be reachable from the first node
- **V-MBT-04**: At least one terminal node (with `result`)
- **V-MBT-05**: Each node has exactly one exit type
- **V-MBT-06**: Node IDs are unique within the journey
- **V-MBT-07**: Gate rule IDs must exist in the feature's rules
- **V-MBT-08**: No cycles (only DAGs)
- **V-MBT-09**: Each outcome must have `when` and a valid `then`

## Writing Guidelines

1. **Always use business language.** Write `action` and `when` as the PO would say it. "Seats available" not "AvailabilityService returns 200".
2. **Think about what can go wrong.** Every decision point, validation, external dependency, or timeout is a potential bifurcation.
3. **Use `gate` to link rules.** If a step validates something, reference the rule ID. This enables traceability and visualization.
4. **Keep graphs focused.** If a journey has more than ~10 nodes, consider splitting.
5. **The first node is the entry point.** No explicit `start` field needed.

## Test Generation from Journeys

Every flow-based journey automatically generates a **test class** with one test method per enumerated path. This has implications for how you write journeys:

1. **Every step must be testable.** If a step says "User goes to the store physically", it cannot be automated. Write steps that the system can execute or verify.
2. **Decisions create paths.** Each combination of decision outcomes produces a separate test method. 3 decisions with 2 outcomes each = 8 paths. Keep decision count reasonable.
3. **Gate rules are asserted.** When a step has `gate: [RULE-ID]`, the test is expected to verify that rule at that point in the flow.
4. **Terminal results define success/failure.** Each path's test method asserts the outcome declared by the terminal node.

Coverage is **path-level**: a journey is only fully covered when all its paths are tested.

**Validation rule:** Once a journey has a `flow` graph, implementation tests cannot reference it via `traceability.journey`. Only the auto-generated journey test class counts as coverage. This means converting a linear journey to a flow journey is a breaking change for any existing tests that reference it.
