# Audio como etapa de prepublicación · 24/10/2026

## Resultado
CONTENT excluye los controles de audio del análisis, de la selección y contexto de corrección y de la comparación. Conserva coherencia de la oración guardada y barras en respuestas. Los reportes guardados se proyectan sin alterar sus archivos ni presentar el audio omitido como resuelto. PREPUBLICATION conserva los controles de registro de audio y sus textos finales, con presupuesto cero.

Publicar incorpora «Audios · antes de publicar»: distingue renovar/evaluar, problemas del registro y registro coincidente con sonido sin evaluar. Los pedidos guardados de la bandeja se identifican como históricos, distintos del estado actual. Las lecturas alternativas quedan plegadas.

No se añadió generación ni certificación sonora automática. No se generó audio, no se publicaron cambios y no se hicieron consultas reales en esta entrega. La integración local existente con pedidos pendientes conserva su contrato.

## Verificaciones ejecutadas
- Línea de base antes de editar: 60 pruebas focales, sin fallos (`baseline.log`).
- Versión final integrada: 186 pruebas de Java, 20 clases, cero fallos/errores/omitidas; compila todos los módulos dependientes y CLI (`verified-tests.log/json`).
- UI final: 111 pruebas JS, 52 Flutter y análisis estático sin problemas. Golden de Publicar sin diferencias; sólo se actualizó su bloque (`ui-evidence.md`).
- Dos revisiones independientes. Se corrigieron registro actualizado frente a pedido antiguo, claves compartidas entre niveles y uso incorrecto del token de caché. Las pruebas mantienen assertions de comportamiento y compatibilidad.
- Lectura real del comando `batch audio` y del análisis vigente de C2: 2.402 ejercicios; 208 requieren renovar/evaluar, 2.194 registros coinciden. Entre los diez, Peter y tea requieren audio; ocho registros coinciden. Coincidir no aprueba el sonido (`selected-ten-prepublication.json`).
- El análisis de contenido vigente contiene cero incidencias de audio y conserva el problema de la oración guardada de Peter (`content-read-checks.json`).
- Navegador real: navegación Corrección 2 → Publicar, apertura/cierre del panel, estados y texto final, recarga y Releer; captura `publicar-audios.png`. No se accionó publicar.
- C1 y C2 conservan exactamente los bytes de sus borradores (`real-read-integrity.json`). El piloto sigue en 275/1.000 consultas.
- Reproyección nativa y gratuita de los informes pareados anteriores: 12→7 observaciones de contenido; cinco mejoran parcialmente, una empeora y cuatro no cambian. El audio retirado no cuenta como arreglado. No hay aprobación completa certificable porque un juez sigue bloqueado (`projected-ten-summary.json`). No existe una referencia humana certificada.

## Límites de la verificación general
La suite general hasta audit-infrastructure ejecutó 2.997 casos: un fallo de validación de excepciones de habilitación de jueces y una prueba omitida; no se declara verde. El fallo corresponde a archivos con cambios previos ajenos al audio, no editados en esta entrega. La suite general JS mantiene dos fallos ajenos y diferencias de otras pantallas; se preservaron esos cambios. Véanse `general-tests-limit.md` y `ui-evidence.md`.

Los primeros intentos fallidos se conservaron por trazabilidad. Los archivos con «verified» y los logs «ui-publicar-*-final» documentan la versión final, no los intermedios.

## Integración y reversión
Worktree compartido y con muchos cambios previos, registrados en dirty-before.txt. No se usó Sentinel ni se regeneró código. La reversión debe retirar sólo los cambios de esta etapa; los cachés son descartables. No hay efectos de TTS o publicación que compensar.
