# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-05-11 — qa-tester — 3 stubs @Disabled R002 eliminados por delete patch
  - Tests "Given an AuditableKnowledge/Topic/Milestone and AuditContext, when
    onKnowledge/onTopic/onMilestone is called, then completes without error"
    eran inconsistent_traceability triplet:
    (1) Mis-tag: assertDoesNotThrow no verifica "orden determinista" (R002).
    (2) API obsoleta: nombres mencionan AuditContext, pero la firma actual
        de onKnowledge/onTopic/onMilestone es (AuditNode), sin AuditContext.
    (3) Falso PASSING: el sentinel-report contaba R002 PASSING solo por estos
        3 @Disabled (JUnit cuenta @Disabled como passing); los 3 tests reales
        en .java líneas 60-79 cubren assertDoesNotThrow pero NO llevan
        @Tag("F-LREC-R002").
  - _change: delete via patch correctivo (--replace). Post-apply: R002 vuelve
    a NO_TESTS legítimo. Comando apply emitido al lead.
  - R002 queda sin tag directo; propuesto al lead opción B: test "should
    process milestones in CEFR order A1, A2, B1, B2 regardless of input order"
    para cerrarlo honestamente en un próximo turno.
  - Esperando decisión del lead sobre si extender el patch con test honesto
    de R002 en este mismo turno o dejarlo separado.
  - why: la regla del usuario sobre cobertura directa exige que R002 caiga
    a NO_TESTS visible antes de que se le invente cobertura — un PASSING
    fantasma es peor que un NO_TESTS visible.

2026-05-11 — qa-tester + lead — R002 cubierto cross-feature sobre IAuditEngine
  - Lead autorizó Camino A: handwrittenTest sobre IAuditEngine (no sobre el
    analyzer pasivo) con tag F-LREC-R002.
  - Test propuesto: "Given two AuditableCourses with identical content but
    milestones declared in different orders, when AuditEngine.runAudit is
    invoked on each with the LemmaRecurrenceAnalyzer registered, then both
    AuditReports produce the same lemma-recurrence score..." — observable
    externamente sin tocar contrato del analyzer.
  - Patch validado: 0 additions, 1 modifications, 0 conflicts. Reportado al lead.
  - Architect aún no respondió al ESCALATION, pero el lead avanzó con la
    misma propuesta (Camino A). Si architect objeta más tarde, se paraliza.
  - why: el orden CEFR es responsabilidad del motor (IAuditEngine), no del
    analyzer pasivo. Test cross-feature sobre el motor con tag a la regla
    del feature owner es la única ruta honesta sin tests forzados.

2026-05-11 — qa-tester — Patch consolidado FEAT-LREC: 11 reglas NO_TESTS cubiertas
  - Scope ampliado de Task #6: además de R002 (cross-feature sobre IAuditEngine),
    se cubren R001/R004/R005/R006/R008/R009/R010/R011/R012 sobre LemmaRecurrenceAnalyzer
    y R014 sobre DefaultLemmaRecurrenceConfig.
  - R008 sobre el analyzer (no sobre DefaultExposureClassifier) porque la regla
    describe el flujo end-to-end de clasificación; classifier es bajo-nivel.
  - R014 sobre el config: getters del config exponen los valores con las
    restricciones de validez documentadas en la regla.
  - Validador: 0 additions, 3 modifications, 0 conflicts.
  - 15/15 reglas con tag directo, cero transitividad.
  - R003/R007/R013/R015 ya tenían cobertura previa; no se tocan.
  - why: auto mode + descripción ampliada en Task #6 mostrando 11 NO_TESTS reales.
