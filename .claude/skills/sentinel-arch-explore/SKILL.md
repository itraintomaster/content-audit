---
name: sentinel-arch-explore
description: >
  Sentinel architecture exploration tools. Granular CLI queries for exploring
  architecture incrementally: list modules, inspect modules with dependency contracts,
  describe individual components. Use instead of reading full sentinel.yaml.
---

<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->

# Sentinel Architecture Exploration

Use these CLI tools to explore the architecture incrementally instead of loading the entire `sentinel.yaml` at once. This saves context and lets you focus on what's relevant.

## Context Strategy

1. **Start with `listModules`** — get the system map (modules, components, test counts)
2. **Use `inspectModule`** — dive into relevant modules and their dependency contracts
3. **Use `describeComponent`** — drill into a specific model, interface, or implementation
4. **Use `readArchitectureNoTests`** — only if you need the full structural picture
5. **Avoid `Read sentinel.yaml`** — unless you specifically need test definitions or the raw file

## Available Commands

### listModules

Returns a compact summary of every module: name, description, dependsOn, allowedClients, component names, and test counts.

```bash
java -jar /Users/josecullen/projects/sentinel/sentinel-core/target/sentinel-core-0.0.1-SNAPSHOT.jar tool listModules --root .
```

**Output format:**
```
## system: FlightBooking (v0.0.1, hexagonal)
## packagePrefix: com.flightbook
## dependencies: jdbc (org.springframework:spring-jdbc:6.1.0)

### domain
  description: Core business logic
  dependsOn: []
  allowedClients: [infrastructure]
  models: Booking, Payment
  interfaces: BookingService (sealed), PaymentGateway (sealed)
  implementations: -
  packages: analyzers [public], validation [internal]
    analyzers: 1 model, 1 interface
    validation: 1 model
  tests: 0

### infrastructure
  description: External adapters
  dependsOn: [domain]
  models: -
  interfaces: -
  implementations: BookingAdapter (implements BookingService)
  tests: 6
```

### inspectModule

Returns a focused view of a single module: its full YAML definition plus contracts (models, interfaces, implementation summaries) from its transitive dependencies.

```bash
java -jar /Users/josecullen/projects/sentinel/sentinel-core/target/sentinel-core-0.0.1-SNAPSHOT.jar tool inspectModule --root . --module <moduleName>
# With test definitions:
java -jar /Users/josecullen/projects/sentinel/sentinel-core/target/sentinel-core-0.0.1-SNAPSHOT.jar tool inspectModule --root . --module <moduleName> --include-tests
```

**Key behavior:**
- Target module is serialized in full YAML including packages (tests stripped by default)
- Dependency modules show only **summaries**: model fields, interface signatures, implementation names + which interfaces they implement, plus packages with their components
- To see full details of a dependency implementation, use `describeComponent`

### describeComponent

Finds a specific model, interface, or implementation by name across all modules. Returns full YAML definition, owning module, and relationships.

```bash
java -jar /Users/josecullen/projects/sentinel/sentinel-core/target/sentinel-core-0.0.1-SNAPSHOT.jar tool describeComponent --root . --name <ComponentName>
```

**What it returns per type:**
- **Model**: full fields, type, owning module (+ package if inside one)
- **Interface**: full method signatures, sealed status, list of implementations (+ package if inside one)
- **Implementation**: full definition with requiresInject, tests, implements, owning module (+ package if inside one)
- Searches both root-level and package-level components

### readArchitectureNoTests

Returns the full `sentinel.yaml` as YAML but with all `tests` and `handwrittenTests` stripped from every implementation. Significantly smaller for large systems.

```bash
java -jar /Users/josecullen/projects/sentinel/sentinel-core/target/sentinel-core-0.0.1-SNAPSHOT.jar tool readArchitectureNoTests --root .
```

## When to Use Each Tool

| Scenario | Tool |
|----------|------|
| First look at a system | `listModules` |
| Designing changes to a specific module | `inspectModule` |
| Understanding a specific type or interface | `describeComponent` |
| Exploring packages within a module | `inspectModule` (shows full YAML including packages) |
| Need full picture minus test noise | `readArchitectureNoTests` |
| Need test definitions (QA agent) | `inspectModule --include-tests` |
| Need raw sentinel.yaml | `Read sentinel.yaml` (last resort) |
