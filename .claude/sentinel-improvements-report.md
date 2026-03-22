# Sentinel Improvements Report: Test Review Loop

**Date**: 2026-03-21
**Project**: content-audit
**Branch**: sentineling/sentence-length-analyzer

## Context

During evaluation of the qa-tester agent on the SentenceLengthAnalyzer implementation, we identified systematic errors in test proposals that the agent cannot self-correct due to context overload. We designed and validated a review loop that catches these errors mechanically before they reach the user.

## Problem

The qa-tester agent makes mechanical errors when proposing many tests at once:
- Uses fixture fields that don't exist in sentinel.yaml models (e.g., `AuditableQuiz.tokenCount` when only `sentence` exists)
- Claims record types lack `equals()` despite `type: record` in sentinel.yaml
- Proposes assertions that contradict requirement rules (e.g., expecting score=1.0 when the rule says "no score produced")
- Omits required mock dependencies that are in `requiresInject`
- Constructs incomplete record fixtures (missing required fields)
- Generates tests based on assumed implementation details not in the interface contract

These errors are invisible when reading test names but produce tests that fail to compile or verify nothing.

## Solution: Test Review Loop

A two-agent loop where a lightweight reviewer verifies every proposal mechanically before the patch is generated.

```
qa-tester proposes → test-reviewer verifies → feedback → qa-tester corrects → patch
```

### Files Changed

#### 1. qa-tester agent: Self-verification step (Step 1.5)

**File**: `/Users/josecullen/projects/learney/ittm/content-audit/.claude/agents/qa-tester.md`

**What changed**: Added a "Step 1.5 — Self-Verification" between the analysis (Step 1) and presenting to the user. It's a 5-point checklist:
- (a) Verify each fixture type exists in sentinel.yaml and whether it's `type: record`
- (b) Verify each mock dependency matches a field in `requiresInject`
- (c) Cross-check each traceability claim against the actual requirement rule text
- (d) Structural checks (integration type with steps, no empty assertions, no speculative tests)
- (e) Report corrections made

**Why**: Forces the agent to re-read sentinel.yaml for specific facts before presenting, reducing errors from attention dilution. In practice, the agent doesn't always follow this step rigorously, which is why the external reviewer exists as a second layer.

#### 2. New agent: test-reviewer

**File**: `/Users/josecullen/projects/learney/ittm/content-audit/.claude/agents/test-reviewer.md`

**What changed**: New agent (sonnet, read-only, tools: [Read]) that performs mechanical verification of qa-tester proposals. Checks 5 categories:
1. Fixture types (exist in sentinel.yaml? record? correct fields?)
2. Mock dependencies (match requiresInject? method exists in interface? void methods not mocked with thenReturn?)
3. Assertions vs requirements (rule exists? behavior matches? no contradictions?)
4. Structural correctness (integration type with steps, complete record fixtures, Gherkin names)
5. Speculative tests (based on undocumented implementation details)

Returns a structured report with numbered issues, severity (error/warning/speculation), source references, and suggested fixes.

**Why**: Independent verification from a clean context catches errors the qa-tester misses. The reviewer has a single focused purpose with minimal instructions, avoiding the context overload problem.

#### 3. New skill: sentinel-test-loop

**File**: `/Users/josecullen/projects/learney/ittm/content-audit/.claude/skills/sentinel-test-loop/SKILL.md`

**What changed**: Orchestration skill that automates the loop. When the user asks for tests, the main conversation:
1. Invokes qa-tester with the user's request
2. Sends the full proposal to test-reviewer
3. If errors found, sends feedback to qa-tester via SendMessage
4. qa-tester corrects and generates the patch
5. Presents final results to the user

**Why**: Without this skill, the user would need to manually invoke qa-tester, then test-reviewer, then send feedback. The skill makes the loop automatic and invisible.

#### 4. AGENTS.md: Added qa-tester and test-reviewer to agent table

**File**: `/Users/josecullen/projects/learney/ittm/content-audit/AGENTS.md`

**What changed**: Added two rows to the Specialized Agents table:
- **QA Tester** (`@qa-tester`): Design and propose declarative tests
- **Test Reviewer** (`@test-reviewer`): Verify qa-tester proposals for correctness

Updated delegation rules to include test design as a delegated task.

**Why**: Agents need to be listed in AGENTS.md so Claude knows to delegate test work instead of doing it inline.

#### 5. CLAUDE.md: Test Creation section

**File**: `/Users/josecullen/projects/learney/ittm/content-audit/CLAUDE.md`

**What changed**: Added a "Test Creation" section instructing to always use the `sentinel-test-loop` skill instead of invoking qa-tester directly.

**Why**: Ensures the review loop is the default path, not an optional step.

## Validation Results

We ran the full loop on SentenceLengthAnalyzer (audit-domain) with requirement F-SLEN:

| Metric | Without loop | With loop |
|--------|-------------|-----------|
| Fixture compilation errors | 11 (tokenCount field) | 0 |
| Missing mock dependencies | 14 (nlpTokenizer) | 0 |
| Requirement contradictions | 1 (R012) | 0 |
| Speculative tests | 2 | 0 |
| Incomplete record fixtures | 14 (AuditContext, ScoredItem) | 0 |
| Final validated tests | N/A | 16 |

## Recommendation for Sentinel Generator

When `sentinel generate` produces AGENTS.md and CLAUDE.md, it should:

1. **Include qa-tester and test-reviewer in the AGENTS.md template** — these agents should be part of every Sentinel-governed project's agent table.
2. **Include the sentinel-test-loop skill reference in CLAUDE.md** — the "Test Creation" section should be generated automatically.
3. **The Step 1.5 self-verification in qa-tester.md** should be part of the qa-tester agent template that Sentinel generates.
4. **The test-reviewer agent** should be a standard agent that Sentinel generates alongside qa-tester.
5. **The sentinel-test-loop skill** should be bundled with Sentinel as a standard skill.
