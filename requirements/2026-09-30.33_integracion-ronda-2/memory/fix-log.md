# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer — Aridades de la ronda 2 en los tests: PadRonda2.java (scratchpad/impl-ronda2-comun, el script
  de la ronda 1 con la tabla nueva) completa 334 llamadas en 88 archivos, sitio por sitio como el censo; los siete
  journeys que arman GetCmd por reflexión, a mano a 12.
2026-10-01 — developer — generate no agregó al stub el método de la interfaz que su interfaz extiende (describe() en las
  tres configuraciones de B, status() en los tres adaptadores de local-language-infrastructure), ni las sobrecargas
  nuevas cuando el archivo ya tenía un método con ese nombre (build de la vista consolidada, getConsolidated con topes,
  changeableFields por partes). Se suman sin implementar, como el resto del stub.
2026-10-01 — developer — Los tests del caché de tokens corren un procesador de prueba en Python que habla el protocolo
  de sample_processor.py y anota cada lanzamiento al lado suyo (SpacyProcessRunnerTest.FakeSpacy); el runner y la
  fábrica tienen un constructor de paquete con el directorio del caché, para que cada prueba use el suyo.
2026-10-01 — developer — Una prueba que crea un procesador o una lista debe darle a la segunda corrida otro runner: la
  huella de un archivo se recalcula cuando cambia su tamaño o su fecha, como entre dos procesos.

2026-10-01 — integrador — Rehacer encima de una base nueva: juntar ronda1/integracion en ronda2/integracion y correr
  `integrador-2/rehacer-ronda-2.sh` (specs.py, armar32.py, cadena.sh real y revisar.sh real). Probado con FEAT-CIERRE:
  los 14 parches salen iguales salvo su descripción, y sólo cambian las huellas.

2026-10-01 — integrador — Rebasar desde el commit de DISEÑO de cada paquete (B ae83b2d0, E 70c50c1a, F cfb6b20a), no desde
  HEAD: así `rehacer-ronda-2.sh` se puede correr otra vez encima del parche de cierre de la ronda 1 sin aplicar nada dos
  veces. Volver a tomar B = cambiar su commit en la tabla ORIGEN de integrador-2/rebase.py.
2026-10-01 — integrador — Una implementación no tiene `description` en el DSL (tampoco una interfaz ni un modelo): `patch
  propose` la descarta sin aviso. El porqué de las once correcciones y los cinco arreglos va en el TECH_SPEC.
2026-10-01 — integrador — yaml.safe_dump escribe anclas (&id001) cuando dos elementos comparten la misma lista, y el lector
  del DSL no las entiende («no String-argument constructor ... 'id001'»). Copiar las listas o volcar con un Dumper que
  ignora alias (rebase.py, armar32.py).
2026-10-01 — integrador — La revisión de visibilidad (integrador-2/visibilidad.py) encuentra lo que `patch validate` no mira y
  rompe la compilación: un tipo interno de un paquete usado desde otro, o de un módulo del que no se depende. Con el modelo
  de hoy da 0; con la ronda cruda, SpokenTextBuilder.
2026-10-01 — integrador — El generate simulado (DryRunGenerate del scratchpad, misma librería) más un censo con JavaParser
  sobre el código que dejaría generate (integrador-2/tools/Censo.java) dan qué deja de compilar: la aridad vale la del
  código, no la del modelo (GetCmd tiene 11 en el código y 12 inyecciones en el modelo), y el smart-merge cambia el
  constructor de una implementación aunque su archivo no tenga @Generated.
