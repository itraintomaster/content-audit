# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-10-01 — qa-tester — F-NATUR-J001 en audit-domain, `com.learney.contentaudit.auditdomain.naturalness`: el
  analizador y sus piezas son package-private ahí; los puertos se simulan y el runtime de los jueces corre con libros en
  memoria, como F-TRSEN-J001.
2026-10-01 — qa-tester — «Con llama3 nunca se consulta a claude -p» se traza a F-NATUR-R008 (es su condición: «sin su
  modelo local»), en el proveedor; la suma de las formas de sí y no, a R002 en OllamaYesNoModel (de FEAT-MLOCAL); y la
  lista de un nivel como conjunto de un juez, a R005 en DefaultReevaluationQuizSetResolver.
2026-10-01 — qa-tester — Para el analista: 15 de las 120 marcas de A2 del prototipo son ejercicios de la lista «a / b»
  dentro de la oración ya cerrada, leídos sin la respuesta: «He did to help.» (14,48), «I invited to enter our room.»,
  «I didn't see on the street.», «I require to eat.»… y el ejemplo de R004 y R008, «She bought at the market.»
  (…3974). Si F-GRAMA-R001 los lee con la respuesta en el lugar de la lista, el umbral de A2 (10,2) y ese ejemplo se
  vuelven a medir.

2026-10-01 — integrador — Rebasado sobre ronda1/integracion y FEAT-RONDA2 (propose --replace, tech-spec write y validate en exit 0): sin cambios de estructura. naturalness no tiene tipo de tarea (sólo ordena, F-RONDA2-R008) y no entra en la corrida base (F-RONDA2-R003).

2026-09-30 — architect — Las dos rutas abren por `JudgeRuntime` y resuelven en lote (onQuiz junta, onCourseComplete
  resuelve), como los jueces; el proveedor ya no recibe la fábrica de sesiones sino `judgeRuntime` y `alternativeJudge`.
  El contenido de claude -p es {system, question} de `NaturalnessConfig`: la misma pregunta por construcción. `number` pasa
  a `Double` (null con claude -p); `alternative` en el diagnóstico y en la lista. Reglas `unlikely-sentence` (modelo local)
  y `native-says-no` (modelo pago): la ficha pasa a costo modelo pago.

2026-09-30 — analyst — Nueva R008: sin Ollama, la misma pregunta a claude -p; un «no» marca, sin número ni umbral, y la
  lista del nivel va en el orden del curso. DOUBT-NATUR-ALTERNATIVA (OPEN; A aplicada). R006: el triple que el azar con la
  misma cantidad de marcas; con claude -p, sobre los 186 + 400 al azar de A1–A2 (586 consultas). R007: dos reglas, ficha
  de costo modelo pago (el mayor). Consultas: A1 2.334, A2 2.403, B1 4.535, B2 1.901 (114 de 11.287 no alcanzan).
  why: claude -p no devuelve probabilidades, así que el cociente de R002 no se puede calcular.

2026-09-30 — analyst — Umbral de A1 verificado otra vez con `arq-B2/umbral_nat.py` sobre `yn-a1a2.jsonl`: puesto 116 = 9,0752,
  puesto 117 = 9,0664 → 9,07 (se muestra 9,1); 10,0 marcaría 90 (3,9 %). A2: 10,2313 / 10,1865 → 10,2 marca 120.

2026-09-30 — architect — El número se guarda en el libro de evaluaciones con un evaluador propio: contenido = la oración; id =
  «naturalness» + versión examinada (modelo@huella · methodId de la pregunta), así que otra versión no reusa nada. Se guardan
  las dos probabilidades, no el número. Sin `--budget`, sin tope (gancho B-06).
  why: F-MLOCAL-R002 inv. 2 pide reusar mientras no cambien contenido y versión, y el libro no hace de la versión identidad.

2026-09-30 — architect — Los umbrales van en `DefaultNaturalnessConfig` por versión examinada: A1 9,07 y A2 10,2 para llama3:8b
  (365c0bd3c000); B1 y B2 se calibran antes de registrar el examen (gancho B-19); un nivel sin umbral publica números sin marcar.
  La lista por nivel es un diagnóstico del curso (gancho B-03); `get naturalness-queue` es opcional (B-16). Sin ligadura al plan.

2026-09-30 — analyst — Corrección, notas del orquestador: el umbral de A1 recalculado sobre `yn-a1a2.jsonl` es 9,07 (se muestra
  9,1; marca 116 de 2.334), no 10,0. El de A2, 10,2, queda (120 de 2.403).

2026-09-30 — analyst — José, 30/9 («Dale con … el método nuevo de naturalidad»): DOUBT-NATUR-METODO → A (pregunta sí/no,
  ln(P(no)/P(sí)); SLOR descartado), DOUBT-NATUR-MARCA → A (sólo ordena) y DOUBT-NATUR-UMBRAL → A (fijo por nivel).
  R002 y R003 pasan a VALIDATED; R001 ya no pide leer la forma larga (sólo hacía falta con SLOR).

2026-09-30 — analyst — Código FEAT-NATUR / F-NATUR y nombre `naturalness`: nuevos.

2026-09-30 — analyst — Método propuesto: preguntarle al modelo local (llama3 8B en Ollama, ya instalado) si un nativo diría
  la oración y usar ln(P(no)/P(sí)) del primer token (R002) [ASSUMPTION, DOUBT-NATUR-METODO], en vez del SLOR de la propuesta.
  why: medido sobre A1–A2 (4.813): la cola del 5 % por nivel trae 51 de los 186 antinatural+sentido de escribir (27,4 %,
  5,5 veces el azar; 47,9 % de la cola en algún oro contra 23,2 % de base). SLOR con el mismo modelo, leído de a un token
  con 20 alternativas porque Ollama no da la probabilidad del texto: 14 de 186 (7,5 %), casi el azar.

2026-09-30 — analyst — El cociente y no P(no): P(no) pasa de 0,999 en más de 300 ejercicios por nivel y el borde del 5 % se
  vuelve un empate. Umbrales del 5 % en la base: 10,0 (A1) y 10,2 (A2).

2026-09-30 — analyst — Sólo ordena (como aprobó José en el 089) y umbral fijo por nivel, no «el 5 % de cada corrida»
  (DOUBT-NATUR-MARCA, DOUBT-NATUR-UMBRAL).
  why: con 48 % de la cola en algún oro, como error inflaría «algún error»; con el 5 % por corrida, arreglar una oración
  rara marcaría otra que no cambió (F-HALL-R014).

2026-09-30 — analyst — Examen (R006): al menos el triple del azar en la cola del 5 % (28 de 186 en escribir). La pregunta
  sí/no lo aprueba; SLOR con lo instalado no. Sin verbo o de una palabra no alcanza (76 de 4.813 en A1–A2).
