---
description: >
  Journey test reference. Use when implementing or reviewing journey test stubs —
  auto-generated test classes where each method covers a specific path through a
  flow-based journey. Different from implementation-level handwritten tests.
---

<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->

# Journey Test Reference

## What are Journey Tests?

Journey tests are **integration test stubs** auto-generated from flow-based journeys defined in REQUIREMENT.md. Unlike handwritten tests (declared in `sentinel.yaml` under an implementation), journey tests are:

- **Auto-generated** from the journey's flow graph — not proposed by a QA agent
- **One class per journey** (e.g., `FCartJ001JourneyTest.java`)
- **One method per path** through the journey DAG (e.g., `path1_quantityValid_success()`)
- **Cross-cutting** — they test a complete end-to-end flow, not a single implementation
- **Located in** `<testModule>/src/test/java/<testPackage>/` — placed in the exact Java package declared by the QA agent via `testPackage`, so it has package-private visibility to the classes it needs to instantiate

## Generated Class Structure

```java
@Generated(value = "com.sentinel.SentinelEngine")
@Tag("F-CART")         // feature code
@Tag("F-CART-J001")    // journey code
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FCartJ001JourneyTest {

    @Test @Order(1)
    @Tag("path-1")
    @DisplayName("path-1: User selects item → ... → success")
    public void path1_quantityValid_success() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Test @Order(2)
    @Tag("path-2")
    @DisplayName("path-2: User selects item → ... → failure")
    public void path2_quantityInvalid_failure() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
```

## How to Read the Tags

Each method has three levels of traceability via `@Tag`:

| Tag | Level | Example | Purpose |
|-----|-------|---------|---------|
| Feature code | Class | `@Tag("F-CART")` | Which feature this journey belongs to |
| Journey code | Class | `@Tag("F-CART-J001")` | Which journey |
| Path ID | Method | `@Tag("path-1")` | Which specific path through the DAG |

To find the journey definition: open REQUIREMENT.md and search for the journey code. The flow graph shows all nodes and decisions. The `@DisplayName` shows which nodes and decisions this specific path traverses.

## Implementing a Journey Test Method

Journey tests are fundamentally different from unit tests:

| Aspect | Implementation Test | Journey Test |
|--------|--------------------|--------------|
| Scope | Single method on one adapter | End-to-end flow across services |
| Mocking | Mock injected dependencies | Mock external boundaries only |
| Steps | Single arrange/act/assert | Multi-step: one per journey node |
| Data | Isolated fixture | State flows between steps |
| Declared in | `sentinel.yaml` handwrittenTests | Auto-generated from REQUIREMENT.md |

### Step-by-step implementation

1. **Read the journey flow** in REQUIREMENT.md — find the journey by its code
2. **Identify your path** — the `@DisplayName` lists the nodes and decisions. Follow only those decisions (e.g., `[Quantity valid]` means this path takes the "Quantity valid" outcome at that decision node)
3. **Set up the context** — create the initial state needed for the first node
4. **Execute each step in sequence** — each node's `action` translates to a call or assertion. State from one step feeds the next
5. **Assert at gates** — if a node has `gate: [RULE-ID]`, verify that the rule's constraint holds at that point in the flow
6. **Assert the final result** — `success` or `failure` as declared by the terminal node

### Example implementation

Given a journey: `select_item → validate_qty[Quantity valid] → add_to_cart → success`

```java
@Test @Order(1) @Tag("path-1")
@DisplayName("path-1: ...")
public void path1_quantityValid_success() {
    // Step 1: select_item — User selects an item
    var item = new Item("SKU-001", "Widget", 9.99);
    var cart = new Cart(userId);

    // Step 2: validate_qty — System validates quantity (gate: RULE-QTY)
    int quantity = 3; // valid quantity per RULE-QTY
    var result = cartService.addItem(cart.id(), item.sku(), quantity);

    // Step 3: add_to_cart — Item added to cart (result: success)
    assertThat(result.status()).isEqualTo("success");
    assertThat(result.cart().items()).hasSize(1);
    assertThat(result.cart().items().getFirst().quantity()).isEqualTo(3);
}
```

## Coverage Tracking

Journey coverage is **path-level**, not binary:

- Each journey has N paths (enumerated from the DAG)
- Each path is tracked individually: `PASSING`, `FAILING`, `DECLARED`, `NO_TESTS`
- A journey is `PASSING` only when **all** its paths pass
- A journey with some paths passing and some not is `PARTIAL`

The sentinel report shows:
```
Journey F-CART-J001: 2/3 paths passing
  path-1: PASSING  ✓
  path-2: PASSING  ✓
  path-3: DECLARED    (stub not yet implemented)
```

## Smart Merge

Running `sentinel generate` again:
- **Adds** new path methods if the journey flow changed and new paths appeared
- **Never overwrites** existing methods (whether stubs or implemented)
- **Never removes** methods from the file

## Key Differences from Handwritten Tests

1. Journey test classes are NOT declared in `sentinel.yaml` — they come from REQUIREMENT.md flows
2. The QA agent declares **where** journey tests go via `testModule` + `testPackage` on the journey, but does NOT propose the test methods — those are auto-generated from the flow graph
3. Journey test method names are derived from path IDs and conditions, not from test names
4. Each method covers one specific **path**, not one specific **rule**
5. A journey may reference rules via `gate` — but rule coverage comes from implementation tests

## VALIDATION RULE: Flow Journey Traceability

**Implementation tests CANNOT reference flow-based journeys** via `traceability.journey`.

If a journey has a `flow` graph (i.e., it is not a simple linear `steps` list), the ONLY valid way to cover it is through the auto-generated journey test class. Adding `traceability: { journey: JOURNEY-ID }` on a handwrittenTest or declarativeTest under an implementation will produce a **validation error**.

This rule exists because:
- Flow journeys have multiple paths — a single impl test cannot cover the DAG
- Path-level coverage requires the generated `@Tag("path-N")` structure
- Mixing impl-level and journey-level traceability creates false coverage signals

**Linear journeys** (only `steps`, no `flow`) are exempt — they can still be referenced from impl tests via `traceability.journey`.

### What to do if you have old tests referencing a flow journey

1. Remove `journey: JOURNEY-ID` from the test's `traceability` in `sentinel.yaml`
2. Keep the `rule:` reference if the test validates a specific business rule
3. Implement the journey path test stubs in the generated `*JourneyTest.java` class
