# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-09-30 — analyst — José aprobó el diseño de FEAT-HALL y eligió la recomendación en las seis dudas: A en
  DOUBT-EJERCICIO-JUZGADO, DOUBT-HALLAZGO-DE-TEMA, DOUBT-GRAVEDAD-EXISTENTES, DOUBT-FAMILIA-OTRO-NIVEL y
  DOUBT-STATS-DE-LOS-JUECES, y «no por ahora» en el cajón tipado (duda 3 de la ficha). Detalle en decisions.md.
  REQUIREMENT: las cinco dudas RESOLVED (2026-09-30), R008 sin [ASSUMPTION] y el cajón anotado en el Alcance. FICHA:
  `dudas: 0` y la sección 7 con lo decidido. `requirement validate` → [OK], con la misma salida que antes (44 avisos
  del glosario compartido y «Lemma» sin enlace).
  Next: aplicar este parche recién después del de FEAT-OPMUL (0.2), revalidándolo contra el sentinel.yaml nuevo; sumar
  la carpeta a `definitions` (el parche no la suma), `feature sync --feature FEAT-HALL` (J001, J002), `sentinel generate`,
  @qa-tester y @developer. La promoción al glosario que esperaba la aprobación ya se puede hacer; acá no se tocó
  domain-glossary.yaml.

2026-09-30 — architect — Diseño de FEAT-HALL propuesto, sin aplicar: `architectural_patch.yaml` (validado: 16 altas, 16 cambios, 1 baja, 0 conflictos;
  `patch validate` también OK), `TECH_SPEC.md` (16 fragmentos, todos subconjunto del parche) y `FICHA.md` (3 dudas para José).
  El parche registra `features: FEAT-HALL` (sin journeys). No toca el modelo del curso ni los elementos del parche de 0.2 (FEAT-OPMUL).
  Next: José aprueba y contesta; el integrador aplica después de 0.2 y revalida; luego `feature sync --feature FEAT-HALL`
  (J001, J002), `sentinel generate`, @qa-tester y @developer (cambios de cuerpo listados en decisions.md).

2026-09-30 — analyst — REQUIREMENT.md v1 de FEAT-HALL (paquete 0.3 del brief 087 (hoy 089)): 15 reglas, 2 journeys, 5 dudas.
  Validación: `sentinel requirement validate --file requirements/2026-09-30.02_contrato-del-hallazgo` → [OK]; único aviso
  propio: el término de glosario «Lemma» sin enlace (sale de los nombres `lemma-*`, no bloquea).
  Next: José aprueba el contrato y contesta las 5 dudas. Queda para quien integre, porque tocan sentinel.yaml y acá estaba
  prohibido: sumar la carpeta a `definitions`, correr `feature sync --feature FEAT-HALL` (registra J001 y J002) y el
  diseño de @architect. Después, @qa-tester.
  Glosario: promover «Hallazgo», «Gravedad», «Familia de analizadores» y «Tema» (= knowledge) cuando José apruebe; no
  se tocó domain-glossary.yaml para no chocar con los paquetes en paralelo.

2026-09-30 — qa-tester — Diseño de pruebas de FEAT-HALL: 101 handwrittenTests en 35 implementaciones, las 15 reglas con prueba, ninguna portada (ningún test existente verifica una regla del contrato, que no está en el código). J001 y J002 en audit-cli, `com.learney.contentaudit.auditcli.commands` (4 + 2 caminos). `patch propose` 16 altas, 36 cambios, 1 baja, 0 conflictos; `tech-spec write` 18 fences; `patch validate` OK contra el sentinel.yaml de hoy y contra el de después de 0.2.
  why: el permiso le negó al qa-tester escribir en esta carpeta; lo escribió José el 30/9 corriendo el script del qa-tester (propose con fusión, domainLinks restaurados, --replace y tech-spec), idéntico byte a byte a lo ensayado.

2026-09-30 — qa-tester — Simulación en memoria (librería del jar) de aplicar 0.2 y después 0.3: sin conflictos (0.2: 3/26/0/0; 0.3 encima: 16/38/1/0), y generate no aborta (DefinitionValidator, traza y ubicación OK). Con el parche del arquitecto solo, abortaba por J001 y J002 sin testModule. Los generadores corren sin excepción: 25 modelos, 4 interfaces, 10 implementaciones, 13 clases de test y 5 journeys nuevos; 101 + 34 stubs, ninguno salteado por tags.

2026-09-30 — qa-tester — Próximo paso: aplicar 0.2, después este parche sin --as (trae estructura), y generate. @test-writer escribe las 101 y los 6 caminos; los números salen del análisis 2026-09-30T11-54-02. Adaptar los cuerpos que rompe el cambio de estructura (26 `new DefaultAuditRunner`, 8 `new IAuditEngine`, 2 `new DefaultRefinerEngine`, 25 usos de EvaluationAnalyzerFactory en tests) y la prueba común de FileSystemAuditReportStoreTest que afirma el promedio viejo (choca con R010). Para el analista: extender R012 al tope pedido con un nombre desconocido, y el caso real de R001 da 0,8 (el ejemplo dice 0,75).

2026-09-30 — developer — Código principal del contrato (817a60e7): el motor arma cada analizador por corrida desde su proveedor, en el orden del catálogo; el colector valida el contrato y estampa analizador, nodo, costo e identidad; la calculadora publica vocabulario, puntajes por analizador con sus submétricas y errores por ejercicio; el almacén escribe el resumen en `audit-digests/`; los consumidores del número leen lo publicado. ContentAnalyzer y AuditNode, sin @Generated, a mano (`findingsAt`; `findings`, `numbers`, `unevaluatedBy`).
2026-09-30 — developer — Tests adaptados a la estructura nueva (4cd2c246): 26 `new DefaultAuditRunner`, 8 `new IAuditEngine`, 2 `new DefaultRefinerEngine`, los usos de EvaluationAnalyzerFactory y el promedio viejo de FileSystemAuditReportStoreTest, que ahora lee el número publicado (R010).
2026-09-30 — test-writer — Las 101 pruebas y los 6 caminos de J001-J002 (80265b60, c54a0581, 0a44c639), con casos reales de la base del 29/9: tokens de spaCy, nodos, diagnósticos y plan del análisis 2026-09-30T11-54-02 como fixtures (scripts/fhall_base_fixtures.py). Verde: 1.821 tests (1.714 + 107), ArchUnit incluido.
2026-09-30 — developer — Control sobre copias en scratch: analyze sin juez da 73,9 % y 96,5 · 94,2 · 79,5 · 63,5, con los 11.760 nodos iguales a la base (puntajes, orden de claves y diagnósticos); get audits, get audit y -f raw dan 73,9 (antes 73,4); el plan, 4.036 tareas idénticas tarea por tarea al plan-2026-09-30T11-54-12; el resumen, 6,7 MB (9,9 MB con el juez a presupuesto 0, que declara los 11.287 ejercicios sin evaluar); get analyzers -f json, las 8 fichas completas. `sentinel verify`: VERIFY OK, sin drift; `sentinel analyze`: 690 componentes y los mismos 5 huérfanos que 0.2.
2026-09-30 — developer — Próximo paso: revisión del orquestador y OK de José para llevar la rama a main.
