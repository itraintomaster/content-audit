---
feature:
  id: FEAT-DSLEN
  code: F-DSLEN
  name: Diagnosticos Tipados para el Analizador de Longitud de Oraciones
  priority: critical
---

# Diagnosticos Tipados para el Analizador de Longitud de Oraciones

Exponer la informacion que el analizador de longitud de oraciones (FEAT-SLEN) calcula internamente como un **registro de diagnostico tipado** a nivel quiz, de modo que los consumidores (en particular el futuro refiner) puedan conocer no solo la puntuacion sino tambien los datos que la determinaron: cuantos tokens tiene la oracion, cual es el rango esperado para su nivel CEFR, cuanto se desvio y cual es el margen de tolerancia.

Este es el tercer paso de la iniciativa de diagnosticos tipados, tras FEAT-DLABS (lemma-absence) y FEAT-DCOCA (coca-buckets-distribution).

## Contexto

### Relacion con FEAT-DLABS y FEAT-DCOCA

FEAT-DLABS establecio toda la infraestructura necesaria para los diagnosticos tipados:

- La interfaz sellada `NodeDiagnoses` con sub-interfaces por nivel (`CourseDiagnoses`, `LevelDiagnoses`, `TopicDiagnoses`, `KnowledgeDiagnoses`, `QuizDiagnoses`).
- El campo `diagnoses` en `AuditNode` y el mecanismo `ancestor(AuditTarget)` para navegacion entre niveles.
- Las implementaciones por defecto con getters opcionales tipados.
- El patron establecido: cada analizador agrega metodos `getXxxDiagnosis()` a las interfaces por nivel correspondientes.

FEAT-DCOCA aplico el patron al segundo analizador (coca-buckets), confirmando que la infraestructura es reutilizable.

Este requerimiento **no modifica** dicha infraestructura. Solo agrega un nuevo metodo a `QuizDiagnoses` y define un unico registro de diagnostico.

### Relacion con FEAT-SLEN

Este requerimiento **no modifica** lo que el analizador calcula. Todas las reglas de negocio de FEAT-SLEN permanecen identicas: la exclusion de quizzes no-oracion, el conteo de tokens, el scoring por distancia al rango, el margen de tolerancia y la evaluacion de progresion siguen funcionando de la misma manera.

La diferencia clave respecto a las dos migraciones anteriores es que el analizador de longitud de oraciones **actualmente no emite metadata alguna** -- solo deposita la puntuacion en `node.getScores()`. Los datos que componen el diagnostico (conteo de tokens, rango objetivo, nivel CEFR, desviacion) son valores que el analizador calcula internamente durante el scoring pero que descarta sin exponerlos. Este requerimiento los hace visibles a traves de un registro tipado.

### Alcance: solo nivel quiz

El analizador opera exclusivamente a nivel quiz (FEAT-SLEN R002). Las puntuaciones en niveles superiores (knowledge, topic, milestone, curso) provienen de la agregacion generica de la plataforma, no del analizador. Por lo tanto, **solo se define un diagnostico a nivel quiz**. No hay diagnosticos en otros niveles porque el analizador no produce datos especificos en ellos.

### Consumidor principal: el futuro refiner

A diferencia de FEAT-DLABS y FEAT-DCOCA, el analizador de longitud de oraciones no tiene un `DetailedFormatter` dedicado. El diagnostico esta orientado principalmente al futuro refiner, que necesita saber con precision por que una oracion obtuvo una puntuacion baja: "esta oracion tiene 18 tokens, el rango para A1 es 5-10, esta 8 tokens por encima, el margen de tolerancia es 4". Con esta informacion el refiner puede decidir acortar la oracion de forma informada. No se requiere migracion de formateadores.

---

## Reglas de Negocio

Las reglas de infraestructura compartida (interfaz sellada, mapa de diagnosticos, acceso tipado, navegacion de ancestros) estan definidas en FEAT-DLABS Grupo A (R001-R003) y Grupo C (R011-R012). Aplican directamente a este analizador sin modificacion.

---

### Rule[F-DSLEN-R001] - Registro de diagnostico a nivel quiz: SentenceLengthDiagnosis
**Severity**: critical | **Validation**: AUTO_VALIDATED

En cada nodo de quiz del arbol de auditoria donde el analizador de longitud de oraciones haya producido una puntuacion (es decir, quizzes validos no excluidos por FEAT-SLEN R001), el analizador debe emitir un registro `SentenceLengthDiagnosis` que contiene:

| Campo | Tipo | Descripcion | Regla FEAT-SLEN |
|-------|------|-------------|-----------------|
| tokenCount | Entero | Cantidad real de tokens linguisticos en la oracion del quiz | R013 |
| targetMin | Entero | Minimo de tokens esperados para el nivel CEFR del quiz | R012 |
| targetMax | Entero | Maximo de tokens esperados para el nivel CEFR del quiz | R012 |
| cefrLevel | CefrLevel | Nivel CEFR derivado del milestone ancestro del quiz (A1, A2, B1, B2) | R012 |
| delta | Entero | Desviacion respecto al rango: 0 si esta dentro del rango, positivo si excede el maximo, negativo si esta por debajo del minimo | R002 |
| toleranceMargin | Entero | Margen de tolerancia configurado usado para el calculo de la puntuacion | R009 |

El campo `delta` se calcula asi: si `tokenCount` esta entre `targetMin` y `targetMax` (inclusive), `delta` es 0. Si `tokenCount` es mayor que `targetMax`, `delta` es `tokenCount - targetMax` (positivo, indica exceso). Si `tokenCount` es menor que `targetMin`, `delta` es `tokenCount - targetMin` (negativo, indica carencia).

Ejemplo: para un quiz en nivel A1 (rango 5-8) con 18 tokens, el diagnostico seria: `tokenCount=18`, `targetMin=5`, `targetMax=8`, `cefrLevel=A1`, `delta=10`, `toleranceMargin=4`. Esto comunica al consumidor que la oracion excede el rango por 10 tokens y que el margen de tolerancia es de solo 4, lo cual explica la puntuacion 0.0.

**Error**: N/A (esta regla define la estructura de un registro)

### Rule[F-DSLEN-R002] - Ausencia de diagnostico en quizzes excluidos
**Severity**: major | **Validation**: AUTO_VALIDATED

Los quizzes que no son oraciones (excluidos por FEAT-SLEN R001) **no** reciben un `SentenceLengthDiagnosis`. El diagnostico solo se emite para quizzes donde el analizador produjo una puntuacion. Esto es consistente con el comportamiento actual: si un quiz no fue evaluado, no tiene datos de diagnostico que exponer.

**Error**: N/A (esta regla define una restriccion de alcance)

### Rule[F-DSLEN-R003] - Ausencia de diagnostico en niveles superiores
**Severity**: major | **Validation**: AUTO_VALIDATED

El analizador de longitud de oraciones **no** emite diagnosticos a nivel knowledge, topic, milestone ni curso. Las interfaces `KnowledgeDiagnoses`, `TopicDiagnoses`, `LevelDiagnoses` y `CourseDiagnoses` **no** reciben un metodo `getSentenceLengthDiagnosis()`.

Esto es consistente con el diseno de FEAT-SLEN: el analizador opera exclusivamente a nivel quiz. Las puntuaciones en niveles superiores provienen de la agregacion generica de la plataforma (FEAT-SLEN R003-R005, R008), no del analizador. Las estadisticas por nivel (FEAT-SLEN R006, R007, R015) son producidas por un procesamiento posterior independiente y no forman parte del diagnostico del analizador.

**Error**: N/A (esta regla define una restriccion de alcance)

### Rule[F-DSLEN-R004] - Nuevo metodo en la interfaz QuizDiagnoses
**Severity**: critical | **Validation**: AUTO_VALIDATED

La interfaz `QuizDiagnoses` debe extenderse con un metodo tipado para acceder al diagnostico de longitud de oraciones:

| Interfaz | Nuevo metodo | Tipo de retorno |
|----------|-------------|-----------------|
| QuizDiagnoses | getSentenceLengthDiagnosis() | Opcional de SentenceLengthDiagnosis |

Este metodo sigue el mismo patron establecido por `getLemmaAbsenceDiagnosis()` en FEAT-DLABS y `getCocaBucketsDiagnosis()` en FEAT-DCOCA: retorna un valor opcional que esta vacio cuando el analizador no se ejecuto sobre ese nodo (quiz excluido o analizador no habilitado), y contiene el registro tipado cuando el analizador produjo datos. La implementacion por defecto (`DefaultQuizDiagnoses`) debe actualizarse para soportar el nuevo metodo.

**Error**: N/A (esta regla define la extension de una interfaz existente)

---

## User Journeys

### Journey[F-DSLEN-J001] - Consultar diagnostico de longitud desde el refiner para corregir una oracion
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-DSLEN-J001
    name: Consultar diagnostico de longitud desde el refiner para corregir una oracion
    flow:
      - id: identificar_quiz
        action: "El refiner identifica un quiz con puntuacion baja en el analizador de longitud de oraciones"
        then: obtener_diagnostico

      - id: obtener_diagnostico
        action: "El refiner solicita el SentenceLengthDiagnosis del nodo quiz para conocer los datos del problema"
        gate: [F-DSLEN-R001, F-DSLEN-R004]
        outcomes:
          - when: "El diagnostico existe (quiz fue evaluado)"
            then: analizar_desviacion
          - when: "El diagnostico no existe (quiz excluido por no ser oracion)"
            then: omitir_quiz

      - id: analizar_desviacion
        action: "El refiner examina el diagnostico: lee tokenCount, targetMin, targetMax, cefrLevel y delta para entender la magnitud y direccion del problema"
        outcomes:
          - when: "Delta es positivo (oracion demasiado larga)"
            then: decidir_acortar
          - when: "Delta es negativo (oracion demasiado corta)"
            then: decidir_alargar

      - id: decidir_acortar
        action: "El refiner sabe que la oracion excede el rango por delta tokens y decide acortarla, conociendo exactamente cuantos tokens debe eliminar para entrar en el rango"
        then: cruzar_con_otros

      - id: decidir_alargar
        action: "El refiner sabe que la oracion esta por debajo del rango por delta tokens y decide alargarla, pudiendo consultar diagnosticos de otros analizadores (por ejemplo, lemas ausentes de lemma-absence) para elegir palabras que cubran multiples dimensiones"
        then: cruzar_con_otros

      - id: cruzar_con_otros
        action: "El refiner combina el diagnostico de longitud con diagnosticos de otros analizadores para generar una correccion que mejore multiples dimensiones simultaneamente"
        result: success

      - id: omitir_quiz
        action: "El refiner omite el quiz porque no fue evaluado por el analizador de longitud"
        result: success
```

---

## Open Questions

No hay preguntas abiertas. El patron esta bien establecido por FEAT-DLABS y FEAT-DCOCA, el alcance es minimo (un solo registro en un solo nivel) y no hay formateador que migrar.
