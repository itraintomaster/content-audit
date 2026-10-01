# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-10-01 — equipo de una tarea (FILAS) — Construido y en verde, en ronda2/filas-claras.
  - apply de los tres parches (FILAS 3+10, RONDA1 4, RONDA2 2): el diff de sentinel.yaml contra HEAD es sólo lo
    pedido (patrones, allowedClients y domainLinks intactos); generate restaurando los pom.xml, SpacyProcessRunner y
    HttpOllamaApi. sentinel verify (sobre una copia en la raíz): VERIFY OK.
  - Cuerpos: nombres y frases en las 20 fichas; DefaultGoalBook con el nombre y la frase de las dos filas generales;
    el comparador con Legend (nombre y frase), changeVerdict y RowExamples (ayudante, como ReportTree); el
    formateador escribe example y explanation aunque sean null (mix-in de Jackson); get analyzers muestra la frase y
    ensancha a 48 la columna del nombre en la tabla.
  - Los cuatro renombres no los materializó generate (bug del @Tag repetido): se renombraron a mano, nombre derivado
    del display name.
  - Mutación de control: invertir el veredicto o la preferencia por lo arreglado tumba 4 pruebas.
  - JSON de muestra: audit-cli/src/test/resources/compare/filas-claras-sample.json, del comando real
    (DefaultCompareCommandTest); el test lo compara byte a byte y deja la salida en audit-cli/target/compare-sample.json.
  Next: que José haga push; la app se arma contra el JSON de muestra.

2026-10-01 — equipo de una tarea (FILAS) — Requerimiento y parches propuestos sobre ronda2/integracion (c10506d3),
  en la rama ronda2/filas-claras.
  - FEAT-FILAS: 3 altas (BetterDirection, ChangeVerdict, RowExample) y 10 modificaciones; valida.
  - Los renombres de las cuatro pruebas que citaban nombres viejos van en dos parches aparte, en las carpetas de
    FEAT-RONDA1 (3) y FEAT-RONDA2 (1): Sentinel rechaza pruebas de otro feature en un parche.
  Next: apply de los tres, generate, cuerpos y pruebas.
