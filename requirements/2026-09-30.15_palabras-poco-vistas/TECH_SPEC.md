---
patch: FEAT-POCOV
requirement: 2026-09-30.15_palabras-poco-vistas
generated: 2026-09-30T19:50:00Z
---

# Tech Spec: Palabras poco vistas, con umbral por banda (FEAT-POCOV)

Paquete D, ronda 1. `lemma-count` conserva su nombre, su cálculo y su falta de tareas (José, 30/9), y suma tres cosas: el
umbral de cada palabra sale de su banda de frecuencia, publica una fila por palabra, y deja hallazgos que sólo ordenan.
Diseñado contra el modelo de 0.2 y 0.3: **vienen de 0.3** `ContentAnalyzer.findingsAt`, `FindingDraft` y el resto del
hallazgo, y el `LemmaCountAnalyzerProvider` con su ficha, que no cambia de forma. Lo compartido está en
`ronda1/ganchos-D.md`: las sugeridas, las palabras escasas y el orden dentro de su grupo (D-07 a D-09). Todo lo medido sale del
análisis 2026-09-30T11-54-02, que el prototipo reproduce exacto: curso 0,6698448 y A1 515/62, A2 810/418, B1 885/688, B2
384/366, C1 3/1.

## Poner el umbral en una tabla de bandas, y dejarla como configuración
José eligió 8 · 6 · 4 · 3 y lo confirma antes de construir, porque mueve el número del curso: por eso el umbral es una
tabla de cuatro bandas por puesto COCA en la configuración, no un cálculo. «4 para todas» son las mismas cuatro bandas con
4 y da exactamente lo de hoy (R001 inv. 1), así que volver atrás es cambiar un valor: el cargador sigue siendo
`load(String)`, entiende «4» y «8/6/4/3», y su valor por omisión es la única línea que cambia. La última banda no tiene
tope y toma también las palabras sin puesto, que son raras por definición. La lista vacía existe sólo para configuraciones
de prueba: entonces toda palabra usa `getThreshold()`, y los 26 tests de LCOUNT que simulan la configuración siguen dando
lo mismo sin tocarlos.

```architecture
modules:
  - name: audit-domain
    _change: modify
    models:
      - name: LemmaCountBand
        _change: add
        type: record
        fields:
          - { name: name, type: String }
          - { name: maxCocaRank, type: Integer }
          - { name: threshold, type: int }
    interfaces:
      - name: LemmaCountConfig
        _change: modify
        exposes:
          - signature: "getBands(): List<LemmaCountBand>"
            _change: add
  - name: audit-application
    _change: modify
    implementations:
      - name: DefaultLemmaCountConfig
        _change: modify
        requiresInject:
          - { name: bands, type: "List<LemmaCountBand>", _change: add }
```

## Sacar la banda del puesto del catálogo
La banda de una palabra sale de su puesto COCA en el catálogo EVP para su lema y su categoría: es el mismo que muestran las
sugeridas y el semáforo, y no depende de qué ejercicio aparece primero ni de su forma escrita (R001 inv. 3). El resolutor de
nivel ya envuelve al catálogo, así que suma una pregunta en vez de inyectarle al analizador una dependencia nueva: el
constructor de `LemmaCountAnalyzer` (27 construcciones) no cambia, y en los tests un resolutor simulado responde «sin puesto».
El prototipo que dio 73,5 % tomaba el puesto de la primera oración: en C1, que tiene 3 palabras y pesa un quinto del curso,
«hop» (que es «hoped» mal leído por el lematizador) tomaba el 413 de «hope» y pedía 8 oraciones; con el catálogo, 5.603 y
pide 3. Por eso con las bandas el vocabulario da 73,86 % y no 73,5: es la duda nueva DOUBT-PUESTO-DE-LA-BANDA.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: lemmacount
        _change: modify
        visibility: internal
        interfaces:
          - name: LemmaCefrLevelResolver
            _change: modify
            exposes:
              - signature: "resolveCocaRank(LemmaAndPos lemmaAndPos): Optional<Integer>"
                _change: add
```

## Publicar una fila por palabra, con su banda, su umbral y sus temas
Los hallazgos, el semáforo y las sugeridas necesitan saber, de cada palabra, qué N se le aplicó, en qué banda está y qué
temas la usan (R005). Van en una tabla nueva dentro de `LemmaCountResult`, que tiene 3 construcciones, y no como campos de
`LemmaCountStats` (44, casi todas en los tests de F-SLEM) ni de `LevelLemmaCountResult` (27): la lista de sub-expuestos
sigue igual, con su orden por oraciones (F-LCOUNT-R017), y el `score` de cada una ya usa el N de su banda. La tabla trae
todas las palabras con nivel, no sólo las poco vistas, porque el semáforo muestra cuántas oraciones tiene cada palabra del
tema; va ordenada por banda, oraciones y puesto (R004), que es el orden que el contrato no deja poner en los hallazgos de un
nodo. `thresholdN` queda como umbral de referencia, el uniforme, para quien todavía no lee la banda. Son unas 2.600 filas.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: lemmacount
        _change: modify
        visibility: internal
        models:
          - name: LemmaExposure
            _change: add
            type: record
            fields:
              - { name: lemmaAndPos, type: LemmaAndPos }
              - { name: assignedLevel, type: CefrLevel }
              - { name: count, type: int }
              - { name: cocaRank, type: Integer }
              - { name: band, type: String }
              - { name: threshold, type: int }
              - { name: score, type: double }
              - { name: knowledgeIds, type: "List<String>" }
          - name: LemmaCountResult
            _change: modify
            fields:
              - { name: thresholdN, type: int, _change: modify }
              - { name: bands, type: "List<LemmaCountBand>", _change: add }
              - { name: exposures, type: "List<LemmaExposure>", _change: add }
```

## Dejar un hallazgo por palabra poco vista, que sólo ordena
Con la ficha de 0.3 (`lemma-count` evalúa MILESTONE y COURSE), `findingsAt` devuelve en cada nivel con puntaje menor que 1
un borrador por palabra de la tabla con `score` menor que 1: regla F-POCOV-R003, marca `lema|categoría`, gravedad baja,
resolución «sólo ordena»; la evidencia dice «education (B1): 1 oración de 4; banda de las 1.000 más comunes» y nombra sus
temas. En el curso, uno por nivel con puntaje menor que 1, marcado con el nivel. C1 no tiene nodo en el curso, así que su
palabra poco vista («hop») se nombra en el hallazgo del curso sobre C1. Con 4 para todas son 1.534 de nivel y 5 de curso;
con bandas, 1.615 y 5. No crean tareas: sin ligadura al plan, como ya dejó 0.3, y «sólo ordena» no baja ningún puntaje.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: lemmacount
        _change: modify
        visibility: internal
        models:
          - name: LemmaExposure
            _change: add
            type: record
            fields:
              - { name: lemmaAndPos, type: LemmaAndPos }
              - { name: assignedLevel, type: CefrLevel }
              - { name: count, type: int }
              - { name: cocaRank, type: Integer }
              - { name: band, type: String }
              - { name: threshold, type: int }
              - { name: score, type: double }
              - { name: knowledgeIds, type: "List<String>" }
features:
  - id: FEAT-POCOV
    code: F-POCOV
```

## Usar en las sugeridas el umbral de la banda de cada palabra
Pregunta del orquestador: ¿las sugeridas usan el umbral de la banda de cada palabra? **Sí** (R007). Hoy la
corrección toma un solo N del diagnóstico del curso; con bandas, «number» (A1, 5 oraciones de 8) sería poco vista para el
hallazgo y expuesta para las sugeridas, y mantener 4 ni siquiera se puede calcular, porque la lista de sub-expuestos ya
viene cortada con el N de cada banda. Con el N de cada palabra, «poco vista» para las sugeridas es exactamente «hallazgo de
`lemma-count`», y F-SLEM-R013 sigue valiendo: el umbral informado y la marca coinciden. Efecto medido con 8 · 6 · 4 · 3: el
grupo de poco vistas pasa de 62 a 89 en A1 (entran number, word, eye, face; salen mobile, cow, picnic), de 418 a 461 en A2,
de 688 a 707 en B1 y de 366 a 358 en B2; en la lista de 10 que viaja con cada tarea de A2 entran real, describe y clearly,
y en B1 y B2 no cambia. Con 4 para todas, todo queda idéntico. El cambio es de cuerpo en el resolutor, el ordenador de
grupos y el inventario de nivel, que son compartidos: enganches D-07 y D-08.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: lemmacount
        _change: modify
        visibility: internal
        models:
          - name: LemmaCountResult
            _change: modify
            fields:
              - { name: exposures, type: "List<LemmaExposure>", _change: add }
```

## Fijar los dos números con una prueba que no necesita el curso real
Para @qa-tester (R001): una tabla fija del curso del 29/9 en los recursos de prueba, con una fila por lema de contenido
(lema, categoría, oraciones, nivel EVP o vacío, puesto EVP o vacío: 2.597 con nivel y 473 sin), y un curso sintético donde
el ejercicio k contiene todo lema con más de k oraciones, así que cada lema aparece en exactamente las suyas (597 como
máximo). Con 4 para todas, `lemma-count` tiene que dar A1 0,9388350, A2 0,7114198, B1 0,5251412, B2 0,3404948, C1
0,8333333, curso 0,6698448 y 1.535 poco vistas; con 8 · 6 · 4 · 3, 0,9077670, 0,6782407, 0,4903013, 0,3612196,
0,8888889, curso 0,6652835 y 1.616. Con los otros seis puntajes de curso publicados, eso es 73,92 % y 73,86 % de
vocabulario, y los niveles 96,54 · 94,16 · 79,45 · 63,51 contra 96,02 · 93,60 · 78,87 · 63,86. Si José eligiera el puesto de
la primera oración, la segunda fila pasa a curso 0,6389 y 73,48 %. La prueba de F-HALL-R013 del integrador corre lo mismo
de punta a punta sobre el curso real.

```architecture
modules:
  - name: audit-application
    _change: modify
    implementations:
      - name: DefaultLemmaCountConfig
        _change: modify
        requiresInject:
          - { name: bands, type: "List<LemmaCountBand>", _change: add }
```
