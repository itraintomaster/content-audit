# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — analyst — Naturalidad cambia de método (ver FEAT-NATUR): pregunta sí/no a llama3 en Ollama, que aprueba el examen;
  con eso las dos herramientas de modelo corren en Ollama, ya instalado, y no hace falta bajar nada (DOUBT-MLOCAL-DEPENDENCIAS A).

2026-09-30 — analyst — Repetibilidad: F-MLOCAL-R002 inv. 2 guarda el número de cada ejercicio con su contenido y versión y lo
  reusa. why: sí/no dio diferencias de hasta 0,025 entre la corrida larga y una corta, suficiente para cambiar una marca de borde.

2026-09-30 — analyst — Código FEAT-MLOCAL / F-MLOCAL: nuevo. Aloja lo común a Gramática, Naturalidad y Duplicados parecidos,
  como FEAT-JEXAM aloja lo común de los jueces.
  why: sin esto, cada uno repetía qué pasa sin la herramienta, la versión, la corrida por omisión y el candidato de la mesa.

2026-09-30 — analyst — No se fusionan Naturalidad y Duplicados parecidos: comparten sólo «modelo local», que vive acá; sus
  reglas, modelos, examen y resolución (sólo ordena contra mesa) no se tocan.

2026-09-30 — analyst — Sin la herramienta: pedido por nombre → rechazo antes de empezar; sin selección → sin evaluar con
  motivo, y el resto corre (R003).
  why: F-HALL-R012 rechaza antes de empezar lo que no puede correr; F-HALL-R008 ya cuenta lo sin evaluar.

2026-09-30 — analyst — Se midió sin instalar nada: LanguageTool 6.5 de IntelliJ (Grazie) + jars de ~/.m2 y ~/.gradle en el
  classpath del prototipo; Ollama 0.34.4 con llama3:8b (Q4_0) y nomic-embed-text (137M), ya instalados.
  why: pedido del orquestador y regla de no descargar sin OK. Lo que falta está en DOUBT-MLOCAL-DEPENDENCIAS.

2026-09-30 — analyst — Ollama no devuelve la probabilidad del texto de entrada, sólo la de lo generado (máx. 20 alternativas),
  y la salida forzada con formato JSON da la probabilidad real pero con una tokenización rara. Para SLOR, Ollama no sirve
  tal como está; la pregunta sí/no de FEAT-NATUR sí corre ahí (entrada de arriba).
