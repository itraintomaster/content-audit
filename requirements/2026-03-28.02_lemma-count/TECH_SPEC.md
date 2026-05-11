---
patch: ARCH-LCOUNT-001
requirement: 2026-03-28.02_lemma-count
generated: 2026-05-10T00:00:00Z
---

# Tech Spec: Analisis de Conteo de Repeticiones de Lemas Content-Word

## Encapsular el analisis bajo un paquete `lemmacount` interno en audit-domain

El analizador comparte forma con `lrec` y `labs`: un grafo cohesivo de modelos, una capability accesoria (la resolucion de CEFR) y un unico punto de entrada que el composition root instancia. Lo empaquetamos en `audit-domain.lemmacount` con `visibility: internal` para que los unicos seams visibles desde otros modulos sean el `ContentAnalyzer` que ya conocen y el `LemmaCountConfig` que vive en la raiz del modulo. Asi el interior (resolver, models, diagnoses) queda libre para evolucionar sin filtrarse a `audit-application` ni al CLI.

```architecture
modules:
  - name: "audit-domain"
    _change: "modify"
    packages:
      - name: "lemmacount"
        _change: "add"
        description: "Conteo de repeticiones de lemas content-word por oraciones distintas en todo el curso. Asigna un nivel CEFR a cada lema con la cadena EVP -> NLP -> 'no asignado', calcula score por lema (min(count/N, 1.0)), agrega por nivel CEFR (promedio simple) y por curso (promedio simple de niveles)."
        visibility: "internal"
```

## Exponer `LemmaCountConfig` como port sealed con `getThreshold()`

El analizador necesita un unico parametro de configuracion: el umbral N (R006, R007, R008). Lo modelamos como port en la raiz de `audit-domain`, en linea con `LemmaRecurrenceConfig` y `LemmaAbsenceConfig`, y lo extendemos de `SelfDescribingConfig` para que `AnalyzerRegistry` pueda exponerlo en el comando `config analyzer`. Lo marcamos `sealed: true` porque hoy no hay caso de plugin externo: la unica implementacion legitima es la default del composition root, y queremos que cualquier futura variante pase por revision arquitectonica (P7). R007 (default 4) y R008 (validacion de N invalido) son responsabilidades de la implementacion default, no del contrato — el contrato solo asegura que `getThreshold()` siempre devuelve el N efectivo.

```architecture
modules:
  - name: "audit-domain"
    _change: "modify"
    interfaces:
      - name: "LemmaCountConfig"
        _change: "add"
        stereotype: "port"
        sealed: true
        extends: ["SelfDescribingConfig"]
        exposes:
          - signature: "getThreshold(): int"
```

## Aislar la cadena CEFR detras de `LemmaCefrLevelResolver`

R009 (EVP) -> R010 (NLP latente) -> R011 (no asignado) es una capability propia, separable del conteo. La extraemos en una interfaz dedicada `LemmaCefrLevelResolver` y le damos una implementacion `EvpThenNlpLemmaCefrLevelResolver` que inyecta `EvpCatalogPort`. Hoy el `NlpToken` del proyecto no expone CEFR, asi que el resolver implementa la cadena con EVP y, cuando el EVP no cubre el lema, devuelve `Optional.empty` — exactamente el comportamiento descripto en el detalle de R010 para el caso "NLP no aporta CEFR". La interfaz queda lista para una segunda implementacion el dia que el tokenizer empiece a entregar nivel: P6 (una capability = un seam), sin generalizar de mas. Manteniendo este resolver fuera del analyzer permite que `LemmaCountAnalyzer` se mantenga concentrado en el conteo y el scoring.

```architecture
modules:
  - name: "audit-domain"
    _change: "modify"
    packages:
      - name: "lemmacount"
        _change: "modify"
        interfaces:
          - name: "LemmaCefrLevelResolver"
            _change: "add"
            stereotype: "port"
            description: "Resuelve el nivel CEFR de un lema aplicando la cadena de fuentes definida en R009 (EVP) -> R010 (NLP si esta disponible) -> R011 (no asignado, devuelve Optional.empty)."
            exposes:
              - signature: "resolve(LemmaAndPos lemmaAndPos): Optional<CefrLevel>"
        implementations:
          - name: "EvpThenNlpLemmaCefrLevelResolver"
            _change: "add"
            visibility: "public"
            implements: ["LemmaCefrLevelResolver"]
            requiresInject:
              - name: "evpCatalogPort"
                type: "EvpCatalogPort"
                description: "Fuente primaria de nivel CEFR (R009)."
```

## Anadir `lookupLevel(LemmaAndPos)` a `EvpCatalogPort`

El catalogo EVP hoy solo expone "que lemas espero en este nivel". El resolver necesita la consulta inversa: "en que nivel CEFR esta este lema (si lo esta)". Anadimos una sola firma a `EvpCatalogPort`; mantiene el catalogo como unica fuente de verdad sobre EVP (no duplicamos indices en el resolver) y deja la implementacion del port libre de elegir la representacion interna. Devuelve `Optional<CefrLevel>` para que el resolver pueda encadenar limpiamente con el siguiente paso de la cadena (R010/R011).

```architecture
modules:
  - name: "audit-domain"
    _change: "modify"
    interfaces:
      - name: "EvpCatalogPort"
        _change: "modify"
        exposes:
          - signature: "lookupLevel(LemmaAndPos lemmaAndPos): Optional<CefrLevel>"
            _change: "add"
```

## Modelar `LemmaCountStats` y los carriers del reporte

El reporte que pide el grupo D necesita cuatro nociones distintas: el dato por lema (`LemmaCountStats`), el detalle por nivel CEFR con sus sub-expuestos (`LevelLemmaCountResult`), la fila informativa del grupo "no asignado" sin score (`UnassignedLemmaEntry`) y el resultado consolidado del analisis (`LemmaCountResult`). Los modelamos como records inmutables en el paquete `lemmacount`. `courseScore` es `Optional<Double>` porque R013 contempla explicitamente el caso "ningun nivel CEFR con score" (R015 dice que el reporte debe indicar esa condicion en vez de devolver un valor arbitrario). `UnassignedLemmaEntry` deliberadamente no tiene `score` para hacer cumplir R014 en tipos: el agregador del curso no puede sumar lo que no existe.

```architecture
modules:
  - name: "audit-domain"
    _change: "modify"
    packages:
      - name: "lemmacount"
        _change: "modify"
        models:
          - name: "LemmaCountStats"
            _change: "add"
            type: "record"
            fields:
              - name: "lemmaAndPos"
                type: "LemmaAndPos"
                description: "Lema content-word con su POS (par identificador segun R002)"
              - name: "count"
                type: "int"
                description: "Numero de oraciones distintas del curso que contienen el lema (R004, R005)"
              - name: "score"
                type: "double"
                description: "Score del lema: min(count/N, 1.0) (R006)"
              - name: "assignedLevel"
                type: "Optional<CefrLevel>"
                description: "Nivel CEFR asignado por la cadena R009 -> R010; vacio si el lema cae al grupo 'no asignado' (R011)"
          - name: "LevelLemmaCountResult"
            _change: "add"
            type: "record"
            fields:
              - name: "level"
                type: "CefrLevel"
                description: "Nivel CEFR cuyo score se reporta"
              - name: "score"
                type: "double"
                description: "Score del nivel: promedio simple de los scores de sus lemas (R012)"
              - name: "totalLemmas"
                type: "int"
                description: "Total de lemas evaluados asignados a este nivel (R017)"
              - name: "subExposedLemmas"
                type: "List<LemmaCountStats>"
                description: "Lemas del nivel con score < 1.0 (R017); ordenados por count ascendente para priorizar"
          - name: "UnassignedLemmaEntry"
            _change: "add"
            type: "record"
            fields:
              - name: "lemmaAndPos"
                type: "LemmaAndPos"
                description: "Lema sin nivel CEFR asignado por ninguna fuente (R011)"
              - name: "count"
                type: "int"
                description: "Conteo de oraciones distintas con el lema (R018); no se calcula score"
          - name: "LemmaCountResult"
            _change: "add"
            type: "record"
            fields:
              - name: "thresholdN"
                type: "int"
                description: "Umbral N usado para calcular los scores (R016)"
              - name: "courseScore"
                type: "Optional<Double>"
                description: "Score global del curso: promedio simple de los scores de los niveles CEFR (R013, R015). Vacio si ningun nivel tiene score asignable."
              - name: "levels"
                type: "List<LevelLemmaCountResult>"
                description: "Detalle por nivel CEFR con score (R017). Niveles sin lemas asignados no aparecen (R012)."
              - name: "unassigned"
                type: "List<UnassignedLemmaEntry>"
                description: "Lemas que no pudieron clasificarse en ningun nivel CEFR (R011, R018). Informativos: no entran al score del curso (R014)."
```

## Conectar los resultados al arbol `AuditNode` via diagnoses tipados

`AuditNode.diagnoses` es la via canonica por la que cada feature inyecta su informacion al arbol y la expone a CLI/UI sin contaminar `metadata`. El resultado consolidado vive a nivel curso (`LemmaCountCourseDiagnosis` con todo el `LemmaCountResult`), pero el detalle por nivel CEFR pide tambien un anclaje por nivel para que las consultas del comando `get` o futuros refiners puedan navegar al sub-arbol de un nivel y leer solo lo que les corresponde (`LemmaCountLevelDiagnosis`). Sumamos los getters opcionales correspondientes a `CourseDiagnoses` y `LevelDiagnoses` en linea con como `labs` ya lo hace.

```architecture
modules:
  - name: "audit-domain"
    _change: "modify"
    interfaces:
      - name: "CourseDiagnoses"
        _change: "modify"
        exposes:
          - signature: "getLemmaCountDiagnosis(): Optional<LemmaCountCourseDiagnosis>"
            _change: "add"
      - name: "LevelDiagnoses"
        _change: "modify"
        exposes:
          - signature: "getLemmaCountDiagnosis(): Optional<LemmaCountLevelDiagnosis>"
            _change: "add"
    packages:
      - name: "lemmacount"
        _change: "modify"
        models:
          - name: "LemmaCountCourseDiagnosis"
            _change: "add"
            type: "record"
            fields:
              - name: "result"
                type: "LemmaCountResult"
                description: "Resultado consolidado del analisis: score global, N usado, detalle por nivel y lemas no asignados."
          - name: "LemmaCountLevelDiagnosis"
            _change: "add"
            type: "record"
            fields:
              - name: "levelResult"
                type: "LevelLemmaCountResult"
                description: "Resultado de este nivel CEFR concreto (score, total de lemas, sub-expuestos)."
```

## Exponer `LemmaCountAnalyzer` como unica seam publica del paquete

`LemmaCountAnalyzer` es el unico punto que el composition root necesita instanciar: lo declaramos `visibility: public` para que el `Main` pueda inyectarlo en el `DefaultAuditRunner`. El resto del paquete (resolver implementation incluida, salvo su uso como dependencia inyectable) queda accesible solo dentro del paquete o detras de su interfaz publica. Reusamos `ContentWordFilter` para R001 (mismo filtro POS que comparten `lrec` y `labs`) y delegamos toda la cadena CEFR al resolver, lo que mantiene el analyzer concentrado en R003-R008 y R012-R013.

```architecture
modules:
  - name: "audit-domain"
    _change: "modify"
    packages:
      - name: "lemmacount"
        _change: "modify"
        implementations:
          - name: "LemmaCountAnalyzer"
            _change: "add"
            visibility: "public"
            implements: ["ContentAnalyzer"]
            requiresInject:
              - name: "contentWordFilter"
                type: "ContentWordFilter"
                description: "Aplica el filtro POS content-word del universo (R001)."
              - name: "lemmaCefrLevelResolver"
                type: "LemmaCefrLevelResolver"
                description: "Asigna CEFR al lema con la cadena R009 -> R010 -> R011."
              - name: "lemmaCountConfig"
                type: "LemmaCountConfig"
                description: "Provee el umbral N usado en el score (R006, R007, R008, R016)."
```

## Anclar el config default y su registro en `audit-application`

`DefaultLemmaCountConfig` es donde viven las decisiones operativas: aplicar 4 cuando el campo no esta (R007), fallar al cargar la configuracion cuando el campo es invalido (R008) y exponer el N efectivo via `SelfDescribingConfig.describe()` para que el comando `config analyzer` lo muestre. Lo declaramos en `audit-application` con `types: [Component]` siguiendo el patron exacto de `DefaultLemmaAbsenceConfig`; el `DefaultAnalyzerRegistry` lo recoge automaticamente porque inyecta `List<SelfDescribingConfig>`, lo que cubre R016 sin tocar el registry. La interfaz `LemmaCountConfig` esta sealed, asi que esta implementacion es la unica permitida por contrato.

```architecture
modules:
  - name: "audit-application"
    _change: "modify"
    implementations:
      - name: "DefaultLemmaCountConfig"
        _change: "add"
        visibility: "public"
        implements: ["LemmaCountConfig"]
        types: ["Component"]
```
