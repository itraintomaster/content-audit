# Fix Log

Fixes that worked, with a short why. Newest entries on top.

<!-- entries below -->

2026-09-30 — developer (C) — `AuditNode.equals/hashCode` recorren padre e hijos (todo el árbol): la corrida guarda pedidos y juicios en `IdentityHashMap`, nunca en un `HashMap` por nodo.
2026-09-30 — developer (C) — El clasificador no recibe el render y `quizsentenceengine` está oculto para audit-domain: usa `new DefaultQuizStudentViewRenderer(null)` (`render` no toca el conversor; sólo `finishedSentence`). Escalado para sumarlo a su requiresInject.
2026-09-30 — developer (C) — Una resolución FAILED no dice si fue salida inutilizable o falla del servicio: el motivo sale de `session.unavailableReason()` si hubo falla del servicio en la corrida, y si no, «el juez contestó algo inutilizable».
