---
name: sentinel-tech-spec
description: >
  How to author TECH_SPEC.md — the narrative companion of an architectural patch.
  Use when writing the tech spec that pairs with requirements/<folder>/architectural_patch.yaml.
  Covers chunking, the ```architecture``` fence rules, the WHY convention, and the CLI
  to validate and write it.
---

<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->

# Tech Spec Authoring

A tech spec lives alongside a per-requirement architectural patch:

```
requirements/YYYY-MM-DD.NN_name/
├── REQUIREMENT.md          # feature spec (WHAT)
├── sentinel-baseline.yaml  # frozen snapshot of sentinel.yaml at proposal time
├── architectural_patch.yaml
└── TECH_SPEC.md            # narrative explanation of the patch (WHY, chunk-by-chunk)
```

The spec is a **photo** of the architectural change at the moment it was proposed. Years later, it may look out of date vs. the current architecture — that is by design. The VS Code extension renders each ```architecture``` fence as a mini-diff against `sentinel-baseline.yaml`, so a reader can see "we were here → we went here".

## Structure

```markdown
---
patch: ARCH-001
requirement: 2026-04-17.01_user-age-validation
generated: 2026-04-17T12:34:56Z
---

# Tech Spec: <Feature name>

## Add `age` to Persona model
We need to validate the user's age before checkout. Persona is the natural
carrier — it already centralises identity fields and is referenced by BookingFlow.

```architecture
modules:
  - name: domain
    _change: modify
    models:
      - name: Persona
        _change: modify
        fields:
          - { name: age, type: Integer, _change: add }
```

## Create AgeValidator interface
Validation varies by jurisdiction (US: 21, EU: 18). Extracting the interface
lets us swap validators without touching BookingFlow.

```architecture
modules:
  - name: domain
    _change: modify
    interfaces:
      - name: AgeValidator
        _change: add
        exposes:
          - signature: "validate(Persona p): ValidationResult"
```
```

## Format rules

1. **One `##` section per logical change.** Split the patch into the smallest units that tell a coherent story — a single model field, a single interface, a single package, etc. Never paste the full patch in one fence.
2. **Imperative section titles.** "Add X", "Extract Y interface", "Split Z module". Avoid nouns like "Persona model changes".
3. **Body explains the WHY, not the WHAT.** 2–4 sentences per section. Assume the reader can read the fence — do not re-describe it. Explain the constraint, the decision, and the trade-off you accepted.
4. **Each ```architecture``` fence is a slice of the real patch.** Copy only the modules/elements relevant to the section, but keep the `_change` annotations intact so the fence is a valid standalone `ArchitecturePatch` YAML.
5. **Every element named in a fence must exist in `architectural_patch.yaml`.** The CLI validator rejects fences that reference names absent from the on-disk patch.
6. **Frontmatter carries identifiers.** `patch:`, `requirement:`, `generated:` timestamp.

## Anti-patterns

- Narrating every field ("We add `age` — this is an Integer. We also add ..."). The fence already shows the fields; prose repeats noise.
- One giant fence containing the whole patch at the top. Defeats the purpose of chunking.
- Sections describing files the patch does not touch. The spec must match the patch — if you wanted to touch something else, it belongs in the patch first.
- Treating the spec as a design discussion log (options, alternatives, meeting notes). The spec is the decision + the WHY, not the path to it.

## Writing the tech spec

After `patch propose --requirement-folder <folder>` succeeded, compose the markdown and submit it via stdin:

```bash
java -jar /Users/josecullen/projects/sentinel/sentinel-core/target/sentinel-core-0.0.1-SNAPSHOT.jar tech-spec write --requirement-folder requirements/<folder>/ <<'SPEC'
---
patch: ARCH-001
requirement: <folder-name>
generated: <ISO8601 timestamp>
---

# Tech Spec: <Feature name>

## <Imperative title>
<WHY, 2–4 sentences>

\`\`\`architecture
<slice of the patch>
\`\`\`
SPEC
```

The CLI:
1. Checks `architectural_patch.yaml` and `sentinel-baseline.yaml` exist in the folder.
2. Extracts every ```architecture``` fence and parses it as an `ArchitecturePatch`.
3. Rejects fences that reference modules / models / interfaces / implementations / packages absent from the on-disk patch.
4. Writes `<folder>/TECH_SPEC.md` on success.

If a fence fails validation, fix either the fence (to match the real patch) or re-propose the patch first — never loosen the validation by omitting names.
