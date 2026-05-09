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

2026-05-09 — analyst — R002 agregada: contexto de correccion inline opt-in
  en modo efimero. Gap descubierto al integrar el dashboard: get task solo
  opera contra planes persistidos, asi que un plan efimero queda inutilizable
  para hidratar contexto. R002 cierra el gap moviendo el contexto inline al
  plan efimero cuando se activa la opcion. La forma del contexto es la ya
  definida por FEAT-RCSL / FEAT-RCLA (no se redefine), solo cambia el canal
  de entrega. Doubts nuevos: DOUBT-CTX-FLAG-SHAPE, DOUBT-CTX-IN-DISK-MODE,
  DOUBT-CTX-DEFAULT.
  why: el operador necesitaba algo accionable sin abrir un feature nuevo
  para esto: cae naturalmente como extension del modo efimero. La opcion
  alternativa (extender get task para que acepte un plan inyectado por
  stdin) cambiaba la superficie de un verbo distinto y era mas costosa.

2026-05-09 — analyst — Gap 2 (revise con override de contexto) NO se
  absorbe en FEAT-PLANEF; se abre FEAT-REVCTX
  ([requirement](../../2026-05-09.01_revise-correction-context-override/REQUIREMENT.md)).
  why: cambia la semantica del verbo `revise`, que no es plan ni es
  efimero. Las preocupaciones propias (validacion del override, trazabilidad
  del artefacto, interaccion con LAPS, semantica de reemplazo vs merge) son
  semanticamente independientes y mezclarlas en F-PLANEF rompia la cohesion.

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

2026-05-09 — architect — Patch ARCH-PLANEF reescrito para incluir F-PLANEF-R002.
  Decisiones sobre los 3 doubts CTX-*:
  - DOUBT-CTX-FLAG-SHAPE → Opcion A (--with-correction-context booleano).
    Razon: aditivo, simple, ortogonal a --storage; opcion B (--include=...)
    se considera over-engineering hasta que aparezca un segundo opt-in.
  - DOUBT-CTX-IN-DISK-MODE → Opcion A (rechazo explicito con error).
    Razon: simetria con la regla durable del proyecto de no introducir
    comportamientos silenciosos; las opciones B (no-op) y C (persistir
    inline en disk) o son ambiguas o requieren migracion de schema fuera
    de scope.
  - DOUBT-CTX-DEFAULT → Opcion A (default OFF, opt-in).
    Razon: preserva el contrato de R001, deja la decision al cliente,
    confirmacion explicita del operador.
  Cambios estructurales:
  - audit-domain agrega AuditNodeIndex / AuditNodeIndexFactory + package
    interno auditnodeindex (Factory Seam: solo DefaultAuditNodeIndexFactory
    publico, MapAuditNodeIndex package-private). Razon: el indice es
    capacidad pura sobre el arbol del AuditReport, vive donde vive el
    arbol; manana cualquier consumidor puede tomarlo sin atravesar
    refiner-domain.
  - refiner-domain extiende CorrectionContextResolver con resolveWithIndex
    (ambas firmas coexisten: resolve para single-task de GetCmd,
    resolveWithIndex para bulk del renderer). Las 3 implementaciones
    (Sentence, LemmaAbsence, Dispatching) implementan ambas.
  - audit-cli extiende PlanCommand sealed con boolean withCorrectionContext
    (firma vieja borrada, no se mantiene overload). Agrega
    EphemeralRenderOptions (record) + CorrectionContextJsonMapper port +
    DefaultCorrectionContextJsonMapper en commands/. EphemeralPlanRenderer
    cambia firma a render(plan, report, options). PlanCmd inyecta el
    renderer extendido y valida la combinacion DISK + withCorrectionContext
    como rechazo. GetCmd pasa a inyectar el mapper compartido.
  why: la equivalencia de schema R002 #1 entre 'get task' y 'plan efimero'
  es estructural -un solo serializador-, no copiable a mano (dos
  implementaciones que se desincronicen al primer campo nuevo). El indice
  cierra el problema de performance que ya estaba latente en GetCmd
  cuando se quisiera escalar a planes grandes; aprovechamos la apertura
  del feature para resolverlo en `audit-domain` donde corresponde.

2026-05-09 — architect — Escalacion qa-tester (missing_field F-PLANEF-J002/J003): NO requiere patch arquitectonico.
  Resolucion: la CLI rechaza el _change sobre features[].journeys[] con el mensaje
  "features[].rules[] and features[].journeys[] are derived from REQUIREMENT.md
  and cannot be modified via patch. To apply these changes, edit REQUIREMENT.md
  and run `sentinel generate`". J002 y J003 ya estan declarados en REQUIREMENT.md
  con shape valido (flow + outcomes); sentinel generate los materializara
  automaticamente en sentinel.yaml#features.FEAT-PLANEF.journeys cuando se corra
  tras aplicar el patch actual. testModule/testPackage los infiere Sentinel del
  paquete donde aterrice el JourneyTest (precedente: F-PLANEF-J001 quedo con
  testModule=audit-cli, testPackage=com.learney.contentaudit.auditcli.commands
  -via *JourneyTest.java escrito en commit eb8cadb-).
  Bloque features: residual en architectural_patch.yaml (lineas 82-92) -la CLI
  ignora los _change pero deja el bloque pegado en el merge. Es benigno: sentinel
  generate sobreescribe ese bloque con el sintetizado desde REQUIREMENT.md.
  TECH_SPEC.md no se reescribe: contiene la narrativa completa del patch
  arquitectonico de R002 (firma PlanCommand extendida, AuditNodeIndex,
  CorrectionContextResolver con resolveWithIndex, CorrectionContextJsonMapper,
  EphemeralRenderOptions, etc.) ya aplicado en commit a69a81a/eb8cadb. El
  architectural_patch.yaml hoy solo acumula los 6 handwrittenTests de qa-tester
  para R002, asi que tech-spec write contra el patch actual rechazaria todos los
  fences de la narrativa arquitectonica con "no esta en architectural_patch.yaml".
  Conservar TECH_SPEC.md tal cual es la unica via no-destructiva.
  why: la escalacion buscaba un cambio de contrato; pero el "contrato" de los
  journeys no vive en sentinel.yaml editable -vive en REQUIREMENT.md derived.
  Lo que el qa-tester observa como "missing field" es un estado transitorio
  (REQUIREMENT.md cambio, sentinel generate aun no se corrio); sentinel generate
  cierra el ciclo sin necesidad de patch. Comunicado a qa-tester via SendMessage.

2026-05-09 — architect — Asuncion del 2026-05-09 (entrada anterior) FALSADA empiricamente:
  sentinel generate NO materializa F-PLANEF-J002/J003 a pesar de estar bien
  declarados en REQUIREMENT.md. Genera WARNING explicito:
    "REQUIREMENT.md declares journey 'F-PLANEF-J002' under feature 'FEAT-PLANEF'
     which is not listed in sentinel.yaml — skipping for generation
     (add 'F-PLANEF-J002' to features.FEAT-PLANEF.journeys: to materialize it)."
  Re-intentar via patch propose con _change: add sigue rechazado (mismo mensaje
  que antes). Es un deadlock real entre las dos puertas del CLI:
    - patch propose dice "edita REQUIREMENT.md y corre sentinel generate"
    - sentinel generate dice "agregalo a sentinel.yaml manualmente"
  Bug reportado en /Users/josecullen/projects/sentinel/.bugs/in_progress/
    2026-05-09-01-features-journeys-derived-but-not-materialized.md
  Mismo problema con F-REVCTX-J004 (otra implementacion en paralelo).
  Decision pragmatica: avanzar SIN J002/J003 materializados. Las 5 invariantes
  de R002 ya estan cubiertas funcionalmente por los 6 handwrittenTests sobre
  PlanCmd/DefaultEphemeralPlanRenderer (architectural_patch.yaml en applied-patches/
  2026-05-09T16-48...). Los journeys son cobertura adicional path-based que se
  pueden agregar despues de que Sentinel resuelva el bug, editando sentinel.yaml
  manualmente o por la via que el equipo de Sentinel decida.
  why: bloquear el avance esperando un fix de Sentinel cuesta mas que la
  cobertura adicional que J002/J003 daria. Las invariantes #1..#5 de R002 estan
  cubiertas por los 6 handwrittenTests propuestos por qa-tester. La adicion
  posterior de los JourneyTest queda planificada para cuando Sentinel resuelva
  la inconsistencia.
