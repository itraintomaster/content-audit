# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-10-01 — qa-tester — Antes de proponer, qa-E2/chequear.py contra el modelo anterior de la cadena: que cada implementación
  con pruebas exista (si no, el apply la crea vacía), que ningún nombre ya exista (el merge pisaría su traza) y que ningún
  método derivado se repita. Después, comparar.py contra la copia de antes: sin pérdidas (bug 08 no muerde: ninguna
  implementación de los parches de E tiene domainLinks ni glossarySuggestions; los de interfaces y modelos quedan intactos).
2026-10-01 — qa-tester — El aviso «reglas sin prueba en el parche» del apply (TestCoverageValidator) cuenta sólo las pruebas
  declarativas, no las handwrittenTests: es ruido. La cobertura real: `tool listModules --with-coverage` (52 de 52).

