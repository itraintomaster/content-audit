# Decisions

Decisions future sessions should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — developer (C) — La gravedad del hallazgo sale del problema (falsa y contradicha HIGH, exagera MEDIUM, no cubre LOW; F-MINIT-R004), nunca de la que escriba el modelo: así nunca es BLOCKING. Un veredicto que reprocha una miniteoría o un ejercicio que el tema no tiene, o una contradicción sin su ejercicio, no es de ese contenido: el tema queda sin evaluar en vez de adivinar dónde contarlo.
2026-09-30 — developer (C) — Un tema sin ninguna miniteoría no es unidad del juez (no se consulta ni se cuenta). La evidencia muestra cada ejercicio como su oración terminada (y sus opciones en la opción múltiple): el repartidor sólo recibe la configuración, no el render (escalado).

2026-09-30 — integrador — Rebasado sobre el sentinel.yaml real y FEAT-RONDA1 (propose --replace, exit 0): MiniTheoryAnalyzerProvider implements AnalyzerProvider (C-03).

2026-09-30 — qa-tester — F-MINIT-J001 en audit-domain, `com.learney.contentaudit.auditdomain.minitheoryengine`.
2026-09-30 — qa-tester — «Sólo donde se muestra» y «en todo el tema» se prueban con alcance explícito en el repartidor («enjoy», 9 de 80 en «Contraste inicial»); el valor por omisión, en la configuración.
  why: si José vuelve a «en todo el tema» (DOUBT-MINIT-VARIAS, a confirmar) cambia una sola prueba.

2026-09-30 — architect — Consulta por tema (onKnowledge arma el sujeto; el tope cuenta temas), pero puntaje, hallazgo y
  diagnóstico en cada ejercicio: así «sólo donde se muestra» sale sin tocar F-HALL-R008 (el hallazgo de tema contaría en
  todo el tema). `MiniTheoryErrorScope` (SHOWN_ONLY por omisión, WHOLE_TOPIC) en la config es el parámetro de José.
2026-09-30 — architect — Mapa de contenido numerado (M1…, E1…) en el orden del tema; el repartidor numera igual. La
  marca del hallazgo es el texto de la miniteoría, para que la mesa agrupe una reescritura por miniteoría.

2026-09-30 — analyst — DOUBT-MINIT-VARIAS → B (José, 30/9 a la tarde: «cuenta sólo donde se muestra»; ⚠ se confirma
  con José al volver). R003 reescrita (VALIDATED): el hallazgo va en cada ejercicio que muestra la miniteoría con problema
  y cuenta ahí; «en todo el tema» queda como parámetro del analizador. R004 (puntaje por ejercicio), R005.4, R007 y J001
  ajustados.
  why: corrige la decisión de la mañana, tomada con la premisa equivocada de una miniteoría por tema. Poner el hallazgo en
  el ejercicio respeta F-HALL-R008 tal como José la aprobó (el de tema cuenta en todo el tema) sin tocar el contrato 0.3.
2026-09-30 — analyst — DOUBT-MINIT-TEMA-GRANDE → A (José, 30/9, «por lo demás, continúa»): una consulta por tema.
2026-09-30 — analyst — R002: que la miniteoría pida una forma no es problema de este juez; lo marca A (F-JEXAM-R012).

2026-09-30 — analyst — Código FEAT-MINIT / F-MINIT; nombre del analizador `mini-theory`, familia de errores, evalúa el tema
  (KNOWLEDGE), una consulta por tema (413).

2026-09-30 — analyst — La miniteoría NO es una por tema: es un campo de cada ejercicio (`miniTheory`). En la base del 29/9,
  154 temas tienen una sola y 259 tienen dos o más (hasta 50); 1.488 distintas (A1 266, A2 455, B1 473, B2 294).
  why: contradice la premisa de la propuesta («la miniteoría es una sola para el tema») y la razón de la decisión del 30/9
  (DOUBT-HALLAZGO-DE-TEMA de FEAT-HALL). Se respeta la decisión en R003 (VALIDATED) y el cambio va como DOUBT-MINIT-VARIAS.

2026-09-30 — analyst — El juez lee cada miniteoría con los ejercicios que la muestran y todo el tema; no lee traducciones,
  audios ni la teoría larga (theoryId). Huella = todo el tema: cualquier cambio en un ejercicio vuelve a juzgar el tema.
  why: la regla se compara con el tema entero; 413 consultas es el costo más bajo del paquete, así que la cascada no duele.

2026-09-30 — analyst — Hallazgo por miniteoría con problema, identificado por su texto (F-HALL-R014, «qué marca»); nunca
  bloqueante (la app la muestra en la corrección). Gravedad: falsa o contradicha alta; exagera media; no cubre baja.
  Escala 1 / 0,8 / 0,6 / 0,3 [ASSUMPTION].

2026-09-30 — analyst — En el examen fijo las verduras APRUEBAN para este juez: su miniteoría («Con he, she, it el verbo
  lleva -s…») es cierta; el problema de las verduras es la pista y la traducción. Se aparta del «juez: todos» del grupo a.
