#!/bin/bash
# Piloto de la versión nueva de Didáctica A1: cuatro exámenes, uno por vez, fuera de la campaña real.
# Espera la señal $P/listo (la base integrada y el tablero reiniciado) para no pisar otra medición paga.
P=/private/tmp/claude-501/-Users-josecullen-projects-learney-content-audit--claude-worktrees-iterative-correction-improvements-1c3d2c/7aeb7923-ce6d-404e-87c0-0be9207254b5/scratchpad/piloto-a1d
W=/Users/josecullen/projects/learney/content-audit/.claude/worktrees/iterative-correction-improvements-1c3d2c
M=/Users/josecullen/projects/learney/content-audit
while [ ! -f "$P/listo" ]; do sleep 60; done
export CONTENT_AUDIT_JUDGE_PROVIDER=codex-cli CONTENT_AUDIT_JUDGE_MODEL=gpt-6.1-sol \
  CONTENT_AUDIT_QINST_JUDGE_PROVIDER=codex-cli CONTENT_AUDIT_QINST_JUDGE_MODEL=gpt-6.1-sol \
  CONTENT_AUDIT_LAGEN_PROVIDER=codex-cli CONTENT_AUDIT_LAGEN_MODEL=gpt-6.1-sol CONTENT_AUDIT_LAGEN_ENDPOINT=codex-cli \
  CONTENT_AUDIT_MODEL=gpt-6.1-sol CONTENT_AUDIT_REASONING_EFFORT=medium
correr() { # version foto casos
  local out="$P/salida-$1-$2.jsonl"
  [ -s "$out" ] && { echo "$(date +%T) ya estaba $out"; return; }
  rm -f "$out" "$P/metricas-$1-$2.json"
  local n; n=$(python3 -c "import json;print(len(json.load(open('$P/casos-$2.json'))))")
  echo "$(date +%T) empieza $1 $2 ($n casos)"
  env -C "$M" "$W/audit-cli.sh" --workdir "$P/wd-$1" exam run a1-didactic --cases "$P/casos-$2.json" \
    --course "$P/foto-$2" --budget "$n" --concurrency 4 --out "$out" --metrics-out "$P/metricas-$1-$2.json" \
    > "$P/log-$1-$2.txt" 2>&1
  echo "$(date +%T) termina $1 $2 exit=$? $(tail -3 "$P/log-$1-$2.txt" | tr '\n' ' ')"
}
correr nueva hoy
correr nueva base
correr vieja hoy
correr vieja base
echo "$(date +%T) fin"
