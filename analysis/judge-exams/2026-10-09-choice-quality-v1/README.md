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
