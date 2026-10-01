# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

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
