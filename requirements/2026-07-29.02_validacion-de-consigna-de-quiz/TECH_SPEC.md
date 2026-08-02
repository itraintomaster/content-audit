---
patch: FEAT-QINST
requirement: 2026-07-29.02_validacion-de-consigna-de-quiz
generated: 2026-08-01T00:00:00Z
---

# Tech Spec: cierre de cuatro huecos de contrato (patch correctivo)

> Este documento es la foto del **patch correctivo** posterior a la arquitectura
> original de FEAT-QINST/FEAT-EVCOST. La foto de aquel primer patch (752 lineas,
> 15 fences) sigue disponible en git:
> `git show 17449eca:requirements/2026-07-29.02_validacion-de-consigna-de-quiz/TECH_SPEC.md`,
> y el patch correspondiente en `.applied-patches/`.

El patch no agrega capacidades: cierra cuatro lugares donde el contrato permitia
que un dato se perdiera, se degradara o se ignorara **sin que nada lo dijera**.
Los cuatro son variantes del mismo defecto —el error silencioso— que es justamente
lo que estas dos features existen para evitar.

## Hacer que la sesion declare bajo que version resolvio

`QuizInstructionCoverageDiagnosis.evaluatorVersion` quedaba siempre en `null`: el
analizador recibe una `EvaluationSession` ya abierta, no el `Evaluator`, y solo la
factory tenia acceso a `evaluator.evaluatorVersion()`. Pasarla por constructor
habria duplicado la fuente de verdad, porque la sesion **ya usa** esa version para
armar cada `EvaluationKey` (F-EVCOST-R001). Se expone donde ya vive: la sesion
declara la version bajo la que resolvio, congelada al abrir, y el analizador la
republica en la cobertura sin cambiar su `requiresInject`.

```architecture
modules:
  - name: evaluation-ledger-domain
    _change: modify
    interfaces:
      - name: EvaluationSession
        _change: modify
        exposes:
          - signature: "evaluatorVersion(): String"
            _change: add
```

## Separar el presupuesto que la sesion honra de la politica que interpreta el consumidor

`EvaluationRunPolicy` llevaba tres campos y la sesion leia uno solo: `reevaluate`
y `reevaluationScope` entraban por `open(...)` y nunca se miraban. Un contrato que
recibe datos que ignora invita al error que ya ocurrio —pedir re-evaluacion y no
re-evaluar, en silencio—. La sesion pasa a abrirse con `EvaluationBudget`, que
contiene exactamente lo que honra; la re-evaluacion sigue siendo explicita y
per-sujeto via `resolveForced` (F-EVCOST-R008), que es la unica forma en que la
capacidad general puede expresarla sin conocer el vocabulario del consumidor.

```architecture
modules:
  - name: evaluation-ledger-domain
    _change: modify
    models:
      - name: EvaluationBudget
        _change: add
        type: record
        fields:
          - { name: maxNewEvaluations, type: int }
    interfaces:
      - name: EvaluationSessionFactory
        _change: modify
        exposes:
          - signature: "open(EvaluationRunPolicy policy,Evaluator evaluator): EvaluationSession"
            _change: delete
          - signature: "open(EvaluationBudget budget,Evaluator evaluator): EvaluationSession"
            _change: add
    packages:
      - name: evaluationsession
        _change: modify
        implementations:
          - name: DefaultEvaluationSession
            _change: modify
            requiresInject:
              - { name: policy, type: EvaluationRunPolicy, _change: delete }
              - { name: budget, type: EvaluationBudget, _change: add }
```

## Mudar EvaluationRunPolicy al dominio que si puede resolverla

El desajuste de fondo es que `reevaluationScope` es un id de knowledge y lo unico
que la sesion conoce de un sujeto es su `subjectRef`, que es el id del ejercicio:
la capacidad general **no puede** aplicar ese alcance ni aunque quisiera. El
alcance solo tiene sentido donde existe la jerarquia del curso, asi que la
politica de corrida se muda a `audit-domain`, junto al `QuizInstructionScopeMatcher`
que ya la interpreta. `evaluation-ledger-domain` queda sin ningun tipo que declare
campos que no lee.

```architecture
modules:
  - name: evaluation-ledger-domain
    _change: modify
    models:
      - name: EvaluationRunPolicy
        _change: delete
  - name: audit-domain
    _change: modify
    models:
      - name: EvaluationRunPolicy
        _change: add
        type: record
        fields:
          - { name: maxNewEvaluations, type: int }
          - { name: reevaluate, type: boolean }
          - { name: reevaluationScope, type: String }
```

## Extraer a un unico locator la cadena de resolucion del directorio del agente

El runner resolvia el directorio de definiciones con una cadena de tres pasos
(config, `project_root`, CWD) y el resolutor de version tenia su propia copia
CWD-relativa. Dos implementaciones de la misma pregunta es lo que permitio que
divergieran, y la divergencia hace que el digest se calcule sobre un directorio
distinto del que se ejecuta. Se extrae una unica implementacion de la cadena, con
las dos entradas explicitas que el runtime necesita: con `project_root` cuando el
consumidor lo aporta, y sin el cuando no.

```architecture
modules:
  - name: agent-runtime-infrastructure
    _change: modify
    interfaces:
      - name: AgentDefinitionLocator
        _change: add
        stereotype: port
        exposes:
          - signature: "locate(String agentName): AgentDefinitionLocation"
            _change: add
          - signature: "locateUnderProjectRoot(String projectRoot,String agentName): AgentDefinitionLocation"
            _change: add
      - name: AgentDefinitionLocatorFactory
        _change: add
        stereotype: factory
        exposes:
          - signature: "create(AgentGraphRunnerConfig config): AgentDefinitionLocator"
            _change: add
```

## Hacer que "no encontre la definicion" sea un valor y no una ausencia

La degradacion silenciosa era posible porque el resultado de buscar el directorio
era un booleano descartable (`Files.isDirectory`) y el camino "no existe" no
obligaba a nadie a hacer nada. El resultado pasa a ser un valor con dos formas
distintas, de manera que ningun consumidor puede seguir de largo por accidente, y
el caso negativo lleva la ruta donde se busco para que el diagnostico diga **donde
miro**, no solo que fallo.

```architecture
modules:
  - name: agent-runtime-infrastructure
    _change: modify
    interfaces:
      - name: AgentDefinitionLocation
        _change: add
        exposes: []
    models:
      - name: AgentDefinitionFound
        _change: add
        type: record
        implements: [AgentDefinitionLocation]
        fields:
          - { name: directory, type: Path }
      - name: AgentDefinitionUnresolved
        _change: add
        type: record
        implements: [AgentDefinitionLocation]
        fields:
          - { name: agentName, type: String }
          - { name: searchedIn, type: Path }
```

## Hacer que el runner use el locator, para que digest y ejecucion no puedan divergir

Que el resolutor de version use el locator no alcanza si el runner sigue teniendo
su copia: quedarian dos cadenas que pueden separarse otra vez. Con el runner
delegando, "el directorio del que sale el digest" y "el directorio que se carga
para correr el grafo" son el mismo por construccion, no por disciplina. El locator
es package-private junto al runner y solo su factory es publica, asi que la
superficie del modulo crece en un seam, no en un motor.

```architecture
modules:
  - name: agent-runtime-infrastructure
    _change: modify
    packages:
      - name: graphexecution
        _change: modify
        implementations:
          - name: DefaultAgentDefinitionLocatorFactory
            _change: add
            visibility: public
            implements: [AgentDefinitionLocatorFactory]
          - name: DefaultAgentDefinitionLocator
            _change: add
            implements: [AgentDefinitionLocator]
            requiresInject:
              - { name: config, type: AgentGraphRunnerConfig }
          - name: DefaultAgentGraphRunner
            _change: modify
            requiresInject:
              - { name: agentDefinitionLocator, type: AgentDefinitionLocator, _change: add }
```

## Dar al resolutor de version la definicion en vez de una ruta

El resolutor deja de conocer rutas: pide la definicion al mismo locator con el que
se construye el runner. Con eso, la mitad de F-QINST-R010 que exige que retocar el
prompt invalide los veredictos guardados vuelve a ser cierta fuera de la raiz del
repo, y ademas pasa a ser probable con un directorio de prueba, que es lo que hoy
ningun test puede hacer. Cuando el locator responde `AgentDefinitionUnresolved`, la
version se emite con un prefijo reservado en lugar de un digest con la misma forma
que uno legitimo: una version que ignora la definicion del agente tiene que
decirlo, y ademas nunca coincide con ninguna registrada.

```architecture
modules:
  - name: quiz-instruction-infrastructure
    _change: modify
    packages:
      - name: instructionjudge
        _change: modify
        implementations:
          - name: DefaultQuizInstructionJudgeVersionResolver
            _change: modify
            requiresInject:
              - { name: agentDefinitionLocator, type: AgentDefinitionLocator, _change: add }
```

## Entregar el locator por el mismo seam por el que ya se entrega el runner

El locator y el runner se construyen los dos desde `AgentGraphRunnerConfig` en la
composition root, que es el unico lugar autorizado a conocer ambos concretos. Se
entrega junto al runner en la misma llamada, en vez de inyectarlo en la factory:
mantiene la factory sin estado y deja explicito en la firma que un juez necesita
las dos capacidades del runtime —correr el grafo y saber donde vive su definicion—
y no una sola.

```architecture
modules:
  - name: quiz-instruction-infrastructure
    _change: modify
    interfaces:
      - name: QuizInstructionJudgeFactory
        _change: modify
        exposes:
          - signature: "create(QuizInstructionJudgeConfig config,AgentGraphRunner agentGraphRunner): Evaluator"
            _change: delete
          - signature: "create(QuizInstructionJudgeConfig config,AgentGraphRunner agentGraphRunner,AgentDefinitionLocator agentDefinitionLocator): Evaluator"
            _change: add
```

## Hacer que la factory de analizadores hable el nombre que el usuario ve

El runner decidia incluir, excluir y presupuestar comparando contra
`evaluatorId()`, que es el nombre del agente (`quiz-instruction-validator`),
mientras que el informe muestra el nombre del analizador (`quiz-instruction`). El
usuario leia un nombre y escribia ese nombre, y no pasaba nada: sin error y sin
aviso. Son dos identidades legitimamente distintas —el id del evaluador forma
parte de la clave del registro (F-EVCOST-R001) y cambiarlo invalidaria todos los
veredictos guardados— pero solo una de las dos es vocabulario de auditoria, y es
la que este seam tiene que exponer. Se expone en la factory y no en el analizador
porque F-QINST-R011 exige que un analizador excluido **no se construya**: el
nombre tiene que conocerse sin instanciar nada.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: EvaluationAnalyzerFactory
        _change: modify
        stereotype: factory
        exposes:
          - signature: "analyzerName(): String"
            _change: add
```

## Retirar evaluatorId de la factory en vez de dejarlo conviviendo con el nombre

Despues del cambio, `EvaluationAnalyzerFactory.evaluatorId()` se queda sin ningun
consumidor: su unico llamador era el runner, exactamente en las tres decisiones
equivocadas. Dejarlo por si algun informe futuro quiere decir que evaluador
respalda a un analizador conservaria justamente la trampa que hizo posible el
error —dos metodos que devuelven `String` y parecen intercambiables, en la
interfaz que el runner mira— para un caso que hoy nadie pidio. `Evaluator.evaluatorId()`
queda intacto: es la identidad del registro y los veredictos guardados siguen
siendo validos. Del lado de la implementacion, la factory del juez vive en el
mismo paquete que su analizador, asi que puede devolver la misma constante que
`getName()` publica: la coincidencia entre lo que se filtra y lo que se muestra
queda por construccion, no por disciplina.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: EvaluationAnalyzerFactory
        _change: modify
        exposes:
          - signature: "evaluatorId(): String"
            _change: delete
```

## Indexar las politicas de corrida por nombre de analizador

El mapa de politicas era la segunda victima del mismo desajuste, y la mas cara: el
CLI armaba la entrada con la clave `quiz-instruction` y el runner la buscaba por
`quiz-instruction-validator`, asi que el `get` nunca acertaba y la corrida caia al
presupuesto por defecto. Es decir, `--instruction-budget 10` —el flag cuyo
proposito entero es que el costo no aparezca por sorpresa— corria con 500 sin
avisar, y `--reevaluate-instructions` no re-evaluaba nada. Se renombra el campo
ademas de cambiar la clave: con `includedAnalyzers`, `excludedAnalyzers` y
`analyzerPolicies` los tres en el mismo vocabulario, un `Map<String,...>` deja de
ser una cadena anonima y volver a mezclar identidades se vuelve visible al leer el
record.

```architecture
modules:
  - name: audit-application
    _change: modify
    models:
      - name: AuditRunRequest
        _change: modify
        type: record
        fields:
          - { name: evaluationPolicies, _change: delete }
          - { name: analyzerPolicies, type: "Map<String,EvaluationRunPolicy>", _change: add }
```
