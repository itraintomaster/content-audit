# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-09-30 — analyst — En el prototipo, expandir «'s» probando todas las reglas a la vez daba «is gone» para «'s gone»: hay que tomar
  la primera que aplica (participio → has, si no → is; lo mismo 'd → had/would).
  why: generaba formas «que faltan» falsas; el analizador real tiene que elegir la expansión por contexto.
