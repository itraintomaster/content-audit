---
patch: FEAT-LABS
requirement: 2026-03-28.03_lemma-absence
generated: 2026-07-16T00:00:00Z
---

# Tech Spec: Vocabulario C1/C2 y palabras fuera de catalogo en lemma-absence

## Extender CefrLevel con C1 y C2

El catalogo descarta hoy las entradas C1/C2 al cargar porque el enum no puede representarlas: ese descarte es la causa raiz del punto ciego de F-LABS-R020 (la quiz A1 "It hails every year." con "hail" C2 puntuaba 1.0). Extender el enum habilita la retencion en la carga sin tocar la firma de ningun port. El confinamiento de los niveles nuevos (solo mal-ubicacion, jamas cobertura — F-LABS-R033/R037) queda documentado en la descripcion de cada constante para que ningun consumidor futuro asuma que participan de umbrales o pesos.

```architecture
modules:
  - name: audit-domain
    _change: modify
    models:
      - name: CefrLevel
        _change: modify
        type: enum
        fields:
          - { name: C1, _change: add }
          - { name: C2, _change: add }
```

## Agregar el fallback por lema a EvpCatalogPort

F-LABS-R036 define un emparejamiento en DOS pasos con precedencia estricta: (1) match exacto por lema+POS con el metodo existente `lookupLevel(LemmaAndPos)` — que este patch NO toca y sigue siendo el primer paso obligatorio, usando el nivel de ESA entrada aunque otra POS del mismo lema tenga nivel menor —; (2) solo cuando no existe entrada para el par, el metodo nuevo resuelve el fallback devolviendo el MENOR nivel entre las POS disponibles del lema (beneficio de la duda: el fallback nunca penaliza mas). Usar el minimo-por-lema en todos los casos seria incorrecto: sub-penalizaria cuando la entrada exacta tiene nivel mayor que otra POS del mismo lema (ej.: "record" verbo B1 en oracion A2 debe penalizar via paso 1, no resolverse al A2 del sustantivo). Optional vacio en el paso 2 significa fuera de catalogo y deriva a las bandas de F-LABS-R034. Este metodo NO participa del calculo de ausencia: la presencia relajada de F-LABS-R039 se implementa comparando el conjunto de lemas PRESENTES en el curso indexado por lema solo (se relaja el lado curso de la comparacion, no el lado catalogo) y es expresable con `getExpectedLemmas` existente, sin cambio de contrato.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: EvpCatalogPort
        _change: modify
        exposes:
          - signature: "lookupLevelByLemma(String lemma): Optional<CefrLevel>"
            _change: add
```

## Parametrizar las bandas de fuera-de-catalogo en LemmaAbsenceConfig

F-LABS-R034 define bandas de frecuencia cuyos umbrales dependen del nivel de la oracion (ancla del usuario: A1 sin penalidad hasta ranking 1000) y dos descuentos planos (leve 0.1, fuerte 0.3). Se sigue el patron ya establecido en esta misma interfaz de valores parametrizados por CefrLevel (thresholds, coverage targets), de modo que los numeros de la tabla — marcados ASSUMPTION en el requirement — sean configurables sin tocar el analizador. La interfaz es sealed: DefaultLemmaAbsenceConfig sigue siendo la unica implementacion y absorbe los valores por defecto.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: LemmaAbsenceConfig
        _change: modify
        exposes:
          - signature: "getOutOfCatalogNoPenaltyRankBound(CefrLevel level): int"
            _change: add
          - signature: "getOutOfCatalogMildDiscountRankBound(CefrLevel level): int"
            _change: add
          - signature: "getOutOfCatalogMildDiscount(): double"
            _change: add
          - signature: "getOutOfCatalogStrongDiscount(): double"
            _change: add
```

## Registrar el detalle observable de penalizaciones en el diagnosis de quiz

F-LABS-R038 exige exponer cada lema que genero descuento con su motivo, y prohibe inventar un nivel CEFR para las palabras fuera de catalogo. En lugar de hacer nullable el expectedLevel de MisplacedLemma (invariante por convencion), la variante fuera-de-catalogo se modela como record hermano — el tipo mismo es el discriminador del motivo y garantiza el invariante. MisplacedLemma gana la POS observada (puede diferir de la del catalogo cuando opero el fallback de R036) y el descuento aplicado; ambos nullables porque los reportes serializados previos no los traen.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: labs
        _change: modify
        models:
          - name: OutOfCatalogWord
            _change: add
            type: record
            fields:
              - { name: lemma, type: String }
              - { name: observedPos, type: String }
              - { name: frequencyRank, type: Integer }
              - { name: discount, type: double }
          - name: MisplacedLemma
            _change: modify
            fields:
              - { name: observedPos, type: String, _change: add }
              - { name: discount, type: Double, _change: add }
          - name: LemmaPlacementDiagnosis
            _change: modify
            fields:
              - { name: outOfCatalogWordCount, type: int, _change: add }
              - { name: outOfCatalogWords, type: "List<OutOfCatalogWord>", _change: add }
```

## Propagar el detalle al contexto de correccion del plan

F-LABS-R038 pide que las nuevas penalizaciones produzcan tareas corregibles por el pipeline existente de planes y revisiones. Se replica el patron misplacedLemmas: un record espejo en refiner-domain (el diagnostico de audit-domain no cruza el carrier JSON de revise) y una lista nueva en LemmaAbsenceCorrectionContext, vacia — nunca null — cuando no hay penalizadas o el reporte fuente es previo. MisplacedLemmaContext gana el descuento para que el agente priorice que palabra reemplazar.

```architecture
modules:
  - name: refiner-domain
    _change: modify
    models:
      - name: OutOfCatalogWordContext
        _change: add
        type: record
        fields:
          - { name: lemma, type: String }
          - { name: observedPos, type: String }
          - { name: frequencyRank, type: Integer }
          - { name: discount, type: double }
      - name: MisplacedLemmaContext
        _change: modify
        fields:
          - { name: discount, type: Double, _change: add }
      - name: LemmaAbsenceCorrectionContext
        _change: modify
        fields:
          - { name: outOfCatalogWords, type: "List<OutOfCatalogWordContext>", _change: add }
```
