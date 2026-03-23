#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER="$PROJECT_DIR/audit-cli.sh"

echo "Building project..."
mvn -f "$PROJECT_DIR/pom.xml" install -DskipTests -Dexec.skip=true -q

echo "Resolving classpath..."
CLASSPATH=$(mvn -f "$PROJECT_DIR/audit-cli/pom.xml" \
  dependency:build-classpath -q \
  -DincludeScope=runtime \
  -Dmdep.outputFile=/dev/stdout 2>/dev/null)

# Add the compiled classes of each module
for module in audit-cli audit-application audit-domain course-domain course-infrastructure refiner-domain; do
  CLASSPATH="$PROJECT_DIR/$module/target/classes:$CLASSPATH"
done

cat > "$WRAPPER" <<EOF
#!/usr/bin/env bash
exec java -cp "$CLASSPATH" com.learney.contentaudit.auditcli.Main "\$@"
EOF

chmod +x "$WRAPPER"

echo "Done. You can now run:"
echo "  ./audit-cli.sh <path-to-course> [--format text|json]"
echo ""
echo "Example:"
echo "  ./audit-cli.sh db/english-course"
