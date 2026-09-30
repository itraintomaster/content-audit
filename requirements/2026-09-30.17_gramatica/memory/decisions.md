# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — analyst — Código FEAT-GRAMA / F-GRAMA y nombre `grammar`: nuevos; no chocan con ningún código de las ramas de ronda 1 ni 2.

2026-09-30 — analyst — Gramática lee su propia «oración terminada» (R001), no la oración plana de content-audit.
  why: la plana deja «They 're» (123), «- water» (331 MC), «was / were was» (110 en 9 temas) y «..» tras una nota; con ella
  LanguageTool dio 920 marcas, con esta lectura 693. Arreglar la plana movería el largo de oración (F-HALL-R013).

2026-09-30 — analyst — Una marca cuenta sólo si la dan en-US y en-GB en el mismo lugar (R002).
  why: el curso mezcla grafías (theatre / traveled, 83 y 102 marcas de una sola variante) y José acepta las dos; también
  saca los colectivos en plural del británico. Ortografía que queda: «videogames» ×13, «teached», «clothers».

2026-09-30 — analyst — Afuera: CASING (401, 359 son opciones de MC → F-OMVAL-R003), I_LOWERCASE (9, todas en opciones MC),
  estilo, redundancia, tipografía salvo el espacio tras el punto, compuestos, variantes y las reglas apagadas por omisión
  (+48 marcas, 42 ARTICLE_MISSING, todas falsas). SINGULAR_AGREEMENT_SENT_START, como contradice al curso.
  why: medido regla por regla sobre el curso entero; la lista final es DOUBT-GRAMA-REGLAS.

2026-09-30 — analyst — Regla propia de signo final (R003): 10 marcas, 8 reales, 3 del examen. El 077 los puso en gramática, baja.

2026-09-30 — analyst — Gravedad por lugar (R004): repetición pegada al hueco = bloqueante (la respuesta incluye palabras
  que ya siguen al hueco: «noise noise», «in August in August», «the apartment the apartment»); toca una respuesta = alta;
  sólo texto fijo = media; signo o espacio = baja.
  why: F-HALL-R002 define bloqueante como «no se puede acertar»: quien sigue la pista escribe sin la palabra repetida.

2026-09-30 — analyst — Costo instantáneo: LanguageTool son reglas; el curso entero en ~10 s + ~1 s de arranque.
  why: F-HALL-R002 va por regla; por eso `grammar` entra en la corrida base (FEAT-MLOCAL R004).

2026-09-30 — analyst — Examen: 4 de 47 en escribir (8,5 %). No es una falla del diseño: 18 de los 47 son «gone to» por
  «been to» y el resto calcos y colocaciones; lo busca FEAT-TRSEN. Se deja como DOUBT-GRAMA-ALCANCE, recomendado «sí,
  como control barato de los candidatos de la mesa».
