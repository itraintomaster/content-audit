# Decisions - FEAT-REVCTX

2026-05-09 — analyst — Feature creado como independiente de FEAT-PLANEF.
  why: el operador descubrio dos gaps al integrar el dashboard. Gap 1
  (correctionContext inline en plan efimero) cae naturalmente como
  extension de FEAT-PLANEF (R002). Gap 2 (revise con override) cambia la
  semantica de un verbo distinto (revise) y tiene preocupaciones propias
  (validacion del input externo, trazabilidad del artefacto, interaccion
  con LAPS). Mezclar los dos en FEAT-PLANEF rompia la cohesion del feature
  ("plan efimero / no persistido" no abarca revise).

2026-05-09 — analyst — Reemplazo total, no merge, en R002.
  why: cualquier composicion (override > derivado, derivado > override,
  union, interseccion) introduce semanticas no obvias que el cliente no
  puede predecir. El operador necesita garantia de "lo que vio en la UI
  es lo que se ejecuta". Reemplazo total es la unica semantica donde eso
  se cumple. R002 lo declara explicitamente (#1) y prohibe merge.

2026-05-09 — analyst — Validacion estructural obligatoria (R003), pero la
  coherencia semantica entre override y tarea persistente queda como
  responsabilidad del cliente (R005, DOUBT-OVERRIDE-COHERENCE).
  why: solo el cliente sabe la correspondencia entre la tarea proyectada
  y la persistente; content-audit no puede inferirla. La coherencia
  profunda (e.g. nodeId match) seria una capa de defensa contra errores
  del cliente que vale considerar pero que no puede ser una invariante
  fuerte. Lo dejo abierto a decision en DOUBT-OVERRIDE-COHERENCE; la
  preferencia provisoria es la Opcion B (sanity check minimo de taskId).

2026-05-09 — analyst — R004: el override no aplica a kinds sin contrato
  de contexto (los que caen al bypass).
  why: el bypass produce elementAfter == elementBefore ignorando el
  contexto. Aceptar override sobre esos kinds es darle al cliente la
  ilusion de que su override afecta el resultado cuando no lo hace.
  Mejor rechazar explicitamente.

2026-05-09 — analyst — R006: el override no toca aprobacion ni persistencia.
  why: scope strict. El cambio es localizado: cambia la fuente del
  contexto que la estrategia consume, todo lo demas (FEAT-REVAPR,
  FEAT-REVBYP) opera identico. Esto reduce la superficie de pruebas
  necesarias y deja claro que el feature es minimal.
