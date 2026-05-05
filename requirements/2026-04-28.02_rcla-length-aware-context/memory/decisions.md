# Decisions

2026-04-28 — analyst — fallback when SentenceLengthDiagnosis is absent: omit numeric fields, set lengthDirection=UNKNOWN, do not fail.
  why: longitude is an enricher; the central LEMMA_ABSENCE info still allows correction.

2026-04-28 — analyst — lengthDirection derived in backend (SHORTEN/LENGTHEN/KEEP_SAME/UNKNOWN), not delegated to LLM.
  why: deterministic prompt; CLI can show a human-readable label.

2026-04-28 — analyst — postponed course-level length trend stats (avg/p50/p90) to a separate feature.
  why: would require new aggregator/diagnosis; out of scope for this iteration.

2026-04-28 — analyst — kept three simple directions (no NEAR_MAX/NEAR_MIN variants).
  why: LLM can infer edge proximity from tokenCount/targetMin/targetMax it already receives.

2026-05-04 — architect — patch ARCH-RCLALEN-001: model-only (1 add LengthDirection enum + 4 add Integer fields + 1 add LengthDirection field on LemmaAbsenceCorrectionContext). No new ports, no new packages, no new implementations.
  why: F-RCLALEN-R003 lee QuizDiagnoses.getSentenceLengthDiagnosis() ya expuesto por FEAT-DSLEN sobre el mismo nodo quiz; no hay cambios de superficie en interfaces.

2026-05-04 — architect — los 4 numericos (tokenCount/targetMin/targetMax/delta) son Integer (boxed), no int como en SentenceLengthCorrectionContext.
  why: F-RCLALEN-R004 requiere ausencia (null) cuando no hay SentenceLengthDiagnosis; en RCSL el contexto solo existe si el diagnostico existe, asi que int es seguro alli pero no aqui.

2026-05-04 — architect — lengthDirection nunca es null (UNKNOWN cubre el caso ausente).
  why: F-RCLALEN-R005 serializa lengthDirection siempre, incluso cuando los numericos se omiten; tener null + UNKNOWN seria redundante y forzaria logica en el formatter.

2026-05-04 — architect — pivote a discriminador unico via lengthDirection: el record sigue declarando los 4 numericos como Integer en el patch, pero el contrato observable (resolver invariante: rellena 5 campos siempre; consumidores invariante: leen los numericos solo si lengthDirection != UNKNOWN) reemplaza la nullability perdida.
  why: bug Sentinel /Users/josecullen/projects/sentinel/.bugs/in_progress/2026-05-04-01-boxed-types-emitted-as-primitives_20260504-222704.md colapsa Integer -> int en el codigo emitido; sin el fix, tokenCount=0/delta=0 (default sin diagnostico) colisiona con KEEP_SAME en el limite inferior con delta=0. R004/R005 quedan realizables al nivel de output observable (JSON, CLI, prompt LLM) pero no al nivel del modelo Java en si.

2026-05-04 — architect — decision: NO se propone patch nuevo a sentinel.yaml. La superficie arquitectonica (enum LengthDirection + 5 fields en LemmaAbsenceCorrectionContext) sigue siendo correcta. El cambio es de contrato semantico del resolver y los formatters / prompt builder, capturable enteramente en TECH_SPEC.md.
  why: agregar un sub-record LengthSignal solo para esquivar un bug del generator es deuda arquitectonica que sobrevive al fix; el discriminador con UNKNOWN ya existe y es suficiente para preservar el comportamiento observable.

2026-05-04 — architect — re-propose del patch ARCH-RCLALEN-001 con descripciones actualizadas (mencionan el bug y el discriminador) para que tech-spec write tenga un architectural_patch.yaml fresco contra el cual validar las 3 fences. La superficie del patch no cambio respecto al snapshot previo.
  why: tech-spec write requiere architectural_patch.yaml en la carpeta del requirement. El snapshot post-apply no satisface esa precondicion. Re-proponer con las mismas estructuras es no-op a nivel de sentinel.yaml.

2026-05-04 — architect — escalada al @analyst sobre la redaccion de F-RCLALEN-R004: el detail dice "los campos numericos quedan ausentes (nulos en el registro)", lenguaje que filtra la implementacion (Integer nullable en Java). El comportamiento observable (omision en JSON / CLI / prompt LLM) se preserva via el discriminador independientemente de la representacion interna. Sugerencia: reformular a "los campos numericos no se exponen al consumidor cuando no hay diagnostico de longitud", ortogonal a si la representacion interna usa null o un discriminador.
  why: REQUIREMENT.md es propiedad del @analyst; el architect no edita reglas. La reformulacion alinea R004 con el resto del requirement (R005/R006 ya hablan del output observable, no del estado interno) y desacopla la regla del bug del generator.

2026-05-04 — analyst — accepted handoff: F-RCLALEN-R004 reformulada al limite observable. Texto final del summary: lengthDirection se establece a UNKNOWN, y este es el indicador unico de que no hay diagnostico de longitud disponible; tokenCount/targetMin/targetMax/delta no se exponen al consumidor. Detail enumera los tres canales observables (JSON via R005, prompt LLM, CLI `(unavailable)` via R006). Removido todo vocabulario de implementacion (nulo/registro). ID, severity (major) y validation (AUTO_VALIDATED) preservados. Resto del documento intacto: R002/R005/R006 y los Journeys ya hablaban observable y siguen coherentes.
  why: cierra la escalada del architect; la regla queda independiente del bug del generator y del eventual fix (no requiere cambios futuros si Sentinel pasa a soportar boxed reales). Ref: bug Sentinel /Users/josecullen/projects/sentinel/.bugs/in_progress/2026-05-04-01-boxed-types-emitted-as-primitives_20260504-222704.md.
