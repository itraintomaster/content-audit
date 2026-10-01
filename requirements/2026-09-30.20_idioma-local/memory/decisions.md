# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-10-01 — coordinador — OG1 resuelto: lo común de MLOCAL que realizan los analizadores (la fila de cada corrida,
  R002 inv. 3 y R003 inv. 3, y lo que haga falta de R005) se prueba con un segundo parche de pruebas de MLOCAL, después
  de aplicar los cuatro de B, autoaplicado con `patch apply --as=qa-tester`.
  why: `patch propose` exige trazar a la feature del parche, y GrammarAnalyzer, NaturalnessAnalyzer y
  NearDuplicatesAnalyzer no existen cuando se aplica MLOCAL.

2026-10-01 — qa-tester — F-MLOCAL-J001 en audit-cli, `com.learney.contentaudit.auditcli.commands`: su paso es
  `analyze` (AnalyzeCmd, package-private ahí) y lo recorren el runner, la compuerta, los adaptadores y el evaluador de
  claude -p de C, que sólo audit-cli ve. Mismo criterio que F-JEXAM-J001 y F-HALL-J001.
2026-10-01 — qa-tester — Lo común que realizan los tres analizadores se prueba en GRAMA, NATUR y DUPAR con sus propias
  reglas: `patch propose` exige trazar a la feature del parche y esos parches se aplican después. Sin prueba propia: la
  fila de cada corrida (R002 inv. 3, R003 inv. 3) y qué agrega o saca un candidato (R005, lo calcula la mesa de E).
  why: duda de traza para el orquestador (OG1); se cierra con un parche de pruebas de MLOCAL después de aplicar los cuatro.

2026-10-01 — integrador — Rebasado sobre ronda1/integracion y FEAT-RONDA2 (propose --replace, tech-spec write y validate en exit 0): sin cambios de estructura. Lo compartido de B (tarjetas en la ficha, ranuras en QuizDiagnoses y CourseDiagnoses) va en FEAT-RONDA2; el tope por omisión según la ruta es F-RONDA2-R002; localtoolsengine recupera [audit-cli] en el paso de los allowedClients (B-23).

2026-09-30 — architect — La alternativa usa el runtime de C sin duplicarlo: los dos de modelo abren SIEMPRE con
  `JudgeRuntime.open(nombre, [evaluador], política)` —local con `LocalToolGateDecision.localPolicy` (la de la corrida o, sin
  pedido, sin tope y de a una); claude -p con la política de la corrida, que sin pedido es tope 0—. El evaluador de claude -p
  lo arma Main con `AgentJudgeFactory` de C (gancho B-21); cobertura y costo en `getJudgeCoverages` (C-04).
  why: «que B use el runtime de C, sin duplicarlo»; y el tope que no alcanza al modelo local vive en un solo lugar.

2026-09-30 — architect — Fuera `LocalToolExamState` y el `examRegistry` de `DefaultLocalToolGate`: el examen sale de
  `JudgeRuntime.standing` y se lee como `JudgeExamStatus`. `LocalToolRunDiagnosis` pierde `coverage` y suma `alternative`;
  `LocalToolCard` suma `alternative`; `LocalToolGate` suma `alternativeCard(nombre, evaluador)`.
  why: el estado propio no tenía TAKEN ni NO_VERSION, y la cobertura quedaba escrita dos veces.

2026-09-30 — architect — B-06 cambia de sentido: el runner no le da Integer.MAX_VALUE a los de costo LOCAL_MODEL (llegaría a
  claude -p sin tope); deja la política en null y la completa la decisión. `CONTENT_AUDIT_OLLAMA_URL=off` apaga los modelos
  locales a pedido: así se rinde el examen de la alternativa en una máquina con Ollama.

2026-09-30 — analyst — José, 30/9 a la noche: sin Ollama, naturalness y near-duplicates le preguntan a claude -p
  (DOUBT-MLOCAL-SIN-OLLAMA → A). Nuevas R007 (la alternativa, con las reglas de los jueces: 4 a la vez, sesión verificada,
  costo declarado) y R008 (tope 0 sin pedido y «falta el tope»; el modelo local nunca tiene tope). R001: nada sale de la
  máquina con sus herramientas; a claude -p, sólo sin Ollama y con tope. R003: por nombre se rechaza sólo sin herramienta
  ni alternativa. R006: la versión de la alternativa rinde su propio examen.
  why: «sin Ollama no se deja sin evaluar; recordá que tenemos claude -p».

2026-09-30 — analyst — Nueva DOUBT-MLOCAL-TOPE-ALTERNATIVA (OPEN; A recomendada y aplicada: el tope 0 de los jueces).
  why: la nota de José «cuidar que no pase por el tope 0 de los jueces pagos» se lee de dos maneras; el orquestador fijó A.

2026-09-30 — analyst — Costo de la alternativa contado con scripts, sin consultar (scratchpad `arq-B2/`): naturalness
  11.173 consultas (A1 2.334), near-duplicates 699 (A1 276): 67,5 h de a una y 16,9 con 4, a 176/h (ritmo de C).

2026-09-30 — architect — Las herramientas son de este parche: puertos `GrammarChecker`, `YesNoModel` y `EmbeddingModel` (los
  tres extienden `LocalTool`) en `audit-domain.localtools`, y sus adaptadores en el módulo nuevo `local-language-infrastructure`
  (`allowedClients: [audit-cli]`; audit-cli depende de él): LanguageTool 6.8 en el proceso y Ollama detrás de una fábrica.
  Los otros tres parches traen sólo su analizador.
  why: `patch propose` rechaza un `implements` hacia una interfaz de otro parche; y «el modelo y dónde corre» es de MLOCAL.

2026-09-30 — architect — Una sola decisión de publicar para los tres (`LocalToolGate`, con `DefaultLocalToolGate` en
  `localtoolsengine`): herramienta disponible y examen aprobado de la versión examinada (herramienta + methodId del analizador),
  o corrida de examen (`purpose = EXAM`). Usa el registro de exámenes de FEAT-JEXAM; B no declara otro (gancho B-11).
  why: el orquestador pidió no duplicar lo de la ronda 1; la dependencia queda en una sola clase.

2026-09-30 — architect — La tarjeta de la herramienta va en la ficha (campo `tools`, gancho B-01) y el runner rechaza desde ahí lo
  pedido por nombre, antes de cargar el curso (B-05); cada corrida deja una fila por analizador en el curso (B-03). Los números de
  modelo, en el libro de evaluaciones con la versión examinada en el id del evaluador; el libro necesita un índice (B-09).

2026-09-30 — analyst — Nueva DOUBT-MLOCAL-MESA (OPEN, recomendada A): F-MESA-R004 (E) mide los candidatos sólo con los
  instantáneos y R005 pide que los tres de B lean el candidato.

2026-09-30 — analyst — José, 30/9: DOUBT-MLOCAL-DEPENDENCIAS → A (LanguageTool 6.x dentro de content-audit, la vigente es
  `org.languagetool:language-en:6.8` del 5/5/2026; nomic-embed-text y llama3 8B en Ollama; no se baja nada más),
  DOUBT-MLOCAL-DONDE → A y DOUBT-MLOCAL-POR-OMISION → A (los tres en todo `analyze`; en la corrida base, sólo Gramática).
  R004 pasa a VALIDATED.

2026-09-30 — analyst — Naturalidad cambia de método (ver FEAT-NATUR): pregunta sí/no a llama3 en Ollama, que aprueba el examen;
  con eso las dos herramientas de modelo corren en Ollama, ya instalado, y no hace falta bajar nada (DOUBT-MLOCAL-DEPENDENCIAS A).

2026-09-30 — analyst — Repetibilidad: F-MLOCAL-R002 inv. 2 guarda el número de cada ejercicio con su contenido y versión y lo
  reusa. why: sí/no dio diferencias de hasta 0,025 entre la corrida larga y una corta, suficiente para cambiar una marca de borde.

2026-09-30 — analyst — Código FEAT-MLOCAL / F-MLOCAL: nuevo. Aloja lo común a Gramática, Naturalidad y Duplicados parecidos,
  como FEAT-JEXAM aloja lo común de los jueces.
  why: sin esto, cada uno repetía qué pasa sin la herramienta, la versión, la corrida por omisión y el candidato de la mesa.

2026-09-30 — analyst — No se fusionan Naturalidad y Duplicados parecidos: comparten sólo «modelo local», que vive acá; sus
  reglas, modelos, examen y resolución (sólo ordena contra mesa) no se tocan.

2026-09-30 — analyst — Sin la herramienta: pedido por nombre → rechazo antes de empezar; sin selección → sin evaluar con
  motivo, y el resto corre (R003).
  why: F-HALL-R012 rechaza antes de empezar lo que no puede correr; F-HALL-R008 ya cuenta lo sin evaluar.

2026-09-30 — analyst — Se midió sin instalar nada: LanguageTool 6.5 de IntelliJ (Grazie) + jars de ~/.m2 y ~/.gradle en el
  classpath del prototipo; Ollama 0.34.4 con llama3:8b (Q4_0) y nomic-embed-text (137M), ya instalados.
  why: pedido del orquestador y regla de no descargar sin OK. Lo que falta está en DOUBT-MLOCAL-DEPENDENCIAS.

2026-09-30 — analyst — Ollama no devuelve la probabilidad del texto de entrada, sólo la de lo generado (máx. 20 alternativas),
  y la salida forzada con formato JSON da la probabilidad real pero con una tokenización rara. Para SLOR, Ollama no sirve
  tal como está; la pregunta sí/no de FEAT-NATUR sí corre ahí (entrada de arriba).
