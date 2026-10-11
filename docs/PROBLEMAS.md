# Problemas del curso

Implementación inicial de `docs/propuestas/problemas.html`. El tablero HTML está en
`ittm/pipeline/vocabulary_dashboard`; el registro y los comandos están en ContentAudit.

Desde Correcciones, el botón **Problema** y el atajo **P** guardan el antes y el después,
el comentario, la expectativa opcional y la procedencia. Se puede elegir un problema
existente o crear uno. Por omisión se rechaza la corrección; la opción se puede quitar y
la anotación se puede deshacer mientras la corrección y la decisión sigan siendo compatibles.
Las decisiones pendientes en la cola deben guardarse o descartarse antes de anotar.

Cada curso tiene su registro en `<curso>/.content-audit/problems/`. No pertenece a la
tanda: las campañas del mismo curso comparten los problemas. Los casos son snapshots
congelados y sobreviven a cambios en la campaña. Un caso nuevo reabre el problema y
retira la entrega anterior.

La pantalla **Problemas** permite editar el criterio, buscar textos o expresiones
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
    "version": "sha256-o-commit",
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

Para probar exige una entrega que pase. Si se cambió un juez, debe evaluar todos los
casos Examen y Está bien, con al menos un positivo y un negativo. Un falso positivo,
un problema no detectado o una corrección fallida deja el trabajo En trabajo. Resuelto
lo decide el operador después de una entrega Para probar.

Esta primera implementación registra las evaluaciones entregadas por el agente. La
ejecución de exámenes y nuevas vueltas con un tope desde la ficha todavía queda por
integrar con el lanzador del tablero. Estos comandos no ejecutan consultas ni publican
contenido.
