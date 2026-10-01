# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer (construcción E) — `tray show` muestra cada ejercicio entero (consigna, opciones, lo que acepta
  cada hueco, miniteoría) y los pares de la ciega lado por lado como `sample show`, sin las respuestas de los huecos
  (bd7e9cfa, `TrayCmdStudentViewIntegrationTest`). why: en el control las reglas de A1 cambian respuestas, opciones o la
  consigna, no la oración; con sólo la oración, los 100 pares de la ciega parecían iguales.

2026-10-01 — developer (construcción E) — Dejar una regla sin muestra aprobada saca también los cambios de la mesa
  hechos encima de ella en el mismo ejercicio (opción A de DOUBT-SACAR-ENCADENADO): en las pruebas, las verduras
  reescritas por la mesa salen con la regla de la pista. why: los dos tocaron PARTS/SENTENCES del mismo ejercicio.

