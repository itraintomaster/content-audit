# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-10-01 — qa-tester — F-METAS-J001 en audit-cli, `com.learney.contentaudit.auditcli.commands`.
  why: el camino es set-goals y compare, package-private ahí; el comparador y el libro de metas están en comparisonengine, público para audit-cli.
2026-10-01 — qa-tester — Para el analista (1): «abrir una tanda también las fija» (R006) no tiene prueba posible: DefaultBatchService (FEAT-TANDA) no existe cuando se aplica METAS y ninguna regla de TANDA lo dice. Sumarlo a F-TANDA-R001 o probarlo en un parche de METAS posterior a TANDA.
2026-10-01 — qa-tester — Para el analista (2): R001 pide filas «para cada analizador que corrió en la línea base», pero su detalle deja vacías las de errores de la base del 29/9 y R007 deja indecidibles las de una medida nueva. La prueba sigue el detalle: vacías, no en cero.

2026-10-01 — integrador — Rebasado sobre ronda1/integracion y FEAT-RONDA2 (propose --replace, tech-spec write y validate en exit 0): DefaultBatchComparator inyecta TopicTeachingResolver de D (F-14); offlevelwords suma revision-domain a sus clientes en el paso de los allowedClients de FEAT-RONDA2.

2026-09-30 — analyst — R003: «salvo la que el tema enseña» (F-SEMAF-R004, ya aceptada por José), para que la mesa de E, su compuerta y la comparación no difieran; lo resuelve el `TopicTeachingResolver` de D (gancho F-14). Duda nueva DOUBT-ERRORES-CONTRA-QUE (OPEN, recomendada A: errores contra el inicio de la tanda; vocabulario contra la base).

2026-09-30 — architect — Comparación en revision-domain: contrato en `comparison` (público) y motor en `comparisonengine` (público sólo para audit-cli, como findingengine de HALL): `DefaultBatchComparator`, `DefaultGoalBook` y `LeaveOneOutChangeAttributor`.
  why: la comparación lee la proyección, que vive ahí; un módulo nuevo no agrega nada que un paquete no dé.

2026-09-30 — architect — Metas: declaración (`GoalDeclarationSource`, archivo versionado propuesto `analysis/metas.json`) con pisos por familia, metas nombradas y la meta de la muestra ciega; `GoalBook` expande el piso de familia a todo analizador del catálogo y fija (`seal`) las metas efectivas enteras, no sólo la huella, para mostrar antes y ahora (R006). `set-goals`/`get-goals`; E llama `seal()` al abrir una tanda.

2026-09-30 — architect — R004 sin tokenizar: cada variante «sin el cambio k» se arma con los ejercicios ya mapeados de las dos fotos (entidades del informe) y cuesta una corrida del motor; una corrida por cambio para todos los pisos rotos. R007: la base se re-mide corriendo los analizadores de hoy sobre las entidades de su propio informe.
  why: 38 s por variante con el caché de hoy; 0,05–0,1 s así.

2026-09-30 — architect — `SampleOutcome` y `SampleOutcomeSource` los declara FEAT-CIEGA; METAS sólo los inyecta/lleva.
  why: `patch propose` rechaza que un parche implemente una interfaz de otro parche todavía no aplicado; así no hay implements cruzado.

2026-09-30 — analyst — José, 30/9: DOUBT-METAS → A y DOUBT-VUELVE-VOCABULARIO → A, las dos RESOLVED. Pisos que deciden: ningún ejercicio tocado gana un hallazgo, los errores no suben, el vocabulario baja 0,5 como mucho, y la muestra ciega da 90 de 100 mejor o igual con 1 peor como mucho; metas de avance (0 imposibles y 0 errores por nivel, menos de 5 % al terminar) sólo se muestran. Las fija José antes de cada tanda.

2026-09-30 — analyst — R003 reconciliada con E (José 11/8, F-NOEMP-R003): vuelve por errores y por palabras de otro nivel o fuera del catálogo (vocabulario de gravedad media o más); el largo nunca devuelve un cambio. Se compara contra el inicio de la tanda.
  why: pedido del orquestador; E mantiene que el largo no rechaza. Con la gravedad de HALL (otro nivel/fuera de catálogo = media, resto del vocabulario = baja) la regla queda genérica.

2026-09-30 — analyst — R005: el piso de errores se decide contra el inicio de la tanda y el de vocabulario contra la línea base (acumulado). R006: las metas se fijan con `set-goals` y al abrir una tanda, no al cambiar el par activo. R007: criterio con los números de D (73,92 → 73,86; A1 96,54 → 96,02; lemma-count 67,0 → 66,5; sin R007 la primera tanda rompería A1 −0,52, A2 −0,56, B1 −0,58).
  why: la base queda fija mientras el activo avanza; los errores contra el inicio son la cuenta de la compuerta de E (decisions de NOEMP) y, encadenados, tampoco suben contra la base.

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
