# Estabilidad de opiniones al agregar una conversión

Verificación local: 23 de octubre de 2026.

## Reproducción y causa

Agregar la propuesta de conversión de «He closed the door hard.» cambió la
tanda A1 de versión 8 a 9. El lector dejó de usar el análisis semilla completo
porque su número de versión ya no coincidía; al reconstruir sin consultas se
perdían de la vista opiniones guardadas sobre contenido que seguía igual.
Los archivos originales y las decisiones humanas seguían presentes.

Además, la compuerta de herramientas locales no reconocía las equivalencias
explícitas del proceso de Ejercicio casi repetido. El tablero tampoco
representaba como pendiente ese juez cuando sólo estaba declarado en la
cobertura, por lo que esos ejercicios desaparecían de las listas.

## Criterios y corrección

- Conservar una opinión guardada sólo cuando coincide la huella exacta de la
  entrada que juzga. Para jueces de tema, comparar todos sus ejercicios.
- No sustituir una opinión actual ya guardada; conservar también las
  abstenciones, su carácter pendiente y la procedencia histórica.
- Una versión o fecha desconocida no se completa con la del juez actual.
  Sólo las versiones con mediciones son evidencia de procedencia.
- Reconocer las equivalencias ya declaradas del proceso únicamente con el
  proveedor, modelo y esfuerzo explícitos correspondientes.
- Mostrar los jueces pendientes declarados en cobertura para cada ejercicio;
  un pendiente nunca acredita consenso ni habilita entrega.
- Abrir el catálogo, una sugerencia o su reintento sólo lee. La escritura
  exige «Agregar conversión a la tanda», control de versión y una única
  operación incluso con doble clic.

## Pruebas ejecutadas

Se exportaron las bases `a42b02d44` de ContentAudit y `6ec8d5ab4` de ITTM
a `/tmp/learney-filter-fix-io31fo2n` y se incorporaron únicamente los diez
archivos Java y los siete archivos del tablero enumerados en su
`manifest.json`. Se excluyeron los cambios ajenos de miniteoría y exámenes.
El séptimo archivo del tablero conserva el movimiento del panel de jueces
debajo de la tarjeta central solicitado anteriormente.

| Comprobación sobre la combinación aislada | Resultado |
|---|---:|
| Reactor Java: course, evaluation-ledger, audit, refiner y revision | 2.532 pasan |
| Dart: clasificación de ejercicios y cobertura de jueces | 15 pasan |
| JavaScript: conversión, filtros, gestos y opciones con/sin cambios | 94 pasan |

Sin fallos, errores ni omisiones. Registros: `/tmp/mc-filter-isolated-java.log`,
`/tmp/mc-filter-isolated-dart.log` y `/tmp/mc-filter-isolated-js.log`.
Las nuevas regresiones reprodujeron el fallo antes de corregirlo; entre ellas,
una conversión en un tema deja pendientes sólo las entradas afectadas y
conserva las opiniones de otro tema. Se comprueban abstenciones, resultados
actuales prioritarios, procedencia antigua e historial sin escrituras.

La validación amplia encontró además un caso existente de calidad inglesa
con versión explícita en metadata. Se conserva esa identidad antes de crear
procedencia; una procedencia con versión desconocida sigue siendo autoritativa.
Una revisión independiente confirmó este ajuste y los límites del reuso.

## Lectura real del tablero

La copia de sólo lectura de la campaña real conserva la tanda abierta en
versión 9 y sus 1.769 correcciones. En pantalla recupera 84 ejercicios en
«Todos los jueces lo aprueban» y 185 en «Preparados para entregar».
Las 211 aprobaciones humanas se distribuyen entre los 185 preparados y los
26 de «Vos lo aprobás». Hay 50 en «Faltan opiniones».

Después de actualizar el lector del puerto 7800 se verificaron los mismos
contadores en el tablero habitual. Se abrió y cerró una sugerencia de
`lovely / lovelily`: el navegador emitió únicamente GET, mostró el botón
explícito para agregarla y conservó los contadores. El archivo de la tanda
conserva exactamente su SHA-256 previo y la versión 9. También se comprobó
que el panel de jueces aparece después de la tarjeta central.

No se ejecutaron jueces ni exámenes para recuperar esas opiniones. No hay
migración de datos ni reescritura de aprobaciones: la recuperación ocurre
al leer. Las conversiones existentes conservan el contrato de producción.

## Reversión

Revertir sólo los archivos de esta corrección. No hay datos del curso ni
historial de evaluaciones que deshacer. Conservar los cambios ajenos del
worktree y el movimiento previo del panel central.

## Rectificación: aprobaciones humanas sin correcciones

El operador señaló que había revisado casi toda la lista y reaparecían
ejercicios. La comprobación anterior de 211 decisiones sólo acreditaba las
decisiones de correcciones reconocidas por la vista; no demostraba que se
reconocieran también las aprobaciones de ejercicios sin correcciones.

Se encontró otro defecto en `clavesDeAprobacion`: la clave persistida incluía
`contentVersion` global además de tanda, revisión, quiz y contenido visible.
El salto 8 → 9 invalidaba el reconocimiento de todos esos registros.

Auditoría de una copia de 187 aprobaciones, sin modificar el archivo real:

- 76 aprobaciones de versión 8 coinciden exactamente con el contenido visible
  actual. Sus fechas van de 02:13 a 02:53 UTC del 23/10.
- Los mismos 76 ejercicios fueron aprobados otra vez en versión 9 entre
  12:02 y 12:06 UTC. Son revisiones repetidas por el defecto de identidad.
- Las otras 35 claves también fueron identificadas: 17 CLOZE cuya vista
  antigua tenía `s=''`, y 18 MC cuya vista antigua tenía `mc=[]` y `s=''`.
  Esas vistas incompletas no equivalen a la vista completa actual; no se
  transfieren como aprobación de un contenido visible distinto.

La reconstrucción independiente de las 918 claves actuales coincide 918/918
con el servidor. El detalle está en `/tmp/mc-approval-history-audit.json` y
la copia previa en `/tmp/mc-approval-before-stability-fix.json`. El operador
siguió revisando durante la investigación: los números de esta captura no
deben interpretarse como un contador final inmutable.

Criterios de la corrección adicional: identidad estable por contenido visible
dentro de la misma tanda/revisión; reconocer las claves anteriores sólo por
coincidencia exacta de todas sus entradas; lectura sin migrar ni cambiar fechas;
reabrir retira todas las claves compatibles para evitar resurrecciones; una
escritura desde una versión vieja sigue rechazándose.

La corrección conserva el contrato de F-NADIE-R006: tanda, revisión y contenido,
sin acoplar la aprobación al número global de versión. Agrega compatibilidad
de lectura exacta con las claves históricas y mantiene la validación de versión
antes de aceptar un POST. Una reaprobación compatible no escribe de nuevo;
reabrir retira todas las claves compatibles del mismo contenido en una sola
escritura serializada. Ninguna lectura modifica el registro de aprobaciones.

Verificación de esta etapa:

- Las regresiones nuevas reprodujeron cuatro fallos antes de corregir:
  `/tmp/quiz-aprobaciones-identidad-red.log`.
- Pasan las 23 pruebas del registro y la sesión de aprobación. Cubren cambio
  de otro quiz, cambios del contenido propio, clave histórica independiente,
  versión futura no aceptada, autor y fecha originales, rechazo 409 de vista
  obsoleta, reapertura sin resurrección y fallos de escritura.
- En la combinación aislada actualizada (`manifest-stage2.json`), pasan las
  **1.300 pruebas Dart completas del tablero** y las **94 de interfaz** del
  recorrido afectado. Logs: `/tmp/quiz-aprobaciones-isolated-dashboard.log`
  y `/tmp/quiz-aprobaciones-isolated-ui.log`.
- Dos revisiones independientes no encontraron hallazgos pendientes. Los diez
  archivos de cada repositorio en el manifiesto coinciden byte por byte con
  los probados; no se incorporaron cambios ajenos.
- La copia real de sólo lectura reconoce **76/76** registros con su autor y
  fecha original. El tablero anterior reconocía sus reaprobaciones nuevas y
  **0/76** con la fecha original. Las copias de ambas respuestas están en
  `/tmp/quiz-aprobaciones-live-before.json` y
  `/tmp/quiz-aprobaciones-shadow-after.json`.

Esto corrige el reconocimiento perdido y evita repetir revisiones al cambiar
otro ejercicio; no revierte el tiempo que el operador ya tuvo que dedicar a
las 76 reaprobaciones.

Verificación final en el puerto habitual 7800: reconoce las **76/76**
aprobaciones con fecha/autor originales, y el archivo persistido conserva
exactamente sus bytes anteriores al reinicio. La tanda sigue en versión 9.
Después de las últimas decisiones del operador, la pantalla muestra 5 en
«Todos los jueces lo aprueban», 264 preparados, 26 en «Vos lo aprobás» y 50
con opiniones pendientes. La comparación anterior/posterior conserva el
mismo conjunto de 76 quizzes sin correcciones: el arreglo recupera su
procedencia original; las reaprobaciones que el operador ya realizó también
siguen guardadas. No se atribuye al arreglo el trabajo manual que ya repitió.
