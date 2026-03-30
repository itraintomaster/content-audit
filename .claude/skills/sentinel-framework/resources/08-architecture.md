# Architecture & Engine Internals

Sentinel applies the **Kubernetes Operator Pattern** to software architecture:
declare the desired state, enforce it, and let workers (AI agents) reconcile.

## Supported Architecture Patterns

| Pattern | Description | Module Layout |
|---------|-------------|---------------|
| `hexagonal` | Ports & Adapters (DDD) | domain -> application -> infrastructure |
| `layered` | Traditional layered | presentation -> service -> repository |
| `modular` | Feature-based modules | feature-a, feature-b, shared |

**Current system architecture:** `hexagonal`

## Hexagonal Architecture in Sentinel

```
+---------------------------+
|    Inbound Adapters       |  (Controllers, REST endpoints)
|    [presentation module]  |
+-------------|-------------+
              |
    +---------v----------+
    |    Application      |  Interfaces (Ports) — InboundPort, OutboundPort
    |    [application]    |
    +---------|-----------+
              |
    +---------v----------+
    |    Domain           |  Models (Records) — immutable entities
    |    [domain]         |
    +--------------------+
              |
+-------------|-------------+
|    Outbound Adapters      |  Implementations — DB, API, messaging
|    [infrastructure]       |
+---------------------------+
```

## Engine Pipeline

The Sentinel engine operates in a strict 3-phase pipeline:

```
sentinel.yaml  ──parse──>  SentinelDefinition  ──generate──>  Java Sources + Tests
```

1. **Parse:** `DslReader` reads YAML via Jackson, produces `SentinelDefinition` object graph
2. **Validate:** Optional `ExistenceValidator` checks all declared artifacts exist
3. **Generate:** Generators produce `GeneratedArtifact` records, written to filesystem

## Enforcement Mechanisms

### 1. Structural Immutability (Compile-Time)

- **Sealed interfaces:** `sealed interface X permits Impl1, Impl2` — prevents undeclared implementations
- **Java Records:** Immutable data models — no setter mutations
- **@Generated annotation:** Marks files owned by Sentinel — agents must not modify

### 2. Architectural Boundaries (Test-Time)

- **ArchUnit rules:** `SentinelArchitectureTest.java` verifies module boundaries
  - `enforceModuleBoundaries()` — modules only depend on declared `dependsOn`
  - `enforce<Module>AllowedClients()` — only `allowedClients` can import
  - `enforce<Interface>SealedImplementation()` — cross-module sealed enforcement
- **JPMS module-info.java:** Compile-time access control via `exports ... to ...`

### 3. Behavioral Contracts (Test-Time)

- **JUnit 5 tests:** Generated from declarative test definitions in YAML
- **Mockito mocking:** Verifies side-effects (method calls, argument passing)
- **Assertion types:** return value, exception throwing, method invocation counts

## DSL Object Graph

```
SentinelDefinition
├── system: String
├── version: String
├── architecture: String
├── packagePrefix: String
├── language: Language
│   ├── name: String                    # e.g. "java"
│   └── version: String                 # e.g. "17"
├── definitions: List<String>           # External YAML file paths
├── modules: List<Module>
│   ├── name, description, dependsOn, allowedClients, scope
│   ├── models: List<Model>
│   │   ├── name: String
│   │   └── fields: List<Field>
│   │       ├── name: String
│   │       └── type: String
│   ├── interfaces: List<Interface>
│   │   ├── name, stereotype, sealed
│   │   └── exposes: List<MethodSignature>
│   │       ├── signature: String
│   │       └── throws: List<String>
│   └── implementations: List<Implementation>
│       ├── name, implements, types, requiresInject
│       ├── handwrittenTests: List<HandwrittenTest>
│       │   ├── name, traceability
│       └── tests: List<DeclarativeTest>
│           ├── name, target, type
│           ├── fixtures: List<FixtureDef>
│           ├── mocks: List<MockDef>
│           ├── invoke: InvokeDef
│           ├── assert: AssertDef
│           ├── steps: List<StepDef>        # integration tests
│           └── traceability: Traceability
└── features: List<Feature>
    ├── id, name, description, code
    ├── rules: List<BusinessRule>
    │   ├── id, name, severity, description, errorMessage
    └── journeys: List<UserJourney>
        ├── id, name
        └── steps: List<String>
```
