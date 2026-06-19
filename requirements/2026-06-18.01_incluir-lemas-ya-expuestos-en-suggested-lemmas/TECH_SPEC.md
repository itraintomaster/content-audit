---
patch: ARCH-INCEXP-CLI
requirement: 2026-06-18.01_incluir-lemas-ya-expuestos-en-suggested-lemmas
generated: 2026-06-18T16:15:00Z
---

# Tech Spec: Exponer includeExposed en el filtro del CLI

> Esta es la segunda iteracion aditiva de FEAT-INCEXP. La primera (carrier `SuggestedLemmaQueryCriteria` + contrato interno `LeveledLemmaInventory` con sus dos implementaciones en `refiner-domain`) ya fue aplicada y queda archivada en `.applied-patches/`. Este patch cierra el unico hueco de contrato que restaba: que la opcion pueda viajar desde el CLI.

## Agregar includeExposed a SuggestedLemmasFilter
El carrier `SuggestedLemmaQueryCriteria` (refiner-domain) ya transporta la opcion dentro del dominio, pero el flag no tenia por donde entrar desde afuera: el contrato publico `GetCommand.get(resource, name, SuggestedLemmasFilter)` —que ejercitan los journey tests J001/J002 en audit-cli— solo aceptaba limit, partOfSpeech y level. Se agrega el campo en paridad con el del carrier, con default semantico `false` para preservar el comportamiento de FEAT-CSLATDC (F-INCEXP-R001). El mapeo `SuggestedLemmasFilter.includeExposed` → `SuggestedLemmaQueryCriteria.includeExposed` dentro de GetCmd es codigo developer-owned y no se modela aqui. Aceptamos que sumar el campo regenera el constructor del record a 4-arg, rompiendo los `new SuggestedLemmasFilter(limit, pos, level)` existentes, que el developer migra junto con la adopcion del carrier.

```architecture
modules:
  - name: audit-cli
    _change: modify
    models:
      - name: SuggestedLemmasFilter
        _change: modify
        fields:
          - { name: includeExposed, type: boolean, _change: add }
```
