# Ciclo sintético juez → corrector → jueces

Autorización humana: «sí» al plan079. Tope nuevo e independiente:50 lanzamientos físicos, con reintentos incluidos. Se gastaron43; no se autoriza otra corrida con el remanente.

Resultado:16/16 diagnósticos iniciales compatibles;7/7 controles válidos intactos;9/9 propuestas pasan calidad, unicidad y reglas locales. Inspección local en review.json; no es aprobación humana, oficial del juez, ni del curso. Los errores de escritura propuestos en7 y9 merecen calibración pedagógica.

## Procedencia

- codex-cli, sesión local ChatGPT, gpt-6.1-sol, medium; sin fallback.
-25 juicios de calidad,9 de respuesta única y9 propuestas;43 llamadas físicas, sin reintentos.
- Corpus y versiones congelados antes de gastar; preflight.json registra huellas efectivas. El16 es un control nuevo de significado; no se modifica el examen histórico ni se corren sus2 casos adicionales.
- Se usa el analizador nativo, recolector de hallazgos, planificador, tareas y adaptador del corrector. No se inyectan instrucciones correctivas escritas por el arnés.
- Ambos jueces finales reciben sólo la propuesta visible. Expectativas, diagnóstico previo y argumento del corrector quedan fuera de su entrada. La marca de clave es parte del contrato de auditoría.
- Los controles positivos sólo reciben la evaluación inicial. La segunda evaluación y unicidad son para las9 correcciones.
- El renderizador nativo separa un punto después del hueco en el13; el contenido lingüístico se conserva.
-17 tests Java,5 pruebas Python y prueba de integración nativa con evaluador falso aprobados. Ninguno consultó modelos.

## Evidencia y conservación

result.json y case-NN/ contienen entradas, devoluciones, hallazgos, tareas y propuestas reales. physical-calls.jsonl conserva cada lanzamiento antes de ejecutarlo. raw-evidence.tgz preserva los34 juicios consolidados y todos los grafos reales, incluidos los9 del corrector. preservation.json registra los contadores y huellas sin cambios de la campaña y del examen anterior.

Los estados AUTOMATIC_CHECKS_PASS_REQUIRES_CONTENT_REVIEW originales no se reescriben. review.json añade la inspección de contenido, sin simular una aprobación. No se publica, aplica o aprueba ninguna propuesta.

## Ejecución y recuperación

El arnés está en scripts/exams/ChoiceCorrectionCycle.java y el lanzador en run_choice_correction_cycle.py. Por defecto sólo sus modos preflight/selftest consultan cero modelos; run exige esta autorización, el tope durable y las versiones congeladas. Hay bloqueo global y por caso. Una etapa iniciada sin resultado sólo recupera juicios consolidados: no repite una llamada incierta ni renueva presupuesto. No se permite volver a congelar después de gastar.

