# Opción múltiple: conservación del contrato de producción

Fecha local: 22 de octubre de 2026.

## Versión y alcance

- `content-audit`: base `e9ab0d82b4887ff6cd585018d578bcc5fcad3cb1`.
- `ittm`: base `432c3e4b31cba1f9fd8232f25772d98ccbea2027`.
- Se conservaron los cambios locales previos, incluidos los del tablero y los de exámenes/miniteoría.
- Entrega sin commit: conversor independiente, extractor de curso, lector de evaluaciones y sus pruebas de integración.

La autoridad es el documento de producción: `form.items`, `form.selection`,
`form.sentenceParts`, `form.sentences`; las opciones conservan `id`, `label` e
`incidence`. Los adaptadores internos de Java no cambian ese contrato persistido.

## Criterios fijados antes de modificar

1. Conservar opciones, orden, IDs, incidencias, respuesta correcta y todas las oraciones guardadas.
2. No perder atributos adicionales ni la copia original; proyectar/extraer no modifica la entrada.
3. Conversión y relectura producen el formato de producción. Un snapshot anidado no se acepta como documento de producción.
4. Repetir no escribe de nuevo; cambios concurrentes no se sobrescriben ni cuentan como escritos.
5. Recorrer conversor real → escritura simulada → extractor real → exportación/relectura BSON/JSON → inputs reales del juez.
6. Agregar cobertura de las entradas Java actuales, incluyendo rechazo seguro cuando una conversión altere las oraciones originales.

## Línea de base y cambios

- El extractor tenía 75 pruebas existentes verdes en modelos/servicios/cache. Las 7 regresiones iniciales fallaban: perdía opciones, oraciones y campos adicionales.
- El lector tenía 7 casos nuevos con fallos antes de la corrección, incluidos Mongo Extended JSON, prioridad del curso y CLOZE con campos residuales.
- El conversor perdió `sentences` y atributos del formulario/partes en la reproducción. Las pruebas nuevas también expusieron ausencia de control de concurrencia.
- El recorrido integrado falló antes del arreglo del conversor; luego conservó documentos completos, opciones y sus respuestas.
- Una revisión independiente detectó dos defectos de la primera versión del extractor: MC anidado aceptado y campos ausentes exportados como `null`. Ambos se corrigieron y se agregaron regresiones usando exportación y cache reales.

## Verificación ejecutada

Entorno temporal: Python 3.14, Pydantic 2, PyMongo 4, jsonschema 4; para pruebas de extractor, pytest 9. Dependencias existentes del proyecto reutilizadas en Java con JDK 25.

| Control | Resultado |
|---|---|
| `python3 -m unittest discover -s be/scripts/tests -v` en `ittm` | 8 pasan |
| `python -m pytest extract/full_course/tests` en `ittm/be/tools/pipeline` | 114 pasan |
| `python3 -m unittest discover -s .sentinel/agents/quiz-instruction-validator/eval -p 'test_student_view_inputs.py' -v` | 7 pasan |
| `python -m unittest discover -s scripts/contract_tests -v` | 5 pasan; recorre además los 2.810 MC guardados |
| `PYTHONPATH=scripts python3 -m unittest discover -s scripts/production_publish/tests -t scripts` | 202 pasan |
| Entradas Java de conversión → producción y regresiones relacionadas | 67 pruebas focalizadas pasan; incluye ambas entradas reales y rechazo de pérdida de variantes |
| `bash scripts/test-multiple-choice-contract.sh` sobre la combinación final | Código 0: 336 pruebas Python + 41 Java = 377; sin fallos ni omisiones |

Registros locales de esta ejecución: `/tmp/mc-converter-red.log`, `/tmp/mc-converter-final.log`, `/tmp/mc-e2e-red.log`, `/tmp/mc-e2e-final.log`, `/tmp/mc-harness-final.log`, `/tmp/mc-extractor-final.log`, `/tmp/mc-publication-full.log`, `/tmp/mc-contract-all-final.log`.

La ejecución integrada usó `CONTRACT_PYTHON=/tmp/content-audit-mc-contract-venv/bin/python`,
`ITTM_ROOT=/Users/josecullen/projects/learney/ittm` y JDK 25 en `JAVA_HOME`.
Los tests Java usan sólo una dependencia adicional de `course-infrastructure` con
alcance `test`, sin ampliar dependencias de ejecución del adaptador del panel.

## Revisión y límites

- Conversor y pruebas integradas: revisión independiente sin hallazgos pendientes tras corregir la lectura de exportaciones Mongo y ampliar las comprobaciones del enunciado/consigna/contexto.
- Extractor: segunda revisión independiente confirma resueltos los dos hallazgos; 14 regresiones del archivo revisado pasan.
- Java: revisión del coordinador y una revisión independiente final sin hallazgos pendientes. El caso positivo del panel usa `DefaultPanelRunnerFactory`, no reproduce manualmente la aplicación del candidato. La comparación de oraciones ocurre después de la aplicación real.
- La regresión del panel reprodujo `ACCEPTED` con pérdida de una variante antes del cambio. La guardia específica de conversión ahora devuelve `NO_WINNER` con motivo explícito, sin guardar el candidato ni modificar el original. La entrada de opciones existentes también rechaza variantes incompatibles sin escritura.
- Se sustituyó MongoDB por una colección que reproduce comparación del original antes de escribir. No se consultaron modelos ni se escribieron datos en una base de producción.
- El contrato verificado del esquema es el de cada quiz, no el de todos los DTO de jerarquía del curso.

## Reversión

Revertir únicamente los archivos de esta entrega, conservando los cambios previos de otros trabajos. No hay migración de datos reales que deshacer. El formato de producción no cambia; se corrigen sus consumidores/productores y se rechazan entradas incompatibles.

Decisión: criterios satisfechos en la combinación local verificada. Las tres rutas y las entradas Java cubiertas conservan el contrato de producción o rechazan la conversión antes de escribir. La validación contra una base real de producción está fuera de este control offline.

## Regresión de versionado de Consigna

Pedido posterior del 22/10: las pruebas y sus lectores auxiliares no deben invalidar el examen si el juez efectivo no cambió.

- Se reprodujo el error: editar `eval/` cambiaba la versión (1 fallo de 5 antes del arreglo, `/tmp/consigna-version-red.log`).
- La versión actual excluye únicamente `eval/`; definición, prompts, esquema y scripts siguen contando. El algoritmo histórico acoplado al motor no se modifica.
- Se comprobó desde los bytes que los 7 archivos efectivos son idénticos a HEAD. La versión aprobada `533462b9b23f613d16368f934b397056f726c65111c2821ad0112e1f85f9f0fe` y la actual tienen el mismo proceso `67dc36e1772ce26a5548de4ebb91b8c378684f2e3839e9ee373cfb1ed2169dac` sin `eval/`.
- La equivalencia exacta está en `.sentinel/agents/quiz-instruction-version-equivalences.jsonl`, separada del corte global anterior. Sólo se reconoce su primer lote; otro proceso no hereda la aprobación.
- El puente distingue equivalencia de proceso de alias histórico acoplado al motor. Se reprodujeron 2 fallos de reuso indebido sin motor antes del arreglo (`/tmp/exam-process-alias-red.log`); se conservan los resultados con motor explícito coincidente.

Verificación offline con JDK 25 sobre la combinación final:

| Control | Resultado |
|---|---|
| Resolver y factory de Consigna; resolver y corte genéricos | 21 pasan (`/tmp/consigna-version-green.log`) |
| Módulo completo `evaluation-ledger-domain` | 51 pasan (`/tmp/exam-process-alias-green.log`) |
| `ConsignaVersionStandingIntegrationTest` | 8 pasan; factory, registro persistido y estado reales; corredor simulado |

La integración cubre edición/alta/baja de pruebas y lectores; cambios efectivos de prompt/script/esquema/definición; cambios de proveedor/modelo/esfuerzo; intento de agregar una equivalencia posterior; historial intacto y cero consultas. Revisión independiente sin hallazgos pendientes.

Se verificó el tablero ejecutable en `/examenes.html?juez=quiz-instruction`: «Aprobado», «Versión actual habilitada» y aprobación original del 5/10 en hora local, con `codex-cli / gpt-6.1-sol / medium`. La API confirma `APPROVED`, `consultsOnCourse=true` y `blocksClose=false`. Los archivos históricos de examen y de veredictos conservan exactamente sus bytes. No se rindió otro examen ni se consultó un modelo.

Reversión: retirar sólo este cambio de versionado y su archivo de equivalencia. No hubo reescritura de aprobaciones ni migración de datos del curso; el corte anterior y los cambios ajenos se conservaron.

## Integración solicitada el 22/10

Se exportaron las ramas principales vigentes (`content-audit` en `e9ab0d82b`, `ittm` en `432c3e4b3`) a una carpeta aislada y se incorporaron únicamente los 24 archivos de esta tarea en ContentAudit y los 9 de ITTM. Se dejaron fuera los cambios ajenos de miniteoría y exámenes permanentes. Las referencias remotas coincidían con esas bases.

Sobre esa combinación, sin depender de los cambios ajenos, pasaron **466 pruebas**: 336 Python, 121 Java (41 del contrato de conversión y 80 del versionado/reuso), 5 Dart y 4 de la interfaz JavaScript. No hubo fallos ni pruebas omitidas. Se reutilizaron las dependencias locales existentes.

Registros de integración: `/tmp/learney-merge-contract.log`, `/tmp/learney-merge-versioning.log`, `/tmp/learney-merge-dart.log`, `/tmp/learney-merge-ui.log`. El control de pre-commit de ITTM también pasó. La integración local de ITTM quedó en `6ec8d5ab4` sobre `master`. Se verificaron los hashes de los archivos ajenos para conservarlos intactos.
