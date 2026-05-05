---
patch: ARCH-PIPRE-VIS
requirement: 2026-05-03.01_proposal-impact-preview
generated: 2026-05-03T00:00:00Z
---

# Tech Spec: FEAT-PIPRE — Drift de visibilidad del formatter

## Exponer ImpactPreviewFormatter como interfaz publica

`ImpactPreviewFormatter` no es un detalle interno del package `formatting`: es el seam que convierte los scores de dominio (escala 0..1) en strings porcentuales para la salida CLI. Sus consumidores reales son `Main` (composition root) y `GetCmd`, ambos en el package hermano `audit-cli.commands`. Con `visibility: internal` (package-private en Java) el contrato no se puede importar desde `commands`, lo que rompe el wireado. El package `formatting` se mantiene con `visibility: internal` a nivel modulo: la interfaz queda accesible solo dentro de `audit-cli`, no cross-module, que es el alcance correcto.

```architecture
modules:
  - name: "audit-cli"
    _change: "modify"
    packages:
      - name: "formatting"
        _change: "modify"
        interfaces:
          - name: "ImpactPreviewFormatter"
            _change: "modify"
            visibility: "public"
```

## Exponer DefaultImpactPreviewFormatter como clase publica

`DefaultImpactPreviewFormatter` es la unica implementacion del formatter y la instancia el composition root via `new DefaultImpactPreviewFormatter()`. Sin `visibility: public`, la clase es package-private y `Main` no puede instanciarla desde `audit-cli.commands`. Esto sigue el patron Public Port + Hidden Adapter del proyecto pero adaptado al limite intra-modulo: la implementacion se hace publica a nivel Java porque el composition root vive en un package hermano dentro del mismo modulo. El package `formatting` continua siendo `internal`, asi que nada de esto se filtra fuera de `audit-cli`.

```architecture
modules:
  - name: "audit-cli"
    _change: "modify"
    packages:
      - name: "formatting"
        _change: "modify"
        implementations:
          - name: "DefaultImpactPreviewFormatter"
            _change: "modify"
            visibility: "public"
```
