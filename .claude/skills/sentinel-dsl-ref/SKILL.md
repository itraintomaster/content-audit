---
name: sentinel-dsl-ref
description: >
  Sentinel DSL and patch schema reference. Use when composing patches or when you
  need the exact YAML schema for modules, models, interfaces, implementations,
  packages, patterns, dependencies, or common types.
---

<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->

# Sentinel DSL Schema Reference

Complete reference for all Sentinel YAML structures you can use in patches.

## Module

```yaml
modules:
  - name: string              # Module name (e.g., "payments", "domain")
    description: string        # What this module is responsible for
    scope: public|internal     # "public" = exported API, "internal" = generates module-info.java
    dependsOn: [string]        # Other module names this depends on (ArchUnit-enforced)
    allowedClients: [string]   # Restrict which modules can depend on this (JPMS qualified exports)
    uses: [string]             # Aliases of root-level dependencies this module uses
    models: [Model]
    interfaces: [Interface]
    implementations: [Implementation]
    packages: [Package]           # Organizational groupings with visibility control
    patterns: [PatternDeclaration]
```

## Package

Packages are **organizational groupings** within a module. They create Java sub-packages with visibility control but do NOT create separate Maven modules.

```yaml
packages:
  - name: string              # lowercase Java identifier (e.g., "analyzers", "validation")
    description: string        # What this package is responsible for
    visibility: public|internal|private  # Default: internal
    models: [Model]            # Models in this package
    interfaces: [Interface]    # Interfaces in this package
    implementations: [Implementation]  # Implementations in this package
```

**Visibility:**
- `public` — Accessible from any module (exported API of this module)
- `internal` (default) — Accessible within this module only, not from other modules
- `private` — Accessible only within this package, not even from the module root

**Java mapping:** Module `domain` (base `com.acme.domain`) with package `analyzers` → `com.acme.domain.analyzers`

**ArchUnit enforcement:** Generated rules prevent external access to `internal` and `private` packages.

**Sub-domain encapsulation pattern:** Use packages to model sub-domains that expose a public interface and/or model but hide their internal implementation. Place the contract (interface + shared models) in a `public` package and the implementations in an `internal` or `private` sibling package. This enforces that consumers depend on the contract, never on the implementation details.

Example — a `pricing` sub-domain inside an `orders` module:
```yaml
packages:
  - name: "pricing"
    visibility: "public"          # Exposed contract
    interfaces:
      - name: "PricingEngine"
        exposes:
          - signature: "calculate(Order order): PriceBreakdown"
    models:
      - name: "PriceBreakdown"
        fields:
          - { name: "subtotal", type: "BigDecimal" }
          - { name: "tax", type: "BigDecimal" }
          - { name: "total", type: "BigDecimal" }
  - name: "pricinginternal"
    visibility: "private"          # Hidden implementation
    implementations:
      - name: "TieredPricingEngine"
        implements: ["PricingEngine"]
        requiresInject:
          - { name: "discountRepository", type: "DiscountRepository" }
```

## Model

```yaml
models:
  - name: string              # PascalCase (e.g., "Order", "OrderStatus")
    type: record|enum|exception # record = Java record, enum = Java enum, exception = RuntimeException subclass
    extends: string            # Optional supertype (default: RuntimeException for exceptions)
    message: string            # Optional message template for exceptions (uses String.format with field values)
    fields:
      - name: string           # camelCase for records, UPPER_CASE for enum constants
        type: string           # Java type (ignored for enums)
        description: string    # Optional documentation
```

Exception example:
```yaml
  - name: "OrderNotFoundException"
    type: exception
    extends: "RuntimeException"   # default, can be omitted
    message: "Order not found: %s" # %s placeholders filled from fields in order
    fields:
      - { name: "orderId", type: "UUID" }
```

## Interface

```yaml
interfaces:
  - name: string              # PascalCase (e.g., "OrderRepository")
    stereotype: string         # Free-text label: "port", "gateway", "repository", etc.
    sealed: boolean            # true = only declared implementations allowed
    exposes:
      - signature: string      # "methodName(Type arg): ReturnType"
        throws: [string]       # Optional exception types
```

**Signature format:** `methodName(Type argName, Type2 argName2): ReturnType`

- Parameters can be type-only (`read(Path): Result`) or type + name (`read(Path path): Result`)
- Use `:` to separate return type — NOT `->` or `=>`
- No space between method name and `(` — `send(...)` not `send (...)`
- Generics without spaces: `Map<String,Object>` not `Map<String, Object>`
- `patch propose` does NOT validate signatures — malformed signatures pass proposal but fail during `sentinel generate`

## Implementation

```yaml
implementations:
  - name: string              # PascalCase (e.g., "PostgresOrderAdapter")
    implements: [string]       # Interfaces this class implements (always an array)
    externalImplements:        # External framework/library interfaces (FQN with generics)
      - string                 # e.g., "java.util.concurrent.Callable<Integer>"
    requiresInject:            # Constructor-injected dependencies
      - name: string           # camelCase dependency name
        type: string           # Interface or class type being injected
    types: [string]            # Framework annotations: Component, Service, Repository,
                               #   RestController, UseCase, Adapter
```

**`externalImplements`** — Use when the implementation class must implement an interface from an external framework or the JDK (e.g., picocli's `Callable<Integer>`, Spring's `InitializingBean`, Jackson's `Serializer<T>`). These are NOT validated against sentinel interfaces and are passed through to the generated class declaration as-is. Always use the fully-qualified name.

## External Dependencies (root-level)

Dependencies are **optional** — declare them when an external type is structurally significant to the architecture (e.g., it appears in interface signatures or as an injected type).

```yaml
dependencies:
  - alias: string             # Short name referenced by module.uses
    artifact: string           # groupId:artifactId:version
    scope: string              # compile (default) | test | provided | runtime
    provides:                  # Types this dependency exports
      - type: string           # Class name (e.g., "JdbcTemplate")
        package: string        # Full package (e.g., "org.springframework.jdbc.core")
```

Modules reference dependencies via `uses: ["alias"]` to enable type resolution.

**Patches cannot add dependencies.** The patch format only supports `modules`. If your design requires a new external dependency, instruct the user to add it to `sentinel.yaml` first. Then reference it in the patch via `uses: ["alias"]` on the relevant module.

## Common Types

| Category | Types |
|----------|-------|
| Primitives | `int`, `long`, `double`, `float`, `boolean`, `byte`, `short`, `char` |
| Java types | `String`, `UUID`, `BigDecimal`, `BigInteger`, `LocalDate`, `LocalDateTime`, `Instant`, `Duration` |
| Generics | `List<T>`, `Set<T>`, `Map<K,V>`, `Optional<T>` |
| Custom | Any model or interface name from the same definition |

## Pattern Declarations

```yaml
patterns:
  - type: Strategy            # Strategy, Decorator, Observer, Adapter, Factory, etc.
    interface: string          # The interface being patterned
    implementations: [string]  # For Strategy: interchangeable implementations
    base: string               # For Decorator: the real implementation
    decorators: [string]       # For Decorator: wrapping implementations
    observers: [string]        # For Observer: listener implementations
```

---

## Patterns Catalog

| Pattern | When to use | Sentinel expression |
|---------|------------|---------------------|
| **Hexagonal** | Isolate domain from infrastructure | Domain module with interfaces (ports), infra module with implementations (adapters) |
| **DDD Bounded Contexts** | Separate business subdomains | One module per context, explicit `dependsOn` |
| **Dependency Inversion** | High-level shouldn't depend on low-level | Interface in domain, implementation in infra, `dependsOn` points inward |
| **Strategy** | Multiple interchangeable algorithms | One interface + N implementations, `patterns: [{type: "Strategy"}]` |
| **Repository** | Abstract data access | Interface with CRUD signatures, `types: ["Repository"]` on implementation |
| **Adapter** | Translate between interfaces | Implementation that wraps external type via `requiresInject` |

## Packages vs Sub-Modules

| Criterion | Packages | Sub-Modules |
|-----------|----------|-------------|
| **Purpose** | Organize components within a module | Separate compilation units |
| **Maven** | Same `pom.xml` | Own `pom.xml` |
| **Java** | Sub-package (e.g., `com.acme.domain.analyzers`) | Separate module |
| **Visibility** | `public`/`internal`/`private` with ArchUnit enforcement | Module boundary |
| **Nesting** | Flat (one level only) | Can be nested |
| **Use when** | Module has >5-6 components; need bounded contexts within module; want to hide internals | Need separate build artifacts; need independent versioning |

**Rules of thumb:**
- Start without packages — only add them when a module grows complex
- Use `internal` (default) for most packages — only expose `public` for the module's API surface
- Use `private` for implementation details that even sibling packages shouldn't access
- **Prefer packages for sub-domains** — when a module contains a cohesive sub-domain (e.g., pricing, scheduling, notifications), encapsulate it: expose the contract in a `public` package and hide the implementation in a `private` package. This is lighter than a full sub-module
- Package names must be valid Java identifiers (lowercase, no hyphens)
