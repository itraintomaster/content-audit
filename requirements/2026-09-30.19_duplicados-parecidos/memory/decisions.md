# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer — «Pide lo mismo» compara todos los huecos (el prototipo, sólo el primero) con
  CorrectorMatcher.sameAnswer, también tapando pronombres y demostrativos de la respuesta (todos, como el prototipo,
  no sólo los copiados del enunciado). Contra pares.json: 24.800 de 24.889 (89 de varios huecos con otro segundo hueco)
  y 13 nuevos («'re going to» = «are going to»). El comodín es de texto, sin spaCy (lema por raíz): 24 de 24 y uno más
  (…412e/…415f, they → you). Candidatos de claude -p: 696 de 699.
2026-10-01 — developer — El plan del runtime va por tema (unidades KNOWLEDGE): un conjunto declarado y el examen traen
  temas enteros, porque el par necesita a los dos. Vector sólo para los ejercicios de algún par que el comodín no
  resuelve. Queda el primero del tema con el que es casi igual; un par anterior sin respuesta deja la copia a medias.

2026-10-01 — coordinador — El doble hallazgo de …2260 en Práctica mixta es lo esperado: cada analizador dice su
  relación —copia exacta de …224b en exact-duplicates, casi igual a …2231 en near-duplicates— y la mesa los agrupa.
  La prueba de R001 que lo fija queda como está. Responde la pregunta del qa-tester del 1/10.

2026-10-01 — qa-tester — F-DUPAR-J001 en audit-domain, `com.learney.contentaudit.auditdomain.nearduplicates`: el
  analizador y sus piezas son package-private ahí y los puertos se simulan, como F-DUPEX-J001.
2026-10-01 — qa-tester — «Si lo encuentran el comodín y el vector, gana el comodín» (decisión del arquitecto) se prueba bajo
  R004, que fija el costo por la regla que lo encontró. «Con nomic-embed-text nunca se consulta a claude -p», bajo R008.
2026-10-01 — qa-tester — Para el analista: en Práctica mixta, …2260 es copia exacta de …224b (exact-duplicates) y además
  casi igual a …2231 por el comodín: con R001 inv. 3 y R003 lleva dos hallazgos, uno de cada analizador. Es lo que cuenta
  el prototipo (44 copias); la prueba lo sigue. ¿Es lo esperado o la copia exacta no debería contar acá?

2026-10-01 — integrador — Rebasado sobre ronda1/integracion y FEAT-RONDA2 (propose --replace, tech-spec write y validate en exit 0): sin cambios de estructura. NearDuplicatesCorrection la suma TAREA; NEAR_DUPLICATES es tipo de tarea (F-RONDA2-R008); no entra en la corrida base por su regla de modelo local (F-RONDA2-R003, B-10).

2026-09-30 — architect — Las dos rutas abren por `JudgeRuntime`: un vector por ejercicio, o un juicio por par
  (`SameExerciseQuestion`: los dos ejercicios de `QuizStudentViewRenderer`; veredicto {sameExercise, reason}). onKnowledge
  arma pares, comodín y pedidos; onCourseComplete resuelve todo junto y decide. Regla `same-exercise` (modelo pago),
  condición JUDGED; el comodín corre siempre; lo que falta queda a medias (`partiallyEvaluatedBy`). Candidatos con
  `wordSimilarity` ≥ `candidateWordSimilarity()`, 0,6. El proveedor ya no recibe la fábrica de sesiones.

2026-09-30 — analyst — Nueva R008: sin Ollama, candidatos = pares de R001 que el comodín no resuelve con parecido de
  palabras ≥ 0,6 (cambian como mucho 4 de cada 10); claude -p juzga si es el mismo ejercicio; el comodín corre siempre; sin
  tope, a medias (F-RONDA1-R006). Dudas OPEN con A aplicada: DOUBT-DUPAR-PARECIDO-TEXTO (0,6) y DOUBT-DUPAR-A-MEDIAS.
  why: contado con `arq-B2/candidatos_dup.py` y `umbral_texto.py`: 699 candidatos (A1 276, A2 110, B1 299, B2 14), los 7
  casi iguales no exactos del 077 (escribir 6, examinar 1) y 14 de los 21 del modelo local.

2026-09-30 — architect — Usa dos piezas de la ronda 1 (A) sin declarar otras: `CorrectorMatcher.sameAnswer` para «la misma
  respuesta» y para descartar los exactos, y `QuizStudentViewRenderer` (A-01) para el ejercicio terminado y la evidencia. Se
  aplica después de integrar la ronda 1.
  why: decisión 1 de José, una sola tabla de equivalencias; y así ningún par cuenta en exact-duplicates y acá a la vez.

2026-09-30 — architect — Compara en `onKnowledge` contra los anteriores del tema, como exact-duplicates; el primero con el que es
  casi igual queda. Vectores en el libro de evaluaciones con la versión examinada en el id (hace falta el índice del gancho B-09:
  46 MB). Si se cumplen las dos condiciones gana el comodín. Sin modelo o sin examen, el analizador entero queda sin evaluar.

2026-09-30 — analyst — José, 30/9: DOUBT-DUPAR-CUENTA → A (error de gravedad baja, como los exactos) y DOUBT-DUPAR-UMBRAL → A
  (0,95). R002 y R004 pasan a VALIDATED.

2026-09-30 — analyst — Código FEAT-DUPAR / F-DUPAR y nombre `near-duplicates` (estilo de `exact-duplicates`): nuevos.

2026-09-30 — analyst — Condición previa: misma respuesta (como el corrector, contraída = larga, sin el pronombre o
  demostrativo copiado del enunciado) y no exactos (R001).
  why: sin ella los pares más parecidos son contrastes a propósito («observed him clean» / «cleaning», coseno 0,995;
  «I am great» / «You are great»); con ella quedan 45 pares y los 6 casi iguales del 077 en escribir.

2026-09-30 — analyst — Dos caminos (R002): comodín para pronombres, demostrativos, posesivos y tiempo del auxiliar (24
  pares, 5 de 6 del 077) o parecido ≥ 0,95 con nomic-embed-text (21 pares más, incluido Rome / visited).
  why: el comodín solo no ve la misma idea con otro verbo; el modelo solo marca también cambios de sustantivo.

2026-09-30 — analyst — Mismo criterio de copia que FEAT-DUPEX (el primero del tema queda; n−1 hallazgos) y gravedad baja,
  mesa, que cuenta como error [ASSUMPTION, DOUBT-DUPAR-CUENTA].
  why: el 077 los contó como duplicados; la propuesta decía «para revisar».

2026-09-30 — analyst — Examen propuesto (R007): los casi iguales del 077 en las dos mitades, una muestra ciega de 40 + 40
  pares revisada por una persona y la estabilidad entre corridas.
  why: el 077 no tiene categoría propia; son 6 de los 15 de escribir/duplicados.json, pocos para medir precisión.
