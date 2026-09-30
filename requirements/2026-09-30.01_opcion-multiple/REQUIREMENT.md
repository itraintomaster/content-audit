---
feature:
  id: FEAT-OPMUL
  code: F-OPMUL
  name: Ejercicios de opcion multiple - cargar, conservar y medir sin corregir
  priority: critical
---

# Ejercicios de opcion multiple: cargar, conservar y medir sin corregir

## TL;DR

**Que**: ContentAudit carga y guarda los ejercicios de opcion multiple (MC) sin perder
nada, los mide, se los muestra al juez de consigna con sus opciones y no los corrige.

**Por que**: Es lo que el sistema hace desde el 30/9 sin ninguna regla que lo diga; sin
reglas, nada impide volver a perder las opciones al guardar o corregir un MC.

## Recorrido de uso (estado actual)

Como el operador audita y corrige hoy el curso vivo (2.810 MC entre 11.287 ejercicios):

1. Audita → `AnalyzeCommand.analyze(...)`. ✓ Disponible: mide los 11.287; antes abortaba en el primer MC.
2. Planifica → `PlanCommand.plan(...)`. ✓ Disponible: 4.036 tareas, 841 sobre MC.
3. Corrige → `ReviseCommand.revise(...)`, `ReviseInstructionsCommand.reviseInstructions(...)`,
   `AssessCandidateCommand.assessCandidate(...)`, `ApproveCommand.approve(...)`. ✓ Disponible: sobre un MC se rechaza.
4. Repara → `RepairCommand.repair(...)`. ✓ Disponible: un MC nunca vuelve a CLOZE.
5. Cuenta con que siga asi tras el proximo cambio → ⚠️ ninguna regla lo enuncia, y FEAT-COURSE sigue diciendo que todo ejercicio es CLOZE.

**Friction point**: paso 5. El soporte entro el 30/9 fuera del flujo del [Framework Sentinel](glossary:Framework Sentinel),
por decision explicita del usuario: funciona, pero no hay regla contra la cual verificarlo.

**Cambio minimo necesario**: enunciar como reglas lo que el sistema ya hace con los MC, sin agregar
comportamiento. Declararlo en la arquitectura es la fase siguiente.

## Reglas de Negocio

<a id="F-OPMUL-R001"></a>
### Rule[F-OPMUL-R001] - Cargar y guardar el curso sin cambios lo deja identico byte a byte
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Un curso con ejercicios de opcion multiple, escrito en el formato en que el sistema lo guarda,
> que se carga y se guarda sin cambios queda identico byte a byte: opciones, opcion correcta, modo
> de seleccion y todo dato que el sistema no interpreta vuelven tal cual y en el mismo orden.

**Criterio de aceptacion**: sobre el curso vivo del 2026-09-30, **886 de 886** archivos identicos;
antes diferian 133, porque el guardado borraba las opciones y la copia CLOZE del ejercicio
convertido. Es mas estricta que [F-COURSE-R003](#F-COURSE-R003), que admite reordenar.

<a id="F-OPMUL-R002"></a>
### Rule[F-OPMUL-R002] - Lo que el sistema no interpreta de un ejercicio sobrevive, con su valor y en su orden
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Todo dato de un ejercicio o de su formulario que el sistema no interpreta se conserva al guardar,
> con su valor y en su orden, despues de los que si interpreta. Aplicar una revision aprobada —a un
> CLOZE o a la etiqueta de su knowledge— tampoco lo pierde, ni en el revisado ni en sus hermanos,
> que ademas conservan sus opciones.

**Criterio de aceptacion**: (a) dos datos no interpretados —hoy, la copia CLOZE y respaldos de
instrucciones— vuelven al final y en su orden; (b) el CLOZE corregido conserva los suyos; (c) si se
corrige la etiqueta, el MC hermano solo cambia su titulo ([F-RPRES-R004](#F-RPRES-R004)). Es
[F-RPRES-R001](#F-RPRES-R001) aplicada a lo que el sistema no lee.

<a id="F-OPMUL-R003"></a>
### Rule[F-OPMUL-R003] - La auditoria mide todos los ejercicios, los de opcion multiple incluidos
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Auditar un curso con ejercicios de opcion multiple termina sin error y mide a todos sus ejercicios.
> Linea base del curso vivo al 2026-09-30: 11.287 medidos de 11.287, 2.810 de ellos de opcion
> multiple, y el curso en 73,9 %.

**Criterio de aceptacion**: esa linea base, medida sin el juez de consigna: A1 96,5 %, A2 94,2 %,
B1 79,5 %, B2 63,5 %; su plan tiene 4.036 tareas, 841 sobre MC. Antes abortaba en el primer MC,
rechazado como CLOZE malformado ([F-QSENT-R004](#F-QSENT-R004)). Es MC el ejercicio cuyo
formulario lo declara; un tipo que el sistema no reconoce se sigue tratando como CLOZE.

<a id="F-OPMUL-R004"></a>
### Rule[F-OPMUL-R004] - La oracion medible de un MC es el enunciado con la opcion correcta en el hueco, tal cual
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Un MC se mide sobre una sola oracion: su enunciado con el texto de la opcion correcta en el hueco,
> tal cual, la misma que tendria el CLOZE cuya unica respuesta aceptada fuera esa opcion. Ademas:
> 1. Ignora el modo REWRITE de su knowledge: la oracion previa al hueco se queda.
> 2. No tiene representacion en el lenguaje de oracion de FEAT-QSENT: ese dato queda vacio.
> 3. Se guarda como la de un CLOZE, entre sus [oraciones planas](glossary:Oraciones planas), y la auditoria la lee de ahi.
> 4. Sin opcion correcta, o con mas de un hueco, derivarla falla: no se inventa una.

"She ___ English." con am / **is** / are se mide "She is English."; la opcion entra como esta
escrita ("is he a doctor?") y una "|" en ella es literal. REWRITE ([F-SMODE-R004](#F-SMODE-R004))
no aplica porque el alumno lee la oracion entera: de 60 MC en knowledges REWRITE, a 10 les habria
recortado la oracion previa. **Criterio de aceptacion**: los 2.810 MC guardan la oracion derivada y
la de los 8.477 CLOZE no cambio. Sobre (2), ver [DOUBT-MC-HERMANOS](#DOUBT-MC-HERMANOS).

<a id="F-OPMUL-R005"></a>
### Rule[F-OPMUL-R005] - El juez ve las opciones de un MC con la correcta marcada, y un CLOZE igual que antes
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Por un MC, el juez de consigna recibe todas las opciones en el lugar del hueco, en el orden del
>    alumno y con la correcta marcada `[CORRECT]`, venga la consulta de la auditoria, de la
>    revalidacion de un diagnostico o de la evaluacion de un candidato.
> 2. Por un CLOZE recibe, caracter por caracter, lo mismo que antes de la opcion multiple: su
>    llegada no deja pendiente a ningun CLOZE con veredicto registrado.

MC: `TEXT:She:|MULTIPLE_CHOICE::am,is[CORRECT],are|TEXT:English.:|`; CLOZE, fijo:
`TEXT:She:|CLOZE::is|TEXT:English.:|`. Como el veredicto se identifica por el contenido juzgado
([F-QINST-R009](#F-QINST-R009)), cambiar una opcion, cual es la correcta o su orden deja pendiente
al MC, y los 2.810 arrancan pendientes. **Criterio de aceptacion**: dos MC que solo difieren en la
correcta tienen contenido juzgado distinto; tras el cambio, auditar no consulta al juez por ningun
CLOZE con veredicto.

<a id="F-OPMUL-R006"></a>
### Rule[F-OPMUL-R006] - Ninguna correccion toca un ejercicio de opcion multiple
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Pedir `revise`, `revise-instructions`, `assess-candidate` o `approve` sobre un MC se rechaza con
> un mensaje que lo dice, sin consultar a ningun modelo ni escribir el curso:
> 1. `revise` deja la [tarea](glossary:Tarea de corrección) SKIPPED, sea cual sea su diagnostico.
> 2. `revise-instructions` la cuenta aparte, ni corregida ni fallida, y la deja SKIPPED: la corrida
>    siguiente no la vuelve a tomar.
> 3. `assess-candidate` declara la consulta no disponible y no juzga ningun candidato.
> 4. `approve` no aplica la propuesta y la deja sin decidir, aunque sea de cuando era CLOZE.

**Criterio de aceptacion**: `revise` termina bien —es una politica, no una falla—; la corrida de
consignas informa "Opcion multiple (salteadas): N" (la tarea ocupa un lugar del tope solo esa vez);
`assess-candidate` y `approve` terminan con error, y `approve` mira el ejercicio en la propuesta y en
el curso de hoy. Ver [DOUBT-CORREGIR-MC](#DOUBT-CORREGIR-MC).

<a id="F-OPMUL-R007"></a>
### Rule[F-OPMUL-R007] - repair nunca restaura un MC desde una foto de la era CLOZE
**Severity**: critical | **Validation**: AUTO_VALIDATED

> `repair` no restaura un MC desde ninguna foto registrada por una revision: sigue siendo de
> opcion multiple, con sus opciones y sus datos no interpretados. Si perdio sus opciones, lo
> informa como irreparable y no lo completa.

**Criterio de aceptacion**: con un MC cuyas revisiones aprobadas son de cuando era CLOZE, `repair`
no repara nada y el ejercicio queda igual. Toda foto de un MC es anterior a su conversion:
restaurarla lo devolveria al CLOZE que fue ([F-RPRES-R005](#F-RPRES-R005)).

<a id="F-OPMUL-R008"></a>
### Rule[F-OPMUL-R008] - Los datos nuevos se comparan solo si la referencia ya los conocia
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Las opciones de un MC y los datos no interpretados se comparan contra una referencia —el curso
> antes de aprobar una correccion, o la foto desde la que `repair` restaura— solo si la referencia
> los trae. Si no los trae, no cuenta como ausencia: no hay violacion de la preservacion y `repair`
> no los borra. Si los trae, perderlos o cambiarlos viola [F-RPRES-R001](#F-RPRES-R001) y `approve`
> no escribe nada.

**Criterio de aceptacion**: (a) aprobar una correccion que pierde los datos no interpretados de un
CLOZE se aborta; (b) un CLOZE cuya foto es anterior al 30/9 conserva esos datos tras `repair`: en
esa foto "no estan" quiere decir "no se conocian".

<a id="F-OPMUL-R009"></a>
### Rule[F-OPMUL-R009] - Si el formulario no dice de que tipo es el ejercicio, vale el tipo del ejercicio
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Para no corregir ([F-OPMUL-R006](#F-OPMUL-R006)) ni restaurar ([F-OPMUL-R007](#F-OPMUL-R007)) un MC,
> el tipo lo dice el formulario: si dice CLOZE u opcion multiple, vale ese, diga lo que diga el
> ejercicio. Si no dice ninguno de los dos —no trae tipo, trae uno que el sistema no reconoce o no hay
> formulario—, vale el tipo del ejercicio.

Un MC cuyo formulario perdio su tipo sigue siendo MC: `revise` lo rechaza y `repair` no lo restaura
desde su foto de cuando era CLOZE. La auditoria no mira el tipo del ejercicio: mide segun el
formulario ([F-OPMUL-R003](#F-OPMUL-R003)). **Criterio de aceptacion**: si el ejercicio es de opcion
multiple, un formulario sin tipo da MC y uno que dice CLOZE da CLOZE. En el curso vivo los dos tipos
coinciden en los 11.287 ejercicios, asi que la regla no cambia ningun rechazo de hoy.

## Contexto

El curso vivo tiene 11.287 ejercicios; 2.810 son MC que produccion convirtio desde CLOZE, y cada uno
trae datos que el sistema no interpretaba, como la copia de su formulario CLOZE. Hasta el 30/9 la
auditoria abortaba en el primer MC y guardar el curso borraba esos datos y las opciones. El soporte
entro ese dia sin requerimiento, con el diseño de los briefs 021 y 022 (`docs/briefs/`); este
documento lo enuncia tal como quedo. La decision de fondo es **medir si, corregir no**: medido como el
CLOZE cuya unica respuesta es su opcion correcta, un MC queda comparable con el resto del curso;
corregirlo exige decidir que hacer con sus opciones. Una "foto" es la copia del ejercicio que una
revision aprobada registro antes de cambiarlo.

## Alcance

- **En alcance**: guardar sin perdida, tambien al revisar; la oracion medible de un MC; lo que el juez
  recibe por un MC y por un CLOZE; el rechazo de toda correccion sobre un MC; `repair` y la
  preservacion frente a los MC y a los datos nuevos.
- **Fuera de alcance**: **corregir un MC** ([DOUBT-CORREGIR-MC](#DOUBT-CORREGIR-MC)); **el criterio con
  que el juez juzga un MC**, que define el juez como en FEAT-QINST; **la publicacion a produccion**,
  paso externo que ya no publica formularios MC; **datos no interpretados en otros niveles**
  (knowledge, topic, hito) **o dentro de una opcion**, que hoy no hay
  ([DOUBT-SELECTION-NULL](#DOUBT-SELECTION-NULL)); **las 718 oraciones CLOZE que no coinciden con su
  derivacion** ([DOUBT-718-CLOZE](#DOUBT-718-CLOZE)); **otros modos de seleccion** que "SINGLE".

## User Journeys

### Journey[F-OPMUL-J001] - Auditar un curso con ejercicios de opcion multiple
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-OPMUL-J001
    name: Auditar un curso con ejercicios de opcion multiple
    flow:
      - id: auditar
        action: "El usuario audita un curso que mezcla CLOZE y MC, y la auditoria termina midiendo a todos"
        gate: [F-OPMUL-R003]
        outcomes:
          - {when: "Un MC esta en un knowledge FILL", then: mide_enunciado}
          - {when: "Un MC esta en un knowledge REWRITE y tiene una oracion antes del hueco", then: mide_entero}
          - {when: "El juez de consigna evalua un MC sin veredicto registrado", then: juez_ve_opciones}
          - {when: "Un CLOZE ya tiene veredicto de consigna registrado", then: reusa_cloze}
      - {id: mide_enunciado, action: "El MC se mide sobre su enunciado con la opcion correcta en el hueco, tal cual", gate: [F-OPMUL-R004], result: success}
      - {id: mide_entero, action: "La oracion medida conserva la oracion previa al hueco: REWRITE no la recorta", gate: [F-OPMUL-R004], result: success}
      - {id: juez_ve_opciones, action: "El juez recibe todas las opciones en el orden del alumno, con la correcta marcada [CORRECT]", gate: [F-OPMUL-R005], result: success}
      - {id: reusa_cloze, action: "El juez no recibe ninguna consulta por ese CLOZE: su veredicto se reutiliza", gate: [F-OPMUL-R005], result: success}
```

### Journey[F-OPMUL-J002] - Pedir una correccion sobre un ejercicio de opcion multiple
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-OPMUL-J002
    name: Pedir una correccion sobre un ejercicio de opcion multiple
    flow:
      - id: pedir
        action: "El usuario pide corregir un MC que tiene tareas en el plan"
        outcomes:
          - {when: "Revisa una tarea del MC, de cualquier tipo de diagnostico", then: revise_saltea}
          - {when: "Corre la correccion de consignas y la tarea del MC es elegible", then: lote_saltea}
          - {when: "Pide evaluar un candidato para la tarea del MC", then: sin_evaluacion}
          - {when: "Aprueba una propuesta sobre el MC hecha cuando todavia era CLOZE", then: sin_aprobacion}
      - {id: revise_saltea, action: "Ningun modelo es consultado, el curso no cambia y la tarea queda SKIPPED", gate: [F-OPMUL-R006], result: failure}
      - {id: lote_saltea, action: "El reporte cuenta la tarea como MC salteada y no como fallida; queda SKIPPED y la corrida siguiente no la toma", gate: [F-OPMUL-R006], result: failure}
      - {id: sin_evaluacion, action: "La consulta se declara no disponible y ningun candidato es juzgado", gate: [F-OPMUL-R006], result: failure}
      - {id: sin_aprobacion, action: "La propuesta queda sin decidir y el curso no cambia: el ejercicio sigue siendo MC", gate: [F-OPMUL-R006], result: failure}
```

### Journey[F-OPMUL-J003] - Guardar, revisar al lado y reparar sin perder nada
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-OPMUL-J003
    name: Guardar, revisar al lado y reparar sin perder nada
    flow:
      - id: operar
        action: "El usuario opera sobre un curso con MC y con datos que el sistema no interpreta"
        outcomes:
          - {when: "Lo carga y lo guarda sin cambios", then: identico}
          - {when: "Aprueba la correccion de un CLOZE, o de la etiqueta de su knowledge, que tiene un hermano MC", then: nada_se_pierde}
          - {when: "Repara el curso y un MC tiene una foto de cuando era CLOZE", then: mc_intacto}
          - {when: "Repara el curso y un CLOZE tiene datos no interpretados que su foto no conocia", then: datos_intactos}
      - {id: identico, action: "Cada archivo del curso queda identico byte a byte al original", gate: [F-OPMUL-R001], result: success}
      - {id: nada_se_pierde, action: "El CLOZE revisado conserva sus datos no interpretados y el MC hermano queda identico, opciones incluidas", gate: [F-OPMUL-R002], result: success}
      - {id: mc_intacto, action: "repair no repara el MC: sigue siendo de opcion multiple, con sus opciones y sus datos", gate: [F-OPMUL-R007], result: success}
      - {id: datos_intactos, action: "repair no borra esos datos: el CLOZE los conserva", gate: [F-OPMUL-R008], result: success}
```

## Open Questions

<a id="DOUBT-CORREGIR-MC"></a>
### Doubt[DOUBT-CORREGIR-MC] - Se corrigen los ejercicios de opcion multiple?
**Status**: OPEN — Hoy no ([F-OPMUL-R006](#F-OPMUL-R006)): 841 tareas del plan caen sobre MC y
revisarlas solo las deja SKIPPED; el juez sumara tareas de consigna sobre MC cuando corra.

- [ ] Opcion A (vigente): medir sin corregir.
- [ ] Opcion B: corregir enunciado y traduccion, sin tocar las opciones.
- [ ] Opcion C: corregir el ejercicio entero, con el juez vetando distractores tambien correctos.

**Answer**: Pendiente. Recomiendo A hasta que el juez responda sobre MC reales
([DOUBT-JUEZ-MC-EN-VIVO](#DOUBT-JUEZ-MC-EN-VIVO)) y muestre que defectos tienen; con esos datos,
elegir entre B y C.

<a id="DOUBT-SELECTION-NULL"></a>
### Doubt[DOUBT-SELECTION-NULL] - Un "selection": null explicito se pierde al guardar
**Status**: OPEN — Tambien las opciones en nulo explicito y cualquier dato extra dentro de una
opcion. El curso no tiene ninguno (0 de 2.810), y por eso [F-OPMUL-R001](#F-OPMUL-R001) se cumple.

- [ ] Opcion A: aceptarlo como limite conocido.
- [ ] Opcion B: conservarlos, como el resto de lo no interpretado.
- [ ] Opcion C: rechazar al cargar un MC con esa forma.

**Answer**: Pendiente. Recomiendo B la proxima vez que se toque la carga de MC:
[F-COURSE-R003](#F-COURSE-R003) ya exige distinguir nulo de ausente, asi que es una violacion
latente de una regla vigente, con cero casos. Mientras tanto, A alcanza.

<a id="DOUBT-718-CLOZE"></a>
### Doubt[DOUBT-718-CLOZE] - 718 CLOZE guardan una oracion distinta de la que se derivaria hoy
**Status**: OPEN — No es de MC. En 718 de 8.477 CLOZE la oracion guardada no es la que se
derivaria hoy (325 por espacios como `We 've`, 352 dialogos REWRITE sin recortar, 41 otros). La
auditoria mide lo guardado ([F-DBSENT-R002](#F-DBSENT-R002)), pero `revise` vuelve a derivar al
corregir, y la medicion cambia por algo que no es la correccion.

- [ ] Opcion A: dejarlo asi.
- [ ] Opcion B: volver a derivar y guardar los 718, moviendo la linea base de R003.
- [ ] Opcion C: que derivar un ejercicio sin corregir devuelva su oracion guardada.

**Answer**: Pendiente. Recomiendo C, en un requerimiento propio y antes de corregir CLOZE en masa:
B escribiria en los datos los defectos de la derivacion. No bloquea a los MC.

<a id="DOUBT-JUEZ-MC-EN-VIVO"></a>
### Doubt[DOUBT-JUEZ-MC-EN-VIVO] - El juez todavia no respondio sobre un MC real
**Status**: OPEN — Se verifico que recibe lo que pide [F-OPMUL-R005](#F-OPMUL-R005), pero no se
vio una respuesta: la sesion del proveedor habia vencido. Los 2.810 MC arrancan pendientes y un
veredicto solo se rehace a pedido ([F-QINST-R012](#F-QINST-R012)): un formato mal leido se pagaria
una vez y quedaria fijo.

- [ ] Opcion A: validar con 3 MC antes de la corrida completa (el brief 022 trae cuales y como).
- [ ] Opcion B: correr la pasada completa y re-evaluar los MC a pedido si algo salio mal.
- [ ] Opcion C (descartada): excluir los MC del juez; contradice [F-QINST-R014](#F-QINST-R014).

**Answer**: Pendiente. Recomiendo A: tres consultas no son nada al lado de 2.810, y B paga dos
veces lo que A paga una.

<a id="DOUBT-MC-HERMANOS"></a>
### Doubt[DOUBT-MC-HERMANOS] - Un MC queda fuera de la comparacion con sus hermanos al corregir un CLOZE
**Status**: OPEN — El brief 021 le daba al MC una forma CLOZE en el lenguaje de oracion para
seguir en el control anti-duplicado de las correcciones; quedo sin ella (R004). Al corregir un
CLOZE, la comparacion con los hermanos no ve a los MC; la del resto del curso si, con un umbral de
parecido mas alto.

- [ ] Opcion A (vigente): dejarlo asi.
- [ ] Opcion B: que los MC entren a la comparacion con los hermanos por su oracion medible.
- [ ] Opcion C: darles la forma CLOZE del brief 021.

**Answer**: Pendiente. Recomiendo B si el control importa en los knowledges que mezclan CLOZE y
MC: cierra el hueco sin que el MC se haga pasar por CLOZE, como hace C.

## References

- **FEAT-COURSE** — Idempotencia del guardado, que aca se vuelve byte a byte; deja de ser cierto
  que todo ejercicio es CLOZE. Citada por R001 y DOUBT-SELECTION-NULL.
- **FEAT-RPRES** — Preservacion por complemento y reparacion, extendidas a los datos nuevos y
  acotadas frente a los MC. Citada por R002, R007 y R008.
- **FEAT-QSENT**, **FEAT-SMODE**, **FEAT-DBSENT** — Lenguaje de oracion, modo REWRITE y oraciones
  planas guardadas. Citadas por R003, R004 y DOUBT-718-CLOZE.
- **FEAT-QINST**, **FEAT-QICOR**, **FEAT-REVAPR** — El juez de consigna y el reuso de sus
  veredictos; la correccion de consignas, los candidatos y la aprobacion. Citadas por R005 y R006.
