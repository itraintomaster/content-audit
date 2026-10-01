---
patch: FEAT-CIERRE
requirement: 2026-09-30.32_cierre-ronda-1
generated: 2026-10-01T02:30:00Z
---

# Tech Spec: Cierre de la ronda 1, los cuatro huecos de los jueces

Cuatro cambios chicos al contrato que dejó la construcción del paquete C. Hoy se resuelven en el código con un
renderizador armado adentro, una regla de enunciado duplicada o la primera causa de la corrida. Tienen que quedar
fijos antes de la primera corrida paga.

**C-27 no está en este parche porque ya está cumplido.**
- Las cinco features de C están registradas con su J001 y su lugar de prueba (`feature sync --dry-run` dice «in sync»
  y `feature status` dice «OK»).
- `definitions` no es un campo del parche, y la aplicación lo copia tal cual.

## Add JudgeQuizTally and count it in the tally and the coverage

`mini-theory` consulta por tema, y su cobertura sólo puede contar temas: los 11.287 ejercicios que deja sin evaluar se
ven en el informe, pero no en su cobertura, que F-MINIT-R005 inv. 4 pide. Por eso se suma una cuenta de ejercicios con
los tres mismos estados, en la cuenta que arma el analizador y en la cobertura que arma el runtime común.

La cuenta va como un tipo propio y no como tres enteros sueltos en cada modelo: los dos la llevan igual, y el runtime la
copia de una a la otra sin recalcular. Para los cuatro jueces que consultan por ejercicio, los números coinciden con las
unidades, así que no hay que pensar dos veces cuál mirar.

```architecture
modules:
- name: audit-domain
  packages:
  - name: judging
    models:
    - name: JudgeQuizTally
      type: record
      fields:
      - name: reached
        type: int
      - name: evaluated
        type: int
      - name: notEvaluated
        type: int
      _change: add
    - name: JudgeUnitTally
      type: record
      fields:
      - name: quizzes
        type: JudgeQuizTally
        _change: add
      _change: modify
    - name: JudgeCoverageDiagnosis
      type: record
      fields:
      - name: quizzes
        type: JudgeQuizTally
        _change: add
      _change: modify
    _change: modify
  _change: modify
```

## Add the failure cause to EvaluationResolution

Una sesión que resuelve en lote devuelve un FAILED sin causa. Los analizadores ponen entonces la primera causa de la
corrida en todos los ejercicios fallidos, aunque uno haya fallado por una salida inutilizable y otro por el servicio caído.

La causa ya existe como modelo: es el `EvaluationNotEmitted` que devuelve el evaluador, con el tipo y el motivo. El
resultado la lleva tal cual, y la sesión la pone sólo en FAILED. Así la falla sigue sin registrarse en el libro, y un
pendiente por tope o por la parada del servicio no se confunde con una falla.

```architecture
modules:
- name: evaluation-ledger-domain
  models:
  - name: EvaluationResolution
    type: record
    fields:
    - name: failure
      type: EvaluationNotEmitted
      _change: add
    _change: modify
  _change: modify
```

## Inject QuizStudentViewRenderer into the three classifiers

El juez lee el ejercicio con la vista del alumno (F-RONDA1-R001), pero hoy tres componentes escriben el mismo ejercicio en
la evidencia con su propio código:
- el clasificador de opción múltiple instancia un renderizador adentro;
- el de una sola respuesta duplica la regla del enunciado;
- el repartidor de miniteoría arma su oración terminada.

Hoy coinciden, pero nada lo garantiza. Con el renderizador inyectado, lo que muestra un hallazgo y lo que leyó el juez
salen del mismo lugar, y un cambio del formato los mueve a los dos juntos.

Los proveedores que los crean ya reciben el renderizador (gancho C-15).

```architecture
modules:
- name: audit-domain
  packages:
  - name: singleanswerengine
    implementations:
    - name: DefaultSingleAnswerClassifier
      requiresInject:
      - name: studentViewRenderer
        type: QuizStudentViewRenderer
        _change: add
      _change: modify
    _change: modify
  - name: choicealternativeengine
    implementations:
    - name: DefaultChoiceAlternativeClassifier
      requiresInject:
      - name: studentViewRenderer
        type: QuizStudentViewRenderer
        _change: add
      _change: modify
    _change: modify
  - name: minitheoryengine
    implementations:
    - name: DefaultMiniTheoryDistributor
      requiresInject:
      - name: studentViewRenderer
        type: QuizStudentViewRenderer
        _change: add
      _change: modify
    _change: modify
  _change: modify
```

## Give DefaultQuizInstructionConfig the judges runtime config

`config analyzer quiz-instruction` muestra hoy «500», que es el tope de `revise-instructions`. El tope de `analyze` es 0
y vive en la configuración común de los jueces (F-JEXAM-R010).

Inyectarla en la configuración de consigna deja que `describe()` muestre los dos topes, cada uno con su comando, sin
copiar el 0: si la configuración común cambia, lo que se muestra cambia con ella. El tope propio de esta configuración
sigue siendo el de `revise-instructions`.

```architecture
modules:
- name: audit-application
  implementations:
  - name: DefaultQuizInstructionConfig
    requiresInject:
    - name: judgeRuntimeConfig
      type: JudgeRuntimeConfig
      _change: add
    _change: modify
  _change: modify
```
