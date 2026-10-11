#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "Running all tests..."
# El paso a producción (Python, solo biblioteca estándar, sin red ni Mongo).
(cd "$SCRIPT_DIR" && python3 -I -m unittest discover -s scripts/production_publish/tests -t scripts)
mvn -f "$SCRIPT_DIR/pom.xml" test -Dexec.skip=true -q
echo "All tests passed."
