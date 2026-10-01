# Decisions

Decisions future sessions should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — developer (C, líder) — El juez de consigna resuelve siempre en lote (onQuiz junta, onCourseComplete resuelve), sin
  camino de a uno aparte: sus 27 pruebas de FEAT-QINST resuelven cada ejercicio con un onCourseComplete después de pedirlo, con
  una sesión falsa que contesta resolveAll con sus respuestas de a uno. Lo que leen y su huella no cambian (R007).
2026-09-30 — developer (C, líder) — En la sesión, un pedido posterior por el mismo contenido que otro programado en el mismo lote
  se resuelve después, de a uno: dos ejercicios iguales nunca pagan dos veces y el tope rinde lo mismo con cualquier N.
2026-09-30 — developer (C, líder) — TAKEN se escribe sólo si la versión no tiene registro todavía: volver a rendir no le pisa un
  APROBADO. «exam record» exige un TAKEN previo de esa versión en la historia.

2026-09-30 — integrador — Rebasado sobre el sentinel.yaml real y FEAT-RONDA1 (propose --replace, exit 0): DefaultJudgeProviderReadinessProbe inyecta authStatusCommand: List<String> (pendiente del qa-tester de C).

2026-09-30 — qa-tester — F-JEXAM-J001 en audit-cli, `com.learney.contentaudit.auditcli.commands`.
  why: sus pasos son `exam run`, `exam record` y `analyze`; ExamCmd y AnalyzeCmd son package-private ahí (como F-CLEX-J001).
2026-09-30 — qa-tester — R012 queda sin prueba propia: su criterio («isn't» / «is not») se prueba bajo F-UNICA-R002 y F-MCVALE-R002. Duda de traza para el orquestador (OG1).
  why: la cumplen los clasificadores de UNICA y MCVALE, y `propose` rechaza trazar a otra feature.
2026-09-30 — qa-tester — R006 y la primera consulta sola (R011) se prueban en DefaultEvaluationSession, que el parche no nombra.
  why: es la implementación del contrato nuevo de EvaluationSession (resolveAll, recorded, unavailableReason).
2026-09-30 — qa-tester — Ninguna prueba consulta un modelo pago: evaluador doble con veredictos grabados que cuenta llamadas; `claude auth status` grabado.
2026-09-30 — qa-tester — Hallazgos para otros roles: (1) analista: el criterio de R004 dice 39 horas para 7.400 pendientes, que es a 189/h; al ritmo medido (176/h) son 42. (2) test-writer: la prueba de hoy de F-QINST-R006 en la fábrica afirma 500 y R010 lo lleva a 0. (3) arquitecto: DefaultJudgeProviderReadinessProbe no recibe con qué correr `claude auth status`. (4) SentinelAgentsAgentJudgeChatModelFactory sin pruebas: es cableado sin regla, como el del juez de consigna.

2026-09-30 — architect — Runtime común `JudgeRuntime` (audit-domain.judging; impl en judgingengine, allowedClients
  audit-cli): único lugar que abre sesiones con el freno del examen (tope 0 sin APPROVED), el tope 0 por omisión, las 4 a
  la vez, el plan (conjunto, re-evaluación, EXAM) y la cobertura común (examen por versión, costo, causa).
  why: cinco jueces abriendo cada uno a su modo son cinco lugares donde olvidar el freno o el tope 0.
2026-09-30 — architect — Libro de examen = `ExamScopedEvaluationLedger` (prefijo exam:, reusa sólo la versión que rinde,
  del examen o del curso) sobre las sesiones de siempre; `DefaultEvaluationSession` no cambia de reglas.
2026-09-30 — architect — Sesión: `resolveAll(requests, N)` (decide en orden quién consulta, igual que de a uno; N hilos
  sólo evaluate+append; la primera sale sola), `recorded()` sin contar, `unavailableReason()`. Ritmo en `EvaluationTimeline`
  aparte para no romper los libros falsos de los tests.
2026-09-30 — architect — Propósito en la política: COURSE y EXAM; CANDIDATE descartado (P3): un candidato es un curso
  candidato con COURSE y el informe sin guardar.
2026-09-30 — architect — El examen corre por AuditRunner sobre la foto con un solo analizador y política EXAM; los
  veredictos salen de los hallazgos de 0.3 (reglas de costo pago de la ficha); el resultado se mide afuera y `exam record`
  lo acepta sólo si la versión tiene TAKEN (rindió todo).
2026-09-30 — architect — Adaptadores de los jueces nuevos en quiz-instruction-infrastructure (evaluador genérico
  `AgentJudge` por `AgentJudgeSpec`), no en un módulo nuevo; la verificación de sesión (`claude auth status`, sin modelo)
  y `explain` clasifican la causa. Render y tabla del corrector: los de A (`QuizStudentViewRenderer`, `CorrectorMatcher`).
  why: un módulo nuevo pedía tocar allowedClients de agent-runtime y dependsOn de audit-cli sólo por el nombre.
2026-09-30 — architect — Ritmo = promedio entre veredictos seguidos dentro de cada corrida (176/h en el juez de
  consigna), no la mediana (15,5 s, 232/h): las horas son la suma de los intervalos. R004 corregida con los datos.
2026-09-30 — architect — Medido sin consultar, replicando la huella del juez de consigna sobre la base del 29/9:
  3.920 con veredicto y 7.367 pendientes (A1 1.913, A2 1.845, B1 2.917, B2 692). Script en el scratchpad (ronda1-C).

2026-09-30 — analyst — Decisiones de José, 30/9 (decisiones-ronda1.md): PROVEEDOR → A (claude-cli; venció el 30/9);
  PRESUPUESTO → A para los cinco, consigna incluida (nuevo R010: su tope por omisión pasa de 500 a 0); MUESTRA → A
  (examen fijo, escribir, A1); PARALELO → B (4 a la vez); CONSIGNA-HABILITA → A («sin aprobar, no paga»);
  CONSIGNA-QUE-VE-EL-ALUMNO → A (arreglo de datos fuera del paquete, no se aplica ahora). Las seis RESOLVED.
  why: la diapositiva de «Cómo se paga» ponía «500 por juez: hasta 2.500»: la respuesta cubre a los cinco.

2026-09-30 — analyst — Nuevo R011 (sesión vencida: verificar sin costo antes de la primera consulta, la primera sale
  sola, el informe nombra causa y remedio) y R012 (formas que el corrector acepta; José, 30/9: «que acepte siempre ambas»).
  why: la sesión de claude-cli venció el 30/9; y el cambio de short/long forms toca a los cuatro jueces nuevos a la vez.
  Se agregan al final (R010–R012) para no renumerar lo que citan los requirements 08 a 11.

2026-09-30 — analyst — R001: el examen entra como un archivo con sólo los ejercicios y se mide afuera; content-audit
  guarda el resultado medido sólo para una versión que rindió. J001 suma el paso «registrar».
  why: pedido del orquestador (30/9): el oro lo tiene él y el juez no lo ve.

2026-09-30 — analyst — Nueva DOUBT-EXAMEN-Y-CANDIDATOS (¿el freno alcanza a los candidatos y a revise-instructions?);
  recomendado A: curso y mesa nueva sí; revise-instructions sigue como hoy hasta que la mesa lo reemplace.

2026-09-30 — analyst — Código FEAT-JEXAM / F-JEXAM: nuevo. Lo común a los cinco jueces vive acá (grupos A y B) y los
  otros cuatro lo citan; cada juez fija en su propio requirement qué grupos del examen le tocan y su meta (R006 de cada uno).
  why: evita repetir cinco veces el examen, el costo, el paralelo y las dudas de proveedor y presupuesto.

2026-09-30 — analyst — «Aprueba 178 de los 187 imposibles» (propuesta d-6-13) se lee como que los DEJA PASAR.
  why: verificado con el libro `.content-audit/evaluations/quiz-instruction-validator.jsonl` (último veredicto por
  subjectRef, 3–14/8): «cumple» en 181 de 187 imposibles CLOZE, en el de opción múltiple, en las verduras y en los 50
  buenos. Encaja con «se afinó para que dejara de marcar de más, y nunca se midió lo que deja pasar».

2026-09-30 — analyst — El examen no escribe en el libro del curso y reusa sólo veredictos de la MISMA versión (R002).
  why: en el curso gana el más reciente (F-EVCOST-R006); si el examen escribiera ahí, una versión reprobada quedaría
  vigente en 238 ejercicios. En el examen la versión es lo que se mide; no reabre DOUBT-IDENTIDAD-VERSION.

2026-09-30 — analyst — Medido sin consultar modelos: comparando lo que el juez de consigna leía el 14/8
  (audit-2026-08-14T14-39-40.json del archivo) con la base del 29/9, conservan veredicto unos 3.889 CLOZE si la consigna
  sale del tema (1.065 si sale de la copia del ejercicio); pendientes unos 7.400 (2.810 MC + ~4.590 CLOZE cambiados).
  why: mismos datos de entrada => misma huella; no leí el código del fingerprint. Queda como estimación.

2026-09-30 — analyst — La app muestra la consigna de la copia del ejercicio (mobile quiz.dart usa template.instructions) y
  la traducción y la miniteoría recién en la corrección (QuizFeedback). Copia y tema difieren en 9.923 de 11.287.
  why: base de DOUBT-CONSIGNA-QUE-VE-EL-ALUMNO y de lo que leen los jueces nuevos «con lo que ve el alumno».

2026-09-30 — analyst — Ritmo medido del juez de consigna: mediana 189 veredictos por hora de corrida (unos 19 s cada
  uno, de a una), sobre 15.351 registros del 3 al 14/8. Base de todos los costos del paquete C.

2026-09-30 — analyst — Numeración que citan los otros cuatro: examen de cada juez = su R006; examen del de consigna =
  F-JEXAM-R009; habilitación = F-JEXAM-R003; costo pendiente = R004; conjunto y candidato de la mesa = R005; a la vez = R006.
  why: los requirements 08 a 11 enlazan esas reglas; renumerar acá obliga a tocarlos.
