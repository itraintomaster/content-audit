# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer — Con `env -i` el PATH pierde el python de miniconda y la gramática no arranca:
  «NlpResourcesUnavailableException … No module named 'spacy'». Poner `/opt/homebrew/Caskroom/miniconda/base/bin` en el
  PATH limpio, después del claude falso.
2026-10-01 — developer — `--workdir` necesita los recursos del árbol (analysis/recursos-compartidos y demás): una carpeta
  de estado aparte se arma con enlaces a las carpetas de la copia y su propio `.content-audit`.

2026-10-01 — developer — LanguageTool 6.8: `new AmericanEnglish()` revienta en el inicializador; usar
  `Languages.getLanguageForShortCode("en-US"/"en-GB")`. Con Java 25 hace falta jdk.xml.totalEntitySizeLimit (lo sube el
  adaptador si nadie lo fijó).
2026-10-01 — developer — `-Dgroups=herramienta-real` y cualquier -Dtest: siempre con `-am`; sin él, los jars viejos de
  ~/.m2 dan NoClassDefFoundError.
2026-10-01 — developer — El catálogo lee las fichas al construirse: en una prueba que registra un examen y después pide
  la ficha, construir el catálogo después del registro (si no, la tarjeta dice «sin examen»).

2026-10-01 — qa-tester — El aviso «reglas sin prueba en el parche» de la simulación (TestCoverageValidator) cuenta
  sólo pruebas declarativas, no handwrittenTests: la cobertura real la da `tool listModules --with-coverage --root
  <modelo simulado>` (las 8 reglas [covered]).

2026-09-30 — architect — `patch propose --replace -i <sentinel.yaml de ronda1/integracion>` no fusiona: domainLinks y
  glossarySuggestions quedan (comparado con `arq-B2/comparar.py`); sólo agrega `suppressed: false` en patterns.
  why: el bug 08 de Sentinel pierde domainLinks al fusionar; con --replace no hay fusión.

2026-09-30 — architect — Simulado en memoria (arq-B2/sim.sh, Simular de arq-B): al aplicar, `localtoolsengine` queda sin
  `allowedClients: [audit-cli]` (bug 10), igual que `judgingengine` en la ronda 1. Va como gancho para el integrador.

2026-09-30 — architect — `patch propose` y `patch validate` no resuelven tipos (requiresInject, firmas, `extends`), pero sí
  rechazan un `implements` hacia una interfaz que no está ni en sentinel.yaml ni en el mismo parche; y aceptan `_change: modify`
  sobre un módulo que no existe. Para parches que se aplican en cadena: cada uno implementa sólo interfaces propias o ya aplicadas.

2026-09-30 — architect — El buscador de Maven Central (`solrsearch`) daba 6.6 como última de `org.languagetool:language-en`;
  `repo1.maven.org/…/maven-metadata.xml` dice 6.8 (5/5/2026). Para la versión vigente, leer el maven-metadata.xml.

2026-09-30 — architect — En el YAML de un parche, un campo llamado `yes` o `no` se lee como booleano (YAML 1.1): se usaron
  `yesProbability` y `noProbability`.
