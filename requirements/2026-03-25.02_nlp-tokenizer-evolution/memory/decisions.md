# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-05-11 — analyst — Resolucion final de 8 ASSUMPTION y 2 journeys ASSUMPTION
  Aplicacion del diagnostico de 3-buckets que documento qa-tester+architect (entrada
  posterior en este archivo). Confirme `NlpTokenizerConfig` (sentinel.yaml:6668-6679)
  expone `pythonScriptPath`, `cocaDataPath`, `timeoutSeconds` → R021 y R031 SI tienen
  superficie observable; el `[ASSUMPTION]` del documento estaba obsoleto.

  Bucket 1 — REFORMULADAS como AUTO_VALIDATED (la regla SI describe comportamiento
  observable; ancladas a NlpTokenizerConfig):
    - R021: las rutas del script Python y datos COCA se proveen al tokenizador via
      configuracion; el tokenizador las usa y reporta error claro si la ruta no
      existe o no es accesible. Ancla: NlpTokenizerConfig.pythonScriptPath/cocaDataPath.
    - R031: timeout maximo del proceso Python provisto via configuracion;
      superado el limite no se reciben tokens y se emite error citando el valor.
      Ancla: NlpTokenizerConfig.timeoutSeconds.
    - R030: comportamiento de modo reducido cuando SpaCy no disponible. Tokens
      basicos + analizadores conteo OK + analizadores ricos reportan omision +
      informe indica al usuario. La "alternativa via catalogo enriquecido" se
      retiro del cuerpo de R030 y se abrio como Doubt[DOUBT-NLP-FALLBACK-SOURCE]
      OPEN (decision si usar catalogo enriquecido como fuente del modo reducido).
    - J002: AUTO_VALIDATED, ejecuta el comportamiento de R030. Reducido a 4 steps
      (eliminados pasos de "consultar documentacion" que no son del sistema).

  Bucket 2 — RETIRADAS como reglas numeradas (sin comportamiento observable
  adicional al cubierto por otras reglas; movidas a Contexto > "Decisiones de
  simplicidad (fuera del alcance de esta version)"):
    - R018: comunicacion via archivos JSON (transporte interno, no es contrato funcional).
    - R022: procesamiento sincrono (afirmacion negativa "no es asincrono", no testeable).
    - R025: cache persistente en disco (declarado explicitamente "no obligatorio").
    - R027: volumetria del cache (estimacion, no afirmacion testeable).
    - R033: inclusion de tokens de puntuacion/espacios en la salida. El comportamiento
      (isPunct=true, posTag=PUNCT/SYM/SPACE) ya esta cubierto por R001 (campos
      obligatorios) y R004 (esquema UD). R033 solo agregaba la *politica* de
      incluirlos vs filtrarlos, que es decision de scope.

  Bucket 3 — RETIRADO (journey):
    - J003: era inspeccion humana de tokens sin contrato definido del sistema.
      El [ASSUMPTION] mismo admitia "el mecanismo de acceso se definira en la
      implementacion" — sin superficie testeable.

  IDs retirados (R018, R022, R025, R027, R033, J003) NO se reasignan, para no
  romper trazabilidad con commits historicos.
  Validacion: `sentinel requirement validate` → [OK].
  Reglas numeradas finales: 28 (R001-R017, R019, R020, R021, R023, R024, R026, R028-R032).
  Journeys finales: 2 (J001, J002).

  Pendiente para architect: `sentinel.yaml` (definitions/FEAT-NLP en linea ~9952)
  sigue listando F-NLP-J003. Necesita patch para retirar esa entrada (no es
  lane de analyst). Las reglas retiradas no aparecen en `definitions` (solo
  journeys aparecen alli), asi que solo J003 requiere accion del architect.

  Pendiente para qa-tester: proponer handwrittenTests directos para R021, R030,
  R031 y J002 (ahora AUTO_VALIDATED con superficie clara). Las 5 reglas retiradas
  (R018, R022, R025, R027, R033) y J003 dejan de aparecer como NO_TESTS.
  Sobre R030: el test puede testear solo el modo basico (Opcion A del Doubt) hasta
  que se resuelva DOUBT-NLP-FALLBACK-SOURCE. Si despues se resuelve por Opcion B
  (catalogo enriquecido), agregar test adicional.

  why: aplicacion refinada del patron KTLEN R004/R007. La leccion clave que
  aporto architect: **cuando una regla dice `[ASSUMPTION]` literal pero hay un
  modelo/record en sentinel.yaml que materializa el contrato, la regla SI tiene
  superficie**. No presumir bucket unico por la palabra ASSUMPTION en
  REQUIREMENT.md. Por eso R021 y R031 NO fueron al bucket de retiradas, aunque
  su redaccion original los hacia parecer "X es configurable" decoradas.

2026-05-11 — qa-tester — Patch FEAT-NLP: 23 AUTO_VALIDATED + 8 ASSUMPTION escaladas
  - 23 reglas AUTO_VALIDATED cubiertas con handwrittenTests directos:
    * 19 sobre SpacyNlpTokenizer (Grupos A/B/C/D/F).
    * 3 sobre CachedNlpTokenizer (Grupo E).
    * 1 sobre CourseToAuditableMapper (R010 + journey J001 cross-cutting).
  - 8 reglas con Validation: ASSUMPTION escaladas a architect (mismo patrón
    FEAT-KTLEN): R018, R021, R022, R025, R027, R030, R031, R033.
    Propuesta: rutear a analyst para mover a "Decisiones de simplicidad"
    o reformular como observables (R030/R031 podrían serlo).
  - J002 y J003 también con Validation: ASSUMPTION — pending-analyst.
  - Validador: 0 additions, 3 modifications, 0 conflicts.
  - 23/31 reglas testeables con tag directo, 8 reglas en standby pending-analyst.

2026-05-11 — qa-tester + architect — Reclasificación 8 ASSUMPTION en 3 buckets (escalado a analyst)
  - Architect corrigió mi diagnóstico inicial (bucket único): son 3 buckets.
  - Bucket 1 (reformular como AUTO_VALIDATED + ancla): R021 (rutas) y R031 (timeout).
    `NlpTokenizerConfig` record (sentinel.yaml:6657-6668) expone pythonScriptPath,
    cocaDataPath y timeoutSeconds — por lo tanto las reglas SÍ tienen superficie
    observable; el `[ASSUMPTION]` literal del documento estaba obsoleto.
  - Bucket 2 (reformulación parcial + Doubt): R030 (fallback sin SpaCy) mezcla
    parte observable (modo reducido) con parte no-contractual (alternativa via
    catálogo). Propuesta: parte observable → AUTO_VALIDATED, alternativa →
    Doubt[DOUBT-NLP-FALLBACK-SOURCE] OPEN.
  - Bucket 3 (retirar como reglas): R018, R022, R025, R027, R033 — decisiones
    de implementación del adapter sin business surface, mismo patrón KTLEN-R004/R007.
  - J002 depende de R030; J003 sin contrato concreto.
  - Lección operativa: cuando una regla dice `[ASSUMPTION]` literal pero hay
    DSL/record que materializa el contrato, la regla SÍ tiene superficie. No
    presumir bucket único por la palabra ASSUMPTION en REQUIREMENT.md.

2026-05-11 — qa-tester — Patch ampliado FEAT-NLP: +4 tests (R021/R030/R031/J002)
  - Analyst aceptó los 3 buckets propuestos:
    * R018/R022/R025/R027/R033 RETIRADAS, contenido a sección "Decisiones de simplicidad".
    * R021/R030/R031 reformuladas como AUTO_VALIDATED, ancladas a NlpTokenizerConfig fields.
    * J002 reformulada como AUTO_VALIDATED. J003 RETIRADO.
  - 4 nuevos handwrittenTests añadidos al patch consolidado:
    * R021 sobre SpacyNlpTokenizerFactory (rutas configurables).
    * R030 + J002 co-tag sobre SpacyNlpTokenizerFactory (fallback observable).
    * R031 sobre SpacyProcessRunner (timeout configurable).
  - Patch consolidado: 27 handwrittenTests, validado 0 additions, 2 modifications, 0 conflicts.
  - Sentinel CLI prune-warning de J003 confirmado (esperado tras analyst cleanup).
  - 28/28 reglas con tag directo. Cero transitividad.

2026-05-11 — qa-tester — Patch NLP re-emitido limpio (--replace) tras fallo apply por J003 huérfana
  - Apply previo falló: "Patch references journey 'F-NLP-J003' which does not exist in sentinel.yaml".
  - Causa: el patch consolidado tenía un bloque heredado `features: - id: FEAT-NLP - journeys: - id: F-NLP-J003`
    de algún merge previo (probablemente cuando se mergearon los re-tags del analyst).
    El bloque DECLARABA J003 mientras la description decía "retirarla" — yaml inconsistente.
  - Solución: --replace desde cero con SOLO handwrittenTests. Validador limpio.
  - 27 handwrittenTests totales, 0 referencias a J003, 0 bloque features: extra.
  - Lección operativa: cuando un patch combina contribuciones de roles diferentes (analyst editing features
    block + qa-tester adding handwrittenTests), el role guard --as=qa-tester rechaza el apply. Mantener
    los patches de qa-tester ESTRICTAMENTE como handwrittenTests-only.
  - Observado lateralmente: analyst ya retiró F-SLEN-J002 y F-SLEN-J003 de REQUIREMENT.md (sentinel.yaml
    aún las tiene en líneas 10091/10093 — orphan, cleanup pending architect/feature-sync).
