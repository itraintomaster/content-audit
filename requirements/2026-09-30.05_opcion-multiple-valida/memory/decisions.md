# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — developer (impl A) — Con un guion al abrir la oración, R003 marca sólo el guion (mesa) y no la minúscula de las
  otras opciones: la mesa rearma el ejercicio entero, mayúscula incluida, y un arreglo por regla en paralelo chocaría con él.
  La mayúscula al abrir y la «i» son un hallazgo por ejercicio cada uno (marcas «mayuscula» e «i», por regla sobre todas las
  opciones a la vez); el guion, «guion»; R001 marca «ninguna» o la segunda opción que vale; R002 la opción que sobra (la gemela
  de la correcta, o la segunda de dos incorrectas); R004 la opción que no encaja con el artículo.
  why: la identidad es regla + ejercicio + opción marcada (F-HALL-R014) y la mayúscula se arregla en todas a la vez.

2026-09-30 — developer (impl A) — El puntaje es 0 con un hallazgo de regla y 1 sin ninguno; si el juez juzgó, gana el menor entre
  ése y el suyo (1, 0,6, 0,3 o 0). Lo que el juez no juzgó (pendiente, fallido o sin juez) queda «a medias» con su puntaje de
  reglas. La fábrica «no disponible» declara la regla del juez con el mismo id, texto y costo que la de C.

2026-09-30 — integrador — Rebasado sobre el sentinel.yaml real y FEAT-RONDA1 (propose --replace, exit 0): el juez entra por el contrato de C que vive en la integración (choicealternative): salen MultipleChoiceJudgeFactory/Session, MultipleChoiceJudgement(+Status), AlsoValidOption y MultipleChoiceJudgeCoverageDiagnosis; judgeRun, judgeRunFactory, diagnosis.judgment y UnavailableChoiceAlternativeJudgeRunFactory. DOUBT-OMVAL-JUEZ-A-MEDIAS y CORRIDA-BASE: A (F-RONDA1-R006, R007).

2026-09-30 — qa-tester — F-OMVAL-J001 en audit-domain, paquete multiplechoicevalidity: el analizador y sus colaboradores son package-private y los puertos se simulan; mismo criterio que F-QINST-J001/J002 y F-LREC.
2026-09-30 — qa-tester — Las gemelas por contracción («'re going to»/«are going to», 26ef; «'ve got to»/«have got to», 2e0c) usan la tabla del corrector nuevo: con la de hoy sólo cuentan mayúscula y signos.
2026-09-30 — qa-tester — La prueba del ejercicio que el juez no vio sigue la opción A de DOUBT-OMVAL-JUEZ-A-MEDIAS (abierta) y el gancho A-08.
2026-09-30 — qa-tester — No se declara que la fábrica «sin juez» conteste NOT_AVAILABLE (ninguna regla lo pide); su prueba es la de la ficha: la regla del juez aunque no haya juez.

2026-09-30 — architect — El juez de C entra por MultipleChoiceJudgeFactory/Session (raíz de audit-domain, para que C implemente desde
  cualquier módulo): sesión por corrida con la política del analizador; judgeRule() da a la ficha la regla del juez con su id y costo.
  UnavailableMultipleChoiceJudgeFactory hasta que C conecte el suyo. La interfaz va a los ganchos (A-07) para reconciliar con C.
2026-09-30 — architect — Choque con 0.3: un ejercicio con reglas corridas y juez pendiente → «a medias» (gancho A-08, DOUBT-OMVAL-JUEZ-A-MEDIAS);
  la ficha es de modelo pago y la corrida base la dejaría afuera → tope 0 (gancho A-09, DOUBT-OMVAL-CORRIDA-BASE).
2026-09-30 — architect — R002 compara con CorrectorMatcher.sameAnswer (contraída/larga y grafías de la lista = misma respuesta).

2026-09-30 — analyst — R002 (José, 30/9): dos opciones que sólo difieren en contraída y larga, o en una grafía de la lista, son la
  misma respuesta; si una es la correcta hay dos correctas (alta, mesa); si las dos son incorrectas, sobra una (baja, mesa).
  Medido 29/9: 0 con la correcta; 28 con dos incorrectas (A1 20 «Elegir la forma natural», A2 8 «Obligación»).
  why: José lo cambió; se compara con la tabla de F-RESPC-R001, así que hasta que se actualice la copia sólo cuentan mayúsculas y signos.

2026-09-30 — analyst — DOUBT-OMVAL-DOS-CORRECTAS → A (José, 30/9): siempre una sola correcta, y la mesa rearma.

2026-09-30 — analyst — DOUBT-OMVAL-JUEZ → A (José, 30/9): el juez de «otra opción también vale» (FEAT-MCVALE, paquete C) es parte de
  multiple-choice-validity: una sola fila y una sola ficha, con costo modelo pago.

2026-09-30 — analyst — DOUBT-OMVAL-FORMA → A y DOUBT-OMVAL-CORREGIR → A (José, 30/9, con la recomendación).

2026-09-30 — analyst — Código FEAT-OMVAL / F-OMVAL: nuevo.

2026-09-30 — analyst — «Abre la oración» exige que lo que sigue al hueco termine en punto, pregunta o exclamación.
  why: sin eso se marcaban los 20 fragmentos de «A o an» («___ excited man»), que no son oraciones.

2026-09-30 — analyst — «Sólo en la forma» se lee como mayúscula/signos (R002) y artículo a/an de antes del hueco (R004).
  why: la lectura literal (artículo o número de la opción) marca 73 ejercicios de 4 temas donde eso es la lección; queda como duda.

2026-09-30 — analyst — Una sola correcta se verifica contra el backend: la respuesta vale sólo si lo elegido suma el puntaje entero
  (be/domain/quiz/pkg/evaluation/evaluation_multiple_choice.go), y todas las opciones múltiples son de selección SINGLE.

2026-09-30 — analyst — Choque con F-OPMUL-R006 (no se corrige ninguna opción múltiple): detectar no choca, aplicar sí → DOUBT-OMVAL-CORREGIR.
