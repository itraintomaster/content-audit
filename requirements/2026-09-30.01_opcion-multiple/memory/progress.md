# Progress — FEAT-OPMUL

2026-09-30 — analyst — REQUIREMENT.md creado a posteriori: documenta el soporte de opcion multiple de 83228e54 (entro sin flujo Sentinel, por decision de José). 8 reglas (R001-R008), 3 journeys de flujo (J001 auditar, J002 correccion rechazada, J003 guardar/revisar/reparar) y 5 doubts OPEN. `requirement validate` y `feature validate` [OK]; `feature list` = FEAT-OPMUL 8 reglas / 3 journeys.
  why: José pidio respetar el diseño del proyecto: primero el requirement, despues el arquitecto lo declara en sentinel.yaml.

2026-09-30 — analyst — Cifras verificadas sobre db/english-course del worktree (0bc29a52): 886 archivos; 11.287 ejercicios = 8.477 CLOZE + 2.810 MC; los 2.810 con oracion guardada y con formCloze; una sola opcion correcta cada uno; 0 con "selection": null; 60 MC en 3 knowledges REWRITE de B1, 10 con oracion previa al hueco. Tomadas del brief 022 sin re-medir: 886/886 identicos, 73,9 %, 4.036 tareas (841 sobre MC) y los 718 CLOZE.

2026-09-30 — analyst — Pendiente, fuera del alcance del analista: FEAT-OPMUL no esta en sentinel.yaml (`feature status` = "not found"); falta registrar la carpeta y la feature, y recien entonces `feature sync --feature FEAT-OPMUL`.
  why: el analista no edita sentinel.yaml; `feature sync` escribe en el.

2026-09-30 — analyst — Proximo paso: (a) José valida las reglas (hoy todas AUTO_VALIDATED) y resuelve las 5 doubts; (b) @architect declara el modelo MC, los datos no interpretados y los desenlaces MULTIPLE_CHOICE_UNSUPPORTED sobre los 26 archivos @Generated editados a mano; (c) @qa-tester traza los 39 tests MC existentes a R001-R008 y ubica J001-J003.

2026-09-30 — architect — Deriva medida sin escribir en el worktree (verify y analyze sobre copias en scratch: verify puede reescribir sentinel.yaml por inferAndUpdate y analyze reescribe .sentinel/implementation.sentinel.yaml, que esta versionado). verify = 10 derivas: los 8 modelos/enums @Generated, requirements/MEMORY.md y .sentinel/sentinel-jar.path. analyze = sin declarar, del MC, solo FormKind, MultipleChoiceEntity y MultipleChoiceItemEntity.

2026-09-30 — architect — Simulacion de generate en memoria, con la libreria del jar y sin tocar el worktree: con el sentinel.yaml de HOY ya rompe el build. Reescribe los 8 modelos y el smart-merge rellena 5 llamadas a constructores de copia en 4 implementaciones (`new FormEntity(base, 0.0, null, null, null)`, no compila). Las otras 15 implementaciones quedan intactas.
  why: generate preserva los cuerpos de las implementaciones, pero padStaleConstructorCalls toma un constructor de copia por un constructor completo viejo.

2026-09-30 — architect — `patch propose` OK (3 altas, 11 modificaciones, 0 conflictos): architectural_patch.yaml + sentinel-baseline.yaml. `tech-spec write` OK (8 fences): TECH_SPEC.md. No aplicado.

2026-09-30 — architect — Con el parche (simulado): verify deja de marcar los 3 enums y marca 25 derivas (FormEntity, QuizTemplateEntity, AuditableQuiz, QuizInstructionSubjectView, RunReport por 1 comentario, 2 tests generados y docs administradas). generate real aborta por J001-J003 sin testModule; si corriera, tocaria 8 implementaciones (4 sin compilar) y dejaria unas 420 llamadas de tests con aridad vieja.

2026-09-30 — architect — Proximo paso: José (1) aplica el parche y corre verify y mvn test, sin generate; (2) decide como cerrar la brecha de los 4 modelos y de las 5 llamadas a constructores de copia; (3) recien despues @qa-tester ubica J001-J003 y traza los 39 tests MC; (4) generate y verificacion completa.

2026-09-30 — qa-tester — patch propose OK sobre esta carpeta, fusionado con el del arquitecto (3 altas, 23 modificaciones, 0 conflictos; `patch validate` igual). Aporte: 32 handwrittenTests en 12 implementaciones y J001-J003 ubicados. No aplicado.
  why: José eligio la opcion A (adaptar el codigo al generador), asi que tests y ubicacion van en el mismo parche para aplicar una sola vez.

2026-09-30 — qa-tester — Simulacion en memoria de apply + generate, escribiendo solo en scratch: los 32 tests aterrizan, J001-J003 quedan ubicados, `feature status` OK, 8/8 reglas [covered]. generate crearia 3 journey tests (12 caminos) y 32 stubs en 12 {Impl}Test existentes, sin choques de nombre.

2026-09-30 — qa-tester — Censo (JavaParser) de tests que rompe generate: 419 llamadas en 83 archivos. Son 288 AuditableQuiz 9->10, 82 FormEntity 5->7, 32 QuizTemplateEntity 21->22, 7 QuizInstructionSubjectView 6->7, 5 constructores de copia y 5 formKind(). De ellas, 4 se regeneran solas (FileSystemCourseRepositorySentinelTest) y quedan 415 a mano en 82 archivos; en main hay 20.

2026-09-30 — qa-tester — Al aplicar aparece un aviso esperado: "Rule F-OPMUL-R00x has no test coverage". No bloquea.
  why: TestCoverageValidator solo cuenta tests declarativos de implementaciones de raiz; la cobertura real la da `tool listModules --with-coverage`.

2026-09-30 — qa-tester — Proximo paso: José resuelve las dudas de traza (OG1) y aplica sin --as; despues corre generate. El equipo de implementacion adapta el codigo y los 415 sitios. @test-writer porta 27 cuerpos, borra los originales y escribe los 5 nuevos y los 12 caminos. Meta: ~1.713 tests en verde (1.696 + 5 + 12).

2026-09-30 — analyst — F-OPMUL-R009 agregada (brief 092, pregunta 3). `requirement validate`, `feature validate` y `glossary validate` [OK]; `feature list` = FEAT-OPMUL 9 reglas / 3 journeys. Sin `feature sync`: solo se agrego una regla y FEAT-OPMUL todavia no esta en sentinel.yaml.

2026-09-30 — qa-tester — patch propose OK, fusionado (3 altas, 23 modificaciones, 0 conflictos; `patch validate` igual): +2 handwrittenTests para R009, quedan 34 (28 portados, 6 nuevos) y 11 tests MC comunes. `tech-spec write` OK: 9 fences (+1, seccion R009). No aplicado.

2026-09-30 — qa-tester — Simulacion en scratch (apply en memoria con la libreria del jar, sin CLI apply ni generate): los 34 tests aterrizan, J001-J003 ubicados, `feature status` OK, 9/9 reglas [covered]; generate crearia 34 stubs en 12 {Impl}Test existentes, sin choques de nombre. El aviso de TestCoverageValidator ahora lista R001-R009.

2026-09-30 — qa-tester — Proximo paso, sin cambios: José aplica sin --as y corre generate. @test-writer porta 28 cuerpos (quizFormKindFallsBackToQuizKind, por revise) y borra los originales, y escribe los 6 nuevos y los 12 caminos. Meta: ~1.714 tests en verde (1.696 + 6 + 12).

2026-09-30 — analyst — Por revision del coordinador, R009 pasa a AUTO_VALIDATED: José acepto la recomendacion, no confirmo el texto.

2026-09-30 — qa-tester — Por revision del coordinador: la descripcion del parche se reescribio con `patch propose --replace` (el resto del parche queda byte a byte: 9 reglas, 34 tests, 28 portados, 6 nuevos). TECH_SPEC: «Registrar FEAT-OPMUL y ubicar sus tres journeys» al dia y un parrafo sobre las 34 pruebas; quedan 11 comunes (39 - 28), no 12, porque quizFormKindFallsBackToQuizKind paso a portado.

2026-09-30 — developer — Opcion A aplicada: lo que generate borro de FormEntity y QuizTemplateEntity (constructores de copia, formKind()) vive en FormEntities y QuizTemplateEntities, escritas a mano en course-domain; 9 formKind() y 5 copias migradas, compila (50c68001). FormKind.from y correctItem() no se perdieron.
2026-09-30 — developer — 405 llamadas de tests en 81 archivos pasadas a las aridades nuevas con null, el valor que dejaba el constructor viejo (f3962d68).
2026-09-30 — test-writer — 28 pruebas portadas a su {Impl}Test y originales borrados (f55dfb66); 6 nuevas (70acda33); J001-J003 con 12 caminos (6247e06a). Verde: 1.714 tests, ArchUnit incluido.
2026-09-30 — developer — Control sobre copias en scratch: analyze sin juez da 73,9 % y 96,5 · 94,2 · 79,5 · 63,5; el plan, 4.036 tareas (841 MC), identico al plan-2026-09-30T11-54-12 de main. `sentinel verify`: VERIFY OK, sin drift.
2026-09-30 — developer — Proximo paso: revision del orquestador y OK de José para llevar la rama a main.
