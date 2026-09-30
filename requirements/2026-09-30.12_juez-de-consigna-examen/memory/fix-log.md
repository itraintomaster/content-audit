# Fix Log

Fixes that worked, with a short why. Newest entries on top.

<!-- entries below -->

2026-09-30 — architect — `patch propose` rechaza un implements de una implementación de RAÍZ hacia una interfaz de un
  PAQUETE de otro módulo («implements unknown interface»); de paquete a paquete y de paquete a raíz funciona. Por eso
  JudgeExamRegistry, JudgeRuntimeConfig y los *Config de cada juez viven en la raíz de audit-domain.
2026-09-30 — architect — Al aplicar, `patterns` de un módulo se REEMPLAZA entero si el parche trae alguno
  (PatchApplier.mergeModule): el parche repite el patrón Factory de QuizInstructionJudgeFactory junto al nuevo.
2026-09-30 — architect — `patch propose --replace` reemplaza la propuesta en vez de fusionarla (sirve para sacar
  elementos sin dejar un _change: delete); `tech-spec write` se niega a achicar el archivo sin `--replace`.
2026-09-30 — architect — El shell es zsh: `${PIPESTATUS[0]}` sale vacío; para el exit code, redirigir a archivo y leer `$?`.
