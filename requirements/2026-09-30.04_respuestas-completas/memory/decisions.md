# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

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
