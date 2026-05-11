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
