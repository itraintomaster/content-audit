---
patch: FEAT-RESPC-NOMBRES
requirement: 2026-09-30.04_respuestas-completas
generated: 2026-10-01T03:30:00Z
---

# Tech Spec: Dos nombres de prueba que dicen lo que hace el código (FEAT-RESPC)

Segundo parche de FEAT-RESPC, sólo de qa-tester. El primero ya está aplicado y su TECH_SPEC queda en la historia de
git. Diseñado contra el `sentinel.yaml` de 859e96af (`ronda1/integracion`). No cambia estructura: ni modelos, ni
interfaces, ni implementaciones, ni el journey.

## Por qué
Desde 3a3238ed, una forma de una clase que el corrector en producción ya iguala y que igual rechaza es un límite del
corrector. El hallazgo sólo ordena, nombra la regla que le falta al corrector y nunca propone «|» (brief 101, José 30/9;
el coordinador, 1/10), y la ficha sumó *sólo ordena* a sus resoluciones. El código y el cuerpo de las pruebas ya lo
hacen, pero dos nombres declarados seguían diciendo lo de antes:

| Prueba | Decía | Dice |
|---|---|---|
| «has trained», R005 | se resuelve agregándola después de «\|» | sólo ordena y nombra la regla que le falta al corrector |
| La ficha, R006 | resuelve por regla o en la mesa | por regla, en la mesa o sólo ordenando |

R006 del REQUIREMENT ya dice las tres resoluciones; R005 no cambia.

## Cómo se renombra
Una prueba declarada se identifica por su nombre. Cada una se borra con el nombre viejo y se agrega con el nuevo, con la
misma regla.

```architecture
modules:
- name: audit-domain
  _change: modify
  packages:
  - name: answercompleteness
    _change: modify
    implementations:
    - name: AnswerCompletenessAnalyzer
      _change: modify
      handwrittenTests:
      - name: "should resolve by adding it after «|» a missing form the new corrector still rejects, as «has trained» if «He ___ (train) hard today.» only accepted «'s trained», a limit of the corrector"
        _change: delete
      - name: "should only rank a missing form of a class the corrector in production equates and still rejects, naming the rule the corrector lacks instead of adding it after «|», as «has trained» if «He ___ (train) hard today.» only accepted «'s trained»"
        traceability: { feature: FEAT-RESPC, rule: F-RESPC-R005 }
    - name: AnswerCompletenessAnalyzerProvider
      _change: modify
      handwrittenTests:
      - name: "should describe answer-completeness in the catalog as an errors analyzer that evaluates exercises, resolves by rule or at the panel and costs instant, asking «¿El corrector acepta todas las formas de la respuesta, y ninguna consigna pide una?»"
        _change: delete
      - name: "should describe answer-completeness in the catalog as an errors analyzer that evaluates exercises, resolves by rule, at the panel or by ranking only, and costs instant, asking «¿El corrector acepta todas las formas de la respuesta, y ninguna consigna pide una?»"
        traceability: { feature: FEAT-RESPC, rule: F-RESPC-R006 }
```

## Después de generate
generate no agrega los stubs de los nombres nuevos: las etiquetas FEAT-RESPC, F-RESPC-R005 y F-RESPC-R006 ya están en
`AnswerCompletenessAnalyzerTest` y `AnswerCompletenessAnalyzerProviderTest` (bug 2026-07-22-01 de Sentinel, abierto).
Tampoco borra los métodos viejos, porque tienen cuerpo. Los dos se renombran a mano, método y `@DisplayName`; el cuerpo
no cambia.
