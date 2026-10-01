---
patch: FEAT-NIVFR
requirement: 2026-09-30.13_nivel-con-frases
generated: 2026-10-01T05:10:00Z
---

# Tech Spec: el nivel propio de la forma derivada (FEAT-NIVFR, parche del 1/10, sin aplicar)

La corrección del 1/10 dice que el nivel que el EVP le da a la palabra misma gana a cualquier lectura que tome el nivel
de una base, una parte, un sufijo del tema, un par británico o la palabra castellana (F-NIVFR-R002 y R004). Casi todo
entró en el cuerpo de `OffLevelWordsAnalyzer`, sin parche (commit `3595f477`): esas lecturas valen sólo fuera del
catálogo, el préstamo sólo para sustantivos y la variante americana sólo con un par exacto; el curso del 29/9 pasó de
95 a 104 ejercicios y `escribir/` sigue en 26 de 52. Queda un hueco que el cuerpo no puede cerrar: el EVP trae aparte
formas derivadas y compuestas de otro lema («rewarding», C1, de reward; «smoking», A1, de smoke; «terrifying», B2, de
terrify), y F-LABS-R040 las deja, con razón, fuera del nivel de su lema base. Ningún método del puerto las consulta,
así que para el analizador «rewarding» está fuera del EVP y la lectura del derivado la suelta, y «smoking» es B2, por
la entrada curada del 19/7, aunque el EVP la trae en A1. Un prototipo en el scratchpad, sobre el curso
del 29/9, da: rewarding vuelve a marcar en B1, smoking deja de marcar en «smoke - smoking» (A1), 104 ejercicios igual
(13 · 27 · 44 · 20), `escribir/` 26 de 52 con 40 marcas en la parte, y en la revisión a mano 63 de 78 reales (81 %)
en vez de 62 de 78.

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
Fuera del catálogo, si la forma escrita o el lema es una forma derivada que el EVP trae, manda su nivel: dos o más
arriba del ejercicio, marca salvo que el tema la enseñe, el lematizador la haya leído mal o sea un nombre propio (ni
base, ni parte, ni sufijo, ni par británico, ni préstamo); menos de dos, no marca. Dentro del catálogo, si la forma
escrita es una forma derivada que el EVP trae a menos de dos niveles, no marca: «smoking» en «smoke - smoking». No es la
lectura por forma de DOUBT-LECTURAS-NUEVAS (opción C, que José no quiso): aquélla sumaba marcas, 127 rescates con
falsos como «living-room»; ésta sólo deja o suelta.

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

## Después de aplicar
`generate` regenera el puerto; el método de `FileSystemEvpCatalog` y el uso en el analizador se escriben a mano (el
prototipo del 1/10 está probado sobre el curso). Cambian cuatro construcciones: `Main`, la prueba del proveedor, el
ayudante de la prueba del analizador y J001, que pasa a 13 · 27 · 44 · 20. Las pruebas de las tres reglas y del método
nuevo las declara @qa-tester. El parche de la teoría del tema sigue en espera hasta después de la ronda 2, con su
TECH_SPEC, en `en-espera/`.
