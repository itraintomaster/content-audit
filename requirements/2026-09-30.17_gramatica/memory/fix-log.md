# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-10-01 — qa-tester — Bug 08 al fusionar: `qa-B2/tools/restaurar.py <parche del arquitecto> <fusionado> <salida>`
  copia domainLinks y glossarySuggestions perdidos y se vuelve a proponer con --replace; `comparar.py` da 0 perdidos.

2026-09-30 — analyst — LanguageTool 6.x con Java 24 o más no carga su grammar.xml: «JAXP00010004 … jdk.xml.totalEntitySizeLimit».
  Arranca con -Djdk.xml.totalEntitySizeLimit=50000000 (0 o valores de 2.000 millones dan «Invalid setting»).
  why: el JDK nuevo bajó el límite de entidades XML; el proyecto usa Java 17, pero la Mac tiene 25 por omisión.

2026-09-30 — analyst — El LanguageTool de IntelliJ (Grazie) no trae los modelos de OpenNLP del agrupador inglés
  («/en-token.bin», «/en-pos-maxent.bin», «/en-chunker.bin»): se midió con un AmericanEnglish sin chunker.
  why: las reglas que dependen de él no corrieron; el LanguageTool de Maven Central los trae y puede marcar algo más.

2026-09-30 — analyst — Al medir, una opción «-» de MC, una lista «a / b» sin paréntesis y una nota «(…).» tras un punto
  producían marcas falsas (palabra repetida, doble punto): se sacan al armar la oración (R001).
