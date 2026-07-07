# Decisions — FEAT-LEMMA-SUGGESTIONS (F-SLEM)

2026-05-16 — analyst — Observable shape of lemma-count inside suggestedLemmas is fixed to exactly three optional values: lemmaCount, lemmaCountThreshold, isUnderexposed. Encoded in R013.
  why: aligns with the architectural patch that added those three nullable fields to SuggestedLemma; no extra derived field (e.g. lemmaCountDeficit, suggestionReason) is exposed.

2026-05-16 — analyst — "Señal no disponible" (auditoría sin lemma-count) y "señal no aplicable" (COMPLETELY_ABSENT) se representan ambas de la misma forma observable: los tres valores quedan no informados en bloque.
  why: una sola convención simplifica la traducción a aserciones de test y evita ramas observables divergentes.

2026-05-16 — analyst — DOUBT-ERROR-TEXT cerrada sin fijar literal exacto.
  why: la regla R011 ya define el comportamiento observable (el revise no se aplica); el wording del mensaje no es testeable como behavior.

2026-05-16 — architect — Escalation del developer (CannedLemmaAbsenceQuizCandidateGeneratorTest + LemmaAbsenceMvpStrategyTest rotos por 4-arg SuggestedLemma): NO requiere patch arquitectónico. Diagnóstico:
  - Ambos archivos son `@Generated` y declarados en sentinel.yaml como `handwrittenTests` (4 en CannedLemmaAbsenceQuizCandidateGenerator, 9 en LemmaAbsenceMvpStrategy).
  - El shape 7-arg de SuggestedLemma es correcto por REQUIREMENT (R013 exige los 3 campos).
  - Records Java tienen solo el constructor canónico N-arg = N campos; la DSL Sentinel no soporta constructores secundarios, así que un 4-arg "convenience" no es expresable. Opción A descartada.
  - `sentinel generate` preserva bodies de handwrittenTests existentes; no los re-genera para reflejar el nuevo shape. Opción B descartada.
  - Los bodies tienen lógica real (mocks, asserts) escrita en su día por @test-writer para FEAT-LAGEN/FEAT-LAPS. Re-homing de las llamadas obsoletas es lane del @test-writer (F4), no del @developer ni del @architect. Opción C es la correcta.
  - feedback_preserve_features aplica: no romper FEAT-LAGEN/FEAT-LAPS por side effect del patch F-SLEM; @test-writer debe re-home `new SuggestedLemma(lemma, pos, reason, cocaRank)` → `new SuggestedLemma(lemma, pos, reason, cocaRank, null, null, null)` en ambos archivos.
  why: el problema no es estructural en sentinel.yaml — es un side effect en test bodies que pertenecen a otra feature; el fix corresponde al rol que mantiene esos bodies.

2026-07-01 — analyst — Extensión ADITIVA: nuevo desempate por bandas de frecuencia COCA deficitarias del nivel DENTRO de cada grupo de prioridad, ANTES de COCA ascendente. Reglas nuevas F-SLEM-R014 (desempate binario pertenece/no-pertenece a banda deficitaria) y F-SLEM-R015 (degradación elegante sin análisis de bandas, espeja R012). Enmendada R005 (COCA ya no es el único desempate; desempata entre lemas con el mismo estado de banda). Los 5 grupos de R003 quedan INTACTOS. J001 ampliada: 3 ramas (bandas presentes → gate R014; lemma-count sin bandas → gate R015; sin lemma-count → R012). Validate: [OK] (solo WARN de glosario preexistentes).
  why: regla de convivencia elegida por el usuario — no se altera qué grupo va antes que cuál, solo la elección dentro del grupo empuja hacia arriba lemas que cubren bandas de frecuencia deficitarias del nivel.

2026-07-01 — analyst — Sin campo observable nuevo por lema (respeta contrato R013 "no se exponen otros valores derivados"). El orden se observa/testea con el cocaRank ya expuesto + la distribución de bandas del nivel (FEAT-COCA/FEAT-DCOCA). DOUBT-BAND-DEFICIT-MARKER dejada ABIERTA con recomendación Opción B (no agregar marcador); si al testear se ve inobservable, escalar a analyst antes de agregar cualquier campo. DOUBT-BAND-RANGE-OVERLAP cerrada (bandas contiguas/disjuntas → pertenencia binaria bien definida).
  why: el usuario pidió preferir SOLO orden como mecanismo observable y NO agregar campos por cuenta propia.

2026-07-01 — analyst — Reconciliada cláusula OUT: "no priorizar por mayor déficit" ahora distingue explícitamente déficit de EXPOSICIÓN de lemma-count (por-lema, SIGUE excluido del orden) vs déficit de BANDA de frecuencia COCA del nivel (por-nivel, SÍ usado como desempate binario). Agregada OUT: no se ordena por magnitud del déficit de banda.
  why: evitar contradicción entre la cláusula OUT vigente y la nueva señal permitida.

2026-07-01 — analyst — PIVOTE del ranking (SUPERSEDES la decisión "5 grupos de R003 intactos + banda solo intra-grupo"). Nuevo esquema autoritativo de SEIS TIERS (first-match-wins), banda deficitaria elevada de desempate a CLAVE SECUNDARIA FUERTE dentro de la macro-escasez:
  T1 sub-expuesto(1..N-1)+banda | T2 ausente+banda | T3 sub-expuesto | T4 ausente | T5 expuesto-escaso(N..2N-1)+banda | T6 expuesto-escaso.
  Invariantes: (a) sub-expuesto>ausente dentro del mismo estado de banda (1>2,3>4); (b) ausente-deficitario T2 > sub-expuesto-no-deficitario T3 (DELIBERADO); (c) expuestos-escasos T5-6 SIEMPRE al final. COCA asc = desempate final DENTRO del tier.
  Reescritas: R003 (tabla de 6 tiers, subsume subtipo de ausencia en count), R014 (def formal: estado de exposición cortado por N + banda binaria + COCA; una sola función de prioridad SIN branch de banda). Enmendadas: R005 (COCA = desempate final dentro del tier), R007 (COMPLETELY_ABSENT detrás del sub-expuesto SOLO dentro del mismo estado de banda; relajación deliberada; lemma-count sigue no informado para ausente, R013 intacta), R012 (sin lemma-count no hay estado de exposición → tiers no aplican → COCA), R015 (degradación = misma función con tiers de banda 1/2/5 vacíos → 3→4→6, sin branch). Cortes con N (sub=1..N-1, ausente=sin exposición, ampliado=N..2N-1; borde count==N inicia ampliado). R004/R013 INTACTAS. Journey J001: assert nodes reescritos (tiers en vez de grupos); estructura de 3 ramas y gates sin cambio. Validate: [OK] (solo WARN de glosario preexistentes).
  why: el usuario decidió que tapar una banda de frecuencia deficitaria del nivel pesa más que la distinción sub-expuesto/ausente dentro de la macro-escasez; el subtipo de ausencia pierde peso frente a count+banda.

2026-07-01 — analyst — includeExposed (FEAT-INCEXP) NO se toca: sigue gobernando QUÉ lemas expuestos (N..2N-1) entran al pool como fallback opcional (no default). El tier scheme solo define el ORDEN (van a T5-6). Explicitado en Context, Scope OUT y References para no revertir Fase 1.
  why: separar "qué entra al pool" (política includeExposed) de "cómo se ordena" (tiers) — el pivote solo cambia lo segundo.

2026-07-01 — analyst — Dos DOUBTS nuevas ABIERTAS (pendientes de confirmación del usuario):
  - DOUBT-ABSENCE-SUBTYPE-TIEBREAK: ¿colapsar orden fino por subtipo (APPEARS_TOO_LATE/EARLY) o conservarlo como desempate más profundo (después de banda, antes de COCA)? Encodeado provisional Opción A (colapsado). Recomendación A.
  - DOUBT-EXPANDED-BAND-EDGE: ¿banda ampliada arranca en N o N+1? Encodeado Opción A (arranca en count==N; sub-expuesto=1..N-1 estricto, coherente con isUnderexposed = count<threshold). Recomendación A.
  why: el usuario pidió explícitamente dejar ambos puntos como Doubt y no inventar la resolución.

2026-05-16 — architect — FLAG: el developer modificó SentenceLengthContextResolver y SentenceLengthContextStructuralValidator (ambos `@Generated`) manualmente para pasarlos al shape 7-arg (memory/fix-log.md entry 1). Eso viola F1/F2. Sin embargo, el contenido del cambio (agregar null,null,null al 4-arg literal) es EXACTAMENTE lo que `sentinel generate` debería emitir si re-generara la producción tras el cambio de SuggestedLemma. Necesita verificación: correr `sentinel generate` tras todo lo demás debería re-escribir esos dos archivos y la edición manual del developer desaparece sin daño. Si tras regenerar quedan rotos, hay bug en el generador (reportar). Si el generate los regenera con shape correcto, no se requiere acción extra.
  why: F1/F2 son estrictas, pero el daño práctico es nulo siempre que regenerar produzca la misma forma; lo que se preserva es la auditoría de Sentinel sobre quién posee el archivo.
