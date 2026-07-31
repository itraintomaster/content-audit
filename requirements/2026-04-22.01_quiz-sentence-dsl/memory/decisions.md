# Decisions — FEAT-QSENT

2026-04-20 — analyst — DOUBT-OPTIONS-MULTIPLE RESOLVED: all entries + all `|` splits are equivalent variants of the single correct answer. No semantic distinction between multiple entries and pipe-splits. Serialization unifies all variants into one `[a|b|c]` block.

2026-04-20 — analyst — DOUBT-HINT-EXTRACTION RESOLVED: hints are formally extractable by the parser but remain stored inside `text` of TEXT parts (no model change). Preserved in `quizSentence` and `sentenceParts`; removed in plain sentence.

2026-04-20 — analyst — DOUBT-MAPPER-DELEGATION RESOLVED: mapper is an eager delegator — one call per quiz, result stamped into `AuditableQuiz`. No lazy evaluation, no cache.

2026-04-20 — analyst — plain sentence redefined as `List<String>`, one per variant, canonical at index 0. Audit consumes list[0]; multi-variant analysis is future evolution of the audit, out of scope for QSENT.

2026-04-20 — analyst — whitespace rule: canonical serialization emits single spaces between text and markers; parser is tolerant to runs; equivalence is whitespace-normalized. Round-trip invariants weakened to whitespace-normalized equivalence (was byte-exact).

2026-04-20 — analyst — plain sentence declared strictly unidirectional (lossy). Reconstruction of quizSentence/sentenceParts from plain is out of scope.

2026-04-20 — analyst — buildSentence migration fixes TWO bugs: (1) pipe variants leaked as literals; (2) hints left in analyzer input. Both fixes ship together with the migration.

Open doubts remaining: DOUBT-ESCAPE-CHARS (mechanism for reserved chars), DOUBT-CONVERTER-LOCATION (architectural form of the public API).

2026-04-20 — architect — DOUBT-CONVERTER-LOCATION RESOLVED: Option B (domain service) inside a dedicated public package `quizsentence` in course-domain. Engine collaborators (parser, serializer, plainDeriver, whitespaceNormalizer) hidden in a sibling `quizsentenceengine` internal package. Factory Seam (`QuizSentenceConverterFactory` + `DefaultQuizSentenceConverterFactory`) is the only cross-module construction path.
  why: keeps entity records as pure data; canonical Public-Port-Hidden-Adapter + Factory Seam matches existing style (nlp-infrastructure / revision-domain/engine).

2026-04-20 — architect — DOUBT-ESCAPE-CHARS RESOLVED: Option D (prohibition + fail-fast). No escape mechanism. Converter raises QuizSentenceSerializationException / QuizSentenceParseException when `[`, `]` or `____` appear literally in TEXT.
  why: real corpus (db/english-course) has zero literal brackets or four-underscore sequences; escape grammar is complexity without demand; can be retrofitted if a future dataset needs it.

2026-04-20 — architect — AuditableQuiz carrier change: renamed `sentence: String` → `sentences: List<String>` (not just retyped). Audit consumers read `sentences().get(0)` at call sites; no interface signature changes.
  why: name documents the list nature; silent retype would produce subtle bugs where legacy callers still expected a single-sentence string.

2026-04-20 — architect — QSENT design simplified: removed QuizSentenceConverterFactory, DefaultQuizSentenceConverterFactory, record QuizSentence, record PlainSentences. Converter signatures now use String and List<String> directly. DefaultQuizSentenceConverter is visibility: public so the composition root (audit-cli bootstrap) instantiates it directly via new; collaborators stay package-private in quizsentenceengine.
  why: QSENT is a stateless pure function with zero alternative implementations and no costly state — a factory decides nothing (unlike nlp-infrastructure which caches SpaCy state, or revision-domain/engine which selects pluggable strategies). Wrapper records over String and List<String> added ceremony at every call site without carrying new invariants.

2026-04-20 — qa-tester — testModule/testPackage for F-QSENT-J001 set to course-domain / com.learney.contentaudit.coursedomain.quizsentenceengine.
  why: the journey flow ends-to-end needs to construct DefaultQuizSentenceConverter with its package-private collaborators (QuizSentenceSerializer, QuizSentenceParser, PlainSentenceDeriver, WhitespaceNormalizer). Placing the test in quizsentence (public) would lose package-private visibility of the collaborators.

2026-04-20 — qa-tester — R023 (plain-sentence unidireccional) tagged on a reflection-style test "should expose only the three forward conversions". R023 is a contract/shape rule, not a behavior rule; without this test the rule would be uncovered.
  why: user memory rule forbids transitive coverage. The rule has no natural behavioral test; a reflective interface-shape test is the minimal direct tag.

2026-04-20 — qa-tester — R025 (public domain functionality) tagged on a cross-module consumability test, not on ArchUnit. R025 is critical and needs an explicit test per user memory rule (no transitive coverage).

2026-04-20 — architect — post-apply amendment: propuesto patch para declarar testModule: course-domain y testPackage: com.learney.contentaudit.coursedomain.quizsentenceengine en el journey F-QSENT-J001, eliminando el warning de sentinel generate que impedia generar el stub del journey test.
  why: la decision de ubicacion ya estaba tomada por qa-tester (collaborators package-private en quizsentenceengine); solo faltaba reflejarla en sentinel.yaml. Sin estos campos, generate skipea el stub.

2026-05-21 — analyst — R026 y R027 retiradas de REQUIREMENT.md. Superadas por FEAT-DBSENT: el mapper ya no invoca toPlainSentences (contradice F-DBSENT-R002) y la correccion de los dos bugs del buildSentence privado vive ahora en el paso de pre-computo offline que pobla QuizTemplateEntity.sentences, no en el mapper. Patron de retiro al estilo "Nota sobre numeracion" de FEAT-SLEN; IDs no se reasignan; demas IDs (incluido R028) mantienen su numeracion. Validacion sentinel OK. Pendiente: sentinel feature sync (lo corre el usuario).
  why: el conflicto era directo (R027 vs F-DBSENT-R002); R026 describia una transicion arquitectonica que ya quedo atras. Limpieza incluye referencias inline en R018, Assumption[3], Assumption[5] y DOUBT-MAPPER-DELEGATION.

2026-07-30 — analyst — R023: la conversion de preservacion de FEAT-RPRES (`quizSentence` + ejercicio base -> `sentenceParts`, alias `parseOnto`) queda declarada DENTRO del alcance permitido. No viola la unidireccionalidad: va en la misma direccion que el parseo del Grupo D (de `quizSentence` a `sentenceParts`); el base solo aporta atributos que la DSL nunca codifico (tipo de formulario, incidencia, etiquetas, nombre — F-RPRES-R001/R002), no reconstruye nada de lo que la plain sentence pierde.
  why: lo que R023 prohibe es la direccion plain sentence -> ejercicio, no que la entrada textual venga acompanada de contexto no codificado.

2026-07-30 — analyst — R023 reformulada para que no dependa de un conteo de conversiones. Ahora se enuncia como tres invariantes observables: (1) la derivacion no es inyectiva (dos ejercicios distintos -> misma lista de plain sentences); (2) una plain sentence entregada a una conversion que lee texto no recupera blanks/respuestas/hints, ni siquiera con el ejercicio original como base; (3) ninguna conversion admite una plain sentence (ni la lista) como entrada. Se agrega tabla de conversiones permitidas y nota explicita de que la regla acota una direccion, no una cantidad.
  why: la formulacion "expone solo las tres conversiones hacia adelante" del primer test forzo a subir el assert de 3 a 4 al llegar FEAT-SMODE sin tocar el enunciado, y volvio a romper al llegar `parseOnto` (5). Cada metodo nuevo legitimo rompia la regla; la unidireccionalidad nunca habia cambiado.

2026-07-30 — analyst — consecuencia para el test de R023 (`shouldExposeOnlyTheThreeForwardConversions...`): el conteo reflectivo `assertEquals(4, methodNames.size())` deja de tener respaldo en la regla y debe salir. El cuerpo pasa a asertar los tres invariantes (colision de derivacion, no-recuperacion al alimentar una plain sentence, ausencia de conversion que tome la lista de plain sentences como entrada). Nombre del test a renombrar por @qa-tester/@test-writer; el analyst no toca codigo.
  why: dejar el numero magico obliga a un ritual de bump en cada feature futura, que es el mecanismo que produjo este desalineamiento.
