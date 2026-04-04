---
feature:
  id: FEAT-DLABS
  code: F-DLABS
  name: Diagnosticos Tipados para el Analizador de Ausencia de Lemas
  priority: critical
---

# Diagnosticos Tipados para el Analizador de Ausencia de Lemas

Reemplazar los resultados no tipados (`Map<String, Object> metadata`) que el analizador de ausencia de lemas (FEAT-LABS) emite en cada nodo del arbol de auditoria por **registros de diagnostico tipados** que describan de forma explicita y navegable la informacion producida en cada nivel de la jerarquia del curso. Este cambio es el primer paso de una iniciativa mas amplia para dotar a todos los analizadores del sistema ContentAudit de diagnosticos tipados, y se aplica inicialmente al analizador lemma-absence por ser el que emite la estructura de datos mas compleja y el que mas se beneficia de la tipificacion.

## Contexto

### Iniciativa de diagnosticos tipados

El sistema ContentAudit produce un arbol de auditoria donde cada nodo (curso, nivel, topic, knowledge, quiz) almacena los resultados de cada analizador. Actualmente, estos resultados se almacenan como un mapa generico sin tipo (`metadata`) donde cada analizador deposita sus datos como pares clave-valor sin estructura formal. Los consumidores de estos datos (formateadores de informes, y en el futuro un modulo de correccion automatica denominado "refiner") deben conocer de memoria las claves y los tipos de los valores, y realizar conversiones inseguras para acceder a la informacion.

Esta situacion genera varios problemas concretos:

1. **Fragilidad**: Si un analizador cambia el nombre de una clave o el tipo de un valor, los consumidores fallan en tiempo de ejecucion sin advertencia previa. No hay manera de detectar estas incompatibilidades durante la construccion del sistema.
2. **Legibilidad**: Un desarrollador que quiere entender que datos produce un analizador debe leer el codigo que escribe en el mapa y el codigo que lee del mapa, sin una definicion centralizada.
3. **Navegabilidad**: Las herramientas de desarrollo no pueden ofrecer autocompletado ni navegacion porque los datos son opacos.
4. **Habilitacion del refiner**: El futuro modulo refiner necesitara consumir datos de diagnostico de multiples analizadores para corregir automaticamente problemas de contenido. Por ejemplo, para corregir un quiz con un lema mal ubicado, el refiner necesita el diagnostico de ubicacion del quiz Y el diagnostico de ausencia del nivel ancestro (para saber que lemas de reemplazo estan disponibles). Esto requiere datos tipados y navegacion entre niveles del arbol.
5. **Consumo cruzado entre analizadores**: Cuando el refiner corrija una oracion demasiado corta (detectada por sentence-length), necesitara saber que lemas estan ausentes (de lemma-absence) y que bandas de frecuencia necesitan mas palabras (de coca-buckets). Los diagnosticos tipados hacen que esta consulta cruzada sea directa mediante la navegacion del arbol.

La solucion consiste en reemplazar el mapa generico por un **mapa de diagnosticos tipados**, donde cada analizador emite instancias de registros bien definidos que describen su dominio de datos. Una interfaz sellada (`AnalyzerDiagnosis`) actua como tipo base, y cada analizador define sus propios registros de diagnostico que la implementan.

### Por que lemma-absence es el primer analizador migrado

El analizador de ausencia de lemas (FEAT-LABS) es el candidato ideal para iniciar esta migracion por varias razones:

1. **Complejidad de datos**: Emite datos en cinco niveles distintos de la jerarquia (curso, nivel, topic, knowledge, quiz), con estructuras diferentes en cada nivel. Es el analizador con la estructura de metadatos mas rica y variada.
2. **Consumidores existentes**: El formateador `LemmaAbsenceDetailedFormatter` ya realiza multiples conversiones inseguras para leer los datos de ausencia, lo cual demuestra la necesidad inmediata.
3. **Caso de uso del refiner**: La correccion de lemas mal ubicados es el caso de uso mas complejo del futuro refiner, y requiere navegacion entre niveles del arbol (del quiz al nivel ancestro). Implementar los diagnosticos tipados aqui establece los patrones de navegacion que usaran los demas analizadores.
4. **Diversidad de registros**: La variedad de datos por nivel (assessment global en curso, metricas de cobertura en nivel, conteo de lemas mal ubicados en topic/knowledge, detalle de lemas en quiz) ejercita todos los patrones del sistema de diagnosticos.

### Relacion con FEAT-LABS

Este requerimiento **no modifica** lo que el analizador calcula. Todas las reglas de negocio de FEAT-LABS (R001-R031) permanecen identicas: la clasificacion de ausencias, las prioridades COCA, el scoring por oracion, el assessment global y las recomendaciones siguen funcionando de la misma manera. Lo que cambia es **como** el analizador expone sus resultados: en lugar de escribir claves arbitrarias en un mapa generico, emite registros de diagnostico tipados con campos nombrados y tipos explicitos.

### Modelo actual de datos por nivel

El analizador emite actualmente los siguientes datos sin tipo en cada nivel de la jerarquia:

**Nivel curso**: Una evaluacion global del estado de ausencia de vocabulario (OPTIMAL, ACCEPTABLE, NEEDS_IMPROVEMENT).

**Nivel milestone (nivel CEFR)**: Metricas detalladas de cobertura de vocabulario: totales esperados y ausentes, porcentaje de ausencia, objetivo de cobertura, puntuaciones desglosadas por tipo de ausencia, y la lista completa de lemas ausentes con su lema, parte de la oracion, tipo de ausencia y prioridad.

**Nivel topic**: Conteo de lemas mal ubicados dentro del topic.

**Nivel knowledge**: Conteo de lemas mal ubicados y la lista detallada de lemas con su lema, parte de la oracion y nivel esperado.

**Nivel quiz**: Lista de lemas mal ubicados con su lema, parte de la oracion y nivel esperado.

### Modelo deseado: registros de diagnostico

El analizador debe emitir registros de diagnostico tipados en cada nivel. Cada registro es un objeto con campos nombrados que describen exactamente la informacion disponible:

**En el nodo curso**: Un registro `LemmaAbsenceCourseDiagnosis` que contiene la evaluacion global (`AbsenceAssessment`: OPTIMAL, ACCEPTABLE, NEEDS_IMPROVEMENT).

**En cada nodo de nivel (milestone)**: Un registro `LemmaAbsenceLevelDiagnosis` que contiene: total de lemas esperados, total de lemas ausentes, porcentaje de ausencia, objetivo de cobertura, puntuaciones por tipo de ausencia (completamente ausente, aparece tarde, aparece temprano), y una lista de registros `AbsentLemma`. Cada `AbsentLemma` contiene: el lema, la parte de la oracion, el tipo de ausencia (`AbsenceType`: COMPLETELY_ABSENT, APPEARS_TOO_LATE, APPEARS_TOO_EARLY) y la prioridad (`Priority`: HIGH, MEDIUM, LOW).

**En cada nodo de topic, knowledge y quiz**: Un registro `LemmaPlacementDiagnosis` que contiene: el conteo de lemas mal ubicados y una lista de registros `MisplacedLemma`. Cada `MisplacedLemma` contiene: el lema, la parte de la oracion y el nivel CEFR esperado (`CefrLevel`). Este mismo tipo de registro se reutiliza en los tres niveles porque la informacion estructural es identica.

### Infraestructura compartida: el mapa de diagnosticos y la navegacion del arbol

Para que los registros de diagnostico sean accesibles a los consumidores, el nodo del arbol de auditoria debe proveer:

1. **Mapa de diagnosticos**: En lugar del mapa generico actual, un mapa donde la clave es el nombre del analizador (por ejemplo, "lemma-absence") y el valor es un registro de diagnostico tipado que implementa la interfaz sellada `AnalyzerDiagnosis`.
2. **Acceso tipado**: Un mecanismo para obtener el diagnostico de un analizador especifico con su tipo concreto, evitando que el consumidor deba realizar conversiones manuales.
3. **Navegacion hacia ancestros**: Un mecanismo para navegar desde un nodo hacia su ancestro en un nivel determinado de la jerarquia (por ejemplo, desde un quiz hacia el milestone que lo contiene). Esto es esencial para el caso de uso del refiner: al corregir un quiz con lemas mal ubicados, el refiner necesita consultar el diagnostico de ausencia del milestone para saber que lemas de reemplazo estan disponibles en ese nivel.

---

## Reglas de Negocio

Las reglas se organizan en cuatro grupos:

- **Grupo A - Interfaz de diagnostico y mapa de diagnosticos (R001-R003)**: reglas que definen la infraestructura compartida por todos los analizadores.
- **Grupo B - Registros de diagnostico del analizador lemma-absence (R004-R010)**: reglas que definen los registros tipados especificos que emite el analizador de ausencia de lemas en cada nivel de la jerarquia.
- **Grupo C - Navegacion del arbol de auditoria (R011-R012)**: reglas que definen como los consumidores navegan el arbol para acceder a diagnosticos de nodos ancestros.
- **Grupo D - Migracion de consumidores (R013-R014)**: reglas que definen como los consumidores existentes deben adaptarse al nuevo modelo.

---

### Grupo A - Interfaz de diagnostico y mapa de diagnosticos

### Rule[F-DLABS-R001] - Interfaz sellada AnalyzerDiagnosis
**Severity**: critical | **Validation**: AUTO_VALIDATED

Todos los registros de diagnostico que emiten los analizadores del sistema deben implementar una interfaz comun denominada `AnalyzerDiagnosis`. Esta interfaz actua como tipo base sellado: solo los registros de diagnostico definidos dentro del sistema pueden implementarla. Esto garantiza que el mapa de diagnosticos solo contenga registros conocidos y validables.

La interfaz no define operaciones propias; su proposito es exclusivamente servir como tipo comun para que el mapa de diagnosticos pueda almacenar registros de cualquier analizador de forma segura.

**Error**: N/A (esta regla define una interfaz de tipo)

### Rule[F-DLABS-R002] - Mapa de diagnosticos en el nodo de auditoria
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cada nodo del arbol de auditoria debe contener un **mapa de diagnosticos** donde la clave es el nombre del analizador (cadena de texto) y el valor es un registro que implementa `AnalyzerDiagnosis`. Este mapa reemplaza al mapa generico sin tipo que existe actualmente.

Las claves del mapa corresponden a los nombres de analizadores definidos en sus respectivos requerimientos. Para el analizador de ausencia de lemas, la clave es `"lemma-absence"` (definida en FEAT-LABS R031).

Un nodo puede tener diagnosticos de multiples analizadores simultaneamente (por ejemplo, un nodo de quiz puede tener diagnosticos de lemma-absence, sentence-length y coca-buckets). Cada analizador escribe bajo su propia clave sin interferir con los demas.

**Error**: "No se encontro diagnostico para el analizador '{nombre}' en el nodo {nodoId}"

### Rule[F-DLABS-R003] - Acceso tipado a diagnosticos
**Severity**: critical | **Validation**: AUTO_VALIDATED

El nodo de auditoria debe proveer un mecanismo de acceso tipado que permita a los consumidores obtener el diagnostico de un analizador especifico con su tipo concreto, sin necesidad de realizar conversiones manuales. El consumidor solicita el diagnostico indicando el nombre del analizador y el tipo esperado del registro, y el sistema retorna el registro ya convertido al tipo solicitado.

Si el diagnostico existe pero no corresponde al tipo solicitado, el sistema debe senalar el error de forma clara indicando el tipo esperado y el tipo real encontrado. Si el diagnostico no existe para el analizador solicitado, el sistema debe retornar una indicacion de ausencia (no un error).

**Error**: "El diagnostico del analizador '{nombre}' es de tipo {tipoReal}, pero se solicito {tipoEsperado}"

---

### Grupo B - Registros de diagnostico del analizador lemma-absence

### Rule[F-DLABS-R004] - Registro de diagnostico a nivel curso: LemmaAbsenceCourseDiagnosis
**Severity**: critical | **Validation**: AUTO_VALIDATED

En el nodo de curso del arbol de auditoria, el analizador lemma-absence debe emitir un registro `LemmaAbsenceCourseDiagnosis` que contiene:

| Campo | Tipo | Descripcion | Regla FEAT-LABS |
|-------|------|-------------|-----------------|
| assessment | AbsenceAssessment | Evaluacion global: OPTIMAL, ACCEPTABLE o NEEDS_IMPROVEMENT | R022 |

El valor de `AbsenceAssessment` es una enumeracion con exactamente tres valores posibles, correspondientes a las categorias definidas en FEAT-LABS R022. Este registro es el mas simple del analizador, ya que a nivel de curso solo se emite la evaluacion global.

**Error**: N/A (esta regla define la estructura de un registro)

### Rule[F-DLABS-R005] - Registro de diagnostico a nivel milestone: LemmaAbsenceLevelDiagnosis
**Severity**: critical | **Validation**: AUTO_VALIDATED

En cada nodo de nivel (milestone) del arbol de auditoria, el analizador lemma-absence debe emitir un registro `LemmaAbsenceLevelDiagnosis` que contiene:

| Campo | Tipo | Descripcion | Regla FEAT-LABS |
|-------|------|-------------|-----------------|
| totalExpected | Entero | Cantidad total de lemas esperados para este nivel | R023 |
| totalAbsent | Entero | Cantidad total de lemas ausentes en este nivel | R023 |
| absencePercentage | Decimal | Porcentaje de lemas ausentes respecto al total esperado | R023 |
| coverageTarget | Decimal | Objetivo de cobertura configurado para este nivel | R032 |
| completelyAbsentScore | Decimal | Puntuacion ponderada de lemas completamente ausentes | R008 |
| tooLateScore | Decimal | Puntuacion ponderada de lemas que aparecen demasiado tarde | R008 |
| tooEarlyScore | Decimal | Puntuacion ponderada de lemas que aparecen demasiado temprano | R008 |
| absentLemmas | Lista de AbsentLemma | Lista detallada de cada lema ausente con su clasificacion | R006, R011, R012 |

Este es el registro mas complejo del analizador, ya que consolida todas las metricas de cobertura del nivel junto con el detalle de cada lema ausente. Todos los campos corresponden a datos que el analizador ya calcula segun las reglas de FEAT-LABS; la diferencia es que ahora se exponen como campos tipados en lugar de entradas de un mapa sin tipo.

**Error**: N/A (esta regla define la estructura de un registro)

### Rule[F-DLABS-R006] - Registro auxiliar AbsentLemma
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cada lema ausente dentro de `LemmaAbsenceLevelDiagnosis.absentLemmas` se representa como un registro `AbsentLemma` con los siguientes campos:

| Campo | Tipo | Descripcion | Regla FEAT-LABS |
|-------|------|-------------|-----------------|
| lemma | Texto | Forma base de la palabra ausente | R003 |
| pos | Texto | Parte de la oracion del lema (sustantivo, verbo, etc.) | R001, R015 |
| absenceType | AbsenceType | Tipo de ausencia: COMPLETELY_ABSENT, APPEARS_TOO_LATE o APPEARS_TOO_EARLY | R006, R007 |
| priority | Priority | Nivel de prioridad basado en frecuencia COCA: HIGH, MEDIUM o LOW | R011 |

`AbsenceType` es una enumeracion con exactamente tres valores: COMPLETELY_ABSENT, APPEARS_TOO_LATE, APPEARS_TOO_EARLY, correspondientes a los tipos definidos en FEAT-LABS R006.

`Priority` es una enumeracion con exactamente tres valores: HIGH, MEDIUM, LOW, correspondientes a las bandas de prioridad definidas en FEAT-LABS R011.

**Error**: N/A (esta regla define la estructura de un registro auxiliar)

### Rule[F-DLABS-R007] - Registro de diagnostico a nivel topic: LemmaPlacementDiagnosis
**Severity**: critical | **Validation**: AUTO_VALIDATED

En cada nodo de topic del arbol de auditoria, el analizador lemma-absence debe emitir un registro `LemmaPlacementDiagnosis` que contiene:

| Campo | Tipo | Descripcion | Regla FEAT-LABS |
|-------|------|-------------|-----------------|
| misplacedLemmaCount | Entero | Cantidad de lemas mal ubicados en este topic | R017 |
| misplacedLemmas | Lista de MisplacedLemma | Lista detallada de cada lema mal ubicado | R017 |

Este registro se reutiliza en los niveles topic, knowledge y quiz (R008, R009) porque la estructura de datos de ubicacion de lemas es identica en los tres niveles. La diferencia esta en el alcance: a nivel topic incluye todos los lemas mal ubicados del topic, a nivel knowledge los del knowledge, y a nivel quiz los del quiz individual.

**Error**: N/A (esta regla define la estructura de un registro)

### Rule[F-DLABS-R008] - Reutilizacion de LemmaPlacementDiagnosis en knowledge
**Severity**: critical | **Validation**: AUTO_VALIDATED

En cada nodo de knowledge del arbol de auditoria, el analizador lemma-absence debe emitir el mismo tipo de registro `LemmaPlacementDiagnosis` definido en R007. Los campos son identicos; el contenido refleja los lemas mal ubicados dentro de ese knowledge especifico.

**Error**: N/A (esta regla define la reutilizacion de un registro)

### Rule[F-DLABS-R009] - Reutilizacion de LemmaPlacementDiagnosis en quiz
**Severity**: critical | **Validation**: AUTO_VALIDATED

En cada nodo de quiz del arbol de auditoria, el analizador lemma-absence debe emitir el mismo tipo de registro `LemmaPlacementDiagnosis` definido en R007. Los campos son identicos; el contenido refleja los lemas mal ubicados dentro de ese quiz especifico.

**Error**: N/A (esta regla define la reutilizacion de un registro)

### Rule[F-DLABS-R010] - Registro auxiliar MisplacedLemma
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cada lema mal ubicado dentro de `LemmaPlacementDiagnosis.misplacedLemmas` se representa como un registro `MisplacedLemma` con los siguientes campos:

| Campo | Tipo | Descripcion | Regla FEAT-LABS |
|-------|------|-------------|-----------------|
| lemma | Texto | Forma base de la palabra mal ubicada | R017 |
| pos | Texto | Parte de la oracion del lema | R017 |
| expectedLevel | CefrLevel | Nivel CEFR donde el EVP indica que el lema deberia estar | R017 |

`CefrLevel` es una enumeracion con los valores A1, A2, B1, B2, representando los niveles del Marco Comun Europeo de Referencia para las Lenguas.

**Error**: N/A (esta regla define la estructura de un registro auxiliar)

---

### Grupo C - Navegacion del arbol de auditoria

### Rule[F-DLABS-R011] - Navegacion hacia nodos ancestros
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cada nodo del arbol de auditoria debe proveer un mecanismo para navegar hacia su nodo ancestro en un nivel especifico de la jerarquia. Dado un nodo (por ejemplo, un quiz), el consumidor debe poder solicitar "dame el ancestro de tipo milestone" y obtener el nodo del nivel CEFR que contiene ese quiz.

La navegacion es ascendente (del nodo hacia la raiz) y acepta como parametro el nivel de la jerarquia al que se quiere llegar (curso, milestone, topic, knowledge). Si el nodo solicitado como ancestro no existe en la cadena de ancestros del nodo actual (por ejemplo, solicitar el ancestro "knowledge" desde un nodo de curso), el sistema debe retornar una indicacion de ausencia.

Este mecanismo es esencial para el caso de uso del refiner: al corregir un quiz con lemas mal ubicados, el refiner navega hasta el milestone ancestro para consultar el `LemmaAbsenceLevelDiagnosis` y obtener la lista de lemas ausentes disponibles como reemplazo.

**Error**: "No se encontro ancestro de tipo '{nivel}' para el nodo {nodoId}"

### Rule[F-DLABS-R012] - Combinacion de navegacion y acceso tipado
**Severity**: major | **Validation**: AUTO_VALIDATED

Los consumidores deben poder combinar la navegacion hacia ancestros (R011) con el acceso tipado a diagnosticos (R003) para realizar consultas cruzadas entre niveles. El patron de uso tipico es:

1. Partir de un nodo hoja (por ejemplo, un quiz).
2. Navegar hacia un ancestro (por ejemplo, el milestone).
3. Obtener el diagnostico tipado de un analizador en ese ancestro (por ejemplo, `LemmaAbsenceLevelDiagnosis`).

Este patron permite al refiner, por ejemplo:
- Desde un quiz con lemas mal ubicados, navegar al milestone y consultar los lemas ausentes de ese nivel para sugerir reemplazos.
- Desde un quiz con oracion corta (detectada por sentence-length), navegar al milestone y consultar que lemas estan ausentes (de lemma-absence) para sugerir palabras que alarguen la oracion y al mismo tiempo cubran vocabulario faltante.

La combinacion de ambos mecanismos es lo que habilita el consumo cruzado entre analizadores a traves de la jerarquia.

**Error**: N/A (esta regla describe un patron de uso que combina R011 y R003)

---

### Grupo D - Migracion de consumidores

### Rule[F-DLABS-R013] - Migracion del formateador de detalle de lemma-absence
**Severity**: major | **Validation**: AUTO_VALIDATED

El formateador de detalle de ausencia de lemas (`LemmaAbsenceDetailedFormatter`) es el principal consumidor actual de los datos de diagnostico del analizador. Tras la migracion, este formateador debe leer los registros de diagnostico tipados en lugar de acceder al mapa generico.

El formateador accede a datos en los siguientes niveles:

| Nivel | Dato que consume | Registro tipado |
|-------|-----------------|-----------------|
| Curso | Assessment global | LemmaAbsenceCourseDiagnosis |
| Milestone | Metricas de cobertura y lista de lemas ausentes | LemmaAbsenceLevelDiagnosis |
| Knowledge | Conteo y detalle de lemas mal ubicados | LemmaPlacementDiagnosis |
| Quiz | Detalle de lemas mal ubicados | LemmaPlacementDiagnosis |

Tras la migracion, el formateador no debe contener ninguna conversion insegura ni supresion de advertencias relacionada con tipos. Todos los accesos a datos de diagnostico deben ser a traves del mecanismo de acceso tipado (R003).

**Error**: N/A (esta regla describe una obligacion de migracion)

### Rule[F-DLABS-R014] - Eliminacion del mapa generico para lemma-absence
**Severity**: major | **Validation**: AUTO_VALIDATED

Una vez completada la migracion del analizador lemma-absence y todos sus consumidores, el analizador no debe escribir datos en el mapa generico sin tipo. Toda la informacion de diagnostico de lemma-absence debe fluir exclusivamente a traves de los registros tipados definidos en este requerimiento (R004-R010).

Otros analizadores que aun no hayan sido migrados pueden seguir usando el mapa generico temporalmente. La coexistencia del mapa de diagnosticos tipados y el mapa generico es aceptable durante el periodo de migracion, pero para lemma-absence la migracion debe ser completa.

**Error**: N/A (esta regla describe una restriccion de migracion)

---

## User Journeys

### Journey[F-DLABS-J001] - Consultar diagnosticos tipados del analizador lemma-absence
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-DLABS-J001
    name: Consultar diagnosticos tipados del analizador lemma-absence
    flow:
      - id: ejecutar_auditoria
        action: "El usuario ejecuta una auditoria de un curso en ContentAudit"
        then: obtener_diagnostico_curso

      - id: obtener_diagnostico_curso
        action: "El consumidor solicita el diagnostico tipado de lemma-absence en el nodo curso, obteniendo el LemmaAbsenceCourseDiagnosis con el assessment global"
        gate: [F-DLABS-R002, F-DLABS-R003, F-DLABS-R004]
        outcomes:
          - when: "El assessment es OPTIMAL"
            then: confirmar_cobertura
          - when: "El assessment es ACCEPTABLE o NEEDS_IMPROVEMENT"
            then: obtener_diagnostico_nivel

      - id: obtener_diagnostico_nivel
        action: "El consumidor solicita el diagnostico tipado de lemma-absence en un nodo de nivel (milestone), obteniendo el LemmaAbsenceLevelDiagnosis con las metricas de cobertura y la lista tipada de lemas ausentes"
        gate: [F-DLABS-R003, F-DLABS-R005, F-DLABS-R006]
        outcomes:
          - when: "Hay lemas ausentes en el nivel"
            then: explorar_detalle_quiz
          - when: "No hay lemas ausentes en el nivel"
            then: confirmar_cobertura

      - id: explorar_detalle_quiz
        action: "El consumidor solicita el diagnostico tipado de lemma-absence en un nodo de quiz, obteniendo el LemmaPlacementDiagnosis con los lemas mal ubicados tipados"
        gate: [F-DLABS-R003, F-DLABS-R009, F-DLABS-R010]
        then: confirmar_acceso_tipado

      - id: confirmar_acceso_tipado
        action: "El consumidor ha accedido a todos los datos de diagnostico sin conversiones inseguras, con campos nombrados y autocompletado disponible"
        result: success

      - id: confirmar_cobertura
        action: "La cobertura de vocabulario es adecuada, el consumidor no necesita profundizar"
        result: success
```

### Journey[F-DLABS-J002] - Navegar desde un quiz hacia el diagnostico de su milestone ancestro
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-DLABS-J002
    name: Navegar desde un quiz hacia el diagnostico de su milestone ancestro
    flow:
      - id: identificar_quiz_problematico
        action: "El consumidor (por ejemplo, el futuro refiner) identifica un quiz con lemas mal ubicados a partir de su LemmaPlacementDiagnosis"
        gate: [F-DLABS-R003, F-DLABS-R009]
        then: navegar_a_milestone

      - id: navegar_a_milestone
        action: "El consumidor solicita el nodo ancestro de tipo milestone desde el nodo del quiz"
        gate: [F-DLABS-R011]
        outcomes:
          - when: "El milestone ancestro existe"
            then: obtener_diagnostico_nivel
          - when: "No se encuentra el milestone ancestro (estructura de arbol incompleta)"
            then: error_navegacion

      - id: obtener_diagnostico_nivel
        action: "El consumidor obtiene el LemmaAbsenceLevelDiagnosis del milestone, accediendo a la lista de lemas ausentes disponibles como posibles reemplazos"
        gate: [F-DLABS-R003, F-DLABS-R005, F-DLABS-R012]
        then: cruzar_datos

      - id: cruzar_datos
        action: "El consumidor cruza los lemas mal ubicados del quiz con los lemas ausentes del nivel para identificar reemplazos que mejoren tanto la ubicacion como la cobertura"
        result: success

      - id: error_navegacion
        action: "El sistema informa que no se pudo navegar al milestone ancestro desde el quiz"
        result: failure
```

### Journey[F-DLABS-J003] - Formatear informe de ausencia de lemas usando diagnosticos tipados
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-DLABS-J003
    name: Formatear informe de ausencia de lemas usando diagnosticos tipados
    flow:
      - id: iniciar_formateo
        action: "El formateador de detalle de lemma-absence recibe el arbol de auditoria para generar el informe"
        then: leer_diagnostico_curso

      - id: leer_diagnostico_curso
        action: "El formateador solicita el LemmaAbsenceCourseDiagnosis del nodo curso para obtener el assessment global"
        gate: [F-DLABS-R003, F-DLABS-R004, F-DLABS-R013]
        outcomes:
          - when: "El diagnostico existe en el nodo curso"
            then: leer_diagnosticos_niveles
          - when: "El diagnostico no existe (el analizador no se ejecuto)"
            then: omitir_seccion

      - id: leer_diagnosticos_niveles
        action: "El formateador itera sobre los nodos de nivel y solicita el LemmaAbsenceLevelDiagnosis de cada uno para obtener metricas y lemas ausentes"
        gate: [F-DLABS-R003, F-DLABS-R005, F-DLABS-R006, F-DLABS-R013]
        then: leer_diagnosticos_quiz

      - id: leer_diagnosticos_quiz
        action: "El formateador itera sobre los nodos de quiz dentro de cada knowledge y solicita el LemmaPlacementDiagnosis para obtener el detalle de lemas mal ubicados"
        gate: [F-DLABS-R003, F-DLABS-R009, F-DLABS-R010, F-DLABS-R013]
        then: generar_informe

      - id: generar_informe
        action: "El formateador genera el informe completo sin ninguna conversion insegura, accediendo a todos los datos a traves de campos tipados"
        gate: [F-DLABS-R014]
        result: success

      - id: omitir_seccion
        action: "El formateador omite la seccion de ausencia de lemas porque el analizador no produjo diagnosticos"
        result: success
```

---

## Open Questions

### Doubt[DOUBT-COEXISTENCE] - Coexistencia del mapa generico y el mapa de diagnosticos
**Status**: OPEN

Durante la migracion, el arbol de auditoria tendra dos mecanismos de almacenamiento de resultados: el mapa generico sin tipo (para analizadores no migrados) y el mapa de diagnosticos tipados (para analizadores migrados). La pregunta es como gestionar esta coexistencia.

**Pregunta**: Deben coexistir ambos mapas en el nodo de auditoria, o el mapa de diagnosticos tipados debe reemplazar completamente al generico desde el inicio?

- [ ] Opcion A: Coexistencia temporal — ambos mapas existen en paralelo. Los analizadores migrados escriben en el mapa tipado; los no migrados siguen escribiendo en el generico. El mapa generico se elimina cuando todos los analizadores esten migrados.
- [ ] Opcion B: Reemplazo inmediato — el mapa generico se elimina desde el inicio. Los analizadores no migrados deben emitir un diagnostico generico (por ejemplo, `RawDiagnosis` que envuelve un mapa sin tipo) para cumplir con la interfaz. Esto fuerza la migracion de todos los analizadores a corto plazo.
- [ ] Opcion C: Reemplazo gradual con wrapper — el mapa generico se envuelve automaticamente en un `RawDiagnosis` por defecto, y los analizadores migrados emiten sus diagnosticos tipados. Funcionalmente equivalente a Opcion B pero sin exigir cambios inmediatos en los analizadores no migrados.

### Doubt[DOUBT-DIAGNOSIS-KEY] - Clave del mapa de diagnosticos
**Status**: OPEN

Cada analizador registra su diagnostico bajo una clave en el mapa. Actualmente los metadatos se escriben directamente en el mapa raiz sin clave de analizador (las claves son los nombres de los campos, no del analizador).

**Pregunta**: La clave del mapa de diagnosticos debe ser el nombre del analizador (por ejemplo, `"lemma-absence"`) u otro identificador?

- [ ] Opcion A: Usar el nombre del analizador como clave (alineado con R031 de FEAT-LABS). Simple, descriptivo, unico por definicion.
- [ ] Opcion B: Usar un identificador mas formal (por ejemplo, un enum o constante). Mas robusto ante errores tipograficos, pero menos flexible.

### Doubt[DOUBT-LEVEL-DIAGNOSIS-TYPE] - Tipo de diagnostico para topic vs knowledge vs quiz
**Status**: OPEN

Actualmente se propone reutilizar `LemmaPlacementDiagnosis` en los tres niveles (topic, knowledge, quiz). Sin embargo, hay una diferencia sutil: a nivel topic solo se emite `misplacedLemmaCount` (sin la lista detallada), mientras que a nivel knowledge y quiz se emite tanto el conteo como la lista.

**Pregunta**: Se debe usar el mismo tipo con la lista vacia a nivel topic, o crear un tipo separado mas simple?

- [ ] Opcion A: Reutilizar `LemmaPlacementDiagnosis` en los tres niveles. A nivel topic, la lista `misplacedLemmas` puede estar vacia mientras que el conteo refleja el total. Mas simple, un solo tipo.
- [ ] Opcion B: Crear `LemmaPlacementSummaryDiagnosis` (solo conteo) para topic, y `LemmaPlacementDiagnosis` (conteo + lista) para knowledge y quiz. Mas preciso, pero mas tipos para mantener.
- [ ] Opcion C: Enriquecer el nivel topic para que tambien emita la lista detallada, alineandolo con knowledge y quiz. Elimina la asimetria a costa de emitir mas datos.

### Doubt[DOUBT-ANCESTOR-SCOPE] - Alcance de la navegacion hacia ancestros
**Status**: OPEN

La regla R011 define la navegacion hacia ancestros como un mecanismo generico (desde cualquier nodo hacia cualquier nivel de la jerarquia). Sin embargo, el caso de uso inmediato es especificamente "desde quiz hacia milestone".

**Pregunta**: Se debe implementar la navegacion generica (cualquier nodo a cualquier ancestro) o solo el caso especifico necesario ahora?

- [ ] Opcion A: Navegacion generica desde el inicio. Es mas util a futuro cuando el refiner necesite navegar hacia otros niveles, y el esfuerzo adicional es minimo.
- [ ] Opcion B: Solo la navegacion quiz-a-milestone por ahora. Se generaliza cuando haya un segundo caso de uso concreto. Sigue el principio de "no construir lo que no se necesita aun".
