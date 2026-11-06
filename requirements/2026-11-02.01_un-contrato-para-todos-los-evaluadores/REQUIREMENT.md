---
feature:
  id: FEAT-CONTR
  code: F-CONTR
  name: Un contrato para todos los evaluadores - cada evaluador deja a cada ejercicio en uno de cuatro estados con los mismos cinco pasos, y un solo recorrido decide la lista y el veredicto del cambio
  priority: major
---

# Un contrato para todos los evaluadores

## TL;DR

**Qué**: cada evaluador de ejercicio (los jueces, los analizadores y el revisor) deja a cada ejercicio en uno de cuatro
estados —no aplica, falta, supera, no supera— con los mismos cinco pasos. Un solo recorrido sobre esos estados decide en
qué lista está el ejercicio y qué dice el veredicto del cambio. Lo calcula content-audit una vez; el tablero lo cuenta y
lo dibuja.

**Por qué**: el 1/11/2026, en la C2 de A1, José encontró `…24a7` en «Todos los jueces lo aprueban» con «Oraciones con el
largo de su nivel: error nuevo» en rojo. Tres lugares decidían si un ejercicio tenía un problema, cada uno con su copia
de las reglas, y 296 de los 665 de la lista se contradecían con su propio veredicto (brief 246). José pidió que todos los
evaluadores cumplan el mismo contrato y que la decisión quede centralizada (brief 248), que la gravedad baja no frene
(«Está bien que "baja" no frene», 1/11), y aprobó las decisiones de los briefs 249 y 250.

## Recorrido de uso (estado actual)

1. José abre «Todos los jueces lo aprueban» ✓.
2. ⚠️ Un ejercicio de la lista muestra un renglón en rojo: la lista y el renglón deciden con reglas distintas.
3. ⚠️ Un cambio dice «Lo dan por bueno» y su ejercicio no está en la lista, o al revés.

**Punto de fricción**: pasos 2 y 3. Cada pantalla decide por su cuenta.

**Cambio mínimo necesario**: un estado por evaluador y ejercicio, calculado una vez, que lean la lista, el veredicto y
los renglones.

## Reglas de Negocio

<a id="F-CONTR-R001"></a>
### Rule[F-CONTR-R001] - Cuatro estados y cinco pasos, iguales para todos
**Severity**: major | **Validation**: VALIDATED

> Cada evaluador deja a cada ejercicio en uno de cuatro estados: **no aplica**, **falta**, **supera** o **no supera**.
> Lo decide con los mismos cinco pasos, en este orden; el primero que decide, decide:
>
> 1. **¿Aplica?** Si el evaluador no corresponde a ese ejercicio —la opción múltiple a uno de completar, la calidad
>    didáctica A1 a otro nivel— o no lo alcanza, **no aplica**.
> 2. **¿Dejó una opinión?** Si aplica y no la dejó —no se midió, quedó sin evaluar o a medias—, **falta**.
> 3. **¿Es de la versión de hoy?** Si es de una versión anterior del juez, **falta**
>    ([F-ALDIA-R003](../2026-10-18.01_jueces-al-dia/REQUIREMENT.md#F-ALDIA-R003)): sólo la versión decide, el motor
>    nunca.
> 4. **¿Decidió?** Si se abstuvo diciendo que no aplica, **no aplica**; si se abstuvo para que alguien lo revise o porque
>    le falta contexto, **falta**.
> 5. **¿Dejó algo de gravedad media o más?** Si sí, **no supera**; si no, **supera**.
>
> Son evaluadores los veinte analizadores de ejercicio del catálogo y el revisor. Lo único que cambia entre ellos es a
> qué ejercicios aplican, cuál es su versión de hoy y desde qué gravedad frenan, que es media para todos.

<a id="F-CONTR-R002"></a>
### Rule[F-CONTR-R002] - Lo leve no frena, lo que sólo ordena cuenta y la oración de producción no es del ejercicio
**Severity**: major | **Validation**: VALIDATED

> En el paso 5:
>
> - Una observación leve no frena a ningún evaluador, sea juez o analizador: el evaluador **supera** y la observación se
>   sigue viendo, como aviso ([F-LEVE-R005](../2026-10-28.04_lo-leve-no-frena/REQUIREMENT.md#F-LEVE-R005)). Si un juez
>   llama leve a algo que está mal, se recalibra el juez.
> - Una observación que sólo ordena —«ofrece otra respuesta» de «Una sola respuesta»— cuenta por su gravedad, como
>   cualquier otra.
> - La oración que guarda producción, que sólo cambia al publicar
>   ([F-MESA-R026](../2026-09-30.22_la-mesa-por-tema/REQUIREMENT.md#F-MESA-R026)), no es una observación del
>   ejercicio: no lo frena.
> - Una observación sin gravedad conocida frena: no se la supone leve.

<a id="F-CONTR-R003"></a>
### Rule[F-CONTR-R003] - El desacuerdo espera y «Casi repetido» cuenta sin examen
**Severity**: major | **Validation**: VALIDATED

> - Cuando los dos jueces del sentido de la traducción no se ponen de acuerdo, el evaluador queda en **falta** hasta que
>   se desempate. Si el mismo evaluador dejó además otra observación de gravedad media o más, **no supera**: esa
>   observación ya decide.
> - «Ejercicio casi repetido» no tiene hoy una versión aprobada: cuenta su opinión con cualquier versión hasta que su
>   versión de hoy rinda examen, y la pantalla lo avisa. Como sólo deja observaciones leves, no saca a ningún ejercicio
>   de la lista.

<a id="F-CONTR-R004"></a>
### Rule[F-CONTR-R004] - El revisor cumple el mismo contrato
**Severity**: major | **Validation**: VALIDATED

> El revisor lee lo que la tanda cambió, con producción al lado
> ([F-MARCA-R001](../2026-10-26.02_el-revisor-marca-lo-aprobado/REQUIREMENT.md#F-MARCA-R001)):
>
> | Marca | Estado |
> |---|---|
> | El ejercicio no cambió | no aplica |
> | Sin marca, o una que no se puede usar | falta |
> | «Bien» | supera |
> | «Dudoso» | supera, y se ve en ámbar |
> | «Heredado» (todos sus problemas ya estaban en producción) | no supera |
> | «Mal» | no supera |
>
> Cuenta también la marca de una versión anterior del revisor
> ([F-TODOS-R002](../2026-10-31.01_todos-quiere-decir-todos/REQUIREMENT.md#F-TODOS-R002)). Leer las marcas no consulta al
> revisor ([F-MARCA-R003](../2026-10-26.02_el-revisor-marca-lo-aprobado/REQUIREMENT.md#F-MARCA-R003)).

<a id="F-CONTR-R005"></a>
### Rule[F-CONTR-R005] - Un recorrido decide la lista
**Severity**: major | **Validation**: VALIDATED

> Se recorren los estados de todos los evaluadores en la corrección:
>
> - si alguno **no supera**, el ejercicio tiene un problema;
> - si no, y alguno **falta**, va a «Faltan opiniones»;
> - si no, va a «Todos los jueces lo aprueban».
>
> Tu aprobación los reparte como hoy: aprobado y sin problema, «Preparados para entregar»; aprobado y con un problema,
> «Vos lo aprobás».
>
> El contrato es del contenido de hoy: una medición guardada de otro contenido —el ejercicio se volvió a corregir
> después— no saca al ejercicio de la lista ni cambia el veredicto de su cambio (R006). En la C2 del 3/11 escondía 744
> ejercicios que el contrato daba por buenos (brief 257).

<a id="F-CONTR-R006"></a>
### Rule[F-CONTR-R006] - El veredicto del cambio sale del mismo recorrido
**Severity**: major | **Validation**: VALIDATED

> Cada ejercicio que la tanda cambió tiene un veredicto, uno por ejercicio:
>
> - **Empeoró**: algún evaluador **no supera** en la corrección y en producción **supera** o **no aplica**. Del revisor,
>   «Mal» empeoró; «Heredado» no, porque el problema ya estaba.
> - Si no, **No consta arreglado** si el ejercicio tiene un problema; **Falta medir** si va a «Faltan opiniones»; **Lo dan
>   por bueno** si va a «Todos los jueces lo aprueban».
>
> Un evaluador que en producción **falta** no prueba que el cambio empeoró: el ejercicio queda en «No consta arreglado».
> Así, «Lo dan por bueno» quiere decir exactamente que el ejercicio está en «Todos los jueces lo aprueban» o en
> «Preparados para entregar».

<a id="F-CONTR-R007"></a>
### Rule[F-CONTR-R007] - El texto de vos se mide gratis
**Severity**: major | **Validation**: VALIDATED

> Un analizador de errores instantáneo que la tanda no tenía al crearse —el del texto de vos— se mide al leer el análisis,
> en producción y en la corrección, sin consultar a ningún modelo. Sus observaciones se ven y cuentan como las de
> cualquier otro analizador.

<a id="F-CONTR-R008"></a>
### Rule[F-CONTR-R008] - Una sola cuenta para toda la pantalla
**Severity**: major | **Validation**: VALIDATED

> La lista, el veredicto del cambio, los renglones de cada evaluador y el «No entra porque…» leen los mismos estados:
>
> - ningún ejercicio de «Todos los jueces lo aprueban» o «Preparados para entregar» tiene un evaluador que **no supera**
>   ni un renglón en rojo;
> - un evaluador que **supera** o **no aplica** nunca deja un renglón en rojo: lo que vio se ve en ámbar, como lo leve.
>   El rojo es de uno que **no supera** o de uno al que le **falta** la opinión de hoy (la dio con una versión anterior,
>   o sus dos jueces no coinciden);
> - un renglón dice que el error es nuevo sólo si su evaluador empeoró (R006).
>
> Si el análisis todavía no trae los estados, la pantalla sigue con su cuenta de antes.

## Contexto

- **Cuánto cambia (simulación del 1/11 sobre la C2, antes de medir el texto de vos)**: «Todos los jueces lo aprueban»
  1.580, «Preparados para entregar» 192, con un problema 515, «Faltan opiniones» 108, «Vos lo aprobás» 7. De los 1.922
  ejercicios cambiados: Empeoró 135, Lo dan por bueno 1.417, Falta medir 78, No consta arreglado 292.
- **Qué reemplaza**: la elegibilidad de [F-NADIE-R001](../2026-10-20.01_nadie-le-ve-problemas/REQUIREMENT.md#F-NADIE-R001),
  [F-LEVE-R001](../2026-10-28.04_lo-leve-no-frena/REQUIREMENT.md#F-LEVE-R001),
  [F-LEVE-R003](../2026-10-28.04_lo-leve-no-frena/REQUIREMENT.md#F-LEVE-R003),
  [F-LEVE-R004](../2026-10-28.04_lo-leve-no-frena/REQUIREMENT.md#F-LEVE-R004) y
  [F-TODOS-R001 a R003](../2026-10-31.01_todos-quiere-decir-todos/REQUIREMENT.md#F-TODOS-R001): cada uno decidía con su
  propia regla lo que ahora decide el contrato.
- **Por fases (brief 250)**: primero content-audit calcula los estados sin que cambie la pantalla y se comparan con la
  simulación; después el tablero los lee; las copias de las reglas del tablero se borran después de unos días de uso.
- **Fuera de alcance**: Publicar, los resúmenes, Evolución y la app. El cierre quedó afuera hasta el 5/11/2026, cuando
  José decidió que siga a R002 (brief 275): lo cuentan [F-NOEMP-R005](../2026-09-30.23_no-empeorar/REQUIREMENT.md#F-NOEMP-R005)
  y [F-METAS-R003](../2026-09-30.27_antes-y-despues-con-metas/REQUIREMENT.md#F-METAS-R003). Que «aplica a» sea un dato de la ficha
  del evaluador queda para otra fase.

## Cómo se prueba

| Regla | Prueba |
|---|---|
| R001 | `EvaluatorContractTest`: cada paso decide en su orden para un juez y para un analizador (no aplica, falta sin opinión, falta sin evaluar, falta de versión anterior, abstenciones, supera, no supera) |
| R002 | `EvaluatorContractTest`: lo leve supera; lo que sólo ordena con gravedad media no supera; la oración de producción no frena; sin gravedad frena |
| R003 | `EvaluatorContractTest`: el desacuerdo queda en falta y con otra observación media no supera; un analizador pago sin juez de hoy cuenta cualquier versión; `CurrentAnalysisServiceTest`: la vista trae su aviso; en ittm, `contrato_de_evaluadores_test.dart` (el aviso viaja con su ficha) y `contrato-de-evaluadores.test.js` (su renglón dice «cualquier versión», no «al día») |
| R004 | `EvaluatorContractTest`: las seis filas de la tabla, la marca de una versión anterior, y que leer las marcas no gasta consultas |
| R005 | `EvaluatorContractTest`: el recorrido con un no supera, con un falta y con todos superan o no aplican; en ittm, `contrato_de_evaluadores_test.dart` (la lista es la del contrato) y `contrato-de-evaluadores.test.js` (la pantalla pone cada ejercicio donde dice el contrato) |
| R006 | `EvaluatorContractTest`: empeoró desde supera y desde no aplica, no desde falta ni desde no supera, y quién empeoró viaja con el veredicto; «Mal» empeoró y «Heredado» no; los otros tres veredictos siguen al recorrido; `CurrentAnalysisServiceTest`: el ejercicio que la tanda no cambió no tiene veredicto |
| R007 | `CurrentAnalysisServiceTest`: el analizador instantáneo que la tanda no tenía se mide en la base y en la corrección, y no se mide el que ya tenía ni uno pago |
| R008 | En ittm, `contrato_de_evaluadores_test.dart`: el chip de cada veredicto, el color y el «nuevo» de cada renglón (también «no aplica» en ámbar y «falta» en rojo), un ejercicio que el contrato no trae, un contrato con valores que el tablero no conoce, la invariante (lo que dan por bueno es justo «Todos», sin un no supera ni un renglón en rojo) y la cuenta de antes sin contrato; `contrato-de-evaluadores.test.js`: sin contrato cuenta como antes y el revisor se nombra sin examen. La invariante sobre la C2 entera se verifica con su lectura real (brief 251) |

## References

- Brief 248: el contrato. Brief 249: las cuatro decisiones. Brief 250: cómo construirlo.
- [F-ALDIA-R003](../2026-10-18.01_jueces-al-dia/REQUIREMENT.md#F-ALDIA-R003): la marca de cada opinión contra el juez de
  hoy.
- [F-MARCA-R001](../2026-10-26.02_el-revisor-marca-lo-aprobado/REQUIREMENT.md#F-MARCA-R001): qué lee el revisor.
