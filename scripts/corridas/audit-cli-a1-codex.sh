#!/bin/sh
set -eu

# Puente para la app: lee la copia operativa con el entorno Codex declarado,
# pero fuerza el modo sin inferencias aunque una pantalla dispare una acción.
RUN_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
exec "$RUN_ROOT/control-codex.sh" --app "$@"
