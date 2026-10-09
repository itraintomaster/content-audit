# Examen inicial de calidad de distractores

Corpus sintético congelado antes de ejecutar: 16 casos centrales (8 con defecto,
6 con distractores útiles y 2 prácticas introductorias legítimas) y 2 controles
de falta de contexto. No copia los ejemplos del prompt ni reutiliza su fixture.

El juez sólo recibe el curso y los identificadores: gold.json queda fuera del
contexto. Se usa el camino exam run, con libro propio del examen y sin guardar
un análisis del curso real. Los controles de abstención corren separados:
NEEDS_CONTEXT debe conservarse sin puntaje ni cobertura aprobada.

El límite conjunto es 18 evaluaciones y 18 ejecuciones físicas de Codex, contando
antes de lanzar cada proceso. Si una validación necesita otra llamada, consume
este mismo límite y puede dejar casos pendientes: no se agrega saldo ni se
relanza automáticamente.

Una primera pasada mide este corpus; no demuestra estabilidad ni eficacia en
alumnos. No se aprueba el juez automáticamente ni se modifica el prompt con
respuestas del examen.

## Incidente de entrega del contexto

La primera versión omitía la sección User prompt template: los parámetros
existían en el estado pero el modelo no recibía sus valores. Se detuvo la pasada
tras 16 lanzamientos físicos: 15 respuestas consolidadas sin contexto y una
llamada en vuelo sin resultado consolidado, que también consume el tope.
No son 15 errores pedagógicos medidos ni cobertura válida.

La corrección de la plantilla cambia la versión efectiva. La prueba local
ChoiceQualityPromptDeliveryTest recorre el grafo real con un modelo simulado y
verifica los seis valores en el mensaje efectivamente enviado. Falló antes de
la reparación y pasó después; nunca llama a Codex.

repair-smoke-cases.json selecciona un caso con defecto y uno válido del corpus
ya congelado. Sólo puede usar las dos llamadas restantes del mismo límite
físico de 18, sin reiniciarlo. Es una comprobación de integración, no sustituye
el examen de los 16 casos centrales y dos controles de contexto. Los registros
de la versión anterior se conservan, pero no se reutilizan como versión nueva.

## Resultado conservado

Los dos casos de integración de la versión reparada coinciden con el oro: uno
FAIL con ANSWER_CUE y uno PASS sin alarmas. El mensaje real guardado confirma
la entrega de los seis datos. El registro queda TAKEN, goalMet=false; no se
registró APPROVED. El corpus completo de esta versión aún no rindió.

El tope terminó en 18/18 lanzamientos físicos, con 17 resultados consolidados
y una llamada incierta descontada. Preflight gratuito posterior: 2/16 casos
centrales vigentes, faltan 14; 0/2 controles de contexto, faltan dos. Completar
sin repetir los dos resultados vigentes requiere una nueva decisión para hasta
16 llamadas físicas adicionales. No están autorizadas ni se ejecutan aquí.
result-partial.json registra identidades, resultados, gasto y evidencia.

## Complemento autorizado y examen completo

Las secciones anteriores y result-partial.json son la instantánea histórica
del primer tope, no el estado actual. José autorizó explícitamente hasta 16
llamadas adicionales (authorization-complement.json). No se reinició el
contador: 18 anteriores más 16 nuevas, 34/34 físicas acumuladas; cero saldo.
Se reutilizaron las dos respuestas válidas y se completaron los 14 casos
centrales restantes y los dos controles, sin cambiar versión, prompt u oro.

Los 16 casos centrales tienen veredicto vigente: 16/16 decisiones correctas,
8/8 defectos detectados y 8/8 válidos aceptados, sin falsas alarmas. El criterio
congelado también exige todas las categorías requeridas: coincide en 14/16.
irrelevant-past e irrelevant-perfect fueron correctamente marcados FAIL, pero
con IMPLAUSIBLE_DISTRACTORS en lugar de IRRELEVANT_DISTRACTORS. No se cambió
el criterio para hacerlos pasar. measured-result.json se registró mediante
exam record: FAILED, 16 casos medidos, goalMet=false, nunca APPROVED.

Los dos controles dieron NEEDS_CONTEXT con explicación y sin puntaje: 2/2
abstenciones esperadas. El exportador los presenta como NOT_JUDGED y 0/2 de
cobertura formal; no son llamadas pendientes ni un fallo de ejecución.
result-complete.json conserva las 18 comparaciones, gasto, procedencia y
respaldos. Las primeras 17 líneas del ledger, el oro y el checkpoint de
Corrección 1 permanecieron idénticos. No se ejecutó análisis/corrección del
curso real, ni se aprobó o publicó contenido. No hubo afinado ni reexamen.

La frontera entre distractor ajeno al objetivo e inverosímil requiere revisión
local; su solapamiento es una interpretación del resultado, no una aprobación
retroactiva. Este único corpus mide coincidencia con el oro, no estabilidad
entre corridas ni eficacia pedagógica con alumnos.
