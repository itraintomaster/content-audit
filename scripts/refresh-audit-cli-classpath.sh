#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT_FILE="$PROJECT_DIR/audit-cli/target/runtime-classpath.txt"
LOG_FILE="$PROJECT_DIR/audit-cli/target/runtime-classpath.log"

mkdir -p "$(dirname "$OUTPUT_FILE")"
TEMP_FILE="$(mktemp "$OUTPUT_FILE.tmp.XXXXXX")"

cleanup() {
    rm -f "$TEMP_FILE"
}
trap cleanup EXIT

# `package` gives Maven real reactor artifact paths under target/. The dependency goal can then
# resolve the complete runtime graph without publishing local modules to ~/.m2. Tests are neither
# compiled nor executed, and Sentinel is no longer part of the Maven lifecycle.
if ! mvn -o -q -f "$PROJECT_DIR/pom.xml" \
        -pl audit-cli -am \
        -Dmaven.test.skip=true \
        package dependency:build-classpath \
        -DincludeScope=runtime \
        -Dmdep.outputFile="$TEMP_FILE" >"$LOG_FILE" 2>&1; then
    cat "$LOG_FILE" >&2
    echo "No se pudo resolver el classpath runtime de ContentAudit." >&2
    exit 1
fi

if [[ ! -s "$TEMP_FILE" ]] \
        || ! grep -q '/local-language-infrastructure/target/' "$TEMP_FILE"; then
    echo "El classpath resuelto está incompleto: falta local-language-infrastructure." >&2
    exit 1
fi

mv "$TEMP_FILE" "$OUTPUT_FILE"
trap - EXIT
