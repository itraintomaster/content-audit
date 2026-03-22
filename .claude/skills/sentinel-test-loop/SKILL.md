---
name: sentinel-test-loop
description: >
  Orchestrates the test creation loop: qa-tester proposes tests, test-reviewer verifies them
  mechanically against sentinel.yaml and REQUIREMENT.md, then feedback is sent back to fix errors.
  Use this skill whenever the user asks to create, generate, or design tests for any implementation.
  Triggers on: "create tests", "generate tests", "design tests", "test X", "tests for X".
---

<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->

# Sentinel Test Loop

You are orchestrating a quality-controlled test creation pipeline. Instead of invoking the qa-tester alone, you run a loop that produces verified, correct test proposals.

The loop exists because the qa-tester generates many tests at once and tends to make mechanical errors (wrong fixture types, missing mock dependencies, assertions that contradict requirements). The test-reviewer catches these errors by cross-referencing against sentinel.yaml and REQUIREMENT.md.

## Protocol

When the user asks for tests for an implementation, follow this protocol exactly:

### Step 1: Invoke qa-tester

Spawn the `qa-tester` agent with the user's request. Include the target implementation name and requirement path if provided.

```
Agent(subagent_type: "qa-tester", prompt: "<user's request>")
```

The qa-tester will analyze the implementation, propose tests, and wait for confirmation. Its output includes proposed test titles with rationale.

### Step 2: Check qa-tester proposal quality

Before sending to the reviewer, verify the qa-tester's proposal:
- **DSL limitation check**: If the qa-tester classified the implementation as "DSL-limited" or "stateful accumulation" but still generated declarative tests with `doesNotThrow` as sole assertion — STOP. Inform the user that these scenarios need `@SentinelTest` handwritten tests. Do NOT send placeholder tests to the reviewer.
- **Requirement ownership**: Verify each test traces to a rule that the target implementation actually owns. If a test covers aggregation, orchestration, or cross-component behavior, flag it — the test belongs to a different implementation.

### Step 3: Invoke test-reviewer

Take the qa-tester's complete proposal (all proposed tests with their details) and send it to the `test-reviewer` agent for mechanical verification.

```
Agent(subagent_type: "test-reviewer", prompt: "Review the following test proposal for <ImplName> in <moduleName>. Read sentinel.yaml and <requirement-path>. <paste full proposal>")
```

The test-reviewer returns a structured report with issues found, tests verified OK, and a summary table.

### Step 4: Evaluate reviewer output

- If the reviewer found **0 errors** (warnings and speculation are acceptable): proceed to Step 6.
- If the reviewer found **errors**: proceed to Step 4b.

### Step 4b: Correction loop (max 3 iterations)

Spawn a **new** `qa-tester` agent with the reviewer's error list and explicit instructions to correct the errors and generate the patch directly (skip re-proposing titles).

```
Agent(subagent_type: "qa-tester", prompt: "The test-reviewer found the following errors in the test proposal for <ImplName> in <moduleName>. Correct them and generate the patch directly via the CLI (skip proposing titles). Errors: <paste full error list>")
```

After the qa-tester corrects and generates the patch, **re-run the test-reviewer** on the corrected proposal. If the reviewer finds 0 errors, proceed to Step 6. If errors remain, repeat Step 4b. **Maximum 3 correction iterations** — if errors persist after 3 rounds, STOP and present the remaining issues to the user.

### Step 5: DSL escalation

If during correction the qa-tester or reviewer identifies that an error cannot be fixed because the declarative DSL cannot express the test (e.g., complex nested return types, stateful accumulation, parameterized inputs), escalate to the user:
1. List which tests cannot be expressed declaratively and why
2. Recommend `@SentinelTest` handwritten tests for those scenarios
3. Continue with the remaining declarative tests that ARE expressible

### Step 6: Present results to user

Summarize what was produced:
- Number of tests generated
- Coverage by requirement rule
- Any corrections the reviewer caught and the qa-tester fixed
- Number of correction iterations needed
- Any tests escalated to handwritten (with rationale)
- Path to the generated patch file

## Important Rules

1. **Never skip the test-reviewer.** Even if the qa-tester's output looks correct, always run the reviewer. Mechanical errors are invisible to humans reading test names.

2. **Pass the FULL proposal to the reviewer.** Don't summarize or filter — the reviewer needs all details to cross-reference against sentinel.yaml.

3. **Re-review after corrections.** Unlike the original single-round protocol, always re-run the reviewer after corrections. Corrections can introduce new errors.

4. **Don't add your own test suggestions.** Your role is orchestration, not test design. The qa-tester designs tests, the reviewer verifies them, you connect them.

5. **The qa-tester must generate the patch.** The final output is always a validated patch file at `.sentinel/proposals/test-patch-{module}-{Impl}.yaml`, produced by the qa-tester via the Sentinel CLI. You never write test YAML yourself.

6. **Escalate DSL limitations, never work around them.** If a test scenario cannot be expressed with the declarative DSL, do NOT generate a `doesNotThrow` placeholder. Escalate to the user with a recommendation for `@SentinelTest`.

7. **Verify requirement ownership.** Each test must target a rule that the implementation actually implements. Tests for aggregation rules on a single-quiz analyzer, or orchestration rules on a repository, are wrong — flag them and skip.
