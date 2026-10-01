# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer (impl A) — Salió el corrector nuevo: ittm #1191 (30e0efcc0) en producción desde el 1/10/2026 00:30 UTC
  (learney-app, sa-east-1). formas-equivalentes.json es ahora su tabla, versión «corrector en producción, ittm #1191
  (30e0efcc0), desplegado el 1/10/2026», y es la única copia: la del «corrector nuevo» se fue. La vacía quedó como
  formas-equivalentes-corrector-viejo.json, sólo para la columna «hoy» de los casos de oro (153 de 153 en las dos pruebas).
  Curso del 29/9 con la tabla en producción: 706 ejercicios con hallazgo (A1 554, A2 105, B1 23, B2 24): 697 pedidos de
  forma (654 por regla, 43 a la mesa, baja) y 12 formas que el corrector todavía rechaza (11 contraída/larga, 1 till,
  media), 3 ejercicios con las dos cosas; antes eran 2.124 ejercicios y 2.791 hallazgos. Examen escribir: 0 de 128, que se
  lee como «los 128 los resuelve el corrector»: en esos ejercicios ninguna forma equivalente queda rechazada. El control de
  que el analizador ve lo que un corrector rechaza son sus pruebas con la tabla vieja (y 123 de 128 con ella). En la
  parte quedan 5 marcas fuera del oro: 5 de las 12 formas (A2, Participios irregulares).
  Next: las 12 formas son reglas que le faltan al corrector (decisión pendiente de R005 sobre el «|»).

2026-09-30 — developer (impl A) — answer-completeness construido y encendido en Main (línea 11): DefaultEquivalentFormGenerator,
  DefaultFormRequestDetector, AnswerCompletenessAnalyzer y su proveedor, sobre la comparación del corrector (DefaultCorrectorMatcher,
  153 de 153 casos de oro) y la tabla de formas irregulares. 36 pruebas en verde (generador 8, detector 6, analizador 16, ficha 1,
  journey 5). Curso del 29/9 con la copia de hoy (tabla vacía): 2.124 ejercicios con hallazgo (A1 630, A2 301, B1 800, B2 393),
  2.791 hallazgos: 1.877 contraída/larga, 85 negación de be, 132 grafía o till, 697 pedidos de forma (654 por regla, 43 a la mesa);
  gravedad 50 alta, 2.005 media, 736 baja. Con la tabla del corrector nuevo (#1191): 706 ejercicios; quedan 12 formas que el
  corrector nuevo sigue sin aceptar (se agregan con «|») y los 697 pedidos. Examen escribir: 123 de 128 (96,1 %, promesa 95 %);
  con la tabla nueva no queda ninguna de las 128.
  Next: integrador; cuando salga el corrector nuevo se cambia la copia de la tabla y se vuelve a medir.

2026-09-30 — qa-tester — patch propose OK, fusionado con el parche del arquitecto (5 altas, 2 cambios, 0 conflictos); `patch validate` y `tech-spec write` OK, con -i contra el modelo después de 0.2 y 0.3. 38 handwrittenTests nuevas (0 portadas: no había tests de estas reglas), las 7 reglas [covered]; F-RESPC-J001 en audit-domain, paquete answercompleteness. Simulación en memoria (scratchpad/qa-A/SimularQA.java): aterrizan las 38, `feature status` OK, generate crearía 5 caminos y 38 stubs en {Impl}Test nuevos, sin choques de nombre. Sin aplicar.
  Next: integrador (A-01, A-02, A-03, A-15, A-16); José aplica sin --as; generate; @test-writer.
  Ojo: con 0.3 aplicado, generate aborta por F-HALL-J001/J002 sin testModule (no es de A: los ubica el qa-tester de 0.3).

2026-09-30 — architect — Parche (5 altas, 2 cambios, 0 conflictos) y TECH_SPEC (7 fragmentos) en exit 0; `patch validate` OK contra el
  modelo después de 0.2 y 0.3. Ganchos en ittm docs/propuestas/tablero-corregir-sin-empeorar/ronda1/ganchos-A.md. Sin aplicar.
  Next: José contesta DOUBT-RESPC-CONTRACCIONES y DOUBT-RESPC-DESCRIBE; el integrador arma el parche de la ronda; feature sync; @qa-tester.

2026-09-30 — analyst — Primer pase de la ronda 1 con las decisiones de José (30/9): sin excepción por consigna (R002, R003), grafías
  y until/till en la tabla del corrector (R004), R007 nueva (consigna o miniteoría que pide una forma), cuatro dudas resueltas
  y dos nuevas (DOUBT-RESPC-CONTRACCIONES, DOUBT-RESPC-DESCRIBE). Medido: quedan 142 de lo que marcaba la regla si el corrector
  iguala las tres clases. Next: @architect (parche y TECH_SPEC de la ronda 1).

2026-09-30 — analyst — REQUIREMENT.md v1 de FEAT-RESPC (paquete A, ronda 1 del brief 089): 6 reglas, 1 journey, 4 dudas.
  Prototipo en el scratchpad: 1.251 ejercicios (sólo sujeto pronombre) o 1.743 (cualquier sujeto) contra 484 del 077;
  76 sólo por grafía. En escribir encuentra 127 de 128 (el que falta, la tilde de «café», lo acepta el corrector).
  Next: José decide DOUBT-CONTRAIDA-LARGA, que define casi todo el volumen; el integrador suma la carpeta y corre
  `feature sync --feature FEAT-RESPC`; después @architect (y la tabla de casos compartida con el backend si va A en DOUBT-RESPC-IMITAR).
