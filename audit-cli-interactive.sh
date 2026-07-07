#!/usr/bin/env bash
# Convenience launcher: runs content-audit in INTERACTIVE generation mode (FEAT-LAGAG).
#
# This wrapper just sets CONTENT_AUDIT_LAGEN_MODE=llm_interactive and delegates to the
# normal audit-cli.sh launcher. It does NOT modify the shared launcher, so the regular
# ./audit-cli.sh and the journey tests keep their default behavior.
#
# Interactive mode REQUIRES a provider with real tool-calling (function-calling) support.
# By default this points at OpenAI gpt-4o-mini using your existing OPENAI_API_KEY.
# Override any of the vars below by exporting them before calling this script.
#
# Usage:
#   ./audit-cli-interactive.sh revise task <task-id> [--plan <plan-id>]
# Example:
#   ./audit-cli-interactive.sh revise task task-3663

set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"

# --- generation mode (the FEAT-LAGAG opt-in) ---
export CONTENT_AUDIT_LAGEN_MODE=llm_interactive

# --- provider (defaults to OpenAI; override by pre-exporting these) ---
export CONTENT_AUDIT_LAGEN_PROVIDER="${CONTENT_AUDIT_LAGEN_PROVIDER:-openai}"
export CONTENT_AUDIT_LAGEN_ENDPOINT="${CONTENT_AUDIT_LAGEN_ENDPOINT:-https://api.openai.com/v1}"
export CONTENT_AUDIT_LAGEN_MODEL="${CONTENT_AUDIT_LAGEN_MODEL:-gpt-4o-mini}"
# Map your existing OPENAI_API_KEY into the var the resolver reads, unless already set.
export CONTENT_AUDIT_LAGEN_API_KEY="${CONTENT_AUDIT_LAGEN_API_KEY:-${OPENAI_API_KEY:-}}"

# --- optional: request budget for "ask for more lemmas" (defaults to 5 if unset) ---
# export CONTENT_AUDIT_LAGEN_MAX_LEMMA_REQUESTS=5

echo "[interactive] mode=$CONTENT_AUDIT_LAGEN_MODE provider=$CONTENT_AUDIT_LAGEN_PROVIDER model=$CONTENT_AUDIT_LAGEN_MODEL endpoint=$CONTENT_AUDIT_LAGEN_ENDPOINT key=$([ -n "$CONTENT_AUDIT_LAGEN_API_KEY" ] && echo set || echo MISSING)" >&2

exec "$HERE/audit-cli.sh" "$@"
