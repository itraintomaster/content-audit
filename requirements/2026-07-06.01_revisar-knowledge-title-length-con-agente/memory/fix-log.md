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

2026-07-30 — test-writer — FKtlrJ002JourneyTest path-1: testigo de "quiz corregido persistido igual que antes" (R008) usaba `elementAfterQuiz.getTitle()`. Movido a `getSentences()` — el contenido del quiz, no su titulo — siguiendo el mismo criterio ya aplicado en DefaultCourseElementLocatorTest cuando se movio el testigo de staleness de F-KTLR-R007 del titulo al contenido. Agregada asercion complementaria: el titulo queda preservado del elementBefore (F-LAPS-R014/F-RPRES-R004), tambien "exactamente como antes de esta feature".
  why: R008 es una regla de no-regresion sobre el camino de quizzes; el titulo dejo de ser un proxy valido del contenido corregido cuando produccion paso a preservarlo del elementBefore (antes ambos campos cargaban la misma oracion plana, por eso el testigo viejo "funcionaba" por accidente).
