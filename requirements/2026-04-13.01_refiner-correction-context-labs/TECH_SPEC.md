---
patch: ARCH-001
requirement: 2026-04-13.01_refiner-correction-context-labs
generated: 2026-07-01T00:00:00Z
---

# Tech Spec: FEAT-RCLA — Palabras de contenido escasas en el contexto de correccion

## Agregar el modelo carrier `ScarceContentWord`
F-RCLA-R003b define un registro nuevo por palabra escasa con exactamente cuatro campos (`lemma`, `pos`, `count`, `threshold`). Es un carrier puro que cruza el borde de modulo hacia JSON, igual que `MisplacedLemmaContext` y `SuggestedLemma` ya lo hacen para el mismo contexto; por eso vive en `refiner-domain` como record publico y no lleva logica. `count` y `threshold` son `Integer` (no primitivos) para reusar la semantica de `SuggestedLemma.lemmaCount`/`lemmaCountThreshold` de F-SLEM, aunque R003b garantiza que ambos siempre estan presentes (la palabra solo entra a la lista si `count < threshold`).

```architecture
modules:
  - name: "refiner-domain"
    _change: "modify"
    models:
      - name: "ScarceContentWord"
        _change: "add"
        type: "record"
        fields:
          - { name: "lemma", type: "String" }
          - { name: "pos", type: "String" }
          - { name: "count", type: "Integer" }
          - { name: "threshold", type: "Integer" }
```

## Agregar el campo `scarceContentWords` al contexto de correccion
El contexto de LEMMA_ABSENCE ya reune señales de multiples fuentes (misplaced, suggested, longitud). R003b/R003c agregan una lista mas — las palabras que el LLM NO debe quitar — siguiendo el mismo patron aditivo `List<T>` que `misplacedLemmas`/`suggestedLemmas`, sin tocar campos existentes ni la interfaz `CorrectionContext`. La semantica de degradacion elegante de R003c (lista vacia, nunca null, cuando falta lemma-count o no hay palabras escasas) espeja R004c y se documenta en el campo para el desarrollador; a nivel arquitectura solo se declara el campo.

```architecture
modules:
  - name: "refiner-domain"
    _change: "modify"
    models:
      - name: "LemmaAbsenceCorrectionContext"
        _change: "modify"
        fields:
          - name: "scarceContentWords"
            type: "List<ScarceContentWord>"
            _change: "add"
```
