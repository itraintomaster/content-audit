# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->
2026-07-21 — developer — R040's literal word!=lemma exclusion inicialmente rompia el test de R041 "let's"/"let" (evpPos="phrase"): la contraccion tiene word!=lemma pero no es una forma derivada/compuesta en el sentido de R040. Fix: `derivedForm` excluye ademas las entradas cuya evpPartOfSpeech sea "phrase"/"phrasal verb" (case-insensitive), espejando el carve-out que R041 ya declara para esas categorias. Con eso los 12/12 tests quedan verdes sin tocar la fixture.
  why: sin este carve-out, cualquier entrada monopalabra de categoria "phrase" (contraccion/idiom con apostrofe) caeria en R040 aunque el Doubt[DOUBT-PHRASE-MONOPALABRA] la deja explicitamente diferida.
2026-07-21 — test-writer — R040 test de fallback por lema necesito invertir los niveles fixture (derivada mas baja que base) para que el test falle pre-implementacion; con derivada B1 > base A2 el min() actual coincidia por casualidad con el resultado correcto y el test pasaba en falso (no probaba R040 en absoluto)
  why: al testear un fallback tipo minimo(), la forma excluida debe tener el nivel MAS BAJO para que su inclusion cambie el resultado observable
2026-05-09 — test-writer — J001 usa @ExtendWith(MockitoExtension) + InjectMocks sobre LemmaByLevelAbsenceAnalyzer; J002–J005 son carriers in-memory sin el analyzer
  why: J001 es pipeline completo (full-audit), J002–J005 son journeys de "consulta de datos" donde basta construir los modelos y validar sus propiedades
