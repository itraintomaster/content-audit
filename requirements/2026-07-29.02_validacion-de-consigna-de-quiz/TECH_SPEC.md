---
patch: FEAT-QINST
requirement: 2026-07-29.02_validacion-de-consigna-de-quiz
generated: 2026-08-03T00:00:00Z
---

# Tech Spec: La vista detallada honra la peticion de corrida

> Historico: los tech specs de los patches anteriores de FEAT-QINST estan en git
> (`git log -- requirements/2026-07-29.02_validacion-de-consigna-de-quiz/TECH_SPEC.md`)
> y sus patches en `.applied-patches/`. Este archivo se reemplaza en cada patch porque
> `tech-spec write` valida TODAS las fences contra el patch vigente.

## La vista detallada recibe la peticion de corrida, no un nombre suelto

Hoy `runDetailedAudit(Path,String)` solo filtra los `ContentAnalyzer` clasicos: como el
analisis de consigna se construye por corrida a traves de `EvaluationAnalyzerFactory`, el
filtro queda vacio y el metodo cae a la auditoria completa **sin ningun analizador por
evaluacion** — verificado con el binario, `analyze --analyzers quiz-instruction --detailed`
imprime solo el encabezado, sin la cobertura de F-QINST-R005 ni la evidencia de
F-QINST-R003. El arreglo local (buscar la factory dentro del metodo) no sirve: sin la
peticion de corrida el analizador se construiria con politica nula, o sea con el
presupuesto por defecto de 500 consultas, y `--detailed --instruction-budget 2` pagaria
500 en silencio — exactamente el defecto que F-QINST-R015 castigo una vez.

La sobrecarga nueva sigue el mismo criterio que ya se aplico a `runAudit(Path,Set<String>)`
→ `runAudit(Path,AuditRunRequest)`: el parametro ad-hoc se reemplaza por la peticion
completa, que ya trae inclusion (F-QINST-R011), exclusion y politica por analizador
(F-QINST-R006 y F-QINST-R012). El nombre del analisis a detallar viaja en
`includedAnalyzers`, coherente con lo que ese campo ya declara ("subconjunto a correr").

```architecture
modules:
  - name: audit-application
    _change: modify
    interfaces:
      - name: AuditRunner
        _change: modify
        exposes:
          - signature: "runDetailedAudit(Path coursePath,AuditRunRequest request): AuditNode"
            _change: add
```

## Por que el nombre NO viaja ademas como parametro suelto

La alternativa evaluada era `runDetailedAudit(Path,String,AuditRunRequest)`. Se descarto
porque reintroduce la forma exacta del Hueco 4: dos identificaciones del mismo analisis
—el `String` suelto y `request.includedAnalyzers`— que el runner puede mirar por separado
y que nada obliga a coincidir. Ese fue el mecanismo de los cinco defectos silenciosos ya
pagados en esta feature, y F-QINST-R015 existe precisamente para prohibirlo: un solo
nombre, en un solo lugar. Costo aceptado: el llamador debe envolver el nombre en un
`AuditRunRequest`, cosa que `AnalyzeCmd.toAuditRunRequest(...)` ya hace hoy para la
corrida normal, asi que en el CLI no hay contrato nuevo.

```architecture
modules:
  - name: audit-application
    _change: modify
    interfaces:
      - name: AuditRunner
        _change: modify
        exposes:
          - signature: "runDetailedAudit(Path coursePath,AuditRunRequest request): AuditNode"
            _change: add
```

## Semantica esperada de la implementacion

`DefaultAuditRunner` resuelve el unico nombre de `includedAnalyzers` contra las dos
familias. Si corresponde a un `ContentAnalyzer` clasico, el comportamiento es identico al
de hoy (motor filtrado con ese solo analizador), de modo que las vistas detalladas de
`lemma-absence` y `coca-buckets-distribution` no cambian. Si corresponde a una
`EvaluationAnalyzerFactory`, el arbol se construye sin analizadores clasicos y se le
superpone unicamente ese analizador, construido con `factory.create(policy)` donde la
politica sale de `request.getAnalyzerPolicies()` — el mismo camino que ya recorre
`runAudit(Path,AuditRunRequest)`, incluida la guarda de F-QINST-R011 (un analisis excluido
no se construye nunca, ni siquiera si se lo pidio detallar) y el aislamiento de fallas de
F-QINST-R007. Con `request` nulo o sin inclusiones, se conserva el desenlace actual de la
sobrecarga vieja. La sobrecarga `runDetailedAudit(Path,String)` se mantiene viva por el
mismo motivo por el que se mantuvo `runAudit(Path,Set<String>)`: hay llamadores legados
(`AnalyzeCommand.analyze` de siete parametros) que no tienen peticion de corrida que pasar.

```architecture
modules:
  - name: audit-application
    _change: modify
    interfaces:
      - name: AuditRunner
        _change: modify
        exposes:
          - signature: "runDetailedAudit(Path coursePath,AuditRunRequest request): AuditNode"
            _change: add
```
