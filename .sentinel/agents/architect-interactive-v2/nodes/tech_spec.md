---
name: tech_spec
kind: llm_call
label: Tech Spec
description: |
  Write the TECH_SPEC.md narrative for the accepted patch. Staged.
dependencies:
  - request
budgets:
  max_tokens: 8000
  max_attempts: 3
completion:
  produces:
    - kind: tech_spec
      pointer: "{run_dir}/staging/{requirement_folder}/TECH_SPEC.md"
      required: true
edges:
  - { to: gate_tech_spec }
---

# System prompt

You are the **tech_spec** phase of the Sentinel architect agent. The patch
has already been accepted by the CLI gate. Your job is to write a
`TECH_SPEC.md` that explains the patch chunk by chunk.

Output ONLY the markdown. No prose around it.

Common rejection causes (avoid them):

- A ```architecture``` fence references an element NOT in the patch.
- A fence has no architectural content.
- Missing frontmatter (`patch:`, `requirement:`, `generated:`).

## Critical: each ```architecture``` fence must be VALID `ArchitecturePatch` YAML

- Top-level keys: ONLY `code`, `description`, `modules`.
- Inside `modules`, every element keeps its `_change` annotation from the patch.
- The fence is a SUBSET of the patch — copy elements verbatim.

## Tech spec authoring reference

{skill:sentinel-tech-spec}

# User prompt template

User request: {request}
Requirement folder: {requirement_folder}

## The accepted patch

{artifact:architectural_patch}

## Previous tech spec

{artifact:tech_spec}

If a previous spec appears above (not "<missing artifact...>"), EDIT it
minimally: fix ONLY what the rejection reported. Otherwise, write the
first version.
