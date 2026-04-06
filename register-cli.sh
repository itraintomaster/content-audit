#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER="$PROJECT_DIR/audit-cli.sh"
ALIAS_NAME="content-audit"
RC_FILE="$HOME/.zshrc"

echo "Building project..."
mvn -f "$PROJECT_DIR/pom.xml" install -DskipTests -Dexec.skip=true -q

echo "Resolving classpath..."
CLASSPATH=$(mvn -f "$PROJECT_DIR/audit-cli/pom.xml" \
  dependency:build-classpath -q \
  -DincludeScope=runtime \
  -Dmdep.outputFile=/dev/stdout 2>/dev/null)

# Add the compiled classes of each module
for module in audit-cli audit-application audit-domain course-domain course-infrastructure nlp-infrastructure refiner-domain; do
  CLASSPATH="$PROJECT_DIR/$module/target/classes:$CLASSPATH"
done

cat > "$WRAPPER" <<EOF
#!/usr/bin/env bash
exec java -cp "$CLASSPATH" com.learney.contentaudit.auditcli.commands.Main "\$@"
EOF

chmod +x "$WRAPPER"

# Register alias in .zshrc
if grep -q "alias $ALIAS_NAME=" "$RC_FILE"; then
    echo "Updating existing alias in $RC_FILE..."
    sed -i '' "s|alias $ALIAS_NAME=.*|alias $ALIAS_NAME='$WRAPPER'|g" "$RC_FILE"
else
    echo "Registering alias in $RC_FILE..."
    echo "" >> "$RC_FILE"
    echo "# Content Audit CLI" >> "$RC_FILE"
    echo "alias $ALIAS_NAME='$WRAPPER'" >> "$RC_FILE"
fi

echo ""
echo "Done! Run 'source ~/.zshrc' or restart your terminal, then use:"
echo ""
echo "  content-audit db/english-course"
echo "  content-audit db/english-course --level A1"
echo "  content-audit db/english-course --level A1 -f table"
echo "  content-audit --help"
