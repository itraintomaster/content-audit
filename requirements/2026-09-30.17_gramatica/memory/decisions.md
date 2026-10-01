# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer (paquete B) — Revisión a mano de las 23 marcas fuera del oro de escribir/ (filtro-99bd09ee): 11
  de 20 reales en la muestra (semilla 87), 13 de 23 con las tres que quedaron afuera. Las falsas eran patrones y salieron
  (71a54d6f): AT_TIME, AGREEMENT_SENT_START_2, SOME_FACULTY, SINGED_CONTRACT y THESE_ONES fuera del filtro, y el signo
  final v2 (FINAL_SIGN_VERSION en la huella): sólo la solución, «?.» tras la pista ya está cerrado, «Have …» imperativo
  no abre pregunta y el signo que falta es «.» aunque un diálogo traiga un «?» antes.
  why: cada regla excluida marcaba en el curso sólo inglés correcto; las respuestas aceptadas que siguen a la primera no
  las lee el alumno, y su signo no es un error del curso.

2026-10-01 — analista y arquitecto (qa-tester B, por el coordinador) — R001 inv. 5: la lista «a / b» dentro de una
  oración ya cerrada, con el hueco después, se lee con la respuesta en el lugar de la lista y su mayúscula: «Something
  was wrong with the telephone.». Son 40 de A2 y en los 40 es el texto con que se grabó el audio. El arreglo vive sólo en
  DefaultFinishedSentenceReader (Gramática y Naturalidad): el ejercicio terminado del renderizador compartido (`get
  student-view`) sigue «Something / anything was wrong with the telephone. Something», congelado para los jueces (C-07).
  why: leída sin la respuesta, daba el signo final falso de …396f y 15 marcas de A2 de Naturalidad que no eran del curso.
2026-10-01 — analista (qa-tester B) — R003 y el Contexto pasan a contar con la lectura de R001: 8 signos finales, todos
  reales (el prototipo daba 10: la etiqueta tras «Link type:» y …396f), y 47 ejercicios con hallazgo, 36 reales. En las
  40 oraciones de la inv. 5, LanguageTool 6.5 (el del prototipo) no da marcas que pasen el filtro (medido el 1/10).

2026-10-01 — analista (qa-tester B, por el coordinador) — Los casos de R004 dicen lo que dice la regla: de los 13
  «videogames», los 5 que están dentro de la respuesta de una reescritura (…294b, …404c, …41e0, …4552, …47f8) son alta,
  y los 8 del texto fijo, media. La regla no cambia; el TECH_SPEC, igual.

2026-10-01 — qa-tester — F-GRAMA-J001 en audit-domain, `com.learney.contentaudit.auditdomain.grammar`: el
  analizador y sus piezas son package-private ahí y los puertos se simulan, como los J001 de los analizadores de la ronda 1.
2026-10-01 — qa-tester — Dos pruebas con LanguageTool de verdad van sobre LanguageToolGrammarChecker (de FEAT-MLOCAL),
  trazadas a F-GRAMA-R002: lo apagado por omisión no marca («You prefer Italian food.») y «explained us» en las dos
  variantes. Llevan `herramienta-real`.
2026-10-01 — qa-tester — Para el analista: (1) R001 no dice cómo se lee un ejercicio con la lista «a / b» dentro de la
  oración ya cerrada y el hueco después: «He did something / anything to help.» + «something» (40 en A2, Indefinidos con
  some/any y every/no). El prototipo saca la lista sin poner la respuesta y lee «He did to help.»: de ahí sale la marca
  falsa de signo final «was wrong with the telephone.» (…396f) y 15 de las 120 marcas de A2 de Naturalidad.
  (2) La lista de casos de R004 pone los 13 «videogames» como media; en 5 (…294b, …404c, …41e0, …4552, …47f8) la palabra
  está dentro de la respuesta de una reescritura y R004 inv. 2 los hace alta. Las pruebas siguen a la regla.

2026-10-01 — integrador — Rebasado sobre ronda1/integracion y FEAT-RONDA2 (propose --replace, tech-spec write y validate en exit 0): sin cambios de estructura. GrammarCorrection y GrammarRuleFix, en grammar, los suman TAREA y TANDA (F-RONDA2-R009); GRAMMAR es tipo de tarea (F-RONDA2-R008, DOUBT-RONDA2-TAREAS-B con A).

2026-09-30 — analyst — Verificados los dos números de B: 3 bloqueantes (…320a, …4009, …408e) con `arq-B2/bloqueantes.py`; el
  cuarto que da ese chequeo, …3529, es la lista «is / are» de la pista y SINGULAR_AGREEMENT_SENT_START, excluida. Y 17 «gone
  to» en `escribir/gramatica.json` (todos A2). El REQUIREMENT ya los tenía bien desde 884355c2; sin cambios.

2026-09-30 — architect — La oración terminada es un paquete propio (`finishedsentence`: `FinishedSentenceReader`, una por
  respuesta aceptada, con las posiciones de cada respuesta) que también lee naturalness; no reemplaza la oración plana ni el
  ejercicio terminado de A-01 (gancho B-20). R002, R003 y R004/R005 son tres piezas internas del paquete `grammar`.
  why: la plana deja «They 're» y «- water» (920 marcas contra 693) y cambiarla movería el largo (F-HALL-R013).

2026-09-30 — architect — El puerto `GrammarChecker`, su adaptador y la dependencia viven en FEAT-MLOCAL; este parche trae sólo
  el analizador. La versión que rinde examen es «LanguageTool 6.8 · methodId del filtro»: cambiar una lista es versión nueva.
  why: `patch propose` rechaza un `implements` hacia una interfaz de otro parche; y la herramienta es de MLOCAL.

2026-09-30 — architect — Si LanguageTool no carga, el analizador deja todo sin evaluar con el motivo en vez de cortar la corrida;
  sin ligadura al plan (E decide la tarea, B-18). Ficha: reglas `english-error` y `final-punctuation`, instantáneas.

2026-09-30 — analyst — Corrección, notas del orquestador: los bloqueantes son 3 (las repeticiones pegadas al hueco), no 4:
  con R004 «He've had» es alta. Los «gone to» de `escribir/gramatica.json` son 17, no 18 (47 casos: A1 7, A2 40).

2026-09-30 — analyst — José, 30/9 («Dale con LanguageTool»): DOUBT-GRAMA-REGLAS → A (afuera estilo, mayúsculas, compuestos,
  variantes, lo que no marcan las dos variantes, las apagadas por omisión y SINGULAR_AGREEMENT_SENT_START) y
  DOUBT-GRAMA-ALCANCE → A (control gratis de los arreglos de la mesa). R002 pasa a VALIDATED. La dependencia es LanguageTool
  6.8 (`org.languagetool:language-en:6.8`), la 6.x vigente; lo medido con 6.5 lo vuelve a medir su examen.

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
