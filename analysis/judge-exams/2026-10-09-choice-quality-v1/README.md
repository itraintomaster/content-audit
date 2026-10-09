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
