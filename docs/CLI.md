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

