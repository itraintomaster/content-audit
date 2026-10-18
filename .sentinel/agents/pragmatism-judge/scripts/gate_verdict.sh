#!/bin/bash
set -euo pipefail
exec python3 "$(dirname "$0")/verdict.py" gate
