# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-05-10 — analyst — Resuelta DOUBT-CEFR-FROM-NLP: la regla R010 se mantiene como contrato latente (fallback condicional).
  Ahora el texto de R010 deja explicito que aplica solo si el procesamiento linguistico provee CEFR; si no lo
  provee, el lema cae a R011 sin error. La cadena queda explicita: R009 (EVP) -> R010 (NLP si esta) -> R011 (no asignado).
  why: no comprometemos al sistema a que NLP siempre devuelva CEFR; si la implementacion concreta del tokenizer
  no expone CEFR, R010 queda inactiva pero el sistema sigue siendo correcto via R011. Asi evitamos que la
  feature dependa de una asuncion no verificada sobre el tokenizer.

2026-05-10 — analyst — Resuelta DOUBT-N-VALIDATION-BEHAVIOR: ante N invalido, la carga de configuracion falla y el analisis no se ejecuta.
  R007 y R008 se separaron en intencion: R007 cubre el caso "campo ausente -> default 4" (carga OK); R008 cubre
  "campo presente pero invalido -> falla al cargar". No hay fallback silencioso al default. Severidad de R008
  promovida de medium a high por ser una condicion de carga.
  why: privilegiar comportamiento explicito y reproducible. Si el usuario intento configurar N pero su valor
  es invalido, debe enterarse y corregirlo, no que el sistema lo ignore o use un valor que no eligio.

2026-05-10 — analyst — Rediseno completo del REQUIREMENT.md respecto del legacy y la version anterior.
  Cambios clave: (1) universo restringido a content-words con count >= 1 (ausencia delegada a FEAT-LABS),
  (2) metrica = oraciones distintas (no tokens; repetir el lema en la misma oracion suma 1),
  (3) eliminada la categoria "sobre-expuesto" y la formula de degradacion 1/50,
  (4) score por lema = min(count/N, 1.0), N configurable, default 4,
  (5) asignacion CEFR del lema con cadena EVP -> NLP -> "no asignado",
  (6) score curso = promedio simple de niveles CEFR; cada nivel pesa igual; "no asignado" no entra al curso.
  why: el rediseno separa responsabilidades con FEAT-LABS, evita falsos positivos por sobre-exposicion natural
  de lemas basicos, y cambia la unidad de exposicion a "contextos distintos" (oraciones), que es lo que el
  estudiante percibe como repeticion real.

2026-05-10 — architect — Propuesta arquitectura FEAT-LCOUNT en patch ARCH-LCOUNT-001.
  Decisiones: (1) paquete lemmacount [internal] en audit-domain siguiendo patron lrec/labs;
  (2) LemmaCountConfig en module root, sealed, extends SelfDescribingConfig;
  (3) cadena CEFR extraida a LemmaCefrLevelResolver (port + impl EvpThenNlpLemmaCefrLevelResolver)
  para aislar R009/R010/R011; (4) EvpCatalogPort gana lookupLevel(LemmaAndPos): Optional<CefrLevel>;
  (5) diagnoses tipados a nivel Course y Level (LemmaCountCourseDiagnosis/LemmaCountLevelDiagnosis);
  (6) UnassignedLemmaEntry no tiene score por tipo (impone R014 a nivel modelo);
  (7) courseScore: Optional<Double> para reflejar el caso "ningun nivel con score" (R015).
  why: minima superficie publica (solo el analyzer es visibility:public), un seam por capability
  (conteo vs. resolucion CEFR), y composabilidad con AnalyzerRegistry sin tocar wiring.

2026-05-11 — architect — Patch ARCH-LCOUNT-002 propuesto (mergeado al patch del feature).
  Cambio: introduce LemmaCountConfigLoader (port en audit-application) + DefaultLemmaCountConfigLoader
  para habilitar superficie observable de F-LCOUNT-R007 y F-LCOUNT-R008.
  DefaultLemmaCountConfig gana requiresInject:threshold:int (constructor parametrizado).
  why: R008 exige rechazo de inputs no-parseables ("abc", "1.5", "") que un constructor (Integer) no
  cubre — la regla habla literalmente de "la carga de la configuracion falla", asi que vive en el
  acto de cargar/parsear, no en el POJO. Opcion C elegida sobre A (constructor) y B (factory
  generica): mismo seam por capability (carga) sin introducir una capa abstracta.
  Implica wiring change en Main.java: pasar de `new DefaultLemmaCountConfig()` a
  `new DefaultLemmaCountConfigLoader().load(null)` (trabajo del developer post-apply).
  Escalacion de R010 (qa-tester) en discusion: propuesto Camino 4 (cambiar firma del resolver a
  resolve(LemmaAndPos, Optional<CefrLevel> nlpCefr)) vs Camino 1 de qa-tester (agregar cefrLevel a
  NlpToken). Sin confirmacion aun de qa-tester; patch de R010 pendiente.

2026-05-11 — architect — Patch ARCH-LCOUNT-003 propuesto (mergeado al patch del feature).
  Cambio: firma del port LemmaCefrLevelResolver cambia de
    resolve(LemmaAndPos): Optional<CefrLevel>
  a
    resolve(LemmaAndPos, Optional<CefrLevel> nlpCefr): Optional<CefrLevel>
  Habilita superficie observable para F-LCOUNT-R010 (EVP miss + NLP con CEFR -> usar NLP).
  why: R010 era contrato latente porque NlpToken no expone CEFR. Camino 4 elegido sobre Camino 1
  (agregar campo cefrLevel a NlpToken): blast radius mucho menor, NlpToken queda puro NLP, y la
  condicionalidad de R010 vive en la firma del port (donde la regla vive arquitecturalmente) en
  lugar de un campo "fantasma" del modelo que ninguna implementacion del tokenizer llena hoy.
  Sigue P3 (versatilidad on demand): activar R010 en runtime el dia que un tokenizer entregue CEFR
  se vuelve un cambio de wiring local al analyzer (mapear NlpToken -> Optional<CefrLevel>), sin
  tocar el modelo. Runtime sin cambio: LemmaCountAnalyzer invoca con Optional.empty().
  Implica ajuste de 1 linea en LemmaCountAnalyzer.java (trabajo del developer post-apply).

2026-05-11 — qa-tester — Decisión R007/R008: la regla "campo ausente -> default 4" (R007) vive en `DefaultLemmaCountConfigLoader.load(null)`, no en el POJO `DefaultLemmaCountConfig`. El POJO solo expone un threshold ya validado e inyectado; el default es responsabilidad del loader (es donde se decide cómo manejar la ausencia del campo en la entrada cruda).
  why: separar "qué hace el POJO con el valor recibido" de "cómo se obtiene el valor inicial". Antes R007 estaba taggeado en el POJO con test trivial "getThreshold==4"; ahora es un test de comportamiento del loader frente a input null.

2026-05-11 — qa-tester — Decisión R010: la regla "fallback NLP CEFR" se materializa cambiando la firma de `LemmaCefrLevelResolver.resolve()` a `(LemmaAndPos, Optional<CefrLevel> nlpCefr): Optional<CefrLevel>`, en lugar de agregar campo `cefrLevel` a `NlpToken`.
  why: NlpToken es modelo compartido por todos los analyzers (sentence-length, knowledge-title-length, etc.); contaminarlo con un campo que ningún tokenizer real llena hoy ni en el futuro inmediato amplifica blast radius innecesariamente. La condicionalidad de R010 vive en el contrato del port (donde la regla vive arquitecturalmente), no en el modelo. Runtime sin cambio: el analyzer pasa Optional.empty() y R010 sigue siendo "latente" en ejecución pero deja de ser latente en el contrato (testeable).
