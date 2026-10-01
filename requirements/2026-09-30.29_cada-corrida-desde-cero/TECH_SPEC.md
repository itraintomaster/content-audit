---
patch: FEAT-CERO
requirement: 2026-09-30.29_cada-corrida-desde-cero
generated: 2026-09-30T23:15:00Z
---

# Tech Spec: Cada corrida del motor empieza de cero (FEAT-CERO)

Paquete F, ronda 2, contra el `sentinel.yaml` con 0.2 y 0.3 aplicados. La pregunta del orquestador era qué le queda
propio a FEAT-CERO si el diseño de FEAT-HALL ya construye los analizadores en cada corrida. Respuesta, verificada en el
modelo y en el código de este worktree: ningún cambio de estructura; le quedan el registro de la feature, la prueba que
la fija y una garantía que el developer tiene que cumplir al implementar los proveedores de HALL.

## Dejar la estructura como la dejó FEAT-HALL
El arrastre medido —72,90 en vez de 73,92, y los 55 topics de la primera foto sin bandas— viene de cuatro analizadores
que guardan en campos lo que cuentan (`CocaBucketsAnalyzer`, `LemmaRecurrenceAnalyzer`, `LemmaByLevelAbsenceAnalyzer`,
`LemmaCountAnalyzer`) y que hoy se construyen una sola vez en `Main`. En el modelo aplicado, `IAuditEngine` ya no inyecta
analizadores: pide al catálogo un analizador nuevo por corrida con `AnalyzerProvider.create`, y ninguna implementación
del modelo inyecta un `ContentAnalyzer`. Sus colaboradores no guardan estado entre corridas: la agregación, el
recolector, el calculador de números, el puntuador léxico, el mapeador —que vacía sus mapas en cada `map`— y el libro de
evaluaciones, que lee su archivo en cada consulta y así ve lo que la corrida anterior registró. Agregar una estructura
acá sería duplicar lo que HALL ya garantiza.

```architecture
features:
  - id: FEAT-CERO
    code: F-CERO
modules:
  - name: audit-domain
    _change: modify
```

## Fijar la garantía con la prueba y con una regla para el developer
Lo propio es la prueba, que vale aunque la implementación de HALL se demore: el mismo curso corrido dos veces en el
mismo proceso da, en los 11.760 nodos, lo mismo que una invocación nueva —73,92 en el curso, 96,54 · 94,16 · 79,45 ·
63,51 en los niveles, 57,93 de COCA en B1 y 8,00 de recurrencia— y el primer informe conserva sus diagnósticos de topic
(A1 · Present Simple, 2.366 tokens en cuatro bandas). Se prueba en el motor y en la vista consolidada con pendientes,
donde FEAT-PROYU mide las dos fotos. La regla para el developer: un proveedor no reusa entre dos `create` nada que el
analizador modifique; si hace falta un caché, que sea de datos que no dependen del curso, como el catálogo EVP o los
tokens por oración. La feature se registra para que `feature sync --feature FEAT-CERO` anote J001; su ubicación y las
pruebas son del qa-tester.

```architecture
modules:
  - name: audit-domain
    _change: modify
```
