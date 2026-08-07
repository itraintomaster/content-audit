---
patch: ARCH-QINST-R018-R019
requirement: 2026-07-29.02_validacion-de-consigna-de-quiz
generated: 2026-08-06T18:15:00Z
---

# Tech Spec: Re-evaluar un conjunto de ejercicios declarado en el pedido (F-QINST-R018 / R019 / J004)

## Llevar el conjunto declarado dentro de la politica de corrida
La politica es el unico canal que llega hasta el analizador de consigna: `EvaluationAnalyzerFactory.create(policy)` es lo unico que el runner le pasa por corrida, asi que el conjunto tiene que viajar ahi o no viaja. El campo es nullable a proposito y con dos valores distintos que no se pueden confundir: `null` es "no se declaro conjunto" y vacio es "se declaro y esta vacio", que es un pedido rechazado. Leer vacio como "todo el curso" es exactamente la factura sorpresa que F-QINST-R006 existe para impedir, y leerlo como "no re-evalues nada" produce una auditoria normal indistinguible de la que el usuario cree haber pedido. Es `Set` y no `List` porque un ejercicio declarado dos veces cuenta una sola (F-QINST-R019), y es excluyente con `reevaluate`: un conjunto declarado **es** el pedido de re-evaluacion, y eso es lo que permite distinguir "conjunto solo" de "conjunto mas alcance total", que de otro modo tendrian la misma forma en la politica y el rechazo del pedido ambiguo seria inexpresable.

```architecture
modules:
  - name: audit-domain
    _change: modify
    models:
      - name: EvaluationRunPolicy
        _change: modify
        type: record
        fields:
          - name: reevaluationSubjectIds
            type: Set<String>
            _change: modify
```

## Darle a la sesion las dos formas de resolver que faltaban
La sesion tenia dos: reutilizar-o-consultar (`resolve`) y consultar-o-nada (`resolveForced`). R018 necesita las otras dos casillas de esa matriz y ninguna se puede componer desde afuera, porque la cobertura se contabiliza dentro de la sesion y dos llamadas por el mismo ejercicio inflarian `reached` y romperian las sumas de F-QINST-R014. `resolveWithoutConsulting` reutiliza lo registrado y nunca consulta: es lo que aplica a todo ejercicio ajeno al conjunto. `resolvePreferringNew` vuelve a juzgar pero **sin destruir**: si el tope ya se gasto devuelve el veredicto registrado en vez de dejar el ejercicio pendiente, que es literalmente el desenlace que exige la invariante 2 de R018. No se toco `resolveForced`: FEAT-QICOR se apoya en que una revalidacion nunca devuelva el veredicto viejo (`LedgerBackedQuizInstructionComplianceChecker.revalidate`), y darle un fallback a reuso lo haria validar en silencio contra la marca que R009 de esa feature existe para detectar.

```architecture
modules:
  - name: evaluation-ledger-domain
    _change: modify
    interfaces:
      - name: EvaluationSession
        _change: modify
        stereotype: port
        sealed: false
        exposes:
          - signature: "resolvePreferringNew(EvaluationSubject subject): EvaluationResolution"
            _change: add
          - signature: "resolveWithoutConsulting(EvaluationSubject subject): EvaluationResolution"
            _change: add
```

## Nombrar la decision de alcance en un tipo, y no en un if
Hoy el alcance se decide con un booleano armado en el analizador (`policy.isReevaluate() && scopeMatcher.matches(...)`). Con dos formas de acotar y tres desenlaces posibles por ejercicio, ese booleano deja de alcanzar y la alternativa —una cadena de ifs dentro de `onQuiz`— esconderia el corazon de la regla en el metodo mas largo del analizador. El enum tiene un valor por **motivo** y cada motivo mapea uno a uno a un metodo de la sesion, asi que la decision se lee sin seguir ninguna condicion. `OUTSIDE_DECLARED_SET` es la pieza que hace innecesario cualquier mecanismo de prioridad de tope: si fuera del conjunto no hay consultas nuevas, el tope solo lo puede gastar el conjunto, y la invariante 2 de R018 sale por construccion en vez de por un orden de recorrido que el arbol no garantiza.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: quizinstructionengine
        _change: modify
        visibility: public
        models:
          - name: QuizInstructionReevaluationDecision
            _change: add
            type: enum
            visibility: internal
            fields:
              - name: ORDINARY
              - name: AREA_REEVALUATION
              - name: DECLARED_SET_REEVALUATION
              - name: OUTSIDE_DECLARED_SET
```

## Sumarle la decision al matcher existente en vez de reemplazarlo
`decide` entra como segundo metodo del `QuizInstructionScopeMatcher` que ya existe, y no como interfaz nueva ni como renombre. La razon es doble. Primero, `matches` sigue siendo la rama de area de la nueva decision —`decide` la invoca—, asi que no hay dos criterios que puedan discrepar: hay uno solo con dos puertas de entrada. Segundo, el costo del camino alternativo es real y ya se pago una vez en este repositorio: reemplazar el colaborador cambiaria la firma del constructor de `QuizInstructionAnalyzer`, que tiene 34 sitios de construccion en tests escritos a mano que el smart-merge no reescribe, y borraria los tests de F-QINST-R012 que hoy cuelgan del matcher. Con este corte el analizador conserva su constructor intacto y el cambio queda en el cuerpo de `onQuiz`.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: quizinstructionengine
        _change: modify
        visibility: public
        interfaces:
          - name: QuizInstructionScopeMatcher
            _change: modify
            sealed: false
            visibility: internal
            exposes:
              - signature: "decide(AuditNode quizNode,EvaluationRunPolicy policy): QuizInstructionReevaluationDecision"
                _change: add
```

## Rechazar el pedido inintepretable al construir el analizador, no al recorrer el arbol
El lugar donde se rechaza no es una preferencia de estilo: `DefaultAuditRunner` envuelve cada llamada por nodo y el `onCourseComplete` en un `catch (RuntimeException)` para cumplir F-QINST-R007, asi que una excepcion lanzada desde `onQuiz` se la traga en silencio y el sintoma visible seria "cero evaluaciones" —el error caro y callado que esta familia de reglas existe para impedir—. `factory.create(policy)` es la unica llamada del recorrido que queda **fuera** de esa red, corre una sola vez por corrida y sucede antes de abrir la sesion: el pedido se rechaza sin que el juez reciba una sola consulta. Las dos excepciones heredan de `IllegalArgumentException` porque el comando ya distingue esa rama y la imprime como "Error: ..." en vez de "Error running audit: ...", que seria mentir: no llego a correr nada.

```architecture
modules:
  - name: audit-domain
    _change: modify
    models:
      - name: EmptyReevaluationSetException
        _change: add
        type: exception
        extends: IllegalArgumentException
        fields:
          - name: analyzerName
            type: String
      - name: AmbiguousReevaluationScopeException
        _change: add
        type: exception
        extends: IllegalArgumentException
        fields:
          - name: analyzerName
            type: String
          - name: declaredScope
            type: String
```

## Declarar los identificadores no correspondidos al lado de la cobertura, no dentro
R018 pide informar los identificadores que no correspondieron a ningun ejercicio y, a la vez, que esos identificadores no entren en ningun recuento de cobertura. Las dos mitades se satisfacen con la misma decision: la lista viaja en el diagnostico de cobertura pero **fuera** del `EvaluationCoverage`, que sigue contando ejercicios del curso y cuyas dos sumas de F-QINST-R005 siguen cerrando sin tocarse. El acumulador es el propio analizador, que es el unico que ve todos los ejercicios alcanzados y ya cumple ese papel para el recuento por version del juez; extraer un colaborador para tachar ids de un conjunto seria versatilidad sin demanda. Cada identificador figura una sola vez porque el conjunto llega deduplicado desde su declaracion (R019).

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: quizinstruction
        _change: modify
        visibility: public
        models:
          - name: QuizInstructionCoverageDiagnosis
            _change: modify
            type: record
            fields:
              - name: unmatchedReevaluationIds
                type: List<String>
                _change: modify
```

## Resolver las dos formas de declarar el conjunto en el adaptador, y entregar una sola al dominio
Son 966 identificadores en el caso que motivo la regla: unos 24.000 caracteres que ningun pedido transporta comodo, y por eso F-QINST-R019 admite tambien apuntar a un origen externo. El puerto recibe **las dos declaraciones** —por eso la firma tiene dos parametros y no uno— y devuelve un unico conjunto ya normalizado: separadores de mas, espacios y entradas en blanco descartados, repetidos contados una vez. Aguas abajo las dos vias son literalmente indistinguibles, que es la invariante 1 de R019. Leer el origen es I/O y se queda de este lado; el dominio no sabe ni puede saber como se declaro el conjunto, y esa ignorancia es lo que garantiza la indistinguibilidad en vez de tener que testearla en cada consumidor.

```architecture
modules:
  - name: audit-cli
    _change: modify
    packages:
      - name: bootstrap
        _change: modify
        visibility: internal
        interfaces:
          - name: ReevaluationQuizSetResolver
            _change: modify
            stereotype: port
            sealed: false
            exposes:
              - signature: "resolve(String optionValue): Set<String>"
                _change: delete
              - signature: "resolve(String enumeratedIds,String origin): Set<String>"
                _change: add
        implementations:
          - name: DefaultReevaluationQuizSetResolver
            _change: modify
            visibility: public
            implements:
              - ReevaluationQuizSetResolver
```

## Separar la causa "no se pudo leer el origen" de la causa "el conjunto esta vacio"
Los dos rechazos frenan la corrida sin gastar, asi que el desenlace se parece; lo que no se parece es donde esta el arreglo, y presentarlos igual manda al operador a revisar el lugar equivocado. Por eso son dos tipos y viven en capas distintas a proposito: "no se pudo leer el origen" lo levanta el adaptador —es el unico que sabe que habia un origen y como se llamaba— y "conjunto vacio" lo levanta el dominio, porque un origen legible del que no sale ningun identificador **si** es una seleccion conocida que dice "nada" (invariante 3 de R019). Un origen ilegible nunca degrada a conjunto vacio ni a "no se declaro nada": el conjunto no se llego a conocer. La tercera excepcion cubre el borde de declarar las dos formas a la vez, que union y "una gana" leerian distinto y con ordenes de magnitud de gasto de diferencia.

```architecture
modules:
  - name: audit-cli
    _change: modify
    packages:
      - name: bootstrap
        _change: modify
        visibility: internal
        models:
          - name: UnreadableQuizSetOriginException
            _change: add
            type: exception
            visibility: public
            extends: IllegalArgumentException
            fields:
              - name: origin
                type: String
              - name: detail
                type: String
          - name: AmbiguousQuizSetDeclarationException
            _change: add
            type: exception
            visibility: public
            extends: IllegalArgumentException
            fields:
              - name: enumeratedIds
                type: String
              - name: origin
                type: String
```

## Retirar el tipo que documentaba la convencion muerta
`UnreadableQuizSetFileException` entro con la version anterior de este diseño, cuando el origen se declaraba con un prefijo `@` dentro de la opcion enumerada. Al reemplazar esa convencion por dos opciones separadas, el tipo quedo sin ningun lanzador —lo sustituye `UnreadableQuizSetOriginException`, que habla de "origen" y no de "file", como R019 que deliberadamente no fija la clase del origen— pero **no desaparece solo**: ya estaba aplicado en `sentinel.yaml`, con una descripcion de campo que explicaba el prefijo `@`. Un tipo muerto que documenta una convencion retirada es peor que ninguna documentacion, porque el proximo lector no tiene como saber que no rige. Se borra explicitamente.

```architecture
modules:
  - name: audit-cli
    _change: modify
    packages:
      - name: bootstrap
        _change: modify
        visibility: internal
        models:
          - name: UnreadableQuizSetFileException
            _change: delete
```

## Darle a cada forma su propia opcion, en vez de un valor con prefijo magico
Las dos formas se declaran en dos opciones separadas y mutuamente excluyentes, siguiendo exactamente el precedente que R019 cita: `--correction-context` y `--correction-context-file` de F-REVCTX-R001. La alternativa —una sola opcion cuyo valor puede ser una lista o un `@origen`— se descarto por dos razones que se refuerzan: el valor mixto `q1,@lista.txt` haria que un origen declarado se colara como un identificador mas y la corrida siguiera adelante, que es la condicion prohibida textual de R019; y `@` ya tiene dueño en esta CLI, porque picocli expande argumentos que empiezan con `@` como argument files antes de que el comando vea nada. Con dos opciones no hay forma de nombrar un origen que no se lea: o se lee, o el pedido se rechaza nombrandolo. `AnalyzeOptions` lleva un solo campo con el conjunto ya resuelto porque la resolucion pasa en `call()`, el borde donde picocli entrega los valores, y asi `analyze(coursePath, options)` sigue siendo un metodo sin I/O que se puede ejercitar con `Set.of()`.

```architecture
modules:
  - name: audit-cli
    _change: modify
    models:
      - name: AnalyzeOptions
        _change: modify
        type: record
        fields:
          - name: reevaluateInstructionQuizIds
            type: Set<String>
            _change: modify
    packages:
      - name: commands
        _change: modify
        visibility: internal
        implementations:
          - name: AnalyzeCmd
            _change: modify
            requiresInject:
              - name: reevaluationQuizSetResolver
                type: ReevaluationQuizSetResolver
                _change: add
```
