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
CP=""
for m in $MODS; do
  [[ -d "$C/$m/target/classes" ]] && CP="$CP:$C/$m/target/classes"
done
CP="${CP#:}:$(<"$R/cp-reactor.txt")"

mkdir -p "$R/fakebin" "$R/logs"
if [[ "${MODO:-sin}" == con ]]; then
  CODEX_REAL="${CONTENT_AUDIT_CODEX_REAL_BINARY:-$(command -v codex || true)}"
  [[ -x "$CODEX_REAL" ]] || CODEX_REAL=/Applications/Codex.app/Contents/Resources/codex
  [[ -x "$CODEX_REAL" ]] || { echo "No se encontró el ejecutable real de Codex" >&2; exit 1; }
  printf '#!/bin/sh\necho "$(date +%%FT%%T) $*" >> %s/fakebin/codex-llamadas.log\nCONTENT_AUDIT_CODEX_REAL_BINARY=%s exec %s/scripts/codex-runtime/codex "$@"\n' \
    "$R" "$CODEX_REAL" "$C" > "$R/fakebin/codex"
else
  printf '#!/bin/sh\necho "Codex bloqueado: este paso corre con MODO=sin" >&2\necho "$(date +%%FT%%T) $*" >> %s/fakebin/codex-intentos-bloqueados.log\nexit 97\n' \
    "$R" > "$R/fakebin/codex"
fi
chmod +x "$R/fakebin/codex"

cd "$W"
env -i HOME="$HOME" USER="$USER" LOGNAME="${LOGNAME:-$USER}" LANG="${LANG:-en_US.UTF-8}" \
  TMPDIR="${TMPDIR:-/tmp}" SHELL=/bin/zsh \
  PATH="$R/fakebin:/Users/josecullen/.local/bin:/opt/homebrew/Caskroom/miniconda/base/bin:/usr/bin:/bin:/usr/sbin:/sbin:/opt/homebrew/bin" \
  JAVA_HOME="$JAVA_HOME" CONTENT_AUDIT_MODEL="$MODEL" CONTENT_AUDIT_REASONING_EFFORT="$EFFORT" \
  CONTENT_AUDIT_JUDGE_PROVIDER=codex-cli CONTENT_AUDIT_JUDGE_MODEL="$MODEL" \
  CONTENT_AUDIT_QINST_JUDGE_PROVIDER=codex-cli CONTENT_AUDIT_QINST_JUDGE_MODEL="$MODEL" \
  /usr/bin/time -p "$JAVA_HOME/bin/java" -Xmx8g -cp "$CP" \
  com.learney.contentaudit.auditcli.commands.Main --workdir "$W" "$@" \
  > "$R/logs/$NAME.out" 2> "$R/logs/$NAME.err"
