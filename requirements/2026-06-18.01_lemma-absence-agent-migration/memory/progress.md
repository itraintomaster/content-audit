# progress

2026-06-19 — architect — Patch F-LASAG propuesto (NO aplicado) y validado.
  Salida: "Validated: 2 additions, 4 modifications, 4 deletions, 0 conflicts."
  Proposal: requirements/2026-06-18.01_lemma-absence-agent-migration/architectural_patch.yaml
  Baseline congelado: .../sentinel-baseline.yaml
  Reemplaza lagenopenai+lageninteractive por paquete interno lemmaabsenceagent; agrega dep com.sentinel:sentinel-agents:0.0.1-SNAPSHOT; retira LagenMode.LLM_INTERACTIVE; agrega maxEvalRetries a LagenConfig.
  Siguiente: escribir TECH_SPEC.md (español). Pendiente externo: @qa-tester para handwrittenTests; el grafo declarativo del agente (.sentinel/agents/) lo construye sentinel-agent fuera de sentinel.yaml.

2026-06-19 — analyst — Requirement-domain: reformulación de validación + retiro de journeys absorbidos + sync F-LASAG.
  R007-R010 → Validation VALIDATED (+nota "validada por harness de evals, no por suite JUnit"): el DSL ValidationStatus solo tiene VALIDATED|AUTO_VALIDATED|ASSUMPTION; no hay valor para "validado por evals del agente". AUTO_VALIDATED afirmaría cobertura JUnit (falsa aquí); VALIDATED = confirmado por humano/harness, el más fiel. Blockquotes describen conducta de calidad/reintento observada por el consumidor (lenguaje funcional); la superficie de validación (harness de evals) solo se nombra en los Detail.
  Mitades precisadas: R004 (conteo ≤N interno=graph, "agotar no es falla"=AUTO_VALIDATED), R011 (default 3=AUTO_VALIDATED, "CANNED ignora maxEvalRetries"=interno), R012 (categoría+falla inmediata=AUTO_VALIDATED, distinción de fase/no-retry=interno). Las 3 quedan AUTO_VALIDATED (su mitad observable la cubren los 9 handwrittenTests; NO tocados).
  Journeys F-LAGEN-J001..J006 y F-LAGAG-J001..J003 retirados de sus REQUIREMENT.md (reemplazados por nota de absorción a F-LASAG). `sentinel feature sync` es ADITIVO: NO podó los 9 refs colgantes a lagenopenai/lageninteractive; dice "will be pruned at next generate". Poda = `sentinel generate` (fuera de lane analyst). ESCALADO a @architect.
  F-LASAG: el bloque combinado de 9 journeys en un solo fence yaml leía 0 journeys en el parser. Reescrito a formato estándar del proyecto (1 `### Journey[...]` + 1 fence single-entry por journey). Sync registró los 9 (AUTO_VALIDATED, sin testModule/testPackage → placement = qa-tester).
  Bug latente corregido: J004 tenía un ciclo (evaluar→reintentar→evaluar); V-MBT-08 prohíbe ciclos. Desenrollado a evaluar_primer_intento→reintentar→evaluar_reintento (DAG). Nunca se detectó antes porque el parser jamás leyó el bloque combinado.
