#!/usr/bin/env bash
set -euo pipefail

repo_root="${CONTENT_AUDIT_REPO_ROOT:-/Users/josecullen/projects/learney/content-audit}"
campaign_root="${CONTENT_AUDIT_WORKDIR:-$repo_root/.content-audit/campaigns/a1-v11}"
run_root="${CONTENT_AUDIT_RUN_ROOT:-/Users/josecullen/projects/learney/content-audit-corridas/2026-10-06-evaluacion-ampliada-v1}"
batch_id="${1:?uso: monitor-correction-run.sh BATCH_ID ACTIVITY_ID START_STAMP [TOTAL]}"
activity_id="${2:?falta ACTIVITY_ID}"
start_stamp="${3:?falta START_STAMP, por ejemplo 2026-10-07T05:53:00}"
total="${4:-1000}"

batch_file="$campaign_root/.content-audit/batches/$batch_id.json"
calls_log="$run_root/fakebin/codex-llamadas.log"
activity_script="$repo_root/analysis/activity_run.py"

while [[ -f "$batch_file" ]]; do
  batch_status="$(jq -r '.status' "$batch_file")"
  calls="$(awk -v start="$start_stamp" '$1 >= start { count++ } END { print count + 0 }' "$calls_log")"
  (( calls > total )) && calls="$total"
  panel="$(jq -r '(.panelDecisions // []) | length' "$batch_file")"
  changes="$(jq -r '(.changes // []) | length' "$batch_file")"
  resolved="$(jq -r '[.outcomes[]? | select(.kind == "RESOLVED_BY_PANEL")] | length' "$batch_file")"
  no_winner="$(jq -r '[.outcomes[]? | select(.kind == "NO_WINNER")] | length' "$batch_file")"
  summary="Mesa en curso: $calls consultas iniciadas; $panel decisiones de panel, $resolved tareas resueltas y $changes propuestas reversibles. Sin ganador: $no_winner. Cero publicación."
  next="Continuar hasta el tope duro de $total o hasta que se agote el trabajo del tramo."
  python3 "$activity_script" update --workdir "$repo_root" --id "$activity_id" \
    --status RUNNING --completed "$calls" --summary "$summary" --next "$next" >/dev/null
  [[ "$batch_status" == "RUNNING" ]] || break
  sleep 15
done
