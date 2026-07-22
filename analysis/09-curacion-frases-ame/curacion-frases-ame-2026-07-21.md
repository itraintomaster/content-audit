# Curación de frases con lema mal asignado + variantes AmE — 2026-07-21

Fuente: audit `2026-07-21T23-48-04` (post F-LABS-R040/R041/R042/R043/R044), 3026 misplaced + 831 OOC.
Compañera del backlog de iteración 3 documentado tras el fix R042-R044.

## Problema 1 — frases con el lema del primer token

El enriquecimiento asignó a cada fila de frase el lema de su **primer token**, no el del
headword EVP: "be able to do sth" (A2) quedó con lemma=`be`, "a lot" (A1) con lemma=`a`,
"I suppose" (A2) con lemma=`I`. Consecuencia: el techo asimétrico de F-LABS-R042 (las
frases solo rebajan) no podía operar sobre el lema correcto, y `able` respondía C2 (19
ocurrencias falsas), `suppose` C1 (44), `lot` A2 (43), `call` A2 (34), `few` B1 (17).

**Criterio de curación**: si EVP espera que un alumno de nivel X produzca la frase,
cada palabra de contenido de la frase es accesible en X. Como R042 solo puede REBAJAR
niveles, adjuntar la frase a todos sus lemas de contenido es seguro por construcción
(un idiom C2 jamás sube nada).

**Proceso**: se lematizó el texto de cada fila de frase multipalabra del catálogo
(spaCy, limpiando `sth/sb/etc.`, barras y paréntesis) y se generaron los pares
(lema de contenido, nivel de frase) que REBAJAN un nivel vigente. De 78 candidatos se
aceptaron **74** y se rechazaron 4:

- `graph` (artefacto de tokenización de "take a picture/photo(graph)")
- `easily`, `writer`, `convict` (palabras incidentales en frases-patrón tipo
  "become available/rich/a writer": el nivel acredita el patrón, no el ejemplo)

Cada lema aceptado agrega filas `evpPartOfSpeech: "phrase"` con topic
`curated-phrase-lemma-2026-07-21`, una por cada par (lema, POS de contenido) del
catálogo cuyo nivel supera el de la frase — así el techo de R042 opera tanto en el
match exacto como en el fallback. Destacados: `able` C2→A2 ("be able to do sth"),
`suppose` C1→A2 ("I suppose"), `lot` A2→A1 ("a lot"), `call` A2→A1 ("be called sth"),
`soon` A2→A1 ("see you later/soon/tomorrow"), `forget` A2→A1 ("Don't forget..."),
`worry` A2→A1 ("Don't worry"), `marry` B1→A2 ("get married"), `least` B1→A2 ("at least").

## Problema 2 — eje BrE/AmE y metalenguaje (topic `curated-ame-meta-2026-07-21`)

| lemma | nivel | cat | nota |
|---|---|---|---|
| cookie | A1 | ame | equivalente BrE: biscuit (A1); el C2 de EVP es otro sentido |
| store | A1 | ame | variante AmE de shop (A1); el B1 de EVP es el sentido BrE |
| canadian | A1 | nacionalidad | precedente italian/german/spanish (curated-ooc-2026-07-19) |
| infinitive | A1 | metalenguaje | término de consignas; precedente countable/uncountable |

## Fuera de esta curación (a propósito)

- **Variantes flexivas barridas por R040** (maths→math, trousers→trouser, clothes→clothe,
  DVD/CD/TV/OK, jeans, stairs, sunglasses, scissors...; 52 ocurrencias OOC de regresión,
  109 lemas huérfanos en el catálogo): se corrigen refinando la regla F-LABS-R040
  (tolerancia flexiva/ortográfica), no con filas duplicadas.
- **Contenido sucio** ("I am early. Verb in infinitive = be", "...next door. subject"):
  hay que limpiar la db, el analyzer detecta bien.
- **`let` en el knowledge de "let's"** (33 occ): decisión de producto (diagnóstico a
  nivel knowledge), no de catálogo.
- **Señal genuina intacta**: require, suppose queda en A2 solo si la oración es A1
  (el flag B1+ desaparece), purchase, bark, hop, hail, desire, snow/early/late siguen
  flaggeando donde corresponde.
