# Decisions - FEAT-PLANEF

2026-05-07 — analyst — Feature reescrito de scope grande a scope minimo.
  why: el primer draft (FEAT-DSUGW) modelaba content-audit como responsable
  de orquestar la simulacion de propuestas, calcular consumo de lemas y
  matchear cross-plan. El operador clarifico que todo eso lo hace el
  cliente externo: content-audit solo necesita exponer un modo de
  invocacion no-persistente del comando `plan`. El feature pasa a tener 3
  reglas en lugar de 10.

2026-05-07 — analyst — Nombre final: FEAT-PLANEF / F-PLANEF
  ("Plan efimero / no persistido").
  why: el operador prefiere nombres descriptivos del concepto sobre
  acronimos del patron. "Plan efimero" describe el concepto: existe solo
  durante la invocacion, no se persiste. La sigla queda sucinta y legible.

2026-05-07 — analyst — Doubts del primer draft eliminados.
  why: DOUBT-CONSUMPTION-SCOPE y DOUBT-CROSSPLAN-MATCHING desaparecieron
  porque ya no son responsabilidad de content-audit. Ambos vivian de
  asumir que content-audit modelaba "consumo de lemas" y "matcheo entre
  planes", cosas que el cliente externo cubre por otra via.

2026-05-07 — analyst — Doubts nuevos, todos no-bloqueantes.
  why: DOUBT-FLAG-SHAPE (forma del flag), DOUBT-DIAGNOSTIC-OUTPUT (canal
  de mensajes auxiliares), DOUBT-OTHER-COMMANDS (modo efimero para otros
  verbos). Ninguno bloquea el avance a arquitectura: la regla R001 obliga
  la existencia del modo, no el shape exacto.

2026-05-07 — analyst — La nocion "matcheo por nodeId" se documenta como
  Uso esperado, no como regla testeable.
  why: el matcheo lo hace el cliente, content-audit no lo verifica ni lo
  produce como output. Documentado en la seccion "Uso esperado" para
  contexto del lector, sin imponer obligaciones.

2026-05-08 — analyst — Reglas refundidas: R001 + R002 → unica R001 con
  tres invariantes observables (no escribe, emite por stdout, ortogonal
  al resto de los flags). R003 (no deja artefactos parciales) eliminada.
  why: el operador pidio recortar al maximo. R002 era el contrato del
  output del mismo flag que R001 ya describia; mantenerlas separadas era
  redundancia. R003 era over-specification: si el modo es "no persistir",
  la propiedad para el caso de error es emergente, no regla aparte.

2026-05-08 — analyst — Investigacion de contratos del CLI: `plan` ya
  admite `--audit <id>` para apuntar a un analisis explicito sin tocar
  la ActiveAnalysisSelection.
  why: confirmado en F-CLIRV-R014 ("The audit source can be selected
  with --audit <id>"). El cliente puede persistir su analisis simulado
  en su propio workdir (combinable con --workdir / CONTENT_AUDIT_HOME
  via F-CLIRV-R017) y apuntarle plan. No hace falta regla nueva en
  F-PLANEF. Documentado en "Uso esperado" como nota explicita.

2026-05-08 — analyst — Por que reescrito en clave ergonomica del
  operador (no en clave filesystem).
  why: el operador clarifico que la motivacion no es "no contaminar el
  filesystem" (consecuencia tecnica) sino "ver el plan como se veria si
  las decisiones tomadas estuvieran aplicadas, sin aplicarlas todavia".
  Caso concreto: dos tareas consecutivas sugieren la misma palabra. La
  consecuencia de no contaminar el historial sigue presente pero baja a
  segundo plano.

2026-05-08 — architect — Patch arquitectonico propuesto (FEAT-PLANEF / ARCH-PLANEF).
  Decisiones sobre los 3 doubts:
  - DOUBT-FLAG-SHAPE → Opcion A (--storage=<mode>) modelado como enum
    PlanStorageMode {DISK, EPHEMERAL}. Razon: extensibilidad sin romper
    contrato (vs booleano), per-invocacion (vs env var).
  - DOUBT-DIAGNOSTIC-OUTPUT → en modo EPHEMERAL stdout queda reservado para
    JSON; banner/info van a stderr. Modo DISK sin cambios.
  - DOUBT-OTHER-COMMANDS → Opcion A: solo plan. Generalizar a analyze/revise
    sin caso de uso concreto seria over-engineering (P3). El shape del flag
    queda reusable si en el futuro se repite el patron.
  Cambios contenidos en audit-cli: agrega PlanStorageMode (enum) y el puerto
  EphemeralPlanRenderer + DefaultEphemeralPlanRenderer (ambos package-private
  en commands/). Modifica firma sealed PlanCommand.plan() para llevar el modo
  explicito. Inyecta EphemeralPlanRenderer en PlanCmd. Cero modulos nuevos,
  cero deps nuevas, cero cambios de boundary.
  why: el operador habia recortado el scope al maximo en la fase analista;
  el patch tenia que reflejar esa contencion. La opcion alternativa
  (introducir un InMemoryRefinementPlanStore no-op) enmascaraba la invariante
  R001 #1 detras de un swap de adapter; mejor que PlanCmd decida no llamar a
  save segun el modo, asi el test queda directamente apuntable.
