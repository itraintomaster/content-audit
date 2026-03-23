---
feature:
  id: FEAT-CLI
  code: F-CLI
  name: Punto de Entrada CLI para Ejecucion de Auditoria
  priority: critical
---

# Punto de Entrada CLI para Ejecucion de Auditoria

Proveer un punto de entrada por linea de comandos (CLI) que permita ejecutar la auditoria completa de un curso, ejercitando toda la cadena: carga del curso desde disco, transformacion a modelo auditable, ejecucion del motor de auditoria con todos los analizadores configurados, y presentacion del resultado en consola.

## Contexto

El sistema ContentAudit cuenta con toda la logica de dominio necesaria para auditar un curso: modelos de curso (course-domain), repositorio de persistencia en disco (course-infrastructure), mapeadores de transformacion y configuracion (audit-application), motor de auditoria, analizadores y agregador de puntuaciones (audit-domain). Sin embargo, no existe actualmente ningun punto de entrada que conecte estas piezas y permita ejecutar una auditoria de extremo a extremo.

### Situacion actual

- **audit-application** orquesta la logica de negocio pero no tiene un metodo publico para disparar la auditoria completa. No expone la operacion de auditoria como un caso de uso invocable.
- **No existe capa de transporte**: no hay CLI, API HTTP, ni ninguna otra interfaz que permita a un usuario o sistema externo solicitar la ejecucion de una auditoria.
- Las piezas individuales (repositorio, mapper, engine, analyzers, aggregator) estan definidas como contratos pero no estan conectadas en un flujo ejecutable.

### Objetivo

Crear un nuevo modulo **audit-cli** que actue como capa de presentacion/transporte del sistema. Este modulo sera una aplicacion ejecutable por linea de comandos que:

1. Reciba como argumento la ruta al directorio del curso en disco
2. Orqueste la ejecucion completa de la auditoria delegando a audit-application
3. Presente el resultado del AuditReport en consola de forma legible

### Flujo de ejecucion esperado

El CLI debe ejercitar toda la cadena de componentes existentes en este orden:

```
CLI (nuevo)
 └─> audit-application: ContentAudit.audit()
      ├─> course-infrastructure: CourseRepository.load(path) → CourseEntity
      ├─> audit-application: CourseToAuditableMapper.map(CourseEntity) → AuditableCourse
      │    └─> NlpTokenizer (tokenizacion de oraciones)
      └─> audit-domain: AuditEngine.runAudit(AuditableCourse)
           ├─> ContentAnalyzer[] (todos los analizadores configurados)
           │    ├─> SentenceLengthAnalyzer
           │    ├─> KnowledgeTitleLengthAnalyzer
           │    └─> KnowledgeInstructionsLengthAnalyzer
           └─> ScoreAggregator.aggregate(List<ScoredItem>) → AuditReport
```

El resultado final es un `AuditReport` que contiene la puntuacion general del curso y el arbol jerarquico de puntuaciones por nivel, topic, knowledge y quiz.

### Separacion de responsabilidades

- **audit-cli** (nuevo): solo se encarga de parsear argumentos de entrada, invocar la auditoria, y formatear la salida. No contiene logica de negocio.
- **audit-application**: orquesta la logica de negocio. Debe exponer un metodo publico que el CLI pueda invocar, recibiendo la ruta del curso y devolviendo el AuditReport.
- **audit-domain**: contiene toda la logica de auditoria (engine, analyzers, aggregator).
- **course-infrastructure**: carga el curso desde disco.
- **course-domain**: modelos de dominio del curso.

### Sobre la capa de aplicacion

Actualmente `ContentAudit` define `AuditReport audit(AuditableCourse)`, pero el CLI no deberia preocuparse de cargar el curso ni mapearlo. La capa de aplicacion debe exponer una operacion de mas alto nivel que acepte la ruta del curso y devuelva el reporte, encapsulando internamente la carga, el mapeo y la ejecucion de la auditoria.

---

## Reglas de Negocio

### Rule[F-CLI-R001] - Ejecucion de auditoria de extremo a extremo
**Severity**: critical | **Validation**: AUTO_VALIDATED

El CLI debe ejecutar la auditoria completa de un curso proporcionando la ruta al directorio del curso como argumento. La ejecucion debe atravesar toda la cadena de componentes: carga del curso desde disco (CourseRepository), transformacion al modelo auditable (CourseToAuditableMapper con NlpTokenizer), ejecucion del motor de auditoria (AuditEngine con todos los ContentAnalyzer configurados), y agregacion de puntuaciones (ScoreAggregator). El resultado es un AuditReport completo.

**Error**: "No se pudo ejecutar la auditoria para el curso en la ruta {path}: {detalle}"

### Rule[F-CLI-R002] - Argumento obligatorio: ruta del curso
**Severity**: critical | **Validation**: AUTO_VALIDATED

El CLI debe recibir como argumento obligatorio la ruta al directorio raiz del curso en disco. Si no se proporciona el argumento o la ruta no existe, el CLI debe informar el error y terminar con un codigo de salida distinto de cero.

**Error**: "Uso: audit-cli <ruta-al-directorio-del-curso>"

### Rule[F-CLI-R003] - Presentacion del resultado en consola
**Severity**: major | **Validation**: AUTO_VALIDATED

Al finalizar la auditoria exitosamente, el CLI debe presentar el AuditReport en consola. El nivel de detalle por defecto es un resumen ejecutivo que incluye:
- La puntuacion general del curso (overall score)
- La puntuacion de cada nivel (milestone) con su identificador
- Un resumen que permita identificar rapidamente si hay niveles con problemas

El CLI soporta dos formatos de salida seleccionables mediante el argumento `--format`:
- **text** (por defecto): texto plano formateado para lectura humana en terminal
- **json**: JSON estructurado para facilitar integracion con pipelines de CI/CD u otras herramientas

Si no se especifica `--format`, el CLI usa formato texto.

**Error**: N/A (esta regla describe formato de salida)

### Rule[F-CLI-R004] - Codigo de salida
**Severity**: major | **Validation**: AUTO_VALIDATED

El CLI debe retornar codigo de salida 0 cuando la auditoria se ejecuta exitosamente (independientemente de las puntuaciones obtenidas). Debe retornar un codigo de salida distinto de cero cuando ocurre un error que impide completar la auditoria (ruta invalida, error de carga, error en el motor de auditoria).

**Error**: N/A (esta regla describe el comportamiento del proceso)

### Rule[F-CLI-R005] - Metodo publico en la capa de aplicacion
**Severity**: critical | **Validation**: AUTO_VALIDATED

La capa de aplicacion (audit-application) debe exponer un metodo publico que permita ejecutar la auditoria completa a partir de una ruta de directorio de curso. Este metodo encapsula toda la orquestacion interna: carga del curso via CourseRepository, transformacion via CourseToAuditableMapper, y ejecucion via ContentAudit. El CLI solo necesita invocar este metodo unico.

La firma esperada es: `AuditReport runAudit(Path coursePath)`

**Error**: "Error al ejecutar la auditoria del curso: {detalle}"

### Rule[F-CLI-R006] - Ensamblaje de dependencias
**Severity**: major | **Validation**: AUTO_VALIDATED

El CLI es responsable de ensamblar (construir e inyectar) todas las dependencias necesarias para ejecutar la auditoria. Esto incluye instanciar el repositorio de cursos, el tokenizador, la configuracion de analizadores, los analizadores, el motor de auditoria, el agregador, y el servicio de aplicacion. El ensamblaje se realiza al inicio de la ejecucion, antes de invocar la auditoria.

Dado que el sistema no usa un framework de inyeccion de dependencias en el CLI (no es una aplicacion Spring Boot), el ensamblaje se realiza manualmente mediante constructor injection en el metodo main.

**Error**: "Error al inicializar el sistema de auditoria: {detalle}"

---

## User Journeys

### Journey[F-CLI-J001] - Ejecutar auditoria de un curso desde la terminal
**Validation**: AUTO_VALIDATED

1. El usuario abre una terminal y navega al directorio del proyecto
2. El usuario ejecuta el CLI proporcionando la ruta al curso: `java -jar audit-cli.jar /path/to/english-course`
3. El sistema carga el curso desde el directorio especificado
4. El sistema transforma el curso al modelo auditable, tokenizando las oraciones
5. El sistema ejecuta todos los analizadores configurados sobre el curso auditable
6. El sistema agrega las puntuaciones producidas por los analizadores a traves de la jerarquia del curso
7. El CLI presenta en consola la puntuacion general y las puntuaciones por nivel
8. El CLI termina con codigo de salida 0

### Journey[F-CLI-J002] - Error por ruta invalida
**Validation**: AUTO_VALIDATED

1. El usuario ejecuta el CLI sin argumentos o con una ruta que no existe
2. El CLI muestra un mensaje de error indicando el uso correcto y la causa del error
3. El CLI termina con codigo de salida distinto de cero

### Journey[F-CLI-J003] - Error durante la auditoria
**Validation**: AUTO_VALIDATED

1. El usuario ejecuta el CLI con una ruta valida pero el contenido del curso tiene problemas (por ejemplo, archivos JSON malformados)
2. El sistema detecta el error durante la carga o el procesamiento
3. El CLI muestra un mensaje de error descriptivo indicando que fallo y donde
4. El CLI termina con codigo de salida distinto de cero

---

## Open Questions

### Doubt[DOUBT-OUTPUT-FORMAT] - El formato de salida debe ser texto plano o estructurado?
**Status**: RESOLVED

El CLI necesita presentar los resultados en consola. Hay diferentes opciones de formato que afectan la usabilidad y la posibilidad de integrar con otras herramientas.

**Pregunta**: Que formato de salida debe usar el CLI?

- [ ] Opcion A: Texto plano formateado para lectura humana (tablas, indentacion, colores opcionales)
- [ ] Opcion B: JSON estructurado para facilitar integracion con otras herramientas
- [x] Opcion C: Ambos, seleccionable por argumento (--format text|json)

**Answer**: Se soportan ambos formatos seleccionables por argumento `--format text|json`. Texto plano es el formato por defecto. Esto permite tanto lectura humana directa como integracion con herramientas externas.

### Doubt[DOUBT-VERBOSITY] - Que nivel de detalle debe mostrar la salida por defecto?
**Status**: RESOLVED

El AuditReport contiene un arbol completo de puntuaciones que puede ser muy extenso (~608 knowledges, ~11.500 quizzes). Mostrar todo el arbol en consola no seria practico.

**Pregunta**: Que nivel de detalle debe mostrar el CLI por defecto?

- [x] Opcion A: Solo puntuacion general y puntuaciones por nivel (resumen ejecutivo)
- [ ] Opcion B: Hasta nivel de topic
- [ ] Opcion C: Arbol completo con un flag --verbose para controlar la profundidad

**Answer**: Resumen ejecutivo por defecto (puntuacion general + niveles). Suficiente para la primera version; el detalle granular puede agregarse en futuras iteraciones con flags opcionales.
