# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-10-06 — José — V11 queda como versión operativa con criterio de estabilidad cuantitativo: formal estricto,
  acuerdo por ejercicio ≥99,5 %, Jaccard de copias ≥98 % y revisión humana de toda divergencia. La igualdad absoluta
  sigue siendo ideal, no una condición que justifique gastar indefinidamente. Las decisiones coincidentes se pueden
  reutilizar; las divergentes no entran al libro operativo hasta revisarlas.
  why: V11 logró 87/87 en el formal, 2.399/2.402 ejercicios iguales y 265/268 copias comunes; los cuatro desacuerdos
  son fronteras pedagógicas finas y toda corrección pasa por la mesa humana.

2026-10-06 — coordinador — V7 exige identificar primero la respuesta seleccionada en `Resuelto:`. Las alternativas y
  pistas no elegidas son distractores; cuando la selección es `-`, el juez compara la decisión de omitir y no atribuye
  al hueco la función de un reflexivo o artículo visible pero descartado. El cambio pasó 29/29 microcasos (22 mismo,
  7 distinto) con el modelo efectivo y sin errores de gate.
  why: V6 había fallado sólo el par 48 por razonar sobre un distractor; el criterio R002 manda juzgar la decisión que
  efectivamente practica el alumno.

2026-10-06 — coordinador — La revisión completa de los 57 pares, leyendo tema, hueco y respuesta resuelta, deja 50
  positivos y 7 negativos. Los pares 206 (`this`/`these` en el tema de demostrativos) y 239 (`love`/`enjoy` en
  gerundio vs infinitivo) pasan de S a O-decision porque el cambio es precisamente el contraste enseñado. El par 48
  conserva S: en ambos la respuesta seleccionada es omitir el reflexivo (`-`); la función del distractor visible no es
  la función de la respuesta.
  why: R002 manda mirar la decisión concreta del tema y la respuesta seleccionada, no heredar el prefijo del oro viejo
  ni atribuirle a un distractor la función del hueco resuelto.

2026-10-06 — coordinador — El oro congelado el 3/10 pertenece al criterio anterior y no puede aprobar ni reprobar al
  juez vigente. Las clases `O-cosa`, `O-oracion` y `O-sentido` separaban por contenido o situación aunque se conservaran
  la operación y la función; bajo R002 son positivos. Sólo `O-funcion` conserva negativos inequívocos. Se reconstruyó
  de forma reproducible una puerta de transición de 57 pares (52 mismo, 5 distinto), pero no sustituye la ampliación
  balanceada de negativos que exige R007 para la aprobación formal.
  why: afinar el prompt contra etiquetas incompatibles produjo la oscilación V3–V5; cambiar Sol por Astra no podía
  corregir una contradicción del examen.

2026-10-05 — analyst — R002 define duplicado por misma operacion/decision pedagogica, respuesta objetivo y funcion.
  Sujeto, tiempo, lugar, numero, posesivo y lexico no objetivo son superficie salvo que cambien esa decision; dialogo ya
  no es una exclusion automatica. El oro vigente debe reclasificarse y ampliarse antes de volver a medir.
  why: ajuste aprobado por Jose en el brief 029 tras explicar los 27 falsos negativos de Codex.

2026-10-03 — analyst — Precisa las trazas de dos pruebas del qa-tester (pedido del coordinador): R010 suma la inv. 3
  (si el juez de pares no enuncia su versión, la de Ollama no se nombra ni rinde, no se consulta ni a nomic-embed-text
  ni a claude -p, y near-duplicates queda sin evaluar, comodín incluido, con el motivo del juez); R009 inv. 3 dice que
  comodín y vectores corren en todos los temas de la corrida aunque haya un conjunto declarado para el juez, que sólo
  elige qué pares se le vuelven a preguntar a claude -p.
  why: «juez sin versión» (R010) y «vectores con conjunto declarado» (R009 inv. 3) trazaban a algo que la regla no
  decía literalmente.

2026-10-03 — qa-tester — El reuso de vectores entre versiones del paso traza a F-MLOCAL-R002 inv. 2, no a R010 inv. 2,
  que habla sólo de lo que contestó el juez; va en un parche aparte de FEAT-MLOCAL (propose rechaza otro feature).
  why: forzar R010 sería la regla «más cercana»; si se quiere en DUPAR, el analista suma una invariante a R010.
2026-10-03 — qa-tester — La versión sin Ollama («palabras-0,60 · juez-…») traza a R007, que la nombra; R010 nombra sólo
  la de Ollama. «Todo sin evaluar sin examen aprobado» pasa de R007 a R010: la de tres piezas no hereda el del 30/9.
2026-10-03 — qa-tester — Un sí del juez con Ollama, bajo R002 (condición 2); el no de «That car is his.», 0,97, bajo R009
  (el parecido solo no marca). El comodín gana sin preguntarle al juez, bajo R004, que lo dice. Juez sin versión y
  PairStepEvaluator, bajo R010. Los vectores con conjunto declarado, bajo R009 inv. 3 («corren siempre»).
2026-10-03 — qa-tester — Se quedan con su nombre y el cuerpo a reescribir: el grupo de n de «Could I» (R003, ahora con
  veredicto del juez) y el examen dos veces sin consultas (R007). Ninguna prueba depende del id same-meaning de la ficha
  de IAuditEngineTest: las dos de F-RONDA2-R003 sólo necesitan una regla de modelo local.

2026-10-01 — architect — v3, el paso por pares: PairStepEvaluator (interno) envuelve al juez de pares en las dos rutas
  y, con Ollama, a los vectores: mismo evaluatorId que el envuelto; enuncia la versión de la ruta si el envuelto enuncia
  la suya. Con Ollama, dos sesiones (vectores sin tope aunque haya --budget; juez con la política de la corrida) en un
  solo JudgeSessions, vectores primero y juez último.
  why: el freno del runtime y el examen (ExamScopedEvaluationLedger fija la versión) miran la versión de cada evaluador;
  el libro identifica por id + huella, así lo que contestó el juez se reusa entre rutas (R010 inv. 2).
2026-10-01 — architect — Distinto del pedido del coordinador: methodId() sigue siendo sólo «clustering-1»; umbral y juez
  van en vectorRouteMethodId(judgeVersion), y los vectores se guardan con la versión de los vectores solos. Sin Ollama el
  juez también va envuelto, bajo wordRouteVersion «palabras-0,60 · juez-…».
  why: cambiar el umbral o el juez no recalcula vectores (F-MLOCAL-R002 inv. 2) y sirven los de la v2; R007 nombra la
  versión sin Ollama «parecido de palabras y juez», y con el juez pelado cambiar el 0,6 no obligaba a rendir.
2026-10-01 — architect — La cobertura se pide con la sesión del juez sola; los modos del plan son del juez (vectores
  siempre «reusar o calcular»); en EXAM sólo los pares cuya copia está en el archivo, y los de afuera ni cuentan.
  why: DefaultJudgeRuntime.coverage suma lo pendiente de todas las sesiones y mide el ritmo con todos los evaluatorIds;
  con los vectores (~4.600 en un minuto) diría minutos en vez de 8,3 h para 1.468 pares.

2026-10-01 — analyst — Ajuste del coordinador (verificado en el código): la ficha conserva una regla de modelo local.
  R006: tres reglas, `same-but-wildcard` (instantáneo), `similar-candidates` (modelo local: con Ollama propone los pares
  desde 0,80 y nunca marca solo) y `same-exercise` (modelo pago); `same-meaning` sale porque cambió lo que hace. R004:
  la regla de modelo local no da hallazgos. Reemplaza la entrada de «dos reglas» de más abajo: F-RONDA2-R003 queda igual.
  why: la corrida base deja afuera a un analizador con modelo sólo si su ficha tiene una regla de modelo local, y es la
  verdad: los vectores eligen qué se le pregunta al juez.
2026-10-01 — analyst — Ajuste del coordinador: el filtro del examen vale para las dos versiones. Pasa de R009 (sale su
  inv. 5) a R007, como invariante del examen de cualquier versión. El 699 sin Ollama queda como costo sin el filtro y sin
  recontar con las reglas de producción. Reemplaza la entrada «sólo de la versión con Ollama» de más abajo.

2026-10-01 — analyst — v3, el paso por pares (diseño del orquestador, con números): el parecido ya no marca. Con Ollama,
  candidatos = pares de R001 que el comodín no resuelve con coseno ≥ 0,80 (nomic-embed-text, «clustering: »); los decide
  claude -p con `near-duplicates-pair-judge`, el mismo juez y la misma pregunta que sin Ollama (R008, palabras ≥ 0,6).
  Comodín y vectores siempre y sin tope; `--budget near-duplicates=N` sólo para claude -p. Examen: sólo los pares cuya
  copia está en su archivo (≤ 255 consultas, no 978).
  why: nomic-embed-text ≥ 0,95 reprobó (20 de 40 no marcados de 0,90–0,95 eran el mismo ejercicio); llama3:8b no separa
  pares (dijo sí a 50 de 50, AUC 0,49–0,67); claude -p con la guía, 23 de 24 inventados.
2026-10-01 — analyst — R002 pasa a ASSUMPTION (cambia lo validado el 30/9); R004 sigue VALIDATED (sólo sale el costo
  *modelo local*). Ficha (R006): dos reglas, el comodín instantáneo y el juez modelo pago; sale `same-meaning`.
  why: sin regla de modelo local, F-RONDA2-R003 dejaría entrar a near-duplicates en la corrida base, contra
  F-MLOCAL-R004 (José, 30/9). Queda escrito en R006 y en el informe al coordinador; FEAT-RONDA2 no se tocó. Lo mismo con
  F-RONDA2-R002 («--budget limita la ruta que toque»): con Ollama, el tope es sólo de claude -p (R009, detalle).
2026-10-01 — analyst — J001: las dos salidas nuevas con Ollama (el juez dice que no; sin tope) van al final, y la segunda
  pasa de «0,95» a «0,80 y el juez dice que sí» (nodo marca_juez): los caminos 1, 3, 4 y 5 conservan su número.
2026-10-01 — analyst — El filtro del examen (R009 inv. 5) es sólo de la versión con Ollama, como lo pidió el orquestador;
  la de sin Ollama sigue rindiendo 699. Si vale para las dos, es una invariante más en R008.
2026-10-01 — analyst — Los ejemplos de «el mismo ejercicio» (R002) son los inventados de la guía y los del 077 en
  escribir/; ninguno sale de la zona de la muestra ciega ni se leyó nada de content-audit-corridas ni de examinar/.
  why: el examen lo rinde otro equipo a ciegas.

2026-10-01 — developer (paquete B) — El comodín ampliado del prototipo, todavía sin adoptar. E1: un nombre (PROPN y con
  mayúscula) vale lo mismo que otro, salvo si la respuesta es un artículo o «-» (ahí el nombre es lo que se pregunta);
  por un pronombre, sólo como sujeto. E2: el sujeto del principio de cada oración (un pronombre, o determinantes y
  adjetivos y un sustantivo) cuenta como una palabra si le sigue un verbo y no tiene palabras de la respuesta; nunca
  un «who/what/…», «there» o «here». E3: fuera «not» y el auxiliar do, y el auxiliar que abre la pregunta vuelve
  después de su sujeto. E4: fuera artículos, «another», «some», «any» y los posesivos que van con un sustantivo. Lee
  el POS de spaCy de los tokens del ejercicio (AuditableQuiz.getTokens(), los de la solución).
  why: con el nombre como pronombre en cualquier lugar «it is Monday» quedaba igual a «it is him!»; con el PROPN de
  spaCy sin mayúscula, «pencils» y «relatives» (clasificar sustantivos) quedaban iguales; un sujeto con la respuesta
  adentro tapaba lo que pregunta el ejercicio («that film» / «this phone»). Adoptarlo es otra versión examinada
  (p. ej. «clustering-1 · comodín-2» en el methodId), con su examen y una muestra ciega nueva, que conviene que
  revise José: casi todos los no marcados de 0,90 a 0,95 ya los vi.

2026-10-01 — developer (paquete B) — La muestra ciega del modelo local (`armar_muestra_ciega.py` de la carpeta del
  examen): marcados = las copias de una corrida del curso entero por el camino del examen (siempre publica), con el que
  queda; no marcados = pares del mismo tema con la misma respuesta y coseno de 0,90 a 0,95 de los vectores de esa
  corrida, sin una copia marcada entre los dos. «La misma respuesta» se lee más estricta que el analizador (primeras
  respuestas en minúscula y sin signo final, o tapando pronombres y demostrativos; nunca una copia exacta): 19 de 19
  same-meaning con vector salen con la misma respuesta y su parecido. El revisor escribe sus respuestas antes de abrir
  la clave; `puntuar_muestra_ciega.py` cruza las dos.

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
