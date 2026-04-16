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

- **F-LREC-J002**: Identificar lemas sub-expuestos que el estudiante podria olvidar

- **F-LREC-J003**: Identificar lemas sobre-expuestos que saturan al estudiante

- **F-LREC-J004**: Detectar lemas con distribucion irregular usando la desviacion estandar

- **F-LREC-J005**: Comparar la recurrencia antes y despues de ajustar el contenido

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
| F-COCA-R004 | Tokens sin ranking de frecuencia | major | Proporcion alta de tokens sin ranking de frecuencia en el nivel {nivel}: {porcentaje}% de tokens excluidos del analisis |
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
| F-COCA-R033 | Directivas a nivel de trimestre (estrategia quarters) | minor | - |
| F-COCA-R034 | Contenido de las directivas de mejora | minor | - |

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

- **F-COCA-J006**: Comparar resultados entre estrategia levels y quarters
  1. El usuario ejecuta la auditoria con la estrategia LEVELS
  2. El usuario observa que el nivel A2 tiene estado OPTIMO con puntuacion 0.95
  3. El usuario cambia la configuracion a la estrategia QUARTERS y re-ejecuta la auditoria
  4. El usuario descubre que si bien A2 en promedio es adecuado, Q1 de A2 tiene exceso de vocabulario frecuente (top1k al 85% cuando el objetivo Q1 es 75% atLeast) y Q4 tiene deficit de vocabulario variado (top4k al 5% cuando el objetivo Q4 es 15% atMost)
  5. El usuario concluye que la estrategia quarters proporciona informacion mas granular y util para mejorar el contenido dentro de cada nivel

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
| F-NLP-R018 | Comunicacion con el proceso Python via archivos JSON | major | Error en la comunicacion con el proceso Python: no se pudo leer/escribir el archivo {ruta} |
| F-NLP-R019 | El proceso Python integra lematizacion y busqueda de frecuencia | critical | - |
| F-NLP-R020 | Requisitos del entorno de ejecucion | major | Requisito de entorno faltante: {componente}. Consulte la documentacion de instalacion. |
| F-NLP-R021 | La ruta del script Python y de los datos COCA es configurable | minor | Ruta del script Python no configurada o no encontrada: {ruta} |
| F-NLP-R022 | El procesamiento es sincrono | minor | - |
| F-NLP-R023 | Cache en memoria de tokens enriquecidos por oracion | major | - |
| F-NLP-R024 | El cache usa la oracion completa como clave | minor | - |
| F-NLP-R025 | Cache persistente opcional en disco | minor | - |
| F-NLP-R026 | El cache del CachedNlpTokenizer debe evolucionar | major | - |
| F-NLP-R027 | Volumetria del cache | minor | - |
| F-NLP-R028 | Tokens sin ranking de frecuencia | major | - |
| F-NLP-R029 | Fallo del proceso Python | critical | El procesamiento NLP fallo: {causa}. Verifique que Python 3.11+, SpaCy y el modelo en_core_web_sm estan instalados. |
| F-NLP-R030 | Fallback cuando SpaCy no esta disponible | major | SpaCy no disponible: los analizadores que requieren datos linguisticos enriquecidos no se ejecutaran. Los analizadores basicos (longitud de oracion) funcionan normalmente. |
| F-NLP-R031 | Timeout del proceso Python | major | El procesamiento NLP excedio el tiempo limite de {timeout} segundos. Considere procesar en lotes mas pequenos o aumentar el timeout. |
| F-NLP-R032 | Errores de tokens individuales no detienen el lote completo | major | Error al procesar el token '{text}' en la oracion '{oracion}': {detalle}. Se uso fallback basico. |
| F-NLP-R033 | Signos de puntuacion y espacios no participan como tokens linguisticos | minor | - |

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
  2. El sistema intenta invocar el proceso Python para la tokenizacion enriquecida
  3. El proceso falla porque Python no esta disponible (o SpaCy no esta instalado)
  4. El sistema reporta un mensaje claro: "SpaCy no disponible. Los analizadores que requieren datos linguisticos (distribucion COCA) no se ejecutaran."
  5. El sistema ejecuta la auditoria con funcionalidad reducida: solo los analizadores que no requieren tokens enriquecidos (como el analizador de longitud de oraciones) producen resultados
  6. El usuario recibe los resultados parciales con una nota indicando que analizadores no se ejecutaron y por que
  7. El usuario consulta la documentacion para instalar Python, SpaCy y el modelo requerido

- **F-NLP-J003**: Diagnosticar problemas de tokenizacion en una oracion especifica
  1. El usuario ejecuta la auditoria y observa resultados inesperados en el analizador COCA para un knowledge especifico
  2. El usuario consulta los tokens enriquecidos de una oracion problematica
  3. El usuario observa que un token tiene `frequencyRank` nulo cuando esperaba un valor
  4. El usuario verifica el lema y el POS del token para entender por que la busqueda COCA fallo
  5. El usuario descubre que el lema fue asignado incorrectamente por SpaCy (por ejemplo, "lead" como sustantivo en lugar de verbo) lo que provoco que la busqueda por lema+POS no encontrara resultado
  6. El usuario anota el caso como una limitacion del modelo de SpaCy y decide si ajustar el contenido o aceptar la limitacion

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
| F-LABS-R020 | Oraciones sin lemas mal ubicados | minor | - |
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
| F-KTLEN-R004 | Los pesos de caracteres no son configurables | minor | - |
| F-KTLEN-R005 | Limites de longitud de instrucciones | critical | Configuracion de limites de instrucciones invalida: el limite suave ({soft}) debe ser menor que el limite duro ({hard}) |
| F-KTLEN-R006 | Puntuacion de longitud de instrucciones | critical | Puntuacion fuera de rango [0.0, 1.0] calculada para las instrucciones del knowledge {knowledgeId}: {puntuacion} |
| F-KTLEN-R007 | Los limites de instrucciones no son configurables | minor | - |
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
| F-SLEN-R006 | Calculo del promedio de longitud por nivel | critical | Error al calcular el promedio de longitud para el nivel {nivel}: {detalle} |
| F-SLEN-R007 | Evaluacion de estado por nivel | critical | Estado indeterminado para el nivel {nivel}: no se pudo evaluar el promedio {promedio} contra los rangos [{min}, {max}] |
| F-SLEN-R010 | Evaluacion de progresion entre niveles | major | No se pudo evaluar la progresion: {detalle} |
| F-SLEN-R011 | Progresion evalua solo niveles con datos | major | - |
| F-SLEN-R014 | Generacion de recomendaciones por nivel | minor | - |
| F-SLEN-R015 | Registro de estadisticas por nivel | major | Estadisticas incompletas para el nivel {nivel}: falta el campo {campo} |

**User Journeys:**

- **F-SLEN-J001**: Auditar la longitud de oraciones de un curso completo
  1. El usuario inicia una auditoria de un curso previamente cargado en el sistema
  2. El sistema recorre la jerarquia del curso de arriba hacia abajo: para cada nivel (milestone), sus topics, sus knowledges, y sus quizzes
  3. Para cada quiz, el analizador de longitud determina si es una oracion valida; los quizzes que no son oraciones se excluyen del analisis
  4. Para cada quiz valido, el analizador cuenta los tokens linguisticos de su oracion y calcula su puntuacion individual comparando la longitud contra el rango objetivo de su nivel CEFR (R002)
  5. La plataforma agrega las puntuaciones de quizzes hacia arriba a traves de la jerarquia: para cada knowledge calcula el promedio de sus quizzes (R003), para cada topic el promedio de sus knowledges (R004), para cada nivel el promedio de sus topics (R005), y para el curso el promedio de sus niveles (R008)
  6. El sistema calcula estadisticas especificas de longitud por nivel: promedio de longitud en tokens (R006), estado respecto a los rangos objetivo (R007), y recomendaciones (R014)
  7. El sistema evalua la progresion de longitud entre niveles (R010)
  8. El usuario recibe un informe con la puntuacion general, las puntuaciones y estadisticas por nivel, el estado de progresion, y la posibilidad de profundizar en topics, knowledges y quizzes individuales

- **F-SLEN-J002**: Consultar el detalle de un nivel especifico
  1. El usuario ha ejecutado la auditoria de longitud de oraciones (J001)
  2. El usuario selecciona un nivel CEFR especifico (por ejemplo, A2)
  3. El sistema muestra las estadisticas detalladas del nivel: puntuacion del nivel, cantidad de quizzes validos, cantidad de quizzes excluidos, total de tokens, promedio de longitud, rango objetivo, estado y recomendacion (R015)
  4. El sistema muestra la lista de topics del nivel con su puntuacion individual, permitiendo identificar que topics contribuyen a una puntuacion baja
  5. El usuario puede identificar si el nivel esta dentro de los parametros esperados o si requiere ajustes en el contenido

- **F-SLEN-J003**: Identificar problemas de progresion entre niveles
  1. El usuario ha ejecutado la auditoria de longitud de oraciones (J001)
  2. El usuario consulta el estado de progresion del curso
  3. Si la progresion es POSITIVA, el usuario confirma que la dificultad escala correctamente a traves de los niveles
  4. Si la progresion es REGRESIVA o ESTANCADA, el usuario identifica que hay niveles donde las oraciones no aumentan de longitud como se espera pedagogicamente
  5. El usuario revisa los promedios de longitud por nivel para localizar en que transicion (por ejemplo, A2 a B1) se rompe la progresion
  6. El usuario profundiza en el nivel problematico, revisando las puntuaciones por topic y knowledge para localizar donde se concentran las oraciones con longitud inadecuada
  7. El usuario puede tomar acciones correctivas sobre el contenido de los knowledges y quizzes afectados

- **F-SLEN-J004**: Navegar la jerarquia para localizar problemas de longitud
  1. El usuario ha ejecutado la auditoria de longitud de oraciones (J001)
  2. El usuario observa que un nivel (por ejemplo, B1) tiene una puntuacion baja
  3. El usuario profundiza en los topics del nivel B1 y encuentra que el topic "Modal Verbs" tiene la puntuacion mas baja
  4. El usuario profundiza en los knowledges de "Modal Verbs" y encuentra que el knowledge "Should for advice" tiene puntuacion 0.4
  5. El usuario revisa los quizzes de "Should for advice" y encuentra que varias oraciones tienen 18 tokens (demasiado largas para B1, rango 11-14)
  6. El usuario identifica exactamente que oraciones necesitan simplificarse y puede tomar acciones correctivas puntuales

- **F-SLEN-J005**: Ajustar rangos objetivo para un curso distinto
  1. El usuario tiene un curso con caracteristicas diferentes al curso estandar (por ejemplo, un curso para ninos o un curso intensivo)
  2. El usuario modifica los rangos objetivo de longitud por nivel en la configuracion del sistema
  3. El usuario ejecuta la auditoria con los nuevos rangos
  4. Los resultados reflejan los rangos actualizados: las puntuaciones de quizzes se recalculan con los nuevos valores (R002), la plataforma reagrega las puntuaciones a traves de la jerarquia (R003-R005-R008), y las estadisticas y recomendaciones por nivel se actualizan (R006, R007, R014)
  5. El usuario valida que los rangos ajustados se alinean con las expectativas pedagogicas del nuevo curso

### FEAT-LCOUNT: Analisis de Conteo de Apariciones de Lemas EVP [F-LCOUNT]

> Evaluar si cada lema esperado del catalogo EVP (English Vocabulary Profile) aparece un numero adecuado de veces a lo largo del curso, clasificando cada lema segun su nivel de exposicion (ausente, sub-expuesto, normal, sobre-expuesto) y generando puntuaciones que reflejan la adecuacion de la frecuencia de aparicion. Las puntuaciones individuales por lema se agregan a traves de la jerarquia del curso mediante el motor de agregacion generico de la plataforma ContentAudit, permitiendo localizar donde se concentran los problemas de exposicion de vocabulario.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-LCOUNT-R001 | Conteo de apariciones por lema esperado | critical | Error al contar apariciones del lema {lema} ({parteOracion}) en el nivel {nivel}: {detalle} |
| F-LCOUNT-R002 | El conteo abarca todo el curso, no solo el nivel del lema | critical | - |
| F-LCOUNT-R003 | Clasificacion de lemas por nivel de exposicion | critical | Estado de exposicion indeterminado para el lema {lema} ({parteOracion}): apariciones = {count}, umbrales = [{min}, {max}] |
| F-LCOUNT-R004 | Los lemas esperados se obtienen del catalogo EVP | critical | No se encontraron lemas esperados en el catalogo EVP para el nivel {nivel} |
| F-LCOUNT-R005 | Los datos de lematizacion provienen de procesamiento linguistico previo | critical | Token sin datos de lematizacion en el quiz {quizId}: el procesamiento linguistico previo no asocio lema |
| F-LCOUNT-R006 | Puntuacion individual por lema | critical | Puntuacion fuera de rango [0.0, 1.0] calculada para el lema {lema} ({parteOracion}): {puntuacion} |
| F-LCOUNT-R007 | Puntuacion por oracion (quiz) | critical | Error al calcular la puntuacion de la oracion del quiz {quizId} en el knowledge {knowledgeId}: {detalle} |
| F-LCOUNT-R008 | Puntuacion general del analisis (overall score) | critical | Error al calcular la puntuacion general: {detalle} |
| F-LCOUNT-R009 | Exclusion de quizzes que no son oraciones | critical | Se incluyo un quiz no-oracion en el conteo de lemas del knowledge {knowledgeId} |
| F-LCOUNT-R010 | Resultado detallado por lema | major | - |
| F-LCOUNT-R011 | Agregacion a traves de la jerarquia (provista por la plataforma) | critical | - |
| F-LCOUNT-R012 | Nombre del analizador en el informe | major | - |
| F-LCOUNT-R013 | Los umbrales de exposicion no son configurables en esta version | minor | - |

**User Journeys:**

- **F-LCOUNT-J001**: Auditar el conteo de apariciones de lemas EVP de un curso completo
  1. El usuario inicia una auditoria de un curso previamente cargado en el sistema
  2. El sistema recorre todas las oraciones del curso, identificando en cada una los tokens lematizados
  3. Para cada nivel CEFR, el analizador obtiene los lemas esperados del catalogo EVP
  4. El analizador cuenta cuantas veces aparece cada lema esperado en el conjunto completo de oraciones del curso
  5. El analizador clasifica cada lema segun su nivel de exposicion (ausente, sub-expuesto, normal, sobre-expuesto) y le asigna una puntuacion individual
  6. El analizador calcula la puntuacion de cada oracion como el promedio de las puntuaciones de los lemas esperados que contiene
  7. La plataforma agrega las puntuaciones de oraciones hacia arriba a traves de la jerarquia: para cada knowledge calcula el promedio de sus quizzes, para cada topic el promedio de sus knowledges, para cada nivel el promedio de sus topics, y para el curso el promedio de sus niveles
  8. El usuario recibe un informe con la puntuacion general, las puntuaciones por nivel de la jerarquia, y el detalle de la clasificacion de cada lema

- **F-LCOUNT-J002**: Identificar lemas con exposicion insuficiente
  1. El usuario ha ejecutado la auditoria de conteo de lemas (J001)
  2. El usuario observa que la puntuacion general del analizador "lemma-count" es baja (por ejemplo, 0.65)
  3. El usuario consulta el detalle de lemas por nivel y filtra por estado "ausente" y "sub-expuesto"
  4. El usuario identifica los lemas que requieren mas apariciones en el curso, priorizando los ausentes (puntuacion 0.0) sobre los sub-expuestos
  5. El usuario puede tomar acciones correctivas: agregar oraciones que incluyan los lemas faltantes o sub-expuestos, o modificar oraciones existentes para incorporarlos

- **F-LCOUNT-J003**: Localizar problemas de exposicion en la jerarquia del curso
  1. El usuario ha ejecutado la auditoria de conteo de lemas (J001)
  2. El usuario observa que un nivel (por ejemplo, B1) tiene una puntuacion baja en el analizador "lemma-count"
  3. El usuario profundiza en los topics del nivel B1 y encuentra que el topic "Travel Vocabulary" tiene la puntuacion mas baja
  4. El usuario profundiza en los knowledges de "Travel Vocabulary" y revisa las puntuaciones por quiz
  5. El usuario identifica que varias oraciones contienen unicamente lemas sobre-expuestos (que aportan puntuaciones inferiores a 1.0) y ningun lema sub-expuesto o ausente que podria incorporarse
  6. El usuario reorganiza el contenido para distribuir mejor los lemas que necesitan mas exposicion

- **F-LCOUNT-J004**: Comparar exposicion de vocabulario entre niveles
  1. El usuario ha ejecutado la auditoria de conteo de lemas (J001)
  2. El usuario consulta las puntuaciones del analizador "lemma-count" por nivel CEFR
  3. El usuario compara la proporcion de lemas ausentes y sub-expuestos entre niveles
  4. El usuario identifica si los problemas de exposicion se concentran en un nivel especifico (por ejemplo, B2 tiene muchos lemas ausentes porque el contenido de B2 es menos extenso) o se distribuyen uniformemente
  5. El usuario prioriza las acciones correctivas en los niveles con peor cobertura de vocabulario

- **F-LCOUNT-J005**: Revisar lemas sobre-expuestos para optimizar el contenido
  1. El usuario ha ejecutado la auditoria de conteo de lemas (J001)
  2. El usuario filtra los lemas con estado "sobre-expuesto" y revisa cuales son
  3. El usuario encuentra que lemas como "be" y "have" (nivel A1) tienen mas de 100 apariciones cada uno
  4. El usuario evalua si estas sobre-exposiciones son realmente problematicas (verbos basicos que naturalmente aparecen con alta frecuencia) o si indican contenido repetitivo que podria mejorarse
  5. El usuario decide si las sobre-exposiciones de verbos basicos son aceptables o si necesitan revision, considerando que la penalizacion por sobre-exposicion puede producir falsos positivos para vocabulario de muy alta frecuencia

### FEAT-RCLA: Re-routing y Contexto de Correccion para LEMMA_ABSENCE en el Refiner [F-RCLA]

> El refiner actualmente genera tareas LEMMA_ABSENCE a nivel MILESTONE y COURSE. Estas tareas no son accionables: dicen "al nivel B1 le faltan estos lemas" pero no apuntan a un quiz especifico que un LLM pueda corregir. Mientras tanto, el analyzer ya detecta vocabulario fuera de nivel en quizzes individuales (un quiz de A1 que usa una palabra de B2), pero el refiner ignora esos hallazgos.

Este requerimiento hace dos cosas: (1) re-rutea las tareas LEMMA_ABSENCE para que apunten a quizzes individuales en lugar de milestones/cursos, y (2) agrega el contexto de correccion que un LLM necesita para reescribir la oracion reemplazando el vocabulario fuera de nivel, incluyendo sugerencias de lemas ausentes del nivel que pueden servir como reemplazo.

**Business Rules:**

| ID | Rule | Severity | Error Message |
|----|------|----------|---------------|
| F-RCLA-R001 | Las tareas LEMMA_ABSENCE deben apuntar a QUIZ, no a MILESTONE ni COURSE | critical | - |
| F-RCLA-R002 | El analyzer no se modifica | major | - |
| F-RCLA-R003 | Estructura del contexto de correccion para LEMMA_ABSENCE | critical | - |
| F-RCLA-R004 | Estructura de cada lema fuera de nivel | critical | - |
| F-RCLA-R004b | Obtencion de lemas sugeridos como candidatos de reemplazo | critical | - |
| F-RCLA-R004c | Contexto sin lemas sugeridos | major | - |
| F-RCLA-R005 | Resolucion del nodo quiz desde una tarea de refinamiento | critical | No se pudo cargar el reporte de auditoria '{sourceAuditId}' necesario para construir el contexto de correccion |
| F-RCLA-R006 | Contexto cuando el diagnostico de placement no esta disponible | major | No se pudo obtener el diagnostico de placement de lemas para el quiz '{nodeId}' |
| F-RCLA-R007 | El comando refiner next incluye el contexto de correccion para tareas LEMMA_ABSENCE | critical | - |
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
| F-RCSL-R006 | El comando refiner next incluye el contexto de correccion para tareas SENTENCE_LENGTH | critical | - |
| F-RCSL-R007 | Formato JSON del contexto de correccion | critical | No se pudo construir el contexto de correccion: {motivo} |
| F-RCSL-R008 | Formato texto del contexto de correccion | major | - |

**User Journeys:**

- **F-RCSL-J001**: LLM recibe contexto para corregir una oracion demasiado larga

- **F-RCSL-J002**: LLM recibe contexto para corregir una oracion demasiado corta

- **F-RCSL-J003**: Contexto de correccion sin lemas sugeridos disponibles

