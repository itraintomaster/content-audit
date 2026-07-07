# Fix Log

Fixes de implementación/tests que funcionaron, con el porqué no obvio.
Entrada más reciente arriba.

<!-- entries below -->

2026-07-07 — developer — Falsos rojos en audit-cli (MainTest, ReviseCmdTest, FRevaprJ004/J005,
  FKtlrJ001/J002) causados por jar STALE de revision-infrastructure en ~/.m2, no por el wiring.
  - Causa raiz: `mvn test -pl audit-cli` (sin `-am`) resuelve `revision-infrastructure` desde el
    repo local instalado, no desde el source del reactor. El jar instalado era anterior al paquete
    `knowledgetitleagent` → NoClassDefFoundError al cargar Main.class reflectivamente en MainTest,
    y en cascada ReviseCmdTest/FRevaprJ004/J005/FKtlrJ001/J002 (todos dependen de que Main cargue).
  - Fix: reinstalar en orden refiner-domain → revision-domain → revision-infrastructure con
    `mvn install -pl <mod> -Dmaven.test.skip=true -Dexec.skip=true` (revision-domain/refiner-domain
    sin problema); para revision-infrastructure el goal `install` dispara `verify` → PIT → falla
    por el mismatch JDK25/JDK24 conocido. Workaround: `mvn package -pl revision-infrastructure
    -Dmaven.test.skip=true -Dexec.skip=true` (package no encadena verify) + instalar el jar a mano
    con `org.apache.maven.plugins:maven-install-plugin:3.1.1:install-file
    -Dfile=target/revision-infrastructure-0.0.1.jar -DpomFile=pom.xml`.
  - Tras esto: MainTest, ReviseCmdTest, FRevaprJ004/J005, FKtlrJ001/J002 → todos verdes. No se tocó
    ningún test ni Main.java para "arreglar" esto — el wiring ya era correcto.
  - why: confirma que el patron LAPS de reinstalar dependencias en cadena (memoria de sesiones
    anteriores) también aplica a revision-infrastructure, con el paso extra de rodear pit vía
    `package` en vez de `install` directo.
