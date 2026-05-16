<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->

# Sentinel Interactive Analyst (MCP)

Use this command to run a Sentinel interactive analyst session via MCP tools.
You (Claude) act as the human-in-the-loop, answering the analyst's questions
using your knowledge of the codebase to produce a validated REQUIREMENT.md.

## When to use

- The user asks to create or refine a requirement and you want automated
  requirement quality (gates, glossary sync, validation) without interrupting
  the user for every Q&A turn.
- You have enough context about the feature from the conversation or codebase
  to answer the analyst's questions yourself.

## Prerequisites

The `sentinel-agents` MCP server must be configured in `.mcp.json` and running.
If the MCP tools (`analyst_start`, `analyst_send`) are not available, fall back
to the batch analyst (`@analyst` agent) or suggest the VS Code interactive panel.

## Protocol

### 1. Start the session

Call `analyst_start` with:
- `root`: the absolute path to this project root
- `requirement_folder`: relative path to the requirement folder (e.g. `requirements/2026-05-15.01_feature-name`)
- `request`: a summary of what the requirement should cover (from the user's request)
- `profile`: LLM profile name if configured (check `.sentinel/agent-profiles.yaml`)

The response will contain `status: "waiting_for_input"` with either:
- `interrupted_at: "usage_walkthrough"` — the analyst produced an analysis draft and walkthrough
- An error if configuration is wrong

### 2. Review the analysis (usage_walkthrough interrupt)

The response includes `analysis_draft` and `usage_walkthrough` fields.
Read them to understand what the analyst found. Then:
- If the analysis looks correct, send `"ok"` to proceed to Q&A
- If you have corrections or guidance, send them as the message

```
analyst_send(session_id=..., message="ok")
```

### 3. Answer questions (wait_for_input interrupts)

The analyst asks questions to refine its understanding. The `question` field
contains the current question. Answer using your codebase knowledge:

- Read relevant code, configs, or existing requirements to form your answer
- Be specific and concrete — the analyst uses your answers to write rules
- When the analyst has enough information (or you've exhausted your knowledge),
  send `"done"` or `"listo"` to end Q&A and move to consolidation

```
analyst_send(session_id=..., message="The authentication uses JWT tokens stored in...")
```

### 4. Review the brief (consolidate interrupt)

The analyst consolidates Q&A into a brief. The `brief` field contains it.
- If it captures the requirement scope correctly, send `"ok"`
- If it needs changes, describe what to fix (the analyst will go back to Q&A)

### 5. Completion

After brief approval, the analyst drafts a formal REQUIREMENT.md, validates it
against Sentinel's gates, and syncs the domain glossary. The response will have
`status: "completed"` with `requirement_validated: true`.

At this point, inform the user that the requirement has been created and where
to find it (`run_dir` in the response).

## Error handling

- If `status: "failed"`, report the error to the user and suggest alternatives
- If `status: "halted"`, the analyst exhausted retries — the requirement may need
  manual editing or a fresh session with more context
- Use `analyst_abort` to cleanly terminate a stuck session

## Tips for better results

- Before starting, read existing requirements in `requirements/` to match style
- Read the glossary (`requirements/domain-glossary.yaml`) for consistent terminology
- The more specific your initial `request`, the fewer Q&A turns needed
- When answering questions, cite specific code paths or existing behaviors
