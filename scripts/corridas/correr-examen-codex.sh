#!/usr/bin/env zsh
set -euo pipefail

# Lanzador para una copia de examen. Por omisión bloquea cualquier consulta;
# MODO=con se habilita únicamente después de aprobar el presupuesto.
R="$(cd "$(dirname "$0")" && pwd)"
C="$R/content-audit"
W="${WORKDIR:-$C}"
JAVA_HOME="${JAVA_HOME:-/Users/josecullen/.sdkman/candidates/java/25.0.2-open}"
MODEL="${CONTENT_AUDIT_MODEL:-gpt-6.1-sol}"
EFFORT="${CONTENT_AUDIT_REASONING_EFFORT:-medium}"
MODS=(audit-domain course-domain refiner-domain audit-application course-infrastructure audit-cli nlp-infrastructure vocabulary-infrastructure audit-infrastructure revision-domain revision-infrastructure evaluation-ledger-domain evaluation-ledger-infrastructure agent-runtime-infrastructure quiz-instruction-infrastructure local-language-infrastructure)

[[ $# -ge 1 ]] || { echo "Uso: [MODO=con] correr-codex.sh <nombre> [args de Main]" >&2; exit 2; }
NAME="$1"
shift
ACTIVITY_SCRIPT="$C/analysis/activity_run.py"
ACTIVITY_ID="${CONTENT_AUDIT_ACTIVITY_ID:-$(date +%Y-%m-%dT%H-%M-%S)-$NAME}"
CP=""
for m in $MODS; do
  [[ -d "$C/$m/target/classes" ]] && CP="$CP:$C/$m/target/classes"
done
CP="${CP#:}:$(<"$R/cp-reactor.txt")"

mkdir -p "$R/fakebin" "$R/logs"
FAKEBIN="$(mktemp -d "$R/fakebin.XXXXXX")"
readonly FAKEBIN
cleanup_fakebin() {
  /bin/rm -f "$FAKEBIN/codex" || true
  /bin/rmdir "$FAKEBIN" 2>/dev/null || true
}
trap cleanup_fakebin EXIT

if [[ "${MODO:-sin}" == con ]]; then
  CODEX_REAL="${CONTENT_AUDIT_CODEX_REAL_BINARY:-$(command -v codex || true)}"
  [[ -x "$CODEX_REAL" ]] || CODEX_REAL=/Applications/Codex.app/Contents/Resources/codex
  [[ -x "$CODEX_REAL" ]] || { echo "No se encontró el ejecutable real de Codex" >&2; exit 1; }
  # El shim es privado; el log común conserva su ruta histórica.
  printf '#!/bin/sh\necho "$(date +%%FT%%T) $*" >> %s/fakebin/codex-llamadas.log\nCONTENT_AUDIT_CODEX_REAL_BINARY=%s exec %s/scripts/codex-runtime/codex "$@"\n' \
    "$R" "$CODEX_REAL" "$C" > "$FAKEBIN/codex"
else
  # El shim es privado; el log común conserva su ruta histórica.
  printf '#!/bin/sh\necho "Codex bloqueado: este paso corre con MODO=sin" >&2\necho "$(date +%%FT%%T) $*" >> %s/fakebin/codex-intentos-bloqueados.log\nexit 97\n' \
    "$R" > "$FAKEBIN/codex"
fi
chmod +x "$FAKEBIN/codex"

cd "$W"
if [[ -f "$ACTIVITY_SCRIPT" ]]; then
  ACTIVITY_START=(start --workdir "$W" --id "$ACTIVITY_ID"
    --title "${CONTENT_AUDIT_ACTIVITY_TITLE:-$NAME}"
    --scope "${CONTENT_AUDIT_ACTIVITY_SCOPE:-Examen de juez}"
    --kind "${CONTENT_AUDIT_ACTIVITY_KIND:-JUDGE_EXAM}"
    --provider codex-cli --model "$MODEL" --reasoning-effort "$EFFORT")
  [[ -n "${CONTENT_AUDIT_ACTIVITY_VERSION:-}" ]] && ACTIVITY_START+=(--version "$CONTENT_AUDIT_ACTIVITY_VERSION")
  [[ -n "${CONTENT_AUDIT_ACTIVITY_TOTAL:-}" ]] && ACTIVITY_START+=(--total "$CONTENT_AUDIT_ACTIVITY_TOTAL" --unit "${CONTENT_AUDIT_ACTIVITY_UNIT:-consultas}")
  python3 "$ACTIVITY_SCRIPT" "${ACTIVITY_START[@]}" >/dev/null || echo "No se pudo publicar el inicio de la actividad" >&2
fi
set +e
env -i HOME="$HOME" USER="$USER" LOGNAME="${LOGNAME:-$USER}" LANG="${LANG:-en_US.UTF-8}" \
  TMPDIR="${TMPDIR:-/tmp}" SHELL=/bin/zsh \
  PATH="$FAKEBIN:/Users/josecullen/.local/bin:/opt/homebrew/Caskroom/miniconda/base/bin:/usr/bin:/bin:/usr/sbin:/sbin:/opt/homebrew/bin" \
  JAVA_HOME="$JAVA_HOME" CONTENT_AUDIT_MODEL="$MODEL" CONTENT_AUDIT_REASONING_EFFORT="$EFFORT" \
  CONTENT_AUDIT_JUDGE_PROVIDER=codex-cli CONTENT_AUDIT_JUDGE_MODEL="$MODEL" \
  CONTENT_AUDIT_QINST_JUDGE_PROVIDER=codex-cli CONTENT_AUDIT_QINST_JUDGE_MODEL="$MODEL" \
  /usr/bin/time -p "$JAVA_HOME/bin/java" -Xmx8g -cp "$CP" \
  com.learney.contentaudit.auditcli.commands.Main --workdir "$W" "$@" \
  > "$R/logs/$NAME.out" 2> "$R/logs/$NAME.err"
RC=$?
set -e
if [[ -f "$ACTIVITY_SCRIPT" ]]; then
  STATUS=FAILED
  SUMMARY="El examen terminó con código $RC."
  [[ "$RC" == 0 ]] && STATUS=NEEDS_REVIEW && SUMMARY="El examen terminó; falta medirlo contra el oro antes de aprobarlo."
  ACTIVITY_FINISH=(update --workdir "$W" --id "$ACTIVITY_ID" --status "$STATUS" --summary "$SUMMARY" --finish)
  [[ "$RC" == 0 && -n "${CONTENT_AUDIT_ACTIVITY_TOTAL:-}" ]] && ACTIVITY_FINISH+=(--completed "$CONTENT_AUDIT_ACTIVITY_TOTAL")
  python3 "$ACTIVITY_SCRIPT" "${ACTIVITY_FINISH[@]}" >/dev/null || echo "No se pudo publicar el cierre de la actividad" >&2
fi
exit "$RC"
