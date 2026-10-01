# Features & Business Rules

Features define the business requirements that the system must satisfy. Each feature contains
business rules (constraints) and user journeys (workflows). Tests can link back to features
via **traceability**, enabling requirements-to-test coverage tracking.

## Feature Hierarchy

```
Feature (id, name, description, code)
├── BusinessRule (id, name, severity, description, errorMessage)
└── UserJourney (id, name, steps[])
```

## Traceability

Tests can reference features, rules, and journeys via the `traceability` block:

```yaml
tests:
  - name: "Email must be valid"
    traceability:
      feature: "FEAT-001"
      rule: "RULE-001"
      journey: "JOURNEY-001"
```

This generates `@Tag("FEAT-001")` and `@Tag("RULE-001")` annotations on the test method,
enabling JUnit 5 tag-based filtering (e.g., run only tests for `FEAT-001`).

## Severity Levels

| Severity | Meaning |
|----------|---------|
| `critical` | System cannot function without this rule |
| `high` | Major functionality affected |
| `medium` | Important but not blocking |
| `low` | Nice-to-have validation |

## Declared Features

### FEAT-DLABS: Diagnosticos Tipados para el Analizador de Ausencia de Lemas [F-DLABS]

> Reemplazar los resultados no tipados (`Map<String, Object> metadata`) que el analizador de ausencia de lemas (FEAT-LABS) emite en cada nodo del arbol de auditoria por **registros de diagnostico tipados** que describan de forma explicita y navegable la informacion producida en cada nivel de la jerarquia del curso. Este cambio es el primer paso de una iniciativa mas amplia para dotar a todos los analizadores del sistema ContentAudit de diagnosticos tipados, y se aplica inicialmente al analizador lemma-absence por ser el que emite la estructura de datos mas compleja y el que mas se beneficia de la tipificacion.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-DLABS-R001 | Interfaz sellada AnalyzerDiagnosis | critical | - |
| F-DLABS-R002 | Mapa de diagnosticos en el nodo de auditoria | critical | No se encontro diagnostico para el analizador '{nombre}' en el nodo {nodoId} |
| F-DLABS-R003 | Acceso tipado a diagnosticos | critical | El diagnostico del analizador '{nombre}' es de tipo {tipoReal}, pero se solicito {tipoEsperado} |
| F-DLABS-R004 | Registro de diagnostico a nivel curso: LemmaAbsenceCourseDiagnosis | critical | - |
| F-DLABS-R005 | Registro de diagnostico a nivel milestone: LemmaAbsenceLevelDiagnosis | critical | - |
| F-DLABS-R006 | Registro auxiliar AbsentLemma | critical | - |
| F-DLABS-R007 | Registro de diagnostico a nivel topic: LemmaPlacementDiagnosis | critical | - |
| F-DLABS-R008 | Reutilizacion de LemmaPlacementDiagnosis en knowledge | critical | - |
| F-DLABS-R009 | Reutilizacion de LemmaPlacementDiagnosis en quiz | critical | - |
| F-DLABS-R010 | Registro auxiliar MisplacedLemma | critical | - |
| F-DLABS-R011 | Navegacion hacia nodos ancestros | critical | No se encontro ancestro de tipo '{nivel}' para el nodo {nodoId} |
| F-DLABS-R012 | Combinacion de navegacion y acceso tipado | major | - |
| F-DLABS-R013 | Migracion del formateador de detalle de lemma-absence | major | - |
| F-DLABS-R014 | Eliminacion del mapa generico para lemma-absence | major | - |

**User Journeys:**

- **F-DLABS-J001**: Consultar diagnosticos tipados del analizador lemma-absence

- **F-DLABS-J002**: Navegar desde un quiz hacia el diagnostico de su milestone ancestro

- **F-DLABS-J003**: Formatear informe de ausencia de lemas usando diagnosticos tipados

### FEAT-LREC: Analisis de Recurrencia de Lemas por Repeticion Espaciada [F-LREC]

> Evaluar la **distribucion espacial de los lemas** a lo largo del curso para determinar si las palabras de contenido se repiten a intervalos regulares, aplicando el principio pedagogico de repeticion espaciada (spaced repetition). Un lema que aparece concentrado en una seccion del curso y luego desaparece es menos efectivo para la retencion que uno que se distribuye uniformemente. El analizador produce un resultado global a nivel de curso (no por oracion ni por nivel), con estadisticas individuales por lema y una puntuacion general que refleja la proporcion de lemas con recurrencia adecuada.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-LREC-R001 | Asignacion de posicion global a cada palabra | critical | Error al construir las posiciones globales: el curso no contiene oraciones procesables |
| F-LREC-R002 | Orden de procesamiento determinista | critical | - |
| F-LREC-R003 | Filtrado de palabras de contenido | critical | - |
| F-LREC-R004 | Registro de posiciones por lema | critical | - |
| F-LREC-R005 | Seleccion de los lemas mas frecuentes (top N) | critical | No se encontraron lemas de contenido en el curso. El analisis de recurrencia no puede ejecutarse. |
| F-LREC-R006 | Exclusion de lemas con menos de 2 apariciones | major | - |
| F-LREC-R007 | Calculo de intervalo medio y desviacion estandar | critical | Error al calcular intervalos para el lema '{lema}': la lista de posiciones esta vacia o tiene un solo elemento |
| F-LREC-R008 | Clasificacion de exposicion de cada lema | critical | Configuracion de umbrales invalida: overExposed ({overExposed}) debe ser menor que subExposed ({subExposed}) |
| F-LREC-R009 | Resumen de exposicion | major | Inconsistencia en el resumen de exposicion: normalCount ({normal}) + subExposedCount ({sub}) + overExposedCount ({over}) != totalCount ({total}) |
| F-LREC-R010 | Puntuacion general del analisis (overall score) | critical | Error al calcular la puntuacion general: totalCount es cero, no hay lemas analizados |
| F-LREC-R011 | La desviacion estandar es informativa, no participa en el scoring | major | - |
| F-LREC-R012 | Estructura del resultado por lema | major | - |
| F-LREC-R013 | Estructura del resultado global | major | - |
| F-LREC-R014 | Parametros configurables del analisis | major | Configuracion de recurrencia de lemas invalida: {detalle de la violacion} |
| F-LREC-R015 | Nombre del analizador en el informe | major | - |

**User Journeys:**

- **F-LREC-J001**: Auditar la recurrencia de vocabulario de un curso completo

- **F-LREC-J004**: Detectar lemas con distribucion irregular usando la desviacion estandar

### FEAT-DCOCA: Diagnosticos Tipados para el Analizador de Distribucion COCA [F-DCOCA]

> Reemplazar los resultados no tipados (`Map<String, Object> metadata`) que el analizador de distribucion COCA (FEAT-COCA) emite en cada nodo del arbol de auditoria por **registros de diagnostico tipados** que describan de forma explicita la informacion producida en cada nivel de la jerarquia del curso. Este es el segundo paso de la iniciativa de diagnosticos tipados iniciada por FEAT-DLABS, y aplica el mismo patron al analizador coca-buckets-distribution.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-DCOCA-R001 | Registro de diagnostico a nivel curso: CocaProgressionDiagnosis | critical | - |
| F-DCOCA-R002 | Registro de diagnostico a nivel milestone: CocaBucketsLevelDiagnosis | critical | - |
| F-DCOCA-R003 | Registro de diagnostico a nivel topic: CocaBucketsTopicDiagnosis | critical | - |
| F-DCOCA-R004 | Ausencia de diagnostico en niveles knowledge y quiz | major | - |
| F-DCOCA-R005 | Nuevos metodos en las interfaces de diagnostico por nivel | critical | - |
| F-DCOCA-R006 | Migracion del formateador de detalle de coca-buckets | major | - |
| F-DCOCA-R007 | Eliminacion del mapa generico para coca-buckets | major | - |

**User Journeys:**

- **F-DCOCA-J001**: Formatear informe de distribucion COCA usando diagnosticos tipados

- **F-DCOCA-J002**: Consultar diagnosticos COCA desde el futuro refiner

### FEAT-COURSE: Modelo de Dominio y Persistencia de Estructura de Curso [F-COURSE]

> Representar la estructura jerarquica completa de un curso de idiomas (Course, Milestone, Topic, Knowledge, Quiz Template) como modelo en memoria, con capacidad de lectura y escritura a archivos JSON en formato MongoDB Extended JSON, garantizando idempotencia semantica en el ciclo lectura-escritura.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-COURSE-R001 | Jerarquia estricta de 5 niveles | critical | Estructura jerarquica invalida: se esperaban exactamente 5 niveles (Course > ROOT > Milestone > Topic > Knowledge > Quiz Template) |
| F-COURSE-R002 | Orden significativo de hijos | critical | El orden de los elementos hijos fue alterado en {entidad} ({id}) |
| F-COURSE-R003 | Idempotencia semantica lectura-escritura | critical | La operacion de lectura-escritura modifico el contenido del archivo {nombre}: diferencia semantica encontrada en {detalle} |
| F-COURSE-R004 | Cada knowledge debe tener quiz templates | major | El knowledge '{label}' ({id}) no tiene quiz templates asociados |
| F-COURSE-R005 | Consistencia de IDs entre niveles | critical | Inconsistencia de IDs en {entidad} ({id}): la lista {campo} referencia IDs que no existen en la estructura: {ids_faltantes} |
| F-COURSE-R006 | Identificadores unicos | critical | Identificador duplicado: el ID {id} aparece en {entidad_1} y en {entidad_2} |
| F-COURSE-R007 | Correspondencia directorio-entidad | major | El directorio '{ruta}' no contiene el archivo descriptor esperado '{archivo}' |
| F-COURSE-R008 | Integridad referencial parent-child | critical | Integridad referencial rota: {entidad} ({id}) referencia al padre {parentId}, pero el padre esperado es {padre_real} |
| F-COURSE-R009 | Campos obligatorios por entidad | critical | Campo obligatorio ausente: {entidad} ({id}) no tiene el campo '{campo}' |
| F-COURSE-R010 | Preservacion de campos vacios y valores por defecto | major | El campo '{campo}' en {entidad} ({id}) fue omitido o alterado durante la escritura; valor original: {valor_original}, valor escrito: {valor_escrito} |
| F-COURSE-R011 | Doble ID en Quiz Templates | major | Quiz template con IDs inconsistentes: _id={oid_value}, id={id_value} |
| F-COURSE-R012 | Formato numerico MongoDB Extended JSON | major | Formato numerico incorrecto en {entidad} ({id}), campo '{campo}': se esperaba formato $numberDouble |
| F-COURSE-R013 | Orden jerarquico explicito | critical | Orden no asignado o duplicado: {entidad} ({id}) tiene order={order}, pero ya existe otra entidad con el mismo orden en {padre} |
| F-COURSE-R014 | Comportamiento ante datos inconsistentes durante la carga | critical | Error al cargar el curso desde '{ruta}': {descripcion_de_la_inconsistencia}. La carga fue abortada. |
| F-COURSE-R015 | Cada milestone debe tener al menos un topic | major | Estructura incompleta: {entidad} '{label}' ({id}) no tiene hijos |
| F-COURSE-R016 | Generacion determinista de slugs desde el label | major | No se pudo generar un slug valido para la entidad '{label}' ({id}) |

**User Journeys:**

- **F-COURSE-J001**: Cargar un curso completo desde archivos
  1. El usuario indica la ruta al directorio raiz del curso (ej: `db/english-course/`)
  2. El sistema verifica que el directorio existe y contiene `_course.json`
  3. El sistema lee `_course.json` del directorio raiz
  4. El sistema descubre los subdirectorios de milestones y lee cada `_milestone.json`
  5. Para cada milestone, el sistema descubre los subdirectorios de topics y lee cada `_topic.json`
  6. Para cada topic, el sistema descubre los subdirectorios de knowledges y lee cada `_knowledge.json` y `quizzes.json`
  7. El sistema construye la jerarquia completa en memoria con todas las relaciones padre-hijo resueltas, incluyendo el nodo ROOT
  8. El sistema calcula el campo `order` para cada milestone, topic y knowledge segun su posicion en la lista de hijos del padre (R013)
  9. El sistema valida la consistencia de IDs entre niveles (R005, R008) y la integridad de campos obligatorios (R009)
  10. Si alguna validacion falla, la carga se aborta completamente con un mensaje descriptivo (R014)
  11. El curso completo queda disponible para navegacion y consulta en memoria

- **F-COURSE-J002**: Escribir un curso completo a archivos
  1. El usuario tiene un curso cargado en memoria (posiblemente modificado)
  2. El usuario indica la ruta de destino donde escribir la estructura
  3. Si el directorio destino ya contiene una estructura de curso, el sistema la sobreescribe completamente: los archivos existentes se reemplazan por los nuevos
  4. El sistema crea la estructura de directorios necesaria (si no existe)
  5. El sistema escribe `_course.json` en el directorio raiz
  6. Para cada milestone, el sistema crea su directorio (usando el slug derivado del label o preservando el nombre original si se leyo desde disco) y escribe `_milestone.json`
  7. Para cada topic, el sistema crea su directorio y escribe `_topic.json`
  8. Para cada knowledge, el sistema crea su directorio, escribe `_knowledge.json` y `quizzes.json`
  9. Los archivos resultantes preservan el formato MongoDB Extended JSON y la equivalencia semantica con los datos en memoria

- **F-COURSE-J003**: Verificar idempotencia lectura-escritura
  1. El usuario carga un curso desde un directorio origen
  2. El usuario escribe el curso a un directorio destino (puede ser el mismo u otro)
  3. El usuario compara los archivos de origen y destino
  4. Todos los archivos JSON deben ser semanticamente equivalentes (mismos campos, mismos valores, mismo formato de tipos MongoDB). El orden de campos (keys) dentro de un objeto puede variar
  5. La estructura de directorios debe ser identica (mismos nombres, misma jerarquia)

- **F-COURSE-J004**: Navegar la estructura del curso en memoria
  1. El usuario carga el curso
  2. El usuario puede posicionarse en el nodo ROOT y obtener una vista global del curso (cantidad de milestones, topics, knowledges)
  3. El usuario puede obtener la lista de milestones del curso, ordenados segun su campo `order`
  4. Desde un milestone, el usuario puede obtener sus topics, ordenados segun su campo `order`
  5. Desde un topic, el usuario puede obtener sus knowledges, ordenados segun su campo `order`
  6. Desde un knowledge, el usuario puede obtener sus quiz templates
  7. El usuario puede navegar hacia arriba (de knowledge a topic, de topic a milestone, de milestone a ROOT)
  8. El usuario puede acceder a cualquier entidad por su ID directamente, sin importar su tipo o nivel en la jerarquia. El sistema debe ofrecer un mecanismo de busqueda por ID que recorra todos los niveles

- **F-COURSE-J005**: Modificar datos de contenido y persistir cambios
  1. El usuario carga el curso desde archivos
  2. El usuario modifica datos de contenido del curso en memoria (ej: cambiar el label de un knowledge, modificar la traduccion de un quiz template)
  3. El usuario escribe el curso modificado a archivos
  4. Al releer los archivos, los cambios se reflejan correctamente
  5. Los datos no modificados permanecen intactos (sin efectos colaterales)

- **F-COURSE-J006**: Manejo de errores durante la carga
  1. El usuario indica una ruta a un directorio que no existe, o que no contiene `_course.json`
  2. El sistema informa que la ruta no es un directorio de curso valido
  3. Alternativamente, el usuario indica una ruta valida pero con datos inconsistentes (ej: un milestone referencia un topic que no existe)
  4. El sistema informa exactamente cual regla se violo y en que entidad, y aborta la carga
  5. El usuario no obtiene un curso parcial o corrupto en ningun caso

### FEAT-COCA: Analisis de Distribucion de Vocabulario por Frecuencia COCA [F-COCA]

> Evaluar si la distribucion de vocabulario a lo largo del curso es apropiada para cada nivel de dificultad esperado (CEFR), clasificando los tokens del curso en bandas de frecuencia basadas en el corpus COCA (Corpus of Contemporary American English) y comparando la distribucion real contra objetivos pedagogicos configurables. Los conteos de tokens se acumulan a traves de la jerarquia del curso (knowledge, topic, nivel, curso) mediante una estrategia de agregacion propia de esta funcionalidad, ya que la naturaleza de los datos (conteos de tokens por banda, no scores individuales) requiere acumulacion en lugar de promediado. Adicionalmente, se evalua la progresion de la distribucion de vocabulario entre niveles y se generan directivas de mejora para el creador de contenido.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-COCA-R001 | Clasificacion de tokens en bandas de frecuencia | critical | Token con frequencyRank invalido ({rank}) en el quiz {quizId} del knowledge {knowledgeId} |
| F-COCA-R002 | Banda abierta (top4k) | major | - |
| F-COCA-R003 | Configuracion de bandas de frecuencia | major | Configuracion de bandas invalida: los valores deben ser positivos y estar en orden ascendente |
| F-COCA-R004 | Tokens sin ranking de frecuencia | major | - |
| F-COCA-R005 | Calculo de distribucion porcentual | critical | Error al calcular la distribucion porcentual para el nivel {nivel}: total de tokens clasificados es cero |
| F-COCA-R006 | Los datos de frecuencia provienen de procesamiento linguistico previo | critical | Token sin datos de frecuencia en el quiz {quizId}: el procesamiento linguistico previo no asocio frequencyRank |
| F-COCA-R007 | Evaluacion de estado por banda (assessment) | critical | Estado indeterminado para la banda {banda} en el nivel {nivel}: porcentaje real {real}%, objetivo {target}% |
| F-COCA-R008 | Semantica de la direccionalidad (atLeast / atMost) | critical | Kind desconocido '{kind}' para la banda {banda} en el nivel {nivel}: debe ser 'atLeast' o 'atMost' |
| F-COCA-R009 | Tolerancias optimalRange y adequateRange | major | Configuracion de tolerancias invalida: optimalRange ({optimal}) debe ser menor que adequateRange ({adequate}) y ambos deben ser positivos |
| F-COCA-R010 | Puntuacion por banda individual (bucket score) | critical | Puntuacion fuera de rango [0.0, 1.0] calculada para la banda {banda} en el nivel {nivel}: {puntuacion} |
| F-COCA-R011 | Puntuacion por trimestre (quarter score) | critical | Error al calcular la puntuacion del trimestre Q{n} del nivel {nivel}: ninguna banda tiene objetivo definido |
| F-COCA-R012 | Puntuacion por nivel | critical | Error al calcular la puntuacion del nivel {nivel}: {detalle} |
| F-COCA-R013 | Puntuacion general del curso (overall score) | critical | Error al calcular la puntuacion general: {detalle} |
| F-COCA-R014 | Dos estrategias de analisis | major | Estrategia de analisis desconocida: '{estrategia}'. Las estrategias validas son: LEVELS, QUARTERS |
| F-COCA-R015 | Interpolacion lineal de trimestres intermedios | critical | Error al interpolar trimestres para el nivel {nivel}: faltan datos de trimestre inicial o final |
| F-COCA-R016 | Herencia de direccionalidad en trimestres interpolados | major | - |
| F-COCA-R017 | Asignacion de contenido a trimestres | major | Error al asignar topics a trimestres para el nivel {nivel}: el nivel no tiene topics |
| F-COCA-R018 | Configuracion de objetivos por trimestre | critical | Configuracion incompleta para el trimestre {trimestre} del nivel {nivel}: falta la banda {banda} |
| F-COCA-R019 | Estrategia levels usa objetivos de nivel directamente | major | Configuracion de objetivos no encontrada para el nivel {nivel} con estrategia LEVELS |
| F-COCA-R020 | Progresion esperada por banda de frecuencia | major | - |
| F-COCA-R021 | Algoritmo de evaluacion de progresion real | critical | No se pudo evaluar la progresion de la banda {banda}: {detalle} |
| F-COCA-R022 | Progresion evalua solo niveles con datos | major | - |
| F-COCA-R023 | Margen de cambio significativo en progresion | minor | - |
| F-COCA-R024 | Resultado de la evaluacion de progresion | major | - |
| F-COCA-R025 | Acumulacion de conteos de tokens por banda | critical | - |
| F-COCA-R026 | Distribucion porcentual por nodo de la jerarquia | critical | Error al calcular la distribucion porcentual del nodo {nodoId}: total de tokens clasificados es cero |
| F-COCA-R027 | Score solo donde existen targets definidos | critical | - |
| F-COCA-R028 | Informacion por banda en cada nodo | major | - |
| F-COCA-R029 | Contrato de agregacion polimorfica | major | - |
| F-COCA-R030 | Generacion de directivas de mejora | major | - |
| F-COCA-R031 | Rango de frecuencia en directivas | major | - |
| F-COCA-R032 | Directivas se generan solo para bandas fuera de rango | major | - |

**User Journeys:**

- **F-COCA-J001**: Auditar la distribucion de vocabulario de un curso completo
  1. El usuario inicia una auditoria de un curso previamente cargado en el sistema ContentAudit
  2. El sistema recorre la jerarquia del curso: para cada nivel (milestone), sus topics, sus knowledges, y los quizzes de cada knowledge
  3. Para cada quiz, el analizador obtiene los tokens con su `frequencyRank` (proveniente del procesamiento linguistico previo)
  4. El analizador clasifica cada token en su banda de frecuencia correspondiente (top1k, top2k, top3k, top4k) segun R001
  5. Para cada nivel (o trimestre si se usa la estrategia quarters), el analizador calcula la distribucion porcentual de tokens por banda (R005)
  6. El analizador compara la distribucion real contra los objetivos configurados para cada banda y genera puntuaciones (R007, R010)
  7. Si se usa la estrategia quarters, los objetivos de los trimestres intermedios se calculan por interpolacion lineal (R015)
  8. Los conteos de tokens por banda se acumulan a traves de la jerarquia del curso (quiz -> knowledge -> topic -> nivel), y en cada nodo se calcula la distribucion porcentual. Los scores se generan solo a nivel de nivel/quarter donde existen targets definidos (R025-R029)
  9. El sistema evalua la progresion por banda de frecuencia entre niveles (R021)
  10. El sistema genera directivas de mejora para los niveles/trimestres que no alcanzan sus objetivos (R030)
  11. El usuario recibe un informe con la puntuacion general, la puntuacion y distribucion por nivel, el estado de progresion por banda, y las directivas de mejora

- **F-COCA-J002**: Consultar la distribucion de vocabulario de un nivel especifico
  1. El usuario ha ejecutado la auditoria de distribucion COCA (J001)
  2. El usuario selecciona un nivel CEFR especifico (por ejemplo, A2)
  3. El sistema muestra para el nivel seleccionado:
  4. El sistema muestra la lista de topics del nivel con su distribucion de bandas, permitiendo identificar que topics tienen distribucion problematica
  5. El usuario puede profundizar en topics y knowledges para localizar donde se concentran las palabras de frecuencia inadecuada
  6. Si hay directivas de mejora para este nivel, el sistema las muestra indicando que bandas necesitan mas o menos palabras

- **F-COCA-J003**: Evaluar la progresion de vocabulario entre niveles
  1. El usuario ha ejecutado la auditoria de distribucion COCA (J001)
  2. El usuario consulta el estado de progresion por banda de frecuencia
  3. Para la banda top1k, el usuario verifica si la progresion es DESCENDENTE (lo esperado): que el porcentaje de palabras de alta frecuencia disminuya al avanzar de A1 a B2
  4. Para la banda top4k, el usuario verifica si la progresion es ASCENDENTE (lo esperado): que el porcentaje de palabras de baja frecuencia aumente al avanzar de A1 a B2
  5. Si la progresion de alguna banda es IRREGULAR o ESTATICA, el usuario identifica en que transicion entre niveles se rompe el patron esperado
  6. El usuario revisa los porcentajes reales por nivel para localizar donde el vocabulario no evoluciona como se espera pedagogicamente
  7. El usuario puede tomar acciones correctivas sobre el contenido de los niveles afectados, guiado por las directivas de mejora (J004)

- **F-COCA-J004**: Usar las directivas de mejora para corregir el contenido
  1. El usuario ha ejecutado la auditoria y encontrado que un nivel/trimestre tiene bandas con estado DEFICIENTE o EXCESIVO
  2. El usuario consulta las directivas de mejora generadas para ese nivel/trimestre
  3. Para cada directiva de tipo "enriquecer" (bucketsToEnrich), el usuario identifica que debe agregar mas palabras de la franja de frecuencia indicada. Por ejemplo: "Enriquecer top2k: agregar palabras con ranking 1001-2000"
  4. Para cada directiva de tipo "reducir" (bucketsToReduce), el usuario identifica que debe reemplazar palabras de la franja de frecuencia indicada por palabras de otras franjas. Por ejemplo: "Reducir top4k: reemplazar palabras con ranking 3001+ por palabras mas frecuentes"
  5. El usuario localiza los quizzes afectados navegando la jerarquia (nivel -> topic -> knowledge -> quiz)
  6. El usuario modifica el contenido de los quizzes segun las directivas
  7. El usuario re-ejecuta la auditoria para verificar que los cambios mejoraron la distribucion

- **F-COCA-J005**: Navegar la jerarquia para localizar problemas de vocabulario
  1. El usuario ha ejecutado la auditoria de distribucion COCA (J001)
  2. El usuario observa que el nivel B1 tiene una puntuacion baja (por ejemplo, 0.6)
  3. El usuario profundiza en los trimestres de B1 (si se usa la estrategia quarters) y encuentra que Q3 tiene la puntuacion mas baja (0.4)
  4. El usuario profundiza en los topics del Q3 de B1 y encuentra que el topic "Travel Vocabulary" tiene 40% de tokens en top4k (cuando el objetivo es 21.67% atMost)
  5. El usuario profundiza en los knowledges de "Travel Vocabulary" y encuentra que el knowledge "Airport Procedures" tiene una concentracion inusual de vocabulario tecnico de baja frecuencia
  6. El usuario identifica que varias oraciones de "Airport Procedures" usan vocabulario especializado (boarding, customs, luggage carousel) que infla la banda top4k
  7. El usuario decide si simplificar el vocabulario o aceptar la desviacion como apropiada para el contexto tematico

### FEAT-NLP: Evolucion del Tokenizador NLP para Tokenizacion Rica con Datos Linguisticos [F-NLP]

> Evolucionar la interfaz `NlpTokenizer` del sistema ContentAudit para que produzca **tokens enriquecidos** con informacion linguistica completa (lema, parte de la oracion, ranking de frecuencia, indicadores de stop word y puntuacion), reemplazando la tokenizacion actual basada en division por espacios en blanco. Esta evolucion es un prerequisito critico para que el analizador de distribucion COCA (FEAT-COCA) y futuros analizadores linguisticos puedan funcionar, ya que estos requieren datos de frecuencia y categorias gramaticales que la tokenizacion actual no provee.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-NLP-R001 | Campos obligatorios del token enriquecido | critical | Token incompleto: falta el campo obligatorio '{campo}' para el token '{text}' |
| F-NLP-R002 | Campos opcionales del token enriquecido | minor | - |
| F-NLP-R003 | Lematizacion produce la forma base del token | critical | Error de lematizacion: el token '{text}' no pudo ser lematizado |
| F-NLP-R004 | Etiquetas POS usan el esquema Universal Dependencies | critical | Etiqueta POS desconocida '{posTag}' para el token '{text}'. Se esperaba una etiqueta del esquema Universal Dependencies |
| F-NLP-R005 | El token enriquecido reemplaza a la representacion de cadena de texto | critical | - |
| F-NLP-R006 | Nueva operacion de tokenizacion enriquecida | critical | Error al tokenizar la oracion: '{oracion}' - {detalle del error} |
| F-NLP-R007 | Compatibilidad con analizadores existentes que usan conteo de tokens | major | - |
| F-NLP-R008 | Procesamiento de multiples oraciones en lote | major | Error en procesamiento en lote: {cantidad} de {total} oraciones fallaron durante la tokenizacion |
| F-NLP-R009 | El tokenizador es un servicio del dominio, no un analizador | major | - |
| F-NLP-R010 | El mapeo del curso debe utilizar la tokenizacion enriquecida | critical | Error al mapear el quiz '{quizId}': la tokenizacion enriquecida fallo para la oracion |
| F-NLP-R011 | El ranking de frecuencia se obtiene del lema, no de la forma flexionada | critical | - |
| F-NLP-R012 | Mapeo de etiquetas POS para busqueda de frecuencia | critical | - |
| F-NLP-R013 | Estrategia de busqueda de frecuencia en tres niveles | critical | - |
| F-NLP-R014 | Estructura de los datos de frecuencia COCA | major | Error al cargar datos de frecuencia COCA: {detalle} |
| F-NLP-R015 | Los datos de frecuencia se cargan una sola vez al inicio | major | No se pudieron cargar los datos de frecuencia COCA al inicio |
| F-NLP-R016 | El ranking de frecuencia retornado es el ranking del lema | major | - |
| F-NLP-R017 | SpaCy como motor de procesamiento linguistico | critical | No se pudo iniciar el motor SpaCy: {detalle} |
| F-NLP-R019 | El proceso Python integra lematizacion y busqueda de frecuencia | critical | - |
| F-NLP-R020 | Requisitos del entorno de ejecucion | major | Requisito de entorno faltante: {componente}. Consulte la documentacion de instalacion. |
| F-NLP-R021 | Las rutas del script Python y de los datos COCA se proveen al tokenizador | minor | Ruta del script Python no configurada o no encontrada: {ruta} |
| F-NLP-R023 | Cache en memoria de tokens enriquecidos por oracion | major | - |
| F-NLP-R024 | El cache usa la oracion completa como clave | minor | - |
| F-NLP-R026 | El cache del CachedNlpTokenizer debe evolucionar | major | - |
| F-NLP-R028 | Tokens sin ranking de frecuencia | major | - |
| F-NLP-R029 | Fallo del proceso Python | critical | El procesamiento NLP fallo: {causa}. Verifique que Python 3.11+, SpaCy y el modelo en_core_web_sm estan instalados. |
| F-NLP-R030 | Fallback cuando SpaCy no esta disponible | major | SpaCy no disponible: los analizadores que requieren datos linguisticos enriquecidos no se ejecutaran. Los analizadores basicos (longitud de oracion) funcionan normalmente. |
| F-NLP-R031 | Timeout del proceso Python | major | El procesamiento NLP excedio el tiempo limite de {timeout} segundos. Considere procesar en lotes mas pequenos o aumentar el timeout. |
| F-NLP-R032 | Errores de tokens individuales no detienen el lote completo | major | Error al procesar el token '{text}' en la oracion '{oracion}': {detalle}. Se uso fallback basico. |

**User Journeys:**

- **F-NLP-J001**: Auditar un curso con tokenizacion enriquecida
  1. El usuario inicia una auditoria de un curso en ContentAudit
  2. El sistema comienza el mapeo del curso: recorre todos los niveles, topics, knowledges y quizzes
  3. Para cada quiz, el sistema extrae la oracion del texto del quiz
  4. El sistema agrupa las oraciones y las envia al tokenizador NLP enriquecido en un lote
  5. El tokenizador invoca al proceso Python/SpaCy, que procesa todas las oraciones: tokeniza, lematiza, asigna etiquetas POS, busca frecuencias COCA, y retorna la lista de tokens enriquecidos
  6. El sistema almacena los tokens enriquecidos en el modelo de cada quiz (AuditableQuiz)
  7. Los analizadores procesan los quizzes con sus tokens enriquecidos: el analizador de longitud de oraciones usa el conteo de tokens, el analizador COCA usa el `frequencyRank` y el lema de cada token
  8. El usuario recibe los resultados de la auditoria con las evaluaciones de todos los analizadores

- **F-NLP-J002**: El usuario no tiene SpaCy instalado
  1. El usuario inicia una auditoria sin tener Python o SpaCy instalados en su entorno
  2. El sistema intenta invocar el proceso Python para la tokenizacion enriquecida y detecta que SpaCy no esta disponible
  3. El sistema continua la auditoria en modo de funcionalidad reducida (R030): los analizadores que solo necesitan conteo de tokens completan su evaluacion; los analizadores que requieren datos linguisticos reportan que fueron omitidos
  4. El informe de auditoria final indica al usuario cuales analizadores se ejecutaron y cuales no, con el motivo (SpaCy no disponible)

### FEAT-LABS: Analisis de Ausencia de Lemas por Nivel CEFR [F-LABS]

> Detectar y clasificar los **lemas (formas base de palabras) que deberian estar presentes en un nivel CEFR pero estan ausentes del curso**, utilizando el catalogo EVP (English Vocabulary Profile) como referencia de vocabulario esperado por nivel. El analizador no solo identifica que lemas faltan, sino que clasifica el tipo de ausencia (completamente ausente, aparece demasiado temprano, aparece demasiado tarde, ubicacion dispersa), asigna una prioridad basada en la frecuencia COCA del lema, y genera recomendaciones accionables para que el creador de contenido corrija las brechas de vocabulario. Adicionalmente, evalua el impacto de los lemas mal ubicados en las oraciones del curso mediante un sistema de scoring por oracion.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-LABS-R001 | Obtencion de lemas esperados por nivel | critical | No se pudieron obtener los lemas esperados para el nivel {nivel}: el catalogo EVP no contiene entradas para este nivel |
| F-LABS-R002 | Obtencion de lemas presentes por nivel | critical | No se pudieron obtener los lemas presentes para el nivel {nivel}: no hay oraciones procesadas en este nivel |
| F-LABS-R003 | Calculo de lemas ausentes por nivel | critical | - |
| F-LABS-R004 | Busqueda de lemas ausentes en otros niveles | critical | - |
| F-LABS-R005 | Exclusion de frases multipalabra del EVP | major | - |
| F-LABS-R040 | Las formas derivadas y compuestas no definen el nivel del lema base | critical | - |
| F-LABS-R041 | La categoria gramatical autoritativa de una entrada es la declarada por EVP | critical | - |
| F-LABS-R042 | Las entradas de frase solo pueden rebajar el nivel de un lema, nunca subirlo | critical | - |
| F-LABS-R043 | Las categorias gramaticales no de contenido rebajan el nivel del lema | critical | - |
| F-LABS-R044 | Puente sustantivo-adverbio para expresiones temporales y locativas | major | - |
| F-LABS-R039 | Presencia de lemas bajo cualquier categoria gramatical | critical | - |
| F-LABS-R046 | Las entradas de metalenguaje curado no fijan lemas esperados por nivel | major | - |
| F-LABS-R006 | Tipos de ausencia | critical | - |
| F-LABS-R007 | Algoritmo de clasificacion de ausencia | critical | - |
| F-LABS-R008 | Puntuacion de impacto por tipo de ausencia | major | - |
| F-LABS-R009 | APPEARS_TOO_LATE es mas grave que APPEARS_TOO_EARLY | major | - |
| F-LABS-R010 | Un lema presente en su nivel esperado no es ausente | critical | - |
| F-LABS-R011 | Asignacion de prioridad por frecuencia COCA | critical | No se pudo determinar la prioridad del lema '{lema}': ranking COCA no disponible |
| F-LABS-R012 | Enriquecimiento de informacion de lemas ausentes | major | No se pudo enriquecer la informacion del lema '{lema}': {detalle del error} |
| F-LABS-R013 | Lemas sin ranking COCA disponible | major | Lema '{lema}' sin ranking COCA disponible: se asigna prioridad LOW por defecto |
| F-LABS-R014 | Umbrales de alerta por prioridad | major | - |
| F-LABS-R015 | Consistencia de etiquetas POS entre EVP y procesamiento linguistico | major | Inconsistencia de POS detectada para el lema '{lema}': EVP indica '{posEVP}' pero el procesamiento linguistico asigna '{posNLP}' |
| F-LABS-R016 | Lemas funcionales criticos | major | - |
| F-LABS-R017 | Identificacion de lemas mal ubicados en una oracion | major | - |
| F-LABS-R018 | Descuento por distancia de nivel | major | - |
| F-LABS-R019 | Calculo de score por oracion | critical | Score calculado fuera de rango [0.0, 1.0] para la oracion {sentenceId}: {score} |
| F-LABS-R020 | Puntuacion perfecta solo con vocabulario integramente justificado | major | - |
| F-LABS-R033 | Vocabulario de nivel superior al curso (C1/C2) | critical | - |
| F-LABS-R034 | Palabras de contenido fuera de catalogo: bandas de frecuencia por nivel | critical | - |
| F-LABS-R035 | Exclusion de nombres propios y tokens no lexicos | critical | - |
| F-LABS-R036 | Emparejamiento con fallback por lema (relajacion de la categoria gramatical) | critical | - |
| F-LABS-R045 | Repliegue del emparejamiento por lema normalizado a minusculas | major | - |
| F-LABS-R037 | Los niveles C1/C2 no participan de la cobertura de ausencias | critical | - |
| F-LABS-R038 | Detalle observable de los lemas penalizados por oracion | major | - |
| F-LABS-R021 | Umbrales de tolerancia por nivel CEFR | critical | Configuracion de umbrales invalida para el nivel {nivel}: los valores deben ser no negativos |
| F-LABS-R022 | Categorias de assessment global | critical | - |
| F-LABS-R023 | Metricas por nivel | critical | - |
| F-LABS-R024 | Puntuacion por nivel relativa al coverage target | critical | Error al calcular la puntuacion global: {detalle} |
| F-LABS-R032 | Coverage targets configurables por nivel | critical | - |
| F-LABS-R025 | Umbrales de assessment global | major | - |
| F-LABS-R026 | Limites de reporte por prioridad | minor | - |
| F-LABS-R027 | Generacion de recomendaciones por nivel | major | - |
| F-LABS-R028 | Tipos de accion en recomendaciones | major | - |
| F-LABS-R029 | Prioridad de las recomendaciones | major | - |
| F-LABS-R030 | Nivel de esfuerzo estimado | minor | - |
| F-LABS-R031 | Nombre del analizador en el informe | major | - |

**User Journeys:**

- **F-LABS-J001**: Auditar la ausencia de vocabulario de un curso completo

- **F-LABS-J002**: Investigar lemas completamente ausentes de alta prioridad

- **F-LABS-J003**: Corregir lemas que aparecen demasiado tarde

- **F-LABS-J004**: Revisar el impacto por oracion de los lemas mal ubicados

- **F-LABS-J005**: Planificar mejoras de contenido a partir de las recomendaciones

- **F-LABS-J006**: Detectar vocabulario avanzado invisible al rango del curso

### FEAT-CLI: Punto de Entrada CLI para Ejecucion de Auditoria [F-CLI]

> Proveer un punto de entrada por linea de comandos (CLI) que permita ejecutar la auditoria completa de un curso, ejercitando toda la cadena: carga del curso desde disco, transformacion a modelo auditable, ejecucion del motor de auditoria con todos los analizadores configurados, y presentacion del resultado en consola.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-CLI-R001 | Ejecucion de auditoria de extremo a extremo | critical | No se pudo ejecutar la auditoria para el curso en la ruta {path}: {detalle} |
| F-CLI-R002 | Argumento obligatorio: ruta del curso | critical | Uso: audit-cli <ruta-al-directorio-del-curso> |
| F-CLI-R003 | Presentacion del resultado en consola | major | - |
| F-CLI-R004 | Codigo de salida | major | - |
| F-CLI-R005 | Metodo publico en la capa de aplicacion | critical | Error al ejecutar la auditoria del curso: {detalle} |
| F-CLI-R006 | Ensamblaje de dependencias | major | Error al inicializar el sistema de auditoria: {detalle} |

**User Journeys:**

- **F-CLI-J001**: Ejecutar auditoria de un curso desde la terminal
  1. El usuario abre una terminal y navega al directorio del proyecto
  2. El usuario ejecuta el CLI proporcionando la ruta al curso: `java -jar audit-cli.jar /path/to/english-course`
  3. El sistema carga el curso desde el directorio especificado
  4. El sistema transforma el curso al modelo auditable, tokenizando las oraciones
  5. El sistema ejecuta todos los analizadores configurados sobre el curso auditable
  6. El sistema agrega las puntuaciones producidas por los analizadores a traves de la jerarquia del curso
  7. El CLI presenta en consola la puntuacion general y las puntuaciones por nivel
  8. El CLI termina con codigo de salida 0

- **F-CLI-J002**: Error por ruta invalida
  1. El usuario ejecuta el CLI sin argumentos o con una ruta que no existe
  2. El CLI muestra un mensaje de error indicando el uso correcto y la causa del error
  3. El CLI termina con codigo de salida distinto de cero

- **F-CLI-J003**: Error durante la auditoria
  1. El usuario ejecuta el CLI con una ruta valida pero el contenido del curso tiene problemas (por ejemplo, archivos JSON malformados)
  2. El sistema detecta el error durante la carga o el procesamiento
  3. El CLI muestra un mensaje de error descriptivo indicando que fallo y donde
  4. El CLI termina con codigo de salida distinto de cero

### FEAT-KTLEN: Analisis de Longitud de Titulos e Instrucciones de Knowledge [F-KTLEN]

> Evaluar si la longitud de los titulos (labels) y las instrucciones de cada knowledge (ejercicio) del curso cumple con los limites establecidos para garantizar una buena experiencia de usuario. Los titulos deben ser concisos para caber en elementos de navegacion e interfaz, y las instrucciones deben ser lo suficientemente breves para que el estudiante las lea antes de comenzar el ejercicio. Se producen puntuaciones individuales por knowledge que se agregan a traves de la jerarquia del curso mediante el motor de agregacion generico de la plataforma ContentAudit.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-KTLEN-R001 | Limite maximo de longitud de titulo | critical | El titulo del knowledge {knowledgeId} excede el limite maximo de longitud ponderada: {longitudPonderada} > 28 |
| F-KTLEN-R002 | Sistema de pesos por caracter para titulos | critical | - |
| F-KTLEN-R003 | Puntuacion de longitud de titulo | critical | Puntuacion fuera de rango [0.0, 1.0] calculada para el titulo del knowledge {knowledgeId}: {puntuacion} |
| F-KTLEN-R005 | Limites de longitud de instrucciones | critical | - |
| F-KTLEN-R006 | Puntuacion de longitud de instrucciones | critical | Puntuacion fuera de rango [0.0, 1.0] calculada para las instrucciones del knowledge {knowledgeId}: {puntuacion} |
| F-KTLEN-R008 | Nombres de los analizadores en el informe | major | - |

**User Journeys:**

- **F-KTLEN-J001**: Auditar la longitud de titulos e instrucciones de un curso completo
  1. El usuario inicia una auditoria de un curso previamente cargado en el sistema
  2. El sistema recorre la jerarquia del curso de arriba hacia abajo: para cada nivel (milestone), sus topics, y sus knowledges
  3. Para cada knowledge, el analizador de titulos calcula la longitud ponderada del titulo usando el sistema de pesos por caracter (R002) y produce una puntuacion individual (R003)
  4. Para cada knowledge, el analizador de instrucciones calcula la longitud ponderada de las instrucciones usando el sistema de pesos por caracter (R002) y produce una puntuacion basada en los umbrales suave y duro (R006)
  5. La plataforma agrega las puntuaciones de ambos analizadores por separado a traves de la jerarquia: para cada topic calcula el promedio de sus knowledges, para cada nivel el promedio de sus topics, y para el curso el promedio de sus niveles
  6. El usuario recibe un informe con las puntuaciones de longitud de titulos y de instrucciones en cada nivel de la jerarquia, permitiendo identificar donde se concentran los problemas

- **F-KTLEN-J002**: Identificar knowledges con titulos demasiado largos
  1. El usuario ha ejecutado la auditoria de un curso (J001)
  2. El usuario observa que la puntuacion del analizador "knowledge-title-length" en un topic es baja (por ejemplo, 0.6)
  3. El usuario profundiza en los knowledges del topic
  4. El usuario identifica los knowledges cuyo titulo excede los 28 caracteres ponderados y tienen puntuacion menor a 1.0
  5. El usuario revisa los titulos afectados y los acorta o reformula para que se ajusten al limite, priorizando los que tienen puntuacion mas baja (titulos mas excedidos)

- **F-KTLEN-J003**: Identificar knowledges con instrucciones excesivamente largas
  1. El usuario ha ejecutado la auditoria de un curso (J001)
  2. El usuario observa que la puntuacion del analizador "knowledge-instructions-length" en un nivel es baja
  3. El usuario profundiza en los topics y knowledges del nivel
  4. El usuario identifica los knowledges cuyas instrucciones tienen puntuacion 0.0 (mas de 100 caracteres ponderados) o 0.5 (entre 70 y 100 caracteres ponderados)
  5. El usuario prioriza las instrucciones con puntuacion 0.0 (las mas criticas) y las sintetiza para reducir su longitud por debajo de los 70 caracteres

- **F-KTLEN-J004**: Comparar problemas de titulos vs instrucciones por nivel
  1. El usuario ha ejecutado la auditoria de un curso (J001)
  2. El usuario consulta las puntuaciones por nivel para ambos analizadores
  3. El usuario compara las puntuaciones de "knowledge-title-length" y "knowledge-instructions-length" en cada nivel
  4. El usuario identifica si los problemas de longitud se concentran en los titulos, en las instrucciones, o en ambos
  5. Esta comparacion permite al usuario priorizar acciones correctivas: si solo los titulos son problematicos, puede enfocarse en reformularlos sin revisar las instrucciones, y viceversa

### FEAT-DSLEN: Diagnosticos Tipados para el Analizador de Longitud de Oraciones [F-DSLEN]

> Exponer la informacion que el analizador de longitud de oraciones (FEAT-SLEN) calcula internamente como un **registro de diagnostico tipado** a nivel quiz, de modo que los consumidores (en particular el futuro refiner) puedan conocer no solo la puntuacion sino tambien los datos que la determinaron: cuantos tokens tiene la oracion, cual es el rango esperado para su nivel CEFR, cuanto se desvio y cual es el margen de tolerancia.

Este es el tercer paso de la iniciativa de diagnosticos tipados, tras FEAT-DLABS (lemma-absence) y FEAT-DCOCA (coca-buckets-distribution).

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-DSLEN-R001 | Registro de diagnostico a nivel quiz: SentenceLengthDiagnosis | critical | - |
| F-DSLEN-R002 | Ausencia de diagnostico en quizzes excluidos | major | - |
| F-DSLEN-R003 | Ausencia de diagnostico en niveles superiores | major | - |
| F-DSLEN-R004 | Nuevo metodo en la interfaz QuizDiagnoses | critical | - |

**User Journeys:**

- **F-DSLEN-J001**: Consultar diagnostico de longitud desde el refiner para corregir una oracion

### FEAT-SLEN: Analisis de Longitud de Oraciones por Nivel CEFR [F-SLEN]

> Evaluar si la longitud de las oraciones a lo largo del curso es apropiada para cada nivel de dificultad esperado (CEFR), generando puntuaciones por quiz, estadisticas por nivel, recomendaciones y un analisis de progresion entre niveles. Las puntuaciones individuales por quiz se agregan a traves de la jerarquia del curso (knowledge, topic, nivel, curso) mediante el motor de agregacion generico de la plataforma ContentAudit.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-SLEN-R001 | Exclusion de quizzes que no son oraciones | critical | Se incluyo un quiz no-oracion en el calculo de longitud del knowledge {knowledgeId} |
| F-SLEN-R002 | Puntuacion por quiz (oracion individual) | critical | Puntuacion fuera de rango [0.0, 1.0] calculada para el quiz ({quizId}) en el knowledge {knowledgeId}, nivel {nivel}: {puntuacion} |
| F-SLEN-R009 | Margen de tolerancia fijo de 4 tokens | minor | - |
| F-SLEN-R012 | Rangos objetivo configurables por nivel | major | Configuracion de rangos invalida para el nivel {nivel}: el minimo ({min}) debe ser menor o igual al maximo ({max}) |
| F-SLEN-R013 | La longitud se mide en tokens linguisticos | critical | La longitud de la oracion no se obtuvo de tokens linguisticos: fuente inesperada de datos |
| F-SLEN-R003 | Puntuacion por knowledge (agregacion de la plataforma) | critical | Error al calcular la puntuacion del knowledge {knowledgeId}: {detalle} |
| F-SLEN-R004 | Puntuacion por topic (agregacion de la plataforma) | critical | Error al calcular la puntuacion del topic {topicId}: {detalle} |
| F-SLEN-R005 | Puntuacion por nivel (agregacion de la plataforma) | critical | Error al calcular la puntuacion del nivel {nivel}: {detalle} |
| F-SLEN-R008 | Puntuacion general del curso (agregacion de la plataforma) | critical | Error al calcular la puntuacion general: {detalle} |
| F-SLEN-R016 | Puntuaciones disponibles en cada nivel de la jerarquia (agregacion de la plataforma) | major | - |

**User Journeys:**

- **F-SLEN-J004**: Navegar la jerarquia para localizar problemas de longitud
  1. El usuario ha ejecutado una auditoria de longitud de oraciones sobre un curso previamente cargado en el sistema (el analizador puntua cada quiz valido segun R001/R002/R013 y la plataforma agrega las puntuaciones a traves de la jerarquia segun R003/R004/R005/R008)
  2. El usuario observa que un nivel (por ejemplo, B1) tiene una puntuacion baja
  3. El usuario profundiza en los topics del nivel B1 y encuentra que el topic "Modal Verbs" tiene la puntuacion mas baja
  4. El usuario profundiza en los knowledges de "Modal Verbs" y encuentra que el knowledge "Should for advice" tiene puntuacion 0.4
  5. El usuario revisa los quizzes de "Should for advice" y encuentra que varias oraciones tienen 18 tokens (demasiado largas para B1, rango 11-14)
  6. El usuario identifica exactamente que oraciones necesitan simplificarse y puede tomar acciones correctivas puntuales

### FEAT-LCOUNT: Analisis de Conteo de Repeticiones de Lemas Content-Word [F-LCOUNT]

> **Que**: Mide, para cada lema content-word que aparece al menos una vez en
el curso, en cuantas oraciones distintas se repite, y resume la
exposicion por nivel CEFR y a nivel del curso completo.

**Por que**: Una vez que el curso introduce un lema, el aprendizaje
requiere que se repita varias veces en oraciones distintas. Sin esa
repeticion, la exposicion es nominal y el lema no se consolida.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-LCOUNT-R001 | Solo se evaluan lemas content-word | high | Token sin etiqueta POS en el quiz {quizId}: el procesamiento
linguistico previo no asigno parte de la oracion |
| F-LCOUNT-R002 | El lema se identifica por su forma base mas su POS | high | - |
| F-LCOUNT-R003 | Solo entran al analisis los lemas con al menos una aparicion | high | - |
| F-LCOUNT-R004 | El conteo es la cantidad de oraciones distintas que contienen el lema | high | Error al contar oraciones para el lema {lema} ({pos}):
{detalle} |
| F-LCOUNT-R005 | El conteo abarca todo el curso | high | - |
| F-LCOUNT-R006 | El score por lema es lineal hasta el umbral N | high | Score fuera de rango [0.0, 1.0] calculado para el lema
{lema} ({pos}): {score} |
| F-LCOUNT-R007 | Si N no esta configurado, se aplica el default 4 | medium | - |
| F-LCOUNT-R008 | Si N esta configurado y es invalido, falla al cargar la configuracion | high | Umbral N invalido en la configuracion: {valor}. Debe ser un
entero positivo. |
| F-LCOUNT-R009 | El nivel CEFR del lema se toma del catalogo EVP cuando esta disponible | high | - |
| F-LCOUNT-R010 | Si el lema no esta en EVP, se intenta el nivel CEFR del procesamiento linguistico (solo si esta disponible) | high | - |
| F-LCOUNT-R011 | Si ninguna fuente provee nivel CEFR, el lema queda en el grupo "no asignado" | high | - |
| F-LCOUNT-R012 | El score de un nivel CEFR es el promedio simple de los scores de sus lemas | high | No se pudo calcular el score del nivel CEFR {nivel}: {detalle} |
| F-LCOUNT-R013 | El score del curso es el promedio simple de los scores de los niveles CEFR | high | No se pudo calcular el score del curso: ningun nivel CEFR
tiene score asignable |
| F-LCOUNT-R014 | El grupo "no asignado" no entra al score del curso | high | - |
| F-LCOUNT-R015 | El reporte expone el score global del curso | medium | - |
| F-LCOUNT-R016 | El reporte expone explicitamente el umbral N usado | medium | - |
| F-LCOUNT-R017 | El reporte detalla cada nivel CEFR | medium | - |
| F-LCOUNT-R018 | El reporte lista los lemas del grupo "no asignado" | medium | - |
| F-LCOUNT-R019 | El analizador se identifica con el nombre "lemma-count" | low | - |

**User Journeys:**

- **F-LCOUNT-J001**: Consultar la exposicion de los lemas content-word del curso

### FEAT-RCLA: Re-routing y Contexto de Correccion para LEMMA_ABSENCE en el Refiner [F-RCLA]

> El refiner actualmente genera tareas LEMMA_ABSENCE a nivel MILESTONE y COURSE. Estas tareas no son accionables: dicen "al nivel B1 le faltan estos lemas" pero no apuntan a un quiz especifico que un LLM pueda corregir. Mientras tanto, el analyzer ya detecta vocabulario fuera de nivel en quizzes individuales (un quiz de A1 que usa una palabra de B2), pero el refiner ignora esos hallazgos.

Este requerimiento hace dos cosas: (1) re-rutea las tareas LEMMA_ABSENCE para que apunten a quizzes individuales en lugar de milestones/cursos, y (2) agrega el contexto de correccion que un LLM necesita para reescribir la oracion reemplazando el vocabulario fuera de nivel, incluyendo sugerencias de lemas ausentes del nivel que pueden servir como reemplazo.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-RCLA-R001 | Las tareas LEMMA_ABSENCE deben apuntar a QUIZ, no a MILESTONE ni COURSE | critical | - |
| F-RCLA-R003 | Estructura del contexto de correccion para LEMMA_ABSENCE | critical | - |
| F-RCLA-R004 | Estructura de cada lema fuera de nivel | critical | - |
| F-RCLA-R004b | Obtencion de lemas sugeridos como candidatos de reemplazo | critical | - |
| F-RCLA-R004c | Contexto sin lemas sugeridos | major | - |
| F-RCLA-R003b | Palabras de contenido escasas de la oracion actual | major | - |
| F-RCLA-R003c | Contexto sin palabras escasas | major | - |
| F-RCLA-R003d | Estructura de cada palabra fuera de catalogo | critical | - |
| F-RCLA-R003e | Contexto sin palabras fuera de catalogo | major | - |
| F-RCLA-R005 | Resolucion del nodo quiz desde una tarea de refinamiento | critical | No se pudo cargar el reporte de auditoria '{sourceAuditId}' necesario para construir el contexto de correccion |
| F-RCLA-R006 | Contexto cuando el diagnostico de placement no esta disponible | major | No se pudo obtener el diagnostico de placement de lemas para el quiz '{nodeId}' |
| F-RCLA-R007 | El comando que muestra una tarea individual incluye el contexto de correccion para tareas LEMMA_ABSENCE | critical | - |
| F-RCLA-R008 | Formato JSON del contexto de correccion | critical | No se pudo construir el contexto de correccion: {motivo} |
| F-RCLA-R009 | Formato texto del contexto de correccion | major | - |

**User Journeys:**

- **F-RCLA-J001**: LLM recibe contexto para corregir un quiz con vocabulario fuera de nivel

- **F-RCLA-J002**: El plan de refinamiento ya no contiene tareas LEMMA_ABSENCE de milestone

### FEAT-RCSL: Contexto de Correccion para SENTENCE_LENGTH en el Refiner [F-RCSL]

> Cuando el refiner identifica una tarea de tipo SENTENCE_LENGTH, actualmente muestra **que** hay que corregir (cual quiz, que analizador la detecto, con que prioridad) pero no provee el **contexto** necesario para que un LLM pueda reescribir la oracion. Este requerimiento agrega la informacion contextual que un LLM necesita para generar una correccion informada: la oracion actual, el diagnostico de longitud, y una lista de lemas sugeridos que el LLM puede incorporar en la nueva oracion.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-RCSL-R001 | Estructura del contexto de correccion para SENTENCE_LENGTH | critical | - |
| F-RCSL-R002 | Resolucion del nodo quiz desde una tarea de refinamiento | critical | No se pudo cargar el reporte de auditoria '{sourceAuditId}' necesario para construir el contexto de correccion |
| F-RCSL-R003 | Obtencion de lemas sugeridos desde el milestone ancestro | critical | - |
| F-RCSL-R004 | Contexto sin lemas sugeridos | major | - |
| F-RCSL-R005 | Limite de lemas sugeridos | minor | - |
| F-RCSL-R006 | El comando que muestra una tarea individual incluye el contexto de correccion para tareas SENTENCE_LENGTH | critical | - |
| F-RCSL-R007 | Formato JSON del contexto de correccion | critical | No se pudo construir el contexto de correccion: {motivo} |
| F-RCSL-R008 | Formato texto del contexto de correccion | major | - |

**User Journeys:**

- **F-RCSL-J001**: LLM recibe contexto para corregir una oracion demasiado larga

- **F-RCSL-J002**: LLM recibe contexto para corregir una oracion demasiado corta

- **F-RCSL-J003**: Contexto de correccion sin lemas sugeridos disponibles

### FEAT-REVBYP: Fase de revision (bypass skeleton) - aplicacion de cambios a elementos del curso [F-REVBYP]

> Hoy la pipeline de ContentAudit cubre tres fases: `analyze` produce un reporte de auditoria, `refiner plan` genera un plan de tareas a partir del reporte, y `refiner next` resuelve el contexto de correccion de la proxima tarea. Lo que falta es la fase que efectivamente **aplica** un cambio a un elemento del curso en base a ese contexto.

Este requerimiento define esa fase, llamada **revision**. El alcance es deliberadamente minimo: se construye el esqueleto end-to-end con componentes **bypass** (identidad, auto-aprobacion) para validar que la pipeline completa funciona — que una tarea se puede tomar, construir su propuesta de revision, persistirla como artefacto auditable, aprobarla, y reescribir el curso modificado al disco. Ninguna logica real de revision (AI, reglas, transformaciones) entra en esta iteracion. Esas estrategias se enchufaran en requerimientos futuros.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-REVBYP-R001 | Estructura de la RevisionProposal | critical | - |
| F-REVBYP-R002 | En el caso bypass, elementBefore y elementAfter son semanticamente equivalentes | critical | - |
| F-REVBYP-R003 | El Reviser es pluggable por DiagnosisKind | critical | - |
| F-REVBYP-R004 | Existe un Reviser bypass que actua como baseline por defecto | critical | - |
| F-REVBYP-R005 | Si no hay Reviser aplicable, la revision no se realiza | major | No hay Reviser registrado para el diagnostico '{diagnosisKind}' |
| F-REVBYP-R006 | El Validator decide si una propuesta se aplica | critical | - |
| F-REVBYP-R007 | Existe un Validator bypass que auto-aprueba toda propuesta | critical | - |
| F-REVBYP-R008 | Cada propuesta se persiste como artefacto bajo .content-audit/revisions/ | critical | No se pudo persistir la propuesta de revision '{proposalId}' bajo .content-audit/revisions/ |
| F-REVBYP-R009 | Organizacion de los artefactos de propuesta | major | - |
| F-REVBYP-R010 | El artefacto es suficiente para reconstruir la decision | major | - |
| F-REVBYP-R011 | Una propuesta aprobada se aplica al curso en disco | critical | No se pudo aplicar la propuesta de revision '{proposalId}' al curso |
| F-REVBYP-R012 | Una propuesta rechazada no modifica el curso | critical | - |
| F-REVBYP-R013 | El estado de la tarea refleja el resultado de la revision | major | - |
| F-REVBYP-R014 | El artefacto se persiste antes de aplicar al curso | major | La propuesta '{proposalId}' fue aprobada y persistida como artefacto, pero la escritura del curso fallo |

**User Journeys:**

- **F-REVBYP-J001**: Revision bypass end-to-end de una tarea

### FEAT-CLIRV: CLI restructure to kubectl-style verb-resource grammar [F-CLIRV]

> The `content-audit` CLI evolved feature-by-feature into a tree of subcommand groups (`refiner plan`, `refiner next`, `refiner list`, `refiner revise`, `analyzer list`, `analyzer config`, `analyzer stats`) plus the standalone `analyze`. This shape has produced several recurring problems in actual operator use, and adding more capabilities on top of it would amplify them. This requirement specifies a one-shot restructure of the CLI surface into a kubectl-style **verb-then-resource** grammar so that future features land naturally instead of accreting another subcommand group per concern.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-CLIRV-R001 | `get` is read-only and returns one or many | critical | No <resource> found with id '<id>' |
| F-CLIRV-R002 | `delete` removes exactly one resource by id, on `audit` and `plan` only | critical | Cannot delete <resource>: id is required |
| F-CLIRV-R003 | `prune` performs bulk cleanup under an explicit retention policy | critical | Cannot prune <resource>: --keep N is required |
| F-CLIRV-R004 | `analyze`, `plan`, `revise`, `config` are preserved as domain verbs | major | - |
| F-CLIRV-R005 | Recognized resources | critical | Unknown resource '<name>'. Known resources: audits, plans, tasks, analyzers |
| F-CLIRV-R006 | Singular and plural resource names are interchangeable | major | - |
| F-CLIRV-R007 | Resource ids are presented uniformly across verbs | minor | - |
| F-CLIRV-R008 | `get tasks` filters by plan, status, target, diagnosis, and result count | critical | Invalid value for --status: '<value>'. Allowed: pending, completed, skipped |
| F-CLIRV-R009 | `--limit 0` yields an empty result, not an error | minor | Invalid value for --limit: must be zero or positive |
| F-CLIRV-R010 | When a referenced source resource does not exist, the request fails clearly | major | <Resource> '<id>' not found |
| F-CLIRV-R011 | Empty result sets are reported, not silenced | minor | - |
| F-CLIRV-R012 | `get tasks` without `--plan` resolves to the most recent plan | major | No plans available. Run 'content-audit plan' to create one. |
| F-CLIRV-R013 | `analyze` is a top-level verb that creates an audit | critical | - |
| F-CLIRV-R014 | `plan` is a top-level verb that creates a refinement plan | critical | Audit '<id>' not found |
| F-CLIRV-R015 | `revise` operates on a task, scoped to a plan | critical | No plans available. Run 'content-audit plan' first. |
| F-CLIRV-R016 | `config` operates on an analyzer | major | Analyzer '<name>' not found. Run 'content-audit get analyzers' to see available analyzers. |
| F-CLIRV-R021 | `stats` is a top-level verb that introspects a single analyzer's behavior on a course | major | Analyzer '<name>' not found. Run 'content-audit get analyzers' to see available analyzers. |
| F-CLIRV-R017 | The CLI accepts an external override for the workdir via `--workdir` flag and `CONTENT_AUDIT_HOME` env var | critical | The configured workdir override '<path>' is not a writable directory |
| F-CLIRV-R018 | The course content path is independent of the workdir override | major | - |
| F-CLIRV-R019 | No domain logic changes | critical | - |
| F-CLIRV-R020 | No backwards-compatibility shims | major | - |

**User Journeys:**

- **F-CLIRV-J001**: Full happy-path pipeline end-to-end with the new grammar

- **F-CLIRV-J002**: Reading a single resource by id, present and absent

- **F-CLIRV-J003**: Deleting a resource, present and absent

- **F-CLIRV-J004**: Singular and plural forms produce the same result

- **F-CLIRV-J005**: Sandbox workdir override redirects all artifact I/O

### FEAT-REVAPR: Fase de revision con aprobacion humana [F-REVAPR]

> FEAT-REVBYP introdujo la fase de **revision** como un flujo sincronico end-to-end: en una sola invocacion de `revise task <id>`, el sistema genera una propuesta, la valida, la persiste como artefacto y la aplica al curso. En esa iteracion el validator era el **bypass** (auto-aprueba todo), de modo que la aprobacion no involucraba al operador.

Este requerimiento introduce un **modo de aprobacion humana**: la revision se parte en dos fases separadas por un estado persistido de "pendiente de aprobacion". El operador corre primero `revise task <id>`, que produce la propuesta y la deja esperando decision; luego inspecciona el artefacto y corre `approve proposal <id>` o `reject proposal <id>` para decidir. Solo al aprobarse se reescribe el curso. El rechazo devuelve la tarea al estado previo sin tocar el curso.

El objetivo es validar que la pipeline de revision soporta un **punto de corte operativo** entre la generacion de la propuesta y su aplicacion, sin introducir todavia logica de revision real ni workflows multi-etapa.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-REVAPR-R001 | Se agrega `PENDING_APPROVAL` al vocabulario de veredictos | critical | - |
| F-REVAPR-R002 | `proposal` es un recurso de primera clase en la CLI | critical | No proposal found with id '<id>' |
| F-REVAPR-R003 | Filtros soportados en `get proposals` | major | Invalid value for --status: '<value>'. Allowed: pending, approved, rejected |
| F-REVAPR-R004 | `delete` y `prune` sobre `proposal` quedan fuera de alcance | minor | - |
| F-REVAPR-R005 | El modo de aprobacion se selecciona por variable de entorno | critical | Invalid value for CONTENT_AUDIT_APPROVAL_MODE: '<value>'. Allowed: auto, human |
| F-REVAPR-R006 | Hay exactamente un validator activo por invocacion | major | - |
| F-REVAPR-R007 | El validator humano emite `PENDING_APPROVAL` | critical | - |
| F-REVAPR-R008 | Artefacto persistido con `PENDING_APPROVAL` | critical | - |
| F-REVAPR-R009 | La tarea queda en estado "esperando aprobacion" | critical | - |
| F-REVAPR-R010 | Re-revisar una tarea con propuesta pendiente es un error | major | Task '<task-id>' in plan '<plan-id>' already has a pending proposal '<proposalId>'. Decide it with 'approve proposal <proposalId>' or 'reject proposal <proposalId>' first. |
| F-REVAPR-R011 | `approve proposal <id>` aprueba una propuesta pendiente y aplica al curso | critical | No proposal found with id '<id>' |
| F-REVAPR-R012 | `reject proposal <id>` rechaza una propuesta pendiente sin tocar el curso | critical | No proposal found with id '<id>' |
| F-REVAPR-R013 | Decidir dos veces la misma propuesta es un error, no un no-op | major | Cannot <approve|reject> proposal '<id>': its current verdict is '<verdict>', not PENDING_APPROVAL |
| F-REVAPR-R014 | Transiciones de estado de la tarea bajo el flujo aprobado | critical | - |
| F-REVAPR-R015 | `approve` y `reject` solo operan sobre el recurso `proposal` | major | Unknown resource '<name>' for verb 'approve' / 'reject'. Supported: proposal |
| F-REVAPR-R016 | El id de la propuesta impreso por `revise task <id>` en modo humano se puede copiar a `approve` / `reject` | major | - |
| F-REVAPR-R017 | El workdir override de FEAT-CLIRV R017 aplica a las propuestas | major | - |

**User Journeys:**

- **F-REVAPR-J001**: Flujo humano end-to-end: propuesta -> aprobacion -> aplicado

- **F-REVAPR-J002**: Flujo humano end-to-end: propuesta -> rechazo -> tarea vuelve a PENDING

- **F-REVAPR-J003**: Decidir dos veces la misma propuesta es un error

- **F-REVAPR-J004**: Re-revisar una tarea con propuesta pendiente es un error

- **F-REVAPR-J005**: Listar propuestas por plan y por estado

### FEAT-QSENT: Formalizar quizSentence como concepto de primera clase del course-domain [F-QSENT]

> El modelo del curso (FEAT-CSTRUCT) tiene `FormEntity.sentenceParts`, una lista de `SentencePartEntity { kind: TEXT|CLOZE, text, options }` que describe la forma estructural del ejercicio (texto plano + huecos con sus respuestas). Esa estructura sirve para renderizar el ejercicio y para evaluar la respuesta del alumno. **No sirve** como representacion compacta para transmitir un ejercicio a un humano (o a un LLM) ni como formato sobre el cual un componente externo pueda proponer una variante (reescribirlo, generarlo, corregirlo).

Hoy existe una conversion implicita y parcial: el mapper de auditoria del sistema (audit-application, privado) concatena los TEXT y toma el primer elemento de `options` del CLOZE, produciendo una oracion plana para NLP. Esa conversion **tiene dos bugs conocidos**: (1) ignora las variantes aceptadas separadas por `|` dentro de las options (toma el primer elemento de la lista pero no resuelve el pipe, emitiendo literales tipo `"is|'s"` en la oracion que consume el analyzer); (2) deja los hints pedagogicos en el texto que procesa el analyzer (por ejemplo `"(loud / loudly)"` aparece tal cual en la oracion plana). Ademas, el pipeline legacy que vivia en una fase de generacion de oraciones manejaba un formato textual tipo DSL (`____ [correct|variant] (hint)`) para el mismo proposito, pero esa DSL no esta formalizada en ningun lugar del dominio — vive como convenciones sueltas en un prompt.

Este requerimiento formaliza el concepto **`quizSentence`**: una representacion textual, compacta y deterministica del ejercicio, bidireccionalmente convertible con la `sentenceParts`, con su gramatica explicita, sus invariantes y sus tests. Es condicion para que otras features (la que propone modificaciones reales de quizzes via LLM, por ejemplo) puedan operar sobre una representacion de quiz que el dominio garantiza correcta.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-QSENT-R001 | Todas las entries de `options` y todos los splits por `|` son variantes equivalentes de la misma respuesta correcta | critical | El CLOZE de id '{cloze}' expone una lista de options malformada |
| F-QSENT-R002 | El caracter `|` separa variantes equivalentes dentro de una entry | critical | - |
| F-QSENT-R003 | Un TEXT no puede tener options | critical | El TEXT de indice {idx} trae options; los TEXT no pueden tener options |
| F-QSENT-R004 | Un CLOZE no puede tener options ausentes | critical | El CLOZE de indice {idx} no tiene options; un CLOZE requiere al menos una respuesta aceptada |
| F-QSENT-R005 | Blank se escribe como cuatro guiones bajos | critical | Se encontro una secuencia de guiones bajos de largo distinto de cuatro; un blank se escribe '____' |
| F-QSENT-R006 | Bloque de respuesta sigue al blank entre corchetes | critical | Bloque de respuesta ausente o desvinculado del blank que lo antecede en la posicion {pos} |
| F-QSENT-R007 | Hint pedagogico es un elemento formal de la DSL escrito entre parentesis inline | major | - |
| F-QSENT-R008 | Un quizSentence valido alterna texto y blank+bloque | critical | quizSentence malformado: {descripcion} en la posicion {pos} |
| F-QSENT-R009 | Los caracteres reservados requieren un mecanismo de escape | major | Caracter reservado encontrado sin escapar en el texto: '{char}' en la posicion {pos} |
| F-QSENT-R010 | Whitespace canonico en la serializacion, tolerante en el parser, normalizado en la equivalencia | critical | - |
| F-QSENT-R011 | Cada TEXT se concatena con whitespace canonico | critical | - |
| F-QSENT-R012 | Cada CLOZE se serializa como `____ [variantes-unidas-por-pipe]` | critical | - |
| F-QSENT-R013 | La serializacion falla ante datos invalidos | critical | No es posible serializar a quizSentence: {razon} |
| F-QSENT-R014 | Parseo secuencial de blank+bloque | critical | - |
| F-QSENT-R015 | Los hints se preservan dentro de los TEXT en el parseo | major | - |
| F-QSENT-R016 | El parseo falla ante un quizSentence malformado | critical | No es posible parsear el quizSentence: {razon} |
| F-QSENT-R017 | La plain sentence es una lista ordenada de strings, una por variante equivalente | critical | - |
| F-QSENT-R018 | La variante canonica es la primera sub-variante del primer entry de options | critical | - |
| F-QSENT-R019 | La plain sentence no contiene hints y tiene whitespace normalizado | critical | - |
| F-QSENT-R020 | Equivalencia de plain sentence entre las dos rutas | critical | Inconsistencia de plain sentence entre rutas: '{via_parts}' vs '{via_dsl}' |
| F-QSENT-R021 | Round-trip sentenceParts -> quizSentence -> sentenceParts preserva la semantica | critical | Round-trip rompe la equivalencia semantica del sentenceParts en {campo} |
| F-QSENT-R022 | Round-trip quizSentence -> sentenceParts -> quizSentence preserva el string con whitespace normalizado | critical | Round-trip rompe la estabilidad textual del quizSentence: '{original}' vs '{recuperado}' |
| F-QSENT-R023 | La derivacion a plain sentence es estrictamente unidireccional | critical | - |
| F-QSENT-R024 | Las conversiones fallan de forma atomica | critical | Conversion abortada: {razon} |
| F-QSENT-R025 | La derivacion a plain sentence es funcionalidad publica del course-domain | critical | - |
| F-QSENT-R028 | Los fixtures reales del curso son base de pruebas | major | - |

**User Journeys:**

- **F-QSENT-J001**: Round-trip de dominio sobre un sentenceParts arbitrario

### FEAT-LAPS: Primera estrategia real de propuesta de revision para LEMMA_ABSENCE [F-LAPS]

> Hasta ahora la fase de revision (FEAT-REVBYP / FEAT-REVAPR) produce propuestas **identidad**: el Reviser activo para cualquier `DiagnosisKind` es el bypass y `elementAfter == elementBefore`. Eso sirvio para validar la pipeline end-to-end (construccion de propuesta, persistencia del artefacto, validacion, aprobacion, escritura al curso), pero no corrige contenido.

Este requerimiento introduce la **primera propuesta real** del sistema, y la acota a tareas de tipo `LEMMA_ABSENCE`. Dada una tarea de refinamiento de ese tipo y su `CorrectionContext` completo (el que ya construye FEAT-RCLA, incluyendo el `quizSentence` del quiz original en la DSL de FEAT-QSENT), el sistema debe producir una `RevisionProposal` cuyo `elementAfter` contenga un **quiz nuevo** cuyo cambio central es el ejercicio mismo (la oracion, los blanks, la respuesta correcta y sus variantes aceptadas, todo codificado dentro del `quizSentence`), pudiendo variar tambien la traduccion al espanol, corrigiendo las palabras fuera de nivel segun los `suggestedLemmas` que el contexto ya aporta. El ejercicio se maneja como `quizSentence` (no como oracion plana) porque un quiz no es solo una oracion: su estructura (que se tapa, que respuestas son validas, que variantes se aceptan) es parte intrinseca del ejercicio. Todo el flujo posterior (validacion, aprobacion, persistencia del artefacto, escritura al curso) se reutiliza tal cual de FEAT-REVBYP y FEAT-REVAPR — este requerimiento termina cuando la propuesta quedo construida.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-LAPS-R001 | Las tareas LEMMA_ABSENCE producen propuestas no-identidad | critical | - |
| F-LAPS-R002 | El bypass deja de ser el Reviser activo para LEMMA_ABSENCE | critical | - |
| F-LAPS-R003 | El ejercicio nuevo reemplaza integramente al original | critical | - |
| F-LAPS-R004 | El sistema soporta multiples estrategias de propuesta registradas | major | - |
| F-LAPS-R005 | La identidad de la estrategia queda registrada en la propuesta | critical | - |
| F-LAPS-R006 | Si no hay estrategia activa que soporte LEMMA_ABSENCE, la revision falla explicitamente | major | No hay una estrategia de propuesta activa para el diagnostico 'LEMMA_ABSENCE' |
| F-LAPS-R007 | La estrategia MVP consume el CorrectionContext completo de FEAT-RCLA | critical | - |
| F-LAPS-R008 | La estrategia MVP produce un unico candidato de quiz por invocacion | critical | - |
| F-LAPS-R009 | El candidato de quiz tiene una forma conceptual definida | critical | - |
| F-LAPS-R010 | La estrategia MVP es no-deterministica por defecto | minor | - |
| F-LAPS-R011 | El output de la estrategia es un candidato de quiz expresado en la DSL de FEAT-QSENT | critical | - |
| F-LAPS-R012 | La derivacion del elementAfter a partir del candidato es deterministica | critical | - |
| F-LAPS-R013 | Campos del elemento que pueden diferir entre elementBefore y elementAfter | critical | - |
| F-LAPS-R014 | Campos del elemento que NO pueden diferir entre elementBefore y elementAfter | critical | - |
| F-LAPS-R015 | Si la estrategia no puede producir un candidato, la revision aborta antes de persistir | critical | La estrategia de propuesta '{estrategia}' no pudo generar un candidato de quiz para la tarea '{taskId}' |
| F-LAPS-R016 | Una falla de estrategia no es un REJECTED ni un PENDING_APPROVAL | major | - |
| F-LAPS-R017 | La estrategia no valida el nivel CEFR del output | minor | - |
| F-LAPS-R018 | La estrategia no valida el largo del output | minor | - |
| F-LAPS-R019 | La estrategia emite el candidato y nada mas | critical | - |

**User Journeys:**

- **F-LAPS-J001**: Flujo feliz end-to-end con validador auto

- **F-LAPS-J002**: Flujo feliz end-to-end con aprobacion humana

- **F-LAPS-J003**: La estrategia falla al producir el candidato

- **F-LAPS-J004**: No hay estrategia activa para LEMMA_ABSENCE

- **F-LAPS-J005**: El operador rechaza la propuesta en modo humano

### FEAT-RCLAQS: Exponer quizSentence en el CorrectionContext de LEMMA_ABSENCE [F-RCLAQS]

> FEAT-RCLA construye un `CorrectionContext` para cada tarea de refinamiento de tipo `LEMMA_ABSENCE` con la oracion del quiz en texto plano (`sentence`), la traduccion al espanol, el contexto pedagogico (knowledge, topic, CEFR), la lista de palabras fuera de nivel (`misplacedLemmas`) y lemas sugeridos de reemplazo (`suggestedLemmas`). FEAT-QSENT formalizo la DSL `quizSentence` como representacion textual compacta del ejercicio (tronco + blanks + respuestas aceptadas + hints) y dejo explicitamente como "micro-update posterior" extender el `CorrectionContext` de RCLA para incluir ese campo. FEAT-LAPS, la primera estrategia real de propuesta para `LEMMA_ABSENCE`, consume exactamente ese `quizSentence` como unica fuente de entrada estructural y declara dependencia sobre este cambio.

Este micro-requerimiento es un delta aislado: agrega el campo `quizSentence` al `CorrectionContext` de tareas `LEMMA_ABSENCE` alimentandolo desde la misma fuente de datos desde la que se deriva la `sentence` plana (el `FormEntity` del quiz original). No se modifica ni el modelo de curso, ni la DSL de FEAT-QSENT, ni el resto del `CorrectionContext`, ni la estrategia de propuesta de FEAT-LAPS. La `sentence` plana existente se mantiene sin cambios para preservar a los consumidores actuales que dependen de ella.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-RCLAQS-R001 | El CorrectionContext de LEMMA_ABSENCE expone un campo quizSentence | critical | - |
| F-RCLAQS-R002 | El valor de quizSentence se deriva via el conversor canonico de FEAT-QSENT sobre el mismo FormEntity del quiz original | critical | - |
| F-RCLAQS-R003 | Consistencia entre quizSentence y sentence (plana) dentro del mismo contexto | critical | Inconsistencia entre quizSentence y sentence del CorrectionContext para la tarea '{taskId}' |
| F-RCLAQS-R004 | La derivacion falla atomicamente si el FormEntity original es invalido segun FEAT-QSENT | critical | No se pudo derivar el quizSentence del quiz original '{nodeId}' para la tarea '{taskId}': {razon} |
| F-RCLAQS-R005 | El campo quizSentence aparece en la salida JSON del contexto | major | - |

**User Journeys:**

- **F-RCLAQS-J001**: El CorrectionContext de LEMMA_ABSENCE expone quizSentence derivado del quiz original

### f0b4e57b-7c0f-4e3e-b9b3-843aef216a67: null [F-LEPV]

**User Journeys:**

- **86e348a5-bfdf-4c5e-beef-aa0113168e38**: null

### FEAT-LAGEN: Generador real de candidatos de quiz para LEMMA_ABSENCE basado en LLM [F-LAGEN]

> **Que**: Reemplaza el candidato canned de la estrategia de propuesta para `LEMMA_ABSENCE` por uno generado en vivo por un modelo de lenguaje, alimentado por todo el `LemmaAbsenceCorrectionContext` poblado.

**Por que**: Tras esta iteracion, lo que el operador ve archivado al correr `revise task <id>` es contenido producido por un modelo real e identificado como tal en la metadata de cada propuesta, en lugar de la respuesta fija independiente del contexto.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-LAGEN-R001 | El nombre de la estrategia activa para LEMMA_ABSENCE pasa a ser `lemma-absence-llm` | critical | - |
| F-LAGEN-R003 | Toda la informacion poblada del contexto viaja al modelo | critical | - |
| F-LAGEN-R004 | Al modelo se le exige producir contenido en la DSL `quizSentence` con su traduccion al espanol | critical | - |
| F-LAGEN-R005 | Una respuesta es utilizable si y solo si entrega un objeto JSON con `quizSentence` y `translation` no vacios | critical | - |
| F-LAGEN-R006 | Toda falla de generacion se reporta con una categoria explicita en el `reason` | critical | La estrategia de propuesta 'lemma-absence-llm' no pudo generar un candidato de quiz para la tarea '{taskId}': {categoria} |
| F-LAGEN-R007 | Una falla de generacion no produce candidato parcial ni reintento automatico | critical | - |
| F-LAGEN-R008 | El operador puede ajustar el comportamiento de generacion sin recompilar el sistema | critical | - |
| F-LAGEN-R009 | El `StrategyId.providerId` identifica al proveedor concreto detras de cada propuesta | major | - |
| F-LAGEN-R010 | El sistema no valida la calidad funcional del candidato producido | minor | - |
| F-LAGEN-R011 | La generacion es no-deterministica por defecto | minor | - |
| F-LAGEN-R012 | El modo de generacion fija (canned) entrega siempre el mismo candidato predeterminado | minor | - |
| F-LAGEN-R013 | El operador puede elegir el modo de generacion; el default es dinamico y el modo fijo es opt-in explicito | critical | Modo de generacion no reconocido: '{modo}'. Valores admitidos: dinamico, fijo. |
| F-LAGEN-R014 | La configuracion del operador se valida atomicamente antes de ejecutar cualquier operacion de revision | major | Configuracion invalida en '{perilla}': {detalle}. Corregir antes de re-ejecutar. |
| F-LAGEN-R015 | La direccion de servicio se exige segun la clase del proveedor declarado, con el mismo criterio que la validacion de consigna | critical | No se pudo dirigir la consulta al proveedor '{proveedor}': falta '{dato}'. Declararlo, o pedir el modo de generacion fija para correr sin ningun proveedor. |

**User Journeys:**

- **F-LAGEN-J007**: La direccion de servicio se exige solo cuando el proveedor declarado la necesita

### FEAT-PIPRE: Preview de impacto de propuestas de revision sobre los scores de auditoria [F-PIPRE]

> **Que**: Al generar una `RevisionProposal`, el sistema computa y deja disponible un "preview de impacto" que describe como cambiarian los scores de auditoria del curso (a nivel del nodo afectado, sus contenedores y el curso completo) si esa propuesta se aprobara, discriminado por dimension de diagnostico, sin modificar el curso real.

**Por que**: Hoy la unica forma de saber si una propuesta mejora la auditoria es aprobarla y volver a auditar; el operador necesita ese feedback antes de decidir, especialmente en modo humano (FEAT-REVAPR), para distinguir propuestas que mueven la aguja de las que no.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-PIPRE-R001 | Cada `RevisionProposal` recien generada lleva asociado un preview de impacto | critical | - |
| F-PIPRE-R002 | El preview es estrictamente "what-if" y no modifica el curso ni emite un `AuditReport` | critical | - |
| F-PIPRE-R003 | El preview se computa contra el estado del curso al momento de generar la propuesta | critical | - |
| F-PIPRE-R004 | El preview reporta deltas a nivel del nodo afectado y de cada contenedor hasta el curso | critical | - |
| F-PIPRE-R005 | El preview discrimina por dimension de diagnostico | critical | - |
| F-PIPRE-R006 | Cada delta del preview lleva tres valores: antes, despues y diferencia | major | - |
| F-PIPRE-R007 | El preview se muestra al operador junto con la propuesta misma | major | - |
| F-PIPRE-R008 | El preview es de solo lectura una vez asociado a la propuesta | major | - |
| F-PIPRE-R013 | Los deltas se presentan al operador en notacion porcentual | major | - |
| F-PIPRE-R009 | Si el preview no se puede computar, el sistema lo declara explicitamente con causa | critical | Preview de impacto no disponible para la propuesta '<proposalId>': <causa> |
| F-PIPRE-R010 | Una falla al computar el preview no afecta la persistencia de la propuesta | critical | - |
| F-PIPRE-R011 | El preview se calcula propuesta por propuesta, no en combinaciones | major | - |
| F-PIPRE-R012 | El preview cubre solo cambios sobre nodos existentes | minor | - |

**User Journeys:**

- **F-PIPRE-J001**: El preview se computa y queda asociado al generar la propuesta

- **F-PIPRE-J002**: El operador inspecciona una propuesta y ve su preview

- **F-PIPRE-J003**: Una propuesta sobre un nodo que ya no existe queda con preview no disponible

### FEAT-RCLALEN: Conciencia de Longitud de Oracion en el Contexto de Correccion LEMMA_ABSENCE [F-RCLALEN]

> **Que**: Enriquece el contexto de correccion LEMMA_ABSENCE con la longitud actual de la oracion del quiz (tokens, rango esperado para el nivel CEFR, desviacion respecto al rango y direccion accionable de ajuste), leyendo el diagnostico de longitud que ya existe sobre el mismo nodo quiz.

**Por que**: Hoy el LLM que reescribe la oracion no sabe si esta en el limite alto o bajo del rango de tokens para su nivel; agregar esa senal evita que la correccion empuje la oracion fuera de rango y dispare en la siguiente auditoria una tarea SENTENCE_LENGTH redundante.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-RCLALEN-R001 | Campos de longitud en el contexto de correccion LEMMA_ABSENCE | high | - |
| F-RCLALEN-R002 | Calculo de lengthDirection a partir de delta | high | - |
| F-RCLALEN-R003 | Origen de datos: SentenceLengthDiagnosis del mismo nodo quiz | high | - |
| F-RCLALEN-R004 | Comportamiento cuando SentenceLengthDiagnosis esta ausente | major | - |
| F-RCLALEN-R005 | Extension del formato JSON con campos de longitud | high | - |
| F-RCLALEN-R006 | Extension del formato texto con campos de longitud | major | - |

**User Journeys:**

- **F-RCLALEN-J001**: LLM recibe contexto LEMMA_ABSENCE con senal de acortar para una oracion en limite alto

- **F-RCLALEN-J002**: LLM recibe contexto LEMMA_ABSENCE con senal de mantener longitud

### FEAT-CDIFF: Estructura consolidada de un analisis con sus propuestas aceptadas y pendientes [F-CDIFF]

> **Que**: El CLI de content-audit expone, para cada `AuditReport`, una **estructura consolidada** que combina el baseline del analisis con los efectos de las propuestas aceptadas y pendientes de su plan activo. Para cada nodo afectado del arbol del curso, la salida emite un **mapa de fields cambiados** donde cada entrada lleva tres "fotos" del mismo field a lo largo del stack `original` (baseline) → `consolidated` (con aceptadas) → `pendingProjection` (con aceptadas + pendientes). El conjunto de fields candidatos se descubre **dinamicamente** caminando recursivamente las estructuras del modelo de auditoria y del contenido del nodo: cualquier hoja escalar cuyo valor difiera entre al menos dos fotos aparece como field, sin enumerar tipos de fields posibles en el contrato.

**Por que**: Hoy la salida expone solo un snapshot crudo del nodo afectado y, por estadistica, escalares precomputados. Los diagnosticos tipados que el motor calcula sobre el arbol consolidado y el arbol con pendientes (token count, listas de lemas ausentes, distribucion COCA, etc.) se descartan antes de salir. La forma `field → tripleta` con descubrimiento dinamico corrige ese gap y deja el contrato **extensible sin tocarlo**: cuando aparece un analizador nuevo, un componente nuevo en un registro existente, o un campo nuevo en el contenido del nodo, sus hojas escalares empiezan a participar del diff automaticamente. La defensa contra ruido (timestamps, identificadores opacos, metadatos de persistencia) se hace por exclusiones declaradas a nivel funcional, no por whitelists positivas que requieran mantenimiento por feature.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-CDIFF-R001 | El par activo `(auditId, planId)` es observable en la raiz de la salida | critical | - |
| F-CDIFF-R002 | Cambiar el par activo es idempotente y no destructivo | major | - |
| F-CDIFF-R003 | Un analisis sin decisiones nuevas no marca nodos como afectados | critical | - |
| F-CDIFF-R004 | Las pendientes dormidas se recuperan al apuntar el par activo a su analisis original | critical | - |
| F-CDIFF-R005 | Las propuestas rechazadas no contribuyen a ninguna foto y no aparecen en trazabilidad | major | - |
| F-CDIFF-R006 | Cada nodo afectado expone un mapa de fields cambiados | critical | - |
| F-CDIFF-R007 | Invariantes de las tres fotos por field | critical | - |
| F-CDIFF-R019 | Cualquier hoja escalar con cambio entre fotos aparece como field | critical | - |
| F-CDIFF-R020 | Exclusiones por rol funcional, no por whitelist de campos | critical | - |
| F-CDIFF-R023 | Anidamiento: hojas profundas se identifican por path estable | critical | - |
| F-CDIFF-R022 | Listas: diff por elemento si tienen identidad declarada, posicional si no | major | - |
| F-CDIFF-R009 | La salida no tiene una seccion paralela de "estadisticas afectadas" | critical | - |
| F-CDIFF-R010 | Los deltas no son parte del contrato; son computables por el consumidor | major | - |
| F-CDIFF-R011 | Trazabilidad de propuestas por nodo afectado | major | - |
| F-CDIFF-R012 | Una pendiente no aplicable se marca, se identifica con causa y se excluye de pendingProjection | major | - |
| F-CDIFF-R013 | Si la estructura consolidada no se puede construir, el CLI emite `consolidatedAvailability: UNAVAILABLE` con causa | critical | Estructura consolidada no disponible para el analisis '<auditId>': <categoria> - <detalle> |
| F-CDIFF-R015 | Servir el consolidado no escribe ningun artefacto persistido | critical | - |
| F-CDIFF-R016 | El consolidado combina varias propuestas en un documento; los previews por-propuesta de FEAT-PIPRE permanecen accesibles | major | - |
| F-CDIFF-R017 | Ninguna foto contiene efecto de propuestas de planes ajenos | major | - |

**User Journeys:**

- **F-CDIFF-J001**: El CLI emite el consolidado del par activo

- **F-CDIFF-J002**: Nodo hoja con aceptada y pendiente sobre el mismo nodeId, tripleta completa por field

- **F-CDIFF-J003**: Padre con aceptadas y pendientes en su subarbol, descubrimiento dinamico de hojas y diff de listas

- **F-CDIFF-J004**: El contrato emite la tripleta y deja los deltas al consumidor

- **F-CDIFF-J005**: Pendiente no aplicable: la salida la marca y sigue sirviendo

- **F-CDIFF-J006**: Cambio de par activo: idempotente y no destructivo

- **F-CDIFF-J008**: Supersesion explicita del preview combinado de FEAT-PIPRE

### FEAT-PLANEF: Generacion de plan efimero sin persistencia en disco [F-PLANEF]

> **Que**: El comando `plan` admite un modo de invocacion (`--storage=none` sugerido por el operador, shape final a confirmar) que **no escribe ningun archivo en disco** y emite el plan generado por la salida estandar del CLI en formato JSON, con el mismo schema que un plan persistido. Opcionalmente, en ese mismo modo, cada tarea del plan emitido puede llevar **inline su contexto de correccion** (F-PLANEF-R002), evitando que el cliente tenga que pedirlo aparte sobre un plan que no existe en disco.

**Por que**: Mientras el operador del dashboard revisa varias tareas dentro del mismo plan, quiere ver en cada momento como se veria el plan **si las decisiones que ya tomo (aceptadas y pendientes) estuvieran aplicadas**, sin tener que aplicarlas literalmente todavia. Ese plan-proyectado se construye una y otra vez sobre la marcha, no tiene valor historico y no debe ensuciar el historial de planes que el operador conserva como artefactos del proyecto. El contexto de correccion inline cierra el ciclo: el cliente recibe en una sola pasada todo lo que el operador necesita ver para cada tarea proyectada, sin un round-trip adicional contra un plan que ni siquiera tiene id resoluble.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-PLANEF-R001 | Modo de invocacion del comando `plan` que no persiste y emite el plan por stdout | critical | - |
| F-PLANEF-R002 | El plan efimero puede emitir el contexto de correccion inline por tarea, opt-in | critical | - |

**User Journeys:**

- **F-PLANEF-J001**: Invocar `plan` en modo efimero entrega el plan por stdout y no escribe archivos

- **F-PLANEF-J002**: Invocar `plan` efimero con la opcion de contexto inline emite el plan con `correctionContext` por tarea

- **F-PLANEF-J003**: Invocar `plan` efimero **sin** la opcion de contexto preserva la salida actual

### FEAT-REVCTX: Override del contexto de correccion en el verbo revise [F-REVCTX]

> **Que**: El verbo `revise task <id>` acepta opcionalmente un **`correctionContext` provisto externamente por el cliente** mediante dos flags mutuamente excluyentes: `--correction-context=<json>` (literal en linea) y `--correction-context-file=<path>` (archivo). Cuando se provee, el sistema usa ese contexto como insumo de la estrategia de revision activa, en lugar del contexto que derivaria por si mismo del plan persistente y su `AuditReport` fuente. Cuando no se provee, el comportamiento es el actual y no cambia.

**Por que**: El operador del dashboard revisa decenas de tareas y, mientras decide, ve un **plan proyectado** (FEAT-PLANEF) que refleja el efecto acumulado de sus decisiones aceptadas y pendientes. Cuando finalmente aprueba una tarea, espera que la revision se ejecute sobre el contexto proyectado que efectivamente vio en la UI. Hoy `revise` deriva el contexto del plan persistido + `AuditReport` original, ignorando las pendientes; la consecuencia es que la estrategia (LLM en LEMMA_ABSENCE) puede proponer una palabra que ya consumio una propuesta previa pendiente -exactamente el problema que el plan proyectado prometia evitar visualmente. Permitir un override del contexto cierra el ciclo entre lo que el operador ve y lo que el sistema ejecuta.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-REVCTX-R001 | El verbo `revise` acepta un `correctionContext` provisto externamente, opcional | critical | - |
| F-REVCTX-R002 | Cuando se provee override, el sistema **no** deriva contexto por su cuenta para la tarea revisada | critical | - |
| F-REVCTX-R003 | El override debe respetar el contrato del `correctionContext` y declarar la misma identidad logica que la tarea revisada | critical | - |
| F-REVCTX-R004 | El override no aplica a `DiagnosisKind` que no tienen contexto definido | major | El verbo revise no acepta correctionContext para tareas de tipo '{diagnosisKind}' |
| F-REVCTX-R005 | El plan resoluble por `revise` puede ser distinto del plan que el cliente proyecto | critical | - |
| F-REVCTX-R006 | El override no altera los modos de aprobacion ni la persistencia del artefacto | major | - |
| F-REVCTX-R007 | El artefacto persiste el origen del contexto y un snapshot literal del override | critical | - |
| F-REVCTX-R008 | El payload del override es JSON con la misma forma observable que la salida de `get task` | critical | - |

**User Journeys:**

- **F-REVCTX-J001**: El operador aprueba una tarea sobre el contexto proyectado y `revise` consume el override

- **F-REVCTX-J002**: El cliente invoca `revise` con override sobre una tarea de un kind que no tiene contrato de contexto

- **F-REVCTX-J003**: El cliente invoca `revise` **sin** override y el comportamiento es el actual

- **F-REVCTX-J004**: El cliente invoca `revise` con los dos flags de override a la vez

### FEAT-LEMMA-SUGGESTIONS: LemmaCount como variable de SuggestedLemmas [F-SLEM]

> Agregar la señal de `lemma-count` a `correctionContext.suggestedLemmas` en tareas `LEMMA_ABSENCE`, ordenando por un esquema de **seis tiers de prioridad** (first-match-wins) donde la pertenencia a una **banda de frecuencia COCA deficitaria del nivel** es clave secundaria fuerte dentro de la macro-escasez, por encima de la distinción sub-expuesto/ausente (F-SLEM-R003, F-SLEM-R014).  
El estado de exposición se corta con el umbral `N` de `lemma-count` (sub-expuesto = `1..N-1`, ausente = sin exposición, expuesto-pero-escaso = `N..2N-1`), nunca con literales; COCA ascendente es el desempate final dentro de cada tier (F-SLEM-R005). Si la auditoría no analizó bandas, los tiers de banda quedan vacíos y el orden degrada sin fallar (F-SLEM-R015).  
La funcionalidad se observa mediante `AuditRunner.runAudit(...)`, `AuditRunner.runDetailedAudit(...)`, `PlanCommand.plan(..., true)`, `GetCommand.get(...)` y `ReviseCommand.revise(...)`, sin introducir un comando público nuevo ni nuevos campos observables por lema.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-SLEM-R001 | La auditoría con lemma-count provee la señal funcional para suggestedLemmas | critical | No se pudo usar la señal de lemma-count disponible en la auditoría para enriquecer suggestedLemmas |
| F-SLEM-R002 | Plan con correctionContext enriquece suggestedLemmas para LEMMA_ABSENCE | critical | Las sugerencias de lemas no incorporan la señal disponible de lemma-count |
| F-SLEM-R003 | Ranking por seis tiers de prioridad con banda deficitaria como clave secundaria fuerte | critical | El ranking de suggestedLemmas no respeta los seis tiers de prioridad acordados con lemma-count |
| F-SLEM-R004 | APPEARS_TOO_EARLY se evalúa en el nivel target real | critical | APPEARS_TOO_EARLY fue evaluado fuera de su nivel target real |
| F-SLEM-R005 | COCA ascendente es el desempate final dentro de un mismo tier | major | El desempate de suggestedLemmas no respeta COCA ascendente como desempate final dentro del tier |
| F-SLEM-R006 | El límite existente de suggestedLemmas se aplica después del nuevo ranking | major | El límite de suggestedLemmas no se aplicó después del ranking con lemma-count |
| F-SLEM-R007 | COMPLETELY_ABSENT queda detrás del sub-expuesto solo dentro del mismo estado de banda | critical | COMPLETELY_ABSENT fue ordenado incorrectamente respecto de un lema sub-expuesto dado su estado de banda deficitaria |
| F-SLEM-R008 | Plan proyectado y consulta de tareas muestran el mismo correctionContext enriquecido | major | El correctionContext observado en el plan no coincide con el observado en la consulta de tareas |
| F-SLEM-R009 | Revise consume el correctionContext enriquecido derivado por el sistema | major | La revisión no recibió el correctionContext enriquecido con lemma-count |
| F-SLEM-R010 | Revise acepta override externo válido con la forma observable enriquecida | major | El correctionContext externo no coincide con la forma válida de suggestedLemmas enriquecido |
| F-SLEM-R011 | Revise rechaza override externo con señal de lemma-count inconsistente | major | El correctionContext es inválido por inconsistencia en la señal de lemma-count dentro de suggestedLemmas |
| F-SLEM-R012 | Auditoría sin lemma-count conserva suggestedLemmas sin fallar | major | La ausencia de lemma-count impidió generar o consultar suggestedLemmas |
| F-SLEM-R013 | Forma observable de la señal de lemma-count dentro de suggestedLemmas | critical | La forma observable de lemma-count dentro de suggestedLemmas no respeta el contrato acordado |
| F-SLEM-R014 | Definición formal de los seis tiers: estado de exposición, banda deficitaria y COCA ascendente | major | El cómputo de tiers no aplicó el estado de exposición por N, la pertenencia binaria a banda deficitaria y COCA ascendente en el orden acordado |
| F-SLEM-R015 | Degradación elegante: los tiers de banda quedan vacíos, sin branch condicional | major | La ausencia del análisis de bandas de frecuencia COCA impidió construir suggestedLemmas con la función de prioridad degradada a 3-4-6 |

**User Journeys:**

- **F-SLEM-J001**: Generar plan con suggestedLemmas enriquecido

- **F-SLEM-J002**: Consultar tareas con paridad respecto del plan

- **F-SLEM-J003**: Revisar usando correctionContext derivado por el sistema

- **F-SLEM-J004**: Revisar usando correctionContext externo enriquecido

### FEAT-DBSENT: Plain sentences pre-computadas desde la fuente de datos [F-DBSENT]

> **Que**: El campo `sentences: List<String>` de `QuizTemplateEntity.form` ahora se carga **directamente desde la fuente de datos** del curso (JSON de quiz templates) en lugar de derivarse en runtime a partir de la estructura `sentenceParts`. El `CourseToAuditableMapper` consume esa lista pre-computada y la propaga a `AuditableQuiz.sentences` sin reinvocar `QuizSentenceConverter.toPlainSentences`. El converter sigue existiendo y sus contratos no cambian; deja de ser consumidor productivo dentro del flujo del audit.

**Por que**: La derivacion runtime via concatenacion `TEXT + variante canonica de CLOZE` modela correctamente el patron "cloze inline" pero falla para el patron "transformacion", donde el TEXT es scaffolding pedagogico (ej. `"The dogs / bark / loudly."`) y el CLOZE contiene la oracion completa resuelta. En esos casos el output combinaba scaffolding + resolucion (ej. `"The dogs / bark / loudly. The dogs are barking loudly."`), inflando el conteo de tokens y degradando los scores de `SentenceLengthAnalyzer` para niveles bajos. El issue afecta aproximadamente 3.976 de 11.487 quizzes (~35%). La discriminacion entre patron A (cloze inline) y patron B (transformacion) se resolvio out-of-band con una heuristica sobre los datos persistidos (B si existe exactamente un CLOZE cuya variante canonica empieza con mayuscula, termina en puntuacion final y es multi-palabra), y los datos ya estan pre-computados en la fuente. Mover la fuente de verdad de las plain sentences a la DB elimina el codigo de derivacion del path del audit y se apoya en un campo que ya esta resuelto correctamente upstream.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-DBSENT-R001 | QuizTemplateEntity persiste la lista de plain sentences | critical | - |
| F-DBSENT-R002 | El audit consume las plain sentences pre-computadas | critical | - |
| F-DBSENT-R003 | El converter sigue siendo contrato disponible pero no consumido por el audit | medium | - |
| F-DBSENT-R004 | Persistir un curso preserva las plain sentences de cada quiz (round-trip idempotente) | critical | - |

### FEAT-CSLATDC: Consulta de Suggested Lemmas a través del CLI [F-CSLATDC]

> Se amplía `GetCommand.get(...)` para consultar `Suggested Lemmas` bajo demanda durante la revisión de una `Task/Quiz`.  
La consulta se identifica por `taskId`, permite pedir un `limit`, filtrar por `Part of Speech` y `Level`, y siempre devuelve resultados priorizados.  
El cambio complementa los `suggestedLemmas` estáticos del `correctionContext` sin crear un nuevo verbo CLI.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-CSLATDC-R001 | Exposición pública de suggested lemmas por task | critical | No aplica. |
| F-CSLATDC-R002 | Uso del verbo existente get | major | No aplica. |
| F-CSLATDC-R003 | Filtrado obligatorio por level | critical | No se pueden sugerir lemas sin un nivel aplicable para filtrar. |
| F-CSLATDC-R004 | Inferencia de level desde la task | critical | No aplica. |
| F-CSLATDC-R005 | Validación cuando no hay level disponible | critical | No se pueden sugerir lemas porque falta el nivel necesario para filtrar. |
| F-CSLATDC-R006 | Uso de level explícito cuando no puede inferirse | major | No aplica. |
| F-CSLATDC-R007 | Filtro por partOfSpeech | major | No aplica. |
| F-CSLATDC-R008 | Límite máximo solicitado | major | No aplica. |
| F-CSLATDC-R009 | Lista parcial sin error | major | No aplica. |
| F-CSLATDC-R010 | Ordenamiento por prioridad | critical | No aplica. |
| F-CSLATDC-R011 | Consulta no limitada al top estático del correctionContext | major | No aplica. |
| F-CSLATDC-R012 | Tasks sin suggested lemmas aplicables | major | No hay lemas sugeridos disponibles para esta task. |
| F-CSLATDC-R014 | Resultado mínimo observable | major | No aplica. |
| F-CSLATDC-R015 | La consulta dinámica opera sobre el MISMO análisis fuente del que se derivó el contexto de la task | critical | No aplica. |

**User Journeys:**

- **F-CSLATDC-J001**: El agente pide lemas sugeridos durante la revisión

- **F-CSLATDC-J002**: El agente informa level explícito cuando no puede inferirse

- **F-CSLATDC-J004**: La consulta dinámica supera el conjunto estático del correctionContext

- **F-CSLATDC-J005**: La consulta dinámica refleja el análisis fuente de la task, no el más reciente

### FEAT-LAGAG: Generación interactiva con consulta de suggested lemmas [F-LAGAG]

> **Qué**: Un modo de generación de candidato de quiz para tareas `LEMMA_ABSENCE`
en el que el modelo arranca con una semilla de los suggested lemmas más
prioritarios y, durante la generación, puede solicitar más suggested lemmas
para la misma task hasta cerrar con un único candidato final.

**Por qué**: Hoy la generación dinámica recibe una lista estática horneada de
antemano; cuando esa lista no alcanza, el modelo no tiene forma de pedir las
palabras que necesita. Este modo le da esa capacidad de manera acotada y
trazable.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-LAGAG-R001 | Semilla inicial con los lemmas más prioritarios | high | - |
| F-LAGAG-R002 | El modelo puede solicitar más suggested lemmas | high | - |
| F-LAGAG-R003 | Modo explícito, reconocible y trazable | high | - |
| F-LAGAG-R004 | Presupuesto de solicitudes acotado | high | Se agotó el presupuesto de consultas de suggested lemmas sin obtener un candidato |
| F-LAGAG-R005 | Proveedor incapaz de sostener el intercambio | medium | El proveedor activo no puede sostener la consulta interactiva de suggested lemmas |
| F-LAGAG-R006 | Candidato final con la forma heredada | high | - |

### FEAT-INCEXP: Incluir lemas ya expuestos en la consulta de Suggested Lemmas [F-INCEXP]

> **Qué**: Agrega una opción booleana al filtro de `GetCommand.get("suggested-lemmas", <taskId>, ...)` que, cuando se activa, amplía el universo de candidatos para incluir también los lemas del `Level` y `Part of Speech` pedidos que ya alcanzaron su umbral de exposición.

**Por qué**: Con la consulta por defecto, pedir un `Part of Speech` específico en un `Level` puede traer muy pocos resultados porque sólo se recomiendan lemas que todavía necesitan exposición; la opción permite al agente revisor disponer de más candidatos cuando los pocos sub-expuestos no alcanzan.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-INCEXP-R001 | Comportamiento por defecto inalterado | critical | No aplica. |
| F-INCEXP-R002 | La opción amplía el universo a los lemas ya expuestos | critical | No aplica. |
| F-INCEXP-R003 | El orden se mantiene por prioridad de recomendación | critical | No aplica. |
| F-INCEXP-R004 | El desempate sigue siendo frecuencia COCA ascendente | major | No aplica. |
| F-INCEXP-R005 | El filtro obligatorio por level sigue aplicando | critical | No se pueden sugerir lemas sin un nivel aplicable para filtrar. |
| F-INCEXP-R006 | El filtro por partOfSpeech sigue aplicando | major | No aplica. |
| F-INCEXP-R007 | El limit sigue recortando el resultado final | major | No aplica. |
| F-INCEXP-R008 | La inferencia y el level explícito siguen funcionando con la opción activada | major | No se pueden sugerir lemas porque falta el nivel necesario para filtrar. |
| F-INCEXP-R009 | Respuesta vacía sin error cuando no hay lemas del tipo y nivel pedidos | major | No hay lemas sugeridos disponibles para esta task. |
| F-INCEXP-R010 | Lista parcial sin error sigue valiendo | major | No aplica. |
| F-INCEXP-R011 | La forma observable de cada lema sugerido se mantiene | major | No aplica. |

**User Journeys:**

- **F-INCEXP-J001**: El agente amplía la consulta para incluir lemas ya expuestos

- **F-INCEXP-J002**: Contraste entre la consulta por defecto y la consulta con la opción

### FEAT-LASAG: Agente sentinel-agent para generación de candidatos de lemma-absence [F-LASAG]

> Reemplazar la ruta LLM por defecto de *lemma-absence* (single-shot FEAT-LAGEN + interactivo FEAT-LAGAG) por un **único agente sobre sentinel-agent**, que pasa a ser la única ruta LLM. Contrato externo idéntico: entra el `CorrectionContext` de FEAT-RCLA, sale **un único** `QuizCandidate {quizSentence, translation}`. El agente ejecuta un **react corto con UNA sola tool** (la consulta read-only de suggested-lemmas) para filtrar `suggestedWords`. Timeouts/errores del framework se mapean a falla de estrategia preservando categoría, **sin reintentos**.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-LASAG-R001 | El filtrado de suggestedWords pasa por la tool de suggested-lemmas | high | - |
| F-LASAG-R002 | Ningún otro verbo CLI durante la generación | high | - |
| F-LASAG-R003 | Contrato externo idéntico: un único QuizCandidate | high | - |
| F-LASAG-R004 | Presupuesto de tool por run; agotarlo no es falla | medium | - |
| F-LASAG-R005 | Error de runtime en generación → ProposalStrategyFailedException con categoría preservada | high | - |
| F-LASAG-R006 | Un único intento de generación ante falla de runtime | high | - |
| F-LASAG-R007 | Catálogo y clasificación de evals de calidad sobre el candidato emitido | high | - |
| F-LASAG-R008 | Reintento de generación por veredicto bloqueante, acotado por maxEvalRetries | high | - |
| F-LASAG-R009 | Exhaustion de reintentos de eval: best-effort, no es falla | high | - |
| F-LASAG-R010 | Carry-forward de contexto de fallo e historial de tool entre reintentos | medium | - |
| F-LASAG-R011 | Perilla maxEvalRetries con default 3, ignorada por CANNED | medium | - |
| F-LASAG-R012 | Error de runtime en cualquier fase = falla inmediata fuera del lazo de retry | high | - |
| F-LASAG-R013 | Las palabras fuera de catálogo alimentan los insumos de generación | high | - |
| F-LASAG-R014 | Edición mínima: las palabras objetivo son la unión de mal ubicadas y fuera de catálogo | high | - |
| F-LASAG-R015 | Una tarea sin lemas mal ubicados pero con palabras fuera de catálogo es generación válida | high | - |

**User Journeys:**

- **F-LASAG-J001**: Generación exitosa de QuizCandidate por la ruta LLM (agente sentinel-agent)

- **F-LASAG-J002**: Agotar el presupuesto de tool no es falla

- **F-LASAG-J003**: Error de runtime en generación falla con categoría preservada e intento único

- **F-LASAG-J004**: Veredicto bloqueante negativo dispara reintento con carry-forward, acotado por maxEvalRetries

- **F-LASAG-J005**: Exhaustion de reintentos de eval es best-effort, no falla

- **F-LASAG-J006**: Veredicto deseable negativo no bloquea la emisión

- **F-LASAG-J007**: Error de runtime en la fase de evals falla de inmediato fuera del lazo de retry

- **F-LASAG-J009**: Perilla maxEvalRetries con default 3, ignorada por CANNED

### FEAT-SMODE: Modo de frase del knowledge y frase canonica puntuable [F-SMODE]

> **Que**: Cada knowledge basado en sentencias declara un **modo de frase**
(`REWRITE` o `FILL`) que determina cual es la **frase canonica puntuable** de
sus quizzes: en `FILL` es la oracion completa con el hueco relleno; en
`REWRITE` es solo la oracion respuesta, sin la oracion fuente/prompt.

**Por que**: Hoy la frase canonica se arma concatenando todas las partes del
quiz, lo que en ejercicios de reescritura incluye erroneamente la oracion
fuente, duplica contenido, infla el conteo de tokens y degrada el score de
longitud de frase (F-SLEN-R002). El modo de frase desambigua que se
puntua y mantiene el score estable a traves de las revisiones.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-SMODE-R001 | Todo knowledge basado en sentencias tiene un modo de frase | critical | El knowledge basado en sentencias '{knowledgeId}' no declara un modo de frase valido (REWRITE o FILL) |
| F-SMODE-R002 | El modo de frase es uniforme dentro del knowledge | major | - |
| F-SMODE-R003 | La frase canonica puntuable se determina segun el modo del knowledge | critical | No se pudo determinar la frase canonica puntuable del quiz '{quizId}' para el modo {modo} de su knowledge '{knowledgeId}' |
| F-SMODE-R004 | En REWRITE la oracion fuente nunca se incluye ni se puntua | critical | La frase canonica del quiz '{quizId}' (modo REWRITE) incluyo la oracion fuente; debe contener solo la respuesta |
| F-SMODE-R005 | El score de longitud de frase se calcula sobre la frase canonica determinada por el modo | critical | - |
| F-SMODE-R006 | La revision que preserva la estructura conserva el modo del knowledge | critical | El quiz revisado '{quizId}' se evaluo con un modo de frase distinto al de su knowledge '{knowledgeId}' |
| F-SMODE-R007 | Una revision REWRITE que conserva la longitud de la respuesta no degrada el score de longitud | critical | La revision REWRITE del quiz '{quizId}' degrado el score de longitud sin cambiar la longitud de la respuesta |

**User Journeys:**

- **F-SMODE-J001**: Puntuar la longitud de frase de un quiz segun el modo de su knowledge

- **F-SMODE-J002**: Revisar un quiz REWRITE sin degradar el score de longitud

### FEAT-SLREGEX: Filtrar Suggested Lemmas por expresión regular [F-SLREGEX]

> **Qué**: Agrega un filtro `--regex` a `GetCommand.get("suggested-lemmas", <taskId>, ...)` que, cuando se informa, conserva únicamente los lemas candidatos cuyo texto coincide con el patrón, manteniendo el resto de los filtros y el orden por prioridad intactos.

**Por qué**: En varios casos el agente revisor necesita buscar tipos de palabra específicos por su forma superficial (terminaciones, prefijos, secuencias de letras) y hoy no hay forma de filtrar por el texto del lema; el filtro textual permite acotar la lista a esos casos sin pedir un tipo gramatical entero.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-SLREGEX-R001 | Comportamiento por defecto inalterado | critical | No aplica. |
| F-SLREGEX-R002 | El patrón matchea contra el texto del lema | critical | No aplica. |
| F-SLREGEX-R003 | Coincidencia parcial estilo grep | critical | No aplica. |
| F-SLREGEX-R004 | Coincidencia insensible a mayúsculas y minúsculas | major | No aplica. |
| F-SLREGEX-R005 | El filtro es aditivo y no relaja los demás filtros | critical | No aplica. |
| F-SLREGEX-R006 | El orden por prioridad se preserva | critical | No aplica. |
| F-SLREGEX-R007 | El limit recorta después de aplicar el regex | major | No aplica. |
| F-SLREGEX-R008 | Patrón inválido rechaza la consulta con validación clara | critical | El patrón de búsqueda informado en --regex no es válido. |
| F-SLREGEX-R009 | Sin coincidencias devuelve lista vacía sin error | major | Ningún lema sugerido coincide con el patrón informado en --regex. |
| F-SLREGEX-R010 | La ayuda del comando documenta la semántica del filtro | major | No aplica. |

**User Journeys:**

- **F-SLREGEX-J001**: El agente filtra los lemas sugeridos por patrón de texto

- **F-SLREGEX-J002**: Contraste entre la consulta por defecto y la consulta con --regex

### FEAT-KTLR: Revision de longitud de titulo de knowledge con propuesta del agente [F-KTLR]

> **Que**: Cierra el puente revise→propuesta→aprobar para tareas de diagnostico
`KNOWLEDGE_TITLE_LENGTH`: el sistema resuelve el contexto del knowledge, invoca
al agente de titulos, transforma su salida (nuevo titulo + instructions) en una
propuesta revisable y, al aprobarla, **persiste el nuevo titulo e instructions
en el knowledge** del curso.

**Por que**: Hoy `plan` ya emite estas tareas y el agente ya existe, pero al
aprobar una propuesta el sistema solo sabe escribir cambios sobre quizzes; una
correccion de titulo de knowledge nunca llega al curso. Esta feature elimina ese
bloqueo sin tocar el flujo de revision de quizzes ya existente.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-KTLR-R001 | Las tareas KNOWLEDGE_TITLE_LENGTH resuelven el contexto del knowledge | high | - |
| F-KTLR-R002 | El contexto expone los datos que el agente necesita | high | - |
| F-KTLR-R003 | La revision de KNOWLEDGE_TITLE_LENGTH invoca al agente de titulos | high | - |
| F-KTLR-R004 | La salida del agente es un unico candidato de titulo por revision | high | - |
| F-KTLR-R005 | El candidato se transforma en una propuesta revisable | high | - |
| F-KTLR-R006 | Aprobar una correccion de titulo escribe el knowledge en el curso | critical | - |
| F-KTLR-R007 | Alcance acotado de la correccion de titulo | critical | - |
| F-KTLR-R008 | El flujo de revision de quizzes queda intacto | critical | - |
| F-KTLR-R009 | Rechazar deja el knowledge sin cambios | high | - |
| F-KTLR-R010 | Si el agente no produce un candidato valido, la revision aborta sin tocar el curso | high | El agente de titulos no pudo generar una correccion para el
knowledge '{knowledgeId}' |
| F-KTLR-R011 | Si el knowledge senalado no existe, la revision falla explicitamente | medium | El knowledge '{knowledgeId}' referido por la tarea no existe en el
curso |

**User Journeys:**

- **F-KTLR-J001**: Revisar y aprobar una correccion de titulo de knowledge

- **F-KTLR-J002**: La revision de quizzes sigue intacta

### FEAT-CLEX: Consulta lexica de oracion por CLI [F-CLEX]

> **Que**: Agrega una consulta de **solo lectura** del CLI que, dada una
Oración de quiz (misma DSL que la consulta de tokens) y
un Nivel CEFR, devuelve en JSON las palabras que el analisis de
mal-ubicacion **flaggearia** para ese nivel: mal ubicadas para el nivel y fuera
de catalogo penalizantes.

**Por que**: El eval de no-regresion lexica del agente de lemma-absence
(criterio #7 de F-LASAG-R007)
necesita verificar candidatos con el **mismo** analisis lexico del audit, no una
replica aproximada; hoy ese analisis solo corre sobre un curso persistido al
auditarlo, nunca sobre una oracion suelta.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-CLEX-R001 | Nueva consulta lexica de oracion, de solo lectura | critical | No aplica. |
| F-CLEX-R002 | Entrada: la DSL de quizSentence, incluido el modo de frase | major | No aplica. |
| F-CLEX-R003 | Salida: cada palabra mal ubicada | major | No aplica. |
| F-CLEX-R004 | Salida: cada palabra fuera de catalogo penalizante | major | No aplica. |
| F-CLEX-R005 | La lista contiene solo flags; vacia significa oracion limpia | critical | No aplica. |
| F-CLEX-R006 | Paridad exacta con el analisis del audit | critical | No aplica. |
| F-CLEX-R007 | Errores: DSL invalida y nivel invalido | major | quizSentence invalida: no respeta la DSL. |

**User Journeys:**

- **F-CLEX-J001**: El agente consulta los flags lexicos de un candidato

### FEAT-RPRES: Preservacion del contenido no revisado al aplicar una revision [F-RPRES]

> **Que**: Aplicar una revision a un elemento del curso cambia **solo** lo que la
revision propone corregir; todo lo demas del elemento sobrevive intacto al ciclo
de guardado y recarga, incluidos los valores legitimamente vacios.

**Por que**: Hoy los atributos del ejercicio que la propuesta no describe quedan
en valores por defecto (2.658 quizzes revisados degradados), y eso deja el
contenido inutilizable para la aplicacion que lo sirve a los alumnos.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-RPRES-R001 | Preservacion por defecto: solo cambia lo que la revision propone | critical | La revision del elemento '{elementId}' modifico atributos que no eran objeto de la correccion: {atributos} |
| F-RPRES-R002 | Vacio no es ausente, y un valor propio no es un valor por defecto | critical | El atributo '{atributo}' del elemento '{elementId}' paso de '{estadoOriginal}' a '{estadoResultante}' al aplicar la revision |
| F-RPRES-R003 | Revisar un elemento no altera ningun otro elemento del curso | critical | La revision del elemento '{elementId}' modifico {cantidad} elementos ajenos a la correccion |
| F-RPRES-R004 | Corregir la etiqueta de un knowledge alinea el titulo de sus ejercicios | critical | La correccion de la etiqueta del knowledge '{knowledgeId}' dejo {cantidad} ejercicios con el titulo desalineado |
| F-RPRES-R005 | El contenido ya degradado se repara, una vez vigente la preservacion | critical | Quedan {cantidad} elementos del curso con atributos degradados tras la reparacion |
| F-RPRES-R006 | Una parte nueva del enunciado nace completa, no por defecto | critical | La correccion del ejercicio '{quizId}' agrego {cantidad} partes al enunciado que no estan en el estado que corresponde a su clase: {detalle} |

**User Journeys:**

- **F-RPRES-J001**: Aplicar una revision conservando el contenido no revisado

- **F-RPRES-J002**: Aplicar una revision no altera el resto del curso

- **F-RPRES-J003**: El titulo de los ejercicios sigue a la etiqueta, y solo a ella

### FEAT-QINST: Analisis de cumplimiento de la consigna del quiz [F-QINST]

> **Que**: Un analisis nuevo de la auditoria puntua cada ejercicio segun si cumple
literalmente la consigna que se le da al alumno, apoyandose en un juez de
consigna ya existente y reutilizando los veredictos ya emitidos.

**Por que**: Hoy la auditoria mide longitud y vocabulario, pero nada verifica que
el ejercicio haga lo que su consigna promete; hay ejercicios irresolubles o que
contradicen su propia instruccion y ningun analisis los detecta.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-QINST-R001 | El analisis de consigna corre en toda auditoria, salvo exclusion explicita | critical | - |
| F-QINST-R002 | Del veredicto al puntaje: la severidad define la escala | critical | Puntaje fuera del rango [0,0 - 1,0] para el ejercicio '{quizId}' del knowledge '{knowledgeId}' |
| F-QINST-R003 | Cada ejercicio evaluado deja un diagnostico inspeccionable | critical | El ejercicio '{quizId}' fue evaluado pero no registro diagnostico de consigna |
| F-QINST-R004 | Un ejercicio no evaluado no recibe puntaje ni diagnostico | critical | El ejercicio '{quizId}' no fue evaluado y sin embargo recibio puntaje de consigna |
| F-QINST-R005 | El informe declara la cobertura del analisis y de que versiones del juez vienen sus veredictos | critical | La cobertura declarada del analisis de consigna no cierra: {conVeredicto} + {pendientes} + {fallidos} != {alcanzados} |
| F-QINST-R006 | Cada corrida acota cuantas consultas nuevas hace al juez | critical | - |
| F-QINST-R007 | Una falla del juez no aborta la auditoria ni corrompe lo ya guardado | critical | La auditoria se interrumpio por una falla del juez de consigna |
| F-QINST-R008 | Una corrida no vuelve a consultar por lo ya evaluado | critical | Se consulto al juez por el ejercicio '{quizId}', que ya tenia veredicto registrado |
| F-QINST-R009 | La identidad del veredicto es el contenido juzgado, no el ejercicio | critical | Se reutilizo el veredicto del ejercicio '{quizId}' pese a que su contenido juzgado cambio |
| F-QINST-R010 | La version del juez se registra y se declara; cambiarla no invalida ningun veredicto | critical | El cambio de version del juez de consigna elimino {cantidad} veredictos anteriores |
| F-QINST-R011 | El usuario puede excluir el analisis de una corrida | major | - |
| F-QINST-R012 | La re-evaluacion es explicita, acotable, y el unico modo de rehacer un veredicto | major | - |
| F-QINST-R013 | Un veredicto de infraestructura del juez es una falla, no un juicio | critical | Se registro como veredicto una respuesta de infraestructura del juez para el ejercicio '{quizId}' |
| F-QINST-R014 | Alcance: todo ejercicio del curso, tenga o no restricciones explicitas | major | - |
| F-QINST-R015 | El analisis tiene un solo nombre: el que se lee es el que se escribe | critical | Se pidio '{opcion}' para el analisis '{nombre}' y la corrida lo ignoro: el informe publica ese analisis con ese mismo nombre |
| F-QINST-R016 | El juez admite todo proveedor que el sistema sepa consultar; "invalida" se juzga contra el proveedor declarado | critical | El juez de consigna se declaro no disponible por falta de '{dato}', que el proveedor '{proveedor}' no utiliza |
| F-QINST-R017 | El incumplimiento detectado se convierte en trabajo: cada ejercicio que incumple entra al plan como tarea | critical | El informe declara {n} ejercicios con incumplimiento de consigna y el plan generado a partir de el no contiene ninguna tarea de ese tipo |
| F-QINST-R018 | La re-evaluacion se puede acotar a un conjunto de ejercicios declarado en el pedido | major | El pedido de re-evaluacion declara un conjunto vacio de ejercicios |
| F-QINST-R019 | El conjunto se enumera en el pedido o se lee de un origen externo; un origen ilegible no es un conjunto vacio | major | No se pudo leer el origen '{origen}' declarado para el conjunto de ejercicios a re-evaluar |

**User Journeys:**

- **F-QINST-J001**: Puntuar un ejercicio a partir del veredicto del juez

- **F-QINST-J002**: Auditar con cobertura parcial y reanudar en la corrida siguiente

- **F-QINST-J003**: Ejecucion por omision, exclusion y re-evaluacion explicita

- **F-QINST-J004**: Re-evaluar un conjunto de ejercicios declarado en el pedido

### FEAT-EVCOST: Resultados de evaluacion costosa reutilizables entre analisis [F-EVCOST]

> **Que**: El sistema registra el resultado de cada evaluacion costosa bajo la
huella del contenido evaluado, lo reutiliza en analisis posteriores y lo
conserva aunque la corrida se interrumpa.

**Por que**: Sin esto, cada analisis que se apoya en un evaluador lento y pagado
lo vuelve a pagar entero, y una caida a mitad de camino pierde todo el trabajo.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-EVCOST-R001 | La identidad de un resultado es evaluador + huella del contenido; la version es procedencia | critical | El resultado registrado para el evaluador '{evaluador}' no corresponde al contenido consultado |
| F-EVCOST-R002 | La huella se deriva de todo el contenido entregado al evaluador, y de nada mas | critical | La huella del contenido para el evaluador '{evaluador}' no es reproducible entre corridas |
| F-EVCOST-R003 | Antes de evaluar se consulta el resultado ya registrado | critical | - |
| F-EVCOST-R004 | Cada resultado queda disponible apenas se obtiene | critical | El registro de resultados del evaluador '{evaluador}' contiene una entrada incompleta |
| F-EVCOST-R005 | Solo se registra el resultado definitivo; la falla transitoria no se registra | critical | Se registro como resultado una falla del evaluador '{evaluador}' sobre el contenido {huella} |
| F-EVCOST-R006 | Conviven resultados de distintas versiones del evaluador; todos se reutilizan y nada se borra solo | critical | El cambio de version del evaluador '{evaluador}' elimino {cantidad} resultados de la version anterior |
| F-EVCOST-R007 | El registro es estado propio: sobrevive a los informes y se comparte entre analisis | critical | Los resultados registrados del evaluador '{evaluador}' se perdieron al eliminar informes de analisis |
| F-EVCOST-R008 | La re-evaluacion explicita es el unico modo de rehacer un resultado | critical | Se re-evaluo contenido con resultado registrado sin pedido explicito del usuario |
| F-EVCOST-R009 | Ante una falla, la corrida distingue si fallo ese contenido o fallo el servicio | critical | La corrida siguio consultando al evaluador '{evaluador}' tras una falla
del servicio |

**User Journeys:**

- **F-EVCOST-J001**: Reutilizar, o no, un resultado ya registrado

- **F-EVCOST-J002**: Una interrupcion o una falla no pierden el trabajo hecho

- **F-EVCOST-J003**: Los resultados sobreviven a los informes y solo se rehacen a pedido

### FEAT-QICOR: Correccion del incumplimiento de consigna de un ejercicio [F-QICOR]

> **Que**: Una tarea de incumplimiento de consigna se convierte en una correccion
propuesta del ejercicio —minima, dirigida por la violacion y que cumple la
consigna— revisable y aprobable con el mismo flujo que las demas.

**Por que**: Hoy el incumplimiento se detecta, se puntua y llega al plan como
tarea, pero ahi se detiene: no hay forma de pedirle al sistema que proponga como
arreglarlo, y el operador queda con una lista de problemas sin una sola
correccion que aprobar.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-QICOR-R001 | Una tarea de incumplimiento de consigna produce una correccion propuesta y revisable | critical | La revision de la tarea de incumplimiento de consigna del ejercicio '{quizId}' no produjo ninguna correccion |
| F-QICOR-R002 | La correccion se prepara sobre las violaciones señaladas, con su evidencia | critical | La correccion del ejercicio '{quizId}' se preparo sin las violaciones que registro su diagnostico de consigna |
| F-QICOR-R003 | Corregir no es reescribir: el cambio se limita a lo que la violacion señala | critical | La correccion del ejercicio '{quizId}' modifico contenido que ninguna violacion de su consigna señala |
| F-QICOR-R004 | No empeorar: el catalogo de criterios con el que se juzga al ejercicio corregido | critical | El ejercicio corregido de '{quizId}' se propuso sin haberse sometido al criterio '{criterio}' |
| F-QICOR-R005 | El resultado cumple la consigna, verificado con el mismo criterio que detecto el incumplimiento | critical | Se propuso para el ejercicio '{quizId}' una correccion que sigue incumpliendo su consigna |
| F-QICOR-R006 | No lograr una correccion queda para el unico caso en que no hay nada que entregar | critical | No se logro una correccion aceptable para el ejercicio '{quizId}': ningun candidato resulto entregable ({criterios}) |
| F-QICOR-R007 | Aprobar deja el ejercicio corregido en el curso; rechazar no cambia nada | critical | Se aprobo la correccion de consigna del ejercicio '{quizId}' y el curso conserva su contenido anterior |
| F-QICOR-R008 | Cada corrida acota cuantas correcciones intenta y declara lo que quedo sin corregir | major | - |
| F-QICOR-R009 | Antes de corregir se revalida el original; si ya cumple, no se corrige nada | critical | Se corrigio el ejercicio '{quizId}' a partir de un diagnostico de consigna que la validacion vigente ya no sostiene |
| F-QICOR-R010 | Una parte vacia del enunciado no es contenido: ni bloquea la correccion ni se pierde al aprobarla | critical | El ejercicio '{quizId}' perdio al aplicar su correccion una parte vacia del enunciado que el original tenia |
| F-QICOR-R011 | La longitud se mide y se declara, pero no rechaza una correccion | critical | Se propuso para el ejercicio '{quizId}' una correccion fuera del rango de longitud de su nivel sin declararlo |
| F-QICOR-R012 | El lazo de intentos lo conduce quien corrige, y al agotarlo entrega su mejor candidato | critical | La correccion del ejercicio '{quizId}' agoto sus intentos y descarto el mejor candidato que habia producido |
| F-QICOR-R013 | Cual es el mejor candidato: primero si sirve, despues cuanto conserva, la longitud al final | critical | Se entrego para el ejercicio '{quizId}' un candidato peor que otro que la misma correccion habia producido |
| F-QICOR-R014 | Lo que se entrega sin cumplir todo se entrega diciendolo | critical | Se propuso para el ejercicio '{quizId}' una correccion que no cumple el criterio '{criterio}' sin declararlo |
| F-QICOR-R015 | Los criterios se consultan, no se reimplementan | critical | El candidato del ejercicio '{quizId}' se juzgo con una medicion distinta de la que aplica el sistema |
| F-QICOR-R016 | El ejercicio corregido tiene que describir algo que pueda pasar en el mundo real | critical | Se propuso para el ejercicio '{quizId}' una correccion que describe algo que no puede pasar en el mundo real |

**User Journeys:**

- **F-QICOR-J001**: Corregir un ejercicio que incumple su consigna, y decidir la correccion

- **F-QICOR-J002**: Corregir un tramo acotado de las tareas de consigna del plan

### FEAT-OPMUL: Ejercicios de opcion multiple - cargar, conservar y medir sin corregir [F-OPMUL]

> **Que**: ContentAudit carga y guarda los ejercicios de opcion multiple (MC) sin perder
nada, los mide, se los muestra al juez de consigna con sus opciones y no los corrige.

**Por que**: Es lo que el sistema hace desde el 30/9 sin ninguna regla que lo diga; sin
reglas, nada impide volver a perder las opciones al guardar o corregir un MC.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-OPMUL-R001 | Cargar y guardar el curso sin cambios lo deja identico byte a byte | critical | - |
| F-OPMUL-R002 | Lo que el sistema no interpreta de un ejercicio sobrevive, con su valor y en su orden | critical | - |
| F-OPMUL-R003 | La auditoria mide todos los ejercicios, los de opcion multiple incluidos | critical | - |
| F-OPMUL-R004 | La oracion medible de un MC es el enunciado con la opcion correcta en el hueco, tal cual | critical | - |
| F-OPMUL-R005 | El juez ve las opciones de un MC con la correcta marcada, y un CLOZE igual que antes | critical | - |
| F-OPMUL-R006 | Ninguna correccion toca un ejercicio de opcion multiple | critical | - |
| F-OPMUL-R007 | repair nunca restaura un MC desde una foto de la era CLOZE | critical | - |
| F-OPMUL-R008 | Los datos nuevos se comparan solo si la referencia ya los conocia | critical | - |
| F-OPMUL-R009 | Si el formulario no dice de que tipo es el ejercicio, vale el tipo del ejercicio | critical | - |

**User Journeys:**

- **F-OPMUL-J001**: Auditar un curso con ejercicios de opcion multiple

- **F-OPMUL-J002**: Pedir una correccion sobre un ejercicio de opcion multiple

- **F-OPMUL-J003**: Guardar, revisar al lado y reparar sin perder nada

### FEAT-HALL: Contrato del hallazgo, catálogo de analizadores y números por contexto [F-HALL]

> **Qué**: Todo analizador informa lo que no pasa con un hallazgo de forma fija, se describe solo en `get analyzers`, y
content-audit publica en cada tema, nivel y curso los errores contados y el puntaje de vocabulario de hoy.

**Por qué**: Sin un contrato común, cada uno de los 11 analizadores nuevos pide pantallas y cuentas propias, el curso
da 73,9 % o 73,4 % según quién sume, y lo que decide el juez de consigna no llega a ningún tema, nivel ni curso.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-HALL-R001 | Todo nodo que no pasa deja hallazgos, y todos tienen la misma forma | critical | - |
| F-HALL-R002 | Gravedad, resolución y costo toman valores cerrados | critical | - |
| F-HALL-R003 | La evidencia deja verificar el hallazgo sin volver a correr el analizador | critical | - |
| F-HALL-R004 | Los errores llegan al plan sin cambiar la regla del plan | critical | - |
| F-HALL-R005 | `get analyzers` lista todos los analizadores, cada uno con un solo nombre | critical | - |
| F-HALL-R006 | Cada analizador se describe solo, en su ficha del catálogo | critical | - |
| F-HALL-R007 | Dos familias, y cada analizador pertenece a una sola | critical | - |
| F-HALL-R008 | Los errores se cuentan en content-audit, por analizador y en total | critical | - |
| F-HALL-R009 | El puntaje de vocabulario es el de hoy | critical | - |
| F-HALL-R010 | Un solo número por nodo, publicado por content-audit | critical | - |
| F-HALL-R011 | Los jueces llegan a los números del tema, el nivel y el curso | critical | - |
| F-HALL-R012 | Se puede correr un analizador solo | major | Analyzer '<name>' not found. Run 'content-audit get analyzers' to see available analyzers. |
| F-HALL-R013 | Lo que ya existe no cambia de número | critical | - |
| F-HALL-R014 | La salida es determinista y cada hallazgo tiene una identidad estable | critical | - |
| F-HALL-R015 | Analizar no escribe nada sobre el curso | critical | - |

**User Journeys:**

- **F-HALL-J001**: Analizar el curso y leer qué está mal en cada contexto

- **F-HALL-J002**: Examinar un analizador solo

### FEAT-RONDA1: Integración de la ronda 1 - lo que comparten reglas, jueces y vocabulario, y los tres pedidos de la UI [F-RONDA1]

> **Qué**: Junta en un solo cambio lo que comparten los catorce parches de la ronda 1 (reglas, jueces y vocabulario) y
suma los tres pedidos de la UI que José aprobó: el nombre en castellano en la ficha, el ejercicio como lo ve el alumno y
los conteos por gravedad y por regla.

**Por qué**: Cada paquete dejó ganchos en archivos comunes, y si cada uno los toca por su cuenta chocan. Sin los tres
pedidos, la UI tendría que inventar nombres, oraciones y conteos, que es lo que la línea de la UI prohíbe.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-RONDA1-R001 | El ejercicio como lo ve el alumno sale de un solo lugar, y siempre igual | critical | - |
| F-RONDA1-R002 | En el resumen, cada ejercicio se llama por su enunciado | major | - |
| F-RONDA1-R003 | `get student-view <tema>` devuelve el tema entero como lo ve el alumno | major | - |
| F-RONDA1-R004 | Cada ficha trae su nombre en castellano | major | - |
| F-RONDA1-R005 | Los números de cada contexto cuentan por gravedad y por regla, en las dos familias | critical | - |
| F-RONDA1-R006 | Un ejercicio evaluado a medias cuenta como evaluado, y lo dice | critical | - |
| F-RONDA1-R007 | La corrida base corre las reglas instantáneas de un analizador con juez, sin pagar | major | - |
| F-RONDA1-R008 | Integrar la ronda no mueve ningún número, salvo el de las bandas | critical | - |

### FEAT-PISTA: Pista coherente - la pista anuncia lo que el alumno tiene que escribir [F-PISTA]

> **Qué**: Un analizador de la familia de errores compara la pista de cada ejercicio con su respuesta —el verbo, el
sujeto y la negación—, marca las reescrituras que piden palabras que nada anuncia y marca los ejercicios sin pista en
un tema donde tres cuartos o más la llevan.

**Por qué**: Una pista que no coincide con la respuesta hace imposible acertar: «Who ___ (I / see)?» sólo acepta «are
you seeing». El 077 encontró 188 ejercicios imposibles y content-audit los muestra en verde.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-PISTA-R001 | Si tres cuartos o más del tema llevan pista, la llevan todos | critical | - |
| F-PISTA-R002 | Las palabras de la pista están en la respuesta, y la respuesta no trae un verbo sin anunciar | critical | - |
| F-PISTA-R003 | El sujeto de la pista es el de la respuesta, y ninguno queda sin anunciar | critical | - |
| F-PISTA-R004 | La pista lleva «not» sólo si la respuesta es negativa | major | - |
| F-PISTA-R005 | `hint-coherence` es un analizador de errores con arreglo por regla | major | - |
| F-PISTA-R006 | La reescritura no pide palabras que no salen de la oración de partida, la pista o la consigna | critical | - |

**User Journeys:**

- **F-PISTA-J001**: Revisar si la pista anuncia la respuesta

### FEAT-RESPC: Respuestas completas - el corrector acepta todas las formas de la respuesta y ninguna consigna pide una [F-RESPC]

> **Qué**: Un analizador de la familia de errores arma, para cada respuesta aceptada, sus formas equivalentes —contraída
y larga, las dos negaciones cortas, las dos grafías, until y till—, marca las que el corrector rechazaría y marca las
consignas y miniteorías que piden una forma.

**Por qué**: El 077 encontró 484 ejercicios donde una respuesta correcta da mal, y José decidió el 30/9 que el
corrector acepte siempre las dos formas y que ninguna consigna pida una; hoy nadie lo mide.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-RESPC-R001 | Una forma está aceptada si el corrector del backend la acepta | critical | - |
| F-RESPC-R002 | Están la contraída y la larga, pida lo que pida la consigna | critical | - |
| F-RESPC-R003 | Las dos negaciones cortas de *be* están aceptadas | critical | - |
| F-RESPC-R004 | Están la grafía británica y la americana, y *until* y *till* | major | - |
| F-RESPC-R005 | Un hallazgo por forma que falta, que se arregla en el corrector o agregándola | major | - |
| F-RESPC-R006 | `answer-completeness` es un analizador de errores que evalúa ejercicios | major | - |
| F-RESPC-R007 | Ninguna consigna ni miniteoría pide una forma | major | - |

**User Journeys:**

- **F-RESPC-J001**: Encontrar las formas que el corrector rechaza y los pedidos de forma

### FEAT-OMVAL: Opción múltiple válida - las reglas de las opciones [F-OMVAL]

> **Qué**: Un analizador de la familia de errores revisa las opciones de cada ejercicio de opción múltiple: una sola
correcta, todas distintas —también en la forma—, con la mayúscula que pide la oración y sin un artículo que delate la
respuesta.

**Por qué**: El 077 marcó 315 de los 2.810 ejercicios de opción múltiple (11,2 %), casi todos por forma, y hoy
content-audit los mide pero no mira sus opciones.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-OMVAL-R001 | Una sola opción vale, y vale el puntaje entero | critical | - |
| F-OMVAL-R002 | Las opciones son distintas entre sí, también en la forma | critical | - |
| F-OMVAL-R003 | Si el hueco abre la oración, todas las opciones abren con mayúscula | major | - |
| F-OMVAL-R004 | Ninguna opción queda delatada por el artículo de antes del hueco | major | - |
| F-OMVAL-R005 | `multiple-choice-validity` evalúa sólo los ejercicios de opción múltiple | major | - |

**User Journeys:**

- **F-OMVAL-J001**: Revisar las opciones de un ejercicio de opción múltiple

### FEAT-DATOS: Datos coherentes - todas las copias del ejercicio dicen lo mismo [F-DATOS]

> **Qué**: Un analizador de la familia de errores compara las copias de cada ejercicio —las partes con su primera
respuesta, la oración guardada y el texto con que se grabó su audio— y marca las que no coinciden o llevan «/».

**Por qué**: Cuando se corrige una respuesta, las otras copias quedan viejas: el 077 encontró audios que leen «has not
cut barra hasn't cut» y oraciones guardadas con barra, y hoy nadie lo mide.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-DATOS-R001 | Las dos oraciones guardadas son el ejercicio armado con su primera respuesta | major | - |
| F-DATOS-R002 | Ni las respuestas ni el audio llevan «/» | critical | - |
| F-DATOS-R003 | El audio dice lo mismo que el ejercicio de hoy | major | - |
| F-DATOS-R004 | Todo ejercicio tiene un audio cuyo texto se conoce | minor | - |
| F-DATOS-R005 | `data-coherence` es un analizador de errores que evalúa ejercicios | major | - |

**User Journeys:**

- **F-DATOS-J001**: Comparar las copias de un ejercicio

### FEAT-DUPEX: Duplicados exactos - dos ejercicios iguales del mismo tema [F-DUPEX]

> **Qué**: Un analizador de la familia de errores marca cada ejercicio que repite, tal cual, a otro de su mismo tema: el
alumno escribe dos veces lo mismo.

**Por qué**: Hoy nadie lo mira: el 077 encontró 30 repetidos y content-audit los muestra en verde. La parte exacta es
una regla de segundos; los parecidos, con modelo, son del paquete B.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-DUPEX-R001 | Dos ejercicios del mismo tema que terminan igual son duplicados exactos | major | - |
| F-DUPEX-R002 | Un hallazgo por copia, nunca en el ejercicio que queda | major | - |
| F-DUPEX-R003 | El hallazgo es de gravedad baja, va a la mesa y cuesta segundos | major | - |
| F-DUPEX-R004 | La evidencia muestra los dos ejercicios como los ve el alumno | major | - |
| F-DUPEX-R005 | `exact-duplicates` es un analizador de errores que evalúa ejercicios | major | - |

**User Journeys:**

- **F-DUPEX-J001**: Encontrar las copias de un tema

### FEAT-JEXAM: El examen fijo de los jueces y el juez de consigna, sin tocar su huella [F-JEXAM]

> **Qué**: Todo juez con modelo rinde un examen fijo por versión antes de juzgar el curso y dice cuánto le falta y cuánto
cuesta; el juez de consigna, además, muestra su veredicto entero sin cambiar lo que lee.

**Por qué**: El juez de consigna se afinó para marcar menos y nunca se midió lo que deja pasar: da por buenos 178 de los
187 imposibles. Con cuatro jueces nuevos, pagar sin medir se multiplica por cinco.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-JEXAM-R001 | Cada versión de un juez rinde el examen fijo y su resultado queda a la vista | critical | - |
| F-JEXAM-R002 | El examen no toca el curso y no paga dos veces lo mismo | critical | - |
| F-JEXAM-R003 | Una versión sin examen aprobado no consulta sobre el curso | critical | - |
| F-JEXAM-R004 | Cada juez dice cuánto le falta y cuánto cuesta, sin consultar | critical | - |
| F-JEXAM-R005 | A todo juez se le puede pedir un conjunto del curso o un candidato de la mesa | critical | - |
| F-JEXAM-R006 | Consultas a la vez, sin pasarse del tope | critical | - |
| F-JEXAM-R007 | Lo que lee el juez de consigna no cambia | critical | - |
| F-JEXAM-R008 | El juez de consigna muestra su veredicto entero | major | - |
| F-JEXAM-R009 | El examen del juez de consigna | critical | - |
| F-JEXAM-R010 | Sin pedido explícito, ningún juez consulta | critical | - |
| F-JEXAM-R011 | Con la sesión del proveedor vencida, el juez no consulta y el informe dice cómo renovarla | critical | - |
| F-JEXAM-R012 | Los jueces nuevos no tratan como problema una forma que el corrector acepta | critical | - |

**User Journeys:**

- **F-JEXAM-J001**: Rendir el examen y juzgar el curso con esa versión

### FEAT-TRSEN: Juez de traducción y sentido [F-TRSEN]

> **Qué**: Un analizador nuevo, `translation-sense`, lee cada ejercicio entero como queda y dice si el inglés y la
traducción dicen lo mismo y si un nativo lo diría así; lo juzgan dos jueces que no se hablan.

**Por qué**: El 077 encontró 738 así y ningún analizador los ve: el de consigna no lee la traducción y dio «cumple» en
284 de los 285 de A1 y A2.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-TRSEN-R001 | Lee el ejercicio entero como lo ve el alumno, y nada más | critical | - |
| F-TRSEN-R002 | Cuatro condiciones, sobre el ejercicio como está | critical | - |
| F-TRSEN-R003 | Dos jueces que no se hablan | critical | - |
| F-TRSEN-R004 | Gravedad y puntaje | critical | - |
| F-TRSEN-R005 | Reuso, tope, fallas y cobertura, como el juez de consigna | critical | - |
| F-TRSEN-R006 | El examen de translation-sense | critical | - |
| F-TRSEN-R007 | Lo que marca se resuelve en la mesa, con los mismos dos jueces | major | - |

**User Journeys:**

- **F-TRSEN-J001**: Juzgar la traducción y el sentido de un ejercicio

### FEAT-UNICA: Juez de una sola respuesta [F-UNICA]

> **Qué**: Un analizador nuevo, `single-answer`, lista con lo que el alumno ve antes de contestar las respuestas que
también valdrían y el corrector no acepta: la que ofrece la consigna la deriva a respuestas completas; la distinta, la marca.

**Por qué**: El 077 encontró 476 ejercicios con otra respuesta igual de buena, y el juez de consigna, que revisa la
resolubilidad, dio «cumple» en los 179 de A1 y A2.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-UNICA-R001 | Lee lo que el alumno ve antes de contestar | critical | - |
| F-UNICA-R002 | Lista las respuestas que también valdrían, salvo las que el corrector acepta | critical | - |
| F-UNICA-R003 | Una opción que ofrece la consigna se deriva a respuestas completas | critical | - |
| F-UNICA-R004 | La distinta y válida se marca, con su gravedad | critical | - |
| F-UNICA-R005 | Reuso, tope, fallas y cobertura, como el juez de consigna | critical | - |
| F-UNICA-R006 | El examen de single-answer | critical | - |
| F-UNICA-R007 | Se arregla con una regla o en la mesa, y la mesa juzga con este juez | major | - |

**User Journeys:**

- **F-UNICA-J001**: Buscar otra respuesta que también valga

### FEAT-MINIT: Juez de miniteoría [F-MINIT]

> **Qué**: Un analizador nuevo, `mini-theory`, juzga cada tema de una vez: lee sus miniteorías y todos sus ejercicios, y
dice si cada regla es cierta y si algún ejercicio del tema la contradice.

**Por qué**: El 077 encontró 335 ejercicios con una tarjeta falsa, en pocos temas, y nada la mira: el de consigna no lee
la miniteoría y dio «cumple» en los 112 de A1 y A2.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-MINIT-R001 | Un juez por tema, que lee las miniteorías y el tema entero | critical | - |
| F-MINIT-R002 | Por cada miniteoría: si es cierta y si el tema la contradice | critical | - |
| F-MINIT-R003 | La miniteoría con problema cuenta como error sólo donde se muestra | critical | - |
| F-MINIT-R004 | Gravedad y puntaje de cada ejercicio | critical | - |
| F-MINIT-R005 | Reuso, tope, fallas y cobertura, contando temas | critical | - |
| F-MINIT-R006 | El examen de mini-theory | critical | - |
| F-MINIT-R007 | Se resuelve una vez por miniteoría | major | - |

**User Journeys:**

- **F-MINIT-J001**: Juzgar las miniteorías de un tema

### FEAT-MCVALE: Juez de opción múltiple, ¿otra opción también vale? [F-MCVALE]

> **Qué**: Una regla con modelo del analizador de opción múltiple válida lee cada ejercicio de opción múltiple como lo ve
el alumno y dice si alguna opción marcada como incorrecta también vale en ese contexto, o si la correcta no vale.

**Por qué**: En A1 y A2 el 077 encontró 93 ejercicios de opción múltiple con dos opciones correctas y una sola aceptada;
las reglas de forma no los ven y el juez de consigna los dio por buenos.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-MCVALE-R001 | Lee cada opción múltiple como la ve el alumno | critical | - |
| F-MCVALE-R002 | Qué decide: si otra opción también vale, y si la correcta vale | critical | - |
| F-MCVALE-R003 | Gravedad y puntaje | critical | - |
| F-MCVALE-R004 | Es una regla del analizador de opción múltiple válida | major | - |
| F-MCVALE-R005 | Reuso, tope, fallas y cobertura, como el juez de consigna | critical | - |
| F-MCVALE-R006 | El examen del juez de opción múltiple | critical | - |
| F-MCVALE-R007 | Lo que marca se resuelve en la mesa, con este mismo juez | major | - |

**User Journeys:**

- **F-MCVALE-J001**: Juzgar si otra opción también vale

### FEAT-NIVFR: Palabras de otro nivel, mirando las frases del EVP [F-NIVFR]

> **Qué**: Un analizador nuevo de la familia de errores, `off-level-words`, cuenta los ejercicios con palabras que el alumno
de su nivel no puede conocer, dos niveles arriba o fuera del EVP, leídas con el catálogo y las frases de `lemma-absence`.

**Por qué**: `lemma-absence` marca 337 ejercicios de A1 y el 077 marcó 17. José decidió el 30/9 que el error lo cuente un
analizador aparte, con su ficha, y que `lemma-absence` siga entero en el 73,9 %.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-NIVFR-R001 | El nivel de cada palabra es el que resuelve lemma-absence | critical | - |
| F-NIVFR-R002 | Cuenta como error sólo lo que queda lejos del nivel | critical | - |
| F-NIVFR-R003 | Lo que el tema enseña no es error | major | - |
| F-NIVFR-R004 | La palabra que la traducción repite no es error | major | - |
| F-NIVFR-R005 | Un hallazgo por palabra, contado como error | critical | - |
| F-NIVFR-R006 | No mueve nada de lemma-absence ni del vocabulario | critical | - |
| F-NIVFR-R007 | Se describe en su ficha con un solo nombre | major | - |

**User Journeys:**

- **F-NIVFR-J001**: Contar las palabras de otro nivel de un ejercicio

### FEAT-POCOV: Palabras poco vistas, con prioridad por banda de frecuencia [F-POCOV]

> **Qué**: `lemma-count` sigue midiendo igual, y ahora dice qué palabra falta repetir, en qué orden y dónde está: cada
palabra poco vista es un hallazgo de su nivel, con su banda de frecuencia y los temas que la usan.

**Por qué**: Cuenta en el 73,9 % (67,0 % en el curso) pero sólo deja un número: 1.535 lemas en menos de 4 oraciones, y
nadie sabe si «education» (una oración en B1) importa más que «goalkeeper». José eligió umbrales por banda (8 · 6 · 4 · 3).

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-POCOV-R001 | El umbral de cada palabra es el de su banda, y «4 para todas» da lo de hoy | critical | - |
| F-POCOV-R002 | Sin tareas, como hoy | critical | - |
| F-POCOV-R003 | Cada palabra poco vista es un hallazgo de su nivel | critical | - |
| F-POCOV-R004 | La banda de frecuencia ordena qué repetir primero | critical | - |
| F-POCOV-R005 | Cada palabra dice en qué temas está | major | - |
| F-POCOV-R006 | La ficha la describe con su nombre de siempre | major | - |
| F-POCOV-R007 | Las palabras sugeridas usan el umbral de la banda de cada palabra | critical | - |

**User Journeys:**

- **F-POCOV-J001**: Ver qué palabras del nivel repetir primero

### FEAT-SEMAF: Semáforo de palabras por tema [F-SEMAF]

> **Qué**: Una consulta por tema que devuelve sus palabras en verde (el nivel las espera y el curso casi no las usa) y en
rojo (están por encima del nivel), más las palabras verdes para sumar, salvo las que el propio tema enseña.

**Por qué**: Cuando una corrección tiene que cambiar una palabra, hoy no sabe cuál elegir: «wears» o una más rara. El
curso necesita repetir, no evitar: de los 1.750 lemas que vuelven al menos una vez, sólo 134 lo hacen a un ritmo sano.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-SEMAF-R001 | Una consulta por tema, de sólo lectura | critical | Knowledge '<id>' not found in the analysis. |
| F-SEMAF-R002 | Verde propio: la palabra del tema que conviene que vuelva | critical | - |
| F-SEMAF-R003 | Verde para sumar: las sugeridas del nivel del tema | critical | - |
| F-SEMAF-R004 | Lo que el tema enseña nunca es rojo | critical | - |
| F-SEMAF-R005 | Rojo: lo que `lexis` marca para el nivel del tema | critical | - |
| F-SEMAF-R006 | Una palabra nunca es roja por aparecer mucho | major | - |
| F-SEMAF-R007 | El verde se ofrece; el rojo sólo avisa | critical | - |

**User Journeys:**

- **F-SEMAF-J001**: Pedir el semáforo de un tema

### FEAT-RECUR: Recurrencia visible, por palabra y por nivel [F-RECUR]

> **Qué**: `lemma-recurrence` sigue dando el mismo puntaje de curso, y además publica cada palabra (cuántas veces está,
cada cuántas palabras vuelve y dónde está su hueco más largo) y la recurrencia dentro de cada nivel, como detalle.

**Por qué**: Es la peor métrica del curso (8 %) y hoy sólo deja ese número: la vista del dashboard dice «No lemma
recurrence data available», y nadie puede ver qué palabra vuelve poco ni en qué nivel se pierde.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-RECUR-R001 | Umbrales y orden son los de hoy; las posiciones son las palabras del texto | critical | - |
| F-RECUR-R002 | El plan y las tareas no cambian | critical | - |
| F-RECUR-R003 | Cada palabra analizada queda en el informe | critical | - |
| F-RECUR-R004 | La recurrencia dentro de cada nivel, como detalle | critical | - |
| F-RECUR-R005 | Cada palabra fuera de rango es un hallazgo del curso | major | - |
| F-RECUR-R006 | La ficha la describe con su nombre de siempre | major | - |

**User Journeys:**

- **F-RECUR-J001**: Ver qué palabras vuelven poco, en el curso y en cada nivel

### FEAT-CIERRE: Cierre de la ronda 1 - lo que les falta a los jueces antes de la primera corrida paga [F-CIERRE]

> **Qué**: Cierra los cuatro huecos del contrato que dejó la construcción de los jueces con modelo (paquete C):
- la cobertura de un juez por tema no cuenta los ejercicios que abarca;
- una consulta fallida no dice por qué;
- dos clasificadores y un repartidor escriben el ejercicio a su manera, en vez de con la vista del alumno;
- la configuración del juez de consigna muestra un solo tope, sin decir de qué comando es.

**Por qué**: Todo esto tiene que quedar fijo antes de la primera corrida paga. Lo que se ve de un hallazgo y lo que lee el
juez no pueden divergir. Y una falla sin causa obliga a adivinar por qué un ejercicio quedó sin evaluar después de pagar.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-CIERRE-R001 | La cobertura de un juez cuenta también los ejercicios que abarca | critical | - |
| F-CIERRE-R002 | Una consulta que falla dice por qué, ejercicio por ejercicio | critical | - |
| F-CIERRE-R003 | Los clasificadores escriben el ejercicio con la misma vista que lee el juez | critical | - |
| F-CIERRE-R004 | La configuración del juez de consigna muestra sus dos topes, cada uno con su comando | major | - |

### FEAT-RONDA2: Integración de la ronda 2 - lo que comparten idioma, corrección por tandas y antes y después [F-RONDA2]

> **Qué**: Junta en un solo cambio lo que comparten los trece parches de la ronda 2 —B, idioma con herramientas locales;
E, corrección por tandas; F, antes y después— y lo que cada uno dejó como «gancho» en archivos comunes: los nombres y el
orden de las tres fichas nuevas, la corrida base, la medida de cada analizador en el informe, el caché de tokens por
oración y el tipo de tarea de cada analizador de errores.

**Por qué**: Si cada paquete toca por su cuenta el catálogo, el informe, el plan o el caché, chocan, o uno decide algo
que el otro contradice. Y hay dos riesgos de calidad que ningún paquete cubre: una corrida sin la lista de lemas publica
números falsos sin avisar, y el caché de tokens los guarda para las corridas siguientes.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-RONDA2-R001 | Las tres fichas de B traen su nombre en castellano y van después de las diecisiete | major | - |
| F-RONDA2-R002 | El tope por omisión depende de a quién se consulta | critical | - |
| F-RONDA2-R003 | La corrida base no corre lo que mide con un modelo local | major | - |
| F-RONDA2-R004 | Cada informe dice con qué medida midió cada analizador | critical | - |
| F-RONDA2-R005 | El caché de tokens guarda cada oración por separado | critical | - |
| F-RONDA2-R006 | La clave del caché incluye la huella de la lista de lemas y la del procesador | critical | - |
| F-RONDA2-R007 | Sin la lista de lemas o sin el procesador de spaCy, `analyze` frena con un error claro | critical | - |
| F-RONDA2-R008 | Cada analizador de errores tiene su tipo de tarea, salvo los que sólo ordenan | critical | - |
| F-RONDA2-R009 | Gramática y Duplicados parecidos declaran lo que su arreglo puede tocar | major | - |
| F-RONDA2-R010 | Integrar la ronda 2 no mueve ningún número de vocabulario ni el curso | critical | - |

### FEAT-MLOCAL: Idioma local - herramientas y modelos que corren en la máquina [F-MLOCAL]

> **Qué**: Gramática, Naturalidad y Duplicados parecidos corren con su herramienta en la máquina y su versión a la vista;
sin Ollama, los dos de modelo le preguntan lo mismo a claude -p, sólo con tope; los tres leen candidatos de la mesa.

**Por qué**: Son las primeras dependencias de idioma de content-audit. Sin reglas comunes, cada uno decidiría distinto
qué pasa cuando falta el modelo, cuándo corre y qué cambia cuando cambia la versión.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-MLOCAL-R001 | Con sus herramientas, nada sale de la máquina; a claude -p, sólo sin Ollama y con tope | critical | - |
| F-MLOCAL-R002 | Cada herramienta tiene nombre y versión a la vista, y la misma versión da lo mismo | critical | - |
| F-MLOCAL-R003 | Si falta la herramienta, se sabe antes de empezar | critical | Analyzer '<name>' needs <tool>, which is not available: <how to install>. |
| F-MLOCAL-R004 | Qué corre sin pedirlo | major | - |
| F-MLOCAL-R005 | Los tres leen un candidato de la mesa y dicen qué cambia | critical | - |
| F-MLOCAL-R006 | Cada versión rinde su examen antes de publicar | major | - |
| F-MLOCAL-R007 | Sin Ollama, Naturalidad y Duplicados parecidos le preguntan lo mismo a claude -p | critical | - |
| F-MLOCAL-R008 | La alternativa consulta sólo con un tope pedido, y sin tope dice cuánto costaría | critical | - |

**User Journeys:**

- **F-MLOCAL-J001**: Correr un analizador de idioma

### FEAT-GRAMA: Gramática - la oración terminada no tiene errores de inglés [F-GRAMA]

> **Qué**: `grammar`, de la familia de errores, pasa LanguageTool por cada oración terminada, con cada respuesta
aceptada, y marca gramática y ortografía, nunca estilo; más una regla propia de signo final.

**Por qué**: «He ___ (own) a bike» sólo acepta «'ve had», y «…making noise noise?» sale en verde. Mide el curso en
segundos y la mesa lo necesita para no meter errores nuevos; del «inglés incorrecto» del 077 ve poco: es uso y sentido.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-GRAMA-R001 | Lee cada oración terminada, con cada respuesta aceptada, como la lee el alumno | critical | - |
| F-GRAMA-R002 | Sólo gramática y ortografía, y sólo lo que marcan el inglés americano y el británico | critical | - |
| F-GRAMA-R003 | Toda oración terminada cierra con su signo | major | - |
| F-GRAMA-R004 | La gravedad depende de dónde está el error | critical | - |
| F-GRAMA-R005 | Lo mecánico se arregla con una regla; lo demás, en la mesa | major | - |
| F-GRAMA-R006 | `grammar` es un analizador de errores, instantáneo, que evalúa ejercicios | major | - |
| F-GRAMA-R007 | El examen de Gramática | major | - |

**User Journeys:**

- **F-GRAMA-J001**: Revisar la gramática de un ejercicio

### FEAT-NATUR: Naturalidad - qué oraciones mirar primero [F-NATUR]

> **Qué**: `naturalness` le pregunta a un modelo local —sin Ollama, a claude -p—, por cada oración terminada, si un nativo
la diría así, y marca las más dudosas de cada nivel para que los jueces empiecen por ahí. No cuenta errores.

**Por qué**: Juzgar el curso con los cinco jueces son unas 220 horas de a una. Medido con el modelo ya instalado, esa
cola trae el 27 % de lo que el 077 llamó antinatural o sin sentido, más de cinco veces el azar.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-NATUR-R001 | Lee la oración terminada, y sólo si es una oración | critical | - |
| F-NATUR-R002 | Un número por ejercicio: qué tan poco probable es que un nativo lo diga así | critical | - |
| F-NATUR-R003 | Marca el 5 % más dudoso de cada nivel, y no cuenta como error | critical | - |
| F-NATUR-R004 | La evidencia dice cuánto duda el modelo | major | - |
| F-NATUR-R005 | El orden y los marcados se pueden pasar a un juez | major | - |
| F-NATUR-R006 | Se usa sólo si ordena mucho mejor que el azar | critical | - |
| F-NATUR-R007 | `naturalness` es un analizador de errores que sólo ordena | major | - |
| F-NATUR-R008 | Sin Ollama, la misma pregunta a claude -p: un «no» marca | critical | - |

**User Journeys:**

- **F-NATUR-J001**: Ordenar qué mirar primero

### FEAT-DUPAR: Duplicados parecidos - dos ejercicios casi iguales del mismo tema [F-DUPAR]

> **Qué**: `near-duplicates`, de la familia de errores, marca cada ejercicio que repite casi igual a otro de su tema: la
misma respuesta con otro pronombre, otro demostrativo u otro tiempo del auxiliar, o la misma idea con otras palabras.

**Por qué**: El 077 marcó 6 así en la mitad para escribir, como «He is leaving (certainly)» y «She is leaving
(certainly)»; la regla exacta del paquete A no los ve. Compara con un modelo local; sin Ollama, claude -p juzga los pares.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-DUPAR-R001 | Sólo son casi duplicados dos ejercicios del mismo tema que piden lo mismo | major | - |
| F-DUPAR-R002 | Casi iguales: cambia sólo el sujeto, el demostrativo, el posesivo o el tiempo, o dicen lo mismo | major | - |
| F-DUPAR-R003 | Un hallazgo por copia, nunca en el ejercicio que queda | major | - |
| F-DUPAR-R004 | El hallazgo es de gravedad baja y va a la mesa | major | - |
| F-DUPAR-R005 | La evidencia muestra los dos ejercicios y qué cambia | major | - |
| F-DUPAR-R006 | `near-duplicates` es un analizador de errores que evalúa ejercicios | major | - |
| F-DUPAR-R007 | El examen de Duplicados parecidos | major | - |
| F-DUPAR-R008 | Sin Ollama, claude -p juzga los pares de texto parecido | major | - |

**User Journeys:**

- **F-DUPAR-J001**: Encontrar las copias casi iguales de un tema

### FEAT-PROYU: Una sola proyección del curso con sus propuestas [F-PROYU]

> **Qué**: El curso con lo aprobado y lo pendiente se proyecta de una sola manera, la de `get consolidated`, que da lo
mismo que `analyze` sobre ese curso escrito; ninguna proyección entra al historial de análisis, y la línea base la fija
José y no se mueve cuando el análisis activo avanza.

**Por qué**: Hoy hay tres caminos que no coinciden: sobre el mismo caso, la vista consolidada pone A1 en 92,4 % donde
el análisis de la copia da 96,5 y no muestra el curso, y el plan proyectado deja su análisis como el último.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-PROYU-R001 | La proyección da lo mismo que analizar el curso escrito | critical | - |
| F-PROYU-R002 | Las pendientes entran siempre igual | critical | - |
| F-PROYU-R003 | La proyección mide lo mismo que la línea base y no paga de más | critical | - |
| F-PROYU-R004 | Ninguna proyección entra al historial de análisis | critical | - |
| F-PROYU-R005 | Una proyección guardada se pide por su identificador | major | - |
| F-PROYU-R006 | La línea base la fija José y queda fija; ni ella ni el análisis activo se borran sin querer | critical | Cannot delete audit '<id>': it is the active analysis. Point 'set-active' to another analysis first. |
| F-PROYU-R007 | El impacto de una propuesta sale de la misma proyección | major | - |

**User Journeys:**

- **F-PROYU-J001**: Proyectar las propuestas del plan activo y leer lo mismo en todas partes

### FEAT-METAS: Antes y después de una tanda contra la línea base, con metas fijadas antes de correr [F-METAS]

> **Qué**: content-audit compara la proyección de una tanda con la línea base y publica, por analizador y en cada tema,
topic, nivel y el curso, el antes, el después, el cambio, la meta y si se cumple; y dice qué ejercicios vuelven y si la
tanda pasa.

**Por qué**: Hoy no hay metas ni veredicto, y los números de nivel no alcanzan: «My grandmother ___ (come) every year.»
mete una palabra de A2 en A1 y A1 sigue en 96,54 %; sólo se ve en su ejercicio.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-METAS-R001 | Una fila por analizador y nodo, con antes, después y cambio | critical | - |
| F-METAS-R002 | Cada fila dice su meta y si se cumple | critical | - |
| F-METAS-R003 | Un nodo que la tanda cambió vuelve si gana un error o una palabra de otro nivel | critical | - |
| F-METAS-R004 | Un piso roto de nivel o de curso nombra a los cambios que lo rompen | major | - |
| F-METAS-R005 | Las metas se declaran en un solo lugar, y todo analizador tiene la suya | critical | - |
| F-METAS-R006 | Las metas se fijan antes de correr | critical | - |
| F-METAS-R007 | Un cambio de medida se muestra aparte | critical | - |
| F-METAS-R008 | La tanda pasa si cumple todos sus pisos | critical | - |
| F-METAS-R009 | Comparar no cambia nada | critical | - |

**User Journeys:**

- **F-METAS-J001**: Comparar una tanda con la línea base

### FEAT-CIEGA: Muestra ciega de cada tanda [F-CIEGA]

> **Qué**: Para cada tanda, content-audit sortea pares de antes y después, los muestra sin decir cuál es cuál, guarda lo
que elige quien revisa y publica el resultado, con su margen, contra la meta.

**Por qué**: Es la única medida que no depende de ningún analizador ni agente: en la tanda de las verduras, tres de los
cuatro arreglos no los ve ningún analizador de hoy.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-CIEGA-R001 | La muestra se sortea de la tanda y se puede repetir | critical | - |
| F-CIEGA-R002 | Cada par se ve como lo ve el alumno, sin pistas de cuál es cuál | critical | - |
| F-CIEGA-R003 | Una respuesta de cuatro, que no se cambia después de revelar | critical | - |
| F-CIEGA-R004 | El resultado se publica con su margen, contra la meta | critical | - |
| F-CIEGA-R005 | Con dos personas, cuánto coinciden | major | - |
| F-CIEGA-R006 | La tasa de error a ciegas mide lo que los analizadores no ven | major | - |

**User Journeys:**

- **F-CIEGA-J001**: Revisar a ciegas los cambios de una tanda

### FEAT-CERO: Cada corrida del motor empieza de cero [F-CERO]

> **Qué**: Toda corrida del motor dentro de una misma invocación da lo mismo que una invocación nueva sobre ese curso, y no
toca los informes de las corridas anteriores.

**Por qué**: Cuatro analizadores de curso guardan lo que cuentan y nunca lo vacían: el mismo curso corrido dos veces
seguidas da 72,9 % en vez de 73,9 %, y la segunda corrida vacía los diagnósticos COCA de los 55 topics de la primera.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-CERO-R001 | El resultado de una corrida depende sólo de su curso | critical | - |
| F-CERO-R002 | Una corrida no cambia los informes anteriores | critical | - |

**User Journeys:**

- **F-CERO-J001**: Pedir la vista consolidada de un par activo

### FEAT-TAREA: Una tarea por hallazgo - el plan de la familia de errores [F-TAREA]

> **Qué**: El plan convierte cada hallazgo de la familia de errores en una tarea propia, o declara qué otra tarea lo
cubre o por qué no se convirtió; cada tarea nombra su hallazgo, su gravedad, cómo se resuelve y qué puede tocar su arreglo.

**Por qué**: Hoy el plan hace una tarea por analizador y ejercicio y sólo conoce ocho nombres: los once analizadores nuevos
no tendrían tareas, y un ejercicio con cuatro errores quedaría en una sola tarea que nadie puede dar por cerrada.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-TAREA-R001 | Cada hallazgo que cuenta como error termina en una tarea, cubierto o declarado | critical | - |
| F-TAREA-R002 | La tarea nombra su hallazgo, y el mismo análisis da las mismas tareas | critical | - |
| F-TAREA-R003 | Una miniteoría con problema es una sola tarea, del tema | major | - |
| F-TAREA-R004 | Lo que ya marca otra tarea no abre una segunda | major | - |
| F-TAREA-R005 | Lo que sólo ordena no es tarea, y el desacuerdo de los jueces va a la bandeja | major | - |
| F-TAREA-R006 | Cada tipo de tarea declara lo que su arreglo puede tocar | critical | - |
| F-TAREA-R007 | Las tareas de vocabulario son las de hoy | critical | - |
| F-TAREA-R008 | Las tareas se buscan por analizador, resolución, gravedad, nivel y tema | major | - |
| F-TAREA-R009 | Una tarea de errores se ve con su hallazgo y con lo que su arreglo puede tocar | major | - |

**User Journeys:**

- **F-TAREA-J001**: Armar el plan de un análisis con hallazgos de errores

### FEAT-NOEMP: No empeorar - la compuerta de cada cambio y de cada tanda [F-NOEMP]

> **Qué**: «No empeorar» pasa a ser una compuerta medible en dos niveles: ningún cambio suma un error, una palabra roja ni
un audio distinto sin declararlo; y al cerrar una tanda ningún conteo de errores sube y ningún número de vocabulario baja
más de medio punto.

**Por qué**: Los promedios no lo ven. Aplicadas a una copia, las cuatro propuestas del 077 para el tema de las verduras
meten «grandmother» (A2) en un ejercicio de A1, y ningún puntaje de nivel ni de curso se mueve; sólo cambia la cuenta de
ejercicios con palabras marcadas: 351 → 352 en A1.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-NOEMP-R001 | Un cambio no suma errores | critical | - |
| F-NOEMP-R002 | Un cambio no suma palabras rojas | critical | - |
| F-NOEMP-R003 | El largo se mide y se declara, y cuenta en la tanda | major | - |
| F-NOEMP-R004 | Lo que se escucha sigue al ejercicio, y cambia sólo declarado | critical | - |
| F-NOEMP-R005 | Al cerrar una tanda, ningún error nuevo y ningún conteo que suba | critical | - |
| F-NOEMP-R006 | Al cerrar una tanda, el vocabulario baja como mucho medio punto | critical | - |
| F-NOEMP-R007 | Nada fuera de la tanda cambia | critical | - |
| F-NOEMP-R008 | Si la tanda frena, lo que empeora vuelve a la mesa y el resto espera | critical | - |
| F-NOEMP-R009 | La compuerta es determinista y dice por qué | major | - |
| F-NOEMP-R010 | Sólo se compara lo evaluado en las dos puntas | major | - |

**User Journeys:**

- **F-NOEMP-J001**: Cerrar una tanda contra la compuerta

### FEAT-MESA: La mesa por tema - uno propone, content-audit mide, los jueces juzgan y un script decide [F-MESA]

> **Qué**: Dentro de una tanda, una mesa por tema corrige juntos los ejercicios del tema que necesitan propuesta: revalida,
uno propone, content-audit mide, los jueces del catálogo juzgan el ejercicio como queda y un script decide; varias mesas
corren a la vez.

**Por qué**: Hoy se corrige de a una tarea, con criterios que comparan contra el original y lo dan por bueno, a 17–35
ejercicios por hora; los unos 1.900 ejercicios que necesitan propuesta llevarían semanas y nadie juzgaría lo que queda.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-MESA-R001 | La mesa toma un tema entero, y un tema tiene una sola mesa | critical | - |
| F-MESA-R002 | Antes de proponer, la mesa revalida | critical | - |
| F-MESA-R003 | Quien propone ve el tema entero como lo ve el alumno | critical | - |
| F-MESA-R004 | Content-audit mide cada candidato con la misma vara del análisis | critical | - |
| F-MESA-R005 | Los jueces juzgan el ejercicio como queda, cada uno solo | critical | - |
| F-MESA-R006 | Decide un script: resuelve, no empeora y le gana al original | critical | - |
| F-MESA-R007 | Hasta tres intentos; si nadie gana, queda el original y va a la bandeja | critical | - |
| F-MESA-R008 | Si los dos jueces de un criterio no coinciden, decide José | major | - |
| F-MESA-R009 | La mesa no escribe el curso | critical | - |
| F-MESA-R010 | Varias mesas a la vez, sin pasarse de las consultas pedidas | critical | - |
| F-MESA-R011 | Una opción múltiple sale de la mesa como opción múltiple, con una sola correcta | critical | - |
| F-MESA-R012 | La miniteoría se reescribe una vez, en los ejercicios que la muestran | major | - |
| F-MESA-R013 | Refutan los jueces que ya buscan un contraejemplo | major | - |
| F-MESA-R014 | Fuera de una tanda, la corrección de hoy no cambia | critical | - |
| F-MESA-R015 | Un tema que José manda pasar a opción múltiple sale en opción múltiple | major | - |

**User Journeys:**

- **F-MESA-J001**: Corregir un ejercicio en la mesa de su tema

### FEAT-TANDA: Tandas por nivel - reglas, mesas, recálculo del curso y una sola publicación [F-TANDA]

> **Qué**: La corrección va por tandas de un nivel: cada tanda parte del último estado publicado, aplica primero las reglas
y después las mesas de sus temas, recalcula el curso entero al cerrar, pasa la compuerta de «no empeorar» y se publica
con un solo OK de José.

**Por qué**: Recalcular el curso después de cada cambio obliga a ir de a uno. Por tanda, los temas van en paralelo y el
curso se mide una vez, completo —unos 38 segundos—, antes de que ningún cambio entre.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-TANDA-R001 | Una tanda es un nivel y parte del último estado publicado | critical | - |
| F-TANDA-R002 | Una sola tanda abierta, y cada tarea en una sola tanda | critical | La tarea <id> es de la tanda <id> y se decide en su bandeja. |
| F-TANDA-R003 | Antes de correr, la tanda dice cuánto cuesta, y no consulta sin tope | critical | - |
| F-TANDA-R004 | Primero las reglas, sin modelo | critical | - |
| F-TANDA-R005 | Después, las mesas de sus temas | critical | - |
| F-TANDA-R006 | Al cerrar, se recalcula el curso entero y se pasa la compuerta | critical | - |
| F-TANDA-R007 | Una tanda interrumpida se retoma donde quedó | major | - |
| F-TANDA-R008 | Antes de publicar, cualquier cambio se puede sacar | major | - |
| F-TANDA-R009 | Publicar aplica todo junto, y sólo así cambia el curso | critical | - |
| F-TANDA-R010 | El informe de la tanda cierra sus cuentas | major | - |
| F-TANDA-R011 | Abrir una tanda fija sus metas | critical | - |
| F-TANDA-R012 | Lo que se pidió regrabar queda pendiente, y no vuelve a la mesa | critical | - |

**User Journeys:**

- **F-TANDA-J001**: Correr y publicar la tanda de un nivel

### FEAT-BANDEJA: La bandeja - lo único que la tanda le deja a José, y un solo OK para publicar [F-BANDEJA]

> **Qué**: Una sola bandeja por tanda junta lo que la tanda no puede decidir sola —desempates de los jueces, ejercicios sin
ganador y dos muestras para mirar— y José publica la tanda con un solo OK.

**Por qué**: Hoy se aprueba de a una propuesta, en tres lugares que usan planes distintos: unos 2.900 cambios serían
2.900 aprobaciones, y el criterio de José se gastaría en lo que una regla ya resolvió.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-BANDEJA-R001 | Una sola bandeja por tanda, con todo lo que necesita a José | critical | - |
| F-BANDEJA-R002 | Un desempate muestra los dos veredictos y lo resuelve José | critical | - |
| F-BANDEJA-R003 | Un ejercicio sin ganador muestra lo que se intentó y lo decide José | critical | - |
| F-BANDEJA-R004 | Cada regla se aprueba mirando una muestra | critical | - |
| F-BANDEJA-R005 | La muestra ciega del paquete F se responde desde la bandeja | critical | - |
| F-BANDEJA-R006 | Lo que José decide no vuelve mientras el ejercicio no cambie | major | - |
| F-BANDEJA-R007 | Lo pendiente no frena lo que no lo necesita | major | - |
| F-BANDEJA-R008 | Publicar es un solo OK, y sólo desde la bandeja | critical | La propuesta <id> es de la tanda <id>: entra al curso al publicar la tanda. |
| F-BANDEJA-R009 | La bandeja muestra lo que hay que regrabar | major | - |

**User Journeys:**

- **F-BANDEJA-J001**: Vaciar la bandeja y publicar la tanda

### FEAT-CALUM: El juez de consigna lee el ejercicio como lo ve el alumno [F-CALUM]

> **Qué**: El juez de consigna recibe el ejercicio armado con la vista del alumno (F-RONDA1-R001), como
los otros cuatro jueces:
- el título y la consigna que muestra la app, que son los de la copia del ejercicio y no los del tema;
- el enunciado con «___» y la pista;
- las respuestas aceptadas de cada hueco;
- en la opción múltiple, sus opciones con la correcta marcada.

El nivel y el topic siguen como contexto. Vale igual en `revise-instructions`, para el original y para el candidato, y
el corrector que arma el candidato apunta a esa misma consigna.

**Por qué**: La app muestra la consigna de la copia y el juez leía la del tema. En la base del 29/9 difieren en 9.923 de
11.287 ejercicios, así que el juez marcaba ejercicios por una consigna que el alumno nunca ve. Es la regla de José para
todo juez: lee el ejercicio entero como lo ve el alumno (gancho C-07).

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-CALUM-R001 | El juez de consigna lee el ejercicio como lo ve el alumno | critical | - |
| F-CALUM-R002 | `revise-instructions` juzga el original y el candidato como los verá el alumno | critical | - |
| F-CALUM-R003 | El corrector de consignas apunta a la consigna que ve el alumno | critical | - |

