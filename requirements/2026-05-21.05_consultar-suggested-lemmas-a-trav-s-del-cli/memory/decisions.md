# Decisions — FEAT-CSLATDC

2026-05-21 — qa-tester — R002 ("uso del verbo existente get"): tiene superficie observable en GetCmd (despacho del resource "suggested-lemmas" por la rama de `get` y no por un nuevo command). Se propuso handwrittenTest en GetCmd.
  why: aunque el sealed interface ya está cerrado a `GetCommand`, el routing del string "suggested-lemmas" sí es observable en runtime y diferenciable de "introducir un nuevo verbo CLI" (que sería otro command class).

2026-05-21 — qa-tester — R013 ("filtros no soportados como isIrregular"): NO se propuso handwrittenTest.
  why: el TECH_SPEC declara satisfacción estructural via record cerrado `SuggestedLemmasFilter` (sólo 3 campos). El residual semántico para `SuggestedLemmaQueryRejectedException` requeriría inventar un caso de validación adicional (ej. limit negativo), que no está cubierto por la letra de R013 — la regla habla específicamente de filtros como `isIrregular`. Pasar un campo inexistente es imposible en Java. Reformular o aceptar cobertura estructural es decisión del analista/usuario.

2026-05-21 — qa-tester — R015 ("instrucciones funcionales del agente revisor"): NO se propuso handwrittenTest.
  why: TECH_SPEC y el architect coinciden en que es responsabilidad de prompt template / documentación, sin superficie observable en código. Aplica memoria del usuario "Untestable rules go to analyst, not ArchUnit": preferir retiro/reformulación por @analyst antes que inventar test reflexivo.

2026-05-21 — analyst — Retirados R013 (filtros no soportados / isIrregular) y R015 (instrucciones al agente revisor) del REQUIREMENT por decisión funcional del usuario; eliminado J003 (era el único journey con gate exclusivo en R013) y eliminada DOUBT-IRREGULAR-FILTER. Limpieza en Scope (bullet IN "Instruir al agente revisor"; bullet OUT "filtro isIrregular"). IDs de reglas restantes NO renumerados — identidad estable. Validación CLI: [OK].
  why: R013 queda cubierta estructuralmente por el record cerrado SuggestedLemmasFilter; R015 es prompt/documentación, sin superficie observable. Sin reglas que validar, J003 quedaba sin gate observable.
