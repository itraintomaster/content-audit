# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer (paquete D) — Toda lectura nueva se mide con el SentenceLexicalScorer compartido sobre un token
  suelto (sin puerto nuevo): el nivel de la lectura es el que daría lemma-absence, con su repliegue por lema (R036).
  why: R001 inv. 3 sigue por construcción (sólo se sacan marcas) y no hace falta inyectar el catálogo.
2026-10-01 — developer (paquete D) — Umbrales: el derivado toma el nivel de su base (no es error hasta un nivel arriba);
  el adverbio en -ly, dos más (base de un nivel anterior); el compuesto pide sus dos partes del nivel o de antes.
  why: el 077 marcó soundly, awfully, endlessly y gracefully (A2) y sideboard (A1, side A2): con el umbral de R002 en
  compuestos o en -ly se perdían. El repliegue por lema da «sound» A2 aunque el adjetivo no esté en el EVP.
2026-10-01 — developer (paquete D) — Miniteoría (R003.3): sólo el término de una palabra entre $; «$a warranty for$» no.
  why: con cualquier mención se perdían disembark ×2 y warranty, del oro de escribir. Vive en el analizador: TaughtWords
  no tiene campo para ella (el semáforo sigue con 1 y 2); lo mismo el sufijo del tema y el metalenguaje.
2026-10-01 — developer (paquete D) — Préstamo adaptado (R004.2): nunca un verbo; adjetivo o adverbio, igual plegado o
  con raíz común de 5 letras; sustantivo, además a una letra. Lista cerrada sólo la de pares americano/británico.
  why: «adore»/«adoran» y «reside»/«residimos» siguen marcando (077); «hydrated»/«hidratado» (oro) queda a una letra.
2026-10-01 — developer (paquete D) — La teoría del tema no se lee: vive en ittm/content/theory y el ejercicio la
  nombra por theoryId, que AuditableQuiz no trae. Con un prototipo, sacaría 3 marcas más (tweezers, concur, acquainted:
  las 3 falsas de la muestra) y ninguna del oro. Parche propuesto, sin aplicar: AuditableQuiz.theoryId, puerto
  TopicTheorySource, FileSystemTopicTheorySource (vocabulary-infrastructure), TaughtWords.theoryWords y el resolutor
  que lo inyecta; validado contra ronda1/integracion (8a3f949c). TECH_SPEC reescrito: el del 30/9 queda en 0998557c.
  why: es la causa de las 3 falsas que quedan en la muestra; costo, el constructor de AuditableQuiz en ~314 pruebas.
2026-10-01 — test-writer (paquete D) — FNivfrJ001JourneyTest fija 95 (9 · 25 · 42 · 19) en vez de 170 y la prueba de la
  ficha su nuevo «lee»: valores fijados que cambian con las reglas, no pruebas nuevas.

2026-09-30 — developer (paquete D) — La respuesta de un ejercicio (R003) son todas las variantes «|» de sus huecos o la
  etiqueta de la opción correcta (incidencia > 0, como correctItem de 0.2); se cruza con el texto de los tokens. Un
  término del título con apóstrofo enseña además la palabra antes del apóstrofo. El diagnóstico tipado se escribe en
  todo ejercicio evaluado, con la lista vacía si no hay palabras: el semáforo distingue «no es error» de «sin dato».

2026-09-30 — integrador — Rebasado sobre el sentinel.yaml real y FEAT-RONDA1 (propose --replace, exit 0): OffLevelWordsAnalyzerProvider implements AnalyzerProvider (D-01); proveedor y analizador inyectan QuizStudentViewRenderer (D-14).

2026-09-30 — qa-tester — F-NIVFR-J001 en audit-cli, `com.learney.contentaudit.auditcli.commands`.
  why: cruza analyze y el plan; AnalyzeCmd y PlanCmd son package-private ahí (como F-CLEX-J001) y el analizador se alcanza por su proveedor público.

2026-09-30 — qa-tester — R006 se prueba fuera del paquete: IAuditEngine (lemma-absence igual con y sin el analizador; vocabulario 73,92 % y 96,54 · 94,16 · 79,45 · 63,51) y DefaultRefinerEngine (cubierto por LEMMA_ABSENCE, D-04; 4.036 tareas).
  why: el analizador solo no puede garantizar lo que no mueve; cada número va en la pieza que lo calcula, con fixture del análisis 2026-09-30T11-54-02 (D-12).

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
