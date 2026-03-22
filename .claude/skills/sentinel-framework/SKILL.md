---
name: sentinel-framework
description: |
  Sentinel is an AI-governed architecture framework that bridges human intent with AI-generated code.
  It treats software architecture as executable infrastructure using a Kubernetes Operator Pattern:
  the human declares the Desired State (sentinel.yaml), the engine enforces it via immutable contracts
  (sealed interfaces, records, ArchUnit rules, JUnit tests), and the AI agent implements against those contracts.
  Non-compliance is a compilation error, not a code review comment.
globs:
  - sentinel.yaml
  - "**/sentinel-*.yaml"
  - "**/AGENTS.md"
  - "**/*SentinelTest.java"
  - "**/*SentinelArchitectureTest.java"
  - "**/module-info.java"
  - "requirements/**/requirement.yaml"
  - "requirements/**/architectural_patch.yaml"
resources:
  - resources/01-modules.md
  - resources/02-models.md
  - resources/03-interfaces.md
  - resources/04-implementations.md
  - resources/05-features.md
  - resources/06-tests.md
  - resources/07-configuration.md
  - resources/08-architecture.md
  - resources/09-generators.md
  - resources/10-cli-commands.md
  - resources/11-requirements.md
---

# Sentinel Framework

**"Define the Desired State. Enforce the Implementation."**

## System: ContentAudit
- **Version:** 0.0.1
- **Language:** java 17
- **Architecture:** hexagonal
- **Package Prefix:** com.learney.contentaudit

## What is Sentinel?

Sentinel is an architectural governance framework that applies the **Kubernetes Operator Pattern**
to software development:

1. **Human (Control Plane)** declares the Desired State in `sentinel.yaml` — modules, models, interfaces, tests
2. **Engine (Enforcer)** generates immutable contracts — sealed interfaces, Java records, ArchUnit rules, JUnit tests
3. **AI Agent (Worker Node)** implements logic that satisfies the contracts — the tests must pass, the interfaces must be implemented

**Non-compliance is a compilation error, not a code review comment.**

## The Three Pillars of Enforcement

| Pillar | Mechanism | Effect |
|--------|-----------|--------|
| Structural Immutability | Sealed interfaces + Java Records | AI cannot invent types not declared in YAML |
| Architectural Boundaries | ArchUnit + JPMS module-info.java | Forbidden imports cause build failure |
| Behavioral Contracts | Declarative JUnit 5 + Mockito tests | Implementation must match declared behavior |

## Critical Rules for AI Agents

1. **NEVER** modify files with `@Generated` annotation or `SENTINEL MANAGED FILE` header
2. **ALWAYS** read `sentinel.yaml` before writing implementation code
3. **NEVER** use `new` for dependencies — use constructor injection from `requiresInject`
4. **ALWAYS** implement exactly the interfaces declared in the YAML
5. **NEVER** create implementations not listed in `sentinel.yaml`
6. **NEVER** edit `sentinel.yaml` directly — delegate architectural changes to the **architect agent** (`@architect`)

## Resource Navigation

| Resource | Purpose | When to Use |
|----------|---------|-------------|
| `01-modules.md` | Module boundaries, dependencies, access control | Understanding system structure |
| `02-models.md` | Domain models, fields, type mappings | Working with data structures |
| `03-interfaces.md` | Port contracts, sealed interfaces, method signatures | Implementing adapters |
| `04-implementations.md` | Adapter stubs, dependency injection, framework types | Writing implementation code |
| `05-features.md` | Business rules, user journeys, traceability | Understanding requirements |
| `06-tests.md` | Declarative test schema, fixtures, mocks, assertions | Making tests pass |
| `07-configuration.md` | sentinel.yaml DSL specification and examples | Authoring or modifying YAML |
| `08-architecture.md` | Hexagonal architecture, enforcement pipeline | Architectural decisions |
| `09-generators.md` | Code generation pipeline, generator types | Understanding generated code |
| `10-cli-commands.md` | CLI usage: generate, init, validate, requirement, patch, report | Running Sentinel commands |
| `11-requirements.md` | Requirement folder convention, architecture patch format | Creating requirements and patches |
