# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer (impl A) — El #1194 (9915461ac) está en producción desde el 1/10 a las 04:06 UTC: su copia pasa
  a ser formas-equivalentes.json, con las cuatro reglas, y sus 192 casos de oro, casos-de-oro/. La del #1191 queda como
  formas-equivalentes-corrector-1191.json, con sus 153 casos en casos-de-oro-corrector-1191/, para medir el antes y el
  después; la vacía sigue siendo la del corrector viejo. Ninguna copia duplicada. Las pruebas de oro prueban cada
  versión con su carpeta, y «hoy» las dos con la vacía. Los límites del #1191 que el #1194 arregló (forgotten, overslept,
  till) se prueban contra la copia del #1191; «has trained» sigue siendo límite con el #1194 (la aceptada ambigua queda
  como está escrita).

2026-10-01 — architect (impl A) — Las reglas del corrector que son código las dice la tabla en rules, una lista de
  CorrectorRule (enum), como invertsNegativeQuestions: sin bandera en el código (pedido del coordinador, 1/10). Enum y
  no textos: una regla que content-audit no sabe imitar para la corrida al leer la tabla. Hace falta el parche porque
  FormEquivalenceTable es generado; mientras tanto el código queda probado en la simulación.
2026-10-01 — architect (impl A) — accepts es el Matches de Go (el alumno recibe la duda; la aceptada, los paréntesis);
  sameAnswer, que usan opción múltiple, datos y duplicados, es «el corrector toma cualquiera por el otro»; canonicalForms
  no cambia. Sin reglas, todo es el #1191 igual (los 153 casos de oro lo prueban).
2026-10-01 — qa-tester (impl A) — Cada corrector prueba sus casos de oro: los 192 de la rama no tienen columna del #1191
  (326a tiene la misma regla until-till que los de antes y el #1191 lo rechaza), así que no se puede derivar qué daba;
  los 153 de casos-de-oro/ siguen siendo los de producción hasta el deploy.
2026-10-01 — developer (impl A) — El generador contrae have después de los adverbios (not, already…): sin eso
  answer-completeness no veía 4 de los 14 de contracción ambigua que contó el censo de Go («She's not responded yet»).
  Los 38 de paréntesis no los ve answer-completeness (no es una clase de R002 a R004): los cuenta el censo aparte.

2026-10-01 — qa-tester (impl A) — Los dos nombres van en un parche de esta carpeta y no en el de PISTA: ninguna prueba
  aplicada en el repo traza a otra feature que la de su carpeta. La regla de cada una no cambia (R005 y R006).

2026-10-01 — analyst (impl A) — R006 dice lo que hace la ficha desde el límite del corrector: resoluciones regla, mesa y
  sólo ordena; el puntaje baja sólo con un hallazgo de regla o de mesa (F-HALL-R004 inv. 2). R005 no se toca.
  why: la decisión del 1/10 (brief 101) sumó RANK_ONLY a la ficha y a su prueba; el texto de R006 seguía diciendo «regla
  y mesa» y «con hallazgo, puntaje 0».

2026-10-01 — developer (impl A) — Una forma que falta de una clase que el corrector en producción ya iguala (contracción,
  negación de be, grafía de la lista, until/till) y que igual rechaza es un límite del corrector: se arregla en el corrector y
  no en el curso, y el hallazgo nunca propone «|». Nombra la regla que le falta, preguntándole al propio corrector: «'s»/«'d»
  ambiguo (lo deja como está porque no sabe si es is o has, had o would: tiene que leerlo con la forma larga de la respuesta
  aceptada), falta un participio (lee «'d» como would porque no conoce «overslept») o la regla de till toma «there» como
  sujeto. Resolución sólo ordena (RANK_ONLY): de las tres que existen es la única que no cambia el ejercicio, y por
  F-HALL-R004 inv. 2 no baja el puntaje, así que el plan no hace de ella una tarea de contenido; regla y mesa sí cuentan
  como error. La ficha suma RANK_ONLY a sus resoluciones. Gravedad, la de R005: media en las 12 de hoy. La prueba de «has
  trained» espera el texto del corrector. Cuando el corrector instalado no iguala la clase (sólo con la tabla vieja) sigue
  siendo regla, «la iguala el corrector nuevo», como dice R005.
  why: brief 101, José 30/9: las equivalencias van como regla del corrector y el «|» queda para respuestas de verdad
  distintas; lo decidió el coordinador el 1/10 (R005 está bien y no se toca; el código y la prueba de «has trained» lo
  contradecían). Las tres reglas del corrector de Go las propone el coordinador en ittm.

2026-10-01 — developer (impl A) — Una sola copia de la tabla del corrector en producción (formas-equivalentes.json). La
  vacía del corrector viejo se llama formas-equivalentes-corrector-viejo.json y es dato de prueba: la columna «hoy» de los
  casos de oro y las pruebas que necesitan un corrector que no iguala ninguna clase para ver las reglas del analizador
  (oldCorrector()); el resto de las pruebas usa la de producción (productionCorrector()). content-audit no la lee al
  analizar.

2026-09-30 — developer (impl A) — El generador no inventa formas que nadie escribe: un sujeto sustantivo sólo va de contraída a larga
  («Peter's reading» → «Peter is reading», nunca «The car's»), have como verbo principal no se contrae («I have a car»), al final
  de la oración tampoco («than he is.»), «'s» ante been/got/gotten es has y ante un participio adjetivo sin objeto es is («It's
  crowded»), y «'d» ante un participio igual a la base («She'd run») se saltea porque no se sabe si es had o would.
  why: cada forma falsa sería un hallazgo que manda a agregar con «|» algo que el alumno no escribe.

2026-09-30 — developer (impl A) — La copia de producción de la tabla (formas-equivalentes.json) queda vacía, con la versión
  «corrector de hoy»; la del corrector nuevo va al lado (formas-equivalentes-corrector-nuevo.json) sólo para medir y para las
  pruebas. La resolución de cada forma que falta sale de preguntarle al corrector instalado si ya iguala su clase.

2026-09-30 — integrador — Rebasado sobre el sentinel.yaml real y FEAT-RONDA1 (propose --replace, exit 0): CorrectorMatcher.accepts recibe GapContext (antes y después del hueco, sin pistas) y suma sameAnswer(GapContext, …) para opciones de un hueco; modelo GapContext en correctormatch (pendiente del qa-tester de A).

2026-09-30 — qa-tester — F-RESPC-J001 en audit-domain, paquete answercompleteness: el analizador y sus colaboradores son package-private y los puertos se simulan; mismo criterio que F-QINST-J001/J002 y F-LREC.
2026-09-30 — qa-tester — Casos de oro: dos pruebas de DefaultCorrectorMatcher, columna «hoy» con la tabla vacía y «acepta» con la copia de la tabla nueva; fallan si copia y casos no coinciden.
  why: los casos de Go (testdata/, 4 archivos, 153 casos hoy) no traen tabla: la tabla del corrector es código; los casos son el único contrato.
2026-09-30 — qa-tester — Escalado al arquitecto: el corrector lee «'s»/«'d» con el contexto del hueco (antes y después, sin pistas) y CorrectorMatcher.accepts no lo recibe; sin eso fallan la prueba «acepta» y «is» por «'s» en «It ___ (be) hot inside.».
2026-09-30 — qa-tester — «has trained» (clase «límite» de los casos de oro, 3061) lee R005 como: lo que el corrector nuevo sigue rechazando se agrega con «|». Para el analista: R005 no nombra los límites.
2026-09-30 — qa-tester — Para el analista: R004 pone has got/has gotten en la lista cerrada y grafias.json de Go lo deja afuera (acepta falso); y formas-sin-apostrofo.json («dont» = «do not») no está en R001.
2026-09-30 — qa-tester — Dos pruebas del detector siguen la recomendación de dudas abiertas: Contracciones no se saca por regla (DOUBT-RESPC-CONTRACCIONES) y la miniteoría que nombra sin pedir no es pedido (DOUBT-RESPC-DESCRIBE).
2026-09-30 — qa-tester — No se declara que falte el archivo de la tabla: ninguna regla lo pide.

2026-09-30 — architect — CorrectorMatcher (raíz de audit-domain) + paquete correctormatch: la comparación del corrector con su tabla, en
  un solo lugar; la usan RESPC, OMVAL, DATOS y DUPEX. canonicalForms es la primitiva (Datos necesita «contiene», Duplicados agrupa).
  why: José, una sola tabla (la de Go); cuatro reglas dicen «como el corrector».
2026-09-30 — architect — La tabla es dato (FormEquivalenceTable) leída de una copia en analysis/recursos-compartidos/corrector/ por
  FileSystemFormEquivalenceTableSource (vocabulary-infrastructure). La copia es la del corrector en producción: hoy, tabla vacía.
  Modelos = interfaz esperada hasta que la rama del corrector fije el formato. 's y 'd con dos expansiones; invertsNegativeQuestions.
2026-09-30 — architect — El generador de formas usa reglas propias (R002 a R004), no la tabla: así una equivalencia que la tabla pierda
  vuelve como hallazgo (control de que nada vuelve atrás). Para 's = is/has usa IrregularFormsCatalog (de FEAT-PISTA).
2026-09-30 — architect — R007: FormRequest.removableByRule separa los 654 de la oración estándar (regla) de los 40 de Contracciones
  (mesa). El arreglo es seguro sólo sin formas CONTRACTION/BE_NEGATION faltantes en el ejercicio: lo decide E con el mismo diagnóstico.
2026-09-30 — architect — planBinding y config vacíos: sin DiagnosisKind en esta ronda; el plan declara los puntajes no convertidos.

2026-09-30 — analyst — DOUBT-CONTRAIDA-LARGA → A sin excepción por consigna (José, 30/9): «Que acepte siempre ambas.» Se borró
  la excepción de R002 y la decisión «La consigna manda»; R002 y R003 valen pida lo que pida la consigna.
  why: José lo cambió; la forma contraída y la larga son siempre iguales, como con el corrector nuevo.

2026-09-30 — analyst — Regla nueva R007 (José, 30/9): consigna o miniteoría que pide short/long forms = hallazgo; arreglo: sacar el
  pedido; el audio no cambia. Censo 29/9: 694 consignas (654 con la oración estándar del 26/9, 40 de Contracciones) y 1
  miniteoría que pide (3 ej.); 2 miniteorías de B2 nombran la forma sin pedirla (40 ej.) → DOUBT-RESPC-DESCRIBE.

2026-09-30 — analyst — DOUBT-GRAFIAS → A y DOUBT-PREPOSICIONES → A (José, 30/9): lista cerrada de grafías y until/till van a la
  misma tabla del corrector (R004), no con «|» ejercicio por ejercicio.

2026-09-30 — analyst — DOUBT-RESPC-IMITAR → A (José, 30/9): una sola tabla, la del corrector de Go, con casos de oro compartidos;
  content-audit corre los mismos casos. La copia de content-audit es la del corrector en producción.
  why: orden de salida corrector primero y contenido después; una copia adelantada haría sacar consignas antes de tiempo.

2026-09-30 — analyst — Cuánto queda (prototipo del arquitecto, 30/9): la regla vieja marca 1.246 (pronombre) o 1.608 (cualquier
  sujeto) contra 1.251/1.743 del analista; sin la excepción, 1.898. Con el corrector que iguala contracciones, grafías y
  until/till quedan 142 (B1 112, B2 30), todas preguntas negativas o question tags con inversión; 0 si el corrector mueve el not.
  Si el corrector toma 's sólo como is y 'd sólo como would, quedan 345.

2026-09-30 — analyst — Contracciones (A1, 40 ej.): el pedido de forma es la lección; con el corrector nuevo el ejercicio deja de medir
  → DOUBT-RESPC-CONTRACCIONES (recomendado: mesa, a opción múltiple con contracciones distintas).

2026-09-30 — analyst — Código FEAT-RESPC / F-RESPC: nuevo.

2026-09-30 — analyst — La comparación del corrector sale del código, no del 077: be/domain/quiz/pkg/shared/cloze_sentence_part.go
  (Evaluate/normalize): separa sólo en «|», minúsculas, saca espacios, ! ? : ; , . - _ ' " ` ′ ‘ ’ “ ” y tildes (desde #884, 19/5/2025).
  why: el pedido decía «confirmalo con el brief 077 y la propuesta», pero el LEEME del examen prohíbe abrir el 077 (tiene el oro);
  se confirmó con la propuesta y el código. El caso 22c5 del oro («café») contradice al código.

2026-09-30 — analyst — Gravedad propuesta: alta si la forma que falta está en la miniteoría o el enunciado; baja si es la larga en
  respuesta corta; media el resto. Es determinista; no calca al 077 (33/83/12 en su mitad).
