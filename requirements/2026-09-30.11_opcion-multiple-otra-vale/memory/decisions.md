# Decisions

Decisions future sessions should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — qa-tester — F-MCVALE-J001 en audit-domain, `com.learney.contentaudit.auditdomain.multiplechoicevalidity`, donde A ubica F-OMVAL-J001.
  why: corre analyze con el analizador de A, que aloja al juez; implementarlo necesita el parche de A y el gancho C-09.
2026-09-30 — qa-tester — Las pruebas del run y de la factory siguen la interfaz de C (request, resolveAll, judgmentOf, coverage), sin reconciliar con A-07; el puntaje del juez se fija como dato (C-10).

2026-09-30 — architect — Sin analizador ni proveedor: el juez se ofrece a A por `ChoiceAlternativeJudgeRunFactory`
  (judgeRule, open(policy)) y `ChoiceAlternativeJudgeRun` (request, resolveAll, judgmentOf, coverage). Alineado con A-07
  en estados (JUDGED/PENDING/FAILED/NOT_AVAILABLE), una sola regla y open(policy); distinto en los dos tiempos (sin ellos
  no hay 4 a la vez) y en que el juicio trae gravedad, clave invertida y hallazgos listos. Reconciliación: ganchos C-09/C-10.
2026-09-30 — architect — La factory recibe el nombre del analizador que la aloja (multiple-choice-validity): tope,
  freno del examen y examen son los de ese analizador (F-HALL-R005).

2026-09-30 — analyst — DOUBT-MCVALE-ANALIZADOR → A (José, 30/9): el juez va dentro del analizador de Opción múltiple del
  paquete A, una fila y una ficha. R004 pasa a VALIDATED. C diseña el juez; A, el analizador que recibe sus veredictos.
2026-09-30 — analyst — R002 suma «contraída o larga» a lo que no cuenta acá (José, 30/9: dos opciones que sólo difieren
  así son la misma respuesta y lo marca A); lo que el juez liste igual se descarta con la tabla del corrector.
2026-09-30 — analyst — R005: tope por omisión 0 (F-JEXAM-R010) y [ASSUMPTION] nuevo: un ejercicio que las reglas de A
  evaluaron y el juez no, cuenta como no evaluado del todo por el analizador (piso de «algún error», F-HALL-R008).
  why: si no, un «algún error» sin juez se leería como completo. Lo acuerda el integrador con el arquitecto de A.

2026-09-30 — analyst — Código FEAT-MCVALE / F-MCVALE. El juez es una REGLA del analizador de opción múltiple válida del
  paquete A, no un analizador propio [ASSUMPTION, DOUBT-MCVALE-ANALIZADOR]; nombre de respaldo `choice-alternative`.
  why: la propuesta lo dibuja como «Analizador 3 · regla y juez» y F-HALL-R002 pone la opción múltiple como ejemplo de
  costo por hallazgo (reglas instantáneas, juez modelo pago) dentro de un analizador.

2026-09-30 — analyst — El 077 no tiene categoría propia para esta pregunta: sus casos están en «una sola respuesta»
  (AMBIGUO) sobre opción múltiple: 93 en escribir (A1 48, A2 45). `escribir/opcion.json` (186) es de forma (MC_FORMATO,
  170 de mayúsculas): examen de las reglas del paquete A; para este juez sólo se informa.

2026-09-30 — analyst — Decide dos cosas: qué incorrectas también valen, y si la correcta vale (clave invertida, el
  imposible `…47cc` del examen fijo: «if not» marcada, vale «if so»). Lo que difiere sólo en forma es del paquete A.
  Gravedad: correcta que no vale = bloqueante; otra igual de buena = alta; defendible = media; escala 1 / 0,6 / 0,3 / 0.

2026-09-30 — analyst — Lee nombre del tema, consigna (copia del ejercicio), enunciado con pista y opciones en el orden
  en que las ve el alumno con la correcta marcada; no lee traducción ni miniteoría (se ven al corregir).
