#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER="$PROJECT_DIR/audit-cli.sh"
ALIAS_NAME="content-audit"
RC_FILE="$HOME/.zshrc"

echo "Compilando el reactor y resolviendo el classpath, sin instalar artefactos..."
bash "$PROJECT_DIR/scripts/refresh-audit-cli-classpath.sh"

chmod +x "$WRAPPER"

# The wrapper itself is stable: it reads the reactor and cached external dependencies, so adding
# a module no longer requires embedding another long classpath.
if grep -q "alias $ALIAS_NAME=" "$RC_FILE" 2>/dev/null; then
    echo "Actualizando el alias existente en $RC_FILE..."
    sed -i '' "s|alias $ALIAS_NAME=.*|alias $ALIAS_NAME='$WRAPPER'|g" "$RC_FILE"
else
    echo "Registrando el alias en $RC_FILE..."
    {
        echo ""
        echo "# Content Audit CLI"
        echo "alias $ALIAS_NAME='$WRAPPER'"
    } >> "$RC_FILE"
fi

echo ""
echo "Listo. Ejecutá 'source ~/.zshrc' o reiniciá la terminal y usá:"
echo ""
echo "  content-audit db/english-course"
echo "  content-audit db/english-course --level A1"
echo "  content-audit db/english-course --level A1 -f table"
echo "  content-audit --help"
