# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-10-01 — qa-tester (paquete D) — Parche de sólo nombres, propuesto y sin aplicar (architectural_patch.yaml): borra
  y vuelve a agregar, con la misma regla, las tres pruebas de LemmaRecurrenceAnalyzer cuyo nombre decía los números
  del 30/9 (R001 131 · 1.566 · 46 de 1.743; R004 A1 448/176 … B2 818/165; R005 1.612 hallazgos). `patch validate -i
  sentinel.yaml` OK, solo y junto al de RESPC. TECH_SPEC sin tocar: el parche no cambia estructura.
  Next: el integrador aplica los dos y genera; después se renombran a mano los seis métodos y corre la suite.

2026-10-01 — developer + analyst (paquete D) — Opción A del orquestador: sólo entra un lema escrito con letras (con
  apóstrofo o guion internos); afuera «'», «-», «i.», «1st», «3rd», «4th» y «comic_strip». Las posiciones no cambian
  (91.873). Curso: 0,08 con 131 · 1.566 · 46 de 1.743 (7,52 %), 1.612 hallazgos; niveles 448/176 · 752/155 ·
  1.137/161 · 818/165; vocabulario 73,86 % igual (0,738582). Pruebas con esos valores; REQUIREMENT R001, R004, R005.
  Pendiente @qa-tester: tres nombres declarados en sentinel.yaml llevan los números del 30/9 (134/1.570/46 de 1.750;
  A1 450/178…; 1.616 hallazgos): el cuerpo ya fija los nuevos.
  La opción B (sin puntuación en las posiciones: 0,09 y vocabulario 74,00 %) queda en la rama ronda1/recur-posiciones,
  sin juntar: decide José.

2026-09-30 — developer + test-writer (paquete D) — FEAT-RECUR construido: 24 pruebas sin @Disabled. lemma-recurrence
  publica su LemmaRecurrenceCourseDiagnosis (cada lema con hueco más largo, niveles de inicio y fin y niveles; el detalle
  de cada nivel), deja un hallazgo F-RECUR-R005 por lema fuera de rango y Main enciende su formateador en analyze
  --detailed y en stats analyzer (D-11, D-15). Fixture audit-domain/src/test/resources/recur-2026-09-30/stream.tsv.gz
  (11.287 ejercicios, 91.873 posiciones del análisis 2026-09-30T11-54-02): 0,08 con 134 · 1.570 · 46 de 1.750, los
  niveles 450/178 · 752/155 · 1.139/161 · 819/165 y 1.616 hallazgos, exactos.

2026-09-30 — qa-tester — Ronda 1: `patch propose` exit 0, fusionado con el parche del arquitecto: 24 handwrittenTests nuevos (0 portados) y F-RECUR-J001 en audit-cli, paquete commands; `tech-spec write` exit 0 (+1 sección). No aplicado.
  Simulado en scratch/qa-D (apply en memoria con la librería del jar, sin CLI apply ni generate), sobre el sentinel.yaml de hoy y sobre 0.2 + 0.3 aplicados antes: 24 pruebas aterrizan, 0 errores de traza, J001 ubicado, `feature status` OK, 6/6 reglas [covered]; generate agregaría 24 stubs y FRecurJ001JourneyTest con 2 caminos, sin choques de nombre.
  Hueco para el integrador: D-11 registra el formateador en `analyze --detailed`, pero `stats analyzer` no usa ese mapa; la prueba de StatsAnalyzerCmd (R003) espera ese cableado.
  Next: el integrador aplica después de 0.2 y 0.3 y sin `--as` (el parche lleva la estructura del arquitecto); José corre generate; @test-writer implementa.

2026-09-30 — architect — Ronda 1: REQUIREMENT al día (2 dudas resueltas, José 30/9). Parche y TECH_SPEC en exit 0: `LemmaRecurrenceCourseDiagnosis` con el detalle por nivel adentro, hueco más largo por lema, formateador de stats.
  Next: el integrador suma D-10 y D-11 de ronda1/ganchos-D.md; `feature sync --feature FEAT-RECUR` y @qa-tester.

2026-09-30 — analyst — REQUIREMENT.md v1 de FEAT-RECUR (evolución de lemma-recurrence, paquete D): 6 reglas, 1 journey, 2 dudas.
  Prototipo con posiciones que cuentan la puntuación: 134/1.750 normales = 7,66 % → 0,08 publicado (sin puntuación daría 0,09, así que el analizador la cuenta).
  Next: José contesta DOUBT-PUNTAJE-POR-NIVEL y DOUBT-UNA-SOLA-VEZ; @architect: diagnóstico de curso de lemma-recurrence con estadísticas por lema, detalle por nivel como sub-métrica, hallazgos de curso.
