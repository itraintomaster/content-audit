# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-10-01 — qa-tester (impl A) — Simulado en una copia de la integración: generate no agrega los stubs de los dos nombres
  nuevos y preserva los métodos viejos, con un aviso para cada uno («DSL no longer declares this name»). Se renombran a mano
  al encender; las 31 pruebas de answercompleteness siguen en verde mientras tanto.

2026-10-01 — qa-tester (impl A) — Segundo parche de FEAT-RESPC, sólo nombres: «has trained» (R005) y la ficha (R006) se
  borran con el nombre viejo y se agregan con el que dice lo que hace el código. propose y `patch validate -i` contra
  859e96af OK (0 altas, 3 cambios). TECH_SPEC reescrito para este parche, con --replace. Después de generate, los dos
  métodos se renombran a mano: generate no agrega sus stubs (bug 2026-07-22-01).

2026-10-01 — analyst (impl A) — R006 suma «sólo ordena» a las resoluciones de la ficha y dice que el puntaje baja sólo
  con regla o mesa, como hace el código desde 3a3238ed. Next: qa-tester, los nombres de las dos pruebas que lo
  contradicen («has trained» y la de la ficha).

2026-10-01 — developer (impl A) — Los límites del corrector ya no proponen «|»: las 12 formas son sólo ordena, media, con la regla
  que le falta al corrector (10 «'s»/«'d» ambiguo, 1 overslept, 1 till después de «there»). Curso del 29/9: 706 ejercicios con
  hallazgo de answer-completeness, 694 con error (los pedidos de forma) y 12 marcados; «algún error» del curso, 1.216
  (10,8 %); vocabulario 73,92 % sin cambios.

2026-10-01 — developer (impl A) — Las 12 formas que el corrector en producción sigue rechazando son las 12 reglas que le
  faltan (clase a), ninguna es otra respuesta ni una forma que nadie escribe: 7 «'s» + participio que el corrector no sabe
  si es is o has (he's forgotten, he's turned, she's left, he's meant, she's danced, he's run, that's produced sin «by»), 3
  «'d» + participio igual a la base que no sabe si es had o would (she'd read ×2, he'd run), 1 participio que no está en su
  lista cerrada (overslept: lee «he would overslept») y 1 till después de «there», que el corrector toma por sujeto. Hoy el
  analizador las emite media, por regla, con «se agrega con «|» después de las que hay» (la prueba de «has trained» del
  TECH_SPEC); R005 dice que una forma de una clase que iguala el corrector se arregla en el corrector, y el brief 101 deja
  el «|» sólo para respuestas distintas: decide el coordinador, R005 no se tocó. El generador escribía en minúscula los
  nombres de la forma («in switzerland»): ahora les devuelve la mayúscula.

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
