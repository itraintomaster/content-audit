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

2026-05-16 — architect — FLAG: el developer modificó SentenceLengthContextResolver y SentenceLengthContextStructuralValidator (ambos `@Generated`) manualmente para pasarlos al shape 7-arg (memory/fix-log.md entry 1). Eso viola F1/F2. Sin embargo, el contenido del cambio (agregar null,null,null al 4-arg literal) es EXACTAMENTE lo que `sentinel generate` debería emitir si re-generara la producción tras el cambio de SuggestedLemma. Necesita verificación: correr `sentinel generate` tras todo lo demás debería re-escribir esos dos archivos y la edición manual del developer desaparece sin daño. Si tras regenerar quedan rotos, hay bug en el generador (reportar). Si el generate los regenera con shape correcto, no se requiere acción extra.
  why: F1/F2 son estrictas, pero el daño práctico es nulo siempre que regenerar produzca la misma forma; lo que se preserva es la auditoría de Sentinel sobre quién posee el archivo.
