# Decisions — FEAT-SLREGEX

2026-06-26 — analyst — Decisiones de diseño fijadas con el usuario (no como dudas):
  - El regex matchea SOLO contra el texto del lema (forma superficial), no PoS ni otros campos (R002).
  - Coincidencia PARCIAL estilo grep, no anclada; el cliente ancla con ^ y $ (R003).
  - Case-insensitive (R004).
  - Filtro ADITIVO: se combina con Level/PoS/limit/include-exposed sin relajarlos (R005);
    preserva orden por prioridad (R006); limit recorta DESPUÉS del regex (R007).
  - Regex inválida → rechazo con validación clara, sin reventar (R008).
  - Sin match → lista vacía con explicación, sin error (R009).
  - Default sin --regex idéntico a hoy (R001).
  - Documentación de la semántica en --help es REQUISITO OBSERVABLE (R010), por pedido explícito.

2026-06-26 — analyst — Esta feature supera deliberadamente el OUT de FEAT-CSLATDC y
  FEAT-INCEXP ("no se agregan filtros léxicos textuales"): introduce el primer filtro
  léxico textual. Documentado en Context y References.
  why: que un futuro lector entienda que el OUT previo fue una decisión revisada, no un olvido.

2026-06-26 — analyst — Textos de error (R008, R009) quedan como [ASSUMPTION];
  dialecto exacto de regex queda como DOUBT-REGEX-DIALECT.
  why: el usuario fijó la semántica observable pero no la redacción exacta ni el dialecto fino.

2026-06-26 — architect — Patch propuesto y validado (architectural_patch.yaml): se agrega
  un solo campo regex: Optional<String> a DOS carriers existentes — SuggestedLemmasFilter
  (audit-cli) y SuggestedLemmaQueryCriteria (refiner-domain). Sin interfaces/impls/módulos/deps nuevos.
  why: regex es un eje de filtrado ortogonal (texto del lema) que pertenece a los mismos
    records que ya transportan level/partOfSpeech/limit/includeExposed; Optional<String>
    espeja partOfSpeech y deja el default (sin --regex) idéntico (R001). Validó 0 conflicts.
  nota: el cableado (parseo de --regex, propagación filter→criteria, match real, R008/R009/R010)
    es de @developer; arquitectura sólo abre el campo. Patch NO aplicado (lo aplica el usuario).
