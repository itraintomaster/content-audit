# `content-audit` — referencia de CLI

Herramienta de auditoría y refinamiento de contenido del curso de inglés.

> Este documento describe la **superficie de uso**. La arquitectura vive en los
> `AGENTS.md` de cada módulo, que son generados por Sentinel y no describen cómo
> usar la herramienta.

## Cómo se invoca

```bash
./audit-cli.sh <comando> [opciones]
```

El script arma el classpath y llama a `com.learney.contentaudit.auditcli.commands.Main`.
En los ejemplos de abajo se escribe `content-audit` por brevedad.

## Configuración por entorno

Casi todos los comandos que tocan el curso aceptan la ruta como argumento posicional
o con `-c/--course-path`. Si se omite, se resuelve en este orden: variable de entorno
`CONTENT_AUDIT_CONTENT_FOLDER`, y si no está, un archivo `.env` en el directorio actual.

| Variable | Para qué |
|---|---|
| `CONTENT_AUDIT_CONTENT_FOLDER` | Ruta por defecto del curso |
| `CONTENT_AUDIT_HOME` | Directorio base donde viven `.content-audit/` (auditorías, planes, revisiones) |
| `CONTENT_AUDIT_APPROVAL_MODE` | Modo de aprobación de propuestas |
| `CONTENT_AUDIT_LAPS_STRATEGY` | Estrategia activa de generación de candidatos para ausencia de lema |
| `CONTENT_AUDIT_LAPS_DISABLE` | Desactiva la estrategia de ausencia de lema |
| `CONTENT_AUDIT_LAGEN_PROVIDER` | Proveedor del modelo (generación por agente) |
| `CONTENT_AUDIT_LAGEN_MODEL` | Modelo a usar |
| `CONTENT_AUDIT_LAGEN_ENDPOINT` | Endpoint del proveedor |
| `CONTENT_AUDIT_LAGEN_API_KEY` | Credencial del proveedor |
| `CONTENT_AUDIT_LAGEN_MODE` | Modo de operación del generador |
| `CONTENT_AUDIT_LAGEN_TEMPERATURE` | Temperatura del modelo |
| `CONTENT_AUDIT_LAGEN_MAX_TOKENS` | Tope de tokens por llamada |
| `CONTENT_AUDIT_LAGEN_TIMEOUT` | Tiempo de espera por llamada |
| `CONTENT_AUDIT_LAGEN_MAX_EVAL_RETRIES` | Reintentos de evaluación del generador |

El juez del análisis de consigna se configura con su propio prefijo, separado del
generador. **Ninguna es obligatoria**: sin proveedor declarado se usa el mismo por
defecto que el resto del sistema.

| Variable | Para qué |
|---|---|
| `CONTENT_AUDIT_QINST_JUDGE_PROVIDER` | Clase de proveedor del juez (p. ej. `claude-cli`, `codex-cli`, `openai`) |
| `CONTENT_AUDIT_QINST_JUDGE_MODEL` | Modelo con el que juzga |
| `CONTENT_AUDIT_QINST_JUDGE_BASE_URL` | Dirección del servicio. **Sólo** la exigen los proveedores que se consultan por dirección |
| `CONTENT_AUDIT_QINST_JUDGE_API_KEY` | Credencial. Opcional para cualquier proveedor |
| `CONTENT_AUDIT_QINST_JUDGE_TEMPERATURE` | Temperatura del juez |
| `CONTENT_AUDIT_QINST_JUDGE_TIMEOUT_SECONDS` | Tiempo de espera por consulta |
| `CONTENT_AUDIT_QINST_JUDGE_AGENT_NAME` | Nombre del grafo del juez. Por defecto `quiz-instruction-validator` |

Un proveedor que se autentica con **la sesión local** (los `*-cli`) no lleva dirección
ni credencial: no existen para esa clase, y no configurarlas no vuelve inválida la
configuración. Un proveedor que se consulta **por dirección** sí la exige; si falta, el
juez se declara no disponible y la corrida no emite ninguna consulta.

## El flujo habitual

### Naturalidad y calidad didáctica A1

`naturalness` evalúa cómo se expresa la idea en todas las variantes aceptadas. Reemplaza
el puntaje histórico de naturalidad que mantenía 100 % aunque hubiera alertas. Usa
`native-usage-judge` con salidas `PASS`, `FAIL`, `REVIEW` y `NOT_APPLICABLE`.

`a1-didactic` evalúa por separado la claridad del ejemplo, su objetivo y la carga
incidental en el recorrido A1. Usa `a1-didactic-judge` con salidas `SUITABLE`, `IMPROVE`,
`REWRITE_REQUIRED`, `REVIEW` y `NOT_APPLICABLE`. No invalida inglés ni respuestas
aceptadas por recomendar un ejemplo más claro.

Ambos leen la consigna, el estímulo y las opciones visibles, todas las respuestas
reconstruidas y el orden de lecciones. Los títulos previos no prueban cada estructura
enseñada: si falta esa evidencia, el caso queda `REVIEW`. No usan un filtro POS para
descartar elipsis o frases comunicativas. Las opciones internas de CLOZE no se
presentan al juez como pistas que ve el alumno.

Cada criterio tiene su propia versión, presupuesto y registro de evaluaciones.
Comparten la configuración explícita `CONTENT_AUDIT_JUDGE_PROVIDER`,
`CONTENT_AUDIT_JUDGE_MODEL` y `CONTENT_AUDIT_REASONING_EFFORT`; el proveedor por
omisión es `codex-cli`, sin fallback silencioso. Una salida inválida o un presupuesto
agotado queda pendiente, nunca aprobada. No ejecutar consultas reales sin autorización.

Los defectos propios producen hallazgos para panel, sin reescritura automática.
Una derivación exclusiva a naturalidad, pragmatismo, formato o consigna conserva
ese dueño y no genera otro hallazgo didáctico. El puntaje refleja ausencia de
defectos propios; no mide precisión ni confianza del modelo. Los estados completos
y las cantidades se guardan en los metadatos del informe.

La prueba de desarrollo de 138 ejercicios / 162 variantes de corrección 1 está en
[el informe de resultados](../analysis/naturalness-review/2026-10-14-native-didactic-pilot/resultados.html).
José promovió ambos jueces después de revisar este piloto y sus discrepancias.
Esa adopción por el operador queda registrada aparte de la certificación del
examen fijo. La corrida completa de A1 todavía no se ejecutó.

### Promoción por revisión del operador

`promote-judge` registra la decisión explícita del operador de adoptar una versión
después de revisar sus resultados. Se conserva separada de las mediciones del
examen fijo: no cambia discrepancias, puntajes ni datos del curso.

```bash
content-audit promote-judge naturalness --expected-version <version-revisada> \
  --reviewer José --evidence <archivo-de-decision> --reason "Acepto los resultados del piloto"
```

La versión debe coincidir con la vigente. Se guardan proveedor, modelo, esfuerzo,
revisor, motivo, fecha y la huella del archivo revisado en `.content-audit/judge-promotions/`.
`exam status <juez> -f json` muestra `PROMOTED` y el motivo de adopción; el
historial de exámenes permanece separado. La aprobación no se hereda a otra
versión o modelo, ni a versiones vinculadas solo por una migración de formato.
Una medición de examen posterior reemplaza la decisión para esa versión.

La promoción habilita consultas cuando se solicita un presupuesto. Sin un pedido
de consultas, el tope sigue siendo cero.

```
analyze  →  plan  →  revise  →  approve / reject
```

1. **`analyze`** recorre el curso y produce un informe con puntajes y diagnósticos.
2. **`plan`** convierte el informe en una lista priorizada de tareas de refinamiento.
3. **`revise`** toma una tarea y produce una propuesta de corrección revisable.
4. **`approve`** / **`reject`** deciden sobre la propuesta; aprobar la aplica al curso.

`repair` está fuera de ese flujo: es una operación de mantenimiento sobre contenido
que ya quedó degradado por revisiones anteriores.

---

## Auditoría

### `analyze`

Corre una auditoría de contenido sobre una carpeta de curso.

```bash
content-audit analyze [<ruta-curso>] [opciones]
```

| Opción | Descripción |
|---|---|
| `-f, --format` | `text` (default), `json`, `table`, `raw` |
| `-l, --level` | Bajar a un nivel CEFR (`A1`, `A2`, `B1`, `B2`) |
| `-t, --topic` | Bajar a un topic dentro del nivel. Requiere `--level` |
| `-k, --knowledge` | Bajar a un knowledge. Requiere `--level` y `--topic` |
| `--analyzers` | Lista de analizadores separados por coma |
| `--exclude-analyzers` | Analizadores a dejar afuera de esta corrida, separados por coma |
| `--detailed` | Salida detallada con metadata. Requiere un único `--analyzers` |
| `--instruction-budget` | Tope de consultas **nuevas** al juez de consigna en esta corrida |
| `--reevaluate-instructions` | Vuelve a consultar por contenido que ya tenía veredicto |

```bash
content-audit analyze                                   # todo el curso
content-audit analyze -l B1 -t "Present Simple"         # bajar a un topic
content-audit analyze --analyzers sentence-length --detailed
```

#### Cumplimiento de consigna (`quiz-instruction`)

Este análisis juzga, ejercicio por ejercicio, si cumple lo que pide la consigna de
su knowledge. A diferencia del resto, **consulta un modelo de lenguaje**, así que es
lento y tiene costo. Por eso su corrida se controla aparte.

Corre **siempre**, salvo que se lo excluya:

```bash
content-audit analyze --exclude-analyzers quiz-instruction
```

**Los veredictos se reutilizan.** Cada veredicto queda registrado bajo una huella del
contenido juzgado, no del id del ejercicio —los ejercicios se modifican sin cambiar de
id—. Una corrida posterior reutiliza sin costo todo lo que no cambió, y dedica su
presupuesto íntegro a lo que falta. El registro vive en `.content-audit/evaluations/`.

**El presupuesto es el control de costo.** Acota cuántas consultas nuevas se pagan por
corrida; los ejercicios que quedan sin consultar se informan como pendientes, y la
corrida siguiente sigue por ahí:

```bash
content-audit analyze --instruction-budget 50
```

Si el juez se cae, se queda sin crédito o expira, **la auditoría igual termina**: los
ejercicios afectados se cuentan como fallidos y el resto de los análisis no se ve
afectado. Nada se pierde: lo ya evaluado queda registrado.

Para volver a juzgar contenido que ya tenía veredicto —por ejemplo después de tocar
la definición del juez—:

```bash
content-audit analyze --reevaluate-instructions
```

La cobertura (cuántos con veredicto, cuántos pendientes, cuántos fallidos) se ve con:

```bash
content-audit analyze --analyzers quiz-instruction --detailed
```

### `config analyzer`

Muestra la configuración de un analizador.

```bash
content-audit config analyzer <nombre> [-f text|json]
```

### `stats analyzer`

Muestra estadísticas de un analizador sobre un curso.

```bash
content-audit stats analyzer <nombre> [<ruta-curso>] [-f text|json]
```

Los nombres disponibles salen de `content-audit get analyzers`.

---

## Planes y tareas

### `plan`

Genera un plan de refinamiento a partir de un informe de auditoría.

| Opción | Descripción |
|---|---|
| `--audit` | Id del informe. Si se omite, usa el más reciente |
| `-f, --format` | `text` (default), `json`, `table` |
| `--storage` | `DISK` (default, persiste el plan) o `EPHEMERAL` (emite JSON a stdout sin escribir) |
| `--with-correction-context` | Incluye el contexto de corrección de cada tarea. Solo válido con `--storage EPHEMERAL` |

### `get`

Lee uno o varios recursos.

```bash
content-audit get <tipo> [<id>] [opciones]
```

Tipos: `audits`, `plans`, `tasks`, `analyzers`, `suggested-lemmas`.

| Opción | Aplica a | Descripción |
|---|---|---|
| `-f, --format` | todos | `text` (default), `json`, `table` |
| `--plan` | `tasks` | Restringe al plan indicado. Default: el más reciente |
| `--status` | `tasks` | `pending`, `completed`, `skipped` |
| `--sort` | `tasks` | Solo `priority` |
| `--limit` | `tasks` | Máximo de resultados (`0` devuelve vacío) |
| `-t, --target` | `tasks` | `QUIZ`, `KNOWLEDGE`, `TOPIC`, `MILESTONE`, `COURSE` |
| `-d, --diagnosis` | `tasks` | `SENTENCE_LENGTH`, `LEMMA_ABSENCE`, `COCA_BUCKETS`, `LEMMA_RECURRENCE`, `KNOWLEDGE_TITLE_LENGTH`, `KNOWLEDGE_INSTRUCTIONS_LENGTH`, `QUIZ_INSTRUCTION` |
| `--without-correction-context` | `tasks -f json` | Omite la resolución del contexto de corrección |
| `--part-of-speech`, `--pos` | `suggested-lemmas` | Filtra por categoría gramatical (`NOUN`, `VERB`, …) |
| `--level` | `suggested-lemmas` | Nivel CEFR explícito. Si falta, se infiere de la tarea |
| `--include-exposed` | `suggested-lemmas` | Incluye lemas ya expuestos (default: `false`) |

---

## Revisión

### `revise task`

Corre el flujo de revisión sobre una tarea.

```bash
content-audit revise task <task-id> [opciones]
```

| Opción | Descripción |
|---|---|
| `--plan` | Plan a usar. Si se omite, el más reciente |
| `-c, --course-path` | Ruta del curso |
| `--correction-context` | Override del contexto de corrección, JSON inline |
| `--correction-context-file` | Ídem, desde archivo |

### `approve proposal` / `reject proposal`

```bash
content-audit approve proposal <proposal-id> [--plan <id>] [--note "..."] [-c <ruta>]
content-audit reject  proposal <proposal-id> [--plan <id>] [--reason "..."]
```

Aprobar aplica la propuesta al curso y la persiste. `reject`, sin `--plan`, busca en todos los planes.

### `get-consolidated`

Muestra la vista consolidada (línea base + aceptadas + pendientes) del análisis activo.

```bash
content-audit get-consolidated [<ruta-curso>] [-f <formato>]
```

### `compare`

Proyecta las propuestas pendientes sobre una copia lógica del curso y vuelve a medirla
contra la línea base de la tanda.

```bash
content-audit compare [<ruta-curso>] [-f text|json] [--without-attribution]
```

| Opción | Descripción |
|---|---|
| `--without-attribution` | Conserva métricas, pisos rotos, cambios y veredicto, pero omite el costoso reanálisis individual de cada propuesta para atribuir regresiones. Es el modo usado por el dashboard. |

Sin esa opción, `compare` conserva el comportamiento exhaustivo: para cada propuesta
pendiente vuelve a auditar el curso sin ella. Sirve para investigar qué cambio concreto
rompió un piso, pero una tanda grande puede requerir cientos de auditorías locales. Ninguno
de los dos modos consulta modelos ni publica contenido.

El cierre normal de una tanda —incluido el Análisis iniciado desde el Centro de control—
siempre usa la comparación agregada: aplica todas las propuestas vigentes, audita una vez y
guarda ese antes/después aunque un piso no se cumpla. La atribución exhaustiva queda reservada
para una investigación explícita posterior; no forma parte del análisis ni de su cierre.

### `set-active`

Fija o limpia el par activo (auditoría, plan) que usa la vista consolidada.

```bash
content-audit set-active --audit <id> --plan <id>
content-audit set-active --clear
```

---

## Mantenimiento

### `repair course` — **nuevo**

Restaura atributos que quedaron degradados por revisiones anteriores.

```bash
content-audit repair course [<ruta-curso>] [--dry-run]
```

| Opción | Descripción |
|---|---|
| `--dry-run` | Informa qué se restauraría, sin persistir ningún cambio |

**Qué arregla.** Antes de la preservación del contenido no revisado, al aplicar una
corrección se reconstruía el ejercicio desde cero y se perdía todo lo que la corrección
no tocaba: el tipo de formulario, la incidencia, las etiquetas de presentación, el nombre,
y el texto vacío de cada hueco. El daño medido fue de **2.658 ejercicios**.

Este comando recorre el curso, encuentra para cada ejercicio degradado su contraparte
intacta en el registro de revisiones anteriores, y restaura los atributos perdidos
**conservando la corrección que la revisión sí había aplicado**. Un ejercicio degradado
sin contraparte histórica se informa como no reparable, y **no** se rellena con valores
por defecto: un valor inventado es peor que un hueco declarado.

Corré siempre `--dry-run` primero.

```bash
content-audit repair course --dry-run     # ver qué se tocaría
content-audit repair course               # aplicar
```

### `delete`

Borra un recurso por id.

```bash
content-audit delete <audit|plan> <id>
```

### `prune`

Limpieza masiva por política de retención.

```bash
content-audit prune <audits|plans> --keep <n>
```

---

## Herramientas de consulta

### `tokens`

Cuenta tokens NLP de una oración en DSL de quiz, usando el mismo pipeline spaCy que la auditoría.

```bash
content-audit tokens -q "She ____ [reads] books." [-m FILL|REWRITE] [-l A1]
```

| Opción | Descripción |
|---|---|
| `-q, --quiz-sentence` | La oración en DSL a medir |
| `-m, --mode` | `FILL` (default) o `REWRITE`. Refleja el `sentenceMode` del knowledge |
| `-l, --level` | Nivel CEFR. Si se indica, la salida pasa a ser JSON |

### `lexis`

Consulta las marcas de desubicación léxica de una oración en DSL para un nivel CEFR,
con la misma paridad estructural que usa la auditoría.

```bash
content-audit lexis -q "She ____ [reads] books." -l B1 [-m FILL|REWRITE]
```


## Análisis vigente e integración de corridas

`batch analysis` compone la base y la corrección a partir de las mediciones guardadas y las selecciones explícitas del operador. Leer, previsualizar, integrar, reemplazar o quitar no consulta modelos. Los cierres anteriores conservan su foto.

```bash
content-audit batch analysis --request-json '{"action":"read","batchId":"tanda-A1"}' -f json
```

La respuesta tiene `states` (base/correction, huella, digest, procedencia y ejercicio guardado por juez), `looseRuns` (cobertura y encaje exacto por estado), `history` y `revision`. La huella se construye con el mismo subject builder que usa cada juez. Una medición parcial sólo reemplaza su parte evaluada y compatible. Un extremo sin medir, una versión desconocida o criterios diferentes no prueban mejoras ni regresiones.

Para confirmar se envían los valores que se acaban de revisar, sin inventar ids ni huellas:

```json
{"action":"integrate","batchId":"tanda-A1","state":"correction","runId":"corrida-guardada","operationId":"operacion-unica","expectedRevision":0,"contentVersion":6,"contentFingerprint":"huella-devuelta-por-read","reviewer":"operador"}
```

`replace` exige confirmación cuando hay mediciones previas. `remove` lleva `removesOperationId`: deja de usar esa selección y restaura la anterior, conservando la corrida y el registro de quién confirmó y cuándo. Las confirmaciones son idempotentes y rechazan una revisión obsoleta. El registro separado vive en `.content-audit/analysis-selections/` y comparte el bloqueo de publicación.

`batch selective-correction` acepta `state: "base"|"correction"` y `force: true`/`action: "reanalyze"`. Primero se usa `estimate` con el proveedor/modelo elegido; luego se confirma un `queryCap` explícito con huella y versión. El tope incluye todos los evaluadores del juez. La versión nueva queda suelta hasta su integración manual; los veredictos anteriores conservan su versión. Se admiten Calidad de distractores, Consigna, Respuesta única, Sentido de traducción, Validez de opciones y Mini teoría. Mini teoría usa la entrada del tema completo.

`action: "correct"` admite Calidad de distractores (`multiple-choice-quality`), Respuesta única (`single-answer`), Consigna (`quiz-instruction`) y Sentido de traducción (`translation-sense`), con un alcance `PANEL` declarado. El pedido elige una medición selectiva guardada con `sourceRunId` y sus `taskIds`, y exige un tope nuevo de consultas, autor, huella, versión de contenido y versión del juez. Una tarea sólo pertenece a la medición y al analizador que la originaron; las alertas `RANK_ONLY` no entran como correcciones. Las tareas ya intentadas necesitan una nueva medición antes de otro reintento.

Para Respuesta única, las alternativas de un mismo quiz se trabajan juntas. La mesa puede corregir enunciado, pista, respuestas, traducción y miniteoría de ese quiz; valida la tarjeta contra el ejercicio actualizado antes de aceptar el candidato y devuelve los motivos al corrector dentro del límite de intentos. Una tarjeta compartida en otros quizzes queda sin cambios. El resultado queda como propuesta pendiente de revisión, conservando las decisiones existentes, sin volver a cerrar ni publicar la tanda completa. No se ejecuta el reintento sin un `queryCap` explícito.

Un reintento selectivo conserva el motivo de propuestas descartadas previamente del mismo quiz, aunque sus tareas nuevas comiencen con cero intentos. Ese contexto llega al corrector desde el primer intento; no se vuelven a cobrar los intentos históricos. Por ejemplo, si `When is she at home?` se devolvió por parecerse a otro ejercicio, el corrector ve ese motivo y debe proponer una frase distinta dentro del objetivo del tema. Consigna y Sentido de traducción pueden acompañar ese arreglo con pista, traducción y miniteoría del quiz objetivo; los demás quizzes y sus decisiones se conservan.

Publicar recalcula la comparación y la compuerta desde la selección vigente, respeta las aprobaciones y exclusiones y guarda el informe compuesto. No reutiliza una compuerta histórica como autorización ni ejecuta consultas adicionales. `get student-view <tema> --audit <id> -f json` permite leer el ejercicio de un informe histórico concreto.

Para reanalizar ambos estados, `state: "both", force: true` devuelve una estimación con `scopes`: consultas de la corrección y consultas adicionales de la base. Las entradas idénticas se miden una vez. Un único `queryCap` limita el gasto completo; en la base sólo se reutilizan resultados recién medidos con la versión exigida. Se guardan dos mediciones sueltas, vinculadas a una ejecución y con integración manual independiente. Una reserva incierta nunca se retoma automáticamente.

La ficha de integración incluye errores compatibles con la base, errores actuales y resultantes por juez y las versiones anteriores. Esos datos permiten mostrar el desglose y el antes/después sin atribuir los errores de ejercicios cambiados a la corrección cuando la base sigue sin medir.

Cada encaje de `looseRuns[].fits[]` parte lo que encaja (`compatible`) en seis cuentas que no se pisan y suman exactamente `compatible`: `selected` (ya cuenta), `gaps` (llena un hueco: ahí no contaba ninguna medición) y las cuatro formas de `replaces`. `toToday` trae el juez de hoy donde lo que cuenta no lo es; `toOtherVersion` trae una versión que no es la de hoy, o el juez no tiene versión de hoy; `changesResults` es la misma versión con otras marcas; `repeats` es la misma versión con las mismas marcas. El juez de hoy es la referencia de los jueces al día: si su versión aprobó, cuenta sólo un veredicto «al día»; si no aprobó, cuenta la versión de hoy (o una registrada como la misma) aunque no esté aprobada. `todayApproved` dice si la versión de hoy aprobó su examen y `runToday` si todos los veredictos que encajan son del juez de hoy; los dos son `null` si el juez no tiene versión de hoy, y `runToday` también si nada encaja. `lostErrors` y `newErrors` siguen abarcando todo lo que encaja, huecos incluidos.

### Adjuntar opiniones preliminares guardadas

`batch opinion` admite `estimate-preliminary` e `import-preliminary` para anexar evidencia de una prueba a las propuestas que esa prueba evaluó. `sourceRef` señala una carpeta con `photo-manifest.json`, la foto `course/`, las métricas de Naturalidad, Didáctica A1 y Pragmatismo y sus registros originales bajo `trial-workdir/.content-audit/evaluations/`. No consulta modelos.

Primero se estima el pedido; luego se confirma con la `contentVersion` y la `fingerprint` devueltas, un `operationId` nuevo, `reviewer` y `queryCap: 0`. Una `selection` opcional limita los quizzes y jueces a adjuntar. La fuente debe conservar proveedor, modelo, versión, fecha, veredicto y huella del registro; la foto de cada quiz debe coincidir exactamente con su propuesta vigente.

```json
{"action":"estimate-preliminary","batchId":"tanda-A1","sourceRef":"/ruta/a/la/prueba"}
```

La importación guarda una opinión `PRELIMINARY`, visible por separado junto al quiz. Su origen `EXAM` se conserva aunque el operador habilite después al juez. No la convierte en una medición del curso, no completa la cobertura del análisis vigente ni cambia la aprobación de las propuestas o de los jueces. Las mediciones formales guardadas se integran mediante `batch analysis`.
