# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->
2026-07-21 — analyst — dos correcciones al criterio de consulta de nivel sobre el catalogo enriquecido (falsos positivos Grupo D, evidencia audit 2026-07-20). R040 (formas derivadas/compuestas cuyo headword difiere del lema no fijan el nivel del lema base; hermana de R005; 246/254 ocurrencias). R041 (categoria autoritativa = EVP declarado, traducido a etiquetas universales; spacyPos como respaldo solo si EVP vacio; 26,4% entradas con POS calculado contradictorio). "phrase" monopalabra ("let's") fuera de alcance -> Doubt[DOUBT-PHRASE-MONOPALABRA] OPEN, diferido por el usuario.
  why: cada comportamiento en regla propia y testeable (no cobertura transitiva); R039/R034/R036 semantica intacta.
2026-07-16 — qa-tester — re-baseline por _change: delete (no modify) para los 4 tests cuya afirmacion quedo invertida (LemmaAndPos-absence, non-EVP sin penalidad x2, piso 0.7); renombrar el metodo rompe el smart-merge de stubs, delete+test nuevo es el camino seguro.
2026-07-16 — qa-tester — R036 paso 2 y R033-retencion se testean en FileSystemEvpCatalog (superficie real del port); precedencia paso1>paso2 y bandas R034 en LemmaByLevelAbsenceAnalyzer; propagacion R038 en LemmaAbsenceContextResolver (patron cross-feature ya usado con FEAT-RCLA).
2026-07-16 — qa-tester — pendiente ajeno detectado: feature f0b4e57b (F-LEPV) con journey UUID sin REQUIREMENT.md que lo respalde — missing-placement preexistente en todos los baselines; NO se le propuso placement (seria forzado), corresponde a analyst/usuario decidir sync o limpieza.
