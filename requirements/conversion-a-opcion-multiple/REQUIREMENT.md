# FEAT-CONVERSION — Pasar un cloze a opción múltiple

Fuente: [propuesta de producto](../../docs/propuestas/convertir-a-opcion-multiple.html).

Primera entrega: opciones existentes en el ejercicio o en sus hermanos del mismo conocimiento. La generación de distractores queda pendiente del examen explícito indicado en la propuesta.

## F-CONVERSION-R001 — Sugerencia conservadora

- Un solo hueco y una respuesta breve. Reescrituras, conjuntos ambiguos, opciones repetidas y oraciones guardadas inconsistentes quedan fuera, con motivo visible al pedirlo manualmente.
- Copiar entre dos y ocho opciones sin inventar, conservando su orden. La primera respuesta aceptada sigue siendo la única correcta.
- Las pistas de forma base sólo se convierten a pedido. Excluir ejercicios con otra corrección o una conversión rechazada.
- Mostrar cantidades actuales por tema y procedencia, sin fijar las cifras estimadas de la propuesta.

## F-CONVERSION-R002 — Revisión indivisible

- Crear una corrección que lleva tipo, formulario y consigna juntos. Mostrar antes/después, procedencia y vista del alumno.
- La aprobación requiere mediciones completas de validez y distractores para la versión y los contenidos exactos. La ausencia de hallazgos sin medición no alcanza.
- Pedir jueces usa el flujo existente de examen, proveedor efectivo y presupuesto explícito. Detectar y sugerir no consulta modelos.
- Rechazar o volver a Completar antes de publicar retira toda la conversión.

## F-CONVERSION-R003 — Conservación y recuperación

- Guardar el cloze completo en `clozeOriginal`, incluidos sus atributos no modelados. Mantener oración guardada, traducción, miniteoría y audio.
- Después de publicar, restaurar crea otra corrección explícita y conserva la copia original.
- Si otro atributo cambió después de convertir, detener la restauración para no sobrescribirlo.

## F-CONVERSION-R004 — Publicación atómica

- Una transición aprobada publica `kind`, `form`, `instructions` y `clozeOriginal` mediante una sola comparación y escritura por documento.
- Verificar el antes de todos los campos, incluidos los que no cambian. Conflictos y transiciones parcialmente aplicadas no se pisan.
- Repetir una publicación ya aplicada no escribe otra vez. Rechazar cambios arbitrarios de tipo, copias incompletas y modificaciones del enunciado ajenas a quitar la pista.

## Verificación

Pruebas de detección, servicio y aprobación en `revision-domain`; roundtrip de la copia en `course-infrastructure`; manifiesto Java y ciclo con destino falso en `scripts/production_publish/tests/test_conversion.py`; rutas Dart y recorridos de interfaz en el tablero de ITTM. Ninguna necesita consultas reales ni acceso a producción.
