# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-05-11 — analyst — REQUIREMENT.md reformulado para desbloquear FEAT-SLEN
  - 6 reglas retiradas (R006, R007, R010, R011, R014, R015): stats por nivel CEFR
    sin owner conocido ni consumidor concreto. Movidas a nueva seccion en Contexto
    "Analisis de nivel y progresion: fuera de alcance MVP". Ver decisions.md.
  - 2 journeys retirados (J002, J003): dependian 100% de reglas retiradas.
  - J001 y J005 reformulados sin referencias a reglas retiradas (steps reducidos).
  - 3 Doubts marcadas RESOLVED apuntando al retiro:
    DOUBT-LEVEL-STATS-LOCATION, DOUBT-PROGRESSION-GAPS, DOUBT-EQUAL-AVERAGES.
  - IDs retirados (R006/R007/R010/R011/R014/R015/J002/J003) no se reasignan.
  - Estado final: 10 reglas + 3 journeys.
  - Validacion: sentinel requirement validate → [OK].
  - Next: qa-tester confirma que patch parcial sigue valido (no toca reglas retiradas).
    Architect debe proponer patch retirando F-SLEN-J002 y F-SLEN-J003 de
    sentinel.yaml:10048-10056 (definitions/FEAT-SLEN/journeys).
