#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "Running all tests..."
mvn -f "$SCRIPT_DIR/pom.xml" test -Dexec.skip=true -q
echo "All tests passed."
