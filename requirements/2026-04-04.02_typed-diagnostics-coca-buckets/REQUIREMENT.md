---
feature:
  id: FEAT-DCOCA
  code: F-DCOCA
  name: Diagnosticos Tipados para el Analizador de Distribucion COCA
  priority: critical
---

# Diagnosticos Tipados para el Analizador de Distribucion COCA

Reemplazar los resultados no tipados (`Map<String, Object> metadata`) que el analizador de distribucion COCA (FEAT-COCA) emite en cada nodo del arbol de auditoria por **registros de diagnostico tipados** que describan de forma explicita la informacion producida en cada nivel de la jerarquia del curso. Este es el segundo paso de la iniciativa de diagnosticos tipados iniciada por FEAT-DLABS, y aplica el mismo patron al analizador coca-buckets-distribution.

## Contexto

### Relacion con FEAT-DLABS

FEAT-DLABS establecio toda la infraestructura necesaria para los diagnosticos tipados:

- La interfaz sellada `NodeDiagnoses` con sub-interfaces por nivel (`CourseDiagnoses`, `LevelDiagnoses`, `TopicDiagnoses`, `KnowledgeDiagnoses`, `QuizDiagnoses`).
- El campo `diagnoses` en `AuditNode` y el mecanismo `ancestor(AuditTarget)` para navegacion entre niveles.
- Las implementaciones por defecto (`DefaultCourseDiagnoses`, `DefaultLevelDiagnoses`, etc.) con getters opcionales tipados.
- El primer analizador migrado (lemma-absence) como ejemplo del patron.

Este requerimiento **no modifica** dicha infraestructura. Solo agrega nuevos metodos a las interfaces por nivel y define los registros de diagnostico especificos del analizador coca-buckets-distribution. Las reglas del Grupo A y Grupo C de FEAT-DLABS (R001-R003, R011-R012) aplican directamente y no se repiten aqui.

### Relacion con FEAT-COCA

Este requerimiento **no modifica** lo que el analizador calcula. Todas las reglas de negocio de FEAT-COCA permanecen identicas: la clasificacion de tokens en bandas, el scoring por bucket, la evaluacion por quarters, la progresion entre niveles y las directivas de mejora siguen funcionando de la misma manera. Lo que cambia es **como** el analizador expone sus resultados: en lugar de escribir claves arbitrarias en un mapa generico, emite registros de diagnostico tipados.

### Modelo actual: datos sin tipo en el mapa generico

El analizador coca-buckets-distribution emite actualmente datos sin tipo en tres niveles de la jerarquia:

**Nivel curso** (clave "coca-buckets-distribution"): Una puntuacion global (`overallScore`), una lista de evaluaciones de progresion por banda (`progressionAssessments`) y una lista de directivas de mejora (`improvementDirectives`). Estos datos provienen de la fase de post-procesamiento de FEAT-COCA (evaluacion de progresion y planificacion de mejoras).

**Nivel milestone** (clave "coca-buckets-distribution"): Total de tokens analizados (`totalTokens`), la lista de resultados por banda de frecuencia (`buckets`) con conteo, porcentaje, objetivo, score y assessment, y la lista de resultados por trimestre (`quarters`) con su indice, score y buckets internos. Estos datos provienen de la fase de analisis de FEAT-COCA.

**Nivel topic** (clave "coca-buckets-distribution"): Total de tokens analizados (`totalTokens`) y la lista de resultados por banda (`buckets`) con conteo y porcentaje (sin targets ni scores, ya que el topic muestra la distribucion acumulada sin evaluacion contra objetivos).

El analizador **no** emite datos a nivel knowledge ni quiz porque la unidad natural de evaluacion de distribucion COCA es el nivel/trimestre, no el quiz individual (un quiz con 8 tokens no tiene una distribucion porcentual significativa).

### Modelo deseado: registros de diagnostico tipados

Los registros de diagnostico reutilizan modelos que ya existen en el dominio del analizador COCA. A diferencia de FEAT-DLABS (donde los modelos `AbsentLemma`, `AbsenceType` y `Priority` ya existian como modelos de dominio), en este caso la reutilizacion es aun mas directa porque existen modelos como `BucketResult`, `QuarterResult`, `ProgressionAssessment`, `ImprovementDirective`, `AssessmentState`, `ImprovementDirectiveType` y `ProgressionState` que ya contienen exactamente los campos necesarios.

Los registros de diagnostico son envolventes ligeros: agrupan los modelos existentes en una estructura de diagnostico por nivel que implementa la interfaz correspondiente del sistema de diagnosticos.

### Consumidor principal: CocaBucketsDetailedFormatter

El formateador `CocaBucketsDetailedFormatter` es el principal consumidor actual de los datos del analizador. Accede a `AuditNode.getMetadata()` y realiza multiples conversiones inseguras con `@SuppressWarnings("unchecked")` para leer listas de mapas. Tras esta migracion, el formateador accedara a los registros tipados a traves de las interfaces de diagnostico, eliminando todas las conversiones inseguras.

---

## Reglas de Negocio

Las reglas se organizan en dos grupos:

- **Grupo A - Registros de diagnostico del analizador coca-buckets (R001-R005)**: reglas que definen los registros tipados especificos que emite el analizador en cada nivel de la jerarquia.
- **Grupo B - Migracion de consumidores (R006-R007)**: reglas que definen como los consumidores existentes deben adaptarse al nuevo modelo.

Las reglas de infraestructura compartida (interfaz sellada, mapa de diagnosticos, acceso tipado, navegacion de ancestros) estan definidas en FEAT-DLABS Grupo A (R001-R003) y Grupo C (R011-R012). Aplican directamente a este analizador sin modificacion.

---

### Grupo A - Registros de diagnostico del analizador coca-buckets

### Rule[F-DCOCA-R001] - Registro de diagnostico a nivel curso: CocaProgressionDiagnosis
**Severity**: critical | **Validation**: AUTO_VALIDATED

En el nodo de curso del arbol de auditoria, el analizador coca-buckets-distribution debe emitir un registro `CocaProgressionDiagnosis` que contiene:

| Campo | Tipo | Descripcion | Regla FEAT-COCA |
|-------|------|-------------|-----------------|
| overallScore | Decimal | Puntuacion global del analisis COCA | R018, R019 |
| progressionAssessments | Lista de ProgressionAssessment | Evaluacion de la progresion por banda de frecuencia entre niveles | R022, R023 |
| improvementDirectives | Lista de ImprovementDirective | Directivas de mejora indicando que bandas necesitan ajustes en que niveles | R024, R025, R026 |

`ProgressionAssessment` es un modelo existente en el dominio COCA con los campos: `bandName` (nombre de la banda), `actualProgression` (progresion real, tipo `ProgressionState`), `expectedProgression` (progresion esperada, tipo `ProgressionState`) y `matches` (si la progresion real coincide con la esperada).

`ImprovementDirective` es un modelo existente en el dominio COCA con los campos: `type` (tipo `ImprovementDirectiveType`: ENRICH o REDUCE), `bandName`, `levelName`, `frequencyRangeFrom` (entero), `frequencyRangeTo` (entero), `actualPercentage` (decimal) y `targetPercentage` (decimal).

**Error**: N/A (esta regla define la estructura de un registro)

### Rule[F-DCOCA-R002] - Registro de diagnostico a nivel milestone: CocaBucketsLevelDiagnosis
**Severity**: critical | **Validation**: AUTO_VALIDATED

En cada nodo de nivel (milestone) del arbol de auditoria, el analizador coca-buckets-distribution debe emitir un registro `CocaBucketsLevelDiagnosis` que contiene:

| Campo | Tipo | Descripcion | Regla FEAT-COCA |
|-------|------|-------------|-----------------|
| totalTokens | Entero | Cantidad total de tokens clasificados en este nivel | R005 |
| buckets | Lista de BucketResult | Resultado por banda de frecuencia con conteo, porcentaje, objetivo, score y assessment | R006, R012, R013 |
| quarters | Lista de QuarterResult | Resultado por trimestre con indice, score y buckets internos | R010, R011 |

`BucketResult` es un modelo existente en el dominio COCA con los campos: `bandName`, `count` (entero), `percentage` (decimal), `targetPercentage` (decimal), `score` (decimal) y `assessment` (tipo `AssessmentState`: OPTIMAL, ADEQUATE, DEFICIENT, EXCESSIVE).

`QuarterResult` es un modelo existente en el dominio COCA con los campos: `index` (entero), `bucketResults` (lista de `BucketResult`) y `score` (decimal).

**Error**: N/A (esta regla define la estructura de un registro)

### Rule[F-DCOCA-R003] - Registro de diagnostico a nivel topic: CocaBucketsTopicDiagnosis
**Severity**: critical | **Validation**: AUTO_VALIDATED

En cada nodo de topic del arbol de auditoria, el analizador coca-buckets-distribution debe emitir un registro `CocaBucketsTopicDiagnosis` que contiene:

| Campo | Tipo | Descripcion | Regla FEAT-COCA |
|-------|------|-------------|-----------------|
| totalTokens | Entero | Cantidad total de tokens clasificados en este topic | R005 |
| buckets | Lista de BucketSummary | Resumen de distribucion por banda con conteo y porcentaje (sin evaluacion contra objetivos) | R005, R006 |

`BucketSummary` es un registro nuevo y mas simple que `BucketResult`. A nivel de topic, la distribucion se muestra como referencia informativa sin evaluacion contra objetivos, por lo que no incluye `targetPercentage`, `score` ni `assessment`. Sus campos son:

| Campo | Tipo | Descripcion |
|-------|------|-------------|
| bandName | Texto | Nombre de la banda de frecuencia (ej: top1k, top2k) |
| count | Entero | Cantidad de tokens clasificados en esta banda |
| percentage | Decimal | Porcentaje de tokens en esta banda respecto al total del topic |

**Error**: N/A (esta regla define la estructura de un registro)

### Rule[F-DCOCA-R004] - Ausencia de diagnostico en niveles knowledge y quiz
**Severity**: major | **Validation**: AUTO_VALIDATED

El analizador coca-buckets-distribution **no** emite diagnosticos a nivel knowledge ni a nivel quiz. Esto es consistente con el diseno de FEAT-COCA: la unidad natural de evaluacion de distribucion de vocabulario es el nivel (o trimestre), no el quiz individual. Los tokens se acumulan desde quiz hacia arriba en la jerarquia, y recien a nivel de topic se presenta la distribucion acumulada como referencia informativa, y a nivel de milestone se evalua contra objetivos.

En consecuencia, las interfaces `KnowledgeDiagnoses` y `QuizDiagnoses` **no** reciben un metodo `getCocaBucketsDiagnosis()`. Solicitar datos de coca-buckets en esos niveles no tiene sentido funcional.

**Error**: N/A (esta regla define una restriccion de alcance)

### Rule[F-DCOCA-R005] - Nuevos metodos en las interfaces de diagnostico por nivel
**Severity**: critical | **Validation**: AUTO_VALIDATED

Las interfaces de diagnostico por nivel deben extenderse con metodos tipados para acceder al diagnostico de coca-buckets:

| Interfaz | Nuevo metodo | Tipo de retorno |
|----------|-------------|-----------------|
| CourseDiagnoses | getCocaBucketsDiagnosis() | Opcional de CocaProgressionDiagnosis |
| LevelDiagnoses | getCocaBucketsDiagnosis() | Opcional de CocaBucketsLevelDiagnosis |
| TopicDiagnoses | getCocaBucketsDiagnosis() | Opcional de CocaBucketsTopicDiagnosis |

Estos metodos siguen el mismo patron establecido por `getLemmaAbsenceDiagnosis()` en FEAT-DLABS: retornan un valor opcional que esta vacio cuando el analizador no se ejecuto sobre ese nodo, y contiene el registro tipado cuando el analizador produjo datos. Las implementaciones por defecto (`DefaultCourseDiagnoses`, `DefaultLevelDiagnoses`, `DefaultTopicDiagnoses`) deben actualizarse para soportar el nuevo metodo.

**Error**: N/A (esta regla define la extension de interfaces existentes)

---

### Grupo B - Migracion de consumidores

### Rule[F-DCOCA-R006] - Migracion del formateador de detalle de coca-buckets
**Severity**: major | **Validation**: AUTO_VALIDATED

El formateador de detalle de coca-buckets (`CocaBucketsDetailedFormatter`) es el principal consumidor actual de los datos del analizador. Tras la migracion, este formateador debe leer los registros de diagnostico tipados en lugar de acceder al mapa generico.

El formateador accede a datos en los siguientes niveles:

| Nivel | Dato que consume | Registro tipado |
|-------|-----------------|-----------------|
| Curso | Puntuacion global, progresion y directivas de mejora | CocaProgressionDiagnosis |
| Milestone | Tokens totales, buckets con targets y quarters | CocaBucketsLevelDiagnosis |
| Topic | Tokens totales y buckets informativos | CocaBucketsTopicDiagnosis |

Tras la migracion, el formateador no debe contener ninguna conversion insegura (`@SuppressWarnings("unchecked")`) ni acceso a `getMetadata()` para datos de coca-buckets. Todos los accesos a datos de diagnostico deben ser a traves de los registros tipados obtenidos via las interfaces de diagnostico (R005).

**Error**: N/A (esta regla describe una obligacion de migracion)

### Rule[F-DCOCA-R007] - Eliminacion del mapa generico para coca-buckets
**Severity**: major | **Validation**: AUTO_VALIDATED

Una vez completada la migracion del analizador coca-buckets-distribution y todos sus consumidores, el analizador no debe escribir datos en el mapa generico sin tipo para la clave "coca-buckets-distribution". Toda la informacion de diagnostico de coca-buckets debe fluir exclusivamente a traves de los registros tipados definidos en este requerimiento (R001-R003).

La misma regla de coexistencia temporal establecida en FEAT-DLABS R014 aplica: otros analizadores que aun no hayan sido migrados pueden seguir usando el mapa generico, pero para coca-buckets la migracion debe ser completa.

**Error**: N/A (esta regla describe una restriccion de migracion)

---

## User Journeys

### Journey[F-DCOCA-J001] - Formatear informe de distribucion COCA usando diagnosticos tipados
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-DCOCA-J001
    name: Formatear informe de distribucion COCA usando diagnosticos tipados
    flow:
      - id: iniciar_formateo
        action: "El formateador de detalle de coca-buckets recibe el arbol de auditoria para generar el informe"
        then: leer_diagnostico_curso

      - id: leer_diagnostico_curso
        action: "El formateador solicita el CocaProgressionDiagnosis del nodo curso para obtener la puntuacion global, las evaluaciones de progresion y las directivas de mejora"
        gate: [F-DCOCA-R001, F-DCOCA-R005]
        outcomes:
          - when: "El diagnostico existe en el nodo curso"
            then: leer_diagnosticos_niveles
          - when: "El diagnostico no existe (el analizador no se ejecuto)"
            then: omitir_seccion

      - id: leer_diagnosticos_niveles
        action: "El formateador itera sobre los nodos de nivel y solicita el CocaBucketsLevelDiagnosis de cada uno para obtener tokens totales, buckets evaluados y quarters"
        gate: [F-DCOCA-R002, F-DCOCA-R005]
        then: leer_diagnosticos_topics

      - id: leer_diagnosticos_topics
        action: "El formateador itera sobre los nodos de topic dentro de cada nivel y solicita el CocaBucketsTopicDiagnosis para obtener la distribucion informativa de tokens por banda"
        gate: [F-DCOCA-R003, F-DCOCA-R005]
        then: generar_informe

      - id: generar_informe
        action: "El formateador genera el informe completo sin ninguna conversion insegura, accediendo a todos los datos a traves de campos tipados"
        gate: [F-DCOCA-R006, F-DCOCA-R007]
        result: success

      - id: omitir_seccion
        action: "El formateador omite la seccion de distribucion COCA porque el analizador no produjo diagnosticos"
        result: success
```

### Journey[F-DCOCA-J002] - Consultar diagnosticos COCA desde el futuro refiner
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-DCOCA-J002
    name: Consultar diagnosticos COCA desde el futuro refiner
    flow:
      - id: identificar_quiz_problematico
        action: "El refiner identifica un quiz que necesita correccion (por ejemplo, oraciones con vocabulario inadecuado para el nivel)"
        then: navegar_a_milestone

      - id: navegar_a_milestone
        action: "El refiner navega desde el quiz hasta su milestone ancestro usando el mecanismo de navegacion del arbol (FEAT-DLABS R011)"
        outcomes:
          - when: "El milestone ancestro existe"
            then: obtener_diagnostico_nivel
          - when: "No se encuentra el milestone ancestro"
            then: error_navegacion

      - id: obtener_diagnostico_nivel
        action: "El refiner obtiene el CocaBucketsLevelDiagnosis del milestone para conocer la distribucion de vocabulario actual y los buckets deficientes del nivel"
        gate: [F-DCOCA-R002, F-DCOCA-R005]
        then: cruzar_con_otros_diagnosticos

      - id: cruzar_con_otros_diagnosticos
        action: "El refiner cruza la informacion de distribucion COCA con los diagnosticos de otros analizadores (por ejemplo, lemas ausentes de lemma-absence) para generar sugerencias de correccion que mejoren multiples dimensiones simultaneamente"
        result: success

      - id: error_navegacion
        action: "El sistema informa que no se pudo navegar al milestone ancestro desde el quiz"
        result: failure
```

---

## Open Questions

### Doubt[DOUBT-BUCKET-SUMMARY] - Tipo separado para buckets de topic vs BucketResult reutilizado
**Status**: OPEN

A nivel de topic, los buckets solo incluyen `bandName`, `count` y `percentage` (sin `targetPercentage`, `score` ni `assessment`). Se propone crear un tipo `BucketSummary` especifico para este nivel.

**Pregunta**: Se debe crear un tipo `BucketSummary` separado para el nivel topic, o reutilizar `BucketResult` con los campos de evaluacion en valores por defecto (score 0.0, assessment null)?

- [x] Opcion A: Crear `BucketSummary` con solo `bandName`, `count` y `percentage`. Mas preciso semanticamente: deja claro que a nivel topic no hay evaluacion contra objetivos. Introduce un tipo nuevo pero simple.
- [ ] Opcion B: Reutilizar `BucketResult` en todos los niveles. A nivel topic, los campos `targetPercentage`, `score` y `assessment` tendrian valores por defecto. Menos tipos, pero la semantica es menos clara: un consumidor podria interpretar incorrectamente que el score 0.0 significa una evaluacion deficiente.
