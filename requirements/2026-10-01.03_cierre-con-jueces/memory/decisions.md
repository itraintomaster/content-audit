# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-10-03 — developer (CJUEZ) — En el lado «después» del recorte de ejercicios sólo entra el ejercicio que difiere de
  su versión de partida (`!Objects.equals`), sin mirar si cambió la consigna de su tema; el recorte de temas sí cuenta el
  tema o cualquiera de sus ejercicios (G al pie de la letra).
  why: el juez de consigna lee la consigna propia del ejercicio (`AuditableQuiz.instructions`) y el libro de cambios, al
  componer una propuesta del tema, sólo reemplaza la del tema; incluir de más sólo infla la estimación, que ya avisa que
  puede sobrar.
2026-10-03 — developer (CJUEZ) — `BatchJudgeAccount.budget` es lo que el pedido le da al juez cuando le toca, haya o no
  algo que pagar: su `--budget`, lo que queda de `--cap`, o el menor de los dos (puede ser 0). Un juez sin aprobar sólo
  muestra el `--budget` con que se lo nombró, sin tocar el `--cap`; uno local, nulo. Sin pedido (estimación), nulo.
  why: es lo que lee el informe como «el tope que tuvo»; un sin aprobar nombrado con 600 muestra «tope 600» y «sin
  aprobar», que explica por qué no gastó.
2026-10-03 — developer (CJUEZ) — Un juez sin cobertura de juez (grammar) cuenta como unidades los ejercicios del recorte
  con una marca suya (puntaje, sin evaluar o evaluado en parte); su examen y su motivo salen de la corrida de su
  herramienta (`examState`, y `reason` si no publicó), y es local si esa corrida no es la alternativa de claude -p. Una
  herramienta sin examen (no instalada) queda `approved = true` y sin unidades: no entra en la cuenta ni bloquea.
  why: es la lectura del diseño (A.3 y G); el comparador usa la misma regla para decidir si un juez lee un ejercicio.
2026-10-03 — developer (CJUEZ) — `DefaultBatchCloser` guarda la tanda apenas el paso gastó algo, antes de la compuerta, y
  no guarda defensas para un `judgePass` nulo: un cableado roto tiene que fallar fuerte, no cerrar sin jueces.
  why: lo gastado ya está pagado y en el libro de veredictos; si la compuerta falla después, la tanda no puede perder la
  cuenta de lo que costó.
2026-10-03 — developer (CJUEZ) — El servicio valida primero los topes negativos y después los nombres de `--budget`
  (contra `judges(batch)`, que sólo se pide si hay `--budget`); si la tanda no tiene jueces el mensaje dice «Sus jueces:
  ninguno.». El CLI valida también los negativos antes de llamar al servicio. El bloque «Jueces (n)» no se imprime si no
  hay cuentas.
  why: F define los textos pero no el orden ni los casos vacíos; no hay prueba que los fije.

2026-10-03 — test-writer (CJUEZ) — F-CJUEZ-J001 va por el comparador REAL (`DefaultBatchComparator` con su libro de
  metas), no por uno escrito a mano como el de FNOEMP J001 ni como sugería el pedido.
  why: con uno de mentira el veredicto que nombra al juez sin aprobar (R004) y los pisos por juez (R005) los escribiría la
  propia prueba y el camino 2 no probaría nada; el real funcionó sobre las mediciones del banco sin tocar su código. Lo
  único de la tanda que sigue a mano es el proyector (qué mide el «antes» y el «después»).

2026-10-01 — qa-tester (CJUEZ) — Sin prueba, como dudas al orquestador: la línea «batch close --budget …» de batch
  estimate (ninguna regla la pide), el pedido guardado en BatchClosing (R005 pide el tope por juez, que sí se prueba) y
  la herramienta local no instalada como «no sin aprobar» (R004 no decide ese caso).
2026-10-01 — qa-tester (CJUEZ) — TECH_SPEC.md sin tocar: nada lo exige (sólo valida tech-spec write/append) y sus 12
  fences son del parche de arquitectura ya aplicado; append fallaría porque ya no son subconjunto del parche de pruebas.
  why: write --replace perdería la narrativa del arquitecto.
2026-10-01 — qa-tester (CJUEZ) — El parche de pruebas se aplica sin --as=qa-tester: el guardia rechaza packages, los
  _change y el validation del journey, y todas las implementaciones de la tanda viven en paquetes (como FILAS y NOEMP).

2026-10-01 — architect (CJUEZ) — Parche de FEAT-CJUEZ sin pruebas: propose --replace y validate dan 4 altas, 11
  cambios, 0 bajas, 0 conflictos; TECH_SPEC.md con 12 fences. Entrada guardada en el scratchpad de la sesión.
2026-10-01 — architect (CJUEZ) — BatchJudgePass interno de batchengine (estimate, judge, judges) y DefaultBatchJudgePass
  con AuditEngine, CourseMapper y AnalyzerCatalog. Recibe el curso compuesto como argumento; la fábrica arma uno solo y se
  lo da al cierre y al servicio (judgePass al final de los requiresInject de DefaultBatchCloser y DefaultBatchService).
  why: es un colaborador del motor de la tanda; sin armar el compuesto no depende del libro de cambios y sirve para
  estimar y para pagar.
2026-10-01 — architect (CJUEZ) — BatchCloseRequest y BatchJudgeAccount son records del paquete público batch. Los campos
  nuevos de BatchClosing, BatchEstimate, BatchReport y BatchServiceConfig van al final de cada uno.
  why: audit-cli los lee; al final no se mueve el orden de los existentes, sólo crece el constructor con todos los campos.
2026-10-01 — architect (CJUEZ) — Las firmas que cambian (BatchService.close, BatchCloser.close,
  BatchCommand.closeBatch) van como delete de la vieja más add de la nueva.
  why: patch apply ignora en silencio un add sobre un elemento que ya existe.
2026-10-01 — architect (CJUEZ) — BatchCostEstimator no cambia de contrato: el servicio llena changedQuizzes, judges,
  judgeQueries y judgeHours con lo que da el paso. changedQuizzes tiene que salir del mismo ayudante que usa el paso para
  elegir qué juzgar (por ejemplo BatchBook).
  why: el pedido no lo cambia, y una sola cuenta evita que «sobre los N ejercicios» diga otra cosa que lo juzgado.
2026-10-01 — architect (CJUEZ) — BatchJudgeAccount.approved es false sólo para un juez sin aprobar: una herramienta local
  no instalada no lo está y su motivo va en reason. Un pedido de cierre nulo vale como vacío. A confirmar por el orquestador.
  why: sigue el punto C del diseño («una herramienta que no está instalada no es sin aprobar»).
2026-10-01 — architect (CJUEZ) — Sin cambios de contrato en comparison, comparisonengine ni audit-domain: R004 es cuerpo
  del comparador. Sugerencias de glosario para el analista: «Cuenta del juez» y «Paso de los jueces».
2026-10-01 — analyst (CJUEZ) — DOUBT-CJUEZ-VERSION queda abierta y rige A hasta que José conteste: un veredicto del
  libro dado por otra versión del juez cuenta para decidir, como en todo el curso (F-EVCOST-R006), y el informe sigue
  mostrando las versiones.
  why: B vuelve a pagar todo lo que juzgó otra versión y cambia F-EVCOST-R006 dentro de la tanda.
2026-10-01 — analyst (CJUEZ) — Interpretaciones del analista, a confirmar con José:
  - «pagar» es consultar a claude -p. Lo que un juez le pregunta a su modelo local no suma al total de la estimación
    ni gasta el tope del cierre: lo juzga la comparación sin tope (R002, R003, [ASSUMPTION], F-MLOCAL-R008);
  - el veredicto nombra a los jueces sin aprobar en una sola frase al final del detalle: «juez sin aprobar: A, B, …»,
    cada juez una vez y en el orden del catálogo (R004);
  - R001 inv. 1 dice «no se paga» y no «no se consulta», porque al comparar los jueces locales siguen consultando sin
    tope, como hoy (F-RONDA2-R002).
  why: el pedido no decía qué pasa con los jueces locales al estimar y al pagar, ni si «uno por juez» era una frase
  por juez o una lista.
2026-10-01 — analyst (CJUEZ) — Decisiones de José del 1/10: «no publiques sin los jueces» y «al cerrar la tanda corre
  a los jueces». Quedan en R001 (se juzga lo que cambia, antes y después), R002 (se estima antes de pagar), R003 (se
  paga sólo con un tope pedido), R004 (un juez sin examen aprobado bloquea la publicación y el veredicto lo nombra) y
  R005 (cuentas por juez en el informe).
  why: la tanda A1 de prueba (control del paquete E sobre una copia, 1/10) cerró «no se puede decidir», con 28 pisos
  sin decidir y 601 cambios sin evaluar, sin decir qué juez faltaba.
