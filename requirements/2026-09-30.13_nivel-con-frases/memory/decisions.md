# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — architect — Paquete público `offlevelwords` (audit-domain, allowedClients audit-cli y refiner-domain): diagnóstico, regla del tema (`TopicTeachingResolver`, compartida con el semáforo) y proveedor; el analizador es package-private, recalcula con `SentenceLexicalScorer` (no lee lo de lemma-absence) y puntúa 0 o 1.
  why: correr solo (F-HALL-R012), paridad con lexis por construcción (R001 inv. 3) y una sola regla del tema para dos piezas.

2026-09-30 — architect — El proveedor va declarado como clase con las cinco firmas de `AnalyzerProvider`: `patch propose` no resuelve un `implements` hacia un puerto de 0.3. Enganche D-01: al integrar pasa a `implements: [AnalyzerProvider]`.

2026-09-30 — architect — Medido con un prototipo sobre el análisis del 29/9: 170 ejercicios (18/33/81/38), 175 palabras, R003 saca 37, R004 saca 19; `medir.py --parte escribir --categoria nivel` da 26 de 52.

2026-09-30 — analyst — José resolvió DOUBT-UMBRAL-DE-ERROR → A, dos niveles, y DOUBT-FRASES-SUPERIORES → A, no por ahora (José, 30/9). R002 pasa a VALIDATED.

2026-09-30 — analyst — DOUBT-REBAJA-EN-CONTEXTO, DOUBT-TAREA-COMPARTIDA y DOUBT-LECTURAS-NUEVAS → A, la recomendada (José, 30/9: «por lo demás, continúa»). R003 y R004 pasan a AUTO_VALIDATED; R006 dice que el plan declara los errores como cubiertos por LEMMA_ABSENCE.

2026-09-30 — analyst — Anotado sin arreglar: 1 de los 170 es falso por el lematizador compartido («hoped» → hop, C1, en un ejercicio B1).

2026-09-30 — analyst — Código FEAT-NIVFR / F-NIVFR, analizador `off-level-words`, familia errores (decisión de José del 30/9: lemma-absence queda entero en vocabulario).
  why: el error lo cuenta un analizador aparte; así el 73,9 % y las 4.036 tareas no se mueven (F-HALL-R013).

2026-09-30 — analyst — El nivel de cada palabra es el de lemma-absence, R042 incluida (paridad con lexis, F-CLEX-R006).
  why: el prototipo reproduce LABS exacto; leer las frases «en contexto» sumó 34 marcas, 1 caso del examen y falsos como «Where do you live?». Queda como DOUBT-REBAJA-EN-CONTEXTO.

2026-09-30 — analyst — Error = dos niveles arriba o fuera del EVP en la banda fuerte de R034 (ASSUMPTION, DOUBT-UMBRAL-DE-ERROR).
  why: de los 337 de A1, 329 sólo tienen palabras un nivel arriba y 281 vienen de la fusión del 26/9 (594 ejercicios A2 → A1). Con la regla: 18/33/81/38 = 170 contra 17/35/50/23 del 077.

2026-09-30 — analyst — Excepciones sólo con datos del curso: términos `$...$` del título del tema, respuesta de ≥3 y ≥20 % del tema, y palabra repetida entera en la traducción.
  why: «nunca listas a mano» (LABS R035). Sacan invest, sequence/simultaneous, non-defining, sushi, USB, Emily; pierden un caso del 077 (reside, que también es castellano).

2026-09-30 — analyst — Frases de nivel superior y lecturas nuevas (verbo de un sustantivo, forma de palabra) fuera de v1, como dudas.
  why: medidas: frasales estrictos 23 ejercicios con ~50 % falsos y 1 caso; verbo-de-sustantivo 214 ejercicios casi todo etiquetado; forma de palabra 127 rescates con falsos. R005 de LABS prohíbe que una frase suba una palabra.
