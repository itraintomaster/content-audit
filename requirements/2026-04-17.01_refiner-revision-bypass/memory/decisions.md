# Decisions

2026-04-18 — architect — FEAT-REVBYP: rediseño completo del patch tras dos intentos fallidos.
  why: el primer patch declaraba las 6 impls del engine tanto a module-root como dentro del package `engine`, lo que genera `conflicts with root-level component of the same name` al generar. El cleanup patch removió los roots pero el merge terminó dejando el módulo fuera del sentinel.yaml.

2026-04-18 — architect — Engine encapsulation: package `engine` con visibility=public, UNICA impl pública = DefaultRevisionEngineFactory. El resto (IdentityReviser, AutoApproveValidator, DefaultRevisionValidatorResult, DispatchingReviser, DefaultCourseElementLocator, DefaultRevisionEngine) package-private. Sin duplicados a root.
  why: Factory Seam puro; el composition root sólo conoce `new DefaultRevisionEngineFactory().create(config)`. Todo lo demás queda invisible cross-module.

2026-04-18 — architect — RevisionEngineConfig como Config Record con opcionales nullable.
  why: 8 inputs, dos con bypass default razonable (validator, elementLocator) + lista de revisers (siempre completada con IdentityReviser fallback en el factory). Permite agregar puertos opcionales sin romper callers.

2026-04-18 — architect — RevisionVerdict (APPROVED/REJECTED) separado de RevisionOutcomeKind (6 valores que incluyen pre-proposal aborts).
  why: el validator decide el verdict (R006); el engine decide el outcome (incluye NO_REVISER, CONTEXT_UNAVAILABLE, ELEMENT_NOT_FOUND, APPROVED_APPLY_FAILED). Distintos autores, distintos enums.

2026-04-18 — architect — CourseElementSnapshot como indirection sobre QuizTemplateEntity.
  why: scope-limited a QUIZ pero modelado para crecer a otros NodeKind sin tocar el engine ni el Reviser port.
