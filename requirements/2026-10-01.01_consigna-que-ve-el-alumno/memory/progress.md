# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer (C) — Control y junta en seco.
  - Control, analyze sin tope sobre copias con un claude falso: 0 llamadas y el libro intacto. Contra la misma base
    con el sujeto viejo, los 8.898 hallazgos de los otros analizadores son idénticos. La consigna pasa de 3.920
    veredictos reusados (10 hallazgos HIGH) a 0: el sujeto nuevo cambia la huella. Vuelven sólo si se juzga de nuevo.
  - ronda2/integracion ya trae el cuerpo de NIVFR (27317fa9). 1296e6b2 deja el cableado de prueba de NIVFR igual al
    suyo, y `git merge-tree` junta sin conflicto.
  - En ese árbol: 3.096 pruebas, 0 fallas, 0 errores, 614 apagadas; verify sólo MEMORY.md; el parche de seguimiento
    valida. Son 7 menos que las 3.103 de D: se van las 10 del sujeto viejo, que el parche ya no declara, y entran 3.
  Next: el coordinador junta la rama y aplica el parche de seguimiento. El examen de consigna espera el OK de José.

2026-10-01 — developer (C) — FEAT-CALUM construido en ronda2/impl-calum, sobre fd91447c (apply b5724f63, generate
  9e93a983, arreglo de SpacyProcessRunner fd91447c). Commits:
  - R001 (32f0228b), R002 (9868302f), R003 (68565266);
  - las dos definiciones (b9815c18): la versión del juez de consigna da 53fabe0d…;
  - get y el corrector 1.1.0 (c86055d3).
  Sus tres pruebas van escritas con el nombre que les dará generate. Su declaración es el parche de seguimiento de
  esta carpeta (0 altas, 5 cambios), sin aplicar. Simulado: apply, generate y VERIFY OK; generate no las toca.
  fd91447c no compilaba por FEAT-NIVFR: la aridad que dejó su generate va aparte (6f772245), sin su cuerpo.
  `mvn -o test` (sin verify): 3.081 pruebas, 614 apagadas como antes, 0 fallas. Hay 3 errores: los esbozos de
  lookupDerivedFormLevel en FileSystemEvpCatalogTest, que son el cuerpo de NIVFR.
  sentinel verify, en una copia recién sacada, da una sola diferencia: MEMORY.md. También la da fd91447c intacta: la
  columna de fecha sale de la hora de los archivos.
  Next: el coordinador junta la rama y aplica el parche de seguimiento. El examen nuevo de consigna espera el OK de
  José.

2026-10-01 — developer (C) — R003 sumado al mismo parche, propuesto y sin aplicar.
  - Antes del propose, copia de la carpeta en content-audit-corridas/2026-10-01-consigna-alumno/backup-antes-de-R003
    (bug 08 de Sentinel).
  - El propose fue con --replace y el parche entero, sin fusión. Comparado con la copia, no se perdió nada; propose
    sólo agregó `type: record` al modelo.
  - `patch validate`: 0 altas, 13 cambios, 0 conflictos, contra la rama y contra ronda2/integracion (d80f7f6a).
  - Ensayado entero en copias descartables:
    - en la rama: VERIFY OK y 2.411 pruebas en verde (las dos nuevas de R003 incluidas);
    - en ronda2: VERIFY OK y las mismas 68 líneas en rojo que ronda2 ya tenía, ninguna nueva.
  - generate rellena con null la construcción de main (el resolvedor). A mano quedan 7 construcciones de prueba del
    contexto: llevan la consigna del tema como la de la copia, igual que antes.
  - El kit de content-audit-corridas/2026-10-01-consigna-alumno ya trae R003: cuerpos.diff (32 archivos) y la
    definición de los dos agentes. La versión del juez de consigna no cambia por R003: 53fabe0d….
  Next: el coordinador junta ronda2, aplica y genera; después entran los cuerpos y las definiciones.

2026-10-01 — developer (C) — Validado contra ronda2/integracion (d80f7f6a), la línea principal nueva.
  - `patch validate -i sentinel.yaml` sobre una copia descartable de ronda2: 0 altas, 8 cambios, 0 conflictos.
  - Ensayado ahí el aterrizaje entero: apply, generate, cuerpos y definición. VERIFY OK, y las mismas 68 líneas en
    rojo que ronda2 ya tenía antes (spaCy, stubs de la ronda 2). Ninguna nueva, ninguna arreglada; revision-domain
    suma la prueba nueva del revisor.
  - La versión con la definición completa es la misma: 53fabe0d….
  El merge de ronda2/integracion a esta rama no se hizo: el permiso de la sesión lo bloqueó. Lo junta el coordinador.

2026-10-01 — developer (C) — Parche propuesto y sin aplicar. `patch validate -i sentinel.yaml`: 0 altas, 8 cambios,
  0 conflictos; `feature validate` OK.
  Simulado entero en una copia descartable de ronda1/consigna-alumno: apply, generate, cuerpos y pruebas.
  - generate cambia las tres clases del parche y suma 11 esbozos de prueba en 4 clases;
  - después hay que tocar a mano:
    - main, en dos líneas;
    - 20 construcciones de prueba de DefaultQuizInstructionAnalyzerFactory, en 15 clases;
    - los cuerpos del constructor del sujeto, de las dos fábricas y de los dos lugares de revise-instructions;
    - los fixtures de 4 clases de prueba que armaban el contenido de agosto a mano (la clave `quiz`).
  Con todo eso, `mvn -o test` da verde en los 15 módulos, con sentinel verify y la guardia del examen.
  En la rama sólo entra la guía con el tope de cada texto.
  La guía con la entrada nueva, agent.yaml y run_evals.py esperan al apply, junto con los cuerpos: hasta entonces,
  el juez de esta rama recibe la entrada de siempre.
  Versión del juez de consigna, con el modelo y el proveedor del examen del 1/10:
  - 488c0c72…, la del examen frenado;
  - c6950901…, sólo con el tope, en esta rama;
  - 53fabe0d…, con la definición completa.
  Next: el coordinador aplica y genera; después entran los cuerpos y la definición, y corre la suite. El examen se
  vuelve a rendir sólo con el OK de José.
