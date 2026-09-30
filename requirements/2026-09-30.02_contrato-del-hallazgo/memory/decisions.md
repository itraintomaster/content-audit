# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — architect — Un proveedor por analizador (`AnalyzerProvider`: nombre, ficha, `create(policy)` nuevo por corrida, ligadura al plan, config). Se borra `EvaluationAnalyzerFactory`; la fábrica del juez implementa el proveedor y conserva `analyzerName()`/`create(policy)`.
  why: las dos listas de registro (clásicos y jueces) son la causa de R005 y R012; construir por corrida arregla que los 4 analizadores de curso arrastran estado entre las 2 corridas de la vista consolidada (R014).

2026-09-30 — architect — Hallazgos por «pull»: el motor llama `findingsAt(node)` sólo en niveles de `evaluatedTargets` con puntaje propio, tras el recorrido de cada analizador y antes de agregar; el analizador devuelve `FindingDraft` y el motor estampa analizador, nodo, costo e identidad. Contrato roto = `FindingContractViolationException` (corta).
  why: R001 inv. 2 y R014 por construcción; los ocho existentes no tocan su cálculo de puntaje (R013). El juez da un hallazgo por ejercicio: los códigos de violación los escribe el modelo y no son estables.

2026-09-30 — architect — `evaluatedTargets` se declara en la ficha, no se deduce: SL QUIZ; LABS QUIZ/MILESTONE/COURSE; COCA y LCOUNT MILESTONE/COURSE; LREC COURSE; KTL/KIL KNOWLEDGE; juez QUIZ.
  why: LABS promedia él mismo temas y topics y COCA escribe puntaje de topic sin meta (55 topics < 1 en la base): son claves propias pero no evaluaciones. COCA sigue la tabla del requirement.

2026-09-30 — architect — Ligaduras al plan (proveedor → `AnalyzerPlanBinding`) en vez de las listas y el switch de `DefaultRefinerEngine`: SL/LABS/juez @QUIZ, KTL/KIL @KNOWLEDGE, COCA @MILESTONE+COURSE, LREC @COURSE, LCOUNT ninguna. Verificado sobre la base 2026-09-30T11-54-02: 3.051 + 950 + 25 + 6 + 3 + 1 = 4.036 tareas.
  why: R013; LREC queda en COURSE aunque la lista vieja tenía MILESTONE porque nunca escribe puntaje de nivel (mismas tareas) y así se cumple taskTargets ⊆ evaluatedTargets.

2026-09-30 — architect — R004 inv. 3 como método aparte `RefinerEngine.unconvertedScores`, sólo para la familia de errores; `RefinementPlan` no cambia.
  why: su constructor generado tiene 151 construcciones; y la declaración en vocabulario cambiaría el plan de hoy.

2026-09-30 — architect — Los números (`ContextNumbers`) los calcula el motor una vez; dejan de calcular por su cuenta: `DefaultReportViewModelTransformer` (73,9), `FileSystemAuditReportStore.list` / `GetCmd.printAuditOne` / `RawJsonReportFormatter` (73,4), `DefaultImpactPreviewComputer` y el tablero (B2 51,0 en vez de 63,5).
  why: R010. Medido: 7 claves = 0,73923; 11 claves = 0,73420; niveles 96,54 / 94,16 / 79,45 / 63,51.

2026-09-30 — architect — Resumen del informe (`AuditDigest`) escrito por el almacén al guardar, en un directorio hermano de audits/ (propuesta para developer y paquete G: `.content-audit/audit-digests/audit-<id>.json`, JSON compacto); `get audits` y `get audit` lo leen; delete/prune lo borran con su informe. Cuenta como parte de «su informe» para F-HALL-R015: no toca el curso.
  why: el 92 % del JSON compacto (64 de 70 MB; 146 MB con sangría) son las entidades del curso repetidas en 4 niveles; si viviera dentro de audits/, `list()` y `loadLatest()` lo tomarían por un informe.

2026-09-30 — architect — `runAudit(course)` = selección base de costo INSTANT (hoy los 7 clásicos): vista consolidada y vista previa calculan lo de hoy y nunca pagan juez. Sobrecargas viejas del runner (`Set`, `String`) sólo instantáneos. Falla por nodo aislada sólo para analizadores con modelo, y el motor marca `unevaluatedBy`.
  why: F-QINST-R007 generalizada sin tragarse bugs de los instantáneos; lo que advirtió el arquitecto de QINST el 3/8 sobre stats pagando 500.

2026-09-30 — architect — Tope genérico `--budget nombre=N` (`AnalyzeOptions.analyzerBudgets`); `--instruction-budget` queda de atajo. Re-evaluación sigue siendo sólo del juez de consigna.
  why: R005 «fijarle tope con su nombre»; quita un enganche por juez del paquete C. Acotar corridas está fuera de alcance.

2026-09-30 — architect — Se mantienen los slots tipados de diagnóstico (enganche). El «cajón tipado» (lista de diagnósticos con interfaz marcadora y Jackson por clase) queda como duda 3 de la FICHA, recomendado «no por ahora».
  why: DLABS y el plan aprobado; el cajón suma un segundo patrón y nombres de clase en el JSON.

2026-09-30 — architect — NO se incluye un renderizador «como lo ve el alumno» para la evidencia. Recomendado para el integrador de la ronda 1 (primer consumidor: paquete A), después de 0.2.
  why: P3, y depende de la opción múltiple que declara 0.2; los ocho de hoy arman su evidencia con la oración plana y su diagnóstico.

2026-09-30 — architect — Cambios de cuerpo que el parche no declara y el developer tiene que hacer: los 6 consumidores del número, `StoreHelper` (borrar el resumen), `RecursiveNodeFieldDiffer.walkNode` + `DefaultListIdentityRegistry` (findings por analyzer/rule/identity.marker, numbers), `QuizInstructionAnalyzer` (`unevaluatedBy` en PENDING/FAILED + `findingsAt`), `PlanCmd`/`DefaultEphemeralPlanRenderer` (avisar lo no convertido), `StatsAnalyzerCmd` (pedido exacto, tope 0 si es pago), `AnalyzeCmd` (`--budget`, mensaje de F-CLIRV-R016), `Main` (catálogo y proveedores).

2026-09-30 — analyst — Código FEAT-HALL / F-HALL: nuevo, no aparece en ningún requirement.

2026-09-30 — analyst — El hallazgo es un registro de forma fija, igual para todos; lo propio de cada analizador sigue en su diagnóstico tipado (R001).
  why: el pedido exige mantener DLABS/DCOCA/DSLEN («tipado, nunca un mapa genérico»); campos extra en el hallazgo serían el mapa por la puerta de atrás.

2026-09-30 — analyst — Gravedad, resolución y costo van por hallazgo, no por analizador (R002); el costo de la ficha es el mayor de sus reglas (R006).
  why: pista coherente cambia de gravedad y de resolución según el caso, y opción múltiple mezcla reglas y juez.

2026-09-30 — analyst — Las tareas siguen saliendo del puntaje (F-RCLA-R001 intacta). El contrato sólo exige, en la familia de errores, que un error baje el puntaje, que «sólo ordena» no lo baje y que el plan nunca pierda en silencio un puntaje < 1 (R004).
  why: «una tarea por hallazgo» es del paquete E y cambia algo existente; el hueco del cableado (tipo de tarea olvidado) es de 0.3 según el brief 087.

2026-09-30 — analyst — Una familia por analizador: los 7 clásicos, vocabulario; quiz-instruction y los 11 nuevos, errores (R007).
  why: es lo único que deja el 73,9 % intacto; el caso límite (palabras de otro nivel) quedó como DOUBT-FAMILIA-OTRO-NIVEL.

2026-09-30 — analyst — Puntaje de vocabulario = promedio simple por nodo de los analizadores de vocabulario, uno por analizador, sin sub-métricas (R009, R010).
  why: verificado a mano con el brief 020: (86,4+72,5+89,3+67,0+8,0+98,8+95,5)/7 = 73,9 y B2 = 63,5. El curso promedia analizadores, no niveles. Cifras de nivel tomadas del brief 022 (B1 79,5, no el 79,4 del 020).

2026-09-30 — analyst — «Tema» = knowledge (413); «topic» = agrupación. Los números van también por topic.
  why: en la propuesta y el 087 «tema» es el knowledge («B1 · 147 temas» = 147 knowledges); sumar topic no cuesta y mantiene F-SLEN-R016.

2026-09-30 — analyst — Hallazgos sólo donde el analizador calcula su propio puntaje, nunca donde sólo hay agregación (R001).
  why: mantiene F-DSLEN-R003 y F-DCOCA-R004; si no, cada tema con promedio < 1 tendría un «hallazgo» de largo de oración.

2026-09-30 — analyst — Conteos aditivos; % del analizador sobre lo evaluado; «algún error» sobre todos los ejercicios y declarado como piso (R008).
  why: mantiene F-QINST-R004/R005 (no leer números sobre lo no evaluado); los conteos se suman, los porcentajes no se promedian.

2026-09-30 — analyst — El juez llega a los contenedores = cumplir F-QINST-R004 (hoy incumplida) en la familia de errores, sin mover el vocabulario (R011).
  why: FEAT-QINST ya pedía la agregación genérica; el defecto es que corre después de la suma.

2026-09-30 — analyst — Rechazar el nombre desconocido al seleccionar o excluir (R012).
  why: F-QINST-R015 lo dejó como «mejora de la interfaz sobre cualquier análisis»; el error reusa el de F-CLIRV-R016.

2026-09-30 — analyst — La identidad del hallazgo (analizador + regla + nodo + qué marca) no es la huella de EVCOST (R014).
  why: la huella identifica un veredicto por contenido para reutilizarlo; la identidad identifica un problema en un nodo para que CDIFF lo compare entre fotos (R022 exige identidad declarada).

2026-09-30 — analyst — Sin choques con decisiones existentes. El único número de hoy que cambia (el promedio del ejercicio juzgado) quedó como DOUBT-EJERCICIO-JUZGADO, sin efecto en la base del 29/9.

2026-09-30 — analyst — Diferencia anotada y no resuelta: el largo de oración de A1 es 3–8 en el código y 5–8 en F-SLEN-R012. La ficha muestra lo que aplica el código (regla 4 del brief 087).
