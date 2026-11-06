#!/bin/bash
# La versión nueva otra vez sobre los mismos casos, sin reusar nada: cuánto cambia sola.
P=/private/tmp/claude-501/-Users-josecullen-projects-learney-content-audit--claude-worktrees-iterative-correction-improvements-1c3d2c/7aeb7923-ce6d-404e-87c0-0be9207254b5/scratchpad/piloto-a1d
M=/Users/josecullen/projects/learney/content-audit
export CONTENT_AUDIT_JUDGE_PROVIDER=codex-cli CONTENT_AUDIT_JUDGE_MODEL=gpt-6.1-sol \
  CONTENT_AUDIT_QINST_JUDGE_PROVIDER=codex-cli CONTENT_AUDIT_QINST_JUDGE_MODEL=gpt-6.1-sol \
  CONTENT_AUDIT_LAGEN_PROVIDER=codex-cli CONTENT_AUDIT_LAGEN_MODEL=gpt-6.1-sol CONTENT_AUDIT_LAGEN_ENDPOINT=codex-cli \
  CONTENT_AUDIT_MODEL=gpt-6.1-sol CONTENT_AUDIT_REASONING_EFFORT=medium
for foto in hoy base; do
  n=$(python3 -c "import json;print(len(json.load(open('$P/casos-$foto.json'))))")
  echo "$(date +%T) empieza nueva2 $foto ($n casos)"
  env -C "$M" "$M/audit-cli.sh" --workdir "$P/wd-nueva2" exam run a1-didactic --cases "$P/casos-$foto.json" \
    --course "$P/foto-$foto" --budget "$n" --concurrency 4 --out "$P/salida-nueva2-$foto.jsonl" > "$P/log-nueva2-$foto.txt" 2>&1
  echo "$(date +%T) termina nueva2 $foto exit=$? $(tail -4 "$P/log-nueva2-$foto.txt" | tr '\n' ' ')"
done
echo "$(date +%T) fin"
