---
patch: ARCH-RCLALEN-001
requirement: 2026-04-28.02_rcla-length-aware-context
generated: 2026-05-04T22:40:00Z
---

# Tech Spec: Conciencia de longitud en el contexto LEMMA_ABSENCE

Este patch es deliberadamente **chico**: extiende un registro existente (`LemmaAbsenceCorrectionContext`) con cinco campos de longitud y agrega una enum (`LengthDirection`). No introduce puertos, ni paquetes, ni analyzers, ni nuevos recorridos del arbol. Toda la mecanica de poblado vive dentro del resolver y prompt builder ya existentes — son cambios de implementacion, no de contrato. La razon de fondo: el `SentenceLengthDiagnosis` ya esta materializado por FEAT-SLEN/FEAT-DSLEN sobre el mismo nodo quiz que ya consume FEAT-RCLA, asi que enriquecer el contexto es leer un diagnostico hermano del que ya se lee, no construir nada nuevo.

> **Nota de revision (2026-05-04)**: el bug Sentinel `2026-05-04-01-boxed-types-emitted-as-primitives` colapsa `type: Integer` a `int` primitivo en el codigo generado, invalidando la premisa de nullability sobre la que se diseno la version original de este patch. La seccion **"Encoding de ausencia: discriminador unico via `lengthDirection`"** mas abajo documenta el ajuste de contrato semantico que hace el diseno realizable con la superficie arquitectonica actual. La superficie del patch (la enum + los cinco campos en el record) no cambia: el ajuste vive enteramente en el contrato del resolver y los formatters / prompt builder, capturado aqui en lugar de en `sentinel.yaml`.

## Anadir la enum LengthDirection en refiner-domain

`LengthDirection` materializa la senal accionable que el sistema deriva del signo de `delta` (F-RCLALEN-R002). La pre-calculamos del lado del sistema en lugar de delegar la aritmetica al prompt del LLM por dos razones explicitas en el requirement: la senal pasa a ser deterministica (mismo input -> misma direccion, no depende del modelo) y la CLI puede mostrar una etiqueta legible para humanos sin reglas duplicadas. La cuarta variante `UNKNOWN` no es un caso degenerado — es la representacion canonica de "no hay diagnostico de longitud disponible" (F-RCLALEN-R004), explicita en lugar de `null`, para que el prompt del LLM y los formatters traten ese caso como un valor mas, no como una rama de error. Con el ajuste documentado mas abajo, `UNKNOWN` pasa ademas a ser el **discriminador unico** que decide si los cuatro numericos son significativos o deben suprimirse en la salida observable. La enum vive en el module root de `refiner-domain` (no en un sub-paquete propio) porque es un value-object de uso transversal al modulo, sin internals que esconder. La marcamos como `glossarySuggestions` por si el analista decide elevarla a termino canonico cuando el glosario se materialice.

```architecture
modules:
  - name: refiner-domain
    _change: modify
    models:
      - name: LengthDirection
        _change: add
        type: enum
        visibility: public
        glossarySuggestions:
          - name: Length Direction
            technicalName: LengthDirection
            kind: value-object-candidate
            basedOn: refiner-domain/LengthDirection
            derivedFrom: []
        fields:
          - name: SHORTEN
          - name: LENGTHEN
          - name: KEEP_SAME
          - name: UNKNOWN
```

## Extender LemmaAbsenceCorrectionContext con cuatro campos numericos

Los cuatro numericos (`tokenCount`, `targetMin`, `targetMax`, `delta`) reusan a proposito los nombres y la semantica de `SentenceLengthCorrectionContext` (F-RCSL-R001) para que un consumidor — LLM o humano — que ya leyo el contexto SENTENCE_LENGTH lea este sin diccionario. La declaracion DSL los expresa como `Integer` (boxed) precisamente para senalizar que pueden quedar ausentes cuando no hay `SentenceLengthDiagnosis` (F-RCLALEN-R004); el codigo emitido los representa como `int` primitivo a causa del bug Sentinel `2026-05-04-01-boxed-types-emitted-as-primitives`. La consecuencia practica es que el **modelo en si no puede distinguir "ausente" de "cero"**, y la nullability semantica de R004 / R005 se preserva via el discriminador descrito en la siguiente seccion. La declaracion `Integer` se mantiene en el patch como fuente de verdad de la intencion: cuando el bug se arregle, el codigo emitido pasara a ser nullable sin tocar este patch.

```architecture
modules:
  - name: refiner-domain
    _change: modify
    models:
      - name: LemmaAbsenceCorrectionContext
        _change: modify
        type: record
        fields:
          - name: tokenCount
            type: Integer
            _change: add
          - name: targetMin
            type: Integer
            _change: add
          - name: targetMax
            type: Integer
            _change: add
          - name: delta
            type: Integer
            _change: add
```

## Agregar lengthDirection como campo no nullable

`lengthDirection` se agrega como campo del mismo registro pero con dos diferencias frente a los cuatro numericos. Primero, **no es nullable** — siempre tiene valor: `SHORTEN` / `LENGTHEN` / `KEEP_SAME` cuando el diagnostico esta presente, `UNKNOWN` cuando no. Esto cumple F-RCLALEN-R005 (el JSON serializa `lengthDirection: "UNKNOWN"` aun cuando el resto de los campos numericos se omitan) sin necesidad de logica especial en el formatter. Segundo, su computo es del lado del resolver — `LemmaAbsenceContextResolver` recibe el `delta` del diagnostico y deriva la direccion segun la tabla de F-RCLALEN-R002; el contexto materializa el resultado, no la formula. El consumo posterior (prompt builder LLM, formatters JSON/texto) lee el campo como cualquier otro getter, sin re-derivar nada.

```architecture
modules:
  - name: refiner-domain
    _change: modify
    models:
      - name: LemmaAbsenceCorrectionContext
        _change: modify
        type: record
        fields:
          - name: lengthDirection
            type: LengthDirection
            _change: add
```

## Encoding de ausencia: discriminador unico via `lengthDirection`

Esta seccion no introduce cambios al patch — documenta el contrato semantico del resolver y los formatters / prompt builder que reemplaza la nullability de los cuatro numericos perdida por el bug del generator.

**Invariante del resolver.** `LemmaAbsenceContextResolver` siempre instancia el record con los cinco campos llenos. Cuando lee `QuizDiagnoses.getSentenceLengthDiagnosis()`:

- Si el diagnostico esta presente: copia `tokenCount`, `targetMin`, `targetMax`, `delta` del diagnostico y deriva `lengthDirection` segun F-RCLALEN-R002 (`SHORTEN` / `LENGTHEN` / `KEEP_SAME`).
- Si el diagnostico esta ausente: pone los cuatro numericos en `0` y `lengthDirection = UNKNOWN`. Los ceros son **placeholders sin significado semantico** — el sistema garantiza que ningun consumidor los lea sin antes mirar el discriminador.

**Invariante de los consumidores.** Todo consumidor que lea los cuatro numericos consulta primero `lengthDirection`:

- `lengthDirection != UNKNOWN`: emitir `tokenCount` / `targetMin` / `targetMax` / `delta` en su forma normal (claves JSON presentes con su valor; linea CLI `Length: N tokens (target: A-B, delta: ±D, direction: ...)`; seccion de longitud presente en el prompt LLM).
- `lengthDirection == UNKNOWN`: omitir las claves `tokenCount`, `targetRange` y `delta` del JSON (F-RCLALEN-R005); rendear la linea CLI como `Length: (unavailable, direction: UNKNOWN)` (F-RCLALEN-R006); omitir la seccion de longitud del prompt LLM.

**Por que este encoding es suficiente.** Las reglas observables F-RCLALEN-R005 y F-RCLALEN-R006 hablan del JSON y de la salida CLI — no del estado interno del record. Mientras los formatters respeten el discriminador, el comportamiento al cliente es identico al que tendria un record con los cuatro numericos genuinamente nullables. El record en si pierde una invariante interna ("si los numericos son `0`, eso podria significar ausencia o KEEP_SAME en el limite inferior con delta cero"), pero esa invariante nunca se inspecciona — la unica fuente de verdad es `lengthDirection`. Esto reduce el modelo a un value-object con un discriminador explicito, patron analogo a un `Optional<NumericLengthSignal>` plano pero sin requerir un sub-record.

**Alternativa considerada y descartada: sub-record `LengthSignal`.** Habriamos podido introducir `record LengthSignal(int tokenCount, int targetMin, int targetMax, int delta)` y un campo `LengthSignal lengthSignal` (referencia nullable) en el contexto. Eso modela R004 estructuralmente sin depender del discriminador. Lo descartamos por tres razones: (a) el patron discriminador con `UNKNOWN` ya existe y es suficiente — agregar el sub-record duplica la senal; (b) introducir un nuevo tipo solo para esquivar un bug del generator es deuda arquitectonica que sobrevivira al fix; (c) el cambio del sub-record requiere reescribir tests, resolver, prompt builder y formatters con una estructura nueva, mientras que el discriminador aprovecha la enum y los campos que ya estan emitidos.

**Reversibilidad.** Cuando Sentinel arregle el bug del generator y `Integer` vuelva a emitirse como wrapper boxed, el contrato del resolver puede migrar en una pasada sencilla: poner los cuatro getters como `null` cuando no hay diagnostico, y los consumidores pasan a discriminar por `tokenCount == null` (o cualquier numerico) en lugar de por `lengthDirection == UNKNOWN`. La enum `UNKNOWN` puede sobrevivir como redundancia explicita o eliminarse — esa decision queda diferida al momento del fix. Ningun cliente externo lee los campos sin pasar por los formatters, por lo que la migracion es interna al modulo `refiner-domain` mas las partes de `audit-cli` y `revision-infrastructure` que ya tocan estos campos.

## Por que no introducimos puertos, paquetes ni nuevas implementaciones

El requirement habilita explicitamente esta austeridad estructural: F-RCLALEN-R003 dice que los datos vienen de `QuizDiagnoses.getSentenceLengthDiagnosis()` (ya expuesto por FEAT-DSLEN R004) sobre el mismo nodo que `LemmaAbsenceContextResolver` ya navega para obtener `LemmaPlacementDiagnosis`. Eso significa que: (a) no hay nuevo recorrido del arbol — la firma de `CorrectionContextResolver.resolve(AuditReport, RefinementTask)` cubre el caso sin cambios; (b) no hay nueva dependencia de modulo — `refiner-domain` ya depende de `audit-domain`; (c) no hay nuevo analyzer — el diagnostico ya esta producido por FEAT-SLEN; (d) la firma de `LemmaAbsencePromptBuilder.buildUserPrompt(LemmaAbsenceCorrectionContext)` no cambia porque el contexto **es** el mismo tipo, solo con campos adicionales. La consecuencia de esto es importante: la mayoria del trabajo de FEAT-RCLALEN sucede al nivel de tests (qa-tester anade la traceabilidad por rule sobre el resolver, prompt builder y formatters existentes) y de implementacion (developer modifica los cuerpos de `LemmaAbsenceContextResolver`, `DefaultLemmaAbsencePromptBuilder` y los metodos privados de `GetCmd` que rendean el contexto, respetando el invariante del discriminador). Ninguna de esas modificaciones cambia la superficie arquitectonica que vive en `sentinel.yaml`, por eso este patch se queda en model-only.
