# Traspaso: «corregir sin empeorar» (content-audit + Learney)

**Fecha:** 4/10/2026. Lo escribió el orquestador (Claude) para que otro asistente siga desde acá.

**Dueño:** José Cullen. Él decide, y todo lo que escribe en producción o gasta cupo pago pasa por él.

**Cómo usarlo:**
- Leé primero las secciones 1 y 2: el contexto y las reglas que no se negocian.
- El resto es referencia: estado exacto, comandos, pendientes por prioridad, cómo correr las pruebas y trampas conocidas.

---

## 1. Qué es y dónde estamos

- **Learney** es una app para aprender inglés, pensada para hispanohablantes.
  - Su curso tiene **11.287 ejercicios** (colección Mongo `quizTemplates`) repartidos en **413 temas** (`knowledges`).
  - Hay cuatro niveles: **A1 (2.402 ejercicios)**, A2, B1 y B2.
- **29/9:** una revisión completa propuso 3.133 cambios en 2.892 ejercicios (brief 077). No se aplicó nada.
  - José pidió **«corregir sin empeorar»**: medir cada arreglo antes y después con content-audit y sus jueces.
  - Sólo se publica lo que mejora sin empeorar nada.
- **content-audit** es una herramienta Java (Maven multimódulo, mantenida como código fuente normal).
  - Analiza el curso con unos 20 analizadores: reglas, modelos locales y **jueces LLM**. Desde el 4/10, el proveedor por omisión es `codex-cli` con la sesión local de ChatGPT; proveedor y modelo siguen siendo elegibles.
  - Arma **tandas de corrección**. En cada una, una IA («el que propone», `panel-proposer`) propone arreglos, content-audit los mide y los jueces cierran antes y después.
  - La UI para ver el antes y después es el **Vocabulary Dashboard**, en Flutter, dentro de ittm.
- **Ya está en producción y no depende de esto:** el corrector de respuestas del backend en Go acepta la forma corta y la larga (#1191) y suma 4 reglas (#1194).

**Estado al 4/10:**

| Paso | Estado |
|---|---|
| 1. Revisión del curso (29/9) | hecho |
| 2. Herramientas en content-audit (rondas 0, 1 y 2) | hecho, en `ronda2/integracion` |
| 3. Exámenes de los jueces | 6 de 7 aprobados; duplicados recalibrado y **sin rendir** |
| 4. Tanda A1 | **en pausa** desde el 3/10: 626 arreglos por regla y 21 de la IA; faltan 302 tareas |
| 5. Cierre con jueces | falta, porque cuesta cupo |
| 6. Revisión de José en la app | falta |
| 7. Publicar en content-audit y llevar a producción | falta; el publicador hay que adaptarlo |

**Decisión vigente de José (4/10):** los procesos con modelo se ejecutan con OpenAI/Codex, donde tiene más cupo; no con Claude por omisión.

> **Importante para el cupo.** Ningún juez, examen o propuesta consulta sin presupuesto explícito. Cambiar proveedor, modelo o versión cambia la procedencia y exige volver a examinar el proceso afectado.

---

## 2. Reglas que no se negocian

1. **No se publica sin todos los jueces aprobados.** El sistema lo hace cumplir (FEAT-CJUEZ R004): un juez sin aprobar que lee algo cambiado bloquea la publicación.
2. **Corregir sin empeorar.** Un cambio que empeora cualquier analizador no se publica. El cierre compara antes y después.
3. **Producción es de José.**
   - El CLI de content-audit nunca escribe en producción: publicar una tanda deja la lista de lo que puede llegar (FEAT-PUBPROD R001).
   - Llevar una tanda a producción es `scripts/publish_revisions.py --batch <id>`, sólo con orden expresa de José: toma sólo lo de la lista, ensaya primero y deja registro (FEAT-PUBPROD R006 a R009).
   - Nada escribe en Mongo de producción sin su OK explícito.
   - `scripts/publish_*` corren sólo en ensayo, salvo que José lo pida.
   - Nunca escribir la URI de producción en un archivo.
4. **El push de content-audit lo hace José.** Nunca activar el merge automático.
5. **Nunca `mvn install`**: pisa el `~/.m2` compartido. Se compila y se prueba con `mvn -o … -Dexec.skip=true`.
6. **Sentinel ya no gobierna el repositorio.**
   - Maven no ejecuta `sentinel verify`, `sentinel report`, generación, PIT ni JaCoCo automáticamente.
   - Todo `src/main/java` y `src/test/java` se mantiene a mano, aunque conserve una anotación histórica `@Generated`.
   - `sentinel.yaml`, `.sentinel/`, baselines y propuestas son material histórico hasta archivarlos; no volver a generar desde ahí.
   - `sentinel-agents` se conserva temporalmente sólo como runtime de los agentes y Codex.
7. **Integridad de los exámenes.**
   - Quien construye o recalibra un juez **nunca** lee los casos del examen: `~/projects/learney/content-audit-corridas/` ni `docs/briefs/077-revision-de-ejercicios/examen/examinar/` en ittm. Las clases de error se le pasan en abstracto.
   - El oro se cambia **antes** de medir y se congela con `congelado.sha256`.
   - **No se reetiqueta después de ver los veredictos.** José lo decidió el 3/10 con single-answer: quedó REPROBADO, y el oro corregido se usó para la versión siguiente.
8. **Consultas pagas sólo con el OK de José,** pasándole antes el número.
9. **No leer ni imprimir** `.content-audit/vocabulary-dashboard-revision-settings.json`: tiene una API key en claro.
10. **Criterios de contenido de José** (detalle en `decisiones-ronda1.md` del tablero):
    - **Dos correctas en opción múltiple.** Si una incorrecta también es correcta y natural, aunque sea menos esperable, son dos correctas (MEDIUM). Lo raro o forzado no cuenta. Quien propone no arma opciones ambiguas.
    - **La consigna manda.** Si el enunciado dice qué usar, el alumno que no lo sigue se equivoca.
    - ***some* en una pregunta neutra** cuenta como segunda correcta (MEDIUM).
    - **Ninguna consigna pide forma corta o larga.** El corrector acepta las dos.
    - **Pista que regala.** La forma base que hay que conjugar no regala; la palabra exacta sin transformar, sí.
    - **Duplicados.** La pregunta es si para el alumno es el mismo ejercicio.
      - Pregunta, negación o modal sobre la misma oración: el mismo.
      - La misma respuesta reordenada, con la misma función: el mismo.
      - La misma respuesta con otra función gramatical: otro.
      - Una paráfrasis con otro verbo que no cambia el sentido: el mismo.
      - Cambiar la persona: el mismo. Cambiar la cosa (water por juice): otro. Un sinónimo: el mismo.
    - **Todo juez lee el ejercicio entero como queda,** con inglés y traducción juntos.

---

## 3. Repos, ramas y carpetas

### content-audit: `~/projects/learney/content-audit`
- `main` = `8b11ade1` = `origin/main`: tiene las rondas 0.2 y 0.3.
- **`ronda2/integracion` = `2b258892`**: la línea principal, con la ronda 1, la ronda 2 y los 6 jueces aprobados. Suite: 3.225 pruebas en verde y 1 salteada (spaCy). Worktree: `.claude/worktrees/ronda2-integracion`.
- **Sin juntar todavía** (los junta `docs/propuestas/tablero-corregir-sin-empeorar/traspaso-juntar.sh` de ittm (hoy en el worktree `busy-meninsky-89eb60`)):
  - `ronda2/recalibrar-duplicados` = `6fc28c21`: el juez de duplicados recalibrado con el criterio de José. Le falta el examen. Sale de 2b258892, así que se junta con avance rápido.
  - `claude/quiz-audio-regeneration-11196a` = `b7071230`: `scripts/regenerate_quiz_audios.py` y el manifiesto del 24/8, para regrabar audios. Sólo scripts y datos, sin Java.
  - `scripts/publish_knowledge_instructions.py`: estaba sin seguimiento en `main`. Publica `knowledges.instructions`; corre en ensayo salvo con `--apply`.
- **Trabajo sin terminar,** que se guarda en su rama y **no se junta a propósito**:
  - `ronda1/consigna-alumno` (worktree `ronda1-consigna-alumno`): cambios al requirement `2026-10-01.01_consigna-que-ve-el-alumno` (corrector de consigna 1.1.0) que nunca se aplicaron.
  - `claude/strange-germain-be1a08`: un arnés viejo de evals de consigna. Probablemente quedó obsoleto con las recalibraciones del 3/10.
- **Ramas remotas viejas, no tocar:** `origin/ISSUE-2`, `origin/feat/rpres-preservacion-contenido-no-revisado`, `origin/feature/adding-audit-public-method`.
- **Reglas para agentes:** `AGENTS.md` y `CLAUDE.md`, en la raíz del repo. Leerlos antes de tocar código.

### ittm: `~/projects/learney/ittm`
Es el repo de Learney: backend Go, app Flutter, landing y la herramienta `pipeline/`.
- **Documentos del proyecto:** worktree `.claude/worktrees/busy-meninsky-89eb60`, rama `claude/busy-meninsky-89eb60`.
  - `docs/briefs/`: briefs 076 a 117, numerados. `docs/index.html` los cataloga y se regenera con `python3 docs/indice.py`.
  - **`docs/briefs/077-revision-de-ejercicios/examen/` queda fuera de git a propósito,** porque es el examen, con su mitad escondida (`examinar/`) y el oro. `traspaso-juntar.sh` lo agrega a `.gitignore`.
  - `docs/propuestas/corregir-sin-empeorar.html`: la propuesta.
  - `docs/propuestas/tablero-corregir-sin-empeorar/`: el tablero.
    - `decisiones-ronda1.md`: §1 a §16, **todas las decisiones de José**.
    - Los resúmenes de cada examen y de cada recalibración.
    - `resumenes-de-sesiones/` y este documento.
- **La UI de antes y después:** rama `claude/antes-y-despues-claro`, con 29 commits sobre master; incluye `claude/g-ui-arreglos`. Worktree: `.claude/worktrees/app-presentation-redesign-9fd3a7`. Código en `pipeline/vocabulary_dashboard`, con 183 tests de Flutter.
  - La app llama a la CLI de content-audit a través del `audit-cli.sh` de la carpeta de la tanda.
  - **La app vieja y la nueva tienen el mismo bundle id:** hay que cerrar una antes de abrir la otra.

### Carpeta de corridas: `~/projects/learney/content-audit-corridas/`
Está fuera de git y **contiene exámenes**: no se le muestra a quien construye jueces.

| Subcarpeta | Qué es |
|---|---|
| `2026-10-01-examen-jueces` | examen y registros de traducción, miniteoría y otros |
| `2026-10-01-examen-idioma` | examen de gramática y naturalidad, y el oro del examen de duplicados v1 |
| `2026-10-01-examen-sa-mc` | examen de una sola respuesta y opción múltiple (oros v1, v2 y v3, cada uno con su congelado) |
| `2026-10-01-linea-base` | el libro de A1 con traducción entera; ahí rindió consigna f38f063f |
| `2026-10-01-app-nueva` | la tanda vieja del 1/10, sólo con reglas; la usa la app vieja |
| `2026-10-03-duplicados-por-par` | el examen v1 de duplicados (juez-bbba37bd5fae, reprobado) |
| **`2026-10-03-duplicados-v2`** | **la muestra nueva A′, congelada, y la copia de 6fc28c21 lista para rendir** |
| `2026-10-03-tanda-a1` | la tanda abandonada (`tanda-A1-20261003T204242Z`, con duplicados). No usar |
| **`2026-10-03-tanda-a1-sin-dup`** | **LA TANDA ACTUAL** (sección 5) |
| `2026-10-03-tanda-a1-sin-dup-vista` | vista previa sin jueces parciales. **No es la tanda real** |
| `parches/` | los parches que aplicó José con `git am`, por fecha y tema |

### Backups: `~/projects/learney/mongo-buckups/2026-09-29-revision-de-ejercicios/`
- La foto de producción del 29/9: quizTemplates, knowledges y courses, sin usuarios.
- `foto-content-audit-78815c3a/db/english-course` es el curso en el formato de content-audit; es la foto que usan las tandas.

---

## 4. Cómo funciona content-audit (lo justo)

- **Módulos:** `audit-domain`, `course-domain`, `refiner-domain`, `audit-application`, `course-infrastructure`, `audit-cli`, `nlp-infrastructure`, `vocabulary-infrastructure`, `audit-infrastructure`, `revision-domain`, `revision-infrastructure` y `local-language-infrastructure`, entre otros. Es arquitectura hexagonal con ArchUnit.
- **CLI:** `com.learney.contentaudit.auditcli.commands.Main`. Comandos útiles:
  - `analyze` (con `--exclude-analyzers <id>` y `--budget <juez>=N`);
  - `plan`, `set-active`, `set-baseline`;
  - `batch open <nivel>`, `batch estimate`, `batch run --cap N [--topics-at-once N] [--queries-at-once N]`, `batch close --budget juez=N,…`, `batch report`;
  - `compare`, `save-projection`;
  - `tray show|publish`;
  - `exam run|record|status` (con `--cases`, `--course`, `--budget`, `--concurrency` y `--out`);
  - `get …`.
  - Cada uno documenta sus opciones con `--help`.
- **Agentes y jueces:** cada uno vive en `.sentinel/agents/<agente>/`, con su `nodes/` (prompts) y su `eval/` (fixtures y `run_evals.py`).
  - El grafo es evaluate → gate_verdict (Python dentro de bash, esquema cerrado) → emit. Si la compuerta falla, el grafo reintenta.
  - **La versión de un juez es el SHA-256 de su carpeta.** Cualquier cambio, aunque sea del gate, da una versión nueva y obliga a examinarlo otra vez.
  - Las compuertas toleran claves vacías, coma colgante, `undefined` y elementos de relleno.
  - Hay una muletilla pendiente sin tolerar: `"gapText": "___"`. La salva el reintento.
- **El libro** (`.content-audit/evaluations/<juez>.jsonl`): guarda cada veredicto por juez, versión y contenido de lo que lee.
  - Una corrida posterior lo reusa gratis. Por eso nada de lo medido se vuelve a pagar.
  - Los registros de examen aprobados viven en `.content-audit/judge-exams/`. Sin registro, el juez no consulta y figura como «sin aprobar».
- **Analizadores y nombre que muestra la app:**

| Analizador | Nombre visible | Qué es |
|---|---|---|
| `answer-completeness` | Exige una forma o rechaza una respuesta válida | regla |
| `multiple-choice-validity` | Opción múltiple mal armada | regla + juez `choice-alternative-judge` |
| `hint-coherence` | Pista que falta, confunde o regala la respuesta | regla |
| `data-coherence` | Audio o respuesta que no coincide | regla + registro de grabaciones |
| `exact-duplicates` | Ejercicio repetido igual | regla |
| `near-duplicates` | Ejercicio casi repetido | nomic-embed-text + juez de pares |
| `translation-sense` | Traducción que no corresponde | dos jueces |
| `single-answer` | Más de una respuesta válida | juez |
| `quiz-instruction` | Consigna que no se cumple | juez |
| `mini-theory` | Miniteoría con error | juez |
| `grammar` | Error de gramática | LanguageTool, local |
| `naturalness` | Oración poco natural | llama3:8b en Ollama, local |
| `lemma-absence`, `lemma-count`, `lemma-recurrence`, `coca-buckets-distribution`, `off-level-words`, `sentence-length`, títulos y consignas | vocabulario y largos | reglas |

- **El flujo de una tanda:**
  1. `analyze`, sin budget: reusa el libro.
  2. `plan`, `set-active` y `set-baseline`.
  3. `batch open A1`.
  4. `batch estimate`.
  5. `batch run --cap N`. La **mesa**, por tema: mide el tema entero con los jueces aprobados, el que propone arma candidatos y un script se queda con el que no empeora nada. Las tareas «por regla» se resuelven solas.
  6. `batch close --budget …`: juzga cada ejercicio cambiado en su versión de partida y en la compuesta, compara contra el inicio de la tanda y da el veredicto.
  7. `tray show` y la revisión de José, con la muestra a ciegas.
  8. `tray publish`: escribe el curso en la copia de content-audit, **nunca en producción**, y deja listados los audios para regrabar.
- **Dependencias locales:**
  - Ollama en esta máquina, con `nomic-embed-text` y `llama3:8b`.
  - LanguageTool 6.8.
  - spaCy, por `SpacyProcessRunner`.
  - La lista COCA en `ittm/pipeline/src/main/resources/vocabulary/lemmas_20k_words.txt`. Por eso cada copia lleva un symlink `ittm` al lado.
  - Java 25: `JAVA_HOME=/Users/josecullen/.sdkman/candidates/java/25.0.2-open`.
- **El que propone** usa `CONTENT_AUDIT_LAGEN_PROVIDER=codex-cli` por omisión; declarar también `CONTENT_AUDIT_LAGEN_MODEL=<modelo>` y `CONTENT_AUDIT_LAGEN_TIMEOUT=180` para que la procedencia sea exacta.
  - Sin el modelo, la mesa no tiene quién proponga y la tanda queda INTERRUPTED.
  - Los jueces nuevos usan `codex-cli` por omisión y conservan los selectores `CONTENT_AUDIT_JUDGE_PROVIDER` / `CONTENT_AUDIT_JUDGE_MODEL` y `CONTENT_AUDIT_QINST_JUDGE_PROVIDER` / `CONTENT_AUDIT_QINST_JUDGE_MODEL`.
  - Los exámenes existentes se rindieron con Claude. Pasar a Codex o cambiar el modelo invalida esos exámenes y exige rendirlos de nuevo.

---

## 5. La tanda A1: estado exacto

- **Carpeta:** `content-audit-corridas/2026-10-03-tanda-a1-sin-dup/`. Es una copia por `git archive` de 2b258892, compilada sin install.
  - `control.sh`: corre Main en la copia, con un entorno limpio.
    - `MODO=sin` es el modo por omisión: el `claude` del PATH falla con exit 97 y anota el intento.
    - `MODO=con`: anota y ejecuta el `claude` real.
    - **Es infraestructura histórica de Claude:** antes de reanudar la tanda hay que regenerar la copia desde el código actual y adaptar el control para interceptar/ejecutar `codex`, sin reutilizar `MODO=con` tal como está.
    - `control.sh --app` es lo que llama la app.
  - `guardia.py`: corre un comando de Main vigilado. Lo frena si los reintentos pasan el tope, si hay N fallas seguidas o si llega a un máximo de llamadas.
  - `desenganchar.py`: lanza con `nohup` y `setsid`. **Las herramientas de las sesiones matan los procesos de fondo a los 30 minutos.**
  - `fakebin/llamadas.log`: una línea con fecha por llamada. Para contarlas: `grep -cE '^2026-[0-9-]+T[0-9:]+ ' fakebin/llamadas.log`. `wc -l` no sirve, porque el prompt ocupa varias líneas.
  - `logs/`, `medicion-cupo/lecturas.txt` y `tramo800.py`, que da las cifras de un tramo.
- **La tanda:** `tanda-A1-20261003T220409Z`, estado **OPEN**.
  - Se abrió **sin near-duplicates** (`analyze --exclude-analyzers near-duplicates`). Es la opción B de José: esa tanda no tenía arreglos de duplicados.
  - **992 tareas en 1.096 ejercicios y 64 temas:**
    - 626 resueltas por regla;
    - la mesa aceptó 21 tareas, en 19 propuestas;
    - 35 sin ganador, en 27 decisiones de la mesa;
    - 8 ya no se sostienen;
    - **302 sin intentar**.
  - De los 56 temas con tareas de mesa, 6 están terminados.
  - **Gastado:** 161 + 802 consultas. Todo quedó en el libro: 700 veredictos nuevos que se reusan.
  - **Vista previa sin jueces parciales, A1 antes → después:**

| Fila | Antes | Después |
|---|---:|---:|
| Ejercicios con algún error | 1.096 | 494 |
| Exige una forma o rechaza una respuesta válida | 554 | 43 |
| Opción múltiple mal armada | 109 | 34 |
| Traducción que no corresponde | 130 | 70 |
| Miniteoría con error | 416 | 342 |
| Ejercicio repetido igual | 17 | 4 |
| Pista que falta, confunde o regala | 23 | 15 |

  - Lo único que empeora es vocabulario, en décimas y dentro de la tolerancia: largo de oración −0,21 y puntaje total −0,04.
  - Consigna y una sola respuesta quedan «sin evaluar» hasta el cierre.
  - Hay un cambio de la mesa que «vuelve» en la vista previa (`task-2864`, deja 12 audios) y no se sabe por qué. **Mirarlo.**
- **El cierre de lo que ya cambió** (622 ejercicios), según la estimación de la CLI: **3.181 consultas**.
  - consigna 1.019;
  - traducción 1.092 (lee una línea por cada respuesta aceptada, así que si se suma una, vuelve a mirar);
  - una sola respuesta 901;
  - opción múltiple 150;
  - miniteoría 19.
  - De 5.042 evaluaciones, 1.861 ya están hechas.
- **Por qué `compare` dice que «empeora» consigna y una sola respuesta:** es cobertura.
  - La línea base las tenía sin medir y las contaba como 0. La mesa las midió y encontró 38: 25 en ejercicios sin tocar y 13 en cambiados.
  - Un piso «roto» dispara `LeaveOneOutChangeAttributor`: unas 630 auditorías completas con LanguageTool, así que el `compare` tarda horas.
  - **`batch close --budget` no se traba:** compara contra el inicio de la tanda, con las dos fotos medidas con el mismo libro, y juzga también el antes de cada cambiado (F-CJUEZ-R001).
- **Cuidados:**
  - **No correr `batch close` sin `--budget`.** No es un ensayo inocente: pone la tanda CLOSED y saca la muestra a ciegas del estado parcial.
  - Para mirar sin escribir: `batch report`, `batch estimate`, `compare` y `save-projection`.
  - **Al cerrar, usar el pedido completo** que propone `batch estimate`, con `--budget` de cada juez por todo lo que falta. Con un tope corto, un cambiado que quedó sin juzgar el antes rompe el piso igual.
- **Hay 26 audios para regrabar:** son propuestas de la mesa que cambian el inglés, por ejemplo la serie *until/till*. El comparador los descuenta como pendientes.

---

## 6. Próxima ejecución con Codex, en orden

**Antes de cada paso que gaste, pasarle el número a José y esperar su OK.** No usar las copias Claude-era: primero crear copias desde el código actual, declarar proveedor y modelo, y verificar el control sin inferencias reales.

1. **El examen de duplicados**, solo: ≈1.105 consultas y unas 2 horas.
   - Carpeta: `content-audit-corridas/2026-10-03-duplicados-v2/`. Tiene `correr.sh`, `guardia.py`, la muestra congelada y `medir_v2.py`.
   - La copia es de 6fc28c21. La versión es «nomic-embed-text@0a109f422b47 · clustering-1 · candidatos-0,80 · juez-f9048aca9c92».
   - **Muestra A′:** 52 pares iguales y 19 distintos. Hay 3 dudosos que no cuentan. Semilla 87. No tiene ningún id contaminado.
   - **Meta:**
     - de los pares marcados, al menos 8 de cada 10 son el mismo ejercicio;
     - de los no marcados, como mucho 2 de cada 10. Equivale a perder como mucho 4 de 52 iguales;
     - la parte 3 tiene que ser estable: dos corridas sobre A1 (2 × 513 pares), en carpetas separadas, con las mismas copias marcadas.
   - Las partes 1 y 2 corren en la carpeta de la corrida 1 de A1 y reusan sus juicios.
   - Registrar con `exam record` sólo si se midieron las tres partes.
   - Lo preparó la sesión «Rendir el examen del juez de consigna, con el filtro nuevo». Leer su carpeta antes de lanzar.
2. **Antes de retomar la tanda,** dos ajustes al que propone. No rinde examen, así que no se invalida nada:
   - **Endurecer su guía** (`.sentinel/agents/panel-proposer/nodes/generate.md`):
     - Si una tarea sólo permite tocar las respuestas aceptadas, el ejercicio vuelve **idéntico** salvo esas respuestas: sin tocar enunciado, pista, audio ni miniteoría.
     - Hoy falla 21 de 21 de `answer-completeness`, porque reescribe el ejercicio entero y la compuerta lo rechaza tres veces.
     - Con la miniteoría, el texto nuevo tiene que llegar exactamente a los ejercicios que la muestran.
     - Commitearlo en una rama desde integración, con sus evals, y copiar la carpeta del agente a la copia de la tanda.
   - **Limitar su pensamiento:** históricamente gastó una mediana de 14.000 tokens de salida para escribir unos 1.000 caracteres.
     - Configurar el esfuerzo bajo sólo para `panel-proposer` usando las opciones admitidas por el Codex instalado; no copiar flags de Claude.
     - Los jueces conservan su configuración y examen propios.
   - **Probarlo con unas 20 propuestas:** comparar tokens por llamada (mediana de 14.000) y la aceptación de la mesa (19 de 48 decididas).
3. **Retomar la tanda,** con unas 4.100 consultas: unas 6 horas con 6 a la vez, o unas 9 con 4. Patrón usado el 3/10, en la carpeta de la tanda:
   ```
   python3 desenganchar.py logs/guardia-batch-run-4.log \
     python3 guardia.py batch-run-4 --tope-reintentos 0.25 --max-fallas-seguidas 3 -- \
     batch run --cap 4500 --topics-at-once 6 --queries-at-once 6
   ```
   - `--cap` cuenta sólo esa corrida, es un tope duro y, al llegar, deja terminar lo que está en vuelo.
   - Verificar que `guardia.py` invoque el control Codex-aware y que `MODO=sin` bloquee y cuente cualquier intento.
   - Medir concurrencia y consumo de Codex en un ensayo corto; no extrapolar la ventana de Claude ni correr el examen y la tanda a la vez.
4. **El cierre:**
   - `batch estimate` (`MODO=sin`) da el pedido completo.
   - Con el OK de José: `batch close --budget quiz-instruction=…,multiple-choice-validity=…,translation-sense=…,single-answer=…,mini-theory=…`, con guardia y desenganchado.
   - Para lo cambiado al 3/10 eran 3.181; con todo, unas 3.500 a 4.000.
5. **La revisión de José:** la app de antes y después, la bandeja y la muestra a ciegas. Después, `tray publish`.
6. **Llevar a producción**, cuando José lo apruebe:
   - Adaptar `scripts/publish_revisions.py`, que hoy escribe sólo `title`, `translation`, `form.sentences` y `form.sentenceParts` de quizTemplates, y `label` e `instructions` de knowledges.
     - **Falta:** las opciones de opción múltiple y la consigna de cada ejercicio.
     - En opción múltiple, el id es la etiqueta en minúscula y sólo se publica `label`.
     - Nunca restaurar documentos enteros de `quizTemplates`: sólo actualizaciones quirúrgicas por campo.
     - Probar en ensayo contra una copia de producción. El `--apply` lo corre José.
   - **Audios:** regrabar las oraciones cuyo inglés cambió, con `scripts/regenerate_quiz_audios.py` (rama de audios) y el registro de `scripts/armar_grabaciones.py` (`db/grabaciones.json`). Subirlos es un paso de José.

---

## 7. Pendientes, por prioridad

1. **Juntar las ramas:** correr `docs/propuestas/tablero-corregir-sin-empeorar/traspaso-juntar.sh` de ittm (hoy en el worktree `busy-meninsky-89eb60`). El push lo hace José.
2. **Próxima ejecución paga:** la sección 6, una vez preparada y verificada la copia Codex-aware.
3. **La segunda tanda de A1:** lo que encuentran consigna, una sola respuesta, opción múltiple y duplicados.
   - Ahí entran los ejercicios imposibles de resolver, que son lo más grave.
   - Parte de la medición queda hecha el viernes, en el libro.
4. **Arreglar el comparador** (`DefaultBatchComparator`):
   - Que un ejercicio cambiado que el juez dejó sin evaluar en la foto de antes cuente como abierto, o sea «sin decidir», y no como «gana un hallazgo».
   - Así los pisos dan «sin decidir» en vez de rotos y no arranca el LeaveOneOut.
   - Son unas 30 líneas, 3 pruebas y aclarar F-METAS-R002/R003. Se mantiene como código y pruebas manuales.
5. **Rendimiento de `compare`:** el LeaveOneOut con LanguageTool tarda horas cuando hay un piso roto.
6. **A2, B1 y B2:** unas 50.000 consultas. Antes de encararlo, medir el cupo OpenAI y bajar el costo:
   - un modelo OpenAI más económico, que obliga a examinar los jueces de nuevo;
   - empezar por los temas que más usan los alumnos;
   - el que propone con menos pensamiento.
7. **Chicos:**
   - falsos positivos de spaCy en pistas: *close*, *dance*, *knock*;
   - la muletilla `"gapText": "___"` en las compuertas;
   - archivar los reportes históricos de Sentinel que ya no correspondan;
   - los ejercicios `…37b0` (necesita «|») y `…3528` del corrector, antes de A2;
   - la pregunta de consigna y formas cortas, sólo si un examen la marca.

---

## 8. Cómo correr las pruebas, del todo o en parte

Todo con `JAVA_HOME=/Users/josecullen/.sdkman/candidates/java/25.0.2-open`, Maven offline y **sin install**.

- **La suite entera** quedó verde el 5/10 con JDK 25, desde el repo:
  ```
  mvn -o -q test -Dexec.skip=true
  ```
  - Los tests de `HttpOllamaApi` abren servidores HTTP locales; dentro de un sandbox que prohíbe sockets fallan con `Operation not permitted`, pero fuera de esa restricción pasan.
- **Un módulo con sus dependencias:**
  ```
  mvn -o test -pl audit-cli -am -Dexec.skip=true -Dsurefire.failIfNoSpecifiedTests=false
  ```
- **Una clase o un método:** `-Dtest=BatchCmdTest` o `-Dtest='BatchCmdTest#nombreDelMetodo'`.
- **Por regla de negocio:** muchos tests conservan `@Tag("F-XXXX-R00N")`, así que se filtran con `-Dgroups=F-CJUEZ-R004`. La etiqueta ya no depende de Sentinel.
- **No ejecutar Sentinel.** Sus comandos de generación y verificación fueron retirados del ciclo Maven porque reescribían fuentes, POMs y metadatos versionados.
- **Launcher:** `audit-cli.sh` usa los `target/classes` del reactor y `audit-cli/target/runtime-classpath.txt`. Si cambian los POM, `scripts/refresh-audit-cli-classpath.sh` lo recompone con `mvn package`, nunca con `mvn install`.
- **Evals de un juez, sin modelo y gratis:** `python3 .sentinel/agents/<juez>/eval/run_evals.py`. Revisa estructura, fidelidad y el esquema del veredicto.
  - Con `--command "<cli>"` califica un CLI de verdad, **y eso paga**.
- **Examen de un juez,** en una copia y nunca en el repo:
  1. `git archive <commit>` a una carpeta nueva de corridas.
  2. Symlink `ittm` al lado.
  3. Compilar con `mvn -o -q -Dexec.skip=true -DskipTests compile` y usar el launcher reproducible del reactor. Maven ya no ejecuta `sentinel-verify`.
  4. `exam run <analizador> --cases <archivo> --course <foto>` **sin `--budget`** da la versión, gratis. **Con `--budget N --concurrency 4`** rinde, y paga.
  5. Medir con el `medir*.py` de la carpeta del examen. `exam record` registra y `exam status` muestra el estado; `exam status -f json` da el historial de todos los jueces en JSON, sin consultar (F-JEXAM-R015, lo lee la pantalla Exámenes del tablero).
  6. Las corridas largas se lanzan desenganchadas, con guardia: tope de rechazos en un 25 % de los casos y 3 fallas seguidas. Antes, un ensayo de 10 casos.
- **Probar la CLI sin gastar:** en las corridas históricas, `control.sh` en `MODO=sin` bloquea Claude. Para Codex hay que crear una copia nueva y adaptar primero ese control; no asumir que el fake viejo intercepta `codex`.

---

## 9. Costos históricos de Claude, medidos el 3/10

- **Tramo de 802 consultas** de la tanda (consigna 324, una sola respuesta 339, el que propone 102, opción múltiple 20 y miniteoría 17):
  - **0,37 puntos del cupo semanal cada 100 consultas**, una cota alta;
  - 2,4 puntos cada 100 en la ventana de 5 horas, así que esa ventana aguanta unas 4.200 consultas;
  - **7,6 consultas por minuto** con 4 a la vez.
- **Lo que falta de A1:** unas 8.900 consultas, unos 33 puntos.
  - unas 4.100 de la tanda;
  - unas 3.500 a 4.000 del cierre;
  - unas 1.105 del examen de duplicados.
- **A2 a B2:** unas 50.000 consultas, alrededor de 1,9 semanas de cupo entero.
- **No usar estas cifras para presupuestar Codex.** Medir el primer ensayo OpenAI por separado, con un único proceso corriendo y el panel de uso de la cuenta.

---

## 10. Trampas conocidas

- **Procesos:** las herramientas de las sesiones matan los procesos de fondo a los 30 minutos. Lanzar con `desenganchar.py` o con `nohup` y `setsid`.
- **`/tmp`:** se borra al reiniciar la Mac. Lo que tiene que durar va a `content-audit-corridas/` o al tablero.
- **zsh:**
  - `echo ======` falla, porque `=` se expande. Usar comillas.
  - `grep` es ugrep y tiene límite de complejidad con multibyte. Usar `/usr/bin/grep`.
- **Legado de Sentinel:** las anotaciones `@Generated`, `sentinel.yaml`, `.sentinel/` y los baselines todavía existen como historia. No son fuentes activas ni deben regenerar el proyecto.
- **La app:** la nueva y la vieja comparten bundle id. «Antes y después» llama a `compare`, que tarda horas si hay un piso roto.
- **Los gates:** cambiar un `gate_verdict.sh` cambia la versión del juez.
- **Exámenes:** `rendir-v2.sh` casi pisa veredictos registrados; los registrados quedaron en sólo lectura. Usar un `TAG` por oro.
- **El primer `batch run` del 3/10 no consultó nada,** porque faltaba `CONTENT_AUDIT_LAGEN_MODEL`. Ver `control.sh`.

---

## 11. Referencias

- **Briefs en ittm** (`docs/briefs/`, catalogados en `docs/index.html`):
  - 077: la revisión;
  - 085 y 089: el método y la división del trabajo;
  - 104 a 111: las rondas y los exámenes;
  - **112:** dónde estamos;
  - **113:** arreglar duplicados;
  - **114:** la mesa mide;
  - **115:** lo que midió el tramo;
  - **116:** publicar lo hecho;
  - **117:** lo que falta.
- **El tablero:** `docs/propuestas/tablero-corregir-sin-empeorar/`.
  - `decisiones-ronda1.md`: todas las decisiones de José.
  - `examen-*.md`: los exámenes.
  - `recalibrar-*.md`: las recalibraciones.
  - `cierre-con-jueces.md`, `duplicados-por-par.md`, `traduccion-a1.md`, `pistas-que-regalan.md`.
  - `resumenes-de-sesiones/`.
- **content-audit:** `AGENTS.md`, `CLAUDE.md` y `requirements/`; los requisitos siguen vigentes y los parches de Sentinel quedan como historia.
