---
patch: FEAT-NIVFR
requirement: 2026-09-30.13_nivel-con-frases
generated: 2026-10-01T05:40:00Z
---

# Tech Spec: el nivel propio de la forma derivada (FEAT-NIVFR, parche del 1/10, aplicado en b5724f63 y construido)

La corrección del 1/10 dice que el nivel que el EVP le da a la palabra misma gana a cualquier lectura que tome el nivel
de una base, una parte, un par británico o la palabra castellana (F-NIVFR-R002 y R004), y el orquestador decidió que
sólo le gana el sufijo que enseña el tema: cuando el tema enseña justamente a formar la palabra, usarla es el ejercicio
(R003). Todo eso entró en el cuerpo de `OffLevelWordsAnalyzer`, sin parche (`3595f477` y `92bf8a21`): el curso del 29/9
pasó de 95 a 99 ejercicios y `escribir/` sigue en 26 de 52. Queda un hueco que el cuerpo no puede cerrar: el EVP trae
aparte formas derivadas y compuestas de otro lema («rewarding», C1, de reward; «smoking», A1, de smoke; «terrifying»,
B2, de terrify), y F-LABS-R040 las deja, con razón, fuera del nivel de su lema base. Ningún método del puerto las
consulta, así que para el analizador «rewarding» está fuera del EVP y la lectura del derivado la suelta, y «smoking» es
B2, por la entrada curada del 19/7, aunque el EVP la trae en A1. En el curso del 29/9 el parche no cambia ninguna marca
(un prototipo da las mismas 99): rewarding está en «-ed vs -ing» y smoking en «Doble consonante y e», y a las dos las
cubre el sufijo de su tema. Cuida lo que el curso no trae hoy: el derivado que el EVP pone dos niveles arriba o más,
en un tema que no enseña su sufijo, que la lectura de su base soltaría.

## Consultar la forma derivada por el puerto
`EvpCatalogPort` suma `lookupDerivedFormLevel(word)`: el menor nivel entre las entradas del catálogo cuya palabra
encabezado es `word` (sin distinguir mayúsculas) y que R040 trata como forma derivada o compuesta de otro lema; vacío
si no hay ninguna. Las frases y las variantes flexivas de R040 (clothes, DVD) no entran: ésas ya fijan el nivel de su
lema. `FileSystemEvpCatalog` llena un índice propio en la misma pasada en que calcula `derivedForm`, y no toca
`levelByLemma`, `lowestLevelByLemmaOnly` ni `expectedByLevel`: `lookupLevel`, `lookupLevelByLemma` y
`getExpectedLemmas` dan lo mismo, así que `lemma-absence`, `lexis`, el vocabulario de 73,9 % y las 4.036 tareas no se
mueven (F-NIVFR-R006). Sólo las formas derivadas, y no toda palabra encabezado, porque la que coincide con su lema ya
está en `lookupLevel`, y mezclarlas haría que el adjetivo «fine» (A1) soltara el sustantivo «fine» (multa, B1): la misma
falla que la corrección le sacó a la lectura de otra categoría.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: EvpCatalogPort
        _change: modify
        exposes:
          - signature: "lookupDerivedFormLevel(String word): Optional<CefrLevel>"
            _change: add
```

## El analizador recibe el catálogo por inyección
El proveedor y el analizador de `offlevelwords` reciben el puerto; la raíz de composición les pasa el mismo
`FileSystemEvpCatalog` que ya arma para el puntuador. El nivel de cada palabra sigue siendo el del puntuador compartido
(R001): el catálogo sólo decide si una lectura puede soltar lo que `lexis` ya marca, y nunca suma una palabra.
Fuera del catálogo, si la forma escrita o el lema es una forma derivada que el EVP trae, manda su nivel: a menos de dos
niveles del ejercicio, no marca; a dos o más, marca salvo que el tema la enseñe (el sufijo incluido), el lematizador la
haya leído mal o sea un nombre propio, y nunca la sueltan la base, la parte, el par británico ni el préstamo. Dentro del
catálogo, si la forma escrita es una forma derivada que el EVP trae a menos de dos niveles, no marca: «smoking» en
«smoke - smoking». No es la lectura por forma de DOUBT-LECTURAS-NUEVAS (opción C, que José no quiso): aquélla sumaba
marcas, 127 rescates con falsos como «living-room»; ésta sólo deja o suelta.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: offlevelwords
        _change: modify
        implementations:
          - name: OffLevelWordsAnalyzerProvider
            _change: modify
            requiresInject:
              - { name: evpCatalogPort, type: EvpCatalogPort, _change: add }
          - name: OffLevelWordsAnalyzer
            _change: modify
            requiresInject:
              - { name: evpCatalogPort, type: EvpCatalogPort, _change: add }
```

## Las pruebas (qa-tester)
Van en el mismo parche, para no hacer dos vueltas. En `OffLevelWordsAnalyzer`, 15: la forma derivada que el EVP trae
(smoking en una lista de un tema que no enseña -ing, rewarding a C1 aunque su base sea B1, terrifying a B2) y el derivado
que no trae (creamy), con «nicely» marcado donde el tema no enseña -ly (R002); la otra categoría sólo en una lista de
palabras («He paid a fine.» contra «run - running», R002); el par americano exacto (takeout sí, recess no) y la palabra
americana que el EVP trae con su nivel (truck, aunque lorry sea A2, R002); el sufijo del tema por encima del EVP (nicely
en un tema que enseña $-ly$, R003); y el préstamo sólo sustantivo (allergic marca, limousine no, adore leída como
sustantivo después de «They» sigue marcando, y rehabilitation, que el EVP trae a C1, no es préstamo, R004). En
`FileSystemEvpCatalog`, 3: el menor nivel de las formas derivadas de una palabra (rewarding C1), vacío para la palabra
que es su propio lema, el plural de su lema o parte de una frase (R002), y `lookupLevel` y `lookupLevelByLemma` sin
cambios (reward sigue en B1, R006). Todas se escriben contra el ayudante `Lexicon` de la prueba del analizador, que suma
las formas derivadas al mock del puerto, y contra un catálogo JSON chico en la del adaptador.

## Después de aplicar
`generate` regenera el puerto y deja los 18 stubs; el método de `FileSystemEvpCatalog`, su uso en el analizador y las
pruebas se escriben a mano (el prototipo del 1/10 está probado sobre el curso: `proto-v2/aplicar.py` en el scratchpad de
la sesión). Cambian cuatro construcciones: `Main`, la prueba del proveedor, el ayudante de la prueba del analizador y
J001, que se queda en 99 (9 · 27 · 43 · 20). El parche de la teoría del tema sigue en espera hasta después de la ronda
2, con su TECH_SPEC, en `en-espera/`; valida también contra el `sentinel.yaml` de `ronda2/integracion`.

Construido el 1/10 sobre `ronda2/integracion` (generate 9e93a983): `lookupDerivedFormLevel` y sus 3 pruebas (f70bf4ee),
su uso en el analizador y las construcciones del proveedor, que eran nueve y no cuatro: además de Main, la prueba del
proveedor, `Lexicon` y J001, también `IAuditEngineTest`, el semáforo, `DefaultRefinerEngineTest`,
`DefaultAuditRunnerTest`, `AnalyzeCmdTest` y `GetCmdTest` (b4f15968); y las 15 pruebas del analizador, que generate no
sumó a la clase (6f1028a6). Control sobre una copia: 99 ejercicios, 26 de 52 en `escribir/` y vocabulario 73,86 %.
