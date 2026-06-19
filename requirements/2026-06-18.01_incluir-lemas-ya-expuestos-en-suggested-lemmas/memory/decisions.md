# Decisions — FEAT-INCEXP

2026-06-18 — analyst — Feature nueva (NO edita FEAT-CSLATDC). Identidad: FEAT-INCEXP / F-INCEXP, prioridad major. Contraste central modelado como dos reglas: R001 (por defecto la prioridad EXCLUYE) vs R002 (con la opción la prioridad sólo ORDENA).
  why: el usuario pidió dejar MUY claro el contraste por-defecto vs con-la-opción; separarlo en dos reglas observables permite testear cada lado de forma directa.

2026-06-18 — analyst — La opción se describe funcionalmente como parámetro booleano del filtro de get suggested-lemmas; no se nombra el flag CLI `--include-exposed` en las reglas (lenguaje de boundary, no de implementación).
  why: restricción del usuario — documento funcional, sin vocabulario de CLI/arquitectura en las reglas.

2026-06-18 — analyst — Dudas DOUBT-STATIC-DUPLICATES-INCEXP y DOUBT-LIMIT-DEFAULT-MAX-INCEXP heredan/extienden las homónimas de FEAT-CSLATDC; se pide consistencia entre ambas features. Se agregó además DOUBT-EXPOSED-TIE-WITH-UNDEREXPOSED (granularidad de prioridad dentro del bloque de ya expuestos).
  why: el usuario pidió referenciar explícitamente las dudas existentes de FEAT-CSLATDC en vez de re-decidirlas aquí.

2026-06-18 — architect — Patch ARCH-INCEXP propuesto y validado (4 add / 4 mod / 0 del / 0 conflicts). Decisiones de transporte y flujo:
  - Carrier SuggestedLemmaQueryCriteria (record refiner-domain root, public): limit, partOfSpeech, explicitLevel, includeExposed=false default. Reemplaza los 3 params sueltos de query()/requestMore(). Motivo: leccion FEAT-LAGAG (merge aditivo NO reemplaza firmas) → un solo punto de evolucion futura.
  - Firmas nuevas: SuggestedLemmaQueryPort.query(report, task, criteria) y SuggestedLemmaQuerySession.requestMore(criteria). Patch expresa firma nueva COMPLETA + _change:delete de la vieja.
  - Universo de candidatos extraido a interfaz interna LeveledLemmaInventory (paquete lemmasuggestion, internal) con 2 impls: UnderexposedLemmaInventory (flujo OFF, fuente historica) y LevelWideLemmaInventory (flujo ON, EvpCatalogPort.getExpectedLemmas + LemmaCountConfig). El port elige por criteria.includeExposed(). Nombre = concepto (inventario), no patron.
  - LemmaAbsenceContextSuggestedLemmaQueryPort: cambia requiresInject — pierde lemmaAbsenceContextResolver (pasa al UnderexposedLemmaInventory), gana underexposedLemmaInventory + levelWideLemmaInventory (dos beans del mismo tipo, DI manual por posicion).
  why: el contraste OFF/ON debe ser trazable por contrato (interface-completeness), no por un if interno en una clase @Generated.

2026-06-18 — architect — RIESGO DE MERGE explicitado al coordinador: aplicar el patch puede dejar conviviendo la firma vieja de query()/requestMore() (merge aditivo). Migrar el impl actual y los tests de FEAT-CSLATDC/FEAT-SLEM al carrier es trabajo de Development; OFF debe quedar bit-a-bit equivalente.
  why: requisito explicito de no romper FEAT-CSLATDC/FEAT-SLEM.

2026-06-18 — architect — DOUBT-EXPOSED-TIE cerrada en diseño (ya-expuestos = 1 grado, COCA asc). DOUBT-STATIC-DUPLICATES-INCEXP y DOUBT-LIMIT-DEFAULT-MAX-INCEXP fuera de alcance: NO se modelo dedupe contra contexto estatico ni clamp/default de limit.

2026-06-18 — architect — Round 2 (aditivo). Patch ARCH-INCEXP-CLI propuesto y validado (0 add / 2 mod / 0 del / 0 conflicts). Unico cambio: campo includeExposed:boolean en SuggestedLemmasFilter (audit-cli), paridad con SuggestedLemmaQueryCriteria.includeExposed, default semantico false.
  why: el flag --include-exposed no tenia por donde viajar desde GetCommand.get(resource,name,SuggestedLemmasFilter) hasta el carrier; los journey tests J001/J002 (audit-cli) lo ejercitan via ese filtro. Round-1 (carrier + LeveledLemmaInventory) ya aplicado y archivado en .applied-patches/.
  nota: agregar el campo regenera el constructor del record a 4-arg → rompe los new SuggestedLemmasFilter(limit,pos,level) existentes; migracion es developer-owned (ya rotos por el cambio de firma del carrier). Mapeo filter→criteria en GetCmd es developer-owned, no modelado.
  nota: TECH_SPEC.md reescrito con --replace para reflejar SOLO el proposal activo (campo CLI). La narrativa de round-1 vive en .applied-patches/ + git; un requirement folder tiene un unico architectural_patch.yaml activo.
