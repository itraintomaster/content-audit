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
    allowedClients: [string]   # Optional: narrow a public package to a specific list of consumer modules
    models: [Model]            # Models in this package
    interfaces: [Interface]    # Interfaces in this package
    implementations: [Implementation]  # Implementations in this package
```

**`allowedClients` on a package** — only meaningful when `visibility: "public"`. Narrows the public package's reach from "every module that can see the owning module" down to the listed modules. Generates an additional ArchUnit rule. Use this when one module exposes multiple public packages but each is meant for a different consumer (e.g., an `api` package consumed only by the CLI, a `spi` package consumed only by plugin authors).

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

**Relocate (cross-scope move).** Moving an element from the module root into a package (or between packages) is **not** a primitive — it is `_change: "delete"` at the old scope plus `_change: "add"` at the new scope, in the same patch:

```yaml
modules:
  - name: "domain"
    _change: "modify"
    implementations:
      - name: "OrderAnalyzer"
        _change: "delete"          # remove from module root
    packages:
      - name: "analyzers"
        _change: "modify"           # or "add" if the package is new
        implementations:
          - name: "OrderAnalyzer"
            _change: "add"          # re-add at the new location
            implements: ["Analyzer"]
```

Merge-by-name is **scoped**: a root-level `OrderAnalyzer` and `analyzers.OrderAnalyzer` are distinct entries. `patch propose` does not detect duplicate-name collisions across scopes — the conflict only surfaces at `sentinel generate` time. Always emit the delete + add pair explicitly when relocating.

## Model

```yaml
models:
  - name: string              # PascalCase (e.g., "Order", "OrderStatus")
    type: record|enum|exception # record = Java record, enum = Java enum, exception = RuntimeException subclass
    visibility: public|internal # Default: public. "internal" = package-private Java class
    extends: string            # Optional supertype (default: RuntimeException for exceptions)
    implements: [string]        # Optional: interfaces this model implements (marker interfaces, sealed hierarchies)
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
    visibility: public|internal # Default: public. "internal" = package-private interface
    sealed: boolean            # true = only declared implementations allowed
    typeParameters: [string]   # Optional generic type parameters (e.g., ["T extends Bound"])
    exposes:
      - signature: string      # "methodName(Type arg): ReturnType"
        throws: [string]       # Optional exception types
        _change: add|modify|delete  # Optional — for removing individual signatures in patches
```

**Signature format:** `methodName(Type argName, Type2 argName2): ReturnType`

- Parameters can be type-only (`read(Path): Result`) or type + name (`read(Path path): Result`)
- Use `:` to separate return type — NOT `->` or `=>`
- No space between method name and `(` — `send(...)` not `send (...)`
- Generics without spaces: `Map<String,Object>` not `Map<String, Object>`
- `_change: "delete"` on a signature removes that single method from the interface without deleting the whole interface
- `patch propose` does NOT validate signatures — malformed signatures pass proposal but fail during `sentinel generate`

When `typeParameters` is set, method signatures can reference the type variables (e.g. `resolve(Report r): Optional<T>`). Implementations specify the concrete type via `implements: ["InterfaceName<ConcreteType>"]`.

## Implementation

```yaml
implementations:
  - name: string              # PascalCase (e.g., "PostgresOrderAdapter")
    implements: [string]       # Interfaces this class implements (always an array)
    visibility: public|internal # Default: internal (package-private). "public" = accessible cross-module
    externalImplements:        # External framework/library interfaces (FQN with generics)
      - string                 # e.g., "java.util.concurrent.Callable<Integer>"
    requiresInject:            # Constructor-injected dependencies
      - name: string           # camelCase dependency name
        type: string           # Interface or class type being injected
    types: [string]            # Framework annotations: Component, Service, Repository,
                               #   RestController, UseCase, Adapter
```

**`externalImplements`** — Use when the implementation class must implement an interface from an external framework or the JDK (e.g., picocli's `Callable<Integer>`, Spring's `InitializingBean`, Jackson's `Serializer<T>`). These are NOT validated against sentinel interfaces and are passed through to the generated class declaration as-is. Always use the fully-qualified name.

### Visibility Defaults

| Component | Default | Meaning |
|---|---|---|
| Model | `public` | Accessible from any module |
| Interface | `public` | Accessible from any module |
| Implementation | `internal` | Package-private (same module only) |

Implementations are internal by default — external modules depend on interfaces, not concrete classes. Use `visibility: "public"` on an implementation only when it must be instantiated from another module (e.g., CLI wiring, factory classes).

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

**Interface completeness principle:** Every distinct execution flow should be traceable through sentinel interface contracts. If an implementation handles multiple cases (e.g., filtered vs unfiltered), each case should be a separate method on the interface (e.g., `runAudit()` and `runFilteredAudit(analyzers)`), not a single method with internal branching that creates ad-hoc concrete classes. This ensures:
- Sequence diagrams capture ALL execution paths (flows through injected interfaces are visible; flows through locally instantiated classes are not)
- The architecture is explicit about what operations exist
- Tests can target each path independently through the contract

## Visibility Toolbox

Two orthogonal axes govern every visibility decision:

- **Java visibility** (public class vs. package-private) — controlled by `visibility` on interfaces and implementations.
- **Cross-module accessibility** (JPMS-style qualified exports) — controlled by `visibility` on packages and `allowedClients` on modules **or packages**.

A type can be a `public` class in Java but unreachable from another module if its package is `internal`. Both axes must be considered for every public-facing declaration.

| Concern | Tool | Element | Default | Values |
|---------|------|---------|---------|--------|
| Which modules can depend on this module | `allowedClients: [module]` | module | open to all | list of modules |
| Which modules can reach a public package (per-package gate) | `allowedClients: [module]` | package | open to all that can see the module | list of modules |
| Is this package reachable from other modules | `visibility` | package | `internal` | `public` / `internal` / `private` |
| Can sibling packages in the same module access it | `visibility: "private"` | package | — | `private` forbids even the module root |
| Is this type a public class in Java | `visibility` | implementation | `internal` (package-private) | `public` / `internal` |
| Is this type a public interface in Java | `visibility` | interface | `public` | `public` / `internal` |
| Which methods of the interface are callable | `exposes: [signature]` | interface | all listed | explicit subset |
| Who may implement this interface | `sealed: true` + permits | interface | open | sealed + permits list |
| How does outside code construct instances without seeing the constructor | `stereotype: "factory"` + `patterns: [Factory]` | interface + module | — | declarative |
| What sealed / marker types does a model belong to | `implements: [Interface]` | model | none | list (sealed hierarchies, marker types) |

## Common Patterns

One-line summaries. See the architect agent's **Pattern Catalog** for full shape, when-to-apply criteria, and canonical examples.

- **Public Port, Hidden Adapter** — public interface at module root + package-private implementation in a restricted package. Default for hexagonal modules with a single adapter.
- **Factory Seam** — `stereotype: "factory"` interface in module root + one public implementation inside an internal package whose other members are package-private.
- **Config Record** — public record collecting wiring inputs; nullable optional fields, required mandatory fields. Pair with Factory Seam when wiring takes >3–4 inputs.
- **Strategy Registry by Key** — `Map<Key, Strategy>` injected into a dispatcher with a fallback strategy. Use when the plugin point is indexed by a runtime value.
- **Sealed Polymorphism** — `sealed: true` interface with explicit permits. Closed set, compiler exhaustiveness, no plugin need.
- **Module Façade** — module exposes exactly one interface plus its factory; everything else internal. Single-purpose infrastructural modules.
- **Internal Utility Package** — `visibility: "private"` package containing support types the module root cannot access.
- **Qualified Export** — `allowedClients: [consumer-module]` on a module **or** on a `public` package, scoping the surface to a specific consumer. Use the package-level form to expose different parts of one module to different consumers.

## Factory Seam — Worked Example

The factory seam is the canonical mechanism for hiding a graph of collaborators behind a single one-call instantiation point. It assembles four pieces consistently:

1. A `stereotype: "factory"` interface in the **module root** (cross-module surface).
2. A `patterns: [{type: Factory, ...}]` declaration in the module (binds factory to product).
3. A `visibility: "internal"` package containing the engine's internals.
4. **One** `visibility: "public"` implementation in that package (the seam) plus several package-private collaborators around it.

```yaml
modules:
  - name: "nlp-infrastructure"
    description: "NLP tokenization adapter"
    interfaces:
      - name: "NlpTokenizerFactory"
        stereotype: "factory"
        exposes:
          - signature: "create(NlpConfig config): NlpTokenizer"
      - name: "NlpTokenizer"
        exposes:
          - signature: "tokenize(String text): List<Token>"
    models:
      - name: "NlpConfig"        # Config Record — public carrier
        fields:
          - { name: "language", type: "String" }
          - { name: "modelPath", type: "String" }
    patterns:
      - type: "Factory"
        interface: "NlpTokenizerFactory"
        implementations: ["SpacyNlpTokenizerFactory"]
    packages:
      - name: "spacy"
        visibility: "internal"          # engine hidden from other modules
        implementations:
          - name: "SpacyNlpTokenizerFactory"
            visibility: "public"          # the one seam — public class
            implements: ["NlpTokenizerFactory"]
          - name: "SpacyTokenizer"        # collaborator — package-private
            implements: ["NlpTokenizer"]
          - name: "SpacyModelLoader"      # collaborator — package-private
            requiresInject:
              - { name: "config", type: "NlpConfig" }
```

**Why this works:** External modules see exactly two interfaces (`NlpTokenizerFactory`, `NlpTokenizer`) plus the `NlpConfig` carrier. They cannot import `SpacyTokenizer` or `SpacyModelLoader` (same-package only). They cannot construct a `SpacyNlpTokenizerFactory` from the wrong place — the package is `internal`. The composition root calls `new SpacyNlpTokenizerFactory().create(config)`; everyone else asks for `NlpTokenizerFactory` by injection. P2, P4, and P5 are all satisfied by this single shape.
