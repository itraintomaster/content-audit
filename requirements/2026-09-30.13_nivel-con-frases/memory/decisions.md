# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

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
