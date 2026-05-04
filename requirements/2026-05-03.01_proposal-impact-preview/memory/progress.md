# Progress

2026-05-03 — analyst — Initial REQUIREMENT.md authored for FEAT-PIPRE (preview de impacto de propuestas).
  why: Llena el gap funcional donde el operador no puede saber el efecto de aprobar una propuesta sin aprobarla y reauditar. Eager por propuesta individual; agnostico de dimension; reglas R001..R012 cubren cuando, que, como se muestra y que pasa si falla. Multiples doubts abiertas para arquitecto y para futuras iteraciones (batch preview, staleness detection, exposicion como recurso CLI).

2026-05-03 — analyst — DOUBT-PRESENTATION-FORMAT cerrado; agregada regla F-PIPRE-R013 (presentacion porcentual al operador) y enganchada al journey J002.
  why: El usuario decidio porcentual. La regla nueva mantiene la disciplina "una regla = un test directo" sin contaminar F-PIPRE-R006 (que sigue siendo sobre los tres valores estructurales antes/despues/diferencia). Doubts pendientes restantes: UNAVAILABILITY-TAXONOMY (architect), BATCH-PREVIEW (feature.future), STALENESS-DETECTION (feature.future), PREVIEW-CLI-EXPOSURE (architect).
