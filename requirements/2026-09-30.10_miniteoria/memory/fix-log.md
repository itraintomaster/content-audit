# Fix Log

Fixes that worked, with a short why. Newest entries on top.

<!-- entries below -->

2026-09-30 — developer (C) — AuditNode.equals/hashCode recorren padre e hijos: los hallazgos por ejercicio se guardan en un IdentityHashMap (en un HashMap, el hashCode de un nodo desborda la pila). En audit-domain no hay Jackson: las pruebas graban el JSON del veredicto y un lector falso lo devuelve.
