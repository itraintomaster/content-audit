---
patch: FEAT-OPMUL
requirement: 2026-09-30.01_opcion-multiple
generated: 2026-09-30T17:56:13Z
---

# Tech Spec: Ejercicios de opcion multiple, declarados despues de hechos

> **Alcance de este documento.** El soporte de opcion multiple entro el 30/9 sin pasar por Sentinel (83228e54). Este parche no diseña nada: declara lo que el codigo ya tiene, sin agregar comportamiento ni tocar firmas de interfaces. Los fences diffean contra `sentinel-baseline.yaml`, el `sentinel.yaml` de antes del parche.
>
> **Lo que el DSL no puede declarar.** Un modelo del DSL es una lista de campos: el generador emite constructor vacio, constructor completo con todos los campos, getters, setters, `equals` y `hashCode`, y nada mas. Por eso cinco piezas del codigo quedan fuera del parche: los constructores de copia de `FormEntity` y `QuizTemplateEntity`, sus `formKind()`, `FormKind.from(String)`, `MultipleChoiceEntity.correctItem()` y los comentarios de los campos nuevos. A eso se suma que en cuatro modelos el codigo dejo el constructor completo con la aridad vieja. Con este parche aplicado, `generate` todavia no reproduce el codigo de hoy; cada seccion dice que parte de esa brecha le toca.

## Declarar FormKind en course-domain

El codigo decide que hacer con un formulario leyendo su `kind` crudo de forma tipada; el string crudo no se toca porque tiene que volver byte a byte. Un valor desconocido cae en OTHER y se sigue tratando como CLOZE, igual que antes de la opcion multiple (F-OPMUL-R003). Se declara porque `FormEntity.formKind()` lo devuelve y el ArchUnit de course-domain verifica que cada tipo declarado exista. `FormKind.from(String)` no es expresable: el archivo sigue escrito a mano, sin `@Generated`, y `generate` lo saltea sin tocarlo.

```architecture
modules:
  - name: course-domain
    _change: modify
    models:
      - name: FormKind
        _change: add
        type: enum
        fields:
          - name: CLOZE
          - name: MULTIPLE_CHOICE
          - name: OTHER
```

## Declarar la carga de opcion multiple: MultipleChoiceItemEntity y MultipleChoiceEntity

Son `form.items` y `form.selection`, lo que el guardado borraba antes del 30/9 (F-OPMUL-R001). `selection` viaja como string crudo y no como enum: solo se observo SINGLE, nada ramifica sobre el, y un enum pondria en riesgo el byte a byte de un valor nuevo. `correctItem()` —la opcion con incidence mayor que cero— no es expresable, asi que los dos archivos siguen escritos a mano y `generate` los saltea. La sugerencia de glosario deja al analista decidir si el concepto que nombran todas las reglas de FEAT-OPMUL merece termino canonico.

```architecture
modules:
  - name: course-domain
    _change: modify
    models:
      - name: MultipleChoiceItemEntity
        _change: add
        type: record
        fields:
          - { name: id, type: String }
          - { name: incidence, type: double }
          - { name: label, type: String }
      - name: MultipleChoiceEntity
        _change: add
        type: record
        fields:
          - { name: selection, type: String }
          - { name: items, type: "List<MultipleChoiceItemEntity>" }
        glossarySuggestions:
          - name: "Ejercicio de opcion multiple"
            technicalName: MultipleChoice
            kind: value-object-candidate
            basedOn: course-domain/MultipleChoiceEntity
```

## Agregar multipleChoice y unmodeledFields a FormEntity

El round trip byte a byte del curso vivo (886 de 886 archivos, F-OPMUL-R001) depende de que el formulario conserve sus opciones y toda clave que no interpreta, con su valor y en su orden (F-OPMUL-R002). Los dos campos van al final y en el orden del codigo, para que getters, `equals` y `hashCode` generados coincidan. No se declaran el constructor de copia ni `formKind()`: `generate` reescribe entero un modelo `@Generated` y los borraria. Entre los dos modelos, los constructores de copia se usan en 5 lugares de produccion y `formKind()` en 10. Aparte, el smart-merge de las implementaciones toma cada `new FormEntity(x)` o `new QuizTemplateEntity(x)` por un constructor completo viejo y lo rellena con valores por defecto, codigo que no compila: pasa ya hoy, sin este parche, en 4 implementaciones. Tampoco se puede declarar el constructor completo de 5 parametros que dejo el codigo: el generado tendria 7. Cerrar esa brecha pide cambiar codigo o cambiar Sentinel, y esa decision es de José.

```architecture
modules:
  - name: course-domain
    _change: modify
    models:
      - name: FormEntity
        _change: modify
        fields:
          - { name: multipleChoice, type: MultipleChoiceEntity, _change: add }
          - { name: unmodeledFields, type: "Map<String,Object>", _change: add }
        glossarySuggestions:
          - name: "Dato no interpretado"
            technicalName: UnmodeledField
            kind: value-object-candidate
            basedOn: course-domain/FormEntity
```

## Agregar unmodeledFields a QuizTemplateEntity

Mismo motivo un nivel arriba: en el ejercicio viven `formCloze` y los respaldos de instrucciones que produccion agrega y el sistema no lee (F-OPMUL-R001, F-OPMUL-R002). Quedan fuera, por la misma limitacion, el constructor de copia y `formKind()`, que prefiere el kind del formulario y cae al del ejercicio cuando el formulario no trae uno reconocible. El constructor completo del codigo sigue con 21 parametros; el generado tendria 22.

```architecture
modules:
  - name: course-domain
    _change: modify
    models:
      - name: QuizTemplateEntity
        _change: modify
        fields:
          - { name: unmodeledFields, type: "Map<String,Object>", _change: add }
```

## Llevar las opciones hasta el juez: multipleChoice en AuditableQuiz y QuizInstructionSubjectView

El juez de consigna tiene que ver las opciones de un MC con la correcta marcada, venga la consulta de la auditoria, de la revalidacion o de un candidato (F-OPMUL-R005), y estos dos modelos son el camino de las opciones hasta el render. El campo es nulo en un CLOZE, que por eso se renderiza identico y no pierde ningun veredicto pagado. En el codigo el constructor completo no se extendio (9 y 6 parametros); el generado tendria 10 y 7, y eso toca casi 300 llamadas en tests.

```architecture
modules:
  - name: audit-domain
    _change: modify
    models:
      - name: AuditableQuiz
        _change: modify
        fields:
          - { name: multipleChoice, type: MultipleChoiceEntity, _change: add }
    packages:
      - name: quizinstruction
        _change: modify
        models:
          - name: QuizInstructionSubjectView
            _change: modify
            fields:
              - { name: multipleChoice, type: MultipleChoiceEntity, _change: add }
```

## Agregar MULTIPLE_CHOICE_UNSUPPORTED a los tres desenlaces

Rechazar la correccion de un MC es una politica, no una falla (F-OPMUL-R006): `revise` termina bien y deja la tarea SKIPPED, la corrida de consignas la cuenta aparte y `approve` deja la propuesta sin decidir. Un desenlace propio en cada enum es lo que deja que la CLI y el runner lo distingan de NO_REVISER, FAILED o PRESERVATION_VIOLATED. Son los unicos elementos que este parche reconcilia del todo: aplicado, `verify` deja de marcar los tres enums.

```architecture
modules:
  - name: revision-domain
    _change: modify
    models:
      - name: RevisionOutcomeKind
        _change: modify
        type: enum
        fields:
          - { name: MULTIPLE_CHOICE_UNSUPPORTED, _change: add }
      - name: ProposalDecisionOutcomeKind
        _change: modify
        type: enum
        fields:
          - { name: MULTIPLE_CHOICE_UNSUPPORTED, _change: add }
    packages:
      - name: quizinstruction
        _change: modify
        models:
          - name: QuizInstructionTaskOutcomeKind
            _change: modify
            type: enum
            fields:
              - { name: MULTIPLE_CHOICE_UNSUPPORTED, _change: add }
```

## Contar aparte las tareas MC en QuizInstructionCorrectionRunReport

La corrida informa "Opcion multiple (salteadas): N" y no las suma a failed ni a notCorrected, para que el reporte siga cerrando contra attempted (F-OPMUL-R006). Aca el codigo si extendio el constructor completo a 17 parametros, asi que lo unico que separa el archivo de lo generado es una linea de comentario.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: quizinstruction
        _change: modify
        models:
          - name: QuizInstructionCorrectionRunReport
            _change: modify
            fields:
              - { name: multipleChoiceUnsupported, type: int, _change: add }
```

## Registrar FEAT-OPMUL y ubicar sus tres journeys

`feature status` no encuentra FEAT-OPMUL, y sin registro la feature no llega a la generacion ni a la trazabilidad. Al aplicar, Sentinel copia desde REQUIREMENT.md la compuerta de activacion con F-OPMUL-J001, J002 y J003, y el parche los ubica: cada uno trae testModule y testPackage, asi que `generate` no se niega a correr y crea sus tres journey tests, con 12 caminos entre los tres. Cada journey va al modulo que ve a todos sus participantes y a su paquete exacto, con el mismo criterio que F-QINST, F-REVBYP y F-RPRES.

```architecture
features:
  - id: FEAT-OPMUL
    code: F-OPMUL
    journeys:
      - { id: F-OPMUL-J001, testModule: audit-application, testPackage: com.learney.contentaudit.auditapplication }
      - { id: F-OPMUL-J002, testModule: revision-domain, testPackage: com.learney.contentaudit.revisiondomain.engine }
      - { id: F-OPMUL-J003, testModule: audit-cli, testPackage: com.learney.contentaudit.journeys }
modules:
  - name: course-domain
    _change: modify
```

## Declarar las 34 pruebas de FEAT-OPMUL

El parche declara 34 handwrittenTests para las 9 reglas. 28 portan un test MC que ya existe desde 83228e54: su cuerpo pasa al stub que `generate` crea en `{Impl}Test`, porque generate ignora className y sourceFile, y el original se borra. 6 son nuevos y cubren clausulas que ningun test probaba, como el approve que perderia los datos de hoy (R008) o el MC cuyo formulario perdio su tipo (R009). De los 39 tests MC, los otros 11 quedan como pruebas comunes porque no verifican una regla de FEAT-OPMUL: 4 sostienen la carga y el guardado, entre ellos el del orden en que se escribe un formulario MC; 5 son internos del modelo, y 2 son guardas de FEAT-QSENT.

## Cubrir F-OPMUL-R009 en revise y en repair

Si el formulario no dice CLOZE ni opcion multiple, vale el tipo del ejercicio (F-OPMUL-R009, pregunta 3 del brief 092). Hoy lo resuelve `QuizTemplateEntity.formKind()` y lo cubre una sola prueba, `MultipleChoiceModelTest.quizFormKindFallsBackToQuizKind`; con la opcion A ese metodo sale del modelo, y un modelo no lleva handwrittenTests, asi que la regla se prueba en quien decide con ella. La de revise porta los casos de esa prueba: formulario sin tipo en un ejercicio de opcion multiple, rechazo; formulario CLOZE, se corrige. La de repair es nueva: el MC cuyo formulario perdio su tipo, que sin la regla volveria al CLOZE de su foto.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: engine
        implementations:
          - name: DefaultRevisionEngine
            handwrittenTests:
              - name: "should reject revising a quiz whose own kind is multiple choice when its form does not say one, but not when its form says CLOZE"
                traceability: { feature: FEAT-OPMUL, rule: F-OPMUL-R009 }
      - name: preservationengine
        implementations:
          - name: DefaultPreservationRepair
            handwrittenTests:
              - name: "should never restore a quiz whose own kind is multiple choice and whose form lost its kind from a snapshot recorded when it was a CLOZE"
                traceability: { feature: FEAT-OPMUL, rule: F-OPMUL-R009 }
```
