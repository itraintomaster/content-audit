#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
AUDIT_CLI_CLASSES="$PROJECT_DIR/audit-cli/target/classes"
RUNTIME_CLASSPATH_FILE="$PROJECT_DIR/audit-cli/target/runtime-classpath.txt"
CLASSPATH_REFRESHER="$PROJECT_DIR/scripts/refresh-audit-cli-classpath.sh"
CODEX_RUNTIME_DIR="$PROJECT_DIR/scripts/codex-runtime"
CODEX_WRAPPER="$CODEX_RUNTIME_DIR/codex"

resolve_real_codex() {
    local path_entry candidate candidate_dir
    local -a path_entries
    IFS=: read -r -a path_entries <<< "${PATH:-}"
    for path_entry in "${path_entries[@]}"; do
        candidate="${path_entry:-.}/codex"
        if [[ -x "$candidate" ]]; then
            candidate_dir="$(cd -- "$(dirname -- "$candidate")" && pwd -P)"
            candidate="$candidate_dir/$(basename -- "$candidate")"
            if [[ "$candidate" != "$CODEX_WRAPPER" ]]; then
                printf '%s' "$candidate"
                return 0
            fi
        fi
    done
    return 1
}

# sentinel-agents shells out to a binary literally named `codex` and currently has no
# reasoning-effort input. Resolve the user's real CLI before shadowing that name with our thin
# adapter. Non-exec Codex commands remain byte-for-byte pass-through.
REAL_CODEX_BINARY="$(resolve_real_codex || true)"
if [[ -n "$REAL_CODEX_BINARY" ]]; then
    export CONTENT_AUDIT_CODEX_REAL_BINARY="$REAL_CODEX_BINARY"
fi
export PATH="$CODEX_RUNTIME_DIR:$PATH"

needs_classpath_refresh() {
    [[ ! -s "$RUNTIME_CLASSPATH_FILE" ]] && return 0
    [[ ! -d "$AUDIT_CLI_CLASSES" ]] && return 0
    find "$PROJECT_DIR" -name pom.xml -newer "$RUNTIME_CLASSPATH_FILE" -print -quit \
        | grep -q .
}

if needs_classpath_refresh; then
    bash "$CLASSPATH_REFRESHER"
fi

if [[ ! -d "$AUDIT_CLI_CLASSES" ]]; then
    echo "ContentAudit no está compilado: falta $AUDIT_CLI_CLASSES" >&2
    echo "Ejecutá: mvn -pl audit-cli -am -DskipTests compile" >&2
    exit 1
fi

# target/classes always precedes the packaged reactor jars. This makes a normal `mvn compile`
# immediately runnable without installing local modules into ~/.m2 or rebuilding the launcher.
REACTOR_CLASSES=("$AUDIT_CLI_CLASSES")
for classes_dir in "$PROJECT_DIR"/*/target/classes; do
    if [[ -d "$classes_dir" && "$classes_dir" != "$AUDIT_CLI_CLASSES" ]]; then
        REACTOR_CLASSES+=("$classes_dir")
    fi
done

REACTOR_CLASSPATH="$(IFS=:; printf '%s' "${REACTOR_CLASSES[*]}")"
RUNTIME_CLASSPATH="$(<"$RUNTIME_CLASSPATH_FILE")"

exec java -cp "$REACTOR_CLASSPATH:$RUNTIME_CLASSPATH" \
    com.learney.contentaudit.auditcli.commands.Main "$@"
