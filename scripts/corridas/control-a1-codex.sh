#!/usr/bin/env zsh
set -euo pipefail

# Control seguro para una copia operativa A1. Por omisión, cualquier intento de
# consultar Codex falla antes de salir de la máquina. MODO=con sólo se usa luego
# de aprobar un presupuesto explícito.
K="$(cd "$(dirname "$0")" && pwd)"
C="$K/content-audit"
JAVA_HOME="${JAVA_HOME:-/Users/josecullen/.sdkman/candidates/java/25.0.2-open}"
MODEL="${CONTENT_AUDIT_MODEL:-gpt-6.1-sol}"
EFFORT="${CONTENT_AUDIT_REASONING_EFFORT:-medium}"
MODS=(audit-domain course-domain refiner-domain audit-application course-infrastructure audit-cli nlp-infrastructure vocabulary-infrastructure audit-infrastructure revision-domain revision-infrastructure evaluation-ledger-domain evaluation-ledger-infrastructure agent-runtime-infrastructure quiz-instruction-infrastructure local-language-infrastructure)

APP=0
if [[ "${1:-}" == --app ]]; then
  APP=1
  shift
fi
MODO="${MODO:-sin}"
[[ "$APP" == 1 ]] && MODO=sin

CP=""
for m in $MODS; do
  [[ -d "$C/$m/target/classes" ]] && CP="$CP:$C/$m/target/classes"
done
CP="${CP#:}:$(<"$K/classpath.txt")"

mkdir -p "$K/fakebin" "$K/logs"
if [[ "$MODO" == con ]]; then
  CODEX_REAL="${CONTENT_AUDIT_CODEX_REAL_BINARY:-$(command -v codex || true)}"
  [[ -x "$CODEX_REAL" ]] || CODEX_REAL=/Applications/Codex.app/Contents/Resources/codex
  [[ -x "$CODEX_REAL" ]] || { echo "No se encontró el ejecutable real de Codex" >&2; exit 1; }
  printf '#!/bin/sh\necho "$(date +%%FT%%T) $*" >> %s/fakebin/codex-llamadas.log\nCONTENT_AUDIT_CODEX_REAL_BINARY=%s exec %s/scripts/codex-runtime/codex "$@"\n' \
    "$K" "$CODEX_REAL" "$C" > "$K/fakebin/codex"
else
  printf '#!/bin/sh\necho "Codex bloqueado: este paso corre con MODO=sin" >&2\necho "$(date +%%FT%%T) $*" >> %s/fakebin/codex-intentos-bloqueados.log\nexit 97\n' \
    "$K" > "$K/fakebin/codex"
fi
chmod +x "$K/fakebin/codex"

ENTORNO=(
  HOME="$HOME" USER="$USER" LOGNAME="${LOGNAME:-$USER}" LANG="${LANG:-en_US.UTF-8}"
  TMPDIR="${TMPDIR:-/tmp}" SHELL=/bin/zsh
  PATH="$K/fakebin:/Users/josecullen/.local/bin:/opt/homebrew/Caskroom/miniconda/base/bin:/usr/bin:/bin:/usr/sbin:/sbin:/opt/homebrew/bin"
  JAVA_HOME="$JAVA_HOME"
  CONTENT_AUDIT_MODEL="$MODEL"
  CONTENT_AUDIT_REASONING_EFFORT="$EFFORT"
  CONTENT_AUDIT_JUDGE_PROVIDER=codex-cli
  CONTENT_AUDIT_JUDGE_MODEL="$MODEL"
  CONTENT_AUDIT_QINST_JUDGE_PROVIDER=codex-cli
  CONTENT_AUDIT_QINST_JUDGE_MODEL="$MODEL"
  CONTENT_AUDIT_LAGEN_PROVIDER=codex-cli
  CONTENT_AUDIT_LAGEN_MODEL="$MODEL"
  CONTENT_AUDIT_LAGEN_TIMEOUT=180
)

if [[ "$APP" == 1 ]]; then
  cd "$C"
  exec env -i "${ENTORNO[@]}" \
    CONTENT_AUDIT_CONTENT_FOLDER="${CONTENT_AUDIT_CONTENT_FOLDER:-$C/db/english-course}" \
    "$JAVA_HOME/bin/java" -Xmx8g -cp "$CP" com.learney.contentaudit.auditcli.commands.Main "$@"
fi

[[ $# -ge 1 ]] || { echo "Uso: [MODO=con] control-codex.sh <nombre> [args de Main]" >&2; exit 2; }
NAME="$1"
shift
cd "$C"
env -i "${ENTORNO[@]}" CONTENT_AUDIT_CONTENT_FOLDER="$C/db/english-course" \
  /usr/bin/time -p "$JAVA_HOME/bin/java" -Xmx8g -cp "$CP" \
  com.learney.contentaudit.auditcli.commands.Main --workdir "$C" "$@" \
  > "$K/logs/$NAME.out" 2> "$K/logs/$NAME.err"
