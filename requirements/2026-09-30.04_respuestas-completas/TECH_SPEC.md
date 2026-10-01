---
patch: FEAT-RESPC-NOMBRES-1194
requirement: 2026-09-30.04_respuestas-completas
generated: 2026-10-01T04:30:00Z
---

# Tech Spec: Tres nombres de prueba, después de que el #1194 salió a producción (FEAT-RESPC)

Tercer parche de FEAT-RESPC, sólo de qa-tester. Los dos primeros ya están aplicados y sus TECH_SPEC quedan en la
historia de git. Validado contra el `sentinel.yaml` de `ronda1/integracion` con el segundo aplicado. No cambia
estructura.

## Por qué
El #1194 (9915461ac), el corrector de las cuatro reglas, está en producción desde el 1/10/2026. Su copia pasó a ser
`formas-equivalentes.json` y la del #1191 quedó al lado como `formas-equivalentes-corrector-1191.json`. Cada versión
prueba sus casos de oro: el #1194, los 192 de `casos-de-oro/`; el #1191, los 153 de `casos-de-oro-corrector-1191/`.
Tres nombres declarados dejaron de decir lo que hace el código:

| Prueba | Decía | Dice |
|---|---|---|
| Casos de oro del #1191 | con la tabla del corrector nuevo | con la copia del #1191 |
| La misma respuesta en cualquier orden | dos respuestas con el corrector en producción | dos con el #1191 |
| Las reglas que lee la tabla | ninguna en la de producción | las cuatro en la del #1194, ninguna en la del #1191 |

Cada una se borra con el nombre viejo y se agrega con el nuevo, con la misma regla. El cuerpo ya prueba lo que dice el
nombre nuevo.

```architecture
modules:
- name: audit-domain
  _change: modify
  packages:
  - name: correctormatch
    _change: modify
    implementations:
    - name: DefaultCorrectorMatcher
      _change: modify
      handwrittenTests:
      - name: "should decide every golden case of the backend corrector as its «acepta» column says with the copy of the new corrector's table, reading each gap with the sentence around it, as «is» for «It ___ (be) hot inside.» answered «'s»"
        _change: delete
      - name: "should decide every golden case of the corrector #1191 as its «acepta» column says with the copy of its table, reading each gap with the sentence around it, as «is» for «It ___ (be) hot inside.» answered «'s»"
        traceability: { feature: FEAT-RESPC, rule: F-RESPC-R001 }
      - name: "should take two texts as the same answer when the corrector matches them in either order, so «He's forgotten to pay.» and «He has forgotten to pay.» are one answer with the four rules and two with the corrector in production"
        _change: delete
      - name: "should take two texts as the same answer when the corrector matches them in either order, so «He's forgotten to pay.» and «He has forgotten to pay.» are one answer with the four rules of #1194 and two with #1191"
        traceability: { feature: FEAT-RESPC, rule: F-RESPC-R001 }
- name: vocabulary-infrastructure
  _change: modify
  packages:
  - name: correctortable
    _change: modify
    implementations:
    - name: FileSystemFormEquivalenceTableSource
      _change: modify
      handwrittenTests:
      - name: "should read from «reglas» the rules that are code of the version of the corrector, the four in the copy of the new corrector and none in the one in production, and stop at a rule it does not know"
        _change: delete
      - name: "should read from «reglas» the rules that are code of the version of the corrector, the four in the copy of #1194, in production, and none in the copy of #1191, and stop at a rule it does not know"
        traceability: { feature: FEAT-RESPC, rule: F-RESPC-R001 }
```

## Después de generate
generate no agrega los stubs de los tres nombres nuevos, porque sus etiquetas ya están en esos archivos (bug
2026-07-22-01). Conserva los métodos viejos, que tienen cuerpo, y avisa. Los tres se renombran a mano, método y
`@DisplayName`; el cuerpo no cambia.
