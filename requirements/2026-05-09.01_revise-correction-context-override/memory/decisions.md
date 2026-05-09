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

2026-05-09 — analyst — DOUBT-OVERRIDE-SHAPE cerrado: opcion C (dos flags
  mutuamente excluyentes, --correction-context y --correction-context-file).
  why: cubre los dos casos de uso reales (inline para programatico chico,
  archivo para integraciones que ya tienen materializado el contexto o
  que exceden los limites de la shell). La exclusion mutua evita
  ambiguedad. Quedo formalizado en R001 con su mensaje de error.

2026-05-09 — analyst — DOUBT-OVERRIDE-CHANNEL cerrado: opcion A (JSON con
  forma espejo de get task).
  why: el cliente del dashboard ya consume get task y reusa su
  serializador. Pedirle JSON con la misma forma elimina una capa de
  transformacion y habilita el flujo "copiar-pegar la salida y editar
  los campos cambiados". Quedo formalizado en la regla nueva R008.

2026-05-09 — analyst — DOUBT-OVERRIDE-COHERENCE cerrado: NO con la opcion
  B original (taskId). El operador la rechazo y propuso B' (nodeId +
  diagnosisKind). Implementado.
  why: dos planes distintos sobre el mismo AuditReport reasignan taskId
  por reglas internas. Comparar por taskId rechazaria el caso de uso real
  (override proveniente de un plan proyectado, tarea persistente con otro
  taskId). La identidad LOGICA estable entre planes es (nodeId,
  diagnosisKind). Comparar por esa dupla atrapa el error mas comun
  (cliente pega el contexto equivocado) sin rechazar el caso valido.
  La frase semantica que el operador quiere preservar: "es la misma
  tarea pero con el contexto actualizado". R003 reescrito; R005
  reformulado para no contradecirse (antes decia que el sistema NO
  validaba nodeId; ahora SI valida nodeId + diagnosisKind).

2026-05-09 — analyst — DOUBT-OVERRIDE-AUDITABILITY cerrado: opcion A
  (snapshot literal + indicador de origen).
  why: cuando una propuesta resulta cuestionable, la auditoria humana
  necesita LEER el contexto exacto que el sistema recibio, no solo
  verificar integridad. Un hash sirve para integridad pero no para
  diagnosis. El indicador de origen (derived vs override) permite
  diferenciar fallos del derivador de fallos del cliente. Tamano del
  artefacto se acepta como costo del feature. Quedo formalizado en
  la regla nueva R007.

2026-05-09 — analyst — DOUBT-OVERRIDE-MUTACION-LATERAL cerrado: opcion A
  (la estrategia confia, el sistema no re-deriva ni cruza-valida).
  why: confirma el modelo "el cliente sabe". Las dos UNICAS defensas
  son: R007 (auditabilidad a posteriori) y R003 (sanity check de
  identidad logica antes de invocar la estrategia). Eso es intencional
  — defensiva mas profunda romperia el invariante "lo que el operador
  vio en la UI es lo que la estrategia consume". Quedo reflejado en
  R002 #3 reformulado.

2026-05-09 — architect — Patch ARCH-REVCTX-001 propuesto y aplicado por
  el usuario via Architect Studio. 11 secciones / 11 fences en TECH_SPEC.md.
  why: traduje las 6 reglas + 5 doubts cerrados a la mas chica superficie
  arquitectural posible. Decisiones clave que NO son obvias del REQUIREMENT:
  (a) el parser vive en revision-domain.contextoverride (paquete interno),
  no en una infra propia ni en audit-cli — el modulo gana una dependencia
  formal de Jackson 2.17.2 alineada al BOM Spring Boot del proyecto;
  (b) supports(DiagnosisKind) entra al port CorrectionContextResolver
  en refiner-domain como UNICA fuente de verdad de "este kind tiene
  contexto" (R004); el engine consulta supports() antes de invocar al
  parser, evita duplicar el set de kinds en revision-domain;
  (c) el override viaja como overload nuevo de RevisionEngine.revise(...)
  con un parametro String overridePayload — NO se modela como Optional
  ni como flag booleano: dos firmas distintas, una con derivacion y otra
  con override. Reviser y RevisionProposal quedan totalmente opacos al
  feature (Q5 del usuario); el origen del contexto vive en RevisionArtifact;
  (d) extraje CorrectionContextStructuralValidator como port plug-in por
  DiagnosisKind (LemmaAbsenceContextStructuralValidator,
  SentenceLengthContextStructuralValidator) para que un nuevo kind
  (futura estrategia) solo agregue un validator nuevo sin tocar el parser.
  R007 (trazabilidad explicita) y R008 (JSON espejo get task) que el
  analyst agrego DESPUES de mi diseno quedan cubiertas sin patch
  adicional: contextSource + contextOverridePayload en RevisionArtifact
  satisfacen R007; R008 es constraint del impl del parser, sin
  elementos sentinel nuevos.

2026-05-09 — architect — Intento de patch para registrar F-REVCTX-J004 en
  features.FEAT-REVCTX.journeys rechazado por la CLI: `features[].rules[]`
  y `features[].journeys[]` son derivados de REQUIREMENT.md y no se pueden
  modificar via architectural_patch. Mensaje literal: "Patch had no
  applicable changes after dropping unsupported features[].rules[]/
  journeys[] _change operations." Resolucion correcta: el usuario corre
  `sentinel generate` y la journey J004 (ya declarada en REQUIREMENT.md
  lineas 301-318) se promueve automaticamente al bloque
  features.FEAT-REVCTX.journeys de sentinel.yaml.
  why: el patch arquitectural cubre estructuras (modulos, modelos,
  interfaces, paquetes, dependencias), no la sincronizacion de
  reglas/journeys con REQUIREMENT.md. Esa sincronizacion es responsabilidad
  de `sentinel generate`. Lo mismo aplica a F-PLANEF-J002 y F-PLANEF-J003,
  declaradas en REQUIREMENT.md y aun no promovidas — un solo `generate`
  cubre los tres casos.

2026-05-09 — architect — Implement-team kickoff: contracts confirmed settled.
  why: revisé el patch aplicado, los stubs generados y el estado del build
  antes de delegar. El contrato esta fijo y no necesita escalation; lo que
  falta es solo cuerpo. Confirme: (1) RevisionEngine.revise ahora es
  4-arg (planId, taskId, coursePath, overridePayload) — la firma anterior
  fue REEMPLAZADA, no overload; el build ya rompe en DefaultRevisionEngine
  (linea 80, sigue declarando la firma vieja) y en DefaultRevisionEngineFactory
  (linea 42, no pasa el parser). (2) RevisionArtifact creció a 8 args con
  contextSource + contextOverridePayload; el constructor de 6-args en
  DefaultRevisionEngine linea 155 no compila. (3) ReviseCommand interface
  paso a 4-arg revise(taskId, planId, ctxJson, ctxFile); ReviseCmd.revise
  sigue con 2-arg — picocli rompe. (4) supports(DiagnosisKind) ya esta en
  CorrectionContextResolver y DispatchingCorrectionContextResolver lo
  implementa; el test stub R004 esta listo. (5) Main.java construye
  RevisionEngineConfig pero no setea correctionContextOverrideParser —
  composition root tambien necesita cambio. La simetria critica para
  R008: el productor del shape "get task" es DefaultCorrectionContextJsonMapper
  (audit-cli.commands); el parser es el consumidor inverso. Cualquier
  divergencia rompe R008.

2026-05-09 — architect — Restriccion del entorno: este preset spawnea
  architect/developer/test-writer pero SOLO architect tiene tooling.
  SendMessage y Task NO estan en mi toolbox. Devuelvo los briefs en mi
  respuesta final y el lead los rutea. Si en futuras sesiones el preset
  pasa a in-process con SendMessage habilitado, tomamos los mismos
  briefs como mensajes directos.
