# CLI Commands

Sentinel provides a CLI built with picocli for project generation, scaffolding, and validation.

## Commands Overview

| Command | Purpose | Input | Output |
|---------|---------|-------|--------|
| `sentinel generate` | Generate all code from YAML | `-i sentinel.yaml` | Java sources, tests, AGENTS.md, SKILL |
| `sentinel init` | Scaffold new project | `-i sentinel.yaml` (optional) | pom.xml, directories, template YAML |
| `sentinel validate` | Verify architecture compliance | `-i sentinel.yaml -o ./` | ArchUnit test execution |
| `sentinel requirement validate` | Validate requirement file | `--file requirements/YYYY-MM-DD.NN_name/` | Validation report |
| `sentinel requirement coverage` | Check test-to-rule coverage | `--file requirements/YYYY-MM-DD.NN_name/` | Coverage matrix |
| `sentinel architecture diff` | Diff current vs patch | `--patch requirements/.../architectural_patch.yaml` | Colored diff output |
| `sentinel patch apply` | Apply patch to sentinel.yaml | `--patch requirements/.../architectural_patch.yaml` | Updated sentinel.yaml |
| `sentinel report generate` | Generate sentinel report | `-i sentinel.yaml` | `.sentinel/sentinel-report.yaml` |

## sentinel generate

Generates all artifacts from a `sentinel.yaml` definition.

```bash
java -jar sentinel-core.jar generate -i sentinel.yaml -o ./
```

**Options:**
- `-i, --input` (required): Path to sentinel.yaml file
- `-o, --output, --root` (default: `.`): Project root directory

**Pipeline:**
1. Parse sentinel.yaml via DslReader
2. Scaffold project structure (pom.xml, directories)
3. Generate models, interfaces, tests, implementations, ArchUnit, JPMS, AGENTS.md, SKILL
4. Write artifacts with smart-merge protection

**Smart-merge behavior:**
- @Generated files → fully regenerated
- Implementation files without @Generated → new methods added, existing code preserved
- User-owned files → skipped entirely

## sentinel init

Initializes a new Sentinel project.

```bash
# With definition file — scaffolds full project
java -jar sentinel-core.jar init -i sentinel.yaml -o ./my-project

# Without definition — creates template sentinel.yaml
java -jar sentinel-core.jar init -o ./my-project
```

**Options:**
- `-i, --input` (optional): Path to existing sentinel.yaml
- `-o, --output` (default: `.`): Target project directory

**With input file:** Generates root pom.xml, module pom.xml files, src directories

**Without input file:** Creates a minimal template:
```yaml
system: MySystem
version: 0.0.1
architecture: hexagonal
packagePrefix: com.example

modules:
  - name: domain
    description: Domain logic
```

## sentinel validate

Validates architecture compliance by generating and running ArchUnit tests.

```bash
java -jar sentinel-core.jar validate -i sentinel.yaml -o ./
```

**Options:**
- `-i, --input` (required): Path to sentinel.yaml
- `-o, --output` (required): Project root directory

**Flow:**
1. Parse sentinel.yaml
2. Run `ExistenceValidator` — checks all declared artifacts exist on filesystem
3. Generate `SentinelArchitectureTest.java`
4. Execute `mvn test -Dtest=SentinelArchitectureTest`
5. Exit code 0 = all validations pass

## sentinel tool (Architecture Queries)

Exposes granular, read-only queries over the architecture. Used by agents to explore incrementally instead of loading the full `sentinel.yaml`.

```bash
# List all modules with descriptions, component counts, and dependency info
java -jar sentinel-core.jar tool listModules --root /path/to/project

# Read full architecture with test definitions stripped (significantly smaller output)
java -jar sentinel-core.jar tool readArchitectureNoTests --root /path/to/project

# Inspect a single module with its dependency contracts
java -jar sentinel-core.jar tool inspectModule --root /path/to/project --module <moduleName> [--include-tests]

# Describe a specific model, interface, or implementation by name
java -jar sentinel-core.jar tool describeComponent --root /path/to/project --name <ComponentName>
```

**Context Strategy for Agents:**
1. Start with `listModules` — gives you the map (modules, components, test counts)
2. Use `inspectModule` to dive into relevant modules and their dependency contracts
3. Use `describeComponent` for full details on a specific model/interface/implementation
4. Use `readArchitectureNoTests` only if you need the full structural picture
5. Avoid reading the full `sentinel.yaml` unless you specifically need test definitions

## Error Recovery

| Error | Cause | Fix |
|-------|-------|-----|
| ArchUnit: forbidden access | Import from non-`dependsOn` module | Remove import, use correct port |
| Sealed class violation | Undeclared implementation | Add to `sentinel.yaml` implementations |
| Constructor mismatch | Wrong `requiresInject` params | Align constructor with YAML declaration |
| Test failure | Implementation doesn't match contract | Fix implementation to satisfy test assertions |
| ExistenceValidator | Missing declared class | Create the class or update YAML |
