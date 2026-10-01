# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-10-01 — test-writer (paquete D) — generate sumó los 3 stubs nuevos de FileSystemEvpCatalogTest pero ninguno de los
  15 de OffLevelWordsAnalyzerTest (bug 2026-07-22-01): se escriben a mano con el nombre y el DisplayName que da
  TestStubGenerator (leído con la librería del jar, sin generate).
2026-10-01 — developer (paquete D) — Desde ronda 2, `dependency:build-classpath -pl audit-cli` falla offline porque
  local-language-infrastructure no está instalado: con `compile ... -pl audit-cli -am` el reactor lo resuelve.
2026-10-01 — developer (paquete D) — Las construcciones del proveedor no eran cuatro sino nueve: buscar
  «OffLevelWordsAnalyzerProvider(» y no «new OffLevelWordsAnalyzerProvider», porque varias usan el nombre completo.

2026-10-01 — qa-tester (paquete D) — `patch validate -i <copia>/sentinel.yaml` falla («Patch validation error:
  .../requirements/.../REQUIREMENT.md») si la copia no trae la carpeta requirements al lado: el validador busca las
  carpetas junto al sentinel.yaml. Para validar contra otra rama: `git archive <rama> sentinel.yaml requirements`.
2026-10-01 — architect (paquete D) — propose fusiona con el architectural_patch.yaml que haya en la carpeta: para
  proponer otro parche sin tocar uno en espera, éste se mueve a en-espera/ (con su TECH_SPEC) y se restaura después.

2026-10-01 — developer (paquete D) — «awfully» (oro) se escapaba por el sufijo -y: «awfull» + y, sin la doble l, daba
  «awful». Antes de -y la doble l no se simplifica (sunny sí es sun + y).
2026-10-01 — developer (paquete D) — `mvn -o compile -pl audit-domain` solo compila contra los jar viejos de ~/.m2
  (FormEntity, EvaluationTimeline «cannot find symbol»): sin `mvn install`, hay que sumar `-am`.
2026-10-01 — developer (paquete D) — Con la caché .cache de la copia ya armada, `analyze --analyzers off-level-words`
  tarda 2 s; la primera corrida, con la caché limpia, 55 s.

2026-09-30 — developer (paquete D) — Una copia del worktree para correr Main necesita ../ittm/pipeline/src/main/resources/
  vocabulary/lemmas_20k_words.txt (COCA_DATA_PATH del spaCy) o la caché .cache/spacy-nlp del 29/9: sin ellas los tokens
  salen sin frequencyRank y off-level-words marca 524 ejercicios (todo «fuera del EVP» en la banda fuerte).

2026-09-30 — qa-tester — `patch propose` rechaza una prueba de otra feature en la carpeta («references feature FEAT-SEMAF but patch targets FEAT-NIVFR»): let's como let quedó en el resolutor trazado a F-NIVFR-R003 y en la consulta del semáforo trazado a F-SEMAF-R004.
  why: la traza se valida contra el REQUIREMENT.md de la carpeta, no contra todas las features.
