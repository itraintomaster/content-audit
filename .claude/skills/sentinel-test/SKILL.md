---
name: sentinel-test
description: >
  Sentinel declarative testing reference. Use when writing or editing declarative test definitions in YAML,
  understanding the four-phase test model, or troubleshooting test generation. Use this skill whenever
  working with test-patch YAML proposals, reviewing qa-tester agent output, authoring sentinel tests,
  or debugging generated JUnit code that doesn't match the YAML definition.
---

<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->

# Sentinel Declarative Testing

You are helping author declarative test definitions for Sentinel. Tests are defined in YAML and compiled into JUnit 5 + Mockito code.

## Four-Phase Test Model

```yaml
tests:
  - name: "Should save task successfully"
    target: "PostgresTaskAdapter"       # Class under test (SUT)
    type: unit                          # unit (default) | integration

    # PHASE 1: FIXTURES (Object Instantiation)
    fixtures:
      - id: "taskInput"
        type: "com.app.domain.Task"
        construct: { id: "uuid-1", title: "Buy Milk", completed: false }

    # PHASE 2: MOCKS (Behavior Definition)
    mocks:
      - dependency: "postgresClient"    # Field name in the adapter
        method: "executeSql"
        args:
          - match: "eq"                 # eq | any | isA | capture
            value: "INSERT INTO..."
          - match: "any"
        thenReturn: ref:taskInput       # ref: prefix = fixture reference
        # OR: thenThrow: { type: "java.sql.SQLException", message: "timeout" }

    # PHASE 3: INVOKE (The Action)
    invoke:
      method: "save"
      args: [ref:taskInput]             # ref: prefix = fixture reference

    # PHASE 4: ASSERT (The Verdict)
    assert:
      returns: ref:taskInput            # ref: prefix = fixture reference
      # OR: assertThrows: "java.lang.RuntimeException"
      # OR: doesNotThrow: true
      verifyCall:                       # Side-effect verification
        dependency: "postgresClient"
        method: "executeSql"
        times: 1
        message: "SQL must be called once"
```

## Matchers

| YAML | Generated Java |
|------|---------------|
| `match: "eq", value: "X"` | `Mockito.eq("X")` |
| `match: "any"` | `Mockito.any()` |
| `match: "isA", type: "com.app.Task"` | `Mockito.isA(Task.class)` |
| `match: "capture"` | `captor.capture()` |

## Fixture References (`ref:`)

Use the `ref:` prefix to distinguish fixture references from literal strings. Without `ref:`, values are treated as string literals.

| Context | Syntax | Example |
|---------|--------|--------|
| Invoke args | `ref:fixtureId` | `args: [ref:taskInput]` |
| Mock return | `ref:fixtureId` | `thenReturn: ref:taskInput` |
| Assert return | `ref:fixtureId` | `returns: ref:taskInput` |
| List return | `"[ref:a, ref:b]"` | `returns: "[ref:score1, ref:score2]"` |

**Fixtures without `construct:`** — When a fixture omits the `construct:` block, it is instantiated with default/no-arg construction (or as a mock if the type is an interface). Use this for types that need no specific state:

```yaml
- id: milestone
  type: AuditableMilestone    # No construct: block — default instantiation
```

**Omitting `thenReturn:`** — When a mock omits `thenReturn`, Mockito returns `null` by default. Use this to simulate "not found" or "not configured" scenarios:

```yaml
mocks:
  - dependency: "config"
    method: "getTargetRange"
    args: [{ match: "eq", value: "C2" }]
    # No thenReturn — returns null
```

## Assertions

| YAML | Generated Java |
|------|---------------|
| `returns: ref:fixtureId` | `assertEquals(fixture, result)` |
| `returns: "literal"` | `assertEquals("literal", result)` |
| `returns: "[ref:a, ref:b]"` | `assertEquals(List.of(a, b), result)` |
| `returns: "void"` | No return assertion |
| `assertThrows: "RuntimeException"` | `assertThrows(RuntimeException.class, () -> ...)` |
| `doesNotThrow: true` | `assertDoesNotThrow(() -> ...)` |

## Assertion Rules

1. **NEVER write `assert: {}` (empty assertions).** Every `assert:` block MUST contain at least one assertion field (`returns`, `assertThrows`, `doesNotThrow`, or `verifyCall`). An empty assert generates no assertion — the test always passes and provides false confidence.
2. **Always assert return values.** When a method returns a value (non-void), use `returns:` with the expected value. Scalar: `returns: true`, `returns: 42`. Fixture: `returns: "ref:fixtureId"`. If `assertEquals` fails due to identity equality, the developer fixes the model's `equals()` — do not omit the assertion. Note: All Sentinel models with `type: record` are Java records with automatic `equals()`. Assertions with `returns: "ref:fixture"` always work for these types.
3. **`doesNotThrow: true` is a weak assertion.** Use it ONLY when the method is void AND has no observable side effects, or as a complement to an `assertThrows` test. NEVER use `doesNotThrow` as the sole assertion on a method that returns a value.

## verifyCall

Can be a string shorthand or structured object:

```yaml
# Shorthand
verifyCall: "postgresClient.executeSql"

# Structured
verifyCall:
  dependency: "postgresClient"
  method: "executeSql"
  times: 1
  message: "SQL must be called"
```

## Integration Tests (Multi-Step)

```yaml
- name: "Full cycle: create and retrieve"
  type: integration
  fixtures: [...]
  steps:
    - mocks: [...]
      invoke: { method: "save", args: [ref:taskInput] }
      assert: { doesNotThrow: true }

    - mocks: [...]
      invoke: { method: "findById", args: ["uuid-1"] }
      assert: { returns: ref:taskInput }
```

**CRITICAL:** When using `steps:`, you MUST set `type: "integration"`. The code generator silently drops all steps for `type: "unit"` tests, producing tests that only construct fixtures with zero mocks, zero invocations, and zero assertions — tests that always pass and verify nothing.

## Traceability

```yaml
- name: "Should throw on expired passport"
  traceability:
    feature: "FEAT-BOOK-INT"
    rule: "BR-PASS-01"
    journey: "UJ-BUY-ticket"
```

## Exploring Existing Tests

Before writing new tests, explore what already exists using granular queries:

| CLI Command | Purpose |
|-------------|--------|
| `sentinel tool listModules --root .` | Shows test counts per module |
| `sentinel tool inspectModule --root . --module X --include-tests` | Full module with existing test definitions |
| `sentinel tool describeComponent --root . --name X` | Full implementation details including requiresInject |

**Strategy:** Use `listModules` to find which modules have tests, then `inspectModule --include-tests` on the relevant module to see existing coverage before proposing new tests.

## Critical Notes

- **Void methods:** Cannot mock with `when().thenReturn()`. Mockito does nothing by default on void methods — skip mocking them
- **Fixture references:** Always use the `ref:` prefix when referencing fixtures in `args`, `thenReturn`, and `returns` fields. Without `ref:`, values are treated as string literals
- **AssertDef deserialization:** Uses custom `AssertDefDeserializer` for polymorphic `verifyCall` (string vs object)
- **Target resolution:** `target` must match an implementation `name` in sentinel.yaml
- **Last step in multi-step:** The LAST step must NEVER use `doesNotThrow: true` as its only assertion. If the last step calls a result method and you cannot express the expected value with `returns:`, this test needs to be handwritten with `@SentinelTest`.

## Common Patterns

### Happy Path
```yaml
mocks: [{ dependency: "repo", method: "save", args: [{match: "any"}], thenReturn: ref:entity }]
invoke: { method: "save", args: [ref:entity] }
assert: { returns: ref:entity }
```

### Exception Path
```yaml
mocks: [{ dependency: "repo", method: "save", args: [{match: "any"}], thenThrow: { type: "RuntimeException", message: "DB error" } }]
invoke: { method: "save", args: [ref:entity] }
assert: { assertThrows: "RuntimeException" }
```

### Verify Side-Effect
```yaml
invoke: { method: "process", args: [ref:input] }
assert:
  doesNotThrow: true
  verifyCall: { dependency: "notifier", method: "send", times: 1 }
```
