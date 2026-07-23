# Decisions

2026-07-22 — analyst — La lista solo contiene palabras FLAGGEADAS (mal ubicadas + fuera de catalogo penalizantes).
  why: es lo que hace cierto "lista vacia = oracion limpia" y da directamente el conjunto flags() que compara el eval #7 (F-LASAG-R007). Alternativa verbosa diferida en DOUBT-VERBOSE-MODE.

2026-07-22 — analyst — Paridad EXACTA con el audit (F-CLEX-R006): mismo tokenizador y catalogo, todo el pipeline (R005 frases, R033 techos, R034 bandas, R035 exclusiones, R040-R046). No es replica aproximada.
  why: el criterio #7 exige el MISMO analisis del audit, no una copia; el precedente es el eval #3 de longitud reusando `content-audit tokens`.

2026-07-22 — analyst — En REWRITE se analiza solo la oracion respuesta (frase canonica por modo, F-SMODE-R003/R004).
  why: si se analizara la oracion fuente, se reportarian palabras que el audit nunca flaggea, rompiendo la paridad.

2026-07-22 — analyst — La sintaxis concreta del comando (verbo, flags, JSON) se deja a arquitectura; la feature fija solo el contrato observable.
  why: coherente con la resolucion Opcion A de DOUBT-EVAL-LEXICO-CLI en FEAT-LASAG.

2026-07-22 — architect — Diseño F-CLEX: extraer scoreQuiz a motor compartido SentenceLexicalScorer (labs, interno) + proyeccion publica SentenceLexicalEvaluator/SentenceLexicalFlags (nuevo pkg lexicalflags publico) + verbo CLI LexisCommand/LexisCmd. Analyzer delega (inyecta el scorer) => una sola fuente de verdad del emparejamiento (paridad F-CLEX-R006 sin duplicacion).
  why: la alternativa (comando corre el analyzer entero con curso sintetico de 1 quiz) acopla al arbol AuditNode, corre Grupos A-F inutiles y IGUAL exige exponer tipos internos; la extraccion rompe menos contratos (solo +1 requiresInject al analyzer) y es durable.
2026-07-22 — architect — Carrier publico minimo (solo lema/POS/nivel esperado/nivel oracion; lema/POS/rank) — NO expone score/discount/cocaRank; mantiene "listas vacias = limpia" (F-CLEX-R005) y P1 superficie minima. Patch validado (6 add, 3 mod). Handoff: @analyst debe `feature sync` F-CLEX-J001 tras apply; luego @qa-tester agrega placement + handwrittenTests.
