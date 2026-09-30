# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — analyst — Código FEAT-METAS / F-METAS: nuevo.

2026-09-30 — analyst — La comparación es una salida nueva; el contrato de la vista consolidada no cambia (F-CDIFF-R010 sigue sin deltas).
  why: G necesita que content-audit publique el cambio y el estado; cambiar CDIFF-R010 sería mover una decisión existente.

2026-09-30 — analyst — Piso por nodo cambiado, por identidad de hallazgo (R003), además de los pisos de nivel.
  why: medido: las 4 propuestas no mueven ningún número de nivel ni del curso en dos decimales; «grandmother» sólo baja su ejercicio (lemma-absence 1,0 → 0,9), su tema (1,0 → 0,995) y su topic (0,99552 → 0,99519).

2026-09-30 — analyst — Los pisos se miden contra la línea base, no contra la tanda anterior (R005, DOUBT-METAS C descartada en la recomendación).
  why: cuatro tandas de −0,4 no rompen un piso por tanda y suman −1,6.

2026-09-30 — analyst — R004 (atribución quitando de a uno) queda como ASSUMPTION.
  why: el motor tarda 0,05–0,1 s, pero tokenizar el curso entero son 38 s por variante; sólo es viable tokenizando lo que cambió.

2026-09-30 — analyst — Un cambio de medida se muestra aparte y la tanda se mide contra la línea base re-medida (R007).
  why: pedido del orquestador; con las bandas 8·6·4·3 de lemma-count (paquete D) el mismo curso pasa de 73,9 a 73,5.

2026-09-30 — analyst — Metas por defecto por familia para que un analizador nuevo quede cubierto sin tocar la declaración (R005); valores y quién las fija, DOUBT-METAS (recomendado A: los fija José antes de abrir cada tanda).
