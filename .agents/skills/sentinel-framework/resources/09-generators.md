# Generators Pipeline

Sentinel's code generation runs through a sequence of specialized generators.
Each generator reads the `SentinelDefinition` and produces `GeneratedArtifact` records.

## Execution Order

The `GenerateCommand` invokes generators in this strict order:

| # | Generator | Output | Location |
|---|-----------|--------|----------|
| 0 | `ScaffoldGenerator` | pom.xml, directories | `<module>/pom.xml` |
| 1 | `ModelGenerator` | Java model classes | `<module>/src/main/java/` |
| 2 | `InterfaceGenerator` | Java interfaces | `<module>/src/main/java/` |
| 3 | `JunitGenerator` | JUnit 5 test classes | `<module>/src/test/java/` |
| 4 | `ImplementationGenerator` | Stub classes | `<module>/src/main/java/` |
| 5 | `ArchUnitGenerator` | Architecture test | `<module>/src/test/java/` |
| 6 | `ModuleInfoGenerator` | JPMS module-info | `<module>/src/main/java/` |
| 7 | `AgentRulesGenerator` | AGENTS.md files | `<root>/`, `<module>/` |
| 8 | `SkillGenerator` | Claude Code skill | `<root>/.claude/skills/` |

## Generator Details

### ScaffoldGenerator

Creates the Maven project structure:
- **Root pom.xml** with all modules, plugins (exec-maven-plugin for Sentinel), and shared dependencies
- **Module pom.xml** with parent reference and inter-module `<dependency>` from `dependsOn`
- **Directory structure:** `src/main/java`, `src/test/java` per module
- Shared dependencies: JUnit 5, Mockito, ArchUnit

### ModelGenerator

Generates model classes from `models` definitions:
- Private fields with getters and setters
- No-args constructor + all-args constructor
- `@Generated` annotation on the class
- Type resolution: `String`, `int`, `long`, `double`, `float`, `boolean`, `UUID`, `List<T>`

### InterfaceGenerator

Generates interface files from `interfaces` definitions:
- **Sealed interfaces** (same module): raw Java output with `sealed ... permits ...`
- **Regular interfaces**: JavaPoet-generated with `@Generated` annotation
- Method signatures parsed from `"methodName(Type param): ReturnType"` format
- Supports generics: `Optional<T>`, `List<T>`

### JunitGenerator

Generates JUnit 5 + Mockito tests from `tests` definitions:
- `@ExtendWith(MockitoExtension.class)` on test class
- `@Mock` fields for all declared mock dependencies
- `@InjectMocks` for the system under test
- Fixture instantiation, mock setup, invocation, assertions
- Support for integration tests with `steps`
- Traceability: `@Tag` annotations for feature/rule linkage

### ImplementationGenerator

Generates implementation stubs:
- `@Generated` annotation + `@Override` on methods
- Stub body: `throw new UnsupportedOperationException("Not implemented yet")`
- Constructor with `requiresInject` parameters
- The AI agent replaces these stubs with real logic

### ArchUnitGenerator

Generates `SentinelArchitectureTest.java` per module:
- `enforceModuleBoundaries()` — validates `dependsOn` constraints
- `enforce<Module>AllowedClients()` — validates `allowedClients` constraints
- `enforce<Interface>SealedImplementation()` — cross-module sealed enforcement
- `enforceAllDeclaredClassesExist()` — checks all declared classes are present

### ModuleInfoGenerator

Generates `module-info.java` for JPMS:
- `requires java.compiler` (for `@Generated`)
- `requires <module>` for each `dependsOn` entry
- `exports <pkg>` or `exports <pkg> to <client>` based on `allowedClients`
- Skipped when `scope: public`

### AgentRulesGenerator

Generates `AGENTS.md` guidance files:
- **Root AGENTS.md:** global rules, sealed interfaces, module map, features, boundaries
- **Module AGENTS.md:** per-module models, interfaces, implementations, test names, dependencies

### SkillGenerator

Generates this Claude Code skill:
- **SKILL file:** entry point with framework overview
- **Resource files:** one per concern (modules, models, interfaces, etc.)
- Regenerated on every `sentinel generate` to stay in sync with `sentinel.yaml`

## GeneratedArtifact Record

All generators produce `GeneratedArtifact` records:

```java
record GeneratedArtifact(
    JavaFile file,           // JavaPoet file (null for raw files)
    String rawContent,        // Raw string content (null for JavaPoet files)
    String rawRelativePath,   // Relative path for raw files
    String moduleName,        // Target module (null for root-level files)
    ArtifactType type          // SOURCE or TEST
)
```

Two factory methods:
- `GeneratedArtifact.javaFile(file, moduleName, type)` — for JavaPoet-generated code
- `GeneratedArtifact.rawFile(content, path, moduleName, type)` — for raw files (AGENTS.md, module-info, sealed interfaces, SKILL)
