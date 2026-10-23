# Contrato de opción múltiple de producción

La fuente válida es el documento persistido de producción: `form.items`,
`form.selection`, `form.sentenceParts` y `form.sentences`. Cada opción conserva
`id`, `label` e `incidence`. Las entidades Java y las propuestas de los agentes
tienen adaptadores; su representación interna no se escribe como formulario del curso.

## Ejecutar

Se necesitan los checkouts de `content-audit` e `ittm`. Por defecto se busca
`ittm` junto a `content-audit`; `ITTM_ROOT` permite elegir su ubicación.

```sh
python3 -m venv /tmp/mc-contract-venv
/tmp/mc-contract-venv/bin/python -m pip install -r scripts/contract_tests/requirements.txt
ITTM_ROOT=/ruta/a/ittm /tmp/mc-contract-venv/bin/python -m unittest discover -s scripts/contract_tests -v
```

Para ejecutar los controles de las tres rutas y las integraciones Java juntos,
con JDK 25 configurado en `JAVA_HOME` y las dependencias Maven locales disponibles:

```sh
CONTRACT_PYTHON=/tmp/mc-contract-venv/bin/python ITTM_ROOT=/ruta/a/ittm bash scripts/test-multiple-choice-contract.sh
```

La suite ejecuta el conversor real, sustituye MongoDB por una colección con
comparación del original antes de escribir, ejecuta `FullCourseExtractor`,
exporta y relee BSON/JSON y construye los inputs reales del juez. No invoca modelos.

Comprueba opciones y su orden, IDs distintos de las etiquetas, incidencia y
respuesta correcta, todas las oraciones guardadas, metadatos y copia original.
Incluye lectura de exportaciones Mongo Extended JSON, ejecución en seco,
repetición sin escrituras y conflicto por edición concurrente. El JSON del quiz
se valida contra el esquema del extractor; la jerarquía del curso tiene un contrato aparte.
Además recorre cada opción múltiple presente en `db/english-course`.

Pruebas complementarias:

```sh
# En ittm:
python3 -m unittest discover -s be/scripts/tests -v
cd be/tools/pipeline
/tmp/mc-contract-venv/bin/python -m pytest extract/full_course/tests

# En content-audit:
python3 -m unittest discover -s .sentinel/agents/quiz-instruction-validator/eval -p 'test_student_view_inputs.py' -v
```

Los escenarios de Mongo son simulados. La suite no escribe en una base real.
