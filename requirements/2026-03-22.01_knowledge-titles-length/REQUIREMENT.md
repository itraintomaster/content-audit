---
feature:
  id: FEAT-KTLEN
  code: F-KTLEN
  name: Analisis de Longitud de Titulos e Instrucciones de Knowledge
  priority: major
---

# Analisis de Longitud de Titulos e Instrucciones de Knowledge

Evaluar si la longitud de los titulos (labels) y las instrucciones de cada knowledge (ejercicio) del curso cumple con los limites establecidos para garantizar una buena experiencia de usuario. Los titulos deben ser concisos para caber en elementos de navegacion e interfaz, y las instrucciones deben ser lo suficientemente breves para que el estudiante las lea antes de comenzar el ejercicio. Se producen puntuaciones individuales por knowledge que se agregan a traves de la jerarquia del curso mediante el motor de agregacion generico de la plataforma ContentAudit.

## Contexto

El sistema ContentAudit audita cursos de idiomas para garantizar que el contenido es adecuado desde multiples perspectivas. Una dimension importante es la **longitud del contenido textual de los knowledges**: en una aplicacion de aprendizaje, cada knowledge (ejercicio) tiene un titulo visible en la navegacion y en las tarjetas de la interfaz, y opcionalmente un texto de instrucciones que guia al estudiante sobre que debe hacer en el ejercicio.

### Problema de UX: titulos largos

Los titulos de los knowledges aparecen en multiples contextos de la interfaz: listas de ejercicios, tarjetas de navegacion, breadcrumbs, menus laterales. Estos elementos tienen espacio limitado, especialmente en dispositivos moviles. Un titulo que excede el espacio disponible se trunca o causa desbordamiento visual, degradando la experiencia del usuario. El limite maximo de longitud garantiza que los titulos sean concisos y se muestren correctamente en todos los contextos de la interfaz.

### Problema de UX: instrucciones extensas

Las instrucciones de un knowledge aparecen al inicio del ejercicio y deben comunicar rapidamente que se espera del estudiante. Instrucciones demasiado largas reducen la probabilidad de que el estudiante las lea, lo que puede causar confusion y errores. Se establecen dos umbrales: un limite suave (las instrucciones son aceptables pero empiezan a ser largas) y un limite duro (las instrucciones son definitivamente demasiado largas).

### Longitud ponderada para titulos

En interfaces con fuentes proporcionales, no todos los caracteres ocupan el mismo ancho visual. Un titulo de 28 caracteres compuesto por letras anchas (como "m", "w") ocupa mas espacio que uno compuesto por letras estrechas (como "i", "l"). Para reflejar con mayor precision el espacio visual que ocupa un titulo, se utiliza un sistema de pesos por caracter que ajusta la longitud calculada segun el ancho visual tipico de cada caracter.

### Jerarquia del curso

Los knowledges estan organizados dentro de la jerarquia del curso: **Curso -> Nivel (Milestone) -> Topic -> Knowledge**. Cada knowledge tiene un titulo y opcionalmente instrucciones. El analisis opera a nivel de knowledge individual, y las puntuaciones se agregan ascendentemente a traves de la jerarquia por el motor generico de la plataforma.

### Dos analisis, dos analizadores

Esta funcionalidad comprende dos analisis distintos que operan sobre datos diferentes del knowledge:

1. **Longitud de titulo**: evalua el titulo (label) del knowledge usando longitud ponderada por caracteres.
2. **Longitud de instrucciones**: evalua el texto de instrucciones del knowledge usando longitud simple en caracteres.

Cada analisis opera como un analizador independiente dentro de la plataforma, produciendo su propio conjunto de puntuaciones. Esto permite que el informe de auditoria presente las puntuaciones de cada analisis por separado, y que el usuario pueda distinguir si los problemas son de titulos, de instrucciones, o de ambos. Los dos analizadores se identifican como **"knowledge-title-length"** (longitud de titulos) y **"knowledge-instructions-length"** (longitud de instrucciones).

### Separacion entre analisis y agregacion

Al igual que en otros analizadores del sistema:

1. **Fase de analisis (especifica de esta funcionalidad)**: Cada analizador evalua cada knowledge individualmente y produce una puntuacion de 0.0 a 1.0 que refleja la adecuacion de la longitud del titulo o de las instrucciones.

2. **Fase de agregacion (generica de la plataforma)**: Las puntuaciones individuales por knowledge son agregadas a traves de la jerarquia del curso por el motor de agregacion de ContentAudit. Este motor construye un arbol de resultados (Curso -> Nivel -> Topic -> Knowledge) y calcula promedios ascendentes. Esta agregacion es identica para todos los analizadores del sistema, no es exclusiva de esta funcionalidad.

### Volumenes esperados

El curso actual contiene aproximadamente 608 knowledges distribuidos en 4 niveles (A1, A2, B1, B2). Cada knowledge tiene un titulo y puede tener instrucciones. El analisis debe procesar todos los knowledges del curso.

### Referencia: implementacion original

Esta funcionalidad se basa en el analisis documentado en `analysis/06-knowledge-titles-length`. En la implementacion original, este analisis no seguia la interfaz estandar de analizador, sino que operaba directamente sobre la lista de knowledges en el orquestador. En la migracion a ContentAudit, se integra como analizador estandar para que sus resultados participen en el informe de auditoria y se beneficien de la agregacion generica de la plataforma.

---

## Reglas de Negocio

Las reglas se organizan en dos grupos segun el aspecto que evaluan:

- **Grupo A - Analisis de longitud de titulos (R001, R002, R003, R004)**: reglas propias del analizador de longitud de titulos, que evalua el titulo de cada knowledge usando longitud ponderada por caracteres.
- **Grupo B - Analisis de longitud de instrucciones (R005, R006, R007)**: reglas propias del analizador de longitud de instrucciones, que evalua el texto de instrucciones de cada knowledge.
- **Grupo C - Identificacion de analizadores (R008)**: regla que define los nombres con los que cada analizador se identifica en el informe de auditoria.

> **Nota sobre agregacion**: Las reglas de agregacion de puntuaciones a traves de la jerarquia (knowledge -> topic -> nivel -> curso) son provistas por la plataforma ContentAudit y estan documentadas en FEAT-SLEN (R003-R005, R008, R016). Aplican de manera identica a estos analizadores y no se repiten aqui.

---

### Grupo A - Analisis de longitud de titulos

### Rule[F-KTLEN-R001] - Limite maximo de longitud de titulo
**Severity**: critical | **Validation**: AUTO_VALIDATED

El titulo de cada knowledge se evalua contra un limite maximo de longitud ponderada de 28 caracteres. La longitud se calcula usando el sistema de pesos por caracter (R002), no la longitud simple del string. Si la longitud ponderada del titulo es menor o igual a 28, se considera adecuado. Si excede 28, se penaliza proporcionalmente segun la formula de puntuacion (R003).

El valor de 28 caracteres ponderados proviene de la implementacion de referencia y refleja el espacio disponible en los elementos de interfaz del sistema de aprendizaje.

**Error**: "El titulo del knowledge {knowledgeId} excede el limite maximo de longitud ponderada: {longitudPonderada} > 28"

### Rule[F-KTLEN-R002] - Sistema de pesos por caracter para titulos
**Severity**: critical | **Validation**: AUTO_VALIDATED

La longitud de un titulo se calcula como la suma de los pesos de cada caracter individual. Los pesos reflejan el ancho visual tipico de cada caracter en una fuente proporcional:

| Peso | Caracteres | Razon |
|------|-----------|-------|
| 0.0 | `$`, `*` | Caracteres de formato que no ocupan espacio visual significativo |
| 0.5 | `i`, `,`, `.` | Caracteres visualmente estrechos |
| 0.7 | `f`, `t`, `"` | Caracteres de ancho medio-estrecho |
| 1.0 | Todos los demas | Ancho estandar |

Ejemplo: el titulo "fitting" se calcula como: f(0.7) + i(0.5) + t(0.7) + t(0.7) + i(0.5) + n(1.0) + g(1.0) = 5.1 caracteres ponderados.

Los caracteres que no estan listados explicitamente en la tabla reciben el peso por defecto de 1.0. Esto incluye letras mayusculas, numeros, espacios, y caracteres Unicode fuera del rango ASCII basico.

**Error**: N/A (esta regla define un mecanismo de calculo, no una condicion de error)

### Rule[F-KTLEN-R003] - Puntuacion de longitud de titulo
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cada knowledge recibe una puntuacion entre 0.0 y 1.0 que refleja la adecuacion de la longitud de su titulo. Las condiciones se evaluan en el siguiente orden de prioridad:

1. **Titulo nulo o vacio**: la puntuacion es 0.0. Todo knowledge debe tener un titulo; la ausencia de titulo se considera un defecto grave. Esta condicion se evalua antes que cualquier calculo de longitud ponderada.
2. **Longitud ponderada dentro del limite** (menor o igual a 28): la puntuacion es 1.0.
3. **Longitud ponderada excede el limite**: la puntuacion disminuye linealmente:
   - `score = max(0.0, 1.0 - (longitudPonderada - limiteMaximo) / limiteMaximo)`
   - Esto significa que un titulo con el doble del limite maximo (56 caracteres ponderados) recibe puntuacion 0.0.

Ejemplo para limite maximo de 28:

| Longitud ponderada | 0 (vacio) | 15  | 28  | 35   | 42   | 56  |
|--------------------|-----------|-----|-----|------|------|-----|
| Puntuacion         | 0.0       | 1.0 | 1.0 | 0.75 | 0.50 | 0.0 |

**Error**: "Puntuacion fuera de rango [0.0, 1.0] calculada para el titulo del knowledge {knowledgeId}: {puntuacion}"

### Rule[F-KTLEN-R004] - Los pesos de caracteres no son configurables
**Severity**: minor | **Validation**: ASSUMPTION

Los pesos por caracter definidos en R002 estan fijos y no son configurables por el usuario. Fueron calibrados para la fuente proporcional utilizada en la interfaz del sistema de aprendizaje.

[ASSUMPTION] Se asume que los pesos de la implementacion de referencia son adecuados para la interfaz actual. Si la interfaz de destino usa una fuente con proporciones significativamente diferentes, los pesos deberian recalibrarse. Se mantienen fijos por simplicidad.

**Error**: N/A (esta regla define un parametro, no una condicion de error)

---

### Grupo B - Analisis de longitud de instrucciones

### Rule[F-KTLEN-R005] - Limites de longitud de instrucciones
**Severity**: critical | **Validation**: AUTO_VALIDATED

Las instrucciones de cada knowledge se evaluan contra dos umbrales de longitud medida en caracteres simples (no ponderada):

| Parametro | Valor | Significado |
|-----------|-------|-------------|
| Limite suave (soft limit) | 70 caracteres | Las instrucciones empiezan a ser largas |
| Limite duro (hard limit) | 100 caracteres | Las instrucciones son definitivamente demasiado largas |

La longitud se mide como la cantidad de caracteres del string de instrucciones (length del string). A diferencia de los titulos, no se usa longitud ponderada porque las instrucciones se muestran en bloques de texto donde el truncamiento no es un problema visual critico; lo que importa es la cantidad total de texto que el estudiante debe procesar.

**Error**: "Configuracion de limites de instrucciones invalida: el limite suave ({soft}) debe ser menor que el limite duro ({hard})"

### Rule[F-KTLEN-R006] - Puntuacion de longitud de instrucciones
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cada knowledge recibe una puntuacion entre 0.0 y 1.0 para la longitud de sus instrucciones, basada en un sistema de tres niveles:

| Condicion | Puntuacion | Significado |
|-----------|-----------|-------------|
| Instrucciones nulas o vacias | 1.0 | No se penaliza la ausencia de instrucciones |
| Longitud <= limite suave (70) | 1.0 | Instrucciones adecuadas |
| Longitud > limite suave (70) y <= limite duro (100) | 0.5 | Instrucciones aceptables pero largas |
| Longitud > limite duro (100) | 0.0 | Instrucciones demasiado largas |

El scoring discreto (tres niveles) en lugar de lineal refleja la naturaleza de la evaluacion: no hay una diferencia gradual entre "adecuado" y "largo", sino umbrales claros donde la legibilidad cambia. 70 caracteres es aproximadamente una linea de texto en la interfaz; 100 caracteres fuerza al texto a ocupar multiples lineas en la mayoria de dispositivos.

**Error**: "Puntuacion fuera de rango [0.0, 1.0] calculada para las instrucciones del knowledge {knowledgeId}: {puntuacion}"

### Rule[F-KTLEN-R007] - Los limites de instrucciones no son configurables
**Severity**: minor | **Validation**: ASSUMPTION

Los limites suave (70) y duro (100) para instrucciones estan fijos y no son configurables por el usuario. Provienen de la implementacion de referencia y reflejan restricciones de la interfaz del sistema de aprendizaje.

[ASSUMPTION] Se asume que los limites de la referencia son adecuados para la interfaz actual. Si en el futuro se requiere ajustar estos valores para diferentes tipos de cursos o interfaces, deberia considerarse hacerlos configurables. Se mantienen fijos por simplicidad, alineado con el alcance de esta primera version.

**Error**: N/A (esta regla define parametros, no una condicion de error)

---

### Grupo C - Identificacion de analizadores

### Rule[F-KTLEN-R008] - Nombres de los analizadores en el informe
**Severity**: major | **Validation**: AUTO_VALIDATED

Cada uno de los dos analizadores de esta funcionalidad se identifica con un nombre unico en el informe de auditoria, de modo que el usuario pueda distinguir las puntuaciones de longitud de titulos de las puntuaciones de longitud de instrucciones:

| Analizador | Nombre en el informe | Que evalua |
|------------|---------------------|------------|
| Longitud de titulos | `knowledge-title-length` | Longitud ponderada del titulo de cada knowledge (R001-R003) |
| Longitud de instrucciones | `knowledge-instructions-length` | Longitud simple de las instrucciones de cada knowledge (R005-R006) |

Estos nombres aparecen en las puntuaciones por nodo de la jerarquia y permiten al usuario filtrar y comparar resultados de ambos analisis de forma independiente.

**Error**: N/A (esta regla define nombres de identificacion, no una condicion de error)

---

## User Journeys

### Journey[F-KTLEN-J001] - Auditar la longitud de titulos e instrucciones de un curso completo
**Validation**: AUTO_VALIDATED

1. El usuario inicia una auditoria de un curso previamente cargado en el sistema
2. El sistema recorre la jerarquia del curso de arriba hacia abajo: para cada nivel (milestone), sus topics, y sus knowledges
3. Para cada knowledge, el analizador de titulos calcula la longitud ponderada del titulo usando el sistema de pesos por caracter (R002) y produce una puntuacion individual (R003)
4. Para cada knowledge, el analizador de instrucciones mide la longitud simple de las instrucciones y produce una puntuacion basada en los umbrales suave y duro (R006)
5. La plataforma agrega las puntuaciones de ambos analizadores por separado a traves de la jerarquia: para cada topic calcula el promedio de sus knowledges, para cada nivel el promedio de sus topics, y para el curso el promedio de sus niveles
6. El usuario recibe un informe con las puntuaciones de longitud de titulos y de instrucciones en cada nivel de la jerarquia, permitiendo identificar donde se concentran los problemas

### Journey[F-KTLEN-J002] - Identificar knowledges con titulos demasiado largos
**Validation**: AUTO_VALIDATED

1. El usuario ha ejecutado la auditoria de un curso (J001)
2. El usuario observa que la puntuacion del analizador "knowledge-title-length" en un topic es baja (por ejemplo, 0.6)
3. El usuario profundiza en los knowledges del topic
4. El usuario identifica los knowledges cuyo titulo excede los 28 caracteres ponderados y tienen puntuacion menor a 1.0
5. El usuario revisa los titulos afectados y los acorta o reformula para que se ajusten al limite, priorizando los que tienen puntuacion mas baja (titulos mas excedidos)

### Journey[F-KTLEN-J003] - Identificar knowledges con instrucciones excesivamente largas
**Validation**: AUTO_VALIDATED

1. El usuario ha ejecutado la auditoria de un curso (J001)
2. El usuario observa que la puntuacion del analizador "knowledge-instructions-length" en un nivel es baja
3. El usuario profundiza en los topics y knowledges del nivel
4. El usuario identifica los knowledges cuyas instrucciones tienen puntuacion 0.0 (mas de 100 caracteres) o 0.5 (entre 70 y 100 caracteres)
5. El usuario prioriza las instrucciones con puntuacion 0.0 (las mas criticas) y las sintetiza para reducir su longitud por debajo de los 70 caracteres

### Journey[F-KTLEN-J004] - Comparar problemas de titulos vs instrucciones por nivel
**Validation**: AUTO_VALIDATED

1. El usuario ha ejecutado la auditoria de un curso (J001)
2. El usuario consulta las puntuaciones por nivel para ambos analizadores
3. El usuario compara las puntuaciones de "knowledge-title-length" y "knowledge-instructions-length" en cada nivel
4. El usuario identifica si los problemas de longitud se concentran en los titulos, en las instrucciones, o en ambos
5. Esta comparacion permite al usuario priorizar acciones correctivas: si solo los titulos son problematicos, puede enfocarse en reformularlos sin revisar las instrucciones, y viceversa

---

## Open Questions

### Doubt[DOUBT-WEIGHTED-CHARS] - Se debe usar el sistema de caracteres ponderados para titulos?
**Status**: OPEN

La implementacion de referencia (`analysis/06-knowledge-titles-length`) usa un sistema de pesos por caracter para calcular la longitud de los titulos, disenado para una fuente proporcional especifica. La implementacion provisional actual en el sistema usa longitud simple (cantidad de caracteres del texto).

El sistema de pesos agrega complejidad al calculo pero refleja con mayor precision el espacio visual que ocupa cada titulo en la interfaz. Sin el, un titulo de 28 caracteres compuesto por "i" y "," (caracteres estrechos) se penalizaria igual que uno compuesto por "m" y "w" (caracteres anchos), a pesar de que visualmente el segundo ocupa mucho mas espacio.

**Pregunta**: El sistema de pesos por caracter es necesario para la interfaz actual, o la longitud simple es suficiente?

- [ ] Opcion A: Usar longitud ponderada con los pesos de la referencia (0.0, 0.5, 0.7, 1.0) — mayor precision visual, mayor complejidad
- [ ] Opcion B: Usar longitud simple (todos los caracteres pesan 1.0) — mas simple, menos preciso visualmente
- [ ] Opcion C: Usar longitud ponderada pero recalibrar los pesos para la fuente de la interfaz actual

### Doubt[DOUBT-TITLE-MAX-LENGTH] - Cual es el limite maximo correcto para titulos?
**Status**: OPEN

Existe una discrepancia significativa entre la implementacion de referencia y la implementacion provisional:

- **Referencia**: 28 caracteres ponderados (disenado para un contexto de UI con espacio limitado)
- **Implementacion provisional**: rango de 3 a 80 caracteres simples (mucho mas permisivo)

Un limite de 28 es muy restrictivo y asume que los titulos se muestran en elementos de UI con espacio muy limitado (como tarjetas o botones). Un limite de 80 es muy permisivo y probablemente no detectaria titulos problematicos.

**Pregunta**: Cual es el limite maximo adecuado para los titulos de knowledges en la interfaz actual?

- [ ] Opcion A: 28 caracteres (ponderados o simples), como en la referencia original
- [ ] Opcion B: 80 caracteres simples, como en la implementacion provisional
- [ ] Opcion C: Otro valor que se determine segun los elementos de interfaz donde se muestran los titulos

### Doubt[DOUBT-INSTRUCTIONS-LIMITS] - Cuales son los limites correctos para instrucciones?
**Status**: OPEN

Existe una discrepancia importante entre la referencia y la implementacion provisional:

- **Referencia**: limite suave 70, limite duro 100, scoring discreto de tres niveles (1.0 / 0.5 / 0.0)
- **Implementacion provisional**: limite unico de 300, scoring lineal continuo

Los limites de la referencia (70/100) son significativamente mas estrictos que el limite provisional (300). Ademas, los modelos de scoring son diferentes: la referencia usa tres niveles discretos, mientras que la implementacion provisional usa una penalizacion lineal gradual.

**Pregunta**: Que limites y sistema de scoring se deben usar para las instrucciones?

- [ ] Opcion A: Limites 70/100 con scoring de tres niveles, como en la referencia — mas estricto, refleja restricciones reales de UI
- [ ] Opcion B: Limite unico de 300 con scoring lineal, como en la implementacion provisional — mas permisivo, penalizacion gradual
- [ ] Opcion C: Limites configurables que se determinen segun las necesidades del equipo de contenido

### Doubt[DOUBT-TITLE-MIN-LENGTH] - Se debe evaluar un largo minimo para titulos?
**Status**: OPEN

La implementacion provisional establece un minimo de 3 caracteres para titulos (un titulo de 1-2 caracteres recibe puntuacion reducida). La referencia no menciona un minimo explicito, solo evalua que no excedan el maximo.

Un titulo de 1-2 caracteres probablemente no es descriptivo ni util para el estudiante, pero es un caso raro que podria no justificar una regla adicional.

**Pregunta**: Debe haber un largo minimo para titulos de knowledges?

- [ ] Opcion A: No evaluar minimo, solo importa que no excedan el maximo — simplicidad, alineado con la referencia
- [ ] Opcion B: Establecer un minimo de 3 caracteres, titulos mas cortos reciben puntuacion 0.0 — protege contra titulos no descriptivos
- [ ] Opcion C: Establecer un minimo pero con penalizacion gradual en lugar de puntuacion 0.0

### Doubt[DOUBT-EMPTY-INSTRUCTIONS] - Como puntuar knowledges sin instrucciones?
**Status**: OPEN

Algunos knowledges no tienen instrucciones (campo vacio o nulo). El requisito actual (R006) no penaliza la ausencia de instrucciones (puntuacion 1.0), asumiendo que son opcionales. Sin embargo, si el equipo de contenido considera que todos los knowledges deberian tener instrucciones, la ausencia deberia penalizarse.

**Pregunta**: Cual debe ser la puntuacion de un knowledge sin instrucciones?

- [ ] Opcion A: 1.0 — no penalizar, las instrucciones son opcionales y su ausencia no afecta la experiencia
- [ ] Opcion B: 0.0 — penalizar, todos los knowledges deberian tener instrucciones para guiar al estudiante
- [ ] Opcion C: No producir puntuacion — excluir del analisis los knowledges sin instrucciones, similar a como FEAT-SLEN excluye quizzes que no son oraciones
