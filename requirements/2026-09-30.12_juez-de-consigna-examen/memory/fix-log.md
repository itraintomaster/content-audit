# Fix Log

Fixes that worked, with a short why. Newest entries on top.

<!-- entries below -->

2026-09-30 — developer (C, líder) — Sin `mvn clean` offline (falta el plugin): tras cambiar un constructor, borrar
  */target/classes, */target/test-classes y */target/maven-status; la compilación incremental no recompila a los que lo llaman.
2026-09-30 — developer (C, líder) — Con 4 consultas a la vez los jueces falsos de las pruebas tienen que guardar lo que se les
  pregunta en listas seguras entre hilos: FHallJ001 perdió una de 64 llamadas con un ArrayList.

2026-09-30 — developer (C) — Una consulta que vuelve por sesión o cuota casi nunca llega como excepción: AgentRun la atrapa y devuelve un RunState HALTED sin artifact, con terminationReason «[ERROR] top-level agent threw: claude CLI exited with 1: …». Por eso el juez explica el terminationReason cuando falta el artifact.
2026-09-30 — developer (C) — Al clasificar fallas, nunca pasar el mensaje completo de Jackson (trae «line: 401, column: 429» y el texto del veredicto): usar getOriginalMessage(); los códigos HTTP se buscan como palabra y no detrás de «line:»/«column:».

2026-09-30 — qa-tester — `patch propose` rechaza una prueba trazada a otra feature: «references feature FEAT-JEXAM but patch targets FEAT-UNICA». Cada prueba traza a la feature de su propio parche.
2026-09-30 — qa-tester — Para simular sobre otro modelo, copiarlo a la raíz simulada: DslReader resuelve `definitions` desde la carpeta del sentinel.yaml que lee (qa-C/sim.sh).

2026-09-30 — architect — `patch propose` rechaza un implements de una implementación de RAÍZ hacia una interfaz de un
  PAQUETE de otro módulo («implements unknown interface»); de paquete a paquete y de paquete a raíz funciona. Por eso
  JudgeExamRegistry, JudgeRuntimeConfig y los *Config de cada juez viven en la raíz de audit-domain.
2026-09-30 — architect — Al aplicar, `patterns` de un módulo se REEMPLAZA entero si el parche trae alguno
  (PatchApplier.mergeModule): el parche repite el patrón Factory de QuizInstructionJudgeFactory junto al nuevo.
2026-09-30 — architect — `patch propose --replace` reemplaza la propuesta en vez de fusionarla (sirve para sacar
  elementos sin dejar un _change: delete); `tech-spec write` se niega a achicar el archivo sin `--replace`.
2026-09-30 — architect — El shell es zsh: `${PIPESTATUS[0]}` sale vacío; para el exit code, redirigir a archivo y leer `$?`.
