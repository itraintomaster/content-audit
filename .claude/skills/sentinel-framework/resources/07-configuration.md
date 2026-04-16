# Configuration: sentinel.yaml DSL Specification

The `sentinel.yaml` file is the **single source of truth** for the entire system.
It declares modules, models, interfaces, implementations, tests, and features.

## Root Structure

```yaml
system: "SystemName"          # Unique system identifier
version: "1.0.0"              # Semantic version
architecture: "hexagonal"      # hexagonal | layered | modular
packagePrefix: "com.example"   # Base Java package for all modules

language:                        # Target language for code generation
  name: java                     # Language name (default: java)
  version: "17"                 # Language version (default: 17)

definitions:                    # Requirement files — one per feature folder
  - requirements/2026-02-19.01_my-feature/REQUIREMENT.md

modules: [...]                  # Module definitions
features: [...]                 # Business features (or from definitions)
```

## Module Schema

```yaml
modules:
  - name: "module-name"         # Maven module + package name
    description: "..."           # Human-readable description
    dependsOn: ["other-module"]  # Allowed imports (enforced by ArchUnit)
    allowedClients: ["client"]   # Who can import this (enforced by ArchUnit + JPMS)
    scope: "public"              # public | internal
    models: [...]                # Domain model definitions
    interfaces: [...]            # Port/contract definitions
    implementations: [...]       # Adapter definitions with tests
    packages: [...]              # Organizational groupings with visibility control
```

## Package Schema

Packages organize components within a module into sub-packages with visibility control.

```yaml
packages:
  - name: "analyzers"            # Lowercase Java identifier
    description: "..."           # What this package contains
    visibility: "internal"       # public | internal (default) | private
    models: [...]                # Models in this sub-package
    interfaces: [...]            # Interfaces in this sub-package
    implementations: [...]       # Implementations in this sub-package
```

Visibility: `public` = any module, `internal` = same module only, `private` = same package only.
Java mapping: module `domain` + package `analyzers` → `com.example.domain.analyzers`.

## Model Schema

```yaml
models:
  - name: "ModelName"           # Class name (PascalCase)
    type: record                 # record | enum | exception
    visibility: "public"         # public (default) | internal (package-private)
    extends: "RuntimeException"  # Optional supertype (exception only, default: RuntimeException)
    implements: ["Interface"]     # Optional: interfaces this model implements (marker interfaces, sealed hierarchies)
    message: "Not found: %s"     # Optional message template (exception only, %s from fields)
    fields:
      - name: "fieldName"        # Field name (camelCase)
        type: "String"           # Java type
```

## Interface Schema

```yaml
interfaces:
  - name: "InterfaceName"       # Interface name (PascalCase)
    stereotype: "OutboundPort"   # InboundPort | OutboundPort | (custom)
    visibility: "public"          # public (default) | internal (package-private)
    sealed: true                  # sealed interface ... permits ...
    typeParameters: ["T extends Bound"]  # Optional generic type parameters
    exposes:
      - signature: "method(Type param): ReturnType"
        throws: ["ExceptionType"] # Optional checked exceptions
```

When `typeParameters` is set, method signatures can reference the type variables (e.g. `resolve(Report r): Optional<T>`). Implementations specify the concrete type via `implements: ["InterfaceName<ConcreteType>"]`.

## Implementation Schema

```yaml
implementations:
  - name: "ClassName"            # Implementation class name
    implements: ["Interface"]     # Interfaces to implement
    visibility: "internal"        # internal/package-private (default) | public
    externalImplements:            # External framework/library interfaces (FQN)
      - "java.util.concurrent.Callable<Integer>"
    types: ["Repository"]         # Framework annotations to apply
    requiresInject:               # Constructor dependencies
      - name: "depName"
        type: "DepType"
    tests: [...]                  # Declarative test definitions
    handwrittenTests:              # Handwritten test stubs
      - name: "should do X"       # Test name (becomes @DisplayName + method)
        traceability:              # Optional traceability
          feature: "FEAT-001"
          rule: "RULE-001"
```

**Visibility defaults:** Models and interfaces default to `public`. Implementations default to `internal` (package-private). Use `visibility: "public"` on an implementation only when cross-module instantiation is needed.

## Handwritten Test Schema

The preferred way to declare tests. `sentinel generate` creates JUnit stub classes that the developer implements by hand.

```yaml
handwrittenTests:
  - name: "should save and return entity"  # Required: descriptive name
    traceability:                           # Optional: requirement traceability
      feature: "FEAT-001"
      rule: "RULE-001"
      journey: "JOURNEY-001"
  - name: "should throw on invalid input"  # Another test
```

Generated stub:
```java
@Test @DisplayName("should save and return entity") @Tag("FEAT-001")
public void shouldSaveAndReturnEntity() {
    throw new UnsupportedOperationException("Not implemented yet");
}
```

## Declarative Test Schema

```yaml
tests:
  - name: "Test description"     # Human-readable test name
    target: "ClassName"           # Class under test (optional, inferred)
    type: unit                     # unit | integration
    traceability:                  # Link to feature/rule/journey
      feature: "FEAT-001"
      rule: "RULE-001"
      journey: "JOURNEY-001"
    fixtures: [...]                # Test objects
    mocks: [...]                   # Mock behavior
    invoke:                        # Method to call
      method: "methodName"
      args: ["fixture-ref", "literal"]
    assert:                        # Verification
      doesNotThrow: true
      returns: "value"
      assertThrows: "ExceptionType"
      verifyCall:
        dependency: "depName"
        method: "method"
        times: 1
        message: "Verification message"
    steps: [...]                   # For integration tests only
```

## Feature Schema

```yaml
features:
  - id: "FEAT-001"               # Unique feature identifier
    name: "Feature Name"
    description: "..."
    code: "F-FN"                    # Auto-generated feature code
    rules:
      - id: "RULE-001"
        name: "Rule Name"
        severity: high
        description: "..."
        errorMessage: "..."
    journeys:
      - id: "JOURNEY-001"
        name: "Journey Name"
        steps:
          - "Step 1 description"
          - "Step 2 description"
```

## External Definitions

Each requirement lives in its own dated folder under `requirements/`.

**Auto-discovery:** The engine automatically scans `requirements/*/` for `REQUIREMENT.md` (or `requirement.yaml`) files. No explicit `definitions:` entries are needed.

**Explicit mode:** You can optionally list specific files via `definitions:` for fine-grained control:

```yaml
# sentinel.yaml (optional — auto-discovered if omitted)
definitions:
  - requirements/2026-02-19.01_user-registration/REQUIREMENT.md
  - requirements/2026-02-19.02_payment-processing/REQUIREMENT.md
```

Each `REQUIREMENT.md` is the source of truth for a feature. The engine parses them into the main definition.
See `11-requirements.md` for the full convention including architecture patches.
