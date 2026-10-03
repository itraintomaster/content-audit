# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-10-03 — developer — `mvn -pl audit-domain` solo, sin `-am`, rompe con «cannot find symbol EvaluationRequest» y
  `unavailableReason()` en archivos que nadie tocó (QuizInstructionAnalyzer): usa el evaluation-ledger-domain viejo de
  ~/.m2. Siempre `-pl audit-domain,audit-cli -am`; nunca `mvn install`.
  why: el mensaje apunta a código ajeno y parece un contrato roto, pero es el jar instalado.

2026-10-03 — developer — La prueba «la respuesta del juez no sirve» pedía «quedan 1 par sin juzgar» al final del motivo
  y el motivo de hoy decía «falló la consulta de 1 pares». Los motivos del juez cierran todos con «quedan N pares sin
  juzgar», con N = sin respuesta + fallidos + ilegibles (R009 inv. 4: el informe dice cuántos pares faltan; es la cuenta
  del costo pendiente de la cobertura).
  why: contar sólo los pendientes dejaba afuera el par que fallaba, que la cobertura sí cuenta como pendiente.

2026-10-03 — developer — Verificar el analizador contra el mundo de las pruebas sin tocarlas: una clase en el mismo
  paquete, fuera del repo (scratchpad), compilada contra copias de target/classes y target/test-classes y corrida con el
  directorio de trabajo en audit-domain/ (el corrector lee `../analysis/…`). Sirvió para ver rutas que las pruebas
  todavía no cubrían: Ollama que se cae a mitad de los vectores, sesión de claude -p vencida, vectores que no se pueden
  comparar y el juez sin versión en una corrida de examen.

2026-10-03 — test-writer — Las pruebas de audit-domain se corren sin Maven ni tocar `target/`: javac de main y de test a
  una carpeta aparte y el lanzador de JUnit (junit-platform-launcher 1.10.2 de ~/.m2) con
  `-Duser.dir=<repo>/audit-domain`.
  why: el corrector de las pruebas lee `../analysis/recursos-compartidos/…` relativo al módulo; sin ese directorio de
  trabajo todo `World` falla con NoSuchFileException antes de llegar a ninguna aserción.

2026-10-03 — test-writer — Con la producción a medias (el analizador todavía llama a `similarityThreshold()`), las
  pruebas de audit-domain se verifican en una copia fuera del repo: audit-domain + course-domain + evaluation-ledger-domain
  con el pom raíz sólo con esos tres módulos y, en la copia, `config.similarityThreshold()` → `candidateSimilarity()`.
  why: no se toca producción ni el target compartido con el desarrollador y el otro test-writer; con el JDK 25 los
  «Unsupported class file major version 69» de JaCoCo sobre las clases de Mockito son ruido, no fallan nada.

2026-10-01 — developer — Dos `exam run` en la misma carpeta no son dos corridas: el examen guarda sus vectores en
  `.content-audit/evaluations/exam_…` y el segundo los reusa. Para «dos corridas marcan las mismas copias», cada una en
  su carpeta de estado (`WORKDIR=… correr.sh`) y sin libro.
2026-10-01 — developer — El libro guarda un vector por texto (contentFingerprint): un ejercicio con el mismo ejercicio
  terminado que otro no tiene renglón propio (…2405); al leer vectores del libro, saltear esos pares.

2026-10-01 — developer — El ejercicio terminado del renderer (FILL) es igual al del prototipo en los 11.287
  sólo después de pegar contracciones y signos y sacar el «-» de la respuesta que no dice nada (813 → 331 → 0).

2026-09-30 — analyst — nomic-embed-text pide un prefijo de tarea; se midió con «clustering: ». Sin misma respuesta, el
  coseno solo no separa: la mediana de los pares del mismo tema es 0,66 y el p99,9 0,92.
