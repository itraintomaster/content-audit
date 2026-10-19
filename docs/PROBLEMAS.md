# Problemas del curso

Implementación inicial de `docs/propuestas/problemas.html`. El tablero HTML está en
`ittm/pipeline/vocabulary_dashboard`; el registro y los comandos están en ContentAudit.

El operador describe cada problema una sola vez: nombre, descripción general y lista
de quizzes. Desde Correcciones, el botón **Problema** y el atajo **P** permiten elegir
un problema existente o crear uno con esos dos campos. Sumar otro quiz no pide texto
adicional; durante la revisión se recuerda el último problema usado. Se guardan el
antes, el después y la procedencia automáticamente. Por omisión se rechaza la corrección; la opción se puede quitar y
la anotación se puede deshacer mientras la corrección y la decisión sigan siendo compatibles.
Las decisiones pendientes en la cola deben guardarse o descartarse antes de anotar.

Cada curso tiene su registro en `<curso>/.content-audit/problems/`. No pertenece a la
tanda: las campañas del mismo curso comparten los problemas. Los casos son snapshots
congelados y sobreviven a cambios en la campaña. Un caso nuevo reabre el problema y
retira la entrega anterior.

La pantalla **Problemas** permite editar el nombre y la descripción, buscar textos o expresiones
regulares en una tanda y agregar grupos de quizzes como Examen o Está bien. El agente
puede seleccionar ejemplos para el examen; sólo el operador redefine los controles
Está bien. Detección y corrección se presentan por separado; un resultado ausente se
muestra como «Sin medir».

## Pedido y seguimiento

**Preparar pedido** obtiene el texto completo: ficha, todos los casos, guías actuales
con su SHA-256, instrucciones del proyecto y comandos para registrar avances. Se puede
pegar en Claude, ChatGPT o Codex con acceso al checkout. El agente recibe los casos;
el juez recibe sólo su guía general y el quiz evaluado. Los archivos `eval/` no forman
parte del pedido. El tablero consulta el registro cada diez segundos mientras está visible.

Todos los comandos `problem`, incluido `--help`, usan datos guardados y evitan el
arranque de NLP y de modelos:

```sh
./audit-cli.sh --workdir /ruta/campaña problem list --course /ruta/curso
./audit-cli.sh --workdir /ruta/campaña problem show PRB-id --course /ruta/curso
./audit-cli.sh --workdir /ruta/campaña problem inspect PRB-id --course /ruta/curso
./audit-cli.sh --workdir /ruta/campaña problem request PRB-id --course /ruta/curso -f text
./audit-cli.sh --workdir /ruta/campaña problem update PRB-id --course /ruta/curso \
  --revision 3 --state en-trabajo --step diagnostico --note 'Revisando la guía'
```

La revisión es obligatoria para editar registros existentes. Una escritura sobre una
revisión vieja se rechaza; la anotación y su deshacer se coordinan con los demás escritores
del curso. Cada paso queda en el historial. `update` usa el actor `agent` por omisión.
`--actor operator` se usa para decisiones humanas, incluido Resuelto.

## Entrega

El agente adjunta un JSON mediante `--artifacts /ruta/entrega.json`:

```json
{
  "changes": [{
    "path": "ruta/guia.md",
    "version": "sha256:hash-de-los-bytes-del-archivo",
    "summary": "Explica el contraste general",
    "scope": "judge"
  }],
  "results": [{
    "caseId": "CAS-id",
    "detected": true,
    "fixed": true,
    "version": "version-efectiva-probada",
    "modelConsulted": false
  }]
}
```

`scope` puede ser `judge`, `corrector` o `rule`. `detected` indica detección del problema;
`fixed`, corrección del contenido. Se puede registrar sólo la medición realizada. Si
`modelConsulted` es `true`, el resultado exige `provider`, `model` y `runtimeVersion`
efectivos. No se deben inventar resultados ni usar versiones sugeridas como efectivas.

La inspección compara cada `version` con el SHA-256 de los bytes actuales del archivo,
con el prefijo `sha256:`. Un commit o un alias no sustituye esa huella. Si cambió un
archivo o la versión efectiva del juez, la pantalla identifica la diferencia y bloquea
la ejecución. Se debe revisar el cambio y registrar una entrega nueva mediante
`problem update --artifacts`; las mediciones anteriores conservan su procedencia como
historial y no certifican los archivos nuevos.

Para probar exige una entrega que pase. Si se cambió un juez, debe evaluar todos los
casos Examen y Está bien, con al menos un positivo y un negativo. Un falso positivo,
un problema no detectado o una corrección fallida deja el trabajo En trabajo. Resuelto
lo decide el operador después de una entrega Para probar.

El tablero ofrece **Probar la solución** desde la ficha: contrastes, examen fijo,
medir, registrar y reanalizar, en ese orden. Un solo diálogo muestra el costo completo,
el juez, proveedor, modelo y versión efectivos, y un tope para las cinco etapas.
Medir y registrar no consultan modelos. El servidor guarda la autorización y el avance;
la prueba sigue al cerrar la página y se recupera al reiniciar el tablero. Las corridas
se ven también en Ejecuciones. **Frenar** termina el tema en curso y guarda lo medido;
**Seguir** retoma lo pendiente con el saldo autorizado, sin repetir los temas terminados.
Las actualizaciones del historial no invalidan la cuenta; cambiar el juez, el oro o el
contenido exige revisar una cuenta nueva.

Un contraste con otro veredicto frena antes del examen fijo. Las reglas se comparan
normalizando mayúsculas y guiones bajos; `OVERSTATED_FOR_QUIZ` y `overstated-for-quiz`
son la misma regla, pero un cambio de veredicto o gravedad sigue siendo una diferencia.
El resultado muestra las dos vallas, diferencias ordenadas por gravedad y lo esperado
plegado. Cada caso conserva qué se evaluó, qué se esperaba y qué dio, con explicaciones,
señales de revisión del oro y procedencia. JSON y comandos quedan plegados.

El operador puede confirmar el oro, adoptar el veredicto del juez o excluir un caso
como dudoso. Revisar el oro fijo requiere una nota y conserva la etiqueta anterior;
medir de nuevo es gratis. Registrar y reanalizar usan la misma autorización. Un
reanálisis incompleto no habilita el cierre. **Marcar resuelto** requiere un control
**Está bien** elegido por el operador; elegir un caso sano ya medido conserva la entrega
sin otra consulta. El servidor avisa al terminar o cuando hace falta revisar.

La ficha muestra **Ver cambios del juez**, comparando el último commit con los archivos actuales,
y advierte si sus hashes ya no coinciden con la entrega. `inspect` sólo lee archivos del proyecto.
Una entrega puede adjuntar `judgeName` y `nextSteps` (id, title, detail, command, budget,
expectedJudgeVersion, requiresTakenExam, requiresApprovedExam y modelConsulted). Las cinco etapas aparecen en el recuadro **Probar la solución**; los comandos se
conservan en **Ver los comandos**, sin ejecutarlos al abrirlos. La reauditoría queda
bloqueada hasta que el runtime habilite la versión actual.

`exam status -f json` incluye la situación `current` de los jueces de agentes de la ronda 1.
La pantalla conserva el historial, pero una aprobación de otra versión no habilita el juez actual.
Las lecturas se actualizan cuando cambian las definiciones o los registros del examen.

### Ejecución desde la ficha

Un paso ejecutable adjunta `execution`, separado de su `command` opcional:

- `type: exam`, `mode: focused|historical`, `casesPath`, `coursePath`, `goldPath`.
- `type: measure`, `steps`: ids de las corridas que debe comparar.
- `type: record`, `steps`: id de la medición revisada por el operador.
- `type: analyzer`, `coursePath`: propuestas congeladas; exige examen aprobado.

La API valida la revisión, hashes de la entrega, versión/runtime actual, oro y dependencias
al preparar y antes de lanzar. El navegador manda sólo id, paso, huella, tope e id de pedido.
Nunca manda un programa o argumentos ejecutables. Las salidas tienen rutas únicas y no se
sobrescriben. El mismo id de pedido recupera la corrida anterior incluso tras reiniciar.
El oro queda fuera del pedido al juez; la medición del histórico conserva los umbrales
del oro canónico v4. La aprobación requiere también pasar los contrastes específicos.

La ficha actualiza progreso y resultados automáticamente. Una corrida incompleta o con
procedencia mezclada no habilita registrar una aprobación. Los controles GOOD de la ficha
los sigue eligiendo el operador. El botón de cierre registra la entrega completa como
Para probar y sólo entonces permite al operador marcar Resuelto.
